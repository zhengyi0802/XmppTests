package tk.munditv.mundichat.viewmodel;

import android.os.RemoteException;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.gson.Gson;

import java.util.ArrayList;

import tk.munditv.mundichat.MyApplication;
import tk.munditv.mundichat.callback.PresenceListener;
import tk.munditv.mundichat.callback.RosterListener;
import tk.munditv.mundichat.utils.Logger;
import tk.munditv.mundichat.xmpp.XmppRosterEntry;

public class ContactsViewModel extends ViewModel
        implements PresenceListener, RosterListener {

    private final static String TAG = "ContactsViewModel";

    private MutableLiveData<String> mAccount;
    private MutableLiveData<ArrayList<XmppRosterEntry>> mContacts;
    private ArrayList<XmppRosterEntry> arrayList;

    public ContactsViewModel() throws RemoteException {
        mContacts = new MutableLiveData<>();
        mAccount = new MutableLiveData<>();
        MyApplication.getInstance().doConnect();
    }

    public LiveData<String> getAccount() {
        return mAccount;
    }

    public LiveData<ArrayList<XmppRosterEntry>> getContacts() {
        return mContacts;
    }

    @Override
    public void onRosterChanged() throws RemoteException {
        String str = MyApplication.getInstance().doGetRosterEntries();
        XmppRosterEntry[] rosterEntries = new Gson().fromJson(str, XmppRosterEntry[].class);
        if(rosterEntries == null) return;
        arrayList = new ArrayList<>();
        for(int i=0; i < rosterEntries.length; i++) {
            arrayList.add(rosterEntries[i]);
        }
        mContacts.postValue(arrayList);
    }

    @Override
    public void onContactAdded(String remoteAccount) {

    }

    @Override
    public void onContactRemoved(String remoteAccount) {

    }

    @Override
    public void onContactRenamed(String remoteAccount, String newAlias) {

    }

    @Override
    public void onContactAddError(String remoteAccount) {

    }
/*
    @Override
    public void onGetRosterEntries() throws RemoteException {
        String str = MyApplication.getInstance().doGetRosterEntries();
        Logger.debug(TAG, "onGetRosterEntries() str = " + str);
        XmppRosterEntry[] rosterEntries = new Gson().fromJson(str, XmppRosterEntry[].class);
        arrayList = new ArrayList<>();
        for(int i=0; i < rosterEntries.length; i++) {
            arrayList.add(rosterEntries[i]);
        }
        mContacts.postValue(arrayList);
    }
*/
    @Override
    public void onConnected() throws RemoteException {
        Logger.debug(TAG, "onConnected account = " + MyApplication.getInstance().getUsername());
        String account = MyApplication.getInstance().getUsername();
        mAccount.postValue(account);
    }

    @Override
    public void onDisconnected() {
        mAccount.postValue(null);
    }
}