# 実機E2E動作確認チェックリスト

このリポジトリには現在、Paper+Velocity二重起動を伴うE2Eテストの自動化基盤がありません
（`velocity`モジュールはVelocity APIが`compileOnly`のため、実際に起動しているVelocityプロキシ
の外では検証できません。`MockBukkit`のようなサーバーモックも導入されていません — 詳細は
`CLAUDE.md`の「Build」節を参照）。フルの自動E2Eをゼロから構築するのは大掛かりな別プロジェクト
になるため、当面は本チェックリストに沿った**手動検証**で代替します。将来自動化したくなった
場合は、このチェックリストがテストシナリオの土台になります。

## 準備

1. Paperサーバーを2台（`hub`役と`game`役など）用意し、両方に`orelia-serverutil`(paper)の
   jarを導入する。
2. Velocityプロキシを1台用意し、`orelia-serverutil-velocity`のjarを導入する。
3. 両Paperサーバーの`config.yml`で`velocity.enabled: true`・`velocity.channel`をVelocity側の
   設定と一致させる。`hub.mode: PROXY`に設定する。
4. Velocity側の`config.yml`で`hub.server-name`が実際の"hub"サーバー名と一致していることを
   確認する。

## 確認項目

### `/hub`によるサーバー間転送

- [ ] `game`サーバーで`/hub`を実行し、`hub`サーバーへ転送されること。
- [ ] 転送中にVelocity側がタイムアウトした場合、Paper側にエラーメッセージが表示されること
      （`VelocityBridgeModule`の相関ID付きリクエスト/タイムアウト処理）。
- [ ] Velocityが未起動/未接続の状態で`/hub`を実行した場合、エラーメッセージが表示され
      サーバーがクラッシュしないこと。

### サーバー切り替え時のjoin/leaveメッセージ

- [ ] `game`サーバーに他プレイヤーがいる状態で、別プレイヤーが`hub`から`game`へ`/hub`経由で
      入室した際、`{player} | hub -> game`形式のメッセージが`game`側の全員に表示されること
      （`SERVER_SWITCH_NOTIFY`）。
- [ ] 逆に`game`から`hub`へ移動した際、`game`に残っている全員へ`{player} | game -> hub`形式の
      leaveメッセージが表示されること（`SERVER_SWITCH_LEAVE_NOTIFY`）。移動元に誰も残っていない
      場合は何も送信されない（エラーにならない）こと。
- [ ] `SERVER_SWITCH_NOTIFY`とBukkitの`PlayerJoinEvent`のどちらが先に届いても、最終的に
      「切り替えメッセージ」として表示されること（プレーンなjoinメッセージにフォールバック
      しないこと）。`awaitArrival`/`awaitDeparture`の競合状態を突くため、Velocity側を意図的に
      少し重くする（他の重い処理を走らせる等）などして再現を試みる。

### タブリスト/スコアボード/belowname反映

- [ ] OreliaCore（+OreliaExtra）を導入した状態で、レベル・所持金がサイドバーに表示されること
      （`scoreboard.core-lines`）。
- [ ] ギルド/パーティー所属プレイヤーのタブリスト名prefixに`{guild}`/`{party}`が反映される
      こと（`tablist.name-color.prefix-format`）。
- [ ] サイドバーの「現在地」行（`{location}`トークン、`scoreboard.lines`）が移動に応じて
      更新されること（`scoreboard.update-interval-ticks`ごと）。
- [ ] belowname（名札下）が、値を返すプロバイダーがいない間は自動的に非表示になり、
      いずれかが値を返すようになった瞬間に表示されること。

### `/suadmin reload`

- [ ] `config.yml`の`tablist.name-color.suffix-format`等を変更した状態で`/suadmin reload`を
      実行し、再起動なしで新しい書式が反映されること。
- [ ] `velocity.enabled`を切り替えて`/suadmin reload`し、プラグインメッセージチャンネルの
      登録/解除が正しく追従すること（`VelocityBridgeModule.onReload`）。

## 既知の制約

- Velocity APIが`compileOnly`のため、`rpg.serverutil.velocity.*`のユニットテストは書けない
  （静的レビューのみ）。
- `common`モジュールの`ProtocolCodec`往復テストのみが自動化されている
  （`./gradlew :common:test`）。
