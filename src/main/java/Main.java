import listener.WerewolfListener;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.User;

import javax.security.auth.login.LoginException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Main{

    public static void main(String[] args) throws LoginException, IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream("bot.properties"));

        JDABuilder builder = new JDABuilder(AccountType.BOT);
        WerewolfListener werewolfListener = new WerewolfListener();
        builder.setToken(prop.getProperty("token"));
        System.out.println("Building bot");
        builder.addEventListeners(werewolfListener);
        JDA jda = builder.build();
        werewolfListener.setJDA(jda);
    }
}
