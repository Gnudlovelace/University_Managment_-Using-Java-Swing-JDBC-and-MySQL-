# Credit-Based Academic Management System

A desktop application for managing academic activities in a credit-based education system. The project is built with **Java Swing** and **MySQL**, and supports two main roles: administrators and students.

## Technologies

- Java (JDK 17 or later recommended)
- Java Swing – desktop user interface
- MySQL – relational database management system
- JDBC – Java-to-MySQL connectivity
- SQL – database schema, sample data, views, and triggers

> The repository currently does not include a Maven or Gradle build file. If you run the project from an IDE, add MySQL Connector/J to the project's Libraries or classpath.

## Main Features

### Administrator

- Sign in with an administrator account.
- Perform create, read, update, and delete (CRUD) operations on academic data, including faculties, majors, course groups, administrative classes, classrooms, courses, lecturers, students, semesters, course sections, schedules, registrations/grades, tuition fees, academic results, and curricula.
- Access management reports and queries, such as top conduct scores, class schedules, GPA by faculty, course failure rates, open courses and sections, unpaid tuition fees, course-group completion, scholarships, and prerequisite courses.

### Student

- Sign in using the email address stored in the student's profile.
- View and update personal information.
- Register for or withdraw from course sections offered in an open semester.
- View personal schedules, transcripts, GPA, and tuition fees.
- Change the account password.

Course registration rules are also enforced at the database level through triggers. These checks include course prerequisites, courses already passed or currently being taken, credit limits for students under academic warning, class capacity, and schedule conflicts. Students cannot withdraw from a registration record that already has a grade.

## Project Structure

```text
src/
├── DB_Design/
│   ├── Lược đồ ER.drawio
│   └── Lược đồ Quan_he.drawio
├── main/
│   └── java/qltc/
│       ├── Main.java
│       ├── auth/          # Authentication and session management
│       ├── db/            # JDBC connection and SQL query utilities
│       └── ui/            # Screens and user interface components
└── SQL/
    ├── CREATE_DB.sql      # Database creation script
    ├── schema.sql         # Tables and constraints
    ├── seed.sql           # Sample data
    ├── views.sql          # Views for grade calculations
    ├── triggers.sql       # Registration business rules
    └── queries.sql        # Reference queries
```

## Installation and Setup

### 1. Prerequisites

Install the following:

- JDK 17 or later.
- MySQL Server.
- A Java IDE such as IntelliJ IDEA, Eclipse, or NetBeans.
- MySQL Connector/J compatible with your MySQL version.

### 2. Set Up the Database

Open MySQL Workbench or a MySQL client and execute the SQL scripts in this order:

1. `SQL/CREATE_DB.sql` – creates the `QUAN_LY_TIN_CHI` database.
2. `SQL/schema.sql` – creates the tables and constraints.
3. `SQL/seed.sql` – inserts sample data.
4. `SQL/views.sql` – creates views used for grade calculations.
5. `SQL/triggers.sql` – creates triggers that enforce registration rules.

**Important:** The current `CREATE_DB.sql` file contains both `CREATE DATABASE` and `DROP DATABASE` statements. If you execute the entire script, the database will be dropped by the `DROP DATABASE` statement. Comment out or remove `DROP DATABASE QUAN_LY_TIN_CHI;` before running the script if you do not intend to delete existing data.

### 3. Configure the Database Connection

Create a file named `db.properties` in the application's working directory:

```properties
db.url=jdbc:mysql://localhost:3306/QUAN_LY_TIN_CHI?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true
db.user=root
db.password=YOUR_MYSQL_PASSWORD
```

Replace `YOUR_MYSQL_PASSWORD` with your MySQL password. Do not commit a file containing your actual database credentials to GitHub.

The application reads the following properties from `db.properties`:

- `db.url`
- `db.user`
- `db.password`

### 4. Configure the Java Project

1. Open the `src` directory in your IDE, or create a Java project and add the files under `main/java`.
2. Configure the source root so that the `qltc` package is recognized.
3. Add MySQL Connector/J to the project dependencies or classpath.
4. Make sure `db.properties` is available in the application's working directory when running the project.

### 5. Run the Application

Run the following main class:

```text
qltc.Main
```

The application checks the database connection, initializes the default administrator account if necessary, and opens the login screen.

## Default Login Credentials

According to the current source code:

| Role | Username | Default Password |
|---|---|---|
| Administrator | `admin` | `admin123` |
| Student | Email stored in the `sinh_vien` table | `1234` |

On startup, the application creates the default administrator account if it does not already exist. It also creates accounts for students who have an email address but do not yet have an account.

Students should change their password after their first login. This project currently stores passwords as plain text, so the default implementation is intended for coursework or demonstration purposes and should not be used as-is in a production environment.

## Notes

- Initialize the database tables, sample data, views, and triggers before using the corresponding application features.
- Some course-registration rules are implemented in database triggers. Do not skip `triggers.sql`.
- Student usernames are the email addresses stored in the `sinh_vien` table.
- `db.properties` contains machine-specific connection credentials and should not be committed to the repository.

## Authors

Add the names of the author(s), team, class, and course information here.
