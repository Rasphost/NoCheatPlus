/*
 * This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.neatmonster.nocheatplus.utilities.location;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.checks.moving.model.VehicleMoveData;
import fr.neatmonster.nocheatplus.compat.MCAccess;
import fr.neatmonster.nocheatplus.compat.versions.GenericVersion;
import fr.neatmonster.nocheatplus.components.registry.event.IHandle;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.collision.AxisAlignedBBUtils;
import fr.neatmonster.nocheatplus.utilities.collision.CollisionUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache.IBlockCacheNode;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.math.MathUtil;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;
import fr.neatmonster.nocheatplus.utilities.moving.Magic;

/**
 * A location with an entity with a lot of extra stuff.
 * 
 * @author asofold
 *
 */
public class RichEntityLocation extends RichBoundsLocation {
    
    /*
     * NOTE: HumanEntity default with + height (1.11.2): elytra 0.6/0.6,
     * sleeping 0.2/0.2, sneaking 0.6/1.65, normal 0.6/1.8 - head height is 0.4
     * with elytra, 0.2 with sleeping, height - 0.08 otherwise.
     */
    // Final members //
    /** The mc access. */
    private final IHandle<MCAccess> mcAccess;


    // Simple members //
    /** Full bounding box width. */
    /*
     * TODO: This is the entity width, happens to usually be the bounding box
     * width +-. Move to entity / replace.
     */
    private double width; 

    /** Some entity collision height. */
    private double height; // TODO: Move to entity / replace.

    /** Indicate that this is a living entity. */
    private boolean isLiving;

    /** Living entity eye height, otherwise same as height.*/
    private double eyeHeight;

    /**
     * Entity is on ground, due to standing on an entity. (Might not get
     * evaluated if the player is on ground anyway.)
     */
    private boolean standsOnEntity = false;


    // "Heavy" object members that need to be set to null on cleanup. //

    /** The entity. */
    private Entity entity = null;


    /**
     * Instantiates a new rich entity location.
     *
     * @param mcAccess
     *            the mc access
     * @param blockCache
     *            BlockCache instance, may be null.
     */
    public RichEntityLocation(final IHandle<MCAccess> mcAccess, final BlockCache blockCache) {
        super(blockCache);
        this.mcAccess = mcAccess;
    }

    /**
     * Gets the width.
     *
     * @return the width
     */
    public double getWidth() {
        return width;
    }

    /**
     * Gets the height.
     *
     * @return the height
     */
    public double getHeight() {
        return height;
    }

    /**
     * Gets the eye height.
     *
     * @return the eye height
     */
    public double getEyeHeight() {
        return eyeHeight;
    }

    /**
     * Gets the entity.
     *
     * @return the entity
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * Test if this is a LivingEntity instance.
     *
     * @return true, if is living
     */
    public boolean isLiving() {
        return isLiving;
    }

    /**
     * Retrieve the currently registered MCAccess instance.
     *
     * @return the MC access
     */
    public MCAccess getMCAccess() {
        return mcAccess.getHandle();
    }

    /**
     * Get the internally stored IHandle instance for retrieving the currently
     * registered instance of MCAccess.
     * 
     * @return
     */
    public IHandle<MCAccess> getMCAccessHandle() {
        return mcAccess;
    }
    
    /**
     * @return False, for 1.11 and lower clients jumping on beds.
     */
    public boolean isOnBouncyBlock() {
       if (onBouncyBlock != null) {
           return onBouncyBlock;
       }
        if (GenericVersion.isLowerThan(entity, "1.12")) {
            if (onBouncyBlock) {
                if (onSlimeBlock != null && !onSlimeBlock) {
                    // Beds were made bouncy on 1.12
                    onBouncyBlock = false;
                    return onBouncyBlock;
                }
            }
        }
        // Not a legacy client.
        return super.isOnBouncyBlock();
    }
    
    /**
     * Legacy collision method.
     * 
     * @return Whether the entity is on a slime block; always false for 1.7 and below
     */
    public boolean isOnSlimeBlock() {
        if (onSlimeBlock != null) {
            return onSlimeBlock;
        }
        if (GenericVersion.isLowerThan(entity, "1.8")) {
            // Does not exist.
            onSlimeBlock = false;
            return onSlimeBlock;
        }
        if (GenericVersion.isAtMost(entity, "1.19.4")) {
            // Before 1.20, block properties were applied only if the player is at the center of the block.
            final Material typeId = getTypeIdBelow();
            final long theseFlags = BlockFlags.getBlockFlags(typeId);
            onSlimeBlock = (theseFlags & BlockFlags.F_SLIME) != 0;
            return onSlimeBlock;
        }
        // Not a legacy client.
        return super.isOnSlimeBlock();

    }
    
