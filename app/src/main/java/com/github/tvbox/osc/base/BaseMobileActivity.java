package com.github.tvbox.osc.base;

import android.os.Handler;
import android.os.Looper;
import android.view.View;

/**
 * TVBOX-NEXT 优化#11: 手机端 Activity 基类
 * 提供通用的 Handler 管理和生命周期清理,避免内存泄漏
 * 所有手机端 Activity 应继承此类
 */
public abstract class BaseMobileActivity extends BaseActivity {

    // TVBOX-NEXT 优化#11: 通用的主线程 Handler,子类可直接使用
    protected final Handler mMobileHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onResume() {
        super.onResume();
        // 手机端:只隐藏状态栏,保留导航栏(不像 TV 端那样全屏沉浸)
        showSystemUI();
        hideStatusBar();
    }

    /**
     * 手机端:仅隐藏状态栏,保留底部导航栏
     */
    private void hideStatusBar() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                int uiVisibility = getWindow().getDecorView().getSystemUiVisibility();
                uiVisibility |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                uiVisibility |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                uiVisibility |= View.SYSTEM_UI_FLAG_FULLSCREEN;
                uiVisibility |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                // 不隐藏导航栏
                uiVisibility &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                uiVisibility &= ~View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
                getWindow().getDecorView().setSystemUiVisibility(uiVisibility);
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TVBOX-NEXT 优化#11: 统一清理 Handler 回调,避免内存泄漏
        mMobileHandler.removeCallbacksAndMessages(null);
    }
}
