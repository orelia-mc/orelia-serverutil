# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`orelia-serverutil` is a gameplay-independent server-operations/UX plugin, separate from the
Orelia RPG plugin suite (orelia-core/world/extra/debug). It provides hub transfer (same-server
teleport or cross-server via Velocity), one-command world setup (GameRule profiles),
sidebar-scoreboard/tab-list-name/tab-list-value/belowname/chat-placeholder APIs other plugins
can plug into, and join/announce/server-switch messaging.

Three Gradle modules:
- **common** (`rpg.serverutil.common`) - platform-independent protocol/data classes shared by
  paper and velocity. No Bukkit/Velocity API imports.
- **paper** (`rpg.serverutil.paper`) - the Paper/Bukkit plugin. Works standalone; OreliaCore
  is a **soft dependency only** (`plugin.yml` `softdepend`, not `depend`) - this plugin must
  start and function fully without it installed.
- **velocity** (`rpg.serverutil.velocity`) - the Velocity proxy plugin. Only needed for
  `hub.mode: PROXY` and the server-switch join/leave chat messages; the paper module works
  fine without it.

## Build

```
./gradlew build
```

Builds all three modules. `paper/build/libs/orelia-serverutil-1.0.0.jar` and
`velocity/build/libs/orelia-serverutil-velocity-1.0.0.jar` are the deployable artifacts.
Requires network access to `repo.papermc.io` and `jitpack.io` (paper module resolves
OreliaCore from its GitHub repo via jitpack, softdepend only).

```
./gradlew :common:test
```

The only automated test coverage - round-trip encode/decode checks for
`rpg.serverutil.common.protocol.ProtocolCodec`. The Velocity API is compileOnly, so the
velocity module itself can't be exercised outside a running proxy; static review is the only
verification available for `rpg.serverutil.velocity.*` (see README.md).

### Local dev loop against orelia-core

Root `build.gradle.kts`'s `subprojects.repositories` has a temporary `mavenLocal()` entry
ahead of jitpack. When iterating on orelia-core in parallel with this repo, run
`./gradlew publishToMavenLocal` in orelia-core first so changes are picked up without a push.
Meant to be removed before a production release - don't remove it unprompted.

## Architecture

### Why this plugin doesn't reuse orelia-core's ConfigManager/MessageManager

orelia-world/orelia-extra/orelia-debug all reuse `rpg.core.config.ConfigManager` and
`rpg.core.message.MessageManager` directly, because OreliaCore is a **hard** dependency for
them. orelia-serverutil is different: OreliaCore is only a **soft** dependency, so importing
those classes directly would risk a `NoClassDefFoundError` at startup on a server that
doesn't have OreliaCore installed at all. Instead, `rpg.serverutil.paper.config`/
`rpg.serverutil.paper.message`/`rpg.serverutil.paper.util.ColorUtil` are independent
lightweight copies. Keep it that way - don't switch these back to importing `rpg.core.*`.

### Module system

`ServerUtilModule` (`rpg.serverutil.paper.module`) + `ServerUtilModuleManager` mirror
orelia-core's `RpgModule`/`ModuleManager` (registration order = enable order, reverse =
disable order). `OreliaServerUtilPlugin.onEnable()` registers modules in this order:

```
SpawnModule -> WorldSetupModule -> VelocityBridgeModule -> HubModule ->
ScoreboardModule -> TabListModule -> BelownameModule -> JoinMessageModule ->
AnnounceModule -> ChatModule -> HealthCheckModule ->
CoreIntegrationModule (always last)
```

Note: `ServerUtilModuleManager.register()` populates its by-type lookup map for *every*
module before `enableAll()` runs any `onEnable()`, so `plugin.getModuleManager().get(X.class)`
always resolves regardless of registration order - but the returned module's own fields are
only fully initialized once *its* `onEnable()` has actually run. `JoinMessageModule` relies on
`VelocityBridgeModule` being registered (and therefore enabled) earlier in the list above.

