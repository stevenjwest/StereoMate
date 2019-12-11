package stereomate.object;

import ij.IJ;
import stereomate.data.DatasetWrapper;
import stereomate.data.ObjectDataContainer;
import stereomate.image.ImageHandler;
import weka.core.Instance;
import weka.core.Instances;

/**
 * 
 * This class represents a simple high- and low- pass Object Filter.  Objects are filtered based on
 * their value in a given attribute in an ObjectDatasetHandler object.  This class stores the
 * filtered status of each Instance in the ObjectDatasetHandler data object, which can also be set
 * to an appropriate Attribute in the ObjectDatasetHandler data.
 * 
 * @author stevenwest
 *
 */
public class ObjectFilter {

	DatasetWrapper datasetHandler;
	
	ImageHandler imageHandler;
	
	ObjectIdentifier objIdentifier;
	
	/**
	 * Stores the attribute index upon which the filtering is taking place.
	 */
	int attributeIndex;
	
	/**
	 * The actual filterMin and filterMax values - what values are being used as high and low
	 * pass filters.
	 */
	public double filterMax;

	public double filterMin;
	
	/**
	 * Ints to store the index position in the current dataset where the filterMax and filterMin values are.
	 */
	int filterMaxIndex, filterMinIndex;
	
	/**
	 * filterMax/MinReached are true if the max/min value is currently set by the max/min index
	 * variable.
	 */
	public boolean filterMaxReached;

	boolean filterMinReached;
	
	/**
	 * True only while no dataset has been added, it is adjusted to false once one dataset has been added
	 * to this ObjectFilter.
	 */
	//boolean firstDataset;
	
	/**
	 * The constructor captures ObjectIdentifier object. Use addDatasetAndImage to add the images.  The
	 * min/max values are only set with the FIRST import of dataset and image, after this the ObjectFilter
	 * is setup, and with any new data/images the ObjectFilter will continue to use the old settings.
	 * <p>
	 * This constructor will set the instance variable firstDataset to TRUE - this is turned to FALSE by
	 * the addDatasetAndImage() method.
	 * @param datasetHandler
	 */
	public ObjectFilter(ObjectIdentifier objIdentifier) {
		
		this.objIdentifier = objIdentifier;
		
		// set attributeIndex to -1 to indicate its INACTIVE:
		attributeIndex = -1;
	}
	
	/**
	 * Will add references to the datasetHandler & imageHandler.  The filterMaxIndex, filterMinIndex, 
	 * filterMaxReached, filterMinReached are all set to default values in this method IF this is the
	 * first time this method is called.  This is tracked with a boolean Instance Variable, set to TRUE
	 * in the constructor, but set to FALSE when this method is run.  The min/max values are ONLY INITIALISED
	 * if this boolean, firstDataset, is TRUE.
	 * @param datasetHandler
	 * @param imageHandler
	 */
	public void addDatasetAndImage(DatasetWrapper datasetHandler, ImageHandler imageHandler) {
		
		// set dataset and image:
		this.datasetHandler = datasetHandler;
		this.imageHandler = imageHandler;
		
		// if(firstDataset == true) {
			// if this is the first dataset, need to set the indexes and booleans:
		initialiseMinMaxVals();
			// firstDataset = false;
		
	}
	
	public int getAttributeIndex() {
		return attributeIndex;
	}
	
	public void setAttributeIndex(int index) {
		attributeIndex = index;
	}
	
	//public boolean getFirstDataset() {
		//return firstDataset;
	//}
	
	//public void setFirstDataset(boolean b) {
		//firstDataset = b;
	//}
	
	/**
	 * Sets the filterMaxReached instance variable to the passed boolean.
	 * @param maxReached
	 */
	public void setFilterMaxReached(boolean maxReached) {
		filterMaxReached = maxReached;
	}
	
	/**
	 * Sets the filterMinReached instance variable to the passed boolean.
	 * @param maxReached
	 */
	public void setFilterMinReached(boolean maxReached) {
		filterMinReached = maxReached;
	}
	
	/**
	 * Get the filterMaxReached boolean (determines whether the filterMax value is the maximum possible
	 * for the current dataset, or not.
	 * @return
	 */
	public boolean getFilterMaxReached() {
		return filterMaxReached;
	}
	
