package model;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import roles.Undetermined;

import java.util.*;

public class WerewolfSession {

    public static final int MIN_PLAYER_COUNT = 2;

    private final MessageChannel channel;
    private final User moderator;
    private final Map<String, Role> roles;

    private SessionStatus status;

    public WerewolfSession(MessageChannel channel, User moderator, Collection<User> players) {
        this.channel = channel;
        this.moderator = moderator;
        this.roles = new TreeMap<>();
        for (User player : players) {
            addPlayer(player);
        }
        this.status = SessionStatus.WAITING_FOR_PLAYERS;
    }

    public void addPlayer(User player) { // TODO: try catch if player is already added
        roles.put(player.getId(), new Undetermined(player.getName(), player, this));
    }

    public void removePlayer(User player) {
        roles.remove(player.getId());
    }

    public User getModerator() {
        return moderator;
    }

    public Map<String, Role> getRoles() {
        return roles;
    }

    public Collection<String> promptPlayers() {
        List<String> ids = new ArrayList<>();
        for (Map.Entry<String, Role> player : roles.entrySet()) {
            if (player.getValue().prompt()) {
                ids.add(player.getValue().getUser().getId());
            }
        }
        return ids;
    }

    // TODO: checks + game loop
    public boolean sendResponse(User user, String response) {
        if (roles.containsKey(user.getId())) {
            return roles.get(user.getId()).applyResponse(response);
        }
        else {
            System.out.println(user + " is not in this session");
            return true; // removes invalid open channel
        }
    }

    public SessionStatus checkStatus() {
        if (status == SessionStatus.WAITING_FOR_PLAYERS) {
            if (roles.size() < MIN_PLAYER_COUNT) {
                moderator.openPrivateChannel().queue((channel) -> {
                    channel.sendMessage("Cancelling session. You need at least " + MIN_PLAYER_COUNT + " players to play").queue();
                });
                for (Role role : roles.values()) {
                    role.getUser().openPrivateChannel().queue((channel) -> {
                        channel.sendMessage("Cancelling session. Not enough players wanted to play").queue();
                    });
                }
                status = SessionStatus.EXPIRED;
            }
            else if (roles.values().stream().allMatch((role) -> (role.getStatus() == RoleStatus.READY))) {
                moderator.openPrivateChannel().queue((channel) -> {
                    channel.sendMessage("All players are ready! The game will begin shortly").queue();
                });
                for (Role role : roles.values()) {
                    role.getUser().openPrivateChannel().queue((channel) -> {
                        channel.sendMessage("All players are ready! The game will begin shortly").queue();
                    });
                }
                status = SessionStatus.STARTED;
            }
            else {
                status = SessionStatus.WAITING_FOR_PLAYERS;
            }
        }
        return status;
    }
}
