package com.github.tvbox.osc.base;

import android.os.Handler;
import android.os.Looper;

/**
 * TVBOX-NEXT 优化#11: 手机端 Activity 基类
 * 提供通用的 Handler 管理和生命周期清理,避免内存泄漏
 * 所有手机端 Activity 应继承此类
 */
public abstract class BaseMobileActivity extends BaseActivity {

    // TVBOX-NEXT 优化#11: 通用的主线程 Handler,子类可直接使用
    protected final Handler mMobileHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TVBOX-NEXT 优化#11: 统一清理 Handler 回调,避免内存泄漏
        mMobileHandler.removeCallbacksAndMessages(null);
    }
}
