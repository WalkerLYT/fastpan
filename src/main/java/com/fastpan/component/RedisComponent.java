package com.fastpan.component;

import com.fastpan.entity.constants.Constants;
import com.fastpan.entity.dto.DownloadFileDto;
import com.fastpan.entity.dto.SysSettingsDto;
import com.fastpan.entity.dto.UserSpaceDto;
import com.fastpan.entity.po.FileInfo;
import com.fastpan.entity.po.UserInfo;
import com.fastpan.entity.query.FileInfoQuery;
import com.fastpan.entity.query.UserInfoQuery;
import com.fastpan.mappers.FileInfoMapper;
import com.fastpan.mappers.UserInfoMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Redis
 * 目前存了网盘的系统设置类的数据
 */
@Component("redisComponent")
public class RedisComponent {

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Resource
    private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;

    /**
     * 获取系统设置类
     *
     * @return
     */
    public SysSettingsDto getSysSettingsDto() {
        SysSettingsDto sysSettingsDto = (SysSettingsDto) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        if (sysSettingsDto == null) {
            sysSettingsDto = new SysSettingsDto();
            redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingsDto);
        }
        return sysSettingsDto;
    }

    /**
     * 保存网盘设置
     *
     * @param sysSettingsDto
     */
    public void saveSysSettingsDto(SysSettingsDto sysSettingsDto) {
        redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingsDto);
    }

    public void saveDownloadCode(String code, DownloadFileDto downloadFileDto) {
        redisUtils.setex(Constants.REDIS_KEY_DOWNLOAD + code, downloadFileDto, Constants.REDIS_KEY_EXPIRES_FIVE_MIN);
    }

    public DownloadFileDto getDownloadCode(String code) {
        return (DownloadFileDto) redisUtils.get(Constants.REDIS_KEY_DOWNLOAD + code);
    }


    /**
     * 获取用户使用的空间
     *
     * @param userId
     * @return
     */
    public UserSpaceDto getUserSpaceUse(String userId) {
        UserSpaceDto spaceDto = (UserSpaceDto) redisUtils.get(Constants.REDIS_KEY_USER_SPACE_USE + userId);
        if (null == spaceDto) {
            spaceDto = new UserSpaceDto();
            Long useSpace = this.fileInfoMapper.selectUseSpace(userId);
            spaceDto.setUseSpace(useSpace);
            spaceDto.setTotalSpace(getSysSettingsDto().getUserInitUseSpace() * Constants.MB);
            redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE + userId, spaceDto, Constants.REDIS_KEY_EXPIRES_DAY);
        }
        return spaceDto;
    }

    /**
     * 保存已使用的空间
     *
     * @param userId
     */
    public void saveUserSpaceUse(String userId, UserSpaceDto userSpaceDto) {
        redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE + userId, userSpaceDto, Constants.REDIS_KEY_EXPIRES_DAY);
    }

    //将变化后的用户空间存入Redis
    public UserSpaceDto resetUserSpaceUse(String userId) {
        UserSpaceDto spaceDto = new UserSpaceDto();

        Long useSpace = this.fileInfoMapper.selectUseSpace(userId);
        spaceDto.setUseSpace(useSpace);

        UserInfo userInfo = this.userInfoMapper.selectByUserId(userId);
        spaceDto.setTotalSpace(userInfo.getTotalSpace());

        redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE + userId, spaceDto, Constants.REDIS_KEY_EXPIRES_DAY);
        return spaceDto;
    }

    //保存文件临时大小
    public void saveFileTempSize(String userId, String fileId, Long fileSize) {
        Long currentSize = getFileTempSize(userId, fileId);
        redisUtils.setex(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + fileId, currentSize + fileSize, Constants.REDIS_KEY_EXPIRES_ONE_HOUR);
    }

    public Long getFileTempSize(String userId, String fileId) {
        Long currentSize = getFileSizeFromRedis(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + fileId);
        return currentSize;
    }

    private Long getFileSizeFromRedis(String key) {
        Object sizeObj = redisUtils.get(key);
        if (sizeObj == null) {
            return 0L;
        }
        if (sizeObj instanceof Integer) {
            return ((Integer) sizeObj).longValue();
        } else if (sizeObj instanceof Long) {
            return (Long) sizeObj;
        }

        return 0L;
    }
}
