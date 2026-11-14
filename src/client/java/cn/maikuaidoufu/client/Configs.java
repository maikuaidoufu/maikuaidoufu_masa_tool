package cn.maikuaidoufu.client;

import cn.maikuaidoufu.MasaToolMod;
import cn.maikuaidoufu.client.gui.GuiGroupList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.gui.GuiBase;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public final class Configs implements IConfigHandler
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = MasaToolMod.MOD_ID + ".json";

    public static final ConfigHotkey OPEN_GUI = new ConfigHotkey(
        "openGui",
        "C,F",
        "",
        "maikuaidoufu_masa_tool.config.name.open_gui",
        "maikuaidoufu_masa_tool.config.name.open_gui");
    public static final ConfigBoolean RENDER_HIGHLIGHTS = new ConfigBoolean("renderHighlights", true);
    public static final ConfigBoolean VERIFIER_EXPORT = new ConfigBoolean(
        "verifierExport",
        true,
        "maikuaidoufu_masa_tool.config.comment.verifier_export",
        "maikuaidoufu_masa_tool.config.name.verifier_export",
        "maikuaidoufu_masa_tool.config.name.verifier_export");
    public static final ConfigBoolean XRAY_HIGHLIGHTS = new ConfigBoolean(
        "xrayHighlights",
        false,
        "maikuaidoufu_masa_tool.config.comment.xray_render",
        "maikuaidoufu_masa_tool.config.name.xray_render",
        "maikuaidoufu_masa_tool.config.name.xray_render");
    public static final ConfigInteger LOG_CACHE_SIZE = new ConfigInteger(
        "logCacheSize",
        1000,
        100,
        10000,
        "maikuaidoufu_masa_tool.config.comment.log_cache_size",
        "maikuaidoufu_masa_tool.config.name.log_cache_size",
        "maikuaidoufu_masa_tool.config.name.log_cache_size");

    public static final List<IConfigBase> OPTIONS =
        List.of(RENDER_HIGHLIGHTS, XRAY_HIGHLIGHTS, VERIFIER_EXPORT, LOG_CACHE_SIZE);

    private Configs()
    {
    }

    public static void init()
    {
        Configs handler = new Configs();
        ConfigManager.getInstance().registerConfigHandler(MasaToolMod.MOD_ID, handler);
        handler.load();

        OPEN_GUI.getKeybind().setCallback((IHotkeyCallback) (action, keybind) ->
        {
            if (action == KeyAction.PRESS)
            {
                Minecraft mc = Minecraft.getInstance();

                if (mc.screen == null)
                {
                    GuiBase.openGui(new GuiGroupList());
                }

                return true;
            }

            return false;
        });

        InputEventHandler.getKeybindManager().registerKeybindProvider(new IKeybindProvider()
        {
            @Override
            public void addKeysToMap(IKeybindManager manager)
            {
                manager.addKeybindToMap(OPEN_GUI.getKeybind());
            }

            @Override
            public void addHotkeys(IKeybindManager manager)
            {
                manager.addHotkeysForCategory("Doufu's Masa Tool", "main", List.of(OPEN_GUI));
            }
        });
    }

    @Override
    public void load()
    {
        Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);

        if (!Files.exists(file))
        {
            return;
        }

        try
        {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            ConfigUtils.readConfigBase(root, "generic", OPTIONS);
            ConfigUtils.readHotkeys(root, "hotkeys", List.of(OPEN_GUI));
        }
        catch (Exception e)
        {
            MasaToolMod.LOGGER.warn("Failed to load client config: {}", e.getMessage());
        }
    }

    public static void saveConfig()
    {
        new Configs().save();
    }

    @Override
    public void save()
    {
        Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        JsonObject root = new JsonObject();
        ConfigUtils.writeConfigBase(root, "generic", OPTIONS);
        ConfigUtils.writeHotkeys(root, "hotkeys", List.of(OPEN_GUI));

        try
        {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            MasaToolMod.LOGGER.warn("Failed to save client config: {}", e.getMessage());
        }
    }
}
