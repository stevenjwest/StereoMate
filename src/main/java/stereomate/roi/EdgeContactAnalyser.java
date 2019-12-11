package stereomate.roi;

import java.util.ArrayList;

public class EdgeContactAnalyser {

	//Arrays to store the coordinate index in points array of any edges detected in width [x] and height [y]:
	//There is one array for each image side - xStart, xEnd, yStart, yEnd:
	ArrayList<Integer> EdgeIndexes;

	//Arrays to store the lengths of each edge ROI contact to an edge - to calculate total ROI edge
	//contact length for each Edge:
	ArrayList<Integer> EdgeLengths;		

	//This is set to true if the first Point in the parallelPoints array is on the edge, and its matching
	//point is the last point in parallelPoints array.
	//It ensures the last point is skipped during the findEdges() method if the first and last points are
	//added to the EdgeIndexes at the start of the loop
	boolean FirstPointEdge;

	//This holds the coordinates in the dimension the edge needs to be detected
	int[] parallelPoints;

	//This holds the coordinates in the other dimension - used for calculating the length of any edgePoints
	//indexes which equal the edgeValue [i.e. touch the edge!]
	int[] orthogonalPoints;

	//This represents the value in parallel array which is considered on the edge.
	//Typically 0 or image width/height, but can be set to any value.
	int edgeDetectionValue;

	//This ArrayIndexer allows movement around the parallelPoints array without getting an out
	//of bounds error (if at last point, next ref is 0, if at 0, the prev point is last point).
	ArrayIndexer indexer;

	/**
	 * Constructor.  Sets the variables and initialises them appropriately.
	 * @param edgeDetectionValue
	 * @param parallelPoints
	 * @param orthogonalPoints
	 */
	public EdgeContactAnalyser(int edgeDetectionValue, int[] parallelPoints, int[] orthogonalPoints) {

		//Generate Integer ArrayLists:
		EdgeIndexes = new ArrayList<Integer>();
		EdgeLengths = new ArrayList<Integer>();	

		//Set FirstPointEdge to false:
		FirstPointEdge = false;

		//set the passed int and int arrays to relevant instance variables:
		this.parallelPoints = parallelPoints;    		
		this.orthogonalPoints = orthogonalPoints;
		this.edgeDetectionValue = edgeDetectionValue;
		
		//IJ.log("parallelPoints length: "+parallelPoints.length );

		//initialise an ArrayIndexer object to move around the parallelPoints array indexes without
		//out of bounds exception:
		indexer = new ArrayIndexer(parallelPoints.length, 0);

	}


	/**
	 * Returns the length of the EdgeLengths determined in this object.
	 * @return An int of summed Edge Lengths.
	 */
	public int getLength() {
		int length = 0;
		for(int a=0; a<EdgeLengths.size(); a++) {
			length = length + EdgeLengths.get(a);
		}
		return length;
	}