	/**
	 * Get the filterMinReached boolean (determines whether the filterMin value is the minimum possible
	 * for the current dataset, or not.
	 * @return
	 */
	public boolean getFilterMinReached() {
		return filterMinReached;
	}
	
	public void initialiseMinMaxVals() {
		filterMaxIndex = datasetHandler.size();
		filterMinIndex = 0;

		// set filterMaxIndex & filterMinIndex based on the logic comparison to the datasetHandler size:
		setFilterMaxReached( filterMaxIndex == datasetHandler.size() ); // true during initiation
		setFilterMinReached( filterMinIndex == 0 ); // true during initiation

		// filterMin & filterMax are NOT SET -> they may be 0.0, or actually set to the filter values!
	}
	
	/**
	 * NOTE: This method can put the image and datset out of sync!  Use with care!
	 * <p>
	 * Sets the instance variables to the parameters, then calls filterObjects().  The boolean
	 * setPixelValues will determine whether the image pixel values are also set/adjusted
	 * during the filtering.  By setting setPixelValues to FALSE, ONLY the dataset data is filtered
	 * (by adjusting the FILTERCLASS Attribute) and the image pixel values are NOT CHANGED.
	 * <p>
	 * This functionality is used during the SM Analyser to allow the filtering and then classification
	 * of objects in the dataset, before then filtering the objects to either a kept pixel value (224)
	 * or setting the object pixel values to 0 to remove it from the image and the analysis.
	 * <p>
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
	public void filterObjects( double min, double max, int attributeIndex, boolean setPixelValues ) {
		
		// set the filterMin and filterMax values to store the absolute filter values:
		filterMin = min;
		filterMax = max;
		// set the attributeIndex too:
		this.attributeIndex = attributeIndex;
		
		// then call the filterObjects() method:
		filterObjects(setPixelValues);
		
	}
	
	/**
	 * Sets the instance variables to the parameters, then calls filterObjects().
	 * <p>
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
		
		// set the filterMin and filterMax values to store the absolute filter values:
		filterMin = min;
		filterMax = max;
		// set the attributeIndex too:
		this.attributeIndex = attributeIndex;
		
		// then call the filterObjects() method:
		filterObjects(true);
		
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
	 */
	public void filterObjects(boolean setPixelValue) {
		
		// only filter if attributeIndex is set to a valid index (its set to -1 when the filter is inactive):
		if(attributeIndex >=0) {
			// sort the dataset by the attribute selected to filter with:
			datasetHandler.sort(attributeIndex);

			// starting at filterMinIndex, set up a for loop ASCENDING the arff dataset, and for every attribute value
			// which is BELOW the passed max value, set its filterAttribute to Not-Passed:
			applyMinFilter(setPixelValue);

			// starting at filterMaxIndex, set up a for loop DESCENDING the arff dataset, and for every attribute value
			// which is ABOVE the passed max value, set its filterAttribute to Not-Passed:
			applyMaxFilter(setPixelValue);

			// Finally, dont forget to re-sort arff on the objNo attribute! Not Needed now - arff is sorted by OBJNO
			// whenever an object to returned based on OBJNO.
			datasetHandler.sort( ObjectDataContainer.OBJNO );
		}
	
	}
	
