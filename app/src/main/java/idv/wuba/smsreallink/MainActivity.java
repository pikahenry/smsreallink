package idv.wuba.smsreallink;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
//import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.klinker.android.send_message.Message;
import com.klinker.android.send_message.Settings;
import com.klinker.android.send_message.Transaction;

import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMISSION = 0;
    private IntentIntegrator integrator;

    private final AtomicBoolean checkReturn = new AtomicBoolean(false);
    private boolean sending = false;

    private Button buttonPermit;
    final int REQUEST_CODE_ASK_PERMISSIONS = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        integrator = new IntentIntegrator(this);
        integrator.setCaptureActivity(MyCaptureActivity.class);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt(getString(R.string.scan_qr));
        integrator.setCameraId(0);  // Use a specific camera of the device
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(true);
        integrator.setOrientationLocked(true);
//        integrator.setRequestCode(REQ_SCAN_QR);
        setContentView(R.layout.activity_main);
        buttonPermit = findViewById(R.id.buttonPermit);
        ActivityCompat.requestPermissions(this, new String[]{"android.permission.READ_SMS"}, REQUEST_CODE_ASK_PERMISSIONS);
    }

    @Override
    protected void onResume() {
        Log.d("MainActivity", "onResume");
        if (sending) {
            super.onResume();
            finish();
            return;
        }
        if (!checkReturn.get() && checkPermission()) {
            integrator.initiateScan();
        } else if (checkReturn.get() && hasPermission())
            integrator.initiateScan();
        checkReturn.set(true);
        super.onResume();
    }

    private boolean checkPermission() {
        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) ||
                PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this,
                        Manifest.permission.SEND_SMS)) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.SEND_SMS))
                runOnUiThread(() -> buttonPermit.setEnabled(true));
            else
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.SEND_SMS},
                        REQ_PERMISSION);
            return false;
        }
        return true;
    }

    private boolean hasPermission() {
        return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) &&
                PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this,
                        Manifest.permission.SEND_SMS);
    }

    public void headToDetailSettings() {
        Toast.makeText(this, R.string.not_permitted, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    public void onPermitClick(View v) {
        headToDetailSettings();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("MainActivity", "onActivityResult: " + resultCode);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (0 == resultCode)
            finish();
        if (null != result) {
            if (result.getContents() == null) {
                Log.e("QR", "Error...");
            } else {
//                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
//                Log.d("SMS", result.getContents());
                String qr = result.getContents();
                if (qr.startsWith("SMSTO:1922:")) {
                    sending = true;
                    qr = qr.replace("SMSTO:1922:", "");
                    Log.d("SMS", qr);
                    Settings settings = new Settings();
                    settings.setUseSystemSending(true);
                    Transaction transaction = new Transaction(this, settings);
                    Message message = new Message(qr, "1922");
                    transaction.sendNewMessage(message, Transaction.NO_THREAD_ID);
                    Toast.makeText(this, R.string.sending, Toast.LENGTH_LONG).show();
                    String number = "1922";  // The number on which you want to send SMS
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", number, null)));
                } else {
                    Toast.makeText(this, R.string.qr_error, Toast.LENGTH_LONG).show();
//                    integrator.initiateScan();
                }
            }
        } else {
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d("MainActivity", "onRequestPermissionsResult");
        if (REQ_PERMISSION == requestCode) {
            if (PackageManager.PERMISSION_GRANTED == grantResults[0] && PackageManager.PERMISSION_GRANTED == grantResults[1]) {
                integrator.initiateScan();
                checkReturn.set(true);
            } else
                runOnUiThread(() -> buttonPermit.setEnabled(true));
        } else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}