package cs455.scaling.server;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
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
	private final AtomicBoolean isReading;
	private final AtomicBoolean isDead;
	private String clientIP;
	private final AtomicInteger throughput = new AtomicInteger(0);
	// Stat variables
	public ClientConnection(SocketChannel socket) {
		this.socket = socket;
		this.isReading = new AtomicBoolean(false);
		this.isDead = new AtomicBoolean(false);
		this.myID = idCount.getAndIncrement(); // id++
		try {
			this.clientIP = socket.getRemoteAddress().toString();
		} catch (IOException e) {
			e.printStackTrace();
			this.clientIP = "";
		}
	}

	public SocketChannel getSocket() {
		return socket;
	}
	
	public boolean isReading() {
		return isReading.get();
	}
	
	public boolean isDead() {
		return isDead.get();
	}
	
	public void setIsDead(boolean a) {
		isDead.set(a);
	}
	
	public void setIsReading(boolean a) {
		isReading.set(a);
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
	
}
