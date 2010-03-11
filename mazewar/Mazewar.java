/*
 Copyright (C) 2004 Geoffrey Alan Washburn
 Copyright (C) 2010 James Robinson and Jay Suthar
 
 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 3
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;

/**
 * The entry point and glue code for the game. It also contains some helpful
 * global utility methods.
 * 
 * @author Geoffrey Washburn &lt;<a
 *         href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Mazewar.java 371 2004-02-10 21:55:32Z geoffw $
 */

public class Mazewar extends JFrame {

	// The state the game is in
	private static int status = 0;

	public static final int STATUS_WAITING = 0;

	public static final int STATUS_INVITING = 1;

	public static final int STATUS_PLAYING = 2;

	// The people we're actually playing with right now
	public static Vector<RemoteClient> actualPlayers = new Vector<RemoteClient>();

	// The global port SLP is running on
	public static int slpPort = 2048;

	// My IP address
	public static String hostname = null;

	// The global port all direct network conenctions are running on
	public static int directPort = slpPort - 1;

	// Whether or not I'm accepting new connections. Setting this to false kills
	// the ConnectionAcceptor thread.
	public static boolean acceptingNewConnections = true;

	// SLP properties file name (constant)
	public static final String slpPropertiesFileName = "jslp.properties";

	// Jay's stuff
	private static vectorobj personalinfo;

	public static timestamp localtimestamp;

	// Global queues
	public static clientQueue waitingForAcks;

	public static clientQueue toNetwork;

	public static clientQueue toMaze;

	public static final int maxPlayers = 4;

	// To get rid of silly warnings
	private static final long serialVersionUID = (long) 1;

	// Program usage string
	private static final String usageString = "Usage: mazewar [-c] [-p <port>] [-n <name>]";

	// Console-mode variable: don't bother with the GUI, and print everything to
	// the screen
	public static boolean consoleMode = false;

	// We're going to make name global
	public static String localName = null;

	// SLP and middleware servers
	public static MazewarSLP slpServer = null;

	public static MazewarMiddlewareServer middlewareServer = null;

	// The socket I receive connections from other people on
	public static ServerSocket serverSocket = null;
	
	/**
	 * The pointer to the GUI object
	 */
	public static MazewarGUI mazewarGUI = null;

	/**
	 * The {@link Maze} that the game uses.
	 */
	public static MazeImpl maze = null;

	/**
	 * The default width of the {@link Maze}.
	 */
	private static final int mazeWidth = 20;

	/**
	 * The default height of the {@link Maze}.
	 */
	private static final int mazeHeight = 10;

	/**
	 * The default random seed for the {@link Maze}. All implementations of the
	 * same protocol must use the same seed value, or your mazes will be
	 * different.
	 */
	private static final int mazeSeed = 42;

	/**
	 * Create the textpane statically so that we can write to it globally using
	 * the static consolePrint methods
	 */
	public static JTextPane console = null;

	/**
	 * Write a message to the console followed by a newline.
	 * 
	 * @param msg
	 *            The {@link String} to print.
	 */
	public static synchronized void consolePrintLn(String msg) {
		if (consoleMode) {
			System.out.println(msg);
		} else {
			console.setText(console.getText() + msg + "\n");
		}
	}

	/**
	 * Write a message to the console.
	 * 
	 * @param msg
	 *            The {@link String} to print.
	 */
	public static synchronized void consolePrint(String msg) {
		if (consoleMode) {
			System.out.print(msg);
		} else {
			console.setText(console.getText() + msg);
		}
	}

	/**
	 * Clear the console.
	 */
	public static synchronized void clearConsole() {
		if (consoleMode) {
			System.out.println("<clear screen>");
		} else {
			console.setText("");
		}
	}

	public static void writePort(int port) {
		// Two try statements to catch IOExceptions in the first catch block
		try {
			try {
				// Read a line at a time
				Scanner slpConfigInput = new Scanner(new BufferedReader(
						new FileReader(slpPropertiesFileName)))
						.useDelimiter("\n");

				// Write the file to a temporary file first, and then overwrite,
				// rather
				// than accidentally truncating it
				File cwd = new File(System.getProperty("user.dir"));
				File tempFile = File.createTempFile("slpprop", ".tmp", cwd);
				BufferedWriter slpConfigOutput = new BufferedWriter(
						new FileWriter(tempFile));

				while (slpConfigInput.hasNext()) {

					// Copy from input to output
					String input = slpConfigInput.next();

					if (input.contains("net.slp.port")) {
						slpConfigOutput.write("net.slp.port = " + port);
					} else {
						slpConfigOutput.write(input);
					}

				}

				// Flush the temporary file
				slpConfigOutput.flush();

				// Now, move the old file to a backup file
				File propFile = new File(slpPropertiesFileName);
				String backupFileName = slpPropertiesFileName.concat(".bak");
				File backupFile = new File(backupFileName);
				boolean success = propFile.renameTo(backupFile);
				if (!success) {
					IOException noRename = new IOException(
							"Failed to rename file" + slpPropertiesFileName
									+ " to " + backupFileName);
					throw noRename;
				}

				// Rename the temporary file to the database file
				success = tempFile.renameTo(propFile);
				if (!success) {
					IOException noRename = new IOException(
							"Failed to rename temp file to "
									+ slpPropertiesFileName);
					throw noRename;
				}

			} catch (FileNotFoundException e) {
				System.out
						.println("jslp.properties not found or invalid, creating new one...");
				BufferedWriter slpConfigOutput = new BufferedWriter(
						new FileWriter(slpPropertiesFileName));
				slpConfigOutput.write("net.slp.port = " + port);
				slpConfigOutput.flush();
			}
		} catch (IOException e) {
			System.out
					.println("Failed to mess with the jslp.properties file correctly");
		}
	}

