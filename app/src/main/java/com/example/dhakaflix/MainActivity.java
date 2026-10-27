package com.example.dhakaflix;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.addJavascriptInterface(new Object() {
            // Assets থেকে ৯.৫ এমবির movies.json মেমোরিতে লোড
            @JavascriptInterface
            public void loadLocalIndex() {
                new Thread(() -> {
                    StringBuilder builder = new StringBuilder();
                    try {
                        InputStream is = getAssets().open("movies.json");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                        String str;
                        while ((str = reader.readLine()) != null) {
                            builder.append(str);
                        }
                        reader.close();
                        String jsonString = builder.toString();
                        runOnUiThread(() -> webView.evaluateJavascript("window.onIndexLoaded(" + jsonString + ")", null));
                    } catch (Exception e) {
                        runOnUiThread(() -> webView.evaluateJavascript("window.onIndexError()", null));
                    }
                }).start();
            }

            // ব্রাউজিংয়ের সময় এইচটিএমএল রিড করা
            @JavascriptInterface
            public void fetchHtml(String targetUrl) {
                new Thread(() -> {
                    StringBuilder result = new StringBuilder();
                    try {
                        URL url = new URL(targetUrl);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                        conn.setConnectTimeout(8000);
                        conn.setReadTimeout(8000);

                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            result.append(line).append("\n");
                        }
                        reader.close();

                        String cleanHtml = result.toString().replace("`", "\\`").replace("$", "\\$");
                        runOnUiThread(() -> webView.evaluateJavascript("window.onHtmlLoaded(`" + cleanHtml + "`, `" + targetUrl + "`)", null));
                    } catch (Exception e) {
                        runOnUiThread(() -> webView.evaluateJavascript("window.onHtmlError(`" + e.getMessage() + "`)", null));
                    }
                }).start();
            }

            // সরাসরি VLC প্লেয়ার ট্রিগার
            @JavascriptInterface
            public void playVideoInVlc(String videoUrl) {
                runOnUiThread(() -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndTypeAndNormalize(Uri.parse(videoUrl), "video/*");
                        intent.setPackage("org.videolan.vlc");
                        startActivity(intent);
                    } catch (Exception e) {
                        try {
                            Intent generic = new Intent(Intent.ACTION_VIEW);
                            generic.setDataAndTypeAndNormalize(Uri.parse(videoUrl), "video/*");
                            startActivity(Intent.createChooser(generic, "Play video using"));
                        } catch (Exception ex) {
                            Toast.makeText(MainActivity.this, "VLC Player install kora nai!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }, "AndroidBridge");

        webView.setWebViewClient(new WebViewClient());
        loadAppHtml();
    }

    private void loadAppHtml() {
        String html = "<!DOCTYPE html>" +
                "<html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<style>" +
                "* { box-sizing: border-box; margin:0; padding:0; }" +
                "body { background: #0b0f19; color: #f1f5f9; font-family: sans-serif; padding: 14px; }" +
                "header { display:flex; flex-direction:column; gap:10px; margin-bottom:14px; }" +
                ".top-row { display:flex; justify-content:space-between; align-items:center; }" +
                "h1 { color: #38bdf8; font-size: 20px; cursor:pointer; }" +
                ".search-box { display:flex; gap:6px; }" +
                ".search-input { flex:1; padding:10px 12px; background:#1e293b; border:1px solid #334155; color:#fff; border-radius:6px; font-size:14px; outline:none; }" +
                ".search-btn { padding:0 16px; background:#0284c7; color:#fff; border:none; border-radius:6px; font-weight:bold; cursor:pointer; }" +
                ".grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 10px; }" +
                ".card { background: #1e293b; border: 1px solid #334155; border-radius: 8px; padding: 12px; display:flex; align-items:center; gap:10px; cursor:pointer; }" +
                ":focus { outline: 3px solid #38bdf8; background: #334155; }" +
                ".icon { font-size: 20px; flex-shrink:0; }" +
                ".name { font-size: 13px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }" +
                ".badge { font-size: 11px; padding: 3px 8px; border-radius: 4px; background: #064e3b; color: #34d399; }" +
                "</style></head><body>" +
                "<header>" +
                "<div class='top-row'>" +
                "<h1 onclick='loadHome()'>DhakaFlix Quick Hub</h1>" +
                "<span id='idxBadge' class='badge'>Loading 45K Index...</span>" +
                "</div>" +
                "<div class='search-box'>" +
                "<input type='text' id='sInput' class='search-input' placeholder='মুভি বা সিরিজের নাম লিখুন...' />" +
                "<button onclick='doSearch()' class='search-btn'>খুঁজুন</button>" +
                "</div>" +
                "</header>" +
                "<div id='bc' style='margin-bottom:12px; font-size:13px; color:#94a3b8;'></div>" +
                "<div id='grid' class='grid'></div>" +
                "<script>" +
                "let movieDatabase = [];" +
                "const CATS = [" +
                "{ name: 'TV & Web Series', url: 'http://172.16.50.12/DHAKA-FLIX-12/TV-WEB-Series/', icon: '📺' }," +
                "{ name: 'English Movies', url: 'http://172.16.50.7/DHAKA-FLIX-7/English%20Movies/', icon: '🎬' }," +
                "{ name: 'English Movies (1080p)', url: 'http://172.16.50.14/DHAKA-FLIX-14/English%20Movies%20%281080p%29/', icon: '🍿' }," +
                "{ name: 'IMDb Top-250 Movies', url: 'http://172.16.50.14/DHAKA-FLIX-14/IMDb%20Top-250%20Movies/', icon: '⭐' }," +
                "{ name: 'Hindi Movies', url: 'http://172.16.50.14/DHAKA-FLIX-14/Hindi%20Movies/', icon: '🎭' }," +
                "{ name: 'South Hindi Dubbed', url: 'http://172.16.50.14/DHAKA-FLIX-14/SOUTH%20INDIAN%20MOVIES/Hindi%20Dubbed/', icon: '🔥' }," +
                "{ name: 'South Original Movies', url: 'http://172.16.50.14/DHAKA-FLIX-14/SOUTH%20INDIAN%20MOVIES/South%20Movies/', icon: '📽️' }," +
                "{ name: 'Kolkata Bangla Movies', url: 'http://172.16.50.7/DHAKA-FLIX-7/Kolkata%20Bangla%20Movies/', icon: '🇧🇩' }," +
                "{ name: 'Satyajit Ray Films', url: 'http://172.16.50.7/DHAKA-FLIX-7/Kolkata%20Bangla%20Movies/Satyajit%20Ray%20Films/', icon: '🎞️' }," +
                "{ name: 'Foreign Language Movies', url: 'http://172.16.50.7/DHAKA-FLIX-7/Foreign%20Language%20Movies/', icon: '🌍' }," +
                "{ name: 'Korean TV & Web Series', url: 'http://172.16.50.14/DHAKA-FLIX-14/KOREAN%20TV%20%26%20WEB%20Series/', icon: '🇰🇷' }," +
                "{ name: 'Anime & Cartoon Series', url: 'http://172.16.50.10/DHAKA-FLIX-10/Anime%20%26%20Cartoon%20TV%20Series/', icon: '⚡' }," +
                "{ name: 'Animation Movies', url: 'http://172.16.50.14/DHAKA-FLIX-14/Animation%20Movies/', icon: '🎨' }," +
                "{ name: 'Animation Movies (1080p)', url: 'http://172.16.50.14/DHAKA-FLIX-14/Animation%20Movies%20%281080p%29/', icon: '✨' }," +
                "{ name: '3D Movies', url: 'http://172.16.50.7/DHAKA-FLIX-7/3D%20Movies/', icon: '👓' }," +
                "{ name: 'WWE & AEW Wrestling', url: 'http://172.16.50.10/DHAKA-FLIX-10/WWE%20%26%20AEW%20Wrestling/', icon: '🤼' }," +
                "{ name: 'Documentary', url: 'http://172.16.50.10/DHAKA-FLIX-10/Documentary/', icon: '📜' }," +
                "{ name: 'Awards & TV Shows', url: 'http://172.16.50.10/DHAKA-FLIX-10/Awards%20%26%20TV%20Shows/', icon: '🏆' }" +
                "];" +
                "const EXTS = ['.mkv','.mp4','.avi','.webm','.m4v','.ts'];" +
                "const grid = document.getElementById('grid');" +
                "const sInput = document.getElementById('sInput');" +
                "const idxBadge = document.getElementById('idxBadge');" +
                "function loadHome(){" +
                "document.getElementById('bc').innerHTML='<span>Home</span>';" +
                "grid.innerHTML='';" +
                "CATS.forEach(c=>{" +
                "let d=document.createElement('div'); d.className='card'; d.tabIndex=0;" +
                "d.innerHTML='<span class=\"icon\">'+c.icon+'</span><span class=\"name\">'+c.name+'</span>';" +
                "d.onclick=()=>loadDir(c.url, c.name);" +
                "grid.appendChild(d);" +
                "});" +
                "}" +
                "function loadDir(u, title){" +
                "grid.innerHTML='Loading...';" +
                "document.getElementById('bc').innerHTML='<span onclick=\"loadHome()\" style=\"color:#38bdf8;cursor:pointer;\">Home</span> / <b>'+title+'</b>';" +
                "AndroidBridge.fetchHtml(u);" +
                "}" +
                "function doSearch(){" +
                "let q = sInput.value.trim().toLowerCase();" +
                "if(!q) return;" +
                "if(!movieDatabase || movieDatabase.length === 0){ alert('ইনডেক্স ডেটা লোড হয়নি!'); return; }" +
                "document.getElementById('bc').innerHTML='Search Results: <b>'+q+'</b>';" +
                "grid.innerHTML='';" +
                "let hits = movieDatabase.filter(m => ((m.name||'').toLowerCase().includes(q))).slice(0, 100);" +
                "if(hits.length === 0){ grid.innerHTML='কিছু পাওয়া যায়নি!'; return; }" +
                "hits.forEach(m=>{" +
                "let title = m.name || 'Unknown';" +
                "let playUrl = m.url || '';" +
                "let isF = m.is_folder || playUrl.endsWith('/') || !EXTS.some(x=>playUrl.toLowerCase().endsWith(x));" +
                "let d=document.createElement('div'); d.className='card'; d.tabIndex=0;" +
                "d.innerHTML='<span class=\"icon\">'+(isF?'📁':'▶️')+'</span><span class=\"name\">'+title+'</span>';" +
                "if(isF) d.onclick=()=>loadDir(playUrl, title);" +
                "else d.onclick=()=>AndroidBridge.playVideoInVlc(playUrl);" +
                "grid.appendChild(d);" +
                "});" +
                "}" +
                "sInput.addEventListener('keydown', (e)=>{ if(e.key === 'Enter') doSearch(); });" +
                "window.onIndexLoaded = function(data){" +
                "movieDatabase = data;" +
                "idxBadge.innerText = 'Indexed ('+movieDatabase.length+')';" +
                "idxBadge.style.background = '#064e3b';" +
                "};" +
                "window.onIndexError = function(){" +
                "idxBadge.innerText = 'No Index File';" +
                "idxBadge.style.background = '#7f1d1d';" +
                "};" +
                "window.onHtmlLoaded = function(txt, base){" +
                "let doc = new DOMParser().parseFromString(txt, 'text/html');" +
                "let links = Array.from(doc.querySelectorAll('a'));" +
                "grid.innerHTML='';" +
                "let count = 0;" +
                "links.forEach(a=>{" +
                "let h = a.getAttribute('href');" +
                "if(!h || h.startsWith('?') || h==='/' || h==='../' || h.includes('parent directory') || h.includes('h5ai')) return;" +
                "let full = '';" +
                "try { full = new URL(h, base).href; } catch(e){ return; }" +
                "let isF = h.endsWith('/') || full.endsWith('/');" +
                "let name = decodeURIComponent(full.replace(/\\/$/, '').split('/').pop());" +
                "let isV = EXTS.some(x=>name.toLowerCase().endsWith(x));" +
                "if(isF || isV){" +
                "count++;" +
                "let d=document.createElement('div'); d.className='card'; d.tabIndex=0;" +
                "d.innerHTML='<span class=\"icon\">'+(isF?'📁':'▶️')+'</span><span class=\"name\">'+name+'</span>';" +
                "if(isF) d.onclick=()=>loadDir(full, name);" +
                "else d.onclick=()=>AndroidBridge.playVideoInVlc(full);" +
                "grid.appendChild(d);" +
                "}" +
                "});" +
                "if(count === 0) grid.innerHTML='No media found in this folder.';" +
                "};" +
                "window.onHtmlError = function(err){ grid.innerHTML='Network Error: '+err; };" +
                "AndroidBridge.loadLocalIndex();" +
                "loadHome();" +
                "</script></body></html>";

        webView.loadDataWithBaseURL("http://localhost/", html, "text/html", "UTF-8", null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            webView.loadUrl("javascript:loadHome()");
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
