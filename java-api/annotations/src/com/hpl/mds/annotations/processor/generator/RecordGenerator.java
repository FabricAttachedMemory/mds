/*
 *
 *  Managed Data Structures
 *  Copyright © 2016 Hewlett Packard Enterprise Development Company LP.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  As an exception, the copyright holders of this Library grant you permission
 *  to (i) compile an Application with the Library, and (ii) distribute the 
 *  Application containing code generated by the Library and added to the 
 *  Application during this compilation process under terms of your choice, 
 *  provided you also meet the terms and conditions of the Application license.
 *
 */

package com.hpl.mds.annotations.processor.generator;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;

import javax.annotation.processing.Messager;

import org.stringtemplate.v4.AutoIndentWriter;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import com.hpl.mds.annotations.processor.RecordInfo;

/**
 * Generates the source code of managed records
 * 
 * @author Abraham Alcantara
 */
public class RecordGenerator {

    /**
     * Path to the template group for code rendering
     */
    private static final String TEMPLATE_MAIN_GROUP = "template/MainGroup.stg";

    /**
     * Name of the template to generate managed records
     */
    public static final String TEMPLATE_MANAGED_RECORD = "ManagedRecord";

    private final DataTypeRenderer dataTypeRenderer = new DataTypeRenderer();

    /**
     * Reference to string template group for rendering
     */
    private final STGroup stGroup;

    /**
     * To display localized messages to the user
     */
    private final Messager messager;

    /**
     * @param messager
     *            to display error messages to the user
     */
    public RecordGenerator(Messager messager) {
        stGroup = new STGroupFile(TEMPLATE_MAIN_GROUP);
        stGroup.setListener(new STErrorListenerImpl());
        this.messager = messager;
    }

    public boolean noTemplates() {
        return stGroup.getInstanceOf(RecordGenerator.TEMPLATE_MANAGED_RECORD) == null;
    }

    /**
     * Generates and writes the source code of a managed record from a
     * {@link RecordInfo}
     * 
     * @param writer
     *            to write the resulting render
     * @param recordInfo
     *            information of the managed record to generate
     * @throws IOException
     *             if an exception occurred while writing
     */
    public void generate(Writer writer, RecordInfo recordInfo) throws IOException {
        ST recordTemplate = stGroup.getInstanceOf(TEMPLATE_MANAGED_RECORD);
        assert recordTemplate != null; // template should always exist
        recordTemplate.add("pkg", recordInfo.getPkg());
        recordTemplate.add("simple_name", recordInfo.getSimpleName());
        recordTemplate.add("type_name", recordInfo.getTypeName());
        recordTemplate.add("parent", recordInfo.getParent());
        recordTemplate.add("superInterfaces", recordInfo.getSuperInterfaces());
        if (recordInfo.isAbstract()) {
            recordTemplate.add("implModifier", "abstract");
        }
        addFieldsAndMethods(recordTemplate, recordInfo);
        recordTemplate.write(new AutoIndentWriter(writer));
    }

    /**
     * Add fields and methods including constructors to the managed record
     * 
     * @param recordTemplate
     */
    private void addFieldsAndMethods(ST recordTemplate, RecordInfo recordInfo) {

        FieldRenderer fieldRenderer = new FieldRenderer(dataTypeRenderer, recordInfo.getFields(),
                recordInfo.getSimpleName(), stGroup, recordTemplate);
        fieldRenderer.render();
        MethodRenderer methodRenderer = new MethodRenderer(messager, dataTypeRenderer, recordInfo, stGroup,
                recordTemplate);
        methodRenderer.render();

        // merge generated public, protected and private methods

        List<String> privateMethods = merge(methodRenderer.getPrivateMethods(), fieldRenderer.getPrivateMethods());
        List<String> protectedMethods = merge(methodRenderer.getProtectedMethods(),
                fieldRenderer.getProtectedMethods());
        List<String> publicMethods = merge(methodRenderer.getPublicMethods(), fieldRenderer.getPublicMethods());

        // add methods to template
        recordTemplate.add("privateMethods", privateMethods);
        recordTemplate.add("protectedMethods", protectedMethods);
        recordTemplate.add("publicMethods", publicMethods);
    }

    /**
     * @param methods1
     * @param methods2
     * @return the merge of both lists
     */
    private List<String> merge(List<String> methods1, List<String> methods2) {

        if (!methods1.isEmpty()) {
            methods1.addAll(methods2);
            return methods1;
        }

        if (!methods2.isEmpty()) {
            methods2.addAll(methods1);
            return methods2;
        }

        // if both are empty
        return Collections.emptyList();
    }

}