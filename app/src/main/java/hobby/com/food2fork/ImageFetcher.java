package hobby.com.food2fork;

/**
 * Used to fetch images in the background. It's used via ImageDispatcher which send the downloaded
 * images to the appropriate callbacls in the activity in which they were requested. Please see
 * ImageDispatcher for more info on how to use.
 *
 * @author  Nima Poulad
 * @version 1.0
 */

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class ImageFetcher extends AsyncTask<ImageDispatcher.imageTaskParams, Void, Bitmap> {
    private final String TAG = "IMAGE_FETCHER";

    private final OkHttpClient                        m_client           = new OkHttpClient();
    private Exception                                 m_exception;
    private ImageDispatcher.imageResultCallback       m_resultCB         = null;
    private boolean                                   m_fromMainAcitivty = false;
    private String                                    m_url              = null;

    private Bitmap run(String url) throws Exception {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try(Response response = m_client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Error code " + response);

            return (BitmapFactory.decodeStream(response.body().byteStream()));
        }
    }

    protected Bitmap doInBackground(ImageDispatcher.imageTaskParams... params) {
        Bitmap bitmap = null;
        if (params.length > 1) {
            Log.e(TAG, "Incorrect number of parameters received");
            return null;
        }

        m_resultCB         = params[0].callback;
        m_fromMainAcitivty = params[0].fromMainActivity;
        m_url              = params[0].url;

        try {
            bitmap = run(m_url);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            this.m_exception = e;
        }

        return bitmap;
    }

    protected void onPostExecute(Bitmap result) {
        if(this.m_exception instanceof IOException) {
            this.m_exception.printStackTrace();
            return;
        }

        m_resultCB.copy(result,m_fromMainAcitivty,m_url);
    }
}