package cs455.scaling.tasks;

public class TestTask extends Task {
	private int taskNum;
	
	public TestTask(int i) {
		taskNum = i;
	}
	
	@Override
	public void run() {
		System.out.println("Hello World, I am test task number: " + taskNum + "!");
	}
}
