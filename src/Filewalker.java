import java.io.File;
import java.io.IOException;
import java.text.ParseException;

public class Filewalker {
	public void walk( String path ) throws IOException, ParseException {
		ConvertRating ct = new ConvertRating();
        File root = new File( path );
        File[] list = root.listFiles();

        if (list == null) return;

        for ( File f : list ) {
            if ( f.isDirectory() ) {
                walk( f.getAbsolutePath() );
            }
            else {
                ct.converting(f.getAbsoluteFile().toString());
            }
        }
    }

    
}
