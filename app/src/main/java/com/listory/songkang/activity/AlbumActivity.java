package com.listory.songkang.activity;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;

import com.listory.songkang.adapter.ViewPagerAdapter;
import com.listory.songkang.bean.Melody;
import com.listory.songkang.container.NavigationTabStrip;
import com.listory.songkang.fragment.AlbumListFragment;
import com.listory.songkang.fragment.TextViewFragment;
import com.listory.songkang.listory.R;


import java.util.ArrayList;
import java.util.List;

public class AlbumActivity extends BaseActivity implements View.OnClickListener {
    private NavigationTabStrip mNavigationTab;
    private ViewPager mViewPager;
    private ViewPagerAdapter mPagerAdapter;
    private ImageView mBackView;
    private AlbumListFragment mAlbumListFragment;
    private List<Fragment> mViewPagerData;
    private List<Melody> mMelodyList;

    protected void parseNonNullBundle(Bundle bundle){

    }
    protected void initDataIgnoreUi() {
        mViewPagerData = new ArrayList<>();
        mViewPagerData.add(new TextViewFragment());
        mViewPagerData.add(mAlbumListFragment = new AlbumListFragment());
    }
    @LayoutRes
    protected int getLayoutResourceId() { return R.layout.activity_album;}
    protected void viewAffairs(){
        mNavigationTab = fvb(R.id.navigation_tab_strip);
        mBackView = fvb(R.id.toolbar_back);
        mViewPager = fvb(R.id.view_pager);
    }
    protected void assembleViewClickAffairs(){
        mBackView.setOnClickListener(this);
    }
    protected void initDataAfterUiAffairs(){
        mViewPager.setAdapter(mPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), mViewPagerData));
        mNavigationTab.setViewPager(mViewPager, 0);
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
