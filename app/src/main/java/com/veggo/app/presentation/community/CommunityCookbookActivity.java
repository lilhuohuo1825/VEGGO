package com.veggo.app.presentation.community;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.veggo.app.R;

public class CommunityCookbookActivity extends AppCompatActivity {
    public static final String EXTRA_COOKBOOK_ID = "community_cookbook_id";

    private CommunityRepository repository;
    private LinearLayout leftColumn;
    private LinearLayout rightColumn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_cookbook);

        repository = new CommunityRepository(this);
        leftColumn = findViewById(R.id.cookbookLeftColumn);
        rightColumn = findViewById(R.id.cookbookRightColumn);
        findViewById(R.id.cookbookBackButton).setOnClickListener(v -> finish());

        String cookbookId = getIntent().getStringExtra(EXTRA_COOKBOOK_ID);
        repository.loadCookbookRecipes(cookbookId, data -> runOnUiThread(() -> {
            if (data.cookbook != null) {
                ((TextView) findViewById(R.id.cookbookTitle)).setText(data.cookbook.getTitle());
                ((TextView) findViewById(R.id.cookbookCount)).setText(data.recipes.size() + " c\u00f4ng th\u1ee9c");
            }
            CommunityUi.addRecipeMasonry(this, leftColumn, rightColumn, data.recipes);
        }));
    }
}
