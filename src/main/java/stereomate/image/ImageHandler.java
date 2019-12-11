package stereomate.image;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import ij.process.LUT;
import mcib3d.image3d.ImageByte;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.processing.Flood3D;
import stereomate.data.DatasetWrapper;
import stereomate.data.ObjectDataContainer;
import stereomate.data.ObjectDatasetMap;
import stereomate.object.ObjectVoxelProcessing;
import stereomate.object.SelectedObject;
import stereomate.roi.RoiAssessmentHandler;
import stereomate.roi.RoiAssessmentHandler.Points;
import weka.core.Instances;

/**
 * This class contains the original ImagePlus (8-bit, hyperstack), the activeChannel as an ImagePlus
 * object, ImageInt representation of the activeChannel image, a thresholdProcedureStack object for 
 * thresholding the hyperstack, and both an ImagePlus and ImageInt representation of the thresholded image.
 * 
 * @author stevenwest
 *
 */
public class ImageHandler {
	
	/**
	 * ImagePlus object to store the original imp in:
	 */
	public ImagePlus imp;
	
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
	public ImagePlus thresholdImp;
	
	/**
	 * The thresholdImp which has been wrapped into an ImageInt - the TANGO / MCIB 3D representation of an image.
	 * Used for rapid identification and measurement of objects.
	 */
	public ImageInt thresholdImgInt;
	
	/**
	 * Reference to the image stack in the original imp which contains the ImageProcessors representing the channel being
	 * processed.  This is used to build an ImageInt which will collect intensity data on objects which are defined by the
	 * thresholded imp.
	 */
	ImagePlus activeChannel;
	
	/**
	 * The activeChannel which has been wrapped into an ImageInt - the TANGO / MCIB 3D representation of an image.
	 * This ImageInt is used for measuring intensity data of objects (as the threhsoldImgInt is only a mask, whereas
	 * the activeChannelInt contains the raw intensity data).
	 */
	ImageInt activeChannelInt;
	
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
	 * The coordinates which are incremented in returnNextCoord(), and used to assess objects in the thresholdImgInt.
	 */
	int x, y, z;
	
	/**
	 * Variables which encode the position where the image loop should start with each call to loopThresholdImgInt().
	 */
	int xStart, yStart, zStart;
	
	/**
	 * Variable to keep track of the object number in the loopThresholdImgInt() method.
	 */
	int objCounter;
	
	/**
	 * This Boolean represents when the loop through loopThresholdImgInt() is complete - all pixels have been traversed
	 * and all data from all objects has been returned.
	 */
	boolean loopComplete;
	
	/**
	 * Sets the imp and procedureStack variables to passed parameters, then runs the procedure stack, which
	 * sets the thresholdImp activeChannel imps (based on activeChannel int in the procedureStack) and
	 * runs the procedureStack on the thresholdImp.  Then the thresholdImp and activeChannel imps are wrapped
	 * up into their ImgInt objects, thresholdImgInt & activeChannelInt.
	 * @param imp
	 * @param procedureStack
	 */
	public ImageHandler(ImagePlus imp, ImageProcessingProcedureStack3 procedureStack, String objConnectivity) {
		
		// set the imp and procedureStack variables:
		this.imp = imp;
		this.procedureStack = procedureStack;
		
		//For dealing with Border Voxel Processing:
		borderObjPixProcessing3D = new ObjectVoxelProcessing(objConnectivity);
		
		runProcedureStack();
		
		wrapImageInts();
		
	}
	
	public ImageHandler(ImagePlus imp, ImageProcessingProcedureStack3 procedureStack, 
								String objConnectivity, String thresholdPath) {
		
		if(thresholdPath == null) {
			// set the imp and procedureStack variables:
			this.imp = imp;
			this.procedureStack = procedureStack;
			
			//For dealing with Border Voxel Processing:
			borderObjPixProcessing3D = new ObjectVoxelProcessing(objConnectivity);
			
			runProcedureStack();
			
			wrapImageInts();
		}
		else { // if path is not null, try to open the thresholdImp
		
			//IJ.showMessage("ImageHandler - thresholdPath: "+thresholdPath);
			
			// set the imp and procedureStack variables:
			this.imp = imp;
			this.procedureStack = procedureStack;
		
			//For dealing with Border Voxel Processing:
			borderObjPixProcessing3D = new ObjectVoxelProcessing(objConnectivity);
			
			thresholdImp = IJ.openImage(thresholdPath);
			
			//IJ.showMessage("ImageHandler - thresholdImp: "+thresholdImp);
					
			if(thresholdImp != null) {
				// need to set activeChannel:
				setActiveChannel();
			}
			else {
				runProcedureStack(); // if still null, just run ProcedureStack!
			}
		
			wrapImageInts();
		}
	}
	
