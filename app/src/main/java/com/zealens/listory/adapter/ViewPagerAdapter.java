package com.zealens.listory.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

/**
 * Created by songkang on 2018/1/7.
 */

public class ViewPagerAdapter extends FragmentPagerAdapter {


    private List<Fragment> mFragmentList;

    public ViewPagerAdapter(FragmentManager fm, List<Fragment> fragList) {
        super(fm);
        mFragmentList=fragList;
    }

    @Override
    public Fragment getItem(int arg0) {
        return mFragmentList.get(arg0);
    }
    @Override
    public int getCount() {
        return mFragmentList.size();
    }
}

