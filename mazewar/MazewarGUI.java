import java.awt.Button;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/**
 * I'm extending all the main window GUI code into this class, so it doesn't
 * clutter Mazewar()
 */
public class MazewarGUI extends JFrame {

	private static final long serialVersionUID = 1L;

	/**
	 * The window title shown at the top
	 */
	private String windowTitle = "ECE419 Mazewar";
	/**
	 * The {@link GUIClient} for the game.
	 */
	public GUIClient guiClient = null;

	/**
	 * The panel that displays the {@link Maze}.
	 */
	private OverheadMazePanel overheadPanel = null;

	/**
	 * The table the displays the scores.
	 */
	private JTable scoreTable = null;

	/**
	 * The table that displays the available players
	 * 
	 */
	private JList availablePlayers = null;
	
	/**
	 * The start button
	 */
	private Button startButton = null;
	
	private ConnectionDB connectionDB;

	/**
	 * And the functions handling its changes
	 */
	PlayerSelectionHandler playerSelectionHandler = null;

	/**
	 * Selected players (manipulated elsewhere, but this gives a nice central
	 * point for the data structure)
	 * 
	 */
	public Vector<Peer> selectedPlayers = new Vector<Peer>();
	
	/**
	 * Turn on the GUI client and set it to start receiving keystrokes
	 *
	 */
	public void turnOnGUIClient() {

		// The GUI client listens to keystrokes to generate actions
		this.addKeyListener(guiClient);
	}
	
	/**
	 * Turn off the GUI client - might be useful for game end
	 */
	public void turnOffGUIClient() {
		this.removeKeyListener(guiClient);
	}
	
	/**
	 * Add overhead panel - here so we don't need to add clients
	 */
	public void addOverheadPanel() {
		overheadPanel = new OverheadMazePanel(Mazewar.maze, guiClient);
		assert (overheadPanel != null);
		Mazewar.maze.addMazeListener(overheadPanel);
		getContentPane().add(overheadPanel);
		
		// Set its constraints
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 3.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		layout.setConstraints(overheadPanel, c);
		overheadPanel.repaint();
	}
	
	public StartButtonListener startButtonListener = null;
	
	public void addStartButton() {
//		 Create the start button
		startButton = new Button("Start Game");
		startButton.setForeground(Color.white);
		startButton.setBackground(Color.red);
		startButtonListener = new StartButtonListener(this);
		startButton.addMouseListener(startButtonListener);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		layout.setConstraints(startButton, c);
		
		getContentPane().add(startButton);
	}
	
	/**
	 * Remove the start button
	 */
	public void removeStartButton() {
		getContentPane().remove(startButton);
		startButton.removeMouseListener(startButtonListener);
		startButton = null;
	}
	
	public void addAvailablePlayers() {
//		 Create the list of available players
		availablePlayers = new JList(connectionDB.getListModel());
		availablePlayers.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(), "Available Players"));

		// Make sure changes get handled
		playerSelectionHandler = new PlayerSelectionHandler(connectionDB, this);
		availablePlayers.addListSelectionListener(playerSelectionHandler);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.ipadx = 200;
		c.ipady = 200;
		layout.setConstraints(availablePlayers, c);
		
		getContentPane().add(availablePlayers);
	}
	
	/**
	 * Remove the player list
	 */
	public void removeAvailablePlayers() {
		getContentPane().remove(availablePlayers);
		availablePlayers.removeListSelectionListener(playerSelectionHandler);
		availablePlayers = null;
	}
	
	/**
	 * Set the title of the window so I can use it for debugging output as well
	 *
	 */
	public void setWindowTitle(String title) {
		this.setTitle(windowTitle + " - " + title);
	}
	
	/**
	 * The layout manager
	 */
	public GridBagLayout layout = null;
	
	
	/** Start the score table
	 * 
	 */
	
	public void addScoreTable() {
//		 Have the ScoreTableModel listen to the maze to find
		// out how to adjust scores.
		ScoreTableModel scoreModel = new ScoreTableModel();
		assert (scoreModel != null);
		Mazewar.maze.addMazeListener(scoreModel);
		
//		 Create the score table
		scoreTable = new JTable(scoreModel);
		assert (scoreTable != null);
		scoreTable.setFocusable(false);
		scoreTable.setRowSelectionAllowed(false);
		
//		 Allow the score table to scroll too.
		JScrollPane scoreScrollPane = new JScrollPane(scoreTable);
		assert (scoreScrollPane != null);
		scoreScrollPane.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(), "Scores"));
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;
		layout.setConstraints(scoreScrollPane, c);
		
		getContentPane().add(scoreScrollPane);
	}
	
	public void addConsole() {
//		 Don't allow editing the console from the GUI
		Mazewar.console.setEditable(false);
		Mazewar.console.setFocusable(false);
		Mazewar.console.setBorder(BorderFactory
				.createTitledBorder(BorderFactory.createEtchedBorder()));

		// Allow the console to scroll by putting it in a scrollpane
		JScrollPane consoleScrollPane = new JScrollPane(Mazewar.console);
		assert (consoleScrollPane != null);
		consoleScrollPane.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(), "Console"));
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		c.gridx = 0;
		c.gridy = 0;
		c.ipadx = 400;
		c.ipady = 400;
		layout.setConstraints(consoleScrollPane, c);
		
		getContentPane().add(consoleScrollPane);
	}

	/**
	 * Actually start the GUI
	 * 
	 * @param connectionDB
	 */
	public MazewarGUI(ConnectionDB connectionDB) {
		super("ECE419 Mazewar");
		
		this.connectionDB = connectionDB;
		
		Mazewar.consolePrintLn("Waiting for connections from other players.\n"
				+ "Press the <Start Game> button when you're ready to begin!");

		// Make the program end properly when I close the window
		WindowHandler windowHandler = new WindowHandler();
		this.addWindowListener(windowHandler);
		
		// Create the GUIClient, but don't connect it to the keyListener yet.
		guiClient = new GUIClient(Mazewar.localName);
		Mazewar.maze.setName(Mazewar.localName);
		// Don't add GUIClient until the game has actually started
		//Mazewar.maze.addClient(guiClient);

		// Create the layout manager
		layout = new GridBagLayout();
		
		getContentPane().setLayout(layout);
		
		addConsole();
		addAvailablePlayers();
		addStartButton();

		// Pack everything neatly.
		pack();

		// Let the magic begin.
		setVisible(true);
		this.requestFocusInWindow();
	}
}
