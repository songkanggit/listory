
package com.zealens.listory.core.download;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;


import com.zealens.listory.bean.SQLDownLoadInfo;
import com.zealens.listory.core.BaseCoreManager;
import com.zealens.listory.core.CoreContext;
import com.zealens.listory.service.MusicTrack;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 类功能描述：下载管理类</br>
 *
 * @author zhuiji7
 * @email 470508081@qq.com
 * @version 1.0
 * </p>
 */

public class DownLoadManager extends BaseCoreManager {
    public static final String DEFAULT_USER = "guest";

    private static final String TAG = DownLoadManager.class.getSimpleName();
    private CoreContext mCoreContext;
    private Context mContext;
    private volatile ArrayList<DownLoader> mTaskList = new ArrayList<>();
    private final int MAX_DOWNLOADING_TASK = 5; // 最大同时下载数
    /**服务器是否支持断点续传*/
    private boolean isSupportBreakpoint = false;
    //线程池
    private ThreadPoolExecutor mThreadPool;
    /**用户ID,默认值man*/
    private String mUserId;
    private SharedPreferences sharedPreferences;

    @MagicConstant(intValues = {TaskState.TASK_OK, TaskState.TASK_EXIST, TaskState.TASK_MAX, TaskState.TASK_COMPLETE})
    public @interface TaskState {
        int TASK_OK = 0;
        int TASK_EXIST = 1;
        int TASK_MAX = 2;
        int TASK_COMPLETE = 3;
    }

    public DownLoadManager(CoreContext context) {
        mCoreContext = context;
        mContext = mCoreContext.getBaseContext();
        init();
    }

    @Override
    public int order() {
        return ORDER.NETWORK;
    }

    @Override
    public void freeMemory() {

    }

    private DownLoader.DownLoadSuccess mDownloadSuccessListener = taskId -> {
        synchronized (mTaskList) {
            ArrayList<DownLoader> localList = new ArrayList<>();
            localList.addAll(mTaskList);
            int taskSize = mTaskList.size();
            for (int i = 0; i < taskSize; i++) {
                DownLoader downloader = mTaskList.get(i);
                if (downloader.getTaskID().equals(taskId)) {
                    Log.d(TAG, "DownLoader.DownLoadSuccess");
                    localList.remove(downloader);
                }
            }
            mTaskList.clear();
            mTaskList.addAll(localList);
        }
    };

    private void init() {
        mThreadPool = new ThreadPoolExecutor(
                MAX_DOWNLOADING_TASK, MAX_DOWNLOADING_TASK, 30, TimeUnit.SECONDS, 
                new ArrayBlockingQueue<>(2000));

        sharedPreferences = mContext.getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
        mUserId = sharedPreferences.getString("UserID", DEFAULT_USER);
        recoverData(mContext, mUserId);
    }
    
   
    /**
     * (从数据库恢复下载任务信息) 
     * @param context 上下文
     * @param userId  用户ID
     */
    private void recoverData(Context context, String userId){
        stopAllTask();
        mTaskList = new ArrayList<>();
        DataKeeper datakeeper = new DataKeeper(context);
        ArrayList<SQLDownLoadInfo> sqlDownloadInfoList;
        if(userId == null){
            sqlDownloadInfoList = datakeeper.getDownLoadInfoFailed();
        }else{
            sqlDownloadInfoList = datakeeper.getUserDownLoadInfoFailed(userId);
        }
        if (sqlDownloadInfoList.size() > 0) {
            int listSize = sqlDownloadInfoList.size();
            for (int i = 0; i < listSize; i++) {
                SQLDownLoadInfo sqlDownLoadInfo = sqlDownloadInfoList.get(i);
                DownLoader sqlDownLoader = new DownLoader(context, sqlDownLoadInfo, mThreadPool, userId, isSupportBreakpoint,false);
                sqlDownLoader.setDownLoadSuccessListener(mDownloadSuccessListener);
                sqlDownLoader.start();
                mTaskList.add(sqlDownLoader);
            }
        }
    }
    /*
     * 获取附件状态
     *
     * @param info 下载信息
     * @return  reference TaskState
     */
    @TaskState
    private int getAttachmentState(SQLDownLoadInfo info) {
        if(mTaskList.size() < MAX_DOWNLOADING_TASK) {
            int taskSize = mTaskList.size();
            for (int i = 0; i < taskSize; i++) {
                DownLoader downloader = mTaskList.get(i);
                if (downloader.getTaskID().equals(info.getTaskID())) {
                    return TaskState.TASK_EXIST;
                }
            }
            List<SQLDownLoadInfo> downLoadInfoList = getUserDownloadInfo(mUserId, info.getTaskID());
            File file = new File(info.getFilePath());
            if(downLoadInfoList.size() > 0 && file.exists()) {
                return TaskState.TASK_COMPLETE;
            } else if(file.exists()) {
                 file.delete();
            }
            return TaskState.TASK_OK;
        } else {
            return TaskState.TASK_MAX;
        }
    }

