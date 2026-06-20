package com.example.smartbin_app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smartbin_app.R;
import com.example.smartbin_app.adapters.LogAdapter;
import com.example.smartbin_app.databinding.ActivityHistoryBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private ActivityHistoryBinding binding;
    private LogAdapter adapter;

    private List<LogAdapter.LogEvent> allLogs = new ArrayList<>();
    private List<LogAdapter.LogEvent> logList = new ArrayList<>();

    private String binId;
    private DatabaseReference logsRef;
    private ValueEventListener historyListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binId = getIntent().getStringExtra("BIN_ID");

        if (binId == null) {
            finish();
            return;
        }

        binding.btnBack.setOnClickListener(v -> finish());

        binding.rvHistoryLogs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LogAdapter(logList);
        binding.rvHistoryLogs.setAdapter(adapter);

        setupFilterListeners();
        loadHistory();
    }

    private void setupFilterListeners() {
        CompoundButton.OnCheckedChangeListener filterListener = (buttonView, isChecked) -> applyFilters();

        binding.chipFire.setOnCheckedChangeListener(filterListener);
        binding.chipSmell.setOnCheckedChangeListener(filterListener);
        binding.chipFull.setOnCheckedChangeListener(filterListener);
        binding.chipEmptied.setOnCheckedChangeListener(filterListener);

        binding.chipWeek.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) binding.chipMonth.setChecked(false);
            applyFilters();
        });
        binding.chipMonth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) binding.chipWeek.setChecked(false);
            applyFilters();
        });
    }

    private void loadHistory() {
        logsRef = FirebaseDatabase.getInstance().getReference("bins").child(binId).child("logs");

        historyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allLogs.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    LogAdapter.LogEvent log = ds.getValue(LogAdapter.LogEvent.class);
                    if (log != null) {
                        allLogs.add(log);
                    }
                }

                allLogs.sort((o1, o2) -> Long.compare(o2.timestamp, o1.timestamp));

                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HistoryActivity.this, R.string.toast_error_history, Toast.LENGTH_SHORT).show();
            }
        };

        logsRef.addValueEventListener(historyListener);
    }

    private void applyFilters() {
        logList.clear();

        boolean filterFire = binding.chipFire.isChecked();
        boolean filterSmell = binding.chipSmell.isChecked();
        boolean filterFull = binding.chipFull.isChecked();
        boolean filterEmptied = binding.chipEmptied.isChecked();
        boolean filterWeek = binding.chipWeek.isChecked();
        boolean filterMonth = binding.chipMonth.isChecked();

        long currentTime = System.currentTimeMillis();
        long weekInMillis = 7L * 24 * 60 * 60 * 1000L;
        long monthInMillis = 30L * 24 * 60 * 60 * 1000L;

        for (LogAdapter.LogEvent log : allLogs) {

            boolean matchesCategory = true;
            if (filterFire || filterSmell || filterFull || filterEmptied) {
                matchesCategory = false;

                if (filterFire && "🔥".equals(log.icon)) matchesCategory = true;
                if (filterSmell && "🤢".equals(log.icon)) matchesCategory = true;
                if (filterFull && "🗑️".equals(log.icon)) matchesCategory = true;
                if (filterEmptied && "♻️".equals(log.icon)) matchesCategory = true;
            }

            boolean matchesTime = true;
            if (filterWeek && (currentTime - log.timestamp > weekInMillis)) {
                matchesTime = false;
            }
            if (filterMonth && (currentTime - log.timestamp > monthInMillis)) {
                matchesTime = false;
            }

            if (matchesCategory && matchesTime) {
                logList.add(log);
            }
        }

        adapter.notifyDataSetChanged();

        binding.tvTotalEvents.setText(getString(R.string.total_events_prefix) + " " + logList.size());
        binding.tvEmptyHistory.setVisibility(logList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (logsRef != null && historyListener != null) {
            logsRef.removeEventListener(historyListener);
        }
    }
}