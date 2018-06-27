package com.listory.songkang.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.listory.songkang.adapter.RecyclerViewMelodyListAdapter;
import com.listory.songkang.bean.MelodyDetailBean;
import com.listory.songkang.bean.SQLDownLoadInfo;
import com.listory.songkang.constant.DomainConst;
import com.listory.songkang.constant.PreferenceConst;
import com.listory.songkang.helper.HttpHelper;
import com.listory.songkang.R;
import com.listory.songkang.service.MediaService;
import com.listory.songkang.service.MusicTrack;
import com.listory.songkang.core.download.DownLoadListener;
import com.listory.songkang.core.download.DownLoadManager;
import com.listory.songkang.utils.StringUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.listory.songkang.service.MediaService.PLAY_ACTION_PARAM_LIST;
import static com.listory.songkang.service.MediaService.PLAY_ACTION_PARAM_POSITION;


public class MyFavoriteActivity extends BaseActivity implements View.OnClickListener, RecyclerViewMelodyListAdapter.OnItemClickListener {
    private RecyclerViewMelodyListAdapter mMelodyRecyclerViewAdapter;
    private RecyclerView mMelodyRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private List<MelodyDetailBean> mMelodyBeanList;
    private ImageView mBackButton;
    private TextView mTitleView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private LinearLayout mPlayAllLL;
    private volatile int mLastVisibleItem;
    private int mCurrentPage;

    protected void parseNonNullBundle(Bundle bundle){

    }
    protected void initDataIgnoreUi() {
        mCurrentPage = 1;
        mMelodyBeanList = new ArrayList<>();
    }
    @LayoutRes
    protected int getLayoutResourceId() { return R.layout.activity_my_favorite;}
    protected void viewAffairs(){
        mMelodyRecyclerView = fvb(R.id.recycler_view);
        mBackButton = fvb(R.id.toolbar_back);
        mTitleView = fvb(R.id.toolbar_title);
        mPlayAllLL = fvb(R.id.ll_play_all);
        mSwipeRefreshLayout = fvb(R.id.swipe_refresh);
    }
    protected void assembleViewClickAffairs(){
        mPlayAllLL.setOnClickListener(this);
        mBackButton.setOnClickListener(this);
    }
    protected void initDataAfterUiAffairs(){
        mMelodyRecyclerView.setLayoutManager(mLinearLayoutManager = new LinearLayoutManager(getApplicationContext()));
        mMelodyRecyclerView.setHasFixedSize(true);
        mMelodyRecyclerView.setAdapter(mMelodyRecyclerViewAdapter = new RecyclerViewMelodyListAdapter(MyFavoriteActivity.this, mMelodyBeanList));
        mMelodyRecyclerViewAdapter.setOnItemClickListener(this);
        mMelodyRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if(newState == RecyclerView.SCROLL_STATE_IDLE && mMelodyRecyclerViewAdapter.getItemCount() == mLastVisibleItem + 1) {
                    if(mMelodyRecyclerViewAdapter.isLoadMore()) {
                        mCurrentPage ++;
                        requestDataList();
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                mLastVisibleItem = mLinearLayoutManager.findLastVisibleItemPosition();
            }
        });

