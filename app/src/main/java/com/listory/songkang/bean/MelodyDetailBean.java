package com.listory.songkang.bean;

import android.os.Parcel;
import android.os.Parcelable;

import com.listory.songkang.service.MusicTrack;

/**
 * Created by songkang on 2018/4/18.
 */

public class MelodyDetailBean extends BaseIconTitleBean implements Parcelable {
    /**
     * 曲目ID
     */
    public long id;
    /**
     * 曲目网络地址
     */
    public String url;

    /**
     * 本地地址
     */
    public String localUrl;
    /**
     * 曲目封面网络地址
     */
    public String coverImageUrl;
    /**
     * 曲目名称
     */
    public String title;
    /**
     * 曲目专辑
     */
    public String albumName;
    /**
     * 曲目艺术家、作者
     */
    public String artist;

    /**
     * 曲目标签
     */
    public String tags;

    /**
     * 是否喜爱
     */
    public String favorite;

    /**
     * 是否精品
     */
    public String isPrecious;

    public static final Creator<MelodyDetailBean> CREATOR = new Creator<MelodyDetailBean>() {
        @Override
        public MelodyDetailBean createFromParcel(Parcel source) {
            return new MelodyDetailBean(source);
        }

        @Override
        public MelodyDetailBean[] newArray(int size) {
            return new MelodyDetailBean[size];
        }
    };

    public MelodyDetailBean() {
        id = mId = -1;
    }

    public MelodyDetailBean(Parcel in) {
        id = in.readLong();
        url = in.readString();
        localUrl = in.readString();
        coverImageUrl = in.readString();
        title = in.readString();
        albumName = in.readString();
        artist = in.readString();
        favorite = in.readString();
        isPrecious = in.readString();
        mItemIconUrl = coverImageUrl;
        mItemTitle = title;
        mTags = tags;
        mPrecious = isPrecious;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(url);
        dest.writeString(localUrl);
        dest.writeString(coverImageUrl);
        dest.writeString(title);
        dest.writeString(albumName);
        dest.writeString(artist);
        dest.writeString(favorite);
        dest.writeString(isPrecious);
    }

    public MusicTrack convertToMusicTrack(){
        MusicTrack musicTrack = new MusicTrack();
        musicTrack.mId = this.id;
        musicTrack.mFavorite = this.favorite;
        musicTrack.mLocalUrl = this.localUrl;
        musicTrack.mAlbum = this.albumName;
        musicTrack.mArtist = this.artist;
        musicTrack.mUrl = this.url;
        musicTrack.mCoverImageUrl = this.coverImageUrl;
        musicTrack.mTitle = this.title;
        return musicTrack;
    }

    @Override
    public int hashCode() {
        int hashCode = (int) (id^(id >>>32));
        hashCode = 31*hashCode + url.hashCode();
        hashCode = 31*hashCode + localUrl.hashCode();
        hashCode = 31*hashCode + coverImageUrl.hashCode();
        hashCode = 31*hashCode + title.hashCode();
        hashCode = 31*hashCode + albumName.hashCode();
        hashCode = 31*hashCode + artist.hashCode();
        hashCode = 31*hashCode + tags.hashCode();
        hashCode = 31*hashCode + favorite.hashCode();
        hashCode = 31*hashCode + isPrecious.hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        MelodyDetailBean bean = (MelodyDetailBean)obj;
        if(this.id != bean.id) return false;
        if(this.url != null && !this.url.equals(bean.url)) return false;
        if(this.localUrl != null && !this.localUrl.equals(bean.localUrl)) return false;
        if(this.coverImageUrl != null && !this.coverImageUrl.equals(bean.coverImageUrl)) return false;
        if(this.title != null && !this.title.equals(bean.title)) return false;
        if(this.albumName != null && !this.albumName.equals(bean.albumName)) return false;
        if(this.artist != null && !this.artist.equals(bean.artist)) return false;
        if(this.tags != null && !this.tags.equals(bean.tags)) return false;
        if(this.favorite != null && !this.favorite.equals(bean.favorite)) return false;
        if(this.isPrecious != null && !this.isPrecious.equals(bean.isPrecious)) return false;
        return true;
    }
}
