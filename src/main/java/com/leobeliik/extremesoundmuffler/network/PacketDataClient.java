package com.leobeliik.extremesoundmuffler.network;

import com.leobeliik.extremesoundmuffler.interfaces.IAnchorList;
import com.leobeliik.extremesoundmuffler.utils.Anchor;
import com.leobeliik.extremesoundmuffler.utils.DataManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.network.NetworkEvent;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class PacketDataClient implements IAnchorList {

    private final CompoundTag data;

    PacketDataClient(FriendlyByteBuf buf) {
        data = buf.readNbt();
    }

    public PacketDataClient(CompoundTag data) {
        this.data = data;
    }

    void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    @SuppressWarnings("SameReturnValue")
    boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> IntStream.rangeClosed(0, 9).forEach(i -> {
            if (!data.contains("anchor" + i)) {
                anchorList.add(i, new Anchor(i, "Anchor: " + i));
            } else {
                anchorList.add(i, DataManager.deserializeNBT(data.getCompound("anchor" + i)));
            }
        }));
        return true;
    }
}