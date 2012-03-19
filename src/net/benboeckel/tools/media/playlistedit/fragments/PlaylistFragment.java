package net.benboeckel.tools.media.playlistedit.fragments;

import java.util.HashMap;
import java.util.Map;

import net.benboeckel.tools.media.playlistedit.R;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Audio.Albums;
import android.provider.MediaStore.Audio.Artists;
import android.provider.MediaStore.Audio.Genres;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Audio.Playlists;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class PlaylistFragment
        extends ListFragment
        implements TabListener {
    private OnItemSelectedListener mListener;
    private ContentResolver mResolver;
    private Map<Category, Cursor> mCursors;
    private boolean mDualFragments = false;

    private int mLastCategory = -1;
    private int mCategory = 0;
    private int mListPosition = 0;

    private static final String mCategoryState = "category";
    private static final String mListPositionState = "list_position";

    public static enum Category {
        CAT_PLAYLIST,
        CAT_ARTIST,
        CAT_ALBUM,
        CAT_SONG,
        CAT_GENRE
    }

    public boolean
    isMultiChoiceCategory(Category tab) {
        boolean multichoice = false;

        switch (tab) {
        case CAT_ARTIST:
        case CAT_ALBUM:
        case CAT_SONG:
            multichoice = true;
            break;
        default:
            break;
        }

        return multichoice;
    }

    /**
     * Container Activity must implement this interface and we ensure
     * that it does during the onAttach() callback
     */
    public interface OnItemSelectedListener {
        public void
        onItemSelected(Category category, int position);
    }

    public void
    populateCategory(int category) {
        Category cat = Category.values()[category];

        int layoutId = 0;
        String[] viewColumns = null;
        int[] viewIds = null;

        mLastCategory = mCategory;
        mCategory = category;

        Cursor cursor = null;

        if (mCursors.containsKey(cat)) {
            cursor = mCursors.get(cat);

            cursor.requery();

            if (mLastCategory == mCategory) {
                return;
            }
        }

        Uri uri = null;
        String[] projection = null;
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = null;

        switch (cat) {
        case CAT_PLAYLIST:
            uri = Playlists.EXTERNAL_CONTENT_URI;
            projection = new String[] {
                    Playlists._ID,
                    Playlists.NAME
            };
            sortOrder = Playlists.DEFAULT_SORT_ORDER + " ASC";

            layoutId = R.layout.playlist_item;
            viewColumns = new String[] {
                    Playlists.NAME
            };
            viewIds = new int[] {
                    R.id.playlist_name
            };
            break;
        case CAT_ARTIST:
            uri = Artists.EXTERNAL_CONTENT_URI;
            projection = new String[] {
                    Artists._ID,
                    Artists.ARTIST,
                    Artists.NUMBER_OF_ALBUMS,
                    Artists.NUMBER_OF_TRACKS
            };
            sortOrder = Artists.DEFAULT_SORT_ORDER + " ASC";

            layoutId = R.layout.artist_item;
            viewColumns = new String[] {
                    Artists.ARTIST,
                    Artists.NUMBER_OF_ALBUMS,
                    Artists.NUMBER_OF_TRACKS
            };
            viewIds = new int[] {
                    R.id.artist_name,
                    R.id.artist_album_count,
                    R.id.artist_track_count
            };
            break;
        case CAT_ALBUM:
            uri = Albums.EXTERNAL_CONTENT_URI;
            projection = new String[] {
                    Albums._ID,
                    Albums.ALBUM,
                    Albums.ARTIST,
                    Albums.NUMBER_OF_SONGS,
                    Albums.ALBUM_ART
            };
            sortOrder = Albums.DEFAULT_SORT_ORDER + " ASC";

            layoutId = R.layout.album_item;
            viewColumns = new String[] {
                    Albums.ALBUM,
                    Albums.ARTIST,
                    Albums.NUMBER_OF_SONGS,
                    Albums.ALBUM_ART
            };
            viewIds = new int[] {
                    R.id.album_name,
                    R.id.album_artist,
                    R.id.album_track_count,
                    R.id.album_art
            };
            break;
        case CAT_SONG:
            uri = Media.EXTERNAL_CONTENT_URI;
            projection = new String[] {
                    Media._ID,
                    Media.TITLE,
                    Media.ALBUM,
                    Media.ARTIST
            };
            selection = Media.IS_MUSIC + " = 1";
            sortOrder = Media.DEFAULT_SORT_ORDER + " ASC";

            layoutId = R.layout.song_item;
            viewColumns = new String[] {
                    Media.TITLE,
                    Media.ALBUM,
                    Media.ARTIST
            };
            viewIds = new int[] {
                    R.id.song_name,
                    R.id.song_album,
                    R.id.song_artist
            };
            break;
        case CAT_GENRE:
            uri = Genres.EXTERNAL_CONTENT_URI;
            projection = new String[] {
                    Genres._ID,
                    Genres.NAME
            };
            sortOrder = Genres.DEFAULT_SORT_ORDER + " ASC";

            layoutId = R.layout.genre_item;
            viewColumns = new String[] {
                    Genres.NAME
            };
            viewIds = new int[] {
                    R.id.genre_name
            };
            break;
        default:
            break;
        }

        if (cursor == null) {
            try {
                cursor = mResolver.query(uri, projection, selection, selectionArgs, sortOrder);
            } catch (Throwable t) {
                android.util.Log.e(getTag(), "Failed to query: ", t);
                return;
            }

            mCursors.put(cat, cursor);
        }

        if (cursor == null) {
            return;
        } else if (cursor.getCount() < 1) {
            setListAdapter(new ArrayAdapter<String>(
                    getActivity().getApplicationContext(),
                    R.layout.error_item,
                    new String[] {}));

            return;
        } else {
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                    getActivity().getApplicationContext(),
                    layoutId,
                    cursor,
                    viewColumns,
                    viewIds,
                    0);

            setListAdapter(adapter);
        }

        if (mDualFragments) {
            ListView lv = getListView();

            if (isMultiChoiceCategory(cat)) {
                lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            } else {
                lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            }

            if (cat == Category.CAT_PLAYLIST) {
                lv.setOnItemLongClickListener(new OnItemLongClickListener() {
                    @Override
                    public boolean
                    onItemLongClick(AdapterView<?> av, View v, int position, long id) {
                        // TODO: Pop up rename/copy/delete dialog.
                        return true;
                    }
                });
            }
        }
    }

    public void
    selectPosition(int position) {
        mListener.onItemSelected(Category.values()[mCategory], position);
        mListPosition = position;
    }

    @Override
    public void
    onAttach(Activity activity) {
        super.onAttach(activity);
        // Check that the container activity has implemented the callback interface
        try {
            mListener = (OnItemSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnItemSelectedListener");
        }

        mResolver = activity.getContentResolver();
        mCursors = new HashMap<Category, Cursor>();
    }

    @Override
    public void
    onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getActivity().findViewById(R.id.selection_layout);
        if (view != null) {
            mDualFragments = true;
        }

        ActionBar bar = getActivity().getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        bar.addTab(bar.newTab()
                .setText(R.string.playlist_tab_label)
                .setTabListener(this), Category.CAT_PLAYLIST.ordinal());
        bar.addTab(bar.newTab()
                .setText(R.string.artist_tab_label)
                .setTabListener(this), Category.CAT_ARTIST.ordinal());
        bar.addTab(bar.newTab()
                .setText(R.string.album_tab_label)
                .setTabListener(this), Category.CAT_ALBUM.ordinal());
        bar.addTab(bar.newTab()
                .setText(R.string.song_tab_label)
                .setTabListener(this), Category.CAT_SONG.ordinal());
        bar.addTab(bar.newTab()
                .setText(R.string.genre_tab_label)
                .setTabListener(this), Category.CAT_GENRE.ordinal());

        if (savedInstanceState != null) {
            mCategory = savedInstanceState.getInt(mCategoryState);
            mListPosition = savedInstanceState.getInt(mListPositionState);

            bar.selectTab(bar.getTabAt(mCategory));
        }

        populateCategory(mCategory);

        ListView lv = getListView();
        // Improve scrolling performance.
        lv.setCacheColorHint(Color.TRANSPARENT);
    }

    @Override
    public void
    onListItemClick(ListView lv, View v, int position, long id) {
        mListener.onItemSelected(Category.values()[mCategory], position);
        mListPosition = position;
    }

    @Override
    public void
    onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(mCategoryState, mCategory);
        outState.putInt(mListPositionState, mListPosition);
    }

    @Override
    public void
    onTabSelected(Tab tab, FragmentTransaction ft) {
        PlaylistFragment fragment = (PlaylistFragment) getFragmentManager()
                .findFragmentById(R.id.playlist_fragment);
        fragment.populateCategory(tab.getPosition());

        if (mDualFragments) {
            fragment.selectPosition(0);
        }
    }

    @Override
    public void
    onTabReselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void
    onTabUnselected(Tab tab, FragmentTransaction ft) {
    }
}
