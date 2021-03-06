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

package com.hpl.mds.annotations.processor.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

import com.hpl.mds.annotations.Abstract;
import com.hpl.mds.annotations.Emitted;
import com.hpl.mds.annotations.RecordSchema;
import com.hpl.mds.annotations.Static;
import com.hpl.mds.annotations.TypeName;
import com.hpl.mds.annotations.processor.RecordInfo;
import com.hpl.mds.annotations.processor.RecordInfo.DataType;
import com.hpl.mds.annotations.processor.RecordInfo.MethodInfo;
import com.hpl.mds.annotations.processor.RecordInfo.VarInfo;
import com.hpl.mds.annotations.processor.RecordInfo.Visibility;

/**
 * Parser for the schema's content including fields, instance, static and
 * abstract methods and constructors.
 * 
 * The parsed information is retrieved in the form of a {@link RecordInfo}
 * 
 * @author Abraham Alcantara
 */
public class SchemaParser {

    private static final String STATIC_METHOD = "StaticMethod";
    private static final String PRIVATE = "Private";
    private static final String PKG_DELIMITER = ".";

    /**
     * To log messages for debugging purposes
     */
    private static final Logger LOGGER = Logger.getLogger(MethodParser.class.getName());

    /**
     * Utilities for source code types
     */
    private final Types typeUtil;

    /**
     * To display localized messages to the user
     */
    private final Messager messager;

    /*
     * Parsers to extract specific data
     */
    private final DataTypeParser dataTypeParser;
    private final FieldParser fieldParser;
    private final GetterNameParser getterNameParser;
    private final MethodParser methodParser;
    private final ParadigmParser paradigmParser;
    private final VisibilitiesParser visibilitiesParser;

    /**
     * Record schema information context
     */
    private SchemaContext schemaContext;

    public SchemaParser(Messager messager, Elements elements, Types types, VisibilitiesParser visibilitiesParser,
            GetterNameParser getterNameParser, Set<String> managedRecords) {
        this.messager = messager;
        typeUtil = types;
        this.visibilitiesParser = visibilitiesParser;
        this.getterNameParser = getterNameParser;
        dataTypeParser = new DataTypeParser(types, elements, managedRecords);
        fieldParser = new FieldParser(messager, visibilitiesParser, getterNameParser, dataTypeParser);
        methodParser = new MethodParser(dataTypeParser, messager);
        paradigmParser = new ParadigmParser(messager, elements, types, visibilitiesParser, getterNameParser);
    }

    /**
     * Parses a record schema
     * 
     * @param schema
     *            from which the context will be generated
     * @param recordName
     *            name of the record of the given schema
     * @param parent
     *            record information of the parent schema
     * @return a {@link RecordInfo} that represents the schema's information
     */
    public RecordInfo parse(Element schema, String recordName, RecordInfo parent) {
        this.schemaContext = getSchemaContext(schema, recordName, parent);
        RecordInfo recordInfo = getRecordInfo(schema, recordName, parent);
        parserMembers(recordInfo, schema);
        return recordInfo;
    }

    /**
     * 
     * @param schema
     *            from which the context will be generated
     * @param recordName
     *            name of the record of the given schema
     * @param parent
     *            record information of the parent schema
     * @return a new {@link RecordInfo} with all base information, excluding
     *         members
     */
    private RecordInfo getRecordInfo(Element schema, String recordName, RecordInfo parent) {
        RecordInfo recordInfo = new RecordInfo();
        recordInfo.setSchema(((TypeElement) schema).getQualifiedName().toString());
        recordInfo.setTypeName(parseTypeName(recordName, schema));
        recordInfo.setAbstract(schema.getAnnotation(Abstract.class) != null);
        recordInfo.setPkg(schemaContext.getPkg());
        recordInfo.setSimpleName(schemaContext.getRecordSimpleName());
        parseSuperInterfaces(recordInfo, schema);
        if (parent != null) {
            recordInfo.setParent(parent.getPkg() + PKG_DELIMITER + parent.getSimpleName());
        }
        return recordInfo;
    }

