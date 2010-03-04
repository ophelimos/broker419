/*
 * This class implements a queue for every client in the game
 * The queue holds 'gamePackets' objects as elements
 * 
 * The queue is sorted at all times, lowest time stamp value at top of queue and highest
 * at bottom
 * 
 * Before an elements is added it is sorted and inserted in the right place
 */
import java.util.*;

public class clientQueue {
	protected Vector<gamePacket> lineup = new Vector<gamePacket>(0, 1);

	// Use this to add an element to the communicating queue
	public boolean addtoQueue(gamePacket toadd) {
		lineup.add(toadd);
		return true;
	}

	// Use this to add an element to the communicating queue
	public void addtoSortedQueue(gamePacket toadd) {
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

	private boolean checktimestamp(gamePacket toinsert, gamePacket tocheck) {
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

	public gamePacket getElement() {
		// This lineup will return firstpacket, and then remove it as well
		if (!lineup.isEmpty()) {
			return lineup.remove(0);
		} else {
			return null;
		}

	}

	// Check if we have all the ACKs for a certain gamePacket we sent out
	// earlier
	public boolean haveACK(gamePacket checkfor) {
		int point = 0, n = 0, trackACK = 0;
		boolean haveit = false;

		for (point = 0; point < lineup.size(); point++) {
			// Check if these timestamps are equal
			if (eqltimestamp(checkfor.timeogram, lineup.get(point).timeogram)
					&& (lineup.get(point).ACK)) {
				for (n = 0; n < checkfor.timeogram.mytimestamp.size(); n++) {
					if (checkfor.timeogram.mytimestamp.get(n).getplayer()
							.equalsIgnoreCase(lineup.get(point).senderName)) {
						trackACK++;
						break;
					}
				}
			}

			if (trackACK == checkfor.timeogram.mytimestamp.size()) {
				// We found all the ACKs we need for this event
				haveit = true;
				break;
			}
		}

		if (trackACK == checkfor.timeogram.mytimestamp.size()) {
			// We found all the ACKs we need for this event
			return haveit;
		}

		// We dont have all the ACKs yet
		return haveit;
	}

	// checks if timestamps are equal
	private boolean eqltimestamp(timestamp value1, timestamp value2) {
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
	
	//Print EVERYTHING in this queue
	private void printQueue(){
		int iterator =0;
		
		System.out.println("=====        START OF QUEUE        =====\n");
		System.out.println("Total elements in this queue: " + lineup.size() + "\n\n");
		
		for (iterator =0; iterator <= lineup.size(); iterator++){
			//Print the name of the sender of this packet
			System.out.println("Sender name: " + lineup.elementAt(iterator).senderName + "\n");
			
			//Print all the players and their timestamp values  
			for(int i =0; i <= lineup.elementAt(iterator).timeogram.mytimestamp.size(); i++) {
				System.out.println(lineup.elementAt(iterator).timeogram.mytimestamp.get(i).playername + " : " + lineup.elementAt(iterator).timeogram.mytimestamp.get(i).gettime());
				if (i != lineup.elementAt(iterator).timeogram.mytimestamp.size()){
					System.out.println(" | ");
				}
				else {
					System.out.println("\n");
				}				
			}
			
			//Print if this is a first sent packet or an ACK packet
			if (lineup.get(iterator).wantACK){
				System.out.println("Message type: wantACK\n");
			}
			if (lineup.get(iterator).ACK){
				System.out.println("Message type: ACK\n");
			}
		}
		
		System.out.println("\n=====        END OF QUEUE        =====\n");
	}
}
