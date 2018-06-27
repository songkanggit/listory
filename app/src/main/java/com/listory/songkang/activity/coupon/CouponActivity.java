package com.listory.songkang.activity.coupon;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.listory.songkang.activity.BaseActivity;
import com.listory.songkang.constant.DomainConst;
import com.listory.songkang.constant.PreferenceConst;
import com.listory.songkang.R;
import com.listory.songkang.utils.DensityUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class CouponActivity extends BaseActivity implements View.OnClickListener {
    private ImageView mBackButton;
    private TextView mTitleView;
    private EditText mEditText;
    private TextView mConfirmText, mErrorTextView;

    protected void parseNonNullBundle(Bundle bundle){

    }
    protected void initDataIgnoreUi() {

    }
    @LayoutRes
    protected int getLayoutResourceId() { return R.layout.activity_coupon;}

    protected void viewAffairs(){
        mBackButton = fvb(R.id.toolbar_back);
        mTitleView = fvb(R.id.toolbar_title);
        mEditText = fvb(R.id.edit_text);
        mConfirmText = fvb(R.id.tv_confirm);
        mErrorTextView = fvb(R.id.tv_error_tip);
    }

    protected void assembleViewClickAffairs(){
        mBackButton.setOnClickListener(this);
        mConfirmText.setOnClickListener(this);

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() == 10) {
                    mConfirmText.setEnabled(true);
                    GradientDrawable gradientDrawable = (GradientDrawable)mConfirmText.getBackground();
                    final int color = getResources().getColor(R.color.colorFBC600);
                    gradientDrawable.setColor(color);
                    gradientDrawable.setStroke(DensityUtil.dip2px(mContext, 1), color);
                } else {
                    mConfirmText.setEnabled(false);
                    GradientDrawable gradientDrawable = (GradientDrawable)mConfirmText.getBackground();
                    final int color = getResources().getColor(R.color.colorCCCCCC);
                    gradientDrawable.setColor(color);
                    gradientDrawable.setStroke(DensityUtil.dip2px(mContext, 1), color);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    protected void initDataAfterUiAffairs(){
        mTitleView.setText(R.string.coupon_activity_title);
        GradientDrawable gradientDrawable = (GradientDrawable)mConfirmText.getBackground();
        final int color = getResources().getColor(R.color.colorCCCCCC);
        gradientDrawable.setColor(color);
        gradientDrawable.setStroke(DensityUtil.dip2px(mContext, 1), color);
        mConfirmText.setEnabled(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.toolbar_back:
                finish();
                break;
            case R.id.tv_confirm:
                final String code = mEditText.getText().toString().trim();
                verifyCouponFromServer(code);
                break;
        }
    }

    private void verifyCouponFromServer(final String cardNo) {
        mCoreContext.executeAsyncTask(new Runnable() {
            @Override
            public void run() {
                try {
                    int accountId = mPreferencesManager.get(PreferenceConst.ACCOUNT_ID, -1);
                    if(accountId != -1) {
                        JSONObject param = new JSONObject();
                        param.put("cardNo", cardNo);
                        param.put("accountId", accountId);
                        String response = mHttpService.post(DomainConst.COUPON_VERIFY_URL, param.toString());
                        JSONObject responseObject = new JSONObject(response);
                        if(responseObject.getString("code").equals("0000")) {
                            JSONObject dataObject = responseObject.getJSONObject("data");
                            final String validDate = dataObject.getString("endTime");
                            final String validDays = dataObject.getString("validDays");
                            final String productName = dataObject.getString("productName");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Intent intent = new Intent(CouponActivity.this, CouponResultActivity.class);
                                    intent.putExtra(CouponResultActivity.BUNDLE_NAME, productName);
                                    intent.putExtra(CouponResultActivity.BUNDLE_VALID_DATE, validDate);
                                    intent.putExtra(CouponResultActivity.BUNDLE_VALID_DAYS, validDays);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mErrorTextView.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
    }
}
