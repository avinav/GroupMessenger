package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by avinav on 2/15/15.
 */
public class MessengerOpenHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "MSG";
    private static final String TABLE_NAME = "MESSENGER";
    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    "key" + " TEXT NOT NULL UNIQUE, " + "value  " +
                    " TEXT);";
    MessengerOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, 2 );
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int x, int y) {

    }

}
