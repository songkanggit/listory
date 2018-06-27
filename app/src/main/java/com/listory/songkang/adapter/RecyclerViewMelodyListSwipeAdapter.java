package com.listory.songkang.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.listory.songkang.bean.MelodyDetailBean;
import com.listory.songkang.R;
import com.listory.songkang.utils.QiniuImageUtil;
import com.listory.songkang.utils.StringUtil;
import com.listory.songkang.view.CachedImageView;

import org.intellij.lang.annotations.MagicConstant;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by songkang on 2018/4/13.
 */

public class RecyclerViewMelodyListSwipeAdapter extends RecyclerView.Adapter<RecyclerViewMelodyListSwipeAdapter.MyViewHolder> implements View.OnClickListener{

    private static final String TAG = RecyclerViewMelodyListSwipeAdapter.class.getSimpleName();
    private static final int FOOTER_VIEW_THRESHOLD = 7;
    private Context mContext;
    private List<MelodyDetailBean> mDataList;
    private WeakReference<OnItemClickListener> mOnItemClickListenerRf;
    private boolean isLoadMore;

    @MagicConstant(intValues = {ItemType.TYPE_ITEM, ItemType.TYPE_FOOTER})
    public @interface ItemType {
        int TYPE_ITEM = 0;
        int TYPE_FOOTER = 1;
    }
    public interface OnItemClickListener {
        void onItemClick(View view, final int position);
        void onLikeClick(final int position, Callback callback);
        void onDeleteClick(final int position);
        void onDownloadClick(final int position, Callback callback);
    }

    public interface Callback {
        void onSuccess();
        void onFailed();
    }

    public RecyclerViewMelodyListSwipeAdapter(Context context, List<MelodyDetailBean> dataList) {
        mContext = context;
        mDataList = dataList;
        isLoadMore = true;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == ItemType.TYPE_ITEM) {
            View holder = LayoutInflater.from(mContext).inflate(R.layout.recycler_view_holder_melody_swipe, null);
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
            if(!StringUtil.isEmpty(bean.isPrecious) && bean.isPrecious.equals("true")) {
                itemHolder.vipImageView.setVisibility(View.VISIBLE);
            } else {
                itemHolder.vipImageView.setVisibility(View.GONE);
            }
            itemHolder.deleteTextView.setOnClickListener(v -> {
                if(mOnItemClickListenerRf.get() != null) {
                    mOnItemClickListenerRf.get().onDeleteClick(position);
                }
            });
            itemHolder.contentRY.setTag(position);
            itemHolder.contentRY.setOnClickListener(this);
        } else if(holder instanceof FooterViewHolder){
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
    public void onClick(View v) {
        if(mOnItemClickListenerRf.get() != null) {
            mOnItemClickListenerRf.get().onItemClick(v, (int)v.getTag());
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
    public int getItemCount() {
        if(mDataList.size() < FOOTER_VIEW_THRESHOLD) {
            return mDataList.size();
        }
        return mDataList.size() + 1;
    }

    public void setOnItemClickListener(final OnItemClickListener listener) {
        mOnItemClickListenerRf = new WeakReference<>(listener);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public MyViewHolder(View v){
            super(v);
        }
    }

    public class ItemViewHolder extends MyViewHolder {
        public CachedImageView autoLoadImageView;
        public TextView nameTextView, authorTextView, deleteTextView;
        public ImageView vipImageView;
        public RelativeLayout contentRY;
        public ItemViewHolder(View v) {
            super(v);
            autoLoadImageView = (CachedImageView)v.findViewById(R.id.iv_icon);
            nameTextView = (TextView) v.findViewById(R.id.tv_name);
            authorTextView = (TextView) v.findViewById(R.id.tv_author);
            deleteTextView = (TextView) v.findViewById(R.id.tv_delete);
            contentRY = (RelativeLayout) v.findViewById(R.id.content_ry);
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
