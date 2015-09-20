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
public class TypeDocMeta {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** name. */
    private String name;
    /** comment. */
    private String comment;
    /** type. */
    private String type;
    /** annotation list. */
    private List<String> annotationList = DfCollectionUtil.newArrayList();
    /** nest meta bean list. */
    private List<TypeDocMeta> nestTypeDocMetaList = DfCollectionUtil.newArrayList();

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getAnnotationList() {
        return annotationList;
    }

    public void setAnnotationList(List<String> annotationList) {
        this.annotationList = annotationList;
    }

    public List<TypeDocMeta> getNestTypeDocMetaList() {
        return nestTypeDocMetaList;
    }

    public void setNestTypeDocMetaList(List<TypeDocMeta> nestTypeDocMetaList) {
        this.nestTypeDocMetaList = nestTypeDocMetaList;
    }
}
