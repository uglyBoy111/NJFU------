package com.yuanseen.shuati.util;

import android.content.Context;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateTimeUtils {
    public static void updateDateTimeDisplay(Context context, TextView dateTimeTextView) {
        // 获取当前日期和时间
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();

        // 格式化日期和时间
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        // 获取问候语
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) {
            greeting = "Good morning";
        } else if (hour < 18) {
            greeting = "Good afternoon";
        } else {
            greeting = "Good evening";
        }

        // 构建显示文本
        String displayText = dateFormat.format(now) + "\n"
                + timeFormat.format(now) + "\n"
                + greeting;

        // 设置文本
        dateTimeTextView.setText(displayText);
    }
}
