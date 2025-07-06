package com.yuanseen.shuati.ui5;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
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
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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

    public String currentPaperId;
    private int examScore = 0;
    private int totalExamScore = 0;

    private boolean isProcessingSwipe = false;
    // 在成员变量部分添加
    private Handler timeCheckHandler = new Handler(Looper.getMainLooper());
    private Runnable timeCheckRunnable;
    private static final long TIME_CHECK_INTERVAL = 1000; // 1秒检查一次

    private WeakReference<QuizHomeActivity> activityWeakReference;
    private boolean isDestroyed = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_home5);
        // 获取传入的参数
        currentBankId = getIntent().getStringExtra("question_bank_id");
        currentPaperId = getIntent().getStringExtra("paper_id");

        initExamData();
        activityWeakReference = new WeakReference<>(this);
        initTimeCheck();
        // 初始化视图
        quizDrawerLayout = findViewById(R.id.quiz_drawer_layout);

        navigationToggleFab = findViewById(R.id.navigation_toggle_fab);

        // 初始化BankInfoManager
        bankInfoManager = new BankInfoManager(this);

        // 设置FAB点击事件
        navigationToggleFab.setOnClickListener(view -> showQuestionTypeDialog());


        // 初始化Fragment后设置滑动监听
        questionDetailFragment = (QuestionDetailFragment) getSupportFragmentManager()
                .findFragmentById(R.id.question_detail_fragment5);
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
    private void initTimeCheck() {
        timeCheckRunnable = new Runnable() {
            @Override
            public void run() {
                QuizHomeActivity activity = activityWeakReference.get();
                if (activity != null && !activity.isDestroyed) {
                    activity.checkExamTime();
                    timeCheckHandler.postDelayed(this, TIME_CHECK_INTERVAL);
                }
            }
        };
        timeCheckHandler.postDelayed(timeCheckRunnable, TIME_CHECK_INTERVAL);
    }
    private void checkExamTime() {
        if (isDestroyed || isFinishing()) {
            return;
        }
        try {
            File paperFile = new File(getFilesDir(), "question_banks/" + currentPaperId + ".json");
            if (!paperFile.exists()) return;

            String jsonStr = new String(Files.readAllBytes(paperFile.toPath()));
            JSONObject paperJson = new JSONObject(jsonStr);

            // 如果考试已经结束，直接返回
            if (paperJson.optBoolean("shifoukaoshijieshu", false)) {
                timeCheckHandler.removeCallbacks(timeCheckRunnable);
                return;
            }

            // 检查考试结束时间
            String endTimeStr = paperJson.getString("zuichijiezhishijian");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.getDefault());
            Date endTime = sdf.parse(endTimeStr);
            Date currentTime = new Date();

            if (currentTime.after(endTime)) {
                // 考试时间结束，自动交卷
                runOnUiThread(() -> {
                    showAutoSubmitDialog();
                });
            }
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "检查考试时间失败", e);
        }
    }

    private void showAutoSubmitDialog() {
        if (isDestroyed || isFinishing()) {
            return;
        }
        // 停止时间检查
        timeCheckHandler.removeCallbacks(timeCheckRunnable);

        // 显示自动交卷对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialog);
        builder.setTitle("考试时间结束");
        builder.setMessage("考试时间已到，系统将自动交卷");
        builder.setCancelable(false);
        builder.setPositiveButton("确定", (dialog, which) -> {
            // 调用交卷逻辑
            submitExamAutomatically();
        });
        builder.show();
    }


    public void submitExamAutomatically() {
        // 标记考试结束
        MidActivity.finishExamPaper(getFilesDir(), currentPaperId);

        // 计算分数
        int score = MidActivity.calculateExamScore(getFilesDir(), currentPaperId);

        // 显示考试结果
        showExamResult(score);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyed = true;

        // 移除定时检查
        if (timeCheckHandler != null && timeCheckRunnable != null) {
            timeCheckHandler.removeCallbacks(timeCheckRunnable);
        }

        // 如果有对话框正在显示，尝试关闭它
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        dialog = null;
    }
    public int getTotalQuestionsInPaper() {
        try {
            File paperFile = new File(getFilesDir(), "question_banks/" + currentPaperId + ".json");
            if (!paperFile.exists()) return 0;

            String jsonStr = new String(Files.readAllBytes(paperFile.toPath()));
            JSONObject paperJson = new JSONObject(jsonStr);
            JSONObject timu = paperJson.getJSONObject("timu");

            int total = 0;
            total += timu.getJSONArray("danxuan").length(); // 单选题数量
            total += timu.getJSONArray("duoxuan").length(); // 多选题数量
            total += timu.getJSONArray("panduan").length(); // 判断题数量

            return total;
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "获取试卷题目总数失败", e);
            return 0;
        }
    }

    public int getQuestionCountInPaper(String questionType) {
        try {
            File paperFile = new File(getFilesDir(), "question_banks/" + currentPaperId + ".json");
            if (!paperFile.exists()) return 0;

            String jsonStr = new String(Files.readAllBytes(paperFile.toPath()));
            JSONObject paperJson = new JSONObject(jsonStr);
            JSONObject yushelianxi = paperJson.getJSONObject("yushelianxi");

            switch (questionType) {
                case "SINGLE_CHOICE":
                    return yushelianxi.getJSONArray("danxuan").getInt(0);
                case "MULTI_CHOICE":
                    return yushelianxi.getJSONArray("duoxuan").getInt(0);
                case "TRUE_FALSE":
                    return yushelianxi.getJSONArray("panduan").getInt(0);
                default:
                    return 0;
            }
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "获取题目数量失败", e);
            return 0;
        }
    }
    // 添加新方法获取已保存的答案
    public String getSavedAnswerFromPaper(String questionType, int questionNumber) {
        try {
            File paperFile = new File(getFilesDir(), "question_banks/" + currentPaperId + ".json");
            if (!paperFile.exists()) return null;

            String jsonStr = new String(Files.readAllBytes(paperFile.toPath()));
            JSONObject paperJson = new JSONObject(jsonStr);
            JSONObject timu = paperJson.getJSONObject("timu");
            String typeKey = getTypeKey(questionType);
            JSONArray questions = timu.getJSONArray(typeKey);

            for (int i = 0; i < questions.length(); i++) {
                JSONObject question = questions.getJSONObject(i);
                if (question.getInt("id") == questionNumber) {
                    // 对于多选题，确保答案格式正确（如"AB"而不是"A,B"）
                    String answer = question.optString("yourans", null);
                    if (answer != null && questionType.equals("MULTI_CHOICE")) {
                        return answer.replace(",", "").replace(" ", "");
                    }
                    return answer;
                }
            }
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "获取已保存答案失败", e);
        }
        return null;
    }

    // 添加新方法保存答案到试卷
    public void saveAnswerToPaper(String questionType, int questionNumber, String answer) {
        try {
            File paperFile = new File(getFilesDir(), "question_banks/" + currentPaperId + ".json");
            if (!paperFile.exists()) return;

            String jsonStr = new String(Files.readAllBytes(paperFile.toPath()));
            JSONObject paperJson = new JSONObject(jsonStr);
            JSONObject timu = paperJson.getJSONObject("timu");
            String typeKey = getTypeKey(questionType);
            JSONArray questions = timu.getJSONArray(typeKey);

            for (int i = 0; i < questions.length(); i++) {
                JSONObject question = questions.getJSONObject(i);
                if (question.getInt("id") == questionNumber) {
                    // 对于多选题，确保答案格式一致
                    if (questionType.equals("MULTI_CHOICE")) {
                        answer = sortAnswer(answer); // 排序并格式化
                    }
                    question.put("yourans", answer);

                    // 更新答题状态
                    boolean isCorrect = checkAnswerCorrectness(questionType, questionNumber, answer);
                    question.put("duicuo",isCorrect);
                    question.put("iscorr", true);

                    break;
                }
            }

            // 写回文件
            Files.write(paperFile.toPath(), paperJson.toString().getBytes());
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "保存答案失败", e);
        }
    }

    private boolean checkAnswerCorrectness(String questionType, int questionNumber, String userAnswer) {
        try {
            // 从题库中获取正确答案
            Question question = loadQuestionFromFile(currentBankId, questionType, questionNumber);
            if (question == null) return false;

            String correctAnswer = question.getAnswer();

            if (questionType.equals("MULTI_CHOICE")) {
                // 多选题需要特殊处理
                String sortedUserAnswer = sortAnswer(userAnswer);

                String sortedCorrectAnswer = sortAnswer(correctAnswer);

                return sortedUserAnswer.equals(sortedCorrectAnswer);
            }
            return userAnswer.equals(correctAnswer);
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "检查答案正确性失败", e);
            return false;
        }
    }

    private String sortAnswer(String answer) {
        if (answer == null) return "";
        char[] chars = answer.toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }
    private void loadFirstQuestionFromPaper() {
        try {
            File paperFile = new File(getFilesDir(), "question_banks/" + currentPaperId + ".json");
            if (!paperFile.exists()) {
                Toast.makeText(this, "试卷文件不存在", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String jsonStr = new String(Files.readAllBytes(paperFile.toPath()));
            JSONObject paperJson = new JSONObject(jsonStr);
            JSONObject timu = paperJson.getJSONObject("timu");

            // 1. 首先尝试加载单选题
            if (timu.getJSONArray("danxuan").length() > 0) {
                JSONObject firstQuestion = timu.getJSONArray("danxuan").getJSONObject(0);
                displayQuestion("SINGLE_CHOICE", firstQuestion.getInt("id"));
                return;
            }

            // 2. 如果没有单选题，尝试多选题
            if (timu.getJSONArray("duoxuan").length() > 0) {
                JSONObject firstQuestion = timu.getJSONArray("duoxuan").getJSONObject(0);
                displayQuestion("MULTI_CHOICE", firstQuestion.getInt("id"));
                return;
            }

            // 3. 最后尝试判断题
            if (timu.getJSONArray("panduan").length() > 0) {
                JSONObject firstQuestion = timu.getJSONArray("panduan").getJSONObject(0);
                displayQuestion("TRUE_FALSE", firstQuestion.getInt("id"));
                return;
            }

            Toast.makeText(this, "试卷中没有题目", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "加载试卷题目失败", e);
            Toast.makeText(this, "加载试卷失败", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
        // Add this method to QuizHomeActivity
// 修改后的方法，从试卷文件中读取题目
        private Map<String, List<Integer>> loadFavoritesFromInfoFile() {
            Map<String, List<Integer>> favoritesMap = new HashMap<>();
            try {
                File paperFile = new File(getFilesDir(), "question_banks/" + currentPaperId + ".json");
                if (paperFile.exists()) {
                    String jsonStr = new String(Files.readAllBytes(paperFile.toPath()));
                    JSONObject paperJson = new JSONObject(jsonStr);
                    JSONObject timu = paperJson.getJSONObject("timu");

                    // 从试卷中获取单选题列表
                    JSONArray danxuanQuestions = timu.getJSONArray("danxuan");
                    List<Integer> danxuanList = new ArrayList<>();
                    for (int i = 0; i < danxuanQuestions.length(); i++) {
                        danxuanList.add(danxuanQuestions.getJSONObject(i).getInt("id"));
                    }
                    favoritesMap.put("danxuan", danxuanList);

                    // 从试卷中获取多选题列表
                    JSONArray duoxuanQuestions = timu.getJSONArray("duoxuan");
                    List<Integer> duoxuanList = new ArrayList<>();
                    for (int i = 0; i < duoxuanQuestions.length(); i++) {
                        duoxuanList.add(duoxuanQuestions.getJSONObject(i).getInt("id"));
                    }
                    favoritesMap.put("duoxuan", duoxuanList);

                    // 从试卷中获取判断题列表
                    JSONArray panduanQuestions = timu.getJSONArray("panduan");
                    List<Integer> panduanList = new ArrayList<>();
                    for (int i = 0; i < panduanQuestions.length(); i++) {
                        panduanList.add(panduanQuestions.getJSONObject(i).getInt("id"));
                    }
                    favoritesMap.put("panduan", panduanList);
                }
            } catch (Exception e) {
                Log.e("QuizHomeActivity", "加载试卷题目失败", e);
            }
            return favoritesMap;
        }



    private void initExamData() {
        try {
            File paperFile = new File(getFilesDir(), "question_banks/" + currentPaperId + ".json");
            if (!paperFile.exists()) return;

            String jsonStr = new String(Files.readAllBytes(paperFile.toPath()));
            JSONObject paperJson = new JSONObject(jsonStr);
            JSONObject examConfig = paperJson.getJSONObject("yushelianxi");
            totalExamScore = examConfig.getJSONArray("danxuan").getInt(0) * examConfig.getJSONArray("danxuan").getInt(1)
                    + examConfig.getJSONArray("duoxuan").getInt(0) * examConfig.getJSONArray("duoxuan").getInt(1)
                    + examConfig.getJSONArray("panduan").getInt(0) * examConfig.getJSONArray("panduan").getInt(1);
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "初始化考试数据失败", e);
        }
    }



    private void updateExamPaperQuestionStatus(String questionType, int questionId, boolean isCorrect) {
        try {
            File paperFile = new File(getFilesDir(), "question_banks/" + currentPaperId + ".json");
            if (!paperFile.exists()) return;

            String jsonStr = new String(Files.readAllBytes(paperFile.toPath()));
            JSONObject paperJson = new JSONObject(jsonStr);
            JSONObject timu = paperJson.getJSONObject("timu");

            String typeKey = getTypeKey(questionType);
            JSONArray questions = timu.getJSONArray(typeKey);

            for (int i = 0; i < questions.length(); i++) {
                JSONObject question = questions.getJSONObject(i);
                if (question.getInt("id") == questionId) {
                    question.put("iscorr", isCorrect);
                    break;
                }
            }

            // 写回文件
            Files.write(paperFile.toPath(), paperJson.toString().getBytes());
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "更新试卷题目状态失败", e);
        }
    }

    private int getQuestionScore(String questionType) {
        try {
            File paperFile = new File(getFilesDir(), "question_banks/" + currentPaperId + ".json");
            if (!paperFile.exists()) return 0;

            String jsonStr = new String(Files.readAllBytes(paperFile.toPath()));
            JSONObject paperJson = new JSONObject(jsonStr);
            JSONObject examConfig = paperJson.getJSONObject("yushelianxi");

            switch (questionType) {
                case "SINGLE_CHOICE":
                    return examConfig.getJSONArray("danxuan").getInt(1);
                case "MULTI_CHOICE":
                    return examConfig.getJSONArray("duoxuan").getInt(1);
                case "TRUE_FALSE":
                    return examConfig.getJSONArray("panduan").getInt(1);
                default:
                    return 0;
            }
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "获取题目分数失败", e);
            return 0;
        }
    }

    private boolean isAllQuestionsAnswered() {
        try {
            File paperFile = new File(getFilesDir(), "question_banks/" + currentPaperId + ".json");
            if (!paperFile.exists()) return false;

            String jsonStr = new String(Files.readAllBytes(paperFile.toPath()));
            JSONObject paperJson = new JSONObject(jsonStr);
            JSONObject timu = paperJson.getJSONObject("timu");

            // 检查单选题
            JSONArray danxuanQuestions = timu.getJSONArray("danxuan");
            for (int i = 0; i < danxuanQuestions.length(); i++) {
                if (!danxuanQuestions.getJSONObject(i).has("iscorr")) {
                    return false;
                }
            }

            // 检查多选题
            JSONArray duoxuanQuestions = timu.getJSONArray("duoxuan");
            for (int i = 0; i < duoxuanQuestions.length(); i++) {
                if (!duoxuanQuestions.getJSONObject(i).has("iscorr")) {
                    return false;
                }
            }

            // 检查判断题
            JSONArray panduanQuestions = timu.getJSONArray("panduan");
            for (int i = 0; i < panduanQuestions.length(); i++) {
                if (!panduanQuestions.getJSONObject(i).has("iscorr")) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "检查题目完成状态失败", e);
            return false;
        }
    }

    private void finishExam() {
        try {
            File paperFile = new File(getFilesDir(), "question_banks/" + currentPaperId + ".json");
            if (!paperFile.exists()) return;

            String jsonStr = new String(Files.readAllBytes(paperFile.toPath()));
            JSONObject paperJson = new JSONObject(jsonStr);
            paperJson.put("shifoukaoshijieshu", true);
            paperJson.put("defen", examScore);
            double correctRate = (double) examScore / totalExamScore * 100;
            Files.write(paperFile.toPath(), paperJson.toString().getBytes());
            showExamResult(examScore, totalExamScore, correctRate);
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "完成考试失败", e);
        }
    }

    private void showExamResult(int score, int totalScore, double correctRate) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialog);
            builder.setTitle("考试结果");

            String message = String.format("得分: %d/%d (%.1f%%)", score, totalScore, correctRate);
            builder.setMessage(message);

            builder.setPositiveButton("确定", (dialog, which) -> {
                // 返回题库选择界面
                finish();
            });

            builder.setCancelable(false);
            builder.show();
        });
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

    // 原有方法（保持不变）
    private void setupQuestionGrid(RecyclerView recyclerView, String questionType) {
        // 从bankid_info.json中获取收藏的题目
        List<Integer> favoriteQuestions = getFavoriteQuestions(questionType);

        setupQuestionGrid(recyclerView, questionType, favoriteQuestions); // 调用新方法，传入null表示使用默认题目列表
    }

    // 新方法（添加自定义题目列表参数）
    private void setupQuestionGrid(RecyclerView recyclerView, String questionType, List<Integer> customQuestions) {
        recyclerView.setLayoutManager(new GridLayoutManager(this, 6));

        QuestionGridAdapter adapter = new QuestionGridAdapter(customQuestions, questionType);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener((type, displayNumber) -> {
            // 将显示编号转换为原始题目ID
            int originalId = getOriginalQuestionId(type, displayNumber);
            if (originalId > 0) {
                displayQuestion(type, originalId);
                if (dialog != null) dialog.dismiss();
            }
        });
    }

    private List<Integer> getFavoriteQuestions(String questionType) {
        List<Integer> questions = new ArrayList<>();
        try {
            File paperFile = new File(getFilesDir(), "question_banks/" + currentPaperId + ".json");
            if (paperFile.exists()) {
                String jsonStr = new String(Files.readAllBytes(paperFile.toPath()));
                JSONObject paperJson = new JSONObject(jsonStr);
                JSONObject timu = paperJson.getJSONObject("timu");

                String typeKey = getTypeKey(questionType);
                JSONArray questionArray = timu.getJSONArray(typeKey);

                // 计算起始编号
                int startNumber = 1;
                if ("duoxuan".equals(typeKey)) {
                    startNumber += timu.getJSONArray("danxuan").length();
                } else if ("panduan".equals(typeKey)) {
                    startNumber += timu.getJSONArray("danxuan").length() +
                            timu.getJSONArray("duoxuan").length();
                }

                // 按顺序添加编号
                for (int i = 0; i < questionArray.length(); i++) {
                    questions.add(startNumber + i);
                }
            }
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "加载" + questionType + "题目失败", e);
        }
        return questions;
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

        // 获取当前题型的所有题目ID
        List<Integer> questionNumbers = loadQuestionNumbers(currentQuestionType);
        if (questionNumbers.isEmpty()) {
            isProcessingSwipe = false;
            return;
        }

        // 获取当前题目在题型列表中的位置
        int currentIndex = questionNumbers.indexOf(currentQuestionId);
        if (currentIndex < questionNumbers.size() - 1) {
            // 同题型还有下一题
            int nextQuestionId = questionNumbers.get(currentIndex + 1);
            displayQuestion(currentQuestionType, nextQuestionId);

            // 计算新位置并更新进度
            int newPosition = calculateNewPosition(currentQuestionType, currentIndex + 1, true);
            if (questionDetailFragment != null) {
                questionDetailFragment.updateProgress(newPosition);
            }
        } else {
            // 尝试加载下一题型的第一题
            if (!tryLoadNextQuestionType(true)) {
                // 已经是最后一道题了，检查未答题目
                checkUnansweredQuestionsBeforeSubmit();
            }
        }

        isProcessingSwipe = false;
    }

    private void checkUnansweredQuestionsBeforeSubmit() {
        List<String> unansweredQuestions = getUnansweredQuestions();
        if (unansweredQuestions.isEmpty()) {
            // 没有未答题目，直接询问是否交卷
            showSubmitExamDialog(null);
        } else {
            // 显示未答题目列表
            showUnansweredQuestionsDialog(unansweredQuestions);
        }
    }

    private List<String> getUnansweredQuestions() {
        List<String> unanswered = new ArrayList<>();
        try {
            File paperFile = new File(getFilesDir(), "question_banks/" + currentPaperId + ".json");
            if (!paperFile.exists()) return unanswered;

            String jsonStr = new String(Files.readAllBytes(paperFile.toPath()));
            JSONObject paperJson = new JSONObject(jsonStr);
            JSONObject timu = paperJson.getJSONObject("timu");

            // 检查单选题
            JSONArray danxuanQuestions = timu.getJSONArray("danxuan");
            for (int i = 0; i < danxuanQuestions.length(); i++) {
                JSONObject question = danxuanQuestions.getJSONObject(i);
                if (question.isNull("yourans") || !question.has("iscorr") || !question.getBoolean("iscorr")) {
                    unanswered.add("单选题 " + question.getInt("id"));
                }
            }

            // 检查多选题
            JSONArray duoxuanQuestions = timu.getJSONArray("duoxuan");
            for (int i = 0; i < duoxuanQuestions.length(); i++) {
                JSONObject question = duoxuanQuestions.getJSONObject(i);
                if (question.isNull("yourans") || !question.has("iscorr") || !question.getBoolean("iscorr")) {
                    unanswered.add("多选题 " + question.getInt("id"));
                }
            }

            // 检查判断题
            JSONArray panduanQuestions = timu.getJSONArray("panduan");
            for (int i = 0; i < panduanQuestions.length(); i++) {
                JSONObject question = panduanQuestions.getJSONObject(i);
                if (question.isNull("yourans") || !question.has("iscorr") || !question.getBoolean("iscorr")) {
                    unanswered.add("判断题 " + question.getInt("id"));
                }
            }

        } catch (Exception e) {
            Log.e("QuizHomeActivity", "获取未答题目失败", e);
        }
        return unanswered;
    }

    private void showUnansweredQuestionsDialog(List<String> unansweredQuestions) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialog);
        builder.setTitle("以下题目未作答或未答对");

        // 先按题型和ID排序未答题目
        Collections.sort(unansweredQuestions, (a, b) -> {
            String[] partsA = a.split(" ");
            String[] partsB = b.split(" ");
            int typeOrderA = getTypeOrder(partsA[0]);
            int typeOrderB = getTypeOrder(partsB[0]);
            if (typeOrderA != typeOrderB) {
                return Integer.compare(typeOrderA, typeOrderB);
            }
            return Integer.compare(Integer.parseInt(partsA[1]), Integer.parseInt(partsB[1]));
        });

        Map<String, List<Integer>> questionMap = new LinkedHashMap<>(); // 使用LinkedHashMap保持顺序
        questionMap.put("SINGLE_CHOICE", new ArrayList<>());
        questionMap.put("MULTI_CHOICE", new ArrayList<>());
        questionMap.put("TRUE_FALSE", new ArrayList<>());

        try {
            File paperFile = new File(getFilesDir(), "question_banks/" + currentPaperId + ".json");
            if (paperFile.exists()) {
                String jsonStr = new String(Files.readAllBytes(paperFile.toPath()));
                JSONObject paperJson = new JSONObject(jsonStr);
                JSONObject timu = paperJson.getJSONObject("timu");

                int danxuanCount = timu.getJSONArray("danxuan").length();
                int duoxuanCount = timu.getJSONArray("duoxuan").length();

                for (String questionInfo : unansweredQuestions) {
                    String[] parts = questionInfo.split(" ");
                    String type = parts[0];
                    int originalId = Integer.parseInt(parts[1]);
                    int displayId = originalId;

                    switch (type) {
                        case "单选题":
                            displayId = originalId;
                            questionMap.get("SINGLE_CHOICE").add(displayId);
                            break;
                        case "多选题":
                            displayId = originalId + danxuanCount;
                            questionMap.get("MULTI_CHOICE").add(displayId);
                            break;
                        case "判断题":
                            displayId = originalId + danxuanCount + duoxuanCount;
                            questionMap.get("TRUE_FALSE").add(displayId);
                            break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "处理未答题目失败", e);
        }

        // 使用现有的dialog_question_types布局
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_question_types, null);
        builder.setView(dialogView);

        // 初始化题号网格
        RecyclerView singleChoiceGrid = dialogView.findViewById(R.id.single_choice_grid);
        RecyclerView multiChoiceGrid = dialogView.findViewById(R.id.multi_choice_grid);
        RecyclerView trueFalseGrid = dialogView.findViewById(R.id.true_false_grid);

        // 设置网格布局
        setupQuestionGrid(singleChoiceGrid, "SINGLE_CHOICE", questionMap.get("SINGLE_CHOICE"));
        setupQuestionGrid(multiChoiceGrid, "MULTI_CHOICE", questionMap.get("MULTI_CHOICE"));
        setupQuestionGrid(trueFalseGrid, "TRUE_FALSE", questionMap.get("TRUE_FALSE"));

        // 根据是否有题目决定显示/隐藏
        singleChoiceGrid.setVisibility(questionMap.get("SINGLE_CHOICE").isEmpty() ? View.GONE : View.VISIBLE);
        multiChoiceGrid.setVisibility(questionMap.get("MULTI_CHOICE").isEmpty() ? View.GONE : View.VISIBLE);
        trueFalseGrid.setVisibility(questionMap.get("TRUE_FALSE").isEmpty() ? View.GONE : View.VISIBLE);

        builder.setPositiveButton("仍然交卷", (dialog, which) -> {
            showSubmitExamDialog(unansweredQuestions);
        });

        builder.setNegativeButton("取消", null);
        dialog = builder.create();
        dialog.show();
    }

    private int getTypeOrder(String type) {
        switch (type) {
            case "单选题": return 1;
            case "多选题": return 2;
            case "判断题": return 3;
            default: return 4;
        }
    }
    private void showSubmitExamDialog(List<String> unansweredQuestions) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialog);

        if (unansweredQuestions != null && !unansweredQuestions.isEmpty()) {
            builder.setTitle("确认交卷？");
            builder.setMessage("仍有" + unansweredQuestions.size() + "道题目未作答或未答对，确定要交卷吗？");
        } else {
            builder.setTitle("确认交卷");
            builder.setMessage("所有题目已完成，确定要交卷吗？");
        }

        builder.setPositiveButton("确定交卷", (dialog, which) -> {
            // 调用MidActivity的静态方法完成考试
            MidActivity.finishExamPaper(getFilesDir(), currentPaperId);
            MidActivity.tiqianfinishExamPaper(getFilesDir(), currentPaperId);

            // 计算分数
            int score = MidActivity.calculateExamScore(getFilesDir(), currentPaperId);

            // 显示考试结果
            showExamResult(score);
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showExamResult(int score) {
        try {
            File paperFile = new File(getFilesDir(), "question_banks/" + currentPaperId + ".json");
            if (!paperFile.exists()) return;

            String jsonStr = new String(Files.readAllBytes(paperFile.toPath()));
            JSONObject paperJson = new JSONObject(jsonStr);
            JSONObject examConfig = paperJson.getJSONObject("yushelianxi");

            int totalScore = 0;
            totalScore += examConfig.getJSONArray("danxuan").getInt(0) * examConfig.getJSONArray("danxuan").getInt(1);
            totalScore += examConfig.getJSONArray("duoxuan").getInt(0) * examConfig.getJSONArray("duoxuan").getInt(1);
            totalScore += examConfig.getJSONArray("panduan").getInt(0) * examConfig.getJSONArray("panduan").getInt(1);

            double correctRate = (double) score / totalScore * 100;

            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialog);
            builder.setTitle("考试结果");
            builder.setMessage(String.format("得分: %d/%d (%.1f%%)", score, totalScore, correctRate));
            builder.setPositiveButton("确定", (dialog, which) -> {
                finish();
            });
            builder.setCancelable(false);
            builder.show();

        } catch (Exception e) {
            Log.e("QuizHomeActivity", "显示考试结果失败", e);
        }
    }

    @Override
    public void onSwipeRight() {
        if (isProcessingSwipe) return;
        isProcessingSwipe = true;

        // 获取当前题型的所有题目ID
        List<Integer> questionNumbers = loadQuestionNumbers(currentQuestionType);
        if (questionNumbers.isEmpty()) {
            isProcessingSwipe = false;
            return;
        }

        // 获取当前题目在题型列表中的位置
        int currentIndex = questionNumbers.indexOf(currentQuestionId);
        if (currentIndex > 0) {
            // 同题型还有上一题
            int prevQuestionId = questionNumbers.get(currentIndex - 1);
            displayQuestion(currentQuestionType, prevQuestionId);

            // 计算新位置并更新进度
            int newPosition = calculateNewPosition(currentQuestionType, currentIndex - 1, false);
            if (questionDetailFragment != null) {
                questionDetailFragment.updateProgress(newPosition);
            }
        } else {
            // 尝试加载上一题型的最后一题
            tryLoadNextQuestionType(false);
        }

        isProcessingSwipe = false;
    }

    /**
     * 计算题目在试卷中的全局位置
     * @param questionType 题目类型
     * @param indexInType 在当前题型中的索引位置
     * @param isNext 是否是下一题（用于边界判断）
     * @return 题目在整张试卷中的序号（1-based）
     */
    private int calculateNewPosition(String questionType, int indexInType, boolean isNext) {
        int position = 0;

        // 1. 先计算该题型之前的题目总数
        if ("MULTI_CHOICE".equals(questionType)) {
            position += getQuestionCountInPaper("SINGLE_CHOICE");
        } else if ("TRUE_FALSE".equals(questionType)) {
            position += getQuestionCountInPaper("SINGLE_CHOICE");
            position += getQuestionCountInPaper("MULTI_CHOICE");
        }

        // 2. 加上当前题型中的位置
        position += indexInType + 1; // +1转换为1-based索引

        // 3. 边界检查
        int totalQuestions = getTotalQuestionsInPaper();
        if (position < 1) return 1;
        if (position > totalQuestions) return totalQuestions;

        return position;
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
    private boolean tryLoadNextQuestionType(boolean forward) {
        String nextType = getNextQuestionType(currentQuestionType, forward);
        if (nextType != null) {
            List<Integer> questionNumbers = loadQuestionNumbers(nextType);
            if (!questionNumbers.isEmpty()) {
                // 如果是向前滑动，加载新题型第一题；向后则加载最后一题
                int targetId = forward ? questionNumbers.get(0) : questionNumbers.get(questionNumbers.size() - 1);
                animateAndDisplayQuestion(nextType, targetId, forward);
                return true;
            }
        }
        return false;
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

        QuestionDetailFragment.ReadingProgress progress = null;

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
                        loadFirstQuestionFromPaper();
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

        adapter.setOnItemClickListener((questionType, displayNumber) -> {
            // 将显示编号转换为原始题目ID
            int originalId = displayNumber;
            try {
                File paperFile = new File(getFilesDir(), "question_banks/" + currentPaperId + ".json");
                if (paperFile.exists()) {
                    String jsonStr = new String(Files.readAllBytes(paperFile.toPath()));
                    JSONObject paperJson = new JSONObject(jsonStr);
                    JSONObject timu = paperJson.getJSONObject("timu");

                    int danxuanCount = timu.getJSONArray("danxuan").length();
                    int duoxuanCount = timu.getJSONArray("duoxuan").length();

                    if ("MULTI_CHOICE".equals(questionType)) {
                        originalId = displayNumber - danxuanCount;
                    } else if ("TRUE_FALSE".equals(questionType)) {
                        originalId = displayNumber - danxuanCount - duoxuanCount;
                    }
                }
            } catch (Exception e) {
                Log.e("QuizHomeActivity", "转换题目ID失败", e);
            }

            displayQuestion(questionType, originalId);
            if (dialog != null) dialog.dismiss();
        });

        builder.setView(recyclerView);
        builder.setNegativeButton("取消", null);

        dialog = builder.create();
        dialog.show();
    }
    public List<Integer> loadQuestionNumbers(String type) {
        List<Integer> numbers = new ArrayList<>();
        try {
            File paperFile = new File(getFilesDir(), "question_banks/" + currentPaperId + ".json");
            if (!paperFile.exists()) return numbers;

            String jsonStr = new String(Files.readAllBytes(paperFile.toPath()));
            JSONObject paperJson = new JSONObject(jsonStr);
            JSONObject timu = paperJson.getJSONObject("timu");
            String typeKey = getTypeKey(type);

            JSONArray questions = timu.getJSONArray(typeKey);
            for (int i = 0; i < questions.length(); i++) {
                numbers.add(questions.getJSONObject(i).getInt("id"));
            }
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "加载题目列表失败", e);
        }
        return numbers;
    }


    private int getOriginalQuestionId(String questionType, int displayNumber) {
        try {
            File paperFile = new File(getFilesDir(), "question_banks/" + currentPaperId + ".json");
            if (paperFile.exists()) {
                String jsonStr = new String(Files.readAllBytes(paperFile.toPath()));
                JSONObject paperJson = new JSONObject(jsonStr);
                JSONObject timu = paperJson.getJSONObject("timu");

                if ("SINGLE_CHOICE".equals(questionType)) {
                    JSONArray danxuan = timu.getJSONArray("danxuan");
                    if (displayNumber <= danxuan.length()) {
                        return danxuan.getJSONObject(displayNumber - 1).getInt("id");
                    }
                } else if ("MULTI_CHOICE".equals(questionType)) {
                    JSONArray danxuan = timu.getJSONArray("danxuan");
                    JSONArray duoxuan = timu.getJSONArray("duoxuan");
                    if (displayNumber > danxuan.length() &&
                            displayNumber <= danxuan.length() + duoxuan.length()) {
                        return duoxuan.getJSONObject(displayNumber - danxuan.length() - 1).getInt("id");
                    }
                } else if ("TRUE_FALSE".equals(questionType)) {
                    JSONArray danxuan = timu.getJSONArray("danxuan");
                    JSONArray duoxuan = timu.getJSONArray("duoxuan");
                    JSONArray panduan = timu.getJSONArray("panduan");
                    if (displayNumber > danxuan.length() + duoxuan.length() &&
                            displayNumber <= danxuan.length() + duoxuan.length() + panduan.length()) {
                        return panduan.getJSONObject(displayNumber - danxuan.length() - duoxuan.length() - 1).getInt("id");
                    }
                }
            }
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "获取原始题目ID失败", e);
        }
        return displayNumber; // 如果转换失败，返回原始值
    }
    private void displayQuestion(String questionType, int questionNumber) {
        // 预加载下一题数据
        new Thread(() -> {
            List<Integer> questionNumbers = loadQuestionNumbers(questionType);
            int currentIndex = questionNumbers.indexOf(questionNumber);

            if (currentIndex < questionNumbers.size() - 1) {
                loadQuestionFromPaper(questionType, questionNumbers.get(currentIndex + 1));
            }
        }).start();

        // 主线程显示当前题目
        runOnUiThread(() -> {
            Question question = loadQuestionFromPaper(questionType, questionNumber);
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

    private Question loadQuestionFromPaper(String type, int number) {
        try {
            File paperFile = new File(getFilesDir(), "question_banks/" + currentPaperId + ".json");
            if (!paperFile.exists()) return null;

            String jsonStr = new String(Files.readAllBytes(paperFile.toPath()));
            JSONObject paperJson = new JSONObject(jsonStr);
            JSONObject timu = paperJson.getJSONObject("timu");
            String typeKey = getTypeKey(type);
            JSONArray questions = timu.getJSONArray(typeKey);

            for (int i = 0; i < questions.length(); i++) {
                JSONObject q = questions.getJSONObject(i);
                if (q.getInt("id") == number) {
                    // 从题库中加载完整题目信息
                    return loadQuestionFromFile(currentBankId, type, number);
                }
            }
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "从试卷加载题目失败", e);
        }
        return null;
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