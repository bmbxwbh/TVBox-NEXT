package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseMobileActivity;
import com.github.tvbox.osc.bean.AbsXml;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.bean.VodSeriesGroup;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.adapter.SeriesAdapter;
import com.github.tvbox.osc.ui.adapter.SeriesFlagAdapter;
import com.github.tvbox.osc.ui.adapter.SeriesGroupAdapter;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.ImgUtil;
import com.github.tvbox.osc.util.anim.SpringAnimHelper;
import com.github.tvbox.osc.util.anim.TransitionHelper;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.lzy.okgo.OkGo;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

/**
 * TVBOX-NEXT: 手机端详情页 Activity
 * 全屏背景图 + 渐变遮罩 + 标题/信息/简介/选集
 * 复用原版 DetailActivity 的详情加载逻辑(getDetail/VodInfo/选集列表)
 */
public class MobileDetailActivity extends BaseMobileActivity {

    public static final String EXTRA_ID = "id";
    public static final String EXTRA_SOURCE_KEY = "sourceKey";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_TRANSITION_NAME = "transition_name";

    private ImageView ivDetailBg;
    private TextView tvTitle;
    private TextView tvYear;
    private TextView tvArea;
    private TextView tvRate;
    private TextView tvDesc;
    private TextView tvDirector;
    private TextView tvActor;
    private View btnFav;
    private RecyclerView rvFlagList;
    private RecyclerView rvSeriesGroup;
    private RecyclerView rvEpisodes;

    private SourceViewModel sourceViewModel;
    private Movie.Video mVideo;
    private VodInfo vodInfo;
    private SeriesFlagAdapter seriesFlagAdapter;
    private SeriesAdapter seriesAdapter;
    private SeriesGroupAdapter seriesGroupAdapter;
    private List<List<VodInfo.VodSeries>> uu;
    private int GroupCount;
    private int GroupIndex = 0;