    /**
     * 根据附件id获取下载器
     */
    private DownLoader getDownloader(String taskID) {
        for (int i = 0; i < mTaskList.size(); i++) {
            DownLoader downloader = mTaskList.get(i);
            if (taskID != null && taskID.equals(downloader.getTaskID())) {
                return downloader;
            }
        }
        return null;
    }

    /**
     * 获取已下载信息列表
     * @param userId
     * @return
     */
    public ArrayList<SQLDownLoadInfo> getUserDownloadInfoList(@NotNull String userId) {
        DataKeeper datakeeper = new DataKeeper(mContext);
        return datakeeper.getUserDownLoadInfo(userId);
    }

    /**
     * 获取指定下载内容信息
     * @param userId
     * @return
     */
    public ArrayList<SQLDownLoadInfo> getUserDownloadInfo(@NotNull String userId, final String melodyId) {
        DataKeeper datakeeper = new DataKeeper(mContext);
        return datakeeper.getUserDownLoadInfo(userId, melodyId);
    }

    public void updateSQLDownLoadInfo(SQLDownLoadInfo info) {
        DataKeeper datakeeper = new DataKeeper(mContext);
        datakeeper.saveOrUpdateDownLoadInfo(info, DataKeeper.DownloadState.COMPLETE);
    }

    /**
     * 删除用户指定的下载曲目
     * @param userId
     * @param melodyId
     * @return
     */
    public boolean deleteUserDownloadMelody(final String userId, final String melodyId) {
        DataKeeper datakeeper = new DataKeeper(mContext);
        SQLDownLoadInfo info = datakeeper.deleteDownLoadInfo(userId, melodyId);
        if(info != null) {
            File downloadFile = new File(generateFileStorageName(false, info));
            if(downloadFile.exists() && downloadFile.canWrite()) {
                if(downloadFile.delete())
                return true;
            }
        }
        return false;
    }

    /**
     * (设置下载管理是否支持断点续传) 
     * @param isSupportBreakpoint
     */
    public void setSupportBreakpoint(boolean isSupportBreakpoint) {
        if((!this.isSupportBreakpoint) && isSupportBreakpoint){
            int taskSize = mTaskList.size();
            for (int i = 0; i < taskSize; i++) {
                DownLoader downloader = mTaskList.get(i);
                downloader.setSupportBreakpoint(true);
            } 
        }
        this.isSupportBreakpoint = isSupportBreakpoint;
    }
    
    /**
     * (切换用户)
     * @param userId 用户ID
     */
    public void changeUser(final String userId){
        this.mUserId = userId;
        SharedPreferences.Editor editor = sharedPreferences.edit();// 获取编辑器
        editor.putString("UserID", userId);
        editor.commit();// 提交修改
        FileHelper.setUserID(userId);
        recoverData(mContext, userId);
    }

    public String getUserID(){
        return mUserId;
    }

    /**
     * (增加一个任务，默认开始执行下载任务)
     * 
     * @param musicTrack 任务号
     * @return -1 : 文件已存在 ，0 ： 已存在任务列表 ， 1 ： 添加进任务列表
     */
    public int addTask(MusicTrack musicTrack) {
        return addTask(musicTrack, null);
    }

