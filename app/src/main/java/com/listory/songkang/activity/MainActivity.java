package com.listory.songkang.activity;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.LayoutRes;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.activity.CaptureActivity;
import com.joker.annotation.PermissionsDenied;
import com.joker.annotation.PermissionsGranted;
import com.joker.annotation.PermissionsRationale;
import com.joker.annotation.PermissionsRequestSync;
import com.joker.api.Permissions4M;
import com.listory.songkang.activity.coupon.CouponActivity;
import com.listory.songkang.bean.AlbumDetailBean;
import com.listory.songkang.bean.BannerItemBean;
import com.listory.songkang.bean.HttpResponseBean;
import com.listory.songkang.bean.MelodyDetailBean;
import com.listory.songkang.constant.DomainConst;
import com.listory.songkang.constant.PermissionConstants;
import com.listory.songkang.constant.PreferenceConst;
import com.listory.songkang.R;
import com.listory.songkang.service.MediaService;
import com.listory.songkang.service.MusicPlayer;
import com.listory.songkang.service.MusicTrack;
import com.listory.songkang.core.download.DownLoadManager;
import com.listory.songkang.transformer.ScaleInTransformer;
import com.listory.songkang.utils.DensityUtil;
import com.listory.songkang.utils.QiniuImageUtil;
import com.listory.songkang.utils.StringUtil;
import com.listory.songkang.view.AutoLoadImageView;
import com.listory.songkang.view.AvatarCircleView;

import org.intellij.lang.annotations.MagicConstant;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.listory.songkang.service.MediaService.PLAY_ACTION_PARAM_LIST;
import static com.listory.songkang.service.MediaService.PLAY_ACTION_PARAM_POSITION;

@PermissionsRequestSync(permission = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
        value = {PermissionConstants.STORAGE_READ_CODE, PermissionConstants.STORAGE_WRITE_CODE})
public class MainActivity extends BaseActivity implements View.OnClickListener, MusicPlayer.ConnectionState {

    private DrawerLayout mDrawerLayout;
    private List<BannerItemBean> mBannerItemList;
    private ViewPager mViewPager;
    private AvatarCircleView mCircleView, mHeadImageCircleView;
    private ImageView mPlayControlImageView, mToolbarOpen, mToolbarQR;
    private TextView mMelodyNameTV;
    private ObjectAnimator mRotateObjectAnimation;
    private MusicTrack mMusicTrack;
    private Bitmap mDefaultLoadBitMap;

    private RelativeLayout mContentRL;
    private LinearLayout mFavoriteLL, mDownloadLL, mVipLL, mCouponLL, mExitLL;
    private ImageView mVipFlagIV;
    private TextView mHintTV;
    private EditText mNameEditText;
    private int mAccountId;

