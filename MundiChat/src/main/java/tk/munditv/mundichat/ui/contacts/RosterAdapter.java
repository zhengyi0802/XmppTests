package tk.munditv.mundichat.ui.contacts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import tk.munditv.mundichat.R;
import tk.munditv.mundichat.utils.Logger;
import tk.munditv.mundichat.xmpp.XmppRosterEntry;

public class RosterAdapter extends ArrayAdapter
        implements View.OnClickListener {

    private final static String TAG = "RosterAdapter";

    private ArrayList<XmppRosterEntry> xmppRosterEntry;

    private Context mContext;
    private int mResource;
    private int lastPosition = -1;

    private static class ViewHolder {
        String remoteAccount;
        ImageView avatar;
        TextView alias;
    }

    public RosterAdapter(@NonNull Context context, int resource, @NonNull List objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
        xmppRosterEntry = (ArrayList<XmppRosterEntry>) objects;
        Logger.debug(TAG, "Roster size = " + xmppRosterEntry.size());
        for(int i=0; i < xmppRosterEntry.size(); i++) {
            XmppRosterEntry m = xmppRosterEntry.get(i);
            Logger.debug(TAG, "i=" + i + ", alias=" + m.getAlias());
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        XmppRosterEntry item = xmppRosterEntry.get(position);
        ViewHolder viewHolder;

        final View result;

        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.contacts_list, parent, false);
            viewHolder.alias = (TextView) convertView.findViewById(R.id.contact_alias);
            viewHolder.avatar = (ImageView) convertView.findViewById(R.id.contact_avatar);
            convertView.setTag(viewHolder);
            result = convertView;
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            result = convertView;
        }

        Logger.debug(TAG, "position = " + position + ", alias = " + item.getAlias());
        String alias = item.getAlias();
        viewHolder.remoteAccount = item.getXmppJID();
        viewHolder.alias.setText(alias.split("@")[0]);
        viewHolder.avatar.setImageDrawable(item.getAvatarDrawable(mContext));
        result.setTag(item);

        return result;
    }

    @Override
    public void onClick(View view) {
        String tag =  (String) view.getTag();
        Logger.debug(TAG, " tag = " + tag);
    }
}
