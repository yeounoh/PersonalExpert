import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

/**
 * 
 * @author yeounoh chung
 *
 */
public class KNearestNeighbor {

	//private double[][] ui_train;
	private SMFactory ui_train;
	//private double[][] ui_train_sparse;
	private SMFactory ui_train_sparse;
	//private double[][] ui_test;
	private SMFactory ui_test;
	private int[] i_test;
	private double[] u_avg_rating;
	private double[] i_avg_rating;
	private double[] u_std;
	//private double[][] uu_sim; //store in file
	//private long[][] ui_time;
	private long[] i_release;
	private long ref_time; //22-Apr-1998
	private long cut_time;
	private int[] u_icount;
	
	private int nuser;
	private int nitem;
	private int sparse_month = -1;
	
	public KNearestNeighbor(String p_train, String p_test, String p_train_t, String p_release, String p_genre, int nuser, int nitem, int ngenre) throws IOException{
		this.nuser= nuser;
		this.nitem= nitem;
		//this.ngenre= ngenre;
		
		KNNsetUserItemTest(p_train, p_test, p_train_t, p_release, p_genre);
	}
	
	public KNearestNeighbor(String p_train, String p_test, String p_train_t, String p_release, String p_genre, int nuser, int nitem, int ngenre, int sparse_month) throws IOException{
		this.nuser= nuser;
		this.nitem= nitem;
		//this.ngenre= ngenre;
		this.sparse_month= sparse_month;
		
		try {
			ref_time = new SimpleDateFormat("dd-MMMM-yyyy", Locale.ENGLISH).parse("22-APR-1998").getTime()/1000;
			cut_time = ref_time - 60*60*24*30*sparse_month;
		} 
		catch (ParseException e) {
			System.err.println("Error@KNN()");
		}
		
		KNNsetUserItemTest(p_train, p_test, p_train_t, p_release, p_genre);
	}
	