    /**
     * Legacy collision method.
     * 
     * @return Whether the entity is on a ice-like block.
     */
    public boolean isOnIce() {
        if (onIce != null) {
            return onIce;
        }
        if (GenericVersion.isAtMost(entity, "1.19.4")) {
            // Before 1.20, block properties were applied only if the player is at the center of the block.
            final Material typeId = getTypeIdBelow();
            final long thisFlags = BlockFlags.getBlockFlags(typeId);
            onIce = isOnGround() && (thisFlags & BlockFlags.F_ICE) != 0;
            return onIce;
        }
        // Not a legacy client.
        return super.isOnIce();
    }
    
    /**
     * Legacy collision method.
     * 
     * @return Whether the entity is on blue ice. Always false for 1.12 and below (in which case, the onIce field is changed instead).
     */
    public boolean isOnBlueIce() {
        if (onBlueIce != null) {
            return onBlueIce;
        }
        if (GenericVersion.isAtMost(entity, "1.19.4")) {
            // Before 1.20, block properties were applied only if the player is at the center of the block.
            final Material typeId = getTypeIdBelow();
            final long thisFlags = BlockFlags.getBlockFlags(typeId);
            onBlueIce = isOnGround() && (thisFlags & BlockFlags.F_BLUE_ICE) != 0;
            if (onBlueIce && GenericVersion.isLowerThan(entity, "1.13")) {
                // Does not exist, but assume multiprotocol plugins to map it to regular ice.
                onBlueIce = false;
                onIce = true;
            }
            return onBlueIce;
        }
        // Not a legacy client.
        return super.isOnBlueIce();
    }
    
    /** 
     * @return Always false for 1.12 and below clients.
     */
    public boolean isInWaterLogged() {
        if (inWaterLogged != null) {
            return inWaterLogged;
        }
        if (GenericVersion.isLowerThan(entity, "1.13")) {
            // Waterlogged blocks don't exist for older clients.
            inWaterLogged = false;
            return inWaterLogged;
        }
        return super.isInWaterLogged();
    }

    /**
     * Legacy collision method(s)
     * 
     * @return true, if the player is in lava
     */
    public boolean isInLava() {
        if (inLava != null) {
            return inLava;
        }
        // 1.13 and below clients use this no-sense method to check if the player is in lava
        // 1.8 client, Entity.java -> handleLavaMovement() -> isMaterialInBB in World.java
        if (GenericVersion.isLowerThan(entity, "1.14")) {
            // Force-override the inLava result from RichBoundsLocation.
            inLava = false;
            double[] aaBB = getAABBCopy();
            int iMinX = MathUtil.floor(aaBB[0] + 0.1);
            int iMaxX = MathUtil.floor(aaBB[3] - 0.1 + 1.0);
            int iMinY = MathUtil.floor(aaBB[1] + 0.4);
            int iMaxY = MathUtil.floor(aaBB[4] - 0.4 + 1.0);
            int iMinZ = MathUtil.floor(aaBB[2] + 0.1);
            int iMaxZ = MathUtil.floor(aaBB[5] - 0.1 + 1.0);
            for (int x = iMinX; x < iMaxX; x++) {
                for (int y = iMinY; y < iMaxY; y++) {
                    for (int z = iMinZ; z < iMaxZ; z++) {
                        final IBlockCacheNode node = blockCache.getOrCreateBlockCacheNode(x, y, z, false);
                        if ((BlockFlags.getBlockFlags(node.getType()) & BlockFlags.F_LAVA) != 0) {
                            inLava = true;
                            return inLava;
                        }
                    }
                }
            }
            // Did not collide.
            return inLava;
        }
        // Mojang tweaked lava collision in 1.14 to use the checkInsideBlocks method, likewise webs / berry bushes / powder snow etc...)
        if (GenericVersion.isAtLeast(entity, "1.14") && GenericVersion.isLowerThan(entity, "1.16")) {
            // Force-override the inLava result from RichBoundsLocation
            inLava = false;
            inLava = isInsideBlock(BlockFlags.F_LAVA);
            return inLava;
        }
        // Not a legacy client, nothing to do.
        return super.isInLava();
    }

