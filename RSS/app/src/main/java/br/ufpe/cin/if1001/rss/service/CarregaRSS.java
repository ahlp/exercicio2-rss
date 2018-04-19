package br.ufpe.cin.if1001.rss.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
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
import br.ufpe.cin.if1001.rss.ui.MainActivity;
import br.ufpe.cin.if1001.rss.util.ParserRSS;

public class CarregaRSS extends IntentService {

    public static String BROADCAST_UPDATE_RSS_ACTION = "NEW_FEED_RSS";
    public static String BROADCAST_NEW_ITEM_ACTION = "NEW_ITEM_RSS";
    private SQLiteRSSHelper db;
    private String linkfeed;

    public CarregaRSS() {
        super("CarregaRSS");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.linkfeed = preferences.getString("rssfeedlink", getResources().getString(R.string.rssfeed));

        this.db = SQLiteRSSHelper.getInstance(getApplicationContext());

        boolean flag_problema = false;
        boolean new_item = false;
        List<ItemRSS> items = null;
        try {
            String feed = getRssFeed(linkfeed);
            items = ParserRSS.parse(feed);
            for (ItemRSS i : items) {
                Log.d("DB", "Buscando no Banco por link: " + i.getLink());
                ItemRSS item = db.getItemRSS(i.getLink());
                if (item == null) {
                    Log.d("DB", "Encontrado pela primeira vez: " + i.getTitle());
                    db.insertItem(i);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            flag_problema = true;
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            flag_problema = true;
        }
        if(items != null) {
            for (ItemRSS item : items) {
                if(db.getItemRSS(item.getLink()) == null){
                    db.insertItem(item);
                    new_item = true;
                }
            }
        }

        // broadcast
        Intent intent = new Intent();
        intent.setAction(BROADCAST_UPDATE_RSS_ACTION);
        intent.putExtra("problema", flag_problema);

//        new MainActivity.ExibirFeed().execute();
        sendBroadcast(intent);

        if (new_item) {
            Intent notifyIntent = new Intent();
            sendBroadcast(intent.setAction(BROADCAST_NEW_ITEM_ACTION));
        }

    }

    private String getRssFeed(String feed) throws IOException {
        InputStream in = null;
        String rssFeed = "";
        try {
            URL url = new URL(feed);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            in = conn.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int count; (count = in.read(buffer)) != -1; ) {
                out.write(buffer, 0, count);
            }
            byte[] response = out.toByteArray();
            rssFeed = new String(response, "UTF-8");
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return rssFeed;
    }
}

