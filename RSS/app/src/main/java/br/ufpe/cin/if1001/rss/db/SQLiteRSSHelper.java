package br.ufpe.cin.if1001.rss.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import br.ufpe.cin.if1001.rss.domain.ItemRSS;


public class SQLiteRSSHelper extends SQLiteOpenHelper {
    //Nome do Banco de Dados
    private static final String DATABASE_NAME = "rss";
    //Nome da tabela do Banco a ser usada
    public static final String DATABASE_TABLE = "items";
    //Versão atual do banco
    private static final int DB_VERSION = 1;

    //alternativa
    Context c;

    private SQLiteRSSHelper(Context context) {
        super(context, DATABASE_NAME, null, DB_VERSION);
        c = context;
    }

    private static SQLiteRSSHelper db;

    //Definindo Singleton
    public static SQLiteRSSHelper getInstance(Context c) {
        if (db==null) {
            db = new SQLiteRSSHelper(c.getApplicationContext());
        }
        return db;
    }

    //Definindo constantes que representam os campos do banco de dados
    public static final String ITEM_ROWID = RssProviderContract._ID;
    public static final String ITEM_TITLE = RssProviderContract.TITLE;
    public static final String ITEM_DATE = RssProviderContract.DATE;
    public static final String ITEM_DESC = RssProviderContract.DESCRIPTION;
    public static final String ITEM_LINK = RssProviderContract.LINK;
    public static final String ITEM_UNREAD = RssProviderContract.UNREAD;

    //Definindo constante que representa um array com todos os campos
    public final static String[] columns = { ITEM_ROWID, ITEM_TITLE, ITEM_DATE, ITEM_DESC, ITEM_LINK, ITEM_UNREAD};

    //Definindo constante que representa o comando de criação da tabela no banco de dados
    private static final String CREATE_DB_COMMAND = "CREATE TABLE " + DATABASE_TABLE + " (" +
            ITEM_ROWID +" integer primary key autoincrement, "+
            ITEM_TITLE + " text not null, " +
            ITEM_DATE + " text not null, " +
            ITEM_DESC + " text not null, " +
            ITEM_LINK + " text not null, " +
            ITEM_UNREAD + " boolean not null);";

    @Override
    public void onCreate(SQLiteDatabase db) {
        //Executa o comando de criação de tabela
        db.execSQL(CREATE_DB_COMMAND);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //estamos ignorando esta possibilidade no momento
        throw new RuntimeException("nao se aplica");
    }

    //Implemente a manipulação de dados nos métodos auxiliares para não ficar criando consultas manualmente
    public long insertItem(ItemRSS item) {
        return insertItem(item.getTitle(),item.getPubDate(),item.getDescription(),item.getLink());
    }

    // Insert with ContentValues
    public long insertItem(ContentValues values) {
        return getWritableDatabase().insert(DATABASE_TABLE, null, values);
    }

    public long insertItem(String title, String pubDate, String description, String link) {
        ContentValues values = new ContentValues();
        values.put(ITEM_TITLE, title);
        values.put(ITEM_DATE, pubDate);
        values.put(ITEM_DESC, description);
        values.put(ITEM_LINK, link);
        values.put(ITEM_UNREAD, true);
        return insertItem(values);
    }
    public ItemRSS getItemRSS(String link) throws SQLException {

        Cursor cursor = getReadableDatabase().rawQuery("select * from " + DATABASE_TABLE + " WHERE " + ITEM_LINK + " = ?",
                new String[] { link });
        if(cursor.moveToNext()) {
            return new ItemRSS(cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4));
        } else {
            return null;
        }
    }

    // update data with param condition and values
    public long updateData(ContentValues values, String where, String[] whereArgs) {
        return getWritableDatabase().update(DATABASE_TABLE, values, where , whereArgs);
    }

    // logical delete, dont remove from DB
    public long deleteData(String where, String[] whereArgs) {
        ContentValues values = new ContentValues();
        values.put(ITEM_UNREAD, false);
        return getWritableDatabase().update(DATABASE_TABLE, values, where, whereArgs);
    }

    public Cursor getSelect(String[] projection, String selection,
                     String[] selectionArgs, String sortOrder) {
        return getReadableDatabase().query(DATABASE_TABLE, projection, selection, selectionArgs,
                null, null, sortOrder);
    }

    public Cursor getItems() throws SQLException {
        return getReadableDatabase().rawQuery("select * from " + DATABASE_TABLE + " WHERE " + ITEM_UNREAD, new String[]{});
    }

    public boolean markAsUnread(String link) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues row = new ContentValues();
        row.put(ITEM_UNREAD, true);

        int modified = db.update(DATABASE_TABLE, row, ITEM_LINK + " = ?", new String[] { link });

        return modified > 0;
    }

    public boolean markAsRead(String link) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues row = new ContentValues();
        row.put(ITEM_UNREAD, false);

        int modified = db.update(DATABASE_TABLE, row, ITEM_LINK + " = ?", new String[] { link });

        return modified > 0;
    }

}
