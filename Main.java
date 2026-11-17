/**
 * Starts the Employee Search program.
 */
public class Main {
	public static void main(String[] args) {
		EmployeeSearchFrame.installLookAndFeel();
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					EmployeeSearchFrame frame = new EmployeeSearchFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
