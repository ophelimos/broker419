import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/**
 * I'm extending all the main window GUI code into this class, so it doesn't clutter Mazewar()
 */
public class MazewarGUI extends JFrame {

	private static final long serialVersionUID = 1L;

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

	

	public MazewarGUI() {
		super("ECE419 Mazewar");
		Mazewar.consolePrintLn("Waiting for connections from other players.\n"
				+ "Press the <Start Game> button when you're ready to begin!");

		// Have the ScoreTableModel listen to the maze to find
		// out how to adjust scores.
		ScoreTableModel scoreModel = new ScoreTableModel();
		assert (scoreModel != null);
		Mazewar.maze.addMazeListener(scoreModel);

		// Create the GUIClient and connect it to the KeyListener queue
		guiClient = new GUIClient(Mazewar.localName);
		Mazewar.maze.setName(Mazewar.localName);
		Mazewar.maze.addClient(guiClient);

		// The GUI client listens to keystrokes to generate actions
		this.addKeyListener(guiClient);
		
		// Make the program end properly when I close the window
		WindowHandler windowHandler = new WindowHandler();
		this.addWindowListener(windowHandler);

		// Create the panel that will display the maze.
		overheadPanel = new OverheadMazePanel(Mazewar.maze, guiClient);
		assert (overheadPanel != null);
		Mazewar.maze.addMazeListener(overheadPanel);

		// Don't allow editing the console from the GUI
		Mazewar.console.setEditable(false);
		Mazewar.console.setFocusable(false);
		Mazewar.console.setBorder(BorderFactory.createTitledBorder(BorderFactory
				.createEtchedBorder()));

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
		startButton.setForeground(Color.red);
		Dimension buttonSize = startButton.getSize();
		Mazewar.consolePrintLn("Button width = " + buttonSize.getWidth()
				+ " and button height = " + buttonSize.height);
		StartButtonListener startButtonListener = new StartButtonListener();
		startButton.addMouseListener(startButtonListener);

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
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;
		layout.setConstraints(scoreScrollPane, c);

		// Add the components
		getContentPane().add(overheadPanel);
		getContentPane().add(consoleScrollPane);
		getContentPane().add(scoreScrollPane);
		getContentPane().add(startButton);

		// Pack everything neatly.
		pack();

		// Let the magic begin.
		setVisible(true);
		overheadPanel.repaint();
		this.requestFocusInWindow();
	}
}
