package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.github.tvbox.osc.base.App;

/**
 * 启动路由 Activity
 * 根据设备类型自动选择 TV 或手机首页
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent;
        if (App.IS_TV) {
            // TV 设备:进入 TV 首页
            intent = new Intent(this, HomeActivity.class);
        } else {
            // 手机设备:进入手机首页
            intent = new Intent(this, MobileHomeActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
