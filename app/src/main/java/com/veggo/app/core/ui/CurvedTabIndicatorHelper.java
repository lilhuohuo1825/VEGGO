package com.veggo.app.core.ui;

import android.animation.ValueAnimator;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;

import androidx.annotation.Nullable;

import com.veggo.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Elastic sliding curved tab indicator (same motion as Material TabLayout elastic mode).
 */
public final class CurvedTabIndicatorHelper {

    private static final int INDICATOR_HEIGHT_DP = 8;
    private static final int ANIM_DURATION_MS = 300;

    public static final class TabItem {
        public final View tabView;
        @Nullable public final View legacyIndicator;

        public TabItem(View tabView) {
            this(tabView, null);
        }

        public TabItem(View tabView, @Nullable View legacyIndicator) {
            this.tabView = tabView;
            this.legacyIndicator = legacyIndicator;
        }
    }

    private final TabItem[] tabs;
    @Nullable private final HorizontalScrollView scrollView;
    private final List<Overlay> overlays = new ArrayList<>();
    private int selectedIndex = -1;
    private int currentLeft;
    private int currentRight;
    private boolean positioned;
    @Nullable private ValueAnimator animator;

    private static final class Overlay {
        final FrameLayout host;
        final View indicator;

        Overlay(FrameLayout host, View indicator) {
            this.host = host;
            this.indicator = indicator;
        }
    }

    private CurvedTabIndicatorHelper(@Nullable HorizontalScrollView scrollView, TabItem[] tabs) {
        this.scrollView = scrollView;
        this.tabs = tabs;
        for (TabItem tab : tabs) {
            if (tab.legacyIndicator != null) {
                tab.legacyIndicator.setVisibility(View.GONE);
            }
        }
    }

    public static CurvedTabIndicatorHelper attach(
            @Nullable HorizontalScrollView scrollView,
            TabItem... tabs
    ) {
        if (tabs.length == 0) {
            throw new IllegalArgumentException("At least one tab is required");
        }
        CurvedTabIndicatorHelper helper = new CurvedTabIndicatorHelper(scrollView, tabs);
        FrameLayout host = ensureIndicatorHost(scrollView, tabs[0].tabView);
        helper.attachOverlay(host);
        host.post(() -> helper.selectTab(0, false));
        return helper;
    }

    public CurvedTabIndicatorHelper attachOverlay(FrameLayout host) {
        View indicator = new View(host.getContext());
        indicator.setBackgroundResource(R.drawable.bg_order_tab_indicator);
        int heightPx = dp(host, INDICATOR_HEIGHT_DP);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(0, heightPx);
        lp.gravity = Gravity.BOTTOM;
        host.addView(indicator, lp);
        overlays.add(new Overlay(host, indicator));
        if (selectedIndex >= 0) {
            snapToSelectedTab();
        }
        return this;
    }

    public void selectTab(int index) {
        selectTab(index, true);
    }

