package main.java.com.twodimensiondata;


import main.java.com.twodimension.SkyLine;
import main.java.com.twodimension.TargetData;
import main.java.exactsolution.onedimensionalcontiguousmodifygurobi.ModifyModeRequiredData;

import java.util.List;

/**
 * @author xzbz
 * @create 2023-11-21 15:06
 */
public class SkyLineResultData {
    public SkyLine skyLine;
    public TargetData targetData;
    public int[][] sortRule;
    // 记录当前方法的计算高度
    public int currentHeight;
    // 记录当前方法的检查高度
    public int check;

    public List<ModifyModeRequiredData> listModeRequiredData;
    public boolean isHeuristic;

    public SkyLineResultData() {
    }

    public SkyLineResultData(SkyLine skyLine, TargetData targetData) {
        this.skyLine = skyLine;
        this.targetData = targetData;
    }

    public SkyLineResultData(SkyLine skyLine, TargetData targetData, int[][] sortRule) {
        this.skyLine = skyLine;
        this.targetData = targetData;
        this.sortRule = sortRule;
        if(skyLine != null){
            this.currentHeight = skyLine.skyHeight;
        }
        this.check = targetData.checkHeight;
    }
}
