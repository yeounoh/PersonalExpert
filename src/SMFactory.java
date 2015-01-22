import java.util.HashMap;
import java.util.Map.Entry;

/**
 * (Memory efficient) Sparse representation of row-oriented matrix. 
 * @author Noo-ri Kim, 
 * @author Yeounoh Chung
 *
 */

public class SMFactory implements SparseMatrix {
	
	private HashMap<Integer, HashMap> userList = new HashMap<Integer, HashMap>();
	private int userID, itemID, timestamp;
	private double rating; //rating can be double
	private int maxItemID=-1;
	private int maxUserID=-1;
	
	public SMFactory(int nuser, int nitem){
		this.maxItemID = nitem;
		this.maxUserID = nuser;
	}
	
	public SMFactory(int nuser, int nitem, HashMap<Integer, HashMap> userList){
		this.maxItemID = nitem;
		this.maxUserID = nuser;
		this.userList = userList;
	}
	
	/**
	 * Insert ratings from each line
	 * @param String line: read line 
	 * @return int the number of inserted ratings
	 */
	public int insertRating(String line) {
		String[] result = line.split(","); 					// 0: userID
		userID = Integer.parseInt(result[0]);

		for(int i=1; i<result.length; i++) {
			String[] eachItem = result[i].split(":");		// 0: ItemID, 1: rating
			itemID = Integer.parseInt(eachItem[0]);
			rating = Double.parseDouble(eachItem[1]); 
			
			if(userList.containsKey(userID-1) && userList.get(userID-1).containsKey(itemID-1)) {	// user ok, item ok
				((EntryInfo)userList.get(userID-1).get(itemID-1)).setRating(rating);
			}
			else if(userList.containsKey(userID-1) && !userList.get(userID-1).containsKey(itemID-1)) {	// user ok, item empty
				EntryInfo ItemInfo = new EntryInfo();
				ItemInfo.setRating(rating);
				//ItemInfo.setTimestamp(0);
				userList.get(userID-1).put(itemID-1, ItemInfo);
			}
			else {	// user empty
				EntryInfo ItemInfo = new EntryInfo();
				ItemInfo.setRating(rating);
				//ItemInfo.setTimestamp(0);
				HashMap<Integer, EntryInfo> ratings = new HashMap<Integer, EntryInfo>();
				ratings.put(itemID-1, ItemInfo);
				userList.put(userID-1, ratings);
			}
		}
		
		return result.length - 1;
	}
	
	public void insertRating(int row, int col, double rating){
		int userID = row+1;
		int itemID = col+1;
		
		if(userList.containsKey(userID-1) && userList.get(userID-1).containsKey(itemID-1)) {	// user ok, item ok
			((EntryInfo)userList.get(userID-1).get(itemID-1)).setRating(rating);
		}
		else if(userList.containsKey(userID-1) && !userList.get(userID-1).containsKey(itemID-1)) {	// user ok, item empty
			EntryInfo ItemInfo = new EntryInfo();
			ItemInfo.setRating(rating);
			//ItemInfo.setTimestamp(0);
			userList.get(userID-1).put(itemID-1, ItemInfo);
		}
		else {	// user empty
			EntryInfo ItemInfo = new EntryInfo();
			ItemInfo.setRating(rating);
			//ItemInfo.setTimestamp(0);
			HashMap<Integer, EntryInfo> ratings = new HashMap<Integer, EntryInfo>();
			ratings.put(itemID-1, ItemInfo);
			userList.put(userID-1, ratings);
		}
	}
	
	/**
	 * Delete a rating from userList
	 * @param row userID-1
	 * @param col itemID-1 
	 * @return void
	 */
	public void deleteRating(int row, int col) {
		if(userList.containsKey(row) && userList.get(row).containsKey(col))
			userList.get(row).remove(col);
	}
	
