package com.example.learningtracker;

public enum Course {
    JAVA("Java"),
    DSA("Data Structures and Algorithms"),
    DATABASES("Databases"),
    SPRING("Spring");

    private final String courseName;

    Course(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseName() {
        return courseName;
    }

    public static boolean isValidCourse(String name) {
        for (Course course : values()) {
            if (course.name().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}

