package com.listory.songkang.bean;

/**
 * Created by songkang on 2018/4/25.
 */

public class Melody {
    private String id;
    private String name;
    private String author;
    private String icon;
    private String url;
    private String like;

    public Melody(String name){
        this.name = name;
        this.author = "杨梅、西柚、榴莲";
        this.like = "0";
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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLike() {
        return like;
    }

    public void setLike(String like) {
        this.like = like;
    }
}
