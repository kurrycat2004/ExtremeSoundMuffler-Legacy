package com.leobeliik.extremesoundmuffler;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.client.IModGuiFactory;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;

public class GuiFactory implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraftInstance) {}

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return GuiESMConfig.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }

    public static class GuiESMConfig extends GuiConfig {

        public GuiESMConfig(GuiScreen parent) {
            super(
                Minecraft.getMinecraft().currentScreen,
                getConfigElements(),
                SoundMuffler.MODID,
                false,
                false,
                SoundMuffler.MODNAME + " Configuration");
        }

        private static List<IConfigElement> getConfigElements() {
            Configuration config = Config.config;
            return config.getCategoryNames()
                .stream()
                .map(name -> new ConfigElement<>(config.getCategory(name)))
                .collect(Collectors.toList());
        }
    }
}
