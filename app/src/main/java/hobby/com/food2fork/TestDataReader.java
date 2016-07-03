package hobby.com.food2fork;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * ONLY USED FOR TESTING - Also used as a workaround in case the server is down. A text file containing
 * cached query results can be placed in /sdcarc/file.txt.
 *
 * For sample use, please see the WORKAROUND tag in SearchFetcher.java and RecipeFetcher.java
 *
 * @author  Nima Poulad
 * @version 1.0
 */

public class TestDataReader {
    private String filename = null;
    public TestDataReader(String filename) {
        this.filename = filename;
    }

    public String read ()
    {
        if(filename == null) {
            return null;
        }

        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(sdcard,filename);

        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();

            return text.toString();
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
