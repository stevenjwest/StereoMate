package stereomate.object;

import java.util.ArrayList;

import stereomate.data.ObjectDataContainer;

/**
 * Class to represent the data of a Selected Object - including FIRST PIXEL of an object, its pixel value,
 * object classification value, whether an object is selected.
 * 
 * TODO -> Think about renaming this class - possibly to SelectedObjectContainer?
 * 
 * @author stevenwest
 *
 */
public class SelectedObject {
	
	/**
	 * Static references to the integers that represent the SELECTED PIXEL VALUES of an object.
	 */
	public static final int UNCLASSIFIED = 222, FEATURE = 221, NONFEATURE = 220, CONNECTED = 219;
	
	public static final int[] SELECTEDPIXVALS = new int[] { UNCLASSIFIED, FEATURE, NONFEATURE, CONNECTED };
	
	/**
	 * If the passed objVal is equal to any of the selected pixel values (as defined by SELECTEDPIXVALS),
	 * then this returns true, else this method returns false.
	 * @param objVal
	 * @return
	 */
	public static boolean isSelectedObjectValue(int objVal) {
		for(int a=0; a<SELECTEDPIXVALS.length; a++) {
			if(SELECTEDPIXVALS[a] == objVal) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Will return the appropriate selected pixel value based on the passed Manual Class attribute value
	 * string.  This manClassVal string must be equal to ObjectDataContainer. UNCLASSIFIEDATR, FEATUREATR,
	 * NONFEATUREATR, CONNECTEDATR.  Returns -1 if none of these Strings are found in manClassVal.
	 * @return
	 */
	public static int returnSelectedPixelValue(String manClassVal) {
		
		if( manClassVal.equals(ObjectDataContainer.UNCLASSIFIEDATR) ) {
			return UNCLASSIFIED;
		}
		else if( manClassVal.equals(ObjectDataContainer.FEATUREATR) ) {
			return FEATURE;
		}
		else if( manClassVal.equals(ObjectDataContainer.NONFEATUREATR) ) {
			return NONFEATURE;
		}
		else if( manClassVal.equals(ObjectDataContainer.CONNECTEDATR) ) {
			return CONNECTED;
		}
		return -1;
	}
	
	/**
	 * Coordinates of the first pixel: X, Y, Z.
	 */
	public int x, y, z;
	
	/**
	 * Boolean value to represent whether this object is currently selected.
	 */
	boolean objectSelected;
	
	/**
	 * Integer to represent the ACTUAL PIXEL VALUE of the currently selected object - this will be one of:
	 * SELECTED, CLASSIFIED_FEATURE_SELECTED, CLASSIFIED_NONFEATURE_SELECTED, or CLASSIFIED_CONNECTED_SELECTED.
	 * <p>
	 * This is used to assess what the Actual Selected Object Pixel Value is.
	 */
	int objSelectedPixVal;
	
	/**
	 * Integer to represent this objects pixel value when not selected.
	 */
	int objPixVal;
	
	public SelectedObject(int x, int y, int z, boolean objectSelected, int objSelectedPixVal, int objPixVal) {
		
		this.x = x;
		this.y = y;
		this.z = z;
		
		this.objectSelected = objectSelected;
		
		this.objSelectedPixVal = objSelectedPixVal;
		
		this.objPixVal = objPixVal;
	}
	
	/**
	 * Returns true if the current object is selected.
	 * @return
	 */
	public boolean isSelected() {
		return objectSelected;
	}
	
	/**
	 * Sets whether the current object is selected.
	 * @param selected
	 */
	public void setSelected(boolean selected) {
		objectSelected = selected;
	}
	
	/**
	 * Returns the current objects selected pixel value.
	 * @return
	 */
	public int getSelectedPixelValue() {
		return objSelectedPixVal;
	}
	
	/**
	 * Sets the current objects' selected pixel value.
	 * @param pixelValue
	 */
	public void setSelectedPixelValue(int pixelValue) {
		objSelectedPixVal = pixelValue;
	}
	
	/**
	 * Returns the current objects unselected pixel value.
	 * @return
	 */
	public int getUnselectedPixelValue() {
		return objPixVal;
	}
	
	/**
	 * Sets the current objects unselected pixel value.
	 * @return
	 */
	public void setUnselectedPixelValue(int pixelValue) {
		this.objPixVal = pixelValue;
	}
	
	public void setXYZ( int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public int getZ() {
		return z;
	}
	
	/**
	 * Return an ArrayList<Integer> of the XYZ coordinate of the FIRST PIXEL this Selected Object.
	 * @return
	 */
	public ArrayList<Integer> returnArrayListFirstPixCoord() {
		ArrayList<Integer> list = new ArrayList<Integer>();
		list.add(x);
		list.add(y);
		list.add(z);
		return list;
	}
	
}
