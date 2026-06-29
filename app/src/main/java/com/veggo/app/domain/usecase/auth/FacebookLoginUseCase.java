package com.veggo.app.domain.usecase.auth;

import com.veggo.app.data.remote.dto.UserDto;
import com.veggo.app.domain.repository.AuthRepository;
import retrofit2.Callback;

public class FacebookLoginUseCase {
    private final AuthRepository repository;

    public FacebookLoginUseCase(AuthRepository repository) {
        this.repository = repository;
    }

    public void execute(String idToken, Callback<UserDto> callback) {
        repository.firebaseLogin(idToken, callback);
    }
}
