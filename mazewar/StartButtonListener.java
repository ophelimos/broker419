import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


public class StartButtonListener implements MouseListener {

	public void mouseClicked(MouseEvent e) {
		// Action performed on release

	}

	public void mouseEntered(MouseEvent e) {
//		 Action performed on release

	}

	public void mouseExited(MouseEvent e) {
//		 Action performed on release

	}

	public void mousePressed(MouseEvent e) {
//		 Action performed on release

	}

	public void mouseReleased(MouseEvent e) {
		// Send a "start game" packet
		Mazewar.consolePrintLn("Starting Mazewar with clients...");

	}

}
