import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Random;


public class CommonExpert {
	//private double[][] ui_train;
	private SMFactory ui_train;
	//private double[][] ui_train_sparse;
	private SMFactory ui_train_sparse;
	//private double[][] ui_valid;
	private SMFactory ui_valid;
	//private double[][] ui_test;
	private SMFactory ui_test;
	private int[] i_test;
	private double[] u_avg_rating;
	private double[] i_avg_rating;
	//private double[][] uu_sim;
	//private int[][] uu_uicount;
	//private int[][] uu_cicount;
	private int[] u_icount;
	private int[] i_ucount;
	//private long[][] ui_time;
	private long[] i_release;
	private long ref_time; //22-Apr-1998
	private long cut_time;
	//private double[][][] uu_expertise;
	private double[][] norm_param;
	
	private int nuser;
	private int nitem;
	private int sparse_month= -1;
	
	public CommonExpert(String p_train, String p_test, String p_train_t, String p_release, int nuser, int nitem){
		this.nuser= nuser;
		this.nitem= nitem;
		
		try {
			this.ref_time = new SimpleDateFormat("dd-MMMM-yyyy", Locale.ENGLISH).parse("22-APR-1998").getTime()/1000;
			this.cut_time = ref_time - 60*60*24*30*0; 
		} 
		catch (ParseException e) {
			System.err.println("Error@ExpertMeasure()");
		}
		
		CEsetUserItemTest(p_train, p_test, p_train_t, p_release);
	}
	
	public CommonExpert(String p_train, String p_test, String p_train_t, String p_release, int nuser, int nitem, int sparse_month){
		this.nuser= nuser;
		this.nitem= nitem;
		this.sparse_month= sparse_month;
		
		try {
			this.ref_time = new SimpleDateFormat("dd-MMMM-yyyy", Locale.ENGLISH).parse("22-APR-1998").getTime()/1000;
			this.cut_time = ref_time - 60*60*24*30*sparse_month;
		} 
		catch (ParseException e) {
			System.err.println("Error@CommonExpert()");
		}
		
		CEsetUserItemTest(p_train, p_test, p_train_t, p_release);
	}
	
