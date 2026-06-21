package com.veggo.app.presentation.profile;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.veggo.app.core.network.ApiHttpException;
import com.veggo.app.domain.model.Address;
import com.veggo.app.domain.repository.AddressRepository;

import java.util.ArrayList;
import java.util.List;

public class AddressBookViewModel extends ViewModel {
    private final AddressRepository addressRepository;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<List<Address>> addresses = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> retryableError = new MutableLiveData<>(false);
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();

    public AddressBookViewModel(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<List<Address>> getAddresses() { return addresses; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getRetryableError() { return retryableError; }
    public LiveData<String> getSuccessMessage() { return successMessage; }

    public void loadAddresses(String userId) {
        if (userId == null || userId.isEmpty()) {
            retryableError.setValue(false);
            error.setValue("Không tìm thấy thông tin người dùng");
            return;
        }

        loading.setValue(true);
        addressRepository.getAddresses(userId, new AddressRepository.Callback<List<Address>>() {
            @Override
            public void onSuccess(List<Address> result) {
                loading.setValue(false);
                retryableError.setValue(false);
                addresses.setValue(result == null ? new ArrayList<>() : result);
            }

            @Override
            public void onError(Throwable t) {
                loading.setValue(false);
                handleError(t, "Không thể tải danh sách địa chỉ");
            }
        });
    }

    public void deleteAddress(String id, String userId) {
        loading.setValue(true);
        addressRepository.deleteAddress(id, new AddressRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                successMessage.setValue("Đã xóa địa chỉ");
                loadAddresses(userId);
            }

            @Override
            public void onError(Throwable t) {
                loading.setValue(false);
                handleError(t, "Không thể xóa địa chỉ");
            }
        });
    }

    public void setDefaultAddress(String id, String userId) {
        loading.setValue(true);
        addressRepository.setDefaultAddress(id, new AddressRepository.Callback<Address>() {
            @Override
            public void onSuccess(Address result) {
                successMessage.setValue("Đã đặt địa chỉ mặc định");
                loadAddresses(userId);
            }

            @Override
            public void onError(Throwable t) {
                loading.setValue(false);
                handleError(t, "Không thể đặt địa chỉ mặc định");
            }
        });
    }

    public void clearMessages() {
        error.setValue(null);
        successMessage.setValue(null);
    }

    private void handleError(Throwable t, String fallback) {
        if (t instanceof ApiHttpException) {
            retryableError.setValue(false);
            error.setValue(t.getMessage());
            return;
        }
        retryableError.setValue(true);
        error.setValue(fallback + ". Vui lòng thử lại.");
    }
}
