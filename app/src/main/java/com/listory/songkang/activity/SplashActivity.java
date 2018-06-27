package com.listory.songkang.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.listory.songkang.R;


/**
 * Created by SouKou on 2017/7/22.
 */

public class SplashActivity extends Activity {
    private static final int START_MAIN_ACTIVITY_DELAY_MS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 判断是否是第一次开启应用
//        boolean isFirstOpen = SpUtils.getBoolean(this, AppConstants.FIRST_OPEN);
//        // 如果是第一次启动，则先进入功能引导页
//        if (!isFirstOpen) {
//            Intent intent = new Intent(this, WelcomeGuideActivity.class);
//            startActivity(intent);
//            finish();
//            return;
//        }

        // 如果不是第一次启动app，则正常显示启动屏
        setContentView(R.layout.activity_splash);
        new Handler().postDelayed(() -> enterHomeActivity(), START_MAIN_ACTIVITY_DELAY_MS);
    }

    private void enterHomeActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}