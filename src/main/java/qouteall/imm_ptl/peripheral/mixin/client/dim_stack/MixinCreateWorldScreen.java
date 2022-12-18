package qouteall.imm_ptl.peripheral.mixin.client.dim_stack;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Lifecycle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldGenSettingsComponent;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.peripheral.guide.IPOuterClientMisc;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.dimension.DimId;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackInfo;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackManagement;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackScreen;
import qouteall.imm_ptl.peripheral.ducks.IECreateWorldScreen;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.api.DimensionAPI;
import qouteall.q_misc_util.mixin.dimension.IELayeredRegistryAccess;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Mixin(CreateWorldScreen.class)
public abstract class MixinCreateWorldScreen extends Screen implements IECreateWorldScreen {
    @Shadow
    public abstract void removed();
    
    @Shadow
    protected DataPackConfig dataPacks;
    
    @Shadow
    @org.jetbrains.annotations.Nullable
    protected abstract Pair<File, PackRepository> getDataPackSelectionSettings();
    
    @Shadow
    @Final
    public WorldGenSettingsComponent worldGenSettingsComponent;
    
    @Shadow
    protected abstract void tryApplyNewDataPacks(PackRepository repository);
    
    @Shadow @Final private static Logger LOGGER;
    private Button dimStackButton;
    
    @Nullable
    private DimStackScreen ip_dimStackScreen;

//    @Nullable
//    private WorldGenSettings ip_lastWorldGenSettings;
//
//    @Nullable
//    private RegistryAccess ip_lastRegistryAccess;
    
    protected MixinCreateWorldScreen(Component title) {
        super(title);
        throw new RuntimeException();
    }
    
    @Inject(
        method = "<init>(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/world/level/DataPackConfig;Lnet/minecraft/client/gui/screens/worldselection/WorldGenSettingsComponent;)V",
        at = @At("RETURN")
    )
    private void onConstructEnded(
        Screen screen, DataPackConfig dataPackSettings, WorldGenSettingsComponent moreOptionsDialog,
        CallbackInfo ci
    ) {
    
    }
    
    @Inject(
        method = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;init()V",
        at = @At("HEAD")
    )
    private void onInitEnded(CallbackInfo ci) {
        
        dimStackButton = (Button) this.addRenderableWidget(
            Button
                .builder(
                    Component.translatable("imm_ptl.altius_screen_button"),
                    (buttonWidget) -> {
                        openDimStackScreen();
                    }
                )
                .pos(width / 2 + 5, 151)
                .size(150, 20)
                .build()
        );
        dimStackButton.visible = false;
        
    }
    
    @Inject(
        method = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;setWorldGenSettingsVisible(Z)V",
        at = @At("RETURN")
    )
    private void onMoreOptionsOpen(boolean moreOptionsOpen, CallbackInfo ci) {
        if (moreOptionsOpen) {
            dimStackButton.visible = true;
        }
        else {
            dimStackButton.visible = false;
        }
    }
    
    @Inject(
        method = "createNewWorld",
        at = @At("HEAD")
    )
    private void onCreateNewWorld(CallbackInfo ci) {
        DimStackInfo info = ip_getEffectiveDimStackInfoForWorldCreation();
        
        if (info != null) {
            DimStackManagement.dimStackToApply = info;
            
            Helper.log("Generating dimension stack world");
        }
    }
    
    @Nullable
    private DimStackInfo ip_getEffectiveDimStackInfoForWorldCreation() {
        if (ip_dimStackScreen != null) {
            return ip_dimStackScreen.getDimStackInfo();
        }
        else {
            // if the dimension stack button is not clicked,
            // it will not create the screen object
            return IPOuterClientMisc.getDimStackPreset();
        }
    }
    
    // Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;method_40209(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/world/level/DataPackConfig;)Lcom/mojang/datafixers/util/Pair
    @Inject(
        method = "lambda$tryApplyNewDataPacks$18",
        at = @At("RETURN")
    )
    private void onTryingApplyNewDatapackLoading(
        ResourceManager resourceManager,
        DataPackConfig dataPackConfig,
        CallbackInfoReturnable<Pair<Pair<WorldGenSettings, Lifecycle>, RegistryAccess.Frozen>> cir
    ) {
        ip_lastWorldGenSettings = cir.getReturnValue().getFirst().getFirst();

        ip_lastRegistryAccess = cir.getReturnValue().getSecond();
    }
    
