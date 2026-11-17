/**
 * Original template author: Lon Smith, Ph.D.
 * Extended by: <your group names here>
 *
 * Employee Search – a redesigned version of the CSCI 4055 template.
 *
 * What is different from the template window:
 *   - Resizable layout (BorderLayout / GridBagLayout instead of absolute positions).
 *   - Department and Project lists are "checklists": a single click toggles an item,
 *     so selecting several items does not require holding Ctrl/Cmd.
 *   - "All" / "None" quick buttons and a live "n selected" counter above each list.
 *   - A plain-English preview sentence that explains what the next search will return.
 *   - Results are shown in a sortable table (Name, SSN, Department, Projects) with a count.
 *   - Database work runs in a background thread so the window never freezes.
 *   - Keyboard: Enter in the Database field = Fill, Ctrl/Cmd+Enter = Search, Esc = Clear.
 *
 * SQL lives in src/query/EmployeeSearchQuery.java and the connection in
 * src/database/DatabaseManager.java. This class only handles the window.
 *
 * Assignment requirements still covered:
 *   - Fill resets and reloads departments/projects; pops up an alert if the database can't be opened.
 *   - Multiple departments and projects can be selected.
 *   - Department, Project and Employee lists are all scrollable.
 *   - Clear empties the employee list, removes every selection and unchecks both "Not" boxes.
 *   - "Not" boxes list employees NOT in the selected departments / NOT on the selected projects.
 */

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.prefs.Preferences;

