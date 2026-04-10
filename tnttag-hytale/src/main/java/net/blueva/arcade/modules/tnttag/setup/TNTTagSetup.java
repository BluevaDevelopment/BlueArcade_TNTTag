package net.blueva.arcade.modules.tnttag.setup;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.setup.GameSetupHandler;
import net.blueva.arcade.api.setup.SetupContext;
import net.blueva.arcade.api.setup.TabCompleteContext;
import net.blueva.arcade.api.setup.TabCompleteResult;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.entities.Player;

import java.util.Collections;
import java.util.List;

public class TNTTagSetup implements GameSetupHandler {

    private final ModuleConfigAPI moduleConfig;
    public TNTTagSetup(ModuleConfigAPI moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    @Override
    public boolean handle(SetupContext context) {
        return handleInternal(castSetupContext(context));
    }

    private boolean handleInternal(SetupContext<Player, CommandSender, Location> context) {
        context.getMessagesAPI().sendRaw(context.getPlayer(),
                moduleConfig.getStringFrom("language.yml", "setup_messages.no_extra_steps"));
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
