package com.example.cardvr.diagnostics;

import android.content.Context;

import com.example.cardvr.database.ErrorSeverity;

public final class CrashReporter implements Thread.UncaughtExceptionHandler {
    private final Thread.UncaughtExceptionHandler previous;
    private final ErrorLogRepository errors;

    public CrashReporter(Context context) {
        previous = Thread.getDefaultUncaughtExceptionHandler();
        errors = new ErrorLogRepository(context);
    }

    public void install() {
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        errors.log("CrashReporter", "UNCAUGHT_EXCEPTION", ErrorSeverity.CRITICAL,
                throwable.getMessage(), throwable, null, null, null,
                false, true, false);
        if (previous != null) previous.uncaughtException(thread, throwable);
    }
}
