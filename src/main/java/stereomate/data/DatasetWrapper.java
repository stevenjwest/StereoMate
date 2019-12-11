package stereomate.data;


import java.awt.BorderLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JFrame;

import ij.IJ;
import weka.classifiers.Classifier;
import weka.classifiers.lazy.IBk;
import weka.core.Attribute;
import weka.core.AttributeStats;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSink;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Add;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.instance.RemoveWithValues;
import weka.gui.arffviewer.ArffPanel;
import weka.gui.visualize.VisualizePanel;

/**
 *  This Class represents the Object Dataset - it holds a reference to the Instances data, and also
 *  an ArrayList containing all the attributes in this data object.  It also holds a reference to a
 *  Classifier which can be used on this data.
 *  
 * @author stevenwest
 *
 */
public class DatasetWrapper {
	
	/**
	 * An Instances Object - which holds the ARFF data object.
	 * TODO refactor name to 'data'
	 */
	public Instances data;
	
	/**
	 * ArrayList of all the Attributes added to the ARFF data object.
	 */
	ArrayList<Attribute> attributes;
	
	
	/**
	 * Initialise instance variables.  currentOutputFileName is the name of the output file, which is set
	 * as the name to the arff dataset.  The attributes object will contain all the Attributes for the
	 * Instances data object.  These Attributes can be retrieved from the ObjectDataContainer_MCIB class,
	 * for example the static method: ObjectDataContainer_MCIB..returnObjectMeasuresClassificationsAttributes().
	 */
	public DatasetWrapper(String currentOutputFileName, ArrayList<Attribute> attributes) {
		
		// set attributes:
		this.attributes = attributes;

		// generate new data object:
		data = new Instances(currentOutputFileName, attributes, 1000 );
		
	}
	
	/**
	 * Initialise instance variables.  currentOutputFileName is the name of the output file, which is set
	 * as the name to the arff dataset.  The attributes object will contain all the Attributes for the
	 * Instances data object.  These Attributes can be retrieved from the ObjectDataContainer_MCIB class,
	 * for example the static method: ObjectDataContainer_MCIB..returnObjectMeasuresClassificationsAttributes().
	 */
	public DatasetWrapper(String currentOutputFileName, Instances instances) {
		
		// set attributes:
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for(int a=0; a<instances.numAttributes(); a++) {
			attributes.add( instances.attribute(a) );
		}
		this.attributes = attributes;

		// generate new data object:
		data = instances;
		
	}
	
	/**
	 * Load the Instances dataset from file.  Assumes the dataset can be read, and is therefore either a
	 * ".arff" or ".csv" file.  The loaded data is checked against the objects attributes instance variable, 
	 * and is only loaded into the data variable if the attributes match.  If the loaded data is not successful, 
	 * or the attributes of the loaded data do not match the passed attributes, the data instance variable is
	 * set to null.
	 * @param File file: File to load dataset from.
	 * @param ArrayList<Attribute> attributes: ArrayList of attributes to check the loaded data contains the
	 * expected attributes.
	 */
	public void loadData(File file) {
		loadData(file, attributes);
	}
	
	
	/**
	 * Load the Instances dataset from file.  Assumes the dataset can be read, and is therefore either a
	 * ".arff" or ".csv" file.  The loaded data is checked against the passed attributes, and is
	 * only loaded into the data variable if the attributes match.  If the loaded data is not successful, or
	 * the attributes of the loaded data do not match the passed attributes, the data instance variable is
	 * set to null.
	 * @param File file: File to load dataset from.
	 * @param ArrayList<Attribute> attributes: ArrayList of attributes to check the loaded data contains the
	 * expected attributes.
	 */
	public void loadData(File file, ArrayList<Attribute> attributes) {

		Instances data2 = null;
		
		try {
			
			data2 = DataSource.read( file.getAbsolutePath() );

			boolean attributesMatch = true;

			//if(data.numAttributes() != data2.numAttributes() ) {
				//attributesMatch = false;
				//IJ.showMessage("number of attrs does not match");
			//}
			//else {

				for(int a=0; a<data2.numAttributes(); a++) {

					if(data2.attribute(a).name().compareTo( attributes.get(a).name() ) != 0 && 
							data2.attribute(a).type() != attributes.get(a).type() ) {

						attributesMatch = false;

					}

				}

			//}

			if(attributesMatch) { // only set data to data2 if the attributes match:
				//IJ.showMessage("Attributes Match!");
				data = data2;
			}
			else {
				IJ.showMessage("Instances data is not compatible: "+file.getAbsolutePath() );
				data = null;
			}

		} catch (Exception e) {
			IJ.showMessage("Instances data failed to load: "+file.getAbsolutePath() );
			data = null;
		}
	}
	
	
	/**
	 * Saves the current data object to the file location, after filtering the data object to 
	 * include only the Attributes in the  attributes Instances object.
	 */
	public void saveData(String filePath, Instances attributes) {
		// remove from arff the filter and classifier class attributes:
			// Instances inst = removeAttributesFromInstances(arff, classifierAttributesNoFilterNoClassifier);
		Instances inst = filterInstancesAttributes(data, attributes);

		try {
			DataSink.write(filePath, inst);
			//DataSink.write(dw.getCurrentOutputFile().getAbsolutePath() +".arff", inst);
		} catch (Exception e) {
			IJ.showMessage("Instances data failed to save: "+filePath );
		}
	}
	
