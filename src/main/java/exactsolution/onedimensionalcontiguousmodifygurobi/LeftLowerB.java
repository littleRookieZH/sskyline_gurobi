package main.java.exactsolution.onedimensionalcontiguousmodifygurobi;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

/**
 * @author xzbz
 * @create 2023-11-14 18:59
 * 使用提升下界
 */
public class LeftLowerB {

    /**
     * @description 定义cplex内部类的对象
     */
    IloCplex model;

    /**
     * @description 定义z[x][y][i]表示小矩形i是否放置在离散点(x, y)处
     */
    IloNumVar[][] R;

    /**
     * @description H表示LowBound的最小高度
     */
    public IloNumVar[] H;


    public IloCplex lowerBoundsBased(ModeRequiredData modeRequiredData) throws IloException {

        IloCplex checkModel = new IloCplex();
        int blockNum = modeRequiredData.targetBlockSize.length;
        int Height = modeRequiredData.heightPointsMax[modeRequiredData.lbIndex];
        int[][] targetBlockSize = modeRequiredData.targetBlockSize;
        int[] oriSize = modeRequiredData.oriSize;
        // 添加求解模型的时间限制
        checkModel.setParam(IloCplex.DoubleParam.TiLim, ModeRequiredData.X_CHECK_TIMELIMIT);

        //
        IloNumVar[][] x = new IloNumVar[blockNum][Height];


        // 目标块在X方向只能放一次
        for (int i = 0; i < blockNum; i++) {
            for (int j = 0; j < Height; j++) {
                //将 x[i][j]为0，1变量
                x[i][j] = checkModel.numVar(0, 1, IloNumVarType.Bool, "x[" + i + "][" + j + "]");
            }
        }

        for (int i = 0; i < blockNum; i++) {
            IloNumExpr expr1 = checkModel.numExpr();
            for (int j = 0; j < Height; j++) {
                expr1 = checkModel.sum(expr1, checkModel.prod(x[i][j], 1));
            }
            checkModel.addGe(expr1, targetBlockSize[i][1]);
        }
        for (int i = 0; i < Height; i++) {
            IloNumExpr expr2 = checkModel.numExpr();
            for (int j = 0; j < blockNum; j++) {
                expr2 = checkModel.sum(expr2, checkModel.prod(x[j][i], targetBlockSize[j][0]));
            }
            checkModel.addLe(expr2, oriSize[0]);
        }
        checkModel.setOut(null);
        return checkModel;
    }

    public void solveModel(ModeRequiredData modeRequiredData) throws IloException {
        IloCplex iloCplex = lowerBoundsBased(modeRequiredData);
        while(!iloCplex.solve()){
            modeRequiredData.lbIndex++;
            iloCplex = lowerBoundsBased(modeRequiredData);
            System.out.println("下界 " + modeRequiredData.heightPointsMax[modeRequiredData.lbIndex]);
        }
    }

    /**
     * @param arr
     * @return int
     * @description 求和
     * @author hao
     * @date 2023/3/26 15:37
     */
    public int getTotal(int[] arr) {
        int total = 0;
        for (int i : arr) {
            total += i;
        }
        return total;
    }
}
