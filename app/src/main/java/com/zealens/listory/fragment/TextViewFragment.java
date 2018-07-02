package com.zealens.listory.fragment;

import android.os.Bundle;
import android.widget.TextView;

import com.zealens.listory.R;

/**
 * Created by songkang on 2018/4/25.
 */

public class TextViewFragment extends BaseFragment {
    private TextView mTextView;

    private String mAbstractText;

    public void setText(final String text) {
        mAbstractText = text;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.view_page_item;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        mTextView = mRootView.findViewById(R.id.iv_pager);
        mTextView.setText(mAbstractText);
    }
}
