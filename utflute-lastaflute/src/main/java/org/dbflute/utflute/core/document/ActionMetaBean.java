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
//TODO jflute class suffix naming. Bean or Document or none(Meta)  by p1us2er0 (2015/09/18)
public class ActionMetaBean {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** url. */
    private String url;
    /** class name. */
    private String className;
    /** class comment. */
    private String classCommnet;
    /** method mame. */
    private String methodName;
    /** method comment. */
    private String methodCommnet;
    /** annotation list. */
    private List<String> annotationList = DfCollectionUtil.newArrayList();
    /** form meta bean. */
    private TypeMetaBean formMetaBean;
    /** return Meta bean. */
    private TypeMetaBean returnMetaBean;

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassCommnet() {
        return classCommnet;
    }

    public void setClassCommnet(String classCommnet) {
        this.classCommnet = classCommnet;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodCommnet() {
        return methodCommnet;
    }

    public void setMethodCommnet(String methodCommnet) {
        this.methodCommnet = methodCommnet;
    }

    public List<String> getAnnotationList() {
        return annotationList;
    }

    public void setAnnotationList(List<String> annotationList) {
        this.annotationList = annotationList;
    }

    public TypeMetaBean getFormMetaBean() {
        return formMetaBean;
    }

    public void setFormMetaBean(TypeMetaBean formMetaBean) {
        this.formMetaBean = formMetaBean;
    }

    public TypeMetaBean getReturnMetaBean() {
        return returnMetaBean;
    }

    public void setReturnMetaBean(TypeMetaBean returnMetaBean) {
        this.returnMetaBean = returnMetaBean;
    }
}
