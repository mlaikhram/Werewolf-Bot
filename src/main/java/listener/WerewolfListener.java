package listener;

import model.WerewolfSession;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import util.MessageUtils;

import java.util.HashMap;
import java.util.Map;

public class WerewolfListener extends ListenerAdapter {

    private JDA jda;
    private Map<String, WerewolfSession> sessions;

    public WerewolfListener() {
        this.sessions = new HashMap<>();
    }

    public void setJDA(JDA jda) {
        this.jda = jda;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        if (event.isFromType(ChannelType.TEXT)) {
            System.out.println("message received from " + event.getAuthor() + "!");
            System.out.println("on server " + event.getChannel().getName());
            System.out.println("on server " + event.getChannel().getId());
            String rawMessage = event.getMessage().getContentRaw();
            System.out.println(rawMessage);
            System.out.println("my id is " + jda.getSelfUser().getId());

            String[] messageTokens = rawMessage.split(" ");
            if (messageTokens[0].equals(MessageUtils.userIDToMention(jda.getSelfUser().getId()))) {
                if (messageTokens.length >= 2) {
                    switch (messageTokens[1]) {
                        case "play":
                            initializeGame(event);
                            event.getChannel().sendMessage("Do you want to play a game?").queue();
                            break;

                        default:
                            event.getChannel().sendMessage("Did you mention me?").queue();
                            break;
                    }
                }
            }
        }
        else if (event.isFromType(ChannelType.PRIVATE)) {
            System.out.println("private message received from " + event.getAuthor() + "!");
            System.out.println(event.getMessage().getContentDisplay());

            if (event.getMessage().getContentRaw().equals("!ping")) {
                event.getChannel().sendMessage("Private Pong!").queue();
            }
        }
    }

    public void initializeGame(MessageReceivedEvent event) {

    }
}
