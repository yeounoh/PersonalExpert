import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;

/**
 * @author yeounoh chung
 */
public class ParserMovieLens {

	private int nuser;
	private int nitem;
	private int ngenre;
	
	public ParserMovieLens(int nuser, int nitem, int ngenre){
		this.nuser = nuser;
		this.nitem = nitem;
		this.ngenre = ngenre;
	}
	
	private class User{
		int suid= 0;
		int i_cnt= 0;
		String ratings= ""; //mid:rating
		String timestamps= ""; //mid:timestamp
		
		public User(int suid, String ratings, String timestamps){
			this.suid= suid;
			this.ratings= ratings;
			this.timestamps= timestamps;
			this.i_cnt= 1;
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
			this.i_cnt++;
		}
		
		public void addTimestamp(String t){
			this.timestamps= this.timestamps + "," + t;
		}
		
		public int getUID(){
			return this.suid;
		}
		
		public int getItemCount(){
			return this.i_cnt;
		}
		
		public String getRatings(){
			return this.ratings;
		}
		
		public String getTimestamps(){
			return this.timestamps;
		}
	}
	
	/**
	 * MovieLens_100k data set (943 users)
	 * 
	 * tab separated (|) in a form of:
	 * user id | item id | rating | timestamp[
	 * e.g)----------------
	 * 196	242	3	881250949
	 * 296	24	2	831250969
	 * --------------------
	 * 
	 * @param src_dir MovieLens working directory containing u.data file
	 * @param dst_dir working directory for storing the processed output file(s)
	 */
	public void preprocess(String src_dir, String dst_dir){
		try{ 
			FileInputStream fis= null;
			BufferedReader br= null;
			String[] tokens= null;
			FileOutputStream fos= null, fos2= null, fos3= null;
			BufferedWriter bw= null, bw2= null, bw3= null;
			String line, wline;
			
			fis= new FileInputStream(src_dir + "/u.data");
			br= new BufferedReader(new InputStreamReader(fis));
			
			//to alleviate the issue of frequent file open/close operations
			HashMap<String, User> uid_map= new HashMap<String, User>(nuser);
			
			while((line = br.readLine())!=null){
				tokens = line.split("\t|::");
				String tuid = tokens[0]; 
				String trating = "" + tokens[1] + ":" + tokens[2];
				String ttimestamp = "" + tokens[1] + ":" + tokens[3];
				
				if(uid_map.containsKey(tuid)){
					uid_map.get(tuid).addRating(trating);
					uid_map.get(tuid).addTimestamp(ttimestamp);
				}
				else{
					User tuser = new User(Integer.parseInt(tuid),trating,ttimestamp);
					uid_map.put(tuid, tuser);
				}
			}
			br.close();
			
			//make user_ data; user_ : user id | movie id | rating format
			fos= new FileOutputStream(dst_dir + "/user_"+nuser+".txt"); //user_943.txt 
			bw= new BufferedWriter(new OutputStreamWriter(fos));
			
			fos2= new FileOutputStream(dst_dir + "/user_"+nuser+"_t.txt");
			bw2= new BufferedWriter(new OutputStreamWriter(fos2));
			
			fos3= new FileOutputStream(dst_dir + "/user_"+nuser+"_c.txt");
			bw3= new BufferedWriter(new OutputStreamWriter(fos3));
			
			Iterator<Entry<String, User>> itr= uid_map.entrySet().iterator();
			while(itr.hasNext()){
				Entry<String, User> temp= (Entry<String, User>) itr.next();
				User tuser= (User) temp.getValue();
				wline= ""+tuser.getUID()+","+tuser.getRatings();
				bw.write(wline);
				bw.newLine();
				bw.flush();
				
				wline= ""+tuser.getUID()+","+tuser.getTimestamps();
				bw2.write(wline);
				bw2.newLine();
				bw2.flush();
				
				wline= ""+tuser.getUID()+","+tuser.getItemCount();
				bw3.write(wline);
				bw3.newLine();
				bw3.flush();
			}
			bw.close();
			bw2.close();
			bw3.close();
		}catch (Exception e){
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	/**
	 * make 5 folds : 'user_test_1.txt' 'user_test_2.txt' ...
	 * @param pdir
	 */
	public void makeDataSet(String pdir, int nfold){
		try{
			FileInputStream fis= null;
			BufferedReader br= null;
			FileOutputStream fos= null, fos2= null;
			BufferedWriter bw= null, bw2= null;
			String line;
			String[] tokens, tokens2;
			
			for(int j=0;j<nfold;j++){
			
				fis= new FileInputStream(pdir+"/user_"+nuser+".txt");
				br= new BufferedReader(new InputStreamReader(fis));
				
				fos= new FileOutputStream(pdir+"/user_test_f"+(j+1)+".txt");
				bw= new BufferedWriter(new OutputStreamWriter(fos));
				
				fos2= new FileOutputStream(pdir+"/user_train_f"+(j+1)+".txt");
				bw2= new BufferedWriter(new OutputStreamWriter(fos2));
				
				while((line=br.readLine())!=null){
					tokens= line.split("[,]");
					int suid= Integer.parseInt(tokens[0]);
					String ratings_test= "";
					String ratings_train= "";
					for(int i=1;i<tokens.length;i++){
						tokens2= tokens[i].split("[:]");
						int smid= Integer.parseInt(tokens2[0]);
						double rating= (double) Integer.parseInt(tokens2[1]);
						
						if(i%nfold == j){
							ratings_test= ratings_test + "," + smid + ":" + (int) rating;
						}
						else{
							ratings_train= ratings_train + "," + smid + ":" + (int) rating;
						}
					}
					bw.write("" + suid + ratings_test);
					bw.newLine();
					bw.flush();
					
					bw2.write("" + suid + ratings_train);
					bw2.newLine();
					bw2.flush();
				}
				bw.close();
				bw2.close();
				br.close();
			}
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public void makeValidSet(String p_train, int ratio){
		try{
			FileInputStream fis= null;
			BufferedReader br= null;
			FileOutputStream fos= null, fos2= null;
			BufferedWriter bw= null, bw2= null;
			String line;
			String[] tokens, tokens2;
			
			fis= new FileInputStream(p_train);
			br= new BufferedReader(new InputStreamReader(fis));
			
			String prefix= p_train.substring(0, p_train.indexOf('.'));
			fos= new FileOutputStream(prefix+"_tr.txt");
			bw= new BufferedWriter(new OutputStreamWriter(fos));
			
			fos2= new FileOutputStream(prefix+"_vl.txt");
			bw2= new BufferedWriter(new OutputStreamWriter(fos2));
			
			while((line=br.readLine())!=null){
				tokens= line.split("[,]");
				int suid= Integer.parseInt(tokens[0]);
				String ratings_valid= "";
				String ratings_train= "";
				for(int i=1;i<tokens.length;i++){
					tokens2= tokens[i].split("[:]");
					int smid= Integer.parseInt(tokens2[0]);
					double rating= (double) Integer.parseInt(tokens2[1]);
					
					if(i%ratio == (ratio-1)){
						ratings_valid= ratings_valid + "," + smid + ":" + (int) rating;
					}
					else{
						ratings_train= ratings_train + "," + smid + ":" + (int) rating;
					}
				}
				bw.write("" + suid + ratings_train);
				bw.newLine();
				bw.flush();
				
				bw2.write("" + suid + ratings_valid);
				bw2.newLine();
				bw2.flush();
			}
			bw.close();
			bw2.close();
			br.close();
		}catch (Exception e){
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	/**
	 * read u.item 
	 * 1) to store movie item release date. convert release date to Unix timestamp, 
	 * which counts the elapsed time in milliseconds from 1970 Jan 01
	 * the converted time could be negative, if the movie was introduced before 1970.
	 * 
	 * 2)
	 * @param src_dir
	 * @param dst_dir
	 */
	public int[][] loadItemGenre(String src_dir, String dst_dir){
		int[][] i_genre= new int[nitem][ngenre];
		
		try{
			String line; 
			String[] tokens;
			
			FileInputStream fis = new FileInputStream(src_dir + "/u.item");
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			
			FileOutputStream fos = new FileOutputStream(dst_dir + "/genre.txt");
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			
			while((line = br.readLine()) != null){
				tokens = line.split("[|]"); 
				if(tokens.length != 24)
					continue;
				
				if(tokens[2].length() == 0 ) 
					continue;
				
				String tgenre= "";
				for(int i=0; i<ngenre; i++){
					tgenre= tokens[tokens.length-1-i] + "|" + tgenre;
					i_genre[Integer.parseInt(tokens[0])-1][ngenre-1-i]= Integer.parseInt(tokens[tokens.length-1-i]);
				}
				bw.write("" + tokens[0] + ":" + tgenre);
				bw.newLine();
				bw.flush();
			}
			bw.close();
			br.close();
		}
		catch (Exception e){
			e.printStackTrace();
			System.exit(0);
		}
		
		return i_genre;
	}
	
	
	/**
	 * read u.item 
	 * 1) to store movie item release date. convert release date to Unix timestamp, 
	 * which counts the elapsed time in milliseconds from 1970 Jan 01
	 * the converted time could be negative, if the movie was introduced before 1970.
	 * 
	 * 2)
	 * @param src_dir
	 * @param dst_dir
	 */
	public double[] loadItemRelease(String src_dir, String dst_dir){
		double[] i_release= new double[nitem];
		
		try{
			String line; 
			String[] tokens;
			
			FileInputStream fis = new FileInputStream(src_dir + "/u.item");
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			
			FileOutputStream fos = new FileOutputStream(dst_dir + "/release.txt");
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			
			while((line = br.readLine()) != null){
				tokens = line.split("[|]"); 
				if(tokens.length != 24)
					continue;
				
				if(tokens[2].length() == 0 ) 
					continue;
				
				Date tdate = new SimpleDateFormat("dd-MMMM-yyyy", Locale.ENGLISH).parse(tokens[2]);
				long time = tdate.getTime()/1000;
				bw.write("" + tokens[0] + ":" + time);
				bw.newLine();
				bw.flush();
				
				i_release[Integer.parseInt(tokens[0])-1]= time;
			}
			bw.close();
			br.close();
		}
		catch (Exception e){
			e.printStackTrace();
			System.exit(0);
		}
		
		return i_release;
	}
}
