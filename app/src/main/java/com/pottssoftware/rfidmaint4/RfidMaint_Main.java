package com.pottssoftware.rfidmaint4;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class RfidMaint_Main extends Activity implements
        LoaderManager.LoaderCallbacks<Cursor>{

    private SimpleCursorAdapter dataAdapter;
    public static final String KEY_EPC = "epc";
    public static final String mode = null;
    String wEpc = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rfidmaint_main);
        Intent in = getIntent();
        Bundle b = in.getExtras();
        wEpc = b.getString("epc");




       displayListView();



    }

    @Override
    protected void onResume() {
        super.onResume();
        //Starts a new or restarts an existing Loader in this manager
        getLoaderManager().restartLoader(0, null, this);
    }

    private void displayListView() {


        // The desired columns to be bound
        String[] columns = new String[] {

                TreesDb.KEY_EPC,
                TreesDb.KEY_LNAME,
                TreesDb.KEY_LATITUDE,
                TreesDb.KEY_LONGITUDE,
                TreesDb.KEY_APPLICATION,
                TreesDb.KEY_CLIENT,
                TreesDb.KEY_MDATE
        };

        // the XML defined views which the data will be bound to
        int[] to = new int[] {
                R.id.epc,
                R.id.lname,
                R.id.latitude,
                R.id.longitude,
                R.id.application,
                R.id.client,
                R.id.date
        };

        // create an adapter from the SimpleCursorAdapter
        dataAdapter = new SimpleCursorAdapter(
                this,
                R.layout.country_info,
                null,
                columns,
                to,
                0);

        // get reference to the ListView
        ListView listView = (ListView) findViewById(R.id.treeList);
        // Assign adapter to ListView
        listView.setAdapter(dataAdapter);
        //Ensures a loader is initialized and active.
        getLoaderManager().initLoader(0, null, this);


        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> listView, View view,
                                    int position, long id) {
                // Get the cursor, positioned to the corresponding row in the result set
                Cursor cursor = (Cursor) listView.getItemAtPosition(position);

                // display the selected country

                cursor.getString(cursor.getColumnIndexOrThrow(TreesDb.KEY_EPC));

                String rowId =
                        cursor.getString(cursor.getColumnIndexOrThrow(TreesDb.KEY_ROWID));

                // starts a new Intent to update/delete a Country
                // pass in row Id to create the Content URI for a single row


            }
        });

    }

    // This is called when a new Loader needs to be created.
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                TreesDb.KEY_ROWID,
                TreesDb.KEY_EPC,
                TreesDb.KEY_LNAME,
                TreesDb.KEY_LATITUDE,
                TreesDb.KEY_LONGITUDE,
                TreesDb.KEY_APPLICATION,
                TreesDb.KEY_CLIENT,
                TreesDb.KEY_MDATE};


        String mSelection = "epc  = ? ";
        String[] mSelectionArgs = new String[]{wEpc};
        CursorLoader cursorLoader = new CursorLoader(this,
                MyContentProvider.CONTENT_URI, projection, mSelection, mSelectionArgs, null);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        dataAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        dataAdapter.swapCursor(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }
}


