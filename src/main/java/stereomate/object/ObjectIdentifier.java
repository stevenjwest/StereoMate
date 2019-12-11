package stereomate.object;

import java.util.ArrayList;

/**
 * 
 * Contains the integer flags for the identity of an object across multiple classes.  These integer flags
 * can be adjusted in this class to move between different instances of a given class.  The 
 * ObjectClassCoordinator can contain multiple classes, with each containing multiple instances, as laid
 * out in the ObjectClassCoordinator constructor.  
 * <p>
 * Each Object which the ObjectClassCoordinator is responsible for identifying, should by definition be 
 * identified with one instance of each class.  This means the Instance identifier flag ints need to
 * be overlaid in an Array to ensure that each set of instances from each class are represented by
 * one int flag.
 * <p>
 * Example:  3 classes, composed of 2 instances each [A B : C D : E F].  The int flags would need to
 * cover the following combinations:
 * <p>
 * ACE, ACF, ADE, ADF, BCE, BCE, BDE, BDF [8 flags, equal to 2x2x2].
 * <p>
 * This class also provides the ability to set the BASE VALUE - which is the value where the int flags
 * begin, and methods to move object flag values in such a wasy as to move the object between different 
 * instances in a given class.
 * <p>
 * Example:  A method would allow an object to move from ACE -> ACF in the example above, or from 
 * ADF -> BDF.
 * <p>
 * This class defines in an abstract manner, using only int flags, the identity of objects.  These can
 * be used by other classes to set object pixel values, to create LUTs for the different classes, or
 * in other ways to identify and move objects between different class instance identities.
 * 
 * TODO Add Exceptions where indexes may be out of bounds.
 * 
 * TODO Add use of Collections class and use more efficient search algorithms -> is it better to use HashMap
 * for each set of instances in a class than an Array?  Or are there better search algorithms to use on an
 * array as opposed to a linear search?
 * 
 * @author stevenwest
 *
 */
public class ObjectIdentifier {

	/**
	 * This int is the value where the class identities begin with.  By default this is 0.
	 */
	private int baseValue;
	/**
	 * The length of the 1D array required to hold all the object identities, assuming each object is defined
	 * as a combination of instances, one from each class.  This is equal to the multiplication of the
	 * length of each array of instances for each class.
	 */
	private int arrayLength;
	/**
	 * String[] array encoding the class titles.  Used for choosing a specific class by name rather than
	 * by index.
	 */
	private String[] classTitles;
	/**
	 * String[][] array encoding class (1st dim.) and instance (2nd dim.) values.
	 */
	private String[][] instanceTitles;
	
	// private int[] flagValues; // -> NO LONGER NEEDED, can derive the "index" from baseValue and index 
									// calculations!
	
	
	// ***  CONSTRUCTOR & METHODS  *** //
	/**
	 * Calls ObjectIdentifier(0, classInstances).
	 * @param classInstances String[][] array which contains each class title plus all instances titles
	 * arrays in each 1st dim array.  All 2nd dim arrays are laid out: [classTitle], [inst1title], 
	 * [inst2title], etc.
	 */
	public ObjectIdentifier(String[][] classInstances) {
		this(0, classInstances);
	}

	/**
	 * The default constructor defines the baseValue (the integer value of the first class)
	 * and also a 2D String array which defines ths title for each Class and its Instances.
	 * <p>
	 * The 2D String array is assumed to be laid out such that each 1st dimension defines a
	 * String array, holding the class and instances data for each class, and the 2nd
	 * dimension defines the class name, followed by the name of each instance.  Below
	 * is an example of a 2D String array defining 3 classes, containing 2, 2, 3 instances
	 * respectively:
	 * <p>
	 * { class1, inst1, inst2 },
	 * <p>
	 * { class2, inst3, inst4 },
	 * <p>
	 * { class3, inst5, inst6, inst7 }
	 * <p>
	 * For this array, and the baseValue, object flag values can be determined.
	 * <p>
	 * The titles of the classes and each instance is stored in this class, and can be used
	 * to identify an object and move it between instances in a class.
	 * <p>
	 * Equally, the int flag value of an object can be used to identify its instance values
	 * across all classes, and its new value can be computed when moving from its current
	 * instance in a class to another instance.
	 * 
	 * @param baseValue
	 * @param classInstances String[][] array which contains each class title plus all instances titles
	 * arrays in each 1st dim array.  All 2nd dim arrays are laid out: [classTitle], [inst1title], 
	 * [inst2title], etc.
	 */
	public ObjectIdentifier(int baseValue, String[][] classInstances) {
		
		// set baseValue:
		this.baseValue = baseValue;
		
		// define the classTitles and instanceTitles:
		classTitles = defineClassTitles(classInstances);
		instanceTitles = defineInstanceTitles(classInstances);
		
		// compute the arrayLength (multiplying all the instanceTitles 2nd dimension array lengths):
		arrayLength = compute1DArrayLength(instanceTitles);
		
		// compute the int flag values array:
		// flagValues = computeFlagValues(instanceTitles, this.baseValue);	
	}
	
