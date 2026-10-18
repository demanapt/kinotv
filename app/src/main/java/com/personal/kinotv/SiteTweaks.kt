package com.personal.kinotv

import android.webkit.WebView

/** Подгонка страниц сайта под телевизор: убираем баннеры и при желании меняем ширину страницы. */
object SiteTweaks {

    private const val CSS =
        "body,body.userLogged{margin-top:0!important}" +
            ".ad-branding,.kng-rekl,.kinogo-short,.app-download," +
            "ins[data-key],[id^=\"adangle-\"],a[href*=\"kinogo.luxury\"]" +
            "{display:none!important}"

    /** pageWidth = 0 оставляет ширину, которую выбрал сайт. */
    fun apply(view: WebView, pageWidth: Int) {
        val js = """
            (function(){
              if(!document.getElementById('kinotv-style')){
                var s=document.createElement('style');
                s.id='kinotv-style';
                s.textContent='$CSS';
                (document.head||document.documentElement).appendChild(s);
              }
              var w=$pageWidth;
              if(w>0&&document.head){
                var m=document.querySelector('meta[name=viewport]');
                if(!m){m=document.createElement('meta');m.name='viewport';document.head.appendChild(m);}
                var c='width='+w;
                if(m.content!==c)m.content=c;
              }
            })();
        """.trimIndent()
        view.evaluateJavascript(js, null)
    }
}
