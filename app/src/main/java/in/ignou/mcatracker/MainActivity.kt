package `in`.ignou.mcatracker

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleChecks()
        if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            TrackerScreen { url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        }
    }
    private fun scheduleChecks() {
        val request = PeriodicWorkRequestBuilder<NotificationWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ignou_mca_updates", ExistingPeriodicWorkPolicy.KEEP, request
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(onOpen: (String) -> Unit) {
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf("All") }
    val scope = rememberCoroutineScope()
    val filters = listOf("All", "Project", "Hall Ticket", "Exam Registration", "Assignments", "Exams", "Results", "Registration", "General")

    fun refresh() {
        scope.launch {
            loading = true
            error = null
            try { announcements = AnnouncementRepository().fetch() }
            catch (_: Exception) { error = "Unable to reach IGNOU. Check your internet and retry." }
            loading = false
        }
    }
    LaunchedEffect(Unit) { refresh() }
    val visible = announcements.filter { filter == "All" || it.category == filter }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFF83AFFF), background = Color(0xFF0B1020), surface = Color(0xFF151D30)
    )) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Column {
                        Text("IGNOU MCA Tracker", fontWeight = FontWeight.Bold)
                        Text("MCA_NEW · Official updates", style = MaterialTheme.typography.labelSmall)
                    } },
                    actions = { IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, "Refresh") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(12.dp))
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Your MCA_NEW noticeboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Project, synopsis, assignments, examinations and results from official IGNOU sources.",
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${announcements.size} updates found", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.weight(1f))
                            Text("Checks every 6 hours", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        onOpen("https://ignou.samarth.edu.in/index.php/site/login")
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Hall ticket availability", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            SuggestionChip(onClick = {
                                onOpen("https://ignou.samarth.edu.in/index.php/site/login")
                            }, label = { Text("Check portal") })
                        }
                        Text(
                            "Open the official IGNOU Samarth portal to check and download your personal hall ticket. The app does not assume a ticket is released.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Categories", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filters) { item ->
                        FilterChip(selected = filter == item, onClick = { filter = item }, label = { Text(item) })
                    }
                }
                Spacer(Modifier.height(10.dp))
                when {
                    loading && announcements.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    error != null && announcements.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                    }
                    visible.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(if (loading) "Checking official sources…" else "No matching announcements found.")
                    }
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(visible) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onOpen(item.url) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SuggestionChip(onClick = {}, label = { Text(item.category) })
                                        Spacer(Modifier.weight(1f))
                                        if (item.date.isNotBlank()) Text(item.date, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(item.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                    if (item.summary.isNotBlank()) {
                                        Spacer(Modifier.height(5.dp))
                                        Text(item.summary, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                                    }
                                    Spacer(Modifier.height(7.dp))
                                    Text("${item.source} · Open notice ↗", color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                Text("Official-source links. Verify deadlines in the original notice.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .65f),
                    modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}