	/**
	 * Run the procedureStack in this class on the imp's activeChannel.  The method will generate a copy of the active
	 * channel stack in imp, which is put into thresholdImp.  The thresholdImp is then converted to the correct
	 * bit depth, and the threshold procedure stack is then run on it.
	 */
	public void runProcedureStack() {
		
		//FIRST -> Duplicate the correct channel from the imp:
		thresholdImp = duplicateChannel( imp, procedureStack.activeChannel );
		
		//ALSO set the active imp -> this references the image stack of the activeChannel in original imp,
		//which is used to build the ImageInt for intensity analysis on the original imp active channel:
		setActiveChannel();
				
		//SECOND -> Convert to correct bit depth, if necessary:				
		ImageWindowWithPanel.convertBitDepth(thresholdImp, procedureStack.bitDepth);
				
		//THIRD -> Apply the ProcedureStack to the thresholdedImp:
		procedureStack.runProcedureStack(thresholdImp);
		
	}
	
	public static ImagePlus duplicateChannel(ImagePlus imp, int channel) {
		//Duplicate the correct channel from the imp:
		ImageStack is = ImageWindowWithPanel.filterImageStack( imp.getStack(), channel, imp.getNChannels() );		
		return new ImagePlus(imp.getTitle(), is);
	}
	
	/**
	 * Set the active channel based on the imp and activeChannel int in procedureStack.
	 * @param activeChannel
	 */
	public void setActiveChannel() {
		
		activeChannel = new ImagePlus(imp.getTitle(), 
				ImageWindowWithPanel.filterImageStackNoDup( imp.getStack(), 
															procedureStack.activeChannel, 
															imp.getNChannels() )  
						);
	
	}
	
	/**
	 * Wrap the thresholdImp, activeChannel into ImageInt objects
	 */
	public void wrapImageInts() {
		//Get calibration from original imp:
		Calibration cal = thresholdImp.getCalibration();
	
		thresholdImgInt = ImageInt.wrap(thresholdImp);
		if (cal != null) {
			thresholdImgInt.setCalibration(cal);
		}
	
		//ALSO -> Wrap the activeChannel into an ImageInt obj from MCIB3D library:
		//This is used to calculate intensity information from obj masks derived from thresholdImp
		//As this image contains greyscale data!
	
		cal = activeChannel.getCalibration();
	
		activeChannelInt = ImageInt.wrap(activeChannel);
		if (cal != null) {
			activeChannelInt.setCalibration(cal);
		}
	}
	
	/**
	 * Save image as TIFF to filePath.
	 * @param filePath
	 */
	public void saveThresholdImp(File filePath) {
		saveThresholdImp( filePath.getAbsolutePath() );
	}
	
	/**
	 * Save image as TIFF to path.
	 * @param path
	 */
	public void saveThresholdImp(String path) {
		//IJ.showMessage("Path: "+path);
		saveThresholdImp(path, ".tif");
	}
	
	/**
	 * Save image to path with extension.
	 * @param path
	 * @param extension
	 */
	public void saveThresholdImp(String path, String extension) {
		IJ.save(thresholdImp, path + extension);
	}
	
	/**
	 * Set the selectedObj to the unselectedObj's unselectedPixelValue.
	 * @param selectedObj
	 */
	public void setObjValue(SelectedObject selectedObj) {
		borderObjPixProcessing3D.selectObj3d(thresholdImgInt, selectedObj.x, selectedObj.y, 
				selectedObj.z, selectedObj.getUnselectedPixelValue(), 0 );
	}
	
