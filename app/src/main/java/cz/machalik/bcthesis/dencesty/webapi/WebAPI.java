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

import cz.machalik.bcthesis.dencesty.model.User;
import cz.machalik.bcthesis.dencesty.other.FileLogger;

/**
 * Lukáš Machalík
 */
public class WebAPI {
    protected static final String TAG = "WebAPI";

    //public static final String URL_WEBSERVER = "http://46.13.199.141:3000";
    public static final String URL_WEBSERVER = "http://machalik.kolej.mff.cuni.cz:3000";
    //public static final String URL_WEBSERVER = "https://www.dencesty.cz"; // must be with 'www.' !

    public static final String URL_LOGINHANDLER = URL_WEBSERVER + "/api/login.json";
    public static final String URL_EVENTHANDLER = URL_WEBSERVER + "/api/push_events.json";
    public static final String URL_WALKERSLIST = URL_WEBSERVER + "/api/scoreboard/%d.json?walker_id=%d";
    public static final String URL_RACESLIST = URL_WEBSERVER + "/api/races.json";
    public static final String URL_RACEDATA = URL_WEBSERVER + "/api/race_data/%d.json";

    public static final DateFormat DATE_FORMAT_UPLOAD = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
    public static final DateFormat DATE_FORMAT_DOWNLOAD = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public static JSONObject synchronousLoginHandlerRequest(String email, String password) { // TODO: ošetřit nedostupné internetové připojení
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
                FileLogger.log(TAG, message);
                // TODO: Create error event
            }

        } catch (MalformedURLException e) {
            String message = "Login handler: MalformedURLException: " + e.getLocalizedMessage();
            //Log.e(TAG, message);
            FileLogger.log(TAG, message);
            e.printStackTrace();
        } catch (IOException e) {
            String message = "Login handler: IOException: " + e.getLocalizedMessage();
            //Log.e(TAG, message);
            FileLogger.log(TAG, message);
            //e.printStackTrace();
        } catch (JSONException e) {
            String message = "Login handler: JSONException: " + e.getLocalizedMessage();
            //Log.e(TAG, message);
            FileLogger.log(TAG, message);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return jsonResponse;
    }

    public static JSONObject synchronousEventHandlerRequest(JSONArray eventsAsJson) {
        if (!User.isLogged()) {
            //Log.e(TAG, "User is not logged to do synchronousEventHandlerRequest!");
            return null;
        }

        JSONObject jsonResponse = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(String.format(URL_EVENTHANDLER));
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
                FileLogger.log(TAG, message);
                // TODO: Create error event
            }

        } catch (MalformedURLException e) {
            String message = "Event handler: MalformedURLException: " + e.getLocalizedMessage();
            //Log.e(TAG, message);
            FileLogger.log(TAG, message);
            e.printStackTrace();
        } catch (IOException e) {
            String message = "Event handler: IOException: " + e.getLocalizedMessage();
            //Log.e(TAG, message);
            FileLogger.log(TAG, message);
            //e.printStackTrace();
        } catch (JSONException e) {
            String message = "Event handler: JSONException: " + e.getLocalizedMessage();
            //Log.e(TAG, message);
            FileLogger.log(TAG, message);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return jsonResponse;
    }

    public static JSONObject synchronousWalkersListRequest(int raceId, int walkerId) {
        if (!User.isLogged()) {
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
                FileLogger.log(TAG, message);
                // TODO: Create error event
            }

        } catch (MalformedURLException e) {
            String message = "Walkers list update: MalformedURLException: " + e.getLocalizedMessage();
            //Log.e(TAG, message);
            FileLogger.log(TAG, message);
            e.printStackTrace();
        } catch (IOException e) {
            String message = "Walkers list update: IOException: " + e.getLocalizedMessage();
            //Log.e(TAG, message);
            FileLogger.log(TAG, message);
            //e.printStackTrace();
        } catch (JSONException e) {
            String message = "Walkers list update: JSONException: " + e.getLocalizedMessage();
            //Log.e(TAG, message);
            FileLogger.log(TAG, message);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return jsonResponse;
    }

    public static JSONArray synchronousRacesListUpdateRequest() {
        if (!User.isLogged()) {
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
                Log.e(TAG, message);
                FileLogger.log(TAG, message);
                // TODO: Create error event
            }

        } catch (MalformedURLException e) {
            String message = "Races list update: MalformedURLException: " + e.getLocalizedMessage();
            Log.e(TAG, message);
            FileLogger.log(TAG, message);
            e.printStackTrace();
        } catch (IOException e) {
            String message = "Races list update: IOException: " + e.getLocalizedMessage();
            Log.e(TAG, message);
            FileLogger.log(TAG, message);
            e.printStackTrace();
        } catch (JSONException e) {
            String message = "Races list update: JSONException: " + e.getLocalizedMessage();
            Log.e(TAG, message);
            FileLogger.log(TAG, message);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return jsonResponse;
    }

    public static JSONObject synchronousRaceDataRequest(int raceId) {
        if (!User.isLogged()) {
            Log.e(TAG, "User is not logged to do synchronousRaceDataRequest!");
            return null;
        }

        JSONObject jsonResponse = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(String.format(URL_RACEDATA, raceId));
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
                FileLogger.log(TAG, message);
                // TODO: Create error event
            }

        } catch (MalformedURLException e) {
            String message = "Race data request: MalformedURLException: " + e.getLocalizedMessage();
            Log.e(TAG, message);
            FileLogger.log(TAG, message);
            e.printStackTrace();
        } catch (IOException e) {
            String message = "Race data request: IOException: " + e.getLocalizedMessage();
            Log.e(TAG, message);
            FileLogger.log(TAG, message);
            e.printStackTrace();
        } catch (JSONException e) {
            String message = "Race data request: JSONException: " + e.getLocalizedMessage();
            Log.e(TAG, message);
            FileLogger.log(TAG, message);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return jsonResponse;
    }

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
