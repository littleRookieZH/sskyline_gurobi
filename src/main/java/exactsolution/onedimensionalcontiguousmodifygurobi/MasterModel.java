package main.java.exactsolution.onedimensionalcontiguousmodifygurobi;

import com.sun.org.apache.xpath.internal.SourceTree;
import gurobi.*;
import ilog.concert.*;
import ilog.cplex.IloCplex;
import main.java.com.commonfunction.CommonToolClass;
import main.java.com.pointset.ToolClass;
import main.java.com.twodimension.TargetData;
import main.java.com.twodimensiondata.SkyLineResultData;

import java.io.FileNotFoundException;
import java.util.*;


/**
 * @author hao
 * @description 主模型
 * @date 2023/7/5 19:34
 */
public class MasterModel {
    GRBEnv env;
    GRBModel model;
    /**
     * @description 定义R[i][y]表示小矩形i是否放置在离散点 y 处
     */
    GRBVar[][] R;
    ArrayList<Integer> integers = new ArrayList<>();
    ArrayList<Integer> integers1 = new ArrayList<>();

    public GRBModel rectangularModel(ModeRequiredData modeRequiredData, double timeout) throws IloException, RuntimeException, GRBException {

        // 创建环境和模型
        env = new GRBEnv();
        // 设置日志级别为0（完全禁用）
        env.set(GRB.IntParam.OutputFlag, 0);
        model = new GRBModel(env);

        // 设置时间限制和输出标志
        model.set(GRB.DoubleParam.TimeLimit, timeout);
        model.set(GRB.IntParam.OutputFlag, 0);

        // 获取原放置区域的宽度
        int Width = modeRequiredData.oriSize[0];
        // 获取下界
        int Height = modeRequiredData.oriSize[1];
        // 获得每种目标块的尺寸信息, w h
        int[][] blockSize = modeRequiredData.targetBlockSize;
        System.out.println(" --- ");
        for (int i = 0; i <blockSize.length; i++) {
            System.out.println("ARRAY = " + Arrays.toString(blockSize[i]));
        }
        System.out.println(" --- ");

        // 缺陷块每一行的长度
        int[] defectWidth = modeRequiredData.defectWidth;
        int[] heightPoints = modeRequiredData.heightPoints;
        // 缺陷块
        int[][] defectSize = modeRequiredData.defectSize;
        // 高度可放置点
        boolean[][] heightPlacedPoints = modeRequiredData.heightPlacedPoints;
        int minWidth = modeRequiredData.minWidth;
        int minHeight = modeRequiredData.minHeight;

        // 创建模型变量
        R = new GRBVar[blockSize.length][Height + 1];
        for (int j = 0; j < blockSize.length; j++) {
            for (int y = 0; y < heightPoints.length; y++) {
                int yPoint = heightPoints[y];
                if (heightPlacedPoints[j][yPoint]) {
                    // 将R[y][i]为0，1变量
                    R[j][yPoint] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "R[" + j + "][" + yPoint + "]");
                }
            }
        }

        // 约束一：所有的目标块必须能切割
        for (int i = 0; i < blockSize.length; i++) {
            GRBLinExpr expr1 = new GRBLinExpr();
            for (int k = 0; k < heightPoints.length; k++) {
                int yPoint = heightPoints[k];
                if (heightPlacedPoints[i][yPoint]) {
                    expr1.addTerm(1.0, R[i][yPoint]);
                }
            }
            model.addConstr(expr1, GRB.EQUAL, 1.0, "cutting_constraint_" + i);
        }

        // 约束二：覆盖第 q 行长条目标块总长度不超过该长条的可用长度
        for (int i = 0; i < heightPoints.length; i++) {
            GRBLinExpr expr2 = new GRBLinExpr();
            for (int j = 0; j < blockSize.length; j++) {
                int lowBound1 = Math.max(heightPoints[i] - blockSize[j][1] + 1, 0);
                int upperBound1 = Math.min(heightPoints[i], Height - blockSize[j][1]);
                for (int y = lowBound1; y <= upperBound1; y++) {
                    if (heightPlacedPoints[j][y]) {
                        expr2.addTerm(modeRequiredData.targetBlockSize[j][0], R[j][y]);
                    }
                }
            }
            if (defectWidth != null) {
                expr2.addConstant(defectWidth[i]);
            }
            model.addConstr(expr2, GRB.LESS_EQUAL, Width, "row_coverage_constraint_" + i);
        }

        // 约束三：缺陷块两侧宽度的约束
        GRBVar[][] leftVar = new GRBVar[defectSize.length][blockSize.length];
        GRBVar[][] rightVar = new GRBVar[defectSize.length][blockSize.length];
        GRBVar[][][] leftPBinVar = new GRBVar[defectSize.length][blockSize.length][2];
        GRBVar[][][] rightPBinVar = new GRBVar[defectSize.length][blockSize.length][2];

