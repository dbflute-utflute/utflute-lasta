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

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public List<ActionMetaBean> generateActionMetaBeanList() {
        List<String> actionComponentNameList = findActionComponentNameList();
        List<ActionMetaBean> list = DfCollectionUtil.newArrayList();
        ModuleConfig moduleConfig = LaModuleConfigUtil.getModuleConfig();
        actionComponentNameList.forEach(componentName -> {
            moduleConfig.findActionMapping(componentName).alwaysPresent(actionMapping -> {
                actionMapping.getExecuteMap().values().forEach(execute -> {
                    list.add(createActionMetaBean(execute));
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

    private ActionMetaBean createActionMetaBean(ActionExecute execute) {
        Class<?> componentClass = execute.getActionMapping().getActionDef().getComponentClass();
        ActionMetaBean actionMetaBean = new ActionMetaBean();
        UrlChain urlChain = new UrlChain(componentClass);
        if (!"index".equals(execute.getUrlPattern())) {
            urlChain.moreUrl(execute.getUrlPattern());
        }

        actionMetaBean.setUrl(getActionPathResolver().toActionUrl(componentClass, urlChain));
        Method method = execute.getExecuteMethod();
        actionMetaBean.setClassName(method.getDeclaringClass().getName());
        actionMetaBean.setMethodName(method.getName());

        actionMetaBean.setAnnotationList(Arrays.stream(method.getDeclaringClass().getAnnotations()).map(annotation -> {
            return adjustmentTypeName(annotation.annotationType());
        }).collect(Collectors.toList()));
        actionMetaBean.getAnnotationList().addAll(Arrays.stream(method.getAnnotations()).map(annotation -> {
            return adjustmentTypeName(annotation.annotationType());
        }).collect(Collectors.toList()));

        for (int i = 0; i < method.getParameters().length; i++) {
            Parameter parameter = method.getParameters()[i];
            StringBuilder builder = new StringBuilder();
            builder.append("{").append(parameter.getName()).append(":");
            builder.append(adjustmentTypeName(parameter.getParameterizedType())).append("}");
            actionMetaBean.setUrl(actionMetaBean.getUrl().replaceFirst("\\{\\}", builder.toString()));
        }

        if (sourceParserHandler != null) {
            sourceParserHandler.reflect(actionMetaBean, method, srcDirList);
        }

        execute.getFormMeta().ifPresent(formMetaBean -> {
            actionMetaBean.setFormMetaBean(new TypeMetaBean());
            formMetaBean.getListFormParameterParameterizedType().ifPresent(type -> {
                actionMetaBean.getFormMetaBean().setType(adjustmentTypeName(type));
            }).orElse(() -> {
                actionMetaBean.getFormMetaBean().setType(adjustmentTypeName(formMetaBean.getFormType()));
            });
            Class<?> formType = formMetaBean.getListFormParameterGenericType().orElse(formMetaBean.getFormType());
            actionMetaBean.getFormMetaBean().setNestMetaBeanList(createTypeMetaBean(actionMetaBean.getFormMetaBean(), formType, depth));
        });

        TypeMetaBean returnMetaBean = new TypeMetaBean();
        returnMetaBean.setType(adjustmentTypeName(method.getGenericReturnType()));
        Class<?> returnClass = DfReflectionUtil.getGenericFirstClass(method.getGenericReturnType());
        if (returnClass != null) {
            if (List.class.isAssignableFrom(returnClass)) {
                try {
                    String JsonResponseName = JsonResponse.class.getSimpleName();
                    Matcher matcher =
                            Pattern.compile(".+<([^,]+)>").matcher(returnMetaBean.getType().replaceAll(JsonResponseName + "<(.*)>", "$1"));
                    if (matcher.matches()) {
                        returnClass = DfReflectionUtil.forName(matcher.group(1));
                    }
                } catch (RuntimeException ignore) {

                }
            }

            List<Class<? extends Object>> ignoreList = Arrays.asList(Void.class, Integer.class, Long.class, Byte.class);
            if (returnClass != null && !ignoreList.contains(returnClass)) {
                returnMetaBean.setNestMetaBeanList(createTypeMetaBean(returnMetaBean, returnClass, depth));
            }
        }

        actionMetaBean.setReturnMetaBean(returnMetaBean);
        return actionMetaBean;
    }

    private List<TypeMetaBean> createTypeMetaBean(TypeMetaBean typeMetaBean, Class<?> clazz, int depth) {
        if (depth < 0) {
            return DfCollectionUtil.newArrayList();
        }

        return Arrays.asList(clazz.getFields()).stream().map(field -> {
            TypeMetaBean bean = new TypeMetaBean();
            bean.setName(field.getName());
            bean.setType(adjustmentTypeName(field.getType()));
            bean.setAnnotationList(Arrays.stream(field.getAnnotations()).map(annotation -> {
                return adjustmentTypeName(annotation.annotationType());
            }).collect(Collectors.toList()));

            List<String> targetTypeSuffixNameList = getTargetTypeSuffixNameList();
            if (targetTypeSuffixNameList.stream().anyMatch(suffix -> field.getType().getName().contains(suffix))) {
                bean.setNestMetaBeanList(createTypeMetaBean(bean, field.getType(), depth - 1));
            } else if (targetTypeSuffixNameList.stream().anyMatch(suffix -> field.getGenericType().getTypeName().contains(suffix))) {
                Class<?> typeArgumentClass = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                bean.setNestMetaBeanList(createTypeMetaBean(bean, typeArgumentClass, depth - 1));
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

    // ===================================================================================
    //                                                                     Action Property
    //                                                                     ===============
    public Map<String, Map<String, String>> generateActionPropertyNameMap(List<ActionMetaBean> actionMetaBeanList) {
        Map<String, Map<String, String>> propertyNameMap = actionMetaBeanList.stream().collect(Collectors.toMap(key -> {
            return key.getUrl().replaceAll("\\{.*", "").replaceAll("/$", "").replaceAll("/", "_");
        } , value -> {
            return convertPropertyNameMap("", value.getFormMetaBean());
        }));
        return propertyNameMap;
    }

    protected Map<String, String> convertPropertyNameMap(String parentName, TypeMetaBean typeMetaBean) {
        if (typeMetaBean == null) {
            return DfCollectionUtil.newLinkedHashMap();
        }

        Map<String, String> propertyNameMap = DfCollectionUtil.newLinkedHashMap();

        String name = calculateName(parentName, typeMetaBean.getName(), typeMetaBean.getType());
        if (DfStringUtil.is_NotNull_and_NotEmpty(name)) {
            propertyNameMap.put(name, "");
        }

        if (typeMetaBean.getNestMetaBeanList() != null) {
            typeMetaBean.getNestMetaBeanList().forEach(nestMetaBean -> {
                propertyNameMap.putAll(convertPropertyNameMap(name, nestMetaBean));
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
