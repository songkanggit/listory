package com.listory.songkang.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.listory.songkang.bean.MelodyDetailBean;
import com.listory.songkang.constant.PreferenceConst;
import com.listory.songkang.listory.R;
import com.listory.songkang.utils.QiniuImageUtil;
import com.listory.songkang.utils.StringUtil;
import com.listory.songkang.view.CachedImageView;

import org.intellij.lang.annotations.MagicConstant;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by songkang on 2018/4/13.
 */

public class RecyclerViewMelodyListAdapter extends RecyclerView.Adapter<RecyclerViewMelodyListAdapter.MyViewHolder> implements View.OnClickListener{

    private static final String TAG = RecyclerViewMelodyListAdapter.class.getSimpleName();
    private static final int FOOTER_VIEW_THRESHOLD = 7;
    private Context mContext;
    private List<MelodyDetailBean> mDataList;
    private WeakReference<OnItemClickListener> mOnItemClickListener = null;
    private boolean isLoadMore;
    private boolean isVip;

    @MagicConstant(intValues = {ItemType.TYPE_ITEM, ItemType.TYPE_FOOTER})
    public @interface ItemType {
        int TYPE_ITEM = 0;
        int TYPE_FOOTER = 1;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, final int position);
        void onLikeClick(final int position, Callback callback);
        void onDownloadClick(final int position, Callback callback);
    }

    public interface Callback {
        void onSuccess();
        void onFailed();
    }

    public RecyclerViewMelodyListAdapter(Context context, List<MelodyDetailBean> dataList) {
        mContext = context;
        mDataList = dataList;
        isLoadMore = true;
        SharedPreferences pref = mContext.getSharedPreferences(PreferenceConst.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        isVip = pref.getBoolean(PreferenceConst.ACCOUNT_VIP, false);
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == ItemType.TYPE_ITEM) {
            View holder = LayoutInflater.from(mContext).inflate(R.layout.recycler_view_holder_melody, null);
            holder.setOnClickListener(this);
            return new ItemViewHolder(holder);
        } else {
            View holder = LayoutInflater.from(mContext).inflate(R.layout.list_view_footer, parent, false);
            return new FooterViewHolder(holder);
        }
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        if(holder instanceof ItemViewHolder) {
            ItemViewHolder itemHolder = (ItemViewHolder)holder;
            MelodyDetailBean bean = mDataList.get(position);
            final String imageUrl = bean.coverImageUrl + QiniuImageUtil.generateFixSizeImageAppender(mContext, QiniuImageUtil.ImageType.MELODY_SQUARE_S);
            itemHolder.autoLoadImageView.setImageUrl(imageUrl);
            itemHolder.nameTextView.setText(bean.title);
            itemHolder.authorTextView.setText(bean.artist);
            if(bean.isPrecious.equals("true")) {
                itemHolder.vipImageView.setVisibility(View.VISIBLE);
                if(!isVip) {
                    itemHolder.nameTextView.setTextColor(Color.parseColor("#cccccc"));
                    itemHolder.authorTextView.setTextColor(Color.parseColor("#cccccc"));
                }
            } else {
                itemHolder.vipImageView.setVisibility(View.GONE);
            }
            if(!StringUtil.isEmpty(bean.favorite) && bean.favorite.equals("true")) {
                itemHolder.favoriteImageView.setImageResource(R.mipmap.melody_like);
            } else {
                itemHolder.favoriteImageView.setImageResource(R.mipmap.melody_unlike);
            }
            itemHolder.favoriteImageView.setOnClickListener(v -> {
                if(mOnItemClickListener.get() != null) {
                    mOnItemClickListener.get().onLikeClick(position, new Callback() {
                        @Override
                        public void onSuccess() {
                            bean.favorite = "true";
                            itemHolder.favoriteImageView.setImageResource(R.mipmap.melody_like);
                        }

                        @Override
                        public void onFailed() {
                            bean.favorite = "false";
                            itemHolder.favoriteImageView.setImageResource(R.mipmap.melody_unlike);
                        }
                    });
                }
            });

            itemHolder.itemView.setTag(position);
        } else if(holder instanceof FooterViewHolder) {
            FooterViewHolder footerView = (FooterViewHolder) holder;
            if (isLoadMore) {
                footerView.progressBar.setVisibility(View.VISIBLE);
                footerView.loadMoreTextView.setText(R.string.app_load_more);
            } else {
                footerView.progressBar.setVisibility(View.GONE);
                footerView.loadMoreTextView.setText(R.string.app_load_no_more);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(position == mDataList.size()) {
            return ItemType.TYPE_FOOTER;
        } else {
            return ItemType.TYPE_ITEM;
        }
    }

    @Override
    public void onClick(View v) {
        if(mOnItemClickListener.get() != null) {
            mOnItemClickListener.get().onItemClick(v, (int) v.getTag());
        }
    }

    @Override
    public int getItemCount() {
        if(mDataList.size() < FOOTER_VIEW_THRESHOLD) {
            return mDataList.size();
        }
        return mDataList.size() + 1;
    }

    public void setOnItemClickListener(final OnItemClickListener listener) {
        mOnItemClickListener = new WeakReference<>(listener);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public MyViewHolder(View v){
            super(v);
        }
    }

    public class ItemViewHolder extends MyViewHolder {
        public CachedImageView autoLoadImageView;
        public TextView nameTextView, authorTextView;
        public ImageView favoriteImageView, vipImageView;
        public ItemViewHolder(View v) {
            super(v);
            autoLoadImageView = (CachedImageView)v.findViewById(R.id.iv_icon);
            nameTextView = (TextView) v.findViewById(R.id.tv_name);
            authorTextView = (TextView) v.findViewById(R.id.tv_author);
            favoriteImageView = (ImageView) v.findViewById(R.id.iv_favorite);
            vipImageView = (ImageView) v.findViewById(R.id.iv_vip);
        }
    }

    public class FooterViewHolder extends MyViewHolder {
        ProgressBar progressBar;
        TextView loadMoreTextView;
        public FooterViewHolder(View v) {
            super(v);
            progressBar = (ProgressBar)v.findViewById(R.id.progress_bar);
            loadMoreTextView = (TextView) v.findViewById(R.id.tv_pull_refresh);
        }
    }

    public boolean isLoadMore() {
        return isLoadMore;
    }

    public void setLoadMore(boolean loadMore) {
        isLoadMore = loadMore;
    }
}
