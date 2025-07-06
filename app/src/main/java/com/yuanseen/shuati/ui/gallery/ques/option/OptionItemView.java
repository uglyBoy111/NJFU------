package com.yuanseen.shuati.ui.gallery.ques.option;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.yuanseen.shuati.R;

public class OptionItemView extends LinearLayout {
    private ImageView icon;
    private TextView text;

    public OptionItemView(Context context) {
        this(context, null);
    }

    public OptionItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);

        // 添加图标
        icon = new ImageView(context);
        icon.setLayoutParams(new LayoutParams(180, 180));
        addView(icon);
        setBackgroundResource(R.drawable.item_background); // Add this line for item background
        setPadding(16, 16, 16, 16); // Add padding
        // 添加文字
        text = new TextView(context);
        text.setLayoutParams(new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        ));
        text.setTextSize(14);
        text.setPadding(0, 8, 0, 0);
        // 在创建 TextView 的代码部分添加
        text.setBackgroundResource(R.drawable.item_background);
// 设置文字背景颜色
//        text.setBackgroundColor(ContextCompat.getColor(context, R.color.gray_200));
        addView(text);
    }

    public void setData(int iconResId, String label) {
        icon.setImageResource(iconResId);
        text.setText(label);
    }
}