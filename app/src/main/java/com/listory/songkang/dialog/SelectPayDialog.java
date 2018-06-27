package com.listory.songkang.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.listory.songkang.R;

import org.intellij.lang.annotations.MagicConstant;

import java.lang.ref.WeakReference;


/**
 * Created by songkang on 2018/4/10.
 */

public class SelectPayDialog extends Dialog implements View.OnClickListener {

    @MagicConstant(intValues = {PayStyle.WECHAT_PAY, PayStyle.ALIPAY})
    public @interface PayStyle {
        int WECHAT_PAY = 0;
        int ALIPAY = 1;
    }
    private int mSelectedPay = PayStyle.WECHAT_PAY;

    private LinearLayout mWechatPayLayout, mAlipayLayout;
    private ImageView mWechatSelectorImageView, mAlipaySelectorImageView;
    private TextView mGoPayTextView;
    private WeakReference<PayAction> mPayActionRf;


    public SelectPayDialog(Context context, int resId){
        super(context, resId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_charge_vip);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(lp);

        mWechatPayLayout = (LinearLayout) findViewById(R.id.wechat_pay);
        mAlipayLayout = (LinearLayout) findViewById(R.id.alipay);
        mWechatSelectorImageView = (ImageView) findViewById(R.id.wechat_selector);
        mAlipaySelectorImageView = (ImageView) findViewById(R.id.alipay_selector);
        mGoPayTextView = (TextView) findViewById(R.id.go_pay);

        mWechatPayLayout.setOnClickListener(this);
        mAlipayLayout.setOnClickListener(this);
        mGoPayTextView.setOnClickListener(this);
        updateView();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.wechat_pay:
                mSelectedPay = PayStyle.WECHAT_PAY;
                updateView();
                break;
            case R.id.alipay:
                mSelectedPay = PayStyle.ALIPAY;
                updateView();
                break;
            case R.id.go_pay:
                if(mPayActionRf.get() != null) {
                    mPayActionRf.get().onPay(mSelectedPay);
                }
//                hide();
                break;
        }
    }

    public void setPayActionCallback(PayAction payAction) {
        mPayActionRf = new WeakReference<>(payAction);
    }

    private void updateView() {
        mWechatSelectorImageView.setImageResource(R.mipmap.popup_window_unselect_pay);
        mAlipaySelectorImageView.setImageResource(R.mipmap.popup_window_unselect_pay);
        switch (mSelectedPay) {
            case PayStyle.WECHAT_PAY:
                mWechatSelectorImageView.setImageResource(R.mipmap.popup_window_select_pay);
                break;
            case PayStyle.ALIPAY:
                mAlipaySelectorImageView.setImageResource(R.mipmap.popup_window_select_pay);
                break;
        }
    }

    public interface PayAction {
        void onPay(@PayStyle int payStyle);
    }
}
