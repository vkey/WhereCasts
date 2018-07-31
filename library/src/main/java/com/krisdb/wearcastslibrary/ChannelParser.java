package com.krisdb.wearcastslibrary;

import android.sax.Element;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;

import org.xml.sax.Attributes;

import java.io.InputStream;

public class ChannelParser {

    public static ChannelItem
    parse(final String url) {
        return parse(null, null, url);
    }

    public static ChannelItem parse(final String title, final String description, String url) {
        final ChannelItem channel = new ChannelItem();
        final RootElement root = new RootElement("rss");
        final Element channelNode = root.getChild("channel");

        channelNode.getChild("title").setEndTextElementListener(new EndTextElementListener()
        {
            public void end(final String body) {
                channel.setTitle(title == null || title.length() == 0 ? body : title);
            }
        });

        channelNode.getChild("link").setEndTextElementListener(new EndTextElementListener()
        {
            public void end(final String body) {
                channel.setSiteUrl(body);
            }
        });

        channelNode.getChild("description").setEndTextElementListener(new EndTextElementListener()
        {
            public void end(final String body) {
                channel.setDescription(description == null ? body : description);
            }
        });

        if (channel.getThumbnailUrl() == null) {
            channelNode.getChild("image").getChild("url").setEndTextElementListener(new EndTextElementListener()
            {
                public void end(final String body) {
                    channel.setThumbnailUrl(body);
                }
            });
        }

        channelNode.getChild("http://search.yahoo.com/mrss/", "thumbnail").setStartElementListener(new StartElementListener()
        {
            public void start(Attributes attr)
            {
                if (attr.getValue("url") != null)
                    channel.setThumbnailUrl(attr.getValue("url"));
            }
        });

        if (channel.getThumbnailUrl() == null) {
            channelNode.getChild("http://www.itunes.com/dtds/podcast-1.0.dtd", "image").setStartElementListener(new StartElementListener() {
                public void start(Attributes attr) {
                    if (attr.getValue("href") != null)
                        channel.setThumbnailUrl(attr.getValue("href"));
                }
            });
        }

        try
        {
            final InputStream stream = CommonUtils.getRemoteStream(url);

            if (stream != null) {
                Xml.parse(stream, Xml.Encoding.UTF_8, root.getContentHandler());

                stream.close();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (channel.getThumbnailUrl() != null)
            channel.setThumbnailName(CommonUtils.GetThumbnailName(channel));

        if (channel.getTitle() == null)
            channel.setTitle(title == null ? "N/A" : title);

        if (channel.getSiteUrl() == null)
            channel.setSiteUrl(url);

        if (channel.getRSSUrl() == null)
            channel.setRSSUrl(url);

        if(channel.getDescription() == null)
            channel.setDescription(description != null ? description : "");

        channel.setRSSUrl(url);

        return channel;
    }
}