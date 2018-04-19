package br.ufpe.cin.if1001.rss.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

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
        // puxando as preferences pra pegar o link do feed rss
        // Removi do MainActivity junto com o service para modularizar melhor
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.linkfeed = preferences.getString("rssfeedlink", getResources().getString(R.string.rssfeed));

        // get instance database
        this.db = SQLiteRSSHelper.getInstance(getApplicationContext());

        // verifica se deu problema e manda pelo entity
        boolean flag_problema = false;
        // se algum item novo foi adicionado lança o broadcast e levantar notificação se em backgroud
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
                    new_item = true;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            flag_problema = true;
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            flag_problema = true;
        }

        // manda broadcast do que o request acabou
        // manda com a flag se deu problema
        Intent intent = new Intent();
        intent.setAction(BROADCAST_UPDATE_RSS_ACTION);
        intent.putExtra("problema", flag_problema);
        sendBroadcast(intent);

        // lança notificação caso algum item novo tenha sido adicionado
        if (new_item) {
            Intent notifyIntent = new Intent();
            notifyIntent.setAction(BROADCAST_NEW_ITEM_ACTION);
            sendBroadcast(notifyIntent);
        }

    }

    // request pra pegar feed RSS
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

