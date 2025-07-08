package org.ohdsi.webapi.achilles.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.ohdsi.webapi.achilles.domain.AchillesCacheEntity;
import org.ohdsi.webapi.achilles.service.AchillesCacheService;
import org.ohdsi.webapi.source.Source;
import org.ohdsi.webapi.source.SourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.PathParam;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Aspect
@Component
public class AchillesCacheAspect {
    private static final Logger LOG = LoggerFactory.getLogger(AchillesCacheAspect.class);

    private final SourceRepository sourceRepository;
    private final ObjectMapper objectMapper;
    private final AchillesCacheService cacheService;

    public AchillesCacheAspect(SourceRepository sourceRepository,
                               ObjectMapper objectMapper,
                               AchillesCacheService cacheService) {
        this.sourceRepository = sourceRepository;
        this.objectMapper = objectMapper;
        this.cacheService = cacheService;
    }

    @Pointcut("@annotation(AchillesCache)")
    public void cachePointcut() {
    }

    @Around("cachePointcut()")
    public Object cache(ProceedingJoinPoint joinPoint) throws Throwable {
        String cacheName = getCacheName(joinPoint);
        Map<String, String> params = getParams(joinPoint);
        String sourceKey = params.get("sourceKey");
        
        // Add better logging for debugging
        if (sourceKey == null) {
            LOG.warn("sourceKey parameter is null. Available parameters: {}", params.keySet());
            LOG.debug("Method signature: {}", joinPoint.getSignature());
            LOG.debug("Method arguments: {}", java.util.Arrays.toString(joinPoint.getArgs()));
            // Fall back to proceeding without cache
            return joinPoint.proceed();
        }
        
        try {
            Source source = getSource(sourceKey);
            if (source == null) {
                LOG.warn("No source found for sourceKey: {}", sourceKey);
                // Fall back to proceeding without cache
                return joinPoint.proceed();
            }
            
            AchillesCacheEntity cacheEntity = cacheService.getCache(source, cacheName);
            if (Objects.isNull(cacheEntity)) {
                Object result = joinPoint.proceed();
                try {
                    cacheEntity = cacheService.createCache(source, cacheName, result);
                } catch (DataIntegrityViolationException e) {
                    // cache can be created during executing join point, try to get it again
                    cacheEntity = cacheService.getCache(source, cacheName);
                }
            }

            return objectMapper.readValue(cacheEntity.getCache(), getReturnType(joinPoint));
        } catch (Exception e) {
            LOG.error("exception during getting cache " + cacheName + " for source " + sourceKey, e);
            // ignore exception and call join point
            return joinPoint.proceed();
        }
    }

    private Class<?> getReturnType(ProceedingJoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        return ((MethodSignature) signature).getReturnType();
    }

    private Source getSource(String sourceKey) {
        return sourceRepository.findBySourceKey(sourceKey);
    }

    private String getAnnotationValue(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        return method.getAnnotation(AchillesCache.class).value();
    }

    private String getCacheName(JoinPoint joinPoint) {
        String cachePrefix = getAnnotationValue(joinPoint);
        Map<String, String> params = getParams(joinPoint);
        String paramNamePart = params.entrySet().stream()
                .filter(entry -> !"sourceKey".equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.joining("_"));
        return cachePrefix + (StringUtils.isEmpty(paramNamePart) ? "" : "_" + paramNamePart);
    }

    private Map<String, String> getParams(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] names = signature.getParameterNames();
        Object[] objects = joinPoint.getArgs();
        Method method = signature.getMethod();
        
        if(names == null || objects == null) {
            LOG.warn("Parameter names or values are null for method: {}", joinPoint.getSignature());
            // Try to extract from annotations if parameter names are not available
            return extractParamsFromAnnotations(method, objects);
        }
        
        if(names.length != objects.length) {
            LOG.warn("Parameter names and values length mismatch for method: {}. Names: {}, Values: {}", 
                    joinPoint.getSignature(), names.length, objects.length);
            return new HashMap<String, String>();
        }
        
        Map<String, String> params = IntStream.range(0, names.length)
                .boxed()
                .collect(Collectors.toMap(i -> names[i],
                        i -> objects[i] != null ? String.valueOf(objects[i]) : "null"));
        
        LOG.debug("Extracted parameters for method {}: {}", joinPoint.getSignature(), params);
        return params;
    }
    
    private Map<String, String> extractParamsFromAnnotations(Method method, Object[] objects) {
        Map<String, String> params = new HashMap<>();
        java.lang.annotation.Annotation[][] paramAnnotations = method.getParameterAnnotations();
        
        for (int i = 0; i < paramAnnotations.length && i < objects.length; i++) {
            for (java.lang.annotation.Annotation annotation : paramAnnotations[i]) {
                if (annotation instanceof jakarta.ws.rs.PathParam) {
                    jakarta.ws.rs.PathParam pathParam = (jakarta.ws.rs.PathParam) annotation;
                    String paramName = pathParam.value();
                    String paramValue = objects[i] != null ? String.valueOf(objects[i]) : "null";
                    params.put(paramName, paramValue);
                    LOG.debug("Extracted @PathParam {}: {}", paramName, paramValue);
                }
            }
        }
        
        return params;
    }
}
