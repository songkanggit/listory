package com.listory.songkang.activity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.joker.annotation.PermissionsDenied;
import com.joker.annotation.PermissionsGranted;
import com.joker.annotation.PermissionsRationale;
import com.joker.annotation.PermissionsRequestSync;
import com.joker.api.Permissions4M;
import com.listory.songkang.adapter.RecyclerViewMelodyListAdapter;
import com.listory.songkang.adapter.ViewPagerAdapter;
import com.listory.songkang.bean.AlbumDetailBean;
import com.listory.songkang.bean.MelodyDetailBean;
import com.listory.songkang.constant.PermissionConstants;
import com.listory.songkang.constant.PreferenceConst;
import com.listory.songkang.container.NavigationTabStrip;
import com.listory.songkang.fragment.AlbumListFragment;
import com.listory.songkang.fragment.TextViewFragment;
import com.listory.songkang.helper.HttpHelper;
import com.listory.songkang.R;
import com.listory.songkang.service.MediaService;
import com.listory.songkang.service.MusicTrack;
import com.listory.songkang.utils.QiniuImageUtil;
import com.listory.songkang.view.CachedImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

@PermissionsRequestSync(permission = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
        value = {PermissionConstants.STORAGE_READ_CODE, PermissionConstants.STORAGE_WRITE_CODE})
public class AlbumActivity extends BaseActivity implements View.OnClickListener, RecyclerViewMelodyListAdapter.OnItemClickListener {

    public static final String BUNDLE_DATA = "data";
    private NavigationTabStrip mNavigationTab;
    private ViewPager mViewPager;
    private ImageView mBackView;
    private CachedImageView mAlbumCover;
    private AlbumListFragment mAlbumListFragment;
    private TextViewFragment mTextViewFragment;
    private List<Fragment> mViewPagerData;
    private List<MelodyDetailBean> mMelodyList;
    private TextView mTitleText;
    private CollapsingToolbarLayout mCollapseToolbar;
    private AlbumDetailBean mAlbumDetailBean;
    