import src.database.DatabaseManager;
import src.query.EmployeeSearchQuery;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class EmployeeSearchFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	// ---- Palette (deep ink header + teal accent) ----
	private static final Color INK = new Color(0x17, 0x21, 0x33);
	private static final Color INK_SOFT = new Color(0x9F, 0xAE, 0xC4);
	private static final Color ACCENT = new Color(0x0E, 0x9F, 0x8E);
	private static final Color ACCENT_DARK = new Color(0x0B, 0x7F, 0x72);
	private static final Color DANGER = new Color(0xE0, 0x5A, 0x4F);
	private static final Color PAGE = new Color(0xF2, 0xF4, 0xF7);
	private static final Color CARD = Color.WHITE;
	private static final Color LINE = new Color(0xDD, 0xE2, 0xE9);
	private static final Color MUTED = new Color(0x6B, 0x76, 0x85);
	private static final Color TEXT = new Color(0x1F, 0x29, 0x37);

	private static final String RESULTS = "results";
	private static final String EMPTY = "empty";

	// ---- Same core fields as the template ----
	private JPanel contentPane;
	private JTextField txtDatabase;
	private JList<String> lstDepartment;
	private DefaultListModel<String> department = new DefaultListModel<String>();
	private JList<String> lstProject;
	private DefaultListModel<String> project = new DefaultListModel<String>();
	private JCheckBox chckbxNotDept;
	private JCheckBox chckbxNotProject;

	// ---- Additions ----
	private DefaultTableModel employeeModel;
	private JTable tblEmployee;
	private JPanel resultsCards;
	private JLabel lblEmptyState;
	private JLabel lblDeptCount;
	private JLabel lblProjectCount;
	private JLabel lblResultCount;
	private JLabel lblPreview;
	private JLabel lblStatusDot;
	private JLabel lblStatusText;
	private JLabel lblFooter;
	private JButton btnDBFill;
	private JButton btnSearch;
	private JButton btnClear;

	private final EmployeeSearchQuery searchQuery = new EmployeeSearchQuery();
	private boolean loaded = false;
	private final Preferences prefs = Preferences.userNodeForPackage(EmployeeSearchFrame.class);

	/**
	 * Uses FlatLaf (lib/flatlaf-*.jar) when available, otherwise the system look and feel.
	 * Called from Main before the window is created.
	 */
	public static void installLookAndFeel() {
		try {
			Class.forName("com.formdev.flatlaf.FlatLightLaf").getMethod("setup").invoke(null);
			UIManager.put("Button.arc", 10);
			UIManager.put("Component.arc", 10);
			UIManager.put("CheckBox.arc", 6);
			UIManager.put("TextComponent.arc", 10);
			UIManager.put("ScrollBar.thumbArc", 999);
			UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
			UIManager.put("Component.focusColor", ACCENT);
			UIManager.put("Component.focusedBorderColor", ACCENT);
			UIManager.put("CheckBox.icon.selectedBackground", ACCENT);
			UIManager.put("CheckBox.icon.selectedBorderColor", ACCENT);
			UIManager.put("CheckBox.icon.checkmarkColor", Color.WHITE);
			UIManager.put("Table.selectionBackground", new Color(0xD6, 0xF2, 0xEE));
			UIManager.put("Table.selectionForeground", TEXT);
		} catch (Exception notFound) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception ignored) {
			}
		}
	}

	/**
	 * Create the frame.
	 */
	public EmployeeSearchFrame() {
		setTitle("Employee Search");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(640, 600));
		setSize(780, 700);
		setLocationRelativeTo(null);

		contentPane = new JPanel(new BorderLayout());
		contentPane.setBackground(PAGE);
		setContentPane(contentPane);

		contentPane.add(buildHeader(), BorderLayout.NORTH);
		contentPane.add(buildBody(), BorderLayout.CENTER);
		contentPane.add(buildFooter(), BorderLayout.SOUTH);

		installShortcuts();
		setConnected(false);
		updatePreview();

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				DatabaseManager.closeConnection();
			}
		});
	}

	// =====================================================================
	//  Layout
	// =====================================================================

	private JComponent buildHeader() {
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(INK);
		header.setBorder(new EmptyBorder(16, 22, 16, 22));

		JPanel titles = new JPanel();
		titles.setOpaque(false);
		titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
		JLabel title = new JLabel("Employee Search");
		title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
		title.setForeground(Color.WHITE);
		JLabel subtitle = new JLabel("Filter the COMPANY database by department and project");
		subtitle.setForeground(INK_SOFT);
		titles.add(title);
		titles.add(Box.createVerticalStrut(2));
		titles.add(subtitle);
		header.add(titles, BorderLayout.WEST);

		JPanel status = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		status.setOpaque(false);
		lblStatusDot = new JLabel("●");
		lblStatusText = new JLabel();
		lblStatusText.setForeground(INK_SOFT);
		status.add(lblStatusDot);
		status.add(lblStatusText);
		JPanel statusWrap = new JPanel(new GridBagLayout()); // vertically center
		statusWrap.setOpaque(false);
		statusWrap.add(status);
		header.add(statusWrap, BorderLayout.EAST);
		return header;
	}

	private JComponent buildBody() {
		JPanel body = new JPanel(new GridBagLayout());
		body.setOpaque(false);
		body.setBorder(new EmptyBorder(16, 18, 8, 18));
		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0;
		g.fill = GridBagConstraints.BOTH;
		g.weightx = 1;

		g.gridy = 0;
		g.insets = new Insets(0, 0, 12, 0);
		body.add(buildConnectionCard(), g);

		g.gridy = 1;
		g.weighty = 0.45;
		JPanel filters = new JPanel(new GridLayout(1, 2, 12, 0));
		filters.setOpaque(false);
		filters.add(buildFilterCard("Department", true));
		filters.add(buildFilterCard("Project", false));
		body.add(filters, g);

		g.gridy = 2;
		g.weighty = 0;
		g.insets = new Insets(0, 4, 10, 4);
		lblPreview = new JLabel();
		lblPreview.setForeground(MUTED);
		body.add(lblPreview, g);

		g.gridy = 3;
		g.weighty = 0.55;
		g.insets = new Insets(0, 0, 0, 0);
		body.add(buildResultsCard(), g);
		return body;
	}

	private JComponent buildConnectionCard() {
		JPanel card = card(new GridBagLayout());
		GridBagConstraints g = new GridBagConstraints();
		g.gridy = 0;
		g.fill = GridBagConstraints.HORIZONTAL;

		JLabel lbl = new JLabel("Database");
		lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
		lbl.setForeground(TEXT);
		g.gridx = 0;
		g.insets = new Insets(0, 0, 0, 12);
		card.add(lbl, g);

		txtDatabase = new JTextField(prefs.get("lastDatabase", "company"));
		txtDatabase.putClientProperty("JTextField.placeholderText", "Database name in resources/ (e.g. company)");
		txtDatabase.putClientProperty("JTextField.showClearButton", true);
		txtDatabase.setToolTipText("Opens resources/<name>.db — press Enter to fill");
		txtDatabase.addActionListener(e -> fill());
		g.gridx = 1;
		g.weightx = 1;
		g.insets = new Insets(0, 0, 0, 8);
		card.add(txtDatabase, g);

		btnDBFill = primaryButton("Fill");
		btnDBFill.setToolTipText("Reload departments and projects from this database (Enter)");
		/**
		 * The btnDBFill fills the department and project JLists with the
		 * departments and projects from the entered database name.
		 */
		btnDBFill.addActionListener(e -> fill());
		g.gridx = 2;
		g.weightx = 0;
		g.insets = new Insets(0, 0, 0, 0);
		card.add(btnDBFill, g);
		return card;
	}

	private JComponent buildFilterCard(String title, boolean isDept) {
		JPanel card = card(new BorderLayout(0, 8));

		// header: title + counter on the left, All/None on the right
		JPanel head = new JPanel(new BorderLayout());
		head.setOpaque(false);
		JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		left.setOpaque(false);
		JLabel lblTitle = new JLabel(title);
		lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 14f));
		lblTitle.setForeground(TEXT);
		JLabel counter = new JLabel();
		counter.setForeground(MUTED);
		counter.setBorder(new EmptyBorder(0, 8, 0, 0));
		left.add(lblTitle);
		left.add(counter);
		head.add(left, BorderLayout.WEST);

		JList<String> list = new JList<String>(isDept ? department : project);
		JPanel quick = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
		quick.setOpaque(false);
		quick.add(linkButton("All", () -> {
			if (list.getModel().getSize() > 0) list.addSelectionInterval(0, list.getModel().getSize() - 1);
		}));
		quick.add(linkButton("None", list::clearSelection));
		head.add(quick, BorderLayout.EAST);
		card.add(head, BorderLayout.NORTH);

		// checklist-style list: one click toggles, multiple selection allowed
		list.setSelectionModel(new ToggleSelectionModel());
		list.setCellRenderer(new CheckListRenderer());
		list.setVisibleRowCount(5);
		list.setFixedCellHeight(28);
		list.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				updateCounters();
				updatePreview();
			}
		});
		JScrollPane scroll = new JScrollPane(list);
		scroll.setBorder(BorderFactory.createLineBorder(LINE));
		card.add(scroll, BorderLayout.CENTER);

		JCheckBox not = new JCheckBox("Not");
		not.setOpaque(false);
		not.setFont(not.getFont().deriveFont(Font.BOLD));
		not.setToolTipText(isDept ? "List employees NOT in the selected departments"
				: "List employees NOT working on the selected projects");
		not.addActionListener(e -> {
			not.setForeground(not.isSelected() ? DANGER : TEXT);
			updatePreview();
		});
		JLabel hint = new JLabel(isDept ? "exclude these departments" : "exclude these projects");
		hint.setForeground(MUTED);
		JPanel foot = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		foot.setOpaque(false);
		foot.add(not);
		foot.add(Box.createHorizontalStrut(4));
		foot.add(hint);
		card.add(foot, BorderLayout.SOUTH);

		if (isDept) {
			lstDepartment = list;
			chckbxNotDept = not;
			lblDeptCount = counter;
		} else {
			lstProject = list;
			chckbxNotProject = not;
			lblProjectCount = counter;
		}
		return card;
	}

	private JComponent buildResultsCard() {
		JPanel card = card(new BorderLayout(0, 8));

		JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		head.setOpaque(false);
		JLabel lblEmployee = new JLabel("Employee");
		lblEmployee.setFont(lblEmployee.getFont().deriveFont(Font.BOLD, 14f));
		lblEmployee.setForeground(TEXT);
		lblResultCount = new JLabel();
		lblResultCount.setForeground(MUTED);
		lblResultCount.setBorder(new EmptyBorder(0, 8, 0, 0));
		head.add(lblEmployee);
		head.add(lblResultCount);
		card.add(head, BorderLayout.NORTH);

		employeeModel = new DefaultTableModel(new Object[] { "Name", "SSN", "Department", "Projects" }, 0) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int r, int c) {
				return false;
			}
		};
		tblEmployee = new JTable(employeeModel);
		tblEmployee.setAutoCreateRowSorter(true);
		tblEmployee.setRowHeight(26);
		tblEmployee.setShowVerticalLines(false);
		tblEmployee.setGridColor(LINE);
		tblEmployee.setFillsViewportHeight(true);
		tblEmployee.getTableHeader().setReorderingAllowed(false);
		tblEmployee.getColumnModel().getColumn(0).setPreferredWidth(170);
		tblEmployee.getColumnModel().getColumn(1).setPreferredWidth(90);
		tblEmployee.getColumnModel().getColumn(2).setPreferredWidth(130);
		tblEmployee.getColumnModel().getColumn(3).setPreferredWidth(260);
		JScrollPane scroll = new JScrollPane(tblEmployee);
		scroll.setBorder(BorderFactory.createLineBorder(LINE));

		lblEmptyState = new JLabel("", SwingConstants.CENTER);
		lblEmptyState.setForeground(MUTED);
		JPanel emptyPanel = new JPanel(new BorderLayout());
		emptyPanel.setBackground(CARD);
		emptyPanel.setBorder(BorderFactory.createDashedBorder(LINE, 4, 4));
		emptyPanel.add(lblEmptyState, BorderLayout.CENTER);

		resultsCards = new JPanel(new CardLayout());
		resultsCards.setOpaque(false);
		resultsCards.add(scroll, RESULTS);
		resultsCards.add(emptyPanel, EMPTY);
		card.add(resultsCards, BorderLayout.CENTER);
		showEmptyState("Fill from a database, pick some filters, then press Search.");
		return card;
	}

	private JComponent buildFooter() {
		JPanel footer = new JPanel(new BorderLayout());
		footer.setOpaque(false);
		footer.setBorder(new EmptyBorder(8, 22, 16, 18));

		lblFooter = new JLabel(" ");
		lblFooter.setForeground(MUTED);
		footer.add(lblFooter, BorderLayout.WEST);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		buttons.setOpaque(false);

		btnClear = new JButton("Clear");
		btnClear.setToolTipText("Clear results, selections and Not boxes (Esc)");
		btnClear.addActionListener(e -> clearAll());
		buttons.add(btnClear);

		btnSearch = primaryButton("Search");
		btnSearch.setToolTipText("Run the search (" + shortcutName() + "+Enter)");
		btnSearch.addActionListener(e -> search());
		buttons.add(btnSearch);
		footer.add(buttons, BorderLayout.EAST);
		return footer;
	}

	private void installShortcuts() {
		int mod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
		JComponent root = getRootPane();
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, mod), "search");
		root.getActionMap().put("search", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				if (btnSearch.isEnabled()) search();
			}
		});
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clear");
		root.getActionMap().put("clear", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				clearAll();
			}
		});
	}

	// =====================================================================
	//  Actions
	// =====================================================================

	/** Fill: reset both lists and reload them from the entered database. */
	private void fill() {
		String name = txtDatabase.getText().trim();
		department.clear();
		project.clear();
		loaded = false;
		clearAll();
		DatabaseManager.closeConnection();
		setConnected(false);
		setBusy(true, "Opening “" + name + "”…");

		new SwingWorker<Object[], Void>() {
			@Override
			protected Object[] doInBackground() throws Exception {
				DatabaseManager.open(name);
				try {
					searchQuery.checkSchema();
					return new Object[] { searchQuery.loadDepartments(), searchQuery.loadProjects() };
				} catch (Exception ex) {
					DatabaseManager.closeConnection();
					throw ex;
				}
			}

			@SuppressWarnings("unchecked")
			@Override
			protected void done() {
				setBusy(false, " ");
				try {
					Object[] r = get();
					for (String d : (List<String>) r[0]) department.addElement(d);
					for (String p : (List<String>) r[1]) project.addElement(p);
					loaded = true;
					prefs.put("lastDatabase", name);
					setConnected(true);
					lblFooter.setText("Loaded " + department.size() + " departments and "
							+ project.size() + " projects.");
					showEmptyState("Ready — choose filters (or none for everyone) and press Search.");
				} catch (Exception ex) {
					Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
					setConnected(false);
					lblFooter.setText("Could not open database.");
					showEmptyState("No database loaded.");
					JOptionPane.showMessageDialog(EmployeeSearchFrame.this,
							"The database “" + name + "” could not be opened.\n\n" + friendly(cause),
							"Database Error", JOptionPane.ERROR_MESSAGE);
				}
				updateCounters();
				updatePreview();
			}
		}.execute();
	}

	private void search() {
		if (!loaded) {
			JOptionPane.showMessageDialog(this,
					"Load a database first: type its name and press Fill.",
					"No database", JOptionPane.WARNING_MESSAGE);
			txtDatabase.requestFocusInWindow();
			return;
		}
		List<String> depts = lstDepartment.getSelectedValuesList();
		List<String> projs = lstProject.getSelectedValuesList();
		boolean notDept = chckbxNotDept.isSelected();
		boolean notProj = chckbxNotProject.isSelected();
		setBusy(true, "Searching…");

		new SwingWorker<List<EmployeeSearchQuery.Employee>, Void>() {
			@Override
			protected List<EmployeeSearchQuery.Employee> doInBackground() throws Exception {
				return searchQuery.searchEmployees(depts, notDept, projs, notProj);
			}

			@Override
			protected void done() {
				setBusy(false, " ");
				try {
					List<EmployeeSearchQuery.Employee> rows = get();
					employeeModel.setRowCount(0);
					for (EmployeeSearchQuery.Employee e : rows) {
						employeeModel.addRow(new Object[] { e.name, formatSsn(e.ssn), e.department, e.projects });
					}
					lblResultCount.setText(rows.size() == 1 ? "1 match" : rows.size() + " matches");
					if (rows.isEmpty()) {
						showEmptyState("No employees match these filters.");
					} else {
						((CardLayout) resultsCards.getLayout()).show(resultsCards, RESULTS);
					}
					lblFooter.setText("Search finished.");
				} catch (Exception ex) {
					Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
					JOptionPane.showMessageDialog(EmployeeSearchFrame.this,
							"The search failed.\n\n" + friendly(cause), "Search Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}.execute();
	}

	/** Clear: empty the employee list, drop every selection, untick both Not boxes. */
	private void clearAll() {
		employeeModel.setRowCount(0);
		lblResultCount.setText("");
		lstDepartment.clearSelection();
		lstProject.clearSelection();
		chckbxNotDept.setSelected(false);
		chckbxNotProject.setSelected(false);
		chckbxNotDept.setForeground(TEXT);
		chckbxNotProject.setForeground(TEXT);
		showEmptyState(!loaded ? "Fill from a database, pick some filters, then press Search."
				: "Cleared — choose filters and press Search.");
		updateCounters();
		updatePreview();
	}

	// =====================================================================
	//  UI state helpers
	// =====================================================================

	private void setBusy(boolean busy, String message) {
		btnDBFill.setEnabled(!busy);
		btnSearch.setEnabled(!busy);
		btnClear.setEnabled(!busy);
		setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
		lblFooter.setText(message);
	}

	private void setConnected(boolean connected) {
		lblStatusDot.setForeground(connected ? new Color(0x3D, 0xD6, 0xB0) : new Color(0x7A, 0x86, 0x99));
		lblStatusText.setText(connected ? "Connected to " + DatabaseManager.getCurrentName() : "Not connected");
	}

	private void showEmptyState(String message) {
		lblEmptyState.setText(message);
		((CardLayout) resultsCards.getLayout()).show(resultsCards, EMPTY);
	}

	private void updateCounters() {
		lblDeptCount.setText(countText(lstDepartment, department.size()));
		lblProjectCount.setText(countText(lstProject, project.size()));
	}

	private static String countText(JList<String> list, int total) {
		if (total == 0) return "";
		int n = list.getSelectedIndices().length;
		return n + " of " + total + " selected";
	}

	/** Builds a plain-English sentence describing the next search. */
	private void updatePreview() {
		if (lblPreview == null || lstDepartment == null || lstProject == null) return;
		List<String> d = lstDepartment.getSelectedValuesList();
		List<String> p = lstProject.getSelectedValuesList();
		StringBuilder s = new StringBuilder("<html>→ Search will list <b>");
		if (d.isEmpty() && p.isEmpty()) {
			s.append("all employees</b>");
		} else {
			s.append("employees</b>");
			if (!d.isEmpty()) {
				s.append(chckbxNotDept.isSelected() ? " <font color='#E05A4F'>not</font> in " : " in ")
				 .append(joinOr(d));
			}
			if (!d.isEmpty() && !p.isEmpty()) s.append(" <b>and</b>");
			if (!p.isEmpty()) {
				s.append(chckbxNotProject.isSelected() ? " <font color='#E05A4F'>not</font> working on "
						: " working on ").append(joinOr(p));
			}
		}
		s.append(".</html>");
		lblPreview.setText(s.toString());
	}

	private static String joinOr(List<String> items) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < items.size(); i++) {
			if (i > 0) sb.append(i == items.size() - 1 ? " or " : ", ");
			sb.append("<i>").append(escape(items.get(i))).append("</i>");
		}
		return sb.toString();
	}

	private static String escape(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static String formatSsn(String ssn) {
		if (ssn != null && ssn.matches("\\d{9}")) {
			return ssn.substring(0, 3) + "-" + ssn.substring(3, 5) + "-" + ssn.substring(5);
		}
		return ssn;
	}

	private static String friendly(Throwable t) {
		String msg = t.getMessage();
		if (msg == null || msg.isBlank()) msg = t.getClass().getSimpleName();
		if (msg.contains("No suitable driver")) {
			msg = "The JDBC driver is missing. Make sure the jars in lib/ are on the classpath.";
		}
		return msg;
	}

	private static String shortcutName() {
		return System.getProperty("os.name", "").toLowerCase().contains("mac") ? "Cmd" : "Ctrl";
	}

	// =====================================================================
	//  Small reusable widgets
	// =====================================================================

	private static JPanel card(java.awt.LayoutManager layout) {
		JPanel p = new JPanel(layout);
		p.setBackground(CARD);
		p.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(LINE), new EmptyBorder(12, 14, 12, 14)));
		return p;
	}

	private static JButton primaryButton(String text) {
		JButton b = new JButton(text);
		b.setBackground(ACCENT);
		b.setForeground(Color.WHITE);
		b.setFont(b.getFont().deriveFont(Font.BOLD));
		b.putClientProperty("JButton.buttonType", "roundRect");
		b.setFocusPainted(false);
		b.setPreferredSize(new Dimension(Math.max(90, b.getPreferredSize().width + 16), b.getPreferredSize().height + 4));
		b.getModel().addChangeListener(e -> b.setBackground(b.getModel().isRollover() ? ACCENT_DARK : ACCENT));
		return b;
	}

	private static JButton linkButton(String text, Runnable action) {
		JButton b = new JButton(text);
		b.putClientProperty("JButton.buttonType", "borderless");
		b.setForeground(ACCENT_DARK);
		b.setFocusable(false);
		b.setMargin(new Insets(0, 6, 0, 6));
		b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		b.addActionListener(e -> action.run());
		return b;
	}

	/** Selection model where a plain click toggles an item instead of replacing the selection. */
	private static class ToggleSelectionModel extends DefaultListSelectionModel {
		private static final long serialVersionUID = 1L;
		private boolean gestureStarted = false;

		ToggleSelectionModel() {
			setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		}

		@Override
		public void setSelectionInterval(int index0, int index1) {
			if (index0 != index1) { // "All" button or shift-range
				super.setSelectionInterval(index0, index1);
				return;
			}
			// During a mouse press/drag, toggle only once per gesture.
			if (getValueIsAdjusting()) {
				if (gestureStarted) return;
				gestureStarted = true;
			}
			if (isSelectedIndex(index0)) removeSelectionInterval(index0, index1);
			else addSelectionInterval(index0, index1);
		}

		@Override
		public void setValueIsAdjusting(boolean adjusting) {
			if (!adjusting) gestureStarted = false;
			super.setValueIsAdjusting(adjusting);
		}
	}

	/** Draws each list row as a checkbox so multi-select is obvious. */
	private static class CheckListRenderer extends JCheckBox implements ListCellRenderer<String> {
		private static final long serialVersionUID = 1L;
		private static final Color SELECTED_ROW = new Color(0xE8, 0xF7, 0xF4);

		CheckListRenderer() {
			setBorder(new EmptyBorder(0, 8, 0, 8));
			setBorderPainted(true);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
				boolean isSelected, boolean cellHasFocus) {
			setText(value);
			setSelected(isSelected);
			setOpaque(true);
			setBackground(isSelected ? SELECTED_ROW : list.getBackground());
			setForeground(TEXT);
			setFont(list.getFont());
			return this;
		}
	}
}
