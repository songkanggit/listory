package com.listory.songkang.core;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created on 2017/3/18
 * in BlaBla by Kyle
 */

public class CoreObjectImpl implements CoreObject {
    protected final AtomicBoolean mInitialized;

    public CoreObjectImpl() {
        mInitialized = new AtomicBoolean(false);
    }

    @Override
    public void initialize() {
        mInitialized.set(true);
    }

    @Override
    public void dispose() {
        mInitialized.set(false);
    }

    @Override
    public boolean isInitialized() {
        return mInitialized.get();
    }
}
