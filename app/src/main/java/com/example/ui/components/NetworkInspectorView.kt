package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class HttpTrafficItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val method: String,
    val url: String,
    val statusCode: Int,
    val latencyMs: Long,
    val timestamp: String,
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestBody: String? = null,
    val responseHeaders: Map<String, String> = emptyMap(),
    val responseBody: String = ""
)

@Composable
fun NetworkInspectorView(
    onLogEvent: (String, String, String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var httpMethod by remember { mutableStateOf("GET") }
    var requestUrl by remember { mutableStateOf("https://dummyjson.com/quotes/random") }
    var requestBodyInput by remember { mutableStateOf("{\n  \"query\": \"Android Studio\",\n  \"limit\": 10\n}") }
    var isSending by remember { mutableStateOf(false) }
    var filterText by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<HttpTrafficItem?>(null) }
    var isBodyExpanded by remember { mutableStateOf(false) }

    // Pre-populated realistic history for developers
    val trafficHistory = remember {
        mutableStateListOf(
            HttpTrafficItem(
                method = "GET",
                url = "https://api.github.com/repos/google/accompanist/releases/latest",
                statusCode = 200,
                latencyMs = 142,
                timestamp = "15:28:10",
                requestHeaders = mapOf("Accept" to "application/vnd.github.v3+json", "User-Agent" to "AndroidStudio-Mobile/1.0"),
                responseBody = "{\n  \"tag_name\": \"v0.36.0\",\n  \"name\": \"Accompanist 0.36.0\",\n  \"published_at\": \"2026-08-15T12:00:00Z\",\n  \"prerelease\": false,\n  \"body\": \"Compose 1.7.x compatibility updates\"\n}"
            ),
            HttpTrafficItem(
                method = "POST",
                url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.7-flash:generateContent",
                statusCode = 200,
                latencyMs = 380,
                timestamp = "15:27:44",
                requestHeaders = mapOf("Content-Type" to "application/json", "x-goog-api-client" to "gl-kotlin/2.0"),
                requestBody = "{\n  \"contents\": [{\"parts\": [{\"text\": \"Generate Jetpack Compose Counter\"}]}]\n}",
                responseBody = "{\n  \"candidates\": [{\n    \"content\": {\n      \"parts\": [{\"text\": \"@Composable fun CounterScreen() { ... }\"}],\n      \"role\": \"model\"\n    },\n    \"finishReason\": \"STOP\"\n  }]\n}"
            ),
            HttpTrafficItem(
                method = "GET",
                url = "https://repo1.maven.org/maven2/androidx/compose/ui/ui/maven-metadata.xml",
                statusCode = 200,
                latencyMs = 88,
                timestamp = "15:26:02",
                requestHeaders = mapOf("Accept" to "application/xml"),
                responseBody = "<metadata>\n  <groupId>androidx.compose.ui</groupId>\n  <artifactId>ui</artifactId>\n  <versioning>\n    <release>1.7.5</release>\n    <latest>1.7.5</latest>\n  </versioning>\n</metadata>"
            )
        )
    }

    val presetEndpoints = listOf(
        Pair("Quotes API", "https://dummyjson.com/quotes/random"),
        Pair("GitHub Releases", "https://api.github.com/repos/google/accompanist/releases/latest"),
        Pair("Weather Sample", "https://api.open-meteo.com/v1/forecast?latitude=37.77&longitude=-122.41&current_weather=true"),
        Pair("HTTP Bin (POST)", "https://httpbin.org/post")
    )

    fun executeHttpRequest() {
        if (requestUrl.isBlank() || isSending) return
        isSending = true

        coroutineScope.launch {
            val startTime = System.currentTimeMillis()
            var code = 0
            var respBody = ""
            val reqHeaders = mutableMapOf("User-Agent" to "AndroidStudio-Mobile-Inspector/2026.1")
            if (httpMethod == "POST" || httpMethod == "PUT") {
                reqHeaders["Content-Type"] = "application/json"
            }

            try {
                withContext(Dispatchers.IO) {
                    val urlObj = URL(requestUrl)
                    val conn = (urlObj.openConnection() as HttpURLConnection).apply {
                        requestMethod = httpMethod
                        connectTimeout = 8000
                        readTimeout = 8000
                        reqHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
                        if (httpMethod == "POST" || httpMethod == "PUT") {
                            doOutput = true
                            outputStream.use { os ->
                                os.write(requestBodyInput.toByteArray(Charsets.UTF_8))
                            }
                        }
                    }

                    code = conn.responseCode
                    val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
                    respBody = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
                }
            } catch (e: Exception) {
                code = if (code != 0) code else 500
                respBody = "{\n  \"error\": \"${e.javaClass.simpleName}\",\n  \"message\": \"${e.localizedMessage ?: "Connection Failed"}\"\n}"
            } finally {
                val latency = System.currentTimeMillis() - startTime
                val now = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                val newItem = HttpTrafficItem(
                    method = httpMethod,
                    url = requestUrl,
                    statusCode = code,
                    latencyMs = latency,
                    timestamp = now,
                    requestHeaders = reqHeaders,
                    requestBody = if (httpMethod in listOf("POST", "PUT")) requestBodyInput else null,
                    responseBody = respBody
                )
                trafficHistory.add(0, newItem)
                selectedItem = newItem
                isSending = false
                onLogEvent("NetworkInspector", if (code in 200..299) "I" else "E", "$httpMethod $requestUrl -> $code ($latency ms)")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
    ) {
        // Top Toolbar & Request Runner
        Surface(
            color = StudioSurface,
            tonalElevation = 2.dp,
            border = BorderStroke(0.5.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Address Bar & Method Dropdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Method Selector
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (httpMethod) {
                            "GET" -> StudioGreen.copy(alpha = 0.2f)
                            "POST" -> StudioCyan.copy(alpha = 0.2f)
                            "PUT" -> StudioOrange.copy(alpha = 0.2f)
                            else -> StudioRed.copy(alpha = 0.2f)
                        },
                        border = BorderStroke(1.dp, when (httpMethod) {
                            "GET" -> StudioGreen
                            "POST" -> StudioCyan
                            "PUT" -> StudioOrange
                            else -> StudioRed
                        }),
                        modifier = Modifier.clickable {
                            httpMethod = when (httpMethod) {
                                "GET" -> "POST"
                                "POST" -> "PUT"
                                "PUT" -> "DELETE"
                                else -> "GET"
                            }
                        }
                    ) {
                        Text(
                            text = httpMethod,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (httpMethod) {
                                "GET" -> StudioGreen
                                "POST" -> StudioCyan
                                "PUT" -> StudioOrange
                                else -> StudioRed
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }

                    // URL Input Box
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF141720),
                        border = BorderStroke(1.dp, Color(0xFF2C3242)),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            BasicTextField(
                                value = requestUrl,
                                onValueChange = { requestUrl = it },
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                cursorBrush = SolidColor(StudioGreen),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Send Button
                    FilledIconButton(
                        onClick = { executeHttpRequest() },
                        enabled = !isSending,
                        shape = RoundedCornerShape(6.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = StudioGreen,
                            contentColor = Color(0xFF003919)
                        ),
                        modifier = Modifier.size(34.dp)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF003919), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Preset Pills & POST body toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (httpMethod in listOf("POST", "PUT")) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isBodyExpanded) StudioCyan.copy(alpha = 0.25f) else Color(0xFF202637),
                            border = BorderStroke(0.5.dp, if (isBodyExpanded) StudioCyan else Color.Transparent),
                            modifier = Modifier.clickable { isBodyExpanded = !isBodyExpanded }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DataArray, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Payload (JSON)", fontSize = 10.sp, color = StudioCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    presetEndpoints.forEach { (title, url) ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF202637),
                            modifier = Modifier.clickable {
                                requestUrl = url
                                if (title.contains("POST")) httpMethod = "POST" else httpMethod = "GET"
                            }
                        ) {
                            Text(
                                text = title,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.LightGray,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Expandable Request Body Editor
                AnimatedVisibility(visible = isBodyExpanded && (httpMethod in listOf("POST", "PUT"))) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .background(Color(0xFF0D0F14), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF2A2E3D), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text("HTTP Request Payload (application/json):", fontSize = 10.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        BasicTextField(
                            value = requestBodyInput,
                            onValueChange = { requestBodyInput = it },
                            textStyle = TextStyle(
                                color = StudioCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            cursorBrush = SolidColor(StudioCyan),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp, max = 120.dp)
                        )
                    }
                }
            }
        }

        // Main Traffic Content & Detail Pane
        Row(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Traffic Log List (Left Pane)
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .background(StudioSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    .padding(6.dp)
            ) {
                // Filter Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Traffic Calls (${trafficHistory.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(
                        onClick = { trafficHistory.clear(); selectedItem = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    }
                }

                if (trafficHistory.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No network calls recorded", color = Color.Gray, fontSize = 11.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(trafficHistory) { item ->
                            val isSelected = item.id == selectedItem?.id
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) StudioSurfaceVariant else Color(0xFF161922),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) StudioGreen else Color(0xFF222634)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedItem = item }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.method,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (item.method) {
                                                    "GET" -> StudioGreen
                                                    "POST" -> StudioCyan
                                                    "PUT" -> StudioOrange
                                                    else -> StudioRed
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = item.url.substringAfter("://"),
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White,
                                                maxLines = 1
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.padding(top = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(item.timestamp, fontSize = 9.sp, color = Color.Gray)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("${item.latencyMs} ms", fontSize = 9.sp, color = Color.LightGray)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (item.statusCode in 200..299) StudioGreen.copy(alpha = 0.2f) else StudioRed.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "${item.statusCode}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.statusCode in 200..299) StudioGreen else StudioRed,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Detail Inspector View (Right Pane)
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
                    .background(StudioSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                if (selectedItem == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a network call to view response payload & headers", color = Color.Gray, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                } else {
                    val item = selectedItem!!
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Response Inspector", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(item.responseBody))
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy JSON", tint = StudioGreen, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    HorizontalDivider(color = StudioBorder, modifier = Modifier.padding(vertical = 4.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Headers Chip list
                        if (item.requestHeaders.isNotEmpty()) {
                            Text("Request Headers:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            item.requestHeaders.forEach { (k, v) ->
                                Text("• $k: $v", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.LightGray)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Text("Payload Body (${item.responseBody.length} bytes):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudioCyan)
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0E1117),
                            border = BorderStroke(0.5.dp, Color(0xFF232838)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.responseBody,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (item.statusCode in 200..299) Color(0xFFCE9178) else StudioRed,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
