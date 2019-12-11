package stereomate.object;

import java.awt.Component;
import java.io.File;

import javax.swing.JFileChooser;

import ij.IJ;
import stereomate.data.DatasetWrapper;
import stereomate.data.ObjectDataContainer;
import stereomate.image.ImageHandler;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

/**
 * This Class represents the WEKA Classifier used to determine:
 * <p>
 * 1. Build/Train a classifier on a given Instances data object.
 * <p>
 * 2. Use the Trained Classifier to determine the Distribution (probability of belonging to each
 * value of the Class attribute) for each Instance in the dataset.  This data is used to sort
 * the instances databased on its probability of belonging to a given nominal value in the Class
 * Attribute.
 * <p>
 * 3. Use the Trained Classifier to classify an Instance.  This data is used to set the classification,
 * as determined by this classifier, of a given instance object.
 * 
 * @author stevenwest
 *
 */
public class ObjectClassifier {
	
	
	/**
	 * A Classifier Object to represent the Classifier used to ML driven object selection.  This is either a
	 * rough ML classifier generated using the pre-set Classifier option & pre-set attributes selected, or
	 * it is the imported classifier.
	 */
	private Classifier classifier;
	
	/**
	 * A file object from which a classifier might be loaded.  If a Classifier is loaded from disk, a representation
	 * of the file it was loaded from is stored in this variable.  If the classifier was not loaded from disk,
	 * this will be null.
	 */
	private File classifierFile;
	
	/**
	 * Instances objects which hold the attributes which the classifier will use/has been trained on, the
	 * attributes which give the first pixel and object numbers, and attributes for the entire dataset EXCEPT
	 * the Filter and Classifier Attributes.
	 */
	private Instances classifierAttributes;
	
	/**
	 * For the Class Attribute set on data, the CLASS_INDEX indicates which value in the Class Attribute should
	 * be used for computing the Classifier Statistics.
	 */
	private int CLASS_INDEX;
	
	/**
	 * A boolean to represent whether the current Classifier was loaded for a file.  If a classifier and its
	 * attributes were loaded from a file, it implies the classifier is also trained!  Therefore the classifier
	 * will not be trained during the 
	 */
	private boolean classifierLoaded;
	

	
	/**
	 * Sets CLASS_INDEX and build a rough classifier and classifierAttributes 
	 * as defined by method setRoughClassifierAndAttributes().
	 * @param classIndex
	 */
	public ObjectClassifier(int classIndex) {
		CLASS_INDEX = classIndex;
		setRoughClassifierAndAttributes();
	}
	
	/**
	 * Sets the index of the nominal value encoding the Class Attribute.  The ClassIndex is used to compute
	 * and return the correct DistributionForInstance.
	 * @param classIndex
	 */
	public void setClassIndex(int classIndex) {
		CLASS_INDEX = classIndex;
	}
	
	public int getClassIndex() {
		return CLASS_INDEX;
	}
	
	public Classifier getClassifier() {
		return classifier;
	}
	
	/**
	 * Returns whether the classifier and its attributes was loaded from a file, or if its a rough classifier.
	 * @return
	 */
	public boolean isLoaded() {
		return classifierLoaded;
	}
	
	/**
	 * Builds the classifier as chosen by the user - indicated in the CLASSIFIER string attribute.
	 * @return a new classifier as specified in the CLASSIFIER String.
	 * <p>
	 * IF a Classifier has been IMPORTED by the User, then THE IMPORTED CLASSIFIER is used instead!
	 * @param classifier String to name a Classifier?
	 */
	public Classifier getRoughClassifier(String classifier) {
		
		// First, re-set the classifierAttributes value:
		// setInstancesClassifierAttributesObject();
		
		// TODO add ability for User Imported Classifier to be used!
		
		//Set CLASSIFIER to "NaiveBayes":
	  	//Other options would be "J48", "IBk"...
		
		if(classifier.equalsIgnoreCase("NaiveBayes")) {
			return new NaiveBayes();
		}
		else if(classifier.equalsIgnoreCase("J48")) {
			return new J48();
		}
		else if(classifier.equalsIgnoreCase("IBk")) {
			return new IBk();
		}
		else {
			return new NaiveBayes(); //return NaiveBayes as default.
		}
		
	}
	
