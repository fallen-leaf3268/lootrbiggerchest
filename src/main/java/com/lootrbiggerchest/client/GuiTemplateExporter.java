package com.lootrbiggerchest.client;

import com.lootrbiggerchest.LootrBiggerChestConfig;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu.ContainerType;
import net.minecraft.server.packs.resources.ResourceProvider;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class GuiTemplateExporter {
    private GuiTemplateExporter() {
    }

    public static Path export(ResourceProvider resources, Path packRoot, ContainerType type,
                              int rows, int cols) throws IOException {
        if (!LootrBiggerChestConfig.isValidSize(rows, cols)) {
            throw new IllegalArgumentException("Invalid container size: " + rows + "x" + cols);
        }
        BufferedImage source;
        try (InputStream input = resources.open(ContainerGuiBackground.TEXTURE)) {
            source = ImageIO.read(input);
        }
        if (source == null) {
            throw new IOException("Cannot decode " + ContainerGuiBackground.TEXTURE);
        }
        BufferedImage image = ContainerGuiBackground.createImage(source, rows, cols);
        var texture = ContainerGuiTextures.location(type, rows, cols);
        Path output = packRoot.resolve("assets").resolve(texture.getNamespace()).resolve(texture.getPath());
        Files.createDirectories(output.getParent());
        Path metadata = packRoot.resolve("pack.mcmeta");
        if (Files.notExists(metadata)) {
            Files.writeString(metadata,
                    "{\"pack\":{\"pack_format\":15,\"description\":\"Lootr Bigger Chest UI templates\"}}\n",
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        }
        boolean created = false;
        try {
            try (OutputStream stream = Files.newOutputStream(output, StandardOpenOption.CREATE_NEW)) {
                created = true;
                if (!ImageIO.write(image, "PNG", stream)) {
                    throw new IOException("PNG writer is unavailable");
                }
            }
        } catch (IOException | RuntimeException failure) {
            if (created) {
                try {
                    Files.deleteIfExists(output);
                } catch (IOException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        }
        return output;
    }
}
