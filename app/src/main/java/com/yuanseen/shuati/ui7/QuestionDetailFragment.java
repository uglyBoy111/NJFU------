package com.yuanseen.shuati.ui7;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yuanseen.shuati.R;
import com.yuanseen.shuati.ui.gallery.addqes.bankinfo.BankInfoManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuestionDetailFragment extends Fragment {
    // 添加回调接口
    public interface OnSwipeListener {
        void onSwipeLeft();
        void onSwipeRight();
    }
    // 添加成员变量
    private boolean isAnswerShown = false ;

    private Question question;
    private String userSelectedAnswer = "";
    private Button confirmButton;
    // 在类顶部添加动画常量
    private static final int ANIMATION_DURATION = android.R.integer.config_mediumAnimTime;
    private long lastAnimationTime = 0;
    private static final long MIN_ANIMATION_INTERVAL = 200; // 最小动画间隔200ms
    private View view;
    private OnSwipeListener swipeListener;
    private GestureDetector gestureDetector;
    private FrameLayout rootContainer;
    // SharedPreferences 相关常量
    private static final String PREFS_NAME = "QuestionReadingProgress";
    private static final String KEY_LAST_QUESTION_TYPE_PREFIX = "last367_type_";
    private static final String KEY_LAST_QUESTION_ID_PREFIX = "last367_id_";
    private TextView questionTypeText;
    private RecyclerView optionsRecyclerView;
    private OptionAdapter optionAdapter;
    private TextView questionContentView;
    private TextView questionOptionsView;
    private TextView questionAnswerView;
    private ImageButton favoriteButton;

    ImageButton studyModeButton;

    private BankInfoManager bankInfoManager;
    private String currentBankId;
    private String currentQuestionType;
    private int currentQuestionId;
    private boolean isFavorite;
    private boolean isWrong;
    private boolean isStudyMode = false; // true表示背题模式，false表示做题模式
    private ProgressBar progressBar;
    private ImageButton wrongQuestionButton;
    private float x1, x2,y1,y2; // 用于记录触摸位置
    private static final int SWIPE_THRESHOLD = 50; // 滑动阈值
    public boolean isAnimating() {
        return isAnimating;
    }

    private boolean isAnimating = false; // 添加动画状态标志
    private ViewPropertyAnimator currentAnimation;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bankInfoManager = new BankInfoManager(requireContext());
        if (savedInstanceState != null) {
            isStudyMode = savedInstanceState.getBoolean("study_mode", false);
            isAnswerShown = savedInstanceState.getBoolean("answer_shown", false);
            updateStudyModeUI();
            if (isStudyMode) {
                showAnswerImmediately();
            }
        }
    }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("study_mode", isStudyMode);
        outState.putBoolean("answer_shown", isAnswerShown);
    }

    // Modify the onCreateView method
    @SuppressLint({"ClickableViewAccessibility"})
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_question_detail7, container, false);

        rootContainer = view.findViewById(R.id.root_container);
        questionTypeText = view.findViewById(R.id.question_type_text6);
        questionContentView = view.findViewById(R.id.question_content_text);
        optionsRecyclerView = view.findViewById(R.id.question_options_recycler);
        questionAnswerView = view.findViewById(R.id.question_answer_text);
        wrongQuestionButton = view.findViewById(R.id.btn_wrong_question);
        favoriteButton = view.findViewById(R.id.btn_favorite);
        progressBar = view.findViewById(R.id.question_progress_bar);
        confirmButton = view.findViewById(R.id.btn_confirm);
        confirmButton.setOnClickListener(v -> checkAnswer());
        confirmButton.setVisibility(View.GONE); // 初始隐藏

        studyModeButton = view.findViewById(R.id.btn_isread);
        studyModeButton.setOnClickListener(v -> toggleStudyMode());
        // Set up RecyclerView
        optionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        optionAdapter = new OptionAdapter();
        optionsRecyclerView.setAdapter(optionAdapter);

        favoriteButton.setOnClickListener(v -> toggleFavoriteStatus());

        wrongQuestionButton.setOnClickListener(v -> toggleWrongQuestionStatus());

        // Improved gesture detection
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 50; // Reduced threshold
            private static final int SWIPE_VELOCITY_THRESHOLD = 50; // Reduced velocity threshold

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (isAnimating) return false;

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                // Check if it's mainly horizontal swipe
                if (Math.abs(diffX) > Math.abs(diffY) &&
                        Math.abs(diffX) > SWIPE_THRESHOLD &&
                        Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {

                    if (diffX > 0) {
                        if (swipeListener != null) swipeListener.onSwipeRight();
                    } else {
                        if (swipeListener != null) swipeListener.onSwipeLeft();
                    }
                    return true;
                }
                return false;
            }
        });

        // 修改 rootContainer 的触摸监听
        rootContainer.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x1 = event.getX();
                    y1 = event.getY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float diffX = Math.abs(event.getX() - x1);
                    float diffY = Math.abs(event.getY() - y1);

                    // 如果是水平滑动，消费事件
                    if (diffX > SWIPE_THRESHOLD && diffX > diffY) {
                        return true;
                    }
                    break;
            }
            return false;
        });

        // Set touch listener for NestedScrollView to allow swiping
        NestedScrollView nestedScrollView = view.findViewById(R.id.nested_scroll_view);
