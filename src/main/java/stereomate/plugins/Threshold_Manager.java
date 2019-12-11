package stereomate.plugins;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Panel;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
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
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileSystemView;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.process.LUT;
import stereomate.dialog.DialogWindow;
import stereomate.image.ImageWindowWithPanel;
import stereomate.settings.OptionsPanel;
import stereomate.settings.StereoMateUtilities;

/**
 * This class provides the interface to generate, edit and save image thresholding methods.
 * <p>
 * Thresholding methods often are composed of more than one processing step:
 * <p>
 * 1. Image noise reduction.
 * <p>
 * 2. Image thresholding.
 * <p>
 * This interface allows different image processing and thresholding methods to be strung together
 * to generate optimal thresholding for the type of image & its labelling quality - the Image Processing
 * Procedure Stack.
 * 
 * @author stevenwest
 *
 */
public class Threshold_Manager implements PlugIn, StereoMateAlgorithm {

	/**
	 * dialogWindow object - to create a dialog to select files for processing.
	 */
	DialogWindow dw;
	
	/**
	 * ImageWindowWithPanel object - to hold the current ImagePlus, and to provide the tools to
	 * manipulate it in the panel.
	 */
	ImageWindowWithPanel IWP;
	
	
	int bitDepth;
	
	JComboBox<String> bitDepthComboBox;

	/**
	 * Shuts down the iw on IWP when window is closed.
	 */
	WindowAdapter imageWindowAdapter;
	
	
	/**
	 * An ImagePlus object to represent the channel being thresholded.  This imp is filled with a copy
	 * of the currently processed channel, and it is this imp which the processing and thresholding methods
	 * will act on.  This will be displayed in the IWP window over the original data, with an appropriate
	 * projection of the stack applied.
	 * <p>
	 * Note, this means that if any process is removed, the activeImp must be re-populated with the original
	 * pixel values -> which means copying the original IWP ImageStack from the correct channel into activeImp
	 * before re-applying all of the image processing steps.  This may make processing quite slow!
	 * <p>
	 * Is there another way to achieve this?  I dont think there is, other than storing both the original un-processed
	 * stack and then a processed stack, but this increases the memory requirements even further!
	 * <p>
	 * Ensure there is a single method to perform this operation..
	 */
	ImagePlus activeImp;
	
	
	/**
	 * Represents whether the activeImp is ON - ie displayed in the IWP.
	 */
	boolean activeImpOn;
	
	
	
	/**  MAIN PANEL   **/
	
	/**
	 * A panel to hold the list and the buttons which manipulate the list.
	 */
	JPanel mainPanel;
	

	
	/**  ACTIVE CHANNEL PANEL   **/
	
	/**
	 * Panel to hold the Active Channel selector.
	 */
	JPanel activeChannelPanel;
	
	/**
	 * A Spinner to select the activeChannel - the channel to apply the thresholding method to.
	 */
	JSpinner activeChannelSpinner;
	
	/**
	 * Spinner model for activeChannelSpinner.
	 */
	SpinnerModel spinnerModel;
	
	/**
	 * This will represent the channel which the user has changed to when activeChannelSpinner is altered.  Set in its
	 * changeListener and used by activeChannelThread to set the correct channel as the active channel.
	 */
	int activeChannelNum;
	
	/**
	 * This represents the thread which is executed by the activeChannelSpinner changeListener.  Frees up the EDT.  This
	 * thread will update the activeChannel to the channel selected by the user - i.e. sets active imp to a copy of the 
	 * channel from original imp in IWP.
	 */
	ActiveChannelThread activeChannelThread;
	
	/**
	 * A Label for the activeChannel panel.
	 */
	JLabel activeChannelLabel;
	
	
	/**
	 * Checkbox from the channelcheckbox for the activeImp inside IWP.
	 */
	JCheckBox activeChannelcb;
	
	JPanel activeChannelcbPanel;
	
	/**  Image Processing Buttons  **/
	
	/**
	 * Panel to hold the image processing buttons.
	 */
	JPanel processingPanel;
	
	/*
	 * GridLayout for processing panel.
	 */
	//GridLayout gridLayout;
	
	/**
	 * Buttons to access the pre-processing, thresholding, and post-processing panels for image manipulation.
	 */
	JButton preProcessingButton, thresholdingButton, postProcessingButton;
	
	//ArrayList<String> preProcessingStrings, thresholdingStrings, postProcessingStrings;
	String[] preProcessingStrings, thresholdingStrings, postProcessingStrings;
	
	/**  Image Processing List  **/
	
	/**
	 * A panel to hold all the list components.
	 */
	JPanel listPanel;
	
	/**
	 * A list to hold representations of each image processing step added to the thresholding procedure.
	 */
	JList<String> list;
	
	/**
	 * List model for the image processing list.
	 */
	DefaultListModel<String> listModel;
	
	/**
	 * Boolean to record when an item is being removed from list.  When true, should use the saved 
	 */
	//boolean removingListItem;
	
	/**
	 * A JScrollPane for containing the list - will add scroll bars to the JList.
	 */
	JScrollPane listScrollPane;
	
	/**
	 * A panel to hold the buttons to manipulate the list.
	 */
	JPanel leftListButtonPanel, rightListButtonPanel;
	
	/**
	 * Buttons to manipulate the items on the image processing step list: Toggle selected item on and off, edit the
	 * image processing parameters of selected item, or delete selected item, move item up/down the list.
	 */
	JToggleButton toggleItem;
	JButton editItem, deleteItem, upItem, downItem;
	
	/**
	 * ImageIcons to hold the toggle on and off icons.
	 */
	ImageIcon toggleIconOn, toggleIconOff;
	

	
	/**  Status Panel  **/
	
	/**
	 * A panel to hold the JTextArea showing the status - which algorithm is being applied to
	 * the image.
	 */
	JPanel statusPanel;
	
	/**
	 * A Text Area to hold the current status of the processing step - this is filled when an algorithm
	 * is being applied to the image, to let the user know to wait for processing to finish.
	 */
	JTextArea status;
	
	/**
	 * Scroll pane for the status text area:
	 */
	JScrollPane statusScrollPane;
	
	
	/**  Image Navigation Buttons  **/
	
	/**
	 * Panel to hold navigation buttons.
	 */
	JPanel navigationPanel;
	
	/**
	 * Buttons to navigate through images in selected input folder.
	 */
	JButton nextButton, previousButton;
	
	
	
	/**  Image Load/Save Buttons  **/
	
	/**
	 * Panel to hold load and save buttons.
	 */
	JPanel loadSavePanel;
	
	/**
	 * Buttons to load and save thresholding procedures.
	 */
	JButton loadButton, saveButton;
	
	/**
	 * Panel to hold navigation and load/save panels.
	 */
	JPanel navLoadSavePanel;
	
	
	
	
	/** IMAGE-PROCESSING PANEL  **/
	
	/**
	 * Panel to hold the image processing tools - algorithm selection, parameter selection.
	 */
	JPanel imageProcessingPanel;
	
	/**
	 * Combo Box to hold algorithm choices in in the imageProcessingPanel.
	 */
	JComboBox<String> algorithmComboBox;
	
	/**
	 * A String array to hold the potential choices for the algorithmComboBox.
	 */
	String[] algorithmArray;
	
	/**
	 * A panel to hold the options for the selected algorithm in algorithmComboBox.  Allows
	 * users to select correct options for the selected algorithm.
	 */
	OptionsPanel optionsPanel;

	/**
	 * A Panel to hold the Preview and Add buttons.
	 */
	JPanel previewAddButtonsPanel;
	
	/**
	 * A JButton to add the selected algorithm to the image processing procedure stack.
	 */
	JButton addAlgorithmButton, cancelButton;
	
	/**
	 * Checkbox to set whether to preview the image processing step.
	 */
	JCheckBox previewButton;
	
	
	
	/**
	 * This object represents the image processing procedure stack - the stack of procedures which
	 * will process the image to form the thresholded (processed) image.
	 * <p>
	 * This can call the set of image processing methods on a given imp, and can read/write the procedure
	 * stack from/to an XML file.
	 */
	ProcedureStack procedureStack;
	
	
	
	/**
	 * Sets up the Dialog Window for Threshold Manager PlugIn.
	 */
	@Override
	public void run(String arg) {
		
		dw = new DialogWindow("Threshold Manager", this);
		
		dw.addFileSelector("Image or DIR to setup Threshold on:"); //add FileSelector panel.
		//dw.addFileSelector("Image02:", dw.FILES_AND_DIRS, dw.MATCHED_INPUT);
		
		dw.add( addBitDepthSelector() );
		
		dw.addActionPanel(); //add Action panel - for Cancel and Process buttons.
		dw.setPlugInSuffix("_TM");  //This is probably not required.
		
		dw.setMethodCallNumber(1); //sets the loop to only call the process() method only ONCE.
		
		dw.layoutAndDisplayFrame(); //display the DialogWindow.
		
	}
	
	
	
