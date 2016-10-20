package com.bangblue;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sourceforge.jdatepicker.JDateComponentFactory;
import net.sourceforge.jdatepicker.JDatePicker;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * POAM Builder, for SCAP/Retina CSV file parsing to PMMI POAM format
 * 
 * @author rogerssa
 * @version 0.8 BETA
 * 
 * 
 * @Info
 * 
 */
public class POAMbuilder extends JPanel
			implements ActionListener {

	private static final long serialVersionUID = -425666944415786622L;
	//GUI Vars
	private JButton selectButton;
	private JButton executeButton;
	private JButton clearButton;
	private JFormattedTextField poamDescriptorTextbox;
	private JFormattedTextField retinaVersionTextbox;
	private JFormattedTextField dateOfScanTextbox;
	private JTextArea filesSelected;
	private JFileChooser fileChooser;
	private JDatePicker datePicker;
	//UNDOC private JComboBox<String> comboBox;
	private JScrollPane filesSelectedScrollPane;
	
	//These are used in building the new POAM
	private static final String newline = "\n";
	private static final String split = "#";

	//this eventually ends up with the user selected data from datePicker, YYYYMMDD format
	private String formattedDate;
	//private String forwardDate;
		
	//These 2 strings will always be in the outputs, these are to test to see which one is which
	private static final String retinaHeaderTest = "NetBIOSName";
	private static final String scapHeaderTest = "Vuln ID";
	private static final String acasHeaderTest = "Plugin";
	
	private static int[] columnLocations;
	
	private static List<String> retinaColumns = new ArrayList<String>();
	private static List<String> scapColumns = new ArrayList<String>();
	private static List<String>	acasColumns = new ArrayList<String>();
	private static final String poamFirstLine = "Weakness#CAT#IA Control and Impact Code#POC#Resources Required#Scheduled Completion Date#Milestones with Completion Dates#Milestone Changes#Source Identifying Weakness#Status#Comments#System"; //#Severity
	//hardcodedgoodness aww yis
	
	private static final String id = "ID: ";
	private static final String title = "TITLE: ";
	private static final String description = "DESCRIPTION: ";
	private static final String poc = "Project Officer"; // this is doc'd out because POC is now a varible to select by the user
	//private String poc;
	private static final String status = "OnGoing";
	private static final String resourcesrequired = "Time and Manpower";
	private String scheduledcompletiondate = "Fixed in ";
	private static final String notApplicable = "N/A";
	private static final String zeroMessage = "Vender patch will be applied to the next system update when released";
	private static final String retinaZeroDay = "(Zero-Day)";
	private static final String retina = "Retina";
	
	//iacMap is the retina id maped to the IAC, 
	private Map<String, String> iacMap = new HashMap<String, String>();
	//waviedMap is the list of waived findings
	private Map<String, String> waviedMap = new HashMap<String, String>();
	//this actually holds all the entries and is written to file
	private Map<String, POAMEntry> entryMap = new HashMap<String, POAMEntry>();
	
	/**
	 * @param args
	 * 
	 * You know for a main method, it doesn't do shit except call up GUI
	 */
	public static void main(String[] args){
		try {
			
			SwingUtilities.invokeLater(new Runnable() {
	            public void run() {
	                //Turn off metal's use of bold fonts
	                UIManager.put("swing.boldMetal", Boolean.FALSE); 
	                createAndShowGUI();
	            }
	        });	
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * POAMMaker
	 * @param retinaVersionTextbox 
	 * 
	 */
	private POAMbuilder() {
        super(new BorderLayout());
 
        try {
       	
	        //This is the TextArea where the filesselected output goes to.
        	filesSelected = new JTextArea(5,20);
        	filesSelected.setMargin(new Insets(10,10,10,10));
        	filesSelected.setEditable(false);
        	filesSelected.setAutoscrolls(true);
	        
        	filesSelectedScrollPane = new JScrollPane(filesSelected);
	 
	        //Create a file chooser
	        fileChooser = new JFileChooser();
	        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files", "csv");
	        fileChooser.setFileFilter(filter);
	
	        //button that activates the file selection option
	        selectButton = new JButton("Select File");
	        selectButton.addActionListener(this);
	        
	        //this label/textbox is for the system description 
	        Label systemDescriptorLabel = new Label("POAM Name:");
	        poamDescriptorTextbox = new JFormattedTextField();	
	        poamDescriptorTextbox.setColumns(10);
	        poamDescriptorTextbox.setToolTipText("Set POAM Name here");
	        
	        
	        Label retinaVersionLabel = new Label("Retina Ver:");
	        retinaVersionTextbox = new JFormattedTextField();
	        retinaVersionTextbox.setColumns(10);
	        retinaVersionTextbox.setToolTipText("Retina version can be found under Help > About");
	        
	        Label dateOfScanLabel = new Label("Date of Scan");
	        dateOfScanTextbox = new JFormattedTextField(new Date());
	        dateOfScanTextbox.setColumns(10);
	        dateOfScanTextbox.setToolTipText("Input date of scans, expected format YYYYMMMDD");
	        
	        JPanel inputPanel = new JPanel(); //use FlowLayout
	        
	        	inputPanel.add(selectButton);
	        	inputPanel.add(systemDescriptorLabel);
	        	inputPanel.add(poamDescriptorTextbox);
	        	inputPanel.add(retinaVersionLabel);
	        	inputPanel.add(retinaVersionTextbox);
	        	
	        JPanel secoundaryInputPanel = new JPanel();
	      //UNDOC     DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
	      //UNDOC     model.addElement("Project Officer");
	      //UNDOC     model.addElement("NAVSEA");
	      //UNDOC     model.addElement("DCGS-MC IAM");
	      //UNDOC     model.addElement("IAS FoS IAM");
	      //UNDOC     model.addElement("Other");
          //UNDOC     comboBox = new JComboBox<>(model);
            secoundaryInputPanel.add(new Label("Select POC: "));
          //UNDOC     secoundaryInputPanel.add(comboBox);
            secoundaryInputPanel.add(dateOfScanLabel);
            
            datePicker = JDateComponentFactory.createJDatePicker();
            secoundaryInputPanel.add((JComponent) datePicker);
	        	
	        
	        JPanel executePanel = new JPanel();
	        	executeButton = new JButton("Execute");
	        	executeButton.addActionListener(this);
	        	executePanel.add(executeButton);
	        	clearButton = new JButton("Clear");
	        	clearButton.addActionListener(this);
	        	executePanel.add(clearButton);
	        	

	        //build panel
	        JPanel rootPanel = new JPanel(new GridLayout(4, 1));
		        rootPanel.add(inputPanel);
		        
		        rootPanel.add(secoundaryInputPanel);   
		 
		    
	        //add the buttons and the log to this panel.
	        add(rootPanel, BorderLayout.PAGE_START);
	        add(filesSelectedScrollPane, BorderLayout.CENTER);
	        add(executePanel,  BorderLayout.PAGE_END);
	        
        } catch (Exception e) {
    		e.printStackTrace();
    	}
        
    }
 
	/**
	 * actionPerformed
	 * 
	 * 
	 */
    public void actionPerformed(ActionEvent actionEvent) {
    	try {		
    		//this handles the EXECUTEBUTTON action,
	        if (actionEvent.getSource() == executeButton) {
	        	//ensure some files are selected
	        	if (fileChooser.getSelectedFiles().length > 0) {
		            parser(fileChooser.getSelectedFiles());
	        	} else {
        			JOptionPane.showMessageDialog(this, "Error, No Files Selected", "POAM Maker", JOptionPane.ERROR_MESSAGE);
        		}
	        	
	        } //if (actionEvent.getSource() == executeButton) { 
	        
	        //this handles the file selection and changes the button name to the file name
	        if(actionEvent.getSource() == selectButton){
	        	//open the file choose dialog box
	        	fileChooser.setMultiSelectionEnabled(true);
	        	
	        	//set Cursor to wait mode
	        	setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	        	
	        	int returnVal = fileChooser.showOpenDialog(POAMbuilder.this);
	        	
	        	//if they've selected a file
	        	if (returnVal == JFileChooser.APPROVE_OPTION) { 
	        		//once you select file change button name to file name to confirm selection
	        		File[] file = fileChooser.getSelectedFiles();;   
	        		
        			for (int counter = 0; counter < file.length; counter++ ){
        				//display selected files in the text box so user can confirm selection
        				filesSelected.append(file[counter].getName() + newline);
        			} //while (counter < file.length )
	        	} //if (returnVal == JFileChooser.APPROVE_OPTION) 
	        	
	        	//release Cursor
	        	setCursor(null);
	        	
	        } //if(actionEvent.getSource() == selectButton)
	        
	        //this clear/cancel button doesn't work, don't know why
	        if(actionEvent.getSource() == clearButton){
	        	fileChooser.cancelSelection();
	        	filesSelected.removeAll();
	        	fileChooser = new JFileChooser();
		        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files", "csv");
		        fileChooser.setFileFilter(filter);
		        filesSelectedScrollPane = new JScrollPane(filesSelected);
	        }//if(actionEvent.getSource() == clearButton){
	        
	      
	        
    	 } catch (Exception e) {
				e.printStackTrace();
		 } 
    } //end actionPerformed
 
    /**
     * createAndShowGUI
     * 
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
    	JFrame.setDefaultLookAndFeelDecorated(true);
        JFrame frame = new JFrame("File Chooser");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 300);
        
        //Add content to the window.
        frame.add(new POAMbuilder());
 
        //Display the window.
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

    } // end createAndShowGUI   
    
    /**
     * parser
     * Creates the POAM with CSV from SCAP out put
     * 
     * 
     */
    public void parser(File[] file) {
    	try {
    		fillColumns();
    		loadIACMap();
    		loadWaivedRetinaFindings();
    		setDate();

    		String poamDescriptorString = poamDescriptorTextbox.getText();
    		String retinaVersionString = "";
    		
    		if(retinaVersionTextbox.getText() != null){
    			retinaVersionString = retinaVersionTextbox.getText();
    		}//if(retinaVersionTextbox.getText() != null){
            
			//this is for loop'n multiple files
            int counter = 0;
            File currentFile;
            
            while ( counter < file.length  && (currentFile = file[counter].getCanonicalFile()) != null){
           
	            //This reads in the CSV file name, it's looking for everything before the _
            	//so IW-Workstation_Foo Bar File name.csv becomes "IW-Workstation"
            	int endIndex = currentFile.getName().indexOf("_");
            	String systemName = "unknown";
            	if (endIndex > 0){
            		systemName = currentFile.getName().substring(0, endIndex);
            	} else { //if they forgot just use the whole file name, they'll have to fix it later
            		systemName = currentFile.getName();
            	}//if (endIndex > 0){
            	
	            CSVReader reader = new CSVReader(new FileReader(currentFile));

				String[] firstLine = reader.readNext();//this allows us to figure what kind of file it is
				
				//UNDOC poc = (String) comboBox.getSelectedItem();
				
				if(stringContainsItemFromList(scapHeaderTest, firstLine)){
					loadColumns(scapColumns, firstLine);
					scapEntry(reader, systemName);
	        	}//IF SCAP
				
				if(stringContainsItemFromList(retinaHeaderTest, firstLine)){
					loadColumns(retinaColumns, firstLine);
					retinaEntry(reader, systemName, retinaVersionString);
	        	}//IF Retina
				
				if(stringContainsItemFromList(acasHeaderTest, firstLine)){
					loadColumns(acasColumns, firstLine);
					acasEntry(reader, systemName);
				}//IF ACAS
				
		        counter++; //incrimate the counter
			    reader.close();
			    
            }
            
            CSVWriter writer = new CSVWriter(new FileWriter(poamDescriptorString + "_POAM.csv"), ',');
            
            //put first line of poam headers in
            writer.writeNext(poamFirstLine.split(split));

            //this part takes the entryMap and puts it to paper!
			Iterator<Entry<String, POAMEntry>> entries = entryMap.entrySet().iterator();
			while (entries.hasNext()) {
			  Entry<String, POAMEntry> thisEntry = (Entry<String, POAMEntry>) entries.next();
			  //String key = (String) thisEntry.getKey();  //not needed..yet
			  POAMEntry value = (POAMEntry) thisEntry.getValue();
			  
			  String line;  // = id + value.getFindingID();					  
			  line = title + value.getTitle();
			  line = line + newline + newline + description + value.getDescription();
			  line = line + split + value.getCat();
			  //line = line + split + value.getLevel();
			  line = line + split + value.getIac();
			  line = line + split + value.getPoc();
			  line = line + split + value.getResourcesrequired();
			  if(value.getResourcesrequired().equals(notApplicable)){
				  line = line + split + notApplicable;
			  } else {
				  line = line + split + value.getScheduledcompletiondate();;
			  }//Weakness	CAT	IA Control and Impact Code	POC	Resources Required	Scheduled Completion Date	Milestones with Completion Dates	Milestone Changes	Source Identifying Weakness	Status	Comments	System

			  line = line + split;
			  line = line + split; // + value.getScheduledcompletiondate();
			  line = line + split;
			  if(value.getFindingSource().equals(retina)){
				  line = line + "eEye Retina Scan on";
				  line = line + newline + formattedDate;
				  if(!retinaVersionTextbox.getText().equals("")){
					  line = line + newline + "v" + retinaVersionTextbox.getText( );
		    	  } 
				  line = line + newline +"ID: " + value.getFindingID();
			  } else {
				  line = line + value.getFindingSource();
				  line = line  + newline + formattedDate;
				  line = line  + newline + "ID: " + value.getFindingID();
			  }

			  line = line + split + value.getStatus();
			  line = line + split + value.getComment();
			  line = line + split + value.getSystemName();
			  
			  writer.writeNext(line.split(split));
			}
            JOptionPane.showMessageDialog(this, "Success", "POAM Maker", JOptionPane.PLAIN_MESSAGE);
		    writer.close();
		    
		} catch (Exception e) {
			e.printStackTrace();
		}

    } // end SCAPParser
    

    /**
     * There's got to be a better way to do this
     * */
	private void setDate() {
		try {
			Map<String, String> monthMap = new HashMap<String, String>();
			monthMap.put("0","JAN");
			monthMap.put("1","FEB");
			monthMap.put("2","MAR");
			monthMap.put("3","APR");
			monthMap.put("4", "MAY");
			monthMap.put("5", "JUNE");
			monthMap.put("6", "JULY");
			monthMap.put("7", "AUG");
			monthMap.put("8", "SEPT");
			monthMap.put("9", "OCT");
			monthMap.put("10", "NOV");
			monthMap.put("11", "DEC");
			monthMap.put("12", "JAN");
			monthMap.put("13", "FEB");
			monthMap.put("14", "MAR");
			
			//datePicker is a global var, user selected
			//if datePicker is null it's set to todays date,
			int y = datePicker.getModel().getYear();
			int m = datePicker.getModel().getMonth(); //month count starts at 0
			int d = datePicker.getModel().getDay();
			
			String day = "";
			if(d <= 9){
				day = "0" + Integer.toString(d);
			} else {
				day = Integer.toString(d);
			}
			//YYYYMMDD 
			formattedDate = Integer.toString(y) + monthMap.get(Integer.toString(m)) + day;
			
			//forward date is for typical 90 day patch cycle
			//datePicker.getModel().addMonth(3);   //wtf this doesn't work...
			
			scheduledcompletiondate = Integer.toString(y) + monthMap.get(Integer.toString(m+3));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

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
    public static boolean stringContainsItemFromList(String inputString, String[] firstLine){
        for(int i =0; i < firstLine.length; i++){
            if(inputString.equals(firstLine[i])){
                return true;
            }
        }
        return false;
    }//public static boolean stringContainsItemFromList(String inputString, String[] firstLine){
    
    /**
     *  
     * loadColumns
     * 
     * @var List<String> columns,
     * This is your static string of SCAP/Retina column names
     *  
     * @var  String[] firstLine,
     * This is the first line of the file that is currently being processed
     * 
     * @info
     * This takes the given SCAP or Retina arrays and finds them in the first line of the file and returns their column location, 
     * this way we know the location of the columns we need each time
     * 
     * */
    public static void loadColumns(List<String> columns, String[] firstLine){
    	//reset column locations, there might be a better way to do this, but I don't know
    	columnLocations = new int[] {0,0,0,0,0,0,0,0,0,0};
    	//load columns with file column locations
    	List<String> fl = Arrays.asList(firstLine);
    	int x = 0;
    	for(String s:columns){
		   columnLocations[x] = fl.indexOf(s);
		   x++;
		}
    }// public static void loadColumns(List<String> columns, String[] firstLine){
    
    
    /**
     * acasEntry
     * 
     * parser() calls this method to handle ACAS files
     *
     * @var CSVReader reader, 
     * the current loaded file from filechooser 
     *  
     * @var String systemName, 
     * a parsed system name that came from the filename
     * 
     */
    
    private void acasEntry(CSVReader reader, String systemName){
    	String[] nextLine;
    	String cat = "";
    	
    	try {
    		while ((nextLine = reader.readNext()) != null) {
    			
    			cat = nextLine[columnLocations[2]];
    			System.out.println(cat);
    			
				if(!nextLine[columnLocations[2]].equals("Info") ){ 
					/**
					 * 		acasColumns.add("Plugin Name"); //0
							acasColumns.add("Synopsis"); //1
							acasColumns.add("Severity"); //2
							acasColumns.add("Plugin"); //3
							acasColumns.add("CVE"); //4
							acasColumns.add("Solution"); //5
					 */
					POAMEntry pe = new POAMEntry();
					pe.setTitle(nextLine[columnLocations[0]]);
					pe.setDescription(nextLine[columnLocations[1]]);
					pe.setLevel(nextLine[columnLocations[2]]);
					
					if(cat.equals("high") || cat.equals("critical")){
						pe.setCat("I");
					} else if (cat.equals("medium")){
						pe.setCat("II");
					} else if (cat.equals("low")){
						pe.setCat("III");
					} else {
						pe.setCat("CAT NOT FOUND");
					}

					
					pe.setIac(""); 
					pe.setPoc(poc);
					pe.setResourcesrequired(resourcesrequired);
					pe.setScheduledcompletiondate(scheduledcompletiondate);
					pe.setFindingSource("ACAS");
					pe.setFindingID(nextLine[columnLocations[3]]); //the great vuln id vs stig id debate, stigid = 4
					pe.setSystemName(systemName);
		    		pe.setStatus(status);
		    		
		    		String key = nextLine[columnLocations[4]]+"ACAS"; 
		    		
		    		entryMap.put(key, pe);
				} // end if(nextLine[columnLocations[7]].equals("Open") ){
					
			} // end while ((nextLine = reader.readNext()) != null) {
    	} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    /**
     * scapEntry
     * 
     * parser() calls this method to handle SCAP files
     * 
     * @var CSVReader reader, 
     * the current loaded file from filechooser 
     *  
     * @var String systemName, 
     * a parsed system name that came from the filename
     * */
    private void scapEntry(CSVReader reader, String systemName){
    	String[] nextLine;
    	String cat = "";
    	
    	try {
			while ((nextLine = reader.readNext()) != null) {

				if(nextLine[columnLocations[7]].equals("Open") ){ // && !nextLine[0].equals("Vuln ID")
					//Fix/Shorten STIG name
					POAMEntry pe = new POAMEntry();
					String stigName = shortenSTIGName(nextLine[columnLocations[6]]);

					pe.setTitle(nextLine[columnLocations[0]]);
					pe.setDescription(nextLine[columnLocations[1]]);
					
					String level = nextLine[columnLocations[3]];
					if(level.contains("high") || level.contains("medium") || level.contains("low")){
						//order matters here, because some controls have mutliple levele, let the highest be set last
						if(level.contains("low")){level="low";}
						if(level.contains("medium")){level="medium";}
						if(level.contains("high")){level="high";}
					} else {
						pe.setLevel("");
					}
					
					pe.setLevel(level);	
					pe.setIac(level);
					
					//Fix CAT levels
					String catToNumber = nextLine[columnLocations[2]];
					if(catToNumber.equals("high")){cat = "I";}
					if(catToNumber.equals("medium")){cat = "II";}
					if(catToNumber.equals("low")){cat = "III";}
					pe.setCat(cat);
					
    				pe.setIac(nextLine[columnLocations[3]]); 
					pe.setPoc(poc);
					pe.setResourcesrequired(resourcesrequired);
					pe.setScheduledcompletiondate(scheduledcompletiondate);
					pe.setFindingSource(stigName);
					pe.setFindingID(nextLine[columnLocations[8]]); //the great vuln id vs stig id debate, stigid = 4
		    		pe.setStatus(status);
    				
		    		//UNIX sometimes have # in its comments, have to parse it out
		    		String comment = nextLine[columnLocations[5]].replace(split, ", ");
		    		pe.setComment(comment);
					pe.setSystemName(systemName);
				   
				    // feed poamEntry in array
					String key = nextLine[columnLocations[4]]+stigName; //id + stig allows me to fold the duplicates in faster and more effiecntly
					
					if(entryMap.get(key) == null){ // && entryMap.get(key).equals("")
						entryMap.put(key, pe);
					} else {
						POAMEntry dupePE = entryMap.get(key);
						dupePE.setSystemName(dupePE.getSystemName() + ", " + newline + systemName);
						entryMap.remove(key);
						entryMap.put(key, dupePE);
					}//if(entryMap.get(key) == null){
				    
				}//if(nextLine[columnLocations[7]].equals("Open") ){
			}//while ((nextLine = reader.readNext()) != null) {
		} catch (IOException e) {
			e.printStackTrace();
		}
    }//private void scapEntry(CSVReader reader, String systemName){

	
    /**
     * retinaEntry
     * 
     * parser() calls this method to handle retina files
     * 
     * @var CSVReader reader, the current loaded file from filechooser , 
     * @var String systemName, a parsed system name that came from the filename
     * */
    private void retinaEntry(CSVReader reader, String systemName, String retinaVersionString){
    	String[] nextLine;
    	String cat = "";
    	
    	try {
			while ((nextLine = reader.readNext()) != null) {
				//remove extra "Category" just want the I-II-III 
				cat = nextLine[columnLocations[2]].replace("Category ", "");
				
				//CAT 4s are weeded out because they're pointless to put on a poam
				if(!cat.equals("IV") && !nextLine[0].equals("NetBIOSName")){
					POAMEntry pe = new POAMEntry();
           		
            		pe.setTitle(nextLine[columnLocations[0]]);
            		pe.setDescription(nextLine[columnLocations[1]]);
            		pe.setCat(cat);
            		
            		//apply IAC if available, see method loadIACMap for complete data list
            		String key = nextLine[columnLocations[3]];
            		String iac = iacMap.get(key);
            		String risk = nextLine[columnLocations[6]];
            		//int endIndex;
            		//String cveScore = nextLine[columnLocations[7]];  //Dale requested removal of cveScore
            		//if ( cveScore.indexOf("[") > -1) {
            			//endIndex = cveScore.indexOf("[");
            			//cveScore = cveScore.substring(0, endIndex);
            		//}
            		
            		pe.setLevel(risk);
            		
            		if(iac != null){
            			//IA control double split on purpose
            			pe.setIac(iac); //+ "/" + risk);//+ cveScore);
            		} //else {
            			//pe.setIac(risk);//+ cveScore);
            		//}//if(iac != null){

            		pe.setPoc(poc);
					
            		//fix Zero-day entries
					if(pe.getTitle().contains(retinaZeroDay)){
						pe.setResourcesrequired(notApplicable);
						pe.setScheduledcompletiondate(zeroMessage);
						pe.setStatus(status);
						
					} else {
						pe.setResourcesrequired(resourcesrequired);
						pe.setScheduledcompletiondate(scheduledcompletiondate);
						pe.setStatus(status);
						
					}//if(title.contains(retinaZeroDay)){
					
					// cause sometimes you do know what the retina version is sometimes you don't 
					// retina scan data at nextLine[columnLocations[4]] 
					
					pe.setFindingSource(retina);
					pe.setFindingID(nextLine[columnLocations[3]]);
					
					String waived = waviedMap.get(key);
					if(waived != null){
						pe.setComment(waived + nextLine[columnLocations[5]].replace(split, " "));
						//fix text,  split is sometimes in the UNIX findings, must be pulled out for propper parsing
            		} else {
            			pe.setComment(nextLine[columnLocations[5]].replace(split, " "));
            			 //fix text,  split is sometimes in the UNIX findings, must be pulled out for propper parsing
            		} //if(waived != null){
					
					pe.setSystemName(systemName);
					
					//this sorts out the duplicates and combines entries
					if(entryMap.get(key) == null){ // && entryMap.get(key).equals("")
						entryMap.put(key, pe);
					} else {
						POAMEntry dupePE = entryMap.get(key);
						String dupeSystemName = dupePE.getSystemName();
						//This if/else is because of a retina report that had dupelicate entries, no differences where found but there could be errors in the future
						if(!dupeSystemName.equals(systemName)){
							dupePE.setSystemName(dupePE.getSystemName() + ", " + newline + systemName);
						} else {
							dupePE.setSystemName(dupePE.getSystemName() + ", " + newline + "POSSIBLE ERROR, PLEASE REVIEW THIS FINDING");
						}//if(!dupeSystemName.equals(systemName)){
						entryMap.remove(key);
						entryMap.put(key, dupePE);
					} //if(entryMap.get(key) == null){

	        	
	        	}//if(!nextLine[6].equals("IV") && !nextLine[0].equals("NetBIOSName")){
	        }//while ((nextLine = reader.readNext()) != null) {
			
		} catch (IOException e) {
			e.printStackTrace();
		}
    }//retinaEntry
    
    /**
     * POAMEntry Java Object
     * This object contain singular lines of the poam, you'll need to set each of the vars for a complete entry
     *  @var title;	description;cat;iac;poc;resourcesrequired;	scheduledcompletiondate;status;	findingSource;findingID;comment;systemName;
     *  
     *  
     * */
    public class POAMEntry {
    	
    	private String title;
    	private String description;
    	private String level;
    	private String cat;
    	private String iac;
    	private String poc;
    	private String resourcesrequired;
    	private String scheduledcompletiondate;
    	private String status;
    	private String findingSource;
    	private String findingID;
    	private String comment;
    	private String systemName;
    		
    		
    		public void singleEntry( String setTitle, String setDescription, String setLevel, String setCAT, String setIAC, String setPOC, 
    				String setResourcesRequired,String setScheduledCompletionDate,	String setStatus, String setFindingSource, 
    				String setFindingID, String setComment, String setSystemName)
    		{
    			title = setTitle;
    			description = setDescription;
    			level = setLevel;
    			cat = setCAT;
    			iac = setIAC;
    			poc = setPOC;
    			resourcesrequired = setResourcesRequired;
    			scheduledcompletiondate = setScheduledCompletionDate;
    			status = setStatus;
    			findingSource = setFindingSource;
    			findingID = setFindingID;
    			comment = setComment;
    			systemName = setSystemName;
    		}
   		
			public String getTitle() {
				return title;
			}


			public void setTitle(String title) {
				this.title = title;
			}


			public String getDescription() {
				return description;
			}


			public void setDescription(String description) {
				this.description = description;
			}
			
			public String getLevel() {
				return level;
			}


			public void setLevel(String level) {
				this.level = level;
			}

			public String getCat() {
				return cat;
			}


			public void setCat(String cat) {
				this.cat = cat;
			}


			public String getIac() {
				return iac;
			}


			public void setIac(String iac) {
				this.iac = iac;
			}


			public String getPoc() {
				return poc;
			}


			public void setPoc(String poc) {
				this.poc = poc;
			}


			public String getResourcesrequired() {
				return resourcesrequired;
			}


			public void setResourcesrequired(String resourcesrequired) {
				this.resourcesrequired = resourcesrequired;
			}


			public String getScheduledcompletiondate() {
				return scheduledcompletiondate;
			}


			public void setScheduledcompletiondate(String scheduledcompletiondate) {
				this.scheduledcompletiondate = scheduledcompletiondate;
			}


			public String getStatus() {
				return status;
			}


			public void setStatus(String status) {
				this.status = status;
			}


			public String getFindingSource() {
				return findingSource;
			}


			public void setFindingSource(String findingSource) {
				this.findingSource = findingSource;
			}


			public String getFindingID() {
				return findingID;
			}


			public void setFindingID(String findingID) {
				this.findingID = findingID;
			}


			public String getComment() {
				return comment;
			}


			public void setComment(String comment) {
				this.comment = comment;
			}


			public String getSystemName() {
				return systemName;
			}


			public void setSystemName(String systemName) {
				this.systemName = systemName;
			}
    		
    		
    }
    
    /**
     * loadColumns
     * 
     * Loads retina and scap columns into the list so they can be used in loadColumns()
     * */
    private void fillColumns() {
		retinaColumns.add("Name"); //0
		retinaColumns.add("Description");//1
		retinaColumns.add("SevCode");//2
		retinaColumns.add("AuditID");//3
		retinaColumns.add("Date");//4
		retinaColumns.add("FixInformation");//5
		retinaColumns.add("Risk");//6
		retinaColumns.add("CVSSScore");//7

		scapColumns.add("Rule Title");//0
		scapColumns.add("Discussion");//1
		scapColumns.add("Severity");//2
		scapColumns.add("IA Controls");//3
		scapColumns.add("STIG ID");//4
		scapColumns.add("Fix Text");//5
		scapColumns.add("STIG");//6
		scapColumns.add("Status");//7
		scapColumns.add(scapHeaderTest);//8
		
		acasColumns.add("Plugin Name"); //0
		acasColumns.add("Synopsis"); //1
		acasColumns.add("Severity"); //2
		acasColumns.add("Plugin"); //3
		acasColumns.add("CVE"); //4
		acasColumns.add("Solution"); //5

		
		
	}//private void fillColumns() {
    
    
    /**
     * shortSTIGName
     * @param stig
     * @return String parse stig name
     * 
     * STIG names can be unnecessarly long, this shortens them to a more managable length
     */
    private String shortenSTIGName(String stig) {
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
     * loadWaviedRetinaFindings
     * 
     * This loads the waived retina findings map
     * 
     * */
    private void loadWaivedRetinaFindings(){
    	String verbage1 = " Then this vulnerablity can be waived per the WAIVED FINDINGS GUIDEANCE" + newline;
    	String verbage2 = " This vulnerablity can be waived per the WAIVED FINDINGS GUIDEANCE" + newline;
    	String informational = " This is an informational check which does not indentify an actual vulnerability exists. " + newline;
    	String serviceAccounts = " If only service accounts are found "  + newline;
    	waviedMap.put("7", serviceAccounts + verbage1);
    	waviedMap.put("4.026", serviceAccounts + verbage1);
    	waviedMap.put("18", serviceAccounts + verbage1);
    	waviedMap.put("200", verbage1);
    	waviedMap.put("5.068", verbage1);
    	waviedMap.put("3067", "Waived for NMCI due to opertional requirements until an alternative product is implemented for remote control capability, " + verbage2);
    	waviedMap.put("3381", "Provided te Security Option 'Network Access: Let Everyone permissions apply to anonymous users' is set to 'Disabled' and the Power Users Group has no users " + verbage1);
    	waviedMap.put("3483", "If only service accounts are found " + verbage1);
    	waviedMap.put("173","This retina finding is in conflict with the STIG findings for this setting, if its set in accordance to the STIG " + verbage1);
    	waviedMap.put("418", "Waived due to operational requirements. Disabling automatic drive sharing on the host would hinder Retinas ablitiy to scan, " + verbage1);
    	waviedMap.put("419", "Waived due to operational requirements. Disabling automatic drive sharing on the host would hinder Retinas ablitiy to scan, " + verbage1);
    	waviedMap.put("1408", "Waived due to operational requirements. " + verbage2);
    	waviedMap.put("2056", "Wavied due to operational requirements, and this is no longer a STIG requirement, " + verbage2);
    	waviedMap.put("2103", "Wavied due to operational requirements, and this is no longer a STIG requirement, " + verbage2);
    	waviedMap.put("2104", "Wavied due to operational requirements, and this is no longer a STIG requirement, " + verbage2);
    	waviedMap.put("3016", "Wavied due to operational requirements, " + verbage2);
    	waviedMap.put("3228", "Wavied due to operational requirements, " + verbage2);
    	waviedMap.put("3260", "Wavied as this is no longer a STIG requirement, " + verbage2);
    	waviedMap.put("3431", "Wavied due to operational requirements, " + verbage2);
    	waviedMap.put("3432", "Wavied due to operational requirements, " + verbage2);
    	waviedMap.put("3684", serviceAccounts + verbage1);
    	waviedMap.put("5387", "Waived for NMCI due to opertional requirements until an alternative product is implemented for remote control capability, " + verbage2);
    	waviedMap.put("5432", "Wavied due to operational requirements, " + verbage2);
    	waviedMap.put("6185", "Accepted risk for opertional requirements since no fix is available from the vendor, " + verbage2);
    	waviedMap.put("3688", "Wavied, provided HBSS firewall is installed and enabled with the USMC Policy, " + verbage1);
    	waviedMap.put("3725", "If only service accounts are found " + verbage1);
    	waviedMap.put("5328", "Approved as the removal of the (creator-owner) has the potential to break many COM+ components which may be required for functionality"  + newline);
    	waviedMap.put("3144", informational + verbage2);
    	waviedMap.put("3199", informational + verbage2);
    	waviedMap.put("3489", informational + verbage2);
    	waviedMap.put("3492", informational + verbage2);
    	waviedMap.put("3493", informational + verbage2);
    	waviedMap.put("3494", informational + verbage2);
    	waviedMap.put("3496", informational + verbage2);
    	waviedMap.put("3502", informational + verbage2);
    	waviedMap.put("3503", informational + verbage2);
    	waviedMap.put("3504", informational + verbage2);
    	waviedMap.put("3505", informational + verbage2);
    	waviedMap.put("3507", informational + verbage2);
    	waviedMap.put("3509", informational + verbage2);
    	waviedMap.put("3533", informational + verbage2);
    	waviedMap.put("3623", informational + verbage2);
    	waviedMap.put("3683", informational + verbage2);
    	waviedMap.put("3691", informational + verbage2);
    	waviedMap.put("3825", informational + verbage2);
    	waviedMap.put("5894", informational + verbage2);
    	waviedMap.put("6798", informational + verbage2);
    	waviedMap.put("3623", informational + verbage2);   	
    	waviedMap.put("7202", informational + verbage2);
    	waviedMap.put("7203", informational + verbage2);
    	waviedMap.put("7249", informational + verbage2);
    	waviedMap.put("7250", informational + verbage2);
    	waviedMap.put("7255", informational + verbage2);
    	waviedMap.put("7282", informational + verbage2);
    	waviedMap.put("7281", informational + verbage2);
    	waviedMap.put("7296", informational + verbage2);
    }
 
    /**
     * loadIACMap
     * 
     * this list is provided my DISA and extracted from their excel file, 
     * I know hard coding this is terrible practice
     * use the IACMapping package to regenerate this data as needed
     * */
    private void loadIACMap(){
    	iacMap.put("7", "ECSC-1 / High");
    	iacMap.put("9", "ECSC-1 / High");
    	iacMap.put("10", "ECSC-1 / High");
    	iacMap.put("11", "ECSC-1 / High");
    	iacMap.put("12", "ECSC-1 / High");
    	iacMap.put("13", "ECSC-1 / High");
    	iacMap.put("14", "ECSC-1 / High");
    	iacMap.put("18", "DCSQ-1 / Medium");
    	iacMap.put("42", "ECSC-1 / High");
    	iacMap.put("65", "ECSC-1 / High");
    	iacMap.put("68", "ECSC-1 / High");
    	iacMap.put("106", "ECSC-1 / High");
    	iacMap.put("163", "ECSC-1 / High");
    	iacMap.put("165", "ECSC-1 / High");
    	iacMap.put("166", "ECSC-1 / High");
    	iacMap.put("167", "ECSC-1 / High");
    	iacMap.put("169", "ECSC-1 / High");
    	iacMap.put("172", "ECSC-1 / High");
    	iacMap.put("173", "ECSC-1 / High");
    	iacMap.put("183", "ECSC-1 / High");
    	iacMap.put("185", "ECSC-1 / High");
    	iacMap.put("186", "DCSQ-1 / Medium");
    	iacMap.put("187", "ECSC-1 / High");
    	iacMap.put("200", "ECSC-1 / High");
    	iacMap.put("202", "ECSC-1 / High");
    	iacMap.put("207", "ECSC-1 / High");
    	iacMap.put("208", "ECSC-1 / High");
    	iacMap.put("209", "ECSC-1 / High");
    	iacMap.put("219", "ECSC-1 / High");
    	iacMap.put("226", "ECSC-1 / High");
    	iacMap.put("319", "ECSC-1 / High");
    	iacMap.put("321", "ECSC-1 / High");
    	iacMap.put("327", "ECSC-1 / High");
    	iacMap.put("418", "ECSC-1 / High");
    	iacMap.put("419", "ECSC-1 / High");
    	iacMap.put("734", "ECSC-1 / High");
    	iacMap.put("743", "DCSQ-1 / Medium");
    	iacMap.put("781", "ECSC-1 / High");
    	iacMap.put("884", "ECSC-1 / High");
    	iacMap.put("885", "ECSC-1 / High");
    	iacMap.put("892", "ECSC-1 / High");
    	iacMap.put("894", "ECSC-1 / High");
    	iacMap.put("897", "DCSQ-1 / Medium");
    	iacMap.put("899", "ECSC-1 / High");
    	iacMap.put("900", "ECSC-1 / High");
    	iacMap.put("901", "ECSC-1 / High");
    	iacMap.put("902", "ECSC-1 / High");
    	iacMap.put("903", "ECSC-1 / High");
    	iacMap.put("922", "ECSC-1 / High");
    	iacMap.put("976", "ECSC-1 / High");
    	iacMap.put("977", "ECSC-1 / High");
    	iacMap.put("978", "ECSC-1 / High");
    	iacMap.put("984", "ECSC-1 / High");
    	iacMap.put("987", "ECSC-1 / High");
    	iacMap.put("1053", "ECSC-1 / High");
    	iacMap.put("1225", "ECSC-1 / High");
    	iacMap.put("1226", "ECSC-1 / High");
    	iacMap.put("1253", "VIVM-1 / Medium");
    	iacMap.put("1263", "VIVM-1 / Medium");
    	iacMap.put("1267", "ECSC-1 / High");
    	iacMap.put("1293", "ECSC-1 / High");
    	iacMap.put("1329", "ECSC-1 / High");
    	iacMap.put("1351", "ECSC-1 / High");
    	iacMap.put("1407", "ECSC-1 / High");
    	iacMap.put("1408", "ECSC-1 / High");
    	iacMap.put("1428", "ECSC-1 / High");
    	iacMap.put("1543", "ECSC-1 / High");
    	iacMap.put("1578", "VIVM-1 / Medium");
    	iacMap.put("1601", "ECSC-1 / High");
    	iacMap.put("1602", "ECSC-1 / High");
    	iacMap.put("1617", "ECSC-1 / High");
    	iacMap.put("1618", "ECSC-1 / High");
    	iacMap.put("1667", "ECSC-1 / High");
    	iacMap.put("1807", "VIVM-1 / Medium");
    	iacMap.put("1812", "ECSC-1 / High");
    	iacMap.put("1834", "ECSC-1 / High");
    	iacMap.put("1881", "VIVM-1 / Medium");
    	iacMap.put("2039", "ECSC-1 / High");
    	iacMap.put("2041", "ECSC-1 / High");
    	iacMap.put("2053", "ECSC-1 / High");
    	iacMap.put("2056", "ECSC-1 / High");
    	iacMap.put("2058", "ECSC-1 / High");
    	iacMap.put("2079", "ECSC-1 / High");
    	iacMap.put("2101", "ECSC-1 / High");
    	iacMap.put("2103", "ECSC-1 / High");
    	iacMap.put("2104", "ECSC-1 / High");
    	iacMap.put("2111", "ECSC-1 / High");
    	iacMap.put("2112", "ECSC-1 / High");
    	iacMap.put("2113", "ECSC-1 / High");
    	iacMap.put("2115", "ECSC-1 / High");
    	iacMap.put("2116", "ECSC-1 / High");
    	iacMap.put("2121", "VIVM-1 / Medium");
    	iacMap.put("2132", "ECSC-1 / High");
    	iacMap.put("2135", "VIVM-1 / Medium");
    	iacMap.put("2137", "ECSC-1 / High");
    	iacMap.put("2167", "ECSC-1 / High");
    	iacMap.put("2176", "VIVM-1 / Medium");
    	iacMap.put("2177", "VIVM-1 / Medium");
    	iacMap.put("2182", "ECSC-1 / High");
    	iacMap.put("2194", "DCSQ-1 / Medium");
    	iacMap.put("2201", "ECSC-1 / High");
    	iacMap.put("2205", "VIVM-1 / Medium");
    	iacMap.put("2209", "VIVM-1 / Medium");
    	iacMap.put("2240", "ECSC-1 / High");
    	iacMap.put("2247", "ECSC-1 / High");
    	iacMap.put("2249", "VIVM-1 / Medium");
    	iacMap.put("2254", "VIVM-1 / Medium");
    	iacMap.put("2263", "ECSC-1 / High");
    	iacMap.put("2279", "ECSC-1 / High");
    	iacMap.put("2284", "VIVM-1 / Medium");
    	iacMap.put("2528", "ECSC-1 / High");
    	iacMap.put("2727", "VIVM-1 / Medium");
    	iacMap.put("2729", "ECSC-1 / High");
    	iacMap.put("2808", "VIVM-1 / Medium");
    	iacMap.put("2913", "ECSC-1 / High");
    	iacMap.put("2993", "ECSC-1 / High");
    	iacMap.put("2994", "ECSC-1 / High");
    	iacMap.put("2995", "ECSC-1 / High");
    	iacMap.put("2996", "ECSC-1 / High");
    	iacMap.put("2997", "ECSC-1 / High");
    	iacMap.put("2998", "ECSC-1 / High");
    	iacMap.put("3001", "ECSC-1 / High");
    	iacMap.put("3009", "ECSC-1 / High");
    	iacMap.put("3013", "ECSC-1 / High");
    	iacMap.put("3014", "ECSC-1 / High");
    	iacMap.put("3015", "ECSC-1 / High");
    	iacMap.put("3016", "ECSC-1 / High");
    	iacMap.put("3051", "ECSC-1 / High");
    	iacMap.put("3053", "ECSC-1 / High");
    	iacMap.put("3054", "ECSC-1 / High");
    	iacMap.put("3056", "ECSC-1 / High");
    	iacMap.put("3058", "ECSC-1 / High");
    	iacMap.put("3059", "ECSC-1 / High");
    	iacMap.put("3060", "ECSC-1 / High");
    	iacMap.put("3062", "ECSC-1 / High");
    	iacMap.put("3063", "ECSC-1 / High");
    	iacMap.put("3064", "ECSC-1 / High");
    	iacMap.put("3066", "ECSC-1 / High");
    	iacMap.put("3067", "ECSC-1 / High");
    	iacMap.put("3068", "ECSC-1 / High");
    	iacMap.put("3069", "ECSC-1 / High");
    	iacMap.put("3070", "ECSC-1 / High");
    	iacMap.put("3071", "ECSC-1 / High");
    	iacMap.put("3072", "ECSC-1 / High");
    	iacMap.put("3074", "ECSC-1 / High");
    	iacMap.put("3075", "ECSC-1 / High");
    	iacMap.put("3077", "ECSC-1 / High");
    	iacMap.put("3078", "ECSC-1 / High");
    	iacMap.put("3079", "ECSC-1 / High");
    	iacMap.put("3080", "ECSC-1 / High");
    	iacMap.put("3081", "ECSC-1 / High");
    	iacMap.put("3082", "ECSC-1 / High");
    	iacMap.put("3083", "ECSC-1 / High");
    	iacMap.put("3084", "ECSC-1 / High");
    	iacMap.put("3085", "ECSC-1 / High");
    	iacMap.put("3086", "ECSC-1 / High");
    	iacMap.put("3087", "ECSC-1 / High");
    	iacMap.put("3088", "ECSC-1 / High");
    	iacMap.put("3090", "ECSC-1 / High");
    	iacMap.put("3093", "ECSC-1 / High");
    	iacMap.put("3095", "ECSC-1 / High");
    	iacMap.put("3096", "ECSC-1 / High");
    	iacMap.put("3097", "ECSC-1 / High");
    	iacMap.put("3098", "ECSC-1 / High");
    	iacMap.put("3099", "ECSC-1 / High");
    	iacMap.put("3101", "ECSC-1 / High");
    	iacMap.put("3103", "ECSC-1 / High");
    	iacMap.put("3107", "ECSC-1 / High");
    	iacMap.put("3108", "ECSC-1 / High");
    	iacMap.put("3125", "ECSC-1 / High");
    	iacMap.put("3134", "ECSC-1 / High");
    	iacMap.put("3135", "ECSC-1 / High");
    	iacMap.put("3138", "ECSC-1 / High");
    	iacMap.put("3140", "ECSC-1 / High");
    	iacMap.put("3144", "ECSC-1 / High");
    	iacMap.put("3172", "VIVM-1 / Medium");
    	iacMap.put("3173", "ECSC-1 / High");
    	iacMap.put("3177", "ECSC-1 / High");
    	iacMap.put("3180", "IAIA-2 / High (IAC is class only!!!)");
    	iacMap.put("3183", "ECSC-1 / High");
    	iacMap.put("3187", "ECSC-1 / High");
    	iacMap.put("3188", "ECSC-1 / High");
    	iacMap.put("3189", "ECSC-1 / High");
    	iacMap.put("3190", "ECSC-1 / High");
    	iacMap.put("3191", "ECSC-1 / High");
    	iacMap.put("3192", "ECSC-1 / High");
    	iacMap.put("3193", "ECSC-1 / High");
    	iacMap.put("3194", "ECSC-1 / High");
    	iacMap.put("3195", "ECSC-1 / High");
    	iacMap.put("3197", "ECSC-1 / High");
    	iacMap.put("3198", "ECSC-1 / High");
    	iacMap.put("3199", "ECSC-1 / High");
    	iacMap.put("3205", "ECSC-1 / High");
    	iacMap.put("3208", "ECSC-1 / High");
    	iacMap.put("3209", "ECSC-1 / High");
    	iacMap.put("3210", "ECSC-1 / High");
    	iacMap.put("3218", "ECSC-1 / High");
    	iacMap.put("3219", "ECSC-1 / High");
    	iacMap.put("3220", "ECSC-1 / High");
    	iacMap.put("3221", "ECSC-1 / High");
    	iacMap.put("3222", "ECSC-1 / High");
    	iacMap.put("3227", "ECSC-1 / High");
    	iacMap.put("3228", "ECSC-1 / High");
    	iacMap.put("3260", "ECSC-1 / High");
    	iacMap.put("3318", "ECSC-1 / High");
    	iacMap.put("3319", "ECSC-1 / High");
    	iacMap.put("3320", "ECSC-1 / High");
    	iacMap.put("3321", "ECSC-1 / High");
    	iacMap.put("3322", "ECSC-1 / High");
    	iacMap.put("3323", "ECSC-1 / High");
    	iacMap.put("3324", "ECSC-1 / High");
    	iacMap.put("3325", "ECSC-1 / High");
    	iacMap.put("3326", "ECSC-1 / High");
    	iacMap.put("3327", "ECSC-1 / High");
    	iacMap.put("3328", "ECSC-1 / High");
    	iacMap.put("3329", "ECSC-1 / High");
    	iacMap.put("3330", "ECSC-1 / High");
    	iacMap.put("3331", "ECSC-1 / High");
    	iacMap.put("3332", "ECSC-1 / High");
    	iacMap.put("3333", "ECSC-1 / High");
    	iacMap.put("3334", "ECSC-1 / High");
    	iacMap.put("3335", "ECSC-1 / High");
    	iacMap.put("3336", "ECSC-1 / High");
    	iacMap.put("3337", "ECSC-1 / High");
    	iacMap.put("3338", "ECSC-1 / High");
    	iacMap.put("3339", "ECSC-1 / High");
    	iacMap.put("3340", "ECSC-1 / High");
    	iacMap.put("3341", "ECSC-1 / High");
    	iacMap.put("3342", "ECSC-1 / High");
    	iacMap.put("3343", "ECSC-1 / High");
    	iacMap.put("3344", "ECSC-1 / High");
    	iacMap.put("3346", "ECSC-1 / High");
    	iacMap.put("3347", "ECSC-1 / High");
    	iacMap.put("3348", "ECSC-1 / High");
    	iacMap.put("3350", "ECSC-1 / High");
    	iacMap.put("3374", "VIVM-1 / Medium");
    	iacMap.put("3377", "ECSC-1 / High");
    	iacMap.put("3381", "ECSC-1 / High");
    	iacMap.put("3397", "ECSC-1 / High");
    	iacMap.put("3398", "ECSC-1 / High");
    	iacMap.put("3399", "ECSC-1 / High");
    	iacMap.put("3400", "ECSC-1 / High");
    	iacMap.put("3401", "ECSC-1 / High");
    	iacMap.put("3405", "ECSC-1 / High");
    	iacMap.put("3406", "ECSC-1 / High");
    	iacMap.put("3407", "ECSC-1 / High");
    	iacMap.put("3408", "ECSC-1 / High");
    	iacMap.put("3409", "ECSC-1 / High");
    	iacMap.put("3410", "ECLP-1 / High");
    	iacMap.put("3412", "ECSC-1 / High");
    	iacMap.put("3413", "ECSC-1 / High");
    	iacMap.put("3414", "ECLP-1 / High");
    	iacMap.put("3417", "ECSC-1 / High");
    	iacMap.put("3419", "ECSC-1 / High");
    	iacMap.put("3422", "ECSC-1 / High");
    	iacMap.put("3427", "ECSC-1 / High");
    	iacMap.put("3429", "ECSC-1 / High");
    	iacMap.put("3431", "ECSC-1 / High");
    	iacMap.put("3432", "ECSC-1 / High");
    	iacMap.put("3450", "ECSC-1 / High");
    	iacMap.put("3452", "ECSC-1 / High");
    	iacMap.put("3462", "ECSC-1 / High");
    	iacMap.put("3482", "ECSC-1 / High");
    	iacMap.put("3483", "ECSC-1 / High");
    	iacMap.put("3488", "ECSC-1 / High");
    	iacMap.put("3489", "ECSC-1 / High");
    	iacMap.put("3490", "ECSC-1 / High");
    	iacMap.put("3491", "ECSC-1 / High");
    	iacMap.put("3492", "ECSC-1 / High");
    	iacMap.put("3493", "ECSC-1 / High");
    	iacMap.put("3494", "ECSC-1 / High");
    	iacMap.put("3495", "DCSQ-1 / Medium");
    	iacMap.put("3496", "ECSC-1 / High");
    	iacMap.put("3498", "ECSC-1 / High");
    	iacMap.put("3499", "ECSC-1 / High");
    	iacMap.put("3500", "ECSC-1 / High");
    	iacMap.put("3502", "ECSC-1 / High");
    	iacMap.put("3503", "ECSC-1 / High");
    	iacMap.put("3504", "ECSC-1 / High");
    	iacMap.put("3505", "ECSC-1 / High");
    	iacMap.put("3507", "ECSC-1 / High");
    	iacMap.put("3509", "ECSC-1 / High");
    	iacMap.put("3521", "ECSC-1 / High");
    	iacMap.put("3532", "ECSC-1 / High");
    	iacMap.put("3533", "ECSC-1 / High");
    	iacMap.put("3537", "ECSC-1 / High");
    	iacMap.put("3546", "ECSC-1 / High");
    	iacMap.put("3547", "ECSC-1 / High");
    	iacMap.put("3550", "ECSC-1 / High");
    	iacMap.put("3552", "ECSC-1 / High");
    	iacMap.put("3553", "ECSC-1 / High");
    	iacMap.put("3554", "ECSC-1 / High");
    	iacMap.put("3555", "ECSC-1 / High");
    	iacMap.put("3558", "VIVM-1 / Medium");
    	iacMap.put("3565", "VIVM-1 / Medium");
    	iacMap.put("3570", "VIVM-1 / Medium");
    	iacMap.put("3573", "VIVM-1 / Medium");
    	iacMap.put("3576", "ECSC-1 / High");
    	iacMap.put("3579", "VIVM-1 / Medium");
    	iacMap.put("3603", "ECVP-1 / High");
    	iacMap.put("3611", "ECSC-1 / High");
    	iacMap.put("3613", "ECSC-1 / High");
    	iacMap.put("3614", "ECSC-1 / High");
    	iacMap.put("3616", "ECSC-1 / High");
    	iacMap.put("3617", "ECSC-1 / High");
    	iacMap.put("3618", "ECSC-1 / High");
    	iacMap.put("3620", "ECSC-1 / High");
    	iacMap.put("3623", "ECSC-1 / High");
    	iacMap.put("3624", "ECSC-1 / High");
    	iacMap.put("3625", "ECSC-1 / High");
    	iacMap.put("3634", "ECSC-1 / High");
    	iacMap.put("3636", "ECSC-1 / High");
    	iacMap.put("3638", "ECSC-1 / High");
    	iacMap.put("3639", "ECLP-1 / High");
    	iacMap.put("3640", "ECSC-1 / High");
    	iacMap.put("3642", "ECSC-1 / High");
    	iacMap.put("3644", "ECSC-1 / High");
    	iacMap.put("3645", "ECSC-1 / High");
    	iacMap.put("3647", "ECSC-1 / High");
    	iacMap.put("3648", "ECSC-1 / High");
    	iacMap.put("3683", "ECSC-1 / High");
    	iacMap.put("3684", "ECSC-1 / High");
    	iacMap.put("3685", "ECSC-1 / High");
    	iacMap.put("3686", "ECSC-1 / High");
    	iacMap.put("3688", "DCCS-2 / High");
    	iacMap.put("3691", "ECSC-1 / High");
    	iacMap.put("3707", "ECSC-1 / High");
    	iacMap.put("3709", "ECLP-1 / High");
    	iacMap.put("3710", "ECSC-1 / High");
    	iacMap.put("3711", "ECSC-1 / High");
    	iacMap.put("3718", "ECSC-1 / High");
    	iacMap.put("3719", "ECSC-1 / High");
    	iacMap.put("3725", "ECSC-1 / High");
    	iacMap.put("3737", "VIVM-1 / Medium");
    	iacMap.put("3824", "VIVM-1 / Medium");
    	iacMap.put("3825", "VIVM-1 / Medium");
    	iacMap.put("3840", "ECSC-1 / High");
    	iacMap.put("4062", "ECSC-1 / High");
    	iacMap.put("4084", "VIVM-1 / Medium");
    	iacMap.put("4103", "VIVM-1 / Medium");
    	iacMap.put("4425", "DCBP-1 / Medium");
    	iacMap.put("4916", "VIVM-1 / Medium");
    	iacMap.put("4920", "VIVM-1 / Medium");
    	iacMap.put("5226", "VIVM-1 / Medium");
    	iacMap.put("5229", "VIVM-1 / Medium");
    	iacMap.put("5241", "ECSC-1 / High");
    	iacMap.put("5279", "VIVM-1 / Medium");
    	iacMap.put("5288", "VIVM-1 / Medium");
    	iacMap.put("5289", "VIVM-1 / Medium");
    	iacMap.put("5295", "VIVM-1 / Medium");
    	iacMap.put("5297", "VIVM-1 / Medium");
    	iacMap.put("5309", "ECSC-1 / High");
    	iacMap.put("5326", "ECSC-1 / High");
    	iacMap.put("5328", "ECSC-1 / High");
    	iacMap.put("5329", "ECSC-1 / High");
    	iacMap.put("5332", "ECSC-1 / High");
    	iacMap.put("5333", "ECSC-1 / High");
    	iacMap.put("5337", "ECSC-1 / High");
    	iacMap.put("5338", "ECSC-1 / High");
    	iacMap.put("5340", "ECSC-1 / High");
    	iacMap.put("5344", "ECSC-1 / High");
    	iacMap.put("5345", "ECSC-1 / High");
    	iacMap.put("5349", "ECSC-1 / High");
    	iacMap.put("5351", "ECSC-1 / High");
    	iacMap.put("5356", "ECSC-1 / High");
    	iacMap.put("5359", "ECSC-1 / High");
    	iacMap.put("5361", "ECSC-1 / High");
    	iacMap.put("5362", "ECSC-1 / High");
    	iacMap.put("5365", "ECSC-1 / High");
    	iacMap.put("5367", "ECSC-1 / High");
    	iacMap.put("5370", "ECSC-1 / High");
    	iacMap.put("5375", "ECSC-1 / High");
    	iacMap.put("5385", "ECSC-1 / High");
    	iacMap.put("5387", "ECSC-1 / High");
    	iacMap.put("5389", "ECSC-1 / High");
    	iacMap.put("5390", "ECSC-1 / High");
    	iacMap.put("5392", "ECSC-1 / High");
    	iacMap.put("5399", "ECSC-1 / High");
    	iacMap.put("5400", "ECSC-1 / High");
    	iacMap.put("5412", "ECSC-1 / High");
    	iacMap.put("5418", "ECSC-1 / High");
    	iacMap.put("5419", "ECSC-1 / High");
    	iacMap.put("5420", "ECSC-1 / High");
    	iacMap.put("5421", "ECSC-1 / High");
    	iacMap.put("5430", "ECSC-1 / High");
    	iacMap.put("5432", "ECSC-1 / High");
    	iacMap.put("5433", "ECSC-1 / High");
    	iacMap.put("5440", "ECSC-1 / High");
    	iacMap.put("5447", "ECSC-1 / High");
    	iacMap.put("5453", "ECSC-1 / High");
    	iacMap.put("5454", "ECSC-1 / High");
    	iacMap.put("5457", "ECSC-1 / High");
    	iacMap.put("5491", "ECSC-1 / High");
    	iacMap.put("5492", "ECSC-1 / High");
    	iacMap.put("5493", "ECSC-1 / High");
    	iacMap.put("5494", "ECSC-1 / High");
    	iacMap.put("5495", "ECSC-1 / High");
    	iacMap.put("5502", "ECSC-1 / High");
    	iacMap.put("5503", "VIVM-1 / Medium");
    	iacMap.put("5515", "ECSC-1 / High");
    	iacMap.put("5564", "VIVM-1 / Medium");
    	iacMap.put("5567", "VIVM-1 / Medium");
    	iacMap.put("5569", "VIVM-1 / Medium");
    	iacMap.put("5594", "ECSC-1 / High");
    	iacMap.put("5596", "ECSC-1 / High");
    	iacMap.put("5598", "VIVM-1 / Medium");
    	iacMap.put("5610", "VIVM-1 / Medium");
    	iacMap.put("5621", "ECSC-1 / High");
    	iacMap.put("5632", "VIVM-1 / Medium");
    	iacMap.put("5658", "ECSC-1 / High");
    	iacMap.put("5677", "VIVM-1 / Medium");
    	iacMap.put("5681", "VIVM-1 / Medium");
    	iacMap.put("5682", "VIVM-1 / Medium");
    	iacMap.put("5684", "VIVM-1 / Medium");
    	iacMap.put("5685", "VIVM-1 / Medium");
    	iacMap.put("5687", "VIVM-1 / Medium");
    	iacMap.put("5688", "VIVM-1 / Medium");
    	iacMap.put("5708", "DCCS-2 / High");
    	iacMap.put("5730", "VIVM-1 / Medium");
    	iacMap.put("5752", "VIVM-1 / Medium");
    	iacMap.put("5755", "VIVM-1 / Medium");
    	iacMap.put("5757", "ECSC-1 / High");
    	iacMap.put("5759", "VIVM-1 / Medium");
    	iacMap.put("5762", "EBRU-1 / High");
    	iacMap.put("5769", "IATS-2 / Medium");
    	iacMap.put("5772", "DCSQ-1 / Medium");
    	iacMap.put("5784", "DCSQ-1 / Medium, VIVM-1 / Medium");
    	iacMap.put("5806", "VIVM-1 / Medium");
    	iacMap.put("5812", "VIVM-1 / Medium");
    	iacMap.put("5836", "VIVM-1 / Medium");
    	iacMap.put("5838", "DCSQ-1 / Medium, VIVM-1 / Medium");
    	iacMap.put("5839", "ECSC-1 / High");
    	iacMap.put("5842", "DCSQ-1 / Medium, VIVM-1 / Medium");
    	iacMap.put("5844", "DCSQ-1 / Medium, VIVM-1 / Medium");
    	iacMap.put("5846", "DCSQ-1 / Medium, VIVM-1 / Medium");
    	iacMap.put("5847", "DCSQ-1 / Medium, VIVM-1 / Medium");
    	iacMap.put("5853", "DCSL-1 / Medium, DCSQ-1 / Medium, VIVM-1 / Medium");
    	iacMap.put("5869", "VIVM-1 / Medium");
    	iacMap.put("5871", "VIVM-1 / Medium");
    	iacMap.put("5878", "VIVM-1 / Medium");
    	iacMap.put("5884", "ECSC-1 / High");
    	iacMap.put("5886", "DCSQ-1 / Medium, VIVM-1 / Medium");
    	iacMap.put("5891", "ECSC-1 / High");
    	iacMap.put("5892", "VIVM-1 / Medium");
    	iacMap.put("5893", "ECLP-1 / High, IAAC-1 / High");
    	iacMap.put("5894", "VIVM-1 / Medium");
    	iacMap.put("5895", "DCBP-1 / Medium");
    	iacMap.put("5900", "VIVM-1 / Medium");
    	iacMap.put("5916", "VIVM-1 / Medium");
    	iacMap.put("5939", "VIVM-1 / Medium");
    	iacMap.put("5951", "DCSQ-1 / Medium");
    	iacMap.put("5963", "VIVM-1 / Medium");
    	iacMap.put("6015", "DCSQ-1 / Medium");
    	iacMap.put("6038", "DCSQ-1 / Medium");
    	iacMap.put("6098", "VIVM-1 / Medium");
    	iacMap.put("6141", "VIVM-1 / Medium");
    	iacMap.put("6143", "DCSQ-1 / Medium");
    	iacMap.put("6203", "DCSQ-1 / Medium");
    	iacMap.put("6219", "DCSQ-1 / Medium");
    	iacMap.put("6258", "DCSQ-1 / Medium");
    	iacMap.put("6259", "ECSC-1 / High");
    	iacMap.put("6260", "VIVM-1 / Medium");
    	iacMap.put("6263", "VIVM-1 / Medium");
    	iacMap.put("6266", "VIVM-1 / Medium");
    	iacMap.put("6269", "VIVM-1 / Medium");
    	iacMap.put("6310", "VIVM-1 / Medium");
    	iacMap.put("6316", "ECSC-1 / High");
    	iacMap.put("6317", "VIVM-1 / Medium");
    	iacMap.put("6340", "DCSQ-1 / Medium");
    	iacMap.put("6342", "DCSQ-1 / Medium");
    	iacMap.put("6365", "VIVM-1 / Medium");
    	iacMap.put("6390", "DCSQ-1 / Medium");
    	iacMap.put("6415", "DCSQ-1 / Medium");
    	iacMap.put("6416", "DCSQ-1 / Medium");
    	iacMap.put("6420", "DCSQ-1 / Medium");
    	iacMap.put("6441", "ECSC-1 / High");
    	iacMap.put("6460", "VIVM-1 / Medium");
    	iacMap.put("6465", "DCSQ-1 / Medium");
    	iacMap.put("6476", "VIVM-1 / Medium");
    	iacMap.put("6479", "VIVM-1 / Medium");
    	iacMap.put("6606", "ECSC-1 / High");
    	iacMap.put("6607", "ECSC-1 / High");
    	iacMap.put("6609", "ECLP-1 / High");
    	iacMap.put("6622", "VIVM-1 / Medium");
    	iacMap.put("6629", "VIVM-1 / Medium");
    	iacMap.put("6646", "ECSC-1 / High");
    	iacMap.put("6653", "ECSC-1 / High");
    	iacMap.put("6657", "ECSC-1 / High");
    	iacMap.put("6658", "ECSC-1 / High");
    	iacMap.put("6659", "ECSC-1 / High");
    	iacMap.put("6742", "ECSC-1 / High");
    	iacMap.put("6764", "VIVM-1 / Medium");
    	iacMap.put("6767", "VIVM-1 / Medium");
    	iacMap.put("6769", "VIVM-1 / Medium");
    	iacMap.put("6773", "VIVM-1 / Medium");
    	iacMap.put("6775", "VIVM-1 / Medium");
    	iacMap.put("6798", "ECSC-1 / High");
    	iacMap.put("6799", "ECSC-1 / High");
    	iacMap.put("6878", "ECSC-1 / High");
    	iacMap.put("6918", "VIVM-1 / Medium");
    	iacMap.put("6919", "VIVM-1 / Medium");
    	iacMap.put("6920", "VIVM-1 / Medium");
    	iacMap.put("6925", "DCSQ-1 / Medium");
    	iacMap.put("6927", "ECSC-1 / High");
    	iacMap.put("6962", "ECSC-1 / High");
    	iacMap.put("6995", "VIVM-1 / Medium");
    	iacMap.put("7021", "ECSC-1 / High");
    	iacMap.put("7027", "VIVM-1 / Medium");
    	iacMap.put("7099", "VIVM-1 / Medium");
    	iacMap.put("7103", "VIVM-1 / Medium");
    	iacMap.put("7112", "VIVM-1 / Medium");
    	iacMap.put("7113", "VIVM-1 / Medium");
    	iacMap.put("7115", "VIVM-1 / Medium");
    	iacMap.put("7136", "VIVM-1 / Medium");
    	iacMap.put("7175", "DCSQ-1 / Medium");
    	iacMap.put("7200", "VIVM-1 / Medium");
    	iacMap.put("7236", "DCCT-1 / Medium");
    	iacMap.put("7240", "VIVM-1 / Medium");
    	iacMap.put("7245", "ECSC-1 / High");
    	iacMap.put("7248", "ECSC-1 / High");
    	iacMap.put("7249", "ECLP-1 / High, ECPA-1 / High");
    	iacMap.put("7250", "ECLP-1 / High, ECPA-1 / High");
    	iacMap.put("7254", "ECSC-1 / High");
    	iacMap.put("7255", "ECSC-1 / High");
    	iacMap.put("7257", "VIVM-1 / Medium");
    	iacMap.put("7282", "ECSC-1 / High");
    	iacMap.put("7284", "ECSC-1 / High");
    	iacMap.put("7285", "ECSC-1 / High");
    	iacMap.put("7286", "ECLP-1 / High");
    	iacMap.put("7292", "ECLP-1 / High");
    	iacMap.put("7295", "ECLP-1 / High");
    	iacMap.put("7296", "IAIA-2 / High (IAC is class only!!!)");
    	iacMap.put("7300", "ECSC-1 / High");
    	iacMap.put("7301", "ECSC-1 / High");
    	iacMap.put("7314", "VIVM-1 / Medium");
    	iacMap.put("7315", "VIVM-1 / Medium");
    	iacMap.put("7317", "VIVM-1 / Medium");
    	iacMap.put("7319", "VIVM-1 / Medium");
    	iacMap.put("7320", "VIVM-1 / Medium");
    	iacMap.put("7321", "VIVM-1 / Medium");
    	iacMap.put("7323", "VIVM-1 / Medium");
    	iacMap.put("7378", "ECSC-1 / High");
    	iacMap.put("7379", "ECSC-1 / High");
    	iacMap.put("7385", "ECSC-1 / High");
    	iacMap.put("7417", "ECLP-1 / High");
    	iacMap.put("7418", "ECSC-1 / High");
    	iacMap.put("7454", "VIVM-1 / Medium");
    	iacMap.put("7455", "VIVM-1 / Medium");
    	iacMap.put("7456", "VIVM-1 / Medium");
    	iacMap.put("7461", "VIVM-1 / Medium");
    	iacMap.put("7462", "VIVM-1 / Medium");
    	iacMap.put("7464", "VIVM-1 / Medium");
    	iacMap.put("7465", "VIVM-1 / Medium");
    	iacMap.put("7466", "VIVM-1 / Medium");
    	iacMap.put("7467", "VIVM-1 / Medium");
    	iacMap.put("7468", "VIVM-1 / Medium");
    	iacMap.put("7469", "VIVM-1 / Medium");
    	iacMap.put("7470", "VIVM-1 / Medium");
    	iacMap.put("7472", "VIVM-1 / Medium");
    	iacMap.put("7475", "VIVM-1 / Medium");
    	iacMap.put("7571", "VIVM-1 / Medium");
    	iacMap.put("7577", "ECSC-1 / High");
    	iacMap.put("7578", "ECSC-1 / High");
    	iacMap.put("7651", "ECSC-1 / High");
    	iacMap.put("7654", "ECSC-1 / High");
    	iacMap.put("7657", "ECSC-1 / High");
    	iacMap.put("7658", "VIVM-1 / Medium");
    	iacMap.put("7683", "ECSC-1 / High");
    	iacMap.put("7685", "VIVM-1 / Medium");
    	iacMap.put("7690", "DCCT-1 / Medium");
    	iacMap.put("7702", "VIVM-1 / Medium");
    	iacMap.put("7728", "VIVM-1 / Medium");
    	iacMap.put("7730", "VIVM-1 / Medium");
    	iacMap.put("7731", "VIVM-1 / Medium");
    	iacMap.put("7732", "VIVM-1 / Medium");
    	iacMap.put("7778", "VIVM-1 / Medium");
    	iacMap.put("7779", "VIVM-1 / Medium");
    	iacMap.put("7780", "VIVM-1 / Medium");
    	iacMap.put("7851", "VIVM-1 / Medium");
    	iacMap.put("7852", "VIVM-1 / Medium");
    	iacMap.put("7859", "VIVM-1 / Medium");
    	iacMap.put("7863", "VIVM-1 / Medium");
    	iacMap.put("7867", "ECSC-1 / High");
    	iacMap.put("7956", "VIVM-1 / Medium");
    	iacMap.put("7959", "VIVM-1 / Medium");
    	iacMap.put("7963", "ECSC-1 / High");
    	iacMap.put("8004", "VIVM-1 / Medium");
    	iacMap.put("8017", "VIVM-1 / Medium");
    	iacMap.put("8040", "VIVM-1 / Medium");
    	iacMap.put("8041", "VIVM-1 / Medium");
    	iacMap.put("8043", "VIVM-1 / Medium");
    	iacMap.put("8044", "VIVM-1 / Medium");
    	iacMap.put("8045", "VIVM-1 / Medium");
    	iacMap.put("8046", "VIVM-1 / Medium");
    	iacMap.put("8047", "VIVM-1 / Medium");
    	iacMap.put("8049", "VIVM-1 / Medium");
    	iacMap.put("8058", "VIVM-1 / Medium");
    	iacMap.put("8069", "VIVM-1 / Medium");
    	iacMap.put("8072", "ECSC-1 / High");
    	iacMap.put("8074", "VIVM-1 / Medium");
    	iacMap.put("8097", "VIVM-1 / Medium");
    	iacMap.put("8102", "VIVM-1 / Medium");
    	iacMap.put("8135", "ECSC-1 / High");
    	iacMap.put("9058", "ECSC-1 / High");
    	iacMap.put("9059", "VIVM-1 / Medium");
    	iacMap.put("9061", "VIVM-1 / Medium");
    	iacMap.put("9067", "VIVM-1 / Medium");
    	iacMap.put("9758", "DCSQ-1 / Medium");
    	iacMap.put("9759", "ECSC-1 / High");
    	iacMap.put("9760", "ECSC-1 / High");
    	iacMap.put("9789", "ECSC-1 / High");
    	iacMap.put("9792", "VIVM-1 / Medium");
    	iacMap.put("9796", "VIVM-1 / Medium");
    	iacMap.put("9801", "VIVM-1 / Medium");
    	iacMap.put("9808", "VIVM-1 / Medium");
    	iacMap.put("9810", "VIVM-1 / Medium");
    	iacMap.put("9813", "VIVM-1 / Medium");
    	iacMap.put("9815", "ECSC-1 / High");
    	iacMap.put("9829", "ECSC-1 / High");
    	iacMap.put("10413", "VIVM-1 / Medium");
    	iacMap.put("10417", "ECSC-1 / High");
    	iacMap.put("10418", "VIVM-1 / Medium");
    	iacMap.put("10422", "VIVM-1 / Medium");
    	iacMap.put("10465", "ECSC-1 / High");
    	iacMap.put("10466", "ECSC-1 / High");
    	iacMap.put("10526", "ECSC-1 / High");
    	iacMap.put("10535", "VIVM-1 / Medium");
    	iacMap.put("10537", "VIVM-1 / Medium");
    	iacMap.put("10538", "VIVM-1 / Medium");
    	iacMap.put("10539", "VIVM-1 / Medium");
    	iacMap.put("10540", "ECSC-1 / High");
    	iacMap.put("10548", "DCSQ-1 / Medium");
    	iacMap.put("10550", "VIVM-1 / Medium");
    	iacMap.put("10553", "VIVM-1 / Medium");
    	iacMap.put("10555", "VIVM-1 / Medium");
    	iacMap.put("10565", "VIVM-1 / Medium");
    	iacMap.put("10566", "VIVM-1 / Medium");
    	iacMap.put("10581", "VIVM-1 / Medium");
    	iacMap.put("10583", "VIVM-1 / Medium");
    	iacMap.put("10585", "VIVM-1 / Medium");
    }

}//end class body
