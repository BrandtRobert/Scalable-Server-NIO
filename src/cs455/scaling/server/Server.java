package cs455.scaling.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import cs455.scaling.concurrent.ThreadPool;
import cs455.scaling.tasks.ReadMessageAndRespond;
import cs455.scaling.util.StatisticsCollectorAndDisplay;

public class Server {
	private ThreadPool threadpool;
	private List<ClientConnection> clientCache = new LinkedList<ClientConnection>();
	
	/**
	 * Creates a task for reading the msg, hashing, and responding to the client
	 * @param client - the socket channel to read from
	 */
	private void respondToClient (SelectionKey client) {
		((ClientConnection) client.attachment()).setIsReading(true);	// So the server knows that this read is handled
		ReadMessageAndRespond task = new ReadMessageAndRespond(client);
		threadpool.offerTask(task);
	}
	
	private void registerNewConnection (Selector selector, ServerSocketChannel serversocket) {
		SocketChannel newClient;
		try {
			newClient = serversocket.accept();
			newClient.configureBlocking(false);
			ClientConnection clientconnection = new ClientConnection(newClient);
			synchronized (clientCache) {
				clientCache.add(clientconnection);
			}
			newClient.register(selector, SelectionKey.OP_READ, clientconnection);
		} catch (IOException e) {
			System.err.println("Unable to register new client");
		}
	}
	
	private void collectDiagnosticsAndDisplay() {
		StatisticsCollectorAndDisplay stats = new StatisticsCollectorAndDisplay();
		List<Double> clientData = new ArrayList<Double>();
		List<ClientConnection> cleanUpList = new ArrayList<ClientConnection>(clientCache.size());
		synchronized (clientCache) {
			for (ClientConnection c : clientCache) {
				if (!c.isDead()) {
					clientData.add((double) c.getAndResetThroughput());
				} else {
					// To be cleaned up later
					cleanUpList.add(c);
				}
			}
			// Clean up cancelled clients
			for (ClientConnection i : cleanUpList) {
				clientCache.remove(i);
			}
			System.out.println("Now there are " + clientCache.size() + " clients in the cache");
		}
		stats.acceptNewDoubleValues(clientData);
		stats.displayStatistics();
	}
	
	private void startServer(int portnum) {
		Selector selector;
		ServerSocketChannel serversocket;
		try {
			selector = Selector.open();
			// Create and register serversocket
			serversocket = ServerSocketChannel.open();
			serversocket.bind(new InetSocketAddress(portnum));
			serversocket.configureBlocking(false);
			serversocket.register(selector, SelectionKey.OP_ACCEPT);
			// Print server state
			System.out.println("Server Listening on -> " 
					+ InetAddress.getLocalHost().getHostAddress() + ":" + portnum);
		} catch (IOException e) {
			System.err.println("Failed to start server");
			return;
		}
		
		Thread dianosticsThread = new Thread (new Runnable() {
			@Override
			public void run() {
				while (true) {
					int seconds = 20;
					try {
						Thread.sleep(1000 * seconds);
						collectDiagnosticsAndDisplay();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		dianosticsThread.start();
		
		while (true) {
			Set<SelectionKey> selectedKeys = null;
			Iterator<SelectionKey> iter = null;
			SelectionKey key = null;
			try {
				selector.select();
				selectedKeys = selector.selectedKeys();
	            iter = selectedKeys.iterator();
	            while (iter.hasNext()) {
	            	key = iter.next();
	                if (key.isAcceptable()) {
	                    registerNewConnection(selector, serversocket);
	                }
	                if (key.isReadable()) {
	                	// If write to the client caused an IOException than this client is dead, we want to remove it
	                    // Lock the key from producing a task until the read is finished
	                	ClientConnection client = ((ClientConnection) key.attachment());
	                	if (!client.isReading() && !client.isDead()) {
	                		respondToClient(key);
	                	}
	                }
	                iter.remove();
	            }
			} catch (CancelledKeyException e) {
				System.err.println("Selected key was cancelled due to IOException");
				continue;
			} catch (IOException e) {
				System.err.println("Select operation failed: " + e.getMessage());
				return;
			}
		}
	}
	
	private static void usage() {
		System.out.println("java cs455.scaling.server.Server <portnum> <thread-pool-size>");
		System.exit(1);
	}
	
	public static void main (String args []) {
		if (args.length != 2) {
			usage();
		}
		int portnum = Integer.parseInt(args[0]);
		int threadpoolsize = Integer.parseInt(args[1]);
		Server server = new Server();
		server.threadpool = new ThreadPool(threadpoolsize);
		server.threadpool.initialize();
		server.startServer(portnum);
	}
}
