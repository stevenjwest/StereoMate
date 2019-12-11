package stereomate.image;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

//import Threshold_Manager.ActiveChannelThread;
//import Threshold_Manager.ImageProcessingProcedureStack;
//import Threshold_Manager.ImageProcessingProcedureThread;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import stereomate.plugins.StereoMateSettings;


/**
 * This class represents the image processing procedure stack - all of the image processing procedures 
 * which are to be applied to an image in turn.  The stack has an order to it, and in this class are
 * variables to represent the algorithm command, and its options, in the correct order.
 * <p>
 * There are method to run the methods via the IJ built-in command IJ.run(imp,cmd,options).  This runs
 * in the CURRENT THREAD (typically the EDT), which will block user input while it runs.  Use the command
 * IJ.doCommand(imp,cmd,options) to run in a SEPARATE THREAD.
 * <p>
 * This class can also write & read the processing stack to an XML file to be saved for future recall.  This
 * is useful to allow the procedure stack to be recalled in the SM_Analyser algorithm, or previous procedure
 * stacks to be recalled into the Threshold_Manager for editing.
 * 
 * @author stevenwest
 *
 */
public class ImageProcessingProcedureStack3 {
	
	/**
	 * This indicates the bitDepth which the images are processed as - used for recall of the procedureStack to
	 * ensure correct bitDepth of the images are set!
	 */
	public int bitDepth;
	
	/**
	 * This indicates which channel is the ACTIVE CHANNEL -> this is the channel which is being processed by this
	 * ImageProcessingProcedureStack.
	 */
	public int activeChannel;
	
	/**
	 * These ArrayLists hold the cmd title, options and toggle setting of the commands in the procedure stack.  They must
	 * ALWAYS be edited together, which occurs in this class.
	 */
	
	protected ArrayList<String> commandTitles;
	
	protected ArrayList<String> commandOptions;
	
	protected ArrayList<String> toggleProperties;
	
	
	/**
	 * This represents a JTextArea where text can be put to update the status of the procedure stack's run.
	 */
	protected JTextArea status;
	
	
	/**
	 * This will construct a new ImageProcessingProcedureStack object.  
	 * <p>
	 * As each Command is run, the JTextArea 'status' is updated with this information, to let the user know image processing
	 * is taking place.  During this time, the Threshold_Manager buttons on its active panel will be inactivated.
	 * The commands will be run in a separate thread, to free EDT thread to draw on the status JTextArea.
	 */
	public ImageProcessingProcedureStack3(int bitDepth, int activeChannel) {
		
		this(bitDepth, activeChannel, null);
	
	}
	
	
	/**
	 * This will construct a new ImageProcessingProcedureStack object.  
	 * <p>
	 * As each Command is run, the JTextArea 'status' is updated with this information, to let the user know image processing
	 * is taking place.  During this time, the Threshold_Manager buttons on its active panel will be inactivated.
	 * The commands will be run in a separate thread, to free EDT thread to draw on the status JTextArea.
	 */
	public ImageProcessingProcedureStack3(int bitDepth, int activeChannel, JTextArea status) {
		
		this.bitDepth = bitDepth;
		
		this.activeChannel = activeChannel;
		
		commandTitles = new ArrayList<String>();
		
		commandOptions = new ArrayList<String>();
		
		toggleProperties = new ArrayList<String>();
		
		this.status = status;
	
	}
	
	
	public int getActiveChannel() {
		
		return activeChannel;
		
	}
	
	
	public void setActiveChannel(int c) {
		activeChannel = c;
	}
	
	
	
