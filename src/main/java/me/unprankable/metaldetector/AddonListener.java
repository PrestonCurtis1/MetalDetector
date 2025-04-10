package me.unprankable.metaldetector;
import me.unprankable.metaldetector.Earth.Metal.MetalDetector;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.BendingPlayer;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
public class AddonListener implements Listener {
    @EventHandler
    public void MetalDetectorListener(PlayerToggleSneakEvent event) {
        Player p = event.getPlayer();
        BendingPlayer bp = BendingPlayer.getBendingPlayer(p);
        if(event.isCancelled() || bp == null || !bp.canUseSubElement(Element.METAL) || !bp.isToggled() || !bp.isElementToggled(Element.EARTH) || p.getGameMode() == GameMode.SPECTATOR || bp.isOnCooldown("MetalDetector")){
            return;
        }else if (bp.getBoundAbilityName().equalsIgnoreCase("MetalDetector")) {
            new MetalDetector(p);
        }
    }

}