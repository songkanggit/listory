package com.listory.songkang.fragment;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.listory.songkang.activity.MusicPlayerActivity;
import com.listory.songkang.adapter.RecyclerViewMelodyListAdapter;
import com.listory.songkang.application.RealApplication;
import com.listory.songkang.bean.Melody;
import com.listory.songkang.listory.R;
import com.listory.songkang.service.MediaService;
import com.listory.songkang.service.MusicTrack;
import com.listory.songkang.view.CachedImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songkang on 2018/4/25.
 */

public class AlbumListFragment extends BaseFragment implements RecyclerViewMelodyListAdapter.OnItemClickListener {

    private RecyclerView mRecyclerView;
    private RecyclerViewMelodyListAdapter mContentAdapter;
    private List<Melody> mDataList = new ArrayList<>();

    public void setData(List<Melody> list) {
        mDataList.clear();
        mDataList.addAll(list);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_album_list;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        mRecyclerView =  mRootView.findViewById(R.id.recycler_view);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new LinearLayoutItemDecoration(2, 2, getResources().getColor(R.color.colorF4F5F7)));
        mRecyclerView.setAdapter(mContentAdapter = new RecyclerViewMelodyListAdapter(getContext(), mDataList));
        mContentAdapter.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(View view, int position) {
        ArrayList<MusicTrack> dataList = new ArrayList<>();
        for(Melody bean: mDataList) {
            dataList.add(bean.convertToMusicTrack());
        }
        Intent broadcast = new Intent(MediaService.PLAY_ACTION);
        broadcast.putParcelableArrayListExtra(MediaService.PLAY_ACTION_PARAM_LIST, dataList);
        broadcast.putExtra(MediaService.PLAY_ACTION_PARAM_POSITION, 0);
        getActivity().sendBroadcast(broadcast);

        Intent intent = new Intent(getActivity(), MusicPlayerActivity.class);
        intent.putExtra(MusicPlayerActivity.BUNDLE_DATA, mDataList.get(position));
        intent.putExtra(MusicPlayerActivity.BUNDLE_DATA_PLAY, true);
        startActivity(intent);
    }
}