// 修改 NestedScrollView 的触摸监听
        nestedScrollView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x1 = event.getX();
                    y1 = event.getY();
                    return false; // 允许NestedScrollView处理垂直滚动
                case MotionEvent.ACTION_MOVE:
                    float diffX = Math.abs(event.getX() - x1);
                    float diffY = Math.abs(event.getY() - y1);

                    // 如果是水平滑动，消费事件
                    if (diffX > SWIPE_THRESHOLD && diffX > diffY) {
                        return true;
                    }
                    break;
            }
            return false;
        });

        optionsRecyclerView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 记录初始触摸位置
                    x1 = event.getX();
                    y1 = event.getY();
                    // 不拦截DOWN事件，让RecyclerView处理
                    return false;
                case MotionEvent.ACTION_MOVE:
                    // 计算X和Y方向的移动距离
                    float diffX = Math.abs(event.getX() - x1);
                    float diffY = Math.abs(event.getY() - y1);

                    // 如果是明显的水平滑动（X方向移动更多），则拦截事件用于手势检测
                    if (diffX > SWIPE_THRESHOLD && diffX > diffY) {
                        gestureDetector.onTouchEvent(event);
                        return true;
                    }
                    // 否则允许RecyclerView处理垂直滚动
                    return false;
                default:
                    return false; // 不拦截其他事件
            }
        });

        return view;
    }
    private void toggleStudyMode() {
        isStudyMode = !isStudyMode;
        updateStudyModeUI();

        // 根据当前模式更新题目显示
        if (isStudyMode) {
            // 背题模式直接显示答案
            showAnswerImmediately();
        } else {
            // 做题模式重置为初始状态
            resetToQuestionState();
        }
    }

    private void updateStudyModeUI() {
        ImageButton studyModeButton = view.findViewById(R.id.btn_isread);
        studyModeButton.setImageResource(
                isStudyMode ? R.drawable.book_open_duotone_icon : R.drawable.books_duotone_icon
        );

        // 根据模式更新答案可见性
        if (isStudyMode) {
            questionAnswerView.setVisibility(View.VISIBLE);
        } else {
            questionAnswerView.setVisibility(View.GONE);
        }
    }

    private void resetToQuestionState() {
        isAnswerShown = false;
        userSelectedAnswer = "";
        confirmButton.setVisibility(View.GONE);

        // 仅在非背题模式下隐藏答案
        if (!isStudyMode) {
            questionAnswerView.setVisibility(View.GONE);
        }

        optionAdapter.notifyDataSetChanged();
    }

    private void showAnswerImmediately() {
        isAnswerShown = true;
        questionAnswerView.setVisibility(View.VISIBLE); // 强制显示答案
        confirmButton.setVisibility(View.GONE);
        if (optionAdapter != null) {
            optionAdapter.notifyDataSetChanged();
        }
    }
    private void checkAnswer() {
        if (this.question == null) return;
        isAnswerShown = true;
        confirmButton.setVisibility(View.GONE);

        // 显示答案区域
        questionAnswerView.setVisibility(View.VISIBLE);
        String answerText = "答案: " + formatAnswer(question.getAnswer(), question.getType());
        questionAnswerView.setText(answerText);

        // 检查答案是否正确
        boolean isCorrect = checkAnswerCorrectness();

        // 更新选项显示
        optionAdapter.notifyDataSetChanged();

        if (isCorrect) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (swipeListener != null && isAdded()) {
                    swipeListener.onSwipeLeft();
                }
            }, 500);
        } else {
            addToWrongQuestions();
            showAnswerFeedback();
        }
    }

    private int getFavoriteQuestionsCount() {
        if (getActivity() instanceof QuizHomeActivity) {
            QuizHomeActivity activity = (QuizHomeActivity) getActivity();
            String typeKey = getTypeKey(currentQuestionType);
            return activity.bankInfoManager.getBankInfo(currentBankId)
                    .getWrongQuestions().get(typeKey).size();
        }
        return 0;
    }

    private boolean checkAnswerCorrectness() {
        if (question.getType() == Question.Type.MULTI_CHOICE) {
            String sortedUserAnswer = sortAnswer(userSelectedAnswer);
            String sortedCorrectAnswer = sortAnswer(question.getAnswer());
            return sortedUserAnswer.equals(sortedCorrectAnswer);
        }
        return userSelectedAnswer.equals(question.getAnswer());
    }

    private void showAnswerFeedback() {
        if (question.getType() == Question.Type.MULTI_CHOICE) {
            String userAnswer = sortAnswer(userSelectedAnswer);
            String correctAnswer = sortAnswer(question.getAnswer());

            if (userAnswer.isEmpty()) {
                Toast.makeText(getContext(), "未选择任何答案", Toast.LENGTH_SHORT).show();
            } else if (isSubset(userAnswer, correctAnswer)) {
                Toast.makeText(getContext(), "少选了正确答案", Toast.LENGTH_SHORT).show();
            } else if (hasCommonElements(userAnswer, correctAnswer)) {
                Toast.makeText(getContext(), "部分答案正确", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "全部答案错误", Toast.LENGTH_SHORT).show();
            }
        }
    }


    // 检查是否有共同元素（部分正确）
    private boolean hasCommonElements(String userAnswer, String correctAnswer) {
        for (char c : userAnswer.toCharArray()) {
            if (correctAnswer.indexOf(c) != -1) {
                return true;
            }
        }
        return false;
    }
    // 检查是否是子集（用于判断少选情况）
    private boolean isSubset(String userAnswer, String correctAnswer) {
        for (char c : userAnswer.toCharArray()) {
            if (correctAnswer.indexOf(c) == -1) {
                return false;
            }
        }
        return !userAnswer.isEmpty();
    }

    // 添加到错题本的方法
    private void addToWrongQuestions() {
        if (getActivity() instanceof QuizHomeActivity) {
            QuizHomeActivity activity = (QuizHomeActivity) getActivity();
            String typeKey = getTypeKey(currentQuestionType);
            if (!bankInfoManager.getBankInfo(currentBankId).getWrongQuestions().get(typeKey).contains(currentQuestionId)) {
                bankInfoManager.addWrongQuestion(currentBankId, typeKey, currentQuestionId);
                isWrong = true;
                updateButtonStates();
//                Toast.makeText(getContext(), "已添加到错题本", Toast.LENGTH_SHORT).show();
            }
        }
    }
    // 添加辅助方法对多选题答案进行排序
    private String sortAnswer(String answer) {
        char[] chars = answer.toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }
    // 设置滑动监听器
    public void setOnSwipeListener(OnSwipeListener listener) {
        this.swipeListener = listener;
    }

    // 添加更新进度条的方法
    private void updateProgress(int currentPosition, int totalCount) {
        if (progressBar != null) {
            int favoriteCount = getFavoriteQuestionsCount();
            if (favoriteCount > 0) {
                // 如果是收藏题目，按照收藏数量计算进度
                int progress = (int) ((currentPosition * 100f) / favoriteCount);
                progressBar.setProgress(progress);
            } else if (totalCount > 0) {
                // 如果没有收藏题目，按照总题目数量计算进度
                int progress = (int) ((currentPosition * 100f) / totalCount);
                progressBar.setProgress(progress);
            }
        }
    }

    // 修改displayQuestionDetails方法
    // 更新displayQuestionDetails方法
    public void displayQuestionDetails(@Nullable Question question, boolean isFavorite, boolean isWrong, boolean isNext) {
        if (!isAdded() || getView() == null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (isAdded() && getView() != null) {
                    displayQuestionDetails(question, isFavorite, isWrong, isNext);
                }
            });
            return;
        }

        // 更新题目内容
        updateQuestionContent(question, isFavorite, isWrong);

        // 根据当前模式决定是否显示答案
        if (isStudyMode) {
            showAnswerImmediately(); // 背题模式直接显示答案
        } else {
            resetToQuestionState();  // 做题模式隐藏答案
        }
