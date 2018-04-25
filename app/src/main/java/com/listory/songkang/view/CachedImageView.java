package com.listory.songkang.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

import com.listory.songkang.image.ImageLoader;
import com.listory.songkang.utils.StringUtil;


/**
 * Created by songkang on 2018/4/19.
 */

public class CachedImageView extends AppCompatImageView {
    public CachedImageView(Context context) {
        super(context);
    }

    public CachedImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CachedImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setImageUrl(String imageUrl){
        if(!StringUtil.isEmpty(imageUrl)) {
            ImageLoader.getInstance().loadImageView(this, imageUrl);
        }
    }
}
