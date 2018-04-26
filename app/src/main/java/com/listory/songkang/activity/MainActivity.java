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
import com.listory.songkang.service.MusicPlayer;
import com.listory.songkang.transformer.ScaleInTransformer;
import com.listory.songkang.utils.PermissionUtil;
import com.listory.songkang.view.AvatarCircleView;


public class MainActivity extends BaseActivity implements View.OnClickListener {

    private DrawerLayout mDrawerLayout;
    private int[] imgRes = {R.mipmap.will_youth, R.mipmap.mr_black, R.mipmap.will_youth, R.mipmap.mr_black,
            R.mipmap.will_youth, R.mipmap.mr_black};
    private PagerAdapter mAdapter;
    private ViewPager mViewPager;
    private AvatarCircleView mCircleView;
    private ImageView mPlayControlImageView;

    protected void parseNonNullBundle(Bundle bundle){

    }
    protected void initDataIgnoreUi() {
        if(!MusicPlayer.getInstance().isServiceConnected()) {
            MusicPlayer.getInstance().bindMediaService(getApplicationContext());
        }
        PermissionUtil.verifyStoragePermissions(MainActivity.this);
    }
    @LayoutRes
    protected int getLayoutResourceId() { return R.layout.activity_main;}
    protected void viewAffairs(){
        mViewPager = fvb(R.id.id_viewpager);
        mDrawerLayout = fvb(R.id.contentPanel);
        mCircleView = fvb(R.id.circle_view);
        mPlayControlImageView = fvb(R.id.iv_play);
    }
    protected void assembleViewClickAffairs(){
        mCircleView.setOnClickListener(this);
        mPlayControlImageView.setOnClickListener(this);
    }
    protected void initDataAfterUiAffairs(){
        mViewPager.setPageMargin(20);
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setAdapter(mAdapter = new HomePageAdapter());
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.circle_view:
                Intent intent = new Intent(MainActivity.this, MusicPlayerActivity.class);
                startActivity(intent);
                break;
            case R.id.iv_play:
                if(MusicPlayer.getInstance().isPlaying()) {
                    MusicPlayer.getInstance().pause();
                    mPlayControlImageView.setImageResource(R.mipmap.bottom_player_play);
                } else {
                    MusicPlayer.getInstance().play();
                    mPlayControlImageView.setImageResource(R.mipmap.bottom_player_pause);
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MusicPlayer.getInstance().unBindMediaService(getApplicationContext());
    }

    private class HomePageAdapter extends PagerAdapter {
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
                    Intent intent = new Intent(MainActivity.this, AlbumActivity.class);
                    startActivity(intent);
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
    }

}