        for (int i = 0; i < defectSize.length; i++) {
            int leftSpace = defectSize[i][0];
            int rightSpace = Width - defectSize[i][2];
            for (int j = 0; j < blockSize.length; j++) {
                leftVar[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "leftVar[" + i + "][" + j + "]");
                rightVar[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "rightVar[" + i + "][" + j + "]");

                GRBLinExpr expr1 = new GRBLinExpr();
                for (int k = defectSize[i][1] - blockSize[j][1] + 1; k < defectSize[i][3]; k++) {
                    if (k >= 0 && k + blockSize[j][1] <= Height && heightPlacedPoints[j][k]) {
                        expr1.addTerm(1.0, R[j][k]);
                    }
                }

                if (leftSpace < blockSize[j][0]) {
                    model.addConstr(rightVar[i][j], GRB.GREATER_EQUAL, expr1, "right_constraint_" + i + "_" + j);
                } else if (rightSpace < blockSize[j][0]) {
                    model.addConstr(leftVar[i][j], GRB.GREATER_EQUAL, expr1, "left_constraint_" + i + "_" + j);
                }
                GRBLinExpr expr2 = new GRBLinExpr();
                expr2.addTerm(1.0, rightVar[i][j]);
                expr2.addTerm(1.0, leftVar[i][j]);
                model.addConstr(expr2, GRB.EQUAL, 1.0, "side_constraint_" + i + "_" + j);
            }

            int[] pPoints = {defectSize[i][1], defectSize[i][3] - 1};
            for (int p = 0; p < pPoints.length; p++) {
                GRBLinExpr expr2 = new GRBLinExpr();
                GRBLinExpr expr3 = new GRBLinExpr();
                for (int j = 0; j < blockSize.length; j++) {
                    leftPBinVar[i][j][p] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "leftPBinVar[" + i + "][" + j + "][" + p + "]");
                    rightPBinVar[i][j][p] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "rightPBinVar[" + i + "][" + j + "][" + p + "]");
                    // GRBVar leftPBinVar = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "leftPBinVar_" + i + "_" + j + "_" + p);
                    // GRBVar rightPBinVar = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "rightPBinVar_" + i + "_" + j + "_" + p);

                    GRBLinExpr expr4 = new GRBLinExpr();
                    GRBLinExpr expr5 = new GRBLinExpr();
                    for (int l = pPoints[p] - blockSize[j][1] + 1; l <= pPoints[p]; l++) {
                        if (blockSize[j][1] + l <= Height && l >= 0 && heightPlacedPoints[j][l]) {
                            expr4.addTerm(1.0, R[j][l]);
                            expr5.addTerm(1.0, R[j][l]);
                        }
                    }

                    model.addConstr(leftPBinVar[i][j][p], GRB.LESS_EQUAL, leftVar[i][j], "leftPBin_constraint_1_" + i + "_" + j + "_" + p);
                    model.addConstr(rightPBinVar[i][j][p], GRB.LESS_EQUAL, rightVar[i][j], "rightPBin_constraint_1_" + i + "_" + j + "_" + p);

                    expr4.addTerm(1.0, leftVar[i][j]);
                    expr4.addTerm(-1.0, leftPBinVar[i][j][p]);
                    model.addConstr(expr4, GRB.LESS_EQUAL, 1.0, "leftPBin_constraint_2_" + i + "_" + j + "_" + p);

                    expr5.addTerm(1.0, rightVar[i][j]);
                    expr5.addTerm(-1.0, rightPBinVar[i][j][p]);
                    model.addConstr(expr5, GRB.LESS_EQUAL, 1.0, "rightPBin_constraint_2_" + i + "_" + j + "_" + p);

                    if (blockSize[j][0] <= leftSpace) {
                        expr2.addTerm(blockSize[j][0], leftPBinVar[i][j][p]);
                    }
                    if (blockSize[j][0] <= rightSpace) {
                        expr3.addTerm(blockSize[j][0], rightPBinVar[i][j][p]);
                    }
                }
                model.addConstr(expr2, GRB.LESS_EQUAL, leftSpace, "leftSpace_constraint_" + i + "_" + p);
                model.addConstr(expr3, GRB.LESS_EQUAL, rightSpace, "rightSpace_constraint_" + i + "_" + p);
            }
        }


        // 添加额外约束
        for (int i = 0; i < modeRequiredData.exprList.size(); i++) {
            int[][] liftCut = modeRequiredData.exprList.get(i);
            // 向模型添加新的约束
            // IloLinearNumExpr expr = model.linearNumExpr();
            GRBLinExpr expr = new GRBLinExpr();
            for (int j = 0; j < liftCut.length; j++) {
                for (int k = liftCut[j][1]; k <= liftCut[j][2]; k++) {
                    if (modeRequiredData.heightPlacedPoints[liftCut[j][0]][k]) {
                        // expr.addTerm(R[liftCut[j][0]][k], 1);
                        expr.addTerm(1.0, R[liftCut[j][0]][k]);
                    }
                }
            }
            model.addConstr(expr, GRB.LESS_EQUAL, liftCut.length - 1, "c_expr" + i);
            // model.addLe(expr, liftCut.length - 1);
        }

        int total = 0;
        // 添加小于当前高度的所有 lift_cut 约束
        for (int i = 0; i < modeRequiredData.exprListPrev.size(); i++) {
            GRBLinExpr expr = new GRBLinExpr();
            int[][] liftCut = modeRequiredData.exprListPrev.get(i);
            // modeRequiredData.heightPlacedPoints[blockIndex][temp1]

            // System.out.println(" ----------- ");
            // for (int j = 0; j < liftCut.length; j++) {
            //     System.out.println(Arrays.toString(liftCut[j]));
            // }
            // System.out.println(" ----------- ");

            // for (int j = 0; j < liftCut.length; j++) {
            //     // System.out.print("j = " + liftCut[j][0] + "\t");
            //     try {
            //         for (int k = 0; k < modeRequiredData.heightPlacedPoints[liftCut[j][0]].length; k++) {
            //             if(modeRequiredData.heightPlacedPoints[liftCut[j][0]][k]){
            //                 System.out.print(k + "\t");
            //             }
            //         }
            //     } catch (Exception e) {
            //         throw new RuntimeException(e);
            //     }
            //     System.out.println();
            // }
            // System.out.println(" -------- ");

            // 标记当前
            boolean isValidAll = true;
            for (int j = 0; j < liftCut.length; j++) {
                boolean isNull = true;
                for (int k = liftCut[j][1]; k <= liftCut[j][2]; ++k) {
                    if (k <= Height && modeRequiredData.heightPlacedPoints[liftCut[j][0]][k]) {
                        isNull = false;
                        break;
                    }
                }
                if (isNull) {// blockIndex 无效
                    isValidAll = false;// 无效
                    System.out.println(" 无效  modeRequiredData.exprListPrev.get(i) = " + i);
                    break;
                }
            }

            if (isValidAll) {
                ++total;
                for (int j = 0; j < liftCut.length; j++) {
                    for (int k = liftCut[j][1]; k <= liftCut[j][2]; ++k) {
                        if (k <= Height && modeRequiredData.heightPlacedPoints[liftCut[j][0]][k]) {
                            // System.out.println("额外添加的 lift-cut： i = " + liftCut[j][0] + "  y = " + k);
                            expr.addTerm(1.0, R[liftCut[j][0]][k]);
                        }
                    }
                }
                // System.out.println(" ----------- ");
                model.addConstr(expr, GRB.LESS_EQUAL, liftCut.length - 1, "c_prevexpr" + i);
            }
        }


        System.out.println("modeRequiredData.exprListPrev.size() = " + modeRequiredData.exprListPrev.size());
        System.out.println("valid exprListPrev.size() = " + total);
        model.write("model.lp");// 输出模型文件
        return model;
    }

    // 确定解集中有多少种物品，以及每种物品对应的个数
    public HashMap<Integer, Integer> determineItemsType(int[][] solution, int[][] targetBlockSize) {
        HashMap<Integer, Integer> determineType = new HashMap<>();
        for (int i = 0; i < solution.length; i++) {
            int index = solution[i][0];
            int key = 0;
            key = targetBlockSize[index][3];
            if (determineType.containsKey(key)) {
                int times = determineType.get(key);
                determineType.put(key, ++times);
            } else {
                determineType.put(key, 1);
            }
        }
        System.out.println(9879);
        return determineType;
    }

    // 确定不同种类的取值范围，不选择索引作为结果值，选择具体值作为返回结果
    public HashMap<Integer, ArrayList<Integer>> determineItemsRange(int[][] solution, int[][] targetBlockSize) {
        HashMap<Integer, ArrayList<Integer>> determineRange = new HashMap<>();
        HashMap<Integer, ArrayList<Integer>> range = new HashMap<>();
        for (int i = 0; i < solution.length; i++) {
            // 解集的索引
            int index = solution[i][0];
            // 物品的key
            int key = targetBlockSize[index][3];
            if (determineRange.containsKey(key)) {
                ArrayList<Integer> listIndex = determineRange.get(key);
                listIndex.add(i);
            } else {
                ArrayList<Integer> listIndex = new ArrayList<>();
                listIndex.add(i);
                determineRange.put(key, listIndex);
            }
        }
        // 确定范围
        for (Map.Entry<Integer, ArrayList<Integer>> entry : determineRange.entrySet()) {
            // key指当前种类，value指解的索引值
            Integer key = entry.getKey();
            ArrayList<Integer> indexValues = entry.getValue();
            // 每个种类有一个，防止加入重复值
            HashSet<Integer> hashSet1 = new HashSet<>();
            range.put(key, new ArrayList<>());
            // 每一个解的索引
            for (int i = 0; i < indexValues.size(); i++) {
                int index = indexValues.get(i);
                // liftcut的y1、y2
                for (int j = solution[index][1]; j <= solution[index][2]; j++) {
                    if (hashSet1.add(j)) {
                        ArrayList<Integer> list1 = range.get(key);
                        list1.add(j);
                    }
                }
            }
        }

        return range;
    }


    public ModeRequiredData solveModel(SkyLineResultData exactAndHeuristic, long startTime, long endTime) throws IloException, FileNotFoundException, RuntimeException, GRBException {
        List<ModifyModeRequiredData> list = exactAndHeuristic.listModeRequiredData;
        // 用于计算的数据 -- 可能会与 ori不同 -- 旋转了
        TargetData rLayout;

        ModeRequiredData modeRequiredData;

        double xTimes = 0;
        double yTimes = 0;
//        double cutTimes = 0;
        double misFTimes = 0;
        double combinesFTimes = 0;

        // 最初的没有反转的
        ModifyModeRequiredData oriData = list.get(0);
        // 反转的
        ModifyModeRequiredData modeRequiredData1 = list.get(1);
//            modeRequiredData = oriData.modeRequiredData;
//            rLayout = oriData.rLayout;
//            System.out.println("主模型：高度方向，" + rLayout.oriSize[1] + " 子模型：水平方向，" + rLayout.oriSize[0] + " 数量：" + rLayout.targetNumber);
//            System.out.println("主模型：离散点集，" + modeRequiredData.heightPoints.length + " 子模型：离散点集，" + modeRequiredData.widthPoints.length + " 数量：");
//            System.out.println(222);


        // int cosntValue = 100;
        // && (oriData.modeRequiredData.widthPoints.length <= cosntValue)
        if ((oriData.modeRequiredData.heightPoints.length >= oriData.modeRequiredData.widthPoints.length)) {
            // 用于提升高度
            while (true) {
                // 翻转
                oriData = list.get(0);
                modeRequiredData1 = list.get(1);
                modeRequiredData = modeRequiredData1.modeRequiredData;
                rLayout = modeRequiredData1.rLayout;
                modeRequiredData.isRotation = true;
                double time1 = System.currentTimeMillis() / 1000.0;
                // 加载1CBP模型
                model = rectangularModel(modeRequiredData, modeRequiredData.LIFTLB_TIMELIMIT);
                // 优化模型并求解
                model.optimize();
                System.out.println("GRB.IntAttr.Status = " + ToolClass.getStatusString(model.get(GRB.IntAttr.Status)));
                System.out.println("主模型：高度为：" + rLayout.oriSize[1] + " 子模型：水平为：" + rLayout.oriSize[0] + " 数量：" + rLayout.targetNumber);
                System.out.println("主模型：离散点集，" + modeRequiredData.heightPoints.length + " 子模型：离散点集，" + modeRequiredData.widthPoints.length + " 数量：");
                if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
                    break;
                } else {
                    double time2 = System.currentTimeMillis() / 1000.0;
                    System.out.println("主模型 求解时间为 ： " + (time2 - time1));
                    // 提升下界超时
                    if (model.get(GRB.IntAttr.Status) == GRB.Status.TIME_LIMIT) {
                        System.out.println("--- 我超时了 ---");
                        break;
                    }

                    // 从下往上，逐步缩减高度
                    if (MasterModel.upDateBottomToTop(oriData.modeRequiredData, model.get(GRB.IntAttr.Status), exactAndHeuristic)) {
                        model.dispose();
                        env.dispose();
                        return oriData.modeRequiredData;
                    } else {
                        list = oriData.modeRequiredData.improveModel1(oriData.modeRequiredData, oriData.rLayout, modeRequiredData);
                        continue;
                    }

                }
            }
        } else {
            // 用于提升高度
            while (true) {

                oriData = list.get(0);
                modeRequiredData1 = list.get(1);
                modeRequiredData = oriData.modeRequiredData;
                rLayout = oriData.rLayout;

                double time1 = System.currentTimeMillis() / 1000.0;
                // 加载1CBP模型
                model = rectangularModel(modeRequiredData, modeRequiredData.LIFTLB_TIMELIMIT);
                // 优化模型并求解
                model.optimize();
                System.out.println("主模型：高度为：" + rLayout.oriSize[1] + " 子模型：水平为：" + rLayout.oriSize[0] + " 数量：" + rLayout.targetNumber);
                System.out.println("主模型：离散点集，" + modeRequiredData.heightPoints.length + " 子模型：离散点集，" + modeRequiredData.widthPoints.length + " 数量：");
                if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
                    break;
                } else {
                    double time2 = System.currentTimeMillis() / 1000.0;
                    System.out.println("主模型 求解时间为 ： " + (time2 - time1));

                    // 提升下界超时
                    if (model.get(GRB.IntAttr.Status) == GRB.Status.TIME_LIMIT) {
                        break;
                    }

                    // 从下往上，逐步缩减高度
                    if (MasterModel.upDateBottomToTop(oriData.modeRequiredData, model.get(GRB.IntAttr.Status), exactAndHeuristic)) {
                        model.dispose();
                        env.dispose();
                        return oriData.modeRequiredData;
                    } else {
                        list = oriData.modeRequiredData.improveModel1(oriData.modeRequiredData, oriData.rLayout, modeRequiredData);
                        continue;
                    }
                }
            }
        }


        // System.out.println("主模型：高度方向，" + rLayout.oriSize[1] + " 子模型：水平方向，" + rLayout.oriSize[0] + " 数量：" + rLayout.targetNumber);
        // System.out.println("主模型：离散点集，" + modeRequiredData.heightPoints.length + " 子模型：离散点集，" + modeRequiredData.widthPoints.length + " 数量：");

        // /* 认为是1CBP的点集越多越好 */
        // if (oriData.modeRequiredData.heightPoints.length < oriData.modeRequiredData.widthPoints.length) {
        //     modeRequiredData = oriData.modeRequiredData;
        //     rLayout = oriData.rLayout;
        //     System.out.println("主模型：高度方向，" + rLayout.oriSize[1] + " 子模型：水平方向，" + rLayout.oriSize[0] + " 数量：" + rLayout.targetNumber);
        //     System.out.println("主模型：离散点集，" + modeRequiredData.heightPoints.length + " 子模型：离散点集，" + modeRequiredData.widthPoints.length + " 数量：");
        // } else if ((oriData.modeRequiredData.heightPoints.length == oriData.modeRequiredData.widthPoints.length) && oriData.modeRequiredData.oriSize[1] < oriData.modeRequiredData.oriSize[0]) {
        //     modeRequiredData = oriData.modeRequiredData;
        //     rLayout = oriData.rLayout;
        //     System.out.println("主模型：高度方向，" + rLayout.oriSize[1] + " 子模型：水平方向，" + rLayout.oriSize[0] + " 数量：" + rLayout.targetNumber);
        //     System.out.println("主模型：离散点集，" + modeRequiredData.heightPoints.length + " 子模型：离散点集，" + modeRequiredData.widthPoints.length + " 数量：");
        //
        // } else {
        //     modeRequiredData = modeRequiredData1.modeRequiredData;
        //     rLayout = modeRequiredData1.rLayout;
        //     System.out.println("主模型：高度方向，" + rLayout.oriSize[1] + " 子模型：水平方向，" + rLayout.oriSize[0] + " 数量：" + rLayout.targetNumber);
        //     System.out.println("主模型：离散点集，" + modeRequiredData.heightPoints.length + " 子模型：离散点集，" + modeRequiredData.widthPoints.length + " 数量：");
        // }

        // 有用的
        // 二分法 还需要重新计算测试高度
        // MasterModel.upDateHeightDichotomy(oriData.modeRequiredData, -1, exactAndHeuristic);
        // list = oriData.modeRequiredData.improveModel1(oriData.modeRequiredData, oriData.rLayout, modeRequiredData);

        boolean const1 = oriData.rLayout.oriSize[0] <= 20 || oriData.rLayout.oriSize[1] <= 20|| (oriData.rLayout.oriSize[0] < 40 && oriData.rLayout.oriSize[1] < 40);

        // 不翻转
        boolean condition1 = oriData.modeRequiredData.heightPoints.length >= oriData.modeRequiredData.widthPoints.length && const1;

        // condition1 = false;
        // 翻转
        boolean condition2 = oriData.modeRequiredData.heightPoints.length >= oriData.modeRequiredData.widthPoints.length && !const1;

        // condition2 = true ;
        // 翻转
        boolean condition3 = oriData.modeRequiredData.heightPoints.length < oriData.modeRequiredData.widthPoints.length && const1;

        // 不翻转
        boolean condition4 = oriData.modeRequiredData.heightPoints.length < oriData.modeRequiredData.widthPoints.length  && !const1;

        while (true) {
//             // 最初的没有反转的
//             ModifyModeRequiredData oriData = list.get(0);
//             // 反转的
//             ModifyModeRequiredData modeRequiredData1 = list.get(1);
// //            modeRequiredData = oriData.modeRequiredData;
// //            rLayout = oriData.rLayout;
// //            System.out.println("主模型：高度方向，" + rLayout.oriSize[1] + " 子模型：水平方向，" + rLayout.oriSize[0] + " 数量：" + rLayout.targetNumber);
// //            System.out.println("主模型：离散点集，" + modeRequiredData.heightPoints.length + " 子模型：离散点集，" + modeRequiredData.widthPoints.length + " 数量：");
// //            System.out.println(222);
//

            // 最初的没有反转的
            oriData = list.get(0);
            // 反转的
            modeRequiredData1 = list.get(1);

            if (condition1 ) {
                modeRequiredData = oriData.modeRequiredData;
                rLayout = oriData.rLayout;
                System.out.println("主模型：高度方向，" + rLayout.oriSize[1] + " 子模型：水平方向，" + rLayout.oriSize[0] + " 数量：" + rLayout.targetNumber);
                System.out.println("主模型：离散点集，" + modeRequiredData.heightPoints.length + " 子模型：离散点集，" + modeRequiredData.widthPoints.length + " 数量：");
            } else if (condition2) {
                modeRequiredData = modeRequiredData1.modeRequiredData;
                rLayout = modeRequiredData1.rLayout;
                modeRequiredData.isRotation = true;
                System.out.println("主模型：高度方向，" + rLayout.oriSize[1] + " 子模型：水平方向，" + rLayout.oriSize[0] + " 数量：" + rLayout.targetNumber);
                System.out.println("主模型：离散点集，" + modeRequiredData.heightPoints.length + " 子模型：离散点集，" + modeRequiredData.widthPoints.length + " 数量：");
            } else if (condition3) {
                modeRequiredData = modeRequiredData1.modeRequiredData;
                rLayout = modeRequiredData1.rLayout;
                modeRequiredData.isRotation = true;
                System.out.println("主模型：高度方向，" + rLayout.oriSize[1] + " 子模型：水平方向，" + rLayout.oriSize[0] + " 数量：" + rLayout.targetNumber);
                System.out.println("主模型：离散点集，" + modeRequiredData.heightPoints.length + " 子模型：离散点集，" + modeRequiredData.widthPoints.length + " 数量：");
            } else {
                modeRequiredData = oriData.modeRequiredData;
                rLayout = oriData.rLayout;
                System.out.println("主模型：高度方向，" + rLayout.oriSize[1] + " 子模型：水平方向，" + rLayout.oriSize[0] + " 数量：" + rLayout.targetNumber);
                System.out.println("主模型：离散点集，" + modeRequiredData.heightPoints.length + " 子模型：离散点集，" + modeRequiredData.widthPoints.length + " 数量：");
            }

            int blockNum = modeRequiredData.targetBlockSize.length;
            int[][] step1Solutions = new int[blockNum][3];

            if (isTimeExceeded(startTime, endTime)) {
                modeRequiredData.check = null;
                // 超时
                oriData.modeRequiredData.resultTFTimeOut = true;

                // 从上往下
                // upDateTopToBottom(oriData.modeRequiredData, GRB.Status.TIME_LIMIT, exactAndHeuristic);
                // 从下往上
                upDateBottomToTop(oriData.modeRequiredData, GRB.Status.TIME_LIMIT, exactAndHeuristic);
                // 二分法
                // upDateHeightDichotomy(oriData.modeRequiredData, GRB.Status.TIME_LIMIT, exactAndHeuristic);
                return oriData.modeRequiredData;
            }


            double uploadTimeS = System.currentTimeMillis() / 1000;
            // 加载1CBP模型
            model = rectangularModel(modeRequiredData, modeRequiredData.CBP_TIMELIMIT);
            double uploadTimeE = System.currentTimeMillis() / 1000;
            System.out.println(" modeRequiredData.LB = " + modeRequiredData.LB);
            if (isTimeExceeded(startTime, endTime)) {
                modeRequiredData.check = null;
                System.out.println("model time_out ");
                // 超时
                oriData.modeRequiredData.resultTFTimeOut = true;

                // 从上往下
                // upDateTopToBottom(oriData.modeRequiredData, GRB.Status.TIME_LIMIT, exactAndHeuristic);
                upDateBottomToTop(oriData.modeRequiredData, GRB.Status.TIME_LIMIT, exactAndHeuristic);
                // upDateHeightDichotomy(oriData.modeRequiredData, GRB.Status.TIME_LIMIT, exactAndHeuristic);
                return oriData.modeRequiredData;
            }

            int[] heightPoints = modeRequiredData.heightPoints;
            long startM = System.currentTimeMillis() / 1000;
            long endM;
            long startS;
            long endS;

            // 优化模型并求解
            model.optimize();
            // 1CBP
            if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
                endM = System.currentTimeMillis() / 1000;
                xTimes += endM - startM;
                System.out.println("主模型：" + (endM - startM));
                // System.out.println("限制时间  解的状态：  " + model.getStatus());
                // 记录矩形的位置信息，i yi yi+hi
                // 求得 y 的坐标
                int y = 0;
                integers = new ArrayList<>();
                integers1 = new ArrayList<>();

                for (int i = 0; i < modeRequiredData.targetBlockSize.length; i++) {
                    integers.add(i);
                    boolean exist = true;
                    for (int j = 0; j < heightPoints.length; j++) {
                        y = heightPoints[j];
                        if (modeRequiredData.heightPlacedPoints[i][y]) {
                            double value = R[i][y].get(GRB.DoubleAttr.X);
                            exist = false;
                            if (value > 0.5) {
                                integers1.add(i);
                                step1Solutions[i][0] = i;
                                step1Solutions[i][1] = (int) (y + 0.000001);
                                step1Solutions[i][2] = step1Solutions[i][1] + modeRequiredData.blockSize[i][1];
//                              System.out.println("step1Solutions[i][0] " + step1Solutions[i][0] + " step1Solutions[i][1] " + step1Solutions[i][1] + " step1Solutions[i][2] " + step1Solutions[i][2]);
                            }
                        }
                    }
                    if (exist) {
                        System.out.println("有的点没有离散点---" + i);
                    }
                }

                startS = System.currentTimeMillis() / 1000;
                // X-check
                if (CheckCplex.xCheck(step1Solutions, modeRequiredData)) {
                    endS = System.currentTimeMillis() / 1000;
                    yTimes += endS - startS;
                    System.out.println("子模型时间：" + (endS - startS));

                    if (isTimeExceeded(startS, (long)ModeRequiredData.CBP_TIMELIMIT)) {
                        modeRequiredData.check = null;
                        System.out.println("check = null 3");
                        // 超时
                        oriData.modeRequiredData.resultTFTimeOut = true;

                        // 从上往下
                        // upDateTopToBottom(oriData.modeRequiredData, GRB.Status.TIME_LIMIT, exactAndHeuristic);
                        upDateBottomToTop(oriData.modeRequiredData, GRB.Status.TIME_LIMIT, exactAndHeuristic);
                        // upDateHeightDichotomy(oriData.modeRequiredData, GRB.Status.TIME_LIMIT, exactAndHeuristic);
                        return oriData.modeRequiredData;
                    }
                    int[] widthPoints = modeRequiredData.widthPoints;
                    System.out.println("----------------------------------");
                    // 输出X信息
                    int[][] xCoord = new int[blockNum][3];
                    for (int i = 0; i < blockNum; i++) {
                        int indexI = step1Solutions[i][0];
                        double value = modeRequiredData.check.checkModel.getVarByName("x[" + i + "]").get(GRB.DoubleAttr.X);
                        System.out.println("num i = " + indexI + "xpoint = " + modeRequiredData.check.x[i].get(GRB.DoubleAttr.X) + "y point = " + step1Solutions[i][1]);
                        xCoord[i][0] = i;
                        xCoord[i][1] = (int) (value + 0.000001);
                        xCoord[i][2] = xCoord[i][1] + modeRequiredData.blockSize[indexI][0];
                    }
                    modeRequiredData.resultPoints = new int[blockNum][7];
                    // 合并得到最终结果
                    for (int i = 0; i < blockNum; i++) {
                        modeRequiredData.resultPoints[i][0] = i;
                        modeRequiredData.resultPoints[i][1] = xCoord[i][2] - xCoord[i][1];
                        modeRequiredData.resultPoints[i][2] = step1Solutions[i][2] - step1Solutions[i][1];
                        modeRequiredData.resultPoints[i][3] = xCoord[i][1];
                        modeRequiredData.resultPoints[i][4] = step1Solutions[i][1];
                        modeRequiredData.resultPoints[i][5] = xCoord[i][2];
                        modeRequiredData.resultPoints[i][6] = step1Solutions[i][2];
                        System.out.println(i);
                    }


                    // X可解；直接输出
                    System.out.println("可以求得最终解~~~   =  " + oriData.modeRequiredData.oriSize[1]);
                    System.out.println("主模型总时间：" + xTimes + " 子模型总时间：" + yTimes);
                    System.out.println("计算不可行解总时间：" + misFTimes + " combineCut总时间：" + combinesFTimes);

                    // 需要保留
                    if (oriData.modeRequiredData.oriSize[0] != modeRequiredData.oriSize[0]) {// 旋转了
                        CommonToolClass commonToolClass = new CommonToolClass();
                        commonToolClass.changeLocationExact(modeRequiredData.resultPoints, modeRequiredData.oriSize[1]);
                    }

                    // 需要保留
                    oriData.modeRequiredData.resultPoints = modeRequiredData.resultPoints;



                    System.out.println("modeRequiredData.oriSize[1] = " + modeRequiredData.oriSize[1] + "------------------");
                    // // 二分法缩减高度
                    // if (MasterModel.upDateHeightDichotomy(oriData.modeRequiredData, GRB.Status.OPTIMAL, exactAndHeuristic)) {
                    //     model.dispose();
                    //     env.dispose();
                    //     return oriData.modeRequiredData;
                    // } else {
                    //     list = oriData.modeRequiredData.improveModel1(oriData.modeRequiredData, oriData.rLayout, modeRequiredData);
                    //     continue;
                    // }

                    // 从下往上，逐步缩减高度
                    if (MasterModel.upDateBottomToTop(oriData.modeRequiredData, GRB.Status.OPTIMAL, exactAndHeuristic)) {
                        return oriData.modeRequiredData;
                    } else {
                        list = oriData.modeRequiredData.improveModel1(oriData.modeRequiredData, oriData.rLayout, modeRequiredData);
                        continue;
                    }


                    // 从上往下，逐步缩减高度
                    // if (MasterModel.upDateTopToBottom(oriData.modeRequiredData, GRB.Status.OPTIMAL, exactAndHeuristic)) {
                    //     model.dispose();
                    //     env.dispose();
                    //     return oriData.modeRequiredData;
                    // } else {
                    //     list = oriData.modeRequiredData.improveModel1(oriData.modeRequiredData, oriData.rLayout, modeRequiredData);
                    //     continue;
                    // }

                } else {
                    endS = System.currentTimeMillis() / 1000;
                    System.out.println("子模型时间：" + (endS - startS));

                    if (isTimeExceeded(startS, (long)ModeRequiredData.CBP_TIMELIMIT)) {
                        modeRequiredData.check = null;
                        System.out.println("check = null 3");
                        // 超时
                        oriData.modeRequiredData.resultTFTimeOut = true;

                        // 从上往下
                        // upDateTopToBottom(oriData.modeRequiredData, GRB.Status.TIME_LIMIT, exactAndHeuristic);
                        upDateBottomToTop(oriData.modeRequiredData, GRB.Status.TIME_LIMIT, exactAndHeuristic);
                        // upDateHeightDichotomy(oriData.modeRequiredData, GRB.Status.TIME_LIMIT, exactAndHeuristic);
                        return oriData.modeRequiredData;
                    }



                    double timesCut1 = System.currentTimeMillis() / 1000;

                    // 计算最小不可行解 添加了时间限制 (30s)
                    List<int[][]> divideSolution = MinInfeasible.divideSolution(step1Solutions, modeRequiredData);
                    double timesCut2 = System.currentTimeMillis() / 1000;

                    if(modeRequiredData.resultTFTimeOut){
                        modeRequiredData.check = null;
                        // 超时
                        oriData.modeRequiredData.resultTFTimeOut = true;

                        // 从上往下
                        // upDateTopToBottom(oriData.modeRequiredData, GRB.Status.TIME_LIMIT, exactAndHeuristic);

                        // 从下往上
                        upDateBottomToTop(oriData.modeRequiredData, GRB.Status.TIME_LIMIT, exactAndHeuristic);

                        // 二分法
                        // upDateHeightDichotomy(oriData.modeRequiredData, GRB.Status.TIME_LIMIT, exactAndHeuristic);
                        return oriData.modeRequiredData;
                    }


                    if ((timesCut2 - timesCut1) > 0) {
                        System.out.println("计算最小不可行解时间：" + (timesCut2 - timesCut1));
                    }
                    if ((timesCut2 - timesCut1) < 0) {
                        System.out.println(32);
                    }
                    misFTimes += (timesCut2 - timesCut1);

                    // 求解太慢
                    for (int i = 0; i < divideSolution.size(); i++) {
                        if (divideSolution.get(i).length == 0) {
                            continue;
                        }

                        System.out.println("modeRequiredData.oriSize[1]   " + modeRequiredData.oriSize[1]);

                        System.out.println("---------------divideSolution" + i + "-----------");
                        for (int[] arr3 : divideSolution.get(i)) {
                            // arr3[2] = arr3[1];
                            System.out.println(Arrays.toString(arr3));
                        }

                        // 改善最小不可行解的质量 添加了时间限制，超过时间 (200s)，返回 原不可行解
                        int[][] liftCut = MinInfeasible.liftCut(divideSolution.get(i), step1Solutions, modeRequiredData);
                        // int[][] liftCut = divideSolution.get(i);

                        if (isTimeExceeded(startTime, endTime)) {
                            modeRequiredData.check = null;
                            // 超时
                            oriData.modeRequiredData.resultTFTimeOut = true;

                            // 从上往下
                            // upDateTopToBottom(oriData.modeRequiredData, GRB.Status.TIME_LIMIT, exactAndHeuristic);

                            // 从下往上
                            upDateBottomToTop(oriData.modeRequiredData, GRB.Status.TIME_LIMIT, exactAndHeuristic);

                            // 二分法
                            // upDateHeightDichotomy(oriData.modeRequiredData, GRB.Status.TIME_LIMIT, exactAndHeuristic);
                            return oriData.modeRequiredData;
                        }
                        if (liftCut.length != 0) {
                            modeRequiredData.liftCutPrev.add(new ConserveSolution(divideSolution.get(i), liftCut));
                            modeRequiredData.exprList.add(liftCut);
                        }
                    }
                    System.out.println("modeRequiredData.filterHashSet.size()   " + modeRequiredData.filterHashSet.size());
                    System.out.println("modeRequiredData.hashSetMinInfeases.size()   " + modeRequiredData.hashSetMinInfeases.size());
                    System.out.println("modeRequiredData.hashSetAreaInfeases.size()   " + modeRequiredData.hashSetAreaInfeases.size());
                    System.out.println("modeRequiredData.exprList.size()  " + modeRequiredData.exprList.size());
                    double timesCut3 = System.currentTimeMillis() / 1000;
                    System.out.println("组合combines cut时间：" + (timesCut3 - timesCut2));
                    combinesFTimes += timesCut3 - timesCut2;
                    System.out.println("当前 X-check 模型不可解");
                    // X-check不通过时，需要不断添加约束，直到 Y不可行。说明当前高度确实不可行，则增加高度
                    continue;
                }
            }

            endM = System.currentTimeMillis() / 1000;
            xTimes += endM - startM;
            System.out.println("主模型：" + (endM - startM));
            System.out.println("限制时间  解的状态：  " + model.get(GRB.IntAttr.Status));

            if(model.get(GRB.IntAttr.Status) == GRB.Status.TIME_LIMIT){
                oriData.modeRequiredData.resultTFTimeOut = true;
            }


            // // 求解失败
            // ++oriData.modeRequiredData.oriSize[1];
            // // 判断是否是上下界相等
            // if (oriData.modeRequiredData.oriSize[1] >= exactAndHeuristic.skyLine.skyHeight) {
            //     System.out.println("上界：" + exactAndHeuristic.skyLine.skyHeight);
            //     System.out.println("下界：" + oriData.modeRequiredData.oriSize[1]);
            //     exactAndHeuristic.isHeuristic = true;
            //     modeRequiredData.resultTF = true;
            //     model.dispose();
            //     env.dispose();
            //     return modeRequiredData;
            // }

            // // 二分法缩减高度
            // if (MasterModel.upDateHeightDichotomy(oriData.modeRequiredData, model.get(GRB.IntAttr.Status), exactAndHeuristic)) {
            //     model.dispose();
            //     env.dispose();
            //     return oriData.modeRequiredData;
            // }

            // 从下往上，逐步缩减高度
            if (MasterModel.upDateBottomToTop(oriData.modeRequiredData, model.get(GRB.IntAttr.Status), exactAndHeuristic)) {
                System.out.println("final model.getStatus() = " + ToolClass.getStatusString(model.get(GRB.IntAttr.Status)));
                model.dispose();
                env.dispose();
                return oriData.modeRequiredData;
            }


            // 从上往下，逐步缩减高度
            // if (MasterModel.upDateTopToBottom(oriData.modeRequiredData, model.get(GRB.IntAttr.Status), exactAndHeuristic)) {
            // System.out.println("final model.getStatus() = " + ToolClass.getStatusString(model.get(GRB.IntAttr.Status)));
            //     model.dispose();
            //     env.dispose();
            //     return oriData.modeRequiredData;
            // }

            list = oriData.modeRequiredData.improveModel1(oriData.modeRequiredData, oriData.rLayout, modeRequiredData);
            System.out.println("modeRequiredData.LB :    " + oriData.modeRequiredData.oriSize[1]);
        }
    }

    public static boolean isTimeExceeded(long startTime, long endTime) {
        long elapsedSeconds = (System.currentTimeMillis() / 1000 - startTime);
        return elapsedSeconds >= endTime;
    }

    /**
     * 忘记更新 oriSize[1]了
     *
     * @param modeRequiredData  oriData.modeRequiredData
     * @param status            gurobi状态
     * @param exactAndHeuristic 精确算法结果
     * @return true: 算法结束，无需继续迭代；false: 继续迭代
     */
    public static boolean upDateHeightDichotomy(ModeRequiredData modeRequiredData, int status, SkyLineResultData exactAndHeuristic) {
        // 更新高度
        int nowHeight = modeRequiredData.oriSize[1];
        if (status == GRB.Status.OPTIMAL) { // x-check
            modeRequiredData.UB = nowHeight;
            modeRequiredData.resultTF = true;
        } else if (status == -1) {// 初始化高度，此时还没进行二分
            modeRequiredData.LB = nowHeight;
        } else { // 1cbp 不可行
            modeRequiredData.heightResult[modeRequiredData.LB] = nowHeight;// 标记高度已经验证为不可行
            modeRequiredData.LB = nowHeight + 1;// 当前高度还未验证
        }

        if (status == GRB.Status.TIME_LIMIT) { // 超时
            exactAndHeuristic.isHeuristic = !modeRequiredData.resultTF;
            return true;
        }

        int tmpHeight = modeRequiredData.LB + (modeRequiredData.UB - modeRequiredData.LB + 1) / 2;

        System.out.println(" modeRequiredData.LB  = " + modeRequiredData.LB + " modeRequiredData.UB = " + modeRequiredData.UB);
        // 如果h_t == h_u ---》 h_u = h_l + 1同时 h_l不确定是不是最优解
        if ((tmpHeight == modeRequiredData.UB) && (modeRequiredData.heightResult[modeRequiredData.LB] == -1)) {
            System.out.println("999999");
            tmpHeight = modeRequiredData.LB;
        }
        modeRequiredData.oriSize[1] = tmpHeight;
        // 终止条件
        if (tmpHeight == modeRequiredData.UB) {
            System.out.println("88888");
            // 有结果
            modeRequiredData.resultTF = true;
            if (tmpHeight == exactAndHeuristic.skyLine.skyHeight) {
                // 结果是启发式计算出来的
                exactAndHeuristic.isHeuristic = true;
            } else {
                exactAndHeuristic.isHeuristic = false;
            }
            return true;
        }
        return false;
    }

    /**
     * @param modeRequiredData  oriData.modeRequiredData
     * @param status            gurobi状态   x-check 传入optimal  1cbp 传入 !optimal
     * @param exactAndHeuristic 精确算法结果
     * @return true: 算法结束，无需继续迭代；false: 继续迭代
     */
    public static boolean upDateBottomToTop(ModeRequiredData modeRequiredData, int status, SkyLineResultData exactAndHeuristic) {
        if (status == GRB.Status.OPTIMAL) {// x-check有解  -- 终止求解
            modeRequiredData.resultTF = true;
            exactAndHeuristic.isHeuristic = false;
            return true;
        } else {// 1CBP无解  -- 还有可能是超时 - 终止求解 -- 也可能是上下界相同
            if (status != GRB.Status.TIME_LIMIT) {
                ++modeRequiredData.oriSize[1];
                modeRequiredData.LB = modeRequiredData.oriSize[1];
            }

            if (modeRequiredData.LB == modeRequiredData.UB) { // 提升高度至上界位置
                modeRequiredData.resultTF = true;
                exactAndHeuristic.isHeuristic = true;
                return true;
            }
            if (status == GRB.Status.TIME_LIMIT) {// 超时
                modeRequiredData.resultTF = false;
                exactAndHeuristic.isHeuristic = true;
                return true;
            }
        }
        return false;
    }


    /**
     * @param modeRequiredData  oriData.modeRequiredData
     * @param status            gurobi状态 x-check 传入optimal  1cbp 传入 !optimal
     * @param exactAndHeuristic 精确算法结果
     * @return true: 算法结束，无需继续迭代；false: 继续迭代
     */
    public static boolean upDateTopToBottom(ModeRequiredData modeRequiredData, int status, SkyLineResultData exactAndHeuristic) {
        if (status == GRB.Status.OPTIMAL) {// x-check有解
            modeRequiredData.UB = modeRequiredData.oriSize[1];
            System.out.println("modeRequiredData.oriSize[1] = " + modeRequiredData.oriSize[1] + "   -----   ");
            if (modeRequiredData.UB == modeRequiredData.LB) {
                modeRequiredData.resultTF = true;
                exactAndHeuristic.isHeuristic = false;
                return true;
            }
            --modeRequiredData.oriSize[1];
        } else {// 1CBP无解 -- 终止求解
            modeRequiredData.resultTF = true;
            if (modeRequiredData.UB == exactAndHeuristic.skyLine.skyHeight) {
                exactAndHeuristic.isHeuristic = true;
            } else {
                exactAndHeuristic.isHeuristic = false;
            }
            modeRequiredData.oriSize[1] = modeRequiredData.UB;
            if (status != GRB.Status.TIME_LIMIT) {
                modeRequiredData.LB = modeRequiredData.oriSize[1];
            }

            // modeRequiredData.LB = modeRequiredData.UB;
            return true;
        }
        return false;
    }


}
