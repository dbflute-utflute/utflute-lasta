/*
 * Copyright 2014-2017 the original author or authors.
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
package org.dbflute.utflute.lastaflute.document.generator;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.dbflute.optional.OptionalThing;
import org.dbflute.utflute.lastaflute.document.meta.JobDocMeta;
import org.dbflute.utflute.lastaflute.document.meta.TypeDocMeta;
import org.dbflute.utflute.lastaflute.document.reflector.SourceParserReflector;
import org.lastaflute.di.core.factory.SingletonLaContainerFactory;
import org.lastaflute.job.LaJob;

/**
 * @author p1us2er0
 * @since 0.6.9 (2075/03/05 Sunday)
 */
public class JobDocumentGenerator extends BaseDocumentGenerator {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** source directory. (NotNull) */
    protected final List<String> srcDirList;

    /** depth. */
    protected int depth;

    /** sourceParserReflector. */
    protected final OptionalThing<SourceParserReflector> sourceParserReflector;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    protected JobDocumentGenerator(List<String> srcDirList, int depth, OptionalThing<SourceParserReflector> sourceParserReflector) {
        this.srcDirList = srcDirList;
        this.depth = depth;
        this.sourceParserReflector = sourceParserReflector;
    }

    // -----------------------------------------------------
    //                                    Generate Meta List
    //                                    ------------------
    public List<JobDocMeta> generateJobDocMetaList() {
        org.lastaflute.job.JobManager jobManager =
                SingletonLaContainerFactory.getContainer().getComponent(org.lastaflute.job.JobManager.class);
        List<JobDocMeta> metaList = jobManager.getJobList().stream().map(job -> {
            JobDocMeta jobDocMeta = new JobDocMeta();
            jobDocMeta.setJobKey(getNoException(() -> job.getJobKey().value()));
            jobDocMeta.setJobUnique(getNoException(() -> job.getJobUnique().map(jobUnique -> jobUnique.value()).orElse(null)));
            jobDocMeta.setJobTitle(getNoException(() -> job.getJobNote().flatMap(jobNote -> jobNote.getTitle()).orElse(null)));
            jobDocMeta.setJobDescription(getNoException(() -> job.getJobNote().flatMap(jobNote -> jobNote.getDesc()).orElse(null)));
            jobDocMeta.setCronExp(getNoException(() -> job.getCronExp().orElse(null)));
            Class<? extends LaJob> jobClass = getNoException(() -> job.getJobType());
            if (jobClass != null) {
                jobDocMeta.setTypeName(jobClass.getName());
                jobDocMeta.setSimpleTypeName(jobClass.getSimpleName());
                jobDocMeta.setFieldTypeDocMetaList(Arrays.stream(jobClass.getDeclaredFields()).map(field -> {
                    TypeDocMeta typeDocMeta = new TypeDocMeta();
                    typeDocMeta.setName(field.getName());
                    typeDocMeta.setType(field.getType());
                    typeDocMeta.setTypeName(adjustTypeName(field.getGenericType()));
                    typeDocMeta.setSimpleTypeName(adjustSimpleTypeName((field.getGenericType())));
                    typeDocMeta.setAnnotationTypeList(Arrays.asList(field.getAnnotations()));
                    typeDocMeta.setAnnotationList(analyzeAnnotationList(typeDocMeta.getAnnotationTypeList()));

                    sourceParserReflector.ifPresent(sourceParserReflector -> {
                        sourceParserReflector.reflect(typeDocMeta, field.getType());
                    });
                    return typeDocMeta;
                }).collect(Collectors.toList()));
                jobDocMeta.setMethodName("run");
                sourceParserReflector.ifPresent(sourceParserReflector -> {
                    sourceParserReflector.reflect(jobDocMeta, jobClass);
                });
            }
            jobDocMeta.setParams(getNoException(() -> job.getParamsSupplier().map(paramsSupplier -> paramsSupplier.supply()).orElse(null)));
            jobDocMeta.setNoticeLogLevel(getNoException(() -> job.getNoticeLogLevel().name()));
            jobDocMeta.setConcurrentExec(getNoException(() -> job.getConcurrentExec().name()));
            jobDocMeta.setTriggeredJobKeyList(getNoException(() -> job.getTriggeredJobKeyList().stream()
                    .map(triggeredJobKey -> triggeredJobKey.value()).collect(Collectors.toList())));

            return jobDocMeta;
        }).collect(Collectors.toList());
        return metaList;
    }

    protected <T extends Object> T getNoException(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Throwable t) {
            return null;
        }
    }
}