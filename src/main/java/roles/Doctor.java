package roles;

import model.Role;
import model.RoleInteraction;
import model.RoleStatus;
import model.WerewolfSession;
import net.dv8tion.jda.api.entities.User;

public class Doctor extends Role {

    public Doctor(String nickName, User user, WerewolfSession session) {
        super("Doctor", nickName, false, user, session);
        this.status = RoleStatus.ALIVE;
        user.openPrivateChannel().queue((channel) -> {
            channel.sendMessage("You are a Doctor. You will be trying to execute the werewolves each day. Each night, you can protect a player from the Werewolves").queue();
        });
    }

    @Override
    public boolean prompt() {
        if (status == RoleStatus.ALIVE) {
            user.openPrivateChannel().queue((channel) -> {
                channel.sendMessage( session.getRolesPrompt(false) + "You are a Doctor. Select the player that you want to protect").queue();
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
                session.protectPlayer(target.getUser().getId());
                user.openPrivateChannel().queue((channel) -> {
                    channel.sendMessage(target.getNickName() + " is protected for the night. Please wait for all players to finish").queue();
                });

                session.getModerator().openPrivateChannel().queue((channel) -> {
                    channel.sendMessage(getNickName() + " protected " + target.getNickName()).queue();
                });
                target.onInteract(user.getId(), RoleInteraction.PROTECT);
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
