# Sampler Architecture
## Workflow
1. Launch the benchmark Java process.
   2. Either by `async-profiler`
   3. Separately from the agent.
2. Get PID of the benchmark process, if needed.
3. Run sampling tool for specified amount of time and at specified frequency.
4. Put output into `InputStream` or its implementation.
5. While streaming the output of monitoring (sampling) agent, 