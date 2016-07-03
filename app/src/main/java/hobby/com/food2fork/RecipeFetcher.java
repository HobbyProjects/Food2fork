package hobby.com.food2fork;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * The RecipeFetcher class implements an async approach to
 * fetch the recipe data from the server. This class has a builtin feature for reading fake data from
 * a txt file located on /sdcard/. This approach was used for testing the class before integration.
 * Please see the WORKAROUND tag in onPostExecute to see a sample use.
 *
 * @author  Nima Poulad
 * @version 1.0
 */

public class RecipeFetcher extends AsyncTask<DetailedViewActivity.recipeTaskParams, Void, JSONObject> {
    private final String TAG = "RECIPE_FETCHER";

    private final OkHttpClient                        m_client = new OkHttpClient();
    private Exception                                 m_exception;
    private DetailedViewActivity.recipeResultCallback m_resultCB;

    private JSONObject run(String rID) throws Exception {
        RequestBody formBody = new FormBody.Builder()
                .add("key", StaticValues.KEY)
                .add("rId", rID)
                .build();
        Request request = new Request.Builder()
                .url(StaticValues.RECIPE_URL)
                .post(formBody)
                .build();

        try(Response response = m_client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Error code " + response);

            return (new JSONObject(response.body().string()));
        }
    }

    @Override
    protected JSONObject doInBackground(DetailedViewActivity.recipeTaskParams... recipeParams) {
        if(recipeParams.length > 1) {
            Log.e(TAG, "Incorrect number of parameters received");
            return null;
        }

        // save the callback for onPostExecute
        m_resultCB = recipeParams[0].callback;

        try {
            return (run(recipeParams[0].recipeID));
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
            TestDataReader dataReader = new TestDataReader("recipe.txt");
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

        // make a callback to the DetailedViewActivity with new data
        if (responseObj != null) {
            try {
                JSONObject recipe = responseObj.getJSONObject("recipe");
                m_resultCB.copy(recipe);
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
        }
    }
}
