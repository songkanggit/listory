package com.listory.songkang.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.listory.songkang.IMediaPlayerAidlInterface;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;

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

    public static final String PLAY_ACTION = "com.zealens.storytree.play";
    public static final String PLAY_ACTION_PARAM_LIST = "com.zealens.storytree.play.param_list";
    public static final String PLAY_ACTION_PARAM_POSITION = "com.zealens.storytree.play.param_position";

    public static final String MUSIC_CHANGE_ACTION = "com.zealens.storytree.music_change";
    public static final String MUSIC_CHANGE_ACTION_PARAM = "com.zealens.storytree.play.music_change_param";

    private static final String PAUSE_ACTION = "com.zealens.storytree.pause";
    private static final String NEXT_ACTION = "com.zealens.storytree.next";
    private static final String PREVIOUS_ACTION = "com.zealens.storytree.previous";
    private static final String REPEAT_PLAY_ACTION = "com.zealens.storytree.repeat_play";
    private static final String RANDOM_PLAY_ACTION = "com.zealens.storytree.random_play";

    public static final String BUFFER_UPDATE = "com.zealens.storytree.buffer_update";
    public static final String BUFFER_UPDATE_PARAM_PERCENT = "percent";

    public static final String PLAY_STATE_UPDATE = "com.zealens.storytree.play_state_update";
    public static final String PLAY_STATE_UPDATE_POSITION = "position";
    public static final String PLAY_STATE_UPDATE_DURATION = "duration";

    private IBinder mBinder = new MediaServiceStub(this);
    private AudioManager mAudioManager;
    private MultiPlayer mPlayer;
    private boolean mIsPlaying = false;
    private boolean mIsServiceRunning;

    private HandlerThread mHandlerThread;
    private MusicPlayHandler mMusicPlayHandler;
    private List<MusicTrack> mMusicTrackPlayList = new ArrayList<>();
    private List<MusicTrack> mMusicTrackRandomPlayList = new ArrayList<>();
    private int mPlayPosition;

    @RepeatMode
    private int mRepeatMode = RepeatMode.REPEAT_NONE;
    @MagicConstant(intValues = {RepeatMode.REPEAT_RANDOM, RepeatMode.REPEAT_CURRENT, RepeatMode.REPEAT_NONE})
    public @interface RepeatMode {
        int REPEAT_NONE = 0;
        int REPEAT_CURRENT = 1;
        int REPEAT_RANDOM = 2;
        int REPEAT_ALL = 1;
    }

    @MagicConstant(intValues = {MusicStateChange.POSITION_CHANGED})
    public @interface MusicStateChange {
        int POSITION_CHANGED = 0;
    }


    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleBroadcastIntent(intent);
        }
    };

    private final AudioManager.OnAudioFocusChangeListener mAudioChangeListener = focusChange -> {

    };

    private Runnable updateMusicProgress = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(PLAY_STATE_UPDATE);
            intent.putExtra(PLAY_STATE_UPDATE_POSITION, mPlayer.position());
            intent.putExtra(PLAY_STATE_UPDATE_DURATION, mPlayer.duration());
            sendBroadcast(intent);
            mMusicPlayHandler.postDelayed(this, 1000);
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mIsServiceRunning = true;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        mIsServiceRunning = true;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mIsServiceRunning = false;
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
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mPlayer = new MultiPlayer(this);
        mHandlerThread = new HandlerThread("MusicPlayer", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();
        mMusicPlayHandler = new MusicPlayHandler(this, mHandlerThread.getLooper());
    }

    private void handleBroadcastIntent(Intent intent){
        final String action = intent.getAction();
        switch (action) {
            case PLAY_ACTION:
                List<MusicTrack> musicTrackList = intent.getParcelableArrayListExtra(PLAY_ACTION_PARAM_LIST);
                int position = intent.getIntExtra(PLAY_ACTION_PARAM_POSITION, 0);
                if(position <0 || position > musicTrackList.size()) {
                    position = -1;
                }
                open(musicTrackList, position);
//                play();
                break;
            case PAUSE_ACTION:
                break;
            case REPEAT_PLAY_ACTION:
                break;
            case RANDOM_PLAY_ACTION:
                break;
            case NEXT_ACTION:
                break;
            case PREVIOUS_ACTION:
                break;
        }
    }

    public void sendBroadcastBufferUpdate(final int percent){
        Intent intent = new Intent();
        intent.setAction(BUFFER_UPDATE);
        intent.putExtra(BUFFER_UPDATE_PARAM_PERCENT, percent);
        sendBroadcast(intent);
    }

    public boolean openFile(final String path) {
        Log.d(TAG, "openFile:" + path);
        synchronized (this) {
            mPlayer.setDataSourceExternal(path, null);
            if(mPlayer.isInitialized()) {
                return true;
            }
            return false;
        }
    }

    public void open(List<MusicTrack> musicTrackList, int position) {
        synchronized (this) {
            mMusicTrackPlayList.clear();
            mMusicTrackPlayList.addAll(musicTrackList);
            mMusicTrackRandomPlayList = randomList(mMusicTrackPlayList);
            mPlayPosition = 0;
            if(position != -1) {
                mPlayPosition = position;
            }
            int nextPosition = (mPlayPosition + 1) % mMusicTrackPlayList.size();
            if(mMusicTrackPlayList.size() > 0 && mPlayPosition < mMusicTrackPlayList.size()) {
                mPlayer.setDataSourceExternal(musicTrackList.get(mPlayPosition).mUrl, mMusicTrackPlayList.get(nextPosition).mUrl);
                notifyChange(MusicStateChange.POSITION_CHANGED);
            }
        }
    }

    public void prepareNextMusic() {
        List<MusicTrack> realList = getRepeatMode() == RepeatMode.REPEAT_RANDOM ? mMusicTrackRandomPlayList : mMusicTrackPlayList;
        synchronized (this) {
            mPlayPosition = (++mPlayPosition) % realList.size();
            int nextPosition = (mPlayPosition + 1) % realList.size();
            mPlayer.setNextPlayDataSource(realList.get(nextPosition).mUrl);
        }
    }

    public void play() {
        Log.d(TAG, "play");
        synchronized (this) {
            mIsPlaying = true;
            int status = mAudioManager.requestAudioFocus(mAudioChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if(status != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                if(DEBUG) {
                    Log.d(TAG, "Audio request focus failed.");
                }
                return;
            }
            mPlayer.start();
        }
        mMusicPlayHandler.post(updateMusicProgress);
    }

    public void pause() {
        Log.d(TAG, "play");
        synchronized (this) {
            mIsPlaying = false;
            if(mPlayer.isInitialized()) {
                mPlayer.pause();
            }
        }
    }

    public boolean isPlaying() {
        Log.d(TAG, "mIsPlaying" + mIsPlaying);
        return mIsPlaying;
    }

    public void stop() {
        Log.d(TAG, "stop");
        synchronized (this) {
            if(mPlayer.isInitialized()) {
                mPlayer.stop();
            }
        }
    }

    public long duration() {
        if(mPlayer.isInitialized()) {
            return mPlayer.duration();
        }
        return 0;
    }

    public long position() {
        if(mPlayer.isInitialized()) {
            return mPlayer.position();
        }
        return 0;
    }

    public void seek(final long position) {
        synchronized (this) {
            if(mPlayer.isInitialized()) {
                mPlayer.seek(position);
            }
        }
    }

    public void goToPrevious() {
        List<MusicTrack> realList = getRepeatMode() == RepeatMode.REPEAT_RANDOM ? mMusicTrackRandomPlayList : mMusicTrackPlayList;

        synchronized (this) {
            stop();
            mPlayPosition = --mPlayPosition >= 0 ? mPlayPosition : realList.size()-1;

            int nextPosition = (mPlayPosition + 1) % realList.size();
            mPlayer.setDataSourceExternal(
                    mMusicTrackPlayList.get(mPlayPosition).mUrl, mMusicTrackPlayList.get(nextPosition).mUrl);
            play();
            notifyChange(MusicStateChange.POSITION_CHANGED);
        }
    }

    public void goToNext() {
        synchronized (this) {
            stop();
            List<MusicTrack> realList = getRepeatMode() == RepeatMode.REPEAT_RANDOM ? mMusicTrackRandomPlayList : mMusicTrackPlayList;
            mPlayPosition = ++mPlayPosition % realList.size();

            int nextPosition = (mPlayPosition + 1) % realList.size();
            mPlayer.setDataSourceExternal(
                    mMusicTrackPlayList.get(mPlayPosition).mUrl, mMusicTrackPlayList.get(nextPosition).mUrl);
            play();
            notifyChange(MusicStateChange.POSITION_CHANGED);
        }
    }

    public List<MusicTrack> getMusicTrackList() {
        return mMusicTrackPlayList;
    }

    public MusicTrack getCurrentTrack() {
        if(mMusicTrackPlayList.size() > 0 && mPlayPosition < mMusicTrackPlayList.size()) {
            return mMusicTrackPlayList.get(mPlayPosition);
        }
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mIntentReceiver);
    }

    public int getRepeatMode() {
        return mRepeatMode;
    }

    public void setRepeatMode(int mRepeatMode) {
        this.mRepeatMode = mRepeatMode;
    }

    public MusicPlayHandler getMusicPlayHandler() {
        return mMusicPlayHandler;
    }

    private void notifyChange(@MusicStateChange final int what){
        switch (what) {
            case MusicStateChange.POSITION_CHANGED:
                Intent intent = new Intent(MUSIC_CHANGE_ACTION);
                if(mPlayPosition < mMusicTrackPlayList.size()) {
                    intent.putExtra(MUSIC_CHANGE_ACTION_PARAM, mMusicTrackPlayList.get(mPlayPosition));
                    sendBroadcast(intent);
                }
                break;
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

    private static final class MusicPlayHandler extends Handler {
        public static final int MUSIC_PLAY_COMPLETE = 0;
        private final WeakReference<MediaService> mService;
        public MusicPlayHandler(final MediaService service, final Looper looper) {
            super(looper);
            mService = new WeakReference<MediaService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            final MediaService service = mService.get();
            if (service == null) {
                return;
            }
            synchronized (service) {
                switch (msg.what) {
                    case MUSIC_PLAY_COMPLETE:
                        Log.d(TAG, "MUSIC_PLAY_COMPLETE");
                        if(service.getRepeatMode() == RepeatMode.REPEAT_CURRENT) {
                            service.seek(10);
                        } else {
                            service.prepareNextMusic();
                        }
                        service.play();
                        service.notifyChange(MusicStateChange.POSITION_CHANGED);
                        break;
                }
            }
        }
    }

    private static final class MultiPlayer implements MediaPlayer.OnErrorListener,
            MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnBufferingUpdateListener {
        private final WeakReference<MediaService> mService;
        private MediaPlayer mCurrentPlayer = new MediaPlayer();
        private MediaPlayer mNextPlayer;
        private Handler mHandler = new Handler();

        private boolean mIsCurrentMediaPrepared;
        private boolean mIsNextPlayerPrepared;
        private boolean mIsInitialized;

        private Runnable mStartMediaPlayerIfPrepared = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "mStartMediaPlayerIfPrepared" + mIsCurrentMediaPrepared);
                if(mIsCurrentMediaPrepared) {
                    mIsInitialized = true;
                    if(mService.get().mIsPlaying) {
                        mCurrentPlayer.start();
                    }
                } else {
                    mHandler.postDelayed(mStartMediaPlayerIfPrepared, 500);
                }
            }
        };

        public MultiPlayer(final MediaService service) {
            mService = new WeakReference<>(service);
            mCurrentPlayer.setWakeMode(mService.get(), PowerManager.PARTIAL_WAKE_LOCK);
        }

        public void setDataSourceExternal(@NonNull final String path, final String nextPath) {
            setMediaPlayerDataSource(mCurrentPlayer, path);
            if(nextPath != null) {
                if(mNextPlayer == null) {
                    mNextPlayer = new MediaPlayer();
                }
                setMediaPlayerDataSource(mNextPlayer, nextPath);
            }
        }

        public void setNextPlayDataSource(@NonNull final String path) {
            if(mNextPlayer == null) {
                mNextPlayer = new MediaPlayer();
            }
            setMediaPlayerDataSource(mNextPlayer, path);
        }

        private void setMediaPlayerDataSource(@NotNull MediaPlayer player, final String path){
            if(player == mCurrentPlayer) {
                mIsCurrentMediaPrepared = false;
            } else if(player == mNextPlayer) {
                mIsNextPlayerPrepared = false;
            }
            try {
                player.reset();
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                player.setOnPreparedListener(this);
                player.setOnCompletionListener(this);
                player.setOnErrorListener(this);
                player.setOnBufferingUpdateListener(this);
                player.setDataSource(path);
                player.prepare();
            } catch (final IOException e) {
                if(player == mCurrentPlayer) {
                    mIsInitialized = false;
                }
            } catch (final IllegalArgumentException todo) {
                if(player == mCurrentPlayer) {
                    mIsInitialized = false;
                }
            }
        }

        public void start() {
            mHandler.postDelayed(mStartMediaPlayerIfPrepared, 100);
        }

        public void stop() {
            mHandler.removeCallbacks(mStartMediaPlayerIfPrepared);
            mCurrentPlayer.reset();
            mIsInitialized = false;
            mIsCurrentMediaPrepared = false;
            mService.get().stop();
        }

        public void release() {
            mCurrentPlayer.release();
        }

        public void pause() {
            mHandler.removeCallbacks(mStartMediaPlayerIfPrepared);
            mCurrentPlayer.pause();
        }

        public int duration() {
            if(mIsCurrentMediaPrepared) {
                return mCurrentPlayer.getDuration();
            }
            return -1;
        }

        public void seek(final long position) {
            if(mIsCurrentMediaPrepared) {
                mCurrentPlayer.seekTo((int)position);
            }
        }

        public long position() {
            if(mIsCurrentMediaPrepared) {
                try {
                    return mCurrentPlayer.getCurrentPosition();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return 0;
        }

        public boolean isInitialized() {
            return mIsInitialized;
        }

        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            if(mp == mCurrentPlayer) {
                Log.d(TAG, "percent:" + percent);
                mService.get().sendBroadcastBufferUpdate(percent);
            }
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            Log.d(TAG, "onPrepared");
            if(mp == mCurrentPlayer) {
                mIsCurrentMediaPrepared = true;
            } else if (mp == mNextPlayer) {
                mIsNextPlayerPrepared = true;
            }
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            Log.d(TAG, "onCompletion");
            if(mp == mCurrentPlayer && mNextPlayer != null) {
                mCurrentPlayer.release();
                if(mIsNextPlayerPrepared) {
                    mCurrentPlayer = mNextPlayer;
                    mNextPlayer = null;
                    mIsNextPlayerPrepared = false;
                }
            }
            mService.get().getMusicPlayHandler().sendEmptyMessage(MusicPlayHandler.MUSIC_PLAY_COMPLETE);
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.d(TAG, "what:" + what + ",extra:" + extra);
            switch (what) {
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                    final MediaService service = mService.get();
                    mIsInitialized = false;
                    mCurrentPlayer.release();
                    mCurrentPlayer = new MediaPlayer();
                    mCurrentPlayer.setWakeMode(service, PowerManager.PARTIAL_WAKE_LOCK);
                    mHandler.removeCallbacks(mStartMediaPlayerIfPrepared);
                    return true;
                default:
                    break;
            }
            return false;
        }
    }

    private static final class MediaServiceStub extends IMediaPlayerAidlInterface.Stub {
        private final WeakReference<MediaService> mServiceWeakReference;

        private MediaServiceStub(final MediaService service) {
            mServiceWeakReference = new WeakReference<>(service);
        }

        @Override
        public void openFile(String path) throws RemoteException {
            mServiceWeakReference.get().openFile(path);
        }

        @Override
        public void open(Map info, long[] list, int position) throws RemoteException {

        }

        @Override
        public void stop() throws RemoteException {

        }

        @Override
        public void pause() throws RemoteException {
            mServiceWeakReference.get().pause();
        }

        @Override
        public void play() throws RemoteException {
            mServiceWeakReference.get().play();
        }

        @Override
        public void prev(boolean forcePrevious) throws RemoteException {
            mServiceWeakReference.get().goToPrevious();
        }

        @Override
        public void next() throws RemoteException {
            mServiceWeakReference.get().goToNext();
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
            mServiceWeakReference.get().setRepeatMode(repeatMode);
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
            return mServiceWeakReference.get().isPlaying();
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
            return mServiceWeakReference.get().duration();
        }

        @Override
        public long position() throws RemoteException {
            return mServiceWeakReference.get().position();
        }

        @Override
        public int secondPosition() throws RemoteException {
            return 0;
        }

        @Override
        public long seek(long pos) throws RemoteException {
            mServiceWeakReference.get().seek(pos);
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
            return mServiceWeakReference.get().getCurrentTrack();
        }

        @Override
        public List<MusicTrack> getTrackList() throws RemoteException {
            return mServiceWeakReference.get().getMusicTrackList();
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
        public boolean isTrackLocal() throws RemoteException {
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
            return mServiceWeakReference.get().getRepeatMode();
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
