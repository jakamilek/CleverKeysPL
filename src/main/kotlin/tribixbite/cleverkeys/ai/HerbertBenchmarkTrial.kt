package tribixbite.cleverkeys.ai

/** Verified producer run 37230171787, commit d831e17b6cb99590d6ba036e92a72b6c3fd0cc7c.
 * Metadata ZIP sha256 2310649ba6e8c9b72e40952687d88a50940a71d6cfadb186ca055138bdea91d6.
 * Full hashes are compiled provenance, never accepted from a user ZIP.
 * Model remains external; this diagnostic screen cannot enable live SI.
 */
internal object HerbertBenchmarkTrial {
    private val verified = HerbertBundleTrust(mapOf(
        "NOTICE.txt" to HerbertFileIdentity(1653L, "b9b8af816cbed0c0a5e7565a224407399d028313b2b2ff2a089a86d67d70bc28"),
        "android-score-vectors.json" to HerbertFileIdentity(109889L, "c9ce34814baa697aa9e6d58d9337eb5e5fd91057eb06bfeb34be4874bdc9cb6a"),
        "manifest.json" to HerbertFileIdentity(1243L, "667bd4fee413a13ca8edca75f5b7defff2c58d0450d8d87ab8e9ccef948026b0"),
        "model.onnx" to HerbertFileIdentity(651798883L, "f851436ba9ca35d0c7313ff873cd869b744b295a8a16fc95b4571d5e11b299f2"),
        "portable-tokenizer.json" to HerbertFileIdentity(1468574L, "ee9b13d7732fb22dc7b28d51751daebbe5caaac484ba69d7104b80b52b32681b"),
        "tokenizer-conformance.json" to HerbertFileIdentity(225506L, "b840d6c51df89d15f4797b0c9e06690ed530b5ef85792451a30356edfcce7f28"),
        "tokenizer.json" to HerbertFileIdentity(3688783L, "2e045c82c9ea8b5bc54162d92d5c72141492df88eff098f3e5436e499d1e29fb"),
    ))
    fun trust(): HerbertBundleTrust = verified
}
