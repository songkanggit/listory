package com.listory.songkang.core.http;

import android.support.annotation.Nullable;


import com.listory.songkang.core.BaseCoreManager;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpManager extends BaseCoreManager implements HttpService {
    private OkHttpClient mClient;
    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Override
    public void initialize() {
        super.initialize();
        mClient = new OkHttpClient.Builder().readTimeout(20_000, TimeUnit.MILLISECONDS).build();
    }

    @Override
    public String get(String url) throws IOException {
        return run(url);
    }

    @Override
    public String run(String url) throws IOException {
        Request request = assembleRequest(url);
        return executeRequest(request).body().string();
    }

    @Override
    public String post(String url, Map map) throws IOException {
        JSONObject jsonObject = new JSONObject(map);
        return post(url, jsonObject.toString());
    }

    @Override
    public String post(String url, String jsonStr) throws IOException {
        RequestBody body = RequestBody.create(JSON, jsonStr);
        Request request = assembleRequest(url, body);
        return executeRequest(request).body().string();
    }

    @Override
    public void enqueue(String url, Callback callback) {
        enqueueGet(url, callback);
    }

    @Override
    public void enqueueGet(String url, Callback callback) {
        Request request = assembleRequest(url);
        enqueueRequest(request, callback);
    }

    @Override
    public void enqueuePost(String url, String json, Callback callback) {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = assembleRequest(url, body);
        enqueueRequest(request, callback);
    }

    private Request assembleRequest(String url) {
        return assembleRequestBuilder(url).build();
    }

    private Request assembleRequest(String url, RequestBody body) {
        return assembleRequestBuilder(url).post(body).build();
    }

    private Request.Builder assembleRequestBuilder(String url) {
        return new Request.Builder().url(url);
    }

    private Response executeRequest(Request request) throws IOException {
        return mClient.newCall(request).execute();
    }

    public void enqueueRequest(Request request, @Nullable Callback callback) {
        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                if (callback != null) callback.onFailure(call, e);
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (callback != null) callback.onResponse(call, response);
            }
        });
    }

    @Override
    public int order() {
        return ORDER.HTTP;
    }

    @Override
    public void freeMemory() {

    }
}
