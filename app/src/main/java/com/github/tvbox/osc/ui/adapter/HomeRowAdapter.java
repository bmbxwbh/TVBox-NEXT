package com.github.tvbox.osc.ui.adapter;

import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.util.ImgUtil;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

/**
 * TVBOX-NEXT: 首页横向滚动行适配器
 * 参考 Netflix 风格,首页由多个横向滚动行组成,每行一个分类
 */
public class HomeRowAdapter extends BaseQuickAdapter<HomeRowAdapter.RowData, BaseViewHolder> {

    private OnRowMoreListener moreListener;
    private OnRowItemClickListener itemListener;
    private final ImgUtil.Style style;

    /**
     * 单行数据
     */
    public static class RowData {
        public String title;
        public MovieSort.SortData sortData;
        public List<Movie.Video> videos;

        public RowData(String title, MovieSort.SortData sortData, List<Movie.Video> videos) {
            this.title = title;
            this.sortData = sortData;
            this.videos = videos != null ? videos : new ArrayList<>();
        }
    }

    public interface OnRowMoreListener {
        void onMore(MovieSort.SortData sortData);
    }

    public interface OnRowItemClickListener {
        void onItemClick(Movie.Video video);
    }

    public HomeRowAdapter(ImgUtil.Style style) {
        super(R.layout.item_home_row, new ArrayList<>());
        this.style = style;
    }

    public void setOnRowMoreListener(OnRowMoreListener listener) {
        this.moreListener = listener;
    }

    public void setOnRowItemClickListener(OnRowItemClickListener listener) {
        this.itemListener = listener;
    }

    @Override
    protected void convert(BaseViewHolder helper, RowData item) {
        // 行标题
        helper.setText(R.id.tvRowTitle, item.title);

        // "查看全部"点击事件
        View tvMore = helper.getView(R.id.tvRowMore);
        tvMore.setOnClickListener(v -> {
            if (moreListener != null && item.sortData != null) {
                moreListener.onMore(item.sortData);
            }
        });

        // 横向滚动卡片列表
        TvRecyclerView rowGridView = helper.getView(R.id.mRowGridView);
        rowGridView.setLayoutManager(new V7LinearLayoutManager(mContext, 0, false));
        HomeHotVodAdapter rowAdapter = new HomeHotVodAdapter(style, "");
        rowGridView.setAdapter(rowAdapter);
        rowAdapter.setNewData(item.videos);

        rowAdapter.setOnItemClickListener((adapter, view, position) -> {
            if (itemListener != null) {
                Movie.Video video = (Movie.Video) adapter.getData().get(position);
                itemListener.onItemClick(video);
            }
        });
    }
}
