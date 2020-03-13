package com.krisdb.wearcastslibrary;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.krisdb.wearcastslibrary.DateUtils.FormatDate;

public class FeedParser {

    public static List<PodcastItem> parse(final PodcastItem podcast) {
        return parse(podcast,  Integer.MAX_VALUE);
    }

        public static List<PodcastItem> parse(final PodcastItem podcast, final int count) {
        final List<PodcastItem> episodes = new ArrayList<>();

        try {
            final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            final XmlPullParser parser = factory.newPullParser();

            final InputStream stream = CommonUtils.getRemoteStream(podcast.getChannel().getRSSUrl().toString());
            parser.setInput(stream, "UTF-8");

            boolean done = false;

            PodcastItem episode = null;
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT && !done) {
                String name;
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        name = parser.getName();
                        if (name.equalsIgnoreCase("item")) {
                            episode = new PodcastItem();
                            episode.setChannel(podcast.getChannel());
                        } else if (episode != null) {
                            if (name.equalsIgnoreCase("link")) {
                                episode.setEpisodeUrl(parser.nextText());
                            } else if (name.equalsIgnoreCase("description")) {
                                episode.setDescription(CommonUtils.CleanDescription(parser.nextText().trim()));
                            } else if (name.equalsIgnoreCase("title")) {
                                episode.setTitle(parser.nextText().trim());
                            } else if (name.equalsIgnoreCase("pubDate")) {
                                episode.setPubDate(FormatDate(parser.nextText().trim()));
                            } else if (name.equalsIgnoreCase("enclosure")) {
                                episode.setMediaUrl(parser.getAttributeValue(null, "url"));
                                //episode.setDuration(CommonUtils.GetDuration(episode.getMediaUrl().toString()));
                            } else if (name.equalsIgnoreCase("itunes:duration")) {
                                final String text = parser.nextText();
                                episode.setDuration(text != null && text.length() > 0 ? DateUtils.getMilliseconds(text) : 0);
                                //episode.setDuration(CommonUtils.GetDuration(episode.getMediaUrl().toString()));
                            }
                            else if (name.equalsIgnoreCase("media:thumbnail")) {
                                episode.setThumbnailUrl(parser.getAttributeValue(null, "url"));
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        name = parser.getName();
                        if (name.equalsIgnoreCase("item") && episode != null) {
                            episode.setPodcastId(podcast.getPodcastId());
                            episodes.add(episode);
                        } else if (name.equalsIgnoreCase("channel")) {
                            done = true;
                        }
                        break;
                }
                eventType = parser.next();
                if (episodes.size() == count) break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

            try {
                Collections.sort(episodes, (item1, item2) -> DateUtils.ConvertDate(item2.getPubDate()).compareTo(DateUtils.ConvertDate(item1.getPubDate())));
            } catch (Exception ignored) {}

        return episodes;
    }
}