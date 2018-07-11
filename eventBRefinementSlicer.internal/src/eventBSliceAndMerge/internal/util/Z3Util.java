package eventBSliceAndMerge.internal.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;

public class Z3Util {
	private static String z3Path = "/usr/local/bin/z3";

	/**
	 * Runs Z3 with the converted PO as the input
	 * @return The output from Z3
	 * @throws IOException
	 */
	public static String runZ3(String inputFilePath) throws IOException {
		String result = "";
		Process pr = Runtime.getRuntime().exec(z3Path + " -smt2 " + inputFilePath);
		BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		String line;
		while ((line = in.readLine()) != null) {
			result = result + "\n" + line;
		}
		in.close();
		return result;
	}
	
	public static boolean isSAT(String z3Output) throws IOException {
		BufferedReader in = new BufferedReader(new StringReader(z3Output));
		String line;
		while ((line = in.readLine()) != null) {
			if (line.equals("sat")) {
				return true;
			}
		}
		return false;
	}

	public static boolean isUNSAT(String z3Output) throws IOException {
		BufferedReader in = new BufferedReader(new StringReader(z3Output));
		String line;
		while ((line = in.readLine()) != null) {
			if (line.equals("unsat")) {
				return true;
			}
		}
		return false;
	}
}
