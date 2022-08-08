package com.gs.util;

import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder;
import org.openspaces.core.space.EmbeddedSpaceFactoryBean;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TieredStorageImpl {
    public static void main(String[] args) {
        try{
            BufferedReader buf = new BufferedReader(new FileReader("TieredData.tab"));
            Map<String,String> words = new HashMap<>();
            String lineJustFetched = null;
            String[] wordsArray;
            String[] tabSplitted;

            while(true){
                lineJustFetched = buf.readLine();
                if(lineJustFetched == null){
                    break;
                }else{

                    System.out.println(lineJustFetched);
                    wordsArray = lineJustFetched.split("\t");
                    System.out.println("Class :"+wordsArray[0]+" Property :"+wordsArray[1]+" Criteria :"+wordsArray[2]);



                }
            }
            /*
            for(String each : words){
                System.out.println(each);
            }
            */
            buf.close();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
