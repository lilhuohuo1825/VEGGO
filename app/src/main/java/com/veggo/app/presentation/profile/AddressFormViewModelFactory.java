package com.veggo.app.presentation.profile;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.veggo.app.domain.repository.AddressRepository;

public class AddressFormViewModelFactory implements ViewModelProvider.Factory {
    private final AddressRepository addressRepository;

    public AddressFormViewModelFactory(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AddressFormViewModel.class)) {
            return (T) new AddressFormViewModel(addressRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
