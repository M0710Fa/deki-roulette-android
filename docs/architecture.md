# デキレーレット Android 版 アーキテクチャ仕様書

## 概要

結果をユーザが事前に指定できる抽選アプリ「デキレーレット」の Android 版。iOS 版（隣のフォルダの `../deki-roulette-ios/`、SwiftUI）からの移植。

- 機能と見た目の仕様の正は iOS リポジトリの `docs/SPEC.md`。数値・配色は iOS の `Core/Config.swift` / `Design/Theme.swift` の値を使う
- コードの構成は iOS をなぞらず、Android 推奨アーキテクチャ（UI / Domain / Data 層、UDF、`StateFlow`）で組む。
  モジュールの切り方と設計規約は KomgaExplorer、ビルド構成（convention plugin）は Now in Android を参考にしている
- 3D 表示・端末の傾き追従・揺れは対象外

| 項目 | 内容 |
|---|---|
| UI | Jetpack Compose（Material3） |
| ツールチェーン | AGP 9.4.1（Kotlin 2.2.10 内蔵）/ Gradle 9.6.0 / JVM 17 |
| SDK | minSdk 30 / targetSdk 37 / compileSdk 37 |
| applicationId | `com.basekeita.roulette`（iOS の Bundle ID と同じ） |
| DI | Hilt |
| 画面遷移 | Navigation 3 |
| ローカル保存 | DataStore（Preferences） |

---

## 画面構成

ボトムナビゲーションで 2 画面を切り替える。項目リストや指定はタブ間で共有しない。

| タブ | 画面 | 説明 |
|---|---|---|
| ルーレット | `feature/roulette` | 項目から 1 つを選ぶ |
| 順番決め | `feature/order` | 項目をランダムに並べ替える |

タブは `NavigationSuiteScaffold` で、ウィンドウの大きさに応じて M3 の基準でナビゲーションバーとレールを切り替える。
両画面のヘッダの歯車から設定（`feature/settings`）、項目リストの見出しのメニューから保存したリストの管理（`feature/saved-lists`）を
ボトムシート（`ModalBottomSheet`）で開く。

### UI の作り方（M3）

iOS 版をコピーせず、Android のアプリとして作る。iOS 版から引き継ぐのは機能・仕様（SPEC、ステルスの規約）とデザイントークン（配色・書体のサイズ）だけで、
部品・操作感は Material 3 の標準に従う。標準部品を基本にし、プロダクトの個性が強い部分だけ自作する（判断の目安は iOS 版の作り。
iOS で標準部品なら M3 の標準部品、iOS で自作ならプロダクトの見た目として自作のまま。iOS が iOS 標準の操作を再現するために自作したものは、Android 標準の部品にする）。

- 標準部品: タブ（`NavigationSuiteScaffold`）、設定・保存したリストの管理（`ModalBottomSheet` と `TopAppBar`）、確認と名前の入力（`AlertDialog`）、
  メニュー（`IconButton` + `DropdownMenu`）、「すべて削除」（`TextButton`）、入力欄と追加（`OutlinedTextField` / `FilledTonalButton`）、
  設定の行（`ListItem` + `Switch` / `RadioButton`）、保存したリストの行（`ListItem`）、スワイプ削除（`SwipeToDismissBox`）、元に戻す（`Snackbar`）、
  画面の枠（`Scaffold`）、歯車（`OutlinedIconButton`）
- 自作（プロダクトの見た目）: 見出しとタグライン、スピン / シャッフルのボタン、結果のコピー・共有、フッターの折りたたみ、項目の行の見た目、盤面、結果の帯、順番の結果
- 押せる要素は 48×48dp 以上（自作の部品は `minimumInteractiveComponentSize()` か高さ 48dp）
- アイコンは Material Symbols（Outlined）の vector drawable。公式リポジトリ（google/material-design-icons の `symbols/android`）から取り、
  `android:tint="?attr/colorControlNormal"`（AppCompat の属性）を外して置く。色は `Icon` の tint で付ける
- 色は `DekiTheme` が Material の colorScheme をトークンから組むので、標準部品には色を個別に渡さない（Snackbar の inverse 系もトークンに結び付けてある）

