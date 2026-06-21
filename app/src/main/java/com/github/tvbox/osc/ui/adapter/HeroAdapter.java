package com.github.tvbox.osc.ui.adapter;

import android.text.TextUtils;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.util.ImgUtil;
import com.github.tvbox.osc.util.SpringAnimHelper;

import java.util.ArrayList;

/**
 * TVBOX-NEXT: Hero 大卡片区适配器
 * 参考 Netflix 2025 设计,展示推荐内容的背景图 + 标题 + 信息标签 + 操作按钮
 */
public class HeroAdapter extends BaseQuickAdapter<Movie.Video, BaseViewHolder> {

    private OnHeroActionListener listener;

    public interface OnHeroActionListener {
        void onPlay(Movie.Video item);

        void onFavorite(Movie.Video item);

        void onDetail(Movie.Video item);
    }

    public HeroAdapter() {
        super(R.layout.item_hero_card, new ArrayList<>());
    }

    public void setOnHeroActionListener(OnHeroActionListener listener) {
        this.listener = listener;
    }

    @Override
    protected void convert(final BaseViewHolder helper, final Movie.Video item) {
        // 背景图:优先使用 pic,无则使用占位图
        if (!TextUtils.isEmpty(item.pic)) {
            ImgUtil.load(item.pic, helper.getView(R.id.ivHeroBg), 0);
        } else {
            helper.setImageResource(R.id.ivHeroBg, R.drawable.img_loading_placeholder);
        }

        // 标题
        helper.setText(R.id.tvHeroTitle, item.name);

        // 信息标签:年份
        String year = item.year > 0 ? String.valueOf(item.year) : "";
        helper.setText(R.id.tvHeroYear, year)
                .setVisible(R.id.tvHeroYear, !TextUtils.isEmpty(year));

        // 信息标签:类型
        String type = item.tag;
        if (TextUtils.isEmpty(type)) {
            type = item.area;
        }
        helper.setText(R.id.tvHeroType, type)
                .setVisible(R.id.tvHeroType, !TextUtils.isEmpty(type));

        // 信息标签:评分/备注
        String rate = item.note;
        helper.setText(R.id.tvHeroRate, rate)
                .setVisible(R.id.tvHeroRate, !TextUtils.isEmpty(rate));

        // 简介
        String desc = item.des;
        helper.setText(R.id.tvHeroDesc, desc == null ? "" : desc);

        // 按钮点击事件
        View btnPlay = helper.getView(R.id.btnHeroPlay);
        View btnFav = helper.getView(R.id.btnHeroFav);
        View btnDetail = helper.getView(R.id.btnHeroDetail);

        btnPlay.setOnClickListener(v -> {
            if (listener != null) listener.onPlay(item);
        });
        btnFav.setOnClickListener(v -> {
            if (listener != null) listener.onFavorite(item);
        });
        btnDetail.setOnClickListener(v -> {
            if (listener != null) listener.onDetail(item);
        });

        // Spring 动画:按钮焦点放大
        SpringAnimHelper.bindFocusScale(btnPlay);
        SpringAnimHelper.bindFocusScale(btnFav);
        SpringAnimHelper.bindFocusScale(btnDetail);
    }
}
