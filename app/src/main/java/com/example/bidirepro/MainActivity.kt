package com.example.bidirepro

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text as M3Text

/**
 * Minimal reproduction for: RTL paragraph context lost when Arabic content
 * begins with a Latin strong character.
 *
 * The Activity forces layoutDirection = RTL on the root view, so the whole
 * screen runs in the same RTL context an Arabic system locale produces. No
 * locale change is required to reproduce.
 *
 * Every pair on screen shares a byte-identical tail and differs only by the
 * leading Arabic word "شركة". The first strong character is the only variable.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Same RTL context an Arabic locale establishes.
        findViewById<View>(R.id.root).layoutDirection = View.LAYOUT_DIRECTION_RTL

        val shortLatinFirst = getString(R.string.short_latin_first)
        val longArabicFirst = getString(R.string.long_arabic_first)
        val longLatinFirst = getString(R.string.long_latin_first)

        findViewById<ComposeView>(R.id.compose).setContent {
            // Compose equivalent of the RTL layout direction set above.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ComposeSection(
                    shortLatinFirst = shortLatinFirst,
                    longArabicFirst = longArabicFirst,
                    longLatinFirst = longLatinFirst
                )
            }
        }

        // dir="auto" resolves from the first strong character — the WebView/CSS
        // equivalent of firstStrong, and the behaviour visible on the Gemini
        // answer surface.
        findViewById<WebView>(R.id.web_auto).loadHtml(page("auto", longLatinFirst))
        findViewById<WebView>(R.id.web_rtl).loadHtml(page("rtl", longLatinFirst))
    }

    private fun WebView.loadHtml(html: String) =
        loadDataWithBaseURL(null, html, "text/html", "utf-8", null)

    private fun page(dir: String, text: String) = """
        <!doctype html>
        <html>
        <head><meta name="viewport" content="width=device-width, initial-scale=1"></head>
        <body style="margin:8px;font-family:sans-serif;font-size:16px;background:#F5F5F5">
          <div dir="$dir" style="border:1px solid #BDBDBD;border-radius:4px;padding:8px;background:#FFF">$text</div>
        </body>
        </html>
    """.trimIndent()
}

@Composable
private fun ComposeSection(
    shortLatinFirst: String,
    longArabicFirst: String,
    longLatinFirst: String
) {
    Column(Modifier.fillMaxWidth()) {

        Section("4 — Jetpack Compose")

        Label("Arabic-first, default TextStyle.textDirection = Content — CORRECT")
        Sample { M3Text(longArabicFirst, fontSize = 16.sp) }

        Label("Latin-first, default TextStyle.textDirection = Content — DEFECT")
        Sample { M3Text(longLatinFirst, fontSize = 16.sp) }

        Label("Latin-first + TextStyle(textDirection = TextDirection.Rtl) — MITIGATION")
        Sample {
            M3Text(
                longLatinFirst,
                fontSize = 16.sp,
                style = TextStyle(textDirection = TextDirection.Rtl)
            )
        }

        Label("BasicTextField, Latin-first, default — DEFECT (tap at the end and compare caret side)")
        var a by remember { mutableStateOf(shortLatinFirst) }
        Sample {
            BasicTextField(
                value = a,
                onValueChange = { a = it },
                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Label("BasicTextField + TextDirection.Rtl — MITIGATION")
        var b by remember { mutableStateOf(shortLatinFirst) }
        Sample {
            BasicTextField(
                value = b,
                onValueChange = { b = it },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = Color.Black,
                    textDirection = TextDirection.Rtl
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Section(text: String) = CompositionLocalProvider(
    LocalLayoutDirection provides LayoutDirection.Ltr
) {
    M3Text(
        text = text,
        fontSize = 15.sp,
        color = Color.Black,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun Label(text: String) = CompositionLocalProvider(
    LocalLayoutDirection provides LayoutDirection.Ltr
) {
    M3Text(
        text = text,
        fontSize = 12.sp,
        color = Color(0xFF666666),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp)
    )
}

@Composable
private fun Sample(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFBDBDBD))
            .background(Color.White)
            .padding(8.dp)
    ) { content() }
}
