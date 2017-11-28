
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
	TimeOutHandler timeoutHandler;
	
	ScheduledExecutorService sExecService;
	Future<?> future;
	
	Socket socket;
	ObjectOutputStream out;
	ObjectInputStream in;
	
	int[] linkCost;
	int[][] minCost;
	int[] nextHop;
	int numRouters;
	
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
			DvrPacket hello = new DvrPacket(routerId, DvrPacket.SERVER, DvrPacket.HELLO);
			out.writeObject(hello);
			out.flush();
			
			
			System.out.println("Reading in Hello Packet from server");
			DvrPacket serverResponse = (DvrPacket) in.readObject();
			
			numRouters = serverResponse.mincost.length;
			
			linkCost = new int[numRouters];
			linkCost = serverResponse.mincost;
			
			System.out.println("Link Cost");
			printArray(linkCost);
			
			minCost = new int[numRouters][numRouters];
			minCost[routerId] = linkCost.clone();
			
			
			System.out.println("Min cost received");
			printArray(minCost[routerId]);
			
			nextHop = new int[numRouters];
			

			
			//initializing next hop routers (will only be neighbors at this point)
			for(int i = 0 ; i < numRouters; i++) {
				if(minCost[routerId][i] != 999) {
					nextHop[i] = i;
				}
				else {
					nextHop[i] = -1;
				}
				
			}
			
			System.out.println("Next hop");
			printArray(nextHop);

			System.out.println("Number of routers " + numRouters);
			System.out.println("Min cost array:\n" + minCost);
			
			for(int i = 0 ; i < numRouters; i++) {
				System.out.println(minCost[i]);
			}
			/**
			System.out.println("Experiment");
			minCost[routerId][0] = 1;
			minCost[routerId][1] = 1;
			minCost[routerId][2] = 1;
			minCost[routerId][3] = 1;
			System.out.println("mincost after changes");
			printArray(minCost[routerId]);
			System.out.println("Link cost after changes");
			printArray(linkCost);
			*/
			
			System.out.println("Starting timer...");
			
			sExecService = Executors.newScheduledThreadPool(1);
			future = sExecService.scheduleAtFixedRate(new TimeOutHandler(this), (long)updateInterval, (long)updateInterval, TimeUnit.MILLISECONDS);
			
			//Timer timer = new Timer(true);
			//timeoutHandler = new TimeOutHandler(this);
			//timer.schedule(timeoutHandler, updateInterval, updateInterval);	
			
			
			System.out.println("Starting packet receiving loop");
			DvrPacket receive;
			do
			{
				System.out.println("Received packet");
				receive = (DvrPacket) in.readObject();
				processDVR(receive);
				
			}while(receive.type != DvrPacket.QUIT);
			
			
			System.out.println("Received Quit Packet");
			System.out.println("Closing socket, cancelling timer");
			sExecService.shutdown();
			socket.close();
			
			
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		
		
		
		return new RtnTable(minCost[routerId], nextHop);
	}
	
	
	synchronized void processDVR(DvrPacket dvr) {
		
		if(dvr.type == DvrPacket.QUIT)
			return;
		
		
		int senderId = dvr.sourceid;
		
		if(senderId == DvrPacket.SERVER) {
			System.out.println("New routing table formation");
			
			linkCost = new int[numRouters];
			linkCost = dvr.mincost;
			
			minCost = new int[numRouters][numRouters];
			minCost[routerId] = linkCost.clone();
			
			nextHop = new int[numRouters];
			
			//initializing next hop routers (will only be neighbors at this point)
			for(int i = 0 ; i < numRouters; i++) {
				if(minCost[routerId][i] != 999) {
					nextHop[i] = i;
				}
				else {
					nextHop[i] = -1;
				}
				
			}
			
		}
		else {
			
			System.out.println("Received new mincost vector from " + senderId);
			minCost[senderId] = dvr.mincost;
			
			boolean localMinCostChanged = false;
			
			for(int i = 0; i < numRouters; i++) {
				
				//Check if i is self 
				if(i == routerId) {
					continue;
				}
				

				if(minCost[routerId][i] > linkCost[senderId] + minCost[senderId][i]) {
					minCost[routerId][i] = linkCost[senderId] + minCost[senderId][i];
					nextHop[i] = senderId;
					localMinCostChanged = true;
				}
					
			}
			
			if(localMinCostChanged) {
				
				for(int i = 0; i < numRouters; i++) {
					
					//dpnt send dvrpackets to self or non-neighbors
					if(i == routerId || linkCost[i] == DvrPacket.INFINITY){
						System.out.println("This is router " + routerId + " skipping send to non-neighbor " + i);
						continue;
					}
					
					//send to neighbors only
					DvrPacket sendPacket = new DvrPacket(routerId, i, DvrPacket.ROUTE, minCost[routerId]);
					try {
						out.writeObject(sendPacket);
						out.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}

			
				}
				System.out.println("Resetting timer due to change of local minCost vector");
				future.cancel(true);
				future = sExecService.scheduleAtFixedRate(new TimeOutHandler(this), (long)updateInterval, (long)updateInterval, TimeUnit.MILLISECONDS);
			}
		
		}
	}
	
	synchronized void processTimeOut() {
		System.out.println("Processing Timeout exp");
		
		for(int i = 0; i < numRouters; i++) {
			
			//dpnt send dvrpackets to self or non-neighbors
			if(i == routerId || linkCost[i] == DvrPacket.INFINITY)
				continue;
			
			
			System.out.println("Sending to " + i + " whose link cost is " + linkCost[i]);
			
			//send to neighbors only
			DvrPacket sendPacket = new DvrPacket(routerId, i, DvrPacket.ROUTE, minCost[routerId]);
			try {
				out.writeObject(sendPacket);
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	
		}
			
	}
	

	void printArray(int[] array){
		for (int i = 0; i < array.length; i++) {
			System.out.println("Index " + i + " holds " + array[i]);
		}
		
		
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
		
		System.out.println("Classpath");
		System.getProperty("java.classpath");
		
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
