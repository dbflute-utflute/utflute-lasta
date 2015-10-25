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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

/**
 * @author p1us2er0
 * @since 0.5.0-sp9 (2015/09/18 Friday)
 */
public class JavaparserSourceParserReflector implements SourceParserReflector {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** src dir list. */
    private List<String> srcDirList;

    /** compilationUnitMap. */
    private Map<Class<?>, CompilationUnit> compilationUnitMap = DfCollectionUtil.newHashMap();

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public JavaparserSourceParserReflector(List<String> srcDirList) {
        this.srcDirList = srcDirList;
    }

    // ===================================================================================
    //                                                                             Reflect
    //                                                                             =======
    @Override
    public List<Method> getMethodListOrderByDefinition(Class<?> clazz) {
        // TODO adjustment
        List<String> methodDeclarationList = DfCollectionUtil.newArrayList();
        parseClass(clazz).ifPresent(compilationUnit -> {
            VoidVisitorAdapter<Void> voidVisitorAdapter = new VoidVisitorAdapter<Void>() {
                public void visit(final MethodDeclaration methodDeclaration, final Void arg) {
                    methodDeclarationList.add(methodDeclaration.getName());
                    super.visit(methodDeclaration, arg);
                }
            };
            voidVisitorAdapter.visit(compilationUnit, null);
        });

        List<Method> methodList = Arrays.stream(clazz.getMethods()).sorted(Comparator.comparing(method -> {
            return methodDeclarationList.indexOf(method.getName());
        })).collect(Collectors.toList());

        return methodList;
    }

