package roles;

import model.Role;
import model.RoleInteraction;
import model.RoleStatus;
import model.WerewolfSession;
import net.dv8tion.jda.api.entities.User;

public class Seer extends Role {

    public Seer(String nickName, User user, WerewolfSession session) {
        super("Seer", nickName, false, user, session);
        this.status = RoleStatus.ALIVE;
        user.openPrivateChannel().queue((channel) -> {
            channel.sendMessage("You are a Seer. You will be trying to execute the werewolves each day. Each night, you can inspect a player and reveal whether or not they are a Werewolf").queue();
        });
    }

    @Override
    public boolean prompt() {
        if (status == RoleStatus.ALIVE) {
            user.openPrivateChannel().queue((channel) -> {
                channel.sendMessage( session.getRolesPrompt(false) + "You are a Seer. Select the player that you want to inspect").queue();
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
            if (target.getStatus() == RoleStatus.ALIVE) {
                user.openPrivateChannel().queue((channel) -> {
                    channel.sendMessage(target.getNickName() + " is " + (target.inspect() ? "" : "not ") + "a Werewolf. Please wait for all players to finish").queue();
                });

                session.getModerator().openPrivateChannel().queue((channel) -> {
                    channel.sendMessage(getNickName() + " inspected " + target.getNickName()).queue();
                });
                target.onInteract(user.getId(), RoleInteraction.INSPECT);
                respondedThisRound = true;
                return true;
            }
            else {
                user.openPrivateChannel().queue((channel) -> {
                    channel.sendMessage( "You must select a player that's alive").queue();
                });
                return false;
            }
        }
        catch (Exception e) {
            user.openPrivateChannel().queue((channel) -> {
                channel.sendMessage( "Please enter a number that corresponds to a player").queue();
            });
            return false;
        }
    }
}
