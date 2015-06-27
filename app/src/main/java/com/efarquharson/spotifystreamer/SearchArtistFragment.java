package com.efarquharson.spotifystreamer;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;

/**
 * Created by Emerset on 18/06/2015.
 *
 * instructions at
 * https://docs.google.com/document/u/1/d/1v4Kv5lSd8-4cs0BW6F24ccA3c1-KDQZG3EV49CUHQys/pub
 */

public class SearchArtistFragment extends Fragment {

    static final String LOG_TAG = SearchArtistFragment.class.getSimpleName();

    public SearchArtistFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_artist, container, false);

        // Set up listener for text input
        final EditText searchArtist = (EditText) rootView.findViewById(R.id.searchArtist);
        searchArtist.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // call Asynctask with query String from searchArtist
                    String inputSearch = v.getText().toString();
                    FetchArtistClass artistClass = new FetchArtistClass();
                    artistClass.execute(inputSearch);
                    // end of AsyncTask call
                    handled = true;
                    // hide soft keyboard
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(searchArtist.getWindowToken(), 0);
                }
                return handled;
            }
        });
        return rootView;
    } // End of onCreateView

    // Create Custom AsyncTask
    public class FetchArtistClass extends AsyncTask<String, Void, ArtistsPager> {
        private final String LOG_TAG = FetchArtistClass.class.getSimpleName();

        @Override
        protected ArtistsPager doInBackground(String... params) {
            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();
            ArtistsPager results = spotify.searchArtists(params[0]);
            return results;
        }

        @Override
        protected void onPostExecute(ArtistsPager pager) {
            // Call custom adapter to populate ListView
            SearchArtistAdapter mArtistAdapter = new SearchArtistAdapter(
                    // context
                    getActivity(),
                    // name of xml layout file
                    R.layout.item_search_list,
                    // arrayList of Artist objects to get data
                    pager.artists.items
            );

            // Bind ListView to ArrayAdapter
            ListView listView = (ListView) getActivity().findViewById(R.id.listArtists);
            listView.setAdapter(mArtistAdapter);
        }
    } // End of Custom AsyncTask

    // Create Custom Adapter for ListView
    public class SearchArtistAdapter extends ArrayAdapter<Artist> {
        private final String LOG_TAG = SearchArtistAdapter.class.getSimpleName();

        public SearchArtistAdapter(Context context, int textViewResourceId, List<Artist> artists) {
            super(context, textViewResourceId, artists);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                v = inflater.inflate(R.layout.item_search_list, null);
            }

            Artist artist = getItem(position);

            if (artist != null) {
                ImageView artistImage = (ImageView) v.findViewById(R.id.searchListItemPic);
                TextView artistName = (TextView) v.findViewById(R.id.searchListItemName);

                // load image
                if (artistImage != null) {
                    if (!artist.images.isEmpty()) {
                        Picasso.with(getContext()).load(artist.images.get(0).url).into(artistImage);
                    } // else default image will load
                }

                // load artist name
                if (artistName != null) {
                    artistName.setText(artist.name);
                }
            } else {
                Log.i(LOG_TAG, "Error: Adapter returned null Artist object");
            }
            return v;
        }
    } // End of Custom Adapter
}