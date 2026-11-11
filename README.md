# Donation Alerts (Fabric, client-side)

Stream-style donation alerts + a top-donor leaderboard for Donut SMP. Bigger donations get bigger animations.
Client-only: it reads "X paid Y $amount" server messages and draws on your HUD. No textures or sounds to install.

## Tiers (change them in config/donationalerts.json)
| Tier       | Default min | What you get |
|------------|-------------|--------------|
| COMMON     | any         | small banner |
| NICE       | $10K        | green banner + confetti |
| BIG        | $50K       | cyan banner, sunburst rays, firework sound |
| HUGE       | $250K         | orange banner, screen shake, more confetti |
| LEGENDARY  | $1M        | gold shimmer title, dimmed screen, falling confetti |
| GODLIKE    | $10M        | "connecting to DonutSMP mainframe" hack intro, falling code, ACCESS GRANTED, rainbow title |
| UNIVERSE   | $100M        | black-screen "In the beginning, there was nothing... then NAME donated." intro, flash, crown, expanding rings |

The leaderboard updates when the banner appears (not before), so it doesn't spoil the alert.

## Leaderboard
Default is a podium (top 3 with heads, names, amounts, a crowned #1 and gold/silver/bronze blocks), top-left.
`/donationalerts style` switches between PODIUM and LIST (top N rows, size set by `leaderboardSize`).
Heads show the donor's real skin when they're in the tab list (remembered for the session), otherwise a default skin.
The donor who just paid gets a green glow on their column.

## Commands
- `/donationalerts test 5m [name]` - preview a tier (test donations are never saved)
- `/donationalerts leaderboard` - show/hide the leaderboard
- `/donationalerts style` - switch podium / list
- `/donationalerts top` - print the top 10 in chat
- `/donationalerts skip` - skip the alert that's playing
- `/donationalerts clear` - clear the queue + test donations
- `/donationalerts reset` - wipe all saved donations
- `/donationalerts reload` - reload the config

## Build the .jar (no installs needed)
1. Make a free GitHub account, create a new repository, and upload everything from this folder (including the hidden `.github` folder).
2. Open the repo's **Actions** tab. The `build` workflow runs by itself (or press "Run workflow").
3. When it turns green, open the run and download **donationalerts-mod-jar** under Artifacts. Unzip it to get the `.jar`.
4. Put the `.jar` in `.minecraft/mods/` together with Fabric API for 1.21.11.

If the build fails, open the failed step and send me the red error lines; I'll fix the code.

Building locally instead: needs JDK 21 and Gradle 9.x, then run `gradle build`. The jar ends up in `build/libs/` (use the one without `-sources`).

## Settings that matter
- `trackAll` (false): only react to payments sent to YOU. Set true to react to every "X paid Y" message.
- `chatRegex`: groups are 1 = payer, 2 = recipient, 3 = number, 4 = k/m/b/t. If Donut changes its pay message
  and nothing triggers, edit this. Use `/donationalerts test` to confirm the visuals work independent of chat parsing.
- `leaderboardMode`: `TOTAL` (sum per donor) or `SINGLE` (biggest single donations).
- `leaderboardAnchor`: `TOP_LEFT` or `TOP_RIGHT`. `leaderboardScale` / `leaderboardOffsetY` move and resize it.
- `thresholds`: the dollar amount where each tier starts. Raise or lower them to fit your donation sizes.

## Notes
- Understands both "X paid you $ 99.9K" (payments to you) and "X paid Y $1M" (only with `trackAll`).
- If the old donation-alert mod from the video is still installed, remove it so you don't get two overlays.
- Only listens to system/game messages, so other players can't fake a donation by typing the message in chat.
  If your server delivers pay messages through the normal chat channel instead, nothing will trigger; tell me and
  I'll add that listener.
- Donor head uses their skin if they're in the tab list, otherwise a default skin.
- Written for Minecraft 1.21.11 (Yarn mappings, Loom 1.14, Fabric API 0.141.x). It uses the newer HUD API
  (`HudElementRegistry`), so it will not work on 1.21.4 or earlier. If you press F1 (hide HUD), alerts hide too.
