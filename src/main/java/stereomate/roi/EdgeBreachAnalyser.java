package stereomate.roi;

public class EdgeBreachAnalyser {
	boolean edgeAboveBuffer, bufferBreached;
	int edge, buffer;
	
	/**
	 * Only Constructor.  The edge value (the value where the edge exists) and the buffer value
	 * (the value where the buffer edge exists) are passed into this object.  Note, the edge and
	 * buffer MUST be different values for this class to function correctly.  It assumes the default
	 * values of edge and buffer are such that the buffer has NOT been breached by the edge.
	 * <p>
	 * The constructor also determines whether the edge is larger or smaller than the buffer, which
	 * is encoded into the instance variable, edgeAboveBuffer.  This boolean is used to determine
	 * what type of comparison (either < or >) should be made between the edge and buffer to determine
	 * when the buffer edge has been breached (since it can be breached from above or below, depending
	 * on whether edge is greater or lower than buffer).
	 * <p>
	 * Finally, the bufferBreached boolean is calculated in the method determineBufferBreached - which will
	 * determine the baseline boolean value for whether the edge has breached the buffer value (which of 
	 * course is initially false).
	 * @param edge
	 * @param buffer
	 */
	public EdgeBreachAnalyser(int edge, int buffer) {
		
		// set instance variables:
		this.edge = edge;
		this.buffer = buffer;
		
		// determine whether edge is above buffer:
		if(edge>buffer) {
			edgeAboveBuffer = true;
		}
		else {
			edgeAboveBuffer = false;
		}
		
		// determine the edgeBreached boolean:
		determineBufferBreached();
		
	}
	
	/**
	 * This determines the original bufferBreached boolean. This boolean encodes whether the
	 * buffer is currently breached by the edge.  Since the construction assumes that the
	 * edge does not breach the buffer, this will be false.  However, the comparison
	 * needed to determine this (whether edge<buffer, or edge>buffer) is determined by
	 * the values of edge and buffer, which is encoded in the edgeAboveBuffer boolean.
	 */
	public void determineBufferBreached() {
		if(edgeAboveBuffer == true) {
			// edge is greater than buffer, so want to set edgeBreached to boolean comparing
				// whether edge is < than buffer:
			bufferBreached = edge < buffer;
		}
		else {
			// edge is lower than buffer, so want to set edgeBreached to boolean comparing
				// whether edge is > than buffer
			bufferBreached = edge > buffer;
		}
	}
	
	/**
	 * Returns the boolean value which indicates if the buffer has been breached with the
	 * newEdgeValue - i.e if the buffer value has been crossed by the newEdgeValue, when
	 * considering the original edge value.
	 * @param newEdgeValue
	 * @return
	 */
	public boolean determineBufferBreached(int newEdgeValue) {
		
		if(edgeAboveBuffer == true) {
			// if the original edge value was greater than buffer, need to compare
			// the boolean of (newEdgeValue < buffer) to the previously determined
			// bufferBreached value (which was determined with edge < buffer - which
				// would have to be FALSE).
			// therefore if its TRUE that newEdgeValue is lower than buffer, this
				// expression will return FALSE, otherwise, both booleans are false,
				// in which case this expression will return TRUE:
			return ( (newEdgeValue < buffer) == bufferBreached );
			
		}
		else {
			// if the original edge value was lower than buffer, need to compare
			// the boolean of (newEdgeValue > buffer) to the previously determined
			// bufferBreached value (which was determined with edge > buffer - which
				// would have to be FALSE).
			// therefore if its TRUE that newEdgeValue is greater than buffer, this
				// expression will return FALSE, otherwise, both booleans are false,
				// in which case this expression will return TRUE:
			return ( (newEdgeValue > buffer) == bufferBreached );
			
		}
		
	}
	
}