	/**
	 * Saves the current data object to the file location, after filtering the data object to 
	 * include only the Attributes in the  attributes ArrayList<Attribute> object.
	 */
	public void saveData(String filePath, ArrayList<Attribute> attributes) {
		// remove from arff the filter and classifier class attributes:
			// Instances inst = removeAttributesFromInstances(arff, classifierAttributesNoFilterNoClassifier);
		Instances inst = filterInstancesAttributes(data, attributes);

		try {
			DataSink.write(filePath, inst);
			//DataSink.write(dw.getCurrentOutputFile().getAbsolutePath() +".arff", inst);
		} catch (Exception e) {
			IJ.showMessage("Instances data failed to save: "+filePath );
		}
	}
	
	
	/**
	 * Saves the current data object to the file location, after filtering the data object to 
	 * include only the Attributes in the  attributes Instances object.
	 */
	public void saveData(File file, Instances attributes) {
		// remove from arff the filter and classifier class attributes:
			// Instances inst = removeAttributesFromInstances(arff, classifierAttributesNoFilterNoClassifier);
		Instances inst = filterInstancesAttributes(data, attributes);

		try {
			DataSink.write(file.getAbsolutePath(), inst);
			//DataSink.write(dw.getCurrentOutputFile().getAbsolutePath() +".arff", inst);
		} catch (Exception e) {
			IJ.showMessage("Instances data failed to save: "+file.getAbsolutePath() );
		}
	}
	
	/**
	 * Returns the instances dataset in this object.
	 * @return
	 */
	public Instances getInstances() {
		return data;
	}
	
	/**
	 * Filters the data Instances object - removes from data each attribute which 
	 * is not in attributeToRetain.
	 * @param attributesToRetain
	 * @return
	 */
	public Instances filterInstancesAttributes(Instances attributesToRetain) {
		return filterInstancesAttributes(data, attributesToRetain);
	}
	
	/**
	 * Filters the data Instances object - removes from data each attribute which 
	 * is not in attributeToRetain.
	 * @param attributesToRetain
	 * @return
	 */
	public Instances filterInstancesAttributes(ArrayList<Attribute> attributesToRetain) {
		return filterInstancesAttributes(data, attributesToRetain);
	}
	
	/**
	 * Removes from inputInstances each attribute which is not in attributeToRetain.
	 * @param inputInstances
	 * @param attributesToRetain
	 * @return
	 */
	public static Instances filterInstancesAttributes(Instances inputInstances, Instances attributesToRetain) {
		
		ArrayList<Attribute> attrs = new ArrayList<Attribute>();
		for(int a=0; a<attributesToRetain.numAttributes(); a++) {
			attrs.add( attributesToRetain.attribute(a) );
		}
		
		return filterInstancesAttributes(inputInstances, attrs);
	}
	
