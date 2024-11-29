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
public record Config(String classPath, String mainClass, String profilerPath, String outputPath, String profilerRawOutputPath) implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    // TODO: test whether config properties passed as args work
    public static Config retrieveConfiguration(String... args) {
        if (args != null && args.length != 1) {
            log.warn("No config specified through arguments. Falling back to config.json");
            return retrieveConfiguration(new File("config.json"));
        }

        assert args != null;
        String configFilePath = args[0];

        return retrieveConfiguration(new File(configFilePath));
    }

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
        if(!config.classPath.contains(".jar") && config.classPath.contains(".txt")) {
            try {
                String classPath = FileUtils.readFileToString(config.classPath);
                return new Config(classPath, config.mainClass, config.profilerPath, config.outputPath, config.profilerRawOutputPath);
            } catch (IOException e) {
                return config;
            }
        } else {
            return config;
        }
    }

    private boolean hasValidProfilerExecutable() {
        return Files.exists(new File(profilerPath).toPath());
    }
}
