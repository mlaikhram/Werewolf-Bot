package listener;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class WerewolfListener extends ListenerAdapter {



    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        if (event.isFromType(ChannelType.TEXT)) {
            System.out.println("message received from " + event.getAuthor() + "!");
            System.out.println(event.getMessage().getContentDisplay());

            if (event.getMessage().getContentRaw().equals("!ping")) {
                event.getChannel().sendMessage("Pong!").queue();
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