	public void applyMinFilter(boolean setPixelValue) {
		
		// create a boolean value which will indicate if the arff should be traversed in other direction
		// this ensures this method will filter objects both as filterMin values go up AND down, and same for
		// filterMax
		boolean searchArffReverse = true;

		if(filterMinIndex == 0) {
			// if the min index is already 0, cannot search arff in reverse (down)!  So set to false:
			searchArffReverse = false;
		}

		//IJ.log("filterMinIndex: "+filterMinIndex);
		//IJ.log("");
		//IJ.log("Min Search UP - start: "+filterMinIndex);
		//IJ.log("");

		// starting at filterMinIndex, set up a for loop ASCENDING the arff dataset, and for every attribute value
		// which is BELOW the passed min value, set its filterAttribute to Not-Passed:
		for(int a=filterMinIndex; a<datasetHandler.size(); a++) {
			//if(arff.instance(a).value((filterComboBox.getSelectedIndex() - 1 )) < min) {
			if(datasetHandler.get(a, attributeIndex) < filterMin) {
				// if the value of the selected attribute is BELOW min, set the filterClass attribute to NOTPASSEDATR:
				// and adjust the pixel value of the object at this position:
				setFilterObj(ObjectDataContainer.FIL_NOTPASSED_INDEX, 
						datasetHandler.instance(a), 
						datasetHandler.getInstances(),
						setPixelValue );
				// and set searchArffReverse to false, as have now identified objects to filter in forward direction
				// through arff:
				searchArffReverse = false;
				//IJ.log("index:"+a+" value:"+datasetHandler.get(a, attributeIndex)+" min: "+min+" NOTPASSED");
			}
			else {
				filterMinIndex = a;
				//IJ.log("filterMinIndex set to: "+filterMinIndex+" value: "+datasetHandler.get(a, attributeIndex));
				break;
			}

			if(a == datasetHandler.size()-1) {
				filterMinIndex = datasetHandler.size();
				//IJ.log("filterMinIndex set to: "+filterMinIndex+" value: "+datasetHandler.get(a+1, attributeIndex));
			}
		}
		//IJ.log("");

		// if searchArffReverse is still true, then need to search FROM filterMinIndex-1 DOWN TO 0 in arff:
		// AND ADJUST filterClass to PASSED if the attribute value is greater or equal to min:
		if(searchArffReverse == true) {
			//IJ.log("Min Search DOWN - start: "+filterMinIndex);
			//IJ.log("");
			for(int a=filterMinIndex-1; a>=0; a--) {
				if(datasetHandler.instance(a).value(attributeIndex) >= filterMin) {
					if(a <= filterMaxIndex) {
						// if the value of the selected attribute is ABOVE min, set the filterClass attribute to PASSEDATR:
						// and adjust the pixel value of the object at this position:
						setFilterObj(ObjectDataContainer.FIL_PASSED_INDEX, 
								datasetHandler.instance(a), 
								datasetHandler.getInstances(),
								setPixelValue );
						//IJ.log("index:"+a+" value:"+datasetHandler.get(a, attributeIndex)+" min: "+min+" PASSED");
					}
				}
				else {
					filterMinIndex = a+1;
					//IJ.log("filterMinIndex set to: "+filterMinIndex+" value: "+datasetHandler.get(a+1, attributeIndex));
					break;
				}

				if(a == 0) {
					// need to add this clause at the end, as if the for loop reaches 0 and min is >= to the value,
					// the filterMinIndex is not set to the correct value!
					filterMinIndex = 0;
					//IJ.log("filterMinIndex set to: "+filterMinIndex+" value: "+datasetHandler.get(a, attributeIndex));
				}
			}
		}

		// set filterMaxIndex based on the logic comparison between filterMinIndex & the 0:
		setFilterMinReached( filterMinIndex == 0 );
	}
	
	
	public void applyMaxFilter(boolean setPixelValue) {

		// create a boolean value which will indicate if the arff should be traversed in other direction
		// this ensures this method will filter objects both as filterMin values go up AND down, and same for
		// filterMax
		boolean searchArffReverse = true;

		if( filterMaxIndex == datasetHandler.size() ) {
			// if the min index is already 0, cannot search arff in reverse!  So set to false:
			searchArffReverse = false;
		}

		// IJ.showMessage("filterMaxIndex: "+filterMaxIndex);

		// starting at filterMinIndex, set up a for loop ASCENDING the arff dataset, and for every attribute value
		// which is BELOW the passed min value, set its filterAttribute to Not-Passed:

		for(int a=filterMaxIndex-1; a>=0; a--) {
			if(datasetHandler.instance(a).value(attributeIndex) > filterMax) {
				// if the value of the selected attribute is BELOW min, set the filterClass attribute to NOTPASSEDATR:
				// and adjust the pixel value of the object at this position:
				setFilterObj(ObjectDataContainer.FIL_NOTPASSED_INDEX, 
						datasetHandler.instance(a), 
						datasetHandler.getInstances(),
						setPixelValue );
				// and set searchArffReverse to false, as have now identified objects to filter in forward direction
				// through arff:
				searchArffReverse = false;
			}
			else {
				filterMaxIndex = a+1;
				break;
			}
			if(a == 0) {
				// need to add this clause at the end, as if the for loop reaches 0 and max is > the value,
				// the filterMinIndex is not set to the correct value!
				filterMaxIndex = 0;
			}
		}

		// if searchArffReverse is still true, then need to search FROM filterMinIndex-1 DOWN TO 0 in arff:
		if(searchArffReverse == true) {
			for(int a=filterMaxIndex; a<datasetHandler.size(); a++) {
				if(datasetHandler.instance(a).value(attributeIndex) <= filterMax) {
					if(a >= filterMinIndex) {
						// if the value of the selected attribute is BELOW min, set the filterClass attribute to NOTPASSEDATR:
						// and adjust the pixel value of the object at this position:
						setFilterObj(ObjectDataContainer.FIL_PASSED_INDEX, 
								datasetHandler.instance(a), 
								datasetHandler.getInstances(),
								setPixelValue );
					}
				}
				else {
					filterMaxIndex = a;
					break;
				}

				if(a == datasetHandler.size()-1) {
					filterMaxIndex = datasetHandler.size();
				}
			}
		}

		// set filterMaxIndex based on the logic comparison between filterMaxIndex & the datasetHandler size:
		setFilterMaxReached( filterMaxIndex == datasetHandler.size() );
	}
	
	

	
	
	
	/**
	 * Removes filter from objects in the ARFF dataset by the selected attribute in filterComboBox, where objects
	 * with attribute value: min <= ATTR <= max  pass the filter, but objects with attribute values
	 * below min and above max are set to not passed.
	 * <p>
	 * The method only runs if the filterCheckBox is selected (and is also run WHEN the filterCheckBox is
	 * selected!).
	 * @param attrIndex The Attribute Index in ObjectDatasetHandler data which the filter should be removed from.
	 */
	public void removeFilterObjects() {
		
		// ObjectFilter class?
		
		if(attributeIndex >= 0) {

			datasetHandler.sort( (attributeIndex ) );


			// IJ.showMessage("filterMinIndex: "+filterMinIndex);

			// from 0 to filterMinIndex, set up a for loop ASCENDING the arff dataset, and for every object set 
			//its filterAttribute to Passed:
			for(int a=0; a<=filterMinIndex; a++) {
				// Set all objects from index 0 to filterMinIndex to the filterClass attribute PASSEDATR:
				// and adjust the pixel value of the object at this position:
				setFilterObj(ObjectDataContainer.FIL_PASSED_INDEX, 
									datasetHandler.instance(a), 
									datasetHandler.getInstances(),
									true );
			}

			// from filterMaxIndex to end of ARFF, set up a for loop ASCENDING the arff dataset, and for every object
			// set its filterAttribute to Passed:

			for(int a=filterMaxIndex-1; a<datasetHandler.size(); a++) {
				// Set all objects from index filterMaxIndex-1 to arff size to the filterClass attribute PASSEDATR:
				// and adjust the pixel value of the object at this position:
				setFilterObj(ObjectDataContainer.FIL_PASSED_INDEX, 
									datasetHandler.instance(a), 
									datasetHandler.getInstances(),
									true );

			}

			// IJ.showMessage("FilterMaxIndex post processing: "+filterMaxIndex);

			// Finally, dont forget to re-sort arff on the objNo attribute! Not Needed now - arff is sorted by OBJNO
				// whenever an object to returned based on OBJNO.
			datasetHandler.sort( ObjectDataContainer.OBJNO );
			
			// and dont forget to reset the filterMaxIndex and filterMinIndex variables!
			filterMaxIndex = datasetHandler.size();
			filterMinIndex = 0;
			
			// set filterMaxIndex & filterMinIndex based on the logic comparison with the datasetHandler size:
			setFilterMaxReached( filterMaxIndex == datasetHandler.size() );
			setFilterMinReached( filterMinIndex == 0 );
			
			// AND set the filterMin and filterMax to their default values -> 0.0:
			filterMin = 0.0;
			filterMax = 0.0;
			
			// AND set Instance Var attributeIndex to its default: -1
			this.attributeIndex = -1;

			// and repaint the IWP:
			// IWP.updateSlices(true);

		}
	
	}
	
	
	
	
	/**
	 * Removes filter from objects in the ARFF dataset by the selected attribute in filterComboBox, where objects
	 * with attribute value: min <= ATTR <= max  pass the filter, but objects with attribute values
	 * below min and above max are set to not passed.
	 * <p>
	 * The method only runs if the filterCheckBox is selected (and is also run WHEN the filterCheckBox is
	 * selected!).
	 * @param attrIndex The Attribute Index in ObjectDatasetHandler data which the filter should be removed from.
	 */
	public void removeFilterObjects(int attrIndex) {
		
		// ObjectFilter class?
		
		if(attrIndex >= 0) {

			datasetHandler.sort( (attrIndex ) );


			// IJ.showMessage("filterMinIndex: "+filterMinIndex);

			// from 0 to filterMinIndex, set up a for loop ASCENDING the arff dataset, and for every object set 
			//its filterAttribute to Passed:
			for(int a=0; a<=filterMinIndex; a++) {
				// Set all objects from index 0 to filterMinIndex to the filterClass attribute PASSEDATR:
				// and adjust the pixel value of the object at this position:
				setFilterObj(ObjectDataContainer.FIL_PASSED_INDEX, 
									datasetHandler.instance(a), 
									datasetHandler.getInstances(),
									true );
			}

			// from filterMaxIndex to end of ARFF, set up a for loop ASCENDING the arff dataset, and for every object
			// set its filterAttribute to Passed:

			for(int a=filterMaxIndex-1; a<datasetHandler.size(); a++) {
				// Set all objects from index filterMaxIndex-1 to arff size to the filterClass attribute PASSEDATR:
				// and adjust the pixel value of the object at this position:
				setFilterObj(ObjectDataContainer.FIL_PASSED_INDEX, 
									datasetHandler.instance(a), 
									datasetHandler.getInstances(),
									true );

			}

			// IJ.showMessage("FilterMaxIndex post processing: "+filterMaxIndex);

			// Finally, dont forget to re-sort arff on the objNo attribute! Not Needed now - arff is sorted by OBJNO
				// whenever an object to returned based on OBJNO.
			datasetHandler.sort( ObjectDataContainer.OBJNO );
			
			// and dont forget to reset the filterMaxIndex and filterMinIndex variables!
			filterMaxIndex = datasetHandler.size();
			filterMinIndex = 0;
			
			// set filterMaxIndex & filterMinIndex based on the logic comparison with the datasetHandler size:
			setFilterMaxReached( filterMaxIndex == datasetHandler.size() );
			setFilterMinReached( filterMinIndex == 0 );
			
			// AND set the filterMin and filterMax to their default values -> 0.0:
			filterMin = 0.0;
			filterMax = 0.0;
			
			// AND set Instance Var attributeIndex to its default: -1
			this.attributeIndex = -1;

			// and repaint the IWP:
			// IWP.updateSlices(true);

		}
	
	}


