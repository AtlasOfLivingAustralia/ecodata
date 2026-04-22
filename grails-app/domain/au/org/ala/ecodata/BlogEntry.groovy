package au.org.ala.ecodata

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class BlogEntry {
    String blogEntryId
    String title
    boolean keepOnTop
    String imageId
    String type
    String content
    String date
    String imageThumbnailUrl
    String stockIcon
    String viewMoreUrl
}
