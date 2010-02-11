import java.io.Serializable;
import java.util.*;
public class MazewarPacket implements Serializable {

	/* define packet formats */
	public static final int MW_NULL    		= 0;
	public static final int MW_UPDATE 		= 100;
	public static final int MW_BROADCAST    = 200;
	public static final int MW_BYE     		= 300;
	
	/* the packet payload */
	
	/* initialized to be a null packet */
	public int type = MW_NULL;
	//public int sequenceNumber = -1; 
	public MazewarMsg msg; 

	/*public int compare(Object p1, Object p2)
	{
		return ((MazewarPacket)p1).sequenceNumber - ((MazewarPacket)p2).sequenceNumber;
	}*/
}
