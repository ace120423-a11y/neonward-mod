> 運用先更新（2026-09-15）：正本ワールドは `C:/Users/ace12/Documents/NeonWardServer/world`。現在はこのPCの専用サーバー方式。詳細は `work/active-server.json`。旧シングルプレイの `NeonWard-Rebuilt` は退避コピーであり、今後の地形更新で上書き元にしない。サーバー実行中はMOD・地形を書き換えない。

# Neon Ward: Street Tech 0.1

Minecraft Java 26.2 / Fabric Loader 0.19.5 / Fabric API 0.160.0+26.2 / Java 25。

スマホは接続時にプレイヤーの専用枠へ自動装備。通常のインベントリ36枠とホットバー9枠を消費しません。アイテム本体のデータをプレイヤーへ保存し、死亡後も引き継ぎます。専用枠は読み取り専用で、ドラッグ・投げ捨て・取り外しの対象になりません。Pまたはインベントリ横のボタンで取り出す／しまう操作。手に持って右クリックすると開きます。

ホーム画面は12アプリ。地図は街の案内図と現在地、カメラはneonward/photosへ画像を保存する実装です（撮影・カメラロールは実機未確認）。PULSE（パルス）は街内SNSとして表示しています。友達・実際の音声通話・トーク・SNS投稿・録音・送金・土地管理・クエスト・称号の機能は未実装で「準備中」です。

`/neon kit` でバイクと車のキー。道路に向かって使用すると配置。車体を右クリックして乗り、W/Sで加速・後退、A/Dで操舵、Spaceでブレーキ、Shiftで降車。所有車両をShift＋右クリックで回収。近傍64ブロック以内に自分の車両を最大8台まで配置できます。

車体の形状はユーザー提供の参考画像を元に独自に制作。AutomobilityのREADMEにある運転操作を参考にしました。Automobilityのコードやモデルは取り込んでいません。
参考: https://github.com/FoundationGames/Automobility/blob/1.21-rewrite/README.md

ビルド: `python build.py`、導入: `python build.py --install`。ローカルのMinecraftランチャー付属JDKとインストール済みライブラリでコンパイルします。更新後はゲーム自体の再起動が必要です。

検証済み: Javaコンパイル、Fabricサーバーの設定初期化、加速上限・後退上限・ブレーキ・惰性減速の計算テスト。
未確認: 実走行での速度表示・実際の撮影・専用枠の再接続／死亡時の保存。画面表示の実機確認状況は以下を参照。

2026-09-11: スマホの本体と画面でブロック／アイテムのアトラスを混在させていた読み込みエラーを修正。すべて専用のアイテム画像へ統一し、薄型本体・背面カメラ・側面ボタンを実装。再起動後のログで旧アトラスエラーが消えたことを確認。

スマホUIは黒とシアン／ピンクのネオンテーマ。PULSEの波形アイコン、ゆっくり流れるステータスライン、専用の戻る／閉じるボタンを追加。SNS投稿は引き続き準備中。
実機確認: 更新版で起動・街への接続・Pで取り出し・右クリックで新ホーム表示・PULSE詳細表示・ホームへ戻る操作を確認（2026-09-11）。
ガレージ: スマホの12番目のアプリから車／バイクを呼び出し。64ブロック以内の自分の空車を再利用し、なければ生成（近傍所有車8台まで）。乗車中・他人の所有車は対象外。空間・地面・水・ワールド境界を確認、操作間隔2秒。お金や購入制限はまだ設けていない。
カメラ更新: NEON CAMに撮影枠、シャッター、矢印で向き調整、ズーム、自撮り切替を追加。写真はゲームフォルダのneonward/photosへPNG保存、左下の写真からカメラロール（6枚ずつの一覧・拡大表示）。このPC内だけの保存。車両モデルは非同期のコマンドタグへの依存を廃止し、表示パーツの位置を同期する方式へ修正。既存車両もModelVersion=2で自動更新。
車内更新: 車全体を150%へ拡大、車体判定と配車スペース判定も拡張。シート2脚（乗員は従来どおり運転手1人）、ハンドル、センターコンソール、ギアレバー、ダッシュボード、ドア内張りを追加。速度は実移動距離から同期し、車内テキストと運転HUDへ表示。既存車はModelVersion=3へ自動更新。
車内の実機確認: 拡大した外装、乗車時の座席・内張り、停止時HUDの000 km/h・N表示を確認。乗車直後に前方を向き、旋回に視線が追従する調整もビルド・導入して再起動済み。最終版の前方視界、車内テキストの可読性、走行中の数値変化は未確認。