    @MagicConstant(intValues = {BannerType.MELODY_TYPE, BannerType.ALBUM_TYPE, BannerType.BROWSER_TYPE})
    public @interface BannerType {
        int MELODY_TYPE = 0;
        int ALBUM_TYPE = 1;
        int BROWSER_TYPE = 2;
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case MediaService.PLAY_STATE_UPDATE:
                    MusicTrack musicTrack = intent.getParcelableExtra(MediaService.PLAY_STATE_UPDATE_DATA);
                    updatePlayInfo(musicTrack, false);
                    break;
            }
        }
    };

    //==========================================Privilege request start====================================
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[]
            grantResults) {
        Permissions4M.onRequestPermissionsResult(MainActivity.this, requestCode, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @PermissionsGranted({PermissionConstants.STORAGE_READ_CODE, PermissionConstants.STORAGE_WRITE_CODE})
    public void syncGranted(int code) {
    }

    @PermissionsDenied({PermissionConstants.STORAGE_READ_CODE, PermissionConstants.STORAGE_WRITE_CODE})
    public void syncDenied(int code) {
        Toast.makeText(MainActivity.this, "授权失败", Toast.LENGTH_SHORT).show();
    }

    @PermissionsRationale({PermissionConstants.STORAGE_READ_CODE, PermissionConstants.STORAGE_WRITE_CODE})
    public void syncRationale(int code) {
        Toast.makeText(MainActivity.this, "请开启存储授权", Toast.LENGTH_SHORT).show();
    }
    //==========================================Privilege request end====================================
    
    protected void parseNonNullBundle(Bundle bundle){

    }
    protected void initDataIgnoreUi() {
        mDefaultLoadBitMap = BitmapFactory.decodeResource(getResources(), R.mipmap.default_banner_bg);
        IntentFilter intentFilter = new IntentFilter(MediaService.PLAY_STATE_UPDATE);
        registerReceiver(mIntentReceiver, intentFilter);
        mBannerItemList = new ArrayList<>();
    }
    @LayoutRes
    protected int getLayoutResourceId() { return R.layout.activity_main;}
    protected void viewAffairs(){
        mViewPager = fvb(R.id.id_viewpager);
        mDrawerLayout = fvb(R.id.contentPanel);
        mPlayControlImageView = fvb(R.id.iv_play);
        mCircleView = fvb(R.id.circle_view);
        mHeadImageCircleView = fvb(R.id.head_image_view);
        mMelodyNameTV = fvb(R.id.tv_melody_name);
        mToolbarOpen = fvb(R.id.toolbar_nav);
        mToolbarQR = fvb(R.id.toolbar_qr);

        mContentRL = fvb(R.id.rl_content);
        mFavoriteLL = fvb(R.id.ll_favorite);
        mDownloadLL = fvb(R.id.ll_download);
        mVipLL = fvb(R.id.ll_vip);
        mCouponLL = fvb(R.id.ll_coupon);
        mExitLL = fvb(R.id.ll_exit);

        mVipFlagIV = fvb(R.id.iv_vip_flag);
        mHintTV = fvb(R.id.tv_hint);
        mNameEditText = fvb(R.id.edit_name);
    }
    protected void assembleViewClickAffairs(){
        mCircleView.setOnClickListener(this);
        mPlayControlImageView.setOnClickListener(this);
        mToolbarOpen.setOnClickListener(this);
        mToolbarQR.setOnClickListener(this);
        mContentRL.setOnClickListener(this);
        mFavoriteLL.setOnClickListener(this);
        mDownloadLL.setOnClickListener(this);
        mVipLL.setOnClickListener(this);
        mCouponLL.setOnClickListener(this);
        mExitLL.setOnClickListener(this);
        mHintTV.setOnClickListener(this);
        mHeadImageCircleView.setOnClickListener(this);
    }
    protected void initDataAfterUiAffairs(){
        requestBannerViewData();
        mCoreContext.executeAsyncTask(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject param = new JSONObject();
                    param.put("id", "582");
                    String response = mHttpService.post(DomainConst.MELODY_ITEM_URL, param.toString());
                    JSONObject responseObject = new JSONObject(response);
                    JSONObject temp = responseObject.getJSONObject("data");
                    MelodyDetailBean bean = new MelodyDetailBean();
                    bean.id = temp.getLong("id");
                    bean.url = DomainConst.MEDIA_DOMAIN + temp.getString("melodyFilePath");
                    bean.coverImageUrl = DomainConst.PICTURE_DOMAIN + temp.getString("melodyCoverImage");
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
                    mMusicTrack = bean.convertToMusicTrack();
                    runOnUiThread(() -> updatePlayInfo(mMusicTrack, true));
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        mViewPager.setPageMargin(20);
        mViewPager.setOffscreenPageLimit(3);
//        mViewPager.setAdapter(mHomePageAdapter = new HomePageAdapter());
        mViewPager.setPageTransformer(true, new ScaleInTransformer());

        mRotateObjectAnimation = ObjectAnimator.ofFloat(mCircleView, "rotation", 0f, 360.0f);
        mRotateObjectAnimation.setDuration(6000);
        mRotateObjectAnimation.setInterpolator(new LinearInterpolator());
        mRotateObjectAnimation.setRepeatCount(-1);
        mRotateObjectAnimation.setRepeatMode(ValueAnimator.RESTART);

        MusicPlayer.getInstance().bindMediaService(getApplicationContext());
        MusicPlayer.getInstance().addConnectionCallback(this);
        Permissions4M
                .get(MainActivity.this)
                .requestSync();
        setSwipeBackEnable(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUserInfo();
    }

    @Override
    public void onClick(View view) {
        final boolean isLogin = isLogin();
        switch (view.getId()) {
            case R.id.toolbar_nav:
            {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
                break;
            case R.id.toolbar_qr:
            {
                Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                startActivity(intent);
            }
                break;
            case R.id.circle_view: {
                ArrayList<MusicTrack> dataList = new ArrayList<>();
                dataList.add(mMusicTrack);
                Intent broadcast = new Intent(MediaService.PLAY_ACTION);
                broadcast.putParcelableArrayListExtra(PLAY_ACTION_PARAM_LIST, dataList);
                broadcast.putExtra(PLAY_ACTION_PARAM_POSITION, -1);
                sendBroadcast(broadcast);

                Intent intent = new Intent(MainActivity.this, MusicPlayActivity.class);
                startActivity(intent);
            }
                break;
            case R.id.iv_play:
                togglePauseResume();
                break;
            case R.id.ll_favorite:
            {
                if(isLogin) {
                    Intent intent = new Intent(MainActivity.this, MyFavoriteActivity.class);
                    startActivity(intent);
                } else {
                    startLoginActivity();
                }
            }
                break;
            case R.id.ll_download:
                if(isLogin) {
                    Intent intent = new Intent(MainActivity.this, MyDownLoadActivity.class);
                    startActivity(intent);
                } else {
                    startLoginActivity();
                }
                break;
            case R.id.ll_vip:
            {
                if(isLogin) {
                    Intent intent = new Intent(MainActivity.this, ChargeVipActivity.class);
                    startActivity(intent);
                } else {
                 startLoginActivity();
                }
            }
                break;
            case R.id.ll_coupon:
            {
                if(isLogin) {
                    Intent intent = new Intent(MainActivity.this, CouponActivity.class);
                    startActivity(intent);
                } else {
                    startLoginActivity();
                }
            }
            break;
            case R.id.ll_exit:
                if(isLogin) {
                    mPreferencesManager.put(PreferenceConst.ACCOUNT_ID, -1);
                    mPreferencesManager.put(PreferenceConst.ACCOUNT_VIP, false);
                    updateUserInfo();
                }
                break;
            case R.id.head_image_view:
            {
                if(!isLogin()) {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            }
                break;
            case R.id.tv_hint:
                mNameEditText.setEnabled(true);
                mNameEditText.requestFocus();
                mNameEditText.setSelection(mNameEditText.getText().length());
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                        .showSoftInput(mNameEditText, InputMethodManager.SHOW_IMPLICIT);
                break;
            case R.id.rl_content:
                if(mNameEditText.isEnabled()) {
                    mNameEditText.clearFocus();
                    mNameEditText.setEnabled(false);
                    ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                            .hideSoftInputFromWindow(mNameEditText.getWindowToken(),
                                    InputMethodManager.HIDE_NOT_ALWAYS);
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mIntentReceiver != null) {
            unregisterReceiver(mIntentReceiver);
            MusicPlayer.getInstance().unBindMediaService(getApplicationContext());
        }
        MusicPlayer.getInstance().removeConnectionCallback(this);
        if(mDefaultLoadBitMap != null) {
            mDefaultLoadBitMap.recycle();
            mDefaultLoadBitMap = null;
        }
    }

    @Override
    public void onServiceConnected() {

    }

    @Override
    public void onServiceDisconnected() {

    }

    private void togglePauseResume() {
        if(MusicPlayer.getInstance().isPlaying()) {
            MusicPlayer.getInstance().pause();
        } else {
            MusicPlayer.getInstance().play();
        }
    }

    private void updatePlayInfo(MusicTrack musicTrack, boolean force) {
        if(force || (musicTrack != null && !musicTrack.equals(mMusicTrack))) {
            mMusicTrack = musicTrack;
            final String imageUrl = mMusicTrack.mCoverImageUrl + QiniuImageUtil.generateFixSizeImageAppender(mContext, QiniuImageUtil.ImageType.MELODY_SQUARE_S);
            mCircleView.setImageUrl(imageUrl);
            mMelodyNameTV.setText(mMusicTrack.mTitle);
        }

        if(MusicPlayer.getInstance().isPlaying()) {
            if(!mRotateObjectAnimation.isStarted()) {
                mRotateObjectAnimation.start();
            } else {
                if(Build.VERSION.SDK_INT > 18) {
                    mRotateObjectAnimation.resume();
                }
            }
            mPlayControlImageView.setImageResource(R.mipmap.bottom_player_pause);
        } else {
            if(!mRotateObjectAnimation.isRunning()) {
                return;
            }
            if(Build.VERSION.SDK_INT > 18) {
                mRotateObjectAnimation.pause();
            }
            mPlayControlImageView.setImageResource(R.mipmap.bottom_player_play);
        }
    }

    private class HomePageAdapter extends PagerAdapter {
        @Override
        public Object instantiateItem(ViewGroup container, int position)
        {
            AutoLoadImageView view = new AutoLoadImageView(MainActivity.this);
            int pixel = DensityUtil.dip2px(mContext, 10);
            view.setPadding(pixel, pixel, pixel, pixel);
            final int realPosition = getRealPosition(position);
            view.setImageBitmap(mDefaultLoadBitMap);
            if(realPosition >= 0 && realPosition < mBannerItemList.size()) {
                final String imageUrl = mBannerItemList.get(realPosition).getBannerImageUrl() + QiniuImageUtil.generateFixSizeImageAppender(mContext, QiniuImageUtil.ImageType.BANNER);
                Log.d(TAG, imageUrl);
                view.setImageUrl(imageUrl);
            }
            view.setOnClickListener(v -> {
                int pos = getRealPosition(position);
                if(pos >= 0 && pos < mBannerItemList.size()) {
                    JSONObject temp = (JSONObject) mBannerItemList.get(pos).getData();
                    if(temp != null) {
                        AlbumDetailBean bean = new AlbumDetailBean();
                        try {
                            bean.id = temp.getLong("id");
                            bean.albumName = temp.getString("albumName");
                            bean.albumCoverImage = DomainConst.PICTURE_DOMAIN + temp.getString("albumCoverImage");
                            bean.albumAbstract = temp.getString("albumAbstract");
                            bean.isPrecious = temp.getString("albumPrecious");
                            bean.mItemTitle = bean.albumName;
                            bean.mItemIconUrl = bean.albumCoverImage;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Intent intent = new Intent(MainActivity.this, AlbumActivity.class);
                        intent.putExtra(AlbumActivity.BUNDLE_DATA, bean);
                        startActivity(intent);
                    }
                }
            });
            container.addView(view);
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
            return mBannerItemList.size() < 3 ? 3 : mBannerItemList.size();
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

    private void requestBannerViewData() {
        mCoreContext.executeAsyncTask(() -> {
            int tryTimes = 3;
            HttpResponseBean responseBean = new HttpResponseBean();
            do {
                try{
                    String response = mHttpService.post(DomainConst.BANNER_VIEW_ITEM_URL, new JSONObject().toString());
                    JSONObject responseObject = new JSONObject(response);
                    JSONArray dataArray = responseObject.getJSONArray("data");
                    responseBean.setState(responseObject.getBoolean("state"));
                    List<BannerItemBean> tempList = new ArrayList<>();
                    for(int i=0; i < dataArray.length(); i++) {
                        JSONObject temp = dataArray.getJSONObject(i);
                        BannerItemBean bean = new BannerItemBean();
                        bean.setId(temp.getLong("id"));
                        bean.setContentId(temp.getLong("contentId"));
                        bean.setBannerImageUrl(DomainConst.PICTURE_DOMAIN + temp.getString("bannerImage"));
                        bean.setOrderIndex(temp.getInt("orderIndex"));
                        bean.setContentType(temp.getInt("contentType"));
                        tempList.add(bean);
                    }
                    if(mBannerItemList.size() == 0 && tempList.size() > 0) {
                        mBannerItemList.addAll(tempList);
                    }
                    runOnUiThread(() -> mViewPager.setAdapter(new HomePageAdapter()));
                    for(BannerItemBean bean:tempList) {
                        String requestUrl = "";
                        switch (bean.getContentType()) {
                            case BannerType.MELODY_TYPE:
                            {
                                requestUrl = DomainConst.MELODY_ITEM_URL;
                            }
                            break;
                            case BannerType.ALBUM_TYPE:
                            {
                                requestUrl = DomainConst.ALBUM_ITEM_URL;
                            }
                            break;
                        }
                        if(!StringUtil.isEmpty(requestUrl)) {
                            JSONObject secondParam = new JSONObject();
                            secondParam.put("id", String.valueOf(bean.getContentId()));
                            if(isLogin()) {
                                secondParam.put("accountId", String.valueOf(mAccountId));
                            }
                            String secondResponse = mHttpService.post(requestUrl, secondParam.toString());
                            JSONObject secondObject = new JSONObject(secondResponse);
                            JSONObject dataObject = secondObject.getJSONObject("data");
                            bean.setData(dataObject);
                        }
                    }
                } catch (JSONException e) {
                    Log.d(TAG, e.toString());
                } catch (IOException e) {
                    Log.d(TAG, e.toString());
                }
            } while (--tryTimes > 0 && mBannerItemList.size() == 0);
        });
    }

    private void updateUserInfo() {
        if(isLogin()) {
            mDownloadManager.changeUser(String.valueOf(mAccountId));
            mVipFlagIV.setVisibility(View.VISIBLE);
            mHintTV.setVisibility(View.VISIBLE);
            mHeadImageCircleView.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.default_login_logo));
            mExitLL.setVisibility(View.VISIBLE);
            syncAccountInfoFromServer();
        } else {
            mDownloadManager.changeUser(DownLoadManager.DEFAULT_USER);
            mVipFlagIV.setVisibility(View.GONE);
            mHintTV.setVisibility(View.GONE);
            mHeadImageCircleView.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.default_logout_logo));
            mExitLL.setVisibility(View.GONE);
            mNameEditText.setText(R.string.nav_login_register);
        }
    }

    public void syncAccountInfoFromServer() {
        mCoreContext.executeAsyncTask(() -> {
            try {
                if(isLogin()) {
                    JSONObject param = new JSONObject();
                    param.put("id", mAccountId);
                    String response = mHttpService.post(DomainConst.ACCOUNT_INFO_URL, param.toString());
                    JSONObject responseObject = new JSONObject(response);
                    JSONObject accountInfo = responseObject.getJSONObject("data");
                    if(accountInfo != null)
                    saveAccountInfoToPreference(accountInfo);
                    runOnUiThread(() -> {
                        boolean isVip = mPreferencesManager.get(PreferenceConst.ACCOUNT_VIP, false);
                        if(isVip) {
                            mVipFlagIV.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.nav_vip_true));
                        } else {
                            mVipFlagIV.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.nav_vip_false));
                        }
                        final String nickName = mPreferencesManager.get(PreferenceConst.ACCOUNT_NICK_NAME, "");
                        if(!StringUtil.isEmpty(nickName) && nickName.length() < 20) {
                            mNameEditText.setText(nickName);
                        }
                    });
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void saveAccountInfoToPreference(JSONObject accountInfo) {
        try {
            mPreferencesManager.put(PreferenceConst.ACCOUNT_ID, accountInfo.get("id"));
            String telephone = accountInfo.getString("telephone");
            if(!telephone.equals("null")) {
                mPreferencesManager.put(PreferenceConst.ACCOUNT_TELEPHONE, telephone);
            }

            String iconUrl = accountInfo.getString("icon");
            if(!iconUrl.equals("null")) {
                if(!iconUrl.startsWith("http")) {
                    iconUrl = DomainConst.PICTURE_DOMAIN + iconUrl;
                }
                mPreferencesManager.put(PreferenceConst.ACCOUNT_ICON, iconUrl);
            }

            final String isVip = accountInfo.getString("vip");
            if(!StringUtil.isEmpty(isVip) && isVip.equals("true")) {
                mPreferencesManager.put(PreferenceConst.ACCOUNT_VIP, true);
            } else {
                mPreferencesManager.put(PreferenceConst.ACCOUNT_VIP, false);
            }
            mPreferencesManager.put(PreferenceConst.ACCOUNT_NICK_NAME, accountInfo.get("nickName"));

            String startTime = accountInfo.getString("vipStartTime");
            if(!startTime.equals("null")) {
                mPreferencesManager.put(PreferenceConst.ACCOUNT_VIP_START_TIME, accountInfo.getString("vipStartTime"));
                mPreferencesManager.put(PreferenceConst.ACCOUNT_VIP_END_TIME, accountInfo.getString("vipEndTime"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean isLogin() {
        mAccountId = mPreferencesManager.get(PreferenceConst.ACCOUNT_ID, -1);
        return mAccountId != -1;
    }

    private void startLoginActivity () {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
    }
}
