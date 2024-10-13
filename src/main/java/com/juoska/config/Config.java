package com.juoska.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.juoska.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record Config(String classPath, String mainClass, String profilerPath, String outputPath, String profilerRawOutputPath) implements Serializable {

    // TODO: test whether config properties passed as args work
    public static Config retrieveConfiguration(String... args) {
        if (args != null && args.length != 5) {
            System.out.println("No config specified through arguments. Falling back to config.json");
            return retrieveConfiguration(new File("config.json"));
        }

        assert args != null;
        String classPath = args[0];
        String mainClass = args[1];
        String profilerPath = args[2];
        String outputPath = args[3];
        String profilerRawOutputPath = args[4];

        return new Config(classPath, mainClass, profilerPath, outputPath, profilerRawOutputPath);
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
            System.out.println("Failed to read config file: " + configPath);
            throw new RuntimeException(e);
        }
    }

    private static Config adjustClasspathValue(Config config) {
        if(!config.classPath.isEmpty()) {
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
