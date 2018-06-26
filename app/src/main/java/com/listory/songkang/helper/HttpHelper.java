package com.listory.songkang.helper;

import com.listory.songkang.bean.HttpResponseBean;
import com.listory.songkang.bean.MelodyDetailBean;
import com.listory.songkang.constant.DomainConst;
import com.listory.songkang.constant.PreferenceConst;
import com.listory.songkang.core.CoreContext;
import com.listory.songkang.core.http.HttpManager;
import com.listory.songkang.core.http.HttpService;
import com.listory.songkang.core.preference.PreferencesManager;
import com.listory.songkang.utils.StringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kousou on 2018/6/26.
 */

public class HttpHelper {

    public static void requestLikeMelody(final CoreContext coreContext, final JSONObject param, CallBack callBack) {
        coreContext.executeAsyncTask(() -> {
            try {
                HttpResponseBean responseBean = new HttpResponseBean();
                HttpService httpService = coreContext.getApplicationService(HttpManager.class);
                String response = httpService.post(DomainConst.FAVORITE_REQUEST_URL, param.toString());
                JSONObject responseObject = new JSONObject(response);
                if(callBack != null && responseObject.getBoolean("state")) {
                    responseBean.setState(responseObject.getBoolean("data"));
                    callBack.onDataComplete(responseBean);
                }
            } catch (JSONException e){
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
    public static void requestMelodyList(final CoreContext coreContext, final JSONObject params,
                                         final List<MelodyDetailBean> dataList, final String requestUrl, CallBack callBack) {
        coreContext.executeAsyncTask(() -> {
            int tryTimes = 3;
            HttpResponseBean responseBean = new HttpResponseBean();
            do {
                HttpService httpService = coreContext.getApplicationService(HttpManager.class);
                PreferencesManager preferencesManager = coreContext.getApplicationService(PreferencesManager.class);
                int accountId = preferencesManager.get(PreferenceConst.ACCOUNT_ID, -1);
                try {
                    if(accountId != -1) {
                        params.put("accountId", accountId);
                    }
                    String url = DomainConst.MELODY_LIST_URL;
                    if(!StringUtil.isEmpty(requestUrl)) {
                        url = requestUrl;
                    }
                    String response = httpService.post(url, params.toString());
                    JSONObject responseObject = new JSONObject(response);

                    responseBean.setCount(responseObject.getString("count"));
                    responseBean.setPageSize(responseObject.getString("pageSize"));
                    responseBean.setCurrentPage(responseObject.getString("currentPage"));
                    responseBean.setState(responseObject.getBoolean("state"));

                    JSONArray dataArray;

                    if(!StringUtil.isEmpty(requestUrl)) {
                        dataArray = responseObject.getJSONArray("data");
                    } else {
                        JSONObject dataObject = responseObject.getJSONObject("data");
                        dataArray = dataObject.getJSONArray("melodyList");
                    }

                    List<MelodyDetailBean> tempList = new ArrayList<>();
                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject temp = dataArray.getJSONObject(i);
                        MelodyDetailBean bean = new MelodyDetailBean();
                        bean.id = temp.getLong("id");
                        bean.url = DomainConst.MEDIA_DOMAIN + temp.getString("melodyFilePath");
                        bean.coverImageUrl = DomainConst.PICTURE_DOMAIN + temp.getString("melodyCoverImage");
                        bean.albumName = temp.getString("melodyAlbum");
                        bean.title = temp.getString("melodyName");
                        bean.artist = temp.getString("melodyArtist");
                        bean.favorite = temp.getString("favorated");
                        bean.tags = temp.getString("melodyCategory");
                        bean.isPrecious = temp.getString("melodyPrecious");
                        bean.mItemTitle = bean.title;
                        bean.mItemIconUrl = bean.coverImageUrl;
                        bean.mTags = bean.tags;
                        bean.mPrecious = bean.isPrecious;
                        tempList.add(bean);
                    }
                    dataList.clear();
                    dataList.addAll(tempList);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } while(--tryTimes > 0 && dataList.size() == 0);
            if(callBack != null) {
                callBack.onDataComplete(responseBean);
            }
        });
    }
    public interface CallBack {
        void onDataComplete(HttpResponseBean response);
    }
}