	/**
	 * Derives the classTitles array from the classInstances array.  The class titles
	 * are defined as the first entry in each array in classInstances.
	 * @param classInstances String[][] array which contains each class title plus all instances titles
	 * arrays in each 1st dim array.  All 2nd dim arrays are laid out: [classTitle], [inst1title], 
	 * [inst2title], etc.
	 * @return
	 */
	public static String[] defineClassTitles(String[][] classInstances) {
		String[] classTitles = new String[ classInstances.length ];
		
		for(int a=0; a<classTitles.length; a++) {
			classTitles[a] = classInstances[a][0];
		}
		return classTitles;
	}
	
	/**
	 * Derives the instanceTitles instance variables from the classInstances array.  All
	 * instance titles are present in each index after the first index each array in classInstances.
	 * @param classInstances String[][] array which contains each class title plus all instances titles
	 * arrays in each 1st dim array.  All 2nd dim arrays are laid out: [classTitle], [inst1title], 
	 * [inst2title], etc.
	 */
	public static String[][] defineInstanceTitles(String[][] classInstances) {

		
		String[][]instanceTitles = new String[ classInstances.length ][];
		
		String[] currentInstTitles;
		for(int a=0; a<classInstances.length; a++) {
			currentInstTitles = new String[ classInstances[a].length-1 ];
			for(int b=0; b<currentInstTitles.length; b++) {
				currentInstTitles[b] = classInstances[a][b+1]; //collect all instance titles, from index 1-end
			}
			instanceTitles[a] = currentInstTitles;
		}
		
		return instanceTitles;
		
	}
	
	/**
	 * Returns the 1D array length that would be required to hold all objects which could 
	 * be identified with eaxctly 1 instance per class, as defined in the instanceTitles 
	 * 2D String array.
	 * @param instanceTitles String[][] array encoding class (1st dim.) and instance (2nd dim.)
	 * values.
	 * @return
	 */
	public static int compute1DArrayLength(String[][] instanceTitles) {
		int arrayLength = 1;
		for(int a=0; a<instanceTitles.length;a++) {
			arrayLength = arrayLength * instanceTitles[a].length;
		}
		return arrayLength;
	}
	
	/**
	 * Compute the values for all flags - flags are defined as an object which can have exactly
	 * one instance identity with each class, as defined in the instanceTitles object.
	 * @param instanceTitles String[][] array which contains each class title plus all instances titles
	 * arrays in each 1st dim array.  All 2nd dim arrays are laid out: [classTitle], [inst1title], 
	 * [inst2title], etc.
	 * @param baseValue
	 * @return
	 */
	public static int[] computeFlagValues(String[][] instanceTitles, int baseValue) {
		
		int[] flagValues = new int[ computeFlagArrayLength(instanceTitles) ];
		
		fillFlagValues(flagValues, baseValue);
		
		return flagValues;
		
	}
	
	/**
	 * Fills the passed flagValues array with int values starting at baseValue, and
	 * incrementing by 1 with each index.
	 * @param flagValues
	 * @param baseValue
	 */
	public static void fillFlagValues(int[] flagValues, int baseValue) {
		for(int a=0; a<flagValues.length;a++) {
			flagValues[a] = baseValue + a;
		}
	}
	