        mTitleView.setText(R.string.nav_favorite);

        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestDataList();
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        requestDataList();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.toolbar_back:
                finish();
                break;
            case R.id.ll_play_all:
                if(mMelodyBeanList.size() > 0) {
                    startPlayActivity(0);
                }
                break;
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        startPlayActivity(position);
    }

    @Override
    public void onLikeClick(int position, RecyclerViewMelodyListAdapter.Callback callback) {
        if(!isLogin()) {
            Toast.makeText(MyFavoriteActivity.this, R.string.login_tip, Toast.LENGTH_SHORT).show();
            return;
        }
        int accountId = mPreferencesManager.get(PreferenceConst.ACCOUNT_ID, -1);
        if(position >= 0 && position < mMelodyBeanList.size() && !vipIntercept(mMelodyBeanList.get(position))) {
            JSONObject param = new JSONObject();
            try {
                param.put("accountId", accountId);
                param.put("melodyId", mMelodyBeanList.get(position).id);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            HttpHelper.requestLikeMelody(mCoreContext, param, responseBean -> runOnUiThread(() -> {
                if(callback != null) {
                    if(responseBean.isState()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailed();
                    }
                }
            }));
        }
    }

    @Override
    public void onDownloadClick(int position, RecyclerViewMelodyListAdapter.Callback callback) {
        if(!isLogin()) {
            Toast.makeText(MyFavoriteActivity.this, R.string.login_tip, Toast.LENGTH_SHORT).show();
            return;
        }
        if(position >= 0 && position < mMelodyBeanList.size() && !vipIntercept(mMelodyBeanList.get(position))) {
            MelodyDetailBean bean = mMelodyBeanList.get(position);
            if(bean != null) {
                WeakReference<RecyclerViewMelodyListAdapter.Callback> weakReference = new WeakReference<>(callback);
                int taskState = mDownloadManager.addTask(bean.convertToMusicTrack());
                if(taskState == DownLoadManager.TaskState.TASK_OK) {
                    mDownloadManager.setAllTaskListener(new DownLoadListener() {
                        @Override
                        public void onStart(SQLDownLoadInfo sqlDownLoadInfo) {
                            runOnUiThread(() -> Toast.makeText(getApplicationContext(), R.string.downloading, Toast.LENGTH_SHORT).show());
                        }

                        @Override
                        public void onProgress(SQLDownLoadInfo sqlDownLoadInfo, boolean isSupportBreakpoint, int progress) {

                        }

                        @Override
                        public void onStop(SQLDownLoadInfo sqlDownLoadInfo, boolean isSupportBreakpoint) {

                        }

                        @Override
                        public void onError(SQLDownLoadInfo sqlDownLoadInfo) {

                        }

                        @Override
                        public void onSuccess(SQLDownLoadInfo sqlDownLoadInfo) {
                            if(weakReference.get() != null) {
                                weakReference.get().onSuccess();
                            }
                        }
                    });
                } else {
                    int toastRes = R.string.already_downloaded;
                    switch (taskState) {
                        case DownLoadManager.TaskState.TASK_EXIST:
                            toastRes = R.string.download_task_exist;
                            break;
                        case DownLoadManager.TaskState.TASK_MAX:
                            toastRes = R.string.download_over_max;
                            break;
                        case DownLoadManager.TaskState.TASK_COMPLETE:
                            toastRes = R.string.already_downloaded;
                            break;
                    }
                    Toast.makeText(getApplicationContext(), toastRes, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void startPlayActivity(final int position){
        if(position >=0 && position < mMelodyBeanList.size() && !vipIntercept(mMelodyBeanList.get(position))) {
            ArrayList<MusicTrack> dataList = new ArrayList<>();
            MusicTrack musicTrack = mMelodyBeanList.get(position).convertToMusicTrack();
            for(MelodyDetailBean bean: mMelodyBeanList) {
                dataList.add(bean.convertToMusicTrack());
            }
            Intent broadcast = new Intent(MediaService.PLAY_ACTION);
            broadcast.putParcelableArrayListExtra(PLAY_ACTION_PARAM_LIST, dataList);
            broadcast.putExtra(PLAY_ACTION_PARAM_POSITION, dataList.indexOf(musicTrack));
            sendBroadcast(broadcast);

            Intent intent = new Intent(getApplicationContext(), MusicPlayActivity.class);
            intent.putExtra(MusicPlayActivity.BUNDLE_DATA, mMelodyBeanList.get(position));
            startActivity(intent);
        }
    }

    private void requestDataList() {
        if(mCurrentPage == 1) {
            mSwipeRefreshLayout.setRefreshing(true);
        }
        int accountId = mPreferencesManager.get(PreferenceConst.ACCOUNT_ID, -1);
        if(accountId != -1) {
            try {
                JSONObject param = new JSONObject();
                param.put("accountId", accountId);
                param.put("pageSize", "8");
                param.put("page", String.valueOf(mCurrentPage));
                List<MelodyDetailBean> tempList = new ArrayList<>();
                HttpHelper.requestMelodyList(mCoreContext, param, tempList, DomainConst.FAVORITE_MELODY_LIST_URL,
                        responseBean -> runOnUiThread(() -> {
                    final String currentPage = responseBean.getCurrentPage();
                    final String pageSize = responseBean.getPageSize();
                    if(!StringUtil.isEmpty(currentPage)) {
                        if(currentPage.equals("1")) {
                            mMelodyBeanList.clear();
                        }
                        mMelodyBeanList.addAll(tempList);
                        if(Integer.valueOf(currentPage) >= Integer.valueOf(pageSize)) {
                            mMelodyRecyclerViewAdapter.setLoadMore(false);
                        }
                        retrieveDownloadedMelodyInfo();
                        mMelodyRecyclerViewAdapter.notifyDataSetChanged();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void retrieveDownloadedMelodyInfo() {
        int accountId = mPreferencesManager.get(PreferenceConst.ACCOUNT_ID, -1);
        if(accountId != -1) {
            ArrayList<SQLDownLoadInfo> downLoadInfoArrayList = mDownloadManager.getUserDownloadInfoList(String.valueOf(accountId));
            HashMap<String, SQLDownLoadInfo> localMelodyMap = new HashMap<>();
            for(SQLDownLoadInfo info:downLoadInfoArrayList) {
                localMelodyMap.put(info.getTaskID(), info);
            }
            for(MelodyDetailBean bean:mMelodyBeanList) {
                SQLDownLoadInfo info = localMelodyMap.get(String.valueOf(bean.id));
                if(info != null) {
                    bean.localUrl = info.getFilePath();
                }
            }
        }
    }

    private boolean isLogin() {
        int accountId = mPreferencesManager.get(PreferenceConst.ACCOUNT_ID, -1);
        return accountId != -1;
    }

    private boolean vipIntercept(MelodyDetailBean bean) {
        boolean isVip = mPreferencesManager.get(PreferenceConst.ACCOUNT_VIP, false);
        if(bean.isPrecious.equals("true") && !isVip) {
            Toast.makeText(MyFavoriteActivity.this, R.string.vip_tip, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }
}
