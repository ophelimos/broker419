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

import java.lang.Thread;
import java.lang.Runnable;
import java.io.Serializable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.Vector;  
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Collection;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.concurrent.locks.*;

/**
 * A concrete implementation of a {@link Maze}.  
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: MazeImpl.java 371 2004-02-10 21:55:32Z geoffw $
 */

public class MazeImpl extends Maze implements Serializable, CommListener, Runnable {

	private static final long serialVersionUID = 1L;

		/**
         * Create a {@link Maze}.
         * @param point Treat the {@link Point} as a magintude specifying the
         * size of the maze.
         * @param seed Initial seed for the random number generator.
         */
        public MazeImpl(Point point, long seed) {
                maxX = point.getX();
                assert(maxX > 0);
                maxY = point.getY();
                assert(maxY > 0);
                
                //this.isConnected = false;
                this.lock = new ReentrantLock();
                this.isConnected = lock.newCondition();
                
                
                // Initialize the maze matrix of cells
                mazeVector = new Vector<Vector<CellImpl>>(maxX);
                for(int i = 0; i < maxX; i++) {
                        Vector<CellImpl> colVector = new Vector<CellImpl>(maxY);
                        
                        for(int j = 0; j < maxY; j++) {
                                colVector.insertElementAt(new CellImpl(), j);
                        }
                        
                        mazeVector.insertElementAt(colVector, i);
                }

                thread = new Thread(this);

                // Initialized the random number generator
                randomGen = new Random(seed);
                
                // Build the maze starting at the corner
                buildMaze(new Point(0,0));
                
                thread.start();
        }
       
        public boolean isConnected()
        {
        	lock.lock();
        	try{
        	isConnected.await();
        	} catch (Exception ex) {
        		ex.printStackTrace();
        	}
        	
        	
        	lock.unlock();
        
        	return true;
        	
        }
     
       
        /** 
         * Create a maze from a serialized {@link MazeImpl} object written to a file.
         * @param mazefile The filename to load the serialized object from.
         * @return A reconstituted {@link MazeImpl}. 
         */
        public static Maze readMazeFile(String mazefile)
                throws IOException, ClassNotFoundException {
                        assert(mazefile != null);
                        FileInputStream in = new FileInputStream(mazefile);
                        ObjectInputStream s = new ObjectInputStream(in);
                        Maze maze = (Maze) s.readObject();
                        
                        return maze;
                }
        
        /** 
         * Serialize this {@link MazeImpl} to a file.
         * @param mazefile The filename to write the serialized object to.
         * */
        public void save(String mazefile)
                throws IOException {
                        assert(mazefile != null);
                        FileOutputStream out = new FileOutputStream(mazefile);
                        ObjectOutputStream s = new ObjectOutputStream(out);
                        s.writeObject(this);
                        s.flush();
                }
       
        /** 
         * Display an ASCII version of the maze to stdout for debugging purposes.  
         */
        public void print() {
                for(int i = 0; i < maxY; i++) {
                        for(int j = 0; j < maxX; j++) {
                                CellImpl cell = getCellImpl(new Point(j,i));
                                if(j == maxY - 1) {
                                        if(cell.isWall(Direction.South)) {
                                                System.out.print("+-+");
                                        } else {
                                                System.out.print("+ +");
                                        }
                                } else {
                                        if(cell.isWall(Direction.South)) {
                                                System.out.print("+-");
                                        } else {
                                                System.out.print("+ ");
                                        }
                                }
                                
                        }	    
                        System.out.print("\n");
                        for(int j = 0; j < maxX; j++) {
                                CellImpl cell = getCellImpl(new Point(j,i));
                                if(cell.getContents() != null) {
                                        if(cell.isWall(Direction.West)) {
                                                System.out.print("|*");
                                        } else {
                                                System.out.print(" *");
                                        }
                                } else {
                                        if(cell.isWall(Direction.West)) {
                                                System.out.print("| ");
                                        } else {
                                                System.out.print("  ");
                                        }
                                }
                                if(j == maxY - 1) {
                                        if(cell.isWall(Direction.East)) {
                                                System.out.print("|");
                                        } else {
                                                System.out.print(" ");
                                        }
                                }
                        }
                        System.out.print("\n");
                        if(i == maxX - 1) {
                                for(int j = 0; j < maxX; j++) {
                                        CellImpl cell = getCellImpl(new Point(j,i));
                                        if(j == maxY - 1) {
                                                if(cell.isWall(Direction.North)) {
                                                        System.out.print("+-+");
                                                } else {
                                                        System.out.print("+ +");
                                                }
                                        } else {
                                                if(cell.isWall(Direction.North)) {
                                                        System.out.print("+-");
                                                } else {
                                                        System.out.print("+ ");
                                                }
                                        }		
                                }
                                System.out.print("\n");     
                        }   
                }
                
        }
       
        
        public boolean checkBounds(Point point) {
                assert(point != null);
                return (point.getX() >= 0) && (point.getY() >= 0) && 
                        (point.getX() < maxX) && (point.getY() < maxY);
        }
        
