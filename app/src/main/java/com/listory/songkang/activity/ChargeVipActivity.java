package com.listory.songkang.activity;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.LayoutRes;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.listory.songkang.alipay.AlipayApi;
import com.listory.songkang.alipay.PayResult;
import com.listory.songkang.constant.DomainConst;
import com.listory.songkang.constant.PreferenceConst;
import com.listory.songkang.dialog.SelectPayDialog;
import com.listory.songkang.helper.WeiXinHelper;
import com.listory.songkang.R;
import com.listory.songkang.utils.IPUtils;
import com.tencent.mm.opensdk.modelpay.PayReq;

import org.intellij.lang.annotations.MagicConstant;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;

import static com.listory.songkang.alipay.AlipayConfig.SDK_PAY_FLAG;

public class ChargeVipActivity extends BaseActivity implements View.OnClickListener, SelectPayDialog.PayAction {
    public static final String BUNDLE_DATA = "result";
    private static final String TAG = ChargeVipActivity.class.getSimpleName();

    @MagicConstant(stringValues = {SalesProduct.ONE_YEAR_PRO, SalesProduct.THREE_MONTH_PRO, SalesProduct.ONE_MONTH_PRO})
    public @interface SalesProduct {
        String ONE_YEAR_PRO = "LD000";
        String THREE_MONTH_PRO = "LD001";
        String ONE_MONTH_PRO = "LD002";
    }

    private ImageView mBackButton;
    private String mSelectedProduct = SalesProduct.ONE_YEAR_PRO;
    private int mSelectedColor, mUnSelectedColor;
    private TextView mTextView1Year, mTextView3Month, mTextView1Month;
    private TextView mToolbarTitle;
    private Button mChargeButton;
    private SelectPayDialog mSelectPayDialog;
    private long mAccountId;
    private AlipayHandler mAlipayHandler;
    private volatile boolean isPaying;

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
                        Toast.makeText(mActivity.get(), "充值成功", Toast.LENGTH_SHORT).show();
                        mActivity.get().finish();
                    } else {
                        // 该笔订单真实的支付结果，需要依赖服务端的异步通知。
                        Toast.makeText(mActivity.get(), "充值失败", Toast.LENGTH_SHORT).show();
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
        isPaying = false;
        mAlipayHandler = new AlipayHandler(ChargeVipActivity.this);
        mSelectedColor = getResources().getColor(R.color.colorFBC600);
        mUnSelectedColor = getResources().getColor(R.color.colorWhite);
        mAccountId = mPreferencesManager.get(PreferenceConst.ACCOUNT_ID, -1);
        if(mAccountId == -1) {
            Toast.makeText(getApplicationContext(), R.string.error_login, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @LayoutRes
    protected int getLayoutResourceId(){
        return R.layout.activity_charge_vip;
    }
    protected void viewAffairs() {
        mBackButton = fvb(R.id.toolbar_back);
        mToolbarTitle = fvb(R.id.toolbar_title);
        mTextView1Year = fvb(R.id.golden_vip_value);
        mTextView3Month = fvb(R.id.silver_vip_value);
        mTextView1Month = fvb(R.id.bronze_vip_value);
        mChargeButton = fvb(R.id.charge_vip);
    }

    protected void assembleViewClickAffairs() {
        mBackButton.setOnClickListener(this);
        mTextView1Year.setOnClickListener(this);
        mTextView3Month.setOnClickListener(this);
        mTextView1Month.setOnClickListener(this);
        mChargeButton.setOnClickListener(this);
        fvb(R.id.golden_rl).setOnClickListener(this);
        fvb(R.id.silver_rl).setOnClickListener(this);
        fvb(R.id.bronze_rl).setOnClickListener(this);
    }

    protected void initDataAfterUiAffairs(){
        mToolbarTitle.setText(getResources().getString(R.string.title_activity_charge_vip));
        refreshSelectState();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.toolbar_back:
                finish();
                break;
            case R.id.golden_vip_value:
            case R.id.golden_rl:
                mSelectedProduct = SalesProduct.ONE_YEAR_PRO;
                refreshSelectState();
                break;
            case R.id.silver_vip_value:
            case R.id.silver_rl:
                mSelectedProduct = SalesProduct.THREE_MONTH_PRO;
                refreshSelectState();
                break;
            case R.id.bronze_vip_value:
            case R.id.bronze_rl:
                mSelectedProduct = SalesProduct.ONE_MONTH_PRO;
                refreshSelectState();
                break;
            case R.id.charge_vip:
                showPopWindow();
                break;
        }
    }

    @Override
    public void onPay(int payStyle) {
        if(isPaying)return;
        isPaying = true;
        switch (payStyle) {
            case SelectPayDialog.PayStyle.WECHAT_PAY:
                wxPayRequest();
                break;
            case SelectPayDialog.PayStyle.ALIPAY:
                alipayPayRequest();
                break;
        }
    }

    private void refreshSelectState() {
        ((GradientDrawable)mTextView1Year.getBackground()).setColor(mUnSelectedColor);
        ((GradientDrawable)mTextView3Month.getBackground()).setColor(mUnSelectedColor);
        ((GradientDrawable)mTextView1Month.getBackground()).setColor(mUnSelectedColor);
        mTextView1Year.setTextColor(mSelectedColor);
        mTextView3Month.setTextColor(mSelectedColor);
        mTextView1Month.setTextColor(mSelectedColor);
        switch (mSelectedProduct) {
            case SalesProduct.ONE_YEAR_PRO:
                ((GradientDrawable)mTextView1Year.getBackground()).setColor(mSelectedColor);
                mTextView1Year.setTextColor(mUnSelectedColor);
                break;
            case SalesProduct.THREE_MONTH_PRO:
                ((GradientDrawable)mTextView3Month.getBackground()).setColor(mSelectedColor);
                mTextView3Month.setTextColor(mUnSelectedColor);
                break;
            case SalesProduct.ONE_MONTH_PRO:
                ((GradientDrawable)mTextView1Month.getBackground()).setColor(mSelectedColor);
                mTextView1Month.setTextColor(mUnSelectedColor);
                break;
            default:
                break;
        }

    }

    private void showPopWindow() {
        if(mSelectPayDialog == null) {
            mSelectPayDialog = new SelectPayDialog(ChargeVipActivity.this, R.style.transparent_dialog);
            mSelectPayDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            mSelectPayDialog.setPayActionCallback(this);
        }
        Window window = mSelectPayDialog.getWindow();
        window.setGravity(Gravity.BOTTOM);
        window.setWindowAnimations(R.style.custom_popup_window_style);
        mSelectPayDialog.show();
    }

    private void wxPayRequest(){
        mCoreContext.executeAsyncTask(() -> {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("ip", IPUtils.getIpAddress(mContext));
                jsonObject.put("productId", mSelectedProduct);
                jsonObject.put("accountId", mAccountId);
                jsonObject.put("productInfo", "故事树-会员充值");
                String response = mHttpService.post(DomainConst.WX_UNI_ORDER_URL, jsonObject.toString());
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
                isPaying = false;
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
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("ip", IPUtils.getIpAddress(mContext));
                jsonObject.put("productId", mSelectedProduct);
                jsonObject.put("productInfo", "故事树-会员充值");
                jsonObject.put("accountId", mAccountId);
                String response = mHttpService.post(DomainConst.ALIPAY_ORDER_URL, jsonObject.toString());
                JSONObject responseObject = new JSONObject(response);
                String json = responseObject.getString("data");
                AlipayApi.payV2(ChargeVipActivity.this,  mAlipayHandler, json);
                isPaying = false;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }
}
