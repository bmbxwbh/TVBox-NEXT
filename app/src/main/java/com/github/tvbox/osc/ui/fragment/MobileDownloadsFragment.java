package com.github.tvbox.osc.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;

/**
 * TVBOX-NEXT: 手机端文件 Fragment
 * 展示下载/本地文件列表
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
        // TODO: 加载下载/文件列表
    }
}
