package com.github.tvbox.osc.ui.adapter;

import android.text.TextUtils;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.util.ImgUtil;
import com.github.tvbox.osc.util.anim.SpringAnimHelper;

import java.util.ArrayList;

/**
 * TVBOX-NEXT: 大矩形卡片适配器
 * 参考 Netflix 2025 设计,16:9 横向卡片,信息集中在底部
 */
public class BigCardAdapter extends BaseQuickAdapter<Movie.Video, BaseViewHolder> {

    public BigCardAdapter() {
        super(R.layout.item_big_card, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, Movie.Video item) {
        // 背景图
        if (!TextUtils.isEmpty(item.pic)) {
            ImgUtil.load(item.pic, helper.getView(R.id.ivCardThumb), 8);
        } else {
            helper.setImageResource(R.id.ivCardThumb, R.drawable.img_loading_placeholder);
        }

        // 标题
        helper.setText(R.id.tvCardTitle, item.name);

        // 年份
        String year = item.year > 0 ? String.valueOf(item.year) : "";
        helper.setText(R.id.tvCardYear, year)
                .setVisible(R.id.tvCardYear, !TextUtils.isEmpty(year));

        // 类型
        String type = item.tag;
        if (TextUtils.isEmpty(type)) {
            type = item.area;
        }
        helper.setText(R.id.tvCardType, type)
                .setVisible(R.id.tvCardType, !TextUtils.isEmpty(type));

        // 评分/备注
        String rate = item.note;
        helper.setText(R.id.tvCardRate, rate)
                .setVisible(R.id.tvCardRate, !TextUtils.isEmpty(rate));

        // Spring 焦点动画:获得焦点放大 1.1x
        View itemView = helper.itemView;
        SpringAnimHelper.bindFocusScale(itemView);
    }
}
