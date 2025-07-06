package com.yuanseen.shuati.ui6;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.yuanseen.shuati.R;
import com.yuanseen.shuati.ui.gallery.addqes.bankinfo.BankInfo;
import com.yuanseen.shuati.ui.gallery.addqes.bankinfo.BankInfoManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizHomeActivity extends AppCompatActivity implements QuestionDetailFragment.OnSwipeListener {

    private DrawerLayout quizDrawerLayout;
    private NavigationView questionNavigationView;
    private FloatingActionButton navigationToggleFab;
    private QuestionDetailFragment questionDetailFragment;
    public BankInfoManager bankInfoManager;
    private String currentBankId ; // 当前题库ID
    private AlertDialog dialog; // Add this with other member variables
    private String currentQuestionType;
    private int currentQuestionId;

    private boolean isProcessingSwipe = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_home6);
        // 获取传入的题库ID
        currentBankId = getIntent().getStringExtra("question_bank_id");
        // 初始化视图
        quizDrawerLayout = findViewById(R.id.quiz_drawer_layout);

        navigationToggleFab = findViewById(R.id.navigation_toggle_fab);

        // 初始化BankInfoManager
        bankInfoManager = new BankInfoManager(this);

        // 设置FAB点击事件
        navigationToggleFab.setOnClickListener(view -> showQuestionTypeDialog());

        // 初始化题目导航菜单


        // 设置菜单项点击事件


        // 初始化Fragment后设置滑动监听
        questionDetailFragment = (QuestionDetailFragment) getSupportFragmentManager()
                .findFragmentById(R.id.question_detail_fragment6);
        if (questionDetailFragment != null) {
            questionDetailFragment.setOnSwipeListener(this);
        }
        // 延迟加载题目，确保Fragment已附加
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                loadLastReadingProgress();
                highlightLastReadQuestion();
            }
        }, 100); // 延迟100ms确保Fragment就绪
    }
        // Add this method to QuizHomeActivity
        private Map<String, List<Integer>> loadFavoritesFromInfoFile() {
            Map<String, List<Integer>> favoritesMap = new HashMap<>();
            try {
                File infoFile = new File(getFilesDir(), "question_banks/" + currentBankId + "_info.json");
                if (infoFile.exists()) {
                    String jsonStr = new String(Files.readAllBytes(infoFile.toPath()));
                    JSONObject infoJson = new JSONObject(jsonStr);
                    JSONObject favorites = infoJson.getJSONObject("favorites");

                    // Load favorites for each question type
                    favoritesMap.put("danxuan", jsonArrayToList(favorites.optJSONArray("danxuan")));
                    favoritesMap.put("duoxuan", jsonArrayToList(favorites.optJSONArray("duoxuan")));
                    favoritesMap.put("panduan", jsonArrayToList(favorites.optJSONArray("panduan")));
                }
            } catch (Exception e) {
                Log.e("QuizHomeActivity", "加载收藏数据失败", e);
            }
            return favoritesMap;
        }
    // 删除原有菜单相关代码，添加新的对话框显示方法
    private void showQuestionTypeDialog() {

        // 清除之前的选中状态
        QuestionGridAdapter.clearSelection();

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialog);
        builder.setTitle("选择题目类型");

        // 加载自定义布局
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_question_types, null);
        builder.setView(dialogView);

        // 查找视图
        LinearLayout singleChoiceLayout = dialogView.findViewById(R.id.single_choice_layout);
        LinearLayout multiChoiceLayout = dialogView.findViewById(R.id.multi_choice_layout);
        LinearLayout trueFalseLayout = dialogView.findViewById(R.id.true_false_layout);
        RecyclerView singleChoiceGrid = dialogView.findViewById(R.id.single_choice_grid);
        RecyclerView multiChoiceGrid = dialogView.findViewById(R.id.multi_choice_grid);
        RecyclerView trueFalseGrid = dialogView.findViewById(R.id.true_false_grid);

