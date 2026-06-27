package com.example.cardvr.location;

import androidx.annotation.Nullable;

public interface VideoMetadataProvider {
    @Nullable VideoMetadataSnapshot getSnapshot();
}
