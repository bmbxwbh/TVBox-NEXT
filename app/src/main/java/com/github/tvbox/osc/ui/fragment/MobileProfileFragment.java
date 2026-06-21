package com.github.tvbox.osc.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.ui.activity.CollectActivity;
import com.github.tvbox.osc.ui.activity.HistoryActivity;
import com.github.tvbox.osc.ui.activity.LivePlayActivity;
import com.github.tvbox.osc.ui.activity.PushActivity;
import com.github.tvbox.osc.ui.activity.SettingActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * TVBOX-NEXT: 手机端我的 Fragment
 * 展示个人菜单:收藏/历史/设置/关于
 */
public class MobileProfileFragment extends Fragment {

    private RecyclerView rvProfileMenu;
    private ProfileMenuAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mobile_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvProfileMenu = view.findViewById(R.id.rvProfileMenu);
        rvProfileMenu.setLayoutManager(new LinearLayoutManager(getContext()));
        rvProfileMenu.setNestedScrollingEnabled(false);

        // 构建菜单项(对齐 TV 端 UserFragment 的 7 个入口)
        List<ProfileMenuItem> menuItems = new ArrayList<>();
        menuItems.add(new ProfileMenuItem("收藏", R.drawable.hm_fav, CollectActivity.class));
        menuItems.add(new ProfileMenuItem("历史", R.drawable.hm_history, HistoryActivity.class));
        menuItems.add(new ProfileMenuItem("直播", R.drawable.hm_live, LivePlayActivity.class));
        menuItems.add(new ProfileMenuItem("推送", R.drawable.hm_push, PushActivity.class));
        menuItems.add(new ProfileMenuItem("设置", R.drawable.hm_settings, SettingActivity.class));
        menuItems.add(new ProfileMenuItem("应用信息", R.drawable.icon_setting, null));

        adapter = new ProfileMenuAdapter();
        adapter.setNewData(menuItems);
        rvProfileMenu.setAdapter(adapter);

        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                ProfileMenuItem item = (ProfileMenuItem) adapter.getItem(position);
                if (item.targetActivity != null) {
                    Intent intent = new Intent(getContext(), item.targetActivity);
                    startActivity(intent);
                } else if ("应用信息".equals(item.title)) {
                    // 跳转到系统应用详情页
                    try {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.fromParts("package", getContext().getPackageName(), null));
                        startActivity(intent);
                    } catch (Throwable th) {
                        Toast.makeText(getContext(), "无法打开应用信息", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    /**
     * 菜单项数据
     */
    private static class ProfileMenuItem {
        String title;
        int iconRes;
        Class<?> targetActivity;

        ProfileMenuItem(String title, int iconRes, Class<?> targetActivity) {
            this.title = title;
            this.iconRes = iconRes;
            this.targetActivity = targetActivity;
        }
    }

    /**
     * 菜单适配器
     */
    private static class ProfileMenuAdapter extends BaseQuickAdapter<ProfileMenuItem, BaseViewHolder> {

        ProfileMenuAdapter() {
            super(R.layout.item_profile_menu, new ArrayList<>());
        }

        @Override
        protected void convert(BaseViewHolder helper, ProfileMenuItem item) {
            helper.setText(R.id.tvMenuTitle, item.title);
            // 图标(如果有)
            View ivIcon = helper.getView(R.id.ivMenuIcon);
            if (ivIcon instanceof android.widget.ImageView) {
                ((android.widget.ImageView) ivIcon).setImageResource(item.iconRes);
            }
        }
    }
}