	/**
	 * Adds the command title and options to the corresponding arrayLists, and sets the toggle property
	 * to "0" (off).
	 * <p>  
	 * This method also handles "Custom" commands - it extracts the command title and command options from the 
	 * commandOptions String, and adds these to the command title and options.
	 * @param commandTitle
	 * @param commandOptions
	 */
	public void addCommand(String commandTitle, String commandOptions) {
		
		if(commandTitle.equals("Custom")) {
			//extract the command title (String from end of 'command=' to the ' ' after)
			//IJ.showMessage("start title ind: "+commandOptions.indexOf("command=")+8);
			//IJ.showMessage("end title ind: "+);
			commandTitle = commandOptions.substring(commandOptions.indexOf("command=")+8, commandOptions.indexOf(" "));
			
			//extract the command option (String from end of 'options=' to the end of String:
			commandOptions = commandOptions.substring(commandOptions.indexOf("options=")+8);
			
			IJ.showMessage("Custom title: "+commandTitle);
			IJ.showMessage("Custom options: "+commandOptions);

		}
		
		
		//add the cmd title and options to ArrayLists:
		commandTitles.add(commandTitle);
		this.commandOptions.add(commandOptions);
		
		//set the toggleProperty of this item in the toggleProperty ArrayList:
		toggleProperties.add("0");
		
		//and update the list:
		//updateList(); -> no longer performed in this class, now done in Threshold_Manager!
		
	}
	
	
	/**
	 * Add command title, options and the toggle value.  Toggle value MUST be either "1" or
	 * "0", if it is neither of these, the toggle value is set to "0"
	 * @param commandTitle
	 * @param commandOptions
	 * @param toggle
	 */
	public void addCommand(String commandTitle, String commandOptions, String toggle) {
		
		//add the cmd title and options to ArrayLists:
		commandTitles.add(commandTitle);
		this.commandOptions.add(commandOptions);
		
		//set the toggleProperty of this item in the toggleProperty ArrayList:
		if(toggle.equals("1"))
			toggleProperties.add(toggle);
		else {
			toggleProperties.add("0");
		}
		
		//and update the list:
			//Do not update the list in this method - called when loading a new ProcedureStack!
		//updateList();
			//Instead call clearAndFillList() at end of the loadStack method..
		
	}
	
	/**
	 * Sets the Text Area variable - status - to the passed Text Area.
	 * @param ta
	 */
	public void setTextArea(JTextArea ta) {
		status = ta;
	}
	
	/**
	 * Returns the TextArea - status - variable, or null if not set.
	 * @return
	 */
	public JTextArea getTextArea() {
		return status;
	}
	
	
	
	public void remove(int index) {
		commandTitles.remove(index);
		commandOptions.remove(index);
		toggleProperties.remove(index);
	}
	
	
	/**
	 * Returns options string at index.
	 * @param index
	 * @return
	 */
	public String getOptions(int index) {
		return commandOptions.get(index);
	}
	
	
	/**
	 * Returns command title string at index.
	 * @param index
	 * @return
	 */
	public String getCommand(int index) {
		return commandTitles.get(index);
	}
	
	
	/**
	 * Returns state of toggle as boolean at index.
	 * @param index
	 * @return
	 */
	public boolean getToggle(int index) {
		if(toggleProperties.get(index).equals("1")) {
			return true;
		}
		else {
			return false;
		}
	}
	
	

	
	
	/**
	 * This implements the behaviour of the upItem button.  It will move the index cmd up in the procedure stack
	 * list.
	 * @param index
	 */
	public void moveUp(int index) {
		
		//move the index cmd up in all the ArrayLists:
		
		//only if index is greater than 0!
		
		if(index > 0) {

		//Command Titles:
			//first, get the element at the previous index (1 UP in the list):
			String prevElement = commandTitles.get(index-1);
			//then set the item at index to index-1:
			commandTitles.set(index-1, commandTitles.get(index) );
			//finally set the prevElement item (which was at index-1) to index:
			commandTitles.set(index, prevElement);
			
			//adjust the listModel:
			//listModel.set(index-1, commandTitles.get(index-1) );
			//listModel.set(index, commandTitles.get(index) );
				//DO NOT REF LISTMODEL IN THIS CLASS!  Now performed locally in appropriate listener
					//in ThresholdManager class..
			
		//Command Options:
			//first, get the element at the previous index (1 UP in the list):
			 prevElement = commandOptions.get(index-1);
			//then set the item at index to index-1:
			 commandOptions.set(index-1, commandOptions.get(index) );
			//finally set the prevElement item (which was at index-1) to index:
			 commandOptions.set(index, prevElement);
		
		//Toggle Properties:
			//first, get the element at the previous index (1 UP in the list):
			 prevElement = toggleProperties.get(index-1);
			//then set the item at index to index-1:
			 toggleProperties.set(index-1, toggleProperties.get(index) );
			//finally set the prevElement item (which was at index-1) to index:
			 toggleProperties.set(index, prevElement);
			 
		//And to be sure the list is displaying the correct info, clear and refill it:
			 //make sure to re-select the original indexed item, which is now at index-1 now:
			 //clearAndFillList(index-1);				 
			//list.setSelectedIndex( index-1 );
			 	//DO NOT REF LISTMODEL IN THIS CLASS!  Now performed locally in appropriate listener
			 		//in ThresholdManager class..
		
		}
		
	}
	
