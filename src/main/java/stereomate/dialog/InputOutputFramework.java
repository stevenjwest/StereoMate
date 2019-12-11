package stereomate.dialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import stereomate.plugins.StereoMateSettings;

/**
 *  This class represents the StereoMate Input-Output Framework.  This framework validates 
 *  images, files and DIRs selected in the FileSelectors; deals with constrained input if
 *  selected on the FIRST FileSelector; retrieves the file paths and DIR tree for any DIR
 *  input; sets appropriate output based on the structure of the input; sets the output DIR
 *  title to a default title, which can be edited by hte user in the FileSelector.
 * <p>
 * 1. Validity of Input Files & Directories:
 * <p>
 *  The Framework defines the type of input a StereoMate algorithm can receive, and includes 
 *  important checks to ensure the right directory structure is present, which algorithms have
 *  been previously run on the dataset, and ensures certain assumptions concerning the structure
 *  and previous processing are met for the given StereoMate algorithm.
 *  <p>
 *  StereoMate algorithms can be run on the following:
 *  <p>
 *  - Individual Images: Whether selected from the file system or from active images within ImageJ.
 *  <p>
 *  - Simple Directories: Directories can have one of many structures.  The most basic is a Simple
 *  Directory which is a directory that contains an array of image files, which the StereoMate 
 *  algorithm will open and process in turn. In the event a non-image file is present, the algorithm
 *  will skip over that file.  A log message at the end will indicate which files were missed.
 *  <p>
 *  - Compound Directories:  These are nested directories, which can be nested to any degree. This data 
 *  structure should support the principle of Modularity: That each dataset collected should remain 
 *  separate in its collection, processing & analysis.  The best way to achieve this is by saving raw 
 *  data into a designated RAW DATA folder into appropriately labelled sub-folders indicating the 
 *  DataSet stored within it, and to allow subsequent analyses to be saved to separate folders stored 
 *  adjacent to the RAW DATA folder.
 *  <p>
 *  In other words, the Compound Folder Structure should provide support for Modular Analysis of 
 *  multiple datasets. This is achieved by storing all data for a given dataset under one folder.  
 *  Technical repeats of one observation should be stored together, and each observation stored 
 *  in its own folder, a subfolder of the dataset folder.
 *  <p>
 *  These different structures are checked in the validateFile(file) method.
 * <p>
 * 3. Providing the correct input folder:
 * <p>
 * StereoMate algorithms are stand-alone processes/plugins, yet work together in a flexible workflow.  For
 * automation as well as flexibility & user-friendly operation, StereoMate algorithms are organised
 * to receive different types of file/directory input, and previous processes are stored, to allow
 * future algorithms access to this information.
 * <p>
 * In the case of images, the user retains complete control over this process - the user is free
 * to select which image the algorithm should run on next; whether a raw image, or the output from
 * previous StereoMate algorithms.
 * <p>
 * In the case of Simple Directories, the user also retains complete control, opting to process
 * a folder of raw images, or one output from previous StereoMate algorithms.
 * <p>
 * SEQUENTIAL INPUTS:
 * <p>
 * However, in the case of Compound Directories, it is assumed that subsequent runs of StereoMate
 * plugins will be performed on the output from the previous plugin.  Thus, the info file will
 * provide the name of this next folder to be processed.
 * <p>
 * This feature allows image processing and analysis run on Compound Folders to run sequentially
 * in an automated fashion - ideal for users who have optimised their workflow, and are keen to
 * leave the computer to produce the results.
 * <p>
 * StereoMate Settings
 * <p>
 * StereoMate algorithm & File Input-Output settings will be stored in another XML file located in
 * the StereoMate jar package itself.  This will record the following: The appropriate default 
 * settings for different algorithms, default settings for SM algorithm input and output.
 * <p>
 * Information on this XML file can be retrieved if necessary - see getMinDepthSettings() method
 * for example..
 * 
 * @author stevenwest
 *
 */

public class InputOutputFramework {

	/*
	 * NAMED CONSTANTS:  To represent the type of file selected.
	 */
	
	/**
	 * Invalid image file. Either the file is not an image file, or the image parsed is not saved
	 * to the file system.
	 */
	public static final int INVALID_IMAGE = -1;
	
	/**
	 * Invalid DIR.
	 */
	public static final int INVALID_DIR = -2;
	
	/**
	 * Valid image file.
	 */
	public static final int VALID_IMAGE = 0;
	
	/**
	 * Valid simple DIR.
	 */
	public static final int VALID_SIMPLE_DIR = 1;
	
	/**
	 * Valid compound DIR to depth 2.
	 */
	public static final int VALID_COMP_DIR_2 = 2;
	
	/**
	 * Valid compound DIR to depth 3.
	 */
	public static final int VALID_COMP_DIR_3 = 3;
	
	/**
	 * Valid compound DIR to depth 4.
	 */
	public static final int VALID_COMP_DIR_4 = 4;
	
	/**
	 * Valid compound DIR to depth 5.
	 */
	public static final int VALID_COMP_DIR_5 = 5;
	
	/**
	 * Valid compound DIR to depth 6.
	 */
	public static final int VALID_COMP_DIR_6 = 6;
	
	/**
	 * Valid compound DIR to depth 7.
	 */
	public static final int VALID_COMP_DIR_7 = 7;
	
	/**
	 * Valid compound DIR to depth 8.
	 */
	public static final int VALID_COMP_DIR_8 = 8;
	
	/**
	 * Reference to the File Object being parsed.
	 */
	File file;
	
	/**
	 * Instance variable indicating the validity of the currently selected file. This object
	 * contains instance variables for minimumDepth, maximumDepth and validity, as well as
	 * an ArrayList of the files present at the minimumDepth, or an ImagePlus object if the
	 * file parsed is an open image in ImageJ.
	 */
	FileValidation validity;
	
	/**
	 * Maximum Depth of the selected Folder. i.e. the maximum path length to the bottom of
	 * the DIR tree.
	 */
	int maxDepth;
	
	/**
	 * Minimum Depth of the selected Folder. i.e. the minimum path length to the bottom of
	 * the DIR tree.
	 */
	int minDepth;
	
	/**
	 * Value of the validity of the parsed file.  This is set to the minimum depth by default.
	 */
	int validityValue;
	
	/**
	 * A boolean to represent whether the MinDepth of the DirTree is used to collect files for
	 * processing, or whether the maxDepth of the DirTree is used.  This is retrieved during the
	 * construction of a SteroMateInputOutputFramework object from the
	 * StereoMateSettings, and can be adjusted by the User there.
	 */
	boolean useMinDepth;
	
	/**
	 * If the selected (constrained) File is a File (and not DIR), then the method determineOutputName will
	 * remove the fileExtension string from the file for calculating the output Title.  This variable will
	 * store the fileExtension, so it can be retrieved at a later time.
	 */
	String fileExtension;
	
	
	/**
	 * A file array to hold references to the files found in the validateCompoundDir() method at
	 * the minValueReached -> this ensures the correct fileArray is returned after the validateCompoundDir()
	 * method has completed its recursion.
	 */
	public ArrayList<File> fileArray;
	
	/**
	 * A file array to hold references to the files which represent all directory paths present
	 * below a compound DIR.  This is used to fill the constrainedComboBox with appropriate strings,
	 * as well as create the appropriate DIR Tree in the output DIR.
	 */
	ArrayList<File> dirTreeArray;
	
	/**
	 * A String array to hold Strings of the DIR tree below the main DIR being parsed. This is used
	 * to fill the constrainComboBox with appropriate options for selection, as well as create the 
	 * appropriate DIR Tree in the output DIR.
	 */
	ArrayList<String> dirTreeStrings;
	
	
	/**
	 * Reference to the input file for analysis. Either equal to the File Object parsed,
	 * or a sub-directory of the File Object parsed which the analysis has been constrained
	 * to.
	 */
	File constrainedFile;
	
	/**
	 * Boolean to represent whether the StereoMateInputOutputFramework is using the constrained
	 * File for Analysis input, or not.
	 */
	boolean constrainedAnalysis;
	
	/**
	 * Instance variable indicating the validity of the currently selected constrained file. This 
	 * object contains instance variables for minimumDepth, maximumDepth and validity, as well as
	 * an ArrayList of the files present at the minimumDepth, or an ImagePlus object if the file 
	 * parsed is an open image in ImageJ.
	 */
	FileValidation constrainedValidity;
	
	/**
	 * Maximum Depth of the selected Folder. i.e. the maximum path length to the bottom of
	 * the DIR tree.
	 */
	int constrainedMaxDepth;
	
	/**
	 * Minimum Depth of the selected Folder. i.e. the minimum path length to the bottom of
	 * the DIR tree.
	 */
	int constrainedMinDepth;
	
	/**
	 * Value of the validity of the parsed file.  This is set to the minimum depth by default.
	 */
	int constrainedValidityValue;
	
	/**
	 * A file array to hold references to the files which represent all directory paths present
	 * below a Constrained compound DIR.  This is used to create the appropriate DIR Tree in 
	 * the output DIR.
	 */
	ArrayList<File> dirTreeArrayConstrained;
	
	/**
	 * A String array to hold Strings of the DIR tree below the constrained DIR being parsed. This 
	 * is used to create the appropriate DIR Tree in the output DIR.
	 */
	ArrayList<String> dirTreeStringsConstrained;
	
	
	/**
	 * Reference to the output file DIR of analysis.  This is the parent of the input file.
	 * And provides the Absolute Path from root to the point where the output File/DIR
	 * should be saved.
	 */
	File outputDir;
	
	/**
	 * Reference to the output file Name. This is the actual name of the output DIR or file.
	 */
	String outputName;
	