	/**
	 * Set the selectedObj to the value objClassSelected.
	 * @param selectedObj
	 * @param newObjVal
	 */
	public void setObjValue(SelectedObject selectedObj, int newObjVal) {
		borderObjPixProcessing3D.selectObj3d(thresholdImgInt, selectedObj.x, selectedObj.y, 
				selectedObj.z, newObjVal, 0);
	}
	
	/**
	 * Set the selectedObj to the value objClassSelected.
	 * @param selectedObj
	 * @param newObjVal
	 */
	public void setObjValue(SelectedObject selectedObj, int newObjVal, int newObjValueUnselected) {
		borderObjPixProcessing3D.selectObj3d(thresholdImgInt, selectedObj.x, selectedObj.y, 
				selectedObj.z, newObjVal, newObjValueUnselected);
	}
	
	/**
	 * Set the object in thresholdedImp found at pixel (X,Y,Z) to the newObjValue.  If newObjValue is already 
	 * the pixel value of the object, then the method will just return (prevents infinite loop in setting 
	 * object pixel value!).
	 * @param x
	 * @param y
	 * @param z
	 * @param pixelValue
	 */
	public void setObjValue(int x, int y, int z, int newObjValue) {
		if(newObjValue != getPixelValueThresholded(x,y,z) ) {
			borderObjPixProcessing3D.selectObj3d(thresholdImgInt, x, y, z, newObjValue, 0);
		}
	}
	
	/**
	 * Set the object in thresholdedImp found at pixel (X,Y,Z) to the newObjValue.  If newObjValue is already 
	 * the pixel value of the object, then the method will just return (prevents infinite loop in setting 
	 * object pixel value!).
	 * @param x
	 * @param y
	 * @param z
	 * @param pixelValue
	 */
	public SelectedObject setObjValue(int x, int y, int z, int newObjValue, int newObjValueUnselected) {
		if(newObjValue != getPixelValueThresholded(x,y,z) ) {
			return borderObjPixProcessing3D.selectObj3d(thresholdImgInt, x, y, z, newObjValue, newObjValueUnselected);
		}
		return null;
	}
	
	/**
	 * Returns the pixel value in the thresholded image, using 0-based reference.
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public int getPixelValueThresholded(int x, int y, int z) {
		return thresholdImgInt.getPixelInt(x, y, z);
	}
	
	/**
	 * Retrieves the first instance of a pixel above 0 in the thresholdImp starting from zStart and
	 * moving through to zEnd in the ImageStack.
	 * @param x
	 * @param y
	 * @return
	 */
	public int[] getPixelAndZ(int x, int y, int zStart, int zEnd) {
		ImageStack is = thresholdImp.getStack();
		int pix = 0;
		int z = 0;
		if( x < thresholdImp.getProcessor().getWidth() && y < thresholdImp.getProcessor().getHeight() ) {
			
			for(int a=zStart; a<=zEnd; a++) {
				pix = is.getProcessor(a).get(x, y);
				if(pix > 0) {
					//if pix is greater than 0, break out of this for loop -> have found a pixel!
					//First, store this Z slice coordinate:
					z = a;
					break;
				}
			}
			// If the user clicked on an object in the image, the pix value will be greater than 0, and z will be the
				//z slice of this coordinate, and the offscreenX/Y ints will be its XY coordinate.
		}
		return new int[] { pix, z };
	}
	
	/**
	 * Remove all thresholded objects from thresholdImgInt/thresholdImp which touch any image edge, if
	 * remove is true.
	 * @param remove
	 */
	public void removeEdgeObjects(boolean remove) {
		if(remove) {
			removeEdgeObjects();
		}
	}
	
