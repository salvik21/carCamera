package com.example.cardvr.storage;

import com.example.cardvr.settings.SettingsRepository;

public final class StorageLimitManager {
    private final SettingsRepository settings;
    private final StorageManager storage;

    public StorageLimitManager(SettingsRepository settings, StorageManager storage) {
        this.settings = settings;
        this.storage = storage;
    }

    public boolean requiresCleanup(long normalBytes) {
        return normalBytes > settings.getMaxRecordingBytes()
                || storage.getAvailableBytes() < settings.getReserveFreeBytes();
    }

    public boolean hasEnoughFreeSpace() {
        return storage.getAvailableBytes() >= settings.getReserveFreeBytes();
    }
}