	/**
	 * This implements the behaviour of the upItem button.  It will move the index cmd up in the procedure stack
	 * list.
	 * @param index
	 */
	public void moveDown(int index) {
		
		//move the index cmd down in all the ArrayLists:
		
		//only if index is less than list max index -1!
		
		if(index < (commandTitles.size()-1) ) {

		//Command Titles:
			//first, get the element at the next index (1 DOWN in the list):
			String nextElement = commandTitles.get(index+1);
			//then set the item at index to index+1:
			commandTitles.set(index+1, commandTitles.get(index) );
			//finally set the nextElement item (which was at index+1) to index:
			commandTitles.set(index, nextElement);
			
			//adjust the listModel:
			//listModel.set(index+1, commandTitles.get(index+1) );
			//listModel.set(index, commandTitles.get(index) );
				//DO NOT REF LISTMODEL IN THIS CLASS!  Now performed locally in appropriate listener
					//in ThresholdManager class..
			
		//Command Options:
			//first, get the element at the next index (1 DOWN in the list):
			nextElement = commandOptions.get(index+1);
			//then set the item at index to index+1:
			 commandOptions.set(index+1, commandOptions.get(index) );
			//finally set the nextElement item (which was at index+1) to index:
			 commandOptions.set(index, nextElement);
		
		//Toggle Properties:
			//first, get the element at the next index (1 DOWN in the list):
			 nextElement = toggleProperties.get(index+1);
			//then set the item at index to index+1:
			 toggleProperties.set(index+1, toggleProperties.get(index) );
			//finally set the nextElement item (which was at index+1) to index:
			 toggleProperties.set(index, nextElement);
			 
		//And to be sure the list is displaying the correct info, clear and refill it:
			 //make sure to re-select the original indexed item, which is now at index-1 now:
			 //clearAndFillList(index+1);
			 //list.setSelectedIndex(index+1);
			 	//DO NOT REF LISTMODEL IN THIS CLASS!  Now performed locally in appropriate listener
					//in ThresholdManager class..
			 
		}
		
	}
	
	
	
	
	/**
	 * This method turns the toggleItem button to selected & updates its icon, and sets the toggleProperties
	 * item at correct index to the correct string.  It also updates the activeImp:  any of the items in the
	 * procedure stack where the toggleProperties is "1", run the command on the activeImp (this is all
	 * performed in the runProcedureStack(imp) method!).
	 */
	public void toggleOn(int index) {
		
		//first, get the toggleProperties at index:
		String toggle = toggleProperties.get(index);
		
		//if this is off, then adjust the toggleProperties and updateToggle, and runProcedureStack:
		if(toggle.equals("0")) {
			toggleProperties.set(index, "1");
			
			//ALL DONE IN THRESHOLD_MANAGER CLASS:
			//updateToggle(index);
			//run procedure stack IN SEPARATE THREAD:
			//ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread(mainPanel);
			//ippt.start();
			//runProcedureStack(activeImp);
		}
		
		//if toggle is ON - do nothing, it is already on!
		
	}
	
	
	/**
	 * This method turns the toggleItem button to deselected & updates its icon, and sets the toggleProperties
	 * item at correct index to the correct string.  It also updates the activeImp:  any of the items in the
	 * procedure stack where the toggleProperties is "1", run the command on the activeImp (this is all
	 * performed in the runProcedureStack(imp) method!).
	 */
	public void toggleOff(int index) {
		
		//first, get the toggleProperties at index:
		String toggle = toggleProperties.get(index);
		
		//if this is on, then adjust the toggleProperties and updateToggle, and runProcedureStack:
		if(toggle.equals("1")) {
			toggleProperties.set(index, "0");
			
			//ALL DONE IN THRESHOLD MANAGER CLASS:
			//updateToggle(index);
			//run procedure stack IN SEPARATE THREAD:
			//ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread(mainPanel, true);
			//ippt.start();
			//runProcedureStack(activeImp);
		}
		
		//if toggle is OFF - do nothing, it is already off!
		
	}
	
	
	public boolean isAnyToggleOn() {
		
		boolean togOn = false;
		
		for(int a=0; a<toggleProperties.size();a++) {
			if(toggleProperties.get(a).equals("1") ) {
				togOn = true;
			}
		}
		
		return togOn;
		
	}

	
	/**
	 * Runs the current image processing procedure stack on the passed imp.
	 * @param imp The ActiveImp from the IWP which is being processed.
	 * @param IWP The ImageWindowWithPanel object which contains the original imageStack -> required to update
	 * the IWP activeImp and its whole stack to ensure proper projection in IWP (and so not to lose the reference in case
	 * the IJ.run() method replaces the imp with a new reference!).
	 */
	public boolean runProcedureStack(ImagePlus imp, ImageWindowWithPanel IWP) {
		boolean cmdRan = false;
					
		for(int a=0; a<commandTitles.size(); a++) {
			if(toggleProperties.get(a).equals("1") ) {
				
				//only run the procedure IF the toggle property is 1 for this item in the stack:
				if(status != null) {
					status.setText("Running: "+commandTitles.get(a) );
				}
				
				IJ.run( imp, commandTitles.get(a), commandOptions.get(a) );
				
				//The IWP Ref to the third channel may now be in-correct (if a new imagestack is put into activeImp
					//during the running of this command - will lose the ip refs in IWP to the activeImp channel!)
				//Therefore, update IWP last channel with activeImp and its new stack ref!!
				
				//SO - **REPLACE** the current extra channel in IWP with this new extracted channel:
				
				IWP.getOriginalImagePlus().setStack( IWP.replaceStacks( IWP.getOriginalImagePlus().getStack(), 
																		imp.getStack() ), 
																	IWP.getOriginalImagePlus().getNChannels(), 
																	IWP.getOriginalImagePlus().getNSlices(), 
																	IWP.getOriginalImagePlus().getNFrames() );
				
				//if command is run, set cmdRan to true:
				cmdRan = true;
			}
		}
		
		return cmdRan;
		
	}
	
	
	/**
	 * Runs the current image processing procedure stack on the passed imp.  No IWP is required, the procedure stack
	 * is just run directly on the passed ImagePlus.
	 * @param imp The ActiveImp from the IWP which is being processed.
	 */
	public void runProcedureStack(ImagePlus imp) {
					
		for(int a=0; a<commandTitles.size(); a++) {
			if(toggleProperties.get(a).equals("1") ) {
				
				//only run the procedure IF the toggle property is 1 for this item in the stack:
				if(status != null) {
					status.setText("Running: "+commandTitles.get(a) );
				}
				
				IJ.run( imp, commandTitles.get(a), commandOptions.get(a) );
			}
		}
		
		
	}
	
	
	
