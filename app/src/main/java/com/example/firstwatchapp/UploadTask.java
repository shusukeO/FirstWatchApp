package com.example.firstwatchapp;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;


public class UploadTask extends AsyncTask<String, Void, String> {

    private Listener listener;

    // 非同期処理
    @Override
    protected String doInBackground(String... params) {

        // 使用するサーバーのURLに合わせる
//        String urlSt = "http://10.0.2.2:3000/public";

//        String urlSt = "http://192.168.11.3:3000/public";

        String urlSt = "http://192.168.50.208:3000/public";

//        String urlSt = "http://httpbin.org/post";


        HttpURLConnection httpConn = null;
        String result = null;
//        String word = "word="+params[0];

        try{
            // URL設定
            URL url = new URL(urlSt);

            // HttpURLConnection
            httpConn = (HttpURLConnection) url.openConnection();

            // request POST
            httpConn.setRequestMethod("POST");

            // no Redirects
            httpConn.setInstanceFollowRedirects(false);

            // データを書き込む
            httpConn.setDoOutput(true);

            // 時間制限
            httpConn.setReadTimeout(10000);
            httpConn.setConnectTimeout(20000);

            httpConn.addRequestProperty("Content-Type", "application/json; charset=UTF-8");

            // 接続
            httpConn.connect();

            try(// POSTデータ送信処理
                OutputStream outStream = httpConn.getOutputStream()) {
//                word = (word.getBytes(StandardCharsets.UTF_8).toString()).replace("\\", "aa");
//                outStream.write( word.getBytes(StandardCharsets.UTF_8));
//                outStream.flush();
//                Log.d("debug","flush");

//                HashMap<String, Object> jsonMap = new HashMap<>();
//                jsonMap.put("text" , "value");
//                ArrayList<String> array = new ArrayList<>();
//                array.add("array001");
//                array.add("array002");
//                array.add("array003");
//                jsonMap.put("array" , array);
//                if (jsonMap.size() > 0) {
//                    //JSON形式の文字列に変換する。
//                    JSONObject responseJsonObject = new JSONObject(jsonMap);
//                    String jsonText = responseJsonObject.toString();
//                    PrintStream ps = new PrintStream(httpConn.getOutputStream());
//                    ps.print(jsonText);
//                    ps.close();
//                }

                PrintStream ps = new PrintStream(httpConn.getOutputStream());
                ps.print(params[0]);
                ps.close();

                outStream.close();
                outStream.flush();
                Log.d("debug","flush");

                result = convertToString(httpConn.getInputStream());

            } catch (IOException e) {
                // POST送信エラー
                e.printStackTrace();
                result = "POST送信エラー";
            }

//            final int status = httpConn.getResponseCode();
//            if (status == HttpURLConnection.HTTP_OK) {
//                // レスポンスを受け取る処理等
//                result="HTTP_OK";
//            }
//            else{
//                result="status="+String.valueOf(status);
//            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
        return result;
    }

    // 非同期処理が終了後、結果をメインスレッドに返す
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        if (listener != null) {
            listener.onSuccess(result);
        }
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    interface Listener {
        void onSuccess(String result);
    }

    public String convertToString(InputStream stream) throws IOException {
        StringBuffer sb = new StringBuffer();
        String line = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        try {
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
