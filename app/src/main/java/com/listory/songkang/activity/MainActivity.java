package com.listory.songkang.activity;

import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.listory.songkang.listory.R;
import com.listory.songkang.transformer.ScaleInTransformer;


public class MainActivity extends BaseActivity {

    private DrawerLayout mDrawerLayout;
    private int[] imgRes = {R.mipmap.view_page_01, R.mipmap.view_page_02, R.mipmap.view_page_03, R.mipmap.view_page_01,
            R.mipmap.view_page_02, R.mipmap.view_page_03};
    private PagerAdapter mAdapter;
    private ViewPager mViewPager;

    protected void parseNonNullBundle(Bundle bundle){

    }
    protected void initDataIgnoreUi() {

    }
    @LayoutRes
    protected int getLayoutResourceId() { return R.layout.activity_main;}
    protected void viewAffairs(){
        mViewPager = fvb(R.id.id_viewpager);
        mDrawerLayout = fvb(R.id.contentPanel);
    }
    protected void assembleViewClickAffairs(){

    }
    protected void initDataAfterUiAffairs(){
        mViewPager.setPageMargin(0);
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setAdapter(mAdapter = new PagerAdapter()
        {
            @Override
            public Object instantiateItem(ViewGroup container, int position)
            {
                ImageView view = new ImageView(MainActivity.this);
                final int realPosition = getRealPosition(position);
                view.setImageResource(imgRes[realPosition]);
                container.addView(view);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startMusicPlayer();
                    }
                });
                return view;
            }


            @Override
            public int getItemPosition(Object object) {
                return POSITION_NONE;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object)
            {
                container.removeView((View) object);
            }

            @Override
            public int getCount()
            {
                return Integer.MAX_VALUE;
            }

            @Override
            public boolean isViewFromObject(View view, Object o)
            {
                return view == o;
            }

            @Override
            public void startUpdate(ViewGroup container) {
                super.startUpdate(container);
                ViewPager viewPager = (ViewPager) container;
                int position = viewPager.getCurrentItem();
                if (position == 0) {
                    position = getFirstItemPosition();
                } else if (position == getCount() - 1) {
                    position = getLastItemPosition();
                }
                viewPager.setCurrentItem(position, false);

            }

            private int getRealCount() {
                return imgRes.length;
            }

            private int getRealPosition(int position) {
                return position % getRealCount();
            }

            private int getFirstItemPosition() {
                return Integer.MAX_VALUE / getRealCount() / 2 * getRealCount();
            }

            private int getLastItemPosition() {
                return Integer.MAX_VALUE / getRealCount() / 2 * getRealCount() - 1;
            }
        });

        mViewPager.setPageTransformer(true, new ScaleInTransformer());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                break;
        }
        return true;
    }

    private void startMusicPlayer() {
        Intent intent = new Intent(MainActivity.this, MusicPlayerActivity.class);
        startActivity(intent);
    }

}
