package xyz.tehbrian.buildersutilities.option;

import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.tehbrian.buildersutilities.BuildersUtilities;
import xyz.tehbrian.buildersutilities.Constants;
import xyz.tehbrian.buildersutilities.user.UserService;
import xyz.tehbrian.restrictionhelper.core.ActionType;
import xyz.tehbrian.restrictionhelper.spigot.SpigotRestrictionHelper;

import java.util.Objects;

@SuppressWarnings("unused")
public final class GlazedTerracottaListener implements Listener {

    private final BuildersUtilities buildersUtilities;
    private final UserService userService;
    private final SpigotRestrictionHelper restrictionHelper;

    @Inject
    public GlazedTerracottaListener(
            final @NonNull BuildersUtilities buildersUtilities,
            final @NonNull UserService userService,
            final @NonNull SpigotRestrictionHelper restrictionHelper
    ) {
        this.buildersUtilities = buildersUtilities;
        this.userService = userService;
        this.restrictionHelper = restrictionHelper;
    }

    @EventHandler(ignoreCancelled = true)
    public void onGlazedTerracottaInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();

        if (!this.userService.getUser(player).glazedTerracottaRotateEnabled()
                || !player.hasPermission(Constants.Permissions.GLAZED_TERRACOTTA_ROTATE)) {
            return;
        }

        final Block block = Objects.requireNonNull(event.getClickedBlock());

        if (!block.getType().name().toLowerCase().contains("glazed")
                || player.getInventory().getItemInMainHand().getType() != Material.AIR
                || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getHand() != EquipmentSlot.HAND
                || player.getGameMode() != GameMode.CREATIVE
                || !player.isSneaking()
                || !this.restrictionHelper.checkRestrictions(player, block.getLocation(), ActionType.BREAK)
                || !this.restrictionHelper.checkRestrictions(player, block.getLocation(), ActionType.PLACE)) {
            return;
        }

        Bukkit.getScheduler().runTask(this.buildersUtilities, () -> {
            final Directional directional = (Directional) block.getBlockData();

            switch (directional.getFacing()) {
                case NORTH:
                    directional.setFacing(BlockFace.EAST);
                    break;
                case EAST:
                    directional.setFacing(BlockFace.SOUTH);
                    break;
                case SOUTH:
                    directional.setFacing(BlockFace.WEST);
                    break;
                case WEST:
                    directional.setFacing(BlockFace.NORTH);
                    break;
                default:
                    break;
            }

            block.setBlockData(directional);
        });
        event.setCancelled(true);
    }

}
