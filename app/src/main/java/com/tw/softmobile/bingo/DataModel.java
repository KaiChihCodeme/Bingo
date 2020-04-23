package com.tw.softmobile.bingo;

public class DataModel {
    private static int s_iMax;
    private static int s_iMin;
    private static boolean s_bIsObey = false;
    private static boolean s_bMode = false;
    private static int[] s_arCompleteArr;
    private static int s_iWinCon;
    private static int s_iPickColor = R.color.picked_blue; //預設為blue

    public DataModel() {
    }

    public void setRange(int max, int min) {
        this.s_iMax = max;
        this.s_iMin = min;
    }

    public void setIsObey(boolean bIsObey) {
        this.s_bIsObey = bIsObey;
    }

    public void setMode(boolean mode) {
        this.s_bMode = mode;
    }

    public void setCompleteArray(int[] arr) {
        this.s_arCompleteArr = arr;
    }

    public void setPickColor(int color) {
        this.s_iPickColor = color;
    }

    public void setWinCon(int w) {
        this.s_iWinCon = w;
    }

    public int getWinCon() {
        return s_iWinCon;
    }

    public int getPickColor() {
        return s_iPickColor;
    }

    public int[] getCompleteArray() {
        return s_arCompleteArr;
    }

    public boolean getMode() {
        return s_bMode;
    }

    public boolean getIsObey() {
        return  s_bIsObey;
    }


    public int getRangeMax() {
        return s_iMax;
    }

    public int getRangeMin() {
        return s_iMin;
    }
}