---

## モジュール構成

```
build-logic/convention/   convention plugin（common / application / library / hilt / feature）
app/                      エントリポイント（@HiltAndroidApp, MainActivity）、2 タブ + NavDisplay の組み立て
feature/
  roulette/               ルーレット画面
  order/                  順番決め画面
  settings/               設定（ルーレットの詳細設定はシートの中のページ）
  saved-lists/            保存したリストの管理画面
ui/                       共通 UI（テーマ・トークン、atomic design の部品、UI 専用モデル（UiModel）、UI 用の純粋関数、触覚・音・キャプチャ検知）
navigation/               ルート定義（@Serializable の NavKey）
domain/                   UseCase と、UseCase が使う計算の関数
data/
  definition/             ドメインモデル、Repository の interface
  impl/                   Repository の実装、Hilt の @Binds
local/                    DataStore の LocalDataSource、Hilt の @Provides
```

`network` は持たない（通信しない仕様。SPEC §1.3）。

### モジュール依存関係

```
data:definition  ←  data:impl, domain, feature/*, app
domain           ←  feature/*, app
local            ←  data:impl
navigation       ←  feature/*, app
ui               ←  feature/*, app
data:impl        ←  app
feature/*        ←  app
```

- 依存は一方向のみ。逆方向・同層跨ぎの参照は禁止
- feature は `data:impl` / `local` / 他の feature に依存しない。Repository は `data:definition` の interface で注入され、実装の結び付けは `app` に集めた Hilt のグラフで行う
- domain は `data:definition` に依存する（Android 公式ガイドの層の向き。UseCase が Repository の interface を使う）
- feature 間の遷移は `XxxNavigationEvent` を `app` に返してつなぐ（遷移を要求しない feature は持たない）

### UseCase と Repository の使い分け

- 計算や複数の手順を含む処理は domain の UseCase を呼ぶ（スピンの決定、並べ替え、項目の追加・削除・元に戻す）
- 保存と読み込みだけなら、ViewModel から Repository を直接呼ぶ（項目の入れ替え・すべて削除・初期状態に戻す、保存したリスト、設定）

### convention plugin

| plugin | 中身 | 使うモジュール |
|---|---|---|
| `deki.common` | Kotlin のモジュールなら種類を問わず共通の設定（JVM 17、ktlint、JUnit） | domain、data:definition（JVM）が直接使う。他は library / application を通して受け取る |
| `deki.application` | common + `com.android.application`、SDK、applicationId、versionCode、Compose | app |
| `deki.library` | common + `com.android.library`、SDK、テストランナー | ui, data:impl, local, navigation |
| `deki.hilt` | Hilt プラグイン、KSP、`hilt-android`、`hilt-compiler` | app, data:impl, local, ui |
| `deki.feature` | library + hilt + Compose と、どの feature も持つ依存（`:ui` / `:navigation` / `:domain` / `:data:definition`、Compose、lifecycle、hilt-lifecycle-viewmodel-compose、navigation3、テスト用の coroutines-test / turbine） | feature × 4（各 `build.gradle.kts` は namespace だけ） |

SDK とバイトコードのバージョンは `build-logic/.../ProjectExtensions.kt` の `DekiBuildConfig` だけに置く。

### バージョンの制約

AGP 9.4.1 は Kotlin 2.2.10 を内蔵する。Kotlin 2.2 のコンパイラは 1 つ先（2.3）までのメタデータしか読めないため、
POM の `kotlin-stdlib` が 2.3 以上を要求するライブラリは入れない。

| ライブラリ | 採用 | 理由 |
|---|---|---|
| Hilt | 2.59.2 | 2.60 以上は kotlin-stdlib 2.3.21 を要求する |
| kotlinx-serialization | 1.9.0 | 1.10 以上は kotlin-stdlib 2.3 を要求する |
| KSP | 2.3.12 | AGP 9 内蔵 Kotlin + Hilt で生成できることを確認済み |

---

## 層の責務

