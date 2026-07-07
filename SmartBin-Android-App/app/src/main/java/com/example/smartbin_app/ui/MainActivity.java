package com.example.smartbin_app.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.smartbin_app.R;
import com.example.smartbin_app.adapters.BinAdapter;
import com.example.smartbin_app.adapters.LogAdapter;
import com.example.smartbin_app.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private String currentUserId;

    private List<BinAdapter.Bin> binList = new ArrayList<>();
    private BinAdapter adapter;

    private Map<DatabaseReference, ValueEventListener> activeListeners = new HashMap<>();
    private ValueEventListener myBinsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        setupRecyclerView();
        setupListeners();
        checkPermissions();

        loadMyBins();
    }

    private void setupRecyclerView() {
        binding.recyclerViewBins.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new BinAdapter(binList, new BinAdapter.OnBinClickListener() {
            @Override
            public void onDeleteClick(BinAdapter.Bin bin) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(getString(R.string.dialog_remove_title))
                        .setMessage(getString(R.string.dialog_remove_message) + " " + bin.name + "?")
                        .setPositiveButton(getString(R.string.yes), (dialog, which) -> removeBin(bin.id))
                        .setNegativeButton(getString(R.string.no), null)
                        .show();
            }

            @Override
            public void onBinClick(BinAdapter.Bin bin) {
                Intent intent = new Intent(MainActivity.this, BinDashboardActivity.class);
                intent.putExtra("BIN_ID", bin.id);
                intent.putExtra("BIN_NAME", bin.name);
                startActivity(intent);
            }
        });
        binding.recyclerViewBins.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnAddBin.setOnClickListener(v -> showAddBinDialog());
        binding.btnSettings.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)));

        binding.btnLanguage.setOnClickListener(v -> showLanguageMenu(v));
    }

    private void showLanguageMenu(View anchor) {
        ContextThemeWrapper themedContext = new ContextThemeWrapper(this, R.style.CustomPopupMenuTheme);
        PopupMenu popup = new PopupMenu(themedContext, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_language, popup.getMenu());
        LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
        String currentLangCode = "en";
        if (!currentLocales.isEmpty()) {
            currentLangCode = currentLocales.get(0).getLanguage();
        }
        if (currentLangCode.equals("bg")) {
            popup.getMenu().findItem(R.id.lang_bg).setChecked(true);
        } else if (currentLangCode.equals("es")) {
            popup.getMenu().findItem(R.id.lang_es).setChecked(true);
        } else {
            popup.getMenu().findItem(R.id.lang_en).setChecked(true);
        }
        popup.setOnMenuItemClickListener(item -> {
            String selectedLang = "en";
            int itemId = item.getItemId();
            if (itemId == R.id.lang_bg) {
                selectedLang = "bg";
            } else if (itemId == R.id.lang_es) {
                selectedLang = "es";
            }
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selectedLang));
            return true;
        });
        popup.show();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.POST_NOTIFICATIONS"}, 101);
            }
        }
    }

    private void loadMyBins() {
        DatabaseReference myBinsRef = mDatabase.child("users").child(currentUserId).child("bins");

        myBinsListener = myBinsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                binList.clear();
                clearBinListeners();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String binId = ds.getKey();
                    String binName = ds.getValue(String.class);
                    BinAdapter.Bin newBin = new BinAdapter.Bin(binId, binName);
                    binList.add(newBin);
                    listenToBinLiveStats(newBin);
                    FirebaseMessaging.getInstance().subscribeToTopic("bin_" + binId);
                }
                adapter.notifyDataSetChanged();
                binding.tvEmptyList.setVisibility(binList.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void listenToBinLiveStats(BinAdapter.Bin bin) {
        DatabaseReference binRef = mDatabase.child("bins").child(bin.id);

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    if (snapshot.child("trashPercentage").exists()) bin.percentage = snapshot.child("trashPercentage").getValue(Integer.class);
                    if (snapshot.child("flameAlert").exists()) bin.flameAlert = snapshot.child("flameAlert").getValue(Boolean.class);
                    if (snapshot.child("gasAlert").exists()) bin.gasAlert = snapshot.child("gasAlert").getValue(Boolean.class);

                    adapter.notifyDataSetChanged();
                    checkGlobalAlerts();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        binRef.addValueEventListener(listener);
        activeListeners.put(binRef, listener);
    }

    private void checkGlobalAlerts() {
        boolean hasFlame = false, hasGas = false, hasFull = false;

        for (BinAdapter.Bin b : binList) {
            if (b.flameAlert) hasFlame = true;
            if (b.gasAlert) hasGas = true;
            if (b.percentage >= 80) hasFull = true;
        }

        binding.alertFlameMain.setVisibility(hasFlame ? View.VISIBLE : View.GONE);
        binding.alertGasMain.setVisibility(hasGas ? View.VISIBLE : View.GONE);
        binding.alertFullMain.setVisibility(hasFull ? View.VISIBLE : View.GONE);
    }

    private void removeBin(String binId) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic("bin_" + binId);
        mDatabase.child("users").child(currentUserId).child("bins").child(binId).removeValue();
        mDatabase.child("bins").child(binId).child("owner").removeValue();
        Toast.makeText(this, R.string.toast_bin_removed, Toast.LENGTH_SHORT).show();
    }

    private void showAddBinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_add_title);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText etBinId = new EditText(this);
        etBinId.setHint(R.string.hint_bin_id);
        layout.addView(etBinId);

        final EditText etBinName = new EditText(this);
        etBinName.setHint(R.string.hint_bin_name);
        layout.addView(etBinName);

        builder.setView(layout);

        builder.setPositiveButton(R.string.add, (dialog, which) -> {
            String binId = etBinId.getText().toString().trim();
            String binName = etBinName.getText().toString().trim();

            if (TextUtils.isEmpty(binId) || TextUtils.isEmpty(binName)) return;
            claimBin(binId, binName);
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void claimBin(String binId, String binName) {
        DatabaseReference binRef = mDatabase.child("bins").child(binId);

        binRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String owner = task.getResult().child("owner").getValue(String.class);

                if (owner == null || owner.equals(currentUserId)) {
                    binRef.child("owner").setValue(currentUserId);
                    binRef.child("name").setValue(binName);
                    mDatabase.child("users").child(currentUserId).child("bins").child(binId).setValue(binName);
                    Toast.makeText(MainActivity.this, R.string.toast_bin_added, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, R.string.error_bin_taken, Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(MainActivity.this, R.string.error_invalid_bin_id, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void clearBinListeners() {
        for (Map.Entry<DatabaseReference, ValueEventListener> entry : activeListeners.entrySet()) {
            entry.getKey().removeEventListener(entry.getValue());
        }
        activeListeners.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (myBinsListener != null) {
            mDatabase.child("users").child(currentUserId).child("bins").removeEventListener(myBinsListener);
        }
        clearBinListeners();
    }
}
