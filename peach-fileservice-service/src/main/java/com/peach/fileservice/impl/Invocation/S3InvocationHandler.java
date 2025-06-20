package com.peach.fileservice.impl.Invocation;

import com.peach.common.util.StringUtil;
import com.peach.fileservice.common.constant.FileConstant;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * JDK 动态代理处理器：用于拦截方法调用，清洗参数中多余的路径斜杠。
 * 主要用于 S3 存储相关路径处理，自动规整连续的 / 为单个 /。
 *
 * 示例：
 *  - 入参路径为 "///folder//sub///file.txt"，将被转换为 "/folder/sub/file.txt"
 *
 * 匹配规则：
 *  - 参数类型为 String 且参数名中包含 "path" 或 "key"
 *
 * @param <T> 被代理的目标对象类型
 * @author Mr Shu
 * @version 1.0.0
 * @date 2025/6/20
 */
@Slf4j
public class S3InvocationHandler<T> implements InvocationHandler {

    private final T target;

    public S3InvocationHandler(T target) {
        this.target = target;
    }

    /**
     * JDK 动态代理调用处理器逻辑。
     * 拦截方法调用，对目标方法参数中包含 path 或 key 且为 String 类型的参数进行路径规整。
     *
     * @param proxy 代理对象（通常无用）
     * @param method 被调用的方法
     * @param args 方法参数
     * @return 原方法的执行结果
     * @throws Throwable 若原方法抛出异常
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (args != null && args.length > 0) {
            Parameter[] parameters = method.getParameters();
            Class<?>[] parameterTypes = method.getParameterTypes();

            for (int i = 0; i < args.length; i++) {
                Object paramValue = args[i];
                if (paramValue == null || !parameterTypes[i].equals(String.class)) {
                    continue;
                }

                String paramName = parameters[i].getName().toLowerCase();
                if (paramName.contains(FileConstant.CONSTANT_PATH) || paramName.contains(FileConstant.CONSTANT_KEY)) {
                    String cleanedValue = StringUtil.getStringValue(paramValue)
                            .replaceAll(FileConstant.SEPARATOR_REG, FileConstant.PATH_SEPARATOR);
                    args[i] = cleanedValue;
                }
            }
        }

        try {
            return method.invoke(target, args);
        } catch (Exception e) {
            log.error("方法调用失败：{}，参数：{}", method.getName(), args, e);
            throw e;
        }
    }
}
