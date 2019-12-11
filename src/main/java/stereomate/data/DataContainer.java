package stereomate.data;

public interface DataContainer {
	

	/**
	 * This returns a 2-D Object array, the first dimension is always of size 2:  the FIRST 1D Object[]
	 * array contains the data VALUES, and the SECOND 1D Object[] array contains each datapoints title
	 * (this is the Attribute title for Instances objects).
	 * @return a 2D Objects array, containing two 1D Object arrays of data value and datapoint titles.
	 */
	public double[] returnData();
	
	public String[] returnDataTitles();

	
}
