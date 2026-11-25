package nl.koffie.wiehaaltkoffie

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private val Brown = Color(0xFF6F4E37)
private val Cream = Color(0xFFF8F1E9)
private val Latte = Color(0xFFE8C9A0)

private val Colors = lightColorScheme(
    primary = Brown,
    onPrimary = Color.White,
    primaryContainer = Latte,
    onPrimaryContainer = Color(0xFF3B2416),
    secondary = Color(0xFFA9714B),
    background = Cream,
    surface = Cream,
    surfaceVariant = Color(0xFFEFE2D2)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = Colors) {
                KoffieApp()
            }
        }
    }
}

@Composable
fun KoffieApp(vm: KoffieViewModel = viewModel()) {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                listOf("☕" to "Loten", "👥" to "Team", "📡" to "Verbinden").forEachIndexed { i, (emoji, label) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(emoji, fontSize = 22.sp) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> LotenScreen(vm)
                1 -> TeamScreen(vm)
                else -> VerbindScreen(vm)
            }
        }
    }
}

// ---------------------------------------------------------------- Loten

@Composable
fun LotenScreen(vm: KoffieViewModel) {
    val members by vm.members.collectAsStateWithLifecycle()
    val picked by vm.picked.collectAsStateWithLifecycle()
    val present = members.count { it.present }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Wie haalt er koffie?", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Brown)
        Text("$present van ${members.size} collega's doen mee", color = Color.Gray)

        val winner = picked
        if (winner == null) {
            Spacer(Modifier.height(24.dp))
            Text("☕", fontSize = 90.sp)
            Button(
                onClick = { vm.pick() },
                enabled = present > 0,
                modifier = Modifier.fillMaxWidth().height(60.dp)
            ) { Text("Loot de koffiehaler!", fontSize = 18.sp) }
            if (present == 0) Text("Voeg eerst collega's toe bij Team.", color = Color.Gray)
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉 Vandaag haalt…", color = Color(0xFF3B2416))
                    Text(winner.name, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Brown, textAlign = TextAlign.Center)
                    Text("koffie!", color = Color(0xFF3B2416))
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Bestelling", fontWeight = FontWeight.Bold)
                    vm.orderSummary().forEach { (drink, n) -> Text("${n}× $drink") }
                    val notes = members.filter { it.present && it.note.isNotBlank() }
                    if (notes.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        notes.forEach { Text("${it.name}: ${it.note}", color = Color.Gray, fontSize = 13.sp) }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { vm.pick() }) { Text("Opnieuw loten") }
                Button(onClick = { vm.clearPick() }) { Text("Klaar") }
            }
        }
    }
}

// ---------------------------------------------------------------- Team

@Composable
fun TeamScreen(vm: KoffieViewModel) {
    val members by vm.members.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("Team", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Brown)
            Spacer(Modifier.height(8.dp))
            Card { Column(Modifier.padding(16.dp)) { MemberForm("Toevoegen") { n, d, note -> vm.add(n, d, note) } } }
        }
        items(members, key = { it.id }) { m ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(m.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            m.drink + if (m.note.isNotBlank()) " · ${m.note}" else "",
                            color = Color.Gray
                        )
                        Text("${m.timesPicked}× gehaald", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(checked = m.present, onCheckedChange = { vm.setPresent(m.id, it) })
                    TextButton(onClick = { vm.remove(m.id) }) { Text("🗑") }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun MemberForm(buttonLabel: String, onSubmit: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var drink by remember { mutableStateOf(DRINKS.first()) }
    var note by remember { mutableStateOf("") }
    var open by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Naam") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Box {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Drinken: $drink  ▾")
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DRINKS.forEach { d ->
                    DropdownMenuItem(text = { Text(d) }, onClick = { drink = d; open = false })
                }
            }
        }
        OutlinedTextField(
            value = note, onValueChange = { note = it },
            label = { Text("Opmerking (melk, suiker…)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                onSubmit(name.trim(), drink, note.trim())
                name = ""; note = ""
            },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text(buttonLabel) }
    }
}

// ---------------------------------------------------------------- Verbinden

private fun nearbyPermissions(): List<String> = when {
    Build.VERSION.SDK_INT >= 33 -> listOf(
        Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.NEARBY_WIFI_DEVICES
    )
    Build.VERSION.SDK_INT >= 31 -> listOf(
        Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION
    )
    else -> listOf(Manifest.permission.ACCESS_FINE_LOCATION)
}

@Composable
fun VerbindScreen(vm: KoffieViewModel) {
    val ctx = LocalContext.current
    val status by vm.nearby.status.collectAsStateWithLifecycle()
    val groups by vm.nearby.endpoints.collectAsStateWithLifecycle()
    var groupName by remember { mutableStateOf("Koffiegroep") }
    var showImport by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res.values.all { it }) pending?.invoke()
        else vm.nearby.setStatus("Toestemming geweigerd – nodig voor Bluetooth/WiFi")
        pending = null
    }

    fun withPermissions(action: () -> Unit) {
        val missing = nearbyPermissions().filter {
            ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) action() else {
            pending = action
            launcher.launch(missing.toTypedArray())
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("Verbinden", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Brown)
            Text("Laat collega's zichzelf toevoegen via Bluetooth/WiFi.", color = Color.Gray)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text("Status: $status", Modifier.padding(14.dp).fillMaxWidth(), fontWeight = FontWeight.Medium)
            }
        }
        item {
            Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Groep starten", fontWeight = FontWeight.Bold)
                Text("Eén persoon start de groep. Anderen zien hem in de buurt.", color = Color.Gray, fontSize = 13.sp)
                OutlinedTextField(
                    value = groupName, onValueChange = { groupName = it },
                    label = { Text("Groepsnaam") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { withPermissions { vm.nearby.startHosting(groupName.ifBlank { "Koffiegroep" }) } },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Start groep") }
            } }
        }
        item {
            Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("2. Aansluiten bij een groep", fontWeight = FontWeight.Bold)
                Text("Vul je eigen gegevens in en zoek de groep van je collega.", color = Color.Gray, fontSize = 13.sp)
                MemberForm("Zoek groepen in de buurt") { n, d, note ->
                    val me = Member(name = n, drink = d, note = note)
                    vm.upsert(me)
                    withPermissions { vm.nearby.startSearching(me) }
                }
                groups.forEach { (id, name) ->
                    Button(onClick = { vm.nearby.connectTo(id) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Sluit aan bij \"$name\"")
                    }
                }
            } }
        }
        item {
            Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("3. Lijst delen (zonder Bluetooth)", fontWeight = FontWeight.Bold)
                Text("Stuur de lijst via WhatsApp of mail, of plak een lijst van een collega.", color = Color.Gray, fontSize = 13.sp)
                OutlinedButton(
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, vm.exportJson())
                        }
                        ctx.startActivity(Intent.createChooser(send, "Deel teamlijst"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Deel mijn teamlijst") }
                OutlinedButton(onClick = { showImport = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Plak lijst van collega")
                }
            } }
        }
        item {
            TextButton(onClick = { vm.nearby.stop() }, modifier = Modifier.fillMaxWidth()) { Text("Verbinding stoppen") }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("Lijst plakken") },
            text = {
                OutlinedTextField(value = importText, onValueChange = { importText = it }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = vm.importJson(importText)
                    Toast.makeText(ctx, if (n > 0) "$n collega's toegevoegd" else "Geen geldige lijst", Toast.LENGTH_SHORT).show()
                    importText = ""; showImport = false
                }) { Text("Importeren") }
            },
            dismissButton = { TextButton(onClick = { showImport = false }) { Text("Annuleer") } }
        )
    }
}