	/**
	 * Remove all thresholded objects from thresholdImgInt/thresholdImp which touch any image edge.
	 */
	public void removeEdgeObjects() {
		
		IJ.showStatus("Object Manager: Assessing Objects - removing edge obj.");

		//loop through the thresholdImgInt image:
		for(int x=0; x<thresholdImgInt.sizeX; x++) {
			IJ.showProgress( x, thresholdImgInt.sizeX );
			for(int y=0; y<thresholdImgInt.sizeY; y++) {
				for(int z=0; z<thresholdImgInt.sizeZ; z++) {

					//DELETE TOP:
					if( (z==0) && (thresholdImgInt.getPixelInt(x, y, z) > 0) ) {
						Flood3D.flood3d26(thresholdImgInt, x, y, z, 0);
					}

					//DELETE BOTTOM:
					if( (z==(thresholdImgInt.sizeZ-1)) && (thresholdImgInt.getPixelInt(x, y, z) > 0) ) {
						Flood3D.flood3d26(thresholdImgInt, x, y, z, 0);
					}

					//DELETE LEFT:
					if( (x==0) && (thresholdImgInt.getPixel(x,y,z) > 0) ) {
						Flood3D.flood3d26(thresholdImgInt, x, y, z, 0);
					}

					//DELETE RIGHT:
					if( (x==(thresholdImgInt.sizeX-1) ) && (thresholdImgInt.getPixel(x,y,z) > 0) ) {
						Flood3D.flood3d26(thresholdImgInt, x, y, z, 0);
					}

					//DELETE HIGH:
					if( (y==0) && (thresholdImgInt.getPixel(x,y,z) > 0) ) {
						Flood3D.flood3d26(thresholdImgInt, x, y, z, 0);
					}

					//DELETE LOW:
					if( (y==(thresholdImgInt.sizeY-1) ) && (thresholdImgInt.getPixel(x,y,z) > 0) ) {
						Flood3D.flood3d26(thresholdImgInt, x, y, z, 0);
					}

				} // end z	
			} // end y
		} //end x
		
	}
	
	/**
	 * Extracts the data for all objects in thresholdImgInt.  Loops through all pixels in the thresholdImgInt,
	 * and extracts the object data based on every object identified in the image.  This data is saved into
	 * the passed datasetHandler object.
	 */
	public void extractObjData2(DatasetWrapper datasetHandler, int readPixelValue, int setPixelValue, boolean convexMeasures) {

		// make sure the arff data object is EMPTY:
		datasetHandler.delete();

		IJ.showStatus("Object Manager: Assessing Objects - gathering data");

		// setup loop through thresholdImgInt in imageHandler:
		setupLoopThresholdImgInt(0, 0, 0, 0);

		// using a while loop, extract all data from each object in thresholdImgInt:
		while(true) {
			ObjectDataContainer dataObj = assessThresholdImgIntObj(readPixelValue, setPixelValue, convexMeasures); 
				// returns null when end of image reached.
			if(dataObj == null) {
				break;
			}
			//Put the data from dataObj into the arff data structure:
			datasetHandler.addData( dataObj.returnData(), dataObj.returnDataTitles() );
			IJ.showStatus("Object Manager: Assessing Objects - gathering data");
		}

		// showArffDatatable(arff);
		// IJ.showMessage("New Arff Data Table for New Image.");

		// save the images' ARFF Dataset - MINUS the Filter and Classifier Columns:
		///datasetHandler.saveData(arffPath, 
			//	ObjectDataContainer.returnObjectMeasuresAndManClassAttributes() ); //this method uses the arffPath to save data to correct output.

	}
	
	public void setupLoopThresholdImgInt(int xStart, int yStart, int zStart, int objCounter) {
		x = this.xStart = xStart;
		y = this.yStart = yStart;
		z = this.zStart = zStart;
		
		this.objCounter = objCounter;
		
	}
	
	/**
	 * Loops through the thresholdImgInt, starting at xStart, yStart, zStart.  If an object is found,
	 * returns an ObjectDataContainer object holding all measured attributes. If no object is found,
	 * this method returns NULL.
	 * @param readPixVal
	 * @param setPixVal
	 * @return
	 */
	public ObjectDataContainer assessThresholdImgIntObj(int readPixVal, int setPixVal, boolean convexMeasures) {
		
		while( returnNextCoord() ) {
			
			if(thresholdImgInt.getPixelInt(x, y, z) == readPixVal) {
			
				// IJ.showMessage("xyz: "+x+" "+y+" "+z+" is 255.");

				//if the pixel is 255, want to map the object & get the attributes data from it:

				// Add 1 to objCounter, to count this obj:
				objCounter = objCounter + 1;
				
				// set xStart, yStart, zStart to x, y, z:
				// xStart = x;		yStart = y; 	zStart = z;

				//Analyse obj -> also pass object counter:
				//Method will set the obj voxels to the LAST value specified - here, to MuFpCn 
				// (Manual-unclassified, Filter-nonPassed, Classifier-nonFeature)
				//Data is returned as a MCIB_SM_DataObj object:
				return borderObjPixProcessing3D.objAssessment3d( thresholdImgInt, activeChannelInt, x, y, z, 
						objCounter, setPixVal, convexMeasures );
				
			}
			
		}
		
		return null;
		
	}
	