// 更新进度条
        if (getActivity() instanceof QuizHomeActivity) {
            QuizHomeActivity activity = (QuizHomeActivity) getActivity();
            List<Integer> questionNumbers = activity.loadQuestionNumbers(currentQuestionType);
            int currentIndex = questionNumbers.indexOf(currentQuestionId);
            if (currentIndex != -1) {
                // 获取收藏题目列表
                String typeKey = getTypeKey(currentQuestionType);
                List<Integer> favoriteQuestions = activity.bankInfoManager.getBankInfo(currentBankId)
                        .getWrongQuestions().get(typeKey);

                // 如果是收藏题目，计算在收藏列表中的位置
                if (favoriteQuestions.contains(currentQuestionId)) {
                    int favoriteIndex = favoriteQuestions.indexOf(currentQuestionId);
                    updateProgress(favoriteIndex + 1, favoriteQuestions.size());
                } else {
                    updateProgress(currentIndex + 1, questionNumbers.size());
                }
            }
        }

        // 执行滑动动画
        if (isNext) {
            performSwipeAnimation(true, null);
        } else {
            performReturnAnimation(null);
        }
        updateButtonStates(); // 确保按钮状态更新
    }



    // 移除之前的动画方法，添加新的动画控制方法
    private void performSwipeAnimation(boolean isNext, Runnable onComplete) {
        // 防止动画重叠
        if (isAnimating || System.currentTimeMillis() - lastAnimationTime < MIN_ANIMATION_INTERVAL) {
            return;
        }

        View view = getView();
        if (view == null) return;

        isAnimating = true;
        lastAnimationTime = System.currentTimeMillis();

        float startX = isNext ? view.getWidth() * 0.2f : -view.getWidth() * 0.2f;
        view.setTranslationX(startX);
        view.setAlpha(0.9f);

        view.animate()
                .translationX(0)
                .alpha(1f)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    isAnimating = false;
                    if (onComplete != null) {
                        onComplete.run();
                    }
                })
                .start();
    }
    public void performReturnAnimation(Runnable onComplete) {
        if (isAnimating) {
            if (currentAnimation != null) {
                currentAnimation.cancel();
            }
        }

        View view = getView();
        if (view == null) return;

        isAnimating = true;
        view.clearAnimation();

        // 设置起始状态（从左侧25%位置开始）
        view.setTranslationX(-view.getWidth() * 0.25f);
        view.setAlpha(0.8f);

        // 使用属性动画替代视图动画
        currentAnimation = view.animate()
                .translationX(0)
                .alpha(1f)
                .setDuration(250)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    isAnimating = false;
                    currentAnimation = null;
                    if (onComplete != null) {
                        onComplete.run();
                    }
                })
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationCancel(Animator animation) {
                        isAnimating = false;
                        currentAnimation = null;
                    }
                });
        currentAnimation.start();
    }

    // 内容更新逻辑提取到单独方法
    private void updateQuestionContent(Question question, boolean isFavorite, boolean isWrong) {
        Log.e("QuizHomeActivity", question.getContent());
        this.question = question;
        this.userSelectedAnswer = ""; // 重置用户选择
        // 隐藏答案区域
//        questionAnswerView.setVisibility(View.GONE); // 初始隐藏
        // 添加空检查
        if (questionTypeText == null || questionContentView == null ||
                optionsRecyclerView == null || questionAnswerView == null) {
            return;
        }

        if (question == null) {
            questionTypeText.setText("");
            questionContentView.setText("暂无题目内容");
            optionAdapter.setOptions(new ArrayList<>(), "");
            questionAnswerView.setText("");
            return;
        }

        currentBankId = question.getBankId();
        currentQuestionType = question.getType().name();
        currentQuestionId = question.getNumber();
        this.isFavorite = isFavorite;
        this.isWrong = isWrong;

        saveReadingProgress(requireContext(),currentBankId, currentQuestionType, currentQuestionId);

        // 设置题型和题号
        String typeText = "";
        switch (question.getType()) {
            case SINGLE_CHOICE:
                typeText = "单选题 #" + currentQuestionId;
                break;
            case MULTI_CHOICE:
                typeText = "多选题 #" + currentQuestionId;
                break;
            case TRUE_FALSE:
                typeText = "判断题 #" + currentQuestionId;
                break;
        }
        questionTypeText.setText(typeText);

        // 设置题干
        questionContentView.setText(question.getContent());

        // 设置选项时重置选中状态
        List<OptionItem> options = new ArrayList<>();
        if (question.getType() != Question.Type.TRUE_FALSE) {
            String[] optionLines = question.getOptions().split("\n");
            for (String line : optionLines) {
                if (line.length() > 2) {
                    options.add(new OptionItem(line.substring(0, 1), line.substring(2)));
                }
            }
        } else {
            // 判断题特殊处理
            options.add(new OptionItem("A", "正确"));
            options.add(new OptionItem("B", "错误"));
        }
        optionAdapter.setOptions(options, question.getAnswer());

        // 设置答案
        String answerText = "答案: ";
        switch (question.getType()) {
            case SINGLE_CHOICE:
                answerText += question.getAnswer();
                break;
            case MULTI_CHOICE:
                StringBuilder sb = new StringBuilder();
                for (char c : question.getAnswer().toCharArray()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(c);
                }
                answerText += sb.toString();
                break;
            case TRUE_FALSE:
                answerText += question.getAnswer().equals("A") ? "正确" : "错误";
                break;
        }
        questionAnswerView.setText(answerText);

        updateButtonStates();
    }
    private String formatAnswer(String answer, Question.Type type) {
        if (type == Question.Type.MULTI_CHOICE) {
            // 多选答案格式化为"A,B,C"
            StringBuilder sb = new StringBuilder();
            for (char c : answer.toCharArray()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(c);
            }
            return sb.toString();
        }
        return answer;
    }

    private void updateButtonStates() {
        // 更新收藏按钮状态
        favoriteButton.setImageResource(
                isFavorite ? R.drawable.tar_duotone_icon : R.drawable.star_half_duotone_icon
        );

        // 更新错题按钮状态
        wrongQuestionButton.setImageResource(
                isWrong ? R.drawable.calendar_x_duotone_icon : R.drawable.calendar_duotone_icon
        );
    }

    private void toggleFavoriteStatus() {
        if (getActivity() instanceof QuizHomeActivity) {
            QuizHomeActivity activity = (QuizHomeActivity) getActivity();
            if (isFavorite) {
                activity.removeFavorite(currentQuestionType, currentQuestionId);
                Toast.makeText(getContext(), "已移除收藏", Toast.LENGTH_SHORT).show();
            } else {
                activity.addToFavorite(currentQuestionType, currentQuestionId);
                Toast.makeText(getContext(), "已加入收藏", Toast.LENGTH_SHORT).show();
            }
            isFavorite = !isFavorite;
            updateButtonStates();

        }
    }

    private void toggleWrongQuestionStatus() {
        String typeKey = getTypeKey(currentQuestionType);
        if (isWrong) {
            bankInfoManager.removeWrongQuestion(currentBankId, typeKey, currentQuestionId);
            Toast.makeText(getContext(), "已移出错题本", Toast.LENGTH_SHORT).show();
        } else {
            bankInfoManager.addWrongQuestion(currentBankId, typeKey, currentQuestionId);
            Toast.makeText(getContext(), "已加入错题本", Toast.LENGTH_SHORT).show();
        }
        isWrong = !isWrong;
        updateButtonStates();
    }

    private String getTypeKey(String questionType) {
        switch (questionType) {
            case "SINGLE_CHOICE": return "danxuan";
            case "MULTI_CHOICE": return "duoxuan";
            case "TRUE_FALSE": return "panduan";
            default: return "";
        }
    }

    /**
     * 保存当前题库的阅读进度
     */
