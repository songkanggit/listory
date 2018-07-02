package com.zealens.listory.core;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public interface CoreThreadPool {
    ExecutorService getWorkerThreadPool();

    ScheduledThreadPoolExecutor getScheduleExecutor();

    HandlerThread getWorkerThread();

    Handler getWorkerHandler();

    void executeAsyncTask(@NonNull final AsyncTask<?, ?, ?> task);

    void executeAsyncTask(@NonNull final FutureTask<?> task);

    void executeAsyncTask(@NonNull final Callable<?> callable);

    void executeAsyncTask(@NonNull final Runnable task);

    <T> Future<?> submitByExecutorService(Callable<T> task);

    Future<?> submitByExecutorService(Runnable task);

    <T> Future<?> submitByExecutorService(Runnable task, T result);

    void executeAsyncBackgroundTask(@NonNull final Runnable task);

    void executeAsyncTaskOnQueue(@NonNull Runnable task);

    void executeAsyncTaskWithFixedDelay(long initialDelay, long delay, @NonNull Runnable task);

    boolean removeExecuteAsyncTask(@NonNull Runnable task);

    void postTask(@NonNull final Runnable task);

    void postTaskAtFront(@NonNull final Runnable task);

    void postTaskAtTime(@NonNull final Runnable task, final long uptimeMillis);

    void postTaskAsDelayed(@NonNull final Runnable task, final long delayMillis);
}
