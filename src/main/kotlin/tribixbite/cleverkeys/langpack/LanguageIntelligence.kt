package tribixbite.cleverkeys.langpack

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.Reader
import java.util.Collections
import java.util.Locale

data class SurfaceVariant(val surface: String, val casePolicy: String)
data class CapitalizationInfo(val defaultSurface: String?, val variants: List<SurfaceVariant>)
data class SourceLemma(val lemma: String, val partOfSpeech: String)
data class LanguageIntelligence(
    val surfaceKey: String,
    val canonicalForm: String?,
    val capitalization: CapitalizationInfo?,
    // Opaque, immutable source data. Stage 1 does not infer meanings or run a model.
    val metadataJson: String?,
    val sourceLemmas: Set<SourceLemma> = emptySet(),
)
data class IntelligenceMember(val file: String, val schemaVersion: Int, val sha256: String)
data class IntelligencePackageInfo(val languageCode: String, val version: Int, val provenanceJson: String?)

/** Immutable snapshot. Lookup never opens a file or schedules work. */
class LanguageIntelligenceProvider internal constructor(
    private val info: IntelligencePackageInfo,
    capabilities: Set<String>,
    entries: Map<String, LanguageIntelligence>,
) {
    private val supported = Collections.unmodifiableSet(HashSet(capabilities))
    private val index = Collections.unmodifiableMap(HashMap(entries))
    fun capabilities(): Set<String> = supported
    fun packageInfo(): IntelligencePackageInfo = info
    fun lookup(surface: String): LanguageIntelligence? = index[surface.lowercase(Locale.ROOT)]
    /** Source identities are case-sensitive and include POS; never guess a lemma from a suffix. */
    fun sharesSourceLemma(first: String, second: String): Boolean {
        if ("metadata" !in supported) return false
        val a = lookup(first)?.sourceLemmas ?: return false
        val b = lookup(second)?.sourceLemmas ?: return false
        return a.any { it in b }
    }
}

/** Bounded strict JSON, including duplicate members which Gson's tree parser would overwrite. */
internal object IntelligenceJson {
    const val FILE = "language-intelligence.json"
    const val MAX_BYTES = 32L * 1024 * 1024
    // Additive Polish v6 has ~1.85M nodes. Byte/depth/entry bounds still apply.
    private const val MAX_NODES = 2_000_000
    private const val MAX_ENTRIES = 120_000

    /** Optional typed view of existing Morfeusz evidence; opaque metadata stays unchanged. */
    private fun sourceLemmas(metadata: JsonElement?, key: String): Set<SourceLemma> {
        val evidence = metadata?.takeIf { it.isJsonObject }?.asJsonObject
            ?.get("sourceEvidence")?.takeIf { it.isJsonObject }?.asJsonObject ?: return emptySet()
        val out = LinkedHashSet<SourceLemma>()
        fun text(obj: JsonObject, name: String): String? = obj.get(name)
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
        for (name in listOf("lexicalReadings", "generatedFormProofs", "interpretations")) {
            val rows = evidence.get(name)?.takeIf { it.isJsonArray }?.asJsonArray ?: continue
            for (raw in rows) {
                val row = raw.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                val surfaces = row.get("surfaces")?.takeIf { it.isJsonArray }?.asJsonArray
                val matches = text(row, "form")?.lowercase(Locale.ROOT) == key ||
                    surfaces?.any { it.isJsonPrimitive && it.asJsonPrimitive.isString &&
                        it.asString.lowercase(Locale.ROOT) == key } == true
                if (!matches) continue
                val lemma = text(row, "lemma") ?: continue
                val pos = text(row, "partOfSpeech") ?: text(row, "tag")?.substringBefore(':') ?: continue
                if (lemma.isNotBlank() && lemma.length <= 96 && pos.isNotBlank() && pos.length <= 32) {
                    out.add(SourceLemma(lemma, pos))
                    // Optional relation evidence must remain cheap at gesture time.
                    if (out.size > 32) return emptySet()
                }
            }
        }
        return Collections.unmodifiableSet(out)
    }

    private const val MAX_CHUNK_NODES = 16_384
    private const val MAX_ROOT_MEMBERS = 64

    /** One counter covers the entire stream; a single entry cannot allocate the whole budget. */
    private class BoundedJson(val input: JsonReader) {
        private var nodes = 0
        private var nodeLimit = MAX_NODES

        fun visit(depth: Int) {
            require(depth <= 32 && ++nodes <= nodeLimit) { "JSON complexity limit" }
        }

        private fun bounded(value: String): String {
            require(value.length <= 16_384) { "JSON string limit" }
            return value
        }

        fun name(): String = bounded(input.nextName())

        fun chunk(depth: Int): JsonElement {
            val previous = nodeLimit
            nodeLimit = minOf(MAX_NODES, nodes + MAX_CHUNK_NODES)
            return try { read(depth) } finally { nodeLimit = previous }
        }

        fun read(depth: Int): JsonElement {
            visit(depth)
            return when (input.peek()) {
                JsonToken.BEGIN_OBJECT -> {
                    input.beginObject()
                    val obj = JsonObject()
                    while (input.hasNext()) {
                        val name = name()
                        require(!obj.has(name)) { "Duplicate JSON member: $name" }
                        obj.add(name, read(depth + 1))
                    }
                    input.endObject()
                    obj
                }
                JsonToken.BEGIN_ARRAY -> {
                    input.beginArray()
                    val array = JsonArray()
                    while (input.hasNext()) array.add(read(depth + 1))
                    input.endArray()
                    array
                }
                JsonToken.STRING -> JsonPrimitive(bounded(input.nextString()))
                JsonToken.NUMBER -> JsonPrimitive(java.math.BigDecimal(bounded(input.nextString())))
                JsonToken.BOOLEAN -> JsonPrimitive(input.nextBoolean())
                JsonToken.NULL -> { input.nextNull(); JsonNull.INSTANCE }
                else -> throw IllegalArgumentException("Unexpected JSON token")
            }
        }
    }

