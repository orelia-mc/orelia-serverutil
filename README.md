<img src="https://orelia-mc.github.io/assets/logo_wide.jpg" />
<h1 align="center">Orelia ServerUtil</h1>
<p align="center">ServerUtil plugin of Orelia-MC</p>

Orelia RPGプラグイン群（orelia-core / orelia-world / orelia-extra）とは独立した、サーバー運用・UX周りの汎用機能プラグインです。ゲームプレイに依存しないため、Orelia系プラグインが無いサーバーでも単体で動作します。

`common` / `paper` / `velocity` の3モジュール構成です。`paper`単体でも動きますが、複数のPaperサーバーをVelocityでまとめている場合は`velocity`モジュールも導入することで、サーバー間の`/hub`転送や移動通知演出が使えるようになります。

## 依存関係

- **orelia-core**（任意・softdepend）: 未導入でも起動しますが、導入されている場合はOreliaCoreのレベル/職業/所持金を、サイドバー・タブリスト名色/右側値・名札下・チャットのプレースホルダーに自動反映します（`core-integration.enabled: false`で一括無効化、各サブセクションで個別に無効化も可能）。
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

- `reload`: config/messagesを再読み込みします。scoreboard/tablist/belowname/chat/announce/core-integrationはサーバー再起動不要でその場に反映されます（title・書式・行内容・更新間隔・ヘッダーフッターのon/off含む）。
- `setspawn`: 実行者の現在地をそのワールドのスポーン地点に設定します（`World#setSpawnLocation`）。
- `worldsetup <world> [profile]`: `config.yml`の`world-setup.profiles.<profile>.gamerules`を対象ワールドに一括適用します（`profile`省略時は`default`）。任意のバニラGameRule名がそのままキーとして使えます。

## API（他プラグインからの連携）

`ServicesManager`経由で以下のインターフェースを公開しています。

- `rpg.serverutil.api.ScoreboardApi` / `ScoreboardLineProvider` — サイドバーに行を差し込む。優先度が高いものから上に表示されます。`config.yml`の`scoreboard.hide-numbers: true`で右側の赤い数字を非表示にできます（Paper 1.20.3+）。`config.yml`の`scoreboard.lines`（プレースホルダー対応、`scoreboard.lines-priority`で表示位置調整）を使えば、他プラグイン無しでも固定行を表示できます。
- `rpg.serverutil.api.TabListApi` / `TabListNameFormatter` — タブリストの名前prefix/suffix/colorをカスタマイズする。このTeamは名札（頭上表示）とタブリスト名の両方に同時に効きます（Minecraftの仕様上、1エンティティは1Teamにしか所属できないため）。
- `rpg.serverutil.api.TabListApi` / `TabListValueProvider` — タブリスト名前の右側の値（`registerValueProvider`）。名前装飾とは別の仕組み（`PLAYER_LIST`スロットのObjective）で、ビューワーごとに異なる値を表示できます。
- `rpg.serverutil.api.BelownameApi` / `BelownameValueProvider` — 名札の下に文字を表示する（`BELOW_NAME`スロットのObjective）。タイトルは`belowname.title`（プレースホルダー対応、ビューワーごとに解決）で全員共通、値だけプロバイダーで対象プレイヤーごとに変わります。有効/無効の設定は無く、いずれかのプロバイダーが値を返した瞬間に自動で表示され、誰も値を返さなくなれば自動で非表示になります。
- `rpg.serverutil.api.ChatApi` / `ChatPlaceholderProvider` — チャット送信者の名前の横に表示するプレースホルダー（レベル・称号・ランク等）を差し込む。表示位置・整形は`config.yml`の`chat.format`で制御します。送信者名にマウスホバーすると、送信時刻・プレイヤー名・レベル等のツールチップも表示されます（`chat.tooltip.enabled` / `chat.tooltip.lines`、既定で有効）。

実装例は`rpg.serverutil.paper.integration.CoreIntegrationModule`を参照してください。OreliaCoreが導入されている場合、サイドバー（レベル/所持金）に加え、上記5つのAPI全てにOreliaCoreの職業色・レベル・所持金を反映するプロバイダーを自動登録します（`config.yml`の`core-integration.*`で書式・有効/無効を設定可能）。

### プレースホルダー

各プロバイダーの`format`系設定は`rpg.serverutil.paper.placeholder.PlaceholderService`で解決されます。`{online}` `{tps}` `{ping}` `{world}` `{player}` `{server}` `{date}` `{time}`は常時使用可能、`{level}` `{job}` `{money}` `{health}` `{max_health}`はOreliaCore導入時のみ解決されます（`{health}`/`{max_health}`はバニラの体力ではなくOreliaCore独自のHPです）。PlaceholderAPIが導入されていれば`%...%`記法もそのまま使えます。全プレースホルダーの一覧は`config.yml`冒頭のコメントを参照してください。

## サーバー間移動時のjoin/leaveメッセージ

`velocity.enabled: true`の場合、プレイヤーがバックエンドサーバーを切り替えると、通常のjoin/quitメッセージの代わりに`{player} | {from} -> {to}`形式のメッセージが移動元・移動先それぞれのサーバーにローカル表示されます（Velocity全体へのブロードキャストはしません）。

- 移動先サーバー: 通常のjoinメッセージの代わりに`join.server-switch-message`
- 移動元サーバー: 通常のquitメッセージの代わりに`join.server-switch-leave-message`（移動元サーバーに他のプレイヤーが誰もいない場合は送信されません — best-effort）
- 初回ログイン（前サーバーなし）や、プロキシからの完全切断（切替先の通知が来ない）の場合は、既存の`join.first-join-message` / `join.quit-message`にフォールバックします
- `join.server-switch-wait-ticks`（既定5tick）だけ、切替通知の到着を待ってからメッセージを確定します

## 管理者向けHealthCheck

`admin-healthcheck.enabled: true`（既定）の場合、op権限のプレイヤーがjoinするとTPS/オンライン人数のサマリーを表示します。加えて`core-integration.enabled: true`（既定）の場合、OreliaCore/OreliaWorld/OreliaExtra/OreliaDebugそれぞれの導入状況とバージョンも1行で表示します。

## config.yml 主要セクション

`velocity`, `hub`, `world-setup.profiles.*`, `scoreboard`, `tablist`, `belowname`, `chat`, `join`, `announce`, `admin-healthcheck`, `core-integration`。各セクションの詳細はコメント付きで`config.yml`本体に記載しています。

## Velocity側のセットアップ

1. `orelia-serverutil-velocity-*.jar`をVelocityの`plugins/`に配置して起動し、`plugins/orelia-serverutil/config.yml`を編集（`hub.server-name`をバックエンドサーバー名に合わせる）。
2. 各Paperサーバー側の`config.yml`で`velocity.enabled: true`にし、`channel`がVelocity側と一致していることを確認。
3. `/hub`の`hub.mode`を`PROXY`にする。

## 開発時の注意（mavenLocal依存）

ルート`build.gradle.kts`の`subprojects.repositories`に一時的に`mavenLocal()`を追加しています。orelia-coreを並行して変更している間は、`orelia-core`リポジトリで`./gradlew publishToMavenLocal`を実行してから本プラグインをビルドしてください。本番リリース前にはこの行を削除し、jitpack経由の解決のみに戻す想定です。
