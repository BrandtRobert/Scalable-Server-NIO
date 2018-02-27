package cs455.scaling.client;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Client {
	private final int KB = 1000;
	private final Random random = new Random();
	private final HashList hashlist = new HashList();
	private AtomicInteger sentCount = new AtomicInteger(0);
	private AtomicInteger receivedCount = new AtomicInteger(0);
	
	private final String host;
	private final int port;
	private final int sendRate;
	private final boolean verbose;
	
	private int posion = 0;
	
	public Client (String ip, int port, int sendRate, boolean verbose) {
		this.host = ip;
		this.port = port;
		this.sendRate = sendRate;
		this.verbose = verbose;
	}
	
	/**
	 * Connect to the server and return the connection channel
	 */
	private SocketChannel connectToServer (String ip, int port) {
		SocketChannel hostConnection = null;
		try {
			SocketAddress hostAddress = new InetSocketAddress(InetAddress.getByName(ip), port);
			hostConnection = SocketChannel.open(hostAddress);
			System.out.println("Successfully connected to server");
		} catch (Exception e) {
			e.printStackTrace();
		}			
		return hostConnection;
	}
	
	/**
	 * Generates a buffer of random bytes 8 kb in size
	 * @return
	 */
	private ByteBuffer getRandomBytes() {
		byte [] randomBytes = new byte [KB * 8];
		random.nextBytes(randomBytes);
		return ByteBuffer.wrap(randomBytes);
	}
	
	/**
	 * Algorithm from assignment page:
	 * https://www.cs.colostate.edu/~cs455/CS455-Spring18-HW2-PC.pdf
	 */
	private String SHA1FromBytes(byte[] data) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		 byte[] hash = digest.digest(data);
		 BigInteger hashInt = new BigInteger(1, hash);
		 String str = hashInt.toString(16);
		 while (str.length() < 40) {
			 str += "\0";
		 }
		 return str;
	}
	
	public void displayStatsAndReset() {
		Date now = new Date();
		String dateStr = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss").format(now);
		System.out.printf("[%s] Total Sent Count: %d, Total Received Count: %d\n", dateStr,
				sentCount.getAndSet(0), receivedCount.getAndSet(0));
	}
	
	private void startDiagnosticThread() {
		Thread diagnosticThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						int seconds = 20;
						Thread.sleep(1000 * seconds);
						displayStatsAndReset();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		diagnosticThread.start();
	}
	
	public void run () throws IOException {
		// Connect to the server
		Selector selector = Selector.open();
		SocketChannel server = connectToServer(host, port);
		server.configureBlocking(false);
		server.register(selector, SelectionKey.OP_READ);
		SenderThread senderThread = new SenderThread(server, sendRate);
		if (verbose) {
			startDiagnosticThread();
		}
		senderThread.start();
		// Receive messages from the server
		while (true) {
			selector.select();
			Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();
            ByteBuffer buffer = ByteBuffer.allocate(40);
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                if (key.isReadable()) {
                	// SHA hash is 40 bytes in size ?
                	((SocketChannel) key.channel()).read(buffer);
                	String receivedHash = new String (buffer.array());
                	if (!hashlist.removeIfPresent(receivedHash)) {
                		System.out.println("Received unrecognizd hash: " + receivedHash);
                		System.out.println("Pending Hashes: ");
                		hashlist.printList(receivedHash);
                		if (posion++ == 5) {
                			System.exit(2);
                		}
                	} else {
                		receivedCount.getAndIncrement();
                	}
                }
                buffer.clear();
                iter.remove();
            }
		}
	}
	
	private class SenderThread extends Thread {
		private final SocketChannel server;
		private final int sendRate;
		
		public SenderThread (SocketChannel server, int sendRate) {
			this.server = server;
			this.sendRate = sendRate;
		}

		@Override
		public void run() {
			while (true) {
				ByteBuffer randomBytes = getRandomBytes();
				String hashCode = SHA1FromBytes(randomBytes.array());
				hashlist.add(hashCode);
				try {
					server.write(randomBytes);
					sentCount.getAndIncrement();
					Thread.sleep(1000 / sendRate);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	private static void usage() {
		System.err.println("java cs455.scaling.client.Client server-host server-port message-rate");
		System.exit(1);
	}
	
	public static void main (String [] args) throws IOException {
		if (args.length != 3) {
			usage();
		}
		// Parse input params
		String hostIPString = args[0];
		int hostPort = Integer.parseInt(args[1]);
		int sendRate = Integer.parseInt(args[2]);
		// Start the client
		Client client = new Client(hostIPString, hostPort, sendRate, true);
		client.run();
	}

	
}
