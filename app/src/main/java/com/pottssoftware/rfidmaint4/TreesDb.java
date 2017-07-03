package com.pottssoftware.rfidmaint4;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class TreesDb {

    public static final String KEY_ROWID = "_id";
    public static final String KEY_EPC = "epc";
    public static final String KEY_LNAME = "lname";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";
    public static final String KEY_APPLICATION = "application";
    public static final String KEY_CLIENT = "client";
    public static final String KEY_MDATE = "mdate";



    private static final String LOG_TAG = "TreesDb";
    public static final String SQLITE_TABLE = "Trees";

    private static final String DATABASE_CREATE =
            "CREATE TABLE if not exists " + SQLITE_TABLE + " (" +
                    KEY_ROWID + " integer PRIMARY KEY autoincrement," +
                    KEY_EPC + "," +
                    KEY_LNAME + "," +
                    KEY_LATITUDE + "," +
                    KEY_LONGITUDE + "," +
                    KEY_APPLICATION + "," +
                    KEY_CLIENT + "," +
                    KEY_MDATE + ")";

    public static void onCreate(SQLiteDatabase db) {
        Log.w(LOG_TAG, DATABASE_CREATE);
        db.execSQL(DATABASE_CREATE);
    }

    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(LOG_TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + SQLITE_TABLE);
        onCreate(db);
    }

}