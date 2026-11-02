# Hybrid AI Translation App (Online + Local Edge)

> **Official Submission for Hacktoberfest x MOSS Hack Day Udupi**  
> *Hosted by Manipal Open Source Society (MOSS) & Major League Hacking (MLH)*  
> *MIT Manipal Campus, AB4, Rooms 501–504 · Sunday, 25 October 2026*  
>
> 🏆 **Challenges Entered:**  
> 1. **Best Open-Source AI Project**  
> 2. **Best Use of Gemma 4**  



## 1. What the Project Does & How It Works

### The Problem
In disaster relief camps, emergency clinics, and remote crisis zones, critical communication failures can be catastrophic:
1. **Connectivity Blackouts:** Internet is frequently non-existent or unreliable in humanitarian crises and remote clinics.
2. **Fatal AI Terminology Drift:** Generic translation engines hallucinate or provide colloquial approximations for life-critical terms (e.g., mistranslating *"anaphylaxis"* or *"potable water shelter"*).
3. **Cluttered & Unnatural UI:** Most translators require awkward device-passing or upside-down reading, creating friction during high-stress interactions.

### The Solution: Hybrid AI Translation
**Hybrid AI Translation App** is a native Android application that provides zero-downtime translation across both cloud and offline environments:

- **Apple-Inspired Clutter-Free UI:** Clean, upright, single-direction interface. Features source input, instant translation, audio playback, language swap (`⇄`), and confidence-scored alternative candidates.
- **Physical Edge / Cloud Toggle:** An intuitive hardware-style toggle switch allowing instant swapping between **`⚡ Local Edge Model (Gemma 4 E2B)`** and **`☁️ Cloud API (Gemma 4 Online)`**.
- **Context Setter (Agent Skill Trigger):** Sets the operational domain (e.g., *Medical Emergency*, *Disaster Relief*, *Legal Consultation*), which dynamically loads canonical terminology into the inference engine.
- **Agent Skill (`glossary-lookup`):** Conforms strictly to the **Agent Skills open standard** (`skill.json`), injecting curated multi-lingual glossary rules before generation.
- **Original Model Harness:** An original harness (`ModelHarness.kt`) that handles prompt construction, conversation memory, tool execution, candidate probability scoring, and hybrid routing.
- **Multi-Candidate Scoring:** Outputs the primary context-appropriate translation alongside alternative variations (Formal, Colloquial, Literal) with explicit probability/confidence scores ($0\text{--}100\%$).
- **Script Integrity:** Guarantees strict native script generation (Devanagari for Hindi, Kannada, Telugu, Tamil, Spanish) with zero raw English leakage.
- **Voice Recognition & TTS:** Hands-free speech recognition with real-time RMS volume metering and target-language audio speech synthesis.

---

## 2. System Architecture & Hybrid Routing

