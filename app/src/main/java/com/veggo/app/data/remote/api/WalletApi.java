package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.WalletResponseDto;
import com.veggo.app.data.remote.dto.WalletTransactionsResponseDto;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface WalletApi {
    @GET("wallet/info")
    Call<WalletResponseDto> getWalletInfo(@Query("customerId") String customerId);

    @GET("wallet/find-recipient")
    Call<Map<String, Object>> findRecipient(@Query("phone") String phone);

    @GET("wallet/transactions")
    Call<WalletTransactionsResponseDto> getTransactions(@Query("customerId") String customerId);

    @POST("wallet/link-bank")
    Call<WalletResponseDto> linkBank(@Body Map<String, Object> body);

    @POST("wallet/set-default-bank")
    Call<WalletResponseDto> setDefaultBank(@Body Map<String, Object> body);

    @POST("wallet/deposit")
    Call<WalletResponseDto> deposit(@Body Map<String, Object> body);

    @POST("wallet/activate")
    Call<WalletResponseDto> activateWallet(@Body Map<String, Object> body);

    @POST("wallet/verify-password")
    Call<WalletResponseDto> verifyPassword(@Body Map<String, Object> body);

    @POST("wallet/transfer")
    Call<WalletResponseDto> transferMoney(@Body Map<String, Object> body);

    @GET("wallet/tree")
    Call<Map<String, Object>> getTreeStatus(@Query("customerId") String customerId);

    @POST("wallet/tree/activate")
    Call<Map<String, Object>> activateSeed(@Body Map<String, Object> body);

    @POST("wallet/tree/water")
    Call<Map<String, Object>> waterTree(@Body Map<String, Object> body);
}
