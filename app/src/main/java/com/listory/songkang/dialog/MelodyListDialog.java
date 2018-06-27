package com.listory.songkang.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.listory.songkang.activity.MusicPlayActivity;
import com.listory.songkang.adapter.RecyclerViewMelodyListSimpleAdapter;
import com.listory.songkang.R;
import com.listory.songkang.service.MusicTrack;

import java.util.List;

/**
 * Created by songkang on 2018/4/28.
 */

public class MelodyListDialog extends Dialog implements View.OnClickListener {
    private TextView mCloseButton;
    private RecyclerView mRecyclerView;
    private List<MusicTrack> mDataList;
    private RecyclerViewMelodyListSimpleAdapter mAdapter;
    private int mPlayingPosition = 0;

    public MelodyListDialog(@NonNull Context context, List<MusicTrack> dataList) {
        super(context, R.style.white_bg_dialog);
        mDataList = dataList;
        setOwnerActivity((Activity) context);
    }

    public MelodyListDialog(@NonNull Context context, int themeResId, List<MusicTrack> dataList) {
        super(context, themeResId);
        mDataList = dataList;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_melody_list);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(lp);

        mCloseButton = (TextView) findViewById(R.id.tv_close);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mAdapter = new RecyclerViewMelodyListSimpleAdapter(getContext(), mDataList));
        mAdapter.setOnItemClickListener((MusicPlayActivity)getOwnerActivity());
        mCloseButton.setOnClickListener(this);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAdapter.setPlayingPosition(mPlayingPosition);
    }

    @Override
    public void onClick(View view) {
        dismiss();
    }

    public void setPlayingPosition(final int position) {
        mPlayingPosition = position;
    }
}
