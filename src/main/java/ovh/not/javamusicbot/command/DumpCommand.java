package ovh.not.javamusicbot.command;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.bramhaag.owo.OwO;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ovh.not.javamusicbot.*;

import javax.annotation.Nonnull;
import java.io.IOException;

import static ovh.not.javamusicbot.MusicBot.JSON_MEDIA_TYPE;
import static ovh.not.javamusicbot.Utils.HASTEBIN_URL;
import static ovh.not.javamusicbot.Utils.encode;

public class DumpCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(DumpCommand.class);

    private final OwO owo;

    public DumpCommand() {
        super("dump");
        owo = new OwO.Builder()
                .setKey(MusicBot.getConfigs().config.owoKey)
                .setUploadUrl("https://paste.dabbot.org")
                .setShortenUrl("https://paste.dabbot.org")
                .build();
    }

    @Override
    public void on(CommandContext context) {
        MusicManager musicManager = GuildManager.getInstance().getMusicManager(context.getEvent().getGuild());
        if (!musicManager.isPlayingMusic()) {
            context.reply("No music is playing on this guild! To play a song use `{{prefix}}play`");
            return;
        }

        String[] items = new String[musicManager.getTrackScheduler().getQueue().size() + 1];

        AudioTrack current = musicManager.getPlayer().getPlayingTrack();
        try {
            items[0] = Utils.encode(current);
        } catch (IOException e) {
            logger.error("error occurred encoding an AudioTrack", e);
            context.reply("An error occurred!");
            return;
        }

        int i = 1;
        for (AudioTrack track : musicManager.getTrackScheduler().getQueue()) {
            try {
                items[i] = encode(track);
            } catch (IOException e) {
                logger.error("error occured encoding audio tracks", e);
                context.reply("An error occurred!");
                return;
            }
            i++;
        }

        String json = new JSONArray(items).toString();

        owo.upload(json, "text/plain").execute(file -> {
            context.reply("Dump created! " + file.getFullUrl());
        }, throwable -> {
            logger.error("error uploading to owo", throwable);

            RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, json);

            Request request = new Request.Builder()
                    .url(HASTEBIN_URL)
                    .method("POST", body)
                    .build();

            MusicBot.HTTP_CLIENT.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@Nonnull Call call, @Nonnull IOException e) {
                    logger.error("error occurred posting to hastebin.com", e);
                    context.reply("An error occurred!");
                }

                @Override
                public void onResponse(@Nonnull Call call, @Nonnull Response response) throws IOException {
                    context.reply("Dump created! https://hastebin.com/%s.json",
                            new JSONObject(response.body().string()).getString("key"));
                    response.close();
                }
            });
        });
    }
}
