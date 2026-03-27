package com.ivanov.gptClient.MyGptClient;

public class ImageContentPart  implements ContentPart {

    private String image_url;
    private String type = "input_image";


    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