    /**
     * (增加一个任务，默认开始执行下载任务)
     * 
     * @param musicTrack 任务号
     * @param filepath 下载到本地的路径
     * @return
     */
    @TaskState
    public int addTask(MusicTrack musicTrack, String filepath) {

        String taskId = String.valueOf(musicTrack.mId);
        SQLDownLoadInfo downloadInfo = new SQLDownLoadInfo();
        downloadInfo.setUserID(mUserId);
        downloadInfo.setDownloadSize(0);
        downloadInfo.setFileSize(0);
        downloadInfo.setTaskID(taskId);
        downloadInfo.setFileName(musicTrack.mTitle);
        downloadInfo.setIcon(musicTrack.mCoverImageUrl);
        downloadInfo.setAuthor(musicTrack.mArtist);
        downloadInfo.setLike(musicTrack.mFavorite);
        downloadInfo.setUrl(musicTrack.mUrl);
        if (filepath == null) {
            downloadInfo.setFilePath(generateFileStorageName(false, downloadInfo));
        } else {
            downloadInfo.setFilePath(filepath);
        }
        int state = getAttachmentState(downloadInfo);
        if(state == TaskState.TASK_OK) {
            DownLoader taskDownLoader = new DownLoader(mContext, downloadInfo, mThreadPool, mUserId, isSupportBreakpoint,true);
            taskDownLoader.setDownLoadSuccessListener(mDownloadSuccessListener);
            if(isSupportBreakpoint) {
                taskDownLoader.setSupportBreakpoint(true);
            } else {
                taskDownLoader.setSupportBreakpoint(false);
            }
            taskDownLoader.start();
            mTaskList.add(taskDownLoader);
        }
        return state;
    }

    /**
     * (删除一个任务，包括已下载的本地文件)
     * 
     * @param taskID
     */
    public void deleteTask(String taskID) {
        int taskSize = mTaskList.size();
        for (int i = 0; i < taskSize; i++) {
            DownLoader downloader = mTaskList.get(i);
            if (downloader.getTaskID().equals(taskID)) {
                downloader.destroy();
                mTaskList.remove(downloader);
                break;
            }
        }
    }

    /**
     * (获取当前任务列表的所有任务ID)
     * 
     * @return
     */
    public ArrayList<String> getAllTaskID() {
        ArrayList<String> taskIDlist = new ArrayList<String>();
        int listSize = mTaskList.size();
        for (int i = 0; i < listSize; i++) {
            DownLoader downloader = mTaskList.get(i);
            taskIDlist.add(downloader.getTaskID());
        }
        return taskIDlist;
    }

    /**
     * (获取当前任务列表的所有任务，以TaskInfo列表的方式返回)
     * 
     * @return
     */
    public ArrayList<TaskInfo> getAllTask() {
        ArrayList<TaskInfo> taskInfolist = new ArrayList<>();
        int listSize = mTaskList.size();
        for (int i = 0; i < listSize; i++) {
            DownLoader downloader = mTaskList.get(i);
            SQLDownLoadInfo sqldownloadinfo = downloader.getSQLDownLoadInfo();
            TaskInfo taskinfo = new TaskInfo();
            taskinfo.setFileName(sqldownloadinfo.getFileName());
            taskinfo.setOnDownloading(downloader.isDownLoading());
            taskinfo.setTaskID(sqldownloadinfo.getTaskID());
            taskinfo.setFileSize(sqldownloadinfo.getFileSize());
            taskinfo.setDownFileSize(sqldownloadinfo.getDownloadSize());
            taskInfolist.add(taskinfo);
        }
        return taskInfolist;
    }

    /**
     * (根据任务ID开始执行下载任务)
     * 
     * @param taskID
     */
    public void startTask(String taskID) {
        int listSize = mTaskList.size();
        for (int i = 0; i < listSize; i++) {
            DownLoader downloader = mTaskList.get(i);
            if (downloader.getTaskID().equals(taskID)) {
                downloader.start();
                break;
            }
        }
    }

