import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;


public class Tools {

	/**
	 * return 3*standard deviations of the input vector
	 * 
	 * @param x
	 * @return
	 */
	public static double outlier_threshold(double[] x){
		double cnt = 0.0;
		double mean = 0.0;
		double std = 0.0;
		double[] x_norm = x.clone(); //assumption: all components are positive
		double thresh = 0.0;
		
		for(int i=0;i<x_norm.length;i++){
			if(x_norm[i] > 0.0){
				mean = mean + x_norm[i];
				cnt++;
			}
		}
		if(cnt == 0)
			mean = 0.0;
		else 
			mean = mean / cnt;
		
		for(int i=0;i<x_norm.length;i++){
			if(x_norm[i] > 0.0){
				double tmp = x_norm[i] - mean;
				tmp = tmp * tmp;
				std = std + tmp;
			}
		}
		
		if(cnt == 0)
			std = 0.0;
		else{
			std = std/cnt;
			std = Math.sqrt(std);
		}
		
		thresh = 3 * std;
		
		return thresh;
	}
	
	/**
	 * return magnitude of an input vector
	 * 
	 * @param x
	 * @return
	 */
	public static double vector_magnitude(double[] x){
		double mag= 0.0;
		
		for(int i=0;i<x.length;i++){
			mag = mag + x[i]*x[i];
		}
		mag = Math.sqrt(mag);
		
		return mag;
	}
	
	public static void undersampling(String src_file){
		try{
			FileInputStream fis= new FileInputStream(src_file);
			BufferedReader br= new BufferedReader(new InputStreamReader(fis));	
			FileOutputStream fos= new FileOutputStream("undsmpl_"+src_file);
			BufferedWriter bw= new BufferedWriter(new OutputStreamWriter(fos));
			String[] tokens = null;
			String line= "";
			
			while((line = br.readLine()) != null){
				//take 1 sample
				bw.write(line);
				bw.newLine();
				bw.flush();

				//skip 9 samples
				int term= 0;
				for(int i=0;i<9;i++){
					if((line = br.readLine()) == null)
						term= 1;
				}
				if(term == 1)
					break;
			}
			br.close();
			bw.close();
		}
		catch (Exception e){
			System.err.println("Error@undersampling()");
		}
	}

	public static int[] sortTopK(double[] sim, int k){
		int[] topk= new int[k];
		int[] idx= new int[sim.length]; //nuser
		double[] tmp_sim= sim.clone();
		
		for(int i=0;i<idx.length;i++)
			idx[i]= i;
		
		for(int i=0;i<tmp_sim.length-1;i++){
			for(int j=0;j<tmp_sim.length-1;j++){
				if(tmp_sim[j]>tmp_sim[j+1]){
					double temp= tmp_sim[j];
					tmp_sim[j]= tmp_sim[j+1];
					tmp_sim[j+1]= temp;
					
					int uid= idx[j];
					idx[j]= idx[j+1];
					idx[j+1]= uid;
				}
			}
		}
		
		for(int i=0;i<k;i++){
			topk[i]= idx[idx.length-1-i];
			//System.out.println("sortTopK(): "+tmp_sim[sim.length-1-i] + " "+sim[topk[i]]);
		}
		
		return topk;
	}
	
	public static double jaccardIndex(String file){
		int[][] neighbours = new int[3][50]; //0~49, 50~99, 100~149
		int cnt = 0;
		String line;
		
		try{
			FileInputStream fis= new FileInputStream(file);
			BufferedReader br= new BufferedReader(new InputStreamReader(fis));
			
			while((line = br.readLine()) != null){
				neighbours[cnt/50][cnt%50]= Integer.parseInt(line);
				cnt++;
			}
			br.close();
			
			//jaccard index
			int common_12 = 0;
			int common_13 = 0;
			int common_23 = 0;
			
			for(int i=0;i<50;i++){
				for(int j=0;j<50;j++){
					if(neighbours[0][i] == neighbours[1][j])
						common_12++;
					if(neighbours[0][i] == neighbours[2][j])
						common_13++;
					if(neighbours[1][i] == neighbours[2][j])
						common_23++;
				}
			}
			
			double jaccard_12 = (double) common_12/(double) (100-common_12);
			double jaccard_13 = (double) common_13/(double) (100-common_13);
			double jaccard_23 = (double) common_23/(double) (100-common_23);
			
			return (jaccard_12 + jaccard_13 + jaccard_23)/3;
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		return 0.0;
	}
	
	public static void expert_stats(String expert_list, int k){
		int nuser= 963;
		int[] stats= new int[nuser];
		
		try{
			FileInputStream fis= null;
			BufferedReader br= null;
			FileOutputStream fos= null;
			BufferedWriter bw= null;
			String line;
			String[] tokens, tokens2;
			
			fis= new FileInputStream(expert_list);
			br= new BufferedReader(new InputStreamReader(fis));
			
			fos= new FileOutputStream("expert_stat.txt");
			bw= new BufferedWriter(new OutputStreamWriter(fos));
			
			while((line=br.readLine())!=null){
				tokens= line.split("[,]");
				int suid= Integer.parseInt(tokens[0]);
				
				for(int j=0;j<k;j++){
					int esuid= Integer.parseInt(tokens[j+1]);
					
					stats[esuid-1]= stats[esuid-1]+1;
				}
			}
			
			for(int i=0;i<stats.length;i++){
				bw.write("" + (i+1)+ ":"+stats[i]);
				bw.newLine();
				bw.flush();
			}
			bw.close();
			br.close();
		}catch (Exception e){
			System.err.println("Error@makeTestSet(): " + e.getMessage());
		}
	}
}
