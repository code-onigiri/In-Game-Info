package com.codeoinigiri.ingameinfo.command;

import com.codeoinigiri.ingameinfo.variable.ExpressionEvaluator;
import com.codeoinigiri.ingameinfo.variable.VariableManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.stream.Collectors;

public class ListVariablesCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("igi")
            .then(Commands.literal("list")
                .executes(context -> {
                    Map<String, String> vars = VariableManager.getInstance().getResolvedVariables();
                    String varList = vars.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> e.getKey() + " = " + e.getValue())
                        .collect(Collectors.joining("\n"));

                    context.getSource().sendSuccess(() -> Component.literal("--- Available Variables ---\n" + varList), false);
                    return 1;
                })
            )
            .then(Commands.literal("help")
                .executes(context -> {
                    String helpText = ExpressionEvaluator.getHelp();
                    context.getSource().sendSuccess(() -> Component.literal(helpText), false);
                    return 1;
                })
            );

        dispatcher.register(command);
    }
}
