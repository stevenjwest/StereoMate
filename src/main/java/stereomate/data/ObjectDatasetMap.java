package stereomate.data;


import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JFrame;

import ij.IJ;
import weka.core.Instances;
import weka.gui.arffviewer.ArffPanel;

/**
 * This class represents a HashMap which can map between a list of Integer values as a key, to a single
 * Integer value as the value.  This is used in StereoMate to map between the FirstPixel co-ordinate 
 * (the X Y and Z integer values) of an object in the image, and the Object Number (which is its index 
 * [1-based] in the Weka Instances dataset derived from the image).
 * <p>
 * However, this could be used to map between any set of values to another.
 * 
 * @author stevenwest
 *
 */
public class ObjectDatasetMap {

	/**
	 * The hashmap - maps ArrayList<Integer> object keys to Integer values.
	 */
	protected HashMap<List<Integer>, Integer> firstPixObjNoMap;
	
	/**
	 * ArrayList<String> contains the Attribute titles in an Instances object that the 
	 * Integers are derived from to build the ArrayList<Integer> object HashMap key.
	 */
	protected ArrayList<String> keyTitles;
	
	/**
	 * String contains the Attribute title in an Instances object that the Integer
	 * is derived from for the HashMap value.
	 */
	protected String valueTitle;
	
	/**
	 * Default constructor, initialises the keyTitles variable (an ArrayList of titles
	 * which should match the Attribute titles of the variables in Instances which constitute
	 * the key), and the valueTitle variable (a String which should match the Attribute title
	 * of a variable in Instances which constitute the value).
	 */
	public ObjectDatasetMap(ArrayList<String> keyTitles, String valueTitle) {
		// firstPixObjNoMap = new HashMap<List<Integer>, Integer>();
		this.keyTitles = keyTitles;
		this.valueTitle = valueTitle;
	}
	
	public void setKeyTitles(ArrayList<String> keyTitles) {
		this.keyTitles = keyTitles;
	}
	
	public ArrayList<String> getKeyTitles() {
		return keyTitles;
	}
	
	/**
	 * Add the key:value pair to the HashMap.
	 * @param key
	 * @param value
	 * @return
	 */
	//public int put(List<Integer> key, int value) {
		//return firstPixObjNoMap.put(key, value);
	//}
	
	/**
	 * return the value of a given key.
	 * @param key
	 * @return
	 */
	public int get( List<Integer> key ) {
		return firstPixObjNoMap.get(key);
	}

	
	/**
	 * Builds the object dataset map from the data in ObjectDatasetHandler object.
	 * @param dataset
	 */
	public void buildMap(DatasetWrapper dataset) {
		buildMap(dataset.data);
	}
	
	/**
	 * Builds the object dataset map from an Instances object.
	 * @param data
	 */
	public void buildMap(Instances data) {

		// always re-generate the firstPixObjNoMap to ensure it is empty:
		firstPixObjNoMap = new HashMap<List<Integer>, Integer>();

		int objCounterInt = 0;
		
		// showArffDatatable(data);

		for(int a=0; a<data.size(); a++) {

			ArrayList<Integer> list = new ArrayList<Integer>();
			for(int b=0; b<keyTitles.size(); b++) {
				list.add(   (int)data.get(a).value(  data.attribute( keyTitles.get(b) )  )   );
			}
			
			// IJ.showMessage(" attr0: "+keyTitles.get(0)+" 1: "+keyTitles.get(1)+" 2: "+keyTitles.get(2)+
			// 		" value0: "+list.get(0)+" value1: "+list.get(1)+" value2: "+list.get(2) );

			objCounterInt = (int)data.get(a).value( data.attribute( valueTitle ) );
			
			// IJ.showMessage(" valueTitle: "+valueTitle+" value: "+objCounterInt );

			firstPixObjNoMap.put(list, objCounterInt);

		}

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
	
}
