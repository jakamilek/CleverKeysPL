package tribixbite.cleverkeys.langpack

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.Collections
import java.util.Locale

/** Version-2 tables contain exact source fields, never guessed stems or meanings. */
internal fun jsonNodes(value: JsonElement): Int = 1 + when {
    value.isJsonObject -> value.asJsonObject.entrySet().sumOf { jsonNodes(it.value) }
    value.isJsonArray -> value.asJsonArray.sumOf { jsonNodes(it) }
    else -> 0
}

private fun ordered(value: JsonObject): JsonObject = JsonObject().also { out ->
    value.entrySet().sortedBy { it.key }.forEach { out.add(it.key, it.value) }
}

private fun reference(obj: JsonObject, key: String, size: Int): Int =
    IntelligenceJson.integer(obj, key).also { require(it in 0 until size) { "Invalid source reference" } }

private fun attributesValid(obj: JsonObject) {
    require(obj.keySet().all { it in setOf("nameClasses", "labels") } && obj.size() > 0)
    for (value in obj.entrySet().map { it.value }) {
        require(value.isJsonArray && value.asJsonArray.size() <= 64)
        require(value.asJsonArray.all { it.isJsonPrimitive && it.asJsonPrimitive.isString &&
            it.asString.length in 1..96 })
    }
}

internal data class SharedSourceAttributes(val json: String, val nodes: Int) {
    fun objectValue(): JsonObject = JsonParser.parseString(json).asJsonObject
    companion object {
        fun read(obj: JsonObject): SharedSourceAttributes {
            attributesValid(obj)
            return SharedSourceAttributes(ordered(obj).toString(), jsonNodes(obj))
        }
    }
}

internal data class SharedSourceRecord(val json: String, val expandedNodes: Int, val lemma: SourceLemma) {
    fun objectValue(attributes: List<SharedSourceAttributes>): JsonObject =
        expandAttributes(JsonParser.parseString(json).asJsonObject, attributes)
    companion object {
        fun read(obj: JsonObject, attributes: List<SharedSourceAttributes>,
                 intern: MutableMap<SourceLemma, SourceLemma>): SharedSourceRecord {
            require(obj.keySet().all { it in setOf("lemma", "partOfSpeech", "nameClasses", "labels", "sourceAttributesRef") })
            val expanded = expandAttributes(obj, attributes)
            val lemma = IntelligenceJson.string(expanded, "lemma")
            val pos = IntelligenceJson.string(expanded, "partOfSpeech")
            require(lemma.isNotBlank() && lemma.length <= 96 && pos.isNotBlank() && pos.length <= 32)
            val attrs = JsonObject()
            for (key in listOf("nameClasses", "labels")) expanded.get(key)?.let { attrs.add(key, it) }
            if (attrs.size() > 0) attributesValid(attrs)
            val pair = SourceLemma(lemma, pos)
            return SharedSourceRecord(ordered(obj).toString(), jsonNodes(expanded), intern.getOrPut(pair) { pair })
        }
    }
}

private fun expandAttributes(obj: JsonObject, attributes: List<SharedSourceAttributes>): JsonObject {
    if (!obj.has("sourceAttributesRef")) return ordered(obj.deepCopy())
    require(!obj.has("nameClasses") && !obj.has("labels")) { "Conflicting shared attributes" }
    val shared = attributes[reference(obj, "sourceAttributesRef", attributes.size)].objectValue()
    val out = obj.deepCopy()
    out.remove("sourceAttributesRef")
    shared.entrySet().forEach { out.add(it.key, it.value) }
    return ordered(out)
}

internal class SharedSources(attributes: List<SharedSourceAttributes>, records: List<SharedSourceRecord>) {
    private val attributes = Collections.unmodifiableList(ArrayList(attributes))
    private val records = Collections.unmodifiableList(ArrayList(records))
    fun sameTables(other: SharedSources): Boolean = attributes == other.attributes && records == other.records

    /** Compare validated immutable prefixes once per old pool, never infer source identity. */
    fun extendsTables(other: SharedSources): Boolean = this === other ||
        (attributes.size >= other.attributes.size && records.size >= other.records.size &&
            other.attributes.indices.all { attributes[it] == other.attributes[it] } &&
            other.records.indices.all { records[it] == other.records[it] })

    fun reusePrefix(other: SharedSources?): SharedSources {
        if (other == null || !extendsTables(other)) return this
        if (sameTables(other)) return other
        return SharedSources(attributes.mapIndexed { i, value -> other.attributes.getOrNull(i) ?: value },
            records.mapIndexed { i, value -> other.records.getOrNull(i) ?: value })
    }

