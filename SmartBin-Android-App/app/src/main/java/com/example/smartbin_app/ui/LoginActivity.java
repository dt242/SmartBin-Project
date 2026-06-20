package com.example.smartbin_app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartbin_app.R;
import com.example.smartbin_app.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        setupListeners();
    }

    private void setupListeners() {
        binding.etEmail.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.layoutEmail.setBackgroundResource(R.drawable.bg_input);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.layoutPassword.setBackgroundResource(R.drawable.bg_input);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.btnLogin.setOnClickListener(v -> loginUser());
        binding.tvGoToRegister.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void loginUser() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (!isValidInput(email, password)) return;

        binding.btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            binding.btnLogin.setEnabled(true);

            if (task.isSuccessful()) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(LoginActivity.this, getString(R.string.toast_login_error) + " " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean isValidInput(String email, String password) {
        boolean isValid = true;

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.layoutEmail.setBackgroundResource(R.drawable.bg_input_error);
            binding.etEmail.setError(getString(R.string.error_invalid_email), null);
            isValid = false;
        }

        if (password.isEmpty() || password.length() < 6) {
            binding.layoutPassword.setBackgroundResource(R.drawable.bg_input_error);
            binding.etPassword.setError(getString(R.string.error_short_password), null);
            isValid = false;
        }

        return isValid;
    }
}