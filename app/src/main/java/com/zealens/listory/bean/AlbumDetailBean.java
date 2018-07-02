package com.zealens.listory.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by songkang on 2018/4/18.
 */

public class AlbumDetailBean extends BaseIconTitleBean implements Parcelable {
    public long id;
    public String albumName;
    public String albumCoverImage;
    public String albumAbstract;
    public String isPrecious;
    public String createTime;
    public String tags;

    public static final Creator<AlbumDetailBean> CREATOR = new Creator<AlbumDetailBean>() {
        @Override
        public AlbumDetailBean createFromParcel(Parcel source) {
            return new AlbumDetailBean(source);
        }

        @Override
        public AlbumDetailBean[] newArray(int size) {
            return new AlbumDetailBean[size];
        }
    };

    public AlbumDetailBean() {
        mId = -1;
    }

    public AlbumDetailBean(Parcel in) {
        id = in.readLong();
        albumName = in.readString();
        albumCoverImage = in.readString();
        albumAbstract = in.readString();
        isPrecious = in.readString();
        createTime = in.readString();
        mItemIconUrl = albumCoverImage;
        mItemTitle = albumName;
        mPrecious = isPrecious;
        mTags = tags;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(albumName);
        dest.writeString(albumCoverImage);
        dest.writeString(albumAbstract);
        dest.writeString(isPrecious);
        dest.writeString(createTime);
    }
}
