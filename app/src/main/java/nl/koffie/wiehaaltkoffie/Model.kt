package nl.koffie.wiehaaltkoffie

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

val DRINKS = listOf(
    "Koffie", "Zwarte koffie", "Cappuccino", "Latte macchiato", "Espresso",
    "Thee", "Warme chocolademelk", "Water", "Anders"
)

data class Member(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val drink: String,
    val note: String = "",
    val present: Boolean = true,
    val timesPicked: Int = 0
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("drink", drink)
        .put("note", note)
        .put("present", present)
        .put("times", timesPicked)

    companion object {
        fun fromJson(o: JSONObject) = Member(
            id = o.getString("id"),
            name = o.getString("name"),
            drink = o.getString("drink"),
            note = o.optString("note", ""),
            present = o.optBoolean("present", true),
            timesPicked = o.optInt("times", 0)
        )
    }
}

fun List<Member>.toJsonString(): String {
    val arr = JSONArray()
    forEach { arr.put(it.toJson()) }
    return arr.toString()
}

fun parseMembers(json: String): List<Member> = try {
    val arr = JSONArray(json)
    (0 until arr.length()).map { Member.fromJson(arr.getJSONObject(it)) }
} catch (e: Exception) {
    emptyList()
}
