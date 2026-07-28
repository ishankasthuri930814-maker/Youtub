package com.example.browser

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

object AdBlockEngine {
    private val AD_DOMAINS = listOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "adservice.google.com",
        "amazon-adsystem.com",
        "adform.net",
        "adnxs.com",
        "taboola.com",
        "outbrain.com",
        "pubmatic.com",
        "rubiconproject.com",
        "scorecardresearch.com",
        "pagead2.googlesyndication.com",
        "static.doubleclick.net",
        "yt3.ggpht.com/ads",
        "s.youtube.com/api/stats/ads"
    )

    fun isAdUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return AD_DOMAINS.any { lowerUrl.contains(it) } || 
               lowerUrl.contains("/pagead/") ||
               lowerUrl.contains("youtube.com/api/stats/ads") ||
               lowerUrl.contains("&ad_type=") ||
               lowerUrl.contains("&ad_cp=")
    }

    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
    }

    // JavaScript code injected into WebView to auto-skip ads, hide banner ads, and allow background playback
    val AD_BLOCK_AND_BACKGROUND_JS = """
        (function() {
            if (window.__stream_tube_injected__) return;
            window.__stream_tube_injected__ = true;

            // 1. Prevent YouTube from pausing video when tab or screen is hidden/off
            try {
                Object.defineProperty(document, 'hidden', { get: function() { return false; }, configurable: true });
                Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
                document.hasFocus = function() { return true; };
                
                // Block visibilitychange listeners
                var originalAddEventListener = EventTarget.prototype.addEventListener;
                EventTarget.prototype.addEventListener = function(type, listener, options) {
                    if (type === 'visibilitychange' || type === 'blur') {
                        return;
                    }
                    return originalAddEventListener.call(this, type, listener, options);
                };
            } catch(e) {
                console.log("Visibility override notice:", e);
            }

            // 2. Continuous Ad Skipper & Ad Cleaner loop
            function cleanAds() {
                try {
                    // Fast forward & skip YouTube video ads
                    var video = document.querySelector('video');
                    var adShowing = document.querySelector('.ad-showing, .ad-interrupting, .ytp-ad-player-overlay');
                    if (video && adShowing) {
                        video.muted = true;
                        if (!isNaN(video.duration) && video.duration > 0) {
                            video.currentTime = video.duration - 0.1;
                        }
                        video.playbackRate = 16.0;
                    }

                    // Auto click skip ad buttons
                    var skipButtons = document.querySelectorAll('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, .ytp-ad-skip-button-slot');
                    skipButtons.forEach(function(btn) {
                        if (btn && typeof btn.click === 'function') {
                            btn.click();
                        }
                    });

                    // Hide banner & overlay ads
                    var adSelectors = [
                        '.video-ads', '.ytp-ad-module', '#player-ads',
                        'ytd-promoted-sparkles-web-renderer', 'ytd-display-ad-renderer',
                        'ytd-banner-promo-renderer', '.ytp-ad-overlay-container',
                        '#masthead-ad', 'ytd-statement-banner-renderer',
                        'ytd-in-feed-ad-layout-renderer', '.ytp-ad-text'
                    ];
                    adSelectors.forEach(function(selector) {
                        var elements = document.querySelectorAll(selector);
                        elements.forEach(function(el) {
                            el.style.display = 'none';
                        });
                    });
                } catch(err) {
                    // Ignore transient DOM errors
                }
            }

            // Run ad cleaner every 500ms
            setInterval(cleanAds, 500);

            // Hook media session info to notify Native Android
            function observeMedia() {
                var video = document.querySelector('video');
                if (video) {
                    video.addEventListener('play', function() {
                        if (window.AndroidMediaBridge) {
                            var title = document.title || 'Playing Media';
                            window.AndroidMediaBridge.onMediaStateChanged(true, title, location.href);
                        }
                    });
                    video.addEventListener('pause', function() {
                        if (window.AndroidMediaBridge) {
                            var title = document.title || 'Paused Media';
                            window.AndroidMediaBridge.onMediaStateChanged(false, title, location.href);
                        }
                    });
                }
            }
            setInterval(observeMedia, 2000);
        })();
    """.trimIndent()
}