    // Small manifest/document callers retain their existing strict tree interface.
    fun document(reader: Reader): JsonObject = JsonReader(reader).use { input ->
        input.isLenient = false
        val root = BoundedJson(input).read(0)
        require(input.peek() == JsonToken.END_DOCUMENT && root.isJsonObject) { "Invalid JSON document" }
        root.asJsonObject
    }

    fun integer(obj: JsonObject, field: String): Int {
        val value = obj.get(field)
        require(value != null && value.isJsonPrimitive && value.asJsonPrimitive.isNumber)
        return value.asBigDecimal.intValueExact()
    }

    fun string(obj: JsonObject, field: String): String {
        val value = obj.get(field)
        require(value != null && value.isJsonPrimitive && value.asJsonPrimitive.isString)
        return value.asString
    }

    fun declaration(obj: JsonObject): IntelligenceMember? {
        val hasApi = obj.has("apiVersion")
        if (hasApi) require(integer(obj, "apiVersion") == 1) { "Unsupported apiVersion" }
        if (!obj.has("languageIntelligence")) return null
        require(hasApi) { "Missing apiVersion" }
        val member = obj.getAsJsonObject("languageIntelligence")
        val file = string(member, "file")
        val schema = integer(member, "schemaVersion")
        val hash = string(member, "sha256")
        require(file == FILE && schema == 1 && hash.matches(Regex("[a-fA-F0-9]{64}")))
        return IntelligenceMember(file, schema, hash)
    }

    fun capabilities(obj: JsonObject): Set<String> {
        if (!obj.has("capabilities")) return emptySet()
        val values = obj.getAsJsonArray("capabilities")
        require(values.size() <= 64)
        return values.map {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isString && it.asString.length <= 64)
            it.asString
        }.toSet()
    }

    private fun entry(entry: JsonObject): LanguageIntelligence {
        val key = string(entry, "surfaceKey")
        require(key.isNotBlank() && key.length <= 96 && key == key.lowercase(Locale.ROOT))
        require(key.none { it.isWhitespace() || it.isISOControl() })
        val caps = entry.get("capitalization")?.takeUnless { it.isJsonNull }?.asJsonObject?.let { cap ->
            val variants = cap.getAsJsonArray("variants")
            require(variants.size() in 1..2)
            val seen = HashSet<String>()
            val forms = variants.map { raw ->
                val form = raw.asJsonObject
                val surface = string(form, "surface")
                val policy = string(form, "casePolicy")
                require(surface.lowercase(Locale.ROOT) == key && seen.add(surface))
                require(when (policy) {
                    "lowercase" -> surface == key
                    "capitalized" -> surface == key.replaceFirstChar { it.titlecase(Locale.ROOT) }
                    else -> false
                }) { "Unsupported capitalization policy" }
                SurfaceVariant(surface, policy)
            }
            val default = cap.get("defaultSurface")?.takeUnless { it.isJsonNull }?.let {
                string(cap, "defaultSurface")
            }
            require(default == null || default in seen)
            CapitalizationInfo(default, Collections.unmodifiableList(forms))
        }
        val canonical = entry.get("canonicalForm")?.takeUnless { it.isJsonNull }?.let {
            string(entry, "canonicalForm").also { value -> require(value.length in 1..96) }
        }
        val metadata = entry.get("metadata")
        return LanguageIntelligence(key, canonical, caps, metadata?.toString(), sourceLemmas(metadata, key))
    }

    /** Read one bounded entry at a time, never a tree for the entire sidecar. */
    fun parse(reader: Reader, language: String, version: Int, capabilities: Set<String>): LanguageIntelligenceProvider =
        JsonReader(reader).use { input ->
            input.isLenient = false
            val json = BoundedJson(input)
            json.visit(0)
            require(input.peek() == JsonToken.BEGIN_OBJECT) { "Invalid JSON document" }
            input.beginObject()
            val members = HashSet<String>()
            val header = JsonObject()
            val index = LinkedHashMap<String, LanguageIntelligence>()
            var hasEntries = false
            while (input.hasNext()) {
                val name = json.name()
                require(members.add(name)) { "Duplicate JSON member: $name" }
                require(members.size <= MAX_ROOT_MEMBERS) { "JSON root member limit" }
                if (name == "entries") {
                    hasEntries = true
                    json.visit(1)
                    require(input.peek() == JsonToken.BEGIN_ARRAY)
                    input.beginArray()
                    while (input.hasNext()) {
                        require(index.size < MAX_ENTRIES) { "JSON entry limit" }
                        val raw = json.chunk(2)
                        require(raw.isJsonObject)
                        val value = entry(raw.asJsonObject)
                        require(index.put(value.surfaceKey, value) == null) { "Duplicate surfaceKey" }
                    }
                    input.endArray()
                } else {
                    // Validate unknown fields too, then discard them; preserve only public header data.
                    val value = json.chunk(1)
                    if (name in setOf("schemaVersion", "languageCode", "provenance")) header.add(name, value)
                }
            }
            input.endObject()
            require(input.peek() == JsonToken.END_DOCUMENT) { "Invalid JSON document" }
            require(hasEntries && integer(header, "schemaVersion") == 1 && string(header, "languageCode") == language)
            LanguageIntelligenceProvider(
                IntelligencePackageInfo(language, version, header.get("provenance")?.toString()), capabilities, index
            )
        }
}
