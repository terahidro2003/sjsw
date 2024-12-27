package io.github.terahidro2003.samplers.asyncprofiler;

import java.util.UUID;

public class MeasurementIdentifier {
    private UUID uuid;

    public MeasurementIdentifier(UUID uuid) {
        this.uuid = uuid;
    }

    public MeasurementIdentifier() {
        this.uuid = UUID.randomUUID();
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
}
