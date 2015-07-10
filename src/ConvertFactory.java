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
				String[] tokens = line.split(",");
				file.write(tokens[0] + "\t" + MovieNum + "\t" + tokens[1] +
				"\t" + ChangeUnixTime(tokens[2]));
				file.flush();
				file.newLine();
			}
		}
		br.close();
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

	void convertingMovie(String filename, String dst) throws IOException {
		BufferedWriter file = new BufferedWriter(new FileWriter(dst, true)); //"u.item"
		FileInputStream fis = new FileInputStream(filename);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		String line = null;
		String[] tokens;

		while ((line = br.readLine()) != null) {
			tokens = line.split(",");
			file.write(tokens[0] + "|" + tokens[2] + "|" + "01-Jan-"
					+ tokens[1]
					+ "||http://unknown|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0");
			file.flush();
			file.newLine();
		}
		file.close();
	}

	public void walk(String path, String dst) throws IOException, ParseException {
		ConvertFactory cf = new ConvertFactory();
		
		File root = new File(path);
		File[] list = root.listFiles();

		BufferedWriter file = new BufferedWriter(new FileWriter(dst, true)); //"u.data"
		
		if (list == null)
			return;

		int cnt = 0;
		for (File f : list) {
			if (f.isDirectory()) {
				walk(f.getAbsolutePath(), dst);
			} else {
				String file_path = f.getAbsoluteFile().toString();
				String[] tks = file_path.split("/");
				if(!tks[tks.length-1].equals(".DS_Store")){
					cf.convertingRating(f.getAbsoluteFile().toString(), file);
				}
				System.out.println(++cnt);
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