        public Point getSize() {
                return new Point(maxX, maxY);
        }
        
        public synchronized Cell getCell(Point point) {
                assert(point != null);
                return getCellImpl(point);
        }
        
        public synchronized void addClient(Client client) {
                assert(client != null);
                // Pick a random starting point, and check to see if it is already occupied
                Point point = new Point(randomGen.nextInt(maxX),randomGen.nextInt(maxY));
                CellImpl cell = getCellImpl(point);
                // Repeat until we find an empty cell
                while(cell.getContents() != null) {
                        point = new Point(randomGen.nextInt(maxX),randomGen.nextInt(maxY));
                        cell = getCellImpl(point);
                } 
                
                Direction d = Direction.random();
                while(cell.isWall(d)) {
                  d = Direction.random();
                }
                
                addClient(client, point, d);
        }
        
        public synchronized void addRemoteClient(Client c, Point p, Direction o) {
            assert(c != null);
            assert(p != null);
            assert(o != null);
            
            addClient(c,p,o);
    }
        public synchronized Point getClientPoint(Client client) {
                assert(client != null);
                Object o = clientMap.get(client);
                assert(o instanceof Point);
                return (Point)o;
        }
        
        public synchronized Direction getClientOrientation(Client client) {
                assert(client != null);
                Object o = clientMap.get(client);
                assert(o instanceof DirectedPoint);
                DirectedPoint dp = (DirectedPoint)o;
                return dp.getDirection();
        }
       
        public synchronized void removeClient(Client client) {
                assert(client != null);
                Object o = clientMap.remove(client);
                assert(o instanceof Point);
                Point point = (Point)o;
                CellImpl cell = getCellImpl(point);
                cell.setContents(null);
                clientMap.remove(client);
                
                client.unregisterMaze();
                //client.removeClientListener(this);
                update();
                name2clientLookup.remove(client.getName());
                notifyClientRemove(client);
        }

        public synchronized boolean clientFire(Client client) {
                assert(client != null);
                // If the client already has a projectile in play
                // fail.
                if(clientFired.contains(client)) {
                        return false;
                }
                
                Point point = getClientPoint(client);
                Direction d = getClientOrientation(client);
                CellImpl cell = getCellImpl(point);
                
                /* Check that you can fire in that direction */
                if(cell.isWall(d)) {
                        return false;
                }
                
                DirectedPoint newPoint = new DirectedPoint(point.move(d), d);
                /* Is the point withint the bounds of maze? */
                assert(checkBounds(newPoint));
                
                CellImpl newCell = getCellImpl(newPoint);
                Object contents = newCell.getContents();
                if(contents != null) {
                        // If it is a Client, kill it outright
                        if(contents instanceof Client) {
                                notifyClientFired(client);
                                killClient(client, (Client)contents);
                                update();
                                return true; 
                        } else {
                        // Otherwise fail (bullets will destroy each other)
                                return false;
                        }
                }
                
                clientFired.add(client);
                Projectile prj = new Projectile(client);
                
                /* Write the new cell */
                projectileMap.put(prj, newPoint);
                newCell.setContents(prj);
                notifyClientFired(client);
                update();
                return true; 
        }
        
        public synchronized boolean moveClientForward(Client client) {
                assert(client != null);
                Object o = clientMap.get(client);
                assert(o instanceof DirectedPoint);
                DirectedPoint dp = (DirectedPoint)o;
                return moveClient(client, dp.getDirection());
        }
        