	/**
	 * Removes from inputInstances each attribute which is not in attributeToRetain.
	 * @param inputInstances
	 * @param attributesToRetain
	 * @return
	 */
	public static Instances filterInstancesAttribute(Instances inputInstances, Attribute attributeToRetain) {

		String[] options = new String[2];
		
		//IJ.showMessage("attr retrina num attributes: "+attributesToRetain.numAttributes() );
		boolean removedAnAttribute = false;
		
		for(int a=inputInstances.numAttributes()-1; a>=0; a--) {
			boolean removeAttr = true;
			Attribute attr = inputInstances.attribute(a);
			//IJ.showMessage("attr: "+attr.name() );
				//IJ.showMessage("attr retrain: "+attributesToRetain.attribute(b).name());
			if(  attributeToRetain.name().equals( attr.name() )  ) {
					// retain this attribute in inputInstances
						// set flag to FALSE:
				removeAttr = false;
					//IJ.showMessage("FALSE");
			}
			else {
					//remove this attribute in inputInstances
						// keep flag as true!
					// set removedAnAttribute to true:
				removedAnAttribute = true;
			}
			
			// if removeAttr is true, then remove the attribute:
			if(removeAttr == true) {
				//options[0] = "-R";
				options[1] = options[1]+","+(a+1); //remove the designated attribute.
	
			}
		}
		
		if(removedAnAttribute == true) {
			
			// generate remove object:
			Remove remove = new Remove();
			
			// set options[0]
			options[0] = "-R";
			
			// Remove first comma and word NULL from options[1]
				// null put there as initiall options[1] is 'null' before first string is added, and this
				// is set to options[1] above in first iteration -> 'null,'
			options[1] = options[1].substring(5);
			
			// IJ.showMessage("options[1]: "+options[1]);
			
			try {
		remove.setOptions(options);
			} catch (Exception e) {	}

		// remove from the inputInstances dataset:
			try {
		remove.setInputFormat(inputInstances);
			} catch (Exception e) {	}
		
			try {
		inputInstances = Filter.useFilter(inputInstances, remove);
			} catch (Exception e) {	}
		
		}
	    
		return inputInstances;
		
	}
	
	
	/**
	 * Removes from inputInstances each attribute which is not in attributeToRetain.
	 * @param inputInstances
	 * @param attributesToRetain
	 * @return
	 */
	public static Instances filterInstancesAttributes(Instances inputInstances, ArrayList<Attribute> attributesToRetain) {

		String[] options = new String[2];
		
		//IJ.showMessage("attr retrina num attributes: "+attributesToRetain.numAttributes() );
		boolean removedAnAttribute = false;
		
		for(int a=inputInstances.numAttributes()-1; a>=0; a--) {
			boolean removeAttr = true;
			Attribute attr = inputInstances.attribute(a);
			//IJ.showMessage("attr: "+attr.name() );
			for(int b=0; b<attributesToRetain.size(); b++) {
				//IJ.showMessage("attr retrain: "+attributesToRetain.attribute(b).name());
				if(  attributesToRetain.get(b).name().equals( attr.name() )  ) {
					// retain this attribute in inputInstances
						// set flag to FALSE:
					removeAttr = false;
					//IJ.showMessage("FALSE");
				}
				else {
					//remove this attribute in inputInstances
						// keep flag as true!
					// set removedAnAttribute to true:
					removedAnAttribute = true;
				}
			}
			
			// if removeAttr is true, then remove the attribute:
			if(removeAttr == true) {
				//options[0] = "-R";
				options[1] = options[1]+","+(a+1); //remove the designated attribute.
	
			}
		}
		
		if(removedAnAttribute == true) {
			
			// generate remove object:
			Remove remove = new Remove();
			
			// set options[0]
			options[0] = "-R";
			
			// Remove first comma and word NULL from options[1]
				// null put there as initiall options[1] is 'null' before first string is added, and this
				// is set to options[1] above in first iteration -> 'null,'
			options[1] = options[1].substring(5);
			
			// IJ.showMessage("options[1]: "+options[1]);
			
			try {
		remove.setOptions(options);
			} catch (Exception e) {	}

		// remove from the inputInstances dataset:
			try {
		remove.setInputFormat(inputInstances);
			} catch (Exception e) {	}
		
			try {
		inputInstances = Filter.useFilter(inputInstances, remove);
			} catch (Exception e) {	}
		
		}
	    
		return inputInstances;
		
	}
	
	/**
	 * Adds an Attribute to data at the end of the current Attributes.
	 * @param attribute
	 */
	public void addAttributeToInstances(Attribute attribute) {
		data = addAttributeToInstances(data, attribute);
	}
	
	/**
	 * Adds an Attribute to the Instances object at the end of the current Attributes.  Note,
	 * this method returns the new Instances object.
	 * @param instances
	 * @param attribute
	 * @return
	 */
	public Instances addAttributeToInstances(Instances instances, Attribute attribute) {
		// Generate a new Attribute in instances -> numeric or nominal
		// To put the predictionDistribution number in to!
		
		Add addfilter = new Add();
		
		addfilter.setAttributeIndex("last");
        
		if(attribute.isNominal() == true) {
        	String attrVals = "";
        	for(int a=0; a<attribute.numValues(); a++) {
        		attrVals = attrVals + attribute.value(a);
        		if(a != attribute.numValues()-1) {
        			attrVals = attrVals + ",";
        		}
        	}
        	//IJ.showMessage("Nominal Values: "+attrVals);
        	addfilter.setNominalLabels(attrVals);
        }
        
        addfilter.setAttributeName( attribute.name() );
        
			try {
		addfilter.setInputFormat(instances);
			} catch (Exception e1) {	}
			try {
				instances = Filter.useFilter(instances, addfilter);
			} catch (Exception e1) {	}	
		
		return instances;
	}
	
	
	/**
	 * Set the attribute to value val in each instance in the data object.
	 * @param attribute
	 * @param val
	 */
	public void setAttributeValueOnInstances(String attribute, double val) {
		setAttributeValueOnInstances(data, attribute ,val);
	}
	
	
	/**
	 * Set the attribute to value val in each instance in the instances object.
	 * @param instances
	 * @param attribute
	 * @param val
	 */
	public void setAttributeValueOnInstances(Instances instances, String attribute, double val) {
		
		Attribute attr = instances.attribute( attribute );
		
		for(int a=0; a<instances.size(); a++) {
			// instances.instance(a).setValue(attribute, val);
			instances.instance(a).setValue(attr, val);
		}
	}
	
