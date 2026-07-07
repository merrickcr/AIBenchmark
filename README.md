# AIBenchmark

An Android app for benchmarking on-device inference latency of quantized
MobileNet models, comparing FP32, INT8, and INT4 weights on both CPU and GPU
backends using Google's [LiteRT](https://ai.google.dev/edge/litert) V2
`CompiledModel` API.

## What it does

The app runs each of the three MobileNet variants below on both the CPU and
GPU accelerators, timing 20 inference runs per combination (after 3 untimed
warm-up runs) and reporting the median latency in milliseconds. Results are
shown in the app UI and also logged to Logcat under the tag `ModelBenchmark`.

| Model              | Precision |
|--------------------|-----------|
| `mobilenet_fp32.tflite` | 32-bit float |
| `mobilenet_int8.tflite` | 8-bit integer |
| `mobilenet_int4.tflite` | 4-bit integer |

## Project structure

- `app/src/main/java/com/timiddeer/aibenchmark/MainActivity.kt` - Compose UI
  with a single "Run benchmark" button and a results table.
- `app/src/main/java/com/timiddeer/aibenchmark/ModelBenchmark.kt` - loads each
  `.tflite` model from assets, runs warm-up + timed inference passes on a
  sample image, and computes the median latency per model/backend pair.
- `app/src/main/assets/` - holds the `.tflite` model files, `labels.txt`
  (ImageNet class labels), and a sample JPEG used as benchmark input.

## Requirements

- Android Studio (current stable)
- A device or emulator running API 31+ (`minSdk = 31`)
- The three `.tflite` model files placed in `app/src/main/assets/` (see
  `PLACE_TFLITE_MODELS_HERE.txt` in that folder for exact filenames)

## Building

```
./gradlew assembleDebug
```

Or open the project in Android Studio and run the `app` configuration.
