package com.itmation.tools.stp;

/**
 * Created by Christophe on 04/02/2016.
 */

import com.inductiveautomation.ignition.common.sqltags.model.types.DataQuality;



public class SimulEvent {

    public enum EventType {
        VALUE ,
        QUALITY
    }

    public String tagPath;
    public String ts;
    public EventType type;
    public double value;
    private DataQuality quality;

    public DataQuality getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality.fromIntValue(quality);

    }
    public String getEventType(){
        return type.name();
    }
    public void setEventType(String ev) {
        type = EventType.valueOf(ev);
    }
    public void setEventType(int ev) {
        switch(ev) {
            case 0:
                type = EventType.VALUE;
            case 1:
                type = EventType.QUALITY;
            default:
                type = EventType.VALUE;
        }

    }
}
