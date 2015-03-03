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

import cz.machalik.bcthesis.dencesty.R;
import cz.machalik.bcthesis.dencesty.model.User;
import cz.machalik.bcthesis.dencesty.model.Walker;

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
        this.walkersAhead = Walker.getWalkersAhead();
        this.walkersBehind = Walker.getWalkersBehind();
        this.me = new JSONObject();
        try {
            this.me.put("name", String.format("Já (%s)", User.getWalkerUsername()));
            this.me.put("distance", Walker.getWalkerDistance());
            this.me.put("speed", Walker.getWalkerAvgSpeed());
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
            convertView = inflater.inflate(R.layout.right_detail_list_item, null);
        }

        TextView text1 = (TextView) convertView.findViewById(R.id.textLabel);
        TextView text2 = (TextView) convertView.findViewById(R.id.detailTextLabel);

        JSONObject item = getItem(position);
        text1.setText(item.optString("name"));
        text2.setText(String.format("%d m, %.2f km/h", item.optInt("distance"), item.optDouble("speed")));

        if (position < walkersAhead.length()) { // Ahead
            convertView.setBackgroundColor(context.getResources().getColor(R.color.listitem_ahead));
        } else if (position > walkersAhead.length()) { // Behind
            convertView.setBackgroundColor(context.getResources().getColor(R.color.listitem_behind));
        } else { // Me
            convertView.setBackgroundColor(context.getResources().getColor(R.color.listitem_me));
        }

        return convertView;
    }
}
