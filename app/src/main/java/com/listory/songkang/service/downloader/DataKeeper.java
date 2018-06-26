package com.listory.songkang.service.downloader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.listory.songkang.bean.SQLDownLoadInfo;

import org.intellij.lang.annotations.MagicConstant;

import java.util.ArrayList;

/**
 * 类功能描述：信息存储类，主要在任务下载各个环节执行数据的存储</br>
 *
 * @author zhuiji7
 * @email 470508081@qq.com
 * @version 1.0
 * </p>
 */

public class DataKeeper {

    @MagicConstant(stringValues = {DownloadState.BEGIN, DownloadState.DOWNLOADING, DownloadState.COMPLETE})
    public @interface DownloadState {
        String BEGIN = "0";
        String DOWNLOADING = "1";
        String COMPLETE = "2";
    }
    private SQLiteHelper mDbHelper;
    private SQLiteDatabase db;

    public DataKeeper(Context context){
        this.mDbHelper = new SQLiteHelper(context);
    }

    /**
     * (保存一个任务的下载信息到数据库)
     * @param downloadInfo
     */
    public void saveOrUpdateDownLoadInfo(SQLDownLoadInfo downloadInfo, @DownloadState String state){
        ContentValues cv = new ContentValues();
        cv.put("userID", downloadInfo.getUserID());
        cv.put("taskID", downloadInfo.getTaskID());
        cv.put("downLoadSize", downloadInfo.getDownloadSize());
        cv.put("fileName", downloadInfo.getFileName());
        cv.put("filePath", downloadInfo.getFilePath());
        cv.put("author", downloadInfo.getAuthor());
        cv.put("like", downloadInfo.getLike());
        cv.put("icon", downloadInfo.getIcon());
        cv.put("fileSize", downloadInfo.getFileSize());
        cv.put("url", downloadInfo.getUrl());
        cv.put("state", state);
        Cursor cursor = null;
        int doSaveTimes = 0;
        try{
            db = mDbHelper.getWritableDatabase();
            final String sql = "SELECT * from " + SQLiteHelper.TABLE_NAME + " WHERE userID = ? AND taskID = ? ";
            cursor = db.rawQuery(sql, new String[]{downloadInfo.getUserID(),downloadInfo.getTaskID()});
            if(cursor.moveToNext()){
                db.update(SQLiteHelper.TABLE_NAME, cv, "userID = ? AND taskID = ? ", new String[]{downloadInfo.getUserID(),downloadInfo.getTaskID()});
            }else{
                db.insert(SQLiteHelper.TABLE_NAME, null, cv);
            }
            cursor.close();
            db.close();
        } catch(Exception e) {
            doSaveTimes ++;
            if(doSaveTimes < 5){ //最多只做5次数据保存，降低数据保存失败率
                saveOrUpdateDownLoadInfo(downloadInfo, DownloadState.BEGIN);
            }
            if(cursor != null){
                cursor.close();
            }
            if(db != null){
                db.close();
            }
        }
    }

    public ArrayList<SQLDownLoadInfo> getAllDownLoadInfo(){
        final String sql = "SELECT * from " + SQLiteHelper.TABLE_NAME;
        return getSQLDownloadInfoList(sql, null);
    }

    public ArrayList<SQLDownLoadInfo> getUserDownLoadInfo(final String userID){
        final String sql = "SELECT * from " + SQLiteHelper.TABLE_NAME + " WHERE userID = ? AND state = ? ";
        return getSQLDownloadInfoList(sql, new String[]{userID, DownloadState.COMPLETE});
    }

    public ArrayList<SQLDownLoadInfo> getUserDownLoadInfo(final String userID, final String taskId){
        final String sql = "SELECT * from " + SQLiteHelper.TABLE_NAME + " WHERE userID = ? AND taskID = ? AND state = ? ";
        return getSQLDownloadInfoList(sql, new String[]{userID, taskId, DownloadState.COMPLETE});
    }

    public ArrayList<SQLDownLoadInfo> getUserDownLoadInfoFailed(final String userID){
        final String sql = "SELECT * from " + SQLiteHelper.TABLE_NAME + " WHERE userID = ? AND state != ? ";
        return getSQLDownloadInfoList(sql, new String[]{userID, DownloadState.COMPLETE});
    }

    public ArrayList<SQLDownLoadInfo> getDownLoadInfoFailed(){
        final String sql = "SELECT * from " + SQLiteHelper.TABLE_NAME + " WHERE state != '2'";
        return getSQLDownloadInfoList(sql, null);
    }

    public SQLDownLoadInfo deleteDownLoadInfo(final String userID, final String taskID){
        final String sql = "SELECT * from " + SQLiteHelper.TABLE_NAME + " WHERE userID = ? AND taskID = ? ";

        ArrayList<SQLDownLoadInfo> infoList = getSQLDownloadInfoList(sql, new String[]{userID, taskID});
        SQLDownLoadInfo info = null;
        if(infoList.size() == 1) {
            info = infoList.get(0);
            db = mDbHelper.getWritableDatabase();
            db.delete(SQLiteHelper.TABLE_NAME, "userID = ? AND taskID = ? ", new String[]{userID,taskID});
            db.close();
        }
        return info;
    }
    
    public void deleteUserDownLoadInfo(String userID){
        db = mDbHelper.getWritableDatabase();
        db.delete(SQLiteHelper.TABLE_NAME, "userID = ? ", new String[]{userID});
        db.close();
    }
    
    public void deleteAllDownLoadInfo(){
        db = mDbHelper.getWritableDatabase();
        db.delete(SQLiteHelper.TABLE_NAME, null, null);
        db.close();
    }

    private ArrayList<SQLDownLoadInfo> getSQLDownloadInfoList(final String sql, final String args[]) {
        ArrayList<SQLDownLoadInfo> downloadInfoList = new ArrayList<SQLDownLoadInfo>();
        db = mDbHelper.getWritableDatabase();
        try {
            Cursor cursor = db.rawQuery(sql, args);
            while(cursor.moveToNext()){
                SQLDownLoadInfo downloadInfo = new SQLDownLoadInfo();
                downloadInfo.setDownloadSize(cursor.getLong(cursor.getColumnIndex("downLoadSize")));
                downloadInfo.setFileName(cursor.getString(cursor.getColumnIndex("fileName")));
                downloadInfo.setFilePath(cursor.getString(cursor.getColumnIndex("filePath")));
                downloadInfo.setIcon(cursor.getString(cursor.getColumnIndex("icon")));
                downloadInfo.setAuthor(cursor.getString(cursor.getColumnIndex("author")));
                downloadInfo.setLike(cursor.getString(cursor.getColumnIndex("like")));
                downloadInfo.setFileSize(cursor.getLong(cursor.getColumnIndex("fileSize")));
                downloadInfo.setUrl(cursor.getString(cursor.getColumnIndex("url")));
                downloadInfo.setTaskID(cursor.getString(cursor.getColumnIndex("taskID")));
                downloadInfo.setUserID(cursor.getString(cursor.getColumnIndex("userID")));
                downloadInfoList.add(downloadInfo);
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        db.close();
        return downloadInfoList;
    }
}
