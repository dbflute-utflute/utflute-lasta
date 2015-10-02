/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.dbflute.utflute.core.document;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.DfStringUtil;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.di.core.ComponentDef;
import org.lastaflute.di.core.LaContainer;
import org.lastaflute.di.core.factory.SingletonLaContainerFactory;
import org.lastaflute.web.Execute;
import org.lastaflute.web.UrlChain;
import org.lastaflute.web.path.ActionPathResolver;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.config.ModuleConfig;
import org.lastaflute.web.util.LaModuleConfigUtil;

/**
 * @author p1us2er0
 * @since 0.5.0-sp9 (2015/09/18 Friday)
 */
public class DocumentGenerator {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    /** source directory. */
    private static final String SRC_DIR = "src/main/java/";

    /** depth. */
    private static final int DEPTH = 4;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** source directory. */
    private final List<String> srcDirList;

    /** depth. */
    private final int depth;

    /** sourceParserReflector. */
    private final OptionalThing<SourceParserReflector> sourceParserReflector;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    // TODO jflute option of setting, constructor or method arguments(lamda).  by p1us2er0 (2015/09/18)
    public DocumentGenerator() {
        this.srcDirList = DfCollectionUtil.newArrayList();
        String commonDir = "../" + new File(".").getAbsoluteFile().getParentFile().getName().replaceAll("-.*", "-common") + "/" + SRC_DIR;
        if (new File(commonDir).exists()) {
            this.srcDirList.add(commonDir);
        }
        this.srcDirList.add(SRC_DIR);
        this.depth = DEPTH;
        this.sourceParserReflector = OptionalThing.of(new SourceParserReflectorFactory().reflector(srcDirList));
    }

    public DocumentGenerator(List<String> srcDirList) {
        this(srcDirList, DEPTH);
    }

    public DocumentGenerator(List<String> srcDirList, int depth) {
        this.srcDirList = srcDirList;
        this.depth = depth;
        this.sourceParserReflector = OptionalThing.of(new SourceParserReflectorFactory().reflector(srcDirList));
    }

