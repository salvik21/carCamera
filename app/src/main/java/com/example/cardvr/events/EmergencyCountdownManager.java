package com.example.cardvr.events;

import android.os.Handler;
import android.os.Looper;

public final class EmergencyCountdownManager {
    public interface Listener {
        void onTick(int secondsLeft);
        void onFinished();
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int secondsLeft;
    private Listener listener;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (listener == null) return;
            listener.onTick(secondsLeft);
            if (secondsLeft <= 0) {
                listener.onFinished();
                return;
            }
            secondsLeft--;
            handler.postDelayed(this, 1_000L);
        }
    };

    public void start(int seconds, Listener listener) {
        stop();
        this.secondsLeft = seconds;
        this.listener = listener;
        handler.post(tick);
    }

    public void stop() {
        handler.removeCallbacks(tick);
        listener = null;
    }
}
