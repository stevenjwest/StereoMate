package stereomate.plugins;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
import org.w3c.dom.NodeList;

import com.sun.corba.se.spi.orbutil.threadpool.Work;
import com.sun.xml.internal.ws.api.Component;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GUI;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.LUT;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.processing.Flood3D;
import stereomate.data.ClassificationStats;
import stereomate.data.DatasetWrapper;
import stereomate.data.MaximumDimensions;
import stereomate.data.ObjectDataContainer;
import stereomate.data.ObjectDatasetMap;
import stereomate.dialog.DialogWindow;
import stereomate.image.ImageHandler;
import stereomate.image.ImageProcessingProcedureStack3;
import stereomate.image.ImageWindowWithPanel;
import stereomate.lut.ClassificationModeLUTs;
import stereomate.object.ObjectClassifier;
import stereomate.object.ObjectFilter;
import stereomate.object.ObjectIdentifier;
import stereomate.object.SelectObjects;
import stereomate.object.SelectedObject;
import stereomate.settings.OM_ProcedureSettings;
import stereomate.settings.StereoMateUtilities;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.Utils;
import weka.core.converters.ConverterUtils.DataSink;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Add;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.RenameAttribute;
import weka.filters.unsupervised.instance.RemoveWithValues;
import weka.gui.arffviewer.ArffPanel;
import weka.gui.visualize.MatrixPanel;
import weka.gui.visualize.VisualizePanel;


/**
 * 
 * The Object_Manager:
 * 
 * <p>
 * 
 * This class is primarily for Manual Classification of thresholded objects derived from the application of
 * an optimised Thresholding Procedure (defined in the Threshold Manager class) to a set of images.  The 
 * classification of objects is into two fundamental classes:  The FEATURE CLASS & NON-FEATURE CLASS.
 * <p>
 * As objects are defined into different classes manually, the aim is to create a classification scheme 
 * which successfully divides the FEATURE and NON-FEATURE CLASSES generally such that future images can have 
 * the FEATURE CLASS extracted without user input.
 * <p>
 * Objects can be classified manually by clicking to select them in the image, and then clicking the FEATURE
 * or NON-FEATURE buttons on the panel to classify them.  Classified objects will be colour coded, and the 
 * classification can be changed to unclassified, classified-feature or classified-nonFeature for any 
 * object.
 * <p>
 * The user can also be directed to boundary objects (objects that reside near the classification boundary - 
 * between FEATURE and NON-FEATURE classes), or likely-FEATURE or likely-NON-FEATURE objects to classify.
 * This is directed by a Naive Bayes or IBk classifier set up on the currently classified data to direct the 
 * selection of unclassified objects likely to reside in these regions of the graph.
 * <p>
 * (The User can actually select the probability bounds of objects for the Naive Bayes algorithm to identify
 * and show to the user.)
 * <p>
 * The User can also select an attribute and ranges of values to select objects based on attribute values, 
 * and the User can select individual objects based on their Object Number (this may be obtained from the ARFF
 * data or Graph visualisations, below).
 * <p>
 * The user can view a Graph of the objects, colour coded by manual classification, which can help visualise
 * the classification boundary & to identify where to apply Filters.
 * <p>
 * The plugin supports simple Object Filtering of the data.  This includes low- and high- pass filters
 * of objects based on a single attribute - size, shape, etc.  These will also show up on the Graph when visualised.  
 * These amount to drawing straight lines on the graph plotting objects against their attributes.
 * <p>
 * The plugin also supports machine learning classifiers.  Once manual classification has delineated the classification
 * boundary sufficiently, the user can export the data to a Weka ARFF file containing all classified objects.  This 
 * ARFF file can be used to identify, train and test a classifier to classify unknown objects.  The trained classifier 
 * can be imported into this Plugin, which will use the Weka Library to apply it to the image, and future images.
 * <p>
 * The image post filtering and/or application of a Weka Classifier, can then be visualised to check for any errors 
 * by eye.  The filters / classifiers can then be applied to the full series of images to confirm by eye they work
 * well across images which have not been manually classified - i.e. that they are GENERALISABLE.
 * <p>
 * Finally, the Filters and Classifiers can be saved for future recall in the SM_Analyser Algorithm.
 * 
 * @author stevenwest
 *
 */
public class Object_Manager implements PlugIn, StereoMateAlgorithm, MouseListener, KeyListener {

	/**
	 * DialogWindow - to control image input and output, and initial settings (threshold algorithm selection).
	 */
	DialogWindow dw;
	
	/**
	 * Determine whether to save the thresholded images for future loading from disk.
	 */
	JCheckBox saveThresholdCheckBox;

	/**
	 * To hold the output of the algorithm - data output, files detailing the thresholding and filtering (if applied),
	 * and to hold the classifier model (if applied).
	 */
	File OM_MetaData;

	/**
	 * Objects to construct an XML file.
	 */
	Document docSMS;
	DocumentBuilder dBuilder;
	DocumentBuilderFactory dbFactory;

	/**
	 * String variables to hold the options for this plugin.
	 */
	String gaussianObjStr, linearObjStr, maxObjSelStr, roughClassifierStr, sliceProjectionStr;

	/**
	 * Boolean to indicate whether the initial (first image, first time) image is being processed, or now
	 * an image after the initial image is being processed.  Set to true at start of process() method, and
	 * set to false in the previousButton and nextButton actionListeners.
	 * <p>
	 * This is required as the process() method not completing means saving the ARFF file initially required
	 * the NoPause() clause on fetching correct file from DialogWindow.  Whereas after the initial image,
	 * as the method: incrementTotalFileCountAndCurrentFileIndex() is called on the DW, the NoPause() clause
	 * is NOT needed on the file collection from DW!
	 * <p>
	 * It ensures the correct retrieval of the file path to save the ARFF is given, to ensure correct data is
	 * saved to correct arff file representing an image.
	 * <p>
	 * I HAVE NOW MOVED THIS FUNCTIONALITY INTO THE DIALOGWINDOW ITSELF.
	 */
	//boolean processingInitialImage;
	
	/**
	 * Stores the path for the saved threshold image.
	 */
	String thresholdPath;

	/**
	 * String represents the path of the current output destination for the arff dataset.  Used to save
	 * the dataset, and also compare to see if it currently exists.
	 */
	String arffPath;

	/**
	 * String represents the path of the current output destination for any PNG image to be saved.  The
	 * file name will be incremented each time the image is saved (using pngIndex int).
	 */
	String pngPath;

	/**
	 * This is set to 0 when a new image is opened, and incremented each time an image is saved to pngPath.
	 * This int is tagged onto the end of pngPath to allow multiple images to be saved with a different
	 * int appended.
	 */
	int pngIndex;

	/**
	 * Panels for DialogWindow options: a load JButton for threshold algorithm selection, and a load
	 * JLabel to indicate which threshold procedure has been selected.
	 */
	JPanel threshSelectorPanel;
	JButton loadButton;

	JPanel threshLabelPanel;
	JLabel loadLabel;
	
	JRadioButton wholeObjectButton, objectFragmentButton;
	
	ButtonGroup assessmentModeButtonGroup;
	
	JRadioButton connected6Button, connected18Button, connected26Button;
	
	ButtonGroup objectConnectivityButtonGroup;

	// JCheckBox processAllCheckBox;

	/**
	 * This object holds all image references (including the original imp, activeChannel & thresholdImp, and the
	 * ImageInt wraps of activeChannelInt, thresholdImgInt, as well as the procedureStack.
	 */
	ImageHandler imageHandler;

	/**
	 * ImagePlus object to store the original imp in:
	 */
	ImagePlus originalImp;

	/**
	 * The 'procedure stack' - description of the thresholding procedure to be applied to the images.
	 */
	ImageProcessingProcedureStack3 procedureStack;

	/**
	 * The procedure stack title - used to save the procedure stack to the output DIR OM_MetaData.
	 */
	String procedureStackTitle;

	/**
	 * Reference to the stack of ImageProcessors into one IMP, which is the output of applying the 
	 * threshold procedureStack to a channel in the original imp.  Allows easy access to the thresholded stack 
	 * in the IWP ImagePlus.
	 */
	// ImagePlus thresholdImp;

	/**
	 * Reference to the image stack in the original imp which contains the ImageProcessors representing the channel being
	 * processed.  This is used to build an ImageInt which will collect intensity data on objects which are defined by the
	 * thresholded imp.
	 */
	// ImagePlus activeChannel;

	/**
	 * The thresholdImp which has been wrapped into an ImageInt - the TANGO / MCIB 3D representation of an image.
	 * Used for rapid identification and measurement of objects.
	 */
	// ImageInt thresholdImgInt;

	/**
	 * The activeChannel which has been wrapped into an ImageInt - the TANGO / MCIB 3D representation of an image.
	 * This ImageInt is used for measuring intensity data of objects (as the threhsoldImgInt is only a mask, whereas
	 * the activeChannelInt contains the raw intensity data).
	 */
	// ImageInt activeChannelInt;

	/**
	 * Holds the current image and the processing panel, for annotating the dataset.  Includes reference to the whole imp,
	 * including original channels and additional threshold channel, and also the original stack as well as the 
	 * projected imp for display.
	 */
	ImageWindowWithPanel IWP;

	/**
	 * Two integers which hold the initial first and last projection slices for the IWP Image Projection.  This can be set
	 * in Object Manager settings.
	 */
	int[] sliceProjectionArray;

	/**
	 * ArrayList of all the Attributes added to the ARFF data object.
	 */
	ArrayList<Attribute> attributes, attributesBuffer;

	/**
	 * ClassificationModeLUTs object creates and holds references to the LUTs to be used in displaying the thresholded
	 * imp in IWP.
	 */
	ClassificationModeLUTs classModeLUTs;

	/**
	 * Int to represent INDEX of the manClass, filterClass & classifierClass:
	 * 
	 */
	// int MANCLASS, FILTERCLASS, CLASSIFIERCLASS;

	/** Named Constants of Attribute Nominal Values */

	// final String UNCLASSIFIEDATR = "Unclassified", 	FEATUREATR = "Feature"; 
	// final String NONFEATUREATR = "Non-Feature", 	CONNECTEDATR = "Connected";
	// final String PASSEDATR = "Passed", 				NOTPASSEDATR = "Not-Passed";

	/** Value indexes for Unclassified, Feature, Non-Feature and Connected 
	 * in the Manual, Passed & Not-Passed in the Filter and Non-Feature, Feature
	 * and Connected in the Classifier Attributes of ARFF dataset.
	 */
	// protected int MAN_UNCLASSIFIED_INDEX, MAN_FEATURE_INDEX, MAN_NONFEATURE_INDEX, MAN_CONNECTED_INDEX;

	// protected int FIL_PASSED_INDEX, FIL_NOTPASSED_INDEX;

	// protected int CLAS_UNCLASSIFIED_INDEX, CLAS_NONFEATURE_INDEX, CLAS_FEATURE_INDEX, CLAS_CONNECTED_INDEX;

	/**
	 * Integer ArrayLists to hold all the possible pixel values for different classifications in a given
	 * mode:  Manual, Filter, Classifier.
	 * <p>
	 * Manual:  manualUnclassified, manualFeature, manualNonFeature, manualConnected.
	 * <p>
	 * Filter:  filterPassed, filterNotPassed.
	 * <p>
	 * Classifier:  classifierFeature, classifierNonFeature, classifierConnected.
	 * <p>
	 * These array lists are used to test which group a given objects pixel value is in.
	 */
	// ArrayList<Integer> manualUnclassified, 	manualFeature, 			manualNonFeature, 	manualConnected;
	// ArrayList<Integer> filterPassed, 		filterNotPassed;
	// ArrayList<Integer> classifierFeature, 	classifierNonFeature, 	classifierConnected;

	/**
	 * An Instances Object - which holds the ARFF data object.
	 */
	DatasetWrapper objectDataset;

	/**
	 * An ArffBuffer Object which holds each instance which has been manually classified in this image or
	 * since the manClassArffBuffer was saved with the saveArffButton.  Also includes a BufferIndex, which
	 * is used to record which dataset a given instance was from.
	 */
	//Instances manClassArffBuffer;
	ArffBuffer manClassArffBuffer;
	// ObjectDatasetHandler manClassArffBuffer;

	/**
	 * Instances objects to hold only classified instances (FEATURE, NONFEATURE, CONNECTED), and 
	 * only unclassified instances.
	 * <p>
	 * NOTE: The Class Attribute still can POTENTIALLY hold all four object types (unclass., non-feat., feat., conn.),
	 * but its just that these datasets only actually hold classified or unclassified instances.
	 */
	// Instances classified;
	// Instances unclassified;

	/**
	 * This Instances object represents the unclassified objects, but includes first pixel information too,
	 * this dataset will have an additional attribute added to it, where the probability of each instance being
	 * a feature object will be added.
	 */
	// Instances unclassifiedFirstPix;  //TODO can the unclassifiedFirstPix and unclassified be combined?! NO!


	/**
	 * Hash Map to map the FIRST VOXEL in a Object3DVoxels object to the index of that object in the ARFF
	 * data object.  The FIRST VOXEL can be retrieved from a selected  Object3DVoxel object in an image by calling the 
	 * static getFirstVoxel(obj3Dvox) method in MCIB_SM_BorderObjPixCount3D, returning a Point3D obj.
	 * <p>
	 * Using a List<Integer> to represent the First Voxel as this generates a good and unique Hash Value for each
	 * coordinate supplied.
	 */
	//HashMap<List<Integer>, Integer> firstPixObjNoMap;
	ObjectDatasetMap firstPixObjNoMap;


	/**
	 * This class contains methods to select objects in ImageInt's, and to change the voxel values of these objects or
	 * measure them in different ways.  
	 * <p>
	 * Used here to build the object attributes data (arff), and also to identify any object's FirstPixel (first pixel 
	 * encountered in XYZ of the obj, starting from [0,0,0]), and also to adjust the pixel intensity of a given object 
	 * (to indicate when it is selected, or has been classified).
	 */
	// MCIB_SM_BorderObjPxCount3D borderObjPixProcessing3D;


	/**
	 * This contains the data that is transferred from the MCIB_SM_BorderObjPxCount3D object, including every identified
	 * object's location, size, shape and intensity measures.
	 */
	ObjectDataContainer dataObj;

	/**
	 * Object which holds all of the XML elements for the OM_ProcedureSettings.xml file.  Used for loading,
	 * setting and saving the OM Procedure Settings - including the max dimensions of objects, and the high
	 * and low pass filter settings.
	 */
	OM_ProcedureSettings om_ProcedureSettings;

	/**
	 * Object to hold data for maximum dimensions of objects.
	 */
	MaximumDimensions maxDimensions;

	/**
	 * These integers store the maximum object length in the X Y and Z dimensions.  This information is stored in
	 * the OM_MetaData folder (in the OM_ProcedureSettings.xml file), and is used in the SM Analyser algorithm
	 * to set the Exclusion Zone Dimensions in X, Y and Z.
	 * <p>
	 * They are set based on the max object dimensions found in the set of images presented to the algorithm (the
	 * mean of the 95% largest objects in a given dimension).
	 * <p> 
	 * This data is ESSENTIAL for setting up any Exclusion Zones in the image for Stereological Filtering.
	 */
	// int maxX, maxY, maxZ;

	/**
	 * These ArrayLists keep track of the all object dimensions measures which fall in the top 5% for each image
	 * assessed.  Once an image has been assessed (once it has an ARFF dataset saved to output), then these
	 * arrays will NOT be filled AGAIN with the same data.  These will accumulate all data from all assessed
	 * images to compute the mean 95-100% dimension sizes for objects.
	 */
	// ArrayList<Integer> xDimArrayTotal, yDimArrayTotal, zDimArrayTotal;

	/**
	 * This object defines the pixel values (identifies objects) across the range of three Object Classification
	 * Ranges:  Manual Classification, Filter Classification, Classifier Classification.
	 */
	ObjectIdentifier objIdentifier;

	/**
	 * SelectedObject object - represents the FirstPixel of the currently selected object.  If no object is selected,
	 * this will either represent the default FirstPixel (0,0,0) [which is the first pixel in the IMAGE], or
	 * if a pixel has been previously selected, it will represent this value.
	 */
	SelectedObject selectedObj;

	ClassificationStats manClassStats;


	/**
	 * This generic panel will hold all of the components which control the Object Manager - the parent panel to
	 * this algorithm.
	 */
	JPanel objManagerPanel;

	/**
	 * Classify Panels and Buttons - allow user to classify the currently selected object.
	 */
	JPanel classifyPanel;

	JButton featureButton, nonFeatureButton, unclassifiedButton, connectedButton;

	/**
	 * View Panel, label and buttons - allow user to adjust the object view (manual classification, filter
	 * classification, or classifier classification views).
	 */
	JPanel viewPanel;

	JLabel viewLabel;

	ButtonGroup viewButtonGroup;

	JRadioButton filterViewButton, classifierViewButton, manualViewButton;

	/**
	 * Selected Object Panel and Text Area to display information on the selected object.
	 */
	JPanel selectedObjInfoPanel;

	JTextArea selectedObjInfo;

	JScrollPane selectedObjInfoScrollPane;

	/**
	 * Plot Panel and Button - allow user to access plots of the ARFF dataset.  Plots derived from Weka Library.
	 */
	JPanel plotPanel;

	JButton imagePlotButton, manualPlotButton, imageDataTableButton;




	int MAX_OBJ_SEL;

	/**
	 * An ObjectClassifier Object to represent the Classifier used to ML driven object selection.  This is either a
	 * rough ML classifier generated using the pre-set Classifier option & pre-set attributes selected, or
	 * it is the imported classifier.
	 */
	ObjectClassifier objectClassifier;

	/**
	 * This String represents the Classifier Class for the rough ML Classifier, or if imported the string is set
	 * to IMPORTED (to represent when a Classifier has been imported!).
	 */
	String CLASSIFIER;


	// JPanel objSelLabelPanel;

	JPanel objSelRandPanel;
	

	/**
	 * Allows selection of the Class type for Object Annotation Selection to occur on.
	 */
	JComboBox<String> objSelComboBox;
	
	JButton objSelRandOptionsButton, objSelRandButton, objSelBiasedButton;
	
	/**
	 * This will contain all the Object Selection Options - Random object Number, Biased Object
	 * Selection High and Low Probability, and the Biased Object Selection Selector String.
	 */
	JFrame objSelRandOptionsFrame;
	
	JLabel objSelHighLabel, objSelLowLabel, objSelNoLabel, objSelSeedLabel, objSelBiasedStringLabel;
	
	/**
	 * This holds the index to the Class type selected by the objSelComboBox.
	 */
	//int CLASS_INDEX;

	JSpinner objSelHigh, objSelLow, objSelNo, objSelSeed;

	SpinnerModel highModel, lowModel, noModel, seedModel; //Two models for the Spinners.

	JTextField biasedObjectSelectionTextfield;
	
	JButton objSelOKButton;


	JPanel objSelDistPanel;
	
	JButton linearButton, gaussianButton;

	String biasedSelectionString, linearObjectString;



	/**
	 * Boolean to inform the plugin when the Object Selection Thread is ACTIVE (i.e. objects are being selected
	 * and displayed by the ObjectSelectionThread).
	 * <p>
	 * When this boolean is true, new keyboard shortcuts become available for the user to traverse the object
	 * selection in the ObjectSelectionThread ('<' & '>'), to classify all objects (using the standard key shortcuts,
	 * 'F' 'D' 'C' 'S'), and to exit the thread (ENTER?) when they have finished the classification.
	 * completed their classification.
	 */
	boolean objSelThreadActive;

	/**
	 * An instance reference to the object selection thread - to control allow access to the thread across the
	 * plugin.
	 * <p>
	 * This thread implements the Runnable ObjectSelectionThread class.  The Thread is paused on DialogWindow (dw),
	 * and resumed on this object too.
	 */
	Thread objectSelectionThread;

	/**
	 * An instance reference to the objectSelectionThread Runnable object.  This contains the instance methods 
	 * incrementIndex() and decrementIndex(), which the plugin needs access to in the keyPressed() keyListener
	 * methods to implement behaviour.
	 */
	ObjectSelectionThread objectSelectionRunner;

	/**
	 * Label for Attribute Selection Panel.
	 */
	JLabel attrLabel;

	/**
	 * Check Box for Attribute Selection Panel
	 */
	JCheckBox attrCheckBox;

	/**
	 * ComboBox to set the attribute to select an object on.
	 */
	JComboBox<String> attrSelComboBox;

	/**
	 * Labels to label the attribute max val and attribute min val spinners.
	 */
	JLabel attrMaxLabel, attrMinLabel;

	/**
	 * Attribute max and min val spinners.
	 */
	JSpinner attrMax, attrMin;

	/**
	 * Attribute max and min models for Spinners.
	 */
	SpinnerModel attrMaxModel, attrMinModel; //Two models for the Spinners.

	/**
	 * Attribute Selection Button - to select a random object of object type indicated in attrSelComboBox, and with
	 * an attribute value between attrMax and attrMin values.
	 */
	JButton attrSelectionButton;

	/**
	 * Attribute Selection Panel - to hold all of the components for object selection based on range of attribute values.
	 */
	JPanel attrSelectionPanel;

	/**
	 * Int which hold the currently selected Attribute index on the Object Selection by Attribute Panel and ComboBox,
	 * and min and max attribute values for the filter.
	 */
	// int attrIndexVal;

	/**
	 * 	Double which holds the spinner's Minimum Value on the Object Selection by Attribute Panel and ComboBox.
	 */
	double attrMinVal;

	/**
	 * Double which holds the spinner's Maximum Value on the Object Selection by Attribute Panel and ComboBox.
	 */
	double attrMaxVal;

	/**
	 * ArrayList<Integer> for storing the object references with object selection by attribute, where the object 
	 * references is the sequence of objects with the selected attribute between attrMin & attrMax.
	 */
	ArrayList<Integer> attrObjRefList;

	/**
	 * This int represents which objRef index is to be selected next.
	 */
	//int attrObjIndex;

	/**
	 * ObjectFilter object to apply the logic of filtering the objects in the ObjectDatasetHAndler and Image.
	 */
	ObjectFilter objectFilter;

	/**
	 * Boolean flag whether a filter is selected or not.
	 */
	boolean filterSelected;

	/**
	 * Int to hold the index of the last selected attribute on filterComboBox - needed to remove 
	 * Filter from objects in arff appropriately (allows retrieval of previous selection when a new
	 * selection is made on filterComboBox).
	 */
	//int previousFilterAttributeIndex;

	/**
	 * ComboBox to set the attribute to filter an object on.
	 */
	JComboBox<String> filterComboBox;

	/**
	 * Labels to label the attribute max val and attribute min val spinners for Object Filtering.
	 */
	JLabel filterMaxLabel, filterMinLabel;

	/**
	 * Attribute max and min val spinners for Object Filtering.
	 */
	JSpinner filterMax, filterMin;

	/**
	 * Attribute max and min models for Spinners for Object Filtering.
	 */
	SpinnerModel filterMaxModel, filterMinModel; //Two models for the Spinners.

	/**
	 * Integer to store the current filter Attribute Index.
	 */
	// int filterIndexVal;

	/**
	 * Doubles to store the current filter min value and max value.
	 */
	// double filterMinVal, filterMaxVal;

	/**
	 * Integers to represent where the start index is for applying a new filter with new filterMax and filterMin
	 * values.  Makes the adjustment of a filter fast.
	 */
	// int filterMaxIndex, filterMinIndex;

	/**
	 * Checkbox to turn the filter on and off.
	 */
	JCheckBox filterCheckBox, filterOverClassifierCheckBox;
	
	/**
	 * This represents whether the filterMax is at the maximum.  This will control whether the setFilter() method,
	 * which is called between images, sets the filterMax value to the previou value.  This prevents a bug where
	 * a previous image with a smaller max object size/shape/whatever would set the new filterMax to this value,
	 * thus removing any objects smaller in the filtered value than the max value of the previous image.
	 * <p>
	 * This just mirrors the same boolean in the ObjectFilter class - it is kept here to allow it to be transfered
	 * between images (as the ObjectFilter is re-newed between images).
	 */
	// boolean filterMaxReached; // this logic has been moved to ObjectFilter!  AND also filterMinReached added too!

	/**
	 * Filter Panel - to hold all components for object filtering based on a single attribute and a low and high pass value.
	 */
	JPanel filterPanel;

	/**
	 * Checkbox to turn the Classifier on and off.
	 */
	JCheckBox classifierCheckBox;

	/**
	 * Boolean to indicate if an external classifier has been loaded.
	 */
	// boolean classifierLoaded;

	/**
	 * Instance variable to store the loaded classifier reference, useful to re-load the classifier quickly
	 * if the classifierCheckBox is unchecked and rechecked with a Classifier selected.
	 */
	// Classifier loadedClassifier;

	/**
	 * Instance variable to store the loaded classifier Attributes reference, useful to re-load the classifier quickly
	 * if the classifierCheckBox is unchecked and rechecked with a Classifier selected.
	 */
	// Instances loadedClassifierAttributes;

	/**
	 * Textfield to hold the NAME of the currently selected Classifier - blank if none selected.
	 */
	JTextField classifierTextField;

	/**
	 * Buttons to select and delete the classifier.
	 */
	JButton classifierSelectButton, classifierDeleteButton;

	/**
	 * Panel to hold Classifier Components.
	 */
	JPanel classifierPanel;

	/**
	 * This panel contains options to change images, to save the currently displayed image, save the Object Filter,
	 * and an options button and help button.
	 */
	JPanel optionsPanel;

	/**
	 * Buttons to move to next and previous images.
	 */
	JButton nextButton, previousButton;

	/**
	 * Panel to hold buttons for exporting the Object Filter data, and the currently displayed image.
	 */
	JPanel exportPanel;

	/**
	 * Buttons to export the Object Filter, and save the currently displayed image.
	 */
	JButton exportOMDataButton, saveImageButton;

	JFrame saveImageFrame;
	
	JButton saveImageOKButton;
	
	JLabel saveImageTitleLabel, saveImageZStackLabel;
	
	JTextField saveImageTitleTextfield;
	
	File saveImageFile;
	
	JCheckBox saveImageZStackCheckbox;
	
	/**
	 * Panel to hold options and help buttons.
	 */
	JPanel helpOptionsPanel;

	/**
	 * Buttons for Options and Help.
	 */
	JButton optionsButton, helpButton;


	/**
	 * Sets up the Dialog Window for the Object Manager PlugIn.  DialogWindow generated with one FileSelector (input
	 * images for object annotation and filtering), as well as two panels to hold the threshold procedureStack
	 * label/title and a button to select the procedureStack, respectively.  The DialogWindow can only be by-passed
	 * once a thresholdProcedure has been selected.  The DialogWindow only calls a single image - the ability to
	 * call further input images will be embedded into the processing panel on IWP.
	 */
	@Override
	public void run(String arg) {

		dw = new DialogWindow("Object Manager", this);

		dw.addFileSelector("Image or DIR to setup Object Filter:"); //add FileSelector panel.
		//dw.addFileSelector("Image02:", dw.FILES_AND_DIRS, dw.MATCHED_INPUT);

		// add ability to select threshold procedure:
		dw.add( addThresholdLabel() );

		dw.add( addThresholdSelector() );
		
		// Add ability to select the TYPE of Object Filtering (and the subsequent type of Analysis):
			// Whole Object
			// Object-Fragment:
		dw.add( addAssessmentModeSelector() );
		
		// Add ability to select the type of Object Connectivity:  6, 18, 26 connected
		dw.add( addObjectConnectivitySelector() );
		
		saveThresholdCheckBox = new JCheckBox("Save Segmented Images:");
		saveThresholdCheckBox.setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);
		dw.add( saveThresholdCheckBox );

		// add checkbox to select whether all images should be initially processed
		// auto-check to YES:
		// dw.add( addProcessAllCheckBox() );
			// No Longer adding this, as all images MUST be processed initially to get the maxXYZ dimensions!

		dw.addActionPanel(); //add Action panel - for Cancel and Process buttons.

		dw.setExternalInput(false); //set ExternalInput to false, this needs to be set to 'true'
		// before the "process" button can be pressed & the algorithm started.

		dw.setPlugInSuffix("_OM");  //This is probably not required.

		// moved to setup now, as dependent on state of processAllCheckBox
		//dw.setMethodCallNumber(1); //sets the loop to only call the process() method only ONCE.
		//Can open other images when necessary & by user selection...

