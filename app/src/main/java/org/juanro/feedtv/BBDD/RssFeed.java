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
import androidx.room.PrimaryKey;

@Entity(tableName = "fuentes")
public class RssFeed {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String url;
    
    // Campos extendidos de rssparser
    private String description;
    private String link;
    private String lastBuildDate;
    private String updatePeriod;
    
    // Campos iTunes
    private String itunesAuthor;
    private String itunesCategories;
    private String itunesDescription;
    private String itunesExplicit;
    private String itunesImage;
    private String itunesKeywords;
    private String itunesNewFeedUrl;
    private String itunesOwnerName;
    private String itunesOwnerEmail;
    private String itunesSubtitle;
    private String itunesSummary;
    private String itunesType;
    private String itunesBlock;
    private String itunesComplete;

    public RssFeed(String title, String url) {
        this.title = title;
        this.url = url;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public String getLastBuildDate() { return lastBuildDate; }
    public void setLastBuildDate(String lastBuildDate) { this.lastBuildDate = lastBuildDate; }
    public String getUpdatePeriod() { return updatePeriod; }
    public void setUpdatePeriod(String updatePeriod) { this.updatePeriod = updatePeriod; }

    public String getItunesAuthor() { return itunesAuthor; }
    public void setItunesAuthor(String itunesAuthor) { this.itunesAuthor = itunesAuthor; }
    public String getItunesCategories() { return itunesCategories; }
    public void setItunesCategories(String itunesCategories) { this.itunesCategories = itunesCategories; }
    public String getItunesDescription() { return itunesDescription; }
    public void setItunesDescription(String itunesDescription) { this.itunesDescription = itunesDescription; }
    public String getItunesExplicit() { return itunesExplicit; }
    public void setItunesExplicit(String itunesExplicit) { this.itunesExplicit = itunesExplicit; }
    public String getItunesImage() { return itunesImage; }
    public void setItunesImage(String itunesImage) { this.itunesImage = itunesImage; }
    public String getItunesKeywords() { return itunesKeywords; }
    public void setItunesKeywords(String itunesKeywords) { this.itunesKeywords = itunesKeywords; }
    public String getItunesNewFeedUrl() { return itunesNewFeedUrl; }
    public void setItunesNewFeedUrl(String itunesNewFeedUrl) { this.itunesNewFeedUrl = itunesNewFeedUrl; }
    public String getItunesOwnerName() { return itunesOwnerName; }
    public void setItunesOwnerName(String itunesOwnerName) { this.itunesOwnerName = itunesOwnerName; }
    public String getItunesOwnerEmail() { return itunesOwnerEmail; }
    public void setItunesOwnerEmail(String itunesOwnerEmail) { this.itunesOwnerEmail = itunesOwnerEmail; }
    public String getItunesSubtitle() { return itunesSubtitle; }
    public void setItunesSubtitle(String itunesSubtitle) { this.itunesSubtitle = itunesSubtitle; }
    public String getItunesSummary() { return itunesSummary; }
    public void setItunesSummary(String itunesSummary) { this.itunesSummary = itunesSummary; }
    public String getItunesType() { return itunesType; }
    public void setItunesType(String itunesType) { this.itunesType = itunesType; }
    public String getItunesBlock() { return itunesBlock; }
    public void setItunesBlock(String itunesBlock) { this.itunesBlock = itunesBlock; }
    public String getItunesComplete() { return itunesComplete; }
    public void setItunesComplete(String itunesComplete) { this.itunesComplete = itunesComplete; }
}
