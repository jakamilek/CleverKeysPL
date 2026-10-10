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
    private const val MAX_NODES = 1_000_000
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

    fun document(reader: Reader): JsonObject = JsonReader(reader).use { input ->
        input.isLenient = false
        var nodes = 0
        fun read(depth: Int): JsonElement {
            require(depth <= 32 && ++nodes <= MAX_NODES) { "JSON complexity limit" }
            fun bounded(value: String): String {
                require(value.length <= 16_384) { "JSON string limit" }
                return value
            }
            return when (input.peek()) {
                JsonToken.BEGIN_OBJECT -> {
                    input.beginObject()
                    val obj = JsonObject()
                    while (input.hasNext()) {
                        val name = bounded(input.nextName())
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
        val root = read(0)
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

    fun parse(reader: Reader, language: String, version: Int, capabilities: Set<String>): LanguageIntelligenceProvider {
        val obj = document(reader)
        require(integer(obj, "schemaVersion") == 1 && string(obj, "languageCode") == language)
        val entries = obj.getAsJsonArray("entries")
        require(entries.size() <= MAX_ENTRIES)
        val index = LinkedHashMap<String, LanguageIntelligence>()
        for (element in entries) {
            val entry = element.asJsonObject
            val key = string(entry, "surfaceKey")
            require(key.isNotBlank() && key.length <= 96 && key == key.lowercase(Locale.ROOT))
            require(key.none { it.isWhitespace() || it.isISOControl() })
            require(!index.containsKey(key)) { "Duplicate surfaceKey" }
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
            index[key] = LanguageIntelligence(key, canonical, caps, metadata?.toString(), sourceLemmas(metadata, key))
        }
        return LanguageIntelligenceProvider(
            IntelligencePackageInfo(language, version, obj.get("provenance")?.toString()), capabilities, index
        )
    }
}
