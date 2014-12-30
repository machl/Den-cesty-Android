package cz.machalik.bcthesis.dencesty;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/**
 * Logging into a file on a sdcard.
 *
 * Lukáš Machalík
 */
public class FileLogger {

    public static final String LOG_FILE = "/den-cesty-log.txt";

    private static final File logFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + LOG_FILE);

    private static final String preTag = " [";
    private static final String postTag = "] ";

    private FileLogger() {}

    public static void log(String tag, String text) {
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                Log.e("FileLogger", "Creating new log file failed");
                e.printStackTrace();
            }
        }
        BufferedWriter buf = null;
        try {
            //BufferedWriter for performance, true to set append to file flag
            buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(new Date().toString());
            buf.append(preTag);
            buf.append(tag);
            buf.append(postTag);
            buf.append(text);
            buf.newLine();
        } catch (IOException e) {
            Log.e("FileLogger", "Appending to log file failed");
            e.printStackTrace();
        } finally {
            if (buf != null)
                try {
                    buf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }
}
