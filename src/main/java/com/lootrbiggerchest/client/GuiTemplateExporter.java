package com.lootrbiggerchest.client;

import com.lootrbiggerchest.LootrBiggerChestConfig;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu.ContainerType;
import net.minecraft.server.packs.resources.ResourceProvider;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class GuiTemplateExporter {
    private GuiTemplateExporter() {
    }

    public static Path export(ResourceProvider resources, Path versionDirectory, ContainerType type,
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
        Path output = versionDirectory.resolve("lootrbiggerchest/exports/assets")
                .resolve(texture.getNamespace()).resolve(texture.getPath());
        Files.createDirectories(output.getParent());
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
