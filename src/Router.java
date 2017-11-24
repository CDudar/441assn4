
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;
import cpsc441.a4.shared.*;

/**
 * Router Class
 * 
 * This class implements the functionality of a router
 * when running the distance vector routing algorithm.
 * 
 * The operation of the router is as follows:
 * 1. send/receive HELLO message
 * 2. while (!QUIT)
 *      receive ROUTE messages
 *      update mincost/nexthop/etc
 * 3. Cleanup and return
 * 
 *      
 * @author 	Majid Ghaderi
 * @version	3.0
 *
 */
public class Router {
	
	
	
	int routerId;
	int updateInterval;
	String serverName;
	int serverPort;
	
	Timer timer;
	
	
	Socket socket;
	ObjectOutputStream out;
	ObjectInputStream in;
	
	RtnTable routingTable;
	
    /**
     * Constructor to initialize the rouer instance 
     * 
     * @param routerId			Unique ID of the router starting at 0
     * @param serverName		Name of the host running the network server
     * @param serverPort		TCP port number of the network server
     * @param updateInterval	Time interval for sending routing updates to neighboring routers (in milli-seconds)
     */
	public Router(int routerId, String serverName, int serverPort, int updateInterval) {
		// to be completed

			System.out.println("Initializing Router Object");
			this.routerId = routerId;
			this.updateInterval = updateInterval;
			this.serverName = serverName;
			this.serverPort = serverPort;

		
	}
	

    /**
     * starts the router 
     * 
     * @return The forwarding table of the router
     */
	public RtnTable start() {
		// to be completed
		
		
		try {
			System.out.println("Connecting to " + serverName);
			System.out.println("Listening at " + serverPort);
			
			
			socket = new Socket(serverName, serverPort);
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
			
			
			System.out.println("Writing Hello packet to server");
			DvrPacket hello = new DvrPacket(0, 100, 1);
			out.writeObject(hello);
			out.flush();
			
			
			System.out.println("Reading in Hello Packet from server");
			DvrPacket serverResponse = (DvrPacket) in.readObject();
			

			int[] minCost = serverResponse.mincost;
			int numRouters = minCost.length;

			System.out.println("Number of routers " + numRouters);
			System.out.println("Min cost array:\n" + minCost);

			
			System.out.println("Starting timer...");
			Timer timer = new Timer(true);
			timer.schedule(new TimeOutHandler(this), updateInterval);	
			
			
			System.out.println("Starting packet receiving loop");
			DvrPacket receive;
			do
			{
				
				receive = (DvrPacket) in.readObject();
				processDVR(receive);
				
			}while(receive.type != 2);
			
			
			System.out.println("Received Quit Packet");
			System.out.println("Closing socket, cancelling timer");
			timer.cancel();
			socket.close();
			
			
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		
		return new RtnTable();
	}
	
	
	void processDVR(DvrPacket dvr) {
		
		
		
		
	}
	
	void processTimeOut() {
		
		
	}
	

	
	
    /**
     * A simple test driver
     * 
     */
	public static void main(String[] args) {
		// default parameters
		int routerId = 0;
		String serverName = "localhost";
		int serverPort = 2227;
		int updateInterval = 1000; //milli-seconds
		
		if (args.length == 4) {
			routerId = Integer.parseInt(args[0]);
			serverName = args[1];
			serverPort = Integer.parseInt(args[2]);
			updateInterval = Integer.parseInt(args[3]);
		} else {
			System.out.println("incorrect usage, try again.");
			System.exit(0);
		}
			
		// print the parameters
		System.out.printf("starting Router #%d with parameters:\n", routerId);
		System.out.printf("Relay server host name: %s\n", serverName);
		System.out.printf("Relay server port number: %d\n", serverPort);
		System.out.printf("Routing update intwerval: %d (milli-seconds)\n", updateInterval);
		
		// start the router
		// the start() method blocks until the router receives a QUIT message
		Router router = new Router(routerId, serverName, serverPort, updateInterval);
		RtnTable rtn = router.start();
		System.out.println("Router terminated normally");
		
		// print the computed routing table
		System.out.println();
		System.out.println("Routing Table at Router #" + routerId);
		System.out.print(rtn.toString());
	}

}
