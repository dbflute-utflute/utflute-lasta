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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfStringUtil;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

/**
 * @author p1us2er0
 * @since 0.5.0-sp9 (2015/09/18 Friday)
 */
public class JavaparserSourceParserHandler implements SourceParserHandler {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** compilationUnitMap. */
    private Map<Class<?>, CompilationUnit> compilationUnitMap = DfCollectionUtil.newHashMap();

    // ===================================================================================
    //                                                                             Reflect
    //                                                                             =======
    public void reflect(ActionMetaBean bean, Method method, List<String> srcDirList) {
        List<String> parameterNameList = DfCollectionUtil.newArrayList();
        parseClass(method.getDeclaringClass(), srcDirList).ifPresent(compilationUnit -> {

            VoidVisitorAdapter<ActionMetaBean> voidVisitorAdapter = new VoidVisitorAdapter<ActionMetaBean>() {

                @Override
                public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, ActionMetaBean actionMetaBean) {
                    if (classOrInterfaceDeclaration.getComment() != null
                            && DfStringUtil.is_NotNull_and_NotEmpty(classOrInterfaceDeclaration.getComment().toString())) {
                        actionMetaBean.setClassCommnet(classOrInterfaceDeclaration.getComment().toString());
                    }
                    super.visit(classOrInterfaceDeclaration, actionMetaBean);
                }

                @Override
                public void visit(MethodDeclaration methodDeclaration, ActionMetaBean actionMetaBean) {
                    if (methodDeclaration.getName().equals(actionMetaBean.getMethodName())) {
                        if (methodDeclaration.getComment() != null
                                && DfStringUtil.is_NotNull_and_NotEmpty(methodDeclaration.getComment().toString())) {
                            actionMetaBean.setMethodCommnet(methodDeclaration.getComment().toString());
                        }
                        parameterNameList.addAll(methodDeclaration.getParameters().stream().map(parameter -> parameter.getId().getName())
                                .collect(Collectors.toList()));
                    }
                    super.visit(methodDeclaration, actionMetaBean);
                }
            };
            voidVisitorAdapter.visit(compilationUnit, bean);
        });

        for (int i = 0; i < method.getParameters().length; i++) {
            if (parameterNameList.size() > i) {
                Parameter parameter = method.getParameters()[i];
                bean.setUrl(bean.getUrl().replaceFirst("\\{" + parameter.getName() + ":", parameterNameList.get(i)));
            }
        }
    }

    public void reflect(TypeMetaBean bean, Class<?> clazz, List<String> srcDirList) {
        parseClass(clazz, srcDirList).ifPresent(compilationUnit -> {
            VoidVisitorAdapter<TypeMetaBean> voidVisitorAdapter = new VoidVisitorAdapter<TypeMetaBean>() {
                @Override
                public void visit(FieldDeclaration fieldDeclaration, TypeMetaBean typeMetaBean) {
                    if (fieldDeclaration.getVariables().stream()
                            .anyMatch(variable -> variable.getId().getName().equals(typeMetaBean.getName()))) {
                        if (fieldDeclaration.getComment() != null
                                && DfStringUtil.is_NotNull_and_NotEmpty(fieldDeclaration.getComment().toString())) {
                            typeMetaBean.setCommnet(fieldDeclaration.getComment().toString());
                        }
                    }
                    super.visit(fieldDeclaration, typeMetaBean);
                }
            };

            voidVisitorAdapter.visit(compilationUnit, bean);
        });
    }

    // ===================================================================================
    //                                                                               parse
    //                                                                               =====
    protected OptionalThing<CompilationUnit> parseClass(Class<?> clazz, List<String> srcDirList) {
        if (compilationUnitMap.containsKey(clazz)) {
            return OptionalThing.of(compilationUnitMap.get(clazz));
        }

        for (String srcDir : srcDirList) {
            File file = new File(srcDir + clazz.getName().replaceAll("\\.", "/") + ".java");
            if (!file.exists()) {
                continue;
            }

            try (FileInputStream in = new FileInputStream(file)) {
                CompilationUnit compilationUnit = JavaParser.parse(in);
                compilationUnitMap.put(clazz, compilationUnit);
                return OptionalThing.of(compilationUnit);
            } catch (ParseException e) {
                return OptionalThing.empty();
            } catch (IOException e) {
                return OptionalThing.empty();
            }
        }

        return OptionalThing.empty();
    }
}
