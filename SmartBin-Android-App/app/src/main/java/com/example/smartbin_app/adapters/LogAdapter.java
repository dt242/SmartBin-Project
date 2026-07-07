package com.example.smartbin_app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartbin_app.R;
import com.example.smartbin_app.databinding.ItemLogBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    public static class LogEvent {
        public long timestamp;
        public String icon;
        public String message;
        public String eventCode;

        public LogEvent() {}
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
        Context ctx = holder.itemView.getContext();

        holder.binding.tvLogIcon.setText(log.icon);

        String displayText = log.message;

        if (log.eventCode != null) {
            switch (log.eventCode) {
                case "fire_alert": displayText = ctx.getString(R.string.history_fire_alert); break;
                case "fire_cleared": displayText = ctx.getString(R.string.history_fire_cleared); break;
                case "gas_alert": displayText = ctx.getString(R.string.history_gas_alert); break;
                case "gas_cleared": displayText = ctx.getString(R.string.history_gas_cleared); break;
                case "bin_full": displayText = ctx.getString(R.string.history_bin_full); break;
                case "bin_emptied": displayText = ctx.getString(R.string.history_bin_emptied); break;
            }
        }

        holder.binding.tvLogMessage.setText(displayText);
        holder.binding.tvLogDate.setText(sdf.format(new Date(log.timestamp)));
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
