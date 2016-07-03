package hobby.com.food2fork;

/**
 * The SearchFetcher class implements an async approach to
 * searching the recipes from the server. This class has a builtin feature for reading fake data from
 * a txt file located on /sdcard/. This approach was used for testing the class before integration.
 * Please see the WORKAROUND tag in onPostExecute to see a sample use.
 *
 * @author  Nima Poulad
 * @version 1.0
 */

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.Request;
import okhttp3.RequestBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SearchFetcher extends AsyncTask <MainActivity.searchTaskParams, Void, JSONObject> {
    private final String TAG = "SEARCH_FETCHER";

    private final OkHttpClient                m_client = new OkHttpClient();
    private Exception                         m_exception;
    private MainActivity.searchResultCallback m_resultCB;

    private JSONObject run(String query, String page) throws Exception {
        RequestBody formBody = new FormBody.Builder()
                .add("key", StaticValues.KEY)
                .add("q", query)
                .add("sort", StaticValues.SORT)
                .add("page", page)
                .build();
        Request request = new Request.Builder()
                .url(StaticValues.SEARCH_URL)
                .post(formBody)
                .build();

        try(Response response = m_client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Error code " + response);

            return (new JSONObject(response.body().string()));
        }
    }

    @Override
    protected JSONObject doInBackground(MainActivity.searchTaskParams... searchParams) {
        if(searchParams.length > 1) {
            Log.e(TAG, "Incorrect number of parameters received");
            return null;
        }

        // save the callback for onPostExecute
        m_resultCB = searchParams[0].callback;

        try {
            return (run(searchParams[0].query, searchParams[0].page));
        } catch (Exception e) {
            this.m_exception = e;
        }

        return null;
    }

    @Override
    protected void onPostExecute(JSONObject responseObj) {
        if(this.m_exception instanceof IOException) {
            this.m_exception.printStackTrace();

            // WORKAROUND: try to read data from a text file since the server might be down
            TestDataReader dataReader = new TestDataReader("data.txt");
            String testData = dataReader.read();

            if (testData != null) {
                try {
                    responseObj = new JSONObject(testData);
                    Log.i(TAG, "Fake data is being used...");
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }

        // make a callback to the MainActivity with new data
        if (responseObj != null) {
            try {
                JSONArray recipes = responseObj.getJSONArray("recipes");
                int count = responseObj.getInt("count");

                if(count != recipes.length()) {
                    Log.e(TAG, "Incorrect count: " + count +
                            " vs recipes length: " + recipes.length());
                }

                m_resultCB.copy(recipes);
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
        }
    }
}
