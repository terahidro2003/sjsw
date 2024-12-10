package io.github.terahidro2003.benchmark;

import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.juoska.benchmark.*;

public class ExampleTest {
    @Test
    public void testing() throws InterruptedException {
        List<Double> randomList = new LinkedList<>();
        TestBenchmark.methodA(randomList);
    }
}
