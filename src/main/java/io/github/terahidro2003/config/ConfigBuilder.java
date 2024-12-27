package io.github.terahidro2003.config;

import io.github.terahidro2003.samplers.asyncprofiler.MeasurementIdentifier;

public class ConfigBuilder {
    private Config config;

    private String executable;
    private String mainClass;
    private String profilerPath;
    private String outputPath;
    private Boolean JfrEnabled;
    private Integer frequency;

    public ConfigBuilder() {
        this.config = new Config(null, null, null, null, false, 0);
    }

    public ConfigBuilder executable(String executable) {
        this.executable = executable;
        return this;
    }

    public ConfigBuilder mainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    public ConfigBuilder profilerPath(String profilerPath) {
        this.profilerPath = profilerPath;
        return this;
    }

    public ConfigBuilder autodownloadProfiler() {
        this.profilerPath = null;
        return this;
    }

    public ConfigBuilder outputPath(String outputPath) {
        this.outputPath = outputPath;
        return this;
    }

    public ConfigBuilder outputPathWithIdentifier(String outputPath, MeasurementIdentifier identifier) {
        this.outputPath = outputPath + "/measurement_" + identifier.getUuid().toString();
        return this;
    }

    public ConfigBuilder jfrEnabled(boolean jfrEnabled) {
        JfrEnabled = jfrEnabled;
        return this;
    }

    public ConfigBuilder frequency(int frequency) {
        this.frequency = frequency;
        return this;
    }

    public Config build() {
        Config config1 = new Config(
                this.executable,
                this.mainClass,
                this.profilerPath,
                this.outputPath,
                this.JfrEnabled,
                this.frequency
        );
        return config1;
    }
}
