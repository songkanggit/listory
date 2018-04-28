package com.listory.songkang.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.listory.songkang.listory.R;
import com.listory.songkang.service.MusicTrack;

import java.util.List;

/**
 * Created by songkang on 2018/4/28.
 */

public class RecyclerViewMelodyListSimpleAdapter<T> extends RecyclerView.Adapter<RecyclerViewMelodyListSimpleAdapter.MyViewHolder>
        implements View.OnClickListener {
    private OnItemClickListener onItemClickListener;
    private Context mContext;
    private List<T> mDataList;

    public interface OnItemClickListener {
        void onItemClick(View view , int position);
    }

    public RecyclerViewMelodyListSimpleAdapter(Context context, List<T> dataList) {
        mContext = context;
        mDataList = dataList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View holder = LayoutInflater.from(mContext).inflate(R.layout.recycler_view_holder_melody_list, null);
        holder.setOnClickListener(this);
        return new MyViewHolder(holder);
    }

    @Override
    public void onBindViewHolder(RecyclerViewMelodyListSimpleAdapter.MyViewHolder holder, int position) {
        MusicTrack melody = (MusicTrack) mDataList.get(position);
        holder.name.setText(melody.mTitle);
        holder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    @Override
    public void onClick(View view) {
        if(onItemClickListener != null) {
            onItemClickListener.onItemClick(view, (int)view.getTag());
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        onItemClickListener = listener;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        public MyViewHolder(View v){
            super(v);
            name = v.findViewById(R.id.tv_name);
        }
    }
}
