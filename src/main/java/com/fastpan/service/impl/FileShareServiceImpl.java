package com.fastpan.service.impl;

import com.fastpan.entity.constants.Constants;
import com.fastpan.entity.dto.SessionShareDto;
import com.fastpan.entity.enums.PageSize;
import com.fastpan.entity.enums.ResponseCodeEnum;
import com.fastpan.entity.enums.ShareValidTypeEnums;
import com.fastpan.entity.po.FileShare;
import com.fastpan.entity.query.FileShareQuery;
import com.fastpan.entity.query.SimplePage;
import com.fastpan.entity.vo.PaginationResultVO;
import com.fastpan.exception.BusinessException;
import com.fastpan.mappers.FileShareMapper;
import com.fastpan.service.FileShareService;
import com.fastpan.utils.DateUtil;
import com.fastpan.utils.StringTools;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;


/**
 * 分享信息 业务接口实现
 */
@Service("fileShareService")
public class FileShareServiceImpl implements FileShareService {

    @Resource
    private FileShareMapper<FileShare, FileShareQuery> fileShareMapper;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<FileShare> findListByParam(FileShareQuery param) {
        return this.fileShareMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(FileShareQuery param) {
        return this.fileShareMapper.selectCount(param);
    }

    /**
     * 分页查询方法（生成的）
     */
    @Override
    public PaginationResultVO<FileShare> findListByPage(FileShareQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<FileShare> list = this.findListByParam(param);
        PaginationResultVO<FileShare> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(FileShare bean) {
        return this.fileShareMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<FileShare> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.fileShareMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<FileShare> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.fileShareMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据ShareId获取对象
     */
    @Override
    public FileShare getFileShareByShareId(String shareId) {
        return this.fileShareMapper.selectByShareId(shareId);
    }

    /**
     * 根据ShareId修改
     */
    @Override
    public Integer updateFileShareByShareId(FileShare bean, String shareId) {
        return this.fileShareMapper.updateByShareId(bean, shareId);
    }

    /**
     * 根据ShareId删除
     */
    @Override
    public Integer deleteFileShareByShareId(String shareId) {
        return this.fileShareMapper.deleteByShareId(shareId);
    }

    @Override
    public void saveShare(FileShare share) {
        ShareValidTypeEnums typeEnum = ShareValidTypeEnums.getByType(share.getValidType());
        if (typeEnum == null) {
            // 前端传来的枚举状态不存在
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //永久有效 即数据库expireTime字段为null
        if (typeEnum != ShareValidTypeEnums.FOREVER) {
            // 1，3，7天
            share.setExpireTime(DateUtil.getAfterDate(typeEnum.getDays()));
        }
        Date curDate = new Date();
        share.setShareTime(curDate);
        if (StringTools.isEmpty(share.getCode())) {
            //前端没传验证码，就自定义验证码
            share.setCode(StringTools.getRandomString(Constants.LENGTH_5));
        }
        share.setShareId(StringTools.getRandomString(Constants.LENGTH_20));
        this.fileShareMapper.insert(share);
    }

    //取消分享
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFileShareBatch(String[] shareIdArray, String userId) {
        //直接从数据库中物理删除
        Integer count = this.fileShareMapper.deleteFileShareBatch(shareIdArray, userId);
        if (count != shareIdArray.length) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }

    /**
     * 检查提取码 及 更新浏览次数
     */
    @Override
    public SessionShareDto checkShareCode(String shareId, String code) {
        FileShare share = this.fileShareMapper.selectByShareId(shareId);
        if (share == null || (share.getExpireTime() != null && new Date().after(share.getExpireTime()))) {
            throw new BusinessException(ResponseCodeEnum.CODE_902);
        }
        if (!share.getCode().equals(code)) {
            throw new BusinessException("提取码错误");
        }

        //更新浏览次数（必须用具体的方法在数据库层面修改）（乐观锁）
        //错误的做法是在这里把count+1再存入数据库，会有并发问题（A在更新count中，B也来拿这个count，最后就只加了1）
        this.fileShareMapper.updateShareShowCount(shareId);

        // 提取码验证通过后，将
        SessionShareDto shareSessionDto = new SessionShareDto();
        shareSessionDto.setShareId(shareId);
        shareSessionDto.setShareUserId(share.getUserId());
        shareSessionDto.setFileId(share.getFileId());
        shareSessionDto.setExpireTime(share.getExpireTime());
        return shareSessionDto;
    }
}