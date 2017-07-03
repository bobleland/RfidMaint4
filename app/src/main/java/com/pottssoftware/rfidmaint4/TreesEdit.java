package com.pottssoftware.rfidmaint4;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class TreesEdit extends Activity implements OnClickListener{

 //   private Spinner treeList;
    private Button save, delete;
    private String mode;
    private String wEpc;
    private EditText epc, lname, latitude, longitude, application, client, mdate;
    private String id;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_page);

        // get the values passed to the activity from the calling activity
        // determine the mode - add, update or delete
        Intent in = getIntent();
        Bundle b = in.getExtras();
        mode = b.getString("add");
        wEpc = b.getString("epc");




        Bundle bundle = this.getIntent().getExtras();
            mode = bundle.getString("mode");
            wEpc = bundle.getString("epc");



        // get references to the buttons and attach listeners
        save = (Button) findViewById(R.id.save);
        save.setOnClickListener(this);
        delete = (Button) findViewById(R.id.delete);
        delete.setOnClickListener(this);
        epc = (EditText) findViewById(R.id.epc);
        lname = (EditText) findViewById(R.id.lname);
        latitude = (EditText) findViewById(R.id.latitude);
        longitude = (EditText) findViewById(R.id.longitude);
        application = (EditText) findViewById(R.id.application);
        client = (EditText) findViewById(R.id.client);
        mdate = (EditText) findViewById(R.id.date);

        epc.setText(wEpc);
        // create a dropdown for users to select various continents
     //   continentList = (Spinner) findViewById(continentList);
      //  ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
      //          R.array.continent_array, android.R.layout.simple_spinner_item);
      //  adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
     //   continentList.setAdapter(adapter);

        // if in add mode disable the delete option
      //  if(mode.trim().equalsIgnoreCase("add")){
       //     delete.setEnabled(false);
        }
        // get the rowId for the specific country
      //  else{
      //      Bundle bundle = this.getIntent().getExtras();
      //      id = bundle.getString("rowId");
     //     loadCountryInfo();



    public void onClick(View v) {

        // get values from the spinner and the input text fields
     //  String myContinent = continentList.getSelectedItem().toString();

        String myEpc = epc.getText().toString();
        String myLname = lname.getText().toString();
        String myLatitude = latitude.getText().toString();
        String myLongitude = longitude.getText().toString();
        String myApplication = application.getText().toString();
        String myClient = client.getText().toString();       // check for blanks
        String myDate = mdate.getText().toString();
        if(myLname.trim().equalsIgnoreCase("")){
            Toast.makeText(getBaseContext(), "Please ENTER Latin Name", Toast.LENGTH_LONG).show();
            return;
        }

        // check for blanks
        if(myLatitude.trim().equalsIgnoreCase("")){
            Toast.makeText(getBaseContext(), "Please ENTER latitude", Toast.LENGTH_LONG).show();
            return;
        }
        if(myLongitude.trim().equalsIgnoreCase("")){
            Toast.makeText(getBaseContext(), "Please ENTER longitude", Toast.LENGTH_LONG).show();
            return;
        }
        if(myApplication.trim().equalsIgnoreCase("")){
            Toast.makeText(getBaseContext(), "Please ENTER Application", Toast.LENGTH_LONG).show();
            return;
        }
        if(myClient.trim().equalsIgnoreCase("")){
            Toast.makeText(getBaseContext(), "Please ENTER Client", Toast.LENGTH_LONG).show();
            return;
        }
        if(myDate.trim().equalsIgnoreCase("")){
            Toast.makeText(getBaseContext(), "Please ENTER Date", Toast.LENGTH_LONG).show();
            return;
        }



        switch (v.getId()) {
            case R.id.save:
                ContentValues values = new ContentValues();
                values.put(TreesDb.KEY_EPC, myEpc);
                values.put(TreesDb.KEY_LNAME, myLname);
                values.put(TreesDb.KEY_LATITUDE, myLatitude);
                values.put(TreesDb.KEY_LONGITUDE, myLongitude);
                values.put(TreesDb.KEY_APPLICATION, myApplication);
                values.put(TreesDb.KEY_CLIENT, myClient);
                values.put(TreesDb.KEY_MDATE, myDate);

                // insert a record
                if(mode.trim().equalsIgnoreCase("add")){
                    getContentResolver().insert(MyContentProvider.CONTENT_URI, values);
                }
                // update a record
                else {
                    Uri uri = Uri.parse(MyContentProvider.CONTENT_URI + "/" + id);
                    getContentResolver().update(uri, values, null, null);
                }
                finish();
                break;

            case R.id.delete:
                // delete a record
                Uri uri = Uri.parse(MyContentProvider.CONTENT_URI + "/" + id);
                getContentResolver().delete(uri, null, null);
                finish();
                break;

            // More buttons go here (if any) ...

        }
        Bundle bundle = new Bundle();
        bundle.putString("epc", wEpc);
        Intent inEd = new Intent(getApplicationContext(),RfidMaint_Main.class);
        inEd.putExtras(bundle);
        startActivity(inEd);

    }

    // based on the rowId get all information from the Content Provider
    // about that country
    private void loadCountryInfo(){

        String[] projection = {
                TreesDb.KEY_ROWID,
                TreesDb.KEY_EPC,
                TreesDb.KEY_LNAME,
                TreesDb.KEY_LONGITUDE,
                TreesDb.KEY_APPLICATION,
                TreesDb.KEY_CLIENT,
                TreesDb.KEY_MDATE};
        String mSelection = "epc = ? ";
        String[] mSelectionArgs = new String[]{wEpc};
        Uri uri = Uri.parse(MyContentProvider.CONTENT_URI + "/" + id);

        Cursor cursor = getContentResolver().query(uri, projection, mSelection ,mSelectionArgs,
                null);

        if (cursor != null) {
            cursor.moveToFirst();
            String myEpc = cursor.getString(cursor.getColumnIndexOrThrow(TreesDb.KEY_EPC));
            String myLname = cursor.getString(cursor.getColumnIndexOrThrow(TreesDb.KEY_LNAME));
            String myLatitude = cursor.getString(cursor.getColumnIndexOrThrow(TreesDb.KEY_LATITUDE));
            String myLongitude = cursor.getString(cursor.getColumnIndexOrThrow(TreesDb.KEY_LONGITUDE));
        String myApplication = cursor.getString(cursor.getColumnIndexOrThrow(TreesDb.KEY_APPLICATION));
        String myClient = cursor.getString(cursor.getColumnIndexOrThrow(TreesDb.KEY_CLIENT));
        String myDate = cursor.getString(cursor.getColumnIndexOrThrow(TreesDb.KEY_MDATE));
            epc.setText(myEpc);
            lname.setText(myLname);
            latitude.setText(myLatitude);
            longitude.setText(myLongitude);
            application.setText(myApplication);
            client.setText(myClient);
             mdate.setText(myDate);

        }

    }

    // this sets the spinner selection based on the value
