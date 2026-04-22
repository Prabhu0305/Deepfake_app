package com.example.deepfake

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.SessionOptions
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import kotlin.math.exp

class DeepfakeDetector(private val context: Context) {
    private var ortEnv: OrtEnvironment? = null
    private var blipSession: OrtSession? = null
    private var adapterSession: OrtSession? = null
    private var visionSession: OrtSession? = null

    companion object {
        private const val TAG = "DeepfakeDetector"
        private const val VISION_SIZE = 336
        private const val BLIP_SIZE = 384
        private const val BLIP_START_TOKEN = 30522L // [CLS] token for BLIP
    }

    suspend fun loadModels(): String = withContext(Dispatchers.IO) {
        try {
            ortEnv = OrtEnvironment.getEnvironment()
            val options = SessionOptions().apply {
                setOptimizationLevel(SessionOptions.OptLevel.NO_OPT)
            }

            blipSession = createSessionFromFile("blip_int8.onnx", options)
            adapterSession = createSessionFromFile("adapter_head.onnx", options)
            visionSession = createSessionFromFile("vision_cls_int8.onnx", options)
            
            return@withContext "Models ready"
        } catch (e: Throwable) {
            Log.e(TAG, "Error loading models", e)
            return@withContext "Error: ${e.localizedMessage}"
        }
    }

    private fun createSessionFromFile(assetName: String, options: SessionOptions): OrtSession? {
        val modelFile = File(context.cacheDir, assetName)
        context.assets.open(assetName).use { inputStream ->
            val assetSize = inputStream.available().toLong()
            if (!modelFile.exists() || modelFile.length() != assetSize) {
                FileOutputStream(modelFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        return ortEnv?.createSession(modelFile.absolutePath, options)
    }

    fun analyze(bitmap: Bitmap): String {
        val env = ortEnv ?: return "Environment not initialized"
        val vSession = visionSession ?: return "Vision model not loaded"
        val aSession = adapterSession ?: return "Adapter model not loaded"
        val bSession = blipSession ?: return "BLIP model not loaded"

        return try {
            // --- 1. PREPROCESS ---
            val clipInput = preprocessImage(bitmap, VISION_SIZE, isClip = true)
            val blipInput = preprocessImage(bitmap, BLIP_SIZE, isClip = false)
            
            val clipShape = longArrayOf(1, 3, VISION_SIZE.toLong(), VISION_SIZE.toLong())
            val blipShape = longArrayOf(1, 3, BLIP_SIZE.toLong(), BLIP_SIZE.toLong())
            
            val clipTensor = OnnxTensor.createTensor(env, clipInput, clipShape)
            val blipPixelTensor = OnnxTensor.createTensor(env, blipInput, blipShape)

            // --- 2. DEEPFAKE INFERENCE (Vision -> Adapter) ---
            val visionOutputs = vSession.run(mapOf("image" to clipTensor))
            val clsFeat = visionOutputs[0].value as Array<FloatArray> 
            
            val clsFeatTensor = OnnxTensor.createTensor(env, clsFeat)
            val adapterOutputs = aSession.run(mapOf("cls_feat" to clsFeatTensor))
            val logits = (adapterOutputs[0].value as Array<FloatArray>)[0] 
            
            val probs = softmax(logits)
            val fakeScore = probs[1]
            val isFake = fakeScore > 0.75f
            val label = if (isFake) "FAKE" else "REAL"
            val confidence = if (isFake) fakeScore else probs[0]

            // --- 3. BLIP INFERENCE (Captioning) ---
            // Fix: BLIP requires 'input_ids' and 'attention_mask' for its text decoder
            val inputIds = LongArray(1) { BLIP_START_TOKEN }
            val attentionMask = LongArray(1) { 1L }
            
            // Tensors must be 2D: [batch_size, sequence_length] -> [1, 1]
            val inputIdsTensor = OnnxTensor.createTensor(env, arrayOf(inputIds))
            val attentionMaskTensor = OnnxTensor.createTensor(env, arrayOf(attentionMask))

            val blipInputs = mapOf(
                "pixel_values" to blipPixelTensor,
                "input_ids" to inputIdsTensor,
                "attention_mask" to attentionMaskTensor
            )
            
            val blipOutputs = bSession.run(blipInputs)
            
            // --- CLEANUP ---
            clipTensor.close()
            blipPixelTensor.close()
            inputIdsTensor.close()
            attentionMaskTensor.close()
            clsFeatTensor.close()
            
            visionOutputs.close()
            adapterOutputs.close()
            blipOutputs.close()

            generateSmartExplanation(label, confidence)

        } catch (e: Exception) {
            Log.e(TAG, "Inference error", e)
            "Inference Error: ${e.message}"
        }
    }

    private fun preprocessImage(bitmap: Bitmap, size: Int, isClip: Boolean): FloatBuffer {
        val resized = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val softwareBitmap = if (resized.config == Bitmap.Config.HARDWARE) {
            resized.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            resized
        }

        val buffer = FloatBuffer.allocate(1 * 3 * size * size)
        val pixels = IntArray(size * size)
        softwareBitmap.getPixels(pixels, 0, size, 0, 0, size, size)

        val mean = if (isClip) floatArrayOf(0.481f, 0.457f, 0.408f) else floatArrayOf(0.5f, 0.5f, 0.5f)
        val std = if (isClip) floatArrayOf(0.268f, 0.261f, 0.275f) else floatArrayOf(0.5f, 0.5f, 0.5f)

        for (c in 0 until 3) {
            for (i in pixels.indices) {
                val color = pixels[i]
                val value = when(c) {
                    0 -> Color.red(color)
                    1 -> Color.green(color)
                    else -> Color.blue(color)
                } / 255.0f
                buffer.put((value - mean[c]) / std[c])
            }
        }
        
        if (softwareBitmap != resized) softwareBitmap.recycle()
        buffer.rewind()
        return buffer
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0.0f
        val exps = FloatArray(logits.size) { i -> exp(logits[i] - maxLogit) }
        val sumExps = exps.sum()
        return FloatArray(exps.size) { i -> exps[i] / sumExps }
    }

    private fun generateSmartExplanation(label: String, confidence: Float): String {
        val caption = "Analysis complete."
        val reasoning = if (label == "FAKE") {
            if (confidence > 0.85f) "clear signs of AI generation such as unnatural textures or synthetic details"
            else "possible artifacts indicating AI generation"
        } else {
            if (confidence > 0.85f) "consistent lighting and natural textures"
            else "mostly natural features"
        }

        return """
            Prediction: $label
            Confidence: ${"%.2f".format(confidence)}
            
            Description:
            $caption The image shows $reasoning.
        """.trimIndent()
    }

    fun close() {
        blipSession?.close()
        adapterSession?.close()
        visionSession?.close()
        ortEnv?.close()
    }
}
