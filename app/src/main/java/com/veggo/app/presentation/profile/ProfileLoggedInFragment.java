package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseFragment;
import com.veggo.app.presentation.common.AssetScreenData;
import com.veggo.app.presentation.dialog.VeggoDialog;
import com.veggo.app.presentation.order.OrderHistoryActivity;
import com.veggo.app.presentation.about.AboutUsActivity;

public class ProfileLoggedInFragment extends BaseFragment {
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        rootView = inflater.inflate(R.layout.fragment_profile_logged_in, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.profileAccountHeaderCard).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PersonalInfoActivity.class))
        );
        view.findViewById(R.id.profileAddressRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddressBookActivity.class))
        );
        view.findViewById(R.id.profileWalletRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), VeggoPayActivity.class))
        );
        view.findViewById(R.id.profileCarbonRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CarbonPointsActivity.class))
        );

        view.findViewById(R.id.profileFavoritesRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), FavoritesActivity.class))
        );
        view.findViewById(R.id.profileSmartFridgeRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SmartFridgeActivity.class))
        );
        view.findViewById(R.id.profileNotificationsRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PostNotificationsActivity.class))
        );
        view.findViewById(R.id.profilePolicyRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PolicyActivity.class))
        );
        view.findViewById(R.id.profileSupportRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SupportCustomersActivity.class))
        );
        view.findViewById(R.id.profileAboutRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AboutUsActivity.class))
        );
        view.findViewById(R.id.profileOrdersCard).setOnClickListener(v -> openOrders(null));
        view.findViewById(R.id.profileOrderHistoryRow).setOnClickListener(v -> openOrders(null));
        view.findViewById(R.id.profileOrderPendingShortcut).setOnClickListener(v -> openOrders("pending"));
        view.findViewById(R.id.profileOrderShippingShortcut).setOnClickListener(v -> openOrders("shipping"));
        view.findViewById(R.id.profileOrderDeliveredShortcut).setOnClickListener(v -> openOrders("delivered"));
        view.findViewById(R.id.profileOrderCancelledShortcut).setOnClickListener(v -> openOrders("cancelled"));
        view.findViewById(R.id.profileLogoutRow).setOnClickListener(v -> showLogoutDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (rootView != null) {
            loadProfile(rootView);
        }
    }

    private void openOrders(@Nullable String status) {
        Intent intent = new Intent(requireContext(), OrderHistoryActivity.class);
        if (status != null) {
            intent.putExtra(OrderHistoryFragmentExtras.EXTRA_INITIAL_STATUS, status);
        }
        startActivity(intent);
    }

    private void loadProfile(View view) {
        AppPreferences appPreferences = new AppPreferences(requireContext());
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(requireContext());
            AssetModels.User user = snapshot.user;
            if (user == null) {
                user = buildUserFromPreferences(appPreferences);
            }
            AssetModels.User profileUser = user;
            AssetScreenData.Snapshot profileSnapshot = snapshot;
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                bindProfile(view, profileUser, appPreferences);
                bindOrderStatusBadges(view, profileSnapshot);
            });
        }).start();
    }

    @NonNull
    private AssetModels.User buildUserFromPreferences(@NonNull AppPreferences appPreferences) {
        AssetModels.User user = new AssetModels.User();
        user.phone = appPreferences.getCurrentPhone();
        user.customerId = appPreferences.getCustomerId();
        user.fullName = appPreferences.getFullName();
        user.email = appPreferences.getEmail();
        user.avatarUrl = appPreferences.getAvatarUrl();
        user.carbonPoint = 0;
        return user;
    }

    private void bindProfile(
            View view,
            @Nullable AssetModels.User user,
            @NonNull AppPreferences appPreferences
    ) {
        if (user == null) {
            return;
        }

        String name = AssetScreenData.hasText(user.fullName)
                ? user.fullName
                : "Khách hàng " + user.customerId;
        String phone = AssetScreenData.hasText(user.phone)
                ? user.phone
                : appPreferences.getCurrentPhone();
        String avatarUrl = AssetScreenData.hasText(user.avatarUrl)
                ? user.avatarUrl
                : appPreferences.getAvatarUrl();

        AssetScreenData.setText(view, R.id.profileUserName, name);
        AssetScreenData.setText(view, R.id.profileUserPhone, phone);
        AssetScreenData.setText(view, R.id.profileCarbonBadge, user.carbonPoint + " điểm carbon");
        bindAvatar(view, avatarUrl);
    }

    private void bindAvatar(@NonNull View view, @Nullable String avatarUrl) {
        ImageView avatarView = view.findViewById(R.id.profileUserAvatar);
        if (avatarView == null) {
            return;
        }
        if (!AssetScreenData.hasText(avatarUrl)) {
            avatarView.setImageResource(R.drawable.ic_profile_avatar);
            return;
        }
        Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_profile_avatar)
                .error(R.drawable.ic_profile_avatar)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .circleCrop()
                .into(avatarView);
    }

    private void bindOrderStatusBadges(@NonNull View view, @NonNull AssetScreenData.Snapshot snapshot) {
        updateOrderBadge(view.findViewById(R.id.profileOrderPendingBadge),
                AssetScreenData.filterOrders(snapshot, "pending").size());
        updateOrderBadge(view.findViewById(R.id.profileOrderShippingBadge),
                AssetScreenData.filterOrders(snapshot, "shipping").size());
        updateOrderBadge(view.findViewById(R.id.profileOrderDeliveredBadge),
                AssetScreenData.filterOrders(snapshot, "delivered").size());
        updateOrderBadge(view.findViewById(R.id.profileOrderCancelledBadge),
                AssetScreenData.filterOrders(snapshot, "cancelled").size());
    }

    private void updateOrderBadge(@Nullable TextView badgeView, int count) {
        if (badgeView == null) {
            return;
        }
        if (count <= 0) {
            badgeView.setVisibility(View.GONE);
            return;
        }
        badgeView.setText(count > 99 ? "99+" : String.valueOf(count));
        badgeView.setVisibility(View.VISIBLE);
        ViewGroup.LayoutParams params = badgeView.getLayoutParams();
        if (params != null && count > 9) {
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            int horizontalPadding = dp(4);
            int verticalPadding = dp(1);
            badgeView.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
            badgeView.setMinWidth(dp(18));
            badgeView.setMinHeight(dp(18));
        } else if (params != null) {
            params.width = dp(18);
            params.height = dp(18);
            badgeView.setPadding(0, 0, 0, 0);
            badgeView.setMinWidth(0);
            badgeView.setMinHeight(0);
        }
        badgeView.requestLayout();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showLogoutDialog() {
        VeggoDialog.show(
                requireContext(),
                R.drawable.ic_profile_logout,
                "Xác nhận đăng xuất",
                "Bạn có chắc chắn muốn đăng xuất khỏi tài khoản này không?",
                "Đăng xuất",
                "Hủy",
                new VeggoDialog.DialogListener() {
                    @Override
                    public void onConfirm() {
                        logout();
                    }
                }
        );
    }

    private void logout() {
        new AppPreferences(requireContext()).logout();
        new com.veggo.app.core.preferences.PreferencesManager(requireContext()).clearAccessToken();

        Fragment guestFragment = new ProfileFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mainFragmentContainer, guestFragment)
                .commit();
    }
}
