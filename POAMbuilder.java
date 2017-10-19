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

import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import net.sourceforge.jdatepicker.JDateComponentFactory;
import net.sourceforge.jdatepicker.JDatePicker;

/**
 * POAM Builder, for SCAP/Retina CSV file parsing to PMMI POAM format
 * 
 * @author rogerssa
 * @version 0.9 BETA
 * 
 * 
 * @Info
 * 
 */
public class POAMBuilder extends JPanel
			implements ActionListener {

	private static final long serialVersionUID = -425666944415786622L;
	//GUI Vars
	private JButton selectButton;
	private JButton executeButton;
	private JButton clearButton;
	private JFormattedTextField poamDescriptorTextbox;
	private JFormattedTextField dateOfScanTextbox;
	private JCheckBox poamCheckBox;
	private JCheckBox rarCheckBox;
	private JCheckBox controlsCheckBox;
	private JCheckBox countsCheckBox;
	private JTextArea filesSelected;
	private JFileChooser fileChooser;
	private JDatePicker datePicker;
	
	private JScrollPane filesSelectedScrollPane;
	
	String[] impacts = new String[] {"High", "Medium", "Low"};
	String[] likelihoods = new String[] {"High", "Moderate", "Low"};
	private JComboBox<String> comboBoxLikelihood;
	private JComboBox<String> comboBoxImpact;
	
	//These are used in building the new POAM
	static final String newline = "\n";
	static final String split = "###";

	//These 2 strings will always be in the outputs, these are to test to see which one is which
	//private static final String retinaHeaderTest = "NetBIOSName";
	private static final String scapHeaderTest = "STIG ID";
	private static final String acasHeaderTest = "Plugin";
	
	private static int[] columnLocations;
	private List<String> stigNames = new ArrayList<String>();
	private List<String> iaControls = new ArrayList<String>();
	//private static final String poamFirstLine = "Weakness#CAT#IA Control and Impact Code#POC#Resources Required#Scheduled Completion Date"
	//		+ "#Milestones with Completion Dates#Milestone Changes#Source Identifying Weakness#Status#Comments#System"; //#Severity
	//private static final String newPOAMFirstLine = "Vuln ID#STIG ID#Name#Description#Raw Risk#IA Control#Source Identifying Weakness"
	//		+ "#Technology Area#Program#Affected System#IP Address#Finding Details/Tool Results#Fix Procedures (From Tool)#Point of Contact#Engineer Status#Engineer Comments"
	//		+ "#Remediation Steps#Mitigation#CAT (Residual Risk)#Status (Final/Official)#Validator Comments#Documentation Status#Scan Date#Date Last Updated#IAU";
	private static final String emassPOAMFirstLine = "###Control Vulnerablity Description###Security Control Number###Office Organization###Security Checks###Raw Security Value###Mitigation"
			+ "###Severity Value###Resources Required###Scheduled Completion Date###Milestone with Completion Date###Milestone Changes###Source Idenifitying Weakness###Status###Comments###Affected Systems###Check Content"
			+ "###Fix Text";
	private static final String rarFirstLine = "Identifier Applicable Security Control (1)###Source of Discovery or Test Tool Name (2)###Test ID or Threat IDs (3)###Description of Vulnerability/Weakness (4)"
			+ "###Raw Risk (CAT I, II, III) (5)###Impact (6)###Likelihood (7)###Mitigation Description (10)###Remediation Description (11)###Residual Risk/Risk Exposure (12)###Status (13)"
			+ "###Comment (14)###Devices Affected (15)###Check Content###Fix Text";
	private static final String controlsFirstLine = "Control###Title###Impact###Priority###Subject Area";
	//hardcodedgoodness aww yis

	private static final String status = "OnGoing";
	private static final String resourcesrequired = "Time and Manpower";
	private static final String poc = "Project ISSO";
	static String cat1scheduledcompletiondate = "";
	static String cat2scheduledcompletiondate = "";
	static String cat3scheduledcompletiondate = "";

	//this actually holds all the entries and is written to file
	static Map<String, FindingEntry> entryMap = new HashMap<String, FindingEntry>();
	
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
	 * @param programNameTextbox 
	 * 
	 */
	private POAMBuilder() {
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
	        FileNameExtensionFilter csvfilter = new FileNameExtensionFilter("CSV Files", "csv");
	        FileNameExtensionFilter xmlFilter = new FileNameExtensionFilter("CKL Files", "ckl");
	        FileNameExtensionFilter nessusFilter = new FileNameExtensionFilter("NESSUS Files", "nessus");
	        fileChooser.setFileFilter(xmlFilter);
	        fileChooser.setFileFilter(csvfilter);
	        fileChooser.setFileFilter(nessusFilter);
	
	        //button that activates the file selection option
	        selectButton = new JButton("Select File(s)");
	        selectButton.addActionListener(this);
	        selectButton.setToolTipText("Select all the files you want at once");
	        
	        //this label/textbox is for the system description 
	        Label poamLabel = new Label("POAM Name:");
	        poamDescriptorTextbox = new JFormattedTextField();	
	        poamDescriptorTextbox.setColumns(10);
	        poamDescriptorTextbox.setToolTipText("Set POAM/RAR Name here");
	        
	        Label dateOfScanLabel = new Label("Date of Scan");
	        dateOfScanTextbox = new JFormattedTextField(new Date());
	        dateOfScanTextbox.setColumns(10);
	        dateOfScanTextbox.setToolTipText("Input date of scans, expected format YYYYMMMDD");
	        
	        Label impactLabel = new Label("Impact");
            comboBoxImpact = new JComboBox<String>(impacts);
            comboBoxImpact.setToolTipText("Select base RAR Impact");
            Label likelihoodLabel = new Label("Likelihood");
	        comboBoxLikelihood = new JComboBox<String>(likelihoods);
	        comboBoxLikelihood.setToolTipText("Select base RAR Likelihood");
	        
	        poamCheckBox = new JCheckBox("POAM");
	        poamCheckBox.setSelected(false);
	        
	        rarCheckBox = new JCheckBox("RAR");
	        rarCheckBox.setSelected(false);
	        
	        controlsCheckBox = new JCheckBox("Controls");
	        controlsCheckBox.setSelected(false);
	        
	        countsCheckBox = new JCheckBox("Counts");
	        countsCheckBox.setSelected(true);

	        
	        JPanel inputPanel = new JPanel(); //use FlowLayout
	        //inputPanel.add(selectButton);
	        inputPanel.add(poamLabel);
	        inputPanel.add(poamDescriptorTextbox);
	        	
	        JPanel secoundaryInputPanel = new JPanel();
            secoundaryInputPanel.add(dateOfScanLabel);

            datePicker = JDateComponentFactory.createJDatePicker();
            secoundaryInputPanel.add((JComponent) datePicker);
            
            JPanel thirdInputPanel = new JPanel();
            thirdInputPanel.add(impactLabel);
            thirdInputPanel.add(comboBoxImpact);
            thirdInputPanel.add(likelihoodLabel);
            thirdInputPanel.add(comboBoxLikelihood);
            
            JPanel forthinputPanel = new JPanel();
            forthinputPanel.add(selectButton);
            
            JPanel fifthinputpanel = new JPanel();
            fifthinputpanel.add(poamCheckBox);
            fifthinputpanel.add(rarCheckBox);
            fifthinputpanel.add(controlsCheckBox);
            fifthinputpanel.add(countsCheckBox);
            
	        JPanel executePanel = new JPanel();
	        executeButton = new JButton("Execute");
	        executeButton.addActionListener(this);
	        executePanel.add(executeButton);
	        //clearButton = new JButton("Clear");
	        //clearButton.addActionListener(this);
	        //executePanel.add(clearButton);
	        	
	        //build panel
	        JPanel rootPanel = new JPanel(new GridLayout(5, 1));
		    rootPanel.add(inputPanel);
		    rootPanel.add(secoundaryInputPanel);  
		    rootPanel.add(thirdInputPanel);
		    rootPanel.add(fifthinputpanel);
		    rootPanel.add(forthinputPanel);
		    
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
	 * Does the button things
	 * 
	 */
    public void actionPerformed(ActionEvent actionEvent) {
    	try {		
    		//this handles the EXECUTEBUTTON action,
	        if (actionEvent.getSource() == executeButton) {
	        	//ensure some files are selected
	        	setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	        	int cnt = fileChooser.getSelectedFiles().length;
	        	if (cnt > 0) {
		            parser(fileChooser.getSelectedFiles());
	        	} else {
        			JOptionPane.showMessageDialog(this, "Error, No Files Selected", "POAM Maker", JOptionPane.ERROR_MESSAGE);
        		}
	        	setCursor(null);
	        } //if (actionEvent.getSource() == executeButton) { 
	        
	        //this handles the file selection and changes the button name to the file name
	        if(actionEvent.getSource() == selectButton){
	        	//open the file choose dialog box
	        	fileChooser.setMultiSelectionEnabled(true);	        	
	        	//set Cursor to wait mode
	        	setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	        	
	        	int returnVal = fileChooser.showOpenDialog(POAMBuilder.this);
	        	
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
        JFrame frame = new JFrame("POAM - RAR Builder");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 300);
        
        //Add content to the window.
        frame.add(new POAMBuilder());
 
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
    public void parser(File[] files) {
    	try {
    		setDate();
    		POAMBuilderData.load();
    		String poamDescriptorString = poamDescriptorTextbox.getText();           
    		for(File file : files) {
            	String fileName = file.getPath();
            	if(fileName.endsWith(".csv")){
            		//System.out.println("CSV");
            		CSVReader reader = new CSVReader(new FileReader(file));
    				String[] firstLine = reader.readNext();//this allows us to figure what kind of file it is
    				
    				if(POAMBuilderTools.stringContainsItemFromList(scapHeaderTest, firstLine)){
    					loadColumns(POAMBuilderData.scapColumns, firstLine);
    					scapEntry(reader);
    	        	}//IF SCAP
    				   				
    				if(POAMBuilderTools.stringContainsItemFromList(acasHeaderTest, firstLine)){
    					loadColumns(POAMBuilderData.acasColumns, firstLine);
    					acasEntry(reader);
    				}//IF ACAS
    				
    			    reader.close();
            	}// end if(fileName.endsWith(".csv")){
            	
            	if(fileName.endsWith(".ckl")){
            		//only SCAP files so no check for ACAS, BECAUSE ACAS IS TERRIBLE
            		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        		    DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        		    Document document = docBuilder.parse(file);
        		    cklParser(document);
            	}//if(fileName.endsWith(".ckl")){
            	
            	if(fileName.endsWith(".nessus")) {
            		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        		    DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        		    Document document = docBuilder.parse(file);
        		    nessusParser(document);
            	}//if(fileName.endsWith(".nessus")) {
            }//end for(File file : files) {
    		
    		if(poamCheckBox.isSelected()){
    		
	            CSVWriter poamWriter = new CSVWriter(new FileWriter(poamDescriptorString + "_POAM.csv"), ',');
	            poamWriter.writeNext(emassPOAMFirstLine.split(split));
	            
	            //this part takes the entryMap and puts it to paper!
	            int cnt = 1;
				Iterator<Entry<String, FindingEntry>> entries = entryMap.entrySet().iterator();
				while (entries.hasNext()) {
				  Entry<String, FindingEntry> thisEntry = (Entry<String, FindingEntry>) entries.next();
				  FindingEntry entry = (FindingEntry) thisEntry.getValue();
				 
				  String poamline;  // = id + value.getFindingID();
				  poamline = cnt + split;
				  poamline = poamline + entry.getFindingID() + " : "+ entry.getDescription();
				  poamline = poamline + split + entry.getControl();
				  poamline = poamline + split + poc;
				  poamline = poamline + split + entry.getFindingID();
				  poamline = poamline + split + entry.getCat();
				  poamline = poamline + split;
				  poamline = poamline + split + entry.getLevel();
				  poamline = poamline + split + resourcesrequired;
				  poamline = poamline + split + entry.getScheduledcompletiondate();
				  poamline = poamline + split + entry.getScheduledcompletiondate();
				  poamline = poamline + split;
				  poamline = poamline + split + entry.getFindingSource();
				  poamline = poamline + split + status;
				  poamline = poamline + split + entry.getFindingDetail();
				  poamline = poamline + split + entry.getSystemName();
				  poamline = poamline + split + entry.getCheckContent();
				  poamline = poamline + split + entry.getFixText();
				  
				  poamline = poamline.replaceAll("\"", " ");
				  poamline = poamline.replaceAll(",", " ");
	
				  poamWriter.writeNext(poamline.split(split));
				  cnt++;  //leave this here, it's for POAM line numbers
				}//while (entries.hasNext()) {
            
				poamWriter.close();
	            JOptionPane.showMessageDialog(this, "POAM Created", "Success", JOptionPane.PLAIN_MESSAGE);
    		}//if(poamCheckBox.isSelected()){
            
    		if(rarCheckBox.isSelected()){
	            CSVWriter rarWriter = new CSVWriter(new FileWriter(poamDescriptorString + "_RAR.csv"), ',');	            
	            //put first line of RAR headers in
	            rarWriter.writeNext(rarFirstLine.split(split));

				Iterator<Entry<String, FindingEntry>> entries = entryMap.entrySet().iterator();
				while (entries.hasNext()) {
				  Entry<String, FindingEntry> thisEntry = (Entry<String, FindingEntry>) entries.next();
				  FindingEntry entry = (FindingEntry) thisEntry.getValue();
	          
				  String rarline = "";  // = id + value.getFindingID();
				  rarline = rarline + entry.getControl() + split;
				  rarline = rarline + entry.getFindingSource() + split;
				  rarline = rarline + entry.getFindingID() + split;
				  rarline = rarline + entry.getFindingID() + " : " + entry.getDescription() + split;
				  rarline = rarline + entry.getCat() + split;
				  rarline = rarline + (String) comboBoxImpact.getSelectedItem() + split;
				  rarline = rarline + (String) comboBoxLikelihood.getSelectedItem() + split;
				  rarline = rarline + split + split;
				  rarline = rarline + split + status;
				  rarline = rarline + split + entry.getFindingDetail();
				  rarline = rarline + split + entry.getSystemName();
				  rarline = rarline + split + entry.getCheckContent();
				  rarline = rarline + split + entry.getFixText();
				  
				  rarline = rarline.replaceAll("\"", " ");
				  rarline = rarline.replaceAll(",", " ");
				  
				  rarWriter.writeNext(rarline.split(split));
				}
				
			 rarWriter.close();
	         JOptionPane.showMessageDialog(this, "RAR Created", "Success", JOptionPane.PLAIN_MESSAGE);
		            
    		}//if(rarCheckBox.isSelected()){
    		
    		if(controlsCheckBox.isSelected()){
				
    			Iterator<Entry<String, FindingEntry>> entries = entryMap.entrySet().iterator();
    			while (entries.hasNext()) {
				  Entry<String, FindingEntry> thisEntry = (Entry<String, FindingEntry>) entries.next();
				  FindingEntry entry = (FindingEntry) thisEntry.getValue();
    		
				  String iac = entry.getControl().trim();
				  String[] iacarray = iac.split(newline);
				  for(int n = 0; n < iacarray.length; n++){
					  if(!iaControls.contains(iacarray[n].trim())){
						  iaControls.add(iacarray[n].trim());
					  }
				  }// for(int n = 0; n < iacarray.length; n++){
				}//while (entries.hasNext()) {
				
				CSVWriter controlsWriter = new CSVWriter(new FileWriter(poamDescriptorString + "_CONTROLS.csv"), ',');
				controlsWriter.writeNext(controlsFirstLine.split(split));
	            for (int c = 0; c < iaControls.size(); c++){
	            	//System.out.println(iaControls.get(c));
	            	String name = iaControls.get(c);
	            	String control[] = iaControls.get(c).split(" ");
	            	String conData = POAMBuilderData.rmfData.get(control[0]);

	            	String allTogether = name + split + conData;
	            	
	            	controlsWriter.writeNext(allTogether.split(split));
	            }//for (int c = 0; c < iaControls.size(); c++){
	            
	            controlsWriter.close();
	            
	            JOptionPane.showMessageDialog(this, "Contorls List Created", "Success", JOptionPane.PLAIN_MESSAGE);
			} // if(controlsCheckBox.isSelected()){
			
			
    		if(countsCheckBox.isSelected()){
 				Iterator<Entry<String, FindingEntry>> entries = entryMap.entrySet().iterator();
 				Map<String, Counts> fc = new HashMap<String, Counts>(); 				 				
 				int totalcat1 = 0;
 				int totalcat2 = 0;
 				int totalcat3 = 0;
 				
 				while (entries.hasNext()) {
 					Entry<String, FindingEntry> thisEntry = (Entry<String, FindingEntry>) entries.next();
 					FindingEntry entry = (FindingEntry) thisEntry.getValue();
	    			//if(!stigNames.contains(entry.getFindingSource())){
	    			//	stigNames.add(entry.getFindingSource());
	   			  	//}

	    			String cat = entry.getCat();
	    			if(cat.equalsIgnoreCase("I")) {
	    				totalcat1++;
	    			}else if(cat.equalsIgnoreCase("II")) {
	    				totalcat2++;
	    			} else if(cat.equalsIgnoreCase("III")) {
	    				totalcat3++;
	    			}
	    			
	    			String sourcename = entry.getFindingSource();
	    				    			   			
	    			if(fc.get(sourcename) == null) {
	    				Counts counts = new Counts(); 
	    				counts.setSourceName(sourcename);
	    				if(cat.equalsIgnoreCase("I")) {			
		    				counts.settotalCAT1(1);
		    			}else if(cat.equalsIgnoreCase("II")) {
		    				counts.settotalCAT2(1);
		    			} else if(cat.equalsIgnoreCase("III")) {
		    				counts.settotalCAT3(1);
		    			}
	    				
	    				fc.put(sourcename, counts);
	    				
	    			} else {
	    				Counts counts = fc.get(sourcename);
	    				
	    				if(cat.equalsIgnoreCase("I")) {
	    					int c = counts.gettotalCAT1();
		    				counts.settotalCAT1(c++);
		    			}else if(cat.equalsIgnoreCase("II")) {
		    				int c = counts.gettotalCAT2();
		    				counts.settotalCAT2(c++);
		    			} else if(cat.equalsIgnoreCase("III")) {
		    				int c = counts.gettotalCAT3();
		    				counts.settotalCAT3(c++);
		    			}
	    				fc.remove(sourcename);
	    				fc.put(sourcename, counts);
	    			}
 				}//while (entries.hasNext()) {
 							
	            CSVWriter stigWriter = new CSVWriter(new FileWriter(poamDescriptorString + "_STIG.csv"), ',');        
	            Iterator<Entry<String, Counts>> totalCounts = fc.entrySet().iterator();
	            
	            String line = "";
	            String lineTitles = "Source" + split + "CAT 1" + split + "CAT 2" + split + "CAT 3";
	            stigWriter.writeNext(lineTitles.split(split));
	            
	            while (totalCounts.hasNext()) {
 					Entry<String, Counts> thisEntry = (Entry<String, Counts>) totalCounts.next();
 					Counts entry = (Counts) thisEntry.getValue();
 					
 					line = entry.getSourceName() + split + entry.gettotalCAT1() + split +entry.gettotalCAT2() + split +entry.gettotalCAT3();
 					stigWriter.writeNext(line.split(split));
	            }
	            
	            String totalcats = "Totals:" + split + totalcat1 + split + totalcat2 + split +  totalcat3;

	            stigWriter.writeNext(totalcats.split(split));
	            stigWriter.close();
	            	            
	            JOptionPane.showMessageDialog(this, "STIGs List Created", "Success", JOptionPane.PLAIN_MESSAGE);
 				
    		}//if(countsCheckBox.isSelected()){
  
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "OUTFILE ERROR" + newline + "DO NOT TRUST RESULTS", "APP ERROR", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
    	java.lang.System.exit(2);
    } // end SCAPParser
    /**
     * This parses the ####.nessus files that ACAS produces post scan
     * 
     * 
     * @param document
     */
 
/**
 * This parses STIG viewers CKLs
 * 
 * @param document
 */
    private void cklParser(Document document) {
    	FindingEntry fe = null;
    	
    	try {
    		XPath xpath = XPathFactory.newInstance().newXPath();    		
    	    
    		//get host information
    		Node assetNode = document.getElementsByTagName("ASSET").item(0);
    	    //Node assetNode = assetNodeList.item(0);
    	    String hostName = (String) xpath.evaluate("HOST_NAME/text()", assetNode, XPathConstants.STRING);
			//Node hostIPNode = (Node) xpath.evaluate("HOST_IP/text()", assetNode, XPathConstants.STRING);
			//Node hostMACNode = (Node) xpath.evaluate("HOST_MAC/text()", assetNode, XPathConstants.STRING);
			//Node hostFQDNNode = (Node) xpath.evaluate("HOST_FQDN/text()", assetNode, XPathConstants.STRING);
			//Node techArea = (Node) xpath.evaluate("TECH_AREA/text()", assetNode, XPathConstants.STRING);
			//Node STIGNameNode = (Node) xpath.evaluate("CHECKLIST/STIGS/iSTIG/STIG_INFO/SI_DATA[8]/SID_DATA", document, XPathConstants.NODE);
			//Node STIGVerNode = (Node) xpath.evaluate("CHECKLIST/STIGS/iSTIG/STIG_INFO/SI_DATA[7]/SID_DATA", document, XPathConstants.NODE);
			String stigName = (String) xpath.evaluate("CHECKLIST/STIGS/iSTIG/VULN[1]/STIG_DATA[22]/ATTRIBUTE_DATA", document, XPathConstants.STRING);
			//in the ckl in the vuln node, they have this again, but you save processing time by doing it here
			stigName = POAMBuilderTools.shortenSTIGName(stigName);
			
			//get vuln information and loop thru it 
			NodeList vulnNodeList = document.getElementsByTagName("VULN"); //don't change this, going to * or what ever won't get the right number to loop
	    	for (int cnt = 0; cnt < vulnNodeList.getLength(); cnt++) {
	    		Node vulnNode = vulnNodeList.item(cnt);
	    		String vulnStatus = (String) xpath.evaluate("STATUS/text()", vulnNode, XPathConstants.STRING);  	
	    		
	    		if (vulnStatus.equalsIgnoreCase("Open")){

	    			String vulnID = (String) xpath.evaluate("STIG_DATA[1]/ATTRIBUTE_DATA", vulnNode, XPathConstants.STRING); //this gets the V id				
	    			String stigID = (String) xpath.evaluate("STIG_DATA[5]/ATTRIBUTE_DATA", vulnNode, XPathConstants.STRING); // this gets the SV number										
	    			String catNum = (String) xpath.evaluate("STIG_DATA[2]/ATTRIBUTE_DATA", vulnNode, XPathConstants.STRING);
	    			catNum = POAMBuilderTools.returnCAT(catNum);
	    			String ruleTitle = (String) xpath.evaluate("STIG_DATA[6]/ATTRIBUTE_DATA", vulnNode, XPathConstants.STRING);										
	    			String discussion = (String) xpath.evaluate("STIG_DATA[7]/ATTRIBUTE_DATA", vulnNode, XPathConstants.STRING);
	    			String iac = (String) xpath.evaluate("STIG_DATA[8]/ATTRIBUTE_DATA", vulnNode, XPathConstants.STRING);
	    			String checkContent = (String) xpath.evaluate("STIG_DATA[9]/ATTRIBUTE_DATA", vulnNode, XPathConstants.STRING);
	    			String fixText = (String) xpath.evaluate("ATTRIBUTE_DATA", vulnNode, XPathConstants.STRING);
	    			String cleanComment = (String) xpath.evaluate("FINDING_DETAILS", vulnNode, XPathConstants.STRING);
	    			cleanComment = cleanComment.replace(split, " ");
	    			//Node findingComments = (Node) xpath.evaluate("CHECKLIST/STIGS/iSTIG/VULN[" + cnt +"]/COMMENTS", document, XPathConstants.NODE);
	    			String cci = (String) xpath.evaluate("STIG_DATA[24]/ATTRIBUTE_DATA", vulnNode, XPathConstants.STRING);
					//Node fixText = (Node) xpath.evaluate("CHECKLIST/STIGS/iSTIG/VULN[" + cnt +"]/STIG_DATA[24]/ATTRIBUTE_DATA", document, XPathConstants.NODE);
					
					fe = new FindingEntry();
					fe.setFindingID(vulnID + " : " + stigID);
					fe.setFindingSource(stigName);  
					fe.setCat(catNum);
					fe.setLevel(catNum);
					fe.setDescription(ruleTitle + newline + discussion);		
					fe.setFindingDetail(cleanComment);
					fe.setCheckContent(checkContent);
					fe.setFixText(fixText);
					
					if(hostName != null){
						String hnn = hostName; 
						fe.setSystemName(hnn);
					} else {
						JOptionPane.showMessageDialog(this, "INPUT FILE ERROR, Missing Host information" + newline + "DO NOT TRUST RESULTS", "CKL ERROR", JOptionPane.ERROR_MESSAGE);
						break;
					}	
					
					if (iac.equals("")){
						String rmf = POAMBuilderData.cciToRmf.get(cci);
						fe.setControl(rmf);
					} else {
						String[] capArr = iac.split(" ");
						String rmf = "";
						for(int c=0; c <capArr.length; c++){
							rmf = rmf + newline + POAMBuilderData.capToRmf.get(capArr[c]);
						}
						fe.setControl(rmf);
					}
					fe.setScheduledcompletiondate(POAMBuilderTools.returnDate(catNum));
					
					 // feed poamEntry in array
					String key = vulnID + stigName; //.getTextContent()id + stig allows me to fold the duplicates in faster and more effiecntly
					
					if(entryMap.get(key) == null){ // && entryMap.get(key).equals("")
						entryMap.put(key, fe);
					} else {
						FindingEntry dupePE = entryMap.get(key);
						dupePE.setSystemName(dupePE.getSystemName() + newline + hostName);  //.getTextContent()
						entryMap.remove(key);
						entryMap.put(key, dupePE);
					}//if(entryMap.get(key) == null){
		
	    		}//end if (vulnStatus.getTextContent().equals("Open")){
			}// end for (int cnt = 1; cnt <= nl.getLength(); cnt++) {		

		} catch (XPathExpressionException e) {
			if(fe.getFindingID()!= null && !fe.getFindingID().trim().equals("")){
				JOptionPane.showMessageDialog(this, "INPUT FILE ERROR AT " + fe.getFindingID() + "\nDO NOT TRUST RESULTS", "CKL ERROR", JOptionPane. ERROR_MESSAGE);
			} else {
				JOptionPane.showMessageDialog(this, "Issue with CKL, either wrong format or missing information", "CKL ERROR",	JOptionPane.ERROR_MESSAGE);
			}
			e.printStackTrace();
		}
    	System.gc();
	}//end cklParser
    
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
			monthMap.put("15", "APR");
			monthMap.put("16", "MAY");
			
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
			//formattedDate = Integer.toString(y) + monthMap.get(Integer.toString(m)) + day;
			
			//forward date is for typical 90 day patch cycle
			//datePicker.getModel().addMonth(3);   //wtf this doesn't work...
			if(m+1 >= 12 || m+2 >= 12 || m+3 >= 12){
				cat1scheduledcompletiondate = Integer.toString(y+1) + monthMap.get(Integer.toString(m+1)) + day;
				cat2scheduledcompletiondate = Integer.toString(y+1) + monthMap.get(Integer.toString(m+2)) + day;
				cat3scheduledcompletiondate = Integer.toString(y+1) + monthMap.get(Integer.toString(m+3)) + day;
			} else {
				cat1scheduledcompletiondate = Integer.toString(y) + monthMap.get(Integer.toString(m+1)) + day;
				cat2scheduledcompletiondate = Integer.toString(y) + monthMap.get(Integer.toString(m+2)) + day;
				cat3scheduledcompletiondate = Integer.toString(y) + monthMap.get(Integer.toString(m+3)) + day;
			}			
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Issue with date creation" + newline + "DO NOT TRUST RESULTS", "APP ERROR", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
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
     * This takes the given SCAP or Retina arrays and finds them in the first line of the file and returns their column location, 
     * this way we know the location of the columns we need each time
     * 
     * */
    public void loadColumns(List<String> columns, String[] firstLine){
    	//reset column locations, there might be a better way to do this, but I don't know
    	columnLocations = new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
    	String checkColumns = columns.toString();
    	checkColumns = checkColumns.replace("[", "");
    	checkColumns = checkColumns.replace("]", "");
    	checkColumns = checkColumns.replace(",", "");  
    	//load columns with file column locations
    	List<String> fl = Arrays.asList(firstLine);
    	int x = 0;
    	for(String s:columns){
		   columnLocations[x] = fl.indexOf(s);
		   if(columnLocations[x] != -1){
			   checkColumns = checkColumns.replace(s, "");
		   }
		   x++;
		}//for(String s:columns){
    		checkColumns = checkColumns.trim();
		   	checkColumns = checkColumns.replace("   ", " ");
		   	checkColumns = checkColumns.replaceAll("  ", " ");
		if(!checkColumns.trim().equals("")){
			JOptionPane.showMessageDialog(this, "Issues with column referencing" + newline + 
					"Column header(s), " + checkColumns + " missing", "DATA ERROR",	
					JOptionPane.ERROR_MESSAGE);
		}//if(!checkColumns.trim().equals("")){
    }// loadColumns
    
    
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
    
    private void acasEntry(CSVReader reader){
    	String[] nextLine;
    	String cat = "";
    	FindingEntry fe = new FindingEntry();
    	try {
    		while ((nextLine = reader.readNext()) != null) {
    			
    			cat = nextLine[columnLocations[2]];
    			if(cat == null || cat.trim().equals("")){
    				JOptionPane.showMessageDialog(this, "Issue with ACAS file," + newline+ 
    						"DO NOT TRUST RESULTS", "ACAS ERROR", JOptionPane.ERROR_MESSAGE);
    				break;
    			}

				if(!cat.equals("Info") ){  //don't run the ACAS Detailed Vulnerability Export unless you have info filtered out!
					//String pluginName = nextLine[columnLocations[0]];
					String synopis = nextLine[columnLocations[1]];
					String severity = nextLine[columnLocations[2]];
					String findingDetail = nextLine[columnLocations[3]].replaceAll("#", " ");
					//String cve = nextLine[columnLocations[4]];
					String solution = nextLine[columnLocations[5]];
					String description = nextLine[columnLocations[6]];
					//String riskFactor = nextLine[columnLocations[7]];
					String dnsName =  nextLine[columnLocations[8]];
					String netName = nextLine[columnLocations[9]];
					String ipName = nextLine[columnLocations[10]];
					//String techArea = nextLine[columnLocations[11]];
					//String seeAlso = nextLine[columnLocations[12]];
					String pluginID = nextLine[columnLocations[13]];

					fe = new FindingEntry();
					fe.setFindingID(pluginID);
					fe.setFindingSource("ACAS - Nessus Scanner");
					fe.setDescription(synopis + newline + description); //
					fe.setControl("RA-5"); 
					fe.setCheckContent("");
					fe.setFixText(solution.replace(split, " "));
					fe.setFindingDetail(findingDetail.replace(split, " "));
					
					String sev = POAMBuilderTools.returnCAT(severity.toLowerCase());
					fe.setCat(sev);
					fe.setLevel(sev);
					fe.setScheduledcompletiondate(POAMBuilderTools.returnDate(sev));
					
					String systemName = "";
					if (!netName.trim().equals("") ){
						systemName = netName;
					} else if (!systemName.trim().equals("") && !dnsName.trim().equals("")){
						systemName = dnsName;
					} else if (!systemName.trim().equals("") && !ipName.trim().equals("")){
						systemName = ipName;
					} else {
						systemName = "SystemNotFound";
					}
					
					fe.setSystemName(systemName);
					
		    		String key = pluginID + "ACAS"; 
		    		
		    		if(entryMap.get(key) == null){ // && entryMap.get(key).equals("")
						entryMap.put(key, fe);
					} else {
						FindingEntry dupePE = entryMap.get(key);
						if(!dupePE.getSystemName().contains(systemName)){
							dupePE.setSystemName(dupePE.getSystemName() + newline + systemName);
							entryMap.remove(key);
							entryMap.put(key, dupePE);
						}//ACAS will have multiple findings for the same box just because it's a different port, 
						//no need to double/triple up on same finding
					}//if(entryMap.get(key) == null){		    		
				} // end if(nextLine[columnLocations[7]].equals("Open") ){				
			} // end while ((nextLine = reader.readNext()) != null) {
    	} catch (IOException e) {
    		if(fe.getFindingID() !=  null && !fe.getFindingID().trim().equals("")){ 
    			JOptionPane.showMessageDialog(this, "INPUT FILE FORMAT ERROR AT " + fe.getFindingID() + "\nDO NOT TRUST RESULTS", "STIG ERR OR", JOptionPane.ERROR_MESSAGE ) ;
    		} else {
    			JOptionPane.showMessageDialog(this, "INPUT FILE FORMAT ERROR, make sure you are using the ACAS Detailed Vulnerability Export and you have \''info\" filtered out! ",	"ACAS ERROR", JOptionPane.ERROR_MESSAGE ) ;
    		}//if(pe.getFindingID() != null && !pe.ge tFinding ID().trim() . equals(" ")){ 
    		
			e.printStackTrace();
		}
    	System.gc();
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
    private void scapEntry(CSVReader reader){
    	String[] nextLine;
    	FindingEntry fe = new FindingEntry();
    	try {
			while ((nextLine = reader.readNext()) != null) {
				//scap has bad habit of not having good output, kick it back
				String stat = nextLine[columnLocations[7]];
				if(stat == null || stat.trim().equals("")){
					JOptionPane.showMessageDialog(this, "Issue with STIG CSV" + newline + 
							"DO NOT TRUST RESULTS", "STIG ERROR", JOptionPane.ERROR_MESSAGE);
					break;
				}//if(stat == null || stat.trim().equals("")){
			
				if(nextLine[columnLocations[7]].equalsIgnoreCase("Open") ){ 
					fe = new FindingEntry();
					
					String iac = "";
					if(!nextLine[columnLocations[3]].trim().equals("")){
						iac = nextLine[columnLocations[3]].trim();
					}
					
		    		String cci = nextLine[columnLocations[10]];	
					if(cci.contains("CCI")){
						//int chk = charSqCnt(cci, "CCI");
						String[] cciArr = cci.split("CCI-");
						String rmf = "";
						for(int c=0; c < cciArr.length; c++){
							String[] rmfcon = cciArr[c].split(" :: ");
							if(!rmfcon[rmfcon.length-1].trim().equals("") && rmf.trim().equals("")){
								rmf = rmfcon[rmfcon.length-1].trim();
							} else if(!rmfcon[rmfcon.length-1].trim().equals("")){
								rmf = rmfcon[rmfcon.length-1].trim() + newline + rmf;
							}
						}//for(int c=0; c<cciArr.length; c++){
						if(!iac.trim().equals("")){
							fe.setControl(rmf);	
						} else {
							fe.setControl(rmf);
						}//if(!iac.trim().equals("")){
								
					} //if(cci.contains("CCI")){
					if (fe.getControl() == null){
						//String cap = fe.getControl().trim();
						String[] capArr = iac.split(" ");
						String rmf = "";
						
						for(int c=0; c<capArr.length; c++){
							if(!capArr[c].trim().equals("")){
								rmf = rmf + newline + POAMBuilderData.capToRmf.get(capArr[c]);
							}
						}//for(int c=0; c<capArr.length; c++){
						
						//if(!iac.trim().equals("")){
						//	fe.setControl(iac + rmf);
						//} else {
							fe.setControl(rmf);
						//}//if(!iac.trim().equals("")){
						
					} else if (fe.getControl() == null){
						fe.setControl("ERROR");
					}//if (fe.getControl() == null){
					
					String des = nextLine[columnLocations[0]] + newline + nextLine[columnLocations[1]]; //
					des = des.replace(split, " ");	
					String cat = POAMBuilderTools.returnCAT(nextLine[columnLocations[2]]);					
					String level = POAMBuilderTools.returnCAT(nextLine[columnLocations[2]]);	//because ACAS has levels
					String findingid = nextLine[columnLocations[14]] + " : " + nextLine[columnLocations[4]]; //the great vuln id vs stig id debate, stigid = 4
					String stigName = POAMBuilderTools.shortenSTIGName(nextLine[columnLocations[6]]);	
		    		String comment = nextLine[columnLocations[8]].replace(split, " ");	
		    		String finding = nextLine[columnLocations[9]].replace(split, " ");
		    		String systemName = "";
		    		String dnsName =  nextLine[columnLocations[11]];
					String netName = nextLine[columnLocations[12]];
					String ipName = nextLine[columnLocations[13]];
					if (dnsName != null && !dnsName.trim().equals("")){
						systemName = dnsName;
					} else if (netName != null && !netName.trim().equals("") ){
						systemName = netName;
					} else if (ipName != null && !ipName.trim().equals("")){
						systemName = ipName;
					} else {
						systemName = "SystemNotFound";
						JOptionPane.showMessageDialog(this, "CSV File error at " + fe.getFindingID() + ", No System Name Found" + newline +"DO NOT TRUST RESULTS", 
								"STIG ERROR", JOptionPane.ERROR_MESSAGE);
					}
					String checkContent = nextLine[columnLocations[15]];
					
					fe.setLevel(level);	
					fe.setDescription(des);
					fe.setFindingID(findingid);
					fe.setCat(cat);
					fe.setScheduledcompletiondate(POAMBuilderTools.returnDate(cat));
					fe.setFindingSource(stigName);
					fe.setSystemName(systemName);
		    		fe.setComment(comment);
		    		fe.setFindingDetail(finding);
		    		fe.setCheckContent(checkContent);
		    		fe.setFixText("");//no fix  text in STIG CSVs
		    		
				    // feed poamEntry in array
					String key = nextLine[columnLocations[4]] + stigName; //id + stig allows me to fold the duplicates in faster and more effiecntly
					
					if(entryMap.get(key) == null){ // && entryMap.get(key).equals("")
						entryMap.put(key, fe);
					} else {
						FindingEntry dupePE = entryMap.get(key);
						dupePE.setSystemName(dupePE.getSystemName() + newline + systemName);
						entryMap.remove(key);
						entryMap.put(key, dupePE);
					}//if(entryMap.get(key) == null){
				    
				}//if(nextLine[columnLocations[7]].equals("Open") ){
			}//while ((nextLine = reader.readNext()) != null) {
		} catch (IOException e) {
			if(fe.getFindingID()!= null && !fe.getFindingID().trim().equals("")){ 
				JOptionPane.showMessageDialog(this, "INPUT FILE FORMAT ERROR AT " + fe.getFindingID() + 
						"\nDO NOT TRUST RESULTS", "STIG ERROR", JOptionPane.ERROR_MESSAGE ) ;
			} else {
				JOptionPane.showMessageDialog(this, "Issue with CSV data, make sure you using a santized CSV file," + 
						newline + "DO NOT TRUST RESULTS", "STIG ERROR", JOptionPane. ERROR_MESSAGE );
			}
			e.printStackTrace();
		}
    	System.gc();
    }//private void scapEntry(CSVReader reader, String systemName){
         
    private void nessusParser(Document document) {
    	FindingEntry fe = new FindingEntry();
    	try {
    		XPath xpath = XPathFactory.newInstance().newXPath();
    		NodeList reportHostNodeList = document.getElementsByTagName("ReportHost");
    		//System.out.println("count: " + nl.getLength());	

    		for (int cnt = 0; cnt < reportHostNodeList.getLength(); cnt++) { 
    			Node reportHostNode = (Node) reportHostNodeList.item(cnt);
    			//System.out.println("element :" + reportHostNode.getNodeName());
    			
    			String hostIP = (String) xpath.evaluate("HostProperties/tag[@name='host-ip']/text()", reportHostNode, XPathConstants.STRING);
    			String hostfqdn = (String) xpath.evaluate("HostProperties/tag[@name='host-fqdn']/text()", reportHostNode, XPathConstants.STRING);

    			if(hostIP == null && hostfqdn == null) {
    				JOptionPane.showMessageDialog(this, "INPUT ERROR" + POAMBuilder.newline + "CKL MISSING HOST INFORMAITON" + POAMBuilder.newline + "DO NOT TRUST RESULTS", "DATA ERROR", JOptionPane.ERROR_MESSAGE);
    				break;
    			}
    			
    			NodeList reportItemNodeList = (NodeList) xpath.evaluate("ReportItem", reportHostNode, XPathConstants.NODESET);
    			
    			for (int num = 0; num < reportItemNodeList.getLength(); num++) { 
    				Node reportItemNode = (Node) reportItemNodeList.item(num);
    				String riskfactor = (String) xpath.evaluate("risk_factor/text()", reportItemNode, XPathConstants.STRING);
    				//System.out.println("riskfactor "+ riskfactor);
    				
    				if(!riskfactor.equalsIgnoreCase("none")){
    					fe = new FindingEntry();
	        			//String synopsis = (String) xpath.evaluate("synopsis/text()", reportItemNode, XPathConstants.STRING);
	        			String solution = (String) xpath.evaluate("solution/text()", reportItemNode, XPathConstants.STRING);
						String findingdetail = ((String) xpath.evaluate("plugin_output/text()", reportItemNode, XPathConstants.STRING)).trim();
						String description = (String) xpath.evaluate("description/text()", reportItemNode, XPathConstants.STRING);
						
						String catnum = POAMBuilderTools.returnCAT(riskfactor);

						String pluginID = "";
						String pluginName = "";
						NamedNodeMap attrs = reportItemNode.getAttributes();
					    for(int i = 0 ; i<attrs.getLength() ; i++) {
					      Attr attribute = (Attr)attrs.item(i);   
					      //looping is bullshit but whatever
					      if(attribute.getName().equalsIgnoreCase("pluginID")) {
					          pluginID = attribute.getValue();
					      }
					      if(attribute.getName().equalsIgnoreCase("pluginName")){
					    	  pluginName = attribute.getValue();
					      }
					    }//for(int i = 0 ; i<attrs.getLength() ; i++) {
						
						fe.setFindingID(pluginID);
						fe.setDescription(pluginName + POAMBuilder.newline + description);
						fe.setFindingDetail(findingdetail);
						
						if(hostfqdn != null) {
							fe.setSystemName(hostfqdn);
						} else if (hostIP != null) {
							fe.setSystemName(hostIP);
						}
						
						fe.setCat(catnum);
						fe.setLevel(catnum);
						fe.setControl("RA-5"); 
						fe.setFindingSource("ACAS - Nessus Scanner");
						fe.setScheduledcompletiondate(POAMBuilderTools.returnDate(catnum));
						fe.setFixText(solution);
						fe.setCheckContent("");
						
    					String key = pluginID + "ACAS"; 
    		    		
    		    		if(POAMBuilder.entryMap.get(key) == null){ // && entryMap.get(key).equals("")
    		    			POAMBuilder.entryMap.put(key, fe);
    					} else {
    						FindingEntry dupePE = POAMBuilder.entryMap.get(key);
    						if(!dupePE.getSystemName().contains(hostIP)){
    							dupePE.setSystemName(dupePE.getSystemName() + POAMBuilder.newline + hostIP);
    							POAMBuilder.entryMap.remove(key);
    							POAMBuilder.entryMap.put(key, dupePE);
    						}//ACAS will have multiple findings for the same box just because it's a different port, 
    						//no need to double/triple up on same finding
    					}//if(entryMap.get(key) == null){		    		
    				}//if(!riskfactor.equalsIgnoreCase("none")){
    			} //for (int num = 0; num < reportItemNodeList.getLength(); num++) {			
    		}//for (int cnt = 0; cnt < reportHostNodeList.getLength(); cnt++) { 	
    	} catch (Exception e) {
    		e.printStackTrace();
    		if(fe.getFindingID() !=  null && !fe.getFindingID().trim().equals("")){ 
    			JOptionPane.showMessageDialog(this, "INPUT FILE FORMAT ERROR AT " + 
    					fe.getFindingID() + "\nDO NOT TRUST RESULTS", "STIG ERR OR", 
    					JOptionPane.ERROR_MESSAGE ) ;
    		}
    		
		}
    }//nessusParser



}//end class body
