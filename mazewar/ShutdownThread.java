import ch.ethz.iks.slp.ServiceLocationException;

/**
 * Thread that runs server shutdown to ensure everything is cleanly closed
 * 
 * @author robin162
 * 
 */

public class ShutdownThread extends Thread {
	
	public ShutdownThread() {
		super("ShutdownThread");
	}

	public void run() {
		System.out.println("Shutting down cleanly...");

		if (MazewarSLP.advertiser != null) {
			try {
				MazewarSLP.advertiser.deregister(MazewarSLP.mazewarService);
			} catch (ServiceLocationException e) {
				System.out.println("Failed to properly shut down jSLP server");
				System.out.println("Error Code: " + e.getErrorCode());
				e.printStackTrace();
			}
		}
	}
}