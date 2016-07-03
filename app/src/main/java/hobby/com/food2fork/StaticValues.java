package hobby.com.food2fork;

/**
 * Created by npoulad on 6/29/2016.
 */
public final class StaticValues {
    private StaticValues() {
        // intentionally left empty (static class)
    }

    // Unsafe. The API key shouldn't be included in any Java files. This is
    // an experimental app. Otherwise the key would be stored safely elsewhere.
    public static final String KEY = "54ea8e4998a2b3a498c61e977e6fb4ad";

    public static final String SORT = "r";
    public static final String SEARCH_URL = "http://food2fork.com/api/search";
    public static final String RECIPE_URL = "http://food2fork.com/api/get";

    public static final String INTENT_RID       = "recipeID";
    public static final String INSTRUCTIONS_URL = "InstURL";
    public static final String ORIGINAL_URL     = "OrgURL";
}