カメラ操作更新: カメラ画面を開いたまま通常の移動キーで歩行・ジャンプ・しゃがみ・ダッシュ。撮影枠を左ドラッグして向き調整、Enterかシャッターボタンで撮影（Spaceはジャンプ）。移動キーの割り当て変更に対応（キーボード割り当て）。他アプリへ切り替えた時は移動入力を停止し、通常画面やカメラロールには適用しない。
カメラ操作更新の検証: ビルド・導入・再起動と街への接続、新しい操作案内を含むカメラ画面の表示を確認。移動・ドラッグの実操作はユーザー操作と重なったため自動検証未完了。

独自家具・第一弾（2026-09-11）:
/neon furniture で12種類を各8個入手。クリエイティブの機能的ブロック欄にも追加。全品にクラフトレシピあり（鉄インゴット＋レッドストーン＋家具別の材料）。独自のブロック形状・金属／布／画面／ネオンテクスチャを使用。
- ネオンベッド: 2ブロックのベッド。通常の就寝・リスポーンのルールを継承。
- ソファ／ネットランナーチェア／トイレ: 右クリックで着席、Shiftで降車。家具を壊した時と降車時に座席エンティティを削除。
- デュアルリンクデスク: モニター2台、キーボード、PC本体、配線。装飾用。
- 武器ラック／NEON DRINKS飲料庫: 27枠収納。保存・再読込、破壊時の中身ドロップ処理を実装。ラックの武器形状は装飾で、収納品に応じた見た目変更は未実装。飲料庫は収納として使え、課金・飲料生成は未実装。
- シャワーパネル／ネオン浴槽: 右クリックで水しぶきと水音。継続放水や入浴効果は未実装。
- ケーブル束／工具トレー／缶セット: 独自の細かい形状を持つ装飾ブロック。
検証: Javaコンパイル、Fabric起動初期化、クライアント起動、街への接続、12種類のアイテム入手とホットバー／手持ちモデル表示を確認。着席・就寝・収納の実操作と再接続時の保持は未確認（ユーザー操作中のため自動操作を終了）。既存の街への一括置換は実施していない。

2026-09-11 街の家具差し替え:
ネオン本棚・収納カウンター・ロッカー・ローテーブルを追加（全16種類）。NeonWard-Rebuiltの既存Macaw家具2,774ブロックとベッド4ブロックを、位置・向きを保って独自家具へ変換。既存収納2,354個のデータを引き継ぎ、新規収納150個を作成。独自の建築用ディスプレイ、小物、壁、照明、回路は維持。収納にはすべて27枠を使用。
検証: バックアップとの全チャンク比較で家具以外のブロック・その他チャンクデータ・既存収納内容の保持を確認。現在の街のMacaw家具ブロック残数0。変更前のワールドはwork/backups/before-own-furniture-20260911-192456に保存。差し替え数・対応表はoutputs/独自家具・差し替え結果.json。

2026-09-11 独自街灯・自動ドア:
街灯3色（シアン／ピンク／アンバー）、4ブロック高を1回で設置。ネオン縁、配線、点検端末、照明ヘッド付き。/neon lights で各16個。道路の既存街灯54本を置換。
NEON自動スライドドアは2ブロック高。近接プレイヤーを検知して左右のパネルを開閉。通路上のプレイヤーを検知すると閉めない。開状態の通路には当たり判定なし。クリエイティブの機能的ブロック欄、または鉄のドア＋スカルクセンサー＋レッドストーンのクラフトから入手。
街の入口35組・当時のエレベーター287組、計322組（644枚）へ設置。既存入口回路のピストン・センサー・信号部品と旧エレベーター扉パネル／検知マーカー864体を撤去。NeonWard-elevator-cabins.zipの旧扉操作を無効化。この後、下記の動くエレベーターへ更新。
実機確認: クライアント起動、街接続、マイホーム東入口で離れた状態→近接で開く→離れると閉まる動作、中央道路の新街灯の表示を確認。街灯の手動設置・回収、全入口の通行、全階のエレベーター移動は未確認。
差し替えの記録: outputs/独自街灯と自動ドア・差し替え.json。変更前の街全体をwork/backups/before-mod-street-access-20260911-193604に保存。

