package tw.com.munditv.mundicontroller.xmppservice;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public class XmppCommService extends Service {

    private final static String TAG = "XmppCommService";
    private HandlerThread mWorkerThread;
    private Handler mHandler;
    private PowerManager.WakeLock mWakeLock;

    private XmppConnection mXmppConnection = null;

    public XmppCommService() {
        super();
        Log.i(TAG, "constructor");

    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate()");
        super.onCreate();

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getSimpleName());

        mWakeLock.acquire();
        mWorkerThread = new HandlerThread(getClass().getSimpleName(),
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mWorkerThread.start();
        mHandler = new Handler(mWorkerThread.getLooper());
        mXmppConnection = new XmppConnection();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand()");
        mXmppConnection.openConnection();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        super.onDestroy();
        mXmppConnection.closeConnection();
        mXmppConnection = null;
        mWorkerThread.quitSafely();
        mWakeLock.release();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.i(TAG, "onConfigurationChanged()");
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        Log.i(TAG, "onLowMemory()");
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        Log.i(TAG, "onTrimMemory()");
        super.onTrimMemory(level);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind()");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        Log.i(TAG, "onRebind()");
        super.onRebind(intent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.i(TAG, "onTaskRemoved()");
        super.onTaskRemoved(rootIntent);
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        Log.i(TAG, "dump()");
        super.dump(fd, writer, args);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        Log.i(TAG, "attachBaseContext()");
        super.attachBaseContext(newBase);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind()");
        return mXmppComm;
    }

    protected void enqueueJob(Runnable job) {
        mHandler.post(job);
    }

    private IXmppCommService.Stub mXmppComm = new IXmppCommService.Stub() {
        @Override
        public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {
            Log.i(TAG, "IXmppCommService::basicTypes()");
        }
        @Override
        public boolean login(String username, String password) {
            Log.i(TAG, "IXmppCommService::login(\"" + username +"\", \"" + password + "\")_");
            final String account = username;
            final String passwd = password;
            enqueueJob(new Runnable() {
                @Override
                public void run() {
                    mXmppConnection.login(account, passwd);
                }
            });
            return mXmppConnection.isAuthenticated();
        }
        @Override
        public boolean isAuthenticated() {
            Log.i(TAG, "IXmppCommService::isAuthenticated()");
            return mXmppConnection.isAuthenticated();
        }
        @Override
        public void addGroup(String groupName) {
            Log.i(TAG, "IXmppCommService::addGroup(\"" + groupName + "\")");
            mXmppConnection.addGroup(groupName);
            return;
        }
        @Override
        public void removeGroup(String groupName) {
            Log.i(TAG, "IXmppCommService::removeGroup(\"" + groupName + "\")");
            mXmppConnection.removeGroup(groupName);
            return;
        }
        @Override
        public boolean addUser(String userName, String name, String groupName) {
            return mXmppConnection.addUser(userName, name, groupName);
        }
        @Override
        public boolean removeUser(String userName) {
            return mXmppConnection.removeUser(userName);
        }
        @Override
        public void changeStateMessage(String status) {
            mXmppConnection.changeStateMessage(status);
        }
        @Override
        public boolean deleteAccount() {
            return mXmppConnection.deleteAccount();
        }
        @Override
        public boolean changePassword(String pwd) {
            return mXmppConnection.changePassword(pwd);
        }

        @Override
        public void registerCallback(XmppSerivceCallback callback) throws RemoteException {

        }
    };

}
