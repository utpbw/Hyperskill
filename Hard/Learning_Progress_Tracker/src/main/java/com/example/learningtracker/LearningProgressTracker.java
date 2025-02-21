package com.example.learningtracker;

import java.util.Scanner;

public class LearningProgressTracker {
        public static void main(String[] args) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Learning Progress Tracker");

            while (true) {
                String input = scanner.nextLine().trim();

                if (input.isEmpty()) {
                    System.out.println("No input");
                    continue;
                }

                switch (input.toLowerCase()) {
                    case "exit":
                        System.out.println("Bye!");
                        scanner.close();
                        return;
                    default:
                        System.out.println("Unknown command!");
                }
            }
        }
}
