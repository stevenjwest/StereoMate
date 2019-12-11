package stereomate.data;

import java.util.ArrayList;
import java.util.Collections;

import stereomate.settings.OM_ProcedureSettings;

/**
 * 
 * 
 * 
 * @author stevenwest
 *
 */
public class MaximumDimensions {
	
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
	int maxX, maxY, maxZ;
	
	/**
	 * These ArrayLists keep track of the all object dimensions measures which fall in the top 5% for each image
	 * assessed.  Once an image has been assessed (once it has an ARFF dataset saved to output), then these
	 * arrays will NOT be filled AGAIN with the same data.  These will accumulate all data from all assessed
	 * images to compute the mean 95-100% dimension sizes for objects.
	 */
	ArrayList<Double> xDimArrayTotal, yDimArrayTotal, zDimArrayTotal;
	
	
	public MaximumDimensions(OM_ProcedureSettings om_ProcedureSettings) {
		// Initialise maxX, maxY and maxZ to 0:
		// these will store the maximum dimensions of objects (mean of top 95% objects) to store in 
		// OM_ProcedureSettings.xml file.

		maxX = 0;
		maxY = 0;
		maxZ = 0;

		// initialise the TOTAL arrays which hold all 95-100% largest dimension sizes for objects:

		xDimArrayTotal = new ArrayList<Double>();
		yDimArrayTotal = new ArrayList<Double>();
		zDimArrayTotal = new ArrayList<Double>();
				
	}
	
	public int getMaxX() {
		return maxX;
	}
	
	public int getMaxY() {
		return maxY;
	}
	
	public int getMaxZ() {
		return maxZ;
	}
	
	/**
	 * Calculates the mean of the top 95% biggest dimensions in all passed arrays, and sets this value to
	 * the instance variables maxX maxY and maxZ if they are larger than the current maxX, maxY and maxZ.
	 * @param xDimArray
	 * @param yDimArray
	 * @param zDimArray
	 */
	public void calculateDimMax(ArrayList<Double> xDimArray, ArrayList<Double> yDimArray, ArrayList<Double> zDimArray) {
		
		// first - calculate the max object dimensions in X Y Z:
		maxX = calcDimMax(xDimArray, maxX, xDimArrayTotal);
		
		maxY = calcDimMax(yDimArray, maxY, yDimArrayTotal);
		
		maxZ = calcDimMax(zDimArray, maxZ, zDimArrayTotal);
				
	}
	
	/**
	 * Calculates the top 95% biggest integers in dimArray and computes their mean value, and stores this into
	 * dimMax if the mean is larger than dimMax.
	 * @param DimArray
	 * @param dimMax
	 */
	public int calcDimMax(ArrayList<Double> dimArray, int dimMax, ArrayList<Double> dimArrayTotal) {
		
		// sort ArrayList:
		Collections.sort(dimArray);
				
		// calculate the number of integers which are in top 95% (this is the size / 20).
		int number = (Math.round( dimArray.size() / 20) );
		
		if(number == 0 ) {
			number = 1; // if number is 0, set it to 1 by default [so, just pick the one largest object]
		}
				
		// get the top 95% values -> from [dimArray.size] down to [dimArray.size - number]:
			// add to total array:
		for(int a=dimArray.size()-1; a>=dimArray.size()-number; a--) {
			//sum = sum + dimArray.get(a);
			dimArrayTotal.add( dimArray.get(a) );
		}
		
		// to get the total number of objects in dimArrayTotal, set number to dimArrayTotal size:
		number = dimArrayTotal.size();
		
		// compute sum of all numbers in dimArrayTotal:
		double sum = 0.0;
		
		for(int a=0; a<number; a++) {
			sum = sum + dimArrayTotal.get(a);
		}
		
		//compute the MEAN of these values:
		int mean = (int)Math.round( sum / number );
				
		//if mean is bigger than dimMax, set mean to dimMax:
		
		//if(mean > dimMax) {
			//dimMax = mean;
			//IJ.showMessage("dimMax: "+dimMax);
			//IJ.showMessage("maxX maxY maxZ: "+maxX+" "+maxY+" "+maxZ);
		//}
		
		// Actually should ALWAYS return the mean - as more images are assessed, the dimMax becomes more ACCURATE:
		dimMax = mean;
		
		return dimMax;
		
	}
	
	
}
