package com.example.cardvr.ui;

import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.camera.view.PreviewView;

public final class PreviewController {

    public enum AutoOffMode {
        FIFTEEN_SECONDS(15_000L),
        THIRTY_SECONDS(30_000L),
        ONE_MINUTE(60_000L),
        IMMEDIATELY(0L),
        NEVER(-1L);

        private final long delayMillis;

        AutoOffMode(long delayMillis) {
            this.delayMillis = delayMillis;
        }

        public long getDelayMillis() {
            return delayMillis;
        }
    }

    public interface Listener {
        void onPreviewVisibilityChanged(boolean visible);
    }

    private final PreviewView previewView;
    private final View regularControls;
    private final View minimalScreen;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoHideRunnable = this::hidePreview;

    private boolean previewVisible = true;

    public PreviewController(
            @NonNull PreviewView previewView,
            @NonNull View regularControls,
            @NonNull View minimalScreen,
            @NonNull Listener listener
    ) {
        this.previewView = previewView;
        this.regularControls = regularControls;
        this.minimalScreen = minimalScreen;
        this.listener = listener;
    }

    public void scheduleAutoHide(@NonNull AutoOffMode mode) {
        cancelAutoHide();
        if (mode == AutoOffMode.NEVER) {
            return;
        }
        if (mode.getDelayMillis() == 0L) {
            hidePreview();
        } else {
            handler.postDelayed(autoHideRunnable, mode.getDelayMillis());
        }
    }

    public void cancelAutoHide() {
        handler.removeCallbacks(autoHideRunnable);
    }

    public void hidePreview() {
        cancelAutoHide();
        if (!previewVisible) {
            return;
        }
        previewVisible = false;
        previewView.setVisibility(View.GONE);
        regularControls.setVisibility(View.GONE);
        minimalScreen.setVisibility(View.VISIBLE);
        listener.onPreviewVisibilityChanged(false);
    }

    public void showPreview() {
        cancelAutoHide();
        if (previewVisible) {
            return;
        }
        previewVisible = true;
        minimalScreen.setVisibility(View.GONE);
        previewView.setVisibility(View.VISIBLE);
        regularControls.setVisibility(View.VISIBLE);
        listener.onPreviewVisibilityChanged(true);
    }

    public boolean isPreviewVisible() {
        return previewVisible;
    }

    public void release() {
        cancelAutoHide();
    }
}