	/**
	 * 
	 * @param p path to training data
	 */
	public void CEsetUserItemTest(String p_train, String p_test, String p_train_t, String p_release){	
		ui_train= new SMFactory(nuser,nitem); 
		ui_valid= new SMFactory(nuser,nitem);
		ui_test= new SMFactory(nuser,nitem);
		i_test= new int[nitem];
		u_avg_rating= new double[nuser];
		i_avg_rating= new double[nitem];
		//uu_sim= new double[nuser][nuser];
		//uu_uicount= new int[nuser][nuser]; //relative unknown item access
		//uu_cicount= new int[nuser][nuser]; //common item access
		u_icount= new int[nuser];
		i_ucount= new int[nitem];
		//ui_time= new long[nuser][nitem];
		i_release= new long[nitem];
		//uu_expertise= new double[nuser][nuser][7];
		norm_param = new double[7][2];
		
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
			
			/**
			fis= new FileInputStream(p_valid);
			br= new BufferedReader(new InputStreamReader(fis));
			while((line= br.readLine())!=null){
				ui_valid.insertRating(line);
			}
			br.close();
			*/
			
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
				ui_train.insertTimestamp(line);
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
			
			for(int j=0;j<nitem;j++){
				int cnt= 0;
				for(int i=0;i<nuser;i++){
					EntryInfo ei = (EntryInfo) ui_train_sparse.getEntry(i, j);
					if(ei != null)
						cnt++;
				}
				i_ucount[j]= cnt;
			}
			
			//use file to store uu_sim
			FileOutputStream fos1= new FileOutputStream("ce_uu_sim.txt");
			BufferedWriter bw1= new BufferedWriter(new OutputStreamWriter(fos1));
			FileOutputStream fos2= new FileOutputStream("ce_uu_uicount.txt");
			BufferedWriter bw2= new BufferedWriter(new OutputStreamWriter(fos2));
			FileOutputStream fos3= new FileOutputStream("ce_uu_cicount.txt");
			BufferedWriter bw3= new BufferedWriter(new OutputStreamWriter(fos3));
			Similarity sim= new Similarity();
			for(int i=0;i<nuser;i++){
				double[] r_i = ui_train_sparse.getRowRating(i);
				double[] r_0 = ui_train_sparse.getRowRating(0);
				
				String sim_i = "";
				if(i==0)
					sim_i += "-1";
				else
					sim_i += sim.pearsonCorr(r_i,u_avg_rating[i],r_0,u_avg_rating[0]);
				
				int cnt_ij= 0, cnt_ci= 0;
				for(int k=0;k<nitem;k++){ //for each item
					if((r_i[k]!=0) && (r_0[k]==0))
						cnt_ij++;
					if((r_i[k]!=0) && (r_0[k]!= 0)){
						cnt_ci++;
					}	
				}
				String ui_count_i = ""+cnt_ij;
				String ci_count_i = ""+cnt_ci;
				
				for(int j=1;j<nuser;j++){
					double[] r_j = ui_train_sparse.getRowRating(j);
					if(i==j)
						sim_i += " -1";
					else
						sim_i += " "+sim.pearsonCorr(r_i,u_avg_rating[i],r_j,u_avg_rating[j]);
					cnt_ij= 0; cnt_ci= 0;
					for(int k=0;k<nitem;k++){ //for each item
						if((r_i[k]!=0) && (r_j[k]==0))
							cnt_ij++;
						if((r_i[k]!=0) && (r_j[k]!= 0)){
							cnt_ci++;
						}	
					}
					ui_count_i += " "+cnt_ij;
					ci_count_i += " "+cnt_ci;
				}
				bw1.write(sim_i);bw1.newLine();bw1.flush();
				bw2.write(ui_count_i);bw2.newLine();bw2.flush();
				bw3.write(ci_count_i);bw3.newLine();bw3.flush();
			}
			bw1.close();bw2.close();bw3.close();		
			
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
			
			//expertise
			FileStorage fs_uu_sim = new FileStorage("ce_uu_sim.txt");
			FileStorage fs_uicount = new FileStorage("ce_uu_uicount.txt");
			FileStorage fs_cicount = new FileStorage("ce_uu_cicount.txt");
			
			fs_uu_sim.open(); fs_uicount.open(); fs_cicount.open();
			for(int i=0;i<nuser;i++){
				double[] uu_sim = fs_uu_sim.seqAccess();
				double[] uicount = fs_uicount.seqAccess();
				double[] cicount = fs_cicount.seqAccess();
				for(int j=0;j<nuser;j++){
					double[] info = new double[3];
					info[0] = uu_sim[j]; info[1] = uicount[j]; info[2] = cicount[j];
					double[] exps= getExpertise(info, i+1,j+1,3); //type 3: double[7]
					norm_param[0][0] += exps[0]; norm_param[1][0] += exps[1]; norm_param[2][0] += exps[2]; 
					norm_param[3][0] += exps[3]; norm_param[4][0] += exps[4]; norm_param[5][0] += exps[5]; 
					norm_param[6][0] += exps[6];
				}
			}
			for(int i=0;i<norm_param.length;i++){
				norm_param[i][0] = norm_param[i][0]/((double) nuser*nuser);
			}
			fs_uu_sim.close(); fs_uicount.close(); fs_cicount.close();
			
			double std= 0.0;
			fs_uu_sim.open(); fs_uicount.open(); fs_cicount.open();
			for(int i=0;i<nuser;i++){
				double[] uu_sim = fs_uu_sim.seqAccess();
				double[] uicount = fs_uicount.seqAccess();
				double[] cicount = fs_cicount.seqAccess();
				for(int j=0;j<nuser;j++){
					double[] info = new double[3];
					info[0] = uu_sim[j]; info[1] = uicount[j]; info[2] = cicount[j];
					double[] exps= getExpertise(info, i+1,j+1,3); //type 3: double[7]
					norm_param[0][1] += (exps[0] - norm_param[0][0])*(exps[0] - norm_param[0][0]); 
					norm_param[1][1] += (exps[1] - norm_param[1][0])*(exps[1] - norm_param[1][0]); 
					norm_param[2][1] += (exps[2] - norm_param[2][0])*(exps[2] - norm_param[2][0]); 
					norm_param[3][1] += (exps[3] - norm_param[3][0])*(exps[3] - norm_param[3][0]); 
					norm_param[4][1] += (exps[4] - norm_param[4][0])*(exps[4] - norm_param[4][0]); 
					norm_param[5][1] += (exps[5] - norm_param[5][0])*(exps[5] - norm_param[5][0]); 
					norm_param[6][1] += (exps[6] - norm_param[6][0])*(exps[6] - norm_param[6][0]);
				}
			}
			for(int i=0;i<norm_param.length;i++){
				norm_param[i][1] = Math.sqrt(norm_param[i][1]/((double) nuser*nuser));
			}
			fs_uu_sim.close(); fs_uicount.close(); fs_cicount.close();
			/**
			//----------------------------------------------
			FileOutputStream fos = null;
			BufferedWriter bw= null;
			try{
				String wdir= "C:/Users/user/workspace/MyFavoriteExperts/dataset/MovieLens/100k_data"; //working directory
				fos = new FileOutputStream(wdir+"/15_neighbor_all.txt");
				bw = new BufferedWriter(new OutputStreamWriter(fos));
			
				//expertise
				for(int i=0;i<nuser;i++){
					for(int j=0;j<nuser;j++){
						uu_expertise[i][j]= getExpertise(i+1,j+1,3); //common expertise
						//uu_expertise[i][j]= getExpertise(i+1,j+1,1); //similar common expertise
						//----------------------------------------------- 
						
							if(i == 14){
								
									bw.write("suid:"+ (i+1) + " esuid:" + (j+1) +" sim:" + uu_expertise[i][j][0] + 
											" ea:" + uu_expertise[i][j][1] + " ha:" + uu_expertise[i][j][2] +
											" na:" + uu_expertise[i][j][3] + " ua:" + uu_expertise[i][j][4] +
											" ca:" + uu_expertise[i][j][5] + " er:" + uu_expertise[i][j][6]);
									bw.newLine();
									bw.flush();
							}
						
					}
				}
				bw.close();
			}
			catch(Exception e){
				e.printStackTrace();
				System.err.print(e);
			}
			//----------------------------------------------
			 */
			
			/** now this is done when we compute expertise on the fly
			for(int i=0;i<nuser;i++){
				for(int j=0;j<nuser;j++){
					for(int k=0;k<7;k++){
						uu_expertise[i][j][k]= (uu_expertise[i][j][k]-norm_param[k][0])/norm_param[k][1];
					}
				}
			}
			*/
		}
		catch (Exception e){
			e.printStackTrace();
			System.exit(1);
		}	
	}
	
	/**
	 * find optimal expert group for each user in the test data,
	 * using pure random search algorithm
	 * 
	 * @param p_expert optimal experts record
	 * @param k
	 * @param const_type
	 * @param type metric type
	 */
	public double[] ceEval(int k, int type, int em_type) throws Exception{
		double[] output = new double[4];
		
		int nrating= 0; //cardinality of (u,m) pairs in test set
		int nrec= 0;
		int nhit_rec= 0;
		int nhit_liked= 0;
		int nliked= 0;
		int missed= 0;
		int total= 0;
		
		double mae= 0.0; //type=1
		double precision= 0.0; //type=2
		double recall= 0.0; // type=3

		double[] expertise= new double[nuser];//4 features (common expertise)
		for(int i=0;i<nuser;i++){
			double[] info = new double[3];
			double[] tmp= getExpertise(info,i+1,i+1,2); //2- common expertise? tmp[0] is meaningless
			if(em_type == 1)
				expertise[i] = Tools.vector_magnitude(new double[]{tmp[1], tmp[2], tmp[3]});
			else if(em_type == 2)
				expertise[i] = tmp[1]; //ea
			else if(em_type == 3)
				expertise[i] = tmp[2]; //ha
			else if(em_type == 4)
				expertise[i] = tmp[3]; //na
		}
		
		int[] optimal= Tools.sortTopK(expertise, k); //common experts
		for(int i=0;i<optimal.length;i++){
			optimal[i]= optimal[i] + 1;
		}
		
		FileOutputStream fos = null;
		BufferedWriter bw= null;
		try{
//			String wdir= "C:/Users/user/workspace/MyFavoriteExperts/dataset/MovieLens/100k_data"; //working directory
//			fos = new FileOutputStream(wdir+"/ce_neighbor.txt");
//			bw = new BufferedWriter(new OutputStreamWriter(fos));
			
			FileStorage fs_uu_sim = new FileStorage("ce_uu_sim.txt");
			fs_uu_sim.open(); 
			for(int i=0;i<nuser;i++){
				int suid= i+1;
				
				/**
				for(int ii=0;ii<optimal.length;ii++){
					//-------------------------------------------------
					if(suid == 15){
						bw.write("suid:"+suid + " esuid:"+optimal[ii]);
						bw.newLine();
						bw.flush();
					}
					//-------------------------------------------------
				}*/
				
				double[] uu_sim = fs_uu_sim.seqAccess();
				for(int j=0;j<nitem;j++){
					int smid= j+1;
					EntryInfo ei = (EntryInfo) ui_test.getEntry(i,j);
					if(ei == null)
						continue;
					double trating= ei.getRating();
					
					double prating= predict(optimal,uu_sim,suid,smid);
					
					if(true){
						total++;
						mae+= Math.abs(trating-prating);
						nrating++;
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
				/**
				//------------------------------------
				if(type == 1){
					if(suid == 15){
						bw.write("mae: "+mae_one/nrating_one);
						bw.newLine();
						bw.flush();
						
						System.out.println("mae for this person..." + mae_one/nrating_one);
					}					
				}
				//------------------------------------
				 */
			}
			fs_uu_sim.close();
//			bw.close();
		}
		catch(Exception e){
			e.printStackTrace();
			System.err.print(e);
		}
		
		output[0]= mae/nrating;
		output[1]= (double) nhit_rec / (double) nrec;
		output[2]= (double) nhit_liked / (double) nliked;
		output[3]= missed;
		return output;
	}
	
	/**
	 * find optimal expert group for each user in the test data,
	 * using pure random search algorithm
	 * 
	 * @param p_expert optimal experts record
	 * @param k
	 * @param const_type
	 * @param type metric type
	 */
	public double[] ceEval2(int k, int type, int em_type, int nrec_size) throws Exception{
		double[] output = new double[3];
		
		double diversity= 0.0; //type=4
		
		int[] accessed_cnt= new int[nitem];
		SMFactory ui_pred = new SMFactory(nuser,nitem);
		
		double[] expertise= new double[nuser];//4 features (common expertise)
		for(int i=0;i<nuser;i++){
			double[] info = new double[3];
			double[] tmp= getExpertise(info,i+1,i+1,2); //2- common expertise? tmp[0] is meaningless
			if(em_type == 1)
				expertise[i] = Tools.vector_magnitude(new double[]{tmp[1], tmp[2], tmp[3]});
			else if(em_type == 2)
				expertise[i] = tmp[1]; //ea
			else if(em_type == 3)
				expertise[i] = tmp[2]; //ha
			else if(em_type == 4)
				expertise[i] = tmp[3]; //na
		}
		
		int[] optimal= Tools.sortTopK(expertise, k); //common experts
		for(int i=0;i<optimal.length;i++){
			optimal[i]= optimal[i] + 1;
		}
		
		FileStorage fs_uu_sim = new FileStorage("ce_uu_sim.txt");
		fs_uu_sim.open();
		for(int i=0;i<nuser;i++){
			int suid= i+1;
			
			double[] uu_sim = fs_uu_sim.seqAccess();
			
			for(int j=0;j<nitem;j++){
				int smid= j+1;
				EntryInfo ei = (EntryInfo) ui_test.getEntry(i,j);
				
				if(i_test[smid-1] != 1 || ei == null)
					continue; //only consider items in the test data
				
				double prating= predict(optimal,uu_sim,suid,smid); //i_avg_rating if missed
				ui_pred.insertRating(suid-1,smid-1,prating);
				//row_ui_pred += ","+smid+":"+prating;
				
				accessed_cnt[smid-1]= accessed_cnt[smid-1]+1;
			}
		}
		fs_uu_sim.close();
		
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
	
	public double[] simCeEval(int k, int type) throws Exception{
				
		double[] output = new double[3];
		
		int nrating= 0; //cardinality of (u,m) pairs in test set
		int nrec= 0;
		int nhit_rec= 0;
		int nhit_liked= 0;
		int nliked= 0;
		
		double mae= 0.0; //type=1
		double precision= 0.0; //type=2
		double recall= 0.0; // type=3

		double[] expertise= new double[nuser];//4 features (common expertise)
		for(int i=0;i<nuser;i++){
			double[] info = new double[3];
			double[] tmp= getExpertise(info,i+1,i+1,2); //2- common expertise? tmp[0] is meaningless
			expertise[i] = Tools.vector_magnitude(new double[]{tmp[1], tmp[2], tmp[3]});
		}
		
		int[] search= Tools.sortTopK(expertise, k*3); //common experts
		
		FileOutputStream fos = null;
		BufferedWriter bw= null;
		try{
//			String wdir= "C:/Users/user/workspace/MyFavoriteExperts/dataset/MovieLens/100k_data"; //working directory
//			fos = new FileOutputStream(wdir+"/ce_neighbor.txt");
//			bw = new BufferedWriter(new OutputStreamWriter(fos));
			
			FileStorage fs_uu_sim = new FileStorage("ce_uu_sim.txt");
			fs_uu_sim.open(); 
			for(int i=0;i<nuser;i++){
				int suid= i+1;
				
				double[] uu_sim = fs_uu_sim.seqAccess();
				
				double[] simOfCandidate= new double[k*3];
				for(int ii=0;ii<search.length;ii++){
					simOfCandidate[ii]= uu_sim[search[ii]];
				}
				int[] optimal= Tools.sortTopK(simOfCandidate,  k);
				
				for(int ii=0;ii<optimal.length;ii++){
					optimal[ii]= search[optimal[ii]] + 1;
					/**
					//-------------------------------------------------
					if(suid == 15){
						bw.write("suid:"+suid + " esuid:"+optimal[ii]);
						bw.newLine();
						bw.flush();
					}
					//-------------------------------------------------
					*/
				}
				for(int j=0;j<nitem;j++){
					int smid= j+1;
					EntryInfo ei = (EntryInfo) ui_test.getEntry(i,j);
					if(ei == null)
						continue;
					double trating= ei.getRating();
					
					double prating= predict(optimal,uu_sim,suid,smid);
					
					if(true){
						mae+= Math.abs(trating-prating);
						nrating++;
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
				}
				/**
				//------------------------------------
				if(type == 1){
					if(suid == 15){
						bw.write("mae: "+mae_one/nrating_one);
						bw.newLine();
						bw.flush();
						
						System.out.println("mae for this person..." + mae_one/nrating_one);
					}					
				}
				//------------------------------------
				 */
			}
			fs_uu_sim.close();
//			bw.close();
		}
		catch(Exception e){
			e.printStackTrace();
			System.err.print(e);
		}
		
		output[0]= mae/nrating;
		output[1]= (double) nhit_rec / (double) nrec;
		output[2]= (double) nhit_liked / (double) nliked;
		return output;
	}
	
	public double[] simCeEval2(int k, int type, int nrec_size) throws Exception{
		double[] output = new double[3];
		
		double diversity= 0.0; //type=4
		
		int[] accessed_cnt= new int[nitem];
		SMFactory ui_pred = new SMFactory(nuser,nitem);
		
		double[] expertise= new double[nuser];//4 features (common expertise)
		for(int i=0;i<nuser;i++){
			double[] info = new double[3];
			double[] tmp= getExpertise(info,i+1,i+1,2); //2- common expertise? tmp[0] is meaningless
			expertise[i] = Tools.vector_magnitude(new double[]{tmp[1], tmp[2], tmp[3]});
		}
		
		int[] search= Tools.sortTopK(expertise, k*3); //common experts
		
		FileStorage fs_uu_sim = new FileStorage("ce_uu_sim.txt");
		fs_uu_sim.open();
		for(int i=0;i<nuser;i++){
			int suid= i+1;
			
			double[] uu_sim = fs_uu_sim.seqAccess();
			
			double[] simOfCandidate= new double[k*3];
			for(int ii=0;ii<search.length;ii++){
				simOfCandidate[ii]= uu_sim[search[ii]];
			}
			int[] optimal= Tools.sortTopK(simOfCandidate,  k);
			
			for(int ii=0;ii<optimal.length;ii++){
				optimal[ii]= search[optimal[ii]] + 1;
			}
			for(int j=0;j<nitem;j++){
				int smid= j+1;
				EntryInfo ei = (EntryInfo) ui_test.getEntry(i,j);
				
				if(i_test[smid-1] != 1 || ei == null)
					continue; //only consider items in the test data
				
				double prating= predict(optimal,uu_sim,suid,smid); //i_avg_rating if missed
				ui_pred.insertRating(suid-1,smid-1,prating);
				//row_ui_pred += ","+smid+":"+prating;
				
				accessed_cnt[smid-1]= accessed_cnt[smid-1]+1;
			}
		}
		fs_uu_sim.close();
		
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
	
	/**
	 * take k experts;
	 * make prediction using similarity weighted average on ratings
	 * 
	 * @param optimal contains uids of experts
	 * @param suid active uid
	 * @param smid target item id
	 * @return predicted rating
	 */
	public double predict(int[] optimal, double[] sim, int suid, int smid){
		double sumTop= 0.0;
		double sumBot= 0.0;
		
		//for each user 
		for(int i=0;i<optimal.length;i++){ 
			EntryInfo ei = (EntryInfo) ui_train.getEntry(optimal[i]-1,smid-1);
			if(ei != null && ei.getRating() != 0.0){ //for an expert who rated m
				sumTop+= (ei.getRating() - u_avg_rating[optimal[i]-1]) * sim[optimal[i]-1];
				sumBot+= Math.abs(sim[optimal[i]-1]); 
			}
			else{ //for an expert who hasn't rated m
			}
		}
		if(sumBot == 0.0){
			return u_avg_rating[suid-1];
		}
		return u_avg_rating[suid-1] + sumTop/sumBot;
	}
			
	/**
	 * expertise measure (feature set)
	 * new implementation encourages of using exp_type=3
	 * 
	 * @param suid
	 * @param esuid
	 * @param exp_type
	 * @return double[]  
	 */
	public double[] getExpertise(double[] info, int suid, int esuid, int exp_type){
		
		double[] exps = new double[4];
		
		if(exp_type == 0){ //KIIS'12: personalized expertise
			exps[0]= info[0]; 
			exps[1]= ((double)(u_icount[esuid-1]-u_icount[suid-1]))/nitem; 
			exps[2]= ((double) info[1])/nitem; 
			exps[3]= (u_avg_rating[esuid-1]-u_avg_rating[suid-1])/4.0;
		}
		else if(exp_type == 1){ // MLDM'13: personalized expertise
			
			//early adoption degree
			double ea= 0.0;
			for(int i=0;i<nitem;i++){
				EntryInfo ei = (EntryInfo) ui_train_sparse.getEntry(esuid-1,i);
				if(ei != null){
					ea= ea + ((double) (ref_time - ei.getTimestamp()))/((double) (ref_time - i_release[i]));
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
			for(int i=0;i<nitem;i++){
				EntryInfo ei = (EntryInfo) ui_train_sparse.getEntry(esuid-1,i);
				if(ei != null){
					na= na + Math.log(2)/Math.log(i_ucount[i]+1);
				}
			}
			na= na/u_icount[esuid-1];
			if(u_icount[esuid-1] == 0)
				na= 0.0;
			
			exps[0]= info[0];
			exps[1]= ea * exps[0]; 
			exps[2]= ha * exps[0]; 
			exps[3]= na * exps[0];
		}
		else if(exp_type == 2){ //common expertise
			
			//early adoption degree
			double ea= 0.0;
			for(int i=0;i<nitem;i++){
				EntryInfo ei = (EntryInfo) ui_train_sparse.getEntry(esuid-1,i);
				if(ei != null){
					ea= ea + ((double) (ref_time - ei.getTimestamp()))/((double) (ref_time - i_release[i]));
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
			for(int i=0;i<nitem;i++){
				EntryInfo ei = (EntryInfo) ui_train_sparse.getEntry(esuid-1,i);
				if(ei != null){
					na= na + Math.log(2)/Math.log(i_ucount[i]+1);
				}
			}
			na= na/u_icount[esuid-1];
			if(u_icount[esuid-1] == 0)
				na= 0.0;
			
			exps[0]= info[0];
			exps[1]= ea; 
			exps[2]= ha; 
			exps[3]= na;
		}
		else if(exp_type == 3){ //Journal : personalized expertise
			exps= new double[7];
			
			//early adoption degree
			double ea= 0.0;
			for(int i=0;i<nitem;i++){
				EntryInfo ei = (EntryInfo) ui_train_sparse.getEntry(esuid-1,i);
				if(ei != null){
					ea= ea + ((double) (ref_time - ei.getTimestamp()))/((double) (ref_time - i_release[i]));
				}
			}
			ea= ea/u_icount[esuid-1];
			if(u_icount[esuid-1] == 0)
				ea= 0.0;
			
			//heavy access degree
			double ha= Math.log(u_icount[esuid-1]+1);
			
			//niche item access degree
			double na= 0.0;
			for(int i=0;i<nitem;i++){
				EntryInfo ei = (EntryInfo) ui_train_sparse.getEntry(esuid-1,i);
				if(ei != null){
					na= na + Math.log(2)/Math.log(i_ucount[i]+1);
				}
			}
			na= na/u_icount[esuid-1];
			if(u_icount[esuid-1] == 0)
				na= 0.0;
			
			//Unknown item access degree
			double ua= Math.log(info[1]+1);
			
			//common item access degree
			double ca= Math.log(info[2]+1);
			
			//eccentric rating degree
			double er= 0.0;
			for(int i=0;i<nitem;i++){
				EntryInfo ei = (EntryInfo) ui_train_sparse.getEntry(esuid-1,i);
				if(ei != null){
					er= er + Math.log(Math.abs(i_avg_rating[i]-ei.getRating())+1)/Math.log(5);
				}
			}
			er= er/u_icount[esuid-1];
			if(u_icount[esuid-1] == 0)
				er= 0.0;
			
			exps[0]= info[0];
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
