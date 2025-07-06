package com.yuanseen.shuati.ui.gallery.ques.option;

import android.content.Context;
import android.widget.GridLayout;

import java.util.List;

public class OptionGridBuilder {
    private Context context;
    private GridLayout gridLayout;
    private int columnCount = 2;
    private int rowCount = 4;
    private int iconSize = 48; // dp
    private int itemMargin = 8; // dp

    public OptionGridBuilder(Context context, GridLayout gridLayout) {
        this.context = context;
        this.gridLayout = gridLayout;
        initGridLayout();
    }

    private void initGridLayout() {
        gridLayout.setColumnCount(columnCount);
        gridLayout.setRowCount(rowCount);
        gridLayout.setAlignmentMode(GridLayout.ALIGN_MARGINS);
        gridLayout.setColumnOrderPreserved(false);
    }

    public OptionGridBuilder setColumnCount(int count) {
        this.columnCount = count;
        gridLayout.setColumnCount(count);
        return this;
    }

    public OptionGridBuilder setRowCount(int count) {
        this.rowCount = count;
        gridLayout.setRowCount(count);
        return this;
    }

    public void build(List<OptionItem> items, OnItemClickListener listener) {
        gridLayout.removeAllViews();

        for (int i = 0; i < items.size(); i++) {
            OptionItem item = items.get(i);

            OptionItemView itemView = new OptionItemView(context);
            itemView.setData(item.getIconResId(), item.getLabel());

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(itemMargin, itemMargin, itemMargin, itemMargin);
            itemView.setLayoutParams(params);

            if (listener != null) {
                int finalI = i;
                itemView.setOnClickListener(v -> listener.onItemClick(finalI, item));
            }

            gridLayout.addView(itemView);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position, OptionItem item);
    }
}
