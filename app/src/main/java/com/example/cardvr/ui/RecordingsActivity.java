package com.example.cardvr.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.cardvr.R;
import com.example.cardvr.database.SegmentStatus;
import com.example.cardvr.database.VideoSegmentEntity;
import com.example.cardvr.recording.SegmentRepository;
import com.example.cardvr.settings.SettingsRepository;
import com.example.cardvr.storage.StorageManager;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class RecordingsActivity extends AppCompatActivity {
    private SegmentRepository repository;
    private SettingsRepository settings;
    private StorageManager storage;
    private LinearLayout list;
    private TextView summary;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        repository = new SegmentRepository(this);
        settings = new SettingsRepository(this);
        storage = new StorageManager(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        summary = new TextView(this);
        root.addView(summary);
        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        setTitle(R.string.recordings);
        repository.observeAll().observe(this, this::render);
    }

    private void render(List<VideoSegmentEntity> segments) {
        list.removeAllViews();
        for (VideoSegmentEntity segment : segments) addSegment(segment);
        SegmentRepository.ioExecutor().execute(() -> {
            long total = repository.getTotalSizeBytes();
            long protectedBytes = repository.getProtectedSizeBytes();
            long free = storage.getAvailableBytes();
            long limit = settings.getMaxRecordingBytes();
            runOnUiThread(() -> summary.setText(getString(R.string.storage_summary,
                    size(total), size(limit), size(free), size(protectedBytes))));
        });
    }

    private void addSegment(VideoSegmentEntity segment) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(8, 16, 8, 16);
        TextView details = new TextView(this);
        details.setText(DateFormat.getDateTimeInstance().format(new Date(segment.startTime))
                + "\n" + duration(segment.durationMs) + " · " + size(segment.sizeBytes)
                + "\n" + segment.status
                + (segment.status == SegmentStatus.PROTECTED ? " · защищён" : ""));
        card.addView(details);
        LinearLayout actions = new LinearLayout(this);
        Button play = button(R.string.play, v -> play(segment));
        Button protect = button(segment.status == SegmentStatus.PROTECTED
                ? R.string.unprotect : R.string.protect, v -> toggleProtection(segment));
        Button delete = button(R.string.delete, v -> confirmDelete(segment));
        actions.addView(play, actionLayoutParams());
        actions.addView(protect, actionLayoutParams());
        actions.addView(delete, actionLayoutParams());
        card.addView(actions);
        list.addView(card);
        File file = new File(segment.filePath);
        if (!file.isFile() && segment.status != SegmentStatus.ERROR) {
            SegmentRepository.ioExecutor().execute(() ->
                    repository.setStatus(segment.id, SegmentStatus.ERROR, "FILE_MISSING"));
        }
    }

    private Button button(int text, View.OnClickListener action) {
        Button button = new Button(this);
        button.setText(text);
        button.setOnClickListener(action);
        return button;
    }

    private LinearLayout.LayoutParams actionLayoutParams() {
        return new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    }

    private void play(VideoSegmentEntity segment) {
        File file = new File(segment.filePath);
        if (!file.isFile()) {
            Toast.makeText(this, R.string.file_missing, Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", file);
        startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "video/mp4")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
    }

    private void toggleProtection(VideoSegmentEntity segment) {
        if (segment.status == SegmentStatus.RECORDING) return;
        SegmentStatus target = segment.status == SegmentStatus.PROTECTED
                ? SegmentStatus.NORMAL : SegmentStatus.PROTECTED;
        SegmentRepository.ioExecutor().execute(() ->
                repository.setStatus(segment.id, target,
                        target == SegmentStatus.PROTECTED ? "MANUAL" : null));
    }

    private void confirmDelete(VideoSegmentEntity segment) {
        if (segment.status == SegmentStatus.RECORDING) return;
        if (segment.status == SegmentStatus.PROTECTED) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.delete_protected_title)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.delete, (d, w) -> delete(segment))
                    .show();
        } else {
            delete(segment);
        }
    }

    private void delete(VideoSegmentEntity segment) {
        SegmentRepository.ioExecutor().execute(() -> {
            repository.setStatus(segment.id, SegmentStatus.DELETE_PENDING, null);
            File file = new File(segment.filePath);
            if (!file.exists() || file.delete()) repository.deleteById(segment.id);
            else repository.setStatus(segment.id, SegmentStatus.ERROR, "Delete failed");
        });
    }

    private static String duration(long ms) {
        long seconds = ms / 1000L;
        return String.format(Locale.getDefault(), "%02d:%02d",
                seconds / 60L, seconds % 60L);
    }

    private static String size(long bytes) {
        if (bytes >= SettingsRepository.GIB) {
            return String.format(Locale.getDefault(), "%.2f ГБ",
                    bytes / (double) SettingsRepository.GIB);
        }
        return String.format(Locale.getDefault(), "%.1f МБ", bytes / 1048576d);
    }
}
