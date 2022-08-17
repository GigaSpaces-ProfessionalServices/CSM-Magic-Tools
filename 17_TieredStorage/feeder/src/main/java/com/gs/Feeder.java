package com.gs;

import com.j_spaces.core.IJSpace;
import com.j_spaces.core.client.SQLQuery;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;
import org.openspaces.core.space.cache.LocalViewSpaceConfigurer;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.*;

public class Feeder {
    GigaSpace gs;

    public Feeder(GigaSpace gs) {
        this.gs = gs;
    }

    public void feed(int amount){
        Data1[] objects = new Data1[amount];
        Date dateTime = Calendar.getInstance().getTime();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateTime);
        int total_entires = 0;
        List<Data1> data1List = new ArrayList<Data1>();
        List<Data3> data3List = new ArrayList<Data3>();
        List<Data4> data4List = new ArrayList<Data4>();
        List<Purchase> purchasesList = new ArrayList<Purchase>();
        List<Order> orderList = new ArrayList<Order>();
        for (int k=0; k<amount; k++){
            //objects[k]= new Data1(k, "" + (4 + (k%2)));
            data1List.add(new Data1(k, "" + (4 + (k%2))));
            //gs.write(new Data2(k, "" + (4 + (k%2))));
            data3List.add(new Data3(k, "" + (4 + (k%2))));
            //gs.write(new Data3(k, "" + (4 + (k%2))));
            data4List.add(new Data4(k, "" + (4 + (k%2))));
            //gs.write(new Data4(k, "" + (4 + (k%2))));

            calendar.add(Calendar.DATE, -1);
            Random rand = new Random();
            int randomNum = rand.nextInt(6 - 0) ;
            purchasesList.add(new Purchase(k,calendar.getTime(), WeekOfDay.values()[randomNum]+""));
            //gs.write(new Purchase(k,calendar.getTime(), WeekOfDay.values()[randomNum]+""));
            orderList.add(new Order(k,OrderCategory.values()[randomNum]+""));
            //gs.write(new Order(k,OrderCategory.values()[randomNum]+""));

        }
        //Read
        gs.writeMultiple(data1List.toArray());
        gs.writeMultiple(data3List.toArray());
        gs.writeMultiple(data4List.toArray());
        gs.writeMultiple(purchasesList.toArray());
        gs.writeMultiple(orderList.toArray());
        //gs.writeMultiple(objects);

    }

    public void read(int amount){
        long start = System.currentTimeMillis();
        Data2[] res2 = gs.readMultiple(new Data2(),amount);
        System.out.println("read of Data2 took:" + (System.currentTimeMillis() - start) + " res:" + res2.length);

        long start2 = System.currentTimeMillis();
        Data1[] res4 = gs.readMultiple(new Data1(),amount);
        System.out.println("read of Data1 took:" + (System.currentTimeMillis() - start2) + " res:" + res4.length);

    }

    public static void main(String[] args) throws InterruptedException {
        //GigaSpace gs1 = new GigaSpaceConfigurer(new SpaceProxyConfigurer("bllspace")).gigaSpace();

        SpaceProxyConfigurer spaceConfigurer = new SpaceProxyConfigurer("bllspace");
        //spaceConfigurer.lookupGroups("xap-16.0.0");
        spaceConfigurer.lookupGroups(args[1]);
        IJSpace space = spaceConfigurer.space();
        GigaSpace gs1 = new GigaSpaceConfigurer(space).gigaSpace();
        /*
        LocalViewSpaceConfigurer localViewConfigurer = new LocalViewSpaceConfigurer(new SpaceProxyConfigurer("test1"))
                .batchSize(1000)
                .batchTimeout(100)
                .maxDisconnectionDuration(1000*60*60)
                .addProperty("space-config.engine.memory_usage.high_watermark_percentage", "90")
                .addProperty("space-config.engine.memory_usage.write_only_block_percentage", "88")
                .addProperty("space-config.engine.memory_usage.write_only_check_percentage", "86")
                .addProperty("space-config.engine.memory_usage.retry_count", "5")
                .addProperty("space-config.engine.memory_usage.explicit", "false")
                .addProperty("space-config.engine.memory_usage.retry_yield_time", "50")
                .addViewQuery(new SQLQuery(com.gs.Data1.class, ""));*/
// Create local view:
        //GigaSpace localView = new GigaSpaceConfigurer(localViewConfigurer).gigaSpace();

        Feeder feeder = new Feeder(gs1);
        Integer entries = Integer.parseInt(args[0]);

        feeder.feed(entries);
        feeder.read(entries);
        //Thread.sleep(1000);
        //Data1 obj = localView.read(new Data1());
        //System.out.println("got object:" + obj);
    }

    enum WeekOfDay{
        SUNDAY,MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY
    }

    enum OrderCategory{
        GROCERY,AUTOMOBILE,OFFICE,RAW,MOBILE,ELECTRICAL,HARDWARE
    }
}
