package com.fastpan.aspect;

import com.fastpan.annotation.GlobalInterceptor;
import com.fastpan.annotation.VerifyParam;
import com.fastpan.entity.config.AppConfig;
import com.fastpan.entity.constants.Constants;
import com.fastpan.entity.dto.SessionWebUserDto;
import com.fastpan.entity.enums.ResponseCodeEnum;
import com.fastpan.entity.po.UserInfo;
import com.fastpan.entity.query.UserInfoQuery;
import com.fastpan.exception.BusinessException;
import com.fastpan.service.UserInfoService;
import com.fastpan.utils.StringTools;
import com.fastpan.utils.VerifyUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

/**
 * 面向切面编程（AOP）
 */
@Component("operationAspect")
@Aspect
public class GlobalOperationAspect {

    private static Logger logger = LoggerFactory.getLogger(GlobalOperationAspect.class);
    private static final String TYPE_STRING = "java.lang.String";
    private static final String TYPE_INTEGER = "java.lang.Integer";
    private static final String TYPE_LONG = "java.lang.Long";

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private AppConfig appConfig;


    // 定义切入点（有“@GlobalInterceptor”的方法），而requestInterceptor方法就充当了所需切入的方法的位置，借尸还魂
    @Pointcut("@annotation(com.fastpan.annotation.GlobalInterceptor)")
    private void requestInterceptor() {     //无需有东西，就是个空壳，替死鬼
    }

    /**
     * 在requestInterceptor方法前
     * JoinPoint为执行方法的某个时间点，可以该时间点的状态里面提取所需的东西
     * 利用了反射获取方法的信息
     */
    @Before("requestInterceptor()")
    public void interceptorDo(JoinPoint point) throws BusinessException {
        try {
            // 获取当前连接点的目标对象（即被代理的对象）
            Object target = point.getTarget();
            // 获取方法执行时传入的参数数组
            Object[] arguments = point.getArgs();
            // 获取方法名
            String methodName = point.getSignature().getName();
            // 获取当前连接点对应方法的参数类型数组
            Class<?>[] parameterTypes = ((MethodSignature) point.getSignature()).getMethod().getParameterTypes();
            // 通过方法名与参数类型数组，来准确地定位到所需的方法（防止多态或者其他情况）
            Method method = target.getClass().getMethod(methodName, parameterTypes);

            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);
            if (null == interceptor) {
                return;
            }
            // 校验登录
            if (interceptor.checkLogin() || interceptor.checkAdmin()) {
                checkLogin(interceptor.checkAdmin());
            }
            // 校验参数
            if (interceptor.checkParams()) {
                validateParams(method, arguments);
            }
        } catch (BusinessException e) {
            logger.error("全局拦截器异常", e);
            throw e;
        } catch (Exception e) {
            logger.error("全局拦截器异常", e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        } catch (Throwable e) {
            logger.error("全局拦截器异常", e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }
    }


    //校验登录
    private void checkLogin(Boolean checkAdmin) {
        //在类中获取session对象（全局方法，因为此处非controller，没法直接调出session）
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        SessionWebUserDto sessionUser = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        // 暂时不管他
        if (sessionUser == null && appConfig.getDev() != null && appConfig.getDev()) {
            List<UserInfo> userInfoList = userInfoService.findListByParam(new UserInfoQuery());
            if (!userInfoList.isEmpty()) {
                UserInfo userInfo = userInfoList.get(0);
                sessionUser = new SessionWebUserDto();
                sessionUser.setUserId(userInfo.getUserId());
                sessionUser.setNickName(userInfo.getNickName());
                sessionUser.setAdmin(true);
                session.setAttribute(Constants.SESSION_KEY, sessionUser);
            }
        }
        // 若查到的user为空，即没登录，就抛出异常，前端弹回登录界面
        if (sessionUser == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        // 要查管理员，且他不是超级管理员
        if (checkAdmin && !sessionUser.getAdmin()) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
    }


    private void validateParams(Method m, Object[] arguments) throws BusinessException {
        Parameter[] parameters = m.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object value = arguments[i];
            //获取每个参数旁边的@VerifyParam注解，如果没打标记就跳过
            VerifyParam verifyParam = parameter.getAnnotation(VerifyParam.class);
            if (verifyParam == null) {
                continue;
            }
            //如果传递的是基本数据类型
            if (TYPE_STRING.equals(parameter.getParameterizedType().getTypeName()) || TYPE_LONG.equals(parameter.getParameterizedType().getTypeName()) || TYPE_INTEGER.equals(parameter.getParameterizedType().getTypeName())) {
                checkValue(value, verifyParam);
            }
            //如果传递的是对象
            else {
                checkObjValue(parameter, value);
            }
        }
    }

    /**
     * 校验参数
     * 要把VerifyParam对象传过来，因为里面对参数的限制条件在这里面
     */
    private void checkValue(Object value, VerifyParam verifyParam) throws BusinessException {
        Boolean isEmpty = (value == null || StringTools.isEmpty(value.toString()));
        Integer length = (value == null ? 0 : value.toString().length());

        // 校验空：如果value为空且verifyParam要求不能为空，则抛出异常
        if (isEmpty && verifyParam.required()) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 校验长度：如果value不为空但其长度不在verifyParam指定的范围内，则抛出异常
        if (!isEmpty && (verifyParam.max() != -1 && verifyParam.max() < length || verifyParam.min() != -1 && verifyParam.min() > length)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 校验正则：如果value不为空、verifyParam中有非空正则表达式但value不满足该正则表达式，则抛出异常
        if (!isEmpty && !StringTools.isEmpty(verifyParam.regex().getRegex()) && !VerifyUtils.verify(verifyParam.regex(), String.valueOf(value))) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }

    private void checkObjValue(Parameter parameter, Object value) {
        try {
            String typeName = parameter.getParameterizedType().getTypeName();
            Class classz = Class.forName(typeName);
            Field[] fields = classz.getDeclaredFields();
            for (Field field : fields) {
                VerifyParam fieldVerifyParam = field.getAnnotation(VerifyParam.class);
                if (fieldVerifyParam == null) {
                    continue;
                }
                field.setAccessible(true);
                Object resultValue = field.get(value);
                checkValue(resultValue, fieldVerifyParam);
            }
        } catch (BusinessException e) {
            logger.error("校验参数失败", e);
            throw e;
        } catch (Exception e) {
            logger.error("校验参数失败", e);
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }
}