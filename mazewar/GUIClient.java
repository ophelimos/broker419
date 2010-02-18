/*
Copyright (C) 2004 Geoffrey Alan Washburn
      
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
      
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
      
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
USA.
*/

import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

/**
 * An implementation of {@link LocalClient} that is controlled by the keyboard
 * of the computer on which the game is being run.  
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: GUIClient.java 343 2004-01-24 03:43:45Z geoffw $
 */

public class GUIClient extends LocalClient implements KeyListener {

	private static final long serialVersionUID = 1L;

	//1001010B
	//personalinfo is only used once, when the timestamp for htis player is created
	vectorobj personalinfo = new vectorobj(0, getName());
	
	//This is our local timestamp
	/* ==== HANDLE WITH CARE ==== */
	timestamp localtimestamp = new timestamp(personalinfo);
	
	//This is our local queue to do things on my side of the world
	/* ==== HANDLE WITH CARE ==== */
	clientQueue mytodoList = new clientQueue();
	/*TODO Do we need to increment our timestamp here in the sense that we just successfully created a 
	local player and think of it as an event?
	*/
	//1001010E
	
    /**
     * Create a GUI controlled {@link LocalClient}.
     */
    public GUIClient(String name) {
            super(name);
    }
    
    /**
     * Handle a key press.
     * @param e The {@link KeyEvent} that occurred.
     */
    
    /* The procedure for below is as follows
    - Wait for all the other clients to send ACK and then permit keyPressed to update the local mazewar
    */
    
    public void keyPressed(KeyEvent e) {
        // If the user pressed Q, invoke the cleanup code and quit. 
        if((e.getKeyChar() == 'q') || (e.getKeyChar() == 'Q')) {
        	//Increment our own timestamp
        	localtimestamp.increment(getName());
        	//Add this event to queue
        	mytodoList.addElement(localtimestamp);
        	//Wait in Queue for the ACK
        	if(mytodoList.waitACK(localtimestamp)){
	        	//Continue
	            System.exit(0);
        	}
        // Up-arrow moves forward.
        } else if(e.getKeyCode() == KeyEvent.VK_UP) {
        	//Increment our own timestamp
    		localtimestamp.increment(getName());
        	//Add this event to queue
        	//Wait in Queue for the ACK
        	
        	//Continue
            forward();
        // Down-arrow moves backward.
        } else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
        	//Increment our own timestamp
    		localtimestamp.increment(getName());
        	//Add this event to queue
        	//Wait in Queue for the ACK
        	
        	//Continue
            backup();
        // Left-arrow turns left.
        } else if(e.getKeyCode() == KeyEvent.VK_LEFT) {
        	//Increment our own timestamp
    		localtimestamp.increment(getName());
        	//Add this event to queue
        	//Wait in Queue for the ACK
        	
        	//Continue
    		turnLeft();
        // Right-arrow turns right.
        } else if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
        	//Increment our own timestamp
    		localtimestamp.increment(getName());
        	//Add this event to queue
        	//Wait in Queue for the ACK
        	
        	//Continue
    		turnRight();
        // Spacebar fires.
        } else if(e.getKeyCode() == KeyEvent.VK_SPACE) {
        	//Increment our own timestamp
    		localtimestamp.increment(getName());
        	//Add this event to queue
        	//Wait in Queue for the ACK
        	
        	//Continue
    		fire();
        }
    }
    
    /**
     * Handle a key release. Not needed by {@link GUIClient}.
     * @param e The {@link KeyEvent} that occurred.
     */
    public void keyReleased(KeyEvent e) {
    }
    
    /**
     * Handle a key being typed. Not needed by {@link GUIClient}.
     * @param e The {@link KeyEvent} that occurred.
     */
    public void keyTyped(KeyEvent e) {
    }

}