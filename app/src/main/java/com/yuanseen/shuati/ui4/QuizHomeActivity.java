package com.yuanseen.shuati.ui4;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class QuizHomeActivity extends AppCompatActivity implements QuestionDetailFragment.OnSwipeListener {
    private DrawerLayout quizDrawerLayout;
    private NavigationView questionNavigationView;
    private FloatingActionButton navigationToggleFab;
    private QuestionDetailFragment questionDetailFragment;
    private BankInfoManager bankInfoManager;
    private String currentBankId ; // 当前题库ID
    private AlertDialog dialog; // Add this with other member variables

    private List<String> selectedQuestionTypes = new ArrayList<>(); // 存储用户选择的题型
    private String currentQuestionType;
    private int currentQuestionId;

    private boolean isProcessingSwipe = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_home4);
        // 获取传入的题库ID
        // 获取传入的题库ID和选择的题型
        currentBankId = getIntent().getStringExtra("question_bank_id");
        selectedQuestionTypes = getIntent().getStringArrayListExtra("selected_types"); // 修改这里
        if (selectedQuestionTypes == null) {
            selectedQuestionTypes = new ArrayList<>();
            Toast.makeText(this, "未选择任何题型", Toast.LENGTH_SHORT).show();
        }
        // 初始化视图
        quizDrawerLayout = findViewById(R.id.quiz_drawer_layout);
//        questionNavigationView = findViewById(R.id.question_navigation_view);
//        navigationToggleFab = findViewById(R.id.navigation_toggle_fab);

        // 初始化BankInfoManager
        bankInfoManager = new BankInfoManager(this);

        // 初始化Fragment
        questionDetailFragment = (QuestionDetailFragment) getSupportFragmentManager()
                .findFragmentById(R.id.question_detail_fragment);

//        // 获取传入的题库ID和选择的题型
//        currentBankId = getIntent().getStringExtra("question_bank_id");
//        String[] types = getIntent().getStringArrayExtra("selected_types");
//        if (types != null) {
//            selectedQuestionTypes = Arrays.asList(types);
//        }
        // 设置FAB点击事件
//        navigationToggleFab.setOnClickListener(view -> toggleNavigationDrawer());

        // 初始化题目导航菜单

