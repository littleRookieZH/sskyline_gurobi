package main.java.exactsolution.onedimensionalcontiguousmodifygurobi;

import gurobi.*;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import main.java.com.pointset.ToolClass;
import main.java.com.universalalgorithm.QuickSortInfeasible;

import java.util.*;

public class MinInfeasible {

    /**
     * @param step1Solutions
     * @description 获取 不可行解集
     * @author hao
     * @date 2023/7/9 3:23
     */
    public static List<int[][]> divideSolution(int[][] step1Solutions, ModeRequiredData modeRequiredData) throws IloException, GRBException {

        // 保存可行解
        HashSet<String> hashSetFeases = modeRequiredData.hashSetNonInfeases;
        // 保存区域
        HashSet<String> hashSetAreaInfeases = modeRequiredData.hashSetAreaInfeases;
        // 保存不可行解
        HashSet<String> hashSetInfeases = modeRequiredData.hashSetAreaInfeases;

        for (int i = 0; i < step1Solutions.length; i++) {
            System.out.println(Arrays.toString(step1Solutions[i]));
        }
        System.out.println();

        // 寻找最小不可行解的第一步：将离散点集划分为几个部分
        // 记录分界线
        List<Integer> list = new ArrayList<>();
        int[][] tmpArray = ToolClass.copyTwoDim(step1Solutions);
        // 以 yi 排序 （在原数组基础上排序）
        ToolClass.sortedTwoDim(tmpArray, 1);
        int max = 0;
        for (int i = 0; i < tmpArray.length - 1; i++) {
            max = Math.max(max, tmpArray[i][2]);

            if (tmpArray[i + 1][1] >= max) {
                // 说明找到分界线
                list.add(i);
            }
        }
        list.add(tmpArray.length - 1);
        int index = 0;
        List<List<int[]>> divList = new ArrayList<>();
        List<int[]> listTemp = new ArrayList<>();
        for (int i = 0; i <= tmpArray.length; i++) {
            if (i == tmpArray.length) {
                divList.add(listTemp);
                break;
            }
            if (i > list.get(index)) {
                divList.add(listTemp);
                index++;
                listTemp = new ArrayList<>();
            }
            listTemp.add(tmpArray[i]);
        }

        System.out.println("开始检验划分的子集");

        List<InfeasibleAreaData> divList1 = new ArrayList<>();


        for (int i = 0; i < divList.size(); i++) {
            System.out.println(" fdsf " + divList.size());
            InfeasibleAreaData infeasibleAreaData = new InfeasibleAreaData();
            int[][] tempSolutions = new int[divList.get(i).size()][3];
            int startH = divList.get(i).get(0)[1];
            int endH = divList.get(i).get(0)[2];
            for (int j = 0; j < divList.get(i).size(); j++) {
                for (int k = 0; k < 3; k++) {
                    tempSolutions[j][k] = divList.get(i).get(j)[k];
                }
                startH = Math.min(startH, tempSolutions[j][1]);
                endH = Math.max(endH, tempSolutions[j][2]);
            }

            for (int j = 0; j < tempSolutions.length; j++) {
                System.out.println(Arrays.toString(tempSolutions[j]));
            }

            double times1 = System.currentTimeMillis() / 1000;

//            if ((tempSolutions[0][0] == 5) && (tempSolutions[0][1] == 25) && (tempSolutions[0][2] == 28)) {
//                System.out.println(324);
//            }
            if ((hashSetFeases.contains(encodeT(tempSolutions))) || (hashSetInfeases.contains(encodeT(tempSolutions)))) {
                continue;
            }

            // if (tempSolutions.length >= 10) {
            //     boolean isExit = CheckCplex.xCheck(tempSolutions, modeRequiredData);
            //     ToolClass.sortedTwoDim(tempSolutions, 0);
            //     boolean isExit1 = CheckCplex.xCheck(step1Solutions, modeRequiredData);
            //     boolean isExit2 = CheckCplex.xCheck(tmpArray, modeRequiredData);
            //     ToolClass.sortedTwoDim(tmpArray, 0);
            //     boolean isExit3 = CheckCplex.xCheck(tmpArray, modeRequiredData);
            //     boolean isExit4 = CheckCplex.xCheck(tempSolutions, modeRequiredData);
            //     System.out.println(isExit);
            //     System.out.println(isExit1);
            //     System.out.println(isExit2);
            //     System.out.println(isExit3);
            //     System.out.println(isExit4);
            // }


            // 说明划分不了 或者 确定是不可行区域
            if ((tempSolutions.length == step1Solutions.length) || !CheckCplex.xCheck(tempSolutions, modeRequiredData)) {
                infeasibleAreaData.yPointsSolution = tempSolutions;
                infeasibleAreaData.startHeight = startH;
                infeasibleAreaData.endHeight = endH - 1;
                divList1.add(infeasibleAreaData);
            }
            double times2 = System.currentTimeMillis() / 1000;

            System.out.println("检验划分子集所需时间：" + (times2 - times1));
        }

        List<int[][]> infeasibleItems = reducedSolution(divList1, modeRequiredData);
        if(modeRequiredData.resultTFTimeOut){
            return null;
        }
        // 用于测试的
        // List<int[][]> infeasibleItems = new ArrayList<>();
        // for (int i = 0; i < divList1.size(); i++) {
        //     int[][] yPointsSolution = divList1.get(i).yPointsSolution;
        //     for (int j = 0; j < yPointsSolution.length; j++) {
        //         System.out.println(Arrays.toString(yPointsSolution[j]));
        //     }
        //     System.out.println("      -----        ");
        //     infeasibleItems.add(yPointsSolution);
        // }


        System.out.println("不可行解------");
        for (int i = 0; i < infeasibleItems.size(); i++) {
            for (int j = 0; j < infeasibleItems.get(i).length; j++) {
                System.out.println(Arrays.toString(infeasibleItems.get(i)[j]));
            }
            System.out.println();
        }
        return infeasibleItems;
//        return divList1;
    }

