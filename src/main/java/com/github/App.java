package com.github;

import sun.misc.BASE64Decoder;

import java.io.*;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 转换mht文件，主要针对QQ聊天记录导出的mht文件.
 */
public class App 
{
    private final static Pattern tableStartPattern = Pattern.compile("(<table .*?>)");
    private final static Pattern tableEndPattern = Pattern.compile("(</table>)");
    //private final static Pattern resourceTypePattern = Pattern.compile("Content-Type:(.*)/(.*)");
    private final static Pattern resourceNamePattern = Pattern.compile("Content-Location:(.*)");
    /**
     * 自动分页转换的阈值，超过该大小则自动分页转换，单位：字节
     */
    private final static int autoPageSize = 100 * 1024 * 1024;

    public static void main(String[] args) {
        String mainFilePath = System.getProperty("user.dir");

        Scanner scan = new Scanner(System.in);
        System.out.println("是否分页转换，分页请输入Y，否则任意输入（超过100M分页才会有效）：");
        /*
         * 判断是否还有输入
         */
        boolean isMult = false;
        while(scan.hasNext())
        {
            String next = scan.nextLine();
            if(next != null && !"".equals(next)) {
                if(next.toLowerCase().equals("y")) {
                    isMult = true;
                }
                break;
            }
        }
        scan.close();
        System.out.println("您选择了" + (isMult ? "" : "不") + "分页转换.");

        File mainFolder = new File(mainFilePath);
        if(!mainFolder.exists()) {
            System.out.println("主目录不存在.");
        }
        File[] files = mainFolder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.isFile() && file.getName() != null &&  file.getName().toLowerCase().endsWith(".mht")) {
                    return true;
                }
                return false;
            }
        });
        if(files == null || files.length == 0) {
            System.out.println("未找到.mht文件.");
            return;
        }
        System.out.println("总共待转换文件 " + files.length + " 个.");
        for (File file : files) {
            long size = file.length();
            if(size >= autoPageSize) {
                isMult = true;
            }else{
                isMult = false;
            }
            System.out.print("正在转换：" + file.getAbsolutePath());
            long startTime = System.currentTimeMillis();
            if(isMult) {
                readAndCreateMultFile(file.getAbsolutePath(), mainFilePath);
            }else{
                readAndCreateFile(file.getAbsolutePath(), mainFilePath);
            }
            long endTime = System.currentTimeMillis();
            System.out.println(". 耗时：" + (endTime - startTime) + "毫秒.");
        }
    }

    public static void test() {
        System.out.println( "Hello World!" );
        String inputFile = "D:\\Documents\\企点消息备份\\南通生产环境.mht";
        String outputFilePath = "D:\\Documents\\企点消息备份\\";
        //readAndCreateFile(inputFile, outputFilePath);
        //readAndCreateMultFile(inputFile, outputFilePath);
        //System.out.println(parseHtmlFileName(inputFile));
        //System.out.println(isHtmlStartTag("<html2 xmlns=\"http://www.w3.org/1999/xhtml\">"));

        /*Matcher matcher = tableEndPattern.matcher("</div></td></tr>\n" +
                "</table></body></html>");
        System.out.println(matcher.find());
        System.out.println(matcher.group());
        System.out.println(matcher.start());
        System.out.println(matcher.end());*/

        /*Matcher matcher = resourceTypePattern.matcher("Content-Type:image/png");
        System.out.println(matcher.find());
        System.out.println(matcher.group(1));
        System.out.println(matcher.group(2));
        System.out.println(matcher.start());
        System.out.println(matcher.end());*/

        /*Matcher matcher = resourceNamePattern.matcher("Content-Location:{DC689FEF-C3E2-43c7-A203-6F33DBC8309C}.dat");
        System.out.println(matcher.find());
        System.out.println(matcher.group(1));
        System.out.println(matcher.start());
        System.out.println(matcher.end());*/
    }

    /**
     * 解析资源存放文件夹名
     * @param inputFile
     * @return
     */
    private static String parseHtmlFileName(String inputFile) {
        return inputFile.substring(inputFile.lastIndexOf("\\") + 1, inputFile.lastIndexOf("."));
    }

    private static boolean isHtmlStartTag(String content) {
        String pattern = ".*<html .*>.*";
        return Pattern.matches(pattern, content);
    }

    private static boolean isHtmlEndTag(String content) {
        return content.contains("</html>");
    }

    /*private static String [] parseResourceType(String content) {
        Matcher matcher = resourceTypePattern.matcher(content);
        if(!matcher.find() || matcher.groupCount() != 2) {
            return null;
        }
        return new String[]{matcher.group(1), matcher.group(2)};
    }*/

    private static String parseResourceName(String content) {
        Matcher matcher = resourceNamePattern.matcher(content);
        if(!matcher.find() || matcher.groupCount() != 1) {
            return null;
        }
        return matcher.group(1);
    }

    /**
     * 创建单文件html
     * @param inputFile
     * @param outputFilePath
     */
    public static void readAndCreateFile(String inputFile, String outputFilePath) {
        String htmlFileName = parseHtmlFileName(inputFile);

        File file = new File(inputFile);
        BufferedInputStream fis = null;
        BufferedReader reader = null;
        try {
            fis = new BufferedInputStream(new FileInputStream(file));
            reader = new BufferedReader(new InputStreamReader(fis,"utf-8"),5*1024*1024);

            boolean isCreatedHtml = false, isHtmlContent = false;
            String line = "";
            StringBuilder sb = null;
            //String [] resType = null;
            String resName = null;
            //boolean isGetResType = false;
            boolean isGetResName = false;
            StringBuilder resSb = new StringBuilder();
            while((line = reader.readLine()) != null){
                if(!isCreatedHtml) {
                    if (isHtmlStartTag(line)) {
                        isHtmlContent = true;
                        sb = new StringBuilder(line).append("\n");
                    }else{
                        if(isHtmlContent) {
                            if (isHtmlEndTag(line)) {
                                sb.append(line).append("\n");
                                createHtmlFile(outputFilePath, htmlFileName, sb.toString(), 0, true);
                                sb.delete(0, sb.length());
                                isCreatedHtml = true;
                            } else {
                                sb.append(line).append("\n");
                            }
                        }
                    }
                }

                /**
                 * 开始解析资源文件
                 */
                if(isCreatedHtml) {
                    /*if(!isGetResType) {
                        resType = parseResourceType(line);
                        if (resType != null) {
                            isGetResType = true;
                            continue;
                        }
                    }*/
                    if(!isGetResName) {
                        resName = parseResourceName(line);
                        if (resName != null) {
                            isGetResName = true;
                            resSb.delete(0, resSb.length());
                            continue;
                        }
                    }
                    if(isGetResName) {
                        if(line.length() > 0) {
                            if(line.contains("------=_NextPart_")) {
                                //isGetResType = false;
                                isGetResName = false;
                                generateImage(resSb.toString(), (outputFilePath + File.separator + htmlFileName), resName);
                            }else{
                                resSb.append(line).append("\n");
                            }
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(reader != null) {
                    reader.close();
                }
                if(fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 创建分页html
     * @param inputFile
     * @param outputFilePath
     */
    public static void readAndCreateMultFile(String inputFile, String outputFilePath) {
        String htmlFileName = parseHtmlFileName(inputFile);

        File file = new File(inputFile);
        BufferedInputStream fis = null;
        BufferedReader reader = null;
        try {
            fis = new BufferedInputStream(new FileInputStream(file));
            reader = new BufferedReader(new InputStreamReader(fis,"utf-8"),5*1024*1024);

            boolean isCreatedHtml = false, isTableContent = false;
            String line = "";
            StringBuilder sb = null;
            int trLine = 1;
            int htmlNo = 1;
            //String [] resType = null;
            String resName = null;
            //boolean isGetResType = false;
            boolean isGetResName = false;
            StringBuilder resSb = new StringBuilder();
            while((line = reader.readLine()) != null){
                if(!isCreatedHtml) {
                    Matcher startMatcher = tableStartPattern.matcher(line);
                    if (startMatcher.find()) {
                        isTableContent = true;
                        /**
                         * 将table后面的内容拼接起来
                         */
                        sb = new StringBuilder(line.substring(startMatcher.end())).append("\n");
                    }else{
                        if(isTableContent) {
                            trLine++;
                            Matcher endMacher = tableEndPattern.matcher(line);
                            if (endMacher.find()) {
                                sb.append(line.substring(0, endMacher.start()));
                                createHtmlFile(outputFilePath, htmlFileName, sb.toString(), htmlNo, true);
                                isCreatedHtml = true;
                            } else {
                                sb.append(line).append("\n");
                                if(trLine % 1000 == 0) {
                                    createHtmlFile(outputFilePath, htmlFileName, sb.toString(), htmlNo, false);
                                    htmlNo ++;
                                    sb.delete(0, sb.length());
                                }
                            }
                        }
                    }
                }

                /**
                 * 开始解析资源文件
                 */
                if(isCreatedHtml) {
                    /*if(!isGetResType) {
                        resType = parseResourceType(line);
                        if (resType != null) {
                            isGetResType = true;
                            continue;
                        }
                    }*/
                    if(!isGetResName) {
                        resName = parseResourceName(line);
                        if (resName != null) {
                            isGetResName = true;
                            resSb.delete(0, resSb.length());
                            continue;
                        }
                    }
                    if(isGetResName) {
                        if(line.length() > 0) {
                            if(line.contains("------=_NextPart_")) {
                                //isGetResType = false;
                                isGetResName = false;
                                generateImage(resSb.toString(), (outputFilePath + File.separator + htmlFileName), resName);
                            }else{
                                resSb.append(line).append("\n");
                            }
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(reader != null) {
                    reader.close();
                }
                if(fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 创建html文件
     * @param outputFilePath
     * @param htmlFileName
     * @param htmlContent
     */
    public static void createHtmlFile(String outputFilePath, String htmlFileName, String htmlContent, int htmlNo, boolean isEndCreate) {
        try {
            String folderName = htmlFileName;
            htmlFileName = "{0}" + htmlFileName;
            outputFilePath = outputFilePath.endsWith("\\") ? outputFilePath : outputFilePath + File.separator;

            File htmlFolder = new File(outputFilePath + folderName);
            if(htmlFolder.exists()){
                if(htmlNo == 0 || htmlNo == 1) {
                    File[] files = htmlFolder.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            file.delete();
                        }
                    }
                }
            }else{
                htmlFolder.mkdirs();
                htmlFolder.setWritable(true);
                htmlFolder.setReadable(false);
            }

            File file = new File(outputFilePath + folderName + File.separator + (htmlNo > 0 ? (htmlFileName + "-" + htmlNo) : htmlFileName) + ".html");
            if(!file.exists()){
                file.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            BufferedWriter bw = new BufferedWriter(osw);

            if(htmlNo == 0) {
                bw.write(htmlContent);
            }else {
                StringBuilder sb = new StringBuilder("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />" +
                        "<title>QQ Message</title><style type=\"text/css\">body{font-size:12px; line-height:22px; margin:2px;}td{font-size:12px; line-height:22px;}</style></head><body>");
                sb.append("<table width=100% cellspacing=0>");
                if(!isEndCreate) {
                    sb.append("<tr>");
                    if(htmlNo == 1) {
                        sb.append("<td>");
                    }else{
                        sb.append("<td style=\"height:40px;\">");
                    }
                    if(htmlNo > 1) {
                        sb.append("<a href=\"").append(htmlFileName).append("-").append(htmlNo - 1).append(".html\">上一页</a>").append("&nbsp;&nbsp;&nbsp;");
                    }
                    sb.append("<a href=\"").append(htmlFileName).append("-").append(htmlNo + 1).append(".html\">下一页</a>");
                    sb .append("</td><tr>");
                }else{
                    if(htmlNo > 1) {
                        sb.append("<tr><td style=\"height:40px;\"><a href=\"").append(htmlFileName).append("-").append(htmlNo - 1).append(".html\">上一页</a></td><tr>");
                    }
                }
                sb.append(htmlContent).append("</table></body></html>");
                bw.write(sb.toString());
            }

            bw.close();
            osw.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 对字节数组字符串进行Base64解码并生成图片
     * @param imgStr 图片数据
     * @param imgFilePath 保存图片全路径地址
     * @param filename 保存的附件名称
     * @return
     */
    public static String generateImage(String imgStr, String imgFilePath, String filename) {
        /**
         * 判断保存图片的路径是否存在，如果不存在则创建此路径中。
         */
        File file = new File(imgFilePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        /**
         * 图像数据为空
         */
        if (imgStr == null) {
            return null;
        }
        BASE64Decoder decoder = new BASE64Decoder();
        try {
            /**
             * Base64解码
             */
            byte[] b = decoder.decodeBuffer(imgStr);
            for (int i = 0; i < b.length; ++i) {
                /**
                 * 调整异常数据
                 */
                if (b[i] < 0) {
                    b[i] += 256;
                }
            }

            // 生成图片
            OutputStream out = new FileOutputStream(imgFilePath + File.separator + filename);
            out.write(b);
            out.flush();
            out.close();
            return filename;
        } catch (Exception e) {
            return null;
        }
    }
}
