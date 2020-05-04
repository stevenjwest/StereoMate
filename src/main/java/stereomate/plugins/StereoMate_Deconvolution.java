package stereomate.plugins;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.net.URISyntaxException;

import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.text.PlainDocument;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.StackWindow;
import ij.io.Opener;
import ij.plugin.ChannelSplitter;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;
import ij.plugin.RGBStackMerge;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.StackConverter;

import cern.colt.Arrays;
import edu.emory.mathcs.restoretools.Enums.OutputType;
import edu.emory.mathcs.restoretools.Enums.PrecisionType;
import edu.emory.mathcs.restoretools.iterative.IterativeEnums.BoundaryType;
import edu.emory.mathcs.restoretools.iterative.IterativeEnums.ResizingType;
import edu.emory.mathcs.utils.ConcurrencyUtils;
import edu.emory.mathcs.restoretools.iterative.wpl.WPLDoubleIterativeDeconvolver3D;
import edu.emory.mathcs.restoretools.iterative.wpl.WPLFloatIterativeDeconvolver3D;
import edu.emory.mathcs.restoretools.iterative.wpl.WPLOptions;

import stereomate.dialog.DialogWindow;
import stereomate.settings.MyIntFilter;

/**
 * 
 * This class selects a series of images via DialogWindow and runs them through the 
 * WPL deconvolution algorithm.  For multi-channel images, channels are split and
 * deconvolved separately with user-selected PSFs.  Frames are not currently supported.
 * <p>
 * Each single-channel z-Stack is then split in Z if its Z dimension exceeds the PSF Z dimension
 * which is currently 55 z slices).  The image(s) are then split in XY if the image size is
 * large enough to casue an OOM during deconvolution.  This has been pre-tabulated, and a look-up
 * table is used to determine how to split the images.
 * <p>
 * Deconvolution uses the WPL algorithm in Parallel Iterative Deconvolution, present in the 'pid' 
 * package (a package of Parallel Iterative Deconvolution put together locally).
 * <p>
 * Post-deconvolution, all images and channels are re-combined to reform the deconvolved image,
 * and saved to the relevant output folder.
 * 
 * @author stevenwest
 *
 */
public class StereoMate_Deconvolution implements StereoMateAlgorithm, PlugIn {
	
	/**
	 * NAMED CONSTANTS:
	 */
	int PSF_SLICE_NUMBER = 55;
	int PSF_XY = 125;

	DialogWindow dw;
	Container dialogWindowPanel;
	JComboBox<String> PsfSelector;
	JPanel PsfSelectorPanel;
	ArrayList<JComboBox<String>> PsfSelectors;
	ArrayList<Boolean> PsfSelectorsSelected;
	JPanel comboBoxAndButtonPanel;
	
	String[] channels;
	JComboBox<String> channelsComboBox;
	JComboBox<String> cb;
    String chan;
    int chanInd;
    String psf;
    int psfInd;
	
	JButton PsfSelectorButton;
	JButton OKButton;
	
	String iters;
	String threads;
	
	JTextField itersTextField, threadTextField;
	
	ImagePlus impPSF;
	ImagePlus blurredImage;
	ImagePlus deblurredImage;
	int bitDepth;
	
	ImagePlus blurredChannel;
	File tempDir;
	ImagePlus processingImage;
	
	String pathToDeblurredImage;
	String pathToDeblurredParent;
	
	File file;
	File stereoMateSettings;
	File wplAuto;
	InputStream in;
	
	Document docSMS;
	DocumentBuilder dBuilder;
	DocumentBuilderFactory dbFactory;
	
	String useMinDepthStr;
	
	String boundaryStr;
	String resizingStr;
	String outputStr;
	String precisionStr;
	String thresholdStr;
	String maxItersStr;
	String nOfThreadsStr;
	String showIterationsStr;
	String gammaStr;
	String filterXYStr;
	String filterZStr;
	String normalizeStr;
	String logMeanStr;
	String antiRingStr;
	String changeThreshPercentStr;
	String dbStr;
	String detectDivergenceStr;
	String logImageProcessing;
	
	boolean processedAnImage;
	
	int imagesProcessed;
	int imagesChannelsError;
	int imagesFramesError;
	
	
	/**
	 * Set up the Dialog Window for the SM Deconvolution plugin.  Input images via the default 
	 * File Selector, select Channels and PSFs via Selector boxes and buttons, and select
	 * Iterations and Threads via inputs boxes.
	 */
	@Override
	public void run(String arg) {

		// Set up DialogWindow here...
		dw = new DialogWindow("SM Deconvolution", this);
		
		dw.addFileSelector("Image(s) for Deconvolution:"); //add FileSelector panel.
				
		//Add criteria for User to add:
		
		//Number of Channels:
			//Add panel containing Channels Int Box, and Psf Selector Button:
		
		dw.add(new JSeparator() ); //separate file selector from channels & Psf selectors.
		
		dw.add( addChannelsAndPsfSelector() );
				
		//Add variables for deconvolution, and fill with default values:
			//Some of these may be better stored in a separate window to prevent
			//any alterations without someone knowing what they are doing.
			// only adding Iterations & Threads:
		
		dw.add( addItersAndThreads() );
		
		dw.addActionPanel(); //add Action panel - for Cancel and Process buttons.
	
		dw.setExternalInput(false); //set ExternalInput to false, this needs to be set to 'true'
									// before the "process" button can be pressed & the algorithm started.
	
		dw.setPlugInSuffix("_DEC"); //suffix for the file or DIR output.
		
		dw.layoutAndDisplayFrame(); //display the DialogWindow.
		
		dialogWindowPanel = dw.getContentPane(); //finally, store dialogWindow's panel to instance variable.
		
		//Next, initialise some of the instance variables relating to image processing:
		
		//set counters to 0:
		imagesProcessed = 0;
		imagesChannelsError = 0;
		imagesFramesError = 0;
		
		//set boolean processedAnImage to false:
			//This is for appropriate logging of image processing at end of process() loop 
			// in cleanup()
		processedAnImage = false;
		
	}


