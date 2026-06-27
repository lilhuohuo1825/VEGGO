package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veggo.app.R;
import com.veggo.app.core.ui.BaseFragment;
import com.veggo.app.presentation.auth.LoginActivity;
import com.veggo.app.presentation.order.OrderHistoryActivity;
import com.veggo.app.presentation.about.AboutUsActivity;

public class ProfileFragment extends BaseFragment {
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Các mục cá nhân cần tài khoản để đồng bộ dữ liệu.
        view.findViewById(R.id.profileAddressRow).setOnClickListener(v ->
                LoginRequiredActivity.open(requireContext(), "sổ địa chỉ")
        );
        view.findViewById(R.id.profileCarbonRow).setOnClickListener(v ->
                LoginRequiredActivity.open(requireContext(), "điểm carbon")
        );
        view.findViewById(R.id.profileFavoritesRow).setOnClickListener(v ->
                LoginRequiredActivity.open(requireContext(), "yêu thích")
        );
        view.findViewById(R.id.profileSmartFridgeRow).setOnClickListener(v ->
                LoginRequiredActivity.open(requireContext(), "tủ lạnh thông minh")
        );
        view.findViewById(R.id.profileTasteRow).setOnClickListener(v ->
                LoginRequiredActivity.open(requireContext(), "khẩu vị của tôi")
        );
        view.findViewById(R.id.profileNotificationsRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PostNotificationsActivity.class))
        );

        // Các mục tĩnh không thay đổi dù đăng nhập hay chưa (Chính sách, Hỗ trợ, Về VEGGO)
        view.findViewById(R.id.profilePolicyRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PolicyActivity.class))
        );
        view.findViewById(R.id.profileSupportRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SupportCustomersActivity.class))
        );
        view.findViewById(R.id.profileAboutRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AboutUsActivity.class))
        );

        // Đơn hàng (mở order history nhưng không có data do chưa đăng nhập)
        view.findViewById(R.id.profileOrdersCard).setOnClickListener(v -> openOrders(null));
        view.findViewById(R.id.profileOrderHistoryRow).setOnClickListener(v -> openOrders(null));
        view.findViewById(R.id.profileOrderPendingShortcut).setOnClickListener(v -> openOrders("pending"));
        view.findViewById(R.id.profileOrderShippingShortcut).setOnClickListener(v -> openOrders("shipping"));
        view.findViewById(R.id.profileOrderDeliveredShortcut).setOnClickListener(v -> openOrders("delivered"));
        view.findViewById(R.id.profileOrderCancelledShortcut).setOnClickListener(v -> openOrders("cancelled"));

        // Nút Đăng nhập / Đăng ký
        View.OnClickListener openLoginListener = v ->
                startActivity(new Intent(requireContext(), LoginActivity.class));

        view.findViewById(R.id.profileLoginButton).setOnClickListener(openLoginListener);
        view.findViewById(R.id.profileLoginRegisterRow).setOnClickListener(openLoginListener);
    }

    private void openOrders(@Nullable String status) {
        Intent intent = new Intent(requireContext(), OrderHistoryActivity.class);
        if (status != null) {
            intent.putExtra(OrderHistoryFragmentExtras.EXTRA_INITIAL_STATUS, status);
        }
        startActivity(intent);
    }
}