	/**
	 * Computes the int flag values array and String flag titles array based on the
	 * instanceTitles array and the baseValue.
	 * @param instanceTitles String[][] array encoding class (1st dim.) and instance (2nd dim.)
	 * values.
	 */
	public static int computeFlagArrayLength(String[][] instanceTitles) {
		
		// compute number of instances in each class, and store to a new int[] array:
		int[] instanceNumbers = computeArrayLengths(instanceTitles);
		
		// compute the int[] flag values array and String[] flag titles array total length:
		int flagArrayLength =instanceNumbers[0];
		for(int a=1; a<instanceNumbers.length; a++) {
			flagArrayLength = flagArrayLength * instanceNumbers[a];
		}
		
		return flagArrayLength;		
		
	}
	
	/**
	 * Return the number of instances in each class in an int[] array, where the index
	 * of the array is equal to the class index, and the int at each position is the
	 * number of instances in that class.
	 * @param instanceTitles String[][] array encoding class (1st dim.) and instance (2nd dim.)
	 * values.
	 * @return
	 */
	public static int[] computeArrayLengths(String[][] instanceTitles) {
		
		int[] instanceNumbers = new int[ instanceTitles.length ];
		for(int a=0; a<instanceTitles.length; a++) {
			instanceNumbers[a] = instanceTitles[a].length;
		}
		
		return instanceNumbers;
	}
	

	/***  INSTANCE METHODS  ***/
	
	/**
	 * Returns the baseValue for this object.
	 * @return
	 */
	public int getBaseValue() {
		return baseValue;
	}
	
	/**
	 * Returns the int flagValue for an object of a given identity, where the identity
	 * is defined as the set of instances an object is identified with across all classes.
	 * <p>
	 * instanceIdentifier length must equal the number of classes, and the content of each
	 * index must equal an instance available in each class at that index, otherwise the 
	 * method returns -1.
	 * @param instanceIdentifier
	 * @return
	 */
	public int returnFlagValue(String[] instanceIdentifier) {
		
		// if the instanceIdentifier length is not the same length as number of classes,
			// return -1:
		if(instanceIdentifier.length != classTitles.length) {
			return -1;
		}
		
		// compute the index in flagValues to retrieve the int from, and return the value from flagValues:
		int index = computeFlagValuesIndex(instanceIdentifier, instanceTitles);
		if(index >=0) {
			//return flagValues[index];
			return (index + baseValue);
		}
		else {
			return index;
		}

	}
	
	/**
	 * Returns an ArrayList<Integer> of each flagValue which contains instanceTitle, present in the
	 * class classTitle.
	 * @param classTitle The title of the class to search for instanceTitle in.
	 * @param instanceTitle The title of the instance in class to search for.
	 * @return
	 */
	public ArrayList<Integer> returnFlagValues(String classTitle, String instanceTitle) {
		
		ArrayList<Integer> ints = new ArrayList<Integer>();
		
		int classIndex = returnClassIndex(classTitle, classTitles);
		
		for(int a=0; a<arrayLength; a++) {
			String[] instIdentifier = returnInstanceIdentifier(baseValue+a);
			if( instIdentifier[ classIndex ].equals(instanceTitle) ) {
				ints.add(baseValue+a);
			}
		}
		
		return ints;
	
	}
	/**
	 * Returns a new ArrayList<Integer> which will contain only the flagValues present in the passed
	 * ints ArrayList which contain instanceTitle in the class classTitle.
	 * @param ints Passed ArrayList of flagValues - must contain VALID flag values for this object!
	 * @param classTitle The title of the class to search for instanceTitle in.
	 * @param instanceTitle The title of the instance in class to search for.
	 */
	public ArrayList<Integer> returnFlagValuesContainingInstance(ArrayList<Integer> ints, 
			String classTitle, String instanceTitle ) {
		
		ArrayList<Integer> ints2 = new ArrayList<Integer>();
		
		int classIndex = returnClassIndex(classTitle, classTitles);
		
		for(int a=0; a<ints.size(); a++) {
			String[] instIdentifier = returnInstanceIdentifier( ints.get(a) );
			if( instIdentifier[ classIndex ].equals(instanceTitle) ) {
				ints2.add(baseValue+a);
			}
		}
		
		return ints2;
	}
	
