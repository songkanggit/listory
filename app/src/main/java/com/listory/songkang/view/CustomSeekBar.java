package com.listory.songkang.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;

import com.listory.songkang.R;

/**
 * Created by SouKou on 2018/1/12.
 */

public class CustomSeekBar extends AppCompatSeekBar {
    public CustomSeekBar(Context context) {
        super(context);
    }

    public CustomSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        Bitmap bitmap= BitmapFactory.decodeResource(getResources(), R.mipmap.seekbar_thumb);
        Bitmap thumb= Bitmap.createBitmap(28, 28, Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(thumb);
        canvas.drawBitmap(bitmap,new Rect(0,0,bitmap.getWidth(),bitmap.getHeight()),
                new Rect(0,0,thumb.getWidth(),thumb.getHeight()),null);
        Drawable drawable = new BitmapDrawable(getResources(),thumb);
        setThumb(drawable);
    }
}
