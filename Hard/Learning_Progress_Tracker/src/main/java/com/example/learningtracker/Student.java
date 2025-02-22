package com.example.learningtracker;

import java.util.EnumMap;
import java.util.Map;

public class Student {
    private final String id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final Map<Course, Integer> coursePoints;

    public Student(String id, String firstName, String lastName, String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.coursePoints = new EnumMap<>(Course.class);
        for (Course course : Course.values()) {
            coursePoints.put(course, 0);
        }
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void updatePoints(int java, int dsa, int databases, int spring) {
        coursePoints.put(Course.JAVA, coursePoints.get(Course.JAVA) + java);
        coursePoints.put(Course.DSA, coursePoints.get(Course.DSA) + dsa);
        coursePoints.put(Course.DATABASES, coursePoints.get(Course.DATABASES) + databases);
        coursePoints.put(Course.SPRING, coursePoints.get(Course.SPRING) + spring);
    }

    public String getProgress() {
        return String.format("%s points: Java=%d; DSA=%d; Databases=%d; Spring=%d",
                id, coursePoints.get(Course.JAVA), coursePoints.get(Course.DSA),
                coursePoints.get(Course.DATABASES), coursePoints.get(Course.SPRING));
    }
}
