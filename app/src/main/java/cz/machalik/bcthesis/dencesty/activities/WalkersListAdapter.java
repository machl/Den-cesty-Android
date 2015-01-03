package cz.machalik.bcthesis.dencesty.activities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.machalik.bcthesis.dencesty.model.RaceModel;

/**
 * Lukáš Machalík
 */
public class WalkersListAdapter extends BaseAdapter {

    private Context context;

    private JSONArray walkersAhead;
    private JSONArray walkersBehind;
    private JSONObject me;

    public WalkersListAdapter(Context context) {
        this.context = context;
        this.walkersAhead = RaceModel.getInstance().getRaceInfoWalkersAhead();
        this.walkersBehind = RaceModel.getInstance().getRaceInfoWalkersBehind();
        this.me = new JSONObject();
        try {
            this.me.put("name", String.format("Já (%s)", RaceModel.getInstance().getWalkerUsername()));
            this.me.put("distance", RaceModel.getInstance().getRaceInfoDistance());
            this.me.put("speed", RaceModel.getInstance().getRaceInfoAvgSpeed());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getCount() {
        return walkersAhead.length() + 1 + walkersBehind.length();
    }

    @Override
    public JSONObject getItem(int position) {
        if (position < walkersAhead.length()) { // Ahead
            return walkersAhead.optJSONObject(position);
        } else if (position > walkersAhead.length()) { // Behind
            return walkersBehind.optJSONObject(position - walkersAhead.length() - 1);
        } else { // Me
            return me;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(android.R.layout.simple_list_item_2, null);
        }

        TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
        TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);

        JSONObject item = getItem(position);
        text1.setText(item.optString("name"));
        text2.setText(String.format("%d m, %d km/h", item.optInt("distance"), item.optInt("speed")));

        return convertView;
    }
}
