package com.listory.songkang.activity;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.listory.songkang.adapter.ViewPagerAdapter;
import com.listory.songkang.application.RealApplication;
import com.listory.songkang.bean.Melody;
import com.listory.songkang.container.NavigationTabStrip;
import com.listory.songkang.fragment.AlbumListFragment;
import com.listory.songkang.fragment.TextViewFragment;
import com.listory.songkang.listory.R;


import java.util.ArrayList;
import java.util.List;

public class AlbumActivity extends BaseActivity implements View.OnClickListener {

    public static final String BUNDLE_DATA = "data";
    public static final String BUNDLE_DATA_TYPE = "data_type";
    private NavigationTabStrip mNavigationTab;
    private ViewPager mViewPager;
    private ViewPagerAdapter mPagerAdapter;
    private ImageView mBackView, mAlbumCover;
    private AlbumListFragment mAlbumListFragment;
    private TextViewFragment mTextViewFragment;
    private List<Fragment> mViewPagerData;
    private List<Melody> mMelodyList;
    private TextView mTitleText;
    @RealApplication.MediaContent
    private int dataType = RealApplication.MediaContent.WILL_YOUTH;

    protected void parseNonNullBundle(Bundle bundle){
        mMelodyList = bundle.getParcelableArrayList(BUNDLE_DATA);
        dataType = bundle.getInt(BUNDLE_DATA_TYPE);
    }
    protected void initDataIgnoreUi() {
        mViewPagerData = new ArrayList<>();
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
    }
    protected void assembleViewClickAffairs(){
        mBackView.setOnClickListener(this);
    }
    protected void initDataAfterUiAffairs(){
        mViewPager.setAdapter(mPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), mViewPagerData));
        mNavigationTab.setViewPager(mViewPager, 0);
        if(dataType == RealApplication.MediaContent.WILL_YOUTH) {
            mTextViewFragment.setText(R.string.will_youth_abstract);
            mAlbumCover.setImageResource(R.mipmap.will_youth_album);
            mTitleText.setText(R.string.will_youth);
        } else {
            mTextViewFragment.setText(R.string.mr_black_abstract);
            mAlbumCover.setImageResource(R.mipmap.mr_black_album);
            mTitleText.setText(R.string.mr_black);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.toolbar_back:
                finish();
                break;
        }
    }
}
