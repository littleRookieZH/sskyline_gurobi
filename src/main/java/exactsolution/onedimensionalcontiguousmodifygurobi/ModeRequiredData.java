package main.java.exactsolution.onedimensionalcontiguousmodifygurobi;

import gurobi.GRBException;
import ilog.concert.IloException;
import main.java.com.pointset.DefectBlockSize;
import main.java.com.pointset.ToolClass;
import main.java.com.twodimension.SkyLine;
import main.java.com.twodimension.TargetData;
import main.java.com.twodimensiondata.AdaptiveData;
import main.java.com.twodimensiondata.SkyLineResultData;
import main.java.exactsolution.blockpreprocessing.PreprocessBlock;
import main.java.exactsolution.dualccm1.Ccm1;
import main.java.exactsolution.pointprocessing.DefPointData;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * @author hao
 * @description: 模型数据类
 * @date 2023/7/5 20:07
 */
public class ModeRequiredData implements Serializable {
    /**
     * @description 原料板尺寸，只有宽度和高度
     */
    public int[] oriSize;

//    public int lowerBoundHeight;
    /**
     * @description 预处理之后的尺寸
     * 目标块尺寸, 需要初始化   w  h
     */
    public int[][] targetBlockSize;
    /**
     * @description 原尺寸
     * 目标块尺寸, 需要初始化   w  h
     */
    int[][] blockSize;
    /**
     * @description 目标块数量
     */
    public int targetNumber;
    /**
     * @description 缺陷块的位置
     */
    public int[][] defectSize;
    /**
     * @description 缺陷块的数量
     */
    public int defNum;
    /**
     * @description 宽度离散点集
     */
    public int[] widthPoints;

    /**
     * @description 每一行缺陷块的总长度
     */
    public int[] defectWidth;
    // 宽度：每一个离散点的具体可放置情况
    public boolean[][] widthPlacedPoints;
    /**
     * @description 高度离散点集
     */
    public int[] heightPoints;

    // 高度：每一个离散点的具体可放置情况
    public boolean[][] heightPlacedPoints;
    public int[][] resultPoints;
    //计算高度最小
    public int minHeight;
    // 计算宽度最小
    public int minWidth;
    transient public CheckCplex check;
    /**
     * @description 不可行解的约束集合 --   保留当前高度的lift解
     */
    List<int[][]> exprList;

    /**
     * @description 不可行解的约束集合 --   保留小于当前高度的所有lift解
     */
    List<int[][]> exprListPrev;

    List<ConserveSolution> liftCutPrev;

    /**
     * @description 过滤器
     */
    HashSet<String> filterHashSet;
    HashSet<String> hashSetMinInfeases;
    HashSet<String> hashSetNonInfeases;
    HashSet<String> hashSetAreaInfeases;
    HashSet<String> hashSetInfeases;
    public final static double CBP_TIMELIMIT = 1200;

    public final static double LIFTLB_TIMELIMIT = 100;
    public final static double X_CHECK_TIMELIMIT = CBP_TIMELIMIT;
    public final static double INFEASIBLE_TIMELIMIT = 120;
    public final static double LIFTCUT_TIMELIMIT = 1200;
    public final static double Two_Dimension_Scheme = 1200;
    public final static double Solution_Times = CBP_TIMELIMIT;

    // 以上界为高度的离散点集
    public int[] heightPointsMax;
    // 当前的下界高度
    public int LB;
    // 下界的索引值
    public int lbIndex;

    // 判断模型是否有解
    public boolean resultTF;

    public boolean resultTFTimeOut;
    public HashMap<Integer, ArrayList<Integer>> itemType;

    // 数组长度是启发式计算的上界；记录是否使用求解器计算过高度；初始值为 -1；如果计算过则等于计算高度
    public int[] heightResult;
    // 当前的上界高度
    public int UB;

    public boolean isRotation;




    public void setModeRequiredData(DefPointData defPointData, TargetData targetData) {
        oriSize = targetData.oriSize; // 不变
        defNum = targetData.defNum;  // 不变
        targetNumber = targetData.targetNumber; // 不变
        widthPoints = defPointData.widthPoints;  // 不变
        widthPlacedPoints = defPointData.widthPlacedPoints;
        heightPlacedPoints = defPointData.heightPlacedPoints;
        defectSize = targetData.defPoints; // 不变
        heightPoints = defPointData.heightPoints; // 变化
//        defectWidth = dBS.defectWidth;  // 每一行缺陷块的总长度  变化
    }