    /**
     * @param schema
     *            from which the context will be generated
     * @param recordName
     *            name of the record of the given schema
     * @param parent
     *            record information of the parent schema
     * @return context information of the given schema
     */
    private SchemaContext getSchemaContext(Element schema, String recordName, RecordInfo parent) {
        SchemaContext context = new SchemaContext();
        context.setParent(parent);

        parseRecordName(recordName, context);

        context.setGetterNameFormat(getterNameParser.parse(schema));

        parseInheritedProperties(schema, context);

        // set getter name format to default if not set
        if (context.getGetterNameFormat() == null) {
            context.setGetterNameFormat(getterNameParser.getDefaultGetterFormat());
        }

        return context;
    }

    /**
     * Parses a managed record name to extract its package and simple name
     * 
     * @param recordName
     *            full qualified name of the managed record
     * @param context
     *            to store the parsed values
     */
    private void parseRecordName(String recordName, SchemaContext context) {
        context.setRecordName(recordName);
        int lastIndexOfPkgDelimiter = recordName.lastIndexOf(PKG_DELIMITER);
        if (lastIndexOfPkgDelimiter > 0) {
            context.setRecordSimpleName(recordName.substring(lastIndexOfPkgDelimiter + 1));
            context.setPkg(recordName.substring(0, lastIndexOfPkgDelimiter));
        } else {
            context.setRecordSimpleName(recordName);
        }
    }

    /**
     * Parses and generates the schema properties that are inherited
     * 
     * @param schema
     *            the schema to parse
     * @param context
     *            to store all the inherited properties
     */
    private void parseInheritedProperties(Element schema, SchemaContext context) {
        List<Map<Emitted, Visibility>> inheritedVisibilities = new ArrayList<>();

        // first retrieve the schemas' visibilities
        inheritedVisibilities.add(visibilitiesParser.parse(schema, Emitted.ALL));

        // add inherited attributes from paradigms
        paradigmParser.traverseParadigms(schema, context, inheritedVisibilities);

        Map<Emitted, Visibility> visibilities = new HashMap<>();
        visibilitiesParser.populate(visibilities, inheritedVisibilities, visibilitiesParser.getDefaultVisibilities());
        context.setVisibilities(visibilities);
    }

    /**
     * Identifies if the schema adds a super interface
     * 
     * @param recordInfo
     *            to store a super interface of the managed record
     * @param schema
     *            the schema to parse
     */
    private void parseSuperInterfaces(RecordInfo recordInfo, Element schema) {
        List<? extends TypeMirror> directSupertypes = typeUtil.directSupertypes(schema.asType());
        for (TypeMirror typeMirror : directSupertypes) {
            if (!TypeKind.DECLARED.equals(typeMirror.getKind())) {
                continue;
            }
            if ("java.lang.Object".equals(typeMirror.toString())) {
                continue;
            }
            Element element = typeUtil.asElement(typeMirror);
            if (element.getAnnotation(RecordSchema.class) != null) {
                continue;
            }
            if (dataTypeParser.isManagedRecord(typeMirror, schemaContext.getPkg())) {
                messager.printMessage(Kind.ERROR,
                        "Ignoring interface, use schema interface instead: " + typeMirror.toString(), schema);
            } else {
                recordInfo.addSuperInterface(element.toString());
            }
        }
    }

    /**
     * Generates the MDS type name The default type name is the same as the
     * recordName
     * 
     * @param recordName
     *            qualified name of the record
     * @param schema
     *            schema element to parse
     * @return MDS type name of new record
     */
    private String parseTypeName(String recordName, Element schema) {
        String typeName = recordName;
        TypeName typeNameAnnotation = schema.getAnnotation(TypeName.class);
        if (typeNameAnnotation != null) {
            String name = typeNameAnnotation.name().trim();
            if (!name.isEmpty()) {
                typeName = name;
            } else {
                messager.printMessage(Kind.ERROR, "Empty name in annotation: " + TypeName.class.getName(), schema);
            }
        }
        return typeName;
    }

