package src.query;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import src.database.DatabaseManager;

/**
 * All SQL for the project. It loads the department and project lists and runs the employee search.
 *
 * Expected schema (COMPANY database):
 *   EMPLOYEE(Fname, Minit, Lname, Ssn, ..., Dno)
 *   DEPARTMENT(Dname, Dnumber, ...)
 *   PROJECT(Pname, Pnumber, ...)
 *   WORKS_ON(Essn, Pno, Hours)
 *
 * The user's selections are passed as PreparedStatement parameters. They are never pasted
 * into the SQL text.
 */
public class EmployeeSearchQuery {

	/** One row of the search result. */
	public static class Employee {
		public final String name;
		public final String ssn;
		public final String department;
		public String projects = "";

		Employee(String name, String ssn, String department) {
			this.name = name;
			this.ssn = ssn;
			this.department = department == null ? "—" : department;
		}
	}

	private static final String[] REQUIRED_TABLES = { "employee", "department", "project", "works_on" };

	/**
	 * Checks that the open database really has the COMPANY tables.
	 * Throws a readable SQLException if any are missing.
	 */
	public void checkSchema() throws SQLException {
		Connection conn = DatabaseManager.getConnection();
		List<String> found = new ArrayList<>();
		DatabaseMetaData md = conn.getMetaData();
		try (ResultSet rs = md.getTables(null, null, "%", new String[] { "TABLE" })) {
			while (rs.next()) found.add(rs.getString("TABLE_NAME").toLowerCase());
		}
		List<String> missing = new ArrayList<>();
		for (String t : REQUIRED_TABLES) {
			if (!found.contains(t)) missing.add(t.toUpperCase());
		}
		if (!missing.isEmpty()) {
			throw new SQLException("This database does not use the COMPANY schema (missing: "
					+ String.join(", ", missing) + ").");
		}
	}

	public List<String> loadDepartments() throws SQLException {
		return readNames("SELECT Dname FROM DEPARTMENT ORDER BY Dname");
	}

	public List<String> loadProjects() throws SQLException {
		return readNames("SELECT Pname FROM PROJECT ORDER BY Pname");
	}

	private List<String> readNames(String sql) throws SQLException {
		List<String> out = new ArrayList<>();
		Connection conn = DatabaseManager.getConnection();
		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) out.add(rs.getString(1));
		}
		return out;
	}

	/**
	 * Searches employees. The department and project filters are combined with AND.
	 *
	 * @param depts   selected departments (empty means no department filter)
	 * @param notDept true lists employees NOT in any selected department
	 * @param projs   selected projects (empty means no project filter)
	 * @param notProj true lists employees NOT working on any selected project
	 */
	public List<Employee> searchEmployees(List<String> depts, boolean notDept,
			List<String> projs, boolean notProj) throws SQLException {

		depts = depts == null ? Collections.emptyList() : depts;
		projs = projs == null ? Collections.emptyList() : projs;

		StringBuilder sql = new StringBuilder();
		List<String> params = new ArrayList<>();

		sql.append("SELECT e.Fname, e.Minit, e.Lname, e.Ssn, d.Dname ")
		   .append("FROM EMPLOYEE e LEFT JOIN DEPARTMENT d ON d.Dnumber = e.Dno ")
		   .append("WHERE 1 = 1");

		// Department filter. EXISTS (not IN) keeps employees with a NULL Dno correct in the NOT case.
		if (!depts.isEmpty()) {
			sql.append(notDept ? " AND NOT EXISTS" : " AND EXISTS")
			   .append(" (SELECT 1 FROM DEPARTMENT d2 WHERE d2.Dnumber = e.Dno AND d2.Dname IN (")
			   .append(placeholders(depts.size())).append("))");
			params.addAll(depts);
		}

		// Project filter.
		if (!projs.isEmpty()) {
			sql.append(notProj ? " AND NOT EXISTS" : " AND EXISTS")
			   .append(" (SELECT 1 FROM WORKS_ON w JOIN PROJECT p ON p.Pnumber = w.Pno")
			   .append(" WHERE w.Essn = e.Ssn AND p.Pname IN (")
			   .append(placeholders(projs.size())).append("))");
			params.addAll(projs);
		}

		sql.append(" ORDER BY e.Lname, e.Fname");

		Map<String, Employee> result = new LinkedHashMap<>();
		Connection conn = DatabaseManager.getConnection();
		try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			for (int i = 0; i < params.size(); i++) ps.setString(i + 1, params.get(i));
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					String minit = rs.getString("Minit");
					String name = rs.getString("Fname")
							+ (minit == null || minit.isBlank() ? "" : " " + minit.trim() + ".")
							+ " " + rs.getString("Lname");
					String ssn = rs.getString("Ssn");
					result.put(ssn, new Employee(name, ssn, rs.getString("Dname")));
				}
			}
		}
		attachProjects(result);
		return new ArrayList<>(result.values());
	}

	/** Fills in the projects each found employee works on. This is for the "Projects" column. */
	private void attachProjects(Map<String, Employee> emps) throws SQLException {
		if (emps.isEmpty()) return;
		Map<String, List<String>> byEmp = new HashMap<>();
		String sql = "SELECT w.Essn, p.Pname FROM WORKS_ON w JOIN PROJECT p ON p.Pnumber = w.Pno ORDER BY p.Pname";
		Connection conn = DatabaseManager.getConnection();
		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				byEmp.computeIfAbsent(rs.getString(1), k -> new ArrayList<>()).add(rs.getString(2));
			}
		}
		for (Employee e : emps.values()) {
			e.projects = String.join(", ", byEmp.getOrDefault(e.ssn, Collections.emptyList()));
		}
	}

	private static String placeholders(int n) {
		return String.join(", ", Collections.nCopies(n, "?"));
	}
}
