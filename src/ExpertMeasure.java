import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

	private double[][] ui_train;
	private double[][] ui_train_sparse;
	private double[][] ui_test;
	private double[] u_avg_rating;
	private double[] i_avg_rating;
	private double[][] uu_sim;
	private int[][] uu_uicount; //unknown item count
	private int[][] uu_cicount; //common item count
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
		ui_train= new double[nuser][nitem];
		ui_test= new double[nuser][nitem];
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
			
			ui_train_sparse= ui_train.clone();
			
			double sparsity_orig= (1-(r_cnt/(double)(nuser*nitem)));
			System.out.println("Training set sparsity: "+sparsity_orig);
			
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
					
					if(sparse_month != -1 && timestamp > cut_time && ui_train_sparse[suid-1][smid-1] > 0){
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
		}
		
		for(int j=0;j<ui_train_sparse[0].length;j++){
			int cnt= 0;
			for(int i=0;i<ui_train.length;i++){
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
				if(ui_train_sparse[i][j] != 0){
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
				uu_expertise[i][j]= getExpertise(i+1,j+1,3);
			}
		}
		
		double[][] norm_param= new double[7][2];
		for(int k=0;k<7;k++){
			double mu= 0.0;
			for(int i=0;i<nuser;i++){
				for(int j=0;j<nuser;j++){
					mu+= uu_expertise[i][j][k];
				}
			}
			norm_param[k][0]= mu/((double) nuser*nuser);
		}
		for(int k=0;k<7;k++){
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
				for(int k=0;k<7;k++){
					uu_expertise[i][j][k]= (uu_expertise[i][j][k]-norm_param[k][0])/norm_param[k][1];
				}
			}
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
			
			int[][] experts= new int[uu_sim.length][uu_sim.length]; //mark who is an expert and who is not
			
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
			
			int[] selected_users = new int[line_cnt]; //find experts for each of these selected users
			
			int suser_cnt = 0, pos_cnt= 0;
			while((line = br.readLine())!=null){
				if(line.length() == 0) 
					continue; //empty line (usually the last line)
				
				tokens= line.split("[,]"); //"\\s+"
				suid= Integer.parseInt(tokens[0]); //suid
				selected_users[suser_cnt]= suid; //now it shall contain all users
					
				for(int i=1;i<tokens.length;i++){
					esuid= Integer.parseInt(tokens[i]);
					if(esuid == suid)
						continue;
					
					double[] texps = uu_expertise[suid-1][esuid-1]; //getExpertise(suid,esuid,exp_type); //exp_type
										
					//if((exp_type != 3) && (texps[0] < 0.3)) //filtering(not needed?!): MLDM'13
						//continue;
					
					if(u_icount[esuid-1] == 0)
						continue;
										
					experts[suid-1][esuid-1]= 1;
					
					String psample= "1";
					for(int j=0;j<texps.length;j++){
						psample= psample + " " + (j+1) + ":" + texps[j];
					}
					
					bw.write(psample);
					bw.flush();
					bw.newLine(); 
					
					bw2.write(""+suid+" "+esuid);
					bw2.flush();
					bw2.newLine();
					
					pos_cnt++;
				}
				suser_cnt++;
			}
			
			//within the dataset (selected users), mark other users who are not seen as experts
			for(int i=0;i<selected_users.length;i++){ //selected_users.length
				for(int j=0;j<experts.length;j++){
					int user_i= selected_users[i];
					int user_j= j+1;//selected_users[j];
					
					if(user_i == user_j)
						continue;
					
					if(experts[user_i-1][user_j-1] == 0){
						experts[user_i-1][user_j-1]= -1;
					}
				}
			}
			
			int neg_cnt= 0;
			for(int i=0;i<ui_test.length;i++){
				for(int j=0;j<ui_test.length;j++){
					
					//within the selected user group only, we either have positive/negative data
					if(experts[i][j] == -1){ 
						double[] texps = uu_expertise[i][j];//getExpertise(i+1,j+1,exp_type);
												
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
			bw.close();
			bw2.close();
			br.close();
			
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
