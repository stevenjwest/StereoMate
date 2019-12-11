package stereomate.object;

import java.util.ArrayList;
import java.util.Random;

import ij.IJ;
import stereomate.data.DatasetWrapper;
import stereomate.data.ObjectDataContainer;
import weka.classifiers.Classifier;
import weka.classifiers.lazy.IBk;
import weka.core.Instances;

public class SelectObjects {

	Instances unclassifiedFirstPix;
	
	Random generator;
	
	/**
	 * Constructor, will build the unclassified First Pixel dataset from data, classifier, classifierAttribute and
	 * the CLASS_INDEX.
	 * @param data
	 * @param classifier
	 * @param classifierAttributes
	 * @param CLASS_INDEX
	 */
	public SelectObjects(Instances data, ObjectClassifier objectClassifier, long seed) {
		
		unclassifiedFirstPix = buildClassifiedDataset( data, objectClassifier );
		
		// IJ.showMessage("Random Generator built with seed: "+seed);
		if(seed > 0) {  // only use the seed if its a positive long above 0
			generator = new Random(seed);
		}
		else { //else just to the blank constructor, which uses System.nanoTime() to get a random seed
			generator = new Random();
		}
	}
	
	
	/**
	 * Splits the arff dataset into unclassified instances (UNCLASSIFIED nominal in Manual Class), and
	 * classified instances (FEATURE, NONFEATURE or CONNECTED in Manual Class).  A second unclassified
	 * set of instances, unclassifiedFirstPix, is also generated identical to unclassified.
	 * <p>
	 * Next, removes each instance from unclassified data which does not pass the Filter (where FITLERCLASS
	 * is set to NOTPASSED) ???
	 * <p>
	 * TODO consider moving this to an abstract class SelectObjects - as this method is only used by all
	 * the SelectObjects methods, and would therefore be better placed in its own class?  Actually should
	 * move it to ObjectDatasetHandler, given it is all dealing with arff dataset.
	 */
	public Instances buildClassifiedDataset(Instances data, ObjectClassifier objectClassifier) {
		
		// first filter the dataset to keep each instance where MANCLASS is UNCLASSIFIED:
		//unclassified = removeInstanceWithValues(arff, MANCLASS, "first", true);
		Instances unclassified = DatasetWrapper.removeInstanceWithValues(
									data,
									ObjectDataContainer.MANUALCLASS, 
									"first", 
									true
									);
		//unclassifiedFirstPix = removeInstanceWithValues(arff, MANCLASS, "first", true);
		Instances unclassifiedFirstPix = DatasetWrapper.removeInstanceWithValues(
											data,
											ObjectDataContainer.MANUALCLASS, 
											"first", 
											true
											);
		
		// next, filter dataset to remove each instance where MANCLASS is UNCLASSIFIED
		Instances classified = DatasetWrapper.removeInstanceWithValues(
									data,
									ObjectDataContainer.MANUALCLASS, 
									"first", 
									false
									);
		
		// showArffDatatable(unclassified);		
		// IJ.showMessage("Showing unclassified dataset before filter removal");
		
		// Next, filter the unclassified datasets themselves to collect only objects which are classified as
			// PASSED on the FILTERCLASS.
		// In this instance, when using it to select objects for random, linear or gaussian selection, the
		// filtered objects should be removed to prevent them being selected during object selection.

		unclassified = DatasetWrapper.removeInstanceWithValues(
								unclassified, 
								ObjectDataContainer.FILTERCLASS, 
								"first", 
								true
								);
		unclassifiedFirstPix = DatasetWrapper.removeInstanceWithValues(
								unclassifiedFirstPix, 
								ObjectDataContainer.FILTERCLASS, 
								"first", 
								true
								);
		
		
		
		// showArffDatatable(unclassified);
	
		// IJ.showMessage("Showing unclassified dataset");
	
		// showArffDatatable(classified);
	
		// IJ.showMessage("Showing classified dataset");
	
		// showArffDatatable(unclassifiedFirstPix);
	
		// IJ.showMessage("Showing unclassifiedFirstPix dataset");
		
		return filterAndBuildClassifier( unclassified,  unclassifiedFirstPix,  classified, objectClassifier );
		
	}
	
