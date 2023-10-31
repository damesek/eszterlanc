package rfsa;

import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Map;

public class Dumper extends Thread {
	protected static Runtime rt = Runtime.getRuntime();

	protected boolean stopped;

	public Dumper() {
		setDaemon(true);
	}

	public static void gc() {
		rt.gc();
		rt.runFinalization();
	}

	public void stopp() {
		stopped = true;
		interrupt();
	}

	public void run() {
		while (true) {
			try {
				LineNumberReader reader = new LineNumberReader(new InputStreamReader(
				    System.in));
				while (!stopped && (reader.readLine()) != null) {
					print();
					gc();
					printMem();
				}

				if (stopped) {
					System.out.println(getClass().getSimpleName() + " stopped");
					return;
				}
			} catch (Throwable t) {
				System.err.println("Dumper terminates:");
				t.printStackTrace();
				return;
			}
		}
	}

	public static String stackDump() {
		StringBuffer sb = new StringBuffer();
		Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
		for (Map.Entry<Thread, StackTraceElement[]> entry : map.entrySet()) {
			Thread thread = entry.getKey();
			StackTraceElement[] dump = entry.getValue();

			System.out.println(thread);
			for (StackTraceElement trace : dump) {
				sb.append(" " + trace + "\n");
			}
		}
		return sb.toString();
	}

	public static String memDump() {
		return memDump("");
	}

	public static String memDump(String head) {
		MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
		MemoryUsage heapUsage = memBean.getHeapMemoryUsage();
		MemoryUsage nonHeapUsage = memBean.getNonHeapMemoryUsage();
		return head + "heap: " + heapUsage + "\n" + head + "non heap : "
		    + nonHeapUsage;
	}

	public static void print() {
		System.out.print(stackDump());
		printMem();
	}

	public static void printMem() {
		System.out.println(memDump());
	}
}
