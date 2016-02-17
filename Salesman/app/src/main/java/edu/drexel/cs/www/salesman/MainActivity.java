package edu.drexel.cs.www.salesman;

import android.app.Activity;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

    int x = 2;
    public String routeUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            String apiKey = "aaaabbbbccccddddeeeeffff111122223333444";
            String sURL = "https://maps.googleapis.com/maps/api/distancematrix/json?";
            List<String> locations = Arrays.asList("08091", "08049", "08094", "08014", "19104");
            String locationStr = "";


            locationStr = locations.get(0);
            for (int ii = 1; ii < locations.size(); ii++) {
                //space to +, and | as deliminator
                //locationStr += "%7C" + locations.get(ii);
                locationStr += "|" + locations.get(ii);
            }
            TextView t = (TextView) findViewById(R.id.textBox);
            locationStr = URLEncoder.encode(locationStr, "UTF-8");
            sURL += "origins=" + locationStr;
            sURL += "&destinations=" + locationStr;
            sURL += "&key=" + apiKey;
            //t.setText(GET(sURL));
            //sURL = "http://www.google.com/";
            new HttpAsyncTask().execute(sURL);
            //new HttpAsyncTask().executeOnExecutor(sURL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String GET(String url){
        InputStream inputStream = null;
        String result = "";
        try {

            // create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // make GET request to the given URL
            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // convert inputstream to string
            if(inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException{
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;
    }
    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }
    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return GET(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            TextView t = (TextView)findViewById(R.id.textBox);
            //t.setText(result);
            //t.append("test");
            try {
                int inf = Integer.MAX_VALUE;
                JSONObject json = new JSONObject(result);


                int numRows = json.getJSONArray("rows").length(); //one forecast per hour
                int[][] times = new int[numRows][numRows];
                String[] names = new String[numRows];
                for (int ii=0; ii < numRows; ii++) {
                    int numCols = json.getJSONArray("rows").getJSONObject(ii).getJSONArray("elements").length();
                    names[ii] = json.getJSONArray("destination_addresses").getString(ii);
                    for (int jj=0; jj < numCols; jj++) {
                        times[ii][jj] = json.getJSONArray("rows").getJSONObject(ii).getJSONArray("elements").getJSONObject(jj).getJSONObject("duration").getInt("value");
                        if (ii == jj) { times[ii][jj] = inf; }
                        t.append(times[ii][jj] + " ");
                    }
                    t.append("\n");
                }

                t.setText("");
                //begin finding route:
                routeUrl = "https://www.google.com/maps/dir/";
                int[] path = new int[numRows];
                int from = 0;
                routeUrl += names[from] + "/";
                for (int jj = 0; jj < numRows; jj++) {
                    times[jj][from] = inf;
                }
                int totalTime = 0;
                for (int ii = 1; ii < numRows; ii++) {
                    int minTime = inf;
                    int minCity = 0;
                    for (int to = 0; to < numRows; to++) {
                        if (times[from][to] < minTime) {
                            minTime = times[from][to];
                            minCity = to;
                        }
                    }
                    totalTime += minTime;
                    path[from] = minCity;
                    t.append("\n\nFrom: " + names[from] + "\nTo: " + names[minCity] + "\nTime: " + minTime/60 + " minutes");
                    routeUrl += names[minCity] + "/";

                    for (int jj = 0; jj < numRows; jj++) {
                        times[jj][minCity] = inf;
                    }
                    from = minCity;
                }


                Button button = (Button) findViewById(R.id.button);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(routeUrl));
                        //Thread.sleep(15000);
                        startActivity(intent);
                    }
                });




            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
