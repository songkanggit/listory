package com.listory.songkang.activity;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.listory.songkang.container.NavigationTabStrip;
import com.listory.songkang.listory.R;
import com.listory.songkang.utils.DensityUtil;


import java.util.ArrayList;
import java.util.List;

public class AlbumActivity extends BaseActivity implements View.OnClickListener {
    private NavigationTabStrip mNavigationTab;
    private ViewPager mViewPager;
    private ImageView mBackView;
    private List<View> mViewPagerData;
    private TextView mAbstractText;
    private RecyclerView mRecyclerView;

    protected void parseNonNullBundle(Bundle bundle){

    }
    protected void initDataIgnoreUi() {
        mViewPagerData = new ArrayList<>();
        mAbstractText = new TextView(getApplicationContext());
        mAbstractText.setText(R.string.will_youth_abstract);
        mAbstractText.setTextSize(DensityUtil.dip2px(getApplicationContext(), 14));
        mAbstractText.setTextColor(getResources().getColor(R.color.color666666));

        mRecyclerView = new RecyclerView(getApplicationContext());
        mViewPagerData.add(mAbstractText);
        mViewPagerData.add(mRecyclerView);
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
        mViewPager.setAdapter(new PagerAdapter() {
            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                View view = mViewPagerData.get(position);
                container.addView(view);
                return view;
            }

            @Override
            public int getCount() {
                return mViewPagerData.size();
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                View view = (View) object;
                container.removeView(view);
            }
        });
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