    /**
     * @param divList
     * @param modeRequiredData
     * @return List<int [ ] [ ]>
     * @description 求最小不可行解
     * @author hao
     * @date 2023/7/27 20:17
     */
    public static List<int[][]> reducedSolution(List<InfeasibleAreaData> divList, ModeRequiredData modeRequiredData) throws IloException, GRBException {

        HashSet<String> hashSetMinInfeases = modeRequiredData.hashSetMinInfeases;
        // 保存可行解
        HashSet<String> hashSetFeases = modeRequiredData.hashSetNonInfeases;
        // 保存区域
        HashSet<String> hashSetAreaInfeases = modeRequiredData.hashSetAreaInfeases;
        // 保存不可行解
        HashSet<String> hashSetInfeases = modeRequiredData.hashSetInfeases;
        int[][] targetBlockSize = modeRequiredData.targetBlockSize;
        // 拷贝一份 第二步按区域，挨个删除一列

        List<int[][]> infeasibleItemsStepArea = new ArrayList<>();
        //
        List<int[][]> infeasibleItemsStep3 = new ArrayList<>();

        // 输出一下哪些不可行解需要判断
        System.out.println("   需要判断的不可行解集   ");
        for (int i = 0; i < divList.size(); i++) {
            int[][] yPointsSolution = divList.get(i).yPointsSolution;
            for (int j = 0; j < yPointsSolution.length; j++) {
                System.out.println(Arrays.toString(yPointsSolution[j]));
            }
            System.out.println("      -----        ");
        }


        // 删除区域的起始行和结尾行 --- 容易出错，

        double times1 = System.currentTimeMillis() / 1000;

        // 每一个不可行解的子区域：
        for (int i = 0; i < divList.size(); i++) {
            InfeasibleAreaData infeasibleAreaData = divList.get(i);
            int[][] pointsSolution = infeasibleAreaData.yPointsSolution;
            // if ((pointsSolution[0][0] == 10) && (pointsSolution[0][1] == 23) && (pointsSolution[0][2] == 25)) {
            //     if ((pointsSolution[1][0] == 12) && (pointsSolution[1][1] == 24) && (pointsSolution[1][2] == 27)) {
            //         if ((pointsSolution[2][0] == 9) && (pointsSolution[2][1] == 26) && (pointsSolution[2][2] == 27)) {
            //             System.out.println(324);
            //         }
            //     }
            // }


            // 拷贝一份
            int[][] copyTwoDim1 = ToolClass.copyTwoDim(pointsSolution);


            // 由于yPointsSolution是排序好的结果，所以，areaPlaceP的结果是从小到大
            List<Integer> areaPlaceP = getAreaPlaceP(copyTwoDim1);
            // 从头删
            for (int j = 0; j < areaPlaceP.size(); j++) {
                // 统计所有不在当前列上的物品
                List<int[]> listTemp1 = new ArrayList<>();
                for (int[] tempArr : copyTwoDim1) {
                    if (tempArr[1] != areaPlaceP.get(j)) {
                        listTemp1.add(tempArr);
                    }
                }
                // list转为数组
                int[][] tempSolution = new int[listTemp1.size()][3];
                for (int k = 0; k < listTemp1.size(); k++) {
                    for (int m = 0; m < listTemp1.get(k).length; m++) {
                        tempSolution[k][m] = listTemp1.get(k)[m];
                    }
                }
                // 检验，如果当前数组不可行，继续删除当前数组，这里需要更新数组
                // 删除起始行，判断可不可行

                // 检验
                if (tempSolution.length == 0) {
                    hashSetInfeases.add(encodeT(copyTwoDim1));
                    if (hashSetAreaInfeases.add(encodeT(copyTwoDim1))) {
                        infeasibleItemsStepArea.add(copyTwoDim1);
                    }
                    continue;
                }

                // 如果可行
                if (hashSetFeases.contains(encodeT(tempSolution))) {
                    if (hashSetMinInfeases.add(encodeT(copyTwoDim1))) {
                        // 保存不可行解
                        hashSetInfeases.add(encodeT(copyTwoDim1));
                        infeasibleItemsStep3.add(copyTwoDim1);
                    }
                    break;
                }

                // 如果不可行
                if (hashSetInfeases.contains(encodeT(tempSolution))) {
                    continue;
                }

                if (CheckCplex.xCheck(tempSolution, modeRequiredData)) {
                    // 可行，说明当前从头删结束了，说明删除前copyTwoDim1是不可行的.
                    // copyTwoDim1.length != pointsSolution.length 表示第一次删除就失败时，不添加对应的序列；后续从后往前删除会考虑
                    if ((copyTwoDim1.length != pointsSolution.length) && hashSetAreaInfeases.add(encodeT(copyTwoDim1))) {
                        infeasibleItemsStepArea.add(copyTwoDim1);
                        // 保存可行解
                        hashSetFeases.add(encodeT(tempSolution));
                        // 保存不可行解
                        hashSetInfeases.add(encodeT(copyTwoDim1));
                    }
                    break;
                } else {
                    System.out.println("起始行删除成功");
                    // 保存不可行解
                    hashSetInfeases.add(encodeT(tempSolution));
                    // 更新不可行解
                    copyTwoDim1 = tempSolution;
                }

                double times2 = System.currentTimeMillis() / 1000;
                System.out.println("当前删除物品并检验所花的时间：" + (times2 - times1));

                if (isTimeExceeded((long)times1, (long)ModeRequiredData.CBP_TIMELIMIT)) {
                    // 超时
                    modeRequiredData.resultTFTimeOut = true;
                    return null;
                }
            }
            // 检验，如果当前数组可行，数组不变，从尾部开始删除
            // 尾部删除不可行，继续。如果可行退出。从尾删
            // 拷贝一份
            int[][] copyTwoDim2 = ToolClass.copyTwoDim(pointsSolution);
            // 由于yPointsSolution是排序好的结果，所以，areaPlaceP的结果是从小到大
            // 从尾删
            for (int j = areaPlaceP.size() - 1; j >= 0; j--) {
                // 统计所有不在当前列上的物品
                List<int[]> listTemp1 = new ArrayList<>();
                for (int[] tempArr : copyTwoDim2) {
                    if (tempArr[1] != areaPlaceP.get(j)) {
                        listTemp1.add(tempArr);
                    }
                }
                // list转为数组
                int[][] tempSolution = new int[listTemp1.size()][3];
                for (int k = 0; k < listTemp1.size(); k++) {
                    for (int m = 0; m < listTemp1.get(k).length; m++) {
                        tempSolution[k][m] = listTemp1.get(k)[m];
                    }
                }
                // 检验，如果当前数组不可行，继续删除当前数组，这里需要更新数组
                // 删除尾行，判断可不可行

                // 检验
                if (tempSolution.length == 0) {
                    // 加入不可行解
                    hashSetInfeases.add(encodeT(copyTwoDim2));
                    if (hashSetAreaInfeases.add(encodeT(copyTwoDim2))) {
                        infeasibleItemsStepArea.add(copyTwoDim2);
                    }
                    continue;
                }

                // 如果可行
                if (hashSetFeases.contains(encodeT(tempSolution))) {
                    if (hashSetMinInfeases.add(encodeT(copyTwoDim2))) {
                        // 保存不可行解
                        hashSetInfeases.add(encodeT(copyTwoDim2));
                        infeasibleItemsStep3.add(copyTwoDim2);
                    }
                    break;
                }

                // 如果不可行
                if (hashSetInfeases.contains(encodeT(tempSolution))) {
                    continue;
                }

                if (CheckCplex.xCheck(tempSolution, modeRequiredData)) {
                    // 可行，说明当前从尾删结束了，说明删除前copyTwoDim1是不可行的.
                    // copyTwoDim1.length != pointsSolution.length 表示第一次删除就失败时，不添加对应的序列；后续从后往前删除会考虑
                    if (hashSetAreaInfeases.add(encodeT(copyTwoDim2))) {
                        infeasibleItemsStepArea.add(copyTwoDim2);
                        // 保存可行解
                        hashSetFeases.add(encodeT(tempSolution));
                        // 保存不可行解
                        hashSetInfeases.add(encodeT(copyTwoDim2));
                    }
                    break;
                } else {
                    System.out.println("尾行删除成功");
                    // 保存不可行解
                    hashSetInfeases.add(encodeT(tempSolution));
                    // 更新不可行解
                    copyTwoDim2 = tempSolution;
                }
            }
            if (isTimeExceeded((long)times1, (long)ModeRequiredData.CBP_TIMELIMIT)) {
                // 超时
                modeRequiredData.resultTFTimeOut = true;
                return null;
            }
        }
//        for (int i = 0; i < divList.size(); i++) {
//            System.out.println("删除行");
//            long startTime1 = System.currentTimeMillis() / 1000;
//            InfeasibleAreaData infeasibleAreaData = divList.get(i);
//            // 对每个区域而言
//            int[][] yPointsSolution = infeasibleAreaData.yPointsSolution;
//            // 删除起始行，判断可不可行
//            int[][] lineStartArray = deleteLineArray(infeasibleAreaData.startHeight, yPointsSolution);
//            // 检验
//            if (filterHashSet.add(encode(lineStartArray))) {
//                System.out.println("---------woyanjianlema ------ ");
//                if (lineStartArray.length != 0 && CheckCplex.xCheck(lineStartArray, modeRequiredData)) {
//                    // 如果可行，将加入原区域的解加入其中，如果不可行，将当前删除的解加入其中
//                    if (filterHashSet.add(encode(yPointsSolution))) {
//                        infeasibleItemsStepArea.add(yPointsSolution);
//                    }
//                } else {
//                    System.out.println("起始行删除成功");
//                    infeasibleItemsStepArea.add(lineStartArray);
//                }
//            } else {
//                // 删除起始行的解集已经出现过了，需要判断当前的解是否出现过，如果没有出现，添加
//                if (filterHashSet.add(encode(yPointsSolution))) {
//                    System.out.println("删除起始行的解集已经出现过了");
//
//                    infeasibleItemsStepArea.add(yPointsSolution);
//                }
//            }
//            long endTime1 = System.currentTimeMillis() / 1000;
//
//            System.out.println("按起始行删除时间：" + (endTime1 - startTime1));
//            // 删除(尾-1)行，判断可不可行
//            int[][] lineEndArray = deleteLineArrayEnd(infeasibleAreaData.endHeight, yPointsSolution);
//            if (filterHashSet.add(encode(lineEndArray))) {
//                if (lineEndArray.length != 0 && CheckCplex.xCheck(lineEndArray, modeRequiredData)) {
//                    // 如果可行，将加入原区域的解加入其中，如果不可行，将当前删除的解加入其中
//                    if (filterHashSet.add(encode(yPointsSolution))) {
//                        infeasibleItemsStepArea.add(yPointsSolution);
//                    }
//                } else {
//                    System.out.println("尾行删除成功");
//                    infeasibleItemsStepArea.add(lineEndArray);
//                }
//            }else {
//                // 删除尾行的解集已经出现过了，需要判断当前的解是否出现过，如果没有出现，添加
//                if (filterHashSet.add(encode(yPointsSolution))) {
//                    System.out.println("删除尾行的解集已经出现过了");
//                    infeasibleItemsStepArea.add(yPointsSolution);
//                }
//            }
//            long endTime2 = System.currentTimeMillis() / 1000;
//
//            System.out.println("按尾行行删除时间：" + (endTime2 - endTime1));
//
//        }


        System.out.println("----hghfh----");
        for (int i = 0; i < infeasibleItemsStepArea.size(); i++) {
            for (int j = 0; j < infeasibleItemsStepArea.get(i).length; j++) {
                System.out.println(Arrays.toString(infeasibleItemsStepArea.get(i)[j]));
            }
            System.out.println("----hghfh----");
        }
        System.out.println("----hghfh----");

        // 给动态数组添加时间限制
        long startTime = System.currentTimeMillis() / 1000;

        System.out.println("我即将删除面积");
        // 以面积递增排序，逐个删除，判断是否可行(理论上 infeasibleItemsStep2 是包含 divListTemp )
        for (int i = 0; i < infeasibleItemsStepArea.size(); i++) {

            // if ((infeasibleItemsStepArea.get(i)[0][0] == 7) && (infeasibleItemsStepArea.get(i)[0][1] == 18) &&
            //         (infeasibleItemsStepArea.get(i)[0][2] == 27)) {
            //     System.out.println(2);
            // }

            System.out.println("我删除了面积");
            int[][] arrayTemp = infeasibleItemsStepArea.get(i);
            QuickSortInfeasible.quickSortInfeasible(arrayTemp, targetBlockSize);
            // 改动版
            // 每一个子区域的，以面积递增逐个删除目标块
            int end = arrayTemp.length;
            int index = 0;
            for (int j = 0; j < end; j++) {

                if (isTimeExceeded((long)times1, (long)ModeRequiredData.CBP_TIMELIMIT)) {
                    // 超时
                    modeRequiredData.resultTFTimeOut = true;
                    return null;
                }

                System.out.println("开始删除目标块 " + j);
                double times3 = System.currentTimeMillis() / 1000;
                int[][] deleteArray = deleteArray(arrayTemp, index);

                // 删除之后无元素
                if (deleteArray.length == 0) {
                    if (hashSetMinInfeases.add(encodeT(arrayTemp))) {
                        // 保存不可行解
                        hashSetInfeases.add(encodeT(arrayTemp));
                        infeasibleItemsStep3.add(arrayTemp);
                    }
                    continue;
                }

                // delete有元素
                // 不可行集中，已经存在。也就是后续的序列不用再判断,进行下一个区域的判断
                if (hashSetInfeases.contains(encodeT(deleteArray))) {
                    if ((arrayTemp.length - 1) == index) {
                        if (hashSetMinInfeases.add(encodeT(deleteArray))) {
                            // 保存不可行解
                            hashSetInfeases.add(encodeT(deleteArray));
                            infeasibleItemsStep3.add(deleteArray);
                        }
                    }
                    break;
                }
                // 可行集中，已经存在。当前位置的序号不同，会影响后续的判断。所以执行跳过当前位置，继续判断
                if (hashSetFeases.contains(encodeT(deleteArray))) {
                    if (((arrayTemp.length - 1) == index) && hashSetMinInfeases.add(encodeT(arrayTemp))) {
                        // 保存不可行解
                        hashSetInfeases.add(encodeT(arrayTemp));
                        infeasibleItemsStep3.add(arrayTemp);
                    }
                    index++;
                    continue;
                }
                // 检查
                if (CheckCplex.xCheck(deleteArray, modeRequiredData)) {
                    System.out.println(" 按面积递增删除失败 ");
                    // 保存可行解
                    hashSetFeases.add(encodeT(deleteArray));
                    // 减一是因为index是索引，只有删除到最后才获得最小不可行解集
                    if (((arrayTemp.length - 1) == index) && hashSetMinInfeases.add(encodeT(arrayTemp))) {
                        // 保存不可行解
                        hashSetInfeases.add(encodeT(arrayTemp));
                        infeasibleItemsStep3.add(arrayTemp);
                    }
                    index++;
                    continue;
                } else {
                    // 最后一个元素不可行
                    if ((arrayTemp.length - 1) == index) {
                        if (hashSetMinInfeases.add(encodeT(deleteArray))) {
                            // 保存不可行解
                            hashSetInfeases.add(encodeT(deleteArray));
                            infeasibleItemsStep3.add(deleteArray);
                        }
                    }
                    // 不是最后一个元素
                    System.out.println(" 按面积递增删除成功 ");
                    // 保存不可行解
                    hashSetInfeases.add(encodeT(deleteArray));
                    // 更新不可行解
                    arrayTemp = deleteArray;
                }

                double times4 = System.currentTimeMillis() / 1000;
                System.out.println("当前删除物品并检验所花的时间：" + (times4 - times3));

            }
        }

        // for (int i = 0; i < infeasibleItemsStep3.size(); i++) {
        //     ToolClass.sortedTwoDim(infeasibleItemsStep3.get(i), 0);
        //     if(CheckCplex.xCheck(infeasibleItemsStep3.get(i), modeRequiredData)){
        //         System.out.println("--------------------   删除结果出错   ------------------");
        //     }
        // }

        long endTime = System.currentTimeMillis() / 1000;
        System.out.println("最小不可行解时间： " + (endTime - startTime));
        return infeasibleItemsStep3;
    }

