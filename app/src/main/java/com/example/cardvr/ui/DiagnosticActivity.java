package com.example.cardvr.ui;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cardvr.diagnostics.AppHealthChecker;
import com.example.cardvr.diagnostics.DiagnosticReportManager;
import com.example.cardvr.diagnostics.ErrorLogRepository;
import com.example.cardvr.recording.SegmentRepository;

import java.io.File;
import java.util.ArrayList;

public final class DiagnosticActivity extends AppCompatActivity {
    private final ArrayList<String> rows = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        TextView title = new TextView(this);
        title.setText("Проверка готовности");
        title.setTextSize(20);
        root.addView(title);
        Button refresh = new Button(this);
        refresh.setText("Обновить");
        root.addView(refresh);
        Button export = new Button(this);
        export.setText("Экспортировать диагностический отчёт");
        root.addView(export);
        ListView list = new ListView(this);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows);
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        setTitle("Диагностика");
        refresh.setOnClickListener(v -> render());
        export.setOnClickListener(v -> exportReport());
        render();
    }

    private void render() {
        rows.clear();
        for (AppHealthChecker.Item item : new AppHealthChecker(this).check()) {
            rows.add(symbol(item.status) + " " + item.name + "\n" + item.detail);
        }
        new ErrorLogRepository(this).observeRecent().observe(this, errors -> {
            for (int i = rows.size() - 1; i >= 0; i--) {
                if (rows.get(i).startsWith("Ошибка:")) {
                    rows.remove(i);
                }
            }
            for (int i = 0; i < Math.min(10, errors.size()); i++) {
                rows.add("Ошибка: " + errors.get(i).severity + " " + errors.get(i).module
                        + "\n" + errors.get(i).message);
            }
            adapter.notifyDataSetChanged();
        });
        adapter.notifyDataSetChanged();
    }

    private void exportReport() {
        SegmentRepository.ioExecutor().execute(() -> {
            try {
                File file = new DiagnosticReportManager(this).exportTextReport();
                runOnUiThread(() -> Toast.makeText(this,
                        "Отчёт сохранён: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Ошибка экспорта: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private static String symbol(AppHealthChecker.Status status) {
        switch (status) {
            case READY: return "🟢";
            case WARNING: return "🟡";
            case ERROR:
            default: return "🔴";
        }
    }
}
