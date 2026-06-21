package com.github.tvbox.osc.ui.adapter;

import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.util.ImgUtil;
import com.github.tvbox.osc.util.anim.SpringAnimHelper;
import com.github.tvbox.osc.util.anim.TransitionHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * TVBOX-NEXT: 手机端竖版海报卡片适配器
 * 2:3 海报 + 标题 + 年份 + 评分
 */
public class MobileGridAdapter extends BaseQuickAdapter<Movie.Video, BaseViewHolder> {

    public MobileGridAdapter() {
        super(R.layout.item_mobile_grid, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, Movie.Video item) {
        // 海报图
        if (!TextUtils.isEmpty(item.pic)) {
            ImgUtil.load(item.pic, helper.getView(R.id.ivPoster), 8);
        } else {
            helper.setImageResource(R.id.ivPoster, R.drawable.img_loading_placeholder);
        }

        // 设置共享元素 transitionName(用于一镜到底转场)
        TransitionHelper.setTransitionName(helper.getView(R.id.ivPoster), "poster_" + item.id);

        // 标题
        helper.setText(R.id.tvTitle, item.name);

        // 年份
        String year = item.year > 0 ? String.valueOf(item.year) : "";
        helper.setText(R.id.tvYear, year)
                .setVisible(R.id.tvYear, !TextUtils.isEmpty(year));

        // 评分
        String rate = item.note;
        helper.setText(R.id.tvRate, rate)
                .setVisible(R.id.tvRate, !TextUtils.isEmpty(rate));

        // 触摸动画:按下缩小,抬起回弹
        View itemView = helper.itemView;
        itemView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    SpringAnimHelper.touchPress(v);
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    SpringAnimHelper.touchRelease(v);
                    break;
            }
            return false;
        });
    }

    // TVBOX-NEXT 优化#6: 使用 DiffUtil 增量更新,避免全量刷新导致的闪烁
    private static final DiffUtil.ItemCallback<Movie.Video> DIFF_CALLBACK = new DiffUtil.ItemCallback<Movie.Video>() {
        @Override
        public boolean areItemsTheSame(Movie.Video oldItem, Movie.Video newItem) {
            return oldItem.id != null && oldItem.id.equals(newItem.id)
                    && oldItem.sourceKey != null && oldItem.sourceKey.equals(newItem.sourceKey);
        }

        @Override
        public boolean areContentsTheSame(Movie.Video oldItem, Movie.Video newItem) {
            return TextUtils.equals(oldItem.name, newItem.name)
                    && TextUtils.equals(oldItem.pic, newItem.pic)
                    && TextUtils.equals(oldItem.note, newItem.note)
                    && oldItem.year == newItem.year;
        }
    };

    /**
     * 使用 DiffUtil 增量更新数据
     * 首次加载或数据量较大时回退到 setNewData
     */
    public void setDiffNewData(@Nullable List<Movie.Video> newData) {
        if (newData == null) {
            setNewData(null);
            return;
        }
        List<Movie.Video> oldData = getData();
        if (oldData == null || oldData.isEmpty() || newData.size() == 0) {
            setNewData(newData);
            return;
        }
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldData.size();
            }

            @Override
            public int getNewListSize() {
                return newData.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return DIFF_CALLBACK.areItemsTheSame(oldData.get(oldItemPosition), newData.get(newItemPosition));
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return DIFF_CALLBACK.areContentsTheSame(oldData.get(oldItemPosition), newData.get(newItemPosition));
            }
        });
        setNewData(newData);
        result.dispatchUpdatesTo(this);
    }
}
