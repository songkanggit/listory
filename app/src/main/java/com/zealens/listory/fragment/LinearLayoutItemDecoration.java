package com.zealens.listory.fragment;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.ColorInt;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by songkang on 2018/4/25.
 */

public class LinearLayoutItemDecoration extends RecyclerView.ItemDecoration {
    private final int mGapSizePx;
    private final int mLineSizePx;
    private final ColorDrawable mDivider;

    public LinearLayoutItemDecoration(int gapSizePx, int lineSizePx, @ColorInt int color) {
        mGapSizePx = gapSizePx;
        mLineSizePx = lineSizePx;
        mDivider = new ColorDrawable(color);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(c, parent, state);
        final int itemCount = parent.getChildCount();
        final int left = 0;
        final int right = parent.getWidth();
        for(int i=0; i<itemCount; i++) {
            final View child = parent.getChildAt(i);
            if(child == null)return;
            final int top = child.getBottom();
            final int bottom = top + mLineSizePx;
            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(c);
        }
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDrawOver(c, parent, state);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        final int lastPosition = state.getItemCount();
        final int currentPosition = parent.getChildLayoutPosition(view);
        if(currentPosition == -1)return;
        if(lastPosition == currentPosition) {
            outRect.set(0, 0, 0, 0);
        } else {
            outRect.set(0, 0, 0, mGapSizePx);
        }
    }
}
