package com.yuanseen.shuati.ui.home;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yuanseen.shuati.R;
import com.yuanseen.shuati.ui.gallery.GalleryAdapter;
import com.yuanseen.shuati.ui.gallery.GalleryItem;
import com.yuanseen.shuati.ui.gallery.SharedPreferencesHelper;
import com.yuanseen.shuati.ui.gallery.ques.QuestionBankFragment;
import com.yuanseen.shuati.util.DateTimeUtils;
import com.yuanseen.shuati.util.Exam;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class HomeFragment extends Fragment {

    private static final int MAX_RECENT_BANKS = 3;
    private TextView tvExamName, tvDaysLeft;
    private GalleryAdapter adapter;
    private List<GalleryItem> items;
    private RecyclerView recyclerView;
    private List<Exam> examList = new ArrayList<>();
    private static final String PREFS_NAME = "ExamPrefs";
    private static final String EXAMS_KEY = "exams";

    private SharedPreferencesHelper prefsHelper;
    private int currentExamIndex = 0;
    private Gson gson = new Gson();

    @Override
    public void onResume() {
        super.onResume();
        refreshRecyclerView();
    }

    private void refreshRecyclerView() {
        // 重新加载最近使用的题库数据
        items = prefsHelper.loadRecentItems();

        // 更新适配器数据
        if (adapter != null) {
            adapter.updateData(items);
            adapter.notifyDataSetChanged();
        }
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsHelper = new SharedPreferencesHelper(requireContext());
    }


    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        items = prefsHelper.loadRecentItems();

        TextView dateTimeTextView = view.findViewById(R.id.tv_datetime);
        DateTimeUtils.updateDateTimeDisplay(getActivity(), dateTimeTextView);
        tvExamName = view.findViewById(R.id.tv_exam_name);
        tvDaysLeft = view.findViewById(R.id.tv_days_left);

        CardView cardView = view.findViewById(R.id.cardview1);
        // 初始化RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 设置适配器
        adapter = new GalleryAdapter(items);
        recyclerView.setAdapter(adapter);

        // 加载保存的考试数据
        loadExams();

        // 初始化UI
        if (!examList.isEmpty()) {
            updateUIWithExam(examList.get(currentExamIndex));
        } else {
            tvExamName.setText("暂无考试");
            tvDaysLeft.setText("点击添加考试");
        }

        // 长按卡片打开考试管理对话框
        cardView.setOnLongClickListener(v -> {
            showExamManagementDialog();
            return true;
        });

        // 点击卡片切换到下一个考试
        cardView.setOnClickListener(v -> {
            if (!examList.isEmpty()) {
                currentExamIndex = (currentExamIndex + 1) % examList.size();
                updateUIWithExam(examList.get(currentExamIndex));
            } else {
                showAddExamDialog(); // 如果没有考试，点击直接添加
            }
        });

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
                    .remove(this)  // 隐藏当前 GalleryFragment（不销毁）
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void loadExams() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, 0);
        String json = prefs.getString(EXAMS_KEY, null);

        if (json != null) {
            Type type = new TypeToken<List<Exam>>() {}.getType();
            examList = gson.fromJson(json, type);

            // 需要处理Date的反序列化问题
            if (examList == null) {
                examList = new ArrayList<>();
            }
        }
    }
    private void saveRecentBank(GalleryItem item) {
        SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(requireContext());
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
    private void saveExams() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();

        String json = gson.toJson(examList);
        editor.putString(EXAMS_KEY, json);
        editor.apply();
    }

    private void showExamManagementDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setTitle("考试管理");

        String[] options = {"添加考试", "编辑当前考试", "删除当前考试", "查看全部考试"};

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    showAddExamDialog();
                    break;
                case 1:
                    if (!examList.isEmpty()) {
                        showEditExamDialog(examList.get(currentExamIndex));
                    } else {
                        Toast.makeText(getActivity(), "没有可编辑的考试", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 2:
                    if (!examList.isEmpty()) {
                        deleteExam(currentExamIndex);
                    } else {
                        Toast.makeText(getActivity(), "没有可删除的考试", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 3:
                    showAllExamsDialog();
                    break;
            }
        });

        builder.show();
    }

    // 修改showAddExamDialog方法
    private void showAddExamDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setTitle("添加考试");

        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_add_exam, null);
        EditText etExamName = dialogView.findViewById(R.id.et_exam_name);
        EditText etExamDate = dialogView.findViewById(R.id.et_exam_date);

        // 设置日期选择器
        etExamDate.setOnClickListener(v -> showDatePicker(etExamDate));

        builder.setView(dialogView);

        builder.setPositiveButton("添加", (dialog, which) -> {
            String name = etExamName.getText().toString();
            String dateStr = etExamDate.getText().toString();

            if (!name.isEmpty() && !dateStr.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date date = sdf.parse(dateStr);

                    Exam exam = new Exam(name, date);
                    examList.add(exam);
                    saveExams();

                    currentExamIndex = examList.size() - 1;
                    updateUIWithExam(exam);
                } catch (ParseException e) {
                    Toast.makeText(getActivity(), "日期格式不正确", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getActivity(), "请填写考试名称和日期", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // 添加日期选择器方法
    private void showDatePicker(EditText etExamDate) {
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now());

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("选择考试日期")
                .setCalendarConstraints(constraintsBuilder.build())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String formattedDate = sdf.format(new Date(selection));
            etExamDate.setText(formattedDate);
        });

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }
    private void showEditExamDialog(Exam exam) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setTitle("编辑考试");

        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_add_exam, null);
        EditText etExamName = dialogView.findViewById(R.id.et_exam_name);
        EditText etExamDate = dialogView.findViewById(R.id.et_exam_date);

        // 设置日期选择器
        etExamDate.setOnClickListener(v -> showDatePicker(etExamDate));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        etExamName.setText(exam.getName());
        etExamDate.setText(sdf.format(exam.getDate()));

        builder.setView(dialogView);

        builder.setPositiveButton("保存", (dialog, which) -> {
            String name = etExamName.getText().toString();
            String dateStr = etExamDate.getText().toString();

            if (!name.isEmpty() && !dateStr.isEmpty()) {
                try {
                    Date date = sdf.parse(dateStr);
                    exam.setName(name);
                    exam.setDate(date);
                    saveExams();
                    updateUIWithExam(exam);
                } catch (ParseException e) {
                    Toast.makeText(getActivity(), "日期格式不正确", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getActivity(), "请填写考试名称和日期", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showAllExamsDialog() {
        if (examList.isEmpty()) {
            Toast.makeText(getActivity(), "没有考试记录", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("全部考试");

        List<String> examDisplayList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        for (Exam exam : examList) {
            examDisplayList.add(exam.getName() + "\n" + sdf.format(exam.getDate()));
        }

        ListView listView = new ListView(getActivity());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_list_item_1,
                examDisplayList);
        listView.setAdapter(adapter);

        builder.setView(listView);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            currentExamIndex = position;
            updateUIWithExam(examList.get(position));
            ((AlertDialog) builder.create()).dismiss();
        });

        builder.setNegativeButton("关闭", null);
        builder.show();
    }

    private void deleteExam(int index) {
        examList.remove(index);
        saveExams();

        if (examList.isEmpty()) {
            tvExamName.setText("暂无考试");
            tvDaysLeft.setText("点击添加考试");
            currentExamIndex = 0;
        } else {
            currentExamIndex = Math.min(currentExamIndex, examList.size() - 1);
            updateUIWithExam(examList.get(currentExamIndex));
        }
    }

    private void updateUIWithExam(Exam exam) {
        tvExamName.setText(exam.getName());

        Calendar today = Calendar.getInstance();
        Calendar examDate = Calendar.getInstance();
        examDate.setTime(exam.getDate());

        long diffInMillis = examDate.getTimeInMillis() - today.getTimeInMillis();
        long daysLeft = diffInMillis / (1000 * 60 * 60 * 24);

        if (daysLeft < 0) {
            tvDaysLeft.setText("考试已结束");
        } else if (daysLeft == 0) {
            tvDaysLeft.setText("今天考试");
        } else {
            tvDaysLeft.setText("还有 " + daysLeft + " 天");
        }
    }
}