	/**
	 * This method will add the instance to the dataset.  No attempt is made to check the Attributes
	 * in instance, so the programmer should ensure the attributes are correct.
	 * @param instance
	 */
	public void addData(Instance instance) {
		data.add( instance );
	}
	
	/**
	 * Add the data values to the Instances object (data). This is faster than addData(dataValues,
	 * datapointTitles) as it does not perform any checks of the dataValues array.  
	 * <p>
	 * This algorithm
	 * assumes that the dataValues array length is equal to the data Attributes number, and that
	 * each index in dataValues corresponds to the correct value for that Attribute index in
	 * data.
	 * @param dataValuesAndTitles
	 */
	public void addData(double[] dataValues) {

		//add vals to arff:
		data.add( new DenseInstance(1.0, dataValues) );
		
	}
	
	
	/**
	 * Add the data values to the Instances object (data), in accordance with the matching of the
	 * titles in the 2D array with the Attributes of the dataset.
	 * <p>
	 * If any datapointTitles do not match one of the Attribute titles in data, then no value is
	 * set to the Instance at this index - the default value of 0.0 is set.
	 * @param dataValuesAndTitles
	 */
	public void addData(double[] dataValues, String[] dataAttrTitles) {
		
		// extract values from dataValues:
		double[] vals = new double[data.numAttributes()];
		
		for(int a=0; a<dataAttrTitles.length; a++) {
			int attributeIndex = checkDataForAttribute(dataAttrTitles[a]);
			if(attributeIndex > -1) {
				vals[attributeIndex] = dataValues[a];
			}
		}

		//add vals to arff:
		data.add( new DenseInstance(1.0, vals) );
		
	}
	
	/**
	 * Returns the index of the attribute in data with the title attrTitle, if an Attribute with
	 * title attrTitle exists, otherwise returns -1.
	 * @param attrTitle
	 * @return Index of attribute with attrTitle, otherwise -1.
	 */
	public int checkDataForAttribute(String attrTitle) {
		
		int index = -1;
		// check each title against the attributes in data:
		for(int b=0; b<data.numAttributes(); b++) {
			if(data.attribute(b).name().compareTo( attrTitle ) == 0) {
				index = b;
				break;
			}
		}
		
		return index;
		
	}
	
	/**
	 * Get the value in data of instance at instanceIndex, of attribute at attributeIndex.
	 * @param instanceIndex
	 * @param attributeIndex
	 * @return
	 */
	public double get(int instanceIndex, int attributeIndex) {
		return data.instance(instanceIndex).value( attributeIndex );
	}
	
	/**
	 * Get the value in data of instance at instanceIndex, of attribute with attributeTitle.
	 * @param instanceIndex
	 * @param attributeTitle
	 * @return
	 */
	public double get(int instanceIndex, String attributeTitle) {
		return data.instance(instanceIndex).value( data.attribute(attributeTitle) );
	}

	/**
	 * First, sorts the dataset by the attribute denoted by attributeTitle if sortFirst
	 * is true.  Then, sets the value to the data, in instance at instanceIndex, and in 
	 * attribute denoted by attributeTitle.
	 * @param instanceIndex
	 * @param value
	 * @param attributeTitle
	 * @param sortFirst Boolean to denote whether the dataset should be sorted by attribute
	 * denoted by attributeTitle first.
	 */
	public void setValue(int instanceIndex, double value, String attributeTitle, boolean sortFirst) {
		if(sortFirst == true) {
			sort(attributeTitle);
		}
		data.instance(instanceIndex).setValue(data.attribute(attributeTitle), value);
	}
	
	/**
	 * First, sorts the dataset by the attribute denoted by attributeTitle if sortFirst
	 * is true.  Then, sets the value to the data, in instance at instanceIndex, and in 
	 * attribute denoted by attributeTitle.
	 * @param instanceIndex
	 * @param value
	 * @param attribute Attribute to sort by 
	 * @param sortFirst Boolean to denote wheterh the dataset should be sorted by attribute
	 * denoted by attributeTitle first.
	 */
	public void setValue(int instanceIndex, double value, Attribute attribute, boolean sortFirst) {
		if(sortFirst == true) {
			sort( attribute );
		}
		data.instance(instanceIndex).setValue(attribute, value);
	}
	
	
	/**
	 * Set the Class Attribute on data.
	 * @param attributeTitle
	 */
	public void setClassAttribute(String attributeTitle) {
		data.setClass( data.attribute(attributeTitle) );
	}
	
