/*
 * Copyright (c) 2013, 2024 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2013.02.27 at 11:03:32 AM EST
//


package org.eclipse.persistence.jaxb.xmlmodel;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="xml-named-attribute-node" type="{http://www.eclipse.org/eclipselink/xsds/persistence/oxm}xml-named-attribute-node" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="xml-named-subgraph" type="{http://www.eclipse.org/eclipselink/xsds/persistence/oxm}xml-named-subgraph" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="xml-named-subclass-graph" type="{http://www.eclipse.org/eclipselink/xsds/persistence/oxm}xml-named-subgraph" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "xmlNamedAttributeNode",
    "xmlNamedSubgraph",
    "xmlNamedSubclassGraph"
})
@XmlRootElement(name = "xml-named-object-graph")
public class XmlNamedObjectGraph {

    @XmlElement(name = "xml-named-attribute-node")
    protected List<XmlNamedAttributeNode> xmlNamedAttributeNode;
    @XmlElement(name = "xml-named-subgraph")
    protected List<XmlNamedSubgraph> xmlNamedSubgraph;
    @XmlElement(name = "xml-named-subclass-graph")
    protected List<XmlNamedSubgraph> xmlNamedSubclassGraph;
    @XmlAttribute(name = "name")
    protected String name;

    /**
     * Gets the value of the xmlNamedAttributeNode property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the xmlNamedAttributeNode property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getXmlNamedAttributeNode().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link XmlNamedAttributeNode }
     *
     *
     */
    public List<XmlNamedAttributeNode> getXmlNamedAttributeNode() {
        if (xmlNamedAttributeNode == null) {
            xmlNamedAttributeNode = new ArrayList<>();
        }
        return this.xmlNamedAttributeNode;
    }

    /**
     * Gets the value of the xmlNamedSubgraph property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the xmlNamedSubgraph property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getXmlNamedSubgraph().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link XmlNamedSubgraph }
     *
     *
     */
    public List<XmlNamedSubgraph> getXmlNamedSubgraph() {
        if (xmlNamedSubgraph == null) {
            xmlNamedSubgraph = new ArrayList<>();
        }
        return this.xmlNamedSubgraph;
    }

    /**
     * Gets the value of the xmlNamedSubclassGraph property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the xmlNamedSubclassGraph property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getXmlNamedSubclassGraph().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link XmlNamedSubgraph }
     *
     *
     */
    public List<XmlNamedSubgraph> getXmlNamedSubclassGraph() {
        if (xmlNamedSubclassGraph == null) {
            xmlNamedSubclassGraph = new ArrayList<>();
        }
        return this.xmlNamedSubclassGraph;
    }

    /**
     * Gets the value of the name property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setName(String value) {
        this.name = value;
    }

}
