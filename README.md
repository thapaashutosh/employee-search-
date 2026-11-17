# Employee Search — CSCI 4055 Database Project

A Java Swing program that loads the departments and projects from a database that uses the
COMPANY schema. It then lists employees who are in (or **not** in) the selected departments
and/or working (or **not** working) on the selected projects.

Built from Dr. Smith's template: https://github.com/proflon/CSCI4055-2025

# 📁 Project Structure
```
Database assginment/
│
├── lib/
│   ├── sqlite-jdbc-3.46.1.3.jar        # SQLite JDBC driver
│   └── flatlaf-3.5.1.jar               # Modern look and feel (optional)
│
├── resources/
│   ├── buildCOMPANYdb.sql              # Professor's COMPANY schema + data
│   └── company.db                      # Generated SQLite database
│
├── src/
│   ├── database/
│   │   └── DatabaseManager.java        # Opens/closes the SQLite connection
│   └── query/
│       └── EmployeeSearchQuery.java    # Loads lists, builds queries, searches DB
│
├── ui/
│   └── EmployeeSearchFrame.java        # Main GUI frame
│
├── Main.java                           # Starts the program
└── README.md
```

---

# Prerequisites
- Java 17 or newer
- SQLite JDBC driver (`lib/sqlite-jdbc-3.46.1.3.jar`)
- `sqlite3` command-line tool (only needed to rebuild `company.db`)

---

# How to Run
Run every command from the project folder.

**macOS / Linux**
```
sqlite3 resources/company.db < resources/buildCOMPANYdb.sql
javac -cp "lib/*" -d bin Main.java ui/EmployeeSearchFrame.java src/database/DatabaseManager.java src/query/EmployeeSearchQuery.java
java -cp "bin:lib/*" Main
```

**Windows**
```
sqlite3 resources\company.db < resources\buildCOMPANYdb.sql
javac -cp "lib/*" -d bin Main.java ui\EmployeeSearchFrame.java src\database\DatabaseManager.java src\query\EmployeeSearchQuery.java
java -cp "bin;lib/*" Main
```

The first line rebuilds the database. Skip it if `resources/company.db` already exists.
To rebuild it, delete `resources/company.db` first. The script has no DROP TABLE lines, so it fails if the tables already exist.
SQLite reports errors on the last 8 `ALTER TABLE ... ADD CONSTRAINT` lines because it doesn't support
adding foreign keys that way. These errors are expected. All tables and data still load correctly.

---

# How to Use

| Action | How |
|---|---|
| Load departments & projects | Type `company` (opens `resources/company.db`) and press **Fill** or Enter |
| Select several items | Click items to tick/untick them (no Ctrl needed), or use **All** / **None** |
| Exclude instead of include | Tick **Not** under the list |
| Search | **Search** button or Ctrl/Cmd + Enter |
| Reset | **Clear** button or Esc |

- The line above the results explains the search in plain English before you run it, e.g.
  *"Search will list employees in Research and not working on ProductX."*
- With nothing selected, **Search** lists ALL employees.
- Department and project filters are combined with AND.
- Results show Name, SSN, Department and Projects. Click a column header to sort.

---

# Requirements Checklist
1. Group project. Group names go in the header of `ui/EmployeeSearchFrame.java`.
2. GUI tools are listed below.
3. Multiple departments and projects can be selected.
4. Department, Project and Employee lists are all scrollable.
5. **Fill** clears and reloads both lists. If the database can't be opened, an error pop-up says so.
6. **Clear** empties the employee list, removes all selections and unticks both Not boxes.
7. The **Not** boxes list employees not in the selected departments / not on the selected projects.

---

# Tools Used
- **Java + Swing.** The GUI is written by hand, not with WindowBuilder. It uses `GridBagLayout`,
  `BorderLayout` and `GridLayout`, so the window can be resized.
- **FlatLaf 3.5.1** for the look and feel. If the jar is missing, the app uses the system look.
- **SQLite + SQLite JDBC** for the database.
- Editor: VS Code

---

# The SQL Behind Search
Selections are passed as `PreparedStatement` parameters (`?`), never pasted into the SQL.

```sql
SELECT e.Fname, e.Minit, e.Lname, e.Ssn, d.Dname
FROM EMPLOYEE e LEFT JOIN DEPARTMENT d ON d.Dnumber = e.Dno
WHERE 1 = 1
  AND [NOT] EXISTS (SELECT 1 FROM DEPARTMENT d2
                    WHERE d2.Dnumber = e.Dno AND d2.Dname IN (?, ?, ...))
  AND [NOT] EXISTS (SELECT 1 FROM WORKS_ON w JOIN PROJECT p ON p.Pnumber = w.Pno
                    WHERE w.Essn = e.Ssn AND p.Pname IN (?, ?, ...))
ORDER BY e.Lname, e.Fname;
```
# employee-search-