```
UI 層（feature/*、ui）
  └── ViewModel（状態管理・UseCase / Repository の呼び出し）
  └── Screen（Composable、状態を表示するだけ）
      ↓
domain（UseCase と計算の関数）
      ↓
data:definition（モデル・Repository interface）  ←  data:impl（Repository 実装）
                                                         ↓
                                                  local（DataSource 層: DataStore）
```

### data:definition（JVM。依存は coroutines だけ）

- `model/`: `Item`、`RemovedItem`、`Mark`、`SpinDirection`、`SavedList`、`GlowStyle`、`UserSettings`、`ListKind`
  - ラベルとリスト名の正規化と上限はモデルに持たせる（`Item.normalizeLabel` / `MIN_COUNT` / `MAX_COUNT` / `MAX_LABEL_LENGTH`、
    `SavedList.normalizeName` / `MAX_COUNT` / `MAX_NAME_LENGTH`）。data:impl（読み込んだデータを整える）と domain・feature（追加）の両方が使うため
  - 文字数は見た目の 1 文字（書記素クラスタ）で数える（iOS の `String.prefix` と同じ）
- `repository/`（interface）
  - `ItemsRepository`: `items(kind): Flow<List<Item>>`、`save(kind, items)`、`reset(kind)`
  - `SavedListsRepository`: `lists: Flow<List<SavedList>>`、`save` / `rename` / `delete`（成否を返す）
  - `UserSettingsRepository`: `settings: Flow<UserSettings>`、各値の setter

### domain（JVM。依存は data:definition と `javax.inject` だけ）

パッケージは扱う処理ごとに分ける。

- `spin/`: `DecideSpinUseCase`（結果・止まる累積角・曲線・回転音と触覚の時刻を `SpinPlan` で返す）と、それが使う
  `RouletteMath`、`CubicBezierCurve`、`SpinTicks`、`FlickSpin`（角速度から周回数・向き・曲線を決める部分）、`SpinOutcome`、`SpinConfig`（スピンの数値）
- `order/`: `ShuffleOrderUseCase`（並べ替え・演出の時間・触覚の時刻を `OrderPlan` で返す）と、それが使う `Shuffler`、`RevealTiming`
- `haptic/`: `HapticSchedule`（スピンの境目は `SpinTicks` と同じ式、順番決めは行が現れる時刻）
- `item/`: `AddItemsUseCase`（追加できた件数を返す）、`RemoveItemUseCase`（`RemovedItem` を返す）、`RestoreItemUseCase`。2 画面で同じ処理
- 乱数は `kotlin.random.Random` をコンストラクタで受ける。既定の `SecureRandom().asKotlinRandom()` は `app` の Hilt モジュールで提供する。テストでは `Random(seed)`
- 時間は秒の `Double` で持つ（iOS の `TimeInterval` と同じ）

### local

- DataStore（Preferences、`deki_prefs`）。キーは iOS と同じ: `items.roulette` / `items.order` / `savedLists`（JSON）、`hapticsEnabled` / `soundEnabled`（Boolean）、`glowStyle`（String）
- `DekiLocalDataSource` と、`DataStore` を提供する `LocalModule`（`@Singleton`。ファイルが壊れていたら空から始める）
- `data:definition` に依存しない。項目リストのキーは local の `StoredItemsKey` で受け、`ListKind` との対応は data:impl に置く
- JSON の形は `StoredItem`（`{id,label}`）/ `StoredSavedList`（`{id,name,items}`）。形が違う・id が UUID として読めない JSON は全体を読めないものとする（iOS の `JSONDecoder` と同じ）
- 値が無い・読めないときは Flow に `null` を流す。初期項目や既定値は data:impl で補う
- 保存したリストの読み書きは `updateSavedLists(transform)` で 1 回の `edit` の中で行う（追加・名前の変更・削除の途中に別の書き込みが割り込まないように）

### data:impl

- Repository の実装と `DataModule`（`@Binds`）。読み込んだデータを整える処理（正規化・空と重複 id の除去・上限で切る）はここ
- 保存したリストの追加・名前の変更・削除（iOS の `SavedList` の `adding` / `renaming` / `deleting`）は `SavedListsRepository` の実装の中
- 保存データが無い・読めないときの初期項目は `data:impl` の `res` の `string-array`（日英）から、そのときの言語で読む。
  読む部分は `DefaultItemLabels`（interface）に分け、Repository のテストでは Context を使わない
