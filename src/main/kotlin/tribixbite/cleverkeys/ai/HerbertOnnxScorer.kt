package tribixbite.cleverkeys.ai

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.security.MessageDigest

/**
 * Actual CPU ONNX implementation of the frozen WWM graph. Call only on a dedicated worker.
 * No logging or editor access. Close is serialized with inference; the global environment
 * is borrowed and never closed here. Deliberately not wired into the live suggestion path yet.
 */
internal class HerbertOnnxScorer private constructor(
    private val environment: OrtEnvironment,
    private val session: OrtSession,
) : AutoCloseable {
    private var closed = false

    @Synchronized
    fun score(batch: HerbertPreparedBatch): Map<String, Float> {
        check(!closed)
        val tensors = LinkedHashMap<String, OnnxTensor>()
        try {
            val sequenceShape = longArrayOf(batch.size.toLong(), batch.sequence.toLong())
            val targetShape = longArrayOf(batch.size.toLong(), batch.targets.toLong())
            tensors["input_ids"] = OnnxTensor.createTensor(environment, LongBuffer.wrap(batch.inputIds()), sequenceShape)
            tensors["attention_mask"] = OnnxTensor.createTensor(environment, LongBuffer.wrap(batch.attentionMask()), sequenceShape)
            tensors["target_positions"] = OnnxTensor.createTensor(environment, LongBuffer.wrap(batch.targetPositions()), targetShape)
            tensors["target_ids"] = OnnxTensor.createTensor(environment, LongBuffer.wrap(batch.targetTokenIds()), targetShape)
            tensors["target_mask"] = OnnxTensor.createTensor(environment, FloatBuffer.wrap(batch.targetMask()), targetShape)
            session.run(tensors).use { result ->
                val means = result.get("mean_log_probability").orElseThrow {
                    IllegalStateException("Missing HerBERT mean output")
                }.value as? FloatArray
                    ?: error("Unexpected HerBERT output")
                val sums = result.get("sum_log_probability").orElseThrow {
                    IllegalStateException("Missing HerBERT sum output")
                }.value as? FloatArray
                    ?: error("Unexpected HerBERT output")
                require(means.size == batch.size && sums.size == batch.size)
                val mask = batch.targetMask()
                return batch.surfaces.mapIndexed { b, surface ->
                    val count = (0 until batch.targets).count { mask[b * batch.targets + it] == 1f }
                    require(means[b].isFinite() && sums[b].isFinite() && count > 0)
                    require(kotlin.math.abs(sums[b] / count - means[b]) <= 0.001f)
                    surface to means[b]
                }.toMap()
            }
        } finally {
            tensors.values.forEach { it.close() }
        }
    }

    @Synchronized
    override fun close() {
        if (!closed) {
            closed = true
            session.close()
        }
    }

    companion object {
        private val inputRanks = mapOf("input_ids" to 2, "attention_mask" to 2,
            "target_positions" to 2, "target_ids" to 2, "target_mask" to 2)
        private val outputNames = setOf("mean_log_probability", "sum_log_probability")
        private const val MAX_MODEL_BYTES = 768 * 1024 * 1024

        /** Expected SHA comes from a trusted release/trial identity, never from an arbitrary pack. */
        fun open(verifiedCandidate: ByteArray, expectedSha256: String,
                 environment: OrtEnvironment = OrtEnvironment.getEnvironment()): HerbertOnnxScorer {
            require(verifiedCandidate.size in 1024..MAX_MODEL_BYTES)
            require(expectedSha256.matches(Regex("[a-f0-9]{64}")))
            // A caller cannot mutate the hash-verified bytes between validation and ORT parsing.
            val modelBytes = verifiedCandidate.copyOf()
            val actual = MessageDigest.getInstance("SHA-256").digest(modelBytes)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            require(actual == expectedSha256) { "HerBERT model hash mismatch" }
            val session = OrtSession.SessionOptions().use { options ->
                options.setIntraOpNumThreads(2)
                options.setInterOpNumThreads(1)
                options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                environment.createSession(modelBytes, options)
            }
            try {
                require(session.inputNames == inputRanks.keys && session.outputNames == outputNames)
                for ((name, rank) in inputRanks) {
                    val info = session.inputInfo.getValue(name).info as? TensorInfo
                        ?: error("Non-tensor HerBERT input")
                    val type = if (name == "target_mask") OnnxJavaType.FLOAT else OnnxJavaType.INT64
                    require(info.type == type && info.shape.size == rank)
                }
                for (name in outputNames) {
                    val info = session.outputInfo.getValue(name).info as? TensorInfo
                        ?: error("Non-tensor HerBERT output")
                    require(info.type == OnnxJavaType.FLOAT && info.shape.size == 1)
                }
                return HerbertOnnxScorer(environment, session)
            } catch (failure: Exception) {
                session.close()
                throw failure
            }
        }
    }
}
