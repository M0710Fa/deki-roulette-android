# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 言語

ユーザーへの応答は常に日本語で行うこと。

## このリポジトリ

**デキレーレット**（結果をユーザが事前に指定できる抽選アプリ）の Android 版。iOS 版（隣のフォルダの `../deki-roulette-ios/`）からの移植で、
**機能と見た目の仕様の正は iOS リポジトリの `docs/SPEC.md`**。Android 固有の決定と SPEC との差分は `docs/SPEC.md`（このリポジトリ）と
`docs/architecture.md` に書く。3D 表示・端末の傾き追従・揺れは対象外。

## Build Commands

```shell
# ビルド
.\gradlew.bat assembleDebug

# テスト（全モジュール）
.\gradlew.bat test

# 1 クラスだけ
.\gradlew.bat :domain:test --tests "com.basekeita.roulette.domain.spin.RouletteMathTest"

# 静的チェック
.\gradlew.bat ktlintCheck lintDebug
```

JDK は Android Studio 同梱のもの（`JAVA_HOME` に `C:\Program Files\Android\Android Studio1\jbr`）で起動する。Gradle デーモンは
`gradle/gradle-daemon-jvm.properties` の toolchain（25）で動く。

## 行動規約

- **タスク開始前に必ず `CLAUDE.md` と `docs/architecture.md` を読むこと**
- **ユーザーから質問された場合は、まず質問に答えること。** 回答によってその後の行動が変わる可能性があるため、作業が必要かを確認してから動く
- **推論ベースで実装・変更・削除を行うことを禁止する。** コード・ドキュメントを変更する前に該当ファイルを読んで現状を確認する。不明な点はユーザーに確認する
- **着手前に変更規模を見積もり、完遂できないと判断したら着手しない。** 完遂できる単位にスコープを縮小してから着手する
- **指示の言葉を自分の技術的解釈に置き換えて実行してはいけない。** 解釈を加えていると気づいたら、実装に入らず指示の意味を確認する
- **変数名・関数名は意味のある名前を付けること。** 1 文字変数で名前付けを回避することを禁止する
- **新しい定義を作る前に、同等のものがプロジェクト内に既に存在しないか Grep で検索すること**
- iOS 版の挙動を移すときは、iOS 側の該当ファイルを読んでから実装する（`../deki-roulette-ios/DekiRoulette/`）。仕様の数値（スピンの時間・件数の上限など）と配色は iOS の `Core/Config.swift` / `Design/Theme.swift` の値を使う。
  iOS の部品の寸法・形（ボタンの形、シートの枠など）は写さず、M3 の部品を使う

## 依存追加の規約

新しいライブラリは必ず `gradle/libs.versions.toml` にバージョンとエントリを追加し、`build.gradle.kts` では `libs.*` 経由で参照する。

- **Kotlin は AGP 9.4.1 内蔵の 2.2.10。** Kotlin 2.2 のコンパイラが読めないライブラリ（POM の `kotlin-stdlib` が 2.4 以上）は入れない。
  追加・更新の前に POM の `kotlin-stdlib` を確認する（例: kotlinx-serialization は 1.10 以上が 2.3 を要求するため 1.9.0、Hilt は 2.60 以上が 2.3 を要求するため 2.59.2）
- ショートハンドを使うときは、Gradle キャッシュや公式ドキュメントで実在を確認する
- 新しい API を使う際は `@Experimental` の有無を確認し、必要な `@OptIn` を付ける
- Deprecated の警告が出たら、置き換え先を推測しない。ソース（Gradle キャッシュ）で `@Deprecated` の `replaceWith` を読んでから直す

## 設計規約

> 詳細は `docs/architecture.md`。

### モジュールと依存方向

依存は一方向のみ。逆方向・同層跨ぎの参照は禁止。

```
data:definition  ←  data:impl, domain, feature/*, app
domain           ←  feature/*, app
local            ←  data:impl
navigation       ←  feature/*, app
ui               ←  feature/*, app
data:impl        ←  app
feature/*        ←  app
```

- `data:definition` はドメインモデルと Repository の interface、`data:impl` は Repository の実装、`domain` は UseCase と計算の関数
- feature は `data:impl` / `local` / 他の feature に依存しない。Repository は `data:definition` の interface で注入し、実装は Hilt で `app` に集める
- 計算や複数の手順を含む処理は UseCase を呼ぶ。保存と読み込みだけなら ViewModel から Repository を直接呼ぶ
- feature 間の遷移は `XxxNavigationEvent` を `app` に返してつなぐ
- `build.gradle.kts` は convention plugin（`build-logic`）を使う: `deki.common` / `deki.application` / `deki.library` / `deki.hilt` / `deki.feature`。
  1 回しか使わない組み合わせは plugin にしない