	/**
	 * Returns as a String[] array the instance identity in each class of an object with
	 * a given flagValue.  The instance titles are derived from the instanceTitles String[][]
	 * array (1st dimension encodes classes, 2nd dimension encodes instances).  
	 * <p>
	 * Each instance returned in the String array is derived from the class corresponding 
	 * to the index in the String[] array.
	 * @param flagValue The flag value to use to determine class instances identities.
	 * @return
	 */
	public String[] returnInstanceIdentifier(int flagValue) {
		//return returnInstanceIdentifier( indexOf(flagValue, flagValues), instanceTitles );
		return returnInstanceIdentifierFromIndex( (flagValue-baseValue), instanceTitles );
	}
	
	/**
	 * Returns an individual instance title based on a flagValue and a classTitle, where classTitle
	 * must be one of the titles of the classes passed in the constructor.  If the classTitle is
	 * not found in the classes titles in this object, this method returns null.
	 * @param flagValue The flag value to use to determine class instances identities.
	 * @param classTitle The title of a class.
	 * @return
	 */
	public String returnInstanceIdentifier(int flagValue, String classTitle) {
		String[] instanceIdentifier = returnInstanceIdentifierFromIndex( (flagValue-baseValue), instanceTitles );
		int index = returnClassIndex(classTitle, classTitles);
		if(index > -1) {
			return instanceIdentifier[ index ];
		}
		else {
			return null;
		}
	}
	
	/**
	 * This method will take the current objects flagValue (currentObjFlag), and using the
	 * newClassand newInstance variables, modify this flag value to the new flag value where
	 * the objects instance in newClass is set to newInstance.
	 * @param currentObjFlag
	 * @param newClass
	 * @param newInstance
	 * @return
	 */
	public int returnObjectFlag(int currentObjFlag, String newClass, String newInstance) {
		String[] currentObjIdentifier = returnInstanceIdentifier(currentObjFlag);
		currentObjIdentifier[indexOf(newClass, classTitles)] = newInstance;
		return returnFlagValue(currentObjIdentifier);
	}
	
	/***  STATIC UTILITY METHODS  ***/
	
	/**
	 * Computes the index of the instanceIdentifier String array in flagValues. This requires the
	 * computing of an index in a 1D array which is formed from listing object identities across
	 * all instances in all classes, assuming that each object must be identified with one instance 
	 * in each class.  The list of object identities precedes as such: [0][0][0] -> [0][0][1] ->
	 * [0][1][0], etc.
	 * @param instanceIdentifier
	 * @return
	 */
	public static int computeFlagValuesIndex(String[] instanceIdentifier, String[][] instanceTitles) {
		
		// compute indexes of each instance from instanceIdentifier in each class
			// if a String in instanceIdentifier is not found in instance list of comparable class,
			// return -1.
		int[] instanceIndexes = new int[instanceIdentifier.length];
		for(int a=0; a<instanceIdentifier.length;a++) {
			// check for instanceIdentifier String at index a, in the set of instanceTitles Strings in
				// 1st dimension array at index a:
			instanceIndexes[a] = returnInstanceIndex(instanceIdentifier[a], instanceTitles[a]);
			if(instanceIndexes[a] == -1) {
				return -1;
			}
		}
		
		// compute the actual index in the 1D array from the instanceIndexes values & instanceTitles:
		return computeArrayIndex(instanceIndexes, instanceTitles);
		
	}
	
	public static int[] returnInstanceIndexes(String[] instanceIdentifier, String[][] instanceTitles) throws Exception {
		
		// compute indexes of each instance from instanceIdentifier in each class
		// if a String in instanceIdentifier is not found in instance list of comparable class,
		// return -1.
		int[] instanceIndexes = new int[instanceIdentifier.length];
		for(int a=0; a<instanceIdentifier.length;a++) {
			instanceIndexes[a] = returnInstanceIndex(instanceIdentifier[a], instanceTitles[a]);
			if(instanceIndexes[a] == -1) {
				throw new Exception("Instance not identified in class: "+a);
			}
		}
		return instanceIndexes;
	}
	
