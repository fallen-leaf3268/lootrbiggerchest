package com.lootrbiggerchest.client;

import com.lootrbiggerchest.LootrBiggerChest;
import com.lootrbiggerchest.LootrBiggerChestConfig;
import com.lootrbiggerchest.menu.LootrBiggerChestMenu.ContainerType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.Locale;

public final class ClientGuiCommands {
    private static final DynamicCommandExceptionType EXPORT_ERROR =
            new DynamicCommandExceptionType(message -> Component.literal(message.toString()));

    private ClientGuiCommands() {}

    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Path versionDirectory = minecraft.gameDirectory.toPath();
        register(event.getDispatcher(), (type, rows, cols) ->
                GuiTemplateExporter.export(minecraft.getResourceManager(), versionDirectory, type, rows, cols));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, ExportAction action) {
        var export = Commands.literal("export");
        for (ContainerType type : ContainerType.values()) {
            export.then(Commands.literal(type.name().toLowerCase(Locale.ROOT))
                    .then(Commands.argument("rows", IntegerArgumentType.integer(1, LootrBiggerChestConfig.MAX_ROWS))
                            .then(Commands.argument("cols", IntegerArgumentType.integer(1, LootrBiggerChestConfig.MAX_COLUMNS))
                                    .executes(context -> export(context, action, type)))));
        }
        dispatcher.register(Commands.literal("lootrbiggerchest")
                .then(Commands.literal("ui").then(export)));
    }

    private static int export(CommandContext<CommandSourceStack> context, ExportAction action,
                              ContainerType type) throws CommandSyntaxException {
        int rows = IntegerArgumentType.getInteger(context, "rows");
        int cols = IntegerArgumentType.getInteger(context, "cols");
        try {
            Path path = action.export(type, rows, cols).toAbsolutePath();
            Component file = Component.literal(path.toString()).withStyle(style ->
                    style.withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE,
                            path.getParent().toString())));
            context.getSource().sendSuccess(() -> Component.literal("已导出 UI 背景：").append(file), false);
            return 1;
        } catch (FileAlreadyExistsException exception) {
            throw EXPORT_ERROR.create("文件已存在，未覆盖：" + exception.getFile());
        } catch (IOException exception) {
            LootrBiggerChest.LOGGER.warn("Unable to export {} GUI {}x{}", type, rows, cols, exception);
            throw EXPORT_ERROR.create("导出 UI 失败：" + exception.getMessage());
        }
    }

    @FunctionalInterface
    public interface ExportAction {
        Path export(ContainerType type, int rows, int cols) throws IOException;
    }
}
