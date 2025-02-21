# Learning Progress Tracker

## Stage 2: Verify New User

### Features Implemented:
- **Added the `add students` command**:
  - Prompts the user to enter student credentials.
  - Accepts first name, last name, and a valid email address.
  - Allows users to type `back` to stop adding students and displays the total count.

- **Enhanced Input Validation**:
  - **First and Last Name Rules**:
    - Must contain only English letters (A-Z, a-z), hyphens (`-`), or apostrophes (`'`).
    - Must be at least two characters long.
    - Cannot start or end with hyphens/apostrophes.
    - Cannot contain consecutive hyphens/apostrophes.
  - **Email Validation**:
    - Must follow a basic format: `name@domain.com`.
    - Ensures an `@` symbol and a valid domain structure.

- **Error Handling & Messages**:
  - If credentials are incorrect, specific messages are displayed:
    - `Incorrect first name`
    - `Incorrect last name`
    - `Incorrect email`
  - If input cannot be interpreted as credentials, displays `Incorrect credentials`.
  - If the user enters `back`, prints `Total X students have been added.`

### Project Structure:
```
Learning_Progress_Tracker/
│── src/
│   ├── main/
│   │   └── java/
│   │       └── com/example/learningtracker/
│   │           └── LearningProgressTracker.java
│── build.gradle
│── README.md
│── settings.gradle
```
