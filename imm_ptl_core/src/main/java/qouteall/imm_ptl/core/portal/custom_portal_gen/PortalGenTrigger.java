package qouteall.imm_ptl.core.portal.custom_portal_gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

import java.util.function.Function;

public abstract class PortalGenTrigger {
    public static final Codec<PortalGenTrigger> triggerCodec;
    
    public static final Registry<Codec<? extends PortalGenTrigger>> codecRegistry;
    
    
    public abstract Codec<? extends PortalGenTrigger> getCodec();
    
    public static class UseItemTrigger extends PortalGenTrigger {
        public final Item item;
        public final boolean consume;
        
        public UseItemTrigger(Item item, boolean consume) {
            this.item = item;
            this.consume = consume;
        }
        
        public boolean shouldConsume(UseOnContext context) {
            if (!consume) {
                return false;
            }
            
            Player player = context.getPlayer();
            if (player != null) {
                if (player.isCreative()) {
                    return false;
                }
            }
            
            return true;
        }
        
        @Override
        public Codec<? extends PortalGenTrigger> getCodec() {
            return useItemTriggerCodec;
        }
    }
    
    public static class ThrowItemTrigger extends PortalGenTrigger {
        public final Item item;
        
        public ThrowItemTrigger(Item item) {
            this.item = item;
        }
        
        @Override
        public Codec<? extends PortalGenTrigger> getCodec() {
            return throwItemTriggerCodec;
        }
    }
    
    public static class ConventionalDimensionChangeTrigger extends PortalGenTrigger {
        
        public ConventionalDimensionChangeTrigger() {}
        
        @Override
        public Codec<? extends PortalGenTrigger> getCodec() {
            return conventionalDimensionChangeCodec;
        }
    }
    
    public static final Codec<UseItemTrigger> useItemTriggerCodec = RecordCodecBuilder.create(instance -> {
        return instance.group(
            Registry.ITEM.byNameCodec().fieldOf("item").forGetter(o -> o.item),
            Codec.BOOL.optionalFieldOf("consume", false).forGetter(o -> o.consume)
        ).apply(instance, instance.stable(UseItemTrigger::new));
    });
    
    public static final Codec<ThrowItemTrigger> throwItemTriggerCodec = RecordCodecBuilder.create(instance -> {
        return instance.group(
            Registry.ITEM.byNameCodec().fieldOf("item").forGetter(o -> o.item)
        ).apply(instance, instance.stable(ThrowItemTrigger::new));
    });
    
    public static final Codec<ConventionalDimensionChangeTrigger> conventionalDimensionChangeCodec =
        Codec.unit(ConventionalDimensionChangeTrigger::new);
    
    static {
        codecRegistry = new MappedRegistry<>(
            ResourceKey.createRegistryKey(new ResourceLocation("imm_ptl:custom_portal_gen_trigger")),
            Lifecycle.stable(), null
        );
        
        Registry.register(
            codecRegistry, new ResourceLocation("imm_ptl:use_item"), useItemTriggerCodec
        );
        Registry.register(
            codecRegistry, new ResourceLocation("imm_ptl:throw_item"), throwItemTriggerCodec
        );
        Registry.register(
            codecRegistry, new ResourceLocation("imm_ptl:conventional_dimension_change"),
            ConventionalDimensionChangeTrigger.conventionalDimensionChangeCodec
        );
        
        triggerCodec = codecRegistry.byNameCodec().dispatchStable(
            PortalGenTrigger::getCodec,
            Function.identity()
        );
    }
    
    
}
