package com.zealens.listory.core;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;

import java.util.LinkedList;

public class CoreHandler {
    private final LinkedList<Runnable> mQueue;
    private MessageQueue mMessageQueue;
    private Impl mHandler;

    public CoreHandler() {
        mQueue = new LinkedList<>();
        mMessageQueue = Looper.myQueue();
        mHandler = new Impl();
    }

    /**
     * Schedule runnable to run after everything that's on the queue right now.
     */
    public void post(Runnable runnable) {
        synchronized (mQueue) {
            mQueue.add(runnable);
            if (mQueue.size() == 1) {
                scheduleNextLocked();
            }
        }
    }

    /**
     * Schedule runnable to run when the queue goes onPhoneIdle.
     */
    public void postIdle(final Runnable runnable) {
        post(new IdleRunnable(runnable));
    }

    public void cancelAll() {
        synchronized (mQueue) {
            mQueue.clear();
        }
    }

    /**
     * Runs all queued Runnables from the calling thread.
     */
    public void flush() {
        LinkedList<Runnable> queue = new LinkedList<>();
        synchronized (mQueue) {
            queue.addAll(mQueue);
            mQueue.clear();
        }
        for (Runnable r : queue) {
            r.run();
        }
    }

    void scheduleNextLocked() {
        if (mQueue.size() > 0) {
            Runnable peek = mQueue.getFirst();
            if (peek instanceof IdleRunnable) {
                mMessageQueue.addIdleHandler(mHandler);
            } else {
                mHandler.sendEmptyMessage(1);
            }
        }
    }

    class Impl extends Handler implements MessageQueue.IdleHandler {
        public void handleMessage(Message msg) {
            Runnable r;
            synchronized (mQueue) {
                if (mQueue.size() == 0) {
                    return;
                }
                r = mQueue.removeFirst();
            }
            r.run();
            synchronized (mQueue) {
                scheduleNextLocked();
            }
        }

        public boolean queueIdle() {
            handleMessage(null);
            return false;
        }
    }

    private class IdleRunnable implements Runnable {
        Runnable mRunnable;

        IdleRunnable(Runnable r) {
            mRunnable = r;
        }

        public void run() {
            mRunnable.run();
        }
    }
}

