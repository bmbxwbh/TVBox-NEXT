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
 * TVBOX-NEXT: 手机端我的 Fragment
 * 展示个人设置菜单
 */
public class MobileProfileFragment extends Fragment {

    private RecyclerView rvProfileMenu;

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
        // TODO: 加载设置菜单项(收藏/历史/设置/关于等)
    }
}
