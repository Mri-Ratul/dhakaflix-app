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

            @JavascriptInterface
            public void playVideo(String videoUrl, String title) {
                runOnUiThread(() -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndTypeAndNormalize(Uri.parse(videoUrl), "video/*");
                        intent.putExtra(Intent.EXTRA_TITLE, title);
                        startActivity(Intent.createChooser(intent, "প্লেয়ার অথবা টিভিতে কাস্ট নির্বাচন করুন:"));
                    } catch (Exception ex) {
                        Toast.makeText(MainActivity.this, "ভিডিও প্লেয়ার পাওয়া যায়নি!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }, "AndroidBridge");

        webView.setWebViewClient(new WebViewClient());
        loadAppHtml();
    }

    private void loadAppHtml() {
        String html = "<!DOCTYPE html>" +
                "<html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1,user-scalable=no'>" +
                "<style>" +
                "* { box-sizing: border-box; margin:0; padding:0; -webkit-tap-highlight-color: transparent; }" +
                "body { background: #f8fafc; color: #0f172a; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; padding: 16px; }" +
                "header { margin-bottom: 16px; }" +
                ".top-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }" +
                ".logo { font-size: 24px; font-weight: 800; color: #1e1b4b; letter-spacing: -0.5px; cursor: pointer; }" +
                ".logo span { color: #6366f1; }" +
                ".badge { font-size: 11px; font-weight: 600; padding: 4px 10px; border-radius: 20px; background: #e0e7ff; color: #4338ca; }" +
                ".search-wrapper { position: relative; width: 100%; }" +
                ".search-input { width: 100%; padding: 12px 16px; background: #ffffff; border: 1.5px solid #e2e8f0; color: #0f172a; border-radius: 12px; font-size: 15px; outline: none; box-shadow: 0 2px 6px rgba(0,0,0,0.03); transition: all 0.2s ease; }" +
                ".search-input:focus { border-color: #6366f1; box-shadow: 0 4px 12px rgba(99,102,241,0.12); }" +
                ".breadcrumb { font-size: 13px; color: #64748b; margin-bottom: 14px; font-weight: 500; display:flex; gap:6px; align-items:center; }" +
                ".breadcrumb span { color: #6366f1; cursor: pointer; }" +
                ".grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(165px, 1fr)); gap: 12px; }" +
                ".card { background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 14px; display: flex; align-items: center; gap: 12px; cursor: pointer; box-shadow: 0 1px 3px rgba(0,0,0,0.04); transition: transform 0.15s ease, box-shadow 0.15s ease; }" +
                ".card:active { transform: scale(0.98); background: #f1f5f9; }" +
                ".icon-wrap { width: 36px; height: 36px; border-radius: 8px; background: #f1f5f9; display: flex; align-items: center; justify-content: center; font-size: 18px; flex-shrink: 0; }" +
                ".name { font-size: 13px; font-weight: 600; color: #334155; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }" +
                ".state-msg { text-align: center; padding: 40px 20px; color: #64748b; font-size: 14px; }" +
                "</style></head><body>" +
                "<header>" +
                "<div class='top-bar'>" +
                "<div class='logo' onclick='loadHome()'>Ratul <span>Flix</span></div>" +
                "<div id='idxBadge' class='badge'>Loading Index...</div>" +
                "</div>" +
                "<div class='search-wrapper'>" +
                "<input type='text' id='sInput' class='search-input' placeholder='Search' autocomplete='off' />" +
                "</div>" +
                "</header>" +
                "<div id='bc' class='breadcrumb'></div>" +
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
                "const BLOCKED = ['browsehappy', 'brave.com', 'wordpress.org', 'mozilla', 'apple.com', 'google.com', 'h5ai'];" +
                "const grid = document.getElementById('grid');" +
                "const sInput = document.getElementById('sInput');" +
                "const idxBadge = document.getElementById('idxBadge');" +
                "function loadHome(){" +
                "sInput.value = '';" +
                "document.getElementById('bc').innerHTML='<span>Home</span>';" +
                "grid.innerHTML='';" +
                "CATS.forEach(c=>{" +
                "let d=document.createElement('div'); d.className='card';" +
                "d.innerHTML='<div class=\"icon-wrap\">'+c.icon+'</div><div class=\"name\">'+c.name+'</div>';" +
                "d.onclick=()=>loadDir(c.url, c.name);" +
                "grid.appendChild(d);" +
                "});" +
                "}" +
                "function loadDir(u, title){" +
                "grid.innerHTML='<div class=\"state-msg\">ফোল্ডার লোড হচ্ছে...</div>';" +
                "document.getElementById('bc').innerHTML='<span onclick=\"loadHome()\">Home</span> / <b>'+title+'</b>';" +
                "AndroidBridge.fetchHtml(u);" +
                "}" +
                "sInput.addEventListener('input', function(){" +
                "let q = this.value.trim().toLowerCase();" +
                "if(!q){ loadHome(); return; }" +
                "if(!movieDatabase || movieDatabase.length === 0) return;" +
                "document.getElementById('bc').innerHTML='Search Results for: <b>'+q+'</b>';" +
                "grid.innerHTML='';" +
                "let hits = movieDatabase.filter(m => ((m.name||'').toLowerCase().includes(q))).slice(0, 100);" +
                "if(hits.length === 0){ grid.innerHTML='<div class=\"state-msg\">কিছু পাওয়া যায়নি!</div>'; return; }" +
                "hits.forEach(m=>{" +
                "let title = m.name || 'Unknown';" +
                "let playUrl = m.url || '';" +
                "let isF = m.is_folder || playUrl.endsWith('/') || !EXTS.some(x=>playUrl.toLowerCase().endsWith(x));" +
                "let d=document.createElement('div'); d.className='card';" +
                "d.innerHTML='<div class=\"icon-wrap\">'+(isF?'📁':'▶️')+'</div><div class=\"name\">'+title+'</div>';" +
                "if(isF) d.onclick=()=>loadDir(playUrl, title);" +
                "else d.onclick=()=>AndroidBridge.playVideo(playUrl, title);" +
                "grid.appendChild(d);" +
                "});" +
                "});" +
                "window.onIndexLoaded = function(data){" +
                "movieDatabase = data;" +
                "idxBadge.innerText = 'Online ('+movieDatabase.length.toLocaleString()+')';" +
                "idxBadge.style.background = '#dcfce7'; idxBadge.style.color = '#15803d';" +
                "};" +
                "window.onIndexError = function(){" +
                "idxBadge.innerText = 'Index Missing';" +
                "idxBadge.style.background = '#fee2e2'; idxBadge.style.color = '#b91c1c';" +
                "};" +
                "window.onHtmlLoaded = function(txt, base){" +
                "let doc = new DOMParser().parseFromString(txt, 'text/html');" +
                "let links = Array.from(doc.querySelectorAll('a'));" +
                "grid.innerHTML='';" +
                "let count = 0;" +
                "links.forEach(a=>{" +
                "let h = a.getAttribute('href');" +
                "if(!h || h.startsWith('?') || h==='/' || h==='../' || h.toLowerCase().includes('parent directory')) return;" +
                "let full = '';" +
                "try { full = new URL(h, base).href; } catch(e){ return; }" +
                "let fullLow = full.toLowerCase();" +
                "if(BLOCKED.some(b => fullLow.includes(b))) return;" +
                "let isF = h.endsWith('/') || full.endsWith('/');" +
                "let name = decodeURIComponent(full.replace(/\\/$/, '').split('/').pop());" +
                "if(BLOCKED.some(b => name.toLowerCase().includes(b))) return;" +
                "let isV = EXTS.some(x=>name.toLowerCase().endsWith(x));" +
                "if(isF || isV){" +
                "count++;" +
                "let d=document.createElement('div'); d.className='card';" +
                "d.innerHTML='<div class=\"icon-wrap\">'+(isF?'📁':'▶️')+'</div><div class=\"name\">'+name+'</div>';" +
                "if(isF) d.onclick=()=>loadDir(full, name);" +
                "else d.onclick=()=>AndroidBridge.playVideo(full, name);" +
                "grid.appendChild(d);" +
                "}" +
                "});" +
                "if(count === 0) grid.innerHTML='<div class=\"state-msg\">এই ফোল্ডারে কোনো মিডিয়া নেই।</div>';" +
                "};" +
                "window.onHtmlError = function(err){ grid.innerHTML='<div class=\"state-msg\" style=\"color:#ef4444;\">নেটওয়ার্ক এরর: '+err+'</div>'; };" +
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
