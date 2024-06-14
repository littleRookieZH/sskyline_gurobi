package main.java.resultanaylize;

/**
 * @author xzbz
 * @create 2023-11-13 16:04
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

// 输出： 文件名、avg_gap、best_gap
public class ResultProcessingN {

    double[] LB;
    double height;
    double avgHeight;
    double bestHeight;
    int[][] rectangle;
    int[][] defBlack;
    int[] oriSize;

    public double readNowLB(File fileData) throws FileNotFoundException {
        String line = null;
        String[] substr = null;
        Scanner cin = new Scanner(new BufferedReader((new FileReader(fileData))));
//        substr = cin.nextLine().split("\\s+");
        //先读一行
        line = cin.nextLine(); // 空行
        line = cin.nextLine(); // 时间
        substr = cin.nextLine().split("\\s+"); // 物品数量
        int itemsNums = Integer.parseInt(substr[0]);

        substr = cin.nextLine().trim().split("\\s+"); // 条带尺寸
        int[] oriSize = new int[]{Integer.parseInt(substr[0]), Integer.parseInt(substr[1])};
        substr = cin.nextLine().split("\\s+"); // 缺陷块数量
        int defNums = Integer.parseInt(substr[0]);

        // 缺陷块尺寸
        int[][] defBlack = new int[defNums][4];
        double defArea = 0;
        //读取目标块数据
        for (int i = 0; i < defNums; i++) {
            line = cin.nextLine();
            substr = line.trim().split("\\s+");
            //宽度
            defBlack[i][0] = Integer.parseInt(substr[3]) - Integer.parseInt(substr[1]);
            //高度
            defBlack[i][1] = Integer.parseInt(substr[4]) - Integer.parseInt(substr[2]);
            defArea += defBlack[i][0] * defBlack[i][1];
        }

        line = cin.nextLine(); // 利用率

        //目标块尺寸
        int[][] rectangle = new int[itemsNums][2];
        double area = 0;
        //读取目标块数据
        for (int i = 0; i < itemsNums; i++) {
            line = cin.nextLine();
            substr = line.trim().split("\\s+");
            //宽度
            rectangle[i][0] = Integer.parseInt(substr[1]);
            //高度
            rectangle[i][1] = Integer.parseInt(substr[2]);
            area += rectangle[i][0] * rectangle[i][1];
        }
        return (defArea + area) / oriSize[0];
    }

    public Map<String, Double> readLB(File file) throws FileNotFoundException {
        String line = null;
        String[] substr = null;
        Map<String, Double> stringDoubleMap = new HashMap<String, Double>();
        Scanner cin = new Scanner(new BufferedReader((new FileReader(file))));
//        substr = cin.nextLine().split("\\s+");
        while (true) {
            substr = cin.nextLine().split("\\s+");
            if (substr.length == 1 || substr.length == 0) {
                break;
            }
            String name = null;
            name = substr[0];
//            System.out.println("nameLB   " + name);
            double tempLB = Double.parseDouble(substr[1]);
            stringDoubleMap.put(name, tempLB);
        }
        return stringDoubleMap;
    }

    public static void main(String[] args) throws FileNotFoundException {
        ResultProcessingN resultProcessingN = new ResultProcessingN();

        File fileVar = new File("C:\\BaiduNetdiskDownload\\Files\\数据处理\\def01\\BKW_Def");
        File fileLB = new File("C:\\BaiduNetdiskDownload\\Files\\数据处理\\BKW.txt");
        // 读取LB
        Map<String, Double> stringDoubleMap = resultProcessingN.readLB(fileLB);
        File[] files = fileVar.listFiles();
        HashMap<String, ArrayList<Double>> map = new HashMap<>();
        HashMap<String, Double> mapDef = new HashMap<>();

        int index = 0;
        // 次数
        for (int i = 0; i < files.length; i++) {
            File[] files1 = files[i].listFiles();
            // 组数 12
            if (files1.length != 12) {
                continue;
            }

            if (index >= 20) {
                break;
            }
            // 输出文件名
            if (index == 0) {
                for (int j = 1; j <= files1.length; j++) {
                    String[] split = files1[j - 1].getName().split("\\.");

//                    System.out.println(split);

                    map.put(split[0], new ArrayList<Double>());

//                  System.out.println(files1[j - 1].getName());

                    // 1个一组，计算下界
                    // 读取高度
                    double nowLB = resultProcessingN.readNowLB(files1[j - 1]);
                    mapDef.put(split[0], nowLB);

                }
            }
            // 每1个一组，计算高度
            for (int j = 1; j <= files1.length; j++) {
                // 读取高度
                double heightTotal = resultProcessingN.readHeight(files1[j - 1]);
                String name = files1[j - 1].getName().split("\\.")[0];

//                System.out.println(name);

                ArrayList<Double> list = map.get(name);
                list.add(heightTotal);
            }
            ++index;
        }

        // 计算avg、best
        for (Map.Entry<String, ArrayList<Double>> entry : map.entrySet()) {
            String key = entry.getKey();

            double avgHeight = 0;
            ArrayList<Double> value = entry.getValue();
            double bestHeight = value.get(0);
            for (double var : value) {
                avgHeight += var;
                if (bestHeight > var) {
                    bestHeight = var;
                }
            }
            avgHeight /= value.size();
            Double nowLB = mapDef.get(key);
            Double LB = stringDoubleMap.get(key);
            LB = Math.max(nowLB, LB);

            double avgGap = (avgHeight - LB) / LB;
            double bestGap = (bestHeight - LB) / LB;

            System.out.println(key + "," + LB + "," + avgGap + "," + bestGap);
        }
    }

    public int readHeight(File fileData) throws FileNotFoundException {
        String line = null;
        String[] substr = null;
        Scanner cin = new Scanner(new BufferedReader((new FileReader(fileData))));
        //先读一行
        line = cin.nextLine(); // 空行
        line = cin.nextLine(); // 时间
        cin.nextLine(); // 物品数量
        substr = cin.nextLine().trim().split("\\s+"); // 条带尺寸
        return Integer.parseInt(substr[1]);
    }

    // resFile:结果集的文件路径
    public HashMap<String, Double> getUpper(String resFile) throws FileNotFoundException {
        File fileVar = new File(resFile);
        File[] files = fileVar.listFiles();
        HashMap<String, ArrayList<Double>> map = new HashMap<>();
        HashMap<String, Double> map2 = new HashMap<>();

        int index = 0;
        // 次数
        for (int i = 0; i < files.length; i++) {
            File[] files1 = files[i].listFiles();
            // 组数 12
            if (files1.length != 12) {
                continue;
            }

            if (index >= 20) {
                break;
            }
            // 输出文件名
            if (index == 0) {
                for (int j = 1; j <= files1.length; j++) {
                    String[] split = files1[j - 1].getName().split("\\.");
                    map.put(split[0], new ArrayList<Double>());
                }
            }
            // 每1个一组，计算高度
            for (int j = 1; j <= files1.length; j++) {
                // 读取高度
                double heightTotal = readHeight(files1[j - 1]);
                String name = files1[j - 1].getName().split("\\.")[0];
                ArrayList<Double> list = map.get(name);
                list.add(heightTotal);
            }
            ++index;
        }

        // 计算avg、best
        for (Map.Entry<String, ArrayList<Double>> entry : map.entrySet()) {
            String key = entry.getKey();
            ArrayList<Double> value = entry.getValue();
            double bestHeight = value.get(0);
            for (double var : value) {
                if (bestHeight > var) {
                    bestHeight = var;
                }
            }
            map2.put(key, bestHeight);
        }
        return map2;
    }

}
