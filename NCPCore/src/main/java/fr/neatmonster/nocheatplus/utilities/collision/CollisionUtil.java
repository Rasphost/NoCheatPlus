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
package fr.neatmonster.nocheatplus.utilities.collision;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.utilities.location.TrigUtil;

/**
 * Collision related static utility.
 * 
 * @author asofold
 *
 */
public class CollisionUtil {

    /** Temporary use, setWorld(null) once finished. */
    private static final Location useLoc = new Location(null, 0, 0, 0);

    /**
     * Check if a player looks at a target of a specific size, with a specific
     * precision value (roughly).
     *
     * @param player
     *            the player
     * @param targetX
     *            the target x
     * @param targetY
     *            the target y
     * @param targetZ
     *            the target z
     * @param targetWidth
     *            the target width
     * @param targetHeight
     *            the target height
     * @param precision
     *            the precision
     * @return the double
     */
    public static double directionCheck(final Player player, final double targetX, final double targetY, final double targetZ, final double targetWidth, final double targetHeight, final double precision)
    {
        final Location loc = player.getLocation(useLoc);
        final Vector dir = loc.getDirection();
        final double res = directionCheck(loc.getX(), loc.getY() + player.getEyeHeight(), loc.getZ(), dir.getX(), dir.getY(), dir.getZ(), targetX, targetY, targetZ, targetWidth, targetHeight, precision);
        useLoc.setWorld(null);
        return res;
    }

    /**
     * Convenience method.
     *
     * @param sourceFoot
     *            the source foot
     * @param eyeHeight
     *            the eye height
     * @param dir
     *            the dir
     * @param target
     *            the target
     * @param precision
     *            (width/height are set to 1)
     * @return the double
     */
    public static double directionCheck(final Location sourceFoot, final double eyeHeight, final Vector dir, final Block target, final double precision)
    {
        return directionCheck(sourceFoot.getX(), sourceFoot.getY() + eyeHeight, sourceFoot.getZ(), dir.getX(), dir.getY(), dir.getZ(), target.getX(), target.getY(), target.getZ(), 1, 1, precision);
    }

    /**
     * Convenience method.
     *
     * @param sourceFoot
     *            the source foot
     * @param eyeHeight
     *            the eye height
     * @param dir
     *            the dir
     * @param targetX
     *            the target x
     * @param targetY
     *            the target y
     * @param targetZ
     *            the target z
     * @param targetWidth
     *            the target width
     * @param targetHeight
     *            the target height
     * @param precision
     *            the precision
     * @return the double
     */
    public static double directionCheck(final Location sourceFoot, final double eyeHeight, final Vector dir, final double targetX, final double targetY, final double targetZ, final double targetWidth, final double targetHeight, final double precision)
    {
        return directionCheck(sourceFoot.getX(), sourceFoot.getY() + eyeHeight, sourceFoot.getZ(), dir.getX(), dir.getY(), dir.getZ(), targetX, targetY, targetZ, targetWidth, targetHeight, precision);					
    }

    /**
     * Check how far the looking direction is off the target.
     *
     * @param sourceX
     *            Source location of looking direction.
     * @param sourceY
     *            the source y
     * @param sourceZ
     *            the source z
     * @param dirX
     *            Looking direction.
     * @param dirY
     *            the dir y
     * @param dirZ
     *            the dir z
     * @param targetX
     *            Location that should be looked towards.
     * @param targetY
     *            the target y
     * @param targetZ
     *            the target z
     * @param targetWidth
     *            xz extent
     * @param targetHeight
     *            y extent
     * @param precision
     *            the precision
     * @return Some offset.
     */
    public static double directionCheck(final double sourceX, final double sourceY, final double sourceZ, final double dirX, final double dirY, final double dirZ, final double targetX, final double targetY, final double targetZ, final double targetWidth, final double targetHeight, final double precision)
    {

        //		// TODO: Here we have 0.x vs. 2.x, sometimes !
        //		NCPAPIProvider.getNoCheatPlusAPI().getLogManager().debug(Streams.TRACE_FILE, "COMBINED: " + combinedDirectionCheck(sourceX, sourceY, sourceZ, dirX, dirY, dirZ, targetX, targetY, targetZ, targetWidth, targetHeight, precision, 60));

        // TODO: rework / standardize.

        double dirLength = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (dirLength == 0.0) dirLength = 1.0; // ...

        final double dX = targetX - sourceX;
        final double dY = targetY - sourceY;
        final double dZ = targetZ - sourceZ;

        final double targetDist = Math.sqrt(dX * dX + dY * dY + dZ * dZ);

        final double xPrediction = targetDist * dirX / dirLength;
        final double yPrediction = targetDist * dirY / dirLength;
        final double zPrediction = targetDist * dirZ / dirLength;

        double off = 0.0D;

        off += Math.max(Math.abs(dX - xPrediction) - (targetWidth / 2 + precision), 0.0D);
        off += Math.max(Math.abs(dZ - zPrediction) - (targetWidth / 2 + precision), 0.0D);
        off += Math.max(Math.abs(dY - yPrediction) - (targetHeight / 2 + precision), 0.0D);

        if (off > 1) off = Math.sqrt(off);

        return off;
    }

