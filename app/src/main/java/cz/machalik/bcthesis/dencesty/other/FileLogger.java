package cz.machalik.bcthesis.dencesty.other;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/**
 * Logging into a file on a sdcard.
 * Inspired by:
 * http://stackoverflow.com/questions/3551821/android-write-to-sd-card-folder
 *
 * Lukáš Machalík
 */
public class FileLogger {

    public static final String LOG_FILE = "/den-cesty-log.txt";

    private static final File logFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + LOG_FILE);

    private static final String preTag = " [";
    private static final String postTag = "] ";

    private static boolean ready = false;

    private FileLogger() {}

    public static void log(String tag, String text) {

        // Availability checks:
        if (!ready) {
            boolean mExternalStorageAvailable;
            boolean mExternalStorageWritable;
            String state = Environment.getExternalStorageState();

            if (Environment.MEDIA_MOUNTED.equals(state)) {
                // We can read and write the media
                mExternalStorageAvailable = mExternalStorageWritable = true;
            } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                // We can only read the media
                mExternalStorageAvailable = true;
                mExternalStorageWritable = false;
                Log.e("FileLogger", "External storage is not writable");
            } else {
                // Something else is wrong. It may be one of many other states, but all we need
                //  to know is we can neither read nor write
                mExternalStorageAvailable = mExternalStorageWritable = false;
                Log.e("FileLogger", "External storage not available");
            }

            if (mExternalStorageAvailable && mExternalStorageWritable) {
                ready = true;
            } else {
                ready = false;
                return;
            }
        }

        // File write:
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                Log.e("FileLogger", "Creating new log file failed");
                e.printStackTrace();
                ready = false;
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
            ready = false;
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