    /**
     * Legacy collision method(s)
     * 
     * @return true, if is in water
     */
    public boolean isInWater() {
        if (inWater != null) {
            return inWater;
        }
        if (GenericVersion.isLowerThan(entity, "1.13")) {
            // 1.13 and below use this extra contraction for water collision.
            inWater = false;
            double extraContraction = 0.4;
            final int iMinX = MathUtil.floor(minX + 0.001);
            final int iMaxX = MathUtil.ceil(maxX - 0.001);
            final int iMinY = MathUtil.floor(minY + 0.001 + extraContraction); 
            final int iMaxY = MathUtil.ceil(maxY - 0.001 - extraContraction);
            final int iMinZ = MathUtil.floor(minZ + 0.001);
            final int iMaxZ = MathUtil.ceil(maxZ - 0.001);
            // NMS collision method
            for (int x = iMinX; x < iMaxX; x++) {
                for (int y = iMinY; y < iMaxY; y++) {
                    for (int z = iMinZ; z < iMaxZ; z++) {
                        double liquidHeight = BlockProperties.getLiquidHeightAt(blockCache, x, y, z, BlockFlags.F_WATER, true);
                        double liquidHeightToWorld = y + liquidHeight;
                        if (liquidHeightToWorld >= minY + 0.001 + extraContraction && liquidHeight != 0.0) {
                            // Collided.
                            inWater = true;
                            return inWater;
                        }
                    }
                }
            }
            // Did not collide, override the inWater flag.
            return inWater;
        }
        // Not a legacy client, return the result.
        return super.isInWater();
    }
    
    /**
     * Legacy collision method.
     * 
     * @return Whether the entity is on a honey block; always false for 1.14 and below.
     */
    public boolean isOnHoneyBlock() {
        if (onHoneyBlock != null) {
            return onHoneyBlock;
        }
        // Is the player actually in the block?
        if (GenericVersion.isLowerThan(entity, "1.15")) {
            // Legacy clients don't have such block.
            // This will allow "jumping" on it but won't solve legacy players "floating" midair due to the honey block's lower height (ViaVersion maps it to slime, thus full collision box (1.0))
            // We'd need per-player blocks for such.
            onHoneyBlock = false;
            return onHoneyBlock;
        }
        if (GenericVersion.isAtMost(entity, "1.19.4")) {
            // Only if in the block and at the center. Collision logic was changed in 1.20
            onHoneyBlock = (BlockFlags.getBlockFlags(getTypeId()) & BlockFlags.F_STICKY) != 0;
            return onHoneyBlock;
        }
        // Not a legacy client.
        return super.isOnHoneyBlock();
    }
    
    /**
     * Legacy collision method.
     * 
     * @return Whether the entity is in a soul sand block.
     */
    public boolean isInSoulSand() {
        if (inSoulSand != null) {
            return inSoulSand;
        }
        if (GenericVersion.isAtMost(entity, "1.19.4")) {
            // Only if in the block and at the center.
            inSoulSand = (BlockFlags.getBlockFlags(getTypeId()) & BlockFlags.F_SOULSAND) != 0;
            return inSoulSand;
        }
        // Not a legacy client
        return super.isInSoulSand();
    }

    /** 
     * @return Always false for 1.13 and below
     */
    public boolean isInBerryBush() {
        if (inBerryBush != null) {
            return inBerryBush;
        }
        if (GenericVersion.isLowerThan(entity, "1.14")) {
            // (Mapped to grass with viaver)
            inBerryBush = false;
            return inBerryBush;
        }
        // Not a legacy client.
        return super.isInBerryBush();
    }

    /**
     * From HoneyBlock.java 
     * Test if the player is sliding sideway with a honey block (NMS, checks for speed as well)
     * 
     * @return if the player is sliding on a honey block.
     */
    public boolean isSlidingDown() {
        final Player p = (Player) entity;
        final IPlayerData pData = DataManager.getPlayerData(p);
        final MovingData data = pData.getGenericInstance(MovingData.class);
        final PlayerMoveData playerMove = data.playerMoves.getCurrentMove();
        final VehicleMoveData vehicleMove = data.vehicleMoves.getCurrentMove();
        final double yDistance = entity instanceof Player ? playerMove.yDistance : vehicleMove.yDistance;
        if (GenericVersion.isLowerThan(entity, "1.15")) {
            // This mechanic was introduced in 1.15 alongside honey blocks
            return false;
        }
        if (isOnGround()) {
            // Not sliding, clearly.
            return false;
        }
        // With the current implementation, this condition is never run due to from.getBlockY(), it should be the location of the block not player's
        //if (from.getY() > from.getBlockY() + 0.9375D - 1.0E-7D) {
        //    // Too far from the block.
        //    return false;
        //} 
        if (yDistance >= -Magic.DEFAULT_GRAVITY) {
            // Minimum speed.
            return false;
        }
        // With the current implementation, this condition will always return false, see above
        //double xDistanceToBlock = Math.abs((double)from.getBlockX() + 0.5D - from.getX());
        //double zDistanceToBlock = Math.abs((double)from.getBlockZ() + 0.5D - from.getZ());
        //double var7 = 0.4375D + (width / 2.0F);
        //return xDistanceToBlock + 1.0E-7D > var7 || zDistanceToBlock + 1.0E-7D > var7;
        collectBlockFlags(); // Do call here, else NPE for some places.
        if ((blockFlags & BlockFlags.F_STICKY) == 0) {
            return false;
        }
        // Finally, test for collision
        return isNextToBlock(0.01, BlockFlags.F_STICKY);
    }

