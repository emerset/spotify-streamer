package com.efarquharson.spotifystreamer;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;

/**
 * A placeholder fragment containing a simple view.
 */
public class TopTenTracksFragment extends Fragment {

    static final String LOG_TAG = TopTenTracksFragment.class.getSimpleName();
    String ArtistID = "";
    public TopTracksAdapter topTracksAdapter;
    public TopTenTracksFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // inflate layout
        View rootView = inflater.inflate(R.layout.fragment_top_ten_tracks, container, false);

        // check if intent extra was passed
        if (getActivity().getIntent().hasExtra("ArtistID")) {
            ArtistID = getActivity().getIntent().getExtras().getString("ArtistID");
        }

        // check if bundle was created
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("ArtistID")) {
                ArtistID = savedInstanceState.getString("ArtistID");
                // Call asynctask with artistID
                FetchSongClass songClass = new FetchSongClass();
                songClass.execute(ArtistID);
            }
        }

        // declare ArrayList
        ArrayList<ArrayMap<String, String>> topTracksArrayList = new ArrayList<>();
        // declare Custom ArrayAdapter
        topTracksAdapter = new TopTracksAdapter(
                // context --> fragment's parent activity
                getActivity(),
                // name of xml layout file
                R.layout.fragment_top_ten_tracks,
                // arraylist to get data
                topTracksArrayList
        );

        // Bind ListView to ArrayAdapter
        ListView listView = (ListView) rootView.findViewById(R.id.listviewTopTenTracks);
        listView.setAdapter(topTracksAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // Get data
                // Create Intent
                // Add data to intent
                // Launch intent
            }
        });
        return rootView;
    } // End of onCreateView

    @Override
    public void onStart() {
        super.onStart();
        // declare AsyncTask
        FetchSongClass songClass = new FetchSongClass();
        // add input variables to asynctask
        songClass.execute(ArtistID);

    }

    // Save Artist ID to bundle
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("ArtistID", ArtistID);
    }

    /* Custom Asynctask takes ArtistID,
        parses JSON output,
        populates top ten tracks list
     */
    public class FetchSongClass extends AsyncTask<String, Void, ArrayMap[]> {

        @Override
        protected ArrayMap[] doInBackground(String... params) {
            // Verify input
            if (params.length == 0) {
                return null;
            }
            // open data connection
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String topTenTracksJsonStr = null;

            String countryCode = "CA";
            String artistId = params[0];

            try {
                final String COUNTRY_PARAM = "country";

                // Use UriBuilder to build URL
                Uri.Builder builder = new Uri.Builder();
                builder.scheme("https")
                        .authority("api.spotify.com")
                        .appendPath("v1")
                        .appendPath("artists")
                        .appendPath(artistId)
                        .appendPath("top-tracks")
                        .appendQueryParameter(COUNTRY_PARAM, countryCode);

                // Build URL query
                URL url = new URL(builder.build().toString());

                // Create request to Spotify, open connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Add a newline for debugging
                    buffer.append(line + "\n");
                }
                if (buffer.length() == 0) {
                    // Empty stream. No point in parsing
                    return null;
                }
                // load JSON data to String
                topTenTracksJsonStr = buffer.toString();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Connection Error ", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream: ", e);
                    }
                }
            }

            try {
                return getTrackDataFromJson(topTenTracksJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        } // End of doInBackground

        @Override
        protected void onPostExecute(ArrayMap[] topTracksMapArray) {
            if (topTracksMapArray != null) {
                topTracksAdapter.clear();
                for (ArrayMap topTrackMap : topTracksMapArray) {
                    topTracksAdapter.add(topTrackMap);
                }
            }
        }

        /**
         * Get JSON String and parse it into an ArrayMap<K,V>[]
         */
        private ArrayMap[] getTrackDataFromJson(String topTenJsonStr) throws JSONException {
            ArrayMap[] parsedJsonArray = new ArrayMap[10];

            JSONObject topTenJsonObj = new JSONObject(topTenJsonStr);
            JSONArray tracksArray = topTenJsonObj.getJSONArray("tracks");
            for (int i=0; i < tracksArray.length(); i++) {
                String trackName = tracksArray.getJSONObject(i).getString("name");
                String albumName = tracksArray.getJSONObject(i).getJSONObject("album").getString("name");
                String previewURL = tracksArray.getJSONObject(i).getString("preview_url");
                String largeImageURL = "";
                String smallImageURL = "";
                JSONArray listImages;

                // image URLs
                if (tracksArray.getJSONObject(i).getJSONObject("album").has("images")) {
                    listImages = tracksArray.getJSONObject(i).getJSONObject("album").getJSONArray("images");
                    for (int count=0; count<listImages.length(); count++) {
                        int imageHeight = listImages.getJSONObject(count).getInt("height");
                        // set largeImageURL
                        if (imageHeight <= 640 || (count+1 > listImages.length() || (listImages.getJSONObject(count + 1).getInt("height") < 640))) {
                            if (largeImageURL.isEmpty()) {
                                largeImageURL = listImages.getJSONObject(count).getString("url");
                            }
                        }
                        // set smallImageURL
                        if (imageHeight <= 200 || (count+1 > listImages.length() || (listImages.getJSONObject(count + 1).getInt("height") < 200))) {
                            if (smallImageURL.isEmpty()) {
                                smallImageURL = listImages.getJSONObject(count).getString("url");
                            }
                        }
                    }
                }

                ArrayMap<String, String> trackInfo = new ArrayMap<>();
                trackInfo.put("trackName", trackName);
                trackInfo.put("albumName", albumName);
                trackInfo.put("largeImageURL", largeImageURL);
                trackInfo.put("smallImageURL", smallImageURL);
                trackInfo.put("previewURL", previewURL);
                parsedJsonArray[i] = trackInfo;
            }
            return parsedJsonArray;
        }

    }

    // Define Custom ArrayAdapter
    public class TopTracksAdapter extends ArrayAdapter<ArrayMap<String, String>> {
        // default constructor
        public TopTracksAdapter(
                Context context,
                int layoutID,
                List<ArrayMap<String, String>> listItems) {
            super(context, layoutID, listItems);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View view = convertView;
            if (view == null) {
                LayoutInflater layoutInflater = LayoutInflater.from(getContext());
                view = layoutInflater.inflate(R.layout.item_top_tracks_list, null);
            }

            ArrayMap<String, String> item = getItem(position);

            if (item != null) {
                ImageView albumArt = (ImageView) view.findViewById(R.id.topTracksItemPic);
                TextView trackName = (TextView) view.findViewById(R.id.track_name);
                TextView albumName = (TextView) view.findViewById(R.id.album_name);

                // load album art
                if (item.containsKey("smallImageURL") && item.get("smallImageURL").length() > 0) {
                    Picasso.with(getContext()).load(item.get("smallImageURL")).into(albumArt);
                }

                trackName.setText(item.get("trackName"));
                albumName.setText(item.get("albumName"));
            }
            return view;
        }
    }
}