	/**
	 * This method adds a selector panel for the number of channels in the image as well as a Psf selector
	 * button.  These components need appropriate listeners added to ensure that for each channel added
	 * a PSF is selected for each, and only when both channel number and PSF selections have been made, is
	 * the ability to press the process button allowed to happen.
	 * @return
	 */
	protected JPanel addChannelsAndPsfSelector() {
		
		//A panel to store the channels and PSF selection buttons.
		JPanel p = new JPanel();
		
		//Number of Channels:
		//Make this initially BLANK to make sure the User selects a number of
		//Channels before proceeding [cannot press PROCESS unless this is selected, AND
		//PSFs are selected for Each Channel - this means WHENEVER this is edited, it 
		//should ALWAYS set 'setExternalInput' to false!]
		
		comboBoxAndButtonPanel = new JPanel();
		
		JLabel channelsLabel = new JLabel("No. of Channels:");
		
		comboBoxAndButtonPanel.add(channelsLabel);
		
		//Set of Strings for the ComboBox - beinning with a BLANK String (forces the user to select
			//a channel number - they consciously have to do it, therefore less likely to make a mistake..)
		channels = new String[] { "", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" };
		channelsComboBox = new JComboBox<String>(channels);
		
		//set actionListener to channelsComboBox - this needs to set the PsfSelectorButton to active
		//if a channel number is selected (i.e. the index of selection is above 0), or deactivate the
		//PsfSelectorButton if no channel is selected.
		//The DialogWindow itself should also have ExternalInput set to false if index selection is 0,
		//to deal with bug if channel is selected and then unselected by the user.
		channelsComboBox.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				cb = (JComboBox<String>)e.getSource();
		        chan = (String)cb.getSelectedItem();
		        chanInd = cb.getSelectedIndex();
		        if(chanInd > 0) {
		        	PsfSelectorButton.setEnabled(true);
		        	dw.setExternalInput(false); //set this to false, as if the channels number is altered, need
		        							   //to RESELECT the PSFs - will be enabled after PSFs are selected..
		        }
		        else {
		        	PsfSelectorButton.setEnabled(false);
		        	dw.setExternalInput(false); //set external input to false if number of channels is set to
		        						//blank by the user, after having made a selection...
		        }
			}
			
		});
		
		//Add this combobox to the Panel:
		comboBoxAndButtonPanel.add(channelsComboBox);
		
		
		//The PsfSelectorButton, when pressed, should SWITCH the current Panel on the DW JFrame for
		//a panel which contains the number of PsfSelectors for each channel selected in channelsComboBox...
		
		PsfSelectorButton = new JButton("PSFs");
		PsfSelectorPanel = new JPanel();
		
		//This actionListener needs to switch the panel on DW, and force the user to select a PSF for each
		//channel for the number they have selected.
		//Each channel will have a corresponding PSF Selector - only once a valid input is given for each, will
		//an OK button become active the for user to select - confirming the input, and ensuring valid input
		//of PSFs for each channel.

		//All of this should be built into this listener...
		
		PsfSelectorButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				
				JLabel PsfLabel = null;
				
				Dimension dimDw = dw.getSize();
				//construct the PsfSelectorPanel:
				PsfSelectorPanel = new JPanel();
				PsfSelectorPanel.setLayout( new BoxLayout(PsfSelectorPanel, BoxLayout.PAGE_AXIS) );
				
				//initialise PsfSelectors array:
				PsfSelectors = new ArrayList<JComboBox<String>>();
				
				//This ArrayList of booleans stores boolean values for each PsfSelector to inform the
					//listener if all have been selected.
				PsfSelectorsSelected = new ArrayList<Boolean>();
				
				//add psfSelectors to panel:
				for(int a=0; a<chanInd; a++) {
					
					PsfSelectorsSelected.add(false);
					PsfLabel = new JLabel("PSF for channel "+(a+1)+":" );
					PsfSelector = new JComboBox<String>();
					
					//add the PSFs to the selector, including an initial blank option.
						//This is done in a dedicated method to allow easy manipulation of PSF file selection
							//options.
					AddPsfItems(PsfSelector);
					
					//ActionListener on PSFSelector - should set the PsfSelectorsSelected boolean array
					//appropriately to signal that a selection has been made on given PsfSelector.
					//This should also check the PsfSelectorsSelected array - if all true, then the
					//OK button should be enabled, otherwise it should be disabled.
					//Also, if OK button is enabled, all input for channels is correct, and so the
					//setExternalInput() method in DW should be set to true...
					PsfSelector.addActionListener( new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							//First, set the PsfSelectorsSelected's appropriate boolean value:
							 cb = (JComboBox<String>)e.getSource(); //get the comboBox.
					         psf = (String)cb.getSelectedItem(); //get the String which is selected - name of PSF
					         psfInd = cb.getSelectedIndex(); //get selected index
					        for(int b=0; b<chanInd; b++) { //loop through all PsfSelectors
					        	if(PsfSelectors.get(b) == cb) { //if current PsfSelector equals correct ref in PsfSelectors...
					        		if(psfInd > 0) { //and if the selection index on the PsfSelector is above 0...
					        		PsfSelectorsSelected.set(b,  true); //set boolean in PsfSelectorsSelected to true.
					        		}
					        		else {
					        			PsfSelectorsSelected.set(b,  false); //else, index has been set to 0, so set
					        												//PsfSelectorsSelected to false.
					        		}
					        	}
					        		
					        }

					        //check PsfSelectorsSelected if all true, then activate OK Button:
					        boolean checkPsfSelectorsSelected = true;
					        for(int b=0; b<chanInd; b++) {
					        	if(PsfSelectorsSelected.get(b) == false) {
					        		checkPsfSelectorsSelected = false;
					        	}
					        }
					        
					        if(checkPsfSelectorsSelected == true) {
					        	OKButton.setEnabled(true);
					        	dw.setExternalInput(true);
					        }
					        else {
					        	OKButton.setEnabled(false);
					        	dw.setExternalInput(false);
					        }
					        
						}
						
					}); //end ActionListener.
					
					
					//Add PsfSelector to PsfSelectors array:
					PsfSelectors.add( PsfSelector );
					
					//Add PsfLabel and PsfSelector to the Panel:
					PsfSelectorPanel.add(PsfLabel);
					PsfSelectorPanel.add(PsfSelector);
					
				} //end for a
				
				
				//add OK button:
				OKButton = new JButton("OK");
				
				// OK button listener just needs to set the dw back to its original panel, pack, validate
				// and repaint...
				
				OKButton.addActionListener( new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						dw.setContentPane(dialogWindowPanel);
						dw.pack();
						dw.validate();
						dw.repaint();
					}
					
				}); //end OKButton ActionListener.
				
				
				//Add OK button to Panel:
				PsfSelectorPanel.add(OKButton);
				//initially set OK button to disabled - it is enabled in the PsfSelector's ActionListener.
				OKButton.setEnabled(false);
				
				//set Panel Height - this is to allow it to grow if its bigger than DW panel height..
				int panelHeight = (chanInd * (PsfSelector.getPreferredSize().height + PsfLabel.getPreferredSize().height )) + OKButton.getPreferredSize().height + 20;
				
				//set panel height either as high as DW panel, or bigger if needed:
				PsfSelectorPanel.setMinimumSize(dimDw);
				if(dimDw.height > panelHeight) {
					PsfSelectorPanel.setPreferredSize(dimDw);
				}
				else {
					dimDw.height = panelHeight;
					PsfSelectorPanel.setPreferredSize(dimDw);
				}
				
				//setContentPane of dw to the PsfSelector Panel, pack, validate, repaint...
				dw.setContentPane(PsfSelectorPanel);
				dw.pack();
				dw.validate();
				dw.repaint();
			}
			
		}); //end PsfSelectorButton ActionListener.
		
		
		//Initially PsfSelectorButton should be disabled - only enabled once some valid channel number is selected
		//on the channels combobox..
		PsfSelectorButton.setEnabled(false);
		
		//add this button to the panel:
		comboBoxAndButtonPanel.add(PsfSelectorButton);
		
		//return a panel which contains the comboBoxAndButtonPanel, using BorderLayout:
		p.setLayout(new BorderLayout() );
		p.add(comboBoxAndButtonPanel, BorderLayout.CENTER);
		
		//return the panel:
		
		return p;
	}
	
	
	/**
	 * This method contains the possible selections for PSFs within the Deconvolution Algorithm.  These names
	 * should correspond to file names in the "PSF" folder inside the Stereo_Mate JAR file.  NB: The first 
	 * selection should be BLANK.
	 * @param PsfSelector The PsfSelector object which the PSF selections will be added to.
	 */
	protected void AddPsfItems(JComboBox<String> PsfSelector) {
		PsfSelector.addItem("");
		PsfSelector.addItem("405nm 1.4NA 170um 1.518RI 40x40nm 5.0x5.0.tif");
		PsfSelector.addItem("488nm 1.4NA 170um 1.518RI 40x40nm 5.0x5.0.tif");
		PsfSelector.addItem("546nm 1.4NA 170um 1.518RI 40x40nm 5.0x5.0.tif");
		PsfSelector.addItem("647nm 1.4NA 170um 1.518RI 40x40nm 5.0x5.0.tif");
		PsfSelector.addItem("488nm 40x40x100 ExEm.tif");
		PsfSelector.addItem("546nm 40x40x100 ExEm.tif");
		PsfSelector.addItem("405nm 1.4NA 170um 1.518RI 60x60nm 5.0x5.0.tif");
		PsfSelector.addItem("488nm 1.4NA 170um 1.518RI 60x60nm 5.0x5.0.tif");
		PsfSelector.addItem("546nm 1.4NA 170um 1.518RI 60x60nm 5.0x5.0.tif");
		PsfSelector.addItem("647nm 1.4NA 170um 1.518RI 60x60nm 5.0x5.0.tif");
		//PsfSelector.addItem("1.4NA 170um 1.518RI Alexa 647 40x40nm 5.0x5.0.tif");
	}
	
	
	/**
	 * Method to add a panel containing the iteration and threads selection boxes.
	 * @return
	 */
	protected JPanel addItersAndThreads() {
		
		//create new JPanel:
		JPanel p = new JPanel();
		
		//JPanel for iteration and thread Labels and textfields:
		JPanel itersAndThreadsPanel = new JPanel();
		
		//Label and Textfield for Iteration:
		JLabel itersLabel = new JLabel("Max. Iterations:");
		itersTextField = new JTextField(3);
		
		//Build the DOM from StereoMateSettings.xml:
		docSMS = StereoMateSettings.buildDomStereoMateSettings();
		
		//Now the DOM Document is built, retrieve the desired contents from it:

		docSMS.getDocumentElement().normalize();
		////IJ.showMessage("Root Element: "+ doc.getDocumentElement().getNodeName() );
		
		NodeList nList2;
			
		nList2 = docSMS.getElementsByTagName("maxIters");
		maxItersStr = ((Element)nList2.item(0)).getAttribute("str");

		
		itersTextField.setText(maxItersStr);
		
		//Using a Document Filter to force TextField to contain only Ints
			//This also prevents pasting of non-int characters.
			//I have modified the MyIntFilter class to allow Blank Strings
			//to be inserted..
		PlainDocument doc = (PlainDocument) itersTextField.getDocument();
	     doc.setDocumentFilter(new MyIntFilter());

	    //JPanel for threads label and textfield:
	    JLabel threadLabel = new JLabel(" No. of Threads:");
	    threadTextField = new JTextField(2);
	    threadTextField.setText(""+ConcurrencyUtils.getNumberOfProcessors() );
	    
	    doc = (PlainDocument) threadTextField.getDocument();
	    doc.setDocumentFilter(new MyIntFilter());
	    
	    //add labels and textfields to panel:
	    itersAndThreadsPanel.add(itersLabel);
	    itersAndThreadsPanel.add(itersTextField);
	    itersAndThreadsPanel.add(threadLabel);
	    itersAndThreadsPanel.add(threadTextField);
	    
	    //set max width of panel to 'pack' components onto panel:
	    int maxWidth = itersLabel.getPreferredSize().width + itersTextField.getPreferredSize().width +
	    					threadLabel.getPreferredSize().width + threadTextField.getPreferredSize().width + 25;
	    int maxHeight = itersTextField.getPreferredSize().height + 5;
	    itersAndThreadsPanel.setMaximumSize(new Dimension(maxWidth, maxHeight) );
	    itersAndThreadsPanel.setPreferredSize(new Dimension(maxWidth, maxHeight) );
	    
		//set layout to BoxLayout:
		p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
		p.add(Box.createRigidArea(new Dimension(5,0) ) );
		p.add(itersAndThreadsPanel);
		p.add( Box.createHorizontalGlue() );
		p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		
		return p;
	}


	
	
	/**
	 * Add a String to the log entry to display current processing in this algorithm.
	 * @param logEntry
	 */
	protected void logProcessing(String logEntry) {
		if(logImageProcessing.equalsIgnoreCase("true")) {
			IJ.log(logEntry);
		}
	}
	
	
	@Override
	public void setup() {
		
		
		// NOW: Read the Deconvolution Settings from the StereoMateSettings.xml file in the .settings DIR:
		
		//Retrieve file representation of JAR file - to retrieve its absolute path:
		
			//StereoMateSettings.xml read and DOM built in call to 'addItersAndThreads()', so do not need to
			//construct the DOM again here...
		
		//Now the DOM Document is built, retrieve the desired contents from it:
		
		
		docSMS.getDocumentElement().normalize();
			////IJ.showMessage("Root Element: "+ doc.getDocumentElement().getNodeName() );
		
			NodeList nList2;
			
			//The useMinDepth variable determines whether the DIR structure will accept file lying
			// at the MINIMUM depth (true) or MAXIMUM depth (false).
				//See StereoMateInputOutput class..
			nList2 = docSMS.getElementsByTagName("useMinDepth");
			useMinDepthStr = ((Element)nList2.item(0)).getAttribute("str");
			
			//In the Boundary combo-box you can choose from three types of boundary conditions: 
			//Reflexive, Periodic and Zero. The first ones are usually the best choice.
			//available options: REFLEXIVE, PERIODIC, ZERO			
			 nList2 = docSMS.getElementsByTagName("boundary");
			 boundaryStr = ((Element)nList2.item(0)).getAttribute("str");
			
				//The Resizing combo-box allows you to specify how the blurred image will be padded 
				//before processing. The Minimal resizing means that the pixel data in each dimension 
				//of a blurred image are padded by the size of the corresponding dimension of a PSF 
				//image. If the Next power of two option is selected, then the pixel data in each 
				//dimension of a blurred image are padded to the next power-of-two size that is 
				//greater or equal to the size of an image obtained by minimal padding. Finally, 
				//the Auto option chooses between the two other options to maximize the performance.
				//String resizingStr = "AUTO"; // available options: AUTO, MINIMAL, NEXT_POWER_OF_TWO
			 nList2 = docSMS.getElementsByTagName("resizing");
			resizingStr = ((Element)nList2.item(0)).getAttribute("str");
			
			//The Output list is used to specify the type of an output (reconstructed image)
			// available options: SAME_AS_SOURCE, BYTE, SHORT, FLOAT  
			 nList2 = docSMS.getElementsByTagName("output");
			outputStr = ((Element)nList2.item(0)).getAttribute("str");
			
			//in the Precision combo-box you can choose a floating-point precision used in 
			//computations. Practice shows that a single precision is sufficient for most problems.
			//available options: SINGLE, DOUBLE
			 nList2 = docSMS.getElementsByTagName("precision");
			precisionStr = ((Element)nList2.item(0)).getAttribute("str");
		
			//When the Threshold option is enabled, then all values in the reconstructed image 
			//that are less than the value specified in the threshold text field are replaced 
			//by zero. However, since WPL is a nonnegatively constrained algorithm, this option 
			//is not very useful and is disabled by default.
			//if -1, then disabled
			 nList2 = docSMS.getElementsByTagName("threshold");
			thresholdStr = ((Element)nList2.item(0)).getAttribute("str");
			
			//The Max number of iterations text field is used to specify how many iterations a 
			//given method should perform. It is a maximal value, which means that the process 
			//of reconstruction may stop earlier (when the stopping criterion is met).
			maxItersStr = ""+itersTextField.getText();
			////IJ.showMessage("Max Iters: "+maxItersStr);
			
			//In the Max number of threads (power of 2) text field you can enter how many 
			//computational threads will be used. By default this value is equal to the number 
			//of CPUs available on your machine.
			nOfThreadsStr = ""+threadTextField.getText();
			////IJ.showMessage("No Threads: "+nOfThreadsStr);
			
			//When the Show iterations check-box is selected, then the reconstructed image will 
			//be displayed after each iteration.
			 nList2 = docSMS.getElementsByTagName("showIterations");
			showIterationsStr = ((Element)nList2.item(0)).getAttribute("str");
				
			//The Wiener filter gamma is a tolerance for the preconditioner. It is intended to speed up 
			//the convergence, but can produce spurious artifacts. Setting this parameter to zero turns 
			//off the preconditioner (Wiener Filter).
				//suggest 0 [<0.0001] to turn off, 0.0001 - 1 as tests
			 nList2 = docSMS.getElementsByTagName("gamma");
			gammaStr = ((Element)nList2.item(0)).getAttribute("str");
				
			//The Low pass filter xy and z settings, in pixels, provide a way to smooth the results and 
			//accelerate convergence. Choose 0 to disable this function.
				//Suggest 1.0, 0 to turn off.
			 nList2 = docSMS.getElementsByTagName("filterXY");
			filterXYStr = ((Element)nList2.item(0)).getAttribute("str");
			
			 nList2 = docSMS.getElementsByTagName("filterZ");
			filterZStr = ((Element)nList2.item(0)).getAttribute("str");
			
			//If Normalize PSF is selected then the point spread function is normalized before processing.
			//NORMALIZE causes resulting PSF to be normalized so Total( psf ) = 1.
			 nList2 = docSMS.getElementsByTagName("normalize");
			normalizeStr = ((Element)nList2.item(0)).getAttribute("str");
			
			//selecting Log mean pixel value to track convergence effects in displaying the record 
			//of the convergence in a separate Log window.
			 nList2 = docSMS.getElementsByTagName("logMean");
			logMeanStr = ((Element)nList2.item(0)).getAttribute("str");
		
			//To reduce artifacts from features near the boundary of the imaging volume you should 
			//use the Perform anti-ringing step option.
			 nList2 = docSMS.getElementsByTagName("antiRing");
			antiRingStr = ((Element)nList2.item(0)).getAttribute("str");
			
			//Finally, the Terminate iteration if mean delta less than x% is used as a stopping criterion.
			//Suggest 0.01, 0 to turn off
			 nList2 = docSMS.getElementsByTagName("changeThreshPercent");
			changeThreshPercentStr = ((Element)nList2.item(0)).getAttribute("str");
				
			//For WPL, the inputs in decibels are permitted (Data (image, psf and result) in dB). 
			//This is uncommon in optical image processing, but is the norm in acoustics.
			 nList2 = docSMS.getElementsByTagName("db");
			dbStr = ((Element)nList2.item(0)).getAttribute("str");
				
			//The Detect divergence property stops the iteration if the changes appear to be increasing. 
			//You may try to increase the low pass filter size if this problem occurs.
			 nList2 = docSMS.getElementsByTagName("detectDivergence");
			detectDivergenceStr = ((Element)nList2.item(0)).getAttribute("str");
				
			// This variable determines whether Image Processing is logged.
			 nList2 = docSMS.getElementsByTagName("logProcessing");
			logImageProcessing = ((Element)nList2.item(0)).getAttribute("str");			

			
		// All of the variables have been collected for deconvolution from xml file now,
				// Next need to deconvolve each image selected on the DialogWindow -> via process() method
				
		//Just need to Log the start of the Deconvolution here:
		logProcessing("Beginning Deconvolution of "+dw.totalFileCount+" files...");
		logProcessing("");
		logProcessing("Threads: "+nOfThreadsStr+"  Iterations: "+maxItersStr );
		logProcessing(" Channels: "+PsfSelectors.size()+"  PSF Z Slices: "+PSF_SLICE_NUMBER );
		logProcessing("--------------------------------------");
		
		
	}


	/**
	 * The process() method begins the process of performing deconvolution.  Before the deconvolution itself
	 * can be run, the image must be passed through a series of objects which will determine whether the image
	 * should be split in various dimensions:  Channels, depth, width/height.
	 * <p>
	 * Once the image has been divided into appropriate tiles for deconvolution, the deconvolution algorithm
	 * can process each tile respectively, which will be performed in the deconvolve() method.
	 * <p>
	 * Splitting the image into separate channels, stacks, and image width/heights will be performed by the
	 * following inner classes:  SMChannelSplitter, ZStackSplitter, XYSplitter.
	 */
	@Override
	public void process(ImagePlus impToProcess) {
		
		//open blurred image - from first fileSelector [index 0]:
		//blurredImage =  dw.getCurrentImp(0);  //this will continue trying to open files until a valid image is
												//found...
			//NOT REQUIRED ANYMORE WITH NEW DialogWindow3 STRUCTURE!!
			//IMP IS PASSED INTO PROCESS METHOD FROM DIALOGWINDOW3
		
		//with new Structure in DialogWindow3, only need to set blurredImage to impToProcess..
		blurredImage = impToProcess;
		
		logProcessing("");
		logProcessing("");
		logProcessing("processing image: "+blurredImage.getTitle());
		logProcessing("");
		
		//set bitDepth variable according to outputStr:
			// values: SAME_AS_SOURCE, BYTE, SHORT, FLOAT 
			// required as deconvolution always returns a 32-bit image.
			// and will need to carefully convert to correct bit-depth the z-stack - via resetBitDepth()
		
		if( outputStr.equalsIgnoreCase("SAME_AS_SOURCE") ) {
				 bitDepth = blurredImage.getBitDepth(); 
		}
		else if(outputStr.equalsIgnoreCase("BYTE") ) {
			bitDepth = 8;
		}
		else if(outputStr.equalsIgnoreCase("SHORT") ) {
			bitDepth = 16;
		}
		else if(outputStr.equalsIgnoreCase("FLOAT") ) {
			bitDepth = 32;
		}
		
		//loop through each file present in the selection from the DialogWindow:
			//NOT REQUIRED ANYMORE!!  NEW DIALOGWINDOW3 STRUCTURE DOES THIS!
		//while( dw.moreFilesToProcess() ) {
			// THIS IS A WHILE LOOP!!
				//See DialogWindow3: 2440 & 70...
			
			//need to call this method again, as image may have changed bit-depth as while loop progresses
				//Therefore, set bitdepth for each image parsed:
		//AGAIN REDUNDANT CODE WITH NEW DIALOGWIDNWO3 STRUCTURE!
			//if( outputStr.equalsIgnoreCase("SAME_AS_SOURCE") ) {
			//	 bitDepth = blurredImage.getBitDepth(); 
			//}
			
			//set output file:
			pathToDeblurredImage = dw.getCurrentOutputFile().getAbsolutePath();
			
			//get Output Parent DIR for saving TEMP file, if required:
			pathToDeblurredParent = dw.getCurrentOutputFile().getParentFile().getAbsolutePath();
			
			//First check the image is appropriate for given input.
				//No Frames, & matched Channel number..
			boolean processImage = true;
			int ch = blurredImage.getNChannels();
			int fr = blurredImage.getNFrames();
			int sl = blurredImage.getNSlices();
			////IJ.showMessage("frames: "+fr+" slices: "+sl);
			
				//check if image has any frames:
				if(fr > 1 ) {
					processImage = false;
					//Log this information:
					logProcessing("Image "+dw.getCurrentFileIndex()+" of "+dw.totalFileCount+" failed to deconvolve:");
					logProcessing("  This image contains frames which are not handled");
					logProcessing("  please split the image and deconvolve separately.");
					logProcessing("");
					//count this in the imagesFramesError variable:
					imagesFramesError = imagesFramesError + 1;
				}
			
				//check if image has correct number of channels:
				if(ch != PsfSelectors.size() ) {
					processImage = false;
					logProcessing("Image "+dw.getCurrentFileIndex()+" of "+dw.totalFileCount+" failed to deconvolve:");
					logProcessing("  This image contains a different number of channels");
					logProcessing("  than specified.");
					logProcessing("");
					//count this in the imagesChannelsError variable:
					imagesChannelsError = imagesChannelsError + 1;
				}
			
			
			//proceed with image processing if processImage is still true:
			if(processImage == true) {
			
				//Make Temp DIR:
				makeTempDir();
			
				//pass the blurred image to the splitBlurredImageChannel() method:
					//this begins passing the image through Channel, Z and XY splitters to deconvolution
				deblurredImage = splitBlurredImageChannel(blurredImage);
				
				//the returned image is 32-bit - need to convert back to original bit-depth
					//dont clip the image!
				
				//deblurredImage = resetBitDepth(deblurredImage, bitDepth);
				//DO NOT DO THIS HERE -> MUST DO THIS AFTER RECONSTRUCTING Z STACK!
				
				//deblurredImage.show();
				//IJ.showMessage("Look at deblurred stack");
				//deblurredImage.hide();
				
				//add one to imagesProcessed:
				imagesProcessed = imagesProcessed + 1;			
			
				//Finally, delete the TEMP DIR [which should now be empty, as all channels and other
				//images saved to it have been processed and deleted after processing..]:
					//delete any remaining contents of tempDir:
					//String[] files = tempDir.list();
					File[] files = tempDir.listFiles();
					for(int zed=0; zed<files.length; zed++) {
						//IJ.showMessage("tempDir file no "+zed+" is: "+files[zed]);
						files[zed].delete();
					}
				tempDir.delete();
				
				dw.saveImpAndClose(deblurredImage);
		
			} //end if processImage == true
			
			
			//ALL THE BELOW IS PERFORMED IN THE DIALOGWINDOW processImages() METHOD
				//DO NOT DO IT HERE -> IT WILL NOT WORK!
			
			//Save the output, deblurredImage, via dw [to correct output DIR]:
			//dw.saveImpAndClose(deblurredImage);
				//DO PERFORM THIS STEP -> THIS IS NOT PERFORMED IN DIALOGWINDOW LOOP!!
				//HOWEVER -> MOVE TO INSIDE THE processImage == true LOOP!
			
			//increment current file index - to move to next file!
			//dw.incrementCurrentFileIndex();
			
			//open NEXT blurred image - from first fileSelector [index 0]:
			//blurredImage =  dw.getCurrentImp(0);
		
		//}//end while loop - loop through files in input
				
		
		
	} //end process()
	
	
	
	/**
	 * This method first provides important information to the user concerning the images processed,
	 * then cleans up this class, the Dialog Window class and its components, as well as removing the
	 * ActionListeners set up in this class on the channelsComboBox, PsfSelectorButton,
	 * PsfSelector and OKButton objects (which otherwise prevent these objects from being GC'd).
	 * Finally, the GC is run to clear the un-referenced objects.
	 */
	@Override
	public void cleanup() {
		

		//if the processedAnImage is still false, want to DELETE the DIR structure formed for
		//this analysis - as it is now useless [as NO IMAGES were processed!]
			//THIS SHOULD BE MOVED TO THE DIALOGWINDOW CLASS -> THIS IS NOT THE RESPONSIBILITY OF THE 
				//STEREOMATE ALGORITHM, BUT INPUT-OUTPUT!
		
		if(processedAnImage == false) { 
			//if still false, delete DIRs:
			dw.deleteOutputDirTree();
			//And log this action:
			logProcessing("");
			logProcessing("");
			logProcessing("0 of "+dw.totalFileCount+" files processed.");
			logProcessing("Output DIR Tree deleted, please run again." );
			logProcessing("--------------------------------------");
			logProcessing("");
		}
		else {
			//if images have been deconvolved, then finish with summary information:
			logProcessing("");
			logProcessing("");
			logProcessing("Deconvolution complete." );
			logProcessing(""+imagesProcessed+" of "+dw.totalFileCount+" files processed.");
			if(imagesProcessed != dw.totalFileCount) {
			logProcessing("Channel mismatch error: "+imagesChannelsError);
			logProcessing("Images containing frames: "+imagesFramesError);
			logProcessing("Thanks - and have a nice day!");
			}
			logProcessing("--------------------------------------");
		}
		
		
		// ***  Clean up Object References  *** //
		
		//remove ActionListeners:
		ActionListener[] als = channelsComboBox.getActionListeners();
		for(int a=0; a<als.length; a++) {
			channelsComboBox.removeActionListener(als[a]);
		}
		
		als = PsfSelectorButton.getActionListeners();
		for(int a=0; a<als.length; a++) {
			PsfSelectorButton.removeActionListener(als[a]);
		}
		
		als = PsfSelector.getActionListeners();
		for(int a=0; a<als.length; a++) {
			PsfSelector.removeActionListener(als[a]);
		}
		
		als = OKButton.getActionListeners();
		for(int a=0; a<als.length; a++) {
			OKButton.removeActionListener(als[a]);
		}
		
		//Shutdown DialogWindow:
		dw.shutDownDialogWindow();
		
		//Set all components in this class to null:
		dw = null;
		
		dialogWindowPanel = null;
		PsfSelector = null;
		PsfSelectorPanel = null;
		PsfSelectors = null;
		PsfSelectorsSelected = null;
		comboBoxAndButtonPanel  = null;
		
		channels = null;
		channelsComboBox = null;
		cb = null;
	    chan = null;
	    psf = null;
		
		PsfSelectorButton = null;
		OKButton = null;
		
		iters = null;
		threads = null;
		
		itersTextField = null;
		threadTextField = null;
		
		 impPSF = null;
		 blurredImage = null;
		 deblurredImage = null;
		
		 blurredChannel = null;
		 tempDir = null;
		 processingImage = null;
		
		 pathToDeblurredImage = null;
		 pathToDeblurredParent = null;
		
		 file = null;
		 stereoMateSettings = null;
		 wplAuto = null;
		 in = null;
		
		 docSMS = null;
		 dBuilder = null;
		 dbFactory = null;
		
		 useMinDepthStr = null;
		
		 boundaryStr = null;
		 resizingStr = null;
		 outputStr = null;
		 precisionStr = null;
		 thresholdStr = null;
		 maxItersStr = null;
		 nOfThreadsStr = null;
		 showIterationsStr = null;
		 gammaStr = null;
		 filterXYStr = null;
		 filterZStr = null;
		 normalizeStr = null;
		 logMeanStr = null;
		 antiRingStr = null;
		 changeThreshPercentStr = null;
		 dbStr = null;
		 detectDivergenceStr = null;
		 logImageProcessing = null;

		
		//Run the GC:
		
				System.gc();
				System.gc();
		
		//once the deconvolution is complete, display status in IJ status bar:
		IJ.showStatus("StereoMate Deconvolution: Complete.");
	}
	
	
	/**
	 * Converts the Deconvolved 32-bit image back to original bit-depth.  Ensures the min and max pixel values
	 * are set to the min and max through entire stack, to prevent excessive over-exposed pixels during conversion.
	 * @param imp The 32-bit deconvolved & reconstructed image, which needs converting back to original bit-depth.
	 * @param bitDepth The bit-depth of original un-deconvolved image.
	 * @return ImagePlus the deconvolved and reconstructed image at correct bit depth.
	 */
	public ImagePlus resetBitDepth(ImagePlus imp, int bitDepth) {
			
		//first reset min and max to slice with max maxVal:
			int indexMaxVal = 0;
			double maximum = -1.0;
			double maxImp;
			ImageProcessor ip4;
			for(int a=1; a<=imp.getNSlices(); a++) {
				imp.setSlice(a);
				ip4 = imp.getProcessor();
				ip4.resetMinAndMax();
				maxImp = ip4.getMax();		
				if(maxImp > maximum) {
					maximum = maxImp;
					indexMaxVal = a;
				}
			}

			//IJ.showMessage("max slice: "+indexMaxVal);
			imp.setSlice(indexMaxVal);
			ip4 = imp.getProcessor();
			ip4.resetMinAndMax();		
		
		//use a StackConverter object!
		StackConverter sc = new StackConverter(imp);
		
		//convert imp according to bitDepth:
		
		if(bitDepth == 8) {
			 sc.convertToGray8();
		}
		else if(bitDepth == 16) {
			 sc.convertToGray16();
		}

		return imp;
		
	} //end resetBitDepth()
	
	
	protected void makeTempDir() {
	
		//First, create a TEMP folder in the outputDIR..
		//Create this exactly where the output file will be saved!
		tempDir = new File(pathToDeblurredParent + File.separator + "TEMP" + File.separator);
		////IJ.showMessage("Path TEMP: "+tempDir.getAbsolutePath() );
		boolean madeDir = tempDir.mkdir();
		////IJ.showMessage("Made DIR: "+madeDir);
	
	}
	
	
	public ImagePlus splitBlurredImageChannel(ImagePlus blurredImage) {
		
		//create a channelSplitter object:
		SMChannelSplitter channelSplitter = new SMChannelSplitter(blurredImage, tempDir);
		
		logProcessing("Splitting Channels: "+channelSplitter.getChannelNumber() );
		//logProcessing("");
		
		//At this point, a single channel z stack is present in processingImage inside SMChannelSplitter, 
		//and if more than one channel was present, these are saved to disk in the tempDir, and each file 
		//reference is stored in chFileArray, in SMChannelSplitter.
		
		//Next step is to loop through all channels and process each z stack:
		
		//Loop through each Channel:
		
	for(int b=0; b<channelSplitter.getChannelNumber() ; b++) {
			
		//IJ.showMessage("b in splitBlurredImageChannel: "+b);
		//IJ.showMessage("File count in chFileArray: "+channelSplitter.chFileArray.size() );
		if(b > 0) {
			//if b is greater than 0, the first channel will have been processed - therefore
			//need to retrieve and open the next channel:
				//index of file in channekSplitter chFileArray - its 1 less than b, as the first
								//image is not in the chFileArray!!!
			blurredImage = IJ.openImage( channelSplitter.chFileArray.get(b-1).getAbsolutePath() );

		}
		else { //Else, this is the first image - just set the blurredImage imp to the current image in channelSplitter.
			blurredImage = channelSplitter.processingImage;
		}

		//determine PSF - save to impPSF:
		
		String PSF = (String)PsfSelectors.get(b).getSelectedItem();
		//String plugins = IJ.getDirectory("plugins");
		//String PsfPath = plugins + "Stereo_Mate" + File.separator + PSF.getPath();
		////IJ.showMessage(PsfPath );
		//ImagePlus impPSF = IJ.openImage( PsfPath );
		impPSF = openTiffFromJar(
				File.separator + "PSF" + File.separator + PSF, 
				PSF
				);
		//IJ.showMessage("impPSF: " + impPSF);
		//impPSF.show();
		//IJ.showMessage("Look at PSF!");
		//impPSF.hide();
		//ImageProcessor ip2 = scalePSF(impPSF, 99, 99, 29);
		//ImagePlus imp2 = new ImagePlus("ImagePlus2",ip2);
		//imp2.show();
		
		logProcessing("  Ch"+(b+1)+" PSF: "+PSF);
		
		//determine image dimensions and split image to be processed [processingImage] as required:	
			//Save each segment onto Disk - and delete the Channel image!
		blurredImage = splitBlurredImageZ(blurredImage, b);
			//Note, this method calls the deconvolution algorithm - as it may be split 
			//After a call to this method, and the deconstruction, deconvolution and reconstruction
			//of the image is handled in this method.
		
		//The returned image is 32-bit -> need to convert to correct bitDepth here, before reconstructing
		//any channels:		
		//the returned image is 32-bit - need to convert back to original bit-depth
		//dont clip the image!  Use resetBitDepth to do this:
	
		blurredImage = resetBitDepth(blurredImage, bitDepth);
		
		//only need to save deblurredImage if more than one channel:
		//if(channelSplitter.getChannelNumber() > 1) { -> This Check is made in the saveDeblurred() method!
			//if greater than one, then each channel must be saved to disk, for reconstruction:
			//save image to disk.
			//and save the file to a File ArrayList [inside channelSplitter]
			channelSplitter.saveDeblurred(blurredImage);
		//}
		
		//Finally, delete the representation of the passed image on the disk:
		if(b > 0) {	//delete only if b is greater than 0!  Otherwise, if a is 0, this image is not
				// on the HD...
			//Delete this image on the File System:
			channelSplitter.chFileArray.get(b-1).delete();  //once open, the image is loaded to memory, so can safely
															//delete it from Disk..
		}
		
		
	}//end for b - loop through channel images
	
	
		//Finally, need to reconstruct the images!
			//This is performed in the SmSplitters.
	
		//Once reconstructed, pass the image back to the preceding method - process()...
		//Return an imp!!

		//reconstruction only necessary if channelSplitter split image:
		if( channelSplitter.getChannelNumber() > 1) {
			//reconstruct the saved deblurred images inside channelSplitter
				//these will be derived from calls to saveDeblurred()
			//Make sure this method returns the reconstructed image:
			return channelSplitter.reconstructDeblurred();
		}
		else {
			//else, the zStackSplitter did not split the image, so just return the delburredImage:
			return blurredImage;
		}
	
	
	}//end splitBlurredImageChannel
	
	
	
	/**
	 * Splits the blurred image into separate z stacks.  This is performed with a ZStackSplitter object.
	 * Substacks are formed of the original blurred image stack if the stack itself is larger than 
	 * the PSF image stack.
	 * @param blurredImage Image to be processed.
	 * @param chNum Channel number being processed.
	 */
	public ImagePlus splitBlurredImageZ(ImagePlus blurredImage, int chNum) {

		//FIRST:
		//Construct a new ZStackSplitter obj to process the zStack:
		ZStackSplitter zStackSplitter = new ZStackSplitter(blurredImage, PSF_SLICE_NUMBER, tempDir);
		
			//The above construction will:
				//1. Initialise the zStackSplitter object, and determine the start and end slices for each substack.
				//2. Divide the blurredImage into substacks as required, and save to the subStacks variable.
				// 		Note, even if the stack is not split, the original stack is saved to subStacks!
			
			
			//Now want to SAVE the subStacks not to be processed to the Hard Disk, and set up a loop to process
			//through these...
			
			//Save all imps beyond the first one, and close:
		
		zStackSplitter.saveSubStack();
		
		// At this point, a single channel z stack is present in subStacks [ref0] inside zStackSplitter, 
		//and if the imp was split, the other zStacks are saved to disk in the tempDir, and each file 
		//reference is stored in zFileArray, in zStackSplitter.
		
			
			//next, process each image in zStackSplitter zFileArray - i.e. the saved images from
				//the subStack:
			// This should be performed in the zStackSplitter object!
		
		logProcessing("  Ch"+(chNum+1)+": Splitting Z Stack: "+zStackSplitter.getSize());
		//logProcessing("");
			
		for(int a=0; a<zStackSplitter.getSize(); a++) {
			//Get the next imp from the zStackSplitter:
				// this will only retrieve one image if no splitting occurred - the original stack.
			blurredImage = zStackSplitter.getNextImp(a);
				
				//pass this image to the splitBlurredImageXY method - to continue passing it down the
					//methods to deconvolution:
			
			blurredImage = splitBlurredImageXY( blurredImage, a ); //must return an imp from this method!
			
			//if more than one file is generated in zStackSplitter during the split, as each image is
			// processed it must be saved to disk.
			if(zStackSplitter.getSize() > 1) {
				//save image to disk and save the file to a File ArrayList [inside zStackSplitter]
				zStackSplitter.saveDeblurred(blurredImage);
			}
				
			//DO NOT need to delete the passed image from zStackSplitter, as this now contains the deconvolved
			//image!
				
		}//end for a
					
			//Finally, need to reconstruct the images!
			
			//Once reconstructed, pass the image back to the preceding method - splitChannels()...
			//Return an imp!!
		
		//reconstruction only necessary if zStackSplitter split the original image:
		if(zStackSplitter.getSize() > 1) {
			//reconstruct the saved deblurred images inside zStackSplitter
				//these will be derived from calls to saveDeblurred()
			//Make sure this method returns the reconstructed image:
			return zStackSplitter.reconstructDeblurred();
		}
		else {
			//else, the zStackSplitter did not split the image, so just return the delburredImage:
			//first, look at this image:
			//blurredImage.show();
			//IJ.showMessage("Look at z stack");
			//blurredImage.hide();
			return blurredImage;
		}

		
	}
	
	/**
	 * Split blurred image in XY (width/height).  This depends on the efficiency of the WPL deconvolution algorithm
	 * which has been determined empirically.
	 * @param blurredImage
	 * @param zNum Z Stack number being processed.
	 */
	public ImagePlus splitBlurredImageXY(ImagePlus blurredImage, int zNum) {
		
		ImagePlus deblurredImage;
		
		//SECOND:
		//Does the XY dimensions [Image Size] allow complete deconvolution efficiently & within memory constraints?
			//Efficient - means the image size is deconvolved efficiently with the given PSF size.
			//Memory constraints - calculated based on how much memory is available, an how big the image is.
			
		//If the image does not fit these criteria, it needs to be broken up:
		
		//IJ.showMessage("Check Files on HD - current image is "+blurredImage.getTitle() );
		
		// Construct a XYStackSplitter obj to split image in XY if necessary for Deconvolution:
		XYStackSplitter xyStackSplitter = new XYStackSplitter( blurredImage, tempDir );
		
		//The above construction will:
			// 1. Determine if the image size in XY, and therefore how much memory need for deconvolution,
			//    is within the memory constraints of the current JVM.
			// 2. Determine whether deconvolution with WPL AUTO may be MORE EFFICIENT if the image is divided
			//    into smaller segments.
		// Inside the XYStackSplitter object are two CropPoint objects, which if not null, provide the information
		// on how the image should be split.
		
			// 3. Save Images To Disk!
		
		//Next, can split the image, as required, and save any tiles above the first one to disk..
		//This should be performed in XYStackSplitter, and there is a method for it, which performs
		//these steps:
		xyStackSplitter.splitImage(); //this splits images and puts the Imps into splitImps ArrayList.
		
		//Next, need to save these appropriately - keep the first imp open, but save the remainder and keep
			//a File ArrayList representation of these files on the HD:
		xyStackSplitter.saveSplitImps();
		
		// Need to confirm there are no edge effects after deconvolution....
			// There are no edge effects after deconvolution!  Well, they are insignificant compared to
			// the differences in deconvolution caused by a different evnironment/ array of pixels.
			// see 465-02 EDGE EFFECTS
		
		// Need to confirm over and under exposed pixels can be effectively dealt with...
			// Analysis of this suggests that images are calibrate to min and max - whatever that is.
			// So knocking ALL overexposed pixels down causes the same dimming as if they are all overexposed.
			// The solution is to leave ONE PIXEL over-exposed, to calibrate the other pixels to being highly
			// exposed, but not over-exposed.
			// Can work on this as part of the processing...
		
		logProcessing("    Z"+(zNum+1)+": Splitting XY Image: "+xyStackSplitter.getSize());
		//logProcessing("");
		
		//Loop through splitImages in xyStackSplitter:
		
		for(int a=0; a<xyStackSplitter.getSize(); a++) {
			
			//IJ.showMessage("1239 blurredImage: "+blurredImage);
			
			blurredImage = xyStackSplitter.getNextImp(a);
			
			//logProcessing("  Processing Image: "+blurredImage.getTitle() );
			
			//IJ.showMessage("1243 blurredImage: "+blurredImage);
			
			blurredImage = deconvolve( blurredImage );
			
			//Returned image is 32-bit -> need to convert to original bit-depth, ensuring min & max are
			//within the bounds of the bit-depth (ie. dont clip the image!)
			
			//This needs to be done at the top of the methods call...
			
			//if more than one file is generated in zStackSplitter during the split, as each image is
			// processed it must be saved to disk.
			if(xyStackSplitter.getSize() > 1) {
				//save image to disk and save the file to a File ArrayList [inside zStackSplitter]
				xyStackSplitter.saveDeblurred(blurredImage);
			}
				
			//DO NOT need to delete the passed image from xyStackSplitter, as this now contains the deconvolved
			//image!
				
		}//end for a
					
			//Finally, need to reconstruct the images!
			
			//Once reconstructed, pass the image back to the preceding method - splitChannels()...
			//Return an imp!!
		
		//reconstruction only necessary if zStackSplitter split the original image:
		if(xyStackSplitter.getSize() > 1) {
			//reconstruct the saved deblurred images inside zStackSplitter
				//these will be derived from calls to saveDeblurred()
			//Make sure this method returns the reconstructed image:
			return xyStackSplitter.reconstructDeblurred();
		}
		else {
			//else, the zStackSplitter did not split the image, so just return the delburredImage:
			return blurredImage;
		}
		
	}
	
	
		
	protected ImagePlus deconvolve( ImagePlus blurredImage) {
		//Next, for each image segment (or the whole image) needs to be deconvolved.
		
		//recall inputs for Deconvolution options:
		
		//ImagePlus pathToPsf = impPSF;
		//String pathToDeblurredImage = dw.getCurrentOutputFile().getAbsolutePath();
		//String boundaryStr = "REFLEXIVE"; //available options: REFLEXIVE, PERIODIC, ZERO
		//String resizingStr = "AUTO"; // available options: AUTO, MINIMAL, NEXT_POWER_OF_TWO
		//String resizingStr = "MINIMAL";
		//String outputStr = "BYTE"; // available options: SAME_AS_SOURCE, BYTE, SHORT, FLOAT  
		//String precisionStr = "SINGLE"; //available options: SINGLE, DOUBLE
		//String thresholdStr = "-1"; //if -1, then disabled
		//String maxItersStr = "40";
		//String nOfThreadsStr = "8";
		//String showIterationsStr = "false";
		//String gammaStr = "0";
		//String filterXYStr = "1.0";
		//String filterZStr = "1.0";
		//String normalizeStr = "false";
		//String logMeanStr = "false";
		//String antiRingStr = "true";
		//String changeThreshPercentStr = "0.01";
		//String dbStr = "false";
		//String detectDivergenceStr = "true";
		
		//set processedAnImage to true -> May need to move this to splitBlurredImageXY method,
		//after confirmed the image has been completely processed.
		processedAnImage = true;
		
		//run WPL, return Deconvolved image to blurredImage:
		blurredImage = deconvolveWPL(blurredImage, impPSF, 
			pathToDeblurredImage, boundaryStr, resizingStr, "FLOAT", precisionStr, 
			thresholdStr, maxItersStr, nOfThreadsStr, showIterationsStr, gammaStr, 
			filterXYStr, filterZStr, normalizeStr, logMeanStr, antiRingStr, 
			changeThreshPercentStr, dbStr, detectDivergenceStr);
		
		return blurredImage;
		
	}
	
	/**
	 * Deconvolve the image using the WPL algorithm in Parallel Iterative Deconvolution [Piotr Wendykier &
	 * Robert Dougherty]
	 * @param BlurredImage
	 * @param impPsf
	 * @param pathToDeblurredImage
	 * @param boundaryStr
	 * @param resizingStr
	 * @param outputStr
	 * @param precisionStr
	 * @param thresholdStr
	 * @param maxItersStr
	 * @param nOfThreadsStr
	 * @param showIterationsStr
	 * @param gammaStr
	 * @param filterXYStr
	 * @param filterZStr
	 * @param normalizeStr
	 * @param logMeanStr
	 * @param antiRingStr
	 * @param changeThreshPercentStr
	 * @param dbStr
	 * @param detectDivergenceStr
	 * @return
	 */
	
    public ImagePlus deconvolveWPL(ImagePlus BlurredImage, ImagePlus impPsf, String pathToDeblurredImage, String boundaryStr, String resizingStr, String outputStr, String precisionStr, String thresholdStr, String maxItersStr, String nOfThreadsStr, String showIterationsStr, String gammaStr,
            String filterXYStr, String filterZStr, String normalizeStr, String logMeanStr, String antiRingStr, String changeThreshPercentStr, String dbStr, String detectDivergenceStr) {
        boolean showIterations, normalize, logMean, antiRing, db, detectDivergence;
        double threshold, gamma, filterXY, filterZ, changeThreshPercent;
        int maxIters;
        int nOfThreads;
        BoundaryType boundary = null;
        ResizingType resizing = null;
        OutputType output = null;
        PrecisionType precision = null;
        ImagePlus imX = null;
        ImagePlus imB = BlurredImage;
        if (imB == null) {
            IJ.error("Cannot open image " + BlurredImage);
            return null;
        }
        ImagePlus imPSF = impPsf;
        if (imPSF == null) {
        	IJ.error("Cannot open image " + impPsf);
            return null;
        }
        ImageProcessor ipB = imB.getProcessor();
        if (ipB instanceof ColorProcessor) {
        	IJ.error("RGB images are not currently supported");
            return null;
        }
        if (imB.getStackSize() == 1) {
        	IJ.error("For 2D images use Parallel Iterative Deconvolution 2D");
            return null;
        }
        ImageProcessor ipPSF = imPSF.getProcessor();
        if (ipPSF instanceof ColorProcessor) {
        	IJ.error("RGB images are not currently supported");
            return null;
        }
        if (imPSF.getStackSize() == 1) {
        	IJ.error("For 2D images use Parallel Iterative Deconvolution 2D");
            return null;
        }
        try {
            maxIters = Integer.parseInt(maxItersStr);
        } catch (Exception ex) {
        	IJ.error("maxIters must be a positive integer");
            return null;
        }
        if (maxIters < 1) {
        	IJ.error("maxIters must be a positive integer");
            return null;
        }
        for (BoundaryType elem : BoundaryType.values()) {
            if (elem.toString().equals(boundaryStr)) {
                boundary = elem;
                break;
            }
        }
        if (boundary == null) {
        	IJ.error("boundary must be in " + Arrays.toString(BoundaryType.values()));
            return null;
        }
        for (ResizingType elem : ResizingType.values()) {
            if (elem.toString().equals(resizingStr)) {
                resizing = elem;
                break;
            }
        }
        if (resizing == null) {
        	IJ.error("resizing must be in " + Arrays.toString(ResizingType.values()));
            return null;
        }
        for (OutputType elem : OutputType.values()) {
            if (elem.toString().equals(outputStr)) {
                output = elem;
                break;
            }
        }
        if (output == null) {
        	IJ.error("output must be in " + Arrays.toString(OutputType.values()));
            return null;
        }
        for (PrecisionType elem : PrecisionType.values()) {
            if (elem.toString().equals(precisionStr)) {
                precision = elem;
                break;
            }
        }
        if (precision == null) {
        	IJ.error("precision must be in " + Arrays.toString(PrecisionType.values()));
            return null;
        }
        try {
            threshold = Double.parseDouble(thresholdStr);
        } catch (Exception ex) {
        	IJ.error("threshold must be a nonnegative number or -1 to disable");
            return null;
        }
        if ((threshold != -1) && (threshold < 0)) {
        	IJ.error("threshold must be a nonnegative number or -1 to disable");
            return null;
        }
        try {
            nOfThreads = Integer.parseInt(nOfThreadsStr);
        } catch (Exception ex) {
        	IJ.error("nOfThreads must be power of 2 - no parse int");
            return null;
        }
        if (nOfThreads < 1) {
        	IJ.error("nOfThreads must be power of 2 - < 1");
            return null;
        }
        if (!ConcurrencyUtils.isPowerOf2(nOfThreads)) {
        	IJ.error("nOfThreads must be power of 2 < not Pow2");
            return null;
        }
        try {
            showIterations = Boolean.parseBoolean(showIterationsStr);
        } catch (Exception ex) {
        	IJ.error("showItrations must be a boolean value (true or false)");
            return null;
        }
        try {
            gamma = Double.parseDouble(gammaStr);
        } catch (Exception ex) {
        	IJ.error("gamma must be a nonnegative value");
            return null;
        }
        if (gamma < 0.0) {
        	IJ.error("gamma must be a nonnegative value");
            return null;
        }

        try {
            filterXY = Double.parseDouble(filterXYStr);
        } catch (Exception ex) {
        	IJ.error("filterXY must be a nonnegative value");
            return null;
        }
        if (filterXY < 0.0) {
        	IJ.error("filterXY must be a nonnegative value");
            return null;
        }

        try {
            filterZ = Double.parseDouble(filterZStr);
        } catch (Exception ex) {
        	IJ.error("filterZ must be a nonnegative value");
            return null;
        }
        if (filterZ < 0.0) {
        	IJ.error("filterZ must be a nonnegative value");
            return null;
        }
        try {
            normalize = Boolean.parseBoolean(normalizeStr);
        } catch (Exception ex) {
        	IJ.error("normalize must be a boolean value (true or false)");
            return null;
        }
        try {
            logMean = Boolean.parseBoolean(logMeanStr);
        } catch (Exception ex) {
        	IJ.error("logMean must be a boolean value (true or false)");
            return null;
        }
        try {
            antiRing = Boolean.parseBoolean(antiRingStr);
        } catch (Exception ex) {
        	IJ.error("antiRing must be a boolean value (true or false)");
            return null;
        }
        try {
            db = Boolean.parseBoolean(dbStr);
        } catch (Exception ex) {
        	IJ.error("db must be a boolean value (true or false)");
            return null;
        }
        try {
            detectDivergence = Boolean.parseBoolean(detectDivergenceStr);
        } catch (Exception ex) {
        	IJ.error("detectDivergence must be a boolean value (true or false)");
            return null;
        }
        try {
            changeThreshPercent = Double.parseDouble(changeThreshPercentStr);
        } catch (Exception ex) {
        	IJ.error("changeThreshPercent must be a nonnegative value");
            return null;
        }
        if (changeThreshPercent < 0.0) {
            IJ.error("changeThreshPercent must be a nonnegative value");
        }
        ConcurrencyUtils.setNumberOfThreads(nOfThreads);
        WPLOptions options = new WPLOptions(gamma, filterXY, filterZ, normalize, logMean, antiRing, changeThreshPercent, db, detectDivergence, (threshold == -1) ? false : true, threshold);
        switch (precision) {
        case DOUBLE:
            WPLDoubleIterativeDeconvolver3D dwpl = new WPLDoubleIterativeDeconvolver3D(imB, imPSF, boundary, resizing, output, maxIters, showIterations, options);
            imX = dwpl.deconvolve();
            break;
        case SINGLE:
            WPLFloatIterativeDeconvolver3D fwpl = new WPLFloatIterativeDeconvolver3D(imB, imPSF, boundary, resizing, output, maxIters, showIterations, options);
            imX = fwpl.deconvolve();
            break;
        }
        //dw.saveImpAndClose(imX);
        //IJ.save(imX, pathToDeblurredImage);
        imX.setTitle(BlurredImage.getTitle() );
        //look at imX:
        //imX.show();
        //IJ.showMessage("Look at imX:");
        //imX.hide();
        return imX;
    }
	
	
    /**  
     * Loads and opens a TIFF from within a JAR file using getResourceAsStream().
     * 
     * @param path
     * @param title
     * @return
     */
    public ImagePlus openTiffFromJar(String path, String title) {
           InputStream is = getClass().getResourceAsStream(path);
           System.out.println("input stream: "+is);
           if (is!=null) {
               Opener opener = new Opener();
               System.out.println("opener: "+opener);
               ImagePlus imp = opener.openTiff(is, title);
               System.out.println("imp: "+imp);
               try {
				is.close();
               	} catch (IOException e) {
               		// TODO Auto-generated catch block
               		e.printStackTrace();
               	}
               return imp;
           }
           else {
        	   return null;
           }
    }
    

    
    
    /**
     * This inner class represents objects which will split an ImagePlus hyperstack into multiple
     * z stacks - one for each channel.
     * 
     * extends SmImageSplitter
     */
    class SMChannelSplitter {
    	
    	ImagePlus processingImage;
    	int channelNumber;
    	File DIR;
    	ArrayList<File> chFileArray;
    	
    	ArrayList<File> deblurredFileArray;
    	
    	
    	/**
    	 * Constructor. Splits the channels if more than one channel.
    	 * <p>
    	 * Checks the number of channels, if more than one channel, these are split
    	 * and the first is kept open in processingImage variable, whereas others are saved to disk
    	 * and File references stored in chFileArray.  If only one channel, only processing is 
    	 * the imp is set to processingImage.
    	 * @param imp
    	 * @param DIR
    	 */
    	public SMChannelSplitter(ImagePlus imp, File DIR) {
    		    		
    		//Determine channel number:
    		channelNumber = imp.getNChannels();
    		
    		//Initiate File ArrayList:
    		chFileArray = new ArrayList<File>();
    		deblurredFileArray = new ArrayList<File>();
    		
    		//Store passed DIR to instance variable (for use in saveDeblurred() method):
    		this.DIR = DIR;
    		
    		//First, separate the image into separate channels if required - save all channels
    			// 2 - end to disk, and close Imp [keep first channel open for processing]
    		if(channelNumber > 1) {
    			
    			for(int b=0; b<channelNumber; b++) {
    				int channelNumber = b+1;
    				blurredChannel = new ImagePlus("C"+channelNumber+" "+imp.getTitle(), ChannelSplitter.getChannel(blurredImage, channelNumber) );
    				if(b == 0) {
    					//This is the First channel - store this to processingImage for immediate processing:
    					processingImage = blurredChannel;
    				}
    				if(b>0) {
    					File chFile;
    					//Here subsequent channels are passed - these should be saved and closed.
    					//FileSaver fs = new FileSaver(blurredChannel);
    					if( blurredChannel.getTitle().substring(blurredChannel.getTitle().length() - 4, blurredChannel.getTitle().length()) .equals(".tif") ) {
    						 chFile = new File(DIR.getAbsolutePath() + File.separator + blurredChannel.getTitle() );
    					}
    					else {
    						chFile = new File(DIR.getAbsolutePath() + File.separator + blurredChannel.getTitle() + ".tif" );
    					}
    					IJ.saveAsTiff(blurredChannel, chFile.getAbsolutePath() );
    					//fs.saveAsTiff(tempDir.getAbsolutePath() + File.separator + blurredChannel.getTitle() +".tif" );
    					chFileArray.add(chFile);
    				}
    			}//end for b
    		}//end if ch>1
    		
    		else if(channelNumber == 1) {
    			//if ch is 1, need to simply copy blurredImage into processingImage
    			processingImage = imp;
    		}
    		
    	} //end constructor
    	
    	
    	/**
    	 * This method reconstructs the processed images to re-form the original image.
    	 * @return
    	 */
    	public ImagePlus reconstructDeblurred() {
			// This method needs to reconstruct the channels from the deblurredFileArray.  The order in which
    		// the images are saved to deblurredFileArray is the order the channels must be reconstructed.
    		
    		//open images from deblurredFileArray, and store ImagePlus obhjects to array:
    		
    		ImagePlus[] imps = new ImagePlus[deblurredFileArray.size()];
    		
    		for(int a=0; a<deblurredFileArray.size(); a++) {
        		imps[a] = IJ.openImage( deblurredFileArray.get(a).getAbsolutePath() );    			
    		}
    		
    		//now need to reconstruct the channels:
    			//Use RGBStackMerge!
    		ImagePlus imp = RGBStackMerge.mergeChannels(imps, false);
    		// this may SHOW the imp after reconstruction 
    			//This is not desired, may need to move methods locally to remove the show command
    				// [line 207 in RGBStackMerge]
    		
    		//loop through deblurredFileArray and delete all images:
    		for(int a=0; a<deblurredFileArray.size(); a++) {
        		imps[a] = null; //set imps array to null.
        		deblurredFileArray.get(a).delete(); //and delete File representation of deblurred images
    		}
    		
			return imp;
		}

    	/**
    	 * This method saves the deblurred image to TEMP file in HD -> DIR.
    	 * @param deblurredImage
    	 */
		public void saveDeblurred(ImagePlus deblurredImage) {
			// This method should save the deblurred image to disk, and store a File representation
				// of this file into an ArrayList<File> object.
			
    		if(channelNumber > 1) {
    			
    			//if deblurredFileArray is null, need to initialise:
				if(deblurredFileArray == null) {
					deblurredFileArray = new ArrayList<File>();
				}
    			
    				File chFile;
    				//Here subsequent channels are passed - these should be saved and closed.
    				//FileSaver fs = new FileSaver(blurredChannel);
    				if( deblurredImage.getTitle().substring(deblurredImage.getTitle().length() - 4, deblurredImage.getTitle().length()) .equals(".tif") ) {
    					 chFile = new File(DIR.getAbsolutePath() + File.separator + deblurredImage.getTitle() );
    				}
    				else {
    					chFile = new File(DIR.getAbsolutePath() + File.separator + deblurredImage.getTitle() + ".tif" );
    				}
    				IJ.saveAsTiff(deblurredImage, chFile.getAbsolutePath() );
    				//fs.saveAsTiff(tempDir.getAbsolutePath() + File.separator + blurredChannel.getTitle() +".tif" );
    				//close the current imp:
					deblurredImage.close();
					//add to file array:
    				deblurredFileArray.add(chFile);
    		
    		}//end if ch>1
			
		}


		public int getChannelNumber() {
    		return channelNumber;
    	}
    	
    	
    }
    



	/**
		 * This inner class represents objects which indicate how to split up a z stack into a series of
		 * evenly spaced z stacks.  This class does nothing but represent the start and end slice vlaues
		 * where a Z stack should be split.  The computations to determine this are 
		 * @author stevenwest
		 *
		 */
		class ZStackSplitter {
			
			ImagePlus imp;
			int[] startSlice;
			int[] endSlice;
			int size;
			
			int zStackInt;
			int maxZstackSize;
			
			ArrayList<ImagePlus> subStacks;
			ArrayList<File> zFileArray;
			ArrayList<File> deblurredFileArray;
			File DIR;
			
			boolean imageSplitToSubstacks;
			
			
			/**
			 * This constructor will determine whether a zStack of zStackSize needs to be split into subStacks.  This
			 * will happen if zStackSize is greater than maxZstackSize.  In this case, the ZStackSplitter object will
			 * determine how many substacks to form from the original stack.  Other methods in this class will perform
			 * the computations necessary to split the image up into substacks.
			 * @param zStackSize
			 * @param maxZstackSize
			 */
			public ZStackSplitter(ImagePlus imp, int maxZstackSize, File DIR) {
				
				boolean splitToSubstacks = true;
				
				this.imp = imp;
				zStackInt = imp.getNSlices();
				this.maxZstackSize = maxZstackSize;
				this.DIR = DIR;
				
				deblurredFileArray = new ArrayList<File>();
				
				double divider = (double) zStackInt / maxZstackSize;
				
				//divider equals how many new zStacks will be produced, and zStackInt2 represents how many slices [max]
				//will be in any given zStack.
				
				//Next, need to calculate how to split up the zStack by determining the start and end slices for each
				//new zStack...
				
				int dividerInt = (int)Math.ceil(divider);
				
				//Note, there is no efficiency gains by splitting the z stack to different slices - so just split it
				//minimally - i.e. split it into dividerInt Z Stacks.
					//There is a LINEAR gain in zStack size to deconvolution efficiency, but this is to be expected.
							
				startSlice = new int[dividerInt];
				endSlice = new int[dividerInt];
				size = dividerInt;
				
				//This number represents the number of slices per subStack - it need to be rounded for each substack,
				//which is performed in the for loop below:
				double subStackInt = (double)zStackInt / dividerInt;
				int startInt = 0;  //start slice for given substack
				int endInt;  //end slice for given substack.
				
				//This for loop adds the start and end values for splitting the Z stack into approximately
					//equal pieces.  These values are added to the startSlice and endSlice Int arrays.
				for(int a=0; a<dividerInt; a++) {
					addStartValue(startInt+1, a);
					endInt =  (int) Math.round( (subStackInt*(a+1)) );
					addEndValue(endInt, a);
					startInt=endInt;
				}
				
				//finally, split to substacks if splitToSubstacks is true [which should always be the case?!]
				if(splitToSubstacks == true) {
					splitImageToSubStacks();
				}	
				
			}
			
			public ImagePlus reconstructDeblurred() {
				// This method needs to reconstruct the zStack from the deblurredFileArray.  The order in which
	    		// the images are saved to deblurredFileArray is the order the channels must be reconstructed.
	    		
	    		//open images from deblurredFileArray, and store ImagePlus objects to array:
	    		
	    		ImagePlus[] imps = new ImagePlus[deblurredFileArray.size()];
	    		
	    		for(int a=0; a<deblurredFileArray.size(); a++) {
	        		imps[a] = IJ.openImage( deblurredFileArray.get(a).getAbsolutePath() );    			
	    		}
	    		
	    		//now need to reconstruct the channels:
	    			//Use Concatenator!
	    		Concatenator concat = new Concatenator();
	    		imp = concat.concatenateHyperstacks(imps, imp.getTitle(),  false);
	    		// this may SHOW the imp after reconstruction 
	    			//This is not desired, may need to move methods locally to remove the show command
	    				// [line 134 in Concatenator]
	    		
	    		//now the currentImp contains the deblurred image, can delete the file representations of these
	    		//from TEMP DIR - the list of these is inside deblurredFileArray:
	    		
	    		//loop through deblurredFileArray and delete all images:
	    		for(int a=0; a<deblurredFileArray.size(); a++) {
	        		imps[a] = null; //set imps array to null.
	        		deblurredFileArray.get(a).delete(); //and delete File representation of deblurred images
	    		}
	    		
	    		
				return imp;
			}

			public void saveDeblurred(ImagePlus deblurredImage) {
				
				// only save if more than one image!
				if( getSize() > 1) {
					
					//if deblurredFileArray is null, need to initialise:
					if(deblurredFileArray == null) {
						deblurredFileArray = new ArrayList<File>();
					}

					File zFile;
						
						//create appropriate imp title:
						if( deblurredImage.getTitle().substring(deblurredImage.getTitle().length() - 4, deblurredImage.getTitle().length()).equals(".tif") ) {
							zFile = new File(DIR + File.separator + deblurredImage.getTitle() );
						}
						else {
							zFile = new File(DIR + File.separator + deblurredImage.getTitle() + ".tif" );
						}
						//save Imp:
						IJ.saveAsTiff(deblurredImage, zFile.getAbsolutePath() );
						//close the current imp:
						deblurredImage.close();
						//save the zFile to the `FileArrayList':
						deblurredFileArray.add(zFile);

				}
				
			}
			
			
			

			/**
			 * This method will split the image to substacks.
			 */
			public void splitImageToSubStacks() {
				
				
				
				//split the image up into substacks according to the zStackSplitter:
				 subStacks = new ArrayList<ImagePlus>();			
				String zString;
				
				//need to organise channels and frames handling!
					//this is handled in the dialog and is dependent for processing to commence...
	
				//loop through zStackSplitter number, to split the zStack into substacks...
					//Note, this loop only goes up to zStacksplitter size -2! the final stack cannot
					//be extracted by makeSubstack() as it throws an exception to try to remove ALL
					//images from a stack into a new stack.
					//And actually this is not required - as the final stack IS the remaining stack
					//as images are deleted in the original stack as substacks are removed..
				
				//IJ.showMessage("Z size: "+getSize() );
	
				//for(int a=0; a<getSize()-1; a++) {
				for(int a=getSize()-2; a>=0; a--) {
				
					//first set the zString value - which is the slices from startVal to endVal in the
					//zStackSplitter at ref a:
					if(getStartValue(a) != getEndValue(a) ) {
						zString = "" + getStartValue(a) + "-" + getEndValue(a);
					}
					else {
						//in case the start and end values are the same, just set zString to one slice, which
						//is the value in either the start or end value:
						zString = "" + getStartValue(a);
					}
					
					//IJ.log("zString: "+zString+" loop a: "+a);
					
					//Set the ArrayList with the substack:
					subStacks.add(makeSubstack( imp, zString, true ) );
					//Note, with hyperstack this duplicates the entire image stack - need to make sure there is 
					//sufficient memory for this operation!
						//Can circumvent this by actually saving substacks as they are made, or by extracting the slices from
						//the original zStack..?
					// YES - This is achieved through the makeSubstack method and its auxillary methods derived from the
						//subStackMaker class in ImageJ, copied to end of this class to gain access to the 'delete' boolean 
						//private variable.
					
				} //end for a
				
				//finally, add the blurredImage - which has now had all of the remaining substacks removed from it.
					//This is the final subStack to add to the subStacks array.
				imp.setTitle("Zend "+imp.getTitle() );  //set title so it doesnt conflict with channel when saving..
				subStacks.add( imp);
				
				imageSplitToSubstacks = true;
				
			}
			
			
			public void saveSubStack() {
				zFileArray = new ArrayList<File>();
				File zFile;
				
				//saveSubStack() should only save substacks if more than one exists in the subStacks array:
					// Currently, the code will always run splitImageToSubstacks(), and if no splitting in Z is
					// required, the 'subStacks' ArrayList will just have the original stack within it.
					// Saving of subastacks is only necessary if the original stack was split into more than
					// one image, and then only the images ABOVE the first one need saving.
					// Therefore, this method should only save images above the first index in subStacks.
				
				//ONLY save subStacks if subStacks arrayList does NOT equal NULL!
					//i.e that imageSplitToSubstacks is equal to true!
				//ALSO only run this code if subStacks size is above 1!
				if(imageSplitToSubstacks == true && subStacks.size() > 1) {
					
				//Start loop at 1, as only imps above the first imp in substacks should be saved.				
				for(int a=1; a<subStacks.size(); a++) {
					
					//create appropriate imp title:
					if( subStacks.get(a).getTitle().substring(subStacks.get(a).getTitle().length() - 4, subStacks.get(a).getTitle().length()).equals(".tif") ) {
						zFile = new File(tempDir + File.separator + subStacks.get(a).getTitle() );
					}
					else {
						zFile = new File(tempDir + File.separator + subStacks.get(a).getTitle() + ".tif" );
					}
					//save Imp:
					IJ.saveAsTiff(subStacks.get(a), zFile.getAbsolutePath() );
					//close the current imp:
					subStacks.get(a).close();
					//save the zFile to the `FileArrayList':
					zFileArray.add(zFile);
					
				}//end for a
				
				}//end if subStacks > 1
				else {
					//if SubStack size is 1, means the zStack was not big enough to split up.
						//In this case, just save the imp that is open!
					
					//In this case, just save the original image:
					
					if( imp.getTitle().substring(imp.getTitle().length() - 4, imp.getTitle().length()).equals(".tif") ) {
						zFile = new File(tempDir + File.separator + imp.getTitle() );
					}
					else {
						zFile = new File(tempDir + File.separator + imp.getTitle() + ".tif" );
					}
					//save Imp:
					IJ.saveAsTiff(imp, zFile.getAbsolutePath() );
					//close imp:
					imp.close();
					//save the zFile to the `FileArrayList:
					zFileArray.add(zFile);
					
					
				}
				
			}//end saveSubStacks()
			
			/**
			 * Add a value to the startSlice int array, at the appropriate index.
			 * @param value
			 * @param index
			 */
			public void addStartValue(int value, int index) {
				startSlice[index] = value;
			}
			
			public int getStartValue(int index) {
				return startSlice[index];
			}
			
			public int getEndValue(int index) {
				return endSlice[index];
			}
			
			/**
			 * Add a value to the endSlice int array, at the appropriate index.
			 * @param value
			 * @param index
			 */
			public void addEndValue(int value, int index) {
				endSlice[index] = value;
			}
			
			/**
			 * Get the size of the array of ints.
			 * @return
			 */
			public int getSize() {
				return size;
			}
			
			
		/**
		 * This method needs to retrieve the imp at the given index.  If it is 0, then return
		 * the first imp referred to in subStacks (as this was not saved to disk, and is still in
		 * ImageJ!  If it is above 0, need to open the imp, and then return it.
		 * @param index
		 * @return
		 */
		public ImagePlus getNextImp(int index) {
			//First, check if the index is equal to 0:
			if(index == 0 ) {
				//if index is 0, return the imp in subStacks at index 0:
				return subStacks.get(index);
			}
			else {
				//if index is greater than one, then need to open the image from the file ref in zFileArray:
				//save to the blurredImage ImagePlus:
				return IJ.openImage( zFileArray.get(index-1).getAbsolutePath() );
			}
		}
		
		
		
		/**
		 * Method to split zStack up into subStacks.
		 * @param imp
		 * @param range
		 * @param delete
		 * @return
		 */
		public ImagePlus makeSubstack(ImagePlus imp, String range, boolean delete) {
			String stackTitle = "Z "+range+ " " + imp.getTitle();
			if (stackTitle.length()>25) {
				int idxA = stackTitle.indexOf(",",18);
				int idxB = stackTitle.lastIndexOf(",");
				if(idxA>=1 && idxB>=1){
					String strA = stackTitle.substring(0,idxA);
					String strB = stackTitle.substring(idxB+1);
					stackTitle = strA + ", ... " + strB;
				}
			}
			ImagePlus imp2 = null;
			try {
				int idx1 = range.indexOf("-");
				if (idx1>=1) {									// input displayed in range
					String rngStart = range.substring(0, idx1);
					String rngEnd = range.substring(idx1+1);
					Integer obj = new Integer(rngStart);
					int first = obj.intValue();
					int inc = 1;
					int idx2 = rngEnd.indexOf("-");
					if (idx2>=1) {
						String rngEndAndInc = rngEnd;
						rngEnd = rngEndAndInc.substring(0, idx2);
						String rngInc = rngEndAndInc.substring(idx2+1);
						obj = new Integer(rngInc);
						inc = obj.intValue();
					}
					obj = new Integer(rngEnd);
					int last = obj.intValue();
					imp2 = stackRange(imp, first, last, inc, stackTitle, delete);
				} else {
					int count = 1; // count # of slices to extract
					for (int j=0; j<range.length(); j++) {
						char ch = Character.toLowerCase(range.charAt(j));
						if (ch==',') {count += 1;}
					}
					int[] numList = new int[count];
					for(int i=0; i<count; i++) {
						int idx2 = range.indexOf(",");
						if(idx2>0) {
							String num = range.substring(0,idx2);
							Integer obj = new Integer(num);
							numList[i] = obj.intValue();
							range = range.substring(idx2+1);
						}
						else{
							String num = range;
							Integer obj = new Integer(num);
							numList[i] = obj.intValue();
						}
					}
					imp2 = stackList(imp, count, numList, stackTitle, delete);
				}
			} catch (Exception e) {
				IJ.error("Substack Maker", "Invalid input string:        \n \n  \""+range+"\"");
			}
			return imp2;
		}
	
		// extract specific slices
		ImagePlus stackList(ImagePlus imp, int count, int[] numList, String stackTitle, boolean delete) throws Exception {
			ImageStack stack = imp.getStack();
			ImageStack stack2 = null;
			Roi roi = imp.getRoi();
			for (int i=0, j=0; i<count; i++) {
				int currSlice = numList[i]-j;
				ImageProcessor ip2 = stack.getProcessor(currSlice);
				ip2.setRoi(roi);
				ip2 = ip2.crop();
				if (stack2==null)
					stack2 = new ImageStack(ip2.getWidth(), ip2.getHeight());
				stack2.addSlice(stack.getSliceLabel(currSlice), ip2);
				if (delete) {
					stack.deleteSlice(currSlice);
					j++;
				}
			}
			if (delete) {
				imp.setStack(stack);
				// next three lines for updating the scroll bar
				ImageWindow win = imp.getWindow();
				StackWindow swin = (StackWindow) win;
				if (swin!=null)
					swin.updateSliceSelector();
			}
			ImagePlus impSubstack = imp.createImagePlus();
			impSubstack.setStack(stackTitle, stack2);
			return impSubstack;
		}
	
		// extract range of slices
		ImagePlus stackRange(ImagePlus imp, int first, int last, int inc, String title, boolean delete) throws Exception {
			ImageStack stack = imp.getStack();
			ImageStack stack2 = null;
			Roi roi = imp.getRoi();
			for (int i= first, j=0; i<= last; i+=inc) {
				// IJ.log(first+" "+last+" "+inc+" "+i);
				int currSlice = i-j;
				ImageProcessor ip2 = stack.getProcessor(currSlice);
				ip2.setRoi(roi);
				ip2 = ip2.crop();
				if (stack2==null)
					stack2 = new ImageStack(ip2.getWidth(), ip2.getHeight());
				stack2.addSlice(stack.getSliceLabel(currSlice), ip2);
				if (delete) {
					stack.deleteSlice(currSlice);
					j++;
				}
			}
			if (delete) {
				imp.setStack(stack);
				// next three lines for updating the scroll bar
				ImageWindow win = imp.getWindow();
				StackWindow swin = (StackWindow) win;
				if (swin!=null)
					swin.updateSliceSelector();
			}
			ImagePlus substack = imp.createImagePlus();
			substack.setStack(title, stack2);
			substack.setCalibration(imp.getCalibration());
			return substack;
		}
		
		
		
	 } //end inner class ZStackSplitter
		
		
	/**
	 * This inner class splits up a given Z Stack into appropriate size stacks for EFFICIENT DECONVOLUTION
	 * by the WPL algorithm.  The WPL algorithm has a number of options for RESIZING:
	 * <p>
	 * The Resizing combo-box allows you to specify how the blurred image will be padded 
	 * before processing. The Minimal resizing means that the pixel data in each dimension 
	 * of a blurred image are padded by the size of the corresponding dimension of a PSF 
	 * image. If the Next power of two option is selected, then the pixel data in each 
	 * dimension of a blurred image are padded to the next power-of-two size that is 
	 * greater or equal to the size of an image obtained by minimal padding. Finally, 
	 * the Auto option chooses between the two other options to maximize the performance.
	 * String resizingStr = "AUTO"; // available options: AUTO, MINIMAL, NEXT_POWER_OF_TWO
	 * <p>
	 * An experiment to determine the efficiency of these different options has been conducted in
	 * experiment 465-02, and the following empirical observations have been made:
	 * 
	 * - AUTO is much more efficient that MINIMAL or NEXT_POWER_OF_TWO at any image size.
	 * 
	 * - AUTO Resizing efficiency shows a complex pattern across image sizes consisting of:
	 * 		- Image sizes where the efficiency is sporadic and largely inefficient.
	 * 		- Image sizes where the efficiency is very high, and slows increases as image size
	 * 			increases.
	 * 
	 * - Analysing AUTO Resizing efficiency shows consistency across multiple platforms and computers.
	 * 
	 * 
	 * Using a 16GB RAM computer, a large array of image sizes could be tested before the working memory
	 * was saturated.  This revealed the following pattern of high efficiency:
	 * 
	 * [High efficiency was considered when it took less than 0.001 SEC PER PIXEL]
	 * 
	 * - 270 to 380 w/h images showed high efficiency.
	 * 
	 * - 540 to 920 w/h images showed high efficiency.
	 * 
	 * - 1240 to 1920 w/h images showed high efficiency.
	 * 
	 * 
	 * The first step in determining image sizes is determining the MAXIMUM image size.  This is based
	 * on the available memory in ImageJ, and the corresponding memory usage for a given size image.
	 * 
	 *  - Again, this had to be determined empirically, as the relation between image size & deconvolution
	 *  failure is not predictable or linear in nature.
	 *  
	 *  - A series of points relating available memory to the maximum image size have been determined,
	 *  and from these data extrapolations can be made to determine how large an image a given quantity
	 *  of memory can handle for deconvolution.
	 *  
	 *  
	 *  After constraining the deconvolution based on the max image size, the problem can be broken down
	 *  to maximise efficiency in deconvolving the image by dividing the image into equal sizes which 
	 *  are most efficient AND within the maximum image size the current memory allocation can handle.
	 * 
	 * Empirical determinations of deconvolution efficiency and memory requirements, already reviewed here,
	 * can be found in detail in Exp 465-02.
	 * 
	 * 
	 */
	class XYStackSplitter {
		
		long maxMemory;
		int maxXY;
		
		File DIR;
		boolean currentImpOpen;
		
		ImagePlus currentImp;
		ArrayList<ImagePlus> splitImps;
		ImagePlus currentSplitImp;
		
		int impWidth;
		int impHeight;
		int impDepth;
		int impBitDepth;
		String impTitle;
		
		MemToLength memToLength;
		
		EfficientLengths efficientLengths;
		
		Document docWplAuto;
		
		CropPoints widthCropPoints;
		CropPoints heightCropPoints;
		
		int size;
		int width;
		int height;
		
		ArrayList<File> xyFileArray;
		ArrayList<File> deblurredFileArray;
		
		/**
		 * Constructor for XYStacksplitter.  This applies the two filters:
		 * 
		 * - Max Image Size filter -> determines if the image MUST be split.
		 * 
		 * - High Efficiency Filter -> Determines if deconvolution can be faster if the image
		 *    is split.
		 *    
		 *  
		 * 
		 * @param imp
		 */
		public XYStackSplitter(ImagePlus imp, File DIR) {
			
			//store reference to imp:
			this.currentImp = imp;
			currentImpOpen = true;
			
			//get attirbutes of imp, for reconstruction:
			impWidth = imp.getWidth();
			impHeight = imp.getHeight();
			impDepth = imp.getNSlices();
			impBitDepth = imp.getBitDepth();
			impTitle = imp.getTitle();
			
			this.DIR = DIR;
			
			//Max Image Size Filter:
			
			//determine max memory:
			maxMemory = IJ.maxMemory();
			
			//IJ.showMessage("maxMemory: "+maxMemory );
			
			//load the lookup file for maxMem to maxXY conversion:
				// The WPL AUTO Stats.xml file contains the data converting from available memory in bytes
				// to maximum image lengths in XY [under tags "values"].
				// It also contains the data of which lengths during deconvolution permit the most
				// EFFICIENT PROCESSING via the WPL AUTO algorithm [under tag "efficientLengths"]
			docWplAuto = StereoMateSettings.buildDom("WPL AUTO Stats.xml");
			
			//determine the max XY image dimensions:
			maxXY = getMaxXY(maxMemory, imp.getNSlices());
			
			//High Efficiency Filter:
			
			//Here, need to determine a scheme for dividing the image in XY which will result in
			//an efficient processing of the image.
				//This is determined first by looking up the XY values where efficient deconvolution
				//takes place:
			
			//First, create an EfficientLengths object, to store the start and end XY lengths for
				//efficient WPL Auto Deconvolution:
			 efficientLengths = new EfficientLengths();
			
			//And fill this efficientLengths object with the correct values from the XML file:
			 //This is stored in the docWplAuto XML file
			 //Since this was already loaded above, call a method to retrieve the data inside it:
		   getEfficientLengths(efficientLengths);	
			
			
			//Next, a scheme for deconvolving the current image based on the maxXY and the efficient XY values
			//for deconvolution must be determined:
		   
		   	//First, integrate the maxXY value into the efficientLengths object:
		   
		   efficientLengths.integrateMaxVal(maxXY);
		   
		   //Now, efficientLengths contains values for efficient processing WITHIN THE CONSTRAINTS OF THE
		   //CURRENTLY AVAILABLE MEMORY
		   
		   //Now, this data structure can be used to determine how to divide up the image for efficient
		   //processing within the constraints of the current memory:
		   
		   //Two lengths need to be parsed - the width and the height.
		   //If we assume that efficient deconvolution occurs with any rectangle which consists of
		   //lengths which are efficient (as the lengths have only been tested as perfect square images)
		   //we can parse the width and length separately to determine a length to use, and how many
		   //of those to use:
		   
		   //First, retrieve the width and length of the image:
		   
		   int widthImp = imp.getWidth();
		   int heightImp = imp.getHeight();
		   
		   //Now, parse each to the parseLength() method of the efficientLengths class:
		   
		   int width = efficientLengths.parseLength(widthImp);
		   int height = efficientLengths.parseLength(heightImp);
		   
		   //IJ.showMessage("img w/h: "+widthImp+" "+heightImp+" effLengths w/h: "+width+" "+height);
		   
		   // Now [Hopefully!] width and height equal efficient values for deconvolution image
		   // lengths.
		   // These values need to be used to divide the Image width and heights up into two
		   // arrays
		   
		   		// Note:  Adjusted the "WPL Auto Stats XML" file now to:
		   			// LOWER the lower limit from 270 to 190 - this will cover any numbers between 380 &
		   			// 540.
		   			// ADDED extra entries between 380 & 540: 
		   				// 460 - 500:  This is actually quite efficient processing, and below the values of
		   				// 230 - 250.
		   				// 400 - 430: 210 has a large spike in it, which is now included in the lower limit.
		   				// The only way to reach this other than an image being 210x210 is it being 420x420.
		   				// But, 420 is more efficient than 210!  So included relativley efficient values around
		   				// 420 into the XML file.
		   
		   
		   // The width and height variables are the lengths which the widthImp and heightImp values need to be
		   // divided by - yet, due to rounding errors the actual lengths of given images may be a little
		   // different to width and height.
		   
		   		// Note: The rounding errors are ALWAYS rounding the divided value down [in parseLengthArray()
		   		// method in EfficientLengths inner class], which means determining how many widths/heights
		   		// fit into widthImp/heightImp can be achieved with simple division and storing to an int
		   		// [as that values will either be an exact int, or a little above, and rounded to the
		   		// corretn int automatically]
		   
		   // The next step is to divide the widthImp and heightImp values up appropriately using width and
		   // height.
		   // Two linked arrays can be used to store this information, which include the start and end
		   // points for cropping an image.
		   
		   // To create each Array [can use a similar structure as EfficientLengths], call a new method:
		   
		   widthCropPoints = determineImageDivision(widthImp, width);
		   heightCropPoints = determineImageDivision(heightImp, height);
		   
		   //IJ.log("WIDTH CROP POINTS:");
		   //for(int a=0; a<widthCropPoints.getLength(); a++) {
			 //  IJ.log(" START:"+widthCropPoints.getStart(a));
			  // IJ.log(" END:"+widthCropPoints.getEnd(a));
		 //  }
		   
		   //IJ.log("HEIGHT CROP POINTS:");
		   //for(int a=0; a<heightCropPoints.getLength(); a++) {
			 //  IJ.log(" START:"+heightCropPoints.getStart(a));
			 //  IJ.log(" END:"+heightCropPoints.getEnd(a));
		  // }
		   
		   // Now the widthCropPoints and heightCropPoints should contain the arrays to divide the image.
		   
		   //All the is left to do is to divide the image.
		   		// Note, if either are null NO CROPPING IS REQUIRED IN THAT PLANE!
		   
		   //Need to keep track of the cropping - this should be done inside the splitImages() method.
			
		   // To crop the image, need to set up two nested for loops -> to be done in a separate method!
		   	// The XYStackSplitter object is now generated!
		   
		   //Test to see if the CropPoint objects return the correct values:
		   	//Note, no CropPoint object will EVER be null!!  If length equals image length, the CropPoints
		   	//object is filled with 0 and [length].
		   		//see determineImageDivision() method.
		   
			
		} //end XYStackSplitter constructor.
		
		
		
		public ImagePlus reconstructDeblurred() {
			
			// This method needs to reconstruct the zStack from the deblurredFileArray.  The order in which
    		// the images are saved to deblurredFileArray is the order the channels must be reconstructed.
    		
    		//open images from deblurredFileArray, and store ImagePlus objects to array:
    		
    		ImagePlus[] imps = new ImagePlus[deblurredFileArray.size()];
    		
    		for(int a=0; a<deblurredFileArray.size(); a++) {
        		imps[a] = IJ.openImage( deblurredFileArray.get(a).getAbsolutePath() );   
        		//IJ.showMessage("Filling imps at: "+a+"imps obj: "+imps[a]);
    		}
    		
    		//now need to reconstruct the channels:
    			//Cannt use currentImp!  it was closed to save memory after saving split imps!
    		
    		//Instead, use attributes of currentImp, which are saved in construction, to recreate a blank
    		// imp of correct size and bit depth:
    		
    		currentImp = IJ.createImage(impTitle, impWidth, impHeight, impDepth, imps[0].getBitDepth() );
    		//This should be 32-bit, as returned images are 32-bit from deconvolution...
    		
    		//Loop through width and height numbers:
    		for(int a=0; a<width; a++) {
    			for(int b=0; b<height; b++) {
    				int index = (a*height)+b; //calculate index of imps to put into currentImp:
    				//set roi on currentImp based on current index:
    				//IJ.showMessage("index: "+index);
    				currentImp.setRoi( widthCropPoints.getStart(a), heightCropPoints.getStart(b),
				  					   (widthCropPoints.getEnd(a)-widthCropPoints.getStart(a)), 
				  					   (heightCropPoints.getEnd(b)-heightCropPoints.getStart(b)) );
    				//loop through each slice and insert into currentImp:
    				for(int c=1; c<=currentImp.getNSlices(); c++) {
    					currentImp.setZ(c);
    					imps[index].setZ(c);
    					currentImp.getProcessor().insert(imps[index].getProcessor(), 
    							widthCropPoints.getStart(a), heightCropPoints.getStart(b) );
    				}
    				
    			}
    		}
    		
    		//now the currentImp contains the deblurred image, can delete the file representations of these
    		//from TEMP DIR - the list of these is inside deblurredFileArray:
    		
    		//loop through deblurredFileArray and delete all images:
    		for(int a=0; a<deblurredFileArray.size(); a++) {
        		imps[a] = null; //set imps array to null.
        		deblurredFileArray.get(a).delete(); //and delete File representation of deblurred images
    		}
    		
    		//finally, set ROI to null on current imp:
    		Roi roi = null;
    		currentImp.setRoi(roi);
    		
    		//and return currentImp:
			return currentImp;
			
		}



		public void saveDeblurred(ImagePlus deblurredImage) {
			// TODO Auto-generated method stub
			
			if( getSize() > 1) {

				File xyFile;
				
				//if deblurredFileArray is null, need to initialise:
				if(deblurredFileArray == null) {
					deblurredFileArray = new ArrayList<File>();
				}	
				
					//create appropriate imp title:
					if( deblurredImage.getTitle().substring(deblurredImage.getTitle().length() - 4, deblurredImage.getTitle().length()).equals(".tif") ) {
						xyFile = new File(DIR + File.separator + deblurredImage.getTitle() );
					}
					else {
						xyFile = new File(DIR + File.separator + deblurredImage.getTitle() + ".tif" );
					}
					//save Imp:
					IJ.saveAsTiff(deblurredImage, xyFile.getAbsolutePath() );
					//close the current imp:
					deblurredImage.close();
					//save the zFile to the `FileArrayList':
					deblurredFileArray.add(xyFile);

			}
			
			
		}



		public ImagePlus getNextImp(int index) {
			//First, check if the index is equal to 0:
			if(index == 0 ) {
				//if index is 0, return the imp in subStacks at index 0:
				return currentSplitImp;
			}
			else {
				//if index is greater than one, then need to open the image from the file ref in xyFileArray:
				//save to the blurredImage ImagePlus:
				return IJ.openImage( xyFileArray.get(index-1).getAbsolutePath() );
			}
		}



		public int getSize() {
			// TODO Auto-generated method stub
			return size;
		}



		public void splitImage() {
			// This method needs to split the image based on the processing performed in the constructor.
			// currentImp in the XYStackSplitter needs to be split based on data inside the 
				//widthCropPoints & heightCropPoints objects.
			
			// initialise size variable:
			size = 0;
			
			//construct a Duplicator object outside the nested loops, for duplicating:
			Duplicator duplicator = new Duplicator();
			
			splitImps = new ArrayList<ImagePlus>();
			
			width = widthCropPoints.getLength();
			height = heightCropPoints.getLength();
			
			//use nested loops:
			 for(int a=0; a<widthCropPoints.getLength(); a++) {
				  for(int b=0; b<heightCropPoints.getLength(); b++) {
					  // IJ.showMessage("Width start: " + widthCropPoints.start.get(a) + " Height start:" +
						 //  heightCropPoints.start.get(b) + "");
					  //IJ.showMessage("Width end: " + widthCropPoints.end.get(a) + " Height end:" +
						//   heightCropPoints.end.get(b) + "");
					  
					  //add one to siz, to count the number of images generated:
					  size = size + 1;
					  
					  //Two steps:  Set ROI on imp, and then run a Duplicator:
					  
					  currentImp.setRoi( widthCropPoints.getStart(a), heightCropPoints.getStart(b),
							  			(widthCropPoints.getEnd(a)-widthCropPoints.getStart(a)), 
							  			(heightCropPoints.getEnd(b)-heightCropPoints.getStart(b)) );
					  
					  //save the output to the splitImps array:
					  splitImps.add( duplicator.run(currentImp) );
					  	//need to save these images prior to processing -> do this in saveSplitImps()
					  
			   		} //end b
			  } //end a		
			 
			 
			 //Finally, close the currentImp imp:
			 	//This currentImp should be saved so it can be retrieved during reconstruction...
			 currentImp.changes = false;
			 currentImp.close();
			 currentImpOpen = false;
			
			
		}
		
		
		/**
		 * Save the splitImps array appropriately -> keep the first imp, and save the rest to the HD,
		 * and store File references in xyFileArray.
		 */
		public void saveSplitImps() {
			
			//initialise xyFileArray:
			xyFileArray = new ArrayList<File>();
			
			File xyFile;
			
			//loop through the splitImps array:
			
			for(int a=0; a<splitImps.size(); a++) {
				
				if(a == 0) {
					
					//if a is 0, just set the imp to currentSplitImp:
					currentSplitImp = splitImps.get(a);
				
				}  //end if
				
				else {
					//else, save the imp to DIR [temp file], and close it in ImageJ:
						//The file names should be unique?!
					
					//create appropriate imp title:
					if( splitImps.get(a).getTitle().substring(splitImps.get(a).getTitle().length() - 4, splitImps.get(a).getTitle().length()).equals(".tif") ) {
						xyFile = new File(tempDir + File.separator + a + splitImps.get(a).getTitle() );
					}
					else {
						xyFile = new File(tempDir + File.separator + a + splitImps.get(a).getTitle() + ".tif" );
					}
					//save Imp:
					IJ.saveAsTiff(splitImps.get(a), xyFile.getAbsolutePath() );
					//close the current imp:
					splitImps.get(a).close();
					//save the zFile to the `FileArrayList':
					xyFileArray.add(xyFile);
					
				} //end else
				
			} //end for a
			
		} //end saveSplitImps()



		/**
		 * Convert the max Memory to an XY length, based on the image z stack thickness and
		 * the ImageJ available max memory.
		 * @param maxMem long to represent max memory in bytes.
		 * @param zSlices int to represent the number of slices in the imp.
		 * @return
		 */
		public int getMaxXY(long maxMem, int zSlices) {
			
			//int variable to represent the XY length that can be handled [according to maxMem]
			int xy = 0;
			
			String z = "";
			
			//load xyLength data based on zSlices value:
			
				//Note, the xyLength cannot be bigger than 55!  As the zStackSplitter object will ensure this..
			if(zSlices >50) {
				z = ""+55;
			}
			else if(zSlices >40) {
				z = ""+50;
			}
			else if(zSlices >30) {
				z = ""+40;
			}
			else if(zSlices >20) {
				z = ""+30;
			}
			else if(zSlices >10) {
				z = ""+20;
			}
			else if(zSlices >0) {
				z = ""+10;
			}
			
			try {
				docWplAuto.getDocumentElement().normalize();
				////IJ.showMessage("Root Element: "+ doc.getDocumentElement().getNodeName() );
			
				NodeList nList2;
				String zDepthStr;
				Element zDepthElement = null;
				
				long memory;
				int length;
				
				//search for all Elements with tag name "values"
					//These all contain an attribute "z" which contains the z slice number that the data
					//in that element pertains to...
				nList2 = docWplAuto.getElementsByTagName("values");
				for(int a=0; a<nList2.getLength(); a++) {
					//retrieve the z value, and compare to the "z" string above.
						//If they are equal, this is the values element where the data needs to be retrieved from:
					zDepthStr = ((Element)nList2.item(a)).getAttribute("z");
					if(zDepthStr.equals(z) ) {
						zDepthElement = (Element)nList2.item(a);
					}
				}
				
				//Next, collect the data from the zDepthElement retrieved above
					//First, create a new memToLength object:
				memToLength = new MemToLength();
				
					//The data is stored in this element as xyLength elements:
				nList2 = zDepthElement.getElementsByTagName("xyLength");
				for(int a=0; a<nList2.getLength(); a++) {
					memory = Long.parseLong( ((Element)nList2.item(a)).getAttribute("memory") );
					length = Integer.parseInt( ((Element)nList2.item(a)).getTextContent() );
					////IJ.showMessage("memToLength memory: "+memory);
					////IJ.showMessage("memToLength length: "+length);
					memToLength.add(memory,  length);
				}
				
			}
			catch (NullPointerException e) {
				//close InputStream
				try {
					in.close();
				}
				catch (IOException ex) {
					ex.printStackTrace();
				}
			}
			
			//Now, the memToLength object contains all the data from the XML file.
			
			//look up the MaxXY length based on the maxMem value passed to this method:			
			xy = memToLength.lookUpMaxXY(maxMem);
			
			//IJ.showMessage("Max XY value: "+xy);
			
			// return this MaxXY length from the memToLength object:
			return xy;
			
		}//end getMaxXY()
		
		protected void getEfficientLengths(EfficientLengths efficientLengths) {
			
			//first, retrieve the data from the docWplAuto:
			
			try {
				docWplAuto.getDocumentElement().normalize();
				////IJ.showMessage("Root Element: "+ doc.getDocumentElement().getNodeName() );
			
				NodeList nList2;
				String zDepthStr;
				Element effLenElement = null;
				
				int start;
				int end;
				
				//search for all Elements with tag name "efficientLengths"
					//There is only one element with this tagname, which contains the data for efficient lengths.
				nList2 = docWplAuto.getElementsByTagName("efficientLengths");
	
				//convert the node list into an Element (it only contains one element, so can just use index of 0!)
				effLenElement = (Element)nList2.item(0);
				
				//Next, collect the data from the effLenElement retrieved above
				
					//The data is stored in this element as effLength elements:
				nList2 = effLenElement.getElementsByTagName("effLength");
				for(int a=0; a<nList2.getLength(); a++) {
					start = Integer.parseInt( ((Element)nList2.item(a)).getAttribute("start") );
					end = Integer.parseInt( ((Element)nList2.item(a)).getTextContent() );
					////IJ.showMessage("Efficient Lengths start: "+start);
					////IJ.showMessage("Efficient Lengths end: "+end);
					efficientLengths.add(start, end);
				}
				
			}
			catch (NullPointerException e) {
				//close InputStream
				try {
					in.close();
				}
				catch (IOException ex) {
					ex.printStackTrace();
				}
			}
			
			
		} //end getEfficientLengths()
		
		
		/**
		 * This method converts the two presented values into a matched array object which contains
		 * the start and end values for each crop of an image.  If no cropping is required, this
		 * method will return null.
		 * <p>
		 * No extra padding of the image is necessary - a comparison of edges on two deconvolved images
		 * revealed that effects of edge are minimal compared to the effect of having a different image
		 * with different intensity histograms (which occurs when deconvolving over boundaries), and thus
		 * many objects thoughout a shared corner of two deconvolved images show different intensity
		 * profiles (see EDGE EFFECTS in 465-02 for further details).  To save complication, and since it
		 * will not benefit the deconvolution, no padding is used in this algorithm.
		 * <p>
		 * @param imageLength The length of the Imp in a given plane.
		 * @param length The length which the Imp length should be divided to.
		 */
		public CropPoints determineImageDivision(int imageLength, int length) {
			//CropPoints object to return:
			CropPoints cropPoints ;
			if(length == imageLength) {
				//If these values are equal, no cropping is required, and so CropPoints object with
					//start equal to 0, and end equal to imageLength:
				cropPoints = new CropPoints(); 
				cropPoints.add(0,  imageLength);
				return cropPoints;
			}
			
			 cropPoints = new CropPoints(); //create a new CropPoints object to store values in:
			
			int index = imageLength / length; //use this index to loop through correct number of times to break
												// up the imageLength appropriately.
												// NOTE: This rounds correctly, as length will always round DOWN.
			
			//create ints to store start and end values:
			int start;
			int end;
			
			//before starting loop, divide imageLength by index and STORE AS A DOUBLE:
			double roundedLength = (double)imageLength / index;
			
			//start loop to process imageLength:
			for(int a=0; a<index; a++) {
				// start is easily determined:
				if(a==0) {
					//if this is the first iteration, start MUST be 0:
					start = 0;
				}
				else {
					//if this is above the first iteration, start must be the previous end value, 
						//DO NOT plus 1, as need selections to be CONTIGUOUS!:
					start = (cropPoints.getEnd(a-1)) ;
				}
				
				//determining end is slightly more tricky:
					// If there is a rounding error, need to account for this & actually ROUND the value
					// during division of imageLength by index
					// This can be done by using roundedLength - and multiplying it by (a+1) before rounding:
				end = (int)Math.round( (roundedLength*(a+1)) );
				
				//Now both the start and end values are determined, can add these to the cropPoints object:
				
				cropPoints.add(start, end);
				
			}
			
			return cropPoints;
			
		} //end determineImageDivision()
		
	} //end class XYStackSplitter
	
	
	/**
	 * This class contains two arraylists which associate memory values in bytes to
	 * the maximum XY dimensions that the WPL Auto Deconvolution algorithm can handle
	 * before an OutOfMemory Exception is thrown.
	 * 
	 * @author stevenwest
	 *
	 */
	class MemToLength {
		private ArrayList<Long> mem;
		private ArrayList<Integer> length;
		
		public MemToLength() {
			mem = new ArrayList<Long>();
			length = new ArrayList<Integer>();
		}
		
		public void add(long mem, int length) {
			this.mem.add(mem);
			this.length.add(length);
		}
		
		/**
		 * Look up the XY length value corresponding to the memory value which does
		 * not exceed the passed memory value.
		 * @param memory A long indicating the memory value required to look up.
		 * @return The XY Length int which this memory can handle for deconvolution.
		 */
		public int lookUpMaxXY(long memory) {
			
			int maxMemIndex = -1;
			
			for(int a=0; a<mem.size(); a++) {
				if(memory < mem.get(a) ) {
					maxMemIndex = (a-1);
					break; //must break out of this for loop!!
				}
			}
			
			//Now, maxMemIndex is equal to the index position in the ArrayList
			//where the memory long passed to this method is just above the memory value
			//in the array
			
			//IJ.showMessage("maxMemIndex: "+maxMemIndex);
			//IJ.showMessage("mem size: "+mem.size() );
			//IJ.showMessage("length size: "+length.size() );
			
			//Next, use this index to return the length value at that index from the length ArrayList:
			
			return length.get(maxMemIndex);
			
		} //end lookUpMaxXY
		
	} //end class MemToLength
	
	/**
	 * This class contains two array lists which list the Start and End image XY values where
	 * EFFICIENT deconvolution occurs with WPL Auto Deconvolution.
	 * @author stevenwest
	 *
	 */
	class EfficientLengths {
		ArrayList<Integer> start;
		ArrayList<Integer> end;
		
		public EfficientLengths() {
			start = new ArrayList<Integer>();
			end = new ArrayList<Integer>();
		}
		
		public void add(int startVal, int endVal) {
			start.add(startVal);
			end.add(endVal);
		}
		
		public void remove(int index) {
			start.remove(index);
			end.remove(index);
		}
		
		public int size() {
			return start.size();
		}
		
		public int returnMaxEndVal() {
			return end.get( (end.size()-1) );
		}
		
		public int returnFirstStartVal() {
			return start.get(0);
		}
		
		public void integrateMaxVal(int maxXY) {
			//First, create an int called index:
			int index = -1;
			//loop through the end values in end:
			for(int a=0; a<end.size(); a++) {
				////IJ.showMessage("end at index: "+a+" is: "+end.get(a) );
				if( maxXY < end.get(a) ) {
					//if the parsed value is below a value in end, 
					//next check the start value at this index:
					////IJ.showMessage("maxXY: "+maxXY+" is below end value: "+end.get(a) );
					if(maxXY < start.get(a)) {
						//if maxXY is ALSO below the start value, need to remove all subsequent values from
						//BOTH start and end, up to and including the current reference.
						//to do this, set index to a, and break from this for loop:
						index = a;
						break;
					}
					else if(maxXY >= start.get(a) ) {
						//else if maxXY is BIGGER than the start value, need to set maxXY to corresponding value
						//in end [as this is the maximum XY length the current memory can take]
						//and delete all OTHER entries in start and end:
						end.set(a, maxXY);
						//To delete all OTHER entries, set index to "a+1", to ensure indexes beginning one above
						//the current index are removed:
						index = a+1;
						break;
					}
				}
				
			}
			
			//now if index is greater than -1, the maxXY value has been determined to be between some values
			//in start and end, and has already been integrated if necessary.
			//The value of index indicates from which index in start and end values need to be removed from.
			//To prevent any out of index errors, start from the end of start/end ArrayLists, and work up to
			//the last index:
			//Remember only apply this if index is greater than -1!!:
			if(index > -1) {
				////IJ.showMessage("index for a: "+index);
				for(int a=(size()-1);a>=index; a--) {
					////IJ.showMessage("a: "+a);
					remove(a);
				}
			} //end if
				
		} //end integrateMaxVal()
		
		/**
		 * This method will determine the best means of dividing the startLength to ensure efficient processing
		 * of the image.  The startLength will be divided until it falls within an efficient Length.  Careful
		 * management of the method is required for particular lengths where inefficient processing cannot be 
		 * avoided.
		 * @param startLength
		 * @return length which the image should be divided to
		 */
		public int parseLength(int startLength) {
			int length = 0;
			
			//First, must pass the startLength through the start and end ArrayLists to determine where it sits:
			
			int position = parseVal(startLength);
			
			if(position > -1) {
				//if position is above -1, the startLength fits into an efficient length, so return startLength!
				return startLength;
			}
			else if(position == -1) {
				//the passed value is not efficient, but it may fit into an efficient length, as it sits above
				// some values in start and end.
				//so need to divide the startLength down and re-pass through the parseVal method:
				return parseLengthArray(startLength);
			}
			else if(position == -2) {
				//The passed value exceeds the maximum length which is efficient AND within memory constraints!
				//Here, the value must be divided until it fits into the maximum value FIRST.
				//This can be determined by dividing the startLength by the max value in end:
				int index = startLength / returnMaxEndVal();
				//Since this will automatically be rounded DOWN, next add 1 to index:
				index = index + 1;
				//Next, determine the new startLength which fits into the MaxEndVal:
				startLength = startLength / index;  //note, this is rounded down!
				//and finally parse this val back to THIS METHOD:
				return parseLength(startLength);
				
			}
			else if(position == -3) {
				//the passed value is below the minimum value, this cannot be altered, so just return
				//the value:
				return startLength;
			}
			
			return startLength;

		} //end parseLength()
		
		
		protected int parseLengthArray(int startLength) {
			
			//First, need to divide up the startLength by the natural number set UNTIL the value reaches
			//less than start's FIRST VALUE:
			
			//Store the data in an ArrayList<Integer>
			ArrayList<Integer> startLengths = new ArrayList<Integer>();
			
			//an incrementing integer is needed to divide the startLength value, starting at 2:
			int incrementingInteger = 2;
			//and a value to store the divided start length, initialised to startLength's value:
			int dividedStartLength = startLength;
			// and a value to store position:
			int position;
			
			while( dividedStartLength > returnFirstStartVal() ) {
				dividedStartLength = startLength / incrementingInteger;
				startLengths.add(dividedStartLength);
				incrementingInteger = incrementingInteger + 1;
			}
			
			//startLengths now contains a series of values which must be tested.  These will be run
			//through the parseVal() method first to see if any fit an efficient deconvolution
			//length:
			for(int a=0; a<startLengths.size(); a++) {
				
				position = parseVal( startLengths.get(a) );
				
				if(position > -1) {
					return startLengths.get(a);
				}
				
			}
			
			//if this point is reached, none of the values in startLengths fitted into efficient deconvolution
			//lengths.
			
			//Is this even possible with the current efficient start and end values?!
				//YES!  If the image length is 500 for example, it cannot be easily deconvolved efficiently...
				// NOTE - corrected now, so this should not be an issue really... [see 2043 above]
			
			//in this case, a slightly more fancy means of deconvolving may be required...
			
			//for now, just return the startLength:
			
			return startLength;	
			
		} //end parseLengthArray()

		
		protected int parseVal(int startLength) {

			
			for(int a=0; a<start.size(); a++) {
				if( startLength <= end.get(a) ) {
					//if startLength is less than or equal to the given end value, compare to start value:
					if(startLength >= start.get(a) ) {
						//if startLength is greater than or equal to start's value, it fits into efficient length
						//therefore return a:
						return a;
					}
					else {
						//if startLength is less than start's value, it does not fit into
						//efficient length, so return -1
						if(a > 0) {
							//if a is above 0, the passed value can still potentially fit into 
							//values below the current value of a - signal this by returning -1:
							return -1;
						}
						else {
							//if this is the FIRST data structure, it is impossible to find an efficient
							//length to process the image with, so signal this by returning -3:
							return -3;
						}
					}
				}
			}
			//if the end is reached, the startLength exceeds the largest number in this data structure,
			//return -2 to signal this:
			return -2;
		}
	
	} //end class EfficientLengths
	
	
	public class CropPoints {
		
		protected ArrayList<Integer> start;
		protected ArrayList<Integer> end;
		
		public CropPoints() {
			start = new ArrayList<Integer>();
			end = new ArrayList<Integer>();
		}
		
		public void add(int start, int end) {
			this.start.add(start);
			this.end.add(end);
		}
		
		public void remove(int index) {
			start.remove(index);
			end.remove(index);
		}
		
		public int getEnd(int index) {
			return end.get(index);
		}
		
		public int getStart(int index) {
			return start.get(index);
		}
		
		public int getLength() {
			return start.size();
		}
		
		public int getFirstStart() {
			return start.get(0);
		}
		
		public int getFirstEnd() {
			return end.get(0);
		}
		
	}
	
}
