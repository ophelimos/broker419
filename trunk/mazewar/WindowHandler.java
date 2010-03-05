import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;


public class WindowHandler implements WindowListener {

	public void windowActivated(WindowEvent e) {
		//System.out.println("Window Activated!!!");
		// TODO Auto-generated method stub

	}

	public void windowClosed(WindowEvent e) {
		//System.out.println("Window Closed!!!");
		System.exit(0);
	}

	public void windowClosing(WindowEvent e) {
		//System.out.println("Window Closing!!!");
		// TODO Auto-generated method stub
		System.exit(0);

	}

	public void windowDeactivated(WindowEvent e) {
		//System.out.println("Window Deactivated!!!");
		// TODO Auto-generated method stub

	}

	public void windowDeiconified(WindowEvent e) {
		//System.out.println("Window Deiconified!!!");
		// TODO Auto-generated method stub

	}

	public void windowIconified(WindowEvent e) {
		//System.out.println("Window Iconified!!!");
		// TODO Auto-generated method stub

	}

	public void windowOpened(WindowEvent e) {
		//System.out.println("Window Opened");
		// TODO Auto-generated method stub

	}

}
