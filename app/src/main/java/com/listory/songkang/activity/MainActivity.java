package com.listory.songkang.activity;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.LayoutRes;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.listory.songkang.DomainConst;
import com.listory.songkang.alipay.AlipayApi;
import com.listory.songkang.alipay.PayResult;
import com.listory.songkang.application.RealApplication;
import com.listory.songkang.bean.Melody;
import com.listory.songkang.core.http.HttpManager;
import com.listory.songkang.core.http.HttpService;
import com.listory.songkang.helper.WeiXinHelper;
import com.listory.songkang.listory.R;
import com.listory.songkang.service.MediaService;
import com.listory.songkang.service.MusicPlayer;
import com.listory.songkang.service.MusicTrack;
import com.listory.songkang.transformer.ScaleInTransformer;
import com.listory.songkang.utils.IPUtils;
import com.listory.songkang.utils.PermissionUtil;
import com.listory.songkang.view.AvatarCircleView;
import com.tencent.mm.opensdk.modelpay.PayReq;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.listory.songkang.alipay.AlipayConfig.SDK_PAY_FLAG;


public class MainActivity extends BaseActivity implements View.OnClickListener, MusicPlayer.ConnectionState {

    private DrawerLayout mDrawerLayout;
    private int[] imgRes = {R.mipmap.will_youth, R.mipmap.mr_black, R.mipmap.will_youth, R.mipmap.mr_black,
            R.mipmap.will_youth, R.mipmap.mr_black};
    private ViewPager mViewPager;
    private AvatarCircleView mCircleView;
    private ImageView mPlayControlImageView;
    private TextView mMelodyNameTV;
    private ObjectAnimator mRotateObjectAnimation;
    private AlipayHandler mAlipayHandler;

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
        PermissionUtil.verifyStoragePermissions(MainActivity.this);
        IntentFilter intentFilter = new IntentFilter(MediaService.MUSIC_CHANGE_ACTION);
        registerReceiver(mIntentReceiver, intentFilter);
        mAlipayHandler = new AlipayHandler(MainActivity.this);
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
//                Intent intent = new Intent(MainActivity.this, MusicPlayerActivity.class);
//                intent.putExtra(MusicPlayerActivity.BUNDLE_DATA, ((RealApplication)getApplication()).getMelodyContent(RealApplication.MediaContent.WILL_YOUTH).get(0));
//                intent.putExtra(MusicPlayerActivity.BUNDLE_DATA_PLAY, false);
//                startActivity(intent);
                alipayPayRequest();
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