// 设置每种题型的点击事件
        singleChoiceLayout.setOnClickListener(v -> {
            toggleQuestionNumbers(singleChoiceGrid, "单选题");
            // 设置当前选中题目
            if (currentQuestionType.equals("SINGLE_CHOICE")) {
                ((QuestionGridAdapter)singleChoiceGrid.getAdapter()).setLastReadQuestionId(currentQuestionId);
            }
        });

        multiChoiceLayout.setOnClickListener(v -> {
            toggleQuestionNumbers(multiChoiceGrid, "多选题");
            if (currentQuestionType.equals("MULTI_CHOICE")) {
                ((QuestionGridAdapter)multiChoiceGrid.getAdapter()).setLastReadQuestionId(currentQuestionId);
            }
        });

        trueFalseLayout.setOnClickListener(v -> {
            toggleQuestionNumbers(trueFalseGrid, "判断题");
            if (currentQuestionType.equals("TRUE_FALSE")) {
                ((QuestionGridAdapter)trueFalseGrid.getAdapter()).setLastReadQuestionId(currentQuestionId);
            }
        });
        // 初始化题号网格(初始隐藏)
        setupQuestionGrid(singleChoiceGrid, "SINGLE_CHOICE");
        setupQuestionGrid(multiChoiceGrid, "MULTI_CHOICE");
        setupQuestionGrid(trueFalseGrid, "TRUE_FALSE");
        singleChoiceGrid.setVisibility(View.GONE);
        multiChoiceGrid.setVisibility(View.GONE);
        trueFalseGrid.setVisibility(View.GONE);

        // 设置对话框按钮
