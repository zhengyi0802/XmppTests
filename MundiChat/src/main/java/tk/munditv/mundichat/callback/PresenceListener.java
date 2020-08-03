package tk.munditv.mundichat.callback;

import android.os.RemoteException;

public interface PresenceListener {

    public void onConnected() throws RemoteException;
    public void onDisconnected();
}
