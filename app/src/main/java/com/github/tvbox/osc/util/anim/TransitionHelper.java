package com.github.tvbox.osc.util.anim;

import android.app.Activity;
import android.app.ActivityOptions;
import android.os.Build;
import android.view.View;

import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;

/**
 * 转场动画辅助类(TV/手机共用)
 * 封装共享元素转场(一镜到底)
 */
public class TransitionHelper {

    /**
     * 为 View 设置 transitionName(共享元素名称)
     *
     * @param view           目标 View
     * @param transitionName 共享元素名称
     */
    public static void setTransitionName(View view, String transitionName) {
        ViewCompat.setTransitionName(view, transitionName);
    }

    /**
     * 启动 Activity 并使用共享元素转场
     *
     * @param activity    当前 Activity
     * @param intent      目标 Intent
     * @param sharedView  共享的 View
     * @param transitionName 共享元素名称
     */
    public static void startActivityWithSharedElement(Activity activity, android.content.Intent intent,
                                                       View sharedView, String transitionName) {
        setTransitionName(sharedView, transitionName);
        ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation(activity, sharedView, transitionName);
        activity.startActivity(intent, options.toBundle());
    }

    /**
     * 启动 Activity 并使用多个共享元素转场
     *
     * @param activity    当前 Activity
     * @param intent      目标 Intent
     * @param sharedElements 共享元素数组(View + transitionName)
     */
    @SafeVarargs
    public static void startActivityWithSharedElements(Activity activity, android.content.Intent intent,
                                                        Pair<View, String>... sharedElements) {
        ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation(activity, sharedElements);
        activity.startActivity(intent, options.toBundle());
    }

    /**
     * 在详情页设置共享元素转场
     * 延迟过渡,等待图片加载完成
     *
     * @param activity 目标 Activity
     */
    public static void setupDetailTransition(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.postponeEnterTransition();
        }
    }

    /**
     * 图片加载完成后启动转场
     *
     * @param activity 目标 Activity
     */
    public static void startPostponedTransition(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.startPostponedEnterTransition();
        }
    }
}