	public boolean returnNextCoord() {
		
		z = z+1;
		
		if(z >= thresholdImgInt.sizeZ) {
			z=0;
			y = y+1;
			if(y >= thresholdImgInt.sizeY) {
				y = 0;
				x = x + 1;
				if(x >= thresholdImgInt.sizeX) {
					return false;
				}
			}
		}
		
		return true;
		
	}
	
	/**
	 * Fills any object located at pixel X,Y,Z to setPixVal.  Programmer must provide the
	 * current pixel value (readPixVal) as a fail-safe.
	 * @param readPixVal
	 * @param setPixVal
	 * @param x
	 * @param y
	 * @param z
	 */
	public void fillObject(int readPixVal, int setPixVal, int x, int y, int z) {
		
		if(thresholdImgInt.getPixelInt(x, y, z) == readPixVal) {

			//ObjectVoxelProcessing.flood3d26( thresholdImgInt, x, y, z, setPixVal );
			borderObjPixProcessing3D.flood3d( thresholdImgInt, x, y, z, setPixVal );
			
		}
	}
	
	public void fillThresholdImgIntObj(int readPixVal, int setPixVal) {
		
		IJ.showStatus("Object Manager: Labelling Objects");
		
		//loop through the res image:
		for(int x=0; x<thresholdImgInt.sizeX; x++) {
			IJ.showProgress( x, thresholdImgInt.sizeX );
			for(int y=0; y<thresholdImgInt.sizeY; y++) {
				for(int z=0; z<thresholdImgInt.sizeZ; z++) {

					if(thresholdImgInt.getPixelInt(x, y, z) == readPixVal) {

						//if the pixel is 255, want to map the object & get the attributes data from it:

						// Add 1 to objCounter, to count this obj:
						//objCounter = objCounter + 1;

						//Analyse obj -> also pass object counter:
						//Method will set the obj voxels to the LAST value specified - here, to MuFpCn 
						// (Manual-unclassified, Filter-nonPassed, Classifier-nonFeature)
						//Data is returned as a MCIB_SM_DataObj object:
						//dataObj = borderObjPixProcessing3D.objAssessment3d26( thresholdImgInt, imgInt, x, y, z, 
						//													objCounter, MuFpCn );
						// FirstPixel firstPixelObj = borderObjPixProcessing3D.selectObj3d26( thresholdImgInt, x, y, z, MuFpCn );


						// Not all objects will be manually UNCLASSIFIED
						// and not all objects will be unfiltered or have no Classifier applied.
						// However, getting all the objects to the standard pixel value MuFpCn allows each
						// objects pixel value to be modified effectively in setPixVals() below:
						// ObjectVoxelProcessing.flood3d26( thresholdImgInt, x, y, z, setPixVal );
						borderObjPixProcessing3D.flood3d( thresholdImgInt, x, y, z, setPixVal );

						// IJ.showStatus("Object Manager: Labelling Objects");


						// And finally, put the objCounter number & the firstPixel reference into the HashMap:
						// This allows fast recall of the obj index in arff (which is the objNumber!)
						// The pixel is based on the FIRST VOXEL -> this is searched using a static method in
						// MCIB_SM_BorderObjPixCount3D -> getFirstVoxel(Object3DVoxels)

						//ArrayList<Integer> list = new ArrayList<Integer>();
						//list.add(firstPixelObj.x);
						//list.add(firstPixelObj.y);
						//list.add(firstPixelObj.z);

						//firstPixObjNoMap.put(list, objCounter);

						// This is now done in loadArffData() method!


					} // end if obj pix is 255

				} // end z	
			} // end y
		} //end x
		
	}
	
