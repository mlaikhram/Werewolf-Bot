package roles;

import model.Role;
import model.RoleStatus;
import model.WerewolfSession;
import net.dv8tion.jda.api.entities.User;

public class Undetermined extends Role {


    public Undetermined(String nickName, User user, WerewolfSession session) {
        super("Undetermined", nickName, false, user, session);
        this.status = RoleStatus.NOT_READY;
    }

    @Override
    public void prompt() {
        user.openPrivateChannel().queue((channel) -> {
            if (status == RoleStatus.NOT_READY) {
                channel.sendMessage(session.getModerator().getName() + " invited you to play Werewolf! Would you like to join? (Respond with 'yes' or 'no')").queue();
            }
        });
    }

    @Override
    public boolean applyResponse(String response) {
        if (response.equalsIgnoreCase("yes")) {
            status = RoleStatus.READY;

            user.openPrivateChannel().queue((channel) -> {
                channel.sendMessage("Great! The game will begin once the moderator is ready").queue();
            });

            session.getModerator().openPrivateChannel().queue((channel) -> {
                long pendingResponses = session.getRoles().values().stream().filter((role) -> (role.getStatus() == RoleStatus.NOT_READY)).count();
                long totalPlayers = session.getRoles().size();
                channel.sendMessage(user.getName() + " has accepted you invitation! Waiting on " + pendingResponses + "/" + totalPlayers + " more responses").queue();
            });

            return true;
        }
        else if (response.equalsIgnoreCase("no")) {
            session.removePlayer(user);

            user.openPrivateChannel().queue((channel) -> {
                channel.sendMessage("Ok, you have been removed from the session").queue();
            });

            session.getModerator().openPrivateChannel().queue((channel) -> {
                long pendingResponses = session.getRoles().values().stream().filter((role) -> (role.getStatus() == RoleStatus.NOT_READY)).count();
                long totalPlayers = session.getRoles().size();
                channel.sendMessage(user.getName() + " has declined your invitation. Waiting on " + pendingResponses + "/" + totalPlayers + " more responses").queue();
            });

            return true;
        }
        else {
            user.openPrivateChannel().queue((channel) -> {
                channel.sendMessage("Please respond 'yes' or 'no'").queue();
            });

            return false;
        }
    }
}