	/**
	 * Filters both the 'classified' and 'unclassified' datasets to contain only the instances as set out by the
	 * classifierAttributes object.  This is a series of booleans in an array, which dictates
	 * which attributes (by the index of the boolean) should be removed (false) or kept (true).
	 * <p>
	 * The unclassifiedFirstPix is then filtered, using the classifierAttributesFirstPix object.  This will
	 * only keep the First Pixel data for locating objects from this dataset, once the classifier has been 
	 * applied.  IS THIS NEEDED?  CAN THE ARFF DATASET JUST BE USED INSTEAD?!
	 * <p>
	 * The unclassifiedFirstPix arff dataset has a new numeric attribute added, Probability, which gives the
	 * probability of each instance residing in the FIRST (feature) class.  This is added to this dataset
	 * following the building of the classifier on the classified dataset, and the calculation of the
	 * distributionForInstance in each instance in unclassified.
	 * <p>
	 * [Before the classifier can be built correctly, both the unclassified and classified datasets must be
	 * modified.  Both contain a class Attribute which has 4 potential values, but these need to be converted
	 * such that it only contains 2 values.]
	 * <p>
	 * [ The only way to achieve this at present is to create a new nominal Attribute (called 'Class'), which has
	 * two values ('feature' and 'nonfeature').  Then each dataset's manualClass attribute is looked at, and
	 * the new 'Class' attribute set according to the value of the ManualClass (for unclassified instance, the
	 * value is set to feature [this doesnt matter btw - all the unclassified instances are in the unclassified
	 * dataset, and its Class value is NOT USED - as its not used for training!!], all FEATURE manual Class instance
	 * are set to feature Class, and all NONFEATURE or CONNECTED manual Class instance are set to nonfeature Class). ]
	 * <p>
	 * [ Finally, the Manual Class Attribute is DELETED -> leaving the Class attribute, which has only 2 values,
	 * 'feature' or 'nonfeature', and all instances are set to the correct value in the classified dataset. ]
	 * <p>
	 * [ Only then can the Classifier be TRAINED on the classified dataset, and then each instance in the unclassified
	 * dataset can be presented to the trained classifier to get the probability of each instance being in the
	 * FIRST (feature) class. ]
	 * <p>
	 * This probability (as a decimal) is stored in the unclassifiedFirstPix dataset, which is then SORTED by
	 * its Probability Attribute.
	 */
	public Instances filterAndBuildClassifier( Instances unclassified, Instances unclassifiedFirstPix, 
			Instances classified, ObjectClassifier objectClassifier) {
		
		//Filter the datasets - classified and unclassified must have Attributes which fit with the Classifier:

		classified = DatasetWrapper.filterInstancesAttributes(
						classified, 
						objectClassifier.getClassifierAttributes()
					);

		unclassified = DatasetWrapper.filterInstancesAttributes(
								unclassified, 
								objectClassifier.getClassifierAttributes()
						);
		
		// unclassifiedFirstPix just needs the OBJNO attribute (to look up the object in ObjectDatasetMap obj):
		
		unclassifiedFirstPix = DatasetWrapper.filterInstancesAttribute(
									unclassifiedFirstPix, 
									ObjectDataContainer.returnAttribute( ObjectDataContainer.OBJNO ) 
								);
			
		
		// showArffDatatable(unclassified);
		
		// IJ.showMessage("Showing unclassified dataset post removed attributes");
		
		// showArffDatatable(classified);
		
		// IJ.showMessage("Showing classified dataset post removed attributes");
		
		// showArffDatatable(unclassifiedFirstPix);
		
		// IJ.showMessage("Showing unclassifiedFirstPix dataset post removed attributes");

		// Need to add an extra numeric attribute to unclassifiedFirstPix, to store the probability of
			// the CLASS_INDEX attribute value being true for each unclassified instance:
			// Use the PROBABILITY attribute title as defined in ObjectDataContainer:
		unclassifiedFirstPix = DatasetWrapper.addAttributeToInstances(
									unclassifiedFirstPix, 
									"last", 
									ObjectDataContainer.PROBABILITY 
								);
		
		//set the Class Index on classified and unclassified - the LAST ATTRIBUTE:
		classified.setClassIndex( classified.numAttributes() - 1 );
			
		unclassified.setClassIndex( unclassified.numAttributes() - 1 );
			
		// Check datasets:
			
		// showArffDatatable(unclassified);
			
		// IJ.showMessage("Showing unclassified dataset 2-value class attribute");
			
		// showArffDatatable(classified);
			
		// IJ.showMessage("Showing classified dataset 2-value class attribute");	
		
		// If classifier was not loaded, it needs to be trained on the classified data:
			// This is true each time, as whenever this is run, new objects may have been manually classified!
		if(objectClassifier.isLoaded() == false) {
			objectClassifier.buildClassifier(classified);
		}

		
		// Finally, determine the distributionForInstance() for each item in unclassified, and put it into the new
			// attribute 'Probability' in the unclassifiedFirstPix dataset.
			
			// Note, both unclassified and unclassifiedFirstPix contain the same instances in the same order, but
				// just have different information on them in both dataset
				
			// unclassifiedFirstPix only has the objNo, which is enough to identify it in ARFF for further data retrieval.
		
		// IJ.showMessage("class index: "+CLASS_INDEX);
		
		// retrieve instance variables from objectClassifier:
		Classifier classifier = objectClassifier.getClassifier();
		int CLASS_INDEX = objectClassifier.getClassIndex();
			
		// get the number of test instances from unclassified dataset:
		int numTestInstances = unclassified.numInstances();
	
		double[] predictionDistribution = null;
	
		for(int a=0; a<numTestInstances; a++) {
		
				try {
			predictionDistribution = classifier.distributionForInstance( unclassified.instance(a) );
					} catch (Exception e) {	} 
				
				// Check the values in predictionDistribution are correct!
				
				//IJ.showMessage("predictionDistribution Length: "+predictionDistribution.length);
				
				//for(int b=0; b<predictionDistribution.length; b++) {
				//	IJ.showMessage("unclassified instance num: "+unclassified.instance(a).value(0)+
					//		" distribution: "+b+" val: "+predictionDistribution[b]);
				//}
				
				//IJ.showMessage("Prediction Array to String: "+Utils.arrayToString(predictionDistribution) );
		
			// ONLY CARE ABOUT the Feature predicted value - which is at index: FEATURE_INDEX
				// NOT TRUE -> Now want to Select the index according to what is selected by ComboBox ->
					// New Variable -> CLASS_INDEX.
			
				// Round the Feature predicted double to 5 decimal places:
			predictionDistribution[CLASS_INDEX] = (double)Math.round(predictionDistribution[CLASS_INDEX] * 100000d) / 100000d;
				
			unclassifiedFirstPix.instance(a).setValue(unclassifiedFirstPix.numAttributes()-1, predictionDistribution[CLASS_INDEX]);
				
		}
	
		// unclassifiedFirstPix now has a new LAST attribute "Probability" which contains the probability of each
			// instance being the Selected Class, according to the classifier (and based on the classified object 
				// manual classification and distribution in attribute space).
	
		// Next, need to sort the unclassifiedFirstPix dataset from lowest to highest Probability:
			// Numeric attributes are sorted in ASCENDING order -> low to high
	
		unclassifiedFirstPix.sort( unclassifiedFirstPix.attribute(ObjectDataContainer.PROBABILITY) );
		
		// Check unclassifiedFirstPix dataset:
		
		//DatasetWrapper.showArffDatatable(unclassifiedFirstPix);
		
		//IJ.showMessage("Showing unclassifiedFirstPix dataset post filling probabilities");
		
		//remove the unclassified and classified datasets:
		unclassified = null;
		classified = null;
		
		return unclassifiedFirstPix;
		
	} //end filterAndBuildClassifier()
	
	
	
	
	