	/**
	 * A file array to hold a list of File references where output files should be saved.
	 * This array should be a composite of the ouputDir (parent of input file), an appropriate
	 * name for the output file or DIR (based on input or constrained file/DIR name), and
	 * each of the paths leading to the files to be processed through the DIR Tree that may
	 * or may not be present.  The DIR Tree existing below outputName (assuming outputName is
	 * a DIR) must be created, which is done so using the outputDirTreeArray variable.
	 */
	ArrayList<File> outputFileArray;
	
	/**
	 * This Array List holds a reference to every absolute path constituting every DIR which 
	 * must be present in the output DIR, for saving each File stated in the outputFileArray.
	 */
	ArrayList<File> outputDirTreeArray;

	/**
	 * Reference to the number which should be used to label the output DIR for simple & compound
	 * DIRs, and constrained analyses to DIRs.
	 */
	String outputNumberString;
	
	

	
	

	/**
	 * The standard constructor for the StereoMateInputOutputFramework. This simply
	 * creates the StereoMateInputOutputFramework object, and reads the stereoMateSettings.
	 * In order to parse files, the method 'parseNewFile()' should be used.
	 */
	public InputOutputFramework() {

		
		//Also, retrieve the useMinDepth boolean:
		useMinDepth = getMinDepthSettings();
	}
	
	
	/**
	 * Constructor. First, retrieves the StereoMateSettings.
	 * Validates the file and stores the NAMED CONSTANT to the variable 'validity'.
	 * If the file is valid, an attempt to read the StereoMate.info file associated with the
	 * input is made, and the result stored in the 'StereoMateInfo' object.
	 * @param file The file or directory to be processed.
	 */
	public InputOutputFramework(File file) {
		
		//Also, retrieve the useMinDepth boolean:
		useMinDepth = getMinDepthSettings();
		
		//... and set the file to the instance variable, file.
		this.file = file;
				
		//Set the constrainedFile object to the parsed file
			//[this constrained file can be altered by setconstrainedFile() if
			//the analysis is constrained in the DialogWindow class]
		this.constrainedFile = file;
				
		//initiate the fileArray object:
		fileArray = new ArrayList<File>();
		
		//Finally, in lieu of what has already been found, validate input file:
			//Attempting to read StereoMateInfo first means this information can
			//be used to validate the file:
			//i.e. if StereoMateInfo was found, the file can assumed to be valid,
			//and information in it retrieved to fill instance variables in this class.
				//This assumption can be checked by checking the file path exists!
		validity = validateFile(file);
		
		//set the minDepth, maxDepth and validityValue to values found in validity:
		minDepth = validity.minDepth;
		maxDepth = validity.maxDepth;
		validityValue = validity.validity;
		
		//Now, pass the file object to the setoutputDir() method, to set
				//the outputDir instance variable:
		setOutputDir(file);

		
	} //end constructor
	
	
	
	public void shutdownStereoMateInputOutputFramework() {
		
		file = null;
		
		validity = null;
		
		fileExtension = null;
		
		
		if(fileArray != null) {
		for(int a=0; a< fileArray.size(); a++) {
			fileArray.set(a, null);
		}
		fileArray = null;
		}
		
		if(dirTreeArray != null) {
		for(int a=0; a< dirTreeArray.size(); a++) {
			dirTreeArray.set(a, null);
		}
		dirTreeArray = null;
		}
		
		if(dirTreeStrings != null) {
		for(int a=0; a< dirTreeStrings.size(); a++) {
			dirTreeStrings.set(a, null);
		}
		dirTreeStrings = null;
		}
		
		constrainedFile = null;
		
		constrainedValidity = null;
		
		if(dirTreeArrayConstrained != null) {
		for(int a=0; a< dirTreeArrayConstrained.size(); a++) {
			dirTreeArrayConstrained.set(a, null);
		}
		dirTreeArrayConstrained = null;
		}
		
		if(dirTreeStringsConstrained != null) {
		for(int a=0; a< dirTreeStringsConstrained.size(); a++) {
			dirTreeStringsConstrained.set(a, null);
		}
		dirTreeStringsConstrained = null;
		}
		
		outputDir = null;
		
		outputName = null;
		
		if(outputFileArray != null) {
		for(int a=0; a< outputFileArray.size(); a++) {
			outputFileArray.set(a, null);
		}
		outputFileArray = null;
		}
		
		if(outputDirTreeArray != null) {
		for(int a=0; a< outputDirTreeArray.size(); a++) {
			outputDirTreeArray.set(a, null);
		}
		outputDirTreeArray = null;
		}
		
		outputNumberString = null;
		
	}
	
	
	
