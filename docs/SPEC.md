# デキレーレット Android 版 仕様

機能と見た目の仕様の正は iOS 版の [`docs/SPEC.md`](../../deki-roulette-ios/docs/SPEC.md)（Web 版の仕様と、§12「iOS 版との対応」）。
Android 版は §12 の iOS 版の列の機能・仕様に従う。部品と操作感は iOS をコピーせず Material 3 の標準に従う（`architecture.md` の「UI の作り方（M3）」）。
本書には、iOS 版との差分と、Android での読み替えだけを書く。
実装の構成は [`architecture.md`](architecture.md)。

---

## 1. iOS 版との差分

| 項目 | iOS 版 | Android 版 | SPEC |
|---|---|---|---|
| 3D 表示・傾き追従・揺れ | 設定の「3D 表示」「傾きの基準」で使える | 持たない。盤面は常に平面。設定にも項目を置かない | §10、§12 |
| 画面キャプチャ中に印を伏せる | 常に検知する（`UIScreen.isCaptured`） | API 35 以上だけ検知する（`WindowManager.addScreenRecordingCallback`）。API 30〜34 はキャプチャされていない扱い。`FLAG_SECURE` は使わない（人に画面を見せる使い方なのに、画面全体が黒くなるため） | §6.3、§12 |
| 回転音を鳴らさない条件 | 消音スイッチ | 着信音モードが「通常」でないとき（マナーモード・サイレント） | §12 |
| 書体 | アプリ全体に `.fontDesign(.rounded)` | システムの既定の書体（iOS の rounded が効くのは欧文だけで、和文は通常のゴシックのため） | §9.2 |
| タッチ領域の最小 | 44×44pt | 48×48dp（Android の推奨値） | §10 |
| reduceMotion の判定 | 「視差効果を減らす」（`accessibilityReduceMotion`） | 開発者向けオプション・ユーザー補助の「アニメーションを削除」（`ANIMATOR_DURATION_SCALE == 0`）。挙動（0.32 秒で完了、連続的な触覚を鳴らさない）は同じ | §10 |
| 結果などの読み上げ | `AccessibilityNotification.Announcement` | 新しく現れる要素の `paneTitle`（結果の帯「結果は〇〇です」、まとめて追加で上限を超えたときの注記）。元に戻すは M3 の Snackbar が伝える。`announceForAccessibility` は API 36 で非推奨のため使わない | §10、§12 |
| 設定の「項目を初期状態に戻す」で結果と指定が消える条件 | 戻すと必ず消える | 項目が変わったときだけ消える。一度も編集していない項目を戻したときは、項目（id を含む）が変わらないので、結果と指定は残る | §12 |
| 起動画面 | 背景色だけ（ロゴやアプリ名は置かない） | 背景色（ライト `#FAF7F2` / ダーク `#17111F`）の中央にアプリのアイコンを出す。Android 12 以降は起動画面にアイコンが必ず出るため | §12 |
| タブ | `TabView` | `NavigationSuiteScaffold`。電話ではナビゲーションバー、ウィンドウが広いとき（タブレットなど）はナビゲーションレール（M3 の基準） | §12 |
| 設定・保存したリストの管理 | sheet。ナビゲーションバーに「閉じる」 | ボトムシート（`ModalBottomSheet`）。下げる・外側を押す・戻る操作で閉じ、「閉じる」ボタンは置かない。ルーレットの詳細設定はシートの中のページで、戻るボタンか戻る操作で設定に戻る | §12 |
| スワイプ削除 | 行を左にスワイプすると削除ボタンが現れて止まり、押すか大きくスワイプすると削除 | M3 の `SwipeToDismissBox`。左にスワイプしきると削除する（ボタンを出して止める段階は持たない） | §4.3 |
| 削除の取り消し | 項目リストの下に「元に戻す」の帯を 5 秒 | M3 の Snackbar（アクション「元に戻す」）を画面の下端に 5 秒。ユーザー補助の設定で操作の時間を長くしていれば、その推奨時間まで延ばす | §4.3 |
| 設定の項目 | 枠で囲んだ自作の行 | M3 の `ListItem`（`Switch` / `RadioButton`）。光彩の色はチェックではなくラジオボタンで選ぶ | §12 |
| アイコン | SF Symbols | Material Symbols（Outlined）。メニューは縦の三点（more_vert）、折りたたみは下向きの矢印（expand_more） | — |
| 結果のコピー・共有の領域 | 高さ 32pt | 押せる範囲を 48dp 取るため、領域の高さも 48dp | §7.3 |
| アプリのアイコン | `AppIcon.png`（1024×1024） | 同じ絵をアダプティブアイコンの前景にする（`scripts/make_icon.py`）。カラーの盤面は単色にすると円になるだけなので、テーマアイコン（モノクロ層）は持たない | — |

## 2. Android での読み替え

| 項目 | iOS 版 | Android 版 |
|---|---|---|
| 横並びのレイアウト | `horizontalSizeClass == .regular` | ウィンドウの幅が 600dp 以上（`LocalWindowInfo.containerDpSize`）。分割画面で狭くなれば縦積みに戻る。寸法（盤面 480dp、項目リスト 360dp、ページ幅 928dp）は同じ |
| 画面の向き | iPhone は縦向きだけ、iPad は全方向 | 最小幅（`smallestScreenWidthDp`）が 600dp 未満の端末だけ縦向きに固定し、それ以外は固定しない |
| 文字サイズへの追従 | Dynamic Type。`@ScaledMetric` で寸法を伸ばす | 文字は `sp` で指定する。`@ScaledMetric` の寸法は `dp × fontScale` |
| アクセシビリティサイズ | `accessibility1` 以上 | `fontScale >= 1.5`（`TypeLayout`）。項目リストと順番の結果行のラベルを 2 行まで許し、順番決めのコピーボタンの固定高さを最小高さに緩める |
| 文字サイズに追従させないもの | 盤面と結果の帯 | 同じ（`dp` を `fontScale` で割り戻して `sp` にする） |
| アプリごとの言語 | 設定アプリの「アプリごとの言語」 | 設定アプリの「アプリの言語」（`generateLocaleConfig`。既定は日本語、ほかに英語） |
| 長押しで指定 | `onLongPressGesture(minimumDuration: 0.6)` | 自前の `pointerInput`（600ms、10dp 以内）。TalkBack のアクションとして見えないよう、`combinedClickable` / `semantics { onLongClick }` は使わない |
| 画面キャプチャの判定 | `UIScreen.isCaptured` | 画面録画（`DETECT_SCREEN_RECORDING`）。スクリーンショットは対象外 |
