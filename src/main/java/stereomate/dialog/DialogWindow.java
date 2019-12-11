package stereomate.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.plaf.ComboBoxUI;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GUI;
import ij.io.FileInfo;
import stereomate.plugins.StereoMateAlgorithm;

/**
 * This DialogWindow class contains the basic code structure to generate a JFrame Dialog Window 
 * for setting up image processing for StereoMate algorithms, or any other image processing 
 * algorithms.
 * <p>
 * FRAMES ARE NOT SUPPORTED:
 * <p>
 * This framework will only handle images with slices and channels but not frames.  Images containing frames
 * will not be processed, but flagged in the log window.
 * <p>
 * <b> BASIC SETUP: </b>
 * <p>
 * dw = new DialogWindow3("ROI Di-Sector", this);
 * <p>
 * dw.addFileSelector("Image or DIR:"); //add FileSelector panel.
 * <p>
 * dw.addFileSelector("Image02:", dw.FILES_AND_DIRS, dw.MATCHED_INPUT);
 * <p>
 * dw.addActionPanel(); //add Action panel - for Cancel and Process buttons.
 * <p>
 * dw.setPlugInSuffix("_ROI");
 * <p>
 * dw.layoutAndDisplayFrame(); //display the DialogWindow.
 * <p>
 * <p>
 * <b> BASIC USAGE </b> (inside process() method of StereoMate Algorithms):	
 * <p>
 * 		//First, BEFORE THE WHILE LOOP, open the first file as an imp:
 * <p>		
 * 		imp = dw.getCurrentImp(0);
 * <p>
 *	//this method will continue to attempt to open files from the input from FileSelector0 until one 
 *  returns an imp.If no imp can be returned, this method will return null, 
 * but importantly the dw.currentFileIndex will EQUAL the dw.totalFileCount -> therefore the while loop 
 * will not execute..
 *	<p>		
 *		while(dw.currentFileIndex < dw.totalFileCount) {  
 * <p>
 * Alternatively:
 * <p>
 * 		while( dw.moreFilesToProcess() ) {
 *  <p>
 *  // This while loop ensures files are processed only while current file index is less than totalFileIndex...
 * <p>
 * 		//Do the Computations:
 *		<p>	
 *
 *			basicComputation(imp);
 * <p>			
 *			//save the imp to its designated output DIR:
 * <p>
 *			dw.saveImpAndClose(imp);
 *	<p>		
 *			// finally, increment the currentFileIndex in dw:
 * <p>
 *			dw.incrementCurrentFileIndex();
 * <p>
 *			//open the next file as an imp:
 * <p>
 *			imp = dw.getCurrentImp(0); 
 * <p>
 * //Need to do this at the end, to ensure the while loop checks the 
 *										//currentFileIndex and totalFileCount appropriately in getCurrentImp().
 *				//This returns NULL if the currentFileIndex == totalFileCount, but in this case the while loop
 *				//will also terminate!
 *	<p>		
 *		}//end while loop
 * <p>
 * 		//cleanup code:
 * <p>
 * <b> NOTE: </b> A 'for' loop is not used, as if a file cannot be opened in the input FileSelector, it is easier 
 * to just increment the currentFileIndex and pass through a while loop (which is what getCurrentImp() does), than
 * to increase the currentFileIndex and somehow communicate this to the 'for' loop to ensure the indexes remain
 * aligned. 
 * <p>
 * <b>SETTING UP THE DIALOG WINDOW: </b>
 * <p>
 * This class has one constructor - it requires a Title (for the DialogWindow itself), and a 
 * reference to a StereoMateAlgorithm (i.e. a class implementing the StereoMateAlgorithm 
 * Interface).  In practice, as this class will be called inside a class implementing 
 * StereoMateAlgorithm, a DialogWindow can be initiated with the variable 'this':
 * <p>
 * <b>DialogWindow dw = new DialogWindow("[Dialog Window Title here]", this);</b>
 * <p>
 * Two principal components can be added to the Dialog Window:
 * <p>
 * <i> - FileSelectors.
 * <p>
 *  - An ActionPanel. </i>
 *  <p>
 *  The JFrame can also have any standard Java component added to it, such as JPanels.
 *  <p>
 *  FileSelectors are panels which are a combination of a JComboBox & 
 * JButton which allow the user to select an image from the active images in ImageJ or from the
 * file system.  The First FileSelector also provides an output title for the output File or
 * DIR, and also the ability to limit or constrain the analysis.
 * <p>
 * An ActionPanel provides "cancel" and "process" buttons (where "process" can be set to any other
 * String), which allow the user to either cancel the Plugin at the DialogWindow, or (once appropriate
 * input has been selected) to begin the image processing.
 * <p>
 * <b>FileSelectors:</b>
 * <p>
 * Every DialogWindow class will require at least one FileSelector, to select image input. However, it
 * is the responsibility of the programmer to add these components.  
 * <p>
 * All FileSelectors contain a JComboBox, which contains all active images as well as previously selected
 * files from the file system, and a JButton which will open a JFileSelector to select an image or DIR from
 * the file system.
 * <p>
 * FileSelectors have a number of characteristics which can be set by the programmer:
 * <p>
 * <i> - The Type of Input which can be selected.
 * <p>
 *  - Whether the Input in the currently added FileSelector should match the input of the FIRST
 *  FileSelector. </i>
 *  <p>
 *  Type of input includes Files, DIRs or both.  Note this class does not check if the selected
 *  files are a valid image - this is down to the user. However, image processing does allow
 *  non-image files or invalid files to be skipped in the processing of a DIR, to ensure all
 *  images which can be opened will be processed in any processing run.
 *  <p>
 *  With some image processing a prerequisite may be selecting two (or more) inputs which have a matching
 *  (set of) image(s) to process together from both inputs. The FileSelector class supports this function by 
 *  requiring subsequent FileSelectors to match the first FileSelector's input,if input type 
 *  is set to MATCHED_INPUT.
 *  <p>
 *  See the FileSelector inner class for more information.
 * <p>
 * The method addActionPanel() provides the buttons to either cancel or proceed past the
 * dialog window.  If "Cancel" is pressed, the DialogWindow is disposed; if "Process" (which can
 * be renamed by the programmer) is pressed, the processImages() method is called in this class.
 * <p>
 * This method will display the action panel onto the JFrame supplied by this class.  The PROCESS
 * button will call the processImage() method in a separate thread - to ensure all image processing
 * occurs on a separate thread to the Event Dispatch Thread.
 * <p>
 * The PROCESS button will only become active if certain conditions are met for file input on the file
 * selectors - dictated by the programmer.  By default these include selecting an image or DIR on
 * each FileSelector.  The Programmer can choose to set other dependencies which must be met, for example,
 * ensuring two FileSelectors have matched input, or constraining them to receive a certain type of input
 * (perhaps only files, or only DIRs etc).  See FileSelector inner class for more information on tuning
 * fileSelectors for specific inputs.
 * <p>
 * Other objects can be added to the DialogWindow JFrame - typically these will include other GUI components
 * to help retrieve user input, and can include:
 * <p>
 * JTextField - to receive text input or display text.
 * <p>
 * JList - to choose an option from a set list.
 * <p>
 * JButton - to allow the user to alter further options in other JFrames or Dialogs.
 * <p>
 * JCheckBox & JRadioButton - to select boolean values [these can be placed on a ButtonGroup object].
 * <p>
 * JComboBox - to choose an option from a dropdown list.
 * <p>
 * JTextArea & JTextPane - to display longer written information, or receive more written information.
 * <p>
 * JLabel - to provide information to the user.
 * <p>
 * JList - to choose an option from a set list.
 * <p>
 * Menu - JMenuBar, JMenuItem -> Provide functionality for adding menus to a JFrame.
 * <p>
 * JPanel - organise components into different panels on the JFrame.
 * <p>
 * JSeparator - to provide separating lines across the JFrame.
 * <p>
 * JSlider - User numerical input.
 * <p>
 * JSpinner - User numerical input.
 * <p>
 * JSplitPane, JTabbedPane - to divide the JFrame into separate sections.
 * <p>
 * JTable - Table of data [editable or uneditable].
 * <p>
 * JToolBar - a bar that provides tools in the form of buttons [typically with icons].
 * <p>
 * JTree - for providing heirarchical information [such as folder trees].
 * <p>
 * JTextField - to receive text input or display text.
 * <p>
 * Finally, the method layoutAndDisplayFrame() lays out all GUI objects onto the JFrame, and displays
 * the DialogWindow in a JFrame in ImageJ.
 * <p>
 * <b>USING DIALOG WINDOW IN STEREOMATE ALGORITHM INTERFACE FOR FILE INPUT AND OUTPUT</b>
 * <p>
 * The DialogWindow requires a class implementing the StereoMateAlgorithm interface in its constructor -
 * when the PROCESS button is pressed, image processing is set up in the DialogWindow class, which then
 * calls the process() method in the SMA implementing class.  
 * <p>
 * The class implementing SMA also has access to the DialogWindow object, and this should be used to retrieve
 * the appropriate input for performing the computation.
 * <p>
 * By retrieving the image files through hte DialogWindow, controls such as whether the file is a valid file
 * to be opened in ImageJ can be checked before it is passed through the algorithm.
 * 
 * 
 * 
 * 
 * 
 * However, this method is blank, and in order to implement further behaviour in a class where this
 * DialogWindow is used, this method must be overwritten by declaring the DialogWindow as an 
 * anonymous inner class and overwriting this method.
 * <p>
 * For Example:
 * <p>
 * dw = new DialogWindow2("TITLE_OF_DIALOG_HERE") {
 *		<p>	
 *			public void processImages() {
 *		<p>	
 *				ImagePlus[] imps = dw.getImageArray();
 *		<p>	
 *				dw.dispose(); 
 *		<p>
 *				process(imps);
 *		<p>		
 *			}
 *		<p>			
 *		};
 * <p>
 * Where "process(imps)" is a method declared in the new class to provide the functionality to process
 * the images.
 * <p>
 * 
 * 
 * 
 * To add further components to the DialogWindow, the standard Java classes should be
 * used, which provide a wealth of input.  Any components should be:
 * <p>
 * 1. Declared as an instance variable in the class which uses the DialogWindow class.
 * <p>
 * 2. Initiated and added to the DialogWindow where the DialogWindow is initiated.  It is best to add
 * components in groups on Panels (using BoxLayout for a Panel is best for good layout).
 * <p>
 *  3. Its state retrieved in future methods via its instance variable - since this is accessible
 *  across different methods in the class.
 *  <p>
 *  Below is a list of different GUI Components which provide users with information and allow user
 *  input:
 * <p>
 * JTextField - to receive text input or display text.
 * <p>
 * JList - to choose an option from a set list.
 * <p>
 * JButton - to allow the user to alter further options in other JFrames or Dialogs.
 * <p>
 * JCheckBox & JRadioButton - to select boolean values [these can be placed on a ButtonGroup object].
 * <p>
 * JComboBox - to choose an option from a dropdown list.
 * <p>
 * JTextArea & JTextPane - to display longer written information, or receive more written information.
 * <p>
 * JLabel - to provide information to the user.
 * <p>
 * JList - to choose an option from a set list.
 * <p>
 * Menu - JMenuBar, JMenuItem -> Provide functionality for adding menus to a JFrame.
 * <p>
 * JPanel - organise components into different panels on the JFrame.
 * <p>
 * JSeparator - to provide separating lines across the JFrame.
 * <p>
 * JSlider - User numerical input.
 * <p>
 * JSpinner - User numerical input.
 * <p>
 * JSplitPane, JTabbedPane - to divide the JFrame into separate sections.
 * <p>
 * JTable - Table of data [editable or uneditable].
 * <p>
 * JToolBar - a bar that provides tools in the form of buttons [typically with icons].
 * <p>
 * JTree - for providing heirarchical information [such as folder trees].
 * <p>
 * JTextField - to receive text input or display text.
 * <p>
 * 
 * 
 * 
 * 
 * The FileSelector class inside this class implements the StereoMateInputOutputFramework:
 * <p>
 * The DialogWindow class selects only valid inputs based on the StereoMate Input-Output Framework,
 * Which means only the following can be received as input:
 * <p>
 * - An image file which ImageJ can process.
 * <p>
 * - A directory containing images which ImageJ can process.
 * <p>
 * - A directory of a specified tree structure.
 * <p>
 * This is determined in the StereoMateInputOutputFramework class, which determines whether the input
 * file or directory is valid or not, and provides appropriate input concerning previous data analysis
 * performed on a selected directory, if appropriate (see StereoMateInputOutputFramework.java for 
 * further info).
 * <p>
 * 
 * The status of the selected file or directory, in terms of its validity, structure, and past 
 * processing, is displayed on a Text Area present to the right of the FileSelector(s).  Furthermore,
 * the user can only pass beyond the DialogWindow if a valid input is chosen for all FileSelectors.
 * <p>
 * 
 * Where two or more FileSelectors are used, a TextArea is present for each FileSelector.
 * <p>
 * 
 * Finally, the return type of getImageArray is of type ImagePlus[][], where the first array dimension 
 * is a reference for a given FileSelector, and the second dimension is for the potential array of 
 * images when a directory is selected by a given FileSelector.
 * <p>
 * 
 * With a Compound Directory, this may not be so simple -> and it is probably best to return a structure
 * which can encode the type of file selected (image, simple DIR or compound DIR), as well as the paths
 * to each image in the structure selected. 
 * 
 * @author stevenwest
 *
 */

 public class DialogWindow extends JFrame {

	 /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	  * This object represents the StereoMateAlgorithm being processed by DialogWindow.  This object can be used
	  * to call the process() and writeStereoMateInfo() methods which are defined in the StereoMateAlgorithm 
	  * interface, required for processing of an array of images, and for updating the StereoMateInfo Document.
	  */
	 StereoMateAlgorithm sma;
	 
	/**
	 * An ArrayList to hold FileSelector instances [declared as an inner class in the DialogWindow
	 * class]. Each instance can be accessed from this ArrayList, using an appropriate index, or the
	 * method getFileSelectorPath(int index) can be used to get the file of a given fileSelector
	 * directly.
	 * <p>
	 * The method getImage(int index) will also retrieve the selected image on a given FileSelector.
	 */
	public ArrayList<FileSelector> fileSelectors; //arraylist to hold multiple file selector objects

	/**
	 * An int to store the number of FileSelector objects added to this DialogWindow.
	 */
	int selectorCount;
	
	
	/**
	 * An int to store the total number of files to be processed from the FIRST fileSelector.
	 */
	public int totalFileCount;
	
	/**
	 * Boolean to represent whether setMethoCallNumber has been called, and therefore the programmer has
	 * set the totalFileCount to a specified number - typically set to 1 to ensure process() method is only
	 * called once.
	 */
	boolean overrideTotalFileCount;
	
	/**
	 * The value the programmer has set totalFileCount to be.
	 */
	int manualTotalFileCount;
	
	/**
	 * If the totalFileCount is over-written with the manualTotalFileCount, then the original totalFileCount (i.e.
	 * actually how many files there are in the selected DIR) is still retrievable from this int.
	 */
	int totalFileCountOriginal;
	
	/**
	 * An int to store the index of the current image retrieved from the fileSelector fileArray.
	 */
	public int currentFileIndex;
	
	
	/**
	 * The panel on which the "cancel" and "process" buttons are located.
	 */
	JPanel actionPanel;
	
	/**The JButton 'process' for the actionPanel. */
	JButton process;
	/**The JButton 'cancel' for the actionPanel. */
	JButton cancel;
	
	
	/**
	 * A String to represent the Suffix used automatically within any PlugIn with the output
	 * File or DIR name.  Initialised to an empty String (i.e. not null).
	 */
	String SM_SUFFIX = "";
	
	
	/**
	 * A String Array which contains all titles for the active images.
	 */
	String[] titles;
	
	/**
	 * ArrayList< ArrayList<File> > object to hold all File refs from all FileSelectors.
	 * First ArrayList holds refs to ArrayLists of inputs, and second ArrayList holds refs
	 * to input files on a given FileSelector.
	 */
	ArrayList<ArrayList<File>> inputs;
	
	/**
	 * ArrayList<File> object to hold all File refs for output destinations for images. Derived
	 * from the first FileSelector, which handles output DIR title and image titles.
	 */
	ArrayList<File> output;
	
	/**
	 * This String holds the appropriate output image extension for saving images.
	 */
	String outputExtension;

	
	/**
	 * An ArrayList<File> object to hold all input File objects which were unsuccessfully opened
	 * by the getNextImp() and getCurrentImp() methods.
	 */
	ArrayList<File> nonImageFileLog;
	
	
	/**
	 * Title of the Algorithm -> provided in the Constructor
	 */
	String algorithmTitle;
	
	//NAMED CONSTANTS for FileChooser in the FileSelector.  Used to set the FileSelector
	//appropriately during its constructor:
	final int FILES_AND_DIRS = JFileChooser.FILES_AND_DIRECTORIES;
	final int FILES_ONLY = JFileChooser.FILES_ONLY;
	final int DIRS_ONLY = JFileChooser.DIRECTORIES_ONLY;
	
	//NAMED CONSTANTS for collection of FileSelectors. These determine whether subsequent file selectors 
	//must provide input files which are MATCHED, or not.
		//If subsequent File Selectors require MATCHED INPUT, then their DIR tree structure and file array
		//must match the first File Selectors.  
			//This should ensure to a reasonable level of accuracy that the subsequent file selector has the
			//same Dir Tree form, and the same number of files, as the first - and so files can be processed in
			//turn in a parallel nature.
	public final int MATCHED_INPUT = 1;
	public final int ANY_INPUT = 0;
	
	boolean checkExternalInput;
	boolean externalInput;
	
	/**
	 * Instance variable to inform the Action Panel whether all input is matched across all fileSelectors, if
	 * it is required.  This boolean is true when:
	 * <p>
	 * 1. All FileSelectors have some form of input selected.
	 * <p>
	 * and
	 * <p>
	 * 2. All file inputs to fileSelectors which are required to have MATCHED_INPUT have an input identical to 
	 * the first FileSelector.
	 * <p> 
	 * When these two conditions are met, the Action Panel's Action Button ("Process" by default) will become active
	 * and the Algorithm can begin.
	 */
	boolean allInputMatched;
	
	/*
	 * StereoMate Input-Output Framework instance for storing information relating to selected
	 * file in relation to StereoMate file Input-Output.  NOT NEEDED AS EACH FileSelector NEEDS
	 * A SMInputOutputFramework!
	 */
	//StereoMateInputOutputFramework inputOutputFramework; //Contains information relating to
	//selected file in relation to StereoMate file Input-Output.
	
	
	
	/**
	 * <b>Constructor:</b>
	 * <p>
	 * Default constructor, calls the superclass [JFrame] constructor, sets layout (BoxLayout), 
	 * and initiates instance variables.
	 * @param title The title of the DialogWindow & algorithm (used in status when processing complete!).
	 * @param sma This object represents the StereoMateAlgorithm object which is using this DialogWindow.
	 * This reference is used in the DialogWindow class to call the process() and writeStereoMateInfo() methods
	 * required for StereoMate Algorithm processing of images.
	 */
	public DialogWindow(String title, StereoMateAlgorithm sma) {
		//call the JFrame constructor with title as argument:
		super(title);
		
		//Set the algorithmTitle to title:
		algorithmTitle = title;
		
		//Store the sma to the instance variable:
		this.sma = sma;
		
		//set layout to BoxLayout:
		this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.PAGE_AXIS));
		
		//Initialuse the fileSelectors ArrayList, and the counter:
		fileSelectors = new ArrayList<FileSelector>();		
		selectorCount = 0;
		
		//Initialise the nonImageFileLog ArrayList:
		nonImageFileLog = new ArrayList<File>();
		
		overrideTotalFileCount = false; //set this to false, only set to true if setMethodCallNumber() is 
											//called by programmer using this class..
		
	}
	
	
	/**
	 * This method sets the plugin suffix, which will suffix all output folders with this
	 * String. This is used to set the output file title, in setOutputFileTitle();
	 * @param SUFFIX The String to set the Output Suffix, typically an underscore followed
	 * by three CAPITALISED letters [i.e. "_DEC"].
	 */
	public void setPlugInSuffix(String SUFFIX) {
		SM_SUFFIX = SUFFIX;
	}
	
	
	/**
	 * This method causes the process loop to be over-ridden to stop all files being called in the process loop.
	 * Instead, the process loop is only called 'methodCallNumber' of times.  The programmer then can prevent
	 * all images being called in a selected directory, and have control over the opening and closing of any
	 * number of images.
	 * <p>
	 * If this method is called during the set up of the DialogWindow, the following two methods can be used
	 * to go to the Next and Previous Images in the input:
	 * <p>
	 * Next Image: incrementTotalFileCountAndCurrentFileIndex();
	 * <p>
	 * Previous Image: decrementTotalFileCountAndCurrentFileIndex();
	 * <p>
	 * To retrieve the Next/Previous image, after calling these method, simply call the followin to return the
	 * ImagePlus:
	 * <p>
	 * getCurrentImp(0);
	 * 
	 * @param methodCallNumber
	 */
	public void setMethodCallNumber(int methodCallNumber) {
		
		overrideTotalFileCount = true;
		
		manualTotalFileCount = methodCallNumber;
		
	}
	
	/**
	 * Add default file selector with "Input:" as its title.  See addFileSelector(title) below
	 * for full description of this method.
	 */
	public void addFileSelector() {
		addFileSelector("Input:");
	}
	
	/**
	 * Add a FileSelector object to the DialogWindow. 
	 * <p>
	 * This method creates a FileSelector object, adds it to the fileSelectors array,
	 * and adds the fileSelectors JPanel object to the DialogWindow.
	 * <p>
	 * A FileSelector object contains a JPanel to hold the FileSelectors objects,
	 * including a JComboBox, JButton and JTextField.
	 * <p> 
	 * An uneditable JComboBox holds the paths of all open images 
	 * as well as the path to the image or folder selected, 
	 * <p>
	 * A JButton will open a JFileSelector for choosing a File or DIR.
	 * <p>
	 * A JTextField stores the path to the selected File.
	 * <p>
	 * The FileSelector will include a FileChooser which can select both
	 * Files and Directories.  Note, this behaviour can be modified by calling the
	 * other addFileSelector() method, which takes an additional NAMED CONSTANT as
	 * a parameter.
	 * <p>
	 * FIRST FILESELECTOR:
	 * <p>
	 * The First fileSelector added also possesses two important functions: Output
	 * & Constraining the Input.  
	 * <p>
	 * OUTPUT:
	 * <p>
	 * The First FileSelector dictates the output of the image processing.  If a DIR is 
	 * selected for input, an Output DIR name will be automatically generated, which 
	 * can be modified by the user.  This DIR is created in the SAME PARENT DIRECTORY as
	 * the selected input DIR.
	 * <p>
	 * CONSTRAINING THE INPUT:
	 * <p>
	 * The First FileSelector also allows "constrained input".  This means that the image
	 * processing can be constrained to a sub-folder of the selected input folder.  
	 * This is useful to ensure output is saved to a DIR separate to the 
	 * RAW DATA folder in an experiment, and is present to allow the implementation of
	 * Modular Data Management System, which essentially ensures that Sets of Raw Data
	 * collected in an experiment, and sebsequent analyses of these datasets, are kept
	 * separate in order to easily identify different datasets and analyses.
	 * <p>
	 * Subsequent File Selector objects only include the basic functionality of selecting input,
	 * which with this method can be both FILES or DIRS. 
	 * <p>
	 * Note, the file path (accessed in the getFilePath() method) can be accessed by providing 
	 * the index of the fileSelector, which is linked to the order the FileSelectors are added.
	 * <p>
	 * @param title A string which becomes the title of the fileSelector on the DialogWindow.
	 */
	public FileSelector addFileSelector(String title) {

		//retrieve the String array of all currently open images to add to
		//the JComboBox in the FileSelector (via its constructor):
		titles = WindowManager.getImageTitles();
		
		/* FILE SELECTOR */
		//create a FileSelector object:
		FileSelector fileSelector = new FileSelector(title, titles, selectorCount, FILES_AND_DIRS, ANY_INPUT);
		
		//Add the fileSelector object to the fileSelectors ArrayList:
		fileSelectors.add( fileSelector );
		//and increment the selectorCount by 1:
		selectorCount = selectorCount + 1;
		
		
		//add the fileSelector JPanel to the DialowWindow:
		this.add( fileSelector.getPanel() );
		
		return fileSelector;
		
	}//end addFileSelector()
	
	
	/**
	 * Add a FileSelector object to the DialogWindow. This method creates
	 * a panel to hold the FileSelector objects, which includes an uneditable
	 * JComboBox for holding the paths of all open images as well as the
	 * path to the image or folder selected, and a JButton which will open 
	 * a JFileChooser. The File or Folder selected is returned to the Panel, 
	 * and stored in the JTextField.
	 * <p>
	 * The FileSelector will include a FileChooser which can select both
	 * Files and Directories.  Note, this behaviour can be modified by this method, 
	 * which takes an additional NAMED CONSTANT as a parameter.
	 * <p>
	 * The First fileSelector added also possesses two important functions: Output
	 * & constraining the input.  
	 * <p>
	 * The First FileSelector dicates the output of the image processing.  If a DIR is 
	 * selected for input, an Output DIR name will be automatically generated, which 
	 * can be modified by the user.  This DIR is created in the SAME PARENT DIRECTORY as
	 * the selected input DIR.
	 * <p>
	 * The First FileSelector also allows "constrained input".  This means that the image
	 * processing can be constrained to a subfolder of the selected input folder on this 
	 * file selector.  This is useful to ensure output is saved to a DIR separate to the 
	 * RAW DATA folder in an experiment, and is present to allow the implementation of
	 * Modular Data Management System, which essentially dictates that Sets of Raw Data
	 * collected in an experiment, and sebsequent analyses of these datasets, should be kept
	 * separate in order to easily identify different datasets and analyses.
	 * <p>
	 * Subsequent File Selector objects only include the basic functionality of selecting input,
	 * which with this method can be set to FILES, DIRS or both. Note, the file path (accessed in the 
	 * getFilePath() method) can be accessed by providing the index of the fileSelector, which 
	 * is linked to the order the FileSelector is added.
	 * @param title A string which becomes the title of the fileSelector on the
	 * DialogWindow.
	 * @param matchedInputType This only applies to FileSelectors added AFTER the FIRST FileSelector.  Determines
	 * whether the input structure must match the First FileSelector (MATCHED_INPUT) or not (ANY_INPUT).
	 */
	public void addFileSelector(String title, int matchedInputType) {
		
		//retrieve the String array of title of all currently open images to add to
		//the JComboBox in the FileSelector:
		titles = WindowManager.getImageTitles();
				
		/* FILE SELECTOR */
		//create a FileSelector object:
		FileSelector fileSelector = new FileSelector(title, titles, selectorCount, FILES_AND_DIRS, matchedInputType);
		
		//Add the fileSelector object to the fileSelectors ArrayList:
		fileSelectors.add( fileSelector );
		//increment the selectorCount by 1:
		selectorCount = selectorCount + 1;
		
		
		//add the fileSelector JPanel to the DialowWindow:
		this.add( fileSelector.getPanel() );
					
	} //end addFileSelector
	
	/**
	 * Add a FileSelector object to the DialogWindow. This method creates
	 * a panel to hold the FileSelector objects, which includes an uneditable
	 * JComboBox for holding the paths of all open images as well as the
	 * path to the image or folder selected, and a JButton which will open 
	 * a JFileChooser. The File or Folder selected is returned to the Panel, 
	 * and stored in the JTextField.
	 * <p>
	 * The FileSelector will include a FileChooser which can select both
	 * Files and Directories.  Note, this behaviour can be modified by this method, 
	 * which takes an additional NAMED CONSTANT as a parameter.
	 * <p>
	 * This method also takes another input - matchedInputType.  This needs to be set
	 * to one of the NAMED CONSTANTS in this class:
	 * <p>
	 * MATCHED_INPUT -> This indicates that this fileSelector must have Matched input to the
	 * first fileSelector -> i.e it should contain the same DirTree DIR number, same minDepth
	 * and maxDepth, and same number of files in the fileArray.
	 * <p>
	 * ANY_INPUT -> This indicates that this fileSelector can have any input.
	 * <p>
	 * Note, setting a fileSelector to MATCHED_INPUT makes it impossible to select a DIR or file
	 * which does not match its input with the first fileSelector.
	 * <p>
	 * The First fileSelector added also possesses two important functions: Output
	 * & constraining the input.  
	 * <p>
	 * The First FileSelector dicates the output of the image processing.  If a DIR is 
	 * selected for input, an Output DIR name will be automatically generated, which 
	 * can be modified by the user.  This DIR is created in the SAME PARENT DIRECTORY as
	 * the selected input DIR.
	 * <p>
	 * The First FileSelector also allows "constrained input".  This means that the image
	 * processing can be constrained to a subfolder of the selected input folder on this 
	 * file selector.  This is useful to ensure output is saved to a DIR separate to the 
	 * RAW DATA folder in an experiment, and is present to allow the implementation of
	 * Modular Data Management System, which essentially dictates that Sets of Raw Data
	 * collected in an experiment, and sebsequent analyses of these datasets, should be kept
	 * separate in order to easily identify different datasets and analyses.
	 * <p>
	 * Subsequent File Selector objects only include the basic functionality of selecting input,
	 * which with this method can be set to FILES, DIRS or both. Note, the file path (accessed in the 
	 * getFilePath() method) can be accessed by providing the index of the fileSelector, which 
	 * is linked to the order the FileSelector is added.
	 * @param title A string which becomes the title of the fileSelector on the
	 * DialogWindow.
	 * @param fileChooserType An int to represent what type of FileChooser should
	 * be available on the FileSelector - accepting FILES_ONLY, DIRS_ONLY, or FILES_AND_DIRS.
	 * @param matchedInputType This only applies to FileSelectors added AFTER the FIRST FileSelector.  Determines
	 * whether the input structure must match the First FileSelector (MATCHED_INPUT) or not (ANY_INPUT).
	 */
	
	public void addFileSelector(String title, int matchedInputType, int fileChooserType) {
		
		//retrieve the String array of title of all currently open images to add to
		//the JComboBox in the FileSelector:
		titles = WindowManager.getImageTitles();
				
		/* FILE SELECTOR */
		//create a FileSelector object:
		FileSelector fileSelector = new FileSelector(title, titles, selectorCount, fileChooserType, matchedInputType);
		
		//Add the fileSelector object to the fileSelectors ArrayList:
		fileSelectors.add( fileSelector );
		//increment the selectorCount by 1:
		selectorCount = selectorCount + 1;
		
		
		//add the fileSelector JPanel to the DialowWindow:
		this.add( fileSelector.getPanel() );
					
	} //end addFileSelector
	
	
	/**
	 * This method returns the number of File Selectors added to this Dialog Window.
	 * This can be used to ensure a valid index is given when attempting to access
	 * the array of FileSelector Objects.
	 * @return The total number of File Selectors added to this dialog.
	 */
	public int getSelectorCount() {
		return selectorCount;
	}
	
	/**
	 * This method aims to verify that the inputs on all FileSelectors are valid for image 
	 * processing.  This depends on two conditions:
	 * <p>
	 * 1. All FileSelectors have some form of input selected.
	 * <p>
	 * and
	 * <p>
	 * 2. All file inputs to fileSelectors which are required to have MATCHED_INPUT have an input identical to 
	 * the first FileSelector.
	 * <p>
	 * If both of these conditions are met, then this method will activate the process JButton, otherwise it
	 * will disable it.
	 */
	public boolean verifyMatchedInput() {
		
		boolean inputSet = true;
		//First, check if all file selectors have some input set:
		for(int a=0; a<fileSelectors.size(); a++) {
			if(fileSelectors.get(a).imageSelected == 0) {
				inputSet = false;
			}
		}
		
		//If inputSet is false, then one or more of the file selectors does not have any input,
			//and so the image processing cannot proceed:
		//Set allInputMatched to false, the process button to disabled, and return.
		if(inputSet == false) {
			allInputMatched = inputSet;
			process.setEnabled(allInputMatched);
			return false;
		}
		
		//At this point, condition 1 is fulfill - all of the file selectors have some input selected on them.
		//Now, the second condition must be verified:
			//For every FileSelector beyond the first which also has had MATCHED_INPUT set to its matchedInput
			//variable, it must have the same input as the first fileSelector.
		
			//This will be determined by checking it has the same minDepth, maxDepth, DirTree length and FileArray
			//length.
		
		for(int a=1; a<fileSelectors.size(); a++) {
			
			if(fileSelectors.get(a).matchedInput == MATCHED_INPUT) {
				//a subsequent fileSelector beyond the first has MATCHED_INPUT set to its matchedInput variable.
				//Therefore, the inputs must be confirmed to match before the process button can be activated:
				
				//First, collect both this fileSelectors and the first fileSelectors variables for: minDepth,
				//maxDepth, dirTree length, and fileArray length:
				
				//First FileSelector:
				int firstMinDepth;
				int firstMaxDepth;
				int firstDirTreeLength, firstFileArrayLength;
				if(fileSelectors.get(0).inputOutputFramework.constrainedAnalysis == false) {
					firstMinDepth = fileSelectors.get(0).inputOutputFramework.minDepth;
					firstMaxDepth = fileSelectors.get(0).inputOutputFramework.maxDepth;
					if(firstMinDepth > 0) {
						firstDirTreeLength = fileSelectors.get(0).inputOutputFramework.dirTreeArray.size();
					}
					else {
						firstDirTreeLength = 0;
					}
					if(fileSelectors.get(0).inputOutputFramework.fileArray != null) {
						firstFileArrayLength = fileSelectors.get(0).inputOutputFramework.fileArray.size();
					}
					else {
						firstFileArrayLength = 0;
					}
				}
				else {
					firstMinDepth = fileSelectors.get(0).inputOutputFramework.constrainedMinDepth;
					firstMaxDepth = fileSelectors.get(0).inputOutputFramework.constrainedMaxDepth;
					if(firstMinDepth > 0) {
						firstDirTreeLength = fileSelectors.get(0).inputOutputFramework.dirTreeArrayConstrained.size();
					}
					else {
						firstDirTreeLength = 0;
					}
					if(fileSelectors.get(0).inputOutputFramework.fileArray != null) {
						firstFileArrayLength = fileSelectors.get(0).inputOutputFramework.fileArray.size();
					}
					else {
						firstFileArrayLength = 0;
					}
				}
				
				//Current FileSelector:
				int currentMinDepth = fileSelectors.get(a).inputOutputFramework.minDepth;
				int currentMaxDepth = fileSelectors.get(a).inputOutputFramework.maxDepth;
				//IJ.showMessage("currMinDep.: "+currentMinDepth);
				int currentDirTreeLength, currentFileArrayLength;
				if(currentMinDepth > 0) {
					currentDirTreeLength = fileSelectors.get(a).inputOutputFramework.dirTreeArray.size();
				}
				else {
					currentDirTreeLength = 0;
				}
				if(fileSelectors.get(a).inputOutputFramework.fileArray != null) {
					currentFileArrayLength = fileSelectors.get(a).inputOutputFramework.fileArray.size();
				}
				else {
					currentFileArrayLength = 0;
				}
				
				//CHECK INPUT:
				
			//	IJ.showMessage("First: " +firstMinDepth + " " + firstMaxDepth + " " + firstDirTreeLength + " " 
			//			+ firstFileArrayLength + "\n" + "Current: " + 
			//			currentMinDepth + " " + currentMaxDepth + " " + currentDirTreeLength + " "
			//			+ currentFileArrayLength);
				
				if(firstMinDepth == currentMinDepth && firstMaxDepth == currentMaxDepth &&
						firstDirTreeLength == currentDirTreeLength && firstFileArrayLength == currentFileArrayLength ) {
					//if all the current ints match the first FileSelector's ints, then the inputs are matched:
					allInputMatched = true;
					process.setEnabled(allInputMatched);
					return true;
				}
				else if(firstMinDepth == currentMinDepth && firstMaxDepth == currentMaxDepth &&
						firstDirTreeLength == currentDirTreeLength && firstMinDepth == 0 
						&& firstMaxDepth == 0 &&
						firstDirTreeLength == 0) {
					//if all minDepth maxDepth and dirTreeLength on first file selector are 0, and these match
					//the current file selector, then one may have a IJ image selected, while another has a File
					//Image selected - in which case the FileArrayLength will not match, although the input (i.e.
					//a single image) does match -> in this case, the inputs are matched:
					allInputMatched = true;
					process.setEnabled(allInputMatched);
					return true;
				}
				else {
					allInputMatched = false;
					process.setEnabled(allInputMatched);
					return false;
				}
				
				
			} //end if on fileSelectors
			
		} //end for a
		
		//Finally, this code will be reached if none of the subsequent fileSelectors had MATCHED_INPUT set,
		//or if there is only one fileSelector.
		
		//One final check is to allow programmers to implement a block to the Process button for their own
		//processing:
		
		allInputMatched = checkExternalInput();
		
		//Therefore, set allInputMatched to true, enable the process button, and the method is finished:
		//allInputMatched = true;
		process.setEnabled(allInputMatched);
		
		return allInputMatched;
		
	}//end verifyMatchedInput()
	
	
	protected boolean checkExternalInput() {
		if(checkExternalInput == true) {
			return externalInput;
		}
		else {
			return true;
		}
	}
	
	/**
	 * Set the external input variable.  If this is true, the "Process" button will be active,
	 * if false the "Process" button will be inactive.  Use this method to control the status
	 * of the "Process" button.
	 * @param input
	 */
	public void setExternalInput(boolean input) {
		checkExternalInput = true;
		externalInput = input;
		verifyMatchedInput();
	}
	
	
	/**
	 * FILESELECTOR INNER CLASS:
	 * <p>
	 * This inner class represents a FileSelector object.  It holds all the variables required
	 * to build a FileSelector instance, and contains methods to construct and interact with
	 * an instance (see methods for details).
	 * <p>
	 * This includes a panel for displaying the FileSelector, a JComboBox for selecting either
	 * currently active images or previously selected images from the system, a button for opening
	 * a JFileChooser to select images from the system, and a title string displayed above these
	 * components.
	 * <p>
	 * The First FileSelector added to a DialogWindow also possesses a JCheckBox & JComboBox which
	 * allows the user to constrain processing to a sub folder of the input folder, as well as a
	 * JTextField for the user to dictate the TITLE of the output File.
	 * 
	 * @author stevenwest
	 *
	 */
	public class FileSelector {
		
		//NAMED CONSTANTS: To represent the type of item selected in the JComboBox:
		public static final int NO_IMAGE = 0; //No Item Selected - default value when initialised with no open images.
		public static  final int IJ_IMAGE = 1; //An open image in ImageJ is selected.
		public static final int FILE_IMAGE = 2; //An image in the File System is selected.
		
		int imageSelected; //int to represent the type of image selected -> IJ image, or file "image".
		
		
		//NAMED CONSTANTS:  To represent the File Selection [images, rois, data]:
		//final int IMAGES = 0;
		//final int ROIS = 1;
		//final int DATA = 2;
		
		//int fileSelectionType; //int to represent the file selection type.
		// NOT REQUIRED -> Can just use the StereoMate.info file to determine what inputs should
			// be used.
		
		//NAMED CONSTANTS: To represent the type of FileChooser to implement in this FileSelector:
		final int FILES_AND_DIRS = JFileChooser.FILES_AND_DIRECTORIES;
		final int FILES_ONLY = JFileChooser.FILES_ONLY;
		final int DIRS_ONLY = JFileChooser.DIRECTORIES_ONLY;
		
		int fileChooserType; //int to represent the type of FileChooser to implement in this FileSelector.
		
		//NAMED CONSTANTS: To determine whether subsequent file selectors must provide
		//input files which are MATCHED, or not.
		public static final int MATCHED_INPUT = 1;
		public static final int ANY_INPUT = 0;
		
		public int matchedInput; //int to represent whether the fileSelector needs to match the input of the first fileSelector
							//ONLY APPLICABLE to fileSelectors AFTER the first one!
		
		/* Instance Variables - Image & FileSelector Counters */
		
		int numberActiveImages; //an int to store the number of active images, to keep track of how many items
								//in the arraylist filePaths are image titles, and where system paths begin.
		
		int fileSelectorNumber; //an int to represent the fileSelector number of this FileSelector.
		
		
		/* Instance Variables - Panels and Components */
		
		JPanel panel; //JPanel to hold all components on.
		
		JPanel titleAndSelectorPanel; //Panel to hold Title Panel and Selector And Combo Panel.
		
		JPanel titlePanel; //Panel to hold the JLabel titleLabel - for appropriate layout.
		
		JPanel selectorAndComboPanel; //Panel to hold FileSelector and ComboBox.
		
		//ONLY USED FOR FIRST FileSelector:
		JPanel constrainInputPanel;  //Panel to hold a CheckBox and ComboBox for constraining file input.
		
		JPanel outputPanel; //Panel to hold a JTextField for indicating Title of Folder for Output.

		
		
		//Title Panel variables:
		String title; //title of the FileSelector object.
		JLabel titleLabel; //TextField to hold the title of this FileSelector object.
		
		
		//Selector And Combo Panel variables:
		JComboBox<String> pathComboSelector; //JComboBox to select images open in IJ or the fileChooser path.
		JButton fileChooserButton; //Button to activate the JFileChooser.
		public JFileChooser fileChooser; //FileChooser to select an image or DIR for processing.
		
		public String filePath; //String to hold the file path selected from JFileChooser, 
		//or the image title of a selected & active ImageJ image.
		
		String filePathDisplayed; //String for displayable filePath, which may be shorter than
				//actual filePath for display in the pathComboSelector object.
		
		ArrayList<String> filePaths; //an arraylist to hold all image paths and active image titles.
		
		
		JTextArea fileSelectionStatus; //Text Field to indicate the file selected.
			//This is altered in accordance with the StereoMateInputOutputFramework.
		
		private final static String newline = "\n";
		
		//Constrain Input Panel components:
		JCheckBox constrainCheckBox;
		JComboBox<String> constrainComboBox;
		
		//Output Panel JTextField & JLabel:
		JTextField outputTitle;
		JLabel outputLabel;
		
		
		/* Instance Variables - InputOutputFramework */
		
		public InputOutputFramework inputOutputFramework; //Contains information relating to
		//selected file in relation to StereoMate file Input-Output.
		
		
		/**
		 * CONSTRUCTOR
		 * <p>
		 * Constructor of a FileSelector object.  Initialises all of the variables and assembles the
		 * FileSelector.  This also codes for the behaviour of the JComboBox and JButton. All components
		 * are positioned onto the panel in the FileSelector for placement on the DialogWindow.		
		 * @param title String which sets the title of the FileSelector.
		 * @param titles String which should contain the titles of all the active windows in ImageJ [retrieved
		 *  by the addFileSelector() method].
		 * @param selectorCount The number of FileSelector objects currently added to the DialogWindow [ensures
		 * appropriate selection of initial images on all FileSelector objects - retrieved by the 
		 * addFileSelector() method].
		 * @param fileChooserType This int sets the type of FileChooser located in FileSelector.  It can
		 * accept the named constants 'FILES_AND_DIRS', 'FILES_ONLY', and 'DIRS_ONLY'
		 */
		public FileSelector(String title, String[] titles, int selectorCount, int fileChooserType, int matchedInput) {
			
			numberActiveImages = titles.length; //record the number of active images.
			
			final String[] titlesFinal = titles;
			
			final int selectorCountFinal = selectorCount;
			
			imageSelected = NO_IMAGE; //set imageSelected to NO_IMAGE -> no image is currently selected by the JCombobBox
			
			fileSelectorNumber = selectorCount; //set the fileSelectorNumber to the appropriate selector count number.
			
			this.matchedInput = matchedInput; //set the matched input flag.
			
			//initialise all instance variables:
			panel = new JPanel(); //overall panel for the FileSelector
			titleAndSelectorPanel = new JPanel(); //panel for title panel and selector panel
			titlePanel = new JPanel(); //panel for the title.
			selectorAndComboPanel = new JPanel(); //panel for pathComboSelector and fileChooserButton.
			
			//Panels for Constrained analysis and Output:
				//These are only applied to the First FileSelector, therefore only initialise
				//if selectorCount is 0:
			if(fileSelectorNumber == 0) {
				constrainInputPanel = new JPanel(); //panel for the constrainCheckBox and constrainComboBox.
				outputPanel = new JPanel(); //panel for output JTextField for specifying output DIR title.
			}
			
			//Title Variables:
			this.title = title; //set title to parameter 'title'
			titleLabel = new JLabel(title); //create a JLabel of the title.
			
			//File Chooser & Button Variables:
			fileChooserButton = new JButton("..."); //create button with "..." on its surface
			fileChooser = new JFileChooser(); //initialise the JFileChooser.
			
			//File Selection Status - JTextArea Variables:
			fileSelectionStatus = new JTextArea("Text Area", 7, 10);
			fileSelectionStatus.setLineWrap(true);
			fileSelectionStatus.setBorder( BorderFactory.createLineBorder(Color.BLACK) );
			
			//FilePaths variable - for holding all ImageJ images and file paths available to current input:
			filePaths = new ArrayList<String>(); //create ArrayList of filePaths: Either ImageJ 
													//active window titles, or paths to file
													//system images.
			
			//Constrain checkbox and combo box - only available to the FIRST FileSelector:
			//ALSO output variables - only required for FIRST FileSelector:
			if(fileSelectorNumber == 0) {
				constrainCheckBox = new JCheckBox(); //initialise JCheckBox for constrain analysis.
				constrainComboBox = new JComboBox<String>(); //initialise JComboBox for constrain analysis.
							
				outputTitle = new JTextField(20); //JTextField for DIR title for output.
				outputLabel = new JLabel("Output:"); //JLabel for output JTextField.
			}
			
			//StereoMate Input-Output:
			inputOutputFramework = new InputOutputFramework();
			
			
			/** filePaths - ArrayList<String> containing all relevant images and/or files to input  */
			//fill filePaths with currently active image titles:
			for(int a=0; a<numberActiveImages; a++) {
				filePaths.add(titles[a]);
			}
			
			
			//set the filePath variable to the currently active image title -> performed in 
			//pathComboSelector initiation below...
			
			
			/** pathComboSelector - JComboBox to select different input Files **/
			
			//Setup JComboBox:
				//This depends on whether any images are already open in ImageJ:
			if(titles.length > 0) {
				pathComboSelector = new JComboBox<String>(titles); //make JComboBox from titles of open images in imagej
			}
			else {
				pathComboSelector = new JComboBox<String>(); //else make a blank JComboBox.
			}
			
			
			//add behaviour to the JComboBox:
			pathComboSelector.addActionListener( new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					
					//if selected item matches active image in titles array, change imageSelected to
						//IJ_IMAGE, parse the title to setStereoMateInputOutput, set status, and return
					for(int a=0; a<titlesFinal.length; a++) {
						if(titlesFinal[a].equals( (String)pathComboSelector.getSelectedItem() ) ) {
							//set the imageSelected to IJ_IMAGE:
							imageSelected = IJ_IMAGE;
							//parse the titles[a] String to the setStereoMAteInputOutput,
							//to set the inputOutputFramework:
							setStereoMateInputOutput(titlesFinal[a]);
							//Set the Status Bar:
							IJ.showStatus("Image selected ...");						
							return;
						}
					}
					
					//if the string was not detected as an image, it must be a selected file,
					//so change imageSelected to FILE_IMAGE:

					imageSelected = FILE_IMAGE;
					
					//set filePath to the appropriate filePath from filePaths:
					filePath = filePaths.get( pathComboSelector.getSelectedIndex() );
					
					//... and set the StereoMate InputOutput:
					setStereoMateInputOutput(filePaths.get( pathComboSelector.getSelectedIndex() ) );
					
					//Finally, the constrainCheckBox should be un-selected, 
					//and either disabled or re-enabled
					//depending on whether an IJ image is selected (in which case, the pathComboSelector index
					//will be BELOW the number of active images), or a File Image/DIR is selected (in which
					//case, the pathComboSelector index will be  ABOVE or EQUAL TO the number of active images).
					//ONLY do this if this is the FIRST FileSelector:
					if(selectorCountFinal == 0) {
						
							constrainCheckBox.setSelected(false);
							//Do not need to alter constrainComboBox - automatically performed 
							//in itemStateChanged() on constrainCheckBox, which calls removeAllItems()
							
							//disable constrainCheckBox if the image selected is an IJ image...
							if(pathComboSelector.getSelectedIndex() < numberActiveImages) {
								constrainCheckBox.setEnabled(false);
							}
							else {
								//only enable constrainCheckBox if the selected item is a Compound DIR:
								if(inputOutputFramework.validity.getValidity() > 1) {
									constrainCheckBox.setEnabled(true);
								}
								else {
									constrainCheckBox.setEnabled(false);
								}
							}
							
						
					} //end if selectorCount == 0
														
					//Set the Status Bar:
					IJ.showStatus("File selected ...");
					
					//verify the matched inputs across all File Selectors:
					DialogWindow.this.verifyMatchedInput();
				
				} //end actionPerformed()
				
			}); //end addActionListener on pathComboSelector
			
			
			pathComboSelector.setMinimumSize(new Dimension(450,30) );
			pathComboSelector.setPreferredSize( new Dimension(450,30) );
			pathComboSelector.setMaximumSize(new Dimension(450,30));
	

			
			/** fileChooser - JFileChooser to choose input Files **/
			
			//set fileChooser properties:
				//This depends on the type of FileChooser selected for this FileSelector:
			if(fileChooserType == FILES_ONLY) {
				fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY); //select Files only
			}
			else if(fileChooserType == DIRS_ONLY) {
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); //select DIRs only
			}
			else {
				//make a fileChooser which can select both DIRs and Files
				fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			}
			
			
			/** fileChooserButton - JButton which activates the fileChooser for input file selection **/
		
			//set JButton characteristics & ActionListener (to initiate JFileChooser):
			fileChooserButton.setPreferredSize(new Dimension(20,20));
			fileChooserButton.setMaximumSize(new Dimension(20,20));
			fileChooserButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					// TODO Auto-generated method stub
					int returnVal = fileChooser.showOpenDialog(DialogWindow.this);
				
					if(returnVal == JFileChooser.APPROVE_OPTION) {

						// get filePath:
						File file = fileChooser.getSelectedFile();
						filePath = file.getPath();
						
						// Parse this file through the inputOutputFramework:
						inputOutputFramework.parseNewFile(file);
						
						//And set the Text Area:
						setFileSelectionStatus();
						
						//also a call to set the OutputFile title should be made:
						//ONLY if this is the FIRST FileSelector:
						if(selectorCountFinal == 0) {
						setOutputFileTitle();
						}
						
						// if a compound folder is selected, set the constrainCheckBox to enabled:
						//ONLY if this is the FIRST FileSelector:
					if(selectorCountFinal == 0) {
						if( inputOutputFramework.validityValue > 1 ) {
							constrainCheckBox.setEnabled(true);

						}
						else {
							// else a simple Directory or image file is selected, and 
							// constraining cannot be performed,
							// therefore turn the constrainCheckBox off:
							constrainCheckBox.setEnabled(false);

						}
					}
						
						//if filePath is over 50 characters in length, crop to last 47 characters plus "...":
						if(filePath.length() > 50) {
							filePathDisplayed = "..." + filePath.substring(filePath.length() - 47, filePath.length());
						}
						else {
							filePathDisplayed = filePath;
						}
						
						// set path to pathComboSelector:
							//First, check if the filePath is already in filePaths/the CombobBox pathComboSelector:
						boolean inpathComboSelector = false;
						int inPathPlace = 0;
						for(int a=0; a<filePaths.size(); a++) {
							if(filePath.equals( filePaths.get(a) )) {
								inpathComboSelector = true;
								inPathPlace = a;
							}
						}
						
						//if the filePath is not already in the ComboBox pathComboSelector, add it:
						if(inpathComboSelector == false) {
							//add the filePath string to filePaths:
							filePaths.add(filePath);
							//set filePath to the JComboBox, and select it:
							pathComboSelector.addItem( filePathDisplayed );
							pathComboSelector.setSelectedIndex( pathComboSelector.getItemCount()-1 );
						}
						
						//if the filePath is in the comboBox and in filePaths, then do not re-add it
						//but just select the correct index in pathComboSelector:
						else if(inpathComboSelector == true) {
							pathComboSelector.setSelectedIndex(inPathPlace);
						}
						
						//change imageSelected to FILE_IMAGE:
						imageSelected = FILE_IMAGE;
						
					}
					
					//verify the matched inputs across all File Selectors:
					DialogWindow.this.verifyMatchedInput();
					
					//Set the Status Bar:
					IJ.showStatus("File selected ...");
					
				} //end actionPerformed()
			
			}); //end addActionListener on fileChooserButton
			
			
			/**	constrainCheckBox - JCheckBox to activate constrained analysis */
			
			//only required if this is the FIRST FileSelector:
			
		if(fileSelectorNumber == 0) {
			
			//initialise the constrainCheckBox in de-activated state, set text to Constrain,
			//and put Text to the LEFT of the CheckBox:
			constrainCheckBox.setEnabled(false);
			constrainCheckBox.setText("Constrain:");
			constrainCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
			
			//add itemListener to the CheckBox, to implement behaviour when CheckBox is
			//SELECTED or DESELECTED:
			constrainCheckBox.addItemListener( new ItemListener() {

				@Override
				public void itemStateChanged(ItemEvent e) {
					// TODO Auto-generated method stub
					
					if(e.getStateChange() == ItemEvent.SELECTED) {
						
						//if the checkbox is selected, activate the constrainComboBox:
						constrainComboBox.setEnabled(true);
						//IJ.showMessage("CHECKBOX: 2");
						//IJ.showMessage("size of dirTreeStrings: "+inputOutputFramework.dirTreeStrings.size());
						//IJ.showMessage("dirTreeStrings: "+inputOutputFramework.dirTreeStrings);
						//constrainComboBox.addItem( (String[])inputOutputFramework.dirTreeStrings.toArray() );
						//constrainComboBox = new JComboBox<String>((String[])inputOutputFramework.dirTreeStrings.toArray() );
						
						//get dirTreeString as array, which removes the "/" in any Strings in the Array:
						String[] dirTreeStringsArray = getDirTreeStringsArray();
						for(int a=0; a<dirTreeStringsArray.length; a++) {
							constrainComboBox.addItem(dirTreeStringsArray[a]);
						}
						//for(int a=0; a<inputOutputFramework.dirTreeStrings.size(); a++) {
							//constrainComboBox.addItem( inputOutputFramework.dirTreeStrings.get(a) );
							//constrainComboBox.addItem( makeObj( inputOutputFramework.dirTreeStrings.get(a) ) );
						//}
						//IJ.showMessage("ConstrainComboBox count: "+constrainComboBox.getItemCount() );
						
						//set constrainComboBox to first selection, and set the inputFile in
							//inputOutputFramework to the correct File:
						constrainComboBox.setSelectedIndex(0);
						inputOutputFramework.setConstrainedFile( inputOutputFramework.dirTreeArray.get(0) );
						//And set the constrainedAnalysis variable in inputOutputFramework to true:
						inputOutputFramework.setConstrainedAnalysis(true);

						//adjust Text Area:
						setConstrainedFileSelectionStatus();
						
						//also a call to set the OutputFile title should be made:
						setOutputFileTitle();

					}
					
					else if(e.getStateChange() == ItemEvent.DESELECTED) {
						//if checkbox is de-selected, de-activate the constrainComboBox:
						constrainComboBox.setEnabled(false);
						constrainComboBox.removeAllItems();
						//And set the constrainedAnalysis variable in inputOutputFramework to false:
						inputOutputFramework.setConstrainedAnalysis(false);
						// Parse this file through the inputOutputFramework:
						inputOutputFramework.parseNewFile( new File(filePath) );
						//Also, alter the Text Area to show information of [non-constrained] file input:
						setFileSelectionStatus();
						//also a call to set the OutputFile title should be made:
						setOutputFileTitle();
						
					}
					
					//verify the matched inputs across all File Selectors:
					DialogWindow.this.verifyMatchedInput();
					
				} //end itemStateChanged()
				
			}); //end addItemListener.
			
			
			/** constrainComboBox - JComboBox to select a DIR to constrain an analysis to */

			//initially put the constrainComboBox in de-activated state:
			constrainComboBox.setEnabled(false);
			
			//set the minimum and preferred size:
			constrainComboBox.setMinimumSize( new Dimension(375,30) );
			constrainComboBox.setPreferredSize( new Dimension(375,30) );
			
			//add actionListener to the ComboBox, to implement behaviour when ComboBox selection
			//is changed:
			constrainComboBox.addActionListener( new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					// When a new item is selected in the combo box, the constrained file should
					//be set by calling setConstrainedFile() in inputOutputFramework, with the appropriate
					//File object (which are located in dirTreeArray, and has the same index as the Selected
					//index in combo box.
					
					//First, determine the selected index on the constrainComboBox:
					int index = constrainComboBox.getSelectedIndex();
					//And use this index to select the appropriate dirTreeArray File, and run the
					//setConstrainedFile() method in inputOutputFramework with this File as argument:
					if(index>=0) { //only call if index is valid, i.e equal to or over 0.
						
						inputOutputFramework.setConstrainedFile(inputOutputFramework.dirTreeArray.get(index) );
					
						//adjust Text Area:
						setConstrainedFileSelectionStatus();
						
						//also a call to set the OutputFile title should be made:
						setOutputFileTitle();

					}
					
					//verify the matched inputs across all File Selectors:
					DialogWindow.this.verifyMatchedInput();
					
				} //end actionPerformed()
				
			}); //end actionListener
			
			
			/** OUTPUT */
			
			outputTitle.setText("[Output Title]");
			outputTitle.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					// TODO Auto-generated method stub
					//IJ.showMessage("outputTitle edited");
				}
				
			});
		
			
		} //end if fileSelectorNumber == 0
			
		
			
			
			/** INITIAL SELECTION ON pathComboSelector */
			
			//set initial selection in pathComboSelector to the index provided by selectorCount
			//this ensures each fileSelector object has a different image selected, if possible:
				//this applies to any images open in ImageJ prior to opening the DialogWindow:
			int count = pathComboSelector.getItemCount();
		
			if(count > 0) {
				if(count > selectorCount) { 
					//if selector count is in range, select that image.
					pathComboSelector.setSelectedIndex(selectorCount);
					//and set the filePath to the appropriate name in the titles array:
					filePath = titles[selectorCount];
					//Set imageSelected to IJ_IMAGE to represent an image in IJ is selected:
					imageSelected = IJ_IMAGE;
					
					//parse the filePath through StereoMateInputOutput:
					setStereoMateInputOutput(filePath);
						//This also sets the FileSelectionStatus and the OutputFileTitle, where appropriate..
					
				}
				else { //else set pathComboSelector to the maximum index -> count-1
					pathComboSelector.setSelectedIndex(count-1);
					filePath = titles[count-1];
					//Set imageSelected to IJ_IMAGE to represent an image in IJ is selected:
					imageSelected = IJ_IMAGE;
					
					//parse the filePath through StereoMateInputOutput:
					setStereoMateInputOutput(filePath);
						//This also sets the FileSelectionStatus and the OutputFileTitle, where appropriate..
					
				}
			
			} //end if count > 0
			
			else {
				//If there is nothing selected in pathComboSelector initially,
				//still need to set the File Selection Status, so call setFileSelectionStatus():
				setFileSelectionStatus();
			}
			
			/** LAYOUT */
		
			//set layout and add components to panels:
			
			//selectorAndComboPanel - add ComboBox and Button:
			selectorAndComboPanel.setLayout(new BoxLayout(selectorAndComboPanel, BoxLayout.LINE_AXIS) );
			selectorAndComboPanel.setBorder( BorderFactory.createEmptyBorder(2,5,2,5));
			selectorAndComboPanel.add(pathComboSelector);
			selectorAndComboPanel.add(Box.createRigidArea(new Dimension(5,0)));
			selectorAndComboPanel.add(fileChooserButton);
			//pathComboSelector.setAlignmentY(Component.LEFT_ALIGNMENT);

			
			//titlePanel - add titleLabel:
			titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.LINE_AXIS ) );
			titlePanel.add(Box.createRigidArea( new Dimension(5,0) ) );
			titlePanel.add(titleLabel);
			titlePanel.add( Box.createHorizontalGlue() );
			
			
			//constrainInputPanel - set layout & border, add constrainCheckBox & constrainComboBox:
			//only initialise if this is the First File Selector:
			if(fileSelectorNumber == 0) {
			constrainInputPanel.setLayout(new BoxLayout(constrainInputPanel, BoxLayout.LINE_AXIS));
			constrainInputPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			constrainInputPanel.add(constrainCheckBox);
			constrainInputPanel.add(constrainComboBox);
			} //end if fileSelectorNumber == 0
			
			//outputPanel - set layout and border, add outputLabel & outputTitle:
			//only initialise if this is the First File Selector:
			if(fileSelectorNumber == 0) {
			outputPanel.setLayout( new BoxLayout(outputPanel, BoxLayout.LINE_AXIS) );
			outputPanel.add(outputLabel);
			outputPanel.add(outputTitle);
			} //end if fileSelectorNumber == 0
		
			
			//titleAndSelectorPanel - set layout & border, add titlePanel, selectorAndComboPanel,
			//constrainInputPanel & outputPanel:
			titleAndSelectorPanel.setLayout(new BoxLayout(titleAndSelectorPanel, BoxLayout.PAGE_AXIS) );
			titleAndSelectorPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			titleAndSelectorPanel.add(titlePanel);
			titleAndSelectorPanel.add(selectorAndComboPanel);
			//only add these if this is the First FileSelector:
			if(fileSelectorNumber == 0) {
			titleAndSelectorPanel.add(constrainInputPanel);
			titleAndSelectorPanel.add(outputPanel);
			} //end if fileSelectorNumber == 0
			
			
			//panel - set layout & border, add titleAndSelectorPanel & JTextField:
			panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS) );
			panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			panel.add(titleAndSelectorPanel);
			panel.add(fileSelectionStatus);
			
			//Set the Status Bar:
			IJ.showStatus("Dialog Window");
			
		
		} //end constructor
		
		
		/**
		 * Enable or Disable the FileSelector and its components.
		 * @param activate
		 */
		public void setEnabled(boolean activate) {
			
			panel.setEnabled(activate);
			titleAndSelectorPanel.setEnabled(activate);
			titlePanel.setEnabled(activate);
			selectorAndComboPanel.setEnabled(activate);
			
			titleLabel.setEnabled(activate);

			fileChooserButton.setEnabled(activate);
			//public JFileChooser fileChooser; //FileChooser to select an image or DIR for processing.
			
			//public String filePath; //String to hold the file path selected from JFileChooser, 
			//or the image title of a selected & active ImageJ image.
			
			//String filePathDisplayed; //String for displayable filePath, which may be shorter than
					//actual filePath for display in the pathComboSelector object.
			
			//ArrayList<String> filePaths; //an arraylist to hold all image paths and active image titles.
			
			
			//JTextArea fileSelectionStatus; //Text Field to indicate the file selected.
				//This is altered in accordance with the StereoMateInputOutputFramework.
			
			//private final static String newline = "\n";
			
			if(constrainCheckBox!= null) {
				constrainInputPanel.setEnabled(activate);
				outputPanel.setEnabled(activate);
				constrainCheckBox.setEnabled(activate);
				constrainComboBox.setEnabled(activate);
				outputTitle.setEnabled(activate);
				outputLabel.setEnabled(activate);
			}
						
		}
		
		
		public void shutdownFileSelector() {
			
			panel = null; //JPanel to hold all components on.
			
			titleAndSelectorPanel = null; //Panel to hold Title Panel and Selector And Combo Panel.
			
			titlePanel = null; //Panel to hold the JLabel titleLabel - for appropriate layout.
			
			selectorAndComboPanel = null; //Panel to hold FileSelector and ComboBox.
			
			constrainInputPanel = null;  //Panel to hold a CheckBox and ComboBox for constraining file input.
			
			outputPanel = null; //Panel to hold a JTextField for indicating Title of Folder for Output.

			
			
			title = null; //title of the FileSelector object.
			titleLabel = null; //TextField to hold the title of this FileSelector object.
			
			
			pathComboSelector = null; //JComboBox to select images open in IJ or the fileChooser path.
			fileChooserButton = null; //Button to activate the JFileChooser.
			fileChooser = null; //FileChooser to select an image or DIR for processing.
			
			filePath = null; //String to hold the file path selected from JFileChooser, 
			
			filePathDisplayed = null; //String for displayable filePath, which may be shorter than
					//actual filePath for display in the pathComboSelector object.
			
			
			for(int a=0; a<filePaths.size(); a++) {
				filePaths.set(a, null);
			}
			filePaths = null; //an arraylist to hold all image paths and active image titles.
			
			
			fileSelectionStatus = null; //Text Field to indicate the file selected.
				//This is altered in accordance with the StereoMateInputOutputFramework.
			
			constrainCheckBox = null;
			constrainComboBox = null;
			
			outputTitle = null;
			outputLabel = null;
			
			inputOutputFramework.shutdownStereoMateInputOutputFramework();
			inputOutputFramework = null; //Contains information relating to
			//selected file in relation to StereoMate file Input-Output.
			
		}

		
		/**
		 * This method returns a String[] array which is used by the constrainComboBox inside the First
		 * File Selector.  The returned String is a list of all of the sub-DIRs within the Compound DIR (the
		 * DirTree), except each File.separatorChar ("/" or "\", depending on the system) character is 
		 * replaced with a "-".
		 * <p>
		 * This is required for the constrainComboBox to properly display all sub-folders within the DirTree.
		 * @return
		 */
		public String[] getDirTreeStringsArray() {
			
			//First, fill a String[] array with the dirTreeStrings:
			String[] dirTreeStringsArray = new String[inputOutputFramework.dirTreeStrings.size()];
			for(int a=0; a< dirTreeStringsArray.length; a++) {
				dirTreeStringsArray[a] = inputOutputFramework.dirTreeStrings.get(a);
			}
			
			//next, filter the dirTreeStringsArray to remove the "/" [file separator] in file paths
				//replace with the "-" character:
			for(int a=0; a<dirTreeStringsArray.length; a++) {
				dirTreeStringsArray[a] = dirTreeStringsArray[a].replace(File.separatorChar, '-');
				
			}
			
			//finally, return the Strings Array:
			return dirTreeStringsArray;
			
		} //end getDirTreeStringsArray()
		
		
		
		/**
		 * This method will look at an open image in ImageJ and determine whether it has a
		 * representation on the file system.  If so, its path is retrieved, and it is parsed
		 * to the StereoMateInputOutputFramework object (inputOutputFramework).  If the image
		 * only has a representation in ImageJ, a file object is created with the image title,
		 * which is parsed to the StereoMateInputOutputFramework.
		 * @param fileName The String which matches the title of the image.
		 */
		public void setStereoMateInputOutput(String fileName) {
			//attempt to retrieve the ImagePlus object which has the title fileName:
			ImagePlus imp = WindowManager.getImage( fileName );
			//if imp is not null (i.e it exists - then attempt to identify its position on the 
				//File System:
			if(imp != null) {
				//Try to obtain the FileInfo object:
				FileInfo fi = imp.getOriginalFileInfo();
				if(fi != null) { //if file info is not null, the image is from the file system.
					//Collect the fileName & DIR from the FileInfo object:
					String dir = fi.directory;
					String file = fi.fileName;
					//and create a File object from these variables:
					File imageFile = new File(dir+file);
					//IJ.showMessage("Image in file-system. Parsing file to input-output framework");
					//and parse this file to the inputOutputFramework
						//(to set the instance variables in it):
					inputOutputFramework.parseNewFile(imageFile);
				}
				else if(fi == null) {
					//if the FileInfo is null, the image exists in ImageJ but is not present
					//on the File System.
					//Thus, can parse the imp itself, or a file object from the FilePath:
						//[Either should give the same result in setting the input-output framework]
					inputOutputFramework.parseNewImp(imp);
					//or:
					//File imageFile = new File(fileName);
					//inputOutputFramework.parseNewFile(imageFile);
					//IJ.showMessage("This file is not present on file system.");
				}
			}
			else if(imp == null) {
				//if imp is null, the parsed fileName does not represent an open image.
				//Thus, the file must be present on the file system (and was originally selected
				//by the fileSelector object, and now selected AGAIN in the combobox)
				//Therefore, the filePath String should contain the path on the File System,
				//and this can be used to parse to the inputOutputFramework:
				inputOutputFramework.parseNewFile( new File(fileName) ); 
				
			}
			
			//After appropriate parsing and setting of the inputOutputFramework, the FileSelectionStatus
			//should be set, so call setFileSelectionStatus():
			setFileSelectionStatus();
			
			
			//also a call to set the OutputFile title should be made:
			//ONLY if this is the FIRST FileSelector:
			if(fileSelectorNumber == 0) {
				setOutputFileTitle();
			}
			
		} //end setStereoMateInputOutput()
		
		
		/**
		 * This displays appropriate information in the fileSelectionStatus JTextArea.
		 */
		public void setFileSelectionStatus() {
			if(inputOutputFramework.validity != null) {
			fileSelectionStatus.setText( newline + "File Selected: " + newline +
					inputOutputFramework.validity.getValidityString() + newline +
					" min/max: " + inputOutputFramework.minDepth + "/" 
					  + inputOutputFramework.maxDepth  );
			}
			else {
				fileSelectionStatus.setText( newline + "File Selected: " + newline 
								+ "[None]" + newline +
							    " min/max: 0/0" );
			}
		}
		
		
		/**
		 * 
		 */
		public void setConstrainedFileSelectionStatus() {
			fileSelectionStatus.setText("Constrained" + newline + "File Selected: " + newline +
					inputOutputFramework.constrainedValidity.getValidityString() + newline +
					" min/max: " + inputOutputFramework.constrainedValidity.getMinDepth() + "/" +
							 inputOutputFramework.constrainedValidity.getMaxDepth() );
			//IJ.showMessage("Constrained Validity: "+inputOutputFramework.constrainedValidity.validity);
			//IJ.showMessage("Output Name: "+inputOutputFramework.outputName);
			//IJ.showMessage("Output Dir: "+inputOutputFramework.outputDir);
			//IJ.showMessage("Dir Tree: "+inputOutputFramework.dirTreeStrings);
			//IJ.showMessage("Dir Tree: "+inputOutputFramework.dirTreeStringsConstrained);
		}
		
		
		/**
		 * This method will set the outputName in inputOutputFramework, as well as the outputTitle
		 * String in this class.  This is required for setting the SUFFIX to the output File.
		 */
		public void setOutputFileTitle() {
			//First, set inputOutputFramework.outputName to outputName + SM_SUFFIX:
			inputOutputFramework.outputName = inputOutputFramework.outputName + SM_SUFFIX;
			outputTitle.setText(inputOutputFramework.outputName);
			//IJ.showMessage("Output Name: "+inputOutputFramework.outputName);
			//IJ.showMessage("Output Dir: "+inputOutputFramework.outputDir);
			//IJ.showMessage("Dir Tree: "+inputOutputFramework.dirTreeStrings);
			//IJ.showMessage("Dir Tree: "+inputOutputFramework.dirTreeStringsConstrained);
		}
		
		/**
		 * This method returns the File Selectors current parent output directory.  This is the
		 * directory where the input DIR tree is reflected, and where all output data is stored.
		 * @return
		 */
		public File getOutputParentFile() {
			String pathName = inputOutputFramework.outputDir + File.separator + inputOutputFramework.outputName;
			return new File(pathName);
		}
		
		
		/**
		 * Returns the panel object containing all components generated in this class.
		 * @return The JPanel containing all objects generated in this class.
		 */
		public JPanel getPanel() {
			return panel;
		}

		/**
		 * Returns the selected image on this FileSelector object. This method should be used to
		 * access the selected image.
		 * @return imp An ImagePlus of the selected file or image.  If the selected item is a file,
		 * the image is opened using the IJ.openImage() method. If the selected item is an image,
		 * the ImagePlus object is returned, and the original image is hidden to prevent duplicate
		 * images being opened.
		 * 
		 * DEPRECIATED -> Now This Class Supports Selection of Files and DIRS!
		 */
		@Deprecated
		public ImagePlus getSelectedImage() {
			
			ImagePlus imp; //imp object to store and return reference.

			if(imageSelected == NO_IMAGE) {
				IJ.error("No Image was selected.");
				return null;
			}
			else if(imageSelected == IJ_IMAGE) {
				imp = WindowManager.getImage( (String)pathComboSelector.getSelectedItem() );
				imp.hide(); //"hide" the imp -> i.e. close the window displaying this imp
								//This prevents duplicates of the imp being open.
				return imp;
			}
			else {
				imp = IJ.openImage( filePaths.get( pathComboSelector.getSelectedIndex() ) );
				return imp;
			}
		} //end getSelectedImage()
		
		/**
		 * Returns the status of this FileSelectors selection - 0 NO_IMAGE, 1 IJ_IMAGE, 2 FILE_IMAGE.
		 * @return
		 */
		public int getImageSelected() {
			return imageSelected;
		}
		
		/**
		 * Set the imageSelected status in this FileSelector: 0 NO_IMAGE, 1 IJ_IMAGE, 2 FILE_IMAGE.
		 * @param imageSelected
		 * @return
		 */
		public void setImageSelected(int imageSelected) {
			this.imageSelected = imageSelected;
		}
		
	} //end inner class FileSelector
	
	/**
	 * This method calls the addActionPanel(String processTitle) method
	 * with "Process" as the String.
	 */
	public void addActionPanel() {
		addActionPanel("Process");
	} //end addActionPanel()
		
	
	
	/**
	 * This method will add an Action Panel onto the Dialog, which
	 * provides a cancel button and a process button, titled with
	 * a string of the Programmers' choosing.
	 * <p>
	 * Note that the ActionListener on the process button will call the
	 * processImages() method in this class in a NEW THREAD.  This
	 * thread starts the processImage() method in this class, which
	 * begins the computation on the image/DIR, and sequentially
	 * provides images to the process() method of the associated
	 * StereoMateAlgorithm - which should provide the code for
	 * processing the images (see processImages() for further details).
	 * <p>
	 * The process button must run in a separate thread, to ensure the image
	 * processing does not occur in the EDT - which would otherwise block
	 * ImageJ (for example, the ImageJ status bar will not update). 
	 * <p>
	 * @param processTitle The String displayed on the Process button.
	 */
	public void addActionPanel(String processTitle) {
		/* ACTION PANEL */
		//create a JPanel to hold "Process" and "Cancel" Buttons:
		actionPanel = new JPanel();
		
		//create JButtons:
		process = new JButton(processTitle);
		cancel = new JButton("Cancel");
		
		//Set process button initially to disabled (i.e. to allInputMatched value):
		process.setEnabled(allInputMatched);
		
		//add action listeners with appropriate behaviours:
		cancel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				IJ.showStatus(""); //blank the status bar.
				DialogWindow.this.shutDownDialogWindow();
				DialogWindow.this.dispose();
			}
			
		});
		
		process.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub

				//Should NOT call DialogWindow3.this.processImages() method here
					//this is in the EDT!  Want to create a NEW THREAD
					//to call the processImages() method from!
				
				//First, set the first FileSelector's output title to the content of the output JTextField:
				fileSelectors.get(0).inputOutputFramework.outputName = fileSelectors.get(0).outputTitle.getText();
				
				//The new thread, ProcessThread, is created and started below:
					//This is defined as an inner class which extends the Thread class below..
				
				ProcessThread pThread = new ProcessThread(); //create ProcessThread
				pThread.start(); //Start the ProcessThread's run() method
				
			}
			
		});
		
		//set layout & border, and add components:
		actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.LINE_AXIS) );
		actionPanel.setBorder(BorderFactory.createEmptyBorder(2,2,2,2) );
		actionPanel.add(Box.createHorizontalGlue() );
		actionPanel.add(cancel);
		actionPanel.add(Box.createRigidArea(new Dimension(5,0) ) );
		actionPanel.add(process);
		
		this.add( new JSeparator() );
		this.add(actionPanel);
		
		//verify the matched inputs across all File Selectors 
			//(this initialises correct state of process button):
		DialogWindow.this.verifyMatchedInput();
		
	} //end addActionPanel(String)
	
	
	
	/**
	 * This method lays out and displays the JFrame.
	 */
	public void layoutAndDisplayFrame() {
		//set actionPanel size:
		//actionPanel.setSize(actionPanel.getWidth(), 50);
		//actionPanel.setPreferredSize(new Dimension(actionPanel.getWidth(), 50) );
		
		/* LAYOUT FRAME */
		this.pack(); //pack content into frame.
		this.validate(); //validate this layout.
		GUI.center(this); //centre Window on screen.
		this.setResizable(false);  //stop the Dialog from being resized.
		this.setVisible(true); //make this DialgoWindow visible.

	} //end layoutAndDisplayFrame()
	

	/**
	 * This ProcessThread is used to generate a new thread inside the EDT and run the processImages() method in
	 * a new thread when the Process Button is pressed on the Dialog.
	 * @author stevenwest
	 *
	 */
	protected class ProcessThread extends Thread {
		
		/**
		 * The run method runs the processImages() method inside the DialogWindow class.
		 */
		@Override
		public void run() {

			DialogWindow.this.processImages();

		}
		
	}
	
	
	/*
	 * DIALOG WINDOW IMAGE PROCESSING:
	 * 
	 * The DialogWindow needs a set of methods to retrieve all images from FileSelectors
	 * which can be used in the PlugIn as input. The input can be either images, in which case
	 * the files must be checked for validity (i.e can they be opened?) during the processing, and
	 * any files which are not should be logged and the processing continued.
	 * <p>
	 * The input may also be data files, including ROIs, Results, or other data files which provide
	 * information for the PlugIn.  These should be able to be accessed as a File Array. The processing
	 * must be flexible, thus DialogWindow need to provide a number of structures to allow the
	 * processing and parsing of different numbers of FileSelectors, and their contents.
	 * <p>
	 * Also, FileSelector objects themselves need tags to indicate what type of file they are planning
	 * to collect (File Selection), what kind of File [DIR, File], as well as a Tagging framework to 
	 * ensure fileSelectors which need to choose specific outputs from certain algorithms are choosing 
	 * the correct type of file (Input Selection).
	 * <p>
	 * TAGS of File Selection Type: IMAGES, ROIS, DATA
	 * <p>
	 * TAGS of Kind of Input: IMAGE, SIMPLE_DIR, COMP_DIR, ALL
	 * <p>
	 * TAGS of Input Selection: [based on StereoMate.info File - for example, RoiDisector]
	 * 							This "tag" should inform the fileSelector to look for specific
	 * 							content in the StereoMate.info file...
	 * <p>
	 * An overall tagging framework is required for the fileSelector collection, which indicates whether 
	 * different fileSelectors need to select the same DIR-Tree / File Structure: 
	 * <p>
	 * [i.e. if two fileSelectors are created, this tagging system would indicate whether they must 
	 * 'match', and pick the same file structure of simple files, simple DIRs, or compound DIRs (in 
	 * which case, the compound DIRs should have the same depth, number of files to be processed, etc)]
	 * <p>
	 * TAGS for FileSelector Collection:  MATCHED_INPUT [no other tags required, as can constrain the
	 * 													input on individual FileSelectors by other TAGS
	 * 													above]
	 * <p>
	 * <p>
	 * Structurally, the input from each FileSelector can be either a simple File, a simple DIR, or a 
	 * compound DIR. Each of these structures must be dealt with by the processing methods, for each 
	 * type of file: images or data.
	 * <p>
	 * This is further complicated by the presence of constrained analyses - where an input DIR is
	 * selected, and the analysis is constrained to a sub-DIR in the input DIR. This should not affect
	 * the structures required for processing, but constrained analyses will need to be handled within
	 * these different structures -> ie the output will be different!
	 * <p>
	 * Importantly, all of this information is available in the FileSelectors, and their corresponding
	 * InputOutputFramework objects -> The question is now to make an easily accessible interface in
	 * gathering the appropriate information, getting all of the input image & data files, and providing
	 * an easy to use interface for these images to be saved after processing.
	 * <p>
	 * Types of Processing Structure:
	 * <p>
	 * A. Basic Parsing Procedure:
	 * <p>
	 * The basic parsing procedure involves
	 * <p>
	 * B. Parsing one or more FileSelectors which have selected IMAGE FILES:
	 * <p>
	 * This is the easiest Structure to deal with.  There are two options when processing: 1. Files
	 * are retrieved from the appropriate DIR structures, and are processed in a loop by passing
	 * an imp Array containing one ImagePlus from each FileSelector, which is sent to a blank/abstract 
	 * method that is used by the PlugIn programmer to access the imps, and do the appropriate
	 * processing.
	 * <p>
	 * 2. Alternatively, the arrays of potential files to process from each FileSelector are passed
	 * to a blank method, and the user can access these files in whatever manner they wish to.
	 * <p>
	 * B. 
	 * The first step, in the case of a DIR (simple or compound) is to generate a mirror Dir Tree
	 * in the output DIR.  This is retrieved from the inputOutputFramework located in the appropriate
	 * FileSelector.
	 * <p>
	 * The inputOutputFramework MUST have a place to store the FilePaths for OUTPUT which mirror the
	 * FilePaths for INPUT. Without this, saving the series of images to appropriate locations throughout
	 * a complex DIR tree would be essentially impossible.
	 * <p>
	 * When the process of determining the OUTPUT filePaths which correspond to the INPUT filepaths is 
	 * performed, the DIR Tree itself can be created.  This must occur within the FileSelector object 
	 * which is being used as input.
	 * <p>
	 * Let us consider the types of input which may occur in the Dialog Window:
	 * <p>
	 * 1 FileSelector: Used to process individual images, images in a folder, or images at the bottom of
	 * a DIR Tree.
	 * <p>
	 * 2 FileSelectors:
	 *  - Used to select two images, or an image and data file which will be processed together.
	 *  - Used to select two folders containing the same number of images which will be processed 
	 *  	together in order.
	 *  - Used to select two DIR Trees containing the same structure of images or other file which
	 *  	will be processed together in order.
	 *  - Used to select an image, folder or DIR Tree along with another image or data file, where each
	 *  	image from the first file selector will be processed along with the second image or data file.
	 * 3 FileSelectors:
	 *  - Unlikely to be used, but the only way this might be used is to:
	 *  	Select one Image File, Simple or Compound DIR, and then 2 individual image or data files, and
	 *  	process each file in the first FileSelector together with the 2 individual files.
	 *  	Potentially selecting one Image Folder or DIR Tree, another Image or Data Folder or DIR Tree
	 *  which mirrors the first, and a third Image or Data File, or mirrored Folder or DIR Tree.
	 * 		Or - a combination of those!
	 * 
	 * This implies that with some FileSelectors, only Files can be selected (whether an image or data 
	 * file is irrelevant - but in some cases it would be highly desirable to prevent the FileSelector 
	 * from choosing a DIR).
	 * 
	 * It also shows that only one FileSelector should have a constrain analysis function and an output
	 * name & dir function -> input constraining and output can only feed through one path. So extra
	 * FileSelectors should be made without these functions.
	 * 
	 * I.E. There should only be ONE OUTPUT per computation!  Are there cases where this may be different?
	 * 		Would two input images ever lead to the output of two images?
	 * 		What about outputting an image file and a results file?  That certainly could happen!
	 * 		Is it even true that the algorithm will always output an image?!
	 * 	
	 * The framework should factor in the flexibility so the User has access to the inputs arrays, 
	 * and is able to save multiple images/files as output, or only data files, or even process
	 * all images in an array, and finally only output a single file at the end of the computation.
	 * 
	 * THIS REQUIRES A MINIMALIST APPROACH TO PROCESSING STRUCTURE!  PROVIDE THE PROGRAMMER OF THE
	 * STEREOMATEALGORITHM WITH ALL THE REQUIRED DATA, AND LET THEM DO THE APPROPRIATE PROCESSING
	 * LOCALLY??
	 * 
	 * Finally, the important point is there is only one Output Path for the entire DialogWindow, and
	 * this Output Path must match files on an input path from one FileSelector.  Other FileSelectors
	 * should provide an input path, and it is for the User to decide how to use that input path.
	 * 
	 * Thus, a Structure which is flexible for input and output in the DialogWindow is to treat the 
	 * first FileSelector as the major output.  This should contain a reference to an inputFilePaths
	 * array and a mirroring outputFilePaths array (indeed, this can be created into an array of a
	 * new object containing just two String references for input and output).
	 * 
	 * Other FileSelectors must be scaled back in terms of functionality - they can select files or
	 * DIRs (if desired), but should not provide any functionality in terms of output or constraining.
	 * 
	 * Finally, FileSelectors should also have the functionality to turn on or off the ability to select
	 * DIRs - as the input may only be appropriate if it receives images or a file.
	 * 
	 */

	
	
	
	
	/**
	 * This method is called whenever the Process button is pressed on the
	 * ActionPanel. This method contains standard code which creates the outputDir Tree,
	 * and generates ArrayLists for output and inputs.  This dialogWindow is then removed
	 * from the screen, the first image is retrieved from the inputs, and this is
	 * passed to the process(imp) method of the ImageJAlgorithm interface, which defines
	 * the image processing algorithm run by the PlugIn implementing the ImageJAlgorithm
	 * interface.
	 * <p>
	 * Note, that due to the fact GUI components and therefore listener objects are often
	 * used in image processing algorithms, which tend to create new threads, a simple
	 * loop through each image cannot be performed in this class.  Instead, the actual PlugIn
	 * object (which implements ImageJAlgorithm, and uses this class for data input) should
	 * call the process() method via this class using one of the two following methods:
	 * <p>
	 * processNextImp();
	 * <p>
	 * saveAndProcessNextImp(imp);
	 * <p>
	 * These methods ensure the next imp in the input is retrieved, checks it is a valid
	 * image for input (and logs and skips it if it is not), and presents this to the 
	 * process(imp) method of the containing PlugIn class.  If no more images remain to
	 * be processed, the algorithm ends.
	 * <p>
	 * By using both this method to initialise processing, and one of the above methods for
	 * continuing processing of a set of images, complex GUI behaviour can be implemented
	 * consisting of multiple threads, and only once that has finished will the next imp
	 * be passed to the process() method.
	 * <p> 
	 * <p>
	 * a loop is set up to work through each item in the inputs list.
	 * This is sent to the abstract method processImages( ArrayList<File>), which MUST be
	 * defined in any sub-class of this abstract class.  This method returns an imp, which
	 * is received in this method and used to save the imp to the correct output filePath.
	 * Finally, the imp is closed and set to null (for garbage collection).
	 * 
	 * is blank, but can be overwritten in an anonymous
	 * inner class of this object. This method should be overwritten to call a subroutine
	 * in the class where this dialog is used, to implement the next steps after
	 * the dialog step has been complete.
	 */
	public void processImages2() { 
		
		//First, create DIR Tree for outputs:
			//This generates all the DIRs necessary for saving output.
		createOutputDirTree();
		
		//Create output ArrayList of files from the createOutputArray() method:
		output = createOutputArray(); //Note, output array is derived from First
														//FileSelector only..
		//IJ.showMessage("output: "+output.get(0) );
		
		fillInputsArray();
		
		for(int a=0; a<inputs.size(); a++) {
			//IJ.showMessage("Inputs Array No. "+a+": " + "\n" +inputs.get(a).get(a) + "\n"  );
		}

	}
	
	
	public void processImages() { 
		
		//First, create DIR Tree for outputs:
			//This generates all the DIRs necessary for saving output.
		createOutputDirTree();
		
		//Create output ArrayList of files from the createOutputArray() method:
		output = createOutputArray(); //Note, output array is derived from First
														//FileSelector only..

		//Fill the inputs ArrayList:
		
		//Loop through all fileSelectors, and store the fileArray from each ones inputOutputFramework
		//into it:
		
		//This is called in a separate method, fillInputsArray():
		
		fillInputsArray();

		
		//Now, the inputs ArrayList<ArrayList<File>> contains a reference to each ArrayList<File> object
		//from each FileSelector.
		
		//Therefore, the reference to the current image will refer to the index in each FileSelectors FileArray.
			//Or, to each ArrayList<File> inside inputs.

		//Now the processing can begin:
		
		//First, hide this DialogWindow:
		//this.setVisible(false);
		this.dispose();
		
		//Now, initialise the totalFileCount and currentFileIndex instance variables:
			//These variables store the total number of files to be processed from the First FileSelector, and
			//the current index of the currently processed image
				//[This assumes that images will be processed in turn -> can be overwritten to referring to
				//inputs array directly]
		setUpFileCountIndex();
		
		//Set the IJ status bar to (currentFileIndex + 2)  of (totalFileCount + 2)
		setIJStatus();
		
		//Then, need to retrieve the First Imp from the First FileSelector:
		//ImagePlus imp = getNextImp(0);
			//No Longer required!  Can call the methods in this class from the process() method in the 
				//StereoMateAlgorithm to retrieve current and next imp.
		
		//FINALLY - Call the process(imp) method in the ImageJAglorithm interface.
		// Should perform the loop here, and call the process() method in SMA within that loop
			//This will remove complexity of having to code this each time in each algorithm...
		
		//First, open the first file as an imp:
		ImagePlus currentImp = getCurrentImp(0); //this method will continue to attempt to open files from the input
											//from FileSelector0 until one returns an imp.
					//If no imp can be returned, this method will return null, but importantly the dw.currentFileIndex
				//will EQUAL the dw.totalFileCount -> therefore the while loop will not execute..
					
		sma.setup();
		
		
		//next, set the totalFileCount variable to manualTotalFileCount IF the overrideTotalFileCount variable
			//is TRUE:
		
		//FIRST - store the value of totalFileCount to totalFileCountOriginal, so the actual number of files can
			//be retrieved easily:
		totalFileCountOriginal = totalFileCount;
		
		if(overrideTotalFileCount == true) {

			totalFileCount = manualTotalFileCount;
				//This should work as totalFileCount is set in listeners of the FileSelector, but by this point
					//in the program totalFileCount is fixed.
		}
		
		while( moreFilesToProcess() ) {  // This while loop ensures files are processed only while
																	//current file index is less than totalFileIndex...
					
			//First, set ij status:
			setIJStatus();

			//Do the Computations:
					
			//process the imp in SMA's process() method
			
			//First, check currentImp for number of frames:
				//If only 1, then pass to the process() method in SMA
				//IF more than 1, do not pass to process() method in SMA, and log the information:
			int frames = currentImp.getNFrames();
			
			// IJ.log("Frames: "+frames);
			
			if(frames > 1) {
				///DO NOT PROCESS IMAGES WITH FRAMES
				//do not pass the image to the process() method, but log this image as containing
				//more than one frame:
				IJ.log("Image number: " + getCurrentFileIndex() );
				IJ.log("Image title: " + getCurrentFile(0).getName() );
				IJ.log("IMAGE CONTAINS FRAMES - NOT PROCESSED");
			}
			else if(currentImp.getBitDepth() == 24) {
				//DO NOT PROCESS IMAGS WHICH ARE RGB:
				//do not pass the image to the process() method, but log this image as containing
				//more than one frame:
				IJ.log("Image number: " + getCurrentFileIndex() );
				IJ.log("Image title: " + getCurrentFile(0).getName() );
				IJ.log("IMAGE IS RGB - NOT PROCESSED");
			}
			else {
				// Calls the process() method on the StereoMateAlgorithm:
				sma.process(currentImp);
					// Note:  If this method completes, then the BELOW CODE is also run!
					// This means the currentFileIndex will be incremented below 
				//Check if this method returns:
				// IJ.showMessage("processImages() post process - file index: "+currentFileIndex);
			}
					
				//save the imp to its designated output DIR:
				//May not always want to do this!  Leave this up to programmer of PlugIn:
					//The Programmer can call saveImpAndClose, or access the current DIR to save
					// othjer types of output to...
				//saveImpAndClose(currentImp);
					
				// finally, increment the currentFileIndex in dw:
				incrementCurrentFileIndex();
				
				// IJ.showMessage("processImages() post increment - file index: "+currentFileIndex);
					
				//open the next file as an imp:
				currentImp = getCurrentImp(0); //Need to do this at the end, to ensure the while loop checks the 
											//currentFileIndex and totalFileCount appropriately in getCurrentImp().
						//This returns NULL if the currentFileIndex == totalFileCount, but in this case the while loop
						//will also terminate!
					
		}//end while loop
		
		setIJStatus();
		
		//Call the process(imp) method in the ImageJAglorithm interface.
			//This is what would have happened previous to inserting the loop into this method:
		//sma.process();
			//This method is called to process images
				//It is the programmers responsibility to implement the correct processing based on the input.
		
		//FINALLY, call clean-up code method?
			//How can the sma class control this method without being forced to define it via the interface?
			//Can the dw send a message to the SMA class???
			//It HAS to be via na interface method...
		
		sma.cleanup();
		
		
	} //end processImages()
	
	
	
	/**
	 * Creates the output DIR based on the first FileSelectors [constrained] input, and the designated
	 * output DIR title.
	 */
	public void createOutputDirTree() {
		
		fileSelectors.get(0).inputOutputFramework.createOutputDirTree();
		
	} // end createOutputDirTree()
	
	/**
	 * Deletes the output DIR from first FileSelector.  Can be used to eradicate the Output DirTree
	 * if no output is created in the processing algorithm (i.e. if all processing fails).
	 */
	public void deleteOutputDirTree() {
		fileSelectors.get(0).inputOutputFramework.deleteOutputDirTree();
	}
	
	
	/**
	 * This method generates the output file array -> the DIR path and file name for each input
	 * image from the First FileSelector.  No extension is provided.  This array can be used to
	 * save files to the matching output DIR tree as the input DIR tree from the First FileSelector.
	 * It will also use the same title as the input image title, and can be used to save an ouput
	 * image, or a result file, associated with the computation.
	 * <p>
	 * The output array generated by this method is used in methods in this class used to save 
	 * output.
	 * <p>
	 * Output Array is created in the FIRST fileSelector object -> which represents the only
	 * fileSelector that has output.  Subsequent fileSelectors can be used for input, but
	 * images to be saved to output can be performed relative to the file or DIR selected on the
	 * first fileSelector (index 0) by using the 'output' ArrayList in this class.
	 */
	public ArrayList<File> createOutputArray() {
		
			fileSelectors.get(0).inputOutputFramework.setOutputFileArray();
			
			return fileSelectors.get(0).inputOutputFramework.outputFileArray;

	} // end createOutputArray()
	
	
	
	/**
	 * This method fills the inputs array, by providing an ArrayList<File> object derived from
	 * each file selector's inputsArray into each position in the inputs ArrayList.  This means
	 * the first index is for the fileSelectors, and the second index is for the files within a
	 * file selector.
	 * 
	 */
	protected void fillInputsArray() {
		//Initialise inputs:
		inputs = new ArrayList<ArrayList<File>>();
		//Fill each position in inputs ArrayList with an ArrayList<File> from each fileSelector:
		for(int a=0; a<fileSelectors.size(); a++) {
			inputs.add(fileSelectors.get(a).inputOutputFramework.fileArray);
		}
	}
	
	/**
	 * This method fills the inputs array, which provides a convenient file structure that lays out
	 * files in the second dimension of the array such that each fileSelector's files are laid out 
	 * in order through the array, and the fileSelectors themselves are referenced by their position 
	 * in the first dimension of the File Array.
	 * <p>
	 * This method performs important pre-processing of file arrays from all fileSelectors, to ensure
	 * that subsequent fileSelectors always match the number of input files as the FIRST fileSelector.
	 * <p>
	 * If a different number of files is provided by a subsequent fileSelector, and the file number is
	 * greater than 1, then only the first file is return continuously in the inputs file array. To access
	 * the other files, a call to getFileArray(int fileSelectorIndex) will retrieve the appropriate 
	 * fileArray for the programmer to access.
	 * 
	 */
	@Deprecated
	protected void fillInputsArrayParallel() {
		
		//Create an ArrayList of ArrayList<File> objects:
		//This ArrayList contains items which are themselves ArrayLists of Files.
		//Each Item contains an ArrayList of Files.  
			//Each ArrayList of Files contains a File Reference from EACH File Selector.
		inputs = new ArrayList<ArrayList<File>>();
		
		//Now to fill this inputs array:
		
		
		//Care must be taken here to fill the inputs array correctly.
		//Each item must consist of an ArrayList<File> object which contains one file from each
			//FileSelector, in the order they are presented in the ArrayList in the FileSelector object
			//AND the items placed in order of the FileSelectors (i.e. the first File in the item list
			//of the ArrayList is from the FIRST FileSelector, SECOND file is from the SECOND FileSelector
			//etc.
		//NOTE: This is also dependent on the types of files selected by the FileSelectors.  If they do
			//not match, appropriate means of dealing with the Files is required here...  See above
			//discussion on DialogWindow Image Processing.
	
	
		//for(int a=0; a<fileSelectors.size(); a++) {
		
			//inputs.add(fileSelectors.get(a).inputOutputFramework.fileArray);
		
		//}
		
		//THIS IS INCORRECT -> currently each inputs item is a whole fileArray from a fileSelector.
		//This needs to be re-sorted to add the first file from each fileArray on the fileSelectors
		//to a ArrayList object, which then must be put into inputs.  And this needs to be repeated
		//for each File found in the fileArrays across all fileSelectors...
		
		//The only FileArrays which can be parsed if more than one FileSelector exists [and the only
		//structures which make sense] are either:
		// FIRST FileSelector selects a file, simple DIR or compound DIR + SECOND FileSelector selects
		// a MIRRORING file, simple DIR or compound DIR.
		//FIRST FileSelector selects a file, simple DIR or compound DIR + SECOND FileSelector selects
		//A File ONLY.
		//There can be no way in which the Second FileSelector selects a Different DIR structure to the
		//First FileSelector.  As it becomes impossible to pass appropriate Files for input (the number
		//of Files in the two FileSelectors are not guaranteed to match!]
		//Therefore, the behaviour of the FileSelectors if not the first one, must select either a DIR
		//with the same DIR Tree Strcuture as the First FileSelector, or select a single file, which is
		//ALWAYS passed with each item in the fileArray from the First FileSelector.
		
		//The other option is to allow the Second FileSelector to select a DIFFERENT DirTree DIR from the
		//First FileSelector, and each File in the FileArray of the First FileSelector is passed Along with
		//the ENTIRE FileArray from the Second FileSelector.
			//Would this actually ever be useful or used?
			// For now, lets assume this is not particularly useful, and so will not program this
			//behaviour into the FileSelector inner class...
		
		// The below code assumes that all subsequent FileSelectors after the First FileSelector either:
		// 1. Mirrors the First FileSelector input DIR Tree (i.e. same number of files, same depth etc.
		// 2. Selects a single File, which is then put into input over and over with each file
			//from the fileArray from the First FileSelector.
		
		
		
		//First, retrieve the first fileSelectors fileArray size:
		int firstFileSelectorFileCount = fileSelectors.get(0).inputOutputFramework.fileArray.size();
		
		//Next, determine for each fileSelector whether its fileArray size matches the first
		//fileSelectors fileArray size:
		
		//Create a Boolean ArrayList to store this information in:
		ArrayList<Boolean> fileSelectorsFileArraySizes = new ArrayList<Boolean>();
		//loop through each FileSelector:
		for(int a=1; a<fileSelectors.size(); a++) {
			if(fileSelectors.get(a).inputOutputFramework.fileArray.size() == firstFileSelectorFileCount) {
				//if the current fileSelectors file array count is equal to the first file selectors file array
				//size, set the next ArrayList<Boolean> item to true:
				fileSelectorsFileArraySizes.add(true);
			}
			else {
				//Else the FileSelectors fileArray is different in size, so set the ArrayList<Boolean> item
				//to false:
				fileSelectorsFileArraySizes.add(false);
			}
		}//end for loop.
		
		//Now, the ArrayList<Boolean> is filled with boolean values indicating if each FileSelectors 
		//fileArray has the same number of Files as the First FileSelector.
		//These Booleans can be used below to Fill the ArrayList<ArrayList<File>> 'inputs' correctly:
		
		
		//First, create a loop which is as large as the first fileSelectors fileArray size:
		for(int a=0; a<firstFileSelectorFileCount; a++) {
			
			//Create a local fileArrayList to build an ArayList<File> object to put into 
			//inputs:
			ArrayList<File> fileArrayList = new ArrayList<File>();
			
			//start to fill this fileArrayList:
			
			//first, loop through the fileSelectors:
			for(int b=0; b<fileSelectors.size(); b++) {
				
				if(b == 0) {
				
					//if b is 0, this refers to the first fileSelector. Here, just add
					//the File referenced by 'a' from the first FileSelector fileArray:
					fileArrayList.add(fileSelectors.get(b).inputOutputFramework.fileArray.get(a));
				
				} //end if b == 0
				
				else {
					//else, b is larger than 0, so now the fileSelectorsFileArraySizes boolen values
					//must be checked to figure out what item in the fileArray to add to fileArrayList:
					
					//Check the fileSelectorsFileArraySizes appropriate to the fileSelector count:
					boolean arraySizeBoolean = fileSelectorsFileArraySizes.get(b-1);
						//Note, 'b-1' as the fileSelectorsFileArraySizes starts with the second
						//FileSelector, not the first!
					//Now, check if arraySizeBoolean is true or false, and implement appropriate
					//behaviour:
					if(arraySizeBoolean == true) {
						//if its true, the current fileSelector has the same number of Files in its
						//FileArray as the first one.  This means the inputs are matched, and so each
						//File reference in this fileSelectors FileArray should be added to the 
						//fileArrayList in the order it is present in the original fileArray:
						
						fileArrayList.add(fileSelectors.get(b).inputOutputFramework.fileArray.get(a));
					}
					else {
						//Else, the current fileSelector has a different number of File in its
						//FileArray compared to the First FileSelector.  Since the inputs are not
						//matched, only the FIRST File in the FileArray should be used [infact, if
						//the fileSelectors inputs do not match, any subsequent fileSelectors AFTER
						//the First One should ONLY have ONE FILE in them - as inputs should be matched
						//or subsequent FileSelectors should ONLY select individual Files for input]
						//Thus, use the appropriate reference for the FIRST item in the fileArray, 
						//which is '0':
						fileArrayList.add(fileSelectors.get(b).inputOutputFramework.fileArray.get(0));
					}
					
				} //end else b > 0.
		
			} //end for b
			
			//At this point, the fileArrayList should have file references from each FileSelector stored
			//in the order of each fileSelector.
			//Now, the fileArrayList should be added to the inputs FileArray<FileArray<File>>:
			inputs.add(fileArrayList);
	
		} //end for a
	
	//Now, the inputs ArrayList<ArrayList<File>> should be filled with ArrayLists in which each item contains
		//an ORDERED set of Files from each FileSelector in turn, which should extend through the entire
		//length of files found in the First FileSelectors fileArray.
		//This is precisely the input needed, so now can proceed with the processing of these Files:
	
	} //end fillInputsArray()


	/**
	 * This method initialises the totalFileCount and currentFileIndex instance variables for the start
	 * of image processing.  Calling this method outside of this class will result in resetting the 
	 * currentFileIndex, and thus re-start calls to getNextImp() & allow image processing to begin from
	 * the start.
	 */
	public void setUpFileCountIndex() {
		
		// First, set the totalFileCount to the number of Files in the first FileSelector fileArray:
		
		//totalFileCount = inputs.get(0).size();
		//IJ.showMessage("inputs0 size: " + totalFileCount);
		
		totalFileCount = fileSelectors.get(0).inputOutputFramework.fileArray.size();
		//IJ.showMessage("fileSelector0 fileArray size: " + totalFileCount);
		
		
		//Next, set the currentFileIndex to -1, to start the index from the start of the inputs file array:
			//Note, it starts at -1, as when the next imp is retrieved, this is incremented THEN to match
			//the current index, and for that to work, this index must start at -1 [incremented to 0 in the first call]
		currentFileIndex = 0;
		
	}
	
	/**
	 * This method returns true if there are still more files to process, that is, if the 
	 * currentFileIndex is less than totalFileCount.  If the currentFileIndex equals
	 * totalFileCount, there are no more images to process, and this method returns false.
	 * @return
	 */
	public boolean moreFilesToProcess() {
		if(currentFileIndex < totalFileCount) {
			return true;
		}
		else {
			return false;
		}
	}


	/**
	 * Sets the ImageJ status to the image being processed.  This will inform the user which image of an array
	 * of images the algorithm is currently working on, and shows the User the algorithm is currently processing.
	 * <p>
	 * If the programmer wishes to add a Progress Bar to the status bar, they should call 'IJ.showProgress(double prog)'
	 * throughout the 'process()' method in the StereoMate algorithm image processing algorithm to indicate how much
	 * progress has been made with the current image.
	 */
	public void setIJStatus() {
		
		int cfi = currentFileIndex+1;
		int tfc = totalFileCount;
		if(cfi <= tfc) {
			IJ.showStatus("Processing image No. "+cfi+" of "+tfc);
		}
		else {
			IJ.showStatus("Processing Complete "+algorithmTitle);
		}
		//ImageJ ij = IJ.getInstance();
		//ij.repaint();
		//Panel statusBar = ij.getStatusBar();
		//statusBar.repaint();
	}
	
	
	
	/**
	 * This method retrieves the current imp based on the current image being processed, from the FileSelector
	 * given the fileSelectorIndex int passed to this method.  This method will open the current image if it
	 * is not already open, and return null if there are no more files to open.  If any file is NOT an openable
	 * image, it is not returned but logged via the addToNonImageLogFile() method, and the next file will be
	 * parsed through this method recursively until another image file is located, or the end of the DIR tree
	 * is reached.
	 * @param fileSelectorIndex An int to index the fileSelector from which the imp should be retrieved.
	 * @return An ImagePlus representation of the Image File retrieved from the given fileSelector.
	 */
	public ImagePlus getCurrentImp(int fileSelectorIndex) {
		
		//IJ.showMessage("currentFileIndex: "+currentFileIndex+" totalFileCount: "+totalFileCount);
		
		//First check if the currentFileIndex is in range, if not, return null, if yes, proceed:
		if(currentFileIndex == totalFileCount ) {
			return null;
		}
		
		//If file index is in range,  set IJ status:
		setIJStatus();
		
		// Collect the appropriate fileSelectors File from the fileArray according to the currentFileIndex
		//int:
			//First sort the fileArray object:
			//Collections.sort(fileSelectors.get(fileSelectorIndex).inputOutputFramework.fileArray);
			//DONT SORT AS MESSES UP INPUT AND OUTPUT FILE ORDERS!!

		File file = fileSelectors.get(fileSelectorIndex).inputOutputFramework.fileArray.get(currentFileIndex);
		
		//IJ.showMessage("File Path: "+file.getAbsolutePath() );
				
		//Create an ImagePlus for returning:
		ImagePlus imp = null;
				
		//This file might be the name of an image open in ImageJ -> check this first:
		//Collect all image titles from the WindowManager:
		String[] titles = WindowManager.getImageTitles();
		for(int a=0; a<titles.length; a++) {
			if(titles[a] == file.getName() ) {
				imp = WindowManager.getImage(titles[a]);
				return imp;
			}
		}
				
		//If this has not succeeded in finding the Image, attempt to open the File:
		imp = IJ.openImage(file.getAbsolutePath() );
				
		//If imp is not null, return imp:
		if(imp != null) {
			return imp;
		}
		
		//If imp is still null, need to log this file as being un-openable, and move onto next imp:
		
		addToNonImageFileLog(file); //log this file which cannot be opened.
		incrementCurrentFileIndex(); //increment the current file index.
		if(currentFileIndex < totalFileCount) { //if the current file index is below total file count,
												//there are still files to attempt to open.
			//Therefore, call this method again:
			return getCurrentImp(fileSelectorIndex);
		}
		else { //if currentFileIndex is equal to totalFileCount, there are no more files to attempt to open
			//Return NULL:
			return null;
			//Note, if a while loop is used in process() method of SMA, and the getCurrentImp() is called at
			//the end of that while loop, when this method returns null, it will also mean currentFileIndex
			//is NOT less than totalFileCount -> thus the while loop will end.  Therefore, the algorithm
			//in the SMA will never see a null imp!
		}		
		
	} //end getCurrentImp()
	
	
	/**
	 * This method saves the passed imp to the appropriate output file, according to the currentFileIndex
	 * value.
	 * @param imp The ImagePlus object to be saved.
	 */
	public void saveImp(ImagePlus imp) {
		
		//If the outputExtension String has been set, use this extension to save the image:
		if(outputExtension != null) {
			IJ.save(imp, output.get(currentFileIndex).getAbsolutePath() + outputExtension );
		}
		//else, save with a ".tif" extension:
		else{
			IJ.save(imp, output.get(currentFileIndex).getAbsolutePath() + ".tif");
		}
		
	}
	
	/**
	 * This method saves the passed imp to the appropriate output file, according to the currentFileIndex
	 * value, and closes the imp.
	 * @param imp The ImagePlus object to be saved.
	 */
	public void saveImpAndClose(ImagePlus imp) {
		
		//IJ.showMessage("output File: "+output.get(currentFileIndex).getAbsolutePath() );
		
		//If the outputExtension String has been set, use this extension to save the image:
		if(outputExtension != null) {
			IJ.save(imp, output.get(currentFileIndex).getAbsolutePath() + outputExtension );
		}
		//else, save with a ".tif" extension:
		else{
			IJ.save(imp, output.get(currentFileIndex).getAbsolutePath() + ".tif");
		}
		imp.close();
		
	}
	
	
	/**
	 * Returns the current input file from the selected fileSelector's fileArray.
	 * @param fileSelectorIndex Index of the fileSelector to retrieve the current input file from,
	 * 0-based index.
	 * @return
	 */
	public File getCurrentFile(int fileSelectorIndex) {
		
		//First check if the currentFileIndex is in range, if not, return null, if yes, proceed:
		if(currentFileIndex == totalFileCount ) {
			return null;
		}
		
		//If current File Index is in range, set IJ status:
		setIJStatus();
		
		
		// Collect the appropriate fileSelectors File from the fileArray according to the currentFileIndex
		//int:
			//First sort the fileArray object:
			//Collections.sort(fileSelectors.get(fileSelectorIndex).inputOutputFramework.fileArray);
			//DONT SORT AS MESSES UP INPUT AND OUTPUT FILE ORDERS!!

		File file = fileSelectors.get(fileSelectorIndex).inputOutputFramework.fileArray.get(currentFileIndex);
	
		return file;
	}
	
	/**
	 * Returns the current file to be processed, form fileSelector at fileSelectorIndex.  It returns the file
	 * as an relative path, the path is relative from the input directory - it does not include the
	 * input directory or the leading File.separator character.
	 * @param fileSelectorIndex
	 * @return
	 */
	public File getCurrentFileRelativeToInput(int fileSelectorIndex) {
		//First check if the currentFileIndex is in range, if not, return null, if yes, proceed:
		if(currentFileIndex == totalFileCount ) {
			return null;
		}

		//If current File Index is in range, set IJ status:
		setIJStatus();

		File file = fileSelectors.get(fileSelectorIndex).inputOutputFramework.fileArray.get(currentFileIndex);

		String dirPath = fileSelectors.get(fileSelectorIndex).filePath;
		
		return new File(  file.getAbsolutePath().substring( dirPath.length() +1 )  );
		
	}
	
	/**
	 * Returns the current file to be processed, form fileSelector at fileSelectorIndex.  It returns the file
	 * as an relative path, the path is relative from the input directory - it does not include the
	 * input directory or the leading File.separator character.
	 * @param fileSelectorIndex
	 * @return
	 */
	public File getCurrentFileRelativeToInputNoExt(int fileSelectorIndex) {
		//First check if the currentFileIndex is in range, if not, return null, if yes, proceed:
		if(currentFileIndex == totalFileCount ) {
			return null;
		}

		//If current File Index is in range, set IJ status:
		setIJStatus();

		File file = fileSelectors.get(fileSelectorIndex).inputOutputFramework.fileArray.get(currentFileIndex);

		String dirPath = fileSelectors.get(fileSelectorIndex).filePath;
		
		return new File(  file.getAbsolutePath().substring( dirPath.length() +1, file.getAbsolutePath().length()-4 )  );
		
	}
	
	
	/**
	 * Returns the 'input file' from the selected fileSelector, which is the file selected by the fileSelector!
	 * @param fileSelectorIndex Index of fileSelector.
	 * @return
	 */
	public File getInputFile(int fileSelectorIndex) {
		
		return fileSelectors.get(fileSelectorIndex).inputOutputFramework.file;
		
	}
	
	
	/**
	 * Retrieves the current output file - with NO EXTENSION - for the image being processed.  
	 * <p>
	 * ONLY VALID IF DW THREAD PASUED IN PROCESS() METHOD!  If not use: getCurrentOutputFileNoPause(), see below.
	 * <p>
	 * NOTE:  This method ASSUMES the DialogWindow Thread has been PAUSED during the execution of the SMA.process() 
	 * method (or one of its sub-methods), which will prevent the increment of the currentFileIndex at the end of 
	 * the while loop in the processImages() method in this class.
	 * <p>
	 * If the DialogWindow thread has NOT been paused ( using dw.pause() ), THEN the currentFileIndex will have been
	 * incremented!
	 * <p>
	 * This will only happen if the setMethodCallNumber() has been called to Hack the current processImage() method
	 * and its while loop.  This is called if the user wants to just call one image in first instance, and then
	 * control the display of further images with user-pressed buttons.
	 * <p>
	 * The problem is as the dw.pause() method may not be called in the SMA.process() method, the while loop will
	 * complete, which results in the currentFileIndex being incremented, equalling the (artifically set)
	 * totalFileCount, and causing the processImages() method to return.
	 * <p>
	 * The hack for the getCurrentOutputFile() is just to use currentFileIndex-1, which is implemented in the
	 * getCurrentOutputFileNoPause() method.
	 * @return The current output File object, with no extension.  Only true if dw.pause() is called and therefore the
	 * process() method has not returned.
	 */
	public File getCurrentOutputFile() {
		return output.get(currentFileIndex);
	}
	
	
	/**
	 * This method retrieves the current output file - with NO EXTENSION - for the image being processed.
	 * <p>
	 * This is only TRUE if hte process() method has been allowed to complete, and therefire the currentFileIndex
	 * integer has incremented by 1 in the processImages() method (which will also have returned).
	 * <p>
	 * ONLY Used if the DialogWindow processImages() structure has been HACKED using the setMethodCallNumber() method
	 * to only call a set number of images (typically 1).
	 * <p>
	 * Otherwise this method will return the previous images output File, or return an Exception if its the First image
	 * file!!
	 * 
	 * @return The current output File object, with no extension.  Only true if dw.pause() is NOT called and therefore the
	 * process() method has returned.
	 */
	public File getCurrentOutputFileNoPause() {
		return output.get(currentFileIndex - 1);
	}


	public int getCurrentFileIndex() {
		return currentFileIndex;
	}
	
	public void incrementCurrentFileIndex() {
		currentFileIndex = currentFileIndex + 1;
	}
	
	public void decrementCurrentFileIndex() {
		currentFileIndex = currentFileIndex - 1;
	}
	
	
	public void setCurrentFileIndex(int newIndex) {
		currentFileIndex = newIndex;
	}
	
	public int getTotalFileCount() {
		return totalFileCount;
	}
	
	public void incrementTotalFileCount() {
		totalFileCount = totalFileCount + 1;
	}
	
	public void decrementTotalFileCount() {
		totalFileCount = totalFileCount - 1;
	}
	
	
	public void setTotalFileCount(int newCount) {
		totalFileCount = newCount;
	}
	
	
	/**
	 * This method will increment BOTH totalFileCount and currentFileIndex, but only increments both if
	 * the result is such that currentFileIndex is less than totalFileCount.
	 * <p>
	 * Also will compare the increment against the original (ie. actual) totalFileCount, to ensure any
	 * calls to getCurrentImp() do not hit an IndexOutOfBoundsException.
	 */
	public void incrementTotalFileCountAndCurrentFileIndex() {
		//only increment currentFileIndex IF it is less than totalFileCount,
			//Otherwise just increment totalFileCount to make it more than currentFileIndex!
		if(totalFileCount < totalFileCountOriginal) {
		
			if(currentFileIndex < totalFileCount ) {
				currentFileIndex = currentFileIndex + 1;
			}
		
			totalFileCount = totalFileCount + 1;
		
		}
		else {
			currentFileIndex = 0;
			totalFileCount = 1;
		}
	}
	
	/**
	 * This method will decrement BOTH totalFileCount and currentFileIndex, but only decrements both if
	 * the result is such that currentFileIndex is less than totalFileCount.
	 * <p>
	 * Also will check decrement in totalFileCount, to ensure it does not go below 1 (or currentFileIndex
	 * below 0!), and so calls to getCurrentImp() do not hit an IndexOutOfBoundsException.
	 */
	public void decrementTotalFileCountAndCurrentFileIndex() {
		//only increment currentFileIndex IF it is less than totalFileCount,
			//Otherwise just increment totalFileCount to make it more than currentFileIndex!
		if(totalFileCount > 1) {
		
			if(currentFileIndex == totalFileCount ) {
				//take two away from currentFileIndex
				currentFileIndex = currentFileIndex - 2;
			}
			else {
				currentFileIndex = currentFileIndex - 1;
			}
		
			totalFileCount = totalFileCount - 1;
		
		}
		else {
			totalFileCount = totalFileCountOriginal;
			currentFileIndex = totalFileCount - 1;
		}
	}
	
	
	/**
	 * This method will return the output parent file from this Dialog Window.  This is the Directory in
	 * which the input on first file selector Dir Tree is reflected, and where all output is saved to.
	 * @return
	 */
	public File getOutputParentFile() {
		return fileSelectors.get(0).getOutputParentFile();
	}
	


	/**
	 * This method retrieves the next imp from the First fileSelector, and passes this to the 
	 * process(imp) method in the ImageJAlgorithm interface.
	 */
	@Deprecated
	public void processNext() {
	
		//increment currentFileIndex:
		currentFileIndex = currentFileIndex + 1;
		
		//Set the IJ status bar to (currentFileIndex + 2)  of (totalFileCount + 2)
		setIJStatus();
		
		//and call the process() method in the SM Algorithm:
		if(currentFileIndex < totalFileCount - 1 ) {
			//sma.process();
		}

	}
	
	
	/**
	 * This method retrieves the next imp from the First fileSelector, and passes this to the 
	 * process(imp) method in the ImageJAlgorithm interface.
	 */
	@Deprecated
	public void processNextImp() {
		//get next imp:
		ImagePlus imp = getNextImp(0);
		//Set the IJ status bar to (currentFileIndex + 2)  of (totalFileCount + 2)
		setIJStatus();
		
		//and send this imp to the process() method, if not null:
		if(imp != null) {
			//sma.process();
		}

	}

	
	
	/**
	 * This method should return the next ImagePlus in the inputs stream, from the relevant
	 * fileSelector index.  Calling this method will increment the current index, which means
	 * subsequent calls will retrieve the following ImagePlus object.  To access the current imp
	 * based on this index without incrementing it, use a call to getCurrentImp(int fileSelectorIndex).
	 * <p>
	 * This method returns an imp, which is either generated from a file object (and it assumes the
	 * file object will return a valid imp) or returns an imp present in the ImageJ window.
	 * If a file object does not return an imp, the DW will log this result, and proceed to retrieve
	 * the next imp from the next file object.  This method only returns null when ALL files have been
	 * processed, or attempted to be processed.  Thus, a return of NULL indicates all images have
	 * been processed, and should be used to detect when the processing of all images is complete.
	 * 
	 * @param fileSelectorIndex This int represents which fileSelector the imp should be retrieved from.
	 * 
	 * @return An ImagePlus object of the next image to be processed, or null if all images have been
	 * returned.  Note, if the fileSelector cannot return an imp from the next image (if, for example, the
	 * file object does not point to a valid imp) then that file is skipped, and the next one is opened.
	 * This is logged, and skipped images can be retrieved from the DialogWindow object. This method only
	 * returns NULL when all images have been returned.
	 */
	@Deprecated
	protected ImagePlus getNextImp(int fileSelectorIndex) {
		
		//First check if the currentFileIndex is in range, if not, return null, if yes, proceed:
		if(currentFileIndex == totalFileCount - 1 ) {
			return null;
		}
		
		//And immediately increment the currentFileIndex by 1:
			//Note, it starts at -1 to ensure the currentFileIndex is correct when saving any files using this index
		currentFileIndex = currentFileIndex + 1;
		
		// Collect the appropriate fileSelectors File from the fileArray according to the currentFileIndex
		//int:
		File file = fileSelectors.get(fileSelectorIndex).inputOutputFramework.fileArray.get(currentFileIndex);
		
		
		//Create an ImagePlus for returning:
		ImagePlus imp = null;
		
		//This file might be the name of an image open in ImageJ -> check this first:
		//Collect all image titles from the WindowManager:
		String[] titles = WindowManager.getImageTitles();
		for(int a=0; a<titles.length; a++) {
			if(titles[a] == file.getName() ) {
				imp = WindowManager.getImage(titles[a]);
				return imp;
			}
		}
		
		//If this has not succeeded in finding the Image, attempt to open the File:
		imp = IJ.openImage(file.getAbsolutePath() );
		
		//If imp is not null, return imp:
		if(imp != null) {
			return imp;
		}
		
		//if imp is null, then the file does not point to any detectable image.
		//In this case, DO NOT RETURN NULL!  
		//Log this File as a non-image file in the nonImageFileLog.
		//And then call this method again.
		addToNonImageFileLog(file);
		
		return getNextImp(fileSelectorIndex);
		
	}


	/**
	 * This method saves the passed imp to the appropriate output file, according to the currentFileIndex
	 * value.  This is followed by retrieving the next imp, and passing this to process(imp) method in
	 * the ImageJAlgorithm interface.
	 * @param imp
	 */
	@Deprecated
	public void saveAndProcessNextImp(ImagePlus imp) {
		
		//If the outputExtension String has been set, use this extension to save the image:
		if(outputExtension != null) {
			IJ.save(imp, output.get(currentFileIndex).getAbsolutePath() + outputExtension );
		}
		//else, save with a ".tif" extension:
		else{
			IJ.save(imp, output.get(currentFileIndex).getAbsolutePath() + ".tif");
		}
		
		//close the imp:
		imp.close();
		
		//Next, retrieve the next image and store into imp:
		ImagePlus imp2 = getNextImp(0);
		//Set the IJ status bar to (currentFileIndex + 2)  of (totalFileCount + 2)
		setIJStatus();
		//and pass this imp into the process() method in the ImageJAlgorithm interface / PlugIn.
		// ONLY IF the imp is not equal to NULL:
		if(imp2 != null) {
			//sma.process();
		}
	}



	protected void addToNonImageFileLog(File file) {
		// TODO Auto-generated method stub
		nonImageFileLog.add(file);
	}


	/**
	 * This method will print to the log window information about the files and images processed,
	 * including how many files were successfully processed, and how many as well as which
	 * files were omitted due to failure to open.
	 * <p>
	 * This method can be called as a final step during processing to provide the user with useful
	 * information on the processing of the images.
	 */
	public void printLog() {
		int numberOfImagesProcessed = totalFileCount - nonImageFileLog.size();
		IJ.log(numberOfImagesProcessed + " of " + totalFileCount + " Files Successfully Processed.");
		if(nonImageFileLog.size() == 0) {
			IJ.log("All Files Processed Successfully.");
		}
		else {
			IJ.log("The Following Files failed to process:");
			for(int a=0; a<nonImageFileLog.size(); a++) {
				IJ.log(" - " + nonImageFileLog.get(a).getAbsolutePath());
			}
		}
	}




	/**
	 * A String array to represent the different types of image which can be saved in ImageJ.
	 */
	public final String[] typesDot = {".tif", ".jpg", ".gif", ".zip", ".raw", ".avi", ".bmp", 
			".fits", ".pgm", ".png", ".lut", ".roi", ".txt" };
	
	/**
	 * A String array to represent the different types of image which can be saved in ImageJ, omitting
	 * the preceding dot.
	 */
	public final String[] types = {"tif", "jpg", "gif", "zip", "raw", "avi", "bmp", 
	"fits", "pgm", "png", "lut", "roi", "txt" };
	
	/**
	 * This method sets the output image type.  The file path should end with ".tif", ".jpg", ".gif", 
	 * ".zip", ".raw", ".avi", ".bmp", ".fits", ".pgm", ".png", ".lut", ".roi" or ".txt". Note, the String
	 * can be presented without the "." character at the beginning. The specified image is saved in TIFF 
	 * format by IJ.save() if there is no extension set. If the extension does not match one of these 
	 * types, no change is made to the extension type. Note, this method is case in-sensitive, so the following
	 * will be accepted for TIFF file format:  ".tif", "tif", ".Tif", "TIF", ".TiF", "tIF", etc.
	 * @return A boolean to indicate if the process was successful.
	 */
	public boolean setOutputImageType(String type) {
		//First, check through all the types with no dot:
		for(int a=0; a<this.types.length; a++) {
			if(type.equalsIgnoreCase(this.types[a]) ) {
				//if the passed string equals one of the types, set output extension to "." plus the type String,
				//and return True.
				outputExtension = "." + type;
				return true;
			}
		}
		//If no match was found with the types with no dot, check the types with a preceding dot:
		for(int a=0; a<this.typesDot.length; a++) {
			if(type.equalsIgnoreCase(this.typesDot[a]) ) {
				//if the passed string equals one of the types, set output extension to the type string,
				//and return true:
				outputExtension = type;
				return true;
			}
		}
		//if this part is reached, the String does not match any of the types, so do not change outputExtension,
		//and return false:
		return false;
	}//end setOutputImageType()
	
	
	/**
	 * This method returns the outputExtension which has been set by setOutputImageType. If no image
	 * type has been set, this method will return null - however, images are saved in TIFF format by
	 * default.
	 * @return The String of the extension which has been set in this Dialog Window, or null if none set.
	 */
	public String getOutputImageType() {
		
		if(outputExtension == null) {
			return null;
		}
		else {
			return outputExtension.substring(1);
		}
		
	}
	
	
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
	 * This method returns a reference to the ArrayList<File> object which holds all files present in
	 * the input File Array. It allows access directly to all the file objects on a given FileSelector,
	 * should access to all files be required.
	 * @param fileSelectorIndex This int represents which fileSelector the input array should be retrieved 
	 * from.
	 * @return An ArrayList<File> object representing all the files which were selected on the given
	 * fileSelector object.
	 */
	public ArrayList<File> getInputFileArray(int fileSelectorIndex) {
		return fileSelectors.get(fileSelectorIndex).inputOutputFramework.fileArray;
	}

	
	/**
	 * This method must be defined in a subclass of DialogWindow. This method receives the inputs and
	 * output arrays of Files from the DialogWindow, and these are used in the plugin to process 
	 * images.
	 * @param inputs
	 * @param output
	 */
	//public abstract void processImages(ArrayList<ArrayList<File>>inputs, ArrayList<File> output);
	
	
	/**
	 * This method is called whenever the Process button is pressed on the
	 * ActionPanel. This method is blank, but can be overwritten in an anonymous
	 * inner class of this object. This method should be overwritten to call a subroutine
	 * in the class where this dialog is used, to implement the next steps after
	 * the dialog step has been complete.
	 */
	public void processImages(ArrayList<File> inputs, File output) { } //end processImages(inputs, output)
	
	
	/**
	 * This method is called from the processImage() method, which is called whenever the
	 * "process" button is pressed.  This method will receive each ArrayList<File> object
	 * present as input.  Each item in the ArrayList<File> array is a reference to a File
	 * object from a given FileSelector - for example, if two fileSelectors were used, the
	 * first item in the ArrayList<File> will be from the first FileSelector, and the second 
	 * item will be from the second FileSelector.  
	 * 
	 * This inputs arrayList can be difficult to generate, depending on what items are selected
	 * by the fileSelectors, if more than 1 fileSelector is used... 
	 * 
	 * SEE ABOVE.
	 * 
	 */
	//public ImagePlus processImages(ArrayList<File> inputs) { return null; }
	
	
	//public abstract ImagePlus processImages(ArrayList<File> inputs);
	
	
	
	/**
	 * Access the file path selected on the first FileSelector object.
	 * @return The file path selected in the first FileSelector, or null if the index is out of range.
	 */
	public File getFilePath() {
		return getFilePath(0);
	}
	
	/**
	 * Access the file path of an image selected on a given FileSelector object. If the selected
	 * image does not have a valid path, the method returns null.
	 * @param index An int of the index of the fileSelector whose path is required.
	 * @return file A File object of the path selected in the FileSelector, or null if the index is out of range.
	 */
	public File getFilePath(int index) {
		//if the index is out of bounds, return null.
		if(index >= selectorCount || index < 0) {
			return null;
		}
		//if the filePath to be returned matches one of the Strings in titles, return null
			//If the filePath to be returned matches one item in titles, it does not have a
			//filepath saved to the system, thus returning null allows this fact to be detected.
		for(int a=0; a<titles.length; a++) {
			if(titles[a] == fileSelectors.get(index).filePath) {
				return null;
			}
		}
		return new File(fileSelectors.get(index).filePath);

	}//end getFilePath()
	
	
	
	
	
	
	/**
	 * Access the selected ImagePlus on the first FileSelector object.
	 * @return The ImagePlus selected in the first FileSelector.
	 */
	public ImagePlus getImage() {
		return getImage(0);
	}
	
	/**
	 * Access the ImagePlus selected on a given FileSelector object.
	 * @param index The index of the fileSelector whose imp is required.
	 * @return The ImagePlus selected in the FileSelector, or null if the index is out of range.
	 */
	public ImagePlus getImage(int index) {
		if(index >= selectorCount || index < 0) {
			return null;
		}
		return fileSelectors.get(index).getSelectedImage();
	}
	
	
	/**
	 * Get the array of images from all FileSelectors.
	 * @return
	 */
	public ImagePlus[] getImageArray() {
		//create an array of ImagePlus of appropriate size to hold all the fileSelector imps:
		ImagePlus[] imps = new ImagePlus[fileSelectors.size()];
		
		//fill this array with selected imps in each fileSelector object:
		for(int a=0; a<fileSelectors.size(); a++) {
			imps[a] = fileSelectors.get(a).getSelectedImage();
		}
		
		//return the imps array:
		return imps;
	}
	
	
	/**
	 * Shutdown the DialogWindow - set all instance variables to null, and run garbage collection.
	 */
	public void shutDownDialogWindow() {

		WindowListener[] wls = this.getWindowListeners();
		for(int a=0; a<wls.length; a++) {
			this.removeWindowListener(wls[a]);
			wls[a] = null;
		}
		
		wls = null;
		
		sma = null;
		 
		for(int a=0; a<fileSelectors.size(); a++) {
			fileSelectors.get(a).shutdownFileSelector();
		}
		fileSelectors = null; //arraylist to hold multiple file selector objects

		actionPanel = null;
		
		 process = null;
		 cancel = null;
		


		 SM_SUFFIX = null;
		

		titles = null;
		
		inputs = null;
		
		output = null;
		
		outputExtension = null;

		
		nonImageFileLog = null;
		
		
		algorithmTitle = null;
		
		System.gc();
	}
	

}
