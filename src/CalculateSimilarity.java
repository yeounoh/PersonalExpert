
public class CalculateSimilarity {
	//user1, user1.avg, user2, user2.avg
	
	void calculate(String path){
		SMFactory smf = new SMFactory();
		
	}
	double getAverage(double [] user){
		double avr=0.0;
		int cnt = 0;
		for (double rating : user) {
			if(rating != 0){
				avr += rating;
				cnt++;
			}
		}
		avr = avr / cnt;
		return avr;
	}
}