    /**
     * Simple check with custom margins (Boat, Minecart). Does not update the
     * internally stored standsOnEntity field.
     *
     * @param yOnGround
     *            Margin below the player.
     * @param xzMargin
     *            the xz margin
     * @param yMargin
     *            Extra margin added below and above.
     * @return true, if successful
     */
    public boolean standsOnEntity(final double yOnGround, final double xzMargin, final double yMargin) {
        return blockCache.standsOnEntity(entity, minX - xzMargin, minY - yOnGround - yMargin, minZ - xzMargin, maxX + xzMargin, minY + yMargin, maxZ + xzMargin);
    }

    /**
     * Checks if the player may be on ground due to an entity.
     * 
     * @return true, if the player is on ground
     */
    public boolean isOnGround() { 
        if (onGround != null) {
            return onGround;
        }
        final double d1 = 0.25;
        if (blockCache.standsOnEntity(entity, minX - d1, minY - yOnGround, minZ - d1, maxX + d1, minY, maxZ + d1)) {
            // On ground due to an entity
            // TODO: Again, this check needs to be refined to be as close as possible to vanilla. With prediction, we cannot use a leniency magic value.
            onGround = standsOnEntity = true;
            return onGround;
        }
        return super.isOnGround();
    }

    /**
     * Test if the player is just on ground due to standing on an entity.
     * 
     * @return True, if the player is not standing on blocks, but on an entity.
     */
    public boolean isOnGroundDueToStandingOnAnEntity() {
        return isOnGround() && standsOnEntity; // Just ensure it is initialized.
    }

    /**
     * Retrieve the collision Vector of the entity
     * (From Entity.class -> collide()).
     *
     * @param input    Meant to represent the collision-seeking speed. <br>
     *                 If no collision can be found within the given speed, the method will return the unmodified input Vector as a result.
     *                 Otherwise, a modified Vector containing the "obstructed" speed is returned. <br>
     *                 (Thus, if you wish to know if the player collided with something: inputXYZ != collidedXYZ)
     * @param onGround The "on ground" status of the entity. <br> Can be NCP's or Minecraft's. <br> Do mind that if using NCP's, lost ground cases and mismatches must be taken into account.
     *                 Used to determine whether the entity will be able to step up with the given input.
     * @param ncpAABB  The axis-aligned bounding box of the entity at the position they moved from (in other words, the last AABB of the entity).
     *                 Only makes sense if you call this method during PlayerMoveEvents, because the NMS bounding box will already be moved to the event#getTo() Location, by the time this gets called by moving checks.
     *                 If null, a new AABB using NMS' parameters (width/height) will be created.
     * @return A Vector containing the collision components (collisionXYZ)
     */
    public Vector collide(Vector input, boolean onGround, MovingConfig cc, double[] ncpAABB) {
        if (input.isZero()) {
            return new Vector();
        }
        double[] tAABB = ncpAABB == null ? AxisAlignedBBUtils.createAABB(entity) : ncpAABB.clone();
        List<double[]> collisionBoxes = new ArrayList<>();
        CollisionUtil.getCollisionBoxes(blockCache, entity, AxisAlignedBBUtils.expandToCoordinate(tAABB, input.getX(), input.getY(), input.getZ()), collisionBoxes, false);
        Vector collisionVector = input.lengthSquared() == 0.0 ? input : CollisionUtil.collideBoundingBox(input, tAABB, collisionBoxes);
        boolean collideX = input.getX() != collisionVector.getX();
        boolean collideY = input.getY() != collisionVector.getY();
        boolean collideZ = input.getZ() != collisionVector.getZ();
        boolean touchGround = onGround || collideY && collisionVector.getY() < 0.0;
        // TODO: Not only cc.sfStepHeight (0.6), change on vehicle(boats:0.0, other vehicle 1.0)
        // Entity is on ground, collided with a wall and can actually step upwards: try to make it step up.
        if (cc.sfStepHeight > 0.0 && touchGround && (collideX || collideZ)) {
            Vector stepUpVector = CollisionUtil.collideBoundingBox(new Vector(input.getX(), cc.sfStepHeight, input.getZ()), tAABB, collisionBoxes);
            // Introduced in 1.8
            if (GenericVersion.isAtLeast(entity,"1.8")) {
                Vector stepFix = CollisionUtil.collideBoundingBox(new Vector(0.0, cc.sfStepHeight, 0.0), AxisAlignedBBUtils.expandToCoordinate(tAABB, input.getX(), 0.0, input.getZ()), collisionBoxes);
                // Check this very useful video for a visual representation of this code: https://www.youtube.com/watch?v=Awa9mZQwVi8
                if (stepFix.getY() < cc.sfStepHeight) {
                    Vector combinedStep = CollisionUtil.collideBoundingBox(new Vector(input.getX(), 0.0, input.getZ()), AxisAlignedBBUtils.move(tAABB, stepFix.getX(), stepFix.getY(), stepFix.getZ()), collisionBoxes).add(stepFix);
                    if (TrigUtil.distanceSquared(combinedStep) > TrigUtil.distanceSquared(stepUpVector)) {
                        stepUpVector = combinedStep;
                    }
                }
            }
            // Did the step-up yield a higher distance? If so, apply step motion
            if (TrigUtil.distanceSquared(stepUpVector) > TrigUtil.distanceSquared(collisionVector)) {
                return stepUpVector.add(CollisionUtil.collideBoundingBox(new Vector(0.0, -stepUpVector.getY() + input.getY(), 0.0), AxisAlignedBBUtils.move(tAABB, stepUpVector.getX(), stepUpVector.getY(), stepUpVector.getZ()), collisionBoxes));
            }
        }
        return collisionVector;
    }
    