	/**
	 * Isolates the ROI from the originalImgInt, returning a new ImageInt with the width
	 * and height of the ROI, and the depth of the original ImgInt..  Inside the new image, 
	 * all the pixels from the originalImgInt which are inside the ROI are copied over.
	 * @param originalImgInt
	 * @param roi
	 * @return
	 */
	public ImageInt isolateRoi(ImageInt originalImgInt, Roi roi) {
		
		// For ExclRoi Pixels - Get the mask and the bounds of this ROI:
		ImageProcessor mask = roi.getMask();
		Rectangle r = roi.getBounds();

		// plus 2 to add 1 pixel border:
		ImageInt imgInt = new ImageByte("roi", r.width, r.height, originalImgInt.sizeZ);
		
		for(int z=0; z<originalImgInt.sizeZ; z++) {
			
			for(int y=0; y<mask.getHeight(); y++) {
				
				for(int x=0; x<mask.getWidth(); x++) {
			
					if(mask.getPixel(x, y) > 0) {
						
						int pixel = originalImgInt.getPixelInt(x+r.x, y+r.y, z);
						
						if(pixel > 0) {
																				
							imgInt.setPixel(x, y, z, 255);
						
						}
						
					}
					
				}
			}
		}
		
		return imgInt;
		
	}
	
	/**
	 * Isolates the ROI from the originalImgInt, returning a new ImageInt with the width
	 * and height of the ROI, and the depth of the original ImgInt, plus a blank 1 pixel
	 * border all around.  Inside the new image, all the pixels from the originalImgInt
	 * which are inside the ROI are copied over.
	 * @param originalImgInt
	 * @param roi
	 * @return
	 */
	public ImageInt isolateRoiWithBorder(ImageInt originalImgInt, Roi roi) {
		
		// For ExclRoi Pixels - Get the mask and the bounds of this ROI:
		ImageProcessor mask = roi.getMask();
		Rectangle r = roi.getBounds();

		// plus 2 to add 1 pixel border:
		ImageInt imgInt = new ImageByte("roi", r.width+2, r.height+2, originalImgInt.sizeZ+2);
		
		for(int z=0; z<originalImgInt.sizeZ; z++) {
			
			for(int y=0; y<mask.getHeight(); y++) {
				
				for(int x=0; x<mask.getWidth(); x++) {
			
					if(mask.getPixel(x, y) > 0) {
						
						int pixel = originalImgInt.getPixelInt(x+r.x, y+r.y, z);
						
						if(pixel > 0) {
																				
							imgInt.setPixel(x+1, y+1, z+1, 255);
						
						}
						
					}
					
				}
			}
		}
		
		return imgInt;
		
	}

	/**
	 * 
	 * @param pts
	 * @param sizeZ
	 * @param inVal
	 * @param outVal
	 * @param maxVal
	 * @param roi
	 */
	public void processBorderPoints(Points pts, int sizeZ, int inVal, int outVal, 
																int maxVal, Roi roi) {
		
		for(int z = 0; z < sizeZ; z++) {
			for(int p=0; p< pts.size(); p++) {	
				
				int x = pts.get(p).x;			
				int y = pts.get(p).y;

				//only process pixels with pixelValue of maxVal:
				if(  thresholdImgInt.getPixelInt( x, y, z ) == maxVal) {

					// Label obj depending whether it is in or out of the current ROI:
					borderObjPixProcessing3D.borderObjFilter3d( thresholdImgInt, x, y, z, roi, inVal, outVal );

				} //end if pixelValue > 0
			} //end p
		} //end z
		
	}
	
