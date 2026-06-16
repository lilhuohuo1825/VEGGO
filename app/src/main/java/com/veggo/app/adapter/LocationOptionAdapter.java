package com.veggo.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;

import java.util.List;

public class LocationOptionAdapter extends RecyclerView.Adapter<LocationOptionAdapter.LocationViewHolder> {
    private final List<LocationItemUiModel> items;
    private final OnLocationSelectedListener listener;
    private int selectedPosition;

    public LocationOptionAdapter(List<LocationItemUiModel> items, int selectedPosition, OnLocationSelectedListener listener) {
        this.items = items;
        this.selectedPosition = selectedPosition;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location, parent, false);
        return new LocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        holder.bind(items.get(position), position == selectedPosition);
        holder.itemView.setOnClickListener(v -> select(position));
        holder.radioButton.setOnClickListener(v -> select(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void select(int position) {
        if (position == RecyclerView.NO_POSITION || position == selectedPosition) {
            return;
        }
        int previous = selectedPosition;
        selectedPosition = position;
        if (previous != RecyclerView.NO_POSITION) {
            notifyItemChanged(previous);
        }
        notifyItemChanged(position);
        if (listener != null) {
            listener.onLocationSelected(items.get(position));
        }
    }

    static final class LocationViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatRadioButton radioButton;
        private final TextView nameView;
        private final TextView addressView;
        private final TextView defaultBadgeView;

        private LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            radioButton = itemView.findViewById(R.id.radioLocation);
            nameView = itemView.findViewById(R.id.tvLocationName);
            addressView = itemView.findViewById(R.id.tvLocationAddress);
            defaultBadgeView = itemView.findViewById(R.id.tvLocationDefault);
        }

        private void bind(LocationItemUiModel item, boolean isSelected) {
            radioButton.setChecked(isSelected);
            nameView.setText(item.name);
            addressView.setText(item.address);
            defaultBadgeView.setVisibility(item.isDefault ? View.VISIBLE : View.GONE);
            itemView.setBackgroundResource(isSelected ? R.drawable.bg_selected : R.drawable.bg_normal);
        }
    }

    public interface OnLocationSelectedListener {
        void onLocationSelected(LocationItemUiModel item);
    }

    public static final class LocationItemUiModel {
        public final String name;
        public final String address;
        public final boolean isDefault;

        public LocationItemUiModel(String name, String address, boolean isDefault) {
            this.name = name;
            this.address = address;
            this.isDefault = isDefault;
        }
    }
}
