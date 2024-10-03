package com.juoska.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record Config(String classPath, String mainClass, String profilerPath, String outputPath) implements Serializable {

    public static Config retrieveConfiguration(String... args) {
        if (args != null && args.length != 4) {
            System.out.println("No config specified through arguments. Falling back to config.json");
            return retrieveConfiguration(new File("config.json"));
        }

        assert args != null;
        String classPath = args[0];
        String mainClass = args[1];
        String profilerPath = args[2];
        String outputPath = args[3];

        return new Config(classPath, mainClass, profilerPath, outputPath);
    }

    public static Config retrieveConfiguration(File configPath) {
        ObjectReader reader = Constants.OBJECT_MAPPER.readerFor(Config.class);
        try {
            return reader.readValue(configPath);
        } catch (IOException e) {
            System.out.println("Failed to read config file: " + configPath);
            throw new RuntimeException(e);
        }
    }
}