	/**
	 * this method will find edges in the parallel points array, based on the FirstPointEdge value, and for
	 * every pair of points found on the edge, the EdgeLength is calculated and added to the EdgeLength array
	 * as well as the indexes of the Edge Values in the parallelPoints array saved to the EdgeIndexes array.
	 */
	public void findEdges() {
		
		int ind1 = 0, ind2 = 0;
		
		int upperIndex = (parallelPoints.length - 1) ;
		
		//IJ.log("initial upperIndex: "+upperIndex);
		
		//IJ.log("initial indexer length: "+indexer.getLength() );
		
		outerloop:
		do {
			
			if(parallelPoints[indexer.getIndex()] == edgeDetectionValue) {
				// the point at the index is on the edge:
				
				//IJ.log("Edge detected: "+indexer.getIndex() +" parallel value: "+parallelPoints[indexer.getIndex()]
					//	+" orth value:"+orthogonalPoints[indexer.getIndex()]+ " edgeDetectionVal: "+edgeDetectionValue);
				
				// first, check if this is index0 - and if so, need to check BACKWARDS:
				if(indexer.getIndex() == 0) {
					
					// check backwards:
					int topTarget = parallelPoints.length - 1;
					
					// only execute this code IF there are points at the END of parallelPoints array that equal
						// the edgeDetectionValue:
					if(parallelPoints[topTarget] == edgeDetectionValue) {
						
						//IJ.log("last entry in parallelPoints is on edge: "+parallelPoints[topTarget] +
						//		"index: "+topTarget);

						// determine index where parallelPoints does not equal edgeDetectionValue:
						while(parallelPoints[topTarget] == edgeDetectionValue) {
							topTarget = topTarget - 1;
						}
						
						//IJ.log("topTarget AFTER while loop: "+topTarget);

						// Therefore the last index in parallelPoints which equals edgeDetectionValue is + 1:
						topTarget = topTarget + 1;
						
						//IJ.log("topTarget AFTER +1 (added to EdgeIndexes and set to ind1 & upperIndex): "+topTarget);

						// set this index as the FIRST index in EdgeIndexes:
						EdgeIndexes.add( topTarget );

						// and set the value of ind1 to this first index:
						ind1 = topTarget;
						
						// and adjust the length of indexer to stop these points from the top of the array being
							// assessed twice:
						indexer.setLength(topTarget );
						
						// Change topTarget to the new upper index (to prevent infinite loop!):
							// needs to be -1, as indexer length is topTarget -> otherwise will create infinite loop:
						upperIndex = topTarget-1;
						//IJ.log("topTarget: indexer Length: "+indexer.getLength() );
						//IJ.log("topTarget: upperIndex: "+upperIndex );
						
					}
					else {
						// else, the last value in parallelPoints is NOT on the edge - therefore the FIRST edge
							// detection is actually at 0:
						
						//so, set this index to EdgeIndexes:
						EdgeIndexes.add( indexer.getIndex() );
						
						// and set the value of ind1 to this first index:
						ind1 = indexer.getIndex();
					}
					
				}
				else {
					
					//IJ.log("index added to edgeIndexes (ind1): "+indexer.getIndex() );
					// else, this is NOT index 0 - so just add this index to EdgeIndexes:
					EdgeIndexes.add( indexer.getIndex() );
					
					// and set the value of ind1 to this first index:
					ind1 = indexer.getIndex();
				}
				
				// Explore how many points FORWARD the edge is present for:
				
				// set ind2 to the current index (will be first index where edge detected, unless index is 0,
					// in which case it will still be 0 although the First index where an edge was detected may
					// be at the top of the parallelPoints array.
				ind2 = indexer.getIndex();
				
				//IJ.log("ind2 before while loop: "+ind2 );
				
				// determine index where parallelPoints does not equal edgeDetectionValue:
				while(parallelPoints[ind2] == edgeDetectionValue) {
					
					ind2 = ind2+1;
					//IJ.log("ind2 in while loop: "+ind2 );
					
					// add if clause to check if this while loop ever reaches the end of the indexer:
					if(ind2 == indexer.getLength() ) {
						//IJ.log("ind2 == indexer length");
						// if it does, all remaining points up to the end of the parallelPoints array are
							// on the edge:
						// so - add ind2-1 as the SECOND index in EdgeIndexes:
						EdgeIndexes.add( (ind2-1) );
						
						//IJ.log("ind2-1 added to EdgeIndexes: "+(ind2-1) );
						// calculate the total length between these indexes - use orthogonalPoints array:
						EdgeLengths.add( Math.abs(orthogonalPoints[ ind1 ] - 
								orthogonalPoints[ (ind2-1) ]) );
						
						//IJ.log("orth points ind1: "+orthogonalPoints[ ind1 ]+
						//		"orth points ind2: "+orthogonalPoints[ (ind2-1) ]);

						//IJ.log("length added to EdgeLengths: "+ (Math.abs(orthogonalPoints[ ind1 ] - 
						//												orthogonalPoints[ (ind2-1) ])) );
						
						// and break out of the outer do..while loop:
						break outerloop;
					}
				}
				
				// Therefore the last index in parallelPoints which equals edgeDetectionValue is - 1:
				ind2 = ind2 - 1;
				
				// IJ.log("index added to edgeIndexes (ind2): "+ind2 );
				
				// set this index as the SECOND index in EdgeIndexes:
				EdgeIndexes.add( ind2 );
				
				// and calculate the total length between these indexes - use orthogonalPoints array:
				EdgeLengths.add( Math.abs(orthogonalPoints[ ind1 ] - 
						orthogonalPoints[ ind2 ]) );
				
				//IJ.log("orth points ind1: "+orthogonalPoints[ ind1 ]+
					//			"orth points ind2: "+orthogonalPoints[ ind2 ]);

				//IJ.log("length added to EdgeLengths: "+ (Math.abs(orthogonalPoints[ ind1 ] - 
					//													orthogonalPoints[ ind2 ])) );
				
				// adjust indexer value to ind2:
				indexer.setIndex(ind2);
				
			}
			
		} while(indexer.moveUpTo(upperIndex) );

	}
	
	
	public void findEdges2() {
					
		outerloop:
		do {

			//X COORDINATE:

			// Does current value equal the edgeDetectionValue?
			if(parallelPoints[indexer.getIndex()] == edgeDetectionValue) {
				// if accessed, this point resides on Edge:

				//check either side of parallelPoints[index] to find the other Edge contact:

				if(FirstPointEdge == true && indexer.getIndex() == (parallelPoints.length - 1) ) {
					//if FirstPointEdge is true and this is the last point -> do not process
						// as will be matched to the first point!

				}
				else {
					//else, process as normal

					//First check next point along the array:
					if(parallelPoints[indexer.getIndexPlus1()] == edgeDetectionValue) {
						//the next point along the array is the paired xStart Edge point:

						//add first index to EdgeIndexes arraylist - to add it before the next EdgeIndex:
						EdgeIndexes.add( indexer.getIndex() );
						int ind1 = indexer.getIndex();
						
						int inc = 1;
						while(parallelPoints[indexer.getIndex()] == edgeDetectionValue) {
							boolean breaker = indexer.moveUpTo(parallelPoints.length - 1);
							if(breaker == false) {
								break outerloop;
							}
						}

						//THEN Add this index to xStartEdgeIndex:
						EdgeIndexes.add( indexer.getIndex() );
						int ind2 = ( indexer.getIndex() );

						//calculate the length of the Edge contact:
						//by looking at the y coordinates for these two points and calculating the difference:
						//and add to EdgeLengths:

						EdgeLengths.add( Math.abs(orthogonalPoints[ ind1 ] - 
								orthogonalPoints[ ind2 ]) );
						
						//IJ.showMessage("IndexPlus1 TRUE - EdgeLengths: " + EdgeLengths.get(EdgeLengths.size()-1) +
						//				"EdgesIndexes first: "+ind1+" second: "+ind2);

					}

					//Else check the previous point -> LAST POINT IN POINTS ARRAY!
					//Note: This can only happen if the FIRST point in points is on the edge, and its
					//pair is the LAST point in points (as in all other cases the first point is 
					//always lower in the array)
					else if(parallelPoints[parallelPoints.length-1] == edgeDetectionValue &&
							parallelPoints[parallelPoints.length-2] != edgeDetectionValue) {
						//else the x point previous to current x point must be the other xStartEdge contact

						//First, set the xStartFirstPointEdge to true
						//This will stop the points at the end of points being added again:
						FirstPointEdge = true;

						//In this case, add the length from current point (which can only be the FIRST point)
						//and the last point in points:
						EdgeLengths.add( Math.abs(orthogonalPoints[ indexer.getIndex() ] - 
								orthogonalPoints[ indexer.getIndexMinus1() ]) );

						//Add this index to xStartEdgeIndex first - to ensure its FIRST in the EdgeIndex:
						EdgeIndexes.add( indexer.getIndexMinus1() );
						int ind1 = indexer.getIndexMinus1();

						//add first index to xStartEdgeIndex arraylist - to add it AFTER the end xStartEdgeIndex:
						EdgeIndexes.add( indexer.getIndex() );
						int ind2 = indexer.getIndex();
						
						//IJ.showMessage("parallelPoints last ind TRUE - EdgeLengths: " + EdgeLengths.get(EdgeLengths.size()-1) +
						//				"EdgesIndexes first: "+ind1+" second: "+ind2);

					}

				} // end if xStartFirstPointEdge true && b is points.length-1
				
			} //end if points[b].x == xStart


		} while(indexer.moveUpTo(parallelPoints.length - 1) );

	}


