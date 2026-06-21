package com.github.tvbox.osc.util.anim;

import android.view.View;

import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

/**
 * Spring 动画辅助类(TV/手机共用)
 * 封装常用的 Spring 物理动画
 */
public class SpringAnimHelper {

    /**
     * 卡片焦点放大(TV 端)
     * 放大到 1.1x,带弹性
     *
     * @param view 目标 View
     */
    public static void focusScaleUp(View view) {
        scale(view, 1.1f, AnimInterpolator.FOCUS_SPRING);
    }

    /**
     * 绑定焦点缩放动画(TV 端)
     * 获得焦点时放大到 1.1x,失去焦点时回缩到 1.0x
     *
     * @param view 目标 View
     */
    public static void bindFocusScale(View view) {
        if (view == null) return;
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scale(v, 1.1f, AnimInterpolator.FOCUS_SPRING);
            } else {
                scale(v, 1.0f, AnimInterpolator.FOCUS_SPRING);
            }
        });
    }

    /**
     * 卡片焦点缩小(TV 端)
     * 缩小回 1.0x,带弹性
     *
     * @param view 目标 View
     */
    public static void focusScaleDown(View view) {
        scale(view, 1.0f, AnimInterpolator.FOCUS_SPRING);
    }

    /**
     * 触摸按下(手机端)
     * 缩小到 0.95x,无弹性
     *
     * @param view 目标 View
     */
    public static void touchPress(View view) {
        scale(view, 0.95f, AnimInterpolator.TOUCH_PRESS_SPRING);
    }

    /**
     * 触摸抬起(手机端)
     * 回弹到 1.0x,带弹性
     *
     * @param view 目标 View
     */
    public static void touchRelease(View view) {
        scale(view, 1.0f, AnimInterpolator.TOUCH_RELEASE_SPRING);
    }

    /**
     * 菜单弹出
     * 从 0 放大到 1,带弹性
     *
     * @param view 目标 View
     */
    public static void popUp(View view) {
        view.setScaleX(0f);
        view.setScaleY(0f);
        view.setVisibility(View.VISIBLE);
        scale(view, 1.0f, AnimInterpolator.POPUP_SPRING);
    }

    /**
     * 通用缩放动画
     *
     * @param view   目标 View
     * @param scale  目标缩放值
     * @param spring SpringForce 配置
     */
    public static void scale(View view, float scale, SpringForce spring) {
        if (view == null || spring == null) return;
        try {
            SpringAnimation scaleX = new SpringAnimation(view, SpringAnimation.SCALE_X, scale);
            scaleX.setSpring(spring);
            scaleX.start();

            SpringAnimation scaleY = new SpringAnimation(view, SpringAnimation.SCALE_Y, scale);
            scaleY.setSpring(spring);
            scaleY.start();
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    /**
     * 通用位移动画
     *
     * @param view     目标 View
     * @param offsetX  X 轴位移
     * @param offsetY  Y 轴位移
     * @param stiffness 刚度
     */
    public static void translate(View view, float offsetX, float offsetY, float stiffness) {
        if (view == null) return;
        try {
            SpringForce spring = new SpringForce()
                    .setStiffness(stiffness)
                    .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);

            SpringAnimation animX = new SpringAnimation(view, SpringAnimation.TRANSLATION_X, offsetX);
            animX.setSpring(spring);
            animX.start();

            SpringAnimation animY = new SpringAnimation(view, SpringAnimation.TRANSLATION_Y, offsetY);
            animY.setSpring(spring);
            animY.start();
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }
}
