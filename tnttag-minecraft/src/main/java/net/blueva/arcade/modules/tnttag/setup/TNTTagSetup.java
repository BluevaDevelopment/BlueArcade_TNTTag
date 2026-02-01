package net.blueva.arcade.modules.tnttag.setup;

import net.blueva.arcade.api.setup.GameSetupHandler;
import net.blueva.arcade.api.setup.SetupContext;
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
        context.getMessagesAPI().send(context.getPlayer(),
                module.getModuleConfig().getStringFrom("language.yml", "setup_messages.no_extra_steps"));
        return true;
    }

    @Override
    public TabCompleteResult tabComplete(TabCompleteContext context) {
        return tabCompleteInternal(castTabContext(context));
    }

    private TabCompleteResult tabCompleteInternal(TabCompleteContext<Player, CommandSender> context) {
        return TabCompleteResult.empty();
    }

    @Override
    public List<String> getSubcommands() {
        return Collections.emptyList();
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
