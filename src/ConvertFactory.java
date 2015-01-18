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
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class ConvertFactory {
	static public Map<Integer, Map<Integer, time_info>> RatingList;
	//public 
	public ConvertFactory() {
		super();
		RatingList = new HashMap<Integer, Map<Integer, time_info>>();
	}

	void convertingRating(String filename) throws IOException, ParseException {
		// BufferedWriter file = new BufferedWriter(new FileWriter("u.data",
		// true));//File name is always "u.data" file open as attach mode
		FileInputStream fis = new FileInputStream(filename);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		String line = null;
		String[] tokens;
		String MovieNum = "";

		int count = 0;
		Map<Integer, time_info> in_data1 = new HashMap<Integer, time_info>();
		Map<Integer, time_info> in_data2 = new HashMap<Integer, time_info>();
		time_info info = new time_info();

		while ((line = br.readLine()) != null) {

			if (count == 0) {
				// First line of file
				MovieNum = line.split(":")[0];
				count++;
				System.out.println(MovieNum);
				continue;
			} else {
				tokens = line.split(",");

				if ((in_data2 = RatingList.get(Integer.parseInt(tokens[0]))) != null) {
					info.ratings = Integer.parseInt(tokens[1]);
					info.timeStamp = ChangeUnixTime(tokens[2]);
					//System.out.println(""+info.ratings+"||||"+info.timeStamp);
					in_data2.put(Integer.parseInt(MovieNum), info);
					//System.out.println(in_data2);
					RatingList.put(Integer.parseInt(tokens[0]), in_data2);
					in_data2 = new HashMap<Integer, time_info>();
				} else {
					info.ratings = Integer.parseInt(tokens[1]);
					info.timeStamp = ChangeUnixTime(tokens[2]);
					in_data1 = new HashMap<Integer, time_info>();
					in_data1.put(Integer.parseInt(MovieNum), info);
					RatingList.put(Integer.parseInt(tokens[0]), in_data1);
				}

				// file.write(tokens[0] + "\t" + MovieNum + "\t" + tokens[1] +
				// "\t" + ChangeUnixTime(tokens[2]));
				// file.newLine();
			}
		}
		// System.out.println(RatingList.toString());
		// file.close();
	}

	class time_info {
		Integer ratings;
		Long timeStamp;
		
		public time_info() {
			super();
			ratings = 0;
			this.timeStamp = 0l;
		}

	}

	void convertingMovie(String filename) throws IOException {
		BufferedWriter file = new BufferedWriter(new FileWriter("u.item", true));
		FileInputStream fis = new FileInputStream(filename);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		String line = null;
		String[] tokens;
		String MovieNum = "";
		int count = 0;

		while ((line = br.readLine()) != null) {
			tokens = line.split(",");
			file.write(tokens[0] + "|" + tokens[2] + "|" + "01-Jan-"
					+ tokens[1]
					+ "||http://unknown|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0");
			file.newLine();
		}
		file.close();
	}

	public void walk(String path) throws IOException, ParseException {
		ConvertFactory cf = new ConvertFactory();
		File root = new File(path);
		File[] list = root.listFiles();

		if (list == null)
			return;

		for (File f : list) {
			if (f.isDirectory()) {
				walk(f.getAbsolutePath());
			} else {
				cf.convertingRating(f.getAbsoluteFile().toString());
			}
		}
		BufferedWriter file = new BufferedWriter(new FileWriter("u.data", true));
		TreeMap tm = new TreeMap(RatingList);
		Iterator iteratorKey = tm.keySet().iterator();
		while (iteratorKey.hasNext()) {
			int key = (int) iteratorKey.next();
			TreeMap inner_tm = new TreeMap((Map<Integer, time_info>) tm.get(key));
			Iterator inner_iteratorKey = inner_tm.keySet().iterator();
			while (inner_iteratorKey.hasNext()) {
				int inner_key = (int) inner_iteratorKey.next();
				String str = key + "\t" + inner_key + "\t" + 
				((time_info)(inner_tm.get(inner_key))).ratings + "\t" + ((time_info)(inner_tm.get(inner_key))).timeStamp;
				file.write(str);
				file.newLine();
			}
		}
		file.close();
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
