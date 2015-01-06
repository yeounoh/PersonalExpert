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
public class RandomSearch {

	private double[][] ui_train;
	private double[][] ui_train_sparse;
	private double[][] ui_valid;
	private double[][] ui_test;
	private int[] i_test;
	private double[] u_avg_rating;
	private double[] i_avg_rating;
	private double[][] uu_sim;
	private int[][] uu_uicount;
	private int[][] uu_cicount;
	private int[] u_icount;
	private int[] i_ucount;
	private long[][] ui_time;
	private long[] i_release;
	private long ref_time; //22-Apr-1998
	private long cut_time;
	private double[][][] uu_expertise;
	
	private int nuser;
	private int nitem;
	private int sparse_month= -1;
	
	public RandomSearch(String p_train, String p_valid, String p_test, String p_train_t, String p_release, int nuser, int nitem){
		this.nuser= nuser;
		this.nitem= nitem;
		
		try {
			ref_time = new SimpleDateFormat("dd-MMMM-yyyy", Locale.ENGLISH).parse("22-APR-1998").getTime()/1000;
		} 
		catch (ParseException e) {
			System.err.println("Error@ExpertMeasure()");
		}

		RSsetUserItemTest(p_train, p_valid, p_test, p_train_t, p_release);
	}
	
	public RandomSearch(String p_train, String p_valid, String p_test, String p_train_t, String p_release, int nuser, int nitem, int sparse_month){
		this.nuser= nuser;
		this.nitem= nitem;
		this.sparse_month= sparse_month;
						
		try {
			ref_time = new SimpleDateFormat("dd-MMMM-yyyy", Locale.ENGLISH).parse("22-APR-1998").getTime()/1000;
			cut_time = ref_time - 60*60*24*30*sparse_month;
		} 
		catch (ParseException e) {
			System.err.println("Error@ExpertMeasure()");
		}
		
		RSsetUserItemTest(p_train, p_valid, p_test, p_train_t, p_release);
	}
	
