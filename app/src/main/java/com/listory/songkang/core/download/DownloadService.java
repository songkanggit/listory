package com.listory.songkang.core.download;

/**
 * Created by SouKou on 2017/8/1.
 */

public interface DownloadService {

    void startTask(String url, String fileName);

    void stopTask();

    void pauseTask();

    void resumeTask();

    void enqueueTask(String[] urls);

    void setDownloadCallback(DownloadCallback dc);
}
