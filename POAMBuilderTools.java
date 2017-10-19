package com.bangblue;

public class POAMBuilderTools {
	/***
	    * returnCAT
	    * @param cat
	    * @return  I  or   II   or   I I  I
	    * Takes  a  Critical/High/Medium/Low 4/3/2/1  and translates  it  to the I's  II's and  III ' s
	    */
	static String returnCAT(String cat){ 
	    	
    	if (cat.equalsIgnoreCase("high") || cat.equalsIgnoreCase("critical") ){
    		return "I";
    	} else if (cat.equalsIgnoreCase("medium")){
    		return "II";
    	} else if (cat.equalsIgnoreCase("low")){		
    		return "III";
    	} else if (cat.equalsIgnoreCase("4") || cat.equalsIgnoreCase("3") ){ 
    		return "I";	
    	} else if (cat.equalsIgnoreCase("2")){
    		return "II";
    	} else if (cat.equalsIgnoreCase("1")){		
    		return "III";
    	} else {
    		return "CAT NOT FOUND";
    	}
	}//end retrunCAT
	
    /**
     * shortSTIGName
     * @param stig
     * @return String parse stig name
     * 
     * STIG names can be unnecessarly long, this shortens them to a more managable length
     */
    static String shortenSTIGName(String stig) {
    	if(stig.contains("(STIG)")){
    		stig = stig.replace("(STIG)", "");
    	}
    	if(stig.contains("Security Technical Implementation Guide")){
    		stig = stig.replace("Security Technical Implementation Guide", "STIG");
    	}
    	if(stig.contains("Member Server")){
    		stig = stig.replace("Member Server", "MS");
    	}
    	if(stig.contains("Domain Controller")){
    		stig = stig.replace("Domain Controller", "DC");
    	}
    	if(stig.contains("Checklist")){
    		stig = stig.replace("Checklist", "");
    	}
    	if(stig.contains("Internet Explorer")){
    		stig = stig.replace("Internet Explorer", "IE");
    	}
    	
		return stig.trim();
	}//private String shortenSTIGName(String stig) {
    
	/**
     * stringContainsItemFromList (String inputString, String[] firstLine)
     * @info This is used to see if file is a retina or a scap file by testing the first line column headers
     * 
     * @var inputString is your Retina or SCAP header
     * @var firstLine is the first line of the CSV you want to parse
     * 
     * @note
     * retina headers include
	 * NetBIOSName,	DNSName,IP,	MAC,OS,	AuditID,SevCode,CVE,IAV,Name,Description,Date,Risk,CVSSScore,PCILevel,FixInformation,CCE,NetBIOSDomain,Exploit,Context,CPE,PCIPassFail,PCIReason,
     * */ 
    static boolean stringContainsItemFromList(String inputString, String[] firstLine){
        for(int i =0; i < firstLine.length; i++){
            if(inputString.equalsIgnoreCase(firstLine[i])){
                return true;
            }
        }
        return false;
    }//stringContainsItemFromList
    
	/**
	* This returns the number of times a string is found 15	
	* @param str - full string
	* @param findStr - squence to find
	* @return int - count of number of times found
	*/
    static int charSqCnt(String str, String findStr){ 
		int lastlndex = 0;
		int cnt = 0;

		while(lastlndex != -1){
			lastlndex = str.indexOf(findStr,lastlndex);
			if(lastlndex != -1){
				lastlndex += findStr.length();
				cnt++;
			}
		}
		return cnt;
	}//end charSqCnt
    
	/**
	* returnDate
	* @param  cat
	* @return date
	*
	* Given CAT level, returns date	
	*/
	static String returnDate(String cat){
		if (cat.equalsIgnoreCase("high") || cat.equalsIgnoreCase("critical") || cat.equalsIgnoreCase("I")){
			return POAMBuilder.cat1scheduledcompletiondate;
    	} else if (cat.equalsIgnoreCase("medium") || cat.equalsIgnoreCase("II")){
    		return POAMBuilder.cat2scheduledcompletiondate;
    	} else if (cat.equalsIgnoreCase("low") || cat.equalsIgnoreCase("III")){		
    		return POAMBuilder.cat3scheduledcompletiondate;
    	} else {
    		return POAMBuilder.cat3scheduledcompletiondate;
    	}
	}
    
}
