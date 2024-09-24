package com.juoska.benchmark;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Code from
 */

public class Benchmark {

    public static void main (String[] args) throws InterruptedException {
        Benchmark app = new Benchmark();
        while(true) {
            // Perform various tasks
            app.runCPUIntensiveTask();
            app.runMemoryIntensiveTask();
            app.runIOIntensiveTask();
        }
    }

    // CPU-Intensive Task: Fibonacci Calculation
    public void runCPUIntensiveTask() throws InterruptedException {
        for (int i = 0; i < 2000; i++) {
            Thread.sleep(100);
            System.out.println("Fibonacci(" + i + ") = " + calculateFibonacci(i));
        }
    }

    // Recursively calculates Fibonacci numbers
    private long calculateFibonacci(int n) {
        if (n <= 1) {
            return n;
        }
        return calculateFibonacci(n - 1) + calculateFibonacci(n - 2);
    }

    // Memory-Intensive Task: Create a large list and perform operations
    public void runMemoryIntensiveTask() {
        List<String> largeList = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < 100000; i++) {
            largeList.add(generateRandomString(random, 10000));
        }

        // Perform a simple operation on the list
        largeList.stream().sorted().forEach(str -> processString(str));
    }

    // Generates a random string of a given length
    private String generateRandomString(Random random, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + random.nextInt(26)));
        }
        return sb.toString();
    }

    // Simulate some processing on a string
    private void processString(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(str.getBytes());
            md.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    // IO-Intensive Task: Simulate reading and writing from/to disk
    public void runIOIntensiveTask() {
        try {
            for (int i = 0; i < 5000; i++) {
                String content = "This is a test content for IO-intensive task number " + i;
                writeToFile("testFile" + i + ".txt", content);
                readFromFile("testFile" + i + ".txt");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Writes content to a file
    private void writeToFile(String fileName, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(content);
        }
    }

    // Reads content from a file
    private void readFromFile(String fileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                processString(line);
            }
        }
    }
}
