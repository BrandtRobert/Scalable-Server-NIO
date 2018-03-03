package cs455.scaling.tasks;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import cs455.scaling.server.ClientConnection;

public class ReadMessageAndRespond extends Task {
	private final int KB = 1000;
	private ClientConnection client;
	private SelectionKey key;

	public ReadMessageAndRespond(SelectionKey key) {
		this.client = (ClientConnection) key.attachment();
		this.key = key;
	}
	
	private ByteBuffer readMessage() throws IOException {
		int read = 0;
		// Specific to the assignment: 
		// The client sends a byte[] to the server. The size of this array is 8 KB
		ByteBuffer buffer = ByteBuffer.allocate(8 * KB);
		while (buffer.hasRemaining() && read != -1) {
			read = client.getSocket().read(buffer);
		}
		return buffer;
	}
	
	/**
	 * Algorithm from assignment page:
	 * https://www.cs.colostate.edu/~cs455/CS455-Spring18-HW2-PC.pdf
	 */
	private byte[] SHA1FromBytes(byte[] data) throws NoSuchAlgorithmException {
		 MessageDigest digest = MessageDigest.getInstance("SHA1");
		 return digest.digest(data);
	}

	@Override
	public void run() {
		if (client.isDead()) {
			return;
		}
		try {
			ByteBuffer msg = readMessage();
			ByteBuffer response = ByteBuffer.wrap(SHA1FromBytes(msg.array()));
			client.getSocket().write(response);
			client.incrementThroughput();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Failed to operate on socket: " + e.getMessage());
			System.err.println("Dropping client: " + client.getClientIP());
			client.setIsDead(true);
			key.cancel();
		} finally {
			client.setIsReading(false);
		}
	}
}
