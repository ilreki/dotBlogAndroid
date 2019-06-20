package com.reki.dotBlog.sqliteOpenHelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class UserLoginInfoSQLiteOpenHelper extends SQLiteOpenHelper {
    public UserLoginInfoSQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, Integer version){
        super(context, name, factory, version);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table t_user_login_info(_id integer primary key autoincrement, " +
                "user_id integer, username text, password text, avatar text, user_type integer, is_login integer)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
