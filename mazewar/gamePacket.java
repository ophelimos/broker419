import java.io.Serializable;

//We will use this packet to transmit across players
/*It consists of:- vector timestamps
 * 				 - type of movement the player has made 
 */

public class gamePacket implements Serializable {

	/** ** Constants ***** */
	private static final long serialVersionUID = 1L;

	// Packet types
	public static final int GP_BAD = -1;

	public static final int GP_UNSET = 0;

	public static final int GP_COMMAND = 1;
	public static final int GP_STARTGAME = 2;
	public static final int GP_MYNAME = 3;

	/** **** Variables ****** */
	public int nextmove = 0;

	public int trackACK = 0;

	public String senderName = null;

	public timestamp timeogram = null;

	// True if this packet requires an ACK in return
	public boolean wantACK = false;

	// True if this packet is returning an ACK
	public boolean ACK = false;

	public MazewarMsg msg = null;

	public String playerlist[];
	
	// Packet type
	public int type = GP_UNSET;

	// ===========

	public gamePacket() {
		senderName = Mazewar.localName;
		timeogram = Mazewar.localtimestamp;
	}

	// Copy constructor - just shallow copies for now, since nothing else should
	// be changing
	public gamePacket(gamePacket fromPacket) {
		this.nextmove = fromPacket.nextmove;
		this.trackACK = fromPacket.trackACK;
		this.senderName = fromPacket.senderName;
		this.timeogram = fromPacket.timeogram;
		this.wantACK = fromPacket.wantACK;
		this.ACK = fromPacket.ACK;
		this.msg = fromPacket.msg;
		this.playerlist = fromPacket.playerlist;
	}

	public void setnextmove(int action) {
		nextmove = action;
	}

	public void addtrack() {
		trackACK++;
	}

}
