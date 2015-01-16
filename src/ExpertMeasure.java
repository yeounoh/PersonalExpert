import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

/**
 * Generate SVM training data with attributes:
 * X_ui_uj= <x1_ij, x2_ij, ...>
 * 
 * @author yeounoh chung
 *
 */
public class ExpertMeasure {

	//private double[][] ui_train;
	private SMFactory ui_train;
	//private double[][] ui_train_sparse;
	private SMFactory ui_train_sparse;
	//private double[][] ui_test;
	private SMFactory ui_test;
	private double[] u_avg_rating;
	private double[] i_avg_rating;
	//private double[][] uu_sim; //use FileStorage
	//private int[][] uu_uicount; //unknown item count, use FileStorage
	//private int[][] uu_cicount; //common item count, use FileStorage
	private int[] u_icount;
	private int[] i_ucount;
	//private long[][] ui_time;
	private SMFactory ui_time;
	private long[] i_release;
	private long ref_time; //22-Apr-1998
	private long cut_time;
	//private double[][][] uu_expertise; use FileStorage
	private double[][] norm_param;
	
	private int nuser;
	private int nitem;
	private int sparse_month= -1;
	
	public ExpertMeasure(String p_train, String p_test, String p_train_t, String p_release, int nuser, int nitem){
		this.nuser= nuser;
		this.nitem= nitem;
		
		try {
			ref_time = new SimpleDateFormat("dd-MMMM-yyyy", Locale.ENGLISH).parse("22-APR-1998").getTime()/1000;
		} 
		catch (ParseException e) {
			System.err.println("Error@ExpertMeasure()");
		}
		
		EMsetUserItemTest(p_train, p_test, p_train_t, p_release, nuser, nitem);
	}
	
	public ExpertMeasure(String p_train, String p_test, String p_train_t, String p_release, int nuser, int nitem, int sparse_month){
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
		
		EMsetUserItemTest(p_train, p_test, p_train_t, p_release, nuser, nitem);
	}
	
