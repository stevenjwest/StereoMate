package stereomate.lut;

import java.awt.Color;
import java.util.ArrayList;

import ij.process.LUT;
import stereomate.data.ObjectDataContainer;
import stereomate.object.ObjectIdentifier;
import stereomate.object.SelectedObject;

/**
 * This class will build classification mode LUTs for Manual Classification, Filter Classification and
 * Classifier Classification of object.
 * <p>
 * In future, this will also represent custom user-made LUTs for visualising across classification modes.
 * <p>
 * This contains instance variables for each LUT, and the colours are passed into the constructor, along
 * with the ObjectIdentifier & String[][] Value Arrays for Manual Filter and Classifier classes, to allow
 * the LUTs to be set up correctly.
 * @author stevenwest
 *
 */
public class ClassificationModeLUTs {

	 /**
	  * Look Up Tables for the Three Display Modes:  Manual Classification Mode, Filter Classification Mode
	  * and Classifier Classification Mode.
	  */
	 public LUT manualLut;
	public LUT filterLut;
	public LUT classifierLut;
	LUT filterClassifierLut;
	 
	 /**
	  * Set each LUT according to the colours provided (which match in their arrays the pixel values in
	  * objIdentifier and selectedObj respectively).  
	  * @param colours
	  * @param objIdentifierInput
	  * @param objIdentifier
	  * @param coloursSelected
	  * @param selectedObj
	  */
	 public ClassificationModeLUTs(Color[][] colours, ObjectIdentifier objIdentifier, Color[] coloursSelected) {
		 
		 buildClassificationModeLUTs( colours, objIdentifier, coloursSelected);

	 }


