package main.java.exactsolution.onedimensionalcontiguousmodifygurobi;


import gurobi.*;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
import main.java.com.pointset.DefectBlockSize;
import main.java.com.pointset.PointReductionMethod;
import main.java.com.pointset.ToolClass;
import main.java.com.twodimension.GetFile;
import main.java.com.twodimension.TargetData;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public class CplexLowerBound {
    /**
     * @description 引用矩形对象
     */
    TargetData recLayOutData;

    /**
     * @description 定义cplex内部类的对象
     */
    // IloCplex model;

    /**
     * @description 定义z[x][y][i]表示小矩形i是否放置在离散点(x, y)处
     */
    // IloNumVar[][] R;

    /**
     * @description 生成离散点集
     */
    PointReductionMethod discretePoints;

    /**
     * @description 缺陷块每一列的高度
     */
    DefectBlockSize defectHeight;

    /**
     * @description H表示LowBound的最小高度
     */
    // public IloNumVar[] H;


    public void lowerBoundsBased() throws IloException, GRBException {
        //获取原放置区域的宽度
        int totalWidth = recLayOutData.oriSize[0];
        //获得每种目标块的尺寸信息
        int[][] blockSize = recLayOutData.targetBlockSize;
        //获取每种目标块的数量
        int[] blockNumber = recLayOutData.tarMaxNum;
        //获取目标块的种类
        int kinds = blockNumber.length;
        //获取所有目标块的数量和
        int total = getTotal(blockNumber);
        //缺陷块每一列的高度
        int[] defHeight = defectHeight.defectHeight;
        int[] defWidth =  defectHeight.defectWidth;
        //宽度离散点集
        int[] widthPoints = discretePoints.getLeftBottom(recLayOutData);
        //高度离散点集
        int[] heightPoints = discretePoints.getHeightPoints();

        int area = 0;
        for (int i = 0; i < blockSize.length; i++) {
            area += blockSize[i][0]* blockSize[i][1] * blockNumber[i];
        }
        int aver_width = area / totalWidth;
        System.out.println("所有目标块总面积的平均高度为：" + aver_width);

        //建立model
        GRBEnv env = new GRBEnv();
        GRBModel model = new GRBModel(env);

        //variables  Zxyi   x,y离散点集的X,Y坐标   i是目标块数量
        //实例化变量,确定变量类型和名称
        int maxWidth = discretePoints.getMaxLength(widthPoints);

        GRBVar[][] R = new GRBVar[maxWidth + 1][total];
        //因为x = maxWidth时，也可以放置
        for (int x = 0; x < maxWidth + 1; x++) {
            int sum = 0;
            for (int j = 0; j < kinds; j++) {
                for (int k = 0; k < blockNumber[j]; k++) {
                    // 将R[x][i]松弛为0，1变量
                    R[x][sum] = model.addVar(0, 1, 0, GRB.CONTINUOUS, "z[" + x + "][" + sum + "]");
                    sum++;
                }
            }
        }
        GRBVar h = model.addVar(0, GRB.INFINITY, 0.0, GRB.INTEGER, "h");
        //添加目标函数
        GRBLinExpr objExpr = new GRBLinExpr();
        objExpr.addTerm(1.0, h); // 将 h 作为目标函数的系数
        model.setObjective(objExpr, GRB.MINIMIZE);

        //加入约束
        //约束一：表示所有的目标块必须被切割
        int sum1 = 0;
        for (int i = 0; i < kinds; i++) {
            // 对每一个目标块都创建一个约束表达式
            for (int j = 0; j < blockNumber[i]; j++) {
                GRBLinExpr expr1 = new GRBLinExpr();
                // 对于一个目标块来说，所有离散点 x 的求和应该为1
                for (int k = 0; k < widthPoints.length; k++) {
                    if (widthPoints[k] + blockSize[i][0] <= totalWidth) {
                        expr1.addTerm(1.0, R[widthPoints[k]][sum1]);
                    }
                }
                model.addConstr(expr1, GRB.EQUAL, 1, "c1_" + i + "_" + j);
                sum1++;
            }
        }
        //约束二：表示覆盖第 q 列长条目标块 总长度不超过该长条的可用长度
        for (int q = 0; q < maxWidth + 1; q++) {
            // 判断第 q 列是否属于离散点集
            if (!ToolClass.contains(widthPoints, q)) {
                continue;
            }
            // 每一列都有一个约束条件
            GRBLinExpr expr2 = new GRBLinExpr();
            int sum2 = 0;
            for (int i = 0; i < blockNumber.length; i++) {
                // 获取上下界 q - wi + 1   blockSize
                int lowBound1 = Math.max(q - blockSize[i][0] + 1, 0);
                int upperBound1 = q;
                int hi = blockSize[i][1];

                // 对于每一个目标块而言
                for (int j = 0; j < blockNumber[i]; j++) {
                    for (int x = lowBound1; x <= upperBound1; x++) {
                        // 判断x是否是离散点，同时判断是否可以放在离散点x上
                        if ((ToolClass.contains(widthPoints, x)) && ((x + blockSize[i][0] <= totalWidth))) {
                            expr2.addTerm(hi, R[x][sum2]);
                        }
                    }
                    sum2++;
                }
            }
            // 每一列目标块占据的高度 + 缺陷块的高度 <= 最小高度
            expr2.addConstant(defWidth[q]);
            model.addConstr(expr2, GRB.LESS_EQUAL, h, "c2_" + q);
        }

        model.optimize();
        // Solve the model and extract the solution
        if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
            System.out.println("Optimal objective: " + model.get(GRB.DoubleAttr.ObjVal));
        } else {
            System.out.println("No optimal solution found.");
        }
        model.dispose();
        env.dispose();
    }

    // private void solveModel() throws IloException {
    //     if (model.solve()) {
    //         System.out.println("模型目标值：" + model.getObjValue());
    //     } else {
    //         System.out.println("模型不可解");
    //     }
    // }

    /**
     * @description 求和
     * @author  hao
     * @date    2023/3/26 15:37
     * @param arr
     * @return int
    */
    public int getTotal(int[] arr) {
        int total = 0;
        for (int i : arr) {
            total += i;
        }
        return total;
    }

    @Test
    public void test01() throws FileNotFoundException, IloException, GRBException {
        GetFile getFile = new GetFile();
        File file = new File("E:\\EssayTestSet\\2D-SPPActual\\Test\\cplex_test\\part04");
        List<File> allFile = getFile.getAllFile(file);
        for (File value : allFile) {
            String path = value.getAbsolutePath();
            TargetData recData1 = new TargetData();
            recData1.initData(path);
            int height = LowerBound.averageHeight(recData1.targetBlockSize, recData1.oriSize[0], recData1.defectiveBlocksSize);
            recData1.oriSize[1] = height;
            CplexLowerBound cplexModel = new CplexLowerBound();
            cplexModel.recLayOutData = recData1;
            //PointReductionMethod
            cplexModel.discretePoints = new PointReductionMethod();
            //得到X方向离散点
            cplexModel.discretePoints.getXOrYCoordinates(recData1, true);
            //得到Y方向离散点
            cplexModel.discretePoints.getXOrYCoordinates(recData1, false);
            //初始化 DefectBlockSize
            // 初始化 DefectBlockSize
            cplexModel.defectHeight = new DefectBlockSize();
            // 计算每一行的宽度
            cplexModel.defectHeight.defectColumnWidth(cplexModel.discretePoints.getHeightPoints(), recData1.defPoints);
//            // 计算每一列的高度
//            cplexModel.defectHeight.defectColumnHeight(cplexModel.discretePoints.getWidthPoints(), recData1.defPoints);
//            cplexModel.defectHeight.placementWidth(recData1.targetBlockSize, recData1.oriSize[0], recData1.defPoints);

            cplexModel.lowerBoundsBased();
        }
    }
}
