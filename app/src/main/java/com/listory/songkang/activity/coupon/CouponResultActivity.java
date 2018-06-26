package com.listory.songkang.activity.coupon;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.listory.songkang.activity.BaseActivity;
import com.listory.songkang.listory.R;
import com.listory.songkang.utils.DateTimeUtil;


public class CouponResultActivity extends BaseActivity implements View.OnClickListener {
    public static final String BUNDLE_NAME = "productName";
    public static final String BUNDLE_VALID_DATE = "validDate";
    public static final String BUNDLE_VALID_DAYS = "validDays";

    private TextView mProductNameTextView, mInfoTextView;
    private ImageView mBackButton;
    private String mProductName;
    private String mValidDate;
    private String mValidDays;

    protected void parseNonNullBundle(Bundle bundle){
        mProductName = bundle.getString(BUNDLE_NAME);
        mValidDate = bundle.getString(BUNDLE_VALID_DATE);
        mValidDays = bundle.getString(BUNDLE_VALID_DAYS);
    }
    protected void initDataIgnoreUi() {

    }
    @LayoutRes
    protected int getLayoutResourceId() { return R.layout.activity_coupon_result;}
    protected void viewAffairs(){
        mBackButton = fvb(R.id.toolbar_back);
        mProductNameTextView = fvb(R.id.tv_product_info);
        mInfoTextView = fvb(R.id.tv_info);
    }
    protected void assembleViewClickAffairs(){
        mBackButton.setOnClickListener(this);
    }
    protected void initDataAfterUiAffairs(){
        mProductNameTextView.setText(mProductName);
        final String info = "至" + DateTimeUtil.timeStampToDateString(mValidDate, DateTimeUtil.sdf) + "到期，还剩" + mValidDays + "天";
        mInfoTextView.setText(info);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.toolbar_back:
                finish();
                break;
        }
    }
}
