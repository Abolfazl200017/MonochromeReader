package com.example.monochromereader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import org.json.JSONArray
import org.json.JSONObject

data class Book(val uri: String, val title: String, val mime: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReaderApp() }
    }
}

@Composable
fun ReaderApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("reader", 0) }
    var isDark by remember { mutableStateOf(prefs.getBoolean("dark", false)) }
    var language by remember { mutableStateOf(prefs.getString("lang", "en") ?: "en") }
    var screen by remember { mutableStateOf("library") }
    var books by remember { mutableStateOf(loadBooks(prefs)) }
    var selected by remember { mutableStateOf<Book?>(null) }
    var fontSize by remember { mutableFloatStateOf(prefs.getFloat("fontSize", 20f)) }
    var fontName by remember { mutableStateOf(prefs.getString("font", "Serif") ?: "Serif") }
    var orientation by remember { mutableStateOf(prefs.getString("orientation", "System") ?: "System") }
    var pageText by remember { mutableStateOf("") }
    var pageIndex by remember { mutableIntStateOf(0) }
    var highlights by remember { mutableStateOf(prefs.getStringSet("highlights", emptySet())?.toSet() ?: emptySet()) }
    val darkBg = Color(0xFF111111)
    val lightBg = Color(0xFFF7F7F7)
    val bg = if (isDark) darkBg else lightBg
    val fg = if (isDark) Color(0xFFF2F2F2) else Color(0xFF171717)
    val muted = if (isDark) Color(0xFFAAAAAA) else Color(0xFF666666)

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            val name = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Imported book"
            val mime = context.contentResolver.getType(uri) ?: ""
            val book = Book(uri.toString(), name, mime)
            if (books.none { it.uri == book.uri }) {
                books = books + book
                saveBooks(prefs, books)
            }
        }
    }

    MaterialTheme(colorScheme = if (isDark) darkColorScheme(
        background = darkBg, surface = Color(0xFF202020), onBackground = fg, onSurface = fg,
        primary = Color.White, secondary = Color.LightGray
    ) else lightColorScheme(
        background = lightBg, surface = Color.White, onBackground = fg, onSurface = fg,
        primary = Color.Black, secondary = Color.DarkGray
    )) {
        Surface(Modifier.fillMaxSize(), color = bg) {
            CompositionLocalProvider(LocalLayoutDirection provides if (language == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr) {
                when (screen) {
                    "reader" -> ReaderScreen(
                        book = selected, isDark = isDark, fg = fg, muted = muted,
                        fontSize = fontSize, fontName = fontName, pageText = pageText,
                        pageIndex = pageIndex, highlights = highlights,
                        onBack = { screen = "library" },
                        onPrev = { if (pageIndex > 0) pageIndex--; pageText = loadBookText(context, selected, pageIndex) },
                        onNext = { pageIndex++; pageText = loadBookText(context, selected, pageIndex) },
                        onHighlight = {
                            val item = pageText.take(180)
                            highlights = highlights + item
                            prefs.edit().putStringSet("highlights", highlights).apply()
                        },
                        onSettings = { screen = "settings" }
                    )
                    "settings" -> SettingsScreen(
                        fg, muted, bg, isDark, fontSize, fontName, orientation, language,
                        onBack = { screen = "library" },
                        onDark = { isDark = it; prefs.edit().putBoolean("dark", it).apply() },
                        onSize = { fontSize = it; prefs.edit().putFloat("fontSize", it).apply() },
                        onFont = { fontName = it; prefs.edit().putString("font", it).apply() },
                        onOrientation = { orientation = it; prefs.edit().putString("orientation", it).apply() },
                        onLanguage = { language = it; prefs.edit().putString("lang", it).apply() }
                    )
                    "account" -> AccountScreen(fg, muted, onBack = { screen = "library" })
                    "plus" -> FidiplusScreen(fg, muted, onBack = { screen = "library" })
                    else -> LibraryScreen(
                        books = books, fg = fg, muted = muted, isDark = isDark, language = language,
                        onImport = { importLauncher.launch(arrayOf("application/epub+zip", "application/pdf", "application/octet-stream")) },
                        onOpen = { book ->
                            selected = book; pageIndex = 0
                            pageText = loadBookText(context, book, 0)
                            screen = "reader"
                        },
                        onAccount = { screen = "account" },
                        onPlus = { screen = "plus" },
                        onSettings = { screen = "settings" }
                    )
                }
            }
        }
    }
}