	/**
	 * Sets the classifier and classifierAttributes to default values: Classifier is IBk with k = 3, and
	 * classifierAttributes is set to Attributes returned by ObjectDataContainer.returnClassifierAttributes().
	 * <p>
	 * Sets classifierLoaded to false.
	 */
	public void setRoughClassifierAndAttributes() {
		classifier = new IBk();
			( (IBk) classifier ).setKNN(3);
		classifierAttributes = new Instances("classifier", ObjectDataContainer.returnClassifierAttributes(), 0 );
		// set classifierLoaded to false:
		classifierLoaded = false;
	}
	
	/**
	 * Sets the classifier and classifierAttributes to the passed objects.  Sets classifierLoaded to false.
	 * <p>
	 * Note, when setting the classifierAttributes, the CLASS ATTRIBUTE (the one which determines the class
	 * of an instance) is ASSUMED to be the LAST ATTRIBUTE - so be sure to set this as the last Attribute 
	 * (typically this is MANUALCLASS from ObjectDataContainer).
	 * @param classifier
	 * @param classifierAttributes
	 */
	public void setRoughClassifierAndAttributes(Classifier classifier, Instances classifierAttributes) {
		this.classifier = classifier;
		this.classifierAttributes = classifierAttributes;
		// set classifierLoaded to false:
		classifierLoaded = false;
	}
	
	/**
	 * Returns the classifierAttributes as an Instances object.
	 * @return
	 */
	public Instances getClassifierAttributes() {
		return classifierAttributes;
	}
	
	/**
	 * Sets the classifier and classifierAttributes to the passed values.
	 * @param classifier
	 * @param classifierAttr
	 */
	public void setClassifierAndAttributes(Classifier classifier, Instances classifierAttr) {
		this.classifier = classifier;
		this.classifierAttributes = classifierAttr;
	}
	
	/**
	 * Retrieves the classifierFile from this class - which contains a reference to a location on disk where
	 * the current classifier was loaded from, otherwise null.
	 * @return
	 */
	public File getClassifierFile() {
		return classifierFile;
	}
	
	/**
	 * Loads a Classifier and its Attributes from a file.  If a Classifier is successfully
	 * loaded, the method returns a reference to the File object from which the classifier was
	 * successfully loaded.  If an invalid file is selected,
	 * no classifier is loaded, and this method returns Null.
	 * @param fileChooserStartDir
	 * @param fileChooserParentComp
	 * @return A File object from where the Classifier was loaded, otherwise null.
	 */
	public File loadClassifier(File fileChooserStartDir, Component fileChooserParentComp) {
		// Need to open a File Selector and Select a Classifier File:
		
		JFileChooser fileChooser = new JFileChooser( fileChooserStartDir );
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setDialogTitle("Select WEKA Serialised Classifier File:");
		
		int returnVal = fileChooser.showOpenDialog(fileChooserParentComp);
		
		try { // put try.. catch OUTSIDE the if statement
				// if the readAll method throws an exception, the rest of the if statement is skipped,
				// and the catch statement at the end is run
		
		if(returnVal == JFileChooser.APPROVE_OPTION) {

			// First retrieve a reference to the file:
			classifierFile = fileChooser.getSelectedFile();
			
			//attempt to retrieve the Classifier & its Attributes header into an object array:
				// this requires exception handling, am using this to CANCEL this method at this point
				// if the selected file is NOT a weka based classifier:
					// Good Use of the try... catch statement! ;o)
			Object o[] = null;
			// try {
				o = SerializationHelper.readAll( classifierFile.getAbsolutePath() );
			// } catch (Exception e1) {e1.printStackTrace();} 
			
				// set classifier and classifierAttributes to objects in o:
			classifier = (Classifier) o[0];
			classifierAttributes = (Instances) o[1]; // use to see filter Attributes for this Classifier
			
			// IJ.showMessage("Classifier: "+classifier);
			// IJ.showMessage("Classifier Attributes: "+classifierAttributes);
			
			// set classifierLoaded to true:
			classifierLoaded = true;
			
			// AND set the Classifier name to the Textfield:
			// classifierTextField.setText( file.getName() );
			
			// sets classifierLoaded to true, activates classifierDeleteButton, resets CLASS_INDEX,
				// and classifies the objects with the current classifier:
			// activateLoadedClassifier();
			
			// activate the classifierViewButton:
			// classifierViewButton.doClick();
			
			return classifierFile;
			
		}
		
		} catch (Exception e1) {
				//e1.printStackTrace();
			// if an exception is caught, it means the readAll() method could not resolve a classifier.
			// Therefore do not want to cancel the plugin, but should just return an error message via
				// showMessage:
			IJ.showMessage("Could Not Find Classifier", "The file selected did not contain a classifier,\n"
													   +"please try again.");
		}
		return null;
	}
	
	
	/**
	 * Loads a Classifier and its Attributes from a file.  If a Classifier is successfully
	 * loaded, the method returns a reference to the File object from which the classifier was
	 * successfully loaded.  If an invalid file is selected,
	 * no classifier is loaded, and this method returns Null.
	 * @param fileChooserStartDir
	 * @param fileChooserParentComp
	 * @return A File object from where the Classifier was loaded, otherwise null.
	 */
	public File loadClassifier(File classifierFile) {
		
		try { // put try.. catch OUTSIDE the if statement

			Object o[] = null;
			o = SerializationHelper.readAll( classifierFile.getAbsolutePath() );
			
			classifier = (Classifier) o[0];
			classifierAttributes = (Instances) o[1]; // use to see filter Attributes for this Classifier
			
			//for(int a=0; a<classifierAttributes.numAttributes(); a++) {
				//IJ.showMessage("classifier Attrs: "+a+" "+classifierAttributes.attribute(a).name());
			//}
			//IJ.showMessage("Class Attribute: "+classifierAttributes.classAttribute());
			
			// set classifierLoaded to true:
			classifierLoaded = true;
			
			return classifierFile;
		
		} catch (Exception e1) {
				//e1.printStackTrace();
			// if an exception is caught, it means the readAll() method could not resolve a classifier.
			// Therefore do not want to cancel the plugin, but should just return an error message via
				// showMessage:
			IJ.showMessage("Could Not Find Classifier", "The file selected did not contain a classifier,\n"
													   +"please try again.");
		}
		return null;
	}
	
