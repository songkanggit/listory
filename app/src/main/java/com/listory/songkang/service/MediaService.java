package com.listory.songkang.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.listory.songkang.IMediaPlayerAidlInterface;
import com.listory.songkang.utils.StringUtil;
import com.pili.pldroid.player.AVOptions;
import com.pili.pldroid.player.PLMediaPlayer;
import com.pili.pldroid.player.PLOnBufferingUpdateListener;
import com.pili.pldroid.player.PLOnCompletionListener;
import com.pili.pldroid.player.PLOnErrorListener;
import com.pili.pldroid.player.PLOnInfoListener;
import com.pili.pldroid.player.PLOnPreparedListener;
import com.pili.pldroid.player.PLOnSeekCompleteListener;

import org.intellij.lang.annotations.MagicConstant;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by songkang on 2018/1/14.
 */

public class MediaService extends Service {

    private static final String TAG = MediaService.class.getSimpleName();
    private static final boolean DEBUG = true;

    public static final String PLAY_ACTION = "com.liyang.songkang.play";
    public static final String PLAY_ACTION_PARAM_LIST = "list";
    public static final String PLAY_ACTION_PARAM_POSITION = "position";
    public static final String PLAY_ACTION_PARAM_FORCE_UPDATE = "force";

    public static final String MUSIC_CHANGE_ACTION = "com.liyang.songkang.music_change";
    public static final String MUSIC_CHANGE_ACTION_PARAM = "data";

    public static final String PLAY_STATE_CHANGE_ACTION = "com.liyang.songkang.state_change";
    public static final String PLAY_STATE_CHANGE_ACTION_PARAM = "state";

    private static final String PAUSE_ACTION = "com.liyang.songkang.pause";
    private static final String NEXT_ACTION = "com.liyang.songkang.next";
    private static final String PREVIOUS_ACTION = "com.liyang.songkang.previous";
    private static final String REPEAT_PLAY_ACTION = "com.liyang.songkang.repeat_play";
    private static final String RANDOM_PLAY_ACTION = "com.liyang.songkang.random_play";

    public static final String BUFFER_UPDATE = "com.liyang.songkang.buffer_update";
    public static final String BUFFER_UPDATE_PARAM_PERCENT = "percent";

    public static final String PLAY_STATE_UPDATE = "com.liyang.songkang.state_update";
    public static final String PLAY_STATE_UPDATE_POSITION = "position";
    public static final String PLAY_STATE_UPDATE_DURATION = "duration";
    public static final String PLAY_STATE_UPDATE_DATA = "data";

    private IBinder mBinder;

    private HandlerThread mHandlerThread;
    private Handler mMusicPlayHandler;
    private List<MusicTrack> mMusicTrackPlayList = new ArrayList<>();
    private List<MusicTrack> mMusicTrackRandomPlayList = new ArrayList<>();
    private int mPlayPosition;

    private TelephonyManager mTelephonyManager;
    private PhoneStateListener mPhoneStateListener;

    private PLMediaPlayer mMediaPlayer;
    private AVOptions mAVOptions;
    private volatile boolean mIsInitialized = false;

    @RepeatMode
    private volatile int mPlayMode = RepeatMode.REPEAT_ALL;
    @MagicConstant(intValues = {RepeatMode.REPEAT_CURRENT, RepeatMode.REPEAT_RANDOM, RepeatMode.REPEAT_ALL, RepeatMode.REPEAT_NONE})
    public @interface RepeatMode {
        int REPEAT_CURRENT = 0;
        int REPEAT_RANDOM = 1;
        int REPEAT_ALL = 2;
        int REPEAT_NONE = 3;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if(mBinder == null || !mBinder.isBinderAlive()) {
            mBinder = new MediaServiceStub(this);
        }
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PLAY_ACTION);
        intentFilter.addAction(PAUSE_ACTION);
        intentFilter.addAction(REPEAT_PLAY_ACTION);
        intentFilter.addAction(RANDOM_PLAY_ACTION);
        intentFilter.addAction(NEXT_ACTION);
        intentFilter.addAction(PREVIOUS_ACTION);
        registerReceiver(mIntentReceiver, intentFilter);

