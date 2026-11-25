package nl.koffie.wiehaaltkoffie

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Verbindt telefoons met elkaar via Bluetooth/WiFi (Google Nearby Connections).
 * Eén telefoon is host (maakt de groep), de anderen zoeken de groep en sturen
 * hun naam + drankje door. De host stuurt daarna de volledige lijst terug.
 */
class NearbySync(
    context: Context,
    private val onMember: (Member) -> Unit,
    private val onList: (List<Member>) -> Unit,
    private val currentListJson: () -> String
) {
    private val client: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val strategy = Strategy.P2P_STAR
    private val serviceId = "nl.koffie.wiehaaltkoffie"

    private val _status = MutableStateFlow("Niet verbonden")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _endpoints = MutableStateFlow<Map<String, String>>(emptyMap())
    val endpoints: StateFlow<Map<String, String>> = _endpoints.asStateFlow()

    private val connected = mutableSetOf<String>()
    private var isHost = false
    private var myMember: Member? = null

    fun setStatus(text: String) {
        _status.value = text
    }

    private val payloads = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            try {
                val o = JSONObject(String(bytes, Charsets.UTF_8))
                when (o.optString("type")) {
                    "member" -> {
                        onMember(Member.fromJson(o.getJSONObject("member")))
                        if (isHost) broadcastList()
                    }
                    "list" -> {
                        onList(parseMembers(o.getJSONArray("members").toString()))
                        setStatus("Lijst ontvangen van de groep")
                    }
                }
            } catch (_: Exception) {
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private fun payloadOf(o: JSONObject) = Payload.fromBytes(o.toString().toByteArray(Charsets.UTF_8))

    private fun broadcastList() {
        if (connected.isEmpty()) return
        val o = JSONObject().put("type", "list").put("members", JSONArray(currentListJson()))
        client.sendPayload(connected.toList(), payloadOf(o))
    }

    // ---------- Host ----------

    fun startHosting(groupName: String) {
        stop()
        isHost = true
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        client.startAdvertising(groupName, serviceId, hostLifecycle, options)
            .addOnSuccessListener { setStatus("Groep \"$groupName\" is zichtbaar. Wachten op collega's…") }
            .addOnFailureListener { setStatus("Starten mislukt: ${it.message}") }
    }

    private val hostLifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            client.acceptConnection(endpointId, payloads)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                connected.add(endpointId)
                setStatus("${connected.size} collega('s) verbonden")
                broadcastList()
            }
        }

        override fun onDisconnected(endpointId: String) {
            connected.remove(endpointId)
            setStatus("${connected.size} collega('s) verbonden")
        }
    }

    // ---------- Aansluiten ----------

    fun startSearching(me: Member) {
        stop()
        isHost = false
        myMember = me
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        client.startDiscovery(serviceId, discoveryCallback, options)
            .addOnSuccessListener { setStatus("Zoeken naar groepen in de buurt…") }
            .addOnFailureListener { setStatus("Zoeken mislukt: ${it.message}") }
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            _endpoints.value = _endpoints.value + (endpointId to info.endpointName)
        }

        override fun onEndpointLost(endpointId: String) {
            _endpoints.value = _endpoints.value - endpointId
        }
    }

    fun connectTo(endpointId: String) {
        val me = myMember ?: return
        setStatus("Verbinden…")
        client.requestConnection(me.name, endpointId, joinLifecycle)
            .addOnFailureListener { setStatus("Verbinden mislukt: ${it.message}") }
    }

    private val joinLifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            client.acceptConnection(endpointId, payloads)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                connected.add(endpointId)
                myMember?.let {
                    val o = JSONObject().put("type", "member").put("member", it.toJson())
                    client.sendPayload(endpointId, payloadOf(o))
                }
                setStatus("Verbonden met ${_endpoints.value[endpointId] ?: "de groep"} ✓")
            } else {
                setStatus("Verbinden mislukt of geweigerd")
            }
        }

        override fun onDisconnected(endpointId: String) {
            connected.remove(endpointId)
            setStatus("Verbinding verbroken")
        }
    }

    fun stop() {
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        connected.clear()
        _endpoints.value = emptyMap()
        isHost = false
        setStatus("Niet verbonden")
    }
}
