package com.veggo.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.domain.model.Utility;

public class UtilityAdapter extends ListAdapter<Utility, UtilityAdapter.UtilityViewHolder> {
    private OnUtilityClickListener listener;

    public interface OnUtilityClickListener {
        void onUtilityClick(Utility utility);
    }

    public void setOnUtilityClickListener(OnUtilityClickListener listener) {
        this.listener = listener;
    }

    public UtilityAdapter() {
        super(new DiffUtil.ItemCallback<Utility>() {
            @Override
            public boolean areItemsTheSame(@NonNull Utility oldItem, @NonNull Utility newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Utility oldItem, @NonNull Utility newItem) {
                return oldItem.getName().equals(newItem.getName()) && oldItem.getIconRes() == newItem.getIconRes();
            }
        });
    }

    @NonNull
    @Override
    public UtilityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_utility, parent, false);
        return new UtilityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UtilityViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class UtilityViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgUtility;
        private final TextView tvUtilityName;

        UtilityViewHolder(@NonNull View itemView) {
            super(itemView);
            imgUtility = itemView.findViewById(R.id.imgUtility);
            tvUtilityName = itemView.findViewById(R.id.tvUtilityName);
        }

        void bind(Utility utility, OnUtilityClickListener listener) {
            tvUtilityName.setText(utility.getName());
            if (utility.getIconRes() != 0) {
                imgUtility.setImageResource(utility.getIconRes());
            }
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUtilityClick(utility);
                }
            });
        }
    }
}