    /**
     * Parses the members of the new record
     * 
     * @param recordInfo
     *            to store parsed values
     * @param schema
     *            schema element with the declared members
     */
    private void parserMembers(RecordInfo recordInfo, Element schema) {
        for (Element member : schema.getEnclosedElements()) {
            LOGGER.info("parsing method: " + member.getSimpleName().toString());
            if (!ElementKind.METHOD.equals(member.getKind())) {
                continue;
            }
            assert member instanceof ExecutableElement;
            ExecutableElement method = (ExecutableElement) member;
            if (parseAbstractMethod(recordInfo, method)) {
            } else if (parseStaticMethod(recordInfo, method)) {
            } else if (parseInstanceMethod(recordInfo, method)) {
            } else if (fieldParser.parse(method, recordInfo, schemaContext)) {
            }
        }
    }

    /**
     * Identifies if the given member is a static method
     * 
     * @param recordInfo
     *            information of the record
     * @param method
     *            the member to prove
     * @return true if the given member is a static method
     */
    private boolean parseStaticMethod(RecordInfo recordInfo, ExecutableElement method) {
        if (!method.getModifiers().contains(Modifier.STATIC)) {
            return false;
        }
        try {
            MethodInfo methodInfo = null;
            if (method.getAnnotation(Static.class) != null) {
                methodInfo = methodParser.parse(method, 0, schemaContext);
            } else if (isMethodFirstParamOfType(method.getParameters(), STATIC_METHOD)) {
                methodInfo = methodParser.parse(method, 1, schemaContext);
                methodInfo.setOmitFirstParam(true);
            } else {
                return false;
            }
            if (methodInfo.getVisibility() == null) {
                methodInfo.setVisibility(schemaContext.getVisibilities().get(Emitted.STATIC_METHOD));
            }
            recordInfo.addStaticMethod(methodInfo);
        } catch (ProcessingException e) {
            messager.printMessage(Kind.ERROR, "Ignoring Static Method, " + e.getMessage(), e.getElement());
        }
        return true;
    }

    /**
     * Identifies and parses a member declared as an abstract method
     * 
     * @param recordInfo
     *            information of the record
     * @param method
     *            the member to prove and parse
     * @return true if the given member is an abstract method
     */
    private boolean parseAbstractMethod(RecordInfo recordInfo, ExecutableElement method) {
        if (method.getModifiers().contains(Modifier.STATIC)) {
            return false;
        }
        if (method.getAnnotation(Abstract.class) == null) {
            return false;
        }
        if (!recordInfo.isAbstract()) {
            messager.printMessage(Kind.ERROR, "abstract method found in non-abstract schema", method);
            return true;
        }
        try {
            MethodInfo methodInfo = methodParser.parse(method, 0, schemaContext);
            methodInfo.setAbstract(true);
            if (methodInfo.getVisibility() == null) {
                methodInfo.setVisibility(schemaContext.getVisibilities().get(Emitted.METHOD));
            }
            if (!Visibility.PRIVATE.equals(methodInfo.getVisibility())) {
                recordInfo.addInstanceMethod(methodInfo);
            } else {
                messager.printMessage(Kind.ERROR, "Illegal visibility level for abstract method", method);
            }
        } catch (ProcessingException e) {
            messager.printMessage(Kind.ERROR, "Ignoring Abstract Method, " + e.getMessage(), e.getElement());
        }
        return true;
    }

    /**
     * Identifies if the given member is a instance method
     * 
     * @param recordInfo
     *            information of the record
     * @param method
     *            the member to prove and parse
     * @return true if the given member is an instance method
     */
    private boolean parseInstanceMethod(RecordInfo recordInfo, ExecutableElement method) {
        if (!method.getModifiers().contains(Modifier.STATIC)) {
            return false;
        }
        if (!isMethodFirstParamOfType(method.getParameters(), PRIVATE)) {
            return false;
        }
        try {
            parseInstMethod(recordInfo, method);
        } catch (ProcessingException e) {
            messager.printMessage(Kind.ERROR, "Ignoring Instance Method, " + e.getMessage(), e.getElement());
        }
        return true;
    }