	/**
	 * Returns the Instance from data at the passed index.
	 * @param index
	 * @return
	 */
	public Instance get(int index) {
		return data.instance(index);
	}
	
	/**
	 * Return the smallest value in the dataset in attribute with title attributeString.
	 * @param attributeString
	 * @return
	 */
	public double getMinValue(String attributeTitle) {
		return getMinValue( data.attribute(attributeTitle).index() );
	}
	
	
	/**
	 * Return the smallest value in the dataset in attribute at attributeIndex.
	 * @param attributeIndex
	 * @return
	 */
	public double getMinValue(int attributeIndex) {
		// sort the data by attribute:
		data.sort(attributeIndex);
		
		// read off the first value in data at attributeIndex:
		return data.instance(0).value( attributeIndex );
	}
	
	/**
	 * Returns the largest value in the dataset in attribute with title attributeString.
	 * @param attributeIndex
	 * @return
	 */
	public double getMaxValue(String attributeTitle) {
		return getMaxValue( data.attribute(attributeTitle).index() );
	}
	
	/**
	 * Returns the largest value in the dataset in attribute at attributeIndex.
	 * @param attributeIndex
	 * @return
	 */
	public double getMaxValue(int attributeIndex) {
		// sort the data by attribute:
		data.sort(attributeIndex);

		// read off the first value in data at attributeIndex:
		return data.instance( (data.numInstances()-1) ).value( attributeIndex );
	}
	
	/**
	 * Returns as an ArrayList<Double> all of the values in the dataset with the
	 * attributeTitle.
	 * @param attributeIndex
	 * @return
	 */
	public ArrayList<Double> getValues(String attributeTitle) {
		return getValues( data.attribute(attributeTitle).index() );
	}
	
	/**
	 * Returns as an ArrayList<Double> all of the values in the dataset at the
	 * attribute at attributeIndex.
	 * @param attributeIndex
	 * @return
	 */
	public ArrayList<Double> getValues(int attributeIndex) {
		ArrayList<Double> values = new ArrayList<Double>();
		for(int a=0; a<data.numInstances(); a++) {
			values.add( data.instance(a).value( attributeIndex) );
		}
		return values;
	}
	
	/**
	 * Returns the index of the first attribute with title attributeTitle, or -1 if
	 * attribute is not in data.
	 * @param attributeTitle
	 * @return
	 */
	public int getAttributeIndex(String attributeTitle) {
		return getAttributeIndex( data.attribute(attributeTitle) );
	}
	
	/**
	 * Returns the first attribute which matches attribute in data, or -1 if
	 * attribute is not in data.
	 * @param attribute
	 * @return
	 */
	public int getAttributeIndex(Attribute attribute) {
		return getAttributeIndex(data, attribute);
	}
	
	public static int getAttributeIndex(Instances data, String attributeTitle) {
		return getAttributeIndex(data, data.attribute(attributeTitle) );
	}
	
	/**
	 * Retursn the first attribute which matches attribute in data, or -1 if
	 * attribute is not in data.
	 * @param data
	 * @param attribute
	 * @return
	 */
	public static int getAttributeIndex(Instances data, Attribute attribute) {
		for(int a=0; a<data.numAttributes(); a++) {
			if(attribute.equals(data.attribute(a))) {
				return a;
			}
		}
		return -1;
	}
	
	/**
	 * Returns an attribute given its name. If there is more than one attribute 
	 * with the same name, it returns the first one. Returns null if the attribute 
	 * can't be found.
	 * @param name
	 * @return
	 */
	public Attribute attribute(String name) {
		return data.attribute(name);
	}
	
	/**
	 * Returns attribute at index, 0-based.
	 * @param index
	 * @return
	 */
	public Attribute attribute(int index) {
		return data.attribute(index);
	}
	
	public AttributeStats attributeStats(int index) {
		return data.attributeStats(index);
	}
	
	/**
	 * Returns the number of instances in the dataset, data.
	 * @return
	 */
	public int numInstances() {
		return data.numInstances();
	}
	
	/**
	 * Returns the number of Attributes in the dataset, data.
	 * @return
	 */
	public int numAttributes() {
		return data.numAttributes();
	}
	
	/**
	 * Returns the designated step size for the currently selected attribute on the attrSelComboBox.
	 * @return A double value of the correct step size for the ComboBox.
	 */
	public double getStepSize(int attrIndex) {
		
		// TODO refactor to ObjectDataContainer or ObjectDatasetHandler???
		
		double stepSize = 1.0;
		
		// Attribute attribute = arff.attribute( (jcb.getSelectedIndex()-1) );
		
		String attributeName = data.attribute(attrIndex).name();
		
		if( ObjectDataContainer.returnRealNumberAttributeTitles().contains(attributeName) ) {
			stepSize = 0.001;
		}
		
		//IJ.showMessage("Step Size: "+stepSize);
		
		return stepSize;
	}
	