    private void openDimStackScreen() {
        if (ip_dimStackScreen == null) {
            ip_dimStackScreen = new DimStackScreen(
                (CreateWorldScreen) (Object) this,
                this::portal_getDimensionList,
                a -> {
                    // clicking "Finish" invokes nothing
                }
            );
        }
        
        Minecraft.getInstance().setScreen(ip_dimStackScreen);
    }
    
    private List<ResourceKey<Level>> portal_getDimensionList(Screen addDimensionScreen) {
        Helper.log("Getting the dimension list");
        
        try {
            WorldCreationContext settings = worldGenSettingsComponent.settings();
            RegistryAccess.Frozen registryAccess = settings.worldgenLoadContext();
            
            WorldDimensions selectedDimensions = settings.selectedDimensions();
            
            MappedRegistry<LevelStem> subDimensionRegistry = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable());
            
            // add vanilla dimensions
            for (Map.Entry<ResourceKey<LevelStem>, LevelStem> entry : selectedDimensions.dimensions().entrySet()) {
                subDimensionRegistry.register(entry.getKey(), entry.getValue(), Lifecycle.stable());
            }
            
            RegistryAccess.Frozen subRegistryAccess =
                new RegistryAccess.ImmutableRegistryAccess(List.of(subDimensionRegistry)).freeze();
            
            LayeredRegistryAccess<Integer> wrappedLayeredRegistryAccess = IELayeredRegistryAccess.ip_init(
                List.of(1, 2),
                List.of(registryAccess, subRegistryAccess)
            );
            RegistryAccess.Frozen wrappedRegistryAccess = wrappedLayeredRegistryAccess.compositeAccess();
            
            DimensionAPI.serverDimensionsLoadEvent.invoker().run(settings.options(), wrappedRegistryAccess);
            
            return subDimensionRegistry
                .keySet().stream().map(DimId::idToKey).toList();
        }
        catch (Exception e) {
            LOGGER.error("ImmPtl getting dimension list", e);
            return List.of(DimId.idToKey("error:error"));
        }

//        return datapackDimensions.keySet().stream().map(DimId::idToKey).toList();

//        if (ip_lastWorldGenSettings == null) {
//            Helper.log("Start reloading datapacks for getting the dimension list");
//
//            // if the enabled datapack list does not change, it will not reload
//            // ensure that it really reloads
//            this.dataPacks = new DataPackConfig(new ArrayList<>(), new ArrayList<>());
//
//            // it will load the pack in render thread
//            tryApplyNewDataPacks(minecraft.getResourcePackRepository());
//
//            // it will switch to the create world screen, switch back
//            IPGlobal.preTotalRenderTaskList.addTask(() -> {
//                if (minecraft.screen == this) {
//                    minecraft.setScreen(addDimensionScreen);
//                    return true;
//                }
//                return false;
//            });
//        }
//
//        // this won't contain custom dimensions
//        WorldGenSettings rawGeneratorOptions = worldGenSettingsComponent.createFinalSettings(false).worldGenSettings();
//
//        WorldGenSettings copiedGeneratorOptions = new WorldGenSettings(
//            rawGeneratorOptions.seed(), rawGeneratorOptions.generateStructures(),
//            rawGeneratorOptions.generateBonusChest(),
//            MiscHelper.filterAndCopyRegistry(
//                ((MappedRegistry<LevelStem>) rawGeneratorOptions.dimensions()),
//                (a, b) -> true
//            )
//        );
//
//        try {
//            // register custom dimensions including alternate dimensions
//            if (ip_lastRegistryAccess != null) {
//                DimensionAPI.serverDimensionsLoadEvent.invoker().run(copiedGeneratorOptions, ip_lastRegistryAccess);
//            }
//            else {
//                Helper.err("Null registry access");
//            }
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        HashSet<ResourceKey<Level>> dims = new HashSet<>();
//
//        if (ip_lastWorldGenSettings != null) {
//            ip_lastWorldGenSettings.dimensions().keySet().forEach(id -> {
//                dims.add(DimId.idToKey(id));
//            });
//        }
//        else {
//            Helper.err("Null WorldGen settings");
//        }
//
//        copiedGeneratorOptions.dimensions().keySet().forEach(id -> {
//            dims.add(DimId.idToKey(id));
//        });
//
//        return dims.stream().toList();
    }
}
