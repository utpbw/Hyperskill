package com.example.learningtracker;

import java.util.Scanner;

public class LearningProgressTracker {
    private static final Scanner scanner = new Scanner(System.in);
    private static final StudentManager studentManager = new StudentManager();

    public static void main(String[] args) {
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
                    return;
                case "add students":
                    addStudents();
                    break;
                case "list":
                    studentManager.listStudents();
                    break;
                case "add points":
                    addPoints();
                    break;
                case "find":
                    findStudent();
                    break;
                default:
                    System.out.println("Unknown command!");
            }
        }
    }

    private static void addStudents() {
        System.out.println("Enter student credentials or 'back' to return.");
        while (true) {
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("back")) {
                studentManager.printStudentCount();
                return;
            }
            studentManager.addStudent(input);
        }
    }

    private static void addPoints() {
        System.out.println("Enter an id and points or 'back' to return.");
        while (true) {
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("back")) {
                return;
            }
            studentManager.updatePoints(input);
        }
    }

    private static void findStudent() {
        System.out.println("Enter an id or 'back' to return.");
        while (true) {
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("back")) {
                return;
            }
            studentManager.findStudent(input);
        }
    }
}
