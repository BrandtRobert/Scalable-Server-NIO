package cs455.scaling.concurrent;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

//import java.util.concurrent.LinkedBlockingQueue;

import cs455.scaling.tasks.Task;
import cs455.scaling.tasks.TestTask;

/**
 * Implementation of thread pool class.
 * Objects which implement the task interface may be offered to the threadpool.
 * The pool will allocate a thread to perform the task when there is a thread available.
 * 
 * Primary reference for this class comes from Ima Miri's blog post: 
 * 		https://www.javacodegeeks.com/2016/12/implement-thread-pool-java.html
 * @author Brandt Reutimann
 */
public class ThreadPool {
	private final int numberThreads;
//	private LinkedBlockingQueue<Task> taskQueue;
	private final List<Task> taskQueue = Collections.synchronizedList(new LinkedList<Task>());
	private final WorkerThread [] workerThreads;
	private boolean debug = false;
	private boolean started = false;

	public ThreadPool (int nThreads) {
		this.numberThreads = nThreads;
		this.workerThreads = new WorkerThread [numberThreads];
		// Create nThread new threads and start them
		for (int i = 0; i < numberThreads; i++) {
			workerThreads[i] = new WorkerThread("WorkerThread-" + i);
		}
	}
	
	/**
	 * Starts the threads in the pool
	 */
	public void initialize () {
		for (int i = 0; i < numberThreads; i++) {
			workerThreads[i].start();
		}
		started = true;
	}
	
	/**
	 * Close thread pool, blocking for all threads to finish their tasks.
	 */
	public void closePoolNow () {
		for (int i = 0; i < numberThreads; i++) {
			try {
				workerThreads[i].killMe();
				workerThreads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Offering a task puts it on the queue and notifies a thread waiting for work.
	 * @param task
	 */
	public void offerTask (Task task) {
		if (!started) {
			System.out.println("Initialize the thread pool before offering new tasks");
			return;
		}
		synchronized (taskQueue) {
			taskQueue.add(task);
			taskQueue.notify();
		}
	}
	
	private class WorkerThread extends Thread {
		private volatile boolean isRunning = true;
		private String threadName;
		
		public WorkerThread (String name) {
			this.threadName = name;
		}
		
		public void run() {
			Task task;
			while (isRunning) {
				synchronized (taskQueue) {
					while (taskQueue.isEmpty()) {
						try {
							taskQueue.wait();		// Will relinquish the lock on the taskQueue
						} catch (InterruptedException e) {
							// I have been interrupted for termination reasons
							if (!isRunning) {
								return;
							}
							System.err.println(this.threadName + " interrupted while waiting for next task.");
						}
					}
					// Pop a task from the queue
					task = taskQueue.remove(0);
					try {
						task.run();			// Perform the task.
						task.setFinished(); // Set the finished flag on the task.
						if (debug) {
							System.out.println("Task performed by thread: " + threadName);
						}
					} catch (RuntimeException e) {
						System.err.printf("%s failed a task, because of:\n%s", threadName, e.getMessage());
						e.printStackTrace();
					}
				}
			}
		}
		
		/**
		 * If this thread is waiting for a task then it will be terminated.
		 */
		public void killMe() {
			this.isRunning = false;
			this.interrupt();
		}
	}
	
	public static void main (String args []) {
		ThreadPool threadpool = new ThreadPool(5);
		threadpool.initialize();
		Task [] tasks = new Task [10];
		threadpool.debug = true;
		for (int i = 0; i < 10; i++) {
			tasks[i] = new TestTask(i);
			threadpool.offerTask(tasks[i]);
		}
		for (int i = 0; i < 10; i++) {
			tasks[i].waitOnTaskFinished();
		}
		threadpool.closePoolNow();
	}
}