	/**
	 * This method retrieves the minDepth boolean value from the StereoMateSettings XML file, found in the StereoMate
	 * ".settings" hidden folder.
	 * @return
	 */
	protected boolean getMinDepthSettings() {
		
		//First, define the String which will receive the useMinDepth information:
		String useMinDepthStr = null;
		
		//Retrieve file representation of JAR file - to retrieve its absolute path:
		File file = null;	
		try {
			file = new File(StereoMateSettings.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//Retrieve parent, and then formulate new file object pointing to the StereoMateSettings.xml file:
		file = file.getParentFile();
		File stereoMateSettings = new File(file.getAbsolutePath() + File.separator + ".settings" + File.separator + "StereoMateSettings.xml");
		
		
		//Here, the InputStream is used inside appropriate try... catch statements:
		InputStream in = null;
		
		try {
			in = new FileInputStream(stereoMateSettings);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//Once an InputStream is established, next build the DOM Document:
		
		//generate Document Builder:
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = null;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
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
			// TODO Auto-generated catch block
			e.printStackTrace();
			//close InputStream
			try {
				in.close();
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		//Now the DOM Document is built, retrieve the desired contents from it:
		
		try {
			doc.getDocumentElement().normalize();
			//IJ.showMessage("Root Element: "+ doc.getDocumentElement().getNodeName() );
		
			NodeList nList2;
			
			 nList2 = doc.getElementsByTagName("useMinDepth");
			 useMinDepthStr = ((Element)nList2.item(0)).getAttribute("str");
			
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
		
		//Finally, the useMinDepth boolean can be set depending on the content of the useMinDepthStr String:
		if(useMinDepthStr.equals("true") ) {
			//IJ.showMessage("minDepth is true");
			return true;
		}
		else {
			//IJ.showMessage("minDepth is false");
			return false;
		}
		
	}//end getMinDepthSettings()

	
	/**
	 * Method to parse a new file object with the StereoMateInputOutputFramework.
	 * @param file A new file object to be parsed.
	 */
	public void parseNewFile(File file) {
		
		//First, set the file to the instance variable, file.
		this.file = file;
		
		//Set the constrainedFile object to the parsed file
		//[this constrained file can be altered by setconstrainedFile() if
		//the analysis is constrained in the DialogWindow class]
		this.constrainedFile = file;
		
		//initiate the fileArray object:
		fileArray = new ArrayList<File>();
		
		//Finally, in lieu of what has already been found, validate input file:
		//Attempting to read StereoMateInfo first means this information can
		//be used to validate the file:
		//i.e. if StereoMateInfo was found, the file can assumed to be valid,
		//and information in it retrieved to fill instance variables in this class.
			//This assumption can be checked by checking the file path exists!
		validity = validateFile(file);
		
		//set the minDepth, maxDepth and validityValue to values found in validity:
		minDepth = validity.minDepth;
		maxDepth = validity.maxDepth;
		validityValue = validity.validity;
		
		//Now, pass the file object to the setoutputDir() method, to set
			//the outputDir instance variable:
		setOutputDir(file);
		
		//this is not required now, as the creation of the stereoMateInfo object
			//deals with creating the StereoMateInfo Document.
		//if(validity != INVALID_IMAGE || validity != INVALID_DIR) {
			//stereoMateInfo.stereoMateInfoDoc = readStereoMateInfo(file);
		//}	
		
	} //end parseNewFile()
	
	/**
	 * A method to parse an imp through the StereoMate Input Output Framework. This would
	 * be performed with an active image open in ImageJ.
	 * @param imp The ImagePlus object to be parsed.
	 */
	public void parseNewImp(ImagePlus imp) {
		File imageFile = null; //local variable to represent the file instance of the imp.
		String fileName = imp.getTitle(); //fileName retrieved from imp
		
		//Attempt to build a file object from the ImagePlus object:
			//Do this by trying to read the FileInfo from the imp:
		FileInfo fi = imp.getOriginalFileInfo();
		
		if(fi != null) { //if file info is not null, the image is from the file system.
			String dir = fi.directory;
			fileName = fi.fileName;
			IJ.showMessage("dir: "+dir);
			IJ.showMessage("file name: "+fileName);
			imageFile = new File(dir+fileName);
		}
		
		//if imageFile is not null - i.e. it has been instantiated with the file system
			//representation of the parsed imp:
		if(imageFile != null) {
			//Set file and constrainedFile to the instance variables in this class:
			this.file = imageFile;
			this.constrainedFile = imageFile;
		}
		
		//initiate the fileArray object:
		fileArray = new ArrayList<File>();
		
		//Finally, determine validity of image & imageFile:
		validity = validateFile(imp, imageFile);
		
		//Now, pass the file object to the setoutputDir() method, to set
			//the outputDir instance variable:
		setOutputDir(imageFile, fileName);
		
	} //end parseNewImp(imp)
	
	
	/**
	 * Calls the validateFile(file) method with the instance variable, file.
	 * (Set in constructor).
	 * @return A FileValidation object representing the validation status of the presented file.
	 */
	public FileValidation validateFile() {
		return validateFile(file);
	}


	/**
	 * This method validates the file or directory passed to it, in terms of
	 * the StereoMate Input-Output Framework.  Note, this algorithm does not validate whether
	 * the passed file is a valid image file, but only validates the structure of the file
	 * to match either a simple file, simple DIR containing only files, or a compound DIR. If
	 * a compound DIR is returned, the fileArray returned in the FileValidation object
	 * depends on whether MinDepth or MaxDepth is selected in the StereoMateSettings - if
	 * MinDepth is selected, files present at the minimum depth of the DIR tree are provided,
	 * (this means the first DIR level where files are present in EACH DIR, independent of whether
	 * there are further DIRs to search through), whereas if MaxDepth is selected, files present 
	 * at the maximum depth of the DIR tree are provided.
	 * @param file A File object representing the directory or file to be assessed for validity.
	 * @return FileValidation An object representing the validation status of the presented file.
	 */
	public FileValidation validateFile(File file) {
		
		//First, if StereoMateInfo is not null, read information from this Document
			//to fill instance variables in this class!
			//This is more efficient that trying to parse the file each time to read its structure.
			//This process is performed in readStereoMateInfo(), and values attributed to instance 
			//variables, thus just:
		//Check the path supplied by the StereoMateInfo file is true by comparing the path 
		//provided by StereoMateInfo exists
		//if( new File( this.file.getAbsolutePath() [Plus Path-To-SampleFolder]).exists() ) {
			//return [validity-indicated-in-StereoMateInfo];
		//}
		
		//No need to do the above, as its pretty efficient reading the file structure below anyway:
		

		//First, set dirTreeArray to null (which will stay null if the file is a File,
		//but will be set to an appropriate File ArrayList if it is a DIR):
		dirTreeArray = null;
		
		
		//If the file is a File (and not a DIR), then deal with this File:
		if(file.isFile() ) {
					
			//if the File does not exist on the File System, return INVALID_IMAGE FileValidation:
			if(file.exists() == false) {
				//the parsed file does not exist on the file System,
					//therefore set the validity to INVALID_IMAGE.
				//First, set fileArray instance variable to null:
				this.fileArray = null;
				//and return INVALID FileValidation object:
				return new FileValidation(INVALID_IMAGE, INVALID_IMAGE, INVALID_IMAGE, null, null);
			}
			//else, return a VALID_IMAGE FileValidation, along with a fileList:
			else {
				//The file must be a file located somewhere on the file system,
				 //therefore return validity of VALID_IMAGE:
				ArrayList<File> fileList = new ArrayList<File>();
				fileList.add(file);
				this.fileArray = fileList; //save this fileList to the instance variable fileArray.
				//and return a FileValidation object indicating a VALID_IMAGE, with the fileList
					//object:
				return new FileValidation(VALID_IMAGE, VALID_IMAGE, VALID_IMAGE, fileList, null);
			}
		
		}
		
		//if StereoMateInfo is null, need to attempt to parse the file to understand
			//its structure, and test its validity.
		
		//if file is a directory, check for file structure:
		if(file.isDirectory() ) {
			
			//Initially, initialise/refresh the dirTreeArray present in the StereoMateInputOutputFramework
			//instance variables, to ensure it is an empty array for filling with all DIRs in the compound
			//DIR, or the DIR for the simple DIR:
			dirTreeArray = new ArrayList<File>();
			
			
			//Then, collect fileNames and files arrays;
			File[] files = file.listFiles();
			
			//... And filter out any "hidden" files in this Array - by removing any files beginning with
				//a dot:
			ArrayList<File> filesTemp = new ArrayList<File>();
			for(int a=0; a<files.length; a++) {
				if(!files[a].getName().startsWith(".")) {
					filesTemp.add(files[a]);
				}
			}
			files = new File[filesTemp.size()];
			for(int a=0; a<filesTemp.size(); a++) {
				files[a] = filesTemp.get(a);
			}
			filesTemp = null;
			//IJ.showMessage("files array size: "+files.length);

			//Next, check if this DIR is a valid simple DIR:
				//This is the case if no DIRs are found within it:
			boolean simpleDir = true; //boolean to store information on whether DIRs exist in file.
			ArrayList<File> dirArray = new ArrayList<File>(); //ArrayList to store DIRs found in DIR.
			ArrayList<File> fileArray = new ArrayList<File>(); //ArrayList to hold files found in DIR.
			//loop through each item in DIR:
			for(int a=0; a<files.length; a++) {
				if(files[a].isDirectory() ) {
					//if file being checked is DIR, set simpleDIR to false, and save the DIR to dirArray:
					simpleDir = false;
					dirArray.add(files[a]);
				}
				else {
					//the file is not a directory,
					//so store in fileArray:
					fileArray.add(files[a]);
				}
			}//end for a
			
			//if simpleDir is true, set dirTreeArray to file via dirArray, set the instance variable 
			// fileArray to the fileArray local variable, and return FileValidation as SIMPLE_DIR.
			if(simpleDir == true) {
				//dirArray is blank if simpleDir is true.
				//First, add file to dirArray..
				dirArray.add(file);
				//then set this ArrayList<File> to the dirTreeArray
					//i.e. the dirTreeArray now contains one File, which holds the path to the Simple DIR..
				Collections.sort(dirArray);
				dirTreeArray = dirArray;
				
				//Next, save fileArray to the instance variable fileArray:
				Collections.sort(fileArray);
				this.fileArray = fileArray;
				
				//Then return SIMPLE_DIR FileValidation Object, with fileArray:
				return new FileValidation(VALID_SIMPLE_DIR, VALID_SIMPLE_DIR, VALID_SIMPLE_DIR, fileArray, null);
			}
			
			//If this point is reached, the files array contains directories.
				//Each DIR must be checked to test its validity as a Compound DIR.
				//this now becomes recursive - as each DIR must be searched until the base case is found:

			//First, initialise/refresh the dirTreeArray present in the StereoMateInputOutputFramework
			//instance variables, to ensure it is an empty array for filling with all DIRs in the compound
			//DIR:
			dirTreeArray = new ArrayList<File>();
			
			//Then call the recursive method, validateCompoundDir, with the dirArray [list of all DIRS in
				//the passed file], current min and max values [2,2], and false to indicate the min value
				//has not been reached yet..
			
			return validateCompoundDir(dirArray, VALID_COMP_DIR_2, VALID_COMP_DIR_2, false);
			
		}
		
		//This code should be unreachable - but just in case, return an invalid image with no ArrayList or
		 //imagePlus..
		return new FileValidation(INVALID_IMAGE, INVALID_IMAGE, INVALID_IMAGE, null, null);

		
	} //end validateFile()
	
	
	/**
	 * Validate ImagePlus and File objects. This method is only called in the parseNewImp(imp)
	 * method in FileSelector. In that method, an attempt to create a File representation from
	 * the imp is performed through FileInfo (see method for more details).  If no FilePath 
	 * reference is found, the File object will be null.
	 * <p>
	 * This method aims to validate the imp based on the File object.  If the file object is not null, the
	 * imp has some representation on the File System. Thus, the File is added to an ArrayList,
	 * saved to the fileArray variable, and a FileValidation object returned as VALID_IMAGE.
	 * <p>
	 * If the file is null, the imp has no File System representation. The fileArray variable
	 * is set to null, and a FileValidation object returned as INVALID_IMAGE.
	 * <p>
	 * @param imp The ImagePlus object retrieved from the ImageJ main window.
	 * @param file The File object retrieved from this ImagePlus, or null if no representation
	 * exists on the File System.
	 * @return FileValidation An object representing the validation status of the presented file.
	 */
	public FileValidation validateFile(ImagePlus imp, File file) {
		
		//First, check if the file object is null:
		if(file != null) {
			//if not null, return a VALID IMAGE with the fileList and imp included in the FileValidation
				//object:
			ArrayList<File> fileList = new ArrayList<File>();
			fileList.add(file);
			this.fileArray = fileList;
			return new FileValidation(VALID_IMAGE, VALID_IMAGE, VALID_IMAGE, fileList, imp);
		}
		else {
			//if file is null, set fileArray to null, and return an INVALID IMAGE with no fileList and 
			//include the imp in the FileValidation object:
			this.fileArray = null;
			return new FileValidation(INVALID_IMAGE, INVALID_IMAGE, INVALID_IMAGE, null, imp);
		}
		
	}
	
	
	
	/**
	 * This method searches through the passed ArrayList<File> object, which must contain some
	 * DIRs, to verify the DIR structure.  This is initially achieved by searching through each
	 * DIR present in the ArrayList, and repeating this iteratively until both the minValue (the
	 * minimum DIR tree depth) and the maxValue (the maximum DIR tree depth) have been determined.
	 * @param dirArray
	 * @return A FileValidation object which represents this Compound DIR.
	 */
	public FileValidation validateCompoundDir(ArrayList<File> dirArray, int minVal, 
											int maxVal, boolean minValReached) {
		
		//initially the instance variables should be set - and will be incremented as
		//further levels of the DIR tree are discovered [through recursive calls to this method]	
		//Values to track the minVal [minimum path length through the DIR tree], a boolean
		//value to track when the minVal has been reached, and the maxVal [maximum path length]
		//should be created:
		int minValue = minVal;
		boolean minValueReached = minValReached;
		int maxValue = maxVal;
		
		//a boolean should be used to detect when a single directory is found, anywhere in 
		//dirArray, in which case,
		//the maxValue will need to be incremented after the for loop.
			//Initially, no DIRs have been detected - so set to false:
		boolean dirDetected = false;
		
		//The minDepthIteration boolean is set to true only in the iteration where the minDepth
			//is found. This is used to save the local fileArray to the instance variable fileArray
			//at the correct minDepth, to ensure the files located at minDepth in the DIR tree are
			//saved to the FileValidation object:
		boolean minDepthIteration = false;
		
		//A second dirArray should be generated for storing new sub-directories that exist
		//in any of the directories in the original dirArray:
		ArrayList<File> dirArray2 = new ArrayList<File>();
		
		//A second ArrayList<File> should be made to store the path to each file detected:
		ArrayList<File> fileArray = new ArrayList<File>();
		
		//Next a loop should go through the contents of each directory in the dirArray
		//to test if its contents contains directories:
		for(int a=0; a<dirArray.size(); a++) {
			
			//First, add each DIR in dirArray to the dirTreeArray variable:
			//and finally, add this DIR to the dirTreeArray:
			dirTreeArray.add( dirArray.get(a) );
			
			//generate a local boolean to detect DIRs in the current file being parsed:
				//initially set to false, as no DIR detected in current file being parsed yet:
			boolean localDirDetected = false;
			
			//first, capture the files that exist below the directory being parsed:
			File[] files = dirArray.get(a).listFiles();
			
			//next, loop through these files, and check if they are a directory or not:
			for(int b=0; b<files.length; b++) {
				if(files[b].isDirectory() ) {
					//A DIR has been detected, set dirDetected to true:
					dirDetected = true;
					//set localDirDetected to true, 
					//as DIR detected in currently parsed dirArray file:
					localDirDetected = true;
					//and add this DIR to the new dirArray - dirArray2:
					dirArray2.add(files[b]);
				}
				else {
					//the file is a file, so store it in the fileArray:
					
					if(files[b].getName().substring(0,1).equals(".") == false) {
						//only add to fileArray if the title of the files[b] file does not
						//begin with a "." - i.e. it is not a hidden file.
							//This excludes the .DS_Store file on mac which can sometimes be
							//saved to fileArray...
						fileArray.add(files[b]);
					}
				}
			} //end for b
			
			if(localDirDetected == true) {
				//if localDirDetected is true, a DIR has been found in the parsed file.
				//This means this parsed file is not the end of the DIR tree.
				//Therefore, the minimum value must be continued to be calculated.
				//minValueReached should be kept as it is.
			}
			else {
				//else, the localDirDetected boolean is false, which means no DIR was
				//detected in the parsed file, and this file represents the minimum value.
				//Thus, minValueReached should be set to true:
				if(minValueReached == false) {
					//only change is minValueReached is false:
					minValueReached = true;
					//and change the local variable minDepthIteration to true
						//This marks the exact iteration where minDepth was reached, and is used
						//to save the local fileArray at minDepth correctly to the instance variable
						//fileArray:
					minDepthIteration = true;
				}
			}
			
		} //end for a
		
		
		//after parsing all the files in dirArray, if minValueReached is false, then each
		//parsed file in dirArray contained at least one DIR.
		//Therefore, the minValue should be incremented by one:
			//[The minValue will be searched further in the next recursion of this algorithm,
			//see below]
		if(minValueReached == false) {
			minValue = minValue + 1;
		}
		else if(minValueReached == true) {
			//if minValueReached is true, then no DIRs were detected in at least one parsed DIR.
			//in this case, the local fileArray should be stored to the instance variable, to allow
			//the files at this minDepth level to be stored in FileValidation:
			if(minDepthIteration == true) {
				//only save local fileArray to instance variable fileArray if the minDepthIteration
				//variable is true - as this marks the specific iteration where minDepth was determined.
				Collections.sort(fileArray);
				this.fileArray = fileArray;
			}
		}
		
		//if dirDetected is true, at least one file in dirArray contained a DIR, and thus the
		//maxValue should be incremented by 1, and this method should be re-run with the
		//appropriate input - which is dirArray2[list of all DIRs detected], the new incremented
		//maxValue, the minValue [incremented if minValueReached is false], and the value of
		//minValueReached [true or false depending on if a DIR was detected here or previously
		//with no dub-DIRs]
		if(dirDetected == true) {
			maxValue = maxValue + 1;
			return validateCompoundDir(dirArray2, minValue, maxValue, minValueReached);
		}
		
		else {
			//else, dirDetected is equal to false, which means no new DIRs have been detected
			//in any of the files parsed in dirArray.  Thus means the maxValue equals the maximum
			//number of DIRs that can be traversed in the DIR Tree.
			//Thus, the method should return with the appropriate FileValidation object:
			
			// sort the dirTreeArray:
			Collections.sort(dirTreeArray);
			
			//First, generate an array of Strings for the constrainComboBox from the dirTreeArray
				//of Files:
			dirTreeStrings = new ArrayList<String>();
			dirTreeArrayToStringArray(dirTreeArray, file);
			
			//Next, since the local fileArray object represents all files present at the Maximum Depth
			//through the DirTree - if the useMinDepth is false [i.e. the maxDepth file array should be used]
			//then the instance variable fileArray should be set to the current [maxDepth] fileArray:
			if(useMinDepth == false) {
				Collections.sort(fileArray);
				this.fileArray = fileArray; //set the local fileArray at maxDepth, to the instance variable
											//fileArray to return the maxDepth set of files ONLY IF
											//useMinDepth is FALSE.
			}
			
			return new FileValidation(minValue, maxValue, minValue, this.fileArray, null);
			
		}
		
	} //end validateCompoundDir()
	
	
	/**
	 * This method allows the constrainedFile object to be altered to a new File object.
	 * The original parsed file is kept the same, and thus the new constrainedFile 
	 * should be a sub-file or directory to the original file object.
	 * This File should be an absolute path to the constrained file for Analysis,
	 * and this method is called whenever the original directory selected is to
	 * have its analysis constrained to a specific sub-directory.
	 * Note: The 'constrainedFile' variable is the source of the data, but the output is 
	 * defined by the original 'file' variable.
	 * @param constrainedFile The File Object to replace the constrainedFile variable in 
	 * this class.
	 */
	public void setConstrainedFile(File constrainedFile) {
	
		//set constrainedFile:
		this.constrainedFile = constrainedFile;
		//initiate the fileArray object:
		fileArray = new ArrayList<File>();
		
		//Determine validity of the constrainedFile:
		//constrainedValidity = validateFile(constrainedFile); -> this will alter the dirTreeArray! Instead,
			//a call should be made to a separate method to validate a constrained file:
		constrainedValidity = validateConstrainedFile(constrainedFile);
		
		//set the minDepth, maxDepth and validityValue to values found in validity:
		constrainedMinDepth = constrainedValidity.minDepth;
		constrainedMaxDepth = constrainedValidity.maxDepth;
		constrainedValidityValue = constrainedValidity.validity;
		
		//Finally, call the setOutputNameConstrained() method, which simply takes the
		//constrained DIR name (i.e excluding the rest of the Path) and adds the appropriate
		//number on the front (which was already retrieved in setOutputName() ), and sets this
		//to the variable outputName:
		setOutputNameConstrained();
	
		
		//note, 'constrainedFile' is now the source of input, whereas
		//the original 'file' object indicates the path to output.
		
	}


	/**
	 * Sets whether constrainedAnalysis is used.
	 * @param set Boolean to set whether constrainedAnalsis is used or not.
	 */
	public void setConstrainedAnalysis(boolean set) {
		constrainedAnalysis = set;
	}


	/**
	 * This method validates the constrained file or directory passed to it, in terms of
	 * the StereoMate Input-Output Framework.  Note, this algorithm does not validate whether
	 * the passed file is a valid image file, but only validates the structure of the file
	 * to match either a simple file, simple DIR containing only files, or a specific directory
	 * structure ending with a series of Folders named "SAMPLE", or the valid sample folder
	 * naming convention (set in StereoMate Settings).
	 * @param A File object representing the directory or file to be assessed for validity.
	 * @return An int representing the validation status of the presented file.
	 */
	public FileValidation validateConstrainedFile(File file) {
		
		//First, if StereoMateInfo is not null, read information from this Document
			//to fill instance variables in this class!
			//This is more efficient that trying to parse the file each time to read its structure.
			//This process is performed in readStereoMateInfo(), and values attributed to instance 
			//variables, thus just:
		//Check the path supplied by the StereoMateInfo file is true by comparing the path 
		//provided by StereoMateInfo exists
		//if( new File( this.file.getAbsolutePath() [Plus Path-To-SampleFolder]).exists() ) {
			//return [validity-indicated-in-StereoMateInfo];
		//}
		
		//First, set dirTreeArray to null (which will stay null if the file is a File,
				//but will be set to an appropriate File ArrayList if it is a DIR):
		dirTreeArrayConstrained = null;
				
		if(file.isFile() ) {
					
			if(file.exists() == false) {
				//the parsed file does not exist on the file System,
					//therefore set the validity to INVALID_IMAGE.
				this.fileArray = null; //first, set fileArray to null.
				return new FileValidation(INVALID_IMAGE, INVALID_IMAGE, INVALID_IMAGE, null, null);
			}
			else {
				//The file must be a file located somewhere on the file system,
				 //therefore return validity of VALID_IMAGE:
				ArrayList<File> fileList = new ArrayList<File>();
				fileList.add(file); //First, store the file in a ArrayList.
				this.fileArray = fileList; //Save this arrayList to fileArray - for access to
											//input path, and to produce appropriate output path.
				return new FileValidation(VALID_IMAGE, VALID_IMAGE, VALID_IMAGE, fileList, null);
			}
		
		}
		
		//if StereoMateInfo is null, need to attempt to parse the file to understand
			//its structure, and test its validity.
		
		//if file is a directory, check for file structure:
		if(file.isDirectory() ) {
			
			//Initially, initialise/refresh the dirTreeArray present in the StereoMateInputOutputFramework
			//instance variables, to ensure it is an empty array for filling with all DIRs in the compound
			//DIR:
			dirTreeArrayConstrained = new ArrayList<File>();
			
			//First, collect fileNames and files arrays;
			File[] files = file.listFiles();
			
			//... And filter out any "hidden" files in this Array - by removing any files beginning with
			//a dot:
			ArrayList<File> filesTemp = new ArrayList<File>();
			for(int a=0; a<files.length; a++) {
				if(!files[a].getName().startsWith(".")) {
					filesTemp.add(files[a]);
				}
			}
			files = new File[filesTemp.size()];
			for(int a=0; a<filesTemp.size(); a++) {
				files[a] = filesTemp.get(a);
			}
			filesTemp = null;
	
			//Then, check if this DIR is a valid simple DIR:
			//This is the case if no DIRs are found within it:
			boolean simpleDir = true; //boolean to store information on DIRs in file.
			ArrayList<File> dirArray = new ArrayList<File>(); //ArrayList to store DIRs found in DIR.
			ArrayList<File> fileArray = new ArrayList<File>(); //ArrayList to hold files found in DIR.
			//loop through each item in DIR:
			for(int a=0; a<files.length; a++) {
				if(files[a].isDirectory() ) {
					//if file being checked is DIR, set simpleDIR to false, and save the DIR to dirArray:
					simpleDir = false;
					dirArray.add(files[a]);
				}
				else {
					//the file is not a directory,
					//so store in fileArray:
					fileArray.add(files[a]);
				}
			}
			if(simpleDir == true) {
				//if simpleDir is true, then return this as a SIMPLE_DIR:
				//First, put file into dirArray, and set to dirTreeArrayConstrained
					//To ensure dirTreeArrayConstrained is filled with the DIR Tree
					//[which is just the SIMPLE DIR being parsed]
				dirArray.add(file);
				dirTreeArrayConstrained = dirArray;
				this.fileArray = fileArray;//First, save fileArray to the instance variable fileArray.
				//Then return SIMPLE_DIR FileValidation Object:
				return new FileValidation(VALID_SIMPLE_DIR, VALID_SIMPLE_DIR, VALID_SIMPLE_DIR, fileArray, null);
			}
			
			//If this point is reached, the files array contains directories.
				//Each DIR must be checked to test its validity as a Compound DIR.
				//this now becomes recursive - as each DIR must be searched until the base case is found:
				//A DIR titled with Sample Folder naming convention, which contains only (Image) Files.
				//Thus, call a method to deal with this recursively, which returns the correct value:
	
			
			return validateConstrainedCompoundDir(dirArray, VALID_COMP_DIR_2, VALID_COMP_DIR_2, false);
			
		}
		
		return new FileValidation(INVALID_IMAGE, INVALID_IMAGE, INVALID_IMAGE, null, null);
	
		
	} //end validateConstrainedFile()

	

	/**
	 * This method searches through the passed ArrayList<File> object, which must contain some
	 * DIRs, to verify the DIR structure.  This is initially achieved by searching through each
	 * DIR present in the ArrayList, and repeating this iteratively until both the minValue (the
	 * minimum DIR tree depth) and the maxValue (the maximum DIR tree depth) have been determined.
	 * @param dirArray
	 * @return
	 */
	public FileValidation validateConstrainedCompoundDir(ArrayList<File> dirArray, int minVal, 
											int maxVal, boolean minValReached) {
		
		//initially the instance variables should be set - and will be incremented as
		//further levels of the DIR tree are discovered [through recursive calls to this method]	
		//Values to track the minVal [minimum path length through the DIR tree], a boolean
		//value to track when the minVal has been reached, and the maxVal [maximum path length]
		//should be created:
		int minValue = minVal;
		boolean minValueReached = minValReached;
		int maxValue = maxVal;
		
		//a boolean should be used to detect when a single directory is found, anywhere in 
		//dirArray, in which case,
		//the maxValue will need to be incremented after the for loop.
			//Initially, no DIRs have been detected - so set to false:
		boolean dirDetected = false;
		
		//The minDepthIteration boolean is set to true only in the iteration where the minDepth
			//is found. This is used to save the local fileArray to the instance variable fileArray
			//at the correct minDepth, to ensure the files located at minDepth in the DIR tree are
			//saved to the FileValidation object:
		boolean minDepthIteration = false;
		
		//A second dirArray should be generated for storing new sub-directories that exist
		//in any of the directories in the original dirArray:
		ArrayList<File> dirArray2 = new ArrayList<File>();
		
		//A second ArrayList<File> should be made to store the path to each file detected:
		ArrayList<File> fileArray = new ArrayList<File>();
		
		//Next a loop should go through the contents of each directory in the dirArray
		//to test if its contents contains directories:
		for(int a=0; a<dirArray.size(); a++) {
			
			//First, add each DIR in dirArray to the dirTreeArrayConstrained variable:
			//and finally, add this DIR to the dirTreeArray:
			dirTreeArrayConstrained.add( dirArray.get(a) );
			
			//generate a local boolean to detect DIRs in the current file being parsed:
				//initially set to false, as no DIR detected in current file being parsed yet:
			boolean localDirDetected = false;
			
			//first, capture the files that exist below the directory being parsed:
			File[] files = dirArray.get(a).listFiles();
			
			//next, loop through these files, and check if they are a directory or not:
			for(int b=0; b<files.length; b++) {
				if(files[b].isDirectory() ) {
					//A DIR has been detected, set dirDetected to true:
					dirDetected = true;
					//set localDirDetected to true, 
					//as DIR detected in currently parsed dirArray file:
					localDirDetected = true;
					//and add this DIR to the new dirArray - dirArray2:
					dirArray2.add(files[b]);
				}
				else {
					//the file is a file, so store it in the fileArray:
					
					if(files[b].getName().substring(0,1).equals(".") == false) {
						//only add to fileArray if the title of the files[b] file does not
						//begin with a "." - i.e. it is not a hidden file.
							//This excludes the .DS_Store file on mac which can sometimes be
							//saved to fileArray...
						fileArray.add(files[b]);
					}
				}
			} //end for b
			
			if(localDirDetected == true) {
				//if localDirDetected is true, a DIR has been found in the parsed file.
				//This means this parsed file is not the end of the DIR tree.
				//Therefore, the minimum value must be continued to be calculated.
				//minValueReached should be kept as it is.
			}
			else {
				//else, the localDirDetected boolean is false, which means no DIR was
				//detected in the parsed file, and this file represents the minimum value.
				//Thus, minValueReached should be set to true:
				if(minValueReached == false) {
					//only change is minValueReached is false:
					minValueReached = true;
					//and change the local variable minDepthIteration to true
						//This marks the exact iteration where minDepth was reached, and is used
						//to save the local fileArray at minDepth correctly to the instance variable
						//fileArray:
					minDepthIteration = true;
				}
			}
			
		} //end for a
		
		
		//after parsing all the files in dirArray, if minValueReached is false, then each
		//parsed file in dirArray contained at least one DIR.
		//Therefore, the minValue should be incremented by one:
			//[The minValue will be searched further in the next recursion of this algorithm,
			//see below]
		if(minValueReached == false) {
			minValue = minValue + 1;
		}
		else if(minValueReached == true) {
			//if minValueReached is true, then no DIRs were detected in at least one parsed DIR.
			//in this case, the local fileArray should be stored to the instance variable, to allow
			//the files at this minDepth level to be stored in FileValidation:
			if(minDepthIteration == true) {
				//only save local fileArray to instance variable fileArray if the minDepthIteration
				//variable is true - as this marks the specific iteration where minDepth was determined.
				this.fileArray = fileArray;
			}
		}
		
		//if dirDetected is true, at least one file in dirArray contained a DIR, and thus the
		//maxValue should be incremented by 1, and this method should be re-run with the
		//appropriate input - which is dirArray2[list of all DIRs detected], the new incremented
		//maxValue, the minValue [incremented if minValueReached is false], and the value of
		//minValueReached [true or false depending on if a DIR was detected here or previously
		//with no dub-DIRs]
		if(dirDetected == true) {
			maxValue = maxValue + 1;
			return validateConstrainedCompoundDir(dirArray2, minValue, maxValue, minValueReached);
		}
		
		else {
			//else, dirDetected is equal to false, which means no new DIRs have been detected
			//in any of the files parsed in dirArray.  Thus means the maxValue equals the maximum
			//number of DIRs that can be traversed in the DIR Tree.
			//Thus, the method should return with the appropriate FileValidation object:
			
			//First, generate an array of Strings for the constrainComboBox from the dirTreeArray
				//of Files:
			dirTreeStringsConstrained = new ArrayList<String>();
			dirTreeArrayToStringArrayConstrained(dirTreeArrayConstrained, file);
			
			//Next, since the local fileArray object represents all files present at the Maximum Depth
			//through the DirTree - if the useMinDepth is false [i.e. the maxDepth file array should be used]
			//then the instance variable fileArray should be set to the current [maxDepth] fileArray:
			if(useMinDepth == false) {
				this.fileArray = fileArray; //set the local fileArray at maxDepth, to the instance variable
											//fileArray to return the maxDepth set of files ONLY IF
											//useMinDepth is FALSE.
			}
			
			return new FileValidation(minValue, maxValue, minValue, this.fileArray, null);
			
		}
		
	} //end validateConstrainedCompoundDir()
	
	
	/**
	 * This method converts the dirTreeArray to a String array which contains the top DIR name
	 * and the path to each DIR present below this top DIR, which are present in the dirTreeArray.
	 * @param dirTreeArray
	 * @param file
	 */
	public void dirTreeArrayToStringArray(ArrayList<File> dirTreeArray, File file) {
		
		//String object to hold the relative path:
		String relativePath;
		
		for(int a=0; a<dirTreeArray.size(); a++) {
			
			String absolutePath = dirTreeArray.get(a).getAbsolutePath();
			relativePath = absolutePath.substring( file.getAbsolutePath().length(), absolutePath.length() );
			dirTreeStrings.add(relativePath);
		
		}
	}
	
	
	/**
	 * This method converts the dirTreeArray to a String array which contains the top DIR name
	 * and the path to each DIR present below this top DIR, which are present in the dirTreeArray.
	 * @param dirTreeArray
	 * @param file
	 */
	public void dirTreeArrayToStringArrayConstrained(ArrayList<File> dirTreeArray, File file) {
		
		//String object to hold the relative path:
		String relativePath;
		
		for(int a=0; a<dirTreeArray.size(); a++) {
			
			String absolutePath = dirTreeArray.get(a).getAbsolutePath();
			relativePath = absolutePath.substring( file.getAbsolutePath().length(), absolutePath.length() );
			dirTreeStringsConstrained.add(relativePath);
		
		}
	}
	
	
	/**
	 * This method allows the user to set the output file for the data analysis.
	 * This is where all of the data output will be saved to. This includes a
	 * File reference 'outputDir' which represents the DIR to save the output to,
	 * and a String reference 'outputName' which represents the name of the output.
	 * @param outputDir The File object indicating the output location for data.
	 */
	public void setOutputDir(File file) {
		
		if(file != null) {
		
			if(file.exists() == true) {
				//if the file exists, the output will be the parent DIR:
				outputDir = file.getParentFile();
				outputName = determineOutputName( file.getName() );
			}
			else {
				//else the file does not exist - should retrieve the file name,
				//and use this as the basis of the outputDir:
				outputDir = null;
				outputName = determineOutputName( file.getName() );
			}
			
		} //end if null
		
		else {
			//file is null:
		}
	
	}//end setOutputDir()


	/**
	 * This method allows the user to set the output file for the data analysis.
	 * This is where all of the data output will be saved to.
	 * @param outputDir The File object indicating the output location for data.
	 */
	public void setOutputDir(File file, String fileName) {
		
		if(file != null) {
		
			if(file.exists() == true) {
				//if the file exists, the output will be the parent DIR:
				outputDir = file.getParentFile();
				outputName = determineOutputName( file.getName() );
			}
			else {
				//else the file does not exist - should retrieve the file name,
				//and use this as the basis of the outputDir:
				outputDir = null;
				outputName = determineOutputName( file.getName() );
			}
			
		} //end if null
		
		else {
			//file is null: the parsed image only exists in ImageJ.
			//Therefore set outputDir to null:
			outputDir = null;
			//and set the output name based on the fileName parsed:
			outputName = determineOutputName(fileName);
		}
	
	}//end setOutputDir()


	/**
	 * This method sets the output DIR or File name, which is based on the input file
	 * name.  This name may be a DIR, or a File.  This is irrelevant for naming.
	 * Two goals here are to:
	 * <p>
	 * 1. Remove any File Extension, if it exists.
	 * <p>
	 * 2. Add an appropriate number to the beginning of the file name.
	 * <p>
	 * The naming convention is to begin the DIR or File name with a number: if
	 * this is the case, this new DIR or File should be numbered with an increment
	 * by 1.  Further details on this process are in the incrementString() and 
	 * incrementStringAndArrayList() methods.
	 * @param name String of the name of the input file.
	 * @return String which outputs an appropriate output file or DIR name.
	 */
	public String determineOutputName(String name) {
		
		//First, the method needs to determine if name has a file extension on it.
		//The easiest way to detect this is to test if the fourth from last character
		//is a '.' - which will be the case if a file extension of three letters is used
		//[which is the standard used across computers in general, esp for image files]
		if(name.charAt( (name.length() - 4) ) == '.') {
			//if the fourth from last character is a dot, remove the last four characters
			//from name:
			name = name.substring(0, (name.length() - 4) );
			//Also, retrieve the fileExtension String and save the instance variable fileExtension:
			fileExtension = name.substring(name.length() - 4);
		}
		
		//This method then needs to increment any number on the front of the
		//name String by 1.
		//This does depend on numbers of other files present in the parent DIR [to
		//avoid numbering two files or DIRs with the same number:
		
		if(outputDir == null) {
			//if outputDir is null, then simply need to increment any number on the
			//front of the parsed String by 1, or add the String "01" to the title
			//String if no number is present.
			//NOTE: outputDir is only null if the file does not exist [i.e it is only
			//an image in ImageJ], or is null [again, it means the parsed object is only
			//an image in ImageJ]
			return incrementString(name);
		}
		
		//First, retrieve all other Files located in the parent DIR:
		File[] files = null;
		ArrayList<String> fileNamesArrayList = new ArrayList<String>();
		if(outputDir != null) {
			files = outputDir.listFiles();
			for(int a=0; a<files.length; a++) {
				fileNamesArrayList.add( files[a].getName() );
			}
		}
		// If fileNamesArrayList is not null, then some files have been collected, and these should be
		// checked for any numbers at the beginning of the files:
		if(fileNamesArrayList.size() > 0) {
			return incrementStringAndArrayList(name, fileNamesArrayList);
		}
		else {
			//else, just return the name incremented by itself (as there are no other files to worry about):
			return incrementString(name);
		}
		
	}
	
	
	/**
	 * This method should adjust the outputName variable to now match the constrained file
	 * name.  It uses the outputNumberString, set in incrementString(), which indicates the
	 * appropriate number in the output folder (since a constrained analysis still outputs to
	 * the parent of the originally selected input fold, and thus this numbering must be maintained).
	 * It concatenates this with the constrainedFile's name, after removing any numbers or spaces
	 * at the beginning of it.
	 */
	public void setOutputNameConstrained() {
		outputName = outputNumberString + "_" + removeNumberString(constrainedFile.getName() );
	}
	
	
	/**
	 * This method allows the outputName String to be adjusted.  This will need to be performed
	 * by the JTextField 'outputTitle' in the DialogWindow class, if the user decides to edit
	 * the provided title for output.
	 * @param name
	 */
	public void setOutputName(String name) {
		this.outputName = name;
	}


	/**
	 * This method removes any numbers or spaces present at the beginning of a String.  This
	 * is required for the setOutputNameConstrained() method, to ensure only the outputNumberString
	 * is appended to the start of the constrained DIR, and no other numbers.  Removing any spaces
	 * also ensure the outputName is in the format "XX DIRname".
	 * @param fileName The String of the fileName to be modified.
	 * @return the fileName with no numbers or spaces at the beginning.
	 */
	public String removeNumberString(String fileName) {
		
		//First, create an int to store the number of numbers there are at the beginning of
		//the String:
		int numbersInString = 0;
		
		//loop through the fileName String to look at each character from the start:
		for(int a=0; a<fileName.length(); a++) {
			//store the character under a new variable, character:
			char character = fileName.charAt(a);
			//look at character and see if it is a number or space:
			if(isCharANumberOrSpace(character) == false) {
				//if character is not a number or space, then break out of the for loop.
				break;
			}
			//if no break has occurred, character is a number or space.  
			//Therefore, increment numbersInString by 1:
			numbersInString = numbersInString + 1;
		}
		
		//At this point, numbersInString will equal the number of numbers and spaces present at
		//the beginning of fileName.
		//Thus, fileName needs to be shortened, beginning at the index numbersInString.
		//Use substring:
		
		fileName = fileName.substring(numbersInString);
		
		//Finally, return fileName:
		
		return fileName;
	}
	

	public String incrementString(String name) {
		
		String numberString = ""; //a String to collect number characters at the beginning of 'name' String.
		
		for(int a=0; a<name.length(); a++) {
			char character = name.charAt(a);
			//look at each character and see if it is a number:
			if(isCharANumber(character) == false) {
				break;
			}
			//if no break has occurred, the char is a number.  Add this to the String 'number'
			numberString = numberString + character;
		}
		//at this point, number will either contain a number of characters making a number,
		//or will be an empty string.  The next step is to convert the String to an integer:
		if(numberString.length() ==0) {
			//if the string is empty, simple add the String "01" to the start of name.
			numberString = "01";
			name = numberString + "_" + name;
		}
		else {
			//first, gather the String name without the integer at the start:
			String nameText = name.substring( numberString.length() );
			//else there is a number string to convert to Integer:
			int number = Integer.parseInt(numberString);
			//increment number by 1:
			number = number + 1;
			//next, convert number back to a String.
			//First, if number is less than 10, ensure this String has a 0 at the start:
			if(number < 10) {
				numberString = "0" + number;
			}
			else {
				//else set numberString to the number:
				numberString = "" + number;
			}
			
			//Finally, the numberString must replace the number at the start of the String name:
			//First, loop over nameText to remove any space at the beginning of it:
			int indexSpaces = 0; //int to index any spaces at the beginning of nameText.
			for(int a=0; a<nameText.length(); a++) {
				char character = nameText.charAt(a);
				if(isCharANumberOrSpace(character) == false) {
					break;
				}
				//if character is a space, increment indexSpaces by 1:
				indexSpaces = indexSpaces + 1;
			}
			//Next, make nameText a substring, using indexSpaces as first index:
			nameText = nameText.substring(indexSpaces);
			
			//Finally, set name to numberString, plus one space, plus nameText:
			name = numberString + "_" + nameText;
			
		}
		
		outputNumberString = numberString;
		
		//Finally, return name, which now has a [new] Integer at the beginning of it:
		return name;
	}


	public String incrementStringAndArrayList(String name, ArrayList<String> fileNamesArrayList) {
		
		String numberString = ""; //a String to collect number characters at the beginning of 'name' String.
		
		for(int a=0; a<name.length(); a++) {
			char character = name.charAt(a);
			//IJ.showMessage("character: "+character);
			//look at each character and see if it is a number:
			//IJ.showMessage("isCharANumber return: "+isCharANumber(character) );
			if(isCharANumber(character) == false) {
				break;
			}
			//if no break has occurred, the char is a number.  Add this to the String 'number'
			numberString = numberString + character;
		}
		
		//Next, firstly generate an ArrayList<String>  to store the numberStrings:
		ArrayList<String> numberStrings = new ArrayList<String>();
		
		//Next, repeat this process across all fileNames in the ArrayList:
		for(int a=0; a<fileNamesArrayList.size(); a++) {
			
			String arrayListNumberString = "";
			
			for(int b=0; b<fileNamesArrayList.get(a).length(); b++) {
				char character = fileNamesArrayList.get(a).charAt(b);
				//look at each character and see if it is a number:
				if(isCharANumber(character) == false) {
					break;
				}
				//if no break has occurred, the char is a number.  Add this to the String 'number'
				arrayListNumberString = arrayListNumberString + character;
			}
			//here, the arrayListNumberString now contains the numberString to add to the numberStrings
			//array list:
			numberStrings.add(arrayListNumberString);
			
		}
		
		
		//at this point, numberString will either contain a number of characters making a number,
		//or will be an empty string.  
		//Likewise, the numberStrings ArrayList will contain an array of empty Strings, or Integers.
		
		//The next step is to convert the Strings to integers:
		//First, generate an ArrayList<Integer> for storing the Integers from numberStrings:
		ArrayList<Integer> numberInts = new ArrayList<Integer>();
		
		//loop over the numberStrings array list:
		for(int a=0; a<numberStrings.size(); a++) {
			//if numberStrings item is a number (i.e) is bigger than 0):
			if(numberStrings.get(a).length() > 0) {
				//then parse the int and add it to the numberInte array list:
				numberInts.add( Integer.parseInt(numberStrings.get(a) ) );
			}
			else {
				//if no number to add, just add 0 to the numberInte array list:
				numberInts.add(0);
			}
		}
		
		//numberStrings has been converted to an array numberInts.
		//The next step is to parse numberString to an int, increment it by 1, 
		//and then compare this to the numberInts Array List, continually incrementing
		//until the parsed numberString is incremented to a new number:
		
		//First, concert numberString into an int:
		
		int number; //an int to hold the parsed integer.
		
		//first, gather the String name without the integer at the start:
		String nameText;
		
		//IJ.showMessage("Number String [AL]" + numberString);
		
		if(numberString.length() ==0) {
			//if the string is empty, set number to 1.
			number = 1;
			nameText = name;
		}
		else {
			
			//first, get the Text which makes up the name:
			nameText = name.substring( numberString.length() );
			
			//else there is a number string to convert to Integer:
			number = Integer.parseInt(numberString);
			
			//increment number by 1:
			number = number + 1;
			
		}
		
		//now the number needs to be checked against the numberInts array list and
		//incrementing the number until it passes the whole numberInts list without
		//matching ANY number on the list:
		boolean matched;
		number = number -1; //start by minusing 1 from number, to allow the first addition of number
		//to give the correct start value for number:
		do {
			matched = false;
			number = number+1;
			for(int a=0; a<numberInts.size(); a++) {
				if(number == numberInts.get(a) ) {
					matched = true;
					break;
				}
			}
		} while(matched == true);
		
		//At this point, number should equal a unique value to be added to name:
	
		//next, convert number back to a String.
		//First, if number is less than 10, ensure this String has a 0 at the start:
		if(number < 10) {
			numberString = "0" + number;
		}
		else {
			//else set numberString to the number:
			numberString = "" + number;
		}

		//Finally, the numberString must replace the number at the start of the String name:
		
		//First, loop over nameText to remove any space at the beginning of it:
		int indexSpaces = 0; //int to index any spaces at the beginning of nameText.
		for(int a=0; a<nameText.length(); a++) {
			char character = nameText.charAt(a);
			if(isCharANumberOrSpace(character) == false) {
				break;
			}
			//if character is a space, increment indexSpaces by 1:
			indexSpaces = indexSpaces + 1;
		}
		//Next, make nameText a substring, using indexSpaces as first index:
		nameText = nameText.substring(indexSpaces);
		
		//Finally, set name to numberString, plus one space, plus nameText:
		name = numberString + "_" + nameText;
		
		//Set outputNumberString to numberString, to keep track of the number for constrained
		//analysis file title:
		outputNumberString = numberString;
		
		//Finally, return name, which now has a [new] Integer at the beginning of it:
		return name;
	}
	
	
	public Boolean isCharANumber(char character) {
		//if the character is not a number character, return false:
		if(character != '0' && character != '1' && character != '2' && character != '3' && 
				character != '4' && character != '5' && character != '6' && character != '7' && 
				character != '8' && character != '9') {
			return false;
		}
		else {
			return true;
		}
	}
	
	/**
	 * This method returns FALSE is the character parsed is a number or a space character. Otherwise
	 * it returns TRUE.  Used in the removeNumberString(), incrementString() and incrementStringAndArrayList()
	 * methods.
	 * @param character The character to be tested.
	 * @return
	 */
	public Boolean isCharANumberOrSpace(char character) {
		//if the character is not a number character, return false:
		if(character != '0' && character != '1' && character != '2' && character != '3' && 
				character != '4' && character != '5' && character != '6' && character != '7' && 
				character != '8' && character != '9' && character != ' ') {
			return false;
		}
		else {
			return true;
		}
	}
	
	
	/**
	 * This method should set the outputFileArray from the variables 'fileArray', 'file', 
	 * 'outputDir' and 'outputName'.  fileArray contains an array of File objects which provide
	 * the paths to all images to be processed.  By removing the parent path provided by file
	 * from each path in the fileArray, each File Path in fileArray now provides a relative path
	 * from the parent DIR, which can be set as desired.  The next step is to fill each File Path
	 * with a parent path and DIR - which are provided by outputDir and outputName, respectively.
	 * <p>
	 * If the input is constrained, fileArray is adjusted accordingly, but the File object
	 * 'constrainedFile' should be used to remove the parent path rather than the 'file' object.
	 * <p>
	 * This is further complicated by the selection of ImageJ images or individual Images, where
	 * the output of the file name itself is adjusted.  This will need to be dealt with in a
	 * special manner.
	 */
	public void setOutputFileArray() {
		
		ArrayList<File> outputFileArray = new ArrayList<File>();
		File parentFile = null;
		
		if(fileArray != null) {
		
			//First, set the parentFile to the appropriate file - either 'file' or 'constrainedFile'
			//depending on the value of constrainedAnalysis:
			if(constrainedAnalysis == false) {
				//if constrainedAnalysis is false, use the 'file' reference to remove parent path.
				parentFile = this.file;
			} //end if constrainedAnalysis is false
			else if(constrainedAnalysis == true) {
				//If constrainedAnalysis is true, use the 'constrainedFile' ref to remove parent path.
				parentFile = this.constrainedFile;
			} //end if constrainedAnalysis is true.
			
			if(validityValue > VALID_IMAGE) {
			//This code is for SIMPLE_DIR and COMP_DIRs only:
			
				//First loop through fileArray:
			for(int a=0; a<fileArray.size(); a++) {
				//retrieve the paths of the fileArray and parentFile objects:
				String fileArrayPath = fileArray.get(a).getAbsolutePath();
				String parentPath = parentFile.getAbsolutePath();
				//determine the parent file path's length:
				int parentPathLength = parentPath.length();
				//substring the fileArray path to remove the parentPath:
				fileArrayPath = fileArrayPath.substring(parentPathLength);
				//and store this as a File in outputFileArray:
				outputFileArray.add( new File(fileArrayPath) );
			}//end for a
			
			//At this point, the outputFileArray contains relative paths of all images to be processed.
			//Thus, to build the outputs of these images, each File in outputFileArray must be preceded
			//with the outputDir path and the outputName DIR name:
			
			for(int a=0; a<outputFileArray.size(); a++) {
				String pathName = outputDir + File.separator + outputName + outputFileArray.get(a).getAbsolutePath();
				//If pathName has a file extension at the end, remove it:
				if(pathName.lastIndexOf(".") == pathName.length()-4) {
					pathName = pathName.substring(0, pathName.length() - 4);
				}
				File newFile = new File(pathName);
				outputFileArray.set(a, newFile);
			}
			//outputFileArray should now be filled with the correct pathNames for each input file.
			//Thus, set outputFileArray instance variable to outputFileArray:
			
			this.outputFileArray = outputFileArray;
			
			} //end of validityValue > VALID_IMAGE
			
			else if(validityValue == VALID_IMAGE) {
				//the parsed file is a VALID_IMAGE, which will need to be dealt with in a special
				//manner.
				//First, create a String of the pathName, which will be the outputDir, the outputName
				//[which is set in the JTextField ouputTitle in DialogWindow], and completed with the
				//fileExtension (assumed to be the same as the input??).
				String newFilePath = outputDir + File.separator + outputName ; // + fileExtension;
				//IJ.showMessage("Output Dir: "+outputDir);
					//IJ.showMessage("Output Name: "+outputName);
				//For now keep this as no Extension, as I believe the saving of a file will automatically
					//append an extension to the file.
				
				//Next, create a file object from this path:
				File newFile = new File(newFilePath);
				
				//finally, save newFile into the local variable outputFileArray:
				outputFileArray.add(newFile);
				
				//and save this ArrayList to the instance variable outputFileArray:
				this.outputFileArray = outputFileArray;
				
			} //end else if validityValue == 1
			
			else {
				//Else the validityValue is below VALID_IMAGE, which means the image MUST be open in 
				//ImageJ without any File System representation.
				//In this case, there is no way to know the output, so set outputFileArray to null:
				this.outputFileArray = null;
				//I think this code is redundant, as if validityValue is INVALID_IMAGE, fileArray is null!
				//Keep here just in case and for robustness!				
			}//end else (validity value is BELOW VALID_IMAGE)
			
		
		}//end if fileArray != null
		
		else if(fileArray == null) {
			//if fileArray is null, there is no input from the FileSystem.
			//Thus, set outputFileArray to null:
			this.outputFileArray = null;
		}
		
		//Finally, just display the outputFileArray in IJ as a Message to see what it looks like:
		//IJ.showMessage("OutputFileArray: "+this.outputFileArray);
		
	} //end setOutputFileArray()
	
	
	
	/**
	 * This method will first determine the structure of any output Dir Tree which needs to be 
	 * created in order to fulfill the output of files as indicated in the outputFileArray instance
	 * variable.
	 * <p>
	 * This is achieved by processing the (constrained) dirTreeArray and dirTreeStrings variables,
	 * to determine each DIR which must be created.  Fortunately the dirTree is built up in a
	 * recursive manner, starting with the first layer of DIRs and working downwards, so when creating
	 * the DIR tree, there is no possibility of trying to create a DIR which has not already had
	 * its parent DIR created.
	 * <p>
	 * A similar process should be followed for setOutputFileArray(), where the parent path should
	 * be removed from each of the Files in the dirTreeArray[Constrained], and this filled in with the
	 * appropriate outputDir and outputName to reconfigure the path from input to output.
	 */
	public void createOutputDirTree() {
		
		if(validityValue > VALID_IMAGE) {
			//Only perform processing here if the validityValue is higher than VALID_IMAGE
			//i.e. it is a Simple DIR or Compound DIR which requires the creation of one or more
			//Directories.
			//NB: The Simple DIR might need to have the dirTreeArray set - as I think atm this is
			//only set for Compound DIRs...
				//Now done -> have set dirTreeArray with SIMPLE_DIR to file via dirArray..
			
			File parentFile = null; //local variable to hold a reference to the parentFile
				//either file or constrainedFile, depending on if constrainedAnalysis is being performed.
			
			ArrayList<File> dirTree = new ArrayList<File>(); //local variable to hold a ref to the
				//appropriate dirTreeArray - either constrained or not.
			
			ArrayList<File> outputDirTreeArray = new ArrayList<File>(); //local outputDirTreeArray to
			//store the file references before setting to the instance variable, outputDirTreeArray.
			
			if(constrainedAnalysis == false) {
				//set parentFile to file, as no constrainedAnalysis is being performed
				parentFile = file;
				//Also set dirTree to dirTreeArray:
				dirTree = dirTreeArray;
			}
			else if(constrainedAnalysis == true) {
				//set parentFile to constrainedFile, as a constrainedAnalysis is being performed.
				parentFile = constrainedFile;
				//Also set dirTree to dirTreeArrayConstrained:
				dirTree = dirTreeArrayConstrained;
			}
			
			//IJ.showMessage("Parent path: "+parentFile.getAbsolutePath() );
			//IJ.showMessage("Dir Tree Paths: " + dirTree );
			
			//First loop through dirTreeArray:
			for(int a=0; a<dirTree.size(); a++) {
				//retrieve the paths of the fileArray and parentFile objects:
				String fileArrayPath = dirTree.get(a).getAbsolutePath();
				String parentPath = parentFile.getAbsolutePath();
				//determine the parent file path's length:
				int parentPathLength = parentPath.length();
				//substring the fileArray path to remove the parentPath:
				fileArrayPath = fileArrayPath.substring(parentPathLength);
				//and store this as a File in outputFileArray:
				outputDirTreeArray.add( new File(fileArrayPath) );
			} //end for a
			
			//IJ.showMessage("OutputDirTreeArray Path: "+ outputDirTreeArray.get(0).getAbsolutePath() );
			
			//At this point, the outputDirTreeArray contains relative paths of all DIRs to be created.
			//Thus, to build the output DIRs, each DIR in outputDirTreeArray must be preceded
			//with the outputDir path and the outputName DIR name:
			
			//For Simple DIRS outputDirTreeArray will contain a blank file
				//[Actually, it will return the path to FIJI! Either way, it gives the wrong result]
			//So if the DIR parsed is a SIMPLE DIR, just concatenate outputDir and outputName, with
			//a File.separator character in between:
		if(constrainedAnalysis == false) {
			if(validityValue == VALID_SIMPLE_DIR) {
				
				String pathName = outputDir + File.separator + outputName;
				File newFile = new File(pathName);
				outputDirTreeArray.set(0, newFile);
				
			} //end if validityValue == VALID_SIMPLE_DIR
			
			// else, the outputDirTreeArray will have the relative paths for all DIRs below the selected
			//DIR, so fill it appropriately:
			if(validityValue > VALID_SIMPLE_DIR) {
			
				for(int a=0; a<outputDirTreeArray.size(); a++) {
					String pathName = outputDir + File.separator + outputName 
							+ outputDirTreeArray.get(a).getAbsolutePath() + File.separator;
					File newFile = new File(pathName);
					outputDirTreeArray.set(a, newFile);
				}
			
			}//end if validityValue > VALID_SIMPLE_DIR
		}
		else if(constrainedAnalysis == true) {
			//If constrain analysis is true, need to used constrainedValidityValue to determine if the input is
			//a SIMPLE_DIR or COMP_DIR:
			if(constrainedValidityValue == VALID_SIMPLE_DIR) {
				
				String pathName = outputDir + File.separator + outputName;
				File newFile = new File(pathName);
				outputDirTreeArray.set(0, newFile);
				
			} //end if validityValue == VALID_SIMPLE_DIR
			
			// else, the outputDirTreeArray will have the relative paths for all DIRs below the selected
			//DIR, so fill it appropriately:
			if(constrainedValidityValue > VALID_SIMPLE_DIR) {
			
				for(int a=0; a<outputDirTreeArray.size(); a++) {
					String pathName = outputDir + File.separator + outputName 
							+ outputDirTreeArray.get(a).getAbsolutePath() + File.separator;
					File newFile = new File(pathName);
					outputDirTreeArray.set(a, newFile);
				}
			
			}//end if validityValue > VALID_SIMPLE_DIR
			
		}
			//outputDirTreeArray should now be filled with the correct pathName(s) for each DIR.
			//Thus, set outputDirTreeArray instance variable to outputDirTreeArray:
			
			this.outputDirTreeArray = outputDirTreeArray;
			
			//Now, just display the outputFileArray in IJ as a Message to see what it looks like:
			//IJ.showMessage("OutputDirTreeArray: "+this.outputDirTreeArray);
			
			//Finally, the outputDirTreeArray DIRs should actually be created in the File System:
			
			//loop through the outputDirTreeArray:
			for(int a=0; a<this.outputDirTreeArray.size(); a++) {
				//boolean dirMade = this.outputDirTreeArray.get(a).mkdir();
				//can also run the command mkdirs() - will create any parent DIRs necessary:
				boolean dirMade = this.outputDirTreeArray.get(a).mkdirs();
				//IJ.showMessage("DIR Made: "+dirMade);
			}
			
			
		} //end if validtyValue > VALID_IMAGE
		
		
	} //end createOutputDirTree()
	
	
	/**
	 * This method deletes the Output Dir Tree - can be performed if the processing does not
	 * result in any output.
	 */
	public void deleteOutputDirTree() {
		
		if(outputDirTreeArray != null) {
			
			File parentFile = outputDirTreeArray.get(0).getParentFile();
		
			for(int a=this.outputDirTreeArray.size()-1; a>-1; a--) {
				//boolean dirMade = this.outputDirTreeArray.get(a).mkdir();
				//can also run the command mkdirs() - will create any parent DIRs necessary:
				outputDirTreeArray.get(a).delete();
				//IJ.showMessage("DIR Made: "+dirMade);
		
			}//end for a
			//IJ.showMessage("Parent File: "+parentFile.getAbsolutePath() );
			boolean del = parentFile.delete();
			//IJ.showMessage("deleted: "+del);
		
		}//end if not null
		
	}//end deleteOutputDirTree()


	/**
	 * This class represents the data which is returned by the validate & validateCompoundDir
	 * methods.  The instance variables represent the minimum and maximum depth of the DIR tree,
	 * the validity (which is equal to the minimum depth in the case of compound DIRs), an array
	 * of File objects if the parsed file is on the File System, and an ImagePlus object if the
	 * parsed file is in ImageJ only.
	 * @author stevenwest
	 *
	 */
	public class FileValidation {
		
		int minDepth;
		int maxDepth;
		
		int validity;
		
		ArrayList<File> files;
		
		ImagePlus imp;
		
		public FileValidation(int minVal, int maxVal, int validity, ArrayList<File> files, 
									ImagePlus imp) {
			this.minDepth = minVal;
			this.maxDepth = maxVal;
			this.validity = validity;
			this.files = files;
			this.imp = imp;
		}
		
		
		public ImagePlus getImp() {
			return imp;
		}
		
		
		public ArrayList<File> getFiles() {
			return files;
		}
		
		
		public int getValidity() {
			return validity;
		}
		
		
		public int getMinDepth() {
			return minDepth;
		}
		
		
		public int getMaxDepth() {
			return maxDepth;
		}
		
		
		public String getValidityString() {
			if(validity == -1) {
				return "IJ Image";
			}
			else if(validity == 0) {
				return "File Image";
			}
			else if(validity == 1) {
				return "Simple DIR";
			}
			else {
				//must be a compound DIR:
				return "Compound DIR";
			}
		}
		
		
	}//end class FileValidation
	
	
} //end class StereoMateInputOutputFramework