    private fun rows(metadata: JsonElement?): List<JsonObject> {
        val evidence = metadata?.takeIf { it.isJsonObject }?.asJsonObject
            ?.get("sourceEvidence")?.takeIf { it.isJsonObject }?.asJsonObject ?: return emptyList()
        val value = evidence.get("lexicalReadings") ?: return emptyList()
        require(value.isJsonArray)
        return value.asJsonArray.map { require(it.isJsonObject); it.asJsonObject }
    }

    fun hasReferences(metadata: JsonElement?, allowSurfaceMasks: Boolean = false): Boolean = rows(metadata).any {
        it.has("sourceRef") || it.has("sourceAttributesRef") || (allowSurfaceMasks && it.has("surfaceMask"))
    }

    fun hasSurfaceMasks(metadata: JsonElement?): Boolean = rows(metadata).any { it.has("surfaceMask") }

    private fun maskedSurfaces(row: JsonObject, key: String?, allowed: Boolean): com.google.gson.JsonArray {
        require(allowed && key != null && !row.has("surfaces")) { "Undeclared or conflicting surface mask" }
        require(key.none { it.isSurrogate() }) { "Surface mask key must contain BMP code points" }
        val mask = IntelligenceJson.integer(row, "surfaceMask")
        require(mask in 1..7) { "Invalid source surface mask" }
        val forms = listOf(key, key.take(1).uppercase(Locale.ROOT) + key.drop(1), key.uppercase(Locale.ROOT))
        val selected = forms.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.distinct().sorted()
        require(selected.all { it.length in 1..96 })
        return com.google.gson.JsonArray().also { out -> selected.forEach(out::add) }
    }

    fun expand(metadata: JsonElement?, entry: JsonObject? = null, surfaceKey: String? = null,
               allowSurfaceMasks: Boolean = false): JsonElement? {
        if (metadata == null) return null
        val rows = rows(metadata)
        var size = jsonNodes(entry ?: metadata)
        // Compute the exact expanded cost BEFORE allocating copies of source rows.
        for (row in rows) {
            if (allowSurfaceMasks && row.has("surfaceMask")) {
                size += jsonNodes(maskedSurfaces(row, surfaceKey, allowSurfaceMasks)) - 1
            }
            if (row.has("sourceRef")) {
                val field = if (allowSurfaceMasks && row.has("surfaceMask")) "surfaceMask" else "surfaces"
                require(row.keySet() == setOf("sourceRef", field)) { "Conflicting source fields" }
                if (field == "surfaces") {
                    val surfaces = row.get("surfaces")
                    require(surfaces.isJsonArray && surfaces.asJsonArray.size() in 1..32)
                    require(surfaces.asJsonArray.all { it.isJsonPrimitive && it.asJsonPrimitive.isString &&
                        it.asString.isNotBlank() && it.asString.length <= 96 }) { "Invalid source surfaces" }
                }
                size += records[reference(row, "sourceRef", records.size)].expandedNodes - 2
            } else if (row.has("sourceAttributesRef")) {
                require(!row.has("nameClasses") && !row.has("labels"))
                size += attributes[reference(row, "sourceAttributesRef", attributes.size)].nodes - 2
            }
            require(size <= 16_384) { "Expanded entry complexity limit" }
        }
        if (!hasReferences(metadata, allowSurfaceMasks)) return metadata
        val result = metadata.deepCopy()
        val outRows = result.asJsonObject.getAsJsonObject("sourceEvidence").getAsJsonArray("lexicalReadings")
        for (i in rows.indices) {
            val row = rows[i]
            val expanded = if (row.has("sourceRef")) {
                records[reference(row, "sourceRef", records.size)].objectValue(attributes).also {
                    it.add("surfaces", if (allowSurfaceMasks && row.has("surfaceMask")) maskedSurfaces(row, surfaceKey, allowSurfaceMasks)
                        else row.get("surfaces").deepCopy())
                }
            } else expandAttributes(row, attributes)
            if (allowSurfaceMasks && row.has("surfaceMask") && !row.has("sourceRef")) {
                expanded.remove("surfaceMask")
                expanded.add("surfaces", maskedSurfaces(row, surfaceKey, allowSurfaceMasks))
            }
            outRows.set(i, ordered(expanded))
        }
        return result
    }
}

internal data class SharedSourceMetadata(val encodedJson: String, val sources: SharedSources,
                                         val surfaceKey: String, val surfaceMasks: Boolean) {
    fun expandedJson(): String = sources.expand(JsonParser.parseString(encodedJson), surfaceKey = surfaceKey,
        allowSurfaceMasks = surfaceMasks)!!.toString()
}
