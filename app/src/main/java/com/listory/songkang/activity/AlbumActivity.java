package com.listory.songkang.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.listory.songkang.adapter.ViewPagerAdapter;
import com.listory.songkang.bean.AlbumDetailBean;
import com.listory.songkang.bean.MelodyDetailBean;
import com.listory.songkang.constant.DomainConst;
import com.listory.songkang.container.NavigationTabStrip;
import com.listory.songkang.fragment.AlbumListFragment;
import com.listory.songkang.fragment.TextViewFragment;
import com.listory.songkang.listory.R;
import com.listory.songkang.utils.StringUtil;
import com.listory.songkang.view.CachedImageView;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AlbumActivity extends BaseActivity implements View.OnClickListener {

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

    protected void parseNonNullBundle(Bundle bundle){
        mAlbumDetailBean = (AlbumDetailBean) bundle.get(BUNDLE_DATA);
    }
    protected void initDataIgnoreUi() {
        mViewPagerData = new ArrayList<>();
        mMelodyList = new ArrayList<>();
        mViewPagerData.add(mTextViewFragment = new TextViewFragment());
        mViewPagerData.add(mAlbumListFragment = new AlbumListFragment());
        mAlbumListFragment.setData(mMelodyList);
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
        requestDataList();
        mViewPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager(), mViewPagerData));
        mNavigationTab.setViewPager(mViewPager, 0);
        mNavigationTab.setActiveColor(Color.parseColor("#fbc600"));
        mNavigationTab.setInactiveColor(Color.parseColor("#333333"));
        mTextViewFragment.setText(mAlbumDetailBean.albumAbstract);
        mAlbumCover.setImageUrl(mAlbumDetailBean.albumCoverImage);
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

    private void requestDataList() {
        mCoreContext.executeAsyncTask(() -> {
            if(!StringUtil.isEmpty(mAlbumDetailBean.albumName)) {
                try {
                    JSONObject param = new JSONObject();
                    param.put("melodyAlbum", mAlbumDetailBean.albumName);
                    param.put("pageSize", "8");
                    param.put("page", String.valueOf(1));
                    String response = mHttpService.post(DomainConst.MELODY_LIST_URL, param.toString());
                    JSONObject responseObject = new JSONObject(response);
                    String code = responseObject.getString("code");
                    if (code != null && code.equals(DomainConst.CODE_OK)) {
                        JSONObject dataObject = responseObject.getJSONObject("data");
                        JSONArray dataArray = dataObject.getJSONArray("melodyList");
                        List<MelodyDetailBean> tempList = new ArrayList<>();
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject temp = dataArray.getJSONObject(i);
                            MelodyDetailBean bean = new MelodyDetailBean();
                            bean.id = temp.getLong("id");
                            bean.url = DomainConst.DOMAIN + temp.getString("melodyFilePath");
                            bean.coverImageUrl = DomainConst.DOMAIN + temp.getString("melodyCoverImage");
                            bean.albumName = temp.getString("melodyAlbum");
                            bean.title = temp.getString("melodyName");
                            bean.artist = temp.getString("melodyArtist");
                            bean.favorite = temp.getString("favorated");
                            bean.tags = temp.getString("melodyCategory");
                            bean.isPrecious = temp.getString("melodyPrecious");
                            bean.mItemTitle = bean.title;
                            bean.mItemIconUrl = bean.coverImageUrl;
                            bean.mTags = bean.tags;
                            bean.mPrecious = bean.isPrecious;
                            tempList.add(bean);
                        }
                        runOnUiThread(() -> {
                            if (tempList.size() > 0) {
                                mMelodyList.clear();
                                mMelodyList.addAll(tempList);
                                mAlbumListFragment.notifyDataChange();
                            }
                        });
                    }
                    } catch(JSONException e){
                        e.printStackTrace();
                    } catch(IOException e){
                        e.printStackTrace();
                    }
                }
        });
    }
}
