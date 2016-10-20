package com.bangblue;

import java.io.File;
import java.io.FileReader;

import au.com.bytecode.opencsv.CSVReader;

public class IACMapping {

	public static void main(String[] args) {
		try {
			File file = new File("C:\\Users\\rogerssa\\Desktop\\Retina_AuditID_to_IAC_Mapping.csv");
			CSVReader reader =  new CSVReader(new FileReader(file));
			String[] nextLine;
			
			while ((nextLine = reader.readNext()) != null) {
				if(!nextLine[1].equals("")){
					String iac = nextLine[1];
					if(iac.contains("ECMT-1 / Medium (IAC is unclass only!!!), ")){
						iac = nextLine[1].replace("ECMT-1 / Medium (IAC is unclass only!!!), ", "");
					}
					if(iac.contains("IAIA-1 / High  (IAC is unclass only!!!), ")){
						iac = nextLine[1].replace("IAIA-1 / High  (IAC is unclass only!!!), ", "");
					}
					String output = "iacMap.put(\"" + nextLine[0] + "\", \"" + iac + "\"";
					output = output.replace(",, ", ",");
					output = output.replace(";", ",");
					System.out.println(output + ");");
				}
			}
			
			reader.close();
		} catch (Exception e){
			System.out.println(e);
		}
	}
}
