package com.juoska.samplers.asyncprofiler;

import com.juoska.config.Config;
import com.juoska.samplers.SamplerExecutorPipeline;
import groovy.util.logging.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

@Slf4j
class AsyncProfilerExecutorIntegrationTest {
    @Test
    public void test() throws IOException, InterruptedException {
        // config
        Config config = new Config(
                "./target/classes",
                "com.juoska.benchmark.TestBenchmark",
                "./executables/linux/lib/libasyncProfiler.so",
                "./output.sampler-test.json",
                "./asprof.sjsw.output.test.raw.json"
        );
        Duration duration = Duration.ofSeconds(10);

        SamplerExecutorPipeline pipeline = new AsyncProfilerExecutor();

        // run
        pipeline.execute(config, duration);
        pipeline.write("destination.json");

        // assert
    }
}