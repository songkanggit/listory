package com.listory.songkang.core.download;


import com.listory.songkang.bean.SQLDownLoadInfo;

/**
 * 类功能描述：</br>
 *
 * @author zhuiji7
 * @email 470508081@qq.com
 * @version 1.0
 * </p>
 */
public interface DownLoadListener {
    
    /**
     * (开始下载文件) 
     * @param sqlDownLoadInfo 下载任务对象
     */
    void onStart(SQLDownLoadInfo sqlDownLoadInfo);
    
    /**
     * (文件下载进度情况) 
     * @param sqlDownLoadInfo 下载任务对象
     * @param isSupportBreakpoint 服务器是否支持断点续传
     */
    void onProgress(SQLDownLoadInfo sqlDownLoadInfo, boolean isSupportBreakpoint, int progress);
    
    /**
     * (停止下载完毕) 
     * @param sqlDownLoadInfo 下载任务对象
      * @param isSupportBreakpoint 服务器是否支持断点续传
     */
    void onStop(SQLDownLoadInfo sqlDownLoadInfo, boolean isSupportBreakpoint);
    
    /**
     * (文件下载失败) 
     * @param sqlDownLoadInfo 下载任务对象
     */
    void onError(SQLDownLoadInfo sqlDownLoadInfo);
    
    
    /**
     * (文件下载成功) 
     * @param sqlDownLoadInfo 下载任务对象
     */
    void onSuccess(SQLDownLoadInfo sqlDownLoadInfo);
}
