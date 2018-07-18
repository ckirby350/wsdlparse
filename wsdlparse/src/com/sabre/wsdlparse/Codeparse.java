package com.sabre.wsdlparse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;
import java.util.stream.Stream;

/******
 * Used to parse directory of source files for TPF and check for MVC lines
 * so we can link programs to data and data artifacts to each other
 *  *
 */
public class Codeparse {

	/*****
	 * Only parameter is full path to tpf source file root folder
	 * @param args
	 */
	public static void main(String[] args) {
		final String dosCommand = "cmd /c dir /s";		
		if (args == null || args.length < 1) {
			System.out.println("Must provide full path to source folder. Ex. C:\\docs\\tpf\\tpfsource\\pss");
		}
		try {
			final Process runProc = Runtime.getRuntime().exec(dosCommand + " " + args[0] + "\\*.asm");
			BufferedReader reader = new BufferedReader(new InputStreamReader(runProc.getInputStream())); 
			ArrayList<String> fileList = new ArrayList<String>();
			int spaceSpot = -1;
			String line = "";
			String fileStr = "";
			String folderPrefix = "";
			int lineCnt = 0;
			while ((line = reader.readLine()) != null) { 
				if (line == null || line.trim().length() <6) {
					continue;
				}
				lineCnt++;
				spaceSpot = line.lastIndexOf(" ");
				//System.out.println("line1: " + line);
				if (spaceSpot > -1) {
					fileStr = line.substring(spaceSpot + 1, line.length());					
					if (line.indexOf(" Directory of ") == 0) {
						folderPrefix = fileStr;
					} else if (fileStr.indexOf(".asm") > -1) {
						fileList.add(folderPrefix + "\\" + fileStr);
					}
				}
			} 
			for (String fileName : fileList) {
				System.out.println("Processing " + fileName);
				try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
					while ((line = br.readLine()) != null) {
						processLine(line);
					}
				/*** with streams got MalformedInputException on some files (hah4.asm)
				try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
					System.out.println("    stream null=" + (stream == null));
					stream.forEach(fileLine -> processLine(fileLine));
					//stream.forEach(System.out::println);
				***/
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println("--------------------------------------------------------");
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void processLine(String fileLine) {
		String sourceField = "";
		String targetField = "";
		String nums = "=0123456789";
		boolean ignoreThese = false;
		int commaSpot = -1;
		int spaceSpot = -1;
		int mvcSpot = -1;
		int leftParenSpot = -1;
		int rightParenSpot = -1;
		if (fileLine == null || fileLine.trim().length() < 3) {
			return;
		}
		commaSpot = fileLine.indexOf(",");
		mvcSpot = fileLine.indexOf(" MVC ");
		//This if checks to make sure not a comment line (*) and is an MVC command
		if (fileLine.indexOf("*") != 0 && mvcSpot > -1 && commaSpot > mvcSpot) {
			//System.out.println("   line: " + fileLine);
			try {
				rightParenSpot = fileLine.indexOf(")");
				leftParenSpot = fileLine.indexOf("(");
				if (leftParenSpot > mvcSpot && leftParenSpot < commaSpot && rightParenSpot > commaSpot) {
					commaSpot = fileLine.indexOf(",", commaSpot + 1);
				}
				spaceSpot = fileLine.lastIndexOf(" ", commaSpot);
				targetField = fileLine.substring(spaceSpot + 1, commaSpot);
				spaceSpot = fileLine.indexOf(" ", commaSpot);
				if (spaceSpot < commaSpot) {
					spaceSpot = fileLine.length();
				}
				sourceField = fileLine.substring(commaSpot + 1, spaceSpot).trim();
				if (nums.indexOf(sourceField.substring(0,1)) > -1 || nums.indexOf(targetField.substring(0,1)) > -1) {
					//if first char is a number, then base displacement so ignore
					//or equals sign, then literals
					ignoreThese = true;
				}
				if (!ignoreThese) {
					System.out.println("      target: " + targetField + " source: " + sourceField);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	 

}
