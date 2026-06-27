package com.example.cardvr.ui;

import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;

public final class SimpleItemSelectedListener implements AdapterView.OnItemSelectedListener {

    public interface SelectionListener {
        void onItemSelected(int position);
    }

    private final SelectionListener listener;

    public SimpleItemSelectedListener(@NonNull SelectionListener listener) {
        this.listener = listener;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        listener.onItemSelected(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
