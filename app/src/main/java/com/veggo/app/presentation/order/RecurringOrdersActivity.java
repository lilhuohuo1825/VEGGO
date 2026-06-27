package com.veggo.app.presentation.order;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.veggo.app.R;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecurringOrdersActivity extends BaseActivity {
    private final SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("dd/MM", new Locale("vi", "VN"));
    private AppPreferences preferences;
    private RecurringOrderStore store;
    private String customerId;
    private Calendar visibleMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recurring_orders);
        preferences = new AppPreferences(this);
        store = new RecurringOrderStore(this);
        customerId = preferences.getCustomerId();
        visibleMonth = Calendar.getInstance();
        visibleMonth.set(Calendar.DAY_OF_MONTH, 1);

        findViewById(R.id.recurringOrdersBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.recurringOrdersMenuButton).setOnClickListener(v ->
                com.veggo.app.presentation.common.AssetScreenData.showOrderOptions(this)
        );
        findViewById(R.id.recurringCreateButton).setOnClickListener(v ->
                startActivity(new Intent(this, CreateRecurringOrderActivity.class))
        );
        findViewById(R.id.recurringPrevMonthButton).setOnClickListener(v -> {
            visibleMonth.add(Calendar.MONTH, -1);
            bindRecurringOrders();
        });
        findViewById(R.id.recurringNextMonthButton).setOnClickListener(v -> {
            visibleMonth.add(Calendar.MONTH, 1);
            bindRecurringOrders();
        });

        if (!preferences.isLoggedIn()) {
            findViewById(R.id.recurringCalendarCard).setVisibility(android.view.View.GONE);
            return;
        }

        bindRecurringOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (preferences != null && preferences.isLoggedIn()) {
            bindRecurringOrders();
        }
    }

    private void bindRecurringOrders() {
        List<RecurringOrderStore.RecurringOrder> orders = store.forCustomer(customerId);
        Calendar calendar = visibleMonth == null ? Calendar.getInstance() : (Calendar) visibleMonth.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        ((TextView) findViewById(R.id.recurringMonthTitle)).setText(
                String.format(Locale.getDefault(), "Tháng %d, %d",
                        calendar.get(Calendar.MONTH) + 1,
                        calendar.get(Calendar.YEAR))
        );
        bindCalendar(calendar, orders);
        bindScheduledList(orders, calendar);
    }

    private void bindCalendar(Calendar month, List<RecurringOrderStore.RecurringOrder> orders) {
        GridLayout grid = findViewById(R.id.recurringCalendarGrid);
        grid.removeAllViews();

        String[] weekdays = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
        for (String weekday : weekdays) {
            TextView header = new TextView(this);
            header.setText(weekday);
            header.setGravity(Gravity.CENTER);
            header.setTextColor(getColor(R.color.neutral_70));
            header.setTextSize(14);
            grid.addView(header, gridParams(34));
        }

        Map<String, Integer> countByDate = countOccurrencesInMonth(month);
        Calendar first = (Calendar) month.clone();
        int leadingDays = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        Calendar cellDate = (Calendar) first.clone();
        cellDate.add(Calendar.DAY_OF_MONTH, -leadingDays);
        int daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH);
        int totalCells = leadingDays + daysInMonth <= 35 ? 35 : 42;
        grid.setRowCount((totalCells / 7) + 1);

        for (int i = 0; i < totalCells; i++) {
            Calendar current = (Calendar) cellDate.clone();
            String storageDate = storageFormat.format(current.getTime());
            int count = countByDate.containsKey(storageDate) ? countByDate.get(storageDate) : 0;
            View cell = createCalendarCell(current, month.get(Calendar.MONTH), count, storageDate);
            grid.addView(cell, gridParams(56));
            cellDate.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private Map<String, Integer> countOccurrencesInMonth(Calendar month) {
        Map<String, Integer> countByDate = new LinkedHashMap<>();
        for (String date : store.occurrenceDatesForCustomerInMonth(customerId, month)) {
            countByDate.put(date, countByDate.containsKey(date) ? countByDate.get(date) + 1 : 1);
        }
        return countByDate;
    }

    private View createCalendarCell(Calendar date, int visibleMonth, int count, String storageDate) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setPadding(dp(5), dp(5), dp(5), dp(5));
        cell.setBackgroundResource(R.drawable.bg_recurring_calendar_cell);
        cell.setOnClickListener(v -> openDay(storageDate, count > 0));

        TextView day = new TextView(this);
        day.setText(String.valueOf(date.get(Calendar.DAY_OF_MONTH)));
        day.setTextSize(14);
        boolean isToday = isToday(date);
        day.setTextColor(getColor(isToday ? R.color.background_main
                : (date.get(Calendar.MONTH) == visibleMonth ? R.color.neutral_100 : R.color.neutral_60)));
        if (isToday) {
            day.setGravity(Gravity.CENTER);
            day.setBackgroundResource(R.drawable.bg_recurring_selected_day);
            cell.addView(day, new LinearLayout.LayoutParams(dp(28), dp(28)));
        } else {
            cell.addView(day);
        }

        if (count > 0) {
            TextView badge = new TextView(this);
            badge.setText(count + " đơn");
            badge.setGravity(Gravity.CENTER);
            badge.setMaxLines(1);
            badge.setTextSize(9);
            badge.setTextColor(getColor(R.color.primary_hover));
            badge.setBackgroundResource(R.drawable.bg_recurring_event_green);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(18)
            );
            params.topMargin = dp(8);
            cell.addView(badge, params);
        }
        return cell;
    }

    private void bindScheduledList(List<RecurringOrderStore.RecurringOrder> orders, Calendar month) {
        LinearLayout container = findViewById(R.id.recurringScheduledOrdersContainer);
        TextView emptyText = findViewById(R.id.recurringEmptyListText);
        container.removeAllViews();
        emptyText.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);

        Map<String, Integer> countByDate = countOccurrencesInMonth(month);

        for (Map.Entry<String, Integer> entry : countByDate.entrySet()) {
            container.addView(createScheduledRow(entry.getKey(), entry.getValue()));
        }
    }

    private View createScheduledRow(String date, int count) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(16), 0, dp(16), 0);
        row.setBackgroundResource(R.drawable.bg_recurring_input);
        row.setOnClickListener(v -> openDay(date, true));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44)
        );
        rowParams.topMargin = dp(4);
        row.setLayoutParams(rowParams);

        TextView dateText = new TextView(this);
        dateText.setText(formatDisplayDay(date));
        dateText.setTextColor(getColor(R.color.neutral_90));
        dateText.setTextSize(14);
        row.addView(dateText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView countText = new TextView(this);
        countText.setText(count + " đơn giao");
        countText.setGravity(Gravity.CENTER);
        countText.setMinWidth(dp(72));
        countText.setPadding(dp(8), 0, dp(8), 0);
        countText.setTextColor(getColor(R.color.primary_hover));
        countText.setTextSize(12);
        countText.setBackgroundResource(R.drawable.bg_recurring_event_green);
        row.addView(countText, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(28)));
        return row;
    }

    private void openDay(String date, boolean hasOrders) {
        Intent intent = new Intent(this, hasOrders ? RecurringDayDetailActivity.class : RecurringDayEmptyActivity.class);
        intent.putExtra(RecurringDayDetailActivity.EXTRA_DELIVERY_DATE, date);
        startActivity(intent);
    }

    private GridLayout.LayoutParams gridParams(int heightDp) {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dp(heightDp);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        return params;
    }

    private Calendar parseDate(String value) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(storageFormat.parse(value));
            return calendar;
        } catch (ParseException | NullPointerException e) {
            return null;
        }
    }

    private String formatDisplayDay(String value) {
        Calendar calendar = parseDate(value);
        return calendar == null ? value : dayFormat.format(calendar.getTime());
    }

    private boolean isToday(Calendar date) {
        Calendar today = Calendar.getInstance();
        return today.get(Calendar.YEAR) == date.get(Calendar.YEAR)
                && today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