	/**
	 * This method will assess every object in the passed roiExcl, down to zMax Z depth, and extract
	 * the data to put into roiData.  The method returns the number of voxels in the passed ROI which
	 * have been assessed (essentially the number of pixels in roiExcl multiplied by zMax).
	 * @param roiExcl
	 * @param inVal
	 * @param zMax
	 * @param firstPixObjNoMap
	 * @param datasetHandler
	 * @param roiData
	 * @return The number of pixels inside the 3D ROI assessed in this method (roiExcl x zMax).
	 */
	public int processRoiObjects(Roi roiExcl, int inVal, int objVal, int zMax, 
									ObjectDatasetMap firstPixObjNoMap, DatasetWrapper datasetHandler, 
									DatasetWrapper roiData, String ClassifierAttribute) {

		// For ExclRoi Pixels - Get the mask and the bounds of this ROI:
		ImageProcessor mask = roiExcl.getMask();	
		Rectangle r = roiExcl.getBounds();

		// Int for ROI size (in pixels):		Int for obj counter:
		int roiSize = 0;						int objCounter = 0;

		for(int z = 0; z <  zMax; z++) {
			for(int y=0; y<mask.getHeight(); y++) {
				for(int x=0; x<mask.getWidth(); x++) {

					if(mask.getPixel(x, y) > 0) {

						roiSize = roiSize + 1;

						if( thresholdImgInt.getPixelInt(x+r.x,y+r.y,z) == inVal
								|| thresholdImgInt.getPixelInt(x+r.x,y+r.y,z) == objVal  ) {
							
							objCounter = objCounter + 1;
																				
							ObjectDataContainer objData =
									borderObjPixProcessing3D.objAssessment3d( thresholdImgInt, activeChannelInt, 
											x+r.x, y+r.y, z, objCounter, 0, true );  //true, running convex measures!
							
							roiData.addData( objData.returnData(), objData.returnDataTitles() );
							
							//IJ.log("objCounter: "+objCounter);
							
							// add ClassifierAttribute to the Classifier Column:
							// need to set Classifier Attribute value across the whole datset to ClassifierAttribute
							// By default it will be UNCLASSIFIED, but need to set to ClassifierAttribute:
							//roiData.instance((objCounter-1)).setValue(
									
								//	ObjectDataContainer.returnAttribute(ObjectDataContainer.CLASSIFIERCLASS), 
										
								//	(double)ObjectDataContainer.getValueIndex(
								//							ObjectDataContainer.CLASSIFIERCLASS, 
								//							ClassifierAttribute )
								//	);
														
							roiData.setValue( 
									(roiData.data.size()-1), // -1 as obj ref is -1 from obj no 
									(double)ObjectDataContainer.getValueIndex(ObjectDataContainer.CLASSIFIERCLASS, ClassifierAttribute),
									ObjectDataContainer.CLASSIFIERCLASS,
									false
									);

						}
					}
				}
			}
		}

		return roiSize;

	}
	
	/**
	 * This method assesses all object fragments which sit within the passed roi.  This is achieved by first
	 * adjusting all pixels in the ROI with a pixel value above 0, to its value +1.  This ensures any fragments,
	 * which may have parts running into other ROIs, can be effectively isolated from the remaining parts
	 * of the object fragment.
	 * <p>
	 * Next, the whole ROI is assessed for object fragments by looking for any pixels which are (inVal + 1), or
	 * (maxVal + 1) - once any pixels match this criteria, the object they are part of is fully reconstructed
	 * and analysed.
	 * <p>
	 * The data from each object fragment is put into roiData, and this method returns the ROI pixel volume that
	 * has been assessed in this method. 
	 * 
	 * @param roi
	 * @param inVal
	 * @param zMax
	 * @param firstPixObjNoMap
	 * @param datasetHandler
	 * @param roiData
	 * @param convexMeasures
	 * @return Roi Pixel Volume which was assessed in this method.
	 */
	public int processRoiObjectsFragments(Roi roi, int objVal, int zMax, 
											DatasetWrapper roiData, String ClassifierAttribute) {
		
		// adjust all pixel values in ROI above 0 by adding 1 to them:
		int roiSize = labelRoi(thresholdImgInt, roi, objVal, zMax);
		
		// assess each object in ROI which has pixel value (objVal+1) - labelRoi set each objVal to objVal+1
		assessRoiObj(thresholdImgInt, roi, (objVal+1), 0, zMax, roiData, ClassifierAttribute, false);

		return roiSize;

	}
	
