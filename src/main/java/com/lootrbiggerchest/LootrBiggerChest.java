package com.lootrbiggerchest;

import com.lootrbiggerchest.client.LootrBiggerChestScreen;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu.ContainerType;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.extensions.IForgeMenuType;
import noobanidus.mods.lootr.block.entities.LootrBarrelBlockEntity;
import noobanidus.mods.lootr.block.entities.LootrShulkerBlockEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Mod(LootrBiggerChest.MOD_ID)
public class LootrBiggerChest {

    public static final String MOD_ID = "lootrbiggerchest";

    private static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);

    public static final ConcurrentHashMap<Integer, int[]> PENDING_MENU_SIZES = new ConcurrentHashMap<>();

    public static final RegistryObject<MenuType<LootrBiggerChestMenu>> CHEST_MENU =
            MENU_TYPES.register("lootr_chest", () ->
                    IForgeMenuType.create((id, inv, data) ->
                            LootrBiggerChestMenu.fromNetwork(id, inv, data, ContainerType.CHEST)));

    public static final RegistryObject<MenuType<LootrBiggerChestMenu>> BARREL_MENU =
            MENU_TYPES.register("lootr_barrel", () ->
                    IForgeMenuType.create((id, inv, data) ->
                            LootrBiggerChestMenu.fromNetwork(id, inv, data, ContainerType.BARREL)));

    public static final RegistryObject<MenuType<LootrBiggerChestMenu>> SHULKER_MENU =
            MENU_TYPES.register("lootr_shulker", () ->
                    IForgeMenuType.create((id, inv, data) ->
                            LootrBiggerChestMenu.fromNetwork(id, inv, data, ContainerType.SHULKER)));

    public static final RegistryObject<MenuType<LootrBiggerChestMenu>> MINECART_MENU =
            MENU_TYPES.register("lootr_minecart", () ->
                    IForgeMenuType.create((id, inv, data) ->
                            LootrBiggerChestMenu.fromNetwork(id, inv, data, ContainerType.MINECART)));

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MOD_ID, "menu_size"),
            () -> "1",
            "1"::equals,
            "1"::equals
    );

    public static final ConcurrentHashMap<Integer, int[]> CLIENT_SIZES = new ConcurrentHashMap<>();

    public LootrBiggerChest() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, LootrBiggerChestConfig.COMMON_SPEC);
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        MENU_TYPES.register(modBus);
        modBus.addListener(this::onClientSetup);

        CHANNEL.registerMessage(0, SyncSizePacket.class,
                SyncSizePacket::encode,
                SyncSizePacket::decode,
                SyncSizePacket::handle);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(CHEST_MENU.get(), LootrBiggerChestScreen::new);
            MenuScreens.register(BARREL_MENU.get(), LootrBiggerChestScreen::new);
            MenuScreens.register(SHULKER_MENU.get(), LootrBiggerChestScreen::new);
            MenuScreens.register(MINECART_MENU.get(), LootrBiggerChestScreen::new);
        });
    }

    public static void sendMenuSize(ServerPlayer player, int containerId, int rows, int cols) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncSizePacket(containerId, rows, cols));
    }

    public record SyncSizePacket(int containerId, int rows, int cols) {
        public static void encode(SyncSizePacket msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.containerId);
            buf.writeInt(msg.rows);
            buf.writeInt(msg.cols);
        }

        public static SyncSizePacket decode(FriendlyByteBuf buf) {
            return new SyncSizePacket(buf.readInt(), buf.readInt(), buf.readInt());
        }

        public static void handle(SyncSizePacket msg, Supplier<NetworkEvent.Context> ctx) {
            if (CLIENT_SIZES.size() > 100) {
                Iterator<Integer> it = CLIENT_SIZES.keySet().iterator();
                for (int i = 0; i < 25 && it.hasNext(); i++) {
                    it.next();
                    it.remove();
                }
            }
            CLIENT_SIZES.put(msg.containerId, new int[]{msg.rows, msg.cols});
            ctx.get().setPacketHandled(true);
        }
    }

    public static MenuType<LootrBiggerChestMenu> getMenu(ContainerType type) {
        return switch (type) {
            case CHEST -> CHEST_MENU.get();
            case BARREL -> BARREL_MENU.get();
            case SHULKER -> SHULKER_MENU.get();
            case MINECART -> MINECART_MENU.get();
        };
    }

    public static final String ROWS_KEY = "LBCRows";
    public static final String COLS_KEY = "LBCCols";

    public static int[] resolveOrCreateSize(CompoundTag data, java.util.function.Supplier<int[]> pickSize) {
        if (data.contains(ROWS_KEY))
            return new int[]{data.getInt(ROWS_KEY), data.getInt(COLS_KEY)};
        int[] size = pickSize.get();
        data.putInt(ROWS_KEY, size[0]);
        data.putInt(COLS_KEY, size[1]);
        return size;
    }

    public static void saveSizeToTag(CompoundTag persistentData, CompoundTag saveTag) {
        if (persistentData.contains(ROWS_KEY)) {
            saveTag.putInt(ROWS_KEY, persistentData.getInt(ROWS_KEY));
            saveTag.putInt(COLS_KEY, persistentData.getInt(COLS_KEY));
        }
    }

    public static void loadSizeFromTag(CompoundTag saveTag, CompoundTag persistentData) {
        if (saveTag.contains(ROWS_KEY)) {
            persistentData.putInt(ROWS_KEY, saveTag.getInt(ROWS_KEY));
            persistentData.putInt(COLS_KEY, saveTag.getInt(COLS_KEY));
        }
    }

    public static int[] getOrCreateSize(BlockEntity be, Entity entity) {
        if (!LootrBiggerChestConfig.isExpanded()) return new int[]{3, 9};
        try {
            if (be != null) {
                if (be instanceof LootrBarrelBlockEntity)
                    return resolveOrCreateSize(be.getPersistentData(), LootrBiggerChestConfig::pickBarrelSize);
                if (be instanceof LootrShulkerBlockEntity)
                    return resolveOrCreateSize(be.getPersistentData(), LootrBiggerChestConfig::pickShulkerSize);
                return resolveOrCreateSize(be.getPersistentData(), LootrBiggerChestConfig::pickChestSize);
            }
            if (entity != null) {
                return resolveOrCreateSize(entity.getPersistentData(), LootrBiggerChestConfig::pickMinecartSize);
            }
        } catch (Exception ignored) {}
        return new int[]{3, 9};
    }
}