        public synchronized boolean moveClientBackward(Client client) {
                assert(client != null);
                Object o = clientMap.get(client);
                assert(o instanceof DirectedPoint);
                DirectedPoint dp = (DirectedPoint)o;
                return moveClient(client, dp.getDirection().invert());
        }
        
       
        public synchronized Iterator<Client> getClients() {
                return clientMap.keySet().iterator();
        }
        
        
        public void addMazeListener(MazeListener ml) {
                listenerSet.add(ml);
        }

        public void removeMazeListener(MazeListener ml) {
                listenerSet.remove(ml);
        }

        /**
         * Listen for notifications about action performed by 
         * {@link Client}s in the maze.
         * @param c The {@link Client} that acted.
         * @param ce The action the {@link Client} performed.
         */


        public void commClientUpdate(CommClientWrapper cw, ClientEvent ce, CommClientWrapper cw_optional) 
        { 
                
        	if (ce.equals(ClientEvent.client_added_fin))   
        	{
                	lock.lock();
                	isConnected.signal();
                	lock.unlock();
            }
        	else if (ce.equals(ClientEvent.turnLeft))
        	{
        		if (!cw.name.equals(this.name))
        		{
        			Client c = name2clientLookup.get(cw.name);
        			rotateClientLeft(c);
        		}
        	}
        	else if (ce.equals(ClientEvent.turnRight))
        	{        		
        		if (!cw.name.equals(this.name))
        		{
	    			Client c = name2clientLookup.get(cw.name);
	    			rotateClientRight(c);
    		
        		}
        	}
        	else if (ce.equals(ClientEvent.moveForward))
        	{
        		if (!cw.name.equals(this.name))
        		{
	    			Client c = name2clientLookup.get(cw.name);
	    			moveClientForward(c);
    		
        		}
        	}
        	else if (ce.equals(ClientEvent.moveBackward))
        	{
        		if (!cw.name.equals(this.name))
        		{
	    			Client c = name2clientLookup.get(cw.name);
	    			moveClientBackward(c);
    		
        		}
        	}
        	else if (ce.equals(ClientEvent.fire))
        	{
        		if (!cw.name.equals(this.name))
        		{
	    			Client c = name2clientLookup.get(cw.name);
	    			clientFire(c);
    		
        		}
        	}
        	else if(ce.equals(ClientEvent.client_added))
        	{
        		if (!cw.name.equals(this.name))
        		{
	        		//this.addRemoteClient(c);
	        		Client c = new RemoteClient(cw.name);
	        		this.addRemoteClient(c,cw.point,cw.orientation);
	        		System.out.println("MazeImpl: Remote Client to be added: "+ c.getName());
        		}
        	}
        	else if(ce.equals(ClientEvent.client_removed))
        	{
        		if (!cw.name.equals(this.name))
        		{
        			Client c = name2clientLookup.get(cw.name);
        			this.removeClient(c);
        		}
        	}
        	else if (ce.equals(ClientEvent.client_killed))
        	{
        		Client target = name2clientLookup.get(cw_optional.name);
        		Client source = name2clientLookup.get(cw.name);
        		if (target instanceof GUIClient)
			    {
				  // do nothing
			    }
        		else
        		{
			    	this.killRemoteClient(source,target, cw_optional.point, cw_optional.orientation);
			  
        		}
        	}
            	
            update();
        
        }
               
        
        

        /**
         * Set the name of the maze client (null if server)
         */
        
        public void setName(String name)
        {
        	this.name = name; 
        }
        /**
         * Control loop for {@link Projectile}s.
         */
        public void run() {
                Collection<Object> deadPrj = new HashSet<Object>();
                while(true) {
                        if(!projectileMap.isEmpty()) {
                                Iterator<Object> it = projectileMap.keySet().iterator();
                                synchronized(projectileMap) {
                                        while(it.hasNext()) {   
                                                Object o = it.next();
                                                assert(o instanceof Projectile);
                                                deadPrj.addAll(moveProjectile((Projectile)o));
                                        }               
                                        it = deadPrj.iterator();
                                        while(it.hasNext()) {
                                                Object o = it.next();
                                                assert(o instanceof Projectile);
                                                Projectile prj = (Projectile)o;
                                                projectileMap.remove(prj);
                                                clientFired.remove(prj.getOwner());
                                        }
                                        deadPrj.clear();
                                }
                        }
                        try {
                                Thread.sleep(200);
                        } catch(Exception e) {
                                // shouldn't happen
                        }
                }
        }
        
