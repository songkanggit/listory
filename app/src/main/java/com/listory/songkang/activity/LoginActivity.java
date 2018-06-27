package com.listory.songkang.activity;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.listory.songkang.constant.DomainConst;
import com.listory.songkang.constant.PreferenceConst;
import com.listory.songkang.R;
import com.listory.songkang.utils.CountDownTimerUtils;
import com.listory.songkang.utils.StringUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends BaseActivity implements View.OnClickListener {
    private Button mButtonLogin;
    private ImageView mBackIV;
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
    }
    protected void assembleViewClickAffairs(){
        mBackIV.setOnClickListener(this);
        mButtonLogin.setOnClickListener(this);
        mSendTV.setOnClickListener(this);
    }
    protected void initDataAfterUiAffairs(){

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
}
