import ch.ethz.iks.slp.Advertiser;
import ch.ethz.iks.slp.ServiceLocationException;
import ch.ethz.iks.slp.ServiceURL;

/**
 * Thread that runs server shutdown to ensure everything is cleanly closed
 * 
 * @author robin162
 * 
 */

public class ShutdownThread extends Thread {

	private Advertiser advertiser;
	private ServiceURL mazewarService;
	
	public ShutdownThread(Advertiser advertiser_in, ServiceURL mazewarService_in) {
		super("ShutdownThread");
		this.advertiser = advertiser_in;
		this.mazewarService = mazewarService_in;
	}

	public void run() {
		System.out.println("Shutting down cleanly...");

		if (advertiser != null) {
			try {
				advertiser.deregister(mazewarService);
			} catch (ServiceLocationException e) {
				System.out.println("Failed to properly shut down jSLP server");
				System.out.println("Error Code: " + e.getErrorCode());
				e.printStackTrace();
			}
		}
	}
}