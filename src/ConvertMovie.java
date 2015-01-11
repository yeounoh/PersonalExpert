import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;


public class ConvertMovie {
	void converting(String filename) throws IOException{
		BufferedWriter file = new BufferedWriter(new FileWriter("u.item", true));
		FileInputStream fis= new FileInputStream(filename);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		String line = null;
		String []tokens;
		String MovieNum = "";
		int count = 0;
		
		while((line = br.readLine()) != null){
			tokens = line.split(",");
			file.write(tokens[0] + "|" + tokens[2] + "|" + "01-Jan-" +tokens[1] + "||http://unknown|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0");
			file.newLine();
		}
		file.close();
	}
}
