package com.example.elcinkarabulut.kamera;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by ElcinKarabulut on 4.4.2016.
 */
public class TranslateAsyncTask extends AsyncTask<String, Void, String> {

    TranslateCallback translateCallback;
    String to,from;

    public TranslateAsyncTask(TranslateCallback translateCallback, String to,String from) {
        //Çeviri işlemi için kaynak ve hedef dilin belirlenmesi
        this.translateCallback = translateCallback;
        this.to=to;
        this.from=from;
    }

    protected void onPostExecute(String result) {
        translateCallback.onSuccess(result);

    }



    @Override
    protected String doInBackground(String... params) {
        String result = new String();
        try {
            result += mTranslateData(params[0], to, from);
        } catch (IOException e) {
            return "Metin Çevirilemedi.";
        }
        return result;
    }

    //Yandex ile çevirir işleminin yapılması
    public String mTranslateData(String text, String to, String from) throws IOException {
        String key = "trnsl.1.1.20160404T133544Z.118307a783fa4264.b8bb47bd147e5440f61f898c192922f3e47b93a2";
        String uri = "https://translate.yandex.net/api/v1.5/tr.json/translate?key=" + key + "&text=" + text + "&lang=" + from + "-" + to + "&format=text";

        URL url = new URL(uri);
        URLConnection urlConnection = url.openConnection();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                urlConnection.getInputStream()));
        String json = br.readLine();
        br.close();

        String result = "";
        try {
            JSONObject reader = new JSONObject(json);
            result = reader.getString("text");
            result = result.replaceAll("\\p{P}", "");
            System.out.println(result);
        } catch (JSONException e) {
            return "Metin Çevirilemedi.";
        }
        return result;
    }


}