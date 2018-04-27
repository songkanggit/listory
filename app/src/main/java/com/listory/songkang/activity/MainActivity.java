package com.listory.songkang.activity;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.LayoutRes;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.listory.songkang.application.RealApplication;
import com.listory.songkang.bean.Melody;
import com.listory.songkang.listory.R;
import com.listory.songkang.service.MediaService;
import com.listory.songkang.service.MusicPlayer;
import com.listory.songkang.service.MusicTrack;
import com.listory.songkang.transformer.ScaleInTransformer;
import com.listory.songkang.utils.PermissionUtil;
import com.listory.songkang.view.AvatarCircleView;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends BaseActivity implements View.OnClickListener, MusicPlayer.ConnectionState {

    private DrawerLayout mDrawerLayout;
    private int[] imgRes = {R.mipmap.will_youth, R.mipmap.mr_black, R.mipmap.will_youth, R.mipmap.mr_black,
            R.mipmap.will_youth, R.mipmap.mr_black};
    private ViewPager mViewPager;
    private AvatarCircleView mCircleView;
    private ImageView mPlayControlImageView;
    private TextView mMelodyNameTV;
    private ObjectAnimator mRotateObjectAnimation;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case MediaService.MUSIC_CHANGE_ACTION:
                    MusicTrack musicTrack = intent.getParcelableExtra(MediaService.MUSIC_CHANGE_ACTION_PARAM);
                    if(musicTrack != null) {
                        mCircleView.setImageBitmap(BitmapFactory.
                                decodeFile(musicTrack.mCoverImageUrl.split(";")[1]));
                        mMelodyNameTV.setText(musicTrack.mTitle);
                    }
                    break;
            }
        }
    };

    protected void parseNonNullBundle(Bundle bundle){

    }
    protected void initDataIgnoreUi() {
        PermissionUtil.verifyStoragePermissions(MainActivity.this);
        IntentFilter intentFilter = new IntentFilter(MediaService.MUSIC_CHANGE_ACTION);
        registerReceiver(mIntentReceiver, intentFilter);
    }
    @LayoutRes
    protected int getLayoutResourceId() { return R.layout.activity_main;}
    protected void viewAffairs(){
        mViewPager = fvb(R.id.id_viewpager);
        mDrawerLayout = fvb(R.id.contentPanel);
        mPlayControlImageView = fvb(R.id.iv_play);

        mCircleView = fvb(R.id.circle_view);
        mMelodyNameTV = fvb(R.id.tv_melody_name);
    }
    protected void assembleViewClickAffairs(){
        mCircleView.setOnClickListener(this);
        mPlayControlImageView.setOnClickListener(this);
    }
    protected void initDataAfterUiAffairs(){
        mViewPager.setPageMargin(20);
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setAdapter(new HomePageAdapter());
        mViewPager.setPageTransformer(true, new ScaleInTransformer());

        mRotateObjectAnimation = ObjectAnimator.ofFloat(mCircleView, "rotation", 0f, 360.0f);
        mRotateObjectAnimation.setDuration(6000);
        mRotateObjectAnimation.setInterpolator(new LinearInterpolator());
        mRotateObjectAnimation.setRepeatCount(-1);
        mRotateObjectAnimation.setRepeatMode(ValueAnimator.RESTART);

        MusicPlayer.getInstance().bindMediaService(getApplicationContext());
        MusicPlayer.getInstance().addConnectionCallback(this);
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
                intent.putExtra(MusicPlayerActivity.BUNDLE_DATA, ((RealApplication)getApplication()).getMelodyContent(RealApplication.MediaContent.WILL_YOUTH).get(0));
                intent.putExtra(MusicPlayerActivity.BUNDLE_DATA_PLAY, false);
                startActivity(intent);
                break;
            case R.id.iv_play:
                togglePauseResume();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(MusicPlayer.getInstance().isPlaying()) {
            mPlayControlImageView.setImageResource(R.mipmap.bottom_player_pause);
            togglePauseResumeAnimation(true);
        } else {
            mPlayControlImageView.setImageResource(R.mipmap.bottom_player_play);
            togglePauseResumeAnimation(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        togglePauseResumeAnimation(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mIntentReceiver);
        MusicPlayer.getInstance().unBindMediaService(getApplicationContext());
    }

    @Override
    public void onServiceConnected() {
        ArrayList<MusicTrack> dataList = new ArrayList<>();
        ArrayList<Melody> melodies = ((RealApplication)getApplication()).getMelodyContent(RealApplication.MediaContent.WILL_YOUTH);
        for(Melody bean: melodies) {
            dataList.add(bean.convertToMusicTrack());
        }
        Intent broadcast = new Intent(MediaService.PLAY_ACTION);
        broadcast.putParcelableArrayListExtra(MediaService.PLAY_ACTION_PARAM_LIST, dataList);
        broadcast.putExtra(MediaService.PLAY_ACTION_PARAM_POSITION, 0);
        sendBroadcast(broadcast);
    }

    @Override
    public void onServiceDisconnected() {

    }

    private void togglePauseResume() {
        if(MusicPlayer.getInstance().isPlaying()) {
            MusicPlayer.getInstance().pause();
            togglePauseResumeAnimation(false);
            mPlayControlImageView.setImageResource(R.mipmap.bottom_player_play);
        } else {
            MusicPlayer.getInstance().play();
            togglePauseResumeAnimation(true);
            mPlayControlImageView.setImageResource(R.mipmap.bottom_player_pause);
        }
    }

    private void togglePauseResumeAnimation(boolean rotate) {
        if(rotate) {
            if(!mRotateObjectAnimation.isStarted()) {
                mRotateObjectAnimation.start();
            } else {
                if(Build.VERSION.SDK_INT > 18) {
                    mRotateObjectAnimation.resume();
                }
            }
        } else {
            if(Build.VERSION.SDK_INT > 18) {
                mRotateObjectAnimation.pause();
            }
        }
    }

    private class HomePageAdapter extends PagerAdapter {
        @Override
        public Object instantiateItem(ViewGroup container, int position)
        {
            ImageView view = new ImageView(MainActivity.this);
            final int realPosition = getRealPosition(position);
            view.setImageResource(imgRes[realPosition]);
            container.addView(view);
            view.setOnClickListener(v -> {
                int contentType = RealApplication.MediaContent.WILL_YOUTH;
                if(position%2 == 1) {
                    contentType = RealApplication.MediaContent.MR_BLACK;
                }
                ArrayList<Melody> melodies = ((RealApplication)getApplication()).getMelodyContent(contentType);
                Intent intent = new Intent(MainActivity.this, AlbumActivity.class);
                intent.putParcelableArrayListExtra(AlbumActivity.BUNDLE_DATA, melodies);
                intent.putExtra(AlbumActivity.BUNDLE_DATA_TYPE, contentType);
                startActivity(intent);
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
