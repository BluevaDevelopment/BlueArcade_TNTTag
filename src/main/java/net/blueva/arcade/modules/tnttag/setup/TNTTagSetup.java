package net.blueva.arcade.modules.tnttag.setup;

import net.blueva.arcade.api.setup.GameSetupHandler;
import net.blueva.arcade.api.setup.SetupContext;
import net.blueva.arcade.api.setup.SetupDataAPI;
import net.blueva.arcade.api.setup.TabCompleteContext;
import net.blueva.arcade.api.setup.TabCompleteResult;
import net.blueva.arcade.modules.tnttag.TNTTagModule;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class TNTTagSetup implements GameSetupHandler {

    private final TNTTagModule module;

    public TNTTagSetup(TNTTagModule module) {
        this.module = module;
    }

    @Override
    public boolean handle(SetupContext context) {
        return handleInternal(castSetupContext(context));
    }

    private boolean handleInternal(SetupContext<Player, CommandSender, Location> context) {
        if (!context.hasHandlerArgs(1)) {
            context.getMessagesAPI().send(context.getPlayer(),
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.usage"));
            return true;
        }

        String subcommand = context.getArg(context.getStartIndex() - 1).toLowerCase();
        if ("setregion".equals(subcommand)) {
            return handleSetRegion(context);
        }

        context.getMessagesAPI().send(context.getPlayer(),
                module.getCoreConfig().getLanguage("admin_commands.errors.unknown_subcommand"));
        return true;
    }

    @Override
    public TabCompleteResult tabComplete(TabCompleteContext context) {
        return tabCompleteInternal(castTabContext(context));
    }

    private TabCompleteResult tabCompleteInternal(TabCompleteContext<Player, CommandSender> context) {
        int relIndex = context.getRelativeArgIndex();

        if (relIndex == 0 && "setregion".equals(context.getArg(context.getStartIndex() - 1))) {
            return TabCompleteResult.of("selection");
        }

        return TabCompleteResult.empty();
    }

    @Override
    public List<String> getSubcommands() {
        return Collections.singletonList("setregion");
    }

    @Override
    public boolean validateConfig(SetupContext context) {
        return validateConfigInternal(castSetupContext(context));
    }

    private boolean validateConfigInternal(SetupContext<Player, CommandSender, Location> context) {
        SetupDataAPI data = context.getData();

        boolean hasRegion = data.has("game.play_area.bounds.min.x") && data.has("game.play_area.bounds.max.x");

        if (!hasRegion && context.getSender() != null) {
            context.getMessagesAPI().send(context.getPlayer(),
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.not_configured")
                            .replace("{arena_id}", String.valueOf(context.getArenaId())));
        }

        return hasRegion;
    }

    private boolean handleSetRegion(SetupContext<Player, CommandSender, Location> context) {
        Player player = context.getPlayer();

        if (!context.getSelection().hasCompleteSelection(player)) {
            context.getMessagesAPI().send(player,
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.must_use_stick"));
            return true;
        }

        Location pos1 = context.getSelection().getPosition1(player);
        Location pos2 = context.getSelection().getPosition2(player);

        context.getData().registerRegenerationRegion("game.play_area", pos1, pos2);
        context.getData().save();

        int x = (int) Math.abs(pos2.getX() - pos1.getX()) + 1;
        int y = (int) Math.abs(pos2.getY() - pos1.getY()) + 1;
        int z = (int) Math.abs(pos2.getZ() - pos1.getZ()) + 1;
        int blocks = x * y * z;

        context.getMessagesAPI().send(player,
                module.getModuleConfig().getStringFrom("language.yml", "setup_messages.region_set")
                        .replace("{blocks}", String.valueOf(blocks))
                        .replace("{x}", String.valueOf(x))
                        .replace("{y}", String.valueOf(y))
                        .replace("{z}", String.valueOf(z)));
        return true;
    }


    @SuppressWarnings("unchecked")
    private SetupContext<Player, CommandSender, Location> castSetupContext(SetupContext context) {
        return (SetupContext<Player, CommandSender, Location>) context;
    }

    @SuppressWarnings("unchecked")
    private TabCompleteContext<Player, CommandSender> castTabContext(TabCompleteContext context) {
        return (TabCompleteContext<Player, CommandSender>) context;
    }
}