    /**
     * Combined direction check.
     *
     * @param sourceFoot
     *            the source foot
     * @param eyeHeight
     *            the eye height
     * @param dir
     *            the dir
     * @param targetX
     *            the target x
     * @param targetY
     *            the target y
     * @param targetZ
     *            the target z
     * @param targetWidth
     *            the target width
     * @param targetHeight
     *            the target height
     * @param precision
     *            the precision
     * @param anglePrecision
     *            the angle precision
     * @return the double
     */
    public static double combinedDirectionCheck(final Location sourceFoot, final double eyeHeight, final Vector dir, final double targetX, final double targetY, final double targetZ, final double targetWidth, final double targetHeight, final double precision, final double anglePrecision)
    {
        return combinedDirectionCheck(sourceFoot.getX(), sourceFoot.getY() + eyeHeight, sourceFoot.getZ(), dir.getX(), dir.getY(), dir.getZ(), targetX, targetY, targetZ, targetWidth, targetHeight, precision, anglePrecision);					
    }

    /**
     * Combined direction check.
     *
     * @param sourceFoot
     *            the source foot
     * @param eyeHeight
     *            the eye height
     * @param dir
     *            the dir
     * @param target
     *            the target
     * @param precision
     *            the precision
     * @param anglePrecision
     *            the angle precision
     * @return the double
     */
    public static double combinedDirectionCheck(final Location sourceFoot, final double eyeHeight, final Vector dir, final Block target, final double precision, final double anglePrecision)
    {
        return combinedDirectionCheck(sourceFoot.getX(), sourceFoot.getY() + eyeHeight, sourceFoot.getZ(), dir.getX(), dir.getY(), dir.getZ(), target.getX(), target.getY(), target.getZ(), 1, 1, precision, anglePrecision);
    }

    /**
     * Combine directionCheck with angle, in order to prevent low-distance
     * abuse.
     *
     * @param sourceX
     *            the source x
     * @param sourceY
     *            the source y
     * @param sourceZ
     *            the source z
     * @param dirX
     *            the dir x
     * @param dirY
     *            the dir y
     * @param dirZ
     *            the dir z
     * @param targetX
     *            the target x
     * @param targetY
     *            the target y
     * @param targetZ
     *            the target z
     * @param targetWidth
     *            the target width
     * @param targetHeight
     *            the target height
     * @param blockPrecision
     *            the block precision
     * @param anglePrecision
     *            Precision in grad.
     * @return the double
     */
    public static double combinedDirectionCheck(final double sourceX, final double sourceY, final double sourceZ, final double dirX, final double dirY, final double dirZ, final double targetX, final double targetY, final double targetZ, final double targetWidth, final double targetHeight, final double blockPrecision, final double anglePrecision)
    {
        double dirLength = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (dirLength == 0.0) dirLength = 1.0; // ...

        final double dX = targetX - sourceX;
        final double dY = targetY - sourceY;
        final double dZ = targetZ - sourceZ;

        final double targetDist = Math.sqrt(dX * dX + dY * dY + dZ * dZ);

        if (targetDist > Math.max(targetHeight, targetWidth) / 2.0 && TrigUtil.angle(sourceX, sourceY, sourceZ, dirX, dirY, dirZ, targetX, targetY, targetZ) * TrigUtil.fRadToGrad > anglePrecision){
            return targetDist - Math.max(targetHeight, targetWidth) / 2.0;
        }

        final double xPrediction = targetDist * dirX / dirLength;
        final double yPrediction = targetDist * dirY / dirLength;
        final double zPrediction = targetDist * dirZ / dirLength;

        double off = 0.0D;

        off += Math.max(Math.abs(dX - xPrediction) - (targetWidth / 2 + blockPrecision), 0.0D);
        off += Math.max(Math.abs(dY - yPrediction) - (targetHeight / 2 + blockPrecision), 0.0D);
        off += Math.max(Math.abs(dZ - zPrediction) - (targetWidth / 2 + blockPrecision), 0.0D);

        if (off > 1) off = Math.sqrt(off);

        return off;
    }

    /**
     * Test if the block coordinate is intersecting with min+max bounds,
     * assuming the a full block. Excludes the case of only the edges
     * intersecting.
     *
     * @param min
     *            the min
     * @param max
     *            the max
     * @param block
     *            Block coordinate of the block.
     * @return true, if successful
     */
    public static boolean intersectsBlock(final double min, final double max, final int block) {
        final double db = (double) block;
        return db + 1.0 > min && db < max;
    }

}