// 在 QuestionDetailFragment.java 中
    public static void saveReadingProgress(Context context, String bankId, String questionType, int questionId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_LAST_QUESTION_TYPE_PREFIX + bankId, questionType);
        editor.putInt(KEY_LAST_QUESTION_ID_PREFIX + bankId, questionId);
        editor.apply();
    }

    /**
     * 加载指定题库的阅读进度
     * @param context 上下文
     * @param bankId 题库ID
     * @return 阅读进度对象，如果没有则返回null
     */
    public static ReadingProgress loadReadingProgress(Context context, String bankId) {
        if (bankId == null) return null;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String questionType = prefs.getString(KEY_LAST_QUESTION_TYPE_PREFIX + bankId, null);
        int questionId = prefs.getInt(KEY_LAST_QUESTION_ID_PREFIX + bankId, -1);

        if (questionType == null || questionId == -1) {
            return null;
        }
        return new ReadingProgress(bankId, questionType, questionId);
    }

    /**
     * 清除指定题库的阅读进度
     * @param context 上下文
     * @param bankId 题库ID
     */
    public static void clearReadingProgress(Context context, String bankId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_LAST_QUESTION_TYPE_PREFIX + bankId);
        editor.remove(KEY_LAST_QUESTION_ID_PREFIX + bankId);
        editor.apply();
    }

    /**
     * 阅读进度数据类
     */
    public static class ReadingProgress {
        public final String bankId;          // 题库ID
        public final String questionType;    // 题目类型
        public final int questionId;         // 题目ID

        public ReadingProgress(String bankId, String questionType, int questionId) {
            this.bankId = bankId;
            this.questionType = questionType;
            this.questionId = questionId;
        }
    }

    private static class OptionItem {
        String key;
        String value;
        boolean isSelected; // 添加选中状态

        OptionItem(String key, String value) {
            this.key = key;
            this.value = value;
            this.isSelected = false; // 默认未选中
        }
    }

    private class OptionAdapter extends RecyclerView.Adapter<OptionAdapter.OptionViewHolder> {
        private List<OptionItem> options = new ArrayList<>();
        private String correctAnswer = "";

        void setOptions(List<OptionItem> options, String correctAnswer) {
            this.options = options;
            this.correctAnswer = correctAnswer;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_option, parent, false);
            return new OptionViewHolder(view);
        }

        // 修改OptionAdapter中的onBindViewHolder方法
        @Override
        public void onBindViewHolder(@NonNull OptionViewHolder holder, int position) {
            OptionItem item = options.get(position);
            holder.optionKey.setText(item.key + ".");
            holder.optionValue.setText(item.value);

            // 重置背景和文字颜色
            holder.itemView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            holder.optionKey.setTextColor(getResources().getColor(R.color.black));
            holder.optionValue.setTextColor(getResources().getColor(R.color.black));

            // 如果是多选题且未确认，且选项被选中，则变灰
            if (question.getType() == Question.Type.MULTI_CHOICE && !isAnswerShown && item.isSelected) {
                holder.itemView.setBackgroundColor(getResources().getColor(R.color.gray_200));
                holder.optionKey.setTextColor(getResources().getColor(R.color.gray_500));
                holder.optionValue.setTextColor(getResources().getColor(R.color.gray_500));
            }

            // 修改这部分逻辑，考虑背题模式
            if (isAnswerShown || isStudyMode) {
                // 显示答案时：正确答案标绿，用户错误答案标红
                if (correctAnswer.contains(item.key)) {
                    holder.optionKey.setTextColor(getResources().getColor(R.color.teal_700));
                    holder.optionValue.setTextColor(getResources().getColor(R.color.teal_700));
                } else if (userSelectedAnswer.contains(item.key) && !isStudyMode) {
                    // 只在做题模式下显示用户错误答案
                    holder.optionKey.setTextColor(getResources().getColor(R.color.red_700));
                    holder.optionValue.setTextColor(getResources().getColor(R.color.red_700));
                }
            }

            holder.itemView.setOnClickListener(v -> {
                if (isAnswerShown) return;

                if (question.getType() == Question.Type.SINGLE_CHOICE ||
                        question.getType() == Question.Type.TRUE_FALSE) {
                    // 单选直接验证
                    // 重置所有选项的选中状态
                    for (OptionItem option : options) {
                        option.isSelected = false;
                    }
                    item.isSelected = true;
                    userSelectedAnswer = item.key;
                    checkAnswer();
                } else {
                    // 多选记录选择
                    item.isSelected = !item.isSelected;
                    if (item.isSelected) {
                        if (!userSelectedAnswer.contains(item.key)) {
                            userSelectedAnswer += item.key;
                        }
                    } else {
                        userSelectedAnswer = userSelectedAnswer.replace(item.key, "");
                    }
                    confirmButton.setVisibility(View.VISIBLE);
                }
                notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return options.size();
        }

        class OptionViewHolder extends RecyclerView.ViewHolder {
            TextView optionKey;
            TextView optionValue;

            OptionViewHolder(View itemView) {
                super(itemView);
                optionKey = itemView.findViewById(R.id.option_key);
                optionValue = itemView.findViewById(R.id.option_value);
            }
        }
    }
    // 添加滑动动画方法
    public void animateSwipe(boolean isSwipeLeft, Runnable onComplete) {
        View rootView = getView();
        if (rootView == null) return;

        // 清除当前动画
        rootView.clearAnimation();

        // 加载动画资源
        Animation anim = AnimationUtils.loadAnimation(getContext(),
                isSwipeLeft ? R.anim.slide_in_right : R.anim.slide_in_left);

        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                if (onComplete != null) {
                    onComplete.run();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        rootView.startAnimation(anim);
    }

}