import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;

public class StartButtonListener implements MouseListener {

	/**
	 * The mazewar GUI where selectedPlayers is kept
	 */
	private MazewarGUI mazewarGUI;

	public StartButtonListener(MazewarGUI mazewarGUI) {
		this.mazewarGUI = mazewarGUI;
	}

	public void mouseClicked(MouseEvent e) {
		// Action performed on release

	}

	public void mouseEntered(MouseEvent e) {
		// Action performed on release

	}

	public void mouseExited(MouseEvent e) {
		// Action performed on release

	}

	public void mousePressed(MouseEvent e) {
		// Action performed on release

	}

	public void mouseReleased(MouseEvent e) {
		if (mazewarGUI.selectedPlayers.isEmpty()) {
			Mazewar
					.consolePrintLn("Error: Select some opponents before trying to start the game");
			return;
		}

		if (mazewarGUI.selectedPlayers.size() > Mazewar.maxPlayers) {
			Mazewar.consolePrintLn("Error: Only a maximum of "
					+ Mazewar.maxPlayers + " allowed");
			return;
		}

		// Disabled for complexity reasons
		// // Make sure we're not already sending an invitation
		// if (!Mazewar.setInviting()) {
		// return;
		// }

		// Increase my timestamp
		Mazewar.localtimestamp.increment(Mazewar.localName);

		// Send a "start game" packet
		gamePacket startGamePacket = new gamePacket();
		startGamePacket.type = gamePacket.GP_STARTGAME;
		startGamePacket.wantACK = false;

		// Create a list of the players in the game - including myself
		String[] myTeam = new String[mazewarGUI.selectedPlayers.size() + 1];
		startGamePacket.numPlayers = mazewarGUI.selectedPlayers.size() + 1;

		// Print who we're starting it with while also filling in the selected
		// players
		Mazewar.consolePrint("Starting Mazewar with clients:");
		int i;
		for (i = 0; i < mazewarGUI.selectedPlayers.size(); i++) {
			Mazewar.consolePrint(" "
					+ mazewarGUI.selectedPlayers.get(i).playerName);
			myTeam[i] = mazewarGUI.selectedPlayers.get(i).hostname;
		}
		Mazewar.consolePrint("\n");

		// Now add myself on to the end
		myTeam[i] = Mazewar.hostname;

		startGamePacket.playerlist = myTeam;

		// Send the packet to only the selected clients
		// Mazewar.toNetwork.addtoQueue(startGamePacket);
		for (i = 0; i < mazewarGUI.selectedPlayers.size(); i++) {
			try {
				mazewarGUI.selectedPlayers.get(i).out
						.writeObject(startGamePacket);
			} catch (IOException exc) {
				Mazewar.consoleErrorPrintLn("Dropped the connection with "
						+ mazewarGUI.selectedPlayers.get(i).hostname
						+ " while trying to start the game");
				// Broadcast our name, ensuring dropped connections get
				// discovered properly
				gamePacket nameMessage = new gamePacket();
				nameMessage.type = gamePacket.GP_MYNAME;
				nameMessage.senderName = Mazewar.localName;
				nameMessage.wantACK = false;
				nameMessage.ACK = false;
				Mazewar.toNetwork.addtoQueue(nameMessage);
			}
		}

		// Put it on my own queue, since I won't be receiving this one
		Mazewar.toMaze.addtoSortedQueue(startGamePacket);
	}

}
