package com.zealens.listory.activity;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alipay.sdk.app.AuthTask;
import com.zealens.listory.R;
import com.zealens.listory.alipay.AuthResult;
import com.zealens.listory.constant.DomainConst;
import com.zealens.listory.constant.PreferenceConst;
import com.zealens.listory.helper.HttpHelper;
import com.zealens.listory.helper.WeiXinHelper;
import com.zealens.listory.utils.CountDownTimerUtils;
import com.zealens.listory.utils.StringUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends BaseActivity implements View.OnClickListener {
    private Button mButtonLogin;
    private ImageView mBackIV, mWechatLogin, mAlipayLogin;
    private TextView mSendTV;
    private EditText mEditTextTelephone, mEditTextCode;

    private String mTelephoneNumber;
    private String mVerifyCode;

    protected void parseNonNullBundle(Bundle bundle){

    }
    protected void initDataIgnoreUi() {

    }
    @LayoutRes
    protected int getLayoutResourceId() { return R.layout.activity_login;}
    protected void viewAffairs(){
        mButtonLogin = fvb(R.id.button_login);
        mBackIV = fvb(R.id.toolbar_back);
        mSendTV = fvb(R.id.tv_send);
        mEditTextTelephone = fvb(R.id.edit_telephone);
        mEditTextCode = fvb(R.id.edit_code);

        mWechatLogin = fvb(R.id.iv_wechat);
        mAlipayLogin = fvb(R.id.iv_alipay);
    }
    protected void assembleViewClickAffairs(){
        mBackIV.setOnClickListener(this);
        mButtonLogin.setOnClickListener(this);
        mSendTV.setOnClickListener(this);

        mWechatLogin.setOnClickListener(this);
        mAlipayLogin.setOnClickListener(this);
    }
    protected void initDataAfterUiAffairs(){

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isLogin()) {
            finish();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.toolbar_back:
                finish();
                break;
            case R.id.tv_send:
                mTelephoneNumber = mEditTextTelephone.getText().toString().trim();
                if(checkPhoneNumber(mTelephoneNumber)) {
                    CountDownTimerUtils sendCodeTimer = new CountDownTimerUtils(mSendTV, 60000, 1000);
                    sendCodeTimer.start();
                    smsCodeRequest();
                    mEditTextCode.requestFocus();
                } else {
                    Toast.makeText(mContext, R.string.error_telephone_tip, Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.button_login:
                mVerifyCode = mEditTextCode.getText().toString();
                if(!StringUtil.isEmpty(mVerifyCode)) {
                    smsCodeVerifyRequest();
                } else {
                    Toast.makeText(mContext, R.string.activity_login_hint_code, Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.iv_wechat:
                WeiXinHelper.getInstance().wxLoginReq(getApplicationContext());

                break;
            case R.id.iv_alipay:
                alipayAuthLoginRequest();
                break;
        }
    }

    /**
     * 验证手机号码
     * @param phoneNumber 手机号码
     * @return boolean
     */
    public static boolean checkPhoneNumber(String phoneNumber){
        Pattern pattern=Pattern.compile("^1[0-9]{10}$");
        Matcher matcher=pattern.matcher(phoneNumber);
        return matcher.matches();
    }

    private void smsCodeRequest() {
        mCoreContext.executeAsyncTask(() -> {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("telephone", mTelephoneNumber);
                String response = mHttpService.post(DomainConst.SMS_CODE_REQUEST_URL, jsonObject.toString());
                JSONObject responseObject = new JSONObject(response);
                final String result = responseObject.getString("state");
                runOnUiThread(() -> {
                    if(result.equals("true")) {
                        Toast.makeText(getApplicationContext(), R.string.sms_code_send_success, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.sms_code_send_fail, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (JSONException e) {
                Log.d(TAG, e.toString());
            } catch (IOException e) {
                Log.d(TAG, e.toString());
            }
        });
    }

    private void smsCodeVerifyRequest() {
        mCoreContext.executeAsyncTask(() -> {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("telephone", mTelephoneNumber);
                jsonObject.put("token", mVerifyCode);
                String response = mHttpService.post(DomainConst.SMS_CODE_VERIFY_REQUEST_URL, jsonObject.toString());
                JSONObject responseObject = new JSONObject(response);
                final boolean success = responseObject.getBoolean("state");
                runOnUiThread(() -> {
                    if(success) {
                        try {
                            final JSONObject accountInfo = responseObject.getJSONObject("data");
                            if(accountInfo != null)
                            {
                                saveAccountInfoToPreference(accountInfo);
                            }
                        } catch (JSONException e) {
                            Log.d(TAG, e.toString());
                        }
                    } else {
                        Toast.makeText(mContext, R.string.sms_code_wrong, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (JSONException e) {
                Log.d(TAG, e.toString());
            } catch (IOException e) {
                Log.d(TAG, e.toString());
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

            mPreferencesManager.put(PreferenceConst.ACCOUNT_VIP, accountInfo.get("vip"));
            mPreferencesManager.put(PreferenceConst.ACCOUNT_NICK_NAME, accountInfo.get("nickName"));

            String startTime = accountInfo.getString("vipStartTime");
            if(!startTime.equals("null")) {
                mPreferencesManager.put(PreferenceConst.ACCOUNT_VIP_START_TIME, accountInfo.getString("vipStartTime"));
                mPreferencesManager.put(PreferenceConst.ACCOUNT_VIP_END_TIME, accountInfo.getString("vipEndTime"));
            }
            finish();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void alipayAuthLoginRequest() {
        mCoreContext.executeAsyncTask(() -> {
            try {
                JSONObject jsonObject = new JSONObject();
                String response = mHttpService.post(DomainConst.ALIPAY_LOGIN_URL, jsonObject.toString());
                JSONObject responseObject = new JSONObject(response);
                String json = responseObject.getString("data");

                AuthTask authTask = new AuthTask(LoginActivity.this);
                // 调用授权接口，获取授权结果
                Map<String, String> result = authTask.authV2(json, true);

                AuthResult authResult = new AuthResult(result, true);
                String resultStatus = authResult.getResultStatus();
                // 判断resultStatus 为“9000”且result_code
                // 为“200”则代表授权成功，具体状态码代表含义可参考授权接口文档
                if (TextUtils.equals(resultStatus, "9000") && TextUtils.equals(authResult.getResultCode(), "200")) {
                    // 获取alipay_open_id，调支付时作为参数extern_token 的value
                    // 传入，则支付账户为该授权账户
                    JSONObject param = new JSONObject();
                    param.put("alipayOpenId", authResult.getAlipayOpenId());
                    param.put("registerPlatform", DomainConst.THIRD_PARTY_ALIPAY);
                    param.put("accessCode", authResult.getAuthCode());
                    HttpHelper.thirdPartyLoginRequest(mCoreContext, param, responseBean -> runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), R.string.auth_success, Toast.LENGTH_SHORT).show();
                        finish();
                    }));
                } else {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), R.string.auth_failed , Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private boolean isLogin() {
        int accountId = mPreferencesManager.get(PreferenceConst.ACCOUNT_ID, -1);
        return  accountId != -1;
    }
}