//    private int getIndex(Spinner spinner, String myString){

  //      int index = 0;

  //      for (int i=0;i<spinner.getCount();i++){
  //          if (spinner.getItemAtPosition(i).equals(myString)){
   //             index = i;
   //         }
   //     }
   //     return index;
   // }
  private void loadRfidTaginfo(){

      String[] projection = {
              TreesDb.KEY_ROWID,
              TreesDb.KEY_EPC,
              TreesDb.KEY_LNAME,
              TreesDb.KEY_LONGITUDE,
              TreesDb.KEY_APPLICATION,
              TreesDb.KEY_CLIENT,
              TreesDb.KEY_MDATE};


      Uri uri = Uri.parse(MyContentProvider.CONTENT_URI + "/" + id);

      // SQLiteQueryBuilder is a helper class that creates the
      // proper SQL syntax for us.
      SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();

      // Set the table we're querying.
      qBuilder.setTables(TreesDb.SQLITE_TABLE);

      // If the query ends in a specific record number, we're
      // being asked for a specific record, so set the
      // WHERE clause in our query.

      Context context = getApplicationContext();
      MyDatabaseHelper dbHelper = new MyDatabaseHelper(context);

      // permissions to be writable
      SQLiteDatabase database;

      database = dbHelper.getReadableDatabase();

      // Make the query.
      String selection = "TreesDb.KEY_EPC = ? ";
      String[] selectionArgs = {wEpc};

      Cursor cursor = qBuilder.query(database,
              projection,
              selection,
              selectionArgs,
              null,
              null,
              null);

      cursor.setNotificationUri(getApplicationContext().getContentResolver(), uri);
      // return c;

      if (cursor != null) {
          cursor.moveToFirst();
          String myEpc = cursor.getString(cursor.getColumnIndexOrThrow(TreesDb.KEY_EPC));
          String myLname = cursor.getString(cursor.getColumnIndexOrThrow(TreesDb.KEY_LNAME));
          String myLatitude = cursor.getString(cursor.getColumnIndexOrThrow(TreesDb.KEY_LATITUDE));
          String myLongitude = cursor.getString(cursor.getColumnIndexOrThrow(TreesDb.KEY_LONGITUDE));
          String myApplication = cursor.getString(cursor.getColumnIndexOrThrow(TreesDb.KEY_APPLICATION));
          String myClient = cursor.getString(cursor.getColumnIndexOrThrow(TreesDb.KEY_CLIENT));
          String myDate = cursor.getString(cursor.getColumnIndexOrThrow(TreesDb.KEY_MDATE));
          epc.setText(myEpc);
          lname.setText(myLname);
          latitude.setText(myLatitude);
          longitude.setText(myLongitude);
          application.setText(myApplication);
          client.setText(myClient);
          mdate.setText(myDate);

      }
  }

}

