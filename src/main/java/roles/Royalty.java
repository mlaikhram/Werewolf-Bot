package roles;

import model.Role;
import model.RoleStatus;
import model.WerewolfSession;
import net.dv8tion.jda.api.entities.User;

public class Royalty extends Role {

    public Royalty(String nickName, User user, WerewolfSession session) {
        super("Royalty", nickName, false, user, session);
        this.status = RoleStatus.ALIVE;
        user.openPrivateChannel().queue((channel) -> {
            channel.sendMessage("You are Royalty. You will be trying to execute the werewolves each day. You cannot be executed by the Villagers").queue();
        });
    }

    @Override
    public boolean prompt() {
        if (status == RoleStatus.ALIVE) {
            user.openPrivateChannel().queue((channel) -> {
                channel.sendMessage( session.getRolesPrompt(false) + "You are Royalty. Select the player that you think is a Werewolf").queue();
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
    public boolean execute() {
        session.getChannel().sendMessage(getNickName() + " is Royalty and cannot be executed by the Villagers!").queue();
        return false;
    }
}
