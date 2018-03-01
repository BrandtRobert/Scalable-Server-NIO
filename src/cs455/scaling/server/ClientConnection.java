package cs455.scaling.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds data pertinent for the server to know about a connected client.
 * Also handles statistical information for the client
 * @author brandt
 */
public class ClientConnection {
	private static final AtomicInteger idCount = new AtomicInteger(0);
	private final int myID;
	private final SocketChannel socket;
	private String clientIP;
	private final AtomicInteger throughput = new AtomicInteger(0);
	// Receive 8KB messages, and send 40 byte hashcodes back
	private final List<ByteBuffer> readList;
	private final List<ByteBuffer> hashList;
	
	public int keyReadCount = 0;
	// Stat variables
	public ClientConnection(SocketChannel socket) {
		this.socket = socket;
		this.myID = idCount.getAndIncrement(); // id++
		try {
			this.clientIP = socket.getRemoteAddress().toString();
		} catch (IOException e) {
			e.printStackTrace();
			this.clientIP = "";
		}
		hashList = new LinkedList<ByteBuffer>();
		readList = new LinkedList<ByteBuffer>();
	}

	public SocketChannel getSocket() {
		return socket;
	}
	
	public String getClientIP() {
		return clientIP;
	}
	
	public int getID() {
		return myID;
	}
	
	public int incrementThroughput() {
		return throughput.getAndIncrement();
	}
	
	public int getAndResetThroughput() {
		return throughput.getAndSet(0);
	}
	
	@Override
	public boolean equals(Object other) {
		if (other.getClass() != this.getClass()) {
			return false;
		}
		ClientConnection otherConn = (ClientConnection) other;
		return this.getID() == otherConn.getID();
	}

	/**
	 * @return the hashList
	 */
	public List<ByteBuffer> getHashList() {
		List<ByteBuffer> toReturn = new LinkedList<ByteBuffer>();
		synchronized (hashList) {
			while (!hashList.isEmpty()) {
				toReturn.add(hashList.remove(0));
			}
		}
		return toReturn;
	}

	public void addToHashList(ByteBuffer msg) {
		synchronized (hashList) {
			hashList.add(msg);
		}
	}
	
	public void addNewClientMessage(ByteBuffer msg) {
		synchronized (readList) {
			readList.add(msg);
		}
	}
	
	public ByteBuffer getNextClientMessage() {
		synchronized (readList) {
			return readList.remove(0);
		}
	}

}