    // 获取当前解区域的放置点集合
    public static ArrayList<Integer> getAreaPlaceP(int[][] stepSolution) {
        ArrayList<Integer> var = new ArrayList<>();
        for (int[] arr : stepSolution) {
            if (!var.contains(arr[1])) {
                var.add(arr[1]);
            }
        }
        return var;
    }


    // 删除起始行的物品
    public static int[][] deleteLineArray(int line, int[][] solution) {

        List<int[]> listTemp1 = new ArrayList<>();
        // 每一个子区域的 离散点y
        for (int j = 0; j < solution.length; j++) {
            // 找到不在当前y上的所有目标块
            if (solution[j][1] != line) {
                listTemp1.add(solution[j]);
            }
        }

        // list转为数组
        int[][] tempSolution = new int[listTemp1.size()][3];
        for (int k = 0; k < listTemp1.size(); k++) {
            tempSolution[k] = listTemp1.get(k);
        }
        return tempSolution;

    }


    // 删除尾行的物品
    public static int[][] deleteLineArrayEnd(int line, int[][] solution) {
        List<int[]> listTemp1 = new ArrayList<>();
        // 每一个子区域的 离散点y
        for (int j = 0; j < solution.length; j++) {
            // 找到不在当前y上的所有目标块
            if (solution[j][2] != line) {
                listTemp1.add(solution[j]);
            }
        }

        // list转为数组
        int[][] tempSolution = new int[listTemp1.size()][3];
        for (int k = 0; k < listTemp1.size(); k++) {
            tempSolution[k] = listTemp1.get(k);
        }
        return tempSolution;

    }

