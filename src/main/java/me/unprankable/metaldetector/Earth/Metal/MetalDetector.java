package me.unprankable.metaldetector.Earth.Metal;


import me.unprankable.metaldetector.AddonListener;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import org.bukkit.event.EventHandler;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.joml.Matrix4f;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import java.util.*;
import static com.projectkorra.projectkorra.ProjectKorra.plugin;
import org.bukkit.event.block.BlockBreakEvent;

public class MetalDetector extends MetalAbility implements AddonAbility,Listener {
    private Location eyeLoc;
    private Listener MDL;
    private long startTime;
    private List<BlockDisplay> highlighted = new ArrayList<>();
    private List<Location> oreList;
    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DURATION)
    private long duration;
    @Attribute("Reach")
    private int reach;
    private Permission perm;
    static String path = "unprankable.MetalDetector";
    public MetalDetector(Player player) {
        super(player);

        if (bPlayer.isOnCooldown(this)) {
            return;
        }
        if (!bPlayer.canBend(this)) {
            return;
        }
        if (hasAbility(player, MetalDetector.class)) {
            MetalDetector md = getAbility(player, MetalDetector.class);
            if (md.isStarted()) {
                return;
            }
        }
        this.startTime = System.currentTimeMillis();
        highlighted.clear();
        setFields();

        eyeLoc = player.getEyeLocation();
        oreList = findOres(this.eyeLoc, this.reach);
        if (!oreList.isEmpty()) {
            for (Location ore: oreList) {
                Color color;
                if (ore.getWorld().getBlockAt(ore.getBlockX(),ore.getBlockY(),ore.getBlockZ()).getType().name().toLowerCase().contains("gold")){
                    color = Color.YELLOW;
                } else if (ore.getWorld().getBlockAt(ore.getBlockX(),ore.getBlockY(),ore.getBlockZ()).getType().name().toLowerCase().contains("copper")){
                    color = Color.ORANGE;
                } else if (ore.getWorld().getBlockAt(ore.getBlockX(),ore.getBlockY(),ore.getBlockZ()).getType().name().contains("iron")){
                    color = Color.SILVER;
                } else {
                    color = Color.WHITE;
                }
                applyGlowingBlock(ore,color);
            }
        }
        Location usedAt = bPlayer.getPlayer().getLocation();
        ProjectKorra.log.info(bPlayer.getName() + " Used MetalDetector at " + usedAt.getBlockX() + " " + usedAt.getBlockY() + " " + usedAt.getBlockZ() + "");
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) this.duration/50, 0));
        start();
    }


    public void setFields(){
        this.cooldown = ConfigManager.getConfig().getLong(path + "Cooldown");
        this.duration = ConfigManager.getConfig().getLong(path+"Duration");
        this.reach = ConfigManager.getConfig().getInt(path+"Reach");
    }


        public void applyGlowingBlock(Location blockloc, Color color) {
        Location centerblockloc = blockloc.clone().add(-1.49, -1.99, -1.49);
        World world = centerblockloc.getWorld();
        if (world == null)return;
         // Ensure world is not null
        BlockDisplay display = (BlockDisplay) world.spawnEntity(centerblockloc.add(1.5,2,1.5),EntityType.BLOCK_DISPLAY);//spawn and center the block display
        display.setGlowing(true);
        display.setGlowColorOverride(color);
        display.setBlock(Bukkit.createBlockData(centerblockloc.getBlock().getType()));
        display.setTransformationMatrix(new Matrix4f().scale(0.98f, 0.98f, 0.98f));
        display.setVisibleByDefault(false);
        display.setCustomName("MetalDetector");
        Player viewer = bPlayer.getPlayer();
        viewer.showEntity(plugin, display);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("bending.ability.MetalDetector.viewOthers")) {
                player.showEntity(plugin, display);
            }
        }
        this.highlighted.add(display);
    }



    public boolean MetalBendable(Material material){
        return isMetal(material);
    }

    public List<Location> findOres(Location center, int radius){
        List<Location> ores = new ArrayList<>();
        World world = center.getWorld();
        int centerX = (int) center.getX();
        int centerY = (int) center.getY();
        int centerZ = (int) center.getZ();
        for (int x = centerX - radius; x <= centerX + radius; x++){
            for (int y = centerY - radius; y <= centerY + radius; y++){
                for (int z = centerZ - radius; z <= centerZ + radius; z++){
                    assert world != null;
                    Block block = world.getBlockAt(x, y ,z);
                    if (MetalBendable(block.getType())){
                        ores.add(block.getLocation());
                    }
                }
            }
        }
        return ores;
    }


    @Override
    public void progress() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        // Check if a second has passed since the last countdown log
        if (elapsedTime >= duration | !player.isSneaking()) {
            // Ability duration has elapsed, deactivate the ability

            endMD();
        }else {
            for (int index = 0; index < highlighted.size(); index++) {
                BlockDisplay display = highlighted.get(index);
                if (display.getLocation().getBlock().getType() == Material.AIR) {

                    display.remove();
                    highlighted.remove(index);
                    oreList.remove(index);
                }
            }

        }

        // Ability logic for each tick goes here
    }


    public void endMD(){
        bPlayer.addCooldown(this);
        for(BlockDisplay ore: highlighted){
            ore.remove();
        }
        remove();
    }


    @Override
    public void stop() {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=minecraft:block_display,name=\"MetalDetector\"]");
        endMD();
        ProjectKorra.log.info("Successfully disabled " + getName() + "by" + getAuthor());
        // Clean up any resources or effects when the ability is manually stopped
        plugin.getServer().getPluginManager().removePermission(this.perm);
        HandlerList.unregisterAll(this.MDL);
    }




    @Override
    public String getName() {
        return "MetalDetector";
    }


    @Override
    public String getDescription() {
        return "Allows metalbenders to sense the metal arround them";
    }


    @Override
    public String getInstructions() {
        return "Hold Sneak";
    }


    @Override
    public String getAuthor() {
        return "Unprankable";
    }


    @Override
    public String getVersion() {
        return "1.2";
    }


    @Override
    public boolean isHarmlessAbility() {
        return true;
    }


    @Override
    public boolean isSneakAbility() {
        return true;
    }


    @Override
    public long getCooldown() {
        return this.cooldown;
    }


    @Override
    public Location getLocation() {
        return this.eyeLoc;
    }


    @Override
    public void load() {
        this.MDL = new AddonListener();
        Bukkit.getPluginManager().registerEvents(this.MDL, plugin);

        // Set default configuration values
        ConfigManager.defaultConfig.get().addDefault(path + "Cooldown", 5000);
        ConfigManager.defaultConfig.get().addDefault(path + "Duration", 3000);
        ConfigManager.defaultConfig.get().addDefault(path + "Reach",10);
        // Save the default configuration
        ConfigManager.defaultConfig.save();

        // Log that the ability has been loaded
        perm = new Permission("bending.ability.MetalDetector");
        perm = new Permission("bending.ability.MetalDetector.viewOthers");
        perm.setDefault(PermissionDefault.FALSE);
        perm.setDefault(PermissionDefault.TRUE);
        ProjectKorra.log.info(this.getName() + " by " + this.getAuthor() + " " + this.getVersion() + " has been loaded!");

    }
}