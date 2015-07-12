import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ConvertFactory {
	
	class time_info {
		Integer ratings;
		Long timeStamp;
		
		public time_info() {
			super();
			ratings = 0;
			this.timeStamp = 0l;
		}

	}
	
	/**
	 * CustomerID,Rating,Date
	 * MovieIDs range from 1 to 17770 sequentially.
	 * CustomerIDs range from 1 to 2649429, with gaps. There are 480189 users.
	 * Ratings are on a five star (integral) scale from 1 to 5.
	 * Dates have the format YYYY-MM-DD.
	 */
	private static HashMap<Integer, Integer> user_id_map = new HashMap<Integer,Integer>();
	private static int user_id_cnt = 0;
	//private static HashMap<Integer, Integer> item_id_map = new HashMap<Integer, Integer>();
	//private static int item_id_cnt = 0;
	private static int rating_cnt = 0;
	
	void convertingRating(String filename, BufferedWriter file) throws IOException, ParseException {
		FileInputStream fis = new FileInputStream(filename);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		
		String line = null;
		String MovieNum = "";
		int count = 0;

		while ((line = br.readLine()) != null) {
			if (count == 0) { 	// First line of file
				MovieNum = line.split(":")[0];
				count++;
			} 
			else {
				if(count++%10 != 0)
					continue; //rating count: 10039964, user_id_cnt: 458376
				else{
					String[] tokens = line.split(",");
					int user_id = Integer.parseInt(tokens[0]);
					if(!user_id_map.containsKey(user_id)){
						user_id_map.put(user_id, ++user_id_cnt);
					}
					
					file.write(user_id_map.get(user_id).intValue() + "\t" + MovieNum + "\t" + tokens[1] +
							"\t" + ChangeUnixTime(tokens[2]));
					file.flush();
					file.newLine();
					rating_cnt++;
				}
				
			}
		}
		br.close();
	}

	

	void convertingMovie(String filename, String dst) throws IOException {
		BufferedWriter file = new BufferedWriter(new FileWriter(dst, true)); //"u.item"
		FileInputStream fis = new FileInputStream(filename);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		String line = null;
		String[] tokens;

		while ((line = br.readLine()) != null) {
			tokens = line.split(",");
			String date = tokens[1];
			if(date.equals("NULL")){
				date = "1985";
			}
			file.write(tokens[0] + "|" + tokens[2] + "|" + "01-Jan-"
					+ date
					+ "||http://unknown|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0");
			file.flush();
			file.newLine();
		}
		file.close();
	}

	public int walk(String path, String dst) throws IOException, ParseException {
		File root = new File(path);
		File[] list = root.listFiles();

		BufferedWriter file = new BufferedWriter(new FileWriter(dst, true)); //"u.data"
		
		if (list == null)
			return -1;

		int cnt = 0;
		for (File f : list) {
			if (f.isDirectory()) {
				walk(f.getAbsolutePath(), dst);
			} else {
				String file_path = f.getAbsoluteFile().toString();
				String[] tks = file_path.split("/");
				if(!tks[tks.length-1].equals(".DS_Store")){
					convertingRating(f.getAbsoluteFile().toString(), file);
				}
			}
		}
		file.close();
		
		System.out.println("rating count: " + rating_cnt);
		System.out.println("user_id_cnt: " + user_id_cnt);
		return user_id_cnt;
	}

	long ChangeUnixTime(String tok) throws ParseException {
		String[] token;
		token = tok.split("-");
		long epoch = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
				.parse(token[1] + "/" + token[2] + "/" + token[0] + " 00:00:00")
				.getTime() / 1000;
		return epoch;
	}

}