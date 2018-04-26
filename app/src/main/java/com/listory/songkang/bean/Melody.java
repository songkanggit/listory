package com.listory.songkang.bean;

import android.os.Parcel;
import android.os.Parcelable;

import com.listory.songkang.service.MusicTrack;

/**
 * Created by songkang on 2018/4/25.
 */

public class Melody implements Parcelable {
    private String id;
    private String name;
    private String url;
    private String icon;
    private String author;
    private String like;

    public static final Creator<Melody> CREATOR = new Creator<Melody>() {
        @Override
        public Melody createFromParcel(Parcel parcel) {
            return new Melody(parcel);
        }

        @Override
        public Melody[] newArray(int i) {
            return new Melody[i];
        }
    };

    public Melody(Parcel in) {
        id = in.readString();
        name = in.readString();
        url = in.readString();
        icon = in.readString();
        author = in.readString();
        like = in.readString();
    }

    public Melody(String name, String url, String icon, String author){
        this.name = name;
        this.author = author;
        this.icon = icon;
        this.url = url;
        this.like = "0";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(name);
        parcel.writeString(url);
        parcel.writeString(icon);
        parcel.writeString(author);
        parcel.writeString(like);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getLike() {
        return like;
    }

    public void setLike(String like) {
        this.like = like;
    }

    public MusicTrack convertToMusicTrack(){
        MusicTrack musicTrack = new MusicTrack();
        musicTrack.mFavorite = this.like;
        musicTrack.mArtist = this.author;
        musicTrack.mUrl = this.url;
        musicTrack.mCoverImageUrl = this.icon;
        musicTrack.mTitle = this.name;
        return musicTrack;
    }
}
