package com.example.cardvr.recovery;

import android.content.Context;

import com.example.cardvr.cloud.CloudUploadRepository;
import com.example.cardvr.database.ErrorSeverity;
import com.example.cardvr.diagnostics.ErrorLogRepository;
import com.example.cardvr.recording.RecordingRecoveryManager;
import com.example.cardvr.recording.SegmentRepository;
import com.example.cardvr.storage.StorageConsistencyChecker;
import com.example.cardvr.trips.TripRepository;

public final class RecoveryManager {
    public static final class Result {
        public boolean activeRecordingFound;
        public boolean activeTripFound;
        public int recoveredSegments;
        public int missingSegments;
        public int cloudTasks;
        public String message = "Состояние приложения проверено";
    }

    private final Context context;

    public RecoveryManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public Result recover() {
        Result result = new Result();
        ErrorLogRepository errors = new ErrorLogRepository(context);
        try {
            RecordingStateJournal.Snapshot journal = new RecordingStateJournal(context).snapshot();
            result.activeRecordingFound = journal.active;
            result.activeTripFound = new TripRepository(context).getActive() != null;
            RecordingRecoveryManager recordingRecovery =
                    new RecordingRecoveryManager(new SegmentRepository(context));
            result.recoveredSegments = recordingRecovery.recoverDetailed();
            StorageConsistencyChecker.StorageConsistencyResult storage =
                    new StorageConsistencyChecker(context).check();
            result.missingSegments = storage.missingFiles;
            result.cloudTasks = new CloudUploadRepository(context).countActiveTasks();
            if (result.activeTripFound) {
                result.message = "Предыдущая поездка была завершена некорректно";
            } else if (result.recoveredSegments > 0 || result.missingSegments > 0) {
                result.message = "Найдены восстановленные или повреждённые записи";
            }
            errors.log("Recovery", "STARTUP_RECOVERY", ErrorSeverity.INFO,
                    result.message, null, null, null, null, true,
                    result.activeTripFound || result.missingSegments > 0, false);
        } catch (Exception e) {
            errors.log("Recovery", "RECOVERY_FAILED", ErrorSeverity.ERROR,
                    e.getMessage(), e, null, null, null, false, true, false);
            result.message = "Ошибка восстановления: " + e.getMessage();
        }
        return result;
    }
}
