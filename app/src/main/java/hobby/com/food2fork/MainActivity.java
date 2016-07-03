package hobby.com.food2fork;

/**
 * The MainActivity of the app. The activity has an async approach in relationship to the server.
 * The activity is notified of data availability via searchResultCallback. The searchcallback has
 * two methods:
 *
 * onImageIsReady - which is invoked when an image is downloaded and ready to be consumed
 * copy - which is invoked when there is a new set of search results available for copying over.
 *
 * @author  Nima Poulad
 * @version 1.0
 */

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final String      TAG             = "MAIN_ACTIVITY";
    private ArrayList<recipe> m_searchResults = new ArrayList<recipe>();
    private ListView          m_listView      = null;
    private Button            m_searchBtn     = null;
    private Button            m_nextPage      = null;
    private Button            m_previousPage  = null;
    private TextView          m_searchBox     = null;
    private int               m_page          = 1;
    private String            m_query         = null;

    public interface searchResultCallback {
        void copy(JSONArray result);
        void onImageIsReady(Bitmap bitmap, String url);
    }

    public class recipe {
        String publisher;
        String social_rank;
        String f2f_url;
        String publisher_url;
        String title;
        String source_url;
        String recipe_id;
        String image_url;
        Bitmap thumbnail;
    }

    // Used to pass the parameters and the callback to async tasks
    public class searchTaskParams {
        public  String               query;
        public  String               page;
        public  searchResultCallback callback;

        searchTaskParams(String query, String page, searchResultCallback callback) {
            this.query = query;
            this.page = page;
            this.callback = callback;
        }
    }

    // Callback for copying new search results
    public searchResultCallback searchResults = new searchResultCallback() {
        @Override
        public void onImageIsReady(Bitmap bitmap, String url){
            synchronized (m_searchResults){
                // find the entry in the list and update its thumbnail
                for(int i = 0; i < m_searchResults.size(); i++) {
                    if(m_searchResults.get(i).image_url.equals(url)) {
                        m_searchResults.get(i).thumbnail = bitmap;
                        if(m_listView != null) {
                            ((BaseAdapter) m_listView.getAdapter()).notifyDataSetChanged();
                        } else {
                            Log.e(TAG,"listView was null!");
                        }
                        return;
                    }
                }
            }
        }

        @Override
        public void copy(JSONArray result) {
            // lock m_searchResults before manipulating its members
            synchronized (m_searchResults) {
                // clean the old list first
                m_searchResults.clear();

                for(int i = 0; i < result.length(); i++) {
                    recipe tempRecipe = new recipe();
                    try {
                        tempRecipe.f2f_url       = result.getJSONObject(i).getString("f2f_url");
                        tempRecipe.image_url     = result.getJSONObject(i).getString("image_url");
                        tempRecipe.publisher     = result.getJSONObject(i).getString("publisher");
                        tempRecipe.publisher_url = result.getJSONObject(i).getString("publisher_url");
                        tempRecipe.social_rank   = result.getJSONObject(i).getString("social_rank");
                        tempRecipe.title         = result.getJSONObject(i).getString("title");
                        tempRecipe.source_url    = result.getJSONObject(i).getString("source_url");
                        tempRecipe.recipe_id     = result.getJSONObject(i).getString("recipe_id");
                        tempRecipe.thumbnail     = null;
                        m_searchResults.add(tempRecipe);

                        // try to fetch the thumbnail in the background
                        ImageDispatcher imageDispatcher = new ImageDispatcher(tempRecipe.image_url, this, null);
                        imageDispatcher.execute();

                    } catch (JSONException e) {
                        Log.e(TAG,"Problem with parsing the search results. See below");
                        e.printStackTrace();
                        return;
                    }
                }
            }
            // notify we have new data
            if(m_listView != null) {
                ((BaseAdapter) m_listView.getAdapter()).notifyDataSetChanged();
                Toast.makeText(MainActivity.this, "Page: " + m_page, Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG,"listView was null!");
            }

            Log.i(TAG, "Data copied over successfully");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_listView     = (ListView) findViewById(R.id.listView);
        m_searchBtn    = (Button)   findViewById(R.id.searchButton);
        m_nextPage     = (Button)   findViewById(R.id.nextPageButton);
        m_previousPage = (Button)   findViewById(R.id.prevPageButton);
        m_searchBox    = (EditText) findViewById(R.id.searchBox);

        // attach the adapter and the source data to the ListView
        m_listView.setAdapter(new ListViewAdapter(getApplicationContext(), R.layout.list_item, m_searchResults ));

        // if an item in the listview is clicked, open it in the detailed view
        m_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // launch the detailed activity from here
                Intent intent = new Intent(getApplicationContext(), DetailedViewActivity.class);
                intent.putExtra(StaticValues.INTENT_RID, m_searchResults.get(i).recipe_id);
                startActivity(intent);
            }
        });

        // search button action
        m_searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // run the query using SearchFetcher class
                m_query = m_searchBox.getText().toString();
                m_page = 1;

                runQuery(m_query, m_page);
            }
        });

        // increment the page and rerun the query
        m_nextPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // increment the page number
                synchronized (m_searchResults) {
                    if(m_searchResults.size() == 30) {
                        m_page ++;
                        runQuery(m_query, m_page);
                    } else {
                        Toast.makeText(MainActivity.this, "No more pages...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // decrement the page and rerun the query
        m_previousPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (m_page > 1) {
                    m_page --;
                    runQuery(m_query, m_page);
                } else {
                    Toast.makeText(MainActivity.this, "No previous pages...", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /*
    * This method runs the query in the background. The result will be available via searchResults CB
    * @param query This is the query to run
    * @param page This is the page number to be fetched*/
    private void runQuery (String query, int page) {
        if (query != null && !query.equals("Ingredients")) {
            searchTaskParams params = new searchTaskParams(query, Integer.toString(page), searchResults);

            SearchFetcher fetch = new SearchFetcher();
            fetch.execute(params);
        }
    }

    // adapter used to handle the populating and scrolling on the ListView of the result
    private class ListViewAdapter extends ArrayAdapter<recipe> {
        private int layout;

        public ListViewAdapter(Context context, int resource, List<recipe> objects) {
            super(context, resource, objects);
            layout = resource;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder mainViewHolder;

            // when the list is first created
            if(convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(layout, parent, false);

                ViewHolder viewHolder = new ViewHolder();
                viewHolder.thumbnail = (ImageView) convertView.findViewById(R.id.list_item_thumbnail);
                viewHolder.title = (TextView) convertView.findViewById(R.id.list_item_text);

                if(getItem(position).title != null) {
                    viewHolder.title.setText(getItem(position).title);
                }

                if(getItem(position).thumbnail != null) {
                    viewHolder.thumbnail.setImageBitmap(getItem(position).thumbnail);
                }
                convertView.setTag(viewHolder);
            }
            else { // items need to be updated
                mainViewHolder = (ViewHolder)convertView.getTag();
                mainViewHolder.title.setText(getItem(position).title);
                if(getItem(position).thumbnail != null) {
                    mainViewHolder.thumbnail.setImageBitmap(getItem(position).thumbnail);
                }
            }
            return convertView;
        }
    }

    // used in ListView
    public class ViewHolder {
        ImageView thumbnail;
        TextView title;
    }
}
