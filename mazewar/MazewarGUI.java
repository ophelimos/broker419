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
	 * The table that displays the available players
	 * 
	 */
	private JList availablePlayers = null;

	/**
	 * And the functions handling its changes
	 */
	PlayerSelectionHandler playerSelectionHandler;

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
	 * Set the title of the window so I can use it for debugging output as well
	 *
	 */
	public void setTitle(String title) {
		this.setTitle(windowTitle + " - " + title);
	}

	/**
	 * Actually start the GUI
	 * 
	 * @param connectionDB
	 */
	public MazewarGUI(ConnectionDB connectionDB) {
		super("ECE419 Mazewar");
		Mazewar.consolePrintLn("Waiting for connections from other players.\n"
				+ "Press the <Start Game> button when you're ready to begin!");

		// Have the ScoreTableModel listen to the maze to find
		// out how to adjust scores.
		ScoreTableModel scoreModel = new ScoreTableModel();
		assert (scoreModel != null);
		Mazewar.maze.addMazeListener(scoreModel);

		// Make the program end properly when I close the window
		WindowHandler windowHandler = new WindowHandler();
		this.addWindowListener(windowHandler);
		
		// Create the GUIClient, but don't connect it to the keyListener yet.
		guiClient = new GUIClient(Mazewar.localName);
		Mazewar.maze.setName(Mazewar.localName);
		Mazewar.maze.addClient(guiClient);

		// Create the panel that will display the maze.
		overheadPanel = new OverheadMazePanel(Mazewar.maze, guiClient);
		assert (overheadPanel != null);
		Mazewar.maze.addMazeListener(overheadPanel);

		// Don't allow editing the console from the GUI
		Mazewar.console.setEditable(false);
		Mazewar.console.setFocusable(false);
		Mazewar.console.setBorder(BorderFactory
				.createTitledBorder(BorderFactory.createEtchedBorder()));

		// Allow the console to scroll by putting it in a scrollpane
		JScrollPane consoleScrollPane = new JScrollPane(Mazewar.console);
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

		// Create the start button
		Button startButton = new Button("Start");
		startButton.setForeground(Color.white);
		startButton.setBackground(Color.red);
		StartButtonListener startButtonListener = new StartButtonListener(this);
		startButton.addMouseListener(startButtonListener);

		// Create the list of available players
		availablePlayers = new JList(connectionDB.getListModel());
		availablePlayers.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(), "Available Players"));

		// Make sure changes get handled
		playerSelectionHandler = new PlayerSelectionHandler(connectionDB, this);
		availablePlayers.addListSelectionListener(playerSelectionHandler);

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
		layout.setConstraints(startButton, c);
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.weightx = 2.0;
		c.weighty = 1.0;
		layout.setConstraints(consoleScrollPane, c);
		c.gridwidth = GridBagConstraints.RELATIVE;
		layout.setConstraints(availablePlayers, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;
		layout.setConstraints(scoreScrollPane, c);

		// Add the components
		getContentPane().add(overheadPanel);
		getContentPane().add(consoleScrollPane);
		getContentPane().add(scoreScrollPane);
		getContentPane().add(availablePlayers);
		getContentPane().add(startButton);

		// Pack everything neatly.
		pack();

		// Let the magic begin.
		setVisible(true);
		overheadPanel.repaint();
		this.requestFocusInWindow();
	}
}
