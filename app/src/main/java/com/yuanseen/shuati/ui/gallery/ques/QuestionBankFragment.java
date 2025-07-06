package com.yuanseen.shuati.ui.gallery.ques;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yuanseen.shuati.ui2.QuizHomeActivity;
import com.yuanseen.shuati.R;
import com.yuanseen.shuati.ui.gallery.GalleryAdapter;
import com.yuanseen.shuati.ui.gallery.GalleryItem;
import com.yuanseen.shuati.ui.gallery.SharedPreferencesHelper;
import com.yuanseen.shuati.ui.gallery.ques.option.OptionGridBuilder;
import com.yuanseen.shuati.ui.gallery.ques.option.OptionItem;
import com.yuanseen.shuati.ui4.MidActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestionBankFragment extends Fragment {
    private static final String ARG_QUESTION_BANK_ID = "question_bank_id";
    private String questionBankId;
    private SharedPreferencesHelper prefsHelper;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                returnToGalleryFragment();
                return true;
            }
            return false;
        });
    }

    private void returnToGalleryFragment() {
            requireActivity().getSupportFragmentManager().popBackStack();
    }
    // 创建Fragment实例并传入题库ID
    public static QuestionBankFragment newInstance(String questionBankId) {
        QuestionBankFragment fragment = new QuestionBankFragment();
        Bundle args = new Bundle();
        args.putString(ARG_QUESTION_BANK_ID, questionBankId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsHelper = new SharedPreferencesHelper(requireContext());
        if (getArguments() != null) {
            questionBankId = getArguments().getString(ARG_QUESTION_BANK_ID);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_question_bank, container, false);

//        TextView textView = root.findViewById(R.id.text_question_bank);
//        textView.setText("Displaying questions for bank ID: " + questionBankId);
        RecyclerView recyclerView = root.findViewById(R.id.recyclerViewBankInfo);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        GridLayout gridLayout = root.findViewById(R.id.grid_options);
        // Load the question bank data
        List<GalleryItem> items = prefsHelper.loadItems();
        GalleryItem currentItem = null;
        for (GalleryItem item : items) {
            if (item.getId().equals(questionBankId)) {
                currentItem = item;
                break;
            }
        }

        // Create a single-item list
        List<GalleryItem> singleItemList = new ArrayList<>();
        if (currentItem != null) {
            singleItemList.add(currentItem);
        }

        // Set up the adapter
        GalleryAdapter adapter = new GalleryAdapter(singleItemList);
        recyclerView.setAdapter(adapter);

        // Disable scrolling for the ListView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()) {
            @Override
            public boolean canScrollVertically() {
                return false; // 禁止垂直滚动
            }
        });
        recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_UP) {
                    returnToGalleryFragment();
                    return true;
                }
                return false;
            }
        });
        // 准备数据
        List<OptionItem> options = new ArrayList<>();
        options.add(new OptionItem(R.drawable.book_open_duotone_icon, "背题模式"));
        options.add(new OptionItem(R.drawable.books_duotone_icon, "刷题模式"));
        options.add(new OptionItem(R.drawable.shuffle_angular_duotone_icon, "随机做题"));
        options.add(new OptionItem(R.drawable.table_duotone_icon, "模拟考试"));
        options.add(new OptionItem(R.drawable.star_half_duotone_icon, "收藏本"));
        options.add(new OptionItem(R.drawable.calendar_x_duotone_icon, "错题本"));
        options.add(new OptionItem(R.drawable.magnifying_glass_duotone_icon, "题库搜索"));
        options.add(new OptionItem(R.drawable.list_checks_duotone_icon, "成绩"));
        // 添加所有12个选项...

        // 使用构建器创建网格
        new OptionGridBuilder(getContext(), gridLayout)
                .setColumnCount(2)
                .setRowCount(4)
                .build(options, (position, item) -> {
                    // 处理点击事件
                    // 处理点击事件
                    switch (position) {
                        case 0: // 背题模式
                            startQuizActivity(false);
                            break;
                        case 1: // 刷题模式
                            startQuizActivity2();
                            break;
                        case 2: // 随机做题
                            startRandomQuizActivity();
                            break;
                        case 3: // 模拟考试
                            startMNKSActivity();
                            break;
                        case 4: // 收藏本
                            startFasActivity();
                            break;
                        case 5: // 错题本
                            startWrongQuestionsActivity();
                            break;
                        case 6: //
                            startsousuoQuestionsActivity();
                            break;
                        case 7: //
                            startchengjiQuestionsActivity();
                            break;
                        default:
                            Toast.makeText(getContext(), "点击了: " + item.getLabel(), Toast.LENGTH_SHORT).show();
                            break;
                    }
                });

        return root;
    }
    private void startsousuoQuestionsActivity() {
        Intent intent = new Intent(getActivity(), com.yuanseen.shuati.ui8.MidActivity.class);
        intent.putExtra("question_bank_id", questionBankId);
        startActivity(intent);
    }
    private void startchengjiQuestionsActivity() {
        Intent intent = new Intent(getActivity(), com.yuanseen.shuati.ui9.MidActivity.class);
        intent.putExtra("question_bank_id", questionBankId);
        startActivity(intent);
    }
    // 启动背题/刷题模式Activity
    private void startQuizActivity(Boolean isPracticeMode) {
        Intent intent = new Intent(getActivity(), com.yuanseen.shuati.ui2.QuizHomeActivity.class);
        intent.putExtra("question_bank_id", questionBankId);
        intent.putExtra("practice_mode", isPracticeMode);
        startActivity(intent);
    }

    // 启动背题/刷题模式Activity
    private void startQuizActivity2() {
        Intent intent = new Intent(getActivity(), com.yuanseen.shuati.ui3.QuizHomeActivity.class);
        intent.putExtra("question_bank_id", questionBankId);
//        intent.putExtra("practice_mode", isPracticeMode);
        startActivity(intent);
    }

    // 修改startRandomQuizActivity方法
    private void startRandomQuizActivity() {
        Intent intent = new Intent(getActivity(), com.yuanseen.shuati.ui4.MidActivity.class);
        intent.putExtra("question_bank_id", questionBankId);
        startActivity(intent);
    }

    // 修改startRandomQuizActivity方法
    private void startMNKSActivity() {
        Intent intent = new Intent(getActivity(), com.yuanseen.shuati.ui5.MidActivity.class);
        intent.putExtra("question_bank_id", questionBankId);
        startActivity(intent);
    }


    private Map<String, List<Integer>> loadWrongFromInfoFile() {
        Map<String, List<Integer>> favoritesMap = new HashMap<>();
        // 初始化默认的空列表
        favoritesMap.put("danxuan", new ArrayList<>());
        favoritesMap.put("duoxuan", new ArrayList<>());
        favoritesMap.put("panduan", new ArrayList<>());

        try {
            File infoFile = new File(requireContext().getFilesDir(), "question_banks/" + questionBankId + "_info.json");
            if (infoFile.exists()) {
                String jsonStr = new String(Files.readAllBytes(infoFile.toPath()));
                JSONObject infoJson = new JSONObject(jsonStr);
                JSONObject favorites = infoJson.optJSONObject("wrongQuestions");

                if (favorites != null) {
                    // 安全地获取每个题型的收藏列表
                    favoritesMap.put("danxuan", jsonArrayToList(favorites.optJSONArray("danxuan")));
                    favoritesMap.put("duoxuan", jsonArrayToList(favorites.optJSONArray("duoxuan")));
                    favoritesMap.put("panduan", jsonArrayToList(favorites.optJSONArray("panduan")));
                }
            }
        } catch (Exception e) {
            Log.e("QuestionBankFragment", "加载错题数据失败", e);
        }
        return favoritesMap;
    }

    private void startWrongQuestionsActivity() {
        // 检查是否有收藏题目
        Map<String, List<Integer>> favorites =  loadWrongFromInfoFile();

        // 安全地检查每个题型是否有收藏题目
        boolean hasFavorites = (favorites.get("danxuan") != null && !favorites.get("danxuan").isEmpty()) ||
                (favorites.get("duoxuan") != null && !favorites.get("duoxuan").isEmpty()) ||
                (favorites.get("panduan") != null && !favorites.get("panduan").isEmpty());

        if (!hasFavorites) {
            Toast.makeText(getContext(), "错题本内无题目", Toast.LENGTH_SHORT).show();
            return;
        }

        // 如果有收藏题目，启动Activity
        Intent intent = new Intent(getActivity(), com.yuanseen.shuati.ui7.QuizHomeActivity.class);
        intent.putExtra("question_bank_id", questionBankId);
        intent.putExtra("wrong_questions_mode", true);
        startActivity(intent);
    }


    private Map<String, List<Integer>> loadFavoritesFromInfoFile() {
        Map<String, List<Integer>> favoritesMap = new HashMap<>();
        // 初始化默认的空列表
        favoritesMap.put("danxuan", new ArrayList<>());
        favoritesMap.put("duoxuan", new ArrayList<>());
        favoritesMap.put("panduan", new ArrayList<>());

        try {
            File infoFile = new File(requireContext().getFilesDir(), "question_banks/" + questionBankId + "_info.json");
            if (infoFile.exists()) {
                String jsonStr = new String(Files.readAllBytes(infoFile.toPath()));
                JSONObject infoJson = new JSONObject(jsonStr);
                JSONObject favorites = infoJson.optJSONObject("favorites");

                if (favorites != null) {
                    // 安全地获取每个题型的收藏列表
                    favoritesMap.put("danxuan", jsonArrayToList(favorites.optJSONArray("danxuan")));
                    favoritesMap.put("duoxuan", jsonArrayToList(favorites.optJSONArray("duoxuan")));
                    favoritesMap.put("panduan", jsonArrayToList(favorites.optJSONArray("panduan")));
                }
            }
        } catch (Exception e) {
            Log.e("QuestionBankFragment", "加载收藏数据失败", e);
        }
        return favoritesMap;
    }

    private List<Integer> jsonArrayToList(JSONArray jsonArray) {
        List<Integer> list = new ArrayList<>();
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.optInt(i));
            }
        }
        return list;
    }
    private void startFasActivity() {
        // 检查是否有收藏题目
        Map<String, List<Integer>> favorites = loadFavoritesFromInfoFile();

        // 安全地检查每个题型是否有收藏题目
        boolean hasFavorites = (favorites.get("danxuan") != null && !favorites.get("danxuan").isEmpty()) ||
                (favorites.get("duoxuan") != null && !favorites.get("duoxuan").isEmpty()) ||
                (favorites.get("panduan") != null && !favorites.get("panduan").isEmpty());

        if (!hasFavorites) {
            Toast.makeText(getContext(), "收藏本内无题目", Toast.LENGTH_SHORT).show();
            return;
        }

        // 如果有收藏题目，启动Activity
        Intent intent = new Intent(getActivity(), com.yuanseen.shuati.ui6.QuizHomeActivity.class);
        intent.putExtra("question_bank_id", questionBankId);
        intent.putExtra("fas_questions_mode", true);
        startActivity(intent);
    }

}