	/**
	 * Insert timestamps from each line
	 * @param String line: read line 
	 * @return int the number of inserted timestamps
	 */
	public int insertTimestamp(String line) {
		String[] result = line.split(","); 					// 0: userID
		userID = Integer.parseInt(result[0]);
		
		for(int i=1; i<result.length; i++) {
			String[] eachItem = result[i].split(":");		// 0: ItemID, 1: rating
			itemID = Integer.parseInt(eachItem[0]);
			timestamp = Integer.parseInt(eachItem[1]);
			
			if(userList.containsKey(userID-1) && userList.get(userID-1).containsKey(itemID-1)) {	// user ok, item ok
				((EntryInfo)userList.get(userID-1).get(itemID-1)).setTimestamp(timestamp); 
			} 
			/** insertTimestamp is called after insertRating
			else if(userList.containsKey(userID-1) && !userList.get(userID-1).containsKey(itemID-1)) {	// user ok, item empty
				EntryInfo ItemInfo = new EntryInfo(); //System.out.println("this shouldn't be the case "+(tt++));
				//ItemInfo.setRating(0);
				ItemInfo.setTimestamp(timestamp);
				
				userList.get(userID-1).put(itemID-1, ItemInfo);
			}
			else {	// user empty
				EntryInfo ItemInfo = new EntryInfo();
				//ItemInfo.setRating(0);
				ItemInfo.setTimestamp(timestamp);

				HashMap<Integer, EntryInfo> ratings = new HashMap<Integer, EntryInfo>();
				ratings.put(itemID-1, ItemInfo);
				userList.put(userID-1, ratings);
			}
			*/
		}
		
		return result.length - 1;
	}
	
	/**
	 * Get an entry (can be null)
	 * @param int row: userID-1
	 * @param int col: itemID-1
	 * @return EntryInfo object having double rating & int timestamp
	 */
	public Object getEntry(int row, int col) {
		if(userList.containsKey(row) && userList.get(row).containsKey(col))
			return userList.get(row).get(col);
		else
			return null;
	}
	
	/**
	 * Get a row having entries
	 * @param int row: row number (userID-1)
	 * @return a row containing sparse entries (e.g. zeros)
	 */
	public Object[] getRow(int row) {
		HashMap<Integer, EntryInfo> aUser = userList.get(row);
		
		EntryInfo[] output = new EntryInfo[maxItemID];
		for(int i=0; i<maxItemID; i++) {
			output[i] = new EntryInfo();
		}
		
		for(Entry<Integer, EntryInfo> entry : aUser.entrySet()) {
			System.out.println(entry.getKey());
			System.out.println(entry.getValue().getRating());
			System.out.println(entry.getValue().getTimestamp());

			output[entry.getKey()].setRating(entry.getValue().getRating());
			output[entry.getKey()].setTimestamp(entry.getValue().getTimestamp());
		}
		
		return output;
	}
	
	/**
	 * get a rating row containing sparse entries (e.g. zeros)
	 * @param int row: row number (userID)
	 * @return a row containing sparse entries (e.g. zeros)
	 */
	public double[] getRowRating(int row) {
		HashMap<Integer, EntryInfo> aUser = userList.get(row);
		
		double[] output = new double[maxItemID];
		
		for(Entry<Integer, EntryInfo> entry : aUser.entrySet()) {
			output[entry.getKey()] = entry.getValue().getRating();
		}
		
		return output;
	}

	
	/**
	 * get a timestamp row containing sparse entries (e.g. zeros)
	 * @param int row: row number (userID)
	 * @return a row containing sparse entries (e.g. zeros)
	 */
	public int[] getRowTimestamp(int row) {
		HashMap<Integer, EntryInfo> aUser = userList.get(row);
		
		int[] output = new int[maxItemID];
		
		for(Entry<Integer, EntryInfo> entry : aUser.entrySet()) {
			output[entry.getKey()] = entry.getValue().getTimestamp();
		}
		
		return output;
	}
	
	/**
	 * Print all rating and timestamps
	 * @param
	 * @return
	 */
	public void printAll() {
		for(Entry<Integer, HashMap> user : userList.entrySet()) {
			System.out.println("User: "+user.getKey()+"-------------------");
			for(Entry<Integer, EntryInfo> rating : ((HashMap<Integer, EntryInfo>)user.getValue()).entrySet()) {
				System.out.println(rating.getKey()+"\t"+rating.getValue().getRating()+"\t"+rating.getValue().getTimestamp());
			}
		}
	}

	/**
	 * maxItemID for handling array
	 * @param
	 * @return maxItemID (last index number of rows)
	 */
	public int getMaxItemIndex() {
		return maxItemID;
	}
	
	/**
	 * return a clone of the current SMFactory object
	 * @return SMFactory
	 */
	public SMFactory clone(){
		return new SMFactory(maxUserID,maxItemID,(HashMap<Integer,HashMap>) userList.clone());
	}
}
