package com.ivanov.gptClient.MyGptClient.messages;

public class ImageContentPart  implements ContentPart {

    private String imageUrl;
    private String type;


    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
