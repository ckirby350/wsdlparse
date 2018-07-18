package com.sabre.wsdlparse;
import java.io.*;
import java.util.ArrayList;
import java.util.Properties;

/******
 * Used to parse directory of WSDL files for SWS checked out from SVN
 * and produce latest WSDL results
 * 
 *
 */
public class Folderparse {

	/*****
	 * No args necessary to run
	 * @param args
	 */
	public static void main(String[] args) {
		final String dosCommand = "cmd /c dir /s";		
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream("c:\\apps\\wsdlparse.properties");
			// load a properties file
			prop.load(input);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		//System.out.println("cmd=" + dosCommand + " " + prop.getProperty("wsdlfolder") + "\\*.wsdl");
		try {
			final Process runProc = Runtime.getRuntime().exec(dosCommand + " " + prop.getProperty("wsdlfolder") + "\\*.wsdl");
			BufferedReader reader = new BufferedReader(new InputStreamReader(runProc.getInputStream())); 
			ArrayList<String> fileList = new ArrayList<String>();
			ArrayList<String> folderList = new ArrayList<String>();
			ArrayList<String> latestWSDLFileList = new ArrayList<String>();
			String line = "";
			String folderStr = "";
			String fileStr = "";
			String[] lineParts = null;
			int lineCnt = 0;
			while ((line = reader.readLine()) != null) { 
				if (line == null || line.length() <6) {
					continue;
				}
				lineCnt++;
				//System.out.println(line);
				if (line.indexOf(" Directory of ") == 0) {
					folderStr = line.substring(14, line.length());	
					if (folderList != null && folderList.size() > 0) {
						latestWSDLFileList.addAll(getLatestFilesFromFolderList(folderList));
						/*******debug only *********************
						for (String xstr : folderList) {
							System.out.println(xstr);
						}
						System.out.println("LATEST");
						for (String xstr : getLatestFilesFromFolderList(folderList)) {
							System.out.println("    " + xstr);
						}
						System.out.println("-------------------------------");
						}
						*****************************************/
					}
					folderList = new ArrayList<String>();
				}
				if (line.indexOf(".wsdl") > -1) {
					lineParts = line.split(" ");
					if (lineParts != null && lineParts.length > 0) {
						fileStr = lineParts[lineParts.length - 1];
						fileList.add(folderStr + "\\" + fileStr);
						folderList.add(folderStr + "\\" + fileStr);
					}
				}	
				if ((lineCnt % 100) == 0) {
					System.out.println("..." + lineCnt);
				}
			} 
			
			String eofStr = "\n";
			FileWriter writer = null;
			try {
				writer = new FileWriter("c:\\apps\\wsdllist.txt");
				for (String latestFileStr : latestWSDLFileList) {
					writer.write(latestFileStr + eofStr);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (writer != null) {
					writer.close();
				}
			}
			
			/*** saving in case we need to interrogate char by char instead of line by line
	         int ch;
	         while((ch = in.read()) != -1) {
	            System.out.print((char)ch);
	         }
			 ***/
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/******
	 * At this point in the process, should have a list of all full path wsdl file names
	 * for the given folder, so now we want to get the latest version so we're always
	 * pointing to the latest wsdl for the given service
	 * @param folderList
	 * @return
	 */
	public static ArrayList<String> getLatestFilesFromFolderList(ArrayList<String> folderList) {
		ArrayList<String> resultList = new ArrayList<String>();
		String firstDigit = "";		
		int firstDigitSpot = -1;
		int slashSpot = -1;
		String prefix = "";
		if (folderList == null || folderList.size() < 1) {
			return resultList;
		}
		for (String fileStr : folderList) {
			firstDigit = getLineDigit(fileStr, 0);
			if (firstDigit != null && firstDigit.trim().length() > 0) {
				firstDigitSpot = fileStr.indexOf(firstDigit);
				/****
				 * slashSpot is to handle when versions are subfolders instead of just file name suffixes
				 * Ex. c:\data\workspace\wsdl\tripservices\1.1.0\GetReservation.wsdl
				 * instead of the c:\data\workspace\wsdl\tripservices\GetReservation1.1.0.wsdl format we're expecting
				 * So matching full filepath prefix wouldn't work down line
				 * Leaving the \ infront to avoid situations where service names with the same "sub name" would match
				 * Ex. c:\data\workspace\wsdl\tripservices\1.1.0\Reaccom_Trip_SearchRQ.wsdl
				 *     c:\data\workspace\wsdl\tripservices\1.1.0\Trip_SearchRQ.wsdl
				 */
				slashSpot = fileStr.indexOf("\\", firstDigitSpot);
				if (slashSpot > -1) {
					prefix = fileStr.substring(slashSpot, fileStr.indexOf(".wsdl"));
				} else {					
					/***
					 * This check for _ is to handle where there are files with no version and then
					 * another same service wsdl with _# version to get the prefix correct
					 * c:\data\workspace\wsdl\Rebook\Rebook.wsdl
					 * c:\data\workspace\wsdl\Rebook\Rebook_0.0.4.wsdl
					 */
					if (fileStr.substring(firstDigitSpot -1, firstDigitSpot).equals("_")) {
						firstDigitSpot--;
					} else {
						/***
						 * Similar to _ check, looking for _V or _v suffix to the service name
						 * to get the prefix check correct
						 */
						if (fileStr.substring(firstDigitSpot -2, firstDigitSpot).equals("_V") ||
								fileStr.substring(firstDigitSpot -2, firstDigitSpot).equals("_v")) {
							firstDigitSpot = firstDigitSpot - 2;
						} else {
							/***
							 * Similar to _V check, looking for _V. or _v. suffix to the service name
							 * to get the prefix check correct
							 */
							if (fileStr.substring(firstDigitSpot -3, firstDigitSpot).equals("_V.") ||
									fileStr.substring(firstDigitSpot -3, firstDigitSpot).equals("_v.")) {
								firstDigitSpot = firstDigitSpot - 3;
							}
						}
					}
					prefix = fileStr.substring(0, firstDigitSpot);
				}
			} else {
				firstDigitSpot = fileStr.indexOf(".wsdl");
				prefix = fileStr.substring(0, firstDigitSpot);
			}			
			if (!isPrefixAlreadInFileList(resultList, prefix)) {
				resultList.add(getLatestFile(folderList, prefix));
			}
		}
		return resultList;
	}
	
	/*****
	 * Go through ArrayList and see if any line matches the prefix string provided
	 * @param folderList
	 * @return
	 */
	public static boolean isPrefixAlreadInFileList(ArrayList<String> resultList, String prefix) {
		for (String fileStr : resultList) {
			if (fileStr.indexOf(prefix) > -1) {
				return true;
			}
		}
		return false;
	}
	
	/******
	 * Returns the first digit it finds in the provided String from the startPoint index provided
	 * @param lineStr
	 * @param startPoint
	 * @return
	 */
	public static String getLineDigit(String lineStr, int startPoint) {		
		for (int i = startPoint; i < lineStr.length(); i++) {
            if (Character.isDigit(lineStr.charAt(i))) {
                return String.valueOf(lineStr.charAt(i));
            }
        }
		return "";
	}
	
	/****
	 * Goes through the List of strings (full path file string) and tries to assess
	 * version (Ex v1.0.1, 1-0-1, 1.2, 6_12, etc.) and return the latest version of 
	 * the filename (prefix) provided
	 * @param folderList
	 * @param prefix
	 * @return
	 */
	public static String getLatestFile(ArrayList<String> folderList, String prefix) {
		int arrCnt = 0;
		int lastPos = 0;
		String numStr = "";
		int[] verArray = new int[4];
		ArrayList<String> fileList = new ArrayList<String>();
		ArrayList<int[]> versionList = new ArrayList<int[]>();
		for (String lineStr : folderList) {			
			if (lineStr != null && lineStr.indexOf(prefix) > -1) {
				arrCnt = -1;
				verArray = new int[4];
				lastPos = 0;
				numStr = getLineDigit(lineStr, lastPos);
				while (numStr != null && numStr.length() > 0 && arrCnt < 3) {
//System.out.println("numStr=" + numStr + " arrCnt=" + arrCnt);
					arrCnt++;
					verArray[arrCnt] = Integer.parseInt(numStr);
					lastPos = lineStr.indexOf(numStr) + 1;
					numStr = getLineDigit(lineStr, lastPos);
				}
				versionList.add(verArray);
				fileList.add(lineStr);
			}
		}
		return fileList.get(getLatestVersionIndex(versionList));
	}

	/*****
	 * Takes the list of version arrays (Ex. [1,0,1,0],[1,0,2,0], etc.) and returns
	 * the List index of the latest/greatest version found
	 * @param versionList
	 * @return
	 */
	public static int getLatestVersionIndex(ArrayList<int[]> versionList) {
		int[] maxArr = null;
		for (int[] verArr : versionList) {
			if (maxArr == null) {
				maxArr = verArr;
				continue;
			}
			if (verArr[0] > maxArr[0]
					|| (verArr[0] == maxArr[0] && verArr[1] > maxArr[1]) 
					|| (verArr[0] == maxArr[0] && verArr[1] == maxArr[1] && verArr[2] > maxArr[2]) 
					|| (verArr[0] == maxArr[0] && verArr[1] == maxArr[1] && verArr[2] == maxArr[2] && verArr[3] > maxArr[3])
					) {
				maxArr = verArr;
			}
		}
		if (maxArr != null) {
			return versionList.indexOf(maxArr);
		}
		return 0;
	}

}
