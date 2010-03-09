/*
 * This class implements a queue for every client in the game
 * The queue holds 'gamePackets' objects as elements
 * 
 * The queue is sorted at all times, lowest time stamp value at top of queue and highest
 * at bottom
 * 
 * Before an elements is added it is sorted and inserted in the right place
 * 
 * Every method in the class is synchronized, since queues will be accessed by 
 * different threads at the same time
 */
import java.util.*;

public class clientQueue {
	protected Vector<gamePacket> lineup = new Vector<gamePacket>(0, 1);

	// Use this to add an element to the communicating queue
	public synchronized boolean addtoQueue(gamePacket toadd) {
		lineup.add(toadd);
		return true;
	}

	// Use this to add an element to the communicating queue
	public synchronized void addtoSortedQueue(gamePacket toadd) {
		int linepoint = 0;

		while (linepoint < lineup.size()) {
			// Check for the correct conditions of timestamp
			if (checktimestamp(toadd, lineup.get(linepoint))) {
				lineup.add(linepoint + 1, toadd);
				return;
			}
		}

		// May have not added by now
		return;
	}

	private synchronized boolean checktimestamp(gamePacket toinsert,
			gamePacket tocheck) {
		boolean isgood = false;
		int point1 = 0, point2 = 0, sum1 = 0, sum2 = 0;
		// Check for both timestmap sizes
		if (toinsert.timeogram.mytimestamp.size() != tocheck.timeogram.mytimestamp
				.size()) {
			return isgood;
		}

		for (point1 = 0; point1 < toinsert.timeogram.mytimestamp.size(); point1++) {
			sum1 += toinsert.timeogram.mytimestamp.get(point1).gettime();
		}

		for (point2 = 0; point2 < tocheck.timeogram.mytimestamp.size(); point2++) {
			sum2 = tocheck.timeogram.mytimestamp.get(point2).gettime();
		}

		if (sum2 == sum1 + 1) {
			isgood = true;
			return isgood;
		}
		return isgood;
	}

	public synchronized boolean istimeeql(gamePacket toinsert, gamePacket tocheck) {
		boolean isgood = false;
		int point1 = 0, point2 = 0, sum1 = 0, sum2 = 0;
		// Check for both timestmap sizes
		if (toinsert.timeogram.mytimestamp.size() != tocheck.timeogram.mytimestamp
				.size()) {
			return isgood;
		}

		for (point1 = 0; point1 < toinsert.timeogram.mytimestamp.size(); point1++) {
			sum1 += toinsert.timeogram.mytimestamp.get(point1).gettime();
		}

		for (point2 = 0; point2 < tocheck.timeogram.mytimestamp.size(); point2++) {
			sum2 = tocheck.timeogram.mytimestamp.get(point2).gettime();
		}

		if (sum1 < sum2) {
			isgood = true;
			return isgood;
		}
		return isgood;
	}
	
	public synchronized gamePacket getElement() {
		// This lineup will return firstpacket, and then remove it as well
		if (!lineup.isEmpty()) {
			return lineup.remove(0);
		} else {
			return null;
		}

	}

	// Check if we have all the ACKs for a certain gamePacket we sent out
	// earlier
	public synchronized gamePacket haveACK(gamePacket checkfor) {
		int point = 0;
		boolean foundinqueue = false;

		for (point = 0; point < lineup.size(); point++) {
			// Check if these timestamps are equal
			if (eqltimestamp(checkfor.timeogram, lineup.get(point).timeogram)
					&& (lineup.get(point).ACK) && (checkfor.ACK)) {
				//the timestamps are same for these packets so we shall increment the 
				//trackACK on the packet in queue
				lineup.get(point).addtrack();
				foundinqueue = true;
				break;
			}
		}

		if (lineup.get(point).trackACK == checkfor.timeogram.mytimestamp.size()) {
			// We found all the ACKs we need for this event
			
			// Remove that particular packet from the queue
			gamePacket ackedPacket = lineup.remove(point);
			
			return ackedPacket;
		}
		
		//==========================================
		//By now it is certain that this is the first ACK for this timestamp, so add in queue
		if(!foundinqueue) {
			checkfor.addtrack();
			lineup.add(checkfor);
		}
		// We dont have all the ACKs yet
		return null;
	}

	// checks if timestamps are equal
	private synchronized boolean eqltimestamp(timestamp value1, timestamp value2) {
		boolean isgood = false;
		int point1 = 0, point2 = 0, sum1 = 0, sum2 = 0;
		// Check for both timestmap sizes
		if (value1.mytimestamp.size() != value2.mytimestamp.size()) {
			return isgood;
		}

		for (point1 = 0; point1 < value1.mytimestamp.size(); point1++) {
			sum1 += value1.mytimestamp.get(point1).gettime();
		}

		for (point2 = 0; point2 < value2.mytimestamp.size(); point2++) {
			sum2 = value2.mytimestamp.get(point2).gettime();
		}

		if (sum2 == sum1) {
			isgood = true;
			return isgood;
		}
		return isgood;
	}

	// Print EVERYTHING in this queue
	public synchronized void printQueue() {
		int iterator = 0;

		System.out.println("=====        START OF QUEUE        =====\n");
		System.out.println("Total elements in this queue: " + lineup.size()
				+ "\n\n");

		for (iterator = 0; iterator <= lineup.size(); iterator++) {
			// Print the name of the sender of this packet
			System.out.println("Sender name: "
					+ lineup.elementAt(iterator).senderName + "\n");

			// Print all the players and their timestamp values
			for (int i = 0; i <= lineup.elementAt(iterator).timeogram.mytimestamp
					.size(); i++) {
				System.out
						.println(lineup.elementAt(iterator).timeogram.mytimestamp
								.get(i).playername
								+ " : "
								+ lineup.elementAt(iterator).timeogram.mytimestamp
										.get(i).gettime());
				if (i != lineup.elementAt(iterator).timeogram.mytimestamp
						.size()) {
					System.out.println(" | ");
				} else {
					System.out.println("\n");
				}
			}

			// Print if this is a first sent packet or an ACK packet
			if (lineup.get(iterator).wantACK) {
				System.out.println("Message type: wantACK\n");
			}
			if (lineup.get(iterator).ACK) {
				System.out.println("Message type: ACK\n");
			}
		}

		System.out.println("\n=====        END OF QUEUE        =====\n");
	}
}