    /**
     * @param divList index y1 y2
     * @description 处理两阶段之后的不可行解集，通过 liftcut 进一步增加不可行解
     * 处理的是每一个子集，并返回当前子集的 liftcut 结果
     * @author hao
     * @date 2023/7/10 16:16
     */
    public static int[][] liftCut(int[][] divList, int[][] step1Solutions, ModeRequiredData modeRequiredData) throws IloException, GRBException {
        GRBEnv env = new GRBEnv();
        // 设置日志级别为0（完全禁用）
        env.set(GRB.IntParam.OutputFlag, 0);
        GRBModel liftModel = new GRBModel(env);

        int[] oriSize = modeRequiredData.oriSize;
        int[][] defectSize = modeRequiredData.defectSize;
        // 定义模型变量 并 建立目标块的目标函数
        // upper down
        GRBVar[] upper = new GRBVar[step1Solutions.length];
        GRBVar[] down = new GRBVar[step1Solutions.length];
        GRBLinExpr obj = new GRBLinExpr();


        for (int j = 0; j < divList.length; j++) {
            int index = divList[j][0];
            int height = divList[j][2] - divList[j][1];
            upper[index] = liftModel.addVar(divList[j][1], oriSize[1] - height, 0, GRB.INTEGER, "upper[" + index + "]");
            down[index] = liftModel.addVar(0, divList[j][1], 0, GRB.INTEGER, "down[" + index + "]");
            obj.addTerm(1.0, upper[index]);
            obj.addTerm(-1.0, down[index]);
        }
        liftModel.setObjective(obj, GRB.MAXIMIZE);

        // 建立模型
        for (int i = 0; i < divList.length; i++) {
            int jIndex = divList[i][0];
            List<int[]> overlapBlock = overlapBlock(divList, i);
            for (int k = 0; k < overlapBlock.size(); k++) {
                int[] tempArray = overlapBlock.get(k);
                int kIndex = tempArray[0];
                GRBLinExpr expr1 = new GRBLinExpr();
                expr1.addTerm(1.0, upper[kIndex]);
                expr1.addTerm(-1.0, down[jIndex]);
                liftModel.addConstr(expr1, GRB.LESS_EQUAL, divList[i][2] - divList[i][1] - 1, "c1_" + kIndex + "_" + jIndex);
            }
        }

        for (int i = 0; i < defectSize.length; i++) {
            List<int[]> overlapDefBlock = overlapDefBlock(divList, defectSize[i]);
            for (int j = 0; j < overlapDefBlock.size(); j++) {
                int[] tempArray = overlapDefBlock.get(j);
                GRBLinExpr expr2 = new GRBLinExpr();
                expr2.addTerm(1.0, down[tempArray[0]]);
                expr2.addConstant(-(defectSize[i][1] - (tempArray[2] - tempArray[1]) + 1));
                liftModel.addConstr(expr2, GRB.GREATER_EQUAL, 0, "c2_" + tempArray[0] + "_" + i);

                GRBLinExpr expr3 = new GRBLinExpr();
                expr3.addTerm(1.0, upper[tempArray[0]]);
                expr3.addConstant(-(defectSize[i][3] - 1));
                liftModel.addConstr(expr3, GRB.LESS_EQUAL, 0, "c3_" + tempArray[0] + "_" + i);
            }
        }

        liftModel.optimize();

        int[][] result = new int[divList.length][3];
        System.out.println("liftModel.getStatus()" + liftModel.get(GRB.IntAttr.Status));
        if (liftModel.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
            for (int i = 0; i < divList.length; i++) {
                int index = divList[i][0];
                result[i][0] = index;
                result[i][1] = (int) Math.round(down[index].get(GRB.DoubleAttr.X));
                result[i][2] = (int) Math.round(upper[index].get(GRB.DoubleAttr.X));
            }
            liftModel.dispose();
            env.dispose();
            return result;
        }
        // 模型求解不出来最优解
        for (int i = 0; i < divList.length; i++) {
            result[i][0] = divList[i][0];
            result[i][1] = divList[i][1];
            result[i][2] = divList[i][1];
        }
        liftModel.dispose();
        env.dispose();
        return result;
    }

