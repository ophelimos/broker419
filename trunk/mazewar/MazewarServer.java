import java.net.*;
import java.io.*;


public class MazewarServer {
	
    public static void main(String[] args) throws IOException {
           	
        ServerSocket serverSocket = null;
        Socket clientSocket = null; 
        ObjectOutputStream toClient = null;
        ObjectInputStream fromClient = null;

        try {
        	if(args.length == 1) {
        		serverSocket = new ServerSocket(Integer.parseInt(args[0]));
        		System.err.println("MazeWarServer: so far so good");
        	} else {
        		System.err.println("ERROR: Invalid arguments!");
        		System.exit(-1);
        	}
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }
         
        
	clientSocket = serverSocket.accept();
	toClient = new ObjectOutputStream(clientSocket.getOutputStream());
	fromClient =  new ObjectInputStream(clientSocket.getInputStream());
 	
	boolean gotByePacket = false;
		
	try {
		
		/* stream to read from client */
		MazewarPacket packetFromClient = (MazewarPacket) fromClient.readObject();
		
		while ( packetFromClient!= null) 
		{
			System.out.println(packetFromClient.toString());
			if (MazewarPacket.MW_UPDATE == packetFromClient.type)
			{						
				// send it back to the client	
				toClient.writeObject(packetFromClient);  
			}
			else
			{
				/* if code comes here, there is an error in the packet */
				System.err.println("ERROR: Unknown Mazewar_* packet!!");
				System.exit(-1);
			}
				
			// Get the next packet:
			packetFromClient = (MazewarPacket) fromClient.readObject();
		}
			
		/* cleanup when client exits */
		fromClient.close();
			
	} catch (IOException e) {
		if(!gotByePacket)
			e.printStackTrace();
	} catch (ClassNotFoundException e) {
		if(!gotByePacket)
			e.printStackTrace();
	}
		
	
        serverSocket.close();
    }
}