	public ArrayList<Integer> selectRandomObjects(int number, double low, double high ) {
		// SELECTING RANDOM OBJECTS:
		// get low & high index for 
		int lowIndex = getLowIndex(
				unclassifiedFirstPix, 
				unclassifiedFirstPix.numAttributes()-1, 
				low
				);
		// int lowIndex = unclassifiedFirstPix.getLowIndex(unclassifiedFirstPix.numAttributes()-1, low);
		int highIndex = getHighIndex(
				unclassifiedFirstPix, 
				lowIndex, 
				unclassifiedFirstPix.numAttributes()-1, 
				high);

		// the number of instances can be calculated as highIndex - lowIndex:
		int numInstances = highIndex - lowIndex;
		
		//IJ.showMessage("lowIdnex: "+lowIndex+" highIndex: "+highIndex+" numInstances: "+numInstances);
		
		return collectRandomObjectInstanceValues(lowIndex, numInstances, number, ObjectDataContainer.OBJNO);
		
	}
	
	/**
	 * Get the index in data of when the data at attributeIndex exceeds lowValue.
	 * This method sorts the Instances data by attributeIndex in ascending order 
	 * before computing the lowIndex value to return.
	 * <p>
	 * Returns the number of instances (i.e. out of the actual index range) if all
	 * data at attributeIndex in all instances of data are below lowValue.
	 * @param attributeIndex
	 * @param lowValue
	 * @return
	 */
	public static int getLowIndex(Instances data, int attributeIndex, double lowValue) {
		// first sort the data by the attribute:
		data.sort(attributeIndex);
		
		int lowIndex = 0;

		// IJ.showMessage("Low Index: "+lowIndex);

		for(int a=0; a<data.numInstances(); a++) {
			// search for index when the first attribute 'Probability' exceeds the low value:
			if(data.instance(a).value(attributeIndex) < lowValue ) {
				lowIndex = lowIndex+1;
			}
			else { 
				//the probability is equal or higher than low, so lowIndex is correct, so break out of the loop:
				break;
			}
		}
		
		return lowIndex;
		
	}
	