	/**
	 * Returns a JPanel containing a selector for the bit depth of the image.  This establishes
	 * what bit depth images will be converted to (if required) as the image is opened into a ImageWindowWithPanel
	 * object.
	 * @return
	 */
	public JPanel addBitDepthSelector() {
		
		
			//A panel to store the channels and PSF selection buttons.
				JPanel p = new JPanel();
				
				//panal to put label and combobox on.
				JPanel comboBoxAndLabelPanel = new JPanel();
				
				//create JLabel and add to panel:
				JLabel comboBoxLabel = new JLabel("Bit Depth:");
				comboBoxAndLabelPanel.add(comboBoxLabel);
				
				
				//Set of Strings for the ComboBox - 8, 16, or 32 to represent different bit depths available.
				String[] channels = new String[] { "8", "16", "32" };
				
				//Make combo box:
				bitDepthComboBox = new JComboBox<String>(channels);
				
				//Add Action Listener -> update bitDepth variable to value selected in combo box:
				bitDepthComboBox.addActionListener( new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						JComboBox<String> cb = (JComboBox<String>)e.getSource();
				        String chan = (String)cb.getSelectedItem();
				        int chanInd = cb.getSelectedIndex();
				        bitDepth = Integer.parseInt(chan);
					}
					
				});
				
				//Select first item in comboBox and set bitDepth to this selected value:
				bitDepthComboBox.setSelectedIndex(0);
				bitDepth = Integer.parseInt( (String)bitDepthComboBox.getSelectedItem() );
				
				//Add this combobox to the Panel:
				comboBoxAndLabelPanel.add(bitDepthComboBox);
				
				//finally, add panel to p, and return p:
				p.add(comboBoxAndLabelPanel);
				
				return p;
		
	}
	
	
	
	/**
	 * Blank..
	 */
	@Override
	public void setup() {
		
		// remove the bitDepthComboBox listener:
		StereoMateUtilities.removeActionListener(bitDepthComboBox);
		
		imageWindowAdapter = new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e); // retain any code which is called before.
				
				// shutdown TM:
				removeListeners();
				
				procedureStack = null;
				activeImp = null;
				activeChannelThread = null;
				
				// remove THIS listener from IWP:
				IWP.iw.removeWindowListener( this );
				
				shutdownDW();

				shutdownIWP();
				
				
				Window w = WindowManager.getWindow("Log");
				
				if( w != null ) {
					w.dispose();
				}
				
			}
		};
		
	}
	
	
	/**
	 * Sets up IWP such that the first channel is duplicated and added to HyperStack, retrieves a reference
	 * to this extra channel (stored in activeImp) & sets up the Panel for the Threshold Manager plugin.
	 */
	@Override
	public void process(ImagePlus imp) {
		
		
		//Need to implement the thresholding here -> set up an IWP with the appropriate thresholding
			//Tools added.
		
		
		//Setup activeImp: -> Do this in IWP?!  
			// YES - now done in IWP
		
		//set channelNum to 1 for the initial value:
		//need to do this here, as setupThresholdManager is recalled in the prev / next button listeners
			//and the channelNum may be different then...
		activeChannelNum = 1;
		
		//set up the IWP - set 1st channel as the activeChannel at end (ie duplicated):
			//Channel projector must be set up for this image, as there must be min of 2 channels
			//BUT must HIDE the last channel control, and setVisible(false) for it so it can be controlled
				//by this Plugin!
		IWP = new ImageWindowWithPanel(imp, new Panel(), activeChannelNum, bitDepth );
		
			//Note, this can be controlled by manipulating the channelCheckBox array, 'channels'.
				//Specifically, calling channels[index].stateChanged() will switch between displaying and not displaying
					//the channel.  Or the setState(boolean) method can be used to modify state and checkbox together.
			//ALSO -> bitDepth indicates the image will be converted to bitDepth at point of opening in IWP.
				//This may be 8, 16, 32 -> set by user in DialogWindow.
		
		//Use the IWP returnChannel() method to get an ImagePlus object which contains all the IPs from the LAST CHANNEL:
			//This is the ACTIVE IMP - ie the ImagePlus that represents the duplicated channel that is stuck to the end of
				//the original imp, the imp which image processing steps will be performed on..
		// activeImp = IWP.returnChannelDuplicate( IWP.getChannels() );
		activeImp = IWP.returnChannel( IWP.getChannels() );
		
		//Set active channel to last channel - the one NOT DISPLAYED & which will contain the processed imp:
			//This just means pixel data is being manipulated on the last channel - a moot point..
			//Really required so the "Grays" plugin below acts on this channel..
		IWP.setActiveChannel( IWP.getChannels() );
		
		//Make the LUT of this last channel White on both projected and original imp:
		IJ.run(IWP.getImagePlus(), "Grays", "");
			//to set original imps last channel to GREYS LUT - need to set its channel to the last channel:
			IWP.getOriginalImagePlus().setC( IWP.getChannels() );
		IJ.run(IWP.getOriginalImagePlus(), "Grays", "");
			//re-set the original imp channel to the first channel:
			IWP.getOriginalImagePlus().setC( 1 );
			//This sets the last channel - representing the active channel - to the GREYS LUT - i.e. black to white.
			
			// Setting the close operation to include saving the current Arff Dataset:
			IWP.iw.addWindowListener( imageWindowAdapter );			
		
		// setup the threshold manager -> all buttons and components + listeners on the IWP panel for Threshold setup:
		setupThresholdManager(true);
		
		
	}

	
	/**
	 * Clear all the objects and listeners to prevent memory leaks.
	 */
	@Override
	public void cleanup() {
		
		
	}
	
	
	public void setupThresholdManager(boolean newIPPS) {
		
		//generate a new procedureStack object:
		
		if(newIPPS) {
			//procedureStack = new ImageProcessingProcedureStack(bitDepth, status);
			procedureStack = new ProcedureStack(bitDepth, activeChannelNum);
		}
		
		
		//setup the main panel components and store to the mainPanel:
		mainPanel = new JPanel();

		// set this to boxLayout & LINE_AXIS -> To allow use of JSeparators!
		mainPanel.setLayout( new BoxLayout(mainPanel, BoxLayout.LINE_AXIS) );

		// put this Panel into a scrollPane to make it scroll on smaller screens if all components are not visible:
		JScrollPane scrollPane = new JScrollPane(mainPanel);

		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

		scrollPane.setBorder( BorderFactory.createEmptyBorder(0,0,6,0) );
		
		
		//mainPanel.setLayout( new GridLayout(1,0,2,2));
		//mainPanel.setLayout( new BoxLayout(mainPanel, BoxLayout.LINE_AXIS) );
		
		
		
		/**
		 *   ADD ACTIVE CHANNEL PANEL & COMPONENTS & SET UP THE activeIMP
		 */
		
		//Setup ACTIVE CHANNEL Spinner & label:
		
		// create Panel to store components:
		activeChannelPanel = new JPanel();
		
			//create the spinner model:
				//should move from 1 to channel number, and initial value is 1.
				//need -1 from channel number, as last channel is reserved for the image processing display..
		// IJ.log("channel num: "+activeChannelNum+" IWP.getChannels: "+IWP.getChannels() );

		spinnerModel = new SpinnerNumberModel(activeChannelNum,1,(IWP.getChannels()-1),1);
		////IJ.log("IWP channels: "+IWP.getChannels() );

		//instantiate the JSpinner with the created model:
		activeChannelSpinner = new JSpinner(spinnerModel);

		//Generate a new Thread to perform operations in activechannelSpinner listener in separate thread to EDT:
		//Need to do this to ensure the EDT is free for repainting the status JTextArea, otherwise calls to repaint()
		//on it will only execute once the EDT is free -> which is when the listener method has finished!
		//Therefore cannot update the status JTextArea DURING execution of the listener cord.
		//However, if the code is in a SEPARATE THREAD to EDT, the repaint() calls will be executed immediately
		//by EDT:			

		//Add Listener to Spinner:
		//This will deactivate all buttons and re-generate the activeImp with the newly selected activeChannel on the
		//activeChannelSpinner:
		//This is performed in a new thread, which at the end re-activates the buttons on mainPanel.
		activeChannelSpinner.addChangeListener( new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {

				activeChannelNum = (int)spinnerModel.getValue();

				procedureStack.setActiveChannel(activeChannelNum);

				//to ensure there is no cross-talk between user and EDT (ie user inputs), must deactivate all buttons:
				deactivateButtons(mainPanel);

				//Run the IPPT thread -> this calls the activeChannelThread, but will also update the image if
				//it has any procedureStack already applied:

				ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread(mainPanel, true);
				//set tofOff to true, to force the thread to run & ensure the buttons are re-activated
				ippt.start();

				//Just run the activeChannelThread -> this will free EDT, and execute the code to update the activeChannel:
				// as it is called in this new thread.
				//activeChannelThread = new ActiveChannelThread(); //need to generate new thread object each time!
				//activeChannelThread.start(); //this method reactivates the Buttons when it finishes..

			}

		});


		//instantiate the label:
		// activeChannelLabel = new JLabel("Active Channel:");


		//get the ref to the activeChannel stack channelCheckBox in IWP -> put this onto the activeChannelPanel too!

		activeChannelcb = IWP.channels[IWP.getChannels()-1].getCheckBox();
		
		activeChannelcbPanel = new JPanel();
		
		activeChannelcbPanel.add(activeChannelcb);

		//Add label & Spinner to panel:

		//activeChannelPanel.setLayout( new GridLayout(0,1));
		// activeChannelPanel.setLayout( new BoxLayout(activeChannelPanel, BoxLayout.PAGE_AXIS) );
		
		activeChannelPanel.setLayout( new GridBagLayout() );
		
		GridBagConstraints gbc = new GridBagConstraints();
		
		// Add buttons to Panel:
		// set fill and weights for all:
		//gbc.fill = GridBagConstraints.BOTH; // fill out in both dimensions
		gbc.weightx = 0.5;
		gbc.weighty = 0.5;

		// set ipadx to 70 to increase panel width:
		gbc.ipadx = 60;

		gbc.gridx = 0;
		gbc.gridy = 0;
		activeChannelPanel.add(activeChannelSpinner, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 1;
		activeChannelPanel.add(activeChannelcbPanel, gbc);
		
		
		// reset ipadx:
		gbc.ipadx = 0;
		
		// Set Border with Title to filterPanel:
		activeChannelPanel.setBorder( BorderFactory.createTitledBorder("Active Channel:") );

		// Add panel to Main Panel:
		mainPanel.add(activeChannelPanel);


		/**
		 * ADD PROCESSING BUTTON AND COMPONENTS:
		 */

		processingPanel = new JPanel();

		// Set Layout of processingPanel:
		//processingPanel.setLayout( new GridLayout(0,1) );
		//processingPanel.setLayout( new BoxLayout(processingPanel, BoxLayout.PAGE_AXIS) );
		processingPanel.setLayout( new GridBagLayout() );

		//construct processing buttons:
		preProcessingButton = new JButton("PRE-PROCESSING");
		thresholdingButton = new JButton("THRESHOLDING");
		postProcessingButton = new JButton("POST-PROCESSING");

		//first blank string is blank - no spaces:
		// TODO ENCODE Custom Processing Step to recall effectively when being edited (editButton) 
		preProcessingStrings = new String[] { "", "Gaussian Blur 3D...","Median 3D...", "Mean 3D...", 
				"Minimum 3D...", "Maximum 3D...", "Variance 3D...",  "Gaussian Blur...", 
				"Median...", "Mean...", "Minimum...", "Maximum...", "Variance...",
				"Unsharp Mask...", "Convolve...", "Kuwahara Filter", "Linear Kuwahara", "8-bit", "Custom" } ;

		//first blank string is " " - 1 space:
		// TODO Generate Code to handle C2 Image Calculator:
		// select a pre-set procedure stack to threshold ANOTHER channel (NOT the current Active Channel)
		// Select one of the Image Calculator Steps.
		// This is basically either AND or OR -> AND gives OVERLAP voxels, OR give overlap and each
		// objects voxels.
		// Independent of which is selected, the STEREOLOGICAL FILTER in SM-Analyser should filter based
		// on the combination of both objects & the overlap!
		// Does the User want to analyse the overlap object (this is AND), 
		// or any objects which possess overlap (no encoded), 
		// or the combination of the two objects (this is OR!).
		//PERHAPS the user wants to assess mroe than one of these?!
		// Need a thresholding and analysis structure which accommodates this kind of analysis...

		//Ensure therefore C2 Image Calculator is only available with images of 2 channels or more
		//Ensure that this can only run the image calculator once two thresholded channels are generated
		//The image calc is performed on the two thresholded channels post their processing stacks.
		//Find way to encode this into the XML file.
		//I have decided to NOT implement the 2 channel analysis at this point:
		//2 channel analysis will be performed in a SEPARATE plugin to the original SM Analyser...

		thresholdingStrings = new String[] { " ", "Auto Threshold" } ;

		//first blank string is "  " - 2 spaces:
		postProcessingStrings = new String[] { "  ", "Erode", "Dilate", "Open", "Close-", "Outline", "Fill Holes",
				"Skeletonize", "Watershed" } ;

		// IJ.run(imp, "Erode", "stack");
		// IJ.run(imp, "Dilate", "stack");
		// 	IJ.run(imp, "Open", "stack");
		// IJ.run(imp, "Close-", "stack");
		// IJ.run(imp, "Outline", "stack");
		// IJ.run(imp, "Fill Holes", "stack");
		// IJ.run(imp, "Skeletonize", "stack");

		// IJ.run(imp, "Watershed", "stack");


		//set tooltip:
		preProcessingButton.setToolTipText("Access Pre-Processing Methods");
		thresholdingButton.setToolTipText("Access Thresholding Methods");
		postProcessingButton.setToolTipText("Access Post-Processing (Binary) Methods");

		//add actionlisteners to buttons:
		//These will set up the components to go on the processing panel, and then add these to the processing panel
		//and add the processing panel to the IWP.
		//NB: Must add behaviours to the combobox and buttons -> for preview use a separate thread: 
		// ImageProcessingProcedureThread.
		preProcessingButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				// Fill a new panel with a combo box containing pre-processing algorithms
				//ComboBox containing pre-processing filters:  Gaussian, Gaussian 3D, etc.
				//Allow the comboBox listener to apply appropriate options fields for user
				//to edit.
				//Add a button which allows the current algorithm and its settings to be added to
				//the image processing procedure stack.
				//Add a checkbox button which allows a preview of this image processing step (will
				// combine with any other steps already added to image processing procedure stack, putting
				// this step at the end of the stack).



				//create new ComboBox using preProcessingStrings:
				algorithmComboBox = new JComboBox<String>( preProcessingStrings );

				//Add action listener:
				//This must update the options panel to include the options for the selected algorithm
				algorithmComboBox.addActionListener( new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						//  Depending which algorithm is selected, need to adjust the options fields:

						//get the command name selected in the ComboBox:
						@SuppressWarnings("unchecked")
						JComboBox<String> cb = (JComboBox<String>)e.getSource();

						//update the Options Panel to display correct options for this command:
						updateOptionsPanel( (String)cb.getSelectedItem()  );

						//fill the ProcessingPanel with it components including the updated OptionsPanel:
						Threshold_Manager.this.fillImageProcessingPanel();

					}

				});

				//create a blank optionsPanel:
				optionsPanel = new OptionsPanel("");
				//optionsPanel.setLayout( new GridLayout(1,0) );
				optionsPanel.setLayout( new BoxLayout(optionsPanel, BoxLayout.LINE_AXIS) );

				//create status JTextArea:
				//This is already made in mainPanel -> no need to re-generate it here, just add statusPanel
				//to this processingPanel...
				//status = new JTextArea("Up to date.");

				//create panel for status:
				//statusPanel = new JPanel();
				//statusPanel.setLayout( new GridLayout(1,0) );
				//statusPanel.setLayout( new BoxLayout(statusPanel, BoxLayout.PAGE_AXIS) );

				//add status TextArea to statusPanel:
				//statusPanel.add(status);


				//create the Add algorithm button:
				//This will add the current algorithm and its options to the image processing procedure stack.
				addAlgorithmButton = new JButton("Add Algorithm");

				//Add listener to this button:
				//This must add the selected command and options to the procedureStack(at the end), and re-generate
				//the IWP main panel
				addAlgorithmButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						//  When pressed, add the current algorithm and its options to the
						//ImageProcessingProcedureStack object, and then return to the mainPanel:

						//Need to call the ImageProcessingProcedureStack in its thread
						//This needs to be called if the Preview checkbox is checked, as if so the current drawing
						//on activeImp is not correct -> want to remove this by calling the procedureStack,
						//which doesnt contain the preview, but does contain the correct set of image processing steps:

						boolean prevOn = false;
						if(previewButton.isSelected() == true) {
							//Call IPPT AFTER the mainPanel is filled, and deactivate the main panel buttons!
							//Just set flag prevOn to true:
							prevOn = true;
						}

						//add command to procedureStack - it is toggled off by default:
						procedureStack.addCommand(optionsPanel.commandTitle, optionsPanel.getOptionsString() );
						//and update the list:
						updateList();

						//Fill the mainPanel with its sub-panels and add to IWP:
						fillMainPanel();

						if(prevOn == true) {
							//NOW run the IPPT -> now the mainPanel has loaded..
							ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread( imageProcessingPanel );

							//run the thread:
							ippt.start();
						}

					}

				});

				//Set button inactive when first loading the imageProcessingPanel - only active when an appropriate
				//image processing algorithm is selected from the ComboBox:
				addAlgorithmButton.setEnabled(false);


				//create the Add algorithm button:
				//This will add the current algorithm and its options to the image processing procedure stack.
				cancelButton = new JButton("Canel");

				//Add listener to this button:
				//This should just re-generate the IWP main panel
				cancelButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						//  When pressed, return to the mainPanel:

						setIwpMainPanel();

					}

				});


				//create the preview checkbox:
				previewButton = new JCheckBox("preview:");

				//add listener to preview button:
				previewButton.addActionListener( new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						//  Apply just the procedure stack and then selected process to the activeImage -> activeImp

						if( previewButton.isSelected() ) {
							//Perform this in a separate thread to allow status to update as each command is run:
							ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread(optionsPanel.commandTitle, 
									optionsPanel.getOptionsString(),
									imageProcessingPanel );

							//run the thread:
							ippt.start();  // Does this thread need to run the WHOLE Procedure Stack?  Or only the preview?
							//Only preview, as any procedure stack being displayed in activeImp will have been
							//set up on the mainPanel...  This happens now because command title and options are
							//passed in the ippt constructor!
						}

						else {

							// need to undo the preview:
							//remove the activeImp display?!
							//NO -> need to put the activeImp back to its original state
							//This is achieved by just generating a new ippt with imageProcessingPanel (current active panel)
							//and running the thread!

							ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread( imageProcessingPanel );

							//run the thread:
							ippt.start();

						}

					}

				});



				//add preview and add buttons to previewAddButtonsPanel:
				previewAddButtonsPanel = new JPanel();
				//previewAddButtonsPanel.setLayout( new GridLayout(0,1) );
				previewAddButtonsPanel.setLayout( new BoxLayout(previewAddButtonsPanel, BoxLayout.PAGE_AXIS) );
				previewAddButtonsPanel.add(addAlgorithmButton);
				previewAddButtonsPanel.add(cancelButton);
				previewAddButtonsPanel.add(previewButton);

				//The the image processing panel with the combobox and panel:
				fillImageProcessingPanel();

			}

		});

		thresholdingButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// Fill the algorithm combo box with appropriate thresholding algorithms:
				// Fill a new panel with a combo box containing thresholding algorithms
				//ComboBox containing thresholding filters:  OTSU, IsoData, etc.
				//Allow the comboBox listener to apply appropriate options fields of algorithm for user
				//to edit.
				//Add a button which allows the current algorithm and its settings to be added to
				//the image processing procedure stack.
				//Add a checkbox button which allows a preview of this image processing step (will
				// combine with any other steps already added to image processing procedure stack, putting
				// this step at the end of the stack).




				//create new ComboBox using preProcessingStrings:
				algorithmComboBox = new JComboBox<String>( thresholdingStrings );

				//Add action listener:
				//This must update the options panel to include the options for the selected algorithm
				algorithmComboBox.addActionListener( new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						//  Depending which algorithm is selected, need to adjust the options fields:

						//get the command name selected in the ComboBox:
						@SuppressWarnings("unchecked")
						JComboBox<String> cb = (JComboBox<String>)e.getSource();

						//update the Options Panel to display correct options for this command:
						updateOptionsPanel( (String)cb.getSelectedItem()  );

						//fill the ProcessingPanel with it components including the updated OptionsPanel:
						Threshold_Manager.this.fillImageProcessingPanel();

					}

				});

				//create a blank optionsPanel:
				optionsPanel = new OptionsPanel("");
				//optionsPanel.setLayout( new GridLayout(1,0) );
				optionsPanel.setLayout( new BoxLayout(optionsPanel, BoxLayout.LINE_AXIS) );

				//create status JTextArea:
				//This is already made in mainPanel -> no need to re-generate it here, just add statusPanel
				//to this processingPanel...
				//status = new JTextArea("Up to date.");

				//create panel for status:
				//statusPanel = new JPanel();
				//statusPanel.setLayout( new GridLayout(1,0) );
				//statusPanel.setLayout( new BoxLayout(statusPanel, BoxLayout.PAGE_AXIS) );

				//add status TextArea to statusPanel:
				//statusPanel.add(status);


				//create the Add algorithm button:
				//This will add the current algorithm and its options to the image processing procedure stack.
				addAlgorithmButton = new JButton("Add Algorithm");

				//Add listener to this button:
				//This must add the selected command and options to the procedureStack(at the end), and re-generate
				//the IWP main panel
				addAlgorithmButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						//  When pressed, add the current algorithm and its options to the
						//ImageProcessingProcedureStack object, and then return to the mainPanel:

						//Need to call the ImageProcessingProcedureStack in its thread
						//This needs to be called if the Preview checkbox is checked, as if so the current drawing
						//on activeImp is not correct -> want to remove this by calling the procedureStack,
						//which doesnt contain the preview, but does contain the correct set of image processing steps:

						boolean prevOn = false;
						if(previewButton.isSelected() == true) {
							//Call IPPT AFTER the mainPanel is filled, and deactivate the main panel buttons!
							//Just set flag prevOn to true:
							prevOn = true;
						}

						//add command to procedureStack - it is toggled off by default:
						procedureStack.addCommand(optionsPanel.commandTitle, optionsPanel.getOptionsString() );
						//and update the list:
						updateList();

						//Fill the mainPanel with its sub-panels and add to IWP:
						fillMainPanel();

						if(prevOn == true) {
							//NOW run the IPPT -> now the mainPanel has loaded..
							ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread( imageProcessingPanel );

							//run the thread:
							ippt.start();
						}

					}

				});

				//Set button inactive when first loading the imageProcessingPanel - only active when an appropriate
				//image processing algorithm is selected from the ComboBox:
				addAlgorithmButton.setEnabled(false);


				//create the Add algorithm button:
				//This will add the current algorithm and its options to the image processing procedure stack.
				cancelButton = new JButton("Canel");

				//Add listener to this button:
				//This should just re-generate the IWP main panel
				cancelButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						//  When pressed, return to the mainPanel:

						setIwpMainPanel();

					}

				});


				//create the preview checkbox:
				previewButton = new JCheckBox("preview:");

				//add listener to preview button:
				previewButton.addActionListener( new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						//  Apply just the procedure stack and then selected process to the activeImage -> activeImp

						if( previewButton.isSelected() ) {
							//Perform this in a separate thread to allow status to update as each command is run:
							ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread(optionsPanel.commandTitle, 
									optionsPanel.getOptionsString(),
									imageProcessingPanel );

							//run the thread:
							ippt.start();  // does this thread need to run the WHOLE Procedure Stack?  Or only the preview?
							//Only preview, as any procedure stack being displayed in activeImp will have been
							//set up on the mainPanel...  This happens now because command title and options are
							//passed in the ippt constructor!
						}

						else {

							// need to undo the preview:
							//remove the activeImp display?!
							//NO -> need to put the activeImp back to its original state
							//This is achieved by just generating a new ippt with imageProcessingPanel (current active panel)
							//and running the thread!						
							ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread( imageProcessingPanel );

							//run the thread:
							ippt.start();

						}

					}

				});



				//add preview and add buttons to previewAddButtonsPanel:
				previewAddButtonsPanel = new JPanel();
				//previewAddButtonsPanel.setLayout( new GridLayout(0,1) );
				previewAddButtonsPanel.setLayout( new BoxLayout(previewAddButtonsPanel, BoxLayout.PAGE_AXIS) );
				previewAddButtonsPanel.add(addAlgorithmButton);
				previewAddButtonsPanel.add(cancelButton);
				previewAddButtonsPanel.add(previewButton);

				//The the image processing panel with the combobox and panel:
				fillImageProcessingPanel();


			}

		});

		postProcessingButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// Fill the algorithm combo box with appropriate post-processing filters:
				// Fill a new panel with a combo box containing post-processing algorithms
				//ComboBox containing post-processing BINARY filters:  watershed, fill holes, etc.
				//	ADD AND CALCULATOR HERE to combine with other channels..
				//Allow the comboBox listener to apply appropriate options fields of algorithm for user
				//to edit.
				//Add a button which allows the current algorithm and its settings to be added to
				//the image processing procedure stack.
				//Add a checkbox button which allows a preview of this image processing step (will
				// combine with any other steps already added to image processing procedure stack, putting
				// this step at the end of the stack).


				//create new ComboBox using preProcessingStrings:
				algorithmComboBox = new JComboBox<String>( postProcessingStrings );

				//Add action listener:
				//This must update the options panel to include the options for the selected algorithm
				algorithmComboBox.addActionListener( new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						//  Depending which algorithm is selected, need to adjust the options fields:

						//get the command name selected in the ComboBox:
						@SuppressWarnings("unchecked")
						JComboBox<String> cb = (JComboBox<String>)e.getSource();

						//update the Options Panel to display correct options for this command:
						updateOptionsPanel( (String)cb.getSelectedItem()  );

						//fill the ProcessingPanel with it components including the updated OptionsPanel:
						Threshold_Manager.this.fillImageProcessingPanel();

					}

				});

				//create a blank optionsPanel:
				optionsPanel = new OptionsPanel("");
				//optionsPanel.setLayout( new GridLayout(1,0) );
				optionsPanel.setLayout( new BoxLayout(optionsPanel, BoxLayout.LINE_AXIS) );

				//create status JTextArea:
				//This is already made in mainPanel -> no need to re-generate it here, just add statusPanel
				//to this processingPanel...
				//status = new JTextArea("Up to date.");

				//create panel for status:
				//statusPanel = new JPanel();
				//statusPanel.setLayout( new GridLayout(1,0) );
				//statusPanel.setLayout( new BoxLayout(statusPanel, BoxLayout.PAGE_AXIS) );

				//add status TextArea to statusPanel:
				//statusPanel.add(status);


				//create the Add algorithm button:
				//This will add the current algorithm and its options to the image processing procedure stack.
				addAlgorithmButton = new JButton("Add Algorithm");

				//Add listener to this button:
				//This must add the selected command and options to the procedureStack(at the end), and re-generate
				//the IWP main panel
				addAlgorithmButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						//  When pressed, add the current algorithm and its options to the
						//ImageProcessingProcedureStack object, and then return to the mainPanel:

						//Need to call the ImageProcessingProcedureStack in its thread
						//This needs to be called if the Preview checkbox is checked, as if so the current drawing
						//on activeImp is not correct -> want to remove this by calling the procedureStack,
						//which doesnt contain the preview, but does contain the correct set of image processing steps:

						boolean prevOn = false;
						if(previewButton.isSelected() == true) {
							//Call IPPT AFTER the mainPanel is filled, and deactivate the main panel buttons!
							//Just set flag prevOn to true:
							prevOn = true;
						}

						//add command to procedureStack - it is toggled off by default:
						procedureStack.addCommand(optionsPanel.commandTitle, optionsPanel.getOptionsString() );
						//and update the list:
						updateList();

						//Fill the mainPanel with its sub-panels and add to IWP:
						fillMainPanel();

						if(prevOn == true) {
							//NOW run the IPPT -> now the mainPanel has loaded..
							ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread( imageProcessingPanel );

							//run the thread:
							ippt.start();
						}

					}

				});

				//Set button inactive when first loading the imageProcessingPanel - only active when an appropriate
				//image processing algorithm is selected from the ComboBox:
				addAlgorithmButton.setEnabled(false);


				//create the Add algorithm button:
				//This will add the current algorithm and its options to the image processing procedure stack.
				cancelButton = new JButton("Canel");

				//Add listener to this button:
				//This should just re-generate the IWP main panel
				cancelButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						//  When pressed, return to the mainPanel:

						setIwpMainPanel();

					}

				});


				//create the preview checkbox:
				previewButton = new JCheckBox("preview:");

				//add listener to preview button:
				previewButton.addActionListener( new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						//  Apply just the procedure stack and then selected process to the activeImage -> activeImp

						if( previewButton.isSelected() ) {
							//Perform this in a separate thread to allow status to update as each command is run:
							ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread(optionsPanel.commandTitle, 
									optionsPanel.getOptionsString(),
									imageProcessingPanel );

							//run the thread:
							ippt.start();  // does this thread need to run the WHOLE Procedure Stack?  Or only the preview?
							//Only preview, as any procedure stack being displayed in activeImp will have been
							//set up on the mainPanel...  This happens now because command title and options are
							//passed in the ippt constructor!
						}

						else {

							//  need to undo the preview:
							//remove the activeImp display?!
							//NO -> need to put the activeImp back to its original state
							// This is achieved by just generating a new ippt with imageProcessingPanel (current active panel)
							//and running the thread!

							ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread( imageProcessingPanel );

							//run the thread:
							ippt.start();

						}

					}

				});



				//add preview and add buttons to previewAddButtonsPanel:
				previewAddButtonsPanel = new JPanel();
				//previewAddButtonsPanel.setLayout( new GridLayout(0,1) );
				previewAddButtonsPanel.setLayout( new BoxLayout(previewAddButtonsPanel, BoxLayout.PAGE_AXIS) );
				previewAddButtonsPanel.add(addAlgorithmButton);
				previewAddButtonsPanel.add(cancelButton);
				previewAddButtonsPanel.add(previewButton);

				//The the image processing panel with the combobox and panel:
				fillImageProcessingPanel();

			}

		});

		// make buttons fill the processingPanel:
		gbc.fill = gbc.BOTH;
		
		//Add buttons to panel:
		gbc.gridx = 0;
		gbc.gridy = 0;
		processingPanel.add(preProcessingButton, gbc);
		gbc.gridx = 0;
		gbc.gridy = 1;
		processingPanel.add(thresholdingButton, gbc);
		gbc.gridx = 0;
		gbc.gridy = 2;
		processingPanel.add(postProcessingButton, gbc);
		
		// reset fill:
		gbc.fill = gbc.NONE;

		//processingPanel.setBorder( new LineBorder(Color.BLACK,1,true) );
		
		// Add panel to Main Panel:
		mainPanel.add(processingPanel);

		// Add Vertical JSeparator to objManagerPanel:
		mainPanel.add(Box.createHorizontalStrut(2) );
		mainPanel.add( new JSeparator(SwingConstants.VERTICAL) );
		mainPanel.add(Box.createHorizontalStrut(2) );


		/**
		 * ADD LIST & COMPONENTS:
		 */

		//construct listPanel:
		listPanel = new JPanel();
		
		listPanel.setLayout( new GridBagLayout() );

		//Construct the listModel
		listModel = new DefaultListModel<String>();

		//Add some default elements for testing:
		//listModel.addElement("Gaussian3D x2y2z2");
		//listModel.addElement("OTSU");
		//listModel.addElement("Open"); 

		//Create the list and put it in a scroll pane.
		//create JList:
		list = new JList<String>(listModel);
		//only allow one item to be selected at a time.
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		//set the first item to be selected:
		//list.setSelectedIndex(0); //-> as list will be blank to start with, do not need this.

		if(newIPPS == false) {
			//the procedureStack may contain algorithms already with certain on and off states
			//must recall this and put it into the list object:
			//use clearAndFillList() in procedureStack:
			//procedureStack.clearAndFillList();

			//PERFORM IN THIS CLASS NOW -> no refs to items in this class in ImageProcessingProcedureStack!!!
			clearAndFillList();

			//also at end of this method run the IPPT!
		}


		list.addListSelectionListener( new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				// When different items are selected in the list, need to update the
				//procedureStack object to reflect the fact the selected item on the list is
				//potentially being edited.

				//It is in the buttons associated with this list that editing will happen to this
				//item in the procedureStack - but the procedureStack object needs to know which
				//item is currently selected!

				//Nothing is needed here, the list keeps track of what is selected on it, so can just call that from
				//procedure stack object to find out what item is selected:
				//	list.getSelectedIndex();

				//BUT UPDATE THE TOGGLE BUTTON:
				//if a new item is selected by the user, need to ensure the toggle button is updated to show correct state:
				//ONLY IF item on list is selected
				//prevents exception when removing items from list - where there is now no selection but this valueChanged()
				//listener method is still called...
				//IJ.showMessage("list index selection listener: "+list.getSelectedIndex() );

				if(list.isSelectionEmpty() == false) {
					updateToggle( list.getSelectedIndex() );
					//Updates the toggle button selected status and icon.
					//AND update the status JTextArea to show the options on the selected item:

					status.setText("Up to date. \n options: \n"+procedureStack.getOptions(list.getSelectedIndex() ) );
				}
				else {
					//if the list is not selected, do not need to update any toggle items!
					//set the status to be up to date with no options.
					status.setText("Up to date.");
				}

			}

		} );

		list.setVisibleRowCount(6);

		listScrollPane = new JScrollPane(list);
		
		listScrollPane.setMaximumSize(new Dimension(200, 200));
		listScrollPane.setMinimumSize (new Dimension (200,200));


		//Create buttons on left panel and add to listButtonPanel:
		leftListButtonPanel = new JPanel();
		
		leftListButtonPanel.setLayout( new GridBagLayout() );

		//LayoutManager:
		// leftListButtonPanel.setLayout( new GridLayout(0,1) );
		// leftListButtonPanel.setLayout( new BoxLayout(leftListButtonPanel, BoxLayout.PAGE_AXIS) );
		leftListButtonPanel.setLayout( new GridBagLayout() );


		//construct the buttons:
		upItem = new JButton( createImageIcon("/Icons/Up 100x100.png", "Icon Toggle", 24, 20) );
		downItem = new JButton( createImageIcon("/Icons/Down 100x100.png", "Icon Toggle", 20, 20) );

		//set tooltip:
		
		upItem.setToolTipText("Move selected item up the list");
		downItem.setToolTipText("Move selected item down the list");

		//add listeners:
		upItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				//  Move the selected item up one in the list.
				//ALSO update the procedureStack object to reflect the new order of image processing
				//procedures.
				//All done inside procedureStack:
				procedureStack.moveUp( list.getSelectedIndex() );

				//Now update the list - adjust the listModel:
				listModel.set(list.getSelectedIndex()-1, procedureStack.commandTitles.get(list.getSelectedIndex()-1) );
				listModel.set(list.getSelectedIndex(), procedureStack.commandTitles.get(list.getSelectedIndex()) );

				//And to be sure the list is displaying the correct info, clear and refill it:
				//make sure to re-select the original indexed item, which is now at index-1 now:
				//clearAndFillList(index-1);
				list.setSelectedIndex( list.getSelectedIndex()-1 );

				//AND update the procedureStack display on activeImp -> call a IPPT:
				//call in new thread, outside EDT:
				//only call if the moved item && item it has exchanged places with are BOTH toggled on:
				//as item has moved UP the list, need to check the ref BELOW IT on list -> this is the item it replaced
				//therefore +1 on the currently selected index:
				if(procedureStack.getToggle(list.getSelectedIndex()) == true && procedureStack.getToggle(list.getSelectedIndex()+1) == true) {
					ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread(mainPanel);
					ippt.start();
				}

			}

		});

		downItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				//  Move the selected item down one in the list.
				//ALSO update the procedureStack object to reflect the new order of image processing
				//procedures.
				//All done inside procedureStack:
				//need to pass the max index on list model too!
				procedureStack.moveDown( list.getSelectedIndex() );

				//Now update the list - adjust the listModel:
				listModel.set(list.getSelectedIndex()+1, procedureStack.commandTitles.get(list.getSelectedIndex()+1) );
				listModel.set(list.getSelectedIndex(), procedureStack.commandTitles.get(list.getSelectedIndex()) );

				//And to be sure the list is displaying the correct info, clear and refill it:
				//make sure to re-select the original indexed item, which is now at index-1 now:
				//clearAndFillList(index+1);
				list.setSelectedIndex(list.getSelectedIndex()+1);

				//AND update the procedureStack display on activeImp -> call a IPPT:
				//call in new thread, outside EDT:
				//only call if the moved item && item it has exchanged places with are BOTH toggled on:
				//as item has moved DOWN the list, need to check the ref ABOVE IT on list -> this is the item it replaced
				//therefore -1 on the currently selected index:
				if(procedureStack.getToggle(list.getSelectedIndex()) == true && procedureStack.getToggle(list.getSelectedIndex()-1) == true) {
					ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread(mainPanel);
					ippt.start();
				}

			}

		});

		//add buttons to listButtonPanel:
		gbc.gridx = 0;
		gbc.gridy = 0;
		leftListButtonPanel.add(upItem, gbc);
		gbc.gridx = 0;
		gbc.gridy = 1;
		leftListButtonPanel.add(downItem, gbc);


		//Create buttons on right panel and add to listButtonPanel:
		rightListButtonPanel = new JPanel();

		//Layout manager:
		// rightListButtonPanel.setLayout( new GridLayout(0,1) );
		// rightListButtonPanel.setLayout( new BoxLayout(rightListButtonPanel, BoxLayout.PAGE_AXIS) );
		rightListButtonPanel.setLayout( new GridBagLayout() );

		//construct the buttons:
		toggleIconOn = createImageIcon("/Icons/Toggle On 100x50.png", "Icon Toggle", 40, 20);
		toggleIconOff = createImageIcon("/Icons/Toggle Off 100x50.png", "Icon Toggle", 40, 20);

		toggleItem = new JToggleButton( toggleIconOn, true );
		//toggleItem = new JToggleButton( "toggle", true );
		editItem = new JButton( createImageIcon("/Icons/Edit 120x100 blank.png", "Icon Toggle", 24, 20) );
		deleteItem = new JButton( createImageIcon("/Icons/Delete 100x100 blank.png", "Icon Toggle", 20, 20) );

		//set tooltip:
		toggleItem.setToolTipText("Toggle this command On & Off");
		editItem.setToolTipText("Edit this commands options");
		deleteItem.setToolTipText("Delete this command from the procedure stack");


		//add appropriate listeners:
		//Toggle Item should update the procedureStack item when-ever the button is toggled.
		toggleItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// if toggled ON -> activate this image processing step.
				//if toggled OFF -> de-activate this image processing step.

				//BE SURE TO UPDATE THE procedureStack object with the relevant image processing steps!!

				//AND RUN IN SEPARATE THREAD!!!

				if(toggleItem.isSelected() == true ) {

					//set icon to the on icon:
					//toggleItem.setIcon(toggleIconOn);

					//if toggleItem is turned on, then run the toggleOn method in procedure stack:
					//Will adjust the toggleProperties at correct index and the toggleItem button
					//And it will run the procedure stack on the activeImp:

					procedureStack.toggleOn( list.getSelectedIndex() );		

					//and update the toggle button:
					updateToggle(list.getSelectedIndex() );

					//generate new IPPT & start:
					ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread(mainPanel);
					ippt.start();

				}

				else if(toggleItem.isSelected() == false ) {

					//set icon to the off icon:
					//toggleItem.setIcon(toggleIconOff);

					//if toggleItem is turned off, then run the toggleOff method in procedure stack:
					//Will adjust the toggleProperties at correct index and the toggleItem button
					//And it will run the procedure stack on the activeImp if necessary (i.e. if other
					//items are still on in procedure stack):

					try {
						procedureStack.toggleOff( list.getSelectedIndex() );

						//and update the toggle button:
						updateToggle(list.getSelectedIndex() );

						//generate new IPPT & start:
						ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread(mainPanel);
						ippt.start();

					} catch(ArrayIndexOutOfBoundsException ex) {
						IJ.showMessage("Select item to Toggle Off!");
					}

				}


			}

		});

		//editItem: This should take the user back to the relevant image processing panel to edit the currently
		//selected item on the list.
		editItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// This should take the user back to the panel where this item can be edited:
				// PRE-PROCESSING, THRESHOLDING, POST-PROCESSING panels.

				//Set up the correct processingPanel WITH the correct options panel AND correct options displayed:

				//NOTE:
				// procedureStack.getCommand( list.getSelectedIndex() ) == list.getSelectedValue()


				if( getImageProcessingPanelType(list.getSelectedValue()).equals("PreProcessing") ) {

					//setup the preprocessing panel:

					// Implement these additions:

					// 1. Set AlgorithmComboBox to -> selected list item (algorithm title).
					// 2. Set the Options Panel -> to the options panel required for selected list item (algorithm!)
					// 3. FILL the Options Panel -> with the option values from the list algorithm.

					//create new ComboBox using preProcessingStrings:
					algorithmComboBox = new JComboBox<String>( preProcessingStrings );

					//Add action listener:
					//This must update the options panel to include the options for the selected algorithm
					algorithmComboBox.addActionListener( new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							//  Depending which algorithm is selected, need to adjust the options fields:

							//get the command name selected in the ComboBox:
							@SuppressWarnings("unchecked")
							JComboBox<String> cb = (JComboBox<String>)e.getSource();

							//update the Options Panel to display correct options for this command:
							updateOptionsPanel( (String)cb.getSelectedItem()  );

							//fill the ProcessingPanel with it components including the updated OptionsPanel:
							Threshold_Manager.this.fillImageProcessingPanel();

						}

					});

					// 1. Set AlgorithmComboBox to -> selected list item (algorithm title).
					algorithmComboBox.setSelectedItem( list.getSelectedValue() ); 


					// 2. Set the Options Panel -> to the options panel required for selected list item (algorithm!)
					// 3. FILL the Options Panel -> with the option values from the list algorithm.

					//2 + 3 covered: create the CORRECT optionsPanel:
					//Use the updateOptionsPanel() method where you can pass custom options
					//updateOptionsPanel(SelectedItem, customOptions)..

					updateOptionsPanel(list.getSelectedValue(), procedureStack.getOptions(list.getSelectedIndex()) );
					//updateOptionsPanel(procedureStack.getCommand(list.getSelectedIndex()), procedureStack.getOptions(list.getSelectedIndex()) );


					//create the Add algorithm button:
					//This will add the current algorithm and its options to the image processing procedure stack.
					addAlgorithmButton = new JButton("Add Algorithm");

					//Add listener to this button:
					//This must add the selected command and options to the procedureStack(at the end), and re-generate
					//the IWP main panel
					addAlgorithmButton.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							//  When pressed, add the current algorithm and its options to the
							//ImageProcessingProcedureStack object, and then return to the mainPanel:

							//Need to call the ImageProcessingProcedureStack in its thread
							//This needs to be called if the Preview checkbox is checked, as if so the current drawing
							//on activeImp is not correct -> want to remove this by calling the procedureStack,
							//which doesnt contain the preview, but does contain the correct set of image processing steps:

							boolean prevOn = false;
							if(previewButton.isSelected() == true) {
								//Call IPPT AFTER the mainPanel is filled, and deactivate the main panel buttons!
								//Just set flag prevOn to true:
								prevOn = true;
							}

							//add command to procedureStack - it is toggled off by default:
							procedureStack.addCommand(optionsPanel.commandTitle, optionsPanel.getOptionsString() );
							//and update the list:
							updateList();

							//Fill the mainPanel with its sub-panels and add to IWP:
							fillMainPanel();

							if(prevOn == true) {
								//NOW run the IPPT -> now the mainPanel has loaded..
								ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread( imageProcessingPanel );

								//run the thread:
								ippt.start();
							}

						}

					});

					//Set button active when loading the imageProcessingPanel to edit an algorithm:
					addAlgorithmButton.setEnabled(true);


					//create the Add algorithm button:
					//This will add the current algorithm and its options to the image processing procedure stack.
					cancelButton = new JButton("Canel");

					//Add listener to this button:
					//This should just re-generate the IWP main panel
					cancelButton.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							//  When pressed, return to the mainPanel:

							setIwpMainPanel();

						}

					});


					//create the preview checkbox:
					previewButton = new JCheckBox("preview:");

					//add listener to preview button:
					previewButton.addActionListener( new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							//  Apply just the procedure stack and then selected process to the activeImage -> activeImp

							if( previewButton.isSelected() ) {
								//Perform this in a separate thread to allow status to update as each command is run:
								ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread(optionsPanel.commandTitle, 
										optionsPanel.getOptionsString(),
										imageProcessingPanel );

								//run the thread:
								ippt.start();  // does this thread need to run the WHOLE Procedure Stack?  Or only the preview?
								//Only preview, as any procedure stack being displayed in activeImp will have been
								//set up on the mainPanel...  This happens now because command title and options are
								//passed in the ippt constructor!
							}

							else {

								//  need to undo the preview:
								//remove the activeImp display?!
								//NO -> need to put the activeImp back to its original state
								// This is achieved by just generating a new ippt with imageProcessingPanel (current active panel)
								//and running the thread!

								ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread( imageProcessingPanel );

								//run the thread:
								ippt.start();

							}

						}

					});



					//add preview and add buttons to previewAddButtonsPanel:
					previewAddButtonsPanel = new JPanel();
					//previewAddButtonsPanel.setLayout( new GridLayout(0,1) );
					previewAddButtonsPanel.setLayout( new BoxLayout(previewAddButtonsPanel, BoxLayout.PAGE_AXIS) );
					previewAddButtonsPanel.add(addAlgorithmButton);
					previewAddButtonsPanel.add(cancelButton);
					previewAddButtonsPanel.add(previewButton);

					//remove item from list before removing list from the panel:
					//listModel.remove( list.getSelectedIndex() );
					//procedureStack.remove(list.getSelectedIndex() );

					//The the image processing panel with the combobox and panel:
					fillImageProcessingPanel();	

				}

				if( getImageProcessingPanelType(list.getSelectedValue()).equals("Thresholding") ) {


					//setup the thresholding panel:

					// Implement these additions:

					// 1. Set AlgorithmComboBox to -> selected list item (algorithm title).
					// 2. Set the Options Panel -> to the options panel required for selected list item (algorithm!)
					// 3. FILL the Options Panel -> with the option values from the list algorithm.

					//create new ComboBox using preProcessingStrings:
					algorithmComboBox = new JComboBox<String>( thresholdingStrings );

					//Add action listener:
					//This must update the options panel to include the options for the selected algorithm
					algorithmComboBox.addActionListener( new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							//  Depending which algorithm is selected, need to adjust the options fields:

							//get the command name selected in the ComboBox:
							@SuppressWarnings("unchecked")
							JComboBox<String> cb = (JComboBox<String>)e.getSource();

							//update the Options Panel to display correct options for this command:
							updateOptionsPanel( (String)cb.getSelectedItem()  );

							//fill the ProcessingPanel with it components including the updated OptionsPanel:
							Threshold_Manager.this.fillImageProcessingPanel();

						}

					});

					// 1. Set AlgorithmComboBox to -> selected list item (algorithm title).
					algorithmComboBox.setSelectedItem( list.getSelectedValue() ); 


					// 2. Set the Options Panel -> to the options panel required for selected list item (algorithm!)
					// 3. FILL the Options Panel -> with the option values from the list algorithm.

					//2 + 3 covered: create the CORRECT optionsPanel:
					//Use the updateOptionsPanel() method where you can pass custom options
					//updateOptionsPanel(SelectedItem, customOptions)..

					updateOptionsPanel(list.getSelectedValue(), procedureStack.getOptions(list.getSelectedIndex()) );
					//updateOptionsPanel(procedureStack.getCommand(list.getSelectedIndex()), procedureStack.getOptions(list.getSelectedIndex()) );


					//create the Add algorithm button:
					//This will add the current algorithm and its options to the image processing procedure stack.
					addAlgorithmButton = new JButton("Add Algorithm");

					//Add listener to this button:
					//This must add the selected command and options to the procedureStack(at the end), and re-generate
					//the IWP main panel
					addAlgorithmButton.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							//  When pressed, add the current algorithm and its options to the
							//ImageProcessingProcedureStack object, and then return to the mainPanel:

							//Need to call the ImageProcessingProcedureStack in its thread
							//This needs to be called if the Preview checkbox is checked, as if so the current drawing
							//on activeImp is not correct -> want to remove this by calling the procedureStack,
							//which doesnt contain the preview, but does contain the correct set of image processing steps:

							boolean prevOn = false;
							if(previewButton.isSelected() == true) {
								//Call IPPT AFTER the mainPanel is filled, and deactivate the main panel buttons!
								//Just set flag prevOn to true:
								prevOn = true;
							}

							//add command to procedureStack - it is toggled off by default:
							procedureStack.addCommand(optionsPanel.commandTitle, optionsPanel.getOptionsString() );
							//and update the list:
							updateList();

							//Fill the mainPanel with its sub-panels and add to IWP:
							fillMainPanel();

							if(prevOn == true) {
								//NOW run the IPPT -> now the mainPanel has loaded..
								ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread( imageProcessingPanel );

								//run the thread:
								ippt.start();
							}

						}

					});

					//Set button active when loading the imageProcessingPanel to edit an algorithm:
					addAlgorithmButton.setEnabled(true);


					//create the Add algorithm button:
					//This will add the current algorithm and its options to the image processing procedure stack.
					cancelButton = new JButton("Canel");

					//Add listener to this button:
					//This should just re-generate the IWP main panel
					cancelButton.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							//  When pressed, return to the mainPanel:

							setIwpMainPanel();

						}

					});


					//create the preview checkbox:
					previewButton = new JCheckBox("preview:");

					//add listener to preview button:
					previewButton.addActionListener( new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							//  Apply just the procedure stack and then selected process to the activeImage -> activeImp

							if( previewButton.isSelected() ) {
								//Perform this in a separate thread to allow status to update as each command is run:
								ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread(optionsPanel.commandTitle, 
										optionsPanel.getOptionsString(),
										imageProcessingPanel );

								//run the thread:
								ippt.start();  // does this thread need to run the WHOLE Procedure Stack?  Or only the preview?
								//Only preview, as any procedure stack being displayed in activeImp will have been
								//set up on the mainPanel...  This happens now because command title and options are
								//passed in the ippt constructor!

							}

							else {

								//  need to undo the preview:
								//remove the activeImp display?!
								//NO -> need to put the activeImp back to its original state
								// This is achieved by just generating a new ippt with imageProcessingPanel (current active panel)
								//and running the thread!

								ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread( imageProcessingPanel );

								//run the thread:
								ippt.start();

							}

						}

					});



					//add preview and add buttons to previewAddButtonsPanel:
					previewAddButtonsPanel = new JPanel();
					//previewAddButtonsPanel.setLayout( new GridLayout(0,1) );
					previewAddButtonsPanel.setLayout( new BoxLayout(previewAddButtonsPanel, BoxLayout.PAGE_AXIS) );
					previewAddButtonsPanel.add(addAlgorithmButton);
					previewAddButtonsPanel.add(cancelButton);
					previewAddButtonsPanel.add(previewButton);

					//remove item from list before removing list from the panel:
					//listModel.remove( list.getSelectedIndex() );
					//procedureStack.remove(list.getSelectedIndex() );

					//The the image processing panel with the combobox and panel:
					fillImageProcessingPanel();	


				}

				if( getImageProcessingPanelType(list.getSelectedValue()).equals("PostProcessing") ) {


					//setup the postprocessing panel:

					// Implement these additions:

					// 1. Set AlgorithmComboBox to -> selected list item (algorithm title).
					// 2. Set the Options Panel -> to the options panel required for selected list item (algorithm!)
					// 3. FILL the Options Panel -> with the option values from the list algorithm.

					//create new ComboBox using preProcessingStrings:
					algorithmComboBox = new JComboBox<String>( postProcessingStrings );

					//Add action listener:
					//This must update the options panel to include the options for the selected algorithm
					algorithmComboBox.addActionListener( new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							//  Depending which algorithm is selected, need to adjust the options fields:

							//get the command name selected in the ComboBox:
							@SuppressWarnings("unchecked")
							JComboBox<String> cb = (JComboBox<String>)e.getSource();

							//update the Options Panel to display correct options for this command:
							updateOptionsPanel( (String)cb.getSelectedItem()  );

							//fill the ProcessingPanel with it components including the updated OptionsPanel:
							Threshold_Manager.this.fillImageProcessingPanel();

						}

					});

					// 1. Set AlgorithmComboBox to -> selected list item (algorithm title).
					algorithmComboBox.setSelectedItem( list.getSelectedValue() ); 


					// 2. Set the Options Panel -> to the options panel required for selected list item (algorithm!)
					// 3. FILL the Options Panel -> with the option values from the list algorithm.

					//2 + 3 covered: create the CORRECT optionsPanel:
					//Use the updateOptionsPanel() method where you can pass custom options
					//updateOptionsPanel(SelectedItem, customOptions)..

					updateOptionsPanel(list.getSelectedValue(), procedureStack.getOptions(list.getSelectedIndex()) );
					//updateOptionsPanel(procedureStack.getCommand(list.getSelectedIndex()), procedureStack.getOptions(list.getSelectedIndex()) );


					//create the Add algorithm button:
					//This will add the current algorithm and its options to the image processing procedure stack.
					addAlgorithmButton = new JButton("Add Algorithm");

					//Add listener to this button:
					//This must add the selected command and options to the procedureStack(at the end), and re-generate
					//the IWP main panel
					addAlgorithmButton.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							//  When pressed, add the current algorithm and its options to the
							//ImageProcessingProcedureStack object, and then return to the mainPanel:

							//Need to call the ImageProcessingProcedureStack in its thread
							//This needs to be called if the Preview checkbox is checked, as if so the current drawing
							//on activeImp is not correct -> want to remove this by calling the procedureStack,
							//which doesnt contain the preview, but does contain the correct set of image processing steps:

							boolean prevOn = false;
							if(previewButton.isSelected() == true) {
								//Call IPPT AFTER the mainPanel is filled, and deactivate the main panel buttons!
								//Just set flag prevOn to true:
								prevOn = true;
							}

							//add command to procedureStack - it is toggled off by default:
							procedureStack.addCommand(optionsPanel.commandTitle, optionsPanel.getOptionsString() );
							//and update the list:
							updateList();

							//Fill the mainPanel with its sub-panels and add to IWP:
							fillMainPanel();

							if(prevOn == true) {
								//NOW run the IPPT -> now the mainPanel has loaded..
								ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread( imageProcessingPanel );

								//run the thread:
								ippt.start();
							}

						}

					});

					//Set button active when loading the imageProcessingPanel to edit an algorithm:
					addAlgorithmButton.setEnabled(true);


					//create the Add algorithm button:
					//This will add the current algorithm and its options to the image processing procedure stack.
					cancelButton = new JButton("Canel");

					//Add listener to this button:
					//This should just re-generate the IWP main panel
					cancelButton.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							//  When pressed, return to the mainPanel:

							setIwpMainPanel();

						}

					});


					//create the preview checkbox:
					previewButton = new JCheckBox("preview:");

					//add listener to preview button:
					previewButton.addActionListener( new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							//  Apply just the procedure stack and then selected process to the activeImage -> activeImp

							if( previewButton.isSelected() ) {
								//Perform this in a separate thread to allow status to update as each command is run:
								ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread(optionsPanel.commandTitle, 
										optionsPanel.getOptionsString(),
										imageProcessingPanel );

								//run the thread:
								ippt.start();  // does this thread need to run the WHOLE Procedure Stack?  Or only the preview?
								//Only preview, as any procedure stack being displayed in activeImp will have been
								//set up on the mainPanel...  This happens now because command title and options are
								//passed in the ippt constructor!
							}

							else {

								// need to undo the preview:
								//remove the activeImp display?!
								//NO -> need to put the activeImp back to its original state
								// This is achieved by just generating a new ippt with imageProcessingPanel (current active panel)
								//and running the thread!

								ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread( imageProcessingPanel );

								//run the thread:
								ippt.start();

							}

						}

					});



					//add preview and add buttons to previewAddButtonsPanel:
					previewAddButtonsPanel = new JPanel();
					//previewAddButtonsPanel.setLayout( new GridLayout(0,1) );
					previewAddButtonsPanel.setLayout( new BoxLayout(previewAddButtonsPanel, BoxLayout.PAGE_AXIS) );
					previewAddButtonsPanel.add(addAlgorithmButton);
					previewAddButtonsPanel.add(cancelButton);
					previewAddButtonsPanel.add(previewButton);

					//remove item from list before removing list from the panel:
					//listModel.remove( list.getSelectedIndex() );
					//procedureStack.remove(list.getSelectedIndex() );

					//The the image processing panel with the combobox and panel:
					fillImageProcessingPanel();	


				}

				//ALSO remove the item from the list -> will need to be re-added after editing by user.

				//ALSO update the procedureStack to remove this item (will be re-added when the item is re-added
				//by user after editing.
				//IJ.showMessage("index of list: "+list.getSelectedIndex() );

				//extract selectedIndex - as gets set to 0 once the selection is removed from listModel!
				int selectedIndex = list.getSelectedIndex();
				listModel.remove( selectedIndex );
				procedureStack.remove( selectedIndex );

				//And run the procedureStack in its thread to ensure the image is up to date with displaying correct
				//activeChannel processing steps:

				ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread(imageProcessingPanel);
				ippt.start();

			}

		});

		deleteItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// Remove the item from the list.
				//ALSO remember to remove this item from the procedureStack!

				//extract selectedIndex - as gets set to 0 once the selection is removed from listModel!
				int selectedIndex = list.getSelectedIndex();
				listModel.remove( selectedIndex );
				procedureStack.remove( selectedIndex );

				//And run the procedureStack in its thread to ensure the image is up to date with displaying correct
				//activeChannel processing steps:
				ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread(mainPanel);
				ippt.start();

			}

		});

		//add buttons to listButtonPanel:
		gbc.gridx = 0;
		gbc.gridy = 0;
		rightListButtonPanel.add(toggleItem, gbc);
		gbc.gridx = 0;
		gbc.gridy = 1;
		rightListButtonPanel.add(editItem, gbc);
		gbc.gridx = 0;
		gbc.gridy = 2;
		rightListButtonPanel.add(deleteItem, gbc);


		//add components to listPanel:
		gbc.gridx = 0;
		gbc.gridy = 0;
		listPanel.add(leftListButtonPanel, gbc);
		gbc.gridx = 1;
		gbc.gridy = 0;
		listPanel.add(listScrollPane, gbc);
		gbc.gridx = 2;
		gbc.gridy = 0;
		listPanel.add(rightListButtonPanel, gbc);
		
		// add to main panel:
		mainPanel.add(listPanel);

		//Add a border to the listPanel:
		// listPanel.setBorder(new LineBorder(Color.BLACK,1,true));
		
		// Add Vertical JSeparator to objManagerPanel:
		mainPanel.add(Box.createHorizontalStrut(2) );
		mainPanel.add( new JSeparator(SwingConstants.VERTICAL) );
		mainPanel.add(Box.createHorizontalStrut(2) );



		/**
		 * ADD STATUS COMP + PANEL:
		 */

		//create status JTextArea:
		//This is to update status when applying an algorithm.
		status = new JTextArea(6,25);
		status.setText("Up to date.");

		statusScrollPane = new JScrollPane(status);
		statusScrollPane.setMaximumSize(new Dimension(200, 200));
		statusScrollPane.setMinimumSize (new Dimension (200,200));


		//create panel for status:
		statusPanel = new JPanel();
		//statusPanel.setLayout( new GridLayout(1,0) );
		//statusPanel.setLayout( new BoxLayout(statusPanel, BoxLayout.PAGE_AXIS) );
		statusPanel.setLayout( new GridBagLayout() );

		//add status TextArea to statusPanel:
		gbc.gridx = 0;
		gbc.gridy = 0;
		statusPanel.add(statusScrollPane, gbc);
		
		mainPanel.add(statusPanel);



		/**
		 * ADD NAVIGATION AND LOAD SAVE COMPONENTS:
		 */

		// construct panel:
		navLoadSavePanel = new JPanel();

		// navLoadSavePanel.setLayout( new GridLayout(0,1) );
		// navLoadSavePanel.setLayout( new BoxLayout(navLoadSavePanel, BoxLayout.PAGE_AXIS) );
		navLoadSavePanel.setLayout( new GridBagLayout() );

		//construct the buttons:
		nextButton = new JButton( createImageIcon("/Icons/Next 100x100.png", "Icon Toggle", 40, 40) );
		previousButton = new JButton( createImageIcon("/Icons/Previous 100x100.png", "Icon Toggle", 40, 40) );

		nextButton.setToolTipText("Move to the next image");
		previousButton.setToolTipText("Move to the previous image");

		//add listeners to the buttons:
		nextButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// Move to the next image in the selected directory.

				// Perform in NEW THREAD - to allow Log to update:
				
				Thread t = new Thread() {

					@Override
					public void run() {
						
						//When a new image is brought in, should automatically apply any image processing steps from
						//procedureStack to its first channel.

						////IJ.log("current file index B4 inc: "+dw.getCurrentFileIndex() );
						////IJ.log("total file count B4 inc: "+dw.getTotalFileCount() );

						//close current IWP window:
						IWP.getWindow().dispose(); //dispose the iw.
						IWP.setModal(false); //re-activate the ij window.
						IWP.iw.removeWindowListener( imageWindowAdapter );
												
						IJ.log("");
						IJ.log("");
						IJ.log("*******************************");
						IJ.wait(50);
						IJ.log("");
						IJ.log("NEXT BUTTON:");
						IJ.log("");
						
						IJ.wait(50);
						
						IJ.log("Shutting Down Image Window...");
						IJ.log("===============================");
						IJ.log("   removing window listeners...");
						IWP.shutdownImageWindowWithPanel();
						IWP = null;  // -> set to null to garbage collect.
						// IWP.dispose();

						//Retrieve the imp for the NEXT image:

						//first, increase both currentFileIndex and TotalFileCount by 1:
						//dw.incrementCurrentFileIndex();
						dw.incrementTotalFileCountAndCurrentFileIndex();
						//this increments totalFileCount and currentFileIndex but in a way to ensure currentFileIndex
						//is ALWAYS less than totalFileCount!

						////IJ.log("current file index: "+dw.getCurrentFileIndex() );
						////IJ.log("total file count: "+dw.getTotalFileCount() );
						//This is required to increment totalFileCount, otherwise currentFileIndex will be too high,
						//and the algorithm will terminate!
						//There will still be no more images after this one, and the process() method will not grab any more
						//imps from DW as the totalFileCount has been artificially set...
						
						IJ.log("   removing panel listeners...");
						
						// Remove all Listeners from objects in the current IWP:
							// Prevents Memory Leak! 
						removeListeners();						
						activeImp = null;
						
						IJ.log("");
						IJ.log("Complete.");
						IJ.log("");
						
						IJ.wait(50);
						
						IJ.showStatus("Opening Next Image...");
						IJ.log("Opening Next Image...");
						IJ.log("===============================");

						//then, just retrieve the currentImp on fileselector 0:
						ImagePlus imp = dw.getCurrentImp(0);
						IJ.log("");
						IJ.log("Complete.");
						IJ.log("");
						
						IJ.wait(50);

						//generate new IWP & activeImp:

						IJ.log("Generating New Image Window...");
						IJ.log("===============================");
						IJ.log("   building image window...");

						//create new IWP with the new imp retrieved:
						//Make sure to set the correct image as the dup channel at end!! - use channelNum int
						IWP = new ImageWindowWithPanel(imp, new Panel(), activeChannelNum, bitDepth );

						IJ.log("   editing active channel...");

						//Use the IWP returnChannel() method to get an ImagePlus object which contains all the IPs from the LAST CHANNEL:
						//This is the ACTIVE IMP - ie the ImagePlus that represents the duplicated channel that is stuck to the end of
						//the original imp, the imp which image processing steps will be performed on..
						// activeImp = IWP.returnChannelDuplicate( IWP.getChannels() );
						activeImp = IWP.returnChannel( IWP.getChannels() );

						//Set active channel to last channel - the one NOT DISPLAYED & which will contain the processed imp:
						//This just means pixel data is being manipulated on the last channel - a moot point..
						//Really required so the "Grays" plugin below acts on this channel..
						IWP.setActiveChannel( IWP.getChannels() );

						//Make the LUT of this last channel White on both projected and original imp:
						IJ.run(IWP.getImagePlus(), "Grays", "");

						//to set original imps last channel to GREYS LUT - need to set its channel to the last channel:
						IWP.getOriginalImagePlus().setC( IWP.getChannels() );
						IJ.run(IWP.getOriginalImagePlus(), "Grays", "");

						//re-set the original imp channel to the first channel:
						IWP.getOriginalImagePlus().setC( 1 );
						//This sets the last channel - representing the active channel - to the GREYS LUT - i.e. black to white.
						
						IJ.log("   adding window listener...");
						
						// Setting the close operation to include saving the current Arff Dataset:
						IWP.iw.addWindowListener(imageWindowAdapter);

						//Then need to re-generate the ThresholdManager panel:
						//Use the mainPanel as it is!!  Put this onto the IWP panel!
						//Are the references all teh same though?!  NO!
						//Easier just to setup the whole thing from the start?  Need to record the current procedure stack (i.e do
						//NOT make a new one when running setupThresholdManager -> use boolean!

						IJ.log("   building Threshold Manager panel...");
						
						setupThresholdManager(false);
						//sets up the panel below the IWP -> but KEEPS the procedureStack as it was set up before!
						IJ.log("");
						IJ.log("Complete.");
						IJ.log("");
						
					}
					
				};
				
				t.start();
				
			}

		});
		previousButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// Move to the previous image in the selected directory.
				
				// Perform in NEW THREAD - to allow Log to update:
				
				Thread t = new Thread() {
					
					@Override
					public void run() {
						
						//When a new image is brought in, should automatically apply any image processing steps from
						//procedureStack to its first channel.

						//When a new image is brought in, should automatically apply any image processing steps from
						//procedureStack to its first channel.

						//IJ.log("current file index B4 inc: "+dw.getCurrentFileIndex() );
						//IJ.log("total file count B4 inc: "+dw.getTotalFileCount() );
						
						//close current IWP window:
						IWP.getWindow().dispose(); //dispose the iw.
						IWP.setModal(false); //re-activate the ij window.
						
						IJ.log("");
						IJ.log("");
						IJ.log("*******************************");
						IJ.wait(50);
						IJ.log("");
						IJ.log("PREVIOUS BUTTON:");
						IJ.log("");
						
						IJ.wait(50);
						
						IJ.log("Shutting Down Image Window...");
						IJ.log("===============================");
						IJ.log("   removing window listeners...");
						
						IWP.shutdownImageWindowWithPanel();
						IWP = null;  // -> set to null to garbage collect.
						// IWP.dispose();

						//Retrieve the imp for the NEXT image:

						//first, increase both currentFileIndex and TotalFileCount by 1:
						//dw.incrementCurrentFileIndex();
						dw.decrementTotalFileCountAndCurrentFileIndex();
						//this increments totalFileCount and currentFileIndex but in a way to ensure currentFileIndex
						//is ALWAYS less than totalFileCount!

						//IJ.log("current file index: "+dw.getCurrentFileIndex() );
						//IJ.log("total file count: "+dw.getTotalFileCount() );
						//This is required to increment totalFileCount, otherwise currentFileIndex will be too high,
						//and the algorithm will terminate!
						//There will still be no more images after this one, and the process() method will not grab any more
						//imps from DW as the totalFileCount has been artificially set...
						
						IJ.log("   removing panel listeners...");
						
						// Remove all Listeners from objects in the current IWP:
							// Prevents Memory Leak! 
						removeListeners();
						activeImp = null; // make sure to set activeImp to null - to allow it to be garbage collected!
						
						IJ.log("");
						IJ.log("Complete.");
						IJ.log("");
						
						IJ.wait(100);
						
						IJ.showStatus("Opening Previous Image...");
						IJ.log("Opening Previous Image...");
						IJ.log("===============================");
						
						//then, just retrieve the currentImp on fileselector 0:
						ImagePlus imp = dw.getCurrentImp(0);
						IJ.log("");
						IJ.log("Complete.");
						IJ.log("");
						
						IJ.wait(100);

						//generate new IWP & activeImp:

						IJ.log("Generating New Image Window...");
						IJ.log("===============================");
						IJ.log("   building image window...");

						//create new IWP with the new imp retrieved:
						//Make sure to set the correct image as the dup channel at end!! - use channelNum int
						IWP = new ImageWindowWithPanel(imp, new Panel(), activeChannelNum, bitDepth );
						
						IJ.log("   editing active channel...");

						//Use the IWP returnChannel() method to get an ImagePlus object which contains all the IPs from the LAST CHANNEL:
						//This is the ACTIVE IMP - ie the ImagePlus that represents the duplicated channel that is stuck to the end of
						//the original imp, the imp which image processing steps will be performed on..
						// activeImp = IWP.returnChannelDuplicate( IWP.getChannels() );
						activeImp = IWP.returnChannel( IWP.getChannels() );

						//Set active channel to last channel - the one NOT DISPLAYED & which will contain the processed imp:
						//This just means pixel data is being manipulated on the last channel - a moot point..
						//Really required so the "Grays" plugin below acts on this channel..
						IWP.setActiveChannel( IWP.getChannels() );

						//Make the LUT of this last channel White on both projected and original imp:
						IJ.run(IWP.getImagePlus(), "Grays", "");

						//to set original imps last channel to GREYS LUT - need to set its channel to the last channel:
						IWP.getOriginalImagePlus().setC( IWP.getChannels() );
						IJ.run(IWP.getOriginalImagePlus(), "Grays", "");

						//re-set the original imp channel to the first channel:
						IWP.getOriginalImagePlus().setC( 1 );
						//This sets the last channel - representing the active channel - to the GREYS LUT - i.e. black to white.
						
						IJ.log("   adding window listener...");
						
						// Setting the close operation to include saving the current Arff Dataset:
						IWP.iw.addWindowListener(imageWindowAdapter);

						//Then need to re-generate the ThresholdManager panel:
						//Use the mainPanel as it is!!  Put this onto the IWP panel!
						//Are the references all teh same though?!  NO!
						//Easier just to setup the whole thing from the start?  Need to record the current procedure stack (i.e do
						//NOT make a new one when running setupThresholdManager -> use boolean!

						IJ.log("   building Threshold Manager panel...");
						
						setupThresholdManager(false);
						//sets up the panel below the IWP -> but KEEPS the procedureStack as it was set up before!
						IJ.log("");
						IJ.log("Complete.");
						IJ.log("");
					}
					
				};

				t.start();

			}

		});



		//contruct the buttons:
		loadButton = new JButton( createImageIcon("/Icons/Load 100x100 2.png", "Icon Toggle", 40, 40) );
		saveButton = new JButton( createImageIcon("/Icons/Save 100x100.png", "Icon Toggle", 40, 40) );

		loadButton.setToolTipText("Load an image thresholding procedure stack");
		saveButton.setToolTipText("Save an image thresholding procedure stack");

		//add listeners to the buttons:
		loadButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// Load a previous image processing procedure stack.

				//Need to load the procedureStack object, and add all items to the List

				//First, open a dialog which contains all the potential procedureStack titles to load from:

				//need to build a simple DialogWindow with a JLabel and JComboBox containing all the
				//potential procedureStacks to load...
				//Object[] possibilities = {"ham", "spam", "yam"};
				//Object[] possibilities = procedureStack.loadStackTitles();

				File file = null;	
				try {
					file = new File(StereoMateSettings.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
				} catch (URISyntaxException e1) {
					e1.printStackTrace();
				}
				//Retrieve parent, and then formulate new file object pointing to the .thresholdProcedures DIR & the procedureTitle file:
				file = file.getParentFile();
				File thresholdProceduresFile = new File(file.getAbsolutePath() + File.separator + ".thresholdProcedures" + File.separator);

				//FileSystemView fsv = new SingleRootFileSystemView( thresholdProceduresFile );

				JFileChooser fc = new JFileChooser( thresholdProceduresFile );

				//fc.setSelectedFile(new File( procedureStack.getDefaultName() ) );

				int returnVal = fc.showDialog( IWP.getWindow(), "Load" );

				if(returnVal == JFileChooser.APPROVE_OPTION) {


					String stackTitle = fc.getSelectedFile().getName();



					//String stackTitle = (String)JOptionPane.showInputDialog(
					//                    IWP.getWindow(),
					//                    "Please choose a procedure stack:",
					//                    "Load Procedure Stack",
					//                    JOptionPane.PLAIN_MESSAGE,
					//                    createImageIcon("/Icons/Load 100x100 2.png", "Icon Toggle", 40, 40),
					//                    possibilities,
					//                    possibilities[0]);

					//load new procedureStack into he procedureStack variable:
					procedureStack = procedureStack.loadStack(stackTitle);
					//need to set the procedureStack's status variable to this classes status variable:
					//To allow the procedureStack to update this classes JTextArea (status) when running
					//the procedure stack!
					//procedureStack.setTextArea(status);

					//Now call clearAndFillList() in this class -> to clear and fill list obj!
					clearAndFillList();

					//then apply this procedure stack to current IWP image:
					//just run a IPPS:

					ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread(mainPanel, true);
					//set togOff to true, to ensure the buttons are re-activated
					ippt.start();

				}

			}

		});


		saveButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// Save this procedureStack to XML file in ImageJ.

				//Need to save the procedureStack object as XML for future recall
				//see ImageProcessingProcedureStack class.. Should implement this here in a method!

				//First, open a dialog for user to select a NAME for the procedure:
				//String name = IJ.getString("Save Procedure - Name:", procedureStack.getDefaultName() );

				File file = null;	
				try {
					file = new File(StereoMateSettings.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
				} catch (URISyntaxException e1) {
					e1.printStackTrace();
				}
				//Retrieve parent, and then formulate new file object pointing to the .thresholdProcedures DIR & the procedureTitle file:
				file = file.getParentFile();
				File thresholdProceduresFile = new File(file.getAbsolutePath() + File.separator + ".thresholdProcedures" + File.separator);

				//FileSystemView fsv = new SingleRootFileSystemView( thresholdProceduresFile );

				//JFileChooser fc = new JFileChooser( thresholdProceduresFile );
				
				String thresholdProceduresName = (String)JOptionPane.showInputDialog(IWP.getWindow(), "Select Procedure Stack file name:",
											"Save Procedure Stack",JOptionPane.PLAIN_MESSAGE, null, null, procedureStack.getDefaultName() );

				if(thresholdProceduresName!=null && (thresholdProceduresName.length() > 0)) {
					procedureStack.saveStack(thresholdProceduresName, dw.getOutputParentFile() );
					// TODO Need to determine the outputDir path to save procedureStack to the outputDir XXX_TM
				}
				
				
				//fc.setSelectedFile(new File( procedureStack.getDefaultName() ) );

				//int returnVal = fc.showSaveDialog( IWP.getWindow() );

				//if(returnVal == JFileChooser.APPROVE_OPTION) {

					//thresholdProceduresFile = fc.getSelectedFile();

					//String name = thresholdProceduresFile.getName();
					//then pass this name into the saveStack method
					//This will save the stack with this name into the .thresholdProcedures directory, in the StereoMate
					//directory:
					//procedureStack.saveStack(name);


				//}

			}

		});



		//Add buttons to panel:
		gbc.gridx = 0;
		gbc.gridy = 0;
		navLoadSavePanel.add(previousButton, gbc);
		gbc.gridx = 1;
		gbc.gridy = 0;
		navLoadSavePanel.add(nextButton, gbc);

		//Add buttons to panel:
		gbc.gridx = 0;
		gbc.gridy = 1;
		navLoadSavePanel.add(loadButton, gbc);
		gbc.gridx = 1;
		gbc.gridy = 1;
		navLoadSavePanel.add(saveButton, gbc);

		//navLoadSavePanel.setBorder(new LineBorder(Color.BLACK,1,true));
		
		// Add to main Panel:
		mainPanel.add(navLoadSavePanel);

		// CONSTRUCT MAINPANEL & ADD TO IWP:

		//Add to mainPanel:
		// mainPanel.add(activeChannelPanel);
		// mainPanel.add(processingPanel);
		// mainPanel.add(listPanel);
		// mainPanel.add(navigationPanel);
		// mainPanel.add(loadSavePanel);  //both added now in navLoadSavePanel..
		// mainPanel.add(statusPanel);
		// mainPanel.add(navLoadSavePanel);


		//add mainPanel to IWP panel:
		IWP.addComponent(scrollPane);

		//layout the IWP:
		IWP.layoutWindow();

		//IJ.log("current file index: "+dw.getCurrentFileIndex() );
		//IJ.log("total file count: "+dw.getTotalFileCount() );

		if(newIPPS == false) {
			//run the IPPT with mainPanel passed if this is using the old procedureStack:
			ImageProcessingProcedureThread ippt = new ImageProcessingProcedureThread(mainPanel);
			ippt.start();

		}

	}
	
	/**
	 * This method will filter the ImageStack to return a sub-stack consisting of the series of ImageProcessors
	 * which corresponds to the channelNumber, out of the total number of channels.  This method returns an
	 * ImageStack where the ImageProcessors have been DUPLICATED - they are separate images, and not referencing
	 * the original ImageProcessors from the original ImageStack!
	 * <p>
	 * The image stack is simply a series of ImageProcessors.  These are arranged in a nested loop of channels,
	 * slices, frames.  Therefore, to get every slice of the first channel when total number of channels is 3,
	 * the 1st, 4th, 7th etc ImageProcessor must be duplicated.
	 * <p>
	 * This assumes there is no frames in the image - guaranteed when images are parsed through the DialogWindow
	 * class in StereoMate.
	 * 
	 * @param is The ImageStack to extract from.
	 * @param channelNumber The Channel Number to be extracted - must be between 1 and totalNumberOfChannels.
	 * @param totalNumberOfChannels The total number of channels in this ImageStack.
	 * @return
	 */
	public ImageStack filterImageStack2(ImageStack is, int channelNumber, int totalNumberOfChannels ) {
		
		//Generate a new ImageStack to return - with correct width and height!:
			//is must have at least one processor, so use this as ref to get width and height
		ImageStack is2 = new ImageStack( is.getProcessor(1).getWidth(), is.getProcessor(1).getHeight() );

		//Loop through the parsed ImageStack - start at channelNumber, go to ImageStack size,
			//and step through the stack as number of channels:
				//Will ensure each IP is from the designated channel
		for(int a=channelNumber; a<=is.getSize(); a=a+totalNumberOfChannels) {
			////IJ.log("filterImageStack loop a: "+a);
			//Duplicate the IP from the ImageStack, and copy into the new ImageStack:
			is2.addSlice( is.getProcessor(a).duplicate() );
		}
		
		//Return the new ImageStack:
		return is2;
	}
	
	
	/**
	 * Merges two stacks to form a 2+ channel image.  
	 * It will put the second imagestack at the end of the first imagestack to form a new channel.
	 * It assumes the second imageStack is a single channel.
	 */
	public ImageStack mergeStacks2( ImageStack iwpStack, ImageStack thresholdedStack ) {
		
		//create new image stack to fill with the two stacks to merge:
		ImageStack is2 = new ImageStack(iwpStack.getProcessor(1).getWidth(), iwpStack.getProcessor(1).getHeight() );
		
		//Get the difference in size between IWP and thresholded stacks:
			//this is used to determine when to insert the thresholdedStack IPs in loop below:
		int diff = iwpStack.getSize() / thresholdedStack.getSize();
		
		//IJ.log("merge stacks - diff: "+diff);
		
		for(int a=1; a<=iwpStack.getSize(); a++) {
			//always add the iwpStack IP - and add it BEFORe the thresholded stack IP
			is2.addSlice(iwpStack.getProcessor(a));
			if(a % diff == 0) {
				//if a / diff remainder is 0, then add the thresholded stack IP:
					//if stacks are same size, diff is 1, then a % diff is always 0.
					//if iwp stack is 2x thresholded stack, a % diff only 0 for EVEN numbers (modulus is 0
						//when divisible by 2), etc.
				int b = a / diff;
				is2.addSlice(thresholdedStack.getProcessor(b));
			}
			
		}
		
		//Now, the is2 stack should contain the references to the IPs from both the IWP and thresholded stack
			//and these are presented in the order:  channels : slices.
		
		//return the new ImageStack:
		return is2;
		
	}
	
	
	/**
	 * replaces last channel in iwpStack with thresholdedStack.  
	 * It will put the second imagestack in place of last channel of the first imagestack.
	 * It assumes the second imageStack is a single channel.
	 */
	public ImageStack replaceStacks2( ImageStack iwpStack, ImageStack thresholdedStack ) {
		
		//IJ.log("replace stacks");
		//create new image stack to fill with the two stacks to merge:
		ImageStack is2 = new ImageStack(iwpStack.getProcessor(1).getWidth(), iwpStack.getProcessor(1).getHeight() );
		
		//Get the difference in size between IWP and thresholded stacks:
			//this is used to determine when to insert the thresholdedStack IPs in loop below:
		int diff = iwpStack.getSize() / thresholdedStack.getSize();
		
		//IJ.log("iwp stack size: "+iwpStack.getSize() );
		//IJ.log("thresholdedStack size: "+thresholdedStack.getSize() );
		//IJ.log("Diff: "+diff);
		//IJ.log("a max: "+iwpStack.getSize() );
		
		for(int a=1; a<=iwpStack.getSize(); a++) {
			//always add the iwpStack IP - and add it BEFORe the thresholded stack IP
			if(a % diff != 0) {
				//IJ.log("a: "+a+" a % diff: "+(a%diff) );
				//only add from iwpStack if a % diff does not equal 0 - i.e. IP is not from the 
					//channel being replaced!
				is2.addSlice(iwpStack.getProcessor(a));
			}
			if(a % diff == 0) {
				//IJ.log("a: "+a+" a % diff: "+(a%diff) );
				//if a / diff remainder is 0, then add the thresholded stack IP:
					//if stacks are same size, diff is 1, then a % diff is always 0.
					//if iwp stack is 2x thresholded stack, a % diff only 0 for EVEN numbers (modulus is 0
						//when divisible by 2), etc.
				int b = a / diff;
				is2.addSlice(thresholdedStack.getProcessor(b));
			}
			
		}
		
		//Now, the is2 stack should contain the references to the IPs from both the IWP and thresholded stack
			//and these are presented in the order:  channels : slices.
		
		//return the new ImageStack:
		return is2;
		
	}
	
	
	
	/**
	 * Utility method to call updateOptionsPanel with customOptions as null.
	 * @param algorithmTitle
	 */
	public void updateOptionsPanel(String algorithmTitle) {
		updateOptionsPanel(algorithmTitle, null);
	}
	
	
	/**
	 * This method will update the options panel on the imageProcessingPanel, to allow the appropriate
	 * options for the selected algorithm to appear and be edited by the user.  This allows the user to
	 * select appropriate options for the algorithm they have selected.
	 * <p>
	 * This method works on all image processing steps: pre-processing, thresholding & post-processing, to
	 * ensure the correct options are presented for the selected algorithm.
	 * @param algorithmTitle Title of Algorithm - used to decide what options to put onto the panel.
	 * @param customOptions Potential custom options that can be passed.  Must fit with the options for the
	 * algorithm being passed.
	 */
	public void updateOptionsPanel(String algorithmTitle, String customOptions) {
		
		//Set up the optionsPanel according to the selected algorithm:
		
		//First, make the optionsPanel blank:
		//optionsPanel = new JPanel();
		optionsPanel = new OptionsPanel(algorithmTitle);
		//optionsPanel.setLayout( new BoxLayout(optionsPanel,BoxLayout.LINE_AXIS) );
		optionsPanel.setLayout( new BoxLayout(optionsPanel, BoxLayout.LINE_AXIS) );
		
		// IF BLANK Disable addAglorithm Button, else enable it::
		
		if(algorithmTitle == "") {
			//Do not add anything to optionsPanel - it is BLANK!
			//set the add algorithm button to inactive:
			addAlgorithmButton.setEnabled(false);
		}
		else {
			//else, the algorithm title is not blank - and so the button should be enabled!
			addAlgorithmButton.setEnabled(true);
		}
		
		//Then create optionsPanel according to the algorithmTitle & customOptions:
		optionsPanel.updateOptionsPanel(algorithmTitle, customOptions);
		
	}
	
	
	/**
	 * This method will update the options panel on the imageProcessingPanel, to allow the appropriate
	 * options for the selected algorithm to appear and be edited by the user.  This allows the user to
	 * select appropriate options for the algorithm they have selected.
	 * <p>
	 * This method works on all image processing steps: pre-processing, thresholding & post-processing, to
	 * ensure the correct options are presented for the selected algorithm.
	 * @param algorithmTitle Title of Algorithm - used to decide what options to put onto the panel.
	 * @param customOptions Potential custom options that can be passed.  Must fit with the options for the
	 * algorithm being passed.
	 */
	@Deprecated
	public void updateOptionsPanel2(String algorithmTitle, String customOptions) {
		
		//Set up the optionsPanel according to the selected algorithm:
		
		//First, make the optionsPanel blank:
		//optionsPanel = new JPanel();
		optionsPanel = new OptionsPanel(algorithmTitle);
		//optionsPanel.setLayout( new BoxLayout(optionsPanel,BoxLayout.LINE_AXIS) );
		
		
		// BLANK:
		
		if(algorithmTitle == "") {
			//Do not add anything to optionsPanel - it is BLANK!
			//set the add algorithm button to inactive:
			addAlgorithmButton.setEnabled(false);
		}
		else {
			//else, the algorithm title is not blank - and so the button should be enabled!
			addAlgorithmButton.setEnabled(true);
		}
		
		//PRE PROCESSING:				
				
		if(algorithmTitle ==  "Gaussian Blur 3D...") {
			//Add options for the Gaussian Blur 3D algorithm:
			if(customOptions != null) {
				//extract the options and fill the fields with these options:
				optionsPanel.addNumericField("x", Integer.parseInt( extractValue(customOptions, "x") ) );
				optionsPanel.addNumericField("y", Integer.parseInt( extractValue(customOptions, "y") ) );
				optionsPanel.addNumericField("z", Integer.parseInt( extractValue(customOptions, "z") ) );
				
			}
			else {
				//if null, fill options panel with default options:
				optionsPanel.addNumericField("x", 2);
				optionsPanel.addNumericField("y", 2);
				optionsPanel.addNumericField("z", 2);
			}
			
			//"x=2 y=2 z=2"
		}
		else if(algorithmTitle ==  "Gaussian Blur...") { 
			//Add options for the Gaussian Blur 3D algorithm:
			optionsPanel.addNumericField("sigma", 2);
			
			//"sigma=2"
		}
		else if(algorithmTitle == "Convolve...") {
			//Add options for the Gaussian Blur 3D algorithm:
			optionsPanel.addTextArea("text1", "-1 -1 -1 -1 -1\n-1 -1 -1 -1 -1\n-1 -1 24 -1 -1\n-1 -1 -1 -1 -1\n-1 -1 -1 -1 -1\n");
			optionsPanel.addCheckBox("normalize", false);
			
			//"text1=[-1 -1 -1 -1 -1\n-1 -1 -1 -1 -1\n-1 -1 24 -1 -1\n-1 -1 -1 -1 -1\n-1 -1 -1 -1 -1\n] normalize"
		}
		else if(algorithmTitle == "Median...") {
			//Add options for the Gaussian Blur 3D algorithm:
			optionsPanel.addNumericField("radius", 2);
			
			//"radius=2"
		}
		else if(algorithmTitle == "Mean...") {
			//Add options for the Gaussian Blur 3D algorithm:
			optionsPanel.addNumericField("radius", 2);
			
			//"radius=2"
		}
		else if(algorithmTitle == "Minimum...") {
			//Add options for the Gaussian Blur 3D algorithm:
			optionsPanel.addNumericField("radius", 2);
			
			//"radius=2"
		}
		else if(algorithmTitle == "Maximum...") {
			//Add options for the Gaussian Blur 3D algorithm:
			optionsPanel.addNumericField("radius", 2);
			
			//"radius=2"
		}
		else if(algorithmTitle == "Unsharp Mask...") {
			//Add options for the Gaussian Blur 3D algorithm:
			optionsPanel.addNumericField("radius", 1);
			optionsPanel.addNumericField("mask", 0.60);
			
			//"radius=1 mask=0.60"
		}
		else if(algorithmTitle == "Variance...") {
			//Add options for the Gaussian Blur 3D algorithm:
			optionsPanel.addNumericField("radius", 2);
			
			//"radius=2"
		}
		else if(algorithmTitle == "Median 3D...") {
			//Add options for the Gaussian Blur 3D algorithm:
			optionsPanel.addNumericField("x", 2);
			optionsPanel.addNumericField("y", 2);
			optionsPanel.addNumericField("z", 2);
			
			//"x=2 y=2 z=2"
		}
		else if(algorithmTitle == "Mean 3D...") {
			//Add options for the Gaussian Blur 3D algorithm:
			optionsPanel.addNumericField("x", 2);
			optionsPanel.addNumericField("y", 2);
			optionsPanel.addNumericField("z", 2);
			
			//"x=2 y=2 z=2"
		}
		else if(algorithmTitle == "Minimum 3D...") {
			//Add options for the Gaussian Blur 3D algorithm:
			optionsPanel.addNumericField("x", 2);
			optionsPanel.addNumericField("y", 2);
			optionsPanel.addNumericField("z", 2);
			
			//"x=2 y=2 z=2"
		}
		else if(algorithmTitle == "Maximum 3D...") {
			//Add options for the Gaussian Blur 3D algorithm:
			optionsPanel.addNumericField("x", 2);
			optionsPanel.addNumericField("y", 2);
			optionsPanel.addNumericField("z", 2);
			
			//"x=2 y=2 z=2"
		}
		else if(algorithmTitle == "Variance 3D...") {
			//Add options for the Gaussian Blur 3D algorithm:
			optionsPanel.addNumericField("x", 2);
			optionsPanel.addNumericField("y", 2);
			optionsPanel.addNumericField("z", 2);
			
			//"x=2 y=2 z=2"
		}
		else if(algorithmTitle == "Kuwahara Filter") {
			//Add options for the Gaussian Blur 3D algorithm:
			optionsPanel.addNumericField("sampling", 5);
			
			//"sampling=5 stack"
		}
		else if(algorithmTitle == "Linear Kuwahara") {
			//Add options for the Gaussian Blur 3D algorithm:
			optionsPanel.addNumericField("number_of_angles", 30);
			optionsPanel.addNumericField("line_length", 11);
			optionsPanel.addComboBox("criterion", "Variance", new String[] { "Variance", "Variance / Mean" ,"Variance / Mean^2" }, false);
			
			//"number_of_angles=30 line_length=11 criterion=Variance"
		}
		
		// IJ.run(imp, "Gaussian Blur 3D...", "x=2 y=2 z=2");
		// IJ.run(imp, "Gaussian Blur...", "sigma=2");
		// IJ.run(imp, "Convolve...", "text1=[-1 -1 -1 -1 -1\n-1 -1 -1 -1 -1\n-1 -1 24 -1 -1\n-1 -1 -1 -1 -1\n-1 -1 -1 -1 -1\n] normalize");
		// IJ.run(imp, "Median...", "radius=2");
		// IJ.run(imp, "Mean...", "radius=2");
		// IJ.run(imp, "Minimum...", "radius=2");
		// IJ.run(imp, "Maximum...", "radius=2");
		// IJ.run(imp, "Unsharp Mask...", "radius=1 mask=0.60");
		// IJ.run(imp, "Variance...", "radius=2");
		// IJ.run(imp, "Median 3D...", "x=2 y=2 z=2");
		// IJ.run(imp, "Mean 3D...", "x=2 y=2 z=2");
		// IJ.run(imp, "Minimum 3D...", "x=2 y=2 z=2");
		// IJ.run(imp, "Maximum 3D...", "x=2 y=2 z=2");
		// IJ.run(imp, "Variance 3D...", "x=2 y=2 z=2");
		// IJ.run(imp, "Kuwahara Filter", "sampling=5 stack");
		// IJ.run(imp, "Linear Kuwahara", "number_of_angles=30 line_length=11 criterion=Variance");
		
		
		
		// THRESHOLDING:
		
			// All the Auto Threshold Algorithms -> ALWAYS use options: white stack
												// USER SHOULD CHOOSE:  use_stack_histogram
					//If image is not a stack -> DO NOT USE stack or allow selection of use_stack_histogram
		
		// IJ.run(imp, "Auto Threshold", "method=Otsu white");
		
		// IJ.run(imp, "Auto Threshold", "method=Default white stack use_stack_histogram");
		// IJ.run(imp, "Auto Threshold", "method=Intermodes white stack use_stack_histogram");
		// IJ.run(imp, "Auto Threshold", "method=Huang white stack use_stack_histogram");
		// IJ.run(imp, "Auto Threshold", "method=IsoData white");
		// IJ.run(imp, "Auto Threshold", "method=Li white");
		// IJ.run(imp, "Auto Threshold", "method=MaxEntropy white");
		// IJ.run(imp, "Auto Threshold", "method=Mean white");
		// IJ.run(imp, "Auto Threshold", "method=MinError(I) white");
		// IJ.run(imp, "Auto Threshold", "method=Minimum white");
		// IJ.run(imp, "Auto Threshold", "method=Moments white");
		// IJ.run(imp, "Auto Threshold", "method=Percentile white");
		// IJ.run(imp, "Auto Threshold", "method=RenyiEntropy white");
		// IJ.run(imp, "Auto Threshold", "method=Shanbhag white");
		// IJ.run(imp, "Auto Threshold", "method=Triangle white");
		// IJ.run(imp, "Auto Threshold", "method=Yen white");
		
		
		
		//POST-PROCESSING:
		
		
		
		
		
		//CUSTOM:
		
		else if(algorithmTitle == "Custom") {
			//Add options for the Gaussian Blur 3D algorithm:
			optionsPanel.addTextField("command", "");
			optionsPanel.addTextField("options", "");			
				//This is a special instance of options panel, and will deal with this differently
					//to retrieve the command and options lines for running in the IJ.run(imp, cmd, opt) method..
		}
		
		
		//Finally, need to run the addOptions method to add the options to the optionsPanel! 
		optionsPanel.addOptions();
		
	}//end updateOptionsPanel2(String)
	
	
	
	
	/**
	 * This method will extract the value from the customOptions String which relates to the fieldTitle. For example,
	 * if the custom string is "x=1 y=2 z=3", and fieldTitle is "x", this methofd will return "1".
	 * @param customOptions String containing the fields and their values.  Each field is separated by a space, except the
	 * last field which reaches the end of the String.
	 * @param fieldTitle The title of the field which the value is to be extracted from.
	 * @return The value of the field.
	 */
	public String extractValue(String customOptions, String fieldTitle) {
		String value = "";
		
		int titleIndex = customOptions.indexOf(fieldTitle);
		
		//first isolate the field title & its value + any extra characters in the String after it:
		value = customOptions.substring( customOptions.indexOf(fieldTitle) );
		
		//Now, the String is either [title]=[[val1] [val2] [val3]]
		// Or [title]=[value][END OF STRING] (if the fieldTitle is the last field in this customOptions string
		//Or, the String is [title]=[value] [nextTitle]=[value] etc.
		//The difference is the SPACE!  If the string contains a space, THEN extract from the '=' to the Space
			//Else, extract from the '=' to the end of the String:
		if(value.indexOf(" ") >= 0 ) {
			//The String is in the form -> [title]=[value] [nextTitle]=[value] etc.
				//Extract from AFTER the "=" to the SPACE:
			value = value.substring( value.indexOf("=")+1, value.indexOf(" ") );
			
		}
		else {
			//else the String is in the form -> [title]=[value][END OF STRING]
				//Extract from AFTER the "=" to the END OF STRING:
			value = value.substring(value.indexOf("=")+1); //no endIndex, extract to end of string.
			
		}
		
		//IJ.showMessage("Extracted Field - title: "+fieldTitle+".");
		
		//IJ.showMessage("Extracted Field - value: "+value+".");
		
		
		return value;
	}
	
	
	
	public void fillImageProcessingPanel() {
		
		//Add all components to the imageProcessingPanel:
		imageProcessingPanel = new JPanel();
		//imageProcessingPanel.setLayout( new GridLayout(1,0) );
		//imageProcessingPanel.setLayout( new BoxLayout(imageProcessingPanel,BoxLayout.LINE_AXIS) );
		
		//set status text to up to date with no options displayed:
		status.setText("Up to date.");
		
		imageProcessingPanel.add(algorithmComboBox);
		imageProcessingPanel.add(optionsPanel);
		imageProcessingPanel.add(statusPanel);
		imageProcessingPanel.add(previewAddButtonsPanel);
		
		IWP.clearPanelKeepChSl();
		
		IWP.addComponent(imageProcessingPanel);
		
		//repaint the window:
		IWP.repaintWindow();
		
		
	}	
	
	
	public void fillMainPanel() {
		
		mainPanel = new JPanel();
		
		mainPanel.add(activeChannelPanel);
        mainPanel.add(processingPanel);
        mainPanel.add(listPanel);
        	//mainPanel.add(navigationPanel);
        	//mainPanel.add(loadSavePanel);  //both added now in navLoadSavePanel..
        mainPanel.add(statusPanel);
        mainPanel.add(navLoadSavePanel);
        
        IWP.clearPanelKeepChSl();
		
		IWP.addComponent(mainPanel);
		
		//repaint the window:
		IWP.repaintWindow();
        
	}
	
	
	
	public void setIwpMainPanel() {
		
		//clear IWP panel:
		IWP.clearPanelKeepChSl();
		
		//add the JPanwl component to the panel on IWP:
		IWP.addComponent(mainPanel);
		
		//repaint the window:
		IWP.repaintWindow();
		
		
	}
	
	
	public void showIwpActiveImp() {
		
		activeImpOn = true;
		
		//set the last channelcheckbox item to 1 - on:
		//IWP.channels[ (IWP.getChannels()-1) ].state = "1";
			//Do not change state by itself!  Use setState() which will modify
				//both the state AND the checkbox.
		
		//set last channelcheckbox item to true (ON):
		IWP.channels[ (IWP.getChannels()-1) ].setState(true);
		
		//Make the LUT of this channel White:
		IJ.run(IWP.getImagePlus(), "Grays", "");
		
		//update the IWP image - this will display all channels and slices:
		IWP.updateChannelsAndSlices(true);
		
	}
	
	public void hideIwpActiveImp() {
		
		activeImpOn = false;
		
		//set the last channelcheckbox item to 1 - on:
		//IWP.channels[ (IWP.getChannels()-1) ].state = "0";
		//Do not change state by itself!  Use setState() which will modify
			//both the state AND the checkbox.

		//set last channelcheckbox item to false (OFF):
		IWP.channels[ (IWP.getChannels()-1) ].setState(false);
		
		//update the IWP image - this will display all channels and slices:
		IWP.updateChannelsAndSlices(true);
		
	}
	
	
	/**
	 * Update the Text Area 'status' in this Plugin to display either String 'Up to date.', or this String
	 * plus the current options for the currently selected procedureStack item on list.
	 */
	public void updateStatus() {
		
		if(list.isSelectionEmpty() == true) {
			if(status != null) {
				status.setText("Up to date.");
			}
		}
		else {
			if(status != null) {
				status.setText("Up to date. \n options: \n"+procedureStack.getOptions(list.getSelectedIndex() ) );
			}
		}
	}
	
	
	
	/**
	 * Fills the list model for list with the commands in commandTitles ArrayList.
	 */
	public void updateList() {
		
		listModel.addElement( procedureStack.commandTitles.get( procedureStack.commandTitles.size()-1 ) );
		
		//and set the last added command as the selected one on the list:
		list.setSelectedIndex( procedureStack.commandTitles.size()-1 );
		
		//update the list?

	}
	
	
	
	/**
	 * Fills the list model for list with the commands in commandTitles ArrayList.
	 */
	public void clearAndFillList() {
		
		listModel.clear();
		
		for(int a=0; a<procedureStack.commandTitles.size(); a++) {
			listModel.addElement( procedureStack.commandTitles.get(a) );
	        
		}
		
		//and set the last added command as the selected one on the list:
			//no need to do this!
		//list.setSelectedIndex( commandTitles.size()-1 );
		
		//update the list?

	}
	
	
	
	/**
	 * This is called by listener on the list object.  If a different item is selected, the toggle
	 * button must be updated to reflect the toggle on/off state of this item.  This method achieves
	 * this by setting the toggleButton to on or off according to the toggle property state of
	 * selected item at index.
	 * @param index
	 */
	public void updateToggle(int index) {
		
		if( procedureStack.toggleProperties.get(index).equals("0") ) {
			toggleItem.setSelected(false);
			toggleItem.setIcon(toggleIconOff);
		}
		else if( procedureStack.toggleProperties.get(index).equals("1") ) {
			toggleItem.setSelected(true);
			toggleItem.setIcon(toggleIconOn);
		}
		
	}
	
	
	
	public void displayActiveImp(boolean cmdRan) {
		
		if(cmdRan == true) {
			//if a command was run, then show the active imp:
			showIwpActiveImp();	
		}
		else if(cmdRan == false) {
			//otherwise, hide the active imp on the IWP:
			hideIwpActiveImp();
		}
		
		updateStatus();

	}
	
	
	
	/** Returns an ImageIcon, or null if the path was invalid.  RESCALES the image. */
	public ImageIcon createImageIcon(String path,
	                                           String description,int resizedWidth, int resizedHeight) {
		
	    java.net.URL imgURL = getClass().getResource(path);
	    
	    if (imgURL != null) {
	    	ImageIcon icon = new ImageIcon(imgURL, description);
	    	Image img = icon.getImage();
	        Image resizedImage = img.getScaledInstance(resizedWidth, resizedHeight,  java.awt.Image.SCALE_SMOOTH);  
	        return new ImageIcon(resizedImage, description);
	    } else {
	        System.err.println("Couldn't find file: " + path);
	        return null;
	    }
	}
	
	
	/**
	 *  * This method will deactivate all Buttons on the JPanel or any JPanels this JPanel contains.
	 * <p>
	 * Will loop through the panel, any JPanels -> recall this method, any objects which are JButtons -> setEnabled(false).
	 * @param mainPanel JPanel to search components on.
	 */
	public void deactivateButtons(JPanel mainPanel) {
		
		Component[] cs = mainPanel.getComponents();
		
		for(int a=0; a<cs.length; a++) {
			if(cs[a] instanceof JPanel) {
				deactivateButtons( (JPanel)cs[a] );
			}
			if(cs[a] instanceof JButton) {
				((JButton)(cs[a])).setEnabled(false);
			}
			if(cs[a] instanceof JComboBox<?>) {
				((JComboBox<?>)(cs[a])).setEnabled(false);
			}
			if(cs[a] instanceof JTextField) {
				((JTextField)(cs[a])).setEnabled(false);
			}
			if(cs[a] instanceof JCheckBox) {
				((JCheckBox)(cs[a])).setEnabled(false);
			}
			if(cs[a] instanceof JTextArea) {
				((JTextArea)(cs[a])).setEnabled(false);
			}
		}
		
	}
	
	
	
	/**
	 * This method will reactivate all Buttons on the JPanel or any JPanels this JPanel contains.
	 * <p>
	 * Will loop through the panel, any JPanels -> recall this method, any objects which are JButtons -> setEnabled(true).
	 * @param mainPanel JPanel to search components on.
	 */
	public void reactivateButtons(JPanel mainPanel) {
		
		Component[] cs = mainPanel.getComponents();
		
		for(int a=0; a<cs.length; a++) {
			if(cs[a] instanceof JPanel) {
				reactivateButtons( (JPanel)cs[a] );
			}
			if(cs[a] instanceof JButton) {
				((JButton)(cs[a])).setEnabled(true);
			}
			if(cs[a] instanceof JComboBox<?>) {
				((JComboBox<?>)(cs[a])).setEnabled(true);
			}
			if(cs[a] instanceof JTextField) {
				((JTextField)(cs[a])).setEnabled(true);
			}
			if(cs[a] instanceof JCheckBox) {
				((JCheckBox)(cs[a])).setEnabled(true);
			}
			if(cs[a] instanceof JTextArea) {
				((JTextArea)(cs[a])).setEnabled(true);
			}
		}
		
	}
	
	
	public String getImageProcessingPanelType(String algorithm) {
		
		for(int a=0; a<preProcessingStrings.length; a++) {
			if(preProcessingStrings[a].equals(algorithm)) {
				return "PreProcessing";
			}
		}
		
		for(int a=0; a<thresholdingStrings.length; a++) {
			if(thresholdingStrings[a].equals(algorithm)) {
				return "Thresholding";
			}
		}
		
		for(int a=0; a<postProcessingStrings.length; a++) {
			if(postProcessingStrings[a].equals(algorithm)) {
				return "PostProcessing";
			}
		}
		
		//else retun a blank string:
		
		return "";
		
	}
	
	
	/**
	 * Remove all Listeners from this Object.
	 */
	public void removeListeners() {
		
		StereoMateUtilities.removeChangeListener(activeChannelSpinner);
		
		StereoMateUtilities.removeActionListener(preProcessingButton);
		
		//check if these are null or not - will be NULL if the panel is NOT showing
			// the pre processing, thresholding or post processing algorithm setup:
		if( algorithmComboBox != null ) {
			StereoMateUtilities.removeActionListener(algorithmComboBox);
		}
		if( addAlgorithmButton != null ) {
			StereoMateUtilities.removeActionListener(addAlgorithmButton);
		}
		if( cancelButton != null ) {
			StereoMateUtilities.removeActionListener(cancelButton);
		}
		if( previewButton != null ) {
			StereoMateUtilities.removeActionListener(previewButton);
		}
		
		StereoMateUtilities.removeActionListener(thresholdingButton);
		
		StereoMateUtilities.removeActionListener(postProcessingButton);
		
		StereoMateUtilities.removeListSelectionListener(list);
		
		StereoMateUtilities.removeActionListener(upItem);
		StereoMateUtilities.removeActionListener(downItem);
		
		StereoMateUtilities.removeActionListener(toggleItem);
		StereoMateUtilities.removeActionListener(editItem);
		StereoMateUtilities.removeActionListener(deleteItem);
		
		StereoMateUtilities.removeActionListener(nextButton);
		StereoMateUtilities.removeActionListener(previousButton);
		
		StereoMateUtilities.removeActionListener(loadButton);
		StereoMateUtilities.removeActionListener(saveButton);
		
		
	}
	
	
	public void shutdownDW() {
		
		dw.shutDownDialogWindow();
		dw = null; //DialogWindow object - to create a dialog to select files for
							//processing.
		
	}

	
	public void shutdownIWP() {
		
		// shut down the IWP:
		IWP.getWindow().dispose(); //dispose the iw.
		IWP.setModal(false); //re-activate the ij window.
		IWP.shutdownImageWindowWithPanel();
		IWP = null;  // -> set to null to garbage collect.

		System.gc();
		
	}
	
	
	
	/**
	 * This ProcessThread is used to generate a new thread inside the EDT and run the processImages() method in
	 * a new thread when the Process Button is pressed on the Dialog.
	 * <p>
	 * This thread will update the activeImp according to the channel number selected in the activeChannelSpinner.  This
	 * can also be used to update the activeImp to a fresh (un-processed) imp of the active channel, for example when
	 * running a different iteration of the procedure stack or when running the procedure stack followed by a preview.
	 * @author stevenwest
	 *
	 */
	public class ActiveChannelThread extends Thread {
		
		/**
		 * The run method should perform all the operations that will be run when the activeChannel is updated.
		 */
		@Override
		public void run() {

			//Get the channel number which spinner is set to:
			activeChannelNum = (int)spinnerModel.getValue();
			
			//IJ.log("channel Number: "+channelNum);
			
			status.setText("setting new active channel: "+activeChannelNum);
			
			//status.repaint();
			
			//IWP.updateChannelsAndSlices(true);
			
			//re-generate the extra channel from IWP imp -> set to activeImp:
			
				//Get the stack from IWP imp:
				ImageStack is = IWP.getOriginalImagePlus().getStack();
			
				//Filter ImageStack to retrieve the stack which represents the active channel:
					//this is channelNum:
				is = IWP.filterImageStack(is, activeChannelNum, IWP.getOriginalImagePlus().getNChannels() );
			
				//Set is to activeImp:
				activeImp.setStack(is);
			
			//THEN **REPLACE** the current extra channel in IWP with this new extracted channel:
				
			IWP.getOriginalImagePlus().setStack( IWP.replaceStacks( IWP.getOriginalImagePlus().getStack(), activeImp.getStack() ), 
																IWP.getOriginalImagePlus().getNChannels(), 
																	IWP.getOriginalImagePlus().getNSlices(), 
																		IWP.getOriginalImagePlus().getNFrames() ); 
			
			//Test status updates correctly by pausing thread to give time to view:
			//IJ.wait(2000);			

			//update the IWP image:
			IWP.updateChannelsAndSlices(true);
						
			//reactivate all buttons as appropriate:
			reactivateButtons(mainPanel);
			
			status.setText("Up to date.");
			
			//For Testing - make activeImp visible [this is on 2 channel test image]:
				
				//if(IWP.channels[2].state == "0") {
					//IWP.channels[2].changeState();
				//}
				//IWP.updateChannels();
				//IWP.updateChannelsAndSlices(true);

		}
		
		/**
		 * The run method should perform all the operations that will be run when the activeChannel is updated.  This
		 * method does not reactiveate buttons on mainPanel and does not set text to "up to date".
		 */
		public void runNoReActivate() {

			//Get the channel number which spinner is set to:
			activeChannelNum = (int)spinnerModel.getValue();
			
			//IJ.log("channel Number: "+channelNum);
			if(status != null) {
				status.setText("setting new active channel: "+activeChannelNum);
			}
			//status.repaint();
			
			//IWP.updateChannelsAndSlices(true);
			
			//re-generate the extra channel from IWP imp -> set to activeImp:
			
			// IJ.showMessage("runNoReActivate(): Memory Use: "+IJ.currentMemory()+" free memory: "+IJ.freeMemory() );
			
				//Get the stack from IWP imp:
			ImagePlus imp = IWP.getOriginalImagePlus();
				ImageStack is = imp.getStack();
			
				//Filter ImageStack to retrieve the stack which represents the active channel:
					//this is channelNum:
				
						// To Avoid an OOM - FIRST delete the current end channel from the IWP, THEN replace with the
							// new image stack!
						// IWP.deleteImageStack( imp, imp.getNChannels(), imp.getNChannels() );
				
				// Above deletes the images directly from the imageStack - so the 
				is = IWP.filterImageStack(is, activeChannelNum, imp.getNChannels() );
				//is = IWP.filterImageStackNoDup(is, activeChannelNum, IWP.getOriginalImagePlus().getNChannels() );
			
				//Set is to activeImp:
				activeImp.setStack(is);
			
			//THEN **REPLACE** the current extra channel in IWP with this new extracted channel:
				
			imp.setStack( IWP.replaceStacks( imp.getStack(), activeImp.getStack() ), 
						imp.getNChannels(), 
						imp.getNSlices(), 
						imp.getNFrames() ); 
				
					// ACTUALLY - as the extra channel has been REMOVED, need to MERGE is and the stack in imp,
						// as is performed during the construction of the IWP:
					
			//		IWP.getOriginalImagePlus().setStack( 
			//				ImageWindowWithPanel.mergeStacks( imp.getStack(), is ), 
			//				imp.getNChannels()+1, 
			//				imp.getNSlices(), 
			//				imp.getNFrames() ); 
					
			//Test status updates correctly by pausing thread to give time to view:
			//IJ.wait(2000);			

			//update the IWP image:
			IWP.updateChannelsAndSlices(true);
						
			//reactivate all buttons as appropriate:
			//reactivateButtons(mainPanel);
			
			//status.setText("Up to date.");
			
			//For Testing - make activeImp visible [this is on 2 channel test image]:
				
				//if(IWP.channels[2].state == "0") {
					//IWP.channels[2].changeState();
				//}
				//IWP.updateChannels();
				//IWP.updateChannelsAndSlices(true);

		}
		
	}
	
	
	
	
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
	public class ProcedureStack {
		
		/**
		 * This indicates the bitDepth which the images are processed as - used for recall of the procedureStack to
		 * ensure correct bitDepth of the images are set!
		 */
		int bitDepth;
		
		
		/**
		 * This indicates which channel is the ACTIVE CHANNEL -> this is the channel which is being processed by this
		 * ImageProcessingProcedureStack.
		 */
		int activeChannel;
		
		/**
		 * These ArrayLists hold the cmd title, options and toggle setting of the commands in the procedure stack.  They must
		 * ALWAYS be edited together, which occurs in this class.
		 */
		
		protected ArrayList<String> commandTitles;
		
		protected ArrayList<String> commandOptions;
		
		protected ArrayList<String> toggleProperties;
		
		
		
		/**
		 * This will construct a new ImageProcessingProcedureStack object.  
		 * <p>
		 * As each Command is run, the JTextArea 'status' is updated with this information, to let the user know image processing
		 * is taking place.  During this time, the Threshold_Manager buttons on its active panel will be inactivated.
		 * The commands will be run in a separate thread, to free EDT thread to draw on the status JTextArea.
		 * @param bitDepth -> bitDepth set when IWP is generated.
		 * @param activeChannel -> the activeChannel set when the thresholding procedure stack is run (i.e. what channel
		 * is being processed).
		 */
		public ProcedureStack(int bitDepth, int activeChannel) {
			
			this.bitDepth = bitDepth;
			
			this.activeChannel = activeChannel;
			
			commandTitles = new ArrayList<String>();
			
			commandOptions = new ArrayList<String>();
			
			toggleProperties = new ArrayList<String>();
		
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
				// IJ.showMessage("Custom command: "+commandOptions);
				commandTitle = commandOptions.substring(commandOptions.indexOf("command=")+8, commandOptions.indexOf(" "));
				
				//extract the command option (String from end of 'options=' to the end of String:
				commandOptions = commandOptions.substring(commandOptions.indexOf("options=")+8);
				
				if( commandOptions.contains("[") ) {
					commandOptions = commandOptions.substring(1, commandOptions.length()-1);
				}
				
				IJ.showMessage("Custom title: "+commandTitle);
				IJ.showMessage("Custom options: "+commandOptions);

			}
			
			
			//add the cmd title and options to ArrayLists:
			commandTitles.add(commandTitle);
			this.commandOptions.add(commandOptions);
			
			//set the toggleProperty of this item in the toggleProperty ArrayList:
			toggleProperties.add("0");
			
			//and update the list:
			//updateList(); -> no longer performed in this obj -> done in ThresholdManager class!
			
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
		 * @param index The index of the command to be moved.
		 * @param maxIndex The maxIndex of the list of commands -> typically this is the listModel's size, -1.
		 */
		public void moveDown(int index) {
			
			//move the index cmd down in all the ArrayLists:
			
			//only if index is less than list max index -1!  which is commandTitles.size()-1!!
			
			//if(index < (listModel.size()-1) ) {
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
		 * Returns the number of procedures that are toggled on.
		 * @return
		 */
		public int getProcedureDepth() {
			int d = 0;
			for(int a=0; a<toggleProperties.size(); a++) {
				if(toggleProperties.get(a) == "1") {
					//for each item in toggle properties, 
						//if the item is toggled on
							//then add one to d:
					d = d + 1;
				}
			}
			
			//return d - the number of tollged on items in this procedureStack object.
			return d;
			
		}

		
		/**
		 * Runs the current image processing procedure stack on the passed imp.
		 * @param imp
		 */
		public void runProcedureStack(ImagePlus imp) {
			boolean cmdRan = false;
						
			for(int a=0; a<commandTitles.size(); a++) {
				
				if(toggleProperties.get(a).equals("1") ) {	
					//only run the procedure IF the toggle property is 1 for this item in the stack:
					
					if(status != null) {
						status.setText("Running: "+commandTitles.get(a) );
					}
					
					// IJ.showMessage("activeImp ref: "+activeImp.hashCode() );
					
					// IJ.showMessage("(active)Imp ref: "+imp.hashCode() );
					
					// IJ.showMessage("(active)Imp ip b4 run: "+imp.getProcessor().hashCode());
					
					IJ.run( imp, commandTitles.get(a), commandOptions.get(a) );
					
					// IJ.showMessage("(active)Imp ip after run: "+imp.getProcessor().hashCode());
					
					// IJ.showMessage("activeImp ip after run: "+activeImp.getProcessor().hashCode());
					
					// IJ.showMessage("activeImp ip added after run: "+activeImp.getStack().getProcessor(1).hashCode() );
					
					//The IWP Ref to the third channel may now be in-correct (if a new imagestack is put into activeImp
						//during the running of this command - will lose the ip refs in IWP to the activeImp channel!)
					//Therefore, update IWP last channel with activeImp and its new stack ref!!
					
					//SO - **REPLACE** the current extra channel in IWP with this new extracted channel:
					
					IWP.getOriginalImagePlus().setStack( IWP.replaceStacks( IWP.getOriginalImagePlus().getStack(), 
																			activeImp.getStack() ), 
																		IWP.getOriginalImagePlus().getNChannels(), 
																		IWP.getOriginalImagePlus().getNSlices(), 
																		IWP.getOriginalImagePlus().getNFrames() );
					
					//if command is run, set cmdRan to true:
					cmdRan = true;
				}
			}
			
			if(cmdRan == true) {
				//if a command was run, then show the active imp:
				showIwpActiveImp();	
			}
			else if(cmdRan == false) {
				//otherwise, hide the active imp on the IWP:
				hideIwpActiveImp();
			}
			
			if(list.isSelectionEmpty() == true) {
				if(status != null) {
					status.setText("Up to date.");
				}
			}
			else {
				if(status != null) {
					status.setText("Up to date. \n options: \n"+procedureStack.getOptions(list.getSelectedIndex() ) );
				}
			}
			
			
		}
		
		/**
		 * Runs a given command with given options on the passed imp.  This method takes care of previews on IWP
		 * in Threshold Manager Plugin.
		 * @param imp
		 * @param cmd
		 * @param options
		 */
		public void runProcedure(ImagePlus imp, String cmd, String options) {
			status.setText("Running: "+cmd );

			IJ.run(imp, cmd, options);
			
			if(list.isSelectionEmpty() == true) {
				status.setText("Up to date.");
			}
			else {
				status.setText("Up to date. \n options: \n"+procedureStack.getOptions(list.getSelectedIndex() ) );
			}
		}
		
		
		/**
		 * Returns the default name for this procedureStack - combination of the bitDepth and a sequence of the 
		 * procedureStacks' steps.
		 * @return String of default name of this procedure stack - bit depths + each processes name.
		 */
		public String getDefaultName() {
			String name = "";
			
			//Add the ActiveChannel variable:
			name = name + "C" + activeChannel + " ";
			
			//Add bit depth to String with space:
			name = name + bitDepth + "bit ";
			
			//next loop through all procedure titles and add these, with a space between each:
			for(int a=0; a<commandTitles.size(); a++) {
				//ONLY add process title if it is toggled ON:
				if(toggleProperties.get(a) == "1" ) {
					if(commandTitles.get(a) == "Auto Threshold") {
						name = name + extractThresholdMethod(a) + " ";
					}
					else {
						name = name + commandTitles.get(a) + " ";
					}
				}
			}
			
			//Finally, remove the last space:
			name = name.substring(0, name.length()-1);
			
			
			//and add the ".xml" extension:
			name = name + ".xml";
			
			//and return name:
			return name;
		}
		
		
		/**
		 * Extracts and returns the threshold method used in the ProcedureStack at the passed index, or
		 * null if the command at the supplied index is not a thresholding algorithm.
		 * @return String name of the thresholding procedure, or null.
		 */
		public String extractThresholdMethod(int index) {
			
			if(commandTitles.get(index) != "Auto Threshold") {
				return null;
			}
			
			//now it MUST be a threshold process, so find the name in the options:
			
			int lastIndex = commandOptions.get(index).indexOf(" ");
			
			//return the string between the end of "method=" [7 characters long] and the first space:
			return commandOptions.get(index).substring(7, lastIndex);
			
		}
		
		
		/**
		 * Saves the IPPS to an XML file, for later recall.
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
			File thresholdProceduresFile = new File(file.getAbsolutePath() + File.separator + ".thresholdProcedures" + File.separator + procedureTitle);
			
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
				
				//ADD activeChannel element
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
				procedureDepth.appendChild( doc.createTextNode( "" + getProcedureDepth() ) );
				rootElement.appendChild(procedureDepth);
				
				// ADD procedures element:
				Element procedures = doc.createElement("procedures");
				rootElement.appendChild(procedures);
				
				//ADD procedure elements to procedures element:
				for(int a=0; a<commandTitles.size(); a++) {
					//ONLY ADD is the Toggle is set to ON:
					if(toggleProperties.get(a) == "1") {
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
		
		
		
		/**
		 * Saves the IPPS to an XML file, for later recall.
		 */
		public void saveStack(String procedureTitle, File outputDir) {
			
			//Retrieve file representation of JAR file - to retrieve its absolute path:
			//File file = null;	
			//try {
				//file = new File(StereoMateSettings.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			//} catch (URISyntaxException e1) {
				//e1.printStackTrace();
			//}
			//Retrieve parent, and then formulate new file object pointing to the .thresholdProcedures DIR & the procedureTitle file:
			//file = file.getParentFile();
			//File thresholdProceduresFile = new File(file.getAbsolutePath() + File.separator + ".thresholdProcedures" + File.separator + procedureTitle);
			
			outputDir = new File(outputDir.getAbsolutePath() + File.separator + procedureTitle);
			
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
				
				//ADD activeChannel element
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
				procedureDepth.appendChild( doc.createTextNode( "" + getProcedureDepth() ) );
				rootElement.appendChild(procedureDepth);
				
				// ADD procedures element:
				Element procedures = doc.createElement("procedures");
				rootElement.appendChild(procedures);
				
				// create an int to remember the index of each command in procedures that are toggled on
					// To provide the correct index (from 0 - procedureDepth) number for the command in the procedures
					// title, options and toggle elements:
				int cmdInd = 0;
				
				//ADD procedure elements to procedures element:
				for(int a=0; a<commandTitles.size(); a++) {
					//ONLY ADD is the Toggle is set to ON:
					if(toggleProperties.get(a) == "1") {
						//Add title:
						Element cmdTitle = doc.createElement("title"+cmdInd); // use cmdInd NOT a!
						cmdTitle.appendChild( doc.createTextNode( commandTitles.get(a) ) );
						procedures.appendChild(cmdTitle);
						//Add Options:
						Element cmdOptions = doc.createElement("options"+cmdInd);// use cmdInd NOT a!
						cmdOptions.appendChild( doc.createTextNode( commandOptions.get(a) ) );
						procedures.appendChild(cmdOptions);
						//Add toggleProperties:
						Element cmdTog = doc.createElement("toggle"+cmdInd);// use cmdInd NOT a!
						cmdTog.appendChild( doc.createTextNode( toggleProperties.get(a) ) );
						procedures.appendChild(cmdTog);
						cmdInd = cmdInd+1;
					}
				}


				// write the content into xml file
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(doc);
				// StreamResult result = new StreamResult( thresholdProceduresFile );

				//ensure XML file is formatted for human reading:
				//transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				//transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
				// Output to console for testing
				// StreamResult result = new StreamResult(System.out);

				//transformer.transform(source, result);
				
				// write the content into xml file
				 //transformerFactory = TransformerFactory.newInstance();
				 //transformer = transformerFactory.newTransformer();
				 //source = new DOMSource(doc);
				 StreamResult result = new StreamResult( outputDir );

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
		 * 
		 */
		public ProcedureStack loadStack(String stackTitle) {
		
			ProcedureStack IPPS = null;
			
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
			
			//read activeChannel Node:
			
			int activeChannel = Integer.parseInt( doc.getElementsByTagName("activeChannel").item(0).getTextContent() );
			
				//and create new IPPS object with correct bitDepth:
			
			//IPPS = new ImageProcessingProcedureStack(bitDepth, status);
			IPPS = new ProcedureStack(bitDepth, activeChannel);
			
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


	}//end class ImageProcessingProcedureStack
	
	
	
	
	
	/**
	 * This class is used to run the whole ImageProcessingProcedureStack in a separate thread.
	 * The two constructors reflect the dual purpose of this class -> to run the entire procedure stack, or
	 * to run an individual command (which is used when previewing a specific command before adding it
	 * to the Prcedure Stack).
	 * <p>
	 * When the blank constructor is made, running this thread will just run the procedure stack.
	 * <p>
	 * When the constructor containing two Strings (cmd and options) is made, running this thread will run the
	 * procedure stack followed by the new command 'cmd' with options 'options'.
	 * 
	 * @author stevenwest
	 *
	 */
	public class ImageProcessingProcedureThread extends Thread {
		
		String cmd;
		String options;
		
		JPanel panel;
		
		/**
		 * Set to true to ensure the Panel is re-activated after processing.
		 */
		boolean toggleOffInstance;
		
		public ImageProcessingProcedureThread() {
			cmd = null;
			options = null;
			panel = null;
			toggleOffInstance=false;
		}
		
		public ImageProcessingProcedureThread(JPanel panel) {
			cmd = null;
			options = null;
			
			this.panel = panel;
			
			toggleOffInstance=false;
		}
		
		public ImageProcessingProcedureThread(String cmd, String options) {
			this.cmd = cmd;
			this.options = options;
			
			panel = null;
			toggleOffInstance=false;
		}
		
		public ImageProcessingProcedureThread(String cmd, String options, JPanel panel) {
			this.cmd = cmd;
			this.options = options;
			
			this.panel = panel;
			toggleOffInstance=false;
		}
		
		public ImageProcessingProcedureThread(JPanel panel, boolean togOff) {
			cmd = null;
			options = null;
			
			this.panel = panel;
			
			toggleOffInstance=togOff;
		}
		
		/**
		 * The run method should perform all the operations in the ImageProcessingProcedureStack as required.
		 * This just called the runProcedureStack method on the procedureStack variable, using the activeImp as
		 * the imp to call it on.
		 * <p>
		 * If this instance of ImageProcessingProcedureThread includes a cmd and options, this will also be run
		 * at the end of the procedure stack.
		 */
		@Override
		public void run() {

			//run procedure stack on the fresh activeImp:
				//only IF cmd is null -> cmd is not null if running a preview, which will work ON TOP of a previously
					//run procedure stack, if any of the items were checked in mainPanel list!
				//so only want to run the procedureStack if this is NOT a preview...
			if(cmd == null) {
				
				//only run if there is a procedure stack to run!
					//Its also run is the toggleOffInstance variable is true - to force the stack to run
						//even if not toggle is on.
					
				if(procedureStack.isAnyToggleOn() == true || toggleOffInstance == true) {
				
					//start by deactivating buttons on the panel if passed to this class:
					if(panel != null) {
						deactivateButtons(panel);
						//THIS SHOULD BE PERFORMED IN THE THRESHOLD MANAGER CLASS?
							//OR THE IWP CLASS?!
					}
				
					//FIRST:
										
					//ensure the activeImp is cleared of all previous image processing - run the activeChannelThread:
					activeChannelThread = new ActiveChannelThread(); //need to generate new thread object each time!
					activeChannelThread.runNoReActivate(); 
					//run this in the current thread - as this code is outside the EDT!
						//run the method which does NOT reactivate the mainPanel -> want to do this at the end of THIS method.
										
					//and then run the procedure stack on activeImp:
					procedureStack.runProcedureStack(activeImp);
					
					//boolean ran = procedureStack.runProcedureStack(activeImp);
					
					//if(ran) {
						//CALL DISPLAY METHOD FROM THRESHOLD MANAGER!!
					//}
					
					//set the activeChannel checkbox (on activeChannel spinner panel) to correct state, depending on
						//how the activeImp is displayed in IWP:
					//activeChannelcb.setSelected( IWP.getDisplayChannels().endsWith("1") );
						//This should not really have to be run here -> should make the channels checkboxes SEFL SUFFICIENT:
							//When the display is modified for the last channel, the checkbox should update -> correct this
								//in IWP class...
										
					//finally, reactivate the panel buttons if appropriate:
					if(panel != null) {
						reactivateButtons(panel);
					}
				
				}
			}
			
			//This is run if the preview button is checked - this will ONLY RUN THE PREVIEW PROCEDURE ON TOP
				//OF ANY PROCESSING IN ACTIVE CHANNEL:
			if(cmd != null) {
				//start by deactivating buttons on the panel if passed to this class:
				if(panel != null) {
					deactivateButtons(panel);
				}
				//DO NOT CLEAR ACTIVE IMP - want to keep any procedure stack applied to it so the latest preview
					//will happen on top of it.
				//run the extra command passed to this thread:
				procedureStack.runProcedure(activeImp, cmd, options);
				//If the activeImp is not displayed (because no procedure stack is currently active on it), need
					//to display the activeImp:
				if(activeImpOn == false) {
						//only call if the activeImp is not already on..
					showIwpActiveImp();
				}
				//finally, reactivate the panel buttons if appropriate:
				if(panel != null) {
					reactivateButtons(panel);
				}
				
			}
			
			//display the activeImp in IWP:
			//showIwpActiveImp();
			
			//status.setText("Up to date.");
			
		}
		
	}
	
	
}
