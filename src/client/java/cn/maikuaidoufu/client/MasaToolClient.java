package cn.maikuaidoufu.client;

import cn.maikuaidoufu.MasaToolMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class MasaToolClient implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        if (!FabricLoader.getInstance().isModLoaded("malilib"))
        {
            MasaToolMod.LOGGER.error("MaLiLib is required for the client UI; Doufu's Masa Tool client side will stay inactive.");
            return;
        }

        try
        {
            Class<?> clazz = Class.forName("cn.maikuaidoufu.client.MasaToolClientImpl");
            Object instance = clazz.getDeclaredConstructor().newInstance();
            clazz.getMethod("init").invoke(instance);
        }
        catch (Throwable e)
        {
            MasaToolMod.LOGGER.error("Failed to initialize Doufu's Masa Tool client", e);
        }
    }
}
