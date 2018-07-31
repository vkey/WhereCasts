package com.krisdb.wearcasts;

import android.content.Context;

import com.krisdb.wearcastslibrary.ChannelParser;
import com.krisdb.wearcastslibrary.PodcastItem;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class OPMLParser {

    public static List<PodcastItem> parse(final Context context, final InputStream stream) {
        final List<PodcastItem> podcasts = new ArrayList<>();

        try {
            final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            final XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(stream, "UTF-8");

            PodcastItem podcast;
            int eventType = xpp.getEventType();
            boolean isInOpml = false;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (xpp.getName().equalsIgnoreCase("opml")) {
                            isInOpml = true;
                        } else if (isInOpml && xpp.getName().equalsIgnoreCase("outline")) {

                            int eventType2 = xpp.next();

                            while ( eventType2 != XmlPullParser.END_DOCUMENT)  {
                                if (eventType2 == XmlPullParser.START_TAG) {
                                    if (xpp.getAttributeValue(null, "xmlUrl") != null) {

                                        String title = null;

                                        if (xpp.getAttributeValue(null, "text") != null)
                                            title = xpp.getAttributeValue(null, "text");
                                        else if (xpp.getAttributeValue(null, "title") != null)
                                            title = xpp.getAttributeValue(null, "title");

                                        podcast = new PodcastItem();
                                        podcast.setChannel(ChannelParser.parse(title, null, xpp.getAttributeValue(null, "xmlUrl")));
                                        podcasts.add(podcast);
                                    }
                                }
                                eventType2 = xpp.next();
                            }
                        }
                        break;
                }
                eventType = xpp.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return podcasts;
    }
}