2026-09-11 箱ごと動くエレベーター:
34棟に専用の連続した昇降路と277乗り場を設置。企業タワーの重複する高さを27階に整理し、旧287乗り場からの連絡通路を接続。高さ違いは階段付き。収納の中身を変更せず、作業前の街全体をwork/backups/before-moving-lifts-20260911-201144に保存。
各階のCALLボタンを右クリックして呼び出す。箱に入り右クリックすると階数選択画面が開く。扉が閉まってから床・壁・天井・車内ボタンを持つ箱が実際に連続移動し、プレイヤーを運ぶ。到着音の後に開扉し、箱内で歩ける状態に戻る。乗員は最大9人。移動中のShiftは最寄りの乗り場へ緊急退避する。
箱が不在の乗り場は開扉しない。戸口にプレイヤーがいる間は発車待ち。出発前に昇降路の障害物を確認し、障害物があれば運転停止。呼び出しを順番に受け付け、到着後3秒待って次へ向かう。車体の階数・移動状態・行き先・待ち列を保存。起動済みの街に置いたコントローラーがある場合にだけ生成し、未読み込み区画の境界では生成しない。
各階のネオンモニターは現在階と行き先／方向を表示し、その下で案内が流れる。8文字ずつ約0.4秒間隔、近くのプレイヤーに合わせて表示更新。箱内にも現在階・案内・操作方法を表示。
検証: Javaビルド、保存済みワールドの全34昇降路・277扉ペア／呼び出しボタン／モニター枠の配置。実機で車内右クリック→階数選択、マイホーム1階（Y65）→2階（Y73）、2階→20階（Y217）を確認。長距離移動の途中でY123.17991を通過し、箱とプレイヤーの連続移動を確認。その他の棟の全階乗車・複数人同乗・走行中の保存再開は未確認。
記録: outputs/動くエレベーター・差し替え.json。街固有の固定座標設定はresources/lifts.json。自由に組み立てて階を登録するための設置ツールはまだ含まない。
追加の実機確認: 再起動後も20階の箱を保持。19階のCALLボタンから呼び出すと「20F待機→下降→19F待機」と表示が変わり、到着後に扉が開くことを確認。さらに1階から19階の箱を呼び戻し、連続下降と1階到着を確認。文字が流れる表示も目視確認。再保存後のデータで読み込み済み17棟すべての箱が各1体、マイホームの箱がY65・待機・表示パーツ11体で保存されていることを検証。高層の乗り場外縁にガラス柵610ブロックを追加。

2026-09-11 1枚ドア・寝室家具・自販機・表札:
残っていた木の1枚ドア145組を既存のNEON自動ドアへ変更。向きと蝶番を保持して、上下2ブロックと検知の開始を設定。
ベッドのネオン縁・枕・マットレスの縫い目・頭側端末を追加し、住宅の空きスペースに実際に寝られるベッド33台を配置（既存ベッド2台も同モデルへ更新）。動作はMinecraftのBedBlockを使用。既存の寝室と昇降路を残し、空き床と2ブロックの頭上空間を確認して設置。
ワークデスクとネットランナー端末デスクを追加。ノートPC・紙・ペン・照明・引き出し、複数画面・PC本体・配線などをモデル化。街の既存デスク28台を2種類へ差し替え。/neon furnitureの家具は19種類。
PULSE MART自販機は2ブロック高で、商品棚・決済端末・取り出し口・ネオン縁を備える。街の入口や受付付近へ45台を配置。右クリックで購入画面を開き、ソーダ／コーヒー／パワーバーを選ぶ。現在の価格は各エメラルド1個で、独自通貨との接続はまだない。サーバー側で距離・本体・所持金・持ち物の空き・連続操作間隔を確認してから交換。飲食物は独自アイテムで、ソーダとコーヒーは飲む動作。別途残っているneon_vendorは従来の収納家具。
部屋のネオン表札146枚を追加。部屋番号、寝室、リビング・キッチン、浴室、トイレ、武器庫などを表示。黒い板と発光枠を壁の前に固定し、8秒周期で短いピンク／シアンの点滅・弱い減光・約0.014ブロックの横揺れを加える。部屋番号の重複1件を修正。元の光る看板8枚の内容を移行。
確認済み: Javaコンパイル、45台の自販機の上下と向き、33台のベッドの頭と足の組み合わせ、146組の表札文字と枠、更新版の街の起動と自販機モデル表示。ユーザーがゲーム操作中だったため、購入・飲食・新ベッドでの睡眠・全ドアの実操作テストは未実施。配置記録とバックアップ先はoutputs/部屋家具・自販機・ネオン看板.json。

