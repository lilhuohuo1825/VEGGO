package com.veggo.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veggo.app.R;

public class BankSpinnerAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final String[] banks;

    public BankSpinnerAdapter(@NonNull Context context, String[] banks) {
        super(context, R.layout.item_bank_spinner, banks);
        this.context = context;
        this.banks = banks;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createViewFromResource(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createViewFromResource(position, convertView, parent);
    }

    private View createViewFromResource(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_bank_spinner, parent, false);
        }

        ImageView imgLogo = view.findViewById(R.id.imgBankSpinnerLogo);
        TextView tvName = view.findViewById(R.id.tvBankSpinnerName);

        String bankName = banks[position];
        tvName.setText(bankName);

        // Bind corresponding bank logo
        int logoRes = R.drawable.ic_card;
        String codeLower = bankName.toLowerCase();
        if (codeLower.contains("vietcombank") || codeLower.equals("vcb")) {
            logoRes = R.drawable.logo_vcb;
        } else if (codeLower.contains("techcombank") || codeLower.equals("tcb")) {
            logoRes = R.drawable.logo_tcb;
        } else if (codeLower.contains("mb") || codeLower.contains("mbbank")) {
            logoRes = R.drawable.logo_mb;
        } else if (codeLower.contains("bidv")) {
            logoRes = R.drawable.logo_bidv;
        } else if (codeLower.contains("vietin") || codeLower.equals("ctg")) {
            logoRes = R.drawable.logo_ctg;
        }
        imgLogo.setImageResource(logoRes);

        return view;
    }
}