    public String vodId;
    public String sourceKey;
    public String firstsourceKey;

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_mobile_detail;
    }

    @Override
    protected void init() {
        EventBus.getDefault().register(this);
        initView();
        initViewModel();
        initData();
    }

    private void initView() {
        ivDetailBg = findViewById(R.id.ivDetailBg);
        tvTitle = findViewById(R.id.tvTitle);
        tvYear = findViewById(R.id.tvYear);
        tvArea = findViewById(R.id.tvArea);
        tvRate = findViewById(R.id.tvRate);
        tvDesc = findViewById(R.id.tvDesc);
        tvDirector = findViewById(R.id.tvDirector);
        tvActor = findViewById(R.id.tvActor);
        rvFlagList = findViewById(R.id.rvFlagList);
        rvSeriesGroup = findViewById(R.id.rvSeriesGroup);
        rvEpisodes = findViewById(R.id.rvEpisodes);

        // 启用共享元素转场
        TransitionHelper.setupDetailTransition(this);

        // 设置背景图的 transitionName(与列表页共享)
        Bundle args = getIntent().getExtras();
        if (args != null) {
            String transitionName = args.getString(EXTRA_TRANSITION_NAME);
            if (transitionName != null) {
                TransitionHelper.setTransitionName(ivDetailBg, transitionName);
            }
            String title = args.getString(EXTRA_TITLE);
            if (!TextUtils.isEmpty(title)) {
                tvTitle.setText(title);
            }
        }

        // 返回按钮
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        // 按钮触摸动画
        bindTouchAnimation(R.id.btnPlay);
        bindTouchAnimation(R.id.btnFav);
        bindTouchAnimation(R.id.btnDownload);

        // 播放按钮(复用原版 DetailActivity 的 jumpToPlay 逻辑)
        findViewById(R.id.btnPlay).setOnClickListener(v -> {
            FastClickCheckUtil.check(v);
            jumpToPlay();
        });

        // 收藏按钮(复用原版 DetailActivity 的收藏逻辑)
        btnFav = findViewById(R.id.btnFav);
        btnFav.setOnClickListener(v -> {
            if (vodInfo == null) return;
            boolean isVodCollect = RoomDataManger.isVodCollect(sourceKey, vodId);
            if (!isVodCollect) {
                RoomDataManger.insertVodCollect(sourceKey, vodInfo);
                Toast.makeText(this, getString(R.string.det_fav_add), Toast.LENGTH_SHORT).show();
            } else {
                RoomDataManger.deleteVodCollect(sourceKey, vodInfo);
                Toast.makeText(this, getString(R.string.det_fav_del), Toast.LENGTH_SHORT).show();
            }
        });

        // 选集列表(复用原版 DetailActivity 的列表设置)
        rvEpisodes.setLayoutManager(new GridLayoutManager(this, 4));
        seriesAdapter = new SeriesAdapter();
        rvEpisodes.setAdapter(seriesAdapter);
        seriesAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                if (vodInfo != null && vodInfo.seriesMap.get(vodInfo.playFlag).size() > 0) {
                    boolean reload = false;
                    if (vodInfo.getplayIndex() != GroupIndex * GroupCount + position) {
                        for (int i = 0; i < seriesAdapter.getData().size(); i++) {
                            VodInfo.VodSeries Series = seriesAdapter.getData().get(i);
                            Series.selected = false;
                            seriesAdapter.notifyItemChanged(i);
                        }
                        seriesAdapter.getData().get(position).selected = true;
                        seriesAdapter.notifyItemChanged(position);
                        vodInfo.playIndex = position;
                        vodInfo.playGroup = GroupIndex;
                        reload = true;
                    }
                    if (reload) jumpToPlay();
                }
            }
        });

        // 线路标签列表(复用原版 DetailActivity 的标签设置)
        rvFlagList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        seriesFlagAdapter = new SeriesFlagAdapter();
        rvFlagList.setAdapter(seriesFlagAdapter);
        seriesFlagAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                if (position == -1) return;
                if (vodInfo == null || position >= vodInfo.seriesFlags.size()) return;
                String newFlag = seriesFlagAdapter.getData().get(position).name;
                if (newFlag == null) return;
                if (vodInfo.playFlag == null || !vodInfo.playFlag.equals(newFlag)) {
                    for (int i = 0; i < vodInfo.seriesFlags.size(); i++) {
                        VodInfo.VodSeriesFlag flag = vodInfo.seriesFlags.get(i);
                        if (flag.name.equals(vodInfo.playFlag)) {
                            flag.selected = false;
                            seriesFlagAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                    VodInfo.VodSeriesFlag flag = vodInfo.seriesFlags.get(position);
                    flag.selected = true;
                    if (vodInfo.playFlag != null && vodInfo.seriesMap.containsKey(vodInfo.playFlag)) {
                        List<VodInfo.VodSeries> oldSeries = vodInfo.seriesMap.get(vodInfo.playFlag);
                        if (oldSeries != null && oldSeries.size() > vodInfo.getplayIndex()) {
                            oldSeries.get(vodInfo.playIndex).selected = false;
                        }
                    }
                    vodInfo.playFlag = newFlag;
                    seriesFlagAdapter.notifyItemChanged(position);
                    refreshList();
                }
            }
        });

        // 选集分组列表(复用原版 DetailActivity 的分组设置)
        rvSeriesGroup.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        seriesGroupAdapter = new SeriesGroupAdapter();
        rvSeriesGroup.setAdapter(seriesGroupAdapter);
        seriesGroupAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                if (GroupIndex != position) {
                    seriesGroupAdapter.getData().get(GroupIndex).selected = false;
                    seriesGroupAdapter.notifyItemChanged(GroupIndex);
                    seriesGroupAdapter.getData().get(position).selected = true;
                    seriesGroupAdapter.notifyItemChanged(position);
                    GroupIndex = position;
                    seriesAdapter.setNewData(uu.get(position));
                }
            }
        });

        setLoadSir(rvEpisodes);
    }

    // TVBOX-NEXT 优化#10: 重写 setLoadSir,提供错误重试回调
    @Override
    protected void setLoadSir(View view) {
        if (mLoadService == null) {
            mLoadService = com.kingja.loadsir.core.LoadSir.getDefault().register(view, new com.kingja.loadsir.callback.Callback.OnReloadListener() {
                @Override
                public void onReload(View v) {
                    // 点击空状态/加载状态时重试
                    if (vodId != null && sourceKey != null) {
                        showLoading();
                        sourceViewModel.getDetail(sourceKey, vodId);
                    }
                }
            });
        }
    }

    /**
     * 复用原版 DetailActivity.initViewModel()
     * 观察 detailResult LiveData
     */
    private void initViewModel() {
        sourceViewModel = new ViewModelProvider(this).get(SourceViewModel.class);
        sourceViewModel.detailResult.observe(this, new Observer<AbsXml>() {
            @Override
            public void onChanged(AbsXml absXml) {
                if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && absXml.movie.videoList.size() > 0) {
                    showSuccess();
                    if (!TextUtils.isEmpty(absXml.msg) && !absXml.msg.equals("数据列表")) {
                        Toast.makeText(MobileDetailActivity.this, absXml.msg, Toast.LENGTH_SHORT).show();
                        showEmpty();
                        return;
                    }
                    mVideo = absXml.movie.videoList.get(0);
                    mVideo.id = vodId;
                    if (TextUtils.isEmpty(mVideo.name)) mVideo.name = "片名";
                    vodInfo = new VodInfo();
                    vodInfo.setVideo(mVideo);
                    vodInfo.sourceKey = mVideo.sourceKey;
                    sourceKey = mVideo.sourceKey;

                    // 填充详情 UI
                    tvTitle.setText(mVideo.name);
                    tvYear.setText(mVideo.year == 0 ? "" : String.valueOf(mVideo.year));
                    tvArea.setText(mVideo.area != null ? mVideo.area : "");
                    tvRate.setText(mVideo.note != null ? mVideo.note : "");
                    tvDesc.setText(removeHtmlTag(mVideo.des));
                    tvDirector.setText("Director: " + (mVideo.director != null ? mVideo.director : ""));
                    tvActor.setText("Cast: " + (mVideo.actor != null ? mVideo.actor : ""));
                    if (!TextUtils.isEmpty(mVideo.pic)) {
                        ImgUtil.load(mVideo.pic, ivDetailBg, 0);
                    } else {
                        ivDetailBg.setImageResource(R.drawable.img_loading_placeholder);
                    }

                    // 启动共享元素转场
                    TransitionHelper.startPostponedTransition(MobileDetailActivity.this);

                    // 选集列表(复用原版 DetailActivity 的选集逻辑)
                    if (vodInfo.seriesMap != null && vodInfo.seriesMap.size() > 0) {
                        rvFlagList.setVisibility(View.VISIBLE);
                        rvEpisodes.setVisibility(View.VISIBLE);

                        // 读取历史记录(复用原版 DetailActivity 的历史记录读取)
                        VodInfo vodInfoRecord = RoomDataManger.getVodInfo(sourceKey, vodId);
                        if (vodInfoRecord != null) {
                            vodInfo.playIndex = Math.max(vodInfoRecord.playIndex, 0);
                            vodInfo.playGroup = Math.max(vodInfoRecord.playGroup, 0);
                            GroupIndex = vodInfo.playGroup;
                            vodInfo.playGroupCount = Math.max(vodInfoRecord.playGroupCount, 0);
                            GroupCount = vodInfo.playGroupCount;
                            vodInfo.playFlag = vodInfoRecord.playFlag;
                            vodInfo.playerCfg = vodInfoRecord.playerCfg;
                            vodInfo.reverseSort = vodInfoRecord.reverseSort;
                        } else {
                            vodInfo.playIndex = 0;
                            vodInfo.playGroup = 0;
                            vodInfo.playFlag = null;
                            vodInfo.playerCfg = "";
                            vodInfo.reverseSort = false;
                        }

                        if (vodInfo.reverseSort) {
                            vodInfo.reverse();
                        }

                        if (vodInfo.playFlag == null || !vodInfo.seriesMap.containsKey(vodInfo.playFlag))
                            vodInfo.playFlag = (String) vodInfo.seriesMap.keySet().toArray()[0];

                        int flagScrollTo = 0;
                        for (int j = 0; j < vodInfo.seriesFlags.size(); j++) {
                            VodInfo.VodSeriesFlag flag = vodInfo.seriesFlags.get(j);
                            if (flag.name.equals(vodInfo.playFlag)) {
                                flagScrollTo = j;
                                flag.selected = true;
                            } else
                                flag.selected = false;
                        }

                        seriesFlagAdapter.setNewData(vodInfo.seriesFlags);
                        rvFlagList.scrollToPosition(flagScrollTo);

                        refreshList();
                    } else {
                        rvFlagList.setVisibility(View.GONE);
                        rvEpisodes.setVisibility(View.GONE);
                    }
                } else {
                    // TVBOX-NEXT 优化#10: 详情加载失败,显示空状态和错误提示
                    showEmpty();
                    Toast.makeText(MobileDetailActivity.this, "详情加载失败,点击重试", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 复用原版 DetailActivity.initData()
     */
    private void initData() {
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            loadDetail(bundle.getString("id", null), bundle.getString("sourceKey", ""));
        }
    }

    /**
     * 复用原版 DetailActivity.loadDetail()
     */
    private void loadDetail(String vid, String key) {
        if (vid != null) {
            vodId = vid;
            sourceKey = key;
            firstsourceKey = key;
            showLoading();
            sourceViewModel.getDetail(sourceKey, vodId);

            boolean isVodCollect = RoomDataManger.isVodCollect(sourceKey, vodId);
            // 收藏按钮状态(复用原版逻辑)
        }
    }

    /**
     * 复用原版 DetailActivity.refreshList()
     * 刷新选集列表
     */
    private void refreshList() {
        try {
            if (vodInfo == null || vodInfo.seriesMap == null || vodInfo.playFlag == null) {
                return;
            }
            List<VodInfo.VodSeries> seriesList = vodInfo.seriesMap.get(vodInfo.playFlag);
            if (seriesList == null || seriesList.isEmpty()) {
                return;
            }
            if (seriesList.size() <= vodInfo.getplayIndex()) {
                vodInfo.playIndex = 0;
            }
            int playIndex = this.vodInfo.getplayIndex();
            if (seriesList.size() > playIndex) {
                seriesList.get(playIndex).selected = true;
            } else {
                vodInfo.playGroup = 0;
            }

            List<VodSeriesGroup> seriesGroupList = getSeriesGroupList();
            if (!seriesGroupList.isEmpty()) {
                if (vodInfo.playGroup < seriesGroupList.size()) {
                    seriesGroupList.get(vodInfo.playGroup).selected = true;
                }
                seriesGroupAdapter.setNewData(seriesGroupList);
                rvSeriesGroup.setVisibility(View.VISIBLE);
            } else {
                rvSeriesGroup.setVisibility(View.GONE);
            }
            if (vodInfo.playGroup < uu.size()) {
                seriesAdapter.setNewData(uu.get(vodInfo.playGroup));
            }

            rvEpisodes.postDelayed(new Runnable() {
                @Override
                public void run() {
                    rvEpisodes.scrollToPosition(vodInfo.playGroup);
                    rvSeriesGroup.scrollToPosition(vodInfo.playGroup);
                }
            }, 100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 复用原版 DetailActivity.getSeriesGroupList()
     * 动态计算选集分组
     */
    private List<VodSeriesGroup> getSeriesGroupList() {
        List<VodSeriesGroup> arrayList = new ArrayList<>();
        if (uu != null) {
            uu.clear();
        } else {
            uu = new ArrayList<>();
        }
        try {
            if (vodInfo == null || vodInfo.seriesMap == null || vodInfo.playFlag == null) {
                return arrayList;
            }
            List<VodInfo.VodSeries> vodSeries = vodInfo.seriesMap.get(vodInfo.playFlag);
            if (vodSeries == null || vodSeries.isEmpty()) {
                return arrayList;
            }
            int size = vodSeries.size();
            GroupCount = size > 2500.0d ? 300 : size > 1500.0d ? 200 : size > 1000.0d ? 150 : size > 500.0d ? 100 : size > 300.0d ? 50 : size > 100.0d ? 30 : 20;
            vodInfo.playGroupCount = GroupCount;
            GroupIndex = (int) Math.floor(vodInfo.getplayIndex() / (GroupCount + 0.0f));
            if (GroupIndex < 0) {
                GroupIndex = 0;
            }
            int Groups = (int) Math.ceil(size / (GroupCount + 0.0f));
            for (int i = 0; i < Groups; i++) {
                int s = (i * GroupCount) + 1;
                int e = (i + 1) * GroupCount;
                int name_s = s;
                int name_e = e;
                if (vodInfo.reverseSort) {
                    name_s = size - i * GroupCount;
                    name_e = size - (i + 1) * GroupCount;
                }
                List<VodInfo.VodSeries> info = new ArrayList<>();
                if (e < size) {
                    for (int j = s - 1; j < e; j++) {
                        info.add(vodSeries.get(j));
                    }
                    arrayList.add(new VodSeriesGroup(name_s + "-" + name_e));
                } else {
                    for (int j = s - 1; j < size; j++) {
                        info.add(vodSeries.get(j));
                    }
                    if (vodInfo.reverseSort) {
                        arrayList.add(new VodSeriesGroup(name_s + "-" + 1));
                    } else {
                        arrayList.add(new VodSeriesGroup(name_s + "-" + size));
                    }
                }
                uu.add(info);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arrayList;
    }

    /**
     * 复用原版 DetailActivity.jumpToPlay()
     * 跳转播放(使用原版 PlayActivity)
     */
    private void jumpToPlay() {
        if (vodInfo == null || vodInfo.playFlag == null) return;
        List<VodInfo.VodSeries> series = vodInfo.seriesMap.get(vodInfo.playFlag);
        if (series == null || series.isEmpty()) return;
        if (vodInfo.getplayIndex() < series.size()) {
            // 保存历史
            insertVod(firstsourceKey, vodInfo);
            Bundle bundle = new Bundle();
            bundle.putString("sourceKey", sourceKey);
            bundle.putSerializable("VodInfo", vodInfo);
            jumpActivity(PlayActivity.class, bundle);
        }
    }

    /**
     * 复用原版 DetailActivity.insertVod()
     * 保存播放历史
     */
    private void insertVod(String sourceKey, VodInfo vodInfo) {
        try {
            if (vodInfo.playFlag != null && vodInfo.seriesMap != null) {
                List<VodInfo.VodSeries> series = vodInfo.seriesMap.get(vodInfo.playFlag);
                if (series != null && vodInfo.getplayIndex() < series.size()) {
                    vodInfo.playNote = series.get(vodInfo.getplayIndex()).name;
                }
            }
        } catch (Throwable th) {
            vodInfo.playNote = "";
        }
        RoomDataManger.insertVodRecord(sourceKey, vodInfo);
        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_HISTORY_REFRESH));
    }

    /**
     * 复用原版 DetailActivity.removeHtmlTag()
     */
    private String removeHtmlTag(String info) {
        if (info == null)
            return "";
        return info.replaceAll("\\<.*?\\>", "").replaceAll("\\s", "");
    }

    /**
     * 复用原版 DetailActivity.refresh()
     * RefreshEvent 回调处理播放进度更新
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_REFRESH) {
            if (event.obj != null && event.obj instanceof Integer) {
                int index = (int) event.obj;
                if (GroupCount <= 0) return;
                int mGroupIndex = (int) Math.floor(index / (GroupCount + 0.0f));
                boolean changeGroup = false;
                if (mGroupIndex != GroupIndex) {
                    changeGroup = true;
                    if (vodInfo.playIndex >= 0 && vodInfo.playIndex < seriesAdapter.getData().size()) {
                        seriesAdapter.getData().get(vodInfo.playIndex).selected = false;
                        seriesAdapter.notifyItemChanged(vodInfo.playIndex);
                    }
                    if (GroupIndex >= 0 && GroupIndex < seriesGroupAdapter.getData().size()) {
                        seriesGroupAdapter.getData().get(GroupIndex).selected = false;
                        seriesGroupAdapter.notifyItemChanged(GroupIndex);
                    }
                    if (mGroupIndex >= 0 && mGroupIndex < seriesGroupAdapter.getData().size()) {
                        seriesGroupAdapter.getData().get(mGroupIndex).selected = true;
                        seriesGroupAdapter.notifyItemChanged(mGroupIndex);
                    }
                    if (mGroupIndex >= 0 && mGroupIndex < uu.size()) {
                        seriesAdapter.setNewData(uu.get(mGroupIndex));
                    }
                    GroupIndex = mGroupIndex;
                    rvSeriesGroup.scrollToPosition(mGroupIndex);
                }
                if (index != vodInfo.getplayIndex()) {
                    if (!changeGroup) {
                        if (vodInfo.playIndex >= 0 && vodInfo.playIndex < seriesAdapter.getData().size()) {
                            seriesAdapter.getData().get(vodInfo.playIndex).selected = false;
                            seriesAdapter.notifyItemChanged(vodInfo.playIndex);
                        }
                    }
                    vodInfo.playIndex = index % GroupCount;
                    vodInfo.playGroup = index / GroupCount;
                    if (vodInfo.playIndex >= 0 && vodInfo.playIndex < seriesAdapter.getData().size()) {
                        seriesAdapter.getData().get(vodInfo.playIndex).selected = true;
                        seriesAdapter.notifyItemChanged(vodInfo.playIndex);
                    }
                    rvEpisodes.scrollToPosition(vodInfo.playIndex);
                    insertVod(firstsourceKey, vodInfo);
                }
            } else if (event.obj != null && event.obj instanceof org.json.JSONObject) {
                vodInfo.playerCfg = ((org.json.JSONObject) event.obj).toString();
                insertVod(firstsourceKey, vodInfo);
            }
        }
    }

    private void bindTouchAnimation(int viewId) {
        View view = findViewById(viewId);
        view.setOnTouchListener((v, event) -> {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        OkGo.getInstance().cancelTag("detail");
        EventBus.getDefault().unregister(this);
    }
}
