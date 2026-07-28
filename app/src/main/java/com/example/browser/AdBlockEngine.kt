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
        "s.youtube.com/api/stats/ads",
        "youtube.com/pagead/"
    )

    fun isAdUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return AD_DOMAINS.any { lowerUrl.contains(it) } || 
               lowerUrl.contains("/pagead/") ||
               lowerUrl.contains("youtube.com/api/stats/ads") ||
               lowerUrl.contains("&ad_type=") ||
               lowerUrl.contains("&ad_cp=") ||
               lowerUrl.contains("googleadservices")
    }

    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
    }

    // JavaScript code injected into WebView to auto-skip ads, hide banner ads, and allow background playback
    val AD_BLOCK_AND_BACKGROUND_JS = """
        (function() {
            if (window.__stream_tube_injected__) return;
            window.__stream_tube_injected__ = true;

            // 1. Intercept Fetch and XMLHttpRequest to strip YouTube JSON ad payloads
            try {
                var origFetch = window.fetch;
                if (origFetch) {
                    window.fetch = function() {
                        var args = arguments;
                        var url = args[0] ? (typeof args[0] === 'string' ? args[0] : args[0].url) : '';
                        return origFetch.apply(this, args).then(function(response) {
                            if (url && (url.indexOf('/youtubei/v1/player') !== -1 || url.indexOf('/youtubei/v1/next') !== -1)) {
                                var clone = response.clone();
                                return clone.json().then(function(data) {
                                    if (data) {
                                        if (data.adPlacements) delete data.adPlacements;
                                        if (data.playerAds) delete data.playerAds;
                                        if (data.adSlots) delete data.adSlots;
                                        if (data.adBreakHeartbeatParams) delete data.adBreakHeartbeatParams;
                                    }
                                    return new Response(JSON.stringify(data), {
                                        status: response.status,
                                        statusText: response.statusText,
                                        headers: response.headers
                                    });
                                }).catch(function() { return response; });
                            }
                            return response;
                        });
                    };
                }
            } catch(e) {}

            // 2. Prevent YouTube from pausing video when tab or screen is hidden/off
            try {
                Object.defineProperties(document, {
                    'hidden': { get: function() { return false; }, configurable: true },
                    'visibilityState': { get: function() { return 'visible'; }, configurable: true },
                    'webkitHidden': { get: function() { return false; }, configurable: true },
                    'webkitVisibilityState': { get: function() { return 'visible'; }, configurable: true }
                });
                document.hasFocus = function() { return true; };
                
                var blockVisibility = function(e) {
                    if (['visibilitychange', 'webkitvisibilitychange', 'blur', 'pagehide', 'freeze'].indexOf(e.type) !== -1) {
                        e.stopImmediatePropagation();
                        e.stopPropagation();
                    }
                };
                window.addEventListener('visibilitychange', blockVisibility, true);
                document.addEventListener('visibilitychange', blockVisibility, true);
                window.addEventListener('blur', blockVisibility, true);
                window.addEventListener('pagehide', blockVisibility, true);
            } catch(e) {}

            // 3. Continuous Ad Skipper & Ad Cleaner loop
            function cleanAds() {
                try {
                    // Fast forward & skip YouTube video ads
                    var video = document.querySelector('video');
                    var adShowing = document.querySelector('.ad-showing, .ad-interrupting, .ytp-ad-player-overlay, ytm-promoted-item-renderer');
                    if (video && adShowing) {
                        video.muted = true;
                        if (!isNaN(video.duration) && video.duration > 0) {
                            video.currentTime = video.duration - 0.05;
                        }
                        video.playbackRate = 16.0;
                    }

                    // Auto click skip ad buttons
                    var skipSelectors = [
                        '.ytp-ad-skip-button', '.ytp-ad-skip-button-modern', 
                        '.ytp-skip-ad-button', '.ytp-ad-skip-button-slot',
                        'button.ytp-ad-skip-button', '.ytp-ad-text.ytp-ad-skip-button-text'
                    ];
                    skipSelectors.forEach(function(sel) {
                        var btns = document.querySelectorAll(sel);
                        btns.forEach(function(btn) {
                            if (btn && typeof btn.click === 'function') {
                                btn.click();
                            }
                        });
                    });

                    // Hide banner & overlay ads
                    var adSelectors = [
                        '.video-ads', '.ytp-ad-module', '#player-ads',
                        'ytd-promoted-sparkles-web-renderer', 'ytd-display-ad-renderer',
                        'ytd-banner-promo-renderer', '.ytp-ad-overlay-container',
                        '#masthead-ad', 'ytd-statement-banner-renderer',
                        'ytd-in-feed-ad-layout-renderer', '.ytp-ad-text',
                        'ytm-companion-ad-renderer', 'ytm-promoted-item-renderer',
                        'ad-slot-renderer', '#ad-badge'
                    ];
                    adSelectors.forEach(function(selector) {
                        var elements = document.querySelectorAll(selector);
                        elements.forEach(function(el) {
                            el.style.display = 'none';
                        });
                    });
                } catch(err) {}
            }

            // Run ad cleaner aggressively every 150ms
            setInterval(cleanAds, 150);

            // 4. Hook media playback events to send states to Native Android
            function observeMedia() {
                var video = document.querySelector('video');
                if (video) {
                    if (!video.__stream_observed__) {
                        video.__stream_observed__ = true;
                        
                        var notifyState = function() {
                            if (window.AndroidMediaBridge) {
                                var title = document.title || 'Playing Media';
                                window.AndroidMediaBridge.onMediaStateChanged(!video.paused, title, location.href);
                            }
                        };

                        video.addEventListener('play', notifyState);
                        video.addEventListener('playing', notifyState);
                        video.addEventListener('pause', notifyState);
                        video.addEventListener('timeupdate', function() {
                            if (!video.paused && window.AndroidMediaBridge) {
                                var title = document.title || 'Playing Media';
                                window.AndroidMediaBridge.onMediaStateChanged(true, title, location.href);
                            }
                        });
                    }
                }
            }
            setInterval(observeMedia, 1000);
        })();
    """.trimIndent()
}