    //==========================================Privilege request start====================================
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[]
            grantResults) {
        Permissions4M.onRequestPermissionsResult(AlbumActivity.this, requestCode, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @PermissionsGranted({PermissionConstants.STORAGE_READ_CODE, PermissionConstants.STORAGE_WRITE_CODE})
    public void syncGranted(int code) {
    }

    @PermissionsDenied({PermissionConstants.STORAGE_READ_CODE, PermissionConstants.STORAGE_WRITE_CODE})
    public void syncDenied(int code) {
        Toast.makeText(AlbumActivity.this, "授权失败", Toast.LENGTH_SHORT).show();
    }

    @PermissionsRationale({PermissionConstants.STORAGE_READ_CODE, PermissionConstants.STORAGE_WRITE_CODE})
    public void syncRationale(int code) {
        Toast.makeText(AlbumActivity.this, "请开启存储授权", Toast.LENGTH_SHORT).show();
    }
    //==========================================Privilege request end====================================

    protected void parseNonNullBundle(Bundle bundle){
        mAlbumDetailBean = (AlbumDetailBean) bundle.get(BUNDLE_DATA);
    }
    protected void initDataIgnoreUi() {
        mViewPagerData = new ArrayList<>();
        mMelodyList = new ArrayList<>();
        mViewPagerData.add(mTextViewFragment = new TextViewFragment());
        mViewPagerData.add(mAlbumListFragment = new AlbumListFragment());
        mAlbumListFragment.setData(mAlbumDetailBean);
    }
    @LayoutRes
    protected int getLayoutResourceId() { return R.layout.activity_album;}
    protected void viewAffairs(){
        mNavigationTab = fvb(R.id.navigation_tab_strip);
        mAlbumCover = fvb(R.id.iv_album);
        mBackView = fvb(R.id.toolbar_back);
        mViewPager = fvb(R.id.view_pager);
        mTitleText = fvb(R.id.tv_album_name);
        mCollapseToolbar = fvb(R.id.collapse_toolbar);
    }
    protected void assembleViewClickAffairs(){
        mBackView.setOnClickListener(this);
    }
    protected void initDataAfterUiAffairs(){
        mViewPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager(), mViewPagerData));
        mNavigationTab.setViewPager(mViewPager, 0);
        mNavigationTab.setActiveColor(Color.parseColor("#fbc600"));
        mNavigationTab.setInactiveColor(Color.parseColor("#333333"));
        mTextViewFragment.setText(mAlbumDetailBean.albumAbstract);
        mAlbumCover.setImageUrl(mAlbumDetailBean.albumCoverImage + QiniuImageUtil.generateFixSizeImageAppender(mContext, QiniuImageUtil.ImageType.ALBUM_RECT));
        mTitleText.setText(mAlbumDetailBean.albumName);
        mCollapseToolbar.setTitle(mAlbumDetailBean.albumName);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.toolbar_back:
                finish();
                break;
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        ArrayList<MusicTrack> dataList = new ArrayList<>();
        List<MelodyDetailBean> beanList = mAlbumListFragment.getDataList();
        for(MelodyDetailBean bean: beanList) {
            dataList.add(bean.convertToMusicTrack());
        }
        Intent broadcast = new Intent(MediaService.PLAY_ACTION);
        broadcast.putParcelableArrayListExtra(MediaService.PLAY_ACTION_PARAM_LIST, dataList);
        broadcast.putExtra(MediaService.PLAY_ACTION_PARAM_POSITION, position);
        sendBroadcast(broadcast);

        Intent intent = new Intent(AlbumActivity.this, MusicPlayActivity.class);
        intent.putExtra(MusicPlayActivity.BUNDLE_DATA, beanList.get(position));
        intent.putExtra(MusicPlayActivity.BUNDLE_DATA_PLAY, true);
        startActivity(intent);
    }

    @Override
    public void onLikeClick(int position, RecyclerViewMelodyListAdapter.Callback callback) {
        List<MelodyDetailBean> beanList = mAlbumListFragment.getDataList();
        if(playStateIntercept(true) && position >= 0 && position < beanList.size()) {
            WeakReference<RecyclerViewMelodyListAdapter.Callback> weakReference = new WeakReference<>(callback);
            JSONObject param = new JSONObject();
            try {
                param.put("accountId", mPreferencesManager.get(PreferenceConst.ACCOUNT_ID, -1));
                param.put("melodyId", beanList.get(position).id);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            HttpHelper.requestLikeMelody(mCoreContext, param, responseBean -> runOnUiThread(() -> {
                if(weakReference.get() != null) {
                    if(responseBean.isState()) {
                        weakReference.get().onSuccess();
                    } else {
                        weakReference.get().onFailed();
                    }
                }
            }));
        }
    }

    @Override
    public void onDownloadClick(int position, RecyclerViewMelodyListAdapter.Callback callback) {

    }

    private void startChargeVipActivity() {
//        Intent intent = new Intent(AlbumActivity.this, ChargeVipActivity.class);
//        startActivity(intent);
    }

    private boolean playStateIntercept(boolean redirectLogin) {
        int accountId = mPreferencesManager.get(PreferenceConst.ACCOUNT_ID, -1);
        if(!mAlbumDetailBean.isPrecious.equals("true") && !redirectLogin) {
            return true;
        }
        if(accountId == -1) {
            Intent intent = new Intent(AlbumActivity.this, LoginActivity.class);
            startActivity(intent);
            return false;
        } else {
            boolean isVip = mPreferencesManager.get(PreferenceConst.ACCOUNT_VIP, false);
            if(!isVip && mAlbumDetailBean.isPrecious.equals("true")) {
                startChargeVipActivity();
                return false;
            }
        }
        return true;
    }
}