	/**
	 * Runs a given command with given options on the passed imp.  This method takes care of previews on IWP
		 * in Threshold Manager Plugin.  Processing is run on the substack object imp form IWP.
	 * @param imp  Reference to image stack that is to be processed.
	 * @param cmd  The Command Title to be run.
	 * @param options  The command options to b run.
	 * @param IWP The ImageWindowWithPanel object that the imp is from.
	 */
	public void runProcedure(ImagePlus imp, String cmd, String options, ImageWindowWithPanel IWP) {
		
		//set status, if not null:
		if(status != null) {
			status.setText("Running: "+cmd );
		}
		
		//run the method:
		IJ.run(imp, cmd, options);
				
		//The IWP Ref to the third channel may now be in-correct (if a new imagestack is put into activeImp
			//during the running of this command - will lose the ip refs in IWP to the activeImp channel!)
		//Therefore, update IWP last channel with activeImp and its new stack ref!!
		
		//SO - **REPLACE** the current extra channel in IWP with this new extracted channel:
		
		IWP.getOriginalImagePlus().setStack( IWP.replaceStacks( IWP.getOriginalImagePlus().getStack(), 
																imp.getStack() ), 
															IWP.getOriginalImagePlus().getNChannels(), 
															IWP.getOriginalImagePlus().getNSlices(), 
															IWP.getOriginalImagePlus().getNFrames() );
		
		
	}
	
	
	/**
	 * Returns the default name for this procedureStack - combination of the bitDepth and a sequence of the 
	 * procedureStacks' steps.
	 * @return String of default name of this procedure stack - bit depths + each processes name.
	 */
	public String getDefaultName() {
		String name = "";
		
		//Add bit depth to String with space:
		name = name + bitDepth + "bit ";
		
		//next loop through all procedure titles and add these, with a space between each:
		for(int a=0; a<commandTitles.size(); a++) {
			name = name + commandTitles.get(a) + " ";
		}
		
		//Finally, remove the last space:
		name = name.substring(0, name.length()-1);
		
		//and return name:
		return name;
	}
	
	
	/**
	 * Saves the IPPS to an XML file to default location, .thresholdProcedures in the StereoMate Directory, for later recall.
	 */
	public void saveStack(String procedureTitle) {
		
		//Retrieve file representation of JAR file - to retrieve its absolute path:
		File file = null;	
		try {
			file = new File(StereoMateSettings.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}
		//Retrieve parent, and then formulate new file object pointing to the .thresholdProcedures DIR & the procedureTitle file:
		file = file.getParentFile();
		// File thresholdProceduresFile = new File(file.getAbsolutePath() + File.separator + ".thresholdProcedures" + File.separator + procedureTitle+".xml");
		File thresholdProceduresFile = new File(file.getAbsolutePath() + File.separator + ".thresholdProcedures");
		
		saveStack(procedureTitle, thresholdProceduresFile);
	}
	
	
	/**
	 * Saves the IPPS to a specified XML file, for later recall.  The procedureTitle should specify the XML file title (with NO .xml
	 * extension), and the procedureStackDestination should be a destination directory on the File System.
	 */
	public void saveStack(String procedureTitle, File procedureStackDestination) {
		
		//Retrieve file representation of JAR file - to retrieve its absolute path:
		//File file = null;	
		//try {
		//	file = new File(StereoMateSettings.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
		//} catch (URISyntaxException e1) {
		//	e1.printStackTrace();
		//}
		//Retrieve parent, and then formulate new file object pointing to the .thresholdProcedures DIR & the procedureTitle file:
		//file = file.getParentFile();
		//File thresholdProceduresFile = new File(file.getAbsolutePath() + File.separator + ".thresholdProcedures" + File.separator + procedureTitle+".xml");
		File thresholdProceduresFile = new File(procedureStackDestination.getAbsolutePath() + File.separator + procedureTitle+".xml");
		
		//want to save the procedureStack XML file to the thresholdProceduresFile:
			//write the contents of procedureStack to a new XML Document:
		
		try {

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// root element - procedure Stack:
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("procedureStack");
			doc.appendChild(rootElement);

			// ADD bitDepth element
			Element bitDepth = doc.createElement("bitDepth");
			bitDepth.appendChild(doc.createTextNode(""+this.bitDepth));
			rootElement.appendChild(bitDepth);
			
			// ADD activeChannel element
			Element activeChannel = doc.createElement("activeChannel");
			activeChannel.appendChild(doc.createTextNode(""+this.activeChannel));
			rootElement.appendChild(activeChannel);

			// set attribute to staff element
			//Attr attr = doc.createAttribute("id");
			//attr.setValue("1");
			//staff.setAttributeNode(attr);

			// shorten way
			// staff.setAttribute("id", "1");
			
			// ADD procedureDepth element
			Element procedureDepth = doc.createElement("procedureDepth");
			procedureDepth.appendChild( doc.createTextNode( ""+this.commandTitles.size() ) );
			rootElement.appendChild(procedureDepth);
			
			// ADD procedures element:
			Element procedures = doc.createElement("procedures");
			rootElement.appendChild(procedures);
			
			//ADD procedure elements to procedures element:
			for(int a=0; a<commandTitles.size(); a++) {
				//Add title:
				Element cmdTitle = doc.createElement("title"+a);
				cmdTitle.appendChild( doc.createTextNode( commandTitles.get(a) ) );
				procedures.appendChild(cmdTitle);
				//Add Options:
				Element cmdOptions = doc.createElement("options"+a);
				cmdOptions.appendChild( doc.createTextNode( commandOptions.get(a) ) );
				procedures.appendChild(cmdOptions);
				//Add toggleProperties:
				Element cmdTog = doc.createElement("toggle"+a);
				cmdTog.appendChild( doc.createTextNode( toggleProperties.get(a) ) );
				procedures.appendChild(cmdTog);
			}


			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult( thresholdProceduresFile );

			//ensure XML file is formatted for human reading:
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			// Output to console for testing
			// StreamResult result = new StreamResult(System.out);

			transformer.transform(source, result);

		  } catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		  } catch (TransformerException tfe) {
			tfe.printStackTrace();
		  }
		
		
		
	} //end saveStack(String)
	
	
	
