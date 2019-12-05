package model;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import roles.Undetermined;

import java.util.*;

public class WerewolfSession {

    private final MessageChannel channel;
    private final User moderator;
    private final Map<User, Role> roles;

    public WerewolfSession(MessageChannel channel, User moderator, Collection<User> players) {
        this.channel = channel;
        this.moderator = moderator;
        this.roles = new TreeMap<>();
        for (User player : players) {
            addPlayer(player);
        }
    }

    public void addPlayer(User player) { // TODO: try catch if player is already added
        roles.put(player, new Undetermined(player.getName(), player, this));
    }

    public void removePlayer(User player) {
        roles.remove(player);
    }

    public User getModerator() {
        return moderator;
    }

    public Map<User, Role> getRoles() {
        return roles;
    }

    // TODO: return user IDs to listen to
    public void promptPlayers() {
        for (Role player : roles.values()) {
            player.prompt();
        }
    }

    // TODO: checks + game loop
    public boolean sendResponse(User user, String response) {
        if (roles.containsKey(user)) {
            return roles.get(user).applyResponse(response);
        }
        else {
            System.out.println(user + " is not in this session");
            return true;
        }
    }
}
