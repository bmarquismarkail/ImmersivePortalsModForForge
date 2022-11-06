package qouteall.imm_ptl.core.portal.animation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalExtension;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.DQuaternion;

import javax.annotation.Nullable;
import java.util.function.Function;

public class RotationAnimation implements PortalAnimationDriver {
    public static void init() {
        PortalAnimationDriver.registerDeserializer(
            new ResourceLocation("imm_ptl:rotation"),
            RotationAnimation::deserialize
        );
    }
    
    public static class RotationParameters {
        public final Vec3 center;
        public final Vec3 axis;
        public final double degreesPerTick;
        public final long startGameTime;
        public final long endGameTime;
        
        public RotationParameters(
            Vec3 center, Vec3 axis,
            double degreesPerTick, long startGameTime, long endGameTime
        ) {
            this.center = center;
            this.axis = axis;
            this.degreesPerTick = degreesPerTick;
            this.startGameTime = startGameTime;
            this.endGameTime = endGameTime;
        }
        
        public RotationParameters(CompoundTag tag) {
            center = Helper.getVec3d(tag, "center");
            axis = Helper.getVec3d(tag, "axis");
            degreesPerTick = tag.getDouble("degreesPerTick");
            startGameTime = tag.getLong("startGameTime");
            endGameTime = tag.getLong("endGameTime");
        }
        
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            Helper.putVec3d(tag, "center", center);
            Helper.putVec3d(tag, "axis", axis);
            tag.putDouble("degreesPerTick", degreesPerTick);
            tag.putLong("startGameTime", startGameTime);
            tag.putLong("endGameTime", endGameTime);
            return tag;
        }
        
        // this builder class is fully generated by GitHub Copilot!
        public static class Builder {
            public Vec3 center;
            public Vec3 axis;
            public double degreesPerTick;
            public long startGameTime;
            public long endGameTime;
            
            public Builder setCenter(Vec3 center) {
                this.center = center;
                return this;
            }
            
            public Builder setAxis(Vec3 axis) {
                this.axis = axis;
                return this;
            }
            
            public Builder setDegreesPerTick(double degreesPerTick) {
                this.degreesPerTick = degreesPerTick;
                return this;
            }
            
            public Builder setStartGameTime(long startGameTime) {
                this.startGameTime = startGameTime;
                return this;
            }
            
            public Builder setEndGameTime(long endGameTime) {
                this.endGameTime = endGameTime;
                return this;
            }
            
