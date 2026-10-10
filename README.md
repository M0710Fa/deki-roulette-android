# deki-roulette-android

[デキレーレット](https://roulette.basekeita.com/) の Android アプリ。
結果をユーザが事前に指定できる抽選アプリで、ルーレットと順番決めの 2 画面をタブで切り替える。

機能仕様の正は iOS 版（[shilokuma-inc/deki-roulette-ios](https://github.com/shilokuma-inc/deki-roulette-ios)）の `docs/SPEC.md`。
Android 版での差分は [`docs/SPEC.md`](docs/SPEC.md)、設計は [`docs/architecture.md`](docs/architecture.md) にまとめてある。
Web 版（React + Vite）のコードは ff19748 以前の履歴にある。

## 環境

- Android Studio（JDK は同梱のものを使う）
- AGP 9.4.1（Kotlin 2.2.10 内蔵）/ Gradle 9.6.0 / Compose BOM 2026.02.01
- minSdk 30 / targetSdk 37 / compileSdk 37
- Gradle デーモンは `gradle/gradle-daemon-jvm.properties` の toolchain（25）で動く

## セットアップ

Android Studio でリポジトリのルートを開く。コマンドラインでのビルド・テスト・静的チェック:

```shell
.\gradlew.bat assembleDebug
```

```shell
.\gradlew.bat test
```

```shell
.\gradlew.bat ktlintCheck lintDebug
```

アプリアイコンは iOS 版の `AppIcon.png` から `scripts/make_icon.py`（Pillow）で書き出している。作り直すとき（リポジトリのルートで）:

```shell
python scripts/make_icon.py ../deki-roulette-ios/DekiRoulette/Resources/Assets.xcassets/AppIcon.appiconset/AppIcon.png app/src/main/res
```

## 構成

```
build-logic/convention/  convention plugin（deki.common / application / library / hilt / feature）
app/                     エントリポイント（DekiApplication, MainActivity）、2 タブと NavDisplay
feature/
  roulette/              ルーレット画面
  order/                 順番決め画面
  settings/              設定（ボトムシート）
  saved-lists/           保存したリストの管理（ボトムシート）
ui/                      共通 UI（テーマ、部品、UI 用の純粋関数、触覚・音・画面キャプチャの検知）
navigation/              ルート定義（NavKey）
domain/                  UseCase と計算の関数
data/
  definition/            ドメインモデル、Repository の interface
  impl/                  Repository の実装
local/                   DataStore
```

## ライセンス

MIT
