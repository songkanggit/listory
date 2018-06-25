package com.listory.songkang.utils;

import android.content.Context;

import org.intellij.lang.annotations.MagicConstant;

/**
 * Created by songkang on 2018/5/31.
 */

public class QiniuImageUtil {

    @MagicConstant(intValues = {ImageType.ALBUM_SQUARE, ImageType.ALBUM_RECT,
            ImageType.MELODY_SQUARE_S, ImageType.MELODY_SQUARE_M, ImageType.MELODY_SQUARE_L,
            ImageType.MELODY_RECT, ImageType.COMMON_SQUARE, ImageType.COMMON_RECT})
    public @interface ImageType {
        int ALBUM_SQUARE = 0;
        int ALBUM_RECT = 1;
        int MELODY_SQUARE_S = 2;
        int MELODY_SQUARE_M = 3;
        int MELODY_SQUARE_L = 4;
        int MELODY_RECT = 5;
        int COMMON_SQUARE = 6;
        int COMMON_RECT = 7;
        int BANNER = 8;
        int DISCOVERY = 9;
    }

    public static final String generateFixSizeImageAppender(Context context, @ImageType int imageType) {
        return generateFixSizeImageAppender(context, imageType, -1, -1);
    }

    public static final String generateFixSizeImageAppender(Context context, @ImageType int imageType, int w, int h) {
        String appender = "";
        switch (imageType) {
            case ImageType.ALBUM_SQUARE:
                appender = getFixSizeSquareImageAppender(context, 290);
                break;
            case ImageType.ALBUM_RECT:
                appender = getFixSizeRectImageAppender(context, 375, 250);
                break;
            case ImageType.MELODY_SQUARE_S:
                appender = getFixSizeSquareImageAppender(context, 60);
                break;
            case ImageType.MELODY_SQUARE_M:
                appender = getFixSizeSquareImageAppender(context, 122);
                break;
            case ImageType.MELODY_SQUARE_L:
                appender = getFixSizeSquareImageAppender(context, 302);
                break;
            case ImageType.MELODY_RECT:
                appender = getFixSizeRectImageAppender(context, 185, 115);
                break;
            case ImageType.COMMON_SQUARE:
                appender = getFixSizeSquareImageAppender(context, w);
                break;
            case ImageType.COMMON_RECT:
                appender = getFixSizeRectImageAppender(context, w, h);
                break;
            case ImageType.BANNER:
                appender = getFixSizeRectImageAppender(context, 270, 478);
                break;
            case ImageType.DISCOVERY:
                appender = getFixSizeRectImageAppender(context, 80, 110);
                break;
        }
        return appender;
    }

    private static final String getFixSizeSquareImageAppender(final Context context, int dip) {
        final int width = DensityUtil.dip2px(context, dip);
        final int height = width;
        return "?imageView2/1/w/"+ width +"/h/"+ height +"/q/75|imageslim";
    }

    private static final String getFixSizeRectImageAppender(final Context context, int wdip, int hdip) {
        final int width = DensityUtil.dip2px(context, wdip);
        final int height = DensityUtil.dip2px(context, hdip);
        return "?imageView2/1/w/"+ width +"/h/"+ height +"/q/75|imageslim";
    }

    private static final String getFixHeightAppender(final Context context, int dip) {
        final int height = DensityUtil.dip2px(context, dip);
        return "?imageView2/1/w/"+ DensityUtil.deviceDisplayWidth(context) +"/h/"+ height +"/q/75|imageslim";
    }
}
