package main.java.exactsolution.onedimensionalcontiguousmodifygurobi;


import ilog.concert.*;
import ilog.cplex.IloCplex;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hao
 * @description 计算下界
 * @date 2023/3/6 15:28
 * @return
 */
public class LowerBound {
    /**
     * @param rectangleSize
     * @return int
     * @description 这里是根据 层数确定高度
     * @author hao
     * @date 2023/6/30 15:27
     */
    public int placeLayersNumber1(int[][] rectangleSize, int widthLength, int[][] defectiveBlocksSize) {
        int total = 0;
        for (int[] arr1 : rectangleSize) {
            total += arr1[0];
        }
        for (int[] arr1 : defectiveBlocksSize) {
            total += arr1[0];
        }
        int lay = total / widthLength;
        int[] array = sortArray(rectangleSize);
        total = 0;
        for (int i = 0; i < lay; i++) {
            total += array[i];
        }
//        int defHeight = maxDefHeight(defectiveBlocksSize, widthLength);
        return total;
    }

    public int[] sortArray(int[][] rectangleSize) {
        int[] tempArray = new int[rectangleSize.length];
        for (int i = 0; i < rectangleSize.length; i++) {
            tempArray[i] = rectangleSize[i][1];
        }
        for (int i = 0; i < tempArray.length; i++) {
            int minIndex = i;
            for (int k = i; k < tempArray.length; k++) {
                if (tempArray[k] <= tempArray[minIndex]) {
                    minIndex = k;
                }
            }
            //交换i和minIndex的值
            int temp = tempArray[minIndex];
            tempArray[minIndex] = tempArray[i];
            tempArray[i] = temp;
        }
        return tempArray;
    }
    /**
     * @description 缺陷块的最大高度：max{ 平均高度, 最大缺陷块高度 }
     * @author hao
     * @date 2023/6/30 15:22
     * @param defectiveBlocksSize
     * @param widthLength
     * @return int
     */
//    public int maxDefHeight(int[][] defectiveBlocksSize,int widthLength){
//        int height1 ;
//        int area = 0;
//        for (int[] arr1: defectiveBlocksSize) {
//            area += arr1[0] * arr1[1];
//        }
//        height1 = area / widthLength;
//        int minHeight = defectiveBlocksSize[0][1];
//        for(int[] arr2 : defectiveBlocksSize){
//            minHeight = Math.max(minHeight, arr2[1]);
//        }
//        return Math.max(minHeight, height1);
//    }

    /**
     * @param recArray
     * @param width
     * @param defectiveBlocksSize
     * @return int
     * @description 计算简单下界：目标块均值高度 + 缺陷块的最大高度
     * @author hao
     * @date 2023/6/30 15:36
     */
    public static int averageHeight(int[][] recArray, int width, int[][] defectiveBlocksSize) {
        double areaSum = 0;
        for (int i = 0; i < recArray.length; i++) {
            areaSum += recArray[i][0] * recArray[i][1] * recArray[i][2];
        }
        if (defectiveBlocksSize != null) {
            for (int i = 0; i < defectiveBlocksSize.length; i++) {
                areaSum += defectiveBlocksSize[i][0] * defectiveBlocksSize[i][1];
            }
        }

        int height = (int) Math.ceil(areaSum / width);
        return height;
    }

    @Test
    public void test01() {
        LowerBound lowerBound = new LowerBound();
        int[][] rec = {{4, 5}, {3, 6}, {7, 4}, {9, 7}};
        int[][] rec1 = {{4, 5}, {3, 6}};
        int width = 10;
    }

    public void NCBP(ModeRequiredData modeRequiredData) throws IloException {


        IloCplex ncbpModel = new IloCplex();
        // 定义无界整型变量
        IloIntVar x = ncbpModel.intVar(Integer.MIN_VALUE, Integer.MAX_VALUE, "x");
        IloLinearNumExpr objective = ncbpModel.linearNumExpr();
        objective.addTerm(1, x);  // 目标函数为 x
        ncbpModel.addMinimize(objective);  // 最小化目标函数

        int[] widthPoints = modeRequiredData.widthPoints;
        int blockNum = modeRequiredData.targetBlockSize.length;

        boolean[][] widthPlacedPoints = modeRequiredData.widthPlacedPoints;

        int Height = modeRequiredData.oriSize[1];

        // 添加求解模型的时间限制
        ncbpModel.setParam(IloCplex.DoubleParam.TiLim, ModeRequiredData.X_CHECK_TIMELIMIT);
        // 打印宽度可放置点
        Map<Integer, List<Integer>> map = new HashMap<>();
        for (int i = 0; i < widthPlacedPoints.length; i++) {
            List<Integer> list = new ArrayList<>();
            for (int j = 0; j < widthPlacedPoints[i].length; j++) {
                if (widthPlacedPoints[i][j]) {
                    list.add(j);
                }
            }
            map.put(i, list);
        }
//        for(Map.Entry<Integer, List<Integer>> entry : map.entrySet()){
//            System.out.println("key = " + entry.getKey());
//            System.out.println("value = " + entry.getValue());
//        }
//        System.out.println("===========");


        IloNumVar[][] ait = new IloNumVar[blockNum][];


    }

}
