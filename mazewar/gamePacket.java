//We will use this packet to transmit across players
/*It consists of:- vector timestamps
 * 				 - type of movement the player has made 
 */

public class gamePacket {
	
	public int action;
	public String senderName;
	public timestamp timeogram;
	
	//True if this packet requires an ACK in return
	boolean wantACK = true;
	
	//True if this packet is returning an ACK
	boolean ACK = false;
}
