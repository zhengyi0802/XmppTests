package tk.munditv.mundichat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.telephony.gsm.GsmCellLocation;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import tk.munditv.mundichat.utils.Logger;
import tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventReceiver;

import static tk.munditv.mundichat.xmpp.XmppAccount.PRESENCE_MODE_AVAILABLE;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";

    private final int ACTION_LOGIN = 0;
    private final int ACTION_SCANNER = 1;


    private Handler mHandler = new Handler();
    private boolean isSimulate = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.info(TAG, "onCreate()");
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_contacts, R.id.navigation_controller, R.id.navigation_settings)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
        if (!isSimulate) {
            initScanner();
        } else {
            //if(MyApplication.getInstance().getUsername() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, ACTION_LOGIN);
            //}
        }
        MyApplication.getInstance().setMainActivity(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTION_LOGIN) {
            //initScanner();
        } else {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result != null) {
                if (result.getContents() == null) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                    QrData qrdata = new Gson().fromJson(result.getContents(), QrData.class);
                    Toast.makeText(this, "account = " + qrdata.username, Toast.LENGTH_LONG).show();
                    Toast.makeText(this, "hostname = " + qrdata.host, Toast.LENGTH_LONG).show();
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, ACTION_LOGIN);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Logger.debug(TAG, "onStart()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Logger.info(TAG, "onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logger.info(TAG, "onDestroy()");
        try {
            MyApplication.getInstance().doDisconnect();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        MyApplication.getInstance().closeService();
    }

    public void setTitleName(String str) {
        String title = str.split("@")[0];
        getSupportActionBar().setTitle(title);
    }

    private void initScanner() {
        new IntentIntegrator(this)
                // 自定义Activity，重点是这行----------------------------
                .setCaptureActivity(CustomCaptureActivity.class)
                .setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)// 扫码的类型,可选：一维码，二维码，一/二维码
                .setPrompt("请对准二维码")// 设置提示语
                .setCameraId(0)// 选择摄像头,可使用前置或者后置
                .setBeepEnabled(true)// 是否开启声音,扫完码之后会"哔"的一声
                .setBarcodeImageEnabled(true)// 扫完码之后生成二维码的图片
                .initiateScan();// 初始化扫码
    }

}