	/**
	 * 
	 * @param train_p training set
	 * @param test_p
	 * @param genre_p
	 * @throws IOException 
	 */
	public void KNNsetUserItemTest(String p_train, String p_test, String p_train_t, String p_release, String p_genre) throws IOException{	
		ui_train= new SMFactory(nuser,nitem); 
		ui_test= new SMFactory(nuser,nitem); 
		i_test= new int[nitem];
		u_avg_rating= new double[nuser];
		i_avg_rating= new double[nitem];
		u_std= new double[nuser];
		i_release= new long[nitem];
		u_icount= new int[nuser];
		
		try{
			FileInputStream fis= null;
			BufferedReader br= null;
			String[] tokens, tokens2= null;
			String line;
			
			fis= new FileInputStream(p_train);
			br= new BufferedReader(new InputStreamReader(fis));	
			double r_cnt= 0;
			while((line= br.readLine())!=null){
				r_cnt += ui_train.insertRating(line);
			}
			br.close();
			
			ui_train_sparse= ui_train.clone();
			
			double sparsity_orig= (1-(r_cnt/(double)(nuser*nitem)));
			System.out.println("Training set sparsity: "+sparsity_orig);
			
			fis= new FileInputStream(p_test);
			br= new BufferedReader(new InputStreamReader(fis));
			while((line= br.readLine())!=null){
				ui_test.insertRating(line);
				tokens= line.split("[,]");
				int suid= Integer.parseInt(tokens[0]);
				for(int i=1;i<tokens.length;i++){
					tokens2= tokens[i].split("[:]");
					int smid= Integer.parseInt(tokens2[0]);
					double rating= (double) Integer.parseInt(tokens2[1]);
					i_test[smid-1]= 1;
				}
			}
			br.close();
							
			fis= new FileInputStream(p_train_t);
			br= new BufferedReader(new InputStreamReader(fis));
			while((line= br.readLine())!=null){
				ui_train_sparse.insertTimestamp(line);
				
				tokens= line.split("[,]");
				int suid= Integer.parseInt(tokens[0]);
				for(int i=1;i<tokens.length;i++){
					tokens2= tokens[i].split("[:]");
					int smid= Integer.parseInt(tokens2[0]);
					long timestamp= Long.parseLong(tokens2[1]);
					if(sparse_month != -1 && timestamp > cut_time){ 
						ui_train_sparse.deleteRating(suid-1, smid-1);
						r_cnt--;
					}
				}
			}
			br.close();
			
			double sparsity_new= (1-(r_cnt/(double)(nuser*nitem)));
			System.out.println("Training set new sparsity: "+sparsity_new);
			
			fis= new FileInputStream(p_release);
			br= new BufferedReader(new InputStreamReader(fis));
			while((line= br.readLine())!=null){
				tokens= line.split("[:]");
				int mid= Integer.parseInt(tokens[0]);
				long release= Long.parseLong(tokens[1]);
				i_release[mid-1]= release;
			}
			br.close();
		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
				
		for(int i=0;i<nuser;i++){ //rows or users
			double avgsum=0;
			int cnt= 0;
			for(int j=0;j<nitem;j++){ //cols or items
				EntryInfo ei = (EntryInfo) ui_train_sparse.getEntry(i, j);
				if(ei != null){
					cnt++;
					avgsum+=ei.getRating();
				}
			}
			u_avg_rating[i]= avgsum/((double) cnt);
			u_icount[i]= cnt;
			
			if(u_icount[i] == 0)
				u_avg_rating[i] = 0;
		}
		
		//use file to store uu_sim
		FileOutputStream fos= new FileOutputStream("knn_uu_sim.txt");
		BufferedWriter bw= new BufferedWriter(new OutputStreamWriter(fos));
		Similarity sim= new Similarity();
		for(int i=0;i<nuser;i++){
			double[] r_i = ui_train_sparse.getRowRating(i);
			String sim_i = "";
			if(i==0)
				sim_i += "-1";
			else
				sim_i += sim.pearsonCorr(r_i,u_avg_rating[i],ui_train_sparse.getRowRating(0),u_avg_rating[0]);
			for(int j=1;j<nuser;j++){ //lower half
				double[] r_j = ui_train_sparse.getRowRating(j);
				if(i==j)
					sim_i += " -1";
				else
					sim_i += " "+sim.pearsonCorr(r_i,u_avg_rating[i],r_j,u_avg_rating[j]);
			}
			bw.write(sim_i);
			bw.newLine();
			bw.flush();
		}
		bw.close();
		
		for(int i=0;i<nuser;i++){ //rows or users
			double std=0.0, cnt= 0.0, dev= 0.0;
			for(int j=0;j<nitem;j++){ //cols or items
				EntryInfo ei = (EntryInfo) ui_train_sparse.getEntry(i,j);
				if(ei != null && ei.getRating() != 0){
	        		cnt++;
	        		dev = ei.getRating() - u_avg_rating[i];
	        		std += dev * dev;
	        	}
			}
			u_std[i]= Math.sqrt(std/cnt);
		}
		
		for(int j=0;j<nitem;j++){
			int iu_cnt= 0;
			for(int i=0;i<nuser;i++){
				EntryInfo ei = (EntryInfo) ui_train_sparse.getEntry(i,j);
				if(ei != null && ei.getRating() != 0){
					i_avg_rating[j]= i_avg_rating[j]+ei.getRating();
					iu_cnt++;
				}
			}
			if(iu_cnt != 0 )
				i_avg_rating[j]= i_avg_rating[j]/iu_cnt;
			else
				i_avg_rating[j]= 0.0;
		}
	}
	
	/**
	 * 1. Mean Absolute Error (accuracy)
	 * 2. Hit Ratio (precision): #hits/#recs, for an active user in the test set
	 * generate recommendation lists for the test set predicted rating > active user's average rating;
	 * consider recommendation is a hit only if true rating > active user's average rating;
	 * 3. recall (recall): #hits/#liked, for an active user in the test set
	 * 
	 * @param k k-nearest neighbors
	 * @param type 1-MAE, 2-hit ratio, 3-recall, 5- missed
	 * @return double[] {MAE,hit ratio,recall,missed}
	 */
	public double[] knnEval(int k, int type){
		double[] output = new double[4];
		
		int nrating= 0; //cardinality of (u,m) pairs in test set
		int nrec= 0;
		int nhit_rec= 0;
		int nhit_liked= 0;
		int nliked= 0;
		double missed= 0; //type=5
		double total= 0;
		
		double mae= 0.0; //type=1
		double precision= 0.0; //type=2, hit ratio
		double recall= 0.0; // type=3		
				
		double mae_one = 0.0;
		double nrating_one = 0;
		
		//FileOutputStream fos = null;
		//BufferedWriter bw = null;
		
		try{
			/**String wdir= "C:/Users/user/workspace/MyFavoriteExperts/dataset/MovieLens/100k_data"; //working directory
			fos = new FileOutputStream(wdir+"/knn_neighbor.txt");
			bw = new BufferedWriter(new OutputStreamWriter(fos));*/
			
			FileStorage fs_uu_sim = new FileStorage("knn_uu_sim.txt");
			fs_uu_sim.open(); 
			for(int i=0;i<nuser;i++){
				int suid= i+1; 
				
				double[] uu_sim = fs_uu_sim.seqAccess();
				int[] neighbor= Tools.sortTopK(uu_sim, k); //(uu_sim[suid-1], k);
				for(int j=0;j<neighbor.length;j++){
					neighbor[j]= neighbor[j]+1;
					
					//-------------------------------------------------
					/**if(suid == 15){
						bw.write("suid:"+suid + " esuid:"+neighbor[j]);
						bw.newLine();
						bw.flush();
					}*/
					//-------------------------------------------------
				}
				
				for(int j=0;j<nitem;j++){
					int smid= j+1;
					
					EntryInfo ei = (EntryInfo) ui_test.getEntry(i,j);
					
					if(ei == null)
						continue;
					
					double trating= ei.getRating();
					double prating= predict(neighbor,uu_sim,suid,smid); 					
								
					total++;
					if(true){
						mae+= Math.abs(trating-prating);
						nrating++;
						
						//----------------------------------------
						/**if(suid == 15){
							mae_one+= Math.abs(trating-prating);
							nrating_one++;
						}*/
						//----------------------------------------
					}
					if(prating > u_avg_rating[suid-1]){
						nrec++;
						if(trating > u_avg_rating[suid-1]){
							nhit_rec++;
						}
					}
					if(trating > u_avg_rating[suid-1]){
						nliked++;
						if(prating > u_avg_rating[suid-1]){
							nhit_liked++;
						}
					}
					if(u_avg_rating[suid-1] == prating){
						missed++;
					}
				}
				//------------------------------------
				/**if(type == 1){
					if(suid == 15){
						bw.write("mae: "+mae_one/nrating_one);
						bw.newLine();
						bw.flush();
						
						System.out.println("mae for this person..." + mae_one/nrating_one);
					}					
				}*/
				//-------------------------------------
			}
			fs_uu_sim.close();
		}
		catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		
		output[0]= mae/nrating;
		output[1]= (double) nhit_rec / (double) nrec;
		output[2]= (double) nhit_liked / (double) nliked;
		output[3]= (double) missed/total;
		return output;
	}
	
	/**
	 * 
	 * @param k
	 * @param type
	 * @param nrec_item
	 * @return double[] {diversity, item_cov, user_cov}
	 */
	public double[] knnEval2(int k, int type, int nrec_size){
		double[] output = new double[3];
		
		double diversity= 0.0; //type=4
		//type=6 item_cov
		//type=7 user_cov
		
		int[] accessed_cnt= null;
		
		SMFactory ui_pred = new SMFactory(nuser,nitem);
		accessed_cnt= new int[nitem];
		
		try{
			FileStorage fs_uu_sim = new FileStorage("knn_uu_sim.txt");
			fs_uu_sim.open();
			for(int i=0;i<nuser;i++){
				int suid= i+1;
				
				double[] uu_sim = fs_uu_sim.seqAccess();
				int[] neighbor= Tools.sortTopK(uu_sim, k); //(uu_sim[suid-1], k);
				for(int j=0;j<neighbor.length;j++){
					neighbor[j]= neighbor[j]+1;
				}
				
				//String row_ui_pred= ""+suid; //SMFactory1
				for(int j=0;j<nitem;j++){
					int smid= j+1;
					EntryInfo ei = (EntryInfo) ui_test.getEntry(i,j);
					
					if(i_test[smid-1] != 1 || ei == null)
						continue; //only consider items in the test data
					
					double prating= predict(neighbor,uu_sim,suid,smid); //i_avg_rating if missed
					ui_pred.insertRating(suid-1,smid-1,prating);
					//row_ui_pred += ","+smid+":"+prating;
					
					accessed_cnt[smid-1]= accessed_cnt[smid-1]+1;
				}
				//ui_pred.insertRating(row_ui_pred); //SMFactory1
			}
			fs_uu_sim.close();
		}
		catch(IOException ie){
			ie.printStackTrace();
			System.exit(1);
		}

		double sum_top = 0.0, sum_top2 = 0.0;
		double sum_bot = 0.0, sum_bot2 = 0.0;
		
		//recommendation lists for all users
		int[][] ui_recIdx= new int[nuser][nrec_size];
		int[] recommended= new int[nitem];
		for(int i=0;i<nuser;i++){
			ui_recIdx[i]= Tools.sortTopK(ui_pred.getRowRating(i), nrec_size); //smid-1
			
			for(int ii=0;ii<nrec_size;ii++){ //iterate each rec list
				recommended[ui_recIdx[i][ii]]= 1;
			}
			
			int full= 1;
			for(int ii=0;ii<nrec_size;ii++){ //iterate each rec list
				EntryInfo ei = (EntryInfo) ui_test.getEntry(i,ui_recIdx[i][ii]);
				if(ei != null && ei.getRating() <= u_avg_rating[i]){
					full= 0;
				}
			}
			
			if(full == 1){
				sum_top2 += u_icount[i];
			}
			sum_bot2 += u_icount[i];
		}
		
		int count= 0;
		for(int i=0;i<nuser;i++){	
			for(int j=i;j<nuser;j++){ //symmetric
				if(i == j)
					continue;
				
				int ncitem= 0;
				for(int ii=0;ii<nrec_size;ii++){
					for(int jj=0;jj<nrec_size;jj++){
						if(ui_recIdx[i][ii] == ui_recIdx[j][jj])
							ncitem++;
					}
				}
				diversity += 1 - ((double) ncitem/(double) nrec_size);
				count++;
			}
		}
		
		for(int i=0;i<nitem;i++){
			if(recommended[i] == 1){
				sum_top+=accessed_cnt[i];
			}
			sum_bot+=accessed_cnt[i];
		}
		
		output[0] = diversity/count;
		output[1] = sum_top/sum_bot;
		output[2] = sum_top2/sum_bot2;
		
		return output;
	}
	
	private double predict(int[] neighbor, double[] sim, int suid, int smid){
		double sumTop= 0.0;
		double sumBot= 0.0;
		
		for(int i=0;i<neighbor.length;i++){
			EntryInfo ei = (EntryInfo) ui_train.getEntry(neighbor[i]-1,smid-1);
			if(ei != null && ei.getRating() == 0.0) ;
				//System.out.println("knn predict() "+ei.getRating());
			if(ei != null && ei.getRating() != 0.0){
				sumTop += (ei.getRating() - u_avg_rating[neighbor[i]-1]) * sim[neighbor[i]-1];
				sumBot += Math.abs(sim[neighbor[i]-1]);
			}
			else{ //remove this? smoothing by user-genre preference
			}
		} 
		if(sumBot == 0.0){ //cold start problem
			//return i_avg_rating[smid-1]; 
			return u_avg_rating[suid-1]; //all data set contain users information
		}
		
		return u_avg_rating[suid-1] + sumTop/sumBot;
	}	
}
