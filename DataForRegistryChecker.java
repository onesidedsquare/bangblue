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
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * POAM Builder, for SCAP/Retina CSV file parsing to PMMI POAM format
 * 
 * @author rogerssa
 * @version 0.6 BETA
 * 
 * 
 * @Info
 * 
 */
public class DataForRegistryChecker extends JPanel
			implements ActionListener {

	private static final long serialVersionUID = -425666944415786622L;
	//GUI Vars
	private JButton selectButton;
	private JButton executeButton;
	private JButton clearButton;
	private JTextField poamDescriptorTextbox;
	private JTextArea filesSelected;
	private JFileChooser fileChooser;
	
	//These are used in building the new POAM
	private static final String newline = "\n";
	private static final String split = "#";

	//Data varibles used in the file
	//private static final DateFormat df = new SimpleDateFormat("yyyyMMMdd");;
	//private String formattedDate;
	
	//private File[] file; turns out global file isn't such a good idea...
	
	private CSVWriter writer;
	
	//retina static variables
	//private static final String[] retinaColumns = new String[] {"Name", "Description", "SevCode", "AuditID", "Date", "FixInformation"};
	//private static final String[] scapColumns  = new String[] {"Rule Title","Discussion", "Severity", "IA Controls", "STIG ID", "Fix Text", "STIG Status"};
	private static int[] columnLocations;
	
	private static List<String> columns = new ArrayList<String>();
	private static final String poamFirstLine = "STIG#Vuln ID#STIG ID#Rule ID#Rule Title#Check Content#Key#Type#Value#Found";	
	
	/**
	 * @param args
	 * 
	 * You know for a main method, it doesn't do shit except call up GUI
	 */
	public static void main(String[] args) {
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
	 * 
	 */
	private DataForRegistryChecker() {
        super(new BorderLayout());
 
        try {
       	
	        //This is the log file where the debug output goes to.
        	filesSelected = new JTextArea(5,20);
        	filesSelected.setMargin(new Insets(10,10,10,10));
        	filesSelected.setEditable(false);
        	filesSelected.setAutoscrolls(true);
	        
        	JScrollPane filesSelectedScrollPane = new JScrollPane(filesSelected);
	 
	        //Create a file chooser
	        fileChooser = new JFileChooser();
	        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files", "csv");
	        fileChooser.setFileFilter(filter);
	
	        //button that activates the file selection option
	        selectButton = new JButton("Select File");
	        selectButton.addActionListener(this);
	        
	        //this label/textbox is for the system description 
	        Label systemDescriptorLabel = new Label("Name:");
	        poamDescriptorTextbox = new JTextField(10);	 
	        
	        JPanel inputPanel = new JPanel(); //use FlowLayout
	        	inputPanel.add(selectButton);
	        	inputPanel.add(systemDescriptorLabel);
	        	inputPanel.add(poamDescriptorTextbox);
	        	
	        
	        JPanel executePanel = new JPanel();
	        	executeButton = new JButton("Execute");
	        	executeButton.addActionListener(this);
	        	executePanel.add(executeButton);
	        	clearButton = new JButton("Clear");
	        	clearButton.addActionListener(this);
	        	executePanel.add(clearButton);

	        //build panel
	        JPanel rootPanel = new JPanel(new GridLayout(3, 1));
		        rootPanel.add(inputPanel);
		        rootPanel.add(executePanel);		    
		    
	        //add the buttons and the log to this panel.
	        add(rootPanel, BorderLayout.PAGE_START);
	        add(filesSelectedScrollPane, BorderLayout.CENTER);
	        
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
        			JOptionPane.showMessageDialog(this, "Error, No Files Selected", "Data Creator", JOptionPane.ERROR_MESSAGE);
        		}
	        	
	        } //if (actionEvent.getSource() == executeButton) { 
	        
	        //this handles the file selection and changes the button name to the file name
	        if(actionEvent.getSource() == selectButton){
	        	//open the file choose dialog box
	        	fileChooser.setMultiSelectionEnabled(true);
	        	
	        	//set Cursor to wait mode
	        	setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	        	
	        	int returnVal = fileChooser.showOpenDialog(DataForRegistryChecker.this);
	        	
	        	//if they've selected a file
	        	if (returnVal == JFileChooser.APPROVE_OPTION) { 
	        		//once you select file change button name to file name to confirm selection
	        		File[] file = fileChooser.getSelectedFiles();   
	        		
        			for (int counter = 0; counter < file.length; counter++ ){
        				//display selected files in the text box so user can confirm selection
        				filesSelected.append(file[counter].getName() + newline);
        			} //while (counter < file.length )
	        	} //if (returnVal == JFileChooser.APPROVE_OPTION) 
	        	
	        	//release Cursor
	        	setCursor(null);
	        	
	        } //if(actionEvent.getSource() == selectButton)
	        
	        if(actionEvent.getSource() == clearButton){
	        	fileChooser.cancelSelection();
	        	filesSelected.removeAll();
	        }
	        
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
        frame.add(new DataForRegistryChecker());
 
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
    public void parser(File[] file){
    	try {
    		fillColumns();
    		
    		File currentFile;
    		String dataDescriptorString = poamDescriptorTextbox.getText();
            
            writer = new CSVWriter(new FileWriter(dataDescriptorString + "_scanData.csv"), ',');
            
            //put first line of poam headers in
            writer.writeNext(poamFirstLine.split(split));
            
			//this is for loop'n multiple files
            int counter = 0;
            
            //formattedDate = df.format(new Date()); 
            
            while ( counter < file.length  && (currentFile = file[counter].getCanonicalFile()) != null){          	
	            CSVReader reader = new CSVReader(new FileReader(currentFile));

				String[] firstLine = reader.readNext();
				
				loadColumns(columns, firstLine);
				entry(reader);
			
		        counter++; //incrimate the counter
			    reader.close();
			    
	        } //end  while ((currentFile = file[x].getCanonicalFile()) != null){
            JOptionPane.showMessageDialog(this, "Success", "Data Creator", JOptionPane.PLAIN_MESSAGE);
		    writer.close();
		    
		} catch (Exception e) {
			e.printStackTrace();
		}

    } // end parser
    
    
    /**
     * loadColumns
     * 
     * Loads retina and scap columns into the list so they can be used in loadColumns()
     * */
    private void fillColumns() {
		columns.add("STIG"); //0
		columns.add("Vuln ID");//1
		columns.add("STIG ID");//2
		columns.add("Rule ID");//3
		columns.add("Rule Title");//4
		columns.add("Check Content");//5
		//columns.add("Key");//6
		//columns.add("Type");//7
		//columns.add("Value");//8
		//columns.add("Found");//9

	}

	/** 
     * stringContainsItemFromList (String inputString, String[] firstLine)
     * @info This is used to see if file is a retina for scap file by testing the first line column headers
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
    }
    
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
     * This takes the given SCAP or Retina arrays and finds them in the first line of the file
     * 
     * */
    public static void loadColumns(List<String> columns, String[] firstLine){
    	//reset column locations
    	columnLocations = new int[] {0,0,0,0,0,0,0,0,0};  //There's got to be a better way than to hardcode this
    	//load columns with file column locations
    	List<String> fl = Arrays.asList(firstLine);
    	int x = 0;
    	for(String s:columns){
		   columnLocations[x] =fl.indexOf(s);
		   x++;
		}
    }
    
    /**
     * entry
     * 
     * parser() calls this method to handle SCAP files
     * 
     * @var CSVReader reader, 
     * the current loaded file from filechooser 
     *  
     * @var String systemName, 
     * a parsed system name that came from the filename
     * */
    private void entry(CSVReader reader){
    	String[] nextLine;
    	String lineEntry = "";
    	
    	try {
			while ((nextLine = reader.readNext()) != null) {
				String stigName = nextLine[columnLocations[0]];
				String checkContent = nextLine[columnLocations[5]];
				Boolean twoLines = false;
				String lineTwo[] = null;
				
				//these are for windwos server 2008
				if(stigName.equals("Windows Server 2008 R2 Member Server Security Technical Implementation Guide")){
					if(checkContent.contains("HKCU")){
						int n = checkContent.indexOf("HKCU");
						checkContent = checkContent.substring(n, checkContent.length());
					}
				    
					if(checkContent.contains("HKLM")){
						int n = checkContent.indexOf("HKLM");
						checkContent = checkContent.substring(n, checkContent.length());
						//checkContent = checkContent.replace("*HKLM", "HKLM");
					}
					if(checkContent.contains("Value Name: ")){
						checkContent = checkContent.replace("Value Name: ", "#");
					}
					
					if(checkContent.contains("Type: ")){
						checkContent = checkContent.replace("Type: ", "#");
					}
					
					if(checkContent.contains("Value: ")){
						checkContent = checkContent.replace("Value: ", "#");
					}
					
					if(checkContent.contains("Warning:")){
						int n = checkContent.indexOf("Warning:");
						checkContent = checkContent.substring(0, n);
					}
					if(checkContent.contains("Note:")){
						int n = checkContent.indexOf("Note:");
						checkContent = checkContent.substring(0, n);
					}
					 
					if(checkContent.contains("Documentable:")){
						int n = checkContent.indexOf("Documentable:");
						checkContent = checkContent.substring(0, n);
					}
				}
				
				//these are for the office stigs
				if(stigName.equals("OFFICE")){
					if(checkContent.contains("HKCU")){
						int n = checkContent.indexOf("HKCU");
						checkContent = checkContent.substring(n, checkContent.length());
					}
				    
					if(checkContent.contains("HKLM")){
						int n = checkContent.indexOf("HKLM");
						checkContent = checkContent.substring(n, checkContent.length());
						//checkContent = checkContent.replace("*HKLM", "HKLM");
					}
					
					if(checkContent.contains(", this is not a finding.")){
						checkContent = checkContent.replace(", this is not a finding.", "");
					}
					
					if(checkContent.contains(" is ")){
						checkContent = checkContent.replace(" is ", "#");
					}
					
					if(checkContent.contains(" = ")){
						checkContent = checkContent.replace(" = ", "#");
					}
					
					if(checkContent.contains("Criteria: If the value ")){
						checkContent = checkContent.replace("Criteria: If the value ", "#");
					}
					
					if(checkContent.contains("AND")){
						twoLines = true;
						lineTwo = checkContent.split("AND");
						checkContent = checkContent.substring(0, checkContent.indexOf("AND"));
					}
				
				}
				
				String[] content = checkContent.split(split);
				
			    if (content.length >= 4){
			    		 
			    	lineEntry = nextLine[columnLocations[0]] + split + 
			    		 nextLine[columnLocations[1]] + split +
			    		 nextLine[columnLocations[2]] + split +
			    		 nextLine[columnLocations[3]] + split +
			    		 nextLine[columnLocations[4]] + split +
			    		 content[0] + split +
			    		 content[1] + split +
			    		 content[2] + split +
			    		 content[3];
			    	
			    	if(twoLines){  //this ain't a pretty solution but it works..60% of the time it works 100% of the time..so far this only works with office stigs
			    		writer.writeNext(lineEntry.split(split));
			    		
			    		content = lineTwo[1].split(split);
				    	lineEntry = nextLine[columnLocations[0]] + split + 
					    		 nextLine[columnLocations[1]] + split +
					    		 nextLine[columnLocations[2]] + split +
					    		 nextLine[columnLocations[3]] + split +
					    		 nextLine[columnLocations[4]] + split +
					    		 content[0] + split +
					    		 content[1] + split +
					    		 content[2] + split +
					    		 content[3];
			    	}
			    	
			    } else {
			    	lineEntry = nextLine[columnLocations[0]] + split + 
			    			nextLine[columnLocations[1]] + split +
			    			nextLine[columnLocations[3]] + split +
				    		nextLine[columnLocations[4]] + split + split + "ERROR" + split + "ERROR" + split + "ERROR" + split + "ERROR";
			    }

			   
			    // feed poamEntry in array
			    writer.writeNext(lineEntry.split(split));
				    
			}//while ((nextLine = reader.readNext()) != null) {
		} catch (IOException e) {
			e.printStackTrace();
		}
    }//entry


}//end class body