    /**
     * (根据任务ID停止相应的下载任务)
     * 
     * @param taskID
     */
    public void stopTask(String taskID) {
        int listSize = mTaskList.size();
        for (int i = 0; i < listSize; i++) {
            DownLoader downloader = mTaskList.get(i);
            if (downloader.getTaskID().equals(taskID)) {
                downloader.stop();
                break;
            }
        }
    }

    /**
     * (开始当前任务列表里的所有任务)
     */
    public void startAllTask() {
        int listSize = mTaskList.size();
        for (int i = 0; i < listSize; i++) {
            DownLoader downloader = mTaskList.get(i);
            downloader.start();
        }
    }

    /**
     * (停止当前任务列表里的所有任务)
     */
    public void stopAllTask() {
        int listSize = mTaskList.size();
        for (int i = 0; i < listSize; i++) {
            DownLoader downloader = mTaskList.get(i);
            downloader.stop();
        }
    }

    /**
     * (根据任务ID将监听器设置到相对应的下载任务)
     * 
     * @param taskID
     * @param listener
     */
    public void setSingleTaskListener(String taskID, DownLoadListener listener) {
        int listSize = mTaskList.size();
        for (int i = 0; i < listSize; i++) {
            DownLoader downloader = mTaskList.get(i);
            if (downloader.getTaskID().equals(taskID)) {
                downloader.addDownLoadListener(listener);
                break;
            }
        }
    }

    /**
     * (将监听器设置到当前任务列表所有任务)
     * 
     * @param listener
     */
    public void setAllTaskListener(DownLoadListener listener) {
        int listSize = mTaskList.size();
        for (int i = 0; i < listSize; i++) {
            DownLoader downloader = mTaskList.get(i);
            downloader.addDownLoadListener(listener);
        }
    }

    /**
     * (根据任务ID移除相对应的下载任务的监听器)
     * 
     * @param taskID
     */
    public void removeDownLoadListener(String taskID, DownLoadListener listener) {
        DownLoader downLoader = getDownloader(taskID);
        if (downLoader != null) {
            downLoader.deleteDownLoadListener(listener);
        }
    }

    /**
     * (删除监听所有任务的监听器)
     */
    public void removeAllDownLoadListener(DownLoadListener listener) {
        int listSize = mTaskList.size();
        for (int i = 0; i < listSize; i++) {
            DownLoader downloader = mTaskList.get(i);
            downloader.deleteDownLoadListener(listener);
        }
    }

    /**
     * (根据任务号获取当前任务是否正在下载)
     * 
     * @param taskID
     * @return
     */
    public boolean isTaskDownloading(String taskID) {
        DownLoader downLoader = getDownloader(taskID);
        if (downLoader != null) {
            return downLoader.isDownLoading();
        }
        return false;
    }

    /**
     * 根据id获取下载任务列表中某个任务
     */
    public TaskInfo getTaskInfo(String taskID) {
        DownLoader downloader = getDownloader(taskID);
        if (downloader==null) {
            return null;
        }
        SQLDownLoadInfo sqldownloadinfo = downloader.getSQLDownLoadInfo();
        if (sqldownloadinfo==null) {
            return null;
        }
        TaskInfo taskinfo = new TaskInfo();
        taskinfo.setFileName(sqldownloadinfo.getFileName());
        taskinfo.setOnDownloading(downloader.isDownLoading());
        taskinfo.setTaskID(sqldownloadinfo.getTaskID());
        taskinfo.setDownFileSize(sqldownloadinfo.getDownloadSize());
        taskinfo.setFileSize(sqldownloadinfo.getFileSize());
        return taskinfo;
    }

    public static String generateFileStorageName(boolean isTemp, SQLDownLoadInfo sqldownloadinfo) {
        String dir = FileHelper.getFileDefaultPath();
        if(isTemp) {
            dir = FileHelper.getTempDirPath();
        }
        return dir + "/" + sqldownloadinfo.getUserID() + "-" + FileHelper.filterIDChars(sqldownloadinfo.getTaskID());
    }
}
