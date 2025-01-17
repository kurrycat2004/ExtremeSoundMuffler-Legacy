package com.leobeliik.extremesoundmuffler.utils;

import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.DimensionManager;

import com.leobeliik.extremesoundmuffler.interfaces.ISoundLists;

public class Anchor {

    private final int id;
    private String name;
    private String dimension;
    private int radius;
    private SortedMap<String, Float> muffledSounds = new TreeMap<>();
    private Vec3 anchorPos;

    public Anchor(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public Anchor(int id, String name, Vec3 anchorPos, String dimension, int radius,
        SortedMap<String, Float> muffledSounds) {
        this.id = id;
        this.name = name;
        this.anchorPos = anchorPos;
        this.dimension = dimension;
        this.radius = radius;
        this.muffledSounds = muffledSounds;
    }

    public Vec3 getAnchorPos() {
        return anchorPos;
    }

    private void setAnchorPos(int x, int y, int z) {
        anchorPos = Vec3.createVectorHelper(x, y, z);
    }

    public int getAnchorId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int Radius) {
        this.radius = Radius;
    }

    private void setName(String name) {
        this.name = name;
    }

    public SortedMap<ComparableResource, Float> getMuffledSounds() {
        SortedMap<ComparableResource, Float> temp = new TreeMap<>();
        this.muffledSounds.forEach((R, F) -> temp.put(new ComparableResource(R), F));
        return temp;
    }

    public void setMuffledSounds(SortedMap<ResourceLocation, Float> muffledSounds) {
        muffledSounds.forEach((R, F) -> this.muffledSounds.put(R.toString(), F));
    }

    public void addSound(ResourceLocation sound, float volume) {
        muffledSounds.put(sound.toString(), volume);
    }

    public void replaceSound(ResourceLocation sound, float volume) {
        muffledSounds.replace(sound.toString(), volume);
    }

    public int getX() {
        return anchorPos == null ? 0 : (int) anchorPos.xCoord;
    }

    public int getY() {
        return anchorPos == null ? 0 : (int) anchorPos.yCoord;
    }

    public int getZ() {
        return anchorPos == null ? 0 : (int) anchorPos.zCoord;
    }

    public String getDimension() {
        return dimension;
    }

    private void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public void removeSound(ResourceLocation sound) {
        muffledSounds.remove(sound.toString());
    }

    public void setAnchor() {
        EntityClientPlayerMP player = Objects.requireNonNull(Minecraft.getMinecraft().thePlayer);
        setAnchorPos((int) player.posX, (int) player.posY, (int) player.posZ);
        setDimension(
            DimensionManager.getProvider(player.dimension)
                .getDimensionName());
        setRadius(this.getRadius() == 0 ? 32 : this.getRadius());
    }

    public void deleteAnchor() {
        setName("Anchor: " + this.getAnchorId());
        anchorPos = null;
        setDimension(null);
        setRadius(0);
        muffledSounds.clear();
    }

    public void editAnchor(String title, int Radius) {
        setName(title);
        setRadius(Radius);
    }

    public static Anchor getAnchor(ISound sound) {
        Vec3 soundPos = Vec3.createVectorHelper(sound.getXPosF(), sound.getYPosF(), sound.getZPosF());
        for (Anchor anchor : ISoundLists.anchorList) {
            WorldClient world = Minecraft.getMinecraft().theWorld;
            if (anchor.getAnchorPos() != null && world != null
                && world.provider.getDimensionName()
                    .equals(anchor.getDimension())
                && soundPos.distanceTo(anchor.getAnchorPos()) < anchor.getRadius()
                && anchor.getMuffledSounds()
                    .containsKey(new ComparableResource(sound.getPositionedSoundLocation()))) {
                return anchor;
            }
        }
        return null;
    }
}
