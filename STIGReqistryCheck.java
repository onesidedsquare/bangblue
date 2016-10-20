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
import java.io.InputStream;
import java.io.StringWriter;
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
 *designed for Windows 7
 */
public class STIGReqistryCheck extends JPanel
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
		
	//These 2 strings will always be in the outputs, these are to test to see which one is which
	//private static final String retinaHeaderTest = "NetBIOSName";
	private static final String scapHeaderTest = "Vuln ID";
	
	private CSVWriter writer;
	
	//retina static variables
	private static int[] columnLocations;
	
	private static List<String> columns = new ArrayList<String>();
	private static final String firstLine = "STIG#Vuln ID#STIG ID#Rule ID#Rule Title#Check Content#Key#type#value#altValue";	
	
	//private static final String REG_DWORD = "REG_DWORD";

    /**
     * 
     * @param location path in the registry
     * @param key registry key
     * @return registry value or null if not found
     */
    public static final String readRegistry(String location, String key){
        try {
            // Run reg query, then read output with StreamReader (internal class)
            Process process = Runtime.getRuntime().exec("reg query" + 
                    '"'+ location + "\" /v " + key);

            StreamReader reader = new StreamReader(process.getInputStream());
            reader.start();
            process.waitFor();
            reader.join();
            
        	
        	/*String regKey = location.substring(0,4);
        	String locationEdit = location.substring(5, location.length() - 1);
        	String value = "";
        	if (regKey.equals("HKLM"))
    		{
        		value= WinRegistry.readString (
        	    WinRegistry.HKEY_LOCAL_MACHINE,  //HKEY
        	    locationEdit,           		//Key
        	    key); 
        	}
        	
        	if (regKey.equals("HKCU"))
			{
				value = WinRegistry.readString (
	    	    WinRegistry.HKEY_CURRENT_USER,  //HKEY
	    	    locationEdit,           		//Key
	    	    key); 
			}
        	
        	return value;
        	*/
            //String output = value;
            
            String output = reader.getResult();

            // Parse out the value
            String[] parsed = output.split(" ");  //.split("\t") for windows XP
            String found = parsed[parsed.length-1];  //so far in all my examples the value is always the last thing soo...
           	return found;
        }
        catch (Exception e) {
        	e.printStackTrace();
        	return "error";
        }

    }

    static class StreamReader extends Thread {
        private InputStream inputStream;
        private StringWriter stringWriter= new StringWriter();

        public StreamReader(InputStream is) {
            this.inputStream = is;
        }

        public void run() {
            try {
                int c;
                while ((c = inputStream.read()) != -1)
                    stringWriter.write(c);
            }
            catch (IOException e) { 
            	e.printStackTrace();
            }
        }
        
        public String getResult() {
            return stringWriter.toString();
        }
        
    }
    
	private STIGReqistryCheck() {
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
	        Label systemDescriptorLabel = new Label("File Name:");
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
    
    public void actionPerformed(ActionEvent actionEvent) {
    	try {		
    		//this handles the EXECUTEBUTTON action,
	        if (actionEvent.getSource() == executeButton) {
	        	//ensure some files are selected
	        	if (fileChooser.getSelectedFiles().length > 0) {
		            parser(fileChooser.getSelectedFiles());
	        	} else {
        			JOptionPane.showMessageDialog(this, "Error, No Files Selected", "STIG Registry Check", JOptionPane.ERROR_MESSAGE);
        		}
	        	
	        } //if (actionEvent.getSource() == executeButton) { 
	        
	        //this handles the file selection and changes the button name to the file name
	        if(actionEvent.getSource() == selectButton){
	        	//open the file choose dialog box
	        	fileChooser.setMultiSelectionEnabled(true);
	        	
	        	//set Cursor to wait mode
	        	setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	        	
	        	int returnVal = fileChooser.showOpenDialog(STIGReqistryCheck.this);
	        	
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
	        
	        if(actionEvent.getSource() == clearButton){
	        	fileChooser.cancelSelection();
	        	filesSelected.removeAll();
	        }

    	 } catch (Exception e) {
				e.printStackTrace();
		 } 
    } //end actionPerformed
    /**
     * parser
     * 
     * @var file
     * file should be the current file that needs to be parsed
     * 
     *This takes the file and loads it into the reader for line by line parsing 
     * */
    
    public void parser(File[] file){
    	try {
    		fillColumns();
    		
    		File currentFile;
    		String poamDescriptorString = poamDescriptorTextbox.getText();
    		
    		//DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmm");
    		//Calendar cal = Calendar.getInstance();
    		//String formatedCal = dateFormat.format(cal.getTime());

            
            writer = new CSVWriter(new FileWriter(poamDescriptorString + "_RESULTS.csv"), ',');
            
            //put first line of headers in order
            writer.writeNext(firstLine.split(split));
            
			//this is for loop'n multiple files
            int counter = 0;
             
            while ( counter < file.length  && (currentFile = file[counter].getCanonicalFile()) != null){
                       	
	            CSVReader reader = new CSVReader(new FileReader(currentFile));

				String[] firstLine = reader.readNext();
				
				if(stringContainsItemFromList(scapHeaderTest, firstLine)){
					loadColumns(columns, firstLine);
					String[] nextLine;
			    	String lineEntry;
		    	
					while ((nextLine = reader.readNext()) != null) {
						String value = "", finding = "", match="diff";
						value =  nextLine[columnLocations[8]];
						
						//this right here is the real meat of what this program does
						finding = readRegistry(nextLine[columnLocations[5]], nextLine[columnLocations[6]]);
	
						if (finding.contains(value)){
							match = "same";
						}
						
						if (finding.equals("") && value.equals("(blank)")){
							match = "same";
						}
						
						if (finding.equals("") && value.equals("null")){
							match = "same";
						}
					    		 
				    	lineEntry = nextLine[columnLocations[0]] + split + 
				    		 nextLine[columnLocations[1]] + split +
				    		 nextLine[columnLocations[2]] + split +
				    		 nextLine[columnLocations[3]] + split +
				    		 nextLine[columnLocations[4]] + split +
				    		 nextLine[columnLocations[5]] + split +
				    		 nextLine[columnLocations[6]] + split +
				    		 nextLine[columnLocations[7]] + split +
				    		 nextLine[columnLocations[8]] + split +
				    		 finding + split + match;
					   
					    // feed poamEntry in array
					    writer.writeNext(lineEntry.split(split));
						    
					}//while ((nextLine = reader.readNext()) != null) {

	        	}//if(stringContainsItemFromList(scapHeaderTest, firstLine)){
							
		        counter++; //incrimate the counter
			    reader.close();
			    
	        } //end  while ((currentFile = file[x].getCanonicalFile()) != null){
            JOptionPane.showMessageDialog(this, "Success", "STIG Registry Check", JOptionPane.PLAIN_MESSAGE);
		    writer.close();
		    
		} catch (Exception e) {
			e.printStackTrace();
		}

    } //  public void parser(File[] file){
    
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
		columns.add("Key");//6
		columns.add("Type");//7
		columns.add("Value");//8
		columns.add("Found");//9
	}
    
	/**
     * stringContainsItemFromList (String inputString, String[] firstLine)
     * @info This is used to see if file is a retina for scap file by testing the first line column headers
     * 
     * @var inputString is your Retina or SCAP header
     * @var firstLine is the first line of the CSV you want to parse
     * 
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
    	columnLocations = new int[] {0,0,0,0,0,0,0,0,0,0};
    	//load columns with file column locations
    	List<String> fl = Arrays.asList(firstLine);
    	int x = 0;
    	for(String s:columns){
		   columnLocations[x] =fl.indexOf(s);
		   x++;
		}
    }
    
   
    private static void createAndShowGUI() {
        //Create and set up the window.
    	JFrame.setDefaultLookAndFeelDecorated(true);
        JFrame frame = new JFrame("File Chooser");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 300);
        
        //Add content to the window.
        frame.add(new STIGReqistryCheck());
 
        //Display the window.
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

    } // end createAndShowGUI   
    
}