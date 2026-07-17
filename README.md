<img src="https://orelia-mc.github.io/assets/logo_wide.jpg" />
<h1 align="center">Orelia ServerUtil</h1>
<p align="center">ServerUtil plugin of Orelia-MC</p>

Orelia RPGプラグイン群（orelia-core / orelia-world / orelia-extra）とは独立した、サーバー運用・UX周りの汎用機能プラグインです。ゲームプレイに依存しないため、Orelia系プラグインが無いサーバーでも単体で動作します。

`common` / `paper` / `velocity` の3モジュール構成です。`paper`単体でも動きますが、複数のPaperサーバーをVelocityでまとめている場合は`velocity`モジュールも導入することで、サーバー間の`/hub`転送や移動通知演出が使えるようになります。

## 依存関係

- **orelia-core**（任意・softdepend）: 未導入でも起動しますが、導入されている場合はサイドバーにレベル/所持金のサンプル行を自動追加します（`core-integration.enabled: false`で無効化可能）。
- **Velocity + OreliaServerUtil(Velocity)**（任意）: `hub.mode: PROXY`やサーバー切り替え時の演出には、Velocity側にも`velocity`モジュールのjarを導入し、`config.yml`の`velocity.enabled: true`にする必要があります。

## ビルド

```
./gradlew build
```

- `paper/build/libs/orelia-serverutil-1.0.0.jar`
- `velocity/build/libs/orelia-serverutil-velocity-1.0.0.jar`

`paper`モジュールはOreliaCoreをjitpack経由でsoftdepend参照するため、`repo.papermc.io` / `jitpack.io`へのネットワークアクセスが必要です。`common`モジュールは`./gradlew :common:test`でJUnit5の往復エンコード/デコードテストが実行できます（Velocity APIはcompileOnlyのため、velocityモジュール自体の自動テストはできません）。

## Paper側コマンド

### `/hub`

`config.yml`の`hub.mode`次第で動作が変わります。

- `TELEPORT`（デフォルト）: このサーバー内の`hub.teleport.*`座標へテレポートします。単体サーバーでもそのまま使えます。
- `PROXY`: Velocity経由でハブサーバーへ転送します。転送先サーバー名はVelocity側の`config.yml`（`hub.server-name`）が決定し、Paper側からは指定できません（なりすまし・誤設定対策）。`velocity.enabled: true`かつVelocity側の`orelia-serverutil-velocity`が稼働している必要があります。

### `/suadmin`

```
/suadmin reload
/suadmin setspawn
/suadmin worldsetup <world> [profile]
```

- `reload`: config/messagesを再読み込みします。
- `setspawn`: 実行者の現在地をそのワールドのスポーン地点に設定します（`World#setSpawnLocation`）。
- `worldsetup <world> [profile]`: `config.yml`の`world-setup.profiles.<profile>.gamerules`を対象ワールドに一括適用します（`profile`省略時は`default`）。任意のバニラGameRule名がそのままキーとして使えます。

## API（他プラグインからの連携）

`ServicesManager`経由で以下のインターフェースを公開しています。

- `rpg.serverutil.api.ScoreboardApi` / `ScoreboardLineProvider` — サイドバーに行を差し込む。優先度が高いものから上に表示されます。
- `rpg.serverutil.api.TabListApi` / `TabListNameFormatter` — タブリストの名前prefix/suffixをカスタマイズする。

実装例は`rpg.serverutil.paper.integration.CoreIntegrationModule`（OreliaCoreのレベル/所持金を表示するサンプル）を参照してください。

## config.yml 主要セクション

`velocity`, `hub`, `world-setup.profiles.*`, `scoreboard`, `tablist`, `announce`, `admin-healthcheck`, `server-switch-notify`, `core-integration`。各セクションの詳細はコメント付きで`config.yml`本体に記載しています。

## Velocity側のセットアップ

1. `orelia-serverutil-velocity-*.jar`をVelocityの`plugins/`に配置して起動し、`plugins/orelia-serverutil/config.yml`を編集（`hub.server-name`をバックエンドサーバー名に合わせる）。
2. 各Paperサーバー側の`config.yml`で`velocity.enabled: true`にし、`channel`がVelocity側と一致していることを確認。
3. `/hub`の`hub.mode`を`PROXY`にする。

## 開発時の注意（mavenLocal依存）

ルート`build.gradle.kts`の`subprojects.repositories`に一時的に`mavenLocal()`を追加しています。orelia-coreを並行して変更している間は、`orelia-core`リポジトリで`./gradlew publishToMavenLocal`を実行してから本プラグインをビルドしてください。本番リリース前にはこの行を削除し、jitpack経由の解決のみに戻す想定です。
