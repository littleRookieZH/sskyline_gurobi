package main.java.com.columngeneration;

import ilog.concert.*;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.Arrays;

//    模型使用数据类
class DataCuttingStock {
    //木材长度
    double rollWidth = 20;
    //需求量
    double[] demand = {12, 12, 6, 6, 5, 5, 12, 7, 7, 6, 2, 2, 4, 4, 2, 2};
    //切割方式
    double[] cutSize = {2, 7, 8, 3, 3, 5, 3, 3, 5, 2, 3, 4, 3, 4, 9, 11};
}

public class ColumnGenerationCuttingStockDemo {
    //模型使用变量
    private static class IloNumVarArray {
        //已添加的变量数量
        int addNum = 0;
        //定义变量集合：初始长度为32或者根据data类进行设置
        IloNumVar[] varsArray = new IloNumVar[32];

        //变量添加
        void add(IloNumVar vars) {
            if (addNum >= varsArray.length) {
                varsArray = Arrays.copyOf(varsArray, varsArray.length * 2);
            }
            varsArray[addNum++] = vars;
        }

        //变量获取
        IloNumVar getElement(int i) {
            return varsArray[i];
        }

        //变量数量获取
        int getSize() {
            return addNum;
        }
    }

    //    定义数据
    DataCuttingStock data = new DataCuttingStock();

    public ColumnGenerationCuttingStockDemo(DataCuttingStock data) throws IloException {
        this.data = data;
    }

    //木材长度
    double rollWidth = data.rollWidth;
    //需求量
    double[] demand = data.demand;
    //切割方式
    double[] cutSize = data.cutSize;
    //变量数量
    int variableNum = cutSize.length;
    //变量集合
    IloNumVarArray cut = new IloNumVarArray();
    //切割方案
    ArrayList<int[]> cutMethod = new ArrayList<int[]>();
    //=========================设置主模型===================
    IloCplex masterPro = new IloCplex();
    IloObjective cost = masterPro.addMinimize();
    //添加约束范围
    IloRange[] cons = new IloRange[variableNum];
    //=========================设置子模型===================
    IloCplex subPro = new IloCplex();
    IloObjective reduceCost = subPro.addMinimize();
    IloNumVar[] subVar = subPro.numVarArray(variableNum, 0, Integer.MAX_VALUE, IloNumVarType.Int);

    //获取初始解方案
    private void getInitCutMethod() {
        for (int i = 0; i < variableNum; i++) {
            int[] cutPlan = new int[variableNum];
            cutPlan[i] = (int) (rollWidth / cutSize[i]);
            cutMethod.add(cutPlan);
        }
        for (int i = 0; i < variableNum; i++) {
            for (int j = 0; j < variableNum; j++) {
                System.out.print(cutMethod.get(i)[j] + ",");
            }
            System.out.println();
        }
    }

    //构建主模型
    private void buildMasterModel() throws IloException {
        //添加约束范围
        for (int i = 0; i < cons.length; i++) {
            cons[i] = masterPro.addRange(demand[i], Double.MAX_VALUE);
        }
        //按列添加模型
        for (int i = 0; i < variableNum; i++) {
            IloColumn column = masterPro.column(cost, 1.0).and(masterPro.column(cons[i], cutMethod.get(i)[i]));
            cut.add(masterPro.numVar(column, 0, Double.MAX_VALUE, IloNumVarType.Float));
        }
        //设置求解参数
        masterPro.setParam(IloCplex.Param.RootAlgorithm, IloCplex.Algorithm.Primal);
    }

    //构建子模型
    private void buildSubModel() throws IloException {
        subPro.addRange(-Double.MAX_VALUE, subPro.scalProd(cutSize, subVar), rollWidth);
    }

    //使用列生成算法求解切割下料问题
    private void solveMethod() throws IloException {
        getInitCutMethod();
        buildMasterModel();
        buildSubModel();
        //新方案
        int[] newPlan = new int[variableNum];
        //迭代计数
        int iterCount = 1;
        //循环添加可行列
        for (; ; ) {
            System.out.println("第" + iterCount + "次迭代");
            //主模型求解
            System.out.println("主模型：");
            System.out.println(masterPro);
            masterPro.setOut(null);
            masterPro.solve();
            masterPro.exportModel("master" + iterCount + ".lp");
            System.out.println("主问题的目标函数为：" + masterPro.getObjValue());
            //获取影子价格
            double[] price = masterPro.getDuals(cons);
            System.out.println("影子价格：");
            for (int i = 0; i < price.length; i++) {
                System.out.print(price[i] + "\t");
            }
            System.out.println();
            //根据影子价格添加子模型目标函数
            reduceCost.setExpr(subPro.diff(1, subPro.scalProd(price, subVar)));
            System.out.println("子模型：");
            System.out.println(subPro);
            //子模型求解
            subPro.setOut(null);
            subPro.solve();
            subPro.exportModel("sub" + iterCount + ".lp");
            System.out.println("子模型目标值：" + subPro.getObjValue());
            System.out.println("子模型结果：");
            for (int i = 0; i < subVar.length; i++) {
                System.out.print((int) subPro.getValue(subVar[i]) + "\t");
            }
            System.out.println();
            //若子模型目标值大于0.终止迭代（检验数大于0，无进基列）
            if (subPro.getObjValue() >= 0) break;
            //根据子模型变量值确定新方案
            for (int i = 0; i < newPlan.length; i++) {
                newPlan[i] = (int) subPro.getValue(subVar[i]);
            }
            cutMethod.add(newPlan);
            //主模型添加新列
            IloColumn newCol = masterPro.column(cost, 1);
            for (int i = 0; i < newPlan.length; i++) {
                newCol = newCol.and(masterPro.column(cons[i], cutMethod.get(cutMethod.size() - 1)[i]));
            }
            cut.add(masterPro.numVar(newCol, 0, Double.MAX_VALUE, IloNumVarType.Float));
            //记录操作数
            iterCount++;
        }
        printResult();
    }

    //生成最终结果
    private void printResult() throws IloException {
        for (int i = 0; i < cut.getSize(); i++) {
            //直接将变量转换为整数
            masterPro.add(masterPro.conversion(cut.getElement(i), IloNumVarType.Int));
            //masterPro.add(cut.getElement(i));
        }
        System.out.println("最终模型：");
        System.out.println(masterPro);
        masterPro.solve();
        masterPro.setOut(null);
        masterPro.exportModel("lp1.lp");
        System.out.println("总切割数量为：" + masterPro.getObjValue());
    }

    public static void main(String[] args) throws IloException {
        DataCuttingStock data = new DataCuttingStock();
        ColumnGenerationCuttingStockDemo CGM = new ColumnGenerationCuttingStockDemo(data);
        CGM.solveMethod();

    }
}
