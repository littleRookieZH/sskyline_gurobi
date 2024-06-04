package main.java.exactsolution.onedimensionalcontiguousmodifygurobi;

import gurobi.*;
import ilog.concert.*;
import ilog.cplex.IloCplex;
import main.java.com.pointset.ToolClass;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author pc 检查x是否可行
 */
public class CheckCplex {
    GRBEnv env = null;
    GRBModel checkModel;
    GRBVar[] x;
    GRBVar[][] leftPos;
    static int LARGERM = 100000;

    public static boolean xCheck(int[][] solution, ModeRequiredData modeRequiredData) throws IloException, GRBException {
        int[] widthPoints = modeRequiredData.widthPoints;
        int[][] targetSize = modeRequiredData.targetBlockSize;
        int[][] step1Solutions = ToolClass.copyTwoDim(solution);
        ToolClass.sortedTwoDim(step1Solutions, 0);




        GRBEnv env = new GRBEnv();
        // 设置日志级别为0（完全禁用）
        env.set(GRB.IntParam.OutputFlag, 0);
        GRBModel checkModel = new GRBModel(env);
        modeRequiredData.check.checkModel = checkModel;
        modeRequiredData.check.env = env;

        // int blockNum = modeRequiredData.targetBlockSize.length;

        int nowBlockNum = step1Solutions.length;
        boolean[][] widthPlacedPoints = modeRequiredData.widthPlacedPoints;

        int Height = modeRequiredData.oriSize[1];
        int Width = modeRequiredData.oriSize[0];
        // 添加求解模型的时间限制
        checkModel.set(GRB.DoubleParam.TimeLimit, ModeRequiredData.X_CHECK_TIMELIMIT);

        // lik lki 用来表示i k的相对位置
        // lik lki 用来表示 i k 的相对位置
        GRBVar[][] leftPos = new GRBVar[nowBlockNum][nowBlockNum];

        // x[i][x0]  x0: 表示X方向离散点
        GRBVar[] x = new GRBVar[nowBlockNum];
        modeRequiredData.check.x = x;
        modeRequiredData.check.leftPos = leftPos;

        // 目标块在X方向只能放一次
        for (int i = 0; i < nowBlockNum; i++) {
            int blockIndex = step1Solutions[i][0];
            x[i] = checkModel.addVar(0, Width - targetSize[blockIndex][0], 0.0, GRB.CONTINUOUS, "x[" + i + "]");
            checkModel.addConstr(x[i], GRB.LESS_EQUAL, Width - targetSize[blockIndex][0], "c_x_" + i);
        }

        for (int i = 0; i < nowBlockNum; i++) {
            int blockIndexI = step1Solutions[i][0];
            for (int j = 0; j < i; j++) {
                int blockIndexJ = step1Solutions[j][0];
                // step1Solutions[][] [0]: i ; [1]: yi ; [2]: yi + hi
                // yi + hi - 1 ≥ yj, yj + hj - 1 ≥ yi
                if (step1Solutions[i][2] - 1 >= step1Solutions[j][1] && step1Solutions[j][2] - 1 >= step1Solutions[i][1]) {
                    leftPos[i][j] = checkModel.addVar(0, 1, 0, GRB.BINARY, "leftPos[" + i + "][" + j + "]");
                    leftPos[j][i] = checkModel.addVar(0, 1, 0, GRB.BINARY, "leftPos[" + j + "][" + i + "]");

                    // xk + (1 − lik)M ≥ xi + wi
                    GRBLinExpr expr = new GRBLinExpr();
                    expr.addTerm(1.0, x[i]);
                    expr.addTerm(-1.0, x[j]);
                    expr.addTerm(LARGERM, leftPos[i][j]);
                    checkModel.addConstr(expr, GRB.LESS_EQUAL, LARGERM - targetSize[blockIndexI][0], "c_left_" + i + "_" + j);


                    // GRBLinExpr expr1 = new GRBLinExpr();
                    // for (int k = 0; k < widthPoints.length; k++) {
                    //     if (widthPlacedPoints[blockIndexJ][widthPoints[k]]) {
                    //         expr1.addTerm(widthPoints[k], x[blockIndexJ][widthPoints[k]]);
                    //     }
                    //     if (widthPlacedPoints[blockIndexI][widthPoints[k]]) {
                    //         expr1.addTerm(-widthPoints[k], x[blockIndexI][widthPoints[k]]);
                    //     }
                    // }
                    // expr1.addTerm(-LARGERM, leftPos[blockIndexI][blockIndexJ]);
                    // expr1.addConstant(LARGERM);
                    // checkModel.addConstr(expr1, GRB.GREATER_EQUAL, targetSize[blockIndexI][0], "c2_" + blockIndexI + "_" + blockIndexJ);

                    // xi + (1 − lki)M ≥ xk + wk
                    GRBLinExpr expr2 = new GRBLinExpr();
                    expr2.addTerm(1.0, x[j]);
                    expr2.addTerm(-1.0, x[i]);
                    expr2.addTerm(LARGERM, leftPos[j][i]);
                    checkModel.addConstr(expr2, GRB.LESS_EQUAL, LARGERM - targetSize[blockIndexJ][0], "c_left_" + j + "_" + i);


                    // GRBLinExpr expr2 = new GRBLinExpr();
                    // for (int k = 0; k < widthPoints.length; k++) {
                    //     if (widthPlacedPoints[blockIndexI][widthPoints[k]]) {
                    //         expr2.addTerm(widthPoints[k], x[blockIndexI][widthPoints[k]]);
                    //     }
                    //     if (widthPlacedPoints[blockIndexJ][widthPoints[k]]) {
                    //         expr2.addTerm(-widthPoints[k], x[blockIndexJ][widthPoints[k]]);
                    //     }
                    // }
                    // expr2.addConstant(LARGERM);
                    // expr2.addTerm(-LARGERM, leftPos[blockIndexJ][blockIndexI]);
                    // checkModel.addConstr(expr2, GRB.GREATER_EQUAL, modeRequiredData.targetBlockSize[blockIndexJ][0], "c3_" + blockIndexI + "_" + blockIndexJ);

                    // lik + lki = 1
                    GRBLinExpr expr3 = new GRBLinExpr();
                    expr3.addTerm(1, leftPos[i][j]);
                    expr3.addTerm(1, leftPos[j][i]);
                    checkModel.addConstr(expr3, GRB.EQUAL, 1, "c4_" + i + "_" + j);
                }
            }
        }

        //  lijd，添加关于缺陷块的约束：目标块不能和缺陷块重合
        // 这里应该是有问题
        GRBVar[][] leftDefectPos = new GRBVar[modeRequiredData.defectSize.length][nowBlockNum];
        for (int i = 0; i < modeRequiredData.defectSize.length; i++) {
            for (int j = 0; j < nowBlockNum; j++) {
                // yj + hj - 1 ≥ yd1, yd2 - 1 ≥ yj
                if (step1Solutions[j][2] - 1 >= modeRequiredData.defectSize[i][1] && modeRequiredData.defectSize[i][3] - 1 >= step1Solutions[j][1]) {
                    int blockIndexJ = step1Solutions[j][0];
                    leftDefectPos[i][j] = checkModel.addVar(0, 1, 0, GRB.BINARY, "leftDefectPos[" + j + "][" + i + "]");


                    GRBLinExpr expr3 = new GRBLinExpr();
                    expr3.addTerm(1.0, x[j]);
                    expr3.addTerm(-LARGERM, leftDefectPos[i][j]);
                    // expr3 = x[i] - LARGERM * leftDefectPos[d][i]

                    // Add constraints
                    checkModel.addConstr(expr3, GRB.LESS_EQUAL, modeRequiredData.defectSize[i][0] - targetSize[blockIndexJ][0], "c_left_" + i + "_" + j);
                    checkModel.addConstr(expr3, GRB.GREATER_EQUAL, modeRequiredData.defectSize[i][2] - LARGERM, "c_right_" + i + "_" + j);

                    // // expr4 表达式
                    // GRBLinExpr expr4 = new GRBLinExpr();
                    // GRBLinExpr expr5 = new GRBLinExpr();
                    // for (int l = 0; l < widthPoints.length; l++) {
                    //     if (widthPlacedPoints[blockIndexJ][widthPoints[l]]) {
                    //         expr4.addTerm(widthPoints[l], x[blockIndexJ][widthPoints[l]]);
                    //         expr5.addTerm(widthPoints[l], x[blockIndexJ][widthPoints[l]]);
                    //     }
                    // }
                    // expr4.addTerm(LARGERM, leftDefectPos[blockIndexJ][i]);
                    // expr4.addConstant(-modeRequiredData.defectSize[i][2]);
                    // checkModel.addConstr(expr4, GRB.GREATER_EQUAL, 0, "c4_" + blockIndexJ + "_" + i);
                    //
                    // expr5.addTerm(LARGERM, leftDefectPos[blockIndexJ][i]);
                    // expr5.addConstant(-modeRequiredData.defectSize[i][0]);
                    // checkModel.addConstr(expr5, GRB.LESS_EQUAL, LARGERM - modeRequiredData.targetBlockSize[blockIndexJ][0], "c5_" + blockIndexJ + "_" + i);
                }
            }
        }
        // 优化模型
        checkModel.optimize();

        if (checkModel.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
            boolean accuracyIssue = false;
            double epsilon = 0.0002; // 定义一个很小的数作为容差
            for (int i = 0; i < nowBlockNum; i++) {
                int blockIndexI = step1Solutions[i][0];
                System.out.println("num i = " + blockIndexI + "xpoint = " + modeRequiredData.check.x[i].get(GRB.DoubleAttr.X) + "y point = " + step1Solutions[i][1]);

                for (int j = 0; j < i; j++) {
                    int blockIndexJ = step1Solutions[j][0];
                    // step1Solutions[][] [0]: i ; [1]:  yi ; [2]: yi+hi
                    // yi + hi - 1 ≥ yj, yj + hj - 1 ≥ yi
                    double val1 = 0;
                    double val2 = 0;

                    if (step1Solutions[i][2] - 1 >= step1Solutions[j][1] && step1Solutions[j][2] - 1 >= step1Solutions[i][1]) {
                        val1 = modeRequiredData.check.leftPos[i][j].get(GRB.DoubleAttr.X);
                        val2 = modeRequiredData.check.leftPos[j][i].get(GRB.DoubleAttr.X);
                        System.out.println("leftPos[" + i + "][" + j + "] = " + val1);
                        System.out.println("leftPos[" + j + "][" + i + "] = " + val2);
                        if (Math.abs(val1 - val2) > epsilon) {
                            accuracyIssue = true;
                        }
                    }

                }
            }
            // System.out.println("accuracyIssue = " + (accuracyIssue ? "true" : ""));
            // 可行
            return true;
        } else {
            checkModel.dispose();
            env.dispose();
            return false;
        }
    }
}
