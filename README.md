# Oracle: Predict Anything

<div align="center">

![Oracle Banner](https://img.shields.io/badge/ORACLE-PREDICT%20ANYTHING-6C5CE7?style=for-the-badge&logoColor=white)

**100% Offline &middot; Deterministic Mathematics &middot; Zero Machine Learning &middot; High-Performance Android Forecasting Engine**

<p align="center">
  <a href="#-direct-apk-downloads">
    <img src="https://img.shields.io/badge/Download-Release%20APK%20(9.4%20MB)-00E676?style=for-the-badge&logo=android&logoColor=white" alt="Download Release APK" />
  </a>
  <a href="#-direct-apk-downloads">
    <img src="https://img.shields.io/badge/Download-Debug%20APK%20(13.8%20MB)-00D2D3?style=for-the-badge&logo=android&logoColor=white" alt="Download Debug APK" />
  </a>
</p>

[![Android](https://img.shields.io/badge/Platform-Android%208.0+%20(API%2026--36)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.10-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Repository-orange?style=flat-square)](#-architecture--tech-stack)
[![Airgapped](https://img.shields.io/badge/Permissions-Zero%20Network%20(No%20INTERNET)-blueviolet?style=flat-square)](#-zero-ai--air-gapped-guarantee)
[![Tests](https://img.shields.io/badge/Unit%20Tests-100%25%20Passing-success?style=flat-square)](#-unit-testing--verification)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

</div>

---

## 📖 Overview

**Oracle: Predict Anything** is a universal, offline Android application engineered for deterministic pattern analysis, statistical time-series forecasting, and categorical recurrence modeling. 

From financial series and exam paper topic frequencies to sales forecasting, weather metrics, and sports analytics, Oracle computes future trajectories with analytical confidence bounds, backtest error scores, and rule-based explanations—**entirely on-device without any network connection or cloud dependencies**.

---

## 🛡️ Zero-AI & Air-Gapped Guarantee

Oracle operates under a strict, verifiable non-AI architectural principle:

- **100% Deterministic:** Every calculation is grounded in classical mathematical and statistical formulas written from scratch in Kotlin. The same input dataset will always produce the identical mathematical output.
- **No Machine Learning / Neural Networks:** Zero TensorFlow Lite, PyTorch Mobile, ML Kit, or ONNX runtimes.
- **No Cloud API / LLMs:** Zero network requests, zero remote servers, and zero API keys.
- **Air-Gapped AndroidManifest (`AndroidManifest.xml`):** The app declares **NO `android.permission.INTERNET`** permission. The Android operating system physically prevents the app from opening sockets or transmitting data off the device.
- **Fully Explainable:** Every forecast provides an audit trail detailing formula equations, residual variance, backtest accuracy (MAE, RMSE, MAPE), and dynamic ensemble weights.

---

## 🌟 Key Capabilities & Domains

Oracle includes tailored statistical pipelines for 7 distinct operational domains:

| Domain | Supported Series & Formats | Prediction Output |
| :--- | :--- | :--- |
| 📈 **Trading / Stocks / Crypto** | Date, Open, High, Low, Close, Volume (OHLCV) | Price trajectory, support/resistance clusters, technical momentum indicators, Bullish/Bearish signal scoring. |
| 📚 **Exam Papers (JEE / NEET / Boards)** | Chapter/Topic names, Question counts, Year-by-year Marks | Ranked high-probability topics, expected marks distribution, Markov recurrence, overdue topic detection. |
| 💼 **Sales & Revenue** | Periodic revenue, booking counts, unit sales | Seasonal demand forecasting, growth trends, backtested revenue intervals. |
| ⚽ **Sports & Match Outcomes** | Match scores, goal margins, head-to-head records | Score trajectory, performance volatility, categorical outcome likelihood. |
| ⛅ **Weather & Environmental** | Temperature, rainfall, humidity time-series | Multi-step numeric trends, seasonal smoothing, confidence bands. |
| 🏃 **Personal Metrics** | Expenses, study hours, gym weights, daily step counts | Personal habit forecasting, trajectory stability, anomaly detection. |
| ⚙️ **Custom Series** | User-defined numeric metrics and custom timestamps | Generic dynamic ensemble prediction with automated model selection. |

---

## 📐 Mathematical & Statistical Foundations

Every model in Oracle is implemented from scratch in pure Kotlin within the `com.example.oraclepredictanything.engine` package.

### 1. Numeric Time-Series Algorithms

#### a. Simple Moving Average (SMA)
Computes the unweighted arithmetic mean over a sliding window of size $k$:
$$\hat{y}_{t+1} = \frac{1}{k} \sum_{i=0}^{k-1} y_{t-i}$$

#### b. Weighted Moving Average (WMA)
Prioritizes recent observations linearly with increasing weights $w_i = i$:
$$\hat{y}_{t+1} = \frac{\sum_{i=1}^{k} i \cdot y_{t - k + i}}{\sum_{i=1}^{k} i} = \frac{2}{k(k+1)} \sum_{i=1}^{k} i \cdot y_{t - k + i}$$

#### c. Single Exponential Smoothing (EMA)
Applies recursive smoothing with an optimal smoothing constant $\alpha \in [0.05, 0.95]$ determined via internal backtest grid search:
$$\hat{y}_{t+1} = \alpha y_t + (1 - \alpha) \hat{y}_t$$

#### d. Holt's Linear Trend (Double Exponential Smoothing)
Captures persistent upward or downward trends using coupled level ($L_t$) and trend ($T_t$) equations:
$$L_t = \alpha y_t + (1 - \alpha)(L_{t-1} + T_{t-1})$$
$$T_t = \beta (L_t - L_{t-1}) + (1 - \beta) T_{t-1}$$
$$\hat{y}_{t+h} = L_t + h \cdot T_t$$
Optimal parameters $(\alpha, \beta)$ are evaluated through a two-dimensional grid search minimizing Mean Absolute Error (MAE).

#### e. Holt-Winters Additive Seasonality (Triple Exponential Smoothing)
Decomposes series containing repetitive cyclical patterns into level ($L_t$), trend ($T_t$), and seasonal factors ($S_t$) with period length $m$:
$$L_t = \alpha (y_t - S_{t-m}) + (1 - \alpha)(L_{t-1} + T_{t-1})$$
$$T_t = \beta (L_t - L_{t-1}) + (1 - \beta) T_{t-1}$$
$$S_t = \gamma (y_t - L_t) + (1 - \gamma) S_{t-m}$$
$$\hat{y}_{t+h} = L_t + h \cdot T_t + S_{t - m + (h \bmod m)}$$
*Automated Period Detection:* The engine computes autocorrelation coefficients across multiple lags $\tau$:
$$r_\tau = \frac{\sum_{t=1}^{N-\tau} (y_t - \bar{y})(y_{t+\tau} - \bar{y})}{\sum_{t=1}^{N} (y_t - \bar{y})^2}$$
The dominant peak determines period length $m$.

#### f. Ordinary Least Squares (OLS) Linear Regression
Closed-form analytical regression for linear trajectories:
$$\hat{y}_t = \beta_0 + \beta_1 t$$
$$\beta_1 = \frac{\sum_{t=1}^N (t - \bar{t})(y_t - \bar{y})}{\sum_{t=1}^N (t - \bar{t})^2}, \quad \beta_0 = \bar{y} - \beta_1 \bar{t}$$

#### g. Degree-2 Polynomial Regression
Captures non-linear acceleration while controlling overfitting. Solves the $3 \times 3$ normal equations matrix via Gaussian elimination with numerical damping:
$$\begin{bmatrix} N & \sum t & \sum t^2 \\ \sum t & \sum t^2 & \sum t^3 \\ \sum t^2 & \sum t^3 & \sum t^4 \end{bmatrix} \begin{bmatrix} a_0 \\ a_1 \\ a_2 \end{bmatrix} = \begin{bmatrix} \sum y \\ \sum ty \\ \sum t^2 y \end{bmatrix}$$
$$\hat{y}_t = a_0 + a_1 t + a_2 t^2$$

#### h. Dynamic Inverse-MAPE Ensemble
Rather than relying on a single algorithm, Oracle backtests all models on hold-out data points ($k = \max(2, \lfloor 0.2 N \rfloor)$), computes Mean Absolute Percentage Error (MAPE), and calculates normalized inverse-error weights:
$$w_m = \frac{(1 / \text{MAPE}_m)^p}{\sum_{j} (1 / \text{MAPE}_j)^p}$$
$$\hat{y}_{\text{ensemble}} = \sum_{m} w_m \hat{y}_m$$
Models with superior historical backtest accuracy dynamically receive higher weight in the final forecast.

#### i. Analytical Confidence Bands
Confidence intervals expand with forecast horizon $h$ based on residual standard deviation $\sigma_{\text{res}}$:
$$\sigma_{\text{res}} = \sqrt{\frac{1}{N - p} \sum_{t=1}^N (y_t - \hat{y}_t)^2}$$
$$\text{CI}_{80\%}(h) = \hat{y}_{t+h} \pm 1.282 \cdot \sigma_{\text{res}} \sqrt{h}$$
$$\text{CI}_{95\%}(h) = \hat{y}_{t+h} \pm 1.960 \cdot \sigma_{\text{res}} \sqrt{h}$$

---

### 2. Technical Trading Indicator Engine

For financial time-series, Oracle computes standard technical indicators from scratch:

- **Relative Strength Index (RSI):** 14-period Wilder smoothing of average gains and losses. Identifies Overbought ($>70$) and Oversold ($<30$) regions.
- **Moving Average Convergence Divergence (MACD):** Fast 12-EMA minus Slow 26-EMA with a 9-period signal line and divergence histogram.
- **Bollinger Bands:** 20-period SMA enveloped by $\pm 2$ rolling standard deviations, computing %B indicator:
  $$\%B = \frac{\text{Price} - \text{Lower Band}}{\text{Upper Band} - \text{Lower Band}}$$
- **Stochastic Oscillator:** Fast and slow %K and %D lines tracking close location relative to the high-low range.
- **Moving Average Crossovers:** Tracks Golden Cross (50-SMA crossing above 200-SMA) and Death Cross events.
- **Support & Resistance Clusters:** Scans local extrema pivots and clusters horizontal price interaction nodes.
- **Transparent Composite Scoring:** Combines indicator readings into an interpretable Bullish / Bearish / Neutral summary with an itemized attribution breakdown.

---

### 3. Categorical & Exam Topic Forecasting

When predicting categorical occurrences (e.g., JEE/NEET exam topics, sports match sequences):

- **Recency-Weighted Frequency:** Applies half-life exponential decay giving higher importance to recent exam papers:
  $$W(y) = 2^{-(y_{\text{current}} - y) / \lambda}$$
- **Markov Chain State Transitions:** Calculates first-order transition probability matrix $P(S_{t+1} \mid S_t)$ to model topic-to-topic sequencing.
- **Overdue Cycle Detection:** Computes historical inter-appearance intervals $\bar{\Delta}$. When a topic's elapsed gap since last appearance exceeds $\bar{\Delta}$, its overdue score increases proportionately.
- **Ranked Probability List:** Aggregates scores into an ordered probability distribution with expected question counts.

---

### 4. Data Quality Pre-flight Diagnostics

Before generating any forecast, Oracle executes diagnostic checks on the dataset:
- **Sample Size Sufficiency:** Enforces minimum required points per category.
- **Outlier Detection:** Dual validation via Interquartile Range (IQR $1.5 \times$) and Z-score ($|z| > 2.5$).
- **Variance Checks:** Identifies zero-variance datasets and duplicate entries.
- **Data Quality Score ($0 - 100\%$):** Automatically scales down the overall confidence gauge when data is sparse or noisy.

---

### 5. Deterministic Rule-Based Natural Language Explanations

Oracle generates human-readable audit summaries using template-based natural language generators without LLMs:
> *"Values have trended upward by +14.2% over the last 12 periods with low volatility (CV 4.1%). The Ensemble assigned highest weight (38.5%) to Holt's Linear Trend due to its superior backtest performance (MAPE 2.3%). All 7 models exhibit directional agreement."*

---

## 🎨 UI/UX Design

Built using **Jetpack Compose** and **Material 3**:
- **Custom Canvas Charts:** Hero forecast charts drawn directly with Compose `Canvas` (no external charting dependencies). Features smooth cubic Bézier curves, animated drawing paths, shaded 80%/95% confidence bands, dashed forecast projections, and touch inspection.
- **Circular Animated Confidence Gauge:** Custom drawn canvas gauge with color-coded confidence levels.
- **Modern Dark Hero Theme:** Deep-space OLED background (`#0A0E1A`), elevated glassmorphic cards (`#141927`), and vibrant accents (`#6C5CE7` violet, `#00E676` emerald, `#00D2D3` cyan).
- **Fast Data Input:** Dynamic forms, delimiter-agnostic paste box (comma, tab, newline), CSV file picker with preview table, and 1-click realistic sample demo datasets (NIFTY 50, JEE 10-Year Physics Topics, Monthly Sales, Daily Weather).
- **Export & Share:** Native Android Sharesheet intent integration to export prediction summaries and backtest audits.

---

## 📁 Repository Structure

```
oracle/
├── apk/                                        # Pre-built compilable APKs
│   ├── app-release.apk                         # Signed Production APK (9.37 MB)
│   └── app-debug.apk                           # Universal Debug APK (13.8 MB)
├── app/
│   ├── build.gradle.kts                        # Module build configuration (SDK 36/35/26)
│   ├── proguard-rules.pro                      # R8 optimization rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml             # Zero permissions (No INTERNET)
│       │   ├── kotlin/com/example/oraclepredictanything/
│       │   │   ├── MainActivity.kt             # Edge-to-edge Compose host & SplashScreen
│       │   │   ├── Navigation.kt               # 4-tab Bottom Navigation & routing
│       │   │   ├── OracleApp.kt                # Application singleton
│       │   │   ├── engine/                     # Pure Kotlin Statistical Engines
│       │   │   │   ├── BacktestUtils.kt        # MAE, RMSE, MAPE, Confidence Bands
│       │   │   │   ├── PredictionModels.kt     # Output data classes & structures
│       │   │   │   ├── Predictor.kt            # Universal Predictor interface
│       │   │   │   ├── PredictorRegistry.kt    # 7-model registry & ensemble weighting
│       │   │   │   ├── predictors/             # SMA, WMA, EMA, Holt, Holt-Winters, OLS, Poly
│       │   │   │   ├── technical/              # RSI, MACD, Bollinger, Stochastic, Signals
│       │   │   │   ├── categorical/            # Recency, Markov, Overdue topic ranker
│       │   │   │   ├── quality/                # IQR, Z-score, Data Quality Checker
│       │   │   │   └── explanation/            # Template-driven natural language generator
│       │   │   ├── data/                       # Room DB, Entities, DAOs, DataStore Preferences
│       │   │   ├── ui/                         # Compose UI Screens & Canvas Charts
│       │   │   │   ├── home/                   # Hero dashboard & category cards
│       │   │   │   ├── datasets/               # Dataset manager & creation modal
│       │   │   │   ├── dataentry/              # Manual entry, CSV picker, paste box
│       │   │   │   ├── prediction/             # Canvas forecast chart, tabs, confidence gauge
│       │   │   │   ├── history/                # Saved predictions & search filters
│       │   │   │   ├── compare/                # Side-by-side dataset comparator
│       │   │   │   ├── settings/               # Dark theme toggle, horizon slider, reset
│       │   │   │   ├── onboarding/             # 3-page animated skippable onboarding
│       │   │   │   ├── common/                 # Reusable cards, badges, gauges, empty states
│       │   │   │   └── theme/                  # Color, Typography, Shape, Theme
│       │   │   ├── viewmodel/                  # Architecture ViewModels & StateFlows
│       │   │   └── util/                       # CSV parser, JSON converters, Sample datasets
│       │   └── res/                            # Vector drawables, themes, mipmap icons
│       └── test/kotlin/com/example/oraclepredictanything/engine/
│           ├── BacktestUtilsTest.kt            # Unit tests for error metrics & confidence bands
│           ├── CategoryPredictorTest.kt        # Unit tests for Markov & recency ranking
│           ├── DataQualityTest.kt              # Unit tests for IQR & Z-score outlier detection
│           ├── HoltTest.kt                     # Unit tests for Holt's trend model
│           ├── LinearRegressionTest.kt         # Unit tests for OLS closed-form regression
│           ├── SMATest.kt                      # Unit tests for moving average calculation
│           └── TechnicalIndicatorsTest.kt      # Unit tests for RSI, MACD, Bollinger Bands
├── gradle/
│   ├── libs.versions.toml                      # Version catalog
│   └── wrapper/                                # Gradle 9.1 Wrapper
├── build.gradle.kts                            # Root build configuration
├── settings.gradle.kts                         # Project settings
└── README.md                                   # Comprehensive documentation
```

---

## 📦 Direct APK Downloads

Pre-built APK binaries are available directly inside this repository:

| Build Type | File Path | File Size | Recommended For |
| :--- | :--- | :--- | :--- |
| **Signed Release APK** | [`apk/app-release.apk`](apk/app-release.apk) | **9.37 MB** | Regular users, production installation, sideloading |
| **Debug APK** | [`apk/app-debug.apk`](apk/app-debug.apk) | **13.8 MB** | Developers, debugging, log inspection |

---

## 📲 Installation & Running Guide

### Option 1: Install via ADB (USB Debugging)
1. Connect your Android device via USB and ensure **USB Debugging** is enabled in Developer Options.
2. Run:
```bash
# Verify device connection
adb devices

# Install Release APK
adb install -r apk/app-release.apk

# Launch the app
adb shell am start -n com.example.oraclepredictanything/.MainActivity
```

### Option 2: Sideload onto Android Phone
1. Download [`apk/app-release.apk`](apk/app-release.apk) directly to your phone.
2. Open your device's **Files** or **Downloads** app.
3. Tap `app-release.apk` and confirm installation (*Allow installation from unknown sources* if prompted).

---

## 🛠️ Building from Source

### Prerequisites
- **JDK 17 or JDK 21**
- **Android SDK Platform 35 / 36** with Build-Tools `36.0.0`
- Gradle 9.1 (included via Gradle Wrapper)

### Build Commands
```bash
# Clone the repository
git clone https://github.com/<your-username>/oracle-predict-anything.git
cd oracle-predict-anything/oracle

# Run all statistical engine unit tests
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug

# Assemble Signed Production Release APK
./gradlew assembleRelease
```
Compiled APKs will be output to `app/build/outputs/apk/`.

---

## 🧪 Unit Testing & Verification

Oracle includes a unit test suite verifying mathematical correctness:
- **`SMATest`**: Rolling window calculations, error accumulation, boundary conditions.
- **`HoltTest`**: Trend extrapolation, grid-searched optimal $(\alpha, \beta)$ parameters.
- **`LinearRegressionTest`**: Analytical OLS slopes, intercepts, and horizontal line handling.
- **`TechnicalIndicatorsTest`**: Wilder's RSI normalization $[0, 100]$, MACD histogram convergence, Bollinger $\pm 2\sigma$ envelopes.
- **`DataQualityTest`**: IQR and Z-score outlier detection thresholds on synthetic noisy datasets.
- **`BacktestUtilsTest`**: MAE, RMSE, MAPE formulas and confidence band width scaling.
- **`CategoryPredictorTest`**: Markov transition probability normalization ($\sum P = 1.0$) and half-life recency decay weighting.

To execute tests:
```bash
./gradlew testDebugUnitTest
```

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
