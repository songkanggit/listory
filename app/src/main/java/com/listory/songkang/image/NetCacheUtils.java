package com.listory.songkang.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 网络加载
 * Created by songkang on 2018/4/19.
 */

public class NetCacheUtils {
    private LocalCacheUtils mLocalCacheUtils;
    private MemoryCacheUtils mMemoryCacheUtils;

    public NetCacheUtils(LocalCacheUtils localCacheUtils, MemoryCacheUtils memoryCacheUtils) {
        mLocalCacheUtils = localCacheUtils;
        mMemoryCacheUtils = memoryCacheUtils;
    }

    /**
     * 从网络下载图片
     * @param imageView
     * @param url   下载图片的网络地址
     */
    public void getBitmapFromNet(ImageView imageView, String url) {
        //new BitmapTask().execute(mImageView, url);//启动AsyncTask
        getBitmapFromNet(imageView, url, null);
    }

    /**
     * 从网络下载图片
     * @param imageView 显示图片的imageview
     * @param url   下载图片的网络地址
     */
    public void getBitmapFromNet(ImageView imageView, String url, ImageLoader.ImageDownLoadCallback callback) {
        WeakReference<ImageView> localRef = new WeakReference<>(imageView);
        new BitmapTask().execute(localRef, url, callback);//启动AsyncTask
    }

    /**
     * AsyncTask就是对handler和线程池的封装
     * 第一个泛型:参数类型
     * 第二个泛型:更新进度的泛型
     * 第三个泛型:onPostExecute的返回结果
     */
    class BitmapTask extends AsyncTask<Object, Void, Bitmap> {
        private String mUrl;
        private WeakReference<ImageView> mImageView;
        private WeakReference<ImageLoader.ImageDownLoadCallback> mCallback;

        /**
         * 后台耗时操作,存在于子线程中
         * @param params
         * @return
         */
        @Override
        protected Bitmap doInBackground(Object[] params) {
            mUrl = (String) params[1];
            mImageView =  (WeakReference<ImageView>) params[0];
            mCallback = new WeakReference<>((ImageLoader.ImageDownLoadCallback)params[2]);
            return downLoadBitmap(mUrl);
        }

        /**
         * 更新进度,在主线程中
         * @param values
         */
        @Override
        protected void onProgressUpdate(Void[] values) {
            super.onProgressUpdate(values);
        }

        /**
         * 耗时方法结束后执行该方法,主线程中
         * @param result
         */
        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null && mImageView.get() != null) {
                mImageView.get().setImageBitmap(result);
                //从网络获取图片后,保存至本地缓存
                mLocalCacheUtils.setBitmapToLocal(mUrl, result);
                //保存至内存中
                mMemoryCacheUtils.setBitmapToMemory(mUrl, result);
                if(mCallback.get() != null) {
                    mCallback.get().onImageLoadComplete(mUrl);
                }
            }
        }
    }

    /**
     * 网络下载图片
     * @param url
     * @return
     */
    private Bitmap downLoadBitmap(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                //图片压缩
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize=2;//宽高压缩为原来的1/2
                options.inPreferredConfig=Bitmap.Config.ARGB_4444;

                Bitmap bitmap = BitmapFactory.decodeStream(conn.getInputStream(),null,options);
                return bitmap;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(conn != null)
            conn.disconnect();
        }
        return null;
    }
}
