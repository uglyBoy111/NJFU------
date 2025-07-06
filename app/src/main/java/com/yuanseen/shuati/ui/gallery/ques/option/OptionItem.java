package com.yuanseen.shuati.ui.gallery.ques.option;

public class OptionItem {
    private int iconResId;
    private String label;

    public OptionItem(int iconResId, String label) {
        this.iconResId = iconResId;
        this.label = label;
    }

    // Getters
    public int getIconResId() { return iconResId; }
    public String getLabel() { return label; }
}