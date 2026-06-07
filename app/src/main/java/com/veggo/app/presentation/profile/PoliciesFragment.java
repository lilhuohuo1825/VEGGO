package com.veggo.app.presentation.profile;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.veggo.app.R;
import com.veggo.app.core.ui.BaseFragment;
import com.veggo.app.databinding.FragmentPoliciesBinding;

public class PoliciesFragment extends BaseFragment {
    private FragmentPoliciesBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentPoliciesBinding.inflate(inflater, container, false);
        binding.backButton.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );
        setupReturnPolicy();
        setupPolicyCards();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupReturnPolicy() {
        setArrow(binding.returnArrow, true);
        binding.returnHeader.setOnClickListener(v -> toggle(binding.returnContent, binding.returnArrow));
        binding.returnCard.setOnClickListener(v ->
                toggle(binding.returnContent, binding.returnArrow)
        );
    }

    private void setupPolicyCards() {
        setupPolicy(
                binding.paymentContent,
                binding.paymentArrow,
                binding.paymentHeader,
                binding.paymentCard,
                new PolicySection[]{
                        new PolicySection(
                                "Phương thức thanh toán",
                                "Veggo hỗ trợ nhiều hình thức thanh toán linh hoạt nhằm mang đến trải nghiệm mua sắm thuận tiện cho khách hàng.\n\nKhách hàng có thể thanh toán bằng các phương thức sau:",
                                new String[]{
                                        "Thanh toán khi nhận hàng",
                                        "Chuyển khoản ngân hàng",
                                        "Thanh toán qua ví điện tử hoặc cổng thanh toán được hỗ trợ",
                                        "Thanh toán bằng mã khuyến mãi hoặc điểm tích lũy nếu có"
                                }
                        ),
                        new PolicySection(
                                "Xác nhận thanh toán",
                                "Đối với hình thức chuyển khoản hoặc thanh toán online, đơn hàng sẽ được xác nhận sau khi hệ thống ghi nhận giao dịch thành công.\n\nTrong một số trường hợp, Veggo có thể liên hệ khách hàng để xác minh thông tin thanh toán trước khi tiến hành xử lý đơn hàng.",
                                null
                        ),
                        new PolicySection(
                                "Lưu ý khi thanh toán",
                                "Khách hàng vui lòng kiểm tra kỹ thông tin đơn hàng, địa chỉ nhận hàng và số tiền cần thanh toán trước khi xác nhận.\n\nVeggo không chịu trách nhiệm đối với các giao dịch phát sinh do khách hàng nhập sai thông tin chuyển khoản hoặc thanh toán nhầm tài khoản không thuộc hệ thống của Veggo.",
                                null
                        )
                }
        );

        setupPolicy(
                binding.privacyContent,
                binding.privacyArrow,
                binding.privacyHeader,
                binding.privacyCard,
                new PolicySection[]{
                        new PolicySection(
                                "Thông tin được thu thập",
                                "Veggo có thể thu thập một số thông tin cần thiết để xử lý đơn hàng và nâng cao chất lượng dịch vụ, bao gồm:",
                                new String[]{"Họ và tên", "Số điện thoại", "Địa chỉ giao hàng", "Email nếu có", "Lịch sử mua hàng và thông tin đơn hàng"}
                        ),
                        new PolicySection(
                                "Mục đích sử dụng thông tin",
                                "Thông tin của khách hàng được sử dụng nhằm:",
                                new String[]{"Xác nhận và giao hàng", "Hỗ trợ chăm sóc khách hàng", "Xử lý đổi trả, bảo hành hoặc khiếu nại", "Gửi thông báo về đơn hàng, ưu đãi hoặc chương trình khuyến mãi nếu khách hàng đồng ý"}
                        ),
                        new PolicySection(
                                "Cam kết bảo mật",
                                "Veggo cam kết không bán, trao đổi hoặc chia sẻ thông tin cá nhân của khách hàng cho bên thứ ba khi chưa có sự đồng ý, trừ trường hợp được yêu cầu bởi cơ quan có thẩm quyền theo quy định pháp luật.\n\nThông tin khách hàng được lưu trữ và bảo vệ bằng các biện pháp phù hợp nhằm hạn chế truy cập trái phép, mất mát hoặc rò rỉ dữ liệu.",
                                null
                        )
                }
        );

        setupPolicy(
                binding.shippingContent,
                binding.shippingArrow,
                binding.shippingHeader,
                binding.shippingCard,
                new PolicySection[]{
                        new PolicySection(
                                "Khu vực giao hàng",
                                "Veggo hỗ trợ giao hàng đến các khu vực nằm trong phạm vi phục vụ của hệ thống. Phạm vi giao hàng có thể thay đổi tùy theo địa chỉ, thời điểm đặt hàng và năng lực vận chuyển.",
                                null
                        ),
                        new PolicySection(
                                "Thời gian giao hàng",
                                "Thời gian giao hàng dự kiến:",
                                new String[]{"Nội thành: trong ngày hoặc từ 1-2 ngày làm việc", "Ngoại thành: từ 2-4 ngày làm việc", "Các khu vực xa hơn: thời gian giao hàng có thể thay đổi tùy đơn vị vận chuyển"}
                        ),
                        new PolicySection(
                                "Phí giao hàng",
                                "Phí giao hàng sẽ được hiển thị trước khi khách hàng xác nhận đặt hàng.\n\nTrong một số chương trình khuyến mãi, khách hàng có thể được miễn phí vận chuyển theo điều kiện cụ thể của từng chương trình.",
                                null
                        ),
                        new PolicySection(
                                "Lưu ý khi nhận hàng",
                                "Khách hàng vui lòng kiểm tra sản phẩm ngay khi nhận hàng.\n\nNếu sản phẩm bị hư hỏng, giao sai hoặc thiếu hàng, khách hàng nên chụp ảnh/quay video làm bằng chứng và liên hệ Veggo trong thời gian sớm nhất để được hỗ trợ.",
                                null
                        )
                }
        );

        setupPolicy(
                binding.warrantyContent,
                binding.warrantyArrow,
                binding.warrantyHeader,
                binding.warrantyCard,
                new PolicySection[]{
                        new PolicySection(
                                "Phạm vi bảo hành",
                                "Chính sách bảo hành áp dụng cho các sản phẩm đủ điều kiện theo quy định của Veggo hoặc nhà cung cấp.\n\nĐối với nông sản tươi sống, sản phẩm không áp dụng bảo hành dài hạn nhưng được hỗ trợ đổi trả trong trường hợp sản phẩm hư hỏng, dập nát, giao sai hoặc không đúng chất lượng cam kết.",
                                null
                        ),
                        new PolicySection(
                                "Điều kiện hỗ trợ bảo hành",
                                "Sản phẩm được hỗ trợ bảo hành hoặc xử lý khiếu nại nếu đáp ứng các điều kiện sau:",
                                new String[]{"Sản phẩm còn trong thời gian hỗ trợ", "Có hình ảnh hoặc video xác minh tình trạng sản phẩm", "Lỗi phát sinh từ quá trình vận chuyển, đóng gói hoặc từ phía cửa hàng", "Khách hàng cung cấp được thông tin đơn hàng hợp lệ"}
                        ),
                        new PolicySection(
                                "Trường hợp không hỗ trợ bảo hành",
                                "Veggo có quyền từ chối bảo hành hoặc hỗ trợ nếu:",
                                new String[]{"Sản phẩm đã quá thời gian quy định", "Sản phẩm bị hư hỏng do bảo quản sai cách", "Khách hàng đã sử dụng hoặc làm thay đổi tình trạng ban đầu của sản phẩm", "Không có đủ thông tin xác minh đơn hàng"}
                        ),
                        new PolicySection(
                                "Quy trình hỗ trợ",
                                "Khách hàng liên hệ Veggo qua hotline, email hoặc kênh hỗ trợ trong ứng dụng.\n\nSau khi tiếp nhận thông tin, Veggo sẽ kiểm tra và phản hồi hướng xử lý phù hợp, bao gồm đổi sản phẩm, hoàn tiền hoặc hỗ trợ theo từng trường hợp cụ thể.",
                                null
                        )
                }
        );
    }

    private void setupPolicy(
            LinearLayout content,
            ImageView arrow,
            View header,
            View card,
            PolicySection[] sections
    ) {
        setArrow(arrow, false);
        content.removeAllViews();
        for (PolicySection section : sections) {
            addSection(content, section);
        }
        header.setOnClickListener(v -> toggle(content, arrow));
        card.setOnClickListener(v -> toggle(content, arrow));
    }

    private void toggle(LinearLayout content, ImageView arrow) {
        boolean expanded = content.getVisibility() == View.VISIBLE;
        content.setVisibility(expanded ? View.GONE : View.VISIBLE);
        setArrow(arrow, !expanded);
    }

    private void setArrow(ImageView arrow, boolean expanded) {
        arrow.setImageResource(expanded ? R.drawable.ic_chevron_up : R.drawable.ic_chevron_down);
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(ContextCompat.getColor(requireContext(), expanded ? R.color.primary_main : R.color.neutral_20));
        arrow.setBackground(background);
        arrow.setColorFilter(ContextCompat.getColor(requireContext(), expanded ? R.color.neutral_10 : R.color.neutral_70));
    }

    private void addSection(LinearLayout parent, PolicySection section) {
        LinearLayout titleRow = new LinearLayout(requireContext());
        titleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams titleRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleRowParams.topMargin = dp(14);
        titleRow.setLayoutParams(titleRowParams);

        View bar = new View(requireContext());
        bar.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary_main));
        titleRow.addView(bar, new LinearLayout.LayoutParams(dp(3), dp(17)));

        TextView title = new TextView(requireContext());
        title.setText(section.title);
        title.setTextColor(Color.parseColor("#222222"));
        title.setTextSize(12);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.leftMargin = dp(8);
        titleRow.addView(title, titleParams);
        parent.addView(titleRow);

        addBody(parent, section.body);
        if (section.bullets != null) {
            for (String bullet : section.bullets) {
                addBullet(parent, bullet);
            }
        }
    }

    private void addBody(LinearLayout parent, String text) {
        TextView body = new TextView(requireContext());
        body.setText(text);
        body.setTextColor(Color.parseColor("#333333"));
        body.setTextSize(12);
        body.setLineSpacing(dp(4), 1f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(11);
        body.setPadding(dp(12), 0, dp(8), 0);
        parent.addView(body, params);
    }

    private void addBullet(LinearLayout parent, String text) {
        TextView bullet = new TextView(requireContext());
        bullet.setText("• " + text);
        bullet.setTextColor(Color.parseColor("#333333"));
        bullet.setTextSize(12);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(8);
        bullet.setPadding(dp(24), 0, dp(8), 0);
        parent.addView(bullet, params);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class PolicySection {
        final String title;
        final String body;
        final String[] bullets;

        PolicySection(String title, String body, String[] bullets) {
            this.title = title;
            this.body = body;
            this.bullets = bullets;
        }
    }
}