	/**
	 * returns the first time instance is found in instanceArray, otherwise it returns -1.
	 * @param instance
	 * @param instanceArray
	 * @return
	 */
	public static int returnInstanceIndex(String instance, String[] instanceArray) {

		for(int a=0; a<instanceArray.length; a++) {
			if(instance.equals(instanceArray[a]) ) {
				return a;
			}
		}
		// if the for loop completes, instance was not found in instanceArray, return -1:
		return -1;
		
	}
	
	/**
	 * returns the first time classTitle is found in classes array, otherwise it returns -1.
	 * @param classTitle
	 * @param classes
	 * @return
	 */
	public static int returnClassIndex(String classTitle, String[] classes) {
		for(int a=0; a<classes.length; a++) {
			if(classTitle.equals(classes[a]) ) {
				return a;
			}
		}
		// if the for loop completes, instance was not found in instanceArray, return -1:
		return -1;
	}
	
	/**
	 * computes the array index of the object, given the specific indexes of the object
	 * in instanceIndexes, and the total lengths of each array in instanceTitles.
	 * @param instanceIdentifier
	 * @param instanceTitles String[][] array encoding class (1st dim.) and instance (2nd dim.)
	 * values.
	 * @return
	 */
	public static int computeArrayIndex(int[] instanceIndexes, String[][] instanceTitles) {
		int sum = 0;
		// computation is the sum of each value in instanceIndexes, but multiplied by
		// the total number of instances in each class the comes after the current index:
		for(int a=0; a<instanceIndexes.length;a++) {
			sum = sum + computeDimensionIndex(instanceIndexes[a], computeMultiplicationFactor(a, instanceTitles) );
		}
		
		return sum;
		
	}
	
	/**
	 * Returns the multiplication factor for a given index to allow the calculation of the index
	 * position in a 1D array composed of the sum of all arrays in instanceTitles.  The multiplication
	 * factor is determined by multipling the lengths of all arrays in instanceTitles together AFTER
	 * the index position.  
	 * <p>
	 * This multiplication factor can be used to determine the index in a 1D array representation of
	 * the 2D arrays of classes present in instanceTitles.
	 * @param index
	 * @param instanceTitles String[][] array encoding class (1st dim.) and instance (2nd dim.)
	 * values.
	 * @return
	 */
	public static int computeMultiplicationFactor(int index, String[][] instanceTitles) {
		
		int multiplicationFactor = 1;
		
		// if the index indicates it is at the end of instanceTitles top array length, then return
			// 1 as multiplication factor, as the index does not need to be multiplied up.
		if( (index+1) == instanceTitles.length) {
			return multiplicationFactor;
		}
		
		for(int a=(index+1); a<instanceTitles.length; a++) {
			multiplicationFactor = multiplicationFactor * instanceTitles[a].length;
		}
		
		return multiplicationFactor;
	}
	
	/**
	 * Returns the multiplication factors for all arrays in instanceTitles. The multiplication
	 * factor is determined by multipling the lengths of all arrays in instanceTitles together AFTER
	 * the index position.  
	 * <p>
	 * This multiplication factor can be used to determine the index in a 1D array representation of
	 * the 2D arrays of classes present in instanceTitles.
	 * @param instanceTitles String[][] array encoding class (1st dim.) and instance (2nd dim.)
	 * values.
	 * @return
	 */
	public static int[] computeMultiplicationFactors(String[][] instanceTitles) {
		int[] multiplicationFactorArray = new int[instanceTitles.length];
		for(int a=0; a<instanceTitles.length; a++) {
			multiplicationFactorArray[a] = computeMultiplicationFactor(a, instanceTitles);
		}
		return multiplicationFactorArray;
	}
	
	/**
	 * 
	 * @param index
	 * @param multiplicationFactor
	 * @return
	 */
	public static int computeDimensionIndex(int index, int multiplicationFactor) {
		return index * multiplicationFactor;
	}

