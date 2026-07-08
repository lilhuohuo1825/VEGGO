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

        // 1. Try local asset tree-completes.json first (extremely fast & reliable)
        try (java.io.InputStream inputStream = com.veggo.app.VeggoApplication.getInstance().getAssets().open("tree-completes.json");
             java.io.InputStreamReader reader = new java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8)) {
            com.google.gson.JsonElement element = new com.google.gson.Gson().fromJson(reader, com.google.gson.JsonElement.class);
            if (element != null) {
                JsonArray array;
                if (element.isJsonArray()) {
                    array = element.getAsJsonArray();
                } else {
                    array = new JsonArray();
                    array.add(element);
                }
                cachedTree = VietnamAddressTree.parse(array);
                if (cachedTree != null && !cachedTree.isEmpty()) {
                    return cachedTree;
                }
            }
        } catch (Throwable t) {
            android.util.Log.e("AddressTreeLoader", "Lỗi tải tree_complete từ assets, sẽ thử qua mạng", t);
        }

        // 2. Try network as backup
        try {
            AddressApi addressApi = ApiClient.createService(AddressApi.class);
            Response<JsonArray> response = addressApi.getTreeComplete().execute();
            if (response.isSuccessful() && response.body() != null) {
                cachedTree = VietnamAddressTree.parse(response.body());
                if (cachedTree != null && !cachedTree.isEmpty()) {
                    return cachedTree;
                }
            }
        } catch (Throwable t) {
            android.util.Log.e("AddressTreeLoader", "Không thể tải tree_complete qua mạng", t);
        }

        throw new IllegalStateException("Không thể tải dữ liệu địa chỉ Việt Nam");
    }

    public static synchronized void clearCache() {
        cachedTree = null;
    }
}
