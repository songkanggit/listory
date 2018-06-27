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

import com.listory.songkang.adapter.RecyclerViewMelodyListSwipeAdapter;
import com.listory.songkang.bean.MelodyDetailBean;
import com.listory.songkang.bean.SQLDownLoadInfo;
import com.listory.songkang.constant.PreferenceConst;
import com.listory.songkang.helper.HttpHelper;
import com.listory.songkang.listory.R;
import com.listory.songkang.service.MediaService;
import com.listory.songkang.service.MusicTrack;
import com.listory.songkang.service.downloader.DownLoadManager;
import com.listory.songkang.service.downloader.DownLoadService;
import com.listory.songkang.view.layout.SwipeItemLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.listory.songkang.service.MediaService.PLAY_ACTION_PARAM_LIST;
import static com.listory.songkang.service.MediaService.PLAY_ACTION_PARAM_POSITION;


public class MyDownLoadActivity extends BaseActivity implements View.OnClickListener, RecyclerViewMelodyListSwipeAdapter.OnItemClickListener{
    private RecyclerViewMelodyListSwipeAdapter mMelodyRecyclerViewAdapter;
    private RecyclerView mMelodyRecyclerView;
    private List<MelodyDetailBean> mMelodyBeanList;
    private ImageView mBackButton;
    private TextView mTitleView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private LinearLayout mPlayAllLL;
    private ArrayList<SQLDownLoadInfo> mDownLoadInfoList;

    protected void parseNonNullBundle(Bundle bundle){

    }

    protected void initDataIgnoreUi() {
        mMelodyBeanList = new ArrayList<>();
    }
    @LayoutRes
    protected int getLayoutResourceId() { return R.layout.activity_my_download;}
    protected void viewAffairs(){
        mMelodyRecyclerView = fvb(R.id.recycler_view);
        mBackButton = fvb(R.id.toolbar_back);
        mTitleView = fvb(R.id.toolbar_title);
        mSwipeRefreshLayout = fvb(R.id.swipe_refresh);
        mPlayAllLL = fvb(R.id.ll_play_all);
    }
    protected void assembleViewClickAffairs(){
        mPlayAllLL.setOnClickListener(this);
        mBackButton.setOnClickListener(this);
    }
    protected void initDataAfterUiAffairs(){
        mMelodyRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        mMelodyRecyclerView.setHasFixedSize(true);
        mMelodyRecyclerView.addOnItemTouchListener(new SwipeItemLayout.OnSwipeItemTouchListener(getApplicationContext()));
//        mMelodyRecyclerView.addItemDecoration(new NavItemActivity.LinearLayoutItemDecoration(20, 2, getResources().getColor(R.color.colorF4F5F7)));
        mMelodyRecyclerView.setAdapter(mMelodyRecyclerViewAdapter = new RecyclerViewMelodyListSwipeAdapter(MyDownLoadActivity.this, mMelodyBeanList));
        mMelodyRecyclerViewAdapter.setOnItemClickListener(this);
        mTitleView.setText(R.string.download_activity_title);
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestDownloadData();
            }
        });
        requestDownloadData();
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
    public void onLikeClick(int position, RecyclerViewMelodyListSwipeAdapter.Callback callback) {
        int accountId = mPreferencesManager.get(PreferenceConst.ACCOUNT_ID, -1);
        if(accountId != -1 && position >= 0 && position < mMelodyBeanList.size()) {
            JSONObject param = new JSONObject();
            try {
                param.put("accountId", accountId);
                param.put("melodyId", mMelodyBeanList.get(position).id);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            HttpHelper.requestLikeMelody(mCoreContext, param, responseBean -> runOnUiThread(() -> {
                DownLoadManager manager = DownLoadService.getDownLoadManager();
                SQLDownLoadInfo downLoadInfo = null;
                if(position >= 0 && position < mDownLoadInfoList.size()) {
                    downLoadInfo = mDownLoadInfoList.get(position);
                }
                if(callback != null && responseBean != null) {
                    if(responseBean.isState()) {
                        callback.onSuccess();
                        if(downLoadInfo != null) {
                            downLoadInfo.setLike("true");
                        }
                    } else {
                        callback.onFailed();
                        if(downLoadInfo != null) {
                            downLoadInfo.setLike("false");
                        }
                    }
                }
                if(downLoadInfo != null) {
                    manager.updateSQLDownLoadInfo(downLoadInfo);
                }
            }));
        }
    }

    @Override
    public void onDownloadClick(int position, RecyclerViewMelodyListSwipeAdapter.Callback callback) {
        Toast.makeText(MyDownLoadActivity.this, R.string.already_downloaded, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteClick(int position) {
        DownLoadManager downLoadManager = DownLoadService.getDownLoadManager();
        int accountId = mPreferencesManager.get(PreferenceConst.ACCOUNT_ID, -1);
        if(accountId != -1 && position >= 0 && position < mMelodyBeanList.size()) {
            final String userId = String.valueOf(accountId);
            final String taskId = String.valueOf(mMelodyBeanList.get(position).id);
            if(downLoadManager.deleteUserDownloadMelody(userId, taskId)) {
                Toast.makeText(MyDownLoadActivity.this, R.string.app_delete_success, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MyDownLoadActivity.this, R.string.app_delete_failed, Toast.LENGTH_SHORT).show();
            }
            requestDownloadData();
        }
    }

    private void startPlayActivity(final int position){
        if(position >=0 && position < mMelodyBeanList.size()) {
            ArrayList<MusicTrack> dataList = new ArrayList<>();
            for(MelodyDetailBean bean: mMelodyBeanList) {
                dataList.add(bean.convertToMusicTrack());
            }
            Intent broadcast = new Intent(MediaService.PLAY_ACTION);
            broadcast.putParcelableArrayListExtra(PLAY_ACTION_PARAM_LIST, dataList);
            broadcast.putExtra(PLAY_ACTION_PARAM_POSITION, position);
            sendBroadcast(broadcast);

            Intent intent = new Intent(getApplicationContext(), MusicPlayActivity.class);
            intent.putExtra(MusicPlayActivity.BUNDLE_DATA, mMelodyBeanList.get(position));
            startActivity(intent);
        }
    }

    private void requestDownloadData() {
        mSwipeRefreshLayout.setRefreshing(true);
        DownLoadManager downLoadManager = DownLoadService.getDownLoadManager();
        int accountId = mPreferencesManager.get(PreferenceConst.ACCOUNT_ID, -1);
        if(accountId != -1) {
            mDownLoadInfoList = downLoadManager.getUserDownloadInfoList(String.valueOf(accountId));
            mMelodyBeanList.clear();
            for(SQLDownLoadInfo info:mDownLoadInfoList) {
                MelodyDetailBean bean = new MelodyDetailBean();
                bean.id = Long.valueOf(info.getTaskID());
                bean.title = info.getFileName();
                bean.url = info.getUrl();
                bean.localUrl = info.getFilePath();
                bean.coverImageUrl = info.getIcon();
                bean.artist = info.getAuthor();
                bean.favorite = info.getLike();
                mMelodyBeanList.add(bean);
            }
            mMelodyRecyclerViewAdapter.notifyDataSetChanged();
        }
        mSwipeRefreshLayout.setRefreshing(false);
    }
}