`HubModule` depends on `VelocityBridgeModule` being registered first (looks it up via
`plugin.getModuleManager().get(VelocityBridgeModule.class)` for `hub.mode: PROXY`).
`CoreIntegrationModule` is always last - it reaches for every display API published by the
modules above it (`ScoreboardApi`/`TabListApi`/`BelownameApi`/`ChatApi`) plus OreliaCore's own
`StatusApi`/`EconomyApi`/`JobApi`, all of which must already exist by the time it runs. Each
provider registration independently null-guards the specific OreliaCore API it needs (job
color on the tab-list Team - kept separate from placeholders since it needs the raw job id,
not the display name) - see `rpg.serverutil.paper.integration.Core*Provider`/`CoreTabListFormatter`.

### `/suadmin reload` (`ServerUtilModule.onReload()`)

Most modules already read config.yml fresh on every use (`AnnounceModule`, `ChatModule`,
`HealthCheckModule`, `ConfigScoreboardLineProvider`), so they need no explicit `onReload()` -
`ConfigManager.reloadAll()` re-reading the underlying `YamlConfiguration` is enough. The
exception is any module whose `onEnable()` captures a config value into a long-lived field
(a manager's title/format string, a scheduled task's interval) instead of re-reading it each
time - those values go stale after `/suadmin reload` unless the module implements
`onReload()` to push fresh config into the existing objects. `ScoreboardModule`/
`BelownameModule`/`TabListModule` do this by exposing mutator methods on their manager
(`ScoreboardManager.updateSettings`, `BelownameManager.setTitleTemplate`) and by tracking
their own `BukkitTask` so they can cancel + reschedule it with a new interval, rather than
recreating the manager itself (which would silently drop every provider some other plugin had
already registered into it). `CoreIntegrationModule.onReload()` takes the simpler path of
just calling `onEnable(plugin)` again - safe only because every `Core*Provider`'s `getId()` is
a stable constant, so re-registering just replaces that one map entry in the target manager
without touching providers registered by anyone else. `VelocityBridgeModule.onReload()` is a
third variant - it unregisters its old plugin messaging channel (if it had one) before
re-reading `velocity.enabled`/`velocity.channel` and re-registering, so flipping
`velocity.enabled` or changing `channel` takes effect without a restart. When adding a module
that caches a config value in a field, follow one of these patterns rather than leaving
`onReload()` as the interface's no-op default.

### Placeholders (`rpg.serverutil.paper.placeholder.PlaceholderService`)

Every config `format`/`suffix-format`/`placeholder-format`/`lines` key across this plugin
(`Core*Provider`, `AnnounceModule`, `TabListModule`'s header/footer, `ChatModule`'s hover
tooltip lines) is resolved through one
shared `PlaceholderService` instance (`OreliaServerUtilPlugin.getPlaceholderService()`) instead
of each hand-rolling its own `String#replace` chain. It resolves built-in tokens
(`{online}`/`{tps}`/`{ping}`/`{world}`/`{player}`/`{server}`/`{date}`/`{time}`) unconditionally,
OreliaCore tokens (`{level}`/`{job}`/`{money}`/`{health}`/`{max_health}`) via the same
soft-dependency `ServicesManager` lookups `CoreIntegrationModule` uses (left as literal text if
OreliaCore isn't installed), then PlaceholderAPI's `%...%` syntax last if that plugin is present
- see `PlaceholderApiHook`, the only class allowed to reference `me.clip.placeholderapi`
directly, isolated so a missing PlaceholderAPI jar never triggers `NoClassDefFoundError`
elsewhere. When adding a new config-driven text field, resolve it through this service rather
than adding another one-off `.replace(...)` chain. `CoreStatusLineProvider` (the sidebar's
level/money line) is a `ScoreboardLineProvider`, not a `format` string, so it doesn't fit that
pattern directly - it takes `scoreboard.core-lines.lines` as a `List<String>` and
resolves each line the same way. `ChatModule`'s `chat.color-codes` is a separate mechanism,
not placeholder-related: gated by the `orelia.serverutil.chat.color` permission (default op,
see plugin.yml), it lets the *sender* use `&`-color codes in their own typed message, resolved
via `ColorUtil.component` on the plain-text-serialized message rather than through
`PlaceholderService`. The full token list is documented in the
cheat-sheet comment at the top of `config.yml`.

`ChatModule` also delegates to `MentionService` (`mention.*` in config.yml) to highlight
`@name`/`@all` mentions and ping whoever got mentioned with a sound - this runs on the same
plain-text-serialized message as `chat.color-codes` (regex-matched against currently-online
player names, case-insensitively; unrecognized `@word`s are left as literal text) and is
independent of `PlaceholderService` too. `@all` is gated by `orelia.serverutil.chat.mention.all`
(default op) so it can't be spammed by anyone typing the word. This only covers public chat -
orelia-extra's party/guild/admin chat channels have their own separate broadcast path this
doesn't reach.

### Commands

Unlike orelia-world/orelia-extra/orelia-debug, this plugin does **not** register into
orelia-core's shared `PlayerCommandRegistry`/`AdminCommandRegistry` (OreliaCore is only a
soft dependency - those registries might not exist). It declares its own top-level Bukkit
commands in `plugin.yml`: `/hub` (`HubCommand`, registered by `HubModule`) and `/suadmin`
(`SuAdminCommand`, registered directly in `OreliaServerUtilPlugin.onEnable()`).

