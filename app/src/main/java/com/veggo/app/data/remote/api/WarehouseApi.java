package com.veggo.app.data.remote.api;

import com.veggo.app.data.remote.dto.WarehouseDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface WarehouseApi {
    @GET("warehouses")
    Call<List<WarehouseDto>> getWarehouses();
}
