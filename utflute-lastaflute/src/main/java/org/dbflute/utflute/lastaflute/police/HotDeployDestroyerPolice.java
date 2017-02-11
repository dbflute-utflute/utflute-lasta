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
package org.dbflute.utflute.lastaflute.police;

import java.io.File;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.utflute.core.filesystem.FilesystemPlayer;
import org.dbflute.utflute.core.policestory.javaclass.PoliceStoryJavaClassHandler;
import org.dbflute.util.Srl;
import org.lastaflute.di.naming.NamingConvention;

/**
 * @author jflute
 */
public class HotDeployDestroyerPolice implements PoliceStoryJavaClassHandler {

    protected final Function<Class<?>, Object> componentProvider;

    public HotDeployDestroyerPolice(Function<Class<?>, Object> componentProvider) {
        this.componentProvider = componentProvider;
    }

    public void handle(File srcFile, Class<?> clazz) {
        final NamingConvention namingConvention = (NamingConvention) componentProvider.apply(NamingConvention.class);
        final String[] rootPackageNames = namingConvention.getRootPackageNames();
        final List<String> rootPrefixList = Stream.of(rootPackageNames).map(name -> name + ".").collect(Collectors.toList());
        final String fqcn = clazz.getName();
        if (rootPrefixList.stream().anyMatch(prefix -> fqcn.startsWith(prefix))) {
            return;
        }
        new FilesystemPlayer().readLine(srcFile, "UTF-8", line -> {
            if (line.startsWith("import ")) {
                final String imported = extractImported(line);
                if (rootPrefixList.stream().anyMatch(prefix -> imported.startsWith(prefix))) {
                    throwHotDeployDestroyerException(clazz, imported);
                }
            }
        });
    }

    protected String extractImported(String line) {
        return Srl.substringFirstFront(Srl.ltrim(Srl.substringFirstRear(line, "import "), "static "), ";");
    }

    protected void throwHotDeployDestroyerException(Class<?> clazz, Object destroyer) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("HotDeploy destroyer is here.");
        br.addItem("Advice");
        br.addElement("Non smart deploy package cannot refer smart deploy package.");
        br.addElement("Make sure your references.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    'bizfw' package => 'app' package");
        br.addElement("    'dbflute' package => 'app' package");
        br.addElement("    'mylasta' package => 'app' package");
        br.addElement("  (o):");
        br.addElement("    'app' package => 'app' package");
        br.addElement("    'app' package => 'bizfw' package");
        br.addElement("    'app' package => 'dbflute' package");
        br.addElement("    'app' package => 'mylasta' package");
        br.addItem("Destroyer");
        br.addElement(clazz.getName());
        br.addItem("Destroyed");
        br.addElement(destroyer);
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg);
    }
}
