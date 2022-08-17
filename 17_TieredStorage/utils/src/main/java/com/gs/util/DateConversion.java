package com.gs.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class DateConversion {
    public static void main(String[] args) {
        long timeMillis = System.currentTimeMillis();
        for (int i=0; i<5; i++) {
            timeMillis = timeMillis+10000;
            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timeMillis);
            System.out.println("Time: "+timeMillis+" Date :" + calendar.getTime());


            Random rand = new Random();
            int randomNum = rand.nextInt((6 - 0)) ;
            System.out.println(randomNum);
            System.out.println(weekOfDay.values()[randomNum]);

            Date dateTime = Calendar.getInstance().getTime();
            System.out.println(dateTime);
        }
    }
    enum weekOfDay{
        SUNDAY,MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY
    }
}
