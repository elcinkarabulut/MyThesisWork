package com.example.elcinkarabulut.kamera;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Splash extends AppCompatActivity {

    private final int mSplashDisplay = 1000;
    TessOCR mTessOCR=new TessOCR();

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //Splash ekranından sonra Çeviri ekranının açılacağı belirlenir.
                Intent mainIntent = new Intent(Splash.this, MainActivity.class);
                Splash.this.startActivity(mainIntent);

                String[] mLanguages = new String[]{"tur", "eng"};
                String[] mPaths = new String[]{mTessOCR.mLanguagePath, mTessOCR.mLanguagePath + "tessdata/"};

                //Çeviri işlemi dil paketleri için mobil cihazda klasörler oluşturulur
                for (String path : mPaths) {
                    File dir = new File(path);
                    if (!dir.exists())
                        dir.mkdir();
                }

                for (String language : mLanguages) {
                    writeFile(language);
                }
                Splash.this.finish();
            }
        }, mSplashDisplay);
    }

    //Dil paketlerinin mobil cihaza yüklenmesi
    void writeFile(String language) {

        if (!(new File(mTessOCR.mLanguagePath + "tessdata/" + language + ".traineddata")).exists()) {
            try {

                AssetManager assetManager = getAssets();
                InputStream in = assetManager.open("tessdata/" + language + ".traineddata");
                OutputStream out = new FileOutputStream(mTessOCR.mLanguagePath + "tessdata/" + language + ".traineddata");


                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
