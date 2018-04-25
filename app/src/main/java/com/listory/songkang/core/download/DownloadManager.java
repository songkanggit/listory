package com.listory.songkang.core.download;

import android.os.Environment;
import android.util.Log;

import com.listory.songkang.core.CoreContext;
import com.listory.songkang.core.CoreManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by SouKou on 2017/7/29.
 */
public class DownloadManager extends CoreManager implements DownloadService{
    private static final String TAG = DownloadManager.class.getSimpleName();
    private static final String RETAIN_2_DECIMAL = "%.2f";
    private static final double K_BYTE_SIZE = 1024.0;

    private static final int MAX_READ_DATA_BYTES = 4096;
    private DownloadCallback mDownloadCallback;

    public DownloadManager(CoreContext context) {
        super(context);
    }

    @Override
    public void enqueueTask(String[] urls){

    }

    @Override
    public void startTask(String url, String fileName){
        Runnable downloadTask =  downloadRunnable(url, fileName);
        mCoreContext.executeAsyncTaskOnQueue(downloadTask);
    }

    @Override
    public void stopTask(){

    }

    @Override
    public void pauseTask(){

    }

    @Override
    public void resumeTask() {

    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public void freeMemory() {

    }

    @Override
    public void setDownloadCallback(DownloadCallback downloadCallback){
        mDownloadCallback = downloadCallback;
    }

    private Runnable downloadRunnable(String urlString, String fileName){
        return ()->{
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    int fileLength = connection.getContentLength();
                    if(mDownloadCallback != null) {
                        mDownloadCallback.onDownloadStart(String.format(RETAIN_2_DECIMAL, (double)(fileLength/(K_BYTE_SIZE*K_BYTE_SIZE))));
                    }
                    input = connection.getInputStream();
                    File file = mCoreContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
                    file = new File(file, fileName);
                    if(!file.exists()) {
                        file.createNewFile();
                        output = new FileOutputStream(file);
                        byte data[] = new byte[MAX_READ_DATA_BYTES];
                        long total = 0;
                        long currentTimeMillis = System.currentTimeMillis();
                        int count, size=0;
                        while ((count = input.read(data)) != -1) {
                            total += count;
                            if (fileLength > 0 && mDownloadCallback != null){
                                long timeInterval = System.currentTimeMillis() - currentTimeMillis;
                                if(timeInterval != 0 && timeInterval > 1000) {
                                    mDownloadCallback.onDownloadProgressUpdate((int) (total * 100 / fileLength), (int)((size+count)/(timeInterval)));
                                    currentTimeMillis = System.currentTimeMillis();
                                    size = 0;
                                } else {
                                    size += count;
                                }
                            }
                            output.write(data, 0, count);
                        }
                    }
                } else {
                    Log.d(TAG, "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage());
                }
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {

                }
                if (connection != null)
                    connection.disconnect();
                if(mDownloadCallback != null) {
                    mDownloadCallback.onDownloadStop();
                }
            }
        };
    }
}