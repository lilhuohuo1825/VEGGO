package com.veggo.app.presentation.checkout;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.veggo.app.MainActivity;
import com.veggo.app.R;
import com.veggo.app.core.ui.BaseActivity;

public class VnpayWebViewActivity extends BaseActivity {

    public static final String EXTRA_PAYMENT_URL  = "extra_vnpay_payment_url";
    public static final String EXTRA_ORDER_ID     = "extra_vnpay_order_id";
    public static final String EXTRA_CUSTOMER_ID  = "extra_vnpay_customer_id";
    public static final String EXTRA_CLEAR_CART   = "extra_vnpay_clear_cart";
    public static final String EXTRA_CLEANUP_SKUS = "extra_vnpay_cleanup_skus";
    public static final String EXTRA_CLEANUP_WEIGHTS = "extra_vnpay_cleanup_weights";

    private static final String DEEP_LINK_SCHEME = "veggo";
    private static final String DEEP_LINK_HOST   = "payment-result";

    private WebView webView;
    private ProgressBar progressBar;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vnpay_webview);

        webView     = findViewById(R.id.vnpayWebView);
        progressBar = findViewById(R.id.vnpayProgressBar);

        findViewById(R.id.btnVnpayBack).setOnClickListener(v -> finish());

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith(DEEP_LINK_SCHEME + "://" + DEEP_LINK_HOST)) {
                    handlePaymentResult(Uri.parse(url));
                    return true;
                }
                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(DEEP_LINK_SCHEME + "://" + DEEP_LINK_HOST)) {
                    handlePaymentResult(Uri.parse(url));
                    return true;
                }
                return false;
            }
        });

        String paymentUrl = getIntent().getStringExtra(EXTRA_PAYMENT_URL);
        if (paymentUrl != null && !paymentUrl.isEmpty()) {
            webView.loadUrl(paymentUrl);
        } else {
            Uri data = getIntent().getData();
            if (data != null && DEEP_LINK_SCHEME.equals(data.getScheme()) && DEEP_LINK_HOST.equals(data.getHost())) {
                handlePaymentResult(data);
            } else {
                Toast.makeText(this, "Lỗi: Không có URL thanh toán", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Uri data = intent.getData();
        if (data != null && DEEP_LINK_SCHEME.equals(data.getScheme()) && DEEP_LINK_HOST.equals(data.getHost())) {
            handlePaymentResult(data);
        }
    }

    private void handlePaymentResult(Uri uri) {
        boolean success = "true".equals(uri.getQueryParameter("success"));
        String orderId  = uri.getQueryParameter("orderId");
        String code     = uri.getQueryParameter("code");

        Intent resultData = new Intent();
        resultData.putExtra("orderId", orderId);
        resultData.putExtra("code", code);

        if (success) {
            setResult(RESULT_OK, resultData);
        } else {
            setResult(RESULT_CANCELED, resultData);
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
