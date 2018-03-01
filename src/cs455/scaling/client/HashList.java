package cs455.scaling.client;

import java.util.LinkedList;
import java.util.List;

public class HashList {
	private final List<String> hashlist = new LinkedList<String>();
	
	public void add (String s) {
		synchronized (hashlist) {
			hashlist.add(s);
		}
	}
	
	/**
	 * @param s the hashcode to remove
	 * @return false if string not present
	 */
	public boolean removeIfPresent(String s) {
		synchronized (hashlist) {
			int index = hashlist.indexOf(s);
			if (index == -1) {
				return false;
			} else {
				hashlist.remove(index);
				return true;
			}
		}
	}
	
	public boolean contains(String s) {
		synchronized (hashlist) {
			return hashlist.contains(s);
		}
	}
	
	public void printList(String k) {
		synchronized (hashlist) {
			for (String s : hashlist) {
				System.out.println(s);
//				System.out.println(s + " " + k + "\t Equals?: " + s.equals(k));
//				System.out.println(s.length());
//				System.out.println(Arrays.toString(s.getBytes()));
//				System.out.println(k.length());
//				System.out.println(Arrays.toString(k.getBytes()));
			}
		}
	}
}