        /* Internals */
        
        private synchronized Collection<Object> moveProjectile(Projectile prj) {
                Collection<Object> deadPrj = new LinkedList<Object>();
                assert(prj != null);
                
                Object o = projectileMap.get(prj);
                assert(o instanceof DirectedPoint);
                DirectedPoint dp = (DirectedPoint)o;
                Direction d = dp.getDirection();
                CellImpl cell = getCellImpl(dp);
                
                /* Check for a wall */
                if(cell.isWall(d)) {
                        // If there is a wall, the projectile goes away.
                        cell.setContents(null);
                        deadPrj.add(prj);
                        update();
                        return deadPrj;
                }
                
                DirectedPoint newPoint = new DirectedPoint(dp.move(d), d);
                /* Is the point within the bounds of maze? */
                assert(checkBounds(newPoint));
                
                CellImpl newCell = getCellImpl(newPoint);
                Object contents = newCell.getContents();
                if(contents != null) {
                        // If it is a Client, kill it outright
                        if(contents instanceof Client) {
                                killClient(prj.getOwner(), (Client)contents);
                                cell.setContents(null);
                                deadPrj.add(prj);
                                update();
                                return deadPrj;
                        } else {
                        // Bullets destroy each other
                                assert(contents instanceof Projectile);
                                newCell.setContents(null);
                                cell.setContents(null);
                                deadPrj.add(prj);
                                deadPrj.add(contents);
                                update();
                                return deadPrj;
                        }
                }

                /* Clear the old cell */
                cell.setContents(null);
                /* Write the new cell */
                projectileMap.put(prj, newPoint);
                newCell.setContents(prj);
                update();
                return deadPrj;
        }
        /**
         * Internal helper for adding a {@link Client} to the {@link Maze}.
         * @param client The {@link Client} to be added.
         * @param point The location the {@link Client} should be added.
         */
        private synchronized void addClient(Client client, Point point, Direction d) {
                assert(client != null);
                assert(checkBounds(point));
                CellImpl cell = getCellImpl(point);
                
                cell.setContents(client);
                clientMap.put(client, new DirectedPoint(point, d));
                client.registerMaze(this);
                //client.addClientListener(this);
                update();
                
                name2clientLookup.put(client.getName(), client);
                notifyClientAdd(client);
        }
        
        /**
         * Internal helper for handling the death of a {@link Client}.
         * @param source The {@link Client} that fired the projectile.
         * @param target The {@link Client} that was killed.
         */
        
        //this killClient only executes if target = GUIclient, that is
        //the target decides if they got killed
        private synchronized void killRemoteClient(Client source, Client target, Point p, Direction d)
        {
            Mazewar.consolePrintLn(source.getName() + " just vaporized " + 
                    target.getName());
            
        	//     	this will always be done here
            Object o = clientMap.remove(target);
            assert(o instanceof Point);
         
            Point point = (Point)o;
            CellImpl cell = getCellImpl(point);
            cell.setContents(null);
                        
            cell = getCellImpl(p);
            cell.setContents(target);
            clientMap.put(target, new DirectedPoint(p, d));
            update();
            
            notifyClientKilled(source, target);
        	
        }
        
        
        private synchronized void killClient(Client source, Client target) {
                assert(source != null);
                assert(target != null);
                if (target instanceof GUIClient)
                {
	                Mazewar.consolePrintLn(source.getName() + " just vaporized " + 
	                                target.getName());
	                
	                //this will always be done here
	                Object o = clientMap.remove(target);
	                assert(o instanceof Point);
	             
	                Point point = (Point)o;
	                CellImpl cell = getCellImpl(point);
	                cell.setContents(null);
	                
	                //this is only done if the target is LocalClient 
	                // Pick a random starting point, and check to see if it is already occupied
	                point = new Point(randomGen.nextInt(maxX),randomGen.nextInt(maxY));
	                cell = getCellImpl(point);
	                // Repeat until we find an empty cell
	                while(cell.getContents() != null) {
	                        point = new Point(randomGen.nextInt(maxX),randomGen.nextInt(maxY));
	                        cell = getCellImpl(point);
	                }
	                Direction d = Direction.random();
	                while(cell.isWall(d)) {
	                        d = Direction.random();
	                }
	                
//	              this will be done by the callback
	                cell.setContents(target);
	                clientMap.put(target, new DirectedPoint(point, d));
	                update();
	                
	                notifyClientKilled(source, target);

                }
        }
        
