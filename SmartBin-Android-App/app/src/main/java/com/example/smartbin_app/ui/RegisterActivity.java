package com.example.smartbin_app.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartbin_app.R;
import com.example.smartbin_app.databinding.ActivityRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        setupListeners();
    }

    private void setupListeners() {
        binding.etRegEmail.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.layoutRegEmail.setBackgroundResource(R.drawable.bg_input);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.etRegPassword.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.layoutRegPassword.setBackgroundResource(R.drawable.bg_input);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.etRegConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.layoutRegConfirmPassword.setBackgroundResource(R.drawable.bg_input);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.btnRegister.setOnClickListener(v -> registerUser());
        binding.tvGoToLogin.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String email = binding.etRegEmail.getText().toString().trim();
        String password = binding.etRegPassword.getText().toString().trim();
        String confirmPassword = binding.etRegConfirmPassword.getText().toString().trim();

        if (!isValidInput(email, password, confirmPassword)) return;

        binding.btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            binding.btnRegister.setEnabled(true);

            if (task.isSuccessful()) {
                mAuth.signOut();
                Toast.makeText(RegisterActivity.this, getString(R.string.toast_register_success), Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(RegisterActivity.this, getString(R.string.toast_login_error)  + " " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean isValidInput(String email, String password, String confirmPassword) {
        boolean isValid = true;

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.layoutRegEmail.setBackgroundResource(R.drawable.bg_input_error);
            binding.etRegEmail.setError(getString(R.string.error_invalid_email), null);
            isValid = false;
        }

        if (password.isEmpty() || password.length() < 6) {
            binding.layoutRegPassword.setBackgroundResource(R.drawable.bg_input_error);
            binding.etRegPassword.setError(getString(R.string.error_short_password), null);
            isValid = false;
        }

        if (!password.equals(confirmPassword)) {
            binding.layoutRegConfirmPassword.setBackgroundResource(R.drawable.bg_input_error);
            binding.etRegConfirmPassword.setError(getString(R.string.error_password_mismatch), null);
            isValid = false;
        }

        return isValid;
    }
}