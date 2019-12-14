package roles;

import model.Role;
import model.RoleStatus;
import model.WerewolfSession;
import net.dv8tion.jda.api.entities.User;

public class Lycan extends Role {

    public Lycan(String nickName, User user, WerewolfSession session) {
        super("Lycan", nickName, false, user, session);
        this.status = RoleStatus.ALIVE;
        user.openPrivateChannel().queue((channel) -> {
            channel.sendMessage("You are a Lycan. You will be trying to execute the werewolves each day. You will appear as a Werewolf to the Seer").queue();
        });
    }

    @Override
    public boolean prompt() {
        if (status == RoleStatus.ALIVE) {
            user.openPrivateChannel().queue((channel) -> {
                channel.sendMessage( session.getRolesPrompt(false) + "You are a Lycan. Select the player that you think is a Werewolf").queue();
            });
            return true;
        }
        return false;
    }

    @Override
    public boolean applyResponse(String response) {
        try {
            int index = Integer.parseInt(response);
            Role target = session.getRoleByIndex(index);
            // TODO: Track suspicion stats
            user.openPrivateChannel().queue((channel) -> {
                channel.sendMessage("Response recorded. Please wait for all players to finish").queue();
            });
            respondedThisRound = true;
            return true;
        }
        catch (Exception e) {
            user.openPrivateChannel().queue((channel) -> {
                channel.sendMessage( "Please enter a number that corresponds to a player").queue();
            });
            return false;
        }
    }

    @Override
    public boolean inspect() {
        return true;
    }
}
