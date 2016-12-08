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
package org.dbflute.utflute.lastaflute.document.reflector;

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
import org.dbflute.utflute.lastaflute.document.meta.ActionDocMeta;
import org.dbflute.utflute.lastaflute.document.meta.TypeDocMeta;
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
 * @author jflute
 * @since 0.5.0-sp9 (2015/09/18 Friday)
 */
public class JavaparserSourceParserReflector implements SourceParserReflector {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final Pattern CLASS_METHOD_COMMENT_END_PATTERN = Pattern.compile("\\* (.+)[.。]?.*\r?\n");
    protected static final Pattern FIELD_COMMENT_END_PATTERN = Pattern.compile("/?\\*\\*? ?([^.。]+).* ?\\*");
    protected static final Pattern RETURN_STMT_PATTERN = Pattern.compile("^[^)]+\\)");

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** src dir list. (NotNull) */
    protected final List<String> srcDirList;

    /** compilationUnitMap. (NotNull) */
    protected final Map<Class<?>, CompilationUnit> compilationUnitMap = DfCollectionUtil.newHashMap();

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public JavaparserSourceParserReflector(List<String> srcDirList) {
        this.srcDirList = srcDirList;
    }

    // ===================================================================================
    //                                                                         Method List
    //                                                                         ===========
    @Override
    public List<Method> getMethodListOrderByDefinition(Class<?> clazz) {
        List<String> methodDeclarationList = DfCollectionUtil.newArrayList();
        parseClass(clazz).ifPresent(compilationUnit -> {
            VoidVisitorAdapter<Void> adapter = new VoidVisitorAdapter<Void>() {
                public void visit(final MethodDeclaration methodDeclaration, final Void arg) {
                    methodDeclarationList.add(methodDeclaration.getName());
                    super.visit(methodDeclaration, arg);
                }
            };
            adapter.visit(compilationUnit, null);
        });

        List<Method> methodList = Arrays.stream(clazz.getMethods()).sorted(Comparator.comparing(method -> {
            return methodDeclarationList.indexOf(method.getName());
        })).collect(Collectors.toList());

        return methodList;
    }

