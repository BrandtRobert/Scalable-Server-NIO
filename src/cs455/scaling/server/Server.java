package cs455.scaling.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
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
import cs455.scaling.tasks.HashMessage;
import cs455.scaling.util.StatisticsCollectorAndDisplay;

// Restructure server to do reading and writing: http://adblogcat.com/asynchronous-java-nio-for-dummies/
// Hashes to be done in tasks

public class Server {
	private ThreadPool threadpool;
	private List<ClientConnection> clientCache = new LinkedList<ClientConnection>();
	private final int KB = 1000;
	
	private void collectDiagnosticsAndDisplay() {
		StatisticsCollectorAndDisplay stats = new StatisticsCollectorAndDisplay();
		List<Double> clientData = new ArrayList<Double>();
		synchronized (clientCache) {
			for (ClientConnection c : clientCache) {
				clientData.add((double) c.getAndResetThroughput());
//				System.out.println("IP: " + c.getClientIP() + " reads: " +  c.keyReadCount);
			}
		}
		stats.acceptNewDoubleValues(clientData);
		stats.displayStatistics();
	}
	
	private void startDiagnosticThread() {
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
	
	private void readAndCreateTask(SelectionKey key) {
		ClientConnection client = (ClientConnection) key.attachment();
		try {
			ByteBuffer newClientMsg = ByteBuffer.allocate(8 * KB);
			int read = 0;
			while (newClientMsg.hasRemaining() && read != -1) {
				read = client.getSocket().read(newClientMsg);
			}
if (newClientMsg.position() < 8 * KB) {
	System.out.println("Buffer contains less than 8KB in read");
}
			client.addNewClientMessage(newClientMsg);
			HashMessage shaTask = new HashMessage(key);
			threadpool.offerTask(shaTask);
		} catch (IOException e) {
			System.err.println("Failed to read message for client: " + e.getMessage());
			clientCache.remove(client);
			key.cancel();
		}
	}
	
	private void writeResponse(SelectionKey key) {
		ClientConnection client = (ClientConnection) key.attachment();
		try {
			List<ByteBuffer> msgsToSend = client.getHashList();
			while (!msgsToSend.isEmpty()) {
				ByteBuffer newMsg = msgsToSend.remove(0);
				while (newMsg.hasRemaining()) {
					client.getSocket().write(newMsg);
				}
				client.incrementThroughput();
			}
			key.interestOps(SelectionKey.OP_READ);
		} catch (IOException e) {
			System.err.println("Failed to write to client: " + e.getMessage());
			clientCache.remove(client);
			key.cancel();
		}
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
		startDiagnosticThread();
		while (true) {
			Set<SelectionKey> selectedKeys = null;
			Iterator<SelectionKey> keys = null;
			SelectionKey key = null;
			try {
				selector.select();
				selectedKeys = selector.selectedKeys();
	            keys = selectedKeys.iterator();
	            while (keys.hasNext()) {
	            	key = keys.next();
	            	keys.remove();
	            	if (!key.isValid()) {
	            		clientCache.remove(key.attachment());
	            	} else if (key.isAcceptable()) {
	                    registerNewConnection(selector, serversocket);
	                } else if (key.isReadable()) {
	                	readAndCreateTask(key);
	                } else if (key.isWritable()) {
	                	writeResponse(key);
	                }
	            }
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
