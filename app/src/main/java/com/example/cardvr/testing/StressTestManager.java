package com.example.cardvr.testing;

import android.content.Context;
import android.os.Debug;

import com.example.cardvr.diagnostics.ErrorLogRepository;
import com.example.cardvr.database.ErrorSeverity;

public final class StressTestManager {
    private final ErrorLogRepository errors;

    public StressTestManager(Context context) {
        errors = new ErrorLogRepository(context);
    }

    public void recordCheckpoint(String name, int segmentCount, int cameraRestarts) {
        long usedKb = Debug.getNativeHeapAllocatedSize() / 1024L;
        errors.log("StressTest", "CHECKPOINT", ErrorSeverity.INFO,
                name + ": segments=" + segmentCount
                        + ", cameraRestarts=" + cameraRestarts
                        + ", nativeHeapKb=" + usedKb,
                null, null, null, null, true, false, true);
    }
}
