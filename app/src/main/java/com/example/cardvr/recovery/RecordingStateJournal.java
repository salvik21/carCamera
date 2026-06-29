package com.example.cardvr.recovery;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

public final class RecordingStateJournal {
    private static final String PREFS = "recording_state_journal";
    private final SharedPreferences prefs;

    public RecordingStateJournal(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void update(Long tripId, Long segmentId, long startedAt, String path,
                       String camera, String lastGps, boolean protectionActive) {
        prefs.edit()
                .putBoolean("active", true)
                .putLong("tripId", tripId == null ? -1 : tripId)
                .putLong("segmentId", segmentId == null ? -1 : segmentId)
                .putLong("startedAt", startedAt)
                .putString("path", path)
                .putString("camera", camera)
                .putString("lastGps", lastGps)
                .putBoolean("protectionActive", protectionActive)
                .putLong("updatedAt", System.currentTimeMillis())
                .apply();
    }

    public Snapshot snapshot() {
        Snapshot s = new Snapshot();
        s.active = prefs.getBoolean("active", false);
        long trip = prefs.getLong("tripId", -1);
        long segment = prefs.getLong("segmentId", -1);
        s.tripId = trip < 0 ? null : trip;
        s.segmentId = segment < 0 ? null : segment;
        s.path = prefs.getString("path", null);
        s.updatedAt = prefs.getLong("updatedAt", 0);
        return s;
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    public static final class Snapshot {
        public boolean active;
        @Nullable public Long tripId;
        @Nullable public Long segmentId;
        @Nullable public String path;
        public long updatedAt;
    }
}
