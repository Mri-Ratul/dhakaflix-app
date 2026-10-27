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

        // অ্যাপের জাভাস্ক্রিপ্ট থেকে সরাসরি VLC কল করার ব্রিজ
        webView.addJavascriptInterface(new Object() {
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
                "h1 { color: #38bdf8; font-size: 20px; margin-bottom: 12px; }" +
                ".grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 10px; }" +
                ".card { background: #1e293b; border: 1px solid #334155; border-radius: 8px; padding: 12px; display:flex; align-items:center; gap:10px; cursor:pointer; }" +
                ":focus { outline: 3px solid #38bdf8; background: #334155; }" +
                ".icon { font-size: 22px; }" +
                ".name { font-size: 13px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }" +
                "</style></head><body>" +
                "<h1 onclick='loadHome()'>DhakaFlix Quick Hub</h1>" +
                "<div id='bc' style='margin-bottom:12px; font-size:13px; color:#94a3b8;'></div>" +
                "<div id='grid' class='grid'></div>" +
                "<script>" +
                "const CATS = [" +
                "{name:'TV & Series', url:'http://172.16.50.12/DHAKA-FLIX-12/TV-WEB-Series/', icon:'📺'}," +
                "{name:'English Movies', url:'http://172.16.50.7/DHAKA-FLIX-7/English%20Movies/', icon:'🎬'}," +
                "{name:'English (1080p)', url:'http://172.16.50.14/DHAKA-FLIX-14/English%20Movies%20%281080p%29/', icon:'🍿'}," +
                "{name:'IMDb Top-250', url:'http://172.16.50.14/DHAKA-FLIX-14/IMDb%20Top-250%20Movies/', icon:'⭐'}," +
                "{name:'Hindi Movies', url:'http://172.16.50.14/DHAKA-FLIX-14/Hindi%20Movies/', icon:'🎭'}," +
                "{name:'Animation', url:'http://172.16.50.14/DHAKA-FLIX-14/Animation%20Movies/', icon:'🎨'}," +
                "{name:'Anime & Cartoon', url:'http://172.16.50.10/DHAKA-FLIX-10/Anime%20%26%20Cartoon%20TV%20Series/', icon:'⚡'}" +
                "];" +
                "const EXTS = ['.mkv','.mp4','.avi','.webm','.m4v','.ts'];" +
                "const grid = document.getElementById('grid');" +
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
                "async function loadDir(u, title){" +
                "grid.innerHTML='Loading...';" +
                "document.getElementById('bc').innerHTML='<span onclick=\"loadHome()\" style=\"color:#38bdf8;cursor:pointer;\">Home</span> / <b>'+title+'</b>';" +
                "try {" +
                "let res = await fetch(u);" +
                "let txt = await res.text();" +
                "let doc = new DOMParser().parseFromString(txt, 'text/html');" +
                "let links = Array.from(doc.querySelectorAll('a'));" +
                "grid.innerHTML='';" +
                "links.forEach(a=>{" +
                "let h = a.getAttribute('href');" +
                "if(!h || h.startsWith('?') || h==='/' || h==='../' || h.includes('parent directory')) return;" +
                "let full = new URL(h, u).href;" +
                "let isF = h.endsWith('/') || full.endsWith('/');" +
                "let name = decodeURIComponent(full.replace(/\\/$/, '').split('/').pop());" +
                "let isV = EXTS.some(x=>name.toLowerCase().endsWith(x));" +
                "if(isF || isV){" +
                "let d=document.createElement('div'); d.className='card'; d.tabIndex=0;" +
                "d.innerHTML='<span class=\"icon\">'+(isF?'📁':'▶️')+'</span><span class=\"name\">'+name+'</span>';" +
                "if(isF) d.onclick=()=>loadDir(full, name);" +
                "else d.onclick=()=>AndroidBridge.playVideoInVlc(full);" +
                "grid.appendChild(d);" +
                "}" +
                "});" +
                "} catch(e){ grid.innerHTML='Load failed!'; }" +
                "}" +
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
