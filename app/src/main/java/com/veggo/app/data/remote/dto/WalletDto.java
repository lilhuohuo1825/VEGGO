package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class WalletDto {
    private String customerId;
    private double balance;
    private String status;
    private List<LinkedBankDto> linkedBanks;

    public String getCustomerId() {
        return customerId;
    }

    public double getBalance() {
        return balance;
    }

    public String getStatus() {
        return status;
    }

    public List<LinkedBankDto> getLinkedBanks() {
        return linkedBanks;
    }

    public static class LinkedBankDto {
        private String bankCode;
        private String accountNumber;
        private String accountHolder;
        private boolean isDefault;

        public String getBankCode() {
            return bankCode;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public String getAccountHolder() {
            return accountHolder;
        }

        public boolean isDefault() {
            return isDefault;
        }
    }
}
