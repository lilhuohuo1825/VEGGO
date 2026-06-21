package com.veggo.app.domain.repository;

import com.veggo.app.domain.model.Address;

import java.util.List;

public interface AddressRepository {
    interface Callback<T> {
        void onSuccess(T result);
        void onError(Throwable t);
    }

    void getAddresses(String userId, Callback<List<Address>> callback);

    void createAddress(
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
    );

    void updateAddress(
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
    );

    void deleteAddress(String id, Callback<Void> callback);

    void setDefaultAddress(String id, Callback<Address> callback);
}