	public Object[] loadStackTitles() {
		Object[] ob = new Object[]{ };
					
		//get the ".thresholdProcedures" DIR and collect all file names (minus the .xml extensions):
		
		//Retrieve file representation of JAR file - to retrieve its absolute path:
		File file = null;	
		try {
			file = new File(StereoMateSettings.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}
		//Retrieve parent, and then formulate new file object pointing to the .thresholdProcedures DIR:
		file = file.getParentFile();
		File thresholdProceduresFile = new File(file.getAbsolutePath() + File.separator + ".thresholdProcedures");
		
		//Get all child files to this DIR:
		ob = thresholdProceduresFile.list();
		
		return ob;
		
	}

	
	/**
	 * Load the Procedure Stack based on the stack Title - the file name of the stack.  The stack should be present in
	 * the ".thresholdProcedures" directory in the StereoMateSettings.
	 */
	public static ImageProcessingProcedureStack3 loadStack(String stackTitle) {
	
		ImageProcessingProcedureStack3 IPPS = null;
		
		//Retrieve file representation of JAR file - to retrieve its absolute path:
		File file = null;	
		try {
			file = new File(StereoMateSettings.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}
		//Retrieve parent, and then formulate new file object pointing to the .thresholdProcedures DIR:
		file = file.getParentFile();
		File thresholdProceduresFile = new File(file.getAbsolutePath() + File.separator + ".thresholdProcedures" + File.separator + stackTitle);
		
		
		//load the XML document:
		
		//Here, the InputStream is used inside appropriate try... catch statements:
		InputStream in = null;
		
		try {
			in = new FileInputStream(thresholdProceduresFile);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		
		//Once an InputStream is established, next build the DOM Document:
		
		//generate Document Builder:
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = null;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			//close InputStream
			try {
				in.close();
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
				
		Document doc = null;
		try {
			doc = dBuilder.parse(in);
		} catch (SAXException | IOException e) {
			e.printStackTrace();
			//close InputStream
			try {
				in.close();
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//normalize the document:
			//ensures smooth reading of nodes
		doc.getDocumentElement().normalize();
		
		 //read procedureStack attributes from doc into a new procedureStack object:
					
		
		//read the bitDepth node:
		
		int bitDepth = Integer.parseInt( doc.getElementsByTagName("bitDepth").item(0).getTextContent() );
		
		int activeChannel = Integer.parseInt( doc.getElementsByTagName("activeChannel").item(0).getTextContent() );
		
			//and create new IPPS object with correct bitDepth:
		
		IPPS = new ImageProcessingProcedureStack3(bitDepth, activeChannel);
		
		//read the number of procedures:
		
		int procedureNum = Integer.parseInt( doc.getElementsByTagName("procedureDepth").item(0).getTextContent() );
		
		//loop through all procedures and retrieve the Strings for title, options and the toggle informaiton:
		
		for(int a=0; a<procedureNum; a++) {
			String title = doc.getElementsByTagName("title"+a).item(0).getTextContent() ;
			String options = doc.getElementsByTagName("options"+a).item(0).getTextContent() ;
			String toggle = doc.getElementsByTagName("toggle"+a).item(0).getTextContent() ;
			IPPS.addCommand(title, options, toggle);
			//add to the list as the elements are loaded:
		}
		
		//IPPS.clearAndFillList();
			//DO NOT CALL THIS METHOD HERE -> call it in ThresholdManager class -> no refs to
				//thresholdManager variables in this class to make it work with other plugins!
			//This method is now in ThresholdManager, and called appropriately in the loadButton 
				//actionListener.
		
		return IPPS;
		
	}
	
	
	/**
	 * Load a threshold Procedure Stack from a parsed file.
	 * @param thresholdProceduresFile
	 * @return
	 */
	public static ImageProcessingProcedureStack3 loadStack(File thresholdProceduresFile) {

		ImageProcessingProcedureStack3 IPPS = null;

		//load the XML document:

		//Here, the InputStream is used inside appropriate try... catch statements:
		InputStream in = null;

		try {
			in = new FileInputStream(thresholdProceduresFile);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		//Once an InputStream is established, next build the DOM Document:

		//generate Document Builder:
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = null;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			//close InputStream
			try {
				in.close();
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		Document doc = null;
		try {
			doc = dBuilder.parse(in);
		} catch (SAXException | IOException e) {
			e.printStackTrace();
			//close InputStream
			try {
				in.close();
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		//normalize the document:
		//ensures smooth reading of nodes
		doc.getDocumentElement().normalize();

		//read procedureStack attributes from doc into a new procedureStack object:


		//read the bitDepth node:

		int bitDepth = Integer.parseInt( doc.getElementsByTagName("bitDepth").item(0).getTextContent() );

		int activeChannel = Integer.parseInt( doc.getElementsByTagName("activeChannel").item(0).getTextContent() );

		//and create new IPPS object with correct bitDepth:

		IPPS = new ImageProcessingProcedureStack3(bitDepth, activeChannel);

		//read the number of procedures:

		int procedureNum = Integer.parseInt( doc.getElementsByTagName("procedureDepth").item(0).getTextContent() );

		//loop through all procedures and retrieve the Strings for title, options and the toggle informaiton:

		for(int a=0; a<procedureNum; a++) {
			String title = doc.getElementsByTagName("title"+a).item(0).getTextContent() ;
			String options = doc.getElementsByTagName("options"+a).item(0).getTextContent() ;
			String toggle = doc.getElementsByTagName("toggle"+a).item(0).getTextContent() ;
			IPPS.addCommand(title, options, toggle);
			//IJ.showMessage("procedure index: "+a+" title: "+title);
			//add to the list as the elements are loaded:
		}

		//IPPS.clearAndFillList();
		//DO NOT CALL THIS METHOD HERE -> call it in ThresholdManager class -> no refs to
		//thresholdManager variables in this class to make it work with other plugins!
		//This method is now in ThresholdManager, and called appropriately in the loadButton 
		//actionListener.

		return IPPS;

	}
	
	
	
	/**
	 *  Threshold the imp according to the procedureStack object:
	 * This should also perform the inital object Assessment -> i.e. assess each object and output data to a table.
	 * There is no need to label the objects in the thresholded imp, as the INITIAL PIXEL can be used
	 * to refer to any object in the image...  
	 * <p>
	 * Use MCIB3D & modified code here to extract information from each object.  The data should be output in format
	 * to an ARFF file.
	 * 
	 */
	public ImagePlus applyThresholdProcedureStack(ImagePlus imp, ImageProcessingProcedureStack3 procedureStack) {
		
		//FIRST STEP -> Threshold the passed imp with the thresholding procedure
		
			//This MUST GENERATE & RETURN A NEW IMP, and leave the original imp intact.

		//This should also perform the inital object Assessment -> i.e. assess each object and output data to a table.
			// There is no need to label the objects in the thresholded imp, as the INITIAL PIXEL can be used
			//to refer to any object in the image...
		//Therefore DO NOT LABEL objects in the imp - DO NOT OUTPUT 16-bit image, but just READ the attributes of every
			//object in the thresholded image and output the data to a table.
		
		// REMEMBER to adjust the BIT DEPTH of the image when performing the threshold procedure stack!!

		
		//use variables from procedureStack to get the correct channel, convert to correct bit depth, and to
			//apply the full procedureStack to get a thresholded image.
		
		//FIRST -> Duplicate the correct channel from the imp:
		
		ImageStack is = ImageWindowWithPanel.filterImageStack( imp.getStack(), procedureStack.activeChannel, imp.getNChannels() );
		
		ImagePlus thresholdedImp = new ImagePlus(imp.getTitle(), is);
		
		//ALSO set the active imp -> this references the image stack of the activeChannel in original imp,
			//which is used to build the ImageInt for intensity analysis on the original imp active channel:
		
		// activeChannel = new ImagePlus(imp.getTitle(), ImageWindowWithPanel.filterImageStackNoDup( imp.getStack(), procedureStack.activeChannel, imp.getNChannels() ) );
		
			// This is not required here - as activeChannel is not present in this class...
		
		//SECOND -> Convert to correct bit depth, if necessary:
		
		ImageWindowWithPanel.convertBitDepth(thresholdedImp, procedureStack.bitDepth);
		
		//THIRD -> Apply the ProcedureStack to the thresholdedImp:
		
		procedureStack.runProcedureStack(thresholdedImp);
		
		//Return the imp - it is now of the correct channel, correct bit depth, and thresholded according to
			//the procedure stack:
		
		return thresholdedImp;
		
	}
	
	
}