	/**
	 * Get the index in data of when the data at attributeIndex exceeds or
	 * equals highValue.  The search begins at lowIndex.
	 * This method sorts the Instances data in ascending order before computing the
	 * lowIndex value to return.
	 * <p>
	 * Returns the number of instances (i.e. out of the actual index range) if all
	 * data at attributeIndex in all instances of data are below or equal to highValue.
	 * @param attributeIndex
	 * @param lowValue
	 * @return
	 */
	public static int getHighIndex(Instances data, int lowIndex, int attributeIndex, double highValue) {
		
		// first sort the data by the attribute:
		data.sort(attributeIndex);
		
		int highIndex = lowIndex;

		for(int a=lowIndex; a<data.numInstances(); a++) {
			// search for index when the last attribute 'Probability' equals or exceeds the high value:
			if(data.instance(a).value(attributeIndex) <= highValue ) {
				highIndex = highIndex+1;
			}
			else { 
				//the probability is equal or higher than high, so highIndex is correct, so break out of the loop:
				break;
			}
		}
		
		return highIndex;
		
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
				selectionNumber, unclassifiedFirstPix.attribute(attributeTitle).index() );
		
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
		
		// first sort the data by the attribute:  OBJ_NO?!
		//unclassifiedFirstPix.sort(attributeIndex);
		// actually sort by the PROBABILITY Attribute:
		unclassifiedFirstPix.sort( (unclassifiedFirstPix.numAttributes()-1) );
		
		ArrayList<Integer> objects = new ArrayList<Integer>();
		
		// PREVENT DUPLICATES - Make a NEW ARRAYLIST which holds a series of indexes:
		// from lowIndex up to numInstances:
		ArrayList<Integer> objRefList = new ArrayList<Integer>();
		for(int a=0; a<numInstances; a++) {
			objRefList.add(a+lowIndex);
		}

		//IJ.showMessage("objRefList arraylist number of entries: "+objRefList.size() );
		// IJ.showMessage("objRefList: "+objRefList);


		for(int a=0; a<selectionNumber; a++) {

			if(objRefList.size() != 0) {

				//IJ.showMessage("+++ Rand number selection index: "+a);
				//Randomly select an object between lowIndex and highIndex:
				// (multiply numInstances by the random number and then add lowIndex)
				double rand = generator.nextDouble();
				//IJ.showMessage("index: "+a+" generator: "+rand);
				
				//double randInst = rand * (double)numInstances;
				double randInst = rand * (double)objRefList.size();
				//randInst = randInst + (double)lowIndex;
				int inst = (int)randInst;
				
				//IJ.showMessage("position to collect index from objRefList: "+inst);

				int instInd = objRefList.get(inst);
				objRefList.remove(inst);
				//IJ.showMessage("index from objRefList: "+instInd);
				//IJ.showMessage("unclassifiedFirstPix ObjNo: "+(int)unclassifiedFirstPix.instance(instInd).value(attributeIndex));

				//retrieve the objNo in unclassifiedFirstPix and store in the objects array:
				objects.add( (int)unclassifiedFirstPix.instance(instInd).value(attributeIndex) );

				// Only need the OBjNo, as this can be used to retrieve data from the ARFF dataset in the
				// objSelectionThread..

			}
			else {
				// there are no more objects in the objRefList -> so break out of the for loop:
				//IJ.showMessage("No more objects in objRefList!");
				if(objects.size() > 0 ) {
					IJ.showMessage("All Objects Selected",
							"All remaining Objects found \n "
									+ "between 'low' and 'high' criteria. \n"
									+ "Proceeding to Manual Classification...");
				}
				return objects;
			}

		}

		return objects;

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
	 * @param divisions
	 * @param low
	 * @param high
	 * @return
	 */
	public ArrayList<Integer> selectLinearObjects(int divisions, double low, double high) {
		// collect probability ranges: the start probability, end probability, and probability division points
		// between low and high to form divisions number of divisions:
		ArrayList<Double> ranges = getRanges(divisions, low, high);

		// compute first index value for each division value in ranges:
		ArrayList<Integer> indexes = computeIndexes(unclassifiedFirstPix, ranges, unclassifiedFirstPix.numAttributes()-1);

		// compute 2D array of indexes: 1st - each division, 2nd - each index in division
		ArrayList<ArrayList<Integer>> divisionsIndexes = computeDivisionsIndexes(indexes);

		// compute an instance index for each division, using divisionsIndexes:
		ArrayList<Integer> objects = computeObjectIndexes(divisionsIndexes);

		// Collect the ObjNo values for each instance at each index specified in objects:
			// ArrayList<Integer> objectNumbers = collectInstanceValues(unclassifiedFirstPix, objects, ObjectDataContainer.OBJNO);
		return collectInstanceValues(unclassifiedFirstPix, objects, ObjectDataContainer.OBJNO);
	}
	
