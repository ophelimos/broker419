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
//1.  Modify client.java so that all the notify functions
//(notifyMoveForward() etc.) all do two things:
//
//a)  Updates the timestamp
//b)  Puts a packet with that timestamp and the action (MazewarMsg) on the
//toNetwork queue, where it'll be sent to all other nodes.
//c)  Puts the same packet on the mytodolist queue, where it waits until
//the ACKs come back.
//
//2.  Modify the receivePackets function in MazewarMiddleware to, when an
//ACK comes in, figure out whether we should move a packet from the
//mytodolist queue to the toMaze queue.  You can do it any way you want,
//but I think *you* should be the one who implements it.  This way, you
//can make it work with your haveACK function however you want.

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * An abstract class for clients in a maze.
 * 
 * @author Geoffrey Washburn &lt;<a
 *         href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Client.java 343 2004-01-24 03:43:45Z geoffw $
 */
public abstract class Client implements Serializable {

	// To supress warnings
	private static final long serialVersionUID = 1L;

	/**
	 * Register this {@link Client} as being contained by the specified
	 * {@link Maze}. Naturally a {@link Client} cannot be registered with more
	 * than one {@link Maze} at a time.
	 * 
	 * @param maze
	 *            The {@link Maze} in which the {@link Client} is being placed.
	 */
	public void registerMaze(Maze maze) {
		assert (maze != null);
		assert (this.maze == null);
		this.maze = maze;
	}

	/**
	 * Inform the {@link Client} that it has been taken out of the {@link Maze}
	 * in which it is located. The {@link Client} must already be registered
	 * with a {@link Maze} before this can be called.
	 */
	public void unregisterMaze() {
		assert (maze != null);
		this.maze = null;
	}

	/**
	 * Get the name of this {@link Client}.
	 * 
	 * @return A {@link String} naming the {@link Client}.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Obtain the location of this {@link Client}.
	 * 
	 * @return A {@link Point} specifying the location of the {@link Client}.
	 */
	public Point getPoint() {
		assert (maze != null);
		return maze.getClientPoint(this);
	}

	/**
	 * Find out what direction this {@link Client} is presently facing.
	 * 
	 * @return A Cardinal {@link Direction}.
	 */
	public Direction getOrientation() {
		assert (maze != null);
		return maze.getClientOrientation(this);
	}

	/**
	 * Add an object to be notified when this {@link Client} performs an action.
	 * 
	 * @param cl
	 *            An object that implementing the {@link ClientListener cl}
	 *            interface.
	 */
	public void addClientListener(ClientListener cl) {
		assert (cl != null);
		listenerSet.add(cl);
	}

	/**
	 * Remove an object from the action notification queue.
	 * 
	 * @param cl
	 *            The {@link ClientListener} to remove.
	 */
	public void removeClientListener(ClientListener cl) {
		listenerSet.remove(cl);
	}

	/* Internals ***************************************************** */

	/**
	 * The maze where the client is located. <code>null</code> if not
	 * presently in a maze.
	 */
	protected Maze maze = null;

	/**
	 * Maintain a set of listeners.
	 */
	private Set<ClientListener> listenerSet = new HashSet<ClientListener>();

	/**
	 * Name of the client.
	 */
	private String name = null;

	/**
	 * Create a new client with the specified name.
	 */
	protected Client(String name) {
		assert (name != null);
		this.name = name;
	}

	/**
	 * Move the client forward.
	 * 
	 * @return <code>true</code> if move was successful, otherwise
	 *         <code>false</code>.
	 */
	protected boolean forward() {

		assert (maze != null);

		if (maze.canFire(this)) {
			notifyMoveForward();
			return true;
		} else {
			return false;
		}

		// notifyMoveForward();
		// return true;
	}

