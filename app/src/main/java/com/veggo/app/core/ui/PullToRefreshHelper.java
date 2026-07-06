package com.veggo.app.core.ui;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.veggo.app.R;

/**
 * Shared pull-to-refresh setup used across list and scroll screens.
 */
public final class PullToRefreshHelper {

    private PullToRefreshHelper() {
    }

    public static void bind(
            @Nullable SwipeRefreshLayout refreshLayout,
            @Nullable View scrollTarget,
            Runnable onRefresh
    ) {
        if (refreshLayout == null || onRefresh == null) {
            return;
        }
        refreshLayout.setColorSchemeResources(R.color.primary_main, R.color.primary_hover);
        if (scrollTarget != null) {
            refreshLayout.setOnChildScrollUpCallback((parent, child) ->
                    scrollTarget.canScrollVertically(-1));
        }
        refreshLayout.setOnRefreshListener(onRefresh::run);
    }

    @Nullable
    public static SwipeRefreshLayout wrap(@Nullable View scrollTarget, Runnable onRefresh) {
        if (scrollTarget == null || !(scrollTarget.getParent() instanceof ViewGroup)) {
            return null;
        }
        ViewGroup parent = (ViewGroup) scrollTarget.getParent();
        int index = parent.indexOfChild(scrollTarget);
        ViewGroup.LayoutParams params = scrollTarget.getLayoutParams();
        parent.removeView(scrollTarget);

        SwipeRefreshLayout refreshLayout = new SwipeRefreshLayout(scrollTarget.getContext());
        bind(refreshLayout, scrollTarget, onRefresh);
        refreshLayout.addView(scrollTarget, new SwipeRefreshLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        parent.addView(refreshLayout, index, params);
        return refreshLayout;
    }

    public static void finish(@Nullable SwipeRefreshLayout refreshLayout) {
        if (refreshLayout != null) {
            refreshLayout.setRefreshing(false);
        }
    }
}
