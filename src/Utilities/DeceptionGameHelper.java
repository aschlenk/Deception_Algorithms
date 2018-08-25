package Utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class DeceptionGameHelper {
	
	public static boolean loadLibrariesCplex(String ConfigFile) throws IOException{
		FileReader fstream = new FileReader(ConfigFile);
		BufferedReader in = new BufferedReader(fstream);

		String CplexFileString = null;

		String line = in.readLine();
		
		while(line != null){
			line.trim();
			
			if(line.length() > 0 && !line.startsWith("#")){
				String[] list = line.split("=");
				
				if(list.length != 2){
					throw new RuntimeException("Unrecognized format for the config file.\n");
				}
				
				if(list[0].trim().equalsIgnoreCase("LIB_FILE")){
					CplexFileString = list[1];
				}
				else{
					System.err.println("Unrecognized statement in Config File: " + line);
					return false;
				}
			}
			
			line = in.readLine();
		}

		File CplexFile = new File(CplexFileString);
		
		System.load(CplexFile.getAbsolutePath());
		
		return true;
	}
	
	
}
