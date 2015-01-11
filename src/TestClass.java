import java.io.IOException;
import java.text.ParseException;


public class TestClass {
	public static void main(String[] args) throws IOException, ParseException {
		CalculateSimilarity cs = new CalculateSimilarity();
		Similarity sm = new Similarity();
		double [] user1 = {1,2,1,1,0,0,1,1,1,1};
		double [] user2 = {2,2,2,2,3,2,2,2,2,2};
		//System.out.println(cs.getAverage(user1));
		//System.out.println(cs.getAverage(user2));
		System.out.println(sm.pearsonCorr(user1, cs.getAverage(user1), user2, cs.getAverage(user2)));
		

		/************************
		 * ConvertMovie Example
		 * 
		 * String dir = System.getProperty("user.dir");
		 * dir = dir+"\\NetFlix\\movie_titles.txt";
		 * ConvertMovie cm = new ConvertMovie();
		 * cm.converting(dir);
		 * 
		 */
		
		/***********************
		 * ConvertRating Example
		 * 
		 * Filewalker fw = new Filewalker();
		 * String dir = System.getProperty("user.dir");
		 * dir = dir+"\\NetFlix\\training_set";
		 * fw.walk(dir);
		 */
		
    }
}
