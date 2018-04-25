package com.listory.songkang.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.listory.songkang.core.CoreApplication;
import com.listory.songkang.core.CoreContext;
import com.listory.songkang.core.connection.NetworkManager;
import com.listory.songkang.core.download.DownloadManager;
import com.listory.songkang.core.download.DownloadService;
import com.listory.songkang.core.http.HttpManager;
import com.listory.songkang.core.http.HttpService;
import com.listory.songkang.core.logger.LoggerManager;
import com.listory.songkang.core.logger.LoggerService;
import com.listory.songkang.core.preference.PreferencesManager;
import com.listory.songkang.listory.R;


/**
 * Created by SouKou on 2017/8/1.
 */

public abstract class BaseActivity extends AppCompatActivity {
    protected final String TAG;
    protected CoreContext mCoreContext;
    protected Context mContext;
    protected DownloadService mDownloadService;
    protected PreferencesManager mPreferencesManager;
    protected NetworkManager mNetworkManager;
    protected LoggerService mLoggerService;
    protected HttpService mHttpService;
    protected ViewGroup mRootVG;
    protected Toolbar mToolBar;

    public BaseActivity(){
        TAG = getClass().getSimpleName();
    }

    @Override
    protected void onCreate(@Nullable Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        mCoreContext = getCoreContext();
        mContext = getApplicationContext();

        mLoggerService = mCoreContext.getApplicationService(LoggerManager.class);
        mNetworkManager = mCoreContext.getApplicationService(NetworkManager.class);
        mPreferencesManager = mCoreContext.getApplicationService(PreferencesManager.class);
        mDownloadService = mCoreContext.getApplicationService(DownloadManager.class);
        mHttpService = mCoreContext.getApplicationService(HttpManager.class);
        parseIntent(getIntent());

        initDataIgnoreUi();
        int layoutResId = getLayoutResourceId();
        if(layoutResId != 0) {
            setContentView(layoutResId);
        }

        mRootVG = fvb(R.id.contentPanel);

        mToolBar = fvb(R.id.toolbar);
        mToolBar.setTitle("");
        setSupportActionBar(mToolBar);

        viewAffairs();
        assembleViewClickAffairs();
        initDataAfterUiAffairs();
    }

    protected CoreContext getCoreContext(){
        return ((CoreApplication) getApplication()).getCoreContext();
    }


    protected void parseIntent(Intent intent){
        if(intent == null) return;

        Bundle bundle = intent.getExtras();
        if(bundle != null) {
            parseNonNullBundle(bundle);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    protected <V extends View> V fvb(@IdRes int id){
        return (V) findViewById(id);
    }

    protected void parseNonNullBundle(Bundle bundle){}
    protected void initDataIgnoreUi() {}
    @LayoutRes
    protected int getLayoutResourceId() { return 0;}
    protected void viewAffairs(){}
    protected void assembleViewClickAffairs(){}
    protected void initDataAfterUiAffairs(){}
}
