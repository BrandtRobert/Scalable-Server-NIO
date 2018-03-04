package cs455.scaling.tasks;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import cs455.scaling.server.ClientConnection;

public class HashMessage extends Task {
	private final SelectionKey clientKey;
	public HashMessage (SelectionKey client) {
		this.clientKey = client;
	}

	/**
	 * Algorithm from assignment page:
	 * https://www.cs.colostate.edu/~cs455/CS455-Spring18-HW2-PC.pdf
	 */
	private byte[] SHA1FromBytes(byte[] data) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA1");
			return digest.digest(data);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}

	}
	
	@Override
	public void run() {
		ClientConnection client = (ClientConnection) clientKey.attachment();
		byte [] msg = client.getNextClientMessage().array();
		byte[] hashcode = SHA1FromBytes(msg);
		if (hashcode != null && clientKey.isValid()) {
			client.addToHashList(ByteBuffer.wrap(hashcode));
			clientKey.interestOps(SelectionKey.OP_WRITE);
		}
	}

}
