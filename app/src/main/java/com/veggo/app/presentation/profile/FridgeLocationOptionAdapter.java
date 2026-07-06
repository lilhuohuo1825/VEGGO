package com.veggo.app.presentation.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;

import java.util.List;

final class FridgeLocationOptionAdapter extends RecyclerView.Adapter<FridgeLocationOptionAdapter.ViewHolder> {

    interface OnLocationClickListener {
        void onLocationClick(int index, String name);
    }

    private final List<String> locations;
    private final int selectedIndex;
    private final OnLocationClickListener listener;

    FridgeLocationOptionAdapter(
            List<String> locations,
            int selectedIndex,
            OnLocationClickListener listener
    ) {
        this.locations = locations;
        this.selectedIndex = selectedIndex;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_fridge_location_option, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String name = locations.get(position);
        boolean selected = position == selectedIndex;
        holder.nameView.setText(name);
        holder.radioButton.setChecked(selected);
        holder.itemView.setBackgroundResource(selected ? R.drawable.bg_selected : R.drawable.bg_normal);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLocationClick(holder.getBindingAdapterPosition(), name);
            }
        });
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameView;
        private final AppCompatRadioButton radioButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.fridgeLocationOptionName);
            radioButton = itemView.findViewById(R.id.fridgeLocationOptionRadio);
        }
    }
}
