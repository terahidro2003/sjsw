package io.github.terahidro2003.measurement.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class MeasurementIdentifier {
    private UUID uuid;

    public MeasurementIdentifier() {
        this.uuid = UUID.randomUUID();
    }
}
