package stereomate.plugins;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.io.FileInfo;
import ij.io.RoiDecoder;
import ij.macro.Interpreter;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.ChannelSplitter;
import ij.plugin.DICOM;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;
import ij.plugin.TextReader;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder;
import ij.process.ByteProcessor;
import ij.process.FloatPolygon;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import mcib3d.image3d.ImageInt;
import mcib3d.image3d.ImageLabeller;
import mcib3d.image3d.ImageShort;
import mcib3d.image3d.processing.Flood3D;
import stereomate.data.DatasetWrapper;
import stereomate.data.ObjectDataContainer;
import stereomate.data.ObjectDatasetMap;
import stereomate.data.RoiDataContainer;
import stereomate.dialog.DialogWindow;
import stereomate.dialog.DialogWindow.FileSelector;
import stereomate.image.ImageHandler;
import stereomate.image.ImageProcessingProcedureStack3;
import stereomate.object.ObjectClassifier;
import stereomate.object.ObjectFilter;
import stereomate.object.ObjectIdentifier;
import stereomate.object.ObjectVoxelProcessing;
import stereomate.roi.RoiAssessmentHandler;
import stereomate.settings.OM_ProcedureSettings;
import stereomate.settings.StereoMateUtilities;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils.DataSink;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Add; 

/**
 * 
 * This class implements the StereoAnalyser algorithm.  It uses the SM input-output framework
 * via the DialogWindow3 class to obtain image and matched ROI input, as well as the Object Manager
 * input - which includes the thresholding algorithm, maximum object dimensions in XY and Z, and
 * any object filtering (via Classifier or simple high/low pass filter).
 * <p>
 * With each image-ROI pair, the algorithm applies the appropriate exclusion zone criteria to
 * each ROI to obtain an unbiased sample of objects.  It then measures objects within each
 * ROI to derive a multivariate dataset - consisting of measures from each object in the unbiased sample.
 * <p>
 * Measures include:  Size: volume, surface area, XYZ dimension lengths, Shape parameters: sphericity,
 * FormFactor, etc. Intensity: mean intensity, median, SD, quartiles, etc.  Location.
 * 
 * @author stevenwest
 *
 */
public class StereoMate_Analyser implements PlugIn, StereoMateAlgorithm {

	/** Instance Variables */

	/**
	 * DialogWindow instance variables - to collect information from user:
	 */
	JComboBox<String> channelsComboBox;
	JComboBox<String> cb;
	int channelIndex;


	/**
	 * Variables for the Exclusion Zone checkboxes:  Apply Exclusion Zone in XY, in Z?
	 */
	JCheckBox exclusionXYCheckBox, exclusionZCheckBox;

	/**
	 * Variable for the Edge Object checkbox:  NO LONGER USED - analysis is set as Whole-Object or
	 * Object-Fragment in Object_Manager Plugin!
	 */
	JCheckBox edgeObjectCheckBox;
	
	/**
	 * Reference to object manager fileselector.
	 */
	FileSelector objManagerFS;

	/**
	 * File reference to the OM_MetaData directory in Object Manager directory:
	 */
	File om_MetaData;

	/**
	 * The 'procedure stack' - description of the thresholding procedure to be applied to the images.
	 */
	ImageProcessingProcedureStack3 procedureStack;

	DatasetWrapper objectDataset;

	ObjectIdentifier objIdentifier;
	
	/**
	 * Object pixel values for FEATURE and for CONNECTED objects.
	 */
	int featureObjVal, connectedObjVal;
	/**
	 * ROI IN and OUT Values for feature objects.
	 */
	int featureInVal, featureOutVal;
	/**
	 * ROI IN and OUT Values for connected objects.
	 */
	int connectedInVal, connectedOutVal;
	
	ObjectFilter objectFilter;
	
	ObjectClassifier objectClassifier;

	/**
	 * An Instances Object - which holds the ARFF data object.
	 */
	// Instances arff;

	/**
	 * An Instances Object - which holds the overview data.
	 */
	// Instances dataOverview;
	DatasetWrapper roiOverviewDataset;

	/**
	 * ArrayList of Instances objects, one for each ROI, to store the data of each object in each array in to.
	 */
	ArrayList<DatasetWrapper> roiDatasets;

	/**
	 * String represents the path of the current output destination for the arff dataset.  Used to save
	 * the dataset, and also compare to see if it currently exists.
	 */
	String arffPath;

	/**
	 * Hash Map to map the FIRST VOXEL in a Object3DVoxels object to the index of that object in the ARFF
	 * data object.  The FIRST VOXEL can be retrieved from a selected Object3DVoxel object in an image by calling the 
	 * static getFirstVoxel(obj3Dvox) method in MCIB_SM_BorderObjPixCount3D, returning a Point3D obj.
	 * <p>
	 * Using a List<Integer> to represent the First Voxel as this generates a good and unique Hash Value for each
	 * coordinate supplied.
	 */
	ObjectDatasetMap firstPixObjNoMap;

	/**
	 * This class contains methods to select objects in ImageInt's, and to change the voxel values of these objects or
	 * measure them in different ways.  
	 * <p>
	 * Used here to build the object attributes data (arff), and also to identify any object's FirstPixel (first pixel 
	 * encountered in XYZ of the obj, starting from [0,0,0]), and also to adjust the pixel intensity of a given object 
	 * (to indicate when it is selected, or has been classified).
	 */
	ObjectVoxelProcessing borderObjPixProcessing3D;


	/**
	 * This contains the data that is transferred from the MCIB_SM_BorderObjPxCount3D object, including every identified
	 * object's location, size, shape and intensity measures.
	 */
	ObjectDataContainer objectData;
	
	/**
	 * This can hold the data to be saved from each ROI.  The ImageHandler class can return an RoiDataContainer object
	 * which holds the correct data for a given ROI.
	 */
	RoiDataContainer roiData;

	/**
	 * Procedure Settings XML File - contains the Exclusion Zone dimensions (XYZ dimensions of largest 95% of 
	 * objects), and details of the high and low pass filter settings.
	 */
	OM_ProcedureSettings om_ProcedureSettings;

	/**
	 * Contains the ROIs as defined by the Roi DiSector plugin.
	 */
	RoiAssessmentHandler roiHandler;

	/**
	 * The number of ROIs to be assessed.
	 */
	int nRois;

	/**
	 * ArrayList of ROIs.
	 */
	ArrayList<Roi> rois;

	/**
	 * ArrayList of ROIs with the exclusion zones applied to them.
	 */
	ArrayList<Roi> roisExcl;

	/**
	 * DialogWindow for this plugin.
	 */
	DialogWindow dw;
	
	FileSelector roiFS;
	
	JCheckBox roiCheckBox;

	ImageHandler imageHandler;

