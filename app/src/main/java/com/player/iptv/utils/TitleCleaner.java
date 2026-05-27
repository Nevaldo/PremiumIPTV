package com.player.iptv.utils;

public class TitleCleaner {

    public static String clean(String title) {
        if (title == null) return "";

        String t = title;

        t = t.replaceAll("\\b(4K|HD|HDRip|WEB[-\\s]?DL|WEBRip|BluRay|BRRip|DVDRip|TS|CAM|HDTC|HQCAM|2160p|1080p|720p|480p|360p|XVID|x264|x265|HEVC|H\\.264|H\\.265|AVC|AAC|MP3|AC3|DTS|TRUEHD|DD5\\.1|DDP5\\.1|ATMOS|MULTI|DUAL|AUDIO|LEGENDADO|FULL|UHD|HDR|SDR|IMAX|REMUX|COMPLETE|PROPER|REPACK|INTERNAL|LIMITED|EXTENDED|DIRECTORS?\\s*CUT|UNCUT|UNRATED|THEATRICAL)\\b", "");
        t = t.replaceAll("\\bS\\d{1,2}(\\s*E\\d{1,3})?\\b", "");
        t = t.replaceAll("\\bTemporada\\s*\\d+\\b", "");
        t = t.replaceAll("\\b(Season|Epis[oó]dio|Episode)\\s*\\d+\\b", "");
        t = t.replaceAll("\\b\\d{4}\\b", "");
        t = t.replaceAll("[\\[\\]\\(\\)\\{\\}]", "");
        t = t.replaceAll("\\s+", " ").trim();

        return t;
    }
}