	/**
	 * Computes an ArrayList of Double values which correspond to the start value, end value
	 * and evenly distributed division values between low and high, creating divisions number of divisions.
	 * <p>
	 * TODO consider refactoring this to a new class - ObjectSelection ?
	 * @param divisions
	 * @param low
	 * @param high
	 * @return
	 */
	public static ArrayList<Double> getRanges(int divisions, double low, double high) {
		
		// divide the range 'low' and 'high' into 'divisions' equal segments:
		double range = high - low;
		double rangeDiv = range / divisions;

		ArrayList<Double> ranges = new ArrayList<Double>();

		for(int a=0; a<=divisions; a++) {
			//using a<= ENSURES the number of objects in ranges is divisions+1
			// This will set ranges from 'low' to 'high', with 'divisions' + 1 number of dividing points
			ranges.add(low + (rangeDiv*a));
			if(a == divisions) {
				// this is adding the last value - low + (range x divisions), which is equal to high!
				// however, as the test for filling the indexes below is only testing < and not <=,
				// need to add 0.000001 to this last range value (to make that comparison like <=).
				ranges.set(a, ranges.get(a)+ 0.000001 );
			}
			// IJ.showMessage("index: "+a+" ranges val: "+ranges.get(a) );
		}
		
		return ranges;
		
	}
	
	public static ArrayList<Integer> computeIndexes(Instances data, ArrayList<Double> ranges, String attributeTitle) {
		return computeIndexes(data, ranges, data.attribute(attributeTitle).index() );
	}


	/**
	 * Computes the first indexes of instances in the data dataset at the Attribute residing at 
	 * attributeIndex, where the data values first exceed the values stipulated in the ranges 
	 * ArrayList.  The returned indexes array is the same length as the ranges array, but 
	 * contains the indexes in data where the instances exceeds 
	 * @param ranges
	 * @param attributeIndex
	 * @return
	 */
	public static ArrayList<Integer> computeIndexes(Instances data, ArrayList<Double> ranges, int attributeIndex) {

		// first sort the data by the attribute:
		data.sort(attributeIndex);

		ArrayList<Integer> indexes = new ArrayList<Integer>();

		for(int a=0; a<ranges.size(); a++) {
			// a<ranges.size() ENSURES the number of objects in indexes is divisions+1
			// This just adds the correct number of Integers to indexes
			indexes.add(0);
		}


		// Calculate all the indexes for items in unclassifiedFirstPix dataset for all the ranges:

		// loop through all the ranges set in the ranges ArrayList
		for(int a=0; a<ranges.size(); a++) {

			// for each ranges number, loop through unclassifiedFirstPix, starting at the index specified by the 
			//current index in indexes:
			for(int b=indexes.get(a); b<data.numInstances(); b++) {
				//if the value of the probability at the instance at index 'b' is less than the value specified
				// in ranges, then increment the value in indexes at 'a' by 1:
				if(data.instance(b).value( attributeIndex ) < ranges.get(a) ) {
					indexes.set(a, (indexes.get(a) + 1) );
				}
				else {
					// the value of unclassifiedFirstPix instance probability at this index 'b' exceeds or equals
					//the value in ranges at index 'a', so the index in indexes is now correct:

					break; //just break out of for b loop.

				}
			} // end for b

			//before beginning the next iteration of 'a', need to set the NEXT indexes Int to the CURRENT index:
			// only do this up to the penultimate index:
			if(a < (ranges.size() - 1) ) {
				indexes.set(a+1, indexes.get(a) );
			}

			// IJ.showMessage( "index: "+a+" indexes val: "+indexes.get(a) );

		} // end for a

		return indexes;

	}
	
	
	/**
	 * Computes the indexes which lie in each division, as stipulated in the indexes ArrayList.
	 * The returned 2D array will contain every index value in each division, with each divisions
	 * set of indexes dictated by the values in the current indexes position, to the next indexes
	 * position.  The 2D array therefore returns in its first dimension the number of divisions
	 * (which will by definition be -1 the length of indexes), and in its second dimension it will
	 * return each possible index in that division (a linear set of numbers from indexes.get(a) to
	 * indexes.get(a+1) ).
	 * <p>
	 * TODO consider refactoring this to a new class - ObjectSelection ?
	 * @param indexes
	 * @return
	 */
	public static ArrayList<ArrayList<Integer>> computeDivisionsIndexes(ArrayList<Integer> indexes) {
		
		ArrayList<ArrayList<Integer>> divisionsIndexes = new ArrayList<ArrayList<Integer>>();
		
		ArrayList<Integer> indexesInDivision;
		
		for(int a=0; a<indexes.size()-1; a++) {
			// indexes.size()-1 as indexes+1 used only to get the LAST INDEX in each division.
			
			// compute the index range and make an ArrayList containing these Integers:
			int startIndex = indexes.get(a);
			int endIndex = indexes.get(a+1);
			if(startIndex == endIndex) {
				// if these are equal, there are no indexes to add in this division
					// add a BLANK ArrayList to divisionsIndexes:
				divisionsIndexes.add( new ArrayList<Integer>() );
			}
			else {
				// else, there are indexes to add, so add these to a new ArrayList:
				indexesInDivision = new ArrayList<Integer>();
				for(int b=startIndex; b<endIndex; b++) {
					indexesInDivision.add(b);
				}
				divisionsIndexes.add(indexesInDivision);
			}
		}
		
		return divisionsIndexes;
		
	}
	