	/**
	 * Move the client backward.
	 * 
	 * @return <code>true</code> if move was successful, otherwise
	 *         <code>false</code>.
	 */
	protected boolean backup() {
		assert (maze != null);

		if (maze.canMoveBackward(this)) {
			notifyMoveBackward();
			return true;
		} else {
			return false;
		}
		// notifyMoveBackward();
		// return true;
	}

	/**
	 * Turn the client ninety degrees counter-clockwise.
	 */
	protected void turnLeft() {
		//maze.rotateClientLeft(this);
		notifyTurnLeft();
	}

	/**
	 * Turn the client ninety degrees clockwise.
	 */
	protected void turnRight() {
		//maze.rotateClientRight(this);
		notifyTurnRight();
	}

	/**
	 * Fire a projectile.
	 * 
	 * @return <code>true</code> if a projectile was successfully launched,
	 *         otherwise <code>false</code>.
	 */
	protected boolean fire() {

		assert (maze != null);

		if (maze.canFire(this)) {
			//maze.clientFire(this)
			notifyFire();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Notify listeners that the client moved forward.
	 */
	private void notifyMoveForward() {
		//notifyListeners(ClientEvent.moveForward);
		MazewarMsg msg = new MazewarMsg();
		msg.action = MazewarMsg.MW_MSG_FWD;
		handlelocalEvent(0, msg);
	}

	/**
	 * Notify listeners that the client moved backward.
	 */
	private void notifyMoveBackward() {
		//notifyListeners(ClientEvent.moveBackward);
		MazewarMsg msg = new MazewarMsg();
		msg.action = MazewarMsg.MW_MSG_BKWD;
		handlelocalEvent(1, msg);
	}

	/**
	 * Notify listeners that the client turned right.
	 */
	private void notifyTurnRight() {
		//notifyListeners(ClientEvent.turnRight);
		MazewarMsg msg = new MazewarMsg();
		msg.action = MazewarMsg.MW_MSG_RIGHT;
		handlelocalEvent(3, msg);
	}

	/**
	 * Notify listeners that the client turned left.
	 */
	private void notifyTurnLeft() {
		//notifyListeners(ClientEvent.turnLeft);
		MazewarMsg msg = new MazewarMsg();
		msg.action = MazewarMsg.MW_MSG_LEFT;
		handlelocalEvent(2, msg);
	}

	/**
	 * Notify listeners that the client fired.
	 */
	private void notifyFire() {
		//notifyListeners(ClientEvent.fire);
		MazewarMsg msg = new MazewarMsg();
		msg.action = MazewarMsg.MW_MSG_FIRE;
		handlelocalEvent(4, msg);
	}
	

	 /**
	 * All other players must be listening to this. We shall increment our own
	 * timestamp everytime notifyListener is called
	 */
	private void handlelocalEvent(int theaction, MazewarMsg msg){
		//Increment my own timestamp
		Mazewar.localtimestamp.increment(Mazewar.localName);
		
		gamePacket onetogo = new gamePacket();
		gamePacket fortomaze = new gamePacket();
		
		// Add the MazewarMsg's
		onetogo.msg = msg;
		fortomaze.msg = msg;
		
		//set the action for there packets
		onetogo.setnextmove(theaction);
		
		//Set sender's name in packet
		onetogo.senderName = Mazewar.localName;

		//Set the timestamps for these packets
		onetogo.timeogram = Mazewar.localtimestamp;
		
		//Set this packet as a firsttime packet
		onetogo.wantACK = true;
		
		//Add to the toNETWORK queue
		Mazewar.toNetwork.addtoQueue(onetogo);
		
		//set the action for there packets
		fortomaze.setnextmove(theaction);
		
		//Set sender's name in packet
		fortomaze.senderName = Mazewar.localName;

		//Set the timestamps for these packets
		fortomaze.timeogram = Mazewar.localtimestamp;
		
		//Set this packet as a firsttime packet
		fortomaze.wantACK = true;
		
		//Add to the toMAZE queue | must be in sorted order
		Mazewar.toMaze.addtoSortedQueue(fortomaze);
	}
	
}
