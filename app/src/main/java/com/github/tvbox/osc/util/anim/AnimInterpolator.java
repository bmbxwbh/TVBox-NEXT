package com.github.tvbox.osc.util.anim;

import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import androidx.dynamicanimation.animation.SpringForce;

/**
 * 标准插值器(TV/手机共用)
 * 参考 Netflix 动画设计规范
 */
public class AnimInterpolator {

    /**
     * 元素进入:快启动后减速
     */
    public static final Interpolator ENTER = new DecelerateInterpolator(1.5f);

    /**
     * 元素退出:慢启动后加速
     */
    public static final Interpolator EXIT = new AccelerateInterpolator(1.5f);

    /**
     * 页面切换:两端减速
     */
    public static final Interpolator TRANSITION = new AccelerateDecelerateInterpolator();

    /**
     * 焦点/触摸动画:弹簧物理(TV 焦点放大)
     */
    public static final SpringForce FOCUS_SPRING = new SpringForce()
            .setStiffness(SpringForce.STIFFNESS_LOW)
            .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);

    /**
     * 触摸反馈:按下缩小(无弹性)
     */
    public static final SpringForce TOUCH_PRESS_SPRING = new SpringForce()
            .setStiffness(SpringForce.STIFFNESS_MEDIUM)
            .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY);

    /**
     * 触摸反馈:抬起回弹(有弹性)
     */
    public static final SpringForce TOUCH_RELEASE_SPRING = new SpringForce()
            .setStiffness(SpringForce.STIFFNESS_LOW)
            .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);

    /**
     * 菜单弹出:中等弹性
     */
    public static final SpringForce POPUP_SPRING = new SpringForce()
            .setStiffness(SpringForce.STIFFNESS_MEDIUM)
            .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);

    /**
     * 动画时长:微交互(按钮点击)
     */
    public static final int DURATION_MICRO = 150;

    /**
     * 动画时长:焦点切换
     */
    public static final int DURATION_FOCUS = 250;

    /**
     * 动画时长:卡片展开
     */
    public static final int DURATION_CARD = 350;

    /**
     * 动画时长:页面切换
     */
    public static final int DURATION_TRANSITION = 450;

    /**
     * 动画时长:共享元素转场
     */
    public static final int DURATION_SHARED_ELEMENT = 450;
}
