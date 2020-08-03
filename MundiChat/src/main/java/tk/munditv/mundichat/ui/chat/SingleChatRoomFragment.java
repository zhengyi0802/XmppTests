package tk.munditv.mundichat.ui.chat;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import tk.munditv.mundichat.MyApplication;
import tk.munditv.mundichat.R;
import tk.munditv.mundichat.database.models.Message;
import tk.munditv.mundichat.utils.Logger;
import tk.munditv.mundichat.viewmodel.MessagesViewModel;
import tk.munditv.mundichat.xmpp.XmppRosterEntry;
import tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventReceiver;

public class SingleChatRoomFragment extends Fragment {

    private static final String TAG = "SingleChatRoomFragment";
    private MessagesViewModel mMessagesViewModel;
    private String remoteAccount;
    private XmppServiceBroadcastEventReceiver receiver;
    private RecyclerView mRecyclerView;
    private Context mContext;
    private ChatAdapter mChatAdapter;
    private EditText mMessage;
    private Button mSend;
    private ArrayList<Message> mList;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Logger.debug(TAG, "onCreateView");
        mContext = getContext();
        mMessagesViewModel =
                ViewModelProviders.of(this).get(MessagesViewModel.class);
        try {
            mMessagesViewModel.initialize(mContext, this.getTag());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        View root = inflater.inflate(R.layout.fragment_singlechat, container, false);
        remoteAccount = this.getTag();
        MyApplication.getInstance().setTitle(this.getTag());
        receiver = MyApplication.getInstance().getBroadcastReceiver();
        receiver.registerMessageCallback(mContext, mMessagesViewModel);
        mMessage = (EditText) root.findViewById(R.id.edit_send_message);
        mSend = (Button) root.findViewById(R.id.btn_send);
        XmppRosterEntry entry = MyApplication.getInstance().getRosterEntry();
        remoteAccount = entry.getXmppJID();
        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = mMessage.getText().toString();
                try {
                    MyApplication.getInstance().doSendMessage(remoteAccount, msg);
                    mMessage.setText(null);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
        mRecyclerView = root.findViewById(R.id.messages_box);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mList = new ArrayList<Message>();
        mChatAdapter = new ChatAdapter(mContext, R.layout.item_message, mList);
        mRecyclerView.setAdapter(mChatAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(),
                DividerItemDecoration.VERTICAL));
        mMessagesViewModel.getMessages().observe(getViewLifecycleOwner(), new Observer<ArrayList<Message>>() {
            @Override
            public void onChanged(ArrayList<Message> messages) {
                Logger.debug(TAG, "getMessages() changed");
                mList.clear();
                mList.addAll(messages);
                mChatAdapter.notifyDataSetChanged();
                mRecyclerView.scrollToPosition(mList.size()-1);
            }
        });
        return root;
    }

    /**
     * Called when the Fragment is visible to the user.  This is generally
     * tied to {@link Activity#onStart() Activity.onStart} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onStart() {
        super.onStart();
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to {@link Activity#onResume() Activity.onResume} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * Called when the Fragment is no longer resumed.  This is generally
     * tied to {@link Activity#onPause() Activity.onPause} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onPause() {
        super.onPause();
    }

    /**
     * Called when the Fragment is no longer started.  This is generally
     * tied to {@link Activity#onStop() Activity.onStop} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onStop() {
        super.onStop();
    }

    /**
     * Called when the fragment is no longer in use.  This is called
     * after {@link #onStop()} and before {@link #onDetach()}.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
