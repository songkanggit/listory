package com.listory.songkang.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;

import com.listory.songkang.activity.MusicPlayerActivity;
import com.listory.songkang.adapter.RecyclerViewMelodyListAdapter;
import com.listory.songkang.bean.MelodyDetailBean;
import com.listory.songkang.listory.R;
import com.listory.songkang.service.MediaService;
import com.listory.songkang.service.MusicTrack;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songkang on 2018/4/25.
 */

public class AlbumListFragment extends BaseFragment implements View.OnClickListener, RecyclerViewMelodyListAdapter.OnItemClickListener {

    private RecyclerView mRecyclerView;
    private RecyclerViewMelodyListAdapter mContentAdapter;
    private List<MelodyDetailBean> mDataList = new ArrayList<>();
    private LinearLayout mPlayAllLL;

    public void setData(List<MelodyDetailBean> list) {
        mDataList = list;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_album_list;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        mRecyclerView =  mRootView.findViewById(R.id.recycler_view);
        mPlayAllLL = mRootView.findViewById(R.id.ll_play_all);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new LinearLayoutItemDecoration(0, 0, getResources().getColor(R.color.colorF4F5F7)));
        mRecyclerView.setAdapter(mContentAdapter = new RecyclerViewMelodyListAdapter(getContext(), mDataList));
        mContentAdapter.setOnItemClickListener(this);
        mContentAdapter.notifyDataSetChanged();
        mPlayAllLL.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        onItemClick(null, 0);
    }

    @Override
    public void onItemClick(View view, int position) {
        ArrayList<MusicTrack> dataList = new ArrayList<>();
        for(MelodyDetailBean bean: mDataList) {
            dataList.add(bean.convertToMusicTrack());
        }
        Intent broadcast = new Intent(MediaService.PLAY_ACTION);
        broadcast.putParcelableArrayListExtra(MediaService.PLAY_ACTION_PARAM_LIST, dataList);
        broadcast.putExtra(MediaService.PLAY_ACTION_PARAM_POSITION, position);
        getActivity().sendBroadcast(broadcast);

        Intent intent = new Intent(getActivity(), MusicPlayerActivity.class);
        intent.putExtra(MusicPlayerActivity.BUNDLE_DATA, mDataList.get(position));
        intent.putExtra(MusicPlayerActivity.BUNDLE_DATA_PLAY, true);
        startActivity(intent);
    }

    public void notifyDataChange() {
        if(mContentAdapter != null) {
            mContentAdapter.notifyDataSetChanged();
        }
    }
}
