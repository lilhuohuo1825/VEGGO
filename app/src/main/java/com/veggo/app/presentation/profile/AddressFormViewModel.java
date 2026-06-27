package com.veggo.app.presentation.profile;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.veggo.app.core.address.AddressTreeLoader;
import com.veggo.app.core.address.VietnamAddressTree;
import com.veggo.app.core.network.ApiHttpException;
import com.veggo.app.domain.model.Address;
import com.veggo.app.domain.repository.AddressRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddressFormViewModel extends ViewModel {
    private static final String PHONE_REGEX = "^0\\d{9}$";
    private static final String EMAIL_REGEX = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";

    private final AddressRepository addressRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> treeLoading = new MutableLiveData<>(false);
    private final MutableLiveData<VietnamAddressTree> addressTree = new MutableLiveData<>();
    private final MutableLiveData<Address> savedAddress = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deletedAddress = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> retryableError = new MutableLiveData<>(false);

    public AddressFormViewModel(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Boolean> getTreeLoading() { return treeLoading; }
    public LiveData<VietnamAddressTree> getAddressTree() { return addressTree; }
    public LiveData<Address> getSavedAddress() { return savedAddress; }
    public LiveData<Boolean> getDeletedAddress() { return deletedAddress; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getRetryableError() { return retryableError; }

    public void loadAddressTree() {
        if (addressTree.getValue() != null && !addressTree.getValue().isEmpty()) {
            return;
        }

        treeLoading.setValue(true);
        executor.execute(() -> {
            try {
                VietnamAddressTree tree = AddressTreeLoader.load();
                addressTree.postValue(tree);
                retryableError.postValue(false);
                error.postValue(null);
            } catch (Exception e) {
                retryableError.postValue(true);
                error.postValue("Không thể tải dữ liệu Tỉnh/Huyện/Xã. Vui lòng thử lại.");
            } finally {
                treeLoading.postValue(false);
            }
        });
    }

    public void deleteAddress(@Nullable String addressId) {
        if (addressId == null || addressId.trim().isEmpty()) {
            retryableError.setValue(false);
            error.setValue("Không tìm thấy địa chỉ để xóa");
            return;
        }

        loading.setValue(true);
        addressRepository.deleteAddress(addressId, new AddressRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                loading.setValue(false);
                retryableError.setValue(false);
                deletedAddress.setValue(true);
            }

            @Override
            public void onError(Throwable t) {
                loading.setValue(false);
                handleError(t, "Không thể xóa địa chỉ");
            }
        });
    }

    public void saveAddress(
            @Nullable String addressId,
            String userId,
            String name,
            String phone,
            String email,
            String province,
            String district,
            String ward,
            String detail,
            boolean isDefault
    ) {
        String validationError = validate(userId, name, phone, email, province, district, ward, detail);
        if (validationError != null) {
            retryableError.setValue(false);
            error.setValue(validationError);
            return;
        }

        loading.setValue(true);
        AddressRepository.Callback<Address> callback = new AddressRepository.Callback<Address>() {
            @Override
            public void onSuccess(Address result) {
                loading.setValue(false);
                retryableError.setValue(false);
                savedAddress.setValue(result);
            }

            @Override
            public void onError(Throwable t) {
                loading.setValue(false);
                handleError(t, addressId == null ? "Không thể thêm địa chỉ" : "Không thể cập nhật địa chỉ");
            }
        };

        if (addressId == null || addressId.isEmpty()) {
            addressRepository.createAddress(
                    userId,
                    name.trim(),
                    phone.trim(),
                    email == null ? "" : email.trim(),
                    province.trim(),
                    district.trim(),
                    ward.trim(),
                    detail.trim(),
                    isDefault,
                    callback
            );
        } else {
            addressRepository.updateAddress(
                    addressId,
                    userId,
                    name.trim(),
                    phone.trim(),
                    email == null ? "" : email.trim(),
                    province.trim(),
                    district.trim(),
                    ward.trim(),
                    detail.trim(),
                    isDefault,
                    callback
            );
        }
    }

    @Nullable
    private String validate(
            String userId,
            String name,
            String phone,
            String email,
            String province,
            String district,
            String ward,
            String detail
    ) {
        if (userId == null || userId.trim().isEmpty()) {
            return "Không tìm thấy thông tin người dùng";
        }
        if (name == null || name.trim().isEmpty()) {
            return "Họ và tên không được để trống";
        }
        if (phone == null || !phone.trim().matches(PHONE_REGEX)) {
            return "Số điện thoại phải bắt đầu bằng 0 và gồm đúng 10 chữ số";
        }
        if (email != null && !email.trim().isEmpty() && !email.trim().matches(EMAIL_REGEX)) {
            return "Email không hợp lệ";
        }
        if (province == null || province.trim().isEmpty()) {
            return "Vui lòng chọn Tỉnh/Thành phố";
        }
        if (district == null || district.trim().isEmpty()) {
            return "Vui lòng chọn Quận/Huyện";
        }
        if (ward == null || ward.trim().isEmpty()) {
            return "Vui lòng chọn Phường/Xã";
        }
        if (detail == null || detail.trim().isEmpty()) {
            return "Địa chỉ cụ thể không được để trống";
        }
        return null;
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

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
