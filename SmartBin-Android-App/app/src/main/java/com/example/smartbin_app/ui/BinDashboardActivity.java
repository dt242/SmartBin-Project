package com.example.smartbin_app.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartbin_app.R;
import com.example.smartbin_app.databinding.ActivityBinDashboardBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BinDashboardActivity extends AppCompatActivity {

    private ActivityBinDashboardBinding binding;
    private String binId, binName;
    private DatabaseReference binRef;
    private ValueEventListener binListener;
    private boolean isMaintenanceActive = false;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    uploadImageToFirebase(imageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityBinDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binId = getIntent().getStringExtra("BIN_ID");
        binName = getIntent().getStringExtra("BIN_NAME");

        binding.tvBinNameOnly.setText(binName);

        binding.tvBinIdOnly.setText("(ID: " + binId + ")");

        binRef = FirebaseDatabase.getInstance().getReference("bins").child(binId);

        setupClickListeners();
        startListeningToBinData();
        calculatePrediction();
    }

    private void setupClickListeners() {
        binding.layoutPhoto.setOnClickListener(v -> openGallery());
        binding.cvActualPhoto.setOnClickListener(v -> openGallery());

        binding.btnMenuOpen.setOnClickListener(v -> {
            binding.btnMenuOpen.setVisibility(View.GONE);
            binding.layoutMenuExpanded.setVisibility(View.VISIBLE);
        });

        binding.btnMenuClose.setOnClickListener(v -> {
            binding.layoutMenuExpanded.setVisibility(View.GONE);
            binding.btnMenuOpen.setVisibility(View.VISIBLE);
        });

        binding.btnMenuHome.setOnClickListener(v -> finish());

        binding.btnMenuHistory.setOnClickListener(v -> {
            Intent intent = new Intent(BinDashboardActivity.this, HistoryActivity.class);
            intent.putExtra("BIN_ID", binId);
            startActivity(intent);
        });

        binding.btnMenuCalibration.setOnClickListener(v -> {
            Intent intent = new Intent(BinDashboardActivity.this, CalibrationActivity.class);
            intent.putExtra("BIN_ID", binId);
            startActivity(intent);
        });

        binding.btnDashOpen.setOnClickListener(v -> {
            binRef.child("remoteCommand").setValue(1);
            Toast.makeText(this, R.string.toast_opening_command, Toast.LENGTH_SHORT).show();
        });

        binding.btnDashOpen.setOnLongClickListener(v -> {
            binRef.child("remoteCommand").setValue(2);

            if (isMaintenanceActive) {
                Toast.makeText(this, getString(R.string.toast_maintenance_off), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.toast_maintenance_on), Toast.LENGTH_SHORT).show();
            }

            return true;
        });

        binding.cvActualPhoto.setOnLongClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(BinDashboardActivity.this)
                    .setTitle(getString(R.string.dialog_delete_photo_title))
                    .setMessage(getString(R.string.dialog_delete_photo_msg))
                    .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                        binRef.child("photoBase64").removeValue()
                                .addOnSuccessListener(aVoid -> Toast.makeText(this, R.string.toast_photo_deleted, Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton(getString(R.string.no), null)
                    .show();
            return true;
        });
    }

    private void startListeningToBinData() {
        binListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                if (snapshot.child("photoBase64").exists()) {
                    String base64 = snapshot.child("photoBase64").getValue(String.class);
                    if (base64 != null && !base64.isEmpty()) {
                        try {
                            byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            binding.ivBinPhoto.setImageBitmap(decodedByte);
                            binding.layoutPhoto.setVisibility(View.GONE);
                            binding.cvActualPhoto.setVisibility(View.VISIBLE);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        binding.layoutPhoto.setVisibility(View.VISIBLE);
                        binding.cvActualPhoto.setVisibility(View.GONE);
                    }
                } else {
                    binding.layoutPhoto.setVisibility(View.VISIBLE);
                    binding.cvActualPhoto.setVisibility(View.GONE);
                }

                int currentPercent = 0;
                if (snapshot.child("trashPercentage").exists()) {
                    Object percentObj = snapshot.child("trashPercentage").getValue();
                    if (percentObj != null) {
                        currentPercent = Integer.parseInt(String.valueOf(percentObj));
                        binding.tvDashPercent.setText(getString(R.string.level_prefix) + " " + currentPercent + "%");
                    }
                } else {
                    binding.tvDashPercent.setText(getString(R.string.level_empty));
                }

                boolean flame = snapshot.child("flameAlert").exists() && Boolean.TRUE.equals(snapshot.child("flameAlert").getValue(Boolean.class));
                boolean gas = snapshot.child("gasAlert").exists() && Boolean.TRUE.equals(snapshot.child("gasAlert").getValue(Boolean.class));

                isMaintenanceActive = snapshot.child("maintenanceMode").exists() && Boolean.TRUE.equals(snapshot.child("maintenanceMode").getValue(Boolean.class));

                if (isMaintenanceActive) {
                    binding.btnDashOpen.setText(getString(R.string.btn_close_text));
                } else {
                    binding.btnDashOpen.setText(getString(R.string.btn_open_text));
                }

                binding.alertFlame.setVisibility(flame ? View.VISIBLE : View.GONE);
                binding.alertGas.setVisibility(gas ? View.VISIBLE : View.GONE);
                binding.alertFull.setVisibility((currentPercent >= 80) ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        binRef.addValueEventListener(binListener);

    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void uploadImageToFirebase(Uri imageUri) {
        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

            int maxSize = 600;
            float ratio = Math.min((float) maxSize / selectedImage.getWidth(), (float) maxSize / selectedImage.getHeight());
            int width = Math.round(ratio * selectedImage.getWidth());
            int height = Math.round(ratio * selectedImage.getHeight());
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(selectedImage, width, height, false);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            binRef.child("photoBase64").setValue(base64Image)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, R.string.toast_photo_updated, Toast.LENGTH_SHORT).show());

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.toast_photo_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binRef != null && binListener != null) {
            binRef.removeEventListener(binListener);
        }
    }

    private void calculatePrediction() {
        binRef.child("logs").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Long> emptyTimestamps = new ArrayList<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (ds.child("icon").exists() && "♻️".equals(ds.child("icon").getValue(String.class))) {
                        Long ts = ds.child("timestamp").getValue(Long.class);
                        if (ts != null) {
                            emptyTimestamps.add(ts);
                        }
                    }
                }

                if (emptyTimestamps.size() < 2) {
                    binding.tvPrediction.setText(getString(R.string.pred_no_data));
                    return;
                }

                emptyTimestamps.sort((t1, t2) -> Long.compare(t2, t1));

                int limit = Math.min(emptyTimestamps.size(), 5);
                long totalDiff = 0;

                for (int i = 0; i < limit - 1; i++) {
                    totalDiff += (emptyTimestamps.get(i) - emptyTimestamps.get(i + 1));
                }

                long averageFillTime = totalDiff / (limit - 1);

                long predictedNextEmpty = emptyTimestamps.get(0) + averageFillTime;
                long timeLeft = predictedNextEmpty - System.currentTimeMillis();

                if (timeLeft <= 0) {
                    binding.tvPrediction.setText(getString(R.string.pred_full_now));
                } else {
                    int days = (int) (timeLeft / (1000 * 60 * 60 * 24));
                    int hours = (int) ((timeLeft / (1000 * 60 * 60)) % 24);
                    binding.tvPrediction.setText(getString(R.string.pred_estimated) + " " + days + "d " + hours + "h");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