	/**
	 * Returns as a String[] array the instance identity in each class of an object with
	 * a given flagValue.  The instanceTitles parameter must contain a String[][]
	 * array (1st dimension encodes classes, 2nd dimension encodes instances).  
	 * <p>
	 * Each instance returned in the String array is derived from the class corresponding 
	 * to the index in the String[] array.
	 * @param flagValue The flag value to use to determine class instances identities.
	 * @param instanceTitles String[][] array encoding class (1st dim.) and instance (2nd dim.)
	 * values.
	 * @return
	 */
	public static String[] returnInstanceIdentifier(int flagValue, int baseValue, String[][] instanceTitles) {
		//return returnInstanceIdentifier( indexOf(flagValue, flagValues), instanceTitles );
		return returnInstanceIdentifierFromIndex( (flagValue-baseValue), instanceTitles );
	}
	
	/**
	 * Returns as a String[] array the instance identity in each class of an object with
	 * a given flagValue.  The instanceTitles parameter must contain a String[][]
	 * array (1st dimension encodes classes, 2nd dimension encodes instances).  
	 * <p>
	 * Each instance returned in the String array is derived from the class corresponding 
	 * to the index in the String[] array.
	 * @param flagIndex The index in the 1D array used to determine instance identities.
	 * @param instanceTitles String[][] array encoding class (1st dim.) and instance (2nd dim.)
	 * values.
	 * @return
	 */
	public static String[] returnInstanceIdentifierFromIndex(int flagIndex, String[][] instanceTitles) {
		
		// compute the array of instance indexes for each class:
		int[] instanceIndexes = computeInstanceIndexes(flagIndex, instanceTitles);
		
		// return the instance Strings from the instanceIndexes:
		return computeInstanceStrings(instanceTitles, instanceIndexes);

		
	}
	
	/**
	 * With a given flagIndex int and instanceTitles String[][], this method will return
	 * an int[] array containing the indexes of each instance in each class which the
	 * flagIndex identifies.  
	 * <p>
	 * The computation assumes the String[][] instanceTitles defines classes in 1st dim.,
	 * and instances of each class in the 2nd dim., and that the flagIndex selects an
	 * index in a 1D array which contains every combination of instances across all the
	 * classes, assuming they are mixed from highest index to lowest (i.e. from 
	 * [0][0][0] to [0][0][1] to [0][1][0] etc).
	 * @param flagIndex The index in the 1D array used to determine instance identities.
	 * @param instanceTitles String[][] array encoding class (1st dim.) and instance (2nd dim.)
	 * values.
	 * @return int[] array containing the indexes of each instance in each class, the instance
	 * identities of the object identified with flagIndex.
	 */
	public static int[] computeInstanceIndexes(int flagIndex, String[][] instanceTitles) {
		
		int[] multiplicationFactorArray = computeMultiplicationFactors(instanceTitles);
		
		int[] instanceIndexes = new int[ instanceTitles.length ];
		
		for(int a=0; a<instanceIndexes.length; a++) {
			int instanceIndex = flagIndex/multiplicationFactorArray[a];
			instanceIndexes[a] = instanceIndex;
			flagIndex = flagIndex - (multiplicationFactorArray[a]*instanceIndex);
		}
		
		return instanceIndexes;
	
	}

	/**
	 * Returns a String[] array of each instance from each class in instanceTitles, according to the indexes
	 * passed in by instanceIndexes.
	 * @param instanceTitles
	 * @param instanceIndexes
	 * @return
	 */
	public static String[] computeInstanceStrings(String[][] instanceTitles, int[] instanceIndexes) {
		
		String[] instanceIdentifier = new String[instanceIndexes.length];
		
		for(int a=0; a<instanceIdentifier.length;a++) {
			instanceIdentifier[a] = instanceTitles[a][ instanceIndexes[a] ];
		}
		
		return instanceIdentifier;
	}
	
	/**
	 * Returns the index of flagValue in the array flagValues, or -1 if not found.
	 * @param flagValue
	 * @param flagValues
	 * @return
	 */
	public static int indexOf(int flagValue, int[] flagValues) {
		for(int a=0; a<flagValues.length; a++) {
			if(flagValue == flagValues[a]) {
				return a;
			}
		}
		return -1;
	}
	
	public static int indexOf(String classTitle, String[] classTitles) {
		for(int a=0; a<classTitles.length; a++) {
			if(classTitles[a].equals(classTitle) ){
				return a;
			}
		}
		return -1;
	}
	
}
