package com.veggo.app.presentation.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.adapter.WalletTransactionAdapter;
import com.veggo.app.core.ui.BadgeUiHelper;
import com.veggo.app.core.ui.CurvedTabIndicatorHelper;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.preferences.WalletTransactionReadState;
import com.veggo.app.di.AppModule;
import com.veggo.app.data.remote.dto.WalletTransactionDto;
import com.veggo.app.data.repository.WalletRepository;

import java.util.ArrayList;
import java.util.List;

public class VeggoPayHistoryActivity extends BaseActivity {
    private View layoutEmpty;
    private RecyclerView rvTransactions;
    private WalletTransactionAdapter adapter;
    private final List<WalletTransactionDto> transactionsList = new ArrayList<>();
    private final List<WalletTransactionDto> allTransactions = new ArrayList<>();
    private WalletRepository walletRepository;
    private String customerId;

    // Tabs widgets
    private View tabAll, tabReceive, tabSend;
    private TextView tvTabAll, tvTabReceive, tvTabSend;
    private TextView badgeTabAll, badgeTabReceive, badgeTabSend;
    private View indicatorAll, indicatorReceive, indicatorSend;
    private CurvedTabIndicatorHelper tabIndicator;
    private String currentFilter = "all"; // all, receive, send

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LoginRequiredActivity.redirectIfGuest(this, "lịch sử ví")) {
            return;
        }
        setContentView(R.layout.activity_veggopay_history);

        layoutEmpty = findViewById(R.id.layoutEmpty);
        rvTransactions = findViewById(R.id.rvTransactions);

        walletRepository = AppModule.provideWalletRepository();
        customerId = new AppPreferences(this).getCustomerId();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnReadAll).setOnClickListener(v -> markAllTransactionsRead());

        // Initialize tabs
        tabAll = findViewById(R.id.tabAll);
        tabReceive = findViewById(R.id.tabReceive);
        tabSend = findViewById(R.id.tabSend);

        tvTabAll = findViewById(R.id.tvTabAll);
        tvTabReceive = findViewById(R.id.tvTabReceive);
        tvTabSend = findViewById(R.id.tvTabSend);
        badgeTabAll = findViewById(R.id.badgeTabAll);
        badgeTabReceive = findViewById(R.id.badgeTabReceive);
        badgeTabSend = findViewById(R.id.badgeTabSend);

        indicatorAll = findViewById(R.id.indicatorAll);
        indicatorReceive = findViewById(R.id.indicatorReceive);
        indicatorSend = findViewById(R.id.indicatorSend);

        tabIndicator = CurvedTabIndicatorHelper.attach(
                null,
                new CurvedTabIndicatorHelper.TabItem(tabAll, indicatorAll),
                new CurvedTabIndicatorHelper.TabItem(tabReceive, indicatorReceive),
                new CurvedTabIndicatorHelper.TabItem(tabSend, indicatorSend)
        );

        tabAll.setOnClickListener(v -> selectTab("all"));
        tabReceive.setOnClickListener(v -> selectTab("receive"));
        tabSend.setOnClickListener(v -> selectTab("send"));

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WalletTransactionAdapter(transactionsList, new WalletTransactionAdapter.ReadState() {
            @Override
            public boolean isUnread(WalletTransactionDto tx) {
                return WalletTransactionReadState.isUnread(VeggoPayHistoryActivity.this, tx);
            }

            @Override
            public void markRead(WalletTransactionDto tx) {
                WalletTransactionReadState.markRead(VeggoPayHistoryActivity.this, tx);
                updateTabBadges();
            }
        });
        rvTransactions.setAdapter(adapter);

        selectTab("all");
        loadTransactions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTransactions();
    }

    private void selectTab(String filter) {
        currentFilter = filter;
        
        // Update tab styling
        int activeColor = androidx.core.content.ContextCompat.getColor(this, R.color.primary_main);
        int inactiveColor = android.graphics.Color.parseColor("#707070");

        tvTabAll.setTextColor("all".equals(filter) ? activeColor : inactiveColor);
        tvTabReceive.setTextColor("receive".equals(filter) ? activeColor : inactiveColor);
        tvTabSend.setTextColor("send".equals(filter) ? activeColor : inactiveColor);

        if (tabIndicator != null) {
            int index = "receive".equals(filter) ? 1 : "send".equals(filter) ? 2 : 0;
            tabIndicator.selectTab(index);
        }

        updateTabBadges();
        applyFilter();
    }

    private void updateTabBadges() {
        BadgeUiHelper.applyTabBadge(badgeTabAll, countUnreadForFilter("all"), "all".equals(currentFilter));
        BadgeUiHelper.applyTabBadge(badgeTabReceive, countUnreadForFilter("receive"), "receive".equals(currentFilter));
        BadgeUiHelper.applyTabBadge(badgeTabSend, countUnreadForFilter("send"), "send".equals(currentFilter));
    }

    private int countUnreadForFilter(String filter) {
        int count = 0;
        for (WalletTransactionDto tx : allTransactions) {
            if (!WalletTransactionReadState.isUnread(this, tx)) {
                continue;
            }
            double amount = tx.getAmount();
            if ("all".equals(filter)) {
                count++;
            } else if ("receive".equals(filter) && amount >= 0) {
                count++;
            } else if ("send".equals(filter) && amount < 0) {
                count++;
            }
        }
        return count;
    }

    private void applyFilter() {
        transactionsList.clear();
        for (WalletTransactionDto tx : allTransactions) {
            double amount = tx.getAmount();
            if ("all".equals(currentFilter)) {
                transactionsList.add(tx);
            } else if ("receive".equals(currentFilter) && amount >= 0) {
                transactionsList.add(tx);
            } else if ("send".equals(currentFilter) && amount < 0) {
                transactionsList.add(tx);
            }
        }

        if (transactionsList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvTransactions.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvTransactions.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
    }

    private void markAllTransactionsRead() {
        if (!hasUnreadTransactions()) {
            Toast.makeText(this, "Không có thông báo chưa đọc", Toast.LENGTH_SHORT).show();
            return;
        }
        WalletTransactionReadState.markAllRead(this);
        adapter.notifyDataSetChanged();
        updateTabBadges();
        Toast.makeText(this, "Đã đánh dấu tất cả là đã đọc", Toast.LENGTH_SHORT).show();
    }

    private boolean hasUnreadTransactions() {
        for (WalletTransactionDto tx : allTransactions) {
            if (WalletTransactionReadState.isUnread(this, tx)) {
                return true;
            }
        }
        return false;
    }

    private void loadTransactions() {
        if (TextUtils.isEmpty(customerId)) return;
        showProgress("Đang tải lịch sử giao dịch...");
        walletRepository.getTransactions(customerId, new WalletRepository.ResultCallback<List<WalletTransactionDto>>() {
            @Override
            public void onSuccess(List<WalletTransactionDto> result) {
                runOnUiThread(() -> {
                    hideProgress();
                    allTransactions.clear();
                    if (result != null && !result.isEmpty()) {
                        allTransactions.addAll(result);
                    }
                    WalletTransactionReadState.ensureBaselineIfNeeded(VeggoPayHistoryActivity.this, allTransactions);
                    updateTabBadges();
                    applyFilter();
                });
            }

            @Override
            public void onError(Throwable error) {
                runOnUiThread(() -> {
                    hideProgress();
                    Toast.makeText(VeggoPayHistoryActivity.this, "Lỗi tải lịch sử giao dịch", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private android.app.Dialog progressDialog;
    private android.widget.TextView progressTextView;

    private void showProgress(String message) {
        if (progressDialog == null) {
            progressDialog = new android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
            android.widget.RelativeLayout rootLayout = new android.widget.RelativeLayout(this);
            rootLayout.setBackgroundColor(android.graphics.Color.parseColor("#99000000"));
            android.widget.LinearLayout container = new android.widget.LinearLayout(this);
            container.setOrientation(android.widget.LinearLayout.VERTICAL);
            container.setGravity(android.view.Gravity.CENTER);
            android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
            progressTextView = new android.widget.TextView(this);
            progressTextView.setText(message);
            progressTextView.setTextColor(android.graphics.Color.WHITE);
            progressTextView.setTextSize(16);
            progressTextView.setGravity(android.view.Gravity.CENTER);
            progressTextView.setPadding(30, 32, 30, 0);
            container.addView(progressBar);
            container.addView(progressTextView);
            android.widget.RelativeLayout.LayoutParams layoutParams = new android.widget.RelativeLayout.LayoutParams(
                    android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT);
            rootLayout.addView(container, layoutParams);
            progressDialog.setContentView(rootLayout);
            progressDialog.setCancelable(false);
        } else {
            progressTextView.setText(message);
        }
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
