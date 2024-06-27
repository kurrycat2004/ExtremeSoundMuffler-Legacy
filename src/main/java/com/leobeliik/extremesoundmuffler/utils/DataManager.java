package com.leobeliik.extremesoundmuffler.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.leobeliik.extremesoundmuffler.Config;
import com.leobeliik.extremesoundmuffler.interfaces.ISoundLists;

import cpw.mods.fml.common.FMLCommonHandler;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class DataManager implements ISoundLists {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting()
        .create();

    public static void loadData() {
        loadMuffledMap().forEach((R, F) -> muffledSounds.put(new ComparableResource(R), F));
        if (!Config.getDisableAchors()) {
            anchorList.clear();
            anchorList.addAll(loadAnchors());
        }
    }

    public static void saveData() {
        saveMuffledMap();

        if (!Config.getDisableAchors()) {
            saveAnchors();
        }
    }

    private static String getWorldName() {
        MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        return "test";
        // if (server != null) {
        // return server.getWorldName();
        //// } else if (Minecraft.getMinecraft().isSingleplayer()) {
        //// return server.getWorldName();
        // } else {
        // return "ServerWorld";
        // }
    }

    private static NBTTagCompound serializeAnchor(Anchor anchor) {

        NBTTagCompound anchorNBT = new NBTTagCompound();
        NBTTagCompound muffledNBT = new NBTTagCompound();

        anchorNBT.setInteger("ID", anchor.getAnchorId());
        anchorNBT.setString("NAME", anchor.getName());

        if (anchor.getAnchorPos() == null) {
            return anchorNBT;
        }

        anchorNBT.setInteger("X", (int) anchor.getAnchorPos().xCoord);
        anchorNBT.setInteger("Y", (int) anchor.getAnchorPos().yCoord);
        anchorNBT.setInteger("Z", (int) anchor.getAnchorPos().zCoord);
        anchorNBT.setString("DIM", anchor.getDimension());
        anchorNBT.setInteger("RAD", anchor.getRadius());
        anchor.getMuffledSounds()
            .forEach((R, F) -> muffledNBT.setFloat(R.toString(), F));
        anchorNBT.setTag("MUFFLED", muffledNBT);

        return anchorNBT;
    }

    public static Anchor deserializeAnchor(NBTTagCompound nbt) {
        SortedMap<String, Float> muffledSounds = new TreeMap<>();
        NBTTagCompound muffledNBT = nbt.getCompoundTag("MUFFLED");

        for (String key : muffledNBT.func_150296_c()) {
            muffledSounds.put(key, muffledNBT.getFloat(key));
        }

        if (!nbt.hasKey("POS")) {
            return new Anchor(nbt.getInteger("ID"), nbt.getString("NAME"));
        } else {
            return new Anchor(
                nbt.getInteger("ID"),
                nbt.getString("NAME"),
                nbt.getInteger("X"),
                nbt.getInteger("Y"),
                nbt.getInteger("Z"),
                nbt.getString("DIM"),
                nbt.getInteger("RAD"),
                muffledSounds);
        }
    }

    private static void saveMuffledMap() {
        new File("ESM/").mkdir();
        try (Writer writer = new OutputStreamWriter(
            new FileOutputStream("ESM/soundsMuffled.dat"),
            StandardCharsets.UTF_8)) {
            writer.write(gson.toJson(muffledSounds));
        } catch (IOException ignored) {}
    }

    private static Map<String, Float> loadMuffledMap() {
        try (InputStreamReader reader = new InputStreamReader(
            new FileInputStream("ESM/soundsMuffled.dat"),
            StandardCharsets.UTF_8)) {
            return gson.fromJson(new JsonReader(reader), new TypeToken<Map<String, Float>>() {}.getType());
        } catch (JsonSyntaxException | IOException e) {
            return new HashMap<>();
        }
    }

    private static void saveAnchors() {
        new File("ESM/", getWorldName()).mkdirs();
        try (Writer writer = new OutputStreamWriter(
            new FileOutputStream("ESM/" + getWorldName() + "/anchors.dat"),
            StandardCharsets.UTF_8)) {
            writer.write(gson.toJson(anchorList));
        } catch (IOException ignored) {}
    }

    private static List<Anchor> loadAnchors() {
        try (InputStreamReader reader = new InputStreamReader(
            new FileInputStream("ESM/" + getWorldName() + "/anchors.dat"),
            StandardCharsets.UTF_8)) {
            return gson.fromJson(new JsonReader(reader), new TypeToken<List<Anchor>>() {}.getType());
        } catch (JsonSyntaxException | IOException ignored) {
            return IntStream.range(0, 10)
                .mapToObj(i -> new Anchor(i, "Anchor " + i))
                .collect(Collectors.toList());
        }
    }
}