### Public API (`rpg.serverutil.api`)

`ScoreboardApi`/`ScoreboardLineProvider`, `TabListApi`/`TabListNameFormatter`/`TabListEntry`/
`TabListValueProvider`, `BelownameApi`/`BelownameValueProvider`, and `ChatApi`/
`ChatPlaceholderProvider` are published via Bukkit's `ServicesManager` (registered by
`ScoreboardModule`/`TabListModule`/`BelownameModule`/`ChatModule`). This is the intended
integration surface for other plugins wanting to feed the sidebar/tab-list/belowname/chat -
see `CoreIntegrationModule` for a worked example. `ScoreboardManager`/`TabListManager`/
`BelownameManager` all layer their objective/teams onto the *same* per-player `Scoreboard`
instance via `rpg.serverutil.paper.util.BoardUtil.ensurePersonalBoard(Player)` rather than
each calling `player.setScoreboard(...)` with a fresh board, which would wipe out the other
feature's state. If you add another scoreboard-based feature, use `BoardUtil` too.

`TabListManager.tick()` is O(playerCount^2) by necessity - a player's name color in another
player's tab list is driven by that *other* player's own local scoreboard team membership, so
every online player's team has to be written into every online player's personal board each
tick. Acceptable for the player counts this plugin targets; don't "optimize" this to only
touch the viewer's own board, it will silently break cross-player name coloring.

**Nametags and tab-list name decoration share one Team, intentionally.** Minecraft only lets
a scoreboard entry (player name) belong to one `Team` at a time, and that Team's
prefix/suffix/color drive *both* the above-head nametag and (via Bukkit's default tab-list
display-name synthesis) the tab-list name simultaneously. `TabListEntry`'s `color` field
therefore controls both surfaces together - there is deliberately no separate "Nametags API";
see the plan's design notes (`wiggly-purring-fox.md` at the time this was written) for why a
TAB-style split (Team for nametags, `Player#playerListName()` for tab-list name) was rejected:
it would make tab-list names the same for every viewer, losing this plugin's existing
per-viewer differentiation.

