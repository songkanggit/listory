package com.zealens.listory.core;

import com.zealens.listory.core.logger.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class TaskQueue extends Thread {
    static private String TAG = TaskQueue.class.getSimpleName();
    private BlockingQueue<Runnable> mBlockingQueue;

    public TaskQueue() {
        mBlockingQueue = new LinkedBlockingQueue<>();
    }

    public TaskQueue(String str) {
        this();
        setName(str);
    }

    public void scheduleTask(Runnable task) {
        mBlockingQueue.add(task);
    }

    public void run() {
        while (true) {
            try {
                mBlockingQueue.take().run();
            } catch (InterruptedException e) {
                Logger.e(TAG, e.getMessage());
            } catch (Exception e) {
                if (Logger.S_DEBUG) {
                    e.printStackTrace();
                }
            }
        }
    }
}
