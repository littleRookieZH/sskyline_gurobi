package main.java.com.twodimension;

import main.java.com.commonfunction.Print;
import main.java.com.twodimension.GetFile;
import main.java.com.twodimension.SkyLine;
import main.java.com.twodimension.TargetData;
import main.java.com.twodimensiondata.AdaptiveData;
import main.java.com.twodimensiondata.SkyLineResultData;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;


/**
 * @author zh15178381496
 * @create 2022-09 11:02
 * @说明：
 * @总结：
 */

/**
 * 首先初始化矩形序列，需要从文本中获取矩形数据
 *
 * @author pc
 */
public class Main {
    public static void main(String[] args) throws FileNotFoundException, InvocationTargetException, IllegalAccessException {
        GetFile getFile = new GetFile();
        File file = new File("E:\\SyncFiles\\BaiduSyncdisk\\小论文\\测试集\\测试集合集\\PART1\\ngcut");
        for (int i = 5; i < 10; i++) {
            List<File> allFile = getFile.getAllFile(file);
            for (File value : allFile) {
                String path = value.getAbsolutePath();
                String rePath = value.getName();
                System.out.println(rePath);
                String parentName = value.getParentFile().getName();
                String ppName = value.getParentFile().getParentFile().getName();
                double startTime = System.currentTimeMillis() / 1000.0;
                // 矩形信息
                TargetData targetData = new TargetData();
                List<TargetData> dataList = new ArrayList<>();
                dataList.add(targetData);
                // 天际线
                SkyLine skyLine1 = new SkyLine();
                AdaptiveData adaptiveData = new AdaptiveData();
                // SkyLineResultData skyLineResultData = skyLine1.fitPack(dataList, path);
                SkyLineResultData skyLineResultData = adaptiveData.initialAdaptive(dataList, path);
                if (skyLine1 == null || skyLineResultData== null) {
                    continue;
                }
                double endtime = System.currentTimeMillis() / 1000.0;
                double time = endtime - startTime;
//                System.out.println("总的覆盖缺陷块的次数：" + SkyLine.totalDef);
//                System.out.println("总的次数：" + SkyLine.totalTime);
                // 输出
                String outputFilePath = "E:\\SyncFiles\\BaiduSyncdisk\\小论文\\结果集\\启发式算法集\\自适应算法\\" + ppName + "\\" + parentName + "\\Test0" + i + "\\" + rePath + ".pack"; // 指定输出文件的路径
//                String outputFilePath = "C:\\Users\\浩\\Desktop\\ResultData02\\skyLine\\ORI16\\Test0" + i + "\\" + rePath + ".pack"; // 指定输出文件的路径
                Print.printResults(outputFilePath, skyLineResultData, time);
            }
        }
    }
}