	/**
	 * Deprecated version of find Edges()
	 */
	@Deprecated
	public void findEdgesDep() {

		for(int b=0; b<parallelPoints.length; b++) {

			//X COORDINATE:

			//Does it equal xStart - the value which indicates its on the image edge?
			//note, xStart is probably 0, but may be different!
			if(parallelPoints[b] == edgeDetectionValue) {
				//this point resides on xStart Edge:

				//check either side of points[b] to find the other xStart Edge contact:

				if(FirstPointEdge == true && b == (parallelPoints.length - 1) ) {
					//if FirstPointEdge is true and this is the last point, do not process

				}
				else {
					//else, process as normal

					//First check next point along the array:
					if(parallelPoints[b+1] == edgeDetectionValue) {
						//the next point along the array is the paired xStart Edge point:

						//add first index to xStartEdgeIndex arraylist - to add it before the next xStartEdgeIndex:
						EdgeIndexes.add(b);

						//Add this index to xStartEdgeIndex:
						EdgeIndexes.add(b+1);

						//calculate the length of the xStart Edge contact:
						//by looking at the y coordinates for these two points and calculating the difference:
						//and add to xStartEdgeLengths:

						EdgeLengths.add( Math.abs(orthogonalPoints[b] - orthogonalPoints[b+1]) );

					}

					//Else check the previous point -> LAST POINT IN POINTS ARRAY!
					//Note: This can only happen if the FIRST point in points is on the edge, and its
					//pair is the LAST point in points (as in all other cases the first point is 
					//always lower in the array)
					else if(parallelPoints[parallelPoints.length-1] == edgeDetectionValue) {
						//else the x point previous to current x point must be the other xStartEdge contact

						//First, set the xStartFirstPointEdge to true
						//This will stop the points at the end of points being added again:
						FirstPointEdge = true;

						//In this case, add the length from current point (which can only be the FIRST point)
						//and the last point in points:
						EdgeLengths.add( Math.abs(orthogonalPoints[b] - orthogonalPoints[orthogonalPoints.length-1]) );

						//Add this index to xStartEdgeIndex first - to ensure its FIRST in the EdgeIndex:
						EdgeIndexes.add(parallelPoints.length-1);

						//add first index to xStartEdgeIndex arraylist - to add it AFTER the end xStartEdgeIndex:
						EdgeIndexes.add(b);

					}

				} // end if xStartFirstPointEdge true && b is points.length-1

			} //end if points[b].x == xStart

		} //end for b


	}//end findEdges()



