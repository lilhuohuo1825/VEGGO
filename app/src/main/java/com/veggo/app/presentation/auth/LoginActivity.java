package com.veggo.app.presentation.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginBehavior;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.veggo.app.MainActivity;
import com.veggo.app.R;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.preferences.PreferencesManager;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.checkout.PendingCheckoutStore;

public class LoginActivity extends BaseActivity {
    private AuthViewModel authViewModel;
    private EditText edtPhone;
    private EditText edtPassword;
    private TextView tvPhoneError;
    private TextView tvPasswordError;
    private ImageView imgEye;
    private View btnLogin;
    private View btnGoogle;
    private View btnFacebook;
    private View progressBar;
    private boolean isPasswordVisible;
    private GoogleSignInClient mGoogleSignInClient;
    private CallbackManager mCallbackManager;
    private FirebaseAuth mAuth;
    @Nullable
    private AuthCredential pendingFacebookLinkCredential;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                android.util.Log.d("VEGGO_AUTH", "Google Sign-In ResultCode: " + result.getResultCode());
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            android.util.Log.d("VEGGO_AUTH", "Google ID Token: " + (account.getIdToken() != null ? "Present" : "NULL"));
                            firebaseAuthWithGoogle(account.getIdToken());
                        }
                    } catch (ApiException e) {
                        android.util.Log.e("VEGGO_AUTH", "Google Sign-In ApiException: " + e.getStatusCode() + " - " + e.getMessage());
                        Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    android.util.Log.e("VEGGO_AUTH", "Google Sign-In Canceled or Failed. ResultCode: " + result.getResultCode());
                    authViewModel.resetLoading();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        AuthFormUtils.setupAuthScreen(this);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        mAuth = FirebaseAuth.getInstance();

        initGoogleSignIn();
        initFacebookSignIn();
        initViews();
        setupActions();
        observeViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Boolean.TRUE.equals(authViewModel.getLoading().getValue())) {
            btnLogin.setEnabled(true);
        }
    }

    private void initFacebookSignIn() {
        mCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        android.util.Log.d("VEGGO_AUTH", "Facebook login success");
                        handleFacebookAccessToken(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        android.util.Log.d("VEGGO_AUTH", "Facebook login cancelled");
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        android.util.Log.e("VEGGO_AUTH", "Facebook login error", exception);
                        Toast.makeText(LoginActivity.this, "Facebook login error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void initGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void initViews() {
        edtPhone = findViewById(R.id.edtPhone);
        edtPassword = findViewById(R.id.edtPassword);
        tvPhoneError = findViewById(R.id.tvPhoneError);
        tvPasswordError = findViewById(R.id.tvPasswordError);
        imgEye = findViewById(R.id.imgEye);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnFacebook = findViewById(R.id.btnFacebook);
//        progressBar = findViewById(R.id.progressBar); // Đảm bảo layout có progressBar hoặc xử lý ẩn/hiện btnLogin

        tvPhoneError.setVisibility(View.GONE);
        tvPasswordError.setVisibility(View.GONE);
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }

    private void setupActions() {
        ImageView imgBack = findViewById(R.id.imgBack);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        TextView tvRegister = findViewById(R.id.tvRegister);

        imgBack.setOnClickListener(v -> finish());
        imgEye.setOnClickListener(v -> togglePasswordVisibility());
        btnLogin.setOnClickListener(v -> handleLogin());
        btnGoogle.setOnClickListener(v -> signInWithGoogle());
        btnFacebook.setOnClickListener(v -> signInWithFacebook());
        edtPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validatePhone(edtPhone.getText().toString().trim());
            }
        });
        edtPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                validatePassword(s.toString());
            }
        });
        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class))
        );
        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    private void observeViewModel() {
        authViewModel.getLoading().observe(this, isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);
            btnLogin.setEnabled(!loading);
            btnLogin.setAlpha(loading ? 0.6f : 1f);
            if (progressBar != null) {
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            }
        });

        authViewModel.getUser().observe(this, userDto -> {
            new AppPreferences(this).saveLoginSession(
                    userDto.getPhone(),
                    userDto.getCustomerId(),
                    userDto.getFullName(),
                    userDto.getEmail(),
                    userDto.getAvatarUrl()
            );
            if (userDto.getAccessToken() != null && !userDto.getAccessToken().trim().isEmpty()) {
                new PreferencesManager(this).saveAccessToken(userDto.getAccessToken().trim());
            }
            Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();

            PendingCheckoutStore pendingCheckoutStore = new PendingCheckoutStore(this);
            if (pendingCheckoutStore.hasPending()) {
                pendingCheckoutStore.openAfterAuth(this, userDto.getCustomerId());
            } else {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra(MainActivity.EXTRA_SELECTED_NAV_ITEM, R.id.nav_profile);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
            finish();
        });

        authViewModel.getError().observe(this, errorMessage -> {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        });
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            imgEye.setImageResource(R.drawable.ic_eye_show_dark);
        } else {
            edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            imgEye.setImageResource(R.drawable.ic_eye_hide_dark);
        }
        edtPassword.setSelection(edtPassword.getText().length());
    }

    private void signInWithGoogle() {
        pendingFacebookLinkCredential = null;
        launchGoogleSignIn();
    }

    private void launchGoogleSignIn() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void signInWithFacebook() {
        LoginManager.getInstance().setLoginBehavior(LoginBehavior.NATIVE_WITH_FALLBACK);
        // Đăng xuất Facebook và Firebase hiện tại để buộc hiển thị màn hình chọn tài khoản
        LoginManager.getInstance().logOut();
        mAuth.signOut();
        LoginManager.getInstance().logInWithReadPermissions(this, mCallbackManager, java.util.Arrays.asList("email", "public_profile"));
    }

    private void handleFacebookAccessToken(AccessToken token) {
        android.util.Log.d("VEGGO_AUTH", "handleFacebookAccessToken appId=" + token.getApplicationId()
                + " userId=" + token.getUserId()
                + " perms=" + token.getPermissions());
        if (token.getToken() == null || token.getToken().isEmpty()) {
            Toast.makeText(this, "Facebook token empty", Toast.LENGTH_SHORT).show();
            return;
        }
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        android.util.Log.d("VEGGO_AUTH", "Facebook Auth Success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            fetchFacebookProfilePicture(token, user);
                        }
                    } else {
                        Exception ex = task.getException();
                        android.util.Log.e("VEGGO_AUTH", "Firebase auth with Facebook failed", ex);
                        if (ex instanceof FirebaseAuthUserCollisionException) {
                            pendingFacebookLinkCredential = credential;
                            Toast.makeText(
                                    LoginActivity.this,
                                    "Email đã đăng ký bằng Google. Vui lòng xác nhận tài khoản Google để tiếp tục.",
                                    Toast.LENGTH_LONG
                            ).show();
                            launchGoogleSignIn();
                            return;
                        }
                        String detail = ex != null && ex.getMessage() != null ? ex.getMessage() : "unknown";
                        Toast.makeText(LoginActivity.this, "Đăng nhập Facebook thất bại: " + detail, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void fetchFacebookProfilePicture(AccessToken token, FirebaseUser firebaseUser) {
        com.facebook.GraphRequest request = com.facebook.GraphRequest.newMeRequest(
                token,
                (object, response) -> {
                    String avatarUrl = null;
                    try {
                        if (object != null && object.has("picture")) {
                            avatarUrl = object.getJSONObject("picture")
                                    .getJSONObject("data")
                                    .getString("url");
                        }
                    } catch (Exception e) {
                        android.util.Log.e("VEGGO_AUTH", "Error parsing Facebook profile picture", e);
                    }
                    
                    final String finalAvatarUrl = avatarUrl;
                    firebaseUser.getIdToken(true).addOnCompleteListener(tokenTask -> {
                        if (tokenTask.isSuccessful()) {
                            String firebaseIdToken = tokenTask.getResult().getToken();
                            authViewModel.facebookLogin(firebaseIdToken, finalAvatarUrl);
                        } else {
                            Toast.makeText(this, "Failed to get Firebase ID Token", Toast.LENGTH_SHORT).show();
                        }
                    });
                });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "picture.type(large)");
        request.setParameters(parameters);
        request.executeAsync();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Pass the activity result back to the Facebook SDK
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        android.util.Log.d("VEGGO_AUTH", "Starting FirebaseAuth with Google Token...");
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        android.util.Log.d("VEGGO_AUTH", "FirebaseAuth Success with Google");
                        if (pendingFacebookLinkCredential != null) {
                            linkPendingFacebookCredential();
                        } else {
                            completeFirebaseLogin(true);
                        }
                    } else {
                        android.util.Log.e("VEGGO_AUTH", "FirebaseAuth Failed: " + task.getException());
                        pendingFacebookLinkCredential = null;
                        Toast.makeText(this, "Firebase authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void linkPendingFacebookCredential() {
        AuthCredential facebookCredential = pendingFacebookLinkCredential;
        pendingFacebookLinkCredential = null;
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || facebookCredential == null) {
            completeFirebaseLogin(true);
            return;
        }
        user.linkWithCredential(facebookCredential)
                .addOnCompleteListener(this, linkTask -> {
                    if (linkTask.isSuccessful()) {
                        android.util.Log.d("VEGGO_AUTH", "Linked Facebook credential to existing Google account");
                    } else {
                        android.util.Log.w("VEGGO_AUTH", "Facebook link skipped: " + linkTask.getException());
                    }
                    completeFirebaseLogin(true);
                });
    }

    /** @param useGoogleBackend true when Firebase session came from Google (incl. link-after-collision). */
    private void completeFirebaseLogin(boolean useGoogleBackend) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Không lấy được thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }
        user.getIdToken(true).addOnCompleteListener(tokenTask -> {
            if (!tokenTask.isSuccessful()) {
                android.util.Log.e("VEGGO_AUTH", "Failed to get Firebase ID Token: " + tokenTask.getException());
                Toast.makeText(this, "Failed to get Firebase ID Token", Toast.LENGTH_SHORT).show();
                return;
            }
            String firebaseIdToken = tokenTask.getResult().getToken();
            android.util.Log.d("VEGGO_AUTH", "Firebase ID Token acquired. Sending to Backend...");
            if (useGoogleBackend) {
                authViewModel.googleLogin(firebaseIdToken);
            } else {
                authViewModel.facebookLogin(firebaseIdToken);
            }
        });
    }

    private void handleLogin() {
        String phone = edtPhone.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        boolean isPhoneValid = validatePhone(phone);
        boolean isPasswordValid = validatePassword(password);
        if (!isPhoneValid || !isPasswordValid) {
            return;
        }

        authViewModel.login(phone, password);
    }

    private boolean validatePhone(String phone) {
        return AuthFormUtils.showError(tvPhoneError, AuthFormUtils.phoneError(phone));
    }

    private boolean validatePassword(String password) {
        return AuthFormUtils.showError(tvPasswordError, AuthFormUtils.loginPasswordError(password));
    }
}
