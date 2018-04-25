package com.listory.songkang.core;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.UserHandle;
import android.support.annotation.IntegerRes;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.StringRes;
import android.view.Display;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public final class CoreContext implements CoreObject, CoreThreadPool {

    protected final String TAG;

    protected final Context mContext;

    protected final CoreApplication mCoreApplication;

    protected final HashMap<Class<? extends BaseCoreManager>, BaseCoreManager> mServices;

    public CoreContext(@NonNull CoreApplication coreApplication) {
        TAG = getClass().getSimpleName();

        mCoreApplication = coreApplication;

        mContext = mCoreApplication.getApplicationContext();

        mServices = new HashMap<>();
    }

    @Override
    public void initialize() {
        final ArrayList<HashMap.Entry<Class<? extends BaseCoreManager>, BaseCoreManager>> list = new ArrayList<>(mServices.entrySet());
        Collections.sort(list, (o1, o2) -> o1.getValue().compareTo(o2.getValue()));

        for (HashMap.Entry<Class<? extends BaseCoreManager>, BaseCoreManager> entry : list) {
            BaseCoreManager manager = entry.getValue();
            if (manager == null) continue;

            if ((manager.order() & BaseCoreManager.ORDER.POST_MASK) != BaseCoreManager.ORDER.POST_MASK)
                manager.initialize();
            else postTask(manager::initialize);
        }
    }

    @Override
    public void dispose() {
        for (HashMap.Entry<Class<? extends BaseCoreManager>, BaseCoreManager> entry : mServices.entrySet()) {
            BaseCoreManager manager = entry.getValue();
            if (manager == null) continue;

            manager.dispose();
        }

        clearApplicationService();
    }

    @Override
    public boolean isInitialized() {
        return !mServices.isEmpty();
    }

    public Application getApplication() {
        return (Application)mContext;
    }

    public final Context getBaseContext() {
        return mContext;
    }

    public final AssetManager getAssets() {
        return mContext.getAssets();
    }

    public final Resources getResources() {
        return mContext.getResources();
    }

    public final String getString(@StringRes final int resId) {
        return getResources().getString(resId);
    }

    public final int getInteger(@IntegerRes final int resId) {
        return getResources().getInteger(resId);
    }

    public final int getDimensionPixelOffset(int dimen){
        return getResources().getDimensionPixelOffset(dimen);
    }

    public final PackageManager getPackageManager() {
        return mContext.getPackageManager();
    }

    public final ContentResolver getContentResolver() {
        return mContext.getContentResolver();
    }

    public final Looper getMainLooper() {
        return mContext.getMainLooper();
    }

    public final Context getApplicationContext() {
        return mContext.getApplicationContext();
    }

    public final Resources.Theme getTheme() {
        return mContext.getTheme();
    }

    public final void setTheme(final int resid) {
        mContext.setTheme(resid);
    }

    public final ClassLoader getClassLoader() {
        return mContext.getClassLoader();
    }

    public final String getPackageName() {
        return mContext.getPackageName();
    }

    public final ApplicationInfo getApplicationInfo() {
        return mContext.getApplicationInfo();
    }

    public final String getPackageResourcePath() {
        return mContext.getPackageResourcePath();
    }

    public final String getPackageCodePath() {
        return mContext.getPackageCodePath();
    }

    public final FileInputStream openFileInput(final String name) throws FileNotFoundException {
        return mContext.openFileInput(name);
    }

    public final FileOutputStream openFileOutput(final String name, final int mode) throws FileNotFoundException {
        return mContext.openFileOutput(name, mode);
    }

    public final boolean deleteFile(final String name) {
        return mContext.deleteFile(name);
    }

    public final File getFileStreamPath(final String name) {
        return mContext.getFileStreamPath(name);
    }

    public final String[] fileList() {
        return mContext.fileList();
    }

    public final File getFilesDir() {
        return mContext.getFilesDir();
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public final File getNoBackupFilesDir() {
        return mContext.getNoBackupFilesDir();
    }

    public final File getExternalFilesDir(final String type) {
        return mContext.getExternalFilesDir(type);
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    public final File[] getExternalFilesDirs(final String type) {
        return mContext.getExternalFilesDirs(type);
    }

    public final File getObbDir() {
        return mContext.getObbDir();
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    public final File[] getObbDirs() {
        return mContext.getObbDirs();
    }

    public final File getCacheDir() {
        return mContext.getCacheDir();
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public final File getCodeCacheDir() {
        return mContext.getCodeCacheDir();
    }

    public final File getExternalCacheDir() {
        return mContext.getExternalCacheDir();
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    public final File[] getExternalCacheDirs() {
        return mContext.getExternalCacheDirs();
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public final File[] getExternalMediaDirs() {
        return mContext.getExternalMediaDirs();
    }

    public final File getDir(final String name, final int mode) {
        return mContext.getDir(name, mode);
    }

    public final SQLiteDatabase openOrCreateDatabase(final String name, final int mode,
                                                     final SQLiteDatabase.CursorFactory factory) {
        return mContext.openOrCreateDatabase(name, mode, factory);
    }

    public final SQLiteDatabase openOrCreateDatabase(final String name, final int mode,
                                                     final SQLiteDatabase.CursorFactory factory,
                                                     final DatabaseErrorHandler errorHandler) {
        return mContext.openOrCreateDatabase(name, mode, factory, errorHandler);
    }

    public final boolean deleteDatabase(final String name) {
        return mContext.deleteDatabase(name);
    }

    public final File getDatabasePath(final String name) {
        return mContext.getDatabasePath(name);
    }

    public final String[] databaseList() {
        return mContext.databaseList();
    }

    public final Drawable getWallpaper() {
        return mContext.getWallpaper();
    }

    public final void setWallpaper(final InputStream data) throws IOException {
        mContext.setWallpaper(data);
    }

    public final void setWallpaper(final Bitmap bitmap) throws IOException {
        mContext.setWallpaper(bitmap);
    }

    public final Drawable peekWallpaper() {
        return mContext.peekWallpaper();
    }

    public final int getWallpaperDesiredMinimumWidth() {
        return mContext.getWallpaperDesiredMinimumWidth();
    }

    public final int getWallpaperDesiredMinimumHeight() {
        return mContext.getWallpaperDesiredMinimumHeight();
    }

    public final void clearWallpaper() throws IOException {
        mContext.clearWallpaper();
    }

    public final void startActivity(final Intent intent) {
        mContext.startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public final void startActivity(final Intent intent, final Bundle options) {
        mContext.startActivity(intent, options);
    }

    public final void startActivities(final Intent[] intents) {
        mContext.startActivities(intents);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public final void startActivities(final Intent[] intents, final Bundle options) {
        mContext.startActivities(intents, options);
    }

    public final void startIntentSender(final IntentSender intent, final Intent fillInIntent,
                                        final int flagsMask, final int flagsValues,
                                        final int extraFlags) throws IntentSender.SendIntentException {
        mContext.startIntentSender(intent, fillInIntent, flagsMask,
                flagsValues, extraFlags);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public final void startIntentSender(final IntentSender intent, final Intent fillInIntent,
                                        final int flagsMask, final int flagsValues, final int extraFlags,
                                        final Bundle options) throws IntentSender.SendIntentException {
        mContext.startIntentSender(intent, fillInIntent, flagsMask,
                flagsValues, extraFlags, options);
    }

    public final void sendBroadcast(final Intent intent) {
        mContext.sendBroadcast(intent);
    }

    public final void sendBroadcast(final Intent intent, final String receiverPermission) {
        mContext.sendBroadcast(intent, receiverPermission);
    }

    public final void sendOrderedBroadcast(final Intent intent, final String receiverPermission) {
        mContext.sendOrderedBroadcast(intent, receiverPermission);
    }

    public final void sendOrderedBroadcast(final Intent intent, final String receiverPermission,
                                           final BroadcastReceiver resultReceiver, final Handler scheduler,
                                           final int initialCode, final String initialData, final Bundle initialExtras) {
        mContext.sendOrderedBroadcast(intent, receiverPermission,
                resultReceiver, scheduler, initialCode,
                initialData, initialExtras);
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public final void sendBroadcastAsUser(final Intent intent, final UserHandle user) {
        mContext.sendBroadcastAsUser(intent, user);
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public final void sendBroadcastAsUser(final Intent intent, final UserHandle user, final String receiverPermission) {
        mContext.sendBroadcastAsUser(intent, user, receiverPermission);
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public final void sendOrderedBroadcastAsUser(final Intent intent, final UserHandle user, final String receiverPermission,
                                                 final BroadcastReceiver resultReceiver, final Handler scheduler,
                                                 final int initialCode, final String initialData, final Bundle initialExtras) {
        mContext.sendOrderedBroadcastAsUser(intent, user, receiverPermission, resultReceiver,
                scheduler, initialCode, initialData, initialExtras);
    }

    public final void sendStickyBroadcast(final Intent intent) {
        mContext.sendStickyBroadcast(intent);
    }

    public final void sendStickyOrderedBroadcast(final Intent intent, final BroadcastReceiver resultReceiver,
                                                 final Handler scheduler, final int initialCode, final String initialData,
                                                 final Bundle initialExtras) {
        mContext.sendStickyOrderedBroadcast(intent,
                resultReceiver, scheduler, initialCode,
                initialData, initialExtras);
    }

    public final void removeStickyBroadcast(final Intent intent) {
        mContext.removeStickyBroadcast(intent);
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public final void sendStickyBroadcastAsUser(final Intent intent, final UserHandle user) {
        mContext.sendStickyBroadcastAsUser(intent, user);
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public final void sendStickyOrderedBroadcastAsUser(final Intent intent, final UserHandle user,
                                                       final BroadcastReceiver resultReceiver,
                                                       final Handler scheduler, int initialCode,
                                                       final String initialData, final Bundle initialExtras) {
        mContext.sendStickyOrderedBroadcastAsUser(intent, user, resultReceiver,
                scheduler, initialCode, initialData, initialExtras);
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public final void removeStickyBroadcastAsUser(final Intent intent, final UserHandle user) {
        mContext.removeStickyBroadcastAsUser(intent, user);
    }

    public final Intent registerReceiver(final BroadcastReceiver receiver, final IntentFilter filter) {
        return mContext.registerReceiver(receiver, filter);
    }

    public final Intent registerReceiver(final BroadcastReceiver receiver, final IntentFilter filter,
                                         final String broadcastPermission, final Handler scheduler) {
        return mContext.registerReceiver(receiver, filter, broadcastPermission, scheduler);
    }

    public final void unregisterReceiver(final BroadcastReceiver receiver) {
        mContext.unregisterReceiver(receiver);
    }

    public final ComponentName startService(final Intent service) {
        return mContext.startService(service);
    }

    public final boolean stopService(final Intent name) {
        return mContext.stopService(name);
    }

    public final boolean bindService(final Intent service, final ServiceConnection conn, final int flags) {
        return mContext.bindService(service, conn, flags);
    }

    public final void unbindService(final ServiceConnection conn) {
        mContext.unbindService(conn);
    }

    public final boolean startInstrumentation(final ComponentName className, final String profileFile,
                                              final Bundle arguments) {
        return mContext.startInstrumentation(className, profileFile, arguments);
    }

    public Object getSystemService(String name) {
        return mContext.getSystemService(name);
    }

    @RequiresApi(Build.VERSION_CODES.M)
    public String getSystemServiceName(Class<?> serviceClass) {
        return mContext.getSystemServiceName(serviceClass);
    }

    public final int checkPermission(final String permission, final int pid, final int uid) {
        return mContext.checkPermission(permission, pid, uid);
    }

    public final int checkCallingPermission(final String permission) {
        return mContext.checkCallingPermission(permission);
    }

    public final int checkCallingOrSelfPermission(final String permission) {
        return mContext.checkCallingOrSelfPermission(permission);
    }

    @RequiresApi(Build.VERSION_CODES.M)
    public final int checkSelfPermission(final String permission) {
        return mContext.checkSelfPermission(permission);
    }

    public final void enforcePermission(final String permission, final int pid, final int uid,
                                        final String message) {
        mContext.enforcePermission(permission, pid, uid, message);
    }

    public final void enforceCallingPermission(final String permission, final String message) {
        mContext.enforceCallingPermission(permission, message);
    }

    public final void enforceCallingOrSelfPermission(final String permission, final String message) {
        mContext.enforceCallingOrSelfPermission(permission, message);
    }

    public final void grantUriPermission(final String toPackage, final Uri uri, final int modeFlags) {
        mContext.grantUriPermission(toPackage, uri, modeFlags);
    }

    public final void revokeUriPermission(final Uri uri, final int modeFlags) {
        mContext.revokeUriPermission(uri, modeFlags);
    }

    public final int checkUriPermission(final Uri uri, final int pid, final int uid, final int modeFlags) {
        return mContext.checkUriPermission(uri, pid, uid, modeFlags);
    }

    public final int checkCallingUriPermission(final Uri uri, final int modeFlags) {
        return mContext.checkCallingUriPermission(uri, modeFlags);
    }

    public final int checkCallingOrSelfUriPermission(final Uri uri, final int modeFlags) {
        return mContext.checkCallingOrSelfUriPermission(uri, modeFlags);
    }

    public final int checkUriPermission(final Uri uri, final String readPermission, final String writePermission,
                                        final int pid, final int uid, final int modeFlags) {
        return mContext.checkUriPermission(uri, readPermission, writePermission,
                pid, uid, modeFlags);
    }

    public final void enforceUriPermission(final Uri uri, final int pid, final int uid,
                                           final int modeFlags, final String message) {
        mContext.enforceUriPermission(uri, pid, uid, modeFlags, message);
    }

    public final void enforceCallingUriPermission(final Uri uri, final int modeFlags, final String message) {
        mContext.enforceCallingUriPermission(uri, modeFlags, message);
    }

    public final void enforceCallingOrSelfUriPermission(final Uri uri, final int modeFlags, final String message) {
        mContext.enforceCallingOrSelfUriPermission(uri, modeFlags, message);
    }

    public final void enforceUriPermission(Uri uri, String readPermission, String writePermission,
                                           int pid, int uid, int modeFlags, String message) {
        mContext.enforceUriPermission(uri, readPermission, writePermission, pid, uid, modeFlags, message);
    }

    public final Context createPackageContext(final String packageName, final int flags)
            throws PackageManager.NameNotFoundException {
        return mContext.createPackageContext(packageName, flags);
    }

    public final int getProcessId() {
        return android.os.Process.myPid();
    }

    public final int getThreadId() {
        return android.os.Process.myTid();
    }

    public final int getUserId() {
        return android.os.Process.myUid();
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public final Context createConfigurationContext(final Configuration overrideConfiguration) {
        return mContext.createConfigurationContext(overrideConfiguration);
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public final Context createDisplayContext(final Display display) {
        return mContext.createDisplayContext(display);
    }

    public final boolean isRestricted() {
        return mContext.isRestricted();
    }

    @SuppressWarnings("unchecked")
    public <BCM extends BaseCoreManager> BCM getApplicationService(@NonNull final Class<? extends BaseCoreManager> service) {
        return (BCM) mServices.get(service);
    }

    @Override
    public final ExecutorService getWorkerThreadPool() {
        return mCoreApplication.getWorkerThreadPool();
    }

    @Override
    public ScheduledThreadPoolExecutor getScheduleExecutor() {
        return mCoreApplication.getScheduleExecutor();
    }

    @Override
    public final HandlerThread getWorkerThread() {
        return mCoreApplication.getWorkerThread();
    }

    @Override
    public final Handler getWorkerHandler() {
        return mCoreApplication.getWorkerHandler();
    }

    @Override
    public void executeAsyncTask(@NonNull AsyncTask<?, ?, ?> task) {
        mCoreApplication.executeAsyncTask(task);
    }

    @Override
    public void executeAsyncTask(@NonNull final FutureTask<?> task) {
        mCoreApplication.executeAsyncTask(task);
    }

    @Override
    public void executeAsyncTask(@NonNull final Callable<?> callable) {
        mCoreApplication.executeAsyncTask(callable);
    }

    @Override
    public void executeAsyncTask(@NonNull Runnable task) {
        mCoreApplication.executeAsyncTask(task);
    }

    @Override
    public <T> Future<?> submitByExecutorService(Callable<T> task) {
        return mCoreApplication.submitByExecutorService(task);
    }

    @Override
    public <T> Future<?> submitByExecutorService(Runnable task, T result) {
        return mCoreApplication.submitByExecutorService(task, result);
    }

    @Override
    public Future<?> submitByExecutorService(Runnable task) {
        return mCoreApplication.submitByExecutorService(task);
    }

    @Override
    public void executeAsyncBackgroundTask(@NonNull Runnable task) {
        mCoreApplication.executeAsyncBackgroundTask(task);
    }

    @Override
    public void executeAsyncTaskOnQueue(@NonNull Runnable runnable) {
        mCoreApplication.executeAsyncTaskOnQueue(runnable);
    }

    @Override
    public void executeAsyncTaskWithFixedDelay(long initialDelay, long delay, @NonNull Runnable task) {
        mCoreApplication.executeAsyncTaskWithFixedDelay(initialDelay,delay,task);
    }

    @Override
    public boolean removeExecuteAsyncTask(@NonNull Runnable task) {
        return mCoreApplication.removeExecuteAsyncTask(task);
    }

    @Override
    public void postTask(@NonNull Runnable task) {
        mCoreApplication.postTask(task);
    }

    @Override
    public void postTaskAtFront(@NonNull Runnable task) {
        mCoreApplication.postTaskAtFront(task);
    }

    @Override
    public void postTaskAsDelayed(@NonNull Runnable task, long delayMillis) {
        mCoreApplication.postTaskAsDelayed(task, delayMillis);
    }

    @Override
    public void postTaskAtTime(@NonNull Runnable task, long uptimeMillis) {
        mCoreApplication.postTaskAtTime(task, uptimeMillis);
    }

    public void addApplicationService(final BaseCoreManager BaseCoreManager) {
        mServices.put(BaseCoreManager.getClass(), BaseCoreManager);
    }

    public void clearApplicationService() {
        mServices.clear();
    }

    public boolean isSupportCpuArchitecture() {
        final String cpuName = Build.CPU_ABI.toLowerCase(Locale.US);

        if (cpuName.contains("mips"))
            return false;

        return !cpuName.contains("x86");

    }

    protected void freeMemory() {
        for (HashMap.Entry<Class<? extends BaseCoreManager>, BaseCoreManager> entry : mServices.entrySet()) {
            final BaseCoreManager manager = entry.getValue();
            if (manager == null)
                continue;

            manager.freeMemory();
        }
    }

    public <F extends Future> void cancelFutures(F... fs) {
        cancelFutures(true, fs);
    }

    public <F extends Future> void cancelFutures(boolean interrupt, F... fs) {
        for (F f : fs) if (f != null) f.cancel(interrupt);
    }
}
