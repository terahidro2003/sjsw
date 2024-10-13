package com.juoska.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.juoska.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

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
            if(!config.classPath.isEmpty()) {
                try {
                    String classPath = FileUtils.readFileToString(config.classPath);
                    return adjustClasspathValue(config, classPath);
                } catch (IOException e) {
                    return config;
                }
            } else {
                return config;
            }
        } catch (IOException e) {
            System.out.println("Failed to read config file: " + configPath);
            throw new RuntimeException(e);
        }
    }

    public static Config adjustClasspathValue(Config prev, String classPath) {
        return new Config(classPath, prev.mainClass, prev.profilerPath, prev.outputPath, prev.profilerRawOutputPath);
    }
}
