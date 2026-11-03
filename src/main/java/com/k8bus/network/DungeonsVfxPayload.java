package com.k8bus.network;

import com.k8bus.UnnamedDungeons;
import com.k8bus.vfx.VfxTheme;
import com.k8bus.vfx.VfxType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public record DungeonsVfxPayload(
        byte typeOrdinal,
        byte themeOrdinal,
        float x, float y, float z,
        float dx, float dy, float dz,
        float x2, float y2, float z2,
        float scale,
        int durationTicks,
        int targetEntityId
) implements CustomPacketPayload {

    public static final Type<DungeonsVfxPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(UnnamedDungeons.MOD_ID, "dungeons_vfx")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, DungeonsVfxPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeByte(p.typeOrdinal);
                buf.writeByte(p.themeOrdinal);
                buf.writeFloat(p.x);
                buf.writeFloat(p.y);
                buf.writeFloat(p.z);
                buf.writeFloat(p.dx);
                buf.writeFloat(p.dy);
                buf.writeFloat(p.dz);
                buf.writeFloat(p.x2);
                buf.writeFloat(p.y2);
                buf.writeFloat(p.z2);
                buf.writeFloat(p.scale);
                buf.writeInt(p.durationTicks);
                buf.writeInt(p.targetEntityId);
            },
            buf -> new DungeonsVfxPayload(
                    buf.readByte(),
                    buf.readByte(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readInt(),
                    buf.readInt()
            )
    );

    public VfxType getVfxType() {
        VfxType[] vals = VfxType.values();
        int idx = typeOrdinal & 0xFF;
        return idx < vals.length ? vals[idx] : VfxType.IMPACT_BURST;
    }

    public VfxTheme getVfxTheme() {
        VfxTheme[] vals = VfxTheme.values();
        int idx = themeOrdinal & 0xFF;
        return idx < vals.length ? vals[idx] : VfxTheme.FIRE;
    }

    public Vec3 getPos() {
        return new Vec3(x, y, z);
    }

    public Vec3 getDirection() {
        return new Vec3(dx, dy, dz);
    }

    public Vec3 getSecondaryPos() {
        return new Vec3(x2, y2, z2);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
