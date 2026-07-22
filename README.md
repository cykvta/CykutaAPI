# CykutaAPI

Reusable building blocks for Spigot plugins: commands, config files, an update
checker and text helpers.

- **Commands** — `BaseCommand`, `CommandRegistry`
- **Config** — `ConfigManager`, `ConfigFile`, `PluginConfiguration`
- **Update checker** — `UpdateChecker`
- **Util** — `Text`

> 📖 See **[USAGE.md](USAGE.md)** for a detailed guide on how to use each component.

## Importing it

Pick one of the two ways below. Either way, because the classes live in
`icu.cykuta.api`, **shade them into your plugin jar** (or relocate them) so they're
on the classpath at runtime.

### Option A — JitPack (recommended)

CykutaAPI is built on demand by [JitPack](https://jitpack.io) straight from GitHub,
so you don't need to publish or install anything by hand.

Add the JitPack repository and the dependency to your plugin's `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.cykvta</groupId>
        <artifactId>CykutaAPI</artifactId>
        <version>{version}</version>
    </dependency>
</dependencies>
```

Replace `{version}` with a **git tag / GitHub release** of this repo (e.g. `1.0`).
You can also use:

- `master-SNAPSHOT` — always the latest commit on `master`.
- a commit hash (e.g. `a7c3122`) — a specific commit.

> To publish a version, push a tag on GitHub: `git tag 1.0 && git push origin 1.0`.
> The first JitPack build of a tag takes a minute; check the log at
> `https://jitpack.io/#cykvta/CykutaAPI`.

### Option B — Local Maven install

If you'd rather build it locally, install it into your local Maven repo:

```bash
mvn clean install
```

Then depend on it using its own coordinates:

```xml
<dependency>
    <groupId>icu.cykuta</groupId>
    <artifactId>CykutaAPI</artifactId>
    <version>{version}</version>
</dependency>
```

## Building

```bash
mvn clean package
```

The compiled jar lands in `target/`. It targets the Spigot API declared in
`pom.xml`.

## Quickstart

```java
public class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Commands (no plugin.yml entry needed)
        new CommandRegistry(this).register(new CommandMyPlugin(this));

        // Config
        ConfigManager configs = new ConfigManager(this);
        configs.register("config.yml");

        // Update checker
        new UpdateChecker(this, "cykvta/CykutaAPI").check(result -> {
            if (result.isOutdated()) {
                getLogger().warning("New version: " + result.latestVersion());
            }
        });
    }
}
```

Full API walkthrough for every component: **[USAGE.md](USAGE.md)**.
