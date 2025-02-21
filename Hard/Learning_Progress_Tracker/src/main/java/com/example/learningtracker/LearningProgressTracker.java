package com.example.learningtracker;

import java.util.Scanner;
import java.util.regex.Pattern;

public class LearningProgressTracker {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z]+(?:[-'][A-Za-z]+)*(?: [A-Za-z]+(?:[-'][A-Za-z]+)*)*$");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Learning Progress Tracker");
        int studentCount = 0;

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
                case "add students":
                    System.out.println("Enter student credentials or 'back' to return.");
                    while (true) {
                        String credentials = scanner.nextLine().trim();
                        if (credentials.equalsIgnoreCase("back")) {
                            System.out.println("Total " + studentCount + " students have been added.");
                            break;
                        }
                        if (validateCredentials(credentials)) {
                            studentCount++;
                            System.out.println("The student has been added.");
                        }
                    }
                    break;
                default:
                    System.out.println("Unknown command!");
            }
        }
    }

    private static boolean validateCredentials(String credentials) {
        String[] parts = credentials.split(" ", 3);
        if (parts.length < 3) {
            System.out.println("Incorrect credentials");
            return false;
        }

        String firstName = parts[0];
        String lastName = parts[1];
        String email = parts[2];

        if (!NAME_PATTERN.matcher(firstName).matches() || firstName.length() < 2) {
            System.out.println("Incorrect first name");
            return false;
        }
        if (!NAME_PATTERN.matcher(lastName).matches() || lastName.length() < 2) {
            System.out.println("Incorrect last name");
            return false;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            System.out.println("Incorrect email");
            return false;
        }
        return true;
    }
}
