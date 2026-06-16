package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseFragment;
import com.veggo.app.presentation.common.AssetScreenData;
import com.veggo.app.presentation.order.OrderHistoryActivity;

public class ProfileLoggedInFragment extends BaseFragment {
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_profile_logged_in, container, false);
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
        view.findViewById(R.id.profileCarbonRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CarbonPointsActivity.class))
        );
        view.findViewById(R.id.profilePostsRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MyPostsActivity.class))
        );
        view.findViewById(R.id.profileSmartFridgeRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SmartFridgeActivity.class))
        );
        view.findViewById(R.id.profileTasteRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), TastePreferencesActivity.class))
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
        view.findViewById(R.id.profileOrdersCard).setOnClickListener(v -> openOrders(null));
        view.findViewById(R.id.profileOrderHistoryRow).setOnClickListener(v -> openOrders(null));
        view.findViewById(R.id.profileOrderPendingShortcut).setOnClickListener(v -> openOrders("pending"));
        view.findViewById(R.id.profileOrderShippingShortcut).setOnClickListener(v -> openOrders("shipping"));
        view.findViewById(R.id.profileOrderDeliveredShortcut).setOnClickListener(v -> openOrders("delivered"));
        view.findViewById(R.id.profileOrderCancelledShortcut).setOnClickListener(v -> openOrders("cancelled"));
        view.findViewById(R.id.profileLogoutRow).setOnClickListener(v -> logout());

        loadProfile(view);
    }

    private void openOrders(@Nullable String status) {
        Intent intent = new Intent(requireContext(), OrderHistoryActivity.class);
        if (status != null) {
            intent.putExtra(OrderHistoryFragmentExtras.EXTRA_INITIAL_STATUS, status);
        }
        startActivity(intent);
    }

    private void loadProfile(View view) {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(requireContext());
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> bindProfile(view, snapshot.user));
        }).start();
    }

    private void bindProfile(View view, @Nullable AssetModels.User user) {
        if (user == null) {
            return;
        }

        String name = AssetScreenData.hasText(user.fullName)
                ? user.fullName
                : "Khách hàng " + user.customerId;

        AssetScreenData.setText(view, R.id.profileUserName, name);
        AssetScreenData.setText(view, R.id.profileUserPhone, user.phone);
        AssetScreenData.setText(view, R.id.profileCarbonBadge, user.carbonPoint + " điểm carbon");
    }

    private void logout() {
        new AppPreferences(requireContext()).logout();

        Fragment guestFragment = new ProfileFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mainFragmentContainer, guestFragment)
                .commit();
    }
}
