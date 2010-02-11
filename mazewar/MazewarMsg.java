
import java.io.Serializable;

public class MazewarMsg implements Serializable {

	/* define packet formats */
	public static final int MW_MSG_BAD			= -1;
	
	// client actions 
	public static final int MW_MSG_ACTION_TYPE = 0;
	public static final int MW_MSG_FWD    		= 1;
	public static final int MW_MSG_BKWD 		= 2;
	public static final int MW_MSG_LEFT    		= 3;
	public static final int MW_MSG_RIGHT     	= 4;
	public static final int MW_MSG_FIRE			= 5; 
	

	
	// client administration (add/remove/kill/etc..)
	public static final int MW_MSG_CLIENT_ADMIN_TYPE = 1000; 
	public static final int MW_MSG_CLIENT_ADDED = 1001;
	public static final int MW_MSG_CLIENT_REMOVED = 1002;
	public static final int MW_MSG_CLIENT_ADDED_FIN = 1003;
	
	public static final int MW_MSG_CLIENT_KILLED = 1004;
	/* the msg payload */
	
	public int action = MW_MSG_BAD;
	
	public CommClientWrapper cw;
	public CommClientWrapper cw_optional = null; // this is used for client kills
 
	
}
