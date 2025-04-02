package com.icaroerasmo.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class Utilities {
    public String getFullTimeAmount(long millis) {

        final int oneDay = 86400000, oneHour = 3600000, oneMinute = 60000, oneSecond = 1000;

        long days = millis / oneDay;
        millis %= oneDay;

        long hours = millis / oneHour;
        millis %= oneHour;

        long minutes = millis / oneMinute;
        millis %= oneMinute;

        long seconds = millis / oneSecond;
        millis %= oneSecond;

        long milliseconds = millis;

        String strTime = "";

        if(days > 1) {
            strTime += days + " days, ";
        } else if(days == 1) {
            strTime += days + " day, ";
        }

        if(hours > 1) {
            strTime += hours + " hours, ";
        } else if(hours == 1) {
            strTime += hours + " hour, ";
        }

        if (minutes > 1) {
            strTime += minutes + " minutes, ";
        } else if(minutes == 1) {
            strTime += minutes + " minute, ";
        }

        if (seconds > 1) {
            strTime += seconds + " seconds, ";
        } else if(seconds == 1) {
            strTime += seconds + " second, ";
        }

        if(milliseconds > 1) {
            strTime += milliseconds + " milliseconds";
        } else if(milliseconds == 1) {
            strTime += milliseconds + " millisecond";
        }

        if(strTime.endsWith(", ")) {
            strTime = strTime.substring(0, strTime.length() - 2);
        }

        return strTime;
    }
}
