package com.github.tvbox.osc.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.ui.activity.DriveActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * TVBOX-NEXT: 手机端文件 Fragment
 * 提供文件管理入口(网盘/本地文件)
 */
public class MobileDownloadsFragment extends Fragment {

    private RecyclerView rvDownloads;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mobile_downloads, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvDownloads = view.findViewById(R.id.rvDownloads);
        rvDownloads.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDownloads.setNestedScrollingEnabled(false);

        // 构建文件管理菜单项
        List<FileMenuItem> menuItems = new ArrayList<>();
        menuItems.add(new FileMenuItem("网盘/文件管理", R.drawable.icon_drive, DriveActivity.class));

        FileMenuAdapter adapter = new FileMenuAdapter();
        adapter.setNewData(menuItems);
        rvDownloads.setAdapter(adapter);

        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FileMenuItem item = (FileMenuItem) adapter.getItem(position);
                if (item.targetActivity != null) {
                    try {
                        Intent intent = new Intent(getContext(), item.targetActivity);
                        startActivity(intent);
                    } catch (Throwable th) {
                        Toast.makeText(getContext(), "无法打开: " + th.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    /**
     * 菜单项数据
     */
    private static class FileMenuItem {
        String title;
        int iconRes;
        Class<?> targetActivity;

        FileMenuItem(String title, int iconRes, Class<?> targetActivity) {
            this.title = title;
            this.iconRes = iconRes;
            this.targetActivity = targetActivity;
        }
    }

    /**
     * 菜单适配器
     */
    private static class FileMenuAdapter extends BaseQuickAdapter<FileMenuItem, BaseViewHolder> {

        FileMenuAdapter() {
            super(R.layout.item_profile_menu, new ArrayList<>());
        }

        @Override
        protected void convert(BaseViewHolder helper, FileMenuItem item) {
            helper.setText(R.id.tvMenuTitle, item.title);
            View ivIcon = helper.getView(R.id.ivMenuIcon);
            if (ivIcon instanceof android.widget.ImageView) {
                ((android.widget.ImageView) ivIcon).setImageResource(item.iconRes);
            }
        }
    }
}
