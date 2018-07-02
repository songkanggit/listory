package com.zealens.listory.core;

import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import com.zealens.listory.BuildConfig;
import com.zealens.listory.core.logger.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class CoreApplication extends Application implements CoreObject, CoreThreadPool {
    private static final String TAG = CoreApplication.class.getSimpleName();
    private final SparseArray<ScheduledFuture> mScheduledThreadPoolExecutors = new SparseArray<>();
    private ExecutorService mWorkerThreadPool;
    private ExecutorService mBackgroundThreadPool;
    private ScheduledThreadPoolExecutor mScheduledThreadPoolExecutor;
    private HandlerThread mWorkerThread;
    private Handler mWorkerHandler;
    private TaskQueue mTaskQueue;
    private CoreContext mCoreContext;

    protected static void logException(Runnable r, Throwable t) {
        if (t == null && r instanceof Future<?>) {
            try {
                Future<?> future = (Future<?>) r;
                if (future.isDone())
                    future.get();
            } catch (CancellationException ignored) {
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt(); // ignore/reset
            } catch (Exception ee) {
                t = ee.getCause();
            }
        }
        if (t != null) {
            if (!BuildConfig.PUBLISH_FLAG) {
                t.printStackTrace();
            }
            Logger.e("Error", t.getMessage());
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        //you must install multiDex whatever tinker is installed!
//        MultiDex.install(base);
    }

    /**
     * @see <a href="https://github.com/TinkerPatch/tinkerpatch-easy-sample/blob/master/app/src/main/java/com/tinkerpatch/easy_sample/SampleApplication.java">application sample</a>
     */
    @Override
    public void onCreate() {
        super.onCreate();
        initialize();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        dispose();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        if (mCoreContext != null) {
            mCoreContext.freeMemory();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            if (mCoreContext != null) {
                mCoreContext.freeMemory();
            }
        }
    }

    @Override
    public void initialize() {
        mWorkerThreadPool = onCreateWorkerThreadPool();
        mBackgroundThreadPool = onCreateBackgroundThreadPool();
        mScheduledThreadPoolExecutor = onCreateScheduledThreadPool();

        mWorkerThread = onCreateWorkerThread("Queue-ThreadPool");
        mWorkerThread.start();
        mWorkerHandler = new Handler(mWorkerThread.getLooper());

        mTaskQueue = new TaskQueue(CoreApplication.class.getName());
        mTaskQueue.start();

        mCoreContext = new CoreContext(this);
        onInitializeApplicationService(mCoreContext);
        mCoreContext.initialize();
    }

    @Override
    public void dispose() {
        if (mWorkerHandler != null) {
            mWorkerHandler.removeCallbacksAndMessages(null);
            mWorkerHandler = null;
        }

        if (mWorkerThread != null) {
            mWorkerThread.quit();
            mWorkerThread = null;
        }

        if (mWorkerThreadPool != null) {
            mWorkerThreadPool.shutdown();
            mWorkerThreadPool = null;
        }

        if (mCoreContext != null) {
            mCoreContext.dispose();
            mCoreContext = null;
        }
    }

    @Override
    public boolean isInitialized() {
        return mCoreContext.isInitialized();
    }

    @Override
    public final ExecutorService getWorkerThreadPool() {
        return mWorkerThreadPool;
    }

    @Override
    public ScheduledThreadPoolExecutor getScheduleExecutor() {
        return mScheduledThreadPoolExecutor;
    }

    @Override
    public final HandlerThread getWorkerThread() {
        return mWorkerThread;
    }

    @Override
    public final Handler getWorkerHandler() {
        return mWorkerHandler;
    }

    @Override
    public void executeAsyncTask(@NonNull final AsyncTask task) {
        if (mWorkerThreadPool == null)
            return;

        if (mWorkerThreadPool.isShutdown())
            return;

        task.executeOnExecutor(mWorkerThreadPool);
    }

    @Override
    public void executeAsyncTask(@NonNull final FutureTask<?> task) {
        if (mWorkerThreadPool == null)
            return;

        if (mWorkerThreadPool.isShutdown())
            return;

        mWorkerThreadPool.execute(task);
    }

    @Override
    public void executeAsyncTask(@NonNull final Callable<?> callable) {
        if (mWorkerThreadPool == null)
            return;

        if (mWorkerThreadPool.isShutdown())
            return;

        mWorkerThreadPool.submit(callable);
    }

    @Override
    public void executeAsyncTask(@NonNull final Runnable task) {
        if (mWorkerThreadPool == null)
            return;

        if (mWorkerThreadPool.isShutdown())
            return;

        mWorkerThreadPool.execute(task);
    }

    @Override
    public <T> Future<?> submitByExecutorService(Callable<T> task) {
        if (mWorkerThreadPool == null)
            return null;

        if (mWorkerThreadPool.isShutdown())
            return null;

        return mWorkerThreadPool.submit(task);
    }

    @Override
    public Future<?> submitByExecutorService(Runnable task) {
        if (mWorkerThreadPool == null)
            return null;

        if (mWorkerThreadPool.isShutdown())
            return null;

        return mWorkerThreadPool.submit(task);
    }

    @Override
    public <T> Future<?> submitByExecutorService(Runnable task, T result) {
        if (mWorkerThreadPool == null)
            return null;

        if (mWorkerThreadPool.isShutdown())
            return null;

        return mWorkerThreadPool.submit(task, result);
    }

    @Override
    public void executeAsyncBackgroundTask(@NonNull Runnable task) {
        if (mBackgroundThreadPool == null)
            return;

        if (mBackgroundThreadPool.isShutdown())
            return;

        mBackgroundThreadPool.execute(task);
    }

    @Override
    public void executeAsyncTaskOnQueue(@NonNull Runnable task) {
        if (mTaskQueue == null)
            return;

        if (!mTaskQueue.isAlive())
            return;

        mTaskQueue.scheduleTask(task);
    }

    @Override
    public void executeAsyncTaskWithFixedDelay(long initialDelay, long delay, @NonNull Runnable task) {
        synchronized (mScheduledThreadPoolExecutors) {
            if (mScheduledThreadPoolExecutors.indexOfKey(task.hashCode()) >= 0) return;
            mScheduledThreadPoolExecutors.put(task.hashCode(), mScheduledThreadPoolExecutor.scheduleWithFixedDelay(task, initialDelay, delay, TimeUnit.SECONDS));
        }
    }

    @Override
    public boolean removeExecuteAsyncTask(@NonNull Runnable task) {
        synchronized (mScheduledThreadPoolExecutors) {
            if (mScheduledThreadPoolExecutors.indexOfKey(task.hashCode()) >= 0) {
                mScheduledThreadPoolExecutors.get(task.hashCode()).cancel(true);
                mScheduledThreadPoolExecutors.remove(task.hashCode());
                return true;
            }

            return false;
        }
    }

    @Override
    public void postTask(@NonNull final Runnable task) {
        if (mWorkerThread == null)
            return;

        if (!mWorkerThread.isAlive())
            return;

        mWorkerHandler.post(task);
    }

    @Override
    public void postTaskAtFront(@NonNull final Runnable task) {
        if (mWorkerThread == null)
            return;

        if (!mWorkerThread.isAlive())
            return;

        mWorkerHandler.postAtFrontOfQueue(task);
    }

    @Override
    public void postTaskAtTime(@NonNull final Runnable task, final long uptimeMillis) {
        if (mWorkerThread == null)
            return;

        if (!mWorkerThread.isAlive())
            return;

        mWorkerHandler.postAtTime(task, uptimeMillis);
    }

    @Override
    public void postTaskAsDelayed(@NonNull final Runnable task, final long delayMillis) {
        if (mWorkerThread == null)
            return;

        if (!mWorkerThread.isAlive())
            return;

        mWorkerHandler.postDelayed(task, delayMillis);
    }

    public final CoreContext getCoreContext() {
        return mCoreContext;
    }

    protected void onInitializeApplicationService(final CoreContext coreContext) {
    }

    protected abstract ExecutorService onCreateWorkerThreadPool();

    protected abstract ExecutorService onCreateBackgroundThreadPool();

    protected abstract ScheduledThreadPoolExecutor onCreateScheduledThreadPool();

    protected abstract HandlerThread onCreateWorkerThread(final String poolName);
}
