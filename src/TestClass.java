import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;


public class TestClass {
	public static void main(String[] args) throws IOException, ParseException {

		
		//      ConvertMovie Example
		ConvertFactory cf = new ConvertFactory();
		String dir = System.getProperty("user.dir");
		dir = dir+"\\NetFlix\\movie_titles.txt";		  
		cf.convertingMovie(dir);


		//		ConvertRating Example

		//ConvertFactory cf = new ConvertFactory();
		//String dir = System.getProperty("user.dir");
		//dir = dir+"\\NetFlix\\training_set";
		//cf.walk(dir);
		//System.out.println(cf.RatingList.toString());



	}
}