    public ModeRequiredData() {

    }

    /**
     * 初始化模型参数
     * @param path
     * @param rLayout
     * @return
     * @throws IloException
     * @throws FileNotFoundException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public SkyLineResultData initModel1(String path, TargetData rLayout) throws IloException, FileNotFoundException, InvocationTargetException, IllegalAccessException {

        // 计算精确算法的上界
        List<TargetData> dataList = new ArrayList<>();
        dataList.add(rLayout);
        SkyLine skyLine1 = new SkyLine();
        // SkyLineResultData skyLineResultData = skyLine1.fitPackExact(dataList, path);

        AdaptiveData adaptiveData = new AdaptiveData();
        // SkyLineResultData skyLineResultData = skyLine1.fitPack(dataList, path);
        SkyLineResultData skyLineResultData = adaptiveData.initialAdaptive(dataList, path);

        // SkyLineResultData skyLineResultData = skyLine1.fitPackExact(dataList, path);

        ModeRequiredData modeRequiredData = new ModeRequiredData();

        modeRequiredData.UB = skyLineResultData.skyLine.skyHeight;
        modeRequiredData.heightResult = new int[modeRequiredData.UB + 1];
        Arrays.fill(modeRequiredData.heightResult, -1);

        int[][] blockSize = ToolClass.copyTwoDim(rLayout.targetBlockSize);
        int[][] copyBlocks = ToolClass.copyTwoDim(blockSize);

        // 计算下界
        int ccm1 = 0;
        try {
            ccm1 = Ccm1.regionLowerBound(rLayout, rLayout.defectiveBlocksSize);
        } catch (IloException e) {
            throw new RuntimeException(e);
        }

        // 缺陷块的最大高度
        int maxDefHeight = DefPointData.maxSize(rLayout.defPoints, 3);
        // 目标块的最大高度
        int maxBlocksHeight = DefPointData.maxSize(copyBlocks, 1);
        // 确定最低下界
        modeRequiredData.LB = maxBlocksHeight > maxDefHeight ? Math.max(ccm1, maxBlocksHeight) : Math.max(ccm1, maxDefHeight);

        // 从下往上
        rLayout.oriSize[1] = modeRequiredData.LB;

        // 从上往下
        // rLayout.oriSize[1] = (modeRequiredData.UB == modeRequiredData.LB) ?  modeRequiredData.LB : (modeRequiredData.UB - 1);

        // 二分
        // rLayout.oriSize[1] = modeRequiredData.LB;

//        rLayout.oriSize[1] = 31;
        DefPointData discretePoints = new DefPointData();

        modeRequiredData.setModeRequiredData(discretePoints, rLayout);
        // 初始化目标块（整合：种类和数量）
        modeRequiredData.targetBlockSize = blockSize;
        modeRequiredData.blockSize = copyBlocks;

        // 预处理
        PreprocessBlock preprocessBlock = new PreprocessBlock();
        // 计算宽度最小
        int minWidth = DefPointData.minSize(blockSize, 0);
        //计算高度最小
        int minHeight = DefPointData.minSize(blockSize, 1);

        // 处理：blockSize的宽和高 和 oriSize[0]
        preprocessBlock.processBlock(minHeight, minWidth, rLayout.oriSize, rLayout.defPoints, blockSize);

        // DefPointData
        // 得到X方向离散点
        // DefPointData：widthPlacedPoints（每个点的放置情况）、widthPoints、leftModelW、rightModelW 被赋初值
        discretePoints.widthSet(rLayout.oriSize[0], minWidth, blockSize, rLayout.defPoints);
        // 依据上界计算离散点
        int[] upDefPoints = discretePoints.heightSetMax(skyLineResultData.skyLine.skyHeight, minHeight, blockSize, rLayout.defPoints);
        // 判断下界与上界的关系
        // 上界离散点
        modeRequiredData.heightPointsMax = upDefPoints;

        // modeRequiredData.LB = rLayout.oriSize[1];

        if(modeRequiredData.LB >= skyLineResultData.skyLine.skyHeight){
            System.out.println("上界：" + skyLineResultData.skyLine.skyHeight);
            System.out.println("下界：" + modeRequiredData.LB);
            skyLineResultData.isHeuristic = true;
            return skyLineResultData;
        }
        System.out.println("上界：" + skyLineResultData.skyLine.skyHeight);
        System.out.println("下界：" + modeRequiredData.LB);

        // // 初始化UB和heightResult
        // modeRequiredData.UB = skyLineResultData.skyLine.skyHeight;
        // modeRequiredData.heightResult = new int[UB];
        // Arrays.fill(modeRequiredData.heightResult, -1);

        modeRequiredData.lbIndex = modeRequiredData.findLbIndex(upDefPoints, modeRequiredData.LB);
        // 不连续下界，模型。判断下界值
//        LeftLowerB leftLowerB = new LeftLowerB();
//        leftLowerB.solveModel(modeRequiredData);

        // 得到Y方向离散点
        discretePoints.heightSet(rLayout.oriSize[1], minHeight, blockSize, rLayout.defPoints);

        modeRequiredData.setModeRequiredData(discretePoints, rLayout);
        // 初始化目标块（整合：种类和数量）
        modeRequiredData.targetBlockSize = blockSize;
        modeRequiredData.blockSize = copyBlocks;
        modeRequiredData.itemType = rLayout.itemType;
        // 确定下界
        modeRequiredData.minWidth = minWidth;
        modeRequiredData.minHeight = minHeight;
        modeRequiredData.check = new CheckCplex();

        modeRequiredData.exprList = new ArrayList<>();
        modeRequiredData.exprListPrev = new ArrayList<>();
        modeRequiredData.liftCutPrev = new ArrayList<>();
        modeRequiredData.filterHashSet = new HashSet<String>();
        modeRequiredData.hashSetMinInfeases = new HashSet<String>();
        modeRequiredData.hashSetNonInfeases = new HashSet<String>();
        modeRequiredData.hashSetAreaInfeases = new HashSet<String>();
        modeRequiredData.hashSetInfeases = new HashSet<String>();

        ModifyModeRequiredData modifyModeRequiredData = new ModifyModeRequiredData(modeRequiredData, rLayout);

        ModifyModeRequiredData modifyModeRequiredData1 = modifyModeRequiredData.reverseParamer(modifyModeRequiredData);

        // modifyModeRequiredData1 = modifyModeRequiredData;
        // modifyModeRequiredData  = modifyModeRequiredData1;
        modifyModeRequiredData1.modeRequiredData.check = new CheckCplex();
        // 初始化 DefectBlockSize
        DefectBlockSize defectSize = new DefectBlockSize();
        // 计算每一行的宽度
        defectSize.defectColumnWidth(modifyModeRequiredData1.modeRequiredData.heightPoints, modifyModeRequiredData1.rLayout.defPoints);
        modifyModeRequiredData1.modeRequiredData.defectWidth = defectSize.defectWidth;

        ArrayList<ModifyModeRequiredData> list = new ArrayList<>();
        list.add(modifyModeRequiredData);
        list.add(modifyModeRequiredData1);
        skyLineResultData.listModeRequiredData = list;
        return skyLineResultData;
    }

    // 随着高度的变化需要重新更新信息  操作
    public ArrayList<ModifyModeRequiredData> improveModel1(ModeRequiredData modeRequiredData, TargetData rLayout, ModeRequiredData useRequiredData) throws IloException, FileNotFoundException, GRBException {
        // modeRequiredData.LB = rLayout.oriSize[1];
        // 初始化目标块
        int[][] blockSize = ToolClass.copyTwoDim(rLayout.targetBlockSize);
        // DefPointData
        DefPointData discretePoints = new DefPointData();
        // 预处理
        PreprocessBlock preprocessBlock = new PreprocessBlock();
        // 计算宽度最小
        int minWidth = modeRequiredData.minWidth;
        //计算高度最小
        int minHeight = modeRequiredData.minHeight;
        // 处理：blockSize 和 oriSize[0]
        // 应该只用提升高度即可
        preprocessBlock.processBlock(minHeight, minWidth, rLayout.oriSize, rLayout.defPoints, blockSize);

        modeRequiredData.targetBlockSize = blockSize;
        // 得到Y方向离散点
        discretePoints.heightSet(rLayout.oriSize[1], minHeight, modeRequiredData.targetBlockSize, rLayout.defPoints);

        //        // 初始化 DefectBlockSize
//        DefectBlockSize defectSize = new DefectBlockSize();
//        // 计算每一行的宽度
//        defectSize.defectColumnWidth(discretePoints.heightPoints, rLayout.defPoints);

        modeRequiredData.updataInfo(discretePoints, rLayout);


        // modeRequiredData.exprListPrev.addAll(useRequiredData.exprList);
        // System.out.println("copy modeRequiredData.exprListPrev.size = " + modeRequiredData.exprListPrev.size());

        modeRequiredData.exprList = new ArrayList<>();
        modeRequiredData.exprListPrev = new ArrayList<>();
        modeRequiredData.filterHashSet = new HashSet<String>();
        modeRequiredData.hashSetMinInfeases = new HashSet<String>();
        modeRequiredData.hashSetNonInfeases = new HashSet<String>();
        modeRequiredData.hashSetAreaInfeases = new HashSet<String>();
        modeRequiredData.hashSetInfeases = new HashSet<String>();
        ;
        ModifyModeRequiredData modifyModeRequiredData = new ModifyModeRequiredData(modeRequiredData, rLayout);
        ModifyModeRequiredData modifyModeRequiredData1 = modifyModeRequiredData.reverseParamer(modifyModeRequiredData);
        modifyModeRequiredData1.modeRequiredData.check = new CheckCplex();
        // 初始化 DefectBlockSize
        DefectBlockSize defectSize = new DefectBlockSize();
        // 计算每一行的宽度
        defectSize.defectColumnWidth(modifyModeRequiredData1.modeRequiredData.heightPoints, modifyModeRequiredData1.rLayout.defPoints);
        modifyModeRequiredData1.modeRequiredData.defectWidth = defectSize.defectWidth;

        if(useRequiredData.isRotation){
            modifyModeRequiredData1.modeRequiredData.liftCutPrev.addAll(useRequiredData.liftCutPrev);
            List<ConserveSolution> conserveSolutionList = modifyModeRequiredData1.modeRequiredData.liftCutPrev;
            // 是旋转的
            for (int i = 0; i < conserveSolutionList.size(); i++) {
                ConserveSolution conserveSolutions = conserveSolutionList.get(i);
                // 验证解对于提升高度后的条带，是否依旧不可行
                if(CheckCplex.xCheck(conserveSolutions.divideSolution, modifyModeRequiredData1.modeRequiredData)){
                    System.out.println("原不可行解，提升高度后变为可行解！");
                }else{
                    modifyModeRequiredData1.modeRequiredData.exprListPrev.add(conserveSolutions.liftCut);
                }
            }
        }else{
            // 非旋转的，宽度是固定的，x-check不会变
            modifyModeRequiredData.modeRequiredData.exprListPrev.addAll(useRequiredData.exprList);
        }

        ArrayList<ModifyModeRequiredData> list = new ArrayList<>();
        list.add(modifyModeRequiredData);
        list.add(modifyModeRequiredData1);

        return list;
    }

    // 随着高度的变化需要重新更新信息  修改
    public void updataInfo(DefPointData defPointData, TargetData targetData) {
        heightPoints = defPointData.heightPoints; // 变化
        heightPlacedPoints = defPointData.heightPlacedPoints;
        System.out.println("----更新信息---");
        int y = 0;
        for (int i = 0; i < targetBlockSize.length; i++) {
            boolean exist = true;
            for (int j = 0; j < heightPoints.length; j++) {
                y = heightPoints[j];
                if (heightPlacedPoints[i][y]) {
                    exist = false;
                }
            }
            if (exist) {
                System.out.println("有的点没有离散点---" + i);
            }
        }
//        defectWidth = dBS.defectWidth;  // 变化

    }

    /**
     * @param rLayout
     * @return int[][]
     * @description 目标块集合；宽，长
     * @author hao
     * @date 2023/7/17 10:37
     */
    public static int[][] initBlocks(TargetData rLayout) {
        int[][] rec = new int[rLayout.targetNumber][2];
        int temp = 0;
        for (int i = 0; i < rLayout.targetBlockSize.length; i++) {
            for (int j = 0; j < rLayout.targetBlockSize[i][2]; j++) {
                rec[temp][0] = rLayout.targetBlockSize[i][0];
                rec[temp][1] = rLayout.targetBlockSize[i][1];
                temp++;
            }
        }
        return rec;
    }

    public int findLbIndex(int[] arr, int var){
        for (int i = 0; i < arr.length; i++) {
            if(arr[i] == var){
                return i;
            }
        }
        return 0;
    }
}