	/**
	 * Synchronized getters and setters, since status could be modified by
	 * multiple threads
	 * 
	 * @return
	 */

	public static synchronized boolean setInviting() {
		if (status == STATUS_INVITING) {
			consolePrintLn("Wait until your current invitation is processed before trying again");
			return false;
		}

		if (status == STATUS_PLAYING) {
			consolePrintLn("Can't start two games at once");
			return false;
		}

		mazewarGUI.setTitle("Inviting");
		status = STATUS_INVITING;
		return true;
	}

	public static synchronized boolean setPlaying() {
		status = STATUS_PLAYING;
		mazewarGUI.setTitle("Playing");
		return true;
	}

	public static synchronized boolean setWaiting() {
		status = STATUS_WAITING;
		mazewarGUI.setTitle("Waiting");
		return true;
	}

	public static synchronized int getStatus() {
		return status;
	}

	/**
	 * Entry point for the game.
	 * 
	 * @param args
	 *            Command-line arguments.
	 */
	public static void main(String args[]) {

		// Read off command-line arguments

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-c")) {
				consoleMode = true;
				continue;
			} else if (args[i].equals("-p")) {
				if (i + 1 < args.length) {
					slpPort = Integer.parseInt(args[i + 1]);
					// Write the port value into the SLP configuration file
					writePort(slpPort);
					// Skip the next value
					i++;
				} else {
					System.out.println(usageString);
				}
			} else if (args[i].equals("-n")) {
				if (i + 1 < args.length) {
					localName = args[i + 1];
					i++;
				} else {
					System.out.println(usageString);
				}
			}
		}

		// We need to have a name, so if we haven't been given one on the
		// command line, get it now, since nothing starts before we have a name

		if (localName == null) {
			if (!consoleMode) {
				try {
					while ((localName == null) || (localName.length() == 0)) {
						localName = JOptionPane
								.showInputDialog("Enter your name");
						if ((localName == null) || (localName.length() == 0)) {
							consolePrintLn("Error: Invalid name");
							continue;
						}
					}
				} catch (NullPointerException e) {
					consolePrintLn("Error: Invalid name");
					System.exit(1);
				}
			} else {
				System.out.println("Enter your name:");
				BufferedReader cin = new BufferedReader(new InputStreamReader(
						System.in));
				try {
					localName = cin.readLine();
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		}

		// Create the maze
		maze = new MazeImpl(new Point(mazeWidth, mazeHeight), mazeSeed);
		assert (maze != null);

		// Set up network queues
		// 1001010B
		// personalinfo is only used once, when the timestamp for htis player is
		// created
		personalinfo = new vectorobj(0, localName);

		// This is our local timestamp
		/* ==== HANDLE WITH CARE ==== */
		localtimestamp = new timestamp(personalinfo);

		// This is our local queue to do things on my side of the world
		/* ==== HANDLE WITH CARE ==== */
		waitingForAcks = new clientQueue();
		toNetwork = new clientQueue();
		toMaze = new clientQueue();

		// 1001010E

		// Create the database to handle connections
		ConnectionDB connectionDB = new ConnectionDB();

		// Create our local server socket, so that we can receive connections
		// from other people.

		ConnectionAcceptor connectionAcceptor = new ConnectionAcceptor(
				connectionDB, directPort);
		connectionAcceptor.start();

		// Use braces to force constructors not to be called at the beginning of
		// the constructor.
		{
			// maze.addClient(new RobotClient("Norby"));
			// maze.addClient(new RobotClient("Robbie"));
			// maze.addClient(new RobotClient("Clango"));
			// maze.addClient(new RobotClient("Marvin"));
		}

		// Figure out who I am
		try {
			InetAddress addr;
			addr = InetAddress.getLocalHost();
			hostname = addr.getHostAddress();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}

		/* Set up the networking using SLP */
		slpServer = new MazewarSLP(connectionDB);
		slpServer.start();

		// Make things get closed properly on exit
		Runtime.getRuntime().addShutdownHook(new ShutdownThread());

		try {
			/* Create the GUI */
			if (!consoleMode) {
				console = new JTextPane();
				mazewarGUI = new MazewarGUI(connectionDB);
				/* Create and start the communications middleware */
				middlewareServer = new MazewarMiddlewareServer(connectionDB,
						maze, slpServer, mazewarGUI);
				middlewareServer.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// System.out.println("GUI startup thread finished");
	}
}