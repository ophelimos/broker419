import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

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
		
		// TODO: Testing purposes
//		vectorobj myname = new vectorobj(8787, "Bobbie");
//		startGamePacket.timeogram = new timestamp(myname);

		// Create a list of the players in the game
		String[] myTeam = new String[mazewarGUI.selectedPlayers.size()];
		startGamePacket.numPlayers = mazewarGUI.selectedPlayers.size();

		// Print who we're starting it with while also filling in the selected
		// players
		Mazewar.consolePrint("Starting Mazewar with clients:");
		for (int i = 0; i < mazewarGUI.selectedPlayers.size(); i++) {
			Mazewar.consolePrint(" "
					+ mazewarGUI.selectedPlayers.get(i).playerName);
			myTeam[i] = mazewarGUI.selectedPlayers.get(i).hostname;
		}
		Mazewar.consolePrint("\n");

		startGamePacket.playerlist = myTeam;

		// Send the packet
		Mazewar.toNetwork.addtoQueue(startGamePacket);
	}

}
