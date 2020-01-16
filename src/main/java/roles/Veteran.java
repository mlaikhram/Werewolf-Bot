package roles;

import model.Role;
import model.RoleInteraction;
import model.RoleStatus;
import model.WerewolfSession;
import net.dv8tion.jda.api.entities.User;

public class Veteran extends Role {

    public Veteran(String nickName, User user, WerewolfSession session) {
        super("Veteran", nickName, false, user, session);
        this.status = RoleStatus.ALIVE;
        user.openPrivateChannel().queue((channel) -> {
            channel.sendMessage("You are a Veteran. You will be trying to execute the werewolves each day. You will kill any player that interacts with you in the night").queue();
        });
    }

    @Override
    public boolean prompt() {
        if (status == RoleStatus.ALIVE) {
            user.openPrivateChannel().queue((channel) -> {
                channel.sendMessage( session.getRolesPrompt(false) + "You are a Veteran. Select the player that you think is a Werewolf").queue();
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
    public void onInteract(String id, RoleInteraction interaction) {
        session.addVictim(id);
        session.getModerator().openPrivateChannel().queue((channel) -> {
            channel.sendMessage(getNickName() + " attacked " + session.getRoles().get(id).getNickName()).queue();
        });
    }
}