    /**
     * How Minecraft calculates liquid pushing speed.
     * Can be found in: Entity.java, updateFluidHeightAndDoFluidPushing()
     * 
     * @param xDistance 
     * @param zDistance
     * @param liquidTypeFlag The flags F_LAVA or F_WATER to use.
     * @return A vector representing the pushing force (read as: speed) of the liquid.
     */
    public Vector getLiquidPushingVector(final double xDistance, final double zDistance, final long liquidTypeFlag) {
        final Player p = (Player) entity;
        if (isInLava() && GenericVersion.isLowerThan(entity, "1.16")) {
            // Lava pushes entities starting from the nether update (1.16+)
            return new Vector();
        }
        // No Location#locToBlock() here (!)
        // Contract bounding box.
        double extraContraction = GenericVersion.isLowerThan(entity, "1.13") ? 0.4 : 0.0;
        final int iMinX = MathUtil.floor(minX + 0.001);
        final int iMaxX = MathUtil.ceil(maxX - 0.001);
        final int iMinY = MathUtil.floor(minY + 0.001 + extraContraction);
        final int iMaxY = MathUtil.ceil(maxY - 0.001 - extraContraction);
        final int iMinZ = MathUtil.floor(minZ + 0.001);
        final int iMaxZ = MathUtil.ceil(maxZ - 0.001);
        double d2 = 0.0;
        Vector pushingVector = new Vector();
        int k1 = 0;
        // NMS collision method. We need to check for a second collision because of how Minecraft handles fluid pushing
        // (And we need the exact speed for predictions)
        for (int x = iMinX; x < iMaxX; x++) {
            for (int y = iMinY; y < iMaxY; y++) {
                for (int z = iMinZ; z < iMaxZ; z++) {
                    // LEGACY 1.13-
                    if (GenericVersion.isLowerThan(entity, "1.13")) {
                        double liquidHeight = BlockProperties.getLiquidHeightAt(blockCache, x, y, z, liquidTypeFlag, false);
                        if (liquidHeight != 0.0) {
                            double d0 = (float) (y + 1) - liquidHeight;
                            if (!p.isFlying() && iMaxY >= d0) {
                                // Collided
                                Vector flowVector = getFlowForceVector(x, y, z, liquidTypeFlag);
                                pushingVector.add(flowVector);
                            }
                        }
                    }
                    // MODERN 1.13+
                    else {
                        double liquidHeight = BlockProperties.getLiquidHeightAt(blockCache, x, y, z, liquidTypeFlag, false);
                        double liquidHeightToWorld = y + liquidHeight;
                        if (liquidHeightToWorld >= minY + 0.001 && liquidHeight != 0.0 && !p.isFlying()) {
                            // Collided.
                            d2 = Math.max(liquidHeightToWorld - minY + 0.001, d2); // 0.001 is the Magic number the game uses to expand the box with newer versions.
                            // Determine pushing speed by using the current flow of the liquid.
                            Vector flowVector = getFlowForceVector(x, y, z, liquidTypeFlag);
                            if (d2 < 0.4) {
                                flowVector = flowVector.multiply(d2);
                            }
                            pushingVector = pushingVector.add(flowVector);
                            k1++ ;
                        }
                    }
                }
            }
        }
        // LEGACY
        if (GenericVersion.isLowerThan(entity, "1.13")) {
            if (isInWater() && pushingVector.lengthSquared() > 0.0) {
                pushingVector.normalize();
                pushingVector.multiply(0.014);
            }
        }
        // MODERN
        else {
            // In Entity.java:
            // LAVA: 0.0023333333333333335 if in any other world that isn't nether, 0.007 otherwise.
            // WATER: 0.014
            // NOTE: Water first then Lava (fixes issue with the player's box being both in water and in lava)
            double flowSpeedMultiplier = isInWater() ? 0.014 : (world.getEnvironment() == World.Environment.NETHER ? 0.007 : 0.0023333333333333335);
            if (pushingVector.lengthSquared() > 0.0) {
                if (k1 > 0) {
                   pushingVector = pushingVector.multiply(1.0 / k1);
                }
                if (p.isInsideVehicle()) {
                    // Normalize the vector anyway if inside liquid on a vehicle... (ease some work with the (future) vehicle rework)
                    pushingVector = pushingVector.normalize();
                }
                pushingVector = pushingVector.multiply(flowSpeedMultiplier); 
                if (Math.abs(xDistance) < Magic.NEGLIGIBLE_SPEED_THRESHOLD 
                    && Math.abs(zDistance) < Magic.NEGLIGIBLE_SPEED_THRESHOLD
                    && pushingVector.length() < 0.0045000000000000005) {
                    pushingVector = pushingVector.normalize().multiply(0.0045000000000000005);
                }
            }
        }
        return pushingVector;
    }
    
