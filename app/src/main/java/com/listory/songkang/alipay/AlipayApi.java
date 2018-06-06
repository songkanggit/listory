package com.listory.songkang.alipay;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.alipay.sdk.app.AuthTask;
import com.alipay.sdk.app.PayTask;

import java.util.Map;

/**
 * Created by songkang on 2018/4/20.
 */

public class AlipayApi {
    /**
     * 支付宝支付业务
     */
    public static void payV2(Activity activity, Handler handler, String totalMount, String productInfo, String body) {
        /**
         * 这里只是为了方便直接向商户展示支付宝的整个支付流程；所以Demo中加签过程直接放在客户端完成；
         * 真实App里，privateKey等数据严禁放在客户端，加签过程务必要放在服务端完成；
         * 防止商户私密数据泄露，造成不必要的资金损失，及面临各种安全风险；
         *
         * orderInfo的获取必须来自服务端；
         */
        boolean rsa2 = (AlipayConfig.RSA2_PRIVATE.length() > 0);
        Map<String, String> params = OrderInfoUtil2_0.buildOrderParamMap(AlipayConfig.APPID, totalMount, productInfo, body, rsa2);
        String orderParam = OrderInfoUtil2_0.buildOrderParam(params);

        String privateKey = rsa2 ? AlipayConfig.RSA2_PRIVATE : AlipayConfig.RSA_PRIVATE;
        String sign = OrderInfoUtil2_0.getSign(params, privateKey, rsa2);
        final String orderInfo = orderParam + "&" + sign;

        Runnable payRunnable = () -> {
            PayTask alipay = new PayTask(activity);
            Map<String, String> result = alipay.payV2(orderInfo, true);
            Log.i("msp", result.toString());

            Message msg = new Message();
            msg.what = AlipayConfig.SDK_PAY_FLAG;
            msg.obj = result;
            handler.sendMessage(msg);
        };

        Thread payThread = new Thread(payRunnable);
        payThread.start();
    }

    /**
     * 支付宝支付业务
     */
    public static void payV2(Activity activity, Handler handler, final String orderInfo) {
        PayTask alipay = new PayTask(activity);
        Map<String, String> result = alipay.payV2(orderInfo, true);
        Log.i("msp", result.toString());

        Message msg = new Message();
        msg.what = AlipayConfig.SDK_PAY_FLAG;
        msg.obj = result;
        handler.sendMessage(msg);
    }

    /**
     * 支付宝账户授权业务
     */
    public static void authV2(Activity activity, Handler handler) {
        /**
         * 这里只是为了方便直接向商户展示支付宝的整个支付流程；所以Demo中加签过程直接放在客户端完成；
         * 真实App里，privateKey等数据严禁放在客户端，加签过程务必要放在服务端完成；
         * 防止商户私密数据泄露，造成不必要的资金损失，及面临各种安全风险；
         *
         * authInfo的获取必须来自服务端；
         */
        boolean rsa2 = (AlipayConfig.RSA2_PRIVATE.length() > 0);
        Map<String, String> authInfoMap = OrderInfoUtil2_0.buildAuthInfoMap(AlipayConfig.PID, AlipayConfig.APPID, AlipayConfig.TARGET_ID, rsa2);
        String info = OrderInfoUtil2_0.buildOrderParam(authInfoMap);

        String privateKey = rsa2 ? AlipayConfig.RSA2_PRIVATE : AlipayConfig.RSA_PRIVATE;
        String sign = OrderInfoUtil2_0.getSign(authInfoMap, privateKey, rsa2);
        final String authInfo = info + "&" + sign;
        Runnable authRunnable = () -> {
            // 构造AuthTask 对象
            AuthTask authTask = new AuthTask(activity);
            // 调用授权接口，获取授权结果
            Map<String, String> result = authTask.authV2(authInfo, true);

            Message msg = new Message();
            msg.what = AlipayConfig.SDK_AUTH_FLAG;
            msg.obj = result;
            handler.sendMessage(msg);
        };

        // 必须异步调用
        Thread authThread = new Thread(authRunnable);
        authThread.start();
    }

    /**
     * 支付宝账户授权业务
     */
    public static void authV2(Activity activity, Handler handler, final String authInfo) {
        Runnable authRunnable = () -> {
            // 构造AuthTask 对象
            AuthTask authTask = new AuthTask(activity);
            // 调用授权接口，获取授权结果
            Map<String, String> result = authTask.authV2(authInfo, true);

            Message msg = new Message();
            msg.what = AlipayConfig.SDK_AUTH_FLAG;
            msg.obj = result;
            handler.sendMessage(msg);
        };

        // 必须异步调用
        Thread authThread = new Thread(authRunnable);
        authThread.start();
    }
}