	/**
	 * Sets the instance to the new filter class as indicated in newFilterClass.  Adjusts the filterClass
	 * attribute to match what the newFilterClass flag indicates, and adjusts the objects pixel value
	 * in the image to indicate its new classification.
	 * @param newFilterClass Int Flag to represent the New filter class, must be FIL_PASSED_INDEX or
	 * FIL_NOTPASSED_INDEX.
	 * @param instance The instance of the object in the ARFF dataset, used to change the filterClass classification
	 * and to gather the firstPixel data on the object to adjust its pixel value in the image.
	 */
	public void setFilterObj(int newFilterValueIndex, Instance instance, Instances instances, boolean setPixelValue) {

		// get the firstPixel of this object & use it to adjust the pixel value by pixValAdj:

		//Retrieve First Pixel Co-ordinates:
		int x = (int)instance.value(instances.attribute(ObjectDataContainer.X1));  // x1
		int y = (int)instance.value(instances.attribute(ObjectDataContainer.Y1));  // y1
		int z = (int)instance.value(instances.attribute(ObjectDataContainer.Z1));  // z1


		// retrieve the title of the filter value from the newFilterValueIndex (which is the index of this
		// value in the Filter Class):
		String filterTitleValue = ObjectDataContainer.getValueTitleFromIndex(
				ObjectDataContainer.FILTERCLASS, 
				newFilterValueIndex
				);

		// adjust pixel value of obj in image to reflect the new filter class value:
			// only if setPixelValue is TRUE:
		if(setPixelValue == true) {
			imageHandler.setObjValue(
					x, y, z, 
					objIdentifier.returnObjectFlag(
							imageHandler.getPixelValueThresholded(x, y, z), 
							ObjectDataContainer.FILTERCLASS, 
							filterTitleValue
							)
					);
		}

		// adjust the filter attribute's value in datasetHandler appropriately
		instance.setValue(
				datasetHandler.attribute(ObjectDataContainer.FILTERCLASS), 
				filterTitleValue
				);

	}
	
	
}
