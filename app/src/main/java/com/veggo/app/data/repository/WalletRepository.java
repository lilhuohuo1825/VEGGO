package com.veggo.app.data.repository;

import com.veggo.app.core.network.ApiHttpException;
import com.veggo.app.data.remote.api.WalletApi;
import com.veggo.app.data.remote.dto.WalletDto;
import com.veggo.app.data.remote.dto.WalletResponseDto;
import com.veggo.app.data.remote.dto.WalletTransactionDto;
import com.veggo.app.data.remote.dto.WalletTransactionsResponseDto;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletRepository {
    public interface ResultCallback<T> {
        void onSuccess(T result);
        void onError(Throwable error);
    }

    private final WalletApi walletApi;

    public WalletRepository(WalletApi walletApi) {
        this.walletApi = walletApi;
    }

    public void getWalletInfo(String customerId, ResultCallback<WalletDto> callback) {
        walletApi.getWalletInfo(customerId).enqueue(new Callback<WalletResponseDto>() {
            @Override
            public void onResponse(Call<WalletResponseDto> call, Response<WalletResponseDto> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess() && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                    return;
                }
                callback.onError(new ApiHttpException(response.code(), parseErrorMessage(response)));
            }

            @Override
            public void onFailure(Call<WalletResponseDto> call, Throwable t) {
                callback.onError(new IOException(ChatRepository.mapNetworkError(t), t));
            }
        });
    }

    public void findRecipient(String phone, ResultCallback<Map<String, Object>> callback) {
        walletApi.findRecipient(phone).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }
                String errorMsg = "Không tìm thấy người dùng hoặc ví chưa kích hoạt";
                try {
                    if (response.errorBody() != null) {
                        JSONObject obj = new JSONObject(response.errorBody().string());
                        errorMsg = obj.optString("message", errorMsg);
                    }
                } catch (Exception ignored) {}
                callback.onError(new Exception(errorMsg));
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                callback.onError(new IOException("Lỗi kết nối máy chủ", t));
            }
        });
    }

    public void getTransactions(String customerId, ResultCallback<List<WalletTransactionDto>> callback) {
        walletApi.getTransactions(customerId).enqueue(new Callback<WalletTransactionsResponseDto>() {
            @Override
            public void onResponse(Call<WalletTransactionsResponseDto> call, Response<WalletTransactionsResponseDto> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess() && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                    return;
                }
                callback.onError(new ApiHttpException(response.code(), parseTransactionsErrorMessage(response)));
            }

            @Override
            public void onFailure(Call<WalletTransactionsResponseDto> call, Throwable t) {
                callback.onError(new IOException(ChatRepository.mapNetworkError(t), t));
            }
        });
    }

    public void linkBank(String customerId, String bankCode, String accountNumber, String accountHolder, ResultCallback<WalletDto> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("customerId", customerId);
        body.put("bankCode", bankCode);
        body.put("accountNumber", accountNumber);
        body.put("accountHolder", accountHolder);

        walletApi.linkBank(body).enqueue(new Callback<WalletResponseDto>() {
            @Override
            public void onResponse(Call<WalletResponseDto> call, Response<WalletResponseDto> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess() && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                    return;
                }
                callback.onError(new ApiHttpException(response.code(), parseErrorMessage(response)));
            }

            @Override
            public void onFailure(Call<WalletResponseDto> call, Throwable t) {
                callback.onError(new IOException(ChatRepository.mapNetworkError(t), t));
            }
        });
    }

    public void setDefaultBank(String customerId, String bankCode, String accountNumber, ResultCallback<WalletDto> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("customerId", customerId);
        body.put("bankCode", bankCode);
        body.put("accountNumber", accountNumber);

        walletApi.setDefaultBank(body).enqueue(new Callback<WalletResponseDto>() {
            @Override
            public void onResponse(Call<WalletResponseDto> call, Response<WalletResponseDto> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess() && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                    return;
                }
                callback.onError(new ApiHttpException(response.code(), parseErrorMessage(response)));
            }

            @Override
            public void onFailure(Call<WalletResponseDto> call, Throwable t) {
                callback.onError(new IOException(ChatRepository.mapNetworkError(t), t));
            }
        });
    }

    public void deposit(String customerId, double amount, String bankCode, String password, ResultCallback<WalletDto> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("customerId", customerId);
        body.put("amount", amount);
        body.put("bankCode", bankCode);
        body.put("password", password);

        walletApi.deposit(body).enqueue(new Callback<WalletResponseDto>() {
            @Override
            public void onResponse(Call<WalletResponseDto> call, Response<WalletResponseDto> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess() && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                    return;
                }
                callback.onError(new ApiHttpException(response.code(), parseErrorMessage(response)));
            }

            @Override
            public void onFailure(Call<WalletResponseDto> call, Throwable t) {
                callback.onError(new IOException(ChatRepository.mapNetworkError(t), t));
            }
        });
    }

    public void activateWallet(String customerId, String password, ResultCallback<WalletDto> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("customerId", customerId);
        body.put("password", password);

        walletApi.activateWallet(body).enqueue(new Callback<WalletResponseDto>() {
            @Override
            public void onResponse(Call<WalletResponseDto> call, Response<WalletResponseDto> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess() && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                    return;
                }
                callback.onError(new ApiHttpException(response.code(), parseErrorMessage(response)));
            }

            @Override
            public void onFailure(Call<WalletResponseDto> call, Throwable t) {
                callback.onError(new IOException(ChatRepository.mapNetworkError(t), t));
            }
        });
    }

    public void verifyPassword(String customerId, String password, ResultCallback<Boolean> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("customerId", customerId);
        body.put("password", password);

        walletApi.verifyPassword(body).enqueue(new Callback<WalletResponseDto>() {
            @Override
            public void onResponse(Call<WalletResponseDto> call, Response<WalletResponseDto> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(true);
                    return;
                }
                callback.onError(new ApiHttpException(response.code(), parseErrorMessage(response)));
            }

            @Override
            public void onFailure(Call<WalletResponseDto> call, Throwable t) {
                callback.onError(new IOException(ChatRepository.mapNetworkError(t), t));
            }
        });
    }
    public void transferMoney(String senderCustomerId, String recipientPhone, double amount, String description, String password, ResultCallback<WalletDto> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("senderCustomerId", senderCustomerId);
        body.put("recipientPhone", recipientPhone);
        body.put("amount", amount);
        body.put("description", description);
        body.put("password", password);

        walletApi.transferMoney(body).enqueue(new Callback<WalletResponseDto>() {
            @Override
            public void onResponse(Call<WalletResponseDto> call, Response<WalletResponseDto> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess() && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                    return;
                }
                callback.onError(new ApiHttpException(response.code(), parseErrorMessage(response)));
            }

            @Override
            public void onFailure(Call<WalletResponseDto> call, Throwable t) {
                callback.onError(new IOException(ChatRepository.mapNetworkError(t), t));
            }
        });
    }

    public void getTreeStatus(String customerId, ResultCallback<Map<String, Object>> callback) {
        walletApi.getTreeStatus(customerId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null && Boolean.TRUE.equals(response.body().get("success"))) {
                    callback.onSuccess((Map<String, Object>) response.body().get("data"));
                    return;
                }
                callback.onError(new ApiHttpException(response.code(), "Không thể lấy trạng thái cây trồng"));
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                callback.onError(new IOException(ChatRepository.mapNetworkError(t), t));
            }
        });
    }

    public void activateSeed(String customerId, ResultCallback<Map<String, Object>> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("customerId", customerId);

        walletApi.activateSeed(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null && Boolean.TRUE.equals(response.body().get("success"))) {
                    callback.onSuccess(response.body());
                    return;
                }
                callback.onError(new ApiHttpException(response.code(), parseRawErrorMessage(response)));
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                callback.onError(new IOException(ChatRepository.mapNetworkError(t), t));
            }
        });
    }

    public void waterTree(String customerId, ResultCallback<Map<String, Object>> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("customerId", customerId);

        walletApi.waterTree(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null && Boolean.TRUE.equals(response.body().get("success"))) {
                    callback.onSuccess((Map<String, Object>) response.body().get("data"));
                    return;
                }
                callback.onError(new ApiHttpException(response.code(), parseRawErrorMessage(response)));
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                callback.onError(new IOException(ChatRepository.mapNetworkError(t), t));
            }
        });
    }

    private static String parseRawErrorMessage(Response<?> response) {
        String fallback = "Thao tác thất bại";
        if (response.errorBody() == null) {
            return fallback;
        }
        try {
            String raw = response.errorBody().string();
            org.json.JSONObject json = new org.json.JSONObject(raw);
            if (json.has("message")) {
                return json.getString("message");
            }
            return raw;
        } catch (Exception ignored) {
            return fallback;
        }
    }
    private static String parseErrorMessage(Response<WalletResponseDto> response) {
        String fallback = "Thao tác ví thất bại";
        if (response.errorBody() == null) {
            return fallback;
        }
        try {
            String raw = response.errorBody().string();
            JSONObject json = new JSONObject(raw);
            if (json.has("message")) {
                return json.getString("message");
            }
            return raw;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String parseTransactionsErrorMessage(Response<WalletTransactionsResponseDto> response) {
        String fallback = "Không thể lấy lịch sử giao dịch";
        if (response.errorBody() == null) {
            return fallback;
        }
        try {
            String raw = response.errorBody().string();
            JSONObject json = new JSONObject(raw);
            if (json.has("message")) {
                return json.getString("message");
            }
            return raw;
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
