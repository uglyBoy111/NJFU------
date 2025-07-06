package com.yuanseen.shuati.ui.gallery.addqes;

import static java.util.Locale.filter;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.yuanseen.shuati.R;
import com.yuanseen.shuati.ui.gallery.GalleryFragment;
import com.yuanseen.shuati.ui.gallery.GalleryItem;
import com.yuanseen.shuati.ui.gallery.addqes.bankinfo.BankInfo;
import com.yuanseen.shuati.ui.gallery.addqes.bankinfo.BankInfoManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AddQuestionBankFragment extends Fragment {

    private QuestionBankAdapter adapter;
    private List<QuestionBankItem> availableBanks;  // 所有可用题库列表
    private List<QuestionBankItem> filteredBanks;  // 过滤后的题库列表
    private ProgressBar progressBar;  // 加载进度条
    private TextView errorTextView;   // 错误提示文本
    private View retryButton;        // 重试按钮
    // 在类中添加常量定义
    private static final String LOCAL_STORAGE_DIR = "question_banks";
    private File localBankDirectory;

    private int downloadCounter = 0;
    private int totalDownloads = 0;
    private boolean isDownloading = false;

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            view.setFocusableInTouchMode(true);
            view.requestFocus();
            view.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (isDownloading) {
                        Toast.makeText(getContext(), "题库下载中，请稍候...", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    returnToGalleryFragment();
                    return true;
                }
                return false;
            });
        }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_add_question_bank, container, false);

        // 初始化视图组件
        progressBar = root.findViewById(R.id.progressBar);
        errorTextView = root.findViewById(R.id.errorTextView);
        retryButton = root.findViewById(R.id.retryButton);
        RecyclerView recyclerView = root.findViewById(R.id.recyclerView);
        SearchView searchView = root.findViewById(R.id.searchview);
        Button addButton = root.findViewById(R.id.btnAddSelected);

        // 设置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        availableBanks = new ArrayList<>();
        filteredBanks = new ArrayList<>();
        adapter = new QuestionBankAdapter(filteredBanks);
        recyclerView.setAdapter(adapter);

        // 从API加载题库数据
        fetchQuestionBanks();

        // 设置重试按钮点击事件
        retryButton.setOnClickListener(v -> {
            showLoading();  // 显示加载状态
            fetchQuestionBanks();  // 重新获取数据
        });

        // 设置搜索功能
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);  // 根据输入过滤题库
                return true;
            }
        });

        // 设置添加按钮点击事件
        // 修改 addButton 的点击事件处理
        addButton.setOnClickListener(v -> {
            List<QuestionBankItem> selectedBanks = getSelectedBanks();
            if (selectedBanks.isEmpty()) {
                Toast.makeText(getContext(), "请至少选择一个题库", Toast.LENGTH_SHORT).show();
                return;
            }

            addSelectedBanksToGallery(selectedBanks);
            // 开始下载流程
            downloadSelectedBanks(selectedBanks);
        });
