package com.totalhealthdashboard.data.models;

public class RawApiResponse {
    private String source;
    private String data;

    public RawApiResponse(String source, String data) {
        this.source = source;
        this.data = data;
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
}