        /**
         * Internal helper called when a {@link Client} emits a turnLeft action.
         * @param client The {@link Client} to rotate.
         */
        public synchronized void rotateClientLeft(Client client) {
                assert(client != null);
                Object o = clientMap.get(client);
                assert(o instanceof DirectedPoint);
                DirectedPoint dp = (DirectedPoint)o;
                clientMap.put(client, new DirectedPoint(dp, dp.getDirection().turnLeft()));
               // update();
        }
        
        /**
         * Internal helper called when a {@link Client} emits a turnRight action.
         * @param client The {@link Client} to rotate.
         */
        public synchronized void rotateClientRight(Client client) {
                assert(client != null);
                Object o = clientMap.get(client);
                assert(o instanceof DirectedPoint);
                DirectedPoint dp = (DirectedPoint)o;
                clientMap.put(client, new DirectedPoint(dp, dp.getDirection().turnRight()));
              // update();
        }
        
        /**
         * Internal helper called to move a {@link Client} in the specified
         * {@link Direction}.
         * @param client The {@link Client} to be move.
         * @param d The {@link Direction} to move.
         * @return If the {@link Client} cannot move in that {@link Direction}
         * for some reason, return <code>false</code>, otherwise return 
         * <code>true</code> indicating success.
         */
        private synchronized boolean moveClient(Client client, Direction d) {
                assert(client != null);
                assert(d != null);
                Point oldPoint = getClientPoint(client);
                CellImpl oldCell = getCellImpl(oldPoint);
                
                /* Check that you can move in the given direction */
                if(oldCell.isWall(d)) {
                        /* Move failed */
                        clientMap.put(client, oldPoint);
                        return false;
                }
                
                DirectedPoint newPoint = new DirectedPoint(oldPoint.move(d), getClientOrientation(client));
                
                /* Is the point withint the bounds of maze? */
                assert(checkBounds(newPoint));
                CellImpl newCell = getCellImpl(newPoint);
                if(newCell.getContents() != null) {
                        /* Move failed */
                        clientMap.put(client, oldPoint);
                        return false;
                }
                
                /* Write the new cell */
                clientMap.put(client, newPoint);
                newCell.setContents(client);
                /* Clear the old cell */
                oldCell.setContents(null);	
                
                //update();
                return true; 
        }
       
        private Lock lock;
        private Condition isConnected;
        
        //private boolean  isConnected = false;
        private final Map<String, Client> name2clientLookup = new HashMap<String, Client>();
        /**
         * Client name (null if server)
         */
        private  String name;
        /**
         * The random number generator used by the {@link Maze}.
         */
        private final Random randomGen;

        /**
         * The maximum X coordinate of the {@link Maze}.
         */
        private final int maxX;

        /**
         * The maximum Y coordinate of the {@link Maze}.
         */
        private final int maxY;

        /** 
         * The {@link Vector} of {@link Vector}s holding the
         * {@link Cell}s of the {@link Maze}.
         */
        private final Vector<Vector<CellImpl>> mazeVector;

        /**
         * A map between {@link Client}s and {@link DirectedPoint}s
         * locating them in the {@link Maze}.
         */
        private final Map<Client, Point> clientMap = new HashMap<Client, Point>();

        /**
         * The set of {@link MazeListener}s that are presently
         * in the notification queue.
         */
        private final Set<MazeListener> listenerSet = new HashSet<MazeListener>();

        /**
         * Mapping from {@link Projectile}s to {@link DirectedPoint}s. 
         */
        private final Map<Object, DirectedPoint> projectileMap = new HashMap<Object, DirectedPoint>();
        