The tab-list *value* (`TabListValueProvider`) and belowname (`BelownameValueProvider`) are
unrelated mechanisms - separate `Objective`s in the `PLAYER_LIST`/`BELOW_NAME` display slots,
using `io.papermc.paper.scoreboard.numbers.NumberFormat` (not `org.bukkit.scoreboard.*|` -
easy to reach for the wrong package) to show arbitrary text instead of a plain number
(Paper 1.20.3+; older clients silently see the raw score number instead since the format hint
is ignored). Belowname's `title` is shared across every *target* (Minecraft only allows one
title per objective) but resolved per-*viewer* through `PlaceholderService` (each viewer has
their own personal board, so e.g. `{ping}` in the title reflects the viewer's own ping) -
only the per-target value differs by target. `BelownameModule` has no "enabled" flag:
`BelownameManager` auto-hides the objective whenever no registered provider currently resolves
a value for anyone online, and shows it again the moment one does - this is what lets
`CoreIntegrationModule`'s belowname provider work standalone, without a second flag to flip.

### Velocity <-> Paper bridge (`rpg.serverutil.common.protocol`, `*.bridge`)

Plugin messaging channel `orelia:serverutil` (config-driven, must match on both sides).
Wire format via `ProtocolCodec` (hand-rolled Guava `ByteArrayDataOutput`/`ByteArrayDataInput`
binary framing, same convention as the sibling MultiAccount project) - three message types:

- `HUB_TRANSFER_REQUEST` (Paper -> Velocity): deliberately carries no destination server name
  - Velocity's own `config.yml` (`hub.server-name`) decides where "hub" points to, so a
    compromised/misconfigured Paper backend can't redirect players anywhere else.
  - Correlation-ID request/response with a timeout, same pattern as MultiAccount's
    `PaperCommandBridge` (`VelocityBridgeModule.sendHubRequest`).
- `HUB_TRANSFER_RESULT` (Velocity -> Paper): outcome, delivered back through the *same*
  `ServerConnection` the request came in on.
- `SERVER_SWITCH_NOTIFY` (Velocity -> Paper, to the *destination*): best-effort only, feeds
  `JoinMessageModule`'s join broadcast via `VelocityBridgeModule.awaitArrival`. A dropped
  message here is never treated as an error. Sent unconditionally from
  `ServerSwitchListener.onServerConnected` - there used to be a `server-switch-notify.enabled`
  flag gating this send (originally meant to also gate a title popup on arrival), but that
  silently broke the join/leave chat messages for anyone who disabled it expecting it to only
  affect the title. The title feature was removed rather than re-adding a separate flag for
  it - if a similar "show something extra on arrival" feature gets added later, gate its own
  *display*, not the underlying notify send other features (like this one) depend on.
- `SERVER_SWITCH_LEAVE_NOTIFY` (Velocity -> Paper, to the *source*): same payload shape as
  `SERVER_SWITCH_NOTIFY` (different wire tag), feeds `JoinMessageModule`'s quit broadcast via
  `VelocityBridgeModule.awaitDeparture`. Since the departing player is no longer connected to
  the source server by the time this fires, Velocity relays it through *any other* online
  player still on that server (`ServerSwitchListener.sendLeaveNotify`) - if nobody remains
  there, nothing is sent (nobody would see the leave message anyway).

**`awaitArrival`/`awaitDeparture` race**: `SERVER_SWITCH_(LEAVE_)NOTIFY` arrives over the
network from Velocity while `PlayerJoinEvent`/`PlayerQuitEvent` fires from Paper's own login
sequence - either can happen first, and there's no guaranteed ordering between them. An
earlier version of this bridge had `JoinMessageModule` wait a fixed number of ticks after the
Bukkit event and then poll a cache, which silently fell back to a plain join/quit message
whenever the notify was slower than that fixed wait - the exact failure the destination
server's join message showing as a plain join instead of a switch message turned out to be.
`awaitArrival`/`awaitDeparture` fix this by letting whichever side arrives first register
itself (`pendingArrivals`/`pendingDepartures` if the notify wins, `arrivalWaiters`/
`departureWaiters` if the Bukkit event wins), so the second arrival always resolves the
broadcast immediately instead of guessing a delay. `join.server-switch-wait-ticks` is now only
a worst-case timeout (network stall / dropped packet), not the typical-case latency.

Sending a plugin message requires an online `Player` as the carrier - there is no
console-triggered path, same limitation as MultiAccount's bridge.

## Committing changes

When committing, also update README.md accordingly.
