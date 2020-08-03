package tk.munditv.mundichat.callback;

import android.os.RemoteException;

public interface RosterListener {

    public void onRosterChanged() throws RemoteException;
    public void onContactAdded(String remoteAccount);
    public void onContactRemoved(String remoteAccount);
    public void onContactRenamed(String remoteAccount, String newAlias);
    public void onContactAddError(String remoteAccount);
    //public void onGetRosterEntries() throws RemoteException;
}
