package icu.cykuta.api.update;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks a GitHub repository for a newer release than the one the plugin is
 * currently running, using the public GitHub Releases API.
 * <p>
 * The checker only <b>reports</b> the outcome: whether the plugin is outdated and
 * what the latest version is. It does not notify players or the console, keep any
 * config or register any listener — the plugin using the API decides whether, when
 * and how to notify. The request runs asynchronously, so it never blocks the main
 * thread; the callback is delivered back on the main server thread.
 * <pre>{@code
 * new UpdateChecker(this, "cykvta/CykutaAPI", true).check(result -> {
 *     if (result.isOutdated()) {
 *         getLogger().warning("New version available: " + result.latestVersion()
 *                 + " (running " + result.currentVersion() + ")");
 *         getLogger().warning("Download: " + result.downloadUrl());
 *     }
 * });
 * }</pre>
 */
public class UpdateChecker {

    private static final String API_URL = "https://api.github.com/repos/%s/releases/latest";

    private static final Pattern TAG_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern HTML_URL_PATTERN = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"");

    private final JavaPlugin plugin;
    private final String repository;
    private final String currentVersion;
    private boolean enabled;

    /**
     * Creates an enabled checker.
     *
     * @param plugin     The plugin performing the check. Its {@code plugin.yml}
     *                   version is used as the current version.
     * @param repository The GitHub repository in {@code owner/name} form,
     *                   e.g. {@code "cykvta/CykutaAPI"}.
     */
    public UpdateChecker(JavaPlugin plugin, String repository) {
        this(plugin, repository, true);
    }

    /**
     * @param plugin     The plugin performing the check. Its {@code plugin.yml}
     *                   version is used as the current version.
     * @param repository The GitHub repository in {@code owner/name} form,
     *                   e.g. {@code "cykvta/CykutaAPI"}.
     * @param enabled    Whether the check runs. When {@code false}, {@link #check(Consumer)}
     *                   does nothing and the callback is never invoked.
     */
    public UpdateChecker(JavaPlugin plugin, String repository, boolean enabled) {
        this.plugin = plugin;
        this.repository = repository;
        this.currentVersion = plugin.getDescription().getVersion();
        this.enabled = enabled;
    }

    /**
     * Whether the update check is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enable or disable the update check. When disabled, {@link #check(Consumer)}
     * skips the request entirely.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Check for updates asynchronously and hand the outcome to {@code callback}.
     * The callback runs on the main server thread. When the checker is disabled,
     * the request is skipped and the callback is not invoked.
     *
     * @param callback Receives the {@link Result} once the check completes.
     */
    public void check(Consumer<Result> callback) {
        if (!enabled) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Result result = fetch();
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }

    private Result fetch() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(String.format(API_URL, repository)).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", plugin.getName() + "-UpdateChecker");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return Result.failed(currentVersion);
            }

            String body;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                body = sb.toString();
            }

            String latestTag = extract(TAG_PATTERN, body);
            if (latestTag == null) {
                return Result.failed(currentVersion);
            }
            String downloadUrl = extract(HTML_URL_PATTERN, body);

            boolean upToDate = normalize(latestTag).equals(normalize(currentVersion));
            return new Result(upToDate ? Status.UP_TO_DATE : Status.OUTDATED, currentVersion, latestTag, downloadUrl);
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "Update check failed", e);
            return Result.failed(currentVersion);
        }
    }

    @Nullable
    private static String extract(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * Normalize a version so a leading {@code v} does not cause a false mismatch.
     */
    private static String normalize(String version) {
        String trimmed = version.trim();
        if (trimmed.startsWith("v") || trimmed.startsWith("V")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed;
    }

    /**
     * Outcome of an update check.
     */
    public enum Status {
        /** A newer release is available. */
        OUTDATED,
        /** The running version matches the latest release. */
        UP_TO_DATE,
        /** The check could not be completed (network error, no releases, etc.). */
        FAILED
    }

    /**
     * The result of an update check.
     *
     * @param status         The outcome of the check.
     * @param currentVersion The version the plugin is running.
     * @param latestVersion  The latest release tag, or {@code null} if the check failed.
     * @param downloadUrl    The release page URL, or {@code null} if unavailable.
     */
    public record Result(Status status, String currentVersion,
                         @Nullable String latestVersion, @Nullable String downloadUrl) {

        static Result failed(String currentVersion) {
            return new Result(Status.FAILED, currentVersion, null, null);
        }

        /**
         * @return {@code true} if a newer version is available.
         */
        public boolean isOutdated() {
            return status == Status.OUTDATED;
        }
    }
}
