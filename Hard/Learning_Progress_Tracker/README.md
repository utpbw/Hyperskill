# Learning Progress Tracker

## Stage 3: A Detailed Record

### Features Implemented:
- **Unique Student ID Assignment**:
  - Each student is assigned a unique ID upon registration.
  - Email addresses must be unique and cannot be reused.

- **New Commands Added**:
  - `list`: Displays all registered student IDs in the order they were added.
    - If no students are registered, prints `No students found.`
  - `add points`: Allows updating student records with earned points.
    - Accepts input in the format: `studentId number number number number` (for Java, DSA, Databases, Spring).
    - Ensures valid IDs and non-negative points.
    - Prints `Points updated.` upon success.
    - If an ID is invalid, prints `No student is found for id=%s.`
    - If input format is incorrect, prints `Incorrect points format.`
  - `find`: Retrieves student progress details.
    - Displays: `id points: Java=%d; DSA=%d; Databases=%d; Spring=%d`
    - If an ID is invalid, prints `No student is found for id=%s.`

- **Data Storage and Validation**:
  - Ensures that each student can only register once using a unique email.
  - Stores student progress for Java, DSA, Databases, and Spring.
  - Prevents negative points from being entered.
  - Introduced an `enum` (`Course.java`) to manage course names and validation.

### Project Structure:
```
Learning_Progress_Tracker/
│── src/
│   ├── main/
│   │   └── java/
│   │       └── com/example/learningtracker/
│   │           ├── LearningProgressTracker.java
│   │           ├── Student.java
│   │           ├── StudentManager.java
│   │           ├── Course.java
│── build.gradle
│── README.md
│── settings.gradle
```
