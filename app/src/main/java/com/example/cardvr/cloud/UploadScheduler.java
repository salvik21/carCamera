package com.example.cardvr.cloud;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public final class UploadScheduler {
    public static final String UNIQUE_WORK = "cloud_upload_queue";
    private final Context context;

    public UploadScheduler(Context context) {
        this.context = context.getApplicationContext();
    }

    public void schedule() {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(CloudUploadWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK,
                ExistingWorkPolicy.KEEP,
                request
        );
    }

    public void retryNow() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK);
        schedule();
    }
}