    public void selectTab(int index, boolean animate) {
        if (index < 0 || index >= tabs.length) {
            return;
        }
        if (index == selectedIndex && positioned && animate) {
            return;
        }
        selectedIndex = index;
        View tab = tabs[index].tabView;
        if (tab.getWidth() == 0) {
            tab.post(() -> selectTab(index, animate));
            return;
        }
        if (!animate || !positioned) {
            snapToSelectedTab();
            return;
        }
        animateToSelectedTab();
        scrollToTab(tab);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    private void snapToSelectedTab() {
        if (selectedIndex < 0 || selectedIndex >= tabs.length) {
            return;
        }
        View tab = tabs[selectedIndex].tabView;
        int left = boundsForTab(tab).left;
        int right = boundsForTab(tab).right;
        for (Overlay overlay : overlays) {
            applyIndicatorBounds(overlay, left, right - left);
        }
        currentLeft = left;
        currentRight = right;
        positioned = true;
    }

    private void animateToSelectedTab() {
        if (selectedIndex < 0 || selectedIndex >= tabs.length) {
            return;
        }
        if (animator != null) {
            animator.cancel();
        }

        View tab = tabs[selectedIndex].tabView;
        TabBounds target = boundsForTab(tab);
        final int startLeft = currentLeft;
        final int startRight = currentRight;
        final int targetLeft = target.left;
        final int targetRight = target.right;
        final boolean movingRight = startLeft < targetLeft;

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(ANIM_DURATION_MS);
        animator.addUpdateListener(animation -> {
            float offset = (float) animation.getAnimatedValue();
            float leftFraction = movingRight ? accelerate(offset) : decelerate(offset);
            float rightFraction = movingRight ? decelerate(offset) : accelerate(offset);
            int left = lerp(startLeft, targetLeft, leftFraction);
            int right = lerp(startRight, targetRight, rightFraction);
            for (Overlay overlay : overlays) {
                applyIndicatorBounds(overlay, left, Math.max(right - left, 0));
            }
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                currentLeft = targetLeft;
                currentRight = targetRight;
                positioned = true;
            }
        });
        animator.start();
        scrollToTab(tab);
    }

    private TabBounds boundsForTab(View tab) {
        TabBounds bounds = new TabBounds();
        if (overlays.isEmpty()) {
            bounds.left = 0;
            bounds.right = tab.getWidth();
            return bounds;
        }
        bounds.left = leftRelativeTo(tab, overlays.get(0).host);
        bounds.right = bounds.left + tab.getWidth();
        return bounds;
    }