	 /**
	  * This method will build the LUTs for each Classification Mode.  These are applied to the Thresholded Imp
	  * in IWP to update the colours of objects in the image to reflect their classification in the different
	  * classification modes: Manual, Filter, Classifier.
	  * 
	  */
	 public void buildClassificationModeLUTs(Color[][] colours, 
			 ObjectIdentifier objIdentifier, Color[] coloursSelected) {

		 //IJ.showMessage("Build Classification Mode LUTs...");

		 // Byte[] arrays for MANUAL CLASSIFICATION MODE:
		 // REMEMBER:  byte variables are SIGNED -> they run from -128 to +127
		 // Therefore, to encode from 0 to 255, need to -128 from the value and THEN assign it to a byte:
		 byte[] red = new byte[256];
		 byte[] green = new byte[256];
		 byte[] blue = new byte[256];
		 
		 // Make each classification type for Manual Classification Mode a unique color:
		 	// Manual Class is ref 0 in 1st dimension, each title is index 0, each value is index in order here 1-4:
		 modifyRGBArrays(colours[0][0], 
				 objIdentifier.returnFlagValues(ObjectDataContainer.MANUALCLASS, ObjectDataContainer.UNCLASSIFIEDATR), 
				 red, green, blue);
		 modifyRGBArrays(colours[0][1], 
				 objIdentifier.returnFlagValues(ObjectDataContainer.MANUALCLASS, ObjectDataContainer.FEATUREATR), 
				 red, green, blue);
		 modifyRGBArrays(colours[0][2], 
				 objIdentifier.returnFlagValues(ObjectDataContainer.MANUALCLASS, ObjectDataContainer.NONFEATUREATR), 
				 red, green, blue);
		 modifyRGBArrays(colours[0][3], 
				 objIdentifier.returnFlagValues(ObjectDataContainer.MANUALCLASS, ObjectDataContainer.CONNECTEDATR), 
				 red, green, blue);

		 // set all SELECTED pixel values to the manual unclassified selected colour:
		 modifyRGBValue(coloursSelected[0], SelectedObject.UNCLASSIFIED, red, green, blue);
		 modifyRGBValue(coloursSelected[1], SelectedObject.FEATURE, red, green, blue);
		 modifyRGBValue(coloursSelected[2], SelectedObject.NONFEATURE, red, green, blue);
		 modifyRGBValue(coloursSelected[3], SelectedObject.CONNECTED, red, green, blue);

		 //generate the LUT from the red green and blue byte[] arrays:
		 //manualLut = new LUT(red, green, blue);
		 manualLut = new LUT(8, 256, red, green, blue);


		 // Byte[] arrays for FILTER CLASSIFICATION MODE:
		 red = new byte[256];
		 green = new byte[256];
		 blue = new byte[256];

		 // Make each classification type for Filter Classification Mode a unique color:		
		 modifyRGBArrays(colours[1][0], 
				 objIdentifier.returnFlagValues(ObjectDataContainer.FILTERCLASS, ObjectDataContainer.PASSEDATR), 
				 red, green, blue);
		 modifyRGBArrays(colours[1][1], 
				 objIdentifier.returnFlagValues(ObjectDataContainer.FILTERCLASS, ObjectDataContainer.NOTPASSEDATR), 
				 red, green, blue);

		 // set all SELECTED pixel values to the manual unclassified selected colour:
		 modifyRGBValue(coloursSelected[0], SelectedObject.UNCLASSIFIED, red, green, blue);
		 modifyRGBValue(coloursSelected[1], SelectedObject.FEATURE, red, green, blue);
		 modifyRGBValue(coloursSelected[2], SelectedObject.NONFEATURE, red, green, blue);
		 modifyRGBValue(coloursSelected[3], SelectedObject.CONNECTED, red, green, blue);

		 //generate the LUT from the red green and blue byte[] arrays:
		 filterLut = new LUT(8, 256, red, green, blue);


		 // Byte[] arrays for CLASSIFIER CLASSIFICATION MODE:
		 red = new byte[256];
		 green = new byte[256];
		 blue = new byte[256];

		 // Make each classification type for Classifier Classification Mode a unique color:		
		 modifyRGBArrays(colours[2][0], 
				 objIdentifier.returnFlagValues(ObjectDataContainer.CLASSIFIERCLASS, ObjectDataContainer.FEATUREATR), 
				 red, green, blue);
		 modifyRGBArrays(colours[2][1], 
				 objIdentifier.returnFlagValues(ObjectDataContainer.CLASSIFIERCLASS, ObjectDataContainer.NONFEATUREATR), 
				 red, green, blue);
		 modifyRGBArrays(colours[2][2], 
				 objIdentifier.returnFlagValues(ObjectDataContainer.CLASSIFIERCLASS, ObjectDataContainer.CONNECTEDATR), 
				 red, green, blue);

		 // set all SELECTED pixel values to the manual unclassified selected colour:
		 modifyRGBValue(coloursSelected[0], SelectedObject.UNCLASSIFIED, red, green, blue);
		 modifyRGBValue(coloursSelected[1], SelectedObject.FEATURE, red, green, blue);
		 modifyRGBValue(coloursSelected[2], SelectedObject.NONFEATURE, red, green, blue);
		 modifyRGBValue(coloursSelected[3], SelectedObject.CONNECTED, red, green, blue);

		 //generate the LUT from the red green and blue byte[] arrays:
		 classifierLut = new LUT(8, 256, red, green, blue);
		 
		 
		 // Byte[] arrays for CLASSIFIER CLASSIFICATION MODE:
		 red = new byte[256];
		 green = new byte[256];
		 blue = new byte[256];

		 // Make each classification type for Filter-Classifier Classification Mode a unique color:		
		 modifyRGBArrays(colours[2][0], 
				 objIdentifier.returnFlagValues(ObjectDataContainer.CLASSIFIERCLASS, ObjectDataContainer.FEATUREATR), 
				 red, green, blue);
		 modifyRGBArrays(colours[2][1], 
				 objIdentifier.returnFlagValues(ObjectDataContainer.CLASSIFIERCLASS, ObjectDataContainer.NONFEATUREATR), 
				 red, green, blue);
		 modifyRGBArrays(colours[2][2], 
				 objIdentifier.returnFlagValues(ObjectDataContainer.CLASSIFIERCLASS, ObjectDataContainer.CONNECTEDATR), 
				 red, green, blue);
		 // then finally overwrite any colours set by Classifier colours on LUT values where the filter class would
		 	// set teh values to the NOTPASSED colour:
		 	// This will colour all objects that do not pass the filter the appropriate colour!
		 modifyRGBArrays(colours[1][1], 
				 objIdentifier.returnFlagValues(ObjectDataContainer.FILTERCLASS, ObjectDataContainer.NOTPASSEDATR), 
				 red, green, blue);

		 // set all SELECTED pixel values to the manual unclassified selected colour:
		 modifyRGBValue(coloursSelected[0], SelectedObject.UNCLASSIFIED, red, green, blue);
		 modifyRGBValue(coloursSelected[1], SelectedObject.FEATURE, red, green, blue);
		 modifyRGBValue(coloursSelected[2], SelectedObject.NONFEATURE, red, green, blue);
		 modifyRGBValue(coloursSelected[3], SelectedObject.CONNECTED, red, green, blue);

		 //generate the LUT from the red green and blue byte[] arrays:
		 filterClassifierLut = new LUT(8, 256, red, green, blue);



	 }


	 /**
	  * Modify the RGB arrays such that each pixel value in pixelValues is set to the colour color.
	  * @param color
	  * @param pixelValues
	  * @param red
	  * @param green
	  * @param blue
	  */
	 public void modifyRGBArrays(Color color, ArrayList<Integer> pixelValues, byte[] red, byte[] green, byte[] blue) {

		 for(int a=0; a<pixelValues.size(); a++) {
			 red[pixelValues.get(a)] = (byte)color.getRed();
			 green[pixelValues.get(a)] = (byte)color.getGreen();
			 blue[pixelValues.get(a)] = (byte)color.getBlue();
		 }

	 }


	 /**
	  * Modify the RGB arrays such that the pixel value in pixelValue is set to the colour color.
	  * @param color
	  * @param pixelValue
	  * @param red
	  * @param green
	  * @param blue
	  */
	 public void modifyRGBValue(Color color, Integer pixelValue, byte[] red, byte[] green, byte[] blue) {

		 red[pixelValue] = (byte)color.getRed();
		 green[pixelValue] = (byte)color.getGreen();
		 blue[pixelValue] = (byte)color.getBlue();
	 }


}
