package com.gs.csm;

import java.io.*;
import org.apache.commons.lang3.StringEscapeUtils;

import static com.gs.csm.GsFactory.*;
import static com.gs.csm.GsFactory.csvFile;

public class CreatePojoFromCsvHeader {

    public static void main(String[] args) throws IOException {
        packageName=System.getenv("PACKAGE");
        pojoOutputDirectory=System.getenv("POJO_OUTPUT_DIRECTORY");
        csvFile=System.getenv("CSV_FILE");
        File csvObjectFile = new File(csvFile);
        String className=args[0];

        createPojoFromCsvHeader(csvObjectFile,pojoOutputDirectory,packageName,className);
    }
    public static void createPojoFromCsvHeader(File csvInputFile, String directoryOfjavaFile, String packageName, String className)
    {
        try(BufferedReader stream = new BufferedReader(new FileReader(csvInputFile))) {
            String packagePath=packageName.replace(".","/");
            String javaOutputDirPath=directoryOfjavaFile+"/"+packagePath+"/";
            System.out.println("creating directory ->"+javaOutputDirPath);
            File f=new File(javaOutputDirPath);
            if(f.mkdirs()){
                System.out.println("directory :"+javaOutputDirPath+" created succesfully..");
            }else{
                System.out.println("directory :"+javaOutputDirPath+" already exist..");
            }
            String javaOutputFilePath=directoryOfjavaFile+"/"+packagePath+"/"+className+".java";
            File javaOutPutFile=new File(javaOutputFilePath);
            javaOutPutFile.createNewFile();
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(javaOutputFilePath)));
            System.out.println("generating class..");
            createClassHeader(out,className);

            //Create auto generated Id
//            out.println("\t\tprivate String Id = \"Id\";");

            String line = null;
            String[] fields = null;
            int rowNum = 0;
            while ((line = stream.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                } else {
                    if (fields == null) {
                        fields = line.split(",");
                    }

                    rowNum++;
                    String[] values = line.split(",");
                    for (int i = 0; i < fields.length; i++) {
                        out.println("\t\tprivate String " + fields[i] + " = \""+ StringEscapeUtils.escapeJava(values[i])+ "\";");
                    }
                    //Create auto generated Id setter & getter
//                    createAutoGenId(out);

                    for (int i = 0; i < fields.length; i++) {
                        String tempField=StringEscapeUtils.escapeJava(values[i]).substring(0, 1).toUpperCase()+StringEscapeUtils.escapeJava(values[i]).substring(1);

                        //getter method
                        out.println("");
                        out.println("\t\tpublic String  get"+tempField+ "(){");
                        out.println("\t\t\treturn this."+StringEscapeUtils.escapeJava(values[i])+";");
                        out.println("\t\t}");
                        //setter method
                        out.println("\t\tpublic void  set"+tempField+"(String "+ StringEscapeUtils.escapeJava(values[i])+"){");
                        out.println("\t\t\t this."+StringEscapeUtils.escapeJava(values[i])+" = "+ StringEscapeUtils.escapeJava(values[i])+";");
                        out.println("\t\t}");
                    }

                    out.println("");
                    out.println("// toString() Method");
                    out.println("\t\t public String toString(){");
                    StringBuffer buffer=new StringBuffer();
                    buffer.append("\"{");
                    for (int i = 0; i < fields.length; i++) {
                        buffer.append("\\\""+StringEscapeUtils.escapeJava(values[i])+"\\\"=\"+"+StringEscapeUtils.escapeJava(values[i]));
                        if(i < fields.length-1){
                            buffer.append("+\",");
                        }
                        else{
                            buffer.append("+\"");
                        }
                    }
                    buffer.append("}\";");
                    out.println("\t\t\t return "+buffer);
                    out.println("\t\t}");
                }
                out.println("}");
                out.close();
            }
            System.out.println("no of lines fetch from csv :"+rowNum);
        }catch(IOException e)
        {
            e.printStackTrace();
        }

    }

    public static void createClassHeader(PrintWriter out, String className)
    {
        out.println("package "+packageName+";");
        out.println("");
        out.println("import com.gigaspaces.annotation.pojo.SpaceId;");
        out.println("");
        out.println("public class " + className + " {");
    }

    public static void createAutoGenId(PrintWriter out)
    {
        //Create auto generated Id
        //getter method
        out.println("");
        out.println("\t\t@SpaceId (autoGenerate = true)");
        out.println("\t\tpublic String  getId(){");
        out.println("\t\t\treturn this.Id;");
        out.println("\t\t}");
        //setter method
        out.println("\t\tpublic void  setId(String Id){");
        out.println("\t\t\t this.Id = Id;");
        out.println("\t\t}");
    }

}