	/**
	 * Selects a single index from each division in divisionsIndexes to return.  If a division is empty,
	 * then an index from the nearest adjacent division with values in is retrieved.  Returns an ArrayList
	 * containing a single index for each division.
	 * <p>
	 * TODO consider refactoring this to a new class - ObjectSelection ?
	 * @param divisionsIndexes
	 * @return
	 */
	public static ArrayList<Integer> computeObjectIndexes(ArrayList<ArrayList<Integer>> divisionsIndexes) {
		// divisionsIndexes contains all indexes for the current input, and can be used to compute a Linear
		// Selection across the low and high probability range:

		// IJ.showMessage("divisionIndexes: "+divisionsIndexes);

		// The ArrayList of ArrayList<Integer> objects -> to pass to the ObjectSelectionThread:
		// ArrayList<ArrayList<Integer>> objects = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> objects = new ArrayList<Integer>();

		// want to select 1 object from each index in 1st dimension of divisionsIndexes:
		// IF NO OBJECT in a division: want to find one from an ADJACENT division (above & below)
		// IF NO OBJECTS REMAIN: finish the processing & pass the objects array to the thread:

		for(int a=0; a<divisionsIndexes.size(); a++) {
			// see if there are any values in divisionsIndexes at a:
			if(divisionsIndexes.get(a).size() > 0) {
				objects.add( divisionsIndexes.get(a).get( ((int)(divisionsIndexes.get(a).size()/2)) ) ); // divide by 2, will be floored
				// IJ.showMessage("index: "+a+" index added to objects [this Div]: "+divisionsIndexes.get(a).get( ((int)(divisionsIndexes.get(a).size()/2)) ) );
				// dont forget to REMOVE this index from the division:
				divisionsIndexes.get(a).remove( ((int)(divisionsIndexes.get(a).size()/2)) ); // divide by 2, will be floored
			}
			else {
				// there are no objects in this division -> look in division above and below:
				int objectIndexOtherDivision;
				objectIndexOtherDivision = findObjectIndexOtherDivision(divisionsIndexes, a);
				if(objectIndexOtherDivision == -1) {
					// no further object could be found - break out of this loop, and pass objects array to thread
					// as is.
					if(objects.size() > 0) {
						// tell user all objects selected IF some object indexes have been set to objects:
						IJ.showMessage("All Objects Selected",
								"All remaining Objects found \n "
										+ "between 'low' and 'high' criteria. \n"
										+ "Proceeding to Manual Classification...");
					}
					break;
				}
				else {
					// else objectIndexOtherDivision contains the next index, which was found in another division
					// so - add this to objects, and continue the loop!
					objects.add(objectIndexOtherDivision);
					// IJ.showMessage("index: "+a+" index added to objects [other Div]: "+objectIndexOtherDivision );
				}
			}
		}
		
		return objects;

	}
	
