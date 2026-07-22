package icu.cykuta.api.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;

/**
 * Registers commands directly into the server's {@link CommandMap}, so plugins
 * don't need to declare them in their {@code plugin.yml}.
 * <p>
 * Bind it to your plugin and register your {@link BaseCommand}s from {@code onEnable}:
 * <pre>{@code
 * CommandRegistry registry = new CommandRegistry(this);
 * registry.register(new CommandMyPlugin(this));
 * }</pre>
 */
public class CommandRegistry {

    private final JavaPlugin plugin;

    public CommandRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Get the CommandMap of the server.
     */
    public static CommandMap getCommandMap() {
        try {
            return (CommandMap) Bukkit.getServer().getClass().getDeclaredMethod("getCommandMap")
                    .invoke(Bukkit.getServer());
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Register a command to the server. The plugin name is used as the fallback prefix.
     */
    public void register(Command command) {
        getCommandMap().register(plugin.getName().toLowerCase(), command);
    }

    /**
     * Register several commands to the server.
     */
    public void register(Command... commands) {
        for (Command command : commands) {
            register(command);
        }
    }
}
