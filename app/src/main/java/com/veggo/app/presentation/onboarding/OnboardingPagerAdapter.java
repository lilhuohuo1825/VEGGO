package com.veggo.app.presentation.onboarding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.domain.model.OnboardingItem;

import java.util.List;

public class OnboardingPagerAdapter extends RecyclerView.Adapter<OnboardingPagerAdapter.ViewHolder> {

    private final List<OnboardingItem> items;

    public OnboardingPagerAdapter(List<OnboardingItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OnboardingItem item = items.get(position);
        holder.image.setImageResource(item.getImageRes());
        holder.title.setText(item.getTitle());
        holder.desc.setText(item.getDescription());

        // Chỉ có trang onboarding 1 mới hiện tvChaomung và imageLogo
        if (position == 0) {
            holder.tvChaomung.setVisibility(View.VISIBLE);
            holder.imageLogo.setVisibility(View.VISIBLE);
        } else {
            holder.tvChaomung.setVisibility(View.GONE);
            holder.imageLogo.setVisibility(View.GONE);
        }


    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;
        TextView desc;
        TextView tvChaomung;
        ImageView imageLogo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imageOnboarding);
            title = itemView.findViewById(R.id.tvTitle);
            desc = itemView.findViewById(R.id.tvDesc);
            tvChaomung = itemView.findViewById(R.id.tvChaomung);
            imageLogo = itemView.findViewById(R.id.imageLogo);
        }
    }
}
