package com.fastpan.controller;

import com.fastpan.entity.constants.Constants;
import com.fastpan.entity.dto.SessionShareDto;
import com.fastpan.entity.dto.SessionWebUserDto;
import com.fastpan.entity.enums.ResponseCodeEnum;
import com.fastpan.entity.vo.PaginationResultVO;
import com.fastpan.entity.vo.ResponseVO;
import com.fastpan.utils.CopyTools;
import com.fastpan.utils.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;


public class ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(ABaseController.class);

    protected static final String STATUC_SUCCESS = "success";

    protected static final String STATUC_ERROR = "error";

    protected <T> ResponseVO getSuccessResponseVO(T t) {
        ResponseVO<T> responseVO = new ResponseVO<>();
        responseVO.setStatus(STATUC_SUCCESS);
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg());
        responseVO.setData(t);
        return responseVO;
    }

    //将S类的Page对象转换为T类的Page对象（后端向前端传VO时需要）
    //<S, T> 表示该方法接受两种类型的参数化类型。
    protected <S, T> PaginationResultVO<T> convert2PaginationVO(PaginationResultVO<S> result, Class<T> classz) {
        PaginationResultVO<T> resultVO = new PaginationResultVO<>();
        resultVO.setList(CopyTools.copyList(result.getList(), classz));
        resultVO.setPageNo(result.getPageNo());
        resultVO.setPageSize(result.getPageSize());
        resultVO.setPageTotal(result.getPageTotal());
        resultVO.setTotalCount(result.getTotalCount());
        return resultVO;
    }

    //从网页session中得到SessionWebUserDto对象
    protected SessionWebUserDto getUserInfoFromSession(HttpSession session) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        return sessionWebUserDto;
    }


    protected SessionShareDto getSessionShareFromSession(HttpSession session, String shareId) {
        SessionShareDto sessionShareDto = (SessionShareDto) session.getAttribute(Constants.SESSION_SHARE_KEY + shareId);
        return sessionShareDto;
    }

    //网页端读取文件（可以多了解一些）
    protected void readFile(HttpServletResponse response, String filePath) {
        // 检查路径格式是否合法
        if (!StringTools.pathIsOk(filePath)) {
            return;
        }
        // 初始化输出流对象，用于将文件内容写入HTTP响应
        OutputStream out = null;
        // 初始化输入流对象，用于读取指定路径的文件内容
        FileInputStream in = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return;
            }
            in = new FileInputStream(file);
            // 创建一个字节数组，作为缓冲区，用于存储每次读取的数据
            byte[] byteData = new byte[1024];
            // 和前端的连接手段：response，获取HttpServletResponse对象的输出流
            out = response.getOutputStream();
            // 循环写入HTTP响应
            int len = 0;
            while ((len = in.read(byteData)) != -1) {
                out.write(byteData, 0, len);
            }
            // 刷新输出流，确保所有数据都被发送到客户端
            out.flush();
        } catch (Exception e) {
            logger.error("读取文件异常", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    logger.error("IO异常", e);
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error("IO异常", e);
                }
            }
        }
    }
}
