/*
* Copyright (C) 2014 The CyanogenMod Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.zealens.listory.service;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * This is used by the music playback service to track the music tracks it is playing
 * It has extra meta data to determine where the track came from so that we can show the appropriate
 * song playing indicator
 */
public class MusicTrack implements Parcelable {


    public static final Creator<MusicTrack> CREATOR = new Creator<MusicTrack>() {
        @Override
        public MusicTrack createFromParcel(Parcel source) {
            return new MusicTrack(source);
        }

        @Override
        public MusicTrack[] newArray(int size) {
            return new MusicTrack[size];
        }
    };

    public long mId;

    public String mLocalUrl;
    /**
     * 曲目网络地址
     */
    public String mUrl;
    /**
     * 曲目封面网络地址
     */
    public String mCoverImageUrl;
    /**
     * 曲目名称
     */
    public String mTitle;
    /**
     * 曲目专辑
     */
    public String mAlbum;
    /**
     * 曲目艺术家、作者
     */
    public String mArtist;

    /**
     * 是否喜爱
     */
    public String mFavorite;


    public MusicTrack() {
        mId = -1;
    }

    public MusicTrack(Parcel in) {
        mId = in.readLong();
        mLocalUrl = in.readString();
        mUrl = in.readString();
        mCoverImageUrl = in.readString();
        mTitle = in.readString();
        mArtist = in.readString();
        mAlbum = in.readString();
        mFavorite = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeString(mLocalUrl);
        dest.writeString(mUrl);
        dest.writeString(mCoverImageUrl);
        dest.writeString(mTitle);
        dest.writeString(mArtist);
        dest.writeString(mAlbum);
        dest.writeString(mFavorite);
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("mUrl:");sb.append(mUrl);
        sb.append("mLocalUrl:");sb.append(mLocalUrl);
        sb.append(",mCoverImageUrl:");sb.append(mCoverImageUrl);
        sb.append(",mTitle:");sb.append(mTitle);
        sb.append(",mArtist:");sb.append(mArtist);
        sb.append(",mAlbum:");sb.append(mAlbum);
        sb.append(",mFavorite:");sb.append(mFavorite);
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        MusicTrack track = (MusicTrack)obj;
        if(this.mId != track.mId) return false;
        if(this.mUrl != null && !this.mUrl.equals(track.mUrl) || (this.mUrl == null && track.mUrl != null)) return false;
        if(this.mLocalUrl != null && !this.mLocalUrl.equals(track.mLocalUrl) || (this.mLocalUrl == null && track.mLocalUrl != null)) return false;
        if(this.mCoverImageUrl != null && !this.mCoverImageUrl.equals(track.mCoverImageUrl)
                || (this.mCoverImageUrl == null && track.mCoverImageUrl != null)) return false;
        if(this.mTitle != null && !this.mTitle.equals(track.mTitle) || (this.mTitle == null && track.mTitle != null)) return false;
        if(this.mAlbum != null && !this.mAlbum.equals(track.mAlbum) || (this.mAlbum == null && track.mAlbum != null)) return false;
        if(this.mArtist != null && !this.mArtist.equals(track.mArtist) || (this.mArtist == null && track.mArtist != null)) return false;
        if(this.mFavorite != null && !this.mFavorite.equals(track.mFavorite) || (this.mFavorite == null && track.mFavorite != null)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = (int)(mId^(mId >>> 32));
        hashCode = 31*hashCode + mUrl.hashCode();
        hashCode = 31*hashCode + mLocalUrl.hashCode();
        hashCode = 31*hashCode + mCoverImageUrl.hashCode();
        hashCode = 31*hashCode + mTitle.hashCode();
        hashCode = 31*hashCode + mAlbum.hashCode();
        hashCode = 31*hashCode + mArtist.hashCode();
        hashCode = 31*hashCode + mFavorite.hashCode();
        return hashCode;
    }
}
