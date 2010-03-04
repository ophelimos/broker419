import java.io.Serializable;

//We will use this packet to transmit across players
/*It consists of:- vector timestamps
 * 				 - type of movement the player has made 
 */

public class gamePacket implements Serializable {
	
	private static final long serialVersionUID = 1L;
	public int action;
	public String senderName;
	public timestamp timeogram;
	
	//True if this packet requires an ACK in return
	boolean wantACK = true;
	
	//True if this packet is returning an ACK
	boolean ACK = false;
	
	MazewarMsg msg;
	
	//Variables for the waiting room phase
	boolean addme= false;
	boolean addedyou = false;
	boolean startgame = false;
	
	String playerlist[]; 
}