### Material 3 に従う

- **iOS 版をコピーするのではなく、Android のアプリとして作る。** iOS 版から引き継ぐのは機能・仕様（SPEC、ステルスの規約を含む）とデザイントークン（配色など）だけ。
  部品・レイアウト・操作感は M3 の標準に従い、iOS の部品の見た目や挙動に寄せるために自作の部品を作らない
- UI は Material 3 のガイドラインと標準コンポーネントに従う。計画書・iOS 版に書かれていない振る舞いでも、M3 が決めているものは省かない
- 押せる要素は 48×48dp 以上を確保する（M3 の部品を使うか、`minimumInteractiveComponentSize()`）
- トップレベルのナビゲーションは `NavigationSuiteScaffold`（`material3-adaptive-navigation-suite`）で、ウィンドウの大きさに応じてナビゲーションバー / レールを M3 の基準で切り替える。`NavigationBar` を固定で置かない

### 禁止事項

- **DataSource（local）は Repository を参照してはいけない**
- **Repository は他の Repository に依存してはいけない**
- **ドメインモデルを UI 層（ViewModel の公開状態・Screen）で直接使ってはいけない**（UI 専用モデル `XxxUiModel` を定義してマッピングする）
- **UiState に isLoading や errorMessage を混在させてはいけない**（ViewState / UiState 分離パターンを使う）
- **当たり・先頭・末尾の指定を Repository に渡したり保存したりしてはいけない**（ViewModel の中だけで持つ。SPEC §12）
- 下位層に値が必要な場合は、上位層からメソッド引数として渡す
- コードの配置はモジュールの責務に従う。一箇所でしか使われないコードを利便性を理由に共有モジュールに置かない

### ViewState / UiState の分離（全 ViewModel で必須）

```kotlin
sealed interface ViewState {
    data object Loading : ViewState
    data class Error(val message: String) : ViewState
    data class Success(val uiState: UiState) : ViewState
}

data class UiState(/* UI 専用モデルのみ */)
```

- `Loading` は単純に「ローディング中」。呼び出し経緯（初回・再ロード）で振る舞いを変えない
- UiState / ViewState で管理できる状態を別の StateFlow で新設しない
- `StateFlow` を `init` で collect して再ロードするときは、初期値の発火を `.drop(1)` で遮断し、二重起動を防ぐテストを書く

### ViewModel の onIntent を Composable にバケツリレーしない（厳守）

- 子 Composable のパラメータに `onIntent: (XxxIntent) -> Unit` や `viewModel: XxxViewModel` を持たせてはいけない
- ViewModel を保持する Composable は最上位の 1 つ（`XxxScreen` の Route）だけ。子には `onSpinClick: () -> Unit` のような意味のあるコールバックを個別に渡す

### 条件分岐のコーディング規約

- **`when { bool }` 形式の条件分岐を禁止する。** `if / else if / else` に `{}` を付けて書く
- **`else` 節がない単行ガード `if (cond) return` は許可する**
- **`else` 節がある `if/else` は必ず `{}` と改行で展開する。** `when` アームの中でも同様
- **ガード節を複数並べるとき、必ず空行で区切る**

## ステルス前提の UI（仕様であり、削ったり戻したりしない）

- 指定は行の長押し（600ms）のみ。専用ボタン・メニュー・ユーザー補助のアクションは置かない。
  長押しは自前の `pointerInput` で受け、**`combinedClickable` や `semantics { onLongClick }` は使わない**（TalkBack のアクションとして見えてしまう）
- 印は色見本をリングに変えるだけ（末尾は中心に点）。行を押している間と指定直後 1.6 秒しか出さず、演出中・結果表示中・画面キャプチャ中は伏せる。
  伏せている行には `selected` も `stateDescription` も付けない
- 隠し操作の説明はフッターの「使い方」内にのみ置き、演出開始で自動的に閉じる
- 画面本文に「当たり」「必ず」等の語を置かない。コピー・共有の本文に指定の有無を示す情報を入れない

## スピンの仕組み（Android 版の要点）

- 結果と止まる累積角は開始時に `DecideSpinUseCase` が決める（`RouletteMath.nextRotation`）。回転音・触覚の時刻も同じ曲線（`CubicBezierCurve`）から先に求める
- 回転は Screen の `Animatable` が `tween(4500, CubicBezierEasing)` で行う。開発者オプションのアニメーション倍率で長さが変わると音・触覚とずれるため、
  `MotionDurationScale(1f)` の下で回す。完了の保険（5.1 秒）は ViewModel が持つ
- reduceMotion は「アニメーションを削除」（`ANIMATOR_DURATION_SCALE == 0`）で判定し、回さずに 0.32 秒後に完了扱いにする
