package cs455.scaling.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class StatisticsCollectorAndDisplay {
	List<Double> values = new ArrayList<Double>();
	
	public void acceptNewIntegerValues (List<Integer> values) {
		for (Integer i : values) {
			this.values.add(i.doubleValue());
		}
	}
	
	public void acceptNewDoubleValues (List<Double> values) {
		this.values = values;
	}
	
	public void displayStatistics() {
		double sum = getSumFromValues();
		double mean = getMeanFromValues(sum);
		double stddev = getStdDevFromValues(mean);
		int count = values.size();
		Date timestamp = new Date();
		String dateStr = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss").format(timestamp);
		System.out.printf("[%s] Server Throughput: %.0f messages, Active Client Connections: %d,"
				+ " Mean Throughput: %.2f messages, StdDev. Throughput %.2f messages\n",
						dateStr, sum, count, mean, stddev);
	}

	private double getSumFromValues() {
		double sum = 0;
		for (Double i : values) {
			sum += i;
		}
		return sum;
	}
	
	private double getMeanFromValues(double sum) {
		return (sum != 0) ? sum / (double) values.size() : 0;
	}

	// https://www.khanacademy.org/math/probability/data-distributions-a1/
	// 		summarizing-spread-distributions/a/calculating-standard-deviation-step-by-step
	private double getStdDevFromValues(double mean) {
		double variance = 0;
		for (Double i : values) {
			// v = [sum (|x - u|**2) / N]
			variance += (mean - i) * (mean - i);
		}
		if (variance == 0) {
			return 0;
		}
		variance /= values.size();
		return Math.sqrt(variance);
	}

}
