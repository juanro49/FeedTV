/*
 *   Copyright 2026 Juanro49
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.juanro.feedtv.BBDD;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.List;

import static androidx.room.ForeignKey.CASCADE;

@SuppressWarnings("unused")
@Entity(tableName = "articulos",
        foreignKeys = @ForeignKey(entity = RssFeed.class,
                parentColumns = "id",
                childColumns = "feedId",
                onDelete = CASCADE),
        indices = {@Index("feedId")})
public class Article {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int feedId;
    private String guid;
    private String title;
    private String author;
    private String link;
    private String pubDate;
    private String description;
    private String content;
    private String image;
    private String audio;
    private String video;
    private String sourceName;
    private String sourceUrl;
    private String commentsUrl;
    private List<String> categories;
    private long numFecha;

    // Campos iTunes
    private String itunesAuthor;
    private String itunesDuration;
    private String itunesEpisode;
    private String itunesEpisodeType;
    private String itunesExplicit;
    private String itunesImage;
    private String itunesKeywords;
    private String itunesSubtitle;
    private String itunesSummary;
    private String itunesBlock;
    private String itunesSeason;

    /**
     * Constructor por defecto para Room
     */
    public Article() {}

    /**
     * Constructor completo para uso manual
     */
    @Ignore
    public Article(int feedId, String guid, String title, String author, String link, String pubDate,
                   String description, String content, String image, String audio, String video,
                   String sourceName, String sourceUrl, String commentsUrl, List<String> categories, long numFecha) {
        this.feedId = feedId;
        this.guid = guid;
        this.title = title;
        this.author = author;
        this.link = link;
        this.pubDate = pubDate;
        this.description = description;
        this.content = content;
        this.image = image;
        this.audio = audio;
        this.video = video;
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
        this.commentsUrl = commentsUrl;
        this.categories = categories;
        this.numFecha = numFecha;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getFeedId() { return feedId; }
    public void setFeedId(int feedId) { this.feedId = feedId; }
    public String getGuid() { return guid; }
    public void setGuid(String guid) { this.guid = guid; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public String getPubDate() { return pubDate; }
    public void setPubDate(String pubDate) { this.pubDate = pubDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getAudio() { return audio; }
    public void setAudio(String audio) { this.audio = audio; }
    public String getVideo() { return video; }
    public void setVideo(String video) { this.video = video; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getCommentsUrl() { return commentsUrl; }
    public void setCommentsUrl(String commentsUrl) { this.commentsUrl = commentsUrl; }
    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }
    public long getNumFecha() { return numFecha; }
    public void setNumFecha(long numFecha) { this.numFecha = numFecha; }

    public String getItunesAuthor() { return itunesAuthor; }
    public void setItunesAuthor(String itunesAuthor) { this.itunesAuthor = itunesAuthor; }
    public String getItunesDuration() { return itunesDuration; }
    public void setItunesDuration(String itunesDuration) { this.itunesDuration = itunesDuration; }
    public String getItunesEpisode() { return itunesEpisode; }
    public void setItunesEpisode(String itunesEpisode) { this.itunesEpisode = itunesEpisode; }
    public String getItunesEpisodeType() { return itunesEpisodeType; }
    public void setItunesEpisodeType(String itunesEpisodeType) { this.itunesEpisodeType = itunesEpisodeType; }
    public String getItunesExplicit() { return itunesExplicit; }
    public void setItunesExplicit(String itunesExplicit) { this.itunesExplicit = itunesExplicit; }
    public String getItunesImage() { return itunesImage; }
    public void setItunesImage(String itunesImage) { this.itunesImage = itunesImage; }
    public String getItunesKeywords() { return itunesKeywords; }
    public void setItunesKeywords(String itunesKeywords) { this.itunesKeywords = itunesKeywords; }
    public String getItunesSubtitle() { return itunesSubtitle; }
    public void setItunesSubtitle(String itunesSubtitle) { this.itunesSubtitle = itunesSubtitle; }
    public String getItunesSummary() { return itunesSummary; }
    public void setItunesSummary(String itunesSummary) { this.itunesSummary = itunesSummary; }
    public String getItunesBlock() { return itunesBlock; }
    public void setItunesBlock(String itunesBlock) { this.itunesBlock = itunesBlock; }
    public String getItunesSeason() { return itunesSeason; }
    public void setItunesSeason(String itunesSeason) { this.itunesSeason = itunesSeason; }
}
