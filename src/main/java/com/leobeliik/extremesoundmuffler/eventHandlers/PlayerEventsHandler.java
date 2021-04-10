package com.leobeliik.extremesoundmuffler.eventHandlers;

import com.leobeliik.extremesoundmuffler.SoundMuffler;
import com.leobeliik.extremesoundmuffler.interfaces.IAnchorList;
import com.leobeliik.extremesoundmuffler.network.Network;
import com.leobeliik.extremesoundmuffler.network.PacketDataClient;
import com.leobeliik.extremesoundmuffler.utils.DataManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod.EventBusSubscriber(modid = SoundMuffler.MODID)
public class PlayerEventsHandler implements IAnchorList {

    private static ServerPlayerEntity player;
    private static boolean isClientSide = true;

    @SubscribeEvent
    public static void onPlayerLoggin(PlayerEvent.PlayerLoggedInEvent event) {
        anchorList.clear();
        isClientSide = false;
        player = (ServerPlayerEntity) event.getPlayer();

        if (FMLEnvironment.dist.isDedicatedServer()) {
            CompoundNBT data = new CompoundNBT();
            data.putBoolean("isClientSide", isClientSide);
            Network.sendToClient(new PacketDataClient(data), player);
        }

        if (player == null) {
            DataManager.setAnchors(); //this should never happen
        } else {
            CompoundNBT data = player.getPersistentData();
            Network.sendToClient(new PacketDataClient(data), player);
        }

    }

    @SubscribeEvent
    public static void onPlayerLoggout(PlayerEvent.PlayerLoggedOutEvent event) {
        anchorList.clear();
        isClientSide = true;
        player = null;
    }

    public static boolean isClientSide() {
        return isClientSide;
    }

    public static ServerPlayerEntity getPlayerEntity() {
        return player;
    }

    public static void setClientSide(boolean clientSide) {
        isClientSide = clientSide;
    }

    public static void setPlayerEntity(ServerPlayerEntity playerEntity) {
        player = playerEntity;
    }
}