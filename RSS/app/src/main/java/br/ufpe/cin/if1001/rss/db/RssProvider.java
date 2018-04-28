package br.ufpe.cin.if1001.rss.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class RssProvider extends ContentProvider {

    private SQLiteRSSHelper db;

    public RssProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        return (int) this.db.deleteData(selection, selectionArgs);
    }

    @Override
    public String getType(Uri uri) {
        // não tenho certeza se esta certo
        return "vnd.android.cursor.dir/"+ this.db.getItems().getType(0);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // return null if was not successfully
        return this.db.insertItem(values) > 0 ? uri : null;
    }

    @Override
    public boolean onCreate() {
        // get SQL instance
        this.db = SQLiteRSSHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        // Implement this to handle query requests from clients.
        return this.db.getSelect(projection, selection, selectionArgs, sortOrder);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // Implement this to handle requests to update one or more rows.
        return (int)this.db.updateData(values, selection, selectionArgs);
    }
}