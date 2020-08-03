package tk.munditv.mundichat.ui.settings;

import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import tk.munditv.mundichat.MyApplication;
import tk.munditv.mundichat.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Preference passwd = findPreference("password");
        passwd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
/*
                    String stringValue = value.toString();
                    stringValue = toStars(stringValue);
                    preference.setSummary(stringValue);

*/
                return true;
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        Preference account = findPreference("username");
        Preference passwd = findPreference("password");
        if(account != null && passwd != null) {
            try {
                MyApplication.getInstance().doConnect();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
