package com.aventstack.extentreports.model;

public class ScreenCapture extends Media { 

    private static final long serialVersionUID = -3413285738443448335L;

    public String getSource() {
        return "<img data-featherlight='" + getUrlPath() + "' width='10%' src='"+getUrlPath()+"' data-src='" + getUrlPath() + "'>";
    }
    
    public String getSourceWithIcon() {
        return "<a href='#' data-featherlight='" + getPath() + "'><i class='material-icons'>photo</i></a>";
    }

}
