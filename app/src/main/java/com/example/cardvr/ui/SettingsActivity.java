package com.example.cardvr.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;

import com.example.cardvr.R;
import com.example.cardvr.settings.SettingsRepository;

public final class SettingsActivity extends AppCompatActivity {
    private static final String UI_PREFS = "recorder_ui_settings";
    private static final String PREF_AUTO_OFF_POSITION = "preview_auto_off_position";
    private static final String PREF_LENS_FACING = "selected_lens_facing";
    private static final long[] DURATIONS = {30_000L, 60_000L, 180_000L, 300_000L};
    private static final long[] LIMITS = {2, 5, 10, 20, 50};
    private static final long[] RESERVES = {1, 2, 5};
    private static final long[] GPS_INTERVALS = {1_000L, 2_000L, 5_000L};
    private static final float[] GPS_ACCURACIES = {10f, 20f, 50f};
    private static final long[] POWER_DELAYS = {0L, 60_000L, 300_000L, 600_000L, -1L};
    private static final long[] STOP_DELAYS = {300_000L, 600_000L, 900_000L, 1_800_000L, -1L};

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        SettingsRepository settings = new SettingsRepository(this);
        LinearLayout root = column();
        Spinner camera = spinner(root, R.string.camera_setting,
                new String[]{getString(R.string.camera_back), getString(R.string.camera_front)});
        int savedLens = getSharedPreferences(UI_PREFS, MODE_PRIVATE)
                .getInt(PREF_LENS_FACING, CameraSelector.LENS_FACING_BACK);
        camera.setSelection(savedLens == CameraSelector.LENS_FACING_FRONT ? 1 : 0);
        Spinner previewAutoOff = spinner(root, R.string.preview_auto_off_label,
                getResources().getStringArray(R.array.preview_auto_off_options));
        previewAutoOff.setSelection(getSharedPreferences(UI_PREFS, MODE_PRIVATE)
                .getInt(PREF_AUTO_OFF_POSITION, 4));
        Spinner duration = spinner(root, R.string.segment_duration,
                new String[]{"30 секунд", "1 минута", "3 минуты", "5 минут"});
        duration.setSelection(indexOf(DURATIONS, settings.getSegmentDurationMs()));
        Spinner limit = spinner(root, R.string.maximum_storage,
                new String[]{"2 ГБ", "5 ГБ", "10 ГБ", "20 ГБ", "50 ГБ", "Своё"});
        EditText customLimit = number(root);
        limit.setSelection(indexOfGib(LIMITS, settings.getMaxRecordingBytes()));
        Spinner reserve = spinner(root, R.string.free_space_reserve,
                new String[]{"1 ГБ", "2 ГБ", "5 ГБ", "Своё"});
        EditText customReserve = number(root);
        reserve.setSelection(indexOfGib(RESERVES, settings.getReserveFreeBytes()));
        Spinner gpsInterval = spinner(root, R.string.gps_interval,
                new String[]{"1 секунда", "2 секунды", "5 секунд"});
        gpsInterval.setSelection(indexOf(GPS_INTERVALS, settings.getGpsIntervalMs()));
        Spinner gpsPriority = spinner(root, R.string.gps_accuracy_mode,
                new String[]{"Высокая точность", "Сбалансированный", "Экономия энергии"});
        gpsPriority.setSelection(settings.getGpsPriority());
        Spinner maxAccuracy = spinner(root, R.string.gps_max_accuracy,
                new String[]{"10 метров", "20 метров", "50 метров", "Своё"});
        int accuracyIndex = indexOf(GPS_ACCURACIES, settings.getMaxGpsAccuracyMeters());
        maxAccuracy.setSelection(accuracyIndex);
        EditText customAccuracy = number(root);
        customAccuracy.setHint(R.string.custom_meters);
        CheckBox lowBattery = new CheckBox(this);
        lowBattery.setText(R.string.low_battery_gps);
        lowBattery.setChecked(settings.reduceGpsOnLowBattery());
        root.addView(lowBattery);
        Spinner startMode = spinner(root, R.string.trip_start_mode,
                new String[]{"Всегда спрашивать", "Начинать автоматически", "Только вручную"});
        startMode.setSelection(settings.getTripStartMode());
        Spinner powerEnd = spinner(root, R.string.power_end_delay,
                new String[]{"Сразу", "1 минута", "5 минут", "10 минут", "Не завершать"});
        powerEnd.setSelection(indexOf(POWER_DELAYS, settings.getPowerEndDelayMs()));
        Spinner stopEnd = spinner(root, R.string.stop_end_delay,
                new String[]{"5 минут", "10 минут", "15 минут", "30 минут", "Не завершать"});
        stopEnd.setSelection(indexOf(STOP_DELAYS, settings.getStopEndDelayMs()));
        Button save = new Button(this);
        save.setText(R.string.save);
        root.addView(save);
        save.setOnClickListener(v -> {
            try {
                settings.setSegmentDurationMs(DURATIONS[duration.getSelectedItemPosition()]);
                settings.setMaxRecordingBytes(selectedBytes(limit, customLimit, LIMITS));
                settings.setReserveFreeBytes(selectedBytes(reserve, customReserve, RESERVES));
                settings.setGpsIntervalMs(GPS_INTERVALS[gpsInterval.getSelectedItemPosition()]);
                settings.setGpsPriority(gpsPriority.getSelectedItemPosition());
                int accuracyPosition = maxAccuracy.getSelectedItemPosition();
                float accuracy = accuracyPosition < GPS_ACCURACIES.length
                        ? GPS_ACCURACIES[accuracyPosition]
                        : Float.parseFloat(customAccuracy.getText().toString());
                settings.setMaxGpsAccuracyMeters(accuracy);
                settings.setReduceGpsOnLowBattery(lowBattery.isChecked());
                settings.setTripStartMode(startMode.getSelectedItemPosition());
                settings.setPowerEndDelayMs(POWER_DELAYS[powerEnd.getSelectedItemPosition()]);
                settings.setStopEndDelayMs(STOP_DELAYS[stopEnd.getSelectedItemPosition()]);
                getSharedPreferences(UI_PREFS, MODE_PRIVATE).edit()
                        .putInt(PREF_LENS_FACING,
                                camera.getSelectedItemPosition() == 1
                                        ? CameraSelector.LENS_FACING_FRONT
                                        : CameraSelector.LENS_FACING_BACK)
                        .putInt(PREF_AUTO_OFF_POSITION,
                                previewAutoOff.getSelectedItemPosition())
                        .apply();
                Toast.makeText(this, R.string.save, Toast.LENGTH_SHORT).show();
                finish();
            } catch (RuntimeException e) {
                Toast.makeText(this, "Введите положительное значение", Toast.LENGTH_LONG).show();
            }
        });
        setTitle(R.string.settings_title);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.addView(root);
        setContentView(scrollView);
    }

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(32, 32, 32, 32);
        return view;
    }

    private Spinner spinner(LinearLayout root, int label, String[] values) {
        TextView title = new TextView(this);
        title.setText(label);
        root.addView(title);
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, values));
        root.addView(spinner, new ViewGroup.LayoutParams(-1, -2));
        return spinner;
    }

    private EditText number(LinearLayout root) {
        EditText input = new EditText(this);
        input.setHint(R.string.custom_gib);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(input);
        return input;
    }

    private static int indexOf(long[] values, long selected) {
        for (int i = 0; i < values.length; i++) if (values[i] == selected) return i;
        return 1;
    }

    private static int indexOf(float[] values, float selected) {
        for (int i = 0; i < values.length; i++) if (values[i] == selected) return i;
        return values.length;
    }

    private static int indexOfGib(long[] values, long bytes) {
        long gib = bytes / SettingsRepository.GIB;
        for (int i = 0; i < values.length; i++) if (values[i] == gib) return i;
        return values.length;
    }

    private static long selectedBytes(Spinner spinner, EditText custom, long[] presets) {
        int index = spinner.getSelectedItemPosition();
        double gib = index < presets.length ? presets[index]
                : Double.parseDouble(custom.getText().toString());
        return (long) (gib * SettingsRepository.GIB);
    }
}