    // ===================================================================================
    //                                                                      Reflect Method
    //                                                                      ==============
    @Override
    public void reflect(ActionDocMeta meta, Method method) {
        parseClass(method.getDeclaringClass()).ifPresent(compilationUnit -> {
            List<String> parameterNameList = DfCollectionUtil.newArrayList();
            Map<String, List<String>> returnMap = DfCollectionUtil.newLinkedHashMap();
            VoidVisitorAdapter<ActionDocMeta> adapter = createMethodVoidVisitorAdapter(method, parameterNameList, returnMap);
            adapter.visit(compilationUnit, meta);
            List<String> descriptionList = DfCollectionUtil.newArrayList();
            Arrays.asList(meta.getTypeComment(), meta.getMethodComment()).forEach(comment -> {
                if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                    Matcher matcher = CLASS_METHOD_COMMENT_END_PATTERN.matcher(comment);
                    if (matcher.find()) {
                        descriptionList.add(matcher.group(1));
                    }
                }
            });
            if (!descriptionList.isEmpty()) {
                meta.setDescription(String.join(", ", descriptionList));
            }

            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                if (parameterNameList.size() > i) {
                    Parameter parameter = parameters[i];
                    meta.setUrl(meta.getUrl().replace("{" + parameter.getName() + ":", "{" + parameterNameList.get(i) + ":"));
                }
            }
            String methodName = method.getName();
            if (returnMap.containsKey(methodName) && !returnMap.get(methodName).isEmpty()) {
                meta.getReturnTypeDocMeta().setValue(String.join(",", returnMap.get(methodName)));
            }
        });
    }

    protected VoidVisitorAdapter<ActionDocMeta> createMethodVoidVisitorAdapter(Method method, List<String> parameterNameList,
            Map<String, List<String>> returnMap) {
        return new MethodVoidVisitorAdapter(method, parameterNameList, returnMap);
    }

    public class MethodVoidVisitorAdapter extends VoidVisitorAdapter<ActionDocMeta> {

        protected final Method method;
        protected final List<String> parameterNameList;
        protected final Map<String, List<String>> returnMap;

        public MethodVoidVisitorAdapter(Method method, List<String> parameterNameList, Map<String, List<String>> returnMap) {
            this.method = method;
            this.parameterNameList = parameterNameList;
            this.returnMap = returnMap;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, ActionDocMeta actionDocMeta) {
            String comment = adjustComment(classOrInterfaceDeclaration.getJavaDoc());
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

            String comment = adjustComment(methodDeclaration.getJavaDoc());
            if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                actionDocMeta.setMethodComment(comment);
            }
            parameterNameList.addAll(methodDeclaration.getParameters().stream().map(parameter -> {
                return parameter.getId().getName();
            }).collect(Collectors.toList()));

            methodDeclaration.accept(new VoidVisitorAdapter<ActionDocMeta>() {
                @Override
                public void visit(ReturnStmt returnStmt, ActionDocMeta actionDocMeta) {
                    prepareReturnStmt(methodDeclaration, returnStmt);
                    super.visit(returnStmt, actionDocMeta);
                }
            }, actionDocMeta);
            super.visit(methodDeclaration, actionDocMeta);
        }

        protected void prepareReturnStmt(MethodDeclaration methodDeclaration, ReturnStmt returnStmt) {
            if (returnStmt.getExpr() != null) {
                String returnStmtStr = returnStmt.getExpr().toStringWithoutComments();
                Matcher matcher = RETURN_STMT_PATTERN.matcher(returnStmtStr);
                if (!returnMap.containsKey(methodDeclaration.getName())) {
                    returnMap.put(methodDeclaration.getName(), DfCollectionUtil.newArrayList());
                }
                returnMap.get(methodDeclaration.getName()).add(matcher.find() ? matcher.group(0) : "##unanalyzable##");
            }
        }
    }

    // ===================================================================================
    //                                                                       Reflect Class
    //                                                                       =============
    @Override
    public void reflect(TypeDocMeta typeDocMeta, Class<?> clazz) {
        List<Class<?>> classList = DfCollectionUtil.newArrayList();
        for (Class<?> targetClass = clazz; targetClass != null; targetClass = targetClass.getSuperclass()) {
            classList.add(targetClass);
        }
        Collections.reverse(classList);
        classList.forEach(targetClass -> {
            parseClass(targetClass).ifPresent(compilationUnit -> {
                VoidVisitorAdapter<TypeDocMeta> adapter = createClassOrInterfaceVoidVisitorAdapter();
                adapter.visit(compilationUnit, typeDocMeta);
            });
        });
    }

    protected VoidVisitorAdapter<TypeDocMeta> createClassOrInterfaceVoidVisitorAdapter() {
        return new ClassOrInterfaceVoidVisitorAdapter();
    }

    public class ClassOrInterfaceVoidVisitorAdapter extends VoidVisitorAdapter<TypeDocMeta> {

        @Override
        public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, TypeDocMeta typeDocMeta) {
            prepareClassComment(classOrInterfaceDeclaration, typeDocMeta);
            super.visit(classOrInterfaceDeclaration, typeDocMeta);
        }

        protected void prepareClassComment(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, TypeDocMeta typeDocMeta) {
            if (DfStringUtil.is_Null_or_Empty(typeDocMeta.getComment())
                    && classOrInterfaceDeclaration.getName().equals(typeDocMeta.getSimpleTypeName())) {
                String comment = adjustComment(classOrInterfaceDeclaration.getJavaDoc());
                if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                    typeDocMeta.setComment(comment);
                    if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                        Matcher matcher = CLASS_METHOD_COMMENT_END_PATTERN.matcher(comment);
                        if (matcher.find()) {
                            typeDocMeta.setDescription(matcher.group(1));
                        }
                    }
                }
            }
        }

        @Override
        public void visit(FieldDeclaration fieldDeclaration, TypeDocMeta typeDocMeta) {
            prepareFieldComment(fieldDeclaration, typeDocMeta);
            super.visit(fieldDeclaration, typeDocMeta);
        }

        protected void prepareFieldComment(FieldDeclaration fieldDeclaration, TypeDocMeta typeDocMeta) {
            if (fieldDeclaration.getVariables().stream().anyMatch(variable -> variable.getId().getName().equals(typeDocMeta.getName()))) {
                String comment = adjustComment(fieldDeclaration.getJavaDoc());
                if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                    typeDocMeta.setComment(comment);
                    Matcher matcher = FIELD_COMMENT_END_PATTERN.matcher(saveFieldCommentSpecialExp(comment));
                    if (matcher.find()) {
                        String description = matcher.group(1);
                        typeDocMeta.setDescription(restoreFieldCommentSpecialExp(description));
                    }
                }
            }
        }

        protected String saveFieldCommentSpecialExp(String comment) {
            return comment.replace("e.g.", "$$edotgdot$$");
        }

        protected String restoreFieldCommentSpecialExp(String comment) {
            return comment.replace("$$edotgdot$$", "e.g.");
        }
    }

    // ===================================================================================
    //                                                                      Adjust Comment
    //                                                                      ==============
    protected String adjustComment(Comment comment) {
        if (comment == null || DfStringUtil.is_Null_or_Empty(comment.toString())) {
            return null;
        }
        return comment.toStringWithoutComments().replaceAll("\r?\n$", "").replaceAll("(\r?\n) {2,}", "$1 ");
    }

    // ===================================================================================
    //                                                                         Parse Class
    //                                                                         ===========
    protected OptionalThing<CompilationUnit> parseClass(Class<?> clazz) {
        if (compilationUnitMap.containsKey(clazz)) {
            return OptionalThing.of(compilationUnitMap.get(clazz));
        }

        for (String srcDir : srcDirList) {
            File file = new File(srcDir, clazz.getName().replace('.', File.separatorChar) + ".java");
            if (!file.exists()) {
                file = new File(srcDir, clazz.getName().replace('.', File.separatorChar).replaceAll("\\$.*", "") + ".java");
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
