package com.totalhealthdashboard.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.totalhealthdashboard.R;
import com.totalhealthdashboard.repository.HealthRepository;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private EditText etEmail, etPassword;
    private TextView tvError;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        Task<GoogleSignInAccount> task =
                                GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            firebaseAuthWithGoogle(account.getIdToken());
                        } catch (ApiException e) {
                            showError("Google sign in failed: " + e.getMessage());
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        // If already logged in check onboarding then proceed
        if (auth.getCurrentUser() != null) {
            checkOnboardingAndProceed();
            return;
        }

        etEmail    = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        tvError    = findViewById(R.id.tv_error);

        // Google Sign In setup
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Email login — returning users skip onboarding
        findViewById(R.id.btn_login).setOnClickListener(v -> {
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty()) {
                showError("Please enter email and password");
                return;
            }
            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(r -> checkOnboardingAndProceed())
                    .addOnFailureListener(e -> {
                        if (e instanceof FirebaseAuthException) {
                            // FIX 5 — correct error messages per error type
                            String code = ((FirebaseAuthException) e).getErrorCode();
                            if (code.equals("ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL")) {
                                showError("This email is linked to Google — use " +
                                        "'Continue with Google' instead");
                                return;
                            }
                            if (code.equals("ERROR_WRONG_PASSWORD")
                                    || code.equals("ERROR_INVALID_LOGIN_CREDENTIALS")
                                    || code.equals("ERROR_INVALID_CREDENTIAL")) {
                                showError("Email or password is incorrect");
                                return;
                            }
                            if (code.equals("ERROR_USER_NOT_FOUND")) {
                                showError("Email or password is incorrect");
                                return;
                            }
                        }
                        showError(e.getMessage());
                    });
        });

        // Email register — asks for name then goes to onboarding
        findViewById(R.id.btn_register).setOnClickListener(v -> {
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty()) {
                showError("Please enter email and password");
                return;
            }
            if (password.length() < 6) {
                showError("Password must be at least 6 characters");
                return;
            }
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(r -> showNameDialog(r.getUser()))
                    .addOnFailureListener(e -> showError(e.getMessage()));
        });

        // Google sign in — sign out first so user can pick account
        findViewById(R.id.btn_google_signin).setOnClickListener(v ->
                googleSignInClient.signOut().addOnCompleteListener(task -> {
                    Intent signInIntent = googleSignInClient.getSignInIntent();
                    googleSignInLauncher.launch(signInIntent);
                })
        );

        // FIX 8 — Show/Hide text instead of emoji
        findViewById(R.id.btn_toggle_password).setOnClickListener(v -> {
            TextView btnToggle = (TextView) v;
            if (etPassword.getInputType() ==
                    (android.text.InputType.TYPE_CLASS_TEXT |
                            android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                etPassword.setInputType(
                        android.text.InputType.TYPE_CLASS_TEXT |
                                android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnToggle.setText("Hide");
            } else {
                etPassword.setInputType(
                        android.text.InputType.TYPE_CLASS_TEXT |
                                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnToggle.setText("Show");
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        // FIX 7 — Add spam folder reminder to forgot password message
        findViewById(R.id.btn_forgot_password).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                showError("Enter your email address first");
                return;
            }
            auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(r -> {
                        tvError.setTextColor(0xFF4CAF50);
                        tvError.setText("Password reset email sent to " + email
                                + " — check your spam folder if you don't see it");
                        tvError.setVisibility(View.VISIBLE);
                    })
                    .addOnFailureListener(e -> showError(e.getMessage()));
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(r -> checkOnboardingAndProceed())
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    // Check if goals have been set — new users go to onboarding, returning users go to main
    private void checkOnboardingAndProceed() {
        HealthRepository repo = HealthRepository.getInstance();
        repo.init(this);
        new Thread(() -> {
            boolean hasGoals = repo.hasGoalsBeenSet(this);
            runOnUiThread(() -> {
                if (hasGoals) {
                    goToMain();
                } else {
                    startActivity(new Intent(this, OnboardingActivity.class));
                    finish();
                }
            });
        }).start();
    }

    private void showNameDialog(FirebaseUser user) {
        android.app.AlertDialog.Builder builder =
                new android.app.AlertDialog.Builder(this);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(64, 48, 64, 32);
        layout.setBackgroundColor(0xFFFAFAFA);

        android.widget.TextView tvTitle = new android.widget.TextView(this);
        tvTitle.setText("One last thing");
        tvTitle.setTextSize(22);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(0xFF1A1A1A);
        tvTitle.setPadding(0, 0, 0, 8);
        layout.addView(tvTitle);

        android.widget.TextView tvSubtitle = new android.widget.TextView(this);
        tvSubtitle.setText("What should we call you?");
        tvSubtitle.setTextSize(14);
        tvSubtitle.setTextColor(0xFF9E9E9E);
        tvSubtitle.setPadding(0, 0, 0, 24);
        layout.addView(tvSubtitle);

        final EditText input = new EditText(this);
        input.setHint("First name");
        input.setTextSize(16);
        input.setTextColor(0xFF1A1A1A);
        input.setHintTextColor(0xFFBDBDBD);
        input.setPadding(32, 24, 32, 24);
        input.setBackgroundColor(0xFFF0F0F0);
        input.setSingleLine(true);

        android.widget.LinearLayout.LayoutParams inputParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        inputParams.setMargins(0, 0, 0, 24);
        input.setLayoutParams(inputParams);
        layout.addView(input);

        Button btnContinue = new Button(this);
        btnContinue.setText("Continue →");
        btnContinue.setTextSize(16);
        btnContinue.setTypeface(null, android.graphics.Typeface.BOLD);
        btnContinue.setTextColor(0xFFFFFFFF);
        btnContinue.setBackgroundColor(0xFF1A1A1A);
        btnContinue.setPadding(0, 32, 0, 32);

        android.widget.LinearLayout.LayoutParams btnParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        btnContinue.setLayoutParams(btnParams);
        layout.addView(btnContinue);

        builder.setView(layout);
        builder.setCancelable(false);

        android.app.AlertDialog dialog = builder.create();

        btnContinue.setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                input.setError("Please enter your name");
                return;
            }

            UserProfileChangeRequest profileUpdate =
                    new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build();

            user.updateProfile(profileUpdate)
                    .addOnCompleteListener(task -> {
                        dialog.dismiss();
                        // New user — always go to onboarding
                        startActivity(new Intent(LoginActivity.this, OnboardingActivity.class));
                        finish();
                    });
        });

        dialog.show();
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void showError(String message) {
        tvError.setTextColor(0xFFF44336);
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
}