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
	protected Vector<timestamp> lineup = new Vector<timestamp>(10,1);
	
	//Use this to add an element to the queue
	public boolean addElement(timestamp toadd){
		
		return false;
	}
	
	//Take the element at the top of the list and MC it to 
	public boolean waitACK(timestamp waitingfor){
		return true;
	}
	
}
