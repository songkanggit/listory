package com.zealens.listory.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;

import com.zealens.listory.R;
import com.zealens.listory.core.CoreApplication;
import com.zealens.listory.core.CoreContext;
import com.zealens.listory.core.connection.NetworkManager;
import com.zealens.listory.core.download.DownLoadManager;
import com.zealens.listory.core.http.HttpManager;
import com.zealens.listory.core.http.HttpService;
import com.zealens.listory.core.logger.LoggerManager;
import com.zealens.listory.core.logger.LoggerService;
import com.zealens.listory.core.preference.PreferencesManager;
import com.soukou.swipeback.SwipeBackLayout;
import com.soukou.swipeback.Utils;
import com.soukou.swipeback.app.SwipeBackActivityBase;
import com.soukou.swipeback.app.SwipeBackActivityHelper;


/**
 * Created by SouKou on 2017/8/1.
 */

public abstract class BaseActivity extends AppCompatActivity  implements SwipeBackActivityBase {
    protected final String TAG;
    protected CoreContext mCoreContext;
    protected Context mContext;
    protected PreferencesManager mPreferencesManager;
    protected NetworkManager mNetworkManager;
    protected LoggerService mLoggerService;
    protected DownLoadManager mDownloadManager;
    protected HttpService mHttpService;
    protected ViewGroup mRootVG;
    protected Toolbar mToolBar;
    private SwipeBackActivityHelper mHelper;

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
        mHttpService = mCoreContext.getApplicationService(HttpManager.class);
        mDownloadManager = mCoreContext.getApplicationService(DownLoadManager.class);
        parseIntent(getIntent());

        initDataIgnoreUi();
        int layoutResId = getLayoutResourceId();
        if(layoutResId != 0) {
            setContentView(layoutResId);
        }

        mRootVG = fvb(R.id.contentPanel);
        mToolBar = fvb(R.id.toolbar);
        if(mToolBar != null) {
            mToolBar.setTitle("");
        }
        setSupportActionBar(mToolBar);

        viewAffairs();
        mHelper = new SwipeBackActivityHelper(this);
        mHelper.onActivityCreate();

        assembleViewClickAffairs();
        initDataAfterUiAffairs();
    }

    protected CoreContext getCoreContext(){
        return ((CoreApplication) getApplication()).getCoreContext();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mHelper.onPostCreate();
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

    @Override
    public SwipeBackLayout getSwipeBackLayout() {
        return mHelper.getSwipeBackLayout();
    }

    @Override
    public void setSwipeBackEnable(boolean enable) {
        getSwipeBackLayout().setEnableGesture(enable);
    }

    @Override
    public void scrollToFinishActivity() {
        Utils.convertActivityToTranslucent(this);
        getSwipeBackLayout().scrollToFinishActivity();
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
