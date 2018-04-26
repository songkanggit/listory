package com.listory.songkang.fragment;

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

import com.listory.songkang.bean.Melody;
import com.listory.songkang.listory.R;
import com.listory.songkang.view.CachedImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songkang on 2018/4/25.
 */

public class AlbumListFragment extends BaseFragment {

    private RecyclerView mRecyclerView;
    private ContentAdapter mContentAdapter;
    private List<Melody> mDataList = new ArrayList<>();

    public void setData(List<Melody> list) {
        mDataList.clear();
        mDataList.addAll(list);
    }

    public interface OnItemClickListener {
        void onItemClick(View view , int position);
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
        mRecyclerView.setAdapter(mContentAdapter = new ContentAdapter());
    }

    class ContentAdapter extends RecyclerView.Adapter<ContentAdapter.MyViewHolder> implements View.OnClickListener {

        private OnItemClickListener onItemClickListener;

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View holder = LayoutInflater.from(getActivity()).inflate(R.layout.recycler_view_holder_melody, parent, false);
            holder.setOnClickListener(this);
            return new MyViewHolder(holder);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            Melody melody = mDataList.get(position);
            holder.imageView.setImageBitmap(BitmapFactory.decodeFile(melody.getIcon().split(";")[1]));
            holder.name.setText(melody.getName());
            holder.author.setText(melody.getAuthor());
            if(melody.getLike().equals("1")) {
                holder.like.setImageResource(R.mipmap.melody_like);
            }
            holder.itemView.setTag(position);
        }

        @Override
        public int getItemCount() {
            return mDataList.size();
        }

        @Override
        public void onClick(View v) {
            if(onItemClickListener != null) {
                onItemClickListener.onItemClick(v, (int)v.getTag());
            }
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            onItemClickListener = listener;
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView name;
            TextView author;
            ImageView like;

            public MyViewHolder(View v){
                super(v);
                imageView = v.findViewById(R.id.iv_icon);
                name = v.findViewById(R.id.tv_name);
                author = v.findViewById(R.id.tv_author);
                like = v.findViewById(R.id.iv_favorite);
            }
        }
    }
}
