package net.benboeckel.tools.media.playlistedit;

import android.app.Activity;
import android.os.Bundle;

import net.benboeckel.tools.media.playlistedit.fragments.PlaylistFragment;

public class PlaylistEditActivity
        extends Activity
        implements PlaylistFragment.OnItemSelectedListener {
    /** Called when the activity is first created. */
    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
    }

    @Override
    public void
    onItemSelected(int category, int item) {
    }
}
