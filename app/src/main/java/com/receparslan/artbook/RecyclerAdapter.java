package com.receparslan.artbook;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.receparslan.artbook.databinding.RecyclerRowBinding;

import java.util.ArrayList;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    final ArrayList<Art> artList;

    public RecyclerAdapter(ArrayList<Art> artList) {
        this.artList = artList;
    }

    @NonNull
    @Override
    public RecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerRowBinding recyclerRowBinding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(recyclerRowBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerAdapter.ViewHolder holder, int position) {
        holder.recyclerRowBinding.artIdTextView.setText(String.valueOf(position + 1));
        holder.recyclerRowBinding.artNameTextView.setText(artList.get(position).getName());
        holder.itemView.setOnClickListener(view -> {
            Intent showDetailsIntent = new Intent(holder.itemView.getContext(), DetailActivity.class);
            showDetailsIntent.putExtra("artID", artList.get(position).getId());
            holder.itemView.getContext().startActivity(showDetailsIntent);
        });
    }

    @Override
    public int getItemCount() {
        return artList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        final RecyclerRowBinding recyclerRowBinding;

        public ViewHolder(@NonNull RecyclerRowBinding recyclerRowBinding) {
            super(recyclerRowBinding.getRoot());
            this.recyclerRowBinding = recyclerRowBinding;
        }
    }
}
