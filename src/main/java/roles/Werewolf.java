package roles;

import model.Role;
import model.RoleStatus;
import model.WerewolfSession;
import net.dv8tion.jda.api.entities.User;

public class Werewolf extends Role {

    public Werewolf(String nickName, User user, WerewolfSession session) {
        super("Werewolf", nickName, true, user, session);
        this.status = RoleStatus.ALIVE;
        user.openPrivateChannel().queue((channel) -> {
            channel.sendMessage("You are a Werewolf. You will be killing players each night").queue();
        });
    }

    @Override
    public boolean prompt() {
        if (status == RoleStatus.ALIVE) {
            user.openPrivateChannel().queue((channel) -> {
                channel.sendMessage(session.getRolesPrompt(false) + "You are a werewolf. Just pick anyone for now (this will not affect anything)").queue();
            });
            return true;
        }
        return false;
    }

    @Override
    public boolean applyResponse(String response) {
        respondedThisRound = true;
        return true;
    }
}
