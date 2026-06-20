package com.example.smartbin_app.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartbin_app.databinding.ItemLogBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    public static class LogEvent {
        public long timestamp;
        public String icon;
        public String message;

        public LogEvent() {}

        public LogEvent(long timestamp, String icon, String message) {
            this.timestamp = timestamp;
            this.icon = icon;
            this.message = message;
        }
    }

    private List<LogEvent> logList;

    public LogAdapter(List<LogEvent> logList) {
        this.logList = logList;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLogBinding binding = ItemLogBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new LogViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        LogEvent log = logList.get(position);

        holder.binding.tvLogIcon.setText(log.icon);
        holder.binding.tvLogMessage.setText(log.message);

        String dateStr = sdf.format(new Date(log.timestamp));
        holder.binding.tvLogDate.setText(dateStr);
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    public static class LogViewHolder extends RecyclerView.ViewHolder {
        ItemLogBinding binding;

        public LogViewHolder(@NonNull ItemLogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}