	/**
	 * Takes every pixel value above 0 in originalImgInt that is inside roi, and raises its value
	 * by 1.  Note, if the pixel value is 255, it is NOT CHANGED!
	 * <p>
	 * Background pixels which were 0 in the image, are now 1 in the ROI, and any labelled
	 * object will be a pixel value not equal to 1.
	 * <p>
	 * Returns the size of the ROI.
	 * @param originalImgInt
	 * @param roi
	 */
	public int labelRoi(ImageInt originalImgInt, Roi roi, int objVal, int zMax) {
		
		// For ExclRoi Pixels - Get the mask and the bounds of this ROI:
		ImageProcessor mask = roi.getMask();
		Rectangle r = roi.getBounds();
		
		int roiSize = 0;
		
		int objValPlusOne = objVal + 1;
		
		for(int z=0; z<zMax; z++) {
			for(int y=0; y<mask.getHeight(); y++) {
				for(int x=0; x<mask.getWidth(); x++) {
					
					if(mask.getPixel(x, y) > 0) {

						roiSize = roiSize + 1;
						
						int pixelVal = originalImgInt.getPixelInt(x+r.x, y+r.y, z);
						
						if(pixelVal == objVal ) {

							originalImgInt.setPixel(x+r.x, y+r.y, z, objValPlusOne);
							
						}
					}
				}
			}
		}
		
		return roiSize;
	}
	
	/**
	 * Loops through the roi on thresholdImgInt.  If an object is found (pixel above 0),
	 * the object is assessed and its data put into roiData.
	 * <p>
	 * The object is defined by containing a pixel value OTHER THAN setPixVal, and the object is set to
	 * be setPixVal.
	 * 
	 * @param readPixVal
	 * @param setPixVal
	 * @return
	 */
	public void assessRoiObj(ImageInt originalImgInt, Roi roi, int objVal, int setPixVal, int zMax, 
			DatasetWrapper roiData, String ClassifierAttribute, boolean convexMeasures) {

		// For ExclRoi Pixels - Get the mask and the bounds of this ROI:
		ImageProcessor mask = roi.getMask();
		Rectangle r = roi.getBounds();
		
		// Int for obj counter:
		int objCounter = 0;

		for(int z=0; z<zMax; z++) {
			for(int y=0; y<mask.getHeight(); y++) {
				for(int x=0; x<mask.getWidth(); x++) {
					
					if(mask.getPixel(x, y) > 0) {

						if( originalImgInt.getPixelInt(x+r.x, y+r.y, z) == objVal ) {

							objCounter = objCounter + 1;

							ObjectDataContainer dataObj = 
									borderObjPixProcessing3D.objAssessment3d( thresholdImgInt, activeChannelInt, 
											x+r.x, y+r.y, z, objCounter, setPixVal, convexMeasures );

							roiData.addData( dataObj.returnData(), dataObj.returnDataTitles() );
							
							// add ClassifierAttribute to the Classifier Column:
							// need to set Classifier Attribute value across the whole datset to ClassifierAttribute
							// By default it will be UNCLASSIFIED, but need to set to ClassifierAttribute:
							//roiData.instance((objCounter-1)).setValue(
									
								//	ObjectDataContainer.returnAttribute(ObjectDataContainer.CLASSIFIERCLASS), 
										
								//	(double)ObjectDataContainer.getValueIndex(
								//							ObjectDataContainer.CLASSIFIERCLASS, 
								//							ClassifierAttribute )
								//	);
														
							roiData.setValue( 
									(roiData.data.size()-1), // -1 as obj ref is -1 from obj no 
									(double)ObjectDataContainer.getValueIndex(ObjectDataContainer.CLASSIFIERCLASS, ClassifierAttribute),
									ObjectDataContainer.CLASSIFIERCLASS,
									false
									);
							
						} // end if originalImgInt
					} //end if mask
					
				}// end for x
			} // end for y
		} // end for z
	}
	
	
	public void setBorderPoints(Points pts, int sizeZ, int outVal, int maxVal) {

		int pixelValue;
		
		//loop through all of z slices:
		for(int z = 0; z < sizeZ; z++) {

			//loop through borderPoints of current ROI:
			for(int p=0; p<pts.size(); p++) {

				int x = pts.get(p).x;			int y = pts.get(p).y;

				//First, get pixel value for the current borderPoints pixel from res image:
				pixelValue =  thresholdImgInt.getPixelInt(x, y, z);

				// if object pixel value is outVal, set it back to maxVal:
				if(  pixelValue == outVal ) {

					// convert the objects pixel value back to maxVal:
					borderObjPixProcessing3D.selectObj3d(thresholdImgInt, x, y, z, maxVal, maxVal);

				} //end if pixelValue == outVal
			} //end p
		} //end z
		
	}

}