    /**
     * @param parameters
     *            first parameter of a method
     * @param expectedElement
     * 
     * @return true if the first parameter of given parameter has the format
     *         [record name].[expected element]
     */
    private boolean isMethodFirstParamOfType(List<? extends VariableElement> parameters, String expectedElement) {
        if (parameters.isEmpty()) {
            return false;
        }
        TypeMirror firstParam = parameters.get(0).asType();
        TypeKind kind = firstParam.getKind();
        if (TypeKind.DECLARED.equals(kind)) {
            // if the data type is of complex type
            assert firstParam instanceof DeclaredType;
            Element element = ((DeclaredType) firstParam).asElement();
            if (expectedElement.equals(element.getSimpleName().toString())
                    && schemaContext.getRecordName().equals(element.getEnclosingElement().toString())) {
                return true;
            }
        } else if (TypeKind.ERROR.equals(kind)) {
            // if its an unknown data type
            String expected = schemaContext.getRecordSimpleName() + PKG_DELIMITER + expectedElement;
            if (expected.equals(firstParam.toString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parses declared instance methods and constructors
     * 
     * @param recordInfo
     *            to store the parsed values
     * @param method
     *            element to parse with the declared instance method
     * @throws ProcessingException
     */
    private void parseInstMethod(RecordInfo recordInfo, ExecutableElement method) throws ProcessingException {
        MethodInfo methodInfo = methodParser.parse(method, 1, schemaContext);
        if (recordInfo.getSimpleName().equals(method.getSimpleName().toString())) {
            if (isValidConstructor(methodInfo)) {
                recordInfo.addUserConstructor(methodInfo);
                if (methodInfo.getVisibility() == null) {
                    methodInfo.setVisibility(schemaContext.getVisibilities().get(Emitted.CONSTRUCTOR));
                }
            }
        } else if (isValidInstanceMethod(methodInfo)) {
            recordInfo.addInstanceMethod(methodInfo);
            RecordInfo parent = schemaContext.getParent();
            if (parent != null) {
                methodInfo.setAmbiguous(parentHasMethod(parent, methodInfo));
            }
        }
        if (methodInfo.getVisibility() == null) {
            methodInfo.setVisibility(schemaContext.getVisibilities().get(Emitted.METHOD));
        }
    }

    /**
     * @param parent
     *            parent record
     * @param methodInfo
     *            information of the method
     * @return true if a method with the same signature exists in the parent
     */
    private boolean parentHasMethod(RecordInfo parent, MethodInfo methodInfo) {
        for (MethodInfo parentMethod : parent.getMethods()) {
            if (parentMethod.getName().equals(methodInfo.getName())
                    && parentMethod.getParameters().equals(methodInfo.getParameters())) {
                return true;
            }
        }
        return false;
    }

    /**
     * validate if the given method is a valid instance method
     * 
     * @param methodInfo
     *            method to validate
     * @return true if the method is valid, false otherwise
     */
    private boolean isValidInstanceMethod(MethodInfo methodInfo) {
        if (noPublicRecordParameter(methodInfo)) {
            messager.printMessage(Kind.WARNING, "non public record parameter", methodInfo.getMethod());
        }
        return true;
    }

    /**
     * validate if the given method is a valid constructor
     * 
     * @param methodInfo
     *            method to validate
     * @return true if the constructor is valid, false otherwise
     */
    private boolean isValidConstructor(MethodInfo methodInfo) {
        DataType dataType = methodInfo.getReturnType().getType();
        if (!DataType.VOID.equals(dataType)) {
            messager.printMessage(Kind.ERROR, "Ignoring return type of constructor: " + dataType,
                    methodInfo.getMethod());
        }
        if (noPublicRecordParameter(methodInfo)) {
            messager.printMessage(Kind.ERROR,
                    "Ignoring constructor, cannot access private or protected of a different record type",
                    methodInfo.getMethod());
            return false;
        }
        return true;
    }

    /**
     * @param methodInfo
     *            method to verify
     * @return true if the given method has a record type parameter different
     *         from the record name
     */
    private boolean noPublicRecordParameter(MethodInfo methodInfo) {
        for (VarInfo varInfo : methodInfo.getParameters()) {
            if (DataType.RECORD.equals(varInfo.getType()) && varInfo.isEmittedTwice()
                    && !varInfo.getComplexType().startsWith(schemaContext.getRecordName())) {
                return true;
            }
        }
        return false;
    }
}
