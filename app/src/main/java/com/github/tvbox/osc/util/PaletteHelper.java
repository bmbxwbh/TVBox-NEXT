package com.github.tvbox.osc.util;

import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.palette.graphics.Palette;

/**
 * TVBOX-NEXT: 调色板辅助类
 * 从图片提取主色,实现 Netflix 风格的动态背景色
 */
public class PaletteHelper {

    /**
     * 从 Bitmap 提取主色
     *
     * @param bitmap  目标图片
     * @param listener 回调
     */
    public static void extractColor(Bitmap bitmap, OnColorExtractedListener listener) {
        if (bitmap == null) {
            listener.onExtracted(getDefaultBgColor(), getDefaultTextColor());
            return;
        }
        Palette.from(bitmap).generate(palette -> {
            if (palette == null) {
                listener.onExtracted(getDefaultBgColor(), getDefaultTextColor());
                return;
            }

            // 获取深色背景
            int bgColor = palette.getDarkVibrantColor(getDefaultBgColor());
            if (bgColor == getDefaultBgColor()) {
                bgColor = palette.getDarkMutedColor(getDefaultBgColor());
            }
            if (bgColor == getDefaultBgColor()) {
                bgColor = palette.getDominantColor(getDefaultBgColor());
            }

            // 加深背景色(Netflix 风格深色背景)
            bgColor = darkenColor(bgColor, 0.7f);

            // 获取文字色
            int textColor = getContrastColor(bgColor);

            listener.onExtracted(bgColor, textColor);
        });
    }

    /**
     * 获取默认背景色(TVBOX-NEXT 深色)
     */
    public static int getDefaultBgColor() {
        return Color.parseColor("#141414");
    }

    /**
     * 获取默认文字色
     */
    public static int getDefaultTextColor() {
        return Color.WHITE;
    }

    /**
     * 加深颜色
     *
     * @param color  原始颜色
     * @param factor 加深因子(0-1,越小越暗)
     */
    public static int darkenColor(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= factor;  // 降低明度
        return Color.HSVToColor(hsv);
    }

    /**
     * 根据背景色获取对比文字色(黑或白)
     *
     * @param bgColor 背景色
     * @return 适合的文字色
     */
    public static int getContrastColor(int bgColor) {
        double luminance = (0.299 * Color.red(bgColor) +
                0.587 * Color.green(bgColor) +
                0.114 * Color.blue(bgColor)) / 255;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    /**
     * 回调接口
     */
    public interface OnColorExtractedListener {
        void onExtracted(int bgColor, int textColor);
    }
}
