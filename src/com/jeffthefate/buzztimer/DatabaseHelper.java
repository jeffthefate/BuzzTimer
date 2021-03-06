package com.jeffthefate.buzztimer;

import java.util.Arrays;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Executes all the database actions, including many helper functions and
 * constants.
 * 
 * @author Jeff Fate
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    
    public static SQLiteDatabase db;
    
    private static final String DB_NAME = "buzztimerDb";
    
    public static final String TIMER_TABLE = "Timer";
    
    public static final String COL_MSEC = "MSec";
    public static final String COL_BACKGROUND = "Background";
    /**
     * Create timer table string
     */
    private static final String CREATE_TIMER_TABLE = "CREATE TABLE " + 
            TIMER_TABLE + " (" + COL_MSEC + " INTEGER DEFAULT 60000, " +
    		COL_BACKGROUND + " TEXT)";
    /**
     * Create the helper object that creates and manages the database.
     * 
     * @param context	the context used to create this object
     */
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, 1);
        db = getWritableDatabase();
    }
    /**
     * Global instance of this DatabaseHelper.
     */
    private static DatabaseHelper instance;
    
    public static synchronized DatabaseHelper getInstance() {
        if (instance == null)
            instance = new DatabaseHelper(ApplicationEx.getApp());
        return instance;
    }
    
    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TIMER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TIMER_TABLE);
        onCreate(db);
    }

    /**
     * Look for an item in a specific table.
     * 
     * @param name		identifier for the item to lookup
     * @param table		the table to look in
     * @param column	the column to look under
     * @return if the item is found
     */
    public boolean inDb(String[] values, String table, 
            String[] columns) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            if (i != 0)
                sb.append(" AND ");
            sb.append(columns[i]);
            sb.append("=?");
        }
        Cursor cur = db.query(
                table, columns, sb.toString(), values, null, null, null);
        boolean inDb = false;
        if (cur.moveToFirst())
            inDb = true;
        cur.close();
        return inDb;
    }
    
    /**
     * Insert a new record into a table in the database.
     * 
     * @param cv			list of content values to be entered
     * @param tableName		the table name to be inserted into
     * @param columnName	the column that isn't null if the rest are null
     * @return the row id of the inserted row
     */
    public long insertRecord(ContentValues cv, String tableName,
            String columnName) {
        return db.insert(tableName, columnName, cv);
    }
    
    /**
     * Update a record in a table in the database.
     * 
     * @param cv			list of content values to be entered
     * @param tableName		the table name to be inserted into
     * @param whereClause	what to look for
     * @return the number of rows affected
     */
    public long updateRecord(ContentValues cv, String tableName,
            String whereClause, String[] selectionArgs) {
        int result = -1;
        try {
            result = db.update(tableName, cv, whereClause, selectionArgs);
        } catch (IllegalArgumentException e) {}
        return result;
    }
    
    /**
     * Check if a record exists.
     * 
     * @param columnName	column to return so not all columns are returned
     * @return if the record exists
     */
    private boolean recordExists(String columnName) {
        Cursor cur = db.query(TIMER_TABLE, new String[] {columnName}, null,
        		null, null, null, null);
        boolean exists = false;
        if (cur.getCount() > 0)
            exists = true;
        cur.close();
        return exists;
    }
    
    /**
     * Set the current timer time value.
     * 
     * @param mSec	new time value in milliseconds
     */
    public void setTime(long mSec) {
        ContentValues cv = new ContentValues();
        cv.put(COL_MSEC, mSec);
        if (recordExists(COL_MSEC))
            updateRecord(cv, TIMER_TABLE, null, null);
        else
            insertRecord(cv, TIMER_TABLE, COL_MSEC);
    }
    
    /**
     * Get the current timer time value.
     * 
     * @return current time in milliseconds
     */
    public int getTime() {
        Cursor cur = db.query(TIMER_TABLE, new String[] {COL_MSEC}, null, null,
                null, null, null);
        int time = -1;
        if (cur.moveToFirst()) {
            time = cur.getInt(cur.getColumnIndex(COL_MSEC));
        }
        cur.close();
        return time;
    }
    
    /**
     * Set the current background name.
     * 
     * @param currBackground	name of the background that is displayed
     * @return result of the update action
     */
    public long setCurrBackground(String currBackground) {
    	if (currBackground == null)
    		return -1;
        long returnValue = -1;
        ContentValues cv = new ContentValues();
        cv.put(COL_BACKGROUND, currBackground);
        returnValue = updateRecord(cv, TIMER_TABLE, null, new String[] {});
        return returnValue;
    }
    
    /**
     * Get the current background name.
     * 
     * @return the name of the background resource
     */
    public String getCurrBackground() {
        Cursor cur = db.query(TIMER_TABLE, new String[] {COL_BACKGROUND},
                null, new String[] {}, null, null, null);
        String background = null;
        if (cur.moveToFirst())
            background = cur.getString(cur.getColumnIndex(COL_BACKGROUND));
        cur.close();
        return background;
    }
    
    /**
     * Check to make sure all the columns exist.  This is to be used when the
     * application is created to update the database if necessary.
     */
    public void checkUpgrade() {
        Cursor cur = db.query(TIMER_TABLE, null, null, null, null, null, null);
        String[] colArray = cur.getColumnNames();
        List<String> colList = Arrays.asList(colArray);
        cur.close();
        String sqlString;
        if (!colList.contains(COL_MSEC)) {
            sqlString = "ALTER TABLE " + TIMER_TABLE + " ADD " +
                    COL_MSEC + " INTEGER DEFAULT 60000";
            try {
                db.execSQL(sqlString);
            } catch (SQLException e) {
                Log.e(Constants.LOG_TAG, "Bad SQL string: " + sqlString, e);
            }
        }
        if (!colList.contains(COL_BACKGROUND)) {
            sqlString = "ALTER TABLE " + TIMER_TABLE + " ADD " +
            		COL_BACKGROUND + " TEXT";
            try {
                db.execSQL(sqlString);
            } catch (SQLException e) {
                Log.e(Constants.LOG_TAG, "Bad SQL string: " + sqlString, e);
            }
        }
    }
    
}