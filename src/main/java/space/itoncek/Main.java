package space.itoncek;


import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;
import com.rometools.rome.io.impl.FeedGenerators;
import io.javalin.Javalin;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Main {
	private static final Logger L = LoggerFactory.getLogger(Main.class);
	private static DiscordBot db;

	public static void main(String[] args) {
		try {
			db = new DiscordBot(System.getenv("TOKEN"));
		} catch(InterruptedException e) {
			L.error("Discord bot init failed!", e);
		}

		Javalin app = Javalin.create()
				.get("/general", ctx -> ctx.async(() -> ctx.result(getRSS(1319660215318220853L))))
				.get("/vyjezdy", ctx -> ctx.async(() -> ctx.result(getRSS(1319674630138630144L))))
				.get("/technika", ctx -> ctx.async(() -> ctx.result(getRSS(1319984590156206080L))))
				.start(8080);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			app.stop();
			try {
				db.stop();
			} catch(InterruptedException e) {
				L.error("db stop error", e);
			}
		}));
	}

	private static String getRSS(long snowflake) throws FeedException {
		db.startIfNotRunning();
		TextChannel c = db.getChannel(snowflake);
		List<Message> messages = db.listMessages(c);

		SyndFeed feed = new SyndFeedImpl();
		feed.setFeedType("rss_2.0");
		feed.setTitle(c.getName());
		feed.setPublishedDate(Date.from(Instant.now()));
		feed.setLink("https://discord.com/channels/1319660215318220850/" + snowflake);
		feed.setDescription(c.getTopic() == null? " " : c.getTopic());

		L.info("messages.size() = {}", messages.size());

		List<SyndEntry> posts = new ArrayList<>(messages.stream()
				.map(x -> {
					SyndEntry entry = new SyndEntryImpl();
					entry.setTitle(x.getAuthor().getEffectiveName());
					entry.setLink("https://discord.com/channels/1319660215318220850/" + snowflake + "/" + x.getIdLong());
					if(x.getTimeEdited() != null) entry.setUpdatedDate(Date.from(x.getTimeEdited().toInstant()));
					entry.setPublishedDate(Date.from(x.getTimeCreated().toInstant()));
					SyndContent description = new SyndContentImpl();
					description.setType("text/html");

					Parser parser = Parser.builder().build();
					Node document = parser.parse(x.getContentDisplay());
					HtmlRenderer renderer = HtmlRenderer.builder().build();
					description.setValue(renderer.render(document));

					entry.setDescription(description);

					return entry;
				})
				.toList());
		Collections.reverse(posts);
		feed.setEntries(posts);

		SyndFeedOutput out = new SyndFeedOutput();
		return out.outputString(feed, true);
	}
}