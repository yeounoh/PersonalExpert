import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * 
 * @author yeoun
 *
 */
public class Similarity {
	
	public double pearsonCorr(double[] x, double avgx, double[] y, double avgy) {
        if (x.length != y.length) {
            throw new RuntimeException("Both users should have the same number of attributes");
        }
        
        double sumTop = 0.0;
        double sumOne = 0.0;
        double sumTwo = 0.0;
        for (int i = 0; i < x.length; i++) {
        	if(x[i]!=0 && y[i]!=0){
        		sumTop += (x[i]-avgx)*(y[i]-avgy);
                sumOne += (x[i]-avgx)*(x[i]-avgx);
                sumTwo += (y[i]-avgy)*(y[i]-avgy);
        	}
        }
        
        double pearsonCorr = sumTop / (Math.sqrt(sumOne) * Math.sqrt(sumTwo));
        
        if (new Double(pearsonCorr).isNaN())
            pearsonCorr = 0;
        
        return pearsonCorr;
    }
	
	//significance weighted (n/50)
	public double wPearsonCorr(double[] x, double avgx, double[] y, double avgy){
		 if (x.length != y.length) {
	            throw new RuntimeException("Both users should have the same number of attributes");
	        }
	        
	        double sumTop = 0.0;
	        double sumOne = 0.0;
	        double sumTwo = 0.0;
	        double coRated = 0.0;
	        
	        for (int i = 0; i < x.length; i++) {
	        	if(x[i]!=0 && y[i]!=0){ //rated by both users
	        		sumTop += (x[i]-avgx)*(y[i]-avgy);
	                sumOne += (x[i]-avgx)*(x[i]-avgx);
	                sumTwo += (y[i]-avgy)*(y[i]-avgy);
	                coRated++;
	        	}
	        }
	        
	        double pearsonCorr = sumTop / (Math.sqrt(sumOne) * Math.sqrt(sumTwo));
	        
	        if (new Double(pearsonCorr).isNaN())
	            pearsonCorr = 0;
	        
	        if(coRated < 50)
	        	pearsonCorr = pearsonCorr * (coRated/50);
	        
	        return pearsonCorr;
	}
}
