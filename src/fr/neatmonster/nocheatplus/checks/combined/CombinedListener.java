package fr.neatmonster.nocheatplus.checks.combined;

import net.minecraft.server.EntityPlayer;

import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;

import fr.neatmonster.nocheatplus.utilities.TickTask;

/**
 * GarbageCollectior class to combine some things, make available for other checks, or just because they don't fit into another section.
 * @author mc_dev
 *
 */
public class CombinedListener implements Listener {
	
	protected final Improbable improbable;

	public CombinedListener(){
		this.improbable = new Improbable();
	}
	
	@EventHandler(priority=EventPriority.LOW)
	public void onPlayerToggleSneak(final PlayerToggleSneakEvent event){
		// Check also in case of cancelled events.
		if (Improbable.check(event.getPlayer(), 0.35f, System.currentTimeMillis())) event.setCancelled(true);
	}
	
	@EventHandler(priority=EventPriority.LOW)
	public void onPlayerToggleSprint(final PlayerToggleSprintEvent event){
		// Check also in case of cancelled events.
		if (Improbable.check(event.getPlayer(), 0.35f, System.currentTimeMillis())) event.setCancelled(true);
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = false)
	public final void onPlayerMove(final PlayerMoveEvent event){
		final long now = System.currentTimeMillis();
		final Player player = event.getPlayer();
		
		final Location loc = event.getTo(); // player.getLocation();
		final String worldName = loc.getWorld().getName();
		final CombinedData data = CombinedData.getData(player);
		data.lastMoveTime = now;
		// Just add the yaw to the list.
		Combined.feedYawRate(player, loc.getYaw(), now, worldName, data);
	}
	
    /**
     * We listen to this event to prevent players from leaving while falling, so from avoiding fall damages.
     * 
     * @param event
     *            the event
     */
    @EventHandler(
            priority = EventPriority.LOWEST)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        
        // TODO: EventPriority
        
        final Player player = event.getPlayer();
        final CombinedData data = CombinedData.getData(player);
        final CombinedConfig cc = CombinedConfig.getConfig(player);
        
        if (cc.invulnerableCheck && (cc.invulnerableTriggerAlways || cc.invulnerableTriggerFallDistance && player.getFallDistance() > 0)){
            // TODO: maybe make a heuristic for small fall distances with ground under feet (prevents future abuse with jumping) ?
            final EntityPlayer mcPlayer= ((CraftPlayer) player).getHandle();
            final int ticks = cc.invulnerableInitialTicksJoin >= 0 ? cc.invulnerableInitialTicksJoin : mcPlayer.invulnerableTicks;
            data.invulnerableTick = TickTask.getTick() + ticks;
            mcPlayer.invulnerableTicks = 0;
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onEntityDamage(final EntityDamageEvent event){
        final Entity entity = event.getEntity();
        if (!(entity instanceof Player)) return;
        final Player  player = (Player) entity;
        final CombinedConfig cc = CombinedConfig.getConfig(player);
        if (!cc.invulnerableCheck) return;
        final DamageCause cause = event.getCause();
        // Ignored causes.
        if (cc.invulnerableIgnore.contains(cause)) return;
        // Modified invulnerable ticks.
        Integer modifier = cc.invulnerableModifiers.get(cause);
        if (modifier == null) modifier = cc.invulnerableModifierDefault;
        final CombinedData data = CombinedData.getData(player);
        if (TickTask.getTick() >= data.invulnerableTick + modifier.intValue()) return;
        // Still invulnerable.
        event.setCancelled(true);
    }
	
}
