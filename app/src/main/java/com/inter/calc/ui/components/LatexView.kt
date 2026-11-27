package com.inter.calc.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

private fun Color.toCssHex(): String = String.format("#%06X", 0xFFFFFF and this.toArgb())

/**
 * 离线 KaTeX 单行公式渲染组件：
 * 当公式较宽时自动适度缩放并支持横向滑动，避免右侧内容被裁剪。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LatexFormulaView(
    latex: String,
    textColor: Color,
    fontSizePx: Int = 16,
    modifier: Modifier = Modifier,
) {
    val colorHex = remember(textColor) { textColor.toCssHex() }
    var isLoaded by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val baseHtml = remember {
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0" />
          <link rel="stylesheet" href="katex.min.css" />
          <script src="katex.min.js"></script>
          <style>
            html, body {
              margin: 0; padding: 0;
              width: 100%;
              background: transparent;
              overflow-x: auto;
              overflow-y: hidden;
              -webkit-text-size-adjust: none;
            }
            #wrap {
              width: 100%;
              overflow-x: auto;
              overflow-y: hidden;
            }
            #math {
              display: inline-block;
              white-space: nowrap;
              padding: 3px 2px;
              line-height: 1.35;
              transform-origin: left center;
            }
            .katex { font-size: 1.02em; }
          </style>
        </head>
        <body>
          <div id="wrap"><div id="math"></div></div>
          <script>
            function setFormula(tex, color, sizePx) {
              var el = document.getElementById('math');
              var wrap = document.getElementById('wrap');
              el.style.color = color;
              el.style.fontSize = sizePx + 'px';
              if (!tex) { el.innerHTML = ''; return; }
              try {
                katex.render(tex, el, { throwOnError: false, displayMode: false, strict: false });
              } catch (e) {
                el.textContent = tex;
              }
              // 略微超出宽度时自动微调字号，保证完整可见（最低 11.5px，再长可横滑）
              var cw = wrap.clientWidth || window.innerWidth;
              var sw = el.scrollWidth;
              if (cw > 0 && sw > cw) {
                var fitted = Math.max(11.5, Math.floor(sizePx * (cw / sw) * 10) / 10);
                el.style.fontSize = fitted + 'px';
              }
            }
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    LaunchedEffect(latex, colorHex, fontSizePx, isLoaded) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (isLoaded) {
            wv.evaluateJavascript("setFormula(${JSONObject.quote(latex)}, ${JSONObject.quote(colorHex)}, $fontSizePx);", null)
        }
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        isLoaded = true
                        view?.evaluateJavascript("setFormula(${JSONObject.quote(latex)}, ${JSONObject.quote(colorHex)}, $fontSizePx);", null)
                    }
                }
                loadDataWithBaseURL("file:///android_asset/katex/", baseHtml, "text/html", "UTF-8", null)
                webViewRef = this
            }
        },
        update = { wv ->
            if (isLoaded) {
                wv.evaluateJavascript("setFormula(${JSONObject.quote(latex)}, ${JSONObject.quote(colorHex)}, $fontSizePx);", null)
            }
        },
    )
}

/**
 * 离线 KaTeX 混排渲染组件，用于 DeepSeek 独立界面的多行公式与极简中文展示：
 * 自动将过长公式块适配屏幕宽度并支持横向滑动，杜绝公式右侧被裁切。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LatexMarkdownView(
    markdown: String,
    textColor: Color,
    accentColor: Color,
    fontSizePx: Int = 15,
    modifier: Modifier = Modifier,
) {
    val colorHex = remember(textColor) { textColor.toCssHex() }
    val accentHex = remember(accentColor) { accentColor.toCssHex() }
    var isLoaded by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val baseHtml = remember {
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0" />
          <link rel="stylesheet" href="katex.min.css" />
          <script src="katex.min.js"></script>
          <script src="auto-render.min.js"></script>
          <style>
            html, body {
              margin: 0; padding: 0;
              width: 100%;
              background: transparent;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
              word-wrap: break-word;
              -webkit-text-size-adjust: none;
            }
            #content {
              line-height: 1.65;
              padding: 2px 0;
            }
            .katex-display {
              margin: 8px 0 !important;
              overflow-x: auto;
              overflow-y: hidden;
              padding: 4px 0;
              text-align: left !important;
            }
            .katex-display > .katex {
              text-align: left !important;
              white-space: nowrap;
            }
            .method-title {
              font-weight: 700;
              font-size: 1.05em;
              margin-top: 12px;
              margin-bottom: 4px;
            }
            .method-title:first-child { margin-top: 0; }
          </style>
        </head>
        <body>
          <div id="content"></div>
          <script>
            function escapeHtml(str) {
              return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
            }
            function setContent(raw, color, accent, sizePx) {
              var el = document.getElementById('content');
              el.style.color = color;
              el.style.fontSize = sizePx + 'px';
              if (!raw) { el.innerHTML = ''; return; }

              var mathSlots = [];
              var protectedStr = raw
                .replace(/\$\$[\s\S]*?\$\$/g, function(m) {
                  mathSlots.push(m); return '@@MATH' + (mathSlots.length - 1) + '@@';
                })
                .replace(/\\\[[\s\S]*?\\\]/g, function(m) {
                  mathSlots.push(m); return '@@MATH' + (mathSlots.length - 1) + '@@';
                })
                .replace(/\\\([\s\S]*?\\\)/g, function(m) {
                  mathSlots.push(m); return '@@MATH' + (mathSlots.length - 1) + '@@';
                })
                .replace(/\$(?!\s)([^\$\n]+?)(?!\s)\$/g, function(m) {
                  mathSlots.push(m); return '@@MATH' + (mathSlots.length - 1) + '@@';
                });

              var safe = escapeHtml(protectedStr);
              safe = safe.replace(/^###\s+(.+)$/gm, '<div class="method-title" style="color:' + accent + '">$1</div>');
              safe = safe.replace(/^\*\*(.+?)\*\*$/gm, '<div class="method-title" style="color:' + accent + '">$1</div>');
              safe = safe.replace(/\*\*(.+?)\*\*/g, '<strong style="color:' + accent + '">$1</strong>');
              safe = safe.replace(/\n/g, '<br/>');

              safe = safe.replace(/@@MATH(\d+)@@/g, function(_, idx) {
                return escapeHtml(mathSlots[parseInt(idx, 10)]);
              });

              el.innerHTML = safe;
              try {
                renderMathInElement(el, {
                  delimiters: [
                    { left: '$$', right: '$$', display: true },
                    { left: '\\[', right: '\\]', display: true },
                    { left: '$', right: '$', display: false },
                    { left: '\\(', right: '\\)', display: false }
                  ],
                  throwOnError: false,
                  strict: false
                });
                // 对过宽的块级公式自动适度缩小，确保手机屏清晰完整显示
                var cw = el.clientWidth || window.innerWidth;
                var blocks = el.querySelectorAll('.katex-display');
                for (var i = 0; i < blocks.length; i++) {
                  var inner = blocks[i].querySelector('.katex');
                  if (inner && cw > 0 && inner.scrollWidth > cw) {
                    var ratio = Math.max(0.76, cw / inner.scrollWidth);
                    inner.style.fontSize = (ratio * 100).toFixed(1) + '%';
                  }
                }
              } catch (e) {}
            }
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    LaunchedEffect(markdown, colorHex, accentHex, fontSizePx, isLoaded) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (isLoaded) {
            wv.evaluateJavascript("setContent(${JSONObject.quote(markdown)}, ${JSONObject.quote(colorHex)}, ${JSONObject.quote(accentHex)}, $fontSizePx);", null)
        }
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                isVerticalScrollBarEnabled = false
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        isLoaded = true
                        view?.evaluateJavascript("setContent(${JSONObject.quote(markdown)}, ${JSONObject.quote(colorHex)}, ${JSONObject.quote(accentHex)}, $fontSizePx);", null)
                    }
                }
                loadDataWithBaseURL("file:///android_asset/katex/", baseHtml, "text/html", "UTF-8", null)
                webViewRef = this
            }
        },
        update = { wv ->
            if (isLoaded) {
                wv.evaluateJavascript("setContent(${JSONObject.quote(markdown)}, ${JSONObject.quote(colorHex)}, ${JSONObject.quote(accentHex)}, $fontSizePx);", null)
            }
        },
    )
}