- 初期項目の id はラベルが変わるまで（言語の変更まで）使い回す。読み直すたびに振り直すと、表示中の行の id と UseCase が読み直した項目の id が食い違うため
- 光彩の色の保存値は iOS の `GlowStyle.rawValue` と同じ小文字（`white` / `slice` / `rainbow`）。知らない値は既定の色に戻す
- **指定（当たり・先頭・末尾）は Repository に渡さない**（保存しない仕様）

### ui（共通 UI）

- `theme/`: `DekiDesignTokens`（外観で変わる色は `DekiColorScheme` のライト / ダークの 2 値、盤面・開始ボタン・削除ボタン・結果の帯など変わらない色は `DekiFixedColors`）、
  `DekiTheme`（Material の部品の色もアプリのトークンで上書きする）、`DekiTypography`（iOS のテキストスタイルの sp）、`DekiLayout`、
  `DekiMotion`（アニメーション。iOS の `spring(duration:bounce:)` は減衰比 `1 - bounce`・硬さ `(2π / duration)²` に置き換える）、`DekiStopEmphasis`、
  `sliceColor(at,count)` / `DekiColorScheme.sliceAccent(at,count)`、`SlicePalette`
- `component/`（atomic design）: `atoms/`・`molecules/`・`organisms/`・`templates/`
  - `ItemList`（organisms）は表示と一時的な状態（入力中の文字、押している行の数、指定直後の表示、ダイアログ、
    元に戻すの対象）だけを持つ。項目・指定・保存したリストは画面から受け取り、操作はコールバックで返す
  - 入力は改行で行に分けて正規化し（`ItemLabelInput.splitLines`）、まとめて `onAdd` に渡す。同名があれば追加の前に確かめる（追加は止めない）
  - 削除した直後は「元に戻す」の `Snackbar` を 5 秒（ユーザー補助の推奨時間に合わせて延ばす）出し、押されたら `onUndoRemove(id)` を返す。
    Snackbar は画面の `PageFrame`（`Scaffold`）が下端に置き、`SnackbarHostState` を画面から `PageFrame` と `ItemList` に渡す。
    削除した項目と位置は ViewModel が覚えている（直前の 1 件だけ。演出の開始・すべて削除・読み込みで取り消せなくなる）
  - 長押しは `ItemRow` の自前の `pointerInput`（600ms、10dp 以内、親の縦スクロールに取られたら不成立）
  - スワイプ削除は `SwipeToDeleteBox`（molecules。M3 の `SwipeToDismissBox` を左方向だけで使う）。項目リストの行と保存したリストの管理の行で共有する。
    長押しが成立した操作ではスワイプを受けない
  - `ResultActions`（molecules）: 結果の下の「結果をコピー」と「共有」。本文は `ResultText` で組み立て、コピー・共有そのものは画面のコールバックで ViewModel に渡す。
    見た目のカプセルは小さいまま、`minimumInteractiveComponentSize()` で押せる範囲を 48dp 取る（領域の高さも 48dp）
- `uimodel/`: 両画面で使う UI 専用モデル（`ItemUiModel`、`MarkUiModel`、`SavedListSummaryUiModel`、`ItemListLimits`）。
  ui は `data:definition` に依存しないので、件数や文字数の上限は `ItemListLimits` で画面から渡す
- `rule/`: UI 用の純粋関数（`WheelDrag`、`WheelLabel`、`ResultBand`、`MarkVisibility`、`TypeLayout`、`ItemLabelInput`）
  - `TypeLayout`: `fontScale >= 1.5` をアクセシビリティサイズとし、項目リストと順番の結果行のラベルを 2 行まで許す・順番決めのコピーボタンの固定高さを最小高さに緩める