	/**
	 * Attempts to load a classifier from the classifierFile.  The method will return null if a classifier
	 * and its attributes was not successfully loaded, or if the classifierFile is null.  Thus, the return
	 * value can be checked, and if it is not null, can be used to determine whether loading a classifier
	 * was successful.
	 * @return
	 */
	public File loadClassifier() {
				
		if(classifierFile !=null) {
			try {
				//attempt to retrieve the Classifier & its Attributes header into an object array:
				// this requires exception handling, am using this to CANCEL this method at this point
				// if the selected file is NOT a weka based classifier:
				// Good Use of the try... catch statement! ;o)
				Object o[] = null;
				// try {
				o = SerializationHelper.readAll( classifierFile.getAbsolutePath() );
				// } catch (Exception e1) {e1.printStackTrace();} 
				
				// set classifier and classifierAttributes to objects in o:
				classifier = (Classifier) o[0];
				classifierAttributes = (Instances) o[1]; // use to see filter Attributes for this Classifier

				// set classifierLoaded to true:
				classifierLoaded = true;
				
				// return the classifierFile - to signify a classifier was successfully loaded:
				return classifierFile;
			}
			catch(Exception e1) {
				IJ.showMessage("Could Not Find Classifier", "The file selected did not contain a classifier,\n"
						+"please try again.");
			}
			return null;		
		}
		else {
			return null;
		}
	}
	
	/**
	 * Saves the Classifier file to the OM_MetaData Directory.  Classifier should be saved to 
	 * OM_MetaData as 'OM_classifier.model'.  Only saved if a classified is loaded (if classifierLoaded is TRUE).
	 */
	public void saveClassifierFile(File file) {
		
		if(classifierLoaded == true) {
		
			// generate the path for the classifier:
			String classifierPath = file.getAbsolutePath();
			// File classifierFile = new File( OM_MetaData + File.separator + "OM_classifier.model" );

			// serialize classifier with header information
				// be sure to use classifierAttributes !!
			Instances header = new Instances(classifierAttributes, 0); 
			try {
				SerializationHelper.writeAll(classifierPath, new Object[]{classifier, header});
			} catch (Exception e) {
				IJ.showMessage("Unable to save Classifier","The Classifier was not saved.\n"
														  +"Please check classifier is loaded\n"
														  +"and the OM_MetaData file exists.");
			}
		
		}
		
	}
	
	/**
	 * Sets classifierFile to null, to remove the file representation from this class.
	 */
	public void removeClassifierFile() {
		classifierFile = null;
	}
	
	/**
	 * Trains the classifier with the passed instances dataset.  If not successful then an error message
	 * is printed in ImageJ.  If successful, the classifierTrained boolean is set to true.
	 * @param instances
	 */
	public void buildClassifier(Instances instances) {
		try {
			classifier.buildClassifier(instances);
		} catch (Exception e) {
			IJ.error("Classifier not trained correctly.");
			e.printStackTrace();
		}
	}
	
