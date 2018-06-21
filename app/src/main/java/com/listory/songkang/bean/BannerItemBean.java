package com.listory.songkang.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by songkang on 2018/5/2.
 */

public class BannerItemBean implements Parcelable {
    private long id;
    private long contentId;
    private String bannerImageUrl;
    private int contentType;
    private int orderIndex;
    private Object data;

    public static final Creator<BannerItemBean> CREATOR = new Creator<BannerItemBean>() {
        @Override
        public BannerItemBean createFromParcel(Parcel source) {
            return new BannerItemBean(source);
        }

        @Override
        public BannerItemBean[] newArray(int size) {
            return new BannerItemBean[size];
        }
    };

    public BannerItemBean() {
        id = -1;
    }

    public BannerItemBean(Parcel in) {
        this.id = in.readLong();
        this.contentId = in.readLong();
        this.bannerImageUrl = in.readString();
        this.contentType = in.readInt();
        this.orderIndex = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(contentId);
        dest.writeString(bannerImageUrl);
        dest.writeInt(contentType);
        dest.writeInt(orderIndex);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getContentId() {
        return contentId;
    }

    public void setContentId(long contentId) {
        this.contentId = contentId;
    }

    public String getBannerImageUrl() {
        return bannerImageUrl;
    }

    public void setBannerImageUrl(String bannerImageUrl) {
        this.bannerImageUrl = bannerImageUrl;
    }

    public int getContentType() {
        return contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
