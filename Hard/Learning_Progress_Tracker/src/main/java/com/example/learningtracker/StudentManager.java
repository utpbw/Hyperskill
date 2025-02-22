package com.example.learningtracker;

import java.util.LinkedHashMap;
import java.util.Map;

public class StudentManager {
    private final Map<String, Student> students = new LinkedHashMap<>();
    private int studentCounter = 10000;

    public void addStudent(String credentials) {
        String[] parts = credentials.split(" ", 3);
        if (parts.length < 3 || students.values().stream().anyMatch(s -> s.getEmail().equals(parts[2]))) {
            System.out.println("Incorrect credentials or email already taken");
            return;
        }
        String id = String.valueOf(studentCounter++);
        students.put(id, new Student(id, parts[0], parts[1], parts[2]));
        System.out.println("The student has been added.");
    }

    public void listStudents() {
        if (students.isEmpty()) {
            System.out.println("No students found.");
            return;
        }
        System.out.println("Students:");
        students.keySet().forEach(System.out::println);
    }

    public void updatePoints(String input) {
        String[] parts = input.split(" ");
        if (parts.length != 5 || !students.containsKey(parts[0])) {
            System.out.println("Incorrect points format or No student is found for id=" + parts[0]);
            return;
        }
        try {
            int java = Integer.parseInt(parts[1]);
            int dsa = Integer.parseInt(parts[2]);
            int databases = Integer.parseInt(parts[3]);
            int spring = Integer.parseInt(parts[4]);
            students.get(parts[0]).updatePoints(java, dsa, databases, spring);
            System.out.println("Points updated.");
        } catch (NumberFormatException e) {
            System.out.println("Incorrect points format.");
        }
    }

    public void findStudent(String id) {
        if (students.containsKey(id)) {
            System.out.println(students.get(id).getProgress());
        } else {
            System.out.println("No student is found for id=" + id);
        }
    }

    public void printStudentCount() {
        System.out.println("Total " + students.size() + " students have been added.");
    }
}
