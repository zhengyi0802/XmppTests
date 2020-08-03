package tw.com.munditv.mundicontroller;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import tw.com.munditv.mundicontroller.ui.login.LoginActivity;
import tw.com.munditv.mundicontroller.xmppservice.IXmppCommService;
import tw.com.munditv.mundicontroller.xmppservice.XmppCommService;
import tw.com.munditv.mundicontroller.xmppservice.XmppSerivceCallback;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";
    private AppBarConfiguration mAppBarConfiguration;
    private XmppCommServiceConnection mXmppCommServiceConnection;
    private IXmppCommService iXmppCommService;

    private Intent mServiceIntent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        Log.i(TAG, "onCreateOptionsMenu()");
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        Log.i(TAG, "onSupportNavigateUp()");
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "onOptionsItemSelected()");
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.action_settings:
                intent = new Intent(this, SettingsActivity.class);
                break;
            case R.id.action_login:
                intent = new Intent(this, LoginActivity.class);
                break;
        }
        try {
            if (intent != null) startActivity(intent);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart()");
        super.onStart();
        if (mServiceIntent == null) {
            mServiceIntent = new Intent(this, XmppCommService.class);
            XmppCommServiceConnection mXmppCommServiceConnection = new XmppCommServiceConnection();
            boolean ret = bindService(mServiceIntent, mXmppCommServiceConnection, BIND_AUTO_CREATE);
            if (!ret) {
                Toast.makeText(this, "Service can not binded", 3);
                finish();
                return;
            }
            startService(mServiceIntent);
        }
        Handler mHandler = new Handler();
        mHandler.postDelayed(checkpref, 500);
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy()");
        super.onDestroy();
        unbindService(mXmppCommServiceConnection);
        mXmppCommServiceConnection = null;
        if (mServiceIntent != null) {
            stopService(mServiceIntent);
        }
        mServiceIntent = null;
    }

    private class XmppCommServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i(TAG, "onServiceConnected()");
            iXmppCommService = IXmppCommService.Stub.asInterface(iBinder);
            try {
                iXmppCommService.registerCallback(mCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "onServiceDisconnected()");
            iXmppCommService = null;
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Log.i(TAG, "onBindingDied()");
            iXmppCommService = null;
        }

        @Override
        public void onNullBinding(ComponentName name) {
            Log.i(TAG, "onNullBinding()");
            iXmppCommService = null;
        }
    }

    private XmppSerivceCallback.Stub mCallback = new XmppSerivceCallback.Stub() {
        @Override
        public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
                               double aDouble, String aString) throws RemoteException {

        }


    };

    private Runnable checkpref = new Runnable() {
        @Override
        public void run() {
            checkPreferences();
        }
    };

    private void checkPreferences() {
        Log.i(TAG, "checkPreferences()");
        SharedPreferences preferences;
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String username = preferences.getString("username", null);
        String password = preferences.getString("password", null);
        String hostname = preferences.getString("xmppserver", "webrtc01.mundi-tv.tk");
        if (username != null && password != null) {
            try {
                Log.i(TAG, "login()");
                iXmppCommService.login(username, password);
            } catch(RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "SettingsActivity()");
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
    }

}

