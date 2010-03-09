import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class PlayerSelectionHandler implements ListSelectionListener {
	
	/**
	 * Selected players
	 * 
	 */
	public Vector<Peer> selectedPlayers = new Vector<Peer>();
	
	/**
	 * Connection DB (where we pull our data out of)
	 */
	private ConnectionDB connectionDB;
	
	public PlayerSelectionHandler(ConnectionDB connectionDB) {
		this.connectionDB = connectionDB;
	}

	public void valueChanged(ListSelectionEvent e) {
//		 Don't bother doing anything if they're still adjusting the list
		boolean isAdjusting = e.getValueIsAdjusting();
		if (isAdjusting) {
			return;
		}
		
		// Every time our selection changes, remake the selectedPlayers vector
		selectedPlayers.clear();
		JList lsm = (JList) e.getSource();
		int minIndex = lsm.getMinSelectionIndex();
		int maxIndex = lsm.getMaxSelectionIndex();
		// This is dangerous, because it's handling the actual object
		Vector<OutputPeer> availablePlayers = connectionDB.outputPeers;
		for (int i = minIndex; i <= maxIndex; i++) {
			if (lsm.isSelectedIndex(i)) {
				selectedPlayers.add((Peer)availablePlayers.get(i));
			}
		}
	}

}
