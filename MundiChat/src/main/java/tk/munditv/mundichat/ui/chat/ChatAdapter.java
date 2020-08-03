package tk.munditv.mundichat.ui.chat;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.jivesoftware.smackx.vcardtemp.packet.VCard;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import tk.munditv.mundichat.MyApplication;
import tk.munditv.mundichat.R;
import tk.munditv.mundichat.database.models.Message;
import tk.munditv.mundichat.xmpp.XmppAccount;
import tk.munditv.mundichat.xmpp.XmppRosterEntry;
import tk.munditv.mundichat.xmpp.XmppVCard;

import static tk.munditv.mundichat.database.tables.MessagesTable.Direction.INCOMING;
import static tk.munditv.mundichat.database.tables.MessagesTable.Direction.OUTGOING;

public class ChatAdapter extends RecyclerView.Adapter {

    private int mResource;
    private Context mContext;
    private ArrayList<Message> mMessages;

    public ChatAdapter(Context mContext, int mResource, ArrayList<Message> mMessages) {
        this.mResource = mResource;
        this.mContext = mContext;
        this.mMessages = mMessages;
    }

    /**
     * Called when RecyclerView needs a new {@link ViewHolder} of the given type to represent
     * an item.
     * <p>
     * This new ViewHolder should be constructed with a new View that can represent the items
     * of the given type. You can either create a new View manually or inflate it from an XML
     * layout file.
     * <p>
     * The new ViewHolder will be used to display items of the adapter using
     * {@link #onBindViewHolder(ViewHolder, int, List)}. Since it will be re-used to display
     * different items in the data set, it is a good idea to cache references to sub views of
     * the View to avoid unnecessary {@link View#findViewById(int)} calls.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     * @see #getItemViewType(int)
     * @see #onBindViewHolder(ViewHolder, int)
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = View.inflate(mContext, mResource, null);
        return new viewHolder(itemView);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method should
     * update the contents of the {@link ViewHolder#itemView} to reflect the item at the given
     * position.
     * <p>
     * Note that unlike {@link ListView}, RecyclerView will not call this method
     * again if the position of the item changes in the data set unless the item itself is
     * invalidated or the new position cannot be determined. For this reason, you should only
     * use the <code>position</code> parameter while acquiring the related data item inside
     * this method and should not keep a copy of it. If you need the position of an item later
     * on (e.g. in a click listener), use {@link ViewHolder#getAdapterPosition()} which will
     * have the updated adapter position.
     * <p>
     * Override {@link #onBindViewHolder(ViewHolder, int, List)} instead if Adapter can
     * handle efficient partial bind.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        viewHolder holder1=(viewHolder) holder;
        Message m = mMessages.get(position);
        if (m.getDirectionAsEnum() == INCOMING) {
            XmppRosterEntry entry = MyApplication.getInstance().getRosterEntry();
            holder1.alias.setText(m.getRemoteAccount().split("@")[0]);
            holder1.avatar.setImageDrawable(entry.getAvatarDrawable(mContext));
        } else if (m.getDirectionAsEnum() == OUTGOING) {
            VCard mVCard = XmppVCard.getVCard(m.getAccount());
            holder1.avatar.setImageDrawable(null);
            if( mVCard != null){
                byte[] avatar = mVCard.getAvatar();
                Drawable drawable = new BitmapDrawable(mContext.getResources(),
                        BitmapFactory.decodeByteArray(avatar, 0, avatar.length));
                holder1.avatar.setImageDrawable(drawable);
            }
            holder1.alias.setText(m.getAccount());
        }
        holder1.message.setText(m.getMessage());
        String createtime = getDate(m.getCreationTimestamp());
        holder1.timestamp.setText(createtime);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    private class viewHolder extends RecyclerView.ViewHolder {
        private ImageView avatar;
        private TextView alias;
        private TextView message;
        private TextView timestamp;

        public viewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = (ImageView) itemView.findViewById(R.id.mbox_avatar);
            alias = (TextView) itemView.findViewById(R.id.mbox_alias);
            message = (TextView) itemView.findViewById(R.id.mbox_message);
            timestamp = (TextView) itemView.findViewById(R.id.mbox_timestamp);
        }
    }

    private String getDate(long milliSeconds) {
        // Create a DateFormatter object for displaying date in specified
        // format.
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
        // Create a calendar object that will convert the date and time value in
        // milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }

}
