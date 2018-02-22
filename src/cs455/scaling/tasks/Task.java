package cs455.scaling.tasks;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Task implements Runnable {
	protected AtomicBoolean finished = new AtomicBoolean(false);
	// Returns true if this task has completed
	public boolean isFinished() {
		return finished.get();
	}
	
	// Marks the task as finished
	public void setFinished() {
		synchronized (finished) {
			finished.set(true);
			finished.notify();
		}
	}
	
	// Waits for the task to finish
	public void waitOnTaskFinished() {
		synchronized (finished) {
			try {
				// Don't wait on the task if it is already done
				if (isFinished()) {
					return;
				}
				finished.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
