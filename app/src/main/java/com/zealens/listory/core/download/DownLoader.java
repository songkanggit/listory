package com.zealens.listory.core.download;

import android.Manifest;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresPermission;
import android.util.Log;

import com.zealens.listory.bean.SQLDownLoadInfo;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 类功能描述：下载执行类，每一个 DataKeeper对象 代表一个下载任务</br>
 *
 * @author zhuiji7
 * @email 470508081@qq.com
 * @version 1.0
 * </p>
 */
public class DownLoader {
    private static final String TAG = DownLoader.class.getSimpleName();

    private static final int TASK_START = 0;
    private static final int TASK_STOP = 1;
    private static final int TASK_PROGRESS = 2;
    private static final int TASK_ERROR = 3;
    private static final int TASK_SUCCESS = 4;

    /**标识服务器是否支持断点续传*/
    private boolean isSupportBreakpoint = false;
    
    /**用户ID*/
    private String mUserID;
    
    private DataKeeper mDataKeeper;
    private ArrayList<WeakReference<DownLoadListener>> mDownloadListenerList;
    private DownLoadSuccess mDownloadSuccess;
    private SQLDownLoadInfo mSqlDownloadInfo;
    private DownLoadThread mDownloadThread;
    private long mTotalFileSize = 0;//文件总大小
    private long mDownLoadedFileSize = 0;//已经下载的文件的大小
    private int mDownloadRequestTimes = 0;//当前尝试请求的次数
    private int mMaxDownloadRequestTimes = 3;//失败重新请求次数
    /**当前任务的状态 */
    private volatile boolean mIsDownloading = false;
    /**线程池 */
    private ThreadPoolExecutor mThreadPool;
    private DownloadHandler mDownloadHandler;

    /**
     * @param context 上下文
     * @param sqlFileInfo 任务信息对象
     * @param threadPool  线程池
     * @param userId  用户ID
     * @param supportBreak  服务器是否支持断点续传
     * @param isNewTask 标识是新任务还是根据数据库构建的任务
     */
    public DownLoader(Context context, SQLDownLoadInfo sqlFileInfo, ThreadPoolExecutor threadPool,
                      String userId, boolean supportBreak, boolean isNewTask){
        isSupportBreakpoint = supportBreak;
        mThreadPool = threadPool;
        mUserID = userId;
        mTotalFileSize = sqlFileInfo.getFileSize();
        mDownLoadedFileSize = sqlFileInfo.getDownloadSize();
        mDataKeeper = new DataKeeper(context);
        mDownloadListenerList = new ArrayList<>();
        mSqlDownloadInfo = sqlFileInfo;
        mDownloadHandler = new DownloadHandler(this);
        //新建任务，保存任务信息到数据库
        if(isNewTask){
            saveDownloadInfo();
        }
    }
    
    public String getTaskID(){
        return mSqlDownloadInfo.getTaskID();
    }
    
    public void start(){
        if(mDownloadThread == null){
            mDownloadRequestTimes = 0;
            mIsDownloading = true;
            mDownloadHandler.sendEmptyMessage(TASK_START);
            mDownloadThread = new DownLoadThread();
            mThreadPool.execute(mDownloadThread);
        }
    }
    
    public void stop(){
        if(mDownloadThread != null){
            mIsDownloading = false;
            mDownloadThread.stopDownLoad();
            mThreadPool.remove(mDownloadThread);
            mDownloadThread = null;
        }
    }
    
    public void addDownLoadListener(@NonNls DownLoadListener listener){
        WeakReference<DownLoadListener> weakListener = new WeakReference<>(listener);
        if(!mDownloadListenerList.contains(weakListener)) {
            mDownloadListenerList.add(weakListener);
        }
    }
    
    public void deleteDownLoadListener(@NonNls DownLoadListener listener){
        WeakReference<DownLoadListener> weakListener = new WeakReference<>(listener);
        if (mDownloadListenerList.contains(weakListener)) {
            mDownloadListenerList.remove(weakListener);
        }
    }
    
    public void setDownLoadSuccessListener(DownLoadSuccess mDownloadSuccess){
        this.mDownloadSuccess = mDownloadSuccess;
    }
    
    public void destroy(){
        if(mDownloadThread != null){
            mDownloadThread.stopDownLoad();
            mDownloadThread = null;
        }
        mDataKeeper.deleteDownLoadInfo(mUserID, mSqlDownloadInfo.getTaskID());
        File downloadFile = new File(DownLoadManager.generateFileStorageName(true, mSqlDownloadInfo));
        if(downloadFile.exists()){
            downloadFile.delete();
        }
    }
    
    /**当前任务进行的状态 */
    public boolean isDownLoading(){
        return mIsDownloading;
    }
    
