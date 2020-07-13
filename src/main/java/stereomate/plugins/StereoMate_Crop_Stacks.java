package stereomate.plugins;

import java.awt.Dimension;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.PlainDocument;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import stereomate.dialog.DialogWindow;
import stereomate.settings.MyIntFilter;

public class StereoMate_Crop_Stacks  implements PlugIn, StereoMateAlgorithm {

	/**
	 * dialogWindow object - to create a dialog to select files for processing.
	 */
	DialogWindow dw;
	
	JTextField zSlicesTextField;
	
	JCheckBox overwriteCheckBox;
	
	int zDepth;
	
	boolean overwrite;	
	
	Document docSMS;
	
	String logImageProcessing;
	
	File cropLogFile;
	
	FileWriter fw;
	PrintWriter pw;
	
	
	/**
	 * Sets up the Dialog Window for Crop Stacks PlugIn.
	 */
	@Override
	public void run(String arg) {
		
		dw = new DialogWindow("Crop Stacks", this);
		
		dw.addFileSelector("DIR to Crop Stacks:"); //add FileSelector panel.
		//dw.addFileSelector("Image02:", dw.FILES_AND_DIRS, dw.MATCHED_INPUT);
		
		dw.add( addzDepthSelector() );
		
		dw.addActionPanel(); //add Action panel - for Cancel and Process buttons.
		dw.setPlugInSuffix("_CROP");  //This is probably not required.
				
		dw.layoutAndDisplayFrame(); //display the DialogWindow.
		
	}
	
	
	