    // ===================================================================================
    //                                                                         Action Meta
    //                                                                         ===========
    public void saveLastaDocMeta() {
        List<ActionDocMeta> actionDocMetaList = generateActionDocMetaList();
        Map<String, Object> lastaDocDetailMap = DfCollectionUtil.newLinkedHashMap();
        lastaDocDetailMap.put("actionDocMetaList", actionDocMetaList);
        String json = getJsonManager().toJson(lastaDocDetailMap);

        Path path = Paths.get(getLastaDocDir(), "lastadoc.json");
        if (!Files.exists(path.getParent())) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try (BufferedWriter bw = Files.newBufferedWriter(path, Charset.forName("UTF-8"))) {
            bw.write(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ActionDocMeta> generateActionDocMetaList() {
        List<String> actionComponentNameList = findActionComponentNameList();
        List<ActionDocMeta> list = DfCollectionUtil.newArrayList();
        ModuleConfig moduleConfig = LaModuleConfigUtil.getModuleConfig();
        actionComponentNameList.forEach(componentName -> {
            moduleConfig.findActionMapping(componentName).alwaysPresent(actionMapping -> {
                Class<?> actionClass = actionMapping.getActionDef().getComponentClass();
                List<Method> methodList = DfCollectionUtil.newArrayList();
                sourceParserReflector.ifPresent(sourceParserReflector -> {
                    methodList.addAll(sourceParserReflector.getMethodListOrderByDefinition(actionClass));
                });

                if (methodList.isEmpty()) {
                    methodList.addAll(Arrays.stream(actionClass.getMethods()).sorted(Comparator.comparing(method -> {
                        return method.getName();
                    })).collect(Collectors.toList()));
                }

                methodList.forEach(method -> {
                    if (method.getAnnotation(Execute.class) != null) {
                        ActionExecute actionExecute = actionMapping.getActionExecute(method);
                        if (actionExecute != null) {
                            list.add(createActionDocMeta(actionMapping.getActionExecute(method)));
                        }
                    }
                });
            });
        });

        return list;
    }

    protected String getLastaDocDir() {
        if (new File("./pom.xml").exists()) {
            return "./target/lastadoc/";
        }
        return "./build/lastadoc/";
    }

    protected List<String> findActionComponentNameList() {
        List<String> componentNameList = DfCollectionUtil.newArrayList();
        LaContainer container = SingletonLaContainerFactory.getContainer().getRoot();

        srcDirList.forEach(srcDir -> {
            if (Paths.get(srcDir).toFile().exists()) {
                try (Stream<Path> stream = Files.find(Paths.get(srcDir), Integer.MAX_VALUE, (path, attr) -> {
                    return path.toString().endsWith("Action.java");
                })) {
                    stream.forEach(path -> {
                        String className = DfStringUtil.substringFirstRear(path.toFile().getAbsolutePath(), srcDir);
                        if (className.startsWith("/")) {
                            className = className.substring(1);
                        }
                        className = DfStringUtil.substringLastFront(className, ".java").replace('/', '.');
                        Class<?> clazz = DfReflectionUtil.forName(className);

                        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                            return;
                        }

                        String componentName = container.getComponentDef(clazz).getComponentName();
                        if (componentName != null && !componentNameList.contains(componentName)) {
                            componentNameList.add(componentName);
                        }
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        IntStream.range(0, container.getComponentDefSize()).forEach(index -> {
            ComponentDef componentDef = container.getComponentDef(index);
            String componentName = componentDef.getComponentName();
            if (componentName.endsWith("Action") && !componentNameList.contains(componentName)) {
                componentNameList.add(componentDef.getComponentName());
            }
        });
        return componentNameList;
    }

    protected ActionDocMeta createActionDocMeta(ActionExecute execute) {
        Class<?> componentClass = execute.getActionMapping().getActionDef().getComponentClass();
        ActionDocMeta actionDocMeta = new ActionDocMeta();
        UrlChain urlChain = new UrlChain(componentClass);
        if (!"index".equals(execute.getUrlPattern())) {
            urlChain.moreUrl(execute.getUrlPattern());
        }

        actionDocMeta.setUrl(getActionPathResolver().toActionUrl(componentClass, urlChain));
        Method method = execute.getExecuteMethod();
        actionDocMeta.setTypeName(adjustmentTypeName(method.getDeclaringClass()));
        actionDocMeta.setSimpleTypeName(adjustmentSimpleTypeName(method.getDeclaringClass()));
        actionDocMeta.setMethodName(method.getName());

        List<Annotation> annotationList = DfCollectionUtil.newArrayList();
        annotationList.addAll(Arrays.asList(method.getDeclaringClass().getAnnotations()));
        annotationList.addAll(Arrays.asList(method.getAnnotations()));
        actionDocMeta.setAnnotationList(analyzeAnnotationList(annotationList));

        for (int i = 0; i < method.getParameters().length; i++) {
            Parameter parameter = method.getParameters()[i];
            StringBuilder builder = new StringBuilder();
            builder.append("{").append(parameter.getName()).append(":");
            builder.append(adjustmentSimpleTypeName(parameter.getParameterizedType())).append("}");
            actionDocMeta.setUrl(actionDocMeta.getUrl().replaceFirst("\\{\\}", builder.toString()));
        }

        execute.getFormMeta().ifPresent(formTypeDocMeta -> {
            actionDocMeta.setFormTypeDocMeta(new TypeDocMeta());
            formTypeDocMeta.getListFormParameterParameterizedType().ifPresent(type -> {
                actionDocMeta.getFormTypeDocMeta().setTypeName(adjustmentTypeName(type));
                actionDocMeta.getFormTypeDocMeta().setSimpleTypeName(adjustmentSimpleTypeName(type));
            }).orElse(() -> {
                actionDocMeta.getFormTypeDocMeta().setTypeName(adjustmentTypeName(formTypeDocMeta.getFormType()));
                actionDocMeta.getFormTypeDocMeta().setSimpleTypeName(adjustmentSimpleTypeName(formTypeDocMeta.getFormType()));
            });
            Class<?> formType = formTypeDocMeta.getListFormParameterGenericType().orElse(formTypeDocMeta.getFormType());
            actionDocMeta.getFormTypeDocMeta().setNestTypeDocMetaList(
                    createTypeDocMeta(actionDocMeta.getFormTypeDocMeta(), formType, DfCollectionUtil.newLinkedHashMap(), depth));
        });

        actionDocMeta.setReturnTypeDocMeta(analyzeReturnClass(method));

        sourceParserReflector.ifPresent(sourceParserReflector -> {
            sourceParserReflector.reflect(actionDocMeta, method);
        });

        return actionDocMeta;
    }

    protected List<TypeDocMeta> createTypeDocMeta(TypeDocMeta typeDocMeta, Class<?> clazz, Map<String, Class<?>> genericParameterTypesMap, int depth) {
        if (depth < 0) {
            return DfCollectionUtil.newArrayList();
        }

        return Arrays.asList(clazz.getFields()).stream().map(field -> {
            Class<?> genericClass = genericParameterTypesMap.get(field.getGenericType().getTypeName());
            Class<?> type = genericClass != null ? genericClass : field.getType();
            TypeDocMeta bean = new TypeDocMeta();
            bean.setName(field.getName());
            bean.setTypeName(adjustmentTypeName(type));
            bean.setSimpleTypeName(adjustmentSimpleTypeName(type));
            bean.setAnnotationList(analyzeAnnotationList(Arrays.asList(field.getAnnotations())));

            List<String> targetTypeSuffixNameList = getTargetTypeSuffixNameList();
            if (targetTypeSuffixNameList.stream().anyMatch(suffix -> type.getName().contains(suffix))) {
                bean.setNestTypeDocMetaList(createTypeDocMeta(bean, type, genericParameterTypesMap, depth - 1));
            } else if (targetTypeSuffixNameList.stream().anyMatch(suffix -> field.getGenericType().getTypeName().contains(suffix))) {
                Class<?> typeArgumentClass = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                bean.setNestTypeDocMetaList(createTypeDocMeta(bean, typeArgumentClass, genericParameterTypesMap, depth - 1));
                String typeName = bean.getTypeName();
                bean.setTypeName(typeName + "<" + adjustmentTypeName(typeArgumentClass) + ">");
                bean.setSimpleTypeName(typeName + "<" + adjustmentSimpleTypeName(typeArgumentClass) + ">");
            }
            sourceParserReflector.ifPresent(sourceParserReflector -> {
                sourceParserReflector.reflect(bean, clazz);
            });
            return bean;
        }).collect(Collectors.toList());
    }

    protected String adjustmentTypeName(Type type) {
        return type.getTypeName();
    }

    protected String adjustmentSimpleTypeName(Type type) {
        if (type instanceof Class<?>) {
            return ((Class<?>) type).getSimpleName();
        }
        // TODO adjustment
        return adjustmentTypeName(type).replaceAll("[a-z0-9]+\\.", "");
    }

    protected List<String> analyzeAnnotationList(List<Annotation> annotationList) {
        return annotationList.stream().map(annotation -> {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            String typeName = adjustmentSimpleTypeName(annotationType);

            Map<String, Object> methodMap = Arrays.stream(annotationType.getDeclaredMethods()).filter(method -> {
                Object value = DfReflectionUtil.invoke(method, annotation, (Object[]) null);
                Object defaultValue = method.getDefaultValue();
                if (Objects.equals(value, defaultValue)) {
                    return false;
                }
                if (method.getReturnType().isArray() && Arrays.equals((Object[]) value, (Object[]) defaultValue)) {
                    return false;
                }
                return true;
            }).collect(Collectors.toMap(key -> {
                return key.getName();
            } , value -> {
                Object data = DfReflectionUtil.invoke(value, annotation, (Object[]) null);
                if (data != null && data.getClass().isArray()) {
                    List<?> list = Arrays.asList((Object[]) data);
                    if (list.isEmpty()) {
                        return "";
                    }
                    data = list.stream().map(o -> {
                        return o instanceof Class<?> ? adjustmentSimpleTypeName(((Class<?>) o)) : o;
                    }).collect(Collectors.toList());
                }
                return data;
            } , (v1, v2) -> v1, LinkedHashMap::new));

            if (methodMap.isEmpty()) {
                return typeName;
            }
            return typeName + methodMap;
        }).collect(Collectors.toList());
    }

    protected TypeDocMeta analyzeReturnClass(Method method) {
        TypeDocMeta returnTypeDocMeta = new TypeDocMeta();
        returnTypeDocMeta.setTypeName(adjustmentTypeName(method.getGenericReturnType()));
        returnTypeDocMeta.setSimpleTypeName(adjustmentSimpleTypeName(method.getGenericReturnType()));
        Class<?> returnClass = DfReflectionUtil.getGenericFirstClass(method.getGenericReturnType());

        if (returnClass != null) {
            // TODO p1us2er0 optimisation (2015/09/30)
            Map<String, Class<?>> genericParameterTypesMap = DfCollectionUtil.newLinkedHashMap();
            Type[] parameterTypes = DfReflectionUtil.getGenericParameterTypes(method.getGenericReturnType());
            TypeVariable<?>[] typeVariables = returnClass.getTypeParameters();
            Arrays.stream(parameterTypes).forEach(parameterType -> {
                Type[] genericParameterTypes = DfReflectionUtil.getGenericParameterTypes(parameterTypes[0]);
                IntStream.range(0, typeVariables.length).forEach(index -> {
                    genericParameterTypesMap.put(typeVariables[index].getTypeName(), (Class<?>) genericParameterTypes[0]);
                });
            });

            if (List.class.isAssignableFrom(returnClass)) {
                try {
                    String JsonResponseName = JsonResponse.class.getSimpleName();
                    Matcher matcher =
                            Pattern.compile(".+<([^,]+)>").matcher(returnTypeDocMeta.getTypeName().replaceAll(JsonResponseName + "<(.*)>", "$1"));
                    if (matcher.matches()) {
                        returnClass = DfReflectionUtil.forName(matcher.group(1));
                    }
                } catch (RuntimeException ignore) {

                }
            }
            List<Class<? extends Object>> ignoreList = Arrays.asList(Void.class, Integer.class, Long.class, Byte.class, Map.class);
            if (returnClass != null && !ignoreList.contains(returnClass)) {
                returnTypeDocMeta.setNestTypeDocMetaList(createTypeDocMeta(returnTypeDocMeta, returnClass, genericParameterTypesMap, depth));
            }
        }
        return returnTypeDocMeta;
    }

    protected List<String> getTargetTypeSuffixNameList() {
        return DfCollectionUtil.newArrayList("Form", "Body", "Bean");
    }

    protected ActionPathResolver getActionPathResolver() {
        return SingletonLaContainerFactory.getContainer().getComponent(ActionPathResolver.class);
    }

    protected JsonManager getJsonManager() {
        return SingletonLaContainerFactory.getContainer().getComponent(JsonManager.class);
    }

    // ===================================================================================
    //                                                                     Action Property
    //                                                                     ===============
    public Map<String, Map<String, String>> generateActionPropertyNameMap(List<ActionDocMeta> actionDocMetaList) {
        Map<String, Map<String, String>> propertyNameMap = actionDocMetaList.stream().collect(Collectors.toMap(key -> {
            return key.getUrl().replaceAll("\\{.*", "").replaceAll("/$", "").replaceAll("/", "_");
        } , value -> {
            return convertPropertyNameMap("", value.getFormTypeDocMeta());
        } , (v1, v2) -> v1, TreeMap::new));
        return propertyNameMap;
    }

    protected Map<String, String> convertPropertyNameMap(String parentName, TypeDocMeta typeDocMeta) {
        if (typeDocMeta == null) {
            return DfCollectionUtil.newLinkedHashMap();
        }

        Map<String, String> propertyNameMap = DfCollectionUtil.newLinkedHashMap();

        String name = calculateName(parentName, typeDocMeta.getName(), typeDocMeta.getTypeName());
        if (DfStringUtil.is_NotNull_and_NotEmpty(name)) {
            propertyNameMap.put(name, "");
        }

        if (typeDocMeta.getNestTypeDocMetaList() != null) {
            typeDocMeta.getNestTypeDocMetaList().forEach(nestDocMeta -> {
                propertyNameMap.putAll(convertPropertyNameMap(name, nestDocMeta));
            });
        }

        return propertyNameMap;
    }

    protected String calculateName(String parentName, String name, String type) {
        if (DfStringUtil.is_Null_or_Empty(name)) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        if (DfStringUtil.is_NotNull_and_NotEmpty(parentName)) {
            builder.append(parentName + ".");
        }
        builder.append(name);
        if (type.startsWith("List")) {
            builder.append("[]");
        }

        return builder.toString();
    }
}