//        builder.setNegativeButton("关闭", null);
        builder.setNegativeButton("关闭", (dialog, which) -> {
            // 保存选中状态
            if (currentQuestionType != null) {
                QuestionDetailFragment.saveReadingProgress(this, currentBankId, currentQuestionType, currentQuestionId);
            }
        });
        dialog = builder.create();
        dialog.setOnDismissListener(dialog -> {
            // 清除选中状态
            QuestionGridAdapter.clearSelection();
        });
        dialog.show();
    }

    // 切换题号显示/隐藏
    private void toggleQuestionNumbers(RecyclerView gridView, String questionType) {
        if (gridView.getVisibility() == View.VISIBLE) {
            gridView.setVisibility(View.GONE);
        } else {
            // 先隐藏其他网格
            ViewGroup parent = (ViewGroup) gridView.getParent();
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                if (child instanceof RecyclerView && child != gridView) {
                    child.setVisibility(View.GONE);
                }
            }
            gridView.setVisibility(View.VISIBLE);
        }
    }

    // 设置题号网格
    private void setupQuestionGrid(RecyclerView recyclerView, String questionType) {
        recyclerView.setLayoutManager(new GridLayoutManager(this, 6));

        // 从bankid_info.json中获取收藏的题目
        List<Integer> favoriteQuestions = getFavoriteQuestions(questionType);

        QuestionGridAdapter adapter = new QuestionGridAdapter(favoriteQuestions, questionType);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener((type, number) -> {
            displayQuestion(type, number);
            if (dialog != null) dialog.dismiss();
        });
    }

    // 新增方法：从bankid_info.json获取收藏题目
    private List<Integer> getFavoriteQuestions(String questionType) {
        List<Integer> favorites = new ArrayList<>();
        try {
            File infoFile = new File(getFilesDir(), "question_banks/" + currentBankId + "_info.json");
            if (infoFile.exists()) {
                String jsonStr = new String(Files.readAllBytes(infoFile.toPath()));
                JSONObject infoJson = new JSONObject(jsonStr);
                JSONObject favoritesJson = infoJson.getJSONObject("favorites");

                String typeKey = getTypeKey(questionType);
                JSONArray favArray = favoritesJson.optJSONArray(typeKey);

                if (favArray != null) {
                    for (int i = 0; i < favArray.length(); i++) {
                        favorites.add(favArray.getInt(i));
                    }
                }
            }
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "读取收藏题目失败", e);
        }
        return favorites;
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
    @Override
    public void onSwipeLeft() {
        if (isProcessingSwipe) return;
        isProcessingSwipe = true;

        runOnUiThread(() -> {
            // 获取当前题型的收藏题目列表
            List<Integer> favoriteQuestions = bankInfoManager.getBankInfo(currentBankId)
                    .getFavorites().get(getTypeKey(currentQuestionType));

            if (favoriteQuestions.isEmpty()) {
                Toast.makeText(this, "没有收藏的题目", Toast.LENGTH_SHORT).show();
                isProcessingSwipe = false;
                return;
            }

            // 找到当前题目在收藏列表中的位置
            int currentIndex = favoriteQuestions.indexOf(currentQuestionId);

            if (currentIndex == -1) {
                // 当前题目不在收藏列表中，切换到收藏列表第一题
                int firstFavoriteId = favoriteQuestions.get(0);
                currentQuestionId = firstFavoriteId;
                questionDetailFragment.displayQuestionDetails(
                        loadQuestionFromFile(currentBankId, currentQuestionType, firstFavoriteId),
                        true, // 是收藏
                        bankInfoManager.getBankInfo(currentBankId).getWrongQuestions()
                                .get(getTypeKey(currentQuestionType)).contains(firstFavoriteId),
                        true
                );
            } else if (currentIndex < favoriteQuestions.size() - 1) {
                // 切换到下一道收藏题
                int nextId = favoriteQuestions.get(currentIndex + 1);
                currentQuestionId = nextId;
                questionDetailFragment.displayQuestionDetails(
                        loadQuestionFromFile(currentBankId, currentQuestionType, nextId),
                        true,
                        bankInfoManager.getBankInfo(currentBankId).getWrongQuestions()
                                .get(getTypeKey(currentQuestionType)).contains(nextId),
                        true
                );
            } else {
                // 已经是最后一道收藏题
                Toast.makeText(this, "已经是最后一道收藏题了", Toast.LENGTH_SHORT).show();
            }

            isProcessingSwipe = false;
        });
    }

    @Override
    public void onSwipeRight() {
        if (isProcessingSwipe) return;
        isProcessingSwipe = true;

        runOnUiThread(() -> {
            // 获取当前题型的收藏题目列表
            List<Integer> favoriteQuestions = bankInfoManager.getBankInfo(currentBankId)
                    .getFavorites().get(getTypeKey(currentQuestionType));

            if (favoriteQuestions.isEmpty()) {
                Toast.makeText(this, "没有收藏的题目", Toast.LENGTH_SHORT).show();
                isProcessingSwipe = false;
                return;
            }

            // 找到当前题目在收藏列表中的位置
            int currentIndex = favoriteQuestions.indexOf(currentQuestionId);

            if (currentIndex == -1) {
                // 当前题目不在收藏列表中，切换到收藏列表最后一题
                int lastFavoriteId = favoriteQuestions.get(favoriteQuestions.size() - 1);
                currentQuestionId = lastFavoriteId;
                questionDetailFragment.displayQuestionDetails(
                        loadQuestionFromFile(currentBankId, currentQuestionType, lastFavoriteId),
                        true,
                        bankInfoManager.getBankInfo(currentBankId).getWrongQuestions()
                                .get(getTypeKey(currentQuestionType)).contains(lastFavoriteId),
                        false
                );
            } else if (currentIndex > 0) {
                // 切换到上一道收藏题
                int prevId = favoriteQuestions.get(currentIndex - 1);
                currentQuestionId = prevId;
                questionDetailFragment.displayQuestionDetails(
                        loadQuestionFromFile(currentBankId, currentQuestionType, prevId),
                        true,
                        bankInfoManager.getBankInfo(currentBankId).getWrongQuestions()
                                .get(getTypeKey(currentQuestionType)).contains(prevId),
                        false
                );
            } else {
                // 已经是第一道收藏题
                Toast.makeText(this, "已经是第一道收藏题了", Toast.LENGTH_SHORT).show();
            }

            isProcessingSwipe = false;
        });
    }


    // 带动画的题目显示方法
    private void animateAndDisplayQuestion(String type, int questionId, boolean isSwipeLeft) {
        if (questionDetailFragment != null) {
            // 执行滑动动画
            questionDetailFragment.animateSwipe(isSwipeLeft, () -> {
                currentQuestionType = type;
                currentQuestionId = questionId;
                displayQuestion(type, questionId);
            });
        } else {
            currentQuestionType = type;
            currentQuestionId = questionId;
            displayQuestion(type, questionId);
        }
    }

    // 尝试加载下一题型
    private void tryLoadNextQuestionType(boolean forward) {
        String nextType = getNextQuestionType(currentQuestionType, forward);
        if (nextType != null) {
            List<Integer> questionNumbers = loadQuestionNumbers(nextType);
            if (!questionNumbers.isEmpty()) {
                // 如果是向前滑动，加载新题型第一题；向后则加载最后一题
                int targetId = forward ? questionNumbers.get(0) : questionNumbers.get(questionNumbers.size() - 1);
                animateAndDisplayQuestion(nextType, targetId, forward);

                return;
            }
        }
        Toast.makeText(this, forward ? "已经是最后一题了" : "已经是第一题了", Toast.LENGTH_SHORT).show();
    }

    // 获取下一题型
    private String getNextQuestionType(String currentType, boolean forward) {
        String[] types = {"SINGLE_CHOICE", "MULTI_CHOICE", "TRUE_FALSE"};
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(currentType)) {
                if (forward && i < types.length - 1) return types[i + 1];
                if (!forward && i > 0) return types[i - 1];
                break;
            }
        }
        return null;
    }

    private void loadNextQuestion() {
        if (currentQuestionType == null) return;

        List<Integer> questionNumbers = loadQuestionNumbers(currentQuestionType);
        if (questionNumbers.isEmpty()) return;

        int currentIndex = questionNumbers.indexOf(currentQuestionId);
        if (currentIndex < questionNumbers.size() - 1) {
            int nextQuestionId = questionNumbers.get(currentIndex + 1);
            displayQuestion(currentQuestionType, nextQuestionId);
        } else {
            Toast.makeText(this, "已经是最后一题了", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPreviousQuestion() {
        if (currentQuestionType == null) return;

        List<Integer> questionNumbers = loadQuestionNumbers(currentQuestionType);
        if (questionNumbers.isEmpty()) return;

        int currentIndex = questionNumbers.indexOf(currentQuestionId);
        if (currentIndex > 0) {
            int prevQuestionId = questionNumbers.get(currentIndex - 1);
            displayQuestion(currentQuestionType, prevQuestionId);
        } else {
            Toast.makeText(this, "已经是第一题了", Toast.LENGTH_SHORT).show();
        }
    }

    private void highlightLastReadQuestion() {
        if (currentBankId == null) return;

            QuestionDetailFragment.ReadingProgress progress =
            QuestionDetailFragment.loadReadingProgress(this, currentBankId);

        if (progress != null) {


            // 如果是通过对话框选择的题目，需要记录当前题目类型和ID
            currentQuestionType = progress.questionType;
            currentQuestionId = progress.questionId;
        }
    }

    private void loadLastReadingProgress() {
        if (currentBankId == null) return;

        QuestionDetailFragment.ReadingProgress progress =
                QuestionDetailFragment.loadReadingProgress(this, currentBankId);

        if (progress != null) {
            // 确保Fragment已初始化
            if (questionDetailFragment != null && questionDetailFragment.isAdded()) {
                displayQuestion(progress.questionType, progress.questionId);
            } else {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        displayQuestion(progress.questionType, progress.questionId);
                    }
                }, 150);
            }
        } else {
            // 加载默认题目也添加延迟
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    try {
                        List<Integer> questionNumbers = loadQuestionNumbers("SINGLE_CHOICE");
                        if (!questionNumbers.isEmpty()) {
                            displayQuestion("SINGLE_CHOICE", questionNumbers.get(0));
                        }
                    } catch (Exception e) {
                        Log.e("QuizHomeActivity", "加载默认题目失败", e);
                    }
                }
            }, 150);
        }
    }
    private void toggleNavigationDrawer() {
        if (quizDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            quizDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            quizDrawerLayout.openDrawer(GravityCompat.START);
        }
    }



    private void showQuestionSelectionDialog(String type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialog);
        builder.setTitle("选择题目");

        // 创建网格布局
        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 6));

        // 加载收藏的题目数据
        List<Integer> favoriteQuestions = new ArrayList<>();
        try {
            File infoFile = new File(getFilesDir(), "question_banks/" + currentBankId + "_info.json");
            if (infoFile.exists()) {
                String jsonStr = new String(Files.readAllBytes(infoFile.toPath()));
                JSONObject infoJson = new JSONObject(jsonStr);
                JSONObject favorites = infoJson.getJSONObject("favorites");
                String typeKey = getTypeKey(type);
                JSONArray favArray = favorites.optJSONArray(typeKey);
                if (favArray != null) {
                    for (int i = 0; i < favArray.length(); i++) {
                        favoriteQuestions.add(favArray.getInt(i));
                    }
                }
            }
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "加载收藏题目失败", e);
        }

        QuestionGridAdapter adapter = new QuestionGridAdapter(favoriteQuestions, type);

        // 如果是当前类型的题目，设置上次阅读的题目ID
        QuestionDetailFragment.ReadingProgress progress =
                QuestionDetailFragment.loadReadingProgress(this, currentBankId);
        if (progress != null && progress.questionType.equals(type)) {
            adapter.setLastReadQuestionId(progress.questionId);
        }

        recyclerView.setAdapter(adapter);
        // 设置当前显示的题目ID

        adapter.setOnItemClickListener((questionType, number) -> {
            displayQuestion(questionType, number);
            if (dialog != null) dialog.dismiss();
            quizDrawerLayout.closeDrawer(GravityCompat.START);

            // 更新当前题目类型和ID
            currentQuestionType = questionType;
            currentQuestionId = number;
        });

        builder.setView(recyclerView);
        builder.setNegativeButton("取消", null);

        dialog = builder.create();
        dialog.show();
    }
    public List<Integer> loadQuestionNumbers(String type) {
        List<Integer> numbers = new ArrayList<>();
        try {
            File bankFile = new File(getFilesDir(), "question_banks/" + currentBankId + ".json");
            if (!bankFile.exists()) return numbers;

            String jsonStr = new String(Files.readAllBytes(bankFile.toPath()));
            JSONObject bankJson = new JSONObject(jsonStr);
            JSONArray questions = bankJson.getJSONObject("tiku").getJSONArray(getTypeKey(type));

            for (int i = 0; i < questions.length(); i++) {
                numbers.add(questions.getJSONObject(i).getInt("id"));
            }
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "加载题目列表失败", e);
        }
        return numbers;
    }



    private void displayQuestion(String questionType, int questionNumber) {
        // 预加载下一题数据
        new Thread(() -> {
            List<Integer> questionNumbers = loadQuestionNumbers(questionType);
            int currentIndex = questionNumbers.indexOf(questionNumber);

            if (currentIndex < questionNumbers.size() - 1) {
                loadQuestionFromFile(currentBankId, questionType, questionNumbers.get(currentIndex + 1));
            }
        }).start();

        // 主线程显示当前题目
        runOnUiThread(() -> {
//            Log.e("QuizHomeActivity", questionType+questionNumber);
            Question question = loadQuestionFromFile(currentBankId, questionType, questionNumber);
//            Log.e("QuizHomeActivity", question.getContent());
            if (question != null && questionDetailFragment != null) {
                BankInfo bankInfo = bankInfoManager.getBankInfo(currentBankId);
                boolean isFavorite = bankInfo.getFavorites().get(getTypeKey(questionType)).contains(questionNumber);
                boolean isWrong = bankInfo.getWrongQuestions().get(getTypeKey(questionType)).contains(questionNumber);

                questionDetailFragment.displayQuestionDetails(question, isFavorite, isWrong, questionNumber > currentQuestionId);

                currentQuestionType = questionType;
                currentQuestionId = questionNumber;

                // 更新对话框中的选中状态（如果对话框正在显示）
                if (dialog != null && dialog.isShowing()) {
                    View dialogView = dialog.findViewById(R.id.single_choice_grid);
                    if (dialogView != null && questionType.equals("SINGLE_CHOICE")) {
                        ((QuestionGridAdapter) ((RecyclerView) dialogView).getAdapter()).setLastReadQuestionId(questionNumber);
                    } else {
                        dialogView = dialog.findViewById(R.id.multi_choice_grid);
                        if (dialogView != null && questionType.equals("MULTI_CHOICE")) {
                            ((QuestionGridAdapter) ((RecyclerView) dialogView).getAdapter()).setLastReadQuestionId(questionNumber);
                        } else {
                            dialogView = dialog.findViewById(R.id.true_false_grid);
                            if (dialogView != null && questionType.equals("TRUE_FALSE")) {
                                ((QuestionGridAdapter) ((RecyclerView) dialogView).getAdapter()).setLastReadQuestionId(questionNumber);
                            }
                        }
                    }
                }
            }
        });
    }

    private Question loadQuestionFromFile(String bankId, String type, int number) {
        File bankFile = new File(getFilesDir(), "question_banks/" + bankId + ".json");
        if (!bankFile.exists()) {
            Log.e("QuizHomeActivity", "题库文件不存在: " + bankId);
            return null;
        }

        try {
            String jsonStr = new String(Files.readAllBytes(bankFile.toPath()));
            JSONObject bankJson = new JSONObject(jsonStr);
            JSONObject tiku = bankJson.getJSONObject("tiku");

            String typeKey = getTypeKey(type);
            JSONArray questions = tiku.getJSONArray(typeKey);

            for (int i = 0; i < questions.length(); i++) {
                JSONObject q = questions.getJSONObject(i);
                if (q.getInt("id") == number) {
                    String content = q.getString("stem");
                    String answer = q.getString("answer");
                    String options = "";

                    if (!type.equals("TRUE_FALSE")) {
                        StringBuilder sb = new StringBuilder();
                        char optionChar = 'A';
                        for (int j = 0; j < 6; j++) {
                            String opKey = "op" + (char)('a' + j);
                            if (q.has(opKey) && !q.getString(opKey).isEmpty()) {
                                sb.append(optionChar++).append(". ").append(q.getString(opKey)).append("\n");
                            }
                        }
                        options = sb.toString().trim();
                    }
                    return new Question(
                            bankId,
                            Question.Type.valueOf(type),
                            number,
                            content,
                            options,
                            answer
                    );
                }
            }
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "加载题目失败", e);
        }
        return null;
    }

    private String getTypeKey(String questionType) {
        switch (questionType) {
            case "SINGLE_CHOICE": return "danxuan";
            case "MULTI_CHOICE": return "duoxuan";
            case "TRUE_FALSE": return "panduan";
            default: return "";
        }
    }

    // 添加到收藏
    public void addToFavorite(String questionType, int questionNumber) {
        updateFavoriteInInfoFile(questionType, questionNumber, true);
    }

    // 从收藏移除
    public void removeFavorite(String questionType, int questionNumber) {
        updateFavoriteInInfoFile(questionType, questionNumber, false);
    }

    private void updateFavoriteInInfoFile(String questionType, int questionNumber, boolean add) {
        try {
            File infoFile = new File(getFilesDir(), "question_banks/" + currentBankId + "_info.json");
            JSONObject infoJson;

            if (infoFile.exists()) {
                String jsonStr = new String(Files.readAllBytes(infoFile.toPath()));
                infoJson = new JSONObject(jsonStr);
            } else {
                infoJson = new JSONObject();
                infoJson.put("bankId", currentBankId);
                infoJson.put("favorites", new JSONObject());
                infoJson.put("wrongQuestions", new JSONObject());
                infoJson.put("generatedPapers", new JSONArray());
            }

            JSONObject favorites = infoJson.getJSONObject("favorites");
            String typeKey = getTypeKey(questionType);
            JSONArray favArray = favorites.optJSONArray(typeKey);

            if (favArray == null) {
                favArray = new JSONArray();
                favorites.put(typeKey, favArray);
            }

            // Convert JSONArray to List for easier manipulation
            List<Integer> favList = new ArrayList<>();
            for (int i = 0; i < favArray.length(); i++) {
                favList.add(favArray.getInt(i));
            }

            if (add) {
                if (!favList.contains(questionNumber)) {
                    favList.add(questionNumber);
                }
            } else {
                favList.remove(Integer.valueOf(questionNumber));
            }

            // Update the JSONArray
            favorites.put(typeKey, new JSONArray(favList));

            // Write back to file
            Files.write(infoFile.toPath(), infoJson.toString().getBytes());

        } catch (Exception e) {
            Log.e("QuizHomeActivity", "更新收藏失败", e);
        }
    }

}