    private void applyIndicatorBounds(Overlay overlay, int left, int width) {
        View indicator = overlay.indicator;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) indicator.getLayoutParams();
        if (lp.width != width) {
            lp.width = Math.max(width, 0);
            indicator.setLayoutParams(lp);
        }
        indicator.setTranslationX(left);
    }

    private void scrollToTab(View tab) {
        if (scrollView == null) {
            return;
        }
        scrollView.post(() -> {
            int scrollX = tab.getLeft() - (scrollView.getWidth() - tab.getWidth()) / 2;
            scrollView.smoothScrollTo(Math.max(scrollX, 0), 0);
        });
    }

    private static FrameLayout ensureIndicatorHost(
            @Nullable HorizontalScrollView scrollView,
            View tabView
    ) {
        if (scrollView != null) {
            return ensureScrollViewHost(scrollView);
        }
        return ensureFrameWrapper(resolveTabsRow(tabView));
    }

    private static FrameLayout ensureScrollViewHost(HorizontalScrollView scrollView) {
        if (scrollView.getChildCount() > 0) {
            View existing = scrollView.getChildAt(0);
            if (existing instanceof FrameLayout) {
                return (FrameLayout) existing;
            }
            ViewGroup.LayoutParams existingParams = existing.getLayoutParams();
            scrollView.removeView(existing);
            FrameLayout wrapper = createOverlayFrame(scrollView);
            wrapper.addView(existing, frameChildLayoutParams(existingParams));
            scrollView.addView(wrapper, existingParams);
            return wrapper;
        }
        FrameLayout wrapper = createOverlayFrame(scrollView);
        scrollView.addView(
                wrapper,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );
        return wrapper;
    }

    private static FrameLayout.LayoutParams frameChildLayoutParams(ViewGroup.LayoutParams source) {
        int width = source != null ? source.width : ViewGroup.LayoutParams.WRAP_CONTENT;
        int height = source != null ? source.height : ViewGroup.LayoutParams.MATCH_PARENT;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height);
        if (source instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) source;
            lp.setMargins(
                    margins.leftMargin,
                    margins.topMargin,
                    margins.rightMargin,
                    margins.bottomMargin
            );
        }
        return lp;
    }

    private static FrameLayout createOverlayFrame(ViewGroup parent) {
        FrameLayout wrapper = new FrameLayout(parent.getContext());
        wrapper.setClipChildren(false);
        wrapper.setClipToPadding(false);
        return wrapper;
    }

    private static View resolveTabsRow(View tabView) {
        View current = tabView;
        while (current.getParent() instanceof View) {
            View parent = (View) current.getParent();
            if (parent instanceof HorizontalScrollView) {
                HorizontalScrollView scrollView = (HorizontalScrollView) parent;
                if (scrollView.getChildCount() > 0) {
                    View directChild = scrollView.getChildAt(0);
                    if (directChild instanceof FrameLayout) {
                        FrameLayout frame = (FrameLayout) directChild;
                        if (frame.getChildCount() > 0) {
                            return frame.getChildAt(0);
                        }
                    }
                    return directChild;
                }
                return current;
            }
            current = parent;
        }
        View parent = tabView.getParent() instanceof View ? (View) tabView.getParent() : tabView;
        ViewParent grand = parent.getParent();
        if (grand instanceof android.widget.LinearLayout) {
            android.widget.LinearLayout linearLayout = (android.widget.LinearLayout) grand;
            if (linearLayout.getOrientation() == android.widget.LinearLayout.HORIZONTAL) {
                return (View) grand;
            }
        }
        return parent;
    }

    private static int leftRelativeTo(View view, View ancestor) {
        int left = 0;
        View current = view;
        while (current != null && current != ancestor) {
            left += current.getLeft();
            ViewParent parent = current.getParent();
            if (!(parent instanceof View)) {
                break;
            }
            current = (View) parent;
        }
        return left;
    }

    private static FrameLayout ensureFrameWrapper(View child) {
        ViewParent parent = child.getParent();
        if (parent instanceof FrameLayout) {
            FrameLayout frame = (FrameLayout) parent;
            if (child.getParent() == frame) {
                return frame;
            }
        }
        if (!(parent instanceof ViewGroup)) {
            throw new IllegalStateException("Tab row must have a ViewGroup parent");
        }
        ViewGroup group = (ViewGroup) parent;
        if (group instanceof HorizontalScrollView) {
            return ensureScrollViewHost((HorizontalScrollView) group);
        }
        int index = group.indexOfChild(child);
        if (index < 0) {
            throw new IllegalStateException("Tab row is not attached to its parent");
        }
        ViewGroup.LayoutParams childParams = child.getLayoutParams();
        group.removeView(child);
        FrameLayout wrapper = createOverlayFrame(group);
        wrapper.addView(child, frameChildLayoutParams(childParams));
        ViewGroup.LayoutParams wrapperParams = childParams;
        if (childParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams source = (ViewGroup.MarginLayoutParams) childParams;
            ViewGroup.MarginLayoutParams margins = copyMarginLayoutParams(source, group);
            margins.width = source.width;
            margins.height = source.height;
            wrapperParams = margins;
        }
        group.addView(wrapper, index, wrapperParams);
        return wrapper;
    }

    private static ViewGroup.MarginLayoutParams copyMarginLayoutParams(
            ViewGroup.MarginLayoutParams source,
            ViewGroup parent
    ) {
        if (parent instanceof android.widget.LinearLayout) {
            return new android.widget.LinearLayout.LayoutParams(source);
        }
        if (parent instanceof androidx.constraintlayout.widget.ConstraintLayout) {
            return new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(source);
        }
        return new ViewGroup.MarginLayoutParams(source);
    }

    private static int lerp(int start, int end, float fraction) {
        return Math.round(start + (end - start) * fraction);
    }

    /** Ease-in sine (accelerating). */
    private static float accelerate(float fraction) {
        return (float) (1.0 - Math.cos((fraction * Math.PI) / 2.0));
    }

    /** Ease-out sine (decelerating). */
    private static float decelerate(float fraction) {
        return (float) Math.sin((fraction * Math.PI) / 2.0);
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }

    private static final class TabBounds {
        int left;
        int right;
    }
}