```
┌─────────────────────────────────────────────────────────────────────────┐
│               Native Jetpack Compose UI       │
│  - Context Setter Pill (Agent Skill Active)                             │
│  - Physical Toggle Switch: [⚡ Local Edge] <-----> [☁️ Cloud API]         │
│  - Source Input Card (Speech-to-Text / Audio RMS Meter)                 │
│  - Target Translation Card + Alternative Probability Candidates         │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        Original Model Harness                           │
│                 (com.moss.hybridtranslator.harness)                     │
│                                                                         │
│  1. Domain Evaluation (Medical, Disaster Relief, Legal, General)        │
│  2. Agent Skill Execution (agent-skills/glossary-lookup/skill.json)     │
│  3. Strict System Prompt Assembly & Conversation Memory Window          │
│  4. Dynamic Hybrid Engine Dispatcher                                    │
└───────────────────┬─────────────────────────────────┬───────────────────┘
                    │                                 │
     (Online Route) │                                 │ (Offline Route)
                    ▼                                 ▼
┌──────────────────────────────────────┐  ┌───────────────────────────────┐
│          Google Gemini API           │  │   Local Edge Engine (Gemma 4) │
│       (`GeminiGemmaService.kt`)      │  │   (`LocalGemmaService.kt`)    │
│                                      │  │                               │
│  - Model: `gemma-4-26b-a4b-it`       │  │  - Gemma 4 E2B `.litertlm`    │
│  - Turbo Flash: `gemini-3.5-flash`   │  │  - LiteRT-LM Engine +         │
│  - Deep reasoning cloud translation  │  │    Conversation API           │
│  - Sub-second responsive generation  │  │  - GPU first, CPU fallback    │
│  - 0% Thinking Overhead optimization │  │  - 100% Zero-Network Private  │
│                                      │  │  - Explicit phrasebook only   │
│                                      │  │    if the model is unavailable│
└───────────────────┬──────────────────┘  └───────────────┬───────────────┘
                    │                                     │
                    └──────────────────┬──────────────────┘
                                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                   Multi-Candidate Output & Telemetry                    │
│  - Primary Translation (Context Formal / Respectful)                    │
│  - Alternative Candidates (Colloquial, Literal) + Confidence % Scores   │
│  - Real-Time Execution Latency Badge (e.g., ⚡ 120ms Edge / 1.4s Cloud) │
│  - Android Native TextToSpeech (TTS) Immediate Audio Output             │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Team Members

- **Aditya Trivedi** — Developer & Project Lead (`adt1050@gmail.com`)
- **Aaryan Kumar B** — Developer & Collaborator (GitHub: [`@aaryankb`](https://github.com/aaryankb))

*Both team members are registered, checked-in students at MIT Manipal for Hacktoberfest x MOSS Hack Day Udupi.*

---

## 4. Open-Source License

This project is open-source software licensed under the **OSI-approved [MIT License](LICENSE)**.

```
Copyright (c) 2026 Aditya Trivedi, Aaryan Kumar B
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files...
```

---

## 5. Open-Weight Models & Licenses

In accordance with the hackathon rulebook definitions for **open-weight models**:

| Model Name | Parameter Class | Role in Project | License / Terms Link |
|---|---|---|---|
| **Google Gemma 4** (`gemma-4-26b-a4b-it`) | Large Language Model (> 10B parameters: 26B Mixture-of-Experts) | Cloud reasoning translation via Google Gemini API | [Gemma Terms of Use](https://ai.google.dev/gemma/terms) |
| **Google Gemma 4 E2B** (`gemma-4-E2B-it-litert-lm`) | Small Language Model (≤ 10B parameters: ~2.3B parameters) | On-device offline edge execution via Google LiteRT-LM (`Engine` + `Conversation`) | [Gemma Open Model License](https://ai.google.dev/gemma/terms) |

---

## 6. Libraries, Frameworks, APIs, and Datasets

### Libraries & Frameworks
- **Language & Runtime:** Kotlin 2.4.0, Java 17 bytecode target, Android SDK (API 26–35)
- **UI Toolkit:** Jetpack Compose + Material 3 (Apple Translate aesthetic, single-direction cards)
- **Cloud AI Client:** `com.google.ai.client.generativeai:generativeai:0.9.0` (Official Google Generative AI Android SDK)
- **Edge AI Client:** `com.google.ai.edge.litertlm:litertlm-android:0.19.0` (Google LiteRT-LM runtime for Android; 64-bit `arm64-v8a` / `x86_64` only)
- **Offline Model:** `gemma-4-E2B-it.litertlm` (2.6 GB, downloaded on demand from the LiteRT community on Hugging Face)
- **Speech & Audio:** Android `android.speech.SpeechRecognizer` and `android.speech.tts.TextToSpeech`
- **Asynchronous Processing:** Kotlinx Coroutines (`Dispatchers.IO`, `StateFlow`)
- **JSON Serialization:** Kotlinx Serialization (`kotlinx.serialization.json.Json`)
- **Build System:** Gradle 8.11.1, Android Gradle Plugin (AGP) 8.7.2

### Curated Datasets
Curated multi-lingual terminology datasets bundled under [`agent-skills/glossary-lookup/data/`](agent-skills/glossary-lookup/data/):
- **`medical.json`**: Anaphylaxis, epinephrine, myocardial infarction, tourniquet, triage, resuscitation, glucose, etc.
- **`disaster_relief.json`**: Potable water shelter, ration distribution point, evacuation zone, casualty clearing station, etc.
- **`legal.json`**: Asylum affidavit, non-refoulement, temporary protection directive, legal counsel, bail bond, etc.

Supported language pairings: English (`en`) $\longleftrightarrow$ Kannada (`kn`), Hindi (`hi`), Telugu (`te`), Tamil (`ta`), Spanish (`es`).

---

## 7. Mandatory AI Coding Tools Disclosure

In strict adherence to **Section 06 of the Student Rulebook**, the following AI tools were used to build this project:

| AI Tool Name | Tool Category | Role & Usage | Exact Project Parts Produced |
|---|---|---|---|
| **Google Gemini** | Chat Assistant & LLM | Architecture ideation, terminology dataset generation, prompt strategy, and translation evaluation | Prompt engineering templates for `ModelHarness.kt` and multi-lingual dictionary data in `agent-skills/glossary-lookup/data/`. |
| **Google Antigravity** | Agentic Coding Assistant | Scaffolding, code authoring, Jetpack Compose UI layout, service refactoring, unit tests, and ADB deployment | Implemented Compose UI (`FaceToFaceScreen.kt`), `ModelHarness.kt`, `LocalGemmaService.kt`, `LocalEdgeTranslationEngine.kt`, `SpeechService.kt`, and test suites. |
| **VS Code Autocomplete** | Code Completion Tool | Inline coding suggestions and syntax completion | Autocompletion for Kotlin function signatures, Compose modifiers, imports, and Gradle dependencies. |

*Note: All architectural designs, agent skill definitions, model harnesses, and code integrations were reviewed, audited, and tested by Aditya Trivedi and Aaryan Kumar B.*

---

## 8. How to Build & Run (Step-by-Step)

### Prerequisites
- **JDK:** JDK 21 (Android Studio's bundled JBR 21 works; a newer JDK such as 27 is not compatible with AGP 8.7.2)
- **Android SDK:** Platforms API 34 or 35 with Build-Tools `35.0.0`
- **Device:** Physical 64-bit Android device connected via ADB (recommended for microphone, TTS, and GPU inference) or a 64-bit emulator
- **Storage:** About 2.6 GB free on the device for the optional offline model

### Build Commands

1. **Clone Repository:**
   ```bash
   git clone https://github.com/aaryankb/MLH-HACKHACK.git
   cd MLH-HACKHACK
   ```

2. **Configure SDK Path:**
   Ensure `local.properties` exists with your Android SDK location:
   ```properties
   sdk.dir=/home/<user>/Android/Sdk
   ```

3. **Run Unit Tests:**
   Execute the automated test suite verifying the Agent Skill, Model Harness, and Offline Engine:
   ```bash
   ./gradlew testDebugUnitTest
   ```

4. **Build the Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   The APK is compiled to: `app/build/outputs/apk/debug/app-debug.apk`.

5. **Install on Connected Device via ADB:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

6. **Launch the Application:**
   ```bash
   adb shell am start -n com.moss.hybridtranslator/.MainActivity
   ```

7. **Install the Offline Model (Optional):**
   Open the offline-model dialog in the app and tap **Download**. The app fetches
   `gemma-4-E2B-it.litertlm` (about 2.6 GB) from Hugging Face, resumes interrupted
   transfers, validates the `LITERTLM` header, and loads it automatically.
   The file is stored in app-specific external storage, so no broad storage
   permission is needed:

   ```text
   /sdcard/Android/data/com.moss.hybridtranslator/files/models/gemma-4-E2B-it.litertlm
   ```

   To sideload it instead (for example on a device without Wi-Fi):

   ```bash
   adb push gemma-4-E2B-it.litertlm \
     /sdcard/Android/data/com.moss.hybridtranslator/files/models/
   ```

   The model is not bundled in the APK. The first load takes a few seconds; the
   app tries the GPU/OpenCL backend first and falls back to CPU automatically.
   If both backends fail, the UI says so explicitly and the emergency phrasebook
   is used instead — it is never presented as model output.

---

## 9. Challenge Alignment 1: Best Use of Gemma 4

### How Gemma 4 is Used Through the Gemini API
- **Model Identified:** **`gemma-4-26b-a4b-it`** (Google Gemma 4 open-weight 26B instruction-tuned model) and **`gemini-3.5-flash`** (Turbo fast sub-second tier).
- **Service Implementation:** Located at [`app/src/main/java/com/moss/hybridtranslator/services/GeminiGemmaService.kt`](app/src/main/java/com/moss/hybridtranslator/services/GeminiGemmaService.kt).
- **SDK Integration:** Built using Google's official Generative AI SDK (`com.google.ai.client.generativeai.GenerativeModel`).
- **Prompt Harnessing:** Prompts are synthesized by our custom model harness ([`ModelHarness.kt`](app/src/main/java/com/moss/hybridtranslator/harness/ModelHarness.kt)), injecting conversation history, user domain context, and canonical glossary rules directly into the Gemma 4 system prompt.
- **Structured Multi-Candidate Output:** Gemma 4 generates a structured JSON output specifying the primary translation and up to two alternative variations (Formal, Colloquial, Literal) with confidence probability scores:
  ```json
  {
    "primary": "नमस्ते भाई",
    "alternatives": [
      { "text": "नमस्ते भाईसाहब", "type": "Formal", "confidence": 92 },
      { "text": "नमस्ते आदमी", "type": "Literal", "confidence": 65 }
    ]
  }
  ```
- **Turbo Flash Mode (Latency Optimization):** Provides an instant top-bar mode switcher between **`⚡ Fast`** (`gemini-3.5-flash`, 1–2s response) and **`🧠 Deep`** (`gemma-4-26b-a4b-it` full reasoning), solving cloud latency bottlenecks during live conversational speech.

---

## 10. Challenge Alignment 2: Best Open-Source AI Project

### 1. Open-Weight Model Usage
- Operates Google Gemma 4 models both in the cloud and on the edge.
- Features **Gemma 4 E2B** running fully on-device through Google **LiteRT-LM** (`gemma-4-E2B-it-litert-lm`), an open-weight small language model (SLM). The 2.6 GB `.litertlm` bundle is stored under app-specific external storage (`Android/data/com.moss.hybridtranslator/files/models/`) and can be downloaded in-app or side-loaded with ADB.

### 2. Agent Skill Following the Open Standard
- **Directory:** [`agent-skills/glossary-lookup/`](agent-skills/glossary-lookup/)
- **Standard:** Follows the **Agent Skills open standard** specification:
  - [`skill.json`](agent-skills/glossary-lookup/skill.json): Declares schema, author, license, input/output parameters, and trigger conditions.
  - [`SKILL.md`](agent-skills/glossary-lookup/SKILL.md): Comprehensive instructions on domain classification and translation rules.
  - [`GlossaryLookupSkill.kt`](app/src/main/java/com/moss/hybridtranslator/harness/GlossaryLookupSkill.kt): Android skill runner executing token normalization and glossary extraction before model invocation.

### 3. Original Model Harness
- **Source:** [`app/src/main/java/com/moss/hybridtranslator/harness/ModelHarness.kt`](app/src/main/java/com/moss/hybridtranslator/harness/ModelHarness.kt)
- **Features:**
  - Original Kotlin implementation of a model harness (not a wrapper or reskin).
  - Evaluates operational context dynamically.
  - Automatically executes the `glossary-lookup` agent skill.
  - Enforces native-script output rules (e.g. Devanagari or Kannada scripts, rejecting Latin transliterations).
  - Generates multi-candidate alternatives with probability scoring.
  - Routes traffic seamlessly between the Google LiteRT-LM edge runtime and Gemini Cloud.
  - Emits telemetry (latency, active terms, domain matching) surfaced directly in the interactive **Agent Skill Inspector Dialog**.

### 4. Honest Offline Fallback Engine
- When in offline mode, [`LocalGemmaService.kt`](app/src/main/java/com/moss/hybridtranslator/services/LocalGemmaService.kt) runs Gemma 4 E2B through LiteRT-LM's `Engine` and `Conversation` APIs. It tries GPU/OpenCL first and automatically retries on CPU if that backend cannot initialize.
- If the model is missing, still downloading, or cannot load, [`LocalEdgeTranslationEngine.kt`](app/src/main/java/com/moss/hybridtranslator/services/LocalEdgeTranslationEngine.kt) supplies a curated emergency phrasebook for common medical, disaster-relief, travel, and daily phrases.
- The fallback is always labelled as **“Emergency phrasebook (offline fallback …)”** in the engine badge. It is never presented as neural model output, and the app reports load errors instead of silently pretending a translation came from Gemma.

---

## 11. User Experience

- ** Translate Style Upright UI:** Single-direction clean view with high-contrast cards.
- **Physical Toggle Switch:** Effortlessly switch between `⚡ Local Edge Model (Gemma 4 E2B)` and `☁️ Cloud API (Gemma 4 Online)`.
- **Live Latency & Engine Badge:** Instant visual confirmation of speed (e.g. `⚡ 120ms • Local Edge` or `⚡ 1.4s • Gemini Flash`).
- **Interactive Agent Skill Inspector:** Tap the brain icon (`Psychology`) in the top app bar to view raw injected prompts, active domain matching, and glossary parameters in real-time.
- **Persistent Model Cache:** The offline bundle lives in app-specific external storage (`Android/data/com.moss.hybridtranslator/files/models/`), so it survives app updates and reinstalls made with `adb install -r` without downloading 2.6 GB again. Interrupted downloads resume from a `.part` file.
- **Truthful Offline Status:** The top bar and model dialog distinguish *not installed*, *downloading*, *loading*, *ready*, and *failed*, and the translation badge shows the exact engine that produced each result.



HERE IS THE APK LINK FOR YOUR PHONE : https://drive.google.com/drive/folders/1PyQEIWtnwg-wCELZv2x446-eJKWtelMC
---