- `modifier/`: `revealOnAppear`（結果が現れる演出。`Modifier.Node` で作り、出し直すときは `key` で作り直す）
- `component/templates/PageFrame`: 両画面の枠（`Scaffold` の中に、ヘッダ・本文・フッターの折りたたみ 3 つ。演出開始で「使い方」だけ閉じる。下端に Snackbar）
- `component/templates/AdaptiveStack`: 両画面の本文の並べ方。ウィンドウの幅が 600dp 以上（`isExpandedWidth()`、`LocalWindowInfo.containerDpSize`）なら
  盤面（結果）と項目リストを横に並べ、上下は中央でそろえる。項目リストの幅は 2 列の均等割りと 360dp の小さいほうで、残りを盤面側に渡す
  （iOS の `HStack` と同じ配分）。それ以外は縦に積む
- `component/templates/SheetFrame`: ボトムシートで出す画面（設定・詳細設定・保存したリストの管理）の枠。見出しの `TopAppBar` と中身。
  シートは下げる・外側を押す・戻る操作で閉じるので「閉じる」ボタンは置かない。シートの中で次のページに進んでいるときは戻るボタンを出す
- 両画面の項目リストと結果で使う純粋関数: `DuplicateLabels`（同名の判定）、入力欄の整形（iOS の `ItemLabel.splitLines` / `clampLines`）、`ResultText`（コピー・共有の本文）
- `feedback/`: `HapticPlayer`、`SpinSoundPlayer`（波形を焼く `ClickTrack`、素材を読む `WavDecoder` を含む）、`ScreenCaptureMonitor`、`ReduceMotionMonitor`、
  `ResultSharer`（interface と実装、Hilt の `FeedbackModule`）と、触覚の刻みを順に呼ぶ `launchTicks`
  - 触覚（`View.performHapticFeedback`）と画面録画の検知（`WindowManager`）は画面に結び付くので、`MainActivity` が `FeedbackHost.attach(this)` で画面を渡す。
    `FeedbackHost` は Activity を弱参照で持ち、画面が見えている間（`onStart`〜`onStop`）だけ録画の検知を登録する
  - 回転音の素材は `ui` の `res/raw/click.wav`（iOS と同じファイル）

### feature/*

```
feature/<name>/src/main/kotlin/com/basekeita/roulette/feature/<name>/
  <Name>Screen.kt              Route（hiltViewModel を持つ唯一の Composable）と UI
  <Name>ViewModel.kt           @HiltViewModel。ViewState / UiState をこのファイル内に定義
  <Name>Navigation.kt          EntryProviderScope<NavKey>.<name>Entry(onNavigationEvent)
  <Name>NavigationEvent.kt     sealed interface
  uimodel/                     この画面専用の UiModel
  ui/molecules|organisms/      この画面専用の部品
```

- ViewState（`Loading` / `Error` / `Success(uiState)`）と UiState を分ける。UiState には UI 専用モデルだけを置く
- ViewModel は UseCase と Repository を呼ぶ（使い分けは「UseCase と Repository の使い分け」）。Repository の `Flow` と ViewModel 内の一時状態を `combine` し、UiModel に変換して `stateIn(WhileSubscribed(5_000))` で公開する
- 触覚・音・コピー・共有など一度だけの出来事は、ViewModel が `ui/feedback` の interface を呼ぶ。設定（触覚・効果音）が OFF なら呼ばない
- 読み上げは ViewModel からは呼ばず、新しく現れる要素に `paneTitle` を付けて UI で伝える（結果の帯「結果は〇〇です」、
  まとめて追加で上限を超えたときの注記。元に戻すは M3 の Snackbar が自分で伝える）。`View.announceForAccessibility` は API 36 で非推奨（読み上げサービスが無視してよいイベント）のため使わない。
  Compose は `paneTitle` を持つノードが新しく現れると `CONTENT_CHANGE_TYPE_PANE_APPEARED` を送る。画面に見えていないノード（サイズ 0・画面外）は
  読み上げの木から外れるので、見えない live region のノードでは伝わらない
- 「アニメーションを削除」と画面録画の状態は、ViewModel が `ReduceMotionMonitor` / `ScreenCaptureMonitor` を `combine` して UiState に出す
- 結果（ルーレットの止まったスライス・順番決めの並び）は、出したときの項目と今の項目が違えば出さない。設定の「項目を初期状態に戻す」は
  別の画面の ViewModel が Repository を直接戻すので、各画面は Repository から届く項目の変化で結果を消す。指定は、その項目が消えれば印も出さない。
  一度も編集していない状態で戻したときは項目（id を含む）が変わらないので、結果と指定は残る（iOS は消える）
