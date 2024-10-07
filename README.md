# Simple Java Sampling Wrapper

## About
SJSW (Simple Java Sampling Wrapper) - a library that currently wraps async-profiler (`asprof`) and permits to launch a Java application with `asprof` attached as a Java agent to collect samples, and convert them into stack call tree structure.

SJSW currently can:
1. Launch Java process with async-profiler attached as a Java agent.
2. Write raw async-profiler results to a file.
3. Read such results, and convert them into stack call tree.

This project is part of my bachelor's thesis `Examination of Performance Change Detection Efficiency Using Sampling and Instrumentation Techniques` (2024-2025). 

## Usage
### Configuration
Regardless in what mode (standalone or integrated as a lib) you plan to use this project, you will need to adjust the configuration. 
This is possible in one of the two possible ways:

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
Be advised, that configuration file can be overridden by Java arguments if they are supplied in the same order as they are in the `config.json`. 

`classPath`: folder where compiled Java classes of the benchmark application reside.

`mainClass`: main class coordinates of the benchmark application

`profilerPath`: path to the async-profiler executable file. If you're using Linux or MacOS, you can use ones already included in this project:
    
For linux: `./executables/linux/lib/libasyncProfiler.so`

For MacOS: `./executables/macos/lib/libasyncProfiler.so`

`outputPath`: output path of internal SJSW structure. In the next release, this will be changed to the serialized output of the call stack tree.

`profilerRawOutputPath`: raw output path of async-profiler.


### Standalone Usage
If you want just observe the call stack tree and basic metrics,
then you have to:
1. Adjust `config.json` to your needs, or pass arguments in chronological order as Java arguments.
2. In `StandaloneSampler` class adjust sampling duration according to your needs.
   ```java
    executor.execute(CONFIGURATION, Duration.ofSeconds(8)); 
   ```
2. Launch the main method in `StandaloneSampler` class
3. Raw output and the tree should be outputted to the sout.

### As a library
If you want to use SJSW in you own project:
1. Create new executor instance:
    ```java
    private static final SamplerExecutorPipeline executor = new AsyncProfilerExecutor();
    ```
2. Retrieve configuration
    ```java
    Config CONFIGURATION = Config.retrieveConfiguration(new File("config.json"));
   
   // OR
   
   Config CONFIGURATION = Config.retrieveConfiguration(args);
    ```
3. Call `execute` method to start Java app with async-profiler attached, and `write` method afterward to record results to a file.
    ```java
    executor.execute(CONFIGURATION, Duration.ofSeconds(8));
    executor.write(CONFIGURATION.outputPath());
    ```