	/**
	 * Return the distribution probability, based on the current classifier and its training, for the
	 * passed instance, of the Class Attribute value at CLASS_INDEX, which is the index instance 
	 * variable defined and set in this class.
	 * @param instance
	 * @return
	 */
	public double distributionForInstanceAtClassIndex(Instance instance) {
		double[] distribution = null;
		try {
			distribution = classifier.distributionForInstance(instance);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return distribution[CLASS_INDEX];
	}
	
	/**
	 * Returns the whole distribution probability, based on the current classifier and its training,
	 * of the passed instance.  This will contain the probability of the instance belonging to each
	 * value in the Class Attributes values.
	 * @param instance
	 * @return
	 */
	public double[] distributionForInstance(Instance instance) {
		try {
			return classifier.distributionForInstance(instance);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Will apply a LOADED CLASSIFIER to the imageHandler and datasetHandler objects, using objIdentifier to
	 * modify the imageHandler object pixels appropriately.  If filterOverClassifier is TRUE, any objects
	 * which are set to NOT_PASSED in the datasetHandler are automatically set to NON_FEATURE.
	 * <p>
	 * This will ONLY RUN if the Classifier in this class has been LOADED - the Rough Classifier will NOT WORK
	 * with this method.
	 * @param datasetHandler
	 * @param imageHandler
	 * @param objIdentifier
	 * @param filterOverClassifier
	 */
	public void classifyObjects(DatasetWrapper datasetHandler, ImageHandler imageHandler, 
			ObjectIdentifier objIdentifier, boolean filterOverClassifier) {
		classifyObjects(datasetHandler, imageHandler, objIdentifier, filterOverClassifier, true);
	}
	
	/**
	 * NOTE: This version may put the imageHandler and datasetHandler OUT OF SYNC:
	 * <p>
	 * Will Classify objects based on a loaded classifier, if loaded.  The boolean
	 * setPixelValues will determine whether the image pixel values are also set/adjusted
	 * during the classification.  By setting setPixelValues to FALSE, ONLY the dataset data is classified
	 * (by adjusting the CLASSIFIERCLASS Attribute) and the image pixel values are NOT CHANGED.
	 * <p>
	 * Will apply a LOADED CLASSIFIER to the imageHandler and datasetHandler objects, using objIdentifier to
	 * modify the imageHandler object pixels appropriately.  If filterOverClassifier is TRUE, any objects
	 * which are set to NOT_PASSED in the datasetHandler are automatically set to NON_FEATURE.
	 * <p>
	 * This will ONLY RUN if the Classifier in this class has been LOADED - the Rough Classifier will NOT WORK
	 * with this method.
	 * @param datasetHandler
	 * @param imageHandler
	 * @param objIdentifier
	 * @param filterOverClassifier
	 */
	public void classifyObjects(DatasetWrapper datasetHandler, ImageHandler imageHandler, 
			ObjectIdentifier objIdentifier, boolean filterOverClassifier, boolean setPixelValues) {
		
		if( isLoaded() ) { // only run if a classifier is LOADED -> do not run if classifier is the rough classifier!
			// note: In the SelectObjects class this method is NOT used if a Rough or Loaded classifier is applied to
				// the 'unclassified' dataset, but the distributionForInstance is determined there with the classifier
				// therefore, this method is not needed in that class, and classifyObjects is only used if a classifier
					// is LOADED by the user!

			// This Instance obj is used to filter the dataset to have the appropriate Attributes for Classification:
			Instances unclassified = datasetHandler.filterInstancesAttributes( getClassifierAttributes() );

			// double to store return value:
			double returnVal = 0.0;

			boolean classifierError = false; // is set to true if an exception is thrown with classifyInstance() method

			// loop through each Instance in datasetHandler:
			for(int a=0; a<datasetHandler.size(); a++) {

				// get the firstPixel coordinate for this instance:
				int x = (int)datasetHandler.instance(a).value(datasetHandler.attribute(ObjectDataContainer.X1));  // x1
				int y = (int)datasetHandler.instance(a).value(datasetHandler.attribute(ObjectDataContainer.Y1));  // y1
				int z = (int)datasetHandler.instance(a).value(datasetHandler.attribute(ObjectDataContainer.Z1)); 


				try {

					// FIRST - try to classify the object - if this fails, DO NOT TRY TO ADJUST Pixel Value or
					// Attribute value of the object!
					// Instead, the catch clause is run at the end -> may want to adjust this to only inform
					// the user that some objects were not classified, rather than inform the user about 
					// each object whcih was not classified??
					returnVal = classifier.classifyInstance( unclassified.instance(a) );

					// If a Filter is applied and this obj is NOT PASSED - set Classifier Attr to NONFEATURE:
					if( (int)datasetHandler.instance(a).value( datasetHandler.attribute(ObjectDataContainer.FILTERCLASS) ) 
							== ObjectDataContainer.FIL_NOTPASSED_INDEX
							&& filterOverClassifier ) {

						if(setPixelValues == true) {
							// adjust pixel value of obj in image to reflect the new classifier class value:
							imageHandler.setObjValue(
									x, y, z, 
									objIdentifier.returnObjectFlag(
											imageHandler.getPixelValueThresholded(x, y, z), 
											ObjectDataContainer.CLASSIFIERCLASS, 
											ObjectDataContainer.NONFEATUREATR
											)
									);
						}

						// adjust Classifier Attribute value for this instance:
						datasetHandler.instance(a).setValue(
								datasetHandler.attribute(ObjectDataContainer.CLASSIFIERCLASS), 
								ObjectDataContainer.NONFEATUREATR
								);				

						// setClassifierObj(ObjectDataContainer.CLAS_NONFEATURE_INDEX, datasetHandler.instance(a), datasetHandler );

					}
					else {
						// else the object has not been filtered, so classify this instance and adjust:
						// object pixel value in image appropriately
						// The classifier attribute's value in datasetHandler appropriately

						// retrieve the title of the classifier value from the returnVal (which is the index of this
						// value in the Classifier Class):
						String classifierTitleValue = ObjectDataContainer.getValueTitleFromIndex(
								ObjectDataContainer.CLASSIFIERCLASS, 
								(int)returnVal
								);

						if(setPixelValues == true) {
							// adjust pixel value of obj in image to reflect the new classifier class value:
							imageHandler.setObjValue(
									x, y, z, 
									objIdentifier.returnObjectFlag(
											imageHandler.getPixelValueThresholded(x, y, z), 
											ObjectDataContainer.CLASSIFIERCLASS, 
											classifierTitleValue
											)
									);
						}

						// adjust the classifier attribute's value in datasetHandler appropriately
						datasetHandler.instance(a).setValue(
								datasetHandler.attribute(ObjectDataContainer.CLASSIFIERCLASS), 
								classifierTitleValue
								);

						// setClassifierObj( (int)returnVal, datasetHandler.instance(a), datasetHandler );


					}

				} // place all the object editing inside the try clause
				catch (Exception e) { 
					classifierError = true;
					// IJ.error("unable to classify instance: "+a);
				}
			}

			if(classifierError == true) {
				IJ.error("Error in classifying instances - the classifier has not been built or loaded.");
			}

		}
	}
	
	/**
	 * Set the set of objects to the denoted classifierIndex, both setting the object in the imageHandler
	 * and the datasetHandler.
	 * @param datasetHandler
	 * @param imageHandler
	 * @param objIdentifier
	 * @param classifierIndex
	 */
	public void classifyObjects(DatasetWrapper datasetHandler, ImageHandler imageHandler, 
			ObjectIdentifier objIdentifier, String classifierValue) {
		
		// loop through each Instance in datasetHandler:
		for(int a=0; a<datasetHandler.size(); a++) {
			
			// get the firstPixel coordinate for this instance:
			int x = (int)datasetHandler.instance(a).value(datasetHandler.attribute(ObjectDataContainer.X1));  // x1
			int y = (int)datasetHandler.instance(a).value(datasetHandler.attribute(ObjectDataContainer.Y1));  // y1
			int z = (int)datasetHandler.instance(a).value(datasetHandler.attribute(ObjectDataContainer.Z1)); 
				
			// adjust pixel value of obj in image to reflect the new classifier class value:
			imageHandler.setObjValue(
					x, y, z, 
					objIdentifier.returnObjectFlag(
							imageHandler.getPixelValueThresholded(x, y, z), 
							ObjectDataContainer.CLASSIFIERCLASS, 
							classifierValue
							)
					);

			// adjust Classifier Attribute value for this instance:
			datasetHandler.instance(a).setValue(
					datasetHandler.attribute(ObjectDataContainer.CLASSIFIERCLASS), 
					//ObjectDataContainer.NONFEATUREATR -> use classifierValue parameter!
					classifierValue
					);				
							
			}	
	}
	

}
