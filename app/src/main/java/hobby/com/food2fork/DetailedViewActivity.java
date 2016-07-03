package hobby.com.food2fork;

/**
 * The DetailedView of the app. The activity has an async approach in relationship to the server.
 * The activity is notified of data availability via recipeResultCallback. The recipeResult has
 * two methods:
 *
 * onImageIsReady - which is invoked when an image is downloaded and ready to be consumed
 * copy - which is invoked when there is a new set of recipe data available for copying over.
 *
 * @author  Nima Poulad
 * @version 1.0
 */

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DetailedViewActivity extends AppCompatActivity {
    private final String      TAG             = "DV_ACTIVITY";
    private String            m_rID           = null;
    private recipeDetails     m_recipe        = null;
    private TextView          m_rankTB        = null;
    private TextView          m_publisherTB   = null;
    private ListView          m_ingredientsLV = null;
    private ImageView         m_reicpeIV      = null;
    private Button            m_viewOrignalBtn= null;
    private Button            m_instructionBtn= null;


    public interface recipeResultCallback {
        void copy(JSONObject result);
        void onImageIsReady(Bitmap bitmap, String url);
    }

    public class recipeDetails {
        public recipeDetails() {
            ingredients   = new ArrayList<String>();
            publisher     = null;
            social_rank   = null;
            f2f_url       = null;
            publisher_url = null;
            title         = null;
            source_url    = null;
            recipe_id     = null;
            image_url     = null;
        }

        String            publisher;
        String            social_rank;
        String            f2f_url;
        String            publisher_url;
        String            title;
        String            source_url;
        String            recipe_id;
        String            image_url;
        ArrayList<String> ingredients;
    }


    // Used to pass the parameters and the callback to async tasks
    public class recipeTaskParams {
        public  String               recipeID;
        public  recipeResultCallback callback;

        recipeTaskParams(String recipeID, recipeResultCallback callback) {
            this.recipeID = recipeID;
            this.callback = callback;
        }
    }

    // Callback for copying recipe get results
    public recipeResultCallback recipeResult = new recipeResultCallback() {
        @Override
        public void onImageIsReady(Bitmap bitmap, String url){
            m_reicpeIV.setImageBitmap(bitmap);
        }

        @Override
        public void copy(JSONObject result) {
            try {
                synchronized (m_recipe) {
                    m_recipe.f2f_url = result.getString("f2f_url");
                    m_recipe.image_url = result.getString("image_url");
                    m_recipe.publisher = result.getString("publisher");
                    m_recipe.publisher_url = result.getString("publisher_url");
                    m_recipe.social_rank = result.getString("social_rank");
                    m_recipe.title = result.getString("title");
                    m_recipe.source_url = result.getString("source_url");
                    m_recipe.recipe_id = result.getString("recipe_id");

                    JSONArray ingreTemp = result.getJSONArray("ingredients");

                    if(m_recipe.ingredients != null) {
                        m_recipe.ingredients.clear();
                    } else {
                        Log.e(TAG, "ingredient list was null!!");
                        return;
                    }

                    for(int i = 0; i < ingreTemp.length(); i++) {
                        m_recipe.ingredients.add(ingreTemp.getString(i));
                    }
                }

                // update the publisher and rank
                if(m_publisherTB != null) {
                    m_publisherTB.setText(m_recipe.publisher);
                }

                if(m_rankTB != null) {
                    m_rankTB.setText(m_recipe.social_rank.substring(0,5));
                }

                // fetch the image and display
                ImageDispatcher imageDispatcher = new ImageDispatcher(m_recipe.image_url, null, this);
                imageDispatcher.execute();

                // notify we have new data
                if(m_ingredientsLV != null) {
                    ((BaseAdapter) m_ingredientsLV.getAdapter()).notifyDataSetChanged();
                } else {
                    Log.e(TAG,"listView was null!");
                }

                Log.i(TAG, "Recipe copied over successfully");
            } catch (JSONException e) {
                Log.e(TAG, "Problem with copying search results over. See below");
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_view);

        // the recipe ID represented in this detailed view
        m_rID = getIntent().getStringExtra(StaticValues.INTENT_RID);

        // assign the UI elements
        m_rankTB        = (TextView) findViewById(R.id.RankTB);
        m_publisherTB   = (TextView) findViewById(R.id.publisherTB);
        m_ingredientsLV = (ListView) findViewById(R.id.ingre_list);
        m_reicpeIV      = (ImageView)findViewById(R.id.image);
        m_viewOrignalBtn= (Button)   findViewById(R.id.viewOrgBtn);
        m_instructionBtn= (Button)   findViewById(R.id.instructionsBtn);

        m_instructionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // open the instructions in webView
                Intent intent = new Intent(getApplicationContext(), WebViewActivity.class);
                intent.putExtra(StaticValues.INSTRUCTIONS_URL, m_recipe.source_url);
                startActivity(intent);
            }
        });

        m_viewOrignalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // open the original recipe in webView
                Intent intent = new Intent(getApplicationContext(), WebViewActivity.class);
                intent.putExtra(StaticValues.ORIGINAL_URL, m_recipe.publisher_url);
                startActivity(intent);
            }
        });

        // create a new recipe object for the activity
        if (m_recipe == null) {
            m_recipe = new recipeDetails();
        }

        // attach the adapter to the activity
        m_ingredientsLV.setAdapter(new ListViewAdapter(getApplicationContext(),
                R.layout.detail_view_listitem, m_recipe.ingredients ));

        // fetch the recipe details
        fetchRecipeDetails(m_rID);
    }

    /*
     * This method runs the query in the background. The result will be available via recipeResults CB
     * @param rID The recipe details to be fetched*/
    private void fetchRecipeDetails(String rID) {
        recipeTaskParams rParams = new recipeTaskParams(rID, recipeResult);
        RecipeFetcher rFetch = new RecipeFetcher();
        rFetch.execute(rParams);
    }

    private class ListViewAdapter extends ArrayAdapter<String> {
        private int layout;

        public ListViewAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
            layout = resource;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder mainViewHolder;
            if(convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(layout, parent, false);

                ViewHolder viewHolder = new ViewHolder();
                viewHolder.title = (TextView) convertView.findViewById(R.id.list_item_ingredient);

                if(getItem(position) != null){
                    viewHolder.title.setText(getItem(position));
                }

                convertView.setTag(viewHolder);
            }
            else {
                mainViewHolder = (ViewHolder)convertView.getTag();
                mainViewHolder.title.setText(getItem(position));
            }
            return convertView;
        }
    }

    // used in listview
    public class ViewHolder {
        TextView title;
    }
}
