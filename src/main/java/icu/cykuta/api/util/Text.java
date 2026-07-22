package icu.cykuta.api.util;

import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Text helpers: color translation ({@code &} codes and {@code #rrggbb} hex),
 * color stripping and positional placeholder replacement.
 */
public class Text {

    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

    /**
     * Colorize a string using the {@code &} character for standard codes and
     * {@code #rrggbb} for hex colors.
     *
     * @param message The message to colorize.
     * @return The colorized message, or an empty string if {@code message} is null.
     */
    public static String color(String message) {
        if (message == null) {
            return "";
        }

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            // Translate "#aabbcc" into "&x&a&a&b&b&c&c"
            StringBuilder replacement = new StringBuilder("&x");
            for (char c : matcher.group().substring(1).toCharArray()) {
                replacement.append('&').append(c);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(result);

        return ChatColor.translateAlternateColorCodes('&', result.toString());
    }

    /**
     * Colorize a list of strings.
     */
    public static List<String> color(List<String> messages) {
        return messages.stream().map(Text::color).collect(Collectors.toList());
    }

    /**
     * Strip translated color codes ({@code §x}) from a string.
     *
     * @param message The message to strip color codes from.
     * @return The message without color codes.
     */
    public static String stripColor(String message) {
        return message.replaceAll("§[a-fA-F0-9]", "");
    }

    /**
     * Replace positional placeholders ({@code {0}}, {@code {1}}, ...) in a string.
     *
     * @param text The text to replace placeholders in.
     * @param replacements The replacements, in order.
     * @return The text with the placeholders replaced.
     */
    public static String replace(String text, String... replacements) {
        if (text == null || replacements == null || replacements.length == 0) {
            return text;
        }

        StringBuilder result = new StringBuilder(text);

        for (int i = 0; i < replacements.length; i++) {
            String placeholder = "{" + i + "}";
            String replacement = (replacements[i] != null) ? replacements[i] : "";

            int index;
            while ((index = result.indexOf(placeholder)) != -1) {
                result.replace(index, index + placeholder.length(), replacement);
            }
        }

        return result.toString();
    }

    /**
     * Replace positional placeholders in a list of strings.
     *
     * @param text The list to replace placeholders in.
     * @param replacements The replacements, in order.
     * @return A new list with the placeholders replaced.
     */
    public static List<String> replace(@Nullable List<String> text, String... replacements) {
        if (text == null) {
            return new ArrayList<>();
        }
        return text.stream().map(s -> replace(s, replacements)).collect(Collectors.toList());
    }
}
