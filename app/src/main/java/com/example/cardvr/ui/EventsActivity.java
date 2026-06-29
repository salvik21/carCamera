package com.example.cardvr.ui;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cardvr.database.EventEntity;
import com.example.cardvr.events.EventRepository;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class EventsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        ListView list = new ListView(this);
        setContentView(list);
        setTitle("Журнал событий");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, new ArrayList<>());
        list.setAdapter(adapter);
        new EventRepository(this).observeAll().observe(this, events -> {
            adapter.clear();
            adapter.addAll(format(events));
        });
    }

    private List<String> format(List<EventEntity> events) {
        List<String> rows = new ArrayList<>();
        DateFormat format = DateFormat.getDateTimeInstance();
        for (EventEntity event : events) {
            rows.add(String.format(Locale.getDefault(),
                    "%s · %s · %s · %d%%\n%.1f км/ч · %.6f, %.6f\n%s",
                    event.type,
                    format.format(new Date(event.timestamp)),
                    event.severity,
                    event.confidence,
                    event.speedBeforeKmh,
                    event.latitude == null ? 0.0 : event.latitude,
                    event.longitude == null ? 0.0 : event.longitude,
                    event.status));
        }
        return rows;
    }
}
