package com.yuanseen.shuati.ui.gallery;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.yuanseen.shuati.R;
import com.yuanseen.shuati.ui.gallery.addqes.AddQuestionBankFragment;
import com.yuanseen.shuati.ui.gallery.ques.QuestionBankFragment;
import com.yuanseen.shuati.ui2.QuestionDetailFragment;

import java.io.File;
import java.util.List;

public class GalleryFragment extends Fragment {
    private static final String LOCAL_STORAGE_DIR = "question_banks";
    private GalleryAdapter adapter;
    private List<GalleryItem> items;
    private TextView emptyView;
    private static final int MAX_RECENT_BANKS = 3;
    private RecyclerView recyclerView;

    private SharedPreferencesHelper prefsHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsHelper = new SharedPreferencesHelper(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_gallery, container, false);

        // 初始化空状态视图
        emptyView = root.findViewById(R.id.emptyView);

        // 从 SharedPreferences 加载数据
        items = prefsHelper.loadItems();

        // 顶部加号按钮
        ImageButton addButton = root.findViewById(R.id.btnAdd);
        addButton.setOnClickListener(v -> {
            AddQuestionBankFragment fragment = new AddQuestionBankFragment();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_right,    // 新Fragment进入动画
                            R.anim.slide_out_left,    // 当前Fragment退出动画
                            R.anim.slide_in_left,     // 返回时新Fragment进入动画（逆向）
                            R.anim.slide_out_right    // 返回时当前Fragment退出动画（逆向）
                    )
                    .add(R.id.fragment_container, fragment)  // 添加新 Fragment
                    .hide(this)  // 隐藏当前 GalleryFragment（不销毁）
                    .addToBackStack(null)
                    .commit();
        });

        // 初始化RecyclerView
        recyclerView = root.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 设置适配器
        adapter = new GalleryAdapter(items);
        recyclerView.setAdapter(adapter);

        // 检查并更新空状态
        updateEmptyView();

// In GalleryFragment.java, replace the current setOnItemClickListener with:
        //#$#在跳转之前通过SharedPreferences保存进入的题库GalleryItem，最多保存三个，如果已经保存了三个那保存前确认是否保存的三个里面已经有本次进入的题库，如果有那就删除之前的，保存现在的，如果没有则删除最早保存的，保存现在的。
        //#$#并提供获取保存的题库GalleryItem的静态方法
        adapter.setOnItemClickListener(position -> {
            GalleryItem selectedItem = items.get(position);
            saveRecentBank(selectedItem);

            QuestionBankFragment fragment = QuestionBankFragment.newInstance(selectedItem.getId());

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_right,    // enter
                            R.anim.slide_out_left,    // exit
                            R.anim.slide_in_left,     // popEnter
                            R.anim.slide_out_right    // popExit
                    )
                    .add(R.id.fragment_container, fragment)
                    .hide(this)  // 隐藏当前 GalleryFragment（不销毁）
                    .addToBackStack(null)
                    .commit();
        });

        // 滑动删除逻辑
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            // 在 GalleryFragment.java 中修改 onSwiped 方法
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                GalleryItem deletedItem = items.get(position);

                // 从列表删除并保存
                items.remove(position);
                prefsHelper.saveItems(items);
                adapter.notifyItemRemoved(position);
                updateEmptyView();

                // 显示撤销 Snackbar
                Snackbar snackbar = Snackbar.make(recyclerView, "正在删除题库", Snackbar.LENGTH_LONG)
                        .setAction("撤销", v -> {
                            // 撤销操作：直接恢复列表项
                            items.add(position, deletedItem);
                            prefsHelper.saveItems(items);
                            adapter.notifyItemInserted(position);
                            updateEmptyView();
                        });

                snackbar.setTextColor(getResources().getColor(R.color.black));
                snackbar.setActionTextColor(getResources().getColor(R.color.gray_800));

                // 设置 Snackbar 消失监听
                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);
                        // 只有当不是点击"撤销"按钮导致的消失时才删除文件
                        if (event != DISMISS_EVENT_ACTION) {
                            // 删除本地题库文件
                            deleteLocalBankFiles(deletedItem.getId());
                            // 清除该题库的阅读进度
                            QuestionDetailFragment.clearReadingProgress(requireContext(), deletedItem.getId());

                            Toast.makeText(getContext(), "题库文件已删除", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                snackbar.show();
            }

        });
        itemTouchHelper.attachToRecyclerView(recyclerView);

        return root;
    }
    private void deleteLocalBankFiles(String bankId) {
        File bankDir = new File(requireContext().getFilesDir(), LOCAL_STORAGE_DIR);
        File[] filesToDelete = {
                new File(bankDir, bankId + ".json"),
//                new File(bankDir, bankId + "_info.json")
        };

        for (File file : filesToDelete) {
            if (file.exists()) {
                if (file.delete()) {
                    Log.d("DeleteSuccess", "Deleted: " + file.getName());
                } else {
                    Log.e("DeleteError", "Failed to delete: " + file.getAbsolutePath());
                }
            }
        }
    }
    // 更新空状态视图
    private void updateEmptyView() {
        if (items.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }
    // 添加新题库
// 添加新题库（检查是否已存在）
    public void addQuestionBank(GalleryItem item) {
        // 检查是否已存在相同id的题库
        boolean alreadyExists = false;
        for (GalleryItem existingItem : items) {
            if (existingItem.getId() != null && existingItem.getId().equals(item.getId())) {
                alreadyExists = true;
                break;
            }
        }

        if (!alreadyExists) {
            items.add(0, item);
            prefsHelper.saveItems(items); // 保存到 SharedPreferences
            adapter.notifyItemInserted(0);
            updateEmptyView();
        } else {
            // 可选：显示提示信息
            Toast.makeText(getContext(), "该题库已存在", Toast.LENGTH_SHORT).show();
        }
    }

    // 添加静态方法获取最近访问的题库
    public static List<GalleryItem> getRecentBanks(android.content.Context context) {
        SharedPreferencesHelper helper = new SharedPreferencesHelper(context);
        return helper.loadRecentItems();
    }

    // 保存最近访问的题库
    private void saveRecentBank(GalleryItem item) {
        List<GalleryItem> recentBanks = prefsHelper.loadRecentItems();

        // 检查是否已存在
        int existingIndex = -1;
        for (int i = 0; i < recentBanks.size(); i++) {
            if (recentBanks.get(i).getId().equals(item.getId())) {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex != -1) {
            // 如果已存在，先移除旧的
            recentBanks.remove(existingIndex);
        } else if (recentBanks.size() >= MAX_RECENT_BANKS) {
            // 如果不存在且已达上限，移除最早的一个
            recentBanks.remove(recentBanks.size() - 1);
        }

        // 添加到开头
        recentBanks.add(0, item);
        prefsHelper.saveRecentItems(recentBanks);
    }


}