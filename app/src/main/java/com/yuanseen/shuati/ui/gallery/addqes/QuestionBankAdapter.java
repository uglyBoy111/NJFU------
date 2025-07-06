// QuestionBankAdapter.java
package com.yuanseen.shuati.ui.gallery.addqes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.yuanseen.shuati.R;
import java.util.List;

public class QuestionBankAdapter extends RecyclerView.Adapter<QuestionBankAdapter.ViewHolder> {

    private final List<QuestionBankItem> itemList;

    public QuestionBankAdapter(List<QuestionBankItem> itemList) {
        this.itemList = itemList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvSubtitle, tvDesc;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvDesc = itemView.findViewById(R.id.tvDesc);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_question_bank, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuestionBankItem item = itemList.get(position);

        holder.ivIcon.setImageResource(item.getImageRes());
        holder.tvTitle.setText(item.getTitle());
        holder.tvSubtitle.setText(item.getSubtitle());
        holder.tvDesc.setText(item.getDesc());
        holder.checkBox.setChecked(item.isSelected());

        holder.itemView.setOnClickListener(v -> {
            item.setSelected(!item.isSelected());
            holder.checkBox.setChecked(item.isSelected());
            notifyItemChanged(position); // 添加这行通知适配器数据变化
        });

        // 新增复选框点击处理
        holder.checkBox.setOnClickListener(v -> {
            item.setSelected(!item.isSelected());
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }
}