@Composable
fun Header(title: String, fg: Color, muted: Color, back: (() -> Unit)? = null, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (back != null) Text("‹", Modifier.clickable { back() }.padding(end = 14.dp), color = fg, fontSize = 30.sp)
        Text(title, color = fg, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun LibraryScreen(
    books: List<Book>, fg: Color, muted: Color, isDark: Boolean, language: String,
    onImport: () -> Unit, onOpen: (Book) -> Unit, onAccount: () -> Unit, onPlus: () -> Unit, onSettings: () -> Unit
) {
    val fa = language == "fa"
    Column(Modifier.fillMaxSize()) {
        Header(if (fa) "کتابخانه من" else "My library", fg, muted, action = {
            Text(if (fa) "ورود" else "Account", color = fg, modifier = Modifier.clickable { onAccount() }.padding(8.dp))
        })
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = fg)) {
                Text(if (fa) "افزودن کتاب" else "Import book")
            }
            OutlinedButton(onClick = onPlus, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = fg)) {
                Text("Fidiplus")
            }
        }
        if (books.isEmpty()) {
            Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("▤", color = muted, fontSize = 54.sp)
                    Text(if (fa) "هنوز کتابی اضافه نکرده‌اید" else "Your library is empty", color = fg, fontSize = 18.sp)
                    Text(if (fa) "فایل EPUB یا PDF خود را وارد کنید" else "Import an EPUB or PDF file to get started", color = muted, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(books) { book ->
                    Row(
                        Modifier.fillMaxWidth().background(if (isDark) Color(0xFF242424) else Color.White, RoundedCornerShape(12.dp))
                            .clickable { onOpen(book) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(width = 48.dp, height = 64.dp).background(if (isDark) Color(0xFF444444) else Color(0xFFE5E5E5), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                            Text("▤", color = fg, fontSize = 24.sp)
                        }
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(book.title, color = fg, fontWeight = FontWeight.Medium, maxLines = 2)
                            Text(if (book.mime.contains("pdf")) "PDF" else "EPUB / document", color = muted, fontSize = 12.sp)
                        }
                        Text("›", color = muted, fontSize = 26.sp)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().border(0.5.dp, muted.copy(alpha = .3f)).padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text(if (fa) "کتابخانه" else "Library", color = fg)
            Text("Fidiplus", color = muted, modifier = Modifier.clickable { onPlus() })
            Text(if (fa) "تنظیمات" else "Settings", color = muted, modifier = Modifier.clickable { onSettings() })
        }
    }
}

@Composable
fun ReaderScreen(
    book: Book?, isDark: Boolean, fg: Color, muted: Color, fontSize: Float, fontName: String,
    pageText: String, pageIndex: Int, highlights: Set<String>, onBack: () -> Unit,
    onPrev: () -> Unit, onNext: () -> Unit, onHighlight: () -> Unit, onSettings: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Header(book?.title ?: "Reader", fg, muted, back = onBack, action = {
            Text("Aa", color = fg, modifier = Modifier.clickable { onSettings() }.padding(8.dp))
        })
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.End) {
            Text("Page ${pageIndex + 1}", color = muted, fontSize = 12.sp)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            SelectionContainer {
                Text(
                    text = if (pageText.isBlank()) "This file could not be previewed yet. Try importing an EPUB or PDF document." else pageText,
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp, vertical = 28.dp),
                    color = fg, fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.65f).sp,
                    fontFamily = when (fontName) { "Sans" -> FontFamily.SansSerif; "Monospace" -> FontFamily.Monospace; else -> FontFamily.Serif }
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Previous", color = fg, modifier = Modifier.clickable { onPrev() }.padding(8.dp))
            Text("Highlight", color = fg, modifier = Modifier.clickable { onHighlight() }.padding(8.dp))
            Text("Next", color = fg, modifier = Modifier.clickable { onNext() }.padding(8.dp))
        }
    }
}

