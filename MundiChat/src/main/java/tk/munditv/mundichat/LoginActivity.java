package tk.munditv.mundichat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import tk.munditv.mundichat.utils.Logger;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText mAccount;
    private EditText mPassword;
    private Button mButtonRegister;
    private Button mButtonLogin;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAccount = findViewById(R.id.txt_account);
        mPassword = findViewById(R.id.txt_password);
        mButtonRegister = findViewById(R.id.btn_register);
        mButtonLogin = findViewById(R.id.btn_login);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mButtonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAccount.getText() != null && mPassword.getText() != null) {
                    savePreferences();
                    MyApplication.getInstance().RefreshPreference();
                    try {
                        MyApplication.getInstance().doRegister();
                        finish();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mButtonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAccount.getText() != null && mPassword.getText() != null) {
                    savePreferences();
                    MyApplication.getInstance().RefreshPreference();
                    try {
                        MyApplication.getInstance().doConnect();
                        finish();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void savePreferences() {
        Logger.debug(TAG, "username = " + mAccount.getText().toString());
        Logger.debug(TAG, "password = " + mPassword.getText().toString());
        preferences.edit().putString("username", mAccount.getText().toString()).commit();
        preferences.edit().putString("password", mPassword.getText().toString()).commit();
        preferences.edit().putString("xmppserver", "webrtc01.mundi-tv.tk").commit();
        preferences.edit().putString("boshflag", "BOSH").commit();
        preferences.edit().putBoolean("sync", true).commit();

        return;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
