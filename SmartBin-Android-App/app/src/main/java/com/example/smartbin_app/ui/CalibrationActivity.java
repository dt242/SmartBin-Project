package com.example.smartbin_app.ui;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartbin_app.R;
import com.example.smartbin_app.databinding.ActivityCalibrationBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class CalibrationActivity extends AppCompatActivity {

    private ActivityCalibrationBinding binding;
    private String binId;
    private DatabaseReference settingsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCalibrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        binId = getIntent().getStringExtra("BIN_ID");
        if (binId == null) {
            Toast.makeText(this, R.string.error_missing_bin_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        settingsRef = FirebaseDatabase.getInstance().getReference("bins").child(binId).child("settings");

        setupSeekBars();
        loadCurrentSettings();

        binding.btnSaveCalibration.setOnClickListener(v -> saveSettings());
    }

    private void setupSeekBars() {
        binding.sbTriggerDistance.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int actualValue = 5 + (progress * 5);
                binding.tvTriggerDistVal.setText(actualValue + " " + getString(R.string.unit_cm));
            }
        });

        binding.sbOpenAngle.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int actualValue = 30 + (progress * 10);
                binding.tvAngleVal.setText(actualValue + "°");
            }
        });

        binding.sbOpenDuration.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int actualValue = 1 + progress;
                binding.tvDurationVal.setText(actualValue + " " + getString(R.string.unit_sec));
            }
        });

        binding.sbYellowThresh.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int actualValue = 10 + (progress * 10);
                binding.tvYellowVal.setText(actualValue + " %");
            }
        });

        binding.sbRedThresh.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int actualValue = 20 + (progress * 10);
                binding.tvRedVal.setText(actualValue + " %");
            }
        });

        binding.tvTriggerDistVal.setText((5 + (binding.sbTriggerDistance.getProgress() * 5)) + " " + getString(R.string.unit_cm));
        binding.tvAngleVal.setText((30 + (binding.sbOpenAngle.getProgress() * 10)) + "°");
        binding.tvDurationVal.setText((1 + binding.sbOpenDuration.getProgress()) + " " + getString(R.string.unit_sec));
        binding.tvYellowVal.setText((10 + (binding.sbYellowThresh.getProgress() * 10)) + " %");
        binding.tvRedVal.setText((20 + (binding.sbRedThresh.getProgress() * 10)) + " %");
    }

    private void loadCurrentSettings() {
        settingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                if(snapshot.child("triggerDistance").exists()) {
                    int val = snapshot.child("triggerDistance").getValue(Integer.class);
                    binding.sbTriggerDistance.setProgress((val - 5) / 5);
                }
                if(snapshot.child("openAngle").exists()) {
                    int val = snapshot.child("openAngle").getValue(Integer.class);
                    binding.sbOpenAngle.setProgress((val - 30) / 10);
                }
                if(snapshot.child("openDuration").exists()) {
                    int val = snapshot.child("openDuration").getValue(Integer.class) / 1000;
                    binding.sbOpenDuration.setProgress((val - 1) / 1);
                }
                if(snapshot.child("yellowThresh").exists()) {
                    int val = snapshot.child("yellowThresh").getValue(Integer.class);
                    binding.sbYellowThresh.setProgress((val - 10) / 10);
                }
                if(snapshot.child("redThresh").exists()) {
                    int val = snapshot.child("redThresh").getValue(Integer.class);
                    binding.sbRedThresh.setProgress((val - 20) / 10);
                }

                if(snapshot.child("beepOpen").exists()) {
                    binding.swBeepOpen.setChecked(Boolean.TRUE.equals(snapshot.child("beepOpen").getValue(Boolean.class)));
                }
                if(snapshot.child("beepFull").exists()) {
                    binding.swBeepFull.setChecked(Boolean.TRUE.equals(snapshot.child("beepFull").getValue(Boolean.class)));
                }
                if(snapshot.child("beepGas").exists()) {
                    binding.swBeepGas.setChecked(Boolean.TRUE.equals(snapshot.child("beepGas").getValue(Boolean.class)));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveSettings() {
        int trigger = 5 + (binding.sbTriggerDistance.getProgress() * 5);
        int angle = 30 + (binding.sbOpenAngle.getProgress() * 10);
        int duration = (1 + binding.sbOpenDuration.getProgress()) * 1000;

        int yellow = 10 + (binding.sbYellowThresh.getProgress() * 10);
        int red = 20 + (binding.sbRedThresh.getProgress() * 10);

        if (yellow >= red) {
            Toast.makeText(this, R.string.error_threshold_logic, Toast.LENGTH_LONG).show();
            return;
        }



        HashMap<String, Object> newSettings = new HashMap<>();
        newSettings.put("triggerDistance", trigger);
        newSettings.put("openAngle", angle);
        newSettings.put("openDuration", duration);
        newSettings.put("yellowThresh", yellow);
        newSettings.put("redThresh", red);
        newSettings.put("beepOpen", binding.swBeepOpen.isChecked());
        newSettings.put("beepFull", binding.swBeepFull.isChecked());
        newSettings.put("beepGas", binding.swBeepGas.isChecked());

        settingsRef.updateChildren(newSettings).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(CalibrationActivity.this, R.string.toast_settings_saved, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(CalibrationActivity.this, R.string.toast_settings_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public abstract static class SimpleSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    }
}