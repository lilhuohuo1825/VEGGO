package com.veggo.app.presentation.product;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.adapter.ConsultationAdapter;
import com.veggo.app.adapter.ProductAdapter;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.ui.ViewModelFactory;
import com.veggo.app.di.AppModule;

public class ConsultationDetailActivity extends BaseActivity {

    public static final String EXTRA_PRODUCT_ID = "extra_product_id";
    private ProductViewModel viewModel;
    private ConsultationAdapter adapter;
    private ProductAdapter relatedProductAdapter;
    private TextView tvQuestionCount;
    private RecyclerView rvRelatedProducts;
    private EditText edtQuestion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultation_detail);

        initViews();
        setupViewModel();
        
        String productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        if (productId == null || productId.isEmpty()) {
            productId = "68e36b50c0042663fb020b01";
        }
        viewModel.setProductId(productId);
        
        observeViewModel();
    }

    private void initViews() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        
        tvQuestionCount = findViewById(R.id.tvQuestionCount);
        edtQuestion = findViewById(R.id.edtQuestion);
        
        RecyclerView rvConsultationsDetail = findViewById(R.id.rvQuestions);
        adapter = new ConsultationAdapter();
        rvConsultationsDetail.setLayoutManager(new LinearLayoutManager(this));
        rvConsultationsDetail.setAdapter(adapter);

        findViewById(R.id.btnSendQuestion).setOnClickListener(v -> {
            String questionText = edtQuestion.getText().toString().trim();
            if (!questionText.isEmpty()) {
                com.veggo.app.assets.AssetModels.Question newQuestion = new com.veggo.app.assets.AssetModels.Question();
                newQuestion.question = questionText;
                newQuestion.customerName = "Bạn";
                newQuestion.createdAt = "Vừa xong";
                
                adapter.addQuestion(newQuestion);
                
                Toast.makeText(this, "Cảm ơn bạn đã gửi câu hỏi!", Toast.LENGTH_SHORT).show();
                edtQuestion.setText("");
                rvConsultationsDetail.scrollToPosition(0);
                
                if (tvQuestionCount != null) {
                    tvQuestionCount.setText("Câu hỏi (" + adapter.getItemCount() + ")");
                }
            }
        });

        rvRelatedProducts = findViewById(R.id.rvRelatedProducts);
        if (rvRelatedProducts != null) {
            relatedProductAdapter = new ProductAdapter();
            rvRelatedProducts.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
            rvRelatedProducts.setAdapter(relatedProductAdapter);
            
            relatedProductAdapter.setOnProductClickListener(product -> {
                // Navigate to product detail
                android.content.Intent intent = new android.content.Intent(this, ProductDetailActivity.class);
                intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
                startActivity(intent);
            });
        }
    }

    private void setupViewModel() {
        ViewModelFactory factory = new ViewModelFactory(AppModule.provideProductRepository(this));
        viewModel = new ViewModelProvider(this, factory).get(ProductViewModel.class);
    }

    private void observeViewModel() {
        viewModel.getConsultations().observe(this, questions -> {
            if (questions != null) {
                adapter.setQuestions(questions);
                if (tvQuestionCount != null) {
                    tvQuestionCount.setText("Câu hỏi (" + questions.size() + ")");
                }
            }
        });

        // Related products logic
        viewModel.getProduct().observe(this, product -> {
            if (product != null) {
                // For demo, we just get all products as related products
                // viewModel.getRelatedProducts().observe(...) is already active from setupViewModel() if needed
            }
        });

        viewModel.getRelatedProducts().observe(this, products -> {
            if (products != null && !products.isEmpty() && relatedProductAdapter != null) {
                relatedProductAdapter.setProducts(products);
            }
        });
    }
}
