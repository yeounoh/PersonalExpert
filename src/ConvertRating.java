import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;


public class ConvertRating {
	void converting(String filename) throws IOException, ParseException{
		BufferedWriter file = new BufferedWriter(new FileWriter("u.data", true));//File name is always "u.data" file open as attach mode
		FileInputStream fis= new FileInputStream(filename);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		String line = null;
		String []tokens;
		String MovieNum = "";
		int count = 0;
		
		while((line = br.readLine()) != null){
			if(count == 0) {
				MovieNum = line.split(":")[0];
				count++;
				System.out.println(MovieNum);
				continue; 
			}
			else{
				tokens = line.split(",");
				file.write(tokens[0] + "\t" + MovieNum + "\t" + tokens[1] + "\t" + ChangeUnixTime(tokens[2]));
				file.newLine();
			}
		}
		file.close();
	}
	long ChangeUnixTime(String tok) throws ParseException
	{
		String []token;
		token = tok.split("-");
		long epoch = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").parse(token[1]+"/" +token[2] +"/" +token[0]+" 00:00:00").getTime() / 1000;
		return epoch;
	}

}
