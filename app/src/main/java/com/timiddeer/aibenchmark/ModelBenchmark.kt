package com.timiddeer.aibenchmark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.graphics.scale
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import com.timiddeer.aibenchmark.ModelBenchmark.benchmark
import kotlin.system.measureNanoTime

/**
 * Latency benchmark for the quantized MobileNet models (FP32 / INT8 / INT4) on CPU vs GPU,
 * using the modern LiteRT (V2) CompiledModel API.
 *
 * The loop, timing harness, and median math are provided. YOU fill in the LiteRT calls in
 * [benchmark] (marked TODO) - that's the part to learn: create a CompiledModel, make I/O
 * buffers, run it, close it.
 */
object ModelBenchmark {

    private const val TAG = "ModelBenchmark"
    private val MODELS = listOf(
        "mobilenet_fp32.tflite",
        "mobilenet_int8.tflite",
        "mobilenet_int4.tflite",
    )
    private const val INPUT_FLOATS = 1 * 224 * 224 * 3   // 1x224x224x3 float32 input
    private const val WARMUP = 3
    private const val RUNS = 20

    /** PROVIDED boilerplate: runs every model on CPU and GPU, returns + logs the table. Blocks. */
    fun runAll(context: Context): String {

        val bitmap = BitmapFactory.decodeStream(context.assets.open("dog.jpg"))
        val scaledBitmap = bitmap.scale(224, 224)

        val sb = StringBuilder("model  | backend | median ms\n")
        sb.appendLine("--------------------------------")
        for (model in MODELS) {
            for (accelerator in listOf(Accelerator.CPU, Accelerator.GPU)) {
                val ms = runCatching { benchmark(context, model, accelerator, scaledBitmap) }
                    .getOrElse { e -> Log.e(TAG, "failed: $model $accelerator", e); -1.0 }
                val name = model.removePrefix("mobilenet_").removeSuffix(".tflite").padEnd(6)
                val backend = accelerator.name.padEnd(7)
                val line = "$name | $backend | ${"%.1f".format(ms)}"
                Log.d(TAG, line)
                sb.appendLine(line)
            }
        }
        Log.d(TAG, "\n$sb")
        return sb.toString()
    }

    /**
     * Benchmark one model on one backend; return the MEDIAN latency in ms.
     * The timing scaffold is provided; fill in the LiteRT calls at the TODOs.
     */
    private fun benchmark(
        context: Context,
        modelName: String,
        accelerator: Accelerator,
        bitmap: Bitmap
    ): Double {
        // TODO (LiteRT): create the CompiledModel for `modelName` from assets, targeting `accelerator`.
        //   Hint: CompiledModel.create(context.assets, modelName, CompiledModel.Options(accelerator))
        val model: CompiledModel = CompiledModel.create(
            context.assets,
            modelName,
            CompiledModel.Options(accelerator)
        )

        val labels = context.assets.open("labels.txt").bufferedReader().readLines()

        val height = 224
        val width = 224
        val intArrayOfPixels = IntArray(height * width)

        bitmap.getPixels(intArrayOfPixels, 0, width, 0, 0, width, height)

        val input = FloatArray(INPUT_FLOATS)
        for (i in intArrayOfPixels.indices) {
            val pixel = intArrayOfPixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = (pixel) and 0xFF

            input[i * 3] = (r / 127.5f) - 1
            input[i * 3 + 1] = (g / 127.5f) - 1
            input[i * 3 + 2] = (b / 127.5f) - 1
        }

        return try {
            // TODO (LiteRT): create input + output buffers, then fill the input once.
            //   Hint: val inputs = model.createInputBuffers()
            //         val outputs = model.createOutputBuffers()
            //         inputs[0].writeFloat(FloatArray(INPUT_FLOATS) { 0f })

            val inputs = model.createInputBuffers()
            val outputs = model.createOutputBuffers()
            inputs[0].writeFloat(input)

            // --- PROVIDED timing: warm up (untimed), then time RUNS runs, return the median ms ---
            repeat(WARMUP) {
                val warmUpTime = measureNanoTime {
                    model.run(inputs, outputs)
                }.nanoToMs()

                Log.e(TAG, "warm up: model = $modelName : time = $warmUpTime ms")
            }
            val times = DoubleArray(RUNS) {
                val runTime = measureNanoTime {
                    model.run(inputs, outputs)
                }.nanoToMs()

                Log.e(TAG, "run time: model = $modelName : time = $runTime ms")
                runTime
            }

            val scores = outputs[0].readFloat()
            val top = scores.indices.maxByOrNull { scores[it] } ?: 0
            val resultLabel = labels[top]
            Log.e(TAG, "$modelName predicted = $resultLabel")

            times.sorted()[RUNS / 2]
        } finally {
            // TODO (LiteRT): model.close()
            model.close()
        }
    }
}

fun Long.nanoToMs(): Double {
    return this / 1_000_000.0
}
