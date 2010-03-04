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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;

import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The entry point and glue code for the game. It also contains some helpful
 * global utility methods.
 * 
 * @author Geoffrey Washburn &lt;<a
 *         href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Mazewar.java 371 2004-02-10 21:55:32Z geoffw $
 */

public class Mazewar extends JFrame {

	// The global port everything's running on
	public static int port = 2048;

	// SLP properties file name (constant)
	public static final String slpPropertiesFileName = "jslp.properties";

	// Jay's stuff
	private static vectorobj personalinfo;

	public static timestamp localtimestamp;

	// Global queues
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

	public static MazewarComm middlewareServer = null;

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
	 * The {@link GUIClient} for the game.
	 */
	private GUIClient guiClient = null;

	/**
	 * The panel that displays the {@link Maze}.
	 */
	private OverheadMazePanel overheadPanel = null;

	/**
	 * The table the displays the scores.
	 */
	private JTable scoreTable = null;

	/**
	 * Create the textpane statically so that we can write to it globally using
	 * the static consolePrint methods
	 */
	private static JTextPane console = null;

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

	/**
	 * GUI code
	 */
	public Mazewar() {
		super("ECE419 Mazewar");
		consolePrintLn("Mazewar started!");

		// Have the ScoreTableModel listen to the maze to find
		// out how to adjust scores.
		ScoreTableModel scoreModel = new ScoreTableModel();
		assert (scoreModel != null);
		maze.addMazeListener(scoreModel);

		// Create the GUIClient and connect it to the KeyListener queue
		guiClient = new GUIClient(localName);
		maze.setName(localName);
		maze.addClient(guiClient);

		this.addKeyListener(guiClient);

		// the middleware needs to listen to GUIClient to update the server
		guiClient.addClientListener(middlewareServer);

		// Use braces to force constructors not to be called at the beginning of
		// the constructor.
		{
			// maze.addClient(new RobotClient("Norby"));
			// maze.addClient(new RobotClient("Robbie"));
			// maze.addClient(new RobotClient("Clango"));
			// maze.addClient(new RobotClient("Marvin"));
		}

		// Create the panel that will display the maze.
		overheadPanel = new OverheadMazePanel(maze, guiClient);
		assert (overheadPanel != null);
		maze.addMazeListener(overheadPanel);

		// Don't allow editing the console from the GUI
		console.setEditable(false);
		console.setFocusable(false);
		console.setBorder(BorderFactory.createTitledBorder(BorderFactory
				.createEtchedBorder()));

		// Allow the console to scroll by putting it in a scrollpane
		JScrollPane consoleScrollPane = new JScrollPane(console);
		assert (consoleScrollPane != null);
		consoleScrollPane.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(), "Console"));

		// Create the score table
		scoreTable = new JTable(scoreModel);
		assert (scoreTable != null);
		scoreTable.setFocusable(false);
		scoreTable.setRowSelectionAllowed(false);

		// Allow the score table to scroll too.
		JScrollPane scoreScrollPane = new JScrollPane(scoreTable);
		assert (scoreScrollPane != null);
		scoreScrollPane.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(), "Scores"));

		// Create the layout manager
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		getContentPane().setLayout(layout);

		// Define the constraints on the components.
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 3.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		layout.setConstraints(overheadPanel, c);
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.weightx = 2.0;
		c.weighty = 1.0;
		layout.setConstraints(consoleScrollPane, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;
		layout.setConstraints(scoreScrollPane, c);

		// Add the components
		getContentPane().add(overheadPanel);
		getContentPane().add(consoleScrollPane);
		getContentPane().add(scoreScrollPane);

		// Pack everything neatly.
		pack();

		// Let the magic begin.
		setVisible(true);
		overheadPanel.repaint();
		this.requestFocusInWindow();
	}

	public static void writePort(int port) {

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
					port = Integer.parseInt(args[i + 1]);
					// Write the port value into the SLP configuration file
					writePort(port);
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
		try {
			while ((localName == null) || (localName.length() == 0)) {
				localName = JOptionPane.showInputDialog("Enter your name");
				if ((localName == null) || (localName.length() == 0)) {
					consolePrintLn("Error: Invalid name");
					continue;
				}
			}
		} catch (NullPointerException e) {
			consolePrintLn("Error: Invalid name");
			System.exit(1);
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
		clientQueue mytodoList = new clientQueue();
		toNetwork = new clientQueue();
		toMaze = new clientQueue();

		// 1001010E

		/* Set up the networking using SLP */
		slpServer = new MazewarSLP();
		slpServer.start();

		// Make things get closed properly on exit
		Runtime.getRuntime().addShutdownHook(new ShutdownThread());

		/* Create and start the communications middleware */
		middlewareServer = new MazewarComm(slpServer);
		middlewareServer.addCommLocalListener((MazeImpl) maze);
		middlewareServer.start();

		// The middleware is now a listener for maze events
		maze.addMazeListener(middlewareServer);

		try {
			/* Create the GUI */
			if (!consoleMode) {
				console = new JTextPane();
				new Mazewar();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// We shouldn't get here, but if we do, let everyone know
		System.out.println("GUI thread finished");
	}
}