	public static ArrayList<Integer> computeObjectIndexes(ArrayList<Integer> distributionList, ArrayList<ArrayList<Integer>> divisionsIndexes) {
		// The ArrayList of ArrayList<Integer> objects -> to pass to the ObjectSelectionThread:
		// ArrayList<ArrayList<Integer>> objects = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> objects = new ArrayList<Integer>();
	
		// Want to select distributionList number of objects from each division index of divisionsIndexes:
			// IF NO OBJECT in a division: want to find one from an ADJACENT division (above & below)
			// IF NO OBJECTS REMAIN: finish the processing & pass the objects array to the thread:
		
		// need to use a double for loop -> 
			// for a: distributionList size,
				// for b: number of objects to be retrieved in each index of distributionList
		for(int a=0; a<distributionList.size(); a++) {
			// IJ.showMessage("distributionList index: "+a+" obj number: "+distributionList.get(a));
			for(int b=0; b<distributionList.get(a); b++) {
				// see if there are any values in divisionsIndexes at index a:
				if(divisionsIndexes.get(a).size() > 0) {
					// retrieve MIDDLE index:
					objects.add( divisionsIndexes.get(a).get( ((int)(divisionsIndexes.get(a).size()/2)) ) ); // divide by 2, will be floored
					// IJ.showMessage("index: "+a+" index added to objects [this Div]: "+divisionsIndexes.get(a).get( ((int)(divisionsIndexes.get(a).size()/2)) ) );
					// dont forget to REMOVE this index from the division:
					divisionsIndexes.get(a).remove( ((int)(divisionsIndexes.get(a).size()/2)) ); // divide by 2, will be floored
				}
				else {
					// there are no objects in this division -> look in division above and below:
					int objectIndexOtherDivision;
					objectIndexOtherDivision = findObjectIndexOtherDivision(divisionsIndexes, a);
					if(objectIndexOtherDivision == -1) {
						// no further object could be found - break out of this loop, and pass objects array to thread
						// as is.
						if(objects.size() > 0) {
							// tell user all objects selected IF some object indexes have been set to objects:
							IJ.showMessage("All Objects Selected",
									"All remaining Objects found \n "
											+ "between 'low' and 'high' criteria. \n"
											+ "Proceeding to Manual Classification...");
						}
						break;
					}
					else {
						// else objectIndexOtherDivision contains the next index, which was found in another division
						// so - add this to objects, and continue the loop!
						objects.add(objectIndexOtherDivision);
						// IJ.showMessage("index: "+a+" index added to objects [other Div]: "+objectIndexOtherDivision );
					}
				}
			}
		}
		
		return objects;
		
	}
	
	/**
	 * 
	 * 
	 * <p>
	 * TODO consider refactoring this to a new class - ObjectSelection ?  Use Interface or Abstract class to
	 * generate RandomObjectSelection, LinearObjectSelection & GaussianObjectSelection objects?
	 * 
	 * @param divisionsIndexes
	 * @param division
	 * @return
	 */
	public static Integer findObjectIndexOtherDivision(ArrayList<ArrayList<Integer>> divisionsIndexes, int division) {
		int returnValue = -1;
		int nextDivisionSet = 1;
		
		boolean continueSearchForward = true;
		boolean continueSearchBackward = true;
		
		int divisionsIndexesSize = divisionsIndexes.size();
		
		while(true) {
			if( division+nextDivisionSet < divisionsIndexesSize && continueSearchForward == true ) {
				returnValue = findObjNextDiv(divisionsIndexes, division+nextDivisionSet);
				if(returnValue != -1) {
					break;
				}
			}
			else {
				continueSearchForward = false;
			}

			if(division-nextDivisionSet >=0 && continueSearchBackward == true) {
				returnValue = findObjPrevDiv(divisionsIndexes, division-nextDivisionSet);
				if(returnValue != -1) {
					break;
				}
			}
			else {
				continueSearchBackward = false;
			}
			
			if(continueSearchForward == false && continueSearchBackward == false) {
				// no more indexes can be found - break out of this loop, and return the -1 value:
				break;
			}
			else {
				// else still more divisions to search, so increment nextDivisionSet, and continue the
					// while loop:
				nextDivisionSet = nextDivisionSet + 1;
			}
		}
		
		
		return returnValue;
		
	}
	
	/**
	 * 
	 * <p>
	 * TODO consider refactoring this to a new class - ObjectSelection ?
	 * 
	 * @param divisionsIndexes
	 * @param division
	 * @return
	 */
	public static Integer findObjNextDiv(ArrayList<ArrayList<Integer>> divisionsIndexes, int division) {
		if(divisionsIndexes.get(division).size() > 0) {
			int val = divisionsIndexes.get(division).get( 0 ); // 0 -> get first obj in the next division
			// dont forget to REMOVE this index from the division:
			divisionsIndexes.get(division).remove( 0 ); // 0 -> get first obj in the next division
			return val;
		}
		else {
			return -1;
		}
	}
	
	
	/**
	 * 
	 * <p>
	 * TODO consider refactoring this to a new class - ObjectSelection ?
	 * 
	 * @param divisionsIndexes
	 * @param division
	 * @return
	 */
	public static Integer findObjPrevDiv(ArrayList<ArrayList<Integer>> divisionsIndexes, int division) {
		if(divisionsIndexes.get(division).size() > 0) {
			int val = divisionsIndexes.get(division).get( (divisionsIndexes.get(division).size()-1) ); // size-1 -> get last obj in the next division
			// dont forget to REMOVE this index from the division:
			divisionsIndexes.get(division).remove( (divisionsIndexes.get(division).size()-1) ); // size-1 -> get last obj in the next division
			return val;
		}
		else {
			return -1;
		}
	}
	
