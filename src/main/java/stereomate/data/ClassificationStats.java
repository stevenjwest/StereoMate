package stereomate.data;

public class ClassificationStats {
	
	public boolean allUnclassified, featureClassified, nonfeatureClassified, connectedClassified;
	
	public int total;

	public int unclassified;

	public int feature;

	public int nonfeature;

	public int connected;
	
	/**
	 * Initialise variables to standard values: ints to 0, and booleans to FALSE.
	 */
	public ClassificationStats() {

		allUnclassified = true;
		featureClassified = false;
		nonfeatureClassified = false;
		connectedClassified = false;
		
		total = 0;
		unclassified = 0;
		feature = 0;
		nonfeature = 0;
		connected = 0;
		
	}
	
	/**
	 * This method will adjust the classStats object according to the object selected classification and what it is
	 * being classified as.
	 */
	public void adjustStats(int objSelectedPixVal, int classification) {
		
		// Adjust classStats according to the initial classification of the obj:
		if(objSelectedPixVal == ObjectDataContainer.UNCLASSIFIED_INDEX ) {
			//selected obj is initially unclassified
				//the obj will be set to another class, so should first set boolean allUnclassified to false:
			allUnclassified = false;
			
			// the numbers and boolean on the type of class needs to be dealt with according to the value of classification:
			if(classification == ObjectDataContainer.FEATURE_INDEX ) {
				//then add 1 to FEATURE int, and set featureClassified to true:
				feature = feature + 1;
				featureClassified = true;
				
			}
			else if(classification == ObjectDataContainer.NONFEATURE_INDEX ) {
				//then add 1 to NONFEATURE int, and set nonFeatureClassified to true:
				nonfeature = nonfeature + 1;
				nonfeatureClassified = true;
			}
			else if(classification == ObjectDataContainer.CONNECTED_INDEX ) {
				//then add 1 to CONNECTED int, and set connectedClassified to true:
				connected = connected + 1;
				connectedClassified = true;
			}
			
		}// end selObjPix UNCLASSIFIED
		
		
		else if(objSelectedPixVal == ObjectDataContainer.FEATURE_INDEX ) {
			//selected obj is initially FEATURE:
			
			//remove 1 from the feature int, and set its boolean to false if int is 0:
			feature = feature - 1;
			if(feature == 0) {
				featureClassified = false;
			}
		
			// the numbers and boolean on the type of class needs to be dealt with according to the value of classificaiton:
			if(classification == ObjectDataContainer.NONFEATURE_INDEX ) {
				//then add 1 to NONFEATURE int, and set nonfeatureClassified to true:
				nonfeature = nonfeature + 1;
				nonfeatureClassified = true;
			}
			else if(classification == ObjectDataContainer.CONNECTED_INDEX ) {
				//then add 1 to CONNECTED int, and set connectedClassified to true:
				connected = connected + 1;
				connectedClassified = true;
			}
			else if(classification == ObjectDataContainer.UNCLASSIFIED_INDEX ) {
				//then do nothing - do not keep track of unclassified numbers...
			}
			
			//if all the obj numbers are 0, then set allUnclassified to true:
			if(feature == 0 && nonfeature == 0 && connected == 0 ) {
				allUnclassified = true;
			}
			
		}// end selObjPix FEATURE
		
		
		else if(objSelectedPixVal == ObjectDataContainer.NONFEATURE_INDEX ) {
			//selected obj is initially NONFEATURE:
			
			//remove 1 from the nonfeature int, and set its boolean to false if int is 0:
			nonfeature = nonfeature - 1;
			if(nonfeature == 0) {
				nonfeatureClassified = false;
			}
		
			// the numbers and boolean on the type of class needs to be dealt with according to the value of classificaiton:
			if(classification == ObjectDataContainer.FEATURE_INDEX ) {
				//then add 1 to FEATURE int, and set featureClassified to true:
				feature = feature + 1;
				featureClassified = true;
			}
			else if(classification == ObjectDataContainer.CONNECTED_INDEX ) {
				//then add 1 to CONNECTED int, and set connectedClassified to true:
				connected = connected + 1;
				connectedClassified = true;
			}
			else if(classification == ObjectDataContainer.UNCLASSIFIED_INDEX ) {
				//then do nothing - do not keep track of unclassified numbers...
			}
			
			//if all the obj numbers are 0, then set allUnclassified to true:
			if(feature == 0 && nonfeature == 0 && connected == 0 ) {
				allUnclassified = true;
			}
			
		}// end selObjPix NONFEATURE
		
		
		else if(objSelectedPixVal == ObjectDataContainer.CONNECTED_INDEX ) {
			//selected obj is initially FEATURE:
			
			//remove 1 from the connected int, and set its boolean to false if int is 0:
			connected = connected - 1;
			if(connected == 0) {
				connectedClassified = false;
			}
		
			// the numbers and boolean on the type of class needs to be dealt with according to the value of classificaiton:
			if(classification == ObjectDataContainer.NONFEATURE_INDEX ) {
				//then add 1 to NONFEATURE int, and set nonfeatureClassified to true:
				nonfeature = nonfeature + 1;
				nonfeatureClassified = true;
			}
			else if(classification == ObjectDataContainer.FEATURE_INDEX ) {
				//then add 1 to FEATURE int, and set featureClassified to true:
				feature = feature + 1;
				featureClassified = true;
			}
			else if(classification == ObjectDataContainer.UNCLASSIFIED_INDEX ) {
				//then do nothing - do not keep track of unclassified numbers...
			}
			
			//if all the obj numbers are 0, then set allUnclassified to true:
			if(feature == 0 && nonfeature == 0 && connected == 0 ) {
				allUnclassified = true;
			}
			
		}  // end selObjPix CONNECTED
		
		// Finally, adjust any objects which are affected by object classification:
		
		// adjustObjSelRandSpinners(classStats);
		
	}
	
	
}
