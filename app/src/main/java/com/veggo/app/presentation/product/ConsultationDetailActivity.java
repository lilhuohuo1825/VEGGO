package com.veggo.app.presentation.product;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.veggo.app.R;
import com.veggo.app.adapter.ConsultationAdapter;
import com.veggo.app.core.network.ApiHttpException;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.ui.ViewModelFactory;
import com.veggo.app.di.AppModule;
import com.veggo.app.domain.model.Product;
import com.veggo.app.domain.repository.ConsultationRepository;

public class ConsultationDetailActivity extends BaseActivity {

    public static final String EXTRA_PRODUCT_ID = "extra_product_id";
    private ProductViewModel viewModel;
    private ConsultationAdapter adapter;
    private TextView tvQuestionCount;
    private RecyclerView rvQuestions;
    private EditText edtQuestion;
    private ImageView btnSendQuestion;
    private ProgressBar progressSendQuestion;
    private View questionInputContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultation_detail);

        initViews();
        setupViewModel();

        String productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        if (productId != null && !productId.isEmpty()) {
            viewModel.setProductId(productId);
        }

        observeViewModel();
    }

    private void initViews() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        tvQuestionCount = findViewById(R.id.tvQuestionCount);
        edtQuestion = findViewById(R.id.edtQuestion);
        btnSendQuestion = findViewById(R.id.btnSendQuestion);
        progressSendQuestion = findViewById(R.id.progressSendQuestion);
        questionInputContainer = edtQuestion != null ? (View) edtQuestion.getParent() : null;

        rvQuestions = findViewById(R.id.rvQuestions);
        adapter = new ConsultationAdapter();
        setupConsultationAdapterListener();
        rvQuestions.setLayoutManager(new LinearLayoutManager(this));
        rvQuestions.setAdapter(adapter);

        if (btnSendQuestion != null) {
            btnSendQuestion.setOnClickListener(v -> submitQuestion());
        }
    }

    private void submitQuestion() {
        if (edtQuestion == null) return;

        AppPreferences appPreferences = new AppPreferences(this);
        if (!appPreferences.isLoggedIn()
                || appPreferences.getCustomerId() == null
                || appPreferences.getCustomerId().isEmpty()) {
            Toast.makeText(this, R.string.consultation_login_required, Toast.LENGTH_SHORT).show();
            startActivity(new android.content.Intent(this, com.veggo.app.presentation.auth.LoginActivity.class));
            return;
        }

        String questionText = edtQuestion.getText().toString().trim();
        if (questionText.isEmpty()) {
            Toast.makeText(this, R.string.consultation_submit_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        Product product = viewModel.getProduct().getValue();
        if (product == null || product.getSku() == null || product.getSku().isEmpty()) {
            Toast.makeText(this, R.string.consultation_submit_no_sku, Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.submitQuestion(
                product.getSku(),
                questionText,
                appPreferences.getCustomerId(),
                appPreferences.getFullName(),
                product.getName(),
                appPreferences.getAvatarUrl(),
                new ConsultationRepository.Callback<java.util.List<com.veggo.app.domain.model.Consultation>>() {
                    @Override
                    public void onSuccess(java.util.List<com.veggo.app.domain.model.Consultation> result) {
                        runOnUiThread(() -> {
                            Toast.makeText(ConsultationDetailActivity.this,
                                    R.string.consultation_submit_success, Toast.LENGTH_SHORT).show();
                            edtQuestion.setText("");
                            if (result != null && !result.isEmpty()) {
                                rvQuestions.scrollToPosition(result.size() - 1);
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable t) {
                        runOnUiThread(() -> showSubmitError(t, questionText));
                    }
                });
    }

    private void showSubmitError(Throwable t, String questionText) {
        String message;
        if (t instanceof ApiHttpException) {
            int code = ((ApiHttpException) t).getCode();
            if (code == 404) {
                message = getString(R.string.consultation_submit_not_found);
            } else if (code == 400) {
                message = getString(R.string.consultation_submit_empty);
            } else if (code == 401) {
                message = getString(R.string.consultation_login_required);
            } else {
                message = getString(R.string.consultation_submit_network_error);
            }
        } else if (t instanceof IllegalArgumentException) {
            message = getString(R.string.consultation_submit_empty);
        } else {
            message = getString(R.string.consultation_submit_network_error);
        }

        View anchor = questionInputContainer != null ? questionInputContainer : findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(anchor, message, Snackbar.LENGTH_LONG);
        if (t instanceof ApiHttpException && ((ApiHttpException) t).getCode() >= 500
                || !(t instanceof ApiHttpException)) {
            snackbar.setAction(R.string.consultation_submit_retry, v -> {
                edtQuestion.setText(questionText);
                submitQuestion();
            });
        }
        snackbar.show();
    }

    private void setSubmittingUi(boolean submitting) {
        if (btnSendQuestion != null) {
            btnSendQuestion.setEnabled(!submitting);
            btnSendQuestion.setAlpha(submitting ? 0.4f : 1f);
        }
        if (edtQuestion != null) {
            edtQuestion.setEnabled(!submitting);
        }
        if (progressSendQuestion != null) {
            progressSendQuestion.setVisibility(submitting ? View.VISIBLE : View.GONE);
        }
        if (btnSendQuestion != null) {
            btnSendQuestion.setVisibility(submitting ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private void setupViewModel() {
        ViewModelFactory factory = new ViewModelFactory(
                AppModule.provideProductRepository(this),
                AppModule.provideReviewRepository(this),
                AppModule.provideConsultationRepository(this)
        );
        viewModel = new ViewModelProvider(this, factory).get(ProductViewModel.class);
    }

    private void observeViewModel() {
        viewModel.getProduct().observe(this, product -> {
            if (product != null) {
                viewModel.triggerConsultationFetch(product);
            }
        });

        viewModel.getConsultations().observe(this, questions -> {
            AppPreferences prefs = new AppPreferences(this);
            adapter.setCurrentCustomerId(prefs.getCustomerId());
            java.util.List<com.veggo.app.domain.model.Consultation> display =
                    ConsultationUiHelper.filterForUser(questions, prefs.getCustomerId());
            adapter.setQuestions(display);
            if (tvQuestionCount != null) {
                tvQuestionCount.setText(getString(
                        R.string.consultation_question_count_format, display.size()));
            }
        });

        viewModel.isSubmittingQuestion().observe(this, this::setSubmittingUi);
    }

    private void setupConsultationAdapterListener() {
        adapter.setActionListener(new ConsultationAdapter.ActionListener() {
            @Override
            public void onToggleLike(com.veggo.app.domain.model.Consultation question) {
                toggleConsultationLike(question);
            }

            @Override
            public void onSubmitReply(com.veggo.app.domain.model.Consultation question, String content) {
                submitConsultationReply(question, content);
            }

            @Override
            public void onLoginRequired() {
                Toast.makeText(ConsultationDetailActivity.this,
                        R.string.consultation_login_required, Toast.LENGTH_SHORT).show();
                startActivity(new android.content.Intent(
                        ConsultationDetailActivity.this,
                        com.veggo.app.presentation.auth.LoginActivity.class));
            }
        });
    }

    private void toggleConsultationLike(com.veggo.app.domain.model.Consultation question) {
        if (question == null || question.getId() == null || question.getId().isEmpty()) {
            return;
        }
        Product product = viewModel.getProduct().getValue();
        if (product == null || product.getSku() == null || product.getSku().isEmpty()) {
            return;
        }
        AppPreferences prefs = new AppPreferences(this);
        final boolean wasLiked = question.isLikedBy(prefs.getCustomerId());
        viewModel.toggleQuestionLike(
                product.getSku(),
                question.getId(),
                prefs.getCustomerId(),
                prefs.getFullName(),
                new ConsultationRepository.Callback<java.util.List<com.veggo.app.domain.model.Consultation>>() {
                    @Override
                    public void onSuccess(java.util.List<com.veggo.app.domain.model.Consultation> result) {
                        boolean likedNow = !wasLiked;
                        if (result != null) {
                            for (com.veggo.app.domain.model.Consultation item : result) {
                                if (item != null && question.getId().equals(item.getId())) {
                                    likedNow = item.isLikedBy(prefs.getCustomerId());
                                    break;
                                }
                            }
                        }
                        final boolean finalLikedNow = likedNow;
                        runOnUiThread(() -> Toast.makeText(
                                ConsultationDetailActivity.this,
                                finalLikedNow ? R.string.consultation_like_success : R.string.consultation_unlike_success,
                                Toast.LENGTH_SHORT
                        ).show());
                    }

                    @Override
                    public void onError(Throwable t) {
                        runOnUiThread(() -> Toast.makeText(
                                ConsultationDetailActivity.this,
                                R.string.consultation_submit_network_error,
                                Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void submitConsultationReply(com.veggo.app.domain.model.Consultation question, String content) {
        if (question == null || question.getId() == null || question.getId().isEmpty()) {
            return;
        }
        Product product = viewModel.getProduct().getValue();
        if (product == null || product.getSku() == null || product.getSku().isEmpty()) {
            return;
        }
        AppPreferences prefs = new AppPreferences(this);
        viewModel.submitReply(
                product.getSku(),
                question.getId(),
                content,
                prefs.getCustomerId(),
                prefs.getFullName(),
                prefs.getAvatarUrl(),
                new ConsultationRepository.Callback<java.util.List<com.veggo.app.domain.model.Consultation>>() {
                    @Override
                    public void onSuccess(java.util.List<com.veggo.app.domain.model.Consultation> result) {
                        runOnUiThread(() -> Toast.makeText(
                                ConsultationDetailActivity.this,
                                R.string.consultation_reply_success,
                                Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onError(Throwable t) {
                        runOnUiThread(() -> {
                            String message = getString(R.string.consultation_submit_network_error);
                            if (t instanceof ApiHttpException
                                    && ((ApiHttpException) t).getCode() == 400) {
                                message = getString(R.string.consultation_reply_self_error);
                            }
                            Toast.makeText(ConsultationDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }
}
