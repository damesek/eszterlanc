package rfsa;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collection;

public class RFSAAnalyser {
	public void online(RFSA rfsa) throws Exception {
		BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
		System.out.println(">");
		String line;
		while ((line = r.readLine()) != null) {
			Collection<String> a = rfsa.analyse(line);
			System.out.println(a.size() + ": ");
			for (String s : a) {
				System.out.println("  " + s);
			}
			System.out.println(">");
		}
	}

}
