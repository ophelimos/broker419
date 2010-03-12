import java.io.Serializable;
import java.util.Vector;

public class timestamp implements Serializable {

	private static final long serialVersionUID = 1L;

	// A vector timestamp is owned by a client which has a player name
	// Fixed size of 4
	public Vector<vectorobj> mytimestamp = new Vector<vectorobj>(1, 1);

	public timestamp() {
		// No first entry
	}

	public timestamp(vectorobj firstentry) {
		mytimestamp.add(firstentry);
	}

	public timestamp(timestamp toCopy) {
		for (int i = 0; i < toCopy.mytimestamp.size(); i++) {
			this.mytimestamp.add(toCopy.mytimestamp.get(i).clone());
		}
	}

	public synchronized timestamp clone() {
		timestamp newtimestamp = new timestamp();
		for (int i = 0; i < this.mytimestamp.size(); i++) {
			vectorobj me = this.mytimestamp.get(i).clone();
			newtimestamp.mytimestamp.add(me);
		}
		return newtimestamp;
	}

	/*
	 * addplayer is called when each localclient needs to add a remote player to
	 * its local timestamp
	 *  - checks for having less than 4 players - checks to not add the same
	 * player twice
	 */

	public synchronized boolean addplayer(vectorobj newguy) {
		int i = 0;
		for (i = 0; i < mytimestamp.size(); i++) {
			if (mytimestamp.elementAt(i).playername
					.equalsIgnoreCase(newguy.playername)) {
				return false;
			}
		}
		mytimestamp.add(newguy);
		return true;
	}

	/*
	 * removeplayer from this vector timestamp
	 */
	public synchronized boolean removePlayer(String deadplayer) {
		for (int n = 0; n < mytimestamp.size(); n++) {
			if (deadplayer.equalsIgnoreCase(mytimestamp.get(n).getplayer())) {
				mytimestamp.remove(n);
				return true;
			}
		}
		return false;
	}
	/**
	 * Get the current value of the logical clock for a player
	 */
	public synchronized int get(vectorobj reqplayer) throws IllegalArgumentException {
		for (int i = 0; i < mytimestamp.size(); i++) {
			if (mytimestamp.get(i).playername
					.equalsIgnoreCase(reqplayer.playername)) {
				return mytimestamp.get(i).gettime();
			}
		}
		return 0;
	}

	/**
	 * Increments the logical clock for the specified player by 1 All other
	 * clocks are left unchanged
	 * 
	 * E.g., if this vector timestamp is [1, 3, 4] and the players name is third
	 * in list, this vector timestamp is [1, 3, 5] after invoking increment
	 * 
	 */
	public synchronized void increment(String thename) {
		for (int i = 0; i < mytimestamp.size(); i++) {
			if (mytimestamp.get(i).playername.equalsIgnoreCase(thename)) {
				int temp = mytimestamp.get(i).gettime();
				mytimestamp.get(i).settime(temp + 1);
			}
		}
	}

	/**
	 * Set the logical clock entries of this vector timestamp to the maximum of
	 * its logical clocks and other's logical clocks
	 * 
	 * E.g., if this vector timestamp is [2, 5] and other is [1, 6], then this
	 * vector timestamp is [2, 6] after invoking max
	 * 
	 * @param other :
	 *            a vector timestamp to maximize this vector timestamp with
	 *            respect to
	 * 
	 * @throws IllegalArgumentException
	 *             if other.size() != size()
	 * 
	 */
	public synchronized void max(timestamp other) throws IllegalArgumentException {
		
		int i = 0;
		for (i = 0; i < mytimestamp.size(); i++) {
			// Check for same player name and that mytimestamp's value
			// for the player is lower than the other'
			if (((mytimestamp.get(i).gettime() < other.mytimestamp.get(
					i).gettime()))
					&& (mytimestamp.get(i).getplayer()
							.equalsIgnoreCase(other.mytimestamp.get(i)
									.getplayer()))) {

				mytimestamp.get(i).settime(
						other.mytimestamp.get(i).gettime());
			}
		}
	}

	/**
	 * A vector timestamp, other, can be delivered to this process if:
	 * 
	 * for 0 <= i < size(), i != other.id(), i != id(): other[i] <= this[i] &&
	 * other[other.id()] == this[other.id()] + 1
	 * 
	 * E.g., if this vector timestamp is [3, 4, 7] and id() == 2 then canDeliver
	 * will return true for ([4, 2, 5], 0) and false for ([5, 2, 5], 0)
	 * 
	 * @param other :
	 *            a VectorTimestamp to compare this vector timestamp to
	 * 
	 * @throws IllegalArgument
	 *             if other is incomparable to this vector timestamp
	 * 
	 * @return true if other is causally next
	 * 
	 */
	public synchronized boolean canDeliver(String myname, vectorobj other)
			throws IllegalArgumentException {
		int i = 0;
		for (i = 0; i < mytimestamp.size(); i++) {
			if (myname.equalsIgnoreCase(other.getplayer())) {
				// Invalid, other player is trying to update my timestamp
				return false;
			} else if (mytimestamp.get(i).getplayer().equalsIgnoreCase(
					other.getplayer())) {
				// Current location on vector is pointing to same name as other
				// player's
				if ((mytimestamp.get(i).gettime() + 1) == other.gettime()) {
					// other player's timestamp is my timestamp+1
					return true;
				}
			}
		}
		return false;
	}

	public String toString() {
		String tmpString = "";
		for (int i = 0; i < mytimestamp.size(); i++) {
			int stamp = mytimestamp.get(i).gettime();
			String timeStamp = Integer.toString(stamp);
			tmpString.concat(mytimestamp.get(i).playername + ":" + timeStamp
					+ "|");
		}
		return tmpString;
	}

	public void printVTS() {
		Mazewar.consolePrint("Timestamp: ");
		for (int i = 0; i < this.mytimestamp.size(); i++) {
			Mazewar
					.consolePrint(this.mytimestamp.get(i).getplayer()
							+ " "
							+ Integer.toString(this.mytimestamp.get(i)
									.gettime()) + " ");
		}
		Mazewar.consolePrint("\n");
	}
}