        /**
         * The set of {@link Client}s that have {@link Projectile}s in 
         * play.
         */
        private final Set<Client> clientFired = new HashSet<Client>();
       
        /**
         * The thread used to manage {@link Projectile}s.
         */
        private final Thread thread;
        
        /**
         * Generate a notification to listeners that a
         * {@link Client} has been added.
         * @param c The {@link Client} that was added.
         */
        private void notifyClientAdd(Client c) {
                assert(c != null);
                Iterator<MazeListener> i = listenerSet.iterator();
                while (i.hasNext()) {
                        Object o = i.next();
                        assert(o instanceof MazeListener);
                        MazeListener ml = (MazeListener)o;
                        ml.clientAdded(c);
                } 
        }
        
        /**
         * Generate a notification to listeners that a 
         * {@link Client} has been removed.
         * @param c The {@link Client} that was removed.
         */
        private void notifyClientRemove(Client c) {
                assert(c != null);
                Iterator<MazeListener> i = listenerSet.iterator();
                while (i.hasNext()) {
                        Object o = i.next();
                        assert(o instanceof MazeListener);
                        MazeListener ml = (MazeListener)o;
                        ml.clientRemoved(c);
                } 
        }
        
        /**
         * Generate a notification to listeners that a
         * {@link Client} has fired.
         * @param c The {@link Client} that fired.
         */
        private void notifyClientFired(Client c) {
                assert(c != null);
                Iterator<MazeListener> i = listenerSet.iterator();
                while (i.hasNext()) {
                        Object o = i.next();
                        assert(o instanceof MazeListener);
                        MazeListener ml = (MazeListener)o;
                        ml.clientFired(c);
                } 
        }
        
        /**
         * Generate a notification to listeners that a
         * {@link Client} has been killed.
         * @param source The {@link Client} that fired the projectile.
         * @param target The {@link Client} that was killed.
         */
        private void notifyClientKilled(Client source, Client target) {
                assert(source != null);
                assert(target != null);
                Iterator<MazeListener> i = listenerSet.iterator();
                while (i.hasNext()) {
                        Object o = i.next();
                        assert(o instanceof MazeListener);
                        MazeListener ml = (MazeListener)o;
                        ml.clientKilled(source, target);
                } 
        }
        
        /**
         * Generate a notification that the {@link Maze} has 
         * changed in some fashion.
         */
        private void update() {
                Iterator<MazeListener> i = listenerSet.iterator();
                while (i.hasNext()) {
                        Object o = i.next();
                        assert(o instanceof MazeListener);
                        MazeListener ml = (MazeListener)o;
                        ml.mazeUpdate();
                } 
        }

        /**
         * A concrete implementation of the {@link Cell} class that
         * special to this implementation of {@link Maze}s.
         */
        private class CellImpl extends Cell implements Serializable {

			private static final long serialVersionUID = 1L;

				/**
                 * Has this {@link CellImpl} been visited while
                 * constructing the {@link Maze}.
                 */
                private boolean visited = false;

                /**
                 * The walls of this {@link Cell}.
                 */
                private boolean walls[] = {true, true, true, true};

                /**
                 * The contents of the {@link Cell}. 
                 * <code>null</code> indicates that it is empty.
                 */
                private Object contents = null;
                
                /**
                 * Helper function to convert a {@link Direction} into 
                 * an array index for easier access.
                 * @param d The {@link Direction} to convert.
                 * @return An integer index into <code>walls</code>.
                 */
                private int directionToArrayIndex(Direction d) {
                        assert(d != null);
                        if(d.equals(Direction.North)) {
                                return 0;
                        } else if(d.equals(Direction.East)) {
                                return 1;
                        } else if(d.equals(Direction.South)) {
                                return 2;
                        } else if(d.equals(Direction.West)) {
                                return 3;
                        }
                        /* Impossible */
                        return -1; 
                }
                
                /* Required for the abstract implementation */
                
                public boolean isWall(Direction d) {
                        assert(d != null);
                        return this.walls[directionToArrayIndex(d)];
                }
                
                public synchronized Object getContents() {
                        return this.contents;
                }
                
                /* Internals used by MazeImpl */
                