	/**
	 * 
	 * @param pdir 
	 */
	public void EMsetUserItemTest(String p_train, String p_test, String p_train_t, String p_release, int nuser, int nitem){	
		ui_train= new SMFactory(nuser,nitem);
		ui_test= new SMFactory(nuser,nitem);
		u_avg_rating= new double[nuser];
		i_avg_rating= new double[nitem];
		//uu_sim= new double[nuser][nuser];
		//uu_uicount= new int[nuser][nuser]; //relative unknown item access
		//uu_cicount= new int[nuser][nuser]; //common item access
		u_icount= new int[nuser];
		i_ucount= new int[nitem];
		//ui_time= new long[nuser][nitem];
		i_release= new long[nitem];
		//uu_expertise= new double[nuser][nuser][7]; //FileStorage, 7 files
		norm_param= new double[7][2]; //exp_type 3
		
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
				/**
				tokens= line.split("[,]");
				int suid= Integer.parseInt(tokens[0]);
				for(int i=1;i<tokens.length;i++){
					tokens2= tokens[i].split("[:]");
					int smid= Integer.parseInt(tokens2[0]);
					double rating= (double) Integer.parseInt(tokens2[1]);
					i_test[smid-1]= 1;
				}
				*/
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
			FileOutputStream fos1= new FileOutputStream("em_uu_sim.txt");
			BufferedWriter bw1= new BufferedWriter(new OutputStreamWriter(fos1));
			FileOutputStream fos2= new FileOutputStream("em_uu_uicount.txt");
			BufferedWriter bw2= new BufferedWriter(new OutputStreamWriter(fos2));
			FileOutputStream fos3= new FileOutputStream("em_uu_cicount.txt");
			BufferedWriter bw3= new BufferedWriter(new OutputStreamWriter(fos3));
			Similarity sim= new Similarity();
			for(int i=0;i<nuser;i++){
				double[] r_i = ui_train_sparse.getRowRating(i);
				double[] r_0 = ui_train_sparse.getRowRating(0);
				String sim_i = ""+sim.pearsonCorr(r_i,u_avg_rating[i],r_0,u_avg_rating[0]);
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
			FileStorage fs_uu_sim = new FileStorage("em_uu_sim.txt");
			FileStorage fs_uicount = new FileStorage("em_uicount.txt");
			FileStorage fs_cicount = new FileStorage("em_cicount.txt");
			
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
	 *  
	 * generate SVM training data in a form:
	 * label 1:X1_ij 2:X2_ij 3:X3_ij 4:X4_ij ...
	 * ...
	 * 
	 * from opimal_k00.txt of a form:
	 * suid,e_suid,e_suid, ...
	 * 
	 * @param src_file
	 * @param dst_file
	 * @param exp_type
	 */
	public void makeSVMdata(String p_optimal, String p_svm_train, String p_svm_train_uid){
		try{
			FileInputStream fis= null;
			BufferedReader br= null;
			FileOutputStream fos= null, fos2= null;
			BufferedWriter bw= null, bw2= null;
			String[] tokens= null;
			String line;
			int suid, esuid;
			
			//int[][] experts= new int[uu_sim.length][uu_sim.length]; //mark who is an expert and who is not
			
			fos= new FileOutputStream(p_svm_train);
			bw= new BufferedWriter(new OutputStreamWriter(fos));
			
			fos2= new FileOutputStream(p_svm_train_uid);
			bw2= new BufferedWriter(new OutputStreamWriter(fos2));
			
			fis= new FileInputStream(p_optimal);
			br= new BufferedReader(new InputStreamReader(fis));	
			
			//count the number of the selected users (recomendee)  within the input file
			int line_cnt= 0;
			while((line = br.readLine()) != null){
				if(line.length() != 0)
					line_cnt++;
			}
			br.close();
			
			fis= new FileInputStream(p_optimal);
			br= new BufferedReader(new InputStreamReader(fis));
			
			//int[] selected_users = new int[line_cnt]; //find experts for each of these selected users
			
			FileStorage fs_uu_sim = new FileStorage("em_uu_sim.txt");
			FileStorage fs_uicount = new FileStorage("em_uu_uicount.txt");
			FileStorage fs_cicount = new FileStorage("em_uu_cicount.txt");
			fs_uu_sim.open(); fs_uicount.open(); fs_cicount.open();
			
			int pos_cnt= 0, neg_cnt= 0;
			for(int i=0;i<nuser;i++){
				line = br.readLine();
				
				tokens= line.split("[,]"); //"\\s+"
				suid= Integer.parseInt(tokens[0]); //suid
				//selected_users[suser_cnt]= suid; //now it shall contain all users
				HashMap<String,String> pexperts = new HashMap<String,String>();
				
				double[] uu_sim = fs_uu_sim.seqAccess();
				double[] uicount = fs_uicount.seqAccess();
				double[] cicount = fs_cicount.seqAccess();
				
				for(int j=1;j<tokens.length;j++){
					esuid= Integer.parseInt(tokens[j]);
					if(esuid == suid)
						continue;
					
					//double[] texps = uu_expertise[suid-1][esuid-1]; //getExpertise(suid,esuid,exp_type); //exp_type
					double[] info = new double[3];
					info[0] = uu_sim[esuid-1]; info[1] = uicount[esuid-1]; info[2] = cicount[esuid-1];
					double[] texps= getExpertise(info, suid,esuid,3); 
					
					//if((exp_type != 3) && (texps[0] < 0.3)) //filtering(not needed?!): MLDM'13
						//continue;
					
					if(info[1] == 0)
						continue;
										
					//experts[suid-1][esuid-1]= 1;
					pexperts.put(""+esuid,"");
					
					String psample= "1";
					for(int k=0;k<texps.length;k++){
						psample= psample + " " + (k+1) + ":" + texps[k];
					}
					
					bw.write(psample);
					bw.flush();
					bw.newLine(); 
					
					bw2.write(""+suid+" "+esuid);
					bw2.flush();
					bw2.newLine();
					
					pos_cnt++;
				}
				
				for(int j=0;j<nuser;j++){
					esuid = j+1;
					//within the selected user group only, we either have positive/negative data
					if(!pexperts.containsKey(""+(j+1))){
						//double[] texps = uu_expertise[i][j];//getExpertise(i+1,j+1,exp_type);
						double[] info = new double[3];
						info[0] = uu_sim[esuid-1]; info[1] = uicount[esuid-1]; info[2] = cicount[esuid-1];
						double[] texps= getExpertise(info, suid,esuid,3); 
						
						//if((exp_type != 3) && (texps[0] < 0.3)) //filtering(not needed?!): MLDM'13
							//continue;
						
						String nsample= "-1";
						for(int ii=0;ii<texps.length;ii++){
							nsample= nsample + " " + (ii+1) + ":" + texps[ii];
						}
						
						bw.write(nsample);
						bw.flush();
						bw.newLine();
						
						bw2.write(""+(i+1)+" "+(j+1));
						bw2.flush();
						bw2.newLine();
						
						neg_cnt++;
					}
				} 
			}
			fs_uu_sim.close(); fs_uicount.close(); fs_cicount.close();
			bw.close();bw2.close();br.close();
			
			System.out.println("pos cnt: "+pos_cnt+" neg cnt: "+neg_cnt);
			System.out.println("pos/neg: " + ((double)pos_cnt/(double)neg_cnt));
		}catch (Exception e){
			System.err.println("Error@makeSVMdata(): " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * expertise measure (feature set)
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
