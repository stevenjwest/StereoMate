package stereomate.roi;

/**
 * This class ensures that iterations through indexes of an array are never out of bounds.  It ensures
 * that if an index reaches 0, the next 'previous' index is length-1, and if an index reaches length-1,
 * the next 'next' index is 0.
 * 
 * @author stevenwest
 *
 */
public class ArrayIndexer {

	int length;
	int index;

	/**
	 * Constructor.  Saves the array length and starting index to the instance variables.
	 * @param length Length of the array.
	 * @param index Index at which to start counting from.
	 */
	public ArrayIndexer(int length, int index) {
		this.length = length;
		this.index = index;
	}

	/**
	 * Returns the current index.
	 * @return Current index.
	 */
	public int getIndex() {
		return index;
	}
	
	/**
	 * Sets the new index - if the index set is greater than or equal to length, the index
	 * is set to 0.
	 * @param index
	 */
	public void setIndex(int index) {
		this.index = index;
		if(index >= length) {
			this.index = 0;
		}
	}
	
	/**
	 * Returns the ArrayIndexer Length.
	 * @return
	 */
	public int getLength() {
		return length;
	}
	
	/**
	 * Set the maximum length of this ArrayIndexer - if the index is currently above or equal to
	 * the NEW length, the index is set to length-1.
	 * @param length
	 */
	public void setLength(int length) {
		this.length = length;
		if(index >=length) {
			index = length-1;
		}
	}


	/**
	 * Increments the index value.  If index reaches array length, the index is reset to 0.
	 * 
	 */
	public void incrementIndex() {
		index = index + 1;
		if(index == length) {
			index = 0;
		}
	}


	/**
	 * Decrements the index value.  If index reaches -1, the index is reset to (length-1).
	 * 
	 */
	public void decrementIndex() {
		index = index -1;
		if(index == -1) {
			index = length-1;
		}
	}


	/**
	 * Increments the index and returns its value.  If index reaches array length, the index is reset to 0.
	 * @return the index after incrementing it.
	 */
	public int incrementIndexAndReturn() {
		index = index + 1;
		if(index == length) {
			index = 0;
		}
		return index;
	}


	/**
	 * Decrements the index and returns its value.  If index reaches -1, the index is reset to (length-1).
	 * @return the index after decrementing it.
	 */
	public int decrementIndexAndReturn() {
		index = index - 1;
		if(index == -1) {
			index = length-1;
		}
		return index;
	}


	/**
	 * Returns the index plus one WITHOUT incrementing the index value.
	 * @return the next index up from the index value.
	 */
	public int getIndexPlus1() {
		int indexPlus = index + 1;
		if(indexPlus == length) {
			indexPlus = 0;
		}
		return indexPlus;
	}
	
	/**
	 * Returns the index plus one WITHOUT incrementing the index value.
	 * @return the next index up from the index value.
	 */
	public int getIndexPlus(int increment) {
		int indexPlus = index + increment;
		if(indexPlus == length) {
			indexPlus = 0;
		}
		return indexPlus;
	}


	/**
	 * Returns the index minus one WITHOUT decrementing the index value.
	 * @return the next index down from the index value.
	 */
	public int getIndexMinus1() {
		int indexMinus = index - 1;
		if(indexMinus == -1) {
			indexMinus = length-1;
		}
		return indexMinus;
	}


	/**
	 * This will return false if the current index is equal to the target index. Otherwise
	 * it will increment the index, and return true.  Note, calls to this method will increment
	 * the index as well as return true until the target index is reached!
	 * @param targetIndex The int which the current index is testing against.
	 * @return
	 */
	public boolean moveUpTo(int targetIndex) {

		//if index equals the targetIndex, just return true
		//No need to increment the index, as the target has been reached!
		if(index == targetIndex) {
			return false;
		}
		//else, increment the index (using the logic in this class!)
		//and return false:
		incrementIndex();
		return true;
	}


	/**
	 * This will return false if the current index is equal to the target index. Otherwise
	 * it will decrement the index, and return true.  Note, calls to this method will decrement
	 * the index as well as return true until the target index is reached in that order!  So first
	 * call to this will bring index down one int, and the last int called is the targetIndex.
	 * <p>
	 * Therefore, when using this for loops from 0 to [length], use a 'do... while' loop.
	 * @param targetIndex The int which the current index is testing against.
	 * @return
	 */
	public boolean moveDownTo(int targetIndex) {

		//if index equals the targetIndex, just return true
		//No need to increment the index, as the target has been reached!
		if(index == targetIndex) {
			return false;
		}
		//else, increment the index (using the logic in this class!)
		//and return false:
		decrementIndex();
		return true;
	}


	/**
	 * This returns true if index equals the target index.
	 * @param targetIndex Index which is tested.
	 * @return Boolean whether targetIndex equals index or not.
	 */
	public boolean indexEquals(int targetIndex) {
		if(index == targetIndex) {
			return true;
		}
		return false;
	}

}
