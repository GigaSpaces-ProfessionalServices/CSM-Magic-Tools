//package com.web.dihx.controller;
//
//import com.google.gson.Gson;
//import com.web.dihx.model.Builder;
//import org.jose4j.json.internal.json_simple.JSONObject;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.ModelAttribute;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.web.servlet.ModelAndView;
//import org.springframework.web.servlet.view.InternalResourceViewResolver;
//
//import java.util.HashMap;
//
//@Controller
//public class DIHxController_backup {
//
//    @RequestMapping("/final")
//    public ModelAndView finalDesign() {
//        ModelAndView modelAndView = new ModelAndView();
//        modelAndView.setViewName("finalDesign");
//        modelAndView.addObject("serviceURL","/two?id="+10);
//        return modelAndView;
//    }
//    @RequestMapping("/one")
//    public ModelAndView getOne() {
//        ModelAndView modelAndView = new ModelAndView();
//        modelAndView.setViewName("one");
//        return modelAndView;
//    }
//    @RequestMapping("/two")
//    public ModelAndView getTwo(Integer id) {
//        System.out.println("ID == " + id);
//        ModelAndView modelAndView = new ModelAndView();
//        modelAndView.setViewName("two");
//        return modelAndView;
//    }
//    @RequestMapping("/three")
//    public ModelAndView getThree() {
//        ModelAndView modelAndView = new ModelAndView();
//        modelAndView.setViewName("three");
//        return modelAndView;
//    }
//
//    @GetMapping("/")
//    public ModelAndView getLogin() {
//        ModelAndView modelAndView = new ModelAndView();
//        modelAndView.setViewName("login");
//        return  modelAndView;
//    }
//
//    @RequestMapping("/inprogress")
//    public ModelAndView tempInProgress() {
//        ModelAndView modelAndView = new ModelAndView();
//        modelAndView.setViewName("in-progress");
//        return  modelAndView;
//    }
//
//    @RequestMapping("/createCluster")
//    public ModelAndView createCluster(Builder builder) {
//
//        HashMap<String, Integer> map = new HashMap<>();
//
//        String params = "name="+builder.getName()
//                +"&region="+builder.getRegion()
//                +"&region="+builder.getRegion()
//                +"&accessKey="+builder.getAccessKey()
//                +"&secretKey="+builder.getSecretKey()
//                ;
//        String url = "http://127.0.0.1:5000/createCluster?"+params;
//        RestTemplate rTemp = new RestTemplate();
//        String result = rTemp.getForObject(url, String.class);
//
//        System.out.println(result);
//        /*Gson gson = new Gson();
//        JSONObject jsonObject = gson.fromJson(result, JSONObject.class);*/
//
//        /*String url = "http://192.168.1.221:5000/listCluster";
//        RestTemplate rTemp = new RestTemplate();
//        String result = rTemp.getForObject(url, String.class);
//
//        System.out.println(result);
//        Gson gson = new Gson();
//        JSONObject jsonObject = gson.fromJson(result, JSONObject.class);*/
//
//        ModelAndView modelAndView = new ModelAndView();
//
//        modelAndView.setViewName("in-progress");
//        //modelAndView.addObject("services",jsonObject);
//        return modelAndView;
//    }
//
//    @RequestMapping("/services")
//    public ModelAndView getServices() {
//        HashMap<String, Integer> map = new HashMap<>();
//        ModelAndView modelAndView = new ModelAndView();
//        modelAndView.setViewName("services");
////        String params = "name="+"dihx2";
//        String url = "http://127.0.0.1:5000/services";
//        RestTemplate rTemp = new RestTemplate();
//        String result = rTemp.getForObject(url, String.class);
//        Gson gson = new Gson();
//        JSONObject jsonObject = gson.fromJson(result, JSONObject.class);
//        System.out.println(jsonObject);
//        modelAndView.addObject("services",jsonObject);
//        return modelAndView;
//    }
//
//
//    @RequestMapping("/createBuilder")
//    public ModelAndView createBuilder() {
//        InternalResourceViewResolver internalResourceViewResolver;
//        ModelAndView modelAndView = new ModelAndView();
//        System.out.println("In builder");
//        try {
////            modelAndView.setViewName("frameset1");
//            modelAndView.addObject("builder",new Builder());
//            modelAndView.setViewName("one");
//        } catch(Exception e) {
//            modelAndView.addObject("error", "Error in retrieving data types. ");
//        }
//        return  modelAndView;
//    }
//
//    @PostMapping("/builder")
//    public ModelAndView getBuilder(@ModelAttribute("builder") Builder builder) {
//
//        System.out.println("*******************"+builder.getNoOfPartitions());
//        System.out.println("*******************"+builder.getAccessKey());
//
//        ModelAndView modelAndView = new ModelAndView();
//
//        //Engine.storeBuilderData(builder);
//        //HS: python REST URL
//        /*String uri = "http://localhost:8080/springrestexample/employees.xml";
//
//        RestTemplate restTemplate = new RestTemplate();
//        String result = restTemplate.getForObject(uri, String.class);
//        //HS: Use google gson object to convert string to Json object.
//        System.out.println(result);
//        */
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("1", "a");
//        jsonObject.put("2", "b");
//        jsonObject.put("3", "c");
//        System.out.println(jsonObject);
//
//        String url = "http://127.0.0.1:5000/services";
//        RestTemplate rTemp = new RestTemplate();
//        String result = rTemp.getForObject(url, String.class);
//        Gson gson = new Gson();
//        JSONObject jsonObject1 = gson.fromJson(result, JSONObject.class);
//        modelAndView.addObject("clusters","List Clusters here");
//        modelAndView.addObject("services",jsonObject1);
//        modelAndView.setViewName("a");
//        return modelAndView;
//
//    }
//}