	/**
	 * Returns a JPanel containing a selector for the bit depth of the image.  This establishes
	 * what bit depth images will be converted to (if required) as the image is opened into a ImageWindowWithPanel
	 * object.
	 * @return
	 */
	public JPanel addzDepthSelector() {
		
		
			//A panel to store the Z Depth Selections and overwrite:
			JPanel p = new JPanel();
				
			//panel to put z slices on
			JPanel zPanel = new JPanel();
				
			//create JLabel and add to panel:
			JLabel zLabel = new JLabel("Z Depth:");
					
			zSlicesTextField = new JTextField(3);
			zSlicesTextField.setText("10");
			
			zSlicesTextField.setToolTipText("Set number of EXPECTED z slices for image stacks.  Stacks will be cropped to remove excess slices from top and bottom.");
				
			//Using a Document Filter to force TextField to contain only Ints
			//This also prevents pasting of non-int characters.
			//I have modified the MyIntFilter class to allow Blank Strings
			//to be inserted..
			PlainDocument doc = (PlainDocument) zSlicesTextField.getDocument();
		    doc.setDocumentFilter(new MyIntFilter());
		     
		    // overwrite checkbox:
			//JLabel overwriteLabel = new JLabel("Overwrite:");	
			overwriteCheckBox = new JCheckBox("Overwrite");
			
			overwriteCheckBox.setToolTipText("Check this to overwrite the input image files with the cropped stacks.  This cannot be undone");
			
			zPanel.add(zLabel);
		    zPanel.add(zSlicesTextField);
		    //zPanel.add(overwriteLabel);
		    zPanel.add(overwriteCheckBox);
		     
			//set layout to BoxLayout:
			p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
			p.add(Box.createRigidArea(new Dimension(5,0) ) );
			p.add(zPanel);
			p.add( Box.createHorizontalGlue() );
			p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			
			return p;
		
	}
	
	
	/**
	 * Add a String to the log entry to display current processing in this algorithm,
	 * if logEntry is TRUE.
	 * <p>
	 * Also, write processing to a log file in the output directory.
	 * @param logEntry
	 */
	protected void logProcessing(String logEntry) {
		if(logImageProcessing.equalsIgnoreCase("true")) {
			IJ.log(logEntry);
		}
		
		// write the data to the decLogFile:
		try {
			fw = new FileWriter(cropLogFile, true);
			pw = new PrintWriter(fw);
			// write to the file:
			pw.print(logEntry+"\n");			
			// close the print writer:
			pw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	
	/**
	 * Retrieve input:
	 */
	@Override
	public void setup() {
		
		// retrieve the zSlices and overwrite variables:		
		zDepth = Integer.parseInt( zSlicesTextField.getText() );
		overwrite = overwriteCheckBox.isSelected();
		
		
		//Build the DOM from StereoMateSettings.xml:
		docSMS = StereoMateSettings.buildDomStereoMateSettings();
		
		//Now the DOM Document is built, retrieve the desired contents from it:
		docSMS.getDocumentElement().normalize();
		////IJ.showMessage("Root Element: "+ doc.getDocumentElement().getNodeName() );
	
		NodeList nList2;
		
		// This variable determines whether Image Processing is logged.
		nList2 = docSMS.getElementsByTagName("logProcessing");
		logImageProcessing = ((Element)nList2.item(0)).getAttribute("str");	
		
		
		if(overwrite) {
			cropLogFile = new File(dw.getInputFile(0).getAbsolutePath()
									+ File.separator + "crop_log.txt" );
		}
		else {
			cropLogFile = new File(dw.getOutputParentFile().getAbsolutePath() 
								+ File.separator + "crop_log.txt" );
		}
		
		try {
			fw = new FileWriter(cropLogFile, true);
			pw = new PrintWriter(fw);
			// write to the file:
			pw.print("Crop Stacks Log\n");
			pw.print("\n");
			pw.print("--------------------\n");
			pw.print("\n");
			pw.print("Settings:\n");
			pw.print("\n");
			pw.print("zDepth: "+zDepth+"\n");
			pw.print("overwrite: "+overwrite+"\n");
			pw.print("\n");
			pw.print("--------------------\n");
			pw.print("\n");
			pw.print("\n");

			// close the print writer:
			pw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}


		// All of the variables have been collected for deconvolution from xml file now,
		// Next need to deconvolve each image selected on the DialogWindow -> via process() method

		//Just need to Log the start of the Deconvolution here:
		logProcessing("Beginning Cropping of "+dw.totalFileCount+" files...");
		logProcessing("");
		logProcessing(" EXPECTED zSlices: "+zDepth+"  Overwrite: "+overwrite );
		logProcessing("--------------------------------------");
		
		
		
	}
	
	
	/**
	 * Sets up IWP such that the first channel is duplicated and added to HyperStack, retrieves a reference
	 * to this extra channel (stored in activeImp) & sets up the Panel for the Threshold Manager plugin.
	 */
	@Override
	public void process(ImagePlus imp) {
		
		
		logProcessing("");
		logProcessing("");
		logProcessing("processing image: "+imp.getTitle());
		logProcessing("");
		
		// just need to crop the image stack as specified:
		
		String pathToCroppedImage;
		
		if(overwrite) {
			pathToCroppedImage = dw.getCurrentFile(0).getAbsolutePath();
		} else {
			pathToCroppedImage = dw.getCurrentOutputFile().getAbsolutePath();
		}
				
		// compute START and END slices:
		int zSlices = imp.getNSlices();
		
		int zDiff = zSlices - zDepth; // this returns the FIRST SLICE POSITION:
		// Example - 60 slices originally recorded, 66 slices formed with stitching
			// The tiles will differentiate at 60 - this is the END SLICE
			// The tiles will ALSO differentiate at slices 1-5, and at slice 6 all tiles will be present
		// Therefore need to have START (zSlices - zDepth) and END (zDepth)
		
		
		// reslice the stack:
		imp = ij.plugin.SubHyperstackMaker.makeSubhyperstack(imp, "1-"+imp.getNChannels(), ""+zDiff+"-"+zDepth, "1-"+imp.getNFrames() );
		
		// save the stack:
		if(overwrite) {
			IJ.save(imp, pathToCroppedImage);
			imp.close();
		}
		else {
			dw.saveImpAndClose(imp);
		}
		
		
	}

	
	/**
	 * Clear all the objects and listeners to prevent memory leaks.
	 */
	@Override
	public void cleanup() {
		
		if(overwrite) {
			dw.deleteOutputDirTree();
		}
		
		//Shutdown DialogWindow:
		dw.shutDownDialogWindow();
				
		//Set all components in this class to null:
		dw = null;
		
	}
	

	
}
