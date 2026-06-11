package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.veggo.app.MainActivity;
import com.veggo.app.R;
import com.veggo.app.databinding.ActivitySupportCustomersBinding;

public class SupportCustomersActivity extends AppCompatActivity {
    private ActivitySupportCustomersBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySupportCustomersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.sendSupportButton.setOnClickListener(v -> Toast.makeText(
                this,
                "Cảm ơn bạn đã liên hệ Veggo. Chúng tôi đã nhận được yêu cầu hỗ trợ và sẽ phản hồi trong thời gian sớm nhất.",
                Toast.LENGTH_LONG
        ).show());

        setupBottomNavigation();
        setupFaqDropdown();
        setupGuideDropdown(
                binding.orderGuideCard,
                new GuideSection[]{
                        new GuideSection(
                                "Bước 1: Chọn sản phẩm",
                                "Truy cập ứng dụng Veggo và tìm sản phẩm bạn muốn mua trong danh mục rau củ, trái cây hoặc thực phẩm tươi sống.\n\nBạn có thể xem thông tin sản phẩm, giá bán, khối lượng và tình trạng còn hàng trước khi thêm vào giỏ."
                        ),
                        new GuideSection(
                                "Bước 2: Thêm vào giỏ hàng",
                                "Nhấn nút “Thêm vào giỏ hàng” sau khi chọn được sản phẩm mong muốn.\n\nBạn có thể điều chỉnh số lượng, xóa sản phẩm hoặc tiếp tục mua thêm các sản phẩm khác."
                        ),
                        new GuideSection(
                                "Bước 3: Kiểm tra đơn hàng",
                                "Vào giỏ hàng để kiểm tra lại danh sách sản phẩm, số lượng, tổng tiền và phí vận chuyển.\n\nNếu có mã giảm giá, hãy nhập mã trước khi chuyển sang bước thanh toán."
                        ),
                        new GuideSection(
                                "Bước 4: Nhập thông tin nhận hàng",
                                "Điền đầy đủ họ tên, số điện thoại và địa chỉ giao hàng.\n\nBạn nên kiểm tra kỹ thông tin để tránh giao nhầm hoặc giao chậm."
                        ),
                        new GuideSection(
                                "Bước 5: Chọn phương thức thanh toán",
                                "Veggo hỗ trợ thanh toán khi nhận hàng, chuyển khoản ngân hàng hoặc các phương thức thanh toán online được tích hợp trong hệ thống.\n\nSau khi hoàn tất, đơn hàng sẽ được Veggo xác nhận và chuẩn bị giao đến bạn."
                        )
                }
        );
        setupGuideDropdown(
                binding.returnGuideCard,
                new GuideSection[]{
                        new GuideSection(
                                "Bước 1: Kiểm tra sản phẩm khi nhận hàng",
                                "Khi nhận hàng, bạn nên kiểm tra tình trạng sản phẩm, số lượng và loại sản phẩm được giao.\n\nNếu phát hiện sản phẩm bị hư hỏng, dập nát, giao sai hoặc thiếu hàng, vui lòng ghi nhận lại ngay."
                        ),
                        new GuideSection(
                                "Bước 2: Chụp ảnh hoặc quay video",
                                "Bạn cần chụp ảnh hoặc quay video rõ tình trạng sản phẩm và bao bì bên ngoài.\n\nĐây là thông tin giúp Veggo xác minh nhanh và hỗ trợ đổi trả chính xác hơn."
                        ),
                        new GuideSection(
                                "Bước 3: Liên hệ bộ phận hỗ trợ",
                                "Gửi thông tin đơn hàng, hình ảnh/video sản phẩm và mô tả vấn đề qua hotline, email hoặc form hỗ trợ trong ứng dụng.\n\nVeggo sẽ tiếp nhận và phản hồi trong thời gian sớm nhất."
                        ),
                        new GuideSection(
                                "Bước 4: Xử lý đổi trả",
                                "Sau khi kiểm tra thông tin, Veggo sẽ đưa ra phương án hỗ trợ phù hợp.\n\nTùy từng trường hợp, bạn có thể được đổi sản phẩm mới, hoàn tiền hoặc nhận ưu đãi bù trừ cho đơn hàng tiếp theo."
                        ),
                        new GuideSection(
                                "Lưu ý",
                                "Đối với nông sản tươi sống, yêu cầu đổi trả nên được gửi trong vòng 24 giờ kể từ khi nhận hàng.\n\nVeggo có thể từ chối hỗ trợ nếu sản phẩm đã qua sử dụng, bảo quản sai cách hoặc không còn đủ thông tin xác minh."
                        )
                }
        );
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_profile);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_profile) {
                return true;
            }
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_SELECTED_NAV_ITEM, item.getItemId());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return true;
        });
    }

    private void setupFaqDropdown() {
        LinearLayout container = (LinearLayout) binding.faqCard.getChildAt(0);
        while (container.getChildCount() > 1) {
            container.removeViewAt(1);
        }

        addFaqItem(
                container,
                new FaqItem(
                        "Tôi có thể hủy đơn hàng sau khi đặt không?",
                        new String[]{
                                "Có. Bạn có thể hủy đơn hàng nếu đơn chưa được xác nhận hoặc chưa bàn giao cho đơn vị vận chuyển.",
                                "Nếu đơn hàng đã được xử lý hoặc đang giao, Veggo sẽ hỗ trợ bạn theo từng trường hợp cụ thể. Vui lòng liên hệ bộ phận hỗ trợ càng sớm càng tốt để được kiểm tra trạng thái đơn hàng."
                        },
                        null
                ),
                false
        );
        addFaqItem(
                container,
                new FaqItem(
                        "Thời gian giao hàng của Veggo là bao lâu?",
                        new String[]{
                                "Chúng tôi cố gắng giao hàng nhanh nhất để rau củ đến tay bạn vẫn tươi ngon như vừa hái.",
                                "Thời gian có thể thay đổi tùy điều kiện thời tiết, lưu thông hoặc khu vực giao hàng. Veggo sẽ luôn cập nhật trạng thái đơn hàng trong suốt quá trình vận chuyển."
                        },
                        new String[]{
                                "Nội thành Hà Nội / TP.HCM: 2 – 4 giờ",
                                "Ngoại thành: 24 – 48 giờ",
                                "Các tỉnh khác: 2 – 5 ngày làm việc"
                        }
                ),
                true
        );
        addFaqItem(
                container,
                new FaqItem(
                        "Nếu sản phẩm có vấn đề thì sao?",
                        new String[]{
                                "Nếu sản phẩm bị hư hỏng, dập nát, giao sai hoặc không đúng chất lượng cam kết, bạn có thể liên hệ Veggo để được hỗ trợ đổi trả.",
                                "Vui lòng chụp ảnh hoặc quay video sản phẩm ngay khi nhận hàng để Veggo có thể kiểm tra và xử lý nhanh hơn.",
                                "Veggo hỗ trợ tiếp nhận phản hồi trong vòng 24 giờ đối với nông sản tươi sống kể từ thời điểm bạn nhận hàng."
                        },
                        null
                ),
                false
        );
        addFaqItem(
                container,
                new FaqItem(
                        "Phí vận chuyển được tính như thế nào?",
                        new String[]{
                                "Phí vận chuyển được tính dựa trên địa chỉ nhận hàng, khối lượng đơn hàng và đơn vị vận chuyển.",
                                "Trước khi xác nhận đặt hàng, hệ thống sẽ hiển thị rõ phí giao hàng để bạn kiểm tra.",
                                "Trong một số chương trình ưu đãi, Veggo có thể miễn phí vận chuyển cho đơn hàng đạt điều kiện nhất định."
                        },
                        null
                ),
                false
        );
        addFaqItem(
                container,
                new FaqItem(
                        "Làm sao để nhận mã giảm giá?",
                        new String[]{
                                "Bạn có thể nhận mã giảm giá thông qua các chương trình khuyến mãi trên ứng dụng, website hoặc fanpage chính thức của Veggo.",
                                "Ngoài ra, khách hàng mới, khách hàng thân thiết hoặc đơn hàng đạt giá trị nhất định có thể được tặng mã ưu đãi riêng.",
                                "Mã giảm giá cần được nhập trước khi xác nhận thanh toán và chỉ áp dụng theo điều kiện của từng chương trình."
                        },
                        null
                ),
                false
        );

        View.OnClickListener listener = v -> toggleChildren(container, binding.faqArrow, 1);
        binding.faqHeader.setOnClickListener(listener);
        binding.faqArrow.setOnClickListener(listener);
        for (int index = 0; index < binding.faqHeader.getChildCount(); index++) {
            binding.faqHeader.getChildAt(index).setOnClickListener(listener);
        }
        setArrow(binding.faqArrow, true);
    }

    private void addFaqItem(LinearLayout parent, FaqItem faqItem, boolean expanded) {
        MaterialCardView itemCard = new MaterialCardView(this);
        itemCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.neutral_10));
        itemCard.setCardElevation(0f);
        itemCard.setRadius(dp(12));
        itemCard.setStrokeColor(0xFFE7E9EB);
        itemCard.setStrokeWidth(dp(1));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = dp(10);
        parent.addView(itemCard, cardParams);

        LinearLayout itemContainer = new LinearLayout(this);
        itemContainer.setOrientation(LinearLayout.VERTICAL);
        itemCard.addView(itemContainer);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(dp(12), 0, dp(12), 0);
        itemContainer.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
        ));

        TextView questionIcon = new TextView(this);
        questionIcon.setGravity(android.view.Gravity.CENTER);
        questionIcon.setText("?");
        questionIcon.setTextColor(ContextCompat.getColor(this, expanded ? R.color.neutral_10 : R.color.neutral_100));
        questionIcon.setTextSize(10);
        questionIcon.setTypeface(Typeface.DEFAULT_BOLD);
        questionIcon.setBackground(createRoundedBackground(
                expanded ? R.color.primary_main : R.color.neutral_20,
                11,
                true
        ));
        header.addView(questionIcon, new LinearLayout.LayoutParams(dp(22), dp(22)));

        TextView title = new TextView(this);
        title.setText(faqItem.title);
        title.setTextColor(ContextCompat.getColor(this, R.color.neutral_100));
        title.setTextSize(12);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        );
        titleParams.leftMargin = dp(12);
        header.addView(title, titleParams);

        ImageView arrow = new ImageView(this);
        arrow.setPadding(dp(8), dp(8), dp(8), dp(8));
        header.addView(arrow, new LinearLayout.LayoutParams(dp(28), dp(28)));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(46), 0, dp(14), dp(14));
        content.setVisibility(expanded ? View.VISIBLE : View.GONE);
        itemContainer.addView(content);

        for (int index = 0; index < faqItem.paragraphs.length; index++) {
            addBodyText(content, faqItem.paragraphs[index], index == 0 ? 0 : 14);
            if (index == 0 && faqItem.bullets != null) {
                addBulletList(content, faqItem.bullets);
            }
        }

        setArrow(arrow, expanded);
        View.OnClickListener listener = v -> {
            boolean nextExpanded = content.getVisibility() != View.VISIBLE;
            content.setVisibility(nextExpanded ? View.VISIBLE : View.GONE);
            questionIcon.setTextColor(ContextCompat.getColor(this, nextExpanded ? R.color.neutral_10 : R.color.neutral_100));
            questionIcon.setBackground(createRoundedBackground(
                    nextExpanded ? R.color.primary_main : R.color.neutral_20,
                    11,
                    true
            ));
            setArrow(arrow, nextExpanded);
        };
        itemCard.setOnClickListener(listener);
        header.setOnClickListener(listener);
        arrow.setOnClickListener(listener);
    }

    private void setupGuideDropdown(MaterialCardView card, GuideSection[] sections) {
        View header = card.getChildAt(0);
        card.removeAllViews();

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        card.addView(container, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        container.addView(header);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setVisibility(View.GONE);
        content.setPadding(dp(14), 0, dp(14), dp(14));
        container.addView(content);

        for (GuideSection section : sections) {
            addGuideSection(content, section);
        }

        ImageView arrow = (ImageView) ((LinearLayout) header).getChildAt(2);
        setArrow(arrow, false);
        View.OnClickListener listener = v -> toggle(content, arrow);
        header.setOnClickListener(listener);
        arrow.setOnClickListener(listener);
        LinearLayout headerRow = (LinearLayout) header;
        for (int index = 0; index < headerRow.getChildCount(); index++) {
            headerRow.getChildAt(index).setOnClickListener(listener);
        }
    }

    private void addGuideSection(LinearLayout parent, GuideSection section) {
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.topMargin = dp(14);
        parent.addView(titleRow, rowParams);

        View bar = new View(this);
        bar.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_main));
        titleRow.addView(bar, new LinearLayout.LayoutParams(dp(3), dp(17)));

        TextView title = new TextView(this);
        title.setText(section.title);
        title.setTextColor(ContextCompat.getColor(this, R.color.neutral_100));
        title.setTextSize(12);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.leftMargin = dp(8);
        titleRow.addView(title, titleParams);

        addBodyText(parent, section.body, 11);
    }

    private void addBodyText(LinearLayout parent, String text, int topMarginDp) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(ContextCompat.getColor(this, R.color.neutral_90));
        textView.setTextSize(12);
        textView.setLineSpacing(dp(4), 1f);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(topMarginDp);
        parent.addView(textView, params);
    }

    private void addBulletList(LinearLayout parent, String[] bullets) {
        for (String bullet : bullets) {
            TextView bulletText = new TextView(this);
            bulletText.setText("• " + bullet);
            bulletText.setTextColor(ContextCompat.getColor(this, R.color.neutral_90));
            bulletText.setTextSize(12);
            bulletText.setLineSpacing(dp(4), 1f);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.topMargin = dp(6);
            parent.addView(bulletText, params);
        }
    }

    private void toggleChildren(LinearLayout container, ImageView arrow, int startIndex) {
        boolean expanded = container.getChildAt(startIndex).getVisibility() == View.VISIBLE;
        int visibility = expanded ? View.GONE : View.VISIBLE;
        for (int index = startIndex; index < container.getChildCount(); index++) {
            container.getChildAt(index).setVisibility(visibility);
        }
        setArrow(arrow, !expanded);
    }

    private void toggle(View content, ImageView arrow) {
        boolean expanded = content.getVisibility() == View.VISIBLE;
        content.setVisibility(expanded ? View.GONE : View.VISIBLE);
        setArrow(arrow, !expanded);
    }

    private void setArrow(ImageView arrow, boolean expanded) {
        arrow.setImageResource(expanded ? R.drawable.ic_chevron_up : R.drawable.ic_chevron_down);
        arrow.setBackground(createRoundedBackground(expanded ? R.color.primary_main : R.color.neutral_20, 14, true));
        arrow.setColorFilter(ContextCompat.getColor(this, expanded ? R.color.neutral_10 : R.color.neutral_70));
    }

    private GradientDrawable createRoundedBackground(int colorRes, int radiusDp, boolean oval) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(oval ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(radiusDp));
        background.setColor(ContextCompat.getColor(this, colorRes));
        return background;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class FaqItem {
        final String title;
        final String[] paragraphs;
        final String[] bullets;

        FaqItem(String title, String[] paragraphs, String[] bullets) {
            this.title = title;
            this.paragraphs = paragraphs;
            this.bullets = bullets;
        }
    }

    private static class GuideSection {
        final String title;
        final String body;

        GuideSection(String title, String body) {
            this.title = title;
            this.body = body;
        }
    }
}
