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
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.listory.songkang.IMediaPlayerAidlInterface;
import com.listory.songkang.utils.StringUtil;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

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
    private AudioManager mAudioManager;
    private MultiPlayer mPlayer;
    private boolean mIsPlaying = false;
    private boolean mIsServiceRunning;

    private HandlerThread mHandlerThread;
    private Handler mMusicPlayHandler;
    private List<MusicTrack> mMusicTrackPlayList = new ArrayList<>();
    private List<MusicTrack> mMusicTrackRandomPlayList = new ArrayList<>();
    private int mPlayPosition;

    @RepeatMode
    private int mRepeatMode = RepeatMode.REPEAT_ALL;
    @MagicConstant(intValues = {RepeatMode.REPEAT_CURRENT, RepeatMode.REPEAT_RANDOM, RepeatMode.REPEAT_ALL, RepeatMode.REPEAT_NONE})
    public @interface RepeatMode {
        int REPEAT_CURRENT = 0;
        int REPEAT_RANDOM = 1;
        int REPEAT_ALL = 2;
        int REPEAT_NONE = 3;
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
            intent.putExtra(PLAY_STATE_UPDATE_DATA, getCurrentTrack());
            sendBroadcast(intent);
            mMusicPlayHandler.postDelayed(this, 1000);
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mIsServiceRunning = true;
        if(mBinder == null || !mBinder.isBinderAlive()) {
            mBinder = new MediaServiceStub(this);
        }
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
        mMusicPlayHandler = new Handler(mHandlerThread.getLooper());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mIntentReceiver);
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

    public synchronized boolean openFile(final String path) {
        Log.d(TAG, "openFile:" + path);
        MusicTrack musicTrack = new MusicTrack();
        musicTrack.mUrl = path;
        mPlayer.setDataSourceExternal(musicTrack, null);
        if(mPlayer.isInitialized()) {
            return true;
        }
        return false;
    }

    public synchronized void open(List<MusicTrack> musicTrackList, int position) {
        boolean needPlayNewMusic = false;
        if(position == -1) {
            if(mMusicTrackPlayList.size() == 0) {
                mMusicTrackPlayList.clear();
                mMusicTrackPlayList.addAll(musicTrackList);
                needPlayNewMusic = true;
            }
        } else {
            if(musicTrackList.size() == mMusicTrackPlayList.size()) {
                boolean isUpdateContent = false;
                boolean isSameMelody = false;
                if(getCurrentTrack().mUrl.equals(musicTrackList.get(position).mUrl)) {
                    isSameMelody = true;
                }
                for(int i=0; i<musicTrackList.size(); i++) {
                    if(!musicTrackList.get(i).equals(mMusicTrackPlayList.get(i))) {
                        isUpdateContent = true;
                        mMusicTrackPlayList.clear();
                        mMusicTrackPlayList.addAll(musicTrackList);
                        break;
                    }
                }
                if(isUpdateContent || !isSameMelody) {
                    needPlayNewMusic = true;
                }
            } else {
                mMusicTrackPlayList.clear();
                mMusicTrackPlayList.addAll(musicTrackList);
                needPlayNewMusic = true;
            }
        }

        if(needPlayNewMusic) {
            mMusicTrackRandomPlayList = randomList(mMusicTrackPlayList);
            mPlayPosition = 0;
            if(position != -1) {
                mPlayPosition = position;
            }

            List<MusicTrack> realList = getRepeatMode() == RepeatMode.REPEAT_RANDOM ? mMusicTrackRandomPlayList : mMusicTrackPlayList;
            if(mRepeatMode == RepeatMode.REPEAT_RANDOM) {
                mPlayPosition = mMusicTrackRandomPlayList.indexOf(mMusicTrackPlayList.get(mPlayPosition));
            }
            int nextPosition = (mPlayPosition + 1) % realList.size();
            if(realList.size() > 0 && mPlayPosition < realList.size()) {
                mPlayer.setDataSourceExternal(realList.get(mPlayPosition), realList.get(nextPosition));
            }
            play();
        }
    }

    public synchronized void prepareNextMusic() {
        List<MusicTrack> realList = getRepeatMode() == RepeatMode.REPEAT_RANDOM ? mMusicTrackRandomPlayList : mMusicTrackPlayList;
        mPlayPosition = (mPlayPosition + 1) % realList.size();
        int nextPosition = (mPlayPosition + 1) % realList.size();
        mPlayer.setNextPlayDataSource(realList.get(nextPosition).mUrl);
    }

    public synchronized void play() {
        Log.d(TAG, "play");
        mIsPlaying = true;
        int status = mAudioManager.requestAudioFocus(mAudioChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if(status != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if(DEBUG) {
                Log.d(TAG, "Audio request focus failed.");
            }
            return;
        }
        mPlayer.start();
        mMusicPlayHandler.post(updateMusicProgress);
    }

    public synchronized void playAt(int position) {
        List<MusicTrack> realList = getRepeatMode() == RepeatMode.REPEAT_RANDOM ? mMusicTrackRandomPlayList : mMusicTrackPlayList;
        if(position >= 0 && position < realList.size()) {
            stop();
            mPlayPosition = position;
            if(mRepeatMode == RepeatMode.REPEAT_RANDOM) {
                mPlayPosition = mMusicTrackRandomPlayList.indexOf(mMusicTrackPlayList.get(mPlayPosition));
            }
            int nextPosition = (mPlayPosition + 1) % realList.size();
            mPlayer.setDataSourceExternal(
                    realList.get(mPlayPosition), realList.get(nextPosition));
            play();
        }
    }

    public synchronized void pause() {
        Log.d(TAG, "pause");
        mIsPlaying = false;
        if(mPlayer.isInitialized()) {
            mPlayer.pause();
        }
    }

    public synchronized boolean isPlaying() {
        return mIsPlaying;
    }

    public synchronized void stop() {
        synchronized (this) {
            Log.d(TAG, "stop");
            if(mPlayer.isInitialized()) {
                mPlayer.stop();
            }
        }
        mMusicPlayHandler.removeCallbacks(updateMusicProgress);
    }

    public synchronized long duration() {
        if(mPlayer.isInitialized()) {
            return mPlayer.duration();
        }
        return 0;
    }

    public synchronized long position() {
        if(mPlayer.isInitialized()) {
            return mPlayer.position();
        }
        return 0;
    }

    public synchronized void seek(final long position) {
        synchronized (this) {
            if(mPlayer.isInitialized()) {
                mPlayer.seek(position);
            }
        }
    }

    public synchronized void goToPrevious() {
        List<MusicTrack> realList = getRepeatMode() == RepeatMode.REPEAT_RANDOM ? mMusicTrackRandomPlayList : mMusicTrackPlayList;
        stop();
        mPlayPosition = mPlayPosition - 1 >= 0 ? mPlayPosition - 1: realList.size()-1;
        int nextPosition = (mPlayPosition + 1) % realList.size();
        mPlayer.setDataSourceExternal(realList.get(mPlayPosition), realList.get(nextPosition));
        play();
    }

    public synchronized void goToNext() {
        stop();
        List<MusicTrack> realList = getRepeatMode() == RepeatMode.REPEAT_RANDOM ? mMusicTrackRandomPlayList : mMusicTrackPlayList;
        mPlayPosition = (mPlayPosition + 1)% realList.size();
        int nextPosition = (mPlayPosition + 1) % realList.size();
        mPlayer.setDataSourceExternal(realList.get(mPlayPosition), realList.get(nextPosition));
        play();
    }

    public boolean isInitialized() {
        return mPlayer.isInitialized();
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
        return mRepeatMode;
    }

    public void setRepeatMode(int repeatMode) {
        if(mRepeatMode == RepeatMode.REPEAT_RANDOM && repeatMode != RepeatMode.REPEAT_RANDOM) {
            mPlayPosition = mMusicTrackPlayList.indexOf(mMusicTrackRandomPlayList.get(mPlayPosition));
        }
        if(repeatMode == RepeatMode.REPEAT_RANDOM && mRepeatMode != RepeatMode.REPEAT_RANDOM) {
            mPlayPosition = mMusicTrackRandomPlayList.indexOf(mMusicTrackPlayList.get(mPlayPosition));
        }
        this.mRepeatMode = repeatMode;
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

    private static final class MultiPlayer implements IjkMediaPlayer.OnErrorListener,
            IjkMediaPlayer.OnCompletionListener, IjkMediaPlayer.OnPreparedListener, IjkMediaPlayer.OnBufferingUpdateListener {
        private final WeakReference<MediaService> mService;
        private IjkMediaPlayer mCurrentPlayer = new IjkMediaPlayer();
        private IjkMediaPlayer mNextPlayer = new IjkMediaPlayer();
        private Handler mHandler = new Handler();

        private boolean mIsCurrentMediaPrepared;
        private boolean mIsNextPlayerPrepared;
        private volatile boolean mIsInitialized;

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
        }

        public void setDataSourceExternal(@NonNull final MusicTrack current, final MusicTrack next) {
            String currentPath = current.mUrl;
            if(!StringUtil.isEmpty(current.mLocalUrl)) {
                currentPath = current.mLocalUrl;
            }

            String nextPath = "";
            if(next != null) {
                nextPath = next.mUrl;
                if(!StringUtil.isEmpty(next.mLocalUrl)) {
                    nextPath = next.mLocalUrl;
                }
            }

            boolean prepareCurrent = !(mCurrentPlayer.getDataSource() != null && mCurrentPlayer.getDataSource().equals(currentPath));
            boolean prepareNext = !(mNextPlayer.getDataSource() != null && mNextPlayer.getDataSource().equals(nextPath));
            if(!StringUtil.isEmpty(mNextPlayer.getDataSource()) && mNextPlayer.getDataSource().equals(currentPath) && mIsNextPlayerPrepared) {
                //下一首
                Log.d(TAG, "==========下一首===========");
                mCurrentPlayer.stop();
                mCurrentPlayer.release();
                mCurrentPlayer = mNextPlayer;
                mNextPlayer = new IjkMediaPlayer();
                prepareCurrent = false;
                prepareNext = true;
                mIsCurrentMediaPrepared = true;
            } else if(!StringUtil.isEmpty(mCurrentPlayer.getDataSource()) && mCurrentPlayer.getDataSource().equals(nextPath) && mIsCurrentMediaPrepared) {
                //上一首
                Log.d(TAG, "==========上一首===========");
                mNextPlayer.stop();
                mNextPlayer.release();
                mNextPlayer = mCurrentPlayer;
                mNextPlayer.pause();
                mCurrentPlayer = new IjkMediaPlayer();
                prepareNext = false;
                prepareCurrent = true;
                mIsNextPlayerPrepared = true;
            }

            if(prepareCurrent) {
                setMediaPlayerDataSource(mCurrentPlayer, currentPath);
            }
            if(!StringUtil.isEmpty(nextPath) && prepareNext) {
                setMediaPlayerDataSource(mNextPlayer, nextPath);
            }
        }

        public void setNextPlayDataSource(@NonNull final String path) {
            setMediaPlayerDataSource(mNextPlayer, path);
        }

        private void setMediaPlayerDataSource(@NotNull IjkMediaPlayer player, final String path){
            player.setWakeMode(mService.get(), PowerManager.PARTIAL_WAKE_LOCK);
            if(player == mCurrentPlayer) {
                mIsInitialized = false;
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
                player.prepareAsync();
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
            mCurrentPlayer.stop();
            mIsInitialized = false;
            mIsCurrentMediaPrepared = false;
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
                return (int)mCurrentPlayer.getDuration();
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
        public void onPrepared(IMediaPlayer mp) {
            Log.d(TAG, "onPrepared");
            if(mp == mCurrentPlayer) {
                mIsCurrentMediaPrepared = true;
            } else if (mp == mNextPlayer) {
                mIsNextPlayerPrepared = true;
                mNextPlayer.pause();
            }
        }

        @Override
        public void onCompletion(IMediaPlayer mp) {
            Log.d(TAG, "onCompletion");
            if(mp == mCurrentPlayer && mService.get().getRepeatMode() == RepeatMode.REPEAT_CURRENT) {
                mCurrentPlayer.seekTo(0);
                mCurrentPlayer.start();
            }
            if(mp == mCurrentPlayer && mService.get().getRepeatMode() != RepeatMode.REPEAT_CURRENT
                    && mIsNextPlayerPrepared) {
                mCurrentPlayer.stop();
                mCurrentPlayer.release();
                mCurrentPlayer = mNextPlayer;
                mNextPlayer = new IjkMediaPlayer();
                mIsNextPlayerPrepared = false;
                start();
                mService.get().prepareNextMusic();
            }
        }

        @Override
        public void onBufferingUpdate(IMediaPlayer mp, int percent) {
            if(mp == mCurrentPlayer) {
                mService.get().sendBroadcastBufferUpdate(percent);
            }
        }

        @Override
        public boolean onError(IMediaPlayer mp, int what, int extra) {
            Log.d(TAG, "what:" + what + ",extra:" + extra);
            switch (what) {
                case IjkMediaPlayer.MEDIA_ERROR_SERVER_DIED:
                    final MediaService service = mService.get();
                    mIsInitialized = false;
                    mCurrentPlayer.release();
                    mCurrentPlayer = new IjkMediaPlayer();
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
