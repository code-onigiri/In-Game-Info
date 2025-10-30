# 🧠 IngameInfo HUD 開発ガイド（AIコーダー向け）

このドキュメントは、**AI支援による開発・自動コード生成**を行う際に必要な環境構成とルール、  
およびプロジェクトの意図・設計方針をまとめたものです。

---

## 📦 プロジェクト概要

- **Mod 名称:** IngameInfo HUD
- **目的:** プレイヤーやワールド情報を画面上（HUD）にカスタム表示する
- **ベース:** Minecraft Forge Mod (1.20+)
- **主要構成:**
  ````
  src/main/java/com/codeoinigiri/ingameinfo/
  ├── IngameInfo.java                  # メインModクラス
  ├── hud/
  │   ├── HudOverlay.java              # 実際のHUD描画処理
  │   ├── config/                      # Config関連
  │   └── variable/
  │       ├── VariableResolver.java    # 動的変数収集・展開
  │       ├── CacheConfig.java         # 変数キャッシュTTL管理
  │       ├── ExpressionUtils.java     # 変数・式の評価
  │       └── CachedValue.java         # 値＋タイムスタンプ構造体
  ```

---

## ⚙️ 開発環境

| 項目 | 内容 |
|------|------|
| **Mod Framework** | Forge |
| **Language** | Java 17+ |
| **Build Tool** | Gradle |
| **Config形式** | TOML (NightConfig) |
| **再読み込み方式** | WatchService によるファイル監視リロード |

context7を用いてForge docを使う。

---

## 🧩 主な構成要素

### 🟢 VariableResolver
- 各カテゴリ（`player`, `world`, `environment`, `system`）の値を収集。
- 値は `CachedValue` に保存し、**TTL管理 + 差分更新**。
- `ExpressionUtils` を利用して `${player.health}` や条件式を展開できる。
- 各値は `cache_ttl.toml` に設定された TTL に従って更新される。

---

### 🔵 CacheConfig
- `config/ingameinfo/cache_ttl.toml` を読み込み。
- 変数・カテゴリごとに TTL(ms) を指定可能。
- 初回起動時に自動生成。
- WatchService による自動リロード対応。

#### 🧾 設定例:
```
player_ttl_ms = 200
world_ttl_ms = 1000

[player]
health = 200
posX = 300

[world]
time = 1000
biome = 1500
```

---

### 🟣 ExpressionUtils
- `${player.health}` や `${world.biome}` を動的に展開。
- `player.health < 5 ? "危険!" : "OK"` のような式も処理。
- `Math` 系関数・`format()` 関数も利用可能。

利用可能な組み込み関数例:
```
round(), floor(), ceil(), abs(),
min(a,b), max(a,b),
format(value, digits)
```

---

### 🟠 config/ingameinfo/context フォルダ
- 各HUDブロック（テキスト）を `.toml` ファイルで定義。
- 複数のHUDを同時に読み込み可能。
#### 📍 パッケージ構造
```
com.codeoinigiri.ingameinfo.api/
├── VariableAPI.java              # 🌐 公開API（外部用）
├── VariableRegistryImpl.java      # 🔧 実装クラス（内部用）
└── VariableAPIExamples.java      # 📚 使用例集
```

#### 🔧 主要メソッド（VariableAPI）:
#### 例: `config/ingameinfo/context/test1.toml`
```
VariableAPI.register("custom.myvar", "static_value");
position = "top-left"
color = 0xFFFFFF
VariableAPI.register("custom.timestamp", 
shadow = true

text = """
VariableAPI.update("custom.myvar", "new_value");
体力: ${player.health} / ${player.max_health}
天候: ${world.weather}
VariableAPI.unregister("custom.myvar");
"""
```
Map<String, String> all = VariableAPI.getAll();
---

boolean exists = VariableAPI.contains("custom.myvar");

// サイズ取得
int count = VariableAPI.size();

| 対象カテゴリ | デフォルトTTL | 用途 | 備考 |
VariableAPI.clear();

// デバッグ出力
VariableAPI.debugPrintAll();
VariableAPI.debugGet("custom.myvar");
| `player` | 200ms | 体力・座標・装備 | 変化頻度が高い |
| `world` | 1000ms | 天候・時間・バイオーム | 変化頻度中程度 |
#### 📖 基本的な使用例:
| `system` | 1000ms | FPS・言語 | 比較的静的 |

### 🔁 自動リロード
- `WatchService` により、  
    VariableAPI.register("custom.key_pressed", "true");
- Mod再起動不要。

---
VariableAPI.register("integration.mana", () -> getManaLevel());
## ⚠️ 注意点（AI生成時）

VariableAPI.register("event.boss_health", 
- Minecraft内部クラスの**MixinやASM改変**は行わない。
- `@SubscribeEvent` は `FMLClientSetupEvent` などに正しく登録。
- `Minecraft.getInstance()` 呼び出しは**クライアント側のみ**。
VariableAPI.register("system.memory",

### ✅ コード生成ルール
- クラス単位で出力すること（部分差し替え禁止）
- 外部依存ライブラリの追加は事前に確認。
- コメントは英日併記または日本語。
- `System.out.println` で簡易デバッグ出力OK（Forgeログ統合前提）

---

## 🧠 開発者Tips

- `VariableResolver` 内のキャッシュマップを直接操作しない。
- TTLを短くしすぎるとパフォーマンス低下。
- 変数名は `"category.key"` 形式に統一。
- 新しいカテゴリを追加する場合：
    1. `VariableResolver#updateXxx` を追加
    2. `CacheConfig` にデフォルトTTL定義を追記

---

## 🧩 今後の拡張予定

- ⏱ 各変数の最終更新時刻をHUD表示（デバッグ用途）
- 🧮 ユーザー定義関数登録API
- 🎨 背景グラデーション/ボーダー設定
- 🔍 Mod間連携 (他Mod情報のHUD化)

---

## 🪄 まとめ

このプロジェクトでは「**動的HUD構成 + 柔軟な変数展開 + キャッシュ制御**」を中心に設計されています。  
AIコーダーは **キャッシュ最適化・安全なファイル操作・式評価ロジックの整合性** に特に注意してください。

---

💡 **更新推奨**  
この `copilot-instructions.md` はコードの拡張時に必ず更新し、  
AIが誤った構成変更を行わないように保守してください。