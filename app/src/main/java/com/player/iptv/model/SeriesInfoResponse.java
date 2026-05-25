package com.player.iptv.model;

import java.util.List;
import java.util.Map;

public class SeriesInfoResponse {

    private Map<String, List<SeriesEpisode>> episodes;

    public Map<String, List<SeriesEpisode>> getEpisodes() {
        return episodes;
    }
}
