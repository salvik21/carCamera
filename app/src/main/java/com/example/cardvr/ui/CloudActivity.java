package com.example.cardvr.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cardvr.cloud.CloudAuthenticationManager;
import com.example.cardvr.cloud.CloudSettingsRepository;
import com.example.cardvr.cloud.CloudUploadRepository;
import com.example.cardvr.cloud.TrafficUsageManager;
import com.example.cardvr.cloud.TripCloudManager;
import com.example.cardvr.cloud.UploadQueueManager;
import com.example.cardvr.cloud.UploadScheduler;
import com.example.cardvr.database.CloudUploadTaskEntity;
import com.example.cardvr.database.TrafficUsageEntity;
import com.example.cardvr.recording.SegmentRepository;
import com.example.cardvr.trips.TripRepository;
import com.example.cardvr.database.TripEntity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CloudActivity extends AppCompatActivity {
    private static final int REQUEST_SIGN_IN = 701;
    private CloudAuthenticationManager auth;
    private CloudUploadRepository repository;
    private CloudSettingsRepository settings;
    private TextView status;
    private ArrayAdapter<String> adapter;
    private Spinner autoMode;
    private Spinner networkMode;
    private Spinner mobilePlan;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        auth = new CloudAuthenticationManager(this);
        repository = new CloudUploadRepository(this);
        settings = new CloudSettingsRepository(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        status = new TextView(this);
        root.addView(status);
        Button connect = button("Подключить Google Drive", root);
        Button disconnect = button("Отключить Google Drive", root);
        Button retry = button("Повторить загрузку", root);
        Button uploadNow = button("Загрузить сейчас", root);
        Button protectedOnly = button("Загрузить только защищённые записи", root);
        Button wholeTrip = button("Сохранить всю последнюю поездку в Google Drive", root);
        autoMode = spinner(root, "Автоматически загружать",
                new String[]{"Только возможные аварии", "Аварии и сильные удары",
                        "Все защищённые записи", "Ничего автоматически"});
        networkMode = spinner(root, "Разрешённая сеть",
                new String[]{"Только Wi‑Fi", "Wi‑Fi и мобильные данные",
                        "Мобильные данные только для аварийных файлов", "Не использовать интернет"});
        mobilePlan = spinner(root, "Тип мобильного тарифа",
                new String[]{"Безлимитный", "Ограниченный", "Неизвестный"});
        Button saveSettings = button("Сохранить настройки облака", root);
        Button testJson = button("Создать тестовый аварийный JSON", root);
        Button clearTests = button("Очистить тестовую очередь", root);
        ListView list = new ListView(this);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        setTitle("Облачное хранилище");
        connect.setOnClickListener(v -> startGoogleSignIn());
        disconnect.setOnClickListener(v -> {
            auth.disconnect();
            renderStatus();
        });
        retry.setOnClickListener(v -> new UploadScheduler(this).retryNow());
        uploadNow.setOnClickListener(v -> new UploadScheduler(this).retryNow());
        protectedOnly.setOnClickListener(v -> SegmentRepository.ioExecutor().execute(() -> {
            new TripCloudManager(this).enqueueProtectedOnly();
            runOnUiThread(() -> Toast.makeText(this, "Защищённые записи добавлены в очередь", Toast.LENGTH_SHORT).show());
        }));
        wholeTrip.setOnClickListener(v -> SegmentRepository.ioExecutor().execute(() -> {
            TripEntity trip = new TripRepository(this).getLatestCompleted();
            if (trip != null) new TripCloudManager(this).enqueueFullTrip(trip.id);
            runOnUiThread(() -> Toast.makeText(this, "Последняя поездка добавлена в очередь", Toast.LENGTH_SHORT).show());
        }));
        saveSettings.setOnClickListener(v -> saveSettings());
        testJson.setOnClickListener(v -> new UploadQueueManager(this).createSimulatedJsonTask());
        clearTests.setOnClickListener(v -> new UploadQueueManager(this).clearSimulatedQueue());
        autoMode.setSelection(settings.autoUploadMode().ordinal());
        networkMode.setSelection(settings.networkMode().ordinal());
        mobilePlan.setSelection(settings.mobilePlan().ordinal());
        repository.observeTasks().observe(this, this::renderTasks);
        renderStatus();
    }

    private Button button(String text, LinearLayout root) {
        Button button = new Button(this);
        button.setText(text);
        root.addView(button);
        return button;
    }

    private Spinner spinner(LinearLayout root, String label, String[] values) {
        TextView title = new TextView(this);
        title.setText(label);
        root.addView(title);
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, values));
        root.addView(spinner);
        return spinner;
    }

    private void startGoogleSignIn() {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope("https://www.googleapis.com/auth/drive.file"))
                .build();
        startActivityForResult(GoogleSignIn.getClient(this, options).getSignInIntent(), REQUEST_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_SIGN_IN) return;
        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener(this::connectAccount)
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Google Drive не подключён: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void connectAccount(GoogleSignInAccount account) {
        if (account.getEmail() == null) return;
        auth.connect(account.getEmail());
        renderStatus();
    }

    private void saveSettings() {
        settings.setAutoUploadMode(CloudSettingsRepository.AutoUploadMode.values()[autoMode.getSelectedItemPosition()]);
        settings.setNetworkMode(CloudSettingsRepository.NetworkMode.values()[networkMode.getSelectedItemPosition()]);
        settings.setMobilePlan(CloudSettingsRepository.MobilePlan.values()[mobilePlan.getSelectedItemPosition()]);
        Toast.makeText(this, "Настройки облака сохранены", Toast.LENGTH_SHORT).show();
    }

    private void renderStatus() {
        TrafficUsageEntity usage = new TrafficUsageManager(this).currentUsage();
        String traffic = usage == null ? "трафик ещё не учитывался"
                : String.format(Locale.getDefault(), "Wi‑Fi %.1f МБ, мобильная сеть %.1f МБ",
                usage.wifiBytes / 1048576d, usage.mobileBytes / 1048576d);
        status.setText("Google Drive: " + (auth.isConnected() ? "подключён" : "не подключён")
                + "\nАккаунт: " + (auth.accountName() == null ? "нет" : auth.accountName())
                + "\nОчередь: " + repository.countActiveTasks()
                + "\nОжидают Wi‑Fi: " + repository.countWaitingForWifi()
                + "\nТрафик: " + traffic);
    }

    private void renderTasks(List<CloudUploadTaskEntity> tasks) {
        adapter.clear();
        for (CloudUploadTaskEntity task : tasks) {
            adapter.add(task.type + " · " + task.status + " · "
                    + String.format(Locale.getDefault(), "%.1f МБ", task.sizeBytes / 1048576d)
                    + "\n" + task.localPath
                    + (task.lastError == null ? "" : "\n" + task.lastError));
        }
        renderStatus();
    }
}
