public class EntryInfo {
	private byte rating=0;
	private int timestamp=0;

	/**
	 * get a rating
	 * @param 
	 * @return int rating
	 */
	public int getRating() {
		return (int)rating;
	}

	/**
	 * set a rating
	 * @param int rating 
	 * @return
	 */
	public void setRating(int rating) {
		this.rating = (byte)rating;
	}

	/**
	 * get a timestamp
	 * @param 
	 * @return int timestamp
	 */
	public int getTimestamp() {
		return timestamp;
	}

	/**
	 * set a timestamp
	 * @param int timestamp 
	 * @return
	 */
	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}
	
}
