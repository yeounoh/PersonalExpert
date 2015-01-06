/**
 * (Memory efficient) Sparse representation of row-oriented matrix. 
 * @author Yeounoh Chung
 *
 */
public interface SparseMatrix {

	/**
	 * 
	 * @param row can be userID
	 * @param col can be itemID
	 * @return Object can contain int rating & long timestamp
	 */
	public Object getEntry(int row, int col);
	
	/**
	 * 
	 * @param row
	 * @return a row containing sparse entries (e.g. zeros)
	 */
	public Object[] getRow(int row);
}
