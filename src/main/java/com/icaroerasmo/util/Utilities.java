package com.icaroerasmo.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class Utilities {
    public String getFullTimeAmount(long millis) {

        long days = 0, hours = 0, minutes = 0, seconds = 0, milliseconds = 0;

        while(millis > 0) {
            if(millis > 86400000) {
                days++;
                millis -= 86400000;
            } else if(millis > 3600000) {
                hours++;
                millis -= 3600000;
            } else if(millis > 60000) {
                minutes++;
                millis -= 60000;
            } else if(millis > 1000) {
                seconds++;
                seconds -= 1000;
            } else {
                milliseconds = millis;
                millis = 0;
            }
        }

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
