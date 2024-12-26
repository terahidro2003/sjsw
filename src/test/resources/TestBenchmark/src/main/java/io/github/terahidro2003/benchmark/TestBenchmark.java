package io.github.terahidro2003.benchmark;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.DoubleStream;

public class TestBenchmark {
    public static void main(String[] args) throws InterruptedException {
        List<Double> randomList = new LinkedList<>();
        methodA(randomList);
    }

    public static void methodA(List<Double> list) throws InterruptedException {
        for (int i = 0; i <= 1000; i++) {
            Random r = new Random();
            methodB(list, r.doubles(i));
            Thread.sleep(100);
        }
    }

    public static void methodB(List<Double> list, DoubleStream stream) throws InterruptedException {
        list.add(stream.sum());
        Thread.sleep(50);
    }
}