    public static List<int[]> overlapDefBlock(int[][] divList, int[] defSize) {
        List<int[]> tempList1 = new ArrayList<>();
        for (int i = 0; i < divList.length; i++) {
            if (defSize[3] - 1 >= divList[i][1] && divList[i][2] - 1 >= defSize[1]) {
                tempList1.add(divList[i]);
            }
        }
        return tempList1;
    }

    public static List<int[]> overlapBlock(int[][] tempArray1, int index) {
        List<int[]> tempList1 = new ArrayList<>();
        for (int i = 0; i < tempArray1.length; i++) {
            if (i != index) {
                if (tempArray1[index][2] > tempArray1[i][1] && tempArray1[i][2] > tempArray1[index][1]) {
                    tempList1.add(tempArray1[i]);
                }
            }
        }
        return tempList1;
    }

    public static int[][] deleteArray(int[][] array, int index) {

        int[][] tempArray = new int[array.length - 1][array[0].length];
        int times = 0;

        for (int i = 0; i < array.length; i++) {
            if (i != index) {
                for (int j = 0; j < array[i].length; j++) {
                    tempArray[times][j] = array[i][j];
                }
                ++times;
            }
        }
        return tempArray;
    }

    /**
     * @return int
     * @description 给每一组序列添加唯一的值与之对应
     * @author hao
     * @date 2023/7/9 6:37
     */
    private static int encode(int[][] arrayTemp) {
        int key = 0;
        TreeSet<Integer> set = new TreeSet<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1 - o2;
            }
        });

        for (int[] item1 : arrayTemp) {
            // 以索引确定唯一值
            set.add(item1[0]);
        }
        Iterator<Integer> iterator = set.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            key += iterator.next() * (int) (Math.pow(10, i));
            i++;
        }
        return key;
    }

    private static String encodeT(int[][] arrayTemp) {
        String key = "";
        // 按第一个元素：索引排序
        int[][] copyArraySort = arraySort(arrayTemp);
        // key的计算方法，索引+ "a" + y1
        for (int i = 0; i < copyArraySort.length; i++) {
            key += copyArraySort[i][0] + "a" + copyArraySort[i][1] + "a";
        }
        return key;
    }

    // 按索引排序
    public static int[][] arraySort(int[][] temp) {
        int[][] copyTwoDim = new int[0][];
        try {
            copyTwoDim = ToolClass.copyTwoDim(temp);
        } catch (Exception e) {
            System.out.println(4324);
        }
        for (int i = 0; i < copyTwoDim.length; i++) {
            int minIndex = i;

            for (int j = i + 1; j < copyTwoDim.length; j++) {
                if (copyTwoDim[j][0] < copyTwoDim[minIndex][0]) {
                    minIndex = j;
                }
            }

            if (minIndex != i) {
                int[] copyTwoDimRow = copyTwoDim[minIndex];
                copyTwoDim[minIndex] = copyTwoDim[i];
                copyTwoDim[i] = copyTwoDimRow;
            }
        }
        return copyTwoDim;
    }

    public static boolean isTimeExceeded(long startTime, double limit) {
        long elapsedSeconds = (System.currentTimeMillis() /1000 - startTime);
        return elapsedSeconds >= limit;
    }

    public static void main(String[] args) {
        int[][] step1Solutions = {{1, 0, 10}, {2, 0, 5}, {3, 0, 15}, {4, 10, 15}, {5, 15, 20}, {6, 20, 25}};
        for (int i = 0; i < step1Solutions.length; i++) {
            System.out.println(Arrays.toString(step1Solutions[i]));
        }
        System.out.println();

        // 寻找最小不可行解的第一步：将离散点集划分为几个部分
        // 记录分界线
        List<Integer> list = new ArrayList<>();
        // 以 yi 排序 （在原数组基础上排序）
        ToolClass.sortedTwoDim(step1Solutions, 1);
        int max = 0;
        for (int i = 0; i < step1Solutions.length - 1; i++) {
            max = Math.max(max, step1Solutions[i][2]);
            if (step1Solutions[i + 1][1] >= max) {
                // 说明找到分界线
                list.add(i);
            }
        }
        list.add(step1Solutions.length - 1);
        int index = 0;
        List<List<int[]>> divList = new ArrayList<>();
        List<int[]> listTemp = new ArrayList<>();
        for (int i = 0; i <= step1Solutions.length; i++) {
            if (i == step1Solutions.length) {
                divList.add(listTemp);
                break;
            }
            if (i > list.get(index)) {
                divList.add(listTemp);
                index++;
                listTemp = new ArrayList<>();
            }
            listTemp.add(step1Solutions[i]);
        }

        for (int i = 0; i < divList.size(); i++) {
            int[][] tempSolutions = new int[divList.get(i).size()][3];
            int startH = divList.get(i).get(0)[1];
            int endH = divList.get(i).get(0)[2];
            for (int j = 0; j < divList.get(i).size(); j++) {
                System.out.println(Arrays.toString(divList.get(i).get(j)));
                for (int k = 0; k < 3; k++) {
                    tempSolutions[j][k] = divList.get(i).get(j)[k];
                }
                startH = Math.min(startH, tempSolutions[j][1]);
                endH = Math.max(endH, tempSolutions[j][2]);
            }
            System.out.println();
        }

        System.out.println(1);
    }
}
