package com.zealens.listory.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.Toast;

import com.zealens.listory.R;
import com.zealens.listory.constant.DomainConst;
import com.zealens.listory.image.ImageLoader;
import com.zealens.listory.utils.WxUtil;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.jetbrains.annotations.NonNls;

/**
 * Created by SouKou on 2017/8/1.
 */

public class WeiXinHelper {
    private static final int THUMB_SIZE = 150;
    static WeiXinHelper s_instance = new WeiXinHelper();

    private WeiXinHelper(){

    }

    public static WeiXinHelper getInstance() {
        return s_instance;
    }

    /**
     * WeChat share method
     * @param shareUrl
     * @param sharedTitle
     */
    public void shareToWeChat(Context context, @NonNls final String shareUrl,
                              final String sharedTitle, final String iconUrl){
        IWXAPI wxApi = WXAPIFactory.createWXAPI(context, DomainConst.APP_ID);
        if(wxApi.isWXAppInstalled()) {
            WXWebpageObject webPage = new WXWebpageObject();
            webPage.webpageUrl = shareUrl;
            WXMediaMessage msg = new WXMediaMessage(webPage);
            msg.title = sharedTitle;
            msg.description = "李扬故事汇";
            Bitmap bmp = ImageLoader.getInstance().getCachedBitmap(context.getResources(), iconUrl);
            Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, THUMB_SIZE, THUMB_SIZE, true);
//            bmp.recycle();
            msg.thumbData = WxUtil.bmpToByteArray(thumbBmp, true);
            SendMessageToWX.Req req = new SendMessageToWX.Req();
            req.transaction = buildTransaction("LiYang");
            req.message = msg;
            req.scene = SendMessageToWX.Req.WXSceneSession;
            wxApi.sendReq(req);
        } else {
            Toast.makeText(context, R.string.wx_not_installed, Toast.LENGTH_LONG).show();
        }
    }

    public void wxPayReq(Context context, PayReq payReq){
        IWXAPI wxApi = WXAPIFactory.createWXAPI(context, DomainConst.APP_ID);
        if(wxApi.isWXAppInstalled()) {
            wxApi.sendReq(payReq);
        } else {
            Toast.makeText(context, R.string.wx_not_installed, Toast.LENGTH_LONG).show();
        }
    }

    public void wxLoginReq(Context context) {
        IWXAPI wxApi = WXAPIFactory.createWXAPI(context, DomainConst.APP_ID);
        if(wxApi.isWXAppInstalled()) {
            final SendAuth.Req req = new SendAuth.Req();
            req.scope = "snsapi_userinfo";
            req.state = "diandi_wx_login";
            wxApi.sendReq(req);
        } else {
            Toast.makeText(context, R.string.wx_not_installed, Toast.LENGTH_LONG).show();
        }
    }

    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }
}