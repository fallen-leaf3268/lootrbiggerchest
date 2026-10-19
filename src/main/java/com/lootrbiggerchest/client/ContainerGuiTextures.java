package com.lootrbiggerchest.client;

import com.lootrbiggerchest.LootrBiggerChest;
import com.lootrbiggerchest.LootrBiggerChestConfig;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu.ContainerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class ContainerGuiTextures implements ResourceManagerReloadListener {
    public static final ContainerGuiTextures INSTANCE = new ContainerGuiTextures();
    private static final Pattern FILE_NAME = Pattern.compile(
            "textures/gui/(chest|barrel|shulker|minecart)/([1-9][0-9]?)x([1-9][0-9]?)\\.png");

    private volatile Map<Key, ResourceLocation> textures = Map.of();

    public static ResourceLocation location(ContainerType type, int rows, int cols) {
        return new ResourceLocation(LootrBiggerChest.MOD_ID, "textures/gui/"
                + type.name().toLowerCase(Locale.ROOT) + "/" + rows + "x" + cols + ".png");
    }

    public Optional<ResourceLocation> find(ContainerType type, int rows, int cols) {
        return Optional.ofNullable(textures.get(new Key(type, rows, cols)));
    }

    @Override
    public void onResourceManagerReload(ResourceManager resources) {
        Map<Key, ResourceLocation> loaded = new HashMap<>();
        resources.listResources("textures/gui", id -> id.getNamespace().equals(LootrBiggerChest.MOD_ID)
                && id.getPath().endsWith(".png")).forEach((id, resource) -> {
            var match = FILE_NAME.matcher(id.getPath());
            if (!match.matches()) return;
            ContainerType type = ContainerType.valueOf(match.group(1).toUpperCase(Locale.ROOT));
            int rows = Integer.parseInt(match.group(2));
            int cols = Integer.parseInt(match.group(3));
            if (!LootrBiggerChestConfig.isValidSize(rows, cols)) return;
            int width = ContainerGuiBackground.width(cols);
            int height = ContainerGuiBackground.height(rows);
            try (InputStream stream = resource.open()) {
                BufferedImage image = ImageIO.read(stream);
                if (image == null) throw new IOException("Invalid PNG image");
                if (image.getWidth() != width || image.getHeight() != height) {
                    LootrBiggerChest.LOGGER.warn("Ignoring GUI texture {}: expected {}x{}, found {}x{}",
                            id, width, height, image.getWidth(), image.getHeight());
                    return;
                }
                loaded.put(new Key(type, rows, cols), id);
            } catch (IOException exception) {
                LootrBiggerChest.LOGGER.warn("Unable to load GUI texture {}", id, exception);
            }
        });
        textures = Map.copyOf(loaded);
    }

    private record Key(ContainerType type, int rows, int cols) {}
}