	/**
	 * Returns the number of instances in the dataset.
	 * @return
	 */
	public int size() {
		return data.size();
	}
	
	/**
	 * Sorts the instances based on an attribute. For numeric attributes, instances are sorted in 
	 * ascending order. For nominal attributes, instances are sorted based on the attribute label 
	 * ordering specified in the header. Instances with missing values for the attribute are placed 
	 * at the end of the dataset.
	 * @param attributeIndex
	 */
	public void sort(int attributeIndex) {
		data.sort(attributeIndex);
	}
	
	/**
	 * Sorts the instances based on an attribute. For numeric attributes, instances are sorted in 
	 * ascending order. For nominal attributes, instances are sorted based on the attribute label 
	 * ordering specified in the header. Instances with missing values for the attribute are placed 
	 * at the end of the dataset.
	 * @param attributeTitle
	 */
	public void sort(String attributeTitle) {
		data.sort( data.attribute(attributeTitle) );
	}
	
	public void sort(Attribute attr) {
		data.sort( getAttributeIndex(attr) );
	}
	
	/**
	 * Returns the instance at the given position.
	 * @param index the instance's index (index starts with 0)
	 * @return the instance at the given position
	 */
	public Instance instance(int index) {
		return data.instance(index);
	}
	
	/**
	 * Compactifies the dataset data. Decreases the capacity of the set so that it matches the number 
	 * of instances in the set.
	 */
	public void compactify() {
		data.compactify();
	}
	
	/**
	 * Removes all instances from the set.  This removes each instance from the dataset data, but it still has
	 * the same attributes as before.
	 */
	public void delete() {
		data.delete();
	}
	
	
	/**
	 * Collects selectionNumber number of random objects, selecting from index lowIndex, and including 
	 * numInstances number of objects in its selection.  The returned ArrayList will contain the value
	 * which is present in that Instance in the Attribute at the attributeIndex.
	 * <p>
	 * This is used to select the object number (objNo) attribute value for a series of random object to
	 * facilitate the random selection of objects for an unbiased Object Classification.
	 * select from, 
	 * @param lowIndex
	 * @param numInstances
	 * @param number
	 * @return
	 */
	public ArrayList<Integer> collectRandomObjectInstanceValues(int lowIndex, int numInstances, 
			int selectionNumber, String attributeTitle) {
	
		return collectRandomObjectInstanceValues(lowIndex, numInstances, 
				selectionNumber, data.attribute(attributeTitle).index() );
		
	}
	
	/**
	 * Collects selectionNumber number of random objects, selecting from index lowIndex, and including 
	 * numInstances number of objects in its selection.  The returned ArrayList will contain the value
	 * which is present in that Instance in the Attribute at the attributeIndex.
	 * <p>
	 * This is used to select the object number (objNo) attribute value for a series of random object to
	 * facilitate the random selection of objects for an unbiased Object Classification.
	 * select from, 
	 * @param lowIndex
	 * @param numInstances
	 * @param number
	 * @return
	 */
	public ArrayList<Integer> collectRandomObjectInstanceValues(int lowIndex, int numInstances, 
																int selectionNumber, int attributeIndex) {

		// first sort the data by the attribute:
		data.sort(attributeIndex);
		
		ArrayList<Integer> objects = new ArrayList<Integer>();
		
		// PREVENT DUPLICATES - Make a NEW ARRAYLIST which holds a series of indexes:
		// from lowIndex up to numInstances:
		ArrayList<Integer> objRefList = new ArrayList<Integer>();
		for(int a=0; a<numInstances; a++) {
			objRefList.add(a+lowIndex);
		}

		// IJ.showMessage("objRefList arraylist number of entries: "+objRefList.size() );
		// IJ.showMessage("objRefList: "+objRefList);


		for(int a=0; a<selectionNumber; a++) {

			if(objRefList.size() != 0) {

				//IJ.showMessage("+++ Rand number selection index: "+a);
				//Randomly select an object between lowIndex and highIndex:
				// (multiply numInstances by the random number and then add lowIndex)
				double rand = Math.random();
				//double randInst = rand * (double)numInstances;
				double randInst = rand * (double)objRefList.size();
				//randInst = randInst + (double)lowIndex;
				int inst = (int)randInst;

				int instInd = objRefList.get(inst);
				objRefList.remove(inst);

				//retrieve the objNo in unclassifiedFirstPix and store in the objects array:
				objects.add( (int)data.instance(instInd).value(attributeIndex) );

				// Only need the OBjNo, as this can be used to retrieve data from the ARFF dataset in the
				// objSelectionThread..

			}
			else {
				// there are no more objects in the objRefList -> so break out of the for loop:
				//IJ.showMessage("No more objects in objRefList!");
				IJ.showMessage("All Objects Selected",
						"All remaining Objects found \n "
								+ "between 'low' and 'high' criteria. \n"
								+ "Proceeding to Manual Classification...");
				return objects;
			}

		}

		return objects;

	}
	
	
	/**
	 * Adds a new NUMERIC Attribute to the Instances object passed.  attributeIndex can be "first", "last", "1",
	 * "2", "4-7", etc.  And attributeTitle is the title of the Attribute in the instances object.
	 * @param instances
	 * @param attributeIndex
	 * @param attributeTitle
	 * @return
	 */
	public Instances addAttributeToInstances( String attributeIndex, String attributeTitle) {
		
		return addAttributeToInstances(data, attributeIndex, attributeTitle);	
		
	}
	