2026-09-11 地図と最上階家具の更新:
スマホのマップをNEON ATLASへ変更。保存済みの街608×736ブロックから建物・道路を抽出した地図を表示し、縦横比を維持。+/-拡大、ドラッグ移動、全体、現在地、主要6施設の選択、4方向の門、リアルタイムの位置・向きに対応。背景は今回の街の保存データを元にした固定地図で、後から建築したものは自動反映しない。work/build_phone_map.pyで再生成できる。
最上階に残っていた階段椅子・浴槽・旧テレビなどを置き換え。専用PULSEテレビとスイート浴槽を新規追加。ソファ12、椅子4、テーブル18、机8、浴槽・シャワー・トイレ各1、テレビ1、ベッド2組を配置／更新。旧家具表示パーツ82体を取り除き、配管や小物は維持。表札の枠を金属パネル・留め具・ネオンレールの独自モデルに更新し、自室5部屋の名前を修正。
Javaコンパイルと配置50ブロック・新モデル座標の検証済み。変更前の街はoutputs/自室・独自家具差し替え.jsonに記した場所へ保存。

2026-09-11 テレビ修復・街の破壊防止:
壊れたテレビを x72..77,y219..221,z218 の6×3画面へ復元。YouTube 0jivuzUPzpY と音声設定を復元。バックアップは work/backups/before-tv-repair-*。
街の範囲（オーバーワールド x=-32..575,z=-32..703）のブロックは、クリエイティブを含め左クリック破壊を拒否し、サーバー側のブロック破壊イベントでも保護。右クリックの通常操作は維持。爆発の破壊対象から街のブロックを除外。ディスプレイ、額縁、絵画、防具立ては直接攻撃を拒否し、周辺で無敵を付与。街外の採掘には適用しない。管理コマンドによる編集は対象外。


### TV URL input (2026-09-11)
PULSE TV now accepts up to 8,192 characters, offers a full clipboard replacement button and character count, and keeps validation errors visible without closing. YouTube watch/share/live/shorts/embed links are normalized to a short watch URL, preserving valid start times. Bing video search links are not video URLs: use YouTube > Share > Copy. Tested normalization with a 6,000-character tracking query and rejected malformed, truncated, over-limit, and non-YouTube links.


### Plaza goldfish hologram (2026-09-11)
An invulnerable, non-colliding projection automatically appears when the plaza anchor chunk is ticking (160,112,154). Four full-bright translucent display meshes form a roughly 21-block goldfish. It follows a 105-second oval path, gently changes height, and independently waves its tail and pectoral fins. Animation updates every four ticks with interpolation; there is no pathfinding or permanent chunk forcing. Mesh generation: work/build_hologram_fish.py.


### Arsenal (2026-09-11)
/neon arsenal grants the original blade, PULSE-10 rifle, four Sentinel armor pieces and 64 energy cells to an operator. Items are also in the Combat creative tab and have crafting recipes. Rifle: right click, one cell per shot outside creative, 0.5-second cooldown, 64-block range, 10 damage before armor, cyan tracer and sound. Ray stops at solid block collision; players and decorations are excluded. Armor uses an original equipped texture asset with 20 total defense, 2 toughness per piece and iron repairs. Compilation and asset references checked; live combat verification pending installation.


### NEON THREADS clothing shop (2026-09-11)
Eight original styles, each with headwear, top, trousers and footwear (32 items). Inspired by CDPR visual-style concepts; no game assets copied. Cosmetic Equippable only: no armor/toughness/knockback attributes, no durability. Sentinel items also changed to cosmetic; cyberware progression is still pending. Southern former armor store now contains a counter at (248,65,485), shop label and three locked mannequins. Click the counter to browse eight pages and buy individual pieces for 150–900 Cr using the same saved account as stocks. Server checks counter proximity, balance and a free inventory slot before charging. Initial account is the existing 10,000 Cr account. Assets and compilation checked; live purchase check pending. Lookbook: outputs/fashion-lookbook.png.


### Tailored collection (2026-09-11)
Replaced the shop catalog with nine colorways (36 items) across kimono, denim jackets and leather jackets. Retired eight simple styles are hidden from the catalog/creative tab; player inventory copies are exchanged every 40 ticks, retaining count. Legacy item IDs remain loadable for saved containers. Detailed 256x256 original atlases and custom humanoid meshes provide raised collars, pockets, cuffs, obi, long kimono hems, buckles and boot details. Footwear: setta sandals, wheat work boots, red sole heel boots. Bottoms: hakama-style, distressed denim and plain black trousers. Clothing is cosmetic only. Custom meshes apply to adult humanoids, including mannequins; babies fall back to vanilla rendering. Build and asset links checked; in-game visual verification pending.

The final active catalog is nine colorways / 36 items (not the earlier eleven styles). Dedicated footwear assets avoid painting boots onto trouser textures. Setta has an empty leg mesh, sole and thong straps; work boots have laces; heel boots have a distinct heel and red sole. Legacy player-inventory exchange is enabled, and existing mannequin gear is refreshed. Installed and world loaded without new rendering/mixin errors; user is checking worn appearance while playing.