@Composable
fun SettingsScreen(
    fg: Color, muted: Color, bg: Color, isDark: Boolean, fontSize: Float, fontName: String,
    orientation: String, language: String, onBack: () -> Unit, onDark: (Boolean) -> Unit,
    onSize: (Float) -> Unit, onFont: (String) -> Unit, onOrientation: (String) -> Unit, onLanguage: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header(if (language == "fa") "تنظیمات" else "Settings", fg, muted, back = onBack)
        SettingLabel(if (language == "fa") "ظاهر" else "Appearance", muted)
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (language == "fa") "حالت تاریک" else "Dark mode", color = fg)
            Switch(checked = isDark, onCheckedChange = onDark)
        }
        SettingLabel("Language / زبان", muted)
        ChoiceRow(listOf("English" to "en", "فارسی" to "fa"), language, fg, onLanguage)
        SettingLabel(if (language == "fa") "اندازه قلم" else "Font size", muted)
        Slider(value = fontSize, onValueChange = onSize, valueRange = 14f..32f, modifier = Modifier.padding(horizontal = 20.dp))
        Text("${fontSize.toInt()} sp", color = muted, modifier = Modifier.padding(horizontal = 20.dp))
        SettingLabel(if (language == "fa") "نوع قلم" else "Font family", muted)
        ChoiceRow(listOf("Serif" to "Serif", "Sans" to "Sans", "Monospace" to "Monospace"), fontName, fg, onFont)
        SettingLabel(if (language == "fa") "جهت صفحه" else "Orientation", muted)
        ChoiceRow(listOf("System" to "System", "Portrait" to "Portrait", "Landscape" to "Landscape"), orientation, fg, onOrientation)
        Text("Note: orientation preference is saved; automatic screen rotation follows Android system settings in this starter build.", color = muted, fontSize = 12.sp, modifier = Modifier.padding(20.dp))
    }
}

@Composable
fun ChoiceRow(items: List<Pair<String, String>>, selected: String, fg: Color, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (label, value) ->
            OutlinedButton(onClick = { onSelect(value) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = fg)) {
                Text(label, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun SettingLabel(text: String, muted: Color) {
    Text(text, color = muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 8.dp))
}

@Composable
fun AccountScreen(fg: Color, muted: Color, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Header("Fidibo account", fg, muted, back = onBack)
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Connect your existing Fidibo account", color = fg, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text("Official Fidibo authentication is not connected in this starter project. Login requires an authorized API or SDK from Fidibo. No password is collected or stored here.", color = muted)
            OutlinedButton(onClick = {}, enabled = false) { Text("Connect Fidibo (integration pending)") }
        }
    }
}

@Composable
fun FidiplusScreen(fg: Color, muted: Color, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Header("Fidiplus", fg, muted, back = onBack)
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Your Fidiplus membership", color = fg, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text("Membership status and subscription books will appear here once official Fidibo account access is available.", color = muted)
            Text("This app does not bypass DRM or download protected books.", color = muted, fontSize = 12.sp)
        }
    }
}

fun loadBooks(prefs: android.content.SharedPreferences): List<Book> {
    return try {
        val arr = JSONArray(prefs.getString("books", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Book(o.getString("uri"), o.getString("title"), o.optString("mime"))
        }
    } catch (_: Exception) { emptyList() }
}

fun saveBooks(prefs: android.content.SharedPreferences, books: List<Book>) {
    val arr = JSONArray()
    books.forEach { arr.put(JSONObject().put("uri", it.uri).put("title", it.title).put("mime", it.mime)) }
    prefs.edit().putString("books", arr.toString()).apply()
}

fun loadBookText(context: android.content.Context, book: Book?, page: Int): String {
    if (book == null) return ""
    return try {
        val uri = Uri.parse(book.uri)
        val mime = book.mime.lowercase()
        if (mime.contains("pdf") || book.title.lowercase().endsWith(".pdf")) {
            "PDF file detected. PDF page rendering is planned for the next build. You can keep this file in your library."
        } else {
            val input = context.contentResolver.openInputStream(uri) ?: return ""
            val bytes = input.use { it.readBytes() }
            val zip = java.util.zip.ZipInputStream(bytes.inputStream())
            val chapters = mutableListOf<String>()
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && (entry.name.endsWith(".xhtml", true) || entry.name.endsWith(".html", true) || entry.name.endsWith(".htm", true))) {
                    val raw = zip.readBytes().toString(Charsets.UTF_8)
                    val clean = raw.replace(Regex("(?is)<(script|style)[^>]*>.*?</\\1>"), " ")
                        .replace(Regex("(?s)<[^>]+>"), " ")
                        .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                        .replace(Regex("\\s+"), " ").trim()
                    if (clean.isNotBlank()) chapters.add(clean)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
            val all = chapters.joinToString("\\n\\n")
            if (all.isBlank()) "This EPUB could not be extracted. Please check that it is a valid, unprotected EPUB file."
            else {
                val chunkSize = 2400
                val start = (page.coerceAtLeast(0) * chunkSize).coerceAtMost(all.length)
                all.substring(start, (start + chunkSize).coerceAtMost(all.length)).ifBlank { "End of book." }
            }
        }
    } catch (e: Exception) {
        "Could not open this file: ${e.localizedMessage ?: "unsupported or protected format"}"
    }
}
