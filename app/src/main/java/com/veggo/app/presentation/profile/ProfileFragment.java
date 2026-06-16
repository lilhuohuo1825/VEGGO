package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veggo.app.R;
import com.veggo.app.core.ui.BaseFragment;
import com.veggo.app.presentation.auth.LoginActivity;

public class ProfileFragment extends BaseFragment {
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View.OnClickListener openLoginListener = v ->
                startActivity(new Intent(requireContext(), LoginActivity.class));

        view.findViewById(R.id.profileLoginButton).setOnClickListener(openLoginListener);
        view.findViewById(R.id.profileLoginRegisterRow).setOnClickListener(openLoginListener);
    }
}
