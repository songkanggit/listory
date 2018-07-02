package com.zealens.listory.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.zealens.listory.IMediaPlayerAidlInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by SouKou on 2018/1/17.
 */

public class MusicPlayer {
    private static final String TAG = MusicPlayer.class.getSimpleName();

    private static IMediaPlayerAidlInterface mService;
    private static MusicPlayer s_Instance = new MusicPlayer();
    private ServiceBinder mServiceConnection;
    private List<ConnectionState> mConnectionCallbacks;
    private volatile boolean isServiceBinded = false;

    private MusicPlayer() {
        mConnectionCallbacks = new ArrayList<>();
    }

    public static MusicPlayer getInstance() {
        return s_Instance;
    }

    public boolean isServiceConnected() {
        return mService != null;
    }

    public synchronized void playUrl(String url) {
        if (!isServiceConnected()) {
            return;
        }
        try {
            mService.openFile(url);
            mService.play();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public synchronized void stop() {
        if (!isServiceConnected()) {
            return;
        }
        try {
            mService.stop();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean isPlaying() {
        if (!isServiceConnected()) {
            return false;
        }
        try {
            return mService.isPlaying();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    public synchronized void play() {
        if (!isServiceConnected()) {
            return;
        }
        try {
            mService.play();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public synchronized void playAt(int position) {
        if (!isServiceConnected()) {
            return;
        }
        try {
            mService.playAt(position);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public synchronized void pause() {
        if (!isServiceConnected()) {
            return;
        }
        try {
            mService.pause();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public synchronized void seek(final long position) {
        if (!isServiceConnected()) {
            return;
        }
        try {
            mService.seek(position);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public synchronized void setRepeatMode(@MediaService.RepeatMode int repeatMode) {
        if (!isServiceConnected()) {
            return;
        }
        try {
            mService.setRepeatMode(repeatMode);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @MediaService.RepeatMode
    public int getRepeatMode() {
        int repeatMode = MediaService.RepeatMode.REPEAT_NONE;
        if (!isServiceConnected()) {
            return repeatMode;
        }
        try {
            repeatMode = mService.getRepeatMode();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return repeatMode;
    }

    public boolean isInitialized() {
        if (!isServiceConnected()) {
            return false;
        }
        try {
            return mService.isInitialized();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean goToPrevious(){
        if (!isServiceConnected()) {
            return false;
        }
        try {
            mService.prev(true);
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean goToNext() {
        if (!isServiceConnected()) {
            return false;
        }
        try {
            mService.next();
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<MusicTrack> getMusicTrackList() {
        if (!isServiceConnected()) {
            return null;
        }
        try {
            return mService.getTrackList();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public MusicTrack getCurrentMusicTrack() {
        if (!isServiceConnected()) {
            return null;
        }
        try {
            return mService.getCurrentTrack();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized void bindMediaService(Context context){
        if(mServiceConnection == null) {
            mServiceConnection = new ServiceBinder();
        }

        Intent intent = new Intent(context, MediaService.class);
        context.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public synchronized void unBindMediaService(Context context){
        if(mServiceConnection != null && isServiceBinded) {
            context.unbindService(mServiceConnection);
        }
        mServiceConnection = null;
    }

    private final class ServiceBinder implements ServiceConnection {

        public ServiceBinder() {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IMediaPlayerAidlInterface.Stub.asInterface(service);
            isServiceBinded = true;
            for(ConnectionState state: mConnectionCallbacks) {
                state.onServiceConnected();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            isServiceBinded = false;
            for(ConnectionState state: mConnectionCallbacks) {
                state.onServiceDisconnected();
            }
        }
    }

    public void addConnectionCallback(ConnectionState callback) {
        if(!mConnectionCallbacks.contains(callback)) {
            mConnectionCallbacks.add(callback);
        }
    }

    public void removeConnectionCallback(ConnectionState callback) {
        if(mConnectionCallbacks.contains(callback)) {
            mConnectionCallbacks.remove(callback);
        }
    }

    public interface ConnectionState {
        void onServiceConnected();
        void onServiceDisconnected();
    }
}