		dw.layoutAndDisplayFrame(); //display the DialogWindow.

	}


	/**
	 * Add Selector to enable user to select a imageProcessingProcedureStack for thresholding the image.
	 * @return Panel containing selector for thresholding procedure, with appropriate behaviour.
	 */
	public JPanel addThresholdSelector() {

		// THIS SHOULD BE AVAILABLE IN THRESHOLD_MANAGER CLASS.

		//New JPanel to return:
		threshSelectorPanel = new JPanel();

		//All thresholding procedures are stored in XML files within StereoMate DIR.

		//Alow user to select these using a JFileChooser:

		//set layout
		// loadSavePanel.setLayout( new GridLayout(1,0) );
		//threshSelectorPanel.setLayout( new BoxLayout(threshSelectorPanel, BoxLayout.PAGE_AXIS) );

		//contruct the button and label:
		loadButton = new JButton( "Load Threshold Procedure" );

		loadButton.setToolTipText("Load an image thresholding procedure stack");

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

				//File file = null;	
				//try {
					//file = new File(StereoMateSettings.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
				//} catch (URISyntaxException e1) {
					//e1.printStackTrace();
				//}
				//Retrieve parent, and then formulate new file object pointing to the .thresholdProcedures DIR & the procedureTitle file:
				// file = file.getParentFile();
				// File thresholdProceduresFile = new File(file.getAbsolutePath() + File.separator + ".thresholdProcedures" + File.separator);

				File thresholdProceduresFile = dw.getOutputParentFile().getParentFile();
				
				//FileSystemView fsv = new SingleRootFileSystemView( thresholdProceduresFile );

				JFileChooser fc = new JFileChooser( thresholdProceduresFile );

				int returnVal = fc.showDialog( dw, "Load" );

				if(returnVal == JFileChooser.APPROVE_OPTION) {



					String stackTitle = fc.getSelectedFile().getName();

					procedureStackTitle = stackTitle;

					//String stackTitle = (String)JOptionPane.showInputDialog(
					//                    IWP.getWindow(),
					//                    "Please choose a procedure stack:",
					//                    "Load Procedure Stack",
					//                    JOptionPane.PLAIN_MESSAGE,
					//                    createImageIcon("Icons/Load 100x100 2.png", "Icon Toggle", 40, 40),
					//                    possibilities,
					//                    possibilities[0]);

					//generate procedureStack object with default values 8 bitdepth 1 activeChannel:
					// procedureStack = new ImageProcessingProcedureStack3(8,1);
					//Need to generate the procedureStack here to use loadStack() method below

					//load new procedureStack into the procedureStack variable:
					procedureStack = ImageProcessingProcedureStack3.loadStack( fc.getSelectedFile() );

					dw.setExternalInput(true);
					//Now that a procedureStack has been selected, set DialogWindow to make OK button active..

					//also set the label to corresponding text:
					loadLabel.setText("Procedure: "+stackTitle);

					//This is all that is needed here -> procedureStack is applied when image is loaded in the
					//process(imp) method.

				}

			}

		});

		threshSelectorPanel.add(loadButton);

		return threshSelectorPanel;

	}


	/**
	 * Add Panel with Label which shows user what ThresholdProcedure Stack has been loaded.
	 * @return Panel containing selector for thresholding procedure, with appropriate behaviour.
	 */
	public JPanel addThresholdLabel() {

		//New JPanel to return:
		threshLabelPanel = new JPanel();

		//set layout
		// loadSavePanel.setLayout( new GridLayout(1,0) );
		//threshLabelPanel.setLayout( new BoxLayout(threshLabelPanel, BoxLayout.PAGE_AXIS) );

		//construct the button and label:
		loadLabel = new JLabel("Please Select a Threshold Procedure..");

		//Set label font to BOLD:
		loadLabel.setFont(loadLabel.getFont().deriveFont(loadLabel.getFont().getStyle() | Font.BOLD));

		//add label to panel:
		threshLabelPanel.add(loadLabel);

		//return panel:
		return threshLabelPanel;

	}
	
	/**
	 * Returns a JPanel with Radio Buttons to select between Whole-Object assessment or 
	 * Object-Fragment assessment.  This will decide:
	 * <p>
	 * - Whether the objects in the image a EXCLUDED when touching the edges (whole-object) or INCLUDED
	 * (object-fragment).
	 * <p>
	 * - The measures performed on the objects, which can be used to filter or classify the objects.
	 * <p>
	 * - The form of analysis that takes place in the SM_Analyser plugin (the type of assessment is stored
	 * in the output of this plugin, and read by SM_Analyser to apply the appropriate analysis).
	 * 
	 * @return
	 */
	public JPanel addAssessmentModeSelector() {
		
		JPanel assessmentModePanel = new JPanel();
	
		//Create the radio buttons.
		wholeObjectButton = new JRadioButton("Whole Object Assessment");
		// wholeObjectButton.setEnabled(false);
		wholeObjectButton.setToolTipText(
				"Limits the Object Manager to"+
				"assess fully reconstructed objects within the image. Used to "+
				"assess images in which objects are fully reconstructed."
				);
		wholeObjectButton.setSelected(true);

		objectFragmentButton = new JRadioButton("Object Fragment Assessment");
		objectFragmentButton.setToolTipText(
				"Allows the Object Manager to"+
				"assess all object fragments within the image. Used to assess "+
				"images in which whole objects cannot be fully reconstructed."
				);

		//Group the radio buttons.
		assessmentModeButtonGroup = new ButtonGroup();
		assessmentModeButtonGroup.add(wholeObjectButton);
		assessmentModeButtonGroup.add(objectFragmentButton);
		
		assessmentModePanel.add( new JLabel("Object Assessment Mode:") );
		assessmentModePanel.add(wholeObjectButton);
		assessmentModePanel.add(objectFragmentButton);
		
		return assessmentModePanel;
	}
	
	/**
	 * Returns a JPanel with Radio Buttons to select Object Connectivity.  This will decide:
	 * <p>
	 * - Whether the objects in the image are connected by 6, 18, or 26 connectedness
	 * <p>
	 * - This will determine how objects are identified in the image.
	 * 
	 * @return
	 */
	public JPanel addObjectConnectivitySelector() {
		
		JPanel objectConnectivityPanel = new JPanel();
	
		//Create the radio buttons.
		connected6Button = new JRadioButton("6-connected");
		connected6Button.setToolTipText(
				"Objects are connected using the 6-connected rule."
				);
		connected6Button.setSelected(true);

		connected18Button = new JRadioButton("18-connected");
		connected18Button.setToolTipText(
				"Objects are connected using the 18-connected rule."
				);
		
		connected26Button = new JRadioButton("26-connected");
		connected26Button.setToolTipText(
				"Objects are connected using the 26-connected rule."
				);

		//Group the radio buttons.
		objectConnectivityButtonGroup = new ButtonGroup();
		objectConnectivityButtonGroup.add(connected6Button);
		objectConnectivityButtonGroup.add(connected18Button);
		objectConnectivityButtonGroup.add(connected26Button);
		
		objectConnectivityPanel.add( new JLabel("Object Connectivity:") );
		objectConnectivityPanel.add(connected6Button);
		objectConnectivityPanel.add(connected18Button);
		objectConnectivityPanel.add(connected26Button);
		
		return objectConnectivityPanel;
	}

	/**
	 * Returns a JPanel which contains the processAllCheckBox checkbox. The processAllCheckBox is
	 * initialised and its tool tip set.
	 * @return
	 */
	public JPanel addProcessAllCheckBox() {

		// processAllCheckBox = new JCheckBox("Process All Images Initially:");

		//processAllCheckBox.setToolTipText("Check ON for all images to be opened and processed initially. "+
			//	"Will ensure max dimensions are populated correctly.");

		JPanel processAllCheckBoxPanel = new JPanel();

		// processAllCheckBoxPanel.add(processAllCheckBox);

		return processAllCheckBoxPanel;

	}


	/**
	 * Set some initial variables - the firstPixel variable, and the objSelected & objSelectedPixVal variables. Setup
	 * the colours for feature, nonfeature, connected, selected, blank obj colours (based on 16 colors LUT).
	 */
	@Override
	public void setup() {

		// This is not needed, as all images will be Forced to be processed initially :
			// This ensures the maxXYZ dimensions data is filled correctly!
		//if(processAllCheckBox.isSelected() == false) {
			//dw.setMethodCallNumber(1); //sets the loop to only call the process() method only ONCE.
			//Can open other images when necessary & by user selection...
		//}

		IJ.showStatus("Object Manager:");
		
		IJ.log("");
		IJ.log("###########################");
		IJ.log("    Object Manager:");
		IJ.log("###########################");
		IJ.log("");
		IJ.log("");
		
		IJ.log("    Gathering Settings...");

		// build the DOM of StereoMateSettings.xml for retrieval of options:
		docSMS = StereoMateSettings.buildDomStereoMateSettings();

		// ... and retrieve the settings from docSMS:
		retrieveOptionsFromDocSMS();
		
		IJ.log("");
		
		IJ.log("    Building Objects...");
		

		// make the OM_MetaData Directory:
		OM_MetaData = new File(dw.getOutputParentFile().getAbsolutePath() 
				+ File.separator + "OM_MetaData");
		OM_MetaData.mkdir();

		// initialise the manClassStats obj:
		manClassStats = new ClassificationStats();

		// initialise the SelectedObject variable - 
		// to default X Y Z values, objSelected bool, objSelectedPixVal int:
		selectedObj = new SelectedObject(0, 0, 0, false, 0, 0);

		// Make an ObjectIdentifier object:
		String[][] objIdentifierInput = new String[][] {
			ObjectDataContainer.MANUALCLASSTITLESVALS,
			ObjectDataContainer.FILTERCLASSTITLESVALS,
			ObjectDataContainer.CLASSIFIERCLASSTITLESVALS,
		};
		
		objIdentifier = new ObjectIdentifier(ObjectDataContainer.BASE_VALUE, objIdentifierInput);

		
		// Make a ClassificationModeLUTs object:
		Color[][] colours = new Color[][] {
			{Color.WHITE, Color.GREEN, Color.RED, Color.BLUE },
			{Color.WHITE, Color.RED },
			{Color.GREEN, Color.RED, Color.BLUE }
		};

		Color[] coloursSelected = new Color[] 
				{ Color.PINK, Color.GREEN, Color.RED, Color.BLUE };

		classModeLUTs = new ClassificationModeLUTs(colours, objIdentifier, coloursSelected );
		
		

		//Set classifierAttributes boolean array - all refs which are TRUE are attributes which are kept:
		//  setClassifierAttributesRemoval();
		setAttributesOnInstances();

		// IJ.showMessage("arff numAttributes @ setAttrributesOnInstances: "+arff.numAttributes() );

		//Set CLASSIFIER to "NaiveBayes":
		// Other options would be "J48", "IBk"...
		// CLASSIFIER = "NaiveBayes";
		// CLASSIFIER = "IBk";
		// NOW set in retrieveOptionsFromDocSMS();

		// initialise the OM_ProcedureSettings object - for storing OM Procedure Settings (maxX,Y,Z, Filter settings)
		om_ProcedureSettings = new OM_ProcedureSettings();
		
		// and set the Object Assessment Mode - selected on DialogWindow:
		om_ProcedureSettings.setObjectAssessmentMode( getObjectAssessmentMode() );
		
		// and set the Object Connectivity - selected on DialogWindow:
		om_ProcedureSettings.setObjectConnectivity( getObjectConnectivity() );

		maxDimensions = new MaximumDimensions(om_ProcedureSettings);


		// Generate the Output Directory to hold the Object Manager Output:

		// While the output Directory from DialogWindow will hold all object data for every image 
		// (partly for efficiency reasons [do not have to run object analysis when 
		// image is opened again], and partly for keeping track of data), 

		// INSIDE the DW Output Directory will be the OM_MetaData which will hold all data relevant 
		// to actually using the Object Manager in the SM Analyser:
		// 1. Threshold Procedure Stack - for applying thresholding to images.
		// 2. Max Object Dimensions [mean of object dimensions 95-100% in X Y Z] - for setting exclusion
		// zones.
		// 3. Attribute Filter Settings [including Attribute, low & high pass values] - for applying 
		// the Attribute Filter.
		// 4. Serialised Weka Classifier - for applying optimised ML Classifier.

		// NOTE:  Will also be saving the ManualClassificationData.arff file to output - 
		// this contains all of the currently manually assessed objects.
		// this will be saved to OM_MetaData.

		// Put the ThresholdProcedureStack XML file into OM_MetaData:
		// Prefixed the procedureStack title with PS_ to allow it to be recognised in SM Analyser algorithm:
		// removed the ".xml" extension from the procedureStackTitle too..
		procedureStack.saveStack("PS_"+procedureStackTitle.substring(0, procedureStackTitle.length()-4), OM_MetaData);

		// Will put the XML holding object max dimensions and Filter settings once first image has been opened
		// and assessed..
		
		// create an objectFilter:
			// Use a ObjectFilter object to apply the logic of the filtering:
			// objectFilter = new ObjectFilter(objectDataset, imageHandler, objIdentifier); move ARGS to the methods
									// like ObjectClassifier!
		objectFilter = new ObjectFilter(objIdentifier);

		// create an objectClassifier:
		objectClassifier = new ObjectClassifier( ObjectDataContainer.FEATURE_INDEX );

	}


	/**
	 * Retrieves all options from the StereoMateSettings.xml elements, and puts them into the correct instance
	 * variables.
	 */
	public void retrieveOptionsFromDocSMS() {

		docSMS.getDocumentElement().normalize();
		////IJ.showMessage("Root Element: "+ doc.getDocumentElement().getNodeName() );

		NodeList nList2;


		nList2 = docSMS.getElementsByTagName("gaussianObj");
		gaussianObjStr = ((Element)nList2.item(0)).getAttribute("str");

		nList2 = docSMS.getElementsByTagName("linearObj");
		linearObjStr = ((Element)nList2.item(0)).getAttribute("str");

		nList2 = docSMS.getElementsByTagName("maxObjSel");
		maxObjSelStr = ((Element)nList2.item(0)).getAttribute("str");

		nList2 = docSMS.getElementsByTagName("roughClassifier");
		roughClassifierStr = ((Element)nList2.item(0)).getAttribute("str");

		nList2 = docSMS.getElementsByTagName("sliceProjection");
		sliceProjectionStr = ((Element)nList2.item(0)).getAttribute("str");

		biasedSelectionString = gaussianObjStr;

		linearObjectString = getLinearObjString(linearObjStr);

		MAX_OBJ_SEL = Integer.parseInt(maxObjSelStr);

		CLASSIFIER = roughClassifierStr;

		sliceProjectionArray = getSliceProjArray(sliceProjectionStr);

	}


	public String getLinearObjString(String linearObjStr) {
		String l = "";
		int stringLength = Integer.parseInt(linearObjStr);
		for(int a=0; a<stringLength; a++) {
			l = l + "1,";
		}
		//remove last comma!
		l = l.substring(0, l.length()-1);
		return l;
	}


	public int[] getSliceProjArray(String sliceProjectionStr) {
		int firstInt = Integer.parseInt(  sliceProjectionStr.substring(0, sliceProjectionStr.indexOf("-") )  );
		int secondInt = Integer.parseInt(  sliceProjectionStr.substring( sliceProjectionStr.indexOf("-")+1 )  );

		return new int[] {firstInt, secondInt};

	}
	
	/**
	 * Returns the Object Assessment Mode selected on the Dialog Window - either Whole Object or
	 * Object Fragment.  The two return values are stored as named constant Strings in the
	 * OM_ProcedureSettings class.
	 * @return
	 */
	public String getObjectAssessmentMode() {
		if(wholeObjectButton.isSelected() == true) {
			return OM_ProcedureSettings.WHOLEOBJECT;
		}
		else {
			return OM_ProcedureSettings.OBJECTFRAGMENT;
		}
	}

	/**
	 * Returns the Object Connectivity selected on the Dialog Window - either Connected 6, 18
	 * or 26.  The return values are stored as named constant Strings in the
	 * OM_ProcedureSettings class.
	 * @return
	 */
	public String getObjectConnectivity() {
		if(connected6Button.isSelected() == true) {
			return OM_ProcedureSettings.CONNECTED6;
		}
		else if(connected18Button.isSelected() == true) {
			return OM_ProcedureSettings.CONNECTED18;
		}
		else {
			return OM_ProcedureSettings.CONNECTED26;
		}
	}

	/**
	 * Applies the procedureStack to the passed imp, and stores the reference to this new Imp into thresholdImp (this
	 * new imp has an extra channel which contains the thresholded channel).
	 * <p>
	 * The IWP is made from the original & thresholded imp, which is converted to correct bitDepth (as stated in 
	 * procedureStack), and the extra channel is displayed.  
	 * <p>
	 * The last channel is set as active channel in IWP to allow user to interact with thresholded objects in this 
	 * channel.
	 * <p>
	 * An appropriate LUT is set -> 16 colors LUT.  UNCLASSIFIED, SELECTED, CLASSIFIED_FEATURE and CLASSIFIED_NONFEATURE
	 * named constants give object pixel values of correct colours.
	 * <p>
	 * Finally, the Object Manager is set up - which adds components and their behaviours to the processing panel
	 * on the IWP.
	 * 
	 */
	@Override
	public void process(ImagePlus imp) {

		// store the passed imp into original imp - to allow the IWP to be constructed in cleanup() method:
		originalImp = imp; // ???  Just use imageHandler?!
		
		IJ.log("");
		IJ.log("");
		IJ.log("Assessing Image: "+imp.getTitle() );
		
		IJ.showStatus("Object Manager: Applying Threshold"); 
		
		IJ.log("");
		IJ.log("    Applying Threshold Procedure Stack...");
		
		thresholdPath = dw.getCurrentOutputFile().getAbsolutePath() +".tif";
		
		File thresholdFile = new File(thresholdPath);
		
		// get the output file name for the arff data table:
		arffPath = dw.getCurrentOutputFile().getAbsolutePath() +".arff";
		
		File arffFile = new File(arffPath);
		
		if(thresholdFile.exists() && arffFile.exists() ) {
			// do NOTHING - as the thresholded image and ARFF Path already exist!
		}
		else { // else either threshold Image or arff File need to be created potentially, so run through those methods:

			imageHandler = new ImageHandler(imp, procedureStack, 
					om_ProcedureSettings.getObjectConnectivity(), thresholdPath );

			if( saveThresholdCheckBox.isSelected() == true ) {
				//thresholdPath = dw.getCurrentOutputFile().getAbsolutePath() +"tif";
				imageHandler.saveThresholdImp( dw.getCurrentOutputFile() );
			}

			// get output file name for PNG saving - 
			// do not include .png extension, this is done in the save image listener:
			pngPath = dw.getCurrentOutputFile().getAbsolutePath();

			// And initialise the png index (for adding to image file name for multi-saving of PNG files):
			pngIndex = 0;

			manClassStats = new ClassificationStats();

			IJ.showStatus("Object Manager: Assessing Objects");

			IJ.log("");
			IJ.log("    Assessing Objects...");

			assessObjects(arffPath);

		}


	}  // end process(imp)

	/**
	 * This method assesses all objects inside the imp, using the MCIB3D library.  No ROIs are being applied here,
	 * but all objects which are touching the image edge are removed.  
	 * <p>
	 * Objects are assessed for a range of attributes:  Location, Size, Shape parameters, Intensity.  
	 * Objects are also identified by their FIRST VOXEL - this is the voxel which is first encountered in the first 
	 * Z plane the object is in.  This is used to move between the object in the image and the object in the generated 
	 * data table.
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
	 * WEKA!  NaiveBayes will do a good rough job.  But, to apply this classifier to the data need to use ARFF data 
	 * format!
	 * <p>
	 * - Finally, WEKA has good pre-made classes for visualising datasets from ARFF files.  By using the ARFF file 
	 * format, I can take advantage of these classes for the user to visualise the objects in a graph, colour coded 
	 * by class, and which allow the user to select any attribute to put on X or Y axis.
	 * 
	 * @param thresholdImp The image which contains the series of thresholded objects to be assessed.
	 */
	public void assessObjects(String arffPath) {

		// NOTE -> arff and other arff data tables are now made in the method: 
		// as dataHandler object!
		// setAttributesOnInstances(); -> performed in the setup() method at start of plugin.

		//generate a file object from the arffPath:
		File arffFile = new File(arffPath);

		// Remove all objects which reside on the image border:
			// only if the wholeObjectButton - Whole Object Assessment - is selected:
		imageHandler.removeEdgeObjects( wholeObjectButton.isSelected() );

		// if the arffFile does NOT exist, need to collect the data on each object in thresholdImp:
		if(arffFile.exists() == false) {

			// Extract object data from image -> involves imageHandler and datasetHandler:
			extractObjData();

			// determine xMax, yMax & zMax from xDimArray, yDimArray & zDimArray
			// maxDimensions.calculateDimMax(xDimArray, yDimArray, zDimArray);
			maxDimensions.calculateDimMax(	objectDataset.getValues( ObjectDataContainer.XLENGTH ), 
											objectDataset.getValues( ObjectDataContainer.YLENGTH ), 
											objectDataset.getValues( ObjectDataContainer.ZLENGTH )  );
			
			om_ProcedureSettings.setMaxXYZ(maxDimensions.getMaxX(), maxDimensions.getMaxY(), maxDimensions.getMaxZ());

		} //end if arffFile.exists == false

		else if(arffFile.exists() == true) {

			// load data - only contains up to manClass - there is No Filter or Classifier Attributes:
			objectDataset.loadData( arffFile, ObjectDataContainer.returnObjectMeasuresAndManClassAttributes() );

			// Add the Filter and Classifier Attributes:
			objectDataset.addAttributeToInstances(ObjectDataContainer.returnFilterClassAttribute());
			objectDataset.setAttributeValueOnInstances(ObjectDataContainer.FILTERCLASS, 0.0);

			objectDataset.addAttributeToInstances(ObjectDataContainer.returnClassifierClassAttribute());
			objectDataset.setAttributeValueOnInstances(ObjectDataContainer.CLASSIFIERCLASS, 1.0);

			// fill each object in imageHandler from 255 to values as indicated in datasetHandler:
			setObjectPixelValues();
			
			// determine xMax, yMax & zMax from xDimArray, yDimArray & zDimArray
			// maxDimensions.calculateDimMax(xDimArray, yDimArray, zDimArray);
			maxDimensions.calculateDimMax(	objectDataset.getValues( ObjectDataContainer.XLENGTH ), 
											objectDataset.getValues( ObjectDataContainer.YLENGTH ), 
											objectDataset.getValues( ObjectDataContainer.ZLENGTH )  );
			
			om_ProcedureSettings.setMaxXYZ(maxDimensions.getMaxX(), maxDimensions.getMaxY(), maxDimensions.getMaxZ());

		} //end if arffFile exists()

		// Compact the arff data table:
		objectDataset.compactify();

		// build the firstPixObjNoMap:
		buildFirstPixObjNoMap(objectDataset);

		//set the total number of objects in manClassStats:
		manClassStats.total = objectDataset.data.numInstances();

		// And finally, set the arff class attribute to the Manual Class attribute: 
		objectDataset.setClassAttribute(ObjectDataContainer.MANUALCLASS);

	}//end assessObjects(ImagePlus)

	/**
	 * Extracts the data for all objects in the imageHandler thresholdImgInt.
	 */
	public void extractObjData() {

		// make sure the arff data object is EMPTY:
		objectDataset.delete();

		IJ.showStatus("Object Manager: Assessing Objects - gathering data");

		// setup loop through thresholdImgInt in imageHandler:
		imageHandler.setupLoopThresholdImgInt(0, 0, 0, 0);

		// using a while loop, extract all data from each object in thresholdImgInt:
		while(true) {
			dataObj = imageHandler.assessThresholdImgIntObj(
											
								255, 
											// MuFpCf
								objIdentifier.returnFlagValue( new String[] 
											{	ObjectDataContainer.UNCLASSIFIEDATR,
												ObjectDataContainer.PASSEDATR,
												ObjectDataContainer.FEATUREATR	}
										),
								false // for now, do not collect convex measures data
					); // returns null when end of image reached.
			
			if(dataObj == null) {
				break;
			}
			//Put the data from dataObj into the arff data structure:
			objectDataset.addData( dataObj.returnData(), dataObj.returnDataTitles() );
			
			IJ.showStatus("Object Manager: Assessing Objects - gathering data");
		}
		
		// need to set Classifier Attribute value across the whole datset to FEATURE now:
			// By default it will be UNCLASSIFIED, but this is not valid/correct (the pixel value is set to FEATURE,
				// and UNCLASSIFIED is really not defined for the Classifier Attribute):
		objectDataset.setAttributeValueOnInstances(
				ObjectDataContainer.CLASSIFIERCLASS, 
				(double)ObjectDataContainer.getValueIndex(
						ObjectDataContainer.CLASSIFIERCLASS, 
						ObjectDataContainer.FEATUREATR)
				);

		// showArffDatatable(arff);
		// IJ.showMessage("New Arff Data Table for New Image.");

		// save the images' ARFF Dataset - MINUS the Filter and Classifier Columns:
		objectDataset.saveData(arffPath, 
				ObjectDataContainer.returnObjectMeasuresAndManClassAttributes() ); //this method uses the arffPath to save data to correct output.

	}

	/**
	 * Sets the pixel values in imageHandler of each object found in datasetHandler to the correct value 
	 * based on the values in Manual, Filter and Classifier Class Attributes in datasetHandler.  The pixel 
	 * value is determined from these values with the objIdentifier object.  The pixel value is set on the 
	 * imageHandler with the setPixelValue() method.
	 */
	public void setObjectPixelValues() {

		for(int a=0; a<objectDataset.numInstances(); a++) {
			int flagVal = objIdentifier.returnFlagValue( new String[] { 
				ObjectDataContainer.MANUALCLASSVALS[(int)objectDataset.get(    a, ObjectDataContainer.MANUALCLASS)],
				ObjectDataContainer.FILTERCLASSVALS[(int)objectDataset.get(	   a, ObjectDataContainer.FILTERCLASS)],
				ObjectDataContainer.CLASSIFIERCLASSVALS[(int)objectDataset.get(a, ObjectDataContainer.CLASSIFIERCLASS)]
			} );
			int x = (int)objectDataset.get(a, ObjectDataContainer.X1);
			int y = (int)objectDataset.get(a, ObjectDataContainer.Y1);
			int z = (int)objectDataset.get(a, ObjectDataContainer.Z1);
			
			//IJ.showMessage("obj index: "+a+" flag val: "+flagVal);

			imageHandler.setObjValue(x, y, z, flagVal);

		}

	}

	/**
	 * Creates a new firstPixObjNoMap object using the X1 Y1 and Z1 Attributes for first pixel coordinates,
	 * and the objNo Attribute as the value to lookup.  The firstPixObjNoMap is then built using the
	 * passed ObjectDatasetHAndler object.
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
	 * Have hacked the process() and cleanup() methods such that all images passed through process() will be initially
	 * assessed, and only when the DialogWindow currentFileIndex and totalFileCount are equal will cleanup() run to
	 * display the last processed image in IWP with all the panel controls.
	 * <p>
	 * If dw.setMethodCallNumber(num) is set, only the first images up to num will be processed - this is set to 1
	 * if the processAllCheckBox is set to FALSE on the DialogWindow, and therefore only the first image is processed
	 * and then displayed.
	 */
	@Override
	public void cleanup() {

		// This is called at the end of the first pass through process() - need to adjust the DialogWindow
		// processImages() method to ensure the setup() process(imp) cleanup() structure is used for both
		// plugins which pass linearly through all images, and for plugins which move forward or backward
		// through input images for processing in a non-linear fashion.

		// TODO Need to re-code this structure to ensure the programmer retains flexibility with how to process
		// images.
		// Typically - will want an image window open which can move backwards and forwards through all images
		// and saving appropriate outputs will be the task of the plugin itself (whether a single object
		// for all images, an object or more per image, or further images, or combinations of these).
		
		// USE DIFFERENT INTERFACES -> One for non-linear movement between images, one to linearly go through all images.

		// IJ.showMessage("Cleanup run");
		// removeListeners();
		
		// FIRST -> LOAD THE FIRST IMAGE:
		
		// then, just retrieve the currentImp on fileselector 0:
		// This is SLOW for large images, or images retrieved from a server or other slow access place
			// VIRTUAL STACKS?!
		dw.incrementTotalFileCountAndCurrentFileIndex(); // need this to RESET current and total file counts!
		originalImp = dw.getCurrentImp(0);
		thresholdPath = dw.getCurrentOutputFile().getAbsolutePath() +".tif";

		//generate new IWP & activeImp:
		
		IJ.log("");
		IJ.log("");
		IJ.log("Loading Image: "+originalImp.getTitle() );


		imageHandler = new ImageHandler(originalImp, procedureStack, om_ProcedureSettings.getObjectConnectivity(), 
													thresholdPath );

		// generate the new thresholdImp from the new imp:
		// thresholdImp = applyThresholdProcedureStack(imp, procedureStack);

		IJ.showStatus("Object Manager: Loading Objects");

		//String arffPath = dw.getCurrentOutputFileNoPause().getAbsolutePath() +".arff";
		// Do not use NoPause as now the currentoutputfile index is in sync as if the process() method
		// were paused!
		arffPath = dw.getCurrentOutputFile().getAbsolutePath() +".arff";
		
		// Set the index on the Manual Classification arff buffer:
		//manClassArffBuffer.setBufferIndex( dw.getCurrentFileIndex() );
		manClassArffBuffer.setBufferString( dw.getCurrentFile(0).getName().substring(0, 
															dw.getCurrentFile(0).getName().lastIndexOf(".")) );

		//IJ.showMessage("arff path new image: "+arffPath);

		// initialise the manClassStats obj:
		manClassStats = new ClassificationStats();

		assessObjects(arffPath);

		// Then set up the IWP - set THRESHOLDED IMP as the activeChannel at end:
		// Channel projector must be set up for this image, as there must be min of 2 channels
		// BUT must HIDE the last channel control, and setVisible(false) for it so it can be controlled
		// by this Plugin!

		IJ.showStatus("Object Manager: Constructing IWP");
		
		IJ.log("");
		IJ.log("    Setting up IWP...");

		IWP = new ImageWindowWithPanel(originalImp, new Panel(), imageHandler.thresholdImp, procedureStack.bitDepth, false );
		// Passed FALSE at the end to ensure the new channel is displayed and toggle is available on IWP.
		// Adjust this so the last imp is only turned ON by a method called in IWP -> want to turn the channel
		// on after having set the LUT on it (which is done below!).

		// Note, this can be controlled by manipulating the channelCheckBox array, 'channels'.
		// Specifically, calling channels[index].stateChanged() will switch between displaying and not displaying
		// the channel.  Or the setState(boolean) method can be used to modify state and checkbox together.
		// ALSO -> bitDepth indicates the image will be converted to bitDepth at point of opening in IWP.
		// This may be 8, 16, 32 -> set by user in DialogWindow.



		// Set active channel to last channel - the one NOT DISPLAYED & which will contain the processed imp:
		// This just means pixel data is being manipulated on the last channel - a moot point.. Although later when
		// user make selections on the image, will want the active channel to be the thresholded channel...
		// For now required so the setIwpLut() method acts on the thresholded (last) channel.
		IWP.setActiveChannel( IWP.getChannels() );

		//set projection to be from and to slices indicated in sliceProjectionArray
		// Note: This method checks to ensure the slices are legal (i.e. within range), and if not it will set the
		// projection to be the largest width it can:	
		IWP.setSliceProjection( sliceProjectionArray );


		// Make the LUT of thresholdImp the manualLut by default:
		setIwpLut( classModeLUTs.manualLut );

		// Setting the close operation to include saving the current Arff Dataset:
		IWP.iw.addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e); // retain any code which is called before.
				//saveArffDataAfterProcessMethod(); // and save the current ARFF Dataset to relevant location.
				objectDataset.saveData(arffPath, ObjectDataContainer.returnObjectMeasuresAndManClassAttributes() ); //this method uses the arffPath to save data to correct output.
				saveManualClassificationData(); // save the manual classification data too!
				
				removeListeners();
				IWP.iw.removeWindowListener( this );
				
				shutdownOM();
				
				shutdownDW();
				
				shutdownIWP();
				
			}
		});
		
		//IJ.showMessage(""+dw.getCurrentFileIndex()+" / "+dw.getTotalFileCount()+" - "+dw.getCurrentFile(0).getName()+" : "+dw.getCurrentFileRelativeToInput(0).getParent() );
		// Set the IWP window title:  [ImgIndex] / [TOTAL INDEX] - [image_title] : [Path_Below_Input_Dir]
		IWP.getImagePlus().setTitle(""+dw.getCurrentFileIndex()+" / "
									+dw.fileSelectors.get(0).inputOutputFramework.fileArray.size()+" - "
									+dw.getCurrentFile(0).getName()+" : "
									+dw.getCurrentFileRelativeToInput(0).getParent() );
		IWP.getOriginalImagePlus().setTitle(""+dw.getCurrentFileIndex()+" / "
									+dw.fileSelectors.get(0).inputOutputFramework.fileArray.size()+" - "
									+dw.getCurrentFile(0).getName()+" : "
									+dw.getCurrentFileRelativeToInput(0).getParent() );
		
		// get output file name for PNG saving - 
		// do not include .png extension, this is done in the save image listener:
		pngPath = dw.getCurrentOutputFile().getAbsolutePath();

		// And initialise the png index (for adding to image file name for multi-saving of PNG files):
		pngIndex = 0;
		
		// Setup the object manager -> all buttons and components + listeners on the IWP panel for Threshold setup:
		setupObjectManager();

		// save the images' ARFF Dataset - MINUS the Filter and Classifier Columns:
		//saveArffDataDuringProcessMethod();
		// arff.saveData(arffPath, classifierAttributesNoFilterNoClassifier); //this method uses the arffPath to save data to correct output.

		// save the OM_ProcedureSettings file - max dimensions and Filter settings:
		saveOM_ProcedureSettingsFile();

	}



	/**
	 * Sets the displayed (projected) ImagePlus from the IWP object & its original ImagePlus (the
	 * original Z stack) from the IWP to the passed Look-Up Table.  In both instances, the active channel
	 * of the ImagePlus (if its a hyperstack) is the channel which LUT is changed.
	 * <p>
	 * This is assumed to be the last channel, and so the original IWP ImagePlus (original Z stack) channel
	 * is changed to the last before changing the LUT, and then its active channel is set back to the
	 * first channel.
	 * 
	 * @param lut The LUT to change to.
	 */
	public void setIwpLut(LUT lut) {

		setLut(lut, IWP.getImagePlus() ); // this is not really necessary as IWP imp is NULL at this point!

		// to set original imps last channel to GREYS LUT - need to set its channel to the last channel:
		IWP.getOriginalImagePlus().setC( IWP.getChannels() );
		setLut(lut, IWP.getOriginalImagePlus() );

		//IJ.run(IWP.getOriginalImagePlus(), "16 colors", "");
		// re-set the original imp channel to the first channel:

		IWP.getOriginalImagePlus().setC( 1 );
	}

	/**
	 * 
	 * @param lut
	 * @param imp
	 */
	public void setLut(LUT lut, ImagePlus imp) {
		ImageProcessor ip = imp.getChannelProcessor();
		IndexColorModel cm = lut;
		if (imp.isComposite())
			((CompositeImage)imp).setChannelColorModel(cm);
		else
			ip.setColorModel(cm);
		if (imp.getStackSize()>1)
			imp.getStack().setColorModel(cm);
		//imp.updateAndRepaintWindow();
	}


	/**
	 * Sets up the panel components and their behaviours for implementing the object manager:
	 * <p>
	 * - 1. User can SELECT & CLASSIFY thresholded objects in the image.
	 * <p>
	 * - 2. User can view a graph & table of the objects - ARFF file: & Export classified ARFF file to filesystem 
	 * -> for WEKA
	 * <p>
	 * - 3. ML & USER Directed object annotation - delineating classification boundary:  Select based on object
	 * classification probabilities, select based on object attribute values.
	 * <p>
	 * - 4. User can select low / high pass filter values (size, shape, other attribute):
	 * <p>
	 * - 5. User can check filter performance on other images in input: 
	 * <p>
	 * - 6. Import WEKA-trained classifiers:
	 * <p>
	 * - 7. Applying Filter / Classifier to remaining images in input:
	 * <p>
	 * - 8. Export the Object Filter -> including max/min obj size, Any Obj Filters, Any Obj Classifiers:
	 * <p>
	 *    X - 9. Export Obj Attributes & Classification Data -> CSV or ARFF format (for plotting data):
	 * <p>
	 * - 10. Save Images -> Raw, Thresholded, Classifier Objects:
	 * 
	 */
	public void setupObjectManager() {

		IJ.showStatus("Object Manager: Setup Obj. Manager");

		// Add StereoMate Header to Log Table:  DO THIS EARLIER!
		//StereoMateUtilities.stereomateTitleToLog();

		// Generate the parent panel - which will hold all components for this algorithm:
		objManagerPanel = new JPanel();

		// set this to boxLayout & LINE_AXIS -> To allow use of JSeparators!
		objManagerPanel.setLayout( new BoxLayout(objManagerPanel, BoxLayout.LINE_AXIS) );

		// put this Panel into a scrollPane to make it scroll on smaller screens if all components are not visible:
		JScrollPane scrollPane = new JScrollPane(objManagerPanel);

		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

		scrollPane.setBorder( BorderFactory.createEmptyBorder(0,0,6,0) );


		// MANUAL CLASSIFICATION OF OBJECTS -> Components & Behaviours:


		// 1. User can SELECT & CLASSIFY objects in the image:


		// SELECTING OBJECTS:

		// SETUP: MouseListener on Image:

		// Need to add a listener to the image canvas to detect clicks on the image:

		IWP.getCanvas().addMouseListener(this);

		// Behaviour of mouse clicks on the image implemented in the MousePressed() method:
		// This allows users to SELECT an object.
		// This object is highlighted, and its FIRST PIXEL coordinate is stored in variable firstPix.


		// MANUALLY CLASSIFYING OBJECTS:

		// SETUP: Classification Buttons and Behaviours

		// need to allow user to manually CLASSIFY the objects:

		// Use selection of buttons to annotate the currently selected object.

		//ANNOTATION:

		// 1. UNCLASSIFIED [S]

		// CLASSIFIED:

		// 2. FEATURE OBJECT [F]

		// 3. NON-FEATURE OBJECT [D]

		// 4. CONNECTED OBJECT [C] (would be a feature object, but it has been corrupted by being joined to
		// another [feature or non-feature] object).
		// These must be MINIMISED to prevent corruption of the data!
		// As its impossible to deal with a population of these objects in general terms

		//LAYOUT MANAGERS - provide vertical column layouts - NOW USING GridBagLayout!
		//panel.setLayout( new GridLayout(0,1));
		//panel.setLayout( new BoxLayout(activeChannelPanel, BoxLayout.PAGE_AXIS) );

		// Provide:
		// Panel for Buttons
		// 4 JButtons - Unclassified, Feature, Non-Feature, Connected

		classifyPanel = new JPanel();

		classifyPanel.setLayout( new GridBagLayout() );

		GridBagConstraints gbc = new GridBagConstraints();

		featureButton 		= 	new JButton( createImageIcon("/Icons/Feature 100x100.png", "Feature Button", 30, 30) );
		nonFeatureButton 	= 	new JButton( createImageIcon("/Icons/NonFeature 100x100.png", "Non-Feature Button", 30, 30) );
		connectedButton 	= 	new JButton( createImageIcon("/Icons/Connected 100x100.png", "Connected Button", 30, 30) );
		unclassifiedButton 	= 	new JButton( createImageIcon("/Icons/Unclassified 100x100.png", "Unclassified Button", 30, 30) );

		//featureButton 		= 	new JButton("Feat. [F]");
		//nonFeatureButton 	= 	new JButton("Non-Feat. [D]");
		//connectedButton 	= 	new JButton("Conn. [C]");
		//unclassifiedButton 	= 	new JButton("Unclass. [S]");


		featureButton.setToolTipText("Classify selected object as Feature Object");
		nonFeatureButton.setToolTipText("Classify selected object as Non-Feature Object");
		connectedButton.setToolTipText("Classify selected object as Connected Object");
		unclassifiedButton.setToolTipText("Classify selected object as Unclassified Object");

		//featureButton.setBorder( BorderFactory.createRaisedBevelBorder() );
		//nonFeatureButton.setBorder( BorderFactory.createRaisedBevelBorder() );
		//connectedButton.setBorder( BorderFactory.createRaisedBevelBorder() );
		//unclassifiedButton.setBorder( BorderFactory.createRaisedBevelBorder() );

		// Add buttons to Panel:
		// set fill and weights for all:
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 0.5;
		gbc.weighty = 0.5;

		gbc.gridx = 0;
		gbc.gridy = 0;
		classifyPanel.add(featureButton, gbc);
		gbc.gridx = 0;
		gbc.gridy = 1;
		classifyPanel.add(nonFeatureButton, gbc);
		gbc.gridx = 1;
		gbc.gridy = 0;
		classifyPanel.add(connectedButton, gbc);
		gbc.gridx = 1;
		gbc.gridy = 1;
		classifyPanel.add(unclassifiedButton, gbc);

		// Add panel to Obj Manager Panel:
		objManagerPanel.add(classifyPanel);

		// Classification Keyboard Shortcuts: Add KeyListener from this class onto IWP:
		IWP.addKeyListener(this);

		// Each Button needs to adjust the selected object's classification status in arff dataset, and change
		// the object's SELECTED & UNSELECTED PIXEL VALUE.

		// ALSO -> Add Keyboard Shortcuts to these classifications

		featureButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				// setManFeatureObj();
				// setObjManClass(CLASSIFIED_FEATURE, CLASSIFIED_FEATURE_SELECTED, FEATUREATR);
				setObjManClass(ObjectDataContainer.FEATUREATR, SelectedObject.FEATURE);

			}

		});

		nonFeatureButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				// setManNonFeatureObj();
				// setObjManClass(CLASSIFIED_NONFEATURE, CLASSIFIED_NONFEATURE_SELECTED, NONFEATUREATR);
				setObjManClass(ObjectDataContainer.NONFEATUREATR, SelectedObject.NONFEATURE);

			}

		});

		connectedButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				// setManConnectedObj();
				// setObjManClass(CLASSIFIED_CONNECTED, CLASSIFIED_CONNECTED_SELECTED, CONNECTEDATR);
				setObjManClass(ObjectDataContainer.CONNECTEDATR, SelectedObject.CONNECTED);

			}

		});

		unclassifiedButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				// setManUnclassifiedObj();
				// setObjManClass(UNCLASSIFIED, UNCLASSIFIED_SELECTED, UNCLASSIFIEDATR);
				setObjManClass(ObjectDataContainer.UNCLASSIFIEDATR, SelectedObject.UNCLASSIFIED);

			}

		});


		// Add View Panel and components - to allow user to modify the object view:


		viewPanel = new JPanel();

		viewPanel.setLayout( new GridBagLayout() );

		gbc = new GridBagConstraints();

		viewLabel = new JLabel("View:");

		//centre align:
		viewLabel.setHorizontalAlignment(JLabel.CENTER);

		// make BOLD:
		viewLabel.setFont(viewLabel.getFont().deriveFont(viewLabel.getFont().getStyle() | Font.BOLD));

		//JCheckBox filterViewCheckBox = new JCheckBox("Filter [I]");

		//filterViewCheckBox.setToolTipText("Check ON to activate Filter, check OFF to de-activate filter");

		//JCheckBox classifierViewCheckBox = new JCheckBox("Classifier [L]");

		//classifierViewCheckBox.setToolTipText("Check ON to activate Filter, check OFF to de-activate filter");

		//JCheckBox manualViewCheckBox = new JCheckBox("Manual [M]");

		//manualViewCheckBox.setToolTipText("Check ON to activate Filter, check OFF to de-activate filter");

		// TODO Move to using a ComboBox for different views, and allow user to make their own views in options?
		// There is potential for many more views to be created with the current array of object classifications.
		// This should be taken advantage of by supplying a series of views which highlight different objects when
		// cross-tabulating across Manual Filter and Classifier Classification Types.

		//Create the radio buttons.
		filterViewButton = new JRadioButton("Filter [L]");
		filterViewButton.setEnabled(false);

		classifierViewButton = new JRadioButton("Classifier [K]");
		classifierViewButton.setEnabled(false);

		manualViewButton = new JRadioButton("Manual [M]");
		manualViewButton.setSelected(true);


		//Group the radio buttons.
		viewButtonGroup = new ButtonGroup();
		viewButtonGroup.add(filterViewButton);
		viewButtonGroup.add(classifierViewButton);
		viewButtonGroup.add(manualViewButton);


		//Register a listener for the radio buttons.
		filterViewButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// Call the adjustFilterView method:
				setToFilterViewMode();
			}

		});
		classifierViewButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// Call the adjustFilterView method:
				setToClassifierViewMode();
			}

		});
		manualViewButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// Call the adjustFilterView method:
				setToManualViewMode();
			}

		});


		// Add buttons to Panel:
		// set fill and weights for all:
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 0.5;
		gbc.weighty = 0.5;

		gbc.gridx = 0;
		gbc.gridy = 0;
		viewPanel.add(viewLabel, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		viewPanel.add(filterViewButton, gbc);
		gbc.gridx = 0;
		gbc.gridy = 2;
		viewPanel.add(classifierViewButton, gbc);
		gbc.gridx = 0;
		gbc.gridy = 3;
		viewPanel.add(manualViewButton, gbc);


		// Add panel to Obj Manager Panel:
		objManagerPanel.add(viewPanel);


		// Add Vertical JSeparator to objManagerPanel:
		objManagerPanel.add(Box.createHorizontalStrut(2) );
		objManagerPanel.add( new JSeparator(SwingConstants.VERTICAL) );
		objManagerPanel.add(Box.createHorizontalStrut(2) );


		// SETUP: the Selected Obj Info Panel:
		// Provides useful information on the image, or if any objects are selected, on the objects.

		// Create JPanel:
		selectedObjInfoPanel = new JPanel();

		// Create JTextArea with default text:
		selectedObjInfo = new JTextArea(6,14);

		updateObjInfo(); //this method will update the ObjInfo TextArea with appropriate text.

		// Make this Text Area non-selectable & non-editable:
		selectedObjInfo.setFocusable(false);
		selectedObjInfo.setEditable(false);

		// Put the text area into a scroll pane:
		selectedObjInfoScrollPane = new JScrollPane(selectedObjInfo);

		//add status TextArea to statusPanel:
		selectedObjInfoPanel.add(selectedObjInfoScrollPane);


		// Add panel to Obj Manager Panel:
		objManagerPanel.add(selectedObjInfoPanel);



		// 2. User can view a graph or a table of object data & export data to FileSystem:

		// What data does the User want to view as graph and table?
		// ARFF -> The current images data
			// manClassArffBuffer -> All current objects which have been classified.
		// Graph:
			// viewing the arff image data is useful to see an overview of object clustering in the image
			// any objects classified in this image will be colour coded.
			// viewing the manClassArffBuffer may be useful to see where different classes are appearing
			// ACROSS images.
		// Data Table:
			// Do Users want to look at the data table?  There is a Danger users may modify it?
			// What does the data table provide that cannot be seen in the graph?
			// Values - are they needed?
			// Editability - is this needed?  Would it even be saved?  
			// It is saved, and it can only cause PROBLEMS!
			// ability to see the obj no or other statistics for objects 
			// This can be found by clicking on points on the Graph!
			// Graph is safer - you cannot modify data, it is visual, it allows all the data to be viewed by
			// clicking on objects.
			// THEREFORE -> Eliminate the Data Table, and just have two buttons - one for viewing the current images
			// graph, the other for viewing the graph of all manually classified objects.

		// Third Button -> User can click this to force the current manual classification data to be saved to the
		// fileSystem.
		// This saving also occurs when a new image is opened, and when the plugin is closed, but after a
		// number of objects have been classified, the user may want to just save the current manually
		// classified objects to the OM output directory to build a classifier.

		// Add a panel with three buttons on to the IWP panel:

		// imagePlotButton will open a new Frame / Panel, which will display the Plot from WEKA library of ARFF dataset:
			// the dataset of all objects in the current image.

		// manualPlotButton will display the Plot from WEKA library of manClassArffBuffer dataset:
			// the dataset of all manually classified objects across all processed images.
			// viewerButton will open new Frame which will display the ARFF data as a table.
			// this is no longer needed - user can view the actual data in weka, and in the arff file on
			// file system.

		// saveArffButton will allow the user to manually save the manClassArffBuffer and arff datasets:
			// arff dataset is saved to the current output file directory - so the data is saved with the same
			// name as the image it is derived from, and in the same directory structure as input image.
			// the manClassArffBuffer is saved to ManualClassificationData.arff in the OM_MetaData directory.

		// Provide:
			// Panel for the Buttons:
		// 3 Buttons:
			// imagePlotButton, manualPlotButton, saveArffButton


		// Generate the panel:
		plotPanel = new JPanel();

		// Set layout to GridLayout with one column:
		plotPanel.setLayout( new GridBagLayout() );

		gbc = new GridBagConstraints();

		//set a border to the panel:
		//plotPanel.setBorder(new LineBorder(Color.BLACK,1,true));

		//JLabel plotLabel = new JLabel("DATA:");

		imagePlotButton = new JButton( createImageIcon("/Icons/Plot 100x100.png", "Image Plot Button", 20, 20) );
		manualPlotButton = new JButton( createImageIcon("/Icons/Plot man 100x100.png", "Manual Plot Button", 20, 20) );
		imageDataTableButton = new JButton( createImageIcon("/Icons/Table 100x100.png", "Image Data Table Button", 20, 20) );

		imagePlotButton.setToolTipText("Open a Plot of the currently open Image Data");
		manualPlotButton.setToolTipText("Open a Plot of the Manually Classified Data");
		imageDataTableButton.setToolTipText("View Current Image Data Table");

		// open a plot of all of the current image data.
		imagePlotButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				// FIRST -> give the IWP custom canvas the focus:
				// Ensures the custom canvas has focus after clicking on the plot button:
				// so keyboard shortcuts will still work on the canvas after the plot panel is closed.
				IWP.cc.requestFocusInWindow();

				// MatrixPanel offers the initial scatterplot matrix panel - showing all attributes
				// as a 2D array of scatterplots. User can select one to move to the visualisePanel:
				//MatrixPanel mp = new MatrixPanel();

				// This goes directly to the VisualisePanel - using the first two attributes by default:
				VisualizePanel mp = new VisualizePanel();

				mp.setInstances(objectDataset.data);

				try {
					mp.setXIndex(4);  //set X to volVoxels by default
					mp.setYIndex(16); //set Y to sphericity by default
				} catch(Exception ex) { }

				// String plotName = arff.relationName();
				JFrame jf = new JFrame("Object Manager: Weka Attribute Selection Visualization");
				// jf.setSize(800, 600);
				jf.setSize(IWP.iw.getSize().width-60, IWP.iw.getSize().height-60);
				jf.setLocation(30,30);
				jf.getContentPane().setLayout(new BorderLayout());
				jf.getContentPane().add(mp, BorderLayout.CENTER);
				jf.addWindowListener(new java.awt.event.WindowAdapter() {

					public void windowClosing(java.awt.event.WindowEvent e) {
						jf.dispose();
					}
				});
				jf.setVisible(true);

			}

		} );

		// open a plot of the manually classified data:
		manualPlotButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				// FIRST -> give the IWP custom canvas the focus:
				// Ensures the custom canvas has focus after clicking on the plot button:
				// so keyboard shortcuts will still work on the canvas after the plot panel is closed.
				IWP.cc.requestFocusInWindow();

				// MatrixPanel offers the initial scatterplot matrix panel - showing all attributes
				// as a 2D array of scatterplots. User can select one to move to the visualisePanel:
				//MatrixPanel mp = new MatrixPanel();

				// This goes directly to the VisualisePanel - using the first two attributes by default:
				VisualizePanel mp = new VisualizePanel();

				mp.setInstances(manClassArffBuffer.arffBuffer);
				// mp.setInstances(manClassArffBuffer.data);

				try {
					mp.setXIndex(5);  //set X to volVoxels by default
					mp.setYIndex(17); //set Y to sphericity by default
				} catch(Exception ex) { }

				// String plotName = arff.relationName();
				JFrame jf = new JFrame("Object Manager: Weka Attribute Selection Visualization");
				// jf.setSize(800, 600);
				jf.setSize(IWP.iw.getSize().width-60, IWP.iw.getSize().height-60);
				jf.setLocation(30,30);
				jf.getContentPane().setLayout(new BorderLayout());
				jf.getContentPane().add(mp, BorderLayout.CENTER);
				jf.addWindowListener(new java.awt.event.WindowAdapter() {

					public void windowClosing(java.awt.event.WindowEvent e) {
						jf.dispose();
					}
				});
				jf.setVisible(true);

			}

		} );

		// save both the arff dataet to its appropriate output file, and the manualClassificationData to
		// ManualClassificationData.arff
		imageDataTableButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				
				// show the current images Data Table:
				objectDataset.showArffDatatable();
				
				// Save the Arff data to correct location in Output Directory:
				//saveArffDataAfterProcessMethod();
				//objectDataset.saveData(arffPath, ObjectDataContainer.returnObjectMeasuresAndManClassAttributes() ); //this method uses the arffPath to save data to correct output.

				// Save the manualClassificationData:
				//saveManualClassificationData();

			}

		});


		// Add button to Panel:
		// set fill and weights for all:
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 0.5;
		gbc.weighty = 0.5;

		gbc.gridx = 0;
		gbc.gridy = 0;
		plotPanel.add(imagePlotButton, gbc);
		gbc.gridx = 0;
		gbc.gridy = 1;
		plotPanel.add(manualPlotButton, gbc);
		gbc.gridx = 0;
		gbc.gridy = 2;
		plotPanel.add(imageDataTableButton, gbc);

		// Add panel to Obj Manager Panel:
		objManagerPanel.add(plotPanel);


		// Add Vertical JSeparator to objManagerPanel:
		objManagerPanel.add(Box.createHorizontalStrut(2) );
		objManagerPanel.add( new JSeparator(SwingConstants.VERTICAL) );
		objManagerPanel.add(Box.createHorizontalStrut(2) );



		// 3. ML & USER Directed object classification - delineating classification boundary:


		// ML DIRECTED OBJECT CLASSIFICATION:

		// IF NO CLASSIFIER HAS BEEN IMPORTED / IS ACTIVE:

		// Use a Rough NaiveBayes or IBk classifier on the currently manually annotated data

		// TRAIN Classifier on the current Manually Classified Points

		// Need to SPLIT the arff dataset into manually CLASSIFIED and UNCLASSIFIED datasets:

		// Split the dataset using FILTERS -> RemoveWithValues, or just set each instance to a new arff...
		// see splitArff() method for more details

		// We are NOT INTERESTED in EVALUATING this Rough Classifier:

		// It is designed to give a ROUGH ESTIMATE of classes -> used to just pick objects for
		// user to classify!  
		// Hopefully roughly locating the decision boundary on attributes it is provided

		// WHAT ABOUT THE ATTRIBUTES -> WHICH ATTRIBUTES SHOULD BE USED?!

		//  Could use Attribute Selection -> BUT not worth the computation 
		// For scheme independent attribute selection it may be worth it...!

		// Instead set some standard attributes for classification, and then give user option to modify

		// TODO OPTIONS -> Add ability to select attributes for rough classifier

		// MAKE SURE TO TRIM THE arff DATASET ATTRIBUTES for both the Training and Test sets!
		// performed in filterAndBuildClassifier()


		// NEXT -> CLASSIFY the UNCLASSIFIED INSTANCES using this built classifier

		// This can give the instances a class value, but ALSO can give the PROBABILITY that a given
		// instance is in the FEATURE or NONFEATURE classes.

		// classifyInstance() method will return the class value (index of nominal, or the double value)

		// distributionForInstance() returns a double[] which contains the decimals indicating the likelihood
		// the classified instance belongs to each class value (index is the same as the index of
		// nominal class values in the original class used for training).
		// These are stored into NAMED CONSTANTS during construction -> FEATURE_INDEX, etc.

		// Above all is Performed in filterAndBuildClassifier(); 


		// Want to use this information to Drive the selection of Ambiguous Objects.

		// Help drive selection of objects for manual classification to delineate the Decision Boundary.

		// Can also BIAS SELECTION towards FEATURE or NON FEATURE (or even CONNECTED) objects to improve 
		//false negative or false positive rates.

		// The user should be able to select what type of object they wish to annotate!
		// i.e, a FEATURE, NON-FEATURE or CONNECTED object.


		// Object Selection Methods:

		// Random Button - randomly select a number of objects in the image, with int input fields to set:

		// the NUMBER of Objects to be classified, and
		// the RANGE of the NaiveBayes/IBk classification of the obj being FEATURE or NON-FEATURE.
		// i.e. two values between 0 and 1 to select a range.


		// L-range -> Linear range, gives 5 points with pre-set statistical chances of being a FEATURE:

		// 0-0.2, 0.2-0.4, 0.4-0.6, 0.6-0.8, 0.8-1.0


		// G-range -> Gaussian (approx) gives 10 points (5 + 3 + 1 + 1) with pre-set statistical chances of
		// being FEATURE:

		//	   			   0.4-0.6
		//      		   0.4-0.6
		//        0.2-0.4, 0.4-0.6, 0.6-0.8	
		// 0-0.2, 0.2-0.4, 0.4-0.6, 0.6-0.8, 0.8-1.0




		// All three should be configurable with the NUMBER and LOW / HIGH probabilities given for the RANDOM
		// selection.



		// THREE -  last two require manual classifications OR IMPORTED CLASSIFIER to use 
		// a classifier to estimate object probabilities:


		// 1. RANDOM with Class Probability:

		// FIRST - set up the settings for all THREE types of Selection:

		// Button & Spinners to randomly select a number of objects in the image:

		// Spinner for:
		// Number of Objects

		// Spinners:  ONLY ACTIVE AFTER OBJECTS HAVE BEEN MANUALLY CLASSIFIED:
		// Lower range of classification probability.
		// Higher range of classification probability.

		// Make Panel:
		objSelRandPanel = new JPanel();

		// set layout:
		objSelRandPanel.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();

		// Combo Box to select what type of object the buttons should based probability selection on:
		// objSelComboBox = new JComboBox<String>( new String[] {"Feature","Non-Feature","Connected"} );
		objSelComboBox = new JComboBox<String>( 
				new String[] {	ObjectDataContainer.FEATUREATR,
						ObjectDataContainer.NONFEATUREATR,
						ObjectDataContainer.CONNECTEDATR } );

		objSelComboBox.setSelectedIndex(0); // set FEATURE Object selection as default

		objSelComboBox.setToolTipText("Select Object Class to use for Object Selection");

		objSelComboBox.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// This sets which Class is looked for during Object Annotation:
				// Will look for class probability on Rough ML with no Filter/Classifier, or
				// Imported Classifier Probability if one has been imported.

				// NOTE: Filter does not give any probabilities, so this cannot be used to help select objects.
				// Instead, Filter just modifies the input -> by preventing objects which do not pass the filter
				// from being considered by the Rough ML or the Imported Classifier.

				// This will allow the User to look for specific types of objects - Feature, Non-Feature, Connected
				// Will still search for these based on probabilities provided - but probabilities of object being of
				// the SELECTED CLASS.
				// 

				// Therefore, this method just needs to modify the instance variable which stores the INDEX of the
				// Probability Array returned when the Classifier is applied to the manually unclassified objects.

				// DONT FORGET - take into account whether a User Classifier is selected - and if so, adjust the values
				// according to the Classifier Indexes!

				String objectClassAttributeValue = (String)objSelComboBox.getSelectedItem();

				// Set the CLASS_INDEX to the index corresponding to the selected Attribute Value 
				// (Feature, Non-Feature or Connected - this is independent of whether a classifier is loaded or
				// not, as both after loading or with a rough Classifier, the Class Index is used the same
				// to select the distributionForInstance data to filter for the CLASS INDEX)

				objectClassifier.setClassIndex( ObjectDataContainer.returnAttributeValueIndex(objectClassAttributeValue) );

				// CLASS_INDEX = ObjectDataContainer_MCIB.returnAttributeValueIndex(objectClassAttributeValue);

				// BELOW NOT NEEDED NOW - Taken care of in the above line!

				//if(ind == 0) {
				//First Index -> FEATURE:
				//if(classifierLoaded == false) {
				//CLASS_INDEX = MAN_FEATURE_INDEX;
				//}
				//else {
				//CLASS_INDEX = CLAS_FEATURE_INDEX;
				//}
				//}
				//else if(ind == 1) {
				//Second Index -> NON-FEATURE:
				//if(classifierLoaded == false) {
				//	CLASS_INDEX = MAN_NONFEATURE_INDEX;
				//}
				//else {
				//	CLASS_INDEX = CLAS_NONFEATURE_INDEX;
				//}
				//}
				//else if(ind == 2) {
				//Third Index -> CONNECTED:
				//if(classifierLoaded == false) {
				//	CLASS_INDEX = MAN_CONNECTED_INDEX;
				//}
				//else {
				//	CLASS_INDEX = CLAS_CONNECTED_INDEX;
				//}
				//}

			}

		});

		// Set the CLASS_INDEX attribute to FEATURE_INDEX in first instance, as this is selected by default by the
		// objSelComboBox:
		// CLASS_INDEX = MAN_FEATURE_INDEX;
		objectClassifier.setClassIndex( ObjectDataContainer.FEATURE_INDEX );

		// Build the Buttons to go under the Combo Box:
			// Options -> to open an options Frame
			// Random Selection Button & Biased Selection Button - to select Objects in the desired manner
		
		// build spinners for the Object Selection Options Panel:
		
		objSelHighLabel = new JLabel("High:");
		objSelLowLabel = new  JLabel("Low:");
		objSelNoLabel = new   JLabel("Number:");
		objSelSeedLabel = new   JLabel("Seed:");
		objSelBiasedStringLabel = new   JLabel("Selection String:");

		objSelHighLabel.setHorizontalAlignment(JLabel.CENTER);
		objSelLowLabel.setHorizontalAlignment(JLabel.CENTER);
		objSelNoLabel.setHorizontalAlignment(JLabel.CENTER);
		objSelSeedLabel.setHorizontalAlignment(JLabel.CENTER);
		
		highModel = new SpinnerNumberModel(1.00,0.00,1.00,0.01);
		lowModel = new SpinnerNumberModel(0.00,0.00,1.00,0.01);

		objSelHigh = new JSpinner(highModel);
		objSelLow = new JSpinner(lowModel);


		objSelHigh.setToolTipText("Set the Upper Bounds of Class Probability for Object Selection");
		objSelLow.setToolTipText( "Set the Lower Bounds of Class Probability for Object Selection");

		// Set number of columns for textfield in spinner to 3:
		JComponent mySpinnerEditor = objSelHigh.getEditor();
		JFormattedTextField jftf = ((JSpinner.DefaultEditor) mySpinnerEditor).getTextField();
		jftf.setColumns(3);

		mySpinnerEditor = objSelLow.getEditor();
		jftf = ((JSpinner.DefaultEditor) mySpinnerEditor).getTextField();
		jftf.setColumns(3);

		//in-activate the spinners initially -> will become active when objects have been classified:
		java.awt.Component[] cps = objSelHigh.getComponents();
		objSelHigh.setEnabled(false);
		for(int a=0; a<cps.length;a++) {
			cps[a].setEnabled(false);
		}

		cps = objSelLow.getComponents();
		objSelLow.setEnabled(false);
		for(int a=0; a<cps.length;a++) {
			cps[a].setEnabled(false);
		}

		// add in options ability to select NUMBER of objects for Random selection
		noModel = new SpinnerNumberModel(10,1,MAX_OBJ_SEL,1);

		objSelNo = new JSpinner(noModel);

		objSelNo.setToolTipText("Set the Number of Objects to be selected");
		
		// add in options ability to select Random Number Seed for Random selection:
		seedModel = new SpinnerNumberModel(100,0,1000000000,1);

		objSelSeed = new JSpinner(seedModel);

		objSelSeed.setToolTipText("Set the Random Number Seed - this will determine the random number sequence. Set to 0 for truly random numbers");
		
		// Also build TextField component for setting Biased Object Selection String
		
		// make Textfield, 12 columns, blank text:
		biasedObjectSelectionTextfield = new JTextField(biasedSelectionString, 12);
		
		biasedObjectSelectionTextfield.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				// set the BiasedSelectionString:
				biasedSelectionString = biasedObjectSelectionTextfield.getText();
				
			}
			
		});		
		
		objSelRandOptionsButton = new JButton("OPTIONS");
		
		objSelRandOptionsButton.setToolTipText("Set Options for Random and Biased object Selection");
		
		objSelRandOptionsButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				objSelRandOptionsFrame = new JFrame();

				JPanel p = new JPanel();

				p.setLayout(new GridBagLayout());
				GridBagConstraints gbc = new GridBagConstraints();

				// set fill and weights for all:
				gbc.fill = GridBagConstraints.BOTH;
				gbc.weightx = 0.5;
				gbc.weighty = 0.5;


				objSelOKButton = new JButton("OK");

				objSelOKButton.addActionListener( new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {

						// set the BiasedSelectionString:
						biasedSelectionString = biasedObjectSelectionTextfield.getText();

						objSelRandOptionsFrame.setVisible(false);
						objSelRandOptionsFrame.dispose();

						// give custom canvas focus back:
						IWP.cc.requestFocusInWindow();
					}

				});

				gbc.gridx = 0;
				gbc.gridy = 0;
				p.add(objSelHighLabel, gbc);
				gbc.gridx = 1;
				gbc.gridy = 0;
				p.add(objSelHigh, gbc);

				gbc.gridx = 0;
				gbc.gridy = 1;
				p.add(objSelLowLabel, gbc);
				gbc.gridx = 1;
				gbc.gridy = 1;
				p.add(objSelLow, gbc);

				gbc.gridx = 0;
				gbc.gridy = 2;
				p.add(objSelNoLabel, gbc);
				gbc.gridx = 1;
				gbc.gridy = 2;
				p.add(objSelNo, gbc);

				gbc.gridx = 0;
				gbc.gridy = 3;
				p.add(objSelSeedLabel, gbc);
				gbc.gridx = 1;
				gbc.gridy = 3;
				p.add(objSelSeed, gbc);

				gbc.gridx = 0;
				gbc.gridy = 4;
				p.add(objSelBiasedStringLabel, gbc);
				gbc.gridx = 1;
				gbc.gridy = 4;
				p.add(biasedObjectSelectionTextfield, gbc);

				gbc.gridx = 1;
				gbc.gridy = 5;
				p.add(objSelOKButton, gbc);


				objSelRandOptionsFrame.setContentPane(p);
				objSelRandOptionsFrame.pack();
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize(); // set location to centre of screen
				objSelRandOptionsFrame.setLocation(
						(screenSize.width/2)-(objSelRandOptionsFrame.getWidth()/2), 
						(screenSize.height/2)-(objSelRandOptionsFrame.getHeight()/2) );
				objSelRandOptionsFrame.setVisible(true);

			}

		});
				
		// 1. RANDOM with Class Probability:
				
		objSelRandButton = new JButton( createImageIcon("/Icons/Random 100x100.png", "Plot Button", 20, 20) );

		objSelRandButton.setToolTipText("Select 'Num' Random objects between 'Low' and 'High' probabilities");
		
		objSelRandButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				// Select set of objects at random:
				// need to run this in a separate thread -> free up EDT for actually classifying the objects!
				selectRandomObjects( (int)noModel.getValue(), (double)lowModel.getValue(), (double)highModel.getValue() );

			}

		});
		
		// 2. BIASED with Class Probability -> Linear, Gaussian, or any distribution:
		
		objSelBiasedButton = new JButton( createImageIcon("/Icons/Gauss 100x100.png", "Plot Button", 20, 20) );

		objSelBiasedButton.setToolTipText("Bias Selection of Objects based on Classification Probability & Selection String");
		
		objSelBiasedButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Set up option to set STRING gaussianNumber
				// gaussianNumber: String separated by commas:  1,2,4,2,1  :  1,2,4,10,4,2,1  :  1,2,4,10
				// gaussianObjectString = "1,2,4,2,1";
				selectGaussianObjects( biasedSelectionString, (double)lowModel.getValue(), (double)highModel.getValue() );

				//selectGaussianObjects( gaussianNumber, (double)lowModel.getValue(), (double)highModel.getValue() );

			}

		});

		// set fill and weights for all:
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 0.5;
		gbc.weighty = 0.5;

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		objSelRandPanel.add(objSelComboBox, gbc);
		gbc.gridwidth = 1;

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridheight = 2;
		objSelRandPanel.add(objSelRandOptionsButton, gbc);
		gbc.gridheight = 1;
		
		gbc.gridx = 1;
		gbc.gridy = 1;
		objSelRandPanel.add(objSelRandButton, gbc);

		gbc.gridx = 1;
		gbc.gridy = 2;
		objSelRandPanel.add(objSelBiasedButton, gbc);

		objSelRandPanel.setBorder( BorderFactory.createTitledBorder("Obj. Selection") );

		// Add panel to Obj Manager Panel:
		objManagerPanel.add(objSelRandPanel);



		//objSelDistPanel = new JPanel();

		//objSelDistPanel.setLayout( new GridBagLayout() );

		//gbc = new GridBagConstraints();

		// set fill and weights for all:
		//gbc.fill = GridBagConstraints.BOTH;
		//gbc.weightx = 0.5;
		//gbc.weighty = 0.5;



		// 1. RANDOM with Class Probability:


		//objSelRandButton = new JButton( createImageIcon("/Icons/Random 100x100.png", "Plot Button", 20, 20) );

		//objSelRandButton.setToolTipText("Select 'Num' Random objects between 'Low' and 'High' probabilities");

		//objSelRandButton.addActionListener( new ActionListener() {

			//@Override
			//public void actionPerformed(ActionEvent e) {

				// Select set of objects at random:
				// need to run this in a separate thread -> free up EDT for actually classifying the objects!
				//selectRandomObjects( (int)noModel.getValue(), (double)lowModel.getValue(), (double)highModel.getValue() );

			//}

		//});

		//gbc.gridx = 0;
		//gbc.gridy = 0;
		//objSelDistPanel.add(objSelRandButton, gbc);



		// 2. Linear Range:
		// Button to run this selection protocol:
		// Presents 5 objects across a linear range of probabilities.


		//linearButton = new JButton( createImageIcon("/Icons/Linear 100x100.png", "Plot Button", 20, 20) );

		//linearButton.setToolTipText("Select a Linear set of 'Num' objects between 'Low' and 'High' probabilities");

		//linearButton.addActionListener( new ActionListener() {

			//@Override
			//public void actionPerformed(ActionEvent e) {

				// TODO Set up option to set linearNumber:
				// selectLinearObjects( 5, (double)lowModel.getValue(), (double)highModel.getValue() );
				// alternatively, can use the selectGaussianObjects() method, with the special Linear String "1,1,1"
				// with the number of 1s indicating the number of divisions:
				//selectGaussianObjects( linearObjectString, (double)lowModel.getValue(), (double)highModel.getValue() );

				// selectLinearObjects( linearNumber, linearDivisions, (double)lowModel.getValue(), (double)highModel.getValue() );

			//}

		//});

		//gbc.gridx = 0;
		//gbc.gridy = 1;
		//objSelDistPanel.add(linearButton, gbc);



		// 3. Gaussian Range:
		// Button to run this selection protocol:
		// Presents 10 objects across a Gaussian range of probabilities.


		//gaussianButton = new JButton( createImageIcon("/Icons/Gauss 100x100.png", "Plot Button", 20, 20) );

		//gaussianButton.setToolTipText("Select a Gaussian set of 'Num' objects between 'Low' and 'High' probabilities");

		//gaussianButton.addActionListener( new ActionListener() {

			//@Override
			//public void actionPerformed(ActionEvent e) {
				// TODO Set up option to set STRING gaussianNumber
				// gaussianNumber: String separated by commas:  1,2,4,2,1  :  1,2,4,10,4,2,1  :  1,2,4,10
				// gaussianObjectString = "1,2,4,2,1";
				//selectGaussianObjects( biasedSelectionString, (double)lowModel.getValue(), (double)highModel.getValue() );

				//selectGaussianObjects( gaussianNumber, (double)lowModel.getValue(), (double)highModel.getValue() );

			//}

		//});

		//gbc.gridx = 0;
		//gbc.gridy = 2;
		//objSelDistPanel.add(gaussianButton, gbc);


		// Add panel to Obj Manager Panel:
		//objManagerPanel.add(objSelDistPanel);


		// Add Vertical JSeparator to objManagerPanel:
		//objManagerPanel.add(Box.createHorizontalStrut(2) );
		//objManagerPanel.add( new JSeparator(SwingConstants.VERTICAL) );
		//objManagerPanel.add(Box.createHorizontalStrut(2) );



		// ATTRIBUTE DIRECTED OBJECT CLASSIFICATION:

		// Objects Selected for Manual Classification based on the value range of one Attribute
		// Only UNCLASSIFIED OBJECTS need classifying, so only UNCLASSIFIED Objects are considered here?
		// However, this function may be used to Select a specific Object - for example, a specific object
		// number; or perhaps to look at objects of a certain size (whether manually classified or not)

		// Therefore -> Need a checkbox to allow the Attribute Object Selection to work on ALL OBJECTS.

		// Here supply a JComboBox to select an attribute - or none!
		// Supply two JSpinners which are the min and max value to filter objects with.
		// JButton will SYSTEMATICALLY select these objects for User to see & classify:
		// Create < & > key pressed commands to move through objects.
		// Create ENTER key pressed commands to cancel object selection mode.
		// Supply a checkbox to allow the user to check whether ALL objects should be assessed:
		// If ON, all objects (UNCLASSIFIED, FEATURE, CONNECTED, NONFEATURE) are searched, 
		// If OFF, only UNCLASSIFIED Objects are searched for.

		// Make Panel:
		attrSelectionPanel = new JPanel();

		// set layout:
		attrSelectionPanel.setLayout( new GridBagLayout() );
		gbc = new GridBagConstraints();

		// set fill and weights for all:
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 0.5;
		gbc.weighty = 0.5;

		// Label for this Panel:
		//attrLabel = new JLabel("Obj. Selection by Attribute:");


		// ComboBox for Attributes:

		//get the attribute names and store to attrs - to construct the combobox:
		String [] attrs = new String[attributes.size() + 1];

		// start with a blank string - represents NO ATTRIBUTE SELECTION
		attrs[0] = "";

		for(int a=0; a<attributes.size(); a++) {
			attrs[a+1] = attributes.get(a).name();
		}


		attrSelComboBox = new JComboBox<String>(attrs);

		attrSelComboBox.setSelectedIndex(0); // set the blank selection as default

		attrSelComboBox.setToolTipText("Select Attribute to use for Object Selection");

		attrSelComboBox.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				// if attrSelComboBox selection index is 0, then need to in-activate the attrMin and attrMax selectors &
				// the SELECT button:

				if(attrSelComboBox.getSelectedIndex() == 0) {

					// in-activate Spinners:
					java.awt.Component[] cps = attrMax.getComponents();
					attrMax.setEnabled(false);
					for(int a=0; a<cps.length;a++) {
						cps[a].setEnabled(false);
					}

					cps = attrMin.getComponents();
					attrMin.setEnabled(false);
					for(int a=0; a<cps.length;a++) {
						cps[a].setEnabled(false);
					}

					// in-activate Selection Button:
					attrSelectionButton.setEnabled(false);

				}


				// Else, an attribute is selected on attrSelComboBox - so need to activate attrMin & attrMax and the
				// SELECT button, and also determine and set min and max values for attrMin & attrMax

				else {

					attrMinVal = objectDataset.getMinValue( (attrSelComboBox.getSelectedIndex()-1) );
					attrMaxVal = objectDataset.getMaxValue( (attrSelComboBox.getSelectedIndex()-1) );


					// Then set these numbers to the Min and Max JSpinners
					// Generate appropriate SpinnerNumberModel
					// how to determine the STEP SIZE?!
					// Will need a method to retrieve the step size based on which attribute is selected.
					// Set this to the JSpinner.
					// Set numbers to min and max.

					if(objectDataset.getStepSize(attrSelComboBox.getSelectedIndex()-1) == 0.001) {
						//IJ.showMessage("Before Round min + max val: "+attrMinVal+" "+attrMaxVal);
						attrMaxVal = ( (double)Math.floor(attrMaxVal * 1000d) / 1000d ) + 0.001;
						attrMinVal = ( (double)Math.floor(attrMinVal * 1000d) / 1000d );
						//IJ.showMessage("Round min + max val: "+attrMinVal+" "+attrMaxVal);
						attrMaxModel = new SpinnerNumberModel(attrMaxVal,attrMinVal,attrMaxVal, 0.001 );
						attrMinModel = new SpinnerNumberModel(attrMinVal,attrMinVal,attrMaxVal, 0.001 );
					}
					else {
						//attrMaxModel = new SpinnerNumberModel((int)attrMaxVal,(int)attrMinVal,(int)attrMaxVal, 1 );
						//attrMinModel = new SpinnerNumberModel((int)attrMinVal,(int)attrMinVal,(int)attrMaxVal, 1 );
						attrMaxModel = new SpinnerNumberModel(attrMaxVal,attrMinVal,attrMaxVal, 1.0 );
						attrMinModel = new SpinnerNumberModel(attrMinVal,attrMinVal,attrMaxVal, 1.0 );
					}

					attrMax.setModel(attrMaxModel);
					attrMin.setModel(attrMinModel);

					// Activate the attrMax and attrMin components:
					java.awt.Component[] cps = attrMax.getComponents();
					attrMax.setEnabled(true);
					for(int a=0; a<cps.length;a++) {
						cps[a].setEnabled(true);
					}

					cps = attrMin.getComponents();
					attrMin.setEnabled(true);
					for(int a=0; a<cps.length;a++) {
						cps[a].setEnabled(true);
					}

					// and activate the attrSelectionButton:
					attrSelectionButton.setEnabled(true);
				}

			}

		});


		// JLabels for JSpinners:

		attrMaxLabel = new JLabel("Max:");
		attrMinLabel = new  JLabel("Min:");

		attrMaxLabel.setHorizontalAlignment(JLabel.CENTER);
		attrMinLabel.setHorizontalAlignment(JLabel.CENTER);

		// JSpinners for min and max values:

		attrMaxModel = new SpinnerNumberModel(1.00,0.00,1.00,0.01);
		attrMinModel = new SpinnerNumberModel(0.00,0.00,1.00,0.01);

		attrMax = new JSpinner(attrMaxModel);
		attrMin = new JSpinner(attrMinModel);


		attrMax.setToolTipText("Set the Max value of the Attribute for Object Selection");
		attrMin.setToolTipText("Set the Min value of the Attribute for Object Selection");

		// Set number of columns for textfield in spinner to 5:
		mySpinnerEditor = attrMax.getEditor();
		jftf = ((JSpinner.DefaultEditor) mySpinnerEditor).getTextField();
		jftf.setColumns(5);

		mySpinnerEditor = attrMin.getEditor();
		jftf = ((JSpinner.DefaultEditor) mySpinnerEditor).getTextField();
		jftf.setColumns(5);

		//in-activate the spinners initially -> will become active when objects have been classified:
		cps = attrMax.getComponents();
		attrMax.setEnabled(false);
		for(int a=0; a<cps.length;a++) {
			cps[a].setEnabled(false);
		}

		cps = attrMin.getComponents();
		attrMin.setEnabled(false);
		for(int a=0; a<cps.length;a++) {
			cps[a].setEnabled(false);
		}

		// JCheckBox to check whether ALL objects should be assessed, or only UNCLASSIFIED Objects:

		attrCheckBox = new JCheckBox("ALL");

		attrCheckBox.setToolTipText("Check ON to run object selection by Attribute Range on ALL objects, or check off"
				+"to run object selection by Attribute Range on only UNCLASSIFIED OBJECTS");

		// Not Necessary to implement an action listener here, Can just read the status of the checkbox itself as
		// readout to whether ALL or only UNCLASSIFIED Objects should be assessed:
		// Use the attrCheckBox.isSelected() method
		// attrCheckBox.isSelected();


		// JButton for selecting the object set:

		//attrSelectionButton = new JButton( createImageIcon("/Icons/Select Object 100x100.png", "Plot Button", 40, 40) );

		// instead of just one button, make two buttons to allow the user to traverse objects in forward and reverse
		// direction?
		// Do not need this, as the user can traverse objects with keyboard shortcuts - as in objSelMode!

		attrSelectionButton = new JButton( "SELECT" );

		attrSelectionButton.setToolTipText("Select Objects with Attribute Values between Min and Max.");

		// Initially set the button as disabled - enable it once user has selected an attribute in the combobox.
		attrSelectionButton.setEnabled(false);

		attrSelectionButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// FIRST - check the spinner values are in a legal range:
				// This code should be un-reachable, as the spinners are always in range - they will not accept
				// an out of range value!
				if ( 	(double)attrMinModel.getValue() < attrMinVal || 
						(double)attrMinModel.getValue() > attrMaxVal ||
						(double)attrMaxModel.getValue() < attrMinVal || 
						(double)attrMaxModel.getValue() > attrMaxVal 	) {
					// Spinner Values are not in the legal range, report an error and return:
					IJ.showMessage("Spinner Values not in legal range", 
							"Spinner values Min & Max must be between"
									+ "\nmin: "+attrMinVal+" and max: "+attrMaxVal);
					return;

				}
				// Run attributeSelectRandomObject:
				attributeSelectObjects();	
			}

		});


		// add Label & combobox to panel - covering 3 cells in width:
		//gbc.gridx = 0;
		//gbc.gridy = 0;
		gbc.gridwidth = 3;

		//attrSelectionPanel.add(attrLabel, gbc);

		gbc.gridx = 0;
		gbc.gridy = 0;

		attrSelectionPanel.add(attrSelComboBox, gbc);

		gbc.gridwidth = 1; //re-set width back to 1.

		gbc.gridx = 0;
		gbc.gridy = 1;
		attrSelectionPanel.add(attrMinLabel, gbc);
		gbc.gridx = 1;
		gbc.gridy = 1;
		attrSelectionPanel.add(attrMin, gbc);
		gbc.gridx = 0;
		gbc.gridy = 2;
		attrSelectionPanel.add(attrMaxLabel, gbc);
		gbc.gridx = 1;
		gbc.gridy = 2;
		attrSelectionPanel.add(attrMax, gbc);

		gbc.gridx = 2;
		gbc.gridy = 1;
		attrSelectionPanel.add(attrCheckBox, gbc);

		gbc.gridx = 2;
		gbc.gridy = 2;
		attrSelectionPanel.add(attrSelectionButton, gbc);

		// Add Border with Label to attrSelectionPanel:
		attrSelectionPanel.setBorder( BorderFactory.createTitledBorder("Obj. Selection: Attribute"));


		// Add panel to Obj Manager Panel:
		objManagerPanel.add(attrSelectionPanel);

		// Add Vertical JSeparator to objManagerPanel:
		// NOT NEEDED AS HAVE A BORDER AROUND THIS PANEL NOW!
		//objManagerPanel.add(Box.createHorizontalStrut(2) );
		//objManagerPanel.add( new JSeparator(SwingConstants.VERTICAL) );
		//objManagerPanel.add(Box.createHorizontalStrut(2) );



		// HIGH / LOW PASS FILTER APPLICATION -> Components & Behaviours:

		// 4. User can select low / high pass filter values (size, shape, attribute):

		// This applies a primitive filter to the data based on low and high pass filter values set 
		// on one attribute.

		// Only allow the user to filter on ONE ATTRIBUTE - Low & High Pass values can be set.

		// Typically, SIZE will be picked, but allow the user to pick any attribute.


		// HOW IS THIS INTEGRATED INTO THE OBJECT MANAGER OUTPUT?

		// OBJECTS WHICH DO NOT PASS THE FILTER ARE NOT ANALYSED.

		// The User may want to apply: 
		// No Filter nor Classifier
		// a high/low pass filter only,
		// a Classifier only.
		// A filter and THEN a Classifier.
		// here, it is important that the User knows to build the Classifier using
		// the subset of data which pass the Filter
		// Therefore, apply the filter and use the combination of FILTER and MANUAL class Attributes to find
		// objects to use for ML

		// If the Filter is Applied -> when saving the arff dataset, should give option to save only the 
		// instances which meet the Filters high and low pass settings?
		// WHAT IS SAVED WHEN ARFF FILE IS SAVED?!  Only the objects which Manual Class is NOT unclassified
		// i.e. it has been set to feature, nonfeature, connected.
		// Therefore, only manually classified data should still only be exported, but the Filter class will also
		// be given for all manually classified data points.

		// A Classifier and THEN a Filter?  I dont feel it makes sense to do this...
		//When would this be useful?  As a Classifier acts to perform essentially a set of complex
		// filter operations across multiple Attributes, it should be able to take into account any
		// Filtering during its training.
		// Indeed, will only add a Filter -> Classifier option as there may be some obvious small objects
		// which the User wants to remove, to allow the Classifier to focus its efforts on dividing 
		// between more nuanced objects in the Image, or enriching the number of FEATURE objects if there
		// is a large number of NON_FEATURE objects...


		// HOW IS THIS INTEGRATED WITH OBJECT ANNOTATION ABOVE?

		// For example, if the Filter is ON -> Should the Object Annotation Algorithms IGNORE or INCLUDE objects
		// which do not by-pass the Filter?

		// Once a Filter is selected, Object Annotation should only include Objects which pass the Filter BY DEFAULT
		// User should be able to turn this on or off!

		// Ensure there is an option to allow Object Annotation to either INCLUDE or IGNORE objects which pass
		// the Object Filter.

		// THIS MAY NOT BE NECESSARY AS THE USER CAN SIMPLY TURN THE FILTER OFF, THEREFORE ENSURING ALL OBJECTS
		// ARE INCLUDED


		// BASIC RULE -> 
		// If a Filter is Applied to the dataset + TOGGLE ON, it is assumed a Filter is being used.
		// If a Classifier is IMPORTED + TOGGLE ON, it is assumed the Classifier is being used.
		// Therefore, if BOTH a Filter is Applied, and a Classifier is IMPORTED, assumed BOTH are being used!
		// The ORDER will ALWAYS BE:  Filter -> Classifier

		// Finally, if a Classifier is not IMPORTED and a Filter is not Applied, assumed none are used.

		// REMEMBER -> Object Manager is needed for any Object Assessment -> as need to know Max Object Dimensions
		// for potential Exclusion Zones??
		// THEREFORE -> May run the Obj Manager and NOT apply a Filter or Classifier



		// Supply:
		// JLabel - Stating these components are for Filtering
		// JComboBox - to select the attribute to filter.
		// JSpinners - to select and set the low and high pass values to filter on.
		// JCheckBox - to activate the Filter Panel components: to set a filter!
		// JPanel - to place these components on to.
		// FilterObject - object to hold the attribute, minVal, maxVal and arrays of object references filtered
		// by the filter?


		// Use a ObjectFilter object to apply the logic of the filtering:
		// objectFilter = new ObjectFilter(objectDataset, imageHandler, objIdentifier); // moved to setup with objIdentifier
		// run the addDatasetAndImage() method from OBjectFilter here - to add dataset and imafge to filter:
		objectFilter.addDatasetAndImage(objectDataset, imageHandler);
		
		// also set the boolean which tracks the objectFilter filterMaxReached
		// filterMaxReached = objectFilter.getFilterMaxReached(); -> implemented in ObjectFilter + filterMinReached too!


		//Make Panel:
		filterPanel = new JPanel();

		//set layout:
		filterPanel.setLayout( new GridBagLayout() );
		gbc = new GridBagConstraints();

		// set fill and weights for all:
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 0.5;
		gbc.weighty = 0.5;

		// ComboBox for Attributes:

		//get the attribute names and store to attrs - to construct the combobox:
		// String [] attrs = new String[attributes.size() + 1];
		// This was generated earlier in this method - so will reuse the attrs array from then!


		filterComboBox = new JComboBox<String>(attrs);

		filterComboBox.setSelectedIndex(0); // set the blank selection as default

		filterComboBox.setToolTipText("Select Attribute to apply an Object Filter on");

		filterComboBox.setEnabled(false); // initially disabled - enable when filterCheckBox is ON

		//previousFilterAttributeIndex = 0;

		filterComboBox.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Automatically Set the filterMax and filterMin Spinners to the Max and Min numbers
				// for the selected Attribute to filter on.

				//IJ.showMessage("filterComboBox Action Performed called.");

				// By setting to the min and max numbers, the Filter AUTOMATICALLY includes ALL OBJECTS.

				// Only if the User changes these values will objects start to be filtered.

				// User MUST ACTIVATE the application of the Filter -> 
				//Checkbox to switch activation and shutdown of Filter
				// This Checkbox should be activated when an attribute is selected in the filterComboBox, and the
				// view should be moved to Filter View automatically.


				// if attrSelComboBox selection index is 0, then need to in-activate the attrMin and attrMax selectors &
				// the SELECT button:

				int selectedIndex = filterComboBox.getSelectedIndex();

				if(selectedIndex == 0) {

					// inactivate Spinners:
					setFilterSpinnersEnabled(false);

					// also remove the object filter on objects in arff:
					removeFilterObjects();

					// previousFilterAttributeIndex = selectedIndex;
					
					deactivateFilterOverClassifier();

				}			

				// Else, an attribute is selected on filterSelComboBox - so need to activate filterMin & filterMax and the
				// SELECT button, and also determine and set min and max values for filterMin & filterMax

				else {

					removeFilterObjects();

					// previousFilterAttributeIndex = selectedIndex;
					
					objectFilter.setAttributeIndex(selectedIndex-1); // not absolutely necessary, but it is keeping
																	// the object filters datai n line with the combo box!
						// its not necessary as in the spinenr listeners, when the run filterObject, they pass the
							// combobox index to the objectFilter at that time...

					// index ints for setting the starting index in ARFF dataset to begin filtering min and max values
					// This will make adjusting the filter values in the ARFF dataset much more efficient!
					// Initialise these to be the number of instances in ARFF and 0, respectively:
					// should move this to removeFilterObject() method - as will ALWAYS have to run with this method!
					// whenever the filter is removed, MUST reset these instance variables!
					// filterMaxIndex = arff.size();
					// filterMinIndex = 0;

					// First, determine the Min and Max value for the selected Attribute:
					// Loop through all instances, and record the min and max values for selected attribute.

					attrMinVal = objectDataset.getMinValue( (selectedIndex-1) );
					attrMaxVal = objectDataset.getMaxValue( (selectedIndex-1) );

					// Then set these numbers to the Min and Max JSpinners
					// Generate appropriate SpinnerNumberModel
					// how to determine the STEP SIZE?!
					// Will need a method to retrieve the step size based on which attribute is selected.
					// Set this to the JSpinner.
					// Set numbers to min and max.

					if(objectDataset.getStepSize(filterComboBox.getSelectedIndex()-1) == 0.001) {
						//IJ.showMessage("Before Round min + max val: "+attrMinVal+" "+attrMaxVal);
						attrMaxVal = ( (double)Math.floor(attrMaxVal * 1000d) / 1000d ) + 0.001;
						attrMinVal = ( (double)Math.floor(attrMinVal * 1000d) / 1000d );
						// IJ.showMessage("Round min + max val: "+attrMinVal+" "+attrMaxVal);
						filterMaxModel = new SpinnerNumberModel(attrMaxVal,attrMinVal,attrMaxVal, 0.001 );
						filterMinModel = new SpinnerNumberModel(attrMinVal,attrMinVal,attrMaxVal, 0.001 );
						//filterMinModel = new SpinnerNumberModel(attrMinVal,0.0,attrMaxVal, 0.001 );
					}
					else {
						//filterMaxModel = new SpinnerNumberModel((int)attrMaxVal,(int)attrMinVal,(int)attrMaxVal, 1 );
						//filterMinModel = new SpinnerNumberModel((int)attrMinVal,(int)attrMinVal,(int)attrMaxVal, 1 );
						filterMaxModel = new SpinnerNumberModel(attrMaxVal,attrMinVal,attrMaxVal, 1.0 );
						filterMinModel = new SpinnerNumberModel(attrMinVal,attrMinVal,attrMaxVal, 1.0 );
						//filterMinModel = new SpinnerNumberModel(attrMinVal,0.0,attrMaxVal, 1.0 );
					}

					filterMax.setModel(filterMaxModel);
					filterMin.setModel(filterMinModel);

					// Activate the filterMax and filterMin components:
					setFilterSpinnersEnabled(true);

					// and activate the filterSelectionButton:
					//filterSelectionButton.setEnabled(true);
					// filterCheckBox.setEnabled(true);
					//if(filterCheckBox.isSelected() == false) {
					//filterCheckBox.doClick();
					//}
					
					activateFilterOverClassifierCheckBox();
					
					// may need to recompute the Loaded Classifier here:
						// if the ON Class is already checked and a classifier is already loaded!
					recomputeLoadedClassifierAfterFilter();

				}

			}

		});

		// JLabels for JSpinners:

		filterMaxLabel = new JLabel("High Pass:");
		filterMinLabel = new  JLabel("Low Pass:");

		filterMaxLabel.setHorizontalAlignment(JLabel.CENTER);
		filterMinLabel.setHorizontalAlignment(JLabel.CENTER);

		filterMaxLabel.setToolTipText("Set the Max value for the Attribute for Object Filtering");
		filterMinLabel.setToolTipText("Set the Min value for the Attribute for object Filtering");

		// JSpinners for min and max values:

		filterMaxModel = new SpinnerNumberModel(1.00,0.00,1.00,0.01);
		filterMinModel = new SpinnerNumberModel(0.00,0.00,1.00,0.01);

		filterMax = new JSpinner(filterMaxModel);
		filterMin = new JSpinner(filterMinModel);

		// index ints for setting the starting index in ARFF dataset to begin filtering min and max values
		// This will make adjusting the filter values in the ARFF dataset much more efficient!
		// Initialise these to be the number of instances in ARFF and 0, respectively:
		// filterMaxIndex = datasetHandler.size();
		// filterMinIndex = 0;


		filterMax.setToolTipText("Set the Max value for the Attribute for Random Object Selection");
		filterMin.setToolTipText("Set the Min value for the Attribute for Random object Selection");

		// Set number of columns for textfield in spinner to 3:
		mySpinnerEditor = filterMax.getEditor();
		jftf = ((JSpinner.DefaultEditor) mySpinnerEditor).getTextField();
		jftf.setColumns(3);

		mySpinnerEditor = filterMin.getEditor();
		jftf = ((JSpinner.DefaultEditor) mySpinnerEditor).getTextField();
		jftf.setColumns(3);

		//in-activate the spinners initially -> will become active when objects have been classified:
		cps = filterMax.getComponents();
		filterMax.setEnabled(false);
		for(int a=0; a<cps.length;a++) {
			cps[a].setEnabled(false);
		}

		cps = filterMin.getComponents();
		filterMin.setEnabled(false);
		for(int a=0; a<cps.length;a++) {
			cps[a].setEnabled(false);
		}

		// set action listeners to filterMax and filterMin:

		filterMax.addChangeListener( new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				// when the state is changed, need to update the object filter with the values of
				// filterMax and filterMin:

				//IJ.showMessage("filterMax stateChanged() called");

				// FIRST - check the spinner values are in a legal range:
				// This code should be un-reachable, as the spinners are always in range - they will not accept
				// an out of range value!
				if ( 	(double)filterMinModel.getValue() < attrMinVal || 
						(double)filterMinModel.getValue() > attrMaxVal ||
						(double)filterMaxModel.getValue() < attrMinVal || 
						(double)filterMaxModel.getValue() > attrMaxVal 	) {
					// Spinner Values are not in the legal range, report an error and return:
					IJ.showMessage("Spinner Values not in legal range", 
							"Spinner values Min & Max must be between"
									+ "\nmin: "+attrMinVal+" and max: "+attrMaxVal);
					return;

				}
				// THEN - ensure all objects are unselected, to ensure they are all filtered correctly.
				unselectAnyObject();

				// Run filterObjects:
				filterObjects(  (double)filterMinModel.getValue(), 
						(double)filterMaxModel.getValue(), 
						(filterComboBox.getSelectedIndex() - 1)  );	
				
				// may need to recompute the Loaded Classifier here:
					// if the ON Class is already checked and a classifier is already loaded!
				recomputeLoadedClassifierAfterFilter();

			}

		});

		filterMin.addChangeListener( new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				// when the state is changed, need to update the object filter with the values of
				// filterMax and filterMin:

				//IJ.showMessage("filterMin stateChanged() called");

				// FIRST - check the spinner values are in a legal range:
				// This code should be un-reachable, as the spinners are always in range - they will not accept
				// an out of range value!
				if ( 	(double)filterMinModel.getValue() < attrMinVal || 
						(double)filterMinModel.getValue() > attrMaxVal ||
						(double)filterMaxModel.getValue() < attrMinVal || 
						(double)filterMaxModel.getValue() > attrMaxVal 	) {
					// Spinner Values are not in the legal range, report an error and return:
					IJ.showMessage("Spinner Values not in legal range", 
							"Spinner values Min & Max must be between"
									+ "\nmin: "+attrMinVal+" and max: "+attrMaxVal);
					// return;

				}

				// THEN - ensure all objects are unselected, to ensure they are all filtered correctly.
				unselectAnyObject();

				// Run filterObjects:
				filterObjects( (double)filterMinModel.getValue(), 
						(double)filterMaxModel.getValue(),
						(filterComboBox.getSelectedIndex() - 1) );	
				
				// may need to recompute the Loaded Classifier here:
					// if the ON Class is already checked and a classifier is already loaded!
				recomputeLoadedClassifierAfterFilter();

			}

		});


		filterCheckBox = new JCheckBox("Filter");

		filterCheckBox.setToolTipText("Check ON to activate Filter, check OFF to de-activate Filter");

		filterCheckBox.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				// Set the filterViewButton to enabled if checked on, or disabled if check off:
				if(filterCheckBox.isSelected() == true) {

					filterViewButton.setEnabled(true);
					// and select it:
					if(filterViewButton.isSelected() == false) {
						//manualViewButton.setSelected(true);
						filterViewButton.doClick();
					}

					// enable the filterComboBox:
					filterComboBox.setEnabled(true);

					if(filterComboBox.getSelectedIndex() != 0) {
						// only if the filterComboBox is on a selected attribute, then:

						// activate the filter Spinners:
						// if filterComboBox is not on an attribute, then when the user sets it to an attribute
						// it will activate the spinners then..
						setFilterSpinnersEnabled(true);

						// also set the object filter on the currently set min and max values:
						// as the filterSpinners may have been set previously, want to re-activate the filter
						// as it was previously set:
						filterObjects( (double)filterMinModel.getValue(), 
								(double) filterMaxModel.getValue(), 
								(filterComboBox.getSelectedIndex() - 1)  );
						
						activateFilterOverClassifierCheckBox();

					}

				}
				else {

					//IJ.showMessage("filterCheckBox: false");

					// if the filter view button is currently selected, ensure another view button is checked
					// check the manual view button by default, as this button will never be de-activated:
					if(filterViewButton.isSelected() == true) {
						//manualViewButton.setSelected(true);
						manualViewButton.doClick();
					}
					filterViewButton.setEnabled(false);

					// disable filterComboBox:
					filterComboBox.setEnabled(false);

					// also remove object filter on arff:
					// what will this do?  It would be a waste of effort to un-filter all objects, but then it
					// would also be a wasted effort to traverse EVERY object when a new filter is applied (when
					// we cannot be sure which objects are set to be filtered).
					// It is probably necessary to removeFilterObjects:
					// use the same algorithm as filterObjects -> look at values set for min and max, and only
					// need to correct the filter values for objects which are NOT_PASSED the filter!
					//IJ.showMessage("previousFilterAttributeIndex: "+previousFilterAttributeIndex );
					
					//if(previousFilterAttributeIndex !=0 ) {
						// only if the filterComboBox is on a selected attribute, then:
							// if its on 0, then no fitler is being applied -> its already been removed!

						// in-activate the filter Spinners:
						// this will inactivate the filters to prevent the user adjusting them while the filter
						// is checked off
						// note, if the filterComboBox is set to the first entry (i.e previousFilterAttributeIndex
						// is 0), then the filter spinners will already be inactivated, so there would be no need
						// to inactivate them again!
						setFilterSpinnersEnabled(false);

						// also inactivate the object filter:
						// as the filterSpinners may have been set previously, want to de-activate the filter
						// based on the previousFilterAttributeIndex (the currently selected index of the
						// filterComboBox):
						removeFilterObjects();
						
						// TODO DO I NEED TO USE previousFilterAttributeIndex?!  I should use the data in
							// the objectFilter object surely!!
						
						deactivateFilterOverClassifier();
					//}
				}
				
				// Give the IWP custom canvas the focus:
				// Ensures the custom canvas has focus after clicking on the filterCheckBox no matter what:
				IWP.cc.requestFocusInWindow();

			}

		});

		filterOverClassifierCheckBox = new JCheckBox("ON class.");

		filterOverClassifierCheckBox.setToolTipText("Check ON to apply Filter output over Classifier Data: Use to quickly turn filter on and off!");

		filterOverClassifierCheckBox.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				// ONLY PERFORM if a classifier is loaded:
				if(objectClassifier.isLoaded() ) {
					// Be sure to run this to run classifyObjects() and adjust the filter over classifier view
					// and arff data as appropriate:
					activateLoadedClassifier();
				}

			}

		});
		
		filterOverClassifierCheckBox.setEnabled(false); // disable by default, only enabled once a Classifier has been selected.

		// set filterCheckBox disabled:
		// activated when a filter is selected.
		// filterCheckBox.setEnabled(false);

		// add combobox to panel - covering 3 cells in width:
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 3;

		filterPanel.add(filterComboBox, gbc);

		gbc.gridwidth = 1; //re-set width back to 1.

		gbc.gridx = 0;
		gbc.gridy = 1;
		filterPanel.add(filterMinLabel, gbc);
		gbc.gridx = 1;
		gbc.gridy = 1;
		filterPanel.add(filterMin, gbc);
		gbc.gridx = 2;
		gbc.gridy = 1;
		filterPanel.add(filterCheckBox, gbc);
		gbc.gridx = 0;
		gbc.gridy = 2;
		filterPanel.add(filterMaxLabel, gbc);
		gbc.gridx = 1;
		gbc.gridy = 2;
		filterPanel.add(filterMax, gbc);
		gbc.gridx = 2;
		gbc.gridy = 2;
		filterPanel.add(filterOverClassifierCheckBox, gbc);

		// Set Border with Title to filterPanel:
		filterPanel.setBorder( BorderFactory.createTitledBorder("Set Object Filter") );


		// Add panel to Obj Manager Panel:
		objManagerPanel.add(filterPanel);

		// Add Vertical JSeparator to objManagerPanel:
		//objManagerPanel.add(Box.createHorizontalStrut(2) );
		//objManagerPanel.add( new JSeparator(SwingConstants.VERTICAL) );
		//objManagerPanel.add(Box.createHorizontalStrut(2) );
		// NOT ADDING THESE - AS the Filter and Classifier Selection Panel will be MERGED.


		// TOGGLE BUTTONS FOR FILTER AND CLASSIFIER:

		// 5. Toggle Buttons for User to turn on and off the Filter and Classifier?

		// ADDED THESE TO THE FILTERPANEL.



		// 6. Import WEKA-trained classifiers:


		// Allow a classifier to be imported, which will be applied to the current data & image:

		// Need a place to set which attribute the class attribute is - manual, filter, classifier.
		// CLASSIFIER CLASS ATTRIBUTE!!

		// Essentially need a File Selector generated from a Button to select the Serialised Classifier file, 
		// a means of toggling the Classifier on and off, and
		// also a way to show the User which Classifier has been selected & whether it is active.


		// When Classifier is Imported:
		// SET each Objects' Classifier Class Attribute to what the Classifier determines it to be.


		// Need a way to check the manual annotation versus the filter or classifier or [filter -> classifier]:

		// CONTINGENCY TABLE -> should be able to see this and save it.
		// Generate as a CSV file of a contingency table using WEKA methods to get the statistics...
		// THIS CAN BE PERFORMED IN WEKA - do not use this here, just use colour coded objects...

		// TODO PLOT and ARFF TABLE of all objects manually annotated - plus the classifier and filter class assignment
		// Filter the plot and arff table to only show correct, or incorrect, class assignments
		// To Achieve This -> Need to have a Class Attribute which encodes ALL Classifier Mode Assignments!
		// Create a new Attribute which encodes Manual Filter and Classifier classification:
		// Use following format:
		//   Mu:Fn:Cn -> Manual unclassified: Filter non-feature : Classifier non-feature.
		//   Mf:Ff:Cf -> Manual Filter & Classifier all feature.
		//   Mc:Ff:Cc -> Manual connected : Filter feature : Classifier connected
		// etc.
		//When this Attribute is used to visualise the ARFF Dataset, every classification across all classifier
		// modes can be visualised -> User can choose to set any of them to be off, or any of them to match
		// colours to allow any data visualisation they want!
		// BASICALLY the same as what was done with the object pixel values, but with a Attribute to allow
		// effective visualisation in WEKA of all object types across all potential classifications
		// (manual, filter and classifier classifications).

		// What does the Classifier do?

		// USED TO DIRECT OBJECT ML DRIVEN ANNOTATION:

		// The Classifier is incrementally improved through the following cycle:

		// OM Object Classification -> Build Weka Classifier -> Classifier-Driven Object Annotation -> etc.

		// With each round, the Classifier built in Weka must be IMPORTED into OM & then used to DRIVE OBJECT
		// SELECTION for manual Object Classification.

		// THEREFORE -> If a Classifier has been imported, must USE THIS CLASSIFIER to calculate the likelihood
		// of UNCLASSIFIED Objects being in the FEATURE, NON-FEATURE or CONNECTED Classes.

		// selectRandomObjects() selectLinearObjects() selectGaussianObjects()

		// splitArff()
		// this creates the classified, unclassified and unclassifiedFirstPix ARFF datasets:
		// instance based filtering where objects which are classified in MAN CLASS are put into
		// classified, and objects which are unclassified in MAN CLASS are put into unclassified
		// and unclassifiedFirstPix
		// NO NEED TO APPLY CLASSIFIER TO INSTANCE BASED FILTERING HERE:
		// The Classifier will have set values for both manually classified and unclassified objects,
		// but this should not be taken into account here as only want to use Classifier to help
		// SELECT the correct unclassified objects, not filter them away!

		// filterAndBuildClassifier()
		// This will filter the Attributes - which will depend on the Attributes the Classifier uses!
		// For the Rough ML Classifier, the attributes are set to only Size and Sphericity.
		// However, for imported classifier want to use ITS Attributes!
		// A boolean Array currently dictates which attributes to keep and discard - classifierAttributes.
		// it would be much easier to filter attributes based on an Instances object which contains
		// all the attributes!

		// setClassifierAttributesRemoval()
		// set classifierAttributes


		// USED TO ADJUST PIXEL VALUES TO VISUALISE HOW THE CLASSIFIER IS FILTERING OBJECTS IN THE Z STACK:


		// SAVED WITH THE OM OUTPUT - TO BE APPLIED TO DATA IN THE SM ANALYSER PLUGIN:


		// Supply:

		// JTextField - uneditable, which gives the classifier file name selected.
		// JButtons - one to select a Classifier File, one to remove the currently selected Classifier.
		// JPanel to hold these Components on.
		// A boolean value - classifierLoaded - which indicates whether a classifier is selected.

		//Make Panel:
		classifierPanel = new JPanel();

		//set layout:
		classifierPanel.setLayout( new GridBagLayout() );
		gbc = new GridBagConstraints();

		// set fill and weights for all:
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 0.5;
		gbc.weighty = 0.5;

		//set the boolean classifierLoaded to FALSE:
		// classifierLoaded = false;

		// make Textfield, 12 columns, blank text:
		classifierTextField = new JTextField("", 12);

		// make uneditable and unselectable:
		classifierTextField.setFocusable(false);
		classifierTextField.setEditable(false);

		// Make select and delete buttons for classifier:
		classifierSelectButton = new JButton("Select");
		classifierDeleteButton = new JButton("Remove");

		// Set tool tip for buttons:
		classifierSelectButton.setToolTipText("Select a Classifier from the File System");
		classifierDeleteButton.setToolTipText("Remove the selected Classifier");

		// Set select & delete button to disabled initially- 
		// activate Select Button when checkbox selected
		// activate Delete Button when classifier selected via Select button!
		classifierSelectButton.setEnabled(false);
		classifierDeleteButton.setEnabled(false);

		// Add Listeners to the Buttons:
		classifierSelectButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				
				IWP.deactivateButtons();
				IWP.deactivateButtons(objManagerPanel);
				
				// open a file dialog to select a Classifier file from disk:
				File classifierFile = objectClassifier.loadClassifier(dw.getOutputParentFile(), IWP.getWindow() );

				// only perform remainder of tasks if a VALID CLASSIFIER has been selected:
				if(classifierFile != null) {
					// AND set the Classifier name to the Textfield:
					classifierTextField.setText( classifierFile.getName() );

					// If a VALID CLASSIFIER is Selected, set classifierLoaded to TRUE:
					// classifierLoaded = true;

					// AND activate the classifierDeleteButton:
					classifierDeleteButton.setEnabled(true);

					// sets classifierLoaded to true, activates classifierDeleteButton, resets CLASS_INDEX,
					// and classifies the objects with the current classifier:
					//activateLoadedClassifier();
					// this contains following method calls:
					//classifierDeleteButton.setEnabled(true);
					// IWP.cc.requestFocusInWindow();
					
					unselectAnyObject();

					classifyObjects(); // break this up ???  I cannot run this only in objectClassifier,
					// as it requires datasetHandler & imageHandler

					// objectClassifier.classifyObjects(datasetHandler, filterOverClassifierCheckBox.isSelected() );

					// enable the classifierViewButton:
					classifierViewButton.setEnabled(true);
					
					// activate the classifierViewButton:
					classifierViewButton.doClick();
					
					activateFilterOverClassifierCheckBox();
					
					// enable the Object Selection High and Low Spinners -> so these values can be selected by
						// user for the object selection:
					adjustObjSelRandSpinners(true);

				}
				
				IWP.reactivateButtons();
				IWP.reactivateButtons(objManagerPanel);
				
				// put focus onto canvas on IWP:
				IWP.cc.requestFocusInWindow();

			}

		});

		classifierDeleteButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
								
				// If clicked, need to remove the content of the Textfield:
				classifierTextField.setText("");

				// remove the classifier file to indicate the classifier has been deleted:
				objectClassifier.removeClassifierFile();
				
				// if the classifier view button is currently selected, ensure another view button is checked
				// check the manual view button by default, as this button will never be de-activated:
				if(classifierViewButton.isSelected() == true) {
					manualViewButton.doClick();
				}

				// disable the classifierViewButton:
				classifierViewButton.setEnabled(false);

				// deactivate the loaded classifier - from the image and dataset:
				deactivateLoadedClassifier();
				
				// filterOverClassifierCheckBox.setEnabled(false);
				// instead run the deactiveFilterOverClassifier():
				deactivateFilterOverClassifier();
				
				// And adjust Object Selection spinners, to off if the stats say so:
				adjustObjSelRandSpinners(manClassStats);


			}

		});

		classifierCheckBox = new JCheckBox("Classifier");

		classifierCheckBox.setToolTipText("Check ON to activate Classifier, check OFF to de-activate Classifier");

		classifierCheckBox.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				// Set the filterViewButton to enabled if checked on, or disabled if check off:
				if(classifierCheckBox.isSelected() == true) {

					// enable the classifierViewButton:
					classifierViewButton.setEnabled(true);

					// enable select button:
					classifierSelectButton.setEnabled(true);

					// if the loadedClassifier is loaded need to relaod the components for this:
					if(objectClassifier.loadClassifier() != null ) {
						// Returns a non-null file if classifier successfully loaded

						filterOverClassifierCheckBox.setEnabled(true);

						// first, set the classifier and classifierAttributes to the loaded refs:
						// classifier = loadedClassifier;
						// classifierAttributes = loadedClassifierAttributes;

						// objectClassifier.setLoadedClassifierAndAttributesToLoaded();

						// sets classifierLoaded to true, activates classifierDeleteButton, resets CLASS_INDEX,
						// and classifies the objects with the current classifier:
						activateLoadedClassifier();
						
						activateFilterOverClassifierCheckBox();

						// activate the classifierViewButton:
						classifierViewButton.doClick();

					}					
				}
				else {

					// if the classifier view button is currently selected, ensure another view button is checked
						// check the manual view button by default, as this button will never be de-activated:
					if(classifierViewButton.isSelected() == true) {
						manualViewButton.doClick();
					}
					
					// disable the classifierViewButton:
					classifierViewButton.setEnabled(false);
					
					// disable classifier select button too:
					classifierSelectButton.setEnabled(false);
					
					filterOverClassifierCheckBox.setEnabled(false);

					// deactivate the loaded classifier - from the image and dataset:
					deactivateLoadedClassifier();
					
					// filterOverClassifierCheckBox.setEnabled(false);
					// instead run the deactiveFilterOverClassifier():
					deactivateFilterOverClassifier();
					
				}

				// Give the IWP custom canvas the focus:
				// Ensures the custom canvas has focus after clicking on the filterCheckBox no matter what:
				IWP.cc.requestFocusInWindow();

			}

		});

		// set classifierCheckBox disabled:
		// activated when a classifier is selected.
		// classifierCheckBox.setEnabled(false);

		// Add components to the classifier panel:
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		classifierPanel.add(classifierTextField, gbc);

		gbc.gridwidth = 1;

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridheight = 2;
		classifierPanel.add(classifierSelectButton, gbc);

		gbc.gridheight = 1;

		gbc.gridx = 1;
		gbc.gridy = 1;
		classifierPanel.add(classifierCheckBox, gbc);

		gbc.gridx = 1;
		gbc.gridy = 2;
		classifierPanel.add(classifierDeleteButton, gbc);

		// Set Border with Title to classifierPanel:
		classifierPanel.setBorder( BorderFactory.createTitledBorder("Import Object Classifier"));

		// Add classifier Panel to objManager Panel:
		objManagerPanel.add(classifierPanel);


		// FILTER / CLASSIFIER VISUAL CHECK -> Plots and Object Image Visualisation

		// Below components are all on the same Panel:  optionsPanel

		optionsPanel = new JPanel();

		//set layout:
		optionsPanel.setLayout( new GridBagLayout() );

		gbc = new GridBagConstraints();

		// set fill and weights for all:
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 0.5;
		gbc.weighty = 0.5;


		// 7. Applying Filter / Classifier to remaining images in input:

		// The application of the Filter / Classifier can then be checked in the image, 
		// and in the ARFF VIEWER & PLOT Frames.
		// This will happen with toggling through the images in the input, using the same structure as the 
		// Threshold_Manager...

		// Add the Image Scroll Arrows as are present in the ThresholdManager:

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

						IWP.hide();

						IJ.log("");
						IJ.log("");
						IJ.log("Removing Current Image:" + imageHandler.imp.getTitle());

						IJ.log("");
						IJ.log("    Saving Dataset...");

						// FIRST -> SAVE ARFF of current image:
						//saveArffDataAfterProcessMethod();
						// datasetHandler.saveData(arffPath, classifierAttributesNoFilterNoClassifier); //this method uses the arffPath to save data to correct output.
						objectDataset.saveData(arffPath, ObjectDataContainer.returnObjectMeasuresAndManClassAttributes() ); //this method uses the arffPath to save data to correct output.
						saveManualClassificationData(); // save the manual classification data too!

						// save the OM_ProcedureSettings file - max dimensions and Filter settings:
						saveOM_ProcedureSettingsFile();

						// The Classifier (if selected) is saved via saveClassifierFile():
						saveClassifierFile();

						//When a new image is brought in, should automatically apply any image processing steps from
						//procedureStack to its first channel.

						//IJ.showMessage("current file index B4 inc: "+dw.getCurrentFileIndex() );
						//IJ.showMessage("total file count B4 inc: "+dw.getTotalFileCount() );

						//Retrieve the imp for the NEXT image:

						//first, increase both currentFileIndex and TotalFileCount by 1:
						//dw.incrementCurrentFileIndex();
						dw.incrementTotalFileCountAndCurrentFileIndex();
						//this increments totalFileCount and currentFileIndex but in a way to ensure currentFileIndex
						//is ALWAYS less than totalFileCount!

						//IJ.showMessage("current file index: "+dw.getCurrentFileIndex() );
						//IJ.showMessage("total file count: "+dw.getTotalFileCount() );
						//This is required to increment totalFileCount, otherwise currentFileIndex will be too high,
						//and the algorithm will terminate!
						//There will still be no more images after this one, and the process() method will not grab any more
						//imps from DW as the totalFileCount has been artificially set...

						//IJ.showMessage("[for manClassBuffer index] DW Current File Index is: "+dw.getCurrentFileIndex() );

						// Set the index on the Manual Classification arff buffer:
						//manClassArffBuffer.setBufferIndex( dw.getCurrentFileIndex() );
						//manClassArffBuffer.setBufferString( dw.getCurrentFile(0).getName() );
						manClassArffBuffer.setBufferString( dw.getCurrentFile(0).getName().substring(0, 
								dw.getCurrentFile(0).getName().lastIndexOf(".")) );

						//IJ.showMessage("Remove Listeners:");

						IJ.log("");
						IJ.log("    Shutting Down Image...");

						// Remove all Listeners from objects in the current IWP:
						// Prevents Memory Leak! 
						removeListeners();

						//IJ.showMessage("Remove Listeners: Complete.");

						IJ.log("");
						IJ.log("    Opening Next Image...");

						// then, just retrieve the currentImp on fileselector 0:
						// This is SLOW for large images, or images retrieved from a server or other slow access place
						ImagePlus imp = dw.getCurrentImp(0);
						thresholdPath = dw.getCurrentOutputFile().getAbsolutePath() +".tif";

						IJ.log("");
						IJ.log("");
						IJ.log("Assessing Image: "+imp.getTitle() );

						IJ.showStatus("Object Manager: Applying Threshold"); 

						IJ.log("");
						IJ.log("    Applying Threshold Procedure Stack...");

						//generate new IWP & activeImp:

						// first, CLEAR the object selection in instance variables if it exists:
						selectedObj = new SelectedObject(0,0,0, false, 0, 0);

						shutdownIWP();

						//imageHandler = new ImageHandler(imp, procedureStack, om_ProcedureSettings.getObjectConnectivity() );
						imageHandler = new ImageHandler(originalImp, procedureStack, 
															om_ProcedureSettings.getObjectConnectivity(), thresholdPath );

						// generate the new thresholdImp from the new imp:
						// thresholdImp = applyThresholdProcedureStack(imp, procedureStack);

						IJ.showStatus("Object Manager: Assessing Objects");

						IJ.log("");
						IJ.log("    Setting up IWP...");

						//String arffPath = dw.getCurrentOutputFileNoPause().getAbsolutePath() +".arff";
						// Do not use NoPause as now the currentoutputfile index is in sync as if the process() method
						// were paused!
						arffPath = dw.getCurrentOutputFile().getAbsolutePath() +".arff";

						//IJ.showMessage("arff path new image: "+arffPath);

						// initialise the manClassStats obj:
						manClassStats = new ClassificationStats();

						assessObjects(arffPath);


						IWP = new ImageWindowWithPanel(imp, new Panel(), imageHandler.thresholdImp, procedureStack.bitDepth, false );
						// Passed FALSE at the end to ensure the new channel is displayed and toggle is available on IWP.
						// Adjust this so the last imp is only turned ON by a method called in IWP -> want to turn the channel
						// on after having set the LUT on it (which is done below!).

						// Note, this can be controlled by manipulating the channelCheckBox array, 'channels'.
						// Specifically, calling channels[index].stateChanged() will switch between displaying and not displaying
						// the channel.  Or the setState(boolean) method can be used to modify state and checkbox together.
						// ALSO -> bitDepth indicates the image will be converted to bitDepth at point of opening in IWP.
						// This may be 8, 16, 32 -> set by user in DialogWindow.


						// Set active channel to last channel - the one NOT DISPLAYED & which will contain the processed imp:
						IWP.setActiveChannel( IWP.getChannels() );

						//set projection to be from slice 1 to slice 10:
						IWP.setSliceProjection( sliceProjectionArray );


						// apply the manual LUT to this last channel on both projected and original imp:
						setIwpLut( classModeLUTs.manualLut );


						// Setting the close operation to include saving the current Arff Dataset:
						IWP.iw.addWindowListener( new WindowAdapter() {
							@Override
							public void windowClosing(WindowEvent e) {
								super.windowClosing(e); // retain any code which is called before.
								//saveArffDataAfterProcessMethod(); // and save the current ARFF Dataset to relevant location.
								// datasetHandler.saveData(arffPath, classifierAttributesNoFilterNoClassifier); //this method uses the arffPath to save data to correct output.
								objectDataset.saveData(arffPath, ObjectDataContainer.returnObjectMeasuresAndManClassAttributes() ); //this method uses the arffPath to save data to correct output.
								saveManualClassificationData(); // save the manual classification data too!

								removeListeners();
								IWP.iw.removeWindowListener( this );

								shutdownOM();

								shutdownDW();

								shutdownIWP();

							}
						});

						//IJ.showMessage(""+dw.getCurrentFileIndex()+" / "+dw.getTotalFileCount()+" - "+dw.getCurrentFile(0).getName()+" : "+dw.getCurrentFileRelativeToInput(0).getParent() );
						// Set the IWP window title:  [ImgIndex] / [TOTAL INDEX] - [image_title] : [Path_Below_Input_Dir]
						IWP.getImagePlus().setTitle(""+dw.getCurrentFileIndex()+" / "
								+dw.fileSelectors.get(0).inputOutputFramework.fileArray.size()+" - "
								+dw.getCurrentFile(0).getName()+" : "
								+dw.getCurrentFileRelativeToInput(0).getParent() );
						IWP.getOriginalImagePlus().setTitle(""+dw.getCurrentFileIndex()+" / "
								+dw.fileSelectors.get(0).inputOutputFramework.fileArray.size()+" - "
								+dw.getCurrentFile(0).getName()+" : "
								+dw.getCurrentFileRelativeToInput(0).getParent() );

						// get output file name for PNG saving - 
						// do not include .png extension, this is done in the save image listener:
						pngPath = dw.getCurrentOutputFile().getAbsolutePath();

						// And initialise the png index (for adding to image file name for multi-saving of PNG files):
						pngIndex = 0;

						// Setup the object manager -> all buttons and components + listeners on the IWP panel for Threshold setup:
						setupObjectManager();

						// Set the Filter and Classifier components to reflect the settings on the previous image:
						// set the settings and if needed filter/classify the data:

						// DOES setupObjectManager() really have to be called here?
						// Do all the components need to be re-made, or can they be re-used?
						// If so, they should retain their current values - this may be a quick way to set up
						// the image with the same settings!
						// NOTE:  The Filter MUST be checked to see if low pass is set to max, as this max value
						// may not be the same in the next image, and therefore may need adjusting.

						// Set the Filter and Classifier components to reflect the settings on the previous image:
						setFilter();
						setClassifier();


						// save the images' ARFF Dataset - MINUS the Filter and Classifier Columns:
						//saveArffDataDuringProcessMethod();
						// datasetHandler.saveData(arffPath, classifierAttributesNoFilterNoClassifier); //this method uses the arffPath to save data to correct output.
						objectDataset.saveData(arffPath, ObjectDataContainer.returnObjectMeasuresAndManClassAttributes() ); //this method uses the arffPath to save data to correct output.

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


						IWP.hide();

						IJ.log("");
						IJ.log("");
						IJ.log("Removing Current Image:" + imageHandler.imp.getTitle());

						IJ.log("");
						IJ.log("    Saving Dataset...");

						// FIRST -> SAVE ARFF of current image:
						//saveArffDataAfterProcessMethod();
						// datasetHandler.saveData(arffPath, classifierAttributesNoFilterNoClassifier); //this method uses the arffPath to save data to correct output.
						objectDataset.saveData(arffPath, ObjectDataContainer.returnObjectMeasuresAndManClassAttributes() ); //this method uses the arffPath to save data to correct output.

						saveManualClassificationData(); // save the manual classification data too!

						// save the OM_ProcedureSettings file - max dimensions and Filter settings:
						saveOM_ProcedureSettingsFile();

						// The Classifier (if selected) is saved via saveClassifierFile():
						saveClassifierFile();

						// IJ.showMessage("before new IWP - maxX: "+maxX);

						//When a new image is brought in, should automatically apply any image processing steps from
						//procedureStack to its first channel.

						//IJ.log("current file index B4 inc: "+dw.getCurrentFileIndex() );
						//IJ.log("total file count B4 inc: "+dw.getTotalFileCount() );

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

						//IJ.showMessage("[for manClassBuffer index] DW Current File Index is: "+dw.getCurrentFileIndex() );

						// Set the index on the Manual Classification arff buffer:
						//manClassArffBuffer.setBufferIndex( dw.getCurrentFileIndex() );
						//manClassArffBuffer.setBufferString( dw.getCurrentFile(0).getName() );
						manClassArffBuffer.setBufferString( dw.getCurrentFile(0).getName().substring(0, 
								dw.getCurrentFile(0).getName().lastIndexOf(".")) );

						IJ.log("");
						IJ.log("    Shutting Down Image...");

						// Remove all Listeners from objects in the current IWP:
						removeListeners();

						IJ.log("");
						IJ.log("    Opening Previous Image...");

						//then, just retrieve the currentImp on fileselector 0:
						ImagePlus imp = dw.getCurrentImp(0);
						thresholdPath = dw.getCurrentOutputFile().getAbsolutePath() +".tif";

						IJ.log("");
						IJ.log("");
						IJ.log("Assessing Image: "+imp.getTitle() );

						IJ.showStatus("Object Manager: Applying Threshold"); 

						IJ.log("");
						IJ.log("    Applying Threshold Procedure Stack...");

						//generate new IWP & activeImp:

						// first, CLEAR the object selection in instance variables if it exists:
						selectedObj = new SelectedObject(0,0,0, false, 0, 0);

						shutdownIWP(); // do NOT shutdown DW -> need dialogWindow for paths to images!

						//imageHandler = new ImageHandler(imp, procedureStack, om_ProcedureSettings.getObjectConnectivity() );
						imageHandler = new ImageHandler(originalImp, procedureStack, 
															om_ProcedureSettings.getObjectConnectivity(), thresholdPath );

						
						//thresholdImp = applyThresholdProcedureStack(imp, procedureStack);

						IJ.showStatus("Object Manager: Assessing Objects");

						IJ.log("");
						IJ.log("    Setting up IWP...");

						//String arffPath = dw.getCurrentOutputFileNoPause().getAbsolutePath() +".arff";
						// Do not use NoPause as now the currentoutputfile index is in sync as if the process() method
						// were paused!
						arffPath = dw.getCurrentOutputFile().getAbsolutePath() +".arff";

						//IJ.showMessage("arff path prev. image: "+arffPath);

						// initialise the manClassStats obj:
						manClassStats = new ClassificationStats();

						assessObjects(arffPath);


						IWP = new ImageWindowWithPanel(imp, new Panel(), imageHandler.thresholdImp, procedureStack.bitDepth, false );
						// Passed FALSE at the end to ensure the new channel is displayed and toggle is available on IWP.
						// Adjust this so the last imp is only turned ON by a method called in IWP -> want to turn the channel
						// on after having set the LUT on it (which is done below!).

						// Note, this can be controlled by manipulating the channelCheckBox array, 'channels'.
						// Specifically, calling channels[index].stateChanged() will switch between displaying and not displaying
						// the channel.  Or the setState(boolean) method can be used to modify state and checkbox together.
						// ALSO -> bitDepth indicates the image will be converted to bitDepth at point of opening in IWP.
						// This may be 8, 16, 32 -> set by user in DialogWindow.


						// Set active channel to last channel - the one NOT DISPLAYED & which will contain the processed imp:
						IWP.setActiveChannel( IWP.getChannels() );

						//set projection to be from slice 1 to slice 10:
						IWP.setSliceProjection( sliceProjectionArray );


						// Make the manual LUT of this last channel on both projected and original imp:
						setIwpLut( classModeLUTs.manualLut );


						// Setting the close operation to include saving the current Arff Dataset:
						IWP.iw.addWindowListener( new WindowAdapter() {
							@Override
							public void windowClosing(WindowEvent e) {
								super.windowClosing(e); // retain any code which is called before.
								//saveArffDataAfterProcessMethod(); // and save the current ARFF Dataset to relevant location.
								// datasetHandler.saveData(arffPath, classifierAttributesNoFilterNoClassifier); //this method uses the arffPath to save data to correct output.
								objectDataset.saveData(arffPath, ObjectDataContainer.returnObjectMeasuresAndManClassAttributes() ); //this method uses the arffPath to save data to correct output.

								saveManualClassificationData(); // save the manual classification data too!

								// shutdown OM:
								removeListeners();

								// remove THIS listener from IWP:
								IWP.iw.removeWindowListener( this );

								shutdownOM();

								shutdownDW();

								shutdownIWP();

							}
						});

						//IJ.showMessage(""+dw.getCurrentFileIndex()+" / "+dw.getTotalFileCount()+" - "+dw.getCurrentFile(0).getName()+" : "+dw.getCurrentFileRelativeToInput(0).getParent() );
						// Set the IWP window title:  [ImgIndex] / [TOTAL INDEX] - [image_title] : [Path_Below_Input_Dir]
						IWP.getImagePlus().setTitle(""+dw.getCurrentFileIndex()+" / "
								+dw.fileSelectors.get(0).inputOutputFramework.fileArray.size()+" - "
								+dw.getCurrentFile(0).getName()+" : "
								+dw.getCurrentFileRelativeToInput(0).getParent() );
						IWP.getOriginalImagePlus().setTitle(""+dw.getCurrentFileIndex()+" / "
								+dw.fileSelectors.get(0).inputOutputFramework.fileArray.size()+" - "
								+dw.getCurrentFile(0).getName()+" : "
								+dw.getCurrentFileRelativeToInput(0).getParent() );

						// get output file name for PNG saving - 
						// do not include .png extension, this is done in the save image listener:
						pngPath = dw.getCurrentOutputFile().getAbsolutePath();

						// And initialise the png index (for adding to image file name for multi-saving of PNG files):
						pngIndex = 0;

						// Setup the object manager -> all buttons and components + listeners on the IWP panel for Threshold setup:
						setupObjectManager();

						// Set the Filter and Classifier components to reflect the settings on the previous image:
						setFilter();
						setClassifier();

						// save the images' ARFF Dataset - MINUS the Filter and Classifier Columns:
						//saveArffDataDuringProcessMethod();
						// datasetHandler.saveData(arffPath, classifierAttributesNoFilterNoClassifier); //this method uses the arffPath to save data to correct output.
						objectDataset.saveData(arffPath, ObjectDataContainer.returnObjectMeasuresAndManClassAttributes() ); //this method uses the arffPath to save data to correct output.

						// IJ.showMessage("After new IWP - maxX: "+maxX);

					}

				};

				t.start();

			}

		});



		// EXPORT OBJECT FILTER / CLASSIFIER, DATA, IMAGES:  all onto exportPanel

		exportPanel = new JPanel();

		//set layout:
		exportPanel.setLayout( new GridBagLayout() );


		// 8. Export the Object Filter -> including max/min obj size, Any Obj Filters, Any Obj Classifiers:

		// Add a Button to allow the User to SAVE the Object Filter:

		exportOMDataButton = new JButton("Export");

		exportOMDataButton.setToolTipText("Export OM Data: Manual Class. data, classifier, filter & thresholdStack..");

		exportOMDataButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Need to export the Object Filter as is currently laid out in the Object Manager:

				// This will consist of:
				// 1. Dimensions of Max Object Size, to be used for Exclusion Zone Dimensions in SM Analyser
				// 2. Any Filter Applied -> Based on one Attribute, with low- and high- pass values.
				// 3. Any Classifier Applied -> This is only the imported Classifiers
				// 4. Threshold Procedure Stack -> This is kept with the Object Filter, so the User does not have
				// to recall which Threshold Procedure Stack sas used with which Object Filter.
				// Also makes it necessary to run the Object Filter even if no Filter or Classifier is applied,
				// Which is required to also get the Dimensions for Exclusion Zones.

				// Save this all to an XML file for recall in SM Analyser.

				// Threshold Procedure Stack is saved to OM_MetaData at the start of the plugin, so do not need to
				// re-save.

				// The Dimensions of Max Object & the applied Filter are saved via saveOM_ProcedureSettingsFile():
				saveOM_ProcedureSettingsFile();

				// The Classifier (if selected) is saved via saveClassifierFile():
				saveClassifierFile();

				// DONT FORGET to save the Arff and manual classification Arff files
				// important to add this so user can click Export to get the ManualClassificationData.arff file!
				// datasetHandler.saveData(arffPath, classifierAttributesNoFilterNoClassifier); //this method uses the arffPath to save data to correct output.
				objectDataset.saveData(arffPath, ObjectDataContainer.returnObjectMeasuresAndManClassAttributes() ); //this method uses the arffPath to save data to correct output.

				saveManualClassificationData(); // save the manual classification data too!
				
				IJ.showMessage("Export Complete", 
								"ManualClassificationData.xml, OM_classifier.model, \n"+
								"   OM_ProcedureSettings.xml & thresholdStack.xml \n"+
								"           exported to OM_MetaData.");

			}

		});


		// 9. Export Obj Attributes & Classification Data -> CSV or ARFF format (for plotting data):

		// Performed Above:  It is only necessary to export the data which is also manually classified?
		// Would a User ever want to just have the Classifier or Filter classification data for all data point,
		// even if this was NOT manually classified?
		// Could get idea of total number of objects filtered or classified as non-feature, connected?

		// DOES THE USER WANT TO FIND OBJECTS BASED ON THEIR CLASSIFICATION BY THE FILTER OR CLASSIFIER?
		// Much like selecting an object based on a Rough Classifier?
		// The Rough Classifier and Imported Classifier both give probabilities of each object belonging to a
		// given class.
		// Now, the Object Selection based on Class Probabilities has a Combo Box which can be used to select
		// which CLASS PROBABILITY the object selection is based on -> Feature, Non-Feature, Connected


		// 10. Save Images -> Raw, Thresholded, Classifier Objects:

		// Add a Button to allow the User to SAVE the Image.

		saveImageButton = new JButton("SAVE");

		saveImageButton.setToolTipText("Save the currently image projection or z stack to the FileSystem.");
		
		saveImageButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				saveImageFrame = new JFrame();

				JPanel p = new JPanel();

				p.setLayout(new GridBagLayout());
				GridBagConstraints gbc = new GridBagConstraints();

				// set fill and weights for all:
				gbc.fill = GridBagConstraints.BOTH;
				gbc.weightx = 0.5;
				gbc.weighty = 0.5;


				saveImageOKButton = new JButton("OK");

				saveImageOKButton.addActionListener( new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {

						// TODO Want to save the image just as it is displayed in the Canvas.

						if(saveImageZStackCheckbox.isSelected() ) {
							// save the whole Z Stack, all channels:
							IJ.save( IWP.getOriginalImagePlus(), saveImageFile.getParent()+File.separator+saveImageTitleTextfield.getText() );
						}
						else {
							// save the current projected image on the ImageWindow, including current active channels:
							IJ.save( IWP.getImagePlus(), saveImageFile.getParent()+File.separator+saveImageTitleTextfield.getText() );
						}
						
						
						//Save as PNG by default - this saves exactly what is in the Canvas, colours and projection, as a
						// single image.
						// IJ.save(pngIndex + "_" + pngPath + ".png");

						// and increment the pngIndex - to incremenet file name for next png image save:
						pngIndex = pngIndex + 1;
						
						saveImageFrame.dispose();
						
					}

				});

				saveImageTitleLabel = new JLabel("Image Title:");
				
				saveImageFile = new File(pngPath);
				saveImageTitleTextfield = new JTextField();
				saveImageTitleTextfield.setText( saveImageFile.getName() + "_" + pngIndex + ".png");
				
				saveImageZStackLabel = new JLabel("Save Z Stack:");
				
				saveImageZStackCheckbox = new JCheckBox();
				
				// If the checkbox is checked, and the textfield extension is NOT tif, set it to TIF:
				saveImageZStackCheckbox.addActionListener( new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						if(  (saveImageZStackCheckbox.isSelected() ) && 
							!(saveImageTitleTextfield.getText().substring(
								saveImageTitleTextfield.getText().length()-3, 
								saveImageTitleTextfield.getText().length() ).equals("tif") )  ) {
							
							saveImageTitleTextfield.setText(
									saveImageTitleTextfield.getText().substring(
											0, 
											saveImageTitleTextfield.getText().length()-3 ) + "tif");
						}
					}
					
				});
				
				gbc.gridx = 0;
				gbc.gridy = 0;
				p.add(saveImageTitleLabel, gbc);
				gbc.gridx = 1;
				gbc.gridy = 0;
				p.add(saveImageTitleTextfield, gbc);
				
				gbc.gridx = 0;
				gbc.gridy = 1;
				p.add(saveImageZStackLabel, gbc);
				gbc.gridx = 1;
				gbc.gridy = 1;
				p.add(saveImageZStackCheckbox, gbc);
				
				gbc.gridx = 1;
				gbc.gridy = 2;
				p.add(saveImageOKButton, gbc);
				

				saveImageFrame.setContentPane(p);
				saveImageFrame.pack();
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize(); // set location to centre of screen
				saveImageFrame.setLocation(
						(screenSize.width/2)-(saveImageFrame.getWidth()/2), 
						(screenSize.height/2)-(saveImageFrame.getHeight()/2) );
				saveImageFrame.setVisible(true);

			}

		});


		// Add Buttons to exportPanel:

		gbc.gridx = 0;
		gbc.gridy = 0;

		exportPanel.add(exportOMDataButton, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;

		exportPanel.add(saveImageButton, gbc);


		// 11. OPTIONS & HELP BUTTONS: helpOptionsPanel used to hold these components.

		helpOptionsPanel = new JPanel();

		//set layout:
		helpOptionsPanel.setLayout( new GridBagLayout() );


		// Options allow adjustment of what attributes are recorded, and which ones are used for the ML directed
		// selection of objects for manual object classification

		// OTHER OPTIONS?!

		// MAX Number of Objects to be selected by Random Button

		// Object Colours for FEATURE NONFEATURE CONNECTED UNCLASSIFIED and SELECTED

		// Default Rough Classifier Used

		// Attributes which this Rough Classifier is supposed to use.

		// Filter Options - if filter is ON, option whether ML or USER Directed Obj Annotation is only performed
		// on objects which pass the filter, OR on ALL OBJECTS?  Default is on objects which pass the filter

		// Linear Int:  Set the Linear Number of Objects Selected.

		// Gaussian String - Ability to set the String: 1,2,4,2,1 to any set of integers separated by commas.

		// Slice Projection Options - what should the initial slice projection values be?

		// Saving/Loading Arff Data for each Image -> User can select option to allow ALL data to be loaded when
		// plugin is initialised, or data to be loaded as each image is loaded??

		optionsButton = new JButton("Options");

		optionsButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Bring up the Options for this Plugin, and allow the user to edit them.

				// When complete these should be updated for the current image and settings.

			}

		});


		// Help will include information on keyboard shortcuts for user to refer to, plus links to further
		// documentation.

		helpButton = new JButton("Help");

		helpButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Provides a useful help screen, giving an overview of the keyboard shortcuts and how to use
				// the plugin, and where to find more extensive help.

			}

		});



		// Add Buttons to helpOptionsPanel:

		gbc.gridx = 0;
		gbc.gridy = 0;

		helpOptionsPanel.add(optionsButton, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;

		helpOptionsPanel.add(helpButton, gbc);



		// Add all components to the optionsPanel:

		gbc.gridx = 0;
		gbc.gridy = 0;

		optionsPanel.add(previousButton, gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;

		optionsPanel.add(nextButton, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;

		optionsPanel.add(exportPanel, gbc);

		gbc.gridx = 1;
		gbc.gridy = 1;

		optionsPanel.add(helpOptionsPanel, gbc);


		// Add classifier Panel to objManager Panel:
		objManagerPanel.add(optionsPanel);

		// Add objManagerPanel to IWP's panel: Using scrollPane to hold objManagerPanel, want to have scrollbars
		// if the Panel is too long for the Window!
		// I have currently BODGED THIS:
		// Set the preferredSize of scrollPane to be 120 high by [maxWidth]-200.
		// -200 as there will be both a Z projector pane and Channel Pane, and this accounts for their width
		// This does NOT dynamically create a scrollpane if image is resized.
		// To properly implement this would need to apply scollpane to IWP panel, and ensure its preferred size
		// was set to [maxWidth], and that this width value was changed if the image window was changed.

		// For now, the bodged version will do!  As long as the user does not resize the window, which they have no
		// reason to do!

		//objManagerPanel.setPreferredSize(new Dimension(1400, 120));
		//IWP.panel.setPreferredSize(new Dimension(1400, 120));
		Rectangle rect = GUI.getMaxWindowBounds();
		scrollPane.setPreferredSize( new Dimension((int)(rect.getWidth()-200),120));

		IWP.addComponent(scrollPane);
		//IWP.addComponent(objManagerPanel);

		//layout the IWP:
		IWP.layoutWindow();

	}


	/**
	 * Saves the OM_Procedures file to the OM_MetaData Directory.  OM_Procedures stores the max 95% dimensions across
	 * all objects in all input images assessed (for applying the DiSector logic), and the data on the high/low
	 * pass filter (the Attribute Index [where index 0 is blank], and the filterMin and filterMax values).
	 */
	public void saveOM_ProcedureSettingsFile() {
		
		// save the filter settings: Avoid the bug where filterMax is eliminated if its max value
			// exceeds the value in the current image!
		// first get the original filter values:
		int origFilterIndexVal = om_ProcedureSettings.getFilterIndexValue();
		double origFilterMin = om_ProcedureSettings.getFilterMinValue();
		double origFilterMax = om_ProcedureSettings.getFilterMaxValue();
		boolean origFilterMaxReached = om_ProcedureSettings.isMaxValReached();
		
		// MVOE ALL THIS LOGIC TO OBJECT FILTER CLASS!!
		
		// to avoid the edge case where the filter HAS been set previously, BUT the current max val does not
			// allow the max filter to reach it, check the values of origFilterMaxReached and current filterMaxReached:
		if(origFilterMaxReached == false && objectFilter.filterMaxReached == true && origFilterIndexVal == getFilterIndexValue()) {
			// then in the last image, the max filter was SET, and that value is now TOO BIG for current image
				// Therefore, DO NOT OVERWRITE THE FilterMax Settings: filterMaxVal or filterMaxReached
				// But can overwrite the filterMinVal
			// THIS ASSUMES that no other changes have occurred to the filter - that the same attribute is filtered
				// for example - check this too! Have added this to the if clause
			// only need to change filterMinVal if its been altered:
			if(origFilterMin != (double)filterMinModel.getValue() ) {
				//om_ProcedureSettings.set
			}
			
		}
		
		om_ProcedureSettings.setFilterValues(
						getFilterIndexValue(), 
						(double)filterMinModel.getValue(), 
						(double)filterMaxModel.getValue(),
						objectFilter.filterMaxReached
						);
		
		if(filterOverClassifierCheckBox.isSelected()) {
			om_ProcedureSettings.setApplyFilterOverClassifier( OM_ProcedureSettings.FILTEROVERCLASSIFIER );
		}
		else {
			om_ProcedureSettings.setApplyFilterOverClassifier( OM_ProcedureSettings.NOFILTEROVERCLASSIFIER );
		}

		// om_ProcedureSettings.filterIndexVal.setContent( getFilterIndexValue() );

		// filter min val:
		// filterMinVal = (double)filterMinModel.getValue();

		// om_ProcedureSettings.filterMinVal.setContent( (double)filterMinModel.getValue() );

		// filter max val:
		// filterMaxVal = (double)filterMaxModel.getValue();

		// om_ProcedureSettings.filterMaxVal.setContent( (double)filterMaxModel.getValue() );


		// save the OM_ProcedureSettings XML file:
		try {
			om_ProcedureSettings.saveXmlDoc(OM_MetaData);
		} catch (Exception e) {
			// e.printStackTrace();
			IJ.error("XML OM_ProcedureSettings.xml not saved to OM_MetaData Directory.");
		}

	}

	/**
	 * Returns the current filter Attribute Index.
	 * @return
	 */
	public int getFilterIndexValue() {
		if(filterCheckBox.isSelected() == true) {
			return filterComboBox.getSelectedIndex();
		}
		else {
			return 0;
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	/**
	 * Gets the X and Y coordinates where the mouse was clicked, and finds the object in the Z projection in 
	 * imageHandler (where pixel is above 0), and using this information will unselect the current object,
	 * and then select the new object.
	 */
	@Override
	public void mousePressed(MouseEvent e) {

		// Get coordinates, and convert to image coordinates:
		int offscreenX = IWP.getCanvas().offScreenX( e.getX() );
		int offscreenY = IWP.getCanvas().offScreenY( e.getY() );

		// IJ.showMessage("xOff: "+offscreenX+" yOff: "+offscreenY);

		//Get Pixel Value at these coordinates:
		int[] pixAndZ = imageHandler.getPixelAndZ(offscreenX, offscreenY, 
				(int)IWP.minModel.getValue(), (int)IWP.maxModel.getValue());

		int pix = pixAndZ[0];
		int z = (pixAndZ[1] -1); // set z to z-1 as the Z slice needs to be 0-based for the below method

		//Next, try to SELECT the whole object in the image:

		//FIRST, if a previous object is ALREADY SELECTED -> UNSELECT IT:
		unselectCurrentObject(pix);

		// THEN, set the currently selected Object to SELECTED pixel value:
		setCurrentObjectToSelectedPixVal(pix, offscreenX, offscreenY, z, true );
		// true -> want to set the object to SELECTED no matter what - allows user to see their selection more clearly.

		//and re-draw the image on IWP:
		IWP.updateSlices(true);

		// Finally, update the Obj Info Text Area to reflect the current classification of the selected obj:
		updateObjInfo();


	}

	/**
	 * Want to set the CURRENT object (whose first pixel value is in firstPixel object) to an UNSELECTED
	 * Pixel Value.  This method uses the firstPix object and the objSelected & objSelectedPixVal variables
	 * to set the previous object to an UNSELECTED pixel value & Colour.
	 * <p>
	 * the firstPix object holds the reference to the OLD firstPixel location, and the objSelectedPixVal
	 * holds the unselected pixel value of the OLD objects classification.  This method therefore assumes
	 * the firstPix and objSelectedPixVal variables have not been updated or changed.
	 * <p>
	 * The int passed here is the pixel value of the NEWLY SELECTED OBJECT.  This is required
	 * to check that the newly selected object is not the same as the old object.  If the previous object
	 * has been clicked on again, then this int will be equal to SELECTED, so just need to check this here
	 * to avoid an infinite loop.
	 * 
	 * @param objectPixVal The pixel value of the Object which has just been selected - this is used to check
	 * that the object just selected is NOT ALREADY SELECTED, and therefore the selection is a NEW Object and not
	 * the previous object.  If the same object is selected again, objectPixVal will equal SELECTED, and there is no
	 * need to change the "previous" (which is also the current) object to its un-selected object value.
	 * 
	 */
	public void unselectCurrentObject(int newObjectPixVal) {
		// Only un-select the Current object IF the newObjectPixVal passed does NOT equal the
		// current selectedObj selectedPixelValue:
		//IJ.showMessage("unselectCurrentObj - newObjPixVal: "+newObjectPixVal);
		// if(selectedObj.isSelected()  &&  newObjectPixVal != selectedObj.getSelectedPixelValue() ) {
		if(selectedObj.isSelected()  &&  newObjectPixVal != SelectedObject.UNCLASSIFIED ) {

			imageHandler.setObjValue(selectedObj);
			
			//IJ.showMessage("unselectCurrentObj - set obj to: "+selectedObj.getUnselectedPixelValue() );

		}
	}

	/**
	 * Want to set the PRESENTLY SELECTED object (whose first pixel is passed to this method via x, y, z)
	 * to a SELECTED Pixel Value.  This method sets the selected object to a SELECTED Pixel Value.  The Pixel Value
	 * is either SELECTED if setSelected is true, or it is set to the correct Classification SELECTED value
	 * (FEATURE/NONFEATURE/CONECTED/UNCLASSIFIED) if setSelected is false.
	 * <p>
	 * If the objectPixVal is 0, then the background has been clicked.  Therefore the objSelected boolean is set to
	 * false, and the firstPix value is set to default (0,0,0).
	 * <p>
	 * Note, this method does not re-draw the image, so it will not be updated by calls to this method.  You must run
	 * IWP.updateSlices(true) AFTER running this method to update object colours.
	 * 
	 * @param objectPixVal The pixel value of the presently selected object.  This is used to check
	 * that the object just selected is NOT ALREADY SELECTED, and therefore the selection is a NEW Object and not
	 * the previous object.  If the same object is selected again, objectPixVal will equal SELECTED, and there is no
	 * need to change the "previous" (which is also the current) object to its un-selected object value.
	 * 
	 * @param x The x co-ordinate of the first pixel of the presently selected object.
	 * 
	 * @param y The y co-ordinate of the first pixel of the presently selected object.
	 * 
	 * @param z The z co-ordinate of the first pixel of the presently selected object.
	 * 
	 * @param setSelected This boolean represents whether the current object should be set to the SELECTED pixel value,
	 * which is the SelectedObject.UNCLASSIFIED value (producing a PINK Selected colour by default)
	 * or if it should be set to the FEATURE/NONFEATURE/CONNECTED/UNCLASSIFIED SELECTED pixel values as appropriate
	 * to the objects current classification.
	 * 
	 */
	public void setCurrentObjectToSelectedPixVal(int objectPixVal, int x, int y, int z, boolean setSelected) {

		// if ObjectPixVal above 0, not already Unclassified Selected & setSelected is true:
			// If setSelected is true -> convert the object to the selected (UNCLASSIFIED) pixel value:
		if(objectPixVal > 0  &&  objectPixVal != SelectedObject.UNCLASSIFIED && setSelected == true) {
			
			// in case the above object that was un-selected is indeed the same as the new selected object, need
				// to ensure pix is transformed to the correct un-selected pixel value, so just get pix again from x, y, z:
			objectPixVal = imageHandler.getPixelValueThresholded(x, y, z);

			// the set the object to the Unclassified Selected value in image:
			selectedObj = imageHandler.setObjValue(x,y,z,SelectedObject.UNCLASSIFIED, objectPixVal);
			
			// and set it to the selectedObj:
			// selectedObj.setSelectedPixelValue(SelectedObject.UNCLASSIFIED);
			
			// set XYZ on selectedObj:
			// selectedObj.setXYZ(x, y, z);

			// and, independent of setSelected, set objectPixVal to the unselectedPixelValue in selectedObj:
			// selectedObj.setUnselectedPixelValue(objectPixVal);

			// And set objSelected to true:
			// selectedObj.setSelected(true);

		}
		else if( objectPixVal > 0  && !SelectedObject.isSelectedObjectValue(objectPixVal) && setSelected == false ) {
			//else if the objectPixVal is above 0 and NOT ANY selected object pixel value:

			// else if setSelected is FALSE - convert the selected object pixel value DEPENDING on the objectPixVal:
			// if FEATURE, set to FEATURE_SELECTED, if NON_FEATURE -> NON_FEATURE_SELECTED etc.
			// NOTE: The pixelValue cannot be a selected pixel value, as the if statement precludes it

			// get the Manual Class attribute value linked to this objectPixVal:
			String manClassVal = objIdentifier.returnInstanceIdentifier( objectPixVal, ObjectDataContainer.MANUALCLASS );

			// use this to retrieve the appropriate selectedPixelValue
			int selectedPixelValue = SelectedObject.returnSelectedPixelValue(manClassVal);

			// and set this to the object at correct voxel:
			selectedObj = imageHandler.setObjValue(x, y, z, selectedPixelValue, objectPixVal );

			// and set it to the selectedObj:
			// selectedObj.setSelectedPixelValue(selectedPixelValue);
			
			// set XYZ on selectedObj:
			// selectedObj.setXYZ(x, y, z);

			// and, independent of setSelected, set objectPixVal to the unselectedPixelValue in selectedObj:
			// selectedObj.setUnselectedPixelValue(objectPixVal);			

			// And set objSelected to true:
			// selectedObj.setSelected(true);

		}
		else if(objectPixVal == 0) {

			// reset the selectedObj to default unselected obj:
			selectedObj = new SelectedObject(0,0,0, false, 0, 0);

		}

	}


	@Override
	public void mouseReleased(MouseEvent e) {}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}

	/**
	 * This method performs all the computations necessary for changing a selected objects manual classification:
	 * <p>
	 * FIRST, It identifies if the selectedObj should be adjusted (based on the current and new Man Class value).
	 * Then:
	 * <p>
	 * 1. It adjust the Manual Classification Stats object appropriately.
	 * <p>
	 * 2. It adjusts the selectedObj's selected and unselected pixel values.
	 * <p>
	 * 3. It modifies the value of all pixels that compose the selectedOnj in the imageHandler.
	 * <p>
	 * 4. It adjusts the MANUALCLASS attribute value for the objects instance in datasetHandler.
	 * <p>
	 * 5. It adjusts the manClassArffBuffer object appropriately.
	 * <p>
	 * 6. Finally, it redraws the image and updates the Object Info box on this plugin.
	 * <p>
	 * NOTE:  I have decided to keep this method quite long, as this long process makes sense as an abstract
	 * unit, and so although the method may seem to violate the "rules" of making short methods, the method
	 * does encapsulate a great deal of complexity.
	 * 
	 * @param newAttrVal
	 * @param newSelectedObjVal
	 */
	public void setObjManClass(String newAttrVal, int newSelectedObjVal) {

		//IJ.showMessage("unselectedPixelValue: "+selectedObj.getUnselectedPixelValue() );
		
		// only run this code if an object is selected -> i.e. if selectedObj.getUnselectedPixelValue() is above 0!
		
		if(selectedObj.getUnselectedPixelValue() != 0) {
		
			// get the current value MANUAL CLASSIFICATION value from selectedObj:
			String currentAttrVal = objIdentifier.returnInstanceIdentifier(
					selectedObj.getUnselectedPixelValue(), 
					// ObjectDataContainer.MANUALCLASS
					ObjectDataContainer.MANUALCLASS
					);

			if(  selectedObj.isSelected()  && !( currentAttrVal.equals(newAttrVal) )  ) {
				// adjust object if one is selected & the object is not already classified as the newAttrVal

				// 1. Adjust the manClassStats obj & spinners as necessary:
				manClassStats.adjustStats( 
						ObjectDataContainer.getValueIndex( ObjectDataContainer.MANUALCLASS, currentAttrVal ), 							
						ObjectDataContainer.getValueIndex( ObjectDataContainer.MANUALCLASS, newAttrVal )
						);

				// And adjust any spinners which are affected by the stats:
				adjustObjSelRandSpinners(manClassStats);


				// 2. Set the selectedObj's new unselectedPixelValue:
				// first determine the newFlagVal from the current unselectedPixVal + the ClassTitle & newAttrVal:
				int newUnselectedObjVal = objIdentifier.returnObjectFlag(
						selectedObj.getUnselectedPixelValue(), 
						ObjectDataContainer.MANUALCLASS, 
						newAttrVal
						);		
				// then set the new unselectedPixVal:
				selectedObj.setUnselectedPixelValue( newUnselectedObjVal );

				// and set the object Selected Pixel Value to the objClassSelected value the object is set to:
				selectedObj.setSelectedPixelValue( newSelectedObjVal );

				

				// 3. Set the obj pixels in the image to the selected obj value:
					// only if the newSelectedObjVal doesnt equal the current value of the object!
					// This can happen when an object is re-selected, and its selectedPixVal is then SELECTED
						// which is the selected value of UNCLASSIFIED objects! So this becomes a problem if the user
						// tries to classify the object as UNCLASSIFIED -> obj pix vals will clash!
				if(newSelectedObjVal != imageHandler.getPixelValueThresholded(
						selectedObj.x, selectedObj.y, selectedObj.z) ) {
					imageHandler.setObjValue( selectedObj, newSelectedObjVal ); // pass newSelectedObjVal!
				}

				// 4. In arff data file, set the manual classifier column to newAttrVal value:
				objectDataset.sort(ObjectDataContainer.OBJNO); // sort by OBJNO first to ensure the CORRECT obj is modified
				objectDataset.setValue( 
						(firstPixObjNoMap.get( selectedObj.returnArrayListFirstPixCoord() ))-1, // -1 as obj ref is -1 from obj no 
						(double)ObjectDataContainer.getValueIndex(ObjectDataContainer.MANUALCLASS, newAttrVal),
						ObjectDataContainer.MANUALCLASS,
						false
						);


				// 5. adjust the manClassArffBuffer as appropriate:
				manClassArffBuffer.adjustArffBufferData(  
						newAttrVal, 
						firstPixObjNoMap.get( selectedObj.returnArrayListFirstPixCoord() )  
						);


				// 6. re-draw the image on IWP & update Obj Info:
				IWP.updateSlices(true);

				// Update the Obj Info Text Area to reflect the current classification of the selected obj:
				updateObjInfo();

			}

			else if(selectedObj.isSelected()  &&  currentAttrVal.equals(newAttrVal)  ) {
				// run if an obj is selected & the object is already classified as objClass
				// want to ensure the object color changes when classifying an object which is selected by the user
				// which has already previously been classified.
				// BUT - also make sure NOT to set the object to the pixel value it CURRENTLY IS:

				//IJ.showMessage("isSelected: "+selectedObj.isSelected() );
				//IJ.showMessage("newSelectedObjVal: "+newSelectedObjVal);
				//IJ.showMessage("current SelectedObjVal: "+selectedObj.getSelectedPixelValue() );
				//IJ.showMessage("Thresholded Pixel Value: "+imageHandler.getPixelValueThresholded(
				//		selectedObj.x, selectedObj.y, selectedObj.z) );

				//if(newSelectedObjVal != selectedObj.getSelectedPixelValue() ) {
					// this has a subtle bug in it:  If the object is clicked (selected), the selectedObj is adjusted
						// to the UNCLASSIFIED selected value, and if the object is then classified as FEATURE(etc),
						// BUT the object was ALREADY FEATURE - the above code does not run. this means the object REMAINS
						// with the UNCLASSIFIED SELECTED VALUE in selectedObj!  Therefore, the comparison here may actually
						// return TRUE when the reality is the actual current object value and 'newSelectedObjVal' do match!
						// THEREFORE -> do NOT used this comparison, but instead compare 'newSelectedObjVal'
						// with the ACTUAL object pixel value -> determined from the imageHandler itself:
				if(newSelectedObjVal != imageHandler.getPixelValueThresholded(
													selectedObj.x, selectedObj.y, selectedObj.z) ) {
					// Set the object (which remains selected) to the objClassSelected pix val (blue):
					//selectedObj = borderObjPixProcessing3D.selectObj3d26(thresholdImgInt, selectedObj.x, selectedObj.y, 
					//													selectedObj.z, objClassSelected);
					imageHandler.setObjValue(selectedObj, newSelectedObjVal);
					//and re-draw the image on IWP:
					IWP.updateSlices(true);
				}
			}
			else {
				// ENSURE -> give the IWP custom canvas the focus:
				// Ensures the custom canvas has focus after clicking on the button:
				IWP.cc.requestFocusInWindow();
			}
		
		}

	}

	/**
	 * Remove all Listeners from this object.
	 */
	public void removeListeners() {

		// remove all listeners on the components of this plugin:
		StereoMateUtilities.removeActionListener(loadButton);
		
		StereoMateUtilities.removeActionListener(featureButton);
		StereoMateUtilities.removeActionListener(nonFeatureButton);
		StereoMateUtilities.removeActionListener(connectedButton);
		StereoMateUtilities.removeActionListener(unclassifiedButton);
		StereoMateUtilities.removeActionListener(filterViewButton);
		StereoMateUtilities.removeActionListener(classifierViewButton);
		StereoMateUtilities.removeActionListener(manualViewButton);
		StereoMateUtilities.removeActionListener(imagePlotButton);
		StereoMateUtilities.removeActionListener(manualPlotButton);
		StereoMateUtilities.removeActionListener(imageDataTableButton);
		StereoMateUtilities.removeActionListener(objSelComboBox);
		StereoMateUtilities.removeActionListener(objSelRandButton);
		StereoMateUtilities.removeActionListener(objSelRandOptionsButton);
		StereoMateUtilities.removeActionListener(biasedObjectSelectionTextfield);
		StereoMateUtilities.removeActionListener(objSelBiasedButton);
		StereoMateUtilities.removeActionListener(attrSelComboBox);
		StereoMateUtilities.removeActionListener(attrSelectionButton);
		StereoMateUtilities.removeActionListener(filterComboBox);
		StereoMateUtilities.removeActionListener(filterCheckBox);
		StereoMateUtilities.removeActionListener(filterOverClassifierCheckBox);
		StereoMateUtilities.removeActionListener(classifierSelectButton);
		StereoMateUtilities.removeActionListener(classifierDeleteButton);
		StereoMateUtilities.removeActionListener(classifierCheckBox);
		StereoMateUtilities.removeActionListener(nextButton);
		StereoMateUtilities.removeActionListener(previousButton);
		StereoMateUtilities.removeActionListener(exportOMDataButton);
		StereoMateUtilities.removeActionListener(saveImageButton);
		StereoMateUtilities.removeActionListener(optionsButton);
		StereoMateUtilities.removeActionListener(helpButton);

		StereoMateUtilities.removeChangeListener(filterMax);
		StereoMateUtilities.removeChangeListener(filterMin);
		
		// Also IWP has had mouseListeners and keyListeners added to it by this class:
			// IWP.addKeyListener(this); [1866]
			// IWP.getCanvas().addMouseListener(this); [1778]
		
		// IWP.removeKeyListeners(); // do not need it here, as its full implementation is added to IWP.shutdownIWP();
		// IWP.removeMouseListeners(); // do not need it here, as its full implementation is added to IWP.shutdownIWP();

	}
	
	/**
	 * sets many of the instance variables to null, to shut down the OM.
	 */
	public void shutdownOM() {
		imageHandler = null;
		originalImp = null;
		procedureStack = null;
		
		attributes = null;	attributesBuffer = null;	classModeLUTs = null;
		objectDataset = null;	manClassArffBuffer = null;	firstPixObjNoMap = null;
		dataObj = null;	om_ProcedureSettings = null;	maxDimensions = null;
		objIdentifier = null;	selectedObj = null;	manClassStats = null;
		
		objectClassifier = null;
		
		viewButtonGroup = null;
		highModel = null; lowModel = null; noModel = null;
		
		objectSelectionThread = null;
		objectSelectionRunner = null;
		
		attrMaxModel = null; 	attrMinModel = null;
		filterMaxModel = null;	filterMinModel = null;
	}
	
	/**
	 * Shuts down the IWP object: Disposes of the window, removes any modal setting on IWP,
	 * calls IWP shutdown method, and sets IWP to null. Calls garbage collector to clear 
	 * de-referenced objects.
	 */
	public void shutdownIWP() {
		// shut down the IWP:
		IWP.getWindow().dispose(); //dispose the iw.
		IWP.setModal(false); //re-activate the ij window.
		IWP.shutdownImageWindowWithPanel();
		IWP = null;  // -> set to null to garbage collect.
		
		System.gc();
	}
	
	public void shutdownDW() {
		dw.shutDownDialogWindow();
		dw = null; //DialogWindow object - to create a dialog to select files for
							//processing.
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
	 * Saves the Classifier file to the OM_MetaData Directory.  Classifier should be saved to 
	 * OM_MetaData as 'OM_classifier.model'.  Only saved if a classified is loaded (if classifierLoaded is TRUE).
	 */
	public void saveClassifierFile() {

		// only run if a classifier is selected:
		// if(classifierCheckBox.isSelected()==true && loadedClassifier !=null) {
		
		File classifierFile = new File(OM_MetaData.getAbsolutePath() + File.separator + "OM_classifier.model");
		
		if(objectClassifier.isLoaded() == true) {
			objectClassifier.saveClassifierFile( classifierFile );
		}
		else { // if the classifier exists, delete the file!
			if( classifierFile.exists() ) {
				classifierFile.delete();
			}
		}

	}


	/**
	 * This method will update the Obj Info Text Area to reflect the information of the currently selected object
	 * (if any). 
	 */
	public void updateObjInfo() {

		// IJ.showMessage("objSelected val: "+objSelected);

		if(objSelThreadActive == false) {

			if(selectedObj.isSelected() == false) {

				selectedObjInfo.setText(  " No Obj. Selection" +
						"\n F / NF / C :  Total: "+
						"\n "+ manClassStats.feature + " / " +manClassStats.nonfeature + " / "+manClassStats.connected +" : "+manClassStats.total +
						"\n Filter: "+ //getFilter() +
						"\n Classifier: " //+ getClassifier() 
						);
			}
			else {
				// else an obj is selected - report its number and attributes and classification:

				//Get the objRef from hashmap:
				ArrayList<Integer> list = new ArrayList<Integer>();
				Collections.addAll(list, selectedObj.x, selectedObj.y, selectedObj.z);
				
				// IJ.showMessage("list - x: "+list.get(0)+" y: "+list.get(1)+" z: "+list.get(2) );

				//Get the objNo from hashmap:
				int objNo = firstPixObjNoMap.get( list );

				int size = (int)objectDataset.get( (objNo-1), ObjectDataContainer.VOLVOXELS);

				// double spher = datasetHandler.get( (objNo-1), ObjectDataContainer.SPHERICITY);
				double spher = (double)Math.round(objectDataset.get(objNo-1).value( 
									objectDataset.attribute(ObjectDataContainer.SPHERICITY) ) 
										* 10000d) / 10000d; // rounded to 4 DP.
				
				//IJ.showMessage("unselected pixel val: "+selectedObj.getUnselectedPixelValue() );

				// Get the classification of currently selected object as a String - derived from its objSelectedPixVal:
				// String classification = getClassificationString( selectedObj.getUnselectedPixelValue() );
				String classification = objIdentifier.returnInstanceIdentifier(
														selectedObj.getUnselectedPixelValue(), 
														ObjectDataContainer.MANUALCLASS );				

				// FINALLY - set the selectedObjInfo Text Area with these values:
				selectedObjInfo.setText("\n Obj: "+objNo+"  size: "+size+
						"\n spher.: "+spher+
						"\n class.: "+classification);

			}

		}
		else {
			// objSelThreadActive is TRUE -> to present information to the user about the current object,
			// the number of objects in the thread to assess, and information on the current object.

			// Should report:
			// The fact we are in Object Selection Mode
			// Current Obj No. / Total Obj Number
			// Object details -> objNo, Size, Sphericity, Classification.

			if(selectedObj.isSelected() == false) {
				// if no obj selected, set default text:
				selectedObjInfo.setText("OBJECT SELECTION MODE" +
						"\n Total No. Objects: "+manClassStats.total +
						"\n Feature Objects:   "+manClassStats.feature +
						"\n Non-Feature Objs:  "+(manClassStats.nonfeature+manClassStats.connected)
						);
			}
			else {
				// else an obj is selected - report its number and attributes and classification:

				// Should report:
				// The fact we are in Object Selection Mode
				// Current Obj No. / Total Obj Number
				// Object details -> objNo, Size, Sphericity, Classification.

				ArrayList<Integer> list = new ArrayList<Integer>();
				list.add(selectedObj.x);
				list.add(selectedObj.y);
				list.add(selectedObj.z);

				//Get the objRef from hashmap:
				int objNo = firstPixObjNoMap.get( list );

				int size = (int)objectDataset.get( (objNo-1), ObjectDataContainer.VOLVOXELS);

				// double spher = (int)datasetHandler.get( (objNo-1), ObjectDataContainer.SPHERICITY);
				double spher = (double)Math.round(objectDataset.get(objNo-1).value( 
						objectDataset.attribute(ObjectDataContainer.SPHERICITY) ) 
							* 10000d) / 10000d; // rounded to 4 DP.

				// Get the classification of currently selected object as a String - derived from its objSelectedPixVal:
				// String classification = getClassificationString( selectedObj.getUnselectedPixelValue() );
				// Actually this doesnt work!  If the object is re-selected, its pix val is set to SELECTED, 
				//and its classification is LOST!
				// Instead, I should READ its classification from the arff file:
				String classification = objIdentifier.returnInstanceIdentifier(
														selectedObj.getUnselectedPixelValue(), 
														ObjectDataContainer.MANUALCLASS );


				// FINALLY - set the selectedObjInfo Text Area with these values:
				selectedObjInfo.setText("OBJECT SELECTION MODE" +
						"\nObj. Selection: " + (objectSelectionRunner.index+1) + " / " + objectSelectionRunner.objects.size() +
						"\n Obj: " + objNo + "  size: " + size +
						"\n spher.: " + spher +
						"\n class.: " + classification);

			}

		}

	}



	/**
	 * This class represents the Instances object plus a buffer for the Image Index.
	 * @author stevenwest
	 *
	 */
	public class ArffBuffer {

		Instances arffBuffer;
		int bufferIndex;
		String bufferString;

		/**
		 * Constructor generating a new arffBuffer Instances object with name datasetName and attributes
		 * as its attributes.  The bufferIndex is initialised to 0.
		 * @param datasetName
		 * @param attributes
		 */
		public ArffBuffer(String datasetName, ArrayList<Attribute> attributes) {
			arffBuffer = new Instances(datasetName, attributes, 1000);
			bufferIndex = 0;
			bufferString = "";
		}
		
		public ArffBuffer(Instances arffBuffer) {
			this.arffBuffer = arffBuffer;
			bufferIndex = 0;
			bufferString = "";
		}
		
		public void setBufferString(String bufferString) {
			this.bufferString = bufferString;
		}


		public void incrementBufferIndex() {
			bufferIndex = bufferIndex + 1;
		}

		/**
		 * Sets the Buffer Index to the passed value - this will adjust what value is put into the arffBuffer
		 * dataset (which is used to incidate which image the data is from).
		 * @param newBufferIndex
		 */
		public void setBufferIndex(int newBufferIndex) {
			bufferIndex = newBufferIndex;
		}

		/**
		 * This method will adjust the passed instance to ensure its Attributes match the Attributes in the
		 * arffBuffer.  It will REMOVE Attributes which are not present in arffBuffer, and ADD Attributes
		 * from arffBuffer which are not present in the instance.
		 * @param instance Reformatted to have the correct Attributes.
		 * @return
		 */
		public Instance formatInstance(Instance instance) {
			return instance;
		}



		/**
		 * This method adjusts the manClassArffBuffer - either adding an instance to it, or removing an instance,
		 * depending on the value of objClassAtr.  the objNo int represents the object number as listed in the
		 * arff dataset.
		 */
		public void adjustArffBufferData(String objClassAtr, int objNo) {

			if( objClassAtr == ObjectDataContainer.FEATUREATR || 
					objClassAtr == ObjectDataContainer.NONFEATUREATR || 
					objClassAtr == ObjectDataContainer.CONNECTEDATR ) {

				// format the instance for its insertion into this buffer:
				// TODO this is hard coded at present, should make this more generalisable!
				// should make the class compare Attribute values for the arffBuffer object and the instance
				// retrieved..!
				//Instance instance = arff.get(objNo-1);
				//instance = removeAttributesFromInstance(arff, instance, classifierAttributesNoFilterNoClassifier);
				//instance = addAttributeToInstance(arffBuffer, instance, "first", "Image Index");

				// if object has been classed feature/non-feature/connected, need to either:
				// add the object - if not already in manClassArffBuffer
				// adjust the objects classification - if object is already in manClassArffBuffer
				addInstanceFromArffToArffBuffer(objNo);
			}
			else {
				// else object has been classed unclassified, need to:
				// remove the object from manClassArffBuffer If it is present in this dataset
				// (if object classed unclassified from unclassified state, object will not actually be in
				// manClassArffBuffer!)
				removeInstanceFromArffBuffer(objNo);
			}

			// Set the value of the manClass Attribute in arff at objRef index to objClass -> objClassAtr					
			//arff.get(objNo - 1).setValue(manClass, manClass.indexOfValue(objClassAtr) );


		}

		/**
		 * This method will add the passed instance from arff at objNo into the manClassArffBuffer.  It is only
		 * added if the objNo has not already been added at this bufferIndex.  If the objNo has been added,
		 * its manClass Attribute is changed to match that of the passed instance manClass Attribute value.
		 * @param objNo
		 * @param instance
		 */
		public void addInstanceFromArffToArffBuffer(int objNo) {

			// First check if the arffBuffer contains this instance - check if objNo Attribute exists
			// with the current bufferIndex:
			
			boolean containsInstance = false;
			
			for(int a=0; a<arffBuffer.numInstances(); a++) {
				
				//if(  bufferIndex == arffBuffer.get(a).value( arffBuffer.attribute( ObjectDataContainer.IMAGEINDEX) )  ) {
				if(  bufferString.equals(arffBuffer.get(a).stringValue( arffBuffer.attribute( ObjectDataContainer.IMAGENAME) ) )  ) {
					
					if(objNo == arffBuffer.get(a).value( arffBuffer.attribute(ObjectDataContainer.OBJNO) ) ) {
						
						containsInstance = true;
						
						alterInstance(a, arffBuffer.attribute(ObjectDataContainer.MANUALCLASS), 
								objectDataset.get(objNo-1).value( objectDataset.attribute(ObjectDataContainer.MANUALCLASS) )  );
					
					}
				}
			}

			// only if the instance is not present, add it to the arffBuffer at correct position:
			// insert instance to the END of arffBuffer, so it keeps a chronological order of object classification
			if(containsInstance == false) {
				//addInstance(instance);
				addInstanceFromArray(objNo-1);
			}

			// showArffDatatable(arffBuffer);
			// IJ.showMessage("arffBuffer after addition of: "+objNo+" at index: "+bufferIndex);

		}


		public void addInstanceFromArray(int objIndex) {
			// first instance
			double[] vals = new double[arffBuffer.numAttributes()];  // important: needs NEW array with each call!

			//vals[0] = bufferIndex;
			vals[0] = arffBuffer.attribute(0).addStringValue(bufferString);

			vals[1] = objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.X1) );
			vals[2] = objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.Y1) );
			vals[3] = objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.Z1) );

			vals[4] = objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.OBJNO) );

			vals[5] = objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.VOLVOXELS) );
			vals[6] = objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.AREAVOXELS) );

			vals[7] = objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.XMIN) );
			vals[8] = objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.YMIN) );
			vals[9] = objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.ZMIN) );
			vals[10] = objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.XLENGTH) );
			vals[11] = objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.YLENGTH) );
			vals[12] = objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.ZLENGTH) );


			vals[13] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.CENTREX) ) * 100000d) / 100000d;
			vals[14] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.CENTREY) ) * 100000d) / 100000d;
			vals[15] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.CENTREZ) ) * 100000d) / 100000d;


			vals[16] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.COMPACTNESS) ) * 100000d) / 100000d;
			//vals[15] = arff.get(objIndex).value(compactness);
			vals[17] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.SPHERICITY) ) * 100000d) / 100000d;
			//vals[16] = arff.get(objIndex).value(sphericity);

			vals[18] = objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.VOLCONVEX) );
			vals[19] = objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.SURFCONVEX) );

			vals[20] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.SOLIDITY3D) ) * 100000d) / 100000d;
			//vals[19] = arff.get(objIndex).value(solidity3D(;
			vals[21] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.CONVEXITY3D) ) * 100000d) / 100000d;
			//vals[20] = arff.get(objIndex).value(convexity3D);

			vals[22] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.VOLTOVOLBOX) ) * 100000d) / 100000d;
			//vals[21] = arff.get(objIndex).value(volToVolBox);

			vals[23] = objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.MAINELONG) );

			vals[24] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.MEDIANELONG) ) * 100000d) / 100000d;
			//vals[23] = arff.get(objIndex).value(medianElong);

			vals[25] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.VOLELLIPSE) ) * 100000d) / 100000d;
			//vals[24] = arff.get(objIndex).value(volEllipse);
			vals[26] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.VOLTOVOLELLIPSE) ) * 100000d) / 100000d;
			//vals[25] = arff.get(objIndex).value(volToVolEllipse);
			
			vals[27] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.HOMINV1) ) * 100000d) / 100000d;
			vals[28] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.HOMINV2) ) * 100000d) / 100000d;
			vals[29] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.HOMINV3) ) * 100000d) / 100000d;
			vals[30] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.HOMINV4) ) * 100000d) / 100000d;			
			vals[31] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.HOMINV5) ) * 100000d) / 100000d;
			
			vals[32] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.GEOINV1) ) * 100000d) / 100000d;
			vals[33] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.GEOINV2) ) * 100000d) / 100000d;
			vals[34] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.GEOINV3) ) * 100000d) / 100000d;
			vals[35] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.GEOINV4) ) * 100000d) / 100000d;
			vals[36] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.GEOINV5) ) * 100000d) / 100000d;
			vals[37] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.GEOINV6) ) * 100000d) / 100000d;
			
			vals[38] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.BIOCATJ1) ) * 100000d) / 100000d;
			vals[39] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.BIOCATJ2) ) * 100000d) / 100000d;
			vals[40] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.BIOCATJ3) ) * 100000d) / 100000d;
			vals[41] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.BIOCATI1) ) * 100000d) / 100000d;
			vals[42] = (double)Math.round(objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.BIOCATI2) ) * 100000d) / 100000d;


			vals[43] = objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.MEANPIX) );
			vals[44] = objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.SDPIX) );
			vals[45] = objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.MAXPIX) );
			vals[46] = objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.MEDIANPIX) );
			vals[47] = objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.MINPIX) );

			// - 1.0 as this ArffBuffer does not contain the UNCLASSIFIED class in manClass Attribute, therefore
			// the classification index must be reduced by 1.0:
			// vals[32] = (arff.get(objIndex).value( arff.attribute(MCIB_SM_DataObj.MANUALCLASS) ))-1.0;

			// NO LONGER REQUIRED as am including UNCLASSIFIED in manClass, as otherwise there are problems with
			// any Imported Classifier classifying data in the plugin if it only used Feature,NonFeature,Connected
			// [3] values for the Class Attribute!
			vals[48] = (objectDataset.get(objIndex).value( objectDataset.attribute(ObjectDataContainer.MANUALCLASS) ));

			// 31-33 are the manual classes, filter classes and classifier classes
			// These will be 0.0 by default in the vals array.
			// Therefore these will all be set to UNCLASSIFIED (index 0) in these attributes by default

			// Here, will set the NAMED CONSTANTS to indicate the indexes of each potential Class Attribute:
			//MANCLASS = 31;
			//FILTERCLASS = 32;
			//CLASSIFIERCLASS = 33;

			//add vals to arff:
			arffBuffer.add( new DenseInstance(1.0, vals) );
		}

		/**
		 * Adds the instance to the END of the arffBuffer.
		 * @param instance
		 */
		public void addInstance(Instance instance) {
			arffBuffer.add(instance);
		}

		/**
		 * Alters the arffBuffer instance at index, at its attribute, by replacing it with newValue.
		 * @param index
		 * @param attribute
		 * @param newValue
		 */
		public void alterInstance(int index, Attribute attribute, double newValue) {
			arffBuffer.get(index).setValue(attribute, newValue);
		}


		/**
		 * This method will remove the instance from arff at objNo into the ArffBuffer.  It is only
		 * removed if the objNo is present at this bufferIndex.
		 * @param objNo
		 */
		public void removeInstanceFromArffBuffer(int objNo) {

			// First check if the arffBuffer contains the instance - check if objNo Attribute exists
			// with the current bufferIndex:

			for(int a=0; a<arffBuffer.numInstances(); a++) {
				//if(bufferIndex == arffBuffer.get(a).value( arffBuffer.attribute(ObjectDataContainer.IMAGEINDEX) )) {
				if(bufferString.equals( arffBuffer.get(a).stringValue( arffBuffer.attribute(ObjectDataContainer.IMAGENAME) )) ) {
					if(objNo == arffBuffer.get(a).value( arffBuffer.attribute(ObjectDataContainer.OBJNO) ) ) {
						// if this is reached, have located the instance.
						// this instance should be removed from arffBuffer:
						arffBuffer.remove(a);
					}
				}
			}

			//showArffDatatable(arffBuffer);
			//IJ.showMessage("arffBuffer after removal of: "+objNo+" at index: "+bufferIndex);

		}


	} //end class ArffBuffer


	/**
	 * This method adjusts the Filter View Mode - activated by key I or checking the Filter View checkbox.
	 * The view mode will adjust which object classification set is visible:  The Filter View colour codes
	 * objects based on a User Applied Filter.
	 * <p>
	 * This mode should therefore ONLY be available if a filter has been applied by the User.  This is dictated
	 * by the filterCheckBox (located on the Filter Panel, which is checked on when a filter is available and
	 * applied to an image).
	 * <p>
	 * 
	 * 
	 */
	public void setToFilterViewMode() {

		// only attempt to change to Filter View Mode if a Filter is available and active on the data:
		if(filterCheckBox.isSelected() == true) {

			// set the filterViewButton to selected, to indicate Filter View Mode is turned on:
			filterViewButton.setSelected(true);

			// Convert the LUT to display all objects as Passed the Filter and Not Passed the Filter:
			//thresholdImp.setLut(filterLut);
			//setLut(filterLut);
			setIwpLut(classModeLUTs.filterLut);

			// And update the image:
			IWP.updateSlices(true);

		}
		else {
			// FIRST -> give the IWP custom canvas the focus:
			// Ensures the custom canvas has focus after clicking on the filterViewButton no matter what:
			// Note, updateSlices() above gives focus back to the Canvas!
			IWP.cc.requestFocusInWindow();
		}


	}



	public void setToClassifierViewMode() {

		// only attempt to change to Filter View Mode if a Filter is available and active on the data:
		if(classifierCheckBox.isSelected() == true) {

			// set the filterViewButton to selected, to indicate Filter View Mode is turned on:
			classifierViewButton.setSelected(true);

			// Convert the LUT to display all objects based on the Objects Classifier Classification:
			//thresholdImp.setLut(classifierLut);
			//setLut(classifierLut);
			setIwpLut(classModeLUTs.classifierLut);

			// And update the image:
			IWP.updateSlices(true);

		}
		else {
			// FIRST -> give the IWP custom canvas the focus:
			// Ensures the custom canvas has focus after clicking on the filterViewButton no matter what:
			// Note, updateSlices() above gives focus back to the Canvas!
			IWP.cc.requestFocusInWindow();
		}

	}



	public void setToManualViewMode() {

		// set the filterViewButton to selected, to indicate Filter View Mode is turned on:
		manualViewButton.setSelected(true);

		// Convert the LUT to display all objects based on the Objects Manual Classification:
		//thresholdImp.setLut(manualLut);
		//setLut(manualLut);
		setIwpLut(classModeLUTs.manualLut);

		// And update the image:
		IWP.updateSlices(true);

	}

	/**
	 * This method will set any currently selected object to its unselected pixel value, and set 
	 * the relevant instance variables relating to a selected object to default un-selected values 
	 * (objSelected = false; firstPix = new FirstPixel(0,0,0);objSelectedPixVal = 0).
	 */
	public void unselectAnyObject() {
		unselectCurrentObject(0);
		selectedObj = new SelectedObject(0,0,0, false, 0, 0);
	}


	public void saveManualClassificationData() {


		// NOTE:  Will also be saving the ManualClassificationData.arff file to output - this will be saved
		// to OM_MetaData

		// make the OM_MetaData Directory:
		//OM_MetaData = new File(dw.getOutputParentFile().getAbsolutePath() 
		//		+ File.separator + "OM_MetaData");
		//OM_MetaData.mkdir();

		// Put the ThresholdProcedureStack XML file into OM_MetaData:
		// Prefixed the procedureStack title with PS_ to allow it to be recognised in SM Analyser algorithm:
		// removed the ".xml" extension from the procedureStackTitle too..
		//procedureStack.saveStack("PS_"+procedureStackTitle.substring(0, procedureStackTitle.length()-4), OM_MetaData);



		try {
			//DataSink.write(dw.getCurrentOutputFileNoPause().getAbsolutePath() +".arff", inst);
			DataSink.write(OM_MetaData.getAbsolutePath() + File.separator +"ManualClassificationData.arff", 
					manClassArffBuffer.arffBuffer);
			//DataSink.write(dw.getCurrentOutputFile().getAbsolutePath() +".arff", inst);
		} catch (Exception e) {}


	}
	
	/**
	 * Loads the ManualClassificationData.arff file if it exists in OM_MetaData Dir, otherwise it
	 * creates a new manClassArffBuffer object.
	 */
	public void loadManualClassificationData(ArrayList<Attribute> attributes) {
		
		File manualClassArffFile = new File(OM_MetaData.getAbsolutePath() + 
										File.separator + "ManualClassificationData.arff");
		
		if(manualClassArffFile.exists()) {
			try {
				Instances manInstances = DataSource.read( manualClassArffFile.getAbsolutePath() );
				manClassArffBuffer = new ArffBuffer(manInstances);
			} catch (Exception e) {
				IJ.showMessage("Loading ManualClassificationData.arff from OM_MetaData failed: "+e.getMessage() );
			}
		}
		else {
			manClassArffBuffer = new ArffBuffer("ManualClassificationData", attributes);
		}
	}




	public Attribute convertIndexToAttribute(int index) {
		return attributes.get(index);
	}


	public void adjustObjSelRandSpinners(ClassificationStats classStats) {

		// if there are objects in the feature and non-feature (which is nonfeature+connected) are 
		// BOTH above 0, then activate the objSelRand spinners:
		if(classStats.feature > 0 && (classStats.nonfeature + classStats.connected) > 0) {
			java.awt.Component[] cps = objSelHigh.getComponents();
			objSelHigh.setEnabled(true);
			for(int a=0; a<cps.length;a++) {
				cps[a].setEnabled(true);
			}

			cps = objSelLow.getComponents();
			objSelLow.setEnabled(true);
			for(int a=0; a<cps.length;a++) {
				cps[a].setEnabled(true);
			}
		}
		else {
			java.awt.Component[] cps = objSelHigh.getComponents();
			objSelHigh.setEnabled(false);
			for(int a=0; a<cps.length;a++) {
				cps[a].setEnabled(false);
			}

			cps = objSelLow.getComponents();
			objSelLow.setEnabled(false);
			for(int a=0; a<cps.length;a++) {
				cps[a].setEnabled(false);
			}
		}


	}
	
	
	public void adjustObjSelRandSpinners(boolean adjust) {
		if(adjust == true) {
			java.awt.Component[] cps = objSelHigh.getComponents();
			objSelHigh.setEnabled(true);
			for(int a=0; a<cps.length;a++) {
				cps[a].setEnabled(true);
			}

			cps = objSelLow.getComponents();
			objSelLow.setEnabled(true);
			for(int a=0; a<cps.length;a++) {
				cps[a].setEnabled(true);
			}
		}
		else {
			java.awt.Component[] cps = objSelHigh.getComponents();
			objSelHigh.setEnabled(false);
			for(int a=0; a<cps.length;a++) {
				cps[a].setEnabled(false);
			}

			cps = objSelLow.getComponents();
			objSelLow.setEnabled(false);
			for(int a=0; a<cps.length;a++) {
				cps[a].setEnabled(false);
			}
		}
		
	}


	/**
	 * This method will select the designated number of random objects from the arff dataset for classification.
	 * Only UNCLASSIFIED objects will be selected for classification, and objects will be selected with
	 * a classification distributionForInstance() decimal between the low and high values supplied.
	 * <p>
	 * First, the arff dataset must be split into UNCLASSIFIED and manually Classified instances.
	 * <p>
	 * If necessary, the datasets must then be filtered & a classifier trained on the manually classified instances
	 * and then used to classify the unclassified instance.  This is only necessary if low and high are not 0.0
	 * and 1.0.  If they are 0.0 and 1.0, then do not need to select objects based on their likelihood of being
	 * in the feature class.
	 * <p>
	 * After splitting, and potential filtering + classification of unclassified instances, the number of objects
	 * with probabilities between low and high should be presented to the user in turn for classification.  This
	 * should happen in a separate thread to allow the user to classify the objects in the EDT!
	 * @param number
	 * @param low
	 * @param high
	 */
	public void selectRandomObjects(int number, double low, double high ) {

		// FIRST - if an objectSelectionThread is ALREADY ACTIVE - need to end it:
		endObjectSelectionThread(); // TODO -> move this method to objectSelectionRunner/Thread?

		// Select Objects from datasetHandler, using SelectObjects:
		int seedValue = (int)seedModel.getValue();
		SelectObjects so = new SelectObjects( objectDataset.getInstances(), objectClassifier, new Long(seedValue) );

		// Select Random Objects:
		ArrayList<Integer> objectNumbers = so.selectRandomObjects( number,  low,  high );

		if(objectNumbers.size() > 0) {

			objSelThreadActive = true;
			// This allows keyListeners to become active in the keyPressed() method to move through
			// the objects array selection & cancel the ObjectSelectionThread run.

			objectSelectionRunner = new ObjectSelectionThread(objectNumbers);

			objectSelectionThread = new Thread( objectSelectionRunner );

			objectSelectionThread.start();
		}
		else {
			IJ.showMessage("No Objects","No Objects found between \n "
					+ "'low' and 'high' criteria. \n"
					+ "\n"
					+ "Please Select a Different Range!");
		}
	}



	/**
	 * Selects a linear set of objects from the manually UNCLASSIFIED set of objects in the Instances
	 * dataset.  The linear set of objects is selected between low and high on Classifier-determined 
	 * probability (0-1 scale).  The number of divisions is passed as an argument, and this determines
	 * how many evenly spaced divisions of the space between low and high will be created.
	 * <p>
	 * The method follows the same layout as the selectRandomObjects(): first, any selectionThread is ended,
	 * and the Instances dataset is split into classified and unclassified instances.  These new datasets are
	 * filtered and a Classifier built with the classified data.
	 * <p>
	 * Next the probability value ranges for object selection are computed from the low, high and divisions
	 * variables and objects are selected in these ranges in a linear fashion.  These are passed into
	 * an objectSelectionRunner/Thread set of objects, to present object to the user.
	 * 
	 * @param divisions
	 * @param low
	 * @param high
	 */
	public void selectLinearObjects(int divisions, double low, double high) {

		// FIRST - if an objectSelectionThread is ALREADY ACTIVE - need to end it:
		endObjectSelectionThread();  // TODO -> move this method to objectSelectionRunner/Thread?

		// Select Objects from datasetHandler, using objectClassifier:
		int seedValue = (int)seedModel.getValue();
		SelectObjects so = new SelectObjects( objectDataset.getInstances(), objectClassifier, new Long(seedValue) );

		// Select Linear Objects:
		ArrayList<Integer> objectNumbers = so.selectLinearObjects( divisions,  low,  high );


		// Now -> Create the ObjectSelectionThread from the objects ArrayList:
		if(objectNumbers.size() > 0 ) {

			// TODO need to refactor this code to the objectSelectionRunner/Thread class:

			//First - set objSelThreadActive to true -> to indicate to program that a series of objects are being
			// traversed:
			objSelThreadActive = true;
			// This allows keyListeners to become active in the keyPressed() method to move through
			// the objects array selection & cancel the ObjectSelectionThread run.

			objectSelectionRunner = new ObjectSelectionThread(objectNumbers);

			objectSelectionThread = new Thread( objectSelectionRunner );

			objectSelectionThread.start();
		}
		else {
			IJ.showMessage("No Objects Found",
					"No Objects found \n "
							+ "between 'low' and 'high' criteria. \n"
							+ "Terminating Object Selection, please\n"
							+ "try different parameters.");
		}

	}


	/**
	 * 
	 * @param distribution
	 * @param low
	 * @param high
	 */
	public void selectGaussianObjects(String distribution, double low, double high ) {

		// FIRST - if an objectSelectionThread is ALREADY ACTIVE - need to end it:
		endObjectSelectionThread(); // TODO -> move this method to objectSelectionRunner/Thread?

		// Select Objects from datasetHandler, using objectClassifier:
		int seedValue = (int)seedModel.getValue();
		SelectObjects so = new SelectObjects( objectDataset.getInstances(), objectClassifier, new Long(seedValue) );

		// Select Gaussian Objects:
		ArrayList<Integer> objectNumbers = so.selectGaussianObjects( distribution,  low,  high );


		// Now -> Create the ObjectSelectionThread from the objects ArrayList:
		if(objectNumbers.size() > 0 ) {

			// TODO need to refactor this code to the objectSelectionRunner/Thread class:

			//First - set objSelThreadActive to true -> to indicate to program that a series of objects are being
			// traversed:
			objSelThreadActive = true;
			// This allows keyListeners to become active in the keyPressed() method to move through
			// the objects array selection & cancel the ObjectSelectionThread run.

			objectSelectionRunner = new ObjectSelectionThread(objectNumbers);

			objectSelectionThread = new Thread( objectSelectionRunner );

			objectSelectionThread.start();
		}
		else {
			IJ.showMessage("No Objects Found",
					"No Objects found \n "
							+ "between 'low' and 'high' criteria. \n"
							+ "Terminating Object Selection, please\n"
							+ "try different parameters.");
		}


	}



	/**
	 * Implements the low- and high- pass filtering of objects based on a selected attribute.
	 * <p>
	 * Filters objects in the ARFF dataset by the selected attribute in filterComboBox, where objects
	 * with attribute value: min <= ATTR <= max  pass the filter, but objects with attribute values
	 * below min and above max are set to not passed.
	 * <p>
	 * The method only runs if the filterCheckBox is selected (and is also run WHEN the filterCheckBox is
	 * selected!).
	 * @param min
	 * @param max
	 */
	public void filterObjects( double min, double max, int attributeIndex ) {
		
		// FIRST - unselect any object, to ensure they are all filtered correctly.
		unselectAnyObject();

		// TODO Re-Factor setFilterObj() and see how this method may also be re-factored...  
		objectFilter.filterObjects(min, max, attributeIndex);
		
		// also set the boolean which tracks the objectFilter filterMaxReached
		//filterMaxReached = objectFilter.getFilterMaxReached();

		// and repaint the IWP:
		IWP.updateSlices(true);

	}
	
	/**
	 * Implements the low- and high- pass filtering of objects based on a selected attribute.
	 * <p>
	 * Filters objects in the ARFF dataset by the selected attribute in filterComboBox, where objects
	 * with attribute value: min <= ATTR <= max  pass the filter, but objects with attribute values
	 * below min and above max are set to not passed.
	 * <p>
	 * The method only runs if the filterCheckBox is selected (and is also run WHEN the filterCheckBox is
	 * selected!).
	 * @param min
	 * @param max
	 */
	public void filterObjects() {
		
		// FIRST - unselect any object, to ensure they are all filtered correctly.
		unselectAnyObject();

		// TODO Re-Factor setFilterObj() and see how this method may also be re-factored...  
		objectFilter.filterObjects(true);
		
		// also set the boolean which tracks the objectFilter filterMaxReached
		//filterMaxReached = objectFilter.getFilterMaxReached();

		// and repaint the IWP:
		IWP.updateSlices(true);

	}




	/**
	 * Removes filter from objects in the ARFF dataset by the selected attribute in filterComboBox, where objects
	 * with attribute value: min <= ATTR <= max  pass the filter, but objects with attribute values
	 * below min and above max are set to not passed.
	 * <p>
	 * The method only runs if the filterCheckBox is selected (and is also run WHEN the filterCheckBox is
	 * selected!).
	 * @param attributeIndex The Attribute Index in ObjectDatasetHandler data which the filter should be removed from.
	 */
	public void removeFilterObjects(int attributeIndex) {

		// FIRST - unselect any object, to ensure they are all filtered correctly.
		unselectAnyObject();
		
		objectFilter.removeFilterObjects(attributeIndex);
		
		// also set the boolean which tracks the objectFilter filterMaxReached
		//filterMaxReached = objectFilter.getFilterMaxReached();

		// and repaint the IWP:
		IWP.updateSlices(true);

	}
	
	/**
	 * Removes filter from objects in the ARFF dataset by the selected attribute in filterComboBox, where objects
	 * with attribute value: min <= ATTR <= max  pass the filter, but objects with attribute values
	 * below min and above max are set to not passed.
	 * <p>
	 * The method only runs if the filterCheckBox is selected (and is also run WHEN the filterCheckBox is
	 * selected!).
	 * @param attributeIndex The Attribute Index in ObjectDatasetHandler data which the filter should be removed from.
	 */
	public void removeFilterObjects() {

		// FIRST - unselect any object, to ensure they are all filtered correctly.
		unselectAnyObject();
		
		objectFilter.removeFilterObjects();
		
		// also set the boolean which tracks the objectFilter filterMaxReached
		//filterMaxReached = objectFilter.getFilterMaxReached();

		// and repaint the IWP:
		IWP.updateSlices(true);

	}


	/**
	 * Sets both the filterMax and filterMin JSpinner objects to enabled or disabled, depending on
	 * what boolean is passed via setEnabled.
	 * @param setEnabled
	 */
	public void setFilterSpinnersEnabled(boolean setEnabled) {

		java.awt.Component[] cps = filterMax.getComponents();
		filterMax.setEnabled(setEnabled);
		for(int a=0; a<cps.length;a++) {
			cps[a].setEnabled(setEnabled);
		}

		cps = filterMin.getComponents();
		filterMin.setEnabled(setEnabled);
		for(int a=0; a<cps.length;a++) {
			cps[a].setEnabled(setEnabled);
		}

	}


	/**
	 * This method will apply a user-selected Classifier to the current ARFF instances object.
	 * Each Instance will be tested, and the ARFF Classifier Class attribute will be modified appropriately.
	 */
	public void classifyObjects() {

		// TODO does any object need to be un-selected before this is run?!

		// Classify objects in datasetHandler, and set the Classifier Class value & imageHandler pixel value
		// for each object, based on ObjectIdentifier flagValues & whether filterOverClassifierCheckBox is selected:
		objectClassifier.classifyObjects(objectDataset, imageHandler, objIdentifier, 
				filterOverClassifierCheckBox.isSelected() );

		// and repaint the IWP:
		IWP.updateSlices(true);

	}



	/**
	 * This method will remove a user-selected Classifier to the current ARFF instances object.
	 * Each Instance will be tested, and if the ARFF Classifier Class attribute is no NONFEATURE, it
	 * will be set to NONFEATURE & the object pixel values altered in the image.
	 */
	public void removeClassifyObjects() {

		// TODO does any object need to be un-selected before this is run?!

		// Classify objects in datasetHandler, and set the Classifier Class value & imageHandler pixel value
		// for each object, based on ObjectIdentifier flagValues & whether filterOverClassifierCheckBox is selected:
		objectClassifier.classifyObjects(objectDataset, imageHandler, objIdentifier, 
				ObjectDataContainer.FEATUREATR );

		// and repaint the IWP:
		IWP.updateSlices(true);

	}



	/**
	 * 
	 * This method implements the Attribute Selection JButton functionality.  It will select a set of objects whose
	 * attribute (as selected in the attrSelComboBox) value is between attrMax value and attrMin value.  If no
	 * objects exist between these values, no object is selected and the fact no object exists is indicated on the
	 * Text Area.
	 * <p>
	 * This method passes a set of object references (in the form of an integer arraylist containing objNo references
	 * to objects) to the Object Selection Thread, which takes care of selecting, viewing and moving between objects 
	 * in the selection.
	 * 
	 */
	public void attributeSelectObjects() {

		// TODO CHECK THIS METHOD

		// FIRST - if an objectSelectionThread is ALREADY ACTIVE - need to end it:
		endObjectSelectionThread(); // TODO -> move this method to objectSelectionRunner/Thread?

		// Determine state of attCheckBox to determine whether ALL objects should be used in selecting this object:		
		boolean useAllObjects = attrCheckBox.isSelected();

		//get the min and max vals for this attribute from the attrMin and attrMax spinners:
		double attrMinVal = (double)attrMinModel.getValue();		
		double attrMaxVal = (double)attrMaxModel.getValue();

		// From the attrVal, attrMinVal & attrMaxVal want to compute an array list of obj references in which the 
		// attribute in attrVal is between attrMinVal and attrMaxVal:
		attrObjRefList = computeObjRefsFromAttrRange( attrMinVal, attrMaxVal, useAllObjects );

		// if no objects selected return a message to the User:
		if(attrObjRefList.size() == 0 ) {
			// Im not sure this code can ever be reached, but its here for safety!
			IJ.showMessage("No Objects in Range","No Objects are in range [min] to [max].");
			return;
		}

		// Now -> Create the ObjectSelectionThread from the objects ArrayList:

		// TODO need to refactor this code to the objectSelectionRunner/Thread class:

		//First - set objSelThreadActive to true -> to indicate to program that a series of objects are being
		// traversed:
		objSelThreadActive = true;
		// This allows keyListeners to become active in the keyPressed() method to move through
		// the objects array selection & cancel the ObjectSelectionThread run.

		objectSelectionRunner = new ObjectSelectionThread(attrObjRefList);

		objectSelectionThread = new Thread( objectSelectionRunner );

		objectSelectionThread.start();


	}


	/**
	 * This method will return an ArrayList which contains the object references of all objects which have an
	 * attribute value between and including attrMinVal and attrMaxVal.  If useAllObjects is TRUE, then all
	 * objects independent of their classification are returned, otherwise only UNCLASSIFIED Objects are
	 * returned.
	 * <p>
	 * 
	 * 
	 * @param attrMinVal
	 * @param attrMaxVal
	 * @param useAllObjects
	 * @return
	 */
	public ArrayList<Integer> computeObjRefsFromAttrRange(double attrMinVal, double attrMaxVal,
			boolean useAllObjects) {

		// A new Integer ArrayList to hold onto the obj No references:
		ArrayList<Integer> orl = new ArrayList<Integer>();

		// First, check if the atrVal, attrMinVal, attrMaxVal values match the instance variables of these
		// This would mean the current attribute, minval and maxval have already been parsed through this algorithm
		// And therefore there is no need to re-run it!

		// In this case - just return the attrObjRefList instance variable, which contains the computed obj refs
		// from a previous run of this method!

		// ACTUALLY - the obj references in attrObjRefList will depend on whether ALL was checked or not
		// Its too complicated to worry about all these possible conditions for this, so just run this algorithm
		// to select a new set of objects from the ARFF dataset no matter what!

		//if( attrVal.equals(this.attrVal) && attrMinVal == this.attrMinVal && attrMaxVal == this.attrMaxVal ) {
		// First - need to INCREMENT the objRefIndex by 1 to ensure the NEXT object is selected in the
		// returned attrObjRefList in the attributeSelectRandomObject() method:
		//attrObjIndex = attrObjIndex + 1;  
		// NOT USING attrObjIndex to move through objects now, as object selection thread takes care of it!

		//IJ.showMessage("Attr vals same as before - returning the current attrObjRefList...");

		//return attrObjRefList;
		//}


		// Set the instance variables:
		//this.attrVal = attrVal; NOT USED - can use attrSelComboBox selected index to get the attribute!
		//this.attrMinVal = attrMinVal;
		//this.attrMaxVal = attrMaxVal;

		// Sort the ARFF dataset on the selected attribute:
		// to retrieve the correct attribute do not even need the attribute string!
		// Can retrieve the correct attribute from the 'attributes' arraylist by retrieving the index-1 of the 
		// selected index on the attrSelComboBox
		// Its -1 as the attrSelComboBox contains a String array which has had a Blank String appended to the start
		// of it.
		// Note, still need the attribute String value, to check if the attribute selected is the same between runs
		// of this method (see comments, code and return statement above)

		objectDataset.sort( (attrSelComboBox.getSelectedIndex() - 1 ) );

		// showArffDatatable(arff);

		// IJ.showMessage("sorted arff by selected Attr: "+attributes.get( attrSelComboBox.getSelectedIndex() - 1 ).toString() 
		//				+ "\n\n attrVal: " + attrVal);	


		// Alternatively, can just find the attribute in arff based on its name:
		//arff.sort( arff.attribute(attrVal) );

		// fill the orl ArrayList with objNo's between minVal and maxVal:
		//int minValIndex = 0;
		boolean minValFound = false;
		//int maxValIndex = 0;
		//boolean maxValFound = false;

		// Set up a loop through arff:
		for(int a=0; a<objectDataset.numInstances(); a++) {

			//get the value of the attrVal attribute at index a:
			//IJ.showMessage("Arff attr value: " + arff.instance(a).value(  convertIndexToAttribute( (attrSelComboBox.getSelectedIndex()-1) )  )  );
			// IJ.showMessage("Arff attr value by index: " + arff.instance(a).value( (attrSelComboBox.getSelectedIndex()-1) )   );
			//double val = Double.parseDouble( arff.attribute(attrVal).value(a) );
			double val = objectDataset.get(a, (attrSelComboBox.getSelectedIndex()-1) );

			// while the minVal has not been found, compare val to the attrMinVal
			if(minValFound == false) {
				if(val >= attrMinVal) {
					// once val is greater or equal to attrMinVal, start collecting the obj no values:

					//minValIndex = a; //set minValIndex to a, as the val is now equal or greater than attrMinVal
					// Do not need this, as getting the objNo's in this for loop!

					minValFound = true; //have found the minVal, so set to true - will stop the for loop calling this code

					// AND collect the FIRST obj No from arff at this index into orl!
					if(useAllObjects == true) {
						// if collecting all objects, just collect this obj no value:
						//orl.add( Integer.parseInt( arff.attribute("Obj No.").value(a) ) );
						// orl.add( (int)datasetHandler.instance(a).value( datasetHandler.attribute(ObjectDataContainer.OBJNO) ) );
						orl.add( (int) objectDataset.get(a, ObjectDataContainer.OBJNO) );

						// IJ.showMessage("First object ObjNo [all TRUE]: "+(int)arff.instance(a).value(objNo)  );

					}
					else {
						// datasetHandler.get(a, ObjectDataContainer.MANUALCLASS)
						//datasetHandler.instance(a).value( datasetHandler.attribute(ObjectDataContainer.MANUALCLASS) )
						// if not collecting all objects, first check this object is UNCLASSIFIED:
						//if( arff.attribute("Manual Class").value(a).equals( UNCLASSIFIEDATR ) ) {
						if( objectDataset.get(a, ObjectDataContainer.MANUALCLASS) 
								== ObjectDataContainer.MAN_UNCLASSIFIED_INDEX ) {
							// only store the object obj ref if the manual class is UNCLASSIFIED:

							//orl.add( Integer.parseInt( arff.attribute("Obj No.").value(a) ) );
							// orl.add( (int)datasetHandler.instance(a).value( datasetHandler.attribute(ObjectDataContainer.OBJNO) ) );
							orl.add( (int) objectDataset.get(a, ObjectDataContainer.OBJNO) );
							// IJ.showMessage("First object ObjNo [all FALSE]: "+(int)arff.instance(a).value(objNo)  );

						}
					}
				}
			}

			// This code is only executed once minVal has been found:
			// MUST use ELSE IF for the test minValFound == false, otherwise this code is called in same loop
			// as when the FIRST object is found!
			else if(minValFound == true) {
				//only start looking for the max val once the min val has been found!

				// Actually here we could start extracting the objRef indexes and putting them into the orl arraylist
				// as the loop is now going through the items.

				// Just make sure to take the FIRST Obj Ref in the previous if statement (if minValFound -> if val >=)
				// (as the FIRST Obj Ref will be when val is >= attrMinVal.
				// AND to start taking obj refs AFTER the if statement below (as it contains the BREAK clause).

				if(val > attrMaxVal) {
					//maxValIndex = a-1; //set maxValIndex to (a-1), as val is now greater than attrMaxVal, so the LAST
					// index value was where val was equal or below attrMaxVal.
					// do not need this anymore, as data is collected in THIS for loop
					//maxValFound = true; // have found the maxVal, so set to true.
					// do not need this anymore, as the break clause below ENDS this for loop!
					break; // break out of the for loop.
				}

				// If the for loop has not been broken collect the obj ref from arff at this index!
				if(useAllObjects == true) {
					// if collecting all objects, just collect this obj no value:
					//orl.add( Integer.parseInt( arff.attribute("Obj No.").value(a) ) );
					// orl.add( (int)datasetHandler.instance(a).value( datasetHandler.attribute(ObjectDataContainer.OBJNO) ) );
					orl.add( (int) objectDataset.get(a, ObjectDataContainer.OBJNO) );
					// IJ.showMessage("Next object ObjNo [all TRUE]: "+(int)arff.instance(a).value(objNo)  );

				}
				else {
					// if not collecting all objects, first check this object is UNCLASSIFIED:
					//if( arff.attribute(MANCLASS).value(a).equals( UNCLASSIFIEDATR ) ) {
					if( objectDataset.get(a, ObjectDataContainer.MANUALCLASS) 
							== ObjectDataContainer.MAN_UNCLASSIFIED_INDEX ) {
						// only store the object obj ref if the manual class is UNCLASSIFIED:
						//orl.add( Integer.parseInt( arff.attribute("Obj No.").value(a) ) );
						// orl.add( (int)datasetHandler.instance(a).value( datasetHandler.attribute(ObjectDataContainer.OBJNO) ) );
						orl.add( (int) objectDataset.get(a, ObjectDataContainer.OBJNO) );
						// IJ.showMessage("Next object ObjNo [all FALSE]: "+(int)arff.instance(a).value(objNo)  );

					}
				}

			}

		}

		// Finally, dont forget to re-sort arff on the objNo attribute!
		objectDataset.sort( objectDataset.attribute(ObjectDataContainer.OBJNO) );

		// IJ.showMessage("re-sorted ARFF by obj No");

		// showArffDatatable(arff);

		// AND - reset the attrObjIndex to 0!
		//attrObjIndex = 0;  DO NOT NEED REFS TO attrObjIndex anymore, as the objSelectionThread takes care of object
		// referencing!

		//IJ.showMessage("resetted attrObjIndex: "+attrObjIndex);


		return orl; // this now contains a list of all the obj ref integer values for objects between minVal and
		// maxVal for the selected attribute, and only UNCLASSIFIED Objects by Manual Class IF the
		// useAllObjects is FALSE.

	}

	/**
	 * 
	 * This class represents a Thread for Object Selection.  
	 * 
	 * @author stevenwest
	 *
	 */
	public class ObjectSelectionThread implements Runnable {

		//ArrayList<ArrayList<Integer>> objects;

		ArrayList<Integer> objects;

		int index;

		/**
		 * Sets the instance variable, objects.  This contains the obj numbers for objects to be processed in the
		 * arff dataset.  Also initialises the 'index' variable to 0, the first index.
		 * @param objects
		 */
		//public ObjectSelectionThread(ArrayList<ArrayList<Integer>> objects ) {

		//this.objects = objects;

		//index = 0;

		//}

		/**
		 * Sets the instance variable, objects.  This contains the obj numbers for objects to be processed in the
		 * arff dataset.  Also initialises the 'index' variable to 0, the first index.
		 * @param objects
		 */
		public ObjectSelectionThread(ArrayList<Integer> objects ) {

			this.objects = objects;

			index = 0;

		}

		/**
		 * The run method loops through all items in the objects array.  It retrieves the objects First Pixel from
		 * the hashmap, and selects the object in the image.  It resizes the image canvas to present the object to
		 * the user, and waits for the user to classify the object.  Once the object is classified by the user (in
		 * the EDT thread), the next object is presented to the user as above.  This continues until all the objects
		 * in the objects array have been presented and classified by the user.
		 */
		@Override
		public void run() {

			// IJ.showMessage("run(); - objSelThreadActive: "+objSelThreadActive);

			//if(objSelThreadActive == true) {
			//Only run the processing of this method if objSelThreadActive is true.
			// Provides control over object selection by this thread - when the thread is ended by the user
			// the objSelThreadActive is set to false, and the processing and pausing of this thread do not
			// occur, but this run() method is allowed to complete, thus terminating the thread.

			//IJ.showMessage("New Thread Running - obj number: "+objects.size() );

			//IJ.showMessage("Processing Obj. No.: "+index);

			// Retrieve First Pixel Co-ordinates & object bounding box from ARFF:

			// first, get the objNo from the objects array at index:
			int objNo = objects.get(index);

			// now, use this objNo to get the attributes for this obj from ARFF:
			// the reference to the object in ARFF is [objNo-1], as indexes start at 0, while objNo starts at 1.

			int x = (int)objectDataset.instance(objNo-1).value( objectDataset.attribute(ObjectDataContainer.X1) );  // x1
			int y = (int)objectDataset.instance(objNo-1).value( objectDataset.attribute(ObjectDataContainer.Y1) );  // y1
			int z = (int)objectDataset.instance(objNo-1).value( objectDataset.attribute(ObjectDataContainer.Z1) );  // z1

			int xMi = (int)objectDataset.instance(objNo-1).value( objectDataset.attribute(ObjectDataContainer.XMIN) );  // xMin
			int xMa = xMi + (int)objectDataset.instance(objNo-1).value( objectDataset.attribute(ObjectDataContainer.XLENGTH) );  // xMax
			int yMi = (int)objectDataset.instance(objNo-1).value( objectDataset.attribute(ObjectDataContainer.YMIN) );  // yMin
			int yMa = yMi + (int)objectDataset.instance(objNo-1).value( objectDataset.attribute(ObjectDataContainer.YLENGTH) );  // yMax
			int zMi = (int)objectDataset.instance(objNo-1).value( objectDataset.attribute(ObjectDataContainer.ZMIN) );  // zMin
			int zMa = zMi + (int)objectDataset.instance(objNo-1).value( objectDataset.attribute(ObjectDataContainer.ZLENGTH) );  // zMax


			// below is redundent, as objects only contains the objNo now!
			//int x = objects.get(index).get(0);
			//int y = objects.get(index).get(1);
			//int z = objects.get(index).get(2);

			//int xMin = objects.get(index).get(3);
			//int xMax = objects.get(index).get(4);
			//int yMin = objects.get(index).get(5);
			//int yMax = objects.get(index).get(6);
			//int zMin = objects.get(index).get(7);
			//int zMax = objects.get(index).get(8);

			// First, set the objSelectedPixVal to the original value of this object:
			//objClassificationVal = thresholdImp.getStack().getProcessor( (z+1) ).get(x, y);
			// int objSelectedVal = thresholdImp.getStack().getProcessor( (z+1) ).get(x, y);
			int objSelectedVal = imageHandler.getPixelValueThresholded(x, y, z);
			//use Z plus 1 as the processor indexes are 1-based

			// Set the Previous Selected Object to its unselected pixel value:
			unselectCurrentObject(objSelectedVal);

			// IJ.showMessage("Obj Selected Pixel Value: "+objSelectedPixVal);

			//Select the object in the image:
			// set the pixel value to SELECTED IF the pixel value is UNCLASSIFIED
			// Need to check through these as the object may be already classified as the user
			// moves through the objects array..

			setCurrentObjectToSelectedPixVal(objSelectedVal,x,y,z, false);

			//if(objClassificationVal == UNCLASSIFIED) {
			//	firstPix = borderObjPixProcessing3D.selectObj3d26(thresholdImgInt, x, y, z, SELECTED);
			//use Z as selectObj3d26() method z is 0-based.
			//}
			//else if(objClassificationVal == CLASSIFIED_FEATURE) {
			//	firstPix = borderObjPixProcessing3D.selectObj3d26(thresholdImgInt, x, y, z, CLASSIFIED_FEATURE_SELECTED);
			//use Z as selectObj3d26() method z is 0-based.
			//}
			//else if(objClassificationVal == CLASSIFIED_NONFEATURE) {
			//	firstPix = borderObjPixProcessing3D.selectObj3d26(thresholdImgInt, x, y, z, CLASSIFIED_NONFEATURE_SELECTED);
			//use Z as selectObj3d26() method z is 0-based.
			//}
			//else if(objClassificationVal == CLASSIFIED_CONNECTED) {
			//	firstPix = borderObjPixProcessing3D.selectObj3d26(thresholdImgInt, x, y, z, CLASSIFIED_CONNECTED_SELECTED);
			//use Z as selectObj3d26() method z is 0-based.
			//}

			//THEN set z projection & re-draw the original imp onto the projected imp in IWP:
			IWP.setSliceProjection( new int[] { (zMi+1), (zMa) } );
			//use Z + 1 as SliceProjection method z is 1-based.
				// Do NOT +1 to zMa, as zMa indicates the first zSlice OUTSIDE the object bounds!
			//IWP.updateSlices(true);

			//IJ.showMessage("Set Slice Projection");

			// And set objSelected to true:
			//objSelected = true;

			// Zoom and project image appropriately for user assessment:
			// use IWP methods:
			// TODO set options for the border value (currently here set as 6):
			IWP.cc.setZoom(xMi, xMa, yMi, yMa, 6);

			//IJ.showMessage("Set Zoom");

			// Update the ObjectInfo TextBox:
			//as ObjSelThreadActive is true, the text box will display information relating to this...
			updateObjInfo();

			//PAUSE this thread - using dw.pause():
			//The Thread needs to be resumed in the assessment methods!
			//To inform them that a thread is paused, set a local boolean to test!
			// doing this at the START of run, so its only set ONCE:

			// IJ.showMessage("Thread about to be paused - thread obj ref: " + this.toString() );

			Object_Manager.this.pause();  // when the thread resumes, it will start running from this point...

			// IJ.showMessage("Thread just un-paused - thread obj ref: " + this.toString() );

			if(objSelThreadActive == true) {

				run(); //as the thread will continue from the pause() method, need to call run() again here
				// if objSelThreadActive is true - to start this method from the beginning!
			}
			else { //need the else here otherwise the last code is run multiple times by the same thread!

				//IJ.showMessage("Thread terminating & calling notify() on EDT - thread obj ref: " + this.toString() );


				//  NOTIFYING IWP to recall the EDT, but this thread will end so quickly, its almost pointless!
				//synchronized(IWP) {
				//IWP.notify(); // calling notify() to resume the EDT Thread IF its been paused on this object,
				// which is the case if a new obj sel thread is set up ON TOP of
				// the current one...
				//}

			} //end else


		} //end run()

		/**
		 * Increment the index - the index will not exceed the maximum ( objects.size() -1 ), but will
		 * move the index to the beginning -> 0.
		 */
		public void incrementIndex() {
			// before changing the index, if the object selected at the current index pixel value is SELECTED,
			// should change it back to UNCLASSIFIED:

			// setCurrentObjectToUnselectedPixVal();

			// THIS IS NOT REQUIRED AS IT HAPPENS IN THE RUN() METHOD

			// first, get the objNo from the objects array at index:
			//int objNo = objects.get(index);

			//Retrieve First Pixel Co-ordinates:
			//int x = (int)arff.instance(objNo-1).value(x1);  // x1
			//int y = (int)arff.instance(objNo-1).value(y1);  // y1
			//int z = (int)arff.instance(objNo-1).value(z1);  // z1

			// get the pixel value of the current object:
			//int objSelectedPixVal = thresholdImp.getStack().getProcessor( (z+1) ).get(x, y);

			// if the current selected object has not been classified by the user (if its value is equal to SELECTED)
			// then need to set its value back to UNCLASSIFIED:
			//if(objSelectedPixVal == SELECTED) {
			//	firstPix = borderObjPixProcessing3D.selectObj3d26(thresholdImgInt, x, y, z, UNCLASSIFIED);
			//use Z as selectObj3d26() method z is 0-based.
			//}
			// Else If - the object WAS classified by the USER, MUST set its pixel value to the UNSELECTED version
			// of its pix value
			//else if(objSelectedPixVal == CLASSIFIED_FEATURE_SELECTED) {
			//	firstPix = borderObjPixProcessing3D.selectObj3d26(thresholdImgInt, x, y, z, CLASSIFIED_FEATURE);
			//use Z as selectObj3d26() method z is 0-based.
			//}
			//else if(objSelectedPixVal == CLASSIFIED_NONFEATURE_SELECTED) {
			//	firstPix = borderObjPixProcessing3D.selectObj3d26(thresholdImgInt, x, y, z, CLASSIFIED_NONFEATURE);
			//use Z as selectObj3d26() method z is 0-based.
			//}
			//else if(objSelectedPixVal == CLASSIFIED_CONNECTED_SELECTED) {
			//	firstPix = borderObjPixProcessing3D.selectObj3d26(thresholdImgInt, x, y, z, CLASSIFIED_CONNECTED);
			//use Z as selectObj3d26() method z is 0-based.
			//}

			// Above ensures the image will only display one object as selected at a time, and prevent any infinite
			// loops by setting selected objects to their un-selected pixel value...

			index = index + 1;
			if(index == objects.size() ) {
				index = 0;
			}
		}


		/**
		 * Decrement the index - the index will not be set lower than 0, the first index, but will
		 * move the index to the end -> objects.size() -1.
		 */
		public void decrementIndex() {
			// before changing the index, if the object selected at the current index pixel value is SELECTED,
			// should change it back to UNCLASSIFIED:

			// setCurrentObjectToUnselectedPixVal();

			// THIS IS NOT REQUIRED AS IT HAPPENS IN THE RUN() METHOD

			// first, get the objNo from the objects array at index:
			//int objNo = objects.get(index);

			//Retrieve First Pixel Co-ordinates:
			//int x = (int)arff.instance(objNo-1).value(x1);  // x1
			//int y = (int)arff.instance(objNo-1).value(y1);  // y1
			//int z = (int)arff.instance(objNo-1).value(z1);  // z1

			// get the pixel value of the current object:
			//int objSelectedPixVal = thresholdImp.getStack().getProcessor( (z+1) ).get(x, y);

			// if the current selected object has not been classified by the user (if its value is equal to SELECTED)
			// then need to set its value back to UNCLASSIFIED:
			//if(objSelectedPixVal == SELECTED) {
			//	firstPix = borderObjPixProcessing3D.selectObj3d26(thresholdImgInt, x, y, z, UNCLASSIFIED);
			//use Z as selectObj3d26() method z is 0-based.
			//}
			// Else If - the object WAS classified by the USER, MUST set its pixel value to the UNSELECTED version
			// of its pix value
			//else if(objSelectedPixVal == CLASSIFIED_FEATURE_SELECTED) {
			//	firstPix = borderObjPixProcessing3D.selectObj3d26(thresholdImgInt, x, y, z, CLASSIFIED_FEATURE);
			//use Z as selectObj3d26() method z is 0-based.
			//}
			//else if(objSelectedPixVal == CLASSIFIED_NONFEATURE_SELECTED) {
			//	firstPix = borderObjPixProcessing3D.selectObj3d26(thresholdImgInt, x, y, z, CLASSIFIED_NONFEATURE);
			//use Z as selectObj3d26() method z is 0-based.
			//}
			//else if(objSelectedPixVal == CLASSIFIED_CONNECTED_SELECTED) {
			//	firstPix = borderObjPixProcessing3D.selectObj3d26(thresholdImgInt, x, y, z, CLASSIFIED_CONNECTED);
			//use Z as selectObj3d26() method z is 0-based.
			//}

			// Above ensures the image will only display one object as selected at a time, and prevent any infinite
			// loops by setting selected objects to their un-selected pixel value...

			index = index - 1;
			if(index == -1) {
				index = (objects.size() - 1);
			}
		}

	} //end class ObjectSelectionThread


	/**
	 * This method pauses the thread which calls it.  This is used to pause the Processing Thread
	 * (ProcessThread) which is used to execute the process() method in a SM Algorithm.  Calling this
	 * method will pause this thread, which is required when setting up a GUI interface for a user
	 * and waiting for their input before moving on to the next processing step.
	 * <p>
	 * This method allows the programmer to make the processing thread wait while the user adjusts a
	 * GUI interface for indicate appropriate input for subsequent steps in the algorithm.  One of the
	 * steps in the GUI interface needs to indicate the user have finished their input, and this Listener
	 * Object should call the corresponding method to this one, which is resume().
	 * <p>
	 * This should only be called in the process() method of the SM algorithm, so ensure it only pauses
	 * the Processing Thread.
	 * <p>
	 * Details: This calls wait() on this object, DialogWindow. If called from the process() method, it will
	 * pause the processing thread on the DialogWindow object.  This thread will awake and continue executing
	 * the process() method when resume() is called, which calls notify on this object to awake the thread.
	 */
	public void pause() {
		synchronized(this) {
			try {
				wait();
			}
			catch(InterruptedException e) {

			}
		}
	}


	/**
	 * This method resumes the paused Processing Thread (ProcessThread) which execute the process() method
	 * in the SM Algorithm.  If the SM Algorithm requires user input during its execution, this is typically
	 * performed with a GUI interface for the user.  However, this is set up with GUI components which have
	 * Listener objects assigned to them -> processing which occurs on the EDT.
	 * <p>
	 * In order to stop the SM Algorithm from setting up the GUI and attempting to plough on with image processing,
	 * when the User still has to give input, the Programmer can pause the processing thread [with the method pause()],
	 * and can resume the thread once the user has put the appropriate input.  Resuming the thread is achieved with
	 * this method.
	 * <p>
	 * This method should only be called inside a Listener Objects's method to indicate user input is complete, and
	 * only in a situation where it will be called AFTER pause() has been executed (which the first condition satisfies).
	 * <p>
	 * Details: This calls notify() on this object, DialogWindow.  Since the wait() method was also called on this object,
	 * notify will awake that thread -> which will be the processing thread.
	 */
	public void resume() {
		synchronized(this) {
			notify();
		}
	}



	/**
	 * This method ends the object selection thread: Sets the objSelThreadActive to FALSE, then resumes the thread.
	 * As the objSelThreadActive boolean is now false, no objects are selected by this thread, the thread is not
	 * paused, and it is allowed to complete & terminate by finishing its run method.
	 */
	public void endObjectSelectionThread() {

		if(objSelThreadActive == true){
			// if an object selection thread is currently active, end it by calling endObjectSelectionThread()

			// first, set the currently selected object to its un-selected pixel value:
			// unselectCurrentObject(UNCLASSIFIED);
			unselectCurrentObject( selectedObj.getUnselectedPixelValue() );

			// next, run the current selection as if it were a click on the background:
			// This sets objSelected to false, and re-sets firstPix to its default val (0,0,0)
			// the setSelected is not used in this situation, so it can be true or false, I just put false
			// by default:
			setCurrentObjectToSelectedPixVal(0,0,0,0,false);


			//and re-draw the image on IWP to update the un-selected image pixel value on the image:
			IWP.updateSlices(true);

			// now no object is selected, and the objSelected boolean is false - back to the original state of object
			// selection!

			// Now - terminate the objsect selection thread:

			// set objSelThreadActive to false, to force that thread to terminate when it is resumed.
			objSelThreadActive = false;
			this.resume(); // and resume the objSelThread!

			// And update the object Info panel:
			updateObjInfo();

		}

	}



	public void showArffDataset(Instances arff) {
		// FIRST -> give the IWP custom canvas the focus:
		// Ensures the custom canvas has focus after clicking on the plot button:
		IWP.cc.requestFocusInWindow();

		// MatrixPanel offers the initial scatterplot matrix panel - showing all attributes
		// as a 2D array of scatterplots. User can select one to move to the visualisePanel:
		//MatrixPanel mp = new MatrixPanel();

		// This goes directly to the VisualisePanel - using the first two attributes by default
		VisualizePanel mp = new VisualizePanel();

		mp.setInstances(arff);

		try {
			mp.setXIndex(0);  //set X to volVoxels by default
			mp.setYIndex(1); //set Y to sphericity by default
		} catch(Exception ex) {

		}

		// String plotName = arff.relationName();
		JFrame jf = new JFrame("Object Manager: Weka Attribute Selection Visualization");
		// jf.setSize(800, 600);
		jf.setSize(IWP.iw.getSize().width-60, IWP.iw.getSize().height-60);
		jf.setLocation(30,30);
		jf.getContentPane().setLayout(new BorderLayout());
		jf.getContentPane().add(mp, BorderLayout.CENTER);
		jf.addWindowListener(new java.awt.event.WindowAdapter() {

			public void windowClosing(java.awt.event.WindowEvent e) {
				jf.dispose();
			}
		});
		jf.setVisible(true);
	}



	public void showArffDatatable(Instances arff) {
		// FIRST -> give the IWP custom canvas the focus:
		// Ensures the custom canvas has focus after clicking on the plot button:
		// IWP.cc.requestFocusInWindow();

		// MatrixPanel offers the initial scatterplot matrix panel - showing all attributes
		// as a 2D array of scatterplots. User can select one to move to the visualisePanel:
		//MatrixPanel mp = new MatrixPanel();

		// This goes directly to the VisualisePanel - using the first two attributes by default
		ArffPanel ap = new ArffPanel(arff);


		// String plotName = arff.relationName();
		JFrame jf = new JFrame("Object Manager: Weka ARFF Viewer");
		jf.setSize(800, 600);
		// jf.setSize(IWP.iw.getSize().width-60, IWP.iw.getSize().height-60);
		jf.setLocation(30,30);
		jf.getContentPane().setLayout(new BorderLayout());
		jf.getContentPane().add(ap, BorderLayout.CENTER);
		jf.addWindowListener(new java.awt.event.WindowAdapter() {

			public void windowClosing(java.awt.event.WindowEvent e) {
				jf.dispose();
			}
		});
		jf.setVisible(true);
	}

	/**
	 * Deactivates a loaded classifier.  Used in classifierDeleteButon and classifierCheckBox to remove a
	 * loaded classifier from the dataset.
	 */
	public void deactivateLoadedClassifier() {
		// Set the classifierLoaded to FLASE:
		// classifierLoaded = false;

		//and inactivate the delete button:
		classifierDeleteButton.setEnabled(false);

		// and REMOVE the classifier from the ARFF Dataset -> reset all the Classifications of objects to BLANK:
		removeClassifyObjects();

		// Reset the classifier and attributes to the Rough Classifier:
		objectClassifier.setRoughClassifierAndAttributes();
	}


	/**
	 * Reactivate a loaded classifier.  Used in the classifierSelectButton action listener, and when the
	 * classifierCheckBox is re-checked on and a loadedClassifier object exists.
	 */
	public void activateLoadedClassifier() {

		// activate the classifierDeleteButton:
		classifierDeleteButton.setEnabled(true);

		// Give the IWP custom canvas the focus:
			// Ensures the custom canvas has focus after clicking on the select button:
		IWP.cc.requestFocusInWindow();

		// ensure no objects are selected in the image:
			// required for classifierObj() method??  As this adjusts the pixel value, and will not adjust
			// correctly if an object is a Selected_Pixel_Value!
		unselectAnyObject();

		// AND apply the Classifier to all data:
		classifyObjects();

	}
	
	/**
	 * Call this method whenever the Filter is adjusted -> either the combobox is changed, or
	 * the filter spinners are changed.  Call AFTER the change has changed the filter itself!
	 */
	public void recomputeLoadedClassifierAfterFilter() {
		
		if(filterOverClassifierCheckBox.isSelected() ) {
			if(objectClassifier.isLoaded() ) {
				activateLoadedClassifier();
			}
		}
		
	}



	/**
	 * Sets the Attributes onto all Instances objects:  arff, manClassArffBuffer[arffBuffer], unclassified,
	 * classified, unclassifiedFirstPix, classifierAttributes, classifierAttributesFirstPix,
	 * classifierAttributesNoFilterNoClassifier.
	 * <p>
	 * 
	 */
	public void setAttributesOnInstances() {

		attributes = new ArrayList<Attribute>();
		attributes.addAll( ObjectDataContainer.returnAllObjectMeasuresClassificationsAttributes() );

		objectDataset = new DatasetWrapper(dw.getCurrentOutputFile().getName(), 
				ObjectDataContainer.returnAllObjectMeasuresClassificationsAttributes() );

		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		//attributes.add( ObjectDataContainer.returnAttribute(ObjectDataContainer.IMAGEINDEX) );
		attributes.add( ObjectDataContainer.returnStringAttribute(ObjectDataContainer.IMAGENAME) );
		attributes.addAll( ObjectDataContainer.returnObjectMeasuresAttributes() );
		// attributes.add( MCIB_SM_DataObj.returnManClassAttributeNoUnclassified() );
		attributes.add( ObjectDataContainer.returnManClassAttribute() );

		loadManualClassificationData(attributes);

	}


	/**
	 * This will set the current filter using the filterIndexVal, filterMinVal, filterMaxVal values,
	 * which store the previous filters values.  Only set if filterIndexVal is greater than 0.
	 * <p>
	 * This method is called when the user moves between images using nextButton or previousButton.
	 * It will re-setup the filter on the new image, based on the settings in the previous image,
	 * including:  The Attribute chosen to filter on, low and high pass filter set, and computing
	 * whether a low pass filter was set on filterMax (ie if filterMaxReached is false) or not
	 * (deals with the bug that a low pass filter may inadvertently be set on an image if the
	 * previous ones largest object was smaller than the largest object(s) in the current image).
	 */
	public void setFilter() {
		
		int attrIndex = objectFilter.getAttributeIndex() + 1;
		
		if( attrIndex > 0) {
			// only setup the filter if its actually set on an Attribute!
			
			// equivalent to filterCheckBox.doClick() :
				filterCheckBox.setSelected(true);
				filterViewButton.setEnabled(true);
				// and select it:
				if(filterViewButton.isSelected() == false) {
					//manualViewButton.setSelected(true);
					filterViewButton.doClick();
				}
	
				// enable the filterComboBox:
				filterComboBox.setEnabled(true);
				
			
			// REMOVE ActionListener(s) from filterComboBox -> the ActionListener will delete the ObjectFilter Fitler
				// values!
			ActionListener[] als = filterComboBox.getActionListeners();
			for(int a=0; a<als.length; a++) {
				filterComboBox.removeActionListener(als[a]);
			} // must remove action listeners to prevent actionEvent trigger with setSelectedIndex below:
			
			filterComboBox.setSelectedIndex( attrIndex );
			
			// now run the select parts of the filterComboBox ActionListener required to setup the filterMin/Max Spinners
			
			// Determine the Min and Max values for the selected Attribute:
			// Loop through all instances, and record the min and max values for selected attribute.

			attrMinVal = objectDataset.getMinValue( (attrIndex-1) );
			attrMaxVal = objectDataset.getMaxValue( (attrIndex-1) );
			
			//IJ.showMessage("dataset: min: "+attrMinVal+" max: "+attrMaxVal+
				//	      " filter: min: "+objectFilter.filterMin+" max: "+objectFilter.filterMax);

			// Now need to compute the min and max values for the spinners, by determining which is
				// smaller/bigger for min/max values:
			// objectDataset values, OR the objectFilter values?
			attrMinVal = Math.min(attrMinVal, objectFilter.filterMin);
			attrMaxVal = Math.max(attrMaxVal, objectFilter.filterMax);
				// This ensures the Spinners will cover the full range from min to max:
					// whether that is the CURRENT IMAGES min/max values, or the CURRENTLY SET Filters min/max values.

			if(objectDataset.getStepSize(filterComboBox.getSelectedIndex()-1) == 0.001) {
				//IJ.showMessage("Before Round min + max val: "+attrMinVal+" "+attrMaxVal);
				attrMaxVal = ( (double)Math.floor(attrMaxVal * 1000d) / 1000d ) + 0.001;
				attrMinVal = ( (double)Math.floor(attrMinVal * 1000d) / 1000d );
				// IJ.showMessage("Round min + max val: "+attrMinVal+" "+attrMaxVal);
				filterMaxModel = new SpinnerNumberModel(objectFilter.filterMax,attrMinVal,attrMaxVal, 0.001 );
				filterMinModel = new SpinnerNumberModel(objectFilter.filterMin,attrMinVal,attrMaxVal, 0.001 );
				//filterMinModel = new SpinnerNumberModel(attrMinVal,0.0,attrMaxVal, 0.001 );
			}
			else {
				//filterMaxModel = new SpinnerNumberModel((int)attrMaxVal,(int)attrMinVal,(int)attrMaxVal, 1 );
				//filterMinModel = new SpinnerNumberModel((int)attrMinVal,(int)attrMinVal,(int)attrMaxVal, 1 );
				filterMaxModel = new SpinnerNumberModel(objectFilter.filterMax,attrMinVal,attrMaxVal, 1.0 );
				filterMinModel = new SpinnerNumberModel(objectFilter.filterMin,attrMinVal,attrMaxVal, 1.0 );
				//filterMinModel = new SpinnerNumberModel(attrMinVal,0.0,attrMaxVal, 1.0 );
			}

			filterMax.setModel(filterMaxModel);
			filterMin.setModel(filterMinModel);

			// Activate the filterMax and filterMin components:
			setFilterSpinnersEnabled(true);
			
			// re-add the listener(s) to the filterComboBox:
			for(int a=0; a<als.length; a++) {
				filterComboBox.addActionListener(als[a]);
			}

			// also set the filterOverClassifierCheckBox!
			filterOverClassifierCheckBox.setSelected( om_ProcedureSettings.isApplyFilterOverClassifier() );
			
			// finally, actually run filterObjects() in this class (also repaints IWP!)
			filterObjects(); // uses the attrIndex, filterMin & filterMax in the objectFilter
			
		}
		// set Filter based on PREVIOUS FILTER SETTINGS -> found in the xml file OM_ProcedureSettings.xml
		//IJ.showMessage("setFilter() -> filterIndexVal: "+filterIndexVal);
		//if( om_ProcedureSettings.getFilterIndexValue() > 0) {
			//filterCheckBox.doClick();
			//IJ.showMessage("setFilter() -> filterComboBox.setSelectedIndex() called: ");
			//filterComboBox.setSelectedIndex( om_ProcedureSettings.getFilterIndexValue() );
			//IJ.showMessage("setFilter() -> filterMin.setValue() called: ");
			//filterMin.setValue( om_ProcedureSettings.getFilterMinValue() );
			//IJ.showMessage("setFilter() -> filterMax.setValue() called: ");
			//if( objectFilter.filterMaxReached == false) {
				// only set filterMax is filterMaxReached is false - prevents bug of eliminating bigger objs in new image
				//filterMax.setValue( om_ProcedureSettings.getFilterMaxValue() );
			//}
			// also set the filterOverClassifierCheckBox!
			//filterOverClassifierCheckBox.setSelected( om_ProcedureSettings.isApplyFilterOverClassifier() );
		//}

	}

	/**
	 * Set the current classifier to the previous classifier from previous image.
	 */
	public void setClassifier() {

		if( objectClassifier.isLoaded() ) {
			// click check box to activate selectButton:
			classifierCheckBox.doClick();
			
			// Then - simulate setting the classifier:
			
			// set name of classifier into text field:
			classifierTextField.setText( objectClassifier.getClassifierFile().getName() );
			
			// AND activate the classifierDeleteButton:
			classifierDeleteButton.setEnabled(true);
			
			unselectAnyObject();

			classifyObjects(); // break this up ???  I cannot run this only in objectClassifier,
			// as it requires datasetHandler & imageHandler

			// objectClassifier.classifyObjects(datasetHandler, filterOverClassifierCheckBox.isSelected() );

			// activate the classifierViewButton:
			classifierViewButton.doClick();
			
			adjustObjSelRandSpinners(true); // make sure object selection random High Low spinners are ENABLED!

			// put focus onto canvas on IWP:
			IWP.cc.requestFocusInWindow();
			
		}
		
	}
	
	/**
	 * Activates the filterOverClassifierCheckBox only if:
	 * <p>
	 * Filter: checkbox is selected, combobox is set to an Atttribute
	 * <p>
	 * Classifier: Checkbox is selected, a Classifier is LOADED in objectClassifier.
	 * <p>
	 * This method is run when a Filter is applied and when a Classifier is applied.
	 */
	public void activateFilterOverClassifierCheckBox() {
		
		if(filterCheckBox.isSelected() && filterComboBox.getSelectedIndex() > 0 &&
			classifierCheckBox.isSelected() && objectClassifier.isLoaded() ) {
			filterOverClassifierCheckBox.setEnabled(true);
		}
		
	}
	
	/**
	 * This un checks and deactivates the filterOverClassifierCheckBox when called, and re-runs
	 * the filterObjects() and classifyObjects() methods to ensure the data and image object states
	 * are updated appropriately.
	 * <p>
	 * This is called if:
	 * <p>
	 * Classifier: Delete button is pressed, checkbox is unchecked.
	 * <p>
	 * Filter: ComboBox is set to BLANK (index 0), checkbox is unchecked.
	 * <p>
	 * The filterObjects() and classifyObjects() methods MUST be able to run correctly when a 
	 * filter/classifier has been DEACTIVATED (through the methods listed above).  This means
	 * if NO FILTER/CLASSIFIER is present, the filterObjects()/classifyObjects() method should
	 * NOT FILTER/CLASSIFY objects!
	 * <p>
	 * FILTER:  When comboBox set to 0 -> sets attributeIndex -> -1, when checkbox is unselected,
	 * sets attributeIndex -> -1.
	 * 
	 */
	public void deactivateFilterOverClassifier() {
		filterOverClassifierCheckBox.setSelected(false);
		filterOverClassifierCheckBox.setEnabled(false);
		filterObjects();
		classifyObjects();
	}

	@Override
	public void keyTyped(KeyEvent e) { }


	@Override
	public void keyPressed(KeyEvent e) {

		int keyCode = e.getKeyCode();
		IJ.setKeyDown(keyCode);


		// MANUAL OBJECT ANNOTATION:


		// if the F key is pressed, set the currently selected object as a FEATURE object
		if(keyCode == KeyEvent.VK_F) {

			// setManFeatureObj();
			// setObjManClass(CLASSIFIED_FEATURE, CLASSIFIED_FEATURE_SELECTED, FEATUREATR);
			setObjManClass(ObjectDataContainer.FEATUREATR, SelectedObject.FEATURE);


		}

		// if the D key is pressed, set the currently selected object as a NON-FEATURE object
		else if(keyCode == KeyEvent.VK_D) {

			// setManNonFeatureObj();
			// setObjManClass(CLASSIFIED_NONFEATURE, CLASSIFIED_NONFEATURE_SELECTED, NONFEATUREATR);
			setObjManClass(ObjectDataContainer.NONFEATUREATR, SelectedObject.NONFEATURE);

		}


		// if the C key is pressed, set the currently selected object as a CONNECTED object
		else if(keyCode == KeyEvent.VK_C) {

			// setManConnectedObj();
			// setObjManClass(CLASSIFIED_CONNECTED, CLASSIFIED_CONNECTED_SELECTED, CONNECTEDATR);
			setObjManClass(ObjectDataContainer.CONNECTEDATR, SelectedObject.CONNECTED);

		}


		// if the S key is pressed, set the currently selected object as an UNCLASSIFIED object
		else if(keyCode == KeyEvent.VK_S) {

			// setManUnclassifiedObj();			
			// setObjManClass(UNCLASSIFIED, UNCLASSIFIED_SELECTED, UNCLASSIFIEDATR);
			setObjManClass(ObjectDataContainer.UNCLASSIFIEDATR, SelectedObject.UNCLASSIFIED);

		}

		// OBJECT SELECTION MODE - MOVE THROUGH OBJECTS AND CANCEL SELECTION:


		// if the '<' [','] key is pressed, check objSelThreadActive boolean, if TRUE, then resume the thread
		//and move DOWN an object.
		else if(keyCode == KeyEvent.VK_COMMA) {

			// IJ.showMessage("COMMA PRESSED");

			if(objSelThreadActive == true) { 

				// IJ.showMessage("Running decrementIndex - current index: "+objectSelectionRunner.index);
				//move index DOWN:
				objectSelectionRunner.decrementIndex();
				// IJ.showMessage("new index: "+objectSelectionRunner.index);


				//and resume the thread:
				this.resume();
				// IJ.showMessage("Resuming objSel Thread - ending EDT thread");

			}

		}

		// if the '>' ['.'] key is pressed, check objSelThreadActive boolean, if TRUE, then resume the thread
		//and move UP an object.
		else if(keyCode == KeyEvent.VK_PERIOD) {

			// IJ.showMessage("FULLSTOP PRESSED");

			if(objSelThreadActive == true) { 

				// IJ.showMessage("Running incrementIndex - current index: "+objectSelectionRunner.index);
				//move index UP:
				objectSelectionRunner.incrementIndex();
				// IJ.showMessage("new index: "+objectSelectionRunner.index);

				//and resume the thread:
				this.resume();
				// IJ.showMessage("Resuming objSel Thread - ending EDT thread");

			}

		}

		// if the 'ENTER' key is pressed, check objSelThreadActive boolean, if TRUE, then END the thread
		else if(keyCode == KeyEvent.VK_ENTER) {

			if(objSelThreadActive == true) { 

				endObjectSelectionThread();

			}

		}


		// OBJECT VIEWING:

		else if(keyCode == KeyEvent.VK_L) {
			// Filter View Mode checked ON/OFF:
			setToFilterViewMode();
		}

		else if(keyCode == KeyEvent.VK_K) {
			//Classifier View Mode checked ON/OFF:
			setToClassifierViewMode();
		}

		else if(keyCode == KeyEvent.VK_M) {
			//Manual Classification View Mode checked ON/OFF:
			setToManualViewMode();
		}	

	}

	@Override
	public void keyReleased(KeyEvent e) {	}

}
