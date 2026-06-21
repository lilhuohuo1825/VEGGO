package com.veggo.app.presentation.profile;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.veggo.app.domain.repository.AddressRepository;

public class AddressBookViewModelFactory implements ViewModelProvider.Factory {
    private final AddressRepository addressRepository;

    public AddressBookViewModelFactory(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AddressBookViewModel.class)) {
            return (T) new AddressBookViewModel(addressRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
