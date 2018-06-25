package com.listory.songkang.activity;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.LayoutRes;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.joker.annotation.PermissionsDenied;
import com.joker.annotation.PermissionsGranted;
import com.joker.annotation.PermissionsRationale;
import com.joker.annotation.PermissionsRequestSync;
import com.joker.api.Permissions4M;
import com.listory.songkang.alipay.AlipayApi;
import com.listory.songkang.alipay.PayResult;
import com.listory.songkang.bean.AlbumDetailBean;
import com.listory.songkang.bean.BannerItemBean;
import com.listory.songkang.bean.HttpResponseBean;
import com.listory.songkang.bean.MelodyDetailBean;
import com.listory.songkang.constant.DomainConst;
import com.listory.songkang.constant.PermissionConstants;
import com.listory.songkang.constant.PreferenceConst;
import com.listory.songkang.core.http.HttpManager;
import com.listory.songkang.core.http.HttpService;
import com.listory.songkang.helper.WeiXinHelper;
import com.listory.songkang.listory.R;
import com.listory.songkang.service.MediaService;
import com.listory.songkang.service.MusicPlayer;
import com.listory.songkang.service.MusicTrack;
import com.listory.songkang.transformer.ScaleInTransformer;
import com.listory.songkang.utils.DensityUtil;
import com.listory.songkang.utils.IPUtils;
import com.listory.songkang.utils.QiniuImageUtil;
import com.listory.songkang.utils.StringUtil;
import com.listory.songkang.view.AutoLoadImageView;
import com.listory.songkang.view.AvatarCircleView;
import com.tencent.mm.opensdk.modelpay.PayReq;

import org.intellij.lang.annotations.MagicConstant;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.listory.songkang.alipay.AlipayConfig.SDK_PAY_FLAG;
import static com.listory.songkang.service.MediaService.PLAY_ACTION_PARAM_LIST;
import static com.listory.songkang.service.MediaService.PLAY_ACTION_PARAM_POSITION;

@PermissionsRequestSync(permission = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
        value = {PermissionConstants.STORAGE_READ_CODE, PermissionConstants.STORAGE_WRITE_CODE})
public class MainActivity extends BaseActivity implements View.OnClickListener, MusicPlayer.ConnectionState {

    private DrawerLayout mDrawerLayout;
    private List<BannerItemBean> mBannerItemList;
    private ViewPager mViewPager;
    private AvatarCircleView mCircleView;
    private ImageView mPlayControlImageView;
    private TextView mMelodyNameTV;
    private ObjectAnimator mRotateObjectAnimation;
    private AlipayHandler mAlipayHandler;
    private MusicTrack mCurrentMusicTrack;
    private Bitmap mDefaultLoadBitMap;

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
                    if(musicTrack != null && !musicTrack.equals(mCurrentMusicTrack)) {
                        mCurrentMusicTrack = musicTrack;
                    }
                    updatePlayInfo();
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

