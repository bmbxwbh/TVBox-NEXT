package com.github.tvbox.osc.util;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;

/**
 * 设备类型识别工具类
 * 综合判断当前设备是 TV、手机还是平板
 */
public class DeviceTypeDetector {

    /**
     * 综合判断是否为 TV 设备
     * 优先级:UI Mode > System Feature > Input Device > Build Info
     */
    public static boolean isTelevision(Context context) {
        // 方法1:检查 UI Mode(最可靠)
        int uiMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_TYPE_MASK;
        if (uiMode == Configuration.UI_MODE_TYPE_TELEVISION) {
            return true;
        }

        // 方法2:检查 Leanback 系统特性
        PackageManager pm = context.getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
                pm.hasSystemFeature("android.software.leanback")) {
            return true;
        }

        // 方法3:检查输入设备(无触摸屏 + 有 D-pad)
        if (!pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)) {
            return true;
        }

        // 方法4:Build 信息辅助判断(厂商定制)
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        String model = Build.MODEL.toLowerCase();
        if (model.contains("tv") || model.contains("box")) {
            return true;
        }
        if (manufacturer.contains("xiaomi") && model.contains("tv")) {
            return true;
        }
        if ((manufacturer.contains("hisense") || manufacturer.contains("skyworth"))
                && !pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            return true;
        }

        return false;
    }

    /**
     * 是否为手机设备
     */
    public static boolean isMobile(Context context) {
        return !isTelevision(context);
    }

    /**
     * 是否为平板(大屏非 TV)
     */
    public static boolean isTablet(Context context) {
        boolean isTv = isTelevision(context);
        boolean isLargeScreen = (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
        return isLargeScreen && !isTv;
    }
}
