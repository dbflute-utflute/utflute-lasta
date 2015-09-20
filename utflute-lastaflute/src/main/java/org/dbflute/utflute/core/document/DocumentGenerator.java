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
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.DfStringUtil;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.di.core.ComponentDef;
import org.lastaflute.di.core.LaContainer;
import org.lastaflute.di.core.factory.SingletonLaContainerFactory;
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

    /** sourceParserHandler. */
    private final SourceParserHandler sourceParserHandler;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    // TODO jflute option of setting, constructor or method arguments(lamda).  by p1us2er0 (2015/09/18)
    public DocumentGenerator() {
        this(DfCollectionUtil.newArrayList(SRC_DIR));
    }

    public DocumentGenerator(List<String> srcDirList) {
        this(srcDirList, DEPTH);
    }

    public DocumentGenerator(List<String> srcDirList, int depth) {
        this.srcDirList = srcDirList;
        this.depth = depth;
        this.sourceParserHandler = new SourceParserHandlerFactory().handler();
    }

    // ===================================================================================
    //                                                                         Action Meta
    //                                                                         ===========
    public void saveLastaDocMeta(String filePath) {
        List<ActionDocMeta> actionDocMetaList = generateActionDocMetaList();
        Map<String, Object> lastaDocDetailMap = DfCollectionUtil.newLinkedHashMap();
        lastaDocDetailMap.put("actionDocMetaList", actionDocMetaList);
        String json = getJsonManager().toJson(lastaDocDetailMap);

        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            try {
                Files.delete(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(path, StandardOpenOption.CREATE)) {
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
                actionMapping.getExecuteMap().values().forEach(execute -> {
                    list.add(createActionDocMeta(execute));
                });
            });
        });

        return list;
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
                        String className = DfStringUtil.substringFirstRear(path.toFile().getPath(), srcDir);
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

    private ActionDocMeta createActionDocMeta(ActionExecute execute) {
        Class<?> componentClass = execute.getActionMapping().getActionDef().getComponentClass();
        ActionDocMeta actionDocMeta = new ActionDocMeta();
        UrlChain urlChain = new UrlChain(componentClass);
        if (!"index".equals(execute.getUrlPattern())) {
            urlChain.moreUrl(execute.getUrlPattern());
        }

        actionDocMeta.setUrl(getActionPathResolver().toActionUrl(componentClass, urlChain));
        Method method = execute.getExecuteMethod();
        actionDocMeta.setClassName(method.getDeclaringClass().getName());
        actionDocMeta.setMethodName(method.getName());

        actionDocMeta.setAnnotationList(Arrays.stream(method.getDeclaringClass().getAnnotations()).map(annotation -> {
            return adjustmentTypeName(annotation.annotationType());
        }).collect(Collectors.toList()));
        actionDocMeta.getAnnotationList().addAll(Arrays.stream(method.getAnnotations()).map(annotation -> {
            return adjustmentTypeName(annotation.annotationType());
        }).collect(Collectors.toList()));

        for (int i = 0; i < method.getParameters().length; i++) {
            Parameter parameter = method.getParameters()[i];
            StringBuilder builder = new StringBuilder();
            builder.append("{").append(parameter.getName()).append(":");
            builder.append(adjustmentTypeName(parameter.getParameterizedType())).append("}");
            actionDocMeta.setUrl(actionDocMeta.getUrl().replaceFirst("\\{\\}", builder.toString()));
        }

        if (sourceParserHandler != null) {
            sourceParserHandler.reflect(actionDocMeta, method, srcDirList);
        }

        execute.getFormMeta().ifPresent(formTypeDocMeta -> {
            actionDocMeta.setFormTypeDocMeta(new TypeDocMeta());
            formTypeDocMeta.getListFormParameterParameterizedType().ifPresent(type -> {
                actionDocMeta.getFormTypeDocMeta().setType(adjustmentTypeName(type));
            }).orElse(() -> {
                actionDocMeta.getFormTypeDocMeta().setType(adjustmentTypeName(formTypeDocMeta.getFormType()));
            });
            Class<?> formType = formTypeDocMeta.getListFormParameterGenericType().orElse(formTypeDocMeta.getFormType());
            actionDocMeta.getFormTypeDocMeta().setNestTypeDocMetaList(createTypeDocMeta(actionDocMeta.getFormTypeDocMeta(), formType, depth));
        });

        TypeDocMeta returnTypeDocMeta = new TypeDocMeta();
        returnTypeDocMeta.setType(adjustmentTypeName(method.getGenericReturnType()));
        Class<?> returnClass = DfReflectionUtil.getGenericFirstClass(method.getGenericReturnType());
        if (returnClass != null) {
            if (List.class.isAssignableFrom(returnClass)) {
                try {
                    String JsonResponseName = JsonResponse.class.getSimpleName();
                    Matcher matcher =
                            Pattern.compile(".+<([^,]+)>").matcher(returnTypeDocMeta.getType().replaceAll(JsonResponseName + "<(.*)>", "$1"));
                    if (matcher.matches()) {
                        returnClass = DfReflectionUtil.forName(matcher.group(1));
                    }
                } catch (RuntimeException ignore) {

                }
            }

            List<Class<? extends Object>> ignoreList = Arrays.asList(Void.class, Integer.class, Long.class, Byte.class);
            if (returnClass != null && !ignoreList.contains(returnClass)) {
                returnTypeDocMeta.setNestTypeDocMetaList(createTypeDocMeta(returnTypeDocMeta, returnClass, depth));
            }
        }

        actionDocMeta.setReturnTypeDocMeta(returnTypeDocMeta);
        return actionDocMeta;
    }

    private List<TypeDocMeta> createTypeDocMeta(TypeDocMeta typeDocMeta, Class<?> clazz, int depth) {
        if (depth < 0) {
            return DfCollectionUtil.newArrayList();
        }

        return Arrays.asList(clazz.getFields()).stream().map(field -> {
            TypeDocMeta bean = new TypeDocMeta();
            bean.setName(field.getName());
            bean.setType(adjustmentTypeName(field.getType()));
            bean.setAnnotationList(Arrays.stream(field.getAnnotations()).map(annotation -> {
                return adjustmentTypeName(annotation.annotationType());
            }).collect(Collectors.toList()));

            List<String> targetTypeSuffixNameList = getTargetTypeSuffixNameList();
            if (targetTypeSuffixNameList.stream().anyMatch(suffix -> field.getType().getName().contains(suffix))) {
                bean.setNestTypeDocMetaList(createTypeDocMeta(bean, field.getType(), depth - 1));
            } else if (targetTypeSuffixNameList.stream().anyMatch(suffix -> field.getGenericType().getTypeName().contains(suffix))) {
                Class<?> typeArgumentClass = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                bean.setNestTypeDocMetaList(createTypeDocMeta(bean, typeArgumentClass, depth - 1));
                bean.setType(bean.getType() + "<" + typeArgumentClass.getName() + ">");
            }
            if (sourceParserHandler != null) {
                sourceParserHandler.reflect(bean, clazz, srcDirList);
            }
            return bean;
        }).collect(Collectors.toList());
    }

    protected String adjustmentTypeName(Type type) {
        String typeName = type.getTypeName();
        typeName = typeName.replaceAll("java\\.(lang|util|time)\\.", "");
        typeName = typeName.replaceAll("javax\\.validation\\.constraints\\.", "");
        typeName = typeName.replaceAll("javax\\.validation\\.", "");
        typeName = typeName.replaceAll("org\\.hibernate\\.validator\\.constraints\\.", "");
        typeName = typeName.replaceAll("org\\.dbflute\\.optional\\.OptionalThing<(.+)>", "$1\\?");
        typeName = typeName.replaceAll("org\\.lastaflute\\.web\\.(response|validation)\\.", "");
        typeName = typeName.replaceAll("org\\.lastaflute\\.web\\.", "");
        return typeName;
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
        }));
        return propertyNameMap;
    }

    protected Map<String, String> convertPropertyNameMap(String parentName, TypeDocMeta typeDocMeta) {
        if (typeDocMeta == null) {
            return DfCollectionUtil.newLinkedHashMap();
        }

        Map<String, String> propertyNameMap = DfCollectionUtil.newLinkedHashMap();

        String name = calculateName(parentName, typeDocMeta.getName(), typeDocMeta.getType());
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