//        // 设置菜单项点击事件
//        questionNavigationView.setNavigationItemSelectedListener(item -> {
//            handleNavigationItemSelection(item);
//            return true;
//        });

        // 初始化Fragment后设置滑动监听
        questionDetailFragment = (QuestionDetailFragment) getSupportFragmentManager()
                .findFragmentById(R.id.question_detail_fragment);
        if (questionDetailFragment != null) {
            questionDetailFragment.setOnSwipeListener(this);
        }
        // 延迟加载题目，确保Fragment已附加
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                loadLastReadingProgress();
//                highlightLastReadQuestion();
            }
        }, 100); // 延迟100ms确保Fragment就绪
    }
    @Override
    public void onSwipeLeft() {
        if (isProcessingSwipe) return;
        isProcessingSwipe = true;

        runOnUiThread(() -> {
            // 先确保连对数据已更新
            if (questionDetailFragment != null) {
                questionDetailFragment.updateStreakDisplay();
            }

            // 然后加载新题目
            startRandomQuiz();
            isProcessingSwipe = false;
        });
    }

    @Override
    public void onSwipeRight() { // 返回上一题
        if (isProcessingSwipe) return;

        runOnUiThread(() -> {
            Toast.makeText(this, "随机模式不支持回到上一题", Toast.LENGTH_SHORT).show();
//            isProcessingSwipe = true;
//
//            List<Integer> questionNumbers = loadQuestionNumbers(currentQuestionType);
//            int currentIndex = questionNumbers.indexOf(currentQuestionId);
//
//            if (currentIndex > 0) {
//                int prevId = questionNumbers.get(currentIndex - 1);
//
//                // 先更新当前题目ID
//                currentQuestionId = prevId;
//
//                // 直接显示题目，不等待动画完成
//                Question question = loadQuestionFromFile(currentBankId, currentQuestionType, prevId);
//                BankInfo bankInfo = bankInfoManager.getBankInfo(currentBankId);
//                boolean isFavorite = bankInfo.getFavorites().get(getTypeKey(currentQuestionType)).contains(prevId);
//                boolean isWrong = bankInfo.getWrongQuestions().get(getTypeKey(currentQuestionType)).contains(prevId);
//
//                questionDetailFragment.displayQuestionDetails(question, isFavorite, isWrong, false);
//            } else {
//                tryLoadNextQuestionType(false);
//            }
//
//            isProcessingSwipe = false;
        });
    }

    // 随机选择一种题型
    private String getRandomQuestionType() {
        if (selectedQuestionTypes.isEmpty()) {
            return null;
        }
        Random random = new Random();
        return selectedQuestionTypes.get(random.nextInt(selectedQuestionTypes.size()));
    }

    // 随机选择一道题目
    private int getRandomQuestionNumber(String type) {
        List<Integer> questionNumbers = loadQuestionNumbers(type);
        if (questionNumbers.isEmpty()) {
            return -1;
        }
        Random random = new Random();
        return questionNumbers.get(random.nextInt(questionNumbers.size()));
    }

    // 开始随机抽题
    private void startRandomQuiz() {
        String type = getRandomQuestionType();
        if (type == null) {
            Toast.makeText(this, "没有可用的题型", Toast.LENGTH_SHORT).show();
            return;
        }

        int questionNumber = getRandomQuestionNumber(type);
        if (questionNumber == -1) {
            Toast.makeText(this, "没有可用的题目", Toast.LENGTH_SHORT).show();
            return;
        }

        displayQuestion(type, questionNumber);
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
//    private void tryLoadNextQuestionType(boolean forward) {
//        String nextType = getNextQuestionType(currentQuestionType, forward);
//        if (nextType != null) {
//            List<Integer> questionNumbers = loadQuestionNumbers(nextType);
//            if (!questionNumbers.isEmpty()) {
//                // 如果是向前滑动，加载新题型第一题；向后则加载最后一题
//                int targetId = forward ? questionNumbers.get(0) : questionNumbers.get(questionNumbers.size() - 1);
//                animateAndDisplayQuestion(nextType, targetId, forward);
//
//                // 更新导航菜单高亮
////                highlightNavigationMenuItem(nextType);
//                return;
//            }
//        }
//        Toast.makeText(this, forward ? "已经是最后一题了" : "已经是第一题了", Toast.LENGTH_SHORT).show();
//    }

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

//    private void highlightLastReadQuestion() {
//        if (currentBankId == null) return;
//
//            QuestionDetailFragment.ReadingProgress progress =
//            QuestionDetailFragment.loadReadingProgress(this, currentBankId);
//
//        if (progress != null) {
//            // 高亮导航菜单中的对应项
//            highlightNavigationMenuItem(progress.questionType);
//
//            // 如果是通过对话框选择的题目，需要记录当前题目类型和ID
//            currentQuestionType = progress.questionType;
//            currentQuestionId = progress.questionId;
//        }
//    }

//    private void highlightNavigationMenuItem(String questionType) {
//        Menu menu = questionNavigationView.getMenu();
//        int itemId = -1;
//
//        switch (questionType) {
//            case "SINGLE_CHOICE":
//                itemId = R.id.menu_single_choice;
//                break;
//            case "MULTI_CHOICE":
//                itemId = R.id.menu_multi_choice;
//                break;
//            case "TRUE_FALSE":
//                itemId = R.id.menu_true_false;
//                break;
//        }
//
//        if (itemId != -1) {
//            for (int i = 0; i < menu.size(); i++) {
//                MenuItem item = menu.getItem(i);
//                item.setChecked(item.getItemId() == itemId);
//            }
//        }
//    }
    private void loadLastReadingProgress() {
        // 不再读取历史进度，直接开始随机抽题
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                startRandomQuiz(); // 直接调用随机抽题方法
            }
        }, 150); // 延迟150ms确保Fragment初始化完成
    }
//    private void toggleNavigationDrawer() {
//        if (quizDrawerLayout.isDrawerOpen(GravityCompat.START)) {
//            quizDrawerLayout.closeDrawer(GravityCompat.START);
//        } else {
//            quizDrawerLayout.openDrawer(GravityCompat.START);
//        }
//    }

