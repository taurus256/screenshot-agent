package ru.taustudio.duckview.manager.driver;

public enum Device {
    IPHONE_SE("iPhone SE (3rd generation)"),
    IPHONE_PRO("iPhone 14 Pro Max"),
    IPAD("iPad Air (5th generation)");

    Device(String systemName){
        this.systemName = systemName;
    }

    // Системное имя - должно соответствовать поддерживаемому эмулятором
    String systemName;
    public String getSystemName(){
        return systemName;
    }
}
