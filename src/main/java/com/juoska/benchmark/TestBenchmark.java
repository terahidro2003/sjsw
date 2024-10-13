package com.juoska.benchmark;

import groovy.util.logging.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            Thread.sleep(10);
        }
    }

    public static void methodB(List<Double> list, DoubleStream stream) {
        list.add(stream.sum());
    }
}
