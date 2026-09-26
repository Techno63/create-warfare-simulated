package com.julian.createwarfare.registry;

import com.julian.createwarfare.commands.RadiationCommand;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class CWCommands {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher =
                event.getDispatcher();

        dispatcher.register(
                Commands.literal("cw")
                        .then(
                                //--------------------------------------------------------------
                                Commands.literal("radiation")
                                        .then(
                                                //----------------------------------------------
                                                Commands.literal("clear")
                                                        .requires(
                                                                source ->
                                                                        source.hasPermission(2)
                                                        )
                                                        .executes(
                                                                context -> {
                                                                    RadiationCommand.clear(
                                                                            context.getSource()
                                                                    );

                                                                    return 1;
                                                                }
                                                        )
                                                //----------------------------------------------
                                        )
                                //--------------------------------------------------------------
                        )
        );
    }
}