    /**
     * (获取当前任务对象) 
     * @return
     */
    public SQLDownLoadInfo getSQLDownLoadInfo(){
        mSqlDownloadInfo.setDownloadSize(mDownLoadedFileSize);
        return mSqlDownloadInfo;
    }
    
    
    /**
     * (设置是否支持断点续传) 
     * @param isSupportBreakpoint
     */
    public void setSupportBreakpoint(boolean isSupportBreakpoint) {
        this.isSupportBreakpoint = isSupportBreakpoint;
    }


    /**
     * 类功能描述：文件下载线程</br>
     */
    class DownLoadThread extends Thread {
        private boolean isDownloading;
        private URL url;
        private RandomAccessFile  localFile;
        private HttpURLConnection urlConn;
        private InputStream inputStream;
        private int progress = -1;
        
        public DownLoadThread(){
            isDownloading = true;
        }
        
        @Override
        public void run() {
            while(mDownloadRequestTimes < mMaxDownloadRequestTimes) { //做3次请求的尝试
                try {
                    if (mDownLoadedFileSize == mTotalFileSize
                            && mTotalFileSize > 0) {
                        mIsDownloading = false;
                        Message msg = new Message();
                        msg.what = TASK_PROGRESS;
                        msg.arg1 = 100;
                        mDownloadHandler.sendMessage(msg);
                        mDownloadRequestTimes = mMaxDownloadRequestTimes;
                        mDownloadThread = null;
                        return;
                    }
                    url = new URL(mSqlDownloadInfo.getUrl());
                    urlConn = (HttpURLConnection) url.openConnection();
                    urlConn.setConnectTimeout(5000);
                    urlConn.setReadTimeout(10000);
                    if (mTotalFileSize < 1) {//第一次下载，初始化
                        openConnection();
                    } else {
                        String tempFilePath = DownLoadManager.generateFileStorageName(true, mSqlDownloadInfo);
                        File temp = new File(tempFilePath);
                        if (temp.exists()) {
                            localFile = new RandomAccessFile(tempFilePath, "rwd");
                            localFile.seek(mDownLoadedFileSize);
                            urlConn.setRequestProperty("Range", "bytes=" + mDownLoadedFileSize + "-");
                        } else {
                            mTotalFileSize = 0;
                            mDownLoadedFileSize = 0;
                            saveDownloadInfo();
                            openConnection();
                        }
                    }
                    inputStream = urlConn.getInputStream();
                    byte[] buffer = new byte[1024 * 4];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1 && isDownloading) {
                        localFile.write(buffer, 0, length);
                        mDownLoadedFileSize += length;
                        final int currentProgress = (int) ((100 * mDownLoadedFileSize) / mTotalFileSize);
                        Message msg = new Message();
                        msg.what = TASK_PROGRESS;
                        msg.arg1 = currentProgress;
                        mDownloadHandler.sendMessage(msg);
                    }
                    //下载完了
                    if (mDownLoadedFileSize == mTotalFileSize) {
                        boolean renameResult = RenameFile();
                        if (renameResult) {
                            mDownloadHandler.sendEmptyMessage(TASK_SUCCESS); //转移文件成功
                        } else {
                            new File(DownLoadManager.generateFileStorageName(true, mSqlDownloadInfo)).delete();
                            mDownloadHandler.sendEmptyMessage(TASK_ERROR);//转移文件失败
                        }
                        //更新数据库任务
                        mDataKeeper.saveOrUpdateDownLoadInfo(mSqlDownloadInfo, DataKeeper.DownloadState.COMPLETE);
                        mDownloadThread = null;
                        mIsDownloading = false;
                    }
                    mDownloadRequestTimes = mMaxDownloadRequestTimes;
                } catch (Exception e) {
                    if (isDownloading) {
                        if (isSupportBreakpoint) {
                            mDownloadRequestTimes++;
                            if (mDownloadRequestTimes >= mMaxDownloadRequestTimes) {
                                if (mTotalFileSize > 0) {
                                    saveDownloadInfo();
                                }
                                mThreadPool.remove(mDownloadThread);
                                mDownloadThread = null;
                                mIsDownloading = false;
                                mDownloadHandler.sendEmptyMessage(TASK_ERROR);
                            }
                        } else {
                            mDownLoadedFileSize = 0;
                            mDownloadRequestTimes = mMaxDownloadRequestTimes;
                            mIsDownloading = false;
                            mDownloadThread = null;
                            mDownloadHandler.sendEmptyMessage(TASK_ERROR);
                        }

                    } else {
                        mDownloadRequestTimes = mMaxDownloadRequestTimes;
                    }
                    e.printStackTrace();
                } finally {
                    try {
                        if (urlConn != null) {
                            urlConn.disconnect();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        if (localFile != null) {
                            localFile.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        public void stopDownLoad(){
            isDownloading = false;
            mDownloadRequestTimes = mMaxDownloadRequestTimes;
            if(mTotalFileSize > 0){
                saveDownloadInfo();
            }
            mDownloadHandler.sendEmptyMessage(TASK_STOP);
        }

        @RequiresPermission(allOf = {Manifest.permission.READ_EXTERNAL_STORAGE})
        private void openConnection() throws Exception{
            long urlFileSize = urlConn.getContentLength();
            if(urlFileSize > 0){
                isFolderExist();
                localFile = new RandomAccessFile (DownLoadManager.generateFileStorageName(true, mSqlDownloadInfo),"rwd");
                localFile.setLength(urlFileSize);
                mSqlDownloadInfo.setFileSize(urlFileSize);
                mTotalFileSize = urlFileSize;
                if(isDownloading){
                    saveDownloadInfo();
                }
            }
        }
    }

    /**
     * (判断文件夹是否存在，不存在则创建) 
     * @return
     */
    private boolean isFolderExist() {
        boolean result = false;
        try{
            String filepath = FileHelper.getTempDirPath();
            File file = new File(filepath);
            if(!file.exists()) {
                if(file.getParentFile().exists()) {
                    file.delete();
                }
                if(file.mkdirs()) {
                    result = true;
                }
            } else {
                result = true;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    
    /**
     * (保存下载信息至数据库) 
     */
    private void saveDownloadInfo(){
        if(isSupportBreakpoint){
            mSqlDownloadInfo.setDownloadSize(mDownLoadedFileSize);
            mDataKeeper.saveOrUpdateDownLoadInfo(mSqlDownloadInfo, DataKeeper.DownloadState.BEGIN);
        }
    }
    
    /**
     * (通知监听器，任务已开始下载) 
     */
    private void startNotice(){
        for(WeakReference<DownLoadListener> listener:mDownloadListenerList) {
            if(listener.get() != null) {
                listener.get().onStart(getSQLDownLoadInfo());
            }
        }
    }
    
    /**
     * (通知监听器，当前任务进度) 
     */
    private void onProgressNotice(final int progress){
        for(WeakReference<DownLoadListener> listener:mDownloadListenerList) {
            if(listener.get() != null)
            listener.get().onProgress(getSQLDownLoadInfo(), isSupportBreakpoint, progress);
        }
    }
    
    /**
     * (通知监听器，当前任务已停止) 
     */
    private void stopNotice(){
        if(!isSupportBreakpoint){
            mDownLoadedFileSize = 0;
        }
        for(WeakReference<DownLoadListener> listener:mDownloadListenerList) {
            if(listener.get() != null)
            listener.get().onStop(getSQLDownLoadInfo(), isSupportBreakpoint);
        }
    }
    
    /**
     * (通知监听器，当前任务异常，并进入停止状态) 
     */
    private void errorNotice(){
        for(WeakReference<DownLoadListener> listener:mDownloadListenerList) {
            if(listener.get() != null)
            listener.get().onError(getSQLDownLoadInfo());
        }
    }
    
    /**
     * (通知监听器，当前任务成功执行完毕) 
     */
    private void successNotice(){
        for(WeakReference<DownLoadListener> listener:mDownloadListenerList) {
            if(listener.get() != null)
            listener.get().onSuccess(getSQLDownLoadInfo());
        }
        if(mDownloadSuccess != null){
            Log.d(TAG, "successNotice");
            mDownloadSuccess.onTaskSuccess(mSqlDownloadInfo.getTaskID());
        }
    }
    
    /**
     * 类功能描述：该接口用于在任务执行完之后通知下载管理器,以便下载管理器将已完成的任务移出任务列表</br>
     */
    public interface DownLoadSuccess {
        void onTaskSuccess(String TaskID);
    }
    
    public boolean RenameFile(){
        File newFile = new File(mSqlDownloadInfo.getFilePath());
        if(newFile.exists()){
            newFile.delete();
        }
        String tempFilePath = DownLoadManager.generateFileStorageName(true, mSqlDownloadInfo);
        File oleFile = new File(tempFilePath);
        
        String filepath = mSqlDownloadInfo.getFilePath();

        filepath = filepath.substring(0, filepath.lastIndexOf("/"));
        File file = new File(filepath);
        if(!file.exists()){
            file.mkdirs();
        }
        return oleFile.renameTo(newFile);
    }

    private static final class DownloadHandler extends Handler {
        private WeakReference<DownLoader> service;

        public DownloadHandler(DownLoader downLoader) {
            service = new WeakReference<>(downLoader);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TASK_START:
                    if(service.get() != null)
                    service.get().startNotice();
                    break;
                case TASK_STOP:
                    if(service.get() != null)
                    service.get().stopNotice();
                    break;
                case TASK_PROGRESS:
                    if(service.get() != null) {
                        final int progress = msg.arg1;
                        service.get().onProgressNotice(progress);
                    }
                    break;
                case TASK_ERROR:
                    if(service.get() != null)
                    service.get().errorNotice();
                    break;
                case TASK_SUCCESS:
                    if(service.get() != null)
                    service.get().successNotice();
                    break;
            }
        }
    }
 }
