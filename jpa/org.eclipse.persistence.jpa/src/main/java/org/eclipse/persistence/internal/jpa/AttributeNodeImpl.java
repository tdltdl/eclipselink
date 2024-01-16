/*
 * Copyright (c) 1998, 2024 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     09 Jan 2013-2.5 Gordon Yorke
//       - 397772: JPA 2.1 Entity Graph Support
package org.eclipse.persistence.internal.jpa;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.AttributeNode;
import jakarta.persistence.Subgraph;


/**
 * Concrete JPA AttributeNode class. For this implementation the AttributeNode includes information from live
 * AttributeItems.
 */
@SuppressWarnings({"rawtypes"})
public class AttributeNodeImpl<X> implements AttributeNode<X>{


    protected String currentAttribute;

    protected Map<Class, Subgraph> subgraphs;

    protected Map<Class, Subgraph> keySubgraphs;

    protected AttributeNodeImpl(){
    }

    protected AttributeNodeImpl(String attribute){
        this.currentAttribute = attribute;
    }

    @Override
    public String getAttributeName() {
        return currentAttribute;
    }

    @Override
    public Map<Class, Subgraph> getSubgraphs() {
        if (this.subgraphs == null){
            this.subgraphs = new HashMap<>();
        }
        return this.subgraphs;
    }

    @Override
    public Map<Class, Subgraph> getKeySubgraphs() {
        if (this.keySubgraphs == null){
            this.keySubgraphs = new HashMap<>();
        }
        return this.keySubgraphs;
    }

    public void addSubgraph(EntityGraphImpl entityGraphImpl) {
        if (this.subgraphs == null){
            this.subgraphs = new HashMap<>();
        }
        this.subgraphs.put(entityGraphImpl.getClassType(), entityGraphImpl);
    }

    public void addKeySubgraph(EntityGraphImpl entityGraphImpl) {
        if (this.keySubgraphs == null){
            this.keySubgraphs = new HashMap<>();
        }
        this.keySubgraphs.put(entityGraphImpl.getClassType(), entityGraphImpl);
    }
}
