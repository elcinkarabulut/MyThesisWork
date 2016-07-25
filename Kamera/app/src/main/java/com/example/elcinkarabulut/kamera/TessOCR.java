package com.example.elcinkarabulut.kamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.ByteArrayOutputStream;

/**
 * Created by ElcinKarabulut on 4.5.2016.
 */
public class TessOCR {

    public final String mLanguagePath = Environment.getExternalStorageDirectory().toString() + "/Görüntülü Çeviri/";
    TessBaseAPI mBaseApi = new TessBaseAPI();
    private Bitmap mPhoto = null;
    private String mOCRText;

    public TessOCR() {

        mBaseApi = new TessBaseAPI();
        mBaseApi.setDebug(false);

    }

    //Türkçe metnin algılanması
    public void recognizeTurkish(String mLanguage) {

        mBaseApi.init(mLanguagePath, mLanguage);
        //Algılanacak karakterler
        mBaseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "ABCÇDEFGĞHIİJKLMNOÖPRSŞTUÜVYZabcçdefgğhıijklmnoöprsştuüvyz");
        //Algılanmayacak karakterler
        mBaseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!@#$%^&*()_+=-[]}{" +
                ";:'\"\\|~`,./<>?1234567890qQwWxX");

    }

    //İngilizce metnin algılanması
    public void recognizeEnglish(String mLanguage) {

        mBaseApi.init(mLanguagePath, mLanguage);
        //Algılanacak karakterler
        mBaseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
        //Algılanmayacak karakterler
        mBaseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!@#$%^&*()_+=-[]}{" +
                ";:'\"\\|~`,./<>?1234567890çÇğĞıİöÖşŞüÜ");
    }

    //Kameradan alınan görüntünün bitmap yapılması ve karakterlerin okunması
    public String getOCRResult(byte[] d, Camera.Parameters parameters, int width, int height) {

        YuvImage yuv = new YuvImage(d, parameters.getPreviewFormat(), width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, width, height), 50, out);
        byte[] bytes = out.toByteArray();
        mPhoto = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        mPhoto = mPhoto.copy(Bitmap.Config.ARGB_8888, true);

        mBaseApi.setImage(mPhoto);
        mOCRText = mBaseApi.getUTF8Text();
        mOCRText = mOCRText.replaceAll("\\s+", " ");
        mBaseApi.clear();
        return mOCRText;
    }

     public void onDestroy() {
        if (mBaseApi != null)
            mBaseApi.end();
    }


}