            public RotationParameters build() {
                return new RotationParameters(
                    center, axis, degreesPerTick, startGameTime, endGameTime
                );
            }
        }
    }
    
    public Vec3 initialPortalOrigin;
    public Vec3 initialPortalDestination;
    public DQuaternion initialPortalOrientation;
    public DQuaternion initialPortalRotation;
    @Nullable
    public RotationParameters thisSideRotation;
    @Nullable
    public RotationParameters otherSideRotation;
    
    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        
        tag.putString("type", "imm_ptl:rotation");
        Helper.putVec3d(tag, "initialPortalOrigin", initialPortalOrigin);
        Helper.putVec3d(tag, "initialPortalDestination", initialPortalDestination);
        tag.put("initialPortalOrientation", initialPortalOrientation.toTag());
        tag.put("initialPortalRotation", initialPortalRotation.toTag());
        if (thisSideRotation != null) {
            tag.put("thisSideRotation", thisSideRotation.toTag());
        }
        if (otherSideRotation != null) {
            tag.put("otherSideRotation", otherSideRotation.toTag());
        }
        
        return tag;
    }
    
    private static RotationAnimation deserialize(CompoundTag tag) {
        RotationAnimation animation = new RotationAnimation();
        animation.initialPortalOrigin = Helper.getVec3d(tag, "initialPortalOrigin");
        animation.initialPortalDestination = Helper.getVec3d(tag, "initialPortalDestination");
        animation.initialPortalOrientation = DQuaternion.fromTag(tag.getCompound("initialPortalOrientation"));
        animation.initialPortalRotation = DQuaternion.fromTag(tag.getCompound("initialPortalRotation"));
        animation.thisSideRotation = tag.contains("thisSideRotation") ?
            new RotationParameters(tag.getCompound("thisSideRotation")) : null;
        animation.otherSideRotation = tag.contains("otherSideRotation") ?
            new RotationParameters(tag.getCompound("otherSideRotation")) : null;
        return animation;
    }
    
    @Override
    public boolean update(Portal portal, long tickTime, float partialTicks) {
        boolean thisSideEnds = false;
        boolean otherSideEnds = false;
        
        DQuaternion thisSideRotationQuaternion = DQuaternion.identity;
        
        if (thisSideRotation != null) {
            double passedTicks = ((double) (tickTime - 1 - thisSideRotation.startGameTime)) + partialTicks;
            
            if (passedTicks > (thisSideRotation.endGameTime - thisSideRotation.startGameTime)) {
                thisSideEnds = true;
                passedTicks = thisSideRotation.endGameTime - thisSideRotation.startGameTime;
            }
            
            double angle = thisSideRotation.degreesPerTick * passedTicks;
            thisSideRotationQuaternion = DQuaternion.rotationByDegrees(thisSideRotation.axis, angle);
            Vec3 thisSideOffset = initialPortalOrigin.subtract(thisSideRotation.center);
            Vec3 rotatedThisSideOffset = thisSideRotationQuaternion.rotate(thisSideOffset);
            portal.setOriginPos(thisSideRotation.center.add(rotatedThisSideOffset));
        }
        
        DQuaternion orientationRotation = thisSideRotationQuaternion.hamiltonProduct(initialPortalOrientation);
        portal.setOrientationRotation(orientationRotation);
        
        DQuaternion otherSideRotationQuaternion = DQuaternion.identity;
        if (otherSideRotation != null) {
            double passedTicks = ((double) (tickTime - 1 - otherSideRotation.startGameTime)) + partialTicks;
            
            if (passedTicks > (otherSideRotation.endGameTime - otherSideRotation.startGameTime)) {
                otherSideEnds = true;
                passedTicks = otherSideRotation.endGameTime - otherSideRotation.startGameTime;
            }
            
            double angle = otherSideRotation.degreesPerTick * passedTicks;
            otherSideRotationQuaternion = DQuaternion.rotationByDegrees(otherSideRotation.axis, angle);
            Vec3 otherSideOffset = initialPortalDestination.subtract(otherSideRotation.center);
            Vec3 rotatedOtherSideOffset = otherSideRotationQuaternion.rotate(otherSideOffset);
            portal.setDestination(otherSideRotation.center.add(rotatedOtherSideOffset));
        }
        
        DQuaternion initialPortalDestOrientation =
            initialPortalRotation.hamiltonProduct(initialPortalOrientation);
        
        DQuaternion destOrientation = otherSideRotationQuaternion.hamiltonProduct(initialPortalDestOrientation);
        portal.setRotationTransformationD(
            destOrientation.hamiltonProduct(orientationRotation.getConjugated())
        );
        
        return thisSideEnds && otherSideEnds;
    }
    
    public static void givePortalRotationAnimation(
        Portal portal,
        RotationParameters thisSideRotation,
        RotationParameters otherSideRotation
    ) {
        PortalExtension extension = PortalExtension.get(portal);
        if (extension.reversePortal != null) {
            if (extension.reversePortal.getAnimationDriver() instanceof RotationAnimation rot) {
                if (thisSideRotation != null) {
                    rot.otherSideRotation = thisSideRotation;
                }
                if (otherSideRotation != null) {
                    rot.thisSideRotation = otherSideRotation;
                }
                extension.reversePortal.reloadAndSyncToClient();
                return;
            }
        }
        
        if (extension.parallelPortal != null) {
            if (extension.parallelPortal.getAnimationDriver() instanceof RotationAnimation rot) {
                if (thisSideRotation != null) {
                    rot.otherSideRotation = thisSideRotation;
                }
                if (otherSideRotation != null) {
                    rot.thisSideRotation = otherSideRotation;
                }
                extension.parallelPortal.reloadAndSyncToClient();
                return;
            }
        }
        
        RotationAnimation animation = new RotationAnimation();
        animation.initialPortalOrigin = portal.getOriginPos();
        animation.initialPortalDestination = portal.getDestPos();
        animation.initialPortalOrientation = portal.getOrientationRotation();
        animation.initialPortalRotation = portal.getRotationD();
        animation.thisSideRotation = thisSideRotation;
        animation.otherSideRotation = otherSideRotation;
        portal.setAnimationDriver(animation);
    }
    
}
