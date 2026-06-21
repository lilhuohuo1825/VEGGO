package com.veggo.app.core.address;

import com.google.gson.JsonArray;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.data.remote.api.AddressApi;

import retrofit2.Response;

public final class AddressTreeLoader {
    private static VietnamAddressTree cachedTree;

    private AddressTreeLoader() {
    }

    public static synchronized VietnamAddressTree load() throws Exception {
        if (cachedTree != null && !cachedTree.isEmpty()) {
            return cachedTree;
        }

        AddressApi addressApi = ApiClient.createService(AddressApi.class);
        Response<JsonArray> response = addressApi.getTreeComplete().execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new IllegalStateException("Không thể tải dữ liệu địa chỉ Việt Nam");
        }

        cachedTree = VietnamAddressTree.parse(response.body());
        if (cachedTree.isEmpty()) {
            throw new IllegalStateException("Dữ liệu Tỉnh/Huyện/Xã không hợp lệ");
        }
        return cachedTree;
    }

    public static synchronized void clearCache() {
        cachedTree = null;
    }
}