    // (Taken from Grim :p)
    private Vector normalizedVectorWithoutNaN(Vector vector) {
        double var0 = vector.length();
        return var0 < 1.0E-4 ? new Vector() : vector.multiply(1 / var0);
    }
    
    /**
     * Minecraft's function to calculate the liquid's flow force.
     * 'FlowingFluid'.java / FluidTypeFlowing.java, getFlow()
     * 
     * @param x
     * @param y
     * @param z
     * @param liquidTypeFlag
     * @return the vector, representing the liquid's flowing force.
     */
    public Vector getFlowForceVector(int x, int y, int z, final long liquidTypeFlag) {
        double xModifier = 0.0D;
        double zModifier = 0.0D;
        float liquidHeight = (float) BlockProperties.getLiquidHeightAt(blockCache, x, y, z, liquidTypeFlag, true);
        for (BlockFace hDirection : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
            int modX = x + hDirection.getModX();
            int modZ = z + hDirection.getModZ();
            if (BlockProperties.affectsFlow(blockCache, x, y, z, modX, y, modZ, liquidTypeFlag)) {
                float modLiquidHeight = (float) BlockProperties.getLiquidHeightAt(blockCache, modX, y, modZ, liquidTypeFlag, true); 
                float flowForce = 0.0F;
                if (modLiquidHeight == 0.0F) {
                    final IBlockCacheNode node = blockCache.getOrCreateBlockCacheNode(modX, y, modZ, false);
                    final Material matAtThisLoc = node.getType();
                    // if (!var1.getBlockState(var8).getMaterial().blocksMotion()) { 
                    // NOTE: the game assigns a "blocksMotion" flag to each and every block, as well as a "isSolid" one.
                    // Thus, a block's ability to obstruct motion isn't determined by its solidity (i.e.: moss block. See in PacketEvents' src, StateTypes.java)
                    // This also means that we cannot directly use BlockProperties#isSolid() to check for this (and NCP does not have this distinction yet).
                    // To hack around this, we can use the isGround() check instead, since all ground blocks are able to obstruct motion.
                    if (!BlockProperties.isGround(matAtThisLoc)) { 
                        if (BlockProperties.affectsFlow(blockCache, x, y, z, modX, y - 1, modZ, liquidTypeFlag)) {
                            modLiquidHeight = (float) BlockProperties.getLiquidHeightAt(blockCache, modX, y - 1, modZ, liquidTypeFlag, true); 
                            if (modLiquidHeight > 0.0F) {
                                flowForce = liquidHeight - (modLiquidHeight - 0.8888889f);
                            }
                        }
                    }
                } 
                else if (modLiquidHeight > 0.0f) {
                    flowForce = liquidHeight - modLiquidHeight;
                }
                if (flowForce != 0.0F) {
                    xModifier += (float) hDirection.getModX() * flowForce;
                    zModifier += (float) hDirection.getModZ() * flowForce;
                }
            }
        }
        // Compose the speed vector
        Vector flowingVector = new Vector(xModifier, 0.0, zModifier);
        IBlockCacheNode originalNode = blockCache.getOrCreateBlockCacheNode(x, y, z, false);
        if (BlockProperties.isLiquid(originalNode.getType()) && originalNode.getData(blockCache, x, y, z) >= 8) { // 8-15 - falling liquid
            for (BlockFace hDirection : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
                if (BlockProperties.isSolidFace(blockCache, (Player) entity, x, y, z, hDirection, liquidTypeFlag, entity.getLocation()) 
                    || BlockProperties.isSolidFace(blockCache, (Player) entity, x, y + 1, z, hDirection, liquidTypeFlag, entity.getLocation())) {
                    flowingVector = normalizedVectorWithoutNaN(flowingVector).add(new Vector(0.0D, -6.0D, 0.0D));
                    break;
                }
            }
        }
        return normalizedVectorWithoutNaN(flowingVector);
    }

