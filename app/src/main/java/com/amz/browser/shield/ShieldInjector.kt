package com.amz.browser.shield

import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

object ShieldInjector {

    // TODO: Video-platform scriptlets (e.g. YouTube) require dedicated continually maintained
    // scriptlet rules and are intentionally out of scope for v1.

    private const val DOCUMENT_START_SCRIPT = """
        (function() {
          if (window.__amzShieldInstalled) return;
          window.__amzShieldInstalled = true;

          var origOpen = window.open;
          window.open = function() {
            if (!navigator.userActivation || !navigator.userActivation.isActive) {
              return null;
            }
            return origOpen.apply(window, arguments);
          };

          var style = document.createElement('style');
          style.textContent = [
            'ins.adsbygoogle, [id*="google_ads"], [id^="div-gpt-ad"],',
            'iframe[src*="doubleclick"], iframe[src*="googlesyndication"],',
            '.ad-banner, .sponsored-ad, .ad-slot, [data-ad-slot],',
            '#onetrust-consent-sdk, #CybotCookiebotDialog, .cookie-banner, .fc-consent-root {',
            '  display: none !important; visibility: hidden !important; pointer-events: none !important;',
            '}'
          ].join('
');
          (document.head || document.documentElement).appendChild(style);

          var interacted = false;
          ['pointerdown', 'keydown'].forEach(function(evt) {
            window.addEventListener(evt, function() { interacted = true; }, { passive: true });
          });

          function sweepOverlays() {
            if (interacted) return;
            var vw = window.innerWidth || 1;
            var vh = window.innerHeight || 1;
            var area = vw * vh;
            var nodes = document.querySelectorAll('div, section, aside');
            for (var i = 0; i < nodes.length; i++) {
              var el = nodes[i];
              var cs = window.getComputedStyle(el);
              if (cs.position !== 'fixed' && cs.position !== 'sticky') continue;
              var z = parseInt(cs.zIndex, 10) || 0;
              if (z < 900) continue;
              var rect = el.getBoundingClientRect();
              if ((rect.width * rect.height) / area > 0.60) {
                el.remove();
                if (document.body && document.body.style.overflow === 'hidden') {
                  document.body.style.overflow = '';
                }
                if (document.documentElement.style.overflow === 'hidden') {
                  document.documentElement.style.overflow = '';
                }
              }
            }
          }

          var observer = new MutationObserver(sweepOverlays);
          observer.observe(document.documentElement, { childList: true, subtree: true });
        })();
    """

    const val READER_MODE_SCRIPT = """
        (function() {
          if (window.__amzReaderBackup) {
            document.body.innerHTML = window.__amzReaderBackup;
            window.__amzReaderBackup = null;
            return 'restored';
          }
          var candidates = Array.from(document.querySelectorAll('article, main, section, div'));
          var best = document.body;
          var bestScore = 0;
          candidates.forEach(function(el) {
            var pCount = el.querySelectorAll('p').length;
            var textLen = (el.innerText || '').trim().length;
            var score = textLen + pCount * 120;
            if (score > bestScore && textLen > 240) {
              bestScore = score;
              best = el;
            }
          });
          var title = document.title || '';
          var paragraphs = Array.from(best.querySelectorAll('p, h1, h2, h3'))
            .map(function(n) { return n.outerHTML; })
            .join('');
          window.__amzReaderBackup = document.body.innerHTML;
          document.body.innerHTML =
            '<article style="max-width:680px;margin:48px auto 160px;padding:0 20px;' +
            'font-family:Georgia,serif;font-size:19px;line-height:1.75;">' +
            '<h1 style="font-size:28px;line-height:1.25;margin-bottom:20px;">' + title + '</h1>' +
            paragraphs + '</article>';
          return 'enabled';
        })();
    """

    fun attachDocumentStartShield(webView: WebView) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(
                webView,
                DOCUMENT_START_SCRIPT,
                setOf("*")
            )
        }
    }
}
