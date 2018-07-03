package com.zealens.listory.core.http;

import java.io.IOException;
import java.util.Map;

import okhttp3.Callback;
import okhttp3.Request;

/**
 * Created by Kyle on 05/07/2017
 */

public interface HttpService {
    String get(String url) throws IOException;

    String run(String url) throws IOException;

    String post(String url, Map map) throws IOException;

    String post(String url, String json) throws IOException;

    void enqueue(String url, Callback callback);

    void enqueueGet(String url, Callback callback);

    void enqueuePost(String url, String json, Callback callback);

    void enqueueRequest(Request request, Callback callback);

    String uploadImageRequest(final String postUrl, final String imagePath);
}