	/**
	 * 
	 * @param p path to training data
	 */
	public void RSsetUserItemTest(String p_train, String p_valid, String p_test, String p_train_t, String p_release){	
		ui_train= new double[nuser][nitem];
		ui_valid= new double[nuser][nitem];
		ui_test= new double[nuser][nitem];
		i_test= new int[nitem];
		u_avg_rating= new double[nuser];
		i_avg_rating= new double[nitem];
		uu_sim= new double[nuser][nuser];
		uu_uicount= new int[nuser][nuser]; //relative unknown item access
		uu_cicount= new int[nuser][nuser]; //common item access
		u_icount= new int[nuser];
		i_ucount= new int[nitem];
		ui_time= new long[nuser][nitem];
		i_release= new long[nitem];
		uu_expertise= new double[nuser][nuser][7];
		
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
			
			double sparsity_orig= (1-(r_cnt/(double)(nuser*nitem)));
			System.out.println("Training set sparsity: "+sparsity_orig);
			
			ui_train_sparse = ui_train.clone();
			
			/**
			if(delta_sparse != -1.0){
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
			
			fis= new FileInputStream(p_valid);
			br= new BufferedReader(new InputStreamReader(fis));				
			while((line= br.readLine())!=null){
				tokens= line.split("[,]");
				int suid= Integer.parseInt(tokens[0]);
				for(int i=1;i<tokens.length;i++){
					tokens2= tokens[i].split("[:]");
					int smid= Integer.parseInt(tokens2[0]);
					double rating= (double) Integer.parseInt(tokens2[1]);
					ui_valid[suid-1][smid-1]= rating;
				}
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
					
					if(sparse_month != -1 && timestamp > cut_time && ui_train_sparse[suid=1][smid-1] != 0){
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
			System.exit(1);
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
		
		for(int j=0;j<ui_train_sparse[0].length;j++){
			int cnt= 0;
			for(int i=0;i<ui_train_sparse.length;i++){
				if(ui_train_sparse[i][j]!=0)
					cnt++;
			}
			i_ucount[j]= cnt;
		}
		
		Similarity sim= new Similarity();
		for(int i=0;i<ui_train_sparse.length;i++){
			for(int j=i;j<ui_train_sparse.length;j++){ //upper half
				uu_sim[i][j]= sim.pearsonCorr(ui_train_sparse[i],u_avg_rating[i],ui_train_sparse[j],u_avg_rating[j]);
				uu_sim[j][i]= uu_sim[i][j];
				
				double[] temp_ui= ui_train_sparse[i];
				double[] temp_uj= ui_train_sparse[j];
				int cnt_ij= 0, cnt_ji= 0;
				for(int k=0;k<temp_ui.length;k++){ //for each item
					if((temp_ui[k]!=0) && (temp_uj[k]==0))
						cnt_ij++;
					if((temp_ui[k]==0) && (temp_uj[k]!=0))
						cnt_ji++;
					if((temp_ui[k]!=0) && (temp_uj[k]!= 0)){
						uu_cicount[i][j]= uu_cicount[i][j] + 1;
					}
						
				}
				uu_uicount[i][j]= cnt_ij; 
				uu_uicount[j][i]= cnt_ji;
				uu_cicount[j][i]= uu_cicount[i][j];
			}
		}
		
		for(int j=0;j<nitem;j++){
			int iu_cnt= 0;
			for(int i=0;i<nuser;i++){
				if(ui_train[i][j] != 0){
					i_avg_rating[j]= i_avg_rating[j]+ui_train_sparse[i][j];
					iu_cnt++;
				}
			}
			if(iu_cnt !=0 )
				i_avg_rating[j]= i_avg_rating[j]/iu_cnt;
			else
				i_avg_rating[j]= 0.0;
		}
		
		//expertise
		for(int i=0;i<nuser;i++){
			for(int j=0;j<nuser;j++){
				uu_expertise[i][j]= getExpertise(i+1,j+1,2);
			}
		}
		
		double[][] norm_param= new double[7][2];
		for(int k=0;k<4;k++){
			double mu= 0.0;
			for(int i=0;i<nuser;i++){
				for(int j=0;j<nuser;j++){
					mu+= uu_expertise[i][j][k];
				}
			}
			norm_param[k][0]= mu/((double) nuser*nuser);
		}
		for(int k=0;k<4;k++){
			double std= 0.0;
			for(int i=0;i<nuser;i++){
				for(int j=0;j<nuser;j++){
					double dev= uu_expertise[i][j][k]-norm_param[k][0];
					std+= dev*dev;
				}
			}
			norm_param[k][1]= Math.sqrt(std/((double) nuser*nuser));
		}
		
		for(int i=0;i<nuser;i++){
			for(int j=0;j<nuser;j++){
				for(int k=0;k<4;k++){
					uu_expertise[i][j][k]= (uu_expertise[i][j][k]-norm_param[k][0])/norm_param[k][1];
				}
			}
		}				
	}
		
	/**
	 * find optimal expert group for each user in the test data,
	 * using pure random search algorithm,
	 * given the following constraint (search space reduction):
	 * 1) top k*3 similar users
	 * 2) top k*3 |<EA,HA,NA>|
	 * 3) top k*3 |<EA,HA,NA>| && top k similar users
	 * 
	 * @param k
	 * @param const_type: 1- (1), 2- (2)
	 */
	public void findExperts(String p_optimal, int k, int const_type){
		try{
			FileOutputStream fos= null;
			BufferedWriter bw= null;
			String line;
			
			fos= new FileOutputStream(p_optimal);
			bw= new BufferedWriter(new OutputStreamWriter(fos));
			
			int search_size = k*3;
			if(k*3 > nuser){
				search_size = nuser;
			}
			
			for(int i=0;i<nuser;i++){
				double mae= 5.0;
				int[] optimal= new int[k]; //optimal expert group for a given user, uid
				
				//reduce search space
				int[] search= null;
				if(const_type == 1){
					search = Tools.sortTopK(uu_sim[i], search_size);
				}
				else if(const_type == 2){
					double[] expertise= new double[nuser];
					
					for(int j=0;j<nuser;j++){
						double[] tmp = uu_expertise[i][j]; //getExpertise(i+1,j+1,2); //2- common expertise
						expertise[j] = Tools.vector_magnitude(new double[]{tmp[1], tmp[2], tmp[3]});
					}
					search = Tools.sortTopK(expertise, search_size); //uid - 1
				} 
				System.out.print(""+i+"/");
				
				Random rand= new Random();
				HashMap<String,String> check= new HashMap<String,String>(k);
				
				//initialize expert group
				int nexp= 0;
				while(nexp < k){		
					int idx= rand.nextInt(k*3);
					
					if(search[idx] == i) //exclude oneself
						continue;
					
					if(!check.containsKey(""+(search[idx]+1))){
						optimal[nexp]= search[idx]+1;
						check.put(""+(search[idx]+1), "expert");
						nexp++;
					}
				}
				
				//variable-depth neighbor search
				int[] search_depth= new int[]{1};
				for(int j=0;j<search_depth.length;j++){
					
					int trial= 0;
					while(trial < k*5){ //repeat
						int[] toptimal= optimal.clone();
						
						int switch_cnt= 0;
						while(switch_cnt < search_depth[j]){ //k-exchange neighbor
							int idx_search= rand.nextInt(k*3);
							int idx_optimal= rand.nextInt(k);
							
							if(search[idx_search] == i)
								continue;
							
							if((search[idx_search]+1) != toptimal[idx_optimal]){
								if(!check.containsKey(""+(search[idx_search]+1))){
									check.remove(""+toptimal[idx_optimal]);
									check.put(""+(search[idx_search]+1), "expert");
									toptimal[idx_optimal]= search[idx_search]+1;
									
									switch_cnt++; //increment switch counter	
								}
							}
						}
						
						//evaluate
						double tmae= 0.0;
						int nrating= 0;
						for(int ii=0;ii<nuser;ii++){
							int suid= ii+1;
							for(int jj=0;jj<nitem;jj++){
								int smid= jj+1;
								double trating= ui_valid[ii][jj];
								
								if(trating == 0.0)
									continue;
								
								double prating= predict(toptimal,suid,smid); 					
								
								if(Double.isNaN(prating)){
									System.out.println("error@rsEval()");
									System.exit(1);
								}
								
								tmae+= Math.abs(trating-prating);
								nrating++;
							}
						}
						
						tmae= tmae/nrating;
						if(mae > tmae){
							mae= tmae;
							optimal= toptimal;
						}
						
						trial++;
					}
				}
				
				//record the optimal expert group for the user
				line= ""+(i+1); //suid 
				for(int j=0;j<optimal.length;j++){
					line= line+"," + optimal[j]; //suid,e_suid,e_suid, ...
				}
				bw.write(line);
				bw.flush();
				bw.newLine();
			}
			bw.close();
		}catch (Exception e){ 
			System.err.println("Error@findExperts(): " + e.getMessage());
			e.printStackTrace();
		}
	}
		
	/**
	 * find optimal expert group for each user in the test data,
	 * using pure random search algorithm
	 * 
	 * @param expert_p optimal experts record
	 * @param k
	 * @param const_type
	 * @param type metric type
	 */
	public double rsEval(String p_expert, int k, int type) throws Exception{
		int nrating= 0; //cardinality of (u,m) pairs in test set
		int nrec= 0;
		int nhit= 0;
		int nliked= 0;
		
		double mae= 0.0; //type=1
		double precision= 0.0; //type=2
		double recall= 0.0; // type=3
				
		for(int i=0;i<nuser;i++){
			int suid= i+1;
									
			int[] optimal= new int[k];
			try{
				FileInputStream fis= new FileInputStream(p_expert);
				BufferedReader br= new BufferedReader(new InputStreamReader(fis));
				
				String line;
				while((line= br.readLine()) != null){
					String[] token= line.split("[,]"); 
					if(suid == Integer.parseInt(token[0])){
						for(int ii=0;ii<k;ii++){
							optimal[ii]= Integer.parseInt(token[ii+1]);
						}
						break;
					}
				}
				br.close();
			}
			catch(Exception e){
				e.printStackTrace();
				System.exit(1);
			}
			
			for(int j=0;j<nitem;j++){
				int smid= j+1;
				double trating= ui_test[i][j];
				
				if(type != 4 && trating == 0.0) //test set
					continue;
				
				double prating= predict(optimal,suid,smid);				
				
				if(type == 1){
					mae+= Math.abs(trating-prating);
					nrating++;
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
			}
		}
		
		if(type == 1){
			mae= mae/nrating;
			return mae;
		}
		else if(type == 2){
			precision= (double) nhit/ (double) nrec;
			return precision;
		}
		else if(type ==3){
			recall= (double) nhit/ (double) nliked;
			return recall;
		}
		return -1.0;
	}
	
	/**
	 * find optimal expert group for each user in the test data,
	 * using pure random search algorithm
	 * 
	 * @param expert_p optimal experts record
	 * @param k
	 * @param const_type
	 * @param type metric type
	 */
	public double rsEval2(String p_expert, int k, int type, int nrec_size) throws Exception{
		
		double diversity= 0.0; //type=4
		double precision= 0.0; //type=2
		double recall= 0.0; //type=3
//		double missed= 0; //type=5
//		double total= 0;
		int[] accessed_cnt= null;
		
		double[][] ui_pred= null;
		if(type == 2 || type == 3 || type == 4 || type == 6 || type ==7)
			ui_pred= new double[nuser][nitem];
		
		if(type == 6)
			accessed_cnt= new int[nitem];
		
		for(int i=0;i<nuser;i++){
			int suid= i+1;
			
			int[] optimal= new int[k];
			try{
				FileInputStream fis= new FileInputStream(p_expert);
				BufferedReader br= new BufferedReader(new InputStreamReader(fis));
				
				String line;
				while((line= br.readLine()) != null){
					String[] token= line.split("[,]"); 
					if(suid == Integer.parseInt(token[0])){
						for(int ii=0;ii<k;ii++){
							optimal[ii]= Integer.parseInt(token[ii+1]);
						}
						break;
					}
				}
				br.close();
			}
			catch(Exception e){
				e.printStackTrace();
				System.exit(1);
			}
			
			for(int j=0;j<nitem;j++){
				int smid= j+1;
				double trating= ui_test[i][j];
												
				if(i_test[smid-1] != 1)
					continue; //only consider items in the test data
				
//				total++;
				double prating= predict(optimal,suid,smid); 					
								
				if(type == 2 || type == 3 || type == 4 || type == 6 || type == 7){
					ui_pred[suid-1][smid-1]= prating; //i_avg_rating if missed
					
					if(type == 6 && trating != 0)
						accessed_cnt[smid-1]= accessed_cnt[smid-1]+1;
				}
//				else if(type == 5 && u_avg_rating[suid-1] == prating){
//					missed++;
//				}
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
//		else if(type == 5){
//			return missed/total;
//		}
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
	 * take k experts;
	 * make prediction using similarity weighted average on ratings
	 * 
	 * @param optimal contains uids of experts
	 * @param suid active uid
	 * @param smid target item id
	 * @return predicted rating
	 */
	public double predict(int[] optimal, int suid, int smid){
		double sumTop= 0.0;
		double sumBot= 0.0;
		
		//for each user 
		for(int i=0;i<optimal.length;i++){ 
			if(optimal[i] == -1)
				continue;
			
			if(ui_train[optimal[i]-1][smid-1] != 0){ //for an expert who rated m
				sumTop+= (ui_train[optimal[i]-1][smid-1] - u_avg_rating[optimal[i]-1]) * uu_sim[suid-1][optimal[i]-1];
				sumBot+= Math.abs(uu_sim[suid-1][optimal[i]-1]); 
			}
			else{ //for an expert who hasn't rated m
				
			}
		}
		if(sumBot == 0.0){
			return u_avg_rating[suid-1];
			//return i_avg_rating[smid-1]; //
		}
		return u_avg_rating[suid-1] + sumTop/sumBot;
	}
	
	/**
	 * expertise measure (feature set)
	 * 
	 * @param suid
	 * @param esuid
	 * @param exp_type
	 * @return double[]  
	 */
public double[] getExpertise(int suid, int esuid, int exp_type){
		
	double[] exps = new double[4];
	
	if(exp_type == 0){ //KIIS'12: personalized expertise
		
		exps[0]= uu_sim[suid-1][esuid-1];
		exps[1]= ((double)(u_icount[esuid-1]-u_icount[suid-1]))/ui_train_sparse[0].length; 
		exps[2]= ((double) uu_uicount[esuid-1][suid-1])/ui_train_sparse[0].length; 
		exps[3]= (u_avg_rating[esuid-1]-u_avg_rating[suid-1])/4.0;
	}
	else if(exp_type == 1){ // MLDM'13: personalized expertise
		
		//early adoption degree
		double ea= 0.0;
		for(int i=0;i<ui_train_sparse[0].length;i++){
			if(ui_train_sparse[esuid-1][i] != 0.0){
				ea= ea + ((double) (ref_time - ui_time[esuid-1][i]))/((double) (ref_time - i_release[i]));
			}
		}
		ea= ea/u_icount[esuid-1];
		if(u_icount[esuid-1] == 0)
			ea= 0.0;
		
		//heavy access degree
		double ha= 0.0;
		int max_cnt= 0;
		for(int i=0;i<u_icount.length;i++){
			if(u_icount[i] > max_cnt)
				max_cnt= u_icount[i];
		}
		ha= Math.log(u_icount[esuid-1]+1)/Math.log(max_cnt+1);
		if(Math.log(max_cnt+1) == 0)
			ha= 0.0;
		
		//niche item access degree
		double na= 0.0;
		for(int i=0;i<ui_train_sparse[0].length;i++){
			if(ui_train_sparse[esuid-1][i] != 0.0){
				na= na + Math.log(2)/Math.log(i_ucount[i]+1);
			}
		}
		na= na/u_icount[esuid-1];
		if(u_icount[esuid-1] == 0)
			na= 0.0;
		
		exps[0]= uu_sim[suid-1][esuid-1];
		exps[1]= ea * exps[0]; 
		exps[2]= ha * exps[0]; 
		exps[3]= na * exps[0];
	}
	else if(exp_type == 2){ //common expertise
		
		//early adoption degree
		double ea= 0.0;
		for(int i=0;i<ui_train_sparse[0].length;i++){
			if(ui_train_sparse[esuid-1][i] != 0.0){
				ea= ea + ((double) (ref_time - ui_time[esuid-1][i]))/((double) (ref_time - i_release[i]));
			}
		}
		ea= ea/u_icount[esuid-1];
		if(u_icount[esuid-1] == 0)
			ea= 0.0;
		
		//heavy access degree
		double ha= Math.log(u_icount[esuid-1]+1);
		
		//niche item access degree
		double na= 0.0;
		for(int i=0;i<ui_train_sparse[0].length;i++){
			if(ui_train_sparse[esuid-1][i] != 0.0){
				na= na + Math.log(2)/Math.log(i_ucount[i]+1);
			}
		}
		na= na/u_icount[esuid-1];
		if(u_icount[esuid-1] == 0)
			na= 0.0;		
		
		exps[0]= uu_sim[suid-1][esuid-1];
		exps[1]= ea; 
		exps[2]= ha; 
		exps[3]= na;
	}
	else if(exp_type == 3){ //Journal : personalized expertise
		exps= new double[7];
		
		//early adoption degree
		double ea= 0.0;
		for(int i=0;i<ui_train_sparse[0].length;i++){
			if(ui_train_sparse[esuid-1][i] != 0.0){
				ea= ea + ((double) (ref_time - ui_time[esuid-1][i]))/((double) (ref_time - i_release[i]));
			}
		}
		ea= ea/u_icount[esuid-1];
		if(u_icount[esuid-1] == 0)
			ea= 0.0;
		
		//heavy access degree
		double ha= Math.log(u_icount[esuid-1]+1);
		
		//niche item access degree
		double na= 0.0;
		for(int i=0;i<ui_train_sparse[0].length;i++){
			if(ui_train_sparse[esuid-1][i] != 0.0){
				na= na + Math.log(2)/Math.log(i_ucount[i]+1);
			}
		}
		na= na/u_icount[esuid-1];
		if(u_icount[esuid-1] == 0)
			na= 0.0;
		
		//Unknown item access degree
		double ua= Math.log(uu_uicount[esuid-1][suid-1]+1);
		
		//common item access degree
		double ca= Math.log(uu_cicount[esuid-1][suid-1]+1);
		
		//eccentric rating degree
		double er= 0.0;
		for(int i=0;i<ui_train_sparse[0].length;i++){
			if(ui_train_sparse[esuid-1][i] != 0.0){
				er= er + Math.log(Math.abs(i_avg_rating[i]-ui_train_sparse[esuid-1][i])+1)/Math.log(5);
			}
		}
		er= er/u_icount[esuid-1];
		if(u_icount[esuid-1] == 0)
			er= 0.0;
		
		exps[0]= uu_sim[suid-1][esuid-1];
		exps[1]= ea;
		exps[2]= ha;
		exps[3]= na;
		exps[4]= ua;
		exps[5]= ca;
		exps[6]= er;
	}
	return exps;
	}
}
