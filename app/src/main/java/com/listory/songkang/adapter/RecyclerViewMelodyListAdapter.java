package com.listory.songkang.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.listory.songkang.bean.MelodyDetailBean;
import com.listory.songkang.listory.R;
import com.listory.songkang.utils.QiniuImageUtil;
import com.listory.songkang.view.CachedImageView;

import java.util.List;

/**
 * Created by songkang on 2018/4/26.
 */

public class RecyclerViewMelodyListAdapter<T> extends RecyclerView.Adapter<RecyclerViewMelodyListAdapter.MyViewHolder> implements View.OnClickListener {
    private OnItemClickListener onItemClickListener;
    private Context mContext;
    private List<T> mDataList;

    public interface OnItemClickListener {
        void onItemClick(View view , int position);
    }

    public RecyclerViewMelodyListAdapter(Context context, List<T> dataList) {
        mContext = context;
        mDataList = dataList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View holder = LayoutInflater.from(mContext).inflate(R.layout.recycler_view_holder_melody, parent, false);
        holder.setOnClickListener(this);
        return new MyViewHolder(holder);
    }

    @Override
    public void onBindViewHolder(RecyclerViewMelodyListAdapter.MyViewHolder holder, int position) {
        MelodyDetailBean melody = (MelodyDetailBean) mDataList.get(position);
        holder.imageView.setImageUrl(melody.coverImageUrl + QiniuImageUtil.generateFixSizeImageAppender(mContext, QiniuImageUtil.ImageType.MELODY_SQUARE_S));
        holder.name.setText(melody.title);
        holder.author.setText(melody.artist);
        if(melody.favorite.equals("true")) {
            holder.like.setImageResource(R.mipmap.melody_like);
            holder.like.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    holder.like.setImageResource(R.mipmap.melody_like);
                }
            });
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

    public class MyViewHolder extends RecyclerView.ViewHolder {
        CachedImageView imageView;
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
