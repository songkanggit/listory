package com.listory.songkang.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.listory.songkang.application.MyApplication;
import com.listory.songkang.core.CoreContext;

import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

/**
 * Created by SouKou on 2018/3/7.
 */

public abstract class BaseFragment extends Fragment {
    protected View mRootView;
    protected WeakReference<CoreContext> mCoreContextRef;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(null == mRootView){
            mRootView = inflater.inflate(getLayoutId(), container, false);
        }
        if(mCoreContextRef == null) {
            mCoreContextRef = new WeakReference<>(((MyApplication)getActivity().getApplication()).getCoreContext());
        }
        return mRootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        afterCreate(savedInstanceState);
    }

    protected abstract int getLayoutId();

    protected abstract void afterCreate(Bundle savedInstanceState);
}