	/**
	 * This method will remove points in the parallelPoints and orthogonalPoints arrays to remove any points 
	 * where the edgeValue equals edgePoints values.  It will also remove all the points that are within the 
	 * maxObjLengthXY length of edgePoints.
	 */
	public int[][] adjustEdgeContacts(int maxObjLengthXY) {    		


		int index1 = 0; // used as the first index in the removeExcessPoints() method
						// default value is 0.
		
		//First calculate the length from the edge which the ROI will be cropped to:
		//This will return either a POSITIVE or a NEGATIVE value, which can be used to calculate when
		//values of parallelPoints cross this cropped boundary (see below on logic):
		int buffer = Math.abs(edgeDetectionValue - maxObjLengthXY);


		//IJ.log("EdgeIndexes size: "+EdgeIndexes.size() );
		
		//loop through the EdgeIndexes ArrayList:

		for(int a=0; a<EdgeIndexes.size(); a++) {
			//The first edge contact in a pair found in EdgeIndexes -> must always work DOWN from this point
			//Whereas the second edge contact in a pair found in EdgeIndexes -> must always work UP from this point

			//First, set the index for starting to look through parallelPoints:
			int index = EdgeIndexes.get(a);
			
			//IJ.log("for a: "+a+" EdgeIndexes value: "+index);

			//also store the original index value:
			int originalIndex = index;

			//And create an ArrayIndexer object:
			ArrayIndexer indexer = new ArrayIndexer(parallelPoints.length, index);
			
			//IJ.log("indexer setup - length: "+parallelPoints.length+" start index: "+index);
			
			//IJ.showMessage("EdgeIndexes index: "+a+" indexer setup - length: "+parallelPoints.length+" start index: "+index);

			//if a is EVEN, need to work DOWN the parallelPoints int array, else if a is ODD need to work UP
			//the parallelPoints in array.

			if(a%2 == 0) {
				//if a is EVEN -> work DOWN
				//IJ.log("a is EVEN");

				//first, we KNOW that parallelPoints @index AT PRESENT is on the Edge, therefore we can calculate
				//whether parallelPoints @index as it is traversed should be seen to be ABOVE or BELOW the buffer:
				
				EdgeBreachAnalyser edgeBreachAnalyser = new EdgeBreachAnalyser(parallelPoints[indexer.getIndex()], buffer);
					//boolean bufferBelowEdge = parallelPoints[indexer.getIndex()] > buffer;
				//The above boolean will be true if parallelPoints on the Edge is GREATER than buffer,
				//if parallelPoints on the Edge is LOWER than of equal to buffer, it is false.
				//This boolean value will switch once a value in parallelPoints is EITHER lower or greater
				//than the original -> and this indicate the parallelPoints position in the array where
				//the buffer has been exceeded (either above or below its value)
					// NOTE: if parallelPoints is LOWER than buffer, the switch only happens once parallelPoints
						// is GREATER than buffer, whereas if parallelPoints is greater than buffer, the switch
						// can happen when parallelPoints is EQUAL to or LOWER than buffer - which is a problem
						// with the future computations in removeExcessPoints.
				// Therefore, have created a new class to deal with the computations needed here (that the comparison
					// between parallelPoints and buffer is dependent on their original values) - in the 
					// EdgeBreachedAnalyser class.

				//IJ.log("EVEN index: "+indexer.getIndex()+" buffer: "+buffer+" ParPoint: "+parallelPoints[indexer.getIndex()]
					//+" OrthPoint: "+orthogonalPoints[indexer.getIndex()] );
				
				//IJ.showMessage("EVEN index: "+indexer.getIndex()+" buffer: "+buffer+" ParPoint: "+parallelPoints[indexer.getIndex()]
				//		+" OrthPoint: "+orthogonalPoints[indexer.getIndex()] );

				//Moving DOWN the parallelPoints array, so decrementIndex():
				indexer.decrementIndex();

				//IJ.log("EVEN index: "+indexer.getIndex()+" buffer: "+buffer+" ParPoint: "+parallelPoints[indexer.getIndex()]
					//	+" OrthPoint: "+orthogonalPoints[indexer.getIndex()] );
				
				//IJ.showMessage("EVEN index: "+indexer.getIndex()+" buffer: "+buffer+" ParPoint: "+parallelPoints[indexer.getIndex()]
					//	+" OrthPoint: "+orthogonalPoints[indexer.getIndex()] );

				while(  edgeBreachAnalyser.determineBufferBreached( parallelPoints[indexer.getIndex()] )  ) {
				// while( (parallelPoints[indexer.getIndex()] > buffer) == bufferBelowEdge ) {
					//This while statement will continue to compare parallelPoints values to the buffer.
					//The bufferBelowEdge boolean is true or false depending on whether the buffer value
					//is below the edge value seen in parallelPoints (true), or the buffer value is above
					//the edge value seen in parallelPoints (false).

					//Deriving new booleans based on the index value moving through parallelPoints, as soon
					//as the parallelPoints value goes either above or below the buffer, the boolean will
					//change, and this is detected by comparing it to bufferBelowEdge.

					//Essentially, the above logic embeds the difference between edge value and buffer into the
					//boolean, and then subsequent comparisons of the next value in parallelPoints to buffer
					//is compared to this ORIGINAL boolean -> when it changes, the parallelPoints index where
					//the buffer is breached has been detected

					//To continue the while loop, all that needs to be done here is move DOWN the indexes:
					indexer.decrementIndex();

					//IJ.log("EVEN index: "+indexer.getIndex()+" buffer: "+buffer+" ParPoint: "+parallelPoints[indexer.getIndex()]
						//	+" OrthPoint: "+orthogonalPoints[indexer.getIndex()] );
					
					//IJ.showMessage("EVEN index: "+indexer.getIndex()+" buffer: "+buffer+" ParPoint: "+parallelPoints[indexer.getIndex()]
						//	+" OrthPoint: "+orthogonalPoints[indexer.getIndex()] );

				} //end while loop

				//The index is now the position in which parallelPoints[index] HAS PASSED the buffer threshold.
				
				// if this is the very first value in EdgeIndexes (i.e. a==0) then store the index in indexer
					// for use in the removeExcessPoints() method below:
				if(a == 0) {
					index1 = indexer.getIndex(); // index1 is the index in parallel & orthogonal points where	
				}									// the value of parallel points is ABOVE the buffer value
													// needed to compute the correct boolean value in removeExcessPoints()
				
				// Therefore the LAST POINT which has NOT PASSED the buffer is:
					// int pp1 = parallelPoints[indexer.getIndexPlus1()];
					// int op1 = orthogonalPoints[indexer.getIndexPlus1()];
				
				// since this point and point at indexer.getIndex() should be a straight line in 
				// orthogonal plane to the buffer threshold, want to adjust THIS POINT very simply:
				// JUST MOVE THE PARALLELPOINTS VALUE TO THE BUFFER VALUE.
				
				indexer.incrementIndex();
				
				//IJ.log("indexer incremented to LAST POINT NOT PASSED: "+indexer.getIndex()
					//		+"Par: "+parallelPoints[indexer.getIndex()]+" Orth: "+orthogonalPoints[indexer.getIndex()]);
				
				//IJ.showMessage("indexer incremented to LAST POINT NOT PASSED: "+indexer.getIndex()
				//+"Par: "+parallelPoints[indexer.getIndex()]+" Orth: "+orthogonalPoints[indexer.getIndex()]);
				
				parallelPoints[indexer.getIndex()] = buffer;
				// no need to adjust orthogonalPoints[indexer.getIndex()} - as its already in line with
					// orthogonalPoints[indexer.getIndexMinus1()] - as its an orthogonal line to it!
				
				//IJ.log("Par Points at index: "+indexer.getIndex()+" set to buffer: "+buffer
				//+"Par: "+parallelPoints[indexer.getIndex()]+" Orth: "+orthogonalPoints[indexer.getIndex()]);

				//IJ.log("");
				//IJ.log("Start while loop edge points BELOW buffer:");
				//IJ.showMessage("Start while loop edge points BELOW buffer:");
				// THEN - move up to the original index in indexer, and set every value in Parallel
					// and Orthogonal Points to -1 (to indicate it should be removed:
				while( indexer.moveUpTo( originalIndex ) ) {
					//IJ.log("while loop edge points BELOW Buffer - indexer index: "+indexer.getIndex() );
					//note, first call to moveUpTo() increments the index, so the first index in parallel
					//points which is set to buffer is actually index+1.
					parallelPoints[indexer.getIndex()] = -1;
					orthogonalPoints[indexer.getIndex()] = -1;
					//set orthogonalPoints to the originalCurrentIndex value plus the gradient interpolation amount.
					//IJ.log("  parallelPoints at index set to: "+parallelPoints[indexer.getIndex()]);
					//IJ.log("  orth points at index set to: "+orthogonalPoints[indexer.getIndex()] );
					//IJ.showMessage("while loop edge points BELOW Buffer - indexer index: "+indexer.getIndex()
					//+" P: "+parallelPoints[indexer.getIndex()]+" O: "+orthogonalPoints[indexer.getIndex()]);
				}
				//IJ.log("");
				
				//IJ.log("After While Loop to set edge points below buffer to -1 - indexer index is: "+indexer.getIndex() );
				
				// Finally, in this part of the for loop (going over the first index pair in the EdgeIndexes array)
					// want to set any points ON the Edge BETWEEN ind1 and ind2 (EdgeIndexes 1st and 2nd in a pair)
					// all to -1:
					// USE THE INDEXER:
				// first set indexer 1st index to ind1+1 - EdgeIndexes.get(a) + 1:
				indexer.setIndex( EdgeIndexes.get(a) + 1 );
				
				//IJ.log("indexer set to EdgeIndexes(a): "+indexer.getIndex() );
				
				//IJ.showMessage("indexer set to EdgeIndexes(a): "+indexer.getIndex() 
				//					+" indexer length: "+indexer.getLength() );
				
				//IJ.log("Start while loop edge points BELOW buffer - moveUpTo: "+EdgeIndexes.get(a+1) );
				
				//IJ.showMessage("Start while loop edge points BELOW buffer -moveUpFrom: "+indexer.getIndex()+
				//				" moveUpTo: "+EdgeIndexes.get(a+1) );
				// then use while loop to move through all indexes up to the second index in the pair, which
					// is at EdgeIndexes.get(a+1):
				while(  ( ! indexer.indexEquals( EdgeIndexes.get(a+1) )  )  ) {
					//IJ.showMessage("while loop Points ON EDGE - indexer index: "+indexer.getIndex()+
					//		" P: "+parallelPoints[indexer.getIndex()]+" O: "+orthogonalPoints[indexer.getIndex()]);
					//IJ.log("while loop Points ON EDGE - indexer index: "+indexer.getIndex() );
					//note, first call to moveUpTo() increments the index, so the first index in parallel
					//points which is set to buffer is actually index+1.
					parallelPoints[indexer.getIndex()] = -1;
					orthogonalPoints[indexer.getIndex()] = -1;
					//set orthogonalPoints to the originalCurrentIndex value plus the gradient interpolation amount.
					//IJ.log("  parallelPoints at index set to: "+parallelPoints[indexer.getIndex()]);
					//IJ.log("  orth points at index set to: "+orthogonalPoints[indexer.getIndex()] );
					indexer.incrementIndex();
				}
				
				//Now want to adjust the values in parallelPoints and orthogonalPoints
				//to make all the points in the Exclusion Zone move up to the buffer:
				//Need to make sure all points are now on the new ROI - not outside it:
				//Need to draw a straight line from parallelPoints[indexer.getIndex()] and
				//parallelPoints[indexer.getIndexPlus1()], which ends at the buffer value:
				//This means set parallelPoints[indexer.getIndex()] to buffer, 
				//and ADJUST the other array,
				//orthogonalPoints[indexer.getIndexPlus1()] to a value calculated to 
				//be linearly mapped on the line
				//which links the original parallelPoints[index] and parallelPoints[index+1]

				// First set all points in parallelPoints
				//from index+1 to originalIndex to buffer, and then set the value at index+1 in 
				//orthogonalPoints to the linearly mapped value which brings the value at index+1, along 
				//with buffer set at parallelPoints[index+1] to lie on the original line from index to 
				//index+1:

				//for orthogonalPoints values, first determine the straight line gradient from index to 
				//index+1:

				//int abscissa = parallelPoints[indexer.getIndexPlus1()] - parallelPoints[indexer.getIndex()];
				//int ordinate = orthogonalPoints[indexer.getIndexPlus1()] - orthogonalPoints[indexer.getIndex()];

				//IJ.log("abscissa: "+abscissa+" PP+1: "+parallelPoints[indexer.getIndexPlus1()]+" PP: "
				//		+parallelPoints[indexer.getIndex()] );
				//IJ.log("ordinate:"+ordinate+" OP+1: "+orthogonalPoints[indexer.getIndexPlus1()]+" OP: "
				//		+orthogonalPoints[indexer.getIndex()] );
				//gradient must be MINUS when the buffer is below the edge (i.e. the Edge is on the right or
				//bottom of the image:
				//double gradient;

				//if(bufferBelowEdge) {
				//if buffer is below edge, want to move OPPOSITE to the gradient
				//Take the NEGATIVE value of the gradient:
				//gradient = 0 - (ordinate / abscissa);
				//}
				//else { 
				//if buffer is above edge, want to move THE SAME AS the gradient
				//Take gradient as it is:
				//gradient = ordinate / abscissa;
				//}

				//and calculate the number between parallelPoints[index+1] and buffer:

				//Need to take into account which direction the buffer edge is relative to actual edge:
				//int pointToBuffer;
				//if(bufferBelowEdge) {
				//if buffer is below (right/bottom of image), minus buffer from parallelPoints[index+1]
				//pointToBuffer = parallelPoints[indexer.getIndexPlus1()] - buffer;
				//}
				//else {
				//if buffer is above (left/top of image), minus parallelPoints[index+1] from buffer
				//pointToBuffer = buffer - parallelPoints[indexer.getIndexPlus1()];
				//}

				//Above is a fancy logical way to calculate the orthogonal length, but actually, the same
				//can be achieved by doing:
				//gradient = (double)ordinate / abscissa;

				//pointToBuffer = parallelPoints[indexer.getIndexPlus1()] - buffer;
				
				//IJ.log("gradient: "+gradient);
				//IJ.log("pointToBuffer: "+pointToBuffer);

				//This is because when the buffer is below edge, pointsToBuffer is positive, but
				//when buffer is greater than edge, pointsToBuffer is negative.
				//This will then modify the gradient when multiplied by pointsToBuffer to yield the
				//correct modification of the int value in orthogonalPoint[index+1]


				//Finally, set both parallelPoints & orthogonalPoints form index+1 to originalIndex to 
				//appropriate values:

				//The moveUpTo() method will keep incrementing index in indexer (using indexer logic)
				//until the index in indexer equals the parsed value, originalIndex.
				//Each time the moveUpTo() method is called, and returns false,

				//To ensure correct orthogonal points is referenced in below equation on right, need to extract the current
				//indexer index and use THIS ONLY
				//Otherwise the ROI if on a jagged line, will have a lagging line...
				//int currentIndex = indexer.getIndex();
				//IJ.log("currentIndex: "+currentIndex);
				//while( indexer.moveUpTo(originalIndex) ) {
				//	IJ.log("while loop - indexer index: "+indexer.getIndex() );
					//note, first call to moveUpTo() increments the index, so the first index in parallel
					//points which is set to buffer is actually index+1.
				//	parallelPoints[indexer.getIndex()] = buffer; //set parallelPoints to buffer value
				//	orthogonalPoints[indexer.getIndex()] = orthogonalPoints[currentIndex] + (int)(gradient*pointToBuffer);
					//set orthogonalPoints to the originalCurrentIndex value plus the gradient interpolation amount.
				//	IJ.log("  parallelPoints at index set to: "+buffer);
				//	IJ.log("  orth points at index set to: orth[currentIndex]: "+orthogonalPoints[currentIndex]+
				//			"grad x pointToBuffer: "+(int)(gradient*pointToBuffer) );
				//}

			} //end if a is EVEN
			else if(a%2 == 1) {
				//if a is ODD -> work UP parallelPoints:
				//IJ.log("a is ODD");

				//first, we KNOW that parallelPoints[index] AT PRESENT is on the Edge, therefore we can calculate
				//whether parallelPoints[index] as it is traversed should be seen to be ABOVE or BELOW the buffer:

				EdgeBreachAnalyser edgeBreachAnalyser = new EdgeBreachAnalyser(parallelPoints[indexer.getIndex()], buffer);
				//boolean bufferBelowEdge = parallelPoints[indexer.getIndex()] > buffer;
				//The above boolean will be true if parallelPoints on the Edge is GREATER than buffer,
				//if parallelPoints on the Edge is LOWER than buffer, it is false.
				//This boolean value will switch once a value in parallelPoints is EITHER lower or greater
				//than the original -> and this indicate the parallelPoints position in the array where
				//the buffer has been exceeded (either above or below its value)

				//IJ.log("ODD index: "+indexer.getIndex()+" ParPoint: "+parallelPoints[indexer.getIndex()]
					//+" OrthPoint: "+orthogonalPoints[indexer.getIndex()] );
				
				//IJ.showMessage("ODD index: "+indexer.getIndex()+" buffer: "+buffer+" ParPoint: "+parallelPoints[indexer.getIndex()]
				//		+" OrthPoint: "+orthogonalPoints[indexer.getIndex()] );

				//Moving UP the parallelPoints array, so incrementIndex():
				indexer.incrementIndex();

				//IJ.log("ODD index: "+indexer.getIndex()+" ParPoint: "+parallelPoints[indexer.getIndex()]
					//+" OrthPoint: "+orthogonalPoints[indexer.getIndex()] );
				
				//IJ.showMessage("ODD index: "+indexer.getIndex()+" buffer: "+buffer+" ParPoint: "+parallelPoints[indexer.getIndex()]
				//		+" OrthPoint: "+orthogonalPoints[indexer.getIndex()] );

				while(  edgeBreachAnalyser.determineBufferBreached( parallelPoints[indexer.getIndex()] )  ) {
				//while( (parallelPoints[indexer.getIndex()] > buffer) == bufferBelowEdge ) {
					//This while statement will continue to compare parallelPoints values to the buffer.
					//The bufferBelowEdge boolean is true or false depending on whether the buffer value
					//is below the edge value seen in parallelPoints (true), or the buffer value is above
					//the edge value seen in parallelPoints (false).

					//Deriving new booleans based on the index value moving through parallelPoints, as soon
					//as the parallelPoints value goes either above or below the buffer, the boolean will
					//change, and this is detected by comparing it to bufferBelowEdge.

					//Essentially, the above logic embeds the difference between edge value and buffer into the
					//boolean, and then subsequent comparisons of the next value in parallelPoints to buffer
					//is compared to this ORIGINAL boolean -> when it changes, the parallelPoints index where
					//the buffer is breached has been detected

					//To continue the while loop, all that needs to be done here is move UP the indexes:
					indexer.incrementIndex();

					//IJ.log("ODD index: "+indexer.getIndex()+" ParPoint: "+parallelPoints[indexer.getIndex()]
						//+" OrthPoint: "+orthogonalPoints[indexer.getIndex()] );
					
					//IJ.showMessage("ODD index: "+indexer.getIndex()+" buffer: "+buffer+" ParPoint: "+parallelPoints[indexer.getIndex()]
					//		+" OrthPoint: "+orthogonalPoints[indexer.getIndex()] );

				} //end while loop

				//The index is now the position in which parallelPoints[index] has passed the buffer threshold.

				// Therefore the LAST POINT which has NOT PASSED the buffer is:
				// int pp1 = parallelPoints[indexer.getIndexPlus1()];
				// int op1 = orthogonalPoints[indexer.getIndexPlus1()];

				// since this point and point at indexer.getIndex() should be a straight line in 
				// orthogonal plane to the buffer threshold, want to adjust THIS POINT very simply:
				// JUST MOVE THE PARALLELPOINTS VALUE TO THE BUFFER VALUE.

				indexer.decrementIndex();

				//IJ.log("indexer incremented to LAST POINT NOT PASSED: "+indexer.getIndex()
				//+"Par: "+parallelPoints[indexer.getIndex()]+" Orth: "+orthogonalPoints[indexer.getIndex()]);
				
				//IJ.showMessage("indexer incremented to LAST POINT NOT PASSED: "+indexer.getIndex()
				//+"Par: "+parallelPoints[indexer.getIndex()]+" Orth: "+orthogonalPoints[indexer.getIndex()]);

				parallelPoints[indexer.getIndex()] = buffer;
				// no need to adjust orthogonalPoints[indexer.getIndex()} - as its already in line with
				// orthogonalPoints[indexer.getIndexMinus1()] - as its an orthogonal line to it!

				//IJ.log("Par Points at index: "+indexer.getIndex()+" set to buffer: "+buffer
				//		+"Par: "+parallelPoints[indexer.getIndex()]+" Orth: "+orthogonalPoints[indexer.getIndex()]);

				//IJ.log("");
				//IJ.log("Start while loop edge points BELOW buffer:");
				
				//IJ.showMessage("Start while loop edge points BELOW buffer:");
				// THEN - move up to the original index in indexer, and set every value in Parallel
				// and Orthogonal Points to -1 (to indicate it should be removed:
				while( indexer.moveDownTo(originalIndex) ) {
					//IJ.log("while loop edge points BELOW Buffer - indexer index: "+indexer.getIndex() );
					//note, first call to moveUpTo() increments the index, so the first index in parallel
					//points which is set to buffer is actually index+1.
					parallelPoints[indexer.getIndex()] = -1;
					orthogonalPoints[indexer.getIndex()] = -1;
					//set orthogonalPoints to the originalCurrentIndex value plus the gradient interpolation amount.
					//IJ.log("  parallelPoints at index set to: "+parallelPoints[indexer.getIndex()]);
					//IJ.log("  orth points at index set to: "+orthogonalPoints[indexer.getIndex()] );
					//IJ.showMessage("while loop edge points BELOW Buffer - indexer index: "+indexer.getIndex()+
					//		" P: "+parallelPoints[indexer.getIndex()]+" O: "+orthogonalPoints[indexer.getIndex()] );
				}
				//IJ.log("");

				//IJ.log("After While Loop to set edge points below buffer to -1 - indexer index is: "+indexer.getIndex() );
				
				
				
				
				

				//Now want to adjust the values in parallelPoints and orthogonalPoints
				//to make all the points in the Exclusion Zone move up to the buffer:
				//Need to make sure all points are now on the new ROI - not outside it:
				//Need to draw a straight line from parallelPoints[indexer.getIndex()] and
				//parallelPoints[indexer.getIndexMinus1()], which ends at the buffer value:
				//This means set parallelPoints[indexer.getIndex()] to buffer, 
				//and ADJUST the other array,
				//orthogonalPoints[indexer.getIndexMinus1()] to a value calculated to 
				//be linearly mapped on the line
				//which links the original parallelPoints[index] and parallelPoints[index-1]

				// First set all points in parallelPoints
				//from index-1 to originalIndex to buffer, and then set the value at index-1 to 
				//originalIndex in orthogonalPoints to the linearly mapped value which brings the 
				//value at index-1, along with buffer set at parallelPoints[index-1] to lie on the 
				//original line from index to index-1:

				//for orthogonalPoints values, first determine the straight line gradient from index to 
				//index-1:

				//int abscissa = parallelPoints[indexer.getIndexMinus1()] - parallelPoints[indexer.getIndex()];
				//int ordinate = orthogonalPoints[indexer.getIndexMinus1()] - orthogonalPoints[indexer.getIndex()];
				
				//IJ.log("abscissa: "+abscissa);
				//IJ.log("ordinate:"+ordinate);

				//gradient must be MINUS when the buffer is below the edge (i.e. the Edge is on the right or
				//bottom of the image:
				//double gradient;

				//if(bufferBelowEdge) {
				//if buffer is below edge, want to move OPPOSITE to the gradient
				//Take the NEGATIVE value of the gradient:
				//gradient = 0 - (ordinate / abscissa);
				//}
				//else { 
				//if buffer is above edge, want to move THE SAME AS the gradient
				//Take gradient as it is:
				//gradient = ordinate / abscissa;
				//}

				//and calculate the number between parallelPoints[index+1] and buffer:

				//Need to take into account which direction the buffer edge is relative to actual edge:
				//int pointToBuffer;
				//if(bufferBelowEdge) {
				//if buffer is below (right/bottom of image), minus buffer from parallelPoints[index+1]
				//pointToBuffer = parallelPoints[indexer.getIndexMinus1()] - buffer;
				//}
				//else {
				//if buffer is above (left/top of image), minus parallelPoints[index+1] from buffer
				//pointToBuffer = buffer - parallelPoints[indexer.getIndexMinus1()];
				//}

				//Above is a fancy logical way to calculate the orthogonal length, but actually, the same
				//can be achieved by doing:
				//gradient = (double)ordinate / abscissa;

				//pointToBuffer = parallelPoints[indexer.getIndexMinus1()] - buffer;
				
				//IJ.log("gradient: "+gradient);
				//IJ.log("pointToBuffer: "+pointToBuffer);

				//This is because when the buffer is below edge, pointsToBuffer is positive, but
				//when buffer is greater than edge, pointsToBuffer is negative.
				//This will then modify the gradient when multiplied by pointsToBuffer to yield the
				//correct modification of the int value in orthogonalPoint[index-1]


				//Finally, set parallelPoints form index-1 to originalIndex to buffer:

				//The moveUpTo() method will keep incrementing index in indexer (using indexer logic)
				//until the index in indexer equals the parsed value, originalIndex.
				//Each time the moveUpTo() method is called, and returns false,

				//to ensure correct orthogonal points is referenced in below equation on right, need to extract the current
				//indexer index and use THIS ONLY
				//Otherwise the ROI if on a jagged line, will have a lagging line...
				//int currentIndex = indexer.getIndex();	
				//IJ.log("currentIndex: "+currentIndex);
				//while( indexer.moveDownTo(originalIndex) ) {
					//IJ.log("while loop - indexer index: "+indexer.getIndex() );
					//note, first call to moveUpTo() increments the index, so the first index in parallel
					//points which is set to buffer is actually index+1.
					//parallelPoints[indexer.getIndex()] = buffer;
					//orthogonalPoints[indexer.getIndex()] = orthogonalPoints[currentIndex] + (int)(gradient*pointToBuffer);
					//IJ.showMessage("Orthogonal Points ind: "+indexer.getIndex()+" val: "+orthogonalPoints[indexer.getIndex()] );
					//IJ.log("  parallelPoints at index set to: "+buffer);
					//IJ.log("  orth points at index set to: orth[currentIndex]: "+orthogonalPoints[currentIndex]+
					//		"grad x pointToBuffer: "+(int)(gradient*pointToBuffer) );
				//}    				

			} //end if a is ODD


		} //end EdgeIndexes for a loop

		//IJ.showMessage("removeExcessPoints index1: "+index1+" buffer: "+buffer);
		// final steps - after all EdgePoints have been dealt with:
		return removeExcessPoints(index1, buffer); // this will remove all excess points - those points in parallel and orthogonal
								// points which were set to -1 
							// & it will set any points running below the buffer value to the buffer value.
								


	} //end adjustEdgeContacts()

	
	/**
	 * Will remove all instances where parallel and orthogonal points arrays equal -1, and it will also
	 * remove all points where parallel points breaches the buffer value.
	 * @param index1
	 */
	public int[][] removeExcessPoints(int index1, int buffer) {
		
		// determine the boolean value when comparing parallel points at index1 with buffer:
		//boolean bufferAboveEdge = parallelPoints[index1] >= buffer;
		EdgeBreachAnalyser edgeBreachAnalyser = new EdgeBreachAnalyser(parallelPoints[index1], buffer);
			// this is used to detect when a value in parallel points breaches the buffer, as we
				// know the value of parallelPoints at index1 is a point ABOVE the buffer line.
		//IJ.showMessage("bufferAbove Edge: "+edgeBreachAnalyser.bufferBreached+" index1: "+index1+" P: "+parallelPoints[index1]+" buffer: "+buffer);
		//IJ.log("bufferAbove Edge: "+edgeBreachAnalyser.bufferBreached+" index1: "+index1+" P: "+parallelPoints[index1]+" buffer: "+buffer);
		
		// loop through all parallelPoints and add the correct values to a new ArrayList of integers
		ArrayList<Integer> parPoints = new ArrayList<Integer>();
		ArrayList<Integer> orthPoints = new ArrayList<Integer>();
		
		// create a new indexer object:
		ArrayIndexer indexer = new ArrayIndexer(parallelPoints.length, index1);
		
		do {
			
			//IJ.showMessage("indexer index: "+indexer.getIndex()+" P: "+parallelPoints[indexer.getIndex()]+
			//				" O: "+orthogonalPoints[indexer.getIndex()]);
			
			// determine if the current parallelPoints value is ABOVE buffer:
			if(parallelPoints[indexer.getIndex()] == -1) {
				// first - if parallelPoints[a] is -1, it should be omitted
				// therefore leave this blank...
				//IJ.log("omitted - index: "+indexer.getIndex()+" ParPoints: "+parallelPoints[indexer.getIndex()]);
				indexer.incrementIndex();
			}
			else if(  edgeBreachAnalyser.determineBufferBreached( parallelPoints[indexer.getIndex()] )  ) {
			// else if( (parallelPoints[indexer.getIndex()] >= buffer) == bufferAboveEdge ) {
				//if true, the parallelPoints value is ABOVE OR EQUAL to the buffer - so want to keep this value:
				//IJ.log("added - index: "+indexer.getIndex()+
				//		" ParPoints: "+parallelPoints[indexer.getIndex()]+
				//		" OrthPoints: "+orthogonalPoints[indexer.getIndex()] );
				parPoints.add(parallelPoints[indexer.getIndex()]);
				orthPoints.add(orthogonalPoints[indexer.getIndex()]);
				indexer.incrementIndex();
			}
			else {
				//IJ.log("amend points: index: "+indexer.getIndex() );
				// else, the value in parallelPoints is BELOW the buffer, but not -1, 
					// and so the points in parallelPoints must be amended as appropriate:
				indexer.setIndex(  amendParPointsBelowBuffer( indexer.getIndex(), buffer, parPoints, orthPoints )  );
					// this returns the new index, after some points have been skipped during the amendment.
						// amendment is where points below buffer, except the first and last, are REMOVED
			}
			
		} while( ( !indexer.indexEquals(index1) ) );
		
		// once the do... while loop is complete, parPoints and orthPoints both contain the new set of points
			// which should be set to parallelPoints and orthogonalPoints arrays:
		
		return setArrays(parPoints, orthPoints);
		
	}
	
