package br.ufpe.cin.if1001.rss.ui;

import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import br.ufpe.cin.if1001.rss.R;
import br.ufpe.cin.if1001.rss.db.SQLiteRSSHelper;
import br.ufpe.cin.if1001.rss.domain.ItemRSS;
import br.ufpe.cin.if1001.rss.service.CarregaRSS;
import br.ufpe.cin.if1001.rss.util.ParserRSS;

public class MainActivity extends Activity {

    private ListView conteudoRSS;
    private final String RSS_FEED = "http://rss.cnn.com/rss/edition.rss";
    private SQLiteRSSHelper db;
    private BroadcastReceiver br;

    private boolean isBackgroud = true;

    public void callURL(String link) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        startActivity(browserIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = SQLiteRSSHelper.getInstance(this);
        conteudoRSS = findViewById(R.id.conteudoRSS);

        br = new FeedDynamicReceiver();

        SimpleCursorAdapter adapter =
                new SimpleCursorAdapter(
                        //contexto, como estamos acostumados
                        this,
                        //Layout XML de como se parecem os itens da lista
                        R.layout.item,
                        //Objeto do tipo Cursor, com os dados retornados do banco.
                        //Como ainda não fizemos nenhuma consulta, está nulo.
                        null,
                        //Mapeamento das colunas nos IDs do XML.
                        // Os dois arrays a seguir devem ter o mesmo tamanho
                        new String[]{SQLiteRSSHelper.ITEM_TITLE, SQLiteRSSHelper.ITEM_DATE},
                        new int[]{R.id.itemTitulo, R.id.itemData},
                        //Flags para determinar comportamento do adapter, pode deixar 0.
                        0
                );
        //Seta o adapter. Como o Cursor é null, ainda não aparece nada na tela.
        conteudoRSS.setAdapter(adapter);

        // permite filtrar conteudo pelo teclado virtual
        conteudoRSS.setTextFilterEnabled(true);

        //Complete a implementação deste método de forma que ao clicar, o link seja aberto no navegador e
        // a notícia seja marcada como lida no banco
        conteudoRSS.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SimpleCursorAdapter adapter = (SimpleCursorAdapter) parent.getAdapter();
                Cursor mCursor = ((Cursor) adapter.getItem(position));
                String link = mCursor.getString(4);

                callURL(link);
                db.markAsRead(link);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter();
        filter.addAction(CarregaRSS.BROADCAST_UPDATE_RSS_ACTION);
        this.registerReceiver(br, filter);


        Intent carregaRSS = new Intent(getApplicationContext(), CarregaRSS.class);
        startService(carregaRSS);

        this.isBackgroud = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.unregisterReceiver(br);
        this.isBackgroud = true;
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mainmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btn_Config:
                startActivity(new Intent(this, ConfigActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void newFeedNotification() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this);

        mBuilder.setContentTitle("Novo item no feed");
        mBuilder.setContentText("Um novo item no seu feed RSS foi adicionado!");
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));

        // launch notification
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(001, mBuilder.build());
    }

    public class FeedDynamicReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            new MainActivity.ExibirFeed().execute();
        }
    }

    public class FeedStaticReceiver extends BroadcastReceiver {

        public FeedStaticReceiver() { }
        @Override
        public void onReceive(Context context, Intent intent) {
            // verify if passed for the stop method
            if (isBackgroud){
                newFeedNotification();
            }
        }
    }


    class ExibirFeed extends AsyncTask<Void, Void, Cursor> {

        @Override
        protected Cursor doInBackground(Void... voids) {
            Cursor c = db.getItems();
            c.getCount();
            return c;
        }

        @Override
        protected void onPostExecute(Cursor c) {
            if (c != null) {
                ((CursorAdapter) conteudoRSS.getAdapter()).changeCursor(c);
            }
        }
    }
}
