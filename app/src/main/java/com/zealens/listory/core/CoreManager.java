package com.zealens.listory.core;

import android.content.Context;


public abstract class CoreManager extends BaseCoreManager implements Comparable, CoreObject, CoreLogger {
    protected final Context mContext;
    protected final CoreContext mCoreContext;

    public CoreManager(final CoreContext coreContext) {
        super();
        mCoreContext = coreContext;
        mContext = coreContext.mContext;
    }

    public CoreContext getCoreContext() {
        return mCoreContext;
    }
}
