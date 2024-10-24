# SJSW
![Verify](https://github.com/terahidro2003/sjsw/actions/workflows/verify.yml/badge.svg)

Simple Java Sampling Wrapper (SJSW) is a async-profiler Java wrapper library that attaches
`async-profiler` to external Java process as an agent.

SJSW currently can:
1. Launch Java process with async-profiler attached as a Java agent.
2. Write raw async-profiler results to a file.
3. Read such results, and convert them into stack call tree.

This project is part of my bachelor's thesis `Examination of Performance Change Detection Efficiency Using Sampling and Instrumentation Techniques` (2024-2025).

# Usage
### Configuration File
Regardless in what mode (standalone or integrated as a lib) you plan to use SJSW, you need to adjust the configuration.

Configuration file looks like this:
```json
{
  "classPath": "/path/.../exampleProject/target/classes",
  "mainClass": "com.example.benchmark.Benchmark",
  "profilerPath": "/home/test/tmp/async-profiler/build/lib/libasyncProfiler.so",
  "outputPath": "/home/test/output.sampler.json",
  "profilerRawOutputPath": "/home/test/asprof.sjsw.output.raw.json"
}
```
Be advised, that the configuration file can be overridden by Java arguments if they are supplied in the same order as they are in the `config.json`.

`classPath`: folder where compiled Java classes of the benchmark application reside, or a location of a text file (*.txt) with all classpaths.

`mainClass`: main class coordinates of the benchmark application

`profilerPath`: path to the async-profiler executable file. If you're using Linux or MacOS, you can use ones already included in this project:
async-profiler executables for Linux and MacOS are included in this project:

For linux: `./executables/linux/lib/libasyncProfiler.so`

For MacOS: `./executables/macos/lib/libasyncProfiler.so`

`outputPath`: output path of internal SJSW structure. In the next release, this will be changed to the serialized output of the call stack tree.

`profilerRawOutputPath`: raw output path of async-profiler.

### Using as a Library
If you want to use SJSW in you own project:
1. Create a new executor instance:
    ```java
    private static final SamplerExecutorPipeline executor = new AsyncProfilerExecutor();
    ```
2. Retrieve configuration
    ```java
    Config CONFIGURATION = Config.retrieveConfiguration(new File("config.json"));
   
   // OR
   
   Config CONFIGURATION = Config.retrieveConfiguration(args);
    ```
3. Call `execute` method to start Java application with async-profiler attached, and execute `write` method afterward to record results to a file.
    ```java
    executor.execute(CONFIGURATION, Duration.ofSeconds(8));
    executor.write(CONFIGURATION.outputPath());
    ```


# Installation
1. Make sure you use SJSW with JDK17.
2. In [config.json](config.json) adjust `profilerPath` value to either your local path of `libasyncProfiler.so`, or to relative path of already included async-profiler executables (see [Configuration File](#configuration-file))

3. Execute to allow event access for unprivileged users (non sudo users)

    ```sh
    sudo sysctl kernel.perf_event_paranoid=1
    ```

4. Adjust values remaining values in [config.json](config.json) like described in [Configuration File](#configuration-file) section that are specific to your benchmark project.
5. Execute [StandaloneSampler](/src/main/java/com/juoska/StandaloneSampler.java) as main, or use the [SamplerExecutorPipeline](src/main/java/com/juoska/samplers/SamplerExecutorPipeline.java) like it is described in [Using as a Library](#using-as-a-library)
6. Raw output and the tree should be outputted to the console.
