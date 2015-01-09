import java.util.HashMap;
import java.util.Map.Entry;

/**
 * (Memory efficient) Sparse representation of row-oriented matrix. 
 * @author Noo-ri Kim
 *
 */

public class SMFactory implements SparseMatrix {
	
	private HashMap<Integer, HashMap> userList = new HashMap<Integer, HashMap>();
	
	private int userID, itemID;
	
	private boolean isFisrtInsert = true;
	
	private int minUserID=0;
	private int minItemID=0;
	private int maxUserID=-1;
	private int maxItemID=-1;
	
	
	/**
	 * Insert the information from each line
	 * @param String line: read line from u.data 
	 * @return void
	 */
	public void insert(String line) {
		String[] result = line.split("\t"); 					// 0: userID, 1: itemID, 2: rating, 3: timestamp
		userID = Integer.parseInt(result[0]);
		itemID = Integer.parseInt(result[1]);
		
		if ( isFisrtInsert ) {
			minUserID=userID;
			minItemID=itemID;
			maxUserID=userID;
			maxItemID=itemID;
			
			isFisrtInsert = false;
		}
		
		if ( userID > maxUserID )	{
			maxUserID = userID;
		}
		if ( itemID > maxItemID )	{
			maxItemID = itemID;
		}

		if ( userID < minUserID ) {
			minUserID = userID;
		}
		if ( itemID < minItemID ) {
			minItemID = itemID;
		}

		EntryInfo ItemInfo = new EntryInfo();
		ItemInfo.setRating(Integer.parseInt(result[2]));
		ItemInfo.setTimestamp(Integer.parseInt(result[3]));
				
		if(userList.containsKey(userID)) {
			userList.get(userID).put(itemID, ItemInfo);
		}
		else {
			HashMap<Integer, EntryInfo> ratings = new HashMap<Integer, EntryInfo>();
			ratings.put(itemID, ItemInfo);
			userList.put(userID, ratings);
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
//File oFile = new File("u.data");
//
//FileReader frd = null;
//BufferedReader brd = null;
//
//String rLine = null;
//
//SMFactory smFactory = new SMFactory();
//   
//try {
//     frd = new FileReader(oFile);
//     brd = new BufferedReader(frd);  
//                                                         
//     while ((rLine = brd.readLine())!= null) {
// 		System.out.println("\n------------------\n 파일로부터 라인별로 저장 \n------------------");
//    	 smFactory.insert(rLine);
//     }		     
//     frd.close();
//     brd.close();
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