                /**
                 * Indicate that this {@link Cell} has been
                 * visited while building the {@link MazeImpl}.
                 */
                public void setVisited() {
                        visited = true;
                }
                
                /**
                 * Has this {@link Cell} been visited in the process
                 * of recursviely building the {@link Maze}?
                 * @return <code>true</code> if visited, <code>false</code> 
                 * otherwise.
                 */
                public boolean visited() {
                        return visited;
                }
                
                /**
                 * Add a wall to this {@link Cell} in the specified
                 * Cardinal {@link Direction}.
                 * @param d Which wall to add.
                 */
                public void setWall(Direction d) {
                        assert(d != null);
                        this.walls[directionToArrayIndex(d)] = true;
                }
                
                /**
                 * Remove the wall from this {@link Cell} in the specified
                 * Cardinal {@link Direction}.
                 * @param d Which wall to remove.
                 */
                public void removeWall(Direction d) {
                        assert(d != null);
                        this.walls[directionToArrayIndex(d)] = false;
                }
                
                /**
                 * Set the contents of this {@link Cell}.
                 * @param contents Object to place in the {@link Cell}.
                 * Use <code>null</code> if you want to empty it.
                 */
                public synchronized void setContents(Object contents) {
                        this.contents = contents;
                }
                
        }
        
        /** 
         * Removes the wall in the {@link Cell} at the specified {@link Point} and 
         * {@link Direction}, and the opposite wall in the adjacent {@link Cell}.
         * @param point Location to remove the wall.
         * @param d Cardinal {@link Direction} specifying the wall to be removed.
         */
        private void removeWall(Point point, Direction d) {
                assert(point != null);
                assert(d != null);
                CellImpl cell = getCellImpl(point);
                cell.removeWall(d);
                Point adjacentPoint = point.move(d);
                CellImpl adjacentCell = getCellImpl(adjacentPoint);
                adjacentCell.removeWall(d.invert());
        }
        
        /** 
         * Pick randomly pick an unvisited neighboring {@link CellImpl}, 
         * if none return <code>null</code>. 
         * @param point The location to pick a neighboring {@link CellImpl} from.
         * @return The Cardinal {@link Direction} of a {@link CellImpl} that hasn't
         * yet been visited.
         */
        private Direction pickNeighbor(Point point) {
                assert(point != null);
                Direction directions[] = { 
                        Direction.North, 
                        Direction.East, 
                        Direction.West, 
                        Direction.South };
                        
                        // Create a vector of the possible choices
                        Vector<Direction> options = new Vector<Direction>();	       
                        
                        // Iterate through the directions and see which
                        // Cells have been visited, adding those that haven't
                        for(int i = 0; i < 4; i++) {
                                Point newPoint = point.move(directions[i]);
                                if(checkBounds(newPoint)) {
                                        CellImpl cell = getCellImpl(newPoint);
                                        if(!cell.visited()) {
                                                options.add(directions[i]);
                                        }
                                }
                        }
                        
                        // If there are no choices just return null
                        if(options.size() == 0) {
                                return null;
                        }
                        
                        // If there is at least one option, randomly choose one.
                        int n = randomGen.nextInt(options.size());
                        
                        Object o = options.get(n);
                        assert(o instanceof Direction);
                        return (Direction)o;
        }
        
        /**
         * Recursively carve out a {@link Maze}
         * @param point The location in the {@link Maze} to start carving.
         */
        private void buildMaze(Point point) {
                assert(point != null);
                CellImpl cell = getCellImpl(point);
                cell.setVisited();
                Direction d = pickNeighbor(point);
                while(d != null) {	    
                        removeWall(point, d);
                        Point newPoint = point.move(d);
                        buildMaze(newPoint);
                        d = pickNeighbor(point);
                }
        }
       
        /** 
         * Obtain the {@link CellImpl} at the specified point. 
         * @param point Location in the {@link Maze}.
         * @return The {@link CellImpl} representing that location.
         */
        private CellImpl getCellImpl(Point point) {
                assert(point != null);
                Object o1 = mazeVector.get(point.getX());
                assert(o1 instanceof Vector);
                Vector v1 = (Vector)o1;
                Object o2 = v1.get(point.getY());
                assert(o2 instanceof CellImpl);
                return (CellImpl)o2;
        }
}
