package tk.munditv.mundichat.ui.contacts;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import java.util.ArrayList;

import tk.munditv.mundichat.MyApplication;
import tk.munditv.mundichat.R;
import tk.munditv.mundichat.ui.chat.SingleChatRoomFragment;
import tk.munditv.mundichat.utils.Logger;
import tk.munditv.mundichat.viewmodel.ContactsViewModel;
import tk.munditv.mundichat.xmpp.XmppRosterEntry;
import tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventReceiver;

public class ContactsFragment extends Fragment {

    private final static String TAG = "ContactsFragment";

    private ContactsViewModel contactsViewModel;
    private Context mContext;
    private XmppServiceBroadcastEventReceiver receiver;
    private TextView mAccount;
    private ListView mListView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Logger.debug(TAG, "onCreateView");
        contactsViewModel =
                ViewModelProviders.of(this).get(ContactsViewModel.class);
        View root = inflater.inflate(R.layout.fragment_contacts, container, false);
        mAccount = root.findViewById(R.id.account_id);
        mListView = root.findViewById(R.id.contacts_listview);
        mListView.setOnItemClickListener(listener);
        mContext = getContext();
        receiver = MyApplication.getInstance().getBroadcastReceiver();
        receiver.registerPresenceCallback(mContext, contactsViewModel);
        receiver.registerRosterCallback(mContext, contactsViewModel);
        contactsViewModel.getAccount().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String account) {
                mAccount.setText(account);
            }
        });
        contactsViewModel.getContacts().observe(getViewLifecycleOwner(), new Observer<ArrayList<XmppRosterEntry>>() {
            @Override
            public void onChanged(@Nullable ArrayList<XmppRosterEntry> arrayList) {
                RosterAdapter mAdapter = new RosterAdapter(mContext, R.layout.contacts_list, arrayList);
                mListView.setAdapter(mAdapter);
            }
        });
        return root;
    }

    private ListView.OnItemClickListener listener = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            XmppRosterEntry selected = (XmppRosterEntry) view.getTag();
            MyApplication.getInstance().setRosterEntry(selected);

            Logger.debug(TAG, "selected = " + selected.getAlias());
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            SingleChatRoomFragment fragment = new SingleChatRoomFragment();
            fragmentTransaction.replace(R.id.nav_host_fragment, fragment, selected.getAlias());
            fragmentTransaction.commit();
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        Logger.info(TAG, "onStart()");
        String account =  contactsViewModel.getAccount().getValue();
        mAccount.setText(account);
        Logger.debug(TAG, "account = " + account);
        ArrayList<XmppRosterEntry> arrayList = contactsViewModel.getContacts().getValue();
        //RosterAdapter mAdapter = new RosterAdapter(mContext, R.layout.contacts_list, arrayList);
        //mListView.setAdapter(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.info(TAG, "onResume()");
    }

    @Override
    public void onPause() {
        super.onPause();
        Logger.info(TAG, "onPause()");
    }

    @Override
    public void onStop() {
        super.onStop();
        Logger.info(TAG, "onStop()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.info(TAG, "onDestroy()");
    }
}
