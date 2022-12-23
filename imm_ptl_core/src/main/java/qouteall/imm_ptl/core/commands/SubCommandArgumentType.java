package qouteall.imm_ptl.core.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SubCommandArgumentType implements ArgumentType<String> {
    public static final SubCommandArgumentType instance = new SubCommandArgumentType();
    
    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String remaining = reader.getRemaining();
        reader.setCursor(reader.getCursor() + remaining.length());
        return remaining;
    }
    
    private static StringRange offset(StringRange range, int offset) {
        return new StringRange(range.getStart() + offset, range.getEnd() + offset);
    }
    
    /**
     * Provide suggestions for the sub-command
     * by creating a new command dispatcher and parse the sub-command.
     * {@link CommandDispatcher#getCompletionSuggestions(ParseResults, int)}
     */
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        CommandNode<S> rootNode = context.getRootNode();
        CommandDispatcher<S> commandDispatcher = new CommandDispatcher<>(((RootCommandNode<S>) rootNode));
        
        S source = context.getSource();
        String remaining = builder.getRemaining();
        ParseResults<S> parse = commandDispatcher.parse(remaining, source);
        
        int deltaCursor = remaining.length();
        CompletableFuture<Suggestions> suggestionsFuture = commandDispatcher.getCompletionSuggestions(parse, deltaCursor);
        
        int offset = builder.getStart();
        
        return suggestionsFuture.thenApply(suggestions -> new Suggestions(
            offset(suggestions.getRange(), offset),
            suggestions.getList().stream().map(suggestion -> new Suggestion(
                offset(suggestion.getRange(), offset),
                suggestion.getText(),
                suggestion.getTooltip()
            )).collect(Collectors.toList())
        ));
        
    }
    
    @Override
    public Collection<String> getExamples() {
        return List.of("say hi");
    }
    
    public static String get(final CommandContext<?> context, final String name) {
        return context.getArgument(name, String.class);
    }
    
    private static class CustomArgumentTypeInfo implements ArgumentTypeInfo<SubCommandArgumentType, ArgumentTypeInfo.Template<SubCommandArgumentType>> {
        @Override
        public void serializeToNetwork(Template<SubCommandArgumentType> template, FriendlyByteBuf friendlyByteBuf) {
            // no data needs serializing
        }
        
        @Override
        public ArgumentTypeInfo.Template<SubCommandArgumentType> deserializeFromNetwork(FriendlyByteBuf friendlyByteBuf) {
            return getTemplate();
        }
        
        @Override
        public void serializeToJson(Template<SubCommandArgumentType> template, JsonObject jsonObject) {
            // no data needs serializing
        }
        
        @Override
        public ArgumentTypeInfo.Template<SubCommandArgumentType> unpack(SubCommandArgumentType argumentType) {
            return getTemplate();
        }
        
        private ArgumentTypeInfo.Template<SubCommandArgumentType> getTemplate() {
            // the new java feature "var" is surprisingly useful here
            var this_ = this;
            return new ArgumentTypeInfo.Template<SubCommandArgumentType>() {
                @Override
                public SubCommandArgumentType instantiate(CommandBuildContext commandBuildContext) {
                    return instance;
                }
                
                @Override
                public ArgumentTypeInfo<SubCommandArgumentType, ?> type() {
                    return this_;
                }
            };
        }
    }

    @SubscribeEvent
    public static void init(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.COMMAND_ARGUMENT_TYPES, helper -> {
            helper.register(new ResourceLocation("imm_ptl:sub_command_argument_type"), new CustomArgumentTypeInfo());
        });
        ArgumentTypeInfos.registerByClass(SubCommandArgumentType.class, new CustomArgumentTypeInfo());
//        ArgumentTypeRegistry.registerArgumentType( //TODO @Nick1st Check if that registration procedure is correct
//            new ResourceLocation("imm_ptl:sub_command_argument_type"),
//            SubCommandArgumentType.class,
//            new CustomArgumentTypeInfo()
//        );

    }
}
