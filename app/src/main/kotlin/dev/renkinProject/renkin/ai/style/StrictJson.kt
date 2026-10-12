package dev.renkinProject.renkin.ai.style

import org.json.JSONArray
import org.json.JSONObject

/** Bounded strict JSON, including duplicate keys, before Android's permissive JSON API.
 * Provider data is untrusted; no comments, coercion, trailing commas or deep recursion. */
internal fun strictObject(text: String, maxBytes: Int = 65536, maxDepth: Int = 12): JSONObject {
    require(text.toByteArray(Charsets.UTF_8).size <= maxBytes) { "JSON exceeds the safety limit." }
    class Reader {
        var offset = 0
        fun ws() { while (offset < text.length && text[offset] in " \t\r\n") offset++ }
        fun take(c: Char) { ws(); require(offset < text.length && text[offset++] == c) { "Invalid JSON." } }
        fun string(): String {
            take('"'); val out = StringBuilder()
            while (offset < text.length) {
                val c = text[offset++]
                if (c == '"') {
                    val value = out.toString()
                    var i = 0
                    while (i < value.length) {
                        val ch = value[i++]
                        if (ch.isHighSurrogate()) require(i < value.length && value[i++].isLowSurrogate())
                        else require(!ch.isLowSurrogate())
                    }
                    return value
                }
                require(c.code >= 32)
                if (c != '\\') out.append(c) else {
                    require(offset < text.length)
                    when (val escape = text[offset++]) {
                        '"', '\\', '/' -> out.append(escape)
                        'b' -> out.append('\b'); 'f' -> out.append('\u000c')
                        'n' -> out.append('\n'); 'r' -> out.append('\r'); 't' -> out.append('\t')
                        'u' -> {
                            require(offset + 4 <= text.length)
                            out.append(text.substring(offset, offset + 4).toInt(16).toChar()); offset += 4
                        }
                        else -> error("Invalid JSON escape.")
                    }
                }
            }
            error("Truncated JSON string.")
        }
        fun value(depth: Int): Any {
            require(depth <= maxDepth); ws(); require(offset < text.length)
            return when (text[offset]) {
                '{' -> {
                    take('{'); val result = JSONObject(); val keys = mutableSetOf<String>(); ws()
                    if (offset < text.length && text[offset] == '}') { offset++; result } else {
                        while (true) {
                            val key = string(); require(keys.add(key)) { "Repeated JSON key." }; take(':')
                            result.put(key, value(depth + 1)); ws(); require(offset < text.length)
                            if (text[offset] == '}') { offset++; break }; take(',')
                        }; result
                    }
                }
                '[' -> {
                    take('['); val result = JSONArray(); ws()
                    if (offset < text.length && text[offset] == ']') { offset++; result } else {
                        while (true) {
                            require(result.length() < 128); result.put(value(depth + 1)); ws(); require(offset < text.length)
                            if (text[offset] == ']') { offset++; break }; take(',')
                        }; result
                    }
                }
                '"' -> string()
                't', 'f', 'n' -> {
                    val word = when (text[offset]) { 't' -> "true"; 'f' -> "false"; else -> "null" }
                    require(text.startsWith(word, offset)); offset += word.length
                    when (word) { "true" -> true; "false" -> false; else -> JSONObject.NULL }
                }
                else -> {
                    val start = offset
                    while (offset < text.length && text[offset] in "-+0123456789.eE") offset++
                    val number = text.substring(start, offset)
                    require(number.length in 1..24 && number.matches(Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")))
                    if (number.any { it in ".eE" }) number.toDouble().also { require(it.isFinite()) }
                    else number.toLong()
                }
            }
        }
    }
    val reader = Reader(); val value = reader.value(0); reader.ws()
    require(reader.offset == text.length && value is JSONObject) { "Expected one JSON object." }
    return value
}

internal fun json(vararg entries: Pair<String, Any?>): JSONObject = JSONObject().apply {
    entries.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) }
}

internal fun cleanText(value: String, limit: Int, multiline: Boolean = false): String {
    require(value.length <= limit && value.none { it.isISOControl() && !(multiline && it in "\n\t") }) {
        "Invalid or oversized text."
    }
    return value.trim()
}

internal fun JSONObject.exactKeys(expected: Set<String>) {
    require(keys().asSequence().toSet() == expected) { "Unexpected or missing JSON properties." }
}
internal fun JSONObject.stringValue(key: String): String = (get(key) as? String) ?: error("Expected text.")
internal fun JSONObject.longValue(key: String): Long = get(key).let {
    require(it is Long || it is Int) { "Expected integer." }; (it as Number).toLong()
}

/** Sorted object keys and preserved array order make fingerprints independent of JSON maps. */
internal fun canonicalJson(value: Any?): String = when (value) {
    null, JSONObject.NULL -> "null"
    is JSONObject -> value.keys().asSequence().toList().sorted().joinToString(",", "{", "}") {
        JSONObject.quote(it) + ":" + canonicalJson(value.get(it))
    }
    is JSONArray -> (0 until value.length()).joinToString(",", "[", "]") { canonicalJson(value.get(it)) }
    is String -> JSONObject.quote(value)
    is Boolean, is Number -> value.toString()
    else -> error("Unsupported JSON value.")
}
