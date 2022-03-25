package com.indooratlas.android.sdk.examples;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    ArrayList<String> list = new ArrayList<String>();
    ArrayList<Double> Dis=new ArrayList<Double>();

    public Adapter(Context ctx) {
        this.context = ctx;
    }

    public void setItems(ArrayList<String> WarningMsg) {
        list.addAll(WarningMsg);
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_item, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        VH vh = (VH) holder;
        String wm = list.get(position);
        vh.txt_name.setText(wm);


    }

    private Picasso getSnapshots() {
        return null;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


    public void setItem(ArrayList<Double> distances) {
        Dis.addAll(distances);
    }
}

