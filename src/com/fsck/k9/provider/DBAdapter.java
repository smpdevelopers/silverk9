package com.fsck.k9.provider;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.sql.SQLException;
import java.util.LinkedHashMap;

public class DBAdapter {
    private static final String TAG = "DBAdapter";
    //DIMA TODO: back to false
    private static final boolean DEBUG = true;

    private static DBAdapter mInstance;

    private static final String DB_NAME = "myMessageDB_events.db";
    private static final String DB_TABLE = "events";
    private static final int DB_VERSION = 4;

    public static final String KEY_ID = "_id";
    public static final String SILVER_VUE_EVENT_ID = "SilverVue_eventID";
    public static final String CALENDAR_EVENT_ID = "Calendar_eventID";

    private static final String DATABASE_CREATE = "create table " +
            DB_TABLE + " (" +
            KEY_ID + " integer primary key autoincrement, " +
            SILVER_VUE_EVENT_ID + " text, " +
            CALENDAR_EVENT_ID + " text" + ");";

    private SQLiteDatabase db;
    private final Context context;
    private myDBHelper dbHelper;

    public static class EventsObj {
        public String SilverVue_EventID;
        public String Calendar_EventID;
        EventsObj(String _SilverVue_EventID, String _Calendar_EventID) {
            SilverVue_EventID = _SilverVue_EventID;
            Calendar_EventID = _Calendar_EventID;
        }
        EventsObj() {
        }
    }

    public static DBAdapter getInstance(Context context) {
        if(null == mInstance) {
            mInstance = new DBAdapter(context);
        }
        return mInstance;
    }

    private DBAdapter(Context _context) {
        context = _context;
        dbHelper = new myDBHelper(context, DB_NAME, null, DB_VERSION);
    }


    public DBAdapter open() throws SQLException {
        db = dbHelper.getWritableDatabase();
        if (DEBUG) Log.d(TAG, "open");
        return this;
    }

    public void close() {
        if (DEBUG) Log.d(TAG, "close");
        db.close();
    }

    public long addID(String SilverVueEventID, String CalendarEventID) {
        if (DEBUG) Log.d(TAG, "addID: SilverVueID: " + SilverVueEventID
                + "; CalendarID: " + CalendarEventID);
        ContentValues cv = new ContentValues();
        cv.put(SILVER_VUE_EVENT_ID, SilverVueEventID);
        cv.put(CALENDAR_EVENT_ID, CalendarEventID);

        return db.insert(DB_TABLE, null, cv);
    }

    public boolean changeID(String SilverVueEventID, String CalendarEventID) {
        if (DEBUG) Log.d(TAG, "changeID: SilverVueID: " + SilverVueEventID
                + "; CalendarID: " + CalendarEventID);
        ContentValues cv = new ContentValues();
        cv.put(SILVER_VUE_EVENT_ID, SilverVueEventID);
        cv.put(CALENDAR_EVENT_ID, CalendarEventID);

        return db.update(DB_TABLE, cv, SILVER_VUE_EVENT_ID + "=" + SilverVueEventID, null) > 0;
    }

    public boolean deleteID(String SilverVueEventID) {
        if (DEBUG) Log.d(TAG, "deleteID: SilverVueID: " + SilverVueEventID);

        Cursor cursor = db.query(DB_TABLE,
                new String[] {KEY_ID, SILVER_VUE_EVENT_ID, CALENDAR_EVENT_ID},
                null, null, null, null, null);

        if((cursor.getColumnCount() == 0) || (!cursor.moveToFirst())) {
            cursor.close();
            return false;
        }

        do {
            if(SilverVueEventID.equalsIgnoreCase(cursor.getString(1))) {
                int row = db.delete(DB_TABLE, KEY_ID + "=" + cursor.getString(0), null);
                if (DEBUG) Log.d(TAG, "deleteID: SilverVueEventID: " + SilverVueEventID +
                        " row: " + row);
                cursor.close();
                return true;
            }
        } while(cursor.moveToNext());

        cursor.close();
        return false;
    }

    public boolean isAddedID(String SilverVueEventID) {
        if (DEBUG) Log.d(TAG, "isAddedID: SilverVueEventID: " + SilverVueEventID);
        Cursor cursor = db.query(DB_TABLE,
                new String[] {SILVER_VUE_EVENT_ID, CALENDAR_EVENT_ID},
                null, null, null, null, null);

        if((cursor.getColumnCount() == 0) || (!cursor.moveToFirst())) {
            if (DEBUG) Log.d(TAG, "isAddedID:1 SilverVueEventID: " + SilverVueEventID + " - does not exist");
            cursor.close();
            return false;
        }
        do {
            if(SilverVueEventID.equalsIgnoreCase(cursor.getString(0))) {
                if (DEBUG) Log.d(TAG, "isAddedID: SilverVueEventID: " + SilverVueEventID +
                        " - exists, value: " + cursor.getString(1));
                cursor.close();
                return true;
            }
        } while(cursor.moveToNext());

        if (DEBUG) Log.d(TAG, "isAddedID:2 SilverVueEventID: " + SilverVueEventID + " - does not exist");
        cursor.close();
        return false;
    }

    public String findID(String SilverVueEventID) {
        if (DEBUG) Log.d(TAG, "findID: SilverVueEventID: " + SilverVueEventID);
        Cursor cursor = db.query(DB_TABLE,
                new String[] {SILVER_VUE_EVENT_ID, CALENDAR_EVENT_ID},
                null, null, null, null, null);

        if((cursor.getColumnCount() == 0) || (!cursor.moveToFirst())) {
            cursor.close();
            return new String();
        }

        do {
            if(SilverVueEventID.equalsIgnoreCase(cursor.getString(0))) {
                if (DEBUG) Log.d(TAG, "findID: SilverVueEventID: " + SilverVueEventID +
                        " has value: " + cursor.getString(1));
                String retVal = cursor.getString(1);
                cursor.close();
                return retVal;
            }
        } while(cursor.moveToNext());

        cursor.close();
        return new String();
    }

    public Cursor getCursor() {
        if (DEBUG) Log.d(TAG, "getCursor");
        Cursor cursor = db.query(DB_TABLE,
                new String[] {
                        KEY_ID,
                        SILVER_VUE_EVENT_ID,
                        CALENDAR_EVENT_ID},
                null, null, null, null, null);


        return cursor;
    }


    public LinkedHashMap<String,String> getEvents() {
        if (DEBUG) Log.d(TAG, "getEvents");
        Cursor cursor = db.query(DB_TABLE,
                new String[] {
                        SILVER_VUE_EVENT_ID,
                        CALENDAR_EVENT_ID},
                null, null, null, null, null);

        LinkedHashMap<String,String> eventsMap = new LinkedHashMap<String,String>();

        if((cursor.getColumnCount() == 0) || (!cursor.moveToFirst())) {
            if (DEBUG) Log.d(TAG, "getEvents: gets null events");
            cursor.close();
            return eventsMap;
        }

        if (DEBUG) Log.d(TAG, "getEvents: gets " + cursor.getCount() + " events");
        do {
            if (DEBUG) Log.d(TAG, "getEvents: add to list: " +
                    cursor.getString(0) + ", " + cursor.getString(1));
            eventsMap.put(cursor.getString(0), cursor.getString(1));
        } while(cursor.moveToNext());

        cursor.close();
        return eventsMap;
    }

    private static class myDBHelper extends SQLiteOpenHelper {

        public myDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase _db) {
            _db.execSQL(DATABASE_CREATE);

        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {

        }
    }
}