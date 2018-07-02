// IMediaPlayerAidlInterface.aidl
package com.zealens.listory;

import com.zealens.listory.service.MusicTrack;

interface IMediaPlayerAidlInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
//    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
//            double aDouble, String aString);
    void openFile(String path);
    void open(in Map info, in long [] list, int position);
    void stop();
    void pause();
    void play();
    void playAt(int position);
    void prev(boolean forcePrevious);
    void next();
    void enqueue(in long [] list,in Map info, int action);
    Map getPlayInfo();
    void setQueuePosition(int index);
    void setShuffleMode(int shuffleMode);
    void setRepeatMode(int repeatMode);
    void moveQueueItem(int from, int to);
    void refresh();
    void playListChanged();
    boolean isPlaying();
    long [] getQueue();
    long getQueueItemAtPosition(int position);
    int getQueueSize();
    int getQueuePosition();
    int getQueueHistoryPosition(int position);
    int getQueueHistorySize();
    int[] getQueueHistoryList();
    long duration();
    long position();
    int secondPosition();
    long seek(long pos);
    void seekRelative(long deltaInMills);
    long getAudioId();
    MusicTrack getCurrentTrack();
    List<MusicTrack> getTrackList();
    MusicTrack getTrack(int index);
    long getNextAudioId();
    long getPreviousAudioId();
    long getArtistId();
    long getAlbumId();
    String getArtistName();
    String getTrackName();
    boolean isInitialized();
    String getAlbumName();
    String getAlbumPath();
    String[] getAlbumPathAll();
    String getPath();
    int getShuffleMode();
    int removeTracks(int first, int last);
    int removeTrack(long id);
    boolean removeTrackAtPosition(long id, int position);
    int getRepeatMode();
    int getMediaMountedCount();
    int getAudioSessionId();
    void setLockScreenAlbumArt(boolean enabled);
    void exit();
    void timing(int time);
}

