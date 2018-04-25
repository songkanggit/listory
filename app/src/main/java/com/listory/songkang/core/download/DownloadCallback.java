package com.listory.songkang.core.download;

/**
 * Created by SouKou on 2017/8/19.
 */

public interface DownloadCallback {
    /**
     *
     * @param fileSize MB
     */
    void onDownloadStart(String fileSize);

    void onDownloadPause();

    void onDownloadResume();

    void onDownloadStop();

    /**
     *
     * @param percentage
     * @param speed KB
     */
    void onDownloadProgressUpdate(int percentage, int speed);
}
