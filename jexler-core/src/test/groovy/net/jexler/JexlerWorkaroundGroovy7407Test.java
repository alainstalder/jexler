/*
   Copyright 2012-now $(whois jexler.net)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package net.jexler;

import groovy.grape.Grape;
import groovy.grape.GrapeEngine;
import groovy.lang.GroovyClassLoader;
import net.jexler.test.FastTests;
import org.codehaus.groovy.control.CompilationFailedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the respective class.
 *
 * @author $(whois jexler.net)
 */
@Category(FastTests.class)
public final class JexlerWorkaroundGroovy7407Test {

    private void reset() throws Exception {
        System.clearProperty(Jexler.WorkaroundGroovy7407.GRAPE_ENGINE_WRAP_PROPERTY_NAME);
        Jexler.WorkaroundGroovy7407.resetForUnitTests();
        Jexler.WorkaroundGroovy7407WrappingGrapeEngine.setEngine(null);
    }

    @Before
    public void setup() throws Exception {
        reset();
    }

    @After
    public void teardown() throws Exception {
        reset();
    }

    @Test
    public void testConstructors() throws Exception {
        new Jexler.WorkaroundGroovy7407();
        new Jexler.WorkaroundGroovy7407WrappingGrapeEngine("lock", null);
    }

    @Test
    public void testCompileOkWithWrapping() throws Exception {

        assertFalse("must be false", Grape.getInstance() instanceof Jexler.WorkaroundGroovy7407WrappingGrapeEngine);
        System.setProperty(Jexler.WorkaroundGroovy7407.GRAPE_ENGINE_WRAP_PROPERTY_NAME, "true");

        File dir = Files.createTempDirectory(null).toFile();
        File file = new File(dir, "test.groovy");

        Files.createFile(file.toPath());

        Jexler jexler = new Jexler(file, new JexlerContainer(dir));
        jexler.start();
        jexler.waitForStartup(10000);
        assertEquals("must be same", RunState.OFF, jexler.getRunState());
        assertTrue("must be true", jexler.getIssues().isEmpty());

        assertTrue("must be true", Grape.getInstance() instanceof Jexler.WorkaroundGroovy7407WrappingGrapeEngine);
    }

    @Test
    public void testCompileFailsWithWrapping() throws Exception {

        assertFalse("must be false", Grape.getInstance() instanceof Jexler.WorkaroundGroovy7407WrappingGrapeEngine);
        System.setProperty(Jexler.WorkaroundGroovy7407.GRAPE_ENGINE_WRAP_PROPERTY_NAME, "true");

        File dir = Files.createTempDirectory(null).toFile();
        File file = new File(dir, "test.groovy");

        FileWriter writer = new FileWriter(file);
        writer.append("&%!+\n");
        writer.close();

        Jexler jexler = new Jexler(file, new JexlerContainer(dir));
        jexler.start();
        jexler.waitForStartup(10000);
        assertEquals("must be same", RunState.OFF, jexler.getRunState());

        assertEquals("must be same", 1, jexler.getIssues().size());
        Issue issue = jexler.getIssues().get(0);
        System.out.println(issue.toString());
        assertTrue("must be true",
                issue.getMessage().contains("Script compile failed."));
        assertEquals("must be same", jexler, issue.getService());
        assertNotNull("must not be null", issue.getCause());
        assertTrue("must be true", issue.getCause() instanceof CompilationFailedException);

        assertTrue("must be true", Grape.getInstance() instanceof Jexler.WorkaroundGroovy7407WrappingGrapeEngine);
    }

    private static class MockEngine implements GrapeEngine {
        @Override public Object grab(String endorsedModule) { return null; }
        @Override public Object grab(Map args) { return null; }
        @Override public Object grab(Map args, Map... dependencies) { return null; }
        @Override public Map<String, Map<String, List<String>>> enumerateGrapes() { return null; }
        @Override public URI[] resolve(Map args, Map... dependencies) { return null; }
        @Override public URI[] resolve(Map args, List depsInfo, Map... dependencies) { return null; }
        @Override public Map[] listDependencies(ClassLoader classLoader) { return null; }
        @Override public void addResolver(Map<String, Object> args) {}
    }

    @Test
    public void shallowTestOfWrappingGrapeEngine() throws Exception {
        final Jexler.WorkaroundGroovy7407WrappingGrapeEngine engine =
                new Jexler.WorkaroundGroovy7407WrappingGrapeEngine("lock", new MockEngine());
        final Map<String,Object> testMap = new HashMap<>();
        testMap.put("calleeDepth", 3);
        assertNull("must be null", engine.grab("dummy endorsed"));
        assertNull("must be null", engine.grab(new HashMap()));
        assertNull("must be null", engine.grab(new HashMap(), new HashMap()));
        assertNull("must be null", engine.grab(testMap));
        assertNull("must be null", engine.grab(testMap, testMap));
        assertNull("must be null", engine.enumerateGrapes());
        assertNull("must be null", engine.resolve(new HashMap(), new HashMap()));
        assertNull("must be null", engine.resolve(testMap, new HashMap()));
        assertNull("must be null", engine.resolve(new HashMap(), new LinkedList(), new HashMap()));
        assertNull("must be null", engine.listDependencies(new GroovyClassLoader()));
        engine.addResolver(new HashMap<String, Object>());
    }

}