	/**
	 * This method re-fills the parallelPoints and orthogonalPoints arrays with the new set of points
	 * in parPoints and orthPoints, ArrayList<Integer> objects passed to this method.
	 * @param parPoints
	 * @param orthPoints
	 */
	public int[][] setArrays(ArrayList<Integer> parPoints, ArrayList<Integer> orthPoints) {
		
		parallelPoints = new int[parPoints.size()];
		orthogonalPoints = new int[orthPoints.size()];
		
		for(int a=0; a<parPoints.size(); a++) {
			parallelPoints[a] = parPoints.get(a);
			orthogonalPoints[a] = orthPoints.get(a);
			//IJ.log("new index:"+a+" parallelPoints: "+parallelPoints[a]+
			//		" orthogonalPoints: "+orthogonalPoints[a]);
		}
		
		return new int[][]{parallelPoints, orthogonalPoints};
		
	}
	
	
	public int amendParPointsBelowBuffer(int currentIndex, int buffer, 
											ArrayList<Integer> parPoints, ArrayList<Integer> orthPoints) {
		
		// find the first point below buffer - which is currentIndex - and the last point below buffer, which is
			// the point before the Points above buffer.
		
		//IJ.log("add FIRST point on BUFFER line: parPoints: "+buffer+
		//				" orthPoints: "+orthogonalPoints[currentIndex]);
		// first, add the first point below buffer, but adjust the parallelPoints to the buffer value:
		parPoints.add(buffer);
		orthPoints.add(orthogonalPoints[currentIndex]);

		// generate a boolean used to determine when the parallelPoints array goes above buffer again:
		//boolean bufferBelowEdge = parallelPoints[currentIndex] >= buffer;
		EdgeBreachAnalyser edgeBreachAnalyser = new EdgeBreachAnalyser(parallelPoints[currentIndex], buffer);
			// this will be true or false, depending on where the buffer value sits relative to the edge.
		//IJ.log("bufferBelowEdge: "+edgeBreachAnalyser.bufferBreached+" buffer: "+buffer+
		//		" parallelPoints[currentIndex: "+parallelPoints[currentIndex]);
		
		// create a new indexer object:
		ArrayIndexer indexer = new ArrayIndexer(parallelPoints.length, currentIndex);
		
		//IJ.log("indexer - initial index: "+indexer.getIndex() );
		// use a while loop to find the index where parallelPoints moves above the buffer:
		while(  edgeBreachAnalyser.determineBufferBreached( parallelPoints[indexer.getIndex()] )  ) {
		// while(  (parallelPoints[indexer.getIndex()] >= buffer) == bufferBelowEdge  ) {
			indexer.incrementIndex();
			//IJ.log("While Loop: index: "+indexer.getIndex() + " parallelPoints[ind]: "+parallelPoints[indexer.getIndex()]);
		}
		
		// at this point, indexer's index is equal to the first point ABOVE the buffer, to get the last point
			// BELOW the buffer, must decrement the indexer:
		indexer.decrementIndex();
		
		// Then, add the points in parallel and orthogonal points at this index to parPoints and orthPoints,
			// remembering to set parPoints to buffer:
		parPoints.add(buffer);
		orthPoints.add( orthogonalPoints[ indexer.getIndex() ] );
		
		//IJ.log("add FIRST point on BUFFER line: parPoints: "+buffer+
		//		" orthPoints: "+orthogonalPoints[indexer.getIndex()]);
		
		// finally, set currentIndex to the indexer index + 1:
		currentIndex = indexer.getIndex() + 1;
		
		//IJ.log("Return index: "+currentIndex );
		
		// and return this index - which is the first index where the parallelPoints value exceeds buffer:
		return currentIndex;
		
	}
	


}