        mHandlerThread = new HandlerThread("MusicPlayer", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();
        mMusicPlayHandler = new Handler(mHandlerThread.getLooper());

        mAVOptions = new AVOptions();
        // the unit of timeout is ms
        mAVOptions.setInteger(AVOptions.KEY_PREPARE_TIMEOUT, 10 * 1000);
        // 1 -> hw codec enable, 0 -> disable [recommended]
//        int codec = getIntent().getIntExtra("mediaCodec", 0);
        mAVOptions.setInteger(AVOptions.KEY_MEDIACODEC, 0);
//        int startPos = getIntent().getIntExtra("start-pos", 0);
//        mAVOptions.setInteger(AVOptions.KEY_START_POSITION, 0);
        mAVOptions.setInteger(AVOptions.KEY_SEEK_MODE, 1);

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        startTelephonyListener();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mIntentReceiver);
        stopTelephonyListener();
        release();
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(null);
    }

    private Runnable updateMusicProgress = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(PLAY_STATE_UPDATE);
            intent.putExtra(PLAY_STATE_UPDATE_POSITION, position());
            intent.putExtra(PLAY_STATE_UPDATE_DURATION, duration());
            intent.putExtra(PLAY_STATE_UPDATE_DATA, getCurrentTrack());
            sendBroadcast(intent);
            mMusicPlayHandler.postDelayed(this, 1000);
        }
    };

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case PLAY_ACTION:
                    List<MusicTrack> musicTrackList = intent.getParcelableArrayListExtra(PLAY_ACTION_PARAM_LIST);
                    int position = intent.getIntExtra(PLAY_ACTION_PARAM_POSITION, 0);
                    open(musicTrackList, position);
                    break;
                case PAUSE_ACTION:
                    pause();
                    break;
                case REPEAT_PLAY_ACTION:
                    mPlayMode = RepeatMode.REPEAT_CURRENT;
                    break;
                case RANDOM_PLAY_ACTION:
                    play();
                    break;
                case NEXT_ACTION:
                    goToNext();
                    break;
                case PREVIOUS_ACTION:
                    goToPrevious();
                    break;
            }
        }
    };

    private PLOnPreparedListener mOnPreparedListener = new PLOnPreparedListener() {
        @Override
        public void onPrepared(int preparedTime) {
            Log.i(TAG, "On Prepared !");
            mMediaPlayer.start();
            mIsInitialized = true;
        }
    };

    private PLOnInfoListener mOnInfoListener = new PLOnInfoListener() {
        @Override
        public void onInfo(int what, int extra) {
            Log.i(TAG, "OnInfo, what = " + what + ", extra = " + extra);
            switch (what) {
                case PLOnInfoListener.MEDIA_INFO_BUFFERING_START:
//                    mLoadingView.setVisibility(View.VISIBLE);
                    break;
                case PLOnInfoListener.MEDIA_INFO_BUFFERING_END:
//                    mLoadingView.setVisibility(View.GONE);
                    break;
                case PLOnInfoListener.MEDIA_INFO_VIDEO_RENDERING_START:
//                    mLoadingView.setVisibility(View.GONE);
                    break;
                case PLOnInfoListener.MEDIA_INFO_VIDEO_GOP_TIME:
                    Log.i(TAG, "Gop Time: " + extra);
                    break;
                case PLOnInfoListener.MEDIA_INFO_AUDIO_RENDERING_START:
//                    mLoadingView.setVisibility(View.GONE);
                    break;
                case PLOnInfoListener.MEDIA_INFO_SWITCHING_SW_DECODE:
                    Log.i(TAG, "Hardware decoding failure, switching software decoding!");
                    break;
                case PLOnInfoListener.MEDIA_INFO_METADATA:
                    Log.i(TAG, mMediaPlayer.getMetadata().toString());
                    break;
                case PLOnInfoListener.MEDIA_INFO_VIDEO_BITRATE:
                case PLOnInfoListener.MEDIA_INFO_VIDEO_FPS:
//                    updateStatInfo();
                    break;
                case PLOnInfoListener.MEDIA_INFO_CONNECTED:
                    Log.i(TAG, "Connected !");
                    break;
                case PLOnInfoListener.MEDIA_INFO_VIDEO_ROTATION_CHANGED:
                    Log.i(TAG, "Rotation changed: " + extra);
                default:
                    break;
            }
        }
    };

    private PLOnBufferingUpdateListener mOnBufferingUpdateListener = new PLOnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(int percent) {
            Intent intent = new Intent();
            intent.setAction(BUFFER_UPDATE);
            intent.putExtra(BUFFER_UPDATE_PARAM_PERCENT, percent);
            sendBroadcast(intent);
        }
    };

    /**
     * Listen the event of playing complete
     * For playing local file, it's called when reading the file EOF
     * For playing network stream, it's called when the buffered bytes played over
     * <p>
     * If setLooping(true) is called, the player will restart automatically
     * And ｀onCompletion｀ will not be called
     */
    private PLOnCompletionListener mOnCompletionListener = new PLOnCompletionListener() {
        @Override
        public void onCompletion() {
            goToNext();
            Log.d(TAG, "Play Completed !");
        }
    };

    private PLOnErrorListener mOnErrorListener = new PLOnErrorListener() {
        @Override
        public boolean onError(int errorCode) {
            switch (errorCode) {
                case PLOnErrorListener.ERROR_CODE_IO_ERROR:
                    /**
                     * SDK will do reconnecting automatically
                     */
//                    Utils.showToastTips(PLAudioPlayerActivity.this, "IO Error !");
                    return false;
                case PLOnErrorListener.ERROR_CODE_OPEN_FAILED:
//                    Utils.showToastTips(PLAudioPlayerActivity.this, "failed to open player !");
                    break;
                case PLOnErrorListener.ERROR_CODE_SEEK_FAILED:
//                    Utils.showToastTips(PLAudioPlayerActivity.this, "failed to seek !");
                    break;
                default:
//                    Utils.showToastTips(PLAudioPlayerActivity.this, "unknown error !");
                    break;
            }
            return true;
        }
    };

    private PLOnSeekCompleteListener mOnSeekCompleteListener = new PLOnSeekCompleteListener() {
        @Override
        public void onSeekComplete() {
            Log.d(TAG, "===========PLOnSeekCompleteListener=============");
        }
    };

    // Listen to the telephone
    private void startTelephonyListener() {
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (mTelephonyManager == null) {
            Log.e(TAG, "Failed to initialize TelephonyManager!!!");
            return;
        }

        mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE:
                        Log.d(TAG, "PhoneStateListener: CALL_STATE_IDLE");
                        if (mMediaPlayer != null) {
                            mMediaPlayer.start();
                        }
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        Log.d(TAG, "PhoneStateListener: CALL_STATE_OFFHOOK");
                        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                            mMediaPlayer.pause();
                        }
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        Log.d(TAG, "PhoneStateListener: CALL_STATE_RINGING: " + incomingNumber);
                        break;
                }
            }
        };

        try {
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopTelephonyListener() {
        if (mTelephonyManager != null && mPhoneStateListener != null) {
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            mTelephonyManager = null;
            mPhoneStateListener = null;
        }
    }

    private void prepare(@NonNull final MusicTrack musicTrack) {
        if (mMediaPlayer == null) {
            mMediaPlayer = new PLMediaPlayer(getApplicationContext(), mAVOptions);
//            mMediaPlayer.setLooping(getIntent().getBooleanExtra("loop", false));
            mMediaPlayer.setOnPreparedListener(mOnPreparedListener);
            mMediaPlayer.setOnCompletionListener(mOnCompletionListener);
            mMediaPlayer.setOnErrorListener(mOnErrorListener);
            mMediaPlayer.setOnInfoListener(mOnInfoListener);
            mMediaPlayer.setOnSeekCompleteListener(mOnSeekCompleteListener);
            mMediaPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        }
        try {
            final String url = StringUtil.isEmpty(musicTrack.mLocalUrl) ? musicTrack.mUrl : musicTrack.mLocalUrl;
            mMediaPlayer.setDataSource(url);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean openFile(final String path) {
        return false;
    }

    public synchronized void open(List<MusicTrack> musicTrackList, int position) {
        boolean isNeedPrepare = false;
        if(position == -1) {
            if(mMusicTrackPlayList.size() == 0) {
                mMusicTrackPlayList.addAll(musicTrackList);
                mMusicTrackRandomPlayList = randomList(mMusicTrackPlayList);
                isNeedPrepare = true;
                mPlayPosition = 0;
            }
        } else {
            if(getCurrentTrack() == null || !getCurrentTrack().mUrl.equals(musicTrackList.get(position).mUrl)) {
                isNeedPrepare = true;
                stop();
            }
            mMusicTrackPlayList.clear();
            mMusicTrackPlayList.addAll(musicTrackList);
            mMusicTrackRandomPlayList = randomList(mMusicTrackPlayList);
            mPlayPosition = position;
            if(mPlayMode == RepeatMode.REPEAT_RANDOM) {
                mPlayPosition = mMusicTrackRandomPlayList.indexOf(mMusicTrackPlayList.get(mPlayPosition));
            }
        }

        if(isNeedPrepare) {
            play();
        }
    }

    public synchronized void play() {
        Log.d(TAG, "play");
        if (isInitialized()) {
            mMediaPlayer.start();
        } else {
            prepare(getCurrentTrack());
        }
        mMusicPlayHandler.post(updateMusicProgress);
    }

    public synchronized void playAt(int position) {
        if(position >= 0 && position < mMusicTrackPlayList.size()) {
            stop();
            mPlayPosition = position;
            if(mPlayMode == RepeatMode.REPEAT_RANDOM) {
                mPlayPosition = mMusicTrackRandomPlayList.indexOf(mMusicTrackPlayList.get(mPlayPosition));
            }
            play();
        }
    }

    public synchronized void pause() {
        Log.d(TAG, "pause");
        if (isInitialized()) {
            mMediaPlayer.pause();
        }
    }

    public synchronized void resume() {
        if (isInitialized()) {
            mMediaPlayer.start();
        }
    }

    public synchronized boolean isPlaying() {
        if (isInitialized()) {
            return mMediaPlayer.isPlaying();
        }
        return false;
    }

    public synchronized void stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }
        mIsInitialized = false;
        mMediaPlayer = null;
        mMusicPlayHandler.removeCallbacks(updateMusicProgress);
    }

    public void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    public synchronized long duration() {
        if (isInitialized()) {
            return mMediaPlayer.getDuration();
        }
        return 0;
    }

    public synchronized long position() {
        if (isInitialized()) {
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public synchronized void seek(final long position) {
        if (isInitialized()) {
            Log.d(TAG, "========position====" + position + ",=====duration======" + duration());
            mMediaPlayer.seekTo(position);
        }
    }

    public synchronized void goToPrevious() {
        if(isInitialized()) {
            List<MusicTrack> realList = getRepeatMode() == RepeatMode.REPEAT_RANDOM ? mMusicTrackRandomPlayList : mMusicTrackPlayList;
            stop();
            mPlayPosition = mPlayPosition - 1 >= 0 ? mPlayPosition - 1: realList.size()-1;
            play();
        }
    }

    public synchronized void goToNext() {
        if(isInitialized()) {
            List<MusicTrack> realList = getRepeatMode() == RepeatMode.REPEAT_RANDOM ? mMusicTrackRandomPlayList : mMusicTrackPlayList;
            stop();
            mPlayPosition = (mPlayPosition + 1)% realList.size();
            play();
        }
    }

    public boolean isInitialized() {
        return mMediaPlayer != null && mIsInitialized;
    }

    public List<MusicTrack> getMusicTrackList() {
        return mMusicTrackPlayList;
    }

    public MusicTrack getCurrentTrack() {
        List<MusicTrack> realList = getRepeatMode() == RepeatMode.REPEAT_RANDOM ? mMusicTrackRandomPlayList : mMusicTrackPlayList;
        if(realList.size() > 0 && mPlayPosition < realList.size()) {
            return realList.get(mPlayPosition);
        }
        return null;
    }

    public int getRepeatMode() {
        return mPlayMode;
    }

    public void setRepeatMode(int repeatMode) {
        if(isInitialized()) {
            if(mPlayMode == RepeatMode.REPEAT_RANDOM && repeatMode != RepeatMode.REPEAT_RANDOM) {
                mPlayPosition = mMusicTrackPlayList.indexOf(mMusicTrackRandomPlayList.get(mPlayPosition));
            }
            if(repeatMode == RepeatMode.REPEAT_RANDOM && mPlayMode != RepeatMode.REPEAT_RANDOM) {
                mPlayPosition = mMusicTrackRandomPlayList.indexOf(mMusicTrackPlayList.get(mPlayPosition));
            }
            mPlayMode = repeatMode;
            if(mPlayMode == RepeatMode.REPEAT_CURRENT) {
                mMediaPlayer.setLooping(true);
            } else {
                mMediaPlayer.setLooping(false);
            }
        }
    }

    private static List<MusicTrack> randomList(final List<MusicTrack> sourceList){
        if (sourceList == null || sourceList.size() == 0) {
            return sourceList;
        }
        List<MusicTrack> sourceListCopy = new ArrayList<>();
        sourceListCopy.addAll(sourceList);
        List<MusicTrack> randomList = new ArrayList<>( sourceListCopy.size( ) );
        do{
            int randomIndex = Math.abs( new Random().nextInt( sourceListCopy.size() ) );
            randomList.add( sourceListCopy.remove( randomIndex ) );
        }while( sourceListCopy.size() > 0 );

        return randomList;
    }

    private static final class MediaServiceStub extends IMediaPlayerAidlInterface.Stub {
        private WeakReference<MediaService> mServiceWeakReference;

        @Override
        public boolean isBinderAlive() {
            return mServiceWeakReference.get() != null;
        }

        private MediaServiceStub(final MediaService service) {
            mServiceWeakReference = new WeakReference<>(service);
        }

        public void setMediaService(final MediaService service) {
            mServiceWeakReference = new WeakReference<>(service);
        }

        @Override
        public void openFile(String path) throws RemoteException {
            if(mServiceWeakReference.get() != null) {
                mServiceWeakReference.get().openFile(path);
            }
        }

        @Override
        public void open(Map info, long[] list, int position) throws RemoteException {

        }

        @Override
        public void stop() throws RemoteException {

        }

        @Override
        public void pause() throws RemoteException {
            if(mServiceWeakReference.get() != null) {
                mServiceWeakReference.get().pause();
            }
        }

        @Override
        public void play() throws RemoteException {
            if(mServiceWeakReference.get() != null) {
                mServiceWeakReference.get().play();
            }
        }

        @Override
        public void playAt(int position) throws RemoteException {
            if(mServiceWeakReference.get() != null) {
                mServiceWeakReference.get().playAt(position);
            }
        }

        @Override
        public void prev(boolean forcePrevious) throws RemoteException {
            if(mServiceWeakReference.get() != null) {
                mServiceWeakReference.get().goToPrevious();
            }
        }

        @Override
        public void next() throws RemoteException {
            if(mServiceWeakReference.get() != null) {
                mServiceWeakReference.get().goToNext();
            }
        }

        @Override
        public void enqueue(long[] list, Map infos, int action) throws RemoteException {

        }

        @Override
        public Map getPlayInfo() throws RemoteException {
            return null;
        }

        @Override
        public void setQueuePosition(int index) throws RemoteException {

        }

        @Override
        public void setShuffleMode(int shuffleMode) throws RemoteException {

        }

        @Override
        public void setRepeatMode(int repeatMode) throws RemoteException {
            if(mServiceWeakReference.get() != null) {
                mServiceWeakReference.get().setRepeatMode(repeatMode);
            }
        }

        @Override
        public void moveQueueItem(int from, int to) throws RemoteException {

        }

        @Override
        public void refresh() throws RemoteException {

        }

        @Override
        public void playListChanged() throws RemoteException {

        }

        @Override
        public boolean isPlaying() throws RemoteException {
            if(mServiceWeakReference.get() != null) {
                return mServiceWeakReference.get().isPlaying();
            }
            return false;
        }

        @Override
        public long[] getQueue() throws RemoteException {
            return new long[0];
        }

        @Override
        public long getQueueItemAtPosition(int position) throws RemoteException {
            return 0;
        }

        @Override
        public int getQueueSize() throws RemoteException {
            return 0;
        }

        @Override
        public int getQueuePosition() throws RemoteException {
            return 0;
        }

        @Override
        public int getQueueHistoryPosition(int position) throws RemoteException {
            return 0;
        }

        @Override
        public int getQueueHistorySize() throws RemoteException {
            return 0;
        }

        @Override
        public int[] getQueueHistoryList() throws RemoteException {
            return new int[0];
        }

        @Override
        public long duration() throws RemoteException {
            if(mServiceWeakReference.get() != null) {
                return mServiceWeakReference.get().duration();
            }
            return 0;
        }

        @Override
        public long position() throws RemoteException {
            if(mServiceWeakReference.get() != null) {
                return mServiceWeakReference.get().position();
            }
            return 0;
        }

        @Override
        public int secondPosition() throws RemoteException {
            return 0;
        }

        @Override
        public long seek(long pos) throws RemoteException {
            if(mServiceWeakReference.get() != null) {
                mServiceWeakReference.get().seek(pos);
            }
            return 0;
        }

        @Override
        public void seekRelative(long deltaInMs) throws RemoteException {

        }

        @Override
        public long getAudioId() throws RemoteException {
            return 0;
        }

        @Override
        public MusicTrack getCurrentTrack() throws RemoteException {
            if(mServiceWeakReference.get() != null) {
                return mServiceWeakReference.get().getCurrentTrack();
            }
            return new MusicTrack();
        }

        @Override
        public List<MusicTrack> getTrackList() throws RemoteException {
            if(mServiceWeakReference.get() != null) {
                return mServiceWeakReference.get().getMusicTrackList();
            }
            return new ArrayList<>();
        }

        @Override
        public MusicTrack getTrack(int index) throws RemoteException {
            return null;
        }

        @Override
        public long getNextAudioId() throws RemoteException {
            return 0;
        }

        @Override
        public long getPreviousAudioId() throws RemoteException {
            return 0;
        }

        @Override
        public long getArtistId() throws RemoteException {
            return 0;
        }

        @Override
        public long getAlbumId() throws RemoteException {
            return 0;
        }

        @Override
        public String getArtistName() throws RemoteException {
            return null;
        }

        @Override
        public String getTrackName() throws RemoteException {
            return null;
        }

        @Override
        public boolean isInitialized() throws RemoteException {
            if(mServiceWeakReference.get() != null) {
                return mServiceWeakReference.get().isInitialized();
            }
            return false;
        }

        @Override
        public String getAlbumName() throws RemoteException {
            return null;
        }

        @Override
        public String getAlbumPath() throws RemoteException {
            return null;
        }

        @Override
        public String[] getAlbumPathAll() throws RemoteException {
            return new String[0];
        }

        @Override
        public String getPath() throws RemoteException {
            return null;
        }

        @Override
        public int getShuffleMode() throws RemoteException {
            return 0;
        }

        @Override
        public int removeTracks(int first, int last) throws RemoteException {
            return 0;
        }

        @Override
        public int removeTrack(long id) throws RemoteException {
            return 0;
        }

        @Override
        public boolean removeTrackAtPosition(long id, int position) throws RemoteException {
            return false;
        }

        @Override
        public int getRepeatMode() throws RemoteException {
            if(mServiceWeakReference.get() != null) {
                return mServiceWeakReference.get().getRepeatMode();
            }
            return RepeatMode.REPEAT_NONE;
        }

        @Override
        public int getMediaMountedCount() throws RemoteException {
            return 0;
        }

        @Override
        public int getAudioSessionId() throws RemoteException {
            return 0;
        }

        @Override
        public void setLockScreenAlbumArt(boolean enabled) throws RemoteException {

        }

        @Override
        public void exit() throws RemoteException {

        }

        @Override
        public void timing(int time) throws RemoteException {

        }
    }
}
