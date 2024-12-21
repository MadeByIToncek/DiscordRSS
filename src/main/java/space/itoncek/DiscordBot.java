package space.itoncek;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.dv8tion.jda.api.requests.GatewayIntent.*;

public class DiscordBot {
	private final Logger L = LoggerFactory.getLogger(this.getClass());
	private final String token;
	private final ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
	private @Nullable JDA jda;

	public DiscordBot(String token) throws InterruptedException {
		this.token = token;
	}

	public void startIfNotRunning() {
		if(jda == null) {
			jda = JDABuilder.create(token,
							MESSAGE_CONTENT,
							GUILD_MEMBERS,
							GUILD_MESSAGE_POLLS,
							GUILD_MESSAGE_REACTIONS,
							GUILD_MESSAGE_TYPING,
							GUILD_MESSAGES)
					.build();

			try {
				jda.awaitReady();
			} catch(InterruptedException e) {
				L.error("Bot startup issue!", e);
			}

			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					try {
						jda.awaitShutdown();
						jda = null;
					} catch(InterruptedException e) {
						L.error("Bot shutdown issue!", e);
					}
				}
			}, 1, TimeUnit.MINUTES);
		}
	}

	public List<Message> listMessages(TextChannel channel) {
		if(jda == null) return null;
		return channel.getHistory().retrievePast(50).complete();
	}

	public TextChannel getChannel(long snowflake) {
		if(jda == null) return null;
		return jda.getChannelById(TextChannel.class, snowflake);
	}

	public void stop() throws InterruptedException {
		if(jda == null) {
			return;
		}

		if(!jda.awaitShutdown(Duration.ofSeconds(10))) {
			jda.shutdownNow();
			jda.awaitShutdown();
		}
		timer.shutdown();
	}
}
