package com.example.smartbin_app.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartbin_app.R;
import com.example.smartbin_app.databinding.ItemBinBinding;

import java.util.List;

public class BinAdapter extends RecyclerView.Adapter<BinAdapter.BinViewHolder> {

    public static class Bin {
        public String id, name;
        public int percentage = 0;
        public boolean flameAlert = false, gasAlert = false;
        public int oldPercentage = -1;
        public boolean wasFlame = false;
        public boolean wasGas = false;

        public Bin(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private List<Bin> binList;
    private OnBinClickListener listener;

    public interface OnBinClickListener {
        void onDeleteClick(Bin bin);
        void onBinClick(Bin bin);
    }

    public BinAdapter(List<Bin> binList, OnBinClickListener listener) {
        this.binList = binList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BinViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBinBinding binding = ItemBinBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new BinViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BinViewHolder holder, int position) {
        Bin bin = binList.get(position);

        if (bin.flameAlert) {
            holder.binding.layoutBinContainer.setBackgroundResource(R.drawable.bg_pill_alert);
            holder.binding.tvItemBinName.setText("🔥 " + bin.name);

        } else if (bin.gasAlert) {
            holder.binding.layoutBinContainer.setBackgroundResource(R.drawable.bg_pill_orange);
            holder.binding.tvItemBinName.setText("🤢 " + bin.name);

        } else if (bin.percentage >= 80) {
            holder.binding.layoutBinContainer.setBackgroundResource(R.drawable.bg_pill_blue);
            holder.binding.tvItemBinName.setText("🗑️ " + bin.name);

        } else {
            holder.binding.layoutBinContainer.setBackgroundResource(R.drawable.bg_pill_dark);
            holder.binding.tvItemBinName.setText(bin.name);
        }

        holder.itemView.setOnClickListener(v -> listener.onBinClick(bin));

        holder.itemView.setOnLongClickListener(v -> {
            listener.onDeleteClick(bin);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return binList.size();
    }

    public static class BinViewHolder extends RecyclerView.ViewHolder {
        ItemBinBinding binding;

        public BinViewHolder(@NonNull ItemBinBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}