	/**
	 * RUN METHOD - sets up the DialogWindow and User Inputs.
	 */
	@Override
	public void run(String arg) {

		dw = new DialogWindow("SM Analyser: Single Channel", this);

		//Add FileSelectors:

		//Input Images:
		dw.addFileSelector("Input Image(s):");
		
		// Add Checkbox to choose NO INPUT for ROIs -> each image will be assumed to have one ROI the size of 
		//the image.  This will apply the classic DiSector Frame to each image for Stereological Assessment.
		roiCheckBox = new JCheckBox("Use ROIs:");
		roiCheckBox.setAlignmentX(Component.RIGHT_ALIGNMENT);
		dw.add( roiCheckBox );

		//Input ROI Files -> must be matched input.
		roiFS = dw.addFileSelector("Input ROI(s):");

		
		roiFS.setEnabled(false);
		
		roiFS.setImageSelected(FileSelector.FILE_IMAGE); // this ensures this FileSelector 
											// will not prevent the Process Button from being active
											// even if no input is selected..
		
		// will activate or de-activate the roiFS
		roiCheckBox.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(roiCheckBox.isSelected()) {
					roiFS.setEnabled(true);
					roiFS.matchedInput = FileSelector.MATCHED_INPUT;
					// set to matched input to force Process button to match the input
				}
				else {
					roiFS.setEnabled(false);
					roiFS.matchedInput = FileSelector.ANY_INPUT;
					roiFS.setImageSelected(FileSelector.FILE_IMAGE); 
					// this ensures this FileSelector 
					// will not prevent the Process Button from being active
					// even if no input is selected..
				}
			}
			
		});

		// File Selector to add Threshold & Object Filter:
			// store to local variable to add action listener to the fileChooser
		objManagerFS = dw.addFileSelector("Input Obj. Manager:");

		// add action listener to detect presence of OM_MetaData file (to check if the input is a legitimate
			// Threshold & Object Filter input):
		objManagerFS.fileChooser.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				// Once FileChooser has made a selection, set the selected directory to om_MetaData:
				om_MetaData = new File(objManagerFS.fileChooser.getSelectedFile().getAbsolutePath() 
						+ File.separator + "OM_MetaData");

				// set the boolean value whether OM_MetaData exists in the inputArray:
				// if an incorrect directory or no directory is selected, exists() will return FALSE
				// inputArray.setValue(OBJ_MANAGER_INPUT, om_MetaData.exists() );
				
				// enable Exclusion Zone Selectors if whole object analysis was selected 
					// in this om-ProcedureSettings:
				activateExclusionZoneSelector( om_MetaData.exists() );

				// set ExternalInput on DW if om_metaData does exist:
					//according to the inputArray logical And operation:
				dw.setExternalInput( om_MetaData.exists() );

			}

		});


		dw.add(new JSeparator() ); //separate file selector remaining settings:

		//COLLECTING THE PRE-SET SETTINGS from SM Settings:
		//setup();

		// *** Additional User Input:


		//IMAGE THRESHOLDING:  Selected in "Input Object Manager" FileSelector above!


		//OBJECT FILTERING:  Selected in "Input Object Manager" FileSelector above!


		//  *** OBJECT ANALYSIS ***

		// CHANNEL SELECTION -> Which Channel should be analysed in this Analysis Run:

		// Channel Selection is made in Object Manager - and selected in "Input Object Manager" FileSelector above!

					// dw.add( addChannelSelector() );

		// EXCLUSION ZONE & HIGH PASS FILTER:  Both Determined by The Object Manager Plugin
			
			// This data is passed in via the OM FileSelector above

				//Exclusion zones set in SM Settings -> present again for user to edit locally.
				//ALSO ASK -> Does User want to apply Exclusion Zone in Z?
				//As the image may capture the entire object, in which case no Exclusion Zone should be applied in Z...
				// In this instance, do not apply exclusion zone in XY either!
				// MORE GENERALLY -> should be able to select the Exclusion Zone application to each dimension X, Y & Z.
		
				// SHOULD THE EXCLUSION ZONES EITHER BE ON OR OFF?!  Why would one want to select them as active
					// in only ONE DIMENSION?!
		
				//HIGH PASS FILTER -> already set in SM Settings -> Do Not Need To ask again for user to edit locally.
				//This is simply the minimum size of object which will be analysed - determined by PSF for Deconvolution
				//or min obj size thresholded from RAW images (hard to determine this!)
		
				//NOTE THIS ALSO DEPENDS ON THE RESOLUTION!!!!
				//Best for user to determine this for each analysis with appropriate plugin...
				
		// This is all determined by the Object_Manager Plugin!

		// STILL NEED TO SELECT whether the Exclusion Zones should be applied or not:
			// This DEPENDS on selection in Object Manager Plugin:
		
				// Whole Object Analysis:
		
					// Turning on Exclusion Zones will apply these to one half of all image edges, and will reject
						// all objects that sit completely inside them
		
						// This will ELIMINATE bias in the object sample

					// Not selecting Exclusion Zones will result in a BIASED selection IF there is a SAMPLING
		
						// If the whole Region under analysis is fully reconstructed, then it is correct to NOT
							// SELECT the Exclusion Zones with whole object analysis.
		
				// OBJECT FRAGMENT ANALYSIS:
		
					// Exclusion Zones should ALWAYS be OFF in this analysis, as all objects are RETAINED (non
						// rejected at image boundary), and exclusion zones would only serve to remove any fragments
						// that existed only in the Exclusion Zone - THUS RESULTING IN A BIASED SAMPLE OF FRAGMENTS!
		
		// inactivate the Exclusion Zone Selectors IF Object Fragment Analysis is run
			// ALREADY IMPLEMENTED VIA: activateExclusionZoneSelector()
		dw.add( addExclusionZoneSelector() );

		// dw.add( addEdgeObjectSelector() );


		// BELOW SHOULD BE SELECTED IN THE PLUGIN OPTIONS - Add these to the StereoMateSettings for this
		// Plugin, and provide a means of accessing these options from the DialogWindow.
	
			//OBJECT MEASUREMENTS:
			//What object measurements does the user wish to make?
				//Simple Checkboxes here will suffice
	
	
			//SUPERVISION - does User wish to Supervise this Analysis?
				//A checkbox will suffice
				// if true, the analysis will display each image: its thresholding and processing.


		// ActionPanel & DialogWindow settings & display:

		dw.addActionPanel(); //add Action panel - for Cancel and Process buttons.

		dw.setExternalInput(false); //set ExternalInput to false, this needs to be set to 'true'
		// before the "process" button can be pressed & the algorithm started.

		dw.setPlugInSuffix("_ANALYSER"); //suffix for the file or DIR output.

		dw.layoutAndDisplayFrame(); //display the DialogWindow.


	} //end run()



	protected JPanel addChannelSelector() {

		//A panel to store the channels and PSF selection buttons.
		JPanel p = new JPanel();

		//Number of Channels:
		//Make this initially BLANK to make sure the User selects a number of
		//Channels before proceeding [cannot press PROCESS unless this is selected, AND
		//PSFs are selected for Each Channel - this means WHENEVER this is edited, it 
		//should ALWAYS set 'setExternalInput' to false!]

		JPanel comboBoxAndButtonPanel = new JPanel();

		JLabel channelsLabel = new JLabel("Which Channel to Analyse:");

		comboBoxAndButtonPanel.add(channelsLabel);

		//Set of Strings for the ComboBox - beinning with a BLANK String (forces the user to select
		//a channel number - they consciously have to do it, therefore less likely to make a mistake..)
		String[] channels = new String[] { "", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" };
		channelsComboBox = new JComboBox<String>(channels);

		//set actionListener to channelsComboBox - this only needs to set the channelIndex variable
		//to the index the user has selected.
		//USER must select a number in this input!  Need to make sure this happens:

		//Ideally, this should also affect the ExternalInput, but this requires some extra
		//Coding:
		//Specifically -> need to add a new inherited method to the StereoMateAlgorithm interface,
		//where this method defines what NEW attributes need to be checked, and checks them, to
		//return a boolean to DialogWindow [ESP in verifyMatchedInput()] to ensure the PROCESS
		//button is blocked appropriately with ALL input, and not over-ridden by the FileSelectors
		//MATCHED INPUT..

		//TODO Add inherited method checkExternalInput() to StereoMateAlgorithm, and put this method
		//into the appropriate places in DialogWindow...

		//this needs to set the PsfSelectorButton to active
		//if a channel number is selected (i.e. the index of selection is above 0), or deactivate the
		//PsfSelectorButton if no channel is selected.
		//The DialogWindow itself should also have ExternalInput set to false if index selection is 0,
		//to deal with bug if channel is selected and then unselected by the user.
		channelsComboBox.addActionListener( new ActionListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void actionPerformed(ActionEvent e) {
				cb = (JComboBox<String>)e.getSource();
				//chan = (String)cb.getSelectedItem();
				channelIndex = cb.getSelectedIndex();

				// set the boolean value whether channelIndex is above 0:
				// inputArray.setValue(CHANNELSELECTOR, (channelIndex > 0) );

				// set ExternalInput on DW according to the inputArray logical And operation:
				// dw.setExternalInput( inputArray.getLogicalAnd() );

				//TODO set the DialogWindow output DIR name -> add suffix to "C" + channelIndex!
			}

		});

		//Add this combobox to the Panel:
		comboBoxAndButtonPanel.add(channelsComboBox);

		p.setLayout(new BorderLayout() );

		p.setMinimumSize( new Dimension(200, 200) );

		p.add(comboBoxAndButtonPanel, BorderLayout.CENTER);

		return p;

	} //end addChannelSelector()


	public JPanel addEdgeObjectSelector() {

		// Generate new JPanel:
		JPanel p = new JPanel();

		// generate the JCheckBoxes:
		edgeObjectCheckBox = new JCheckBox("Include Edge Objects");

		// Set them to Selected initially:
		edgeObjectCheckBox.setSelected(false);

		// Add to JPanel:
		p.add(edgeObjectCheckBox);

		// Return JPanel:
		return p;

	}


	/**
	 * Provides a JPanel which contains two JCheckBoxes - which allows the user to select whether an Exclusion
	 * Zone is applied in Z and/or in XY.
	 * @return JPanel containing the two JCheckBoxes for selecting Exclusion Zones in XY and Z.
	 */
	public JPanel addExclusionZoneSelector() {

		// Generate new JPanel:
		JPanel p = new JPanel();

		// generate the JCheckBoxes:
		exclusionXYCheckBox = new JCheckBox("Exclusion Zone XY");
		exclusionZCheckBox  = new JCheckBox("Exclusion Zone Z");
		
		exclusionXYCheckBox.setToolTipText(
				"Select to apply an Exclusion Zone in XY: "+
				"removes all objects which sit within max obj length of one edge."
				);
		
		exclusionZCheckBox.setToolTipText(
				"Select to apply an Exclusion Zone in Z: "+
				"removes all objects which sit within max obj length of one edge."
				);

		// Set them to Selected & diabled initially:
		exclusionXYCheckBox.setSelected(false);
		exclusionZCheckBox.setSelected(false);
		
		exclusionXYCheckBox.setEnabled(false);
		exclusionZCheckBox.setEnabled(false);

		// Add to JPanel:
		p.add(exclusionXYCheckBox);
		p.add(exclusionZCheckBox);

		// Return JPanel:
		return p;

	}
	
	/**
	 * Loads OM_ProcedureSettings from om_metaData (the passed boolean should determine if om_metaData
	 * does exist), and then checks the content of objectAssessmentMode -> if it is set to WHOLEOBJECT 
	 * the exclusionZone checkboxes are enabled, otherwise they are disabled.
	 */
	public void activateExclusionZoneSelector(boolean om_metaDataExists) {
	
		if(om_metaDataExists) {
			// as om_metaData exists, the om_ProcedureSettings MUST also exist:
			
			loadOmProcedureSettings();

			// This is either TRUE or FALSE
			if(  om_ProcedureSettings.isWholeObjectAnalysis()  ) {
				exclusionXYCheckBox.setEnabled(true);
				exclusionZCheckBox.setEnabled(true);
				
				exclusionXYCheckBox.setSelected(true);
				exclusionZCheckBox.setSelected(true);
			}
			else {
				exclusionXYCheckBox.setEnabled(false);
				exclusionZCheckBox.setEnabled(false);
				
				exclusionXYCheckBox.setSelected(false);
				exclusionZCheckBox.setSelected(false);
			}

		}
		
		else { // need to add this in case a legit dir is selected, then a non
					// legit one is selected!
			exclusionXYCheckBox.setEnabled(false);
			exclusionZCheckBox.setEnabled(false);
			
			exclusionXYCheckBox.setSelected(false);
			exclusionZCheckBox.setSelected(false);
		}
		
	}



	/**
	 * SETUP - involves collecting the variables for this analysis.
	 */
	@Override
	public void setup() {
		
		IJ.showStatus("SM Analyser:");
		
		IJ.log("");
		IJ.log("###########################");
		IJ.log("            SM Analyser:");
		IJ.log("###########################");
		IJ.log("");
		IJ.log("");
		
		IJ.log("    Gathering Settings...");

		// Initial set-up: Retrieve Threshold and Object Manager information:
		
		// 1. Threshold Procedure Stack
		
		// 2. OM_ProcedureSettings - including Exclusion Zone dimensions & any Object Filter settings.
		
		// 3. Any Weka Classifier that may have been saved by Object_Manager.

		// om_MetaData directory already retrieved in the fileSelector listener in run().
		File[] om_files = om_MetaData.listFiles();

		
		// 1. Threshold Procedure Stack
		
		procedureStack = findAndLoadProcedureStack(om_files);

		
		// Make an ObjectIdentifier object:
		String[][] objIdentifierInput = new String[][] {
			ObjectDataContainer.MANUALCLASSTITLESVALS,
			ObjectDataContainer.FILTERCLASSTITLESVALS,
			ObjectDataContainer.CLASSIFIERCLASSTITLESVALS,
		};

		objIdentifier = new ObjectIdentifier(ObjectDataContainer.BASE_VALUE, objIdentifierInput);
		
		// get both the FEATURE and CONNECTED Pixel Values - used to set objects to these pixel vals!
		featureObjVal = 100;
		featureInVal = 101;
		featureOutVal = 102;
		
		connectedObjVal = 120;
		connectedInVal = 121;
		connectedOutVal = 122;

		
		// new borderObjPixProcessing3D for dealing with Border Voxel Processing:
		borderObjPixProcessing3D = new ObjectVoxelProcessing( om_ProcedureSettings.getObjectConnectivity() );

		//create new ObjectDataContainer to collect data from objAssessment3d26() method.
		objectData = new ObjectDataContainer();
		
		// create new RoiDataContainer - to collect Roi-Level Data:
		roiData = new RoiDataContainer();
		
		// Load ExclZone Dimensions and Object Filter/Classifier:
			// loadOmProcedureSettings() - loads the ExclZone Dimensions, if necessary
		
		// load the Filter, if indicated in OM Procedure Settings:
			// AFTER construction of objIdentifier!
		loadFilter();

		// load the Classifier, if present:
		loadClassifier();
		
		// create Roi Dataset with all ROI Attrs:
			// This holds the data for Each ROI @ ROI level: ROI volume, size, mean median SD IQR for
			// some Attributes at the object-level in this ROI:
		roiOverviewDataset = new DatasetWrapper(dw.getCurrentOutputFile().getName(), 
				RoiDataContainer.returnAttributes() );

	}

	/**
	 * Finds the procedure stack file in the Files array om_files, based on the first three
	 * characters of the file title being "PS_".  It then loads and returns an
	 * ImageProcessingProcedureStack3 object based on this file.
	 * @param om_files
	 * @return
	 */
	public ImageProcessingProcedureStack3 findAndLoadProcedureStack(File[] om_files) {

		File procedureStackFile = null;

		// loop through all files in OM_MetaData to try to find the following files:
		// 1. Threshold Procedure Stack - starts with "PS_"
		for(int a=0; a<om_files.length; a++) {
			String fileName = om_files[a].getName();
			//IJ.showMessage( fileName.substring(0, 3) );
			if(fileName.substring(0, 3).equalsIgnoreCase("PS_")) {
				procedureStackFile = om_files[a];
				//IJ.showMessage( "procedureStackFile: "+procedureStackFile.getAbsolutePath() );
				break;
			}
		}

		// Load procedureStack:
		// add try.. catch to catch exception when the procedureStack has not loaded properly:
		return ImageProcessingProcedureStack3.loadStack(procedureStackFile);

	}

	/**
	 * Constructs an om_ProcedureSettings object and loads the appropriate XML file from the
	 * om_MetaData directory.
	 */
	public void loadOmProcedureSettings() {

		// Load OM_ProcedureSettings - Exclusion Zone dimensions & any Object Filter:
		om_ProcedureSettings = new OM_ProcedureSettings();

		//IJ.showMessage("OM_ProcedureSettings check pre load: "+om_ProcedureSettings.maxX.returnContent() );

		om_ProcedureSettings.loadXmlDoc(om_MetaData);

	}

	/**
	 * Loads the classifier, if it exists (based on file name "OM_classifier.model"), from the 
	 * om_MetaData directory.
	 * <p>
	 * Note, if the objectClassifier does not exist, it will not load and will remain NULL.
	 */
	public void loadClassifier() {
		File f = new File(om_MetaData.getAbsolutePath() + File.separator + "OM_classifier.model");
		if(f.exists() ) {
			// create an objectClassifier:
			objectClassifier = new ObjectClassifier( ObjectDataContainer.FEATURE_INDEX );
			// load the classifier
			objectClassifier.loadClassifier( f );
		}
	}
	
	/**
	 * Loads the filter, if it exists (based on filter values in the om_ProcedureSettings.xml
	 * file, in om_MetaData directory).
	 * <p>
	 * Note, if the settings for the filter indicate NO filter is to be applied, the objectFilter
	 * object will remain NULL.
	 */
	public void loadFilter() {
		
		//if((int)om_ProcedureSettings.filterIndexVal.getContent() > 0 ) {
		if(om_ProcedureSettings.getFilterIndexValue() > 0 ) {
			// create an objectFilter:
			objectFilter = new ObjectFilter(objIdentifier);
		}
		
	}

	/**
	 * Defines the processing of each image -> collecting images and ROIs & analysing the correct objects
	 * in each ROI (post BORDER and EXCLUSION ZONE Filtering).
	 */
	@Override
	public void process(ImagePlus imp) {
		
		
		IJ.log("");
		IJ.log("");
		IJ.log("Assessing Image: "+imp.getTitle() );
		
		IJ.showStatus("SM Analyser: Applying Threshold");
		
		IJ.log("");
		IJ.log("    Applying Threshold Procedure Stack...");
		
		// this builds the imageHandler and generates the Thresholded image stacks:
		imageHandler = new ImageHandler(imp, procedureStack, om_ProcedureSettings.getObjectConnectivity() );
		
		
		IJ.showStatus("SM Analyser: Collecting ROIs");
		IJ.log("");
		IJ.log("    Collecting ROIs...");

		rois = getRois(imp);
		
		roiDatasets = new ArrayList<DatasetWrapper>();
		generateDataInstances(roiDatasets, rois.size() ); 
			// The data here contains the Measures Attributes AND the Classifier Class Attribute

		
		IJ.showStatus("SM Analyser: Assessing Objects");

		IJ.log("");
		IJ.log("    Assessing Objects...");
		
		assessObjects( getDatasetPath() );

		
		IJ.showStatus("SM Analyser: Constructing ROIs");
		
		IJ.log("");
		IJ.log("    Constructing ROIs...");
		
		
		int exclXY = om_ProcedureSettings.getMaxXYMean();
		if( exclusionXYCheckBox.isSelected() == false) {
			exclXY = 0;
		}
		int exclZ = (int)om_ProcedureSettings.maxZ.getContent();
		if( exclusionZCheckBox.isSelected() == false) {
			exclZ = 0;
		}

		roiHandler = new RoiAssessmentHandler(rois, roiDatasets, 
													0, 0, 
													imp.getWidth(), imp.getHeight(), 
													exclXY, exclZ );
		
		// *** WHOLE OBJECT ANALYSIS *** //
		
		if( om_ProcedureSettings.isWholeObjectAnalysis() ) {
			
			if(  ( objectClassifier != null && objectClassifier.isLoaded() ) == false  ) {
				
				// CLASSIFIER IS NOT LOADED:  Analyse just "FEATURE" (ALL) Objects
				
				// Only run the Analyser on FEATURE objects - WITH roiOverviewDataset
			
				IJ.showStatus("SM Analyser: Analysing Objects - WHOLE OBJECT");

				IJ.log("");
				IJ.log("    Analysing WHOLE OBJECTS...");

				roiHandler.analyseRoisWholeObjects(imageHandler, objectDataset, firstPixObjNoMap, 
						roiOverviewDataset, dw.getCurrentOutputFile(),
						featureObjVal, featureInVal, featureOutVal, ObjectDataContainer.FEATUREATR,
						exclusionXYCheckBox.isSelected(), exclusionZCheckBox.isSelected() );
			
			}
						
			else if(  ( objectClassifier != null && objectClassifier.isLoaded() )  ) {
				
				// CLASSIFIER IS LOADED: Analyse both FEATURE and CONNECTED objects
				
				// Run the Analyser on FEATURE objects WITHOUT roiOverviewDataset
				
				IJ.showStatus("SM Analyser: Analysing Objects - WHOLE OBJECT");

				IJ.log("");
				IJ.log("    Analysing WHOLE OBJECTS...");

				roiHandler.analyseRoisWholeObjects(imageHandler, objectDataset, firstPixObjNoMap, 
						dw.getCurrentOutputFile(),
						featureObjVal, featureInVal, featureOutVal, ObjectDataContainer.FEATUREATR,
						exclusionXYCheckBox.isSelected(), exclusionZCheckBox.isSelected() );

				// can only have connected objects if an objectClassifier is NOT NULL and is LOADED
				
				// Then run the Analyser on CONNECTED objects - WITH roiOverviewDataset

				IJ.showStatus("SM Analyser: Analysing Objects - WHOLE OBJECT CONNECTED");

				IJ.log("");
				IJ.log("    Analysing WHOLE OBJECTS - CONNECTED...");

				roiHandler.analyseRoisWholeObjects(imageHandler, objectDataset, firstPixObjNoMap, 
						roiOverviewDataset, dw.getCurrentOutputFile(),
						connectedObjVal, connectedInVal, connectedOutVal, ObjectDataContainer.CONNECTEDATR,
						exclusionXYCheckBox.isSelected(), exclusionZCheckBox.isSelected() );
			
			}
			
			// this is INEFFICIENT running the analyse() method TWICE, but its a quick fix until the
				// borderObjPixProcessing3D Objects methods can be adjusted to handle BOTH feature 
					// and connected numbers..
		}
		
		// *** OBJECT FRAGMENT ANALYSIS *** //
		else {

			if(  ( objectClassifier != null && objectClassifier.isLoaded() ) == false  ) {

				// Only run the Analyser on FEATURE objects - WITH roiOverviewDataset

				IJ.showStatus("SM Analyser: Analysing Objects - OBJECT FRAGMENT");

				IJ.log("");
				IJ.log("    Analysing OBJECT FRAGMENTS...");

				roiHandler.analyseRoisObjectsAndFragments(imageHandler, 
						roiOverviewDataset, dw.getCurrentOutputFile(),
						featureObjVal,  ObjectDataContainer.FEATUREATR,
						exclusionXYCheckBox.isSelected(), exclusionZCheckBox.isSelected() );	
			}

			else if(  ( objectClassifier != null && objectClassifier.isLoaded() )  ) {

				// can only have connected objects if an objectClassifier is NOT NULL and is LOADED

				// Run the Analyser on FEATURE objects WITHOUT roiOverviewDataset
				
				IJ.showStatus("SM Analyser: Analysing Objects - OBJECT FRAGMENT");

				IJ.log("");
				IJ.log("    Analysing OBJECT FRAGMENTS...");

				roiHandler.analyseRoisObjectsAndFragments(imageHandler, 
						dw.getCurrentOutputFile(),
						featureObjVal,  ObjectDataContainer.FEATUREATR,
						exclusionXYCheckBox.isSelected(), exclusionZCheckBox.isSelected() );
				
				// Then run the Analyser on CONNECTED objects - WITH roiOverviewDataset

				IJ.showStatus("SM Analyser: Analysing Objects - OBJECT FRAGMENT CONNECTED");

				IJ.log("");
				IJ.log("    Analysing OBJECT FRAGMENTS - CONNECTED...");

				roiHandler.analyseRoisObjectsAndFragments(imageHandler,
						roiOverviewDataset, dw.getCurrentOutputFile(),
						connectedObjVal, ObjectDataContainer.CONNECTEDATR,
						exclusionXYCheckBox.isSelected(), exclusionZCheckBox.isSelected() );

			}
			
		// this is INEFFICIENT running the analyse() method TWICE, but its a quick fix until the
		// borderObjPixProcessing3D Objects methods can be adjusted to handle BOTH feature 
			// and connected numbers..
		}
		
		IJ.log("");
		IJ.log("    Saving ROI Datasets...");
				
		saveInstancesData(roiDatasets, dw.getCurrentOutputFile(), ".csv" );

	}


	/**
	 * 
	 */
	@Override
	public void cleanup() {

		//  Summarise data across ROIS & images -> output.
		
		IJ.log("");
		IJ.log("    Saving ROI Overview Dataset...");

		// SAVE the dataOverview Instances as CSV:
		File outputFile = new File(dw.getCurrentOutputFileNoPause().getParent() + File.separator + "RoiData");
		saveInstancesData( roiOverviewDataset, outputFile, ".csv" );

		//NOTE: Means ACROSS ROIs can be calculated from a series of ROIs by taking the WEIGHTED SUM
		//of ROI means (ie. the Sum of the mean x the fraction of ROI obj number to Total obj number)

		//Therefore, do not need to store ALL data to the end of processing for summary stats - just need
		//obj COUNT for each ROI, and the mean of the different measures, and can calculate
		//the mean of pooled ROI values.

		//NOTE: Does not work for MEDIAN values!  Any data where median values are needed cannot be used.
		//This is likely ALL of them!  Since the median is the more appropriate measure for non-normal data.

		//Have a careful think about what data should be output here...  I am only going to output UP TO the
			// ROI-level, and therefore will not give summary stats ACROSS ROIs in one image...

		// I think that further Data Analysis can be performed in R!


		// SHUTDOWN DialogWindow & other components to remove Memory Leaks:


		//remove ActionListeners:
		// ActionListener[] als = channelsComboBox.getActionListeners();
		// for(int a=0; a<als.length; a++) {
			// channelsComboBox.removeActionListener(als[a]);
		// }


		//Shutdown all variables in this class:

		//ROIs:
		rois = null;
		roisExcl = null;


		//DialogWindow instance variables - to collect information from user:
		channelsComboBox = null;
		cb = null;
		
		
		removeListeners();
		
		shutdownSMA();
		
		shutdownDW();
	

		//Image Labeller to label objs for assessment:
		// labeler = null;

		//ImageInt obj for labelling images:
		// bin = null;
		// impInt = null;
		// impIntThresh = null;
		// res2 = null;	


	} //end cleanup()
	
	public void removeListeners() {
		StereoMateUtilities.removeActionListener(roiCheckBox);
		StereoMateUtilities.removeActionListener(objManagerFS.fileChooser);
		StereoMateUtilities.removeActionListener(channelsComboBox);
	}
	
	public void shutdownSMA() {		
		
		channelsComboBox = null;
		cb = null;

		 exclusionXYCheckBox = null;
		 exclusionZCheckBox = null;

		 edgeObjectCheckBox = null;

		 objManagerFS = null;

		 om_MetaData = null;

		 procedureStack = null;

		 objectDataset = null;

		 objIdentifier = null;
		
		 objectFilter = null;
		
		 objectClassifier = null;

		 roiOverviewDataset = null;

		 roiDatasets = null;

		 arffPath = null;

		 firstPixObjNoMap = null;

		 borderObjPixProcessing3D = null;

		 objectData = null;

		 roiData = null;

		 om_ProcedureSettings = null;

		 roiHandler = null;

		 rois = null;

		 roisExcl = null;
		
		 roiFS = null;
		
		 roiCheckBox = null;

		 imageHandler = null;
		
	}
	
	public void shutdownDW() {
		dw.shutDownDialogWindow();
		dw = null; //DialogWindow object - to create a dialog to select files for
							//processing.
		System.gc();
	}



	public void addInstanceToData(Instances arff, int objRef, Instances data ) {

		// get the instance from arff at objRef, and put it into data:
		data.add( arff.get(objRef) );

	}


	/**
	 * Collects the ROIs from the CurrentFile on FileSelector index 1 in DialogWindow object (dw).
	 * The rois are opened and stored into an ArrayList<Roi>, which is returned after ensuring all
	 * ROIs in the ArrayList are of type PolygonRoi.
	 * <p>
	 * Note, NO ARGS are needed for this method!
	 * @param roiParent
	 * @param impFileName
	 * @return
	 */
	public ArrayList<Roi> getRois(ImagePlus imp) {

		if(roiFS.inputOutputFramework.fileArray == null) {
			// if no ROIs are selected, return the whole image as one ROI:
			
			int[] xpoints = new int[]{ 0, imp.getWidth(), imp.getWidth(), 0 };
			int[] ypoints = new int[]{ 0, 0, imp.getHeight(), imp.getHeight() };
			int npoints = 4;
			
			Roi roi = new PolygonRoi( new Polygon(xpoints,ypoints, npoints), PolygonRoi.POLYGON );
			
			rois = new ArrayList<Roi>();
			rois.add(roi);
			
			return rois;
		
		}
		else {
			// check if the ROIs and Image match!
			//IJ.showMessage("FileSelector0 current file path: "+dw.getCurrentFile(0).getAbsolutePath() );
			//IJ.showMessage("FileSelector1 current file path: "+dw.getCurrentFile(1).getAbsolutePath() );

			rois = new ArrayList<Roi>();

			//obtain ArrayList of Rois via openZip():
			// rois = openZip( roiPath );
			rois = openZip ( dw.getCurrentFile(1).getAbsolutePath() );

			// ensure each ROI is a PolygonRoi:
			return convertToPolygonRois(rois);
			
		}

	}
	
	/**
	 * Returns the path to the current images potential ".arff" dataset in the 3rd FileSelector (OM input).
	 * It achieves this by taking the 3rd fileSelectors filePath, and attaching this to the first fileSelectors
	 * RelativeCurrentFile (using getCurrentFileRelativeToInputNoExt() ) - the NoExt modification is used to
	 * return this current file with no extension.  The ".arff" extension is added and this absolute file
	 * path is returned.
	 * @return
	 */
	public String getDatasetPath() {

		// get the output file name for the arff data table:
		//String arffPath = dw.getCurrentOutputFileNoPause().getAbsolutePath() +".arff";
		// arffPath = dw.getCurrentOutputFile().getAbsolutePath() +".arff";
		// IJ.showMessage("Output File: "+dw.getCurrentOutputFile().getAbsolutePath() );
		// IJ.showMessage("currentFile 2: "+dw.getCurrentFile(2) );
		// IJ.showMessage("InputFile 2: "+dw.getInputFile(2) );
		// IJ.showMessage("currentFile on FS0: "+dw.getCurrentFile(0).getAbsolutePath() );
		// IJ.showMessage("FS0 - filePath: "+dw.fileSelectors.get(0).filePath);
		// IJ.showMessage("the current input file from selected input DIR: "+dw.getCurrentFile(0).getAbsolutePath().
		///		                                 substring( dw.fileSelectors.get(0).filePath.length() ));
		// IJ.showMessage("CurrentFileRelativeToInput: "+dw.getCurrentFileRelativeToInput(0) );	
		// IJ.showMessage("Data File in FileSelector 02 OM: "+dw.fileSelectors.get(2).filePath + File.separator + dw.getCurrentFileRelativeToInputNoExt(0) +".arff");
		
		return dw.fileSelectors.get(2).filePath + File.separator + dw.getCurrentFileRelativeToInputNoExt(0) +".arff";
		
	}


	/**
	 * Saves all the instances in data to the output file, using a numbering system for each ROI, and
	 * the extension supplied (should be ".arff" or ".csv").
	 * @param data
	 * @param outputFile
	 * @param extension
	 */
	public void saveInstancesData(ArrayList<DatasetWrapper> data, File outputFile, String extension ) {

		for(int a=0; a<data.size(); a++) {

			String path = outputFile.getAbsolutePath() + "_0" + a + extension;

			//IJ.showMessage("path to save data (index: "+a+"): "+path);

			try {
				DataSink.write( path, data.get(a).data );
				//DataSink.write(dw.getCurrentOutputFile().getAbsolutePath() +".arff", inst);
			} catch (Exception e) {}

		}

	}
	
	/**
	 * Saves all the instances in data to the output file, using a numbering system for each ROI, and
	 * the extension supplied (should be ".arff" or ".csv").
	 * @param dataset
	 * @param outputFile
	 * @param extension
	 */
	public void saveInstancesData(DatasetWrapper dataset, File outputFile, String extension ) {


		String path = outputFile.getAbsolutePath() + extension;

		//IJ.showMessage("path to save data (index: "+a+"): "+path);

		try {
			DataSink.write( path, dataset.data );
			//DataSink.write(dw.getCurrentOutputFile().getAbsolutePath() +".arff", inst);
		} catch (Exception e) {}


	}


	/**
	 * Saves all the instances in data to the output file, using a numbering system for each ROI, and
	 * the extension supplied (should be ".arff" or ".csv").
	 * @param data
	 * @param outputFile
	 * @param extension
	 */
	public void saveInstancesData(Instances data, File outputFile, String extension ) {


		String path = outputFile.getAbsolutePath() + extension;

		//IJ.showMessage("path to save data (index: "+a+"): "+path);

		try {
			DataSink.write(path, data);
			//DataSink.write(dw.getCurrentOutputFile().getAbsolutePath() +".arff", inst);
		} catch (Exception e) {}


	}



	/**
	 * This method imports ROIs from ZIP file paths passed to it.
	 * @param path Path to a Zip File containing ROIs.
	 * @return An ArrayList of ROIs.
	 */
	// Modified on 2005/11/15 by Ulrik Stervbo to only read .roi files and to not empty the current list
	public ArrayList<Roi> openZip(String path) { 
		//ZipInputStream -> to read the Zip File.
		ZipInputStream in = null; 
		//OutputStream -> to write the data to.
		//Data in this is then converted to a ByteArray -> which can be accepted by a RoiDecoder.
		ByteArrayOutputStream out = null; 
		//set nRois to 0:
		nRois = 0; 
		try { 
			//generate new ZipInputStream -> wrap via FileInputStream:
			in = new ZipInputStream(new FileInputStream(path)); 
			//create new byte[] buffer:
			byte[] buf = new byte[1024]; 
			int len; 
			//retrieve next file in ZipInputStream:
			ZipEntry entry = in.getNextEntry(); 

			while (entry!=null) { 
				//check if file is an ".roi" file:
				String name = entry.getName();
				if (name.endsWith(".roi")) { 
					//read data from ZipInputStream
					//and put it directly into ByteArrayOutputStream:
					out = new ByteArrayOutputStream(); 
					while ((len = in.read(buf)) > 0) 
						out.write(buf, 0, len); 
					//close the ByteArrayOutputStream (as have read all data):
					out.close(); 
					//and store the data into a byte[] array:
					byte[] bytes = out.toByteArray(); 
					//decode the Roi in bytes using RoiDecoder:
					RoiDecoder rd = new RoiDecoder(bytes, name); 
					Roi roi = rd.getRoi(); 
					if (roi!=null) { 
						rois.add(roi); 
						nRois++;
					} 
				} 
				entry = in.getNextEntry(); 
			} 
			in.close(); 
		} catch (IOException e) {
			IJ.error(e.toString());
		} finally {
			if (in!=null)
				try {in.close();} catch (IOException e) {}
			if (out!=null)
				try {out.close();} catch (IOException e) {}
		}
		//if(nRois==0)
		//IJ.error("This ZIP archive does not appear to contain \".roi\" files");
		return rois;
	} 







	/**
	 * Converts all ROIs in rois to Polygon ROIs.  If the roi is a straight line selection, an error
	 * message is returned, and the algorithm will skip this ROI.
	 * @param rois
	 */
	public ArrayList<Roi> convertToPolygonRois(ArrayList<Roi> rois) {

		for(int a=0; a<rois.size(); a++) {
			if(rois.get(a) instanceof PolygonRoi) {
				// do nothing
			}
			else {
				// convert to Polygon ROI:
				Polygon polygon = rois.get(a).getPolygon();	
				if(polygon == null) {
					// then the roi @ a is a straight line selection and cannot be processed - inform the user:
					IJ.log("ROI number: "+a+" is a Straight Line Selection and \n"
						+" cannot be processed in this analysis.  It has been \n"
						+" omitted; please correct and re-run as necessary");
				}
				else {
					// can make a PolygonRoi from the polygon, and set it to rois:
					rois.set(a, new PolygonRoi(polygon, PolygonRoi.POLYGON ) );
				}
			}
		}

		return rois;


	}



	/**
	 * This method assesses all objects inside the imp, using the MCIB3D library.  No ROIs are being applied here,
	 * but all objects which are touching the image edge are removed.  
	 * <p>
	 * Objects are assessed for a range of attributes:  Location, Size, Shape parameters, Intensity.  
	 * Objects are also identified by their FIRST VOXEL - this is the voxel which is first encountered in the first 
	 * Z plane the object is in.  This is used to move between the object in the image and the object in the generated 
	 * data table - FAR MORE EFFICIENT THAN CODING EACH OBJECT WITH ITS OWN SEPARATE PIXEL INTENSITY VALUE!
	 * <p>
	 * To move from First Voxel to an object in the data table, use a Hash Map object that maps the object row and its
	 * first voxel (presented as a list of integers - seefirstPixObjNoMap).
	 * <p>
	 * The Data Table will be stored as an ARFF format for three reasons:
	 * <p>
	 * - ARFF format is the WEKA format, and the data may need to be exported to this format to read into weka to build
	 * advanced machine learning classifiers from the data.
	 * <p>
	 * - When manually classifying, want to add the ability for the plugin to suggest the next best object to classify,
	 * based on its closeness to the Classification Boundary.  To determine this boundary need to use a CLASSIFIER ->
	 * WEKA!  To apply this classifier to the data need to use ARFF data format!
	 * <p>
	 * - Finally, WEKA has good (okay?) pre-made classes for visualising datasets from ARFF files.  By using the ARFF 
	 * file format, I can take advantage of these classes for the user to visualise the objects in a graph, colour 
	 * coded by class, and which allow the user to select any attribute to put on X or Y axis.
	 * 
	 * @param thresholdImp The image which contains the series of thresholded objects to be assessed.
	 */
	public void assessObjects(String arffPath) {

		// only remove is whole object analysis:
		imageHandler.removeEdgeObjects( om_ProcedureSettings.isWholeObjectAnalysis() );
		
		File arffFile = new File(arffPath);

		if(arffFile.exists() == false) {
			
			// create Object Dataset with all Object Measures AND CLASSIFICATION Attrs:
			objectDataset = new DatasetWrapper(dw.getCurrentOutputFile().getName(), 
					ObjectDataContainer.returnAllObjectMeasuresClassificationsAttributes() );
						
			// set all objects to featureObjVal - assume they are all FEATURE OBJECTS until shown otherwise...
			extractObjData(255, featureObjVal);

		}

		else if(arffFile.exists() == true) {
			
			// create Object Dataset with all Object Measures and only Manual Classification Attrs:
			objectDataset = new DatasetWrapper(dw.getCurrentOutputFile().getName(), 
					ObjectDataContainer.returnObjectMeasuresAndManClassAttributes() );

			// Load Data from the Arff File (will not contain Filter or Classifier Attrs!):
			objectDataset.loadData( arffFile, ObjectDataContainer.returnObjectMeasuresAndManClassAttributes() );

			// NOW - add the Filter and Classifier Attrs to objectDataset:
			addFilterAndClassifierAttrs(objectDataset);

			// Set the Pixel Values in the imageHandler: to featureObjVal until shown otherwise...
			setObjectPixelValues(featureObjVal);

		}
		
		// objectDataset contains all Measures and all Classification Attributes
		// The objectFilter and objectClassifier are set to DEFAULT value0.0 
			// below will fill these with the correct values...

		objectDataset.compactify();

		buildFirstPixObjNoMap(objectDataset); // generate hash map between FirstPixel and ObjNo

		objectDataset.setClassAttribute( ObjectDataContainer.MANUALCLASS ); // necessary for CLASSIFIER!
		
		// *** Apply the Filter and then the Classifier, if they exist ***
		
		// Only apply Object Filter if objectFilter does not equal NULL:
		if(objectFilter != null) {
			
			IJ.log("");
			IJ.log("        Applying Object Filter...");
			
			// first, add dataset and image to filter:
			objectFilter.addDatasetAndImage(objectDataset, imageHandler);
			
			// then, apply the filter - ONLY TO THE objectDataset
				// DO NOT ADJUST PIXEL VALUES!!  Pass FALSE to setPixelValues in filterObjects() method:
			objectFilter.filterObjects(
					om_ProcedureSettings.getFilterMinValue(),
					om_ProcedureSettings.getFilterMaxValue(),
					(om_ProcedureSettings.getFilterIndexValue() - 1),
					false );
			// need to pass objIDentifier to this method, but its not used as setPixelValues is FALSE!
		}
		// the objectDataset FILTERCLASS will have been adjusted according to any applied filter, but the imageHandler
			// will NOT have adjusted any pixel values -> all objects are still set to pixel value: featureObjVal.
				
		// only apply objectClassifier if a Classifier has been LOADED:
		if(objectClassifier != null) {

			if( objectClassifier.isLoaded() ) {
				
				IJ.log("");
				IJ.log("        Applying Object Classifier...");
			
				// apply Classifier - ONLY TO THE objectDataset
				// DO NOT ADJUST PIXEL VALUES:  Pass FALSE to setPixelValues in classifyObjects() method
				objectClassifier.classifyObjects(objectDataset, imageHandler, objIdentifier, 
						om_ProcedureSettings.isApplyFilterOverClassifier(),
						false ); 
						// need to pass objIDentifier to this method, but its not used as setPixelValues is FALSE!
				
					// This will set all NON-FILTERED objects to NON-FEATURE, FEATURE or CONNECTED
						// All FILTERED Objects are set to NON_FEATURE
					// NB: Remember by default ALL OBJECTS are set to FEATURE initially!
			}
		}
		// the objectDataset CLASSIFIERCLASS will have been adjusted according to any applied classifier, 
			// but the imageHandler will NOT have adjusted any pixel values -> 
				// all objects are still set to pixel value: featureObjVal
		
		// The FINAL STEP here now is to actually FILTER OBJECTS based on the Filter/Classifier existence & values:
		applyFilterClassifierToObjectPixelValues();

	}


	/**
	 * Extracts the data for all objects in the imageHandler thresholdImgInt.
	 */
	public void extractObjData(int readPixelValue, int setPixelValue) {

		// make sure the arff data object is EMPTY:
		// objectDataset.delete();  // It will be empty as a new object is generated in assessObjects()

		IJ.showStatus("Object Manager: Assessing Objects - gathering data");

		// setup loop through thresholdImgInt in imageHandler:
		imageHandler.setupLoopThresholdImgInt(0, 0, 0, 0);

		// using a while loop, extract all data from each object in thresholdImgInt:
		while(true) {
				// false -> not collect convex measures at present (due to computational constraints)
			objectData = imageHandler.assessThresholdImgIntObj(readPixelValue, setPixelValue, false); 
				// returns null when end of image reached.
			if(objectData == null) {
				break;
			}
			//Put the data from dataObj into the arff data structure:
			objectDataset.addData( objectData.returnData(), objectData.returnDataTitles() );
			IJ.showStatus("Object Manager: Assessing Objects - gathering data");
		}

		// showArffDatatable(arff);
		// IJ.showMessage("New Arff Data Table for New Image.");

		// save the images' ARFF Dataset - MINUS the Filter and Classifier Columns:
			// DO NOT SAVE THE DATA - as SM Analyser will focus on and save ROI data only
		//objectDataset.saveData(arffPath, 
			//	ObjectDataContainer.returnObjectMeasuresAndManClassAttributes() ); //this method uses the arffPath to save data to correct output.

	}

	/**
	 * Adds the Filter and Classifier Attributes to the datasetHandler dataset.
	 */
	public void addFilterAndClassifierAttrs(DatasetWrapper objectDataset) {

		objectDataset.addAttributeToInstances(ObjectDataContainer.returnFilterClassAttribute());
		objectDataset.setAttributeValueOnInstances(ObjectDataContainer.FILTERCLASS, 0.0);

		objectDataset.addAttributeToInstances(ObjectDataContainer.returnClassifierClassAttribute());
		objectDataset.setAttributeValueOnInstances(ObjectDataContainer.CLASSIFIERCLASS, 0.0);

	}

	/**
	 * Sets the pixel values in imageHandler of each object found in datasetHandler to the correct value 
	 * based on the values in Manual, Filter and Classifier Class Attributes in datasetHandler.  The pixel 
	 * value used is the featureObjVal, set in this class.  The pixel value is set on the 
	 * imageHandler with the setObjValue() method.
	 */
	public void setObjectPixelValues(int objVal) {

		for(int a=0; a<objectDataset.numInstances(); a++) {
			//  get this objects First Pixel value (XYZ):
			int x = (int)objectDataset.get(a, ObjectDataContainer.X1);
			int y = (int)objectDataset.get(a, ObjectDataContainer.Y1);
			int z = (int)objectDataset.get(a, ObjectDataContainer.Z1);

			// set all objects to featureObjVal - assume they are all FEATURE OBJECTS until shown otherwise...
			imageHandler.setObjValue(x, y, z, objVal);

		}
	}

	/**
	 * Creates a new firstPixObjNoMap object using the X1 Y1 and Z1 Attributes for first pixel coordinates,
	 * and the objNo Attribute as the value to lookup.  The firstPixObjNoMap is then built using the
	 * passed ObjectDatasetHandler object.
	 * @param data
	 */
	public void buildFirstPixObjNoMap(DatasetWrapper data) {

		ArrayList<String> objMapKeyList = new ArrayList<String>();
		objMapKeyList.add( ObjectDataContainer.X1 );
		objMapKeyList.add( ObjectDataContainer.Y1 );
		objMapKeyList.add( ObjectDataContainer.Z1 );
		firstPixObjNoMap = new ObjectDatasetMap( objMapKeyList, ObjectDataContainer.OBJNO );

		firstPixObjNoMap.buildMap(data);

	}

	/**
	 * Builds size number of DatasetWrapper objects in the passed data object, with
	 * each object containing the Measurement Attributes and the Classifier Class Attribute.
	 * <p>
	 * The Classifier Classification Attribute IS added here.  The Manual Classification and Filter
	 * Classification are both not required - as all objects have to be Filter:PASSED, and their
	 * Manual Classification can be ANY.  BUT the Classifier Classification is still required, to ensure
	 * any CONNECTED and FEATURE objects, both of which are relevant to any analysis, are correctly
	 * attributed in the data.
	 * <p>
	 * This Classifier Class Attribute will be set to either FEATURE for all objects, unless an object
	 * is Classified as CONNECTED by any Classifier applied.
	 * @param data
	 * @param size
	 */
	public void generateDataInstances(ArrayList<DatasetWrapper> data, int size ) {

		// generate the correct number of instances objects in the data array list:
		for(int a=0; a<size; a++) {
			data.add( 
						new DatasetWrapper(dw.getCurrentOutputFile().getName(), 
						// ObjectDataContainer.returnAllObjectMeasuresClassificationsAttributes(), 
						//ObjectDataContainer.returnObjectMeasuresAttributes() 
						ObjectDataContainer.returnObjectMeasuresAndClassifierClassAttributes() ) 
					);
		}

	}
	
	/**
	 * Applies the Filter and/or Classifier values to each object pixel values in the image.  This method
	 * will set each object to 0 if it does not pass the Filter/Classifier as specified in the Object
	 * Manager.  If the Object passes the Filter/Classifier as specified in the Object Manager, its
	 * pixel values are UNALTERED -> this means every object which passes the Filter/Classifier will
	 * remain with pixel values set to 224 (UNCLASSIFIED:PASSED:FEATURE).
	 * 
	 * @return
	 */
	public void applyFilterClassifierToObjectPixelValues() {
		
		if(objectClassifier != null) {
			if( objectClassifier.isLoaded() ) {
				
				if(objectFilter != null) {
					IJ.log("");
					IJ.log("        Adjust Object Pixel Values - Filter & Classifier...");
					// Filter -> Classifier is applied: Modify pixel values according to the CLASSIFIER VALUES:
						// With this setup, if Filter is applied ON the Classifier, the Classifier values are modified too,
						// so just need to modify the imageHandler object pixel values based on the CLASSIFIER Values:
					// set to 0 if NONFEATURE or CONNECTED, do not adjust if FEATURE:
					adjustObjectPixelValuesByClassifier();
				}
				else {
					IJ.log("");
					IJ.log("        Adjust Object Pixel Values - Classifier...");
					// Classifier only applied:  Modify pixel values according to the CLASSIFIER VALUES:
					// set to 0 if NONFEATURE or CONNECTED, do not adjust if FEATURE:
					adjustObjectPixelValuesByClassifier();
				}
				
			}
		}
		else {
			
			if(objectFilter != null) {
				IJ.log("");
				IJ.log("        Adjust Object Pixel Values - Filter...");
				// Filter Only Applied:  Modify pixel values according to the FILTER VALUES:
				// set to 0 if NOTPASSED, do not adjust if PASSED:
				adjustObjectPixelValuesByFilter();
			}
			
		}
		
		// if this code is reached, objectClassifier is NOT loaded and objectFilter EQUALS null
		// so ALL OBJECTS are assessed -> do NOT modify the object pixel values in imageHandler!
		// return 0;
		
	}
	
	/**
	 * This will set any objects pixel values in imageHandler to 0 if its CLASSIFIERCLASS value in objectDataset
	 * is NONFEATURE or CONNECTED.
	 */
	public void adjustObjectPixelValuesByClassifier() {
		
		// loop through each Instance in datasetHandler:
		for(int a=0; a<objectDataset.size(); a++) {
			
			// get the CLASSIFIERCLASS value from the objectDataset:
			String classifierClassValue = ObjectDataContainer.getValueTitleFromIndex(
					ObjectDataContainer.CLASSIFIERCLASS,
					(int)objectDataset.get(a, ObjectDataContainer.CLASSIFIERCLASS) );
			
			if(classifierClassValue.equals(ObjectDataContainer.NONFEATUREATR) ) {
				// if the Classifier Value is NONFEATURE - set object pixel value to
				// 0 -> effectively ELIMINATING this from the image:
				// get the firstPixel coordinate for this instance:
				//IJ.log("");
				//IJ.log("NON_FEATURE OBJECT: "+a);
				int x = (int)objectDataset.instance(a).value(objectDataset.attribute(ObjectDataContainer.X1));  // x1
				int y = (int)objectDataset.instance(a).value(objectDataset.attribute(ObjectDataContainer.Y1));  // y1
				int z = (int)objectDataset.instance(a).value(objectDataset.attribute(ObjectDataContainer.Z1));  // z1
				imageHandler.setObjValue(x, y, z, 0);
			}
			else if( classifierClassValue.equals(ObjectDataContainer.CONNECTEDATR) ) {
				// if the Classifier Value is CONNECTED - set object pixel value to connectedObjVal
				// get the firstPixel coordinate for this instance:
				IJ.log("");
				IJ.log("CONNECTED OBJECT: "+a);
				int x = (int)objectDataset.instance(a).value(objectDataset.attribute(ObjectDataContainer.X1));  // x1
				int y = (int)objectDataset.instance(a).value(objectDataset.attribute(ObjectDataContainer.Y1));  // y1
				int z = (int)objectDataset.instance(a).value(objectDataset.attribute(ObjectDataContainer.Z1));  // z1
				imageHandler.setObjValue(x, y, z, connectedObjVal);
			}
			// else - the classifier value is FEATURE -> so keep the object as is in the image!
			
		}
		
	}
	
	public void adjustObjectPixelValuesByFilter() {

		// loop through each Instance in datasetHandler:
		for(int a=0; a<objectDataset.size(); a++) {

			// get the FILTERCLASS value from the objectDataset:
			String filterClassValue = ObjectDataContainer.getValueTitleFromIndex(
					ObjectDataContainer.FILTERCLASS,
					(int)objectDataset.get(a, ObjectDataContainer.FILTERCLASS) );

			if(filterClassValue.equals(ObjectDataContainer.NOTPASSEDATR) ) {
				// if the Filter Value is NOTPASSED - set object pixel value to
				// 0 -> effectively ELIMINATING this from the image:
				// get x y z first pixel:
				// get the firstPixel coordinate for this instance:
				int x = (int)objectDataset.instance(a).value(objectDataset.attribute(ObjectDataContainer.X1));  // x1
				int y = (int)objectDataset.instance(a).value(objectDataset.attribute(ObjectDataContainer.Y1));  // y1
				int z = (int)objectDataset.instance(a).value(objectDataset.attribute(ObjectDataContainer.Z1));  // z1
				imageHandler.setObjValue(x, y, z, 0);
			}
			// else - the filter value is PASSED -> so keep the object as is in the image!

		}

	}

}
