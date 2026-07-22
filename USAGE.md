# CykutaAPI — Usage guide

How to use each component of the API. For installation and build instructions see
the [README](README.md).

- [Commands](#commands) — `BaseCommand`, `CommandRegistry`
- [Config](#config) — `ConfigManager`, `ConfigFile`, `PluginConfiguration`
- [Update checker](#update-checker) — `UpdateChecker`
- [Text](#text) — `Text`

---

## Commands

Two classes work together: `BaseCommand` defines the command (and its subcommands),
and `CommandRegistry` registers it into the server at runtime — so you **don't need
to declare commands in `plugin.yml`**.

### Creating a command

Extend `BaseCommand`, implement `onCommand`, and add subcommands if you need them.

```java
public class CommandMyPlugin extends BaseCommand {

    public CommandMyPlugin(JavaPlugin plugin) {
        super(plugin, "myplugin", "myplugin.command", CommandMode.BOTH);
        setAliases(List.of("mp"));
        addSubcommand(new CommandReload(plugin));
    }

    @Override
    protected boolean onCommand(CommandSender sender, String[] args) {
        sender.sendMessage("MyPlugin help");
        return true;
    }
}
```

The constructor takes:

| Argument      | Meaning                                                              |
|---------------|---------------------------------------------------------------------|
| `plugin`      | Your `JavaPlugin`. Available to subclasses as the `plugin` field.   |
| `command`     | The command name, e.g. `myplugin` → `/myplugin`.                    |
| `permission`  | Permission node required to run it. Pass `null` for none.          |
| `mode`        | `CommandMode.PLAYER_ONLY`, `CONSOLE_ONLY` or `BOTH`.               |

There are shorter constructors when you don't need a permission or a mode:

```java
super(plugin, "myplugin");                       // no permission, BOTH
super(plugin, "myplugin", CommandMode.PLAYER_ONLY);
super(plugin, "myplugin", "myplugin.command");   // permission, BOTH
```

### Subcommands

A subcommand is just another `BaseCommand` added with `addSubcommand(...)`. When the
first argument matches its name, execution is delegated to it with that argument
stripped, so the subcommand sees `args` starting right after its own name.

```java
public class CommandReload extends BaseCommand {

    public CommandReload(JavaPlugin plugin) {
        super(plugin, "reload", "myplugin.reload");
    }

    @Override
    protected boolean onCommand(CommandSender sender, String[] args) {
        // /myplugin reload  ->  args here is everything after "reload"
        sender.sendMessage("Reloaded!");
        return true;
    }
}
```

Subcommands can themselves have subcommands — nesting is unlimited.

### Registering

Register the top-level command from `onEnable`. `CommandRegistry` injects it into the
server's `CommandMap` via reflection.

```java
@Override
public void onEnable() {
    CommandRegistry registry = new CommandRegistry(this);
    registry.register(new CommandMyPlugin(this));
    // registry.register(cmdA, cmdB, cmdC); // several at once
}
```

### Tab completion

Subcommand names are completed automatically. Add extra completions (e.g. argument
values) with `addTabCompletion(...)`:

```java
addTabCompletion("confirm");
addTabCompletion("cancel");
```

### Custom sender / permission messages

The default replies for a wrong sender type or a missing permission are plain text.
Override the hooks to route them through your own messaging system (for example a
`Chat` helper backed by a `lang.yml`):

```java
@Override
protected void onNoPermission(CommandSender sender) {
    Chat.send(sender, "no-permission");
}

@Override
protected void onInvalidSender(CommandSender sender) {
    Chat.send(sender, "invalid-sender");
}
```

---

## Config

Three classes:

- **`ConfigManager`** — the entry point. Registers YAML files and gives you access
  to them by name.
- **`ConfigFile`** — one YAML file on disk. You rarely touch it directly.
- **`PluginConfiguration`** — the loaded config; a `YamlConfiguration` whose typed
  getters write the default back when a key is missing, so files stay complete
  ("self-healing").

### Registering files

Register each file once, usually in `onEnable`. Keep the `ConfigManager` around so
you can read and reload later.

```java
public class MyPlugin extends JavaPlugin {

    private ConfigManager configs;

    @Override
    public void onEnable() {
        configs = new ConfigManager(this);
        configs.register("config.yml");
        configs.register("lang.yml");
        configs.register("data/players.yml"); // no bundled resource -> created empty
    }

    public ConfigManager configs() {
        return configs;
    }
}
```

**Defaults from bundled resources.** If your plugin jar bundles a resource with the
same name (e.g. a `config.yml` in `src/main/resources`), it is copied out as the
default the first time the file is created. On every load, keys added in newer
versions are **merged into the user's file automatically** (with their comments),
and keys that no longer exist in the bundled default are removed. Files **without** a
bundled resource are simply created empty — ideal for runtime data files.

### Reading and writing

```java
PluginConfiguration config = configs.get("config.yml");

// Typed getters. If the key is missing, the default is written back to the config
// in memory, so the file ends up complete after the next save.
int radius     = config.getInt("protection.radius", 5);
boolean pvp    = config.getBoolean("protection.pvp", false);
double chance  = config.getDouble("drops.chance", 0.25);
String prefix  = configs.get("lang.yml").getString("prefix");
Material block = config.getMaterial("protection.block"); // defaults to BARRIER

// getString(path) with no default returns a "No value found (...)" marker,
// which makes missing keys obvious in-game instead of returning null.
String label = config.getString("gui.title");

// Change a value and persist it.
config.set("protection.radius", 10);
configs.save("config.yml");
```

### Reloading

```java
configs.reload("config.yml"); // one file
configs.reloadAll();          // every registered file
```

The `PluginConfiguration` reference returned by `get(...)` **stays valid across
reloads** — its values are refreshed in place, so you can safely cache it in a field
instead of calling `get(...)` every time.

### Saving

```java
configs.save("config.yml"); // one file
configs.saveAll();          // every registered file
```

---

## Update checker

`UpdateChecker` compares the running plugin version (from your `plugin.yml`) against
the latest GitHub **release** of a repository. It **only reports the result** — it
never messages players, logs to console or reads any config. Your plugin decides
whether, when and how to react.

### Basic use

Point it at a repository in `owner/name` form and fire the check from `onEnable`.
The HTTP request runs asynchronously (never blocking the main thread) and the
callback is delivered back **on the main server thread**, so it's safe to touch the
Bukkit API inside it.

```java
@Override
public void onEnable() {
    new UpdateChecker(this, "cykvta/CykutaAPI").check(result -> {
        if (result.isOutdated()) {
            getLogger().warning("New version available: " + result.latestVersion()
                    + " (running " + result.currentVersion() + ")");
            getLogger().warning("Download: " + result.downloadUrl());
        }
    });
}
```

### Enabling / disabling

The third constructor argument toggles the check on or off (defaults to `true`).
Wire it to your own config so server owners can turn it off:

```java
boolean enabled = getConfig().getBoolean("update-check", true);
UpdateChecker checker = new UpdateChecker(this, "cykvta/CykutaAPI", enabled);
checker.check(result -> { /* ... */ });

// Can also be flipped later:
checker.setEnabled(false);
boolean on = checker.isEnabled();
```

When disabled, `check(...)` is a **no-op** and the callback is never invoked.

### The `Result`

`Result` is a record carrying the outcome:

```java
Status status  = result.status();          // OUTDATED, UP_TO_DATE or FAILED
String current = result.currentVersion();  // the version you are running
String latest  = result.latestVersion();   // latest release tag, null when FAILED
String url     = result.downloadUrl();      // release page URL, null when unavailable
boolean old    = result.isOutdated();       // shorthand for status == OUTDATED
```

`Status` values:

| Status       | Meaning                                                          |
|--------------|------------------------------------------------------------------|
| `OUTDATED`   | A newer release is available.                                    |
| `UP_TO_DATE` | You are running the latest release.                             |
| `FAILED`     | The check could not complete (network error, no releases, etc.).|

Version comparison ignores a leading `v`, so `v1.2` and `1.2` are treated as equal.

### Notifying players yourself

Since the API stays out of notifications, do it however suits your plugin — for
example, message operators when they join:

```java
new UpdateChecker(this, "cykvta/CykutaAPI").check(result -> {
    if (!result.isOutdated()) return;

    getServer().getPluginManager().registerEvents(new Listener() {
        @EventHandler
        public void onJoin(PlayerJoinEvent e) {
            if (e.getPlayer().isOp()) {
                e.getPlayer().sendMessage(Text.color(
                        "&e[MyPlugin] &6New version: &a" + result.latestVersion()));
            }
        }
    }, this);
});
```

---

## Text

Static helpers for colorizing and formatting strings.

### Colors

`color(...)` translates both `&` color codes and `#rrggbb` hex colors:

```java
String title = Text.color("&6Welcome &lto #55ffaaMyServer");
List<String> lore = Text.color(List.of("&7Line one", "&7Line two")); // list overload

// Remove already-translated color codes (§x):
String plain = Text.stripColor(title);
```

`color(null)` returns an empty string rather than throwing.

### Placeholders

`replace(...)` fills positional placeholders `{0}`, `{1}`, … in order:

```java
String msg = Text.replace("&aGave {0} beacons to {1}", "3", player.getName());
List<String> lines = Text.replace(loreTemplate, "3", player.getName()); // list overload
```

A placeholder whose replacement is `null` becomes an empty string; a `null` list
returns an empty list.
