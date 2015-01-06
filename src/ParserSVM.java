import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class ParserSVM {

	public static void parseResult(String dir_result,int[] j, int[] k,int nfold, int[] sparse_month){
		try{
			FileInputStream fis= null;
			BufferedReader br= null;
			FileOutputStream fos= null, fos2= null;
			BufferedWriter bw= null, bw2= null;
			String line;
			String[] tokens, tokens2;
			
			fos= new FileOutputStream(dir_result+"/result_all.txt");
			bw= new BufferedWriter(new OutputStreamWriter(fos));
			
			bw.write("| s | j | k | acc | pre | rec |");
			bw.newLine();
			bw.flush();
			
			for(int di=0;di<sparse_month.length;di++){
				for(int ji=0;ji<j.length;ji++){
					for(int ki=0;ki<k.length;ki++){
						
						double TP=0, TN= 0, FP= 0, FN= 0;
						
						for(int fi=0;fi<nfold;fi++){
							
							double incorrect= 0.0;
							double total= 0.0;
							double correct= 0.0;
							
							fis= new FileInputStream(dir_result+"/result_j"+j[ji]+"_k"+k[ki]+"_f"+(fi+1)+"_s"+sparse_month[di]+".txt");
							br= new BufferedReader(new InputStreamReader(fis));			
							
							while((line=br.readLine())!=null){
								tokens= line.split("[()/% ]");
								
								if(tokens[0].equals("Accuracy")){
									incorrect= Integer.parseInt(tokens[9]); 
									total= Integer.parseInt(tokens[11]);
									correct= total - incorrect;
								}
								
								if(tokens[0].equals("Precision")){
									
									double prec= 0.0, rec= 0.0;
									if(tokens[5].equals("-1.#J") || tokens[5].equals("0.0")){ //precision
									}
									else{
										prec= Double.parseDouble(tokens[5])/100;
									}
									if(tokens[7].equals("-1.#J") || tokens[7].equals("0.0")){ //recall
									}
									else{
										rec= Double.parseDouble(tokens[7])/100;
									}
									if(prec != 0.0 && rec != 0.0){ 
										TP += incorrect/(1/prec + 1/rec - 2);
										FP += TP/prec - TP;
										FN += TP/rec - TP;
										TN += correct - TP; 
									}
									else if(prec == 0.0){
										TP += 0.0;
										FP += 0.0;
										FN += incorrect;
										TN += correct;
									}
									else if(rec == 0.0){
										TP += 0.0;
										FP += incorrect;		
										FN += 0.0;
										TN += correct;
									}
								}
							}
						}
						
						double acc= (TP+TN)/(TP+FN+TN+FP);
						double prec= TP/(TP+FP);
						double rec= TP/(TP+FN);
						
						bw.write(sparse_month[di]+" "+j[ji]+" "+k[ki]+" "+acc+" "+prec+" "+rec);
						bw.newLine();
						bw.flush();
					}
				}
			}
			br.close();
			bw.close();
		}catch (Exception e){
			System.err.println("Error@makeTestSet(): " + e.getMessage());
			e.printStackTrace();
		}
	}
}