- 保存したリストを読み込んだときは、id が同じ項目でも指定と結果を消す（SPEC §4.6）
- ボトムシートは閉じる操作を M3 の `ModalBottomSheet` が受け、app がバックスタックから外す（feature は閉じる要求を返さない）。
  ルーレットの詳細設定は Navigation 3 のルートにせず、設定のシートの中のページとして切り替える（シートを重ねない。戻る操作で設定に戻る）。
  Navigation 3 は重ねて出す Scene を `key` で管理し、同じ `key` の新しい Scene では中身を差し替えないため、ルートを分けると 1 枚のシートの中で入れ替えられない
- 子 Composable には意味のあるコールバックを個別に渡す（`onIntent` / `viewModel` を渡さない）

### app

- `DekiApplication`（`@HiltAndroidApp`）、`MainActivity`（`@AndroidEntryPoint`）
- UseCase が使う乱数（`kotlin.random.Random`）を `SecureRandom().asKotlinRandom()` で提供する Hilt モジュール
- 2 タブの `NavigationSuiteScaffold`（`material3-adaptive-navigation-suite`。ウィンドウの大きさで M3 の基準どおりにバーとレールを切り替える）と `NavDisplay`。
  タブごとに `NavBackStack` と ViewModel の状態を残す
  - タブごとに `rememberDecoratedNavEntries`（`SaveableStateHolder` と `ViewModelStore` の decorator）でエントリを作り、両タブとも毎回組み立てる
  - 各 feature の遷移の要求は、そのタブのバックスタックに積む
  - 設定と保存したリストの管理は `BottomSheetSceneStrategy`（app。Navigation 3 の `OverlayScene` で `ModalBottomSheet` を出す。
    `DialogSceneStrategy` と公式サンプル `AnimatedBottomSheetSample` と同じ作り）のボトムシートとして出す。シートにするかは app が決め、
    `xxxEntry(metadata = BottomSheetSceneStrategy.bottomSheet())` で feature に渡す（feature は navigation3-ui に依存しない）
- 縦向きの固定は `smallestScreenWidthDp < 600` の端末だけ（`MainActivity` の `onCreate` で毎回 `requestedOrientation` を指定する）
- 起動画面は `core-splashscreen`（`Theme.DekiRoulette.Starting`）。背景はライト `#FAF7F2` / ダーク `#17111F`（`values` / `values-night` の `splash_background`）。
  アイコンの既定はライブラリの汎用アイコンなので、`windowSplashScreenAnimatedIcon` にアプリのアイコンを指定する
- アイコンはアダプティブアイコン。前景（`mipmap-*/ic_launcher_foreground.png`）は `scripts/make_icon.py` で iOS 版の `AppIcon.png` から作り、
  針を含む絵の広がりをセーフゾーン（直径 66dp）に収める。背景は `#17111F`。モノクロ層は持たない
- アプリ別の言語設定は `androidResources.generateLocaleConfig`（既定の言語は `res/resources.properties` の `unqualifiedResLocale=ja`）

---

## プラットフォーム API の置き換え（iOS → Android）

