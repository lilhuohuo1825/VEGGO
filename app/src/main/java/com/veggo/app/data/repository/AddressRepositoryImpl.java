package com.veggo.app.data.repository;

import com.veggo.app.core.network.ApiHttpException;
import com.veggo.app.data.mapper.AddressMapper;
import com.veggo.app.data.remote.api.AddressApi;
import com.veggo.app.data.remote.dto.AddressDto;
import com.veggo.app.data.remote.dto.AddressRequestDto;
import com.veggo.app.domain.model.Address;
import com.veggo.app.domain.repository.AddressRepository;

import org.json.JSONObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class AddressRepositoryImpl implements AddressRepository {
    private final AddressApi addressApi;

    public AddressRepositoryImpl(AddressApi addressApi) {
        this.addressApi = addressApi;
    }

    @Override
    public void getAddresses(String userId, Callback<List<Address>> callback) {
        addressApi.getAddresses(userId).enqueue(wrapList(callback));
    }

    @Override
    public void createAddress(
            String userId,
            String name,
            String phone,
            String email,
            String province,
            String district,
            String ward,
            String detail,
            boolean isDefault,
            Callback<Address> callback
    ) {
        AddressRequestDto request = new AddressRequestDto(
                userId, name, phone, email, province, district, ward, detail, isDefault
        );
        addressApi.createAddress(request).enqueue(wrapSingle(callback, "Không thể thêm địa chỉ"));
    }

    @Override
    public void updateAddress(
            String id,
            String userId,
            String name,
            String phone,
            String email,
            String province,
            String district,
            String ward,
            String detail,
            boolean isDefault,
            Callback<Address> callback
    ) {
        AddressRequestDto request = new AddressRequestDto(
                userId, name, phone, email, province, district, ward, detail, isDefault
        );
        addressApi.updateAddress(id, request).enqueue(wrapSingle(callback, "Không thể cập nhật địa chỉ"));
    }

    @Override
    public void deleteAddress(String id, Callback<Void> callback) {
        addressApi.deleteAddress(id).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                    return;
                }
                callback.onError(new ApiHttpException(response.code(), parseErrorMessage(response, "Không thể xóa địa chỉ")));
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onError(t);
            }
        });
    }

    @Override
    public void setDefaultAddress(String id, Callback<Address> callback) {
        addressApi.setDefaultAddress(id).enqueue(wrapSingle(callback, "Không thể đặt địa chỉ mặc định"));
    }

    private retrofit2.Callback<List<AddressDto>> wrapList(Callback<List<Address>> callback) {
        return new retrofit2.Callback<List<AddressDto>>() {
            @Override
            public void onResponse(Call<List<AddressDto>> call, Response<List<AddressDto>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(AddressMapper.fromDtoList(response.body()));
                    return;
                }
                callback.onError(new ApiHttpException(
                        response.code(), parseErrorMessage(response, "Không thể tải danh sách địa chỉ")));
            }

            @Override
            public void onFailure(Call<List<AddressDto>> call, Throwable t) {
                callback.onError(t);
            }
        };
    }

    private retrofit2.Callback<AddressDto> wrapSingle(Callback<Address> callback, String fallback) {
        return new retrofit2.Callback<AddressDto>() {
            @Override
            public void onResponse(Call<AddressDto> call, Response<AddressDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(AddressMapper.fromDto(response.body()));
                    return;
                }
                callback.onError(new ApiHttpException(response.code(), parseErrorMessage(response, fallback)));
            }

            @Override
            public void onFailure(Call<AddressDto> call, Throwable t) {
                callback.onError(t);
            }
        };
    }

    private static String parseErrorMessage(Response<?> response, String fallback) {
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