    /**
     * Check if a player may climb upwards.<br>
     * Assuming this gets called after isOnClimbable returned true (with the player not moving from/to ground).<br>
     * Does not check for motion.
     *
     * @param jumpHeight
     *            Height the player is allowed to have jumped.
     * @return true, if successful
     */
    public boolean canClimbUp(double jumpHeight) {
        if (GenericVersion.isAtLeast(entity, "1.14")) {
            // Since 1.14, all climbable blocks are climbable upwards, always.
            return true;
        }
        // Force legacy clients to behave with legacy mechanics.
        if (BlockProperties.needsToBeAttachedToABlock(getTypeId())) {
            // Check if vine is attached to something solid
            if (BlockProperties.canClimbUp(blockCache, blockX, blockY, blockZ)) {
                return true;
            }
            // Check the block at head height.
            final int headY = Location.locToBlock(maxY);
            if (headY > blockY) {
                for (int cy = blockY + 1; cy <= headY; cy ++) {
                    if (BlockProperties.canClimbUp(blockCache, blockX, cy, blockZ)) {
                        return true;
                    }
                }
            }
            // Finally check possible jump height.
            // TODO: This too is inaccurate.
            if (isOnGround(jumpHeight)) {
                // Here ladders are ok.
                return true;
            }
            return false;
        }
        return true;
    }

    /**
     * Very coarse test to check if something solid/ground-like collides within the given margin above the eye height of the player, using a correction method.
     *
     * @param marginAboveEyeHeight
     *            the margin above eye height
     * @return True, if is head obstructed
     */
    public boolean seekCollisionAbove(double marginAboveEyeHeight) {
        return seekCollisionAbove(marginAboveEyeHeight, true);
    }

    /**
     * Very coarse test to check if something solid/ground-like collides within the given margin above the eye height of the player. <br>
     * For a better (and more accurate) method, use RichEntityLocation#collide().
     *
     * @param marginAboveEyeHeight
     *            Must be greater than or equal zero.
     * @param stepCorrection
     *            If set to true, a correction method is used for leniency, at the cost of accuracy.
     * @return True, if head is obstructed
     * @throws IllegalArgumentException
     *             If marginAboveEyeHeight is smaller than 0.
     */
    public boolean seekCollisionAbove(double marginAboveEyeHeight, boolean stepCorrection) {
        if (marginAboveEyeHeight < 0.0) {
            throw new IllegalArgumentException("marginAboveEyeHeight must be greater than 0.");
        }
        // Step correction: see https://github.com/NoCheatPlus/NoCheatPlus/commit/f22bf88824372de2207e6dca5e1c264f3d251897
        if (stepCorrection) {
            double obstrDistance = maxY + marginAboveEyeHeight;
            obstrDistance = obstrDistance - (double) Location.locToBlock(obstrDistance) + 0.35;
            for (double bound = 1.0; bound > 0.0; bound -= 0.25) {
                if (obstrDistance >= bound) {
                    // Use this level for correction.
                    marginAboveEyeHeight += bound + 0.35 - obstrDistance;
                    break;
                }
            }
        }
        return  BlockProperties.collides(blockCache, minX, maxY, minZ, maxX, maxY + marginAboveEyeHeight, maxZ, BlockFlags.F_GROUND | BlockFlags.F_SOLID)
                // Here the player's AABB would be INSIDE the block sideways(thus, the maxY's AABB would result as hitting the honey block above)
                && !isNextToBlock(0.01, BlockFlags.F_STICKY);
    }

    /**
     * Very coarse test to check if something solid/ground-like collides above the eye height of the player, using a correction method.
     *
     * @return True, if is head obstructed
     */
    public boolean seekCollisionAbove() {
        return seekCollisionAbove(0.0, true);
    }

