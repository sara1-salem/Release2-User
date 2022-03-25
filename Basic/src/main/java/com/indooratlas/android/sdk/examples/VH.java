package com.indooratlas.android.sdk.examples;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class VH extends RecyclerView.ViewHolder {
    public TextView txt_name,Distance;

    public VH(@NonNull View itemView) {
        super(itemView);
        txt_name=itemView.findViewById(R.id.txt_name);
        Distance=itemView.findViewById(R.id.Distance);

    }

    public TextView getTxt_name() {
        return txt_name;
    }
}

