package cz.machalik.bcthesis.dencesty.webapi;

import android.os.BatteryManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import cz.machalik.bcthesis.dencesty.model.User;

/**
 * Interface for communicating with a web server.
 *
 * @author Lukáš Machalík
 */
public class WebAPI {
    protected static final String TAG = "WebAPI";

    /**
     * Web server address.
     */
    //public static final String URL_WEBSERVER = "http://46.13.199.141:3000";
    //public static final String URL_WEBSERVER = "http://machalik.kolej.mff.cuni.cz:3000";
    public static final String URL_WEBSERVER = "https://www.dencesty.cz"; // must be with 'www.' !

    // Specific handler addresses:
    private static final String URL_LOGINHANDLER = URL_WEBSERVER + "/api/login.json";
    private static final String URL_EVENTHANDLER = URL_WEBSERVER + "/api/push_events.json";
    private static final String URL_RACESLIST = URL_WEBSERVER + "/api/races.json";
    private static final String URL_RACEDATA = URL_WEBSERVER + "/api/race_data/%d.json?walker_id=%d";
    private static final String URL_WALKERSLIST = URL_WEBSERVER + "/api/scoreboard/%d.json?walker_id=%d";

    /**
     * DateFormat for parsing outgoing Date to String.
     */
    public static final DateFormat DATE_FORMAT_UPLOAD = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    /**
     * DateFormat for parsing incoming String to Date.
     */
    public static final DateFormat DATE_FORMAT_DOWNLOAD = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
     * Server is responding in UTC timezone.
     */
    static {
        DATE_FORMAT_DOWNLOAD.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Synchronous login request with given credentials to a server.
     * @param email user's e-mail
     * @param password user's password
     * @return response data (user's name, surname, id, ...)
     */
    public static JSONObject synchronousLoginHandlerRequest(String email, String password) {
        JSONObject jsonResponse = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(URL_LOGINHANDLER);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setUseCaches(false);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setConnectTimeout(10 * 1000); // in millis
            urlConnection.setReadTimeout(10 * 1000); // in millis
            urlConnection.connect();

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream(), "US-ASCII"));
            bw.write(String.format("&email=%s&password=%s", email, password));
            bw.flush();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode == 200) {

                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line+"\n");
                }
                br.close();
                String jsonString = sb.toString();

                jsonResponse = new JSONObject(jsonString);

            } else {
                String message = "Login handler: Wrong response code " + responseCode + ": " + urlConnection.getResponseMessage();
                //Log.e(TAG, message);
                // TODO: Create error event
            }

        } catch (MalformedURLException e) {
            String message = "Login handler: MalformedURLException: " + e.getLocalizedMessage();
            //Log.e(TAG, message);
            e.printStackTrace();
        } catch (IOException e) {
            String message = "Login handler: IOException: " + e.getLocalizedMessage();
            //Log.e(TAG, message);
            //e.printStackTrace();
        } catch (JSONException e) {
            String message = "Login handler: JSONException: " + e.getLocalizedMessage();
            //Log.e(TAG, message);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return jsonResponse;
    }

    /**
     * Synchronous Events upload request to a server.
     * @param eventsAsJson Events as JSON array.
     * @return response data (saved Event IDs)
     */
    public static JSONObject synchronousEventHandlerRequest(JSONArray eventsAsJson) {
        if (!User.get().isLogged()) {
            //Log.e(TAG, "User is not logged to do synchronousEventHandlerRequest!");
            return null;
        }

        JSONObject jsonResponse = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(URL_EVENTHANDLER);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setUseCaches(false);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setConnectTimeout(30 * 1000); // in millis
            urlConnection.setReadTimeout(30 * 1000); // in millis
            urlConnection.connect();

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream(), "US-ASCII"));
            bw.write(eventsAsJson.toString());
            bw.flush();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode == 200) {

                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line+"\n");
                }
                br.close();
                String jsonString = sb.toString();

                jsonResponse = new JSONObject(jsonString);

            } else {
                String message = "Event handler: Wrong response code " + responseCode + ": " + urlConnection.getResponseMessage();
                //Log.e(TAG, message);
                // TODO: Create error event
            }

        } catch (MalformedURLException e) {
            String message = "Event handler: MalformedURLException: " + e.getLocalizedMessage();
            //Log.e(TAG, message);
            e.printStackTrace();
        } catch (IOException e) {
            String message = "Event handler: IOException: " + e.getLocalizedMessage();
            //Log.e(TAG, message);
            //e.printStackTrace();
        } catch (JSONException e) {
            String message = "Event handler: JSONException: " + e.getLocalizedMessage();
            //Log.e(TAG, message);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return jsonResponse;
    }

    /**
     * Synchronous scoreboard download request to a server.
     * @param raceId current Race ID
     * @param walkerId logged user's ID
     * @return response data (walkers ahead, walkers behind, ...)
     */
    public static JSONObject synchronousWalkersListRequest(int raceId, int walkerId) {
        if (!User.get().isLogged()) {
            //Log.e(TAG, "User is not logged to do synchronousWalkersListRequest!");
            return null;
        }

        JSONObject jsonResponse = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(String.format(URL_WALKERSLIST, raceId, walkerId));
            urlConnection = (HttpURLConnection) url.openConnection();
            //urlConnection.setDoInput(false);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setUseCaches(false);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setConnectTimeout(10 * 1000); // in millis
            urlConnection.setReadTimeout(10 * 1000); // in millis
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode == 200) {

                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line+"\n");
                }
                br.close();
                String jsonString = sb.toString();

                jsonResponse = new JSONObject(jsonString);

            } else {
                String message = "Walkers list update: Wrong response code " + responseCode + ": " + urlConnection.getResponseMessage();
                //Log.e(TAG, message);
                // TODO: Create error event
            }

        } catch (MalformedURLException e) {
            String message = "Walkers list update: MalformedURLException: " + e.getLocalizedMessage();
            //Log.e(TAG, message);
            e.printStackTrace();
        } catch (IOException e) {
            String message = "Walkers list update: IOException: " + e.getLocalizedMessage();
            //Log.e(TAG, message);
            //e.printStackTrace();
        } catch (JSONException e) {
            String message = "Walkers list update: JSONException: " + e.getLocalizedMessage();
            //Log.e(TAG, message);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return jsonResponse;
    }

    /**
     * Synchronous races list download request to a server.
     * @return response data (list of all available races)
     */
    public static JSONArray synchronousRacesListUpdateRequest() {
        if (!User.get().isLogged()) {
            Log.e(TAG, "User is not logged to do synchronousRacesListUpdateRequest!");
            return null;
        }

        JSONArray jsonResponse = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(URL_RACESLIST);
            urlConnection = (HttpURLConnection) url.openConnection();
            //urlConnection.setDoInput(false);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setUseCaches(false);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setConnectTimeout(10 * 1000); // in millis
            urlConnection.setReadTimeout(10 * 1000); // in millis
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode == 200) {

                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line+"\n");
                }
                br.close();
                String jsonString = sb.toString();

                jsonResponse = new JSONArray(jsonString);

            } else {
                String message = "Races list update: Wrong response code " + responseCode + ": " + urlConnection.getResponseMessage();
                Log.e(TAG, message);;
                // TODO: Create error event
            }

        } catch (MalformedURLException e) {
            String message = "Races list update: MalformedURLException: " + e.getLocalizedMessage();
            Log.e(TAG, message);
            e.printStackTrace();
        } catch (IOException e) {
            String message = "Races list update: IOException: " + e.getLocalizedMessage();
            Log.e(TAG, message);
            e.printStackTrace();
        } catch (JSONException e) {
            String message = "Races list update: JSONException: " + e.getLocalizedMessage();
            Log.e(TAG, message);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return jsonResponse;
    }

    /**
     * Synchronous race data request to a server.
     * @param raceId Race ID
     * @param walkerId logged user's ID
     * @return response data (user's progress in race, checkpoints, race start time, ...)
     */
    public static JSONObject synchronousRaceDataRequest(int raceId, int walkerId) {
        if (!User.get().isLogged()) {
            Log.e(TAG, "User is not logged to do synchronousRaceDataRequest!");
            return null;
        }

        JSONObject jsonResponse = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(String.format(URL_RACEDATA, raceId, walkerId));
            urlConnection = (HttpURLConnection) url.openConnection();
            //urlConnection.setDoInput(false);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setUseCaches(false);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setConnectTimeout(10 * 1000); // in millis
            urlConnection.setReadTimeout(10 * 1000); // in millis
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode == 200) {

                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line+"\n");
                }
                br.close();
                String jsonString = sb.toString();

                jsonResponse = new JSONObject(jsonString);

            } else {
                String message = "Race data request: Wrong response code " + responseCode + ": " + urlConnection.getResponseMessage();
                Log.e(TAG, message);
                // TODO: Create error event
            }

        } catch (MalformedURLException e) {
            String message = "Race data request: MalformedURLException: " + e.getLocalizedMessage();
            Log.e(TAG, message);
            e.printStackTrace();
        } catch (IOException e) {
            String message = "Race data request: IOException: " + e.getLocalizedMessage();
            Log.e(TAG, message);
            e.printStackTrace();
        } catch (JSONException e) {
            String message = "Race data request: JSONException: " + e.getLocalizedMessage();
            Log.e(TAG, message);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return jsonResponse;
    }

    /**
     * Converts battery status as is from system to battery status representation on a server.
     * Server supports battery status representation same as on iOS.
     * @param status battery status
     * @param plugged plugged status
     * @return battery status in format for a server
     */
    public static int convertBatteryStatus (int status, int plugged) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_UNKNOWN: return 0;
            case BatteryManager.BATTERY_STATUS_CHARGING: return 2;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING: return 1;
            case BatteryManager.BATTERY_STATUS_FULL: return 3;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                if (plugged == BatteryManager.BATTERY_PLUGGED_USB) {
                    return 4;
                } else {
                    return 1;
                }
            default: return 0;
        }
    }
}
