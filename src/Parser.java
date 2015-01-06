import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * @author yeounoh chung
 */
public class Parser {

	private class User{
		int suid= 0;
		String ratings= ""; //mid:rating
		
		public User(int suid, String ratings){
			this.suid= suid;
			this.ratings= ratings;
		}
		
		public void setUID(int k){
			this.suid= k;
		}
		
		/**
		 * 
		 * @param r of form "mid:rating"
		 */
		public void addRating(String r){
			this.ratings= this.ratings + "," + r;
		}
		
		public int getUID(){
			return this.suid;
		}
		
		public String getRatings(){
			return this.ratings;
		}
	}
	
	/**
	 * take 1000 movies from the Netflix dataset;
	 * 
	 * Netflix dataset data of a form:
	 * Movie ID:\n
	 * Customer ID,Rating,Date\n
	 * ...
	 * e.g)----------------
	 * 1:
	 * 1488844,3,2005-09-06
	 * 822109,5,2005-05-13
	 * 885013,4,2005-10-19
	 * --------------------
	 */
	public void preprocess(String pdir, String wpdir){
		try{
			FileInputStream fis= null;
			BufferedReader br= null;
			String[] tokens= null;
			FileOutputStream fos= null;
			BufferedWriter bw= null;
			String line, wline;
			
			fos= new FileOutputStream(wpdir+"\\movie_1000.txt");
			bw= new BufferedWriter(new OutputStreamWriter(fos));
			
			int k=0;
			File folder= new File(pdir);
			for(File fileEntry : folder.listFiles()){
				if(fileEntry.isDirectory()){
					continue; //no recursion
				} else{
					k++;
					System.out.println(k);
					//for each file entry
					fis= new FileInputStream(pdir+"/"+fileEntry.getName());
					br= new BufferedReader(new InputStreamReader(fis));
					
					//compact an item file to a line
					wline= "";
					while((line= br.readLine())!=null){
						tokens= line.split("[,]");
						if(tokens.length != 3) {
							wline= line.split("[:]")[0];
						}else{
							wline= wline+","+tokens[0]+":"+tokens[1];
						}
					}
					//write the item line
					bw.write(wline);
					bw.newLine();	
					//close the output stream
					br.close();
				}
			}
			bw.close();
		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	public void rescaleMovieID(String pdir){
		try{
			FileInputStream fis= null;
			BufferedReader br= null;
			String[] tokens= null;
			FileOutputStream fos= null;
			BufferedWriter bw= null;
			String line, wline;
			
			fis= new FileInputStream(pdir+"/movie_1000.txt");
			br= new BufferedReader(new InputStreamReader(fis));
			
			fos= new FileOutputStream(pdir+"\\movie_1000_rescale.txt",true);
			bw= new BufferedWriter(new OutputStreamWriter(fos));
		
			//rescale movie id to increasing integer [1..1000]
			int k= 0;
			while((line= br.readLine())!=null){
				k++;
				System.out.println(k); //chekcing
				wline= ""+k; 
				if(k>380){
					tokens= line.split("[,]");
					//replace the original mid with k
					for(int j=1;j<tokens.length;j++){
						wline= wline+","+tokens[j];
					}
					bw.write(wline);
					bw.newLine();
				}
			}
			bw.close();
			br.close();
		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	/**
	 * 
	 * extract user access log files from preprocessed item list:
	 * e.g.) for a unique user(CID) file
	 * mid:CID:rating:date
	 * mid:CID:rating:date
	 * mid:CID:rating:date
	 * ...
	 * 
	 * @param pdir
	 * @param wpdir
	 */
	public void makeUserData(String pdir){
		try{
			FileInputStream fis= null;
			BufferedReader br= null;
			String[] tokens, tokens2= null;
			FileOutputStream fos= null;
			BufferedWriter bw= null;
			String line, wline;
			
			fis= new FileInputStream(pdir+"\\movie_1000_rescale.txt");
			br= new BufferedReader(new InputStreamReader(fis));
			
			fos= new FileOutputStream(pdir+"\\user_10000.txt"); 
			bw= new BufferedWriter(new OutputStreamWriter(fos));
			
			//to alleviate the issue of frequent file open/close operations
			HashMap<String, User> uid_map= new HashMap<String, User>(48019);
			int k=0;	
			int loop_count= 0;	
			while((line= br.readLine())!=null){
				tokens= line.split("[,]");
				String smid= tokens[0];
				for(int i=1;i<tokens.length;i++){
					tokens2= tokens[i].split("[:]");
					String scid= tokens2[0];
					
					if(uid_map.containsKey(scid)){
						User temp= uid_map.get(scid);
						temp.addRating(smid+":"+tokens2[1]);
					}else{
						k++; 
						if(k>10000) continue; // use only take 20000 users
						System.out.println("number of user count: "+k);//checking
						uid_map.put(scid, new User(k,smid+":"+tokens2[1]));
					}
				} 
				System.out.println("while loop count: "+(loop_count++));//checking
			}

			Iterator<Entry<String, User>> itr= uid_map.entrySet().iterator();
			while(itr.hasNext()){
				Entry<String, User> temp= (Entry<String, User>) itr.next();
				User tuser= (User) temp.getValue();
				wline= ""+tuser.getUID()+","+tuser.getRatings();
				System.out.println(wline); //checking
				bw.write(wline);
				bw.newLine();
			}
			bw.close();
			br.close();
		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	/**
	 * 
	 * @param pdir
	 */
	public void makeTestSet(String pdir){
		try{
			FileInputStream fis= null;
			BufferedReader br= null;
			FileOutputStream fos= null;
			BufferedWriter bw= null;
			String line;
			
			fis= new FileInputStream(pdir+"/user_10000.txt");
			br= new BufferedReader(new InputStreamReader(fis));
				
			int j=0, k=0;
			fos= new FileOutputStream(pdir+"/user_test_"+(j+1)+".txt");
			bw= new BufferedWriter(new OutputStreamWriter(fos));
						
			//assuming there are 10000 users
			while((line=br.readLine())!=null){
				k++;
				if(k>1000){
					k=0; j++;
					bw.close();
					fos= new FileOutputStream(pdir+"/user_test_"+(j+1)+".txt");
					bw= new BufferedWriter(new OutputStreamWriter(fos));
				}
				bw.write(line);
				bw.newLine();
			}
			bw.close();
			br.close();
		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}
}
