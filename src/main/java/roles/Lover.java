package roles;

import model.Role;
import model.RoleStatus;
import model.WerewolfSession;
import net.dv8tion.jda.api.entities.User;

public class Lover extends Role {

    protected User partner;

    public Lover(String nickName, User user, WerewolfSession session, User partner) {
        super("Lover", nickName, false, user, session);
        this.partner = partner;
        this.status = RoleStatus.ALIVE;
        user.openPrivateChannel().queue((channel) -> {
            channel.sendMessage("You are a Lover. You will be trying to execute the werewolves each day. If your partner is killed or executed, you will die at the same time. Your partner is " + partner.getName()).queue();
        });
    }

    @Override
    public String getRoleName(boolean detailed) {
        return getRoleName() + (detailed ? " -> " + partner.getName() : "");
    }

    @Override
    public boolean prompt() {
        if (status == RoleStatus.ALIVE) {
            user.openPrivateChannel().queue((channel) -> {
                channel.sendMessage( session.getRolesPrompt(false) + "You are a Lover. Your partner is " + partner.getName() + ". Select the player that you think is a Werewolf").queue();
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

    public User getPartner() {
        return partner;
    }
}
