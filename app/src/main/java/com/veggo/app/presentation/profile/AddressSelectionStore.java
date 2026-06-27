package com.veggo.app.presentation.profile;

import androidx.annotation.Nullable;

import com.veggo.app.domain.model.Address;

import java.util.HashMap;
import java.util.Map;

final class AddressSelectionStore {
    private static final Map<String, Address> ADDRESSES = new HashMap<>();

    private AddressSelectionStore() {
    }

    static void put(Address address) {
        if (address != null && address.getId() != null && !address.getId().trim().isEmpty()) {
            ADDRESSES.put(address.getId(), address);
        }
    }

    @Nullable
    static Address get(String addressId) {
        if (addressId == null || addressId.trim().isEmpty()) {
            return null;
        }
        return ADDRESSES.get(addressId);
    }

    static void remove(String addressId) {
        if (addressId != null) {
            ADDRESSES.remove(addressId);
        }
    }
}
