package com.listory.songkang.core;

import com.listory.songkang.core.logger.Logger;

import org.intellij.lang.annotations.MagicConstant;

public abstract class BaseCoreManager extends CoreObjectImpl implements Comparable, CoreObject, CoreLogger {


    @MagicConstant(intValues = {ORDER.LOGGER, ORDER.PREFERENCES, ORDER.ANALYTICS, ORDER.NETWORK
            , ORDER.USER_CACHE, ORDER.UMPIRE, ORDER.HTTP, ORDER.POST_MASK, ORDER.DATA_REPORT,})
    public @interface ORDER {
        int LOGGER = 1;
        int PREFERENCES = 2;
        int ANALYTICS = 3;
        int NETWORK = 4;
        int USER_CACHE = 5;
        int UMPIRE = 6;
        int HTTP = 7;
        int POST_MASK = 0x10;
        int DATA_REPORT = 8;
    }

    protected final String TAG;

    public BaseCoreManager() {
        TAG = getClass().getSimpleName();
    }

    @Override
    public void v(String msg) {
        Logger.v(TAG, msg);
    }

    @Override
    public void i(String msg) {
        Logger.i(TAG, msg);
    }

    @Override
    public void d(String msg) {
        Logger.d(TAG, msg);
    }

    @Override
    public void w(String msg) {
        Logger.w(TAG, msg);
    }

    @Override
    public void e(String msg) {
        Logger.e(TAG, msg);
    }

    @Override
    public int compareTo(final Object o) {
        final int thisOrder = order();
        final int otherOrder = ((BaseCoreManager) o).order();
        if (thisOrder < otherOrder)
            return -1;
        if (thisOrder > otherOrder)
            return 1;
        return 0;
    }

    @ORDER
    public abstract int order();

    public abstract void freeMemory();
}
