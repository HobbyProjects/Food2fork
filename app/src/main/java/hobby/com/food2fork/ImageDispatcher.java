package hobby.com.food2fork;

/**
 * The ImageDispatcher class sits between MainActivity, DetailedViewActivity and ImageFetcher. When
 * ImageFetcher has downloaded an image, it will dispatch the bitmap to the appropriate activity
 * based on which activity requested it. As a result, ImageFetcher can be shared between the two
 * activities.
 *
 * Proper user:
 *             ImageDispatcher imageDispatcher = new ImageDispatcher(image_url,
 *                                                                   main_activity_context,
 *                                                                   detailedViewActivity_context);
 *             imageDispatcher.execute();
 *
 * @author  Nima Poulad
 * @version 1.0
 */

import android.graphics.Bitmap;
import android.util.Log;

public class ImageDispatcher {
    private final String TAG   = "IMAGE_DISPATCH";

    private String                                     m_url        = null;
    private boolean                                    m_fromMain;
    private MainActivity.searchResultCallback          m_mainActRef = null;
    private DetailedViewActivity.recipeResultCallback  m_detActRef  = null;

    public ImageDispatcher(String url, MainActivity.searchResultCallback Ma,
                           DetailedViewActivity.recipeResultCallback Dva) {
        m_url = url;

        if(Ma != null && Dva == null) {
            m_fromMain = true;
            m_mainActRef = Ma;
        } else if (Ma == null && Dva != null){
            m_fromMain = false;
            m_detActRef = Dva;
        } else {
            Log.e(TAG, "Wrong context was passed!");
        }
    }

    public interface imageResultCallback {
        void copy(Bitmap result, boolean fromMainActivity, String url);
    }

    // Used to pass the parameters and the callback to async tasks
    public class imageTaskParams {
        public boolean fromMainActivity;
        public String url;
        public imageResultCallback callback;

        imageTaskParams(String url, imageResultCallback callback, boolean fromMainActivity) {
            this.url = url;
            this.callback = callback;
            this.fromMainActivity = fromMainActivity;
        }
    }

    // Callback for copying image get results
    public imageResultCallback imageResult = new imageResultCallback() {
        @Override
        public void copy(Bitmap result, boolean fromMainActivity, String url) {
            if(!fromMainActivity) {
                if(m_detActRef != null) {
                    m_detActRef.onImageIsReady(result, url);
                }
            } else {
                if(m_mainActRef != null) {
                    m_mainActRef.onImageIsReady(result, url);
                }
            }
        }
    };

    public void execute () {
        if (m_url == null) {
            return;
        }

        imageTaskParams param = new imageTaskParams(m_url, imageResult, m_fromMain );
        ImageFetcher imageFetcher = new ImageFetcher();
        imageFetcher.execute(param);

    }
}
