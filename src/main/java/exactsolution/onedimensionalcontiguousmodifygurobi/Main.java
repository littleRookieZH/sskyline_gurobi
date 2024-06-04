package main.java.exactsolution.onedimensionalcontiguousmodifygurobi;

import ilog.cplex.CpxException;
import main.java.com.commonfunction.Print;
import main.java.com.twodimension.GetFile;
import main.java.com.twodimension.TargetData;
import main.java.com.twodimensiondata.SkyLineResultData;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Main implements Serializable {
    public static void main(String[] args) throws Exception {
        GetFile getFile = new GetFile();
        File file = new File("E:\\SyncFiles\\BaiduSyncdisk\\小论文\\测试集\\测试集合集\\PART1");
        List<File> allFile = getFile.getAllFile(file);
        for (int i = 0; i < allFile.size(); i++) {
            File value = allFile.get(i);
            String path = value.getAbsolutePath();
            String rePath = value.getName();
            String parentName = value.getParentFile().getName();
            System.out.println("value.getName() " + rePath + "parentName " + parentName);
            double startTime = System.currentTimeMillis() / 1000.0;
            Date date = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            System.out.println("开始时间： " + formatter.format(date));
            TargetData rLayout = new TargetData();
            ModeRequiredData modeRequiredData = new ModeRequiredData();
            // 初始化模型
            rLayout.initData(path);
            System.out.println(rLayout.targetNum);
            if (rLayout.targetNum > 30) {
                continue;
            }
            String outputFilePath = "E:\\SyncFiles\\BaiduSyncdisk\\小论文\\结果集\\重测精确算法部分\\结果\\def04_Test_btot_g_3\\"+ parentName + "\\" + rePath + ".pack"; // 指定输出文件的路径
            SkyLineResultData exactAndHeuristic = modeRequiredData.initModel1(path, rLayout);
            if (exactAndHeuristic == null) {
                continue;
            }

            if (exactAndHeuristic.isHeuristic) {
                double endtime = System.currentTimeMillis() / 1000.0;
                double time = endtime - startTime;
                Print.printResultsExact(outputFilePath, exactAndHeuristic, time, rLayout.oriSize[1]);
            } else {
                MasterModel masterModel = new MasterModel();
                long modelStartTime = System.currentTimeMillis() / 1000;
                ModeRequiredData isSolveModel = null;
                try {
                    isSolveModel = masterModel.solveModel(exactAndHeuristic, modelStartTime, (long)ModeRequiredData.Solution_Times);
                } catch (CpxException e) {
                    System.out.println("模型太大");
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                double endtime = System.currentTimeMillis() / 1000.0;
                double time = endtime - startTime;
                System.out.println("time  " + time);
//            String outputFilePath = "C:\\Users\\浩\\Desktop\\ResultData 02\\exact\\timenedless02\\" + parentName  + "\\" + rePath + ".pack"; // 指定输出文件的路径
                if (exactAndHeuristic.isHeuristic) {
                    System.out.println(" 启发式的解 ");
                    Print.printResultsExact(outputFilePath, exactAndHeuristic, time,isSolveModel.LB);
                } else {
                    Print.printResultsCBP(outputFilePath, time, isSolveModel);
                }
            }
        }

    }
}
