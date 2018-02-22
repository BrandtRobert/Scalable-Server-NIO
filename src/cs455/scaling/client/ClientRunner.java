package cs455.scaling.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientRunner {
	
	public static void main (String [] args) {
		if (args.length != 4) {
			System.err.println("java ClientRunner <num-clients> <server-ip> <server-port> <send-rate>");
			System.exit(1);
		}
		// Parse Cmd line
		int numClients = Integer.parseInt(args[0]);
		String ip = args[1];
		int port = Integer.parseInt(args[2]);
		int sendRate = Integer.parseInt(args[3]);
		List<Client> clients = new ArrayList<Client>();
		for (int i = 0; i < numClients; i++) {
			Client client = new Client(ip, port, sendRate, false);
			clients.add(client);
			new Thread (new Runnable() {
				@Override
				public void run() {
					try {
						client.run();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
		while (true) {
			int i = 1;
			for (Client c : clients) {
				System.out.print("Client: " + i++ + " ");
				c.displayStatsAndReset();
			}
			int seconds = 20;
			try {
				Thread.sleep(1000 * seconds);
			} catch (InterruptedException e) {
				System.exit(0);
			}
		}
	}
}