	/**
	 * Adds a new NUMERIC Attribute to the Instances object passed.  attributeIndex can be "first", "last", "1",
	 * "2", "4-7", etc.  And attributeTitle is the title of the Attribute in the instances object.
	 * @param instances
	 * @param attributeIndex
	 * @param attributeTitle
	 * @return
	 */
	public static Instances addAttributeToInstances(Instances data, String attributeIndex, String attributeTitle) {
		
		// Generate a new Attribute in instances -> numeric:
		// To put the predictionDistribution number in to!
		Add addfilter = new Add();
		addfilter.setAttributeIndex(attributeIndex);
		addfilter.setAttributeName(attributeTitle);
			try {
		addfilter.setInputFormat(data);
			} catch (Exception e1) {	}
			try {
				data = Filter.useFilter(data, addfilter);
			} catch (Exception e1) {	}	
			
		return data;
		
	}
	
	public Instances removeInstanceWithValues( String attributeTitle, String indexToRemove, boolean invert) {
		return removeInstanceWithValues(getAttributeIndex(attributeTitle), indexToRemove, invert);
	}
	
	/**
	 * Removes each instance from the data object whose attribute at attributeIndex is equal to the String
	 * indexToRemove (which can be "first", "last" or an integer index "1", "2", "4-7", etc).  If the boolean invert is 
	 * false, the attribute selection is inverted such that each instance with attribute values matching indexToRemove 
	 * are kept.
	 * @param attributeIndex
	 * @param indexToRemove
	 * @param invert
	 * @return The new Instances object with appropriate instance objects removed.
	 */
	public Instances removeInstanceWithValues( int attributeIndex, String indexToRemove, boolean invert) {
		return removeInstanceWithValues(data, attributeIndex, indexToRemove, invert);
	}
	
	public static Instances removeInstanceWithValues(Instances instances, String attributeTitle, String indexToRemove, boolean invert) {
		return removeInstanceWithValues(instances, getAttributeIndex(instances, attributeTitle), indexToRemove, invert);
	}
	
	
	/**
	 * Removes each instance from the passed instances object whose attribute at attributeIndex is equal to the String
	 * indexToRemove (which can be "first", "last" or an integer index "1", "2", "4-7", etc).  If the boolean invert is 
	 * false, the attribute selection is inverted such that each instance with attribute values matching indexToRemove 
	 * are kept.
	 * @param instances
	 * @param attributeIndex
	 * @param indexToRemove
	 * @param invert
	 * @return The new Instances object with appropriate instance objects removed.
	 */
	public static Instances removeInstanceWithValues(Instances instances, int attributeIndex, String indexToRemove, boolean invert) {
		
		Instances returnInstances = null;
		
		// Build unclassified dataset from arff:
			RemoveWithValues filter = new RemoveWithValues();

			String[] options = null;
			
			if(invert == true) {
				options = new String[5];
			}
			else {
				options = new String[4];
			}
			
			options[0] = "-C";   // Choose attribute to be used for selection
			options[1] = "" + (attributeIndex+1); // Attribute number -> manualClass attribute [index is 31, number is 32!] [index is 27, number is 28!]
			//options[1] = "" + (arff.indexOf(manClass) +1);
			options[2] = "-L";   // Choose nominal class indexes to keep
			options[3] = indexToRemove;   // The index to keep -> Unclassified
									// labels.add(UNCLASSIFIEDATR);
									// labels.add(FEATUREATR); 
									// labels.add(NONFEATUREATR); 
									// labels.add(CONNECTEDATR); 
			if(invert == true) {
				options[4] = "-V";	// Invert the selection, so keep all instances which are NOT unclassified
			}
			
				try {
			filter.setOptions(options);
				} catch (Exception e) { IJ.showMessage("exception setOptions");	}

			//Make sure the arff class attribute is set to MANCLASS:
			instances.setClassIndex(attributeIndex);
				
				try {
			filter.setInputFormat(instances);
				} catch (Exception e) {	IJ.showMessage("exception setInputFormat"); }
				
				try {
			returnInstances = Filter.useFilter(instances, filter);
				} catch (Exception e) { IJ.showMessage("exception useFilter");	}
				
				
			return returnInstances;
			
	}
	
