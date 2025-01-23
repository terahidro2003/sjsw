package io.github.terahidro2003.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.terahidro2003.utils.FileUtils;
import groovy.util.logging.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
@Slf4j
public record Config(String executable, String mainClass, String profilerPath, String outputPath, Boolean JfrEnabled, Integer frequency) implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    public static Config retrieveConfiguration(File configPath) {
        ObjectReader reader = Constants.OBJECT_MAPPER.readerFor(Config.class);
        try {
            Config config = reader.readValue(configPath);
            if(!config.hasValidProfilerExecutable()) {
                throw new IllegalStateException("Profiler executable could not be found");
            }
            return adjustClasspathValue(config);
        } catch (IOException e) {
            log.error("Failed to read config file: {}", configPath, e);
            throw new RuntimeException(e);
        }
    }

    private static Config adjustClasspathValue(Config config) {
        if(!config.executable.contains(".jar") && config.executable.contains(".txt")) {
            try {
                String classPath = FileUtils.readFileToString(config.executable);
                return new Config(classPath, config.mainClass, config.profilerPath, config.outputPath, config.JfrEnabled, config.frequency);
            } catch (IOException e) {
                return config;
            }
        } else {
            return config;
        }
    }

    public static Config clone(Config config, String outputPath) {
        return new Config(config.executable(), config.mainClass, config.profilerPath, outputPath, config.JfrEnabled, config.frequency);
    }

    private boolean hasValidProfilerExecutable() {
        return Files.exists(new File(profilerPath).toPath());
    }

    public static ConfigBuilder builder() {
        return new ConfigBuilder();
    }
}
