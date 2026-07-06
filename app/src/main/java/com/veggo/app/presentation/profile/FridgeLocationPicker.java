package com.veggo.app.presentation.profile;

import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;

import java.util.List;

public final class FridgeLocationPicker {

    public interface Callback {
        void onLocationSelected(int index, String name);

        void onAddNewRequested();
    }

    private FridgeLocationPicker() {
    }

    public static void show(
            @NonNull AppCompatActivity activity,
            @NonNull List<String> locations,
            int selectedIndex,
            @NonNull Callback callback
    ) {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_fridge_pick_location, null);
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogView);
        dialog.setCanceledOnTouchOutside(true);
        if (dialog.getWindow() != null) {
            FridgeDialogUi.applyPopupWindowStyle(dialog.getWindow(), activity);
        }

        TextView emptyView = dialogView.findViewById(R.id.fridgeLocationEmpty);
        RecyclerView listView = dialogView.findViewById(R.id.fridgeLocationList);

        if (locations.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            listView.setLayoutManager(new LinearLayoutManager(activity));
            int maxHeight = (int) (activity.getResources().getDisplayMetrics().density * 280);
            listView.post(() -> {
                if (listView.getHeight() > maxHeight) {
                    ViewGroup.LayoutParams params = listView.getLayoutParams();
                    params.height = maxHeight;
                    listView.setLayoutParams(params);
                }
            });
            listView.setAdapter(new FridgeLocationOptionAdapter(locations, selectedIndex, (index, name) -> {
                callback.onLocationSelected(index, name);
                dialog.dismiss();
            }));
        }

        dialogView.findViewById(R.id.fridgeLocationAddButton).setOnClickListener(v -> {
            callback.onAddNewRequested();
            dialog.dismiss();
        });

        dialog.show();
    }
}
