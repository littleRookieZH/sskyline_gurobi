package main.java.exactsolution.onedimensionalcontiguousmodifygurobi;

import main.java.com.twodimension.TargetData;
import main.java.com.twodimensiondata.SkyLineResultData;

import java.io.*;


/**
 * @author xzbz
 * @create 2023-11-08 9:37
 */
public class ModifyModeRequiredData implements Serializable {
    ModeRequiredData modeRequiredData;
    TargetData rLayout;


    public ModifyModeRequiredData() {
    }

    public ModifyModeRequiredData(ModeRequiredData modeRequiredData, TargetData rLayout) {
        this.modeRequiredData = modeRequiredData;
        this.rLayout = rLayout;
    }

    public ModifyModeRequiredData reverseParamer(ModifyModeRequiredData modifyModeRequiredData) {
        // 翻转的
        ModifyModeRequiredData copyModify = deepCopy(modifyModeRequiredData);
        // 没有翻转的
        ModeRequiredData modeRequiredData2 = modifyModeRequiredData.modeRequiredData;
            ModeRequiredData modeRequiredData1 = copyModify.modeRequiredData;
            TargetData rLayout1 = copyModify.rLayout;
            modeRequiredData1.oriSize[0] = modeRequiredData2.oriSize[1];
            modeRequiredData1.oriSize[1] = modeRequiredData2.oriSize[0];
            // 这是预处理后的尺寸
            int[][] targetBlockSize = modeRequiredData1.targetBlockSize;
            for (int i = 0; i < targetBlockSize.length; i++) {
                targetBlockSize[i] = new int[]{targetBlockSize[i][1], targetBlockSize[i][0], targetBlockSize[i][2], targetBlockSize[i][3]};
            }
            modeRequiredData1.targetBlockSize = targetBlockSize;

            // 这是原尺寸
            int[][] blockSize = modeRequiredData1.blockSize;
            for (int i = 0; i < blockSize.length; i++) {
                blockSize[i] = new int[]{blockSize[i][1], blockSize[i][0], blockSize[i][2], blockSize[i][3]};
            }

            modeRequiredData1.blockSize = blockSize;
            //以右下为原点
            int[][] defectSize = modeRequiredData1.defectSize;
            for (int i = 0; i < defectSize.length; i++) {
                defectSize[i] = new int[]{defectSize[i][1], modeRequiredData1.oriSize[1] - defectSize[i][2], defectSize[i][3], modeRequiredData1.oriSize[1] - defectSize[i][0]};
            }


            //每一行缺陷块的总长度 --重新计算即可
            // 宽度：每一个离散点的具体可放置情况
            modeRequiredData1.widthPlacedPoints = modifyModeRequiredData.modeRequiredData.heightPlacedPoints;
            modeRequiredData1.heightPlacedPoints = modifyModeRequiredData.modeRequiredData.widthPlacedPoints;
            modeRequiredData1.widthPoints = modifyModeRequiredData.modeRequiredData.heightPoints;
            modeRequiredData1.heightPoints = modifyModeRequiredData.modeRequiredData.widthPoints;
            modeRequiredData1.minHeight = modifyModeRequiredData.modeRequiredData.minWidth;
            modeRequiredData1.minWidth = modifyModeRequiredData.modeRequiredData.minHeight;

            // TargetData交换
            TargetData rLayout2 = modifyModeRequiredData.rLayout;
            rLayout1.oriSize = new int[]{rLayout2.oriSize[1], rLayout2.oriSize[0]};
            for (int i = 0; i < rLayout1.targetSize.length; i++) {
                rLayout1.targetSize[i][2] = rLayout2.targetSize[i][3];
                rLayout1.targetSize[i][3] = rLayout2.targetSize[i][2];
            }

            rLayout1.defPoints = defectSize;

            for (int i = 0; i < rLayout1.defectiveBlocksSize.length; i++) {
                rLayout1.defectiveBlocksSize[i][0] = rLayout2.defectiveBlocksSize[i][1];
                rLayout1.defectiveBlocksSize[i][1] = rLayout2.defectiveBlocksSize[i][0];
            }

            for (int i = 0; i < defectSize.length; i++) {
                rLayout1.defectLowerLeft[i][0] = defectSize[i][0];
                rLayout1.defectLowerLeft[i][1] = defectSize[i][1];
            }

            for (int i = 0; i < defectSize.length; i++) {
                rLayout1.defectUpperRight[i][0] = defectSize[i][2];
                rLayout1.defectUpperRight[i][1] = defectSize[i][3];
            }

            for (int i = 0; i < rLayout2.targetBlockSize.length; i++) {
                rLayout1.targetBlockSize[i][0] = rLayout2.targetBlockSize[i][1];
                rLayout1.targetBlockSize[i][1] = rLayout2.targetBlockSize[i][0];
                rLayout1.targetBlockSize[i][2] = rLayout2.targetBlockSize[i][2];
                rLayout1.targetBlockSize[i][3] = rLayout2.targetBlockSize[i][3];
            }

        return copyModify;
    }
    // 深拷贝
    public ModifyModeRequiredData deepCopy(ModifyModeRequiredData modifyModeRequiredData){
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream stream = new ObjectOutputStream(outputStream);
            stream.writeObject(modifyModeRequiredData);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(outputStream.toByteArray());
            ObjectInputStream inputStream = new ObjectInputStream(byteArrayInputStream);
            return (ModifyModeRequiredData)inputStream.readObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
