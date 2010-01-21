import java.io.Serializable;

public class EchoPacket implements Serializable {

//	 Since "serializable" evidently wants it, and we get warnings otherwise
	public static final long serialVersionUID = 1;
	
	/* define packet formats */
	public static final int ECHO_NULL    = 0;
	public static final int ECHO_REQUEST = 100;
	public static final int ECHO_REPLY   = 200;
	public static final int ECHO_BYE     = 300;
	
	/* the packet payload */
	
	/* initialized to be a null packet */
	public int type = ECHO_NULL;
	
	/* send your message here */
	public String message;
	
}
