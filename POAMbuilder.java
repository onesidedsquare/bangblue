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
import javax.swing.JProgressBar;
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

import org.w3c.dom.Document;
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
public class POAMbuilder extends JPanel
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
	private static final String newline = "\n";
	private static final String split = "#";

	//These 2 strings will always be in the outputs, these are to test to see which one is which
	//private static final String retinaHeaderTest = "NetBIOSName";
	private static final String scapHeaderTest = "STIG ID";
	private static final String acasHeaderTest = "Plugin";
	
	private static int[] columnLocations;

	private static List<String> scapColumns = new ArrayList<String>();
	private static List<String>	acasColumns = new ArrayList<String>();
	private static Map<String, String> capToRmf = new HashMap<String, String>();
	private static Map<String, String> cciToRmf = new HashMap<String, String>();
	private List<String> stigNames = new ArrayList<String>();
	private List<String> iaControls = new ArrayList<String>();
	//private static final String poamFirstLine = "Weakness#CAT#IA Control and Impact Code#POC#Resources Required#Scheduled Completion Date"
	//		+ "#Milestones with Completion Dates#Milestone Changes#Source Identifying Weakness#Status#Comments#System"; //#Severity
	//private static final String newPOAMFirstLine = "Vuln ID#STIG ID#Name#Description#Raw Risk#IA Control#Source Identifying Weakness"
	//		+ "#Technology Area#Program#Affected System#IP Address#Finding Details/Tool Results#Fix Procedures (From Tool)#Point of Contact#Engineer Status#Engineer Comments"
	//		+ "#Remediation Steps#Mitigation#CAT (Residual Risk)#Status (Final/Official)#Validator Comments#Documentation Status#Scan Date#Date Last Updated#IAU";
	private static final String emassPOAMFirstLine = "#Control Vulnerablity Description#Security Control Number#Office Organization#Security Checks#Raw Security Value#Mitigation"
			+ "#Severity Value#Resources Required#Scheduled Completion Date#Milestone with Completion Date#Source Idenifitying Weakness#Status#Comments#Affected Systems#Check Content"
			+ "#Fix Text";
	private static final String rarFirstLine = "Identifier Applicable Security Control (1)#Source of Discovery or Test Tool Name (2)#Test ID or Threat IDs (3)#Description of Vulnerability/Weakness (4)"
			+ "#Raw Risk (CAT I, II, III) (5)#Impact (6)#Likelihood (7)#Mitigation Description (10)#Remediation Description (11)#Residual Risk/Risk Exposure (12)#Status (13)"
			+ "#Comment (14)#Devices Affected (15)#Check Content#Fix Text";
	//hardcodedgoodness aww yis

	private static final String status = "OnGoing";
	private static final String resourcesrequired = "Time and Manpower";
	private static final String poc = "Project ISSO";
	private String cat1scheduledcompletiondate = "";
	private String cat2scheduledcompletiondate = "";
	private String cat3scheduledcompletiondate = "";

	//this actually holds all the entries and is written to file
	private Map<String, FindingEntry> entryMap = new HashMap<String, FindingEntry>();
	
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
	        FileNameExtensionFilter csvfilter = new FileNameExtensionFilter("CSV Files", "csv");
	        FileNameExtensionFilter xmlFilter = new FileNameExtensionFilter("CKL Files", "ckl");
	        fileChooser.setFileFilter(xmlFilter);
	        fileChooser.setFileFilter(csvfilter);
	
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
	        poamCheckBox.setSelected(true);
	        
	        rarCheckBox = new JCheckBox("RAR");
	        rarCheckBox.setSelected(true);
	        
	        controlsCheckBox = new JCheckBox("Controls");
	        controlsCheckBox.setSelected(false);
	        
	        countsCheckBox = new JCheckBox("Counts");
	        countsCheckBox.setSelected(false);

	        
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
        JFrame frame = new JFrame("POAM - RAR Builder");
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
    public void parser(File[] files) {
    	try {
    		setDate();
    		String poamDescriptorString = poamDescriptorTextbox.getText();           
    		for(File file : files) {
            	String fileName = file.getPath();
            	if(fileName.endsWith(".csv")){
            		//System.out.println("CSV");
            		CSVReader reader = new CSVReader(new FileReader(file));
    				String[] firstLine = reader.readNext();//this allows us to figure what kind of file it is
    				
    				if(stringContainsItemFromList(scapHeaderTest, firstLine)){
    					loadColumns(scapColumns, firstLine);
    					scapEntry(reader);
    	        	}//IF SCAP
    				   				
    				if(stringContainsItemFromList(acasHeaderTest, firstLine)){
    					loadColumns(acasColumns, firstLine);
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
				  poamline = poamline + split + entry.getFindingSource();
				  poamline = poamline + split + status;
				  poamline = poamline + split + entry.getFindingDetail();
				  poamline = poamline + split + entry.getSystemName();
				  poamline = poamline + split + entry.getCheckContent();
				  poamline = poamline + split + entry.getFixText();
				  
				  poamline = poamline.replaceAll("\"", " ");
				  poamline = poamline.replaceAll(",", " ");
	
				  poamWriter.writeNext(poamline.split(split));
				  cnt++;
				}
            
				poamWriter.close();
	            JOptionPane.showMessageDialog(this, "POAM Created", "Success", JOptionPane.PLAIN_MESSAGE);
    		}
            
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
		            
    		}
    		
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
				  }
				}
				
				CSVWriter controlsWriter = new CSVWriter(new FileWriter(poamDescriptorString + "_CONTROLS.csv"), ',');
	            for (int c = 0; c < iaControls.size(); c++){
	            	//System.out.println(iaControls.get(c));
	            	controlsWriter.writeNext(iaControls.get(c).split(split));
	            }
	            controlsWriter.close();
	            JOptionPane.showMessageDialog(this, "Contorls List Created", "Success", JOptionPane.PLAIN_MESSAGE);
			} // end while (entries.hasNext()) {
			
			
    		if(countsCheckBox.isSelected()){
 				Iterator<Entry<String, FindingEntry>> entries = entryMap.entrySet().iterator();
 				while (entries.hasNext()) {
 					Entry<String, FindingEntry> thisEntry = (Entry<String, FindingEntry>) entries.next();
 					FindingEntry entry = (FindingEntry) thisEntry.getValue();
	    			if(!stigNames.contains(entry.getFindingSource())){
	    				stigNames.add(entry.getFindingSource());
	   			  	}
 				}
 				
	            CSVWriter stigWriter = new CSVWriter(new FileWriter(poamDescriptorString + "_STIG.csv"), ',');
	            for (int c = 0; c < stigNames.size(); c++){
	            	stigWriter.writeNext(stigNames.get(c).split(split));
	            }
	            stigWriter.close();
	            JOptionPane.showMessageDialog(this, "STIGs List Created", "Success", JOptionPane.PLAIN_MESSAGE);
 				
    		}
  
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "OUTFILE ERROR" + newline + "DO NOT TRUST RESULTS", "APP ERROR", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
    	java.lang.System.exit(2);
    } // end SCAPParser
    

    private void cklParser(Document document) {
    	FindingEntry fe = new FindingEntry();
    	//RAREntry re = new RAREntry();
    	try {
    		
    		
    	    XPath xpath = XPathFactory.newInstance().newXPath();    		
    	    //get host information
			Node hostNameNode = (Node) xpath.evaluate("CHECKLIST/ASSET/HOST_NAME", document, XPathConstants.NODE);
			//Node hostIPNode = (Node) xpath.evaluate("CHECKLIST/ASSET/HOST_IP", document, XPathConstants.NODE);
			//Node hostMACNode = (Node) xpath.evaluate("CHECKLIST/ASSET/HOST_MAC", document, XPathConstants.NODE);
			//Node hostFQDNNode = (Node) xpath.evaluate("CHECKLIST/ASSET/HOST_FQDN", document, XPathConstants.NODE);
			//Node techArea = (Node) xpath.evaluate("CHECKLIST/ASSET/TECH_AREA", document, XPathConstants.NODE);
			//Node STIGNameNode = (Node) xpath.evaluate("CHECKLIST/STIGS/iSTIG/STIG_INFO/SI_DATA[8]/SID_DATA", document, XPathConstants.NODE);
			//Node STIGVerNode = (Node) xpath.evaluate("CHECKLIST/STIGS/iSTIG/STIG_INFO/SI_DATA[7]/SID_DATA", document, XPathConstants.NODE);
			Node STIGNameNode = (Node) xpath.evaluate("CHECKLIST/STIGS/iSTIG/VULN[1]/STIG_DATA[22]/ATTRIBUTE_DATA", document, XPathConstants.NODE);
			//in the ckl in the vuln node, they have this again, but you save processing time by doing it here

			//get vuln information and loop thru it 
			NodeList nl = document.getElementsByTagName("VULN"); //don't change this, going to * or what ever won't get the right number to loop
	    	for (int cnt = 1; cnt <= nl.getLength(); cnt++) {
	    		Node vulnStatus = (Node) xpath.evaluate("CHECKLIST/STIGS/iSTIG/VULN[" + cnt +"]/STATUS", document, XPathConstants.NODE);  	
	    		
	    		if (vulnStatus.getTextContent().equals("Open")){
	    		
					Node vulnNumber = (Node) xpath.evaluate("CHECKLIST/STIGS/iSTIG/VULN[" + cnt +"]/STIG_DATA[1]/ATTRIBUTE_DATA", document, XPathConstants.NODE); //this gets the V id				
					Node vulnSTIGID = (Node) xpath.evaluate("CHECKLIST/STIGS/iSTIG/VULN[" + cnt +"]/STIG_DATA[5]/ATTRIBUTE_DATA", document, XPathConstants.NODE); // this gets the SV number										
					Node vulnSeverity = (Node) xpath.evaluate("CHECKLIST/STIGS/iSTIG/VULN[" + cnt +"]/STIG_DATA[2]/ATTRIBUTE_DATA", document, XPathConstants.NODE);									
					Node vulnRuleTitle = (Node) xpath.evaluate("CHECKLIST/STIGS/iSTIG/VULN[" + cnt +"]/STIG_DATA[6]/ATTRIBUTE_DATA", document, XPathConstants.NODE);										
					Node vulnDiscussion = (Node) xpath.evaluate("CHECKLIST/STIGS/iSTIG/VULN[" + cnt +"]/STIG_DATA[7]/ATTRIBUTE_DATA", document, XPathConstants.NODE);
					Node vulnIAControl = (Node) xpath.evaluate("CHECKLIST/STIGS/iSTIG/VULN[" + cnt +"]/STIG_DATA[8]/ATTRIBUTE_DATA", document, XPathConstants.NODE);
					Node vulnCheckContent = (Node) xpath.evaluate("CHECKLIST/STIGS/iSTIG/VULN[" + cnt +"]/STIG_DATA[9]/ATTRIBUTE_DATA", document, XPathConstants.NODE);
					Node vulnFixText = (Node) xpath.evaluate("CHECKLIST/STIGS/iSTIG/VULN[" + cnt +"]/STIG_DATA[10]/ATTRIBUTE_DATA", document, XPathConstants.NODE);
					Node vulnFindingDetails = (Node) xpath.evaluate("CHECKLIST/STIGS/iSTIG/VULN[" + cnt +"]/FINDING_DETAILS", document, XPathConstants.NODE);
					//Node vulnFindingComments = (Node) xpath.evaluate("CHECKLIST/STIGS/iSTIG/VULN[" + cnt +"]/COMMENTS", document, XPathConstants.NODE);
					Node vulnCCI = (Node) xpath.evaluate("CHECKLIST/STIGS/iSTIG/VULN[" + cnt +"]/STIG_DATA[24]/ATTRIBUTE_DATA", document, XPathConstants.NODE);
					//Node vulFix = (Node) xpath.evaluate("CHECKLIST/STIGS/iSTIG/VULN[" + cnt +"]/STIG_DATA[24]/ATTRIBUTE_DATA", document, XPathConstants.NODE);
					
					fe = new FindingEntry();
					String checkContent = vulnCheckContent.getTextContent();
					String fixText = vulnFixText.getTextContent();
					String catNum = returnCAT(vulnSeverity.getTextContent());
					String cleanComment = vulnFindingDetails.getTextContent();
					cleanComment = cleanComment.replace(split, " ");
					String iac = vulnIAControl.getTextContent().trim();
				
					fe.setFindingID(vulnNumber.getTextContent() + " : " + vulnSTIGID.getTextContent());
					fe.setFindingSource(STIGNameNode.getTextContent());
					fe.setCat(catNum);
					fe.setLevel(catNum);
					fe.setDescription(vulnRuleTitle.getTextContent() + newline + vulnDiscussion.getTextContent());		
					fe.setFindingDetail(cleanComment);
					fe.setCheckContent(checkContent);
					fe.setFixText(fixText);
					
					if(hostNameNode != null){
						String hnn = hostNameNode.getTextContent();
						fe.setSystemName(hnn);
					} else {
						JOptionPane.showMessageDialog(this, "INPUT FILE ERROR, Missing Host information" + newline + "DO NOT TRUST RESULTS", "CKL ERROR", JOptionPane.ERROR_MESSAGE);
						break;
					}	
					
					if (iac.equals("")){
						//pe.setControl("ERROR ECSC-1" + newline + "CM-6");
						String cci = vulnCCI.getTextContent();
						String rmf = cciToRmf.get(cci);
						fe.setControl(rmf);
					} else {
						String[] capArr = iac.split(" ");
						String rmf = "";
						for(int c=0; c <capArr.length; c++){
							rmf = rmf + newline + capToRmf.get(capArr[c]);
						}
						fe.setControl(iac + newline + rmf);
					}
					fe.setScheduledcompletiondate(returnDate(catNum));
					
					 // feed poamEntry in array
					String key = vulnNumber.getTextContent() + STIGNameNode.getTextContent(); //id + stig allows me to fold the duplicates in faster and more effiecntly
					
					if(entryMap.get(key) == null){ // && entryMap.get(key).equals("")
						entryMap.put(key, fe);
					} else {
						FindingEntry dupePE = entryMap.get(key);
						dupePE.setSystemName(dupePE.getSystemName() + newline + hostNameNode.getTextContent());
						entryMap.remove(key);
						entryMap.put(key, dupePE);
					}//if(entryMap.get(key) == null){
		
	    		}//end if (vulnStatus.getTextContent().equals("Open")){
			}	// end for (int cnt = 1; cnt <= nl.getLength(); cnt++) {		

		} catch (XPathExpressionException e) {
			if(fe.getFindingID()!= null && !fe.getFindingID().trim().equals("")){
				JOptionPane.showMessageDialog(this, "INPUT FILE ERROR AT " + fe.getFindingID() + "\nDO NOT TRUST RESULTS", "CKL ERROR", JOptionPane. ERROR_MESSAGE);
			} else {
				JOptionPane.showMessageDialog(this, "Issue with CKL, either wrong format or missing information", "CKL ERROR",	JOptionPane.ERROR_MESSAGE);
			}
			e.printStackTrace();
		}
    	   
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
		}
    		checkColumns = checkColumns.trim();
		   	checkColumns = checkColumns.replace("   ", " ");
		   	checkColumns = checkColumns.replaceAll("  ", " ");
		if(!checkColumns.trim().equals("")){
			JOptionPane.showMessageDialog(this, "Issues with column referencing" + newline + 
					"Column header(s), " + checkColumns + " missing", "DATA ERROR",	
					JOptionPane.ERROR_MESSAGE);
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
					fe = new FindingEntry();
					
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
			
					//replace # for linux files

					fe.setFindingID(pluginID);
					fe.setFindingSource("ACAS - Nessus Scanner");
					fe.setDescription(synopis + newline +description); //
					fe.setControl("VIVM-1" + newline + "CM-6"); 
					fe.setCheckContent("");
					fe.setFixText(solution.replace(split, " "));
					fe.setFindingDetail(findingDetail.replace(split, " "));
					
					String sev = returnCAT(severity.toLowerCase());
					fe.setCat(sev);
					fe.setLevel(sev);
					fe.setScheduledcompletiondate(returnDate(sev));
					
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
				if(stat == null || stat.trim().equals("")){// || !stat.trim().equals("Open") || !stat.trim().equals("Not A Finding") || !stat.trim().equals("Not Applicable")
					JOptionPane.showMessageDialog(this, "Issue with STIG CSV" + newline + 
							"DO NOT TRUST RESULTS", "STIG ERROR", JOptionPane.ERROR_MESSAGE);
					break;
				}
			
				if(nextLine[columnLocations[7]].equals("Open") ){ 
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
						for(int c=0; c<cciArr.length; c++){
							String[] rmfcon = cciArr[c].split(" :: ");
							if(!rmfcon[rmfcon.length-1].trim().equals("") && rmf.trim().equals("")){
								rmf = rmfcon[rmfcon.length-1].trim();
							} else if(!rmfcon[rmfcon.length-1].trim().equals("")){
								rmf = rmfcon[rmfcon.length-1].trim() + newline + rmf;
							}
						}//for(int c=0; c<cciArr.length; c++){
						if(!iac.trim().equals("")){
							fe.setControl(iac + newline + rmf);	
						} else {
							fe.setControl(rmf);
						}
								
					} 
					if (fe.getControl() == null){
						//String cap = fe.getControl().trim();
						String[] capArr = iac.split(" ");
						String rmf = "";
						
						for(int c=0; c<capArr.length; c++){
							if(!capArr[c].trim().equals("")){
								rmf = rmf + newline + capToRmf.get(capArr[c]);
							}
						}
						
						if(!iac.trim().equals("")){
							fe.setControl(iac + rmf);
						} else {
							fe.setControl(rmf);
						}
						
					} else if (fe.getControl() == null){
						fe.setControl("ERROR");
					}//if(chk == 1){
					
					String des = nextLine[columnLocations[0]] + newline + nextLine[columnLocations[1]]; //
					des = des.replace(split, " ");	
					String cat = returnCAT(nextLine[columnLocations[2]]);					
					String level = returnCAT(nextLine[columnLocations[2]]);	//because ACAS has levels
					String findingid = nextLine[columnLocations[14]] + " : " + nextLine[columnLocations[4]]; //the great vuln id vs stig id debate, stigid = 4
					String stigName = shortenSTIGName(nextLine[columnLocations[6]]);
	
					//UNIX sometimes have # in its comments, have to parse it out
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
					fe.setScheduledcompletiondate(returnDate(cat));
					fe.setFindingSource(stigName);
					fe.setSystemName(systemName);
		    		fe.setComment(comment);
		    		fe.setFindingDetail(finding);
		    		fe.setCheckContent(checkContent);
		    		fe.setFixText("");//no fix  text in STIG CSVs
		    		
				    // feed poamEntry in array
					String key = nextLine[columnLocations[4]]+stigName; //id + stig allows me to fold the duplicates in faster and more effiecntly
					
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
    }//private void scapEntry(CSVReader reader, String systemName){
  
    public class poamCounts {
    	private String poamName;
    	private int cat1;
    	private int cat2;
    	private int cat3;
    	private int ovrcat1;
    	private int ovrcat2;
    	private int ovrcat3;
		public int getOvrcat1() {
			return ovrcat1;
		}
		public void setOvrcat1(int ovrcat1) {
			this.ovrcat1 = ovrcat1;
		}
		public int getOvrcat2() {
			return ovrcat2;
		}
		public void setOvrcat2(int ovrcat2) {
			this.ovrcat2 = ovrcat2;
		}
		public int getOvrcat3() {
			return ovrcat3;
		}
		public void setOvrcat3(int ovrcat3) {
			this.ovrcat3 = ovrcat3;
		}
		public String getPoamName() {
			return poamName;
		}
		public void setPoamName(String poamName) {
			this.poamName = poamName;
		}
		public int getCat1() {
			return cat1;
		}
		public void setCat1(int cat1) {
			this.cat1 = cat1;
		}
		public int getCat2() {
			return cat2;
		}
		public void setCat2(int cat2) {
			this.cat2 = cat2;
		}
		public int getCat3() {
			return cat3;
		}
		public void setCat3(int cat3) {
			this.cat3 = cat3;
		}
    }
    
    /**
     * POAMEntry Java Object
     * This object contain singular lines of the poam, you'll need to set each of the vars for a complete entry
     *  @var title;	description;cat;iac;poc;resourcesrequired;	scheduledcompletiondate;status;	findingSource;findingID;comment;systemName;
     *  
     *  
     * */
   
    public class FindingEntry {
    	private String findingID;
    	private String description;
    	private String control;
    	private String cat;
    	private String level;
    	private String scheduledcompletiondate;
    	private String findingSource;
    	private String comment;
    	private String systemName;
    	private String findingDetail;
    	private String fixText;
    	private String checkContent;
	
		public String getCheckContent() {
			return checkContent;
		}
		public void setCheckContent(String checkContent) {
			this.checkContent = checkContent;
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
		public String getControl() {
			return control;
		}
		public void setControl(String iac) {
			this.control = iac;
		}
		public String getPoc() {
			return poc;
		}	
		public String getScheduledcompletiondate() {
			return scheduledcompletiondate;
		}	
		public void setScheduledcompletiondate(String scheduledcompletiondate) {
			this.scheduledcompletiondate = scheduledcompletiondate;
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
		public String getFindingDetail() {
			return findingDetail;
		}
		public void setFindingDetail(String findingDetail) {
			this.findingDetail = findingDetail;
		}
		public String getFixText() {
			return fixText;
		}
		public void setFixText(String fixText) {
			this.fixText = fixText;
		}    		
    }
    
    
    
    /**
     * shortSTIGName
     * @param stig
     * @return String parse stig name
     * 
     * STIG names can be unnecessarly long, this shortens them to a more managable length
     */
    private String shortenSTIGName(String stig) {
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
    
   /***
    * returnCAT
    * @param cat
    * @return  I  or   II   or   I I  I
    * Takes  a  Critical/High/Medium/Low  and translates  it  to the I's  II's and  I I I ' s
    */
    private String returnCAT(String cat ){ 
    	cat = cat.toLowerCase();
    	if (cat.equals("high") || cat.equals("critical") ){
    		return "I";
    	} else if (cat.equals("medium")){
    		return "II";
    	} else if (cat.equals("low")){		
    		return "III";
    	} else {
    		return "CAT NOT FOUND";
    	}
    }//end retrunCAT
    
	/**
	* This returns the number of times a string is found 15	
	* @param str - full string
	* @param findStr - squence to find
	* @return int - count of number of times found
	*/
	private int charSqCnt(String str, String findStr){ 
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
	private String returnDate(String cat){
		if (cat.equals("high") || cat.equals("critical") || cat.equals("I")){
			return cat1scheduledcompletiondate;
    	} else if (cat.equals("medium") || cat.equals("II")){
    		return cat2scheduledcompletiondate;
    	} else if (cat.equals("low") || cat.equals("III")){		
    		return cat3scheduledcompletiondate;
    	} else {
    		return cat3scheduledcompletiondate;
    	}
	}
    
    /**
     * loadColumns
     * 
     * Loads 
     * scapColumns - column headers for STIG CSV files
     * acasColumns - column headers for ACAS CSV files
     * capToRmf - map of DIACAP controls to RMF controls
     * cciToRmf - map of CCI numbers to RMF controls
     * */
    //private void fillColumns() {
    static {
		scapColumns.add("Rule Title");//0
		scapColumns.add("Discussion");//1
		scapColumns.add("Severity");//2
		scapColumns.add("IA Control");//3
		scapColumns.add("STIG ID");//4
		scapColumns.add("Mitigations");//5
		scapColumns.add("STIG");//6
		scapColumns.add("Status");//7
		scapColumns.add("Comments");//8
		scapColumns.add("Finding Details");
		scapColumns.add("CCI");
		scapColumns.add("HostName");
		scapColumns.add("IPAddress");
		scapColumns.add("MACAddress");
		scapColumns.add("Vuln Id");
		scapColumns.add("Check Content");

		acasColumns.add("Plugin Name"); //0 X
		acasColumns.add("Synopsis"); //1 title
		acasColumns.add("Severity"); //2
		acasColumns.add("Plugin Text"); //3 id
		acasColumns.add("CVE"); //4
		acasColumns.add("Solution"); //5 comments
		acasColumns.add("Description"); //6 Description
		acasColumns.add("Risk Factor"); // 7
		acasColumns.add("DNS Name");  // 8
		acasColumns.add("NetBIOS Name"); // 9
		acasColumns.add("IP Address"); // 10		
		acasColumns.add("Family");
		acasColumns.add("See Also");
		acasColumns.add("Plugin");
		
		capToRmf.put("COAS-1", "CP-2; CP-2(3); CP-7");		
		capToRmf.put("COAS-2", "CP-2; CP-2(4); CP-7");		
		capToRmf.put("COBR-1", "CP-6, CP-9; CP-9(5)");		
		capToRmf.put("CODB-1", "CP-9; CP-10; CP-10(6)");		
		capToRmf.put("CODB-2", "CP-9; CP-1 0; CP-10(6)");		
		capToRmf.put("CODB-3", "CP-9; CP-9(6)");		
		capToRmf.put("CODP-1", "CP-2; CP-2(1)(3)");		
		capToRmf.put("CODP-2", "CP-2; CP-2(1)(4)");		
		capToRmf.put("CODP-3", "CP-2; CP-2(1)(5)(6)");		
		capToRmf.put("COEB-1", "CP-6; CP-7; CP-7(5)");		
		capToRmf.put("COEB-2", "CP-6; CP-7; CP-7(5)");		
		capToRmf.put("COED-1", "CP-4; CP-4(1)");		
		capToRmf.put("COED-2", "CP-4; CP-4(1)");		
		capToRmf.put("COMS-1", "MA-6");		
		capToRmf.put("COMS-2", "MA-6");		
		capToRmf.put("COEF-1", "CP-2");		
		capToRmf.put("COEF-2", "CP-2");	
		capToRmf.put("COPS-1", "PE-11; PE-11(1)(2)");
		capToRmf.put("COPS-2", "PE-11; PE-11(1)(2)");
		capToRmf.put("COPS-3", "PE-11; PE-11(1)(2)");
		capToRmf.put("COSP-1", "MA-6");
		capToRmf.put("COSP-2", "MA-6");
		capToRmf.put("COSW-1", "CP-9; CP-9(3)");
		capToRmf.put("COTR-1", "CP-10; CP-10(3)");			
		capToRmf.put("DCAR-l", "CA-l; CA-2; CA-(2)");
		capToRmf.put("DCAR-l", "CA-l; CA-2; CA-(2)");
		capToRmf.put("DCAS-l", "SA-4; SA-4(6) (7)");
		capToRmf.put("DCBP-l", "SA-8");
		capToRmf.put("DCCB-l", "CM-l; CM-3");
		capToRmf.put("DCCB-2", "CM-l; CM-3; CM-3(4)");
		capToRmf.put("DCCS-l", "CM-6");
		capToRmf.put("DCCS-2", "CM-6");
		capToRmf.put("DCCT-l", "CM-4; CM-6; CM-6 (4); SI-2");
		capToRmf.put("DCDS-l", "SA-9; SA-9 (1)");
		capToRmf.put("DCFA-l", "PL-2; PL-2(2)");
		capToRmf.put("DCHW-l", "CM-2; CM-8; CP-9; CP-9 (3)");
		capToRmf.put("DCID-l", "CA-3");
		capToRmf.put("DCII-l", "CM-4; CM-4 (2)");
		capToRmf.put("DCIT-l", "PS-7; SA-9");
		capToRmf.put("DCMC-l", "SC-l8; SC-18(1)(2)(3)(4)(5)");
		capToRmf.put("DCNR-l", "AU-10; AU-10(5); SC-13; SC-13(4)");
		capToRmf.put("DCPA-l", "SC-2");
		capToRmf.put("DCPB-l", "SA-2");
		capToRmf.put("DCPD-l", "SA-6; SA-6(1)");
		capToRmf.put("DCPP-l", "CM-7; CM-7 (3)");
		capToRmf.put("DCPR-l", "CM-l; CM-3; CM-9");
		capToRmf.put("DCSD-l", "PL-l; PL-2; PM-1 0");
		capToRmf.put("DCSL-l", "CM-5; CM-5(6)");
		capToRmf.put("DCSP-l", "SC-3");
		capToRmf.put("DCSQ-l", "SA-4; SA-4(3); SA-11; SA-11(1)");
		capToRmf.put("DCSR-l", "SA-4; SA-4(7)");
		capToRmf.put("DCSR-2", "SA-4; SA-4(6)(7); SC-12; SC-12(2) (3)");
		capToRmf.put("DCSR-3", "SA-4; SA-4(6)(7); SC-12; SC-12(2) (3)");
		capToRmf.put("DCSS-l", "CM-6; SC-24; SI-6");
		capToRmf.put("DCSS-2", "CP-4; CP-4(4); SC-7; SC-7(18); SC-24; SI-6");
		capToRmf.put("DCSW-l", "CM-2; CM-8; CP-9; CP-9(3)");
		capToRmf.put("EBBD-l", "AC-4; SC-7; SC-7(2)");
		capToRmf.put("EBBD-2", "AC-4; SC-7; SC-7(8)(13)");
		capToRmf.put("EBBD-3", "AC-4; CA-3; CA-3 (1)(2)(4)(5); SC-7");
		capToRmf.put("EBCR-l", "CA-3");
		capToRmf.put("EBPW-l", "SC-7");
		capToRmf.put("EBRP-l", "AC-1 7; AC-17(2)(3)(4)(7); MA-4; MA-4 (1) (6)");
		capToRmf.put("EBRU-l", "AC-1 7;	AC-17(2)(3); I A-2; IA-2(2)");
		capToRmf.put("EBVC-l", "SI-4; SI-4(10)");
		capToRmf.put("ECAD-l", "AC-l6; IA-4; IA-4(4)"); 
		capToRmf.put("ECAN-l", "AC-2; AC-2(7); AC-3; AC-3(4)(7); AC-6");
		capToRmf.put("ECAR-l", "AU-2; AU-3; AU-8");
		capToRmf.put("ECAR-2", "AU-2; AU-3; AU-8");
		capToRmf.put("ECAR-3", "AU-2; AU-3; AU-8");
		capToRmf.put("ECAT-l", "AU-6; AU-6(1) (3)");
		capToRmf.put("ECAT-2", "AC-17; AC-17 (9); AU-6; AU-6(5); CM-8; CM-8(3); IR-4; IR-4(5); SI-4");
		capToRmf.put("ECCD-1", "AC-3; AC-6; SC-28");  
		capToRmf.put("ECCD-2", "AC-3; AC-3(3)(4); AC-6; CM-3; CM-3(1)"); 
		capToRmf.put("ECCM-1", "SC-1");
		capToRmf.put("ECCR-1", "MP-4; MP-4(1); SC-13; SC-13(1); SC-28; SC-28(1)"); 
		capToRmf.put("ECCR-2", "AC-3; AC-3(6); SC-13; SC-28; SC-28(1)"); 
		capToRmf.put("ECCR-3", "SC-13; SC-28; SC-28(1)"); 
		capToRmf.put("ECCT-1", "AC-17; AC-17(2); AC-18; AC-18(1); SC-8; SC-13"); 
		capToRmf.put("ECCT-2", "SC-8; SC-13"); 
		capToRmf.put("ECDC-1", "CP-10; CP-10(2)"); 
		capToRmf.put("ECIC-1", "AC-3; AC-3(4); AC-4; AC-4(14)(15)"); 
		capToRmf.put("ECID-1", "SC-7; SC-7(12); SI-4; SI-4(1)"); 
		capToRmf.put("ECIM-1", "SC-7; SC-7(19); SC-15; SC-15(2)"); 
		capToRmf.put("ECLC-1", "AC-4; AC-16, AU-3"); 
		capToRmf.put("ECLO-1", "AC-7; AC-10"); 
		capToRmf.put("ECLO-2", "AC-7; AC-9; AC-9(1); AC-9(2);AC-10"); 
		capToRmf.put("ECLP-1", "AC-5; AC-6; AC-6(2)"); 
		capToRmf.put("ECML-1", "AC-16; AC-16(1); AC-16(5); MP-3"); 
		capToRmf.put("ECMT-1", "CA-2; CA-2(2); RA-5"); 
		capToRmf.put("ECMT-2", "CA-2; CA-2(2); CA-7; RA-5"); 
		capToRmf.put("ECND-1", "CP-10"); 
		capToRmf.put("ECND-2", "AU-2; AU-2(3); CP-10"); 
		capToRmf.put("ECNK-1", "SC-8; SC-13; SC-13(3)"); 
		capToRmf.put("ECNK-2", "SC-8; SC-13; SC-13(2)"); 
		capToRmf.put("ECPA-1", "AC-2; AC-2(7)"); 
		capToRmf.put("ECPC-1", "CM-5; CM-5(5)"); 
		capToRmf.put("ECPC-2", "CM-5; CM-5(5)"); 
		capToRmf.put("ECRC-1", "SC-4"); 
		capToRmf.put("ECRG-1", "AU-7"); 
		capToRmf.put("ECRR-1", "AU-11"); 
		capToRmf.put("ECSC-1", "CM-6"); 
		capToRmf.put("ECSD-1", "CM-5; SA-10"); 
		capToRmf.put("ECSD-2", "CM-5; SA-10; SI-7"); 
		capToRmf.put("ECTB-1", "AU-9; AU-9(2)"); 
		capToRmf.put("ECTC-1", "PE-19; PE-19(1)"); 
		capToRmf.put("ECTM-1", "SC-8; SC-8(2); SC-16; SC-16(1); SI-7, SI-7(1)(12)"); 
		capToRmf.put("ECTM-2", "SC-8; SC-8(2); SC-16; SC-16(1); SC-23; SI-7, SI-7(1)(12)"); 
		capToRmf.put("ECTP-1", "AU-9"); 
		capToRmf.put("ECVI-1", "SC-19"); 
		capToRmf.put("ECVP-1", "SI-3"); 
		capToRmf.put("ECWM-1", "AC-8"); 
		capToRmf.put("ECWN-1", "AC-18; AC-18(1)(3)(4); AC-19"); 
		capToRmf.put("IAAC-1", "AC-2; PS-4; PS-5"); 
		capToRmf.put("IAGA-1", "IA-2; IA-2(5)"); 
		capToRmf.put("IAIA-1", "IA-2; IA-4; IA-4(2); IA-5; IA-5(1)(3)(5)(6)(7)"); 
		capToRmf.put("IAIA-2", "IA-2; IA-4; IA-4(2)(3); IA-5; IA-5(1)(3)(4)(5)(6)(7)"); 
		capToRmf.put("IAKM-1", "SC-12; SC-12-(2)(4)"); 
		capToRmf.put("IAKM-2", "SC-12; SC-12-(2)(5)"); 
		capToRmf.put("IAKM-3", "SC-12; SC-12(3)"); 
		capToRmf.put("IATS-1", "IA-5; IA-5(2); SC-12; SC-12-(4)(5)"); 
		capToRmf.put("IATS-2", "IA-5; IA-5(2); SC-12; SC-12-(4)(5)"); 
		capToRmf.put("PECF-1", "PE-2; PE-2(1); PE-3; PE-7"); 
		capToRmf.put("PECF-2", "PE-2; PE-2(3); PE-3; PE-7"); 
		capToRmf.put("PECS-1", "MP-6; MP-6(1)(4)"); 
		capToRmf.put("PECS-2", "MA-2; MP-6; MP-6(1)(5)"); 
		capToRmf.put("PEDD-1", "MP-6"); 
		capToRmf.put("PEDI-1", "PE-5");
		capToRmf.put("PEEL-1", "PE-12");
		capToRmf.put("PEEL-2", "PE-12; PE-12(1)");
		capToRmf.put("PEFD-1", "PE-13");
		capToRmf.put("PEFD-2", "PE-13; PE-13(1)(2)");
		capToRmf.put("PEFI-1", "PE-13; PE-13(4)");
		capToRmf.put("PEFS-1", "PE-13");
		capToRmf.put("PEFS-2", "PE-13; PE-13(1)(2)");
		capToRmf.put("PEHC-1", "PE-14; PE-14(1)(2)");
		capToRmf.put("PEHC-2", "PE-14; PE-14(1)");
		capToRmf.put("PEMS-1", "PE-10");
		capToRmf.put("PEPF-1", "PE-3; PE-3(3)");
		capToRmf.put("PEPF-2", "PE-2; PE-2(2); PE-3; PE-3(3); PE-8");
		capToRmf.put("PEPS-1", "PE-3; PE-3(6)");
		capToRmf.put("PESL-1", "AC-11; AC-11(1)");
		capToRmf.put("PESP-1", "MP-1; MP-2; SI-12");
		capToRmf.put("PES5-1", "MP-4");
		capToRmf.put("PETC-1", "PE-14; PE-14(2)");
		capToRmf.put("PETC-2", "PE-14; PE-14(1)");
		capToRmf.put("PETN-1", "AT-3; AT-3(1)");
		capToRmf.put("PEVC-1", "PE-3; PE-7; PE-8");
		capToRmf.put("PEVR-1", "PE-9; PE-9(2)");
		capToRmf.put("PRAS-1", "PS-1; PS-6");
		capToRmf.put("PRAS-2", "PS-2; PS-6; PS-6(2)"); 
		capToRmf.put("PRMP-1", "MA-5"); 
		capToRmf.put("PRMP-2", "MA-5; MA-5(1)(2)(3)"); 
		capToRmf.put("PRNK-1", "PS-6; PS-6(1)(2)");
		capToRmf.put("PRRB-1", "PL-4; P5-8");
		capToRmf.put("PRTN-1", "AT-2; AT-3; CM-9; CM-9(1); CP-3; IR-2");
		capToRmf.put("VIIR-1", "IR-2; IR-3; IR-4; IR-5; IR-6; IR-7; IR-7(2); IR-8"); 
		capToRmf.put("VIIR-2", "IR-2; IR-3; IR-4; IR-5; IR-6; IR-7; IR-7(2); IR-8"); 
		capToRmf.put("VIVM-1", "CA-8; RA-5; 5I-2; SI-2(1)(2)"); 
 
		cciToRmf.put("CCI-001545", "AC-1 b 1");
		cciToRmf.put("CCI-001546", "AC-1 b 2");
		cciToRmf.put("CCI-000001", "AC-1 a 1");
		cciToRmf.put("CCI-000004", "AC-1 a 2");
		cciToRmf.put("CCI-000002", "AC-1 a 1");
		cciToRmf.put("CCI-000003", "AC-1 b 1");
		cciToRmf.put("CCI-000005", "AC-1 a 2");
		cciToRmf.put("CCI-000006", "AC-1 b 2");
		cciToRmf.put("CCI-000054", "AC-10");
		cciToRmf.put("CCI-000055", "AC-10");
		cciToRmf.put("CCI-000056", "AC-11 b");
		cciToRmf.put("CCI-000057", "AC-11 a");
		cciToRmf.put("CCI-000058", "AC-11 a");
		cciToRmf.put("CCI-000059", "AC-11 a");
		cciToRmf.put("CCI-000060", "AC-11 (1)");
		cciToRmf.put("CCI-000061", "AC-14 a");
		cciToRmf.put("CCI-000232", "AC-14 b");
		cciToRmf.put("CCI-001559", "AC-16 (2)");
		cciToRmf.put("CCI-001560", "AC-16 (4)");
		cciToRmf.put("CCI-001424", "AC-16 (1)");
		cciToRmf.put("CCI-001425", "AC-16 (2)");
		cciToRmf.put("CCI-001428", "AC-16 (5)");
		cciToRmf.put("CCI-001429", "AC-16 (5)");
		cciToRmf.put("CCI-001430", "AC-16 (5)");
		cciToRmf.put("CCI-001561", "AC-17 (3)");
		cciToRmf.put("CCI-000063", "AC-17 a");
		cciToRmf.put("CCI-000065", "AC-17 b");
		cciToRmf.put("CCI-000067", "AC-17 (1)");
		cciToRmf.put("CCI-000068", "AC-17 (2)");
		cciToRmf.put("CCI-000069", "AC-17 (3)");
		cciToRmf.put("CCI-000070", "AC-17 (4) (a)");
		cciToRmf.put("CCI-000072", "AC-17 (6)");
		cciToRmf.put("CCI-001453", "AC-17 (2)");
		cciToRmf.put("CCI-001438", "AC-18 a");
		cciToRmf.put("CCI-001439", "AC-18 a");
		cciToRmf.put("CCI-001441", "AC-18 b");
		cciToRmf.put("CCI-001443", "AC-18 (1)");
		cciToRmf.put("CCI-001444", "AC-18 (1)");
		cciToRmf.put("CCI-001449", "AC-18 (3)");
		cciToRmf.put("CCI-001451", "AC-18 (5)");
		cciToRmf.put("CCI-000082", "AC-19 a");
		cciToRmf.put("CCI-000083", "AC-19 a");
		cciToRmf.put("CCI-000084", "AC-19 b");
		cciToRmf.put("CCI-001458", "AC-19 (4) (b) (4)");
		cciToRmf.put("CCI-001330", "AC-19 (4) (a)");
		cciToRmf.put("CCI-001331", "AC-19 (4) (b) (1)");
		cciToRmf.put("CCI-001332", "AC-19 (4) (b) (2)");
		cciToRmf.put("CCI-001333", "AC-19 (4) (b) (3)");
		cciToRmf.put("CCI-001334", "AC-19 (4) (b) (4)");
		cciToRmf.put("CCI-001335", "AC-19 (4) (b) (4)");
		cciToRmf.put("CCI-001547", "AC-2 j");
		cciToRmf.put("CCI-000008", "AC-2 c");
		cciToRmf.put("CCI-000010", "AC-2 e");
		cciToRmf.put("CCI-000011", "AC-2 f");
		cciToRmf.put("CCI-000012", "AC-2 j");
		cciToRmf.put("CCI-000015", "AC-2 (1)");
		cciToRmf.put("CCI-000016", "AC-2 (2)");
		cciToRmf.put("CCI-000017", "AC-2 (3)");
		cciToRmf.put("CCI-000018", "AC-2 (4)");
		cciToRmf.put("CCI-000019", "AC-2 (5)");
		cciToRmf.put("CCI-001361", "AC-2 (2)");
		cciToRmf.put("CCI-001365", "AC-2 (2)");
		cciToRmf.put("CCI-000217", "AC-2 (3)");
		cciToRmf.put("CCI-001403", "AC-2 (4)");
		cciToRmf.put("CCI-001404", "AC-2 (4)");
		cciToRmf.put("CCI-001405", "AC-2 (4)");
		cciToRmf.put("CCI-001406", "AC-2 (5)");
		cciToRmf.put("CCI-001407", "AC-2 (7) (a)");
		cciToRmf.put("CCI-001358", "AC-2 (7) (a)");
		cciToRmf.put("CCI-001360", "AC-2 (7) (b)");
		cciToRmf.put("CCI-001682", "AC-2 (2)");
		cciToRmf.put("CCI-001683", "AC-2 (4)");
		cciToRmf.put("CCI-001684", "AC-2 (4)");
		cciToRmf.put("CCI-001685", "AC-2 (4)");
		cciToRmf.put("CCI-001686", "AC-2 (4)");
		cciToRmf.put("CCI-000093", "AC-20 a");
		cciToRmf.put("CCI-000097", "AC-20 (2)");
		cciToRmf.put("CCI-000098", "AC-21 a");
		cciToRmf.put("CCI-000099", "AC-21 (1)");
		cciToRmf.put("CCI-001470", "AC-21 a");
		cciToRmf.put("CCI-001471", "AC-21 b");
		cciToRmf.put("CCI-001472", "AC-21 b");
		cciToRmf.put("CCI-001473", "AC-22 a");
		cciToRmf.put("CCI-001474", "AC-22 b");
		cciToRmf.put("CCI-001475", "AC-22 c");
		cciToRmf.put("CCI-001476", "AC-22 d");
		cciToRmf.put("CCI-001477", "AC-22 d");
		cciToRmf.put("CCI-001478", "AC-22 d");
		cciToRmf.put("CCI-000021", "AC-3 (2)");
		cciToRmf.put("CCI-000024", "AC-3 (5)");
		cciToRmf.put("CCI-000213", "AC-3");
		cciToRmf.put("CCI-001408", "AC-3 (2)");
		cciToRmf.put("CCI-001411", "AC-3 (5)");
		cciToRmf.put("CCI-001548", "AC-4");
		cciToRmf.put("CCI-001549", "AC-4");
		cciToRmf.put("CCI-001550", "AC-4");
		cciToRmf.put("CCI-001551", "AC-4");
		cciToRmf.put("CCI-001553", "AC-4 (10)");
		cciToRmf.put("CCI-001554", "AC-4 (11)");
		cciToRmf.put("CCI-000026", "AC-4 (2)");
		cciToRmf.put("CCI-000027", "AC-4 (3)");
		cciToRmf.put("CCI-000028", "AC-4 (4)");
		cciToRmf.put("CCI-000029", "AC-4 (5)");
		cciToRmf.put("CCI-000030", "AC-4 (6)");
		cciToRmf.put("CCI-000031", "AC-4 (7)");
		cciToRmf.put("CCI-000032", "AC-4 (8)");
		cciToRmf.put("CCI-000034", "AC-4 (10)");
		cciToRmf.put("CCI-000035", "AC-4 (11)");
		cciToRmf.put("CCI-000219", "AC-4 (13)");
		cciToRmf.put("CCI-001414", "AC-4");
		cciToRmf.put("CCI-001415", "AC-4 (5)");
		cciToRmf.put("CCI-001416", "AC-4 (7)");
		cciToRmf.put("CCI-001417", "AC-4 (8)");
		cciToRmf.put("CCI-001368", "AC-4");
		cciToRmf.put("CCI-001371", "AC-4 (14)");
		cciToRmf.put("CCI-001372", "AC-4 (14)");
		cciToRmf.put("CCI-001373", "AC-4 (15)");
		cciToRmf.put("CCI-001374", "AC-4 (15)");
		cciToRmf.put("CCI-000036", "AC-5 a");
		cciToRmf.put("CCI-001380", "AC-5 b");
		cciToRmf.put("CCI-001558", "AC-6 (1)");
		cciToRmf.put("CCI-000039", "AC-6 (2)");
		cciToRmf.put("CCI-000041", "AC-6 (3)");
		cciToRmf.put("CCI-000042", "AC-6 (3)");
		cciToRmf.put("CCI-000225", "AC-6");
		cciToRmf.put("CCI-001419", "AC-6 (2)");
		cciToRmf.put("CCI-001420", "AC-6 (3)");
		cciToRmf.put("CCI-001422", "AC-6 (6)");
		cciToRmf.put("CCI-000043", "AC-7");
		cciToRmf.put("CCI-000044", "AC-7 a");
		cciToRmf.put("CCI-001423", "AC-7");
		cciToRmf.put("CCI-000048", "AC-8 a");
		cciToRmf.put("CCI-000050", "AC-8 b");
		cciToRmf.put("CCI-001384", "AC-8 c 1");
		cciToRmf.put("CCI-001385", "AC-8 c 2");
		cciToRmf.put("CCI-001386", "AC-8 c 2");
		cciToRmf.put("CCI-001387", "AC-8 c 2");
		cciToRmf.put("CCI-001388", "AC-8 c 3");
		cciToRmf.put("CCI-000052", "AC-9");
		cciToRmf.put("CCI-000053", "AC-9 (1)");
		cciToRmf.put("CCI-001389", "AC-9 (2)");
		cciToRmf.put("CCI-001390", "AC-9 (2)");
		cciToRmf.put("CCI-001391", "AC-9 (2)");
		cciToRmf.put("CCI-001392", "AC-9 (2)");
		cciToRmf.put("CCI-001393", "AC-9 (3)");
		cciToRmf.put("CCI-001394", "AC-9 (3)");
		cciToRmf.put("CCI-001395", "AC-9 (3)");
		cciToRmf.put("CCI-001564", "AT-1 b 1");
		cciToRmf.put("CCI-001565", "AT-1 b 2");
		cciToRmf.put("CCI-000100", "AT-1 a 1");
		cciToRmf.put("CCI-000101", "AT-1 a 1");
		cciToRmf.put("CCI-000102", "AT-1 b 1");
		cciToRmf.put("CCI-000103", "AT-1 a 2");
		cciToRmf.put("CCI-000104", "AT-1 a 2");
		cciToRmf.put("CCI-000105", "AT-1 b 2");
		cciToRmf.put("CCI-000106", "AT-2 a");
		cciToRmf.put("CCI-000107", "AT-2 (1)");
		cciToRmf.put("CCI-000112", "AT-2 b");
		cciToRmf.put("CCI-001479", "AT-2 c");
		cciToRmf.put("CCI-001480", "AT-2");
		cciToRmf.put("CCI-001566", "AT-3 (2)");
		cciToRmf.put("CCI-001567", "AT-3 (2)");
		cciToRmf.put("CCI-001568", "AT-3 (2)");
		cciToRmf.put("CCI-000108", "AT-3 a");
		cciToRmf.put("CCI-000109", "AT-3 b");
		cciToRmf.put("CCI-000110", "AT-3 c");
		cciToRmf.put("CCI-000111", "AT-3 c");
		cciToRmf.put("CCI-001481", "AT-3 (1)");
		cciToRmf.put("CCI-001482", "AT-3 (1)");
		cciToRmf.put("CCI-001483", "AT-3 (1)");
		cciToRmf.put("CCI-000113", "AT-4 a");
		cciToRmf.put("CCI-000114", "AT-4 a");
		cciToRmf.put("CCI-001336", "AT-4 b");
		cciToRmf.put("CCI-001337", "AT-4 b");
		cciToRmf.put("CCI-001569", "AU-1 b 1");
		cciToRmf.put("CCI-001570", "AU-1 b 2");
		cciToRmf.put("CCI-000117", "AU-1 a 1");
		cciToRmf.put("CCI-000119", "AU-1 b 1");
		cciToRmf.put("CCI-000120", "AU-1 a 2");
		cciToRmf.put("CCI-000122", "AU-1 b 2");
		cciToRmf.put("CCI-000166", "AU-10");
		cciToRmf.put("CCI-001340", "AU-10 (3)");
		cciToRmf.put("CCI-001341", "AU-10 (4) (a)");
		cciToRmf.put("CCI-000167", "AU-11");
		cciToRmf.put("CCI-000168", "AU-11");
		cciToRmf.put("CCI-001576", "AU-12 (1)");
		cciToRmf.put("CCI-001577", "AU-12 (1)");
		cciToRmf.put("CCI-000169", "AU-12 a");
		cciToRmf.put("CCI-000171", "AU-12 b");
		cciToRmf.put("CCI-000172", "AU-12 c");
		cciToRmf.put("CCI-000173", "AU-12 (1)");
		cciToRmf.put("CCI-000174", "AU-12 (1)");
		cciToRmf.put("CCI-001459", "AU-12 a");
		cciToRmf.put("CCI-001353", "AU-12 (2)");
		cciToRmf.put("CCI-001460", "AU-13");
		cciToRmf.put("CCI-001461", "AU-13");
		cciToRmf.put("CCI-001462", "AU-14 (2)");
		cciToRmf.put("CCI-001464", "AU-14 (1)");
		cciToRmf.put("CCI-001571", "AU-2 a");
		cciToRmf.put("CCI-000123", "AU-2 a");
		cciToRmf.put("CCI-000124", "AU-2 b");
		cciToRmf.put("CCI-000125", "AU-2 c");
		cciToRmf.put("CCI-000126", "AU-2 d");
		cciToRmf.put("CCI-000127", "AU-2 (3)");
		cciToRmf.put("CCI-001484", "AU-2 d");
		cciToRmf.put("CCI-001485", "AU-2 d");
		cciToRmf.put("CCI-001486", "AU-2 (3)");
		cciToRmf.put("CCI-000130", "AU-3");
		cciToRmf.put("CCI-000131", "AU-3");
		cciToRmf.put("CCI-000132", "AU-3");
		cciToRmf.put("CCI-000133", "AU-3");
		cciToRmf.put("CCI-000134", "AU-3");
		cciToRmf.put("CCI-000135", "AU-3 (1)");
		cciToRmf.put("CCI-001487", "AU-3");
		cciToRmf.put("CCI-001488", "AU-3 (1)");
		cciToRmf.put("CCI-001572", "AU-5 a");
		cciToRmf.put("CCI-001573", "AU-5 (3)");
		cciToRmf.put("CCI-000139", "AU-5 a");
		cciToRmf.put("CCI-000140", "AU-5 b");
		cciToRmf.put("CCI-000145", "AU-5 (3)");
		cciToRmf.put("CCI-000147", "AU-5 (2)");
		cciToRmf.put("CCI-001490", "AU-5 b");
		cciToRmf.put("CCI-000148", "AU-6 a");
		cciToRmf.put("CCI-000149", "AU-6 b");
		cciToRmf.put("CCI-000151", "AU-6 a");
		cciToRmf.put("CCI-000153", "AU-6 (3)");
		cciToRmf.put("CCI-000154", "AU-6 (4)");
		cciToRmf.put("CCI-001491", "AU-6 (6)");
		cciToRmf.put("CCI-000158", "AU-7 (1)");
		cciToRmf.put("CCI-000159", "AU-8 a");
		cciToRmf.put("CCI-000161", "AU-8 (1) (a)");
		cciToRmf.put("CCI-001492", "AU-8 (1) (a)");
		cciToRmf.put("CCI-001575", "AU-9 (2)");
		cciToRmf.put("CCI-000162", "AU-9");
		cciToRmf.put("CCI-000163", "AU-9");
		cciToRmf.put("CCI-000164", "AU-9");
		cciToRmf.put("CCI-000165", "AU-9 (1)");
		cciToRmf.put("CCI-001348", "AU-9 (2)");
		cciToRmf.put("CCI-001349", "AU-9 (2)");
		cciToRmf.put("CCI-001350", "AU-9 (3)");
		cciToRmf.put("CCI-001351", "AU-9 (4)");
		cciToRmf.put("CCI-001493", "AU-9");
		cciToRmf.put("CCI-001494", "AU-9");
		cciToRmf.put("CCI-001495", "AU-9");
		cciToRmf.put("CCI-001496", "AU-9 (3)");
		cciToRmf.put("CCI-001578", "CA-1 b 2");
		cciToRmf.put("CCI-000238", "CA-1 b 1");
		cciToRmf.put("CCI-000239", "CA-1 a 1");
		cciToRmf.put("CCI-000240", "CA-1 a 1");
		cciToRmf.put("CCI-000240", "CA-1 a 1");
		cciToRmf.put("CCI-000241", "CA-1 b 1");
		cciToRmf.put("CCI-000242", "CA-1 a 2");
		cciToRmf.put("CCI-000243", "CA-1 a 2");
		cciToRmf.put("CCI-000244", "CA-1 b 2");
		cciToRmf.put("CCI-000245", "CA-2 a");
		cciToRmf.put("CCI-000246", "CA-2 a 1");
		cciToRmf.put("CCI-000247", "CA-2 a 2");
		cciToRmf.put("CCI-000248", "CA-2 a 3");
		cciToRmf.put("CCI-000251", "CA-2 b");
		cciToRmf.put("CCI-000252", "CA-2 b");
		cciToRmf.put("CCI-000253", "CA-2 c");
		cciToRmf.put("CCI-000254", "CA-2 d");
		cciToRmf.put("CCI-000255", "CA-2 (1)");
		cciToRmf.put("CCI-000256", "CA-2 (2)");
		cciToRmf.put("CCI-000257", "CA-3 a");
		cciToRmf.put("CCI-000258", "CA-3 b");
		cciToRmf.put("CCI-000259", "CA-3 b");
		cciToRmf.put("CCI-000260", "CA-3 b");
		cciToRmf.put("CCI-000262", "CA-3 (1)");
		cciToRmf.put("CCI-000263", "CA-3 (2)");
		cciToRmf.put("CCI-000264", "CA-5 a");
		cciToRmf.put("CCI-000265", "CA-5 b");
		cciToRmf.put("CCI-000266", "CA-5 b");
		cciToRmf.put("CCI-000267", "CA-5 (1)");
		cciToRmf.put("CCI-000268", "CA-5 (1)");
		cciToRmf.put("CCI-000269", "CA-5 (1)");
		cciToRmf.put("CCI-000270", "CA-6 a");
		cciToRmf.put("CCI-000271", "CA-6 b");
		cciToRmf.put("CCI-000272", "CA-6 c");
		cciToRmf.put("CCI-000273", "CA-6 c");
		cciToRmf.put("CCI-001581", "CA-7 g");
		cciToRmf.put("CCI-001582", "CA-2 (2)");
		cciToRmf.put("CCI-001583", "CA-2 (2)");
		cciToRmf.put("CCI-000274", "CA-7");
		cciToRmf.put("CCI-000279", "CA-7 c");
		cciToRmf.put("CCI-000280", "CA-7 g");
		cciToRmf.put("CCI-000281", "CA-7 g");
		cciToRmf.put("CCI-000282", "CA-7 (1)");
		cciToRmf.put("CCI-001681", "CA-2 (2)");
		cciToRmf.put("CCI-001584", "CM-1 b 2");
		cciToRmf.put("CCI-000286", "CM-1 b 1");
		cciToRmf.put("CCI-000287", "CM-1 a 1");
		cciToRmf.put("CCI-000289", "CM-1 b 1");
		cciToRmf.put("CCI-000290", "CM-1 a 2");
		cciToRmf.put("CCI-000292", "CM-1 b 2");
		cciToRmf.put("CCI-001585", "CM-2 (1) (b)");
		cciToRmf.put("CCI-000293", "CM-2");
		cciToRmf.put("CCI-000294", "CM-2");
		cciToRmf.put("CCI-000295", "CM-2");
		cciToRmf.put("CCI-000296", "CM-2 (1) (a)");
		cciToRmf.put("CCI-000297", "CM-2 (1) (b)");
		cciToRmf.put("CCI-000298", "CM-2 (1) (c)");
		cciToRmf.put("CCI-000299", "CM-2 (1) (c)");
		cciToRmf.put("CCI-000300", "CM-2 (2)");
		cciToRmf.put("CCI-000301", "CM-2 (2)");
		cciToRmf.put("CCI-000302", "CM-2 (2)");
		cciToRmf.put("CCI-000303", "CM-2 (2)");
		cciToRmf.put("CCI-000304", "CM-2 (3)");
		cciToRmf.put("CCI-000311", "CM-2 (6)");
		cciToRmf.put("CCI-000312", "CM-2 (6)");
		cciToRmf.put("CCI-001497", "CM-2 (1) (a)");
		cciToRmf.put("CCI-001586", "CM-3 g");
		cciToRmf.put("CCI-000313", "CM-3 a");
		cciToRmf.put("CCI-000314", "CM-3 b");
		cciToRmf.put("CCI-000316", "CM-3 e");
		cciToRmf.put("CCI-000318", "CM-3 f");
		cciToRmf.put("CCI-000319", "CM-3 g");
		cciToRmf.put("CCI-000320", "CM-3 g");
		cciToRmf.put("CCI-000321", "CM-3 g");
		cciToRmf.put("CCI-000322", "CM-3 (1) (a)");
		cciToRmf.put("CCI-000323", "CM-3 (1) (b)");
		cciToRmf.put("CCI-000324", "CM-3 (1) (c)");
		cciToRmf.put("CCI-000325", "CM-3 (1) (d)");
		cciToRmf.put("CCI-000326", "CM-3 (1) (e)");
		cciToRmf.put("CCI-000327", "CM-3 (2)");
		cciToRmf.put("CCI-000328", "CM-3 (2)");
		cciToRmf.put("CCI-000329", "CM-3 (2)");
		cciToRmf.put("CCI-000330", "CM-3 (3)");
		cciToRmf.put("CCI-000331", "CM-3 (3)");
		cciToRmf.put("CCI-000332", "CM-3 (4)");
		cciToRmf.put("CCI-001498", "CM-3 (1) (c)");
		cciToRmf.put("CCI-000333", "CM-4");
		cciToRmf.put("CCI-000335", "CM-4 (2)");
		cciToRmf.put("CCI-000336", "CM-4 (2)");
		cciToRmf.put("CCI-000337", "CM-4 (2)");
		cciToRmf.put("CCI-000338", "CM-5");
		cciToRmf.put("CCI-000339", "CM-5");
		cciToRmf.put("CCI-000340", "CM-5");
		cciToRmf.put("CCI-000341", "CM-5");
		cciToRmf.put("CCI-000342", "CM-5");
		cciToRmf.put("CCI-000343", "CM-5");
		cciToRmf.put("CCI-000344", "CM-5");
		cciToRmf.put("CCI-000345", "CM-5");
		cciToRmf.put("CCI-000348", "CM-5 (2)");
		cciToRmf.put("CCI-000349", "CM-5 (2)");
		cciToRmf.put("CCI-000350", "CM-5 (2)");
		cciToRmf.put("CCI-000353", "CM-5 (4)");
		cciToRmf.put("CCI-000354", "CM-5 (4)");
		cciToRmf.put("CCI-001499", "CM-5 (6)");
		cciToRmf.put("CCI-001588", "CM-6 a");
		cciToRmf.put("CCI-000363", "CM-6 a");
		cciToRmf.put("CCI-000364", "CM-6 a");
		cciToRmf.put("CCI-000365", "CM-6 a");
		cciToRmf.put("CCI-000366", "CM-6 b");
		cciToRmf.put("CCI-000367", "CM-6 c");
		cciToRmf.put("CCI-000368", "CM-6 c");
		cciToRmf.put("CCI-000369", "CM-6 c");
		cciToRmf.put("CCI-000370", "CM-6 (1)");
		cciToRmf.put("CCI-000371", "CM-6 (1)");
		cciToRmf.put("CCI-000372", "CM-6 (1)");
		cciToRmf.put("CCI-001502", "CM-6 d");
		cciToRmf.put("CCI-001503", "CM-6 d");
		cciToRmf.put("CCI-001592", "CM-7 (2)");
		cciToRmf.put("CCI-000380", "CM-7 b");
		cciToRmf.put("CCI-000381", "CM-7 a");
		cciToRmf.put("CCI-000382", "CM-7 b");
		cciToRmf.put("CCI-000384", "CM-7 (1) (a)");
		cciToRmf.put("CCI-000387", "CM-7 (3)");
		cciToRmf.put("CCI-000388", "CM-7 (3)");
		cciToRmf.put("CCI-000389", "CM-8 a 1");
		cciToRmf.put("CCI-000390", "CM-8 a 1");
		cciToRmf.put("CCI-000392", "CM-8 a 2");
		cciToRmf.put("CCI-000393", "CM-8 a 2");
		cciToRmf.put("CCI-000395", "CM-8 a 3");
		cciToRmf.put("CCI-000396", "CM-8 a 3");
		cciToRmf.put("CCI-000398", "CM-8 a 4");
		cciToRmf.put("CCI-000399", "CM-8 a 4");
		cciToRmf.put("CCI-000400", "CM-8 a 4");
		cciToRmf.put("CCI-000408", "CM-8 (1)");
		cciToRmf.put("CCI-000409", "CM-8 (1)");
		cciToRmf.put("CCI-000410", "CM-8 (1)");
		cciToRmf.put("CCI-000411", "CM-8 (2)");
		cciToRmf.put("CCI-000412", "CM-8 (2)");
		cciToRmf.put("CCI-000413", "CM-8 (2)");
		cciToRmf.put("CCI-000414", "CM-8 (2)");
		cciToRmf.put("CCI-000415", "CM-8 (3) (a)");
		cciToRmf.put("CCI-000416", "CM-8 (3) (a)");
		cciToRmf.put("CCI-000418", "CM-8 (4)");
		cciToRmf.put("CCI-000419", "CM-8 (5)");
		cciToRmf.put("CCI-000420", "CM-8 (6)");
		cciToRmf.put("CCI-000421", "CM-9 a");
		cciToRmf.put("CCI-000422", "CM-9 a");
		cciToRmf.put("CCI-000423", "CM-9 a");
		cciToRmf.put("CCI-000424", "CM-9 c");
		cciToRmf.put("CCI-000425", "CM-9 c");
		cciToRmf.put("CCI-000426", "CM-9 c");
		cciToRmf.put("CCI-000436", "CM-9 (1)");
		cciToRmf.put("CCI-001596", "CP-1 b 2");
		cciToRmf.put("CCI-001597", "CP-1 a 2");
		cciToRmf.put("CCI-001598", "CP-1 b 2");
		cciToRmf.put("CCI-000437", "CP-1 b 1");
		cciToRmf.put("CCI-000438", "CP-1 a 1");
		cciToRmf.put("CCI-000439", "CP-1 a 1");
		cciToRmf.put("CCI-000440", "CP-1 b 1");
		cciToRmf.put("CCI-000441", "CP-1 a 2");
		cciToRmf.put("CCI-000550", "CP-10");
		cciToRmf.put("CCI-000551", "CP-10");
		cciToRmf.put("CCI-000552", "CP-10");
		cciToRmf.put("CCI-000553", "CP-10 (2)");
		cciToRmf.put("CCI-000556", "CP-10 (4)");
		cciToRmf.put("CCI-000557", "CP-10 (4)");
		cciToRmf.put("CCI-000558", "SI-13 (5)");
		cciToRmf.put("CCI-000559", "SI-13 (5)");
		cciToRmf.put("CCI-000560", "CP-10 (6)");
		cciToRmf.put("CCI-000561", "CP-10 (6)");
		cciToRmf.put("CCI-000562", "CP-10 (6)");
		cciToRmf.put("CCI-001599", "CP-2 (5)");
		cciToRmf.put("CCI-001600", "CP-2 (5)");
		cciToRmf.put("CCI-001601", "CP-2 (6)");
		cciToRmf.put("CCI-001602", "CP-2 (6)");
		cciToRmf.put("CCI-000443", "CP-2 a 1");
		cciToRmf.put("CCI-000444", "CP-2 a 1");
		cciToRmf.put("CCI-000445", "CP-2 a 1");
		cciToRmf.put("CCI-000446", "CP-2 a 2");
		cciToRmf.put("CCI-000447", "CP-2 a 2");
		cciToRmf.put("CCI-000448", "CP-2 a 2");
		cciToRmf.put("CCI-000449", "CP-2 a 3");
		cciToRmf.put("CCI-000450", "CP-2 a 4");
		cciToRmf.put("CCI-000451", "CP-2 a 4");
		cciToRmf.put("CCI-000452", "CP-2 a 4");
		cciToRmf.put("CCI-000453", "CP-2 a 4");
		cciToRmf.put("CCI-000454", "CP-2 a 4");
		cciToRmf.put("CCI-000455", "CP-2 a 4");
		cciToRmf.put("CCI-000456", "CP-2 a 5");
		cciToRmf.put("CCI-000457", "CP-2 a 6");
		cciToRmf.put("CCI-000458", "CP-2 b");
		cciToRmf.put("CCI-000459", "CP-2 b");
		cciToRmf.put("CCI-000460", "CP-2 c");
		cciToRmf.put("CCI-000461", "CP-2 d");
		cciToRmf.put("CCI-000462", "CP-2 d");
		cciToRmf.put("CCI-000463", "CP-2 e");
		cciToRmf.put("CCI-000464", "CP-2 e");
		cciToRmf.put("CCI-000465", "CP-2 e");
		cciToRmf.put("CCI-000466", "CP-2 e");
		cciToRmf.put("CCI-000468", "CP-2 f");
		cciToRmf.put("CCI-000469", "CP-2 (1)");
		cciToRmf.put("CCI-000470", "CP-2 (2)");
		cciToRmf.put("CCI-000471", "CP-2 (2)");
		cciToRmf.put("CCI-000472", "CP-2 (2)");
		cciToRmf.put("CCI-000473", "CP-2 (3)");
		cciToRmf.put("CCI-000474", "CP-2 (3)");
		cciToRmf.put("CCI-000475", "CP-2 (3)");
		cciToRmf.put("CCI-000476", "CP-2 (3)");
		cciToRmf.put("CCI-000477", "CP-2 (4)");
		cciToRmf.put("CCI-000478", "CP-2 (4)");
		cciToRmf.put("CCI-000479", "CP-2 (4)");
		cciToRmf.put("CCI-000480", "CP-2 (4)");
		cciToRmf.put("CCI-000481", "CP-2 (5)");
		cciToRmf.put("CCI-000482", "CP-2 (5)");
		cciToRmf.put("CCI-000483", "CP-2 (6)");
		cciToRmf.put("CCI-000484", "CP-2 (6)");
		cciToRmf.put("CCI-000485", "CP-3 c");
		cciToRmf.put("CCI-000486", "CP-3 a");
		cciToRmf.put("CCI-000487", "CP-3 c");
		cciToRmf.put("CCI-000488", "CP-3 (1)");
		cciToRmf.put("CCI-000489", "CP-3 (2)");
		cciToRmf.put("CCI-000490", "CP-4 a");
		cciToRmf.put("CCI-000492", "CP-4 a");
		cciToRmf.put("CCI-000494", "CP-4 a");
		cciToRmf.put("CCI-000496", "CP-4 b");
		cciToRmf.put("CCI-000497", "CP-4 c");
		cciToRmf.put("CCI-000498", "CP-4 (1)");
		cciToRmf.put("CCI-000500", "CP-4 (2) (a)");
		cciToRmf.put("CCI-000502", "CP-4 (3)");
		cciToRmf.put("CCI-000504", "CP-4 (4)4");
		cciToRmf.put("CCI-001604", "CP-6 (3)");
		cciToRmf.put("CCI-000505", "CP-6 a");
		cciToRmf.put("CCI-000507", "CP-6 (1)");
		cciToRmf.put("CCI-000508", "CP-6 (2)");
		cciToRmf.put("CCI-000509", "CP-6 (3)");
		cciToRmf.put("CCI-001606", "CP-7 (2)");
		cciToRmf.put("CCI-000510", "CP-7 a");
		cciToRmf.put("CCI-000513", "CP-7 a");
		cciToRmf.put("CCI-000514", "CP-7 a");
		cciToRmf.put("CCI-000515", "CP-7 b");
		cciToRmf.put("CCI-000516", "CP-7 (1)");
		cciToRmf.put("CCI-000517", "CP-7 (2)");
		cciToRmf.put("CCI-000518", "CP-7 (3)");
		cciToRmf.put("CCI-000519", "CP-7 (4)");
		cciToRmf.put("CCI-000520", "CP-7 (4)");
		cciToRmf.put("CCI-000521", "CP-7 c");
		cciToRmf.put("CCI-000522", "CP-8");
		cciToRmf.put("CCI-000523", "CP-8");
		cciToRmf.put("CCI-000524", "CP-8");
		cciToRmf.put("CCI-000525", "CP-8");
		cciToRmf.put("CCI-000526", "CP-8 (1) (a)");
		cciToRmf.put("CCI-000527", "CP-8 (1) (a)");
		cciToRmf.put("CCI-000528", "CP-8 (1) (b)");
		cciToRmf.put("CCI-000529", "CP-8 (1) (b)");
		cciToRmf.put("CCI-000530", "CP-8 (2)");
		cciToRmf.put("CCI-000531", "CP-8 (3)");
		cciToRmf.put("CCI-000532", "CP-8 (4) (a)");
		cciToRmf.put("CCI-000533", "CP-8 (4) (a)");
		cciToRmf.put("CCI-001609", "CP-9 (6)");
		cciToRmf.put("CCI-000534", "CP-9 (a)");
		cciToRmf.put("CCI-000535", "CP-9 (a)");
		cciToRmf.put("CCI-000536", "CP-9 (b)");
		cciToRmf.put("CCI-000537", "CP-9 (b)");
		cciToRmf.put("CCI-000538", "CP-9 (c)");
		cciToRmf.put("CCI-000539", "CP-9 (c)");
		cciToRmf.put("CCI-000540", "CP-9 (d)");
		cciToRmf.put("CCI-000541", "CP-9 (1)");
		cciToRmf.put("CCI-000542", "CP-9 (1)");
		cciToRmf.put("CCI-000543", "CP-9 (2)");
		cciToRmf.put("CCI-000547", "CP-9 (5)");
		cciToRmf.put("CCI-000548", "CP-9 (5)");
		cciToRmf.put("CCI-000549", "CP-9 (6)");
		cciToRmf.put("CCI-000756", "IA-1 a 1");
		cciToRmf.put("CCI-000757", "IA-1 a 1");
		cciToRmf.put("CCI-000758", "IA-1 b 1");
		cciToRmf.put("CCI-000759", "IA-1 b 1");
		cciToRmf.put("CCI-000760", "IA-1 a 2");
		cciToRmf.put("CCI-000761", "IA-1 a 2");
		cciToRmf.put("CCI-000762", "IA-1 b 2");
		cciToRmf.put("CCI-000763", "IA-1 b 2");
		cciToRmf.put("CCI-000764", "IA-2");
		cciToRmf.put("CCI-000765", "IA-2 (1)");
		cciToRmf.put("CCI-000766", "IA-2 (2)");
		cciToRmf.put("CCI-000767", "IA-2 (3)");
		cciToRmf.put("CCI-000768", "IA-2 (4)");
		cciToRmf.put("CCI-000770", "IA-2 (5)");
		cciToRmf.put("CCI-000777", "IA-3");
		cciToRmf.put("CCI-000778", "IA-3");
		cciToRmf.put("CCI-000783", "IA-3 (3) (b)");
		cciToRmf.put("CCI-000794", "IA-4 e");
		cciToRmf.put("CCI-000795", "IA-4 e");
		cciToRmf.put("CCI-000796", "IA-4 (1)");
		cciToRmf.put("CCI-000799", "IA-4 (3)");
		cciToRmf.put("CCI-000800", "IA-4 (4)");
		cciToRmf.put("CCI-000801", "IA-4 (4)");
		cciToRmf.put("CCI-001610", "IA-5 g");
		cciToRmf.put("CCI-001611", "IA-5 (1) (a)");
		cciToRmf.put("CCI-001612", "IA-5 (1) (a)");
		cciToRmf.put("CCI-001613", "IA-5 (1) (a)");
		cciToRmf.put("CCI-001614", "IA-5 (1) (a)");
		cciToRmf.put("CCI-001615", "IA-5 (1) (b)");
		cciToRmf.put("CCI-001616", "IA-5 (1) (d)");
		cciToRmf.put("CCI-001617", "IA-5 (1) (d)");
		cciToRmf.put("CCI-001618", "IA-5 (1) (e)");
		cciToRmf.put("CCI-001619", "IA-5 (1) (a)");
		cciToRmf.put("CCI-001621", "IA-5 (8)");
		cciToRmf.put("CCI-000176", "IA-5 b");
		cciToRmf.put("CCI-000179", "IA-5 f");
		cciToRmf.put("CCI-000180", "IA-5 f");
		cciToRmf.put("CCI-000181", "IA-5 f");
		cciToRmf.put("CCI-000182", "IA-5 g");
		cciToRmf.put("CCI-000183", "IA-5 h");
		cciToRmf.put("CCI-000184", "IA-5 i");
		cciToRmf.put("CCI-000185", "IA-5 (2) (a)");
		cciToRmf.put("CCI-000186", "IA-5 (2) (b)");
		cciToRmf.put("CCI-000187", "IA-5 (2) (c)");
		cciToRmf.put("CCI-000201", "IA-5 (6)");
		cciToRmf.put("CCI-000202", "IA-5 (7)");
		cciToRmf.put("CCI-000204", "IA-5 (8)");
		cciToRmf.put("CCI-000192", "IA-5 (1) (a)");
		cciToRmf.put("CCI-000193", "IA-5 (1) (a)");
		cciToRmf.put("CCI-000194", "IA-5 (1) (a)");
		cciToRmf.put("CCI-000195", "IA-5 (1) (b)");
		cciToRmf.put("CCI-000196", "IA-5 (1) (c)");
		cciToRmf.put("CCI-000197", "IA-5 (1) (c)");
		cciToRmf.put("CCI-000198", "IA-5 (1) (d)");
		cciToRmf.put("CCI-000199", "IA-5 (1) (d)");
		cciToRmf.put("CCI-000200", "IA-5 (1) (e)");
		cciToRmf.put("CCI-000203", "IA-5 (7)");
		cciToRmf.put("CCI-000205", "IA-5 (1) (a)");
		cciToRmf.put("CCI-001544", "IA-5 c");
		cciToRmf.put("CCI-000206", "IA-6");
		cciToRmf.put("CCI-000803", "IA-7");
		cciToRmf.put("CCI-000804", "IA-8");
		cciToRmf.put("CCI-000805", "IR-1 a 1");
		cciToRmf.put("CCI-000806", "IR-1 a 1");
		cciToRmf.put("CCI-000807", "IR-1 b 1");
		cciToRmf.put("CCI-000808", "IR-1 b 1");
		cciToRmf.put("CCI-000809", "IR-1 a 2");
		cciToRmf.put("CCI-000810", "IR-1 a 2");
		cciToRmf.put("CCI-000811", "IR-1 b 2");
		cciToRmf.put("CCI-000812", "IR-1 b 2");
		cciToRmf.put("CCI-000813", "IR-2 a");
		cciToRmf.put("CCI-000814", "IR-2 c");
		cciToRmf.put("CCI-000815", "IR-2 c");
		cciToRmf.put("CCI-000816", "IR-2 (1)");
		cciToRmf.put("CCI-000817", "IR-2 (2)");
		cciToRmf.put("CCI-001624", "IR-3");
		cciToRmf.put("CCI-000818", "IR-3");
		cciToRmf.put("CCI-000819", "IR-3");
		cciToRmf.put("CCI-000820", "IR-3");
		cciToRmf.put("CCI-000821", "IR-3 (1)");
		cciToRmf.put("CCI-001625", "IR-4 c");
		cciToRmf.put("CCI-000822", "IR-4 a");
		cciToRmf.put("CCI-000823", "IR-4 b");
		cciToRmf.put("CCI-000824", "IR-4 c");
		cciToRmf.put("CCI-000825", "IR-4 (1)");
		cciToRmf.put("CCI-000826", "IR-4 (2)");
		cciToRmf.put("CCI-000827", "IR-4 (3)");
		cciToRmf.put("CCI-000828", "IR-4 (3)");
		cciToRmf.put("CCI-000829", "IR-4 (4)");
		cciToRmf.put("CCI-000830", "IR-4 (5)");
		cciToRmf.put("CCI-000831", "IR-4 (5)");
		cciToRmf.put("CCI-001626", "IR-5 (1)");
		cciToRmf.put("CCI-001627", "IR-5 (1)");
		cciToRmf.put("CCI-000832", "IR-5");
		cciToRmf.put("CCI-000833", "IR-5 (1)");
		cciToRmf.put("CCI-000834", "IR-6 a");
		cciToRmf.put("CCI-000835", "IR-6 a");
		cciToRmf.put("CCI-000836", "IR-6 b");
		cciToRmf.put("CCI-000837", "IR-6 (1)");
		cciToRmf.put("CCI-000838", "IR-6 (2)");
		cciToRmf.put("CCI-000839", "IR-7");
		cciToRmf.put("CCI-000840", "IR-7 (1)");
		cciToRmf.put("CCI-000841", "IR-7 (2) (a)");
		cciToRmf.put("CCI-000842", "IR-7 (2) (b)");
		cciToRmf.put("CCI-000844", "IR-8 a 8");
		cciToRmf.put("CCI-000845", "IR-8 b");
		cciToRmf.put("CCI-000846", "IR-8 b");
		cciToRmf.put("CCI-000847", "IR-8 c");
		cciToRmf.put("CCI-000848", "IR-8 c");
		cciToRmf.put("CCI-000849", "IR-8 d");
		cciToRmf.put("CCI-000850", "IR-8 e");
		cciToRmf.put("CCI-001628", "MA-1 b 2");
		cciToRmf.put("CCI-000854", "MA-1 b 1");
		cciToRmf.put("CCI-000855", "MA-1 a 2");
		cciToRmf.put("CCI-000856", "MA-1 a 2");
		cciToRmf.put("CCI-000857", "MA-1 b 2");
		cciToRmf.put("CCI-000851", "MA-1 b 1");
		cciToRmf.put("CCI-000852", "MA-1 a 1");
		cciToRmf.put("CCI-000853", "MA-1 a 1");
		cciToRmf.put("CCI-000859", "MA-2 b");
		cciToRmf.put("CCI-000860", "MA-2 c");
		cciToRmf.put("CCI-000861", "MA-2 d");
		cciToRmf.put("CCI-000862", "MA-2 e");
		cciToRmf.put("CCI-000865", "MA-3");
		cciToRmf.put("CCI-000866", "MA-3");
		cciToRmf.put("CCI-000867", "MA-3");
		cciToRmf.put("CCI-000869", "MA-3 (1)");
		cciToRmf.put("CCI-000870", "MA-3 (2)");
		cciToRmf.put("CCI-000871", "MA-3 (3)");
		cciToRmf.put("CCI-001631", "MA-4 (3) (b)");
		cciToRmf.put("CCI-001632", "MA-4 (4) (b)");
		cciToRmf.put("CCI-000873", "MA-4 a");
		cciToRmf.put("CCI-000874", "MA-4 a");
		cciToRmf.put("CCI-000876", "MA-4 b");
		cciToRmf.put("CCI-000877", "MA-4 c");
		cciToRmf.put("CCI-000878", "MA-4 d");
		cciToRmf.put("CCI-000879", "MA-4 e");
		cciToRmf.put("CCI-000881", "MA-4 (2)");
		cciToRmf.put("CCI-000882", "MA-4 (3) (a)");
		cciToRmf.put("CCI-000883", "MA-4 (3) (b)");
		cciToRmf.put("CCI-000884", "MA-4 (4) (a)");
		cciToRmf.put("CCI-000886", "MA-4 (5) (b)");
		cciToRmf.put("CCI-000887", "MA-4 (5) (a)");
		cciToRmf.put("CCI-000890", "MA-5 a");
		cciToRmf.put("CCI-000891", "MA-5 a");
		cciToRmf.put("CCI-000893", "MA-5 (1) (a)");
		cciToRmf.put("CCI-000894", "MA-5 (1) (a) (1)");
		cciToRmf.put("CCI-000895", "MA-5 (1) (a) (2)");
		cciToRmf.put("CCI-000897", "MA-5 (2)");
		cciToRmf.put("CCI-000898", "MA-5 (3)");
		cciToRmf.put("CCI-000899", "MA-5 (4) (a)");
		cciToRmf.put("CCI-000900", "MA-5 (4) (b)");
		cciToRmf.put("CCI-000903", "MA-6");
		cciToRmf.put("CCI-000995", "MP-1 a 1");
		cciToRmf.put("CCI-000996", "MP-1 a 1");
		cciToRmf.put("CCI-000997", "MP-1 b 1");
		cciToRmf.put("CCI-000998", "MP-1 b 1");
		cciToRmf.put("CCI-000999", "MP-1 a 2");
		cciToRmf.put("CCI-001000", "MP-1 a 2");
		cciToRmf.put("CCI-001001", "MP-1 b 2");
		cciToRmf.put("CCI-001002", "MP-1 b 2");
		cciToRmf.put("CCI-001003", "MP-2");
		cciToRmf.put("CCI-001004", "MP-2");
		cciToRmf.put("CCI-001005", "MP-2");
		cciToRmf.put("CCI-001007", "MP-4 (2)");
		cciToRmf.put("CCI-001008", "MP-4 (2)");
		cciToRmf.put("CCI-001010", "MP-3 a");
		cciToRmf.put("CCI-001011", "MP-3 b");
		cciToRmf.put("CCI-001012", "MP-3 b");
		cciToRmf.put("CCI-001013", "MP-3 b");
		cciToRmf.put("CCI-001014", "MP-4 a");
		cciToRmf.put("CCI-001015", "MP-4 a");
		cciToRmf.put("CCI-001016", "MP-4 a");
		cciToRmf.put("CCI-001018", "MP-4 b");
		cciToRmf.put("CCI-001020", "MP-5 a");
		cciToRmf.put("CCI-001021", "MP-5 a");
		cciToRmf.put("CCI-001022", "MP-5 a");
		cciToRmf.put("CCI-001023", "MP-5 b");
		cciToRmf.put("CCI-001024", "MP-5 d");
		cciToRmf.put("CCI-001025", "MP-5 c");
		cciToRmf.put("CCI-001026", "MP-5 (3)");
		cciToRmf.put("CCI-001027", "MP-5 (4)");
		cciToRmf.put("CCI-001028", "MP-6 a");
		cciToRmf.put("CCI-001030", "MP-6 (2)");
		cciToRmf.put("CCI-001031", "MP-6 (2)");
		cciToRmf.put("CCI-001032", "MP-6 (3)");
		cciToRmf.put("CCI-001033", "MP-6 (3)");
		cciToRmf.put("CCI-000904", "PE-1 a 1");
		cciToRmf.put("CCI-000905", "PE-1 a 1");
		cciToRmf.put("CCI-000906", "PE-1 b 1");
		cciToRmf.put("CCI-000907", "PE-1 b 1");
		cciToRmf.put("CCI-000908", "PE-1 a 2");
		cciToRmf.put("CCI-000909", "PE-1 a 2");
		cciToRmf.put("CCI-000910", "PE-1 b 2");
		cciToRmf.put("CCI-000911", "PE-1 b 2");
		cciToRmf.put("CCI-000956", "PE-10 a");
		cciToRmf.put("CCI-000957", "PE-10 b");
		cciToRmf.put("CCI-000958", "PE-10 b");
		cciToRmf.put("CCI-000959", "PE-10 c");
		cciToRmf.put("CCI-000961", "PE-11 (1)");
		cciToRmf.put("CCI-000963", "PE-12");
		cciToRmf.put("CCI-000968", "PE-13 (3)");
		cciToRmf.put("CCI-000965", "PE-13");
		cciToRmf.put("CCI-000971", "PE-14 a");
		cciToRmf.put("CCI-000972", "PE-14 a");
		cciToRmf.put("CCI-000973", "PE-14 b");
		cciToRmf.put("CCI-000974", "PE-14 b");
		cciToRmf.put("CCI-000975", "PE-14 (1)");
		cciToRmf.put("CCI-000976", "PE-14 (2)");
		cciToRmf.put("CCI-000977", "PE-15");
		cciToRmf.put("CCI-000978", "PE-15");
		cciToRmf.put("CCI-000979", "PE-15");
		cciToRmf.put("CCI-000981", "PE-16");
		cciToRmf.put("CCI-000982", "PE-16");
		cciToRmf.put("CCI-000983", "PE-16");
		cciToRmf.put("CCI-000984", "PE-16");
		cciToRmf.put("CCI-000985", "PE-17 a");
		cciToRmf.put("CCI-000987", "PE-17 b");
		cciToRmf.put("CCI-000988", "PE-17 c");
		cciToRmf.put("CCI-000989", "PE-18");
		cciToRmf.put("CCI-000991", "PE-18");
		cciToRmf.put("CCI-000993", "PE-19");
		cciToRmf.put("CCI-000994", "PE-19 (1)");
		cciToRmf.put("CCI-001635", "PE-2 d");
		cciToRmf.put("CCI-000912", "PE-2 a");
		cciToRmf.put("CCI-000913", "PE-2 b");
		cciToRmf.put("CCI-000914", "PE-2 c");
		cciToRmf.put("CCI-000915", "PE-2 c");
		cciToRmf.put("CCI-000916", "PE-2 (1)");
		cciToRmf.put("CCI-000917", "PE-2 (2)");
		cciToRmf.put("CCI-000919", "PE-3 a");
		cciToRmf.put("CCI-000920", "PE-3 a 1");
		cciToRmf.put("CCI-000921", "PE-3 a 2");
		cciToRmf.put("CCI-000923", "PE-3 e");
		cciToRmf.put("CCI-000924", "PE-3 f");
		cciToRmf.put("CCI-000925", "PE-3 f");
		cciToRmf.put("CCI-000926", "PE-3 g");
		cciToRmf.put("CCI-000927", "PE-3 g");
		cciToRmf.put("CCI-000928", "PE-3 (1)");
		cciToRmf.put("CCI-000929", "PE-3 (2)");
		cciToRmf.put("CCI-000930", "PE-3 (3)");
		cciToRmf.put("CCI-000931", "PE-3 (4)");
		cciToRmf.put("CCI-000932", "PE-3 (4)");
		cciToRmf.put("CCI-000933", "PE-3 (5)");
		cciToRmf.put("CCI-000934", "PE-3 (6)");
		cciToRmf.put("CCI-000935", "PE-3 (6)");
		cciToRmf.put("CCI-000936", "PE-4");
		cciToRmf.put("CCI-000937", "PE-5");
		cciToRmf.put("CCI-000939", "PE-6 b");
		cciToRmf.put("CCI-000940", "PE-6 b");
		cciToRmf.put("CCI-000941", "PE-6 c");
		cciToRmf.put("CCI-000942", "PE-6 (1)");
		cciToRmf.put("CCI-000947", "PE-8 a");
		cciToRmf.put("CCI-000948", "PE-8 b");
		cciToRmf.put("CCI-000949", "PE-8 b");
		cciToRmf.put("CCI-000950", "PE-8 (1)");
		cciToRmf.put("CCI-000952", "PE-9");
		cciToRmf.put("CCI-000954", "PE-9 (2)");
		cciToRmf.put("CCI-000955", "PE-9 (2)");
		cciToRmf.put("CCI-001636", "PL-1 b 1");
		cciToRmf.put("CCI-001637", "PL-1 b 1");
		cciToRmf.put("CCI-001638", "PL-1 b 2");
		cciToRmf.put("CCI-000563", "PL-1 a 1");
		cciToRmf.put("CCI-000564", "PL-1 a 1");
		cciToRmf.put("CCI-000566", "PL-1 a 2");
		cciToRmf.put("CCI-000567", "PL-1 a 2");
		cciToRmf.put("CCI-000568", "PL-1 b 2");
		cciToRmf.put("CCI-000571", "PL-2 a 9");
		cciToRmf.put("CCI-000572", "PL-2 c");
		cciToRmf.put("CCI-000573", "PL-2 c");
		cciToRmf.put("CCI-000574", "PL-2 d");
		cciToRmf.put("CCI-000577", "PL-7 b");
		cciToRmf.put("CCI-000578", "PL-7 b");
		cciToRmf.put("CCI-001639", "PL-4 a");
		cciToRmf.put("CCI-000592", "PL-4 a");
		cciToRmf.put("CCI-000593", "PL-4 b");
		cciToRmf.put("CCI-000594", "PL-4 (1)");
		cciToRmf.put("CCI-000595", "PL-4 (1)");
		cciToRmf.put("CCI-001680", "PM-1 a 2");
		cciToRmf.put("CCI-000073", "PM-1 a 1");
		cciToRmf.put("CCI-000074", "PM-1 a 4");
		cciToRmf.put("CCI-000075", "PM-1 b");
		cciToRmf.put("CCI-000076", "PM-1 b");
		cciToRmf.put("CCI-000077", "PM-1 c");
		cciToRmf.put("CCI-000229", "PM-10 a");
		cciToRmf.put("CCI-000230", "PM-10 a");
		cciToRmf.put("CCI-000231", "PM-10 a");
		cciToRmf.put("CCI-000233", "PM-10 b");
		cciToRmf.put("CCI-000234", "PM-10 c");
		cciToRmf.put("CCI-000235", "PM-11 a");
		cciToRmf.put("CCI-000236", "PM-11 b");
		cciToRmf.put("CCI-000078", "PM-2");
		cciToRmf.put("CCI-000080", "PM-3 a");
		cciToRmf.put("CCI-000081", "PM-3 b");
		cciToRmf.put("CCI-000141", "PM-3 c");
		cciToRmf.put("CCI-000142", "PM-4 a 1");
		cciToRmf.put("CCI-000170", "PM-4 a 2");
		cciToRmf.put("CCI-000207", "PM-5");
		cciToRmf.put("CCI-000209", "PM-6");
		cciToRmf.put("CCI-000210", "PM-6");
		cciToRmf.put("CCI-000211", "PM-6");
		cciToRmf.put("CCI-000212", "PM-7");
		cciToRmf.put("CCI-001640", "PM-8");
		cciToRmf.put("CCI-000216", "PM-8");
		cciToRmf.put("CCI-000227", "PM-9 a");
		cciToRmf.put("CCI-000228", "PM-9 b");
		cciToRmf.put("CCI-001504", "PS-1 a 1");
		cciToRmf.put("CCI-001505", "PS-1 a 1");
		cciToRmf.put("CCI-001506", "PS-1 b 2");
		cciToRmf.put("CCI-001507", "PS-1 b 1");
		cciToRmf.put("CCI-001508", "PS-1 b 2");
		cciToRmf.put("CCI-001509", "PS-1 a 2");
		cciToRmf.put("CCI-001510", "PS-1 a 2");
		cciToRmf.put("CCI-001511", "PS-1 b 2");
		cciToRmf.put("CCI-001512", "PS-2 a");
		cciToRmf.put("CCI-001513", "PS-2 b");
		cciToRmf.put("CCI-001514", "PS-2 c");
		cciToRmf.put("CCI-001515", "PS-2 c");
		cciToRmf.put("CCI-001516", "PS-3 a");
		cciToRmf.put("CCI-001517", "PS-3 b");
		cciToRmf.put("CCI-001518", "PS-3 b");
		cciToRmf.put("CCI-001519", "PS-3 b");
		cciToRmf.put("CCI-001520", "PS-3 (1)");
		cciToRmf.put("CCI-001521", "PS-3 (2)");
		cciToRmf.put("CCI-001522", "PS-4 a");
		cciToRmf.put("CCI-001523", "PS-4 c");
		cciToRmf.put("CCI-001524", "PS-4 d");
		cciToRmf.put("CCI-001525", "PS-4 e");
		cciToRmf.put("CCI-001526", "PS-4 e");
		cciToRmf.put("CCI-001527", "PS-5 a");
		cciToRmf.put("CCI-001528", "PS-5 b");
		cciToRmf.put("CCI-001529", "PS-5 b");
		cciToRmf.put("CCI-001530", "PS-5 b");
		cciToRmf.put("CCI-001531", "PS-6 c 1");
		cciToRmf.put("CCI-001532", "PS-6 b");
		cciToRmf.put("CCI-001533", "PS-6 b");
		cciToRmf.put("CCI-001536", "PS-6 (2) (a)");
		cciToRmf.put("CCI-001537", "PS-6 (2) (b)");
		cciToRmf.put("CCI-001538", "PS-6 (2) (c)");
		cciToRmf.put("CCI-001539", "PS-7 a");
		cciToRmf.put("CCI-001540", "PS-7 c");
		cciToRmf.put("CCI-001541", "PS-7 e");
		cciToRmf.put("CCI-001542", "PS-8 a");
		cciToRmf.put("CCI-001037", "RA-1 a 1");
		cciToRmf.put("CCI-001038", "RA-1 a 1");
		cciToRmf.put("CCI-001039", "RA-1 b 1");
		cciToRmf.put("CCI-001040", "RA-1 b 1");
		cciToRmf.put("CCI-001041", "RA-1 a 2");
		cciToRmf.put("CCI-001042", "RA-1 a 2");
		cciToRmf.put("CCI-001043", "RA-1 b 2");
		cciToRmf.put("CCI-001044", "RA-1 b 2");
		cciToRmf.put("CCI-001045", "RA-2 a");
		cciToRmf.put("CCI-001046", "RA-2 b");
		cciToRmf.put("CCI-001047", "RA-2 c");
		cciToRmf.put("CCI-001642", "RA-3 b");
		cciToRmf.put("CCI-001048", "RA-3 a");
		cciToRmf.put("CCI-001049", "RA-3 b");
		cciToRmf.put("CCI-001050", "RA-3 c");
		cciToRmf.put("CCI-001051", "RA-3 c");
		cciToRmf.put("CCI-001052", "RA-3 e");
		cciToRmf.put("CCI-001053", "RA-3 e");
		cciToRmf.put("CCI-001641", "RA-5 a");
		cciToRmf.put("CCI-001643", "RA-5 a");
		cciToRmf.put("CCI-001645", "RA-5 (5)");
		cciToRmf.put("CCI-001054", "RA-5 a");
		cciToRmf.put("CCI-001055", "RA-5 a");
		cciToRmf.put("CCI-001056", "RA-5 a");
		cciToRmf.put("CCI-001057", "RA-5 b");
		cciToRmf.put("CCI-001058", "RA-5 c");
		cciToRmf.put("CCI-001059", "RA-5 d");
		cciToRmf.put("CCI-001060", "RA-5 d");
		cciToRmf.put("CCI-001061", "RA-5 e");
		cciToRmf.put("CCI-001062", "RA-5 (1)");
		cciToRmf.put("CCI-001063", "RA-5 (2)");
		cciToRmf.put("CCI-001064", "RA-5 (2)");
		cciToRmf.put("CCI-001066", "RA-5 (4)");
		cciToRmf.put("CCI-001067", "RA-5 (5)");
		cciToRmf.put("CCI-001068", "RA-5 (6)");
		cciToRmf.put("CCI-001071", "RA-5 (8)");
		cciToRmf.put("CCI-000601", "SA-1 b 1");
		cciToRmf.put("CCI-000602", "SA-1 a 1");
		cciToRmf.put("CCI-000603", "SA-1 a 1");
		cciToRmf.put("CCI-000604", "SA-1 b 1");
		cciToRmf.put("CCI-000605", "SA-1 a 2");
		cciToRmf.put("CCI-000606", "SA-1 a 2");
		cciToRmf.put("CCI-000607", "SA-1 b 2");
		cciToRmf.put("CCI-000692", "SA-10 c");
		cciToRmf.put("CCI-000694", "SA-10 d");
		cciToRmf.put("CCI-000698", "SA-10 (1)");
		cciToRmf.put("CCI-000700", "SA-10 (2)");
		cciToRmf.put("CCI-000722", "SA-12");
		cciToRmf.put("CCI-000723", "SA-12");
		cciToRmf.put("CCI-001646", "SA-1 b 2");
		cciToRmf.put("CCI-000610", "SA-2 b");
		cciToRmf.put("CCI-000611", "SA-2 b");
		cciToRmf.put("CCI-000612", "SA-2 b");
		cciToRmf.put("CCI-000613", "SA-2 c");
		cciToRmf.put("CCI-000614", "SA-2 c");
		cciToRmf.put("CCI-000615", "SA-3 a");
		cciToRmf.put("CCI-000616", "SA-3 b");
		cciToRmf.put("CCI-000618", "SA-3 c");
		cciToRmf.put("CCI-000623", "SA-4 (1)");
		cciToRmf.put("CCI-000631", "SA-4 (6) (a)");
		cciToRmf.put("CCI-000633", "SA-4 (6) (b)");
		cciToRmf.put("CCI-000634", "SA-4 (7) (a)");
		cciToRmf.put("CCI-000635", "SA-4 (7) (b)");
		cciToRmf.put("CCI-000642", "SA-5 c");
		cciToRmf.put("CCI-000664", "SA-8");
		cciToRmf.put("CCI-000665", "SA-8");
		cciToRmf.put("CCI-000666", "SA-8");
		cciToRmf.put("CCI-000667", "SA-8");
		cciToRmf.put("CCI-000668", "SA-8");
		cciToRmf.put("CCI-000669", "SA-9 a");
		cciToRmf.put("CCI-000670", "SA-9 a");
		cciToRmf.put("CCI-000671", "SA-9 b");
		cciToRmf.put("CCI-000672", "SA-9 b");
		cciToRmf.put("CCI-000673", "SA-9 b");
		cciToRmf.put("CCI-000674", "SA-9 b");
		cciToRmf.put("CCI-001074", "SC-1 a 1");
		cciToRmf.put("CCI-001075", "SC-1 a 1");
		cciToRmf.put("CCI-001076", "SC-1 b 1");
		cciToRmf.put("CCI-001077", "SC-1 b 1");
		cciToRmf.put("CCI-001078", "SC-1 a 2");
		cciToRmf.put("CCI-001079", "SC-1 a 2");
		cciToRmf.put("CCI-001080", "SC-1 b 2");
		cciToRmf.put("CCI-001081", "SC-1 b 2");
		cciToRmf.put("CCI-001133", "SC-10");
		cciToRmf.put("CCI-001134", "SC-10");
		cciToRmf.put("CCI-001661", "SC-11");
		cciToRmf.put("CCI-001135", "SC-11");
		cciToRmf.put("CCI-001139", "SC-12 (1)");
		cciToRmf.put("CCI-001150", "SC-15 a");
		cciToRmf.put("CCI-001151", "SC-15 a");
		cciToRmf.put("CCI-001152", "SC-15 b");
		cciToRmf.put("CCI-001153", "SC-15 (1)");
		cciToRmf.put("CCI-001155", "SC-15 (3)");
		cciToRmf.put("CCI-001156", "SC-15 (3)");
		cciToRmf.put("CCI-001157", "SC-16");
		cciToRmf.put("CCI-001158", "SC-16 (1)");
		cciToRmf.put("CCI-001159", "SC-17");
		cciToRmf.put("CCI-001662", "SC-18 (1)");
		cciToRmf.put("CCI-001162", "SC-18 b");
		cciToRmf.put("CCI-001163", "SC-18 c");
		cciToRmf.put("CCI-001164", "SC-18 c");
		cciToRmf.put("CCI-001165", "SC-18 c");
		cciToRmf.put("CCI-001166", "SC-18 (1)");
		cciToRmf.put("CCI-001167", "SC-18 (2)");
		cciToRmf.put("CCI-001168", "SC-18 (2)");
		cciToRmf.put("CCI-001169", "SC-18 (3)");
		cciToRmf.put("CCI-001170", "SC-18 (4)");
		cciToRmf.put("CCI-001171", "SC-18 (4)");
		cciToRmf.put("CCI-001172", "SC-18 (4)");
		cciToRmf.put("CCI-001160", "SC-18 a");
		cciToRmf.put("CCI-001161", "SC-18 b");
		cciToRmf.put("CCI-001687", "SC-18 (2)");
		cciToRmf.put("CCI-001688", "SC-18 (2)");
		cciToRmf.put("CCI-001695", "SC-18 (3)");
		cciToRmf.put("CCI-001173", "SC-19 a");
		cciToRmf.put("CCI-001174", "SC-19 a");
		cciToRmf.put("CCI-001175", "SC-19 b");
		cciToRmf.put("CCI-001176", "SC-19 b");
		cciToRmf.put("CCI-001177", "SC-19 b");
		cciToRmf.put("CCI-001082", "SC-2");
		cciToRmf.put("CCI-001083", "SC-2 (1)");
		cciToRmf.put("CCI-001663", "SC-20 b");
		cciToRmf.put("CCI-001178", "SC-20 a");
		cciToRmf.put("CCI-001179", "SC-20 b");
		cciToRmf.put("CCI-001182", "SC-22");
		cciToRmf.put("CCI-001183", "SC-22");
		cciToRmf.put("CCI-001664", "SC-23 (3)");
		cciToRmf.put("CCI-001184", "SC-23");
		cciToRmf.put("CCI-001185", "SC-23 (1)");
		cciToRmf.put("CCI-001188", "SC-23 (3)");
		cciToRmf.put("CCI-001189", "SC-23 (3)");
		cciToRmf.put("CCI-001665", "SC-24");
		cciToRmf.put("CCI-001190", "SC-24");
		cciToRmf.put("CCI-001191", "SC-24");
		cciToRmf.put("CCI-001192", "SC-24");
		cciToRmf.put("CCI-001193", "SC-24");
		cciToRmf.put("CCI-001194", "SC-25");
		cciToRmf.put("CCI-001195", "SC-26");
		cciToRmf.put("CCI-001196", "SC-35");
		cciToRmf.put("CCI-001197", "SC-27");
		cciToRmf.put("CCI-001198", "SC-27");
		cciToRmf.put("CCI-001199", "SC-28");
		cciToRmf.put("CCI-001201", "SC-29");
		cciToRmf.put("CCI-001084", "SC-3");
		cciToRmf.put("CCI-001085", "SC-3 (1)");
		cciToRmf.put("CCI-001086", "SC-3 (2)");
		cciToRmf.put("CCI-001089", "SC-3 (5)");
		cciToRmf.put("CCI-001203", "SC-29 (1)");
		cciToRmf.put("CCI-001204", "SC-29 (1)");
		cciToRmf.put("CCI-001207", "SC-31 (1)");
		cciToRmf.put("CCI-001210", "SC-34 a");
		cciToRmf.put("CCI-001211", "SC-34 b");
		cciToRmf.put("CCI-001212", "SC-34");
		cciToRmf.put("CCI-001213", "SC-34 b");
		cciToRmf.put("CCI-001214", "SC-34 (1)");
		cciToRmf.put("CCI-001215", "SC-34 (1)");
		cciToRmf.put("CCI-001216", "SC-34 (2)");
		cciToRmf.put("CCI-001090", "SC-4");
		cciToRmf.put("CCI-001093", "SC-5");
		cciToRmf.put("CCI-001094", "SC-5 (1)");
		cciToRmf.put("CCI-001095", "SC-5 (2)");
		cciToRmf.put("CCI-001097", "SC-7 a");
		cciToRmf.put("CCI-001098", "SC-7 c");
		cciToRmf.put("CCI-001101", "SC-7 (3)");
		cciToRmf.put("CCI-001102", "SC-7 (4) (a)");
		cciToRmf.put("CCI-001103", "SC-7 (4) (b)");
		cciToRmf.put("CCI-001105", "SC-7 (4) (d)");
		cciToRmf.put("CCI-001106", "SC-7 (4) (e)");
		cciToRmf.put("CCI-001107", "SC-7 (4) (e)");
		cciToRmf.put("CCI-001108", "SC-7 (4) (e)");
		cciToRmf.put("CCI-001109", "SC-7 (5)");
		cciToRmf.put("CCI-001112", "SC-7 (8)");
		cciToRmf.put("CCI-001113", "SC-7 (8)");
		cciToRmf.put("CCI-001114", "SC-7 (8)");
		cciToRmf.put("CCI-001116", "SC-7 (10)");
		cciToRmf.put("CCI-001119", "SC-7 (13)");
		cciToRmf.put("CCI-001120", "SC-7 (13)");
		cciToRmf.put("CCI-001121", "SC-7 (14)");
		cciToRmf.put("CCI-001122", "SC-7 (14)");
		cciToRmf.put("CCI-001123", "SC-7 (15)");
		cciToRmf.put("CCI-001124", "SC-7 (16)");
		cciToRmf.put("CCI-001125", "SC-7 (17)");
		cciToRmf.put("CCI-001126", "SC-7 (18)");
		cciToRmf.put("CCI-001217", "SI-1 a 1");
		cciToRmf.put("CCI-001218", "SI-1 a 1");
		cciToRmf.put("CCI-001219", "SI-1 b 1");
		cciToRmf.put("CCI-001220", "SI-1 a 2");
		cciToRmf.put("CCI-001221", "SI-1 a 2");
		cciToRmf.put("CCI-001222", "SI-1 b 2");
		cciToRmf.put("CCI-001223", "SI-1 b 1");
		cciToRmf.put("CCI-001224", "SI-1 b 2");
		cciToRmf.put("CCI-001310", "SI-10");
		cciToRmf.put("CCI-001312", "SI-11 a");
		cciToRmf.put("CCI-001314", "SI-11 b");
		cciToRmf.put("CCI-001678", "SI-12");
		cciToRmf.put("CCI-001315", "SI-12");
		cciToRmf.put("CCI-001318", "SI-13 b");
		cciToRmf.put("CCI-001319", "SI-13 (1)");
		cciToRmf.put("CCI-001320", "SI-13 (1)");
		cciToRmf.put("CCI-001321", "SI-7 (16)");
		cciToRmf.put("CCI-001322", "SI-7 (16)");
		cciToRmf.put("CCI-001323", "SI-13 (3)");
		cciToRmf.put("CCI-001324", "SI-13 (3)");
		cciToRmf.put("CCI-001325", "SI-13 (3)");
		cciToRmf.put("CCI-001326", "SI-13 (4) (a)");
		cciToRmf.put("CCI-001327", "SI-13 (4) (a)");
		cciToRmf.put("CCI-001328", "SI-13 (4) (b)");
		cciToRmf.put("CCI-001329", "SI-13 (4) (b)");
		cciToRmf.put("CCI-001225", "SI-2 a");
		cciToRmf.put("CCI-001226", "SI-2 a");
		cciToRmf.put("CCI-001227", "SI-2 a");
		cciToRmf.put("CCI-001228", "SI-2 b");
		cciToRmf.put("CCI-001229", "SI-2 b");
		cciToRmf.put("CCI-001230", "SI-2 d");
		cciToRmf.put("CCI-001231", "SI-2 (1)");
		cciToRmf.put("CCI-001233", "SI-2 (2)");
		cciToRmf.put("CCI-001234", "SI-2 (2)");
		cciToRmf.put("CCI-001235", "SI-2 (3) (a)");
		cciToRmf.put("CCI-001236", "SI-2 (3) (b)");
		cciToRmf.put("CCI-001669", "SI-3 (6) (a)");
		cciToRmf.put("CCI-001240", "SI-3 b");
		cciToRmf.put("CCI-001241", "SI-3 c 1");
		cciToRmf.put("CCI-001242", "SI-3 c 1");
		cciToRmf.put("CCI-001243", "SI-3 c 2");
		cciToRmf.put("CCI-001244", "SI-3 c 2");
		cciToRmf.put("CCI-001245", "SI-3 d");
		cciToRmf.put("CCI-001246", "SI-3 (1)");
		cciToRmf.put("CCI-001247", "SI-3 (2)");
		cciToRmf.put("CCI-001249", "SI-3 (4)");
		cciToRmf.put("CCI-001251", "SI-3 (6) (a)");
		cciToRmf.put("CCI-001670", "SI-4 (7)");
		cciToRmf.put("CCI-001671", "SI-4 (11)");
		cciToRmf.put("CCI-001673", "SI-4 (14)");
		cciToRmf.put("CCI-001253", "SI-4 a 1");
		cciToRmf.put("CCI-001255", "SI-4 c");
		cciToRmf.put("CCI-001256", "SI-4 c");
		cciToRmf.put("CCI-001257", "SI-4 e");
		cciToRmf.put("CCI-001258", "SI-4 f");
		cciToRmf.put("CCI-001260", "SI-4 (2)");
		cciToRmf.put("CCI-001264", "SI-4 (5)");
		cciToRmf.put("CCI-001266", "SI-4 (7)");
		cciToRmf.put("CCI-001267", "SI-4 (7)");
		cciToRmf.put("CCI-001268", "SI-4 (7)");
		cciToRmf.put("CCI-001270", "SI-4 (9)");
		cciToRmf.put("CCI-001271", "SI-4 (9)");
		cciToRmf.put("CCI-001273", "SI-4 (11)");
		cciToRmf.put("CCI-001274", "SI-4 (12)");
		cciToRmf.put("CCI-001275", "SI-4 (12)");
		cciToRmf.put("CCI-001276", "SI-4 (13) (a)");
		cciToRmf.put("CCI-001277", "SI-4 (13) (b)");
		cciToRmf.put("CCI-001282", "SI-4 (15)");
		cciToRmf.put("CCI-001283", "SI-4 (16)");
		cciToRmf.put("CCI-001284", "SI-4 (17)");
		cciToRmf.put("CCI-001285", "SI-5 a");
		cciToRmf.put("CCI-001286", "SI-5 b");
		cciToRmf.put("CCI-001287", "SI-5 c");
		cciToRmf.put("CCI-001288", "SI-5 c");
		cciToRmf.put("CCI-001289", "SI-5 d");
		cciToRmf.put("CCI-001290", "SI-5 (1)");
		cciToRmf.put("CCI-001675", "SI-6 (3)");
		cciToRmf.put("CCI-001294", "SI-6 c");
		cciToRmf.put("CCI-001295", "SI-6 (2)");
		cciToRmf.put("CCI-001296", "SI-6 (3)");
		cciToRmf.put("CCI-001300", "SI-7 (2)");
		cciToRmf.put("CCI-001301", "SI-7 (3)");
		cciToRmf.put("CCI-001306", "SI-8 b");
		cciToRmf.put("CCI-001307", "SI-8 (1)");
		cciToRmf.put("CCI-001308", "SI-8 (2)");
		cciToRmf.put("CCI-002106", "AC-1 a 1");
		cciToRmf.put("CCI-002107", "AC-1 a 1");
		cciToRmf.put("CCI-002108", "AC-1 a 1");
		cciToRmf.put("CCI-002109", "AC-1 a 2");
		cciToRmf.put("CCI-002252", "AC-10");
		cciToRmf.put("CCI-002253", "AC-10");
		cciToRmf.put("CCI-002254", "AC-12");
		cciToRmf.put("CCI-002360", "AC-12");
		cciToRmf.put("CCI-002361", "AC-12");
		cciToRmf.put("CCI-002362", "AC-12 (1)");
		cciToRmf.put("CCI-002363", "AC-12 (1)");
		cciToRmf.put("CCI-002364", "AC-12 (1)");
		cciToRmf.put("CCI-002255", "AC-14 a");
		cciToRmf.put("CCI-002256", "AC-16 a");
		cciToRmf.put("CCI-002257", "AC-16 a");
		cciToRmf.put("CCI-002258", "AC-16 a");
		cciToRmf.put("CCI-002259", "AC-16 a");
		cciToRmf.put("CCI-002260", "AC-16 a");
		cciToRmf.put("CCI-002261", "AC-16 a");
		cciToRmf.put("CCI-002262", "AC-16 a");
		cciToRmf.put("CCI-002263", "AC-16 a");
		cciToRmf.put("CCI-002264", "AC-16 a");
		cciToRmf.put("CCI-002265", "AC-16 b");
		cciToRmf.put("CCI-002266", "AC-16 b");
		cciToRmf.put("CCI-002267", "AC-16 c");
		cciToRmf.put("CCI-002268", "AC-16 c");
		cciToRmf.put("CCI-002269", "AC-16 c");
		cciToRmf.put("CCI-002270", "AC-16 d");
		cciToRmf.put("CCI-002271", "AC-16 d");
		cciToRmf.put("CCI-002272", "AC-16 (1)");
		cciToRmf.put("CCI-002273", "AC-16 (1)");
		cciToRmf.put("CCI-002274", "AC-16 (1)");
		cciToRmf.put("CCI-002275", "AC-16 (1)");
		cciToRmf.put("CCI-002276", "AC-16 (2)");
		cciToRmf.put("CCI-002277", "AC-16 (2)");
		cciToRmf.put("CCI-002278", "AC-16 (3)");
		cciToRmf.put("CCI-002279", "AC-16 (3)");
		cciToRmf.put("CCI-002280", "AC-16 (3)");
		cciToRmf.put("CCI-002281", "AC-16 (3)");
		cciToRmf.put("CCI-002282", "AC-16 (3)");
		cciToRmf.put("CCI-002283", "AC-16 (3)");
		cciToRmf.put("CCI-002284", "AC-16 (3)");
		cciToRmf.put("CCI-002285", "AC-16 (4)");
		cciToRmf.put("CCI-002286", "AC-16 (4)");
		cciToRmf.put("CCI-002287", "AC-16 (4)");
		cciToRmf.put("CCI-002288", "AC-16 (4)");
		cciToRmf.put("CCI-002289", "AC-16 (4)");
		cciToRmf.put("CCI-002290", "AC-16 (4)");
		cciToRmf.put("CCI-002291", "AC-16 (6)");
		cciToRmf.put("CCI-002292", "AC-16 (6)");
		cciToRmf.put("CCI-002293", "AC-16 (6)");
		cciToRmf.put("CCI-002294", "AC-16 (6)");
		cciToRmf.put("CCI-002295", "AC-16 (6)");
		cciToRmf.put("CCI-002296", "AC-16 (6)");
		cciToRmf.put("CCI-002297", "AC-16 (6)");
		cciToRmf.put("CCI-002298", "AC-16 (6)");
		cciToRmf.put("CCI-002299", "AC-16 (7)");
		cciToRmf.put("CCI-002300", "AC-16 (8)");
		cciToRmf.put("CCI-002301", "AC-16 (8)");
		cciToRmf.put("CCI-002302", "AC-16 (8)");
		cciToRmf.put("CCI-002303", "AC-16 (9)");
		cciToRmf.put("CCI-002304", "AC-16 (9)");
		cciToRmf.put("CCI-002305", "AC-16 (10)");
		cciToRmf.put("CCI-002306", "AC-16 (10)");
		cciToRmf.put("CCI-002307", "AC-16 (10)");
		cciToRmf.put("CCI-002308", "AC-16 (10)");
		cciToRmf.put("CCI-002309", "AC-16 (10)");
		cciToRmf.put("CCI-002310", "AC-17 a");
		cciToRmf.put("CCI-002311", "AC-17 a");
		cciToRmf.put("CCI-002312", "AC-17 a");
		cciToRmf.put("CCI-002313", "AC-17 (1)");
		cciToRmf.put("CCI-002314", "AC-17 (1)");
		cciToRmf.put("CCI-002315", "AC-17 (3)");
		cciToRmf.put("CCI-002316", "AC-17 (4) (a)");
		cciToRmf.put("CCI-002317", "AC-17 (4) (a)");
		cciToRmf.put("CCI-002318", "AC-17 (4) (a)");
		cciToRmf.put("CCI-002319", "AC-17 (4) (b)");
		cciToRmf.put("CCI-002320", "AC-17 (4) (b)");
		cciToRmf.put("CCI-002321", "AC-17 (9)");
		cciToRmf.put("CCI-002322", "AC-17 (9)");
		cciToRmf.put("CCI-002323", "AC-18 a");
		cciToRmf.put("CCI-002324", "AC-18 (4)");
		cciToRmf.put("CCI-002325", "AC-19 a");
		cciToRmf.put("CCI-002326", "AC-19 a");
		cciToRmf.put("CCI-002327", "AC-19 (4) (c)");
		cciToRmf.put("CCI-002328", "AC-19 (4) (c)");
		cciToRmf.put("CCI-002329", "AC-19 (5)");
		cciToRmf.put("CCI-002330", "AC-19 (5)");
		cciToRmf.put("CCI-002331", "AC-19 (5)");
		cciToRmf.put("CCI-002110", "AC-2 a");
		cciToRmf.put("CCI-002111", "AC-2 a");
		cciToRmf.put("CCI-002112", "AC-2 b");
		cciToRmf.put("CCI-002113", "AC-2 c");
		cciToRmf.put("CCI-002114", "AC-2 d");
		cciToRmf.put("CCI-002115", "AC-2 d");
		cciToRmf.put("CCI-002116", "AC-2 d");
		cciToRmf.put("CCI-002117", "AC-2 d");
		cciToRmf.put("CCI-002118", "AC-2 d");
		cciToRmf.put("CCI-002119", "AC-2 d");
		cciToRmf.put("CCI-002120", "AC-2 e");
		cciToRmf.put("CCI-002121", "AC-2 f");
		cciToRmf.put("CCI-002122", "AC-2 g");
		cciToRmf.put("CCI-002123", "AC-2 h 1");
		cciToRmf.put("CCI-002124", "AC-2 h 2");
		cciToRmf.put("CCI-002125", "AC-2 h 3");
		cciToRmf.put("CCI-002126", "AC-2 i 1");
		cciToRmf.put("CCI-002127", "AC-2 i 2");
		cciToRmf.put("CCI-002128", "AC-2 i 3");
		cciToRmf.put("CCI-002129", "AC-2 k");
		cciToRmf.put("CCI-002130", "AC-2 (4)");
		cciToRmf.put("CCI-002131", "AC-2 (4)");
		cciToRmf.put("CCI-002132", "AC-2 (4)");
		cciToRmf.put("CCI-002133", "AC-2 (5)");
		cciToRmf.put("CCI-002134", "AC-2 (6)");
		cciToRmf.put("CCI-002135", "AC-2 (6)");
		cciToRmf.put("CCI-002136", "AC-2 (7) (c)");
		cciToRmf.put("CCI-002137", "AC-2 (7) (c)");
		cciToRmf.put("CCI-002138", "AC-2 (8)");
		cciToRmf.put("CCI-002139", "AC-2 (8)");
		cciToRmf.put("CCI-002140", "AC-2 (9)");
		cciToRmf.put("CCI-002141", "AC-2 (9)");
		cciToRmf.put("CCI-002142", "AC-2 (10)");
		cciToRmf.put("CCI-002143", "AC-2 (11)");
		cciToRmf.put("CCI-002144", "AC-2 (11)");
		cciToRmf.put("CCI-002145", "AC-2 (11)");
		cciToRmf.put("CCI-002146", "AC-2 (12) (a)");
		cciToRmf.put("CCI-002147", "AC-2 (12) (a)");
		cciToRmf.put("CCI-002148", "AC-2 (12) (b)");
		cciToRmf.put("CCI-002149", "AC-2 (12) (b)");
		cciToRmf.put("CCI-002150", "AC-2 (13)");
		cciToRmf.put("CCI-002151", "AC-2 (13)");
		cciToRmf.put("CCI-002332", "AC-20 b");
		cciToRmf.put("CCI-002333", "AC-20 (1) (a)");
		cciToRmf.put("CCI-002334", "AC-20 (1) (a)");
		cciToRmf.put("CCI-002335", "AC-20 (1) (a)");
		cciToRmf.put("CCI-002336", "AC-20 (1) (a)");
		cciToRmf.put("CCI-002337", "AC-20 (1) (b)");
		cciToRmf.put("CCI-002338", "AC-20 (3)");
		cciToRmf.put("CCI-002339", "AC-20 (4)");
		cciToRmf.put("CCI-002340", "AC-20 (4)");
		cciToRmf.put("CCI-002341", "AC-21 (2)");
		cciToRmf.put("CCI-002342", "AC-21 (2)");
		cciToRmf.put("CCI-002343", "AC-23");
		cciToRmf.put("CCI-002344", "AC-23");
		cciToRmf.put("CCI-002345", "AC-23");
		cciToRmf.put("CCI-002346", "AC-23");
		cciToRmf.put("CCI-002347", "AC-23");
		cciToRmf.put("CCI-002348", "AC-24");
		cciToRmf.put("CCI-002349", "AC-24");
		cciToRmf.put("CCI-002350", "AC-24 (1)");
		cciToRmf.put("CCI-002351", "AC-24 (1)");
		cciToRmf.put("CCI-002352", "AC-24 (1)");
		cciToRmf.put("CCI-002353", "AC-24 (1)");
		cciToRmf.put("CCI-002354", "AC-24 (2)");
		cciToRmf.put("CCI-002355", "AC-24 (2)");
		cciToRmf.put("CCI-002356", "AC-25");
		cciToRmf.put("CCI-002357", "AC-25");
		cciToRmf.put("CCI-002358", "AC-25");
		cciToRmf.put("CCI-002359", "AC-25");
		cciToRmf.put("CCI-002152", "AC-3 (2)");
		cciToRmf.put("CCI-002153", "AC-3 (3)");
		cciToRmf.put("CCI-002154", "AC-3 (3) (a)");
		cciToRmf.put("CCI-002155", "AC-3 (3) (b) (1)");
		cciToRmf.put("CCI-002156", "AC-3 (3) (b) (2)");
		cciToRmf.put("CCI-002157", "AC-3 (3) (b) (3)");
		cciToRmf.put("CCI-002158", "AC-3 (3) (b) (4)");
		cciToRmf.put("CCI-002159", "AC-3 (3) (b) (4)");
		cciToRmf.put("CCI-002160", "AC-3 (3) (b) (5)");
		cciToRmf.put("CCI-002161", "AC-3 (3) (c)");
		cciToRmf.put("CCI-002162", "AC-3 (3) (c)");
		cciToRmf.put("CCI-002163", "AC-3 (4)");
		cciToRmf.put("CCI-002164", "AC-3 (4)");
		cciToRmf.put("CCI-002165", "AC-3 (4)");
		cciToRmf.put("CCI-002166", "AC-3 (7)");
		cciToRmf.put("CCI-002167", "AC-3 (7)");
		cciToRmf.put("CCI-002168", "AC-3 (7)");
		cciToRmf.put("CCI-002169", "AC-3 (7)");
		cciToRmf.put("CCI-002170", "AC-3 (7)");
		cciToRmf.put("CCI-002171", "AC-3 (7)");
		cciToRmf.put("CCI-002172", "AC-3 (7)");
		cciToRmf.put("CCI-002173", "AC-3 (7)");
		cciToRmf.put("CCI-002174", "AC-3 (7)");
		cciToRmf.put("CCI-002175", "AC-3 (7)");
		cciToRmf.put("CCI-002176", "AC-3 (7)");
		cciToRmf.put("CCI-002177", "AC-3 (8)");
		cciToRmf.put("CCI-002178", "AC-3 (8)");
		cciToRmf.put("CCI-002179", "AC-3 (8)");
		cciToRmf.put("CCI-002180", "AC-3 (9) (a)");
		cciToRmf.put("CCI-002181", "AC-3 (9) (a)");
		cciToRmf.put("CCI-002182", "AC-3 (9) (a)");
		cciToRmf.put("CCI-002183", "AC-3 (9) (b)");
		cciToRmf.put("CCI-002184", "AC-3 (9) (b)");
		cciToRmf.put("CCI-002185", "AC-3 (10)");
		cciToRmf.put("CCI-002186", "AC-3 (10)");
		cciToRmf.put("CCI-003014", "AC-3 (3)");
		cciToRmf.put("CCI-003015", "AC-3 (3) (c)");
		cciToRmf.put("CCI-002187", "AC-4 (1)");
		cciToRmf.put("CCI-002188", "AC-4 (1)");
		cciToRmf.put("CCI-002189", "AC-4 (1)");
		cciToRmf.put("CCI-002190", "AC-4 (1)");
		cciToRmf.put("CCI-002191", "AC-4 (2)");
		cciToRmf.put("CCI-002192", "AC-4 (3)");
		cciToRmf.put("CCI-002193", "AC-4 (4)");
		cciToRmf.put("CCI-002194", "AC-4 (6)");
		cciToRmf.put("CCI-002195", "AC-4 (8)");
		cciToRmf.put("CCI-002196", "AC-4 (9)");
		cciToRmf.put("CCI-002197", "AC-4 (9)");
		cciToRmf.put("CCI-002198", "AC-4 (9)");
		cciToRmf.put("CCI-002199", "AC-4 (10)");
		cciToRmf.put("CCI-002200", "AC-4 (12)");
		cciToRmf.put("CCI-002201", "AC-4 (12)");
		cciToRmf.put("CCI-002202", "AC-4 (13)");
		cciToRmf.put("CCI-002203", "AC-4 (15)");
		cciToRmf.put("CCI-002204", "AC-4 (15)");
		cciToRmf.put("CCI-002205", "AC-4 (17)");
		cciToRmf.put("CCI-002206", "AC-4 (17)");
		cciToRmf.put("CCI-002207", "AC-4 (17)");
		cciToRmf.put("CCI-002208", "AC-4 (17)");
		cciToRmf.put("CCI-002209", "AC-4 (18)");
		cciToRmf.put("CCI-002210", "AC-4 (18)");
		cciToRmf.put("CCI-002211", "AC-4 (19)");
		cciToRmf.put("CCI-002212", "AC-4 (20)");
		cciToRmf.put("CCI-002213", "AC-4 (20)");
		cciToRmf.put("CCI-002214", "AC-4 (20)");
		cciToRmf.put("CCI-002215", "AC-4 (21)");
		cciToRmf.put("CCI-002216", "AC-4 (21)");
		cciToRmf.put("CCI-002217", "AC-4 (21)");
		cciToRmf.put("CCI-002218", "AC-4 (22)");
		cciToRmf.put("CCI-002219", "AC-5 a");
		cciToRmf.put("CCI-002220", "AC-5 c");
		cciToRmf.put("CCI-002221", "AC-6 (1)");
		cciToRmf.put("CCI-002222", "AC-6 (1)");
		cciToRmf.put("CCI-002223", "AC-6 (1)");
		cciToRmf.put("CCI-002224", "AC-6 (3)");
		cciToRmf.put("CCI-002225", "AC-6 (4)");
		cciToRmf.put("CCI-002226", "AC-6 (5)");
		cciToRmf.put("CCI-002227", "AC-6 (5)");
		cciToRmf.put("CCI-002228", "AC-6 (7) (a)");
		cciToRmf.put("CCI-002229", "AC-6 (7) (a)");
		cciToRmf.put("CCI-002230", "AC-6 (7) (a)");
		cciToRmf.put("CCI-002231", "AC-6 (7) (b)");
		cciToRmf.put("CCI-002232", "AC-6 (8)");
		cciToRmf.put("CCI-002233", "AC-6 (8)");
		cciToRmf.put("CCI-002234", "AC-6 (9)");
		cciToRmf.put("CCI-002235", "AC-6 (10)");
		cciToRmf.put("CCI-002236", "AC-7 b");
		cciToRmf.put("CCI-002237", "AC-7 b");
		cciToRmf.put("CCI-002238", "AC-7 b");
		cciToRmf.put("CCI-002239", "AC-7 (2)");
		cciToRmf.put("CCI-002240", "AC-7 (2)");
		cciToRmf.put("CCI-002241", "AC-7 (2)");
		cciToRmf.put("CCI-002242", "AC-7 (2)");
		cciToRmf.put("CCI-002243", "AC-8 a 1");
		cciToRmf.put("CCI-002244", "AC-8 a 2");
		cciToRmf.put("CCI-002245", "AC-8 a 3");
		cciToRmf.put("CCI-002246", "AC-8 a 4");
		cciToRmf.put("CCI-002247", "AC-8 a");
		cciToRmf.put("CCI-002248", "AC-8 c 1");
		cciToRmf.put("CCI-002249", "AC-9 (4)");
		cciToRmf.put("CCI-002250", "AC-9 (4)");
		cciToRmf.put("CCI-002251", "AC-9 (4)");
		cciToRmf.put("CCI-003392", "AP-1");
		cciToRmf.put("CCI-003393", "AP-1");
		cciToRmf.put("CCI-003394", "AP-1");
		cciToRmf.put("CCI-003395", "AP-1");
		cciToRmf.put("CCI-003396", "AP-2");
		cciToRmf.put("CCI-003398", "AP-2");
		cciToRmf.put("CCI-003399", "AP-2");
		cciToRmf.put("CCI-003400", "AP-2");
		cciToRmf.put("CCI-003397", "AR-1 a");
		cciToRmf.put("CCI-003401", "AR-1 b");
		cciToRmf.put("CCI-003402", "AR-1 c");
		cciToRmf.put("CCI-003403", "AR-1 c");
		cciToRmf.put("CCI-003404", "AR-1 c");
		cciToRmf.put("CCI-003405", "AR-1 c");
		cciToRmf.put("CCI-003406", "AR-1 d");
		cciToRmf.put("CCI-003407", "AR-1 e");
		cciToRmf.put("CCI-003408", "AR-1 e");
		cciToRmf.put("CCI-003409", "AR-1 e");
		cciToRmf.put("CCI-003410", "AR-1 e");
		cciToRmf.put("CCI-003411", "AR-1 e");
		cciToRmf.put("CCI-003412", "AR-1 e");
		cciToRmf.put("CCI-003413", "AR-1 f");
		cciToRmf.put("CCI-003414", "AR-1 f");
		cciToRmf.put("CCI-003415", "AR-1 f");
		cciToRmf.put("CCI-003416", "AR-1 f");
		cciToRmf.put("CCI-003417", "AR-2 a");
		cciToRmf.put("CCI-003418", "AR-2 a");
		cciToRmf.put("CCI-003419", "AR-2 a");
		cciToRmf.put("CCI-003420", "AR-2 a");
		cciToRmf.put("CCI-003421", "AR-2 a");
		cciToRmf.put("CCI-003422", "AR-2 a");
		cciToRmf.put("CCI-003423", "AR-2 a");
		cciToRmf.put("CCI-003424", "AR-2 a");
		cciToRmf.put("CCI-003425", "AR-2 b");
		cciToRmf.put("CCI-003426", "AR-3 a");
		cciToRmf.put("CCI-003427", "AR-3 a");
		cciToRmf.put("CCI-003428", "AR-3 a");
		cciToRmf.put("CCI-003429", "AR-3 a");
		cciToRmf.put("CCI-003430", "AR-3 a");
		cciToRmf.put("CCI-003431", "AR-3 a");
		cciToRmf.put("CCI-003432", "AR-3 b");
		cciToRmf.put("CCI-003433", "AR-3 b");
		cciToRmf.put("CCI-003434", "AR-4");
		cciToRmf.put("CCI-003435", "AR-4");
		cciToRmf.put("CCI-003436", "AR-4");
		cciToRmf.put("CCI-003437", "AR-4");
		cciToRmf.put("CCI-003438", "AR-4");
		cciToRmf.put("CCI-003439", "AR-4");
		cciToRmf.put("CCI-003440", "AR-5 a");
		cciToRmf.put("CCI-003441", "AR-5 a");
		cciToRmf.put("CCI-003442", "AR-5 a");
		cciToRmf.put("CCI-003443", "AR-5 b");
		cciToRmf.put("CCI-003444", "AR-5 b");
		cciToRmf.put("CCI-003445", "AR-5 b");
		cciToRmf.put("CCI-003446", "AR-5 b");
		cciToRmf.put("CCI-003447", "AR-5 c");
		cciToRmf.put("CCI-003448", "AR-5 c");
		cciToRmf.put("CCI-003449", "AR-6");
		cciToRmf.put("CCI-003450", "AR-6");
		cciToRmf.put("CCI-003451", "AR-6");
		cciToRmf.put("CCI-003452", "AR-6");
		cciToRmf.put("CCI-003453", "AR-6");
		cciToRmf.put("CCI-003454", "AR-6");
		cciToRmf.put("CCI-003455", "AR-7");
		cciToRmf.put("CCI-003456", "AR-8 a (1)");
		cciToRmf.put("CCI-003457", "AR-8 a (1)");
		cciToRmf.put("CCI-003458", "AR-8 a (1)");
		cciToRmf.put("CCI-003459", "AR-8 a (1)");
		cciToRmf.put("CCI-003460", "AR-8 a (2)");
		cciToRmf.put("CCI-003461", "AR-8 b");
		cciToRmf.put("CCI-003462", "AR-8 c");
		cciToRmf.put("CCI-002048", "AT-1 a 1");
		cciToRmf.put("CCI-002049", "AT-1 a 2");
		cciToRmf.put("CCI-002055", "AT-2 (2)");
		cciToRmf.put("CCI-002050", "AT-3 (1)");
		cciToRmf.put("CCI-002051", "AT-3 (2)");
		cciToRmf.put("CCI-002052", "AT-3 (3)");
		cciToRmf.put("CCI-002053", "AT-3 (4)");
		cciToRmf.put("CCI-002054", "AT-3 (4)");
		cciToRmf.put("CCI-001831", "AU-1 a 1");
		cciToRmf.put("CCI-001832", "AU-1 a 1");
		cciToRmf.put("CCI-001833", "AU-1 a 2");
		cciToRmf.put("CCI-001834", "AU-1 a 2");
		cciToRmf.put("CCI-001835", "AU-1 b 1");
		cciToRmf.put("CCI-001836", "AU-1 b 1");
		cciToRmf.put("CCI-001837", "AU-1 b 1");
		cciToRmf.put("CCI-001838", "AU-1 b 1");
		cciToRmf.put("CCI-001839", "AU-1 b 2");
		cciToRmf.put("CCI-001840", "AU-1 b 2");
		cciToRmf.put("CCI-001841", "AU-1 b 2");
		cciToRmf.put("CCI-001842", "AU-1 b 2");
		cciToRmf.put("CCI-001930", "AU-1 a 1");
		cciToRmf.put("CCI-001931", "AU-1 a 2");
		cciToRmf.put("CCI-001899", "AU-10");
		cciToRmf.put("CCI-001900", "AU-10 (1) (a)");
		cciToRmf.put("CCI-001901", "AU-10 (1) (a)");
		cciToRmf.put("CCI-001902", "AU-10 (1) (b)");
		cciToRmf.put("CCI-001903", "AU-10 (2) (a)");
		cciToRmf.put("CCI-001904", "AU-10 (2) (a)");
		cciToRmf.put("CCI-001905", "AU-10 (2) (b)");
		cciToRmf.put("CCI-001906", "AU-10 (2) (b)");
		cciToRmf.put("CCI-001907", "AU-10 (4) (a)");
		cciToRmf.put("CCI-001908", "AU-10 (4) (b)");
		cciToRmf.put("CCI-001909", "AU-10 (4) (b)");
		cciToRmf.put("CCI-002044", "AU-11 (1)");
		cciToRmf.put("CCI-002045", "AU-11 (1)");
		cciToRmf.put("CCI-001910", "AU-12 b");
		cciToRmf.put("CCI-001911", "AU-12 (3)");
		cciToRmf.put("CCI-001912", "AU-12 (3)");
		cciToRmf.put("CCI-001913", "AU-12 (3)");
		cciToRmf.put("CCI-001914", "AU-12 (3)");
		cciToRmf.put("CCI-002047", "AU-12 (3)");
		cciToRmf.put("CCI-001915", "AU-13");
		cciToRmf.put("CCI-001916", "AU-13 (1)");
		cciToRmf.put("CCI-001917", "AU-13 (2)");
		cciToRmf.put("CCI-001918", "AU-13 (2)");
		cciToRmf.put("CCI-001919", "AU-14");
		cciToRmf.put("CCI-001920", "AU-14 (3)");
		cciToRmf.put("CCI-001921", "AU-15");
		cciToRmf.put("CCI-001922", "AU-15");
		cciToRmf.put("CCI-001923", "AU-16");
		cciToRmf.put("CCI-001924", "AU-16");
		cciToRmf.put("CCI-001925", "AU-16");
		cciToRmf.put("CCI-001926", "AU-16 (1)");
		cciToRmf.put("CCI-001927", "AU-16 (2)");
		cciToRmf.put("CCI-001928", "AU-16 (2)");
		cciToRmf.put("CCI-001929", "AU-16 (2)");
		cciToRmf.put("CCI-001843", "AU-2 (3)");
		cciToRmf.put("CCI-001844", "AU-3 (2)");
		cciToRmf.put("CCI-001845", "AU-3 (2)");
		cciToRmf.put("CCI-001846", "AU-3 (2)");
		cciToRmf.put("CCI-001847", "AU-3 (2)");
		cciToRmf.put("CCI-001848", "AU-4");
		cciToRmf.put("CCI-001849", "AU-4");
		cciToRmf.put("CCI-001850", "AU-4 (1)");
		cciToRmf.put("CCI-001851", "AU-4 (1)");
		cciToRmf.put("CCI-001852", "AU-5 (1)");
		cciToRmf.put("CCI-001853", "AU-5 (1)");
		cciToRmf.put("CCI-001854", "AU-5 (1)");
		cciToRmf.put("CCI-001855", "AU-5 (1)");
		cciToRmf.put("CCI-001856", "AU-5 (2)");
		cciToRmf.put("CCI-001857", "AU-5 (2)");
		cciToRmf.put("CCI-001858", "AU-5 (2)");
		cciToRmf.put("CCI-001859", "AU-5 (3)");
		cciToRmf.put("CCI-001860", "AU-5 (4)");
		cciToRmf.put("CCI-001861", "AU-5 (4)");
		cciToRmf.put("CCI-002907", "AU-5 (4)");
		cciToRmf.put("CCI-001862", "AU-6 a");
		cciToRmf.put("CCI-001863", "AU-6 b");
		cciToRmf.put("CCI-001864", "AU-6 (1)");
		cciToRmf.put("CCI-001865", "AU-6 (1)");
		cciToRmf.put("CCI-001866", "AU-6 (5)");
		cciToRmf.put("CCI-001867", "AU-6 (5)");
		cciToRmf.put("CCI-001868", "AU-6 (7)");
		cciToRmf.put("CCI-001869", "AU-6 (7)");
		cciToRmf.put("CCI-001870", "AU-6 (8)");
		cciToRmf.put("CCI-001871", "AU-6 (9)");
		cciToRmf.put("CCI-001872", "AU-6 (10)");
		cciToRmf.put("CCI-001873", "AU-6 (10)");
		cciToRmf.put("CCI-001874", "AU-6 (10)");
		cciToRmf.put("CCI-001875", "AU-7 a");
		cciToRmf.put("CCI-001876", "AU-7 a");
		cciToRmf.put("CCI-001877", "AU-7 a");
		cciToRmf.put("CCI-001878", "AU-7 a");
		cciToRmf.put("CCI-001879", "AU-7 a");
		cciToRmf.put("CCI-001880", "AU-7 a");
		cciToRmf.put("CCI-001881", "AU-7 b");
		cciToRmf.put("CCI-001882", "AU-7 b");
		cciToRmf.put("CCI-001883", "AU-7 (1)");
		cciToRmf.put("CCI-001884", "AU-7 (2)");
		cciToRmf.put("CCI-001885", "AU-7 (2)");
		cciToRmf.put("CCI-001886", "AU-7 (2)");
		cciToRmf.put("CCI-001887", "AU-7 (2)");
		cciToRmf.put("CCI-001888", "AU-8 b");
		cciToRmf.put("CCI-001889", "AU-8 b");
		cciToRmf.put("CCI-001890", "AU-8 b");
		cciToRmf.put("CCI-001891", "AU-8 (1) (a)");
		cciToRmf.put("CCI-001892", "AU-8 (1) (b)");
		cciToRmf.put("CCI-001893", "AU-8 (2)");
		cciToRmf.put("CCI-002046", "AU-8 (1) (b)");
		cciToRmf.put("CCI-001894", "AU-9 (4)");
		cciToRmf.put("CCI-001895", "AU-9 (5)");
		cciToRmf.put("CCI-001896", "AU-9 (5)");
		cciToRmf.put("CCI-001897", "AU-9 (6)");
		cciToRmf.put("CCI-001898", "AU-9 (6)");
		cciToRmf.put("CCI-002060", "CA-1 a 1");
		cciToRmf.put("CCI-002061", "CA-1 a 1");
		cciToRmf.put("CCI-002062", "CA-1 a 2");
		cciToRmf.put("CCI-002063", "CA-2 (1)");
		cciToRmf.put("CCI-002064", "CA-2 (2)");
		cciToRmf.put("CCI-002065", "CA-2 (2)");
		cciToRmf.put("CCI-002066", "CA-2 (3)");
		cciToRmf.put("CCI-002067", "CA-2 (3)");
		cciToRmf.put("CCI-002068", "CA-2 (3)");
		cciToRmf.put("CCI-002069", "CA-2 (3)");
		cciToRmf.put("CCI-002070", "CA-2 a 3");
		cciToRmf.put("CCI-002071", "CA-2 d");
		cciToRmf.put("CCI-002072", "CA-3 (1)");
		cciToRmf.put("CCI-002073", "CA-3 (1)");
		cciToRmf.put("CCI-002074", "CA-3 (2)");
		cciToRmf.put("CCI-002075", "CA-3 (3)");
		cciToRmf.put("CCI-002076", "CA-3 (3)");
		cciToRmf.put("CCI-002077", "CA-3 (3)");
		cciToRmf.put("CCI-002078", "CA-3 (4)");
		cciToRmf.put("CCI-002079", "CA-3 (4)");
		cciToRmf.put("CCI-002080", "CA-3 (5)");
		cciToRmf.put("CCI-002081", "CA-3 (5)");
		cciToRmf.put("CCI-002082", "CA-3 (5)");
		cciToRmf.put("CCI-002083", "CA-3 c");
		cciToRmf.put("CCI-002084", "CA-3 c");
		cciToRmf.put("CCI-002085", "CA-7 (1)");
		cciToRmf.put("CCI-002086", "CA-7 (3)");
		cciToRmf.put("CCI-002087", "CA-7 a");
		cciToRmf.put("CCI-002088", "CA-7 b");
		cciToRmf.put("CCI-002089", "CA-7 b");
		cciToRmf.put("CCI-002090", "CA-7 d");
		cciToRmf.put("CCI-002091", "CA-7 e");
		cciToRmf.put("CCI-002092", "CA-7 f");
		cciToRmf.put("CCI-002093", "CA-8");
		cciToRmf.put("CCI-002094", "CA-8");
		cciToRmf.put("CCI-002095", "CA-8");
		cciToRmf.put("CCI-002096", "CA-8 (1)");
		cciToRmf.put("CCI-002097", "CA-8 (2)");
		cciToRmf.put("CCI-002098", "CA-8 (2)");
		cciToRmf.put("CCI-002099", "CA-8 (2)");
		cciToRmf.put("CCI-002100", "CA-9 (1)");
		cciToRmf.put("CCI-002101", "CA-9 (a)");
		cciToRmf.put("CCI-002102", "CA-9 (a)");
		cciToRmf.put("CCI-002103", "CA-9 (b)");
		cciToRmf.put("CCI-002104", "CA-9 (b)");
		cciToRmf.put("CCI-002105", "CA-9 (b)");
		cciToRmf.put("CCI-001820", "CM-1 a 1");
		cciToRmf.put("CCI-001821", "CM-1 a 1");
		cciToRmf.put("CCI-001822", "CM-1 a 1");
		cciToRmf.put("CCI-001823", "CM-1 a 2");
		cciToRmf.put("CCI-001824", "CM-1 a 2");
		cciToRmf.put("CCI-001825", "CM-1 a 2");
		cciToRmf.put("CCI-001726", "CM-10 a");
		cciToRmf.put("CCI-001727", "CM-10 a");
		cciToRmf.put("CCI-001728", "CM-10 a");
		cciToRmf.put("CCI-001729", "CM-10 a");
		cciToRmf.put("CCI-001730", "CM-10 b");
		cciToRmf.put("CCI-001731", "CM-10 b");
		cciToRmf.put("CCI-001732", "CM-10 c");
		cciToRmf.put("CCI-001733", "CM-10 c");
		cciToRmf.put("CCI-001734", "CM-10 (1)");
		cciToRmf.put("CCI-001735", "CM-10 (1)");
		cciToRmf.put("CCI-001802", "CM-10 b");
		cciToRmf.put("CCI-001803", "CM-10 b");
		cciToRmf.put("CCI-001804", "CM-11 a");
		cciToRmf.put("CCI-001805", "CM-11 a");
		cciToRmf.put("CCI-001806", "CM-11 b");
		cciToRmf.put("CCI-001807", "CM-11 b");
		cciToRmf.put("CCI-001808", "CM-11 c");
		cciToRmf.put("CCI-001809", "CM-11 c");
		cciToRmf.put("CCI-001810", "CM-11 (1)");
		cciToRmf.put("CCI-001811", "CM-11 (1)");
		cciToRmf.put("CCI-001812", "CM-11 (2)");
		cciToRmf.put("CCI-001736", "CM-2 (3)");
		cciToRmf.put("CCI-001737", "CM-2 (7) (a)");
		cciToRmf.put("CCI-001738", "CM-2 (7) (a)");
		cciToRmf.put("CCI-001739", "CM-2 (7) (a)");
		cciToRmf.put("CCI-001815", "CM-2 (7) (b)");
		cciToRmf.put("CCI-001816", "CM-2 (7) (b)");
		cciToRmf.put("CCI-001740", "CM-3 b");
		cciToRmf.put("CCI-001741", "CM-3 c");
		cciToRmf.put("CCI-001742", "CM-3 (1) (b)");
		cciToRmf.put("CCI-001743", "CM-3 (5)");
		cciToRmf.put("CCI-001744", "CM-3 (5)");
		cciToRmf.put("CCI-001745", "CM-3 (6)");
		cciToRmf.put("CCI-001746", "CM-3 (6)");
		cciToRmf.put("CCI-001819", "CM-3 d");
		cciToRmf.put("CCI-002056", "CM-3 e");
		cciToRmf.put("CCI-002057", "CM-3 (1) (f)");
		cciToRmf.put("CCI-002058", "CM-3 (1) (f)");
		cciToRmf.put("CCI-001817", "CM-4 (1)");
		cciToRmf.put("CCI-001818", "CM-4 (1)");
		cciToRmf.put("CCI-001747", "CM-5 (3)");
		cciToRmf.put("CCI-001748", "CM-5 (3)");
		cciToRmf.put("CCI-001749", "CM-5 (3)");
		cciToRmf.put("CCI-001750", "CM-5 (3)");
		cciToRmf.put("CCI-001751", "CM-5 (4)");
		cciToRmf.put("CCI-001752", "CM-5 (4)");
		cciToRmf.put("CCI-001753", "CM-5 (5) (a)");
		cciToRmf.put("CCI-001754", "CM-5 (5) (a)");
		cciToRmf.put("CCI-001813", "CM-5 (1)");
		cciToRmf.put("CCI-001814", "CM-5 (1)");
		cciToRmf.put("CCI-001826", "CM-5 (2)");
		cciToRmf.put("CCI-001827", "CM-5 (5) (b)");
		cciToRmf.put("CCI-001828", "CM-5 (5) (b)");
		cciToRmf.put("CCI-001829", "CM-5 (5) (b)");
		cciToRmf.put("CCI-001830", "CM-5 (5) (b)");
		cciToRmf.put("CCI-001755", "CM-6 c");
		cciToRmf.put("CCI-001756", "CM-6 c");
		cciToRmf.put("CCI-001757", "CM-6 (2)");
		cciToRmf.put("CCI-001758", "CM-6 (2)");
		cciToRmf.put("CCI-001759", "CM-6 (2)");
		cciToRmf.put("CCI-002059", "CM-6 (1)");
		cciToRmf.put("CCI-001760", "CM-7 (1) (a)");
		cciToRmf.put("CCI-001761", "CM-7 (1) (b)");
		cciToRmf.put("CCI-001762", "CM-7 (1) (b)");
		cciToRmf.put("CCI-001763", "CM-7 (2)");
		cciToRmf.put("CCI-001764", "CM-7 (2)");
		cciToRmf.put("CCI-001765", "CM-7 (4) (a)");
		cciToRmf.put("CCI-001766", "CM-7 (4) (a)");
		cciToRmf.put("CCI-001767", "CM-7 (4) (b)");
		cciToRmf.put("CCI-001768", "CM-7 (4) (c)");
		cciToRmf.put("CCI-001769", "CM-7 (4) (c)");
		cciToRmf.put("CCI-001770", "CM-7 (4) (c)");
		cciToRmf.put("CCI-001771", "CM-7 (4) (c)");
		cciToRmf.put("CCI-001772", "CM-7 (5) (a)");
		cciToRmf.put("CCI-001773", "CM-7 (5) (a)");
		cciToRmf.put("CCI-001774", "CM-7 (5) (b)");
		cciToRmf.put("CCI-001775", "CM-7 (5) (c)");
		cciToRmf.put("CCI-001776", "CM-7 (5) (c)");
		cciToRmf.put("CCI-001777", "CM-7 (5) (c)");
		cciToRmf.put("CCI-001778", "CM-7 (5) (c)");
		cciToRmf.put("CCI-001779", "CM-8 b");
		cciToRmf.put("CCI-001780", "CM-8 b");
		cciToRmf.put("CCI-001781", "CM-8 b");
		cciToRmf.put("CCI-001782", "CM-8 b");
		cciToRmf.put("CCI-001783", "CM-8 (3) (b)");
		cciToRmf.put("CCI-001784", "CM-8 (3) (b)");
		cciToRmf.put("CCI-001785", "CM-8 (7)");
		cciToRmf.put("CCI-001786", "CM-8 (8)");
		cciToRmf.put("CCI-001787", "CM-8 (9) (a)");
		cciToRmf.put("CCI-001788", "CM-8 (9) (a)");
		cciToRmf.put("CCI-001789", "CM-8 (9) (b)");
		cciToRmf.put("CCI-001790", "CM-9 b");
		cciToRmf.put("CCI-001791", "CM-9 b");
		cciToRmf.put("CCI-001792", "CM-9 b");
		cciToRmf.put("CCI-001793", "CM-9 b");
		cciToRmf.put("CCI-001794", "CM-9 b");
		cciToRmf.put("CCI-001795", "CM-9 b");
		cciToRmf.put("CCI-001796", "CM-9 c");
		cciToRmf.put("CCI-001797", "CM-9 c");
		cciToRmf.put("CCI-001798", "CM-9 c");
		cciToRmf.put("CCI-001799", "CM-9 d");
		cciToRmf.put("CCI-001800", "CM-9 d");
		cciToRmf.put("CCI-001801", "CM-9 d");
		cciToRmf.put("CCI-002825", "CP-1 a 1");
		cciToRmf.put("CCI-002826", "CP-1 a 2");
		cciToRmf.put("CCI-002853", "CP-11");
		cciToRmf.put("CCI-002854", "CP-11");
		cciToRmf.put("CCI-002855", "CP-12");
		cciToRmf.put("CCI-002856", "CP-12");
		cciToRmf.put("CCI-002857", "CP-12");
		cciToRmf.put("CCI-002858", "CP-13");
		cciToRmf.put("CCI-002859", "CP-13");
		cciToRmf.put("CCI-002860", "CP-13");
		cciToRmf.put("CCI-002827", "CP-2 (7)");
		cciToRmf.put("CCI-002828", "CP-2 (8)");
		cciToRmf.put("CCI-002829", "CP-2 (8)");
		cciToRmf.put("CCI-002830", "CP-2 a 6");
		cciToRmf.put("CCI-002831", "CP-2 f");
		cciToRmf.put("CCI-002832", "CP-2 g");
		cciToRmf.put("CCI-002833", "CP-3 a");
		cciToRmf.put("CCI-002834", "CP-3 b");
		cciToRmf.put("CCI-002835", "CP-4 (2) (b)");
		cciToRmf.put("CCI-002836", "CP-6 b");
		cciToRmf.put("CCI-002837", "CP-7 (6)");
		cciToRmf.put("CCI-002838", "CP-7 (6)");
		cciToRmf.put("CCI-002839", "CP-7 a");
		cciToRmf.put("CCI-002840", "CP-8");
		cciToRmf.put("CCI-002841", "CP-8");
		cciToRmf.put("CCI-002842", "CP-8 (4) (b)");
		cciToRmf.put("CCI-002843", "CP-8 (4) (c)");
		cciToRmf.put("CCI-002844", "CP-8 (4) (c)");
		cciToRmf.put("CCI-002845", "CP-8 (4) (c)");
		cciToRmf.put("CCI-002846", "CP-8 (4) (c)");
		cciToRmf.put("CCI-002847", "CP-8 (5)");
		cciToRmf.put("CCI-002848", "CP-8 (5)");
		cciToRmf.put("CCI-002849", "CP-9 (3)");
		cciToRmf.put("CCI-002850", "CP-9 (3)");
		cciToRmf.put("CCI-002851", "CP-9 (7)");
		cciToRmf.put("CCI-002852", "CP-9 (7)");
		cciToRmf.put("CCI-003463", "DI-1 a");
		cciToRmf.put("CCI-003464", "DI-1 a");
		cciToRmf.put("CCI-003465", "DI-1 a");
		cciToRmf.put("CCI-003466", "DI-1 a");
		cciToRmf.put("CCI-003467", "DI-1 b");
		cciToRmf.put("CCI-003468", "DI-1 c");
		cciToRmf.put("CCI-003469", "DI-1 c");
		cciToRmf.put("CCI-003470", "DI-1 d");
		cciToRmf.put("CCI-003471", "DI-1 d");
		cciToRmf.put("CCI-003472", "DI-1 d");
		cciToRmf.put("CCI-003473", "DI-1 d");
		cciToRmf.put("CCI-003474", "DI-1 d");
		cciToRmf.put("CCI-003475", "DI-1 d");
		cciToRmf.put("CCI-003476", "DI-1 d");
		cciToRmf.put("CCI-003477", "DI-1 d");
		cciToRmf.put("CCI-003478", "DI-1 (1)");
		cciToRmf.put("CCI-003479", "DI-1 (2)");
		cciToRmf.put("CCI-003480", "DI-1 (2)");
		cciToRmf.put("CCI-003481", "DI-2 a");
		cciToRmf.put("CCI-003482", "DI-2 b");
		cciToRmf.put("CCI-003483", "DI-2 b");
		cciToRmf.put("CCI-003484", "DI-2 b");
		cciToRmf.put("CCI-003485", "DI-2 (1)");
		cciToRmf.put("CCI-003486", "DM-1 a");
		cciToRmf.put("CCI-003487", "DM-1 b");
		cciToRmf.put("CCI-003488", "DM-1 b");
		cciToRmf.put("CCI-003489", "DM-1 c");
		cciToRmf.put("CCI-003490", "DM-1 c");
		cciToRmf.put("CCI-003491", "DM-1 c");
		cciToRmf.put("CCI-003492", "DM-1 c");
		cciToRmf.put("CCI-003493", "DM-1 c");
		cciToRmf.put("CCI-003494", "DM-1 c");
		cciToRmf.put("CCI-003495", "DM-1 (1)");
		cciToRmf.put("CCI-003496", "DM-1 (1)");
		cciToRmf.put("CCI-003497", "DM-2 a");
		cciToRmf.put("CCI-003498", "DM-2 a");
		cciToRmf.put("CCI-003499", "DM-2 b");
		cciToRmf.put("CCI-003500", "DM-2 b");
		cciToRmf.put("CCI-003501", "DM-2 c");
		cciToRmf.put("CCI-003502", "DM-2 c");
		cciToRmf.put("CCI-003503", "DM-2 (1)");
		cciToRmf.put("CCI-003504", "DM-2 (1)");
		cciToRmf.put("CCI-003505", "DM-2 (1)");
		cciToRmf.put("CCI-003506", "DM-2 (1)");
		cciToRmf.put("CCI-003507", "DM-3 a");
		cciToRmf.put("CCI-003508", "DM-3 a");
		cciToRmf.put("CCI-003509", "DM-3 a");
		cciToRmf.put("CCI-003510", "DM-3 a");
		cciToRmf.put("CCI-003511", "DM-3 a");
		cciToRmf.put("CCI-003512", "DM-3 a");
		cciToRmf.put("CCI-003513", "DM-3 b");
		cciToRmf.put("CCI-003514", "DM-3 b");
		cciToRmf.put("CCI-003515", "DM-3 b");
		cciToRmf.put("CCI-003516", "DM-3 (1)");
		cciToRmf.put("CCI-003517", "DM-3 (1)");
		cciToRmf.put("CCI-003518", "DM-3 (1)");
		cciToRmf.put("CCI-001932", "IA-1 a 1");
		cciToRmf.put("CCI-001933", "IA-1 a 1");
		cciToRmf.put("CCI-001934", "IA-1 a 2");
		cciToRmf.put("CCI-002033", "IA-10");
		cciToRmf.put("CCI-002034", "IA-10");
		cciToRmf.put("CCI-002035", "IA-10");
		cciToRmf.put("CCI-002036", "IA-11");
		cciToRmf.put("CCI-002037", "IA-11");
		cciToRmf.put("CCI-002038", "IA-11");
		cciToRmf.put("CCI-002039", "IA-11");
		cciToRmf.put("CCI-001935", "IA-2 (6)");
		cciToRmf.put("CCI-001936", "IA-2 (6)");
		cciToRmf.put("CCI-001937", "IA-2 (6)");
		cciToRmf.put("CCI-001938", "IA-2 (7)");
		cciToRmf.put("CCI-001939", "IA-2 (7)");
		cciToRmf.put("CCI-001940", "IA-2 (7)");
		cciToRmf.put("CCI-001941", "IA-2 (8)");
		cciToRmf.put("CCI-001942", "IA-2 (9)");
		cciToRmf.put("CCI-001943", "IA-2 (10)");
		cciToRmf.put("CCI-001944", "IA-2 (10)");
		cciToRmf.put("CCI-001945", "IA-2 (10)");
		cciToRmf.put("CCI-001946", "IA-2 (10)");
		cciToRmf.put("CCI-001947", "IA-2 (11)");
		cciToRmf.put("CCI-001948", "IA-2 (11)");
		cciToRmf.put("CCI-001949", "IA-2 (11)");
		cciToRmf.put("CCI-001950", "IA-2 (11)");
		cciToRmf.put("CCI-001951", "IA-2 (11)");
		cciToRmf.put("CCI-001952", "IA-2 (11)");
		cciToRmf.put("CCI-001953", "IA-2 (12)");
		cciToRmf.put("CCI-001954", "IA-2 (12)");
		cciToRmf.put("CCI-001955", "IA-2 (13)");
		cciToRmf.put("CCI-001956", "IA-2 (13)");
		cciToRmf.put("CCI-001957", "IA-2 (13)");
		cciToRmf.put("CCI-001958", "IA-3");
		cciToRmf.put("CCI-001959", "IA-3 (1)");
		cciToRmf.put("CCI-001960", "IA-3 (3) (a)");
		cciToRmf.put("CCI-001961", "IA-3 (3) (a)");
		cciToRmf.put("CCI-001962", "IA-3 (3) (a)");
		cciToRmf.put("CCI-001963", "IA-3 (3) (a)");
		cciToRmf.put("CCI-001964", "IA-3 (4)");
		cciToRmf.put("CCI-001965", "IA-3 (4)");
		cciToRmf.put("CCI-001966", "IA-3 (4)");
		cciToRmf.put("CCI-001967", "IA-3 (1)");
		cciToRmf.put("CCI-001968", "IA-3 (4)");
		cciToRmf.put("CCI-001969", "IA-3 (4)");
		cciToRmf.put("CCI-001970", "IA-4 a");
		cciToRmf.put("CCI-001971", "IA-4 a");
		cciToRmf.put("CCI-001972", "IA-4 b");
		cciToRmf.put("CCI-001973", "IA-4 c");
		cciToRmf.put("CCI-001974", "IA-4 d");
		cciToRmf.put("CCI-001975", "IA-4 d");
		cciToRmf.put("CCI-001976", "IA-4 (5)");
		cciToRmf.put("CCI-001977", "IA-4 (6)");
		cciToRmf.put("CCI-001978", "IA-4 (6)");
		cciToRmf.put("CCI-001979", "IA-4 (7)");
		cciToRmf.put("CCI-002040", "IA-4 (2)");
		cciToRmf.put("CCI-001980", "IA-5 a");
		cciToRmf.put("CCI-001981", "IA-5 d");
		cciToRmf.put("CCI-001982", "IA-5 d");
		cciToRmf.put("CCI-001983", "IA-5 d");
		cciToRmf.put("CCI-001984", "IA-5 d");
		cciToRmf.put("CCI-001985", "IA-5 d");
		cciToRmf.put("CCI-001986", "IA-5 d");
		cciToRmf.put("CCI-001987", "IA-5 d");
		cciToRmf.put("CCI-001988", "IA-5 d");
		cciToRmf.put("CCI-001989", "IA-5 e");
		cciToRmf.put("CCI-001990", "IA-5 j");
		cciToRmf.put("CCI-001991", "IA-5 (2) (d)");
		cciToRmf.put("CCI-001992", "IA-5 (3)");
		cciToRmf.put("CCI-001993", "IA-5 (3)");
		cciToRmf.put("CCI-001994", "IA-5 (3)");
		cciToRmf.put("CCI-001995", "IA-5 (3)");
		cciToRmf.put("CCI-001996", "IA-5 (4)");
		cciToRmf.put("CCI-001997", "IA-5 (4)");
		cciToRmf.put("CCI-001998", "IA-5 (5)");
		cciToRmf.put("CCI-001999", "IA-5 (9)");
		cciToRmf.put("CCI-002000", "IA-5 (9)");
		cciToRmf.put("CCI-002001", "IA-5 (10)");
		cciToRmf.put("CCI-002002", "IA-5 (11)");
		cciToRmf.put("CCI-002003", "IA-5 (11)");
		cciToRmf.put("CCI-002004", "IA-5 (12)");
		cciToRmf.put("CCI-002005", "IA-5 (12)");
		cciToRmf.put("CCI-002006", "IA-5 (13)");
		cciToRmf.put("CCI-002007", "IA-5 (13)");
		cciToRmf.put("CCI-002008", "IA-5 (14)");
		cciToRmf.put("CCI-002041", "IA-5 (1) (f)");
		cciToRmf.put("CCI-002042", "IA-5 h");
		cciToRmf.put("CCI-002043", "IA-5 (15)");
		cciToRmf.put("CCI-002365", "IA-5 i");
		cciToRmf.put("CCI-002366", "IA-5 i");
		cciToRmf.put("CCI-002367", "IA-5 (7)");
		cciToRmf.put("CCI-002009", "IA-8 (1)");
		cciToRmf.put("CCI-002010", "IA-8 (1)");
		cciToRmf.put("CCI-002011", "IA-8 (2)");
		cciToRmf.put("CCI-002012", "IA-8 (3)");
		cciToRmf.put("CCI-002013", "IA-8 (3)");
		cciToRmf.put("CCI-002014", "IA-8 (4)");
		cciToRmf.put("CCI-002015", "IA-8 (5)");
		cciToRmf.put("CCI-002016", "IA-8 (5)");
		cciToRmf.put("CCI-002017", "IA-9");
		cciToRmf.put("CCI-002018", "IA-9");
		cciToRmf.put("CCI-002019", "IA-9");
		cciToRmf.put("CCI-002020", "IA-9");
		cciToRmf.put("CCI-002021", "IA-9");
		cciToRmf.put("CCI-002022", "IA-9");
		cciToRmf.put("CCI-002023", "IA-9 (1)");
		cciToRmf.put("CCI-002024", "IA-9 (1)");
		cciToRmf.put("CCI-002025", "IA-9 (1)");
		cciToRmf.put("CCI-002026", "IA-9 (1)");
		cciToRmf.put("CCI-002027", "IA-9 (1)");
		cciToRmf.put("CCI-002028", "IA-9 (1)");
		cciToRmf.put("CCI-002029", "IA-9 (2)");
		cciToRmf.put("CCI-002030", "IA-9 (2)");
		cciToRmf.put("CCI-002031", "IA-9 (2)");
		cciToRmf.put("CCI-002032", "IA-9 (2)");
		cciToRmf.put("CCI-003519", "IP-1 a");
		cciToRmf.put("CCI-003520", "IP-1 a");
		cciToRmf.put("CCI-003521", "IP-1 a");
		cciToRmf.put("CCI-003522", "IP-1 a");
		cciToRmf.put("CCI-003523", "IP-1 b");
		cciToRmf.put("CCI-003524", "IP-1 b");
		cciToRmf.put("CCI-003525", "IP-1 b");
		cciToRmf.put("CCI-003526", "IP-1 b");
		cciToRmf.put("CCI-003527", "IP-1 c");
		cciToRmf.put("CCI-003528", "IP-1 d");
		cciToRmf.put("CCI-003529", "IP-1 d");
		cciToRmf.put("CCI-003530", "IP-1 (1)");
		cciToRmf.put("CCI-003531", "IP-2 a");
		cciToRmf.put("CCI-003532", "IP-2 b");
		cciToRmf.put("CCI-003533", "IP-2 b");
		cciToRmf.put("CCI-003534", "IP-2 c");
		cciToRmf.put("CCI-003535", "IP-2 d");
		cciToRmf.put("CCI-003536", "IP-2 d");
		cciToRmf.put("CCI-003537", "IP-3 a");
		cciToRmf.put("CCI-003538", "IP-3 b");
		cciToRmf.put("CCI-003539", "IP-3 b");
		cciToRmf.put("CCI-003540", "IP-4");
		cciToRmf.put("CCI-003541", "IP-4");
		cciToRmf.put("CCI-003542", "IP-4 (1)");
		cciToRmf.put("CCI-003543", "IP-4 (1)");
		cciToRmf.put("CCI-002776", "IR-1 a 1");
		cciToRmf.put("CCI-002777", "IR-1 a 2");
		cciToRmf.put("CCI-002822", "IR-10");
		cciToRmf.put("CCI-002778", "IR-2 a");
		cciToRmf.put("CCI-002779", "IR-2 b");
		cciToRmf.put("CCI-002780", "IR-3 (2)");
		cciToRmf.put("CCI-002781", "IR-4 (2)");
		cciToRmf.put("CCI-002782", "IR-4 (6)");
		cciToRmf.put("CCI-002783", "IR-4 (7)");
		cciToRmf.put("CCI-002784", "IR-4 (7)");
		cciToRmf.put("CCI-002785", "IR-4 (8)");
		cciToRmf.put("CCI-002786", "IR-4 (8)");
		cciToRmf.put("CCI-002787", "IR-4 (8)");
		cciToRmf.put("CCI-002788", "IR-4 (9)");
		cciToRmf.put("CCI-002789", "IR-4 (9)");
		cciToRmf.put("CCI-002790", "IR-4 (10)");
		cciToRmf.put("CCI-002791", "IR-6 b");
		cciToRmf.put("CCI-002792", "IR-6 (2)");
		cciToRmf.put("CCI-002793", "IR-6 (3)");
		cciToRmf.put("CCI-002794", "IR-8 a");
		cciToRmf.put("CCI-002795", "IR-8 a 1");
		cciToRmf.put("CCI-002796", "IR-8 a 2");
		cciToRmf.put("CCI-002797", "IR-8 a 3");
		cciToRmf.put("CCI-002798", "IR-8 a 4");
		cciToRmf.put("CCI-002799", "IR-8 a 5");
		cciToRmf.put("CCI-002800", "IR-8 a 6");
		cciToRmf.put("CCI-002801", "IR-8 a 7");
		cciToRmf.put("CCI-002802", "IR-8 a 8");
		cciToRmf.put("CCI-002803", "IR-8 e");
		cciToRmf.put("CCI-002804", "IR-8 f");
		cciToRmf.put("CCI-002805", "IR-9 a");
		cciToRmf.put("CCI-002806", "IR-9 b");
		cciToRmf.put("CCI-002807", "IR-9 b");
		cciToRmf.put("CCI-002808", "IR-9 c");
		cciToRmf.put("CCI-002809", "IR-9 d");
		cciToRmf.put("CCI-002810", "IR-9 e");
		cciToRmf.put("CCI-002811", "IR-9 f");
		cciToRmf.put("CCI-002812", "IR-9 f");
		cciToRmf.put("CCI-002813", "IR-9 (1)");
		cciToRmf.put("CCI-002814", "IR-9 (1)");
		cciToRmf.put("CCI-002815", "IR-9 (1)");
		cciToRmf.put("CCI-002816", "IR-9 (2)");
		cciToRmf.put("CCI-002817", "IR-9 (2)");
		cciToRmf.put("CCI-002818", "IR-9 (3)");
		cciToRmf.put("CCI-002819", "IR-9 (3)");
		cciToRmf.put("CCI-002820", "IR-9 (4)");
		cciToRmf.put("CCI-002821", "IR-9 (4)");
		cciToRmf.put("CCI-002861", "MA-1 a 1");
		cciToRmf.put("CCI-002862", "MA-1 a 2");
		cciToRmf.put("CCI-002863", "MA-2 (2) (a)");
		cciToRmf.put("CCI-002864", "MA-2 (2) (b)");
		cciToRmf.put("CCI-002865", "MA-2 (2) (b)");
		cciToRmf.put("CCI-002866", "MA-2 a");
		cciToRmf.put("CCI-002867", "MA-2 a");
		cciToRmf.put("CCI-002868", "MA-2 a");
		cciToRmf.put("CCI-002869", "MA-2 a");
		cciToRmf.put("CCI-002870", "MA-2 a");
		cciToRmf.put("CCI-002871", "MA-2 a");
		cciToRmf.put("CCI-002872", "MA-2 a");
		cciToRmf.put("CCI-002873", "MA-2 a");
		cciToRmf.put("CCI-002874", "MA-2 c");
		cciToRmf.put("CCI-002875", "MA-2 f");
		cciToRmf.put("CCI-002876", "MA-2 f");
		cciToRmf.put("CCI-002905", "MA-2 (2) (a)");
		cciToRmf.put("CCI-002877", "MA-3 (3) (a)");
		cciToRmf.put("CCI-002878", "MA-3 (3) (b)");
		cciToRmf.put("CCI-002879", "MA-3 (3) (c)");
		cciToRmf.put("CCI-002880", "MA-3 (3) (c)");
		cciToRmf.put("CCI-002881", "MA-3 (3) (d)");
		cciToRmf.put("CCI-002882", "MA-3 (3) (d)");
		cciToRmf.put("CCI-002883", "MA-3 (4)");
		cciToRmf.put("CCI-002884", "MA-4 (1) (a)");
		cciToRmf.put("CCI-002885", "MA-4 (1) (a)");
		cciToRmf.put("CCI-002886", "MA-4 (1) (b)");
		cciToRmf.put("CCI-002887", "MA-4 (4) (a)");
		cciToRmf.put("CCI-002888", "MA-4 (5) (a)");
		cciToRmf.put("CCI-002889", "MA-4 (5) (b)");
		cciToRmf.put("CCI-002890", "MA-4 (6)");
		cciToRmf.put("CCI-002891", "MA-4 (7)");
		cciToRmf.put("CCI-003123", "MA-4 (6)");
		cciToRmf.put("CCI-002892", "MA-5 (1) (b)");
		cciToRmf.put("CCI-002893", "MA-5 (5)");
		cciToRmf.put("CCI-002894", "MA-5 b");
		cciToRmf.put("CCI-002895", "MA-5 c");
		cciToRmf.put("CCI-002896", "MA-6");
		cciToRmf.put("CCI-002897", "MA-6");
		cciToRmf.put("CCI-002898", "MA-6 (1)");
		cciToRmf.put("CCI-002899", "MA-6 (1)");
		cciToRmf.put("CCI-002900", "MA-6 (1)");
		cciToRmf.put("CCI-002901", "MA-6 (2)");
		cciToRmf.put("CCI-002902", "MA-6 (2)");
		cciToRmf.put("CCI-002903", "MA-6 (2)");
		cciToRmf.put("CCI-002904", "MA-6 (3)");
		cciToRmf.put("CCI-002566", "MP-1 a 1");
		cciToRmf.put("CCI-002567", "MP-6 (1)");
		cciToRmf.put("CCI-002568", "MP-6 (1)");
		cciToRmf.put("CCI-002569", "MP-6 (1)");
		cciToRmf.put("CCI-002570", "MP-6 (1)");
		cciToRmf.put("CCI-002571", "MP-6 (1)");
		cciToRmf.put("CCI-002572", "MP-6 (1)");
		cciToRmf.put("CCI-002573", "MP-6 (7)");
		cciToRmf.put("CCI-002574", "MP-6 (7)");
		cciToRmf.put("CCI-002575", "MP-6 (8)");
		cciToRmf.put("CCI-002576", "MP-6 (8)");
		cciToRmf.put("CCI-002577", "MP-6 (8)");
		cciToRmf.put("CCI-002578", "MP-6 a");
		cciToRmf.put("CCI-002579", "MP-6 a");
		cciToRmf.put("CCI-002580", "MP-6 b");
		cciToRmf.put("CCI-002581", "MP-7");
		cciToRmf.put("CCI-002582", "MP-7");
		cciToRmf.put("CCI-002583", "MP-7");
		cciToRmf.put("CCI-002584", "MP-7");
		cciToRmf.put("CCI-002585", "MP-7 (1)");
		cciToRmf.put("CCI-002586", "MP-7 (2)");
		cciToRmf.put("CCI-002587", "MP-8 (1)");
		cciToRmf.put("CCI-002588", "MP-8 (2)");
		cciToRmf.put("CCI-002589", "MP-8 (2)");
		cciToRmf.put("CCI-002590", "MP-8 (2)");
		cciToRmf.put("CCI-002591", "MP-8 (2)");
		cciToRmf.put("CCI-002592", "MP-8 (3)");
		cciToRmf.put("CCI-002593", "MP-8 (3)");
		cciToRmf.put("CCI-002594", "MP-8 (4)");
		cciToRmf.put("CCI-002595", "MP-8 a");
		cciToRmf.put("CCI-002596", "MP-8 a");
		cciToRmf.put("CCI-002597", "MP-8 a");
		cciToRmf.put("CCI-002598", "MP-8 b");
		cciToRmf.put("CCI-002599", "MP-8 c");
		cciToRmf.put("CCI-002600", "MP-8 d");
		cciToRmf.put("CCI-002908", "PE-1 a 1");
		cciToRmf.put("CCI-002909", "PE-1 a 2");
		cciToRmf.put("CCI-002955", "PE-11");
		cciToRmf.put("CCI-002956", "PE-11 (2) (a)");
		cciToRmf.put("CCI-002957", "PE-11 (2) (b)");
		cciToRmf.put("CCI-002958", "PE-11 (2) (c)");
		cciToRmf.put("CCI-002959", "PE-12 (1)");
		cciToRmf.put("CCI-002960", "PE-12 (1)");
		cciToRmf.put("CCI-002961", "PE-13 (1)");
		cciToRmf.put("CCI-002962", "PE-13 (1)");
		cciToRmf.put("CCI-002963", "PE-13 (1)");
		cciToRmf.put("CCI-002964", "PE-13 (1)");
		cciToRmf.put("CCI-002965", "PE-13 (2)");
		cciToRmf.put("CCI-002966", "PE-13 (2)");
		cciToRmf.put("CCI-002967", "PE-13 (2)");
		cciToRmf.put("CCI-002968", "PE-13 (4)");
		cciToRmf.put("CCI-002969", "PE-13 (4)");
		cciToRmf.put("CCI-002970", "PE-13 (4)");
		cciToRmf.put("CCI-002971", "PE-13 (4)");
		cciToRmf.put("CCI-002972", "PE-15 (1)");
		cciToRmf.put("CCI-002973", "PE-15 (1)");
		cciToRmf.put("CCI-002974", "PE-16");
		cciToRmf.put("CCI-002975", "PE-17 a");
		cciToRmf.put("CCI-002976", "PE-18");
		cciToRmf.put("CCI-002977", "PE-18 (1)");
		cciToRmf.put("CCI-002978", "PE-18 (1)");
		cciToRmf.put("CCI-002910", "PE-2 a");
		cciToRmf.put("CCI-002911", "PE-2 a");
		cciToRmf.put("CCI-002912", "PE-2 (2)");
		cciToRmf.put("CCI-002913", "PE-2 (3)");
		cciToRmf.put("CCI-002914", "PE-2 (3)");
		cciToRmf.put("CCI-002979", "PE-20 a");
		cciToRmf.put("CCI-002980", "PE-20 a");
		cciToRmf.put("CCI-002981", "PE-20 a");
		cciToRmf.put("CCI-002982", "PE-20 a");
		cciToRmf.put("CCI-002983", "PE-20 b");
		cciToRmf.put("CCI-002915", "PE-3 a");
		cciToRmf.put("CCI-002916", "PE-3 a 2");
		cciToRmf.put("CCI-002917", "PE-3 b");
		cciToRmf.put("CCI-002918", "PE-3 b");
		cciToRmf.put("CCI-002919", "PE-3 c");
		cciToRmf.put("CCI-002920", "PE-3 c");
		cciToRmf.put("CCI-002921", "PE-3 d");
		cciToRmf.put("CCI-002922", "PE-3 d");
		cciToRmf.put("CCI-002923", "PE-3 d");
		cciToRmf.put("CCI-002924", "PE-3 d");
		cciToRmf.put("CCI-002925", "PE-3 f");
		cciToRmf.put("CCI-002926", "PE-3 (1)");
		cciToRmf.put("CCI-002927", "PE-3 (2)");
		cciToRmf.put("CCI-002928", "PE-3 (5)");
		cciToRmf.put("CCI-002929", "PE-3 (5)");
		cciToRmf.put("CCI-002930", "PE-4");
		cciToRmf.put("CCI-002931", "PE-4");
		cciToRmf.put("CCI-002932", "PE-5 (1) (a)");
		cciToRmf.put("CCI-002933", "PE-5 (1) (a)");
		cciToRmf.put("CCI-002934", "PE-5 (1) (b)");
		cciToRmf.put("CCI-002935", "PE-5 (2) (a)");
		cciToRmf.put("CCI-002936", "PE-5 (2) (b)");
		cciToRmf.put("CCI-002937", "PE-5 (3)");
		cciToRmf.put("CCI-002938", "PE-5 (3)");
		cciToRmf.put("CCI-002939", "PE-6 a");
		cciToRmf.put("CCI-002940", "PE-6 b");
		cciToRmf.put("CCI-002941", "PE-6 b");
		cciToRmf.put("CCI-002942", "PE-6 (2)");
		cciToRmf.put("CCI-002943", "PE-6 (2)");
		cciToRmf.put("CCI-002944", "PE-6 (2)");
		cciToRmf.put("CCI-002945", "PE-6 (2)");
		cciToRmf.put("CCI-002946", "PE-6 (3)");
		cciToRmf.put("CCI-002947", "PE-6 (3)");
		cciToRmf.put("CCI-002948", "PE-6 (3)");
		cciToRmf.put("CCI-002949", "PE-6 (3)");
		cciToRmf.put("CCI-002950", "PE-6 (4)");
		cciToRmf.put("CCI-002951", "PE-6 (4)");
		cciToRmf.put("CCI-002952", "PE-8 a");
		cciToRmf.put("CCI-002953", "PE-9 (1)");
		cciToRmf.put("CCI-002954", "PE-9 (1)");
		cciToRmf.put("CCI-003047", "PL-1 a 1");
		cciToRmf.put("CCI-003048", "PL-1 a 2");
		cciToRmf.put("CCI-003049", "PL-2 a");
		cciToRmf.put("CCI-003050", "PL-2 a 1");
		cciToRmf.put("CCI-003051", "PL-2 a 2");
		cciToRmf.put("CCI-003052", "PL-2 a 3");
		cciToRmf.put("CCI-003053", "PL-2 a 4");
		cciToRmf.put("CCI-003054", "PL-2 a 5");
		cciToRmf.put("CCI-003055", "PL-2 a 6");
		cciToRmf.put("CCI-003056", "PL-2 a 7");
		cciToRmf.put("CCI-003057", "PL-2 a 8");
		cciToRmf.put("CCI-003058", "PL-2 b");
		cciToRmf.put("CCI-003059", "PL-2 b");
		cciToRmf.put("CCI-003060", "PL-2 b");
		cciToRmf.put("CCI-003061", "PL-2 b");
		cciToRmf.put("CCI-003062", "PL-2 b");
		cciToRmf.put("CCI-003063", "PL-2 e");
		cciToRmf.put("CCI-003064", "PL-2 e");
		cciToRmf.put("CCI-003065", "PL-2 (3)");
		cciToRmf.put("CCI-003066", "PL-2 (3)");
		cciToRmf.put("CCI-003067", "PL-2 (3)");
		cciToRmf.put("CCI-003068", "PL-4 c");
		cciToRmf.put("CCI-003069", "PL-4 c");
		cciToRmf.put("CCI-003070", "PL-4 d");
		cciToRmf.put("CCI-003071", "PL-7 a");
		cciToRmf.put("CCI-003072", "PL-8 a");
		cciToRmf.put("CCI-003073", "PL-8 a 1");
		cciToRmf.put("CCI-003074", "PL-8 a 2");
		cciToRmf.put("CCI-003075", "PL-8 a 3");
		cciToRmf.put("CCI-003076", "PL-8 b");
		cciToRmf.put("CCI-003077", "PL-8 b");
		cciToRmf.put("CCI-003078", "PL-8 c");
		cciToRmf.put("CCI-003079", "PL-8 c");
		cciToRmf.put("CCI-003080", "PL-8 c");
		cciToRmf.put("CCI-003081", "PL-8 (1) (a)");
		cciToRmf.put("CCI-003082", "PL-8 (1) (a)");
		cciToRmf.put("CCI-003083", "PL-8 (1) (a)");
		cciToRmf.put("CCI-003084", "PL-8 (1) (a)");
		cciToRmf.put("CCI-003085", "PL-8 (1) (a)");
		cciToRmf.put("CCI-003086", "PL-8 (1) (a)");
		cciToRmf.put("CCI-003087", "PL-8 (1) (b)");
		cciToRmf.put("CCI-003088", "PL-8 (2)");
		cciToRmf.put("CCI-003117", "PL-9");
		cciToRmf.put("CCI-003118", "PL-9");
		cciToRmf.put("CCI-002984", "PM-1 a 3");
		cciToRmf.put("CCI-002985", "PM-1 a 1");
		cciToRmf.put("CCI-002986", "PM-1 a 2");
		cciToRmf.put("CCI-002987", "PM-1 a 3");
		cciToRmf.put("CCI-002988", "PM-1 a 4");
		cciToRmf.put("CCI-002989", "PM-1 d");
		cciToRmf.put("CCI-002990", "PM-1 d");
		cciToRmf.put("CCI-002996", "PM-12");
		cciToRmf.put("CCI-002997", "PM-13");
		cciToRmf.put("CCI-002998", "PM-14 a 1");
		cciToRmf.put("CCI-002999", "PM-14 a 1");
		cciToRmf.put("CCI-003000", "PM-14 a 1");
		cciToRmf.put("CCI-003001", "PM-14 a 1");
		cciToRmf.put("CCI-003002", "PM-14 a 1");
		cciToRmf.put("CCI-003003", "PM-14 a 1");
		cciToRmf.put("CCI-003004", "PM-14 a 2");
		cciToRmf.put("CCI-003005", "PM-14 a 2");
		cciToRmf.put("CCI-003006", "PM-14 a 2");
		cciToRmf.put("CCI-003007", "PM-14 b");
		cciToRmf.put("CCI-003008", "PM-14 b");
		cciToRmf.put("CCI-003009", "PM-14 b");
		cciToRmf.put("CCI-003010", "PM-15 a");
		cciToRmf.put("CCI-003011", "PM-15 b");
		cciToRmf.put("CCI-003012", "PM-15 c");
		cciToRmf.put("CCI-003013", "PM-16");
		cciToRmf.put("CCI-002991", "PM-4 a 1");
		cciToRmf.put("CCI-002992", "PM-4 a 3");
		cciToRmf.put("CCI-002993", "PM-4 b");
		cciToRmf.put("CCI-002994", "PM-9 c");
		cciToRmf.put("CCI-002995", "PM-9 c");
		cciToRmf.put("CCI-003017", "PS-1 a 1");
		cciToRmf.put("CCI-003018", "PS-1 a 2");
		cciToRmf.put("CCI-003019", "PS-3 (3) (a)");
		cciToRmf.put("CCI-003020", "PS-3 (3) (b)");
		cciToRmf.put("CCI-003021", "PS-3 (3) (b)");
		cciToRmf.put("CCI-003016", "PS-4 f");
		cciToRmf.put("CCI-003022", "PS-4 a");
		cciToRmf.put("CCI-003023", "PS-4 b");
		cciToRmf.put("CCI-003024", "PS-4 c");
		cciToRmf.put("CCI-003025", "PS-4 f");
		cciToRmf.put("CCI-003026", "PS-4 f");
		cciToRmf.put("CCI-003027", "PS-4 (1) (a)");
		cciToRmf.put("CCI-003028", "PS-4 (1) (b)");
		cciToRmf.put("CCI-003029", "PS-4 (2)");
		cciToRmf.put("CCI-003030", "PS-4 (2)");
		cciToRmf.put("CCI-003031", "PS-5 c");
		cciToRmf.put("CCI-003032", "PS-5 d");
		cciToRmf.put("CCI-003033", "PS-5 d");
		cciToRmf.put("CCI-003034", "PS-5 d");
		cciToRmf.put("CCI-003035", "PS-6 a");
		cciToRmf.put("CCI-003036", "PS-6 c 2");
		cciToRmf.put("CCI-003037", "PS-6 c 2");
		cciToRmf.put("CCI-003038", "PS-6 (3) (a)");
		cciToRmf.put("CCI-003039", "PS-6 (3) (b)");
		cciToRmf.put("CCI-003040", "PS-7 b");
		cciToRmf.put("CCI-003041", "PS-7 d");
		cciToRmf.put("CCI-003042", "PS-7 d");
		cciToRmf.put("CCI-003043", "PS-7 d");
		cciToRmf.put("CCI-003044", "PS-8 b");
		cciToRmf.put("CCI-003045", "PS-8 b");
		cciToRmf.put("CCI-003046", "PS-8 b");
		cciToRmf.put("CCI-002368", "RA-1 a 1");
		cciToRmf.put("CCI-002369", "RA-1 a 2");
		cciToRmf.put("CCI-002370", "RA-3 d");
		cciToRmf.put("CCI-002371", "RA-3 d");
		cciToRmf.put("CCI-002372", "RA-5 (10)");
		cciToRmf.put("CCI-002373", "RA-5 (3)");
		cciToRmf.put("CCI-002374", "RA-5 (4)");
		cciToRmf.put("CCI-002375", "RA-5 (4)");
		cciToRmf.put("CCI-002376", "RA-5 e");
		cciToRmf.put("CCI-002906", "RA-5 (5)");
		cciToRmf.put("CCI-003119", "RA-6");
		cciToRmf.put("CCI-003120", "RA-6");
		cciToRmf.put("CCI-003121", "RA-6");
		cciToRmf.put("CCI-003122", "RA-6");
		cciToRmf.put("CCI-003089", "SA-1 a 1");
		cciToRmf.put("CCI-003090", "SA-1 a 2");
		cciToRmf.put("CCI-003155", "SA-10 a");
		cciToRmf.put("CCI-003156", "SA-10 b");
		cciToRmf.put("CCI-003157", "SA-10 b");
		cciToRmf.put("CCI-003158", "SA-10 b");
		cciToRmf.put("CCI-003159", "SA-10 b");
		cciToRmf.put("CCI-003160", "SA-10 d");
		cciToRmf.put("CCI-003161", "SA-10 e");
		cciToRmf.put("CCI-003162", "SA-10 e");
		cciToRmf.put("CCI-003163", "SA-10 e");
		cciToRmf.put("CCI-003164", "SA-10 e");
		cciToRmf.put("CCI-003165", "SA-10 (3)");
		cciToRmf.put("CCI-003166", "SA-10 (4)");
		cciToRmf.put("CCI-003167", "SA-10 (4)");
		cciToRmf.put("CCI-003168", "SA-10 (4)");
		cciToRmf.put("CCI-003169", "SA-10 (5)");
		cciToRmf.put("CCI-003170", "SA-10 (6)");
		cciToRmf.put("CCI-003171", "SA-11 a");
		cciToRmf.put("CCI-003172", "SA-11 a");
		cciToRmf.put("CCI-003173", "SA-11 b");
		cciToRmf.put("CCI-003174", "SA-11 b");
		cciToRmf.put("CCI-003175", "SA-11 c");
		cciToRmf.put("CCI-003176", "SA-11 c");
		cciToRmf.put("CCI-003177", "SA-11 d");
		cciToRmf.put("CCI-003178", "SA-11 e");
		cciToRmf.put("CCI-003179", "SA-11 (1)");
		cciToRmf.put("CCI-003180", "SA-11 (1)");
		cciToRmf.put("CCI-003181", "SA-11 (2)");
		cciToRmf.put("CCI-003182", "SA-11 (2)");
		cciToRmf.put("CCI-003183", "SA-11 (3) (a)");
		cciToRmf.put("CCI-003184", "SA-11 (3) (a)");
		cciToRmf.put("CCI-003185", "SA-11 (3) (a)");
		cciToRmf.put("CCI-003186", "SA-11 (3) (b)");
		cciToRmf.put("CCI-003187", "SA-11 (4)");
		cciToRmf.put("CCI-003188", "SA-11 (4)");
		cciToRmf.put("CCI-003189", "SA-11 (4)");
		cciToRmf.put("CCI-003190", "SA-11 (5)");
		cciToRmf.put("CCI-003191", "SA-11 (5)");
		cciToRmf.put("CCI-003192", "SA-11 (5)");
		cciToRmf.put("CCI-003193", "SA-11 (6)");
		cciToRmf.put("CCI-003194", "SA-11 (7)");
		cciToRmf.put("CCI-003195", "SA-11 (7)");
		cciToRmf.put("CCI-003196", "SA-11 (8)");
		cciToRmf.put("CCI-003197", "SA-11 (8)");
		cciToRmf.put("CCI-003198", "SA-12 (1)");
		cciToRmf.put("CCI-003199", "SA-12 (1)");
		cciToRmf.put("CCI-003200", "SA-12 (2)");
		cciToRmf.put("CCI-003201", "SA-12 (5)");
		cciToRmf.put("CCI-003202", "SA-12 (5)");
		cciToRmf.put("CCI-003203", "SA-12 (7)");
		cciToRmf.put("CCI-003204", "SA-12 (7)");
		cciToRmf.put("CCI-003205", "SA-12 (8)");
		cciToRmf.put("CCI-003206", "SA-12 (9)");
		cciToRmf.put("CCI-003207", "SA-12 (1)");
		cciToRmf.put("CCI-003208", "SA-12 (1)");
		cciToRmf.put("CCI-003209", "SA-12 (1)");
		cciToRmf.put("CCI-003210", "SA-12 (9)");
		cciToRmf.put("CCI-003211", "SA-12 (9)");
		cciToRmf.put("CCI-003212", "SA-12 (10)");
		cciToRmf.put("CCI-003213", "SA-12 (10)");
		cciToRmf.put("CCI-003214", "SA-12 (11)");
		cciToRmf.put("CCI-003215", "SA-12 (11)");
		cciToRmf.put("CCI-003216", "SA-12 (12)");
		cciToRmf.put("CCI-003217", "SA-12 (12)");
		cciToRmf.put("CCI-003218", "SA-12 (13)");
		cciToRmf.put("CCI-003219", "SA-12 (13)");
		cciToRmf.put("CCI-003220", "SA-12 (13)");
		cciToRmf.put("CCI-003221", "SA-12 (14)");
		cciToRmf.put("CCI-003222", "SA-12 (14)");
		cciToRmf.put("CCI-003223", "SA-12 (14)");
		cciToRmf.put("CCI-003224", "SA-12 (15)");
		cciToRmf.put("CCI-003225", "SA-13 a");
		cciToRmf.put("CCI-003226", "SA-13 a");
		cciToRmf.put("CCI-003227", "SA-13 b");
		cciToRmf.put("CCI-003228", "SA-13 b");
		cciToRmf.put("CCI-003229", "SA-14");
		cciToRmf.put("CCI-003230", "SA-14");
		cciToRmf.put("CCI-003231", "SA-14");
		cciToRmf.put("CCI-003232", "SA-14");
		cciToRmf.put("CCI-003233", "SA-15");
		cciToRmf.put("CCI-003234", "SA-15 a 1");
		cciToRmf.put("CCI-003235", "SA-15 a 2");
		cciToRmf.put("CCI-003236", "SA-15 a 2");
		cciToRmf.put("CCI-003237", "SA-15 a 3");
		cciToRmf.put("CCI-003238", "SA-15 a 4");
		cciToRmf.put("CCI-003239", "SA-15 a 4");
		cciToRmf.put("CCI-003240", "SA-15 a 4");
		cciToRmf.put("CCI-003241", "SA-15 b");
		cciToRmf.put("CCI-003242", "SA-15 b");
		cciToRmf.put("CCI-003243", "SA-15 b");
		cciToRmf.put("CCI-003244", "SA-15 b");
		cciToRmf.put("CCI-003245", "SA-15 b");
		cciToRmf.put("CCI-003246", "SA-15 b");
		cciToRmf.put("CCI-003247", "SA-15 (1) (a)");
		cciToRmf.put("CCI-003248", "SA-15 (1) (b)");
		cciToRmf.put("CCI-003249", "SA-15 (1) (b)");
		cciToRmf.put("CCI-003250", "SA-15 (1) (b)");
		cciToRmf.put("CCI-003251", "SA-15 (2)");
		cciToRmf.put("CCI-003252", "SA-15 (2)");
		cciToRmf.put("CCI-003253", "SA-15 (3)");
		cciToRmf.put("CCI-003254", "SA-15 (3)");
		cciToRmf.put("CCI-003255", "SA-15 (3)");
		cciToRmf.put("CCI-003256", "SA-15 (4)");
		cciToRmf.put("CCI-003257", "SA-15 (4)");
		cciToRmf.put("CCI-003258", "SA-15 (4)");
		cciToRmf.put("CCI-003259", "SA-15 (4)");
		cciToRmf.put("CCI-003260", "SA-15 (4) (a)");
		cciToRmf.put("CCI-003261", "SA-15 (4) (a)");
		cciToRmf.put("CCI-003262", "SA-15 (4) (a)");
		cciToRmf.put("CCI-003263", "SA-15 (4) (a)");
		cciToRmf.put("CCI-003264", "SA-15 (4) (b)");
		cciToRmf.put("CCI-003265", "SA-15 (4) (b)");
		cciToRmf.put("CCI-003266", "SA-15 (4) (b)");
		cciToRmf.put("CCI-003267", "SA-15 (4) (b)");
		cciToRmf.put("CCI-003268", "SA-15 (4) (c)");
		cciToRmf.put("CCI-003269", "SA-15 (4) (c)");
		cciToRmf.put("CCI-003270", "SA-15 (4) (c)");
		cciToRmf.put("CCI-003271", "SA-15 (4) (c)");
		cciToRmf.put("CCI-003272", "SA-15 (5)");
		cciToRmf.put("CCI-003273", "SA-15 (5)");
		cciToRmf.put("CCI-003274", "SA-15 (6)");
		cciToRmf.put("CCI-003275", "SA-15 (7) (a)");
		cciToRmf.put("CCI-003276", "SA-15 (7) (a)");
		cciToRmf.put("CCI-003277", "SA-15 (7) (b)");
		cciToRmf.put("CCI-003278", "SA-15 (7) (c)");
		cciToRmf.put("CCI-003279", "SA-15 (7) (d)");
		cciToRmf.put("CCI-003280", "SA-15 (7) (d)");
		cciToRmf.put("CCI-003281", "SA-15 (8)");
		cciToRmf.put("CCI-003282", "SA-15 (8)");
		cciToRmf.put("CCI-003283", "SA-15 (9)");
		cciToRmf.put("CCI-003284", "SA-15 (9)");
		cciToRmf.put("CCI-003285", "SA-15 (9)");
		cciToRmf.put("CCI-003286", "SA-15 (9)");
		cciToRmf.put("CCI-003287", "SA-15 (9)");
		cciToRmf.put("CCI-003288", "SA-15 (9)");
		cciToRmf.put("CCI-003289", "SA-15 (10)");
		cciToRmf.put("CCI-003290", "SA-15 (11)");
		cciToRmf.put("CCI-003291", "SA-16");
		cciToRmf.put("CCI-003292", "SA-16");
		cciToRmf.put("CCI-003293", "SA-17");
		cciToRmf.put("CCI-003294", "SA-17 a");
		cciToRmf.put("CCI-003295", "SA-17 b");
		cciToRmf.put("CCI-003296", "SA-17 b");
		cciToRmf.put("CCI-003297", "SA-17 c");
		cciToRmf.put("CCI-003298", "SA-17 (1) (a)");
		cciToRmf.put("CCI-003299", "SA-17 (1) (a)");
		cciToRmf.put("CCI-003300", "SA-17 (1) (b)");
		cciToRmf.put("CCI-003301", "SA-17 (2) (a)");
		cciToRmf.put("CCI-003302", "SA-17 (2) (a)");
		cciToRmf.put("CCI-003303", "SA-17 (2) (a)");
		cciToRmf.put("CCI-003304", "SA-17 (2) (a)");
		cciToRmf.put("CCI-003305", "SA-17 (2) (a)");
		cciToRmf.put("CCI-003306", "SA-17 (2) (b)");
		cciToRmf.put("CCI-003307", "SA-17 (2) (b)");
		cciToRmf.put("CCI-003308", "SA-17 (3) (a)");
		cciToRmf.put("CCI-003309", "SA-17 (3) (a)");
		cciToRmf.put("CCI-003310", "SA-17 (3) (a)");
		cciToRmf.put("CCI-003311", "SA-17 (3) (b)");
		cciToRmf.put("CCI-003312", "SA-17 (3) (c)");
		cciToRmf.put("CCI-003313", "SA-17 (3) (c)");
		cciToRmf.put("CCI-003314", "SA-17 (3) (c)");
		cciToRmf.put("CCI-003315", "SA-17 (3) (d)");
		cciToRmf.put("CCI-003316", "SA-17 (3) (d)");
		cciToRmf.put("CCI-003317", "SA-17 (3) (d)");
		cciToRmf.put("CCI-003318", "SA-17 (3) (e)");
		cciToRmf.put("CCI-003319", "SA-17 (3) (e)");
		cciToRmf.put("CCI-003320", "SA-17 (3) (e)");
		cciToRmf.put("CCI-003321", "SA-17 (4) (a)");
		cciToRmf.put("CCI-003322", "SA-17 (4) (a)");
		cciToRmf.put("CCI-003323", "SA-17 (4) (a)");
		cciToRmf.put("CCI-003324", "SA-17 (4) (b)");
		cciToRmf.put("CCI-003325", "SA-17 (4) (c)");
		cciToRmf.put("CCI-003326", "SA-17 (4) (c)");
		cciToRmf.put("CCI-003327", "SA-17 (4) (c)");
		cciToRmf.put("CCI-003328", "SA-17 (4) (d)");
		cciToRmf.put("CCI-003329", "SA-17 (4) (d)");
		cciToRmf.put("CCI-003330", "SA-17 (4) (d)");
		cciToRmf.put("CCI-003331", "SA-17 (4) (e)");
		cciToRmf.put("CCI-003332", "SA-17 (4) (e)");
		cciToRmf.put("CCI-003333", "SA-17 (4) (e)");
		cciToRmf.put("CCI-003334", "SA-17 (5) (a)");
		cciToRmf.put("CCI-003335", "SA-17 (5) (a)");
		cciToRmf.put("CCI-003336", "SA-17 (5) (a)");
		cciToRmf.put("CCI-003337", "SA-17 (5) (b)");
		cciToRmf.put("CCI-003338", "SA-17 (5) (b)");
		cciToRmf.put("CCI-003339", "SA-17 (5) (b)");
		cciToRmf.put("CCI-003340", "SA-17 (6)");
		cciToRmf.put("CCI-003341", "SA-17 (6)");
		cciToRmf.put("CCI-003342", "SA-17 (6)");
		cciToRmf.put("CCI-003343", "SA-17 (7)");
		cciToRmf.put("CCI-003344", "SA-17 (7)");
		cciToRmf.put("CCI-003345", "SA-17 (7)");
		cciToRmf.put("CCI-003346", "SA-18");
		cciToRmf.put("CCI-003347", "SA-18 (1)");
		cciToRmf.put("CCI-003348", "SA-18 (1)");
		cciToRmf.put("CCI-003349", "SA-18 (1)");
		cciToRmf.put("CCI-003350", "SA-18 (1)");
		cciToRmf.put("CCI-003351", "SA-18 (1)");
		cciToRmf.put("CCI-003352", "SA-18 (2)");
		cciToRmf.put("CCI-003353", "SA-18 (2)");
		cciToRmf.put("CCI-003354", "SA-18 (2)");
		cciToRmf.put("CCI-003355", "SA-18 (2)");
		cciToRmf.put("CCI-003356", "SA-19 a");
		cciToRmf.put("CCI-003357", "SA-19 a");
		cciToRmf.put("CCI-003358", "SA-19 a");
		cciToRmf.put("CCI-003359", "SA-19 a");
		cciToRmf.put("CCI-003360", "SA-19 a");
		cciToRmf.put("CCI-003361", "SA-19 a");
		cciToRmf.put("CCI-003362", "SA-19 a");
		cciToRmf.put("CCI-003363", "SA-19 a");
		cciToRmf.put("CCI-003364", "SA-19 b");
		cciToRmf.put("CCI-003365", "SA-19 b");
		cciToRmf.put("CCI-003366", "SA-19 b");
		cciToRmf.put("CCI-003367", "SA-19 (1)");
		cciToRmf.put("CCI-003368", "SA-19 (1)");
		cciToRmf.put("CCI-003369", "SA-19 (2)");
		cciToRmf.put("CCI-003370", "SA-19 (2)");
		cciToRmf.put("CCI-003371", "SA-19 (2)");
		cciToRmf.put("CCI-003388", "SA-19 (4)");
		cciToRmf.put("CCI-003389", "SA-19 (4)");
		cciToRmf.put("CCI-003390", "SA-19 (3)");
		cciToRmf.put("CCI-003391", "SA-19 (3)");
		cciToRmf.put("CCI-003091", "SA-2 a");
		cciToRmf.put("CCI-003386", "SA-20");
		cciToRmf.put("CCI-003387", "SA-20");
		cciToRmf.put("CCI-003377", "SA-21 (1)");
		cciToRmf.put("CCI-003378", "SA-21 (1)");
		cciToRmf.put("CCI-003379", "SA-21 (1)");
		cciToRmf.put("CCI-003380", "SA-21 (1)");
		cciToRmf.put("CCI-003381", "SA-21 b");
		cciToRmf.put("CCI-003382", "SA-21 b");
		cciToRmf.put("CCI-003383", "SA-21 a");
		cciToRmf.put("CCI-003384", "SA-21");
		cciToRmf.put("CCI-003385", "SA-21 a");
		cciToRmf.put("CCI-003372", "SA-22 (1)");
		cciToRmf.put("CCI-003373", "SA-22 (1)");
		cciToRmf.put("CCI-003374", "SA-22 b");
		cciToRmf.put("CCI-003375", "SA-22 b");
		cciToRmf.put("CCI-003376", "SA-22 a");
		cciToRmf.put("CCI-003092", "SA-3 a");
		cciToRmf.put("CCI-003093", "SA-3 d");
		cciToRmf.put("CCI-003094", "SA-4 a");
		cciToRmf.put("CCI-003095", "SA-4 b");
		cciToRmf.put("CCI-003096", "SA-4 c");
		cciToRmf.put("CCI-003097", "SA-4 d");
		cciToRmf.put("CCI-003098", "SA-4 e");
		cciToRmf.put("CCI-003099", "SA-4 f");
		cciToRmf.put("CCI-003100", "SA-4 g");
		cciToRmf.put("CCI-003101", "SA-4 (2)");
		cciToRmf.put("CCI-003102", "SA-4 (2)");
		cciToRmf.put("CCI-003103", "SA-4 (2)");
		cciToRmf.put("CCI-003104", "SA-4 (2)");
		cciToRmf.put("CCI-003105", "SA-4 (2)");
		cciToRmf.put("CCI-003106", "SA-4 (2)");
		cciToRmf.put("CCI-003107", "SA-4 (3)");
		cciToRmf.put("CCI-003108", "SA-4 (3)");
		cciToRmf.put("CCI-003109", "SA-4 (5) (a)");
		cciToRmf.put("CCI-003110", "SA-4 (5) (a)");
		cciToRmf.put("CCI-003111", "SA-4 (5) (b)");
		cciToRmf.put("CCI-003112", "SA-4 (8)");
		cciToRmf.put("CCI-003113", "SA-4 (8)");
		cciToRmf.put("CCI-003114", "SA-4 (9)");
		cciToRmf.put("CCI-003115", "SA-4 (9)");
		cciToRmf.put("CCI-003116", "SA-4 (10)");
		cciToRmf.put("CCI-003124", "SA-5 a 1");
		cciToRmf.put("CCI-003125", "SA-5 a 1");
		cciToRmf.put("CCI-003126", "SA-5 a 1");
		cciToRmf.put("CCI-003127", "SA-5 a 2");
		cciToRmf.put("CCI-003128", "SA-5 a 3");
		cciToRmf.put("CCI-003129", "SA-5 b 1");
		cciToRmf.put("CCI-003130", "SA-5 b 2");
		cciToRmf.put("CCI-003131", "SA-5 b 3");
		cciToRmf.put("CCI-003132", "SA-5 c");
		cciToRmf.put("CCI-003133", "SA-5 c");
		cciToRmf.put("CCI-003134", "SA-5 d");
		cciToRmf.put("CCI-003135", "SA-5 e");
		cciToRmf.put("CCI-003136", "SA-5 e");
		cciToRmf.put("CCI-003137", "SA-9 a");
		cciToRmf.put("CCI-003138", "SA-9 c");
		cciToRmf.put("CCI-003139", "SA-9 c");
		cciToRmf.put("CCI-003140", "SA-9  (1) (a)");
		cciToRmf.put("CCI-003141", "SA-9 (1) (b)");
		cciToRmf.put("CCI-003142", "SA-9 (1) (b)");
		cciToRmf.put("CCI-003143", "SA-9 (2)");
		cciToRmf.put("CCI-003144", "SA-9 (2)");
		cciToRmf.put("CCI-003145", "SA-9 (3)");
		cciToRmf.put("CCI-003146", "SA-9 (3)");
		cciToRmf.put("CCI-003147", "SA-9 (3)");
		cciToRmf.put("CCI-003148", "SA-9 (3)");
		cciToRmf.put("CCI-003149", "SA-9 (4)");
		cciToRmf.put("CCI-003150", "SA-9 (4)");
		cciToRmf.put("CCI-003151", "SA-9 (4)");
		cciToRmf.put("CCI-003152", "SA-9 (5)");
		cciToRmf.put("CCI-003153", "SA-9 (5)");
		cciToRmf.put("CCI-003154", "SA-9 (5)");
		cciToRmf.put("CCI-002377", "SC-1 a 1");
		cciToRmf.put("CCI-002378", "SC-1 a 1");
		cciToRmf.put("CCI-002379", "SC-1 a 2");
		cciToRmf.put("CCI-002380", "SC-1 a 2");
		cciToRmf.put("CCI-002426", "SC-11 (1)");
		cciToRmf.put("CCI-002428", "SC-12");
		cciToRmf.put("CCI-002429", "SC-12");
		cciToRmf.put("CCI-002430", "SC-12");
		cciToRmf.put("CCI-002431", "SC-12");
		cciToRmf.put("CCI-002432", "SC-12");
		cciToRmf.put("CCI-002433", "SC-12");
		cciToRmf.put("CCI-002434", "SC-12");
		cciToRmf.put("CCI-002435", "SC-12");
		cciToRmf.put("CCI-002436", "SC-12");
		cciToRmf.put("CCI-002437", "SC-12");
		cciToRmf.put("CCI-002438", "SC-12");
		cciToRmf.put("CCI-002439", "SC-12");
		cciToRmf.put("CCI-002440", "SC-12");
		cciToRmf.put("CCI-002441", "SC-12");
		cciToRmf.put("CCI-002442", "SC-12");
		cciToRmf.put("CCI-002443", "SC-12 (2)");
		cciToRmf.put("CCI-002444", "SC-12 (2)");
		cciToRmf.put("CCI-002445", "SC-12 (2)");
		cciToRmf.put("CCI-002446", "SC-12 (3)");
		cciToRmf.put("CCI-002447", "SC-12 (3)");
		cciToRmf.put("CCI-002448", "SC-12 (3)");
		cciToRmf.put("CCI-002449", "SC-13");
		cciToRmf.put("CCI-002450", "SC-13");
		cciToRmf.put("CCI-002451", "SC-15 (3)");
		cciToRmf.put("CCI-002452", "SC-15 (4)");
		cciToRmf.put("CCI-002453", "SC-15 (4)");
		cciToRmf.put("CCI-002454", "SC-16");
		cciToRmf.put("CCI-002455", "SC-16");
		cciToRmf.put("CCI-002456", "SC-17");
		cciToRmf.put("CCI-002457", "SC-18 (1)");
		cciToRmf.put("CCI-002458", "SC-18 (1)");
		cciToRmf.put("CCI-002459", "SC-18 (3)");
		cciToRmf.put("CCI-002460", "SC-18 (4)");
		cciToRmf.put("CCI-002461", "SC-18 (5)");
		cciToRmf.put("CCI-002462", "SC-20 a");
		cciToRmf.put("CCI-002463", "SC-20 (2)");
		cciToRmf.put("CCI-002464", "SC-20 (2)");
		cciToRmf.put("CCI-002465", "SC-21");
		cciToRmf.put("CCI-002466", "SC-21");
		cciToRmf.put("CCI-002467", "SC-21");
		cciToRmf.put("CCI-002468", "SC-21");
		cciToRmf.put("CCI-002469", "SC-23 (5)");
		cciToRmf.put("CCI-002470", "SC-23 (5)");
		cciToRmf.put("CCI-002471", "SC-25");
		cciToRmf.put("CCI-002472", "SC-28");
		cciToRmf.put("CCI-002473", "SC-28 (1)");
		cciToRmf.put("CCI-002474", "SC-28 (1)");
		cciToRmf.put("CCI-002475", "SC-28 (1)");
		cciToRmf.put("CCI-002476", "SC-28 (1)");
		cciToRmf.put("CCI-002477", "SC-28 (2)");
		cciToRmf.put("CCI-002478", "SC-28 (2)");
		cciToRmf.put("CCI-002479", "SC-28 (2)");
		cciToRmf.put("CCI-002480", "SC-29");
		cciToRmf.put("CCI-002481", "SC-29 (1)");
		cciToRmf.put("CCI-002381", "SC-3 (3)");
		cciToRmf.put("CCI-002382", "SC-3 (4)");
		cciToRmf.put("CCI-002482", "SC-30");
		cciToRmf.put("CCI-002483", "SC-30");
		cciToRmf.put("CCI-002484", "SC-30");
		cciToRmf.put("CCI-002485", "SC-30");
		cciToRmf.put("CCI-002486", "SC-30 (2)");
		cciToRmf.put("CCI-002487", "SC-30 (2)");
		cciToRmf.put("CCI-002488", "SC-30 (2)");
		cciToRmf.put("CCI-002489", "SC-30 (3)");
		cciToRmf.put("CCI-002490", "SC-30 (3)");
		cciToRmf.put("CCI-002491", "SC-30 (3)");
		cciToRmf.put("CCI-002492", "SC-30 (3)");
		cciToRmf.put("CCI-002493", "SC-30 (4)");
		cciToRmf.put("CCI-002494", "SC-30 (4)");
		cciToRmf.put("CCI-002495", "SC-30 (5)");
		cciToRmf.put("CCI-002496", "SC-30 (5)");
		cciToRmf.put("CCI-002497", "SC-30 (5)");
		cciToRmf.put("CCI-002498", "SC-31 a");
		cciToRmf.put("CCI-002499", "SC-31 b");
		cciToRmf.put("CCI-002500", "SC-31 (2)");
		cciToRmf.put("CCI-002501", "SC-31 (2)");
		cciToRmf.put("CCI-002502", "SC-31 (3)");
		cciToRmf.put("CCI-002503", "SC-31 (3)");
		cciToRmf.put("CCI-002504", "SC-32");
		cciToRmf.put("CCI-002505", "SC-32");
		cciToRmf.put("CCI-002506", "SC-32");
		cciToRmf.put("CCI-002507", "SC-34 (2)");
		cciToRmf.put("CCI-002508", "SC-34 (3) (a)");
		cciToRmf.put("CCI-002509", "SC-34 (3) (a)");
		cciToRmf.put("CCI-002510", "SC-34 (3) (b)");
		cciToRmf.put("CCI-002511", "SC-34 (3) (b)");
		cciToRmf.put("CCI-002512", "SC-34 (3) (b)");
		cciToRmf.put("CCI-002513", "SC-36");
		cciToRmf.put("CCI-002514", "SC-36");
		cciToRmf.put("CCI-002515", "SC-36");
		cciToRmf.put("CCI-002516", "SC-36");
		cciToRmf.put("CCI-002517", "SC-36 (1)");
		cciToRmf.put("CCI-002518", "SC-36 (1)");
		cciToRmf.put("CCI-002519", "SC-36 (1)");
		cciToRmf.put("CCI-002520", "SC-36 (1)");
		cciToRmf.put("CCI-002521", "SC-37");
		cciToRmf.put("CCI-002522", "SC-37");
		cciToRmf.put("CCI-002523", "SC-37, SC-37 (1)");
		cciToRmf.put("CCI-002524", "SC-37");
		cciToRmf.put("CCI-002525", "SC-37 (1)");
		cciToRmf.put("CCI-002526", "SC-37 (1)");
		cciToRmf.put("CCI-002527", "SC-37 (1)");
		cciToRmf.put("CCI-002528", "SC-38");
		cciToRmf.put("CCI-002529", "SC-38");
		cciToRmf.put("CCI-002530", "SC-39");
		cciToRmf.put("CCI-002531", "SC-39 (1)");
		cciToRmf.put("CCI-002532", "SC-39 (2)");
		cciToRmf.put("CCI-002533", "SC-39 (2)");
		cciToRmf.put("CCI-002383", "SC-4 (2)");
		cciToRmf.put("CCI-002384", "SC-4 (2)");
		cciToRmf.put("CCI-002534", "SC-40");
		cciToRmf.put("CCI-002535", "SC-40");
		cciToRmf.put("CCI-002536", "SC-40");
		cciToRmf.put("CCI-002537", "SC-40 (1)");
		cciToRmf.put("CCI-002538", "SC-40 (1)");
		cciToRmf.put("CCI-002539", "SC-40 (2)");
		cciToRmf.put("CCI-002540", "SC-40 (2)");
		cciToRmf.put("CCI-002541", "SC-40 (3)");
		cciToRmf.put("CCI-002542", "SC-40 (4)");
		cciToRmf.put("CCI-002543", "SC-40 (4)");
		cciToRmf.put("CCI-002544", "SC-41");
		cciToRmf.put("CCI-002545", "SC-41");
		cciToRmf.put("CCI-002546", "SC-41");
		cciToRmf.put("CCI-002547", "SC-42 a");
		cciToRmf.put("CCI-002548", "SC-42 a");
		cciToRmf.put("CCI-002549", "SC-42 b");
		cciToRmf.put("CCI-002550", "SC-42 b");
		cciToRmf.put("CCI-002551", "SC-42 (1)");
		cciToRmf.put("CCI-002552", "SC-42 (1)");
		cciToRmf.put("CCI-002553", "SC-42 (2)");
		cciToRmf.put("CCI-002554", "SC-42 (2)");
		cciToRmf.put("CCI-002555", "SC-42 (2)");
		cciToRmf.put("CCI-002556", "SC-42 (3)");
		cciToRmf.put("CCI-002557", "SC-42 (3)");
		cciToRmf.put("CCI-002558", "SC-42 (3)");
		cciToRmf.put("CCI-002559", "SC-43 a");
		cciToRmf.put("CCI-002560", "SC-43 a");
		cciToRmf.put("CCI-002561", "SC-43 b");
		cciToRmf.put("CCI-002562", "SC-43 b");
		cciToRmf.put("CCI-002563", "SC-43 b");
		cciToRmf.put("CCI-002564", "SC-44");
		cciToRmf.put("CCI-002565", "SC-44");
		cciToRmf.put("CCI-002385", "SC-5");
		cciToRmf.put("CCI-002386", "SC-5");
		cciToRmf.put("CCI-002387", "SC-5 (1)");
		cciToRmf.put("CCI-002388", "SC-5 (3) (a)");
		cciToRmf.put("CCI-002389", "SC-5 (3) (a)");
		cciToRmf.put("CCI-002390", "SC-5 (3) (b)");
		cciToRmf.put("CCI-002391", "SC-5 (3) (b)");
		cciToRmf.put("CCI-002392", "SC-6");
		cciToRmf.put("CCI-002393", "SC-6");
		cciToRmf.put("CCI-002394", "SC-6");
		cciToRmf.put("CCI-002395", "SC-7 b");
		cciToRmf.put("CCI-002396", "SC-7 (4) (c)");
		cciToRmf.put("CCI-002397", "SC-7 (7)");
		cciToRmf.put("CCI-002398", "SC-7 (9) (a)");
		cciToRmf.put("CCI-002399", "SC-7 (9) (a)");
		cciToRmf.put("CCI-002400", "SC-7 (9) (b)");
		cciToRmf.put("CCI-002401", "SC-7 (11)");
		cciToRmf.put("CCI-002402", "SC-7 (11)");
		cciToRmf.put("CCI-002403", "SC-7 (11)");
		cciToRmf.put("CCI-002404", "SC-7 (12)");
		cciToRmf.put("CCI-002405", "SC-7 (12)");
		cciToRmf.put("CCI-002406", "SC-7 (12)");
		cciToRmf.put("CCI-002407", "SC-7 (14)");
		cciToRmf.put("CCI-002408", "SC-7 (19)");
		cciToRmf.put("CCI-002409", "SC-7 (19)");
		cciToRmf.put("CCI-002410", "SC-7 (20)");
		cciToRmf.put("CCI-002411", "SC-7 (20)");
		cciToRmf.put("CCI-002412", "SC-7 (21)");
		cciToRmf.put("CCI-002413", "SC-7 (21)");
		cciToRmf.put("CCI-002414", "SC-7 (21)");
		cciToRmf.put("CCI-002415", "SC-7 (21)");
		cciToRmf.put("CCI-002416", "SC-7 (22)");
		cciToRmf.put("CCI-002417", "SC-7 (23)");
		cciToRmf.put("CCI-002418", "SC-8");
		cciToRmf.put("CCI-002419", "SC-8 (1)");
		cciToRmf.put("CCI-002420", "SC-8 (2)");
		cciToRmf.put("CCI-002421", "SC-8 (1)");
		cciToRmf.put("CCI-002422", "SC-8 (2)");
		cciToRmf.put("CCI-002423", "SC-8 (3)");
		cciToRmf.put("CCI-002424", "SC-8 (4)");
		cciToRmf.put("CCI-002425", "SC-8 (4)");
		cciToRmf.put("CCI-002427", "SC-8 (3)");
		cciToRmf.put("CCI-003544", "SE-1 a");
		cciToRmf.put("CCI-003545", "SE-1 a");
		cciToRmf.put("CCI-003546", "SE-1 a");
		cciToRmf.put("CCI-003547", "SE-1 a");
		cciToRmf.put("CCI-003548", "SE-1 a");
		cciToRmf.put("CCI-003549", "SE-1 a");
		cciToRmf.put("CCI-003550", "SE-1 a");
		cciToRmf.put("CCI-003551", "SE-1 b");
		cciToRmf.put("CCI-003552", "SE-1 b");
		cciToRmf.put("CCI-003553", "SE-2 a");
		cciToRmf.put("CCI-003554", "SE-2 a");
		cciToRmf.put("CCI-003555", "SE-2 b");
		cciToRmf.put("CCI-002601", "SI-1 a");
		cciToRmf.put("CCI-002744", "SI-10");
		cciToRmf.put("CCI-002745", "SI-10 (1) (a)");
		cciToRmf.put("CCI-002746", "SI-10 (1) (a)");
		cciToRmf.put("CCI-002747", "SI-10 (1) (b)");
		cciToRmf.put("CCI-002748", "SI-10 (1) (b)");
		cciToRmf.put("CCI-002749", "SI-10 (1) (c)");
		cciToRmf.put("CCI-002750", "SI-10 (2)");
		cciToRmf.put("CCI-002751", "SI-10 (2)");
		cciToRmf.put("CCI-002752", "SI-10 (2)");
		cciToRmf.put("CCI-002753", "SI-10 (2)");
		cciToRmf.put("CCI-002754", "SI-10 (3)");
		cciToRmf.put("CCI-002755", "SI-10 (4)");
		cciToRmf.put("CCI-002756", "SI-10 (5)");
		cciToRmf.put("CCI-002757", "SI-10 (5)");
		cciToRmf.put("CCI-002758", "SI-10 (5)");
		cciToRmf.put("CCI-002759", "SI-11 b");
		cciToRmf.put("CCI-002760", "SI-13 a");
		cciToRmf.put("CCI-002761", "SI-13 a");
		cciToRmf.put("CCI-002762", "SI-13 b");
		cciToRmf.put("CCI-002763", "SI-13 b");
		cciToRmf.put("CCI-002764", "SI-14");
		cciToRmf.put("CCI-002765", "SI-14");
		cciToRmf.put("CCI-002766", "SI-14");
		cciToRmf.put("CCI-002767", "SI-14");
		cciToRmf.put("CCI-002768", "SI-14 (1)");
		cciToRmf.put("CCI-002769", "SI-14 (1)");
		cciToRmf.put("CCI-002770", "SI-15");
		cciToRmf.put("CCI-002771", "SI-15");
		cciToRmf.put("CCI-002772", "SI-15");
		cciToRmf.put("CCI-002823", "SI-16");
		cciToRmf.put("CCI-002824", "SI-16");
		cciToRmf.put("CCI-002773", "SI-17");
		cciToRmf.put("CCI-002774", "SI-17");
		cciToRmf.put("CCI-002775", "SI-17");
		cciToRmf.put("CCI-002602", "SI-2 b");
		cciToRmf.put("CCI-002603", "SI-2 b");
		cciToRmf.put("CCI-002604", "SI-2 c");
		cciToRmf.put("CCI-002605", "SI-2 c");
		cciToRmf.put("CCI-002606", "SI-2 c");
		cciToRmf.put("CCI-002607", "SI-2 c");
		cciToRmf.put("CCI-002608", "SI-2 (3) (b)");
		cciToRmf.put("CCI-002609", "SI-2 (5)");
		cciToRmf.put("CCI-002610", "SI-2 (5)");
		cciToRmf.put("CCI-002611", "SI-2 (5)");
		cciToRmf.put("CCI-002612", "SI-2 (5)");
		cciToRmf.put("CCI-002613", "SI-2 (5)");
		cciToRmf.put("CCI-002614", "SI-2 (5)");
		cciToRmf.put("CCI-002615", "SI-2 (6)");
		cciToRmf.put("CCI-002616", "SI-2 (6)");
		cciToRmf.put("CCI-002617", "SI-2 (6)");
		cciToRmf.put("CCI-002618", "SI-2 (6)");
		cciToRmf.put("CCI-002619", "SI-3 a");
		cciToRmf.put("CCI-002620", "SI-3 a");
		cciToRmf.put("CCI-002621", "SI-3 a");
		cciToRmf.put("CCI-002622", "SI-3 a");
		cciToRmf.put("CCI-002623", "SI-3 c 1");
		cciToRmf.put("CCI-002624", "SI-3 c 1");
		cciToRmf.put("CCI-002625", "SI-3 (6) (b)");
		cciToRmf.put("CCI-002626", "SI-3 (6) (b)");
		cciToRmf.put("CCI-002627", "SI-3 (7)");
		cciToRmf.put("CCI-002628", "SI-3 (8)");
		cciToRmf.put("CCI-002629", "SI-3 (8)");
		cciToRmf.put("CCI-002630", "SI-3 (8)");
		cciToRmf.put("CCI-002631", "SI-3 (8)");
		cciToRmf.put("CCI-002632", "SI-3 (9)");
		cciToRmf.put("CCI-002633", "SI-3 (9)");
		cciToRmf.put("CCI-002634", "SI-3 (10) (a)");
		cciToRmf.put("CCI-002635", "SI-3 (10) (a)");
		cciToRmf.put("CCI-002636", "SI-3 (10) (a)");
		cciToRmf.put("CCI-002637", "SI-3 (9)");
		cciToRmf.put("CCI-002638", "SI-3 (10) (a)");
		cciToRmf.put("CCI-002639", "SI-3 (10) (b)");
		cciToRmf.put("CCI-002640", "SI-3 (10) (b)");
		cciToRmf.put("CCI-002641", "SI-4 a 1");
		cciToRmf.put("CCI-002642", "SI-4 a 2");
		cciToRmf.put("CCI-002643", "SI-4 a 2");
		cciToRmf.put("CCI-002644", "SI-4 a 2");
		cciToRmf.put("CCI-002645", "SI-4 b");
		cciToRmf.put("CCI-002646", "SI-4 b");
		cciToRmf.put("CCI-002647", "SI-4 d");
		cciToRmf.put("CCI-002648", "SI-4 d");
		cciToRmf.put("CCI-002649", "SI-4 d");
		cciToRmf.put("CCI-002650", "SI-4 g");
		cciToRmf.put("CCI-002651", "SI-4 g");
		cciToRmf.put("CCI-002652", "SI-4 g");
		cciToRmf.put("CCI-002653", "SI-4");
		cciToRmf.put("CCI-002654", "SI-4 g");
		cciToRmf.put("CCI-002655", "SI-4 (1)");
		cciToRmf.put("CCI-002656", "SI-4 (1)");
		cciToRmf.put("CCI-002657", "SI-4 (3)");
		cciToRmf.put("CCI-002658", "SI-4 (3)");
		cciToRmf.put("CCI-002659", "SI-4 (4)");
		cciToRmf.put("CCI-002660", "SI-4 (4)");
		cciToRmf.put("CCI-002661", "SI-4 (4)");
		cciToRmf.put("CCI-002662", "SI-4 (4)");
		cciToRmf.put("CCI-002663", "SI-4 (5)");
		cciToRmf.put("CCI-002664", "SI-4 (5)");
		cciToRmf.put("CCI-002665", "SI-4 (10)");
		cciToRmf.put("CCI-002666", "SI-4 (10)");
		cciToRmf.put("CCI-002667", "SI-4 (10)");
		cciToRmf.put("CCI-002668", "SI-4 (11)");
		cciToRmf.put("CCI-002669", "SI-4 (13) (c)");
		cciToRmf.put("CCI-002670", "SI-4 (18)");
		cciToRmf.put("CCI-002671", "SI-4 (18)");
		cciToRmf.put("CCI-002672", "SI-4 (18)");
		cciToRmf.put("CCI-002673", "SI-4 (19)");
		cciToRmf.put("CCI-002674", "SI-4 (19)");
		cciToRmf.put("CCI-002675", "SI-4 (19)");
		cciToRmf.put("CCI-002676", "SI-4 (20)");
		cciToRmf.put("CCI-002677", "SI-4 (20)");
		cciToRmf.put("CCI-002678", "SI-4 (21)");
		cciToRmf.put("CCI-002679", "SI-4 (21)");
		cciToRmf.put("CCI-002680", "SI-4 (21)");
		cciToRmf.put("CCI-002681", "SI-4 (22)");
		cciToRmf.put("CCI-002682", "SI-4 (22)");
		cciToRmf.put("CCI-002683", "SI-4 (22)");
		cciToRmf.put("CCI-002684", "SI-4 (22)");
		cciToRmf.put("CCI-002685", "SI-4 (23)");
		cciToRmf.put("CCI-002686", "SI-4 (23)");
		cciToRmf.put("CCI-002687", "SI-4 (23)");
		cciToRmf.put("CCI-002688", "SI-4 (24)");
		cciToRmf.put("CCI-002689", "SI-4 (24)");
		cciToRmf.put("CCI-002690", "SI-4 (24)");
		cciToRmf.put("CCI-002691", "SI-4 (24)");
		cciToRmf.put("CCI-002692", "SI-5 a");
		cciToRmf.put("CCI-002693", "SI-5 c");
		cciToRmf.put("CCI-002694", "SI-5 c");
		cciToRmf.put("CCI-002695", "SI-6 a");
		cciToRmf.put("CCI-002696", "SI-6 a");
		cciToRmf.put("CCI-002697", "SI-6 b");
		cciToRmf.put("CCI-002698", "SI-6 b");
		cciToRmf.put("CCI-002699", "SI-6 b");
		cciToRmf.put("CCI-002700", "SI-6 c");
		cciToRmf.put("CCI-002701", "SI-6 d");
		cciToRmf.put("CCI-002702", "SI-6 d");
		cciToRmf.put("CCI-002703", "SI-7");
		cciToRmf.put("CCI-002704", "SI-7");
		cciToRmf.put("CCI-002705", "SI-7 (1)");
		cciToRmf.put("CCI-002706", "SI-7 (1)");
		cciToRmf.put("CCI-002707", "SI-7 (1)");
		cciToRmf.put("CCI-002708", "SI-7 (1)");
		cciToRmf.put("CCI-002709", "SI-7 (1)");
		cciToRmf.put("CCI-002710", "SI-7 (1)");
		cciToRmf.put("CCI-002711", "SI-7 (1)");
		cciToRmf.put("CCI-002712", "SI-7 (1)");
		cciToRmf.put("CCI-002713", "SI-7 (2)");
		cciToRmf.put("CCI-002714", "SI-7 (5)");
		cciToRmf.put("CCI-002715", "SI-7 (5)");
		cciToRmf.put("CCI-002716", "SI-7 (6)");
		cciToRmf.put("CCI-002717", "SI-7 (6)");
		cciToRmf.put("CCI-002718", "SI-7 (6)");
		cciToRmf.put("CCI-002719", "SI-7 (7)");
		cciToRmf.put("CCI-002720", "SI-7 (7)");
		cciToRmf.put("CCI-002721", "SI-7 (8)");
		cciToRmf.put("CCI-002722", "SI-7 (8)");
		cciToRmf.put("CCI-002723", "SI-7 (8)");
		cciToRmf.put("CCI-002724", "SI-7 (8)");
		cciToRmf.put("CCI-002725", "SI-7 (9)");
		cciToRmf.put("CCI-002726", "SI-7 (9)");
		cciToRmf.put("CCI-002727", "SI-7 (10)");
		cciToRmf.put("CCI-002728", "SI-7 (10)");
		cciToRmf.put("CCI-002729", "SI-7 (10)");
		cciToRmf.put("CCI-002730", "SI-7 (11)");
		cciToRmf.put("CCI-002731", "SI-7 (11)");
		cciToRmf.put("CCI-002732", "SI-7 (12)");
		cciToRmf.put("CCI-002733", "SI-7 (12)");
		cciToRmf.put("CCI-002734", "SI-7 (13)");
		cciToRmf.put("CCI-002735", "SI-7 (13)");
		cciToRmf.put("CCI-002736", "SI-7 (13)");
		cciToRmf.put("CCI-002737", "SI-7 (14) (a)");
		cciToRmf.put("CCI-002738", "SI-7 (14) (b)");
		cciToRmf.put("CCI-002739", "SI-7 (15)");
		cciToRmf.put("CCI-002740", "SI-7 (15)");
		cciToRmf.put("CCI-002741", "SI-8 a");
		cciToRmf.put("CCI-002742", "SI-8 a");
		cciToRmf.put("CCI-002743", "SI-8 (3)");
		cciToRmf.put("CCI-003556", "TR-1 a");
		cciToRmf.put("CCI-003557", "TR-1 a");
		cciToRmf.put("CCI-003558", "TR-1 a");
		cciToRmf.put("CCI-003559", "TR-1 a");
		cciToRmf.put("CCI-003560", "TR-1 a");
		cciToRmf.put("CCI-003561", "TR-1 a");
		cciToRmf.put("CCI-003562", "TR-1 a");
		cciToRmf.put("CCI-003563", "TR-1 a");
		cciToRmf.put("CCI-003564", "TR-1 a");
		cciToRmf.put("CCI-003565", "TR-1 a");
		cciToRmf.put("CCI-003566", "TR-1 a");
		cciToRmf.put("CCI-003567", "TR-1 a");
		cciToRmf.put("CCI-003568", "TR-1 b");
		cciToRmf.put("CCI-003569", "TR-1 b");
		cciToRmf.put("CCI-003570", "TR-1 b");
		cciToRmf.put("CCI-003571", "TR-1 b");
		cciToRmf.put("CCI-003572", "TR-1 b");
		cciToRmf.put("CCI-003573", "TR-1 b");
		cciToRmf.put("CCI-003574", "TR-1 b");
		cciToRmf.put("CCI-003575", "TR-1 b");
		cciToRmf.put("CCI-003576", "TR-1 b");
		cciToRmf.put("CCI-003577", "TR-1 b");
		cciToRmf.put("CCI-003578", "TR-1 c");
		cciToRmf.put("CCI-003579", "TR-1 c");
		cciToRmf.put("CCI-003580", "TR-1 (1)");
		cciToRmf.put("CCI-003581", "TR-2 a");
		cciToRmf.put("CCI-003582", "TR-2 b");
		cciToRmf.put("CCI-003583", "TR-2 c");
		cciToRmf.put("CCI-003584", "TR-2 (1)");
		cciToRmf.put("CCI-003585", "TR-3 a");
		cciToRmf.put("CCI-003586", "TR-3 a");
		cciToRmf.put("CCI-003587", "TR-3 b");
		cciToRmf.put("CCI-003588", "UL-1");
		cciToRmf.put("CCI-003589", "UL-2 a");
		cciToRmf.put("CCI-003590", "UL-2 b");
		cciToRmf.put("CCI-003591", "UL-2 b");
		cciToRmf.put("CCI-003592", "UL-2 c");
		cciToRmf.put("CCI-003593", "UL-2 c");
		cciToRmf.put("CCI-003594", "UL-2 c");
		cciToRmf.put("CCI-003595", "UL-2 c");
		cciToRmf.put("CCI-003596", "UL-2 d");
		cciToRmf.put("CCI-003597", "UL-2 d");
	}//private void fillColumns() {


}//end class body
