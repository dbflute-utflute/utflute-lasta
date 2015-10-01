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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfReflectionUtil;
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
    public void reflect(ActionDocMeta bean, Method method, List<String> srcDirList) {
        List<String> parameterNameList = DfCollectionUtil.newArrayList();
        parseClass(method.getDeclaringClass(), srcDirList).ifPresent(compilationUnit -> {

            VoidVisitorAdapter<ActionDocMeta> voidVisitorAdapter = new VoidVisitorAdapter<ActionDocMeta>() {

                @Override
                public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, ActionDocMeta actionDocMeta) {
                    if (classOrInterfaceDeclaration.getComment() != null
                            && DfStringUtil.is_NotNull_and_NotEmpty(classOrInterfaceDeclaration.getComment().toString())) {
                        actionDocMeta.setTypeComment(classOrInterfaceDeclaration.getComment().toString());
                    }
                    super.visit(classOrInterfaceDeclaration, actionDocMeta);
                }

                @Override
                public void visit(MethodDeclaration methodDeclaration, ActionDocMeta actionDocMeta) {
                    if (methodDeclaration.getName().equals(actionDocMeta.getMethodName())) {
                        if (methodDeclaration.getComment() != null
                                && DfStringUtil.is_NotNull_and_NotEmpty(methodDeclaration.getComment().toString())) {
                            actionDocMeta.setMethodComment(methodDeclaration.getComment().toString());
                        }
                        parameterNameList.addAll(methodDeclaration.getParameters().stream().map(parameter -> parameter.getId().getName())
                                .collect(Collectors.toList()));
                    }
                    super.visit(methodDeclaration, actionDocMeta);
                }
            };
            voidVisitorAdapter.visit(compilationUnit, bean);
        });

        for (int i = 0; i < method.getParameters().length; i++) {
            if (parameterNameList.size() > i) {
                Parameter parameter = method.getParameters()[i];
                bean.setUrl(bean.getUrl().replace("{" + parameter.getName() + ":", "{" +parameterNameList.get(i) + ":"));
            }
        }
    }

    public void reflect(TypeDocMeta bean, Class<?> clazz, List<String> srcDirList) {

        List<Class<?>> classList = DfCollectionUtil.newArrayList();
        for (Class<?> targetClass = clazz; targetClass != null; targetClass = targetClass.getSuperclass()) {
            classList.add(targetClass);
        }
        Collections.reverse(classList);
        classList.forEach(targetClass -> {
            parseClass(targetClass, srcDirList).ifPresent(compilationUnit -> {
                VoidVisitorAdapter<TypeDocMeta> voidVisitorAdapter = new VoidVisitorAdapter<TypeDocMeta>() {
                    @Override
                    public void visit(FieldDeclaration fieldDeclaration, TypeDocMeta typeDocMeta) {
                        if (fieldDeclaration.getVariables().stream()
                                .anyMatch(variable -> variable.getId().getName().equals(typeDocMeta.getName()))) {
                            if (fieldDeclaration.getComment() != null
                                    && DfStringUtil.is_NotNull_and_NotEmpty(fieldDeclaration.getComment().toString())) {
                                typeDocMeta.setComment(fieldDeclaration.getComment().toString());
                            }
                        }
                        super.visit(fieldDeclaration, typeDocMeta);
                    }
                };

                voidVisitorAdapter.visit(compilationUnit, bean);
            });            
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
                file = new File(srcDir + clazz.getName().replaceAll("\\.", "/").replaceAll("\\$.*", "") + ".java");
                if (!file.exists()) {
                    continue;
                }
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
