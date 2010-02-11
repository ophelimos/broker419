import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;

public class MazewarComm extends Thread implements ClientListener, MazeListener {

	private Socket socket = null;
	private ObjectOutputStream socketOut = null;
	private ObjectInputStream socketIn = null;
	private PriorityQueue<MazewarPacket> serverInQueue = null; 
	
	/**
     * Maintain a set of listeners.
     */
    private Set listenerSet = new HashSet();
    
	private int sequenceLastEx = -1; 
	
	public MazewarComm(String hostname, int port ) { 
			
			if(hostname.length() == 0 || port < 1000) { 
				System.err.println("ERROR: Invalid arguments!");
				System.exit(-1);
			}
			try {  
				socket = new Socket(hostname, port);
				socketIn = new ObjectInputStream(socket.getInputStream());
				socketOut = new ObjectOutputStream(socket.getOutputStream());
				//socketIn = new ObjectInputStream(socket.getInputStream());
				serverInQueue = new PriorityQueue<MazewarPacket>();
			} catch (UnknownHostException e)
			{
				System.err.println("ERROR: Don't know where to connect!!");
				System.exit(1);
			} catch (IOException e)	{
				System.err.println("ERROR: Couldn't get I/O for the connection.");
				System.exit(1);
			} 
	}
	
	public boolean sendMsg(MazewarMsg msg)
	{
		
		MazewarPacket packet = new MazewarPacket(); 
		
		packet.msg = msg; 
		packet.type = MazewarPacket.MW_UPDATE; 
		
		try { 
			socketOut.writeObject(packet);
		} catch(IOException e)
		{
		 //TODO yeah whatever status
			return false; 
		}
		
		return true; 
	}
	

	
	/*
	 * implements ClientListener's clientUpdate 
	 */
	public void clientUpdate(Client c, ClientEvent ce) {
        // When a client turns, update our state.
		MazewarMsg msg = new MazewarMsg();
		msg.cw = new CommClientWrapper(c.getName(),c.getPoint(),c.getOrientation());
		
        if(ce.equals (ClientEvent.turnLeft)) {
                msg.action = MazewarMsg.MW_MSG_LEFT;
        } else if(ce.equals (ClientEvent.turnRight)) {
        		msg.action = MazewarMsg.MW_MSG_RIGHT;
        }else if(ce.equals (ClientEvent.moveForward)) {
        		msg.action = MazewarMsg.MW_MSG_FWD;
        }else if(ce.equals (ClientEvent.moveBackward)) {
        		msg.action = MazewarMsg.MW_MSG_BKWD;
        }else if (ce.equals(ClientEvent.fire)) {
        	msg.action = MazewarMsg.MW_MSG_FIRE;
        }
        sendMsg(msg);
	}

	/*
	 * implements MazeListener's mazeUpdate()
	 */
	public void mazeUpdate()
	{
		// do nothing	
	}
	
	/*
	 * implements MAzeListener's clientAdded();
	 */
	public void clientAdded(Client c)
	{
		if (c instanceof GUIClient)
		{
			MazewarMsg msg = new MazewarMsg();
			msg.cw = new CommClientWrapper(c.getName(),c.getPoint(),c.getOrientation());
			
			
			msg.action = MazewarMsg.MW_MSG_CLIENT_ADDED;
			sendMsg(msg);
		}
		else
		{
			// do nothing 
		}
	}
	
	/*
	 * implements MazeListener's clientRemoved();
	 */
	public void clientRemoved(Client c)
	{
		if (c instanceof GUIClient)
		{
			MazewarMsg msg = new MazewarMsg();
			msg.cw = new CommClientWrapper(c.getName());
			
			msg.action = MazewarMsg.MW_MSG_CLIENT_REMOVED;
			sendMsg(msg);
		}
	}
	
	/*
	 * implements MazeListener's clientKilled();
	 */
	public void clientKilled(Client source, Client target)
	{
		if (target instanceof GUIClient)
		{
			MazewarMsg msg = new MazewarMsg();
			msg.cw = new CommClientWrapper(source.getName(),source.getPoint(),source.getOrientation());
			msg.cw_optional = new CommClientWrapper(target.getName(),target.getPoint(),target.getOrientation());
			msg.action = MazewarMsg.MW_MSG_CLIENT_KILLED;
			sendMsg(msg);
		}
	}
	
	/*
	 * implements MazeListerner's clientFired
	 */
	public void clientFired(Client c)
	{
		
	}
    public void addCommListener(CommListener cl) {
        assert(cl != null);
        listenerSet.add(cl);
    }

	/**
	 * Remove an object from the action notification queue.
	 * @param cl The {@link ClientListener} to remove.
	 */
	public void removeCommListener(CommListener cl) {
	        listenerSet.remove(cl);
	}
	
    private void notifyListeners(MazewarMsg msg) {
    	ClientEvent ce = null;  
    	switch (msg.action)
    	{
	    	case MazewarMsg.MW_MSG_LEFT: 
	    		ce = ClientEvent.turnLeft;
	    		break; 
	    	case MazewarMsg.MW_MSG_RIGHT:
	    		ce = ClientEvent.turnRight;
	    		break; 
	    	case MazewarMsg.MW_MSG_FWD:
	    		ce = ClientEvent.moveForward; 
	    		break;
	    	case MazewarMsg.MW_MSG_BKWD:
	    		ce = ClientEvent.moveBackward;
	    		break;
	    	case MazewarMsg.MW_MSG_FIRE:
	    		ce = ClientEvent.fire; 
	    		break;
	    	case MazewarMsg.MW_MSG_CLIENT_ADDED:
	    		ce = ClientEvent.client_added;
	    		break;
	    	case MazewarMsg.MW_MSG_CLIENT_REMOVED:
	    		ce = ClientEvent.client_removed;
	    		break;
	    	case MazewarMsg.MW_MSG_CLIENT_ADDED_FIN:
	    		ce = ClientEvent.client_added_fin;
	    		break;
	    	case MazewarMsg.MW_MSG_CLIENT_KILLED:
	    		ce = ClientEvent.client_killed;
	    		break;
	    	default: 
	    		//TODO: enter error code
	    			break;    			
    	}
    	
        Iterator i = listenerSet.iterator();
        while (i.hasNext()) {
                Object o = i.next();
                assert(o instanceof CommListener);
                CommListener cl = (CommListener)o;
                cl.commClientUpdate(msg.cw, ce,msg.cw_optional);
        } 
}
    
	public void run() {
		MazewarPacket packet = null; 
		try
		{ 

			packet = (MazewarPacket)socketIn.readObject(); 
			while(packet != null)
			{
				serverInQueue.add(packet); 
				

				packet = serverInQueue.peek(); 
				/* Since there is no guarantee that packets arrive in order from the server
				 * we need to make sure that we execute packets in order 
				 */
				if ((packet != null))
				{
					packet = serverInQueue.poll(); //remove the packet from the queue 
					
					//need to process the message 
					notifyListeners(packet.msg);
					
					sequenceLastEx++; // increment the sequence number
					//look at the next packet to see if we can process anymore messages
					packet = serverInQueue.peek();
				}
				
				packet = (MazewarPacket)socketIn.readObject(); 
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
