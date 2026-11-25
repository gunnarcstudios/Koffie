package nl.koffie.wiehaaltkoffie

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class KoffieViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("koffie", Context.MODE_PRIVATE)

    private val _members = MutableStateFlow(parseMembers(prefs.getString("members", "[]") ?: "[]"))
    val members: StateFlow<List<Member>> = _members.asStateFlow()

    private val _picked = MutableStateFlow<Member?>(null)
    val picked: StateFlow<Member?> = _picked.asStateFlow()

    private var lastPickedId: String? = prefs.getString("lastPicked", null)

    val nearby = NearbySync(
        app,
        onMember = { upsert(it) },
        onList = { list -> list.forEach { upsert(it) } },
        currentListJson = { _members.value.toJsonString() }
    )

    private fun save() {
        prefs.edit().putString("members", _members.value.toJsonString()).apply()
    }

    /** Voegt toe, of werkt bij als id of naam al bestaat. */
    fun upsert(m: Member) {
        val list = _members.value
        val idx = list.indexOfFirst { it.id == m.id || it.name.equals(m.name, ignoreCase = true) }
        _members.value = if (idx >= 0) {
            val old = list[idx]
            list.toMutableList().also {
                it[idx] = old.copy(name = m.name, drink = m.drink, note = m.note)
            }
        } else list + m
        save()
    }

    fun add(name: String, drink: String, note: String) = upsert(Member(name = name, drink = drink, note = note))

    fun remove(id: String) {
        _members.value = _members.value.filterNot { it.id == id }
        save()
    }

    fun setPresent(id: String, present: Boolean) {
        _members.value = _members.value.map { if (it.id == id) it.copy(present = present) else it }
        save()
    }

    /** Loot willekeurig iemand die aanwezig is; de vorige koffiehaler valt af als er alternatieven zijn. */
    fun pick() {
        val present = _members.value.filter { it.present }
        if (present.isEmpty()) return
        val pool = if (present.size > 1) present.filter { it.id != lastPickedId } else present
        val winner = pool.random()
        lastPickedId = winner.id
        prefs.edit().putString("lastPicked", winner.id).apply()
        _members.value = _members.value.map {
            if (it.id == winner.id) it.copy(timesPicked = it.timesPicked + 1) else it
        }
        save()
        _picked.value = winner
    }

    fun clearPick() {
        _picked.value = null
    }

    /** Bestelling van iedereen die aanwezig is, bv. "2× Koffie". */
    fun orderSummary(): List<Pair<String, Int>> =
        _members.value.filter { it.present }
            .groupingBy { it.drink }.eachCount()
            .toList().sortedByDescending { it.second }

    fun exportJson(): String = _members.value.toJsonString()

    fun importJson(text: String): Int {
        val list = parseMembers(text.trim())
        list.forEach { upsert(it) }
        return list.size
    }

    override fun onCleared() {
        nearby.stop()
    }
}