| 機能 | Android の実装 |
|---|---|
| スピンの回転 | `Animatable.animateTo(tween(4500, CubicBezierEasing))`。`MotionDurationScale(1f)` でアニメーション倍率を無視し、音・触覚とずれないようにする |
| reduceMotion | 「アニメーションを削除」（`ANIMATOR_DURATION_SCALE == 0`） |
| フリック・ドラッグ | `pointerInput` + `awaitEachGesture`。タッチスロップを越えたら消費して、盤面の中で始まったドラッグを親の縦スクロールより優先する。位置は dp で `WheelDrag` / `FlickVelocity`（feature/roulette）に渡す |
| 長押しで指定 | 自前の `pointerInput`（600ms、10dp 以内）。`combinedClickable` / `semantics { onLongClick }` は使わない |
| スワイプ削除 | M3 の `SwipeToDismissBox`（左方向だけ。スワイプしきると削除し、Snackbar で元に戻せる） |
| 触覚 | `View.performHapticFeedback`（selection = `CLOCK_TICK`、light = `KEYBOARD_TAP`、medium = `VIRTUAL_KEY`、success = `CONFIRM`） |
| 回転音 | 合成した波形は `AudioTrack`（`MODE_STATIC`、`PCM_FLOAT`）、1 回ずつの音は `SoundPool`。着信音モードが通常でなければ鳴らさない。オーディオフォーカスは取らない |
| 画面キャプチャ | API 35 以上は `WindowManager.addScreenRecordingCallback`（`DETECT_SCREEN_RECORDING`）。API 30〜34 は検知しない。`FLAG_SECURE` は使わない |
| 印の状態の読み上げ | `semantics { selected; stateDescription }`（伏せている間は付けない） |
| 結果の読み上げ | 新しく現れる要素の `semantics { paneTitle }`（`announceForAccessibility` は API 36 で非推奨）。削除の取り消しは Snackbar が伝える |
| タブ | `NavigationSuiteScaffold`（バー / レール） |
| シート | `ModalBottomSheet`（`BottomSheetSceneStrategy`） |
| アイコン | Material Symbols（Outlined） |
| コピー・共有 | `ClipboardManager` / `Intent.ACTION_SEND` |
| 横並び | ウィンドウの幅が 600dp 以上。盤面は最大 480dp、項目リスト 360dp、ページ幅 928dp |
| 文字サイズ | `sp` で指定。iOS の `@ScaledMetric` は `dp × fontScale`。アクセシビリティサイズは `fontScale >= 1.5`。盤面と結果の帯は追従させない |
| 文言 | `strings.xml`（日本語が既定）と `values-en`。アプリ別の言語設定は `generateLocaleConfig` |
| 書体 | システムのフォント |

---

## 実装フェーズ

| フェーズ | 内容 | 状態 |
|---|---|---|
| P0 | build-logic、モジュールの骨組み、パッケージ変更、Hilt / KSP / ktlint、CLAUDE.md、本書 | ✅ 完了 |
| P1 | `data` を `definition` / `impl` に分割。`data:definition`（model、repository interface）、`domain`（UseCase と計算の関数）とテスト | ✅ 完了 |
| P2 | `local`、`data:impl` とテスト | ✅ 完了 |
| P3 | `ui` の theme と部品の基本、`navigation`、`app` の 2 タブと NavDisplay | ✅ 完了 |
| P4 | `feature/roulette`（盤面、ボタンでのスピン、強調、帯、ViewModel とテスト）、`PageFrame` | ✅ 完了 |
| P5 | `feature/order`、`feature/settings`、`feature/saved-lists`、項目リストの保存・読み込みの接続、`SheetFrame` | ✅ 完了 |
| P6 | `ui/feedback` の組み込み、フリック、スワイプ削除、元に戻す、同名の警告、一括追加 | ✅ 完了 |
| P7 | アイコン、スプラッシュ、横並び、縦向き固定、文字サイズ、アプリ別の言語設定、`docs/SPEC.md`、全体の確認 | 実装済み。計画書 §9 のエミュレータ確認は回転音のずれ（音を聞いての確認）を除いて済み |
| P8 | M3 への見直し（標準部品・Material Symbols・ボトムシート・48dp・ナビゲーションの切り替え） | 実装済み。計画書 §9 のエミュレータ確認は回転音のずれ（音を聞いての確認）を除いて済み |

---

## テスト

- Fake の Repository は共通のモジュールに置かず、使うテストの中に個別に書く（KomgaExplorer と同じ）
- `local` / `data:impl` のテストは一時ファイルの DataStore で確かめる。JVM のテストでは `Build.VERSION.SDK_INT` が 0 になり、
  ファイルを渡す `PreferenceDataStoreFactory.create` は `File.renameTo` で書き換えるため、Windows では 2 回目の書き込みが失敗する。
  テストでは `OkioStorage(FileSystem.SYSTEM, PreferencesSerializer)` を `create(storage = ...)` に渡す