//    private void setupQuestionNavigationMenu() {
//        Menu menu = questionNavigationView.getMenu();
//        menu.clear();
//        getMenuInflater().inflate(R.menu.menu_question_categories, menu);
//
//        // 设置简洁的导航视图
//        questionNavigationView.setItemIconTintList(null); // 显示原始图标颜色
//        questionNavigationView.setItemTextAppearance(R.style.NavigationMenuTextStyle);
//
//        // 为每个菜单项添加点击事件
//        for (int i = 0; i < menu.size(); i++) {
//            MenuItem item = menu.getItem(i);
//            item.setActionView(null); // 移除原有复杂布局
//
//            // 添加简单的点击处理
//            item.setOnMenuItemClickListener(menuItem -> {
//                handleMenuItemClick(menuItem);
//                return true;
//            });
//        }
//    }
//
//    private void handleMenuItemClick(MenuItem menuItem) {
//        // 根据菜单项ID加载不同类型的题目
//        String type = "";
//        int itemId = menuItem.getItemId();
//        if (itemId == R.id.menu_single_choice) {
//            type = "SINGLE_CHOICE";
//        } else if (itemId == R.id.menu_multi_choice) {
//            type = "MULTI_CHOICE";
//        } else if (itemId == R.id.menu_true_false) {
//            type = "TRUE_FALSE";
//        }
//        showQuestionSelectionDialog(type);
//    }
//
//    private void showQuestionSelectionDialog(String type) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialog);
//        builder.setTitle("选择题目");
//
//        // 创建网格布局
//        RecyclerView recyclerView = new RecyclerView(this);
//        recyclerView.setLayoutManager(new GridLayoutManager(this, 6));
//
//        // 加载题目数据
//        List<Integer> questionNumbers = loadQuestionNumbers(type);
//        QuestionGridAdapter adapter = new QuestionGridAdapter(questionNumbers, type);
//
//        // 如果是当前类型的题目，设置上次阅读的题目ID
//        QuestionDetailFragment.ReadingProgress progress =
//                QuestionDetailFragment.loadReadingProgress(this, currentBankId);
//        if (progress != null && progress.questionType.equals(type)) {
//            adapter.setLastReadQuestionId(progress.questionId);
//        }
//
//        recyclerView.setAdapter(adapter);
//
//        adapter.setOnItemClickListener((questionType, number) -> {
//            displayQuestion(questionType, number);
//            if (dialog != null) dialog.dismiss();
//            quizDrawerLayout.closeDrawer(GravityCompat.START);
//
//            // 更新当前题目类型和ID
//            currentQuestionType = questionType;
//            currentQuestionId = number;
//        });
//
//        builder.setView(recyclerView);
//        builder.setNegativeButton("取消", null);
//
//        dialog = builder.create();
//        dialog.show();
//    }
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

    /**
     * 检查是否选择了全部三种题型
     */
    public boolean areAllTypesSelected() {
        return selectedQuestionTypes != null &&
                selectedQuestionTypes.size() == 3 && // 三种题型全选
                selectedQuestionTypes.contains("SINGLE_CHOICE") &&
                selectedQuestionTypes.contains("MULTI_CHOICE") &&
                selectedQuestionTypes.contains("TRUE_FALSE");
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
            Question question = loadQuestionFromFile(currentBankId, questionType, questionNumber);
            if (question != null && questionDetailFragment != null) {
                BankInfo bankInfo = bankInfoManager.getBankInfo(currentBankId);
                boolean isFavorite = bankInfo.getFavorites().get(getTypeKey(questionType)).contains(questionNumber);
                boolean isWrong = bankInfo.getWrongQuestions().get(getTypeKey(questionType)).contains(questionNumber);

                questionDetailFragment.displayQuestionDetails(question, isFavorite, isWrong, questionNumber > currentQuestionId);

                currentQuestionType = questionType;
                currentQuestionId = questionNumber;
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
        bankInfoManager.addFavorite(currentBankId, getTypeKey(questionType), questionNumber);
    }

    // 从收藏移除
    public void removeFavorite(String questionType, int questionNumber) {
        bankInfoManager.removeFavorite(currentBankId, getTypeKey(questionType), questionNumber);
    }

}