package com.example.cardvr.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cardvr.database.EventEntity;
import com.example.cardvr.database.EventStatus;
import com.example.cardvr.events.EmergencyCountdownManager;
import com.example.cardvr.events.EventRepository;
import com.example.cardvr.recording.SegmentRepository;

import java.util.Locale;

public final class EmergencyActivity extends AppCompatActivity {
    public static final String EXTRA_EVENT_ID = "extra_event_id";

    private final EmergencyCountdownManager countdown = new EmergencyCountdownManager();
    private TextView countdownView;
    private TextView detailsView;
    private long eventId;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        eventId = getIntent().getLongExtra(EXTRA_EVENT_ID, 0L);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        root.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText("Обнаружена возможная авария");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        countdownView = new TextView(this);
        countdownView.setTextSize(36);
        countdownView.setGravity(Gravity.CENTER);
        root.addView(countdownView);

        detailsView = new TextView(this);
        detailsView.setGravity(Gravity.CENTER);
        root.addView(detailsView);

        Button ok = button("Я в порядке", root);
        Button save = button("Сохранить событие", root);
        Button call = button("Позвонить 112", root);
        Button send = button("Отправить координаты", root);

        ok.setOnClickListener(v -> updateStatus(EventStatus.CANCELLED_BY_USER, true));
        save.setOnClickListener(v -> updateStatus(EventStatus.CONFIRMED, true));
        call.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_DIAL,
                Uri.parse("tel:112"))));
        send.setOnClickListener(v -> Toast.makeText(this,
                "Отправка координат будет добавлена в модуле связи", Toast.LENGTH_LONG).show());

        setContentView(root);
        loadDetails();
        countdown.start(15, new EmergencyCountdownManager.Listener() {
            @Override
            public void onTick(int secondsLeft) {
                countdownView.setText(String.format(Locale.getDefault(), "%d", secondsLeft));
            }

            @Override
            public void onFinished() {
            }
        });
    }

    private Button button(String text, LinearLayout root) {
        Button button = new Button(this);
        button.setText(text);
        root.addView(button, new LinearLayout.LayoutParams(-1, -2));
        return button;
    }

    private void loadDetails() {
        SegmentRepository.ioExecutor().execute(() -> {
            EventEntity event = new EventRepository(this).getById(eventId);
            if (event == null) return;
            String details = String.format(Locale.getDefault(),
                    "Координаты: %.6f, %.6f\nСкорость: %.1f км/ч\nЗапись продолжается\n%s",
                    event.latitude == null ? 0.0 : event.latitude,
                    event.longitude == null ? 0.0 : event.longitude,
                    event.speedAfterKmh,
                    event.explanation);
            runOnUiThread(() -> detailsView.setText(details));
        });
    }

    private void updateStatus(EventStatus status, boolean close) {
        SegmentRepository.ioExecutor().execute(() -> new EventRepository(this)
                .setStatus(eventId, status));
        if (close) finish();
    }

    @Override
    protected void onDestroy() {
        countdown.stop();
        super.onDestroy();
    }
}
