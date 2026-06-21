package com.github.tvbox.osc.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.AbsSortXml;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.activity.FastSearchActivity;
import com.github.tvbox.osc.ui.activity.MobileSearchActivity;
import com.github.tvbox.osc.ui.activity.SettingActivity;
import com.github.tvbox.osc.ui.adapter.MobileGridAdapter;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.ImgUtil;
import com.github.tvbox.osc.util.anim.SpringAnimHelper;
import com.github.tvbox.osc.util.UA;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * TVBOX-NEXT: 手机端首页 Fragment
 * Hero Banner 区 + 横向滚动行
 * 复用原版 UserFragment 的首页推荐数据加载逻辑
 */
public class MobileHomeFragment extends Fragment {

    private ImageView ivHeroBg;
    private TextView tvHeroTitle;
    private TextView tvHeroYear;
    private TextView tvHeroType;
    private TextView tvHeroRate;
    private TextView tvHeroDesc;
    private LinearLayout rowsContainer;
    private RecyclerView rvHotList;
    private MobileGridAdapter hotAdapter;
    private View emptyState;
    private TextView tvEmptyHint;

    // 站点推荐数据(由 MobileHomeActivity 通过 sortResult 设置)
    private static List<Movie.Video> homeSourceRec = null;

    public static void setHomeSourceRec(List<Movie.Video> rec) {
        homeSourceRec = rec;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mobile_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ivHeroBg = view.findViewById(R.id.ivHeroBg);
        tvHeroTitle = view.findViewById(R.id.tvHeroTitle);
        tvHeroYear = view.findViewById(R.id.tvHeroYear);
        tvHeroType = view.findViewById(R.id.tvHeroType);
        tvHeroRate = view.findViewById(R.id.tvHeroRate);
        tvHeroDesc = view.findViewById(R.id.tvHeroDesc);
        rowsContainer = view.findViewById(R.id.rowsContainer);
        emptyState = view.findViewById(R.id.emptyState);
        tvEmptyHint = view.findViewById(R.id.tvEmptyHint);

        // "去设置"按钮
        View btnGoSetting = view.findViewById(R.id.btnGoSetting);
        btnGoSetting.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SettingActivity.class);
            startActivity(intent);
        });

        // Hero 按钮触摸动画
        View btnPlay = view.findViewById(R.id.btnHeroPlay);
        View btnFav = view.findViewById(R.id.btnHeroFav);
        btnPlay.setOnTouchListener((v, event) -> {
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
        btnFav.setOnTouchListener((v, event) -> {
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

        // 初始化横向滚动行(复用原版 UserFragment 的列表逻辑)
        initHotList(view);

        // 观察站点推荐数据(复用原版 HomeActivity 的 sortResult 观察)
        if (getActivity() != null) {
            SourceViewModel sourceViewModel = new ViewModelProvider(getActivity()).get(SourceViewModel.class);
            sourceViewModel.sortResult.observe(getViewLifecycleOwner(), new Observer<AbsSortXml>() {
                @Override
                public void onChanged(AbsSortXml absXml) {
                    if (absXml != null && absXml.videoList != null && absXml.videoList.size() > 0) {
                        homeSourceRec = absXml.videoList;
                        // 站点推荐模式:刷新列表
                        if (Hawk.get(HawkConfig.HOME_REC, 0) == 1 && hotAdapter != null) {
                            hideEmptyState();
                            hotAdapter.setNewData(homeSourceRec);
                            updateHero(homeSourceRec);
                        }
                    } else if (Hawk.get(HawkConfig.HOME_REC, 0) == 1) {
                        showEmptyState("站点推荐数据为空,请检查源配置");
                    }
                }
            });
        }

        // 加载首页推荐数据(复用原版 UserFragment.initHomeHotVod)
        initHomeHotVod();
    }

    /**
     * 初始化横向滚动行
     * 复用原版 UserFragment 的列表和点击逻辑
     */
    private void initHotList(View view) {
        // 动态创建横向滚动行
        View rowView = getLayoutInflater().inflate(R.layout.item_mobile_row, rowsContainer, false);
        TextView tvRowTitle = rowView.findViewById(R.id.tvRowTitle);
        rvHotList = rowView.findViewById(R.id.rvRow);

        // 设置行标题
        String tvRate = "";
        if (Hawk.get(HawkConfig.HOME_REC, 0) == 0) {
            tvRate = "豆瓣热播";
        } else if (Hawk.get(HawkConfig.HOME_REC, 0) == 1) {
            tvRate = homeSourceRec != null ? "站点推荐" : "豆瓣热播";
        } else if (Hawk.get(HawkConfig.HOME_REC, 0) == 2) {
            tvRate = "历史记录";
        }
        tvRowTitle.setText(tvRate);

        // 横向滚动列表
        rvHotList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvHotList.setHasFixedSize(true);
        rvHotList.setNestedScrollingEnabled(false);

        hotAdapter = new MobileGridAdapter();
        rvHotList.setAdapter(hotAdapter);

        // 点击事件(复用原版 UserFragment 的点击逻辑)
        hotAdapter.setOnItemClickListener((adapter, view1, position) -> {
            if (ApiConfig.get().getSourceBeanList().isEmpty())
                return;
            Movie.Video vod = (Movie.Video) adapter.getItem(position);
            if (vod.id != null && !vod.id.isEmpty()) {
                Bundle bundle = new Bundle();
                bundle.putString("id", vod.id);
                bundle.putString("sourceKey", vod.sourceKey);
                if (vod.id.startsWith("msearch:")) {
                    bundle.putString("title", vod.name);
                    startActivityFromFragment(FastSearchActivity.class, bundle);
                } else {
                    startActivityFromFragment(DetailActivity.class, bundle);
                }
            } else {
                Intent newIntent;
                if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, false)) {
                    newIntent = new Intent(getContext(), FastSearchActivity.class);
                } else {
                    newIntent = new Intent(getContext(), MobileSearchActivity.class);
                }
                newIntent.putExtra("title", vod.name);
                newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(newIntent);
            }
        });

        rowsContainer.addView(rowView);
    }

    /**
     * 复用原版 UserFragment.initHomeHotVod()
     * 加载首页推荐数据
     */
    private void initHomeHotVod() {
        if (Hawk.get(HawkConfig.HOME_REC, 0) == 1) {
            // 站点推荐模式
            if (homeSourceRec != null) {
                hotAdapter.setNewData(homeSourceRec);
                updateHero(homeSourceRec);
            }
            return;
        } else if (Hawk.get(HawkConfig.HOME_REC, 0) == 2) {
            // 历史记录模式
            loadHistory();
            return;
        }
        // 豆瓣热播模式
        try {
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1;
            int day = cal.get(Calendar.DATE);
            String today = String.format("%d%d%d", year, month, day);
            String requestDay = Hawk.get("home_hot_day", "");
            if (requestDay.equals(today)) {
                String json = Hawk.get("home_hot", "");
                if (!json.isEmpty()) {
                    ArrayList<Movie.Video> hots = loadHots(json);
                    if (hots.isEmpty()) {
                        showEmptyState("豆瓣数据为空,请尝试切换为站点推荐");
                    } else {
                        hideEmptyState();
                        hotAdapter.setNewData(hots);
                        updateHero(hots);
                    }
                    return;
                }
            }
            String doubanHotURL = "https://movie.douban.com/j/new_search_subjects?sort=U&range=0,10&tags=&playable=1&start=0&year_range=" + year + "," + year;
            String userAgent = UA.random();
            OkGo.<String>get(doubanHotURL).headers("User-Agent", userAgent).execute(new AbsCallback<String>() {
                @Override
                public void onSuccess(Response<String> response) {
                    String netJson = response.body();
                    Hawk.put("home_hot_day", today);
                    Hawk.put("home_hot", netJson);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ArrayList<Movie.Video> hots = loadHots(netJson);
                                if (hots.isEmpty()) {
                                    showEmptyState("豆瓣数据为空,请在设置中切换为站点推荐");
                                } else {
                                    hideEmptyState();
                                    hotAdapter.setNewData(hots);
                                    updateHero(hots);
                                }
                            }
                        });
                    }
                }

                @Override
                public void onError(Response<String> response) {
                    super.onError(response);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> showEmptyState("网络请求失败,请检查网络或配置源地址"));
                    }
                }

                @Override
                public String convertResponse(okhttp3.Response response) throws Throwable {
                    return response.body().string();
                }
            });
        } catch (Throwable th) {
            th.printStackTrace();
            showEmptyState("数据加载失败,请稍后重试");
        }
    }

    /**
     * 复用原版 UserFragment.onFragmentResume()
     * 历史记录模式加载
     */
    private void loadHistory() {
        List<VodInfo> allVodRecord = RoomDataManger.getAllVodRecord(20);
        List<Movie.Video> vodList = new ArrayList<>();
        for (VodInfo vodInfo : allVodRecord) {
            Movie.Video vod = new Movie.Video();
            vod.id = vodInfo.id;
            vod.sourceKey = vodInfo.sourceKey;
            vod.name = vodInfo.name;
            vod.pic = vodInfo.pic;
            if (vodInfo.playNote != null && !vodInfo.playNote.isEmpty())
                vod.note = "上次看到" + vodInfo.playNote;
            vodList.add(vod);
        }
        if (vodList.isEmpty()) {
            showEmptyState("暂无观看历史");
        } else {
            hideEmptyState();
            hotAdapter.setNewData(vodList);
            updateHero(vodList);
        }
    }

    /**
     * 复用原版 UserFragment.loadHots()
     * 解析豆瓣热播 JSON
     */
    private ArrayList<Movie.Video> loadHots(String json) {
        ArrayList<Movie.Video> result = new ArrayList<>();
        try {
            JsonObject infoJson = new Gson().fromJson(json, JsonObject.class);
            JsonArray array = infoJson.getAsJsonArray("data");
            for (JsonElement ele : array) {
                JsonObject obj = (JsonObject) ele;
                Movie.Video vod = new Movie.Video();
                vod.name = obj.get("title").getAsString();
                vod.note = obj.get("rate").getAsString();
                vod.pic = obj.get("cover").getAsString() + "@User-Agent=" + UA.random() + "@Referer=https://www.douban.com/";
                result.add(vod);
            }
        } catch (Throwable th) {
        }
        return result;
    }

    /**
     * 更新 Hero 区内容(取列表第一项)
     */
    private void updateHero(List<Movie.Video> list) {
        if (list == null || list.isEmpty()) return;
        if (tvHeroTitle == null) return;  // 视图未初始化
        Movie.Video video = list.get(0);
        if (!TextUtils.isEmpty(video.pic)) {
            ImgUtil.load(video.pic, ivHeroBg, 0);
        }
        tvHeroTitle.setText(video.name);
        tvHeroYear.setText(video.year > 0 ? String.valueOf(video.year) : "");
        tvHeroType.setText(video.type != null ? video.type : "");
        tvHeroRate.setText(video.note != null ? video.note : "");
        tvHeroDesc.setText(video.des != null ? video.des : "");
    }

    /**
     * 显示空状态提示
     */
    private void showEmptyState(String hint) {
        if (emptyState == null) return;
        emptyState.setVisibility(View.VISIBLE);
        if (hint != null && tvEmptyHint != null) {
            tvEmptyHint.setText(hint);
        }
    }

    /**
     * 隐藏空状态提示
     */
    private void hideEmptyState() {
        if (emptyState != null) {
            emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hotAdapter == null) return;
        // 历史记录模式:每次返回时刷新
        if (Hawk.get(HawkConfig.HOME_REC, 0) == 2) {
            loadHistory();
        }
        // 站点推荐模式:如果 homeSourceRec 已更新,刷新列表
        if (Hawk.get(HawkConfig.HOME_REC, 0) == 1 && homeSourceRec != null
                && hotAdapter.getData().isEmpty()) {
            hotAdapter.setNewData(homeSourceRec);
            updateHero(homeSourceRec);
        }
    }

    /**
     * 从 Fragment 启动 Activity(避免与 BaseActivity.jumpActivity 命名冲突)
     */
    private void startActivityFromFragment(Class<?> clazz, Bundle bundle) {
        Intent intent = new Intent(getContext(), clazz);
        intent.putExtras(bundle);
        startActivity(intent);
    }
}
