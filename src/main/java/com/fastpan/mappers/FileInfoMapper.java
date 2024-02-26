package com.fastpan.mappers;

import com.fastpan.entity.po.FileInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件信息 数据库操作接口
 */
public interface FileInfoMapper<T, P> extends BaseMapper<T, P> {

    /**
     * 根据FileIdAndUserId更新
     */
    Integer updateByFileIdAndUserId(@Param("bean") T t, @Param("fileId") String fileId, @Param("userId") String userId);


    /**
     * 根据FileIdAndUserId删除
     */
    Integer deleteByFileIdAndUserId(@Param("fileId") String fileId, @Param("userId") String userId);


    /**
     * 根据FileIdAndUserId获取对象
     */
    T selectByFileIdAndUserId(@Param("fileId") String fileId, @Param("userId") String userId);


    void updateFileStatusWithOldStatus(@Param("fileId") String fileId, @Param("userId") String userId, @Param("bean") T t,
                                       @Param("oldStatus") Integer oldStatus);

    /**
     * 修改文件删除状态的参数
     * @param filePidList
     * @param fileIdList
     * 事实上这两个参数同时只有一个起作用，且处理方式完全相同...只是含义有细微差别所以分成两个
     */
    void updateFileDelFlagBatch(@Param("bean") FileInfo fileInfo,
                                @Param("userId") String userId,
                                @Param("filePidList") List<String> filePidList,
                                @Param("fileIdList") List<String> fileIdList,
                                @Param("oldDelFlag") Integer oldDelFlag);

    // 彻底删除
    void delFileBatch(@Param("userId") String userId,
                      @Param("filePidList") List<String> filePidList,
                      @Param("fileIdList") List<String> fileIdList,
                      @Param("oldDelFlag") Integer oldDelFlag);

    Long selectUseSpace(@Param("userId") String userId);

    void deleteFileByUserId(@Param("userId") String userId);
}
