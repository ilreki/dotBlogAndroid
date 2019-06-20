package com.reki.dotBlog.contentProvider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.reki.dotBlog.sqliteOpenHelper.UserLoginInfoSQLiteOpenHelper;

public class UserLoginInfoContentProvider extends ContentProvider {
    private static UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private UserLoginInfoSQLiteOpenHelper userLoginInfoSQLiteOpenHelper;
    private final static int T_USER_LOGIN_INFO = 1;
    static {

        uriMatcher.addURI("com.reki.UserLoginInfoContentProvider", "t_user_login_info", T_USER_LOGIN_INFO);
    }

    public UserLoginInfoContentProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase database = userLoginInfoSQLiteOpenHelper.getWritableDatabase();

        int deleteCount = 0;
        switch (uriMatcher.match(uri)){
            case T_USER_LOGIN_INFO:
                deleteCount = database.delete("t_user_login_info", selection, selectionArgs);
                database.close();
                break;
            default:
                throw new IllegalArgumentException("Unknown Uri : " + uri.toString());
        }
        return deleteCount;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)){
            case T_USER_LOGIN_INFO:
                return "com.reki.UserLoginInfoContentProvider/t_user_login_info";
            default:
                throw new IllegalArgumentException("Unknown Uri : " + uri.toString());
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase database = userLoginInfoSQLiteOpenHelper.getWritableDatabase();

        switch (uriMatcher.match(uri)){
            case T_USER_LOGIN_INFO:
                long id = database.insert("t_user_login_info", null, values);
                uri = ContentUris.withAppendedId(uri, id);
                database.close();
                return uri;
            default:
                throw new IllegalArgumentException("Unknown Uri : " + uri.toString());
        }
    }

    @Override
    public boolean onCreate() {
        userLoginInfoSQLiteOpenHelper = new UserLoginInfoSQLiteOpenHelper(this.getContext(), "db_user_login_info.db", null, 1);

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteDatabase database = userLoginInfoSQLiteOpenHelper.getReadableDatabase();

        switch (uriMatcher.match(uri)){
            case T_USER_LOGIN_INFO:
                Cursor cursor = database.query("t_user_login_info", projection, selection, selectionArgs, null, null, sortOrder);
                return cursor;
            default:
                throw new IllegalArgumentException("Unknown Uri : " + uri.toString());
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        SQLiteDatabase database = userLoginInfoSQLiteOpenHelper.getWritableDatabase();

        int updateCount = 0;
        switch (uriMatcher.match(uri)){
            case T_USER_LOGIN_INFO:
                updateCount = database.update("t_user_login_info", values, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown Uri : " + uri.toString());
        }

        return updateCount;
    }
}
