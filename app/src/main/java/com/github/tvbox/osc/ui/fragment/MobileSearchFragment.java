package com.github.tvbox.osc.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.ui.activity.MobileSearchActivity;

/**
 * TVBOX-NEXT: 手机端搜索 Fragment(底部导航搜索页)
 * 搜索入口,实际搜索逻辑在 MobileSearchActivity 中
 */
public class MobileSearchFragment extends Fragment {

    private EditText etSearch;
    private RecyclerView rvResults;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mobile_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        etSearch = view.findViewById(R.id.etSearch);
        rvResults = view.findViewById(R.id.rvSearchResults);

        rvResults.setLayoutManager(new GridLayoutManager(getContext(), 3));

        // 搜索框:按下搜索键时跳转到 MobileSearchActivity
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String keyword = v.getText().toString().trim();
                if (!TextUtils.isEmpty(keyword)) {
                    Intent intent = new Intent(getContext(), MobileSearchActivity.class);
                    intent.putExtra("title", keyword);
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), getString(R.string.search_input), Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });
    }
}
