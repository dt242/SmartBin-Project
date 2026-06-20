package com.example.smartbin_app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartbin_app.R;
import com.example.smartbin_app.databinding.ActivitySettingsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference userSettingsRef;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        binding.tvUserEmail.setText(currentUser.getEmail());
        userSettingsRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid()).child("settings");

        loadSettings();
        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.btnSaveSettings.setOnClickListener(v -> saveSettings());

        binding.btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.btn_logout)
                    .setMessage(R.string.dialog_logout_msg)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        mAuth.signOut();
                        Toast.makeText(SettingsActivity.this, R.string.toast_logged_out, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        binding.btnDeleteAccount.setOnClickListener(v -> deleteUserProfile());

    }

    private void loadSettings() {
        userSettingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    if(snapshot.child("nightMode").exists()) {
                        binding.swNightMode.setChecked(Boolean.TRUE.equals(snapshot.child("nightMode").getValue(Boolean.class)));
                    }
                    if(snapshot.child("pushNotifications").exists()) {
                        binding.swPushNotifications.setChecked(Boolean.TRUE.equals(snapshot.child("pushNotifications").getValue(Boolean.class)));
                    } else {
                        binding.swPushNotifications.setChecked(true);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveSettings() {
        boolean pushEnabled = binding.swPushNotifications.isChecked();

        HashMap<String, Object> settingsMap = new HashMap<>();
        settingsMap.put("nightMode", binding.swNightMode.isChecked());
        settingsMap.put("pushNotifications", pushEnabled);

        DatabaseReference userBinsRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid()).child("bins");
        userBinsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String binId = ds.getKey();
                    if (binId != null) {
                        if (pushEnabled) {
                            FirebaseMessaging.getInstance().subscribeToTopic("bin_" + binId);
                        } else {
                            FirebaseMessaging.getInstance().unsubscribeFromTopic("bin_" + binId);
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        userSettingsRef.updateChildren(settingsMap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(SettingsActivity.this, R.string.toast_settings_saved, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(SettingsActivity.this, R.string.toast_settings_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteUserProfile() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_account_title)
                .setMessage(R.string.delete_account_msg)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    String uid = currentUser.getUid();
                    DatabaseReference userBinsRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("bins");
                    DatabaseReference allBinsRef = FirebaseDatabase.getInstance().getReference("bins");

                    userBinsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                String binId = ds.getKey();
                                if (binId != null) {
                                    allBinsRef.child(binId).child("owner").removeValue();
                                }
                            }

                            FirebaseDatabase.getInstance().getReference("users").child(uid).removeValue();

                            currentUser.delete().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(SettingsActivity.this, R.string.toast_settings_error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}