    private static class AlipayHandler extends Handler {
        private WeakReference<Activity> mActivity;
        public AlipayHandler(Activity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SDK_PAY_FLAG: {
                    @SuppressWarnings("unchecked")
                    PayResult payResult = new PayResult((Map<String, String>) msg.obj);
                    /**
                     对于支付结果，请商户依赖服务端的异步通知结果。同步通知结果，仅作为支付结束的通知。
                     */
                    String resultInfo = payResult.getResult();// 同步返回需要验证的信息
                    String resultStatus = payResult.getResultStatus();
                    // 判断resultStatus 为9000则代表支付成功
                    if (TextUtils.equals(resultStatus, "9000")) {
                        // 该笔订单是否真实支付成功，需要依赖服务端的异步通知。
                        Toast.makeText(mActivity.get(), "支付成功", Toast.LENGTH_SHORT).show();
//                        mActivity.get().finish();
                    } else {
                        // 该笔订单真实的支付结果，需要依赖服务端的异步通知。
                        Toast.makeText(mActivity.get(), "支付失败", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }
    protected void parseNonNullBundle(Bundle bundle){

    }
    protected void initDataIgnoreUi() {
        mDefaultLoadBitMap = BitmapFactory.decodeResource(getResources(), R.mipmap.default_banner_bg);
        IntentFilter intentFilter = new IntentFilter(MediaService.PLAY_STATE_UPDATE);
        registerReceiver(mIntentReceiver, intentFilter);
        mAlipayHandler = new AlipayHandler(MainActivity.this);
        mBannerItemList = new ArrayList<>();
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
                    mCurrentMusicTrack = bean.convertToMusicTrack();
                    runOnUiThread(() -> updatePlayInfo());
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                  e.printStackTrace();
                }
            }
        });
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
        requestBannerViewData();
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
            case R.id.circle_view: {
                ArrayList<MusicTrack> dataList = new ArrayList<>();
                dataList.add(mCurrentMusicTrack);
                Intent broadcast = new Intent(MediaService.PLAY_ACTION);
                broadcast.putParcelableArrayListExtra(PLAY_ACTION_PARAM_LIST, dataList);
                broadcast.putExtra(PLAY_ACTION_PARAM_POSITION, -1);
                sendBroadcast(broadcast);

                Intent intent = new Intent(MainActivity.this, MusicPlayerActivity.class);
                intent.putExtra(MusicPlayerActivity.BUNDLE_DATA_PLAY, false);
                startActivity(intent);
            }
                break;
            case R.id.iv_play:
                togglePauseResume();
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
            updatePlayInfo();
        } else {
            MusicPlayer.getInstance().play();
            updatePlayInfo();
        }
    }

    private void updatePlayInfo() {
        if(mCurrentMusicTrack != null) {
            final String imageUrl = mCurrentMusicTrack.mCoverImageUrl + QiniuImageUtil.generateFixSizeImageAppender(mContext, QiniuImageUtil.ImageType.MELODY_SQUARE_S);
            mCircleView.setImageUrl(imageUrl);
            mMelodyNameTV.setText(mCurrentMusicTrack.mTitle);
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
                        bean.setBannerImageUrl(DomainConst.DOMAIN + temp.getString("bannerImage"));
                        bean.setOrderIndex(temp.getInt("orderIndex"));
                        bean.setContentType(temp.getInt("contentType"));
                        tempList.add(bean);
                    }
                    if(mBannerItemList.size() == 0 && tempList.size() > 0) {
                        mBannerItemList.addAll(tempList);
                    }

                    runOnUiThread(() -> mViewPager.setAdapter(new HomePageAdapter()));

                    int accountId = mPreferencesManager.get(PreferenceConst.ACCOUNT_ID, -1);
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
                            if(accountId != -1) {
                                secondParam.put("accountId", String.valueOf(accountId));
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

    private void wxPayRequest(){
        mCoreContext.executeAsyncTask(() -> {
            try {
                HttpService httpService = mCoreContext.getApplicationService(HttpManager.class);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("ip", IPUtils.getIpAddress(mContext));
                jsonObject.put("productId", "0003");
                jsonObject.put("accountId", "19");
                jsonObject.put("productInfo", "故事树-会员充值");
                String response = httpService.post(DomainConst.WX_UNI_ORDER_URL, jsonObject.toString());
                JSONObject responseObject = new JSONObject(response);
                JSONObject json = responseObject.getJSONObject("data");
                PayReq req = new PayReq();
                req.appId			= json.getString("appid");
                req.partnerId		= json.getString("partnerid");
                req.prepayId		= json.getString("prepayid");
                req.nonceStr		= json.getString("noncestr");
                req.timeStamp		= json.getString("timestamp");
                req.packageValue	= json.getString("package");
                req.sign			= json.getString("sign");
                req.extData			= "app data"; // optional
                WeiXinHelper.getInstance().wxPayReq(getApplicationContext(), req);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private void alipayPayRequest() {
        mCoreContext.executeAsyncTask(() -> {
            try {
                HttpService httpService = mCoreContext.getApplicationService(HttpManager.class);
                JSONObject jsonObject = new JSONObject();

                jsonObject.put("ip", IPUtils.getIpAddress(mContext));
                jsonObject.put("productId", "0002");
                jsonObject.put("productInfo", "故事树-会员充值");
                jsonObject.put("accountId", "19");
                String response = httpService.post(DomainConst.ALIPAY_ORDER_URL, jsonObject.toString());
                JSONObject responseObject = new JSONObject(response);
                String json = responseObject.getString("data");
                AlipayApi.payV2(MainActivity.this,  mAlipayHandler, json);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }
}
