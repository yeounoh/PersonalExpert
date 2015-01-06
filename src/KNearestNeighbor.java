import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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

	private double[][] ui_train;
	private double[][] ui_train_sparse;
	private double[][] ui_test;
	private int[] i_test;
	private double[] u_avg_rating;
	private double[] i_avg_rating;
	private double[] u_std;
	private double[][] uu_sim;
	private double[][] u_avg_genre;
	private int[][] i_genre;
	private long[][] ui_time;
	private long[] i_release;
	private long ref_time; //22-Apr-1998
	private long cut_time;
	private int[] u_icount;
	
	private int nuser;
	private int nitem;
	private int ngenre;
	private int sparse_month = -1;
	
	public KNearestNeighbor(String p_train, String p_test, String p_train_t, String p_release, String p_genre, int nuser, int nitem, int ngenre){
		this.nuser= nuser;
		this.nitem= nitem;
		this.ngenre= ngenre;
		
		KNNsetUserItemTest(p_train, p_test, p_train_t, p_release, p_genre);
	}
	
	public KNearestNeighbor(String p_train, String p_test, String p_train_t, String p_release, String p_genre, int nuser, int nitem, int ngenre, int sparse_month){
		this.nuser= nuser;
		this.nitem= nitem;
		this.ngenre= ngenre;
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
	 */
	public void KNNsetUserItemTest(String p_train, String p_test, String p_train_t, String p_release, String p_genre){	
		ui_train= new double[nuser][nitem];
		ui_test= new double[nuser][nitem];
		i_test= new int[nitem];
		u_avg_rating= new double[nuser];
		i_avg_rating= new double[nitem];
		u_std= new double[nuser];
		uu_sim= new double[nuser][nuser]; 
		u_avg_genre= new double[nuser][ngenre];
		i_genre= new int[nitem][ngenre];
		ui_time= new long[nuser][nitem];
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
				tokens= line.split("[,]");
				int suid= Integer.parseInt(tokens[0]);
				for(int i=1;i<tokens.length;i++){
					tokens2= tokens[i].split("[:]");
					int smid= Integer.parseInt(tokens2[0]);
					double rating= (double) Integer.parseInt(tokens2[1]);
					ui_train[suid-1][smid-1]= rating;
					
					if(rating > 0)
						r_cnt++;
				}
			}
			br.close();
			
			ui_train_sparse= ui_train.clone();
			
			double sparsity_orig= (1-(r_cnt/(double)(nuser*nitem)));
			System.out.println("Training set sparsity: "+sparsity_orig);
			
			/**
			if(sparse_month != -1.0){
				int remove_cnt= (int) Math.ceil(delta_sparse/100 * nitem);
				Random rs= new Random();
				
				for(int i=0;i<nuser;i++){
					int removed_cnt= 0, cnt= 0;
					while(cnt < nitem && removed_cnt < remove_cnt){
						int j= rs.nextInt(nitem);
						if(ui_train[i][j] > 0){
							ui_train[i][j]= 0;
							removed_cnt++;
							r_cnt--;
						}
						cnt++;
					}
				}
				double sparsity_new= (1-(r_cnt/(double)(nuser*nitem)));
				System.out.println("Training set new sparsity: "+sparsity_new);
			}
			*/
			
			fis= new FileInputStream(p_genre);
			br= new BufferedReader(new InputStreamReader(fis));
			int item_cnt= 0;
			while((line= br.readLine())!=null){
				tokens= line.split("[:]");
				tokens2= tokens[1].split("[|]");
				
				for(int i=0;i<tokens2.length;i++){
					i_genre[item_cnt][i]= Integer.parseInt(tokens2[i]); 
				}
				item_cnt++;
			}
			br.close();
						
			fis= new FileInputStream(p_test);
			br= new BufferedReader(new InputStreamReader(fis));
			while((line= br.readLine())!=null){
				tokens= line.split("[,]");
				int suid= Integer.parseInt(tokens[0]);
				for(int i=1;i<tokens.length;i++){
					tokens2= tokens[i].split("[:]");
					int smid= Integer.parseInt(tokens2[0]);
					double rating= (double) Integer.parseInt(tokens2[1]);
					ui_test[suid-1][smid-1]= rating;
					i_test[smid-1]= 1;
				}
			}
			br.close();
												
			fis= new FileInputStream(p_train_t);
			br= new BufferedReader(new InputStreamReader(fis));
			while((line= br.readLine())!=null){
				tokens= line.split("[,]");
				int suid= Integer.parseInt(tokens[0]);
				for(int i=1;i<tokens.length;i++){
					tokens2= tokens[i].split("[:]");
					int smid= Integer.parseInt(tokens2[0]);
					long timestamp= Long.parseLong(tokens2[1]);
					ui_time[suid-1][smid-1]= timestamp; 
					
					if(sparse_month != -1 && timestamp > cut_time && ui_train_sparse[suid-1][smid-1] != 0){
						//ui_test[suid-1][smid-1]= 0;
						ui_train_sparse[suid-1][smid-1]= 0;
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
				
		for(int i=0;i<ui_train_sparse.length;i++){ //rows or users
			double avgsum=0;
			int cnt= 0;
			for(int j=0;j<ui_train_sparse[0].length;j++){ //cols or items
				if(ui_train_sparse[i][j]!=0){
	        		cnt++;
	        		avgsum+=ui_train_sparse[i][j];
	        	}
			}
			u_avg_rating[i]= avgsum/((double) cnt);
			u_icount[i]= cnt;
			
			if(u_icount[i] == 0)
				u_avg_rating[i] = 0;
		}
		
		Similarity sim= new Similarity();
		for(int i=0;i<ui_train_sparse.length;i++){
			for(int j=i;j<ui_train_sparse.length;j++){ //lower half
				uu_sim[i][j]= sim.pearsonCorr(ui_train_sparse[i],u_avg_rating[i],ui_train_sparse[j],u_avg_rating[j]);
				uu_sim[j][i]= uu_sim[i][j];
			}
		}
		
		//u_avg_genre
		int[][] u_cnt_genre= new int[nuser][i_genre[0].length];
		for(int i=0;i<ui_train_sparse.length;i++){
			for(int j=0;j<ui_train_sparse[0].length;j++){
				if(ui_train_sparse[i][j] != 0){
					for(int k=0;k<i_genre[0].length;k++){
						if(i_genre[j][k] == 1){
							u_avg_genre[i][k]= u_avg_genre[i][k] + ui_train_sparse[i][j];
							u_cnt_genre[i][k]= u_cnt_genre[i][k] + 1;
						}
					}
				}
			}
		}
		for(int i=0;i<u_avg_genre.length;i++){
			for(int j=0;j<u_avg_genre[0].length;j++){
				if(u_cnt_genre[i][j] == 0){
					u_avg_genre[i][j]= u_avg_rating[i];
				}
				else{
					u_avg_genre[i][j]= u_avg_genre[i][j]/u_cnt_genre[i][j];
				}
			}
		}
		
		for(int i=0;i<ui_train_sparse.length;i++){ //rows or users
			double std=0.0, cnt= 0.0, dev= 0.0;
			for(int j=0;j<ui_train_sparse[0].length;j++){ //cols or items
				if(ui_train_sparse[i][j]!=0){
	        		cnt++;
	        		dev = ui_train_sparse[i][j] - u_avg_rating[i];
	        		std += dev * dev;
	        	}
			}
			u_std[i]= Math.sqrt(std/cnt);
		}
		
		for(int j=0;j<nitem;j++){
			int iu_cnt= 0;
			for(int i=0;i<nuser;i++){
				if(ui_train_sparse[i][j] != 0){
					i_avg_rating[j]= i_avg_rating[j]+ui_train_sparse[i][j];
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
	 * @param type 1-MAE, 2-hit ratio, 3-recall, 4-diversity, 5- missed
	 * @return 
	 */
	public double knnEval(int k, int type){
		int nrating= 0; //cardinality of (u,m) pairs in test set
		int nrec= 0;
		int nhit= 0;
		int nliked= 0;
		double missed= 0; //type=5
		double total= 0;
		
		double mae= 0.0; //type=1
		double precision= 0.0; //type=2
		double recall= 0.0; // type=3		
				
		double mae_one = 0.0;
		double nrating_one = 0;
		
		FileOutputStream fos = null;
		BufferedWriter bw = null;
		
		try{
			String wdir= "C:/Users/user/workspace/MyFavoriteExperts/dataset/MovieLens/100k_data"; //working directory
			fos = new FileOutputStream(wdir+"/knn_neighbor.txt");
			bw = new BufferedWriter(new OutputStreamWriter(fos));
			
			for(int i=0;i<nuser;i++){
				int suid= i+1;
							
				int[] neighbor= Tools.sortTopK(uu_sim[suid-1], k);
				for(int j=0;j<neighbor.length;j++){
					neighbor[j]= neighbor[j]+1;
					
					//-------------------------------------------------
					if(suid == 15){
						bw.write("suid:"+suid + " esuid:"+neighbor[j]);
						bw.newLine();
						bw.flush();
					}
					//-------------------------------------------------
				}
				
				for(int j=0;j<nitem;j++){
					int smid= j+1;
					double trating= ui_test[i][j];
					
					if(type != 4 && trating == 0.0)
						continue;
					
					double prating= predict(neighbor,suid,smid); 					
								
					total++;
					if(type == 1){
						mae+= Math.abs(trating-prating);
						nrating++;
						
						//----------------------------------------
						if(suid == 15){
							mae_one+= Math.abs(trating-prating);
							nrating_one++;
						}
						//----------------------------------------
					}
					else if(type == 2 && prating > u_avg_rating[suid-1]){
						nrec++;
						if(trating > u_avg_rating[suid-1]){
							nhit++;
						}
					}
					else if(type==3 && trating > u_avg_rating[suid-1]){
						nliked++;
						if(prating > u_avg_rating[suid-1]){
							nhit++;
						}
					}
					else if(type == 5 && u_avg_rating[suid-1] == prating){
						missed++;
					}
				}
				//------------------------------------
				if(type == 1){
					if(suid == 15){
						bw.write("mae: "+mae_one/nrating_one);
						bw.newLine();
						bw.flush();
						
						System.out.println("mae for this person..." + mae_one/nrating_one);
					}					
				}
			}
		}
		catch(Exception e){
			
		}
		
		
		if(type == 1){
			mae= mae/nrating;
			return mae;
		}
		else if(type == 2){
			precision= (double) nhit/ (double) nrec;
			return precision;
		}
		else if(type == 3){
			recall= (double) nhit/ (double) nliked;
			return recall;
		}
		else if(type == 5){
			return missed/total;
		}
		return -1.0;
	}
	
	/**
	 * 
	 * @param k
	 * @param type
	 * @param nrec_item
	 * @return
	 */
	public double knnEval2(int k, int type, int nrec_size){
		
		double diversity= 0.0; //type=4
		double precision= 0.0; //type=2
		double recall= 0.0; //type=3
		
		int[] accessed_cnt= null;
		
		double[][] ui_pred= null;
		if(type == 2 || type == 3 || type == 4 || type == 6 || type ==7)
			ui_pred= new double[nuser][nitem];
		
		if(type == 6)
			accessed_cnt= new int[nitem];
		
		for(int i=0;i<nuser;i++){
			int suid= i+1;
						
			int[] neighbor= Tools.sortTopK(uu_sim[suid-1], k);
			for(int j=0;j<neighbor.length;j++){
				neighbor[j]= neighbor[j]+1;
			}
			
			for(int j=0;j<nitem;j++){
				int smid= j+1;
				double trating= ui_test[i][j];
								
				if(i_test[smid-1] != 1)
					continue; //only consider items in the test data
				
				double prating= predict(neighbor,suid,smid); 					
								
				if(type == 2 || type == 3 || type == 4 || type == 6 || type == 7){
					ui_pred[suid-1][smid-1]= prating; //i_avg_rating if missed
					
					if(type == 6 && trating != 0)
						accessed_cnt[smid-1]= accessed_cnt[smid-1]+1;
				}
				
			}
		}
		if(type == 2){
			double avg= 0.0;
			
			//recommendation lists for all users
			int[][] ui_recIdx= new int[nuser][nitem];
			for(int i=0;i<nuser;i++){
				double sum_top= 0;
				double sum_bot= 0;
				
				ui_recIdx[i]= Tools.sortTopK(ui_pred[i], nitem); //smid-1
				
				for(int ii=0;ii<nrec_size;ii++){ //iterate each rec list
					sum_bot++;
					if(ui_test[i][ui_recIdx[i][ii]] > u_avg_rating[i])
						sum_top++;
					/**
					if(ui_pred[i][ui_recIdx[i][ii]] > u_avg_rating[i]){
						sum_bot++;
						if(ui_test[i][ui_recIdx[i][ii]] > u_avg_rating[i])
							sum_top++;
					}
					*/
				}
				avg += sum_top/sum_bot;
			}
			
			return avg/nuser;
		}
		
		if(type == 3){
			double avg= 0;
			
			//recommendation lists for all users
			int[][] ui_recIdx= new int[nuser][nitem];
			for(int i=0;i<nuser;i++){
				double sum_top= 0;
				double sum_bot= 0;
				
				ui_recIdx[i]= Tools.sortTopK(ui_pred[i], nitem); //smid-1
				
				for(int ii=0;ii<nrec_size;ii++){ //iterate each rec list
					if(ui_test[i][ui_recIdx[i][ii]] > u_avg_rating[i])
						sum_top++;
					else
						sum_bot++;
					/**
					if(ui_test[i][ui_recIdx[i][ii]] > u_avg_rating[i]){
						sum_bot++;
						if(ui_pred[i][ui_recIdx[i][ii]] > u_avg_rating[i])
							sum_top++;
					}
					*/
				}
				avg += sum_top/(sum_top+sum_bot);
			}
			
			return avg/nuser;
		}	
		if(type == 4){ 
			//int nrec_item= 20;
			
			//recommendation lists for all users
			int[][] ui_recIdx= new int[nuser][nitem];
			for(int i=0;i<nuser;i++){
				ui_recIdx[i]= Tools.sortTopK(ui_pred[i], nitem); //smid-1
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
			return diversity/count;
		}
		
		else if(type == 6){
						
			double sum_top= 0;
			double sum_bot= 0;
			
			//recommendation lists for all users
			int[][] ui_recIdx= new int[nuser][nitem];
			int[] recommended= new int[nitem];
			for(int i=0;i<nuser;i++){
				ui_recIdx[i]= Tools.sortTopK(ui_pred[i], nitem); //smid-1
				
				for(int ii=0;ii<nrec_size;ii++){ //iterate each rec list
					recommended[ui_recIdx[i][ii]]= 1;
				}
			}
			
			for(int i=0;i<nitem;i++){
				if(recommended[i] == 1){
					sum_top+=accessed_cnt[i];
				}
				sum_bot+=accessed_cnt[i];
			}
			
			return sum_top/sum_bot;
		}
		else if(type == 7){
			double sum_top= 0;
			double sum_bot= 0;
			
			//recommendation lists for all users
			int[][] ui_recIdx= new int[nuser][nitem];
			int[] recommended= new int[nitem];
			for(int i=0;i<nuser;i++){
				ui_recIdx[i]= Tools.sortTopK(ui_pred[i], nitem); //smid-1
				
				int full= 1;
				for(int ii=0;ii<nrec_size;ii++){ //iterate each rec list
					if(ui_pred[i][ui_recIdx[i][ii]] <= u_avg_rating[i]){
						full= 0;
					}
				}
				
				if(full == 1){
					sum_top += u_icount[i];
				}
				sum_bot += u_icount[i];
			}
			
			return sum_top/sum_bot;
		}
		return -1.0;
	}
	
	/**
	 * take top k most similar neighbors; 
	 * if there is no neighbor purchased the item, 
	 * then take the average rating of the active user
	 * 
	 * @param sim user-user similarity vector for the active user
	 * @param suid active user id
	 * @param smid target item id
	 * @param k neighborhood size
	 * @return predicted rating
	 */
	/**
	private double predict(double[] sim, int suid, int smid, int k){
		double sumTop= 0.0;
		double sumBot= 0.0;
		int neighbor_rated= 0;
		
		double[] tmp_sim= sim.clone();
		//int[] topK= Tools.sortTopK(tmp_sim,tmp_sim.length); //note: value = uid-1; 
		int[] topK= Tools.sortTopK(tmp_sim,k); //value = uid-1;
		
		for(int i=0;i<topK.length;i++){
			if(ui_train[topK[i]][smid-1] != 0.0){
				sumTop+= (ui_train[topK[i]][smid-1] - u_avg_rating[topK[i]]) * uu_sim[suid-1][topK[i]];
				//sumTop+= (ui_train[topK[i]][smid-1] - u_avg_rating[topK[i]])/u_std[topK[i]] * uu_sim[suid-1][topK[i]];
				sumBot+= Math.abs(uu_sim[suid-1][topK[i]]);
				neighbor_rated++;
			}
			else{ //remove this? smoothing by user-genre preference
				double prating= 0.0;
				//user-genre smoothing
				int cnt= 0;
				for(int j=0;j<i_genre[0].length;j++){
					if(i_genre[smid-1][j] == 1){
						prating+= u_avg_genre[topK[i]][j];
						cnt++;
					}
				}
				
				prating = prating/cnt; 
				sumTop+= (prating - u_avg_rating[topK[i]]) * uu_sim[suid-1][topK[i]];
				sumBot+= Math.abs(uu_sim[suid-1][topK[i]]);
				neighbor_rated++;
				
				//user smoothing
				prating = u_avg_rating[topK[i]]; 
				sumTop+= (prating - u_avg_rating[topK[i]]) * uu_sim[suid-1][topK[i]];
				sumBot+= Math.abs(uu_sim[suid-1][topK[i]]);
				neighbor_rated++;
			}
			
			if(neighbor_rated >= k)
				break;
		} 
		if(sumBot == 0.0){ //cold start problem
			return i_avg_rating[smid-1];//u_avg_rating[suid-1];
		}
		
		return u_avg_rating[suid-1] + sumTop/sumBot;
		//return u_avg_rating[suid-1] + u_std[suid-1] * sumTop/sumBot;
	}	
	*/
	
	private double predict(int[] neighbor, int suid, int smid){
		double sumTop= 0.0;
		double sumBot= 0.0;
		int neighbor_rated= 0;
		
		for(int i=0;i<neighbor.length;i++){
			if(ui_train[neighbor[i]-1][smid-1] != 0.0){
				sumTop+= (ui_train[neighbor[i]-1][smid-1] - u_avg_rating[neighbor[i]-1]) * uu_sim[suid-1][neighbor[i]-1];
				sumBot+= Math.abs(uu_sim[suid-1][neighbor[i]-1]);
				neighbor_rated++;
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
