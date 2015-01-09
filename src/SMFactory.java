import java.util.HashMap;
import java.util.Map.Entry;

/**
 * (Memory efficient) Sparse representation of row-oriented matrix. 
 * @author Noo-ri Kim
 *
 */

public class SMFactory implements SparseMatrix {
	
	private HashMap<Integer, HashMap> userList = new HashMap<Integer, HashMap>();
	
	private int userID, itemID, rating, timestamp;
	
	private int maxItemID=-1;
	
	
	/**
	 * Insert ratings from each line
	 * @param String line: read line 
	 * @return void
	 */
	public void insertRating(String line) {
		String[] result = line.split(","); 					// 0: userID
		userID = Integer.parseInt(result[0]);

		for(int i=1; i<result.length; i++) {
			String[] eachItem = result[i].split(":");		// 0: ItemID, 1: rating
			itemID = Integer.parseInt(eachItem[0]);
			rating = Integer.parseInt(eachItem[1]);
			
			if ( itemID > maxItemID )	{
				maxItemID = itemID;
			}
			
			if(userList.containsKey(userID) && userList.get(userID).containsKey(itemID)) {	// user ok, item ok
				((EntryInfo)userList.get(userID).get(itemID)).setRating(rating);
			}
			else if(userList.containsKey(userID) && !userList.get(userID).containsKey(itemID)) {	// user ok, item empty
				EntryInfo ItemInfo = new EntryInfo();
				ItemInfo.setRating(rating);
				ItemInfo.setTimestamp(0);
				
				userList.get(userID).put(itemID, ItemInfo);
			}
			else {	// user empty
				EntryInfo ItemInfo = new EntryInfo();
				ItemInfo.setRating(rating);
				ItemInfo.setTimestamp(0);

				HashMap<Integer, EntryInfo> ratings = new HashMap<Integer, EntryInfo>();
				ratings.put(itemID, ItemInfo);
				userList.put(userID, ratings);
			}
		}
	}
	
	/**
	 * Insert timestamps from each line
	 * @param String line: read line 
	 * @return void
	 */
	public void insertTimestamp(String line) {
		String[] result = line.split(","); 					// 0: userID
		userID = Integer.parseInt(result[0]);

		for(int i=1; i<result.length; i++) {
			String[] eachItem = result[i].split(":");		// 0: ItemID, 1: rating
			itemID = Integer.parseInt(eachItem[0]);
			timestamp = Integer.parseInt(eachItem[1]);
			
			if ( itemID > maxItemID )	{
				maxItemID = itemID;
			}
			
			if(userList.containsKey(userID) && userList.get(userID).containsKey(itemID)) {	// user ok, item ok
				((EntryInfo)userList.get(userID).get(itemID)).setTimestamp(timestamp);
			}
			else if(userList.containsKey(userID) && !userList.get(userID).containsKey(itemID)) {	// user ok, item empty
				EntryInfo ItemInfo = new EntryInfo();
				ItemInfo.setRating(0);
				ItemInfo.setTimestamp(timestamp);
				
				userList.get(userID).put(itemID, ItemInfo);
			}
			else {	// user empty
				EntryInfo ItemInfo = new EntryInfo();
				ItemInfo.setRating(timestamp);
				ItemInfo.setTimestamp(0);

				HashMap<Integer, EntryInfo> ratings = new HashMap<Integer, EntryInfo>();
				ratings.put(itemID, ItemInfo);
				userList.put(userID, ratings);
			}
		}
	}
	
	/**
	 * Get a entry
	 * @param int row: userID
	 * @param int col: itemID
	 * @return EntryInfo object having byte rating & int timestamp
	 */
	public Object getEntry(int row, int col) {
		return userList.get(row).get(col);
	}
	
	/**
	 * Get a row having entries
	 * @param int row: row number (userID)
	 * @return a row containing sparse entries (e.g. zeros)
	 */
	public Object[] getRow(int row) {
		HashMap<Integer, EntryInfo> aUser = userList.get(row);
		
		EntryInfo[] output = new EntryInfo[maxItemID+1];
		for(int i=0; i<maxItemID+1; i++) {
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
	public int[] getRowRating(int row) {
		HashMap<Integer, EntryInfo> aUser = userList.get(row);
		
		int[] output = new int[maxItemID+1];
		
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
		
		int[] output = new int[maxItemID+1];
		
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
}

// 참고자료
//File oFile = new File("rating.data");
//File oFile2 = new File("timestamp.data");
//
//FileReader frd = null;
//BufferedReader brd = null;
//FileReader frd2 = null;
//BufferedReader brd2 = null;
//
//
//String rLine = null;
//String rLine2 = null;
//
//SMFactory smFactory = new SMFactory();
//   
//try {
//     frd = new FileReader(oFile);
//     brd = new BufferedReader(frd);  
//                                                         
//     while ((rLine = brd.readLine())!= null) {
//    	 System.out.println(rLine);
//    	 smFactory.insertRating(rLine);
//     }		     
//     frd.close();
//     brd.close();
//} catch (IOException e) {
//     e.printStackTrace();
//}
//
//try {
//     frd2 = new FileReader(oFile2);
//     brd2 = new BufferedReader(frd2);  
//                                                         
//     while ((rLine2 = brd2.readLine())!= null) {
//    	 System.out.println(rLine2);
//    	 smFactory.insertTimestamp(rLine2);
//     }		     
//     frd2.close();
//     brd2.close();
//} catch (IOException e) {
//     e.printStackTrace();
//}
//
// System.out.println("\n------------------\n Print All \n------------------");
// smFactory.printAll();
// 
// System.out.println("\n------------------\n 특정 아이템 정보 (getEntry) \n------------------");
// System.out.println("user 1 and item 242");
// System.out.println("raintg: "+((EntryInfo)(smFactory.getEntry(22, 302))).getRating());
// System.out.println("time stamp: "+((EntryInfo)(smFactory.getEntry(22, 302))).getTimestamp());
//
// System.out.println("\n------------------\n user 22의 itme에 대한 rating 정보 (getRowRaiting) \n------------------");
// int lastIndex = smFactory.getMaxItemIndex();
// int[] ratingArray = smFactory.getRowRating(22);
// for(int i=0; i<lastIndex; i++) {
//	 System.out.print(i+":"+ratingArray[i]+" | ");
// }
// 
// System.out.println("\n------------------\n user 1의 item에 대한 timestamp 정보 (getRowTimestamp) \n------------------");
//// int lastIndex = smFactory.getMaxItemIndex();
// int[] timestampArray = smFactory.getRowTimestamp(22);
// for(int i=0; i<lastIndex; i++) {
//	 System.out.print(i+":"+timestampArray[i]+" | ");
// }
//
// System.out.println("\n------------------\n user 1의 item에 대한  정보 (getRow) \n------------------");
//// int lastIndex = smFactory.getMaxItemIndex();
// EntryInfo[] row = (EntryInfo[]) smFactory.getRow(22);
// for(int i=0; i<lastIndex; i++) {
//	 System.out.print(i+":"+row[i].getRating()+", "+row[i].getTimestamp()+" | ");
// }
