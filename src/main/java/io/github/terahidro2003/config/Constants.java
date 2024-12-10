package io.github.terahidro2003.config;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Constants {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final String LINUX_ASYNC_PROFILER_URL = "https://github.com/async-profiler/async-profiler/releases/download/v3.0/async-profiler-3.0-linux-x64.tar.gz";
    public static final String LINUX_ASYNC_PROFILER_NAME = "async-profiler-3.0-linux-x64";
    public static final String MACOS_ASYNC_PROFILER_URL = "https://github.com/async-profiler/async-profiler/releases/download/v3.0/async-profiler-3.0-macos.zip";
    public static final String MACOS_ASYNC_PROFILER_NAME = "async-profiler-downloaded";
}