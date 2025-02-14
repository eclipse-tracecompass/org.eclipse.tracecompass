/*******************************************************************************
 * Copyright (c) 2018, 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.common.core.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.traceeventlogger.LogUtils;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * XML Utilities. Useful to avoid copy-pasting secure code generation. Utils
 * here should be OWASP compliant.
 *
 * @author Matthew Khouzam
 * @since 3.2
 */
public final class XmlUtils {

    private static final Logger LOGGER = TraceCompassLog.getLogger(XmlUtils.class);

    private static final String EMPTY = ""; //$NON-NLS-1$
    private static final String DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl"; //$NON-NLS-1$
    private static final String NONVALIDATING_LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd"; //$NON-NLS-1$
    private static final String USE_EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities"; //$NON-NLS-1$
    private static final String ACCESS_EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities"; //$NON-NLS-1$

    private XmlUtils() {
        // Do nothing
    }

    /**
     * <p>
     * Create a new <code>Transformer</code> that performs a copy of the
     * <code>Source</code> to the <code>Result</code>. i.e. the "<em>identity
     * transform</em>".
     * </p>
     * <p>
     * This is thread safe.
     * </p>
     * <p>
     * Use {@link XMLConstants#FEATURE_SECURE_PROCESSING} to ensure that the
     * transformer is secure. Hence the NOSONAR comment that disables the
     * java:S2755 rule check. Eclipse fails to recognize the corresponding, more
     * specific {@code @SuppressWarnings} annotated token unfortunately.
     * </p>
     *
     * @return A Transformer object that may be used to perform a transformation
     *         in a single thread, never null.
     *
     * @throws TransformerConfigurationException
     *             When it is not possible to create a <code>Transformer</code>
     *             instance.
     */
    public static Transformer newSecureTransformer() throws TransformerConfigurationException {
        TransformerFactory factory = TransformerFactory.newInstance(); // NOSONAR
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory.newTransformer();
    }

    /**
     * Create a document builder factory that is safe according to the OWASP
     * injection prevention cheat sheet.
     *
     * @return the documentBuilderFactory
     * @since 4.1
     */
    public static DocumentBuilderFactory newSafeDocumentBuilderFactory() {
        String feature = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            // This one is from Sonar (squid:S2755)
            feature = XMLConstants.FEATURE_SECURE_PROCESSING;
            dbf.setFeature(feature, true);
        } catch (ParserConfigurationException e) {
            logException(feature, e);
        }
        try {
            // This is the PRIMARY defense. If DTDs (doctypes) are disallowed,
            // almost all
            // XML entity attacks are prevented
            // Xerces 2 only -
            // http://xerces.apache.org/xerces2-j/features.html#disallow-doctype-decl
            feature = DISALLOW_DOCTYPE_DECL;
            dbf.setFeature(feature, true);

        } catch (ParserConfigurationException e) {
            logException(feature, e);
        }
        try {
            // If you can't completely disable DTDs, then at least do the
            // following:
            // Xerces 1 -
            // http://xerces.apache.org/xerces-j/features.html#external-general-entities
            // Xerces 2 -
            // http://xerces.apache.org/xerces2-j/features.html#external-general-entities
            // JDK7+ - http://xml.org/sax/features/external-general-entities
            feature = ACCESS_EXTERNAL_GENERAL_ENTITIES;
            dbf.setFeature(feature, false);
        } catch (ParserConfigurationException e) {
            logException(feature, e);
        }
        try {
            // Xerces 1 -
            // http://xerces.apache.org/xerces-j/features.html#external-parameter-entities
            // Xerces 2 -
            // http://xerces.apache.org/xerces2-j/features.html#external-parameter-entities
            // JDK7+ - http://xml.org/sax/features/external-parameter-entities
            feature = USE_EXTERNAL_PARAMETER_ENTITIES;
            dbf.setFeature(feature, false);
        } catch (ParserConfigurationException e) {
            logException(feature, e);
        }
        try {
            // Disable external DTDs as well
            feature = NONVALIDATING_LOAD_EXTERNAL_DTD;
            dbf.setFeature(feature, false);
        } catch (ParserConfigurationException e) {
            logException(feature, e);
        } // and these as well, per Timothy Morgan's 2014 paper: "XML Schema,
          // DTD, and Entity Attacks"
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);

        return dbf;
    }

    private static void logException(String feature, ParserConfigurationException e) {
        // This should catch a failed setFeature feature
        LogUtils.traceInstant(LOGGER, Level.WARNING, "ParserConfigurationException", "feature", feature, "stack", e.getStackTrace()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    /**
     * Create a new safe {@link XMLStreamReader} from an {@link InputStream}
     *
     * @param inputStream
     *            the {@link InputStream} to read
     * @return an XML stream reader
     * @throws XMLStreamException
     *             if the stream could not be read or is badly formatted
     * @since 4.2
     */
    public static @Nullable XMLStreamReader newSafeXmlStreamReader(InputStream inputStream) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        /*
         * Disable XML External entity (XXE) parsing
         */
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        return factory.createXMLStreamReader(inputStream);
    }

    /**
     * Get a safe {@link SchemaFactory}
     *
     * @return a safe {@link SchemaFactory}
     * @throws SAXException
     *             parse exception with
     *             {@link XMLConstants#W3C_XML_SCHEMA_NS_URI}
     * @since 4.2
     */
    public static SchemaFactory newSafeSchemaFactory() throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, EMPTY);
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, EMPTY);
        return factory;
    }

    /**
     * Validate a source, throws an exception if invalid.
     *
     * @param schema
     *            the schema to verify against. Use
     *            {@link #newSafeSchemaFactory()} for a safe schema
     * @param source
     *            the source to verify
     * @throws SAXException
     *             when the underlying XMLReader cannot set the security
     *             properties -or- when the source fails validation
     * @throws IOException
     *             the source failed validation due to file reasons, e.g.
     *             permissions
     * @since 4.2
     */
    public static void safeValidate(Schema schema, Source source) throws SAXException, IOException {
        Validator validator = schema.newValidator();
        validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, EMPTY);
        validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, EMPTY);
        validator.validate(source);
    }

    /**
     * Create a new secure {@link SAXParserFactory}
     *
     * @return a new secure {@link SAXParserFactory}
     * @throws ParserConfigurationException
     *             The parser cannot be created with the default configuration
     * @throws SAXNotRecognizedException
     *             When the underlying XMLReader does not recognize the property
     *             name.
     * @throws SAXNotSupportedException
     *             When the underlying XMLReader does recognize the property
     *             name but does not support it
     * @since 4.2
     */
    public static SAXParserFactory newSafeSaxParserFactory() throws SAXNotRecognizedException, SAXNotSupportedException, ParserConfigurationException {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        parserFactory.setFeature(ACCESS_EXTERNAL_GENERAL_ENTITIES, false);
        parserFactory.setFeature(USE_EXTERNAL_PARAMETER_ENTITIES, false);
        parserFactory.setFeature(NONVALIDATING_LOAD_EXTERNAL_DTD, false);
        parserFactory.setFeature(DISALLOW_DOCTYPE_DECL, true);
        parserFactory.setNamespaceAware(true);
        return parserFactory;
    }
}
