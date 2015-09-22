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

import java.util.List;

import org.dbflute.util.DfCollectionUtil;

/**
 * @author p1us2er0
 * @since 0.5.0-sp9 (2015/09/18 Friday)
 */
public class ActionDocMeta {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** url. */
    private String url;
    /** type name. */
    private String type;
    /** type comment. */
    private String typeComment;
    /** method mame. */
    private String methodName;
    /** method comment. */
    private String methodComment;
    /** annotation list. */
    private List<String> annotationList = DfCollectionUtil.newArrayList();
    /** form meta bean. */
    private TypeDocMeta formTypeDocMeta;
    /** return Meta bean. */
    private TypeDocMeta returnTypeDocMeta;

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTypeComment() {
        return typeComment;
    }

    public void setTypeComment(String typeComment) {
        this.typeComment = typeComment;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodComment() {
        return methodComment;
    }

    public void setMethodComment(String methodComment) {
        this.methodComment = methodComment;
    }

    public List<String> getAnnotationList() {
        return annotationList;
    }

    public void setAnnotationList(List<String> annotationList) {
        this.annotationList = annotationList;
    }

    public TypeDocMeta getFormTypeDocMeta() {
        return formTypeDocMeta;
    }

    public void setFormTypeDocMeta(TypeDocMeta formTypeDocMeta) {
        this.formTypeDocMeta = formTypeDocMeta;
    }

    public TypeDocMeta getReturnTypeDocMeta() {
        return returnTypeDocMeta;
    }

    public void setReturnTypeDocMeta(TypeDocMeta returnTypeDocMeta) {
        this.returnTypeDocMeta = returnTypeDocMeta;
    }
}