    @Override
    public void reflect(ActionDocMeta meta, Method method) {
        parseClass(method.getDeclaringClass()).ifPresent(compilationUnit -> {
            List<String> parameterNameList = DfCollectionUtil.newArrayList();
            Map<String, List<String>> returnMap = DfCollectionUtil.newLinkedHashMap();
            VoidVisitorAdapter<ActionDocMeta> voidVisitorAdapter = new VoidVisitorAdapter<ActionDocMeta>() {

                @Override
                public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, ActionDocMeta actionDocMeta) {
                    String comment = adjustmentComment(classOrInterfaceDeclaration.getComment());
                    if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                        actionDocMeta.setTypeComment(comment);
                    }
                    super.visit(classOrInterfaceDeclaration, actionDocMeta);
                }

                @Override
                public void visit(MethodDeclaration methodDeclaration, ActionDocMeta actionDocMeta) {
                    if (!methodDeclaration.getName().equals(method.getName())) {
                        return;
                    }

                    String comment = adjustmentComment(methodDeclaration.getComment());
                    if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                        actionDocMeta.setMethodComment(comment);
                    }
                    parameterNameList.addAll(methodDeclaration.getParameters().stream().map(parameter -> {
                        return parameter.getId().getName();
                    }).collect(Collectors.toList()));

                    methodDeclaration.accept(new VoidVisitorAdapter<ActionDocMeta>() {

                        @Override
                        public void visit(ReturnStmt returnStmt, ActionDocMeta actionDocMeta) {
                            if (returnStmt.getExpr() != null) {
                                String returnStmtStr = returnStmt.getExpr().toStringWithoutComments();
                                Pattern pattern = Pattern.compile("^[^)]+\\)");
                                Matcher matcher = pattern.matcher(returnStmtStr);
                                if (!returnMap.containsKey(methodDeclaration.getName())) {
                                    returnMap.put(methodDeclaration.getName(), DfCollectionUtil.newArrayList());
                                }
                                returnMap.get(methodDeclaration.getName()).add(matcher.find() ? matcher.group(0) : "##unanalyzable##");
                            }
                            super.visit(returnStmt, actionDocMeta);
                        }
                    }, actionDocMeta);
                    super.visit(methodDeclaration, actionDocMeta);
                }
            };
            voidVisitorAdapter.visit(compilationUnit, meta);
            List<String> descriptionList = DfCollectionUtil.newArrayList();
            Arrays.asList(meta.getTypeComment(), meta.getMethodComment()).forEach(comment -> {
                if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                    Pattern pattern = Pattern.compile("\\* (.+)[.。]?.*\r?\n");
                    Matcher matcher = pattern.matcher(comment);
                    if (matcher.find()) {
                        descriptionList.add(matcher.group(1));
                    }
                }
            });
            if (!descriptionList.isEmpty()) {
                meta.setDescription(String.join(", ", descriptionList));
            }

            for (int i = 0; i < method.getParameters().length; i++) {
                if (parameterNameList.size() > i) {
                    Parameter parameter = method.getParameters()[i];
                    meta.setUrl(meta.getUrl().replace("{" + parameter.getName() + ":", "{" + parameterNameList.get(i) + ":"));
                }
            }
            if (returnMap.containsKey(method.getName()) && !returnMap.get(method.getName()).isEmpty()) {
                meta.getReturnTypeDocMeta().setValue(String.join(",", returnMap.get(method.getName())));
            }
        });
    }

    @Override
    public void reflect(TypeDocMeta typeDocMeta, Class<?> clazz) {
        List<Class<?>> classList = DfCollectionUtil.newArrayList();
        for (Class<?> targetClass = clazz; targetClass != null; targetClass = targetClass.getSuperclass()) {
            classList.add(targetClass);
        }
        Collections.reverse(classList);
        classList.forEach(targetClass -> {
            parseClass(targetClass).ifPresent(compilationUnit -> {
                VoidVisitorAdapter<TypeDocMeta> voidVisitorAdapter = new VoidVisitorAdapter<TypeDocMeta>() {

                    @Override
                    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, TypeDocMeta typeDocMeta) {
                        if (DfStringUtil.is_Null_or_Empty(typeDocMeta.getComment()) &&
                                classOrInterfaceDeclaration.getName().equals(typeDocMeta.getSimpleTypeName())) {
                            String comment = adjustmentComment(classOrInterfaceDeclaration.getComment());
                            if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                                typeDocMeta.setComment(comment);
                                if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                                    Pattern pattern = Pattern.compile("\\* (.+)[.。]?.*\r?\n");
                                    Matcher matcher = pattern.matcher(comment);
                                    if (matcher.find()) {
                                        typeDocMeta.setDescription(matcher.group(1));
                                    }
                                }
                            }
                        }
                        super.visit(classOrInterfaceDeclaration, typeDocMeta);
                    }

                    @Override
                    public void visit(FieldDeclaration fieldDeclaration, TypeDocMeta typeDocMeta) {
                        if (fieldDeclaration.getVariables().stream()
                                .anyMatch(variable -> variable.getId().getName().equals(typeDocMeta.getName()))) {
                            String comment = adjustmentComment(fieldDeclaration.getComment());
                            if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                                typeDocMeta.setComment(comment);
                                Pattern pattern = Pattern.compile("/?\\*\\*? ?([^.。]+).* ?\\*");
                                Matcher matcher = pattern.matcher(comment);
                                if (matcher.find()) {
                                    typeDocMeta.setDescription(matcher.group(1));
                                }
                            }
                        }
                        super.visit(fieldDeclaration, typeDocMeta);
                    }
                };

                voidVisitorAdapter.visit(compilationUnit, typeDocMeta);
            });
        });
    }

    protected String adjustmentComment(Comment comment) {
        if (comment == null || DfStringUtil.is_Null_or_Empty(comment.toString())) {
            return null;
        }
        return comment.toStringWithoutComments().replaceAll("\r?\n$", "").replaceAll("(\r?\n) {2,}", "$1 ");
    }

    // ===================================================================================
    //                                                                               parse
    //                                                                               =====
    protected OptionalThing<CompilationUnit> parseClass(Class<?> clazz) {
        if (compilationUnitMap.containsKey(clazz)) {
            return OptionalThing.of(compilationUnitMap.get(clazz));
        }

        for (String srcDir : srcDirList) {
            File file = new File(srcDir + clazz.getName().replace('.', File.separatorChar) + ".java");
            if (!file.exists()) {
                file = new File(srcDir + clazz.getName().replace('.', File.separatorChar).replaceAll("\\$.*", "") + ".java");
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
