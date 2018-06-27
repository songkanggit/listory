package com.listory.songkang.fragment;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;

import com.listory.songkang.activity.AlbumActivity;
import com.listory.songkang.adapter.RecyclerViewMelodyListAdapter;
import com.listory.songkang.bean.AlbumDetailBean;
import com.listory.songkang.bean.MelodyDetailBean;
import com.listory.songkang.helper.HttpHelper;
import com.listory.songkang.R;
import com.listory.songkang.utils.StringUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songkang on 2018/4/25.
 */

public class AlbumListFragment extends BaseFragment implements View.OnClickListener {

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private RecyclerViewMelodyListAdapter mContentAdapter;
    private List<MelodyDetailBean> mDataList = new ArrayList<>();
    private AlbumDetailBean mAlbumDetailBean;
    private LinearLayout mPlayAllLL;
    private volatile int mLastVisibleItem;
    private int mCurrentPage;

    public void setData(AlbumDetailBean bean) {
        mAlbumDetailBean = bean;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_album_list;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        mRecyclerView =  mRootView.findViewById(R.id.recycler_view);
        mPlayAllLL = mRootView.findViewById(R.id.ll_play_all);

        mRecyclerView.setLayoutManager(mLinearLayoutManager = new LinearLayoutManager(getContext()));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new LinearLayoutItemDecoration(0, 0, getResources().getColor(R.color.colorF4F5F7)));
        mRecyclerView.setAdapter(mContentAdapter = new RecyclerViewMelodyListAdapter(getContext(), mDataList));
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if(newState == RecyclerView.SCROLL_STATE_IDLE && mContentAdapter.getItemCount() == mLastVisibleItem + 1) {
                    if(mContentAdapter.isLoadMore()) {
                        mCurrentPage ++;
                        requestDataList(mCurrentPage);
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                mLastVisibleItem = mLinearLayoutManager.findLastVisibleItemPosition();
            }
        });
        mContentAdapter.setOnItemClickListener((AlbumActivity)getActivity());
        mContentAdapter.notifyDataSetChanged();
        mPlayAllLL.setOnClickListener(this);
        mCurrentPage = 1;
        requestDataList(mCurrentPage);
    }

    @Override
    public void onClick(View view) {
        ((AlbumActivity)getActivity()).onItemClick(null, 0);
    }

    public List<MelodyDetailBean> getDataList() {
        return mDataList;
    }

    private void requestDataList(final int page) {
        mCoreContextRef.get().executeAsyncTask(() -> {
            if(!StringUtil.isEmpty(mAlbumDetailBean.albumName)) {
                try {
                    JSONObject param = new JSONObject();
                    param.put("melodyAlbum", mAlbumDetailBean.albumName);
                    param.put("pageSize", "8");
                    param.put("page", page);
                    List<MelodyDetailBean> tempList = new ArrayList<>();
                    HttpHelper.requestMelodyList(mCoreContextRef.get(), param, tempList, null, responseBean -> getActivity().runOnUiThread(() ->
                    {
                        if(responseBean.getCurrentPage().equals("1")) {
                            mDataList.clear();
                        }
                        mDataList.addAll(tempList);
                        if(Integer.valueOf(responseBean.getCurrentPage()) >= Integer.valueOf(responseBean.getPageSize())) {
                            mContentAdapter.setLoadMore(false);
                        }
                        mContentAdapter.notifyDataSetChanged();
                    }));
                } catch(JSONException e){
                    e.printStackTrace();
                }
            }
        });
    }
}