	/**
	 * Collects the Attribute values of attributeTitle of each instance at the indexes specified in 
	 * objects.  The returned ArrayList will contain the data of each instance at the indexes specified
	 * in objects, of the Attribute specified by attributeTitle.
	 * <p>
	 * This is used to select the object number (objNo) attribute value for a series of object to
	 * facilitate the random selection of objects for an unbiased Object Classification.
	 * select from, 
	 * @param lowIndex
	 * @param numInstances
	 * @param number
	 * @return
	 */
	public static ArrayList<Integer> collectInstanceValues(Instances data, ArrayList<Integer> objects, String attributeTitle) {
		return collectInstanceValues(data, objects, data.attribute(attributeTitle).index() );
	}
	
	/**
	 * Collects the Attribute values at attributeIndex of each instance at the indexes specified in 
	 * objects.  The returned ArrayList will contain the data of each instance at the indexes specified
	 * in objects, at the Attribute specified by attributeIndex.
	 * <p>
	 * This is used to select the object number (objNo) attribute value for a series of object to
	 * facilitate the random selection of objects for an unbiased Object Classification.
	 * select from, 
	 * @param lowIndex
	 * @param numInstances
	 * @param number
	 * @return
	 */
	public static ArrayList<Integer> collectInstanceValues(Instances data, ArrayList<Integer> objects, int attributeIndex) {
		
		ArrayList<Integer> objectNumbers = new ArrayList<Integer>();

		for(int a=0; a<objects.size(); a++) {
			objectNumbers.add( (int)data.instance( objects.get(a) ).value(attributeIndex) );
		}
		
		return objectNumbers;
		
	}
	
	public ArrayList<Integer> selectGaussianObjects(String distribution, double low, double high) {
		// Calculate the number and division ints from the distribution String:
		
			ArrayList<Integer> distributionList = new ArrayList<Integer>();
		
			parseStringToArrayList(distribution, distributionList);
		
			// divisions is the number of segments that need to be delineated between 'low' & 'high':
			// equal to the number of items now in distributionList:
			int divisions = distributionList.size();
		
			// distributionNumber is the SUM of the instances which must be collected:
			int distributionNumber = 0;
		
			// also want to determine the "Height" of the distributionList - what is its largest number?
			int distributionHeight = 0;

			// compute distributionNumber & distributionHeight:
			for(int a=0; a<divisions; a++) {
				distributionNumber = distributionNumber + distributionList.get(a);
				if(distributionHeight < distributionList.get(a)) {
					distributionHeight = distributionList.get(a);
				}
			}

			// collect probability ranges: the start probability, end probability, and probability division points
			// between low and high to form divisions number of divisions:
			ArrayList<Double> ranges = getRanges(divisions, low, high);

			// compute first index value for each division value in ranges:
			ArrayList<Integer> indexes = computeIndexes(unclassifiedFirstPix, ranges, unclassifiedFirstPix.numAttributes()-1);

			// compute 2D array of indexes: 1st - each division, 2nd - each index in division
			ArrayList<ArrayList<Integer>> divisionsIndexes = computeDivisionsIndexes(indexes);

			// compute a number of instance indexes for each division, according to the numbers found
				// in distributionList, using divisionsIndexes:
			ArrayList<Integer> objects = computeObjectIndexes(distributionList, divisionsIndexes);

			// Collect the ObjNo values for each instance at each index specified in objects:
			// ArrayList<Integer> objectNumbers = collectInstanceValues(unclassifiedFirstPix, objects, ObjectDataContainer.OBJNO);
			return collectInstanceValues(unclassifiedFirstPix, objects, ObjectDataContainer.OBJNO);
	}
	
	/**
	 * Sets the distributionList Integer ArrayList to the sequence of integers presented in the 'distribution' String.
	 * The 'distribution' String must contain a sequence of integers separated by commas (else a NumberFormatException
	 * is thrown).
	 * @param distribution String which holds a sequence of Integers, separated by commas.
	 * @param distributionList An ArrayList<Integer> which is filled with the sequence of integers from the distribution
	 * String.
	 */
	public static void parseStringToArrayList(String distribution, ArrayList<Integer> distributionList) {
		
		int numberIndex = 0;
		int divisions = 1;
		
		for(int a=0; a<distribution.length(); a++) {
			//IJ.showMessage("distribution substring - 1st ind: "+a+" 2nd ind: "+(a+1) + " val: "+distribution.substring(a, a+1) );
			if( (distribution.substring(a, a+1)).equals(",") ) {
				divisions = divisions + 1;
				distributionList.add(  Integer.parseInt( distribution.substring(numberIndex, a) )  );
				numberIndex = a+1;
			}
		}
	 	
		//dont forget to add the last int - from last comma to end of string!
		distributionList.add(  Integer.parseInt( distribution.substring(numberIndex, distribution.length() ) )  );
		
		//IJ.showMessage("distribution ArrayList: "+distributionList);
	
	}
	
}
