package com.veggo.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.core.utils.WarehouseDistanceUtils;

import java.util.List;

public class WarehouseOptionAdapter extends RecyclerView.Adapter<WarehouseOptionAdapter.WarehouseViewHolder> {
    private final List<WarehouseDistanceUtils.RankedWarehouse> items;
    private final OnWarehouseActionListener listener;
    private int selectedPosition;

    public WarehouseOptionAdapter(
            List<WarehouseDistanceUtils.RankedWarehouse> items,
            int selectedPosition,
            OnWarehouseActionListener listener
    ) {
        this.items = items;
        this.selectedPosition = selectedPosition;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WarehouseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_warehouse_option, parent, false);
        return new WarehouseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WarehouseViewHolder holder, int position) {
        WarehouseDistanceUtils.RankedWarehouse item = items.get(position);
        boolean selected = position == selectedPosition;
        holder.bind(item, selected);
        holder.itemView.setOnClickListener(v -> select(position));
        holder.radioButton.setOnClickListener(v -> select(position));
        holder.btnViewMap.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewMap(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    private void select(int position) {
        if (position == RecyclerView.NO_POSITION || position == selectedPosition) {
            return;
        }
        int previous = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(previous);
        notifyItemChanged(selectedPosition);
        if (listener != null) {
            listener.onWarehouseSelected(items.get(position));
        }
    }

    static final class WarehouseViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatRadioButton radioButton;
        private final TextView nameView;
        private final TextView addressView;
        private final TextView distanceView;
        private final TextView nearestBadge;
        private final TextView btnViewMap;

        WarehouseViewHolder(@NonNull View itemView) {
            super(itemView);
            radioButton = itemView.findViewById(R.id.radioWarehouse);
            nameView = itemView.findViewById(R.id.tvWarehouseName);
            addressView = itemView.findViewById(R.id.tvWarehouseAddress);
            distanceView = itemView.findViewById(R.id.tvWarehouseDistance);
            nearestBadge = itemView.findViewById(R.id.tvWarehouseNearest);
            btnViewMap = itemView.findViewById(R.id.btnWarehouseViewMap);
        }

        void bind(WarehouseDistanceUtils.RankedWarehouse item, boolean selected) {
            nameView.setText(item.warehouse.getName());
            addressView.setText(item.warehouse.getAddress());
            distanceView.setText(WarehouseDistanceUtils.formatDistanceEta(item.distanceKm, item.etaMinutes));
            nearestBadge.setVisibility(item.nearest ? View.VISIBLE : View.GONE);
            radioButton.setChecked(selected);
            itemView.setBackgroundResource(selected ? R.drawable.bg_selected : R.drawable.bg_normal);
        }
    }

    public interface OnWarehouseActionListener {
        void onWarehouseSelected(WarehouseDistanceUtils.RankedWarehouse warehouse);

        void onViewMap(WarehouseDistanceUtils.RankedWarehouse warehouse);
    }
}
