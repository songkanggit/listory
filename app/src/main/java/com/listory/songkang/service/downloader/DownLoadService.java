package com.listory.songkang.service.downloader;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * 类功能描述：下载器后台服务</br>
 *
 * @author zhuiji7  (470508081@qq.com)
 * @version 1.0
 * </p>
 */

public class DownLoadService extends Service {
    private static final String TAG = DownLoadService.class.getSimpleName();
    private static DownLoadManager  downLoadManager;
    
    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public static DownLoadManager getDownLoadManager(){
        return downLoadManager;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        if(downLoadManager == null){
            downLoadManager = new DownLoadManager(DownLoadService.this);
        }
        flags = START_FLAG_RETRY;
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        downLoadManager = new DownLoadManager(DownLoadService.this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        //释放downLoadManager
        Intent restartIntent = new Intent(this, DownLoadService.class);
        this.startService(restartIntent);
//        downLoadManager.stopAllTask();
//        downLoadManager = null;
    }
}
