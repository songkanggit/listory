package com.listory.songkang.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.listory.songkang.R;
import com.listory.songkang.service.MusicTrack;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by songkang on 2018/4/28.
 */

public class RecyclerViewMelodyListSimpleAdapter<T> extends RecyclerView.Adapter<RecyclerViewMelodyListSimpleAdapter.MyViewHolder>
        implements View.OnClickListener {
    private WeakReference<OnItemClickListener> onItemClickListenerRf;
    private Context mContext;
    private List<T> mDataList;
    private int mPlayingPosition = 0;

    public interface OnItemClickListener {
        void onItemClick(View view, final int position);
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
        if(position == mPlayingPosition) {
            holder.name.setTextColor(Color.parseColor("#ff1f43"));
        } else {
            holder.name.setTextColor(Color.parseColor("#333333"));
        }

        holder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    @Override
    public void onClick(View view) {
        if(onItemClickListenerRf.get() != null) {
            onItemClickListenerRf.get().onItemClick(view, (int)view.getTag());
        }
    }

    public void setPlayingPosition(int position) {
        if(position >= 0 && position < mDataList.size()) {
            mPlayingPosition = position;
        }
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        onItemClickListenerRf = new WeakReference<>(listener);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        public MyViewHolder(View v){
            super(v);
            name = (TextView) v.findViewById(R.id.tv_name);
        }
    }
}
