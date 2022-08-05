/*
 * Copyright (c) 2011, 2022 Oracle and/or its affiliates. All rights reserved.
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
//     01/19/2010-2.1 Guy Pelletier
//       - 211322: Add fetch-group(s) support to the EclipseLink-ORM.XML Schema
//     01/15/2016-2.7 Mythily Parthasarathy
//       - 485984: Add test for retrieval of cached getReference within a txn
package org.eclipse.persistence.testing.tests.jpa.advanced.fetchgroup;

import jakarta.persistence.EntityManager;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.persistence.config.QueryHints;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.descriptors.FetchGroupManager;
import org.eclipse.persistence.internal.sessions.UnitOfWorkImpl;
import org.eclipse.persistence.testing.framework.jpa.junit.JUnitTestCase;
import org.eclipse.persistence.testing.models.jpa.advanced.fetchgroup.AdvancedFetchGroupTableCreator;
import org.eclipse.persistence.testing.models.jpa.advanced.fetchgroup.ChestProtector;
import org.eclipse.persistence.testing.models.jpa.advanced.fetchgroup.GoalieGear.AgeGroup;
import org.eclipse.persistence.testing.models.jpa.advanced.fetchgroup.Helmet;
import org.eclipse.persistence.testing.models.jpa.advanced.fetchgroup.HockeyGear;
import org.eclipse.persistence.testing.models.jpa.advanced.fetchgroup.Pads;
import org.eclipse.persistence.testing.models.jpa.advanced.fetchgroup.Shelf;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class AdvancedFetchGroupTest extends JUnitTestCase {
    private static Integer padsId;
    private static Integer chestProtectorId;

    public AdvancedFetchGroupTest() {
        super();
    }

    public AdvancedFetchGroupTest(String name) {
        super(name);
    }

    @Override
    public String getPersistenceUnitName() {
        return "fetchgroup";
    }

    @Override
    public void setUp() {
        clearCache();
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.setName("AdvancedFetchGroupTest");

        suite.addTest(new AdvancedFetchGroupTest("testSetup"));
        suite.addTest(new AdvancedFetchGroupTest("testVerifyFetchGroups"));
        suite.addTest(new AdvancedFetchGroupTest("testCreateHockeyGear"));
        suite.addTest(new AdvancedFetchGroupTest("testFetchGroupOnPads"));
        suite.addTest(new AdvancedFetchGroupTest("testFetchGroupOnChestProtector"));
        suite.addTest(new AdvancedFetchGroupTest("testFetchGroupOnPadsFromInheritanceParent"));
        // Bug 434120
        suite.addTest(new AdvancedFetchGroupTest("testFetchGroupMergeMapAttribute"));
        // Bug 485984
        suite.addTest(new AdvancedFetchGroupTest("testFetchGroupForCachedReference"));

        return suite;
    }

    /**
     * The setup is done as a test, both to record its failure, and to allow execution in the server.
     */
    public void testSetup() {
        new AdvancedFetchGroupTableCreator().replaceTables(getPersistenceUnitServerSession());
        clearCache();
    }

    public void testVerifyFetchGroups() {
        if (isWeavingEnabled()) {
            ClassDescriptor hockeyGearDescriptor = getPersistenceUnitServerSession().getDescriptor(HockeyGear.class);
            FetchGroupManager hockeyGearFetchGroupManager = hockeyGearDescriptor.getFetchGroupManager();
            assertEquals("Wrong number of fetch groups for HockeyGear", 1, hockeyGearFetchGroupManager.getFetchGroups().size());
            assertNotNull("The 'MSRP' fetch group was not found for HockeyGear", hockeyGearFetchGroupManager.getFetchGroup("MSRP"));

            ClassDescriptor padsDescriptor = getPersistenceUnitServerSession().getDescriptor(Pads.class);
            FetchGroupManager padsFetchGroupManager = padsDescriptor.getFetchGroupManager();
            assertEquals("Wrong number of fetch groups for Pads", 3, padsFetchGroupManager.getFetchGroups().size());
            assertNotNull("The 'HeightAndWidth' fetch group was not found for Pads", padsFetchGroupManager.getFetchGroup("HeightAndWidth"));
            assertNotNull("The 'Weight' fetch group was not found for Pads", padsFetchGroupManager.getFetchGroup("Weight"));
            assertNotNull("The 'AgeGroup' fetch group was not found for Pads", padsFetchGroupManager.getFetchGroup("AgeGroup"));

            ClassDescriptor chestProtectorDescriptor = getPersistenceUnitServerSession().getDescriptor(ChestProtector.class);
            FetchGroupManager chestProtectorFetchGroupManager = chestProtectorDescriptor.getFetchGroupManager();
            assertEquals("Wrong number of fetch groups for ChestProtector", 1, chestProtectorFetchGroupManager.getFetchGroups().size());
            assertNotNull("The 'AgeGroup' fetch group was not found for ChestProtector", chestProtectorFetchGroupManager.getFetchGroup("AgeGroup"));
        }
    }

    public void testCreateHockeyGear() {
        if (isWeavingEnabled()) {
            EntityManager em = createEntityManager();
            beginTransaction(em);

            try {
                Pads pads = new Pads();
                pads.setAgeGroup(AgeGroup.SENIOR);
                pads.setDescription("Louisville TPS");
                pads.setHeight(35.5);
                pads.setMsrp(999.99);
                pads.setWeight(4.9);
                pads.setWidth(11.0);
                em.persist(pads);

                ChestProtector chestProtector = new ChestProtector();
                chestProtector.setAgeGroup(AgeGroup.INTERMEDIATE);
                chestProtector.setDescription("RBK Premier");
                chestProtector.setMsrp(599.99);
                chestProtector.setSize("Large");
                em.persist(chestProtector);

                commitTransaction(em);

                padsId = pads.getSerialNumber();
                chestProtectorId = chestProtector.getSerialNumber();
            } catch (RuntimeException e) {
                if (isTransactionActive(em)){
                    rollbackTransaction(em);
                }
                closeEntityManager(em);
                throw e;
            }
        }
    }

    public void testFetchGroupOnPads() {
        if (isWeavingEnabled()) {
            EntityManager em = createEntityManager();
            Map<String, Object> properties = new HashMap<>();
            properties.put(QueryHints.FETCH_GROUP_NAME, "HeightAndWidth");
            Class<Pads> PadsClass = Pads.class;
            Pads pads = em.find(PadsClass, padsId, properties);

            try {
                verifyFetchedField(PadsClass.getDeclaredField("height"), pads, 35.5);
                verifyFetchedField(PadsClass.getDeclaredField("width"), pads, 11.0);

                verifyNonFetchedField(PadsClass.getDeclaredField("weight"), pads);
                verifyNonFetchedField(PadsClass.getField("ageGroup"), pads);
                verifyNonFetchedField(PadsClass.getField("description"), pads);
                verifyNonFetchedField(PadsClass.getField("msrp"), pads);
            } catch (Exception e) {
                fail("Error verifying field content: " + e.getMessage());
            } finally {
                closeEntityManager(em);
            }
        }
    }

    public void testFetchGroupOnChestProtector() {
        if (isWeavingEnabled()) {
            EntityManager em = createEntityManager();
            Map<String, Object> properties = new HashMap<>();
            properties.put(QueryHints.FETCH_GROUP_NAME, "AgeGroup");
            Class<ChestProtector> chestProtectorClass = ChestProtector.class;
            ChestProtector chestProtector = em.find(chestProtectorClass, chestProtectorId, properties);

            try {
                verifyFetchedField(chestProtectorClass.getField("ageGroup"), chestProtector, AgeGroup.INTERMEDIATE);

                verifyNonFetchedField(chestProtectorClass.getField("description"), chestProtector);
                verifyNonFetchedField(chestProtectorClass.getField("msrp"), chestProtector);
                verifyNonFetchedField(chestProtectorClass.getDeclaredField("size"), chestProtector);
            } catch (Exception e) {
                fail("Error verifying field content: " + e.getMessage());
            } finally {
                closeEntityManager(em);
            }
        }
    }

    public void testFetchGroupOnPadsFromInheritanceParent() {
        if (isWeavingEnabled()) {
            EntityManager em = createEntityManager();
            Map<String, Object> properties = new HashMap<>();
            properties.put(QueryHints.FETCH_GROUP_NAME, "MSRP");
            Class<Pads> PadsClass = Pads.class;
            Pads pads = em.find(PadsClass, padsId, properties);

            try {
                verifyFetchedField(PadsClass.getField("msrp"), pads, 999.99);

                verifyNonFetchedField(PadsClass.getDeclaredField("height"), pads);
                verifyNonFetchedField(PadsClass.getDeclaredField("width"), pads);
                verifyNonFetchedField(PadsClass.getDeclaredField("weight"), pads);
                verifyNonFetchedField(PadsClass.getField("ageGroup"), pads);
                verifyNonFetchedField(PadsClass.getField("description"), pads);

            } catch (Exception e) {
                fail("Error verifying field content: " + e.getMessage());
            } finally {
                closeEntityManager(em);
            }
        }
    }

    public void testFetchGroupMergeMapAttribute() {
        if (!isWeavingEnabled()) {
            return;
        }

        // test data - manual creation
        int helmetId = 1;
        Helmet helmet = null;
        EntityManager em = createEntityManager();
        try {
            beginTransaction(em);
            em.createNativeQuery("DELETE FROM JPA_HELMET_PROPERTIES WHERE HELMET_ID=" + helmetId).executeUpdate();
            em.createNativeQuery("DELETE FROM JPA_HELMET WHERE ID=" + helmetId).executeUpdate();
            em.createNativeQuery("INSERT INTO JPA_HELMET (ID, COLOR) VALUES (" + helmetId + ", 'red')").executeUpdate();
            commitTransaction(em);
        } catch (Exception e) {
            fail("Error creating test data: " + e.getMessage());
        } finally {
            closeEntityManager(em);
        }

        // test
        em = createEntityManager();
        try {
            beginTransaction(em);
            helmet = em.find(Helmet.class, helmetId);
            assertNotNull("Found Helmet entity with id: " + helmetId + " should be non-null", helmet);
            em.clear();

            helmet.getColor();
            helmet.addProperty("random", "This parrot is deceased");
            Helmet helmetMerged = em.merge(helmet);

            commitTransaction(em);
        } catch (Exception e) {
            fail("Error merging an Entity with a Map attribute: " + e.getMessage());
        } finally {
            if (isTransactionActive(em)) {
                rollbackTransaction(em);
            }
            beginTransaction(em);
            helmet = em.find(Helmet.class, helmetId);
            if (helmet != null) {
                em.remove(helmet);
            }
            commitTransaction(em);
            closeEntityManager(em);
        }
    }
    
    public void testFetchGroupForCachedReference() {
        if (isWeavingEnabled()) {
            EntityManager em = createEntityManager();
            beginTransaction(em);

            Shelf original = new Shelf();
            original.setId(1L);
            original.setName("original");
            em.persist(original);

            Shelf modified = new Shelf();
            modified.setId(2L);
            modified.setName("modified");
            em.persist(modified);

            Helmet helmet = new Helmet();
            helmet.setId(3);
            helmet.setShelf(original);
            em.persist(helmet);

            commitTransaction(em);
            closeEntityManager(em);
            clearCache();

            em = createEntityManager();
            //Create a changeset to ensure that the getReference() result is pushed to cache
            beginTransaction(em);
            em.unwrap(UnitOfWorkImpl.class).beginEarlyTransaction();
            
            helmet = em.find(Helmet.class,  helmet.getId());
            modified = em.getReference(Shelf.class, modified.getId());
            helmet.setShelf(modified);

            commitTransaction(em);
            closeEntityManager(em);

            try {
                em = createEntityManager();
                beginTransaction(em);
                em.unwrap(UnitOfWorkImpl.class).beginEarlyTransaction();

                modified = em.find(Shelf.class, modified.getId());
                if (modified.getName() == null) {
                    fail("find returned entity with missing attribute");
                }
            } finally {
                if (isTransactionActive(em)){
                   rollbackTransaction(em); 
                }
                beginTransaction(em);
                helmet = em.find(Helmet.class,  helmet.getId());
                em.remove(helmet);
                modified = em.find(Shelf.class,  modified.getId());
                em.remove(modified);
                original = em.find(Shelf.class,  original.getId());
                em.remove(original);
                commitTransaction(em);
                closeEntityManager(em);
            }
        }

    }

    protected void verifyFetchedField(Field field, Object obj, Object value) {
        try {
            field.setAccessible(true);
            assertEquals("The field [" + field.getName() + "] was not fetched", field.get(obj), value);
        } catch (IllegalAccessException e) {
            fail("Error verifying field content: " + e.getMessage());
        }
    }

    protected void verifyNonFetchedField(Field field, Object obj) {
        try {
            field.setAccessible(true);
            assertNull("The field [" + field.getName() + "] was fetched", field.get(obj));
        } catch (IllegalAccessException e) {
            fail("Error verifying field content: " + e.getMessage());
        }
    }
}