    /**
     * Convenience constructor for using the maximum of mcAccess.getHeight() and
     * eye height for fullHeight.
     *
     * @param location
     *            the location
     * @param entity
     *            the entity
     * @param yOnGround
     *            the y on ground
     */
    public void set(final Location location, final Entity entity, final double yOnGround) {
        final MCAccess mcAccess = this.mcAccess.getHandle();
        doSet(location, entity, mcAccess.getWidth(entity), mcAccess.getHeight(entity), yOnGround);
    }

    /**
     * 
     *
     * @param location
     *            the location
     * @param entity
     *            the entity
     * @param fullHeight
     *            Allows to specify eyeHeight here. Currently might be
     *            overridden by eyeHeight, if that is greater.
     * @param yOnGround
     *            the y on ground
     */
    public void set(final Location location, final Entity entity, double fullHeight, final double yOnGround) {
        final MCAccess mcAccess = this.mcAccess.getHandle();
        doSet(location, entity, mcAccess.getWidth(entity), fullHeight, yOnGround);
    }

    /**
     * 
     *
     * @param location
     *            the location
     * @param entity
     *            the entity
     * @param fullWidth
     *            Override the bounding box width (full width).
     * @param fullHeight
     *            Allows to specify eyeHeight here. Currently might be
     *            overridden by eyeHeight, if that is greater.
     * @param yOnGround
     *            the y on ground
     */
    public void set(final Location location, final Entity entity, final double fullWidth, double fullHeight, final double yOnGround) {
        doSet(location, entity, fullWidth, fullHeight, yOnGround);
    }

    /**
     * Do set.<br>
     * For the bounding box height, the maximum of given fullHeight 
     * and entity height is used. Sets isLiving and
     * eyeHeight.
     *
     * @param location
     *            the location
     * @param entity
     *            the entity
     * @param fullWidth
     *            the full width
     * @param fullHeight
     *            the full height
     * @param yOnGround
     *            the y on ground
     */
    protected void doSet(final Location location, final Entity entity, final double fullWidth, double fullHeight, final double yOnGround) {
        final double eyeHeight;
        final boolean isLiving;
        if (entity instanceof LivingEntity) {
            isLiving = true;
            final LivingEntity living = (LivingEntity) entity;
            eyeHeight = living.getEyeHeight();
            fullHeight = Math.max(fullHeight, eyeHeight);
        }
        else {
            isLiving = false;
            eyeHeight = fullHeight;
        }
        doSetExactHeight(location, entity, isLiving, fullWidth, eyeHeight, fullHeight, fullHeight, yOnGround);
    }

    /**
     * 
     * @param location
     * @param entity
     * @param isLiving
     * @param fullWidth
     * @param eyeHeight
     * @param height
     *            Set as height (as in entity.height).
     * @param fullHeight
     *            Bounding box height.
     * @param yOnGround
     */
    protected void doSetExactHeight(final Location location, final Entity entity, final boolean isLiving, 
                                    final double fullWidth, final double eyeHeight, final double height, 
                                    final double fullHeight, final double yOnGround) {
        this.entity = entity;
        this.isLiving = isLiving;
        final MCAccess mcAccess = this.mcAccess.getHandle();
        this.width = mcAccess.getWidth(entity);
        this.eyeHeight = eyeHeight;
        this.height = mcAccess.getHeight(entity);
        standsOnEntity = false;
        super.set(location, fullWidth, fullHeight, yOnGround);
    }

    /**
     * Not supported.
     *
     * @param location
     *            the location
     * @param fullWidth
     *            the full width
     * @param fullHeight
     *            the full height
     * @param yOnGround
     *            the y on ground
     */
    @Override
    public void set(Location location, double fullWidth, double fullHeight, double yOnGround) {
        throw new UnsupportedOperationException("Set must specify an instance of Entity.");
    }

    /**
     * Set cached info according to other.<br>
     * Minimal optimizations: take block flags directly, on-ground max/min
     * bounds, only set stairs if not on ground and not reset-condition.
     *
     * @param other
     *            the other
     */
    public void prepare(final RichEntityLocation other) {
        super.prepare(other);
        this.standsOnEntity = other.standsOnEntity;
    }

    /**
     * Set some references to null.
     */
    public void cleanup() {
        super.cleanup();
        entity = null;
    }

    /* (non-Javadoc)
     * @see fr.neatmonster.nocheatplus.utilities.RichBoundsLocation#toString()
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(128);
        builder.append("RichEntityLocation(");
        builder.append(world == null ? "null" : world.getName());
        builder.append('/');
        builder.append(Double.toString(x));
        builder.append(", ");
        builder.append(Double.toString(y));
        builder.append(", ");
        builder.append(Double.toString(z));
        builder.append(')');
        return builder.toString();
    }

}
