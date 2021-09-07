package xyz.tehbrian.buildersutilities;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import xyz.tehbrian.buildersutilities.armorcolor.ArmorColorInventoryListener;
import xyz.tehbrian.buildersutilities.banner.listener.BannerBaseInventoryListener;
import xyz.tehbrian.buildersutilities.banner.listener.BannerColorInventoryListener;
import xyz.tehbrian.buildersutilities.banner.listener.BannerPatternInventoryListener;
import xyz.tehbrian.buildersutilities.command.*;
import xyz.tehbrian.buildersutilities.config.ConfigConfig;
import xyz.tehbrian.buildersutilities.config.LangConfig;
import xyz.tehbrian.buildersutilities.inject.*;
import xyz.tehbrian.buildersutilities.option.*;
import xyz.tehbrian.buildersutilities.setting.SettingsListener;
import xyz.tehbrian.restrictionhelper.spigot.SpigotRestrictionHelper;
import xyz.tehbrian.restrictionhelper.spigot.SpigotRestrictionLoader;
import xyz.tehbrian.restrictionhelper.spigot.restrictions.R_PlotSquared_5_13;
import xyz.tehbrian.restrictionhelper.spigot.restrictions.R_WorldGuard_7_0_4;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * The main class for the BuildersUtilities plugin.
 */
public final class BuildersUtilities extends JavaPlugin {

    /**
     * The Guice injector.
     */
    private @MonotonicNonNull Injector injector;

    /**
     * Called when the plugin is enabled.
     */
    @Override
    public void onEnable() {
        try {
            this.injector = Guice.createInjector(
                    new ArmorColorModule(),
                    new BannerModule(),
                    new ConfigModule(),
                    new OptionsModule(),
                    new PluginModule(this),
                    new RestrictionHelperModule(),
                    new UserModule()
            );
        } catch (final Exception e) {
            this.getLogger().severe("Something went wrong while creating the Guice injector.");
            this.getLogger().severe("Disabling plugin.");
            this.getLogger().log(Level.SEVERE, "Printing stack trace, please send this to the developers:", e);
            this.disableSelf();
            return;
        }

        this.loadConfigs();
        this.setupListeners();
        this.setupCommands();
        this.setupRestrictions();

        this.injector.getInstance(NoClipManager.class).start();
    }

    /**
     * Loads the various plugin config files.
     */
    public void loadConfigs() {
        this.saveResource("config.yml", false);
        this.saveResource("lang.yml", false);

        this.injector.getInstance(ConfigConfig.class).load();
        this.injector.getInstance(LangConfig.class).load();
    }

    private void setupListeners() {
        registerListeners(
                Key.get(BannerBaseInventoryListener.class),
                Key.get(BannerColorInventoryListener.class),
                Key.get(BannerPatternInventoryListener.class),
                Key.get(ArmorColorInventoryListener.class),
                Key.get(OptionsInventoryListener.class),
                Key.get(AdvancedFlyListener.class),
                Key.get(DoubleSlabListener.class),
                Key.get(GlazedTerracottaListener.class),
                Key.get(IronDoorListener.class),
                Key.get(SettingsListener.class)
        );
    }

    @SafeVarargs
    private void registerListeners(final Key<? extends Listener>... listeners) {
        final PluginManager pm = this.getServer().getPluginManager();

        for (final Key<? extends Listener> listener : listeners) {
            final Listener instance = this.injector.getInstance(listener);
            pm.registerEvents(instance, this);
        }
    }

    private void setupCommands() {
        final Map<String, Key<? extends CommandExecutor>> toRegister = new HashMap<>();

        toRegister.put("advancedfly", Key.get(AdvancedFlyCommand.class));
        toRegister.put("armorcolor", Key.get(ArmorColorCommand.class));
        toRegister.put("banner", Key.get(BannerCommand.class));
        toRegister.put("nightvision", Key.get(NightVisionCommand.class));
        toRegister.put("noclip", Key.get(NoClipCommand.class));

        this.registerCommandsWithEmptyTabCompleter(toRegister);

        final var buildersUtilitiesCommand = this.injector.getInstance(BuildersUtilitiesCommand.class);
        this.getCommand("buildersutilities").setExecutor(buildersUtilitiesCommand);
        this.getCommand("buildersutilities").setTabCompleter(buildersUtilitiesCommand);
    }

    private void registerCommandsWithEmptyTabCompleter(final Map<String, Key<? extends CommandExecutor>> commands) {
        final EmptyTabCompleter emptyTabCompleter = new EmptyTabCompleter();

        for (final String commandName : commands.keySet()) {
            final PluginCommand command = this.getCommand(commandName);

            final CommandExecutor instance = this.injector.getInstance(commands.get(commandName));
            command.setExecutor(instance);
            command.setTabCompleter(emptyTabCompleter);
        }
    }

    private void setupRestrictions() {
        final var restrictionHelper = this.injector.getInstance(SpigotRestrictionHelper.class);

        final PluginManager pm = this.getServer().getPluginManager();

        final var loader = new SpigotRestrictionLoader(
                this.getLog4JLogger(),
                Arrays.asList(pm.getPlugins()),
                List.of(R_PlotSquared_5_13.class, R_WorldGuard_7_0_4.class)
        );

        loader.load(restrictionHelper);
    }

    /**
     * Disables this plugin.
     */
    public void disableSelf() {
        this.getServer().getPluginManager().disablePlugin(this);
    }

}
