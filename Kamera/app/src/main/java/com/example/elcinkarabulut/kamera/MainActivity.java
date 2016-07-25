package com.example.elcinkarabulut.kamera;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.view.View.OnClickListener;
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, SensorEventListener {


    TessOCR mTessOCR = new TessOCR();
    private String mLanguage = null;
    private String mRecognizedText = null;
    public boolean isProgress = false, isChanging = false, isConnected = true;
    private Camera mCamera;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private LayoutInflater controlInflater = null;
    public TextView mTranslationText;
    private ToggleButton mFlashMode, changeLanguage;
    private static final double THRESHOLD = 1.2;
    private final AutoFocus focus = new AutoFocus();
    private final previewCallback cb = new previewCallback();
    private SensorManager sensorManager;
    private String to, from;
    private NetworkChangeReceiver receiver;
    AlertDialog.Builder builder;

    //Çeviri İşlemi
    void Translate(final String value) {

        new TranslateAsyncTask(new TranslateCallback() {

            @Override
            public void onSuccess(String result) {
                //Eğer dil değiştirme işlemi yapılmadıysa çeviriyi ekranda gösterir.
                if (!isChanging) mTranslationText.setText(result);
                    //Yapıldıysa kullanıcıyı mesaj ile uyarır.
                else mTranslationText.setText("Dil Değiştirildi.");

                //Okuma işleminin yeniden yapılmasına izin verir.
                isProgress = false;

                //İnternet bağlantısı koparsa ekranda hiçbir metin göstermez.
                if (!isConnected)
                    mTranslationText.setText("");
            }
        }, to, from).execute(value);  //Çeviri işlemi için o an seçili olan kaynak ve hedef dili, çevirilecek metni yandexe gönderir.

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {


        //Bildirim çubuğunu gizleme
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //Ekranı yatay hale getirme
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //İnternet bağlantısı kontrolü
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkChangeReceiver();
        registerReceiver(receiver, filter);
        builder = new AlertDialog.Builder(this);



        mSurfaceView = (SurfaceView) findViewById(R.id.camera_surface);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        controlInflater = LayoutInflater.from(getBaseContext());
        View viewControl = controlInflater.inflate(R.layout.activity_control, null);
        LayoutParams layoutParamsControl = new LayoutParams(LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
        this.addContentView(viewControl, layoutParamsControl);
        mTranslationText = (TextView) findViewById(R.id.mTranslationText);
        mTranslationText.setMovementMethod(new ScrollingMovementMethod());
        changeLanguage = (ToggleButton) findViewById(R.id.changeLanguage);
        mFlashMode = (ToggleButton) findViewById(R.id.mFlashMode);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //Bağlangıç kaynak ve hedef dil ayarları
        changeLanguage.setChecked(true);
        mLanguage = "tur";
        mTessOCR.recognizeTurkish(mLanguage);
        from = "tur";
        to = "eng";

        //Dil değiştirme
        changeLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTranslationText.setText("Dil Değiştirildi");
                boolean on = ((ToggleButton) v).isChecked();
                isChanging = true;
                isProgress = false;

                if (!on) {
                    mLanguage = "eng";
                    from = "eng";
                    to = "tur";

                } else {
                    mLanguage = "tur";
                    from = "tur";
                    to = "eng";
                }
                mTessOCR.recognizeTurkish(mLanguage);
            }
        });

        //Flaş ışığı açma/kapama
        mFlashMode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                final Camera.Parameters p = mCamera.getParameters();
                boolean on = ((ToggleButton) view).isChecked();
                if (on) {
                    p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    mCamera.setParameters(p);
                } else {
                    p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    mCamera.setParameters(p);
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        try {

            mCamera.setPreviewDisplay(mSurfaceHolder);

        } catch (IOException e) {
            e.printStackTrace();
        }

        final Camera.Parameters params = mCamera.getParameters();
        final String bestFocusMode = findFocusMode(params.getSupportedFocusModes());
        params.setFocusMode(bestFocusMode);
        mCamera.setParameters(params);
        mCamera.startPreview();

    }

    //Kameranın odaklama modları
    private String findFocusMode(List<String> focusMode) {
        if (focusMode.size() == 1) {
            return focusMode.get(0);
        }
        if (focusMode.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            return Camera.Parameters.FOCUS_MODE_AUTO;
        } else if (focusMode.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            return Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
        } else if (focusMode.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            return Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
        }
        throw new RuntimeException("Odaklama yapılamadı." + Arrays.toString(focusMode.toArray()));

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.setOneShotPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            finish();

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(event);
        }
    }

    private void getAccelerometer(final SensorEvent sensorEvent) {
        if (mCamera == null) {
            return;
        }

        final float[] values = sensorEvent.values;
        final float x = values[0];
        final float y = values[1];
        final float z = values[2];

        final float movement = (x * x + y * y + z * z) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);

        //Eğer mobil cihazın ivmesi 1.2nin üstünde ise, okumaya izin verilmişsse ve internete bağlıysa çeviri işlemi için odaklama yap
        if (movement >= THRESHOLD && !isProgress && isConnected) {

            isProgress = true;
            mCamera.autoFocus(focus);

        }
    }

    private class AutoFocus implements Camera.AutoFocusCallback {
        @Override
        public void onAutoFocus(final boolean success, final Camera camera) {


            if (success) {
                mTranslationText.setText("Metin Okunuyor...");
                isChanging = false;
                //O an kamerada bulunan görüntüyü al
                mCamera.setOneShotPreviewCallback(cb);
                return;

            }
            mCamera.autoFocus(this);


        }
    }


    private class NetworkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent ıntent) {
            ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

            //İnternet bağlantısı varsa
            if (conMgr.getActiveNetworkInfo() != null

                    && conMgr.getActiveNetworkInfo().isAvailable()

                    && conMgr.getActiveNetworkInfo().isConnected()) {
                if (!isConnected) {
                    isConnected = true;
                    mTranslationText.setText("");
                    changeLanguage.setEnabled(true);
                    mFlashMode.setEnabled(true);
                    builder.setMessage("İnternete Bağlandınız!").setPositiveButton("Tamam", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });

                    final AlertDialog alert = builder.create();
                    alert.show();
                }
            }
            //İnternet bağlantısı yoksa
            else {

                isConnected = false;
                mTranslationText.setText("");
                changeLanguage.setEnabled(false);
                mFlashMode.setEnabled(false);
                builder.setMessage("İnternet Bağlantınız Bulunmamaktadır!").setPositiveButton("Uygulamayı Kapat", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        finish();
                    }
                });

                final AlertDialog alert = builder.create();
                alert.show();

            }
        }
    }

    private class previewCallback implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {

            final Camera c = camera;
            final byte[] d = data;
            runOnUiThread(new Runnable() {
                public void run() {
                    try {

                        //Dil değiştiriliyorsa hiçbir işlem yapma
                        while (isChanging)
                            wait();

                        Camera.Parameters parameters = c.getParameters();
                        int width = parameters.getPreviewSize().width;
                        int height = parameters.getPreviewSize().height;

                        //Optik Karakter Tanımlama yap
                        mRecognizedText = (mTessOCR.getOCRResult(d, parameters, width, height));
                        //Çeviri İşlemini Yap
                        Translate(mRecognizedText);

                    } catch (Exception e) {
                    }

                }
            });

        }
    }


}
