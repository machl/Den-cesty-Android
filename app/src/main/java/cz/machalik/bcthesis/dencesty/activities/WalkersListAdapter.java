package cz.machalik.bcthesis.dencesty.activities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import cz.machalik.bcthesis.dencesty.R;
import cz.machalik.bcthesis.dencesty.model.Walker;

/**
 * Lukáš Machalík
 */
public class WalkersListAdapter extends BaseAdapter {

    private final Context context;

    private final Walker[] walkersAhead;
    private final Walker[] walkersBehind;
    private final Walker me;

    public WalkersListAdapter(Context context) {
        this.context = context;
        this.walkersAhead = Walker.getWalkersAhead();
        this.walkersBehind = Walker.getWalkersBehind();
        this.me = Walker.getPresentWalker();
    }

    @Override
    public int getCount() {
        return walkersAhead.length + 1 + walkersBehind.length;
    }

    @Override
    public Walker getItem(int position) {
        if (position < walkersAhead.length) { // Ahead
            return walkersAhead[position];
        } else if (position > walkersAhead.length) { // Behind
            return walkersBehind[position - walkersAhead.length - 1];
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

        Walker item = getItem(position);
        text1.setText(item.getName());
        text2.setText(String.format("%d m, %.2f km/h", item.getDistance(), item.getAvgSpeed()));

        if (position < walkersAhead.length) { // Ahead
            convertView.setBackgroundColor(context.getResources().getColor(R.color.listitem_ahead));
        } else if (position > walkersAhead.length) { // Behind
            convertView.setBackgroundColor(context.getResources().getColor(R.color.listitem_behind));
        } else { // Me
            convertView.setBackgroundColor(context.getResources().getColor(R.color.listitem_me));
        }

        return convertView;
    }
}
