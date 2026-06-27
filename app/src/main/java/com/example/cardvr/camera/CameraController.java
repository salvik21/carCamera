package com.example.cardvr.camera;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.VideoCapture;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public final class CameraController {

    public interface Listener {
        void onCameraReady(@NonNull VideoCapture<Recorder> videoCapture);

        void onCameraError(@NonNull String message);
    }

    private final Context appContext;
    private final LifecycleOwner lifecycleOwner;
    private final PreviewView previewView;
    private final Listener listener;

    private ProcessCameraProvider cameraProvider;
    private int bindingRequestId;

    public CameraController(
            @NonNull Context context,
            @NonNull LifecycleOwner lifecycleOwner,
            @NonNull PreviewView previewView,
            @NonNull Listener listener
    ) {
        this.appContext = context.getApplicationContext();
        this.lifecycleOwner = lifecycleOwner;
        this.previewView = previewView;
        this.listener = listener;
    }

    public void startCamera(@CameraSelector.LensFacing int lensFacing) {
        int requestId = ++bindingRequestId;
        ListenableFuture<ProcessCameraProvider> providerFuture =
                ProcessCameraProvider.getInstance(appContext);

        providerFuture.addListener(() -> {
            if (requestId != bindingRequestId) {
                return;
            }
            try {
                cameraProvider = providerFuture.get();
                bindUseCases(lensFacing);
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause();
                reportError("Не удалось открыть камеру", cause != null ? cause : exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                reportError("Открытие камеры было прервано", exception);
            } catch (RuntimeException exception) {
                reportError("Ошибка инициализации камеры", exception);
            }
        }, ContextCompat.getMainExecutor(appContext));
    }

    private void bindUseCases(@CameraSelector.LensFacing int lensFacing) {
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        try {
            if (!cameraProvider.hasCamera(cameraSelector)) {
                listener.onCameraError("Выбранная камера отсутствует на устройстве");
                return;
            }

            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            QualitySelector qualitySelector = QualitySelector.from(
                    Quality.HD,
                    FallbackStrategy.higherQualityOrLowerThan(Quality.HD)
            );
            Recorder recorder = new Recorder.Builder()
                    .setQualitySelector(qualitySelector)
                    .build();
            VideoCapture<Recorder> videoCapture = VideoCapture.withOutput(recorder);

            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
            );
            listener.onCameraReady(videoCapture);
        } catch (Exception exception) {
            reportError("Не удалось подключить выбранную камеру", exception);
        }
    }

    public void release() {
        bindingRequestId++;
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
    }

    private void reportError(String message, Throwable throwable) {
        String details = throwable.getLocalizedMessage();
        listener.onCameraError(details == null || details.trim().isEmpty()
                ? message
                : message + ": " + details);
    }
}