// 初始化本地存储目录
        localBankDirectory = new File(requireContext().getFilesDir(), LOCAL_STORAGE_DIR);
        if (!localBankDirectory.exists()) {
            localBankDirectory.mkdirs();
        }

        return root;
    }

    // 下载选中的题库
    private void downloadSelectedBanks(List<QuestionBankItem> selectedBanks) {
        totalDownloads = selectedBanks.size();
        downloadCounter = 0;
        isDownloading = true;

        // 显示下载进度提示
        Toast.makeText(getContext(), "正在下载题库...", Toast.LENGTH_SHORT).show();

        for (QuestionBankItem item : selectedBanks) {
            downloadBankJson(item.getId(), new DownloadCallback() {
                @Override
                public void onSuccess(String bankId) {
                    downloadCounter++;
                    checkAllDownloadsComplete();
                }

                @Override
                public void onFailure(String error) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "下载题库失败: " + error, Toast.LENGTH_SHORT).show();
                    });
                    downloadCounter++;
                    checkAllDownloadsComplete();
                }
            });
        }
    }

    // 检查所有下载是否完成
    private void checkAllDownloadsComplete() {
        if (downloadCounter >= totalDownloads) {
            requireActivity().runOnUiThread(() -> {
                isDownloading = false;
                Toast.makeText(getContext(), "题库下载完成", Toast.LENGTH_SHORT).show();
                returnToGalleryFragment();
            });
        }
    }

    // 修改返回逻辑，防止在下载过程中退出
    private void returnToGalleryFragment() {
        if (!isDownloading) {
            requireActivity().getSupportFragmentManager().popBackStack();
        }
    }
    /**
     * 从API获取题库数据
     */
    private void fetchQuestionBanks() {
        showLoading();  // 显示加载状态

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://Mc2.bxvps.cn:24721/api/banks")  // API地址
                .build();

        // 异步网络请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() -> {
                    showError("网络连接失败，请检查网络设置");
                    e.printStackTrace(); // 添加这行查看详细错误
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    // 服务器返回错误
                    requireActivity().runOnUiThread(() -> showError("服务器错误: " + response.code()));
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONArray jsonArray = new JSONArray(responseData);
                    List<QuestionBankItem> banks = new ArrayList<>();

                    // 解析JSON数据
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject bankJson = jsonArray.getJSONObject(i);
                        QuestionBankItem item = new QuestionBankItem(
                                bankJson.getString("id"),
                                R.drawable.book_duotone_icon,
                                bankJson.getString("title"),
                                bankJson.getString("subject"),
                                bankJson.getString("description"),
                                bankJson.optBoolean("isSelected", false)
                        );
                        banks.add(item);
                    }

                    // 更新UI
                    requireActivity().runOnUiThread(() -> {
                        availableBanks.clear();
                        availableBanks.addAll(banks);
                        filteredBanks.clear();
                        filteredBanks.addAll(availableBanks);
                        adapter.notifyDataSetChanged();
                        showContent();  // 显示内容
                    });
                } catch (JSONException e) {
                    // JSON解析错误
                    requireActivity().runOnUiThread(() -> showError("数据解析失败"));
                }
            }
        });
    }

    /**
     * 显示加载状态
     */
    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        errorTextView.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
    }

    /**
     * 显示错误状态
     * @param message 错误信息
     */
    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        errorTextView.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.VISIBLE);
        errorTextView.setText(message);
    }

    /**
     * 显示内容
     */
    private void showContent() {
        progressBar.setVisibility(View.GONE);
        errorTextView.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
    }
    // 获取选中的题库（修改为从过滤后的列表中获取）
    private List<QuestionBankItem> getSelectedBanks() {
        List<QuestionBankItem> selectedBanks = new ArrayList<>();
        for (QuestionBankItem item : filteredBanks) {
            if (item.isSelected()) {
                selectedBanks.add(item);
            }
        }
        return selectedBanks;
    }

    // 过滤方法
    private void filter(String text) {
        filteredBanks.clear();
        if (text.isEmpty()) {
            filteredBanks.addAll(availableBanks);
        } else {
            text = text.toLowerCase();
            for (QuestionBankItem item : availableBanks) {
                if (item.getTitle().toLowerCase().contains(text) ||
                        item.getSubtitle().toLowerCase().contains(text) ||
                        item.getDesc().toLowerCase().contains(text)) {
                    filteredBanks.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    // 修改addSelectedBanksToGallery方法
    private void addSelectedBanksToGallery(List<QuestionBankItem> selectedBanks) {
        GalleryFragment galleryFragment = getGalleryFragment();
        if (galleryFragment == null) return;

        for (QuestionBankItem item : selectedBanks) {
            // 先下载题库JSON文件
            downloadBankJson(item.getId(), new DownloadCallback() {
                @Override
                public void onSuccess(String bankId) {
                    requireActivity().runOnUiThread(() -> {
                        // 下载成功后再添加到Gallery
                        GalleryItem newItem = new GalleryItem(
                                item.getId(),
                                item.getImageRes(),
                                item.getTitle(),
                                item.getSubtitle(),
                                item.getDesc());
                        galleryFragment.addQuestionBank(newItem);
                    });
                }

                @Override
                public void onFailure(String error) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "下载题库失败: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    // 获取GalleryFragment实例
    private GalleryFragment getGalleryFragment() {
        return (GalleryFragment) requireActivity()
                .getSupportFragmentManager()
                .findFragmentByTag("gallery");
    }


    // 下载题库JSON文件的方法
    private void downloadBankJson(String bankId, DownloadCallback callback) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://Mc2.bxvps.cn:24721/api/bank/" + bankId)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("HTTP错误: " + response.code());
                    return;
                }

                try {
                    String jsonData = response.body().string();

                    // 保存题库JSON文件
                    File bankFile = new File(localBankDirectory, bankId + ".json");
                    try (FileWriter writer = new FileWriter(bankFile)) {
                        writer.write(jsonData);
                    }

                    // 创建并保存info文件
                    BankInfo bankInfo = new BankInfo(bankId);
                    File infoFile = new File(localBankDirectory, bankId + "info.json");
                    try (FileWriter infoWriter = new FileWriter(infoFile)) {
                        infoWriter.write(bankInfo.toJsonString());
                    }

                    callback.onSuccess(bankId);
                } catch (Exception e) {
                    callback.onFailure("处理响应失败: " + e.getMessage());
                }
            }
        });
    }

    // 下载回调接口
    interface DownloadCallback {
        void onSuccess(String bankId);
        void onFailure(String error);
    }

    // 在AddQuestionBankFragment.java中添加
    public static String getLocalStoragePath(Context context) {
        return new File(context.getFilesDir(), LOCAL_STORAGE_DIR).getAbsolutePath();
    }
/*
    Example usage:
    BankInfoManager manager = new BankInfoManager(context);
    manager.addGeneratedPaper("tk001");  // Adds "tk001paper001"
    manager.addGeneratedPaper("tk001");  // Adds "tk001paper002"
*/
}