package qouteall.imm_ptl.core.platform_specific;

import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import qouteall.imm_ptl.core.chunk_loading.MyClientChunkManager;
import qouteall.imm_ptl.core.portal.custom_portal_gen.PortalGenInfo;
import qouteall.q_misc_util.Helper;

import javax.annotation.Nullable;

public class O_O {
    public static boolean isDimensionalThreadingPresent = false;
    
    public static boolean isForge() {
        return true;
    }
    
    @OnlyIn(Dist.CLIENT)
    public static void onPlayerChangeDimensionClient(
        ResourceKey<Level> from, ResourceKey<Level> to
    ) {
        RequiemCompat.onPlayerTeleportedClient();
    }

//    @OnlyIn(Dist.CLIENT)
//    public static void segregateClientEntity(
//        ClientWorld fromWorld,
//        Entity entity
//    ) {
//        ((IEClientWorld_MA) fromWorld).segregateEntity(entity);
//        entity.removed = false;
//    }
//
//    public static void segregateServerEntity(
//        ServerWorld fromWorld,
//        Entity entity
//    ) {
//        fromWorld.removeEntity(entity);
//        entity.removed = false;
//    }
//
//    public static void segregateServerPlayer(
//        ServerWorld fromWorld,
//        ServerPlayerEntity player
//    ) {
//        fromWorld.removePlayer(player);
//        player.removed = false;
//    }
    
    public static void onPlayerTravelOnServer(
        ServerPlayer player,
        ResourceKey<Level> from,
        ResourceKey<Level> to
    ) {
        RequiemCompat.onPlayerTeleportedServer(player);
    }
    
    public static void loadConfigFabric() {
        Helper.log("Loading Immersive Portals config");
        IPConfig ipConfig = IPConfig.readConfig();
        ipConfig.onConfigChanged();
        ipConfig.saveConfigFile();
    }
    
    public static void onServerConstructed() {
        // forge version initialize server config
    }
    
    private static final BlockState obsidianState = Blocks.OBSIDIAN.defaultBlockState();
    
    public static boolean isObsidian(BlockState blockState) {
        return blockState == obsidianState;
    }
    
    public static void postClientChunkLoadEvent(LevelChunk chunk) {
        MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load(chunk));
    }
    
    public static void postClientChunkUnloadEvent(LevelChunk chunk) {
        MinecraftForge.EVENT_BUS.post(new ChunkEvent.Unload(chunk));
    }
    
    public static boolean isDedicatedServer() {
        return FMLEnvironment.dist == Dist.DEDICATED_SERVER;
    }
    
    public static void postPortalSpawnEventForge(PortalGenInfo info) {
    
    }
    
    @OnlyIn(Dist.CLIENT)
    public static ClientChunkCache createMyClientChunkManager(ClientLevel world, int loadDistance) {
        return new MyClientChunkManager(world, loadDistance);
    }
    
    public static boolean getIsPehkuiPresent() {
        return ModList.get().isLoaded("pehkui");
    }
    
    @Nullable
    public static String getImmPtlModInfoUrl() {
        String gameVersion = SharedConstants.getCurrentVersion().getName();
        
        int lastDotIndex = gameVersion.lastIndexOf('.');
        
        // only take the major version
        // if it's 1.19.2, the major version is 1.19
        String majorGameVersion = gameVersion.substring(0, lastDotIndex);
        
        return "https://qouteall.fun/immptl_info/%s.x.json".formatted(majorGameVersion);
    }
    
    public static boolean isModLoadedWithinVersion(String modId, @Nullable String startVersion, @Nullable String endVersion) {

        if (ModList.get().isLoaded(modId)) { // TODO @Nick1st hopefully I didn't mess this up
            ArtifactVersion version = ModList.get().getModContainerById(modId).get().getModInfo().getVersion();

            if (startVersion != null) {
                int i = version.compareTo(new DefaultArtifactVersion(startVersion));
                if (i < 0) {
                    return false;
                }
            }

            if (endVersion != null) {
                int i = version.compareTo(new DefaultArtifactVersion(endVersion));
                if (i > 0) {
                    return false;
                }
            }
            
            return true;
            
        }
        else {
            return false;
        }
    }
    
//    public static boolean shouldUpdateImmPtl(String latestReleaseVersion) { // TODO @Nick1st Implement Forge Version checking
//        Version currentVersion = FabricLoader.getInstance()
//            .getModContainer("imm_ptl_core").get().getMetadata().getVersion();
//        try {
//            Version latestVersion = Version.parse(latestReleaseVersion);
//
//            if (latestVersion.compareTo(currentVersion) > 0) {
//                return true;
//            }
//        } catch (VersionParsingException e) {
//            e.printStackTrace();
//        }
//
//        return false;
//    }
    
    public static String getModDownloadLink() {
        return "https://www.curseforge.com/minecraft/mc-mods/immersive-portals-mod";
    }
}