	/**
	 * Removes any attributes from the instance at instanceIndex which are present in the instance at
	 * instanceIndex, but are NOT present in attributesToKeep ArrayList<Attribute>.
	 * @param instanceIndex
	 * @param attributesToKeep
	 * @return
	 */
	public Instance removeAttributesFromInstance(int instanceIndex, ArrayList<Attribute> attributesToKeep) {
		
		Instance instance = data.instance(instanceIndex);
		
		Instances instanceInfo = data;
		
		boolean[] attributesToRemove = new boolean[instance.numAttributes()];
		
		for(int a=0; a<instance.numAttributes(); a++) {
			if( attributesToKeep.contains(instance.attribute(a) )  ) {
				attributesToRemove[a] = false;
			}
			else {
				attributesToRemove[a] = true;
			}
			// instance.attribute(a).name();
		}
		
		//Filter the datasets:
		// remove attributes which are not necessary for object retrieval and object classification:
		Remove remove;
		
		for(int a=attributesToRemove.length-1; a>=0; a--) {
			remove = new Remove();
			// IJ.showMessage("instance: "+instance);
			//classifierAttributes contains an array of boolean values indicating which attribute indices are
				// to be kept in the classified and unclassified datasets.
			// IJ.showMessage("remattrInst index: "+a);
		
			if(attributesToRemove[a] == false) {
			//if its false, need to remove the attribute:
				// IJ.showMessage("remattrInst index: "+a+" attrRem FALSE");
	
				String[] options = new String[2];
				options[0] = "-R";
				options[1] = ""+(a+1); //remove the designated attribute.
				//options[1] = ""+(a); //remove the designated attribute.
	
					try {
				remove.setOptions(options);
						} catch (Exception e) {	}
				
					try {
				remove.setInputFormat(data); //BUG HERE!  Remember to remove the atribute from instanceInfo!
				instanceInfo = removeAttributeFromInstances(instanceInfo,a);
					} catch (Exception e) {}
				
				remove.input(instance);
				
				instance = remove.output();
			
			}// end if
	
		} //end for a
		
		// showArffDatatable(data); //check data has not had its classification removed!
		
		return instance;
		
	}
	
	public Instances removeAttributeFromInstances(Instances instances,int index) {
		
		//Instances returnInstances = null;
		
		//Filter the datasets:
		// remove attributes which are not necessary for object retrieval and object classification:
		Remove remove = new Remove();

		//if its false, need to remove the attribute:

		String[] options = new String[2];
		options[0] = "-R";
		options[1] = ""+(index+1); //remove the designated attribute.

		try {
			remove.setOptions(options);
		} catch (Exception e) {	}

		//remove from the classified dataset:
		try {
			remove.setInputFormat(instances);
		} catch (Exception e) {	}

		try {
			instances = Filter.useFilter(instances, remove);
		} catch (Exception e) {	}

		return instances;
		
	}
	
	public void showArffDatatable() {
		// FIRST -> give the IWP custom canvas the focus:
		// Ensures the custom canvas has focus after clicking on the plot button:
		// IWP.cc.requestFocusInWindow();

		// MatrixPanel offers the initial scatterplot matrix panel - showing all attributes
		// as a 2D array of scatterplots. User can select one to move to the visualisePanel:
		//MatrixPanel mp = new MatrixPanel();

		// This goes directly to the VisualisePanel - using the first two attributes by default
		ArffPanel ap = new ArffPanel(data);


		// String plotName = arff.relationName();
		JFrame jf = new JFrame("Object Manager: Weka Data Table Viewer");
		jf.setSize(1600, 1200);
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
	
	public static void showArffDatatable(Instances data) {
		// FIRST -> give the IWP custom canvas the focus:
		// Ensures the custom canvas has focus after clicking on the plot button:
		// IWP.cc.requestFocusInWindow();

		// MatrixPanel offers the initial scatterplot matrix panel - showing all attributes
		// as a 2D array of scatterplots. User can select one to move to the visualisePanel:
		//MatrixPanel mp = new MatrixPanel();

		// This goes directly to the VisualisePanel - using the first two attributes by default
		ArffPanel ap = new ArffPanel(data);


		// String plotName = arff.relationName();
		JFrame jf = new JFrame("Object Manager: Weka ARFF Viewer");
		jf.setSize(1600, 1200);
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
