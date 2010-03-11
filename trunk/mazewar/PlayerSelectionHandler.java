import java.util.Vector;

import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class PlayerSelectionHandler implements ListSelectionListener {
	
	/**
	 * Connection DB (where we pull our data out of)
	 */
	private ConnectionDB connectionDB;
	
	/**
	 * The mazewar GUI where selectedPlayers is kept
	 */
	private MazewarGUI mazewarGUI;
	
	/**
	 * Constructor
	 * @param connectionDB
	 * @param mazewarGUI
	 */
	public PlayerSelectionHandler(ConnectionDB connectionDB, MazewarGUI mazewarGUI) {
		this.connectionDB = connectionDB;
		this.mazewarGUI = mazewarGUI;
	}

	public void valueChanged(ListSelectionEvent e) {
//		 Don't bother doing anything if they're still adjusting the list
		boolean isAdjusting = e.getValueIsAdjusting();
		if (isAdjusting) {
			return;
		}
		
		// Every time our selection changes, remake the selectedPlayers vector
		mazewarGUI.selectedPlayers.clear();
		JList lsm = (JList) e.getSource();
		int minIndex = lsm.getMinSelectionIndex();
		int maxIndex = lsm.getMaxSelectionIndex();
		// This is dangerous, because it's handling the actual object
		Vector<InputPeer> availablePlayers = connectionDB.inputPeers;
		for (int i = minIndex; i <= maxIndex; i++) {
			if (lsm.isSelectedIndex(i)) {
				mazewarGUI.selectedPlayers.add((Peer)availablePlayers.get(i));
			}
		}
	}

}
