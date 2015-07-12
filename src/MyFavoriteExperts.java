import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Random;

/**
 * finding a user's favorite experts within a user group
 * using the proposed machine learning algorithm.
 * 
 * @author yeounoh chung
 *
 */

public class MyFavoriteExperts {

	private int nuser;
	private int nitem;
	private int ngenre;
	
	public MyFavoriteExperts(int nuser, int nitem, int ngenre){
		this.nuser = nuser;
		this.nitem = nitem;
		this.ngenre = ngenre;
	}
	
	/**
	 * 
	 * @param src
	 * @param dst
	 * @param type : 0 = nothing, 1 = Netflix, 2 = MovieLens
	 */
	private void prepareDataset(String src, String dst, int nfold, int type){
		if(type==0){
			System.out.println("prepareDataset() locked");
			return;
		}
		else if(type==1){ 
			System.out.println("Netflix data experiment");
			
			if(true){ //convert netflix files
				try {
					String dir = System.getProperty("user.dir");
					ConvertFactory cf = new ConvertFactory();
					String src1 = dir+"/dataset/NetFlix/movie_titles.txt";	
					String dst1 = dir + "/dataset/NetFlix/raw_data/u.item";
					cf.convertingMovie(src1,dst1);
					
					//ConvertFactory cf = new ConvertFactory();
					String src_dir2 = dir+"/dataset/NetFlix/training_set";
					String dst2 = dir + "/dataset/NetFlix/raw_data/u.data";
					this.nuser = cf.walk(src_dir2,dst2);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			ParserMovieLens pml= new ParserMovieLens(this.nuser, this.nitem, this.ngenre);
			pml.preprocess(src,dst);
			pml.makeDataSet(dst,nfold); //5-fold cross-validation
			pml.loadItemGenre(src, dst);
			pml.loadItemRelease(src, dst);
		}
		else if(type==2){ //currently in-use
			System.out.println("MovieLens data experiment");
			ParserMovieLens pml= new ParserMovieLens(this.nuser, this.nitem, this.ngenre);
			pml.preprocess(src,dst);
			pml.makeDataSet(dst,nfold); //5-fold cross-validation
			pml.loadItemGenre(src, dst);
			pml.loadItemRelease(src, dst);
			
			for(int f=0;f<nfold;f++){
				pml.makeValidSet(dst+"/user_train_f"+(f+1)+".txt", 5);
			}
		}
		else
			System.out.println("parameter lock is invalid");
		
		System.out.println("prepareDataset() done");
	}
			
	private void kNearestNeighbor(String wdir, int[] k, int nfold, int[] sparse_month, int[] nrec_size){
		try{
			FileOutputStream fos= null, fos2= null, fos3= null, fos4= null, fos5= null, fos6= null, fos7= null;
			BufferedWriter bw= null, bw2= null, bw3= null, bw4= null, bw5= null, bw6= null, bw7= null;
							
			fos= new FileOutputStream(wdir+"/knn_mae_sparse.txt");
			bw= new BufferedWriter(new OutputStreamWriter(fos));
			
			fos2= new FileOutputStream(wdir+"/knn_hit_sparse.txt");
			bw2= new BufferedWriter(new OutputStreamWriter(fos2));
			
			fos3= new FileOutputStream(wdir+"/knn_cov_sparse.txt");
			bw3= new BufferedWriter(new OutputStreamWriter(fos3));
			
			fos4= new FileOutputStream(wdir+"/knn_div_sparse.txt");
			bw4= new BufferedWriter(new OutputStreamWriter(fos4));
			
			//fos5= new FileOutputStream(wdir+"/knn_missed_sparse.txt");
			//bw5= new BufferedWriter(new OutputStreamWriter(fos5));
			
			fos6= new FileOutputStream(wdir+"/knn_covItem_sparse.txt");
			bw6= new BufferedWriter(new OutputStreamWriter(fos6));
			
			fos7= new FileOutputStream(wdir+"/knn_covUser_sparse.txt");
			bw7= new BufferedWriter(new OutputStreamWriter(fos7));
			
			for(int di=0;di<sparse_month.length;di++){
				for(int i=0;i<k.length;i++){	
					for(int f=0;f<nfold;f++){
						
						System.out.println("knn with sparse+: "+sparse_month[di] + ", k: "+k[i]+", f: "+(f+1));
						
						String p_train = wdir+"/user_train_f"+(f+1)+".txt";
						String p_test = wdir+"/user_test_f"+(f+1)+".txt";
						String p_genre = wdir+"/genre.txt";
						String p_train_t = wdir+"/user_"+nuser+"_t.txt";
						String p_release = wdir+"/release.txt";
						
						KNearestNeighbor knn= new KNearestNeighbor(p_train, p_test, p_train_t, p_release, p_genre, this.nuser, this.nitem, this.ngenre, sparse_month[di]);
						
						double[] output = knn.knnEval(k[i], 1);
						bw.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+output[0]); //mae
						bw.newLine();
						bw.flush();
						
						bw2.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+output[1]); //precision/hitratio
						bw2.newLine();
						bw2.flush();
						
						bw3.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+output[2]); //recall
						bw3.newLine();
						bw3.flush();
						
						bw5.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+ output[3]); //missed
						bw5.newLine();
						bw5.flush();
						
						for(int si=0;si<nrec_size.length;si++){
							double[] output2 = knn.knnEval2(k[i], 4, nrec_size[si]);
							bw4.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+ nrec_size[si]+" "+output2[0]); //diversity
							bw4.newLine();
							bw4.flush();
							
							//bw5.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+ nrec_size[si]+" "+missed);
							//bw5.newLine();
							//bw5.flush();
							
							bw6.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+ nrec_size[si]+" "+output2[1]); //item_cov
							bw6.newLine();
							bw6.flush();
							
							bw7.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+ nrec_size[si]+" "+output2[2]); //user_cov
							bw7.newLine();
							bw7.flush();
						}
					}
				}
			}
			
			bw.close();
			bw2.close();
			bw3.close();
			bw4.close();
			//bw5.close();
			bw6.close();
			bw7.close();
		}catch(Exception e){
			e.printStackTrace();
			System.err.print(e);
		}
	}
	
	/**
	 * find random search based optimal expert sets
	 * we use train-valid set
	 * 
	 * @param wdir
	 * @param k
	 */
	private void optimalExperts(String wdir, int[] k, int nfold, int[] sparse_month, int[] nrec_size){
		try{
			FileOutputStream fos= null, fos2= null, fos3= null, fos4=null, fos5= null, fos6=null, fos7= null;
			BufferedWriter bw= null, bw2= null, bw3= null, bw4=null, bw5= null, bw6= null, bw7=null;
			
			fos= new FileOutputStream(wdir+"/rs_mae_sparse.txt");
			bw= new BufferedWriter(new OutputStreamWriter(fos));
			
			fos2= new FileOutputStream(wdir+"/rs_hit_sparse.txt");
			bw2= new BufferedWriter(new OutputStreamWriter(fos2));
			
			fos3= new FileOutputStream(wdir+"/rs_cov_sparse.txt");
			bw3= new BufferedWriter(new OutputStreamWriter(fos3));
			
			fos4= new FileOutputStream(wdir+"/rs_div_sparse.txt");
			bw4= new BufferedWriter(new OutputStreamWriter(fos4));
			
			//fos5= new FileOutputStream(wdir+"/rs_missed.txt");
			//bw5= new BufferedWriter(new OutputStreamWriter(fos5));
			
			fos6= new FileOutputStream(wdir+"/rs_covItem_sparse.txt");
			bw6= new BufferedWriter(new OutputStreamWriter(fos6));
			
			fos7= new FileOutputStream(wdir+"/rs_covUser_sparse.txt");
			bw7= new BufferedWriter(new OutputStreamWriter(fos7));
			
			for(int di=0;di<sparse_month.length;di++){
				for(int i=0;i<k.length;i++){
					for(int f=0;f<nfold;f++){
						System.out.println("rs sparse+: "+sparse_month[di] + ", k: "+k[i]+", f: "+(f+1));
						
						String p_train = wdir+"/user_train_f"+(f+1)+".txt";
						String p_train_v = wdir+"/user_train_f"+(f+1)+"_tr.txt";
						String p_valid = wdir+"/user_train_f"+(f+1)+"_vl.txt";
						String p_test = wdir+"/user_test_f"+(f+1)+".txt";
						String p_train_t = wdir+"/user_"+nuser+"_t.txt";
						String p_release = wdir+"/release.txt";
						//String p_optimal_sim = wdir+"/optimal_k"+k[i]+"_f"+(f+1)+"sim.txt";
						String p_optimal_exp = wdir+"/optimal_k"+k[i]+"_f"+(f+1)+"_s"+sparse_month[di]+"_exp.txt"; //wdir+"/test.txt"; //
						
						RandomSearch rs = new RandomSearch(p_train_v, p_valid, p_test, p_train_t, p_release, this.nuser, this.nitem, sparse_month[di]);
						
						//rs.findExperts(p_optimal_sim, k[i], 1); //const_type = 1, sim
						rs.findExperts(p_optimal_exp, k[i], 2); //const_type = 2, ce

						try{
							rs= new RandomSearch(p_train, p_valid, p_test, p_train_t, p_release, this.nuser, this.nitem, sparse_month[di]);
							
							double[] output = rs.rsEval(p_optimal_exp, k[i], 1);
							
							bw.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+output[0]);
							bw.newLine();
							bw.flush();
					
							bw2.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+output[1]);
							bw2.newLine();
							bw2.flush();
							
							bw3.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+output[2]);
							bw3.newLine();
							bw3.flush();
							
							for(int si=0;si<nrec_size.length;si++){
								double[] output2 = rs.rsEval2(p_optimal_exp, k[i], 4, nrec_size[si]);
								
								bw4.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+ nrec_size[si]+" "+output2[0]);
								bw4.newLine();
								bw4.flush();
								
								bw6.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+ nrec_size[si]+" "+output2[1]);
								bw6.newLine();
								bw6.flush();
								
								bw7.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+ nrec_size[si]+" "+output2[2]);
								bw7.newLine();
								bw7.flush();
							}
						}
						catch(Exception e){
							e.printStackTrace();
							System.exit(1);
						}
					}
				}
			}
			
			bw.close();
			bw2.close();
			bw3.close();
			bw4.close();
			//bw5.close();
			bw6.close();
			bw7.close();
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * generate svmlight training/testing data files;
	 * 
	 * @param wdir
	 * @param k
	 */
	private void getSVMdata(String wdir, int[] k, int[] j, int nfold, int[] sparse_month){
		
		for(int di=0;di<sparse_month.length;di++){
			for(int i=0;i<k.length;i++){
				for(int f=0;f<nfold;f++){
					System.out.println("SVM data generation with sparse+: "+sparse_month[di] + ", k: "+k[i]+", f: "+(f+1));
					
					String p_train = wdir+"/user_train_f"+(f+1)+".txt";
					String p_train_v = wdir+"/user_train_f"+(f+1)+"_tr.txt";
					String p_test = wdir+"/user_test_f"+(f+1)+".txt";
					String p_train_t = wdir+"/user_"+nuser+"_t.txt";
					String p_release = wdir+"/release.txt";
					//String p_optimal_sim = wdir+"/optimal_k"+k[i]+"_f"+(f+1)+"sim.txt";
					String p_optimal_exp = wdir+"/optimal_k"+k[i]+"_f"+(f+1)+"_s"+sparse_month[di]+"_exp.txt";
					String p_svm_train = wdir+"/svm/svm_training_k"+k[i]+"_f"+(f+1)+"_s"+sparse_month[di]+".txt";
					String p_svm_train_uid = wdir+"/svm/svm_training_k"+k[i]+"_f"+(f+1)+"_s"+sparse_month[di]+"_uid.txt";
					
					ExpertMeasure em= new ExpertMeasure(p_train_v,p_test,p_train_t,p_release,this.nuser,this.nitem, sparse_month[di]);
					
					em.makeSVMdata(p_optimal_exp, p_svm_train, p_svm_train_uid); //new feature set
				}
			}
		}
		
		try{
			//make svm script
			FileOutputStream fos= new FileOutputStream(wdir+"/svm/svm_script.bat");
			BufferedWriter bw= new BufferedWriter(new OutputStreamWriter(fos));
			
			for(int di=0;di<sparse_month.length;di++){
				for(int i=0;i<k.length;i++){
					for(int f=0;f<nfold;f++){ //cross-validation
						for(int ji=0;ji<j.length;ji++){
							if(j[ji] == 1){
								bw.write("svm_learn svm_training_k"+k[i]+"_f"+(f+1)+"_s"+sparse_month[di]+".txt svm_j"+j[ji]+"_k"+k[i]+"_f"+(f+1)+"_s"+sparse_month[di]+".dat");
								bw.newLine();
								bw.flush();
								bw.write("svm_classify svm_training_k"+k[i]+"_f"+(f+1)+"_s"+sparse_month[di]+".txt svm_j"+j[ji]+"_k"+k[i]+"_f"+(f+1)+"_s"+sparse_month[di]+".dat output/output_j"+j[ji]+"_k"+k[i]+"_f"+(f+1)+"_s"+sparse_month[di]+".dat > output/result_j"+j[ji]+"_k"+k[i]+"_f"+(f+1)+"_s"+sparse_month[di]+".txt");
								bw.newLine();
								bw.flush();
							}
							else{
								bw.write("svm_learn -j "+j[ji]+" svm_training_k"+k[i]+"_f"+(f+1)+"_s"+sparse_month[di]+".txt svm_j"+j[ji]+"_k"+k[i]+"_f"+(f+1)+"_s"+sparse_month[di]+".dat");
								bw.newLine();
								bw.flush();
								bw.write("svm_classify svm_training_k"+k[i]+"_f"+(f+1)+"_s"+sparse_month[di]+".txt svm_j"+j[ji]+"_k"+k[i]+"_f"+(f+1)+"_s"+sparse_month[di]+".dat output/output_j"+j[ji]+"_k"+k[i]+"_f"+(f+1)+"_s"+sparse_month[di]+".dat > output/result_j"+j[ji]+"_k"+k[i]+"_f"+(f+1)+"_s"+sparse_month[di]+".txt");
								bw.newLine();
								bw.flush();
							}
						}
					}
				}
			}
			bw.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
	}

	
		
	private void personalExperts(String wdir, int[] k, int[] j, int nfold, int[] sparse_month, int[] nrec_size){
		try{
			FileOutputStream fos= null, fos2= null, fos3= null, fos4= null, fos5=null, fos6=null, fos7= null;
			BufferedWriter bw= null, bw2= null, bw3= null, bw4= null, bw5= null, bw6= null, bw7=null;
							
			fos= new FileOutputStream(wdir+"/pe_mae_sparse.txt");
			bw= new BufferedWriter(new OutputStreamWriter(fos));
			
			fos2= new FileOutputStream(wdir+"/pe_hit_sparse.txt");
			bw2= new BufferedWriter(new OutputStreamWriter(fos2));
			
			fos3= new FileOutputStream(wdir+"/pe_cov_sparse.txt");
			bw3= new BufferedWriter(new OutputStreamWriter(fos3));
			
//			fos4= new FileOutputStream(wdir+"/pe_count_sparse.txt");
//			bw4= new BufferedWriter(new OutputStreamWriter(fos4));
			
			fos4= new FileOutputStream(wdir+"/pe_div_sparse.txt");
			bw4= new BufferedWriter(new OutputStreamWriter(fos4));
			
			fos5= new FileOutputStream(wdir+"/pe_missed.txt");
			bw5= new BufferedWriter(new OutputStreamWriter(fos5));
			
			fos6= new FileOutputStream(wdir+"/pe_covItem_sparse.txt");
			bw6= new BufferedWriter(new OutputStreamWriter(fos6));
			
			fos7= new FileOutputStream(wdir+"/pe_covUser_sparse.txt");
			bw7= new BufferedWriter(new OutputStreamWriter(fos7));
			
			
			for(int di=0;di<sparse_month.length;di++){
				for(int i=0;i<k.length;i++){	
					for(int f=0;f<nfold;f++){
						System.out.println("Personalized Expert with sparse+: "+sparse_month[di] + ", k: "+k[i]+", f: "+(f+1));
						
						String p_train = wdir+"/user_train_f"+(f+1)+".txt";
						String p_train_v = wdir+"/user_train_f"+(f+1)+"_tr.txt";
						String p_test = wdir+"/user_test_f"+(f+1)+".txt";
						String p_train_t = wdir+"/user_"+nuser+"_t.txt";
						String p_release = wdir+"/release.txt";
						//String p_optimal_sim = wdir+"/optimal_k"+k[i]+"_f"+(f+1)+"sim.txt";
						
						PersonalizedExpert pe= new PersonalizedExpert(p_train,p_test,p_train_t,p_release,this.nuser,this.nitem,sparse_month[di]);
						
						for(int ji=0;ji<j.length;ji++){
							System.out.println("---evaluation j: "+j[ji]);
							
							String p_svm_output= wdir+"/svm/output/output_j"+j[ji]+"_k"+k[i]+"_f"+(f+1)+"_s"+sparse_month[di]+".dat";
							String p_svm_output_uid= wdir+"/svm/svm_training_k"+k[i]+"_f"+(f+1)+"_s"+sparse_month[di]+"_uid.txt";
							//pe.peStat(p_svm_output, p_svm_output_uid, k[i]);
							double[] output = pe.peEval(p_svm_output, p_svm_output_uid, k[i], 1);
							double mae= output[0]; //mae
							double precision= output[1]; //hit ratio
							double recall= output[2]; //recall
							double missed= output[3]; //missed
									
							bw.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+j[ji]+" "+mae);
							bw.newLine();
							bw.flush();
							
							bw2.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+j[ji]+" "+precision);
							bw2.newLine();
							bw2.flush();
							
							bw3.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+j[ji]+" "+recall);
							bw3.newLine();
							bw3.flush();
							
							bw5.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+ j[ji]+" " + missed);
							bw5.newLine();
							bw5.flush();
							
							for(int si=0;si<nrec_size.length;si++){
								double[] output2 = pe.peEval2(p_svm_output, p_svm_output_uid, k[i], 4, nrec_size[si]);
								double diversity= output2[0]; //diversity
								double item_cov= output2[1]; 
								double user_cov= output2[2]; 
								
								bw4.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+ nrec_size[si]+" "+diversity);
								bw4.newLine();
								bw4.flush();
								
								bw6.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+ nrec_size[si]+" "+item_cov);
								bw6.newLine();
								bw6.flush();
								
								bw7.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+ nrec_size[si]+" "+user_cov);
								bw7.newLine();
								bw7.flush();
							}
						}
					}
				}
			}
			
			bw.close();
			bw2.close();
			bw3.close();
			bw4.close();
			bw5.close();
			bw6.close();
			bw7.close();
		}catch(Exception e){
			e.printStackTrace();
			System.err.print(e);
		}
	}
	
	private void commonExperts(String wdir, int[] k, int nfold, int[] sparse_month, int em_type, int[] nrec_size){
		try{
			FileOutputStream fos= null, fos2= null, fos3= null, fos4= null, fos5=null, fos6=null, fos7= null;
			BufferedWriter bw= null, bw2= null, bw3= null, bw4= null, bw5= null, bw6= null, bw7= null;
			
			String prefix= null;
			if(em_type == 1)
				prefix= "ce";
			else if(em_type == 2)
				prefix= "ea";
			else if(em_type == 3)
				prefix= "ha";
			else if(em_type == 4)
				prefix= "na";
			else if(em_type == 5)
				prefix= "simce";
			
			fos= new FileOutputStream(wdir+"/"+prefix+"_mae_sparse.txt");
			bw= new BufferedWriter(new OutputStreamWriter(fos));
			
			fos2= new FileOutputStream(wdir+"/"+prefix+"_hit_sparse.txt");
			bw2= new BufferedWriter(new OutputStreamWriter(fos2));
			
			fos3= new FileOutputStream(wdir+"/"+prefix+"_cov_sparse.txt");
			bw3= new BufferedWriter(new OutputStreamWriter(fos3));
			
			fos4= new FileOutputStream(wdir+"/"+prefix+"_div_sparse.txt");
			bw4= new BufferedWriter(new OutputStreamWriter(fos4));
			
			fos5= new FileOutputStream(wdir+"/"+prefix+"_missed_sparse.txt");
			bw5= new BufferedWriter(new OutputStreamWriter(fos5));
			
			fos6= new FileOutputStream(wdir+"/"+prefix+"_covItem_sparse.txt");
			bw6= new BufferedWriter(new OutputStreamWriter(fos6));
			
			fos7= new FileOutputStream(wdir+"/"+prefix+"_covUser_sparse.txt");
			bw7= new BufferedWriter(new OutputStreamWriter(fos7));
			
			for(int di=0;di<sparse_month.length;di++){
				for(int i=0;i<k.length;i++){	
					for(int f=0;f<nfold;f++){
						System.out.println("Common Expert with sparse+: "+sparse_month[di] + ", k: "+k[i]+", f: "+(f+1));
						
						String p_train = wdir+"/user_train_f"+(f+1)+".txt";
						String p_train_v = wdir+"/user_train_f"+(f+1)+"_tr.txt";
						String p_test = wdir+"/user_test_f"+(f+1)+".txt";
						String p_train_t = wdir+"/user_"+nuser+"_t.txt";
						String p_release = wdir+"/release.txt";
						
						CommonExpert ce= new CommonExpert(p_train,p_test,p_train_t,p_release,this.nuser,this.nitem,sparse_month[di]);
						
						double mae= 0; //mae
						double precision= 0; //hit ratio -> precision
						double recall= 0; //coverage -> recall
						double diversity= 0; //diversity
						double item_cov= 0;
						double user_cov= 0;
						double missed= 0;
						
						if(em_type == 5){
							double[] output = ce.simCeEval(k[i], 1);
							mae= output[0]; //mae
							precision= output[1]; //hit ratio
							recall= output[2]; //recall
							missed= output[3];
							
							for(int si=0;si<nrec_size.length;si++){
								double[] output2 = ce.simCeEval2(k[i], 4, nrec_size[si]);
								diversity= output2[0]; //diversity
								item_cov= output2[1]; 
								user_cov= output2[2]; 
								
								bw4.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+ nrec_size[si]+" "+diversity);
								bw4.newLine();
								bw4.flush();
								
								bw6.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+ nrec_size[si]+" "+item_cov);
								bw6.newLine();
								bw6.flush();
								
								bw7.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+ nrec_size[si]+" "+user_cov);
								bw7.newLine();
								bw7.flush();
							}
						}
						else{
							double[] output = ce.ceEval(k[i], 1, em_type);
							mae= output[0];//mae
							precision= output[1]; //hit ratio
							recall= output[2]; //coverage -> recall
							missed= output[3]; //missed
							
							for(int si=0;si<nrec_size.length;si++){
								double[] output2 = ce.ceEval2(k[i], 4, em_type, nrec_size[si]);
								diversity= output2[0]; //diversity
								item_cov= output2[1]; 
								user_cov= output2[2]; 
								
								bw4.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+ nrec_size[si]+" "+diversity);
								bw4.newLine();
								bw4.flush();
								
								bw6.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+ nrec_size[si]+" "+item_cov);
								bw6.newLine();
								bw6.flush();
								
								bw7.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+ nrec_size[si]+" "+user_cov);
								bw7.newLine();
								bw7.flush();
							}
						}
						
						bw.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+mae);
						bw.newLine();
						bw.flush();
						
						bw2.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+precision);
						bw2.newLine();
						bw2.flush();
						
						bw3.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+recall);
						bw3.newLine();
						bw3.flush();
						
						bw5.write(""+sparse_month[di]+" "+k[i]+" "+(f+1)+" "+ missed);
						bw5.newLine();
						bw5.flush();
					}
				}
			}
			
			bw.close();
			bw2.close();
			bw3.close();
			bw4.close();
			bw5.close();
			bw6.close();
			bw7.close();
		}catch(Exception e){
			e.printStackTrace();
			System.err.print(e);
		}
	}
	
	/**
	 * This is the main method, where it all begins
	 * @param args command line argument (unused)
	 * 
	 */
	public static void main(String[] args){
		String dir = System.getProperty("user.dir");
		String mlens_100k = dir+"/dataset/MovieLens/raw_data/ml-100k";           
		String wdir_100k= dir+"/dataset/MovieLens/100k_data"; //working directory
		String nflx = dir+"/dataset/Netflix/raw_data"; //~/git/PersonalExpert/dataset/NetFlix/raw_data
		String wdir_nflx = dir+"/dataset/Netflix/sampled";
		
		int nuser= 0;
		int nitem= 0;
		int ngenre= 0;
		String raw="", wdir="";
		
		MyFavoriteExperts mfe= null;
		
		int exp_type= 1; //1:nflx, 2:movielens
		
		if(exp_type == 1){
			raw= nflx;
			wdir= wdir_nflx;
			
			//100,480,507 ratings that 480,189 users gave to 17,770 movies.
			nuser= 458376; //480189;
			nitem= 17770;
			ngenre= 0;
			
			mfe= new MyFavoriteExperts(nuser, nitem, ngenre);
			//mfe.prepareDataset(raw, wdir, 5, 1); //sample the original data set to the tenth.
		}
		else if(exp_type == 2){
			raw= mlens_100k;
			wdir= wdir_100k;
			
			try{
				FileInputStream fis = new FileInputStream(raw+"/u.info");
				BufferedReader br = new BufferedReader(new InputStreamReader(fis));
				
				nuser = Integer.parseInt(br.readLine().split("[ ]")[0]);
				nitem = Integer.parseInt(br.readLine().split("[ ]")[0]);
				ngenre = 19;
				
				br.close();
			}
			catch(Exception e){
				e.printStackTrace();
				System.exit(0);
			}
			
			mfe= new MyFavoriteExperts(nuser, nitem, ngenre);
			mfe.prepareDataset(raw, wdir, 5, 2); //MovieLens 
		}
			
		int[] k= new int[]{20};//{20,30,40,50,60};
		int[] j = new int[]{10}; //1: normal SVM
		int nfold= 1;
		int[] sparse_month= new int[]{0};
		int[] nrec_size= new int[]{20};
		
		mfe.kNearestNeighbor(wdir, k, nfold, sparse_month, nrec_size);
//		mfe.commonExperts(wdir, k, nfold, sparse_month, 1, nrec_size);
//		mfe.commonExperts(wdir, k, nfold, sparse_month, 2, nrec_size);
//		mfe.commonExperts(wdir, k, nfold, sparse_month, 3, nrec_size);
//		mfe.commonExperts(wdir, k, nfold, sparse_month, 4, nrec_size);
//		mfe.commonExperts(wdir, k, nfold, sparse_month, 5, nrec_size);
//		
//		mfe.optimalExperts(wdir, k, nfold, sparse_month, nrec_size);
//		mfe.getSVMdata(wdir, k, j, nfold, sparse_month);
		//-> generate SVM models
//		ParserSVM.parseResult(wdir+"/svm/output", j, k, nfold, sparse_month);
//		mfe.personalExperts(wdir, k, j, nfold, sparse_month, nrec_size);
		
//		k= new int[]{50};
//		sparse_month= new int[]{0,1,2};
//		j = new int[]{10}; //1: normal SVM
//		nrec_size= new int[]{20};
//		mfe.optimalExperts(wdir, k, nfold, sparse_month);
//		mfe.getSVMdata(wdir, k, j, nfold, sparse_month);
//		mfe.personalExperts(wdir, k, j, nfold, sparse_month, nrec_size);
	
		/**
		//jaccard index
		double simce_ji= Tools.jaccardIndex("simce_neighborhoods.txt"); 
		System.out.println("SIMCE Jaccard Index: "+simce_ji);
		double pe_ji= Tools.jaccardIndex("pe_neighborhoods.txt"); 
		System.out.println("PE Jaccard Index: "+pe_ji);
		double ce_ji= Tools.jaccardIndex("ce_neighborhoods.txt"); 
		System.out.println("CE Jaccard Index: "+ce_ji);
		double knn_ji= Tools.jaccardIndex("knn_neighborhoods.txt"); 
		System.out.println("KNN Jaccard Index: "+knn_ji);
		double rs_ji= Tools.jaccardIndex("rs_neighbourhood.txt"); 
		System.out.println("RS Jaccard Index: "+rs_ji);
		*/
		
//		Tools.expert_stats("C:/Users/user/workspace//MyFavoriteExperts/dataset/MovieLens/100k_data/optimal_k50_f1_s0_exp.txt", 50);
		
		

	}
}