package com.veggo.app.presentation.community;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.veggo.app.R;

public class CommunityChefsActivity extends AppCompatActivity {
    private LinearLayout container;
    private BottomNavigationView bottomNavigationView;
    private CommunityRepository repository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_chefs);
        container = findViewById(R.id.communityHomeContainer);
        bottomNavigationView = findViewById(R.id.communityBottomNavigation);
        repository = new CommunityRepository(this);
        CommunityUi.setupBottomNav(bottomNavigationView);
        CommunityUi.setupTopHeader(this, "\u0110\u1ea7u b\u1ebfp");
        repository.loadChefs(chefs -> runOnUiThread(() -> CommunityUi.addChefGrid(this, container, chefs)));
    }
}
