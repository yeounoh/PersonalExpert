import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Read data stored in files
 * @author yeounoh
 *
 */
public class FileStorage {

	private String file;
	private FileInputStream fis;
	private BufferedReader br;
	
	public FileStorage(String file){
		this.file = file;
	}
	
	public void open() throws IOException{
		fis = new FileInputStream(file); //"knn_uu_sim.txt"
		br = new BufferedReader(new InputStreamReader(fis));
	}
	
	public void close() throws IOException{
		br.close();
	}
	
	/**
	 * sequential access by row in the file
	 * open and close BufferedReader separately.
	 * @return a single row in double[], null if reached EOF (or !br.ready()).
	 */
	public double[] seqAccess(){
		double[] out= null;
		try{
			if(!br.ready())
				return null;
			
			String row = br.readLine();
			String[] rtoken = row.split(" ");
			out = new double[rtoken.length];
			for(int i=0;i<rtoken.length;i++){
				out[i] = Double.parseDouble(rtoken[i]);
			}
		}
		catch(IOException ie){
			ie.printStackTrace();
			System.exit(1);
		}
		return out;
	}
	
	/**
	 * random access to a single row in the file
	 * @param r zero-indexed row index
	 * @return a single row in double[]
	 */
	public double[] randAccess(int r){
		
		double[] out= null;
		try{
			open();
			for(int i=0;i<r-1;i++)
				br.readLine();
			String row = br.readLine();
			String[] rtoken = row.split(" ");
			out = new double[rtoken.length];
			for(int i=0;i<rtoken.length;i++){
				out[i] = Double.parseDouble(rtoken[i]);
			}
			close();
		}
		catch(IOException ie){
			ie.printStackTrace();
			System.exit(1);
		}
		return out;
	}
}
