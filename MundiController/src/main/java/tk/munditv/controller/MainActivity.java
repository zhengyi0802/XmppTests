package tk.munditv.controller;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import tk.munditv.controller.xmpp.Logger;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int ACTION_BARCODE = 1;
    private static final int ACTION_LOGIN = 2;

    private Intent intent = new Intent();
    private TextView mAccount;
    private TextView mRemote;
    private TextView mMessageBox;
    private EditText mMessage;
    private Button mSendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();
        initScanner();
    }

    private void initialize() {
        mAccount = findViewById(R.id.label_user);
        mRemote = findViewById(R.id.label_remoteaccount);
        mMessageBox = findViewById(R.id.message_box);
        mMessage = findViewById(R.id.edit_message);
        mSendButton = findViewById(R.id.btn_send);
        mAccount.setText(ControllerApp.getInstance().getAccount());
        mRemote.setText(ControllerApp.getInstance().getRemoteAccount());
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = mMessageBox.getText().toString();
                msg += mMessage.getText().toString() + "\n";
                ControllerApp.getInstance().sendMessage(mMessage.getText().toString());
                mMessage.setText("");
                mMessageBox.setText(msg);
            }
        });
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Logger.debug(TAG, "onActivityResult");
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                QrData qrdata = new Gson().fromJson(result.getContents(), QrData.class);
                Toast.makeText(this, "account = " + qrdata.username, Toast.LENGTH_LONG).show();
                Toast.makeText(this, "hostname = " + qrdata.host, Toast.LENGTH_LONG).show();
                Logger.debug(TAG, "username = " + qrdata.username);
                Logger.debug(TAG, "host = " + qrdata.host);
                ControllerApp.getInstance().setRemoteAccount(qrdata.username, qrdata.host);
                ControllerApp.getInstance().setSerialNumber(qrdata.serialno);
                intent = new Intent(this, LoginActivity.class);
                startActivityForResult(intent, ACTION_LOGIN);
                mAccount.setText(ControllerApp.getInstance().getAccount());
                mRemote.setText(ControllerApp.getInstance().getRemoteAccount());
            }
        } else {
            Logger.debug(TAG, "result = null");
            super.onActivityResult(requestCode, resultCode, data);
        }

    }

}
