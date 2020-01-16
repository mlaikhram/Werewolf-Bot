package roles;

import model.Role;
import model.RoleStatus;
import model.WerewolfSession;
import net.dv8tion.jda.api.entities.User;

public class ToughGuy extends Role {

    private boolean attacked;

    public ToughGuy(String nickName, User user, WerewolfSession session) {
        super("Tough Guy", nickName, false, user, session);
        this.status = RoleStatus.ALIVE;
        this.attacked = false;
        user.openPrivateChannel().queue((channel) -> {
            channel.sendMessage("You are a Tough Guy. You will be trying to execute the werewolves each day. If you are killed by the Werewolves you will instead start bleeding out and die one turn later").queue();
        });
    }

    @Override
    public boolean prompt() {
        if (status == RoleStatus.ALIVE) {
            user.openPrivateChannel().queue((channel) -> {
                channel.sendMessage( session.getRolesPrompt(false) + "You are a Tough Guy." + (attacked ? "You are bleeding out and will die unless you are protected." : "") + " Select the player that you think is a Werewolf").queue();
            });
            if (attacked) {
                session.addVictim(user.getId());
            }
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
    public boolean kill() {
        if (!attacked) {
            attacked = true;
            return false;
        }
        else {
            status = RoleStatus.DEAD;
            